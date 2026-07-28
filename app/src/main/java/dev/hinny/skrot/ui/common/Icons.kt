package dev.hinny.skrot.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.AvTimer
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Healing
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Loop
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.PlusOne
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Segment
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SportsGymnastics
import androidx.compose.material.icons.outlined.SportsMartialArts
import androidx.compose.material.icons.outlined.SportsScore
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.annotation.DrawableRes
import dev.hinny.skrot.R
import dev.hinny.skrot.data.model.Exercise
import dev.hinny.skrot.data.model.MetaDisplay
import dev.hinny.skrot.data.model.ProgramIcon

/**
 * Icons for programs and workout days, limited to things that mean something to
 * someone lifting weights. The concrete gym objects come from Tabler Icons
 * (MIT); the rest are Material Symbols, in their Outlined variant so the whole
 * set reads as one line-drawn family rather than half solid, half line.
 *
 * An earlier attempt shipped hand-drawn muscle-group and equipment glyphs; they
 * were unreadable at list size and are gone. No permissively licensed set with
 * per-muscle-group coverage exists, so muscle groups and equipment are named in
 * words instead — see [ExerciseMeta].
 */

@Composable
fun ProgramIcon.vector(): ImageVector = when (this) {
    // The gym itself — Tabler Icons (MIT), converted to vector drawables
    ProgramIcon.GYM_BARBELL -> vectorRes(R.drawable.ic_gym_barbell)
    ProgramIcon.GYM_DUMBBELL -> vectorRes(R.drawable.ic_gym_dumbbell)
    ProgramIcon.GYM_KETTLEBELL -> vectorRes(R.drawable.ic_gym_kettlebell)
    ProgramIcon.GYM_TREADMILL -> vectorRes(R.drawable.ic_gym_treadmill)
    ProgramIcon.GYM_JUMP_ROPE -> vectorRes(R.drawable.ic_gym_jump_rope)
    ProgramIcon.GYM_STRETCHING -> vectorRes(R.drawable.ic_gym_stretching)
    ProgramIcon.GYM_STRETCHING_2 -> vectorRes(R.drawable.ic_gym_stretching_2)
    ProgramIcon.GYM_YOGA -> vectorRes(R.drawable.ic_gym_yoga)
    ProgramIcon.GYM_BODY_SCAN -> vectorRes(R.drawable.ic_gym_body_scan)
    ProgramIcon.GYM_SHOE -> vectorRes(R.drawable.ic_gym_shoe)
    ProgramIcon.GYM_BOTTLE -> vectorRes(R.drawable.ic_gym_bottle)

    // What you do
    ProgramIcon.BARBELL -> Icons.Outlined.FitnessCenter
    ProgramIcon.DUMBBELL -> Icons.Outlined.SportsGymnastics
    ProgramIcon.FLEX -> Icons.Outlined.SportsMartialArts
    ProgramIcon.BODY -> Icons.Outlined.Accessibility
    ProgramIcon.BODY_ACTIVE -> Icons.Outlined.AccessibilityNew
    ProgramIcon.YOGA -> Icons.Outlined.SelfImprovement
    ProgramIcon.RUN -> Icons.Outlined.DirectionsRun
    ProgramIcon.SUPERSET -> Icons.Outlined.Link
    ProgramIcon.SPLIT -> Icons.Outlined.Segment
    ProgramIcon.BLOCKS -> Icons.Outlined.Layers
    ProgramIcon.TUNE -> Icons.Outlined.Tune

    // Load, reps and measurement
    ProgramIcon.SCALE -> Icons.Outlined.MonitorWeight
    ProgramIcon.MEASURE -> Icons.Outlined.Straighten
    ProgramIcon.PERCENT -> Icons.Outlined.Percent
    ProgramIcon.PLUS_ONE -> Icons.Outlined.PlusOne
    ProgramIcon.NUMBERS -> Icons.Outlined.Numbers
    ProgramIcon.CALCULATE -> Icons.Outlined.Calculate
    ProgramIcon.LEVELS -> Icons.Outlined.SignalCellularAlt
    ProgramIcon.SPEED -> Icons.Outlined.Speed

    // Pacing and scheduling
    ProgramIcon.TIMER -> Icons.Outlined.Timer
    ProgramIcon.ALARM -> Icons.Outlined.Alarm
    ProgramIcon.HOURGLASS -> Icons.Outlined.HourglassBottom
    ProgramIcon.INTERVAL -> Icons.Outlined.AvTimer
    ProgramIcon.SCHEDULE -> Icons.Outlined.Schedule
    ProgramIcon.CALENDAR -> Icons.Outlined.CalendarMonth
    ProgramIcon.EVENT_REPEAT -> Icons.Outlined.EventRepeat
    ProgramIcon.REPEAT -> Icons.Outlined.Repeat
    ProgramIcon.LOOP -> Icons.Outlined.Loop
    ProgramIcon.RESTART -> Icons.Outlined.RestartAlt

    // Progression
    ProgramIcon.TRENDING_UP -> Icons.Outlined.TrendingUp
    ProgramIcon.CHART -> Icons.Outlined.ShowChart
    ProgramIcon.BAR_CHART -> Icons.Outlined.BarChart
    ProgramIcon.EQUALIZER -> Icons.Outlined.Equalizer
    ProgramIcon.INSIGHTS -> Icons.Outlined.Insights
    ProgramIcon.LEADERBOARD -> Icons.Outlined.Leaderboard

    // Intensity
    ProgramIcon.BOLT -> Icons.Outlined.Bolt
    ProgramIcon.FIRE -> Icons.Outlined.LocalFireDepartment
    ProgramIcon.WHATSHOT -> Icons.Outlined.Whatshot
    ProgramIcon.ENERGY -> Icons.Outlined.BatteryChargingFull
    ProgramIcon.MOUNTAIN -> Icons.Outlined.Terrain

    // Goals
    ProgramIcon.TROPHY -> Icons.Outlined.EmojiEvents
    ProgramIcon.MEDAL -> Icons.Outlined.MilitaryTech
    ProgramIcon.PREMIUM -> Icons.Outlined.WorkspacePremium
    ProgramIcon.STAR -> Icons.Outlined.Star
    ProgramIcon.FLAG -> Icons.Outlined.Flag
    ProgramIcon.VERIFIED -> Icons.Outlined.Verified
    ProgramIcon.FINISH -> Icons.Outlined.SportsScore
    ProgramIcon.CELEBRATION -> Icons.Outlined.Celebration

    // Conditioning and recovery
    ProgramIcon.HEART -> Icons.Outlined.Favorite
    ProgramIcon.HEARTBEAT -> Icons.Outlined.MonitorHeart
    ProgramIcon.SHIELD -> Icons.Outlined.Shield
    ProgramIcon.REHAB -> Icons.Outlined.Healing
    ProgramIcon.REST -> Icons.Outlined.Hotel
    ProgramIcon.MOBILITY -> Icons.Outlined.Spa

    // Retired: the hand-drawn equipment and muscle glyphs, and a batch of
    // other-sport and weather icons. Never offered, but a program that already
    // stored one still has to render.
    ProgramIcon.EZ_BAR,
    ProgramIcon.KETTLEBELL,
    ProgramIcon.WEIGHT_PLATE,
    ProgramIcon.MACHINE,
    ProgramIcon.SMITH_MACHINE,
    ProgramIcon.CABLE,
    ProgramIcon.BENCH,
    ProgramIcon.PULLUP_BAR,
    ProgramIcon.DIP_STATION,
    ProgramIcon.RACK,
    ProgramIcon.BAND,
    ProgramIcon.WALK,
    ProgramIcon.BIKE,
    ProgramIcon.SWIM,
    ProgramIcon.HIKE,
    ProgramIcon.ROW,
    ProgramIcon.COMBAT,
    ProgramIcon.HANDBALL,
    ProgramIcon.SOCCER,
    ProgramIcon.BASKETBALL,
    ProgramIcon.ROCKET,
    ProgramIcon.MIND,
    ProgramIcon.SUN,
    ProgramIcon.NIGHT,
    ProgramIcon.FROST,
    ProgramIcon.ANCHOR,
    -> Icons.Outlined.FitnessCenter

    ProgramIcon.MUSCLE_CHEST,
    ProgramIcon.MUSCLE_BACK,
    ProgramIcon.MUSCLE_SHOULDERS,
    ProgramIcon.MUSCLE_BICEPS,
    ProgramIcon.MUSCLE_TRICEPS,
    ProgramIcon.MUSCLE_FOREARMS,
    ProgramIcon.MUSCLE_ABS,
    ProgramIcon.MUSCLE_QUADS,
    ProgramIcon.MUSCLE_HAMSTRINGS,
    ProgramIcon.MUSCLE_GLUTES,
    ProgramIcon.MUSCLE_CALVES,
    ProgramIcon.MUSCLE_FULL_BODY,
    -> Icons.Outlined.Accessibility
}

