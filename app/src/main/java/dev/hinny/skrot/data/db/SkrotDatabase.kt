package dev.hinny.skrot.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
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

@Database(
    entities = [
        Exercise::class,
        ExerciseGroup::class,
        Routine::class,
        RoutineDay::class,
        PlannedExercise::class,
        PlannedSet::class,
        Gym::class,
        GymExercise::class,
        GymOverride::class,
        WorkoutSession::class,
        SessionExercise::class,
        LoggedSet::class,
        BodyMetric::class,
    ],
    version = 4,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class SkrotDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun routineDao(): RoutineDao
    abstract fun sessionDao(): SessionDao
    abstract fun gymDao(): GymDao
    abstract fun bodyMetricDao(): BodyMetricDao
    abstract fun backupDao(): BackupDao

    companion object {
        /**
         * v1 -> v2: equipment became a multi-value field (comma-joined enum names
         * in the same TEXT column) and the BODYWEIGHT value was replaced by NONE.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("UPDATE exercises SET equipment = 'NONE' WHERE equipment = 'BODYWEIGHT'")
            }
        }

        /** v2 -> v3: sessions can be locked against structural edits. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sessions ADD COLUMN locked INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * v3 -> v4: target-rep ranges were removed. Progression already keyed off
         * the top of the range, so the max is what survives as the single target;
         * the column stays (unused, always null) for backward-compatible backups.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    "UPDATE planned_sets SET targetRepsMin = COALESCE(targetRepsMax, targetRepsMin), " +
                        "targetRepsMax = NULL"
                )
            }
        }

        /** Migrations from version 1 onward are registered here. */
        val MIGRATIONS: Array<Migration> =
            arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)

        fun build(context: Context): SkrotDatabase =
            Room.databaseBuilder(context, SkrotDatabase::class.java, "skrot.db")
                .addMigrations(*MIGRATIONS)
                .build()
    }
}
