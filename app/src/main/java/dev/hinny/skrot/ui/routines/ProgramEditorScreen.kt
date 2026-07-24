package dev.hinny.skrot.ui.routines

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import dev.hinny.skrot.data.model.PrefillMode
import dev.hinny.skrot.data.model.ProgramIcon
import dev.hinny.skrot.data.model.Routine
import dev.hinny.skrot.data.model.RoutineDay
import dev.hinny.skrot.data.model.RoutineWithDays
import dev.hinny.skrot.data.model.ScheduleMode
import dev.hinny.skrot.ui.Routes
import dev.hinny.skrot.ui.common.ConfirmDialog
import dev.hinny.skrot.ui.common.DragHandle
import dev.hinny.skrot.ui.common.PendingChangesBar
import dev.hinny.skrot.ui.common.vector
import dev.hinny.skrot.ui.containerViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class ProgramEditorViewModel(
    private val container: AppContainer,
    private val routineId: Long,
) : ViewModel() {
    val routine = MutableStateFlow<RoutineWithDays?>(null)
    val confirmEdits = MutableStateFlow(true)
    val hasPendingChanges = MutableStateFlow(false)

    /** Last-confirmed state; only meaningful while [confirmEdits] is on. */
    private var baseline: RoutineWithDays? = null

    init {
        viewModelScope.launch {
            container.db.routineDao().observeWithDays(routineId).collect { r ->
                routine.value = r
                if (baseline == null) baseline = r
                recomputePending()
            }
        }
        viewModelScope.launch {
            container.settings.settings.collect {
                confirmEdits.value = it.confirmLibraryEdits
                recomputePending()
            }
        }
    }

    /**
     * Every mutation below writes straight to the database as before (add day,
     * reorder, field edits, ...); this just tracks whether the live state has
     * drifted from the last-confirmed [baseline] so the UI can show an
     * Apply/Cancel bar. In auto-apply mode every change re-baselines
     * immediately, so no bar ever appears.
     */
    private fun recomputePending() {
        if (!confirmEdits.value) {
            baseline = routine.value
            hasPendingChanges.value = false
        } else {
            hasPendingChanges.value = routine.value != baseline
        }
    }

    /** Reverts routine fields and the day list back to the last Apply point. */
    fun applyChanges() {
        baseline = routine.value
        hasPendingChanges.value = false
    }

    fun cancelChanges() {
        viewModelScope.launch {
            val snap = baseline ?: return@launch
            val dao = container.db.routineDao()
            dao.update(snap.routine)
            val currentDays = routine.value?.days ?: emptyList()
            val snapDayIds = snap.days.map { it.id }.toSet()
            for (d in currentDays) if (d.id !in snapDayIds) dao.deleteDay(d)
            // REPLACE-insert restores field values on survivors and recreates
            // any day deleted during this editing session, under its original id.
            container.db.backupDao().insertDays(snap.days)
        }
    }

    fun update(transform: (Routine) -> Routine) {
        viewModelScope.launch {
            routine.value?.let { container.db.routineDao().update(transform(it.routine)) }
        }
    }

    fun addDay(name: String) {
        viewModelScope.launch {
            val position = (routine.value?.days?.maxOfOrNull { it.position } ?: -1) + 1
            container.db.routineDao().insertDay(
                RoutineDay(routineId = routineId, name = name, position = position)
            )
        }
    }

    fun updateDay(day: RoutineDay) {
        viewModelScope.launch { container.db.routineDao().updateDay(day) }
    }

    fun deleteDay(day: RoutineDay) {
        viewModelScope.launch { container.db.routineDao().deleteDay(day) }
    }

    fun moveDay(day: RoutineDay, delta: Int) {
        viewModelScope.launch {
            val days = routine.value?.sortedDays ?: return@launch
            val index = days.indexOfFirst { it.id == day.id }
            val target = index + delta
            if (index < 0 || target < 0 || target >= days.size) return@launch
            val reordered = days.toMutableList()
            val moved = reordered.removeAt(index)
            reordered.add(target, moved)
            reordered.forEachIndexed { i, d ->
                if (d.position != i) container.db.routineDao().updateDay(d.copy(position = i))
            }
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
fun ProgramEditorScreen(container: AppContainer, nav: NavHostController, routineId: Long) {
    val vm = containerViewModel(container, key = "program_$routineId") { c, _ ->
        ProgramEditorViewModel(c, routineId)
    }
    val state by vm.routine.collectAsState()
    val hasPendingChanges by vm.hasPendingChanges.collectAsState()
    val r = state ?: return
    var showAddDay by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    var name by remember(r.routine.id) { mutableStateOf(r.routine.name) }
    var description by remember(r.routine.id) { mutableStateOf(r.routine.description) }
    var tags by remember(r.routine.id) { mutableStateOf(r.routine.tags.joinToString(", ")) }

    Column(Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier
            .weight(1f)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { showIconPicker = true }) {
                    Icon(r.routine.icon.vector(), stringResource(R.string.icon))
                }
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
                IconButton(onClick = { showDelete = true }) {
                    Icon(Icons.Filled.Delete, stringResource(R.string.delete))
                }
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
        item {
            Text(stringResource(R.string.workout_days), style = MaterialTheme.typography.titleMedium)
        }
        val days = r.sortedDays
        items(days.size) { i ->
            val day = days[i]
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DragHandle(onMove = { delta -> vm.moveDay(day, delta) })
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
            LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.height(200.dp)) {
                items(ProgramIcon.entries.size) { i ->
                    val icon = ProgramIcon.entries[i]
                    IconButton(onClick = { onPick(icon) }) {
                        Icon(icon.vector(), icon.name)
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
