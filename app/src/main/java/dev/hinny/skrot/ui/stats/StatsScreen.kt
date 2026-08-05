package dev.hinny.skrot.ui.stats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import dev.hinny.skrot.data.model.BodyMetric
import dev.hinny.skrot.data.model.Exercise
import dev.hinny.skrot.data.model.MeasurementType
import dev.hinny.skrot.data.model.SetType
import dev.hinny.skrot.data.model.SetWithContext
import dev.hinny.skrot.data.model.WeightUnit
import dev.hinny.skrot.data.model.WorkoutSession
import dev.hinny.skrot.data.db.MuscleGroupSets
import dev.hinny.skrot.data.prefs.Settings
import dev.hinny.skrot.domain.OneRepMax
import dev.hinny.skrot.domain.Units
import dev.hinny.skrot.ui.Routes
import dev.hinny.skrot.ui.body.BodyMetricDialog
import dev.hinny.skrot.ui.charts.HorizontalBarChart
import dev.hinny.skrot.ui.charts.LineChart
import dev.hinny.skrot.ui.charts.MonthCalendarHeatmap
import dev.hinny.skrot.ui.charts.VerticalBarChart
import dev.hinny.skrot.ui.charts.WeekCalendarHeatmap
import dev.hinny.skrot.ui.common.ExercisePickerDialog
import dev.hinny.skrot.ui.common.displayName
import dev.hinny.skrot.ui.common.muscleLabel
import dev.hinny.skrot.ui.containerViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

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
    /** Every completed set in the selected range, for the range-wide summaries. */
    val rangeSets = MutableStateFlow<List<SetWithContext>>(emptyList())
    /** Only exercises that have actually been logged are worth charting. */
    val exerciseIdsWithData = MutableStateFlow<Set<Long>>(emptySet())
    val bodyMetrics = MutableStateFlow<List<BodyMetric>>(emptyList())
    val finishedSessions = MutableStateFlow<List<WorkoutSession>>(emptyList())
    val gyms = MutableStateFlow<Map<Long, String>>(emptyMap())
    /** Gym filter for machine-level charts; null = all gyms. */
    val gymFilter = MutableStateFlow<Long?>(null)

    private fun fromMs(r: StatsRange): Long =
        r.days?.let { System.currentTimeMillis() - it * 86_400_000L } ?: 0L

    init {
        viewModelScope.launch {
            // No default selection: picking the first exercise with data looks
            // like a real answer to a question nobody asked.
            container.observeExercises().collect { all -> exercises.value = all }
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
            range.flatMapLatest { db.sessionDao().observeCompletedSetsFrom(fromMs(it)) }
                .collect { rangeSets.value = it }
        }
        viewModelScope.launch {
            db.sessionDao().observeExerciseIdsWithData()
                .collect { exerciseIdsWithData.value = it.toSet() }
        }
        viewModelScope.launch {
            db.bodyMetricDao().observeAll().collect { bodyMetrics.value = it }
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
        viewModelScope.launch {
            db.sessionDao().observeFinishedSessions()
                .collect { finishedSessions.value = it.sortedByDescending { s -> s.startedAt } }
        }
    }

    fun selectExercise(e: Exercise) {
        selectedExercise.value = e
    }

    fun addBodyMetric(metric: BodyMetric) {
        viewModelScope.launch { db.bodyMetricDao().insert(metric) }
    }
}

