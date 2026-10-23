package org.example.memosm.ui.component.filter

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import org.example.memosm.model.filter.MemoDateRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FilterDatePresetsTest {

    private val zone = ZoneId.of("Asia/Shanghai")
    private val mondayFirst = Locale.SIMPLIFIED_CHINESE
    // Saturday: the interesting day for both week conventions.
    private val today = LocalDate.of(2026, 10, 24)

    @Test
    fun `today covers the local day only`() {
        val range = FilterDatePreset.TODAY.range(today, zone, mondayFirst)!!
        val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
        assertEquals(start, range.startMillis)
        // Shanghai has no daylight saving, so the day is exactly 24h.
        assertEquals(start + 86_400_000L, range.endMillisExclusive)
    }

    @Test
    fun `week starts on the first day of the locale`() {
        val monday = FilterDatePreset.WEEK.range(today, zone, mondayFirst)!!
        assertEquals(LocalDate.of(2026, 10, 19), localDate(monday.startMillis))
        assertEquals(LocalDate.of(2026, 10, 26), localDate(monday.endMillisExclusive))

        val sunday = FilterDatePreset.WEEK.range(today, zone, Locale.US)!!
        assertEquals(LocalDate.of(2026, 10, 18), localDate(sunday.startMillis))
        assertEquals(LocalDate.of(2026, 10, 25), localDate(sunday.endMillisExclusive))
    }

    @Test
    fun `month covers the calendar month`() {
        val range = FilterDatePreset.MONTH.range(today, zone, mondayFirst)!!
        assertEquals(LocalDate.of(2026, 10, 1), localDate(range.startMillis))
        assertEquals(LocalDate.of(2026, 11, 1), localDate(range.endMillisExclusive))
    }

    @Test
    fun `any time has no window`() {
        assertNull(FilterDatePreset.ANY.range(today, zone, mondayFirst))
    }

    @Test
    fun `a matching window maps back to its preset and a custom one does not`() {
        val week = FilterDatePreset.WEEK.range(today, zone, mondayFirst)!!
        assertEquals(FilterDatePreset.WEEK, filterDatePresetOf(week, today, zone, mondayFirst))
        assertNull(
            filterDatePresetOf(
                MemoDateRange(week.startMillis + 1, week.endMillisExclusive),
                today,
                zone,
                mondayFirst
            )
        )
    }

    @Test
    fun `picker millis round trip as UTC midnights`() {
        assertEquals(today, pickerMillisToLocalDate(today.toPickerMillis()))
    }

    @Test
    fun `a closed range prints both ends and a single day prints once`() {
        val start = LocalDate.of(2026, 10, 1)
        val end = LocalDate.of(2026, 10, 3)
        val range = MemoDateRange(
            startMillis = start.atStartOfDay(zone).toInstant().toEpochMilli(),
            endMillisExclusive = end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        )
        assertEquals("Oct 1, 2026 – Oct 3, 2026", range.displayLabel(zone, Locale.US))

        val singleDay = MemoDateRange(
            startMillis = start.atStartOfDay(zone).toInstant().toEpochMilli(),
            endMillisExclusive = start.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        )
        assertEquals("Oct 1, 2026", singleDay.displayLabel(zone, Locale.US))
    }

    private fun localDate(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
}
