package dev.hinny.skrot.ui.body

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.hinny.skrot.AppContainer
import dev.hinny.skrot.R
import dev.hinny.skrot.data.model.BodyMetric
import dev.hinny.skrot.data.model.WeightUnit
import dev.hinny.skrot.data.prefs.Settings
import dev.hinny.skrot.domain.Units
import dev.hinny.skrot.ui.charts.LineChart
import dev.hinny.skrot.ui.containerViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val DATE_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

/** A single tracked number, for the chart's metric picker. */
enum class BodyMetricKind(val labelRes: Int) {
    WEIGHT(R.string.body_weight),
    WAIST(R.string.waist),
    CHEST(R.string.chest_measure),
    ARMS(R.string.arms),
    THIGHS(R.string.thighs),
    HIPS(R.string.hips);

    /** The value this kind reads off an entry, in its stored unit. */
    fun valueOf(metric: BodyMetric): Double? = when (this) {
        WEIGHT -> metric.weightKg
        WAIST -> metric.waistCm
        CHEST -> metric.chestCm
        ARMS -> metric.armsCm
        THIGHS -> metric.thighsCm
        HIPS -> metric.hipsCm
    }

    /** Only weight is stored in kg; the rest are centimetres and never converted. */
    val isWeight: Boolean get() = this == WEIGHT
}

class BodyViewModel(private val container: AppContainer) : ViewModel() {
    val metrics = MutableStateFlow<List<BodyMetric>>(emptyList())

    init {
        viewModelScope.launch {
            container.db.bodyMetricDao().observeAll().collect { metrics.value = it }
        }
    }

    fun save(metric: BodyMetric) {
        viewModelScope.launch {
            val dao = container.db.bodyMetricDao()
            if (metric.id == 0L) dao.insert(metric) else dao.update(metric)
        }
    }

    fun delete(metric: BodyMetric) {
        viewModelScope.launch { container.db.bodyMetricDao().delete(metric) }
    }

    /** Puts a deleted entry back, id and all, for the undo snackbar. */
    fun restore(metric: BodyMetric) {
        viewModelScope.launch { container.db.bodyMetricDao().insert(metric) }
    }
}

