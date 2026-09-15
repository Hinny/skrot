package dev.hinny.skrot.data.db

import dev.hinny.skrot.data.model.Exercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The instructions live in a separate file keyed by English name, so the two
 * catalogs can drift: an exercise renamed in one and not the other silently
 * loses its text. These tests are the seam.
 */
class SeedInstructionsTest {

    private val catalogNames = SeedData.exercises.map { it.nameEn }.toSet()

    @Test
    fun `every built-in exercise has instructions`() {
        val missing = catalogNames - SeedInstructions.byName.keys
        assertEquals("catalog exercises without instructions", emptySet<String>(), missing)
    }

    @Test
    fun `no instructions for exercises that are not in the catalog`() {
        val orphans = SeedInstructions.byName.keys - catalogNames
        assertEquals("instructions keyed by a name the catalog doesn't have", emptySet<String>(), orphans)
    }

    @Test
    fun `instructions and cues are filled in for both languages`() {
        SeedInstructions.byName.forEach { (name, text) ->
            assertTrue("$name: English instructions", text.en.isNotBlank())
            assertTrue("$name: Swedish instructions", text.sv.isNotBlank())
            assertTrue("$name: English cues", text.cuesEn.isNotEmpty())
            assertEquals("$name: same number of cues in both languages", text.cuesEn.size, text.cuesSv.size)
            assertTrue("$name: at most ${Exercise.MAX_CUES} cues", text.cuesEn.size <= Exercise.MAX_CUES)
            (text.cuesEn + text.cuesSv).forEach { cue ->
                assertTrue("$name: blank cue", cue.isNotBlank())
                assertEquals("$name: cue has stray whitespace: '$cue'", cue.trim(), cue)
            }
        }
    }
}
