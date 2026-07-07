package dev.hinny.skrot.data.backup

import androidx.room.withTransaction
import dev.hinny.skrot.data.db.SkrotDatabase
import dev.hinny.skrot.data.model.BodyMetric
import dev.hinny.skrot.data.model.Exercise
import dev.hinny.skrot.data.model.LoggedSet
import dev.hinny.skrot.data.model.MeasurementType
import dev.hinny.skrot.data.model.MuscleGroup
import dev.hinny.skrot.data.model.PlannedExercise
import dev.hinny.skrot.data.model.PlannedSet
import dev.hinny.skrot.data.model.Routine
import dev.hinny.skrot.data.model.RoutineDay
import dev.hinny.skrot.data.model.SessionExercise
import dev.hinny.skrot.data.model.SetType
import dev.hinny.skrot.data.model.WorkoutSession
import java.time.Instant
import java.time.ZoneId

class JefitImporter(private val db: SkrotDatabase) {

    data class Preview(
        val sessionCount: Int,
        val setCount: Int,
        val matchedExercises: List<String>,
        val newExercises: List<String>,
        val skipped: List<String>,
        val bodyMetricCount: Int = 0,
        val routineCount: Int = 0,
    )

    data class Summary(
        val sessionsCreated: Int,
        val setsCreated: Int,
        val exercisesCreated: Int,
        val skipped: List<String>,
        val bodyMetricsCreated: Int = 0,
        val routinesCreated: Int = 0,
    )

