package dev.hinny.skrot.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import dev.hinny.skrot.R
import dev.hinny.skrot.data.model.Equipment
import dev.hinny.skrot.data.model.Exercise
import dev.hinny.skrot.data.model.MetaDisplay
import dev.hinny.skrot.data.model.MuscleGroup
import dev.hinny.skrot.data.model.ProgramIcon

/**
 * The app's icon vocabulary. Muscle groups and equipment are hand-drawn vectors
 * (`res/drawable/ic_muscle_*`, `ic_equip_*`) in one geometric style, because
 * Material has nothing for them; programs may use those same drawings plus a
 * handful of Material symbols for the non-anatomical concepts.
 */

@DrawableRes
fun muscleIconRes(m: MuscleGroup): Int = when (m) {
    MuscleGroup.CHEST -> R.drawable.ic_muscle_chest
    MuscleGroup.BACK -> R.drawable.ic_muscle_back
    MuscleGroup.SHOULDERS -> R.drawable.ic_muscle_shoulders
    MuscleGroup.BICEPS -> R.drawable.ic_muscle_biceps
    MuscleGroup.TRICEPS -> R.drawable.ic_muscle_triceps
    MuscleGroup.FOREARMS -> R.drawable.ic_muscle_forearms
    MuscleGroup.ABS -> R.drawable.ic_muscle_abs
    MuscleGroup.QUADS -> R.drawable.ic_muscle_quads
    MuscleGroup.HAMSTRINGS -> R.drawable.ic_muscle_hamstrings
    MuscleGroup.GLUTES -> R.drawable.ic_muscle_glutes
    MuscleGroup.CALVES -> R.drawable.ic_muscle_calves
    MuscleGroup.FULL_BODY -> R.drawable.ic_muscle_full_body
}

@DrawableRes
fun equipmentIconRes(e: Equipment): Int = when (e) {
    Equipment.BARBELL -> R.drawable.ic_equip_barbell
    Equipment.EZ_BAR -> R.drawable.ic_equip_ez_bar
    Equipment.DUMBBELL -> R.drawable.ic_equip_dumbbell
    Equipment.KETTLEBELL -> R.drawable.ic_equip_kettlebell
    Equipment.WEIGHT_PLATE -> R.drawable.ic_equip_weight_plate
    Equipment.MACHINE -> R.drawable.ic_equip_machine
    Equipment.MULTI_MACHINE -> R.drawable.ic_equip_multi_machine
    Equipment.SMITH_MACHINE -> R.drawable.ic_equip_smith_machine
    Equipment.CABLE -> R.drawable.ic_equip_cable
    Equipment.BENCH -> R.drawable.ic_equip_bench
    Equipment.PULLUP_BAR -> R.drawable.ic_equip_pullup_bar
    Equipment.DIP_STATION -> R.drawable.ic_equip_dip_station
    Equipment.RACK -> R.drawable.ic_equip_rack
    Equipment.BAND -> R.drawable.ic_equip_band
    Equipment.NONE -> R.drawable.ic_equip_none
    Equipment.OTHER -> R.drawable.ic_equip_other
}

@Composable
fun MuscleGroup.vector(): ImageVector = ImageVector.vectorResource(muscleIconRes(this))

@Composable
fun Equipment.vector(): ImageVector = ImageVector.vectorResource(equipmentIconRes(this))

@Composable
fun ProgramIcon.vector(): ImageVector = when (this) {
    // Equipment drawings
    ProgramIcon.BARBELL -> vectorRes(R.drawable.ic_equip_barbell)
    ProgramIcon.DUMBBELL -> vectorRes(R.drawable.ic_equip_dumbbell)
    ProgramIcon.KETTLEBELL -> vectorRes(R.drawable.ic_equip_kettlebell)
    ProgramIcon.EZ_BAR -> vectorRes(R.drawable.ic_equip_ez_bar)
    ProgramIcon.WEIGHT_PLATE -> vectorRes(R.drawable.ic_equip_weight_plate)
    ProgramIcon.MACHINE -> vectorRes(R.drawable.ic_equip_machine)
    ProgramIcon.SMITH_MACHINE -> vectorRes(R.drawable.ic_equip_smith_machine)
    ProgramIcon.CABLE -> vectorRes(R.drawable.ic_equip_cable)
    ProgramIcon.BENCH -> vectorRes(R.drawable.ic_equip_bench)
    ProgramIcon.PULLUP_BAR -> vectorRes(R.drawable.ic_equip_pullup_bar)
    ProgramIcon.DIP_STATION -> vectorRes(R.drawable.ic_equip_dip_station)
    ProgramIcon.RACK -> vectorRes(R.drawable.ic_equip_rack)
    ProgramIcon.BAND -> vectorRes(R.drawable.ic_equip_band)

    // Muscle-group drawings
    ProgramIcon.MUSCLE_CHEST -> vectorRes(R.drawable.ic_muscle_chest)
    ProgramIcon.MUSCLE_BACK -> vectorRes(R.drawable.ic_muscle_back)
    ProgramIcon.MUSCLE_SHOULDERS -> vectorRes(R.drawable.ic_muscle_shoulders)
    ProgramIcon.MUSCLE_BICEPS -> vectorRes(R.drawable.ic_muscle_biceps)
    ProgramIcon.MUSCLE_TRICEPS -> vectorRes(R.drawable.ic_muscle_triceps)
    ProgramIcon.MUSCLE_FOREARMS -> vectorRes(R.drawable.ic_muscle_forearms)
    ProgramIcon.MUSCLE_ABS -> vectorRes(R.drawable.ic_muscle_abs)
    ProgramIcon.MUSCLE_QUADS -> vectorRes(R.drawable.ic_muscle_quads)
    ProgramIcon.MUSCLE_HAMSTRINGS -> vectorRes(R.drawable.ic_muscle_hamstrings)
    ProgramIcon.MUSCLE_GLUTES -> vectorRes(R.drawable.ic_muscle_glutes)
    ProgramIcon.MUSCLE_CALVES -> vectorRes(R.drawable.ic_muscle_calves)
    ProgramIcon.MUSCLE_FULL_BODY -> vectorRes(R.drawable.ic_muscle_full_body)

    // Concepts Material already draws well
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
}

