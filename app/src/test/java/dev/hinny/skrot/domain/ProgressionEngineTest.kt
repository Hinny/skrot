package dev.hinny.skrot.domain

import dev.hinny.skrot.data.model.LoggedSet
import dev.hinny.skrot.data.model.MeasurementType
import dev.hinny.skrot.data.model.PlannedSet
import dev.hinny.skrot.data.model.SetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionEngineTest {

    private fun plannedSets(vararg targets: Int) = targets.mapIndexed { i, target ->
        PlannedSet(
            id = i.toLong(), plannedExerciseId = 1, position = i,
            targetRepsMin = target,
        )
    }

    private fun logged(vararg sets: Pair<Double, Int>) = sets.mapIndexed { i, (load, reps) ->
        LoggedSet(
            id = i.toLong(), sessionExerciseId = 1, position = i,
            load = load, reps = reps, completed = true,
        )
    }

    @Test
    fun `hitting the target on all sets at the same load suggests an increase`() {
        val suggestion = ProgressionEngine.suggest(
            MeasurementType.WEIGHT_KG,
            logged(60.0 to 12, 60.0 to 12, 60.0 to 12),
            plannedSets(12, 12, 12),
        )
        val increase = suggestion as ProgressionSuggestion.IncreaseLoad
        assertEquals(62.5, increase.toLoad, 0.0)
    }

    @Test
    fun `missing the target on one set means no suggestion`() {
        val suggestion = ProgressionEngine.suggest(
            MeasurementType.WEIGHT_KG,
            logged(60.0 to 12, 60.0 to 11, 60.0 to 12),
            plannedSets(12, 12, 12),
        )
        assertNull(suggestion)
    }

    @Test
    fun `a legacy stored max is ignored - the minimum is the target`() {
        val planned = listOf(
            PlannedSet(
                id = 0, plannedExerciseId = 1, position = 0,
                targetRepsMin = 8, targetRepsMax = 12,
            )
        )
        val suggestion = ProgressionEngine.suggest(
            MeasurementType.WEIGHT_KG,
            logged(60.0 to 8),
            planned,
        )
        assertEquals(62.5, (suggestion as ProgressionSuggestion.IncreaseLoad).toLoad, 0.0)
    }

    @Test
    fun `different loads across sets means no suggestion`() {
        val suggestion = ProgressionEngine.suggest(
            MeasurementType.WEIGHT_KG,
            logged(60.0 to 12, 62.5 to 12),
            plannedSets(12, 12),
        )
        assertNull(suggestion)
    }

    @Test
    fun `machine level suggests plus one level`() {
        val suggestion = ProgressionEngine.suggest(
            MeasurementType.MACHINE_LEVEL,
            logged(7.0 to 12, 7.0 to 12),
            plannedSets(12, 12),
        )
        val increase = suggestion as ProgressionSuggestion.IncreaseLoad
        assertEquals(8.0, increase.toLoad, 0.0)
    }

    @Test
    fun `per-exercise increment override wins`() {
        val suggestion = ProgressionEngine.suggest(
            MeasurementType.WEIGHT_KG,
            logged(100.0 to 5),
            plannedSets(5),
            exerciseIncrementOverride = 5.0,
        )
        assertEquals(105.0, (suggestion as ProgressionSuggestion.IncreaseLoad).toLoad, 0.0)
    }

    @Test
    fun `plain bodyweight suggests one more rep instead of load`() {
        val suggestion = ProgressionEngine.suggest(
            MeasurementType.BODYWEIGHT,
            logged(0.0 to 10, 0.0 to 10),
            plannedSets(10, 10),
        )
        val addRep = suggestion as ProgressionSuggestion.AddRep
        assertEquals(11, addRep.toReps)
    }

    @Test
    fun `weighted bodyweight suggests more added weight`() {
        val suggestion = ProgressionEngine.suggest(
            MeasurementType.BODYWEIGHT,
            logged(10.0 to 8, 10.0 to 8),
            plannedSets(8, 8),
        )
        assertTrue(suggestion is ProgressionSuggestion.IncreaseLoad)
    }

    @Test
    fun `warmup and drop sets are ignored by progression`() {
        val sets = listOf(
            LoggedSet(id = 0, sessionExerciseId = 1, position = 0, setType = SetType.WARMUP, load = 40.0, reps = 12, completed = true),
            LoggedSet(id = 1, sessionExerciseId = 1, position = 1, load = 60.0, reps = 12, completed = true),
            LoggedSet(id = 2, sessionExerciseId = 1, position = 2, setType = SetType.DROP_SET, load = 40.0, reps = 15, completed = true),
        )
        val planned = listOf(
            PlannedSet(id = 0, plannedExerciseId = 1, position = 0, setType = SetType.WARMUP, targetRepsMin = 12),
            PlannedSet(id = 1, plannedExerciseId = 1, position = 1, targetRepsMin = 12),
            PlannedSet(id = 2, plannedExerciseId = 1, position = 2, setType = SetType.DROP_SET),
        )
        val suggestion = ProgressionEngine.suggest(MeasurementType.WEIGHT_KG, sets, planned)
        assertEquals(62.5, (suggestion as ProgressionSuggestion.IncreaseLoad).toLoad, 0.0)
    }
}
