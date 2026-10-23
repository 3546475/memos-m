package org.example.memosm.ui.component.filter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.example.memosm.R
import org.example.memosm.model.Shortcut
import org.example.memosm.model.filter.MemoFilter
import org.example.memosm.model.filter.normalizeTag
import org.example.memosm.ui.theme.AppDivider
import org.example.memosm.ui.theme.AppDropdownMenu
import org.example.memosm.ui.theme.AppDropdownMenuItem

/**
 * The feed's permanent filter entry. It replaces the old horizontally scrolling chip row, which
 * scrolled away with the content and vanished entirely when the account had no shortcuts.
 *
 * The menu edits the structured [MemoFilter]: a saved view is a preset (picking one replaces the
 * base clause), while tags toggle on top of it. Tags picked from a memo card show up here too, so
 * the menu never claims "all memos" while a tag is applied.
 */
@Composable
fun FeedFilterButton(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    filter: MemoFilter,
    shortcuts: List<Shortcut>,
    onSelectView: (Shortcut) -> Unit,
    onClear: () -> Unit,
    onToggleTag: (String) -> Unit,
    onManageShortcuts: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        IconButton(onClick = { onExpandedChange(!expanded) }) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.FilterList,
                    contentDescription = stringResource(
                        if (filter.isActive) R.string.memo_filter_active else R.string.memo_filter_title
                    ),
                    tint = if (filter.isActive) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        LocalContentColor.current
                    }
                )
                if (filter.isActive) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 3.dp, y = (-3).dp)
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }

        AppDropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            AppDropdownMenuItem(
                text = stringResource(R.string.memo_filter_all),
                onClick = {
                    onExpandedChange(false)
                    onClear()
                },
                leadingIcon = { SelectionSlot(selected = !filter.isActive) }
            )
            if (shortcuts.isEmpty()) {
                AppDropdownMenuItem(
                    text = stringResource(R.string.memo_filter_none),
                    onClick = {},
                    enabled = false
                )
            } else {
                shortcuts.forEach { shortcut ->
                    AppDropdownMenuItem(
                        text = shortcut.title.orEmpty(),
                        onClick = {
                            onExpandedChange(false)
                            onSelectView(shortcut)
                        },
                        leadingIcon = {
                            SelectionSlot(
                                selected = shortcut.name != null && shortcut.name == filter.viewName
                            )
                        }
                    )
                }
            }
            if (filter.tags.isNotEmpty()) {
                AppDivider()
                filter.tags.sorted().forEach { tag ->
                    AppDropdownMenuItem(
                        text = "#${normalizeTag(tag)}",
                        onClick = {
                            onExpandedChange(false)
                            onToggleTag(tag)
                        },
                        leadingIcon = { SelectionSlot(selected = true) }
                    )
                }
            }
            AppDivider()
            AppDropdownMenuItem(
                text = stringResource(R.string.memo_filter_manage),
                onClick = {
                    onExpandedChange(false)
                    onManageShortcuts()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Bookmarks,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
        }
    }
}

/** A fixed-width check column: unselected rows keep the label aligned with the selected one. */
@Composable
private fun SelectionSlot(selected: Boolean) {
    Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
        if (selected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
