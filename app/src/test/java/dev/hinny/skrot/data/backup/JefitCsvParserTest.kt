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
    fun `parses real multi-section export with kg detection from SETTING`() {
        val result = JefitCsvParser.parse(fixture("jefit_multisection.csv"))

        assertEquals(WeightUnit.KG, result.detectedUnit)
        assertEquals(2, result.sessions.size)

        val first = result.sessions.first()
        assertEquals(LocalDate.of(2026, 1, 5), first.date)
        // Stretch and Plank rows are timed encodings and must not become exercises
        assertEquals(listOf("Bench Press", "Deadlift", "Pull-Up"), first.exercises.map { it.name })
        assertEquals(3, first.exercises[0].sets.size)
        assertEquals(60.0, first.exercises[0].sets[0].loadKg, 0.0)
        assertEquals(8, first.exercises[0].sets[0].reps)
        // "90x0" is a placeholder and is dropped
        assertEquals(2, first.exercises[1].sets.size)
        // body-weight pull-ups (few entries, real reps) are kept
        assertEquals(3, first.exercises[2].sets.size)
        assertEquals(0.0, first.exercises[2].sets[0].loadKg, 0.0)
        assertEquals(5, first.exercises[2].sets[0].reps)

        val second = result.sessions[1]
        assertEquals(LocalDate.of(2026, 1, 7), second.date)
        assertEquals(listOf("Bench Press"), second.exercises.map { it.name })
        assertEquals(3 + 2 + 3 + 2, result.totalSets)

        // Body metrics: two dates (duplicate legacy row deduped, bad date skipped)
        assertEquals(2, result.bodyMetrics.size)
        val day1 = result.bodyMetrics[0]
        assertEquals(LocalDate.of(2026, 1, 2), day1.date)
        assertEquals(80.0, day1.weightKg!!, 0.0)
        val day2 = result.bodyMetrics[1]
        assertEquals(81.5, day2.weightKg!!, 0.0)
        assertEquals(100.0, day2.chestCm!!, 0.0)
        assertEquals(90.0, day2.waistCm!!, 0.0)
        assertEquals(95.0, day2.hipsCm!!, 0.0)
        assertEquals(60.0, day2.thighsCm!!, 0.0)

        // Skipped: garbage set, timed aggregate, no-sets aggregate, nameless aggregate, bad body date
        assertTrue(result.skipped.any { "garbage" in it })
        assertTrue(result.skipped.any { "timed" in it && it.startsWith("2") })
        assertTrue(result.skipped.any { "bad-date" in it })
    }

    @Test
    fun `parses ROUTINES into programs, days and supersets`() {
        val result = JefitCsvParser.parse(fixture("jefit_multisection.csv"))

        assertEquals(1, result.routines.size)
        val routine = result.routines.first()
        assertEquals("Test Routine", routine.name)
        // A blank line inside the quoted description must not split the record in two.
        assertEquals(2, routine.days.size)

        val day1 = routine.days[0]
        assertEquals("Day 1", day1.name)
        assertEquals(listOf("Bench Press", "Pull-Up", "Side Bend"), day1.exercises.map { it.name })
        assertEquals(3, day1.exercises[0].sets.size)
        assertEquals(60.0, day1.exercises[0].sets[0].loadKg, 0.0)
        assertEquals(8, day1.exercises[0].sets[0].reps)
        assertEquals(90, day1.exercises[0].restSec)

        // Pull-Up (the superset anchor) and Side Bend share a block; Bench Press stands alone.
        assertEquals(day1.exercises[1].supersetKey, day1.exercises[2].supersetKey)
        assertTrue(day1.exercises[0].supersetKey != day1.exercises[1].supersetKey)

        // Rest day carries no exercises but is still imported as an empty day.
        val day2 = routine.days[1]
        assertEquals("Rest Day", day2.name)
        assertTrue(day2.exercises.isEmpty())
    }

    @Test
    fun `blank line inside a quoted ROUTINES field does not corrupt chunk boundaries`() {
        val csv = """
            ### ROUTINES ####################
            row_id,USERID,TIMESTAMP,_id,name,difficulty,focus,dayaweek,description,daytype,tags,rdb_id,bannerCode,progression_flag
            1,42,"2026-01-01 10:00:00",1,"My Routine",0,0,2,"Paragraph one.

            Paragraph two.",0," ",0,,0

            row_id,USERID,TIMESTAMP,package,_id,name,day,dayIndex,interval_mode,rest_day,week,sort_order,day_completed_timestamp
            1,42,"2026-01-01 10:00:00",1,1,"Only Day",1,1,0,0,0,0,
            ##################################
        """.trimIndent()
        val result = JefitCsvParser.parse(csv)
        assertEquals(1, result.routines.size)
        assertEquals("My Routine", result.routines.single().name)
        assertEquals(listOf("Only Day"), result.routines.single().days.map { it.name })
    }

    @Test
    fun `multi-section export honours unit override for sets and body weight`() {
        val result = JefitCsvParser.parse(
            fixture("jefit_multisection.csv"),
            unitOverride = WeightUnit.LBS,
        )
        // 60 "lbs" -> 27.22 kg
        assertEquals(27.22, result.sessions.first().exercises[0].sets[0].loadKg, 0.01)
        // 80 "lbs" body weight -> 36.29 kg
        assertEquals(36.29, result.bodyMetrics.first().weightKg!!, 0.01)
    }

    @Test
    fun `multi-section export detects lbs from SETTING mass column`() {
        val csv = """
            ### SETTING ####################
            row_id,USERID,mass,length
            1,42," lbs"," inches"
            ################################
            ### EXERCISE LOGS ##############
            USERID,logs,mydate,ename
            42,"100x10",2026-01-05,"Bench Press"
            ################################
        """.trimIndent()
        val result = JefitCsvParser.parse(csv)
        assertEquals(WeightUnit.LBS, result.detectedUnit)
        // 100 lbs -> 45.36 kg
        assertEquals(45.36, result.sessions.single().exercises[0].sets[0].loadKg, 0.01)
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
