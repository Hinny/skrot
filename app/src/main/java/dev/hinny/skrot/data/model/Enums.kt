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
    /** Deliberately no icon. Kept out of [pickable]; the picker offers it separately. */
    NONE,
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
    GYM_BICEPS,
    GYM_LEG,
    GYM_FIST,
    GYM_FIST_BUMP,
    GYM_SWORDS,
    GYM_LIFTER,
    EM_DYNAMITE,
    EM_BEER,
    EM_BEERS,
    EM_MEDAL_1ST,
    EM_MEDAL_2ND,
    EM_MEDAL_3RD,
    EM_MEDAL_SPORTS,
    EM_MEDAL_MILITARY,
    EM_CHAINS,
    EM_MAGNET,
    EM_BALANCE_SCALE,
    EM_CIGARETTE,
    EM_PILL,
    EM_PLASTER,
    EM_SHIELD,
    EM_BOMB,
    EM_KEY,
    EM_KEY_OLD,
    EM_GEAR,
    EM_SKULL,
    EM_SKULL_BONES,
    EM_FLAG_CHEQUERED,
    EM_STOPWATCH,
    EM_HOURGLASS,
    EM_BATTERY,
    EM_ICE,
    EM_MILK,
    EM_MEAT,
    EM_EGG,
    EM_BONE,
    EM_HAMMER,
    EM_HAMMER_WRENCH,
    EM_CROWN,
    EM_GEM,
    EM_DNA,
    EM_STETHOSCOPE,
    EM_TEST_TUBE,
    EM_MOAI,
    EM_BRICK,
    EM_AXE,
    EM_LOTION,
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
            GYM_BARBELL, GYM_DUMBBELL, GYM_KETTLEBELL, GYM_LIFTER, GYM_TREADMILL,
            GYM_JUMP_ROPE, GYM_STRETCHING, GYM_STRETCHING_2, GYM_YOGA,
            GYM_BODY_SCAN, GYM_SHOE, GYM_BOTTLE,
            // The body doing the work
            GYM_BICEPS, GYM_LEG, GYM_FIST, GYM_FIST_BUMP, GYM_SWORDS,
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
            // Goals and spoils
            EM_MEDAL_1ST, EM_MEDAL_2ND, EM_MEDAL_3RD, EM_MEDAL_SPORTS,
            EM_MEDAL_MILITARY, EM_CROWN, EM_GEM, EM_FLAG_CHEQUERED,
            // Grit
            EM_DYNAMITE, EM_BOMB, EM_SKULL, EM_SKULL_BONES, EM_MOAI, EM_AXE, EM_HAMMER,
            EM_HAMMER_WRENCH, EM_BRICK, EM_CHAINS, EM_MAGNET, EM_GEAR, EM_KEY,
            EM_KEY_OLD, EM_SHIELD,
            // Weighing and timing
            EM_BALANCE_SCALE, EM_STOPWATCH, EM_HOURGLASS, EM_BATTERY,
            // Fuel and vice
            EM_BEER, EM_BEERS, EM_MILK, EM_MEAT, EM_EGG, EM_CIGARETTE,
            // Patching yourself up
            EM_PILL, EM_PLASTER, EM_STETHOSCOPE, EM_ICE, EM_BONE, EM_DNA, EM_TEST_TUBE,
            EM_LOTION,
        )
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

/**
 * A card the home screen can show. Which ones appear is configurable, since a
 * home screen is only useful if it holds what its owner actually checks.
 */
enum class HomeSection {
    COACH,
    NEXT_WORKOUT,
    RECOVERY,
    BACKUP_REMINDER,
    LAST_SESSION,
    BODY_METRIC,
    DAYS_SINCE_LAST,
    WEEK_STREAK,
    ONE_REP_MAX;

    companion object {
        /**
         * Shown until the user says otherwise: the workout-facing cards plus the
         * recap ones. The rest are opt-in so the screen doesn't start cluttered.
         */
        val DEFAULTS: Set<HomeSection> = setOf(
            COACH, NEXT_WORKOUT, RECOVERY, BACKUP_REMINDER,
            LAST_SESSION, BODY_METRIC,
        )
    }
}

/** Window the home screen's estimated 1RM is taken from. */
enum class OneRepMaxRange {
    /** The most recent estimate, whenever that was. */
    CURRENT,
    PAST_YEAR,
    PAST_3_YEARS,
    ALL_TIME,
}

/** Order exercise lists and pickers are presented in. */
enum class ExerciseSort {
    NAME,
    MOST_USED,
}
