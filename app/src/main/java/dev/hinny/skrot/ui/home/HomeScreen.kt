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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
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
import dev.hinny.skrot.ui.common.displayName
import dev.hinny.skrot.ui.common.lastPerformedText
import dev.hinny.skrot.ui.common.vector
import dev.hinny.skrot.ui.containerViewModel
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
            ) { state, finished, settings, dismissed ->
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
                state.copy(
                    activeRoutine = active,
                    nextDay = nextDay,
                    daysSinceLastSession = daysSince,
                    comebackRoutines = if (daysSince == null) emptyList() else comeback,
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

    suspend fun startEmptySession(gymId: Long?): Long {
        val now = System.currentTimeMillis()
        return db.sessionDao().insertSession(
            WorkoutSession(startedAt = now, gymId = gymId, lastActivityAt = now)
        )
    }
}

@Composable
fun HomeScreen(container: AppContainer, settings: Settings, nav: NavHostController) {
    val vm = containerViewModel(container) { c, _ -> HomeViewModel(c) }
    val state by vm.uiState.collectAsState()
    var startTarget by remember { mutableStateOf<Pair<RoutineWithDays?, RoutineDay?>?>(null) }
    var pending by remember { mutableStateOf<PendingStart?>(null) }
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
        AlertDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            title = { Text(stringResource(R.string.choose_workout)) },
            text = {
                LazyColumn {
                    state.allRoutines.forEach { r ->
                        items(r.sortedDays.size) { i ->
                            val day = r.sortedDays[i]
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showPicker = false
                                        startTarget = r to day
                                    }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                            ) {
                                Text(day.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "${r.routine.name} · " ,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            },
        )
    }

    // Gym + temporary-visit selection, then resolution
    startTarget?.let { (routine, day) ->
        StartWorkoutDialog(
            gyms = state.gyms,
            onDismiss = { startTarget = null },
            onConfirm = { gymId, temporary ->
                startTarget = null
                vm.viewModelScope.launch {
                    if (routine == null && day == null) {
                        val id = vm.startEmptySession(gymId)
                        nav.navigate(Routes.workout(id))
                    } else {
                        val prepared = vm.prepareStart(
                            routine?.routine?.id, day?.id, gymId, temporary,
                        )
                        val needsInput = prepared.items.any {
                            it.resolution is GymResolution.Choice ||
                                it.resolution is GymResolution.NoEquivalent
                        }
                        if (needsInput) {
                            pending = prepared
                        } else {
                            val id = vm.startSession(prepared, emptyMap(), emptySet())
                            nav.navigate(Routes.workout(id))
                        }
                    }
                }
            },
        )
    }

    pending?.let { prepared ->
        ResolveExercisesDialog(
            pending = prepared,
            onDismiss = { pending = null },
            onConfirm = { picks, alwaysUse ->
                pending = null
                vm.viewModelScope.launch {
                    val id = vm.startSession(prepared, picks, alwaysUse)
                    nav.navigate(Routes.workout(id))
                }
            },
        )
    }
}

@Composable
private fun StartWorkoutDialog(
    gyms: List<Gym>,
    onDismiss: () -> Unit,
    onConfirm: (gymId: Long?, temporary: Boolean) -> Unit,
) {
    var selectedGym by remember { mutableStateOf(gyms.find { it.isDefault }?.id) }
    var temporary by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.start_workout)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (gyms.isNotEmpty()) {
                    Text(stringResource(R.string.gym))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedGym == null,
                            onClick = { selectedGym = null },
                            label = { Text(stringResource(R.string.no_gym)) },
                        )
                    }
                    gyms.forEach { gym ->
                        FilterChip(
                            selected = selectedGym == gym.id,
                            onClick = { selectedGym = gym.id },
                            label = { Text(gym.name) },
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = temporary, onCheckedChange = { temporary = it })
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.temporary_visit))
                    }
                    Text(
                        stringResource(R.string.temporary_visit_hint),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedGym, temporary) }) {
                Text(stringResource(R.string.start))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun ResolveExercisesDialog(
    pending: PendingStart,
    onDismiss: () -> Unit,
    onConfirm: (picks: Map<Long, Long?>, alwaysUse: Set<Long>) -> Unit,
) {
    val picks = remember { mutableStateOf(mapOf<Long, Long?>()) }
    val always = remember { mutableStateOf(setOf<Long>()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.resolve_exercises)) },
        text = {
            LazyColumn {
                val needing = pending.items.filter {
                    it.resolution is GymResolution.Choice ||
                        it.resolution is GymResolution.NoEquivalent
                }
                items(needing.size) { i ->
                    val item = needing[i]
                    val plannedId = item.planned.planned.id
                    var expanded by remember { mutableStateOf(false) }
                    val chosen = picks.value.getOrDefault(plannedId, item.planned.exercise.id)
                    Column(Modifier.padding(vertical = 8.dp)) {
                        Text(
                            item.planned.exercise.displayName(),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        if (item.resolution is GymResolution.NoEquivalent) {
                            Text(
                                stringResource(R.string.not_available_no_equivalent),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        OutlinedButton(onClick = { expanded = true }) {
                            Text(
                                when (chosen) {
                                    null -> stringResource(R.string.skip_exercise)
                                    item.planned.exercise.id ->
                                        stringResource(R.string.keep_original)

                                    else -> item.options.find { it.id == chosen }?.displayName()
                                        ?: ""
                                }
                            )
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.keep_original)) },
                                onClick = {
                                    picks.value = picks.value + (plannedId to item.planned.exercise.id)
                                    expanded = false
                                },
                            )
                            item.options.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.displayName()) },
                                    onClick = {
                                        picks.value = picks.value + (plannedId to option.id)
                                        expanded = false
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.skip_exercise)) },
                                onClick = {
                                    picks.value = picks.value + (plannedId to null)
                                    expanded = false
                                },
                            )
                        }
                        if (!pending.temporaryVisit && chosen != null &&
                            chosen != item.planned.exercise.id
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = plannedId in always.value,
                                    onCheckedChange = { checked ->
                                        always.value =
                                            if (checked) always.value + plannedId
                                            else always.value - plannedId
                                    },
                                )
                                Text(
                                    stringResource(R.string.always_use_at_gym),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(picks.value, always.value) }) {
                Text(stringResource(R.string.start))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
