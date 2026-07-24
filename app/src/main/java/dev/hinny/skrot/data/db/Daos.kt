package dev.hinny.skrot.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import dev.hinny.skrot.data.model.BodyMetric
import dev.hinny.skrot.data.model.DayWithContent
import dev.hinny.skrot.data.model.Exercise
import dev.hinny.skrot.data.model.ExerciseGroup
import dev.hinny.skrot.data.model.Gym
import dev.hinny.skrot.data.model.GymExercise
import dev.hinny.skrot.data.model.GymOverride
import dev.hinny.skrot.data.model.LoggedSet
import dev.hinny.skrot.data.model.MuscleGroup
import dev.hinny.skrot.data.model.PlannedExercise
import dev.hinny.skrot.data.model.PlannedSet
import dev.hinny.skrot.data.model.Routine
import dev.hinny.skrot.data.model.RoutineDay
import dev.hinny.skrot.data.model.RoutineWithDays
import dev.hinny.skrot.data.model.SessionExercise
import dev.hinny.skrot.data.model.SessionWithContent
import dev.hinny.skrot.data.model.SetWithContext
import dev.hinny.skrot.data.model.WorkoutSession
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY nameEn")
    fun observeAll(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises ORDER BY nameEn")
    suspend fun getAll(): List<Exercise>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun byId(id: Long): Exercise?

    @Query("SELECT * FROM exercises WHERE id = :id")
    fun observeById(id: Long): Flow<Exercise?>

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int

    @Insert
    suspend fun insert(exercise: Exercise): Long

    @Insert
    suspend fun insertAll(exercises: List<Exercise>): List<Long>

    @Update
    suspend fun update(exercise: Exercise)

    @Delete
    suspend fun delete(exercise: Exercise)

    @Query("UPDATE exercises SET nextTimeNote = :note WHERE id = :id")
    suspend fun setNextTimeNote(id: Long, note: String)

    @Query("SELECT * FROM exercise_groups ORDER BY nameEn")
    fun observeGroups(): Flow<List<ExerciseGroup>>

    @Query("SELECT * FROM exercise_groups")
    suspend fun getGroups(): List<ExerciseGroup>

    @Insert
    suspend fun insertGroup(group: ExerciseGroup): Long

    @Insert
    suspend fun insertGroups(groups: List<ExerciseGroup>): List<Long>

    @Update
    suspend fun updateGroup(group: ExerciseGroup)

    @Delete
    suspend fun deleteGroup(group: ExerciseGroup)

    @Query("SELECT * FROM exercises WHERE groupId = :groupId")
    suspend fun byGroup(groupId: Long): List<Exercise>
}

@Dao
interface RoutineDao {
    @Transaction
    @Query("SELECT * FROM routines ORDER BY position")
    fun observeAllWithDays(): Flow<List<RoutineWithDays>>

    @Transaction
    @Query("SELECT * FROM routines WHERE id = :id")
    fun observeWithDays(id: Long): Flow<RoutineWithDays?>

    @Transaction
    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun withDays(id: Long): RoutineWithDays?

    @Transaction
    @Query("SELECT * FROM routines WHERE isActive = 1 LIMIT 1")
    fun observeActiveWithDays(): Flow<RoutineWithDays?>

    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun byId(id: Long): Routine?

    @Insert
    suspend fun insert(routine: Routine): Long

    @Update
    suspend fun update(routine: Routine)

    @Delete
    suspend fun delete(routine: Routine)

    @Query("UPDATE routines SET isActive = 0")
    suspend fun clearActive()

    @Query("UPDATE routines SET isActive = 1 WHERE id = :id")
    suspend fun markActive(id: Long)

    /** Exactly zero or one program is active at a time. */
    @Transaction
    suspend fun setActive(id: Long?) {
        clearActive()
        if (id != null) markActive(id)
    }

    @Query("UPDATE routines SET nextDayIndex = :index WHERE id = :id")
    suspend fun setNextDayIndex(id: Long, index: Int)

