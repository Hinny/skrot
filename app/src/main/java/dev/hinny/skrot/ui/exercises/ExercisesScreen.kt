package dev.hinny.skrot.ui.exercises

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import dev.hinny.skrot.AppContainer
import dev.hinny.skrot.R
import dev.hinny.skrot.data.model.Equipment
import dev.hinny.skrot.data.model.Exercise
import dev.hinny.skrot.data.model.MeasurementType
import dev.hinny.skrot.data.model.MuscleGroup
import dev.hinny.skrot.domain.ExerciseSearch
import dev.hinny.skrot.ui.Routes
import dev.hinny.skrot.ui.common.CreateExerciseDialog
import dev.hinny.skrot.ui.common.NewExercise
import dev.hinny.skrot.ui.common.displayName
import dev.hinny.skrot.ui.common.equipmentLabel
import dev.hinny.skrot.ui.common.exerciseSubtitle
import dev.hinny.skrot.ui.common.muscleLabel
import dev.hinny.skrot.ui.common.searchEquipmentNames
import dev.hinny.skrot.ui.common.searchMuscleNames
import dev.hinny.skrot.ui.containerViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** Fields to change on every selected custom exercise; null/empty = leave as-is. */
data class BulkEditExercise(
    val muscleGroup: MuscleGroup? = null,
    val measurementType: MeasurementType? = null,
    val addEquipment: Set<Equipment> = emptySet(),
    val removeEquipment: Set<Equipment> = emptySet(),
)

class ExercisesViewModel(private val container: AppContainer) : ViewModel() {
    val exercises = MutableStateFlow<List<Exercise>>(emptyList())

    init {
        viewModelScope.launch {
            container.db.exerciseDao().observeAll().collect { exercises.value = it }
        }
    }