@Composable
fun StatsScreen(container: AppContainer, settings: Settings, nav: NavHostController) {
    val vm = containerViewModel(container) { c, _ -> StatsViewModel(c) }
    val range by vm.range.collectAsState()
    val exercises by vm.exercises.collectAsState()
    val selected by vm.selectedExercise.collectAsState()
    val sets by vm.exerciseSets.collectAsState()
    val dates by vm.sessionDates.collectAsState()
    val muscles by vm.muscleSets.collectAsState()
    val gyms by vm.gyms.collectAsState()
    val gymFilter by vm.gymFilter.collectAsState()
    val finished by vm.finishedSessions.collectAsState()
    val rangeSets by vm.rangeSets.collectAsState()
    val idsWithData by vm.exerciseIdsWithData.collectAsState()
    val bodyMetrics by vm.bodyMetrics.collectAsState()
    var showExercisePicker by remember { mutableStateOf(false) }
    var showBodyDialog by remember { mutableStateOf(false) }

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

        // Range summary: the numbers you want before any chart
        val exercisesById = exercises.associateBy { it.id }
        val rangeSessions = finished.filter { it.startedAt >= fromMs }
        val workingSets = rangeSets.filter { it.set.setType != SetType.WARMUP }
        val rangeVolumeKg = workingSets.sumOf { setVolumeKg(it, exercisesById) }
        val avgDurationMs = rangeSessions
            .mapNotNull { s -> s.endedAt?.let { it - s.startedAt } }
            .takeIf { it.isNotEmpty() }
            ?.average()
        val rangeWeeks = (range.days ?: run {
            val first = finished.minOfOrNull { it.startedAt } ?: System.currentTimeMillis()
            ((System.currentTimeMillis() - first) / 86_400_000L).coerceAtLeast(1)
        }) / 7.0
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.stats_overview),
                    style = MaterialTheme.typography.titleSmall,
                )
                Row(Modifier.fillMaxWidth()) {
                    StatTile(
                        stringResource(R.string.stat_sessions),
                        rangeSessions.size.toString(),
                        Modifier.weight(1f),
                    )
                    StatTile(
                        stringResource(R.string.stat_sets),
                        workingSets.size.toString(),
                        Modifier.weight(1f),
                    )
                    StatTile(
                        stringResource(R.string.stat_volume),
                        Units.formatValue(
                            Units.toDisplay(rangeVolumeKg, settings.unit, MeasurementType.WEIGHT_KG)
                        ),
                        Modifier.weight(1f),
                    )
                }
                Row(Modifier.fillMaxWidth()) {
                    StatTile(
                        stringResource(R.string.stat_avg_duration),
                        avgDurationMs?.let { formatDuration(it.toLong()) } ?: "-",
                        Modifier.weight(1f),
                    )
                    StatTile(
                        stringResource(R.string.stat_per_week),
                        Units.formatValue(
                            if (rangeWeeks > 0) rangeSessions.size / rangeWeeks else 0.0
                        ),
                        Modifier.weight(1f),
                    )
                    StatTile(
                        stringResource(R.string.stat_streak),
                        weekStreak(finished.map { it.startedAt }, zone).toString(),
                        Modifier.weight(1f),
                    )
                }
            }
        }

        // Training frequency heatmap
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text(stringResource(R.string.training_frequency), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                val countsByDay = dates
                    .groupingBy { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
                    .eachCount()
                // Short ranges get one row per week; a year or more would be an
                // unreadably tall grid that way, so those get one row per month.
                if (range == StatsRange.Y1 || range == StatsRange.ALL) {
                    val today = LocalDate.now()
                    val earliest = countsByDay.keys.minOrNull()
                    val months = when {
                        range == StatsRange.Y1 || earliest == null -> 12
                        else -> ChronoUnit.MONTHS
                            .between(YearMonth.from(earliest), YearMonth.from(today))
                            .toInt() + 1
                    }
                    MonthCalendarHeatmap(countsByDay, months = months.coerceIn(1, 60))
                } else {
                    WeekCalendarHeatmap(
                        countsByDay,
                        weeks = ((range.days!! + 6) / 7).toInt(),
                    )
                }
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

        // Volume per week
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    stringResource(R.string.weekly_volume),
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(8.dp))
                val weekFormat = remember { DateTimeFormatter.ofPattern("d MMM") }
                val byWeek = workingSets
                    .groupBy {
                        Instant.ofEpochMilli(it.sessionDate).atZone(zone).toLocalDate()
                            .with(DayOfWeek.MONDAY)
                    }
                    .toSortedMap()
                    .map { (monday, sets) ->
                        monday.format(weekFormat) to sets.sumOf { setVolumeKg(it, exercisesById) }
                    }
                VerticalBarChart(
                    items = byWeek,
                    valueFormatter = {
                        Units.formatValue(
                            Units.toDisplay(it, settings.unit, MeasurementType.WEIGHT_KG)
                        )
                    },
                )
            }
        }

        // Most trained exercises in the range
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    stringResource(R.string.top_exercises),
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(8.dp))
                val top = workingSets
                    .groupingBy { it.exerciseId }
                    .eachCount()
                    .entries
                    .sortedByDescending { it.value }
                    .take(8)
                    .mapNotNull { (id, count) ->
                        exercisesById[id]?.let { it.displayName() to count }
                    }
                HorizontalBarChart(items = top)
            }
        }

        // Per-exercise charts
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Same search as everywhere else, rather than a long flat menu.
                OutlinedButton(onClick = { showExercisePicker = true }) {
                    Icon(Icons.Filled.Search, null)
                    Spacer(Modifier.width(8.dp))
                    Text(selected?.displayName() ?: stringResource(R.string.pick_exercise))
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

                        Text(
                            stringResource(R.string.volume_per_session),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        val volumePerSession = machineFiltered
                            .filter { it.set.completed }
                            .groupBy { it.sessionId }
                            .map { (_, sessionSets) ->
                                sessionSets.first().sessionDate to
                                    sessionSets.sumOf { it.set.load * it.set.reps }
                            }
                        LineChart(
                            points = volumePerSession,
                            valueFormatter = {
                                Units.formatValue(
                                    Units.toDisplay(it, settings.unit, MeasurementType.WEIGHT_KG)
                                )
                            },
                        )
                    }

                    HorizontalDivider()
                    Text(
                        stringResource(R.string.personal_records),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    val prSets = sets.filter { it.set.setType != SetType.WARMUP }
                    if (prSets.isEmpty()) {
                        Text(
                            stringResource(R.string.no_data_yet),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        val loadLabel: (Double) -> String = { value ->
                            when (exercise.measurementType) {
                                MeasurementType.MACHINE_LEVEL -> value.toInt().toString()
                                else -> Units.formatValue(
                                    Units.toDisplay(
                                        value,
                                        settings.unit,
                                        exercise.measurementType,
                                    )
                                )
                            }
                        }
                        RecordRow(
                            stringResource(R.string.record_heaviest),
                            loadLabel(prSets.maxOf { it.set.load }),
                        )
                        RecordRow(
                            stringResource(R.string.record_most_reps),
                            prSets.maxOf { it.set.reps }.toString(),
                        )
                        prSets.mapNotNull { OneRepMax.epley(it.set.load, it.set.reps) }
                            .maxOrNull()
                            ?.let { RecordRow(stringResource(R.string.record_best_e1rm), loadLabel(it)) }
                        if (exercise.measurementType != MeasurementType.MACHINE_LEVEL) {
                            val bestSession = prSets
                                .groupBy { it.sessionId }
                                .values
                                .maxOfOrNull { session ->
                                    session.sumOf { it.set.load * it.set.reps }
                                }
                            bestSession?.let {
                                RecordRow(
                                    stringResource(R.string.record_best_session_volume),
                                    loadLabel(it),
                                )
                            }
                        }
                    }

                    if (exercise.measurementType == MeasurementType.BODYWEIGHT) {
                        Text(
                            stringResource(R.string.reps_per_session),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        val repsPerSession = machineFiltered
                            .filter { it.set.completed }
                            .groupBy { it.sessionId }
                            .map { (_, sessionSets) ->
                                sessionSets.first().sessionDate to
                                    sessionSets.sumOf { it.set.reps }.toDouble()
                            }
                        LineChart(
                            points = repsPerSession,
                            valueFormatter = { it.toInt().toString() },
                        )
                    }
                }
            }
        }

        // Body log: quick entry without leaving Statistics
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.body_metrics),
                    style = MaterialTheme.typography.titleSmall,
                )
                val weights = bodyMetrics
                    .filter { it.weightKg != null && it.date >= fromMs }
                    .map { it.date to it.weightKg!! }
                LineChart(
                    points = weights,
                    valueFormatter = {
                        Units.formatValue(
                            Units.toDisplay(it, settings.unit, MeasurementType.WEIGHT_KG)
                        )
                    },
                )
                OutlinedButton(onClick = { showBodyDialog = true }) {
                    Icon(Icons.Filled.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.log_body_weight))
                }
            }
        }

        // The session list itself lives under Library -> Workout history.
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { nav.navigate(Routes.HISTORY) },
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.History, null)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.session_history),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        stringResource(R.string.library_history_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(40.dp))
    }

    if (showExercisePicker) {
        ExercisePickerDialog(
            exercises = exercises.filter { it.id in idsWithData },
            onPick = { vm.selectExercise(it); showExercisePicker = false },
            onDismiss = { showExercisePicker = false },
        )
    }

    if (showBodyDialog) {
        BodyMetricDialog(
            unit = settings.unit,
            onSave = { vm.addBodyMetric(it); showBodyDialog = false },
            onDismiss = { showBodyDialog = false },
        )
    }
}

