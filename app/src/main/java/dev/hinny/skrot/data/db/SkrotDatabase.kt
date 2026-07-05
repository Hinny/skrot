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
    version = 1,
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
        /** Migrations from version 1 onward are registered here. */
        val MIGRATIONS: Array<Migration> = emptyArray()

        fun build(context: Context): SkrotDatabase =
            Room.databaseBuilder(context, SkrotDatabase::class.java, "skrot.db")
                .addMigrations(*MIGRATIONS)
                .build()
    }
}
