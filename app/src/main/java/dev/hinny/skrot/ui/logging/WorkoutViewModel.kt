package dev.hinny.skrot.ui.logging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.hinny.skrot.AppContainer
import dev.hinny.skrot.data.model.Exercise
import dev.hinny.skrot.data.model.LoggedSet
import dev.hinny.skrot.data.model.MeasurementType
import dev.hinny.skrot.data.model.PlannedExercise
import dev.hinny.skrot.data.model.PlannedSet
import dev.hinny.skrot.data.model.ScheduleMode
import dev.hinny.skrot.data.model.SessionExercise
import dev.hinny.skrot.data.model.SessionExerciseWithDetails
import dev.hinny.skrot.data.model.SessionWithContent
import dev.hinny.skrot.data.model.SetType
import dev.hinny.skrot.domain.CoachEngine
import dev.hinny.skrot.domain.CoachTrigger
import dev.hinny.skrot.domain.PrDetector
import dev.hinny.skrot.domain.PrType
import dev.hinny.skrot.domain.ProgressionEngine
import dev.hinny.skrot.domain.ProgressionSuggestion
import dev.hinny.skrot.domain.ScheduleEngine
import dev.hinny.skrot.domain.SetRecord
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class WorkoutEvent {
    data class Pr(val exerciseName: String, val types: List<PrType>) : WorkoutEvent()
    data class Coach(val trigger: CoachTrigger) : WorkoutEvent()
    data class Finished(val sessionId: Long) : WorkoutEvent()
}

