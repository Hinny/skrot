package dev.hinny.skrot.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.hinny.skrot.data.model.AppLanguage
import dev.hinny.skrot.data.model.CoachFrequency
import dev.hinny.skrot.data.model.CoachPersonality
import dev.hinny.skrot.data.model.Sex
import dev.hinny.skrot.data.model.SwapBehavior
import dev.hinny.skrot.data.model.ThemeMode
import dev.hinny.skrot.data.model.WeightUnit
import dev.hinny.skrot.domain.ProgressionEngine
import dev.hinny.skrot.domain.VolumeCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * All the app's knobs. The guiding principle is configurability: behaviors
 * described in the spec expose their thresholds and defaults here.
 */
data class Settings(
    val language: AppLanguage = AppLanguage.SYSTEM,
    val unit: WeightUnit = WeightUnit.KG,
    val theme: ThemeMode = ThemeMode.DARK,
    val defaultRestSec: Int = 90,
    val timerSound: Boolean = true,
    val timerVibrate: Boolean = true,
    val timerAdjustStepSec: Int = 15,
    /** In-progress sessions with no activity for this long auto-finish. */
    val autoFinishMinutes: Int = 120,
    val swapBehavior: SwapBehavior = SwapBehavior.SKIPPED_STAYS_NEXT,
    /** Days without a session before `rebuild`-tagged programs are suggested. */
    val comebackDays: Int = 14,
    val coachEnabled: Boolean = false,
    val coachPersonality: CoachPersonality = CoachPersonality.PT,
    val coachFrequency: CoachFrequency = CoachFrequency.MEDIUM,
    val progressionIncrementKg: Double = ProgressionEngine.DEFAULT_INCREMENT_KG,
    val progressionIncrementLevel: Double = ProgressionEngine.DEFAULT_INCREMENT_LEVEL,
    val bodyweightFallbackKg: Double = VolumeCalculator.DEFAULT_BODYWEIGHT_FALLBACK_KG,
    val keepScreenOn: Boolean = true,
    /** Days between backup reminders; 0 disables them. */
    val backupReminderDays: Int = 90,
    /** When the last JSON backup was exported; 0 = never. */
    val lastBackupAt: Long = 0,
    /** Offline profile — every field is optional and stays on the device. */
    val profileName: String = "",
    /** Birth year; 0 = unset. */
    val profileBirthYear: Int = 0,
    val profileSex: Sex = Sex.UNSPECIFIED,
    /** Whether editing a custom exercise in the library requires an explicit Apply/Cancel. */
    val confirmLibraryEdits: Boolean = true,
    /** Whether newly started sessions begin locked against structural edits. */
    val sessionsLockedByDefault: Boolean = false,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val language = stringPreferencesKey("language")
        val unit = stringPreferencesKey("unit")
        val theme = stringPreferencesKey("theme")
        val defaultRestSec = intPreferencesKey("default_rest_sec")
        val timerSound = booleanPreferencesKey("timer_sound")
        val timerVibrate = booleanPreferencesKey("timer_vibrate")
        val timerAdjustStepSec = intPreferencesKey("timer_adjust_step_sec")
        val autoFinishMinutes = intPreferencesKey("auto_finish_minutes")
        val swapBehavior = stringPreferencesKey("swap_behavior")
        val comebackDays = intPreferencesKey("comeback_days")
        val coachEnabled = booleanPreferencesKey("coach_enabled")
        val coachPersonality = stringPreferencesKey("coach_personality")
        val coachFrequency = stringPreferencesKey("coach_frequency")
        val progressionIncrementKg = doublePreferencesKey("progression_increment_kg")
        val progressionIncrementLevel = doublePreferencesKey("progression_increment_level")
        val bodyweightFallbackKg = doublePreferencesKey("bodyweight_fallback_kg")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")
        val backupReminderDays = intPreferencesKey("backup_reminder_days")
        val lastBackupAt = longPreferencesKey("last_backup_at")
        val profileName = stringPreferencesKey("profile_name")
        val profileBirthYear = intPreferencesKey("profile_birth_year")
        val profileSex = stringPreferencesKey("profile_sex")
        val confirmLibraryEdits = booleanPreferencesKey("confirm_library_edits")
        val sessionsLockedByDefault = booleanPreferencesKey("sessions_locked_by_default")
    }

    private inline fun <reified E : Enum<E>> String?.toEnum(default: E): E =
        this?.let { name -> enumValues<E>().find { it.name == name } } ?: default

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        val defaults = Settings()
        Settings(
            language = p[Keys.language].toEnum(defaults.language),
            unit = p[Keys.unit].toEnum(defaults.unit),
            theme = p[Keys.theme].toEnum(defaults.theme),
            defaultRestSec = p[Keys.defaultRestSec] ?: defaults.defaultRestSec,
            timerSound = p[Keys.timerSound] ?: defaults.timerSound,
            timerVibrate = p[Keys.timerVibrate] ?: defaults.timerVibrate,
            timerAdjustStepSec = p[Keys.timerAdjustStepSec] ?: defaults.timerAdjustStepSec,
            autoFinishMinutes = p[Keys.autoFinishMinutes] ?: defaults.autoFinishMinutes,
            swapBehavior = p[Keys.swapBehavior].toEnum(defaults.swapBehavior),
            comebackDays = p[Keys.comebackDays] ?: defaults.comebackDays,
            coachEnabled = p[Keys.coachEnabled] ?: defaults.coachEnabled,
            coachPersonality = p[Keys.coachPersonality].toEnum(defaults.coachPersonality),
            coachFrequency = p[Keys.coachFrequency].toEnum(defaults.coachFrequency),
            progressionIncrementKg = p[Keys.progressionIncrementKg] ?: defaults.progressionIncrementKg,
            progressionIncrementLevel = p[Keys.progressionIncrementLevel] ?: defaults.progressionIncrementLevel,
            bodyweightFallbackKg = p[Keys.bodyweightFallbackKg] ?: defaults.bodyweightFallbackKg,
            keepScreenOn = p[Keys.keepScreenOn] ?: defaults.keepScreenOn,
            backupReminderDays = p[Keys.backupReminderDays] ?: defaults.backupReminderDays,
            lastBackupAt = p[Keys.lastBackupAt] ?: defaults.lastBackupAt,
            profileName = p[Keys.profileName] ?: defaults.profileName,
            profileBirthYear = p[Keys.profileBirthYear] ?: defaults.profileBirthYear,
            profileSex = p[Keys.profileSex].toEnum(defaults.profileSex),
            confirmLibraryEdits = p[Keys.confirmLibraryEdits] ?: defaults.confirmLibraryEdits,
            sessionsLockedByDefault = p[Keys.sessionsLockedByDefault] ?: defaults.sessionsLockedByDefault,
        )
    }

    suspend fun setLanguage(v: AppLanguage) = context.dataStore.edit { it[Keys.language] = v.name }
    suspend fun setUnit(v: WeightUnit) = context.dataStore.edit { it[Keys.unit] = v.name }
    suspend fun setTheme(v: ThemeMode) = context.dataStore.edit { it[Keys.theme] = v.name }
    suspend fun setDefaultRestSec(v: Int) = context.dataStore.edit { it[Keys.defaultRestSec] = v }
    suspend fun setTimerSound(v: Boolean) = context.dataStore.edit { it[Keys.timerSound] = v }
    suspend fun setTimerVibrate(v: Boolean) = context.dataStore.edit { it[Keys.timerVibrate] = v }
    suspend fun setTimerAdjustStepSec(v: Int) = context.dataStore.edit { it[Keys.timerAdjustStepSec] = v }
    suspend fun setAutoFinishMinutes(v: Int) = context.dataStore.edit { it[Keys.autoFinishMinutes] = v }
    suspend fun setSwapBehavior(v: SwapBehavior) = context.dataStore.edit { it[Keys.swapBehavior] = v.name }
    suspend fun setComebackDays(v: Int) = context.dataStore.edit { it[Keys.comebackDays] = v }
    suspend fun setCoachEnabled(v: Boolean) = context.dataStore.edit { it[Keys.coachEnabled] = v }
    suspend fun setCoachPersonality(v: CoachPersonality) = context.dataStore.edit { it[Keys.coachPersonality] = v.name }
    suspend fun setCoachFrequency(v: CoachFrequency) = context.dataStore.edit { it[Keys.coachFrequency] = v.name }
    suspend fun setProgressionIncrementKg(v: Double) = context.dataStore.edit { it[Keys.progressionIncrementKg] = v }
    suspend fun setProgressionIncrementLevel(v: Double) = context.dataStore.edit { it[Keys.progressionIncrementLevel] = v }
    suspend fun setBodyweightFallbackKg(v: Double) = context.dataStore.edit { it[Keys.bodyweightFallbackKg] = v }
    suspend fun setKeepScreenOn(v: Boolean) = context.dataStore.edit { it[Keys.keepScreenOn] = v }
    suspend fun setBackupReminderDays(v: Int) = context.dataStore.edit { it[Keys.backupReminderDays] = v }
    suspend fun markBackupDone() =
        context.dataStore.edit { it[Keys.lastBackupAt] = System.currentTimeMillis() }
    suspend fun setProfileName(v: String) = context.dataStore.edit { it[Keys.profileName] = v }
    suspend fun setProfileBirthYear(v: Int) = context.dataStore.edit { it[Keys.profileBirthYear] = v }
    suspend fun setProfileSex(v: Sex) = context.dataStore.edit { it[Keys.profileSex] = v.name }
    suspend fun setConfirmLibraryEdits(v: Boolean) =
        context.dataStore.edit { it[Keys.confirmLibraryEdits] = v }
    suspend fun setSessionsLockedByDefault(v: Boolean) =
        context.dataStore.edit { it[Keys.sessionsLockedByDefault] = v }
}
