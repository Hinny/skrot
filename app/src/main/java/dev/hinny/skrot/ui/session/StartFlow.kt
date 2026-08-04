package dev.hinny.skrot.ui.session

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import dev.hinny.skrot.R
import dev.hinny.skrot.data.model.Exercise
import dev.hinny.skrot.data.model.Gym
import dev.hinny.skrot.data.model.RoutineDay
import dev.hinny.skrot.data.model.RoutineWithDays
import dev.hinny.skrot.data.prefs.Settings
import dev.hinny.skrot.domain.GymResolution
import dev.hinny.skrot.ui.Routes
import dev.hinny.skrot.ui.common.ExercisePickerDialog
import dev.hinny.skrot.ui.common.displayName
import dev.hinny.skrot.ui.home.HomeViewModel
import dev.hinny.skrot.ui.home.PendingStart
import dev.hinny.skrot.ui.home.StartItem
import kotlinx.coroutines.launch

/**
 * Hosts the start-workout dialog chain (gym selection, then exercise resolution)
 * and navigates into the workout when a session is created. Shared between the
 * Home dashboard and the Session tab.
 *
 * @param startTarget (routine, day) to start; (null, null) = empty workout;
 *   null = flow idle.
 */
@Composable
fun StartFlowHost(
    vm: HomeViewModel,
    nav: NavHostController,
    settings: Settings,
    gyms: List<Gym>,
    startTarget: Pair<RoutineWithDays?, RoutineDay?>?,
    onClearTarget: () -> Unit,
) {
    var pending by remember { mutableStateOf<PendingStart?>(null) }

    startTarget?.let { (routine, day) ->
        StartWorkoutDialog(
            gyms = gyms,
            onCreateGym = { name, onCreated -> vm.createGym(name, onCreated) },
            onDismiss = onClearTarget,
            onConfirm = { gymId, temporary ->
                onClearTarget()
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
                        if (needsInput || settings.planExercisesBeforeStart) {
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
            showAll = settings.planExercisesBeforeStart,
            // Only a saved gym has an availability list to add to; a temporary
            // visit deliberately keeps nothing.
            gymName = prepared.gymId
                ?.takeIf { !prepared.temporaryVisit }
                ?.let { id -> gyms.find { it.id == id }?.name },
            onLinkEquivalent = { original, picked -> vm.linkAsEquivalent(original, picked) },
            onAddToGym = { picked ->
                prepared.gymId?.let { vm.addExerciseToGym(it, picked.id) }
            },
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

/**
 * A pick awaiting the "remember this?" question. [picked] equal to the planned
 * exercise means it was kept rather than swapped.
 */
private data class SwapConfirmation(
    val plannedId: Long,
    val original: Exercise,
    val picked: Exercise,
)

/** One-line availability note for an exercise at the selected gym. */
@Composable
private fun ResolutionStatus(resolution: GymResolution) {
    val colors = MaterialTheme.colorScheme
    when (resolution) {
        is GymResolution.Available -> Text(
            stringResource(R.string.status_available),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )

        is GymResolution.AutoSwapped -> Text(
            stringResource(R.string.status_auto_swapped, resolution.to.displayName()),
            style = MaterialTheme.typography.bodySmall,
            color = colors.tertiary,
        )

        is GymResolution.Choice -> Text(
            stringResource(R.string.status_choose),
            style = MaterialTheme.typography.bodySmall,
            color = colors.tertiary,
        )

        // NoEquivalent already gets its own, louder line below.
        is GymResolution.NoEquivalent -> Unit
    }
}

/** Picker for any day of any routine ("choose other workout"). */
@Composable
fun WorkoutPickerDialog(
    routines: List<RoutineWithDays>,
    onDismiss: () -> Unit,
    onPick: (RoutineWithDays, RoutineDay) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        title = { Text(stringResource(R.string.choose_workout)) },
        text = {
            LazyColumn {
                routines.forEach { r ->
                    items(r.sortedDays.size) { i ->
                        val day = r.sortedDays[i]
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onPick(r, day) }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                        ) {
                            Text(day.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "${r.routine.name} · ",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun StartWorkoutDialog(
    gyms: List<Gym>,
    onCreateGym: (name: String, onCreated: (Long) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (gymId: Long?, temporary: Boolean) -> Unit,
) {
    var selectedGym by remember {
        mutableStateOf(gyms.find { it.isDefault }?.id ?: gyms.firstOrNull()?.id)
    }
    var temporary by remember { mutableStateOf(false) }
    var addingGym by remember { mutableStateOf(false) }
    var newGymName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.start_workout)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.gym))
                gyms.forEach { gym ->
                    FilterChip(
                        selected = !temporary && selectedGym == gym.id,
                        onClick = {
                            temporary = false
                            selectedGym = gym.id
                        },
                        label = { Text(gym.name) },
                    )
                }
                if (addingGym) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = newGymName,
                            onValueChange = { newGymName = it },
                            label = { Text(stringResource(R.string.new_gym)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            enabled = newGymName.isNotBlank(),
                            onClick = {
                                onCreateGym(newGymName.trim()) { id ->
                                    selectedGym = id
                                    temporary = false
                                }
                                addingGym = false
                                newGymName = ""
                            },
                        ) { Text(stringResource(R.string.add)) }
                    }
                } else {
                    TextButton(onClick = { addingGym = true }) {
                        Icon(Icons.Filled.Add, null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.new_gym))
                    }
                }

                HorizontalDivider()

                FilterChip(
                    selected = temporary,
                    onClick = {
                        temporary = !temporary
                        if (temporary) selectedGym = null
                        else selectedGym =
                            gyms.find { it.isDefault }?.id ?: gyms.firstOrNull()?.id
                    },
                    label = { Text(stringResource(R.string.temporary_gym)) },
                    leadingIcon = { Icon(Icons.Filled.Place, null) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ),
                )
                Text(
                    stringResource(R.string.temporary_gym_hint),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = temporary || selectedGym != null || gyms.isEmpty(),
                onClick = { onConfirm(if (temporary) null else selectedGym, temporary) },
            ) {
                Text(stringResource(R.string.start))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/**
 * Exercise resolution before a workout starts. Normally only the exercises the
 * gym forces a decision about are listed; with the planning setting on,
 * [showAll] lists the whole day with each exercise's availability and set plan,
 * so the workout can be walked through before the first rep.
 */
@Composable
private fun ResolveExercisesDialog(
    pending: PendingStart,
    showAll: Boolean,
    gymName: String?,
    onLinkEquivalent: (original: Exercise, picked: Exercise) -> Unit,
    onAddToGym: (picked: Exercise) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (picks: Map<Long, Long?>, alwaysUse: Set<Long>) -> Unit,
) {
    val picks = remember { mutableStateOf(mapOf<Long, Long?>()) }
    val always = remember { mutableStateOf(setOf<Long>()) }
    // The item whose replacement is being searched for, then the pair awaiting
    // an answer to "keep this swap in mind?".
    var searchingFor by remember { mutableStateOf<StartItem?>(null) }
    var confirmSwap by remember { mutableStateOf<SwapConfirmation?>(null) }
    // Added to the gym during this flow; keeps the question from being asked
    // twice for the same exercise, since the availability snapshot is fixed.
    var addedToGym by remember { mutableStateOf(setOf<Long>()) }

    searchingFor?.let { item ->
        ExercisePickerDialog(
            exercises = pending.allExercises,
            title = stringResource(R.string.swap_exercise),
            availableIds = pending.availableExerciseIds.takeIf { it.isNotEmpty() },
            onPick = { picked ->
                searchingFor = null
                picks.value = picks.value + (item.planned.planned.id to picked.id)
                confirmSwap = SwapConfirmation(item.planned.planned.id, item.planned.exercise, picked)
            },
            onDismiss = { searchingFor = null },
        )
    }

    confirmSwap?.let { (plannedId, original, picked) ->
        // Keeping the original at a gym that doesn't list it is still news about
        // the gym, so that case gets the availability question with no
        // equivalence question attached.
        val canLink = picked.id != original.id &&
            (original.groupId == null || picked.groupId != original.groupId)
        val canAddToGym = gymName != null &&
            picked.id !in pending.availableExerciseIds &&
            picked.id !in addedToGym
        // The narrowest memory of the three: this program day, at this gym only.
        val canAlwaysUse = gymName != null && picked.id != original.id
        if (!canLink && !canAddToGym && !canAlwaysUse) {
            confirmSwap = null
            return@let
        }
        var linkEquivalent by remember(picked.id) { mutableStateOf(true) }
        var addToGym by remember(picked.id) { mutableStateOf(true) }
        var alwaysUseHere by remember(picked.id) { mutableStateOf(true) }
        val willLink = canLink && linkEquivalent
        val willAdd = canAddToGym && addToGym
        val willAlwaysUse = canAlwaysUse && alwaysUseHere

        AlertDialog(
            onDismissRequest = { confirmSwap = null },
            title = { Text(stringResource(R.string.remember_swap_title)) },
            text = {
                Column {
                    if (canAddToGym) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = addToGym, onCheckedChange = { addToGym = it })
                            Text(
                                stringResource(
                                    R.string.add_to_gym_body,
                                    picked.displayName(),
                                    gymName.orEmpty(),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    if (canAlwaysUse) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = alwaysUseHere,
                                onCheckedChange = { alwaysUseHere = it },
                            )
                            Text(
                                stringResource(
                                    R.string.always_use_here_body,
                                    picked.displayName(),
                                    original.displayName(),
                                    gymName.orEmpty(),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    if (canLink) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = linkEquivalent,
                                onCheckedChange = { linkEquivalent = it },
                            )
                            Text(
                                stringResource(
                                    R.string.flag_equivalent_body,
                                    picked.displayName(),
                                    original.displayName(),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            },
            // One button, reading what it will actually do: untick everything
            // and it plainly says so rather than leaving two ways to say no.
            confirmButton = {
                TextButton(onClick = {
                    if (willLink) onLinkEquivalent(original, picked)
                    if (willAdd) {
                        onAddToGym(picked)
                        addedToGym = addedToGym + picked.id
                    }
                    always.value =
                        if (willAlwaysUse) always.value + plannedId
                        else always.value - plannedId
                    confirmSwap = null
                }) {
                    Text(
                        stringResource(
                            if (willLink || willAdd || willAlwaysUse) R.string.save
                            else R.string.skip
                        )
                    )
                }
            },
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (showAll) R.string.plan_exercises else R.string.resolve_exercises
                )
            )
        },
        text = {
            LazyColumn {
                val needing = pending.items.filter {
                    showAll ||
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
                        if (showAll) {
                            val sets = item.planned.sortedSets
                            val target = sets.firstNotNullOfOrNull { it.targetRepsMin }
                            Text(
                                if (target == null) {
                                    stringResource(R.string.plan_sets, sets.size)
                                } else {
                                    stringResource(R.string.plan_sets_reps, sets.size, target)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            ResolutionStatus(item.resolution)
                        }
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

                                    else -> pending.allExercises.find { it.id == chosen }
                                        ?.displayName().orEmpty()
                                }
                            )
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.keep_original)) },
                                onClick = {
                                    picks.value = picks.value + (plannedId to item.planned.exercise.id)
                                    expanded = false
                                    // Doing it here anyway says the gym has it.
                                    confirmSwap = SwapConfirmation(plannedId, item.planned.exercise, item.planned.exercise)
                                },
                            )
                            // Known equivalents come first, set apart from the
                            // open-ended options below them.
                            if (item.options.isNotEmpty()) {
                                HorizontalDivider()
                                Text(
                                    stringResource(R.string.equivalents_header),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                )
                                item.options.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.displayName()) },
                                        onClick = {
                                            picks.value = picks.value + (plannedId to option.id)
                                            expanded = false
                                            confirmSwap = SwapConfirmation(plannedId, item.planned.exercise, option)
                                        },
                                    )
                                }
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.pick_another_exercise)) },
                                onClick = {
                                    expanded = false
                                    searchingFor = item
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.skip_exercise)) },
                                onClick = {
                                    picks.value = picks.value + (plannedId to null)
                                    expanded = false
                                },
                            )
                        }
                        // "Always use this here" is asked in the confirmation
                        // that follows a pick, along with the other two ways to
                        // remember it — one place, not a checkbox out here too.
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