    @Query("SELECT * FROM routine_days WHERE id = :id")
    suspend fun dayById(id: Long): RoutineDay?

    @Insert
    suspend fun insertDay(day: RoutineDay): Long

    @Update
    suspend fun updateDay(day: RoutineDay)

    @Delete
    suspend fun deleteDay(day: RoutineDay)

    @Transaction
    @Query("SELECT * FROM routine_days WHERE id = :dayId")
    fun observeDayWithContent(dayId: Long): Flow<DayWithContent?>

    @Transaction
    @Query("SELECT * FROM routine_days WHERE id = :dayId")
    suspend fun dayWithContent(dayId: Long): DayWithContent?

    @Insert
    suspend fun insertPlannedExercise(pe: PlannedExercise): Long

    @Update
    suspend fun updatePlannedExercise(pe: PlannedExercise)

    @Delete
    suspend fun deletePlannedExercise(pe: PlannedExercise)

    @Query("SELECT * FROM planned_exercises WHERE id = :id")
    suspend fun plannedExerciseById(id: Long): PlannedExercise?

    @Insert
    suspend fun insertPlannedSet(set: PlannedSet): Long

    @Update
    suspend fun updatePlannedSet(set: PlannedSet)

    @Delete
    suspend fun deletePlannedSet(set: PlannedSet)

    @Query("SELECT * FROM planned_sets WHERE plannedExerciseId = :peId ORDER BY position")
    suspend fun plannedSets(peId: Long): List<PlannedSet>

    /** Write-back from the logging screen: rest duration persists to the routine. */
    @Query("UPDATE planned_sets SET restSec = :restSec WHERE plannedExerciseId = :peId AND position = :position")
    suspend fun writeBackRest(peId: Long, position: Int, restSec: Int)

    /** Write-back from the logging screen: target reps persist to the routine. */
    @Query(
        "UPDATE planned_sets SET targetRepsMin = :repsMin, targetRepsMax = :repsMax " +
            "WHERE plannedExerciseId = :peId AND position = :position"
    )
    suspend fun writeBackTarget(peId: Long, position: Int, repsMin: Int?, repsMax: Int?)

    /** Last time any day of each routine was performed. */
    @Query(
        "SELECT routineId, MAX(startedAt) AS last FROM sessions " +
            "WHERE endedAt IS NOT NULL AND routineId IS NOT NULL GROUP BY routineId"
    )
    fun observeLastPerformedByRoutine(): Flow<List<RoutineLastPerformed>>

    @Query(
        "SELECT routineDayId AS dayId, MAX(startedAt) AS last FROM sessions " +
            "WHERE endedAt IS NOT NULL AND routineDayId IS NOT NULL GROUP BY routineDayId"
    )
    fun observeLastPerformedByDay(): Flow<List<DayLastPerformed>>
}

data class RoutineLastPerformed(val routineId: Long, val last: Long)
data class DayLastPerformed(val dayId: Long, val last: Long)
data class MuscleGroupSets(val muscleGroup: MuscleGroup, val setCount: Int)

@Dao
interface SessionDao {
    @Insert
    suspend fun insertSession(session: WorkoutSession): Long

    @Update
    suspend fun updateSession(session: WorkoutSession)

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun sessionById(id: Long): WorkoutSession?

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Query("SELECT * FROM sessions WHERE endedAt IS NULL ORDER BY startedAt DESC")
    suspend fun openSessions(): List<WorkoutSession>

