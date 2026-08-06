package dev.hinny.skrot.ui.routines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import dev.hinny.skrot.AppContainer
import dev.hinny.skrot.R
import dev.hinny.skrot.data.model.DayWithContent
import dev.hinny.skrot.data.model.Exercise
import dev.hinny.skrot.data.model.MeasurementType
import dev.hinny.skrot.data.model.PlannedExercise
import dev.hinny.skrot.data.model.PlannedExerciseWithDetails
import dev.hinny.skrot.data.model.PlannedSet
import dev.hinny.skrot.data.model.SetType
import dev.hinny.skrot.data.prefs.Settings
import dev.hinny.skrot.domain.Units
import dev.hinny.skrot.ui.common.ExercisePickerDialog
import dev.hinny.skrot.ui.common.ReorderHandle
import dev.hinny.skrot.ui.common.ReorderLockButton
import dev.hinny.skrot.ui.common.ReorderState
import dev.hinny.skrot.ui.common.rememberReorderLock
import dev.hinny.skrot.ui.common.rememberReorderState
import dev.hinny.skrot.ui.common.reorderableRow
import dev.hinny.skrot.ui.common.NewExercise
import dev.hinny.skrot.ui.common.PendingChangesBar
import dev.hinny.skrot.ui.common.loadFieldLabel
import dev.hinny.skrot.ui.common.displayName
import dev.hinny.skrot.ui.common.vector
import dev.hinny.skrot.ui.containerViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Target reps for a freshly planned set: the bottom of the old 8-12 default, the
 * reps you have to reach before the load goes up.
 */
private const val DEFAULT_TARGET_REPS = 8

