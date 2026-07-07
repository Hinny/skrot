package dev.hinny.skrot.domain

import dev.hinny.skrot.data.model.Equipment
import dev.hinny.skrot.data.model.Exercise
import dev.hinny.skrot.data.model.MuscleGroup

/**
 * Shared exercise search: the query matches exercise names (both languages at
 * once), muscle groups and equipment, with name matches ranked first. Muscle
 * and equipment filters are multi-select.
 */
object ExerciseSearch {

    /**
     * @param muscleNames searchable names per muscle group (localized label +
     *   English enum name)
     * @param equipmentNames searchable names per equipment
     * @return match rank (0 = name, 1 = muscle, 2 = equipment) or null for no match
     */
    fun rank(
        exercise: Exercise,
        query: String,
        muscleNames: Map<MuscleGroup, List<String>>,
        equipmentNames: Map<Equipment, List<String>>,
    ): Int? {
        val q = query.trim()
        if (q.isEmpty()) return 0
        if (exercise.nameEn.contains(q, ignoreCase = true) ||
            exercise.nameSv.contains(q, ignoreCase = true)
        ) {
            return 0
        }
        val muscles = listOf(exercise.muscleGroup) + exercise.secondaryMuscles
        if (muscles.any { m -> muscleNames[m].orEmpty().any { it.contains(q, ignoreCase = true) } }) {
            return 1
        }
        if (exercise.equipment.any { e ->
                equipmentNames[e].orEmpty().any { it.contains(q, ignoreCase = true) }
            }
        ) {
            return 2
        }
        return null
    }

    fun search(
        exercises: List<Exercise>,
        query: String,
        muscleFilters: Set<MuscleGroup> = emptySet(),
        equipmentFilters: Set<Equipment> = emptySet(),
        muscleNames: Map<MuscleGroup, List<String>> = defaultMuscleNames,
        equipmentNames: Map<Equipment, List<String>> = defaultEquipmentNames,
    ): List<Exercise> =
        exercises
            .mapNotNull { e ->
                val muscles = listOf(e.muscleGroup) + e.secondaryMuscles
                if (muscleFilters.isNotEmpty() && muscles.none { it in muscleFilters }) {
                    return@mapNotNull null
                }
                if (equipmentFilters.isNotEmpty() && e.equipment.none { it in equipmentFilters }) {
                    return@mapNotNull null
                }
                rank(e, query, muscleNames, equipmentNames)?.let { r -> e to r }
            }
            .sortedBy { it.second } // stable: original order kept within each rank
            .map { it.first }

    /** English fallback names derived from the enum constants ("PULLUP_BAR" -> "pullup bar"). */
    val defaultMuscleNames: Map<MuscleGroup, List<String>> =
        MuscleGroup.entries.associateWith { listOf(it.name.replace('_', ' ')) }

    val defaultEquipmentNames: Map<Equipment, List<String>> =
        Equipment.entries.associateWith { listOf(it.name.replace('_', ' ')) }
}
