package dev.hinny.skrot.domain

import dev.hinny.skrot.data.model.Routine
import dev.hinny.skrot.data.model.RoutineDay
import dev.hinny.skrot.data.model.ScheduleMode
import dev.hinny.skrot.data.model.SwapBehavior
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ScheduleEngineTest {

    private val days = listOf(
        RoutineDay(id = 1, routineId = 1, name = "A", position = 0),
        RoutineDay(id = 2, routineId = 1, name = "B", position = 1),
        RoutineDay(id = 3, routineId = 1, name = "C", position = 2),
    )

    private fun rotating(nextIndex: Int) = Routine(
        id = 1, name = "Test", scheduleMode = ScheduleMode.ROTATING, nextDayIndex = nextIndex,
    )

    private val monday = LocalDate.of(2026, 6, 29) // a Monday

    @Test
    fun `rotating schedule proposes the day at nextDayIndex`() {
        assertEquals(1L, ScheduleEngine.nextDay(rotating(0), days, monday)!!.id)
        assertEquals(2L, ScheduleEngine.nextDay(rotating(1), days, monday)!!.id)
        assertEquals(3L, ScheduleEngine.nextDay(rotating(2), days, monday)!!.id)
    }

    @Test
    fun `rotating index wraps around`() {
        assertEquals(1L, ScheduleEngine.nextDay(rotating(3), days, monday)!!.id)
    }

    @Test
    fun `completing the scheduled day advances the sequence`() {
        val index = ScheduleEngine.indexAfterCompletion(
            rotating(0), days, performedDayId = 1, swapBehavior = SwapBehavior.SKIPPED_STAYS_NEXT,
        )
        assertEquals(1, index)
    }

    @Test
    fun `completing the last day wraps back to the first`() {
        val index = ScheduleEngine.indexAfterCompletion(
            rotating(2), days, performedDayId = 3, swapBehavior = SwapBehavior.SKIPPED_STAYS_NEXT,
        )
        assertEquals(0, index)
    }

    @Test
    fun `swapping in another day keeps the skipped day next by default`() {
        // B is scheduled; user performs C instead
        val index = ScheduleEngine.indexAfterCompletion(
            rotating(1), days, performedDayId = 3, swapBehavior = SwapBehavior.SKIPPED_STAYS_NEXT,
        )
        assertEquals(1, index) // B stays next
    }

    @Test
    fun `swapping with ADVANCE continues after the day actually performed`() {
        // B is scheduled; user performs C; sequence continues after C -> A
        val index = ScheduleEngine.indexAfterCompletion(
            rotating(1), days, performedDayId = 3, swapBehavior = SwapBehavior.ADVANCE,
        )
        assertEquals(0, index)
    }

    @Test
    fun `performing a day from another routine leaves the sequence unchanged by default`() {
        val index = ScheduleEngine.indexAfterCompletion(
            rotating(1), days, performedDayId = 99, swapBehavior = SwapBehavior.SKIPPED_STAYS_NEXT,
        )
        assertEquals(1, index)
    }

    @Test
    fun `fixed weekday schedule picks today's day when assigned`() {
        val routine = Routine(id = 1, name = "T", scheduleMode = ScheduleMode.FIXED_WEEKDAYS)
        val weekdayDays = listOf(
            RoutineDay(id = 1, routineId = 1, name = "Legs", position = 0, weekdays = listOf(1)),
            RoutineDay(id = 2, routineId = 1, name = "Push", position = 1, weekdays = listOf(3)),
        )
        assertEquals(1L, ScheduleEngine.nextDay(routine, weekdayDays, monday)!!.id)
        val tuesday = monday.plusDays(1)
        // Nothing on Tuesday -> nearest upcoming is Wednesday's Push
        assertEquals(2L, ScheduleEngine.nextDay(routine, weekdayDays, tuesday)!!.id)
    }
}
