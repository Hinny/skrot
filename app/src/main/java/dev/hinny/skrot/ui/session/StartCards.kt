package dev.hinny.skrot.ui.session

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.hinny.skrot.R
import dev.hinny.skrot.data.model.RoutineDay
import dev.hinny.skrot.data.model.RoutineWithDays
import dev.hinny.skrot.ui.common.lastPerformedText
import dev.hinny.skrot.ui.common.vectorOrNull

/*
 * The three cards that answer "what am I doing next?". Home and the Session tab
 * both ask that question and used to answer it with their own copies of these,
 * which drifted: one of them had already lost a button the other kept.
 */

/** The workout already running; tapping it goes back to the logging screen. */
@Composable
fun OpenSessionCard(onResume: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onResume),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.workout_in_progress),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(stringResource(R.string.tap_to_resume))
        }
    }
}

/**
 * What the active program proposes next, and the two ways to act on it: start
 * it, or swap in another day. The proposal is never acted on without a tap —
 * that is the whole contract of the scheduling modes.
 *
 * @param lastPerformed when this day was last done, or null for never.
 */
@Composable
fun NextWorkoutCard(
    routine: RoutineWithDays,
    nextDay: RoutineDay?,
    lastPerformed: Long?,
    onStart: (RoutineWithDays, RoutineDay) -> Unit,
    onChooseOther: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                routine.routine.icon.vectorOrNull()?.let { Icon(it, null) }
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
                        "${routine.routine.name} · ${lastPerformedText(lastPerformed)}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { if (nextDay != null) onStart(routine, nextDay) },
                    enabled = nextDay != null,
                ) {
                    Icon(Icons.Filled.PlayArrow, null)
                    Text(stringResource(R.string.start))
                }
                OutlinedButton(onClick = onChooseOther) {
                    Text(stringResource(R.string.choose_other_workout))
                }
            }
        }
    }
}

/**
 * Stand-in for [NextWorkoutCard] when no program is active. There is still a
 * workout to be had — any day of any program — so the offer to pick one stays
 * on the card rather than only appearing where a program exists.
 */
@Composable
fun NoActiveProgramCard(
    onGoToPrograms: () -> Unit,
    onChooseWorkout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.no_active_program))
            TextButton(onClick = onGoToPrograms) {
                Text(stringResource(R.string.go_to_programs))
            }
            OutlinedButton(onClick = onChooseWorkout) {
                Text(stringResource(R.string.choose_workout))
            }
        }
    }
}
