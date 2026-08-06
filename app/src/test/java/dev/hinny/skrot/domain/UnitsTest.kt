package dev.hinny.skrot.domain

import dev.hinny.skrot.data.model.MeasurementType
import dev.hinny.skrot.data.model.WeightUnit
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The formatting six screens used to do for themselves. The cases that matter
 * are the ones they used to disagree about: machine levels and lbs.
 */
class UnitsTest {

    @Test
    fun `unit label follows the setting`() {
        assertEquals("kg", Units.unitLabel(WeightUnit.KG))
        assertEquals("lbs", Units.unitLabel(WeightUnit.LBS))
    }

    @Test
    fun `weight is formatted in the display unit with its label`() {
        assertEquals("100 kg", Units.formatWeight(100.0, WeightUnit.KG))
        assertEquals("220.46 lbs", Units.formatWeight(100.0, WeightUnit.LBS))
    }

    @Test
    fun `machine levels are bare integers with no unit`() {
        assertEquals(
            "7",
            Units.formatLoad(7.0, WeightUnit.KG, MeasurementType.MACHINE_LEVEL),
        )
        // Levels are unit-less, so the weight setting must not convert them.
        assertEquals(
            "7",
            Units.formatLoad(7.0, WeightUnit.LBS, MeasurementType.MACHINE_LEVEL),
        )
    }

    @Test
    fun `weight loads carry their unit and convert`() {
        assertEquals(
            "60 kg",
            Units.formatLoad(60.0, WeightUnit.KG, MeasurementType.WEIGHT_KG),
        )
        assertEquals(
            "132.28 lbs",
            Units.formatLoad(60.0, WeightUnit.LBS, MeasurementType.WEIGHT_KG),
        )
    }

    @Test
    fun `bodyweight assistance keeps its sign`() {
        assertEquals(
            "-20 kg",
            Units.formatLoad(-20.0, WeightUnit.KG, MeasurementType.BODYWEIGHT),
        )
    }

    @Test
    fun `trailing zeroes are dropped`() {
        assertEquals("2.5", Units.formatValue(2.5))
        assertEquals("20", Units.formatValue(20.0))
        assertEquals("20", Units.formatValue(19.999))
    }
}
