package dev.hinny.skrot.ui.importexport

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import dev.hinny.skrot.data.backup.BackupCodec
import dev.hinny.skrot.data.backup.CsvExporter
import dev.hinny.skrot.data.backup.JefitCsvParser
import dev.hinny.skrot.data.backup.JefitImporter
import dev.hinny.skrot.data.model.WeightUnit
import dev.hinny.skrot.ui.containerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

sealed class BackupMessage {
    data class Info(val textRes: Int) : BackupMessage()
    data class Error(val textRes: Int, val detail: String = "") : BackupMessage()
    data class JefitDone(val summary: JefitImporter.Summary) : BackupMessage()
}

class BackupViewModel(private val container: AppContainer) : ViewModel() {
    private val app = container
    val message = MutableStateFlow<BackupMessage?>(null)
    val jefitPreview = MutableStateFlow<Pair<JefitCsvParser.Result, JefitImporter.Preview>?>(null)

    /** Raw CSV text kept while the user confirms unit + preview. */
    private var pendingCsv: String? = null
    val needsUnitChoice = MutableStateFlow(false)

    fun exportJson(uri: Uri, resolver: android.content.ContentResolver) {
        viewModelScope.launch {
            try {
                val backup = app.backupManager.createBackup()
                val text = BackupCodec.encode(backup)
                withContext(Dispatchers.IO) {
                    resolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) }
                }
                message.value = BackupMessage.Info(R.string.export_done)
            } catch (e: Exception) {
                message.value = BackupMessage.Error(R.string.export_failed, e.message ?: "")
            }
        }
    }

    fun importJson(uri: Uri, resolver: android.content.ContentResolver) {
        viewModelScope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    resolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                } ?: throw IllegalArgumentException("Could not read file")
                val backup = BackupCodec.decode(text)
                app.backupManager.restore(backup)
                message.value = BackupMessage.Info(R.string.import_done)
            } catch (e: Exception) {
                message.value = BackupMessage.Error(R.string.import_failed, e.message ?: "")
            }
        }
    }

    fun exportCsv(uri: Uri, resolver: android.content.ContentResolver) {
        viewModelScope.launch {
            try {
                val csv = CsvExporter.buildCsv(app.db)
                withContext(Dispatchers.IO) {
                    resolver.openOutputStream(uri, "wt")?.use { it.write(csv.toByteArray()) }
                }
                message.value = BackupMessage.Info(R.string.export_done)
            } catch (e: Exception) {
                message.value = BackupMessage.Error(R.string.export_failed, e.message ?: "")
            }
        }
    }

    fun loadJefitCsv(uri: Uri, resolver: android.content.ContentResolver) {
        viewModelScope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    resolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                } ?: throw IllegalArgumentException("Could not read file")
                pendingCsv = text
                val parsed = JefitCsvParser.parse(text)
                val hasData = parsed.sessions.isNotEmpty() || parsed.bodyMetrics.isNotEmpty()
                if (parsed.detectedUnit == null && hasData) {
                    needsUnitChoice.value = true
                } else {
                    preview(parsed)
                }
            } catch (e: Exception) {
                message.value = BackupMessage.Error(R.string.import_failed, e.message ?: "")
            }
        }
    }

    fun chooseUnit(unit: WeightUnit) {
        needsUnitChoice.value = false
        val csv = pendingCsv ?: return
        viewModelScope.launch {
            preview(JefitCsvParser.parse(csv, unitOverride = unit))
        }
    }

    private suspend fun preview(parsed: JefitCsvParser.Result) {
        if (parsed.sessions.isEmpty() && parsed.bodyMetrics.isEmpty()) {
            message.value = BackupMessage.Error(
                R.string.jefit_nothing_found,
                parsed.skipped.take(3).joinToString("; "),
            )
            return
        }
        jefitPreview.value = parsed to app.jefitImporter.preview(parsed)
    }

    fun commitJefit() {
        val (parsed, _) = jefitPreview.value ?: return
        jefitPreview.value = null
        viewModelScope.launch {
            try {
                val summary = app.jefitImporter.commit(parsed)
                message.value = BackupMessage.JefitDone(summary)
            } catch (e: Exception) {
                message.value = BackupMessage.Error(R.string.import_failed, e.message ?: "")
            }
        }
    }
}

