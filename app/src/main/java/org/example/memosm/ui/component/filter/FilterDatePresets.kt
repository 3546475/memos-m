package org.example.memosm.ui.component.filter

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale
import org.example.memosm.R
import org.example.memosm.model.filter.MemoDateRange

/** The date windows the filter panel offers. Custom ranges are a later phase. */
enum class FilterDatePreset { ANY, TODAY, WEEK, MONTH }

/**
 * The window of [this] preset. The week starts on the locale's first day (Monday in most of
 * Europe/Asia, Sunday in the US), so "this week" does not silently mean "since Monday" everywhere.
 */
fun FilterDatePreset.range(
    now: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault()
): MemoDateRange? = when (this) {
    FilterDatePreset.ANY -> null
    FilterDatePreset.TODAY -> rangeOf(now, now.plusDays(1), zone)
    FilterDatePreset.WEEK -> {
        val firstDay = WeekFields.of(locale).firstDayOfWeek
        val start = now.with(TemporalAdjusters.previousOrSame(firstDay))
        rangeOf(start, start.plusWeeks(1), zone)
    }
    FilterDatePreset.MONTH -> {
        val start = now.withDayOfMonth(1)
        rangeOf(start, start.plusMonths(1), zone)
    }
}

/** The preset whose window equals [range], or null for a restored/custom range. */
fun filterDatePresetOf(
    range: MemoDateRange,
    now: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault()
): FilterDatePreset? = FilterDatePreset.entries.firstOrNull { it.range(now, zone, locale) == range }

@Composable
fun FilterDatePreset.dateLabel(): String = stringResource(
    when (this) {
        FilterDatePreset.ANY -> R.string.memo_filter_date_any
        FilterDatePreset.TODAY -> R.string.memo_filter_date_today
        FilterDatePreset.WEEK -> R.string.memo_filter_date_week
        FilterDatePreset.MONTH -> R.string.memo_filter_date_month
    }
)

private fun rangeOf(start: LocalDate, endExclusive: LocalDate, zone: ZoneId): MemoDateRange =
    MemoDateRange(
        startMillis = start.atStartOfDay(zone).toInstant().toEpochMilli(),
        endMillisExclusive = endExclusive.atStartOfDay(zone).toInstant().toEpochMilli()
    )