class DayEditorViewModel(
    private val container: AppContainer,
    private val dayId: Long,
) : ViewModel() {
    private val db = container.db
    val day = MutableStateFlow<DayWithContent?>(null)
    val allExercises = MutableStateFlow<List<Exercise>>(emptyList())
    val confirmEdits = MutableStateFlow(true)
    val hasPendingChanges = MutableStateFlow(false)

    /** Last-confirmed state; only meaningful while [confirmEdits] is on. */
    private var baseline: DayWithContent? = null

    init {
        viewModelScope.launch {
            db.routineDao().observeDayWithContent(dayId).collect { d ->
                day.value = d
                if (baseline == null) baseline = d
                recomputePending()
            }
        }
        viewModelScope.launch {
            container.observeExercises().collect { allExercises.value = it }
        }
        viewModelScope.launch {
            container.settings.settings.collect {
                confirmEdits.value = it.confirmLibraryEdits
                recomputePending()
            }
        }
    }

    /**
     * Every mutation below writes straight to the database as before (add/move
     * a block, edit a set, ...); this just tracks whether the live state has
     * drifted from the last-confirmed [baseline] so the UI can show an
     * Apply/Cancel bar. In auto-apply mode every change re-baselines
     * immediately, so no bar ever appears.
     */
    private fun recomputePending() {
        if (!confirmEdits.value) {
            baseline = day.value
            hasPendingChanges.value = false
        } else {
            hasPendingChanges.value = day.value != baseline
        }
    }

    fun applyChanges() {
        baseline = day.value
        hasPendingChanges.value = false
    }

    /** Reverts day fields, blocks/exercises and their sets to the last Apply point. */
    fun cancelChanges() {
        viewModelScope.launch {
            val snap = baseline ?: return@launch
            val dao = db.routineDao()
            val backupDao = db.backupDao()

            dao.updateDay(snap.day)

            val currentPeIds = day.value?.exercises?.map { it.planned.id }?.toSet() ?: emptySet()
            val snapPeIds = snap.exercises.map { it.planned.id }.toSet()
            for (peId in currentPeIds - snapPeIds) {
                dao.plannedExerciseById(peId)?.let { dao.deletePlannedExercise(it) }
            }
            // REPLACE-insert restores field values on survivors (including
            // blockPos/inBlockPos, so reordering/link/unlink undoes too) and
            // recreates anything deleted during this editing session, under
            // its original id.
            backupDao.insertPlannedExercises(snap.exercises.map { it.planned })

            for (pe in snap.exercises) {
                val currentSets = dao.plannedSets(pe.planned.id)
                val snapSetIds = pe.sets.map { it.id }.toSet()
                for (s in currentSets) if (s.id !in snapSetIds) dao.deletePlannedSet(s)
            }
            backupDao.insertPlannedSets(snap.exercises.flatMap { it.sets })
        }
    }

    fun updateDayFields(name: String? = null, description: String? = null) {
        viewModelScope.launch {
            val current = day.value?.day ?: return@launch
            db.routineDao().updateDay(
                current.copy(
                    name = name ?: current.name,
                    description = description ?: current.description,
                )
            )
        }
    }

    fun updateIcon(icon: dev.hinny.skrot.data.model.ProgramIcon) {
        viewModelScope.launch {
            val current = day.value?.day ?: return@launch
            db.routineDao().updateDay(current.copy(icon = icon))
        }
    }

    /** Adds an exercise as a new block, or into an existing block (superset). */
    fun addExercise(exercise: Exercise, intoBlockPos: Int? = null) {
        viewModelScope.launch {
            val settings = container.settings.settings.first()
            val content = day.value ?: return@launch
            val blockPos: Int
            val inBlockPos: Int
            if (intoBlockPos != null) {
                blockPos = intoBlockPos
                inBlockPos = (content.exercises
                    .filter { it.planned.blockPos == intoBlockPos }
                    .maxOfOrNull { it.planned.inBlockPos } ?: -1) + 1
            } else {
                blockPos = (content.exercises.maxOfOrNull { it.planned.blockPos } ?: -1) + 1
                inBlockPos = 0
            }
            val peId = db.routineDao().insertPlannedExercise(
                PlannedExercise(
                    dayId = dayId,
                    exerciseId = exercise.id,
                    blockPos = blockPos,
                    inBlockPos = inBlockPos,
                )
            )
            repeat(3) { i ->
                db.routineDao().insertPlannedSet(
                    PlannedSet(
                        plannedExerciseId = peId,
                        position = i,
                        targetRepsMin = DEFAULT_TARGET_REPS,
                        restSec = settings.defaultRestSec,
                    )
                )
            }
        }
    }

    fun removeExercise(pe: PlannedExerciseWithDetails) {
        viewModelScope.launch { db.routineDao().deletePlannedExercise(pe.planned) }
    }

    fun moveBlock(from: Int, to: Int) {
        viewModelScope.launch {
            val content = day.value ?: return@launch
            val blocks = content.blocks
            if (from !in blocks.indices || to !in blocks.indices || from == to) return@launch
            val reordered = blocks.toMutableList()
            reordered.add(to, reordered.removeAt(from))
            reordered.forEachIndexed { newPos, block ->
                block.forEach { pe ->
                    if (pe.planned.blockPos != newPos) {
                        db.routineDao().updatePlannedExercise(pe.planned.copy(blockPos = newPos))
                    }
                }
            }
        }
    }

    /** Merges this block into the previous one (creates/extends a superset). */
    fun linkWithPrevious(blockPos: Int) {
        viewModelScope.launch {
            val content = day.value ?: return@launch
            val blocks = content.blocks
            val index = blocks.indexOfFirst { it.first().planned.blockPos == blockPos }
            if (index <= 0) return@launch
            val previous = blocks[index - 1]
            val startInBlock = (previous.maxOfOrNull { it.planned.inBlockPos } ?: -1) + 1
            blocks[index].forEachIndexed { i, pe ->
                db.routineDao().updatePlannedExercise(
                    pe.planned.copy(
                        blockPos = previous.first().planned.blockPos,
                        inBlockPos = startInBlock + i,
                    )
                )
            }
        }
    }

    /** Splits an exercise out of its superset into its own block. */
    fun unlink(pe: PlannedExerciseWithDetails) {
        viewModelScope.launch {
            val content = day.value ?: return@launch
            val maxBlock = content.exercises.maxOfOrNull { it.planned.blockPos } ?: 0
            db.routineDao().updatePlannedExercise(
                pe.planned.copy(blockPos = maxBlock + 1, inBlockPos = 0)
            )
        }
    }

    fun addSet(pe: PlannedExerciseWithDetails) {
        viewModelScope.launch {
            val settings = container.settings.settings.first()
            val last = pe.sortedSets.lastOrNull()
            db.routineDao().insertPlannedSet(
                last?.copy(id = 0, position = last.position + 1)
                    ?: PlannedSet(
                        plannedExerciseId = pe.planned.id,
                        position = 0,
                        targetRepsMin = DEFAULT_TARGET_REPS,
                        restSec = settings.defaultRestSec,
                    )
            )
        }
    }

    fun updateSet(set: PlannedSet) {
        viewModelScope.launch { db.routineDao().updatePlannedSet(set) }
    }

    /**
     * Reorders a set within its exercise. Takes the exercise by id and re-reads
     * it from the current day rather than trusting a captured snapshot, the same
     * way [moveBlock] does — the drag state outlives several emissions of the
     * list it is reordering.
     */
    fun moveSet(plannedExerciseId: Long, from: Int, to: Int) {
        viewModelScope.launch {
            val sets = day.value?.exercises
                ?.find { it.planned.id == plannedExerciseId }
                ?.sortedSets
                ?: return@launch
            if (from !in sets.indices || to !in sets.indices || from == to) return@launch
            val reordered = sets.toMutableList()
            reordered.add(to, reordered.removeAt(from))
            reordered.forEachIndexed { newPos, s ->
                if (s.position != newPos) {
                    db.routineDao().updatePlannedSet(s.copy(position = newPos))
                }
            }
        }
    }

    fun removeSet(set: PlannedSet) {
        viewModelScope.launch { db.routineDao().deletePlannedSet(set) }
    }

    fun createCustomExercise(new: NewExercise, onCreated: (Exercise) -> Unit) {
        viewModelScope.launch {
            val id = container.db.exerciseDao().insert(
                Exercise(
                    nameEn = new.name,
                    nameSv = new.name,
                    muscleGroup = new.muscle,
                    equipment = new.equipment,
                    measurementType = new.measurement,
                    isCustom = true,
                )
            )
            container.db.exerciseDao().byId(id)?.let(onCreated)
        }
    }
}

