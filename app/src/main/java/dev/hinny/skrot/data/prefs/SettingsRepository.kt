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
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.hinny.skrot.data.model.AppLanguage
import dev.hinny.skrot.data.model.CoachFrequency
import dev.hinny.skrot.data.model.CoachPersonality
import dev.hinny.skrot.data.model.ExerciseSort
import dev.hinny.skrot.data.model.HomeSection
import dev.hinny.skrot.data.model.MetaDisplay
import dev.hinny.skrot.data.model.OneRepMaxRange
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
    /**
     * Sound played when the rest timer runs out, as a content URI string.
     * Empty means the system's default notification sound — picking something
     * else is how you tell a finished set apart from an incoming email.
     */
    val timerSoundUri: String = "",
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
    /** Seconds a coach comment stays on screen during a workout; 0 = until dismissed. */
    val coachMessageSeconds: Int = 5,
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
    /**
     * Whether starting a routine workout always runs through the exercise plan
     * first — the same screen that otherwise only appears when the gym forces a
     * choice, but listing every exercise and its availability.
     */
    val planExercisesBeforeStart: Boolean = false,
    /** Whether finishing a workout shows what you got done before closing it. */
    val celebrateWorkoutFinish: Boolean = true,
    /** Whether the Session tab offers a recovery workout regardless of time away. */
    val alwaysOfferRecovery: Boolean = true,
    /** Which cards the home screen shows. */
    val homeSections: Set<HomeSection> = HomeSection.DEFAULTS,
    /** Workouts a week must contain to keep the streak alive. */
    val streakMinPerWeek: Int = 1,
    /** Exercises tracked by the home screen's 1RM card, in display order. */
    val oneRepMaxExerciseIds: List<Long> = emptyList(),
    val oneRepMaxRange: OneRepMaxRange = OneRepMaxRange.CURRENT,
    /** Which language exercise names are shown in, independent of the app's UI language. */
    val exerciseNameLanguage: AppLanguage = AppLanguage.SYSTEM,
    /** Order exercise lists and pickers are shown in, everywhere in the app. */
    val exerciseSort: ExerciseSort = ExerciseSort.NAME,
    /** How an exercise's muscle groups are rendered in lists and detail views. */
    val muscleDisplay: MetaDisplay = MetaDisplay.TEXT,
    /** How an exercise's equipment is rendered in lists and detail views. */
    val equipmentDisplay: MetaDisplay = MetaDisplay.TEXT,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val language = stringPreferencesKey("language")
        val unit = stringPreferencesKey("unit")
        val theme = stringPreferencesKey("theme")
        val defaultRestSec = intPreferencesKey("default_rest_sec")
        val timerSound = booleanPreferencesKey("timer_sound")
        val timerSoundUri = stringPreferencesKey("timer_sound_uri")
        val timerVibrate = booleanPreferencesKey("timer_vibrate")
        val timerAdjustStepSec = intPreferencesKey("timer_adjust_step_sec")
        val autoFinishMinutes = intPreferencesKey("auto_finish_minutes")
        val swapBehavior = stringPreferencesKey("swap_behavior")
        val comebackDays = intPreferencesKey("comeback_days")
        val coachEnabled = booleanPreferencesKey("coach_enabled")
        val coachPersonality = stringPreferencesKey("coach_personality")
        val coachFrequency = stringPreferencesKey("coach_frequency")
        val coachMessageSeconds = intPreferencesKey("coach_message_seconds")
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
        val planExercisesBeforeStart = booleanPreferencesKey("plan_exercises_before_start")
        val celebrateWorkoutFinish = booleanPreferencesKey("celebrate_workout_finish")
        val alwaysOfferRecovery = booleanPreferencesKey("always_offer_recovery")
        val homeSections = stringSetPreferencesKey("home_sections")
        val streakMinPerWeek = intPreferencesKey("streak_min_per_week")
        val oneRepMaxExerciseIds = stringPreferencesKey("one_rep_max_exercise_ids")
        val oneRepMaxRange = stringPreferencesKey("one_rep_max_range")
        val exerciseNameLanguage = stringPreferencesKey("exercise_name_language")
        val exerciseSort = stringPreferencesKey("exercise_sort")
        val muscleDisplay = stringPreferencesKey("muscle_display")
        val equipmentDisplay = stringPreferencesKey("equipment_display")
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
            timerSoundUri = p[Keys.timerSoundUri] ?: defaults.timerSoundUri,
            timerVibrate = p[Keys.timerVibrate] ?: defaults.timerVibrate,
            timerAdjustStepSec = p[Keys.timerAdjustStepSec] ?: defaults.timerAdjustStepSec,
            autoFinishMinutes = p[Keys.autoFinishMinutes] ?: defaults.autoFinishMinutes,
            swapBehavior = p[Keys.swapBehavior].toEnum(defaults.swapBehavior),
            comebackDays = p[Keys.comebackDays] ?: defaults.comebackDays,
            coachEnabled = p[Keys.coachEnabled] ?: defaults.coachEnabled,
            coachPersonality = p[Keys.coachPersonality].toEnum(defaults.coachPersonality),
            coachFrequency = p[Keys.coachFrequency].toEnum(defaults.coachFrequency),
            coachMessageSeconds = p[Keys.coachMessageSeconds] ?: defaults.coachMessageSeconds,
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
            planExercisesBeforeStart = p[Keys.planExercisesBeforeStart] ?: defaults.planExercisesBeforeStart,
            celebrateWorkoutFinish = p[Keys.celebrateWorkoutFinish] ?: defaults.celebrateWorkoutFinish,
            alwaysOfferRecovery = p[Keys.alwaysOfferRecovery] ?: defaults.alwaysOfferRecovery,
            homeSections = p[Keys.homeSections]
                ?.mapNotNullTo(mutableSetOf()) { name ->
                    HomeSection.entries.find { it.name == name }
                }
                ?: defaults.homeSections,
            streakMinPerWeek = p[Keys.streakMinPerWeek] ?: defaults.streakMinPerWeek,
            oneRepMaxExerciseIds = p[Keys.oneRepMaxExerciseIds]
                ?.split(',')
                ?.mapNotNull { it.toLongOrNull() }
                ?: defaults.oneRepMaxExerciseIds,
            oneRepMaxRange = p[Keys.oneRepMaxRange].toEnum(defaults.oneRepMaxRange),
            exerciseNameLanguage = p[Keys.exerciseNameLanguage].toEnum(defaults.exerciseNameLanguage),
            exerciseSort = p[Keys.exerciseSort].toEnum(defaults.exerciseSort),
            muscleDisplay = p[Keys.muscleDisplay].toEnum(defaults.muscleDisplay),
            equipmentDisplay = p[Keys.equipmentDisplay].toEnum(defaults.equipmentDisplay),
        )
    }

    suspend fun setLanguage(v: AppLanguage) = context.dataStore.edit { it[Keys.language] = v.name }
    suspend fun setUnit(v: WeightUnit) = context.dataStore.edit { it[Keys.unit] = v.name }
    suspend fun setTheme(v: ThemeMode) = context.dataStore.edit { it[Keys.theme] = v.name }
    suspend fun setDefaultRestSec(v: Int) = context.dataStore.edit { it[Keys.defaultRestSec] = v }
    suspend fun setTimerSound(v: Boolean) = context.dataStore.edit { it[Keys.timerSound] = v }
    suspend fun setTimerSoundUri(v: String) = context.dataStore.edit { it[Keys.timerSoundUri] = v }
    suspend fun setTimerVibrate(v: Boolean) = context.dataStore.edit { it[Keys.timerVibrate] = v }
    suspend fun setTimerAdjustStepSec(v: Int) = context.dataStore.edit { it[Keys.timerAdjustStepSec] = v }
    suspend fun setAutoFinishMinutes(v: Int) = context.dataStore.edit { it[Keys.autoFinishMinutes] = v }
    suspend fun setSwapBehavior(v: SwapBehavior) = context.dataStore.edit { it[Keys.swapBehavior] = v.name }
    suspend fun setComebackDays(v: Int) = context.dataStore.edit { it[Keys.comebackDays] = v }
    suspend fun setCoachEnabled(v: Boolean) = context.dataStore.edit { it[Keys.coachEnabled] = v }
    suspend fun setCoachPersonality(v: CoachPersonality) = context.dataStore.edit { it[Keys.coachPersonality] = v.name }
    suspend fun setCoachFrequency(v: CoachFrequency) = context.dataStore.edit { it[Keys.coachFrequency] = v.name }
    suspend fun setCoachMessageSeconds(v: Int) =
        context.dataStore.edit { it[Keys.coachMessageSeconds] = v.coerceAtLeast(0) }
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
    suspend fun setPlanExercisesBeforeStart(v: Boolean) =
        context.dataStore.edit { it[Keys.planExercisesBeforeStart] = v }
    suspend fun setCelebrateWorkoutFinish(v: Boolean) =
        context.dataStore.edit { it[Keys.celebrateWorkoutFinish] = v }
    suspend fun setAlwaysOfferRecovery(v: Boolean) =
        context.dataStore.edit { it[Keys.alwaysOfferRecovery] = v }
    suspend fun setHomeSections(v: Set<HomeSection>) =
        context.dataStore.edit { p -> p[Keys.homeSections] = v.mapTo(mutableSetOf()) { it.name } }
    suspend fun setStreakMinPerWeek(v: Int) =
        context.dataStore.edit { it[Keys.streakMinPerWeek] = v.coerceAtLeast(1) }
    suspend fun setOneRepMaxExerciseIds(v: List<Long>) =
        context.dataStore.edit { p -> p[Keys.oneRepMaxExerciseIds] = v.joinToString(",") }
    suspend fun setOneRepMaxRange(v: OneRepMaxRange) =
        context.dataStore.edit { it[Keys.oneRepMaxRange] = v.name }
    suspend fun setExerciseNameLanguage(v: AppLanguage) =
        context.dataStore.edit { it[Keys.exerciseNameLanguage] = v.name }
    suspend fun setExerciseSort(v: ExerciseSort) =
        context.dataStore.edit { it[Keys.exerciseSort] = v.name }
    suspend fun setMuscleDisplay(v: MetaDisplay) =
        context.dataStore.edit { it[Keys.muscleDisplay] = v.name }
    suspend fun setEquipmentDisplay(v: MetaDisplay) =
        context.dataStore.edit { it[Keys.equipmentDisplay] = v.name }
}
