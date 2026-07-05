package dev.hinny.skrot.domain

import dev.hinny.skrot.data.model.MeasurementType
import dev.hinny.skrot.data.model.SetType

object VolumeCalculator {
    /** Used when no body weight has ever been logged (configurable in settings). */
    const val DEFAULT_BODYWEIGHT_FALLBACK_KG = 75.0

    /**
     * Volume of one set in kg, or null when the exercise doesn't contribute to
     * kg-based volume (machine levels aren't kilograms).
     *
     * BODYWEIGHT: `(bodyweight * factor + added weight) * reps`; assistance is
     * negative added weight.
     */
    fun setVolumeKg(
        measurement: MeasurementType,
        load: Double,
        reps: Int,
        bodyweightKg: Double,
        bodyweightFactorPercent: Int,
    ): Double? = when (measurement) {
        MeasurementType.WEIGHT_KG -> load * reps
        MeasurementType.BODYWEIGHT ->
            (bodyweightKg * bodyweightFactorPercent / 100.0 + load) * reps

        MeasurementType.MACHINE_LEVEL -> null
    }

    /** Warmup sets count toward volume; this helper exists for stats that exclude them. */
    fun countsForProgression(setType: SetType): Boolean = setType != SetType.WARMUP
}