@Composable
fun DayEditorScreen(
    container: AppContainer,
    settings: Settings,
    nav: NavHostController,
    dayId: Long,
) {
    val vm = containerViewModel(container, key = "day_$dayId") { c, _ ->
        DayEditorViewModel(c, dayId)
    }
    val content by vm.day.collectAsState()
    val exercises by vm.allExercises.collectAsState()
    val hasPendingChanges by vm.hasPendingChanges.collectAsState()
    val d = content ?: return
    var pickerTarget by remember { mutableStateOf<Int?>(-1) } // -1 closed, null new block, else block
    var showIconPicker by remember { mutableStateOf(false) }
    var name by remember(d.day.id) { mutableStateOf(d.day.name) }
    var description by remember(d.day.id) { mutableStateOf(d.day.description) }
    val blockReorder = rememberReorderState { from, to -> vm.moveBlock(from, to) }
    val orderLocked = rememberReorderLock(settings.listsLockedByDefault)

    Column(Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier
            .weight(1f)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                ReorderLockButton(orderLocked)
                TextButton(onClick = { nav.popBackStack() }) {
                    Text(stringResource(R.string.done))
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { showIconPicker = true }) {
                    Icon(
                        (d.day.icon ?: dev.hinny.skrot.data.model.ProgramIcon.BARBELL).vector(),
                        stringResource(R.string.icon),
                    )
                }
                Spacer(Modifier.width(4.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; vm.updateDayFields(name = it) },
                    label = { Text(stringResource(R.string.name)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it; vm.updateDayFields(description = it) },
                label = { Text(stringResource(R.string.description)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        val blocks = d.blocks
        items(blocks.size) { blockIndex ->
            val block = blocks[blockIndex]
            val blockPos = block.first().planned.blockPos
            Card(
                Modifier
                    .fillMaxWidth()
                    .then(
                        if (orderLocked.value) Modifier
                        else Modifier.reorderableRow(blockReorder, blockIndex, blocks.size)
                    )
            ) {
                Column(Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!orderLocked.value) {
                            ReorderHandle(blockReorder, blockIndex, blocks.size)
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (block.size > 1) stringResource(R.string.superset)
                            else stringResource(R.string.exercise),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                        if (blockIndex > 0) {
                            TextButton(onClick = { vm.linkWithPrevious(blockPos) }) {
                                Text(stringResource(R.string.link_superset))
                            }
                        }
                    }
                    block.forEach { pe ->
                        // Keyed by the exercise, not by its slot: reordering
                        // blocks otherwise leaves an editor holding the drag
                        // state of whichever exercise used to sit here.
                        key(pe.planned.id) {
                            PlannedExerciseEditor(
                                pe = pe,
                                settings = settings,
                                inSuperset = block.size > 1,
                                orderLocked = orderLocked.value,
                                vm = vm,
                            )
                        }
                    }
                    TextButton(onClick = { pickerTarget = blockPos }) {
                        Text(stringResource(R.string.add_superset_exercise))
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = { pickerTarget = null },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.add_exercise)) }
            Spacer(Modifier.height(60.dp))
        }
    }

    if (hasPendingChanges) {
        PendingChangesBar(onApply = { vm.applyChanges() }, onCancel = { vm.cancelChanges() })
    }
    }

    if (showIconPicker) {
        IconPickerDialog(
            onPick = { icon ->
                vm.updateIcon(icon)
                showIconPicker = false
            },
            onDismiss = { showIconPicker = false },
        )
    }

    if (pickerTarget != -1 || pickerTarget == null) {
        // pickerTarget: null = add as new block; >= 0 = add into that block
        val target = pickerTarget
        if (target == null || target >= 0) {
            ExercisePickerDialog(
                exercises = exercises,
                onPick = {
                    vm.addExercise(it, target)
                    pickerTarget = -1
                },
                onCreate = { new ->
                    vm.createCustomExercise(new) { created ->
                        vm.addExercise(created, target)
                    }
                    pickerTarget = -1
                },
                onDismiss = { pickerTarget = -1 },
            )
        }
    }
}

@Composable
private fun PlannedExerciseEditor(
    pe: PlannedExerciseWithDetails,
    settings: Settings,
    inSuperset: Boolean,
    orderLocked: Boolean,
    vm: DayEditorViewModel,
) {
    val setReorder = rememberReorderState { from, to -> vm.moveSet(pe.planned.id, from, to) }

    Column(Modifier.padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                pe.exercise.displayName(),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            if (inSuperset) {
                TextButton(onClick = { vm.unlink(pe) }) {
                    Text(stringResource(R.string.unlink))
                }
            }
            IconButton(onClick = { vm.removeExercise(pe) }) {
                Icon(Icons.Filled.Delete, stringResource(R.string.delete))
            }
        }
        val sets = pe.sortedSets
        sets.forEachIndexed { index, set ->
            PlannedSetRow(
                set = set,
                measurement = pe.exercise.measurementType,
                settings = settings,
                orderLocked = orderLocked,
                reorder = setReorder,
                index = index,
                count = sets.size,
                vm = vm,
            )
        }
        TextButton(onClick = { vm.addSet(pe) }) {
            Text(stringResource(R.string.add_set))
        }
    }
}

@Composable
private fun PlannedSetRow(
    set: PlannedSet,
    measurement: MeasurementType,
    settings: Settings,
    orderLocked: Boolean,
    reorder: ReorderState,
    index: Int,
    count: Int,
    vm: DayEditorViewModel,
) {
    var targetText by remember(set.id) { mutableStateOf(set.targetRepsMin?.toString() ?: "") }
    var loadText by remember(set.id) {
        mutableStateOf(
            set.targetLoad?.let { Units.formatValue(Units.toDisplay(it, settings.unit, measurement)) }
                ?: ""
        )
    }
    var restText by remember(set.id) { mutableStateOf(set.restSec.toString()) }

    // The three fields are weighted rather than fixed-width so the drag handle
    // fits on a narrow phone instead of pushing the delete button off the row —
    // the same bargain the logging screen's set row makes.
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (orderLocked) Modifier
                else Modifier.reorderableRow(reorder, index, count)
            ),
    ) {
        if (!orderLocked) {
            ReorderHandle(reorder, index, count)
        }
        val typeLabel = when (set.setType) {
            SetType.WARMUP -> stringResource(R.string.set_marker_warmup)
            SetType.STANDARD -> stringResource(R.string.set_marker_standard)
            SetType.DROP_SET -> stringResource(R.string.set_marker_drop)
            SetType.FAILURE -> stringResource(R.string.set_marker_failure)
        }
        AssistChip(
            onClick = {
                val next = when (set.setType) {
                    SetType.WARMUP -> SetType.STANDARD
                    SetType.STANDARD -> SetType.DROP_SET
                    SetType.DROP_SET -> SetType.FAILURE
                    SetType.FAILURE -> SetType.WARMUP
                }
                // FAILURE sets are AMRAP: no rep target.
                vm.updateSet(
                    if (next == SetType.FAILURE) set.copy(setType = next, targetRepsMin = null)
                    else set.copy(setType = next)
                )
            },
            label = { Text(typeLabel) },
        )
        if (set.setType != SetType.FAILURE) {
            OutlinedTextField(
                value = targetText,
                onValueChange = {
                    targetText = it.filter(Char::isDigit).take(3)
                    vm.updateSet(set.copy(targetRepsMin = targetText.toIntOrNull()))
                },
                label = { Text(stringResource(R.string.target_min), style = MaterialTheme.typography.labelSmall) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        } else {
            Text(stringResource(R.string.amrap), modifier = Modifier.weight(1f))
        }
        OutlinedTextField(
            value = loadText,
            onValueChange = { text ->
                loadText = text.filter { it.isDigit() || it == '.' || it == ',' }
                val parsed = loadText.replace(',', '.').toDoubleOrNull()
                vm.updateSet(
                    set.copy(targetLoad = parsed?.let { Units.fromDisplay(it, settings.unit, measurement) })
                )
            },
            label = {
                Text(
                    loadFieldLabel(measurement, settings.unit),
                    style = MaterialTheme.typography.labelSmall,
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1.1f),
        )
        OutlinedTextField(
            value = restText,
            onValueChange = {
                restText = it.filter(Char::isDigit)
                vm.updateSet(set.copy(restSec = restText.toIntOrNull() ?: 0))
            },
            label = { Text(stringResource(R.string.rest_s), style = MaterialTheme.typography.labelSmall) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { vm.removeSet(set) }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.Close, stringResource(R.string.delete))
        }
    }
}
