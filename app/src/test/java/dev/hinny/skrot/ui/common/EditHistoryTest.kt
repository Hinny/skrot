package dev.hinny.skrot.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Apply/Cancel contract every editor now shares. These are the rules that
 * used to be restated in five view models, where they had already drifted.
 */
class EditHistoryTest {

    private fun history(confirm: Boolean = true) =
        EditHistory<String>().also { it.confirmEdits.value = confirm }

    @Test
    fun `nothing is pending before anything changes`() {
        val h = history()
        h.baselineIfUnset("a")
        h.refresh("a")
        assertFalse(h.hasPendingChanges.value)
        assertFalse(h.canUndo.value)
        assertFalse(h.canRedo.value)
    }

    @Test
    fun `drifting from the baseline is pending`() {
        val h = history()
        h.baselineIfUnset("a")
        h.refresh("b")
        assertTrue(h.hasPendingChanges.value)
    }

    @Test
    fun `apply makes the current state the new baseline`() {
        val h = history()
        h.baselineIfUnset("a")
        h.refresh("b")
        h.rebaseline("b")
        assertEquals("b", h.baseline)
        assertFalse(h.hasPendingChanges.value)
    }

    @Test
    fun `with confirmation off every change re-baselines and nothing is pending`() {
        val h = history(confirm = false)
        h.baselineIfUnset("a")
        h.refresh("b")
        assertEquals("b", h.baseline)
        assertFalse(h.hasPendingChanges.value)
    }

    @Test
    fun `the baseline is only taken once`() {
        val h = history()
        h.baselineIfUnset("a")
        h.baselineIfUnset("b")
        assertEquals("a", h.baseline)
    }

    @Test
    fun `undo returns the pushed state and redo returns the one stepped away from`() {
        val h = history()
        h.push("a")
        assertTrue(h.canUndo.value)
        assertFalse(h.canRedo.value)

        assertEquals("a", h.undo("b"))
        assertFalse(h.canUndo.value)
        assertTrue(h.canRedo.value)

        assertEquals("b", h.redo("a"))
        assertTrue(h.canUndo.value)
        assertFalse(h.canRedo.value)
    }

    @Test
    fun `undo and redo walk a multi-step history in order`() {
        val h = history()
        h.push("a")
        h.push("b")
        assertEquals("b", h.undo("c"))
        assertEquals("a", h.undo("b"))
        assertNull(h.undo("a"))
        assertEquals("b", h.redo("a"))
        assertEquals("c", h.redo("b"))
    }

    @Test
    fun `a new change abandons the redo branch`() {
        val h = history()
        h.push("a")
        h.undo("b")
        assertTrue(h.canRedo.value)
        h.push("a")
        assertFalse(h.canRedo.value)
    }

    @Test
    fun `undo and redo bump the revision so text fields adopt the rolled-back value`() {
        val h = history()
        h.push("a")
        val before = h.revision.value
        h.undo("b")
        assertEquals(before + 1, h.revision.value)
        h.redo("a")
        assertEquals(before + 2, h.revision.value)
    }

    @Test
    fun `a refused undo leaves the revision alone`() {
        val h = history()
        val before = h.revision.value
        assertNull(h.undo("a"))
        assertEquals(before, h.revision.value)
    }

    @Test
    fun `clearing the history leaves the baseline standing`() {
        val h = history()
        h.rebaseline("a")
        h.push("a")
        h.clearHistory()
        assertFalse(h.canUndo.value)
        assertFalse(h.canRedo.value)
        assertEquals("a", h.baseline)
    }
}