/** One number with its caption, as used by the overview grid. */
@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(end = 8.dp)) {
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RecordRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * Kilogram volume of one logged set. Machine levels aren't kilograms and
 * bodyweight needs a body weight to be meaningful, so only weight exercises
 * contribute — the same rule the per-exercise volume chart already follows.
 */
private fun setVolumeKg(set: SetWithContext, exercises: Map<Long, Exercise>): Double =
    if (exercises[set.exerciseId]?.measurementType == MeasurementType.WEIGHT_KG) {
        set.set.load * set.set.reps
    } else {
        0.0
    }

/** Consecutive weeks up to and including this one that contain a session. */
private fun weekStreak(sessionDates: List<Long>, zone: ZoneId): Int {
    if (sessionDates.isEmpty()) return 0
    val weeks = sessionDates
        .map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate().with(DayOfWeek.MONDAY) }
        .toSet()
    var monday = LocalDate.now(zone).with(DayOfWeek.MONDAY)
    // A week that isn't over yet shouldn't break the streak.
    if (monday !in weeks) monday = monday.minusWeeks(1)
    var streak = 0
    while (monday in weeks) {
        streak++
        monday = monday.minusWeeks(1)
    }
    return streak
}

private fun formatDuration(ms: Long): String {
    val minutes = ms / 60_000
    return if (minutes >= 60) "%dh %02dm".format(minutes / 60, minutes % 60) else "%dm".format(minutes)
}
