package dev.hinny.skrot.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import dev.hinny.skrot.AppContainer
import dev.hinny.skrot.R
import dev.hinny.skrot.data.model.Exercise
import dev.hinny.skrot.data.model.Gym
import dev.hinny.skrot.data.model.GymOverride
import dev.hinny.skrot.data.model.LoggedSet
import dev.hinny.skrot.data.model.MeasurementType
import dev.hinny.skrot.data.model.PlannedExerciseWithDetails
import dev.hinny.skrot.data.model.PrefillMode
import dev.hinny.skrot.data.model.RoutineDay
import dev.hinny.skrot.data.model.RoutineWithDays
import dev.hinny.skrot.data.model.SessionExercise
import dev.hinny.skrot.data.model.WorkoutSession
import dev.hinny.skrot.domain.GymResolution
import dev.hinny.skrot.domain.GymResolver
import dev.hinny.skrot.domain.PrefillEngine
import dev.hinny.skrot.domain.ScheduleEngine
import dev.hinny.skrot.ui.Routes
import dev.hinny.skrot.ui.common.lastPerformedText
import dev.hinny.skrot.ui.common.vector
import dev.hinny.skrot.ui.containerViewModel
import dev.hinny.skrot.ui.session.StartFlowHost
import dev.hinny.skrot.ui.session.WorkoutPickerDialog
import dev.hinny.skrot.data.prefs.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HomeUiState(
    val openSession: WorkoutSession? = null,
    val activeRoutine: RoutineWithDays? = null,
    val nextDay: RoutineDay? = null,
    val allRoutines: List<RoutineWithDays> = emptyList(),
    val lastByRoutine: Map<Long, Long> = emptyMap(),
    val lastByDay: Map<Long, Long> = emptyMap(),
    val gyms: List<Gym> = emptyList(),
    val daysSinceLastSession: Int? = null,
    val comebackRoutines: List<RoutineWithDays> = emptyList(),
    val backupOverdue: Boolean = false,
)

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
)

class HomeViewModel(private val container: AppContainer) : ViewModel() {
    private val db = container.db

    val comebackDismissed = MutableStateFlow(false)
    val backupReminderDismissed = MutableStateFlow(false)
    val uiState = MutableStateFlow(HomeUiState())

    init {
        viewModelScope.launch {
            val base = combine(
                db.sessionDao().observeOpenSession(),
                db.routineDao().observeAllWithDays(),
                db.routineDao().observeLastPerformedByRoutine(),
                db.routineDao().observeLastPerformedByDay(),
                db.gymDao().observeAll(),
            ) { open, routines, lastRoutine, lastDay, gyms ->
                HomeUiState(
                    openSession = open,
                    allRoutines = routines,
                    lastByRoutine = lastRoutine.associate { it.routineId to it.last },
                    lastByDay = lastDay.associate { it.dayId to it.last },
                    gyms = gyms,
                )
            }
            combine(
                base,
                db.sessionDao().observeFinishedSessions(),
                container.settings.settings,
                comebackDismissed,
                backupReminderDismissed,
            ) { state, finished, settings, dismissed, backupDismissed ->
                val active = state.allRoutines.find { it.routine.isActive }
                val nextDay = active?.let {
                    ScheduleEngine.nextDay(it.routine, it.days, LocalDate.now())
                }
                val lastSession = finished.maxOfOrNull { it.startedAt }
                val daysSince = lastSession?.let {
                    ((System.currentTimeMillis() - it) / 86_400_000L).toInt()
                }
                val comeback =
                    if (!dismissed && (daysSince == null || daysSince >= settings.comebackDays)) {
                        state.allRoutines.filter { r ->
                            r.routine.tags.any { it.equals("rebuild", ignoreCase = true) }
                        }
                    } else emptyList()
                // Backup reminder: counts from the last backup, or from the oldest
                // logged session if no backup was ever made.
                val backupBasis = settings.lastBackupAt.takeIf { it > 0 }
                    ?: finished.minOfOrNull { it.startedAt }
                val backupOverdue = !backupDismissed &&
                    settings.backupReminderDays > 0 &&
                    backupBasis != null &&
                    System.currentTimeMillis() - backupBasis >
                    settings.backupReminderDays * 86_400_000L
                state.copy(
                    activeRoutine = active,
                    nextDay = nextDay,
                    daysSinceLastSession = daysSince,
                    comebackRoutines = if (daysSince == null) emptyList() else comeback,
                    backupOverdue = backupOverdue,
                )
            }.collect { uiState.value = it }
        }
    }

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
        val allExercises = db.exerciseDao().getAll().associateBy { it.id }

