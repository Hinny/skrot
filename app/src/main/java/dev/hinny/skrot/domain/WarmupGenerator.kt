package dev.hinny.skrot.domain

import kotlin.math.roundToLong

/**
 * Builds a ramp of warm-up sets up to the first working set. Warmups are
 * excluded from PRs, 1RM estimates and progression everywhere else, so
 * generating them costs nothing but the taps it saves.
 */
object WarmupGenerator {

    data class WarmupSet(val load: Double, val reps: Int)

    /** Fractions of the working load, lightest first, by how many sets were asked for. */
    private val RAMPS: Map<Int, List<Double>> = mapOf(
        1 to listOf(0.5),
        2 to listOf(0.5, 0.75),
        3 to listOf(0.4, 0.6, 0.8),
        4 to listOf(0.4, 0.55, 0.7, 0.85),
        5 to listOf(0.35, 0.5, 0.625, 0.75, 0.875),
        6 to listOf(0.3, 0.45, 0.55, 0.65, 0.75, 0.85),
    )

    /** Reps per rung, lightest first: heavier warmups get fewer reps. */
    private val REPS = listOf(10, 8, 5, 3, 2, 1)

    /**
     * @param workingLoad load of the first working set, in whatever unit is stored
     * @param count how many warm-up sets to build (clamped to 1..6)
     * @param rounding smallest load step worth prescribing — the progression
     *   increment, so a warmup is never a weight the gym cannot make
     *
     * Rungs that round onto the working load, or onto each other, are dropped:
     * a "warm-up" at the working weight is just an extra working set.
     */
    fun generate(
        workingLoad: Double,
        count: Int,
        rounding: Double,
    ): List<WarmupSet> {
        if (workingLoad <= 0.0) return emptyList()
        val ramp = RAMPS[count.coerceIn(1, 6)] ?: return emptyList()
        return ramp
            .mapIndexed { i, fraction ->
                WarmupSet(
                    load = round(workingLoad * fraction, rounding),
                    reps = REPS.getOrElse(i) { 1 },
                )
            }
            .filter { it.load > 0.0 && it.load < workingLoad }
            .distinctBy { it.load }
    }

    private fun round(value: Double, step: Double): Double =
        if (step <= 0.0) value else (value / step).roundToLong() * step
}
