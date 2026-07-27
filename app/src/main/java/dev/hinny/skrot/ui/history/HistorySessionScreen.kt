package dev.hinny.skrot.ui.history

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
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
import dev.hinny.skrot.data.model.Exercise
import dev.hinny.skrot.data.model.LoggedSet
import dev.hinny.skrot.data.model.MeasurementType
import dev.hinny.skrot.data.model.SessionExercise
import dev.hinny.skrot.data.model.SessionExerciseWithDetails
import dev.hinny.skrot.data.model.SessionWithContent
import dev.hinny.skrot.data.model.SetType
import dev.hinny.skrot.data.model.WeightUnit
import dev.hinny.skrot.data.prefs.Settings
import dev.hinny.skrot.domain.Units
import dev.hinny.skrot.ui.common.ConfirmDialog
import dev.hinny.skrot.ui.common.ExerciseMeta
import dev.hinny.skrot.ui.common.ExercisePickerDialog
import dev.hinny.skrot.ui.common.PendingChangesBar
import dev.hinny.skrot.ui.common.displayName
import dev.hinny.skrot.ui.containerViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Editing a workout that is already finished is a library-style edit, not
 * logging: there is no rest timer, no coach, no "current set", nothing to
 * finish. Changes go to an in-memory draft with undo/redo, and are written
 * only on Apply when "confirm library edits" is on — exactly like the
 * exercise, program, day and gym editors.
 */
class HistorySessionViewModel(
    private val container: AppContainer,
    private val sessionId: Long,
) : ViewModel() {
    private val db = container.db

    val draft = MutableStateFlow<SessionWithContent?>(null)
    val confirmEdits = MutableStateFlow(true)
    val canUndo = MutableStateFlow(false)
    val canRedo = MutableStateFlow(false)
    val allExercises = MutableStateFlow<List<Exercise>>(emptyList())
    val dayName = MutableStateFlow<String?>(null)
    val gymName = MutableStateFlow<String?>(null)

    /** What the database currently holds; Cancel goes back to this. */
    private var baseline: SessionWithContent? = null

    private val undoStack = ArrayDeque<SessionWithContent>()
    private val redoStack = ArrayDeque<SessionWithContent>()

    /** Ids for rows that exist only in the draft so far; always negative. */
    private var nextTempId = -1L

    val hasPendingChanges = MutableStateFlow(false)

    init {
        viewModelScope.launch { reload() }
        viewModelScope.launch {
            container.settings.settings.collect {
                confirmEdits.value = it.confirmLibraryEdits
                recomputePending()
            }
        }
        viewModelScope.launch {
            db.exerciseDao().observeAll().collect { allExercises.value = it }
        }
    }

    private suspend fun reload() {
        val fresh = db.sessionDao().sessionWithContent(sessionId)
        baseline = fresh
        draft.value = fresh
        undoStack.clear()
        redoStack.clear()
        updateUndoRedoFlags()
        recomputePending()
        val dayId = fresh?.session?.routineDayId
        dayName.value = dayId?.let { db.routineDao().dayById(it)?.name }
        gymName.value = fresh?.session?.gymId?.let { db.gymDao().byId(it)?.name }
    }

    private fun recomputePending() {
        hasPendingChanges.value = confirmEdits.value && draft.value != baseline
    }

    private fun updateUndoRedoFlags() {
        canUndo.value = undoStack.isNotEmpty()
        canRedo.value = redoStack.isNotEmpty()
    }

    /** Applies [transform] to the draft, recording one undo step. */
    private fun edit(transform: (SessionWithContent) -> SessionWithContent) {
        val current = draft.value ?: return
        val updated = transform(current)
        if (updated == current) return
        undoStack.addLast(current)
        redoStack.clear()
        draft.value = updated
        updateUndoRedoFlags()
        recomputePending()
        if (!confirmEdits.value) applyChanges()
    }

    private fun editExercise(
        seId: Long,
        transform: (SessionExerciseWithDetails) -> SessionExerciseWithDetails,
    ) = edit { content ->
        content.copy(
            exercises = content.exercises.map {
                if (it.sessionExercise.id == seId) transform(it) else it
            }
        )
    }

    fun updateSet(seId: Long, set: LoggedSet) = editExercise(seId) { se ->
        se.copy(sets = se.sets.map { if (it.id == set.id) set else it })
    }

    fun addSet(seId: Long) = editExercise(seId) { se ->
        val last = se.sortedSets.lastOrNull()
        val new = last?.copy(id = nextTempId--, position = last.position + 1)
            ?: LoggedSet(
                id = nextTempId--,
                sessionExerciseId = se.sessionExercise.id,
                position = 0,
                completed = true,
            )
        se.copy(sets = se.sets + new)
    }

    fun removeSet(seId: Long, setId: Long) = editExercise(seId) { se ->
        se.copy(sets = se.sets.filterNot { it.id == setId })
    }

    fun removeExercise(seId: Long) = edit { content ->
        content.copy(exercises = content.exercises.filterNot { it.sessionExercise.id == seId })
    }

    fun addExercise(exercise: Exercise) = edit { content ->
        val blockPos = (content.exercises.maxOfOrNull { it.sessionExercise.blockPos } ?: -1) + 1
        content.copy(
            exercises = content.exercises + SessionExerciseWithDetails(
                sessionExercise = SessionExercise(
                    id = nextTempId--,
                    sessionId = sessionId,
                    exerciseId = exercise.id,
                    blockPos = blockPos,
                ),
                exercise = exercise,
                sets = emptyList(),
            )
        )
    }

    fun setNote(note: String) = edit { it.copy(session = it.session.copy(note = note)) }

    fun undo() {
        val current = draft.value ?: return
        val previous = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(current)
        draft.value = previous
        updateUndoRedoFlags()
        recomputePending()
        if (!confirmEdits.value) applyChanges()
    }

    fun redo() {
        val current = draft.value ?: return
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(current)
        draft.value = next
        updateUndoRedoFlags()
        recomputePending()
        if (!confirmEdits.value) applyChanges()
    }

    fun cancelChanges() {
        draft.value = baseline
        undoStack.clear()
        redoStack.clear()
        updateUndoRedoFlags()
        recomputePending()
    }

    /**
     * Writes the draft to the database as a diff against [baseline]: rows the
     * draft dropped are deleted, rows it invented (negative ids) are inserted,
     * the rest are updated. Then everything is re-read so ids are real again.
     */
    fun applyChanges() {
        val content = draft.value ?: return
        viewModelScope.launch {
            val dao = db.sessionDao()
            dao.updateSession(content.session)

            val old = baseline?.exercises ?: emptyList()
            for (gone in old) {
                if (content.exercises.none { it.sessionExercise.id == gone.sessionExercise.id }) {
                    // Cascades to the exercise's logged sets.
                    dao.deleteSessionExercise(gone.sessionExercise)
                }
            }
            for (se in content.exercises) {
                val row = se.sessionExercise
                val realId = if (row.id > 0) {
                    dao.updateSessionExercise(row)
                    row.id
                } else {
                    dao.insertSessionExercise(row.copy(id = 0))
                }
                val oldSets = old.find { it.sessionExercise.id == row.id }?.sets ?: emptyList()
                for (goneSet in oldSets) {
                    if (se.sets.none { it.id == goneSet.id }) dao.deleteLoggedSet(goneSet)
                }
                se.sortedSets.forEachIndexed { position, set ->
                    val setRow = set.copy(sessionExerciseId = realId, position = position)
                    if (setRow.id > 0) dao.updateLoggedSet(setRow)
                    else dao.insertLoggedSet(setRow.copy(id = 0))
                }
            }
            reload()
        }
    }

    fun deleteSession(onDone: () -> Unit) {
        viewModelScope.launch {
            db.sessionDao().deleteSession(sessionId)
            onDone()
        }
    }
}

