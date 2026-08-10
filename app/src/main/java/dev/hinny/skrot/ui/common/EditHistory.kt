package dev.hinny.skrot.ui.common

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * The Apply/Cancel and undo/redo bookkeeping every editor in the app needs.
 *
 * The exercise, program, day, gym and finished-session editors all offer the
 * same bargain: with "confirm library edits" on, changes are provisional until
 * Apply and Cancel goes back to the last confirmed state; with it off, every
 * change stands immediately and no bar appears.
 *
 * All five hold their edits as an **in-memory draft** and write only on Apply.
 * The database is the baseline, never the working copy. That has three
 * consequences worth stating, because the app used to do it the other way for
 * three of the five:
 *
 *  - Cancel cannot fail. It drops the draft; nothing was written, so there is
 *    nothing to put back.
 *  - Deletes cannot half-happen. A removal that cascades — a routine day taking
 *    its planned exercises with it, a planned exercise taking its per-gym
 *    overrides — only reaches the database once you have confirmed it. The old
 *    write-through editors deleted first and tried to reconstruct the fallout
 *    from a snapshot that structurally could not contain it, which lost data in
 *    both of those cases.
 *  - The rest of the app never sees a half-finished edit. A partly typed
 *    exercise name does not appear in every picker while you type it.
 *
 * The cost is that new rows need placeholder ids until Apply gives them real
 * ones. Each editor mints its own negative ids and reconciles them by diffing
 * the draft against the baseline when it writes.
 *
 * [T] is whatever the editor treats as one undoable state: an entity, a
 * relation object, or a pair of them.
 */
class EditHistory<T : Any> {

    /** Whether edits wait for Apply; mirrors `Settings.confirmLibraryEdits`. */
    val confirmEdits = MutableStateFlow(true)

    /** Whether the live state has drifted from [baseline]; drives the Apply/Cancel bar. */
    val hasPendingChanges = MutableStateFlow(false)

    val canUndo = MutableStateFlow(false)
    val canRedo = MutableStateFlow(false)

    /**
     * Bumped by undo and redo. Text fields keep their own state while you type,
     * so without this they would go on showing what you typed after the value
     * behind them was rolled back — undo appeared to do nothing to them.
     */
    val revision = MutableStateFlow(0)

    /** Last-confirmed state; only meaningful while [confirmEdits] is on. */
    var baseline: T? = null
        private set

    private val undoStack = ArrayDeque<T>()
    private val redoStack = ArrayDeque<T>()

    /**
     * Records the state as it was *before* a change, so [undo] can come back to
     * it. Call it before applying the change, not after.
     */
    fun push(before: T) {
        undoStack.addLast(before)
        redoStack.clear()
        updateFlags()
    }

    /**
     * @param current the state being stepped away from, banked for [redo]
     * @return the snapshot the caller should restore, or null with nothing to undo
     */
    fun undo(current: T): T? {
        val previous = undoStack.removeLastOrNull() ?: return null
        redoStack.addLast(current)
        revision.value++
        updateFlags()
        return previous
    }

    /** @return the snapshot the caller should restore, or null with nothing to redo. */
    fun redo(current: T): T? {
        val next = redoStack.removeLastOrNull() ?: return null
        undoStack.addLast(current)
        revision.value++
        updateFlags()
        return next
    }

    /** Makes [current] the state Cancel returns to. This is what Apply does. */
    fun rebaseline(current: T?) {
        baseline = current
        hasPendingChanges.value = false
    }

    /**
     * Recomputes [hasPendingChanges] against [current]. With confirmation off
     * every change re-baselines immediately, so the bar never appears.
     */
    fun refresh(current: T?) {
        if (!confirmEdits.value) rebaseline(current) else hasPendingChanges.value = current != baseline
    }

    /** Forgets the undo and redo stacks, leaving [baseline] alone. */
    fun clearHistory() {
        undoStack.clear()
        redoStack.clear()
        updateFlags()
    }

    private fun updateFlags() {
        canUndo.value = undoStack.isNotEmpty()
        canRedo.value = redoStack.isNotEmpty()
    }
}
