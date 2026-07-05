package dev.hinny.skrot.domain

import dev.hinny.skrot.data.model.Routine
import dev.hinny.skrot.data.model.RoutineDay
import dev.hinny.skrot.data.model.ScheduleMode
import dev.hinny.skrot.data.model.SwapBehavior
import java.time.LocalDate

object ScheduleEngine {

    /**
     * The workout day the schedule proposes next.
     *
     * ROTATING: the day at [Routine.nextDayIndex] in position order.
     * FIXED_WEEKDAYS: the day assigned to today's weekday, otherwise the day
     * with the nearest upcoming weekday assignment.
     */
    fun nextDay(routine: Routine, days: List<RoutineDay>, today: LocalDate): RoutineDay? {
        val sorted = days.sortedBy { it.position }
        if (sorted.isEmpty()) return null
        return when (routine.scheduleMode) {
            ScheduleMode.ROTATING -> sorted[routine.nextDayIndex.mod(sorted.size)]
            ScheduleMode.FIXED_WEEKDAYS -> {
                val assigned = sorted.filter { it.weekdays.isNotEmpty() }
                if (assigned.isEmpty()) return sorted.first()
                // offset 0 = today, 1 = tomorrow, ...
                (0..6).firstNotNullOfOrNull { offset ->
                    val weekday = today.plusDays(offset.toLong()).dayOfWeek.value
                    assigned.firstOrNull { weekday in it.weekdays }
                } ?: sorted.first()
            }
        }
    }

    /** Days until the next FIXED_WEEKDAYS occurrence of [day]; 0 = today. Null when unassigned. */
    fun daysUntil(day: RoutineDay, today: LocalDate): Int? {
        if (day.weekdays.isEmpty()) return null
        return (0..6).firstOrNull { offset ->
            today.plusDays(offset.toLong()).dayOfWeek.value in day.weekdays
        }
    }

    /**
     * The rotating-sequence index after completing [performedDayId].
     *
     * - Performing the scheduled day advances the sequence past it.
     * - Swapping in a different day either leaves the skipped day next
     *   (SKIPPED_STAYS_NEXT, default) or advances past the day actually
     *   performed (ADVANCE).
     */
    fun indexAfterCompletion(
        routine: Routine,
        days: List<RoutineDay>,
        performedDayId: Long?,
        swapBehavior: SwapBehavior,
    ): Int {
        val sorted = days.sortedBy { it.position }
        if (sorted.isEmpty()) return 0
        val scheduled = routine.nextDayIndex.mod(sorted.size)
        val performedIndex = sorted.indexOfFirst { it.id == performedDayId }
        return when {
            performedIndex == scheduled -> (scheduled + 1).mod(sorted.size)
            swapBehavior == SwapBehavior.ADVANCE ->
                if (performedIndex >= 0) (performedIndex + 1).mod(sorted.size)
                else (scheduled + 1).mod(sorted.size)

            else -> scheduled // skipped day stays next
        }
    }
}