    @Query("SELECT * FROM sessions WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeOpenSession(): Flow<WorkoutSession?>

    @Query("UPDATE sessions SET lastActivityAt = :time WHERE id = :id")
    suspend fun touch(id: Long, time: Long)

    @Query("UPDATE sessions SET endedAt = :endedAt WHERE id = :id")
    suspend fun finish(id: Long, endedAt: Long)

    @Query("UPDATE sessions SET locked = :locked WHERE id = :id")
    suspend fun setLocked(id: Long, locked: Boolean)

    @Transaction
    @Query("SELECT * FROM sessions WHERE id = :id")
    fun observeSessionWithContent(id: Long): Flow<SessionWithContent?>

    @Transaction
    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun sessionWithContent(id: Long): SessionWithContent?

    @Query("SELECT * FROM sessions WHERE endedAt IS NOT NULL ORDER BY startedAt DESC")
    fun observeFinishedSessions(): Flow<List<WorkoutSession>>

    @Query("SELECT MAX(startedAt) FROM sessions WHERE endedAt IS NOT NULL")
    suspend fun lastFinishedSessionDate(): Long?

    @Insert
    suspend fun insertSessionExercise(se: SessionExercise): Long

    @Update
    suspend fun updateSessionExercise(se: SessionExercise)

    @Delete
    suspend fun deleteSessionExercise(se: SessionExercise)

    @Query("SELECT * FROM session_exercises WHERE id = :id")
    suspend fun sessionExerciseById(id: Long): SessionExercise?

    @Insert
    suspend fun insertLoggedSet(set: LoggedSet): Long

    @Update
    suspend fun updateLoggedSet(set: LoggedSet)

    @Delete
    suspend fun deleteLoggedSet(set: LoggedSet)

    @Query("SELECT * FROM logged_sets WHERE id = :id")
    suspend fun loggedSetById(id: Long): LoggedSet?

    /**
     * The most recent finished session (before [before]) that contains [exerciseId],
     * optionally restricted to a gym (used for MACHINE_LEVEL exercises).
     */
    @Query(
        "SELECT s.id FROM sessions s " +
            "JOIN session_exercises se ON se.sessionId = s.id " +
            "WHERE se.exerciseId = :exerciseId AND s.endedAt IS NOT NULL AND s.startedAt < :before " +
            "AND (:gymId IS NULL OR s.gymId = :gymId) " +
            "ORDER BY s.startedAt DESC LIMIT 1"
    )
    suspend fun lastSessionIdWithExercise(exerciseId: Long, before: Long, gymId: Long?): Long?

    @Query(
        "SELECT ls.* FROM logged_sets ls " +
            "JOIN session_exercises se ON ls.sessionExerciseId = se.id " +
            "WHERE se.sessionId = :sessionId AND se.exerciseId = :exerciseId AND ls.completed = 1 " +
            "ORDER BY ls.position"
    )
    suspend fun completedSetsInSession(sessionId: Long, exerciseId: Long): List<LoggedSet>

    /** All completed sets for an exercise with session context, oldest first. */
    @Query(
        "SELECT ls.*, s.id AS sessionId, s.startedAt AS sessionDate, s.gymId AS sessionGymId, " +
            "se.exerciseId AS exerciseId " +
            "FROM logged_sets ls " +
            "JOIN session_exercises se ON ls.sessionExerciseId = se.id " +
            "JOIN sessions s ON se.sessionId = s.id " +
            "WHERE se.exerciseId = :exerciseId AND ls.completed = 1 " +
            "ORDER BY s.startedAt"
    )
    suspend fun setsForExercise(exerciseId: Long): List<SetWithContext>

    @Query(
        "SELECT ls.*, s.id AS sessionId, s.startedAt AS sessionDate, s.gymId AS sessionGymId, " +
            "se.exerciseId AS exerciseId " +
            "FROM logged_sets ls " +
            "JOIN session_exercises se ON ls.sessionExerciseId = se.id " +
            "JOIN sessions s ON se.sessionId = s.id " +
            "WHERE se.exerciseId = :exerciseId AND ls.completed = 1 " +
            "ORDER BY s.startedAt"
    )
    fun observeSetsForExercise(exerciseId: Long): Flow<List<SetWithContext>>

    /** Start times of finished sessions in a range (frequency heatmap). */
    @Query(
        "SELECT startedAt FROM sessions WHERE endedAt IS NOT NULL AND startedAt >= :from " +
            "ORDER BY startedAt"
    )
    fun observeSessionDates(from: Long): Flow<List<Long>>

    /** Completed non-warmup sets per muscle group in a range. */
    @Query(
        "SELECT e.muscleGroup AS muscleGroup, COUNT(*) AS setCount " +
            "FROM logged_sets ls " +
            "JOIN session_exercises se ON ls.sessionExerciseId = se.id " +
            "JOIN sessions s ON se.sessionId = s.id " +
            "JOIN exercises e ON se.exerciseId = e.id " +
            "WHERE ls.completed = 1 AND ls.setType != 'WARMUP' AND s.startedAt >= :from " +
            "GROUP BY e.muscleGroup ORDER BY setCount DESC"
    )
    fun observeMuscleGroupSets(from: Long): Flow<List<MuscleGroupSets>>

    /** Every completed set with context, for CSV export. */
    @Query(
        "SELECT ls.*, s.id AS sessionId, s.startedAt AS sessionDate, s.gymId AS sessionGymId, " +
            "se.exerciseId AS exerciseId " +
            "FROM logged_sets ls " +
            "JOIN session_exercises se ON ls.sessionExerciseId = se.id " +
            "JOIN sessions s ON se.sessionId = s.id " +
            "WHERE ls.completed = 1 " +
            "ORDER BY s.startedAt, se.blockPos, se.inBlockPos, ls.position"
    )
    suspend fun allCompletedSets(): List<SetWithContext>
}

@Dao
interface GymDao {
    @Query("SELECT * FROM gyms ORDER BY name")
    fun observeAll(): Flow<List<Gym>>

