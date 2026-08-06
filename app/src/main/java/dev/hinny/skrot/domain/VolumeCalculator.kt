package dev.hinny.skrot.domain

import dev.hinny.skrot.data.model.MeasurementType
import dev.hinny.skrot.data.model.SessionWithContent

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

    /**
     * Total kg volume of every completed set in a session. Machine levels
     * contribute nothing (they aren't kilograms); bodyweight work is priced
     * against [bodyweightKg].
     *
     * The home recap, the finish dialog and the session summary all show "the
     * volume of this workout", so they all count it here rather than each
     * walking the session themselves.
     */
    fun sessionVolumeKg(content: SessionWithContent, bodyweightKg: Double): Double =
        content.exercises.sumOf { se ->
            se.sets.filter { it.completed }.sumOf { set ->
                setVolumeKg(
                    se.exercise.measurementType,
                    set.load,
                    set.reps,
                    bodyweightKg,
                    se.exercise.bodyweightFactor,
                ) ?: 0.0
            }
        }

    /** Completed sets in a session, the count shown alongside [sessionVolumeKg]. */
    fun completedSetCount(content: SessionWithContent): Int =
        content.exercises.sumOf { se -> se.sets.count { it.completed } }
}
