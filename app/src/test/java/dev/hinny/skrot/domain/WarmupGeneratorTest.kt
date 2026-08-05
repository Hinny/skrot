package dev.hinny.skrot.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WarmupGeneratorTest {

    @Test
    fun `three sets ramp to the working load and stop short of it`() {
        val warmups = WarmupGenerator.generate(workingLoad = 100.0, count = 3, rounding = 2.5)
        assertEquals(listOf(40.0, 60.0, 80.0), warmups.map { it.load })
        assertTrue(warmups.all { it.load < 100.0 })
    }

    @Test
    fun `heavier rungs get fewer reps`() {
        val reps = WarmupGenerator.generate(100.0, count = 3, rounding = 2.5).map { it.reps }
        assertEquals(listOf(10, 8, 5), reps)
        assertEquals(reps.sortedDescending(), reps)
    }

    @Test
    fun `loads are rounded to something the gym can actually make`() {
        val warmups = WarmupGenerator.generate(workingLoad = 62.5, count = 3, rounding = 2.5)
        assertTrue(warmups.all { (it.load / 2.5) % 1.0 == 0.0 })
    }

    @Test
    fun `rungs that round onto each other are dropped`() {
        // At 10 kg with 2.5 kg rounding, 0.4 and 0.55 both land on 5.
        val warmups = WarmupGenerator.generate(workingLoad = 10.0, count = 4, rounding = 2.5)
        assertEquals(warmups.map { it.load }.distinct(), warmups.map { it.load })
    }

    @Test
    fun `a load too light to ramp produces nothing`() {
        assertTrue(WarmupGenerator.generate(workingLoad = 2.5, count = 3, rounding = 2.5).isEmpty())
    }

    @Test
    fun `bodyweight work with no added load has nothing to ramp`() {
        assertTrue(WarmupGenerator.generate(workingLoad = 0.0, count = 3, rounding = 2.5).isEmpty())
    }

    @Test
    fun `machine levels ramp in whole steps`() {
        val warmups = WarmupGenerator.generate(workingLoad = 10.0, count = 3, rounding = 1.0)
        assertEquals(listOf(4.0, 6.0, 8.0), warmups.map { it.load })
    }

    @Test
    fun `the count is clamped to what the ramps cover`() {
        assertTrue(WarmupGenerator.generate(100.0, count = 99, rounding = 2.5).isNotEmpty())
        assertTrue(WarmupGenerator.generate(100.0, count = 0, rounding = 2.5).isNotEmpty())
    }
}
