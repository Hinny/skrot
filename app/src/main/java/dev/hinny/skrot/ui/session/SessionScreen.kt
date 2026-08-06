package dev.hinny.skrot.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import dev.hinny.skrot.AppContainer
import dev.hinny.skrot.R
import dev.hinny.skrot.data.model.RoutineDay
import dev.hinny.skrot.data.model.RoutineWithDays
import dev.hinny.skrot.data.prefs.Settings
import dev.hinny.skrot.ui.Routes
import dev.hinny.skrot.ui.containerViewModel
import dev.hinny.skrot.ui.home.HomeViewModel
import dev.hinny.skrot.ui.home.RecoverySection

/**
 * The Session tab: resume the workout in progress, or start one (scheduled day,
 * any other day, or an empty session).
 */
@Composable
fun SessionScreen(container: AppContainer, settings: Settings, nav: NavHostController) {
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
            OpenSessionCard(onResume = { nav.navigate(Routes.workout(open.id)) })
        } else {
            val active = state.activeRoutine
            if (active != null) {
                NextWorkoutCard(
                    routine = active,
                    nextDay = state.nextDay,
                    lastPerformed = state.nextDay?.let { state.lastByDay[it.id] },
                    onStart = { r, day -> startTarget = r to day },
                    onChooseOther = { showPicker = true },
                )
            } else {
                NoActiveProgramCard(
                    onGoToPrograms = { nav.navigate(Routes.PROGRAMS) },
                    onChooseWorkout = { showPicker = true },
                )
            }

            RecoverySection(
                state = state,
                onDismissComeback = { vm.comebackDismissed.value = true },
                onStart = { r, day -> startTarget = r to day },
                alwaysOffer = settings.alwaysOfferRecovery,
            )

            OutlinedButton(onClick = { showPicker = true }) {
                Text(stringResource(R.string.start_another_workout))
            }
            OutlinedButton(onClick = { startTarget = null to null }) {
                Text(stringResource(R.string.start_empty_workout))
            }
        }
    }

    if (showPicker) {
        WorkoutPickerDialog(
            routines = state.allRoutines,
            lastByDay = state.lastByDay,
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
        settings = settings,
        gyms = state.gyms,
        startTarget = startTarget,
        onClearTarget = { startTarget = null },
    )
}
