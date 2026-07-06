package dev.hinny.skrot.data.backup

import androidx.room.withTransaction
import dev.hinny.skrot.data.db.SkrotDatabase
import dev.hinny.skrot.data.model.BodyMetric
import dev.hinny.skrot.data.model.Equipment
import dev.hinny.skrot.data.model.Exercise
import dev.hinny.skrot.data.model.LoggedSet
import dev.hinny.skrot.data.model.MeasurementType
import dev.hinny.skrot.data.model.MuscleGroup
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
    )

    data class Summary(
        val sessionsCreated: Int,
        val setsCreated: Int,
        val exercisesCreated: Int,
        val skipped: List<String>,
        val bodyMetricsCreated: Int = 0,
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
        for (session in parsed.sessions) {
            for (ex in session.exercises) {
                if (match(ex.name) != null) matched.add(ex.name) else unmatched.add(ex.name)
            }
        }
        return Preview(
            sessionCount = parsed.sessions.size,
            setCount = parsed.totalSets,
            matchedExercises = matched.toList(),
            newExercises = unmatched.toList(),
            skipped = parsed.skipped,
            bodyMetricCount = parsed.bodyMetrics.size,
        )
    }

    suspend fun commit(parsed: JefitCsvParser.Result): Summary {
        val match = buildMatcher()
        var sessions = 0
        var sets = 0
        var created = 0
        var bodyMetrics = 0
        val createdByName = mutableMapOf<String, Long>()
        val zone = ZoneId.systemDefault()

        db.withTransaction {
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
                    val exerciseId = match(parsedExercise.name)?.id
                        ?: createdByName.getOrPut(normalize(parsedExercise.name)) {
                            created++
                            db.exerciseDao().insert(
                                Exercise(
                                    nameEn = parsedExercise.name,
                                    nameSv = parsedExercise.name,
                                    muscleGroup = MuscleGroup.FULL_BODY,
                                    equipment = Equipment.OTHER,
                                    measurementType = MeasurementType.WEIGHT_KG,
                                    isCustom = true,
                                )
                            )
                        }

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
        }
        return Summary(sessions, sets, created, parsed.skipped, bodyMetrics)
    }
}
