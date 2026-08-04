package dev.hinny.skrot.ui.logging

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import dev.hinny.skrot.AppContainer
import dev.hinny.skrot.R
import dev.hinny.skrot.data.model.LoggedSet
import dev.hinny.skrot.data.model.MeasurementType
import dev.hinny.skrot.data.model.PlannedSet
import dev.hinny.skrot.data.model.SessionExerciseWithDetails
import dev.hinny.skrot.data.model.SetType
import dev.hinny.skrot.data.prefs.Settings
import dev.hinny.skrot.data.model.WeightUnit
import dev.hinny.skrot.domain.ProgressionSuggestion
import dev.hinny.skrot.domain.PrType
import dev.hinny.skrot.domain.Units
import dev.hinny.skrot.ui.Routes
import dev.hinny.skrot.ui.common.CoachMessages
import dev.hinny.skrot.ui.common.CompactNumberField
import dev.hinny.skrot.ui.common.CompactValueButton
import dev.hinny.skrot.ui.common.ExercisePickerDialog
import dev.hinny.skrot.ui.common.ReorderHandle
import dev.hinny.skrot.ui.common.ReorderState
import dev.hinny.skrot.ui.common.rememberReorderState
import dev.hinny.skrot.ui.common.reorderableRow
import dev.hinny.skrot.ui.common.StepperNumberField
import dev.hinny.skrot.ui.common.displayName
import dev.hinny.skrot.ui.containerViewModel
import dev.hinny.skrot.data.model.Exercise
import dev.hinny.skrot.data.model.MuscleGroup
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    container: AppContainer,
    settings: Settings,
    nav: NavHostController,
    sessionId: Long,
) {
    val vm = containerViewModel(container, key = "workout_$sessionId") { c, _ ->
        WorkoutViewModel(c, sessionId)
    }
    val content by vm.session.collectAsState()
    val plannedSets by vm.plannedSetsByPe.collectAsState()
    val suggestions by vm.suggestions.collectAsState()
    val groupOptions by vm.groupOptions.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    var showAddExercise by remember { mutableStateOf(false) }
    // Index of the block a picked exercise joins as a superset partner; null
    // means the picker (when open) adds a new block instead.
    var addToBlockIndex by remember { mutableStateOf<Int?>(null) }
    var showDiscard by remember { mutableStateOf(false) }
    var showFinish by remember { mutableStateOf(false) }
    var allExercises by remember { mutableStateOf(listOf<Exercise>()) }
    var elapsed by remember { mutableLongStateOf(0L) }

    // Root-relative bounds of the list and of each set row, recorded as they are
    // laid out, so the current set can be scrolled to the middle of the screen.
    val listState = rememberLazyListState()
    val rowBounds = remember { mutableStateMapOf<Long, IntRange>() }
    var listBounds by remember { mutableStateOf<IntRange?>(null) }
    val blockReorder = rememberReorderState { from, to -> vm.moveBlock(from, to) }

    // Keep the screen awake during an active workout (configurable).
    val view = LocalView.current
    DisposableEffect(settings.keepScreenOn) {
        view.keepScreenOn = settings.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    LaunchedEffect(Unit) {
        container.db.exerciseDao().observeAll().collect { allExercises = it }
    }

    // Elapsed clock + coach idle checks
    LaunchedEffect(content?.session?.startedAt) {
        while (true) {
            content?.session?.let { elapsed = System.currentTimeMillis() - it.startedAt }
            delay(1000)
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            vm.onIdleTick()
        }
    }

    val prPrefix = stringResource(R.string.new_pr)
    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is WorkoutEvent.Finished -> {
                    nav.navigate(Routes.summary(event.sessionId)) {
                        popUpTo(Routes.HOME)
                    }
                }

                is WorkoutEvent.Pr -> {
                    snackbar.showSnackbar("$prPrefix ${event.exerciseName}")
                }

                is WorkoutEvent.Coach -> {
                    CoachMessages.random(context, settings.coachPersonality, event.trigger)
                        ?.let { snackbar.showSnackbar(it) }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.workout))
                        Text(
                            formatElapsed(elapsed),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                },
                actions = {
                    content?.let { session ->
                        IconButton(onClick = { vm.toggleLock() }) {
                            Icon(
                                if (session.session.locked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                                contentDescription = stringResource(
                                    if (session.session.locked) R.string.unlock_session
                                    else R.string.lock_session
                                ),
                            )
                        }
                    }
                    IconButton(onClick = { showDiscard = true }) {
                        Icon(Icons.Filled.Delete, stringResource(R.string.discard))
                    }
                    Button(onClick = { showFinish = true }) {
                        Text(stringResource(R.string.done))
                    }
                },
            )
        },
    ) { padding ->
        val session = content
        if (session == null) {
            Spacer(Modifier.padding(padding))
            return@Scaffold
        }
        val locked = session.session.locked
        val removedMsg = stringResource(R.string.exercise_removed)
        val applyLabel = stringResource(R.string.apply_future_sessions)
        val blocks = session.blocks
        // The single set to do next in the whole session: the first block
        // (in order) with an incomplete set, alternating within a superset
        // (A1, B1, A2, B2, ...). Only one set is ever "current" at a time —
        // not one per exercise/block.
        val currentSetId = blocks.firstNotNullOfOrNull { block ->
            block
                .flatMapIndexed { exIndex, se ->
                    se.sortedSets.mapIndexedNotNull { setIndex, s ->
                        if (!s.completed) Triple(setIndex, exIndex, s.id) else null
                    }
                }
                .minWithOrNull(compareBy({ it.first }, { it.second }))
        }?.third

        // Finishing a set moves "current" to the next one, which is often just
        // off-screen. Pull it back to the middle so the next set is always in
        // reach without scrolling.
        LaunchedEffect(currentSetId) {
            val id = currentSetId ?: return@LaunchedEffect
            delay(CENTER_SCROLL_SETTLE_MS)
            if (rowBounds[id] == null) {
                // Not composed yet: jump to its block first, then centre it.
                val blockIndex = blocks.indexOfFirst { block ->
                    block.any { se -> se.sets.any { it.id == id } }
                }
                if (blockIndex < 0) return@LaunchedEffect
                listState.animateScrollToItem(blockIndex + if (locked) 1 else 0)
                delay(CENTER_SCROLL_SETTLE_MS)
            }
            val row = rowBounds[id] ?: return@LaunchedEffect
            val list = listBounds ?: return@LaunchedEffect
            val delta = ((row.first + row.last) / 2 - (list.first + list.last) / 2).toFloat()
            if (abs(delta) > CENTER_SCROLL_THRESHOLD_PX) listState.animateScrollBy(delta)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp)
                .onGloballyPositioned { coords ->
                    val top = coords.positionInRoot().y.roundToInt()
                    listBounds = top..(top + coords.size.height)
                },
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (locked) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Lock,
                                contentDescription = null,
                                modifier = Modifier.padding(start = 10.dp),
                            )
                            Text(
                                stringResource(R.string.session_locked_hint),
                                modifier = Modifier.padding(10.dp),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
            items(blocks.size) { blockIndex ->
                val block = blocks[blockIndex]
                val exerciseReorder = rememberReorderState { from, to ->
                    vm.moveExerciseInBlock(blockIndex, from, to)
                }
                Card(Modifier.reorderableRow(blockReorder, blockIndex, blocks.size)) {
                    Column(Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!locked) {
                                ReorderHandle(blockReorder, blockIndex, blocks.size)
                                Spacer(Modifier.width(6.dp))
                            }
                            if (block.size > 1) {
                                Text(
                                    stringResource(R.string.superset),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            if (!locked && blockIndex > 0) {
                                TextButton(onClick = { vm.linkWithPrevious(blockIndex) }) {
                                    Text(
                                        stringResource(R.string.link_superset),
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            }
                        }
                        block.forEachIndexed { exerciseIndex, se ->
                            ExerciseSection(
                                se = se,
                                vm = vm,
                                settings = settings,
                                plannedSets = se.sessionExercise.plannedExerciseId
                                    ?.let { plannedSets[it] } ?: emptyList(),
                                suggestion = suggestions[se.sessionExercise.id],
                                swapOptions = groupOptions[se.sessionExercise.id] ?: emptyList(),
                                currentSetId = currentSetId,
                                hasRoutineDay = session.session.routineDayId != null,
                                locked = locked,
                                rowBounds = rowBounds,
                                // Only supersets need per-exercise reordering; a
                                // lone exercise moves with its block.
                                blockReorder = exerciseReorder.takeIf { block.size > 1 },
                                indexInBlock = exerciseIndex,
                                blockSize = block.size,
                                onRemove = { removed ->
                                    val peId = removed.sessionExercise.plannedExerciseId
                                    vm.removeExercise(removed)
                                    // Session-only by default; the snackbar action also
                                    // removes it from the routine for future sessions.
                                    if (peId != null) {
                                        scope.launch {
                                            val result = snackbar.showSnackbar(
                                                message = removedMsg,
                                                actionLabel = applyLabel,
                                                duration = SnackbarDuration.Long,
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                vm.deletePlannedExercise(peId)
                                            }
                                        }
                                    }
                                },
                            )
                        }
                        if (!locked) {
                            TextButton(onClick = { addToBlockIndex = blockIndex }) {
                                Text(
                                    stringResource(R.string.add_superset_exercise),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = { showAddExercise = true },
                    enabled = !locked,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.add_exercise)) }
            }
            item {
                var note by remember(session.session.id) {
                    mutableStateOf(session.session.note)
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it; vm.setSessionNote(it) },
                    label = { Text(stringResource(R.string.session_note)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(80.dp))
            }
        }
    }

    if (showAddExercise || addToBlockIndex != null) {
        val intoBlock = addToBlockIndex
        fun closePicker() {
            showAddExercise = false
            addToBlockIndex = null
        }
        ExercisePickerDialog(
            exercises = allExercises,
            title = stringResource(
                if (intoBlock != null) R.string.add_superset_exercise else R.string.pick_exercise
            ),
            onPick = { vm.addExercise(it, intoBlock); closePicker() },
            onCreate = { new ->
                closePicker()
                scope.launch {
                    val id = container.db.exerciseDao().insert(
                        Exercise(
                            nameEn = new.name, nameSv = new.name,
                            muscleGroup = new.muscle,
                            equipment = new.equipment,
                            measurementType = new.measurement,
                            isCustom = true,
                        )
                    )
                    container.db.exerciseDao().byId(id)?.let { vm.addExercise(it, intoBlock) }
                }
            },
            onDismiss = { closePicker() },
        )
    }
    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            title = { Text(stringResource(R.string.discard_workout)) },
            text = { Text(stringResource(R.string.discard_workout_warning)) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscard = false
                    vm.discard()
                    nav.popBackStack(Routes.HOME, inclusive = false)
                }) { Text(stringResource(R.string.discard)) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscard = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
    if (showFinish) {
        AlertDialog(
            onDismissRequest = { showFinish = false },
            title = { Text(stringResource(R.string.finish_workout)) },
            confirmButton = {
                TextButton(onClick = { showFinish = false; vm.finish() }) {
                    Text(stringResource(R.string.done))
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinish = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

private fun formatElapsed(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

@Composable
private fun ExerciseSection(
    se: SessionExerciseWithDetails,
    vm: WorkoutViewModel,
    settings: Settings,
    plannedSets: List<PlannedSet>,
    suggestion: ProgressionSuggestion?,
    swapOptions: List<Exercise>,
    currentSetId: Long?,
    hasRoutineDay: Boolean,
    locked: Boolean,
    rowBounds: SnapshotStateMap<Long, IntRange>,
    blockReorder: ReorderState?,
    indexInBlock: Int,
    blockSize: Int,
    onRemove: (SessionExerciseWithDetails) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var swapOpen by remember { mutableStateOf(false) }
    var noteOpen by remember { mutableStateOf(false) }
    var nextTimeOpen by remember { mutableStateOf(false) }
    var removeSetOpen by remember { mutableStateOf(false) }

    val setReorder = rememberReorderState { from, to ->
        vm.moveSet(se.sessionExercise.id, from, to)
    }

    Column(
        Modifier
            .padding(vertical = 4.dp)
            .then(
                if (blockReorder != null) {
                    Modifier.reorderableRow(blockReorder, indexInBlock, blockSize)
                } else {
                    Modifier
                }
            ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (blockReorder != null && !locked) {
                ReorderHandle(blockReorder, indexInBlock, blockSize)
                Spacer(Modifier.width(6.dp))
            }
            Text(
                se.exercise.displayName(),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, stringResource(R.string.more))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                if (swapOptions.isNotEmpty()) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.swap_exercise)) },
                        enabled = !locked,
                        onClick = { menuOpen = false; swapOpen = true },
                    )
                }
                if (blockSize > 1) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.unlink)) },
                        enabled = !locked,
                        onClick = { menuOpen = false; vm.unlink(se) },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.exercise_note)) },
                    onClick = { menuOpen = false; noteOpen = true },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.next_time_note)) },
                    onClick = { menuOpen = false; nextTimeOpen = true },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.remove_exercise)) },
                    enabled = !locked,
                    onClick = { menuOpen = false; onRemove(se) },
                )
            }
        }

        if (se.exercise.nextTimeNote.isNotBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            ) {
                Text(
                    stringResource(R.string.note_prefix, se.exercise.nextTimeNote),
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        if (suggestion != null) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            ) {
                Row(
                    Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        when (suggestion) {
                            is ProgressionSuggestion.IncreaseLoad -> stringResource(
                                R.string.suggestion_increase,
                                formatLoad(suggestion.toLoad, settings.unit, se.exercise.measurementType),
                            )

                            is ProgressionSuggestion.AddRep -> stringResource(
                                R.string.suggestion_add_rep,
                                suggestion.toReps,
                            )
                        },
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    TextButton(onClick = { vm.acceptSuggestion(se, suggestion) }) {
                        Text(stringResource(R.string.accept))
                    }
                    TextButton(onClick = { vm.dismissSuggestion(se.sessionExercise.id) }) {
                        Text(stringResource(R.string.dismiss))
                    }
                }
            }
        }

        val sets = se.sortedSets
        var standardCounter = 0
        sets.forEachIndexed { setIndex, set ->
            val number = if (set.setType == SetType.STANDARD) ++standardCounter else null
            SetRow(
                se = se,
                set = set,
                number = number,
                planned = plannedSets.find { it.position == set.position },
                settings = settings,
                vm = vm,
                isCurrent = set.id == currentSetId,
                locked = locked,
                rowBounds = rowBounds,
                reorder = setReorder,
                index = setIndex,
                count = sets.size,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { vm.addSet(se) }, enabled = !locked) {
                Text(stringResource(R.string.add_set))
            }
            TextButton(
                onClick = { vm.addSet(se, SetType.DROP_SET, sets.lastOrNull()) },
                enabled = !locked,
            ) {
                Text(stringResource(R.string.add_drop_set))
            }
            TextButton(
                onClick = { removeSetOpen = true },
                enabled = !locked && sets.isNotEmpty(),
            ) {
                Text(stringResource(R.string.remove_set_button))
            }
        }

        // Session edits are session-only by default; these discreet actions
        // write the change back to the routine for future sessions.
        val peId = se.sessionExercise.plannedExerciseId
        if (!locked && peId != null && plannedSets.isNotEmpty() && plannedSets.size != sets.size) {
            TextButton(onClick = { vm.applySetsToPlan(se) }) {
                Text(
                    stringResource(R.string.apply_future_sessions),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        } else if (!locked && peId == null && hasRoutineDay) {
            TextButton(onClick = { vm.addExerciseToPlan(se) }) {
                Text(
                    stringResource(R.string.apply_future_sessions),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }

    if (swapOpen) {
        AlertDialog(
            onDismissRequest = { swapOpen = false },
            title = { Text(stringResource(R.string.swap_exercise)) },
            text = {
                Column {
                    swapOptions.forEach { option ->
                        TextButton(onClick = { vm.swapExercise(se, option); swapOpen = false }) {
                            Text(option.displayName())
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { swapOpen = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
    if (removeSetOpen) {
        RemoveSetDialog(
            se = se,
            settings = settings,
            onRemove = { vm.removeSet(se, it) },
            onDismiss = { removeSetOpen = false },
        )
    }
    if (noteOpen) {
        TextInputDialog(
            title = stringResource(R.string.exercise_note),
            initial = se.sessionExercise.note,
            onSave = { vm.setExerciseNote(se, it) },
            onDismiss = { noteOpen = false },
        )
    }
    if (nextTimeOpen) {
        TextInputDialog(
            title = stringResource(R.string.next_time_note),
            initial = se.exercise.nextTimeNote,
            onSave = { vm.setNextTimeNote(se.exercise, it) },
            onDismiss = { nextTimeOpen = false },
        )
    }
}

/**
 * Set removal, picked explicitly from a list. Replaces a swipe gesture that was
 * both undiscoverable and the only swipe action in the app.
 */
@Composable
private fun RemoveSetDialog(
    se: SessionExerciseWithDetails,
    settings: Settings,
    onRemove: (LoggedSet) -> Unit,
    onDismiss: () -> Unit,
) {
    val sets = se.sortedSets
    var standardCounter = 0
    val labels = sets.map { set ->
        when (set.setType) {
            SetType.WARMUP -> stringResource(R.string.set_marker_warmup)
            SetType.STANDARD -> (++standardCounter).toString()
            SetType.DROP_SET -> stringResource(R.string.set_marker_drop)
            SetType.FAILURE -> stringResource(R.string.set_marker_failure)
        }
    }
    val completedNote = stringResource(R.string.set_completed_note)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.remove_set)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.remove_set_pick),
                    style = MaterialTheme.typography.bodySmall,
                )
                sets.forEachIndexed { index, set ->
                    val summary = buildString {
                        append(labels[index])
                        append("  ")
                        append(formatLoad(set.load, settings.unit, se.exercise.measurementType))
                        append(" × ")
                        append(set.reps)
                        if (set.completed) append("  ($completedNote)")
                    }
                    TextButton(
                        onClick = { onRemove(set); onDismiss() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(summary, modifier = Modifier.weight(1f))
                        Icon(Icons.Filled.Delete, contentDescription = null)
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
fun TextInputDialog(
    title: String,
    initial: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(value = text, onValueChange = { text = it })
        },
        confirmButton = {
            TextButton(onClick = { onSave(text); onDismiss() }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

private fun formatLoad(load: Double, unit: WeightUnit, measurement: MeasurementType): String =
    when (measurement) {
        MeasurementType.MACHINE_LEVEL -> load.toInt().toString()
        else -> {
            val display = Units.toDisplay(load, unit, measurement)
            "${Units.formatValue(display)} ${if (unit == WeightUnit.KG) "kg" else "lbs"}"
        }
    }

@Composable
private fun SetRow(
    se: SessionExerciseWithDetails,
    set: LoggedSet,
    number: Int?,
    planned: PlannedSet?,
    settings: Settings,
    vm: WorkoutViewModel,
    isCurrent: Boolean,
    locked: Boolean,
    rowBounds: SnapshotStateMap<Long, IntRange>,
    reorder: ReorderState,
    index: Int,
    count: Int,
) {
    val measurement = se.exercise.measurementType
    var loadText by remember(set.id) {
        mutableStateOf(
            if (set.load == 0.0 && !set.completed && measurement == MeasurementType.BODYWEIGHT) ""
            else Units.formatValue(Units.toDisplay(set.load, settings.unit, measurement))
        )
    }
    var repsText by remember(set.id) {
        mutableStateOf(if (set.reps == 0 && !set.completed) "" else set.reps.toString())
    }
    var targetOpen by remember { mutableStateOf(false) }
    var restOpen by remember { mutableStateOf(false) }

    fun currentLoadKg(): Double {
        val raw = loadText.replace(',', '.').toDoubleOrNull() ?: 0.0
        return Units.fromDisplay(raw, settings.unit, measurement)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .reorderableRow(reorder, index, count)
            .onGloballyPositioned { coords ->
                val top = coords.positionInRoot().y.roundToInt()
                rowBounds[set.id] = top..(top + coords.size.height)
            },
    ) {
        if (!locked) {
            ReorderHandle(reorder, index, count)
        }

        // The set to do next is boxed in the accent color — the old tinted
        // fill alone was too easy to lose track of mid-workout.
        Surface(
            color = if (isCurrent) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                Color.Transparent
            },
            shape = RoundedCornerShape(10.dp),
            border = if (isCurrent) {
                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            } else {
                null
            },
            modifier = Modifier.weight(1f),
        ) {
            SetRowContent(
                se = se,
                set = set,
                number = number,
                planned = planned,
                settings = settings,
                vm = vm,
                isCurrent = isCurrent,
                locked = locked,
                loadText = loadText,
                onLoadText = { loadText = it },
                repsText = repsText,
                onRepsText = { repsText = it },
                currentLoadKg = ::currentLoadKg,
                onOpenTarget = { targetOpen = true },
                onOpenRest = { restOpen = true },
            )
        }
    }

    if (targetOpen && planned != null) {
        TargetDialog(
            planned = planned,
            onSave = { reps -> vm.updateTarget(se, set, reps) },
            onDismiss = { targetOpen = false },
        )
    }
    if (restOpen) {
        RestDialog(
            initial = set.restSec,
            step = settings.timerAdjustStepSec,
            canApplyToPlan = se.sessionExercise.plannedExerciseId != null,
            onSave = { sec, applyToPlan -> vm.updateRest(se, set, sec, applyToPlan) },
            onDismiss = { restOpen = false },
        )
    }
}

@Composable
private fun SetRowContent(
    se: SessionExerciseWithDetails,
    set: LoggedSet,
    number: Int?,
    planned: PlannedSet?,
    settings: Settings,
    vm: WorkoutViewModel,
    isCurrent: Boolean,
    locked: Boolean,
    loadText: String,
    onLoadText: (String) -> Unit,
    repsText: String,
    onRepsText: (String) -> Unit,
    currentLoadKg: () -> Double,
    onOpenTarget: () -> Unit,
    onOpenRest: () -> Unit,
) {
    val measurement = se.exercise.measurementType
    val isLevel = measurement == MeasurementType.MACHINE_LEVEL
    val focusManager = LocalFocusManager.current

    // Finishing a set drops focus: leaving the cursor in a field keeps the
    // keyboard up over half the screen for no reason.
    fun finishSet() {
        focusManager.clearFocus()
        vm.completeSet(se, set, currentLoadKg(), repsText.toIntOrNull() ?: 0)
    }

    // Everything but the type marker and the trailing button is weighted, so the
    // row fits any phone width instead of pushing the Done button off-screen.
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 4.dp),
    ) {
        // Set type marker; tap cycles warmup -> standard -> drop -> failure
        val typeLabel = when (set.setType) {
            SetType.WARMUP -> stringResource(R.string.set_marker_warmup)
            SetType.STANDARD -> number?.toString() ?: "·"
            SetType.DROP_SET -> stringResource(R.string.set_marker_drop)
            SetType.FAILURE -> stringResource(R.string.set_marker_failure)
        }
        SetTypeMarker(
            label = typeLabel,
            enabled = !locked,
            onClick = {
                val next = when (set.setType) {
                    SetType.WARMUP -> SetType.STANDARD
                    SetType.STANDARD -> SetType.DROP_SET
                    SetType.DROP_SET -> SetType.FAILURE
                    SetType.FAILURE -> SetType.WARMUP
                }
                vm.setSetType(set, next)
            },
        )

        val loadLabel = when (measurement) {
            MeasurementType.WEIGHT_KG ->
                if (settings.unit == WeightUnit.KG) "kg" else "lbs"

            MeasurementType.MACHINE_LEVEL -> stringResource(R.string.level)
            MeasurementType.BODYWEIGHT ->
                if (settings.unit == WeightUnit.KG) "+kg" else "+lbs"
        }
        CompactNumberField(
            value = loadText,
            onValueChange = {
                onLoadText(limitLoadInput(it))
                vm.updateSetValues(set, currentLoadKg(), repsText.toIntOrNull() ?: 0)
            },
            label = loadLabel,
            decimal = !isLevel,
            modifier = Modifier.weight(1.25f),
        )
        CompactNumberField(
            value = repsText,
            onValueChange = {
                val filtered = it.filter { c -> c.isDigit() }.take(MAX_INPUT_DIGITS)
                onRepsText(filtered)
                vm.updateSetValues(set, currentLoadKg(), filtered.toIntOrNull() ?: 0)
            },
            label = stringResource(R.string.reps),
            modifier = Modifier.weight(1f),
        )

        // Target sits directly right of the actual reps: the number you are
        // aiming for next to the number you just entered.
        val targetText = when {
            set.setType == SetType.FAILURE -> stringResource(R.string.amrap)
            planned?.targetRepsMin != null -> "${planned.targetRepsMin}"
            else -> "—"
        }
        CompactValueButton(
            value = targetText,
            label = stringResource(R.string.target_short),
            onClick = { if (planned != null) onOpenTarget() },
            enabled = !locked && planned != null,
            modifier = Modifier.weight(0.9f),
        )
        CompactValueButton(
            value = "${set.restSec}s",
            label = stringResource(R.string.rest_s),
            onClick = onOpenRest,
            enabled = !locked,
            modifier = Modifier.weight(0.9f),
        )

        Box(
            modifier = Modifier
                .width(64.dp)
                .height(40.dp),
            contentAlignment = Alignment.Center,
        ) {
            when {
                set.completed -> IconButton(
                    onClick = { vm.uncompleteSet(set) },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = stringResource(R.string.undo_set),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }

                isCurrent -> Button(
                    onClick = ::finishSet,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(R.string.finish_set),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                    )
                }

                else -> IconButton(
                    onClick = ::finishSet,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = stringResource(R.string.finish_set),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    )
                }
            }
        }
    }
}

/** Digits allowed in the load and reps fields; three is plenty for both. */
private const val MAX_INPUT_DIGITS = 3

/** Time given to layout (and the keyboard) to settle before measuring for the centre scroll. */
private const val CENTER_SCROLL_SETTLE_MS = 80L

/** Don't bother animating a scroll shorter than this; it just looks like jitter. */
private const val CENTER_SCROLL_THRESHOLD_PX = 24f

/**
 * Keeps the load field narrow: at most three whole digits plus one decimal,
 * with an optional leading minus for bodyweight assistance.
 */
private fun limitLoadInput(text: String): String {
    val cleaned = text.filter { it.isDigit() || it == '.' || it == ',' || it == '-' }
    val negative = cleaned.startsWith("-")
    val body = if (negative) cleaned.drop(1) else cleaned
    val separator = body.indexOfFirst { it == '.' || it == ',' }
    val whole: String
    val fraction: String
    if (separator < 0) {
        whole = body.filter(Char::isDigit).take(MAX_INPUT_DIGITS)
        fraction = ""
    } else {
        whole = body.take(separator).filter(Char::isDigit).take(MAX_INPUT_DIGITS)
        fraction = body.drop(separator + 1).filter(Char::isDigit).take(1)
    }
    val separatorText = if (separator < 0) "" else body[separator].toString()
    return (if (negative) "-" else "") + whole + separatorText + fraction
}

/** Compact square marker for the set type; tapping it cycles the type. */
@Composable
private fun SetTypeMarker(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp, 40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(
                enabled = enabled,
                onClickLabel = stringResource(R.string.set_type),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TargetDialog(
    planned: PlannedSet,
    onSave: (Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    var targetText by remember { mutableStateOf(planned.targetRepsMin?.toString() ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.target_reps)) },
        text = {
            OutlinedTextField(
                value = targetText,
                onValueChange = { targetText = it.filter(Char::isDigit).take(3) },
                label = { Text(stringResource(R.string.target_min)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.width(110.dp),
            )
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(targetText.toIntOrNull())
                onDismiss()
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun RestDialog(
    initial: Int,
    step: Int,
    canApplyToPlan: Boolean,
    onSave: (restSec: Int, applyToPlan: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(initial.toString()) }
    var applyToPlan by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rest_duration)) },
        text = {
            Column {
                Text(stringResource(R.string.rest_zero_hint), style = MaterialTheme.typography.bodySmall)
                StepperNumberField(
                    value = value,
                    onValueChange = { value = it },
                    step = step.toDouble(),
                    label = stringResource(R.string.seconds),
                    integerOnly = true,
                )
                if (canApplyToPlan) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = applyToPlan,
                            onCheckedChange = { applyToPlan = it },
                        )
                        Text(
                            stringResource(R.string.apply_future_sessions),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave((value.toIntOrNull() ?: initial).coerceAtLeast(0), applyToPlan)
                onDismiss()
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
