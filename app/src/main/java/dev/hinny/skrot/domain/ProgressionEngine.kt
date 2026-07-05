package dev.hinny.skrot.domain

import dev.hinny.skrot.data.model.LoggedSet
import dev.hinny.skrot.data.model.MeasurementType
import dev.hinny.skrot.data.model.PlannedSet
import dev.hinny.skrot.data.model.SetType

sealed class ProgressionSuggestion {
    /** Increase the load next session (kg or machine level). */
    data class IncreaseLoad(val fromLoad: Double, val toLoad: Double) : ProgressionSuggestion()

    /** Plain bodyweight work without added weight: suggest one more rep instead. */
    data class AddRep(val fromReps: Int, val toReps: Int) : ProgressionSuggestion()
}

object ProgressionEngine {
    const val DEFAULT_INCREMENT_KG = 2.5
    const val DEFAULT_INCREMENT_LEVEL = 1.0

    /**
     * Suggests progression when the previous session hit the target reps (top of
     * the range) on ALL standard sets at the same load. Standard sets only —
     * warmups, drop sets, and failure sets never drive progression.
     */
    fun suggest(
        measurement: MeasurementType,
        lastSessionSets: List<LoggedSet>,
        plannedSets: List<PlannedSet>,
        incrementKg: Double = DEFAULT_INCREMENT_KG,
        incrementLevel: Double = DEFAULT_INCREMENT_LEVEL,
        exerciseIncrementOverride: Double? = null,
    ): ProgressionSuggestion? {
        val plannedStandard = plannedSets
            .sortedBy { it.position }
            .filter { it.setType == SetType.STANDARD }
        val loggedStandard = lastSessionSets
            .sortedBy { it.position }
            .filter { it.setType == SetType.STANDARD && it.completed }

        if (plannedStandard.isEmpty() || loggedStandard.size < plannedStandard.size) return null

        val load = loggedStandard.first().load
        if (loggedStandard.any { it.load != load }) return null

        val allHitTarget = plannedStandard.indices.all { i ->
            val target = plannedStandard[i].targetRepsMax ?: plannedStandard[i].targetRepsMin
            target != null && loggedStandard[i].reps >= target
        }
        if (!allHitTarget) return null

        return when (measurement) {
            MeasurementType.WEIGHT_KG -> {
                val inc = exerciseIncrementOverride ?: incrementKg
                ProgressionSuggestion.IncreaseLoad(load, load + inc)
            }

            MeasurementType.MACHINE_LEVEL -> {
                val inc = exerciseIncrementOverride ?: incrementLevel
                ProgressionSuggestion.IncreaseLoad(load, load + inc)
            }

            MeasurementType.BODYWEIGHT ->
                if (loggedStandard.all { it.load == 0.0 }) {
                    val topTarget = plannedStandard.mapNotNull { it.targetRepsMax ?: it.targetRepsMin }.max()
                    ProgressionSuggestion.AddRep(topTarget, topTarget + 1)
                } else {
                    val inc = exerciseIncrementOverride ?: incrementKg
                    ProgressionSuggestion.IncreaseLoad(load, load + inc)
                }
        }
    }
}
