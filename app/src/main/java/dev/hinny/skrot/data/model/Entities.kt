package dev.hinny.skrot.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * An exercise in the library. Seed exercises carry both English and Swedish names;
 * custom exercises store the user-given name in both fields.
 */
@Serializable
@Entity(
    tableName = "exercises",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseGroup::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("groupId")],
)
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nameEn: String,
    val nameSv: String,
    val muscleGroup: MuscleGroup,
    val secondaryMuscles: List<MuscleGroup> = emptyList(),
    val equipment: Equipment = Equipment.OTHER,
    val measurementType: MeasurementType = MeasurementType.WEIGHT_KG,
    val isCustom: Boolean = false,
    val notes: String = "",
    /** Percent of body weight actually lifted; used for volume of BODYWEIGHT exercises. */
    val bodyweightFactor: Int = 100,
    /** Interchangeable-exercise group; at most one per exercise. */
    val groupId: Long? = null,
    /** Short note shown prominently the next time this exercise comes up. */
    val nextTimeNote: String = "",
    /** Per-exercise progression increment override (kg or levels); null = use the global default. */
    val progressionIncrement: Double? = null,
)

/** A named group of interchangeable exercises (e.g. "Horizontal press"). */
@Serializable
@Entity(tableName = "exercise_groups")
data class ExerciseGroup(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nameEn: String,
    val nameSv: String,
    val isCustom: Boolean = false,
)

/** A training program ("Routine" internally, "Program" in the UI). */
@Serializable
@Entity(tableName = "routines")
data class Routine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val icon: ProgramIcon = ProgramIcon.BARBELL,
    val prefillMode: PrefillMode = PrefillMode.LAST_SESSION,
    val scheduleMode: ScheduleMode = ScheduleMode.ROTATING,
    /** Exactly zero or one routine is active at a time (enforced in the DAO). */
    val isActive: Boolean = false,
    /** Free-form tags, e.g. "rebuild" (used by the comeback suggestion). */
    val tags: List<String> = emptyList(),
    /** Index into the ordered day list of the next day to perform (rotating mode). */
    val nextDayIndex: Int = 0,
    val position: Int = 0,
)

@Serializable
@Entity(
    tableName = "routine_days",
    foreignKeys = [
        ForeignKey(
            entity = Routine::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("routineId")],
)
data class RoutineDay(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val name: String,
    val icon: ProgramIcon? = null,
    val description: String = "",
    val position: Int = 0,
    /** ISO weekday numbers (1 = Monday .. 7 = Sunday) for FIXED_WEEKDAYS scheduling. */
    val weekdays: List<Int> = emptyList(),
)

/**
 * An exercise planned inside a routine day. Exercises sharing the same [blockPos]
 * form a block; a block with two or more exercises is a superset/circuit.
 */
@Serializable
@Entity(
    tableName = "planned_exercises",
    foreignKeys = [
        ForeignKey(
            entity = RoutineDay::class,
            parentColumns = ["id"],
            childColumns = ["dayId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("dayId"), Index("exerciseId")],
)
data class PlannedExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayId: Long,
    val exerciseId: Long,
    val blockPos: Int = 0,
    val inBlockPos: Int = 0,
)

@Serializable
@Entity(
    tableName = "planned_sets",
    foreignKeys = [
        ForeignKey(
            entity = PlannedExercise::class,
            parentColumns = ["id"],
            childColumns = ["plannedExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("plannedExerciseId")],
)
data class PlannedSet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plannedExerciseId: Long,
    val position: Int = 0,
    val setType: SetType = SetType.STANDARD,
    /** Target reps: single value stored in [targetRepsMin]; a range also sets [targetRepsMax]. */
    val targetRepsMin: Int? = null,
    val targetRepsMax: Int? = null,
    /** Optional target load in kg (or machine level). */
    val targetLoad: Double? = null,
    /** Rest timer duration in seconds after this set; 0 = no timer. */
    val restSec: Int = 90,
)

@Serializable
@Entity(tableName = "gyms")
data class Gym(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isDefault: Boolean = false,
)

/** Marks an exercise as available at a gym. */
@Serializable
@Entity(
    tableName = "gym_exercises",
    primaryKeys = ["gymId", "exerciseId"],
    foreignKeys = [
        ForeignKey(
            entity = Gym::class,
            parentColumns = ["id"],
            childColumns = ["gymId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("exerciseId")],
)
data class GymExercise(
    val gymId: Long,
    val exerciseId: Long,
)

/** "Always use this exercise at this gym" override for a planned exercise. */
@Serializable
@Entity(
    tableName = "gym_overrides",
    primaryKeys = ["gymId", "plannedExerciseId"],
    foreignKeys = [
        ForeignKey(
            entity = Gym::class,
            parentColumns = ["id"],
            childColumns = ["gymId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PlannedExercise::class,
            parentColumns = ["id"],
            childColumns = ["plannedExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("plannedExerciseId"), Index("exerciseId")],
)
data class GymOverride(
    val gymId: Long,
    val plannedExerciseId: Long,
    val exerciseId: Long,
)

@Serializable
@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = RoutineDay::class,
            parentColumns = ["id"],
            childColumns = ["routineDayId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = Routine::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = Gym::class,
            parentColumns = ["id"],
            childColumns = ["gymId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("routineDayId"), Index("routineId"), Index("gymId"), Index("startedAt")],
)
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val endedAt: Long? = null,
    val routineId: Long? = null,
    val routineDayId: Long? = null,
    val gymId: Long? = null,
    val note: String = "",
    /** Timestamp of the last user interaction; drives auto-finish. */
    val lastActivityAt: Long,
    /** True when started in "temporary visit" mode (no gym availability filtering). */
    val temporaryVisit: Boolean = false,
)

/** An exercise instance inside a session. */
@Serializable
@Entity(
    tableName = "session_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId"), Index("exerciseId")],
)
data class SessionExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    /** Back-reference to the plan, used for target/rest write-back; null for ad-hoc exercises. */
    val plannedExerciseId: Long? = null,
    val blockPos: Int = 0,
    val inBlockPos: Int = 0,
    val note: String = "",
)

@Serializable
@Entity(
    tableName = "logged_sets",
    foreignKeys = [
        ForeignKey(
            entity = SessionExercise::class,
            parentColumns = ["id"],
            childColumns = ["sessionExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionExerciseId")],
)
data class LoggedSet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionExerciseId: Long,
    val position: Int = 0,
    val setType: SetType = SetType.STANDARD,
    /**
     * Load interpreted by the exercise's measurement type:
     * kg for WEIGHT_KG, integer level for MACHINE_LEVEL,
     * added weight in kg (negative = assistance) for BODYWEIGHT.
     */
    val load: Double = 0.0,
    val reps: Int = 0,
    val completed: Boolean = false,
    val note: String = "",
    /** Rest duration attached to this set when it was logged. */
    val restSec: Int = 90,
    val completedAt: Long? = null,
)

@Serializable
@Entity(tableName = "body_metrics", indices = [Index("date")])
data class BodyMetric(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val weightKg: Double? = null,
    val waistCm: Double? = null,
    val chestCm: Double? = null,
    val armsCm: Double? = null,
    val thighsCm: Double? = null,
    val hipsCm: Double? = null,
)
