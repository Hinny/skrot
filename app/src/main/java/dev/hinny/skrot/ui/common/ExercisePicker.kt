package dev.hinny.skrot.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import dev.hinny.skrot.data.model.MeasurementType
import dev.hinny.skrot.data.model.MuscleGroup
import dev.hinny.skrot.domain.ExerciseSearch

/** What a quick-created exercise consists of. */
data class NewExercise(
    val name: String,
    val muscle: MuscleGroup,
    val equipment: List<Equipment>,
    val measurement: MeasurementType,
)

/**
 * Exercise picker with search; "New exercise" opens the full creation dialog.
 * The same creation dialog is used everywhere an exercise can be created, so
 * the flow is identical mid-session and from the Exercises tab.
 */
@Composable
fun ExercisePickerDialog(
    exercises: List<Exercise>,
    onPick: (Exercise) -> Unit,
    onCreate: (NewExercise) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }

    if (creating) {
        CreateExerciseDialog(
            onSave = { onCreate(it) },
            onDismiss = { creating = false },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pick_exercise)) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.search_exercises)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                val filtered = ExerciseSearch.search(
                    exercises = exercises,
                    query = query,
                    muscleNames = searchMuscleNames(),
                    equipmentNames = searchEquipmentNames(),
                )
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
            }
        },
        confirmButton = {
            TextButton(onClick = { creating = true }) {
                Text(stringResource(R.string.new_exercise))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/** Localized + English searchable names for every muscle group. */
@Composable
fun searchMuscleNames(): Map<MuscleGroup, List<String>> =
    MuscleGroup.entries.associateWith { listOf(muscleLabel(it), it.name.replace('_', ' ')) }

/** Localized + English searchable names for every equipment type. */
@Composable
fun searchEquipmentNames(): Map<Equipment, List<String>> =
    Equipment.entries.associateWith { listOf(equipmentLabel(it), it.name.replace('_', ' ')) }

/**
 * Standalone "New exercise" dialog: name, primary muscle, equipment (several
 * allowed) and measurement type.
 */
@Composable
fun CreateExerciseDialog(
    onSave: (NewExercise) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var muscle by remember { mutableStateOf(MuscleGroup.CHEST) }
    var equipment by remember { mutableStateOf(listOf<Equipment>()) }
    var measurement by remember { mutableStateOf(MeasurementType.WEIGHT_KG) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_exercise)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.exercise_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(stringResource(R.string.primary_muscle), style = MaterialTheme.typography.labelLarge)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    MuscleGroup.entries.forEach { m ->
                        FilterChip(
                            selected = muscle == m,
                            onClick = { muscle = m },
                            label = { Text(muscleLabel(m)) },
                        )
                    }
                }

                Text(stringResource(R.string.equipment), style = MaterialTheme.typography.labelLarge)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    Equipment.entries.forEach { eq ->
                        FilterChip(
                            selected = eq in equipment,
                            onClick = {
                                equipment =
                                    if (eq in equipment) equipment - eq else equipment + eq
                            },
                            label = { Text(equipmentLabel(eq)) },
                        )
                    }
                }

                Text(stringResource(R.string.measurement_type), style = MaterialTheme.typography.labelLarge)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    FilterChip(
                        selected = measurement == MeasurementType.WEIGHT_KG,
                        onClick = { measurement = MeasurementType.WEIGHT_KG },
                        label = { Text(stringResource(R.string.measurement_weight)) },
                    )
                    FilterChip(
                        selected = measurement == MeasurementType.MACHINE_LEVEL,
                        onClick = { measurement = MeasurementType.MACHINE_LEVEL },
                        label = { Text(stringResource(R.string.measurement_level)) },
                    )
                    FilterChip(
                        selected = measurement == MeasurementType.BODYWEIGHT,
                        onClick = { measurement = MeasurementType.BODYWEIGHT },
                        label = { Text(stringResource(R.string.measurement_bodyweight)) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(NewExercise(name.trim(), muscle, equipment, measurement))
                    }
                },
            ) { Text(stringResource(R.string.save)) }
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
