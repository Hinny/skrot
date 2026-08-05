package dev.hinny.skrot.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields

/**
 * Consecutive training weeks, counting back from today. A week counts once it
 * holds at least [minPerWeek] sessions.
 *
 * The current week is a special case: it is still in progress, so falling short
 * of the quota today does not break a streak that is otherwise intact — it just
 * doesn't extend it yet.
 */
object StreakCalculator {

    private val weekFields = WeekFields.ISO

    fun weeks(
        sessionDates: List<Long>,
        minPerWeek: Int,
        zone: ZoneId = ZoneId.systemDefault(),
        today: LocalDate = LocalDate.now(zone),
    ): Int {
        if (sessionDates.isEmpty()) return 0
        val quota = minPerWeek.coerceAtLeast(1)
        val counts = sessionDates
            .map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate().weekKey() }
            .groupingBy { it }
            .eachCount()

        var streak = 0
        var cursor = today
        // An unfinished current week that hasn't hit the quota is skipped rather
        // than counted or treated as a break.
        if ((counts[cursor.weekKey()] ?: 0) < quota) cursor = cursor.minusWeeks(1)

        while ((counts[cursor.weekKey()] ?: 0) >= quota) {
            streak++
            cursor = cursor.minusWeeks(1)
        }
        return streak
    }

    private fun LocalDate.weekKey(): Int =
        get(weekFields.weekBasedYear()) * 100 + get(weekFields.weekOfWeekBasedYear())
}