@Composable
fun BodyScreen(container: AppContainer, settings: Settings) {
    val vm = containerViewModel(container) { c, _ -> BodyViewModel(c) }
    val metrics by vm.metrics.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<BodyMetric?>(null) }
    var chartKind by remember { mutableStateOf(BodyMetricKind.WEIGHT) }
    val zone = remember { ZoneId.systemDefault() }
    val unitLabel = if (settings.unit == WeightUnit.KG) "kg" else "lbs"
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deletedMsg = stringResource(R.string.measurement_deleted)
    val undoLabel = stringResource(R.string.undo)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Filled.Add, null)
                Text(stringResource(R.string.log_body_weight))
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.body_metrics),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            stringResource(R.string.measurements_over_time),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        // Waist, chest, arms, thighs and hips were stored and
                        // listed but never plotted, which is the one thing a
                        // measurement is for.
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 6.dp),
                        ) {
                            BodyMetricKind.entries.forEach { kind ->
                                val hasData = metrics.any { kind.valueOf(it) != null }
                                FilterChip(
                                    selected = chartKind == kind,
                                    enabled = hasData,
                                    onClick = { chartKind = kind },
                                    label = { Text(stringResource(kind.labelRes)) },
                                )
                            }
                        }
                        LineChart(
                            points = metrics
                                .mapNotNull { m -> chartKind.valueOf(m)?.let { m.date to it } }
                                .sortedBy { it.first },
                            valueFormatter = { value ->
                                val shown =
                                    if (chartKind.isWeight && settings.unit == WeightUnit.LBS) {
                                        Units.kgToLbs(value)
                                    } else {
                                        value
                                    }
                                val suffix = if (chartKind.isWeight) unitLabel else "cm"
                                "${Units.formatValue(shown)} $suffix"
                            },
                        )
                    }
                }
            }
            items(metrics.size) { i ->
                val m = metrics[i]
                Card(
                    Modifier
                        .fillMaxWidth()
                        // Fixing a typo used to mean deleting the entry and
                        // typing the whole thing again.
                        .clickable { editing = m },
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                Instant.ofEpochMilli(m.date).atZone(zone).toLocalDateTime()
                                    .format(DATE_TIME_FORMAT),
                                style = MaterialTheme.typography.labelMedium,
                            )
                            m.weightKg?.let {
                                val display =
                                    if (settings.unit == WeightUnit.KG) it else Units.kgToLbs(it)
                                Text(
                                    "${Units.formatValue(display)} $unitLabel",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                            val extras = buildList {
                                m.waistCm?.let { add(stringResource(R.string.waist) + " $it cm") }
                                m.chestCm?.let { add(stringResource(R.string.chest_measure) + " $it cm") }
                                m.armsCm?.let { add(stringResource(R.string.arms) + " $it cm") }
                                m.thighsCm?.let { add(stringResource(R.string.thighs) + " $it cm") }
                                m.hipsCm?.let { add(stringResource(R.string.hips) + " $it cm") }
                            }
                            if (extras.isNotEmpty()) {
                                Text(
                                    extras.joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                        // Used to delete silently on a single tap. Undoable
                        // rather than confirmed: one row is trivial to put back.
                        IconButton(onClick = {
                            scope.launch {
                                vm.delete(m)
                                val result = snackbar.showSnackbar(
                                    message = deletedMsg,
                                    actionLabel = undoLabel,
                                    duration = SnackbarDuration.Long,
                                )
                                if (result == SnackbarResult.ActionPerformed) vm.restore(m)
                            }
                        }) {
                            Icon(Icons.Filled.Delete, stringResource(R.string.delete))
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showAdd) {
        BodyMetricDialog(
            unit = settings.unit,
            onSave = { vm.save(it); showAdd = false },
            onDismiss = { showAdd = false },
        )
    }
    editing?.let { metric ->
        BodyMetricDialog(
            unit = settings.unit,
            initial = metric,
            onSave = { vm.save(it); editing = null },
            onDismiss = { editing = null },
        )
    }
}

/**
 * Body-metric entry dialog, shared between the Body page and Statistics. Pass
 * [initial] to edit an existing entry rather than create a new one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyMetricDialog(
    unit: WeightUnit,
    onSave: (BodyMetric) -> Unit,
    onDismiss: () -> Unit,
    initial: BodyMetric? = null,
) {
    fun show(value: Double?): String = value?.let(Units::formatValue).orEmpty()

    var weight by remember(initial) {
        mutableStateOf(
            show(
                initial?.weightKg?.let { if (unit == WeightUnit.LBS) Units.kgToLbs(it) else it }
            )
        )
    }
    var waist by remember(initial) { mutableStateOf(show(initial?.waistCm)) }
    var chest by remember(initial) { mutableStateOf(show(initial?.chestCm)) }
    var arms by remember(initial) { mutableStateOf(show(initial?.armsCm)) }
    var thighs by remember(initial) { mutableStateOf(show(initial?.thighsCm)) }
    var hips by remember(initial) { mutableStateOf(show(initial?.hipsCm)) }
    // Defaults to now; editable so a measurement can be backdated, and so the
    // time of day is on record for anyone reading trends out of the chart.
    var timestamp by remember(initial) {
        mutableStateOf(
            initial
                ?.let {
                    Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDateTime()
                }
                ?: LocalDateTime.now().withSecond(0).withNano(0)
        )
    }
    var pickingDate by remember { mutableStateOf(false) }
    var pickingTime by remember { mutableStateOf(false) }

    fun parse(text: String): Double? = text.replace(',', '.').toDoubleOrNull()

    if (pickingDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = timestamp.toLocalDate()
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    // The picker works in UTC; only the calendar date it returns
                    // is meaningful, so read it back the same way.
                    state.selectedDateMillis?.let { millis ->
                        val picked = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                        timestamp = LocalDateTime.of(picked, timestamp.toLocalTime())
                    }
                    pickingDate = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { pickingDate = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        ) {
            DatePicker(state = state)
        }
    }
    if (pickingTime) {
        val state = rememberTimePickerState(
            initialHour = timestamp.hour,
            initialMinute = timestamp.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { pickingTime = false },
            title = { Text(stringResource(R.string.time)) },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    timestamp = timestamp.withHour(state.hour).withMinute(state.minute)
                    pickingTime = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { pickingTime = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (initial == null) R.string.log_body_weight
                    else R.string.edit_measurement
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(onClick = { pickingDate = true }) {
                        Text(timestamp.toLocalDate().format(DATE_FORMAT))
                    }
                    OutlinedButton(onClick = { pickingTime = true }) {
                        Text(timestamp.toLocalTime().format(TIME_FORMAT))
                    }
                }
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = {
                        Text(
                            stringResource(R.string.body_weight) +
                                " (${if (unit == WeightUnit.KG) "kg" else "lbs"})"
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                listOf<Triple<String, String, (String) -> Unit>>(
                    Triple(stringResource(R.string.waist), waist) { waist = it },
                    Triple(stringResource(R.string.chest_measure), chest) { chest = it },
                    Triple(stringResource(R.string.arms), arms) { arms = it },
                    Triple(stringResource(R.string.thighs), thighs) { thighs = it },
                    Triple(stringResource(R.string.hips), hips) { hips = it },
                ).forEach { (label, value, setter) ->
                    OutlinedTextField(
                        value = value,
                        onValueChange = setter,
                        label = { Text("$label (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val weightKg = parse(weight)?.let {
                    if (unit == WeightUnit.LBS) Units.lbsToKg(it) else it
                }
                onSave(
                    BodyMetric(
                        // Keeping the id is what makes this an edit rather than
                        // a second entry at the same timestamp.
                        id = initial?.id ?: 0L,
                        date = timestamp.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        weightKg = weightKg,
                        waistCm = parse(waist),
                        chestCm = parse(chest),
                        armsCm = parse(arms),
                        thighsCm = parse(thighs),
                        hipsCm = parse(hips),
                    )
                )
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
