package dev.hinny.skrot.ui.logging

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
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
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import dev.hinny.skrot.ui.common.ExercisePickerDialog
import dev.hinny.skrot.ui.common.StepperNumberField
import dev.hinny.skrot.ui.common.displayName
import dev.hinny.skrot.ui.containerViewModel
import dev.hinny.skrot.data.model.Exercise
import dev.hinny.skrot.data.model.MuscleGroup
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    var showDiscard by remember { mutableStateOf(false) }
    var showFinish by remember { mutableStateOf(false) }
    var allExercises by remember { mutableStateOf(listOf<Exercise>()) }
    var elapsed by remember { mutableLongStateOf(0L) }

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
                    TextButton(onClick = { showDiscard = true }) {
                        Text(stringResource(R.string.discard))
                    }
                    Button(onClick = { showFinish = true }) {
                        Text(stringResource(R.string.finish))
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
        val removedMsg = stringResource(R.string.exercise_removed)
        val applyLabel = stringResource(R.string.apply_future_sessions)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val blocks = session.blocks
            items(blocks.size) { blockIndex ->
                val block = blocks[blockIndex]
                // The set to do next in this block: supersets alternate between the
                // linked exercises (A1, B1, A2, B2, ...).
                val currentSetId = block
                    .flatMapIndexed { exIndex, se ->
                        se.sortedSets.mapIndexedNotNull { setIndex, s ->
                            if (!s.completed) Triple(setIndex, exIndex, s.id) else null
                        }
                    }
                    .minWithOrNull(compareBy({ it.first }, { it.second }))
                    ?.third
                Card {
                    Column(Modifier.padding(10.dp)) {
                        if (block.size > 1) {
                            Text(
                                stringResource(R.string.superset),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                        }
                        block.forEach { se ->
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

    if (showAddExercise) {
        ExercisePickerDialog(
            exercises = allExercises,
            onPick = { vm.addExercise(it); showAddExercise = false },
            onCreate = { new ->
                showAddExercise = false
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
                    container.db.exerciseDao().byId(id)?.let { vm.addExercise(it) }
                }
            },
            onDismiss = { showAddExercise = false },
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
                    Text(stringResource(R.string.finish))
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
    onRemove: (SessionExerciseWithDetails) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var swapOpen by remember { mutableStateOf(false) }
    var noteOpen by remember { mutableStateOf(false) }
    var nextTimeOpen by remember { mutableStateOf(false) }

    Column(Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
                        onClick = { menuOpen = false; swapOpen = true },
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
        sets.forEach { set ->
            val number = if (set.setType == SetType.STANDARD) ++standardCounter else null
            SetRow(
                se = se,
                set = set,
                number = number,
                planned = plannedSets.find { it.position == set.position },
                settings = settings,
                vm = vm,
                isCurrent = set.id == currentSetId,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { vm.addSet(se) }) {
                Text(stringResource(R.string.add_set))
            }
            TextButton(onClick = { vm.addSet(se, SetType.DROP_SET, sets.lastOrNull()) }) {
                Text(stringResource(R.string.add_drop_set))
            }
        }

        // Session edits are session-only by default; these discreet actions
        // write the change back to the routine for future sessions.
        val peId = se.sessionExercise.plannedExerciseId
        if (peId != null && plannedSets.isNotEmpty() && plannedSets.size != sets.size) {
            TextButton(onClick = { vm.applySetsToPlan(se) }) {
                Text(
                    stringResource(R.string.apply_future_sessions),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        } else if (peId == null && hasRoutineDay) {
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
) {
    val measurement = se.exercise.measurementType
    val isLevel = measurement == MeasurementType.MACHINE_LEVEL
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

    // Swipe left to remove the set from this session (completed sets are
    // protected: un-complete first).
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart && !set.completed) {
                vm.removeSet(se, set)
                true
            } else {
                false
            }
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = !set.completed,
        backgroundContent = {
            if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.errorContainer,
                            RoundedCornerShape(10.dp),
                        )
                        .padding(end = 16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.remove_set),
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        },
    ) {
        Surface(
            color = if (isCurrent) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                Color.Transparent
            },
            shape = RoundedCornerShape(10.dp),
        ) {
            SetRowContent(
                se = se,
                set = set,
                number = number,
                planned = planned,
                settings = settings,
                vm = vm,
                isCurrent = isCurrent,
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
            onSave = { min, max -> vm.updateTarget(se, set, min, max) },
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

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp, horizontal = 2.dp),
    ) {
        // Set type marker; tap cycles warmup -> standard -> drop -> failure
        val typeLabel = when (set.setType) {
            SetType.WARMUP -> stringResource(R.string.set_marker_warmup)
            SetType.STANDARD -> number?.toString() ?: "·"
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
                vm.setSetType(set, next)
            },
            label = { Text(typeLabel) },
        )
        Spacer(Modifier.width(4.dp))

        // Target reps (reference next to the inputs; editable, persists to routine)
        val targetText = when {
            set.setType == SetType.FAILURE -> stringResource(R.string.amrap)
            planned?.targetRepsMin != null && planned.targetRepsMax != null ->
                "${planned.targetRepsMin}–${planned.targetRepsMax}"

            planned?.targetRepsMin != null -> "${planned.targetRepsMin}"
            else -> "—"
        }
        TextButton(onClick = { if (planned != null) onOpenTarget() }) {
            Text(targetText, style = MaterialTheme.typography.bodySmall)
        }

        val loadLabel = when (measurement) {
            MeasurementType.WEIGHT_KG ->
                if (settings.unit == WeightUnit.KG) "kg" else "lbs"

            MeasurementType.MACHINE_LEVEL -> stringResource(R.string.level)
            MeasurementType.BODYWEIGHT ->
                if (settings.unit == WeightUnit.KG) "+kg" else "+lbs"
        }
        OutlinedTextField(
            value = loadText,
            onValueChange = {
                onLoadText(it.filter { c -> c.isDigit() || c == '.' || c == ',' || c == '-' })
                vm.updateSetValues(
                    set,
                    currentLoadKg(),
                    repsText.toIntOrNull() ?: 0,
                )
            },
            label = { Text(loadLabel, style = MaterialTheme.typography.labelSmall) },
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isLevel) KeyboardType.Number else KeyboardType.Decimal
            ),
            singleLine = true,
            modifier = Modifier.width(84.dp),
        )
        Spacer(Modifier.width(6.dp))
        OutlinedTextField(
            value = repsText,
            onValueChange = {
                val filtered = it.filter { c -> c.isDigit() }
                onRepsText(filtered)
                vm.updateSetValues(set, currentLoadKg(), filtered.toIntOrNull() ?: 0)
            },
            label = { Text(stringResource(R.string.reps), style = MaterialTheme.typography.labelSmall) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.width(72.dp),
        )
        TextButton(onClick = onOpenRest) {
            Text("${set.restSec}s", style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.weight(1f))
        when {
            set.completed -> IconButton(onClick = { vm.uncompleteSet(set) }) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = stringResource(R.string.undo_set),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            isCurrent -> Button(
                onClick = {
                    vm.completeSet(se, set, currentLoadKg(), repsText.toIntOrNull() ?: 0)
                },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
            ) {
                Text(stringResource(R.string.finish_set))
            }

            else -> IconButton(
                onClick = {
                    vm.completeSet(se, set, currentLoadKg(), repsText.toIntOrNull() ?: 0)
                },
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

@Composable
private fun TargetDialog(
    planned: PlannedSet,
    onSave: (Int?, Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    var minText by remember { mutableStateOf(planned.targetRepsMin?.toString() ?: "") }
    var maxText by remember { mutableStateOf(planned.targetRepsMax?.toString() ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.target_reps)) },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = minText,
                    onValueChange = { minText = it.filter(Char::isDigit) },
                    label = { Text(stringResource(R.string.target_min)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(110.dp),
                )
                OutlinedTextField(
                    value = maxText,
                    onValueChange = { maxText = it.filter(Char::isDigit) },
                    label = { Text(stringResource(R.string.target_max_optional)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(110.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(minText.toIntOrNull(), maxText.toIntOrNull())
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