@Composable
fun HistorySessionScreen(
    container: AppContainer,
    settings: Settings,
    nav: NavHostController,
    sessionId: Long,
) {
    val vm = containerViewModel(container, key = "history_$sessionId") { c, _ ->
        HistorySessionViewModel(c, sessionId)
    }
    val content by vm.draft.collectAsState()
    val canUndo by vm.canUndo.collectAsState()
    val canRedo by vm.canRedo.collectAsState()
    val hasPendingChanges by vm.hasPendingChanges.collectAsState()
    val allExercises by vm.allExercises.collectAsState()
    val dayName by vm.dayName.collectAsState()
    val gymName by vm.gymName.collectAsState()
    var showDelete by remember { mutableStateOf(false) }
    var showAddExercise by remember { mutableStateOf(false) }
    val session = content ?: return

    val zone = ZoneId.systemDefault()
    val dateFormat = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm") }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    IconButton(onClick = { vm.undo() }, enabled = canUndo) {
                        Icon(Icons.Filled.Undo, stringResource(R.string.undo))
                    }
                    IconButton(onClick = { vm.redo() }, enabled = canRedo) {
                        Icon(Icons.Filled.Redo, stringResource(R.string.redo))
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
                Column {
                    Text(
                        dayName ?: stringResource(R.string.freestyle_session),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        Instant.ofEpochMilli(session.session.startedAt).atZone(zone)
                            .format(dateFormat) + (gymName?.let { " · $it" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            val blocks = session.blocks
            items(blocks.size) { blockIndex ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp)) {
                        if (blocks[blockIndex].size > 1) {
                            Text(
                                stringResource(R.string.superset),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        blocks[blockIndex].forEach { se ->
                            HistoryExerciseSection(se = se, settings = settings, vm = vm)
                        }
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = { showAddExercise = true },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.add_exercise)) }
            }
            item {
                var note by remember(session.session.id) { mutableStateOf(session.session.note) }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it; vm.setNote(it) },
                    label = { Text(stringResource(R.string.session_note)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(24.dp))
            }
        }

        if (hasPendingChanges) {
            PendingChangesBar(
                onApply = { vm.applyChanges() },
                onCancel = { vm.cancelChanges() },
            )
        }
    }

    if (showDelete) {
        ConfirmDialog(
            title = stringResource(R.string.delete_session),
            text = stringResource(R.string.delete_session_confirm),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = { vm.deleteSession { nav.popBackStack() } },
            onDismiss = { showDelete = false },
        )
    }
    if (showAddExercise) {
        ExercisePickerDialog(
            exercises = allExercises,
            onPick = { vm.addExercise(it); showAddExercise = false },
            onDismiss = { showAddExercise = false },
        )
    }
}

