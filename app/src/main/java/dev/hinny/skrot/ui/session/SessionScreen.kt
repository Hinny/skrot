package dev.hinny.skrot.ui.session

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.navigation.NavHostController
import dev.hinny.skrot.AppContainer
import dev.hinny.skrot.R
import dev.hinny.skrot.data.model.RoutineDay
import dev.hinny.skrot.data.model.RoutineWithDays
import dev.hinny.skrot.ui.Routes
import dev.hinny.skrot.ui.common.lastPerformedText
import dev.hinny.skrot.ui.common.vector
import dev.hinny.skrot.ui.containerViewModel
import dev.hinny.skrot.ui.home.HomeViewModel

/**
 * The Session tab: resume the workout in progress, or start one (scheduled day,
 * any other day, or an empty session).
 */
@Composable
fun SessionScreen(container: AppContainer, nav: NavHostController) {
    val vm = containerViewModel(container) { c, _ -> HomeViewModel(c) }
    val state by vm.uiState.collectAsState()
    var startTarget by remember { mutableStateOf<Pair<RoutineWithDays?, RoutineDay?>?>(null) }
    var showPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.tab_session), style = MaterialTheme.typography.headlineMedium)

        val open = state.openSession
        if (open != null) {
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
        } else {
            val active = state.activeRoutine
            if (active != null) {
                val nextDay = state.nextDay
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
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
            } else {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.no_active_program))
                        TextButton(onClick = { nav.navigate(Routes.PROGRAMS) }) {
                            Text(stringResource(R.string.go_to_programs))
                        }
                        OutlinedButton(onClick = { showPicker = true }) {
                            Text(stringResource(R.string.choose_workout))
                        }
                    }
                }
            }

            OutlinedButton(onClick = { startTarget = null to null }) {
                Text(stringResource(R.string.start_empty_workout))
            }
        }
    }

    if (showPicker) {
        WorkoutPickerDialog(
            routines = state.allRoutines,
            onDismiss = { showPicker = false },
            onPick = { r, day ->
                showPicker = false
                startTarget = r to day
            },
        )
    }

    StartFlowHost(
        vm = vm,
        nav = nav,
        gyms = state.gyms,
        startTarget = startTarget,
        onClearTarget = { startTarget = null },
    )
}
