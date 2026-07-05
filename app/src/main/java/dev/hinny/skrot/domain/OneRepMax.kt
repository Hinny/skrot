package dev.hinny.skrot.domain

import kotlin.math.min

object OneRepMax {
    /** Reps above this are capped to avoid nonsense estimates from high-rep sets. */
    const val REP_CAP = 12

    /**
     * Estimated 1RM using the Epley formula: `1RM = w * (1 + reps / 30)`.
     * Returns null when no sensible estimate exists.
     */
    fun epley(weightKg: Double, reps: Int): Double? {
        if (weightKg <= 0.0 || reps < 1) return null
        val cappedReps = min(reps, REP_CAP)
        return weightKg * (1.0 + cappedReps / 30.0)
    }
}
