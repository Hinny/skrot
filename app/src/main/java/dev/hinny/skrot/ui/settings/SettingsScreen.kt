package dev.hinny.skrot.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.navigation.NavHostController
import dev.hinny.skrot.AppContainer
import dev.hinny.skrot.R
import dev.hinny.skrot.data.model.AppLanguage
import dev.hinny.skrot.data.model.CoachFrequency
import dev.hinny.skrot.data.model.CoachPersonality
import dev.hinny.skrot.data.model.SwapBehavior
import dev.hinny.skrot.data.model.ThemeMode
import dev.hinny.skrot.data.model.WeightUnit
import dev.hinny.skrot.data.prefs.Settings
import dev.hinny.skrot.ui.Routes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(container: AppContainer, settings: Settings, nav: NavHostController) {
    val scope = remember { CoroutineScope(Dispatchers.Main) }
    val repo = container.settings

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineMedium)

        // Language
        SettingSection(stringResource(R.string.language)) {
            ChipRow(
                options = listOf(
                    AppLanguage.SYSTEM to stringResource(R.string.follow_system),
                    AppLanguage.ENGLISH to "English",
                    AppLanguage.SWEDISH to "Svenska",
                ),
                selected = settings.language,
            ) { language ->
                scope.launch {
                    repo.setLanguage(language)
                    AppCompatDelegate.setApplicationLocales(
                        when (language) {
                            AppLanguage.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
                            AppLanguage.ENGLISH -> LocaleListCompat.forLanguageTags("en")
                            AppLanguage.SWEDISH -> LocaleListCompat.forLanguageTags("sv")
                        }
                    )
                }
            }
        }

        // Display unit
        SettingSection(stringResource(R.string.display_unit)) {
            ChipRow(
                options = listOf(WeightUnit.KG to "kg", WeightUnit.LBS to "lbs"),
                selected = settings.unit,
            ) { scope.launch { repo.setUnit(it) } }
        }

        // Theme
        SettingSection(stringResource(R.string.theme)) {
            ChipRow(
                options = listOf(
                    ThemeMode.DARK to stringResource(R.string.theme_dark),
                    ThemeMode.LIGHT to stringResource(R.string.theme_light),
                    ThemeMode.SYSTEM to stringResource(R.string.follow_system),
                ),
                selected = settings.theme,
            ) { scope.launch { repo.setTheme(it) } }
        }

        HorizontalDivider()

        // Rest timer
        SettingSection(stringResource(R.string.rest_timer)) {
            NumberSetting(
                label = stringResource(R.string.default_rest_for_new_sets),
                value = settings.defaultRestSec,
            ) { scope.launch { repo.setDefaultRestSec(it) } }
            NumberSetting(
                label = stringResource(R.string.timer_adjust_step),
                value = settings.timerAdjustStepSec,
            ) { scope.launch { repo.setTimerAdjustStepSec(it) } }
            ToggleSetting(stringResource(R.string.timer_sound), settings.timerSound) {
                scope.launch { repo.setTimerSound(it) }
            }
            ToggleSetting(stringResource(R.string.timer_vibration), settings.timerVibrate) {
                scope.launch { repo.setTimerVibrate(it) }
            }
        }

        HorizontalDivider()

        // Sessions
        SettingSection(stringResource(R.string.sessions_section)) {
            NumberSetting(
                label = stringResource(R.string.auto_finish_threshold),
                value = settings.autoFinishMinutes,
            ) { scope.launch { repo.setAutoFinishMinutes(it) } }
            Text(
                stringResource(R.string.swap_sequence_behavior),
                style = MaterialTheme.typography.bodyMedium,
            )
            ChipRow(
                options = listOf(
                    SwapBehavior.SKIPPED_STAYS_NEXT to stringResource(R.string.swap_skipped_stays),
                    SwapBehavior.ADVANCE to stringResource(R.string.swap_advance),
                ),
                selected = settings.swapBehavior,
            ) { scope.launch { repo.setSwapBehavior(it) } }
            NumberSetting(
                label = stringResource(R.string.comeback_threshold),
                value = settings.comebackDays,
            ) { scope.launch { repo.setComebackDays(it) } }
            ToggleSetting(stringResource(R.string.keep_screen_on), settings.keepScreenOn) {
                scope.launch { repo.setKeepScreenOn(it) }
            }
        }

        HorizontalDivider()

        // Progression
        SettingSection(stringResource(R.string.progression)) {
            DecimalSetting(
                label = stringResource(R.string.increment_kg),
                value = settings.progressionIncrementKg,
            ) { scope.launch { repo.setProgressionIncrementKg(it) } }
            DecimalSetting(
                label = stringResource(R.string.increment_level),
                value = settings.progressionIncrementLevel,
            ) { scope.launch { repo.setProgressionIncrementLevel(it) } }
            DecimalSetting(
                label = stringResource(R.string.bodyweight_fallback),
                value = settings.bodyweightFallbackKg,
            ) { scope.launch { repo.setBodyweightFallbackKg(it) } }
        }

        HorizontalDivider()

        // Coach
        SettingSection(stringResource(R.string.coach_comments)) {
            ToggleSetting(stringResource(R.string.coach_enabled), settings.coachEnabled) {
                scope.launch { repo.setCoachEnabled(it) }
            }
            if (settings.coachEnabled) {
                Text(stringResource(R.string.coach_personality), style = MaterialTheme.typography.bodyMedium)
                ChipRow(
                    options = listOf(
                        CoachPersonality.CHEERLEADER to stringResource(R.string.personality_cheerleader),
                        CoachPersonality.BRO to stringResource(R.string.personality_bro),
                        CoachPersonality.PT to stringResource(R.string.personality_pt),
                        CoachPersonality.MINIMAL to stringResource(R.string.personality_minimal),
                    ),
                    selected = settings.coachPersonality,
                ) { scope.launch { repo.setCoachPersonality(it) } }
                Text(stringResource(R.string.coach_frequency), style = MaterialTheme.typography.bodyMedium)
                ChipRow(
                    options = listOf(
                        CoachFrequency.LOW to stringResource(R.string.frequency_low),
                        CoachFrequency.MEDIUM to stringResource(R.string.frequency_medium),
                        CoachFrequency.HIGH to stringResource(R.string.frequency_high),
                    ),
                    selected = settings.coachFrequency,
                ) { scope.launch { repo.setCoachFrequency(it) } }
            }
        }

        HorizontalDivider()

        OutlinedButton(onClick = { nav.navigate(Routes.GYMS) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.default_gym_setting))
        }
        OutlinedButton(onClick = { nav.navigate(Routes.BACKUP) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.backup_and_import))
        }
        OutlinedButton(onClick = { nav.navigate(Routes.ABOUT) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.about))
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun SettingSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Composable
private fun <T> ChipRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState()),
    ) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelect(value) },
                label = { Text(label) },
            )
        }
    }
}

@Composable
private fun ToggleSetting(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun NumberSetting(label: String, value: Int, onChange: (Int) -> Unit) {
    var text by remember(label) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it.filter(Char::isDigit)
            text.toIntOrNull()?.let(onChange)
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun DecimalSetting(label: String, value: Double, onChange: (Double) -> Unit) {
    var text by remember(label) {
        mutableStateOf(dev.hinny.skrot.domain.Units.formatValue(value))
    }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it.filter { c -> c.isDigit() || c == '.' || c == ',' }
            text.replace(',', '.').toDoubleOrNull()?.let(onChange)
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}