@Composable
private fun HistoryExerciseSection(
    se: SessionExerciseWithDetails,
    settings: Settings,
    vm: HistorySessionViewModel,
) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(se.exercise.displayName(), style = MaterialTheme.typography.titleMedium)
                ExerciseMeta(se.exercise)
            }
            IconButton(onClick = { vm.removeExercise(se.sessionExercise.id) }) {
                Icon(Icons.Filled.Delete, stringResource(R.string.remove_exercise))
            }
        }
        var standardCounter = 0
        se.sortedSets.forEach { set ->
            val number = if (set.setType == SetType.STANDARD) ++standardCounter else null
            HistorySetRow(se = se, set = set, number = number, settings = settings, vm = vm)
        }
        TextButton(onClick = { vm.addSet(se.sessionExercise.id) }) {
            Icon(Icons.Filled.Add, null)
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.add_set))
        }
    }
}

@Composable
private fun HistorySetRow(
    se: SessionExerciseWithDetails,
    set: LoggedSet,
    number: Int?,
    settings: Settings,
    vm: HistorySessionViewModel,
) {
    val measurement = se.exercise.measurementType
    val seId = se.sessionExercise.id
    var loadText by remember(set.id) {
        mutableStateOf(Units.formatValue(Units.toDisplay(set.load, settings.unit, measurement)))
    }
    var repsText by remember(set.id) { mutableStateOf(set.reps.toString()) }
    val unitLabel = when (measurement) {
        MeasurementType.MACHINE_LEVEL -> stringResource(R.string.measurement_level)
        else -> if (settings.unit == WeightUnit.KG) "kg" else "lbs"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(vertical = 2.dp),
    ) {
        Text(
            number?.toString() ?: setTypeShort(set.setType),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(24.dp),
        )
        OutlinedTextField(
            value = loadText,
            onValueChange = { text ->
                loadText = text.filter { it.isDigit() || it == '.' || it == ',' || it == '-' }
                val raw = loadText.replace(',', '.').toDoubleOrNull() ?: 0.0
                vm.updateSet(
                    seId,
                    set.copy(load = Units.fromDisplay(raw, settings.unit, measurement)),
                )
            },
            label = { Text(unitLabel, style = MaterialTheme.typography.labelSmall) },
            keyboardOptions = KeyboardOptions(
                keyboardType = if (measurement == MeasurementType.MACHINE_LEVEL) {
                    KeyboardType.Number
                } else {
                    KeyboardType.Decimal
                }
            ),
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = repsText,
            onValueChange = { text ->
                repsText = text.filter { it.isDigit() }
                vm.updateSet(seId, set.copy(reps = repsText.toIntOrNull() ?: 0))
            },
            label = { Text(stringResource(R.string.reps), style = MaterialTheme.typography.labelSmall) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        Checkbox(
            checked = set.completed,
            onCheckedChange = { vm.updateSet(seId, set.copy(completed = it)) },
        )
        IconButton(onClick = { vm.removeSet(seId, set.id) }) {
            Icon(Icons.Filled.Delete, stringResource(R.string.delete))
        }
    }
}

@Composable
private fun setTypeShort(type: SetType): String = stringResource(
    when (type) {
        SetType.WARMUP -> R.string.set_marker_warmup
        SetType.DROP_SET -> R.string.set_marker_drop
        SetType.FAILURE -> R.string.set_marker_failure
        SetType.STANDARD -> R.string.set_marker_standard
    }
)
