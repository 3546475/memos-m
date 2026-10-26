package org.example.moxia.ui.component.filter

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import org.example.moxia.model.filter.MemoDateRange
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalendarMonthTest {

    private val zone = ZoneId.of("Asia/Shanghai")

    @Test
    fun `monday first month starts mid week`() {
        // 2026-10-01 is a Thursday, so a Monday-first page pads three cells.
        val cells = calendarMonthCells(YearMonth.of(2026, 10), DayOfWeek.MONDAY)
        assertEquals(CalendarCellCount, cells.size)
        assertNull(cells[0])
        assertNull(cells[1])
        assertNull(cells[2])
        assertEquals(LocalDate.of(2026, 10, 1), cells[3])
        assertEquals(LocalDate.of(2026, 10, 31), cells[33])
        assertNull(cells[34])
    }

    @Test
    fun `sunday first month pads four cells`() {
        val cells = calendarMonthCells(YearMonth.of(2026, 10), DayOfWeek.SUNDAY)
        assertNull(cells[3])
        assertEquals(LocalDate.of(2026, 10, 1), cells[4])
        assertEquals(LocalDate.of(2026, 10, 31), cells[34])
    }

    @Test
    fun `month starting on the first weekday has no padding`() {
        // 2026-06-01 is a Monday.
        val cells = calendarMonthCells(YearMonth.of(2026, 6), DayOfWeek.MONDAY)
        assertEquals(LocalDate.of(2026, 6, 1), cells[0])
        assertEquals(LocalDate.of(2026, 6, 30), cells[29])
        assertNull(cells[30])
    }

    @Test
    fun `weekday order rotates from the first day of the week`() {
        assertEquals(
            listOf(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
            ),
            weekdayOrder(DayOfWeek.MONDAY)
        )
        assertEquals(
            listOf(
                DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
            ),
            weekdayOrder(DayOfWeek.SUNDAY)
        )
    }

    @Test
    fun `chinese weekday initials never collapse to xing`() {
        // The bug this dialog exists for: the desugared platform narrows every day to "星".
        val chinese = listOf("日", "一", "二", "三", "四", "五", "六")
        val mondayFirst = weekdayLabels(Locale.SIMPLIFIED_CHINESE, DayOfWeek.MONDAY, chinese)
        assertEquals(listOf("一", "二", "三", "四", "五", "六", "日"), mondayFirst)
        val sundayFirst = weekdayLabels(Locale.SIMPLIFIED_CHINESE, DayOfWeek.SUNDAY, chinese)
        assertEquals(chinese, sundayFirst)
        assertEquals(7, mondayFirst.toSet().size)
    }

    @Test
    fun `other languages keep their platform initials`() {
        val labels = weekdayLabels(Locale.US, DayOfWeek.SUNDAY, emptyList())
        assertEquals(listOf("S", "M", "T", "W", "T", "F", "S"), labels)
        assertEquals(
            listOf("M", "T", "W", "T", "F", "S", "S"),
            weekdayLabels(Locale.US, DayOfWeek.MONDAY, emptyList())
        )
    }

    @Test
    fun `range covers both ends`() {
        val range = localDatesToRange(
            LocalDate.of(2026, 10, 1),
            LocalDate.of(2026, 10, 25),
            zone
        )
        val (start, end) = range.toLocalDates(zone)
        assertEquals(LocalDate.of(2026, 10, 1), start)
        assertEquals(LocalDate.of(2026, 10, 25), end)
        assertEquals(
            LocalDate.of(2026, 10, 26).atStartOfDay(zone).toInstant().toEpochMilli(),
            range.endMillisExclusive
        )
    }

    @Test
    fun `reversed or single day ranges normalise`() {
        val reversed = localDatesToRange(
            LocalDate.of(2026, 10, 25),
            LocalDate.of(2026, 10, 1),
            zone
        )
        assertEquals(MemoDateRange(
            startMillis = LocalDate.of(2026, 10, 25).atStartOfDay(zone).toInstant().toEpochMilli(),
            endMillisExclusive = LocalDate.of(2026, 10, 26).atStartOfDay(zone)
                .toInstant().toEpochMilli()
        ), reversed)

        val single = localDatesToRange(
            LocalDate.of(2026, 10, 25),
            LocalDate.of(2026, 10, 25),
            zone
        )
        val (start, end) = single.toLocalDates(zone)
        assertEquals(start, end)
    }

    @Test
    fun `utc millis round trip through the search filter convention`() {
        val date = LocalDate.of(2026, 10, 25)
        assertEquals(date, utcMillisToLocalDate(localDateToUtcMillis(date)))
        assertEquals(1_792_886_400_000L, localDateToUtcMillis(date))
    }
}
