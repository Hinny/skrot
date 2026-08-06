package dev.hinny.skrot.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.hinny.skrot.AppContainer
import dev.hinny.skrot.data.model.Exercise
import dev.hinny.skrot.data.model.ExerciseGroup
import dev.hinny.skrot.data.model.Gym
import dev.hinny.skrot.data.model.GymExercise
import dev.hinny.skrot.data.model.GymOverride
import dev.hinny.skrot.data.model.LoggedSet
import dev.hinny.skrot.data.model.MeasurementType
import dev.hinny.skrot.data.model.PlannedExerciseWithDetails
import dev.hinny.skrot.data.model.PrefillMode
import dev.hinny.skrot.data.model.SessionExercise
import dev.hinny.skrot.data.model.SetType
import dev.hinny.skrot.data.model.WorkoutSession
import dev.hinny.skrot.domain.GymResolution
import dev.hinny.skrot.domain.GymResolver
import dev.hinny.skrot.domain.PrefillEngine
import kotlinx.coroutines.launch

/** One planned exercise checked against the selected gym. */
data class StartItem(
    val planned: PlannedExerciseWithDetails,
    val resolution: GymResolution,
    /** Options shown in the picker (group equivalents). */
    val options: List<Exercise>,
)

data class PendingStart(
    val routineId: Long?,
    val dayId: Long?,
    val gymId: Long?,
    val temporaryVisit: Boolean,
    val prefillMode: PrefillMode,
    val items: List<StartItem>,
    /** The whole library, so any exercise can be swapped in from the dialog. */
    val allExercises: List<Exercise> = emptyList(),
    /** Marked as available at the chosen gym; empty for a temporary visit. */
    val availableExerciseIds: Set<Long> = emptySet(),
)

/**
 * Turning a program day into a workout: resolving what the chosen gym actually
 * has, remembering the answers, and writing the session with its sets
 * pre-filled.
 *
 * This is the app's most intricate piece of business logic and it belongs to
 * neither of the two screens that trigger it — Home and the Session tab both
 * start workouts, and both used to reach into HomeViewModel for it.
 */
class StartSessionViewModel(private val container: AppContainer) : ViewModel() {
    private val db = container.db

    /** Checks each planned exercise against the gym; the UI asks the user where needed. */
    suspend fun prepareStart(
        routineId: Long?,
        dayId: Long?,
        gymId: Long?,
        temporaryVisit: Boolean,
    ): PendingStart {
        val routine = routineId?.let { db.routineDao().byId(it) }
        val prefillMode = routine?.prefillMode ?: PrefillMode.LAST_SESSION
        val content = dayId?.let { db.routineDao().dayWithContent(it) }
        val plannedList = content?.blocks?.flatten() ?: emptyList()
        val library = container.exercisesNow()
        val allExercises = library.associateBy { it.id }
        // Hoisted out of the loop: these were being re-queried per exercise.
        val availableIds =
            if (gymId == null || temporaryVisit) emptySet()
            else db.gymDao().exerciseIdsAt(gymId).toSet()
        val overrides =
            if (gymId == null || temporaryVisit) emptyList() else db.gymDao().overridesAt(gymId)

        val items = plannedList.map { planned ->
            val exercise = planned.exercise
            val groupMembers = exercise.groupId
                ?.let { gid -> allExercises.values.filter { it.groupId == gid } }
                ?: listOf(exercise)

            if (temporaryVisit) {
                // Somewhere unfamiliar: assume nothing is there. Every exercise
                // gets confirmed, even when it has no group to swap within.
                val options = groupMembers.filter { it.id != exercise.id }
                StartItem(
                    planned = planned,
                    resolution = GymResolution.Choice(options),
                    options = options,
                )
            } else if (gymId == null) {
                StartItem(planned, GymResolution.Available, emptyList())
            } else {
                val override = overrides
                    .find { it.plannedExerciseId == planned.planned.id }
                    ?.let { allExercises[it.exerciseId] }
                val resolution =
                    GymResolver.resolve(exercise, availableIds, override, groupMembers)
                val options = when (resolution) {
                    is GymResolution.Choice -> resolution.options
                    is GymResolution.AutoSwapped -> listOf(resolution.to)
                    else -> emptyList()
                }
                StartItem(planned, resolution, options)
            }
        }
        return PendingStart(
            routineId = routineId,
            dayId = dayId,
            gymId = gymId,
            temporaryVisit = temporaryVisit,
            prefillMode = prefillMode,
            items = items,
            allExercises = library,
            availableExerciseIds = availableIds,
        )
    }

    /** Marks [exerciseId] as available at [gymId], as the gym editor would. */
    fun addExerciseToGym(gymId: Long, exerciseId: Long) {
        viewModelScope.launch {
            db.gymDao().addExercise(GymExercise(gymId = gymId, exerciseId = exerciseId))
        }
    }

