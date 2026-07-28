package dev.hinny.skrot.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Anchor
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Rowing
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material.icons.filled.SportsHandball
import androidx.compose.material.icons.filled.SportsKabaddi
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.hinny.skrot.data.model.Exercise
import dev.hinny.skrot.data.model.MetaDisplay
import dev.hinny.skrot.data.model.ProgramIcon

/**
 * Icons for programs and workout days, drawn from Material Symbols.
 *
 * An earlier attempt shipped hand-drawn muscle-group and equipment glyphs; they
 * were unreadable at list size and are gone. No permissively licensed set with
 * per-muscle-group coverage exists, so muscle groups and equipment are named in
 * words instead — see [ExerciseMeta].
 */

@Composable
fun ProgramIcon.vector(): ImageVector = when (this) {
    ProgramIcon.BARBELL -> Icons.Filled.FitnessCenter
    ProgramIcon.DUMBBELL -> Icons.Filled.SportsGymnastics
    ProgramIcon.RUN -> Icons.Filled.DirectionsRun
    ProgramIcon.HEART -> Icons.Filled.Favorite
    ProgramIcon.FLEX -> Icons.Filled.SportsMartialArts
    ProgramIcon.BOLT -> Icons.Filled.Bolt
    ProgramIcon.TIMER -> Icons.Filled.Timer
    ProgramIcon.TROPHY -> Icons.Filled.EmojiEvents
    ProgramIcon.SHIELD -> Icons.Filled.Shield
    ProgramIcon.FIRE -> Icons.Filled.LocalFireDepartment
    ProgramIcon.MOUNTAIN -> Icons.Filled.Terrain
    ProgramIcon.YOGA -> Icons.Filled.SelfImprovement
    ProgramIcon.TRENDING_UP -> Icons.Filled.TrendingUp
    ProgramIcon.CALENDAR -> Icons.Filled.CalendarMonth
    ProgramIcon.SCALE -> Icons.Filled.MonitorWeight
    ProgramIcon.REPEAT -> Icons.Filled.Repeat
    ProgramIcon.SPEED -> Icons.Filled.Speed
    ProgramIcon.WALK -> Icons.Filled.DirectionsWalk
    ProgramIcon.BIKE -> Icons.Filled.DirectionsBike
    ProgramIcon.SWIM -> Icons.Filled.Pool
    ProgramIcon.HIKE -> Icons.Filled.Hiking
    ProgramIcon.ROW -> Icons.Filled.Rowing
    ProgramIcon.COMBAT -> Icons.Filled.SportsKabaddi
    ProgramIcon.HANDBALL -> Icons.Filled.SportsHandball
    ProgramIcon.SOCCER -> Icons.Filled.SportsSoccer
    ProgramIcon.BASKETBALL -> Icons.Filled.SportsBasketball
    ProgramIcon.HEARTBEAT -> Icons.Filled.MonitorHeart
    ProgramIcon.WHATSHOT -> Icons.Filled.Whatshot
    ProgramIcon.ALARM -> Icons.Filled.Alarm
    ProgramIcon.CHART -> Icons.Filled.ShowChart
    ProgramIcon.MEDAL -> Icons.Filled.MilitaryTech
    ProgramIcon.PREMIUM -> Icons.Filled.WorkspacePremium
    ProgramIcon.STAR -> Icons.Filled.Star
    ProgramIcon.FLAG -> Icons.Filled.Flag
    ProgramIcon.ROCKET -> Icons.Filled.RocketLaunch
    ProgramIcon.MIND -> Icons.Filled.Psychology
    ProgramIcon.MEASURE -> Icons.Filled.Straighten
    ProgramIcon.BODY -> Icons.Filled.Accessibility
    ProgramIcon.SUN -> Icons.Filled.WbSunny
    ProgramIcon.NIGHT -> Icons.Filled.NightsStay
    ProgramIcon.FROST -> Icons.Filled.AcUnit
    ProgramIcon.ANCHOR -> Icons.Filled.Anchor

    // Retired: these were backed by the hand-drawn equipment and muscle glyphs.
    // They stay in the enum so programs that already chose one still load, but
    // they are no longer offered in the picker.
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
private fun MetaText(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = color)
}
