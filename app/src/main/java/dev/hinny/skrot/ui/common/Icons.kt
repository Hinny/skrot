package dev.hinny.skrot.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.PlusOne
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Segment
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.hinny.skrot.data.model.Exercise
import dev.hinny.skrot.data.model.MetaDisplay
import dev.hinny.skrot.data.model.ProgramIcon

/**
 * Icons for programs and workout days, drawn from Material Symbols and limited
 * to things that mean something to someone lifting weights.
 *
 * An earlier attempt shipped hand-drawn muscle-group and equipment glyphs; they
 * were unreadable at list size and are gone. No permissively licensed set with
 * per-muscle-group coverage exists, so muscle groups and equipment are named in
 * words instead — see [ExerciseMeta].
 */

@Composable
fun ProgramIcon.vector(): ImageVector = when (this) {
    // What you do
    ProgramIcon.BARBELL -> Icons.Filled.FitnessCenter
    ProgramIcon.DUMBBELL -> Icons.Filled.SportsGymnastics
    ProgramIcon.FLEX -> Icons.Filled.SportsMartialArts
    ProgramIcon.BODY -> Icons.Filled.Accessibility
    ProgramIcon.BODY_ACTIVE -> Icons.Filled.AccessibilityNew
    ProgramIcon.YOGA -> Icons.Filled.SelfImprovement
    ProgramIcon.RUN -> Icons.Filled.DirectionsRun
    ProgramIcon.SUPERSET -> Icons.Filled.Link
    ProgramIcon.SPLIT -> Icons.Filled.Segment
    ProgramIcon.BLOCKS -> Icons.Filled.Layers
    ProgramIcon.TUNE -> Icons.Filled.Tune

    // Load, reps and measurement
    ProgramIcon.SCALE -> Icons.Filled.MonitorWeight
    ProgramIcon.MEASURE -> Icons.Filled.Straighten
    ProgramIcon.PERCENT -> Icons.Filled.Percent
    ProgramIcon.PLUS_ONE -> Icons.Filled.PlusOne
    ProgramIcon.NUMBERS -> Icons.Filled.Numbers
    ProgramIcon.CALCULATE -> Icons.Filled.Calculate
    ProgramIcon.LEVELS -> Icons.Filled.SignalCellularAlt
    ProgramIcon.SPEED -> Icons.Filled.Speed

    // Pacing and scheduling
    ProgramIcon.TIMER -> Icons.Filled.Timer
    ProgramIcon.ALARM -> Icons.Filled.Alarm
    ProgramIcon.HOURGLASS -> Icons.Filled.HourglassBottom
    ProgramIcon.INTERVAL -> Icons.Filled.AvTimer
    ProgramIcon.SCHEDULE -> Icons.Filled.Schedule
    ProgramIcon.CALENDAR -> Icons.Filled.CalendarMonth
    ProgramIcon.EVENT_REPEAT -> Icons.Filled.EventRepeat
    ProgramIcon.REPEAT -> Icons.Filled.Repeat
    ProgramIcon.LOOP -> Icons.Filled.Loop
    ProgramIcon.RESTART -> Icons.Filled.RestartAlt

    // Progression
    ProgramIcon.TRENDING_UP -> Icons.Filled.TrendingUp
    ProgramIcon.CHART -> Icons.Filled.ShowChart
    ProgramIcon.BAR_CHART -> Icons.Filled.BarChart
    ProgramIcon.EQUALIZER -> Icons.Filled.Equalizer
    ProgramIcon.INSIGHTS -> Icons.Filled.Insights
    ProgramIcon.LEADERBOARD -> Icons.Filled.Leaderboard

    // Intensity
    ProgramIcon.BOLT -> Icons.Filled.Bolt
    ProgramIcon.FIRE -> Icons.Filled.LocalFireDepartment
    ProgramIcon.WHATSHOT -> Icons.Filled.Whatshot
    ProgramIcon.ENERGY -> Icons.Filled.BatteryChargingFull
    ProgramIcon.MOUNTAIN -> Icons.Filled.Terrain

    // Goals
    ProgramIcon.TROPHY -> Icons.Filled.EmojiEvents
    ProgramIcon.MEDAL -> Icons.Filled.MilitaryTech
    ProgramIcon.PREMIUM -> Icons.Filled.WorkspacePremium
    ProgramIcon.STAR -> Icons.Filled.Star
    ProgramIcon.FLAG -> Icons.Filled.Flag
    ProgramIcon.VERIFIED -> Icons.Filled.Verified
    ProgramIcon.FINISH -> Icons.Filled.SportsScore
    ProgramIcon.CELEBRATION -> Icons.Filled.Celebration

    // Conditioning and recovery
    ProgramIcon.HEART -> Icons.Filled.Favorite
    ProgramIcon.HEARTBEAT -> Icons.Filled.MonitorHeart
    ProgramIcon.SHIELD -> Icons.Filled.Shield
    ProgramIcon.REHAB -> Icons.Filled.Healing
    ProgramIcon.REST -> Icons.Filled.Hotel
    ProgramIcon.MOBILITY -> Icons.Filled.Spa

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
    -> Icons.Filled.FitnessCenter

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
    -> Icons.Filled.Accessibility
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
