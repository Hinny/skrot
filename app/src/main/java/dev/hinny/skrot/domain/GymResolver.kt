package dev.hinny.skrot.domain

import dev.hinny.skrot.data.model.Exercise

/** Outcome of checking one planned exercise against a gym's available equipment. */
sealed class GymResolution {
    /** Available at the gym; used as-is. */
    data object Available : GymResolution()

    /** A saved per-gym override or the single available group equivalent. */
    data class AutoSwapped(val to: Exercise) : GymResolution()

    /** Several group equivalents available; the user picks. */
    data class Choice(val options: List<Exercise>) : GymResolution()

    /** Nothing equivalent at this gym: keep with warning, skip, or pick manually. */
    data object NoEquivalent : GymResolution()
}

object GymResolver {

    /**
     * Resolves a planned exercise at a gym. [groupMembers] are the exercises in
     * the same interchangeable-exercise group (including [exercise] itself).
     * A saved [override] wins when it's still available at the gym.
     */
    fun resolve(
        exercise: Exercise,
        availableIds: Set<Long>,
        override: Exercise?,
        groupMembers: List<Exercise>,
    ): GymResolution {
        if (override != null && override.id in availableIds) {
            return if (override.id == exercise.id) GymResolution.Available
            else GymResolution.AutoSwapped(override)
        }
        if (exercise.id in availableIds) return GymResolution.Available

        val equivalents = groupMembers.filter { it.id != exercise.id && it.id in availableIds }
        return when (equivalents.size) {
            0 -> GymResolution.NoEquivalent
            1 -> GymResolution.AutoSwapped(equivalents.single())
            else -> GymResolution.Choice(equivalents)
        }
    }
}
