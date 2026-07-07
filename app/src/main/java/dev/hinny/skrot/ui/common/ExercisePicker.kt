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
import dev.hinny.skrot.data.model.Equipment
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
                                    exerciseSubtitle(e),
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
fun equipmentLabel(e: Equipment): String = stringResource(
    when (e) {
        Equipment.BARBELL -> R.string.equip_barbell
        Equipment.EZ_BAR -> R.string.equip_ez_bar
        Equipment.DUMBBELL -> R.string.equip_dumbbell
        Equipment.KETTLEBELL -> R.string.equip_kettlebell
        Equipment.WEIGHT_PLATE -> R.string.equip_weight_plate
        Equipment.MACHINE -> R.string.equip_machine
        Equipment.MULTI_MACHINE -> R.string.equip_multi_machine
        Equipment.SMITH_MACHINE -> R.string.equip_smith_machine
        Equipment.CABLE -> R.string.equip_cable
        Equipment.BENCH -> R.string.equip_bench
        Equipment.PULLUP_BAR -> R.string.equip_pullup_bar
        Equipment.DIP_STATION -> R.string.equip_dip_station
        Equipment.RACK -> R.string.equip_rack
        Equipment.BAND -> R.string.equip_band
        Equipment.NONE -> R.string.equip_none
        Equipment.OTHER -> R.string.equip_other
    }
)

/** "muscle · equipment1 + equipment2" subtitle used in exercise lists. */
@Composable
fun exerciseSubtitle(e: Exercise): String {
    val muscles = (listOf(e.muscleGroup) + e.secondaryMuscles)
        .map { muscleLabel(it) }
        .joinToString(", ")
    val equipment = e.equipment.map { equipmentLabel(it) }.joinToString(" + ")
    return if (equipment.isBlank()) muscles else "$muscles · $equipment"
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
