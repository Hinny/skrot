package dev.hinny.skrot.ui.exercises

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import dev.hinny.skrot.AppContainer
import dev.hinny.skrot.R
import dev.hinny.skrot.data.model.Equipment
import dev.hinny.skrot.data.model.Exercise
import dev.hinny.skrot.data.model.ExerciseGroup
import dev.hinny.skrot.data.model.MeasurementType
import dev.hinny.skrot.data.model.MuscleGroup
import dev.hinny.skrot.data.model.SetType
import dev.hinny.skrot.data.model.SetWithContext
import dev.hinny.skrot.data.model.WeightUnit
import dev.hinny.skrot.data.prefs.Settings
import dev.hinny.skrot.domain.OneRepMax
import dev.hinny.skrot.domain.Units
import dev.hinny.skrot.ui.Routes
import dev.hinny.skrot.ui.charts.LineChart
import dev.hinny.skrot.ui.common.displayName
import dev.hinny.skrot.ui.common.equipmentLabel
import dev.hinny.skrot.ui.common.muscleLabel
import dev.hinny.skrot.ui.containerViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseDetailViewModel(
    private val container: AppContainer,
    private val exerciseId: Long,
) : ViewModel() {
    private val db = container.db
    val exercise = MutableStateFlow<Exercise?>(null)
    val sets = MutableStateFlow<List<SetWithContext>>(emptyList())
    val groups = MutableStateFlow<List<ExerciseGroup>>(emptyList())
    val gyms = MutableStateFlow<Map<Long, String>>(emptyMap())

    init {
        viewModelScope.launch {
            db.exerciseDao().observeById(exerciseId).collect { exercise.value = it }
        }
        viewModelScope.launch {
            db.sessionDao().observeSetsForExercise(exerciseId).collect { sets.value = it }
        }
        viewModelScope.launch {
            db.exerciseDao().observeGroups().collect { groups.value = it }
        }
        viewModelScope.launch {
            db.gymDao().observeAll().collect { all -> gyms.value = all.associate { it.id to it.name } }
        }
    }

    fun update(transform: (Exercise) -> Exercise) {
        viewModelScope.launch {
            exercise.value?.let { db.exerciseDao().update(transform(it)) }
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            exercise.value?.let { db.exerciseDao().delete(it) }
            onDone()
        }
    }

    /** Clones this exercise (prefab or custom) into a new custom exercise. */
    fun clone(nameSuffix: String, onDone: (Long) -> Unit) {
        viewModelScope.launch {
            val source = exercise.value ?: return@launch
            val id = db.exerciseDao().insert(
                source.copy(
                    id = 0,
                    nameEn = "${source.nameEn} $nameSuffix",
                    nameSv = "${source.nameSv} $nameSuffix",
                    isCustom = true,
                    nextTimeNote = "",
                )
            )
            onDone(id)
        }
    }
}

