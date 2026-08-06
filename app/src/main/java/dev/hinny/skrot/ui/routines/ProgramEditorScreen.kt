package dev.hinny.skrot.ui.routines

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import dev.hinny.skrot.AppContainer
import dev.hinny.skrot.R
import dev.hinny.skrot.data.model.Gym
import dev.hinny.skrot.data.model.PrefillMode
import dev.hinny.skrot.data.model.ProgramIcon
import dev.hinny.skrot.data.model.Routine
import dev.hinny.skrot.data.model.RoutineDay
import dev.hinny.skrot.data.model.RoutineWithDays
import dev.hinny.skrot.data.model.ScheduleMode
import dev.hinny.skrot.ui.Routes
import dev.hinny.skrot.ui.common.ConfirmDialog
import dev.hinny.skrot.data.prefs.Settings
import dev.hinny.skrot.ui.common.ReorderHandle
import dev.hinny.skrot.ui.common.ReorderLockButton
import dev.hinny.skrot.ui.common.rememberReorderLock
import dev.hinny.skrot.ui.common.rememberReorderState
import dev.hinny.skrot.ui.common.reorderableRow
import dev.hinny.skrot.ui.common.EditHistory
import dev.hinny.skrot.ui.common.PendingChangesBar
import dev.hinny.skrot.ui.common.vector
import dev.hinny.skrot.ui.common.vectorOrNull
import dev.hinny.skrot.ui.containerViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class ProgramEditorViewModel(
    private val container: AppContainer,
    private val routineId: Long,
) : ViewModel() {
    val routine = MutableStateFlow<RoutineWithDays?>(null)
    val gyms = MutableStateFlow<List<Gym>>(emptyList())

    /**
     * Mutations below write straight to the database; the history tracks
     * whether the live state has drifted from the last-confirmed one and puts
     * snapshots back via [restoreSnapshot].
     */
    val edits = EditHistory<RoutineWithDays>()
    val confirmEdits = edits.confirmEdits
    val hasPendingChanges = edits.hasPendingChanges
    val canUndo = edits.canUndo
    val canRedo = edits.canRedo
    val revision = edits.revision

    init {
        viewModelScope.launch {
            container.db.routineDao().observeWithDays(routineId).collect { r ->
                routine.value = r
                edits.baselineIfUnset(r)
                edits.refresh(r)
            }
        }
        viewModelScope.launch {
            container.db.gymDao().observeAll().collect { gyms.value = it }
        }
        viewModelScope.launch {
            container.settings.settings.collect {
                edits.confirmEdits.value = it.confirmLibraryEdits
                edits.refresh(routine.value)
            }
        }
    }

    fun applyChanges() = edits.rebaseline(routine.value)

    /** Reverts routine fields and the day list back to the last Apply point. */
    fun cancelChanges() {
        viewModelScope.launch { restoreSnapshot(edits.baseline ?: return@launch) }
    }

    private suspend fun restoreSnapshot(snap: RoutineWithDays) {
        val dao = container.db.routineDao()
        dao.update(snap.routine)
        val currentDays = routine.value?.days ?: emptyList()
        val snapDayIds = snap.days.map { it.id }.toSet()
        for (d in currentDays) if (d.id !in snapDayIds) dao.deleteDay(d)
        // REPLACE-insert restores field values on survivors and recreates
        // any day deleted during this editing session, under its original id.
        container.db.backupDao().insertDays(snap.days)
    }

    /** Records the pre-change snapshot so [undo] can revert this step. */
    private fun pushUndo() {
        routine.value?.let { edits.push(it) }
    }

    fun undo() {
        val current = routine.value ?: return
        val previous = edits.undo(current) ?: return
        viewModelScope.launch { restoreSnapshot(previous) }
    }

    fun redo() {
        val current = routine.value ?: return
        val next = edits.redo(current) ?: return
        viewModelScope.launch { restoreSnapshot(next) }
    }

    fun update(transform: (Routine) -> Routine) {
        pushUndo()
        viewModelScope.launch {
            routine.value?.let { container.db.routineDao().update(transform(it.routine)) }
        }
    }

    fun addDay(name: String) {
        pushUndo()
        viewModelScope.launch {
            val position = (routine.value?.days?.maxOfOrNull { it.position } ?: -1) + 1
            container.db.routineDao().insertDay(
                RoutineDay(routineId = routineId, name = name, position = position)
            )
        }
    }

    fun updateDay(day: RoutineDay) {
        pushUndo()
        viewModelScope.launch { container.db.routineDao().updateDay(day) }
    }

    fun deleteDay(day: RoutineDay) {
        pushUndo()
        viewModelScope.launch { container.db.routineDao().deleteDay(day) }
    }

    fun moveDay(from: Int, to: Int) {
        val days = routine.value?.sortedDays ?: return
        if (from !in days.indices || to !in days.indices || from == to) return
        pushUndo()
        viewModelScope.launch {
            val reordered = days.toMutableList()
            reordered.add(to, reordered.removeAt(from))
            reordered.forEachIndexed { i, d ->
                if (d.position != i) container.db.routineDao().updateDay(d.copy(position = i))
            }
        }
    }

    /** Deep-copies this program (days, exercises, sets) into a new one. */
    fun copy(nameSuffix: String, onDone: (Long) -> Unit) {
        viewModelScope.launch {
            val current = routine.value ?: return@launch
            val copyId = container.db.routineDao()
                .copyRoutine(current.routine.id, "${current.routine.name} $nameSuffix")
            if (copyId != null) onDone(copyId)
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            routine.value?.let { container.db.routineDao().delete(it.routine) }
            onDone()
        }
    }
}