    fun create(new: NewExercise, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = container.db.exerciseDao().insert(
                Exercise(
                    nameEn = new.name,
                    nameSv = new.name,
                    muscleGroup = new.muscle,
                    equipment = new.equipment,
                    measurementType = new.measurement,
                    isCustom = true,
                )
            )
            onCreated(id)
        }
    }

    /** Applies [edit] to every custom exercise in [ids]; built-ins are never touched. */
    fun bulkEdit(ids: Set<Long>, edit: BulkEditExercise, onDone: () -> Unit) {
        viewModelScope.launch {
            val dao = container.db.exerciseDao()
            for (id in ids) {
                val existing = dao.byId(id) ?: continue
                if (!existing.isCustom) continue
                var updated = existing
                edit.muscleGroup?.let { m ->
                    updated = updated.copy(muscleGroup = m, secondaryMuscles = updated.secondaryMuscles - m)
                }
                edit.measurementType?.let { mt -> updated = updated.copy(measurementType = mt) }
                if (edit.addEquipment.isNotEmpty() || edit.removeEquipment.isNotEmpty()) {
                    updated = updated.copy(
                        equipment = (updated.equipment + edit.addEquipment - edit.removeEquipment).distinct()
                    )
                }
                if (updated != existing) dao.update(updated)
            }
            onDone()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExercisesScreen(container: AppContainer, nav: NavHostController) {
    val vm = containerViewModel(container) { c, _ -> ExercisesViewModel(c) }
    val exercises by vm.exercises.collectAsState()
    var query by remember { mutableStateOf("") }
    var muscleFilters by remember { mutableStateOf(setOf<MuscleGroup>()) }
    var equipmentFilters by remember { mutableStateOf(setOf<Equipment>()) }
    var categoryFilter by remember { mutableStateOf<Boolean?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<Long>()) }
    var showBulkEdit by remember { mutableStateOf(false) }

    fun exitSelection() {
        selectionMode = false
        selected = emptySet()
    }

    Scaffold(
        floatingActionButton = {
            if (!selectionMode) {
                ExtendedFloatingActionButton(onClick = { showCreate = true }) {
                    Icon(Icons.Filled.Add, null)
                    Text(stringResource(R.string.new_exercise))
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            if (selectionMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { exitSelection() }) {
                        Icon(Icons.Filled.Close, stringResource(R.string.cancel))
                    }
                    Text(
                        stringResource(R.string.n_selected, selected.size),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        enabled = selected.isNotEmpty(),
                        onClick = { showBulkEdit = true },
                    ) { Text(stringResource(R.string.bulk_edit)) }
                }
            } else {
                Text(
                    stringResource(R.string.tab_exercises),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(R.string.search_exercises)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 6.dp),
            ) {
                FilterChip(
                    selected = categoryFilter == null,
                    onClick = { categoryFilter = null },
                    label = { Text(stringResource(R.string.category_all)) },
                )
                FilterChip(
                    selected = categoryFilter == false,
                    onClick = { categoryFilter = false },
                    label = { Text(stringResource(R.string.category_builtin)) },
                )
                FilterChip(
                    selected = categoryFilter == true,
                    onClick = { categoryFilter = true },
                    label = { Text(stringResource(R.string.category_custom)) },
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 6.dp),
            ) {
                MuscleGroup.entries.forEach { m ->
                    FilterChip(
                        selected = m in muscleFilters,
                        onClick = {
                            muscleFilters =
                                if (m in muscleFilters) muscleFilters - m else muscleFilters + m
                        },
                        label = { Text(muscleLabel(m)) },
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 6.dp),
            ) {
                Equipment.entries.forEach { eq ->
                    FilterChip(
                        selected = eq in equipmentFilters,
                        onClick = {
                            equipmentFilters =
                                if (eq in equipmentFilters) equipmentFilters - eq
                                else equipmentFilters + eq
                        },
                        label = { Text(equipmentLabel(eq)) },
                    )
                }
            }

            val filtered = ExerciseSearch.search(
                exercises = exercises,
                query = query,
                muscleFilters = muscleFilters,
                equipmentFilters = equipmentFilters,
                customFilter = categoryFilter,
                muscleNames = searchMuscleNames(),
                equipmentNames = searchEquipmentNames(),
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(filtered.size) { i ->
                    val e = filtered[i]
                    val canSelect = e.isCustom
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (selectionMode) {
                                        if (canSelect) {
                                            selected =
                                                if (e.id in selected) selected - e.id else selected + e.id
                                        }
                                    } else {
                                        nav.navigate(Routes.exercise(e.id))
                                    }
                                },
                                onLongClick = {
                                    if (canSelect) {
                                        selectionMode = true
                                        selected = selected + e.id
                                    }
                                },
                            ),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (selectionMode) {
                                Checkbox(
                                    checked = e.id in selected,
                                    onCheckedChange = { checked ->
                                        selected = if (checked) selected + e.id else selected - e.id
                                    },
                                    enabled = canSelect,
                                    modifier = Modifier.padding(start = 4.dp),
                                )
                            }
                            Column(
                                Modifier
                                    .weight(1f)
                                    .padding(12.dp),
                            ) {
                                Row {
                                    Text(
                                        e.displayName(),
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.weight(1f),
                                    )
                                    if (e.isCustom) {
                                        Text(
                                            stringResource(R.string.custom_badge),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.tertiary,
                                        )
                                    }
                                }
                                Text(
                                    exerciseSubtitle(e),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showCreate) {
        CreateExerciseDialog(
            onSave = { new ->
                showCreate = false
                vm.create(new) { id -> nav.navigate(Routes.exercise(id)) }
            },
            onDismiss = { showCreate = false },
        )
    }

    if (showBulkEdit) {
        BulkEditExerciseDialog(
            count = selected.size,
            onApply = { edit ->
                showBulkEdit = false
                vm.bulkEdit(selected, edit) { exitSelection() }
            },
            onDismiss = { showBulkEdit = false },
        )
    }
}

@Composable
private fun BulkEditExerciseDialog(
    count: Int,
    onApply: (BulkEditExercise) -> Unit,
    onDismiss: () -> Unit,
) {
    var setMuscle by remember { mutableStateOf(false) }
    var muscle by remember { mutableStateOf(MuscleGroup.CHEST) }
    var setMeasurement by remember { mutableStateOf(false) }
    var measurement by remember { mutableStateOf(MeasurementType.WEIGHT_KG) }
    var setAddEquipment by remember { mutableStateOf(false) }
    var addEquipment by remember { mutableStateOf(setOf<Equipment>()) }
    var setRemoveEquipment by remember { mutableStateOf(false) }
    var removeEquipment by remember { mutableStateOf(setOf<Equipment>()) }

    @Composable
    fun SectionToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = checked, onCheckedChange = onChange)
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.bulk_edit_title, count)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SectionToggle(stringResource(R.string.bulk_set_primary_muscle), setMuscle) { setMuscle = it }
                if (setMuscle) {
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
                }

                SectionToggle(stringResource(R.string.bulk_set_measurement_type), setMeasurement) {
                    setMeasurement = it
                }
                if (setMeasurement) {
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

                SectionToggle(stringResource(R.string.bulk_add_equipment), setAddEquipment) {
                    setAddEquipment = it
                }
                if (setAddEquipment) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                    ) {
                        Equipment.entries.forEach { eq ->
                            FilterChip(
                                selected = eq in addEquipment,
                                onClick = {
                                    addEquipment =
                                        if (eq in addEquipment) addEquipment - eq else addEquipment + eq
                                },
                                label = { Text(equipmentLabel(eq)) },
                            )
                        }
                    }
                }

                SectionToggle(stringResource(R.string.bulk_remove_equipment), setRemoveEquipment) {
                    setRemoveEquipment = it
                }
                if (setRemoveEquipment) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                    ) {
                        Equipment.entries.forEach { eq ->
                            FilterChip(
                                selected = eq in removeEquipment,
                                onClick = {
                                    removeEquipment =
                                        if (eq in removeEquipment) removeEquipment - eq else removeEquipment + eq
                                },
                                label = { Text(equipmentLabel(eq)) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = setMuscle || setMeasurement || setAddEquipment || setRemoveEquipment,
                onClick = {
                    onApply(
                        BulkEditExercise(
                            muscleGroup = if (setMuscle) muscle else null,
                            measurementType = if (setMeasurement) measurement else null,
                            addEquipment = if (setAddEquipment) addEquipment else emptySet(),
                            removeEquipment = if (setRemoveEquipment) removeEquipment else emptySet(),
                        )
                    )
                },
            ) { Text(stringResource(R.string.apply)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