@Composable
private fun vectorRes(@DrawableRes id: Int): ImageVector = ImageVector.vectorResource(id)

/**
 * How muscle groups and equipment are rendered, chosen in Settings and provided
 * once at the app root so every list and detail view agrees without threading
 * [dev.hinny.skrot.data.prefs.Settings] through every call site.
 */
val LocalMuscleDisplay = compositionLocalOf { MetaDisplay.ICON }
val LocalEquipmentDisplay = compositionLocalOf { MetaDisplay.ICON }

/**
 * An exercise's muscle groups and equipment, rendered per the user's chosen
 * display modes. The primary muscle carries full contrast; secondary muscles
 * and equipment are recessive.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseMeta(e: Exercise, modifier: Modifier = Modifier) {
    val muscleMode = LocalMuscleDisplay.current
    val equipmentMode = LocalEquipmentDisplay.current
    val showMuscles = muscleMode != MetaDisplay.HIDDEN
    val showEquipment = equipmentMode != MetaDisplay.HIDDEN && e.equipment.isNotEmpty()
    if (!showMuscles && !showEquipment) return

    val primaryColor = MaterialTheme.colorScheme.onSurface
    val secondaryColor = MaterialTheme.colorScheme.onSurfaceVariant
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (showMuscles) {
            MetaItem(e.muscleGroup.vector(), muscleLabel(e.muscleGroup), muscleMode, primaryColor)
            e.secondaryMuscles.forEach {
                MetaItem(it.vector(), muscleLabel(it), muscleMode, secondaryColor)
            }
        }
        if (showEquipment) {
            if (showMuscles) {
                Text("·", style = MaterialTheme.typography.bodySmall, color = secondaryColor)
            }
            e.equipment.forEach {
                MetaItem(it.vector(), equipmentLabel(it), equipmentMode, secondaryColor)
            }
        }
    }
}

@Composable
private fun MetaItem(icon: ImageVector, label: String, mode: MetaDisplay, tint: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        if (mode != MetaDisplay.TEXT) {
            Icon(
                imageVector = icon,
                // In icon-only mode the icon has to carry the label for a11y.
                contentDescription = if (mode == MetaDisplay.ICON) label else null,
                tint = tint,
                modifier = Modifier.size(16.dp),
            )
        }
        if (mode != MetaDisplay.ICON) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = tint)
        }
    }
}

/**
 * Label content for a muscle-group or equipment filter chip. Chips follow the
 * display setting too, except that HIDDEN falls back to text — you cannot pick
 * a filter you cannot see.
 */
@Composable
fun MuscleChipLabel(m: MuscleGroup) =
    ChipLabel(m.vector(), muscleLabel(m), LocalMuscleDisplay.current)

@Composable
fun EquipmentChipLabel(e: Equipment) =
    ChipLabel(e.vector(), equipmentLabel(e), LocalEquipmentDisplay.current)

@Composable
private fun ChipLabel(icon: ImageVector, label: String, mode: MetaDisplay) {
    val effective = if (mode == MetaDisplay.HIDDEN) MetaDisplay.TEXT else mode
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (effective != MetaDisplay.TEXT) {
            Icon(
                imageVector = icon,
                contentDescription = if (effective == MetaDisplay.ICON) label else null,
                modifier = Modifier.size(18.dp),
            )
        }
        if (effective != MetaDisplay.ICON) Text(label)
    }
}
