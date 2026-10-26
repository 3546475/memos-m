package org.example.moxia.ui.component.filter

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import org.example.moxia.model.filter.MemoDateRange

/**
 * The month grid the custom date picker draws.
 *
 * Material's own picker cannot be used here: its weekday row is formatted through
 * `DayOfWeek.getDisplayName(NARROW_STANDALONE)`, and with core library desugaring that call narrows
 * every Chinese weekday to the first character of 星期一 — seven "星" on a Chinese phone (verified
 * against the desugared classes the app actually ships). Owning the grid also lets the filter panel
 * pick a range in one dialog instead of two.
 */

/** Number of cells in a month page: six weeks, so the dialog keeps its height while paging. */
const val CalendarCellCount = 42

/**
 * [CalendarCellCount] cells of [month] starting on [firstDayOfWeek]; leading and trailing cells that
 * fall outside the month are null so the grid keeps a stable size.
 */
fun calendarMonthCells(month: YearMonth, firstDayOfWeek: DayOfWeek): List<LocalDate?> {
    val first = month.atDay(1)
    val leading = ((first.dayOfWeek.value - firstDayOfWeek.value) + 7) % 7
    val days = month.lengthOfMonth()
    return List(CalendarCellCount) { index ->
        val dayOfMonth = index - leading + 1
        if (dayOfMonth in 1..days) month.atDay(dayOfMonth) else null
    }
}

/** Weekday headers in display order: [DayOfWeek.NARROW] style, rotated to [firstDayOfWeek]. */
fun weekdayOrder(firstDayOfWeek: DayOfWeek): List<DayOfWeek> {
    val sundayFirst = listOf(
        DayOfWeek.SUNDAY,
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY
    )
    val offset = firstDayOfWeek.value % 7
    return sundayFirst.drop(offset) + sundayFirst.take(offset)
}

/**
 * Weekday initials in display order.
 *
 * Chinese is taken from [chineseWeekdays] (Sunday first) instead of the platform: with core library
 * desugaring `DayOfWeek.getDisplayName(NARROW_STANDALONE)` returns the *first* character of the wide
 * name for Chinese, so every day collapses to "星" — the exact row the old Material picker showed.
 * Every other language is fine on the platform.
 */
fun weekdayLabels(
    locale: java.util.Locale,
    firstDayOfWeek: DayOfWeek,
    chineseWeekdays: List<String>
): List<String> {
    val offset = firstDayOfWeek.value % 7
    val sundayFirst = if (locale.language == "zh") {
        chineseWeekdays
    } else {
        weekdayOrder(DayOfWeek.SUNDAY).map {
            it.getDisplayName(java.time.format.TextStyle.NARROW_STANDALONE, locale)
        }
    }
    return sundayFirst.drop(offset) + sundayFirst.take(offset)
}

/** The range a picker produced, as the local days it covers (end inclusive). */
fun MemoDateRange.toLocalDates(zone: ZoneId): Pair<LocalDate, LocalDate> {
    val start = Instant.ofEpochMilli(startMillis).atZone(zone).toLocalDate()
    val end = if (endMillisExclusive == MemoDateRange.UNBOUNDED) {
        start
    } else {
        Instant.ofEpochMilli(endMillisExclusive - 1).atZone(zone).toLocalDate()
    }
    return start to if (end.isBefore(start)) start else end
}

/** Builds the half-open range [start, end + 1 day) in [zone]. */
fun localDatesToRange(start: LocalDate, end: LocalDate, zone: ZoneId): MemoDateRange {
    val ordered = if (end.isBefore(start)) start to start else start to end
    return MemoDateRange(
        startMillis = ordered.first.atStartOfDay(zone).toInstant().toEpochMilli(),
        endMillisExclusive = ordered.second.plusDays(1).atStartOfDay(zone)
            .toInstant().toEpochMilli()
    )
}

/**
 * The search screen stores its date filter as the UTC midnight of the picked day (the convention
 * Material's pickers used), so the two directions stay explicit.
 */
fun utcMillisToLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

fun localDateToUtcMillis(date: LocalDate): Long =
    date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
