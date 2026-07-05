package dev.hinny.skrot.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OneRepMaxTest {

    @Test
    fun `epley formula for a normal set`() {
        // 100 kg x 5 -> 100 * (1 + 5/30) = 116.67
        assertEquals(116.6667, OneRepMax.epley(100.0, 5)!!, 0.001)
    }

    @Test
    fun `single rep returns just above the weight`() {
        assertEquals(100.0 * (1 + 1 / 30.0), OneRepMax.epley(100.0, 1)!!, 0.0001)
    }

    @Test
    fun `reps above the cap are capped to avoid nonsense values`() {
        val at12 = OneRepMax.epley(60.0, 12)!!
        val at20 = OneRepMax.epley(60.0, 20)!!
        val at30 = OneRepMax.epley(60.0, 30)!!
        assertEquals(at12, at20, 0.0001)
        assertEquals(at12, at30, 0.0001)
        assertEquals(60.0 * (1 + 12 / 30.0), at12, 0.0001)
    }

    @Test
    fun `no estimate for zero or negative weight`() {
        assertNull(OneRepMax.epley(0.0, 5))
        assertNull(OneRepMax.epley(-10.0, 5))
    }

    @Test
    fun `no estimate for zero reps`() {
        assertNull(OneRepMax.epley(100.0, 0))
    }
}
