package dev.hinny.skrot.domain

import dev.hinny.skrot.data.model.LoggedSet
import dev.hinny.skrot.data.model.PlannedSet
import dev.hinny.skrot.data.model.PrefillMode
import dev.hinny.skrot.data.model.SetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PrefillEngineTest {

    private val planned = PlannedSet(
        id = 1, plannedExerciseId = 1, position = 0,
        targetRepsMin = 8, targetRepsMax = 12, targetLoad = 60.0, restSec = 90,
    )
    private val lastSets = listOf(
        LoggedSet(sessionExerciseId = 1, position = 0, load = 65.0, reps = 11, completed = true),
        LoggedSet(sessionExerciseId = 1, position = 1, load = 65.0, reps = 9, completed = true),
    )

    @Test
    fun `LAST_SESSION uses previous actual weight and reps`() {
        val p = PrefillEngine.prefill(PrefillMode.LAST_SESSION, planned, lastSets, 0, SetType.STANDARD)
        assertEquals(65.0, p.load!!, 0.0)
        assertEquals(11, p.reps)
        val p2 = PrefillEngine.prefill(PrefillMode.LAST_SESSION, planned, lastSets, 1, SetType.STANDARD)
        assertEquals(9, p2.reps)
    }

    @Test
    fun `LAST_SESSION falls back to targets without history`() {
        val p = PrefillEngine.prefill(PrefillMode.LAST_SESSION, planned, emptyList(), 0, SetType.STANDARD)
        assertEquals(60.0, p.load!!, 0.0)
        assertEquals(8, p.reps)
    }

    @Test
    fun `TARGETS uses planned weight and reps even with history`() {
        val p = PrefillEngine.prefill(PrefillMode.TARGETS, planned, lastSets, 0, SetType.STANDARD)
        assertEquals(60.0, p.load!!, 0.0)
        assertEquals(8, p.reps)
    }

    @Test
    fun `HYBRID uses last session's weight with target reps`() {
        val p = PrefillEngine.prefill(PrefillMode.HYBRID, planned, lastSets, 0, SetType.STANDARD)
        assertEquals(65.0, p.load!!, 0.0)
        assertEquals(8, p.reps)
    }

    @Test
    fun `ad-hoc set without plan or history is empty`() {
        val p = PrefillEngine.prefill(PrefillMode.LAST_SESSION, null, emptyList(), 0, SetType.STANDARD)
        assertNull(p.load)
        assertNull(p.reps)
    }

    @Test
    fun `type index matches within the same set type only`() {
        val mixed = listOf(
            LoggedSet(sessionExerciseId = 1, position = 0, setType = SetType.WARMUP, load = 40.0, reps = 10, completed = true),
            LoggedSet(sessionExerciseId = 1, position = 1, setType = SetType.STANDARD, load = 70.0, reps = 8, completed = true),
        )
        // First STANDARD set should match the standard 70 kg set, not the warmup.
        val p = PrefillEngine.prefill(PrefillMode.LAST_SESSION, planned, mixed, 0, SetType.STANDARD)
        assertEquals(70.0, p.load!!, 0.0)
    }
}