/**
 * How muscle groups and equipment are shown in lists and detail views, chosen
 * in Settings and provided once at the app root so every call site agrees.
 */
val LocalMuscleDisplay = compositionLocalOf { MetaDisplay.TEXT }
val LocalEquipmentDisplay = compositionLocalOf { MetaDisplay.TEXT }

/**
 * An exercise's muscle groups and equipment. The primary muscle carries full
 * contrast; secondary muscles and equipment are recessive.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseMeta(e: Exercise, modifier: Modifier = Modifier) {
    val showMuscles = LocalMuscleDisplay.current == MetaDisplay.TEXT
    val showEquipment = LocalEquipmentDisplay.current == MetaDisplay.TEXT && e.equipment.isNotEmpty()
    if (!showMuscles && !showEquipment) return

    val primaryColor = MaterialTheme.colorScheme.onSurface
    val secondaryColor = MaterialTheme.colorScheme.onSurfaceVariant
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (showMuscles) {
            MetaText(muscleLabel(e.muscleGroup), primaryColor)
            e.secondaryMuscles.forEach { MetaText(muscleLabel(it), secondaryColor) }
        }
        if (showEquipment) {
            if (showMuscles) MetaText("·", secondaryColor)
            e.equipment.forEach { MetaText(equipmentLabel(it), secondaryColor) }
        }
    }
}

@Composable
private fun MetaText(text: String, color: Color) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = color)
}

@Composable
private fun vectorRes(@DrawableRes id: Int): ImageVector = ImageVector.vectorResource(id)
