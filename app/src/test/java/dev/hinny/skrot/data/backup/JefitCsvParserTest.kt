package dev.hinny.skrot.data.backup

import dev.hinny.skrot.data.model.WeightUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class JefitCsvParserTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader!!.getResource(name)) { "missing fixture $name" }
            .readText()

    @Test
    fun `parses packed weight x reps layout with lbs detection`() {
        val result = JefitCsvParser.parse(fixture("jefit_packed_lbs.csv"))

        assertEquals(WeightUnit.LBS, result.detectedUnit)
        // 2024-03-01 and 2024-03-03; the two 03-05 rows are invalid and skipped
        assertEquals(2, result.sessions.size)

        val first = result.sessions.first()
        assertEquals(LocalDate.of(2024, 3, 1), first.date)
        assertEquals(listOf("Bench Press", "Lat Pulldown"), first.exercises.map { it.name })
        assertEquals(3, first.exercises[0].sets.size)

        // 135 lbs -> 61.23 kg
        assertEquals(61.23, first.exercises[0].sets[0].loadKg, 0.01)
        assertEquals(10, first.exercises[0].sets[0].reps)

        // skipped: bad date, blank name, garbage set entry + its empty exercise row
        assertEquals(4, result.skipped.size)
        assertEquals(3 + 2 + 3 + 1, result.totalSets)
    }

    @Test
    fun `parses one-row-per-set layout with kg detection`() {
        val result = JefitCsvParser.parse(fixture("jefit_rows_kg.csv"))

        assertEquals(WeightUnit.KG, result.detectedUnit)
        assertEquals(2, result.sessions.size)

        val first = result.sessions.first()
        // consecutive rows of the same exercise merge into one exercise with two sets
        assertEquals(2, first.exercises.size)
        assertEquals(2, first.exercises[0].sets.size)
        assertEquals(80.0, first.exercises[0].sets[0].loadKg, 0.0)
        assertEquals(8, first.exercises[0].sets[0].reps)
        assertTrue(result.skipped.isEmpty())
    }

    @Test
    fun `unit override forces conversion`() {
        val result = JefitCsvParser.parse(fixture("jefit_rows_kg.csv"), unitOverride = WeightUnit.LBS)
        // 80 "lbs" -> 36.29 kg
        assertEquals(36.29, result.sessions.first().exercises[0].sets[0].loadKg, 0.01)
    }

    @Test
    fun `unrecognized file reports a helpful skip reason`() {
        val result = JefitCsvParser.parse("foo,bar\n1,2\n")
        assertTrue(result.sessions.isEmpty())
        assertTrue(result.skipped.single().contains("Unrecognized"))
    }

    @Test
    fun `quote-aware csv reader handles quoted commas and escaped quotes`() {
        val rows = JefitCsvParser.readCsv("a,\"b,c\",\"d\"\"e\"\r\nf,g,h\n")
        assertEquals(listOf("a", "b,c", "d\"e"), rows[0])
        assertEquals(listOf("f", "g", "h"), rows[1])
    }
}
