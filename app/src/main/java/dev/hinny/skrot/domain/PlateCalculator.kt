package dev.hinny.skrot.domain

import dev.hinny.skrot.data.model.WeightUnit

/**
 * What to hang on each end of the bar for a given total load.
 *
 * Everything here works in whatever unit it is handed — the caller converts to
 * the display unit first, because plates are physical objects and a lifter
 * reading "20" wants the disc stamped 20, not 44.09.
 */
object PlateCalculator {

    /** Discs found on a metric rack, heaviest first. */
    val PLATES_KG = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)

    /** Discs found on an imperial rack, heaviest first. */
    val PLATES_LBS = listOf(45.0, 35.0, 25.0, 10.0, 5.0, 2.5)

    fun platesFor(unit: WeightUnit): List<Double> =
        if (unit == WeightUnit.KG) PLATES_KG else PLATES_LBS

    data class Loading(
        /** One entry per disc that goes on each side, heaviest first. */
        val perSide: List<Double>,
        /** What the available plates could not make up, across both sides. */
        val remainder: Double,
    ) {
        val isExact: Boolean get() = remainder < TOLERANCE

        /** The same discs as (weight, count) pairs, for a readable "20 x2" label. */
        val perSideGrouped: List<Pair<Double, Int>>
            get() = perSide.groupBy { it }
                .map { (plate, discs) -> plate to discs.size }
                .sortedByDescending { it.first }
    }

    /**
     * Greedy from the heaviest disc down, which is what anyone actually does at
     * the rack. Null when the total does not even cover the bar — there is
     * nothing sensible to show for a load lighter than the bar itself.
     */
    fun forTotal(
        total: Double,
        bar: Double,
        plates: List<Double> = PLATES_KG,
    ): Loading? {
        if (bar <= 0.0 || total < bar - TOLERANCE) return null
        var perSideRemaining = (total - bar) / 2.0
        val used = mutableListOf<Double>()
        for (plate in plates.sortedDescending()) {
            if (plate <= 0.0) continue
            while (perSideRemaining >= plate - TOLERANCE) {
                used += plate
                perSideRemaining -= plate
            }
        }
        return Loading(used, perSideRemaining * 2.0)
    }

    /** Rounding slack, so 60.0000001 still loads as 60. */
    private const val TOLERANCE = 0.001
}
