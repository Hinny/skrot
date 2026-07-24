package dev.hinny.skrot.data.db

import dev.hinny.skrot.data.model.Equipment
import dev.hinny.skrot.data.model.Equipment.BARBELL
import dev.hinny.skrot.data.model.Equipment.BENCH
import dev.hinny.skrot.data.model.Equipment.CABLE
import dev.hinny.skrot.data.model.Equipment.DIP_STATION
import dev.hinny.skrot.data.model.Equipment.DUMBBELL
import dev.hinny.skrot.data.model.Equipment.KETTLEBELL
import dev.hinny.skrot.data.model.Equipment.MACHINE
import dev.hinny.skrot.data.model.Equipment.BAND
import dev.hinny.skrot.data.model.Equipment.EZ_BAR
import dev.hinny.skrot.data.model.Equipment.MULTI_MACHINE
import dev.hinny.skrot.data.model.Equipment.NONE
import dev.hinny.skrot.data.model.Equipment.OTHER
import dev.hinny.skrot.data.model.Equipment.PULLUP_BAR
import dev.hinny.skrot.data.model.Equipment.RACK
import dev.hinny.skrot.data.model.Equipment.SMITH_MACHINE
import dev.hinny.skrot.data.model.Equipment.WEIGHT_PLATE
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
import dev.hinny.skrot.data.model.MuscleGroup.FULL_BODY
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
        val extraEquipment: List<Equipment> = emptyList(),
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
        SeedExercise("Bench Press", "Bänkpress", CHEST, listOf(TRICEPS, SHOULDERS), BARBELL, extraEquipment = listOf(BENCH, RACK), groupKey = "hpress"),
        SeedExercise("Incline Bench Press", "Lutande bänkpress", CHEST, listOf(SHOULDERS, TRICEPS), BARBELL, extraEquipment = listOf(BENCH, RACK), groupKey = "ipress"),
        SeedExercise("Dumbbell Bench Press", "Bänkpress med hantlar", CHEST, listOf(TRICEPS), DUMBBELL, extraEquipment = listOf(BENCH), groupKey = "hpress"),
        SeedExercise("Incline Dumbbell Press", "Lutande hantelpress", CHEST, listOf(SHOULDERS), DUMBBELL, extraEquipment = listOf(BENCH), groupKey = "ipress"),
        SeedExercise("Chest Press Machine", "Bröstpressmaskin", CHEST, listOf(TRICEPS), MACHINE, measurement = MACHINE_LEVEL, groupKey = "hpress"),
        SeedExercise("Push-Up", "Armhävning", CHEST, listOf(TRICEPS, SHOULDERS), NONE, measurement = MeasurementType.BODYWEIGHT, bwFactor = 65, groupKey = "hpress"),
        SeedExercise("Dip", "Dips", CHEST, listOf(TRICEPS), DIP_STATION, measurement = MeasurementType.BODYWEIGHT, bwFactor = 100, groupKey = "hpress"),
        SeedExercise("Cable Fly", "Kabelflyes", CHEST, listOf(SHOULDERS), CABLE, groupKey = "fly"),
        SeedExercise("Dumbbell Fly", "Hantelflyes", CHEST, listOf(SHOULDERS), DUMBBELL, groupKey = "fly"),
        SeedExercise("Pec Deck", "Pec deck-maskin", CHEST, listOf(SHOULDERS), MACHINE, measurement = MACHINE_LEVEL, groupKey = "fly"),

        // Back
        SeedExercise("Deadlift", "Marklyft", BACK, listOf(HAMSTRINGS, GLUTES), BARBELL, groupKey = "hinge"),
        SeedExercise("Romanian Deadlift", "Rumänsk marklyft", HAMSTRINGS, listOf(GLUTES, BACK), BARBELL, groupKey = "hinge"),
        SeedExercise("Barbell Row", "Skivstångsrodd", BACK, listOf(BICEPS), BARBELL, groupKey = "row"),
        SeedExercise("Dumbbell Row", "Hantelrodd", BACK, listOf(BICEPS), DUMBBELL, groupKey = "row"),
        SeedExercise("Seated Cable Row", "Sittande kabelrodd", BACK, listOf(BICEPS), CABLE, groupKey = "row"),
        SeedExercise("Machine Row", "Roddmaskin", BACK, listOf(BICEPS), MACHINE, measurement = MACHINE_LEVEL, groupKey = "row"),
        SeedExercise("T-Bar Row", "T-bar-rodd", BACK, listOf(BICEPS), BARBELL, groupKey = "row"),
        SeedExercise("Pull-Up", "Räckhäv", BACK, listOf(BICEPS), PULLUP_BAR, measurement = MeasurementType.BODYWEIGHT, bwFactor = 100, groupKey = "vpull"),
        SeedExercise("Chin-Up", "Chins", BACK, listOf(BICEPS), PULLUP_BAR, measurement = MeasurementType.BODYWEIGHT, bwFactor = 100, groupKey = "vpull"),
        SeedExercise("Lat Pulldown", "Latsdrag", BACK, listOf(BICEPS), CABLE, groupKey = "vpull"),
        SeedExercise("Assisted Pull-Up Machine", "Assisterad räckhäv", BACK, listOf(BICEPS), MACHINE, measurement = MeasurementType.BODYWEIGHT, bwFactor = 100, groupKey = "vpull"),
        SeedExercise("Back Extension", "Rygglyft", BACK, listOf(GLUTES, HAMSTRINGS), BENCH, measurement = MeasurementType.BODYWEIGHT, bwFactor = 50, groupKey = "hinge"),
        SeedExercise("Barbell Shrug", "Axellyft med skivstång", BACK, listOf(FOREARMS), BARBELL),
        SeedExercise("Face Pull", "Face pull", SHOULDERS, listOf(BACK), CABLE, groupKey = "reardelt"),

        // Shoulders
        SeedExercise("Overhead Press", "Militärpress", SHOULDERS, listOf(TRICEPS), BARBELL, groupKey = "vpress"),
        SeedExercise("Seated Dumbbell Press", "Sittande axelpress med hantlar", SHOULDERS, listOf(TRICEPS), DUMBBELL, extraEquipment = listOf(BENCH), groupKey = "vpress"),
        SeedExercise("Shoulder Press Machine", "Axelpressmaskin", SHOULDERS, listOf(TRICEPS), MACHINE, measurement = MACHINE_LEVEL, groupKey = "vpress"),
        SeedExercise("Arnold Press", "Arnoldpress", SHOULDERS, listOf(TRICEPS), DUMBBELL, groupKey = "vpress"),
        SeedExercise("Lateral Raise", "Sidolyft", SHOULDERS, equipment = DUMBBELL, groupKey = "latraise"),
        SeedExercise("Cable Lateral Raise", "Sidolyft i kabel", SHOULDERS, equipment = CABLE, groupKey = "latraise"),
        SeedExercise("Front Raise", "Frontlyft", SHOULDERS, equipment = DUMBBELL),
        SeedExercise("Rear Delt Fly", "Omvänt flyes", SHOULDERS, listOf(BACK), DUMBBELL, groupKey = "reardelt"),
        SeedExercise("Reverse Pec Deck", "Omvänd pec deck", SHOULDERS, listOf(BACK), MACHINE, measurement = MACHINE_LEVEL, groupKey = "reardelt"),
        SeedExercise("Upright Row", "Stående rodd", SHOULDERS, listOf(BICEPS), BARBELL),

        // Arms
        SeedExercise("Barbell Curl", "Skivstångscurl", BICEPS, listOf(FOREARMS), BARBELL, groupKey = "curl"),
        SeedExercise("Dumbbell Curl", "Hantelcurl", BICEPS, listOf(FOREARMS), DUMBBELL, groupKey = "curl"),
        SeedExercise("Hammer Curl", "Hammercurl", BICEPS, listOf(FOREARMS), DUMBBELL, groupKey = "curl"),
        SeedExercise("Preacher Curl", "Scottcurl", BICEPS, listOf(FOREARMS), BARBELL, groupKey = "curl"),
        SeedExercise("Cable Curl", "Bicepscurl i kabel", BICEPS, listOf(FOREARMS), CABLE, groupKey = "curl"),
        SeedExercise("Biceps Curl Machine", "Bicepsmaskin", BICEPS, listOf(FOREARMS), MACHINE, measurement = MACHINE_LEVEL, groupKey = "curl"),
        SeedExercise("Triceps Pushdown", "Tricepspress i kabel", TRICEPS, equipment = CABLE, groupKey = "triceps"),
        SeedExercise("Skull Crusher", "Fransk press", TRICEPS, equipment = BARBELL, extraEquipment = listOf(BENCH), groupKey = "triceps"),
        SeedExercise("Overhead Triceps Extension", "Tricepspress över huvudet", TRICEPS, equipment = DUMBBELL, groupKey = "triceps"),
        SeedExercise("Close-Grip Bench Press", "Smal bänkpress", TRICEPS, listOf(CHEST), BARBELL, extraEquipment = listOf(BENCH, RACK), groupKey = "triceps"),
        SeedExercise("Triceps Dip Machine", "Dipsmaskin", TRICEPS, listOf(CHEST), MACHINE, measurement = MACHINE_LEVEL, groupKey = "triceps"),
        SeedExercise("Wrist Curl", "Handledscurl", FOREARMS, equipment = DUMBBELL),

        // Legs
        SeedExercise("Squat", "Knäböj", QUADS, listOf(GLUTES, HAMSTRINGS), BARBELL, extraEquipment = listOf(RACK), groupKey = "squat"),
        SeedExercise("Front Squat", "Frontböj", QUADS, listOf(GLUTES), BARBELL, extraEquipment = listOf(RACK), groupKey = "squat"),
        SeedExercise("Goblet Squat", "Goblet squat", QUADS, listOf(GLUTES), DUMBBELL, groupKey = "squat"),
        SeedExercise("Leg Press", "Benpress", QUADS, listOf(GLUTES), MACHINE, groupKey = "squat"),
        SeedExercise("Hack Squat", "Hackböj", QUADS, listOf(GLUTES), MACHINE, groupKey = "squat"),
        SeedExercise("Bulgarian Split Squat", "Bulgarisk utfallsböj", QUADS, listOf(GLUTES), DUMBBELL, extraEquipment = listOf(BENCH), groupKey = "lunge"),
        SeedExercise("Lunge", "Utfall", QUADS, listOf(GLUTES), DUMBBELL, groupKey = "lunge"),
        SeedExercise("Leg Extension", "Benspark", QUADS, equipment = MACHINE, measurement = MACHINE_LEVEL),
        SeedExercise("Lying Leg Curl", "Liggande lårcurl", HAMSTRINGS, equipment = MACHINE, measurement = MACHINE_LEVEL, groupKey = "legcurl"),
        SeedExercise("Seated Leg Curl", "Sittande lårcurl", HAMSTRINGS, equipment = MACHINE, measurement = MACHINE_LEVEL, groupKey = "legcurl"),
        SeedExercise("Hip Thrust", "Höftlyft", GLUTES, listOf(HAMSTRINGS), BARBELL, extraEquipment = listOf(BENCH), groupKey = "thrust"),
        SeedExercise("Glute Kickback", "Kickback i kabel", GLUTES, listOf(HAMSTRINGS), CABLE, groupKey = "thrust"),
        SeedExercise("Standing Calf Raise", "Stående vadpress", CALVES, equipment = MACHINE, measurement = MACHINE_LEVEL, groupKey = "calf"),
        SeedExercise("Seated Calf Raise", "Sittande vadpress", CALVES, equipment = MACHINE, groupKey = "calf"),
        SeedExercise("Kettlebell Swing", "Kettlebellsving", GLUTES, listOf(HAMSTRINGS, BACK), KETTLEBELL, groupKey = "hinge"),

        // Abs
        SeedExercise("Crunch", "Crunch", ABS, equipment = NONE, measurement = MeasurementType.BODYWEIGHT, bwFactor = 30, groupKey = "abs"),
        SeedExercise("Sit-Up", "Situps", ABS, equipment = NONE, measurement = MeasurementType.BODYWEIGHT, bwFactor = 40, groupKey = "abs"),
        SeedExercise("Hanging Leg Raise", "Hängande benlyft", ABS, listOf(FOREARMS), PULLUP_BAR, measurement = MeasurementType.BODYWEIGHT, bwFactor = 50, groupKey = "abs"),
        SeedExercise("Cable Crunch", "Kabelcrunch", ABS, equipment = CABLE, groupKey = "abs"),
        SeedExercise("Ab Machine", "Magmaskin", ABS, equipment = MACHINE, measurement = MACHINE_LEVEL, groupKey = "abs"),
        SeedExercise("Russian Twist", "Rysk vridning", ABS, equipment = NONE, measurement = MeasurementType.BODYWEIGHT, bwFactor = 30),

        // --- Library expansion (v2) ---

        // Chest
        SeedExercise("Decline Bench Press", "Nedåtlutande bänkpress", CHEST, listOf(TRICEPS), BARBELL, listOf(BENCH, RACK), groupKey = "hpress"),
        SeedExercise("Smith Machine Bench Press", "Bänkpress i smithmaskin", CHEST, listOf(TRICEPS), SMITH_MACHINE, listOf(BENCH), groupKey = "hpress"),
        SeedExercise("Incline Cable Fly", "Lutande kabelflyes", CHEST, listOf(SHOULDERS), CABLE, extraEquipment = listOf(BENCH), groupKey = "fly"),
        SeedExercise("Machine Fly", "Flyesmaskin", CHEST, listOf(SHOULDERS), MACHINE, measurement = MACHINE_LEVEL, groupKey = "fly"),
        SeedExercise("Svend Press", "Svendpress", CHEST, listOf(SHOULDERS), WEIGHT_PLATE),
        SeedExercise("Diamond Push-Up", "Diamantarmhävning", TRICEPS, listOf(CHEST), NONE, measurement = MeasurementType.BODYWEIGHT, bwFactor = 65, groupKey = "triceps"),

        // Back
        SeedExercise("Sumo Deadlift", "Sumomarklyft", BACK, listOf(GLUTES, QUADS), BARBELL, groupKey = "hinge"),
        SeedExercise("Hex Bar Deadlift", "Marklyft med hexstång", BACK, listOf(QUADS, GLUTES), OTHER, groupKey = "hinge"),
        SeedExercise("Stiff-Leg Deadlift", "Raka marklyft", HAMSTRINGS, listOf(BACK, GLUTES), BARBELL, groupKey = "hinge"),
        SeedExercise("Rack Pull", "Rackdrag", BACK, listOf(GLUTES), BARBELL, listOf(RACK), groupKey = "hinge"),
        SeedExercise("Good Morning", "Good morning", HAMSTRINGS, listOf(BACK, GLUTES), BARBELL, listOf(RACK), groupKey = "hinge"),
        SeedExercise("Pendlay Row", "Pendlayrodd", BACK, listOf(BICEPS), BARBELL, groupKey = "row"),
        SeedExercise("Single-Arm Cable Row", "Enarmad kabelrodd", BACK, listOf(BICEPS), CABLE, groupKey = "row"),
        SeedExercise("Inverted Row", "Omvänd rodd", BACK, listOf(BICEPS), RACK, measurement = MeasurementType.BODYWEIGHT, bwFactor = 60, groupKey = "row"),
        SeedExercise("Straight-Arm Pulldown", "Rakarmsdrag", BACK, listOf(TRICEPS), CABLE),
        SeedExercise("Close-Grip Lat Pulldown", "Smalt latsdrag", BACK, listOf(BICEPS), CABLE, groupKey = "vpull"),
        SeedExercise("Reverse-Grip Lat Pulldown", "Omvänt latsdrag", BACK, listOf(BICEPS), CABLE, groupKey = "vpull"),
        SeedExercise("Neutral-Grip Pull-Up", "Räckhäv med neutralt grepp", BACK, listOf(BICEPS), PULLUP_BAR, measurement = MeasurementType.BODYWEIGHT, bwFactor = 100, groupKey = "vpull"),
        SeedExercise("Dumbbell Shrug", "Axellyft med hantlar", BACK, equipment = DUMBBELL),
        SeedExercise("Hyperextension", "Hyperextension", BACK, listOf(GLUTES, HAMSTRINGS), BENCH, measurement = MeasurementType.BODYWEIGHT, bwFactor = 50, groupKey = "hinge"),

        // Shoulders
        SeedExercise("Push Press", "Push press", SHOULDERS, listOf(TRICEPS, QUADS), BARBELL, listOf(RACK), groupKey = "vpress"),
        SeedExercise("Seated Barbell Press", "Sittande axelpress med skivstång", SHOULDERS, listOf(TRICEPS), BARBELL, listOf(BENCH, RACK), groupKey = "vpress"),
        SeedExercise("Landmine Press", "Landminepress", SHOULDERS, listOf(CHEST, TRICEPS), BARBELL, groupKey = "vpress"),
        SeedExercise("Cable Front Raise", "Frontlyft i kabel", SHOULDERS, equipment = CABLE),
        SeedExercise("Cable Reverse Fly", "Omvänt flyes i kabel", SHOULDERS, listOf(BACK), CABLE, listOf(MULTI_MACHINE), groupKey = "reardelt"),
        SeedExercise("Cuban Press", "Kubanpress", SHOULDERS, equipment = DUMBBELL),
        SeedExercise("Plate Front Raise", "Frontlyft med viktskiva", SHOULDERS, equipment = WEIGHT_PLATE),
        SeedExercise("Band Pull-Apart", "Band pull-apart", SHOULDERS, listOf(BACK), BAND, groupKey = "reardelt"),

        // Arms
        SeedExercise("EZ Bar Curl", "EZ-stångscurl", BICEPS, listOf(FOREARMS), EZ_BAR, groupKey = "curl"),
        SeedExercise("EZ Bar Reverse Curl", "Omvänd EZ-stångscurl", FOREARMS, listOf(BICEPS), EZ_BAR),
        SeedExercise("Incline Dumbbell Curl", "Lutande hantelcurl", BICEPS, listOf(FOREARMS), DUMBBELL, extraEquipment = listOf(BENCH), groupKey = "curl"),
        SeedExercise("Concentration Curl", "Koncentrationscurl", BICEPS, listOf(FOREARMS), DUMBBELL, extraEquipment = listOf(BENCH), groupKey = "curl"),
        SeedExercise("Cable Rope Hammer Curl", "Hammercurl med rep", BICEPS, listOf(FOREARMS), CABLE, groupKey = "curl"),
        SeedExercise("Cable Overhead Triceps Extension", "Tricepspress över huvudet i kabel", TRICEPS, equipment = CABLE, groupKey = "triceps"),
        SeedExercise("Rope Pushdown", "Tricepspress med rep", TRICEPS, equipment = CABLE, groupKey = "triceps"),
        SeedExercise("Bench Dip", "Bänkdips", TRICEPS, listOf(CHEST), BENCH, measurement = MeasurementType.BODYWEIGHT, bwFactor = 70, groupKey = "triceps"),
        SeedExercise("Barbell Wrist Curl", "Handledscurl med skivstång", FOREARMS, equipment = BARBELL, extraEquipment = listOf(BENCH)),
        SeedExercise("Reverse Wrist Curl", "Omvänd handledscurl", FOREARMS, equipment = DUMBBELL),
        SeedExercise("Wrist Roller", "Handledsrullare", FOREARMS, equipment = OTHER),

        // Legs & glutes
        SeedExercise("Smith Machine Squat", "Knäböj i smithmaskin", QUADS, listOf(GLUTES), SMITH_MACHINE, groupKey = "squat"),
        SeedExercise("Pistol Squat", "Enbensböj", QUADS, listOf(GLUTES), NONE, measurement = MeasurementType.BODYWEIGHT, bwFactor = 100, groupKey = "squat"),
        SeedExercise("Walking Lunge", "Gående utfall", QUADS, listOf(GLUTES), DUMBBELL, groupKey = "lunge"),
        SeedExercise("Reverse Lunge", "Bakåtutfall", QUADS, listOf(GLUTES), DUMBBELL, groupKey = "lunge"),
        SeedExercise("Barbell Lunge", "Utfall med skivstång", QUADS, listOf(GLUTES), BARBELL, listOf(RACK), groupKey = "lunge"),
        SeedExercise("Step-Up", "Uppsteg", QUADS, listOf(GLUTES), DUMBBELL, listOf(BENCH), groupKey = "lunge"),
        SeedExercise("Single-Leg Romanian Deadlift", "Enbens rumänsk marklyft", HAMSTRINGS, listOf(GLUTES), DUMBBELL, groupKey = "hinge"),
        SeedExercise("Nordic Curl", "Nordisk lårcurl", HAMSTRINGS, equipment = BENCH, measurement = MeasurementType.BODYWEIGHT, bwFactor = 70, groupKey = "legcurl"),
        SeedExercise("Glute Ham Raise", "Glute ham raise", HAMSTRINGS, listOf(GLUTES), MACHINE, measurement = MeasurementType.BODYWEIGHT, bwFactor = 70, groupKey = "legcurl"),
        SeedExercise("Cable Pull-Through", "Kabeldrag mellan benen", GLUTES, listOf(HAMSTRINGS), CABLE, groupKey = "thrust"),
        SeedExercise("Hip Abduction Machine", "Utåtföringsmaskin", GLUTES, equipment = MACHINE, measurement = MACHINE_LEVEL),
        SeedExercise("Hip Adduction Machine", "Inåtföringsmaskin", QUADS, equipment = MACHINE, measurement = MACHINE_LEVEL),
        SeedExercise("Calf Press on Leg Press", "Vadpress i benpress", CALVES, equipment = MACHINE, groupKey = "calf"),
        SeedExercise("Smith Machine Calf Raise", "Vadpress i smithmaskin", CALVES, equipment = SMITH_MACHINE, groupKey = "calf"),
        SeedExercise("Single-Leg Calf Raise", "Enbens vadpress", CALVES, equipment = NONE, measurement = MeasurementType.BODYWEIGHT, bwFactor = 100, groupKey = "calf"),
        SeedExercise("Box Jump", "Boxhopp", QUADS, listOf(GLUTES, CALVES), OTHER, measurement = MeasurementType.BODYWEIGHT, bwFactor = 100),

        // Core
        SeedExercise("Plank", "Planka", ABS, equipment = NONE, measurement = MeasurementType.BODYWEIGHT, bwFactor = 30),
        SeedExercise("Side Plank", "Sidoplanka", ABS, equipment = NONE, measurement = MeasurementType.BODYWEIGHT, bwFactor = 30),
        SeedExercise("Mountain Climbers", "Bergsklättrare", ABS, listOf(SHOULDERS), NONE, measurement = MeasurementType.BODYWEIGHT, bwFactor = 40),
        SeedExercise("Dead Bug", "Dead bug", ABS, equipment = NONE, measurement = MeasurementType.BODYWEIGHT, bwFactor = 20),
        SeedExercise("Ab Wheel Rollout", "Abhjul", ABS, listOf(SHOULDERS), OTHER, measurement = MeasurementType.BODYWEIGHT, bwFactor = 50, groupKey = "abs"),
        SeedExercise("Decline Sit-Up", "Nedåtlutande situps", ABS, equipment = BENCH, measurement = MeasurementType.BODYWEIGHT, bwFactor = 40, groupKey = "abs"),
        SeedExercise("Oblique Crunch", "Sneda crunches", ABS, equipment = NONE, measurement = MeasurementType.BODYWEIGHT, bwFactor = 30, groupKey = "abs"),
        SeedExercise("Dumbbell Side Bend", "Sidoböj med hantel", ABS, equipment = DUMBBELL),
        SeedExercise("Plate Twist", "Vridning med viktskiva", ABS, equipment = WEIGHT_PLATE),
        SeedExercise("Cable Woodchopper", "Vedhuggare i kabel", ABS, equipment = CABLE),
        SeedExercise("Pallof Press", "Pallofpress", ABS, equipment = CABLE),
        SeedExercise("Hanging Knee Raise", "Hängande knälyft", ABS, listOf(FOREARMS), PULLUP_BAR, measurement = MeasurementType.BODYWEIGHT, bwFactor = 40, groupKey = "abs"),
        SeedExercise("Toes to Bar", "Tår till stång", ABS, listOf(FOREARMS), PULLUP_BAR, measurement = MeasurementType.BODYWEIGHT, bwFactor = 60, groupKey = "abs"),

        // Full body & conditioning
        SeedExercise("Clean and Jerk", "Frivändning med stöt", FULL_BODY, listOf(SHOULDERS, QUADS), BARBELL),
        SeedExercise("Power Clean", "Frivändning", FULL_BODY, listOf(BACK, QUADS), BARBELL),
        SeedExercise("Snatch", "Ryck", FULL_BODY, listOf(SHOULDERS, QUADS), BARBELL),
        SeedExercise("Thruster", "Thruster", FULL_BODY, listOf(QUADS, SHOULDERS), BARBELL),
        SeedExercise("Burpee", "Burpee", FULL_BODY, equipment = NONE, measurement = MeasurementType.BODYWEIGHT, bwFactor = 100),
        SeedExercise("Farmer\u0027s Carry", "Farmers walk", FULL_BODY, listOf(FOREARMS), DUMBBELL),
        SeedExercise("Turkish Get-Up", "Turkisk uppresning", FULL_BODY, listOf(SHOULDERS, ABS), KETTLEBELL),
        SeedExercise("Kettlebell Clean and Press", "Kettlebell clean and press", FULL_BODY, listOf(SHOULDERS), KETTLEBELL),
        SeedExercise("Kettlebell Goblet Squat", "Goblet squat med kettlebell", QUADS, listOf(GLUTES), KETTLEBELL, groupKey = "squat"),
        SeedExercise("Kettlebell Snatch", "Kettlebellryck", FULL_BODY, listOf(SHOULDERS), KETTLEBELL),
        SeedExercise("Resistance Band Row", "Rodd med gummiband", BACK, listOf(BICEPS), BAND, groupKey = "row"),
        SeedExercise("Band Lateral Walk", "Sidogång med band", GLUTES, equipment = BAND),
        SeedExercise("Neck Flexion", "Nackböjning", SHOULDERS, equipment = WEIGHT_PLATE, extraEquipment = listOf(BENCH)),
        SeedExercise("Neck Extension", "Nacksträckning", SHOULDERS, equipment = WEIGHT_PLATE, extraEquipment = listOf(BENCH)),
    )

    /**
     * Seeds the exercise catalog. On first launch this inserts everything; on
     * later launches it tops up built-ins added in newer app versions
     * (matching non-custom exercises by English name), and re-syncs the
     * definitional fields (muscle groups, equipment, measurement type, ...)
     * of already-seeded built-ins to this catalog. That sync is safe because
     * built-ins can no longer be edited in the UI, and it's how existing
     * installs pick up catalog corrections (e.g. added secondary muscles)
     * shipped in a later app version.
     */
    suspend fun seedIfEmpty(db: SkrotDatabase) {
        val dao = db.exerciseDao()
        val existingGroupsByName = db.backupDao().allGroups().associateBy { it.nameEn }
        val groupIds = mutableMapOf<String, Long>()
        for (g in groups) {
            groupIds[g.key] = existingGroupsByName[g.nameEn]?.id
                ?: dao.insertGroup(ExerciseGroup(nameEn = g.nameEn, nameSv = g.nameSv))
        }
        val existingByName = dao.getAll()
            .filter { !it.isCustom }
            .associateBy { it.nameEn.lowercase() }
        val missing = exercises.filter { it.nameEn.lowercase() !in existingByName.keys }
        if (missing.isNotEmpty()) {
            dao.insertAll(
                missing.map { e ->
                    Exercise(
                        nameEn = e.nameEn,
                        nameSv = e.nameSv,
                        muscleGroup = e.muscle,
                        secondaryMuscles = e.secondary,
                        equipment = (listOf(e.equipment) + e.extraEquipment).distinct(),
                        measurementType = e.measurement,
                        isCustom = false,
                        bodyweightFactor = e.bwFactor,
                        groupId = e.groupKey?.let { groupIds[it] },
                    )
                }
            )
        }
        for (e in exercises) {
            val current = existingByName[e.nameEn.lowercase()] ?: continue
            val synced = current.copy(
                nameSv = e.nameSv,
                muscleGroup = e.muscle,
                secondaryMuscles = e.secondary,
                equipment = (listOf(e.equipment) + e.extraEquipment).distinct(),
                measurementType = e.measurement,
                bodyweightFactor = e.bwFactor,
                groupId = e.groupKey?.let { groupIds[it] },
            )
            if (synced != current) dao.update(synced)
        }
    }
}
