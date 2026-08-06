package dev.hinny.skrot.ui.logging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.hinny.skrot.AppContainer
import dev.hinny.skrot.data.model.Exercise
import dev.hinny.skrot.data.model.GymOverride
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
import dev.hinny.skrot.domain.StreakCalculator
import dev.hinny.skrot.domain.WarmupGenerator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
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

    /**
     * Completed sets of the previous session that included each exercise, keyed
     * by session-exercise id. What you are trying to beat, shown on the row.
     */
    val lastSessionSets = MutableStateFlow<Map<Long, List<LoggedSet>>>(emptyMap())
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
        // Ordered by the exercise-order setting, so the swap options a session
        // offers match every other exercise list in the app.
        val allExercises = container.exercisesNow()

        val planned = mutableMapOf<Long, List<PlannedSet>>()
        val suggestionMap = mutableMapOf<Long, ProgressionSuggestion>()
        val options = mutableMapOf<Long, List<Exercise>>()
        val previous = mutableMapOf<Long, List<LoggedSet>>()

        for (se in content.exercises) {
            val exercise = se.exercise
            options[se.sessionExercise.id] = exercise.groupId
                ?.let { gid -> allExercises.filter { it.groupId == gid && it.id != exercise.id } }
                ?: emptyList()

            val peId = se.sessionExercise.plannedExerciseId
            val plannedSets = peId?.let { db.routineDao().plannedSets(it) } ?: emptyList()
            if (peId != null) planned[peId] = plannedSets

            // Machine levels aren't comparable across gyms, so the lookup is
            // per-gym for them — the same rule the prefill follows.
            val historyGym =
                if (exercise.measurementType == MeasurementType.MACHINE_LEVEL) content.session.gymId
                else null
            val lastSessionId = db.sessionDao().lastSessionIdWithExercise(
                exercise.id, content.session.startedAt, historyGym,
            )
            val lastSets = lastSessionId
                ?.let { db.sessionDao().completedSetsInSession(it, exercise.id) }
            if (lastSets != null) previous[se.sessionExercise.id] = lastSets

            if (se.sessionExercise.id in dismissedSuggestions) continue
            if (plannedSets.isEmpty()) continue
            if (se.sets.any { it.completed }) continue
            if (lastSets == null) continue

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
        lastSessionSets.value = previous
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
                container.restTimer.start(set.restSec, se.exercise.nameEn, sessionId)
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
                ?.targetRepsMin
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

    /**
     * A streak of consecutive training weeks (including this one) triggers
     * praise at finish. The streak is the same one Home and Stats show — same
     * calculator, same per-week quota — so all three agree on what a week
     * counts for.
     */
    private suspend fun checkStreak() {
        val engine = coachEngine() ?: return
        val dates = db.sessionDao().observeSessionDates(0).first()
        val minPerWeek = container.settings.settings.first().streakMinPerWeek
        val streak = StreakCalculator.weeks(dates, minPerWeek)
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

    /**
     * Builds a warm-up ramp in front of the first working set, using that set's
     * load. Existing warmups are left alone — the action adds to what is there
     * rather than rewriting it, so running it twice is visible, not silent.
     *
     * @return how many sets were created; 0 means there was nothing to ramp to
     */
    suspend fun addWarmupSets(se: SessionExerciseWithDetails): Int {
        val settings = container.settings.settings.first()
        val sets = se.sortedSets
        val working = sets.firstOrNull { it.setType != SetType.WARMUP } ?: return 0
        val rounding = when (se.exercise.measurementType) {
            MeasurementType.MACHINE_LEVEL ->
                se.exercise.progressionIncrement ?: settings.progressionIncrementLevel

            else -> se.exercise.progressionIncrement ?: settings.progressionIncrementKg
        }
        val warmups = WarmupGenerator.generate(
            workingLoad = working.load,
            count = settings.warmupSetCount,
            rounding = rounding,
        )
        if (warmups.isEmpty()) return 0

        // The ramp goes in front of everything, so every existing set shifts by
        // the number of rungs added.
        sets.forEach { db.sessionDao().updateLoggedSet(it.copy(position = it.position + warmups.size)) }
        warmups.forEachIndexed { i, warmup ->
            db.sessionDao().insertLoggedSet(
                LoggedSet(
                    sessionExerciseId = se.sessionExercise.id,
                    position = i,
                    setType = SetType.WARMUP,
                    load = warmup.load,
                    reps = warmup.reps,
                    restSec = minOf(working.restSec, WARMUP_REST_CAP_SEC),
                )
            )
        }
        touch()
        return warmups.size
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

    /** Reorders a set within its exercise. */
    fun moveSet(seId: Long, from: Int, to: Int) {
        viewModelScope.launch {
            val sets = session.value?.exercises
                ?.find { it.sessionExercise.id == seId }
                ?.sortedSets
                ?: return@launch
            if (from !in sets.indices || to !in sets.indices || from == to) return@launch
            val reordered = sets.toMutableList()
            reordered.add(to, reordered.removeAt(from))
            reordered.forEachIndexed { newPos, s ->
                if (s.position != newPos) db.sessionDao().updateLoggedSet(s.copy(position = newPos))
            }
            touch()
        }
    }

    /** Reorders whole blocks (an exercise, or a superset) within the session. */
    fun moveBlock(from: Int, to: Int) {
        viewModelScope.launch {
            val blocks = session.value?.blocks ?: return@launch
            if (from !in blocks.indices || to !in blocks.indices || from == to) return@launch
            val reordered = blocks.toMutableList()
            reordered.add(to, reordered.removeAt(from))
            reordered.forEachIndexed { newPos, block ->
                block.forEach { se ->
                    if (se.sessionExercise.blockPos != newPos) {
                        db.sessionDao().updateSessionExercise(
                            se.sessionExercise.copy(blockPos = newPos)
                        )
                    }
                }
            }
            touch()
        }
    }

    /** Reorders the exercises inside one superset block. */
    fun moveExerciseInBlock(blockIndex: Int, from: Int, to: Int) {
        viewModelScope.launch {
            val block = session.value?.blocks?.getOrNull(blockIndex) ?: return@launch
            if (from !in block.indices || to !in block.indices || from == to) return@launch
            val reordered = block.toMutableList()
            reordered.add(to, reordered.removeAt(from))
            reordered.forEachIndexed { newPos, se ->
                if (se.sessionExercise.inBlockPos != newPos) {
                    db.sessionDao().updateSessionExercise(
                        se.sessionExercise.copy(inBlockPos = newPos)
                    )
                }
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

    /**
     * Rewrites the routine day this session started from so it matches what
     * actually happened: exercise list, order, supersets and set counts.
     *
     * Planned exercises are updated in place rather than rebuilt, so their ids
     * survive and per-gym overrides pointing at them are not cascaded away.
     * Targets and target loads already planned stay put — this syncs structure,
     * not the numbers you lifted today.
     */
    fun applySessionToPlan() {
        viewModelScope.launch {
            val content = session.value ?: return@launch
            val dayId = content.session.routineDayId ?: return@launch
            val dao = db.routineDao()
            val existing = dao.dayWithContent(dayId) ?: return@launch
            val existingIds = existing.exercises.map { it.planned.id }.toSet()
            val kept = mutableSetOf<Long>()

            content.blocks.forEachIndexed { blockIndex, block ->
                block.forEachIndexed { inBlockIndex, se ->
                    val existingId = se.sessionExercise.plannedExerciseId
                        ?.takeIf { it in existingIds }
                    val peId = if (existingId != null) {
                        dao.plannedExerciseById(existingId)?.let {
                            dao.updatePlannedExercise(
                                it.copy(
                                    exerciseId = se.exercise.id,
                                    blockPos = blockIndex,
                                    inBlockPos = inBlockIndex,
                                )
                            )
                        }
                        existingId
                    } else {
                        val newId = dao.insertPlannedExercise(
                            PlannedExercise(
                                dayId = dayId,
                                exerciseId = se.exercise.id,
                                blockPos = blockIndex,
                                inBlockPos = inBlockIndex,
                            )
                        )
                        db.sessionDao().updateSessionExercise(
                            se.sessionExercise.copy(plannedExerciseId = newId)
                        )
                        newId
                    }
                    kept += peId
                    syncPlannedSets(peId, se)
                }
            }

            existing.exercises
                .filter { it.planned.id !in kept }
                .forEach { dao.deletePlannedExercise(it.planned) }

            session.value?.let { refreshAuxiliary(it) }
        }
    }

    /** Matches the planned set list of [peId] to the sets actually in the session. */
    private suspend fun syncPlannedSets(peId: Long, se: SessionExerciseWithDetails) {
        val dao = db.routineDao()
        val planned = dao.plannedSets(peId)
        val sessionSets = se.sortedSets
        planned.drop(sessionSets.size).forEach { dao.deletePlannedSet(it) }
        sessionSets.forEachIndexed { i, s ->
            val current = planned.getOrNull(i)
            if (current == null) {
                val template = planned.lastOrNull()
                dao.insertPlannedSet(
                    PlannedSet(
                        plannedExerciseId = peId,
                        position = i,
                        setType = s.setType,
                        targetRepsMin = template?.targetRepsMin,
                        targetLoad = template?.targetLoad,
                        restSec = s.restSec,
                    )
                )
            } else {
                dao.updatePlannedSet(current.copy(setType = s.setType, restSec = s.restSec))
            }
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

    /**
     * Target-reps edits persist back to the routine, like rest durations. An
     * exercise with no plan behind it keeps its target on the logged set, so it
     * stays editable rather than showing a dead "—".
     */
    fun updateTarget(se: SessionExerciseWithDetails, set: LoggedSet, reps: Int?) {
        viewModelScope.launch {
            val peId = se.sessionExercise.plannedExerciseId
            if (peId != null) {
                db.routineDao().writeBackTarget(peId, set.position, reps)
                session.value?.let { refreshAuxiliary(it) }
            } else {
                db.sessionDao().updateLoggedSet(set.copy(targetReps = reps))
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

    /**
     * Swaps the exercise for this session. The swap is session-only unless asked
     * to persist: [applyToPlan] rewrites the routine's planned exercise, and
     * [alwaysAtGym] records it as a per-gym override instead, which keeps the
     * routine intact and only changes what happens at this gym.
     */
    fun swapExercise(
        se: SessionExerciseWithDetails,
        to: Exercise,
        applyToPlan: Boolean = false,
        alwaysAtGym: Boolean = false,
    ) {
        viewModelScope.launch {
            db.sessionDao().updateSessionExercise(se.sessionExercise.copy(exerciseId = to.id))
            val peId = se.sessionExercise.plannedExerciseId
            if (peId != null) {
                if (applyToPlan) {
                    db.routineDao().plannedExerciseById(peId)?.let {
                        db.routineDao().updatePlannedExercise(it.copy(exerciseId = to.id))
                    }
                }
                val current = session.value?.session
                if (alwaysAtGym && current?.gymId != null && !current.temporaryVisit) {
                    db.gymDao().setOverride(GymOverride(current.gymId, peId, to.id))
                }
            }
            touch()
            session.value?.let { refreshAuxiliary(it) }
        }
    }

    /**
     * Adds an exercise to the session, either as a new block or into an existing
     * one ([intoBlockIndex]), which makes that block a superset.
     */
    fun addExercise(exercise: Exercise, intoBlockIndex: Int? = null) {
        viewModelScope.launch {
            val settings = container.settings.settings.first()
            val content = session.value
            val blockPos: Int
            val inBlockPos: Int
            if (intoBlockIndex != null) {
                val block = content?.blocks?.getOrNull(intoBlockIndex) ?: return@launch
                blockPos = block.first().sessionExercise.blockPos
                inBlockPos = (block.maxOfOrNull { it.sessionExercise.inBlockPos } ?: -1) + 1
            } else {
                blockPos =
                    (content?.exercises?.maxOfOrNull { it.sessionExercise.blockPos } ?: -1) + 1
                inBlockPos = 0
            }
            val seId = db.sessionDao().insertSessionExercise(
                SessionExercise(
                    sessionId = sessionId,
                    exerciseId = exercise.id,
                    blockPos = blockPos,
                    inBlockPos = inBlockPos,
                )
            )
            repeat(NEW_EXERCISE_SETS) { i ->
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

    /** Merges a block into the one before it, creating or extending a superset. */
    fun linkWithPrevious(blockIndex: Int) {
        viewModelScope.launch {
            val blocks = session.value?.blocks ?: return@launch
            if (blockIndex <= 0 || blockIndex >= blocks.size) return@launch
            val previous = blocks[blockIndex - 1]
            val targetBlockPos = previous.first().sessionExercise.blockPos
            val start = (previous.maxOfOrNull { it.sessionExercise.inBlockPos } ?: -1) + 1
            blocks[blockIndex].forEachIndexed { i, se ->
                db.sessionDao().updateSessionExercise(
                    se.sessionExercise.copy(blockPos = targetBlockPos, inBlockPos = start + i)
                )
            }
            touch()
        }
    }

    /** Splits an exercise out of its superset into a block of its own at the end. */
    fun unlink(se: SessionExerciseWithDetails) {
        viewModelScope.launch {
            val content = session.value ?: return@launch
            val maxBlock = content.exercises.maxOfOrNull { it.sessionExercise.blockPos } ?: 0
            db.sessionDao().updateSessionExercise(
                se.sessionExercise.copy(blockPos = maxBlock + 1, inBlockPos = 0)
            )
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

    /** Toggles the session lock; locked sessions block structural edits. */
    fun toggleLock() {
        viewModelScope.launch {
            val locked = session.value?.session?.locked ?: return@launch
            db.sessionDao().setLocked(sessionId, !locked)
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

        /** Sets created for an exercise added mid-session. */
        const val NEW_EXERCISE_SETS = 3

        /** Nobody needs the working-set rest between two warm-up rungs. */
        const val WARMUP_REST_CAP_SEC = 60
    }
}
