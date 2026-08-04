package dev.hinny.skrot.ui.common

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import dev.hinny.skrot.R
import kotlin.math.roundToInt

/**
 * Drag-to-reorder for a list of equal-height rows.
 *
 * The dragged row follows the finger and the rows it passes slide out of the
 * way, but nothing is written until the finger lifts: a single [onMove] on drop.
 * The previous implementation swapped neighbours mid-gesture, which meant every
 * swap re-emitted the list from the database and the gesture carried on against
 * a stale copy — the reason set reordering looked available but did nothing.
 *
 * Rows are assumed to be uniformly tall (they are, in every list this is used
 * for); the height of the row being dragged sets the step size.
 */
class ReorderState(private val onMove: (from: Int, to: Int) -> Unit) {
    var draggedIndex by mutableStateOf<Int?>(null)
        private set
    private var dragOffset by mutableFloatStateOf(0f)
    private var rowHeight by mutableIntStateOf(0)

    fun onRowHeight(height: Int) {
        if (height > 0) rowHeight = height
    }

    fun start(index: Int) {
        draggedIndex = index
        dragOffset = 0f
    }

    fun drag(delta: Float) {
        dragOffset += delta
    }

    fun cancel() {
        draggedIndex = null
        dragOffset = 0f
    }

    /** Where the dragged row would land if the finger lifted now. */
    fun targetIndex(count: Int): Int {
        val from = draggedIndex ?: return -1
        if (rowHeight <= 0) return from
        val steps = (dragOffset / rowHeight).roundToInt()
        return (from + steps).coerceIn(0, count - 1)
    }

    fun end(count: Int) {
        val from = draggedIndex ?: return
        val to = targetIndex(count)
        cancel()
        if (to != from) onMove(from, to)
    }

    /** Pixels [index] should be shifted by to show where the dragged row will land. */
    fun offsetFor(index: Int, count: Int): Int {
        val from = draggedIndex ?: return 0
        if (index == from) return dragOffset.roundToInt()
        val to = targetIndex(count)
        return when {
            from < to && index in (from + 1)..to -> -rowHeight
            to < from && index in to..(from - 1) -> rowHeight
            else -> 0
        }
    }
}

@Composable
fun rememberReorderState(onMove: (from: Int, to: Int) -> Unit): ReorderState =
    remember { ReorderState(onMove) }

/**
 * Applies the drag translation for [index] and reports the row height back to
 * [state]. Put this on the row itself, not on the handle.
 */
fun Modifier.reorderableRow(state: ReorderState, index: Int, count: Int): Modifier =
    this
        .zIndex(if (state.draggedIndex == index) 1f else 0f)
        .offset { IntOffset(0, state.offsetFor(index, count)) }
        .onSizeChanged { state.onRowHeight(it.height) }

/**
 * The grab handle. Dragging starts immediately — no long press — and consumes
 * the gesture so the surrounding list doesn't scroll along with it.
 */
@Composable
fun ReorderHandle(state: ReorderState, index: Int, count: Int) {
    val haptics = LocalHapticFeedback.current
    Icon(
        Icons.Filled.DragHandle,
        contentDescription = stringResource(R.string.reorder),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.pointerInput(index, count) {
            detectDragGestures(
                onDragStart = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    state.start(index)
                },
                onDragEnd = { state.end(count) },
                onDragCancel = { state.cancel() },
                onDrag = { change, amount ->
                    change.consume()
                    state.drag(amount.y)
                },
            )
        },
    )
}