    @Query("SELECT * FROM gyms ORDER BY name")
    suspend fun getAll(): List<Gym>

    @Query("SELECT * FROM gyms WHERE id = :id")
    suspend fun byId(id: Long): Gym?

    @Query("SELECT * FROM gyms WHERE isDefault = 1 LIMIT 1")
    suspend fun defaultGym(): Gym?

    @Insert
    suspend fun insert(gym: Gym): Long

    @Update
    suspend fun update(gym: Gym)

    @Delete
    suspend fun delete(gym: Gym)

    @Query("UPDATE gyms SET isDefault = 0")
    suspend fun clearDefault()

    @Query("UPDATE gyms SET isDefault = 1 WHERE id = :id")
    suspend fun markDefault(id: Long)

    @Transaction
    suspend fun setDefault(id: Long?) {
        clearDefault()
        if (id != null) markDefault(id)
    }

    @Query("SELECT exerciseId FROM gym_exercises WHERE gymId = :gymId")
    suspend fun exerciseIdsAt(gymId: Long): List<Long>

    @Query("SELECT exerciseId FROM gym_exercises WHERE gymId = :gymId")
    fun observeExerciseIdsAt(gymId: Long): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addExercise(link: GymExercise)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addExercises(links: List<GymExercise>)

    @Query("DELETE FROM gym_exercises WHERE gymId = :gymId AND exerciseId = :exerciseId")
    suspend fun removeExercise(gymId: Long, exerciseId: Long)

    @Query("SELECT * FROM gym_overrides WHERE gymId = :gymId")
    suspend fun overridesAt(gymId: Long): List<GymOverride>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setOverride(override: GymOverride)

    @Query("DELETE FROM gym_overrides WHERE gymId = :gymId AND plannedExerciseId = :plannedExerciseId")
    suspend fun removeOverride(gymId: Long, plannedExerciseId: Long)
}

@Dao
interface BodyMetricDao {
    @Query("SELECT * FROM body_metrics ORDER BY date DESC")
    fun observeAll(): Flow<List<BodyMetric>>

    @Query("SELECT * FROM body_metrics WHERE weightKg IS NOT NULL AND date <= :date ORDER BY date DESC LIMIT 1")
    suspend fun latestWeightAtOrBefore(date: Long): BodyMetric?

    @Insert
    suspend fun insert(metric: BodyMetric): Long

    @Update
    suspend fun update(metric: BodyMetric)

