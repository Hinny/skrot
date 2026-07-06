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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
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
import dev.hinny.skrot.data.model.Gym
import dev.hinny.skrot.data.model.RoutineDay
import dev.hinny.skrot.data.model.RoutineWithDays
import dev.hinny.skrot.domain.GymResolution
import dev.hinny.skrot.ui.Routes
import dev.hinny.skrot.ui.common.displayName
import dev.hinny.skrot.ui.home.HomeViewModel
import dev.hinny.skrot.ui.home.PendingStart
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
    gyms: List<Gym>,
    startTarget: Pair<RoutineWithDays?, RoutineDay?>?,
    onClearTarget: () -> Unit,
) {
    var pending by remember { mutableStateOf<PendingStart?>(null) }

    startTarget?.let { (routine, day) ->
        StartWorkoutDialog(
            gyms = gyms,
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
