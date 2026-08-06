package dev.hinny.skrot.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import dev.hinny.skrot.AppContainer
import dev.hinny.skrot.R
import dev.hinny.skrot.data.model.Exercise
import dev.hinny.skrot.data.model.HomeSection
import dev.hinny.skrot.data.model.OneRepMaxRange
import dev.hinny.skrot.data.prefs.Settings
import dev.hinny.skrot.ui.common.ExercisePickerDialog
import dev.hinny.skrot.ui.common.StepperNumberField
import dev.hinny.skrot.ui.common.displayName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.hinny.skrot.ui.containerViewModel

class HomeSettingsViewModel(container: AppContainer) : ViewModel() {
    val exercises = MutableStateFlow<List<Exercise>>(emptyList())

    init {
        viewModelScope.launch {
            container.observeExercises().collect { exercises.value = it }
        }
    }
}

/** Label for a home card, used by both this screen and nothing else. */
@Composable
private fun sectionLabel(section: HomeSection): String = stringResource(
    when (section) {
        HomeSection.COACH -> R.string.home_section_coach
        HomeSection.NEXT_WORKOUT -> R.string.home_section_next_workout
        HomeSection.RECOVERY -> R.string.home_section_recovery
        HomeSection.BACKUP_REMINDER -> R.string.home_section_backup
        HomeSection.LAST_SESSION -> R.string.home_section_last_session
        HomeSection.BODY_METRIC -> R.string.home_section_body
        HomeSection.DAYS_SINCE_LAST -> R.string.home_section_days_since
        HomeSection.WEEK_STREAK -> R.string.home_section_streak
        HomeSection.ONE_REP_MAX -> R.string.home_section_one_rep_max
    }
)

@Composable
fun rangeLabel(range: OneRepMaxRange): String = stringResource(
    when (range) {
        OneRepMaxRange.CURRENT -> R.string.range_current
        OneRepMaxRange.PAST_YEAR -> R.string.range_past_year
        OneRepMaxRange.PAST_3_YEARS -> R.string.range_past_3_years
        OneRepMaxRange.ALL_TIME -> R.string.range_all_time
    }
)

/** Picks which cards the home screen shows, and configures the ones with options. */
@Composable
fun HomeSettingsScreen(
    container: AppContainer,
    settings: Settings,
    onOpenSettings: () -> Unit,
) {
    val vm = containerViewModel(container) { c, _ -> HomeSettingsViewModel(c) }
    val allExercises by vm.exercises.collectAsState()
    val scope = rememberCoroutineScope()
    val repo = container.settings
    var addingExercise by remember { mutableStateOf(false) }

    fun toggle(section: HomeSection, on: Boolean) {
        val next =
            if (on) settings.homeSections + section else settings.homeSections - section
        scope.launch { repo.setHomeSections(next) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            stringResource(R.string.home_settings),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            stringResource(R.string.home_settings_hint),
            style = MaterialTheme.typography.bodySmall,
        )

        HomeSection.entries.forEach { section ->
            val enabled = section in settings.homeSections
            // The coach card can't show anything while coach comments are off
            // altogether; saying so beats a switch that silently does nothing.
            val blocked = section == HomeSection.COACH && !settings.coachEnabled
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Switch(
                    checked = enabled && !blocked,
                    enabled = !blocked,
                    onCheckedChange = { toggle(section, it) },
                )
                Spacer(Modifier.width(12.dp))
                Text(sectionLabel(section), modifier = Modifier.weight(1f))
            }
            if (blocked) {
                Text(
                    stringResource(R.string.coach_disabled_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                OutlinedButton(onClick = onOpenSettings) {
                    Text(stringResource(R.string.open_settings))
                }
            }

            // Cards with knobs of their own reveal them once switched on.
            if (enabled && section == HomeSection.WEEK_STREAK) {
                Text(
                    stringResource(R.string.streak_min_per_week),
                    style = MaterialTheme.typography.bodySmall,
                )
                StepperNumberField(
                    value = settings.streakMinPerWeek.toString(),
                    onValueChange = { text ->
                        text.toIntOrNull()?.let { scope.launch { repo.setStreakMinPerWeek(it) } }
                    },
                    step = 1.0,
                    label = stringResource(R.string.workouts_per_week),
                    integerOnly = true,
                )
            }
            if (enabled && section == HomeSection.ONE_REP_MAX) {
                OneRepMaxSettings(
                    settings = settings,
                    allExercises = allExercises,
                    onAdd = { addingExercise = true },
                    onRemove = { id ->
                        scope.launch {
                            repo.setOneRepMaxExerciseIds(settings.oneRepMaxExerciseIds - id)
                        }
                    },
                    onRange = { scope.launch { repo.setOneRepMaxRange(it) } },
                )
            }
            HorizontalDivider()
        }
        Spacer(Modifier.height(40.dp))
    }

    if (addingExercise) {
        ExercisePickerDialog(
            exercises = allExercises,
            onPick = { picked ->
                addingExercise = false
                if (picked.id !in settings.oneRepMaxExerciseIds) {
                    scope.launch {
                        repo.setOneRepMaxExerciseIds(settings.oneRepMaxExerciseIds + picked.id)
                    }
                }
            },
            onDismiss = { addingExercise = false },
        )
    }
}

@Composable
private fun OneRepMaxSettings(
    settings: Settings,
    allExercises: List<Exercise>,
    onAdd: () -> Unit,
    onRemove: (Long) -> Unit,
    onRange: (OneRepMaxRange) -> Unit,
) {
    val tracked = resolveOneRepMaxExercises(settings.oneRepMaxExerciseIds, allExercises)
    Column(Modifier.padding(start = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            stringResource(R.string.one_rep_max_exercises),
            style = MaterialTheme.typography.bodySmall,
        )
        tracked.forEach { exercise ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(exercise.displayName(), modifier = Modifier.weight(1f))
                IconButton(onClick = { onRemove(exercise.id) }) {
                    Icon(Icons.Filled.Close, stringResource(R.string.delete))
                }
            }
        }
        OutlinedButton(onClick = onAdd) { Text(stringResource(R.string.add_exercise)) }

        Text(stringResource(R.string.one_rep_max_range), style = MaterialTheme.typography.bodySmall)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            OneRepMaxRange.entries.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { range ->
                        FilterChip(
                            selected = settings.oneRepMaxRange == range,
                            onClick = { onRange(range) },
                            label = { Text(rangeLabel(range)) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * The exercises the 1RM card tracks. An empty selection falls back to the big
 * three by name, so the card shows something sensible the first time it is
 * switched on; editing it stores explicit ids from then on.
 */
fun resolveOneRepMaxExercises(ids: List<Long>, all: List<Exercise>): List<Exercise> {
    if (ids.isNotEmpty()) return ids.mapNotNull { id -> all.find { it.id == id } }
    return DEFAULT_ONE_REP_MAX_NAMES.mapNotNull { name ->
        all.find { it.nameEn.equals(name, ignoreCase = true) }
    }
}

private val DEFAULT_ONE_REP_MAX_NAMES = listOf("Squat", "Bench Press", "Deadlift")
