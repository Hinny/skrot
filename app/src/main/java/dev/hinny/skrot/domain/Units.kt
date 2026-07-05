package dev.hinny.skrot.domain

import dev.hinny.skrot.data.model.MeasurementType
import dev.hinny.skrot.data.model.WeightUnit
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * All data is stored in kg; conversion to/from lbs happens at display/input time only.
 */
object Units {
    const val LBS_PER_KG = 2.2046226218

    fun kgToLbs(kg: Double): Double = kg * LBS_PER_KG
    fun lbsToKg(lbs: Double): Double = lbs / LBS_PER_KG

    /** Stored kg -> display value in the chosen unit (levels are unit-less). */
    fun toDisplay(kg: Double, unit: WeightUnit, measurement: MeasurementType): Double =
        if (measurement == MeasurementType.MACHINE_LEVEL || unit == WeightUnit.KG) kg
        else kgToLbs(kg)

    /** Input in the chosen display unit -> stored kg. */
    fun fromDisplay(value: Double, unit: WeightUnit, measurement: MeasurementType): Double =
        if (measurement == MeasurementType.MACHINE_LEVEL || unit == WeightUnit.KG) value
        else lbsToKg(value)

    /** Stepper increment in the display unit: +/- 2.5 kg, +/- 5 lbs, +/- 1 level. */
    fun stepSize(unit: WeightUnit, measurement: MeasurementType): Double = when {
        measurement == MeasurementType.MACHINE_LEVEL -> 1.0
        unit == WeightUnit.LBS -> 5.0
        else -> 2.5
    }

    /** Compact number formatting: drop trailing ".0", keep at most two decimals. */
    fun formatValue(value: Double): String {
        val rounded = (value * 100).roundToInt() / 100.0
        return if (abs(rounded - rounded.toLong()) < 0.005) rounded.toLong().toString()
        else String.format(Locale.US, "%.2f", rounded).trimEnd('0').trimEnd('.')
    }
}
