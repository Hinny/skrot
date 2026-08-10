package dev.hinny.skrot.ui.common

import dev.hinny.skrot.data.model.DayWithContent
import dev.hinny.skrot.data.model.Exercise
import dev.hinny.skrot.data.model.MuscleGroup
import dev.hinny.skrot.data.model.PlannedExercise
import dev.hinny.skrot.data.model.PlannedExerciseWithDetails
import dev.hinny.skrot.data.model.PlannedSet
import dev.hinny.skrot.data.model.RoutineDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The editors write by diffing a draft against the baseline: rows the draft
 * dropped are deleted, rows it invented carry negative placeholder ids and are
 * inserted, the rest are updated.
 *
 * The diff itself lives inside `applyChanges`, which needs a database. What is
 * checked here is the classification the diff is built on — which rows are new,
 * which are gone, which survive — because getting that wrong is what would
 * delete the wrong thing, and it is pure list arithmetic.
 */
class DraftDiffTest {

    private fun exercise(id: Long) = Exercise(
        id = id,
        nameEn = "e$id",
        nameSv = "e$id",
        muscleGroup = MuscleGroup.CHEST,
    )

    private fun planned(id: Long, blockPos: Int = 0, sets: List<PlannedSet> = emptyList()) =
        PlannedExerciseWithDetails(
            planned = PlannedExercise(id = id, dayId = 1, exerciseId = id, blockPos = blockPos),
            exercise = exercise(id),
            sets = sets,
        )

    private fun day(vararg exercises: PlannedExerciseWithDetails) = DayWithContent(
        day = RoutineDay(id = 1, routineId = 1, name = "Push", position = 0),
        exercises = exercises.toList(),
    )

    /** Mirrors the classification `applyChanges` performs. */
    private fun gone(baseline: DayWithContent, draft: DayWithContent) =
        baseline.exercises.filter { old -> draft.exercises.none { it.planned.id == old.planned.id } }

    private fun invented(draft: DayWithContent) =
        draft.exercises.filter { it.planned.id <= 0 }

    @Test
    fun `an untouched draft deletes and inserts nothing`() {
        val base = day(planned(1), planned(2))
        assertTrue(gone(base, base).isEmpty())
        assertTrue(invented(base).isEmpty())
    }

    @Test
    fun `a removed exercise is the only one marked gone`() {
        val base = day(planned(1), planned(2))
        val draft = day(planned(1))
        assertEquals(listOf(2L), gone(base, draft).map { it.planned.id })
        assertTrue(invented(draft).isEmpty())
    }

    @Test
    fun `a drafted exercise is recognised by its negative id`() {
        val base = day(planned(1))
        val draft = day(planned(1), planned(-1))
        assertEquals(listOf(-1L), invented(draft).map { it.planned.id })
        // Adding must never be read as removing something.
        assertTrue(gone(base, draft).isEmpty())
    }

    @Test
    fun `removing and adding in one pass keeps the two apart`() {
        val base = day(planned(1), planned(2))
        val draft = day(planned(1), planned(-1))
        assertEquals(listOf(2L), gone(base, draft).map { it.planned.id })
        assertEquals(listOf(-1L), invented(draft).map { it.planned.id })
    }

    @Test
    fun `reordering blocks changes no ids, so nothing is deleted or inserted`() {
        val base = day(planned(1, blockPos = 0), planned(2, blockPos = 1))
        val draft = day(planned(1, blockPos = 1), planned(2, blockPos = 0))
        assertTrue(gone(base, draft).isEmpty())
        assertTrue(invented(draft).isEmpty())
        // ...but the draft is genuinely different, so the bar must appear.
        assertTrue(draft != base)
    }

    @Test
    fun `blocks group by blockPos and order by inBlockPos`() {
        val a = planned(1, blockPos = 0)
        val b = PlannedExerciseWithDetails(
            planned = PlannedExercise(id = 2, dayId = 1, exerciseId = 2, blockPos = 0, inBlockPos = 1),
            exercise = exercise(2),
            sets = emptyList(),
        )
        val c = planned(3, blockPos = 1)
        val blocks = day(c, b, a).blocks
        assertEquals(2, blocks.size)
        assertEquals(listOf(1L, 2L), blocks[0].map { it.planned.id })
        assertEquals(listOf(3L), blocks[1].map { it.planned.id })
    }

    @Test
    fun `a drafted set is spotted inside a persisted exercise`() {
        val withNewSet = planned(
            1,
            sets = listOf(
                PlannedSet(id = 10, plannedExerciseId = 1, position = 0),
                PlannedSet(id = -1, plannedExerciseId = 1, position = 1),
            ),
        )
        val draft = day(withNewSet)
        val hasNewRows = draft.exercises.any { pe ->
            pe.planned.id <= 0 || pe.sets.any { it.id <= 0 }
        }
        assertTrue(hasNewRows)
    }

    @Test
    fun `sets sort by position regardless of insertion order`() {
        val pe = planned(
            1,
            sets = listOf(
                PlannedSet(id = 3, plannedExerciseId = 1, position = 2),
                PlannedSet(id = 1, plannedExerciseId = 1, position = 0),
                PlannedSet(id = 2, plannedExerciseId = 1, position = 1),
            ),
        )
        assertEquals(listOf(1L, 2L, 3L), pe.sortedSets.map { it.id })
    }
}
