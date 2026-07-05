package dev.hinny.skrot.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.hinny.skrot.AppContainer
import dev.hinny.skrot.R
import dev.hinny.skrot.data.model.Exercise
import dev.hinny.skrot.data.model.MeasurementType
import dev.hinny.skrot.data.model.SetType
import dev.hinny.skrot.data.model.SetWithContext
import dev.hinny.skrot.data.model.WeightUnit
import dev.hinny.skrot.data.db.MuscleGroupSets
import dev.hinny.skrot.data.prefs.Settings
import dev.hinny.skrot.domain.OneRepMax
import dev.hinny.skrot.domain.Units
import dev.hinny.skrot.ui.charts.CalendarHeatmap
import dev.hinny.skrot.ui.charts.HorizontalBarChart
import dev.hinny.skrot.ui.charts.LineChart
import dev.hinny.skrot.ui.common.displayName
import dev.hinny.skrot.ui.common.muscleLabel
import dev.hinny.skrot.ui.containerViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class StatsRange(val labelRes: Int, val days: Long?) {
    M1(R.string.range_1m, 30),
    M3(R.string.range_3m, 91),
    M6(R.string.range_6m, 182),
    Y1(R.string.range_1y, 365),
    ALL(R.string.range_all, null),
}

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModel(private val container: AppContainer) : ViewModel() {
    private val db = container.db

    val range = MutableStateFlow(StatsRange.M3)
    val exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val selectedExercise = MutableStateFlow<Exercise?>(null)
    val exerciseSets = MutableStateFlow<List<SetWithContext>>(emptyList())
    val sessionDates = MutableStateFlow<List<Long>>(emptyList())
    val muscleSets = MutableStateFlow<List<MuscleGroupSets>>(emptyList())
    val gyms = MutableStateFlow<Map<Long, String>>(emptyMap())
    /** Gym filter for machine-level charts; null = all gyms. */
    val gymFilter = MutableStateFlow<Long?>(null)

    private fun fromMs(r: StatsRange): Long =
        r.days?.let { System.currentTimeMillis() - it * 86_400_000L } ?: 0L

    init {
        viewModelScope.launch {
            db.exerciseDao().observeAll().collect { all ->
                exercises.value = all
                if (selectedExercise.value == null) {
                    // default to the first exercise that has data
                    for (e in all) {
                        if (db.sessionDao().setsForExercise(e.id).isNotEmpty()) {
                            selectExercise(e)
                            break
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            range.flatMapLatest { db.sessionDao().observeSessionDates(fromMs(it)) }
                .collect { sessionDates.value = it }
        }
        viewModelScope.launch {
            range.flatMapLatest { db.sessionDao().observeMuscleGroupSets(fromMs(it)) }
                .collect { muscleSets.value = it }
        }
        viewModelScope.launch {
            selectedExercise.flatMapLatest { e ->
                if (e == null) MutableStateFlow(emptyList())
                else db.sessionDao().observeSetsForExercise(e.id)
            }.collect { exerciseSets.value = it }
        }
        viewModelScope.launch {
            db.gymDao().observeAll().collect { all ->
                gyms.value = all.associate { it.id to it.name }
                if (gymFilter.value == null) {
                    gymFilter.value = all.find { it.isDefault }?.id
                }
            }
        }
    }

    fun selectExercise(e: Exercise) {
        selectedExercise.value = e
    }
}

@Composable
fun StatsScreen(container: AppContainer, settings: Settings) {
    val vm = containerViewModel(container) { c, _ -> StatsViewModel(c) }
    val range by vm.range.collectAsState()
    val exercises by vm.exercises.collectAsState()
    val selected by vm.selectedExercise.collectAsState()
    val sets by vm.exerciseSets.collectAsState()
    val dates by vm.sessionDates.collectAsState()
    val muscles by vm.muscleSets.collectAsState()
    val gyms by vm.gyms.collectAsState()
    val gymFilter by vm.gymFilter.collectAsState()
    var exerciseMenu by remember { mutableStateOf(false) }

    val zone = ZoneId.systemDefault()
    val fromMs = range.days?.let { System.currentTimeMillis() - it * 86_400_000L } ?: 0L
    val rangedSets = sets.filter { it.sessionDate >= fromMs && it.set.setType != SetType.WARMUP }
    val isMachine = selected?.measurementType == MeasurementType.MACHINE_LEVEL
    val machineFiltered =
        if (isMachine && gymFilter != null) rangedSets.filter { it.sessionGymId == gymFilter }
        else rangedSets

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(stringResource(R.string.tab_stats), style = MaterialTheme.typography.headlineMedium)

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            StatsRange.entries.forEach { r ->
                FilterChip(
                    selected = range == r,
                    onClick = { vm.range.value = r },
                    label = { Text(stringResource(r.labelRes)) },
                )
            }
        }

        // Training frequency heatmap
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text(stringResource(R.string.training_frequency), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                CalendarHeatmap(
                    countsByDay = dates.groupingBy {
                        Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
                    }.eachCount(),
                )
            }
        }

        // Muscle group distribution
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    stringResource(R.string.muscle_distribution),
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(8.dp))
                HorizontalBarChart(
                    items = muscles.map { muscleLabel(it.muscleGroup) to it.setCount },
                )
            }
        }

        // Per-exercise charts
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { exerciseMenu = true }) {
                    Text(selected?.displayName() ?: stringResource(R.string.pick_exercise))
                }
                DropdownMenu(expanded = exerciseMenu, onDismissRequest = { exerciseMenu = false }) {
                    exercises.forEach { e ->
                        DropdownMenuItem(
                            text = { Text(e.displayName()) },
                            onClick = { vm.selectExercise(e); exerciseMenu = false },
                        )
                    }
                }

                if (isMachine && gyms.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                    ) {
                        FilterChip(
                            selected = gymFilter == null,
                            onClick = { vm.gymFilter.value = null },
                            label = { Text(stringResource(R.string.all_gyms)) },
                        )
                        gyms.forEach { (id, name) ->
                            FilterChip(
                                selected = gymFilter == id,
                                onClick = { vm.gymFilter.value = id },
                                label = { Text(name) },
                            )
                        }
                    }
                }

                val exercise = selected
                if (exercise != null) {
                    Text(
                        stringResource(R.string.load_over_time),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    val topSetPerSession = machineFiltered
                        .groupBy { it.sessionId }
                        .mapNotNull { (_, sessionSets) ->
                            val date = sessionSets.first().sessionDate
                            val value = when (exercise.measurementType) {
                                MeasurementType.BODYWEIGHT ->
                                    if (sessionSets.any { it.set.load > 0 }) {
                                        sessionSets.maxOf { it.set.load }
                                    } else {
                                        sessionSets.maxOf { it.set.reps }.toDouble()
                                    }

                                else -> sessionSets.maxOf { it.set.load }
                            }
                            date to value
                        }
                    LineChart(
                        points = topSetPerSession,
                        valueFormatter = { value ->
                            when (exercise.measurementType) {
                                MeasurementType.MACHINE_LEVEL -> value.toInt().toString()
                                MeasurementType.BODYWEIGHT -> Units.formatValue(value)
                                else -> Units.formatValue(
                                    Units.toDisplay(value, settings.unit, exercise.measurementType)
                                )
                            }
                        },
                    )

                    if (exercise.measurementType == MeasurementType.WEIGHT_KG) {
                        Text(
                            stringResource(R.string.estimated_1rm),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        val e1rmPerSession = machineFiltered
                            .groupBy { it.sessionId }
                            .mapNotNull { (_, sessionSets) ->
                                val best = sessionSets
                                    .mapNotNull { OneRepMax.epley(it.set.load, it.set.reps) }
                                    .maxOrNull() ?: return@mapNotNull null
                                sessionSets.first().sessionDate to best
                            }
                        LineChart(
                            points = e1rmPerSession,
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
        Spacer(Modifier.height(40.dp))
    }
}
