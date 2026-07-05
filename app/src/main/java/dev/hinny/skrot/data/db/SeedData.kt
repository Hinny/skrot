package dev.hinny.skrot.data.db

import dev.hinny.skrot.data.model.Equipment
import dev.hinny.skrot.data.model.Equipment.BARBELL
import dev.hinny.skrot.data.model.Equipment.BODYWEIGHT
import dev.hinny.skrot.data.model.Equipment.CABLE
import dev.hinny.skrot.data.model.Equipment.DUMBBELL
import dev.hinny.skrot.data.model.Equipment.KETTLEBELL
import dev.hinny.skrot.data.model.Equipment.MACHINE
import dev.hinny.skrot.data.model.Exercise
import dev.hinny.skrot.data.model.ExerciseGroup
import dev.hinny.skrot.data.model.MeasurementType
import dev.hinny.skrot.data.model.MeasurementType.MACHINE_LEVEL
import dev.hinny.skrot.data.model.MeasurementType.WEIGHT_KG
import dev.hinny.skrot.data.model.MuscleGroup
import dev.hinny.skrot.data.model.MuscleGroup.ABS
import dev.hinny.skrot.data.model.MuscleGroup.BACK
import dev.hinny.skrot.data.model.MuscleGroup.BICEPS
import dev.hinny.skrot.data.model.MuscleGroup.CALVES
import dev.hinny.skrot.data.model.MuscleGroup.CHEST
import dev.hinny.skrot.data.model.MuscleGroup.FOREARMS
import dev.hinny.skrot.data.model.MuscleGroup.GLUTES
import dev.hinny.skrot.data.model.MuscleGroup.HAMSTRINGS
import dev.hinny.skrot.data.model.MuscleGroup.QUADS
import dev.hinny.skrot.data.model.MuscleGroup.SHOULDERS
import dev.hinny.skrot.data.model.MuscleGroup.TRICEPS

/**
 * Built-in exercise catalog, seeded on first launch. Names in English and Swedish,
 * grouped into interchangeable-exercise groups used for gym swapping.
 */
object SeedData {

    data class SeedGroup(val key: String, val nameEn: String, val nameSv: String)

    data class SeedExercise(
        val nameEn: String,
        val nameSv: String,
        val muscle: MuscleGroup,
        val secondary: List<MuscleGroup> = emptyList(),
        val equipment: Equipment,
        val measurement: MeasurementType = WEIGHT_KG,
        val bwFactor: Int = 100,
        val groupKey: String? = null,
    )

    val groups = listOf(
        SeedGroup("hpress", "Horizontal press", "Horisontell press"),
        SeedGroup("ipress", "Incline press", "Lutande press"),
        SeedGroup("vpress", "Vertical press", "Vertikal press"),
        SeedGroup("vpull", "Vertical pull", "Vertikalt drag"),
        SeedGroup("row", "Horizontal row", "Horisontell rodd"),
        SeedGroup("squat", "Squat pattern", "Knäböjsmönster"),
        SeedGroup("hinge", "Hip hinge", "Höftfällning"),
        SeedGroup("lunge", "Lunge pattern", "Utfallsmönster"),
        SeedGroup("legcurl", "Leg curl", "Lårcurl"),
        SeedGroup("curl", "Biceps curl", "Bicepscurl"),
        SeedGroup("triceps", "Triceps extension", "Tricepspress"),
        SeedGroup("latraise", "Lateral raise", "Sidolyft"),
        SeedGroup("reardelt", "Rear delt", "Bakre axel"),
        SeedGroup("fly", "Chest fly", "Flyes"),
        SeedGroup("calf", "Calf raise", "Vadpress"),
        SeedGroup("abs", "Ab flexion", "Magböjning"),
        SeedGroup("thrust", "Hip thrust", "Höftlyft"),
    )

