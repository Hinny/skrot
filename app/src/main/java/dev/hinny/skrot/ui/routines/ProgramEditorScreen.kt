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
     * Edits go to a draft and reach the database only on Apply. Deleting a day
     * cascades to everything planned into it, so nothing is deleted until the
     * deletion is confirmed — under write-through, Cancel could only put the
     * day row back, never its contents.
     */
    val edits = EditHistory<RoutineWithDays>()
    val confirmEdits = edits.confirmEdits
    val hasPendingChanges = edits.hasPendingChanges
    val canUndo = edits.canUndo
    val canRedo = edits.canRedo
    val revision = edits.revision

    /** Ids for days that exist only in the draft so far; always negative. */
    private var nextTempId = -1L

    init {
        viewModelScope.launch {
            container.db.routineDao().observeWithDays(routineId).collect { fresh ->
                // The database is the baseline. Its emissions are adopted into
                // the draft only until the first edit; after that the draft is
                // what the screen shows, and the writes Apply makes would
                // otherwise bounce back and clobber later edits.
                edits.rebaseline(fresh)
                if (!edits.hasPendingChanges.value && !edits.canUndo.value) {
                    routine.value = fresh
                }
                edits.refresh(routine.value)
            }
        }
        viewModelScope.launch {
            container.db.gymDao().observeAll().collect { gyms.value = it }
        }
        viewModelScope.launch {
            container.settings.settings.collect {
                edits.confirmEdits.value = it.confirmLibraryEdits
                if (!it.confirmLibraryEdits) applyChanges()
                edits.refresh(routine.value)
            }
        }
    }

    /** Applies [transform] to the draft, recording one undo step. */
    private fun edit(transform: (RoutineWithDays) -> RoutineWithDays) {
        val current = routine.value ?: return
        val updated = transform(current)
        if (updated == current) return
        edits.push(current)
        routine.value = updated
        edits.refresh(updated)
        if (!confirmEdits.value) applyChanges()
    }

    /** Throws the draft away. Nothing was written, so there is nothing to undo. */
    fun cancelChanges() {
        routine.value = edits.baseline
        edits.clearHistory()
        edits.refresh(edits.baseline)
    }

    fun undo() {
        val current = routine.value ?: return
        val previous = edits.undo(current) ?: return
        routine.value = previous
        edits.refresh(previous)
        if (!confirmEdits.value) applyChanges()
    }

    fun redo() {
        val current = routine.value ?: return
        val next = edits.redo(current) ?: return
        routine.value = next
        edits.refresh(next)
        if (!confirmEdits.value) applyChanges()
    }

    /**
     * Writes the draft as a diff against the baseline: days the draft dropped
     * are deleted, the ones it invented (negative ids) are inserted, the rest
     * updated. Deletion happens here and only here, so a day you remove and
     * then cancel never loses the exercises planned into it.
     *
     * @param onWritten runs once the draft is on disk and ids are real
     */
    fun applyChanges(onWritten: () -> Unit = {}) {
        val content = routine.value ?: return
        val baseline = edits.baseline
        if (content == baseline) {
            onWritten()
            return
        }
        val hasNewRows = content.days.any { it.id <= 0 } ||
            (baseline?.days?.size ?: 0) != content.days.size
        viewModelScope.launch {
            val dao = container.db.routineDao()
            dao.update(content.routine)

            val old = baseline?.days ?: emptyList()
            for (gone in old) {
                if (content.days.none { it.id == gone.id }) dao.deleteDay(gone)
            }
            for (day in content.days) {
                if (day.id > 0) dao.updateDay(day) else dao.insertDay(day.copy(id = 0))
            }
            if (hasNewRows) {
                val fresh = dao.withDays(routineId)
                routine.value = fresh
                edits.rebaseline(fresh)
                edits.clearHistory()
            } else {
                edits.rebaseline(content)
            }
            onWritten()
        }
    }

    /**
     * A day's own contents are edited on their own screen, which loads the day
     * by id — an id a drafted day does not have yet. Opening one therefore
     * commits the program-level draft first: you are going deeper into what you
     * are editing, not leaving it.
     */
    fun applyThenOpenDay(dayId: Long, open: (Long) -> Unit) {
        if (dayId > 0 && !hasPendingChanges.value) {
            open(dayId)
            return
        }
        val position = routine.value?.sortedDays?.indexOfFirst { it.id == dayId } ?: -1
        applyChanges {
            // A drafted day has no real id until Apply; find it again by the
            // position it settled into.
            val real = routine.value?.sortedDays?.getOrNull(position)?.id
            if (real != null && real > 0) open(real)
        }
    }

    fun update(transform: (Routine) -> Routine) = edit { content ->
        content.copy(routine = transform(content.routine))
    }

    fun addDay(name: String) = edit { content ->
        val position = (content.days.maxOfOrNull { it.position } ?: -1) + 1
        content.copy(
            days = content.days + RoutineDay(
                id = nextTempId--,
                routineId = routineId,
                name = name,
                position = position,
            )
        )
    }

    fun updateDay(day: RoutineDay) = edit { content ->
        content.copy(days = content.days.map { if (it.id == day.id) day else it })
    }

    fun deleteDay(day: RoutineDay) = edit { content ->
        content.copy(days = content.days.filterNot { it.id == day.id })
    }

    fun moveDay(from: Int, to: Int) = edit { content ->
        val days = content.sortedDays
        if (from !in days.indices || to !in days.indices || from == to) return@edit content
        val reordered = days.toMutableList()
        reordered.add(to, reordered.removeAt(from))
        content.copy(days = reordered.mapIndexed { i, d -> d.copy(position = i) })
    }

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
                                .clickable {
                                    vm.applyThenOpenDay(day.id) { id ->
                                        nav.navigate(Routes.day(id))
                                    }
                                },
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
