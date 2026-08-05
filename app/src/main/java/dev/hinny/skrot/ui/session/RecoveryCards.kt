package dev.hinny.skrot.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.hinny.skrot.R
import dev.hinny.skrot.data.model.RoutineDay
import dev.hinny.skrot.data.model.RoutineWithDays

/**
 * Offers a recovery workout, letting the day be chosen rather than always
 * starting at day one. Shared by the Home dashboard and the Session tab.
 *
 * @param suggestedDay preselected day; the next one in order when carrying on
 *   from a recovery session, otherwise the program's first day.
 */
@Composable
fun RecoveryStartCard(
    title: String,
    body: String,
    routines: List<RoutineWithDays>,
    suggestedDay: (RoutineWithDays) -> RoutineDay?,
    onStart: (RoutineWithDays, RoutineDay) -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
) {
    if (routines.isEmpty()) return
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (onDismiss != null) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, stringResource(R.string.dismiss))
                    }
                }
            }
            Text(body, style = MaterialTheme.typography.bodySmall)
            routines.forEach { program ->
                RecoveryProgramRow(
                    program = program,
                    suggested = suggestedDay(program),
                    onStart = onStart,
                )
            }
        }
    }
}

@Composable
private fun RecoveryProgramRow(
    program: RoutineWithDays,
    suggested: RoutineDay?,
    onStart: (RoutineWithDays, RoutineDay) -> Unit,
) {
    val days = program.sortedDays
    if (days.isEmpty()) return
    var selected by remember(program.routine.id, suggested?.id) {
        mutableStateOf(suggested ?: days.first())
    }
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(program.routine.name, style = MaterialTheme.typography.titleSmall)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = { expanded = true }) { Text(selected.name) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                days.forEach { day ->
                    DropdownMenuItem(
                        text = { Text(day.name) },
                        onClick = { selected = day; expanded = false },
                    )
                }
            }
            Button(onClick = { onStart(program, selected) }) {
                Icon(Icons.Filled.PlayArrow, null)
                Text(stringResource(R.string.start))
            }
        }
    }
}