    /** Normalizes an exercise name for case-insensitive, punctuation-tolerant matching. */
    fun normalize(name: String): String =
        name.lowercase()
            .replace(Regex("[-_/()]"), " ")
            .replace(Regex("[^a-z0-9åäö ]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

    private suspend fun buildMatcher(): (String) -> Exercise? {
        val all = db.exerciseDao().getAll()
        val byNorm = mutableMapOf<String, Exercise>()
        for (e in all) {
            byNorm.putIfAbsent(normalize(e.nameEn), e)
            byNorm.putIfAbsent(normalize(e.nameSv), e)
        }
        val bySquashed = byNorm.mapKeys { it.key.replace(" ", "") }

        return matcher@{ raw ->
            val norm = normalize(raw)
            byNorm[norm]?.let { return@matcher it }
            bySquashed[norm.replace(" ", "")]?.let { return@matcher it }
            // Fuzz on common variants: plural "s", a leading "barbell " qualifier.
            byNorm[norm.removeSuffix("s")]?.let { return@matcher it }
            byNorm["${norm}s"]?.let { return@matcher it }
            byNorm[norm.removePrefix("barbell ").trim()]?.let { return@matcher it }
            null
        }
    }

    suspend fun preview(parsed: JefitCsvParser.Result): Preview {
        val match = buildMatcher()
        val matched = linkedSetOf<String>()
        val unmatched = linkedSetOf<String>()
        fun note(name: String) {
            if (match(name) != null) matched.add(name) else unmatched.add(name)
        }
        for (session in parsed.sessions) {
            for (ex in session.exercises) note(ex.name)
        }
        for (routine in parsed.routines) {
            for (day in routine.days) {
                for (ex in day.exercises) note(ex.name)
            }
        }
        return Preview(
            sessionCount = parsed.sessions.size,
            setCount = parsed.totalSets,
            matchedExercises = matched.toList(),
            newExercises = unmatched.toList(),
            skipped = parsed.skipped,
            bodyMetricCount = parsed.bodyMetrics.size,
            routineCount = parsed.routines.size,
        )
    }

    suspend fun commit(parsed: JefitCsvParser.Result): Summary {
        val match = buildMatcher()
        var sessions = 0
        var sets = 0
        var created = 0
        var bodyMetrics = 0
        var routinesCreated = 0
        val createdByName = mutableMapOf<String, Long>()
        val extraSkips = mutableListOf<String>()
        val zone = ZoneId.systemDefault()

        db.withTransaction {
            suspend fun resolveExercise(name: String): Long =
                match(name)?.id ?: createdByName.getOrPut(normalize(name)) {
                    created++
                    db.exerciseDao().insert(
                        Exercise(
                            nameEn = name,
                            nameSv = name,
                            muscleGroup = MuscleGroup.FULL_BODY,
                            measurementType = MeasurementType.WEIGHT_KG,
                            isCustom = true,
                        )
                    )
                }

            // Body logs: one entry per date; skip dates that already have one.
            val existingDates = db.backupDao().allBodyMetrics()
                .map { Instant.ofEpochMilli(it.date).atZone(zone).toLocalDate() }
                .toHashSet()
            for (metric in parsed.bodyMetrics) {
                if (metric.date in existingDates) continue
                db.bodyMetricDao().insert(
                    BodyMetric(
                        date = metric.date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli(),
                        weightKg = metric.weightKg,
                        waistCm = metric.waistCm,
                        chestCm = metric.chestCm,
                        armsCm = metric.armsCm,
                        thighsCm = metric.thighsCm,
                        hipsCm = metric.hipsCm,
                    )
                )
                bodyMetrics++
            }

            for (parsedSession in parsed.sessions) {
                val start = parsedSession.date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
                val end = start + 60 * 60 * 1000
                val sessionId = db.sessionDao().insertSession(
                    WorkoutSession(
                        startedAt = start,
                        endedAt = end,
                        lastActivityAt = end,
                        note = "Imported from JEFIT",
                    )
                )
                sessions++

                for ((blockPos, parsedExercise) in parsedSession.exercises.withIndex()) {
                    val exerciseId = resolveExercise(parsedExercise.name)
                    val seId = db.sessionDao().insertSessionExercise(
                        SessionExercise(
                            sessionId = sessionId,
                            exerciseId = exerciseId,
                            blockPos = blockPos,
                        )
                    )
                    for ((i, set) in parsedExercise.sets.withIndex()) {
                        db.sessionDao().insertLoggedSet(
                            LoggedSet(
                                sessionExerciseId = seId,
                                position = i,
                                setType = SetType.STANDARD,
                                load = set.loadKg,
                                reps = set.reps,
                                completed = true,
                                restSec = 0,
                                completedAt = end,
                            )
                        )
                        sets++
                    }
                }
            }

            // Programs: skip routines whose name already exists so re-imports don't duplicate them.
            val existingRoutines = db.backupDao().allRoutines()
            val existingRoutineNames = existingRoutines.map { it.name.lowercase() }.toHashSet()
            var nextPosition = (existingRoutines.maxOfOrNull { it.position } ?: -1) + 1
            for (parsedRoutine in parsed.routines) {
                if (parsedRoutine.name.lowercase() in existingRoutineNames) {
                    extraSkips.add("Routine \"${parsedRoutine.name}\" already exists, skipped")
                    continue
                }
                val routineId = db.routineDao().insert(
                    Routine(name = parsedRoutine.name, position = nextPosition++)
                )
                routinesCreated++

                // JEFIT's day list isn't necessarily written in rotation order; sort_order is.
                for ((dayIndex, parsedDay) in parsedRoutine.days.sortedBy { it.sortOrder }.withIndex()) {
                    val dayId = db.routineDao().insertDay(
                        RoutineDay(routineId = routineId, name = parsedDay.name, position = dayIndex)
                    )

                    val blocks = parsedDay.exercises
                        .sortedBy { it.sortOrder }
                        .groupBy { it.supersetKey }
                        .values
                    for ((blockPos, block) in blocks.withIndex()) {
                        for ((inBlockPos, plannedEx) in block.withIndex()) {
                            val exerciseId = resolveExercise(plannedEx.name)
                            val plannedExerciseId = db.routineDao().insertPlannedExercise(
                                PlannedExercise(
                                    dayId = dayId,
                                    exerciseId = exerciseId,
                                    blockPos = blockPos,
                                    inBlockPos = inBlockPos,
                                )
                            )
                            for ((i, set) in plannedEx.sets.withIndex()) {
                                db.routineDao().insertPlannedSet(
                                    PlannedSet(
                                        plannedExerciseId = plannedExerciseId,
                                        position = i,
                                        setType = SetType.STANDARD,
                                        targetRepsMin = set.reps,
                                        targetLoad = set.loadKg,
                                        restSec = plannedEx.restSec,
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
        return Summary(sessions, sets, created, parsed.skipped + extraSkips, bodyMetrics, routinesCreated)
    }
}
