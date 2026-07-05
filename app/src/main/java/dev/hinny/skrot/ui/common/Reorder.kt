package dev.hinny.skrot.ui.common

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.hinny.skrot.R

/**
 * Drag handle for list reordering: long-press and drag; each time the pointer
 * travels one row height the item swaps with its neighbor via [onMove].
 */
@Composable
fun DragHandle(onMove: (delta: Int) -> Unit, rowHeightDp: Float = 56f) {
    val rowHeightPx = with(LocalDensity.current) { rowHeightDp.dp.toPx() }
    val acc = remember { mutableFloatStateOf(0f) }
    Icon(
        Icons.Filled.DragHandle,
        contentDescription = stringResource(R.string.reorder),
        modifier = Modifier.pointerInput(Unit) {
            detectDragGesturesAfterLongPress(
                onDragStart = { acc.floatValue = 0f },
                onDrag = { change, dragAmount ->
                    change.consume()
                    acc.floatValue += dragAmount.y
                    val threshold = rowHeightPx * 0.7f
                    while (acc.floatValue > threshold) {
                        onMove(1)
                        acc.floatValue -= rowHeightPx
                    }
                    while (acc.floatValue < -threshold) {
                        onMove(-1)
                        acc.floatValue += rowHeightPx
                    }
                },
            )
        },
    )
}