    val exercises = listOf(
        // Chest
        SeedExercise("Bench Press", "Bänkpress", CHEST, listOf(TRICEPS, SHOULDERS), BARBELL, groupKey = "hpress"),
        SeedExercise("Incline Bench Press", "Lutande bänkpress", CHEST, listOf(SHOULDERS, TRICEPS), BARBELL, groupKey = "ipress"),
        SeedExercise("Dumbbell Bench Press", "Bänkpress med hantlar", CHEST, listOf(TRICEPS), DUMBBELL, groupKey = "hpress"),
        SeedExercise("Incline Dumbbell Press", "Lutande hantelpress", CHEST, listOf(SHOULDERS), DUMBBELL, groupKey = "ipress"),
        SeedExercise("Chest Press Machine", "Bröstpressmaskin", CHEST, listOf(TRICEPS), MACHINE, MACHINE_LEVEL, groupKey = "hpress"),
        SeedExercise("Push-Up", "Armhävning", CHEST, listOf(TRICEPS, SHOULDERS), BODYWEIGHT, MeasurementType.BODYWEIGHT, 65, "hpress"),
        SeedExercise("Dip", "Dips", CHEST, listOf(TRICEPS), BODYWEIGHT, MeasurementType.BODYWEIGHT, 100, "hpress"),
        SeedExercise("Cable Fly", "Kabelflyes", CHEST, equipment = CABLE, groupKey = "fly"),
        SeedExercise("Dumbbell Fly", "Hantelflyes", CHEST, equipment = DUMBBELL, groupKey = "fly"),
        SeedExercise("Pec Deck", "Pec deck-maskin", CHEST, equipment = MACHINE, measurement = MACHINE_LEVEL, groupKey = "fly"),

        // Back
        SeedExercise("Deadlift", "Marklyft", BACK, listOf(HAMSTRINGS, GLUTES), BARBELL, groupKey = "hinge"),
        SeedExercise("Romanian Deadlift", "Rumänsk marklyft", HAMSTRINGS, listOf(GLUTES, BACK), BARBELL, groupKey = "hinge"),
        SeedExercise("Barbell Row", "Skivstångsrodd", BACK, listOf(BICEPS), BARBELL, groupKey = "row"),
        SeedExercise("Dumbbell Row", "Hantelrodd", BACK, listOf(BICEPS), DUMBBELL, groupKey = "row"),
        SeedExercise("Seated Cable Row", "Sittande kabelrodd", BACK, listOf(BICEPS), CABLE, groupKey = "row"),
        SeedExercise("Machine Row", "Roddmaskin", BACK, listOf(BICEPS), MACHINE, MACHINE_LEVEL, groupKey = "row"),
        SeedExercise("T-Bar Row", "T-bar-rodd", BACK, listOf(BICEPS), BARBELL, groupKey = "row"),
        SeedExercise("Pull-Up", "Räckhäv", BACK, listOf(BICEPS), BODYWEIGHT, MeasurementType.BODYWEIGHT, 100, "vpull"),
        SeedExercise("Chin-Up", "Chins", BACK, listOf(BICEPS), BODYWEIGHT, MeasurementType.BODYWEIGHT, 100, "vpull"),
        SeedExercise("Lat Pulldown", "Latsdrag", BACK, listOf(BICEPS), CABLE, groupKey = "vpull"),
        SeedExercise("Assisted Pull-Up Machine", "Assisterad räckhäv", BACK, listOf(BICEPS), MACHINE, MeasurementType.BODYWEIGHT, 100, "vpull"),
        SeedExercise("Back Extension", "Rygglyft", BACK, listOf(GLUTES, HAMSTRINGS), BODYWEIGHT, MeasurementType.BODYWEIGHT, 50, "hinge"),
        SeedExercise("Barbell Shrug", "Axellyft med skivstång", BACK, equipment = BARBELL),
        SeedExercise("Face Pull", "Face pull", SHOULDERS, listOf(BACK), CABLE, groupKey = "reardelt"),

        // Shoulders
        SeedExercise("Overhead Press", "Militärpress", SHOULDERS, listOf(TRICEPS), BARBELL, groupKey = "vpress"),
        SeedExercise("Seated Dumbbell Press", "Sittande axelpress med hantlar", SHOULDERS, listOf(TRICEPS), DUMBBELL, groupKey = "vpress"),
        SeedExercise("Shoulder Press Machine", "Axelpressmaskin", SHOULDERS, listOf(TRICEPS), MACHINE, MACHINE_LEVEL, groupKey = "vpress"),
        SeedExercise("Arnold Press", "Arnoldpress", SHOULDERS, equipment = DUMBBELL, groupKey = "vpress"),
        SeedExercise("Lateral Raise", "Sidolyft", SHOULDERS, equipment = DUMBBELL, groupKey = "latraise"),
        SeedExercise("Cable Lateral Raise", "Sidolyft i kabel", SHOULDERS, equipment = CABLE, groupKey = "latraise"),
        SeedExercise("Front Raise", "Frontlyft", SHOULDERS, equipment = DUMBBELL),
        SeedExercise("Rear Delt Fly", "Omvänt flyes", SHOULDERS, listOf(BACK), DUMBBELL, groupKey = "reardelt"),
        SeedExercise("Reverse Pec Deck", "Omvänd pec deck", SHOULDERS, equipment = MACHINE, measurement = MACHINE_LEVEL, groupKey = "reardelt"),
        SeedExercise("Upright Row", "Stående rodd", SHOULDERS, listOf(BICEPS), BARBELL),

        // Arms
        SeedExercise("Barbell Curl", "Skivstångscurl", BICEPS, equipment = BARBELL, groupKey = "curl"),
        SeedExercise("Dumbbell Curl", "Hantelcurl", BICEPS, equipment = DUMBBELL, groupKey = "curl"),
        SeedExercise("Hammer Curl", "Hammercurl", BICEPS, listOf(FOREARMS), DUMBBELL, groupKey = "curl"),
        SeedExercise("Preacher Curl", "Scottcurl", BICEPS, equipment = BARBELL, groupKey = "curl"),
        SeedExercise("Cable Curl", "Bicepscurl i kabel", BICEPS, equipment = CABLE, groupKey = "curl"),
        SeedExercise("Biceps Curl Machine", "Bicepsmaskin", BICEPS, equipment = MACHINE, measurement = MACHINE_LEVEL, groupKey = "curl"),
        SeedExercise("Triceps Pushdown", "Tricepspress i kabel", TRICEPS, equipment = CABLE, groupKey = "triceps"),
        SeedExercise("Skull Crusher", "Fransk press", TRICEPS, equipment = BARBELL, groupKey = "triceps"),
        SeedExercise("Overhead Triceps Extension", "Tricepspress över huvudet", TRICEPS, equipment = DUMBBELL, groupKey = "triceps"),
        SeedExercise("Close-Grip Bench Press", "Smal bänkpress", TRICEPS, listOf(CHEST), BARBELL, groupKey = "triceps"),
        SeedExercise("Triceps Dip Machine", "Dipsmaskin", TRICEPS, listOf(CHEST), MACHINE, MACHINE_LEVEL, groupKey = "triceps"),
        SeedExercise("Wrist Curl", "Handledscurl", FOREARMS, equipment = DUMBBELL),

        // Legs
        SeedExercise("Squat", "Knäböj", QUADS, listOf(GLUTES, HAMSTRINGS), BARBELL, groupKey = "squat"),
        SeedExercise("Front Squat", "Frontböj", QUADS, listOf(GLUTES), BARBELL, groupKey = "squat"),
        SeedExercise("Goblet Squat", "Goblet squat", QUADS, listOf(GLUTES), DUMBBELL, groupKey = "squat"),
        SeedExercise("Leg Press", "Benpress", QUADS, listOf(GLUTES), MACHINE, groupKey = "squat"),
        SeedExercise("Hack Squat", "Hackböj", QUADS, equipment = MACHINE, groupKey = "squat"),
        SeedExercise("Bulgarian Split Squat", "Bulgarisk utfallsböj", QUADS, listOf(GLUTES), DUMBBELL, groupKey = "lunge"),
        SeedExercise("Lunge", "Utfall", QUADS, listOf(GLUTES), DUMBBELL, groupKey = "lunge"),
        SeedExercise("Leg Extension", "Benspark", QUADS, equipment = MACHINE, measurement = MACHINE_LEVEL),
        SeedExercise("Lying Leg Curl", "Liggande lårcurl", HAMSTRINGS, equipment = MACHINE, measurement = MACHINE_LEVEL, groupKey = "legcurl"),
        SeedExercise("Seated Leg Curl", "Sittande lårcurl", HAMSTRINGS, equipment = MACHINE, measurement = MACHINE_LEVEL, groupKey = "legcurl"),
        SeedExercise("Hip Thrust", "Höftlyft", GLUTES, listOf(HAMSTRINGS), BARBELL, groupKey = "thrust"),
        SeedExercise("Glute Kickback", "Kickback i kabel", GLUTES, equipment = CABLE, groupKey = "thrust"),
        SeedExercise("Standing Calf Raise", "Stående vadpress", CALVES, equipment = MACHINE, measurement = MACHINE_LEVEL, groupKey = "calf"),
        SeedExercise("Seated Calf Raise", "Sittande vadpress", CALVES, equipment = MACHINE, groupKey = "calf"),
        SeedExercise("Kettlebell Swing", "Kettlebellsving", GLUTES, listOf(HAMSTRINGS, BACK), KETTLEBELL, groupKey = "hinge"),

        // Abs
        SeedExercise("Crunch", "Crunch", ABS, equipment = BODYWEIGHT, measurement = MeasurementType.BODYWEIGHT, bwFactor = 30, groupKey = "abs"),
        SeedExercise("Sit-Up", "Situps", ABS, equipment = BODYWEIGHT, measurement = MeasurementType.BODYWEIGHT, bwFactor = 40, groupKey = "abs"),
        SeedExercise("Hanging Leg Raise", "Hängande benlyft", ABS, equipment = BODYWEIGHT, measurement = MeasurementType.BODYWEIGHT, bwFactor = 50, groupKey = "abs"),
        SeedExercise("Cable Crunch", "Kabelcrunch", ABS, equipment = CABLE, groupKey = "abs"),
        SeedExercise("Ab Machine", "Magmaskin", ABS, equipment = MACHINE, measurement = MACHINE_LEVEL, groupKey = "abs"),
        SeedExercise("Russian Twist", "Rysk vridning", ABS, equipment = BODYWEIGHT, measurement = MeasurementType.BODYWEIGHT, bwFactor = 30),
    )

    /** Seeds the exercise catalog on first launch (no-op if exercises already exist). */
    suspend fun seedIfEmpty(db: SkrotDatabase) {
        val dao = db.exerciseDao()
        if (dao.count() > 0) return
        val groupIds = mutableMapOf<String, Long>()
        for (g in groups) {
            groupIds[g.key] = dao.insertGroup(ExerciseGroup(nameEn = g.nameEn, nameSv = g.nameSv))
        }
        dao.insertAll(
            exercises.map { e ->
                Exercise(
                    nameEn = e.nameEn,
                    nameSv = e.nameSv,
                    muscleGroup = e.muscle,
                    secondaryMuscles = e.secondary,
                    equipment = e.equipment,
                    measurementType = e.measurement,
                    isCustom = false,
                    bodyweightFactor = e.bwFactor,
                    groupId = e.groupKey?.let { groupIds[it] },
                )
            }
        )
    }
}