class WorkoutViewModel(
    private val container: AppContainer,
    private val sessionId: Long,
) : ViewModel() {
    private val db = container.db

    val session = MutableStateFlow<SessionWithContent?>(null)
    val plannedSetsByPe = MutableStateFlow<Map<Long, List<PlannedSet>>>(emptyMap())
    val suggestions = MutableStateFlow<Map<Long, ProgressionSuggestion>>(emptyMap())
    val groupOptions = MutableStateFlow<Map<Long, List<Exercise>>>(emptyMap())
    val events = MutableSharedFlow<WorkoutEvent>(extraBufferCapacity = 8)

    private var coach: CoachEngine? = null
    private var welcomeChecked = false
    private var lastExerciseAnnounced = false
    private val dismissedSuggestions = mutableSetOf<Long>()

    init {
        viewModelScope.launch {
            db.sessionDao().observeSessionWithContent(sessionId).collect { content ->
                session.value = content
                if (content != null) {
                    refreshAuxiliary(content)
                    if (!welcomeChecked) {
                        welcomeChecked = true
                        checkWelcomeBack(content)
                    }
                }
            }
        }
    }

    private suspend fun coachEngine(): CoachEngine? {
        val settings = container.settings.settings.first()
        if (!settings.coachEnabled) return null
        return coach ?: CoachEngine(settings.coachFrequency).also { coach = it }
    }

    private suspend fun checkWelcomeBack(content: SessionWithContent) {
        val settings = container.settings.settings.first()
        val previous = db.sessionDao().lastFinishedSessionDate() ?: return
        val days = (content.session.startedAt - previous) / 86_400_000L
        if (days >= settings.comebackDays) {
            coachEngine()?.let { if (it.offer(CoachTrigger.WELCOME_BACK)) emit(CoachTrigger.WELCOME_BACK) }
        }
    }

    private suspend fun emit(trigger: CoachTrigger) {
        events.emit(WorkoutEvent.Coach(trigger))
    }

    /** Loads planned targets, progression suggestions, and swap options. */
    private suspend fun refreshAuxiliary(content: SessionWithContent) {
        val settings = container.settings.settings.first()
        val allExercises = db.exerciseDao().getAll()

        val planned = mutableMapOf<Long, List<PlannedSet>>()
        val suggestionMap = mutableMapOf<Long, ProgressionSuggestion>()
        val options = mutableMapOf<Long, List<Exercise>>()

        for (se in content.exercises) {
            val exercise = se.exercise
            options[se.sessionExercise.id] = exercise.groupId
                ?.let { gid -> allExercises.filter { it.groupId == gid && it.id != exercise.id } }
                ?: emptyList()

            val peId = se.sessionExercise.plannedExerciseId
            val plannedSets = peId?.let { db.routineDao().plannedSets(it) } ?: emptyList()
            if (peId != null) planned[peId] = plannedSets

            if (se.sessionExercise.id in dismissedSuggestions) continue
            if (plannedSets.isEmpty()) continue
            if (se.sets.any { it.completed }) continue

            val historyGym =
                if (exercise.measurementType == MeasurementType.MACHINE_LEVEL) content.session.gymId
                else null
            val lastSessionId = db.sessionDao().lastSessionIdWithExercise(
                exercise.id, content.session.startedAt, historyGym,
            )
            val lastSets = lastSessionId
                ?.let { db.sessionDao().completedSetsInSession(it, exercise.id) }
                ?: continue
            val suggestion = ProgressionEngine.suggest(
                measurement = exercise.measurementType,
                lastSessionSets = lastSets,
                plannedSets = plannedSets,
                incrementKg = settings.progressionIncrementKg,
                incrementLevel = settings.progressionIncrementLevel,
                exerciseIncrementOverride = exercise.progressionIncrement,
            )
            if (suggestion != null) suggestionMap[se.sessionExercise.id] = suggestion
        }
        plannedSetsByPe.value = planned
        suggestions.value = suggestionMap
        groupOptions.value = options
    }

    private suspend fun touch() {
        db.sessionDao().touch(sessionId, System.currentTimeMillis())
    }

    fun updateSetValues(set: LoggedSet, load: Double, reps: Int) {
        viewModelScope.launch {
            db.sessionDao().updateLoggedSet(set.copy(load = load, reps = reps))
            touch()
        }
    }

    fun completeSet(se: SessionExerciseWithDetails, set: LoggedSet, load: Double, reps: Int) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            db.sessionDao().updateLoggedSet(
                set.copy(load = load, reps = reps, completed = true, completedAt = now)
            )
            touch()

            if (set.restSec > 0) {
                container.restTimer.start(set.restSec, se.exercise.nameEn)
            }

            // PR detection (warmups excluded; machine levels scoped to this gym)
            val gymId = session.value?.session?.gymId
            val history = db.sessionDao().setsForExercise(se.exercise.id)
                .filter { it.sessionId != sessionId }
                .map { SetRecord(it.set.load, it.set.reps, it.set.setType, it.sessionGymId) }
            val prs = PrDetector.detect(
                se.exercise.measurementType,
                SetRecord(load, reps, set.setType, gymId),
                history,
                gymId,
            )
            if (prs.isNotEmpty()) {
                events.emit(WorkoutEvent.Pr(se.exercise.nameEn, prs))
            }

            checkCoachAfterCompletion(se)
        }
    }

    private suspend fun checkCoachAfterCompletion(se: SessionExerciseWithDetails) {
        val engine = coachEngine() ?: return
        val content = session.value ?: return
        val lastBlock = content.blocks.lastOrNull() ?: return
        if (!lastExerciseAnnounced &&
            lastBlock.any { it.sessionExercise.id == se.sessionExercise.id }
        ) {
            lastExerciseAnnounced = true
            if (engine.offer(CoachTrigger.LAST_EXERCISE)) emit(CoachTrigger.LAST_EXERCISE)
        }

        // "Hit the target now and it's a PR": check the next planned set of this exercise.
        val nextSet = se.sortedSets.firstOrNull { !it.completed && it.setType != SetType.WARMUP }
        if (nextSet != null) {
            val target = se.sessionExercise.plannedExerciseId
                ?.let { plannedSetsByPe.value[it] }
                ?.find { it.position == nextSet.position }
                ?.let { it.targetRepsMax ?: it.targetRepsMin }
            val gymId = content.session.gymId
            val history = db.sessionDao().setsForExercise(se.exercise.id)
                .filter { it.sessionId != sessionId }
                .map { SetRecord(it.set.load, it.set.reps, it.set.setType, it.sessionGymId) }
            val wouldBePr = PrDetector.detect(
                se.exercise.measurementType,
                SetRecord(nextSet.load, target ?: nextSet.reps, nextSet.setType, gymId),
                history,
                gymId,
            ).isNotEmpty()
            if (wouldBePr && engine.offer(CoachTrigger.PR_CHANCE)) emit(CoachTrigger.PR_CHANCE)
        }
    }

    /** A streak of consecutive training weeks (including this one) triggers praise at finish. */
    private suspend fun checkStreak() {
        val engine = coachEngine() ?: return
        val dates = db.sessionDao().observeSessionDates(0).first()
        if (dates.isEmpty()) return
        val zone = java.time.ZoneId.systemDefault()
        val weekFields = java.time.temporal.WeekFields.ISO
        val weeks = dates.map {
            val d = java.time.Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
            d.get(weekFields.weekBasedYear()) * 100 + d.get(weekFields.weekOfWeekBasedYear())
        }.toSet()
        val today = java.time.LocalDate.now()
        var streak = 0
        var cursor = today
        while (cursor.get(weekFields.weekBasedYear()) * 100 +
            cursor.get(weekFields.weekOfWeekBasedYear()) in weeks
        ) {
            streak++
            cursor = cursor.minusWeeks(1)
        }
        if (streak >= STREAK_WEEKS && engine.offer(CoachTrigger.STREAK)) emit(CoachTrigger.STREAK)
    }

    fun uncompleteSet(set: LoggedSet) {
        viewModelScope.launch {
            db.sessionDao().updateLoggedSet(set.copy(completed = false, completedAt = null))
            touch()
        }
    }

    fun setSetType(set: LoggedSet, type: SetType) {
        viewModelScope.launch {
            db.sessionDao().updateLoggedSet(set.copy(setType = type))
            touch()
        }
    }

    fun addSet(se: SessionExerciseWithDetails, type: SetType = SetType.STANDARD, afterSet: LoggedSet? = null) {
        viewModelScope.launch {
            val settings = container.settings.settings.first()
            val sets = se.sortedSets
            val template = afterSet ?: sets.lastOrNull()
            val position = (afterSet?.position ?: sets.lastOrNull()?.position ?: -1) + 1
            // shift positions of later sets
            sets.filter { it.position >= position }.forEach {
                db.sessionDao().updateLoggedSet(it.copy(position = it.position + 1))
            }
            db.sessionDao().insertLoggedSet(
                LoggedSet(
                    sessionExerciseId = se.sessionExercise.id,
                    position = position,
                    setType = type,
                    load = template?.load ?: 0.0,
                    reps = if (type == SetType.DROP_SET) 0 else template?.reps ?: 0,
                    restSec = template?.restSec ?: settings.defaultRestSec,
                )
            )
            touch()
        }
    }

    /** Removes a set from this session only and compacts the positions after it. */
    fun removeSet(se: SessionExerciseWithDetails, set: LoggedSet) {
        viewModelScope.launch {
            db.sessionDao().deleteLoggedSet(set)
            se.sortedSets.filter { it.position > set.position }.forEach {
                db.sessionDao().updateLoggedSet(it.copy(position = it.position - 1))
            }
            touch()
        }
    }

    /**
     * Rest-duration edits apply to this session; [applyToPlan] additionally
     * writes them back to the routine ("apply to future sessions").
     */
    fun updateRest(
        se: SessionExerciseWithDetails,
        set: LoggedSet,
        restSec: Int,
        applyToPlan: Boolean = false,
    ) {
        viewModelScope.launch {
            db.sessionDao().updateLoggedSet(set.copy(restSec = restSec))
            if (applyToPlan) {
                se.sessionExercise.plannedExerciseId?.let { peId ->
                    db.routineDao().writeBackRest(peId, set.position, restSec)
                }
            }
            touch()
        }
    }

    /**
     * "Apply to future sessions" after adding/removing sets: syncs the planned
     * set count of this exercise to the session's current set count. Existing
     * planned targets are kept; new planned sets copy the last one's targets.
     */
    fun applySetsToPlan(se: SessionExerciseWithDetails) {
        viewModelScope.launch {
            val peId = se.sessionExercise.plannedExerciseId ?: return@launch
            val planned = db.routineDao().plannedSets(peId)
            val sessionSets = se.sortedSets
            planned.drop(sessionSets.size).forEach { db.routineDao().deletePlannedSet(it) }
            val template = planned.lastOrNull()
            for (i in planned.size until sessionSets.size) {
                val s = sessionSets[i]
                db.routineDao().insertPlannedSet(
                    PlannedSet(
                        plannedExerciseId = peId,
                        position = i,
                        setType = s.setType,
                        targetRepsMin = template?.targetRepsMin,
                        targetRepsMax = template?.targetRepsMax,
                        targetLoad = template?.targetLoad,
                        restSec = s.restSec,
                    )
                )
            }
            session.value?.let { refreshAuxiliary(it) }
        }
    }

    /**
     * "Apply to future sessions" for an exercise added during the session:
     * plans it into the routine day this session was started from.
     */
    fun addExerciseToPlan(se: SessionExerciseWithDetails) {
        viewModelScope.launch {
            if (se.sessionExercise.plannedExerciseId != null) return@launch
            val dayId = session.value?.session?.routineDayId ?: return@launch
            val day = db.routineDao().dayWithContent(dayId) ?: return@launch
            val blockPos =
                (day.blocks.flatten().maxOfOrNull { it.planned.blockPos } ?: -1) + 1
            val peId = db.routineDao().insertPlannedExercise(
                PlannedExercise(dayId = dayId, exerciseId = se.exercise.id, blockPos = blockPos)
            )
            se.sortedSets.forEachIndexed { i, s ->
                db.routineDao().insertPlannedSet(
                    PlannedSet(
                        plannedExerciseId = peId,
                        position = i,
                        setType = s.setType,
                        restSec = s.restSec,
                    )
                )
            }
            db.sessionDao().updateSessionExercise(
                se.sessionExercise.copy(plannedExerciseId = peId)
            )
            session.value?.let { refreshAuxiliary(it) }
        }
    }

    /** "Apply to future sessions" after removing a planned exercise from the session. */
    fun deletePlannedExercise(peId: Long) {
        viewModelScope.launch {
            db.routineDao().plannedExerciseById(peId)?.let {
                db.routineDao().deletePlannedExercise(it)
            }
        }
    }

    /** Target-reps edits persist back to the routine, like rest durations. */
    fun updateTarget(se: SessionExerciseWithDetails, set: LoggedSet, min: Int?, max: Int?) {
        viewModelScope.launch {
            se.sessionExercise.plannedExerciseId?.let { peId ->
                db.routineDao().writeBackTarget(peId, set.position, min, max)
                val content = session.value ?: return@let
                refreshAuxiliary(content)
            }
            touch()
        }
    }

    fun acceptSuggestion(se: SessionExerciseWithDetails, suggestion: ProgressionSuggestion) {
        viewModelScope.launch {
            for (set in se.sets.filter { !it.completed && it.setType == SetType.STANDARD }) {
                when (suggestion) {
                    is ProgressionSuggestion.IncreaseLoad ->
                        db.sessionDao().updateLoggedSet(set.copy(load = suggestion.toLoad))

                    is ProgressionSuggestion.AddRep ->
                        db.sessionDao().updateLoggedSet(set.copy(reps = suggestion.toReps))
                }
            }
            dismissedSuggestions.add(se.sessionExercise.id)
            suggestions.value -= se.sessionExercise.id
            touch()
        }
    }

    fun dismissSuggestion(seId: Long) {
        dismissedSuggestions.add(seId)
        suggestions.value -= seId
    }

    fun swapExercise(se: SessionExerciseWithDetails, to: Exercise) {
        viewModelScope.launch {
            db.sessionDao().updateSessionExercise(se.sessionExercise.copy(exerciseId = to.id))
            touch()
        }
    }

    fun addExercise(exercise: Exercise) {
        viewModelScope.launch {
            val settings = container.settings.settings.first()
            val content = session.value
            val blockPos = (content?.exercises?.maxOfOrNull { it.sessionExercise.blockPos } ?: -1) + 1
            val seId = db.sessionDao().insertSessionExercise(
                SessionExercise(sessionId = sessionId, exerciseId = exercise.id, blockPos = blockPos)
            )
            repeat(3) { i ->
                db.sessionDao().insertLoggedSet(
                    LoggedSet(
                        sessionExerciseId = seId,
                        position = i,
                        restSec = settings.defaultRestSec,
                    )
                )
            }
            touch()
        }
    }

    fun removeExercise(se: SessionExerciseWithDetails) {
        viewModelScope.launch {
            db.sessionDao().deleteSessionExercise(se.sessionExercise)
            touch()
        }
    }

    fun setExerciseNote(se: SessionExerciseWithDetails, note: String) {
        viewModelScope.launch {
            db.sessionDao().updateSessionExercise(se.sessionExercise.copy(note = note))
        }
    }

    fun setNextTimeNote(exercise: Exercise, note: String) {
        viewModelScope.launch {
            db.exerciseDao().setNextTimeNote(exercise.id, note)
            session.value?.let { refreshAuxiliary(it) }
        }
    }

    fun setSessionNote(note: String) {
        viewModelScope.launch {
            val current = db.sessionDao().sessionById(sessionId) ?: return@launch
            db.sessionDao().updateSession(current.copy(note = note))
        }
    }

    fun finish() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            container.restTimer.skip()
            db.sessionDao().finish(sessionId, now)

            // Advance the rotating sequence for the session's routine.
            val current = db.sessionDao().sessionById(sessionId)
            val routineId = current?.routineId
            if (routineId != null) {
                val routineWithDays = db.routineDao().withDays(routineId)
                if (routineWithDays != null &&
                    routineWithDays.routine.scheduleMode == ScheduleMode.ROTATING
                ) {
                    val settings = container.settings.settings.first()
                    val newIndex = ScheduleEngine.indexAfterCompletion(
                        routineWithDays.routine,
                        routineWithDays.days,
                        current.routineDayId,
                        settings.swapBehavior,
                    )
                    db.routineDao().setNextDayIndex(routineId, newIndex)
                }
            }
            coachEngine()?.let { if (it.offer(CoachTrigger.SESSION_DONE)) emit(CoachTrigger.SESSION_DONE) }
            checkStreak()
            events.emit(WorkoutEvent.Finished(sessionId))
        }
    }

    fun discard() {
        viewModelScope.launch {
            container.restTimer.skip()
            db.sessionDao().deleteSession(sessionId)
        }
    }

    /** Periodic idle check for the coach ("time to focus"). */
    fun onIdleTick() {
        viewModelScope.launch {
            val engine = coachEngine() ?: return@launch
            val content = session.value ?: return@launch
            val lastCompleted = content.exercises
                .flatMap { it.sets }
                .mapNotNull { it.completedAt }
                .maxOrNull() ?: return@launch
            if (System.currentTimeMillis() - lastCompleted >= IDLE_THRESHOLD_MS &&
                engine.offer(CoachTrigger.IDLE)
            ) {
                emit(CoachTrigger.IDLE)
            }
        }
    }

    companion object {
        const val IDLE_THRESHOLD_MS = 5 * 60_000L
        const val STREAK_WEEKS = 3
    }
}