    @Delete
    suspend fun delete(metric: BodyMetric)
}

/** Full-table access used by JSON export/import. Order matters on restore (FK constraints). */
@Dao
interface BackupDao {
    @Query("SELECT * FROM exercise_groups")
    suspend fun allGroups(): List<ExerciseGroup>

    @Query("SELECT * FROM exercises")
    suspend fun allExercises(): List<Exercise>

    @Query("SELECT * FROM routines")
    suspend fun allRoutines(): List<Routine>

    @Query("SELECT * FROM routine_days")
    suspend fun allDays(): List<RoutineDay>

    @Query("SELECT * FROM planned_exercises")
    suspend fun allPlannedExercises(): List<PlannedExercise>

    @Query("SELECT * FROM planned_sets")
    suspend fun allPlannedSets(): List<PlannedSet>

    @Query("SELECT * FROM gyms")
    suspend fun allGyms(): List<Gym>

    @Query("SELECT * FROM gym_exercises")
    suspend fun allGymExercises(): List<GymExercise>

    @Query("SELECT * FROM gym_overrides")
    suspend fun allGymOverrides(): List<GymOverride>

    @Query("SELECT * FROM sessions")
    suspend fun allSessions(): List<WorkoutSession>

    @Query("SELECT * FROM session_exercises")
    suspend fun allSessionExercises(): List<SessionExercise>

    @Query("SELECT * FROM logged_sets")
    suspend fun allLoggedSets(): List<LoggedSet>

    @Query("SELECT * FROM body_metrics")
    suspend fun allBodyMetrics(): List<BodyMetric>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroups(items: List<ExerciseGroup>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(items: List<Exercise>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutines(items: List<Routine>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDays(items: List<RoutineDay>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlannedExercises(items: List<PlannedExercise>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlannedSets(items: List<PlannedSet>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGyms(items: List<Gym>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGymExercises(items: List<GymExercise>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGymOverrides(items: List<GymOverride>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(items: List<WorkoutSession>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionExercises(items: List<SessionExercise>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoggedSets(items: List<LoggedSet>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBodyMetrics(items: List<BodyMetric>)

    @Query("DELETE FROM logged_sets")
    suspend fun clearLoggedSets()

    @Query("DELETE FROM session_exercises")
    suspend fun clearSessionExercises()

    @Query("DELETE FROM sessions")
    suspend fun clearSessions()

    @Query("DELETE FROM gym_overrides")
    suspend fun clearGymOverrides()

    @Query("DELETE FROM gym_exercises")
    suspend fun clearGymExercises()

    @Query("DELETE FROM gyms")
    suspend fun clearGyms()

    @Query("DELETE FROM planned_sets")
    suspend fun clearPlannedSets()

    @Query("DELETE FROM planned_exercises")
    suspend fun clearPlannedExercises()

    @Query("DELETE FROM routine_days")
    suspend fun clearDays()

    @Query("DELETE FROM routines")
    suspend fun clearRoutines()

    @Query("DELETE FROM exercises")
    suspend fun clearExercises()

    @Query("DELETE FROM exercise_groups")
    suspend fun clearGroups()

    @Query("DELETE FROM body_metrics")
    suspend fun clearBodyMetrics()

    /** Removes user-created exercises; their planned and logged uses cascade away. */
    @Query("DELETE FROM exercises WHERE isCustom = 1")
    suspend fun clearCustomExercises()

    /** Removes all logged workout sessions. */
    @Transaction
    suspend fun clearSessionLog() {
        clearLoggedSets()
        clearSessionExercises()
        clearSessions()
    }

    @Transaction
    suspend fun clearAll() {
        clearLoggedSets()
        clearSessionExercises()
        clearSessions()
        clearGymOverrides()
        clearGymExercises()
        clearGyms()
        clearPlannedSets()
        clearPlannedExercises()
        clearDays()
        clearRoutines()
        clearExercises()
        clearGroups()
        clearBodyMetrics()
    }
}
