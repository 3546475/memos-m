package org.example.memosm.ui.component.filter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.UnfoldLess
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import org.example.memosm.R
import org.example.memosm.model.MemoContentType
import org.example.memosm.model.Shortcut
import org.example.memosm.model.filter.MemoDateRange
import org.example.memosm.model.filter.MemoFilter
import org.example.memosm.model.filter.MemoSort
import org.example.memosm.model.filter.TagMatch
import org.example.memosm.model.filter.normalizeTag
import org.example.memosm.model.filter.toCel
import org.example.memosm.ui.component.TagNode
import org.example.memosm.ui.component.buildTagTree
import org.example.memosm.ui.component.collapsibleTagPaths
import org.example.memosm.ui.component.flattenTagTree
import org.example.memosm.ui.theme.AppDialog
import org.example.memosm.ui.theme.AppDivider
import org.example.memosm.ui.theme.AppFilterChip
import org.example.memosm.ui.theme.AppRadioButton
import org.example.memosm.ui.theme.AppTextButton
import org.example.memosm.ui.theme.AppTextField

/**
 * The structured filter panel: saved views, tags (AND/OR), the memo kind, a created-time window
 * and the feed order. It edits a draft and only commits on "apply", so toggling several
 * conditions costs one request instead of one per tap.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    // Custom range flow: start date first, then the end date.
    var showStartPicker by remember(show) { mutableStateOf(false) }
    var showEndPicker by remember(show) { mutableStateOf(false) }
    var pendingStart by remember(show) { mutableStateOf<LocalDate?>(null) }
    val today = remember(show) { LocalDate.now() }
    val zone = remember(show) { ZoneId.systemDefault() }
    val locale = remember(show) { Locale.getDefault() }

    val counts = remember(tagCounts) {
        tagCounts.entries.associate { (tag, count) -> normalizeTag(tag) to count }
    }
    // The tree is built from the known tags plus whatever is already selected, so a restored
    // filter never loses a row just because the stats snapshot has not seen that tag yet.
    val treeCounts = remember(counts, draft.tags) {
        counts.toMutableMap().apply {
            draft.tags.filter { it.isNotBlank() }.forEach { tag -> putIfAbsent(tag, 0) }
        }
    }
    val tagTree = remember(treeCounts) { buildTagTree(treeCounts) }
    var collapsedTags by remember(show) { mutableStateOf(emptySet<String>()) }
    val collapsibleTags = remember(tagTree) { collapsibleTagPaths(tagTree) }
    val filteredTagTree = remember(tagTree, tagQuery) { filterTagTree(tagTree, tagQuery) }
    val tagRows = remember(filteredTagTree, collapsedTags, tagQuery) {
        // While searching every surviving branch is expanded, so a hit is never hidden behind a
        // collapsed ancestor.
        flattenTagTree(
            filteredTagTree,
            if (tagQuery.isBlank()) collapsedTags else emptySet()
        )
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
                        onClick = { draft = draft.copy(viewName = null, legacyCel = emptyList()) }
                    )
                    shortcuts.forEach { shortcut ->
                        val cel = shortcut.filter?.trim().orEmpty()
                        ChoiceRow(
                            selected = draft.viewName != null && draft.viewName == shortcut.name,
                            label = shortcut.title.orEmpty(),
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
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppTextField(
                        value = tagQuery,
                        onValueChange = { tagQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = stringResource(R.string.memo_filter_tag_search),
                        singleLine = true
                    )
                    if (collapsibleTags.isNotEmpty()) {
                        val allCollapsed = collapsedTags.containsAll(collapsibleTags)
                        IconButton(
                            onClick = {
                                collapsedTags = if (allCollapsed) emptySet() else collapsibleTags
                            }
                        ) {
                            Icon(
                                imageVector = if (allCollapsed) {
                                    Icons.Outlined.UnfoldMore
                                } else {
                                    Icons.Outlined.UnfoldLess
                                },
                                contentDescription = stringResource(
                                    if (allCollapsed) R.string.tags_expand_all else R.string.tags_collapse_all
                                ),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (tagRows.isEmpty()) {
                item(key = "tags-empty") {
                    Hint(
                        if (treeCounts.isEmpty()) {
                            stringResource(R.string.memo_filter_no_tags)
                        } else {
                            stringResource(R.string.memo_filter_no_match)
                        }
                    )
                }
            } else {
                items(tagRows, key = { "tag:${it.path}" }) { node ->
                    TagTreeRow(
                        node = node,
                        selected = node.path in draft.tags,
                        descendantSelected = hasSelectedDescendant(draft.tags, node.path),
                        expanded = node.path !in collapsedTags,
                        onToggleSelect = {
                            draft = draft.copy(
                                tags = if (node.path in draft.tags) {
                                    draft.tags - node.path
                                } else {
                                    draft.tags + node.path
                                }
                            )
                        },
                        onToggleExpand = {
                            collapsedTags = if (node.path in collapsedTags) {
                                collapsedTags - node.path
                            } else {
                                collapsedTags + node.path
                            }
                        }
                    )
                }
            }

            item(key = "types") {
                Column {
                    SectionLabel(stringResource(R.string.memo_filter_section_types))
                    ChipRow {
                        MemoContentType.entries.forEach { type ->
                            AppFilterChip(
                                selected = type in draft.types,
                                onClick = {
                                    draft = draft.copy(
                                        types = if (type in draft.types) {
                                            draft.types - type
                                        } else {
                                            draft.types + type
                                        }
                                    )
                                }
                            ) {
                                Text(type.typeLabel())
                            }
                        }
                    }
                }
            }

            item(key = "date") {
                Column {
                    SectionLabel(stringResource(R.string.memo_filter_section_date))
                    val customRange = draft.date?.takeIf {
                        filterDatePresetOf(it, today, zone, locale) == null
                    }
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
                        AppFilterChip(
                            selected = customRange != null,
                            onClick = { showStartPicker = true }
                        ) {
                            Text(stringResource(R.string.memo_filter_date_custom))
                        }
                    }
                    if (customRange != null) {
                        Hint(customRange.displayLabel(zone, locale))
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

        // Material's date pickers are used for the custom range: the panel already runs inside an
        // AppDialog, and a nested dialog is far less code than a hand-rolled calendar. They read
        // the app's Material colour scheme, so MIUIX stays colour-consistent.
        if (showStartPicker) {
            val pickerState = rememberDatePickerState(
                initialSelectedDateMillis = draft.date?.let { range ->
                    Instant.ofEpochMilli(range.startMillis).atZone(zone).toLocalDate().toPickerMillis()
                }
            )
            DatePickerDialog(
                onDismissRequest = { showStartPicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val picked = pickerState.selectedDateMillis
                        showStartPicker = false
                        if (picked != null) {
                            pendingStart = pickerMillisToLocalDate(picked)
                            showEndPicker = true
                        }
                    }) { Text(stringResource(android.R.string.ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { showStartPicker = false }) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }
            ) {
                DatePicker(state = pickerState)
            }
        }

        if (showEndPicker) {
            val pickerState = rememberDatePickerState(
                initialSelectedDateMillis = pendingStart?.toPickerMillis()
            )
            DatePickerDialog(
                onDismissRequest = {
                    showEndPicker = false
                    pendingStart = null
                },
                confirmButton = {
                    TextButton(onClick = {
                        val start = pendingStart
                        val picked = pickerState.selectedDateMillis
                        if (start != null && picked != null) {
                            val endDate = pickerMillisToLocalDate(picked)
                                .let { if (it.isBefore(start)) start else it }
                            draft = draft.copy(
                                date = MemoDateRange(
                                    startMillis = start.atStartOfDay(zone).toInstant().toEpochMilli(),
                                    endMillisExclusive = endDate.plusDays(1).atStartOfDay(zone)
                                        .toInstant().toEpochMilli()
                                )
                            )
                        }
                        showEndPicker = false
                        pendingStart = null
                    }) { Text(stringResource(android.R.string.ok)) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showEndPicker = false
                        pendingStart = null
                    }) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }
            ) {
                DatePicker(state = pickerState)
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
private fun MemoContentType.typeLabel(): String = stringResource(
    when (this) {
        MemoContentType.LINK -> R.string.profile_stats_links
        MemoContentType.CODE -> R.string.profile_stats_code
        MemoContentType.TODO -> R.string.profile_stats_todo
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
        AppRadioButton(selected = selected, onClick = onClick)
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

private val TagTreeRowHeight = 44.dp
private val TagTreeIndent = 20.dp
private val TagTreeLabelStart = 12.dp
private val TagTreeGuideGap = 3.dp
private val TagTreeConnectorGap = 5.dp
private val TagTreeAccentWidth = 3.dp
private val TagTreeAccentHeight = 18.dp
private val TagTreeChevronSlot = 28.dp

/**
 * One row of the file-tree-style tag picker: no checkbox, the whole row toggles the tag, and the
 * fold arrow lives at the row end so the left gutter only carries the tree itself. The 1dp guides
 * hang under the parent's `#` (not under its arrow), and a child's horizontal connector stops a
 * few dp before its own `#`. A selected row gets an accent bar plus accent text, while a branch
 * whose subtree holds selections gets a dimmed bar so the partial state stays visible.
 */
