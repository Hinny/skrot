package dev.hinny.skrot.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class StreakCalculatorTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private val today: LocalDate = LocalDate.of(2026, 8, 5) // a Wednesday

    private fun daysAgo(vararg days: Long): List<Long> = days.map {
        today.minusDays(it).atStartOfDay(zone).toInstant().toEpochMilli()
    }

    @Test
    fun `no sessions is no streak`() {
        assertEquals(0, StreakCalculator.weeks(emptyList(), 1, zone, today))
    }

    @Test
    fun `three consecutive weeks count`() {
        val dates = daysAgo(0, 7, 14)
        assertEquals(3, StreakCalculator.weeks(dates, 1, zone, today))
    }

    @Test
    fun `a missed week ends the streak`() {
        // This week and last week, then nothing until four weeks back.
        val dates = daysAgo(0, 7, 28)
        assertEquals(2, StreakCalculator.weeks(dates, 1, zone, today))
    }

    @Test
    fun `an empty current week does not break an otherwise intact streak`() {
        val dates = daysAgo(7, 14)
        assertEquals(2, StreakCalculator.weeks(dates, 1, zone, today))
    }

    @Test
    fun `weeks below the quota do not count`() {
        // One session in each of the last three weeks, quota of two.
        val dates = daysAgo(0, 7, 14)
        assertEquals(0, StreakCalculator.weeks(dates, 2, zone, today))
    }

    @Test
    fun `quota of two is met by two sessions in a week`() {
        val dates = daysAgo(0, 1, 7, 8)
        assertEquals(2, StreakCalculator.weeks(dates, 2, zone, today))
    }

    @Test
    fun `a short current week is skipped rather than counted`() {
        // One session this week against a quota of two, two in each week before.
        val dates = daysAgo(0, 7, 8, 14, 15)
        assertEquals(2, StreakCalculator.weeks(dates, 2, zone, today))
    }
}
