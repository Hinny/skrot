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

/**
 * Curated icon set for programs and workout days, drawn from Material Symbols.
 * Entries are stored by name, so they may only be appended — the retired block
 * at the end is kept so programs that chose one still load, and is filtered out
 * of the picker by [pickable].
 */
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
    EZ_BAR,
    KETTLEBELL,
    WEIGHT_PLATE,
    MACHINE,
    SMITH_MACHINE,
    CABLE,
    BENCH,
    PULLUP_BAR,
    DIP_STATION,
    RACK,
    BAND,
    MUSCLE_CHEST,
    MUSCLE_BACK,
    MUSCLE_SHOULDERS,
    MUSCLE_BICEPS,
    MUSCLE_TRICEPS,
    MUSCLE_FOREARMS,
    MUSCLE_ABS,
    MUSCLE_QUADS,
    MUSCLE_HAMSTRINGS,
    MUSCLE_GLUTES,
    MUSCLE_CALVES,
    MUSCLE_FULL_BODY,
    TRENDING_UP,
    CALENDAR,
    SCALE,
    REPEAT,
    SPEED,
    WALK,
    BIKE,
    SWIM,
    HIKE,
    ROW,
    COMBAT,
    HANDBALL,
    SOCCER,
    BASKETBALL,
    HEARTBEAT,
    WHATSHOT,
    ALARM,
    CHART,
    MEDAL,
    PREMIUM,
    STAR,
    FLAG,
    ROCKET,
    MIND,
    MEASURE,
    BODY,
    SUN,
    NIGHT,
    FROST,
    ANCHOR,
    BODY_ACTIVE,
    BAR_CHART,
    INSIGHTS,
    LEADERBOARD,
    HOURGLASS,
    SCHEDULE,
    EVENT_REPEAT,
    LOOP,
    VERIFIED,
    SUPERSET,
    SPLIT,
    BLOCKS,
    TUNE,
    PERCENT,
    PLUS_ONE,
    NUMBERS,
    CALCULATE,
    LEVELS,
    EQUALIZER,
    INTERVAL,
    RESTART,
    ENERGY,
    FINISH,
    CELEBRATION,
    REHAB,
    REST,
    MOBILITY,
    GYM_BARBELL,
    GYM_DUMBBELL,
    GYM_KETTLEBELL,
    GYM_TREADMILL,
    GYM_JUMP_ROPE,
    GYM_STRETCHING,
    GYM_STRETCHING_2,
    GYM_YOGA,
    GYM_BODY_SCAN,
    GYM_SHOE,
    GYM_BOTTLE,
    ;

    companion object {
        /**
         * The icons offered in the picker, grouped by what they say about a
         * program: what you do, how it's loaded and measured, how it's paced,
         * how it progresses, how hard it is, what it's aiming at, and recovery.
         * Everything here has to mean something to someone lifting weights.
         */
        val pickable: List<ProgramIcon> = listOf(
            // The gym itself
            GYM_BARBELL, GYM_DUMBBELL, GYM_KETTLEBELL, GYM_TREADMILL, GYM_JUMP_ROPE,
            GYM_STRETCHING, GYM_STRETCHING_2, GYM_YOGA, GYM_BODY_SCAN, GYM_SHOE, GYM_BOTTLE,
            // What you do, and how the program is put together
            BARBELL, DUMBBELL, FLEX, BODY, BODY_ACTIVE, YOGA, RUN,
            SUPERSET, SPLIT, BLOCKS, TUNE,
            // Load, reps and measurement
            SCALE, MEASURE, PERCENT, PLUS_ONE, NUMBERS, CALCULATE, LEVELS, SPEED,
            // Pacing and scheduling
            TIMER, ALARM, HOURGLASS, INTERVAL, SCHEDULE, CALENDAR, EVENT_REPEAT,
            REPEAT, LOOP, RESTART,
            // Progression
            TRENDING_UP, CHART, BAR_CHART, EQUALIZER, INSIGHTS, LEADERBOARD,
            // Intensity
            BOLT, FIRE, WHATSHOT, ENERGY, MOUNTAIN,
            // What it's aiming at
            TROPHY, MEDAL, PREMIUM, STAR, FLAG, VERIFIED, FINISH, CELEBRATION,
            // Conditioning, recovery and rehab
            HEART, HEARTBEAT, SHIELD, REHAB, REST, MOBILITY,
        )

        /**
         * Everything else is retired — the hand-drawn equipment and muscle
         * glyphs, plus a batch of other-sport and weather icons that had
         * nothing to do with strength training. They stay in the enum because
         * the value is persisted by name, but are never offered again.
         */
        val retired: List<ProgramIcon> = entries.filterNot { it in pickable }
    }
}

/**
 * Whether an exercise's muscle groups and equipment are listed at all. Chosen
 * separately for the two, in Settings. (An icon mode existed briefly; the
 * hand-drawn glyphs behind it were unreadable and were removed.)
 */
enum class MetaDisplay {
    TEXT,
    HIDDEN,
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

/** Optional profile field; purely informational, stored on-device only. */
enum class Sex {
    UNSPECIFIED,
    FEMALE,
    MALE,
    OTHER,
}
