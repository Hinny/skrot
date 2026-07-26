package dev.hinny.skrot.domain

import dev.hinny.skrot.data.model.Equipment
import dev.hinny.skrot.data.model.Exercise
import dev.hinny.skrot.data.model.MuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseSearchTest {

    private fun ex(
        nameEn: String,
        nameSv: String = nameEn,
        muscle: MuscleGroup = MuscleGroup.CHEST,
        secondary: List<MuscleGroup> = emptyList(),
        equipment: List<Equipment> = emptyList(),
        isCustom: Boolean = false,
    ) = Exercise(
        nameEn = nameEn,
        nameSv = nameSv,
        muscleGroup = muscle,
        secondaryMuscles = secondary,
        equipment = equipment,
        isCustom = isCustom,
    )

    private val muscleNames = mapOf(
        MuscleGroup.CHEST to listOf("Bröst", "chest"),
        MuscleGroup.BACK to listOf("Rygg", "back"),
        MuscleGroup.TRICEPS to listOf("Triceps", "triceps"),
    )
    private val equipmentNames = mapOf(
        Equipment.BARBELL to listOf("Skivstång", "barbell"),
        Equipment.CABLE to listOf("Kabel", "cable"),
    )

    private val bench = ex("Bench Press", "Bänkpress", MuscleGroup.CHEST, listOf(MuscleGroup.TRICEPS), listOf(Equipment.BARBELL))
    private val row = ex("Barbell Row", "Skivstångsrodd", MuscleGroup.BACK, equipment = listOf(Equipment.BARBELL))
    private val cableFly = ex("Cable Fly", "Kabelflyes", MuscleGroup.CHEST, equipment = listOf(Equipment.CABLE))
    private val all = listOf(bench, row, cableFly)

    @Test
    fun `matches both languages at once`() {
        assertEquals(listOf(bench), ExerciseSearch.search(all, "bänk", muscleNames = muscleNames, equipmentNames = equipmentNames))
        assertEquals(listOf(bench), ExerciseSearch.search(all, "bench", muscleNames = muscleNames, equipmentNames = equipmentNames))
    }

    @Test
    fun `name matches rank above muscle and equipment matches`() {
        // "rygg" matches Row's muscle (Rygg) but no names -> row via muscle
        assertEquals(listOf(row), ExerciseSearch.search(all, "rygg", muscleNames = muscleNames, equipmentNames = equipmentNames))
        // "skivstång" matches Row's name (rank 0) and Bench's equipment (rank 2)
        assertEquals(
            listOf(row, bench),
            ExerciseSearch.search(all, "skivstång", muscleNames = muscleNames, equipmentNames = equipmentNames),
        )
    }

    @Test
    fun `secondary muscles match search and filters`() {
        assertEquals(
            listOf(bench),
            ExerciseSearch.search(all, "triceps", muscleNames = muscleNames, equipmentNames = equipmentNames),
        )
        assertEquals(
            listOf(bench),
            ExerciseSearch.search(all, "", muscleFilters = setOf(MuscleGroup.TRICEPS)),
        )
    }

    @Test
    fun `multi-select filters are OR within and AND between categories`() {
        assertEquals(
            listOf(bench, row, cableFly),
            ExerciseSearch.search(all, "", muscleFilters = setOf(MuscleGroup.CHEST, MuscleGroup.BACK)),
        )
        assertEquals(
            listOf(bench),
            ExerciseSearch.search(
                all, "",
                muscleFilters = setOf(MuscleGroup.CHEST),
                equipmentFilters = setOf(Equipment.BARBELL),
            ),
        )
    }

    @Test
    fun `blank query with no filters returns everything in original order`() {
        assertEquals(all, ExerciseSearch.search(all, "  "))
    }

    @Test
    fun `custom filter narrows to built-in or custom exercises only`() {
        val custom = ex("My Curl", isCustom = true)
        val withCustom = all + custom
        assertEquals(withCustom, ExerciseSearch.search(withCustom, "", customFilter = null))
        assertEquals(listOf(custom), ExerciseSearch.search(withCustom, "", customFilter = true))
        assertEquals(all, ExerciseSearch.search(withCustom, "", customFilter = false))
    }
}
