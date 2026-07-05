package dev.hinny.skrot.domain

import dev.hinny.skrot.data.model.MeasurementType
import dev.hinny.skrot.data.model.SetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrDetectorTest {

    private val history = listOf(
        SetRecord(100.0, 5),
        SetRecord(95.0, 8),
        SetRecord(80.0, 10),
    )

    @Test
    fun `heavier weight is a weight PR`() {
        val prs = PrDetector.detect(MeasurementType.WEIGHT_KG, SetRecord(102.5, 3), history)
        assertTrue(PrType.HEAVIEST_WEIGHT in prs)
    }

    @Test
    fun `more reps at a known weight is a rep PR`() {
        val prs = PrDetector.detect(MeasurementType.WEIGHT_KG, SetRecord(100.0, 6), history)
        assertTrue(PrType.REP_PR_AT_WEIGHT in prs)
    }

    @Test
    fun `better estimated 1RM is an e1RM PR`() {
        // best history e1rm: 95x8 -> 120.3; 100x7 -> 123.3
        val prs = PrDetector.detect(MeasurementType.WEIGHT_KG, SetRecord(100.0, 7), history)
        assertTrue(PrType.BEST_E1RM in prs)
    }

    @Test
    fun `warmup sets never count as PRs`() {
        val prs = PrDetector.detect(
            MeasurementType.WEIGHT_KG,
            SetRecord(150.0, 5, setType = SetType.WARMUP),
            history,
        )
        assertTrue(prs.isEmpty())
    }

    @Test
    fun `warmup sets in history are ignored`() {
        val withWarmup = listOf(SetRecord(200.0, 5, setType = SetType.WARMUP), SetRecord(100.0, 5))
        val prs = PrDetector.detect(MeasurementType.WEIGHT_KG, SetRecord(110.0, 5), withWarmup)
        assertTrue(PrType.HEAVIEST_WEIGHT in prs)
    }

    @Test
    fun `first ever set is not a PR`() {
        val prs = PrDetector.detect(MeasurementType.WEIGHT_KG, SetRecord(100.0, 5), emptyList())
        assertTrue(prs.isEmpty())
    }

    @Test
    fun `machine level PRs are scoped to the current gym`() {
        val gymHistory = listOf(
            SetRecord(9.0, 10, gymId = 1),
            SetRecord(5.0, 10, gymId = 2),
        )
        // Level 7 at gym 2 beats gym 2's best (5) even though gym 1 has seen level 9.
        val prs = PrDetector.detect(
            MeasurementType.MACHINE_LEVEL, SetRecord(7.0, 10, gymId = 2), gymHistory, currentGymId = 2,
        )
        assertEquals(listOf(PrType.HIGHEST_LEVEL), prs)
    }

    @Test
    fun `bodyweight rep and added weight PRs`() {
        val bwHistory = listOf(SetRecord(0.0, 10), SetRecord(5.0, 6))
        val repPr = PrDetector.detect(MeasurementType.BODYWEIGHT, SetRecord(0.0, 12), bwHistory)
        assertTrue(PrType.MOST_REPS in repPr)
        val weightPr = PrDetector.detect(MeasurementType.BODYWEIGHT, SetRecord(10.0, 5), bwHistory)
        assertTrue(PrType.MOST_ADDED_WEIGHT in weightPr)
    }
}
