package dev.hinny.skrot.ui.common

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * The Apply/Cancel and undo/redo bookkeeping every editor in the app needs.
 *
 * The exercise, program, day, gym and finished-session editors all offer the
 * same bargain: with "confirm library edits" on, changes are provisional until
 * Apply and Cancel goes back to the last confirmed state; with it off, every
 * change stands immediately and no bar appears. Each editor used to carry its
 * own copy of the stacks, flags and counters that implement it — five copies
 * that had already drifted apart (the day editor never grew undo at all).
 *
 * What stays with each editor is what genuinely differs: **how a snapshot is
 * put back**. Two strategies are in use, and both are deliberate:
 *
 *  - The program, day and gym editors write each change straight to the
 *    database and restore a snapshot by writing it back — a REPLACE-insert
 *    that resurrects rows deleted during the session under their original ids.
 *    Editing a program is long and fiddly, and this way a crash mid-edit costs
 *    nothing.
 *  - The exercise and finished-session editors keep an in-memory draft and
 *    write only on Apply. Both edit a single object whose invalid intermediate
 *    states (a half-renamed exercise, a set with no reps yet) have no business
 *    reaching the database.
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

    /** Takes the first state the editor sees as the baseline, once. */
    fun baselineIfUnset(current: T?) {
        if (baseline == null) baseline = current
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
