package dev.hinny.skrot.domain

import dev.hinny.skrot.data.model.WeightUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlateCalculatorTest {

    @Test
    fun `loads a common metric total from the heaviest plates down`() {
        // 40 kg a side, taken greedily: the 25 goes on before the 15.
        val loading = PlateCalculator.forTotal(total = 100.0, bar = 20.0)!!
        assertEquals(listOf(25.0, 15.0), loading.perSide)
        assertTrue(loading.isExact)
    }

    @Test
    fun `mixes plate sizes when the total is not a round multiple`() {
        val loading = PlateCalculator.forTotal(total = 87.5, bar = 20.0)!!
        assertEquals(listOf(25.0, 5.0, 2.5, 1.25), loading.perSide)
        assertTrue(loading.isExact)
    }

    @Test
    fun `the bar alone loads no plates`() {
        val loading = PlateCalculator.forTotal(total = 20.0, bar = 20.0)!!
        assertTrue(loading.perSide.isEmpty())
        assertTrue(loading.isExact)
    }

    @Test
    fun `a total below the bar has no loading`() {
        assertNull(PlateCalculator.forTotal(total = 15.0, bar = 20.0))
    }

    @Test
    fun `reports what the available plates cannot make up`() {
        // 0.5 kg per side short: no disc that small is on the rack.
        val loading = PlateCalculator.forTotal(total = 22.0, bar = 20.0)!!
        assertTrue(loading.perSide.isEmpty())
        assertEquals(2.0, loading.remainder, 0.001)
        assertTrue(!loading.isExact)
    }

    @Test
    fun `groups repeated discs for display`() {
        // 70 kg a side: two 25s and a 20.
        val loading = PlateCalculator.forTotal(total = 160.0, bar = 20.0)!!
        assertEquals(listOf(25.0, 25.0, 20.0), loading.perSide)
        assertEquals(listOf(25.0 to 2, 20.0 to 1), loading.perSideGrouped)
    }

    @Test
    fun `imperial racks use imperial discs`() {
        val plates = PlateCalculator.platesFor(WeightUnit.LBS)
        val loading = PlateCalculator.forTotal(total = 225.0, bar = 45.0, plates = plates)!!
        assertEquals(listOf(45.0, 45.0), loading.perSide)
        assertTrue(loading.isExact)
    }

    @Test
    fun `a bar of no weight has nothing to calculate`() {
        assertNull(PlateCalculator.forTotal(total = 100.0, bar = 0.0))
    }
}
