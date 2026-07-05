package dev.hinny.skrot.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import dev.hinny.skrot.R
import dev.hinny.skrot.data.model.Exercise
import dev.hinny.skrot.data.model.ProgramIcon
import java.util.Locale

/** Locale-aware display name: Swedish name when the UI language is Swedish. */
fun Exercise.displayName(): String =
    if (Locale.getDefault().language == "sv") nameSv else nameEn

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
}

/** "Today" / "yesterday" / "N days ago" / "never". */
@Composable
fun lastPerformedText(lastMs: Long?): String {
    if (lastMs == null) return stringResource(R.string.never_performed)
    val days = ((System.currentTimeMillis() - lastMs) / 86_400_000L).toInt()
    return when {
        days <= 0 -> stringResource(R.string.today)
        days == 1 -> stringResource(R.string.yesterday)
        else -> stringResource(R.string.days_ago, days)
    }
}

/** Numeric text field with +/- steppers; big touch targets for tired gym hands. */
@Composable
fun StepperNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    step: Double,
    label: String,
    modifier: Modifier = Modifier,
    integerOnly: Boolean = false,
    fieldWidth: Int = 92,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = {
            val current = value.replace(',', '.').toDoubleOrNull() ?: 0.0
            onValueChange(formatNumber(current - step, integerOnly))
        }) {
            Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.decrease))
        }
        OutlinedTextField(
            value = value,
            onValueChange = { text ->
                val filtered = text.filter { it.isDigit() || it == '.' || it == ',' || it == '-' }
                onValueChange(filtered)
            },
            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
            keyboardOptions = KeyboardOptions(
                keyboardType = if (integerOnly) KeyboardType.Number else KeyboardType.Decimal
            ),
            singleLine = true,
            modifier = Modifier.width(fieldWidth.dp),
        )
        IconButton(onClick = {
            val current = value.replace(',', '.').toDoubleOrNull() ?: 0.0
            onValueChange(formatNumber(current + step, integerOnly))
        }) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.increase))
        }
    }
}

fun formatNumber(value: Double, integerOnly: Boolean): String =
    if (integerOnly) value.toLong().toString()
    else dev.hinny.skrot.domain.Units.formatValue(value)

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String = stringResource(R.string.ok),
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
