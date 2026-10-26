package org.example.moxia.ui.component.filter

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale
import org.example.moxia.R
import org.example.moxia.ui.theme.AppButton
import org.example.moxia.ui.theme.AppDialog

/** The pager stays inside the range a memo can plausibly have. */
private val MinPickerMonth = YearMonth.of(1900, 1)
private val MaxPickerMonth = YearMonth.of(2100, 12)

/**
 * The app's own calendar dialog.
 *
 * Material's picker cannot draw a correct Chinese weekday row here — with core library desugaring
 * `DayOfWeek.getDisplayName(NARROW_STANDALONE)` collapses every Chinese weekday to the first
 * character of 星期一, i.e. a row of "星" (see [calendarMonthCells]). Owning the grid also lets the
 * filter panel choose a whole range in one dialog instead of chaining two pickers.
 *
 * [rangeSelection] = true picks a first and a last day; false replaces the selection with a single
 * day, which is what the advanced-search date fields need.
 */
@Composable
fun AppCalendarDialog(
    title: String,
    initialStart: LocalDate?,
    initialEnd: LocalDate?,
    rangeSelection: Boolean,
    zone: ZoneId,
    onDismissRequest: () -> Unit,
    onClear: () -> Unit,
    onConfirm: (start: LocalDate, end: LocalDate) -> Unit
) {
    val locale = LocalConfiguration.current.locales.let { locales ->
        if (locales.isEmpty) Locale.getDefault() else locales[0]
    }
    val firstDayOfWeek = remember(locale) { java.time.temporal.WeekFields.of(locale).firstDayOfWeek }
    val chineseWeekdays = stringArrayResource(R.array.date_weekday_labels).toList()
    val weekdays = remember(locale, firstDayOfWeek, chineseWeekdays) {
        weekdayLabels(locale, firstDayOfWeek, chineseWeekdays)
    }
    val dayFormat = remember(locale) {
        SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "MMMd"), locale)
    }
    val monthFormat = remember(locale) {
        SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "yMMMM"), locale)
    }
    val today = remember { LocalDate.now(zone) }

    var start by remember { mutableStateOf(initialStart) }
    var end by remember {
        mutableStateOf(if (rangeSelection) initialEnd else initialStart)
    }
    var month by remember { mutableStateOf(YearMonth.from(initialStart ?: today)) }

    val onDayTap: (LocalDate) -> Unit = { date ->
        if (!rangeSelection) {
            start = date
            end = date
        } else {
            val currentStart = start
            if (currentStart == null || end != null || date.isBefore(currentStart)) {
                // Beginning a fresh range (or restarting one from an earlier day).
                start = date
                end = null
            } else {
                end = date
            }
        }
    }

    AppDialog(
        show = true,
        title = title,
        onDismissRequest = onDismissRequest,
        buttons = {
            AppButton(
                onClick = onClear,
                modifier = Modifier.weight(1f),
                secondary = true,
                enabled = start != null
            ) {
                Text(stringResource(R.string.memo_filter_clear))
            }
            AppButton(
                onClick = { start?.let { picked -> onConfirm(picked, end ?: picked) } },
                modifier = Modifier.weight(2f),
                enabled = start != null
            ) {
                Text(stringResource(android.R.string.ok))
            }
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { month = month.minusMonths(1) },
                enabled = month > MinPickerMonth
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChevronLeft,
                    contentDescription = stringResource(R.string.memo_filter_date_previous_month),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = monthFormat.format(Date.from(month.atDay(1).toUtcInstant())),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { month = month.plusMonths(1) },
                enabled = month < MaxPickerMonth
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = stringResource(R.string.memo_filter_date_next_month),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            weekdays.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        val cells = remember(month, firstDayOfWeek) {
            calendarMonthCells(month, firstDayOfWeek)
        }
        val selectedStart = start
        val selectedEnd = end
        Column(modifier = Modifier.fillMaxWidth().padding(top = 2.dp)) {
            for (week in 0 until CalendarCellCount / 7) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (day in 0 until 7) {
                        val date = cells[week * 7 + day]
                        Box(
                            modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (date != null) {
                                CalendarDay(
                                    date = date,
                                    today = today,
                                    selected = date == selectedStart ||
                                        (rangeSelection && date == selectedEnd),
                                    inRange = rangeSelection && selectedStart != null &&
                                        selectedEnd != null &&
                                        date.isAfter(selectedStart) && date.isBefore(selectedEnd),
                                    onClick = { onDayTap(date) }
                                )
                            }
                        }
                    }
                }
            }
        }

        Text(
            text = when {
                selectedStart == null -> stringResource(
                    if (rangeSelection) {
                        R.string.memo_filter_date_pick_start
                    } else {
                        R.string.memo_filter_date_pick_single
                    }
                )
                selectedEnd == null -> stringResource(R.string.memo_filter_date_pick_end)
                else -> dayFormat.format(Date.from(selectedStart.toUtcInstant())) + " – " +
                    dayFormat.format(Date.from(selectedEnd.toUtcInstant()))
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CalendarDay(
    date: LocalDate,
    today: LocalDate,
    selected: Boolean,
    inRange: Boolean,
    onClick: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val background = when {
        selected -> accent
        inRange -> accent.copy(alpha = 0.12f)
        else -> Color.Transparent
    }
    val isToday = date == today
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(background)
            .then(
                if (isToday && !selected) {
                    Modifier.border(1.dp, accent.copy(alpha = 0.45f), CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

private fun LocalDate.toUtcInstant(): java.time.Instant =
    atStartOfDay(ZoneOffset.UTC).toInstant()
