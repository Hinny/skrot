package dev.hinny.skrot.domain

import dev.hinny.skrot.data.model.LoggedSet
import dev.hinny.skrot.data.model.PlannedSet
import dev.hinny.skrot.data.model.PrefillMode
import dev.hinny.skrot.data.model.SetType

data class Prefill(val load: Double?, val reps: Int?)

object PrefillEngine {

    /**
     * What a new, unlogged set is pre-filled with, per the routine's pre-fill mode.
     *
     * @param planned the planned set (null for ad-hoc sets without a plan)
     * @param lastSessionSets completed sets of the same exercise from the previous session
     * @param typeIndex the set's index among the planned sets of the same set type
     */
    fun prefill(
        mode: PrefillMode,
        planned: PlannedSet?,
        lastSessionSets: List<LoggedSet>,
        typeIndex: Int,
        setType: SetType,
    ): Prefill {
        val lastOfType = lastSessionSets.filter { it.setType == setType }
        val last = lastOfType.getOrNull(typeIndex)
        val targetLoad = planned?.targetLoad
        val targetReps = planned?.targetRepsMin
        return when (mode) {
            PrefillMode.LAST_SESSION ->
                if (last != null) Prefill(last.load, last.reps)
                else Prefill(targetLoad, targetReps)

            PrefillMode.TARGETS -> Prefill(targetLoad, targetReps)

            PrefillMode.HYBRID -> Prefill(last?.load ?: targetLoad, targetReps ?: last?.reps)
        }
    }
}
