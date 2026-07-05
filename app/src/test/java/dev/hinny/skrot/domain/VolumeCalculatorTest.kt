package dev.hinny.skrot.domain

import dev.hinny.skrot.data.model.MeasurementType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VolumeCalculatorTest {

    @Test
    fun `weight volume is load times reps`() {
        assertEquals(
            500.0,
            VolumeCalculator.setVolumeKg(MeasurementType.WEIGHT_KG, 100.0, 5, 75.0, 100)!!,
            0.0,
        )
    }

    @Test
    fun `bodyweight volume uses bodyweight times factor plus added weight`() {
        // (80 * 0.65 + 10) * 10 = 620
        assertEquals(
            620.0,
            VolumeCalculator.setVolumeKg(MeasurementType.BODYWEIGHT, 10.0, 10, 80.0, 65)!!,
            0.001,
        )
    }

    @Test
    fun `assistance counts as negative added weight`() {
        // (80 * 1.0 - 20) * 5 = 300
        assertEquals(
            300.0,
            VolumeCalculator.setVolumeKg(MeasurementType.BODYWEIGHT, -20.0, 5, 80.0, 100)!!,
            0.001,
        )
    }

    @Test
    fun `machine levels are excluded from kg volume`() {
        assertNull(VolumeCalculator.setVolumeKg(MeasurementType.MACHINE_LEVEL, 7.0, 10, 75.0, 100))
    }

    @Test
    fun `default fallback bodyweight is 75 kg`() {
        assertEquals(75.0, VolumeCalculator.DEFAULT_BODYWEIGHT_FALLBACK_KG, 0.0)
    }
}
