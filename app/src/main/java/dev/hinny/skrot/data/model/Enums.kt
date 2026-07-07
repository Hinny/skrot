package dev.hinny.skrot.data.model

import kotlinx.serialization.Serializable

/** How load is entered and tracked for an exercise. */
enum class MeasurementType {
    /** Free weights and similar; decimal kg input. */
    WEIGHT_KG,

    /** Pin-loaded machines; integer, unit-less "level" input. */
    MACHINE_LEVEL,

    /** Reps only, with optional added weight (positive kg) or assistance (negative kg). */
    BODYWEIGHT,
}

enum class SetType {
    WARMUP,
    STANDARD,
    DROP_SET,
    FAILURE,
}

/** What a new, unlogged set is pre-filled with when logging. */
enum class PrefillMode {
    /** Previous actual weight + reps. */
    LAST_SESSION,

    /** Planned weight + reps from the routine. */
    TARGETS,

    /** Last session's weight, target reps. */
    HYBRID,
}

enum class ScheduleMode {
    /** Days run in sequence A -> B -> C -> A regardless of weekday. */
    ROTATING,

    /** Days are assigned to fixed weekdays. */
    FIXED_WEEKDAYS,
}

/** What happens to the rotating sequence when the user swaps in another workout. */
enum class SwapBehavior {
    /** The skipped workout stays next in the sequence (default). */
    SKIPPED_STAYS_NEXT,

    /** The sequence advances past the workout that was actually performed. */
    ADVANCE,
}

enum class MuscleGroup {
    CHEST,
    BACK,
    SHOULDERS,
    BICEPS,
    TRICEPS,
    FOREARMS,
    ABS,
    QUADS,
    HAMSTRINGS,
    GLUTES,
    CALVES,
    FULL_BODY,
}

/**
 * Equipment needed to perform an exercise. An exercise can require several
 * pieces at once (e.g. bench press: barbell + bench + rack). NONE means the
 * exercise needs no equipment at all (push-ups) — unlike e.g. dips, which are
 * body-weight loaded but still need a DIP_STATION.
 */
@Serializable
enum class Equipment {
    BARBELL,
    EZ_BAR,
    DUMBBELL,
    KETTLEBELL,
    WEIGHT_PLATE,
    MACHINE,
    MULTI_MACHINE,
    SMITH_MACHINE,
    CABLE,
    BENCH,
    PULLUP_BAR,
    DIP_STATION,
    RACK,
    BAND,
    NONE,
    OTHER,
}

/** Curated icon set for programs and workout days (mapped to Material Symbols in the UI). */
enum class ProgramIcon {
    BARBELL,
    DUMBBELL,
    RUN,
    HEART,
    FLEX,
    BOLT,
    TIMER,
    TROPHY,
    SHIELD,
    FIRE,
    MOUNTAIN,
    YOGA,
}

enum class CoachPersonality {
    CHEERLEADER,
    BRO,
    PT,
    MINIMAL,
}

enum class CoachFrequency {
    LOW,
    MEDIUM,
    HIGH,
}

enum class WeightUnit {
    KG,
    LBS,
}

enum class AppLanguage {
    SYSTEM,
    ENGLISH,
    SWEDISH,
}

enum class ThemeMode {
    DARK,
    LIGHT,
    SYSTEM,
}