@Composable
private fun TagTreeRow(
    node: TagNode,
    selected: Boolean,
    descendantSelected: Boolean,
    expanded: Boolean,
    onToggleSelect: () -> Unit,
    onToggleExpand: () -> Unit
) {
    val guideColor = MaterialTheme.colorScheme.outlineVariant
    val accent = MaterialTheme.colorScheme.primary
    val labelX = TagTreeLabelStart + TagTreeIndent * node.depth
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TagTreeRowHeight)
            .drawBehind {
                val stroke = 1.dp.toPx()
                val overshoot = 2.dp.toPx()
                val centerY = size.height / 2f
                val labelPx = labelX.toPx()
                // The guide sits at the parent's '#' edge, so it reads as the tag's own column.
                for (level in 0 until node.depth) {
                    val x = (TagTreeLabelStart + TagTreeIndent * level - TagTreeGuideGap).toPx()
                    drawLine(guideColor, Offset(x, -overshoot), Offset(x, size.height + overshoot), stroke)
                }
                if (node.depth > 0) {
                    val parentX =
                        (TagTreeLabelStart + TagTreeIndent * (node.depth - 1) - TagTreeGuideGap).toPx()
                    drawLine(
                        guideColor,
                        Offset(parentX, centerY),
                        Offset(labelPx - TagTreeConnectorGap.toPx(), centerY),
                        stroke
                    )
                }
                if (selected || descendantSelected) {
                    val barHeight = TagTreeAccentHeight.toPx()
                    drawRoundRect(
                        color = if (selected) accent else accent.copy(alpha = 0.35f),
                        topLeft = Offset(
                            labelPx - TagTreeConnectorGap.toPx(),
                            centerY - barHeight / 2f
                        ),
                        size = Size(TagTreeAccentWidth.toPx(), barHeight),
                        cornerRadius = CornerRadius(TagTreeAccentWidth.toPx() / 2f)
                    )
                }
            }
            .clip(RoundedCornerShape(8.dp))
            .selectable(
                selected = selected,
                role = Role.Checkbox,
                onClick = onToggleSelect
            )
            .padding(start = labelX, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "#${node.path}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        node.count.takeIf { it > 0 }?.let { count ->
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) {
                    accent.copy(alpha = 0.8f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        if (node.children.isNotEmpty()) {
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .width(TagTreeChevronSlot)
                    .fillMaxHeight()
                    .clickable(role = Role.Button, onClick = onToggleExpand),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = stringResource(
                        if (expanded) R.string.tags_collapse else R.string.tags_expand
                    ),
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(if (expanded) 90f else 0f),
                    tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
