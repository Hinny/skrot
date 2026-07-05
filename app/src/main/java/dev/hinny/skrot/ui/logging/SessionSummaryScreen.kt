package dev.hinny.skrot.ui.logging

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import dev.hinny.skrot.AppContainer
import dev.hinny.skrot.R
import dev.hinny.skrot.data.model.MeasurementType
import dev.hinny.skrot.data.model.WeightUnit
import dev.hinny.skrot.data.prefs.Settings
import dev.hinny.skrot.domain.PrDetector
import dev.hinny.skrot.domain.PrType
import dev.hinny.skrot.domain.SetRecord
import dev.hinny.skrot.domain.Units
import dev.hinny.skrot.domain.VolumeCalculator
import dev.hinny.skrot.ui.Routes
import dev.hinny.skrot.ui.containerViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SummaryUiState(
    val durationMs: Long = 0,
    val completedSets: Int = 0,
    val totalVolumeKg: Double = 0.0,
    val exerciseCount: Int = 0,
    val prs: List<Pair<String, PrType>> = emptyList(),
    val loaded: Boolean = false,
)

class SummaryViewModel(container: AppContainer, private val sessionId: Long) : ViewModel() {
    private val db = container.db
    val state = MutableStateFlow(SummaryUiState())

    init {
        viewModelScope.launch {
            val content = db.sessionDao().sessionWithContent(sessionId) ?: return@launch
            val session = content.session
            val settings = container.settings.settings.first()
            val bodyweight = db.bodyMetricDao().latestWeightAtOrBefore(session.startedAt)
                ?.weightKg ?: settings.bodyweightFallbackKg

            var volume = 0.0
            var setCount = 0
            val prs = mutableListOf<Pair<String, PrType>>()

            for (se in content.exercises) {
                val completed = se.sortedSets.filter { it.completed }
                setCount += completed.size
                for (set in completed) {
                    VolumeCalculator.setVolumeKg(
                        se.exercise.measurementType,
                        set.load,
                        set.reps,
                        bodyweight,
                        se.exercise.bodyweightFactor,
                    )?.let { volume += it }
                }

                // Recompute PRs: each completed set against everything logged before it.
                val allHistory = db.sessionDao().setsForExercise(se.exercise.id)
                    .filter { it.sessionId != sessionId }
                    .map { SetRecord(it.set.load, it.set.reps, it.set.setType, it.sessionGymId) }
                    .toMutableList()
                for (set in completed.sortedBy { it.completedAt ?: 0 }) {
                    val record = SetRecord(set.load, set.reps, set.setType, session.gymId)
                    val found = PrDetector.detect(
                        se.exercise.measurementType, record, allHistory, session.gymId,
                    )
                    prs.addAll(found.map { se.exercise.nameEn to it })
                    allHistory.add(record)
                }
            }

            state.value = SummaryUiState(
                durationMs = (session.endedAt ?: session.startedAt) - session.startedAt,
                completedSets = setCount,
                totalVolumeKg = volume,
                exerciseCount = content.exercises.size,
                prs = prs,
                loaded = true,
            )
        }
    }
}

@Composable
fun prTypeLabel(type: PrType): String = stringResource(
    when (type) {
        PrType.HEAVIEST_WEIGHT -> R.string.pr_heaviest_weight
        PrType.BEST_E1RM -> R.string.pr_best_e1rm
        PrType.REP_PR_AT_WEIGHT -> R.string.pr_reps_at_weight
        PrType.HIGHEST_LEVEL -> R.string.pr_highest_level
        PrType.REP_PR_AT_LEVEL -> R.string.pr_reps_at_level
        PrType.MOST_REPS -> R.string.pr_most_reps
        PrType.MOST_ADDED_WEIGHT -> R.string.pr_most_added_weight
    }
)

@Composable
fun SessionSummaryScreen(
    container: AppContainer,
    settings: Settings,
    nav: NavHostController,
    sessionId: Long,
) {
    val vm = containerViewModel(container, key = "summary_$sessionId") { c, _ ->
        SummaryViewModel(c, sessionId)
    }
    val state by vm.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.session_summary), style = MaterialTheme.typography.headlineSmall)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SummaryRow(
                    stringResource(R.string.duration),
                    formatDuration(state.durationMs),
                )
                SummaryRow(stringResource(R.string.exercises), state.exerciseCount.toString())
                SummaryRow(stringResource(R.string.sets), state.completedSets.toString())
                val volume =
                    if (settings.unit == WeightUnit.KG) state.totalVolumeKg
                    else Units.kgToLbs(state.totalVolumeKg)
                SummaryRow(
                    stringResource(R.string.total_volume),
                    "${Units.formatValue(volume)} ${if (settings.unit == WeightUnit.KG) "kg" else "lbs"}",
                )
            }
        }

        if (state.prs.isNotEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.new_prs),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    state.prs.forEach { (name, type) ->
                        Text("$name — ${prTypeLabel(type)}")
                    }
                }
            }
        }

        Button(
            onClick = { nav.popBackStack(Routes.HOME, inclusive = false) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.done)) }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

private fun formatDuration(ms: Long): String {
    val totalMin = ms / 60_000
    val h = totalMin / 60
    val m = totalMin % 60
    return if (h > 0) "${h}h ${m}min" else "${m}min"
}