    /**
     * Records [picked] as interchangeable with [original], so a gym without the
     * original offers it automatically next time. Joins the original's group, or
     * starts one named after it.
     *
     * Grouping is a statement about your own training, not an edit to the
     * library's definition of an exercise, so this is allowed for built-in
     * exercises too — unlike renaming one.
     */
    fun linkAsEquivalent(original: Exercise, picked: Exercise) {
        viewModelScope.launch {
            val groupId = original.groupId ?: db.exerciseDao().insertGroup(
                ExerciseGroup(
                    nameEn = original.nameEn,
                    nameSv = original.nameSv,
                    isCustom = true,
                )
            ).also { newGroup ->
                db.exerciseDao().update(original.copy(groupId = newGroup))
            }
            db.exerciseDao().update(picked.copy(groupId = groupId))
        }
    }

    /**
     * Creates the session with resolved exercises and pre-filled sets.
     *
     * @param picks plannedExerciseId -> chosen exercise id (null = skip the exercise)
     * @param alwaysUse plannedExerciseIds whose pick should persist as a per-gym override
     */
    suspend fun startSession(
        pending: PendingStart,
        picks: Map<Long, Long?>,
        alwaysUse: Set<Long>,
    ): Long {
        val now = System.currentTimeMillis()
        val settings = container.settingsNow()
        val sessionId = db.sessionDao().insertSession(
            WorkoutSession(
                startedAt = now,
                routineId = pending.routineId,
                routineDayId = pending.dayId,
                gymId = pending.gymId,
                lastActivityAt = now,
                temporaryVisit = pending.temporaryVisit,
                locked = settings.sessionsLockedByDefault,
            )
        )

        for (item in pending.items) {
            val plannedId = item.planned.planned.id
            val resolution = item.resolution
            val chosenId: Long? = when {
                picks.containsKey(plannedId) -> picks[plannedId]
                resolution is GymResolution.AutoSwapped -> resolution.to.id
                else -> item.planned.exercise.id
            }
            if (chosenId == null) continue // skipped
            val exercise = db.exerciseDao().byId(chosenId) ?: continue

            if (chosenId != item.planned.exercise.id &&
                plannedId in alwaysUse && pending.gymId != null && !pending.temporaryVisit
            ) {
                db.gymDao().setOverride(GymOverride(pending.gymId, plannedId, chosenId))
            }

            val seId = db.sessionDao().insertSessionExercise(
                SessionExercise(
                    sessionId = sessionId,
                    exerciseId = chosenId,
                    plannedExerciseId = plannedId,
                    blockPos = item.planned.planned.blockPos,
                    inBlockPos = item.planned.planned.inBlockPos,
                )
            )

            // Machine levels aren't comparable across gyms: last-session lookup is per-gym.
            val historyGym =
                if (exercise.measurementType == MeasurementType.MACHINE_LEVEL) pending.gymId
                else null
            val lastSessionId =
                db.sessionDao().lastSessionIdWithExercise(chosenId, now, historyGym)
            val lastSets = lastSessionId
                ?.let { db.sessionDao().completedSetsInSession(it, chosenId) }
                ?: emptyList()

            val plannedSets = item.planned.sortedSets
            val typeCounters = mutableMapOf<SetType, Int>()
            for ((index, plannedSet) in plannedSets.withIndex()) {
                val typeIndex = typeCounters.getOrDefault(plannedSet.setType, 0)
                typeCounters[plannedSet.setType] = typeIndex + 1
                val prefill = PrefillEngine.prefill(
                    pending.prefillMode, plannedSet, lastSets, typeIndex, plannedSet.setType,
                )
                db.sessionDao().insertLoggedSet(
                    LoggedSet(
                        sessionExerciseId = seId,
                        position = index,
                        setType = plannedSet.setType,
                        load = prefill.load ?: 0.0,
                        reps = prefill.reps ?: 0,
                        completed = false,
                        restSec = plannedSet.restSec,
                    )
                )
            }
            if (plannedSets.isEmpty()) {
                db.sessionDao().insertLoggedSet(
                    LoggedSet(sessionExerciseId = seId, restSec = settings.defaultRestSec)
                )
            }
        }
        return sessionId
    }

    /** Creates a gym inline from the start dialog and hands back its id. */
    fun createGym(name: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            onCreated(db.gymDao().insert(Gym(name = name)))
        }
    }

    suspend fun startEmptySession(gymId: Long?): Long {
        val now = System.currentTimeMillis()
        val locked = container.settingsNow().sessionsLockedByDefault
        return db.sessionDao().insertSession(
            WorkoutSession(startedAt = now, gymId = gymId, lastActivityAt = now, locked = locked)
        )
    }
}
