package dev.hinny.skrot.data.backup

import androidx.room.withTransaction
import dev.hinny.skrot.data.db.SkrotDatabase
import dev.hinny.skrot.data.model.BodyMetric
import dev.hinny.skrot.data.model.Exercise
import dev.hinny.skrot.data.model.ExerciseGroup
import dev.hinny.skrot.data.model.Gym
import dev.hinny.skrot.data.model.GymExercise
import dev.hinny.skrot.data.model.GymOverride
import dev.hinny.skrot.data.model.LoggedSet
import dev.hinny.skrot.data.model.PlannedExercise
import dev.hinny.skrot.data.model.PlannedSet
import dev.hinny.skrot.data.model.Routine
import dev.hinny.skrot.data.model.RoutineDay
import dev.hinny.skrot.data.model.SessionExercise
import dev.hinny.skrot.data.model.WorkoutSession
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Complete database snapshot; the schema version travels with the file. */
@Serializable
data class BackupFile(
    val schemaVersion: Int,
    val exportedAt: Long,
    val appVersion: String,
    val groups: List<ExerciseGroup> = emptyList(),
    val exercises: List<Exercise> = emptyList(),
    val routines: List<Routine> = emptyList(),
    val days: List<RoutineDay> = emptyList(),
    val plannedExercises: List<PlannedExercise> = emptyList(),
    val plannedSets: List<PlannedSet> = emptyList(),
    val gyms: List<Gym> = emptyList(),
    val gymExercises: List<GymExercise> = emptyList(),
    val gymOverrides: List<GymOverride> = emptyList(),
    val sessions: List<WorkoutSession> = emptyList(),
    val sessionExercises: List<SessionExercise> = emptyList(),
    val loggedSets: List<LoggedSet> = emptyList(),
    val bodyMetrics: List<BodyMetric> = emptyList(),
)

object BackupCodec {
    const val SCHEMA_VERSION = 1

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    fun encode(backup: BackupFile): String = json.encodeToString(BackupFile.serializer(), backup)

    /** @throws IllegalArgumentException on malformed input or a newer schema version. */
    fun decode(text: String): BackupFile {
        val backup = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (e: Exception) {
            throw IllegalArgumentException("Not a valid Skrot backup file", e)
        }
        require(backup.schemaVersion <= SCHEMA_VERSION) {
            "Backup schema version ${backup.schemaVersion} is newer than this app supports"
        }
        return backup
    }
}

class BackupManager(private val db: SkrotDatabase, private val appVersion: String) {

    suspend fun createBackup(): BackupFile {
        val dao = db.backupDao()
        return BackupFile(
            schemaVersion = BackupCodec.SCHEMA_VERSION,
            exportedAt = System.currentTimeMillis(),
            appVersion = appVersion,
            groups = dao.allGroups(),
            exercises = dao.allExercises(),
            routines = dao.allRoutines(),
            days = dao.allDays(),
            plannedExercises = dao.allPlannedExercises(),
            plannedSets = dao.allPlannedSets(),
            gyms = dao.allGyms(),
            gymExercises = dao.allGymExercises(),
            gymOverrides = dao.allGymOverrides(),
            sessions = dao.allSessions(),
            sessionExercises = dao.allSessionExercises(),
            loggedSets = dao.allLoggedSets(),
            bodyMetrics = dao.allBodyMetrics(),
        )
    }

    /** Replaces ALL current data with the backup's content. */
    suspend fun restore(backup: BackupFile) {
        val dao = db.backupDao()
        db.withTransaction {
            dao.clearAll()
            dao.insertGroups(backup.groups)
            dao.insertExercises(backup.exercises)
            dao.insertRoutines(backup.routines)
            dao.insertDays(backup.days)
            dao.insertPlannedExercises(backup.plannedExercises)
            dao.insertPlannedSets(backup.plannedSets)
            dao.insertGyms(backup.gyms)
            dao.insertGymExercises(backup.gymExercises)
            dao.insertGymOverrides(backup.gymOverrides)
            dao.insertSessions(backup.sessions)
            dao.insertSessionExercises(backup.sessionExercises)
            dao.insertLoggedSets(backup.loggedSets)
            dao.insertBodyMetrics(backup.bodyMetrics)
        }
    }
}
