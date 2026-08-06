package dev.hinny.skrot

import android.app.Application
import dev.hinny.skrot.data.backup.BackupManager
import dev.hinny.skrot.data.backup.JefitImporter
import dev.hinny.skrot.data.db.SeedData
import dev.hinny.skrot.data.db.SkrotDatabase
import dev.hinny.skrot.data.model.Exercise
import dev.hinny.skrot.data.model.ExerciseSort
import dev.hinny.skrot.data.prefs.Settings
import dev.hinny.skrot.data.prefs.SettingsRepository
import dev.hinny.skrot.timer.RestTimerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Manual dependency container — deliberately no DI framework for an app of this
 * scope (see README).
 */
class AppContainer(app: Application) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val db: SkrotDatabase = SkrotDatabase.build(app)
    val settings = SettingsRepository(app)
    val restTimer = RestTimerController(app, scope, settings)
    val backupManager = BackupManager(db, BuildConfig.VERSION_NAME)
    val jefitImporter = JefitImporter(db)

    /**
     * The settings kept warm for the life of the process, so the many places
     * that need one value mid-action read it instead of collecting the
     * DataStore flow again. Null until the first read lands — a window of
     * milliseconds at startup that [settingsNow] closes by waiting.
     */
    private val settingsCache: StateFlow<Settings?> =
        settings.settings.stateIn(scope, SharingStarted.Eagerly, null)

    /**
     * The current settings. Cheap once the process is warm; falls back to a
     * real read for the brief window before [settingsCache] has a value, so
     * callers never see the defaults standing in for a stored preference.
     */
    suspend fun settingsNow(): Settings = settingsCache.value ?: settings.settings.first()

    /**
     * The exercise library in the order the user asked for — alphabetical, or
     * what they actually train most. Every list and picker in the app reads it
     * from here so the choice applies everywhere.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeExercises(): Flow<List<Exercise>> =
        settings.settings
            .map { it.exerciseSort }
            .distinctUntilChanged()
            .flatMapLatest { sort ->
                when (sort) {
                    ExerciseSort.MOST_USED -> db.exerciseDao().observeAllByUsage()
                    ExerciseSort.NAME -> db.exerciseDao().observeAll()
                }
            }

    /**
     * One-shot counterpart of [observeExercises], for the lists built once when
     * a dialog opens rather than observed. Same setting, same order — a picker
     * that ignored it would be the odd one out.
     */
    suspend fun exercisesNow(): List<Exercise> =
        when (settingsNow().exerciseSort) {
            ExerciseSort.MOST_USED -> db.exerciseDao().getAllByUsage()
            ExerciseSort.NAME -> db.exerciseDao().getAll()
        }

    /**
     * Auto-finish: an in-progress session with no activity for the configured
     * threshold is marked finished with its end time set to the last activity.
     */
    suspend fun autoFinishStaleSessions() {
        val thresholdMs = settingsNow().autoFinishMinutes * 60_000L
        val now = System.currentTimeMillis()
        for (session in db.sessionDao().openSessions()) {
            if (now - session.lastActivityAt >= thresholdMs) {
                db.sessionDao().finish(session.id, session.lastActivityAt)
            }
        }
    }
}

class SkrotApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.scope.launch {
            SeedData.seedIfEmpty(container.db)
            container.autoFinishStaleSessions()
        }
    }
}