@Composable
fun ProgramEditorScreen(
    container: AppContainer,
    settings: Settings,
    nav: NavHostController,
    routineId: Long,
) {
    val vm = containerViewModel(container, key = "program_$routineId") { c, _ ->
        ProgramEditorViewModel(c, routineId)
    }
    val state by vm.routine.collectAsState()
    val gyms by vm.gyms.collectAsState()
    val hasPendingChanges by vm.hasPendingChanges.collectAsState()
    val canUndo by vm.canUndo.collectAsState()
    val canRedo by vm.canRedo.collectAsState()
    val r = state ?: return
    var showAddDay by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    val revision by vm.revision.collectAsState()
    var name by remember(r.routine.id, revision) { mutableStateOf(r.routine.name) }
    var description by remember(r.routine.id, revision) { mutableStateOf(r.routine.description) }
    var tags by remember(r.routine.id, revision) {
        mutableStateOf(r.routine.tags.joinToString(", "))
    }
    val dayReorder = rememberReorderState { from, to -> vm.moveDay(from, to) }
    val orderLocked = rememberReorderLock(settings.listsLockedByDefault)

    Column(Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier
            .weight(1f)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Actions sit on their own row so the name field gets the full width.
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { vm.undo() }, enabled = canUndo) {
                    Icon(Icons.Filled.Undo, stringResource(R.string.undo))
                }
                IconButton(onClick = { vm.redo() }, enabled = canRedo) {
                    Icon(Icons.Filled.Redo, stringResource(R.string.redo))
                }
                val copySuffix = stringResource(R.string.clone_suffix)
                IconButton(onClick = {
                    vm.copy(copySuffix) { id -> nav.navigate(Routes.program(id)) }
                }) {
                    Icon(Icons.Filled.ContentCopy, stringResource(R.string.copy_program))
                }
                IconButton(onClick = { showDelete = true }) {
                    Icon(Icons.Filled.Delete, stringResource(R.string.delete))
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { nav.popBackStack() }) {
                    Text(stringResource(R.string.done))
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { showIconPicker = true }) {
                    Icon(
                        r.routine.icon.vectorOrNull() ?: Icons.Filled.Add,
                        stringResource(R.string.icon),
                    )
                }
                Spacer(Modifier.width(4.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        vm.update { routine -> routine.copy(name = it) }
                    },
                    label = { Text(stringResource(R.string.name)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it
                    vm.update { routine -> routine.copy(description = it) }
                },
                label = { Text(stringResource(R.string.description)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = tags,
                onValueChange = { text ->
                    tags = text
                    val parsed = text.split(',', ' ')
                        .map { it.trim().removePrefix("#") }
                        .filter { it.isNotBlank() }
                    vm.update { routine -> routine.copy(tags = parsed) }
                },
                label = { Text(stringResource(R.string.tags_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = r.routine.isActive,
                    onCheckedChange = { active ->
                        vm.viewModelScope.launch {
                            container.db.routineDao().setActive(if (active) routineId else null)
                        }
                    },
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.active_program))
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = r.routine.isRecovery,
                    onCheckedChange = { on -> vm.update { it.copy(isRecovery = on) } },
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.recovery_program))
            }
            Text(
                stringResource(R.string.recovery_program_hint),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        item {
            Text(stringResource(R.string.schedule), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = r.routine.scheduleMode == ScheduleMode.ROTATING,
                    onClick = { vm.update { it.copy(scheduleMode = ScheduleMode.ROTATING) } },
                    label = { Text(stringResource(R.string.schedule_rotating)) },
                )
                FilterChip(
                    selected = r.routine.scheduleMode == ScheduleMode.FIXED_WEEKDAYS,
                    onClick = { vm.update { it.copy(scheduleMode = ScheduleMode.FIXED_WEEKDAYS) } },
                    label = { Text(stringResource(R.string.schedule_fixed)) },
                )
            }
        }
        item {
            Text(stringResource(R.string.prefill_mode), style = MaterialTheme.typography.titleSmall)
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = r.routine.prefillMode == PrefillMode.LAST_SESSION,
                        onClick = { vm.update { it.copy(prefillMode = PrefillMode.LAST_SESSION) } },
                        label = { Text(stringResource(R.string.prefill_last_session)) },
                    )
                    FilterChip(
                        selected = r.routine.prefillMode == PrefillMode.TARGETS,
                        onClick = { vm.update { it.copy(prefillMode = PrefillMode.TARGETS) } },
                        label = { Text(stringResource(R.string.prefill_targets)) },
                    )
                }
                FilterChip(
                    selected = r.routine.prefillMode == PrefillMode.HYBRID,
                    onClick = { vm.update { it.copy(prefillMode = PrefillMode.HYBRID) } },
                    label = { Text(stringResource(R.string.prefill_hybrid)) },
                )
            }
        }
        // Nothing to choose between until there is more than one gym, and the
        // whole idea is meaningless with none.
        if (gyms.size > 1) {
            item {
                Text(
                    stringResource(R.string.program_default_gym),
                    style = MaterialTheme.typography.titleSmall,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    FilterChip(
                        selected = r.routine.defaultGymId == null,
                        onClick = { vm.update { it.copy(defaultGymId = null) } },
                        label = { Text(stringResource(R.string.default_gym_global)) },
                    )
                    gyms.forEach { gym ->
                        FilterChip(
                            selected = r.routine.defaultGymId == gym.id,
                            onClick = { vm.update { it.copy(defaultGymId = gym.id) } },
                            label = { Text(gym.name) },
                        )
                    }
                }
                Text(
                    stringResource(R.string.program_default_gym_hint),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.workout_days),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                ReorderLockButton(orderLocked)
            }
        }
        val days = r.sortedDays
        items(days.size) { i ->
            val day = days[i]
            Card(
                Modifier
                    .fillMaxWidth()
                    .then(
                        if (orderLocked.value) Modifier
                        else Modifier.reorderableRow(dayReorder, i, days.size)
                    )
            ) {
                Column(Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!orderLocked.value) ReorderHandle(dayReorder, i, days.size)
                        Spacer(Modifier.width(8.dp))
                        Column(
                            Modifier
                                .weight(1f)
                                .clickable { nav.navigate(Routes.day(day.id)) },
                        ) {
                            Text(day.name, style = MaterialTheme.typography.titleSmall)
                            if (day.description.isNotBlank()) {
                                Text(day.description, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        IconButton(onClick = { vm.deleteDay(day) }) {
                            Icon(Icons.Filled.Delete, stringResource(R.string.delete))
                        }
                    }
                    if (r.routine.scheduleMode == ScheduleMode.FIXED_WEEKDAYS) {
                        WeekdayChips(
                            selected = day.weekdays,
                            onToggle = { weekday ->
                                val updated =
                                    if (weekday in day.weekdays) day.weekdays - weekday
                                    else day.weekdays + weekday
                                vm.updateDay(day.copy(weekdays = updated.sorted()))
                            },
                        )
                    }
                }
            }
        }
        item {
            OutlinedButton(onClick = { showAddDay = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.add_day))
            }
            Spacer(Modifier.height(60.dp))
        }
    }

    if (hasPendingChanges) {
        PendingChangesBar(onApply = { vm.applyChanges() }, onCancel = { vm.cancelChanges() })
    }
    }

    if (showAddDay) {
        var dayName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDay = false },
            title = { Text(stringResource(R.string.add_day)) },
            text = {
                OutlinedTextField(
                    value = dayName,
                    onValueChange = { dayName = it },
                    label = { Text(stringResource(R.string.name)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (dayName.isNotBlank()) {
                        vm.addDay(dayName.trim())
                        showAddDay = false
                    }
                }) { Text(stringResource(R.string.create)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDay = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
    if (showDelete) {
        ConfirmDialog(
            title = stringResource(R.string.delete_program),
            text = stringResource(R.string.delete_program_warning),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = { vm.delete { nav.popBackStack() } },
            onDismiss = { showDelete = false },
        )
    }
    if (showIconPicker) {
        IconPickerDialog(
            onPick = { icon ->
                vm.update { it.copy(icon = icon) }
                showIconPicker = false
            },
            onDismiss = { showIconPicker = false },
        )
    }
}

@Composable
fun IconPickerDialog(onPick: (ProgramIcon) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.icon)) },
        text = {
            Column {
                // Going without an icon is a choice, not the absence of one.
                TextButton(onClick = { onPick(ProgramIcon.NONE) }) {
                    Text(stringResource(R.string.no_icon))
                }
                LazyVerticalGrid(columns = GridCells.Fixed(5), modifier = Modifier.height(320.dp)) {
                    val icons = ProgramIcon.pickable
                    items(icons.size) { i ->
                        val icon = icons[i]
                        IconButton(onClick = { onPick(icon) }) {
                            Icon(icon.vector(), icon.name)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
fun WeekdayChips(selected: List<Int>, onToggle: (Int) -> Unit) {
    val labels = listOf(
        R.string.weekday_mon, R.string.weekday_tue, R.string.weekday_wed,
        R.string.weekday_thu, R.string.weekday_fri, R.string.weekday_sat, R.string.weekday_sun,
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(top = 4.dp),
    ) {
        labels.forEachIndexed { index, labelRes ->
            val weekday = index + 1
            FilterChip(
                selected = weekday in selected,
                onClick = { onToggle(weekday) },
                label = { Text(stringResource(labelRes), style = MaterialTheme.typography.labelSmall) },
            )
        }
    }
}
