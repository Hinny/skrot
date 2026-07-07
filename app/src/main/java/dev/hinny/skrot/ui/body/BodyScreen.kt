package dev.hinny.skrot.ui.body

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
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
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class BodyViewModel(private val container: AppContainer) : ViewModel() {
    val metrics = MutableStateFlow<List<BodyMetric>>(emptyList())

    init {
        viewModelScope.launch {
            container.db.bodyMetricDao().observeAll().collect { metrics.value = it }
        }
    }

    fun add(metric: BodyMetric) {
        viewModelScope.launch { container.db.bodyMetricDao().insert(metric) }
    }

    fun delete(metric: BodyMetric) {
        viewModelScope.launch { container.db.bodyMetricDao().delete(metric) }
    }
}

@Composable
fun BodyScreen(container: AppContainer, settings: Settings) {
    val vm = containerViewModel(container) { c, _ -> BodyViewModel(c) }
    val metrics by vm.metrics.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    val dateFormat = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }
    val zone = remember { ZoneId.systemDefault() }
    val unitLabel = if (settings.unit == WeightUnit.KG) "kg" else "lbs"

    Scaffold(
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
                            stringResource(R.string.body_weight_over_time),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Spacer(Modifier.height(8.dp))
                        LineChart(
                            points = metrics
                                .filter { it.weightKg != null }
                                .map { it.date to it.weightKg!! }
                                .sortedBy { it.first },
                            valueFormatter = {
                                Units.formatValue(
                                    if (settings.unit == WeightUnit.KG) it else Units.kgToLbs(it)
                                )
                            },
                        )
                    }
                }
            }
            items(metrics.size) { i ->
                val m = metrics[i]
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                Instant.ofEpochMilli(m.date).atZone(zone).toLocalDate()
                                    .format(dateFormat),
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
                        IconButton(onClick = { vm.delete(m) }) {
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
            onSave = { vm.add(it); showAdd = false },
            onDismiss = { showAdd = false },
        )
    }
}

/** Body-metric entry dialog, shared between the Body page and Statistics. */
@Composable
fun BodyMetricDialog(
    unit: WeightUnit,
    onSave: (BodyMetric) -> Unit,
    onDismiss: () -> Unit,
) {
    var weight by remember { mutableStateOf("") }
    var waist by remember { mutableStateOf("") }
    var chest by remember { mutableStateOf("") }
    var arms by remember { mutableStateOf("") }
    var thighs by remember { mutableStateOf("") }
    var hips by remember { mutableStateOf("") }

    fun parse(text: String): Double? = text.replace(',', '.').toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.log_body_weight)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                        date = System.currentTimeMillis(),
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
