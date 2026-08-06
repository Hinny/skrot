package dev.hinny.skrot.data.model

import androidx.room.Embedded
import androidx.room.Relation

/*
 * The derived views below (sorted lists, superset blocks) are computed once per
 * instance rather than on every read. These objects are immutable snapshots
 * emitted by Room, and the logging screen recomposes every second from its
 * elapsed clock while touching them repeatedly per frame -- as `get()` they
 * re-sorted the whole session each time.
 *
 * `by lazy` is not part of a data class's equals/hashCode (only constructor
 * parameters are), so the editors that compare a draft against a baseline are
 * unaffected.
 */

data class RoutineWithDays(
    @Embedded val routine: Routine,
    @Relation(parentColumn = "id", entityColumn = "routineId")
    val days: List<RoutineDay>,
) {
    val sortedDays: List<RoutineDay> by lazy { days.sortedBy { it.position } }
}

data class PlannedExerciseWithDetails(
    @Embedded val planned: PlannedExercise,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: Exercise,
    @Relation(parentColumn = "id", entityColumn = "plannedExerciseId")
    val sets: List<PlannedSet>,
) {
    val sortedSets: List<PlannedSet> by lazy { sets.sortedBy { it.position } }
}

data class DayWithContent(
    @Embedded val day: RoutineDay,
    @Relation(entity = PlannedExercise::class, parentColumn = "id", entityColumn = "dayId")
    val exercises: List<PlannedExerciseWithDetails>,
) {
    /** Exercises ordered and grouped into blocks; a block of 2+ is a superset. */
    val blocks: List<List<PlannedExerciseWithDetails>> by lazy {
        exercises
            .sortedWith(compareBy({ it.planned.blockPos }, { it.planned.inBlockPos }))
            .groupBy { it.planned.blockPos }
            .toSortedMap()
            .values
            .toList()
    }
}

data class SessionExerciseWithDetails(
    @Embedded val sessionExercise: SessionExercise,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: Exercise,
    @Relation(parentColumn = "id", entityColumn = "sessionExerciseId")
    val sets: List<LoggedSet>,
) {
    val sortedSets: List<LoggedSet> by lazy { sets.sortedBy { it.position } }
}

data class SessionWithContent(
    @Embedded val session: WorkoutSession,
    @Relation(entity = SessionExercise::class, parentColumn = "id", entityColumn = "sessionId")
    val exercises: List<SessionExerciseWithDetails>,
) {
    val blocks: List<List<SessionExerciseWithDetails>> by lazy {
        exercises
            .sortedWith(compareBy({ it.sessionExercise.blockPos }, { it.sessionExercise.inBlockPos }))
            .groupBy { it.sessionExercise.blockPos }
            .toSortedMap()
            .values
            .toList()
    }
}

/** A logged set joined with its session metadata, for history/stats/PR queries. */
data class SetWithContext(
    @Embedded val set: LoggedSet,
    val sessionId: Long,
    val sessionDate: Long,
    val sessionGymId: Long?,
    val exerciseId: Long,
)
