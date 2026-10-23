package org.example.memosm.ui.component.filter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import org.example.memosm.R
import org.example.memosm.model.Shortcut
import org.example.memosm.model.filter.MemoFilter
import org.example.memosm.model.filter.MemoSort
import org.example.memosm.model.filter.TagMatch
import org.example.memosm.model.filter.normalizeTag
import org.example.memosm.model.filter.toCel
import org.example.memosm.ui.theme.AppCheckbox
import org.example.memosm.ui.theme.AppDialog
import org.example.memosm.ui.theme.AppDivider
import org.example.memosm.ui.theme.AppFilterChip
import org.example.memosm.ui.theme.AppRadioButton
import org.example.memosm.ui.theme.AppTextButton
import org.example.memosm.ui.theme.AppTextField

/**
 * The structured filter panel: saved views, tags (AND/OR), a created-time window and the feed
 * order. It edits a draft and only commits on "apply", so toggling several conditions costs one
 * request instead of one per tap.
 */
@Composable
fun FilterPanel(
    show: Boolean,
    filter: MemoFilter,
    shortcuts: List<Shortcut>,
    tagCounts: Map<String, Int>,
    onDismiss: () -> Unit,
    onApply: (MemoFilter) -> Unit,
    onManageShortcuts: () -> Unit,
    onSaveAsView: (String) -> Unit,
    onEditShortcut: (Shortcut) -> Unit
) {
    // Reopening the panel starts from what is actually applied; closing throws the draft away.
    var draft by remember(show, filter) { mutableStateOf(filter) }
    var tagQuery by remember(show) { mutableStateOf("") }
    val today = remember(show) { LocalDate.now() }
    val zone = remember(show) { ZoneId.systemDefault() }
    val locale = remember(show) { Locale.getDefault() }

    val counts = remember(tagCounts) {
        tagCounts.entries.associate { (tag, count) -> normalizeTag(tag) to count }
    }
    val allTags = remember(counts, draft.tags) {
        (counts.keys + draft.tags)
            .filter { it.isNotBlank() }
            .distinct()
            .sortedWith(compareByDescending<String> { counts[it] ?: 0 }.thenBy { it })
    }
    val visibleTags = remember(allTags, tagQuery) {
        val needle = tagQuery.trim()
        if (needle.isEmpty()) allTags else allTags.filter { it.contains(needle, ignoreCase = true) }
    }
    val compiledCel = draft.toCel(null)
    val datePresets = remember(today, zone, locale) {
        FilterDatePreset.entries.associateWith { it.range(today, zone, locale) }
    }

    AppDialog(
        show = show,
        title = stringResource(R.string.memo_filter_title),
        onDismissRequest = onDismiss,
        buttons = {
            AppTextButton(
                text = stringResource(R.string.memo_filter_reset),
                modifier = Modifier.weight(1f),
                onClick = { draft = MemoFilter() }
            )
            AppTextButton(
                text = stringResource(R.string.memo_filter_apply, draft.activeCount),
                modifier = Modifier.weight(1f),
                onClick = { onApply(draft) }
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item(key = "views") {
                Column {
                    SectionLabel(stringResource(R.string.memo_filter_section_views))
                    ChoiceRow(
                        selected = !draft.hasLegacyClause,
                        label = stringResource(R.string.memo_filter_all),
                        multi = false,
                        onClick = { draft = draft.copy(viewName = null, legacyCel = emptyList()) }
                    )
                    shortcuts.forEach { shortcut ->
                        val cel = shortcut.filter?.trim().orEmpty()
                        ChoiceRow(
                            selected = draft.viewName != null && draft.viewName == shortcut.name,
                            label = shortcut.title.orEmpty(),
                            multi = false,
                            onClick = {
                                // A view is a preset: picking it replaces the base clause and any
                                // facets added on top of it. The sort is a preference, not part of
                                // the view, so it survives.
                                draft = if (cel.isEmpty()) {
                                    draft.copy(viewName = null, legacyCel = emptyList())
                                } else {
                                    MemoFilter(
                                        viewName = shortcut.name,
                                        legacyCel = listOf(cel),
                                        sort = draft.sort
                                    )
                                }
                            },
                            trailing = {
                                IconButton(onClick = { onEditShortcut(shortcut) }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = stringResource(R.string.memo_filter_edit_raw),
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        )
                    }
                    if (shortcuts.isEmpty()) {
                        Hint(stringResource(R.string.memo_filter_none))
                    }
                }
            }

            item(key = "tags-header") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.memo_filter_section_tags),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    AppFilterChip(
                        selected = draft.tagMatch == TagMatch.ALL,
                        onClick = { draft = draft.copy(tagMatch = TagMatch.ALL) }
                    ) {
                        Text(stringResource(R.string.memo_filter_tags_all))
                    }
                    Spacer(Modifier.width(8.dp))
                    AppFilterChip(
                        selected = draft.tagMatch == TagMatch.ANY,
                        onClick = { draft = draft.copy(tagMatch = TagMatch.ANY) }
                    ) {
                        Text(stringResource(R.string.memo_filter_tags_any))
                    }
                }
            }

            item(key = "tags-search") {
                AppTextField(
                    value = tagQuery,
                    onValueChange = { tagQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    placeholder = stringResource(R.string.memo_filter_tag_search),
                    singleLine = true
                )
            }

            if (visibleTags.isEmpty()) {
                item(key = "tags-empty") {
                    Hint(
                        if (allTags.isEmpty()) {
                            stringResource(R.string.memo_filter_no_tags)
                        } else {
                            stringResource(R.string.memo_filter_no_match)
                        }
                    )
                }
            } else {
                items(visibleTags, key = { "tag:$it" }) { tag ->
                    ChoiceRow(
                        selected = tag in draft.tags,
                        label = "#$tag",
                        multi = true,
                        onClick = {
                            draft = draft.copy(
                                tags = if (tag in draft.tags) draft.tags - tag else draft.tags + tag
                            )
                        },
                        trailing = {
                            counts[tag]?.takeIf { it > 0 }?.let { count ->
                                Text(
                                    text = count.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                }
            }

            item(key = "date") {
                Column {
                    SectionLabel(stringResource(R.string.memo_filter_section_date))
                    ChipRow {
                        FilterDatePreset.entries.forEach { preset ->
                            val range = datePresets[preset]
                            AppFilterChip(
                                selected = draft.date == range,
                                onClick = { draft = draft.copy(date = range) }
                            ) {
                                Text(preset.dateLabel())
                            }
                        }
                    }
                    val restored = draft.date
                    if (restored != null && filterDatePresetOf(restored, today, zone, locale) == null) {
                        Hint(stringResource(R.string.memo_filter_date_custom))
                    }
                }
            }

            item(key = "sort") {
                Column {
                    SectionLabel(stringResource(R.string.memo_filter_section_sort))
                    ChipRow {
                        MemoSort.entries.forEach { sort ->
                            AppFilterChip(
                                selected = draft.sort == sort,
                                onClick = { draft = draft.copy(sort = sort) }
                            ) {
                                Text(sort.sortLabel())
                            }
                        }
                    }
                }
            }

            item(key = "footer") {
                Column(Modifier.padding(top = 8.dp)) {
                    AppDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppTextButton(
                            text = stringResource(R.string.memo_filter_save_view),
                            enabled = compiledCel != null,
                            onClick = { if (compiledCel != null) onSaveAsView(compiledCel) }
                        )
                        Spacer(Modifier.weight(1f))
                        AppTextButton(
                            text = stringResource(R.string.memo_filter_manage),
                            onClick = onManageShortcuts
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MemoSort.sortLabel(): String = stringResource(
    when (this) {
        MemoSort.PINNED_DESC -> R.string.memo_filter_sort_pinned
        MemoSort.NEWEST -> R.string.memo_filter_sort_newest
        MemoSort.OLDEST -> R.string.memo_filter_sort_oldest
    }
)

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, top = 10.dp, bottom = 4.dp)
    )
}

@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

@Composable
private fun ChipRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

@Composable
private fun ChoiceRow(
    selected: Boolean,
    label: String,
    multi: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (multi) {
            AppCheckbox(checked = selected, onCheckedChange = { onClick() })
        } else {
            AppRadioButton(selected = selected, onClick = onClick)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}
