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
    version = 6,
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
         * v3 -> v4: target-rep ranges were removed. The single target is the reps
         * you must reach for the set to count, so the bottom of the range is what
         * survives — the top was only ever a progression trigger, and a max of 12
         * left behind by the old editor default is not a target anyone chose.
         * The column stays (unused, always null) for backward-compatible backups.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("UPDATE planned_sets SET targetRepsMax = NULL")
            }
        }

        /**
         * v4 -> v5: recovery programs became an explicit flag instead of a magic
         * "rebuild" tag. Tags are joined with the ASCII unit separator, so a LIKE
         * on the bare word matches whether it stands alone or sits among others.
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE routines ADD COLUMN isRecovery INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "UPDATE routines SET isRecovery = 1 WHERE tags = 'rebuild' " +
                        "OR tags LIKE 'rebuild' || char(31) || '%' " +
                        "OR tags LIKE '%' || char(31) || 'rebuild' " +
                        "OR tags LIKE '%' || char(31) || 'rebuild' || char(31) || '%'"
                )
            }
        }

        /**
         * v5 -> v6: sets can carry their own rep target, so an exercise added
         * mid-session is editable instead of showing a dead "—".
         */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE logged_sets ADD COLUMN targetReps INTEGER")
            }
        }

        /** Migrations from version 1 onward are registered here. */
        val MIGRATIONS: Array<Migration> = arrayOf(
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6,
        )

        fun build(context: Context): SkrotDatabase =
            Room.databaseBuilder(context, SkrotDatabase::class.java, "skrot.db")
                .addMigrations(*MIGRATIONS)
                .build()
    }
}