@Composable
fun ExerciseDetailScreen(
    container: AppContainer,
    settings: Settings,
    nav: NavHostController,
    exerciseId: Long,
) {
    val vm = containerViewModel(container, key = "exercise_$exerciseId") { c, _ ->
        ExerciseDetailViewModel(c, exerciseId)
    }
    val exercise by vm.exercise.collectAsState()
    val sets by vm.sets.collectAsState()
    val groups by vm.groups.collectAsState()
    val gyms by vm.gyms.collectAsState()
    val e = exercise ?: return
    var groupMenu by remember { mutableStateOf(false) }
    var gymFilter by remember { mutableStateOf<Long?>(null) }

    val isMachine = e.measurementType == MeasurementType.MACHINE_LEVEL
    val nonWarmup = sets.filter { it.set.setType != SetType.WARMUP }
    val chartSets =
        if (isMachine && gymFilter != null) nonWarmup.filter { it.sessionGymId == gymFilter }
        else nonWarmup
    val zone = ZoneId.systemDefault()
    val dateFormat = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }
    val unitLabel = if (settings.unit == WeightUnit.KG) "kg" else "lbs"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(modifier = Modifier.padding(top = 12.dp)) {
                Text(
                    e.displayName(),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                )
                val copySuffix = stringResource(R.string.clone_suffix)
                IconButton(onClick = {
                    vm.clone(copySuffix) { id -> nav.navigate(Routes.exercise(id)) }
                }) {
                    Icon(Icons.Filled.ContentCopy, stringResource(R.string.clone_exercise))
                }
                if (e.isCustom) {
                    IconButton(onClick = { vm.delete { nav.popBackStack() } }) {
                        Icon(Icons.Filled.Delete, stringResource(R.string.delete))
                    }
                }
            }
        }

        // Measurement type — editable at any time
        item {
            Text(stringResource(R.string.measurement_type), style = MaterialTheme.typography.titleSmall)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                FilterChip(
                    selected = e.measurementType == MeasurementType.WEIGHT_KG,
                    onClick = { vm.update { it.copy(measurementType = MeasurementType.WEIGHT_KG) } },
                    label = { Text(stringResource(R.string.measurement_weight)) },
                )
                FilterChip(
                    selected = e.measurementType == MeasurementType.MACHINE_LEVEL,
                    onClick = { vm.update { it.copy(measurementType = MeasurementType.MACHINE_LEVEL) } },
                    label = { Text(stringResource(R.string.measurement_level)) },
                )
                FilterChip(
                    selected = e.measurementType == MeasurementType.BODYWEIGHT,
                    onClick = { vm.update { it.copy(measurementType = MeasurementType.BODYWEIGHT) } },
                    label = { Text(stringResource(R.string.measurement_bodyweight)) },
                )
            }
        }

        // Muscle groups: one primary + any number of secondary
        item {
            Text(stringResource(R.string.primary_muscle), style = MaterialTheme.typography.titleSmall)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                MuscleGroup.entries.forEach { m ->
                    FilterChip(
                        selected = e.muscleGroup == m,
                        onClick = {
                            vm.update { ex ->
                                ex.copy(muscleGroup = m, secondaryMuscles = ex.secondaryMuscles - m)
                            }
                        },
                        label = { Text(muscleLabel(m)) },
                    )
                }
            }
        }
        item {
            Text(stringResource(R.string.secondary_muscles), style = MaterialTheme.typography.titleSmall)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                MuscleGroup.entries.filter { it != e.muscleGroup }.forEach { m ->
                    FilterChip(
                        selected = m in e.secondaryMuscles,
                        onClick = {
                            vm.update { ex ->
                                ex.copy(
                                    secondaryMuscles =
                                        if (m in ex.secondaryMuscles) ex.secondaryMuscles - m
                                        else ex.secondaryMuscles + m
                                )
                            }
                        },
                        label = { Text(muscleLabel(m)) },
                    )
                }
            }
        }

        // Equipment: any number of pieces can be required at once
        item {
            Text(stringResource(R.string.equipment), style = MaterialTheme.typography.titleSmall)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                Equipment.entries.forEach { eq ->
                    FilterChip(
                        selected = eq in e.equipment,
                        onClick = {
                            vm.update { ex ->
                                ex.copy(
                                    equipment =
                                        if (eq in ex.equipment) ex.equipment - eq
                                        else ex.equipment + eq
                                )
                            }
                        },
                        label = { Text(equipmentLabel(eq)) },
                    )
                }
            }
        }

        if (e.measurementType == MeasurementType.BODYWEIGHT) {
            item {
                var factorText by remember(e.id) { mutableStateOf(e.bodyweightFactor.toString()) }
                OutlinedTextField(
                    value = factorText,
                    onValueChange = {
                        factorText = it.filter(Char::isDigit)
                        factorText.toIntOrNull()?.let { f ->
                            vm.update { ex -> ex.copy(bodyweightFactor = f.coerceIn(0, 200)) }
                        }
                    },
                    label = { Text(stringResource(R.string.bodyweight_factor)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
            }
        }

        item {
            var incText by remember(e.id) {
                mutableStateOf(e.progressionIncrement?.let(Units::formatValue) ?: "")
            }
            OutlinedTextField(
                value = incText,
                onValueChange = { text ->
                    incText = text.filter { it.isDigit() || it == '.' || it == ',' }
                    vm.update {
                        it.copy(progressionIncrement = incText.replace(',', '.').toDoubleOrNull())
                    }
                },
                label = { Text(stringResource(R.string.progression_increment_override)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Interchangeable-exercise group
        item {
            Text(stringResource(R.string.exercise_group), style = MaterialTheme.typography.titleSmall)
            OutlinedButton(onClick = { groupMenu = true }) {
                Text(
                    groups.find { it.id == e.groupId }
                        ?.let { if (Locale.getDefault().language == "sv") it.nameSv else it.nameEn }
                        ?: stringResource(R.string.no_group)
                )
            }
            DropdownMenu(expanded = groupMenu, onDismissRequest = { groupMenu = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.no_group)) },
                    onClick = { vm.update { it.copy(groupId = null) }; groupMenu = false },
                )
                groups.forEach { g ->
                    DropdownMenuItem(
                        text = {
                            Text(if (Locale.getDefault().language == "sv") g.nameSv else g.nameEn)
                        },
                        onClick = { vm.update { it.copy(groupId = g.id) }; groupMenu = false },
                    )
                }
            }
        }

        item {
            var noteText by remember(e.id) { mutableStateOf(e.nextTimeNote) }
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it; vm.update { ex -> ex.copy(nextTimeNote = it) } },
                label = { Text(stringResource(R.string.next_time_note)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            var notesText by remember(e.id) { mutableStateOf(e.notes) }
            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it; vm.update { ex -> ex.copy(notes = it) } },
                label = { Text(stringResource(R.string.notes)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Best set + charts
        item {
            val best = chartSets.maxByOrNull {
                when (e.measurementType) {
                    MeasurementType.BODYWEIGHT -> it.set.load * 1000 + it.set.reps
                    else -> it.set.load
                }
            }
            if (best != null) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(stringResource(R.string.best_set), style = MaterialTheme.typography.titleSmall)
                        Text(
                            formatSet(best, e.measurementType, settings.unit),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                }
            }
        }

        item {
            // Per-gym history filter is gated on equipment: anything gym-bound
            // (i.e. not equipment-free) can differ between gyms.
            val gymScoped = e.equipment.any { it != Equipment.NONE } || isMachine
            if (gymScoped && gyms.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    FilterChip(
                        selected = gymFilter == null,
                        onClick = { gymFilter = null },
                        label = { Text(stringResource(R.string.all_gyms)) },
                    )
                    gyms.forEach { (id, name) ->
                        FilterChip(
                            selected = gymFilter == id,
                            onClick = { gymFilter = id },
                            label = { Text(name) },
                        )
                    }
                }
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(stringResource(R.string.load_over_time), style = MaterialTheme.typography.titleSmall)
                    LineChart(
                        points = chartSets.groupBy { it.sessionId }.map { (_, s) ->
                            s.first().sessionDate to s.maxOf {
                                if (e.measurementType == MeasurementType.BODYWEIGHT &&
                                    s.none { x -> x.set.load > 0 }
                                ) it.set.reps.toDouble() else it.set.load
                            }
                        },
                        valueFormatter = { Units.formatValue(it) },
                    )
                }
            }
        }

        if (e.measurementType == MeasurementType.WEIGHT_KG) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(stringResource(R.string.estimated_1rm), style = MaterialTheme.typography.titleSmall)
                        LineChart(
                            points = chartSets.groupBy { it.sessionId }.mapNotNull { (_, s) ->
                                val best = s.mapNotNull {
                                    OneRepMax.epley(it.set.load, it.set.reps)
                                }.maxOrNull() ?: return@mapNotNull null
                                s.first().sessionDate to best
                            },
                            valueFormatter = {
                                Units.formatValue(
                                    Units.toDisplay(it, settings.unit, MeasurementType.WEIGHT_KG)
                                )
                            },
                        )
                    }
                }
            }
        }

        // Full history (all logged sets, warmups included)
        item {
            Text(stringResource(R.string.history), style = MaterialTheme.typography.titleMedium)
        }
        val bySession = sets.groupBy { it.sessionId }.entries.sortedByDescending { it.value.first().sessionDate }
        items(bySession.size) { i ->
            val (_, sessionSets) = bySession[i].toPair()
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Row {
                        Text(
                            Instant.ofEpochMilli(sessionSets.first().sessionDate)
                                .atZone(zone).toLocalDate().format(dateFormat),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.weight(1f),
                        )
                        sessionSets.first().sessionGymId?.let { gymId ->
                            Text(
                                gyms[gymId] ?: "",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                    sessionSets.sortedBy { it.set.position }.forEach { s ->
                        Row {
                            Text(
                                when (s.set.setType) {
                                    SetType.WARMUP -> stringResource(R.string.set_marker_warmup)
                                    SetType.DROP_SET -> stringResource(R.string.set_marker_drop)
                                    SetType.FAILURE -> stringResource(R.string.set_marker_failure)
                                    SetType.STANDARD -> ""
                                },
                                modifier = Modifier.width(24.dp),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                formatSet(s, e.measurementType, settings.unit),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(40.dp)) }
    }
}

private fun formatSet(
    s: SetWithContext,
    measurement: MeasurementType,
    unit: WeightUnit,
): String = when (measurement) {
    MeasurementType.MACHINE_LEVEL -> "L${s.set.load.toInt()} × ${s.set.reps}"
    MeasurementType.BODYWEIGHT ->
        if (s.set.load != 0.0) {
            val display = Units.toDisplay(s.set.load, unit, measurement)
            val sign = if (s.set.load > 0) "+" else ""
            "$sign${Units.formatValue(display)} × ${s.set.reps}"
        } else "${s.set.reps} reps"

    MeasurementType.WEIGHT_KG -> {
        val display = Units.toDisplay(s.set.load, unit, measurement)
        "${Units.formatValue(display)} ${if (unit == WeightUnit.KG) "kg" else "lbs"} × ${s.set.reps}"
    }
}
