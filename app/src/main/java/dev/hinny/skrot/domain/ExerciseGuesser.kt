package dev.hinny.skrot.domain

import dev.hinny.skrot.data.model.Equipment
import dev.hinny.skrot.data.model.MuscleGroup

/**
 * Best-effort classification of an exercise from its name alone, used when a
 * custom exercise is created from a source that has no muscle group or
 * equipment data (e.g. JEFIT import). Order matters: more specific phrases
 * are checked before the generic keywords they contain.
 */
object ExerciseGuesser {

    private val musclePatterns: List<Pair<Regex, MuscleGroup>> = listOf(
        Regex("romanian deadlift|stiff.?leg deadlift|\\brdl\\b") to MuscleGroup.HAMSTRINGS,
        Regex("leg extension") to MuscleGroup.QUADS,
        Regex("squat|leg press|lunge|step.?up") to MuscleGroup.QUADS,
        Regex("calf") to MuscleGroup.CALVES,
        Regex("hip thrust|glute|kickback") to MuscleGroup.GLUTES,
        Regex("hamstring|leg curl|nordic curl") to MuscleGroup.HAMSTRINGS,
        Regex("deadlift|\\brow\\b|pulldown|pull.?up|chin.?up|lat pull|shrug") to MuscleGroup.BACK,
        Regex("curl") to MuscleGroup.BICEPS,
        Regex("wrist") to MuscleGroup.FOREARMS,
        Regex("tricep|pushdown|skull.?crusher|dip") to MuscleGroup.TRICEPS,
        Regex("shoulder|overhead press|military press|lateral raise|front raise|arnold press") to MuscleGroup.SHOULDERS,
        Regex("bench|chest|\\bfly\\b|flye|push.?up|pec") to MuscleGroup.CHEST,
        Regex("crunch|sit.?up|plank|ab wheel|leg raise|\\babs\\b|woodchopper") to MuscleGroup.ABS,
        Regex("press") to MuscleGroup.CHEST,
    )

    fun guessMuscleGroup(name: String): MuscleGroup {
        val lower = name.lowercase()
        return musclePatterns.firstOrNull { (pattern, _) -> pattern.containsMatchIn(lower) }
            ?.second
            ?: MuscleGroup.FULL_BODY
    }

    private val equipmentPatterns: List<Pair<Regex, Equipment>> = listOf(
        Regex("ez.?bar") to Equipment.EZ_BAR,
        Regex("barbell") to Equipment.BARBELL,
        Regex("dumbbell|\\bdb\\b") to Equipment.DUMBBELL,
        Regex("kettlebell") to Equipment.KETTLEBELL,
        Regex("smith machine|smith") to Equipment.SMITH_MACHINE,
        Regex("cable") to Equipment.CABLE,
        Regex("machine") to Equipment.MACHINE,
        Regex("band") to Equipment.BAND,
        Regex("bench") to Equipment.BENCH,
        Regex("rack") to Equipment.RACK,
        Regex("pull.?up|chin.?up") to Equipment.PULLUP_BAR,
        Regex("dip") to Equipment.DIP_STATION,
    )

    private val bodyweightPattern =
        Regex("push.?up|sit.?up|plank|burpee|crunch|mountain climber|dead bug")

    fun guessEquipment(name: String): List<Equipment> {
        val lower = name.lowercase()
        val matched = equipmentPatterns
            .filter { (pattern, _) -> pattern.containsMatchIn(lower) }
            .map { it.second }
            .distinct()
        if (matched.isNotEmpty()) return matched
        return if (bodyweightPattern.containsMatchIn(lower)) listOf(Equipment.NONE) else emptyList()
    }
}
