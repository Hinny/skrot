package dev.hinny.skrot.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.hinny.skrot.R
import dev.hinny.skrot.data.model.Exercise
import dev.hinny.skrot.data.model.MuscleGroup

/**
 * Exercise picker with search and instant custom-exercise creation
 * (name + muscle group is enough to save).
 */
@Composable
fun ExercisePickerDialog(
    exercises: List<Exercise>,
    onPick: (Exercise) -> Unit,
    onCreate: (name: String, muscle: MuscleGroup) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newMuscle by remember { mutableStateOf(MuscleGroup.CHEST) }
    var muscleMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pick_exercise)) },
        text = {
            Column {
                if (!creating) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text(stringResource(R.string.search)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val filtered = exercises.filter {
                        query.isBlank() || it.displayName().contains(query, ignoreCase = true)
                    }
                    LazyColumn(Modifier.height(320.dp)) {
                        items(filtered.size) { i ->
                            val e = filtered[i]
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onPick(e) }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                            ) {
                                Text(e.displayName(), style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "${muscleLabel(e.muscleGroup)} · ${e.equipment.name.lowercase()}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text(stringResource(R.string.exercise_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(Modifier.padding(top = 8.dp)) {
                        OutlinedButton(onClick = { muscleMenu = true }) {
                            Text(muscleLabel(newMuscle))
                        }
                        DropdownMenu(
                            expanded = muscleMenu,
                            onDismissRequest = { muscleMenu = false },
                        ) {
                            MuscleGroup.entries.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(muscleLabel(m)) },
                                    onClick = { newMuscle = m; muscleMenu = false },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (creating) {
                TextButton(
                    onClick = { if (newName.isNotBlank()) onCreate(newName.trim(), newMuscle) },
                ) { Text(stringResource(R.string.save)) }
            } else {
                TextButton(onClick = { creating = true }) {
                    Text(stringResource(R.string.new_exercise))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
fun muscleLabel(m: MuscleGroup): String = stringResource(
    when (m) {
        MuscleGroup.CHEST -> R.string.muscle_chest
        MuscleGroup.BACK -> R.string.muscle_back
        MuscleGroup.SHOULDERS -> R.string.muscle_shoulders
        MuscleGroup.BICEPS -> R.string.muscle_biceps
        MuscleGroup.TRICEPS -> R.string.muscle_triceps
        MuscleGroup.FOREARMS -> R.string.muscle_forearms
        MuscleGroup.ABS -> R.string.muscle_abs
        MuscleGroup.QUADS -> R.string.muscle_quads
        MuscleGroup.HAMSTRINGS -> R.string.muscle_hamstrings
        MuscleGroup.GLUTES -> R.string.muscle_glutes
        MuscleGroup.CALVES -> R.string.muscle_calves
        MuscleGroup.FULL_BODY -> R.string.muscle_full_body
    }
)