        val items = plannedList.map { planned ->
            val exercise = planned.exercise
            val groupMembers = exercise.groupId
                ?.let { gid -> allExercises.values.filter { it.groupId == gid } }
                ?: listOf(exercise)

            if (temporaryVisit) {
                // Temporary visit: no availability filtering; offer the full group.
                val options = groupMembers.filter { it.id != exercise.id }
                StartItem(
                    planned = planned,
                    resolution = if (options.isEmpty()) GymResolution.Available
                    else GymResolution.Choice(options),
                    options = options,
                )
            } else if (gymId == null) {
                StartItem(planned, GymResolution.Available, emptyList())
            } else {
                val availableIds = db.gymDao().exerciseIdsAt(gymId).toSet()
                val override = db.gymDao().overridesAt(gymId)
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
        return PendingStart(routineId, dayId, gymId, temporaryVisit, prefillMode, items)
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
        val settings = container.settings.settings.first()
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
            val chosenId: Long? = when {
                picks.containsKey(plannedId) -> picks[plannedId]
                item.resolution is GymResolution.AutoSwapped ->
                    (item.resolution as GymResolution.AutoSwapped).to.id

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
            val typeCounters = mutableMapOf<dev.hinny.skrot.data.model.SetType, Int>()
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
        val locked = container.settings.settings.first().sessionsLockedByDefault
        return db.sessionDao().insertSession(
            WorkoutSession(startedAt = now, gymId = gymId, lastActivityAt = now, locked = locked)
        )
    }
}

@Composable
fun HomeScreen(container: AppContainer, settings: Settings, nav: NavHostController) {
    val vm = containerViewModel(container) { c, _ -> HomeViewModel(c) }
    val state by vm.uiState.collectAsState()
    var startTarget by remember { mutableStateOf<Pair<RoutineWithDays?, RoutineDay?>?>(null) }
    var showPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)

        state.openSession?.let { open ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { nav.navigate(Routes.workout(open.id)) },
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.workout_in_progress),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(stringResource(R.string.tap_to_resume))
                }
            }
        }

        if (state.backupOverdue) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.backup_reminder_title),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { vm.backupReminderDismissed.value = true }) {
                            Icon(Icons.Filled.Close, stringResource(R.string.dismiss))
                        }
                    }
                    Text(
                        stringResource(R.string.backup_reminder_body),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    TextButton(onClick = { nav.navigate(Routes.BACKUP) }) {
                        Text(stringResource(R.string.backup_now))
                    }
                }
            }
        }

        if (state.comebackRoutines.isNotEmpty() && state.openSession == null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(
                                R.string.comeback_title,
                                state.daysSinceLastSession ?: 0,
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { vm.comebackDismissed.value = true }) {
                            Icon(Icons.Filled.Close, stringResource(R.string.dismiss))
                        }
                    }
                    Text(stringResource(R.string.comeback_body))
                    state.comebackRoutines.forEach { r ->
                        TextButton(onClick = { startTarget = r to r.sortedDays.firstOrNull() }) {
                            Text(r.routine.name)
                        }
                    }
                }
            }
        }

        val active = state.activeRoutine
        if (active != null && state.openSession == null) {
            val nextDay = state.nextDay
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(active.routine.icon.vector(), null)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                stringResource(R.string.next_workout),
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Text(
                                nextDay?.name ?: stringResource(R.string.no_days_defined),
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                "${active.routine.name} · " +
                                    lastPerformedText(nextDay?.let { state.lastByDay[it.id] }),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { if (nextDay != null) startTarget = active to nextDay },
                            enabled = nextDay != null,
                        ) {
                            Icon(Icons.Filled.PlayArrow, null)
                            Text(stringResource(R.string.start))
                        }
                        OutlinedButton(onClick = { showPicker = true }) {
                            Text(stringResource(R.string.choose_other_workout))
                        }
                    }
                }
            }
        } else if (state.openSession == null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.no_active_program))
                    TextButton(onClick = { nav.navigate(Routes.PROGRAMS) }) {
                        Text(stringResource(R.string.go_to_programs))
                    }
                }
            }
        }

        OutlinedButton(onClick = { startTarget = null to null; }) {
            Text(stringResource(R.string.start_empty_workout))
        }

        if (state.allRoutines.isNotEmpty()) {
            Text(stringResource(R.string.tab_programs), style = MaterialTheme.typography.titleMedium)
            state.allRoutines.forEach { r ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { nav.navigate(Routes.program(r.routine.id)) },
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(r.routine.icon.vector(), null)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(r.routine.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                lastPerformedText(state.lastByRoutine[r.routine.id]),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        if (r.routine.isActive) {
                            Text(
                                stringResource(R.string.active_badge),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    // Workout picker (swap in a different day, from this or another program)
    if (showPicker) {
        WorkoutPickerDialog(
            routines = state.allRoutines,
            onDismiss = { showPicker = false },
            onPick = { r, day ->
                showPicker = false
                startTarget = r to day
            },
        )
    }

    // Gym + temporary-visit selection, then resolution
    StartFlowHost(
        vm = vm,
        nav = nav,
        gyms = state.gyms,
        startTarget = startTarget,
        onClearTarget = { startTarget = null },
    )
}
