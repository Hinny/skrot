package dev.hinny.skrot.domain

import dev.hinny.skrot.data.model.MeasurementType
import dev.hinny.skrot.data.model.SetType

enum class PrType {
    HEAVIEST_WEIGHT,
    BEST_E1RM,
    REP_PR_AT_WEIGHT,
    HIGHEST_LEVEL,
    REP_PR_AT_LEVEL,
    MOST_REPS,
    MOST_ADDED_WEIGHT,
}

/** Minimal set record used for PR detection. */
data class SetRecord(
    val load: Double,
    val reps: Int,
    val setType: SetType = SetType.STANDARD,
    val gymId: Long? = null,
)

object PrDetector {

    /**
     * PRs achieved by [candidate] against [history] (all previously completed sets
     * of the same exercise). Warmup sets are excluded on both sides. A first-ever
     * set is not a PR — there must be a previous best to beat.
     *
     * MACHINE_LEVEL comparisons are restricted to [currentGymId] when provided,
     * because levels aren't comparable across gyms.
     */
    fun detect(
        measurement: MeasurementType,
        candidate: SetRecord,
        history: List<SetRecord>,
        currentGymId: Long? = null,
    ): List<PrType> {
        if (candidate.setType == SetType.WARMUP || candidate.reps < 1) return emptyList()
        val relevant = history.filter { it.setType != SetType.WARMUP && it.reps >= 1 }
        if (relevant.isEmpty()) return emptyList()

        return when (measurement) {
            MeasurementType.WEIGHT_KG -> buildList {
                if (candidate.load > 0 && candidate.load > relevant.maxOf { it.load }) {
                    add(PrType.HEAVIEST_WEIGHT)
                }
                val e1rm = OneRepMax.epley(candidate.load, candidate.reps)
                val bestE1rm = relevant.mapNotNull { OneRepMax.epley(it.load, it.reps) }.maxOrNull()
                if (e1rm != null && bestE1rm != null && e1rm > bestE1rm) {
                    add(PrType.BEST_E1RM)
                }
                val sameWeight = relevant.filter { it.load == candidate.load }
                if (sameWeight.isNotEmpty() && candidate.reps > sameWeight.maxOf { it.reps }) {
                    add(PrType.REP_PR_AT_WEIGHT)
                }
            }

            MeasurementType.MACHINE_LEVEL -> {
                val scoped =
                    if (currentGymId == null) relevant
                    else relevant.filter { it.gymId == currentGymId }
                if (scoped.isEmpty()) return emptyList()
                buildList {
                    if (candidate.load > scoped.maxOf { it.load }) add(PrType.HIGHEST_LEVEL)
                    val sameLevel = scoped.filter { it.load == candidate.load }
                    if (sameLevel.isNotEmpty() && candidate.reps > sameLevel.maxOf { it.reps }) {
                        add(PrType.REP_PR_AT_LEVEL)
                    }
                }
            }

            MeasurementType.BODYWEIGHT -> buildList {
                if (candidate.reps > relevant.maxOf { it.reps }) add(PrType.MOST_REPS)
                if (candidate.load > 0 && candidate.load > relevant.maxOf { it.load }) {
                    add(PrType.MOST_ADDED_WEIGHT)
                }
            }
        }
    }
}
