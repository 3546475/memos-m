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
import org.example.memosm.ui.theme.AppDivider
import org.example.memosm.ui.theme.AppDropdownMenu
import org.example.memosm.ui.theme.AppDropdownMenuItem

/**
 * The feed's permanent filter entry. It used to be a horizontally scrolling chip row inside the
 * list header, which scrolled away with the content and vanished entirely when the account had no
 * shortcuts. A chrome button plus a menu keeps the entry in one place, and the active state is a
 * dot on the icon instead of a chip the user has to scroll back to.
 *
 * The menu is the temporary home of the saved views until the structured filter panel lands: the
 * selection model is still the mutually exclusive `selectedShortcut` / `selectedHashtag` pair.
 */
@Composable
fun FeedFilterButton(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    active: Boolean,
    shortcuts: List<Shortcut>,
    selectedShortcut: Shortcut?,
    selectedHashtag: String?,
    onSelectShortcut: (Shortcut) -> Unit,
    onShowAll: () -> Unit,
    onRemoveHashtag: () -> Unit,
    onManageShortcuts: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        IconButton(onClick = { onExpandedChange(!expanded) }) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.FilterList,
                    contentDescription = stringResource(
                        if (active) R.string.memo_filter_active else R.string.memo_filter_title
                    ),
                    tint = if (active) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        LocalContentColor.current
                    }
                )
                if (active) {
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
                    onShowAll()
                },
                leadingIcon = {
                    SelectionSlot(selected = selectedShortcut == null && selectedHashtag == null)
                }
            )
            // A hashtag picked from a memo card is a filter too, but it lives outside the
            // shortcut list; without a row here the menu would show nothing selected.
            if (selectedHashtag != null) {
                AppDropdownMenuItem(
                    text = "#${selectedHashtag.removePrefix("#")}",
                    onClick = {
                        onExpandedChange(false)
                        onRemoveHashtag()
                    },
                    leadingIcon = { SelectionSlot(selected = true) }
                )
            }
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
                            onSelectShortcut(shortcut)
                        },
                        leadingIcon = {
                            SelectionSlot(
                                selected = shortcut.name != null &&
                                    shortcut.name == selectedShortcut?.name
                            )
                        }
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
