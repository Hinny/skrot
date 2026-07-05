package dev.hinny.skrot

import android.app.Application
import dev.hinny.skrot.data.backup.BackupManager
import dev.hinny.skrot.data.backup.JefitImporter
import dev.hinny.skrot.data.db.SeedData
import dev.hinny.skrot.data.db.SkrotDatabase
import dev.hinny.skrot.data.prefs.SettingsRepository
import dev.hinny.skrot.timer.RestTimerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
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
     * Auto-finish: an in-progress session with no activity for the configured
     * threshold is marked finished with its end time set to the last activity.
     */
    suspend fun autoFinishStaleSessions() {
        val thresholdMs = settings.settings.first().autoFinishMinutes * 60_000L
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