@Composable
fun BackupScreen(container: AppContainer) {
    val vm = containerViewModel(container) { c, _ -> BackupViewModel(c) }
    val message by vm.message.collectAsState()
    val jefitPreview by vm.jefitPreview.collectAsState()
    val needsUnit by vm.needsUnitChoice.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var confirmImport by remember { mutableStateOf<Uri?>(null) }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { vm.exportJson(it, context.contentResolver) } }

    val importJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { confirmImport = it } }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let { vm.exportCsv(it, context.contentResolver) } }

    val jefitLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { vm.loadJefitCsv(it, context.contentResolver) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(R.string.backup_and_import),
            style = MaterialTheme.typography.headlineMedium,
        )

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.backup), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.backup_hint), style = MaterialTheme.typography.bodySmall)
                OutlinedButton(
                    onClick = {
                        exportJsonLauncher.launch("skrot-backup-${LocalDate.now()}.json")
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.export_json)) }
                OutlinedButton(
                    onClick = { importJsonLauncher.launch(arrayOf("application/json", "*/*")) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.import_json)) }
                OutlinedButton(
                    onClick = { exportCsvLauncher.launch("skrot-log-${LocalDate.now()}.csv") },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.export_csv)) }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.jefit_import), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.jefit_import_hint), style = MaterialTheme.typography.bodySmall)
                OutlinedButton(
                    onClick = { jefitLauncher.launch(arrayOf("text/*", "*/*")) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.pick_jefit_csv)) }
            }
        }

        message?.let { msg ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    when (msg) {
                        is BackupMessage.Info -> Text(stringResource(msg.textRes))
                        is BackupMessage.Error -> Text(
                            stringResource(msg.textRes) +
                                if (msg.detail.isNotBlank()) "\n${msg.detail}" else "",
                            color = MaterialTheme.colorScheme.error,
                        )

                        is BackupMessage.JefitDone -> {
                            Text(
                                stringResource(
                                    R.string.jefit_summary,
                                    msg.summary.sessionsCreated,
                                    msg.summary.setsCreated,
                                    msg.summary.exercisesCreated,
                                )
                            )
                            if (msg.summary.bodyMetricsCreated > 0) {
                                Text(
                                    stringResource(
                                        R.string.jefit_summary_body_metrics,
                                        msg.summary.bodyMetricsCreated,
                                    )
                                )
                            }
                            if (msg.summary.skipped.isNotEmpty()) {
                                Text(
                                    stringResource(R.string.jefit_skipped, msg.summary.skipped.size) +
                                        "\n" + msg.summary.skipped.take(10).joinToString("\n"),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // JSON import replaces everything — warn clearly.
    confirmImport?.let { uri ->
        AlertDialog(
            onDismissRequest = { confirmImport = null },
            title = { Text(stringResource(R.string.import_json)) },
            text = { Text(stringResource(R.string.import_replace_warning)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.importJson(uri, context.contentResolver)
                    confirmImport = null
                }) { Text(stringResource(R.string.replace_data)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmImport = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (needsUnit) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(stringResource(R.string.jefit_unit_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.jefit_unit_question))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        FilterChip(
                            selected = false,
                            onClick = { vm.chooseUnit(WeightUnit.KG) },
                            label = { Text("kg") },
                        )
                        FilterChip(
                            selected = false,
                            onClick = { vm.chooseUnit(WeightUnit.LBS) },
                            label = { Text("lbs") },
                        )
                    }
                }
            },
            confirmButton = {},
        )
    }

    jefitPreview?.let { (parsed, preview) ->
        AlertDialog(
            onDismissRequest = { vm.jefitPreview.value = null },
            title = { Text(stringResource(R.string.jefit_preview_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(
                            R.string.jefit_preview_body,
                            preview.sessionCount,
                            preview.setCount,
                            preview.newExercises.size,
                        )
                    )
                    if (preview.bodyMetricCount > 0) {
                        Text(stringResource(R.string.jefit_preview_body_metrics, preview.bodyMetricCount))
                    }
                    if (preview.newExercises.isNotEmpty()) {
                        Text(
                            stringResource(R.string.jefit_new_exercises) + " " +
                                preview.newExercises.take(8).joinToString(", ") +
                                if (preview.newExercises.size > 8) "…" else "",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (parsed.skipped.isNotEmpty()) {
                        Text(
                            stringResource(R.string.jefit_skipped, parsed.skipped.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.commitJefit() }) {
                    Text(stringResource(R.string.import_label))
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.jefitPreview.value = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
