package org.example.memosm.ui.component.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.example.memosm.R
import org.example.memosm.data.DataStoreManager
import org.example.memosm.model.AppearancePreferences
import org.example.memosm.model.NavBarScrollMode
import org.example.memosm.model.TopBarScrollMode
import org.example.memosm.ui.MainDestination
import org.example.memosm.ui.theme.AppDialog
import org.example.memosm.ui.theme.AppListItem
import org.example.memosm.ui.theme.AppSwitch
import org.example.memosm.ui.theme.AppTextButton
import org.koin.core.context.GlobalContext

/**
 * Navigation preferences: how the bars react to scrolling, and which bottom-bar destinations are
 * shown in which order. Destination names are stored as MainDestination enum names so the list
 * survives enum additions, and an empty list means "everything in the default order".
 */
@Composable
fun NavigationSettingsCard(mapSupported: Boolean) {
    val dataStore = remember { GlobalContext.get().get<DataStoreManager>() }
    val appearance by dataStore.appearance.collectAsState(initial = AppearancePreferences())
    val scope = rememberCoroutineScope()

    val available = remember(mapSupported) {
        MainDestination.entries.filter { it != MainDestination.MAP || mapSupported }
    }
    val enabledOrder = remember(appearance.navDestinations, available) {
        enabledDestinations(appearance.navDestinations, available)
    }

    // settingsItem hosts its content in a Box, so the card stacks its groups in an explicit column
    // instead of overlapping them.
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsGroup {
            SettingsOptionRow(
                title = stringResource(R.string.settings_nav_bar_scroll),
                summary = stringResource(
                    when (appearance.navBarScrollMode) {
                        NavBarScrollMode.HIDE_ON_SCROLL -> R.string.settings_nav_bar_scroll_hide
                        NavBarScrollMode.ALWAYS_VISIBLE -> R.string.settings_nav_bar_scroll_always
                    }
                ),
                options = listOf(
                    stringResource(R.string.settings_nav_bar_scroll_hide),
                    stringResource(R.string.settings_nav_bar_scroll_always)
                ),
                selectedIndex = if (appearance.navBarScrollMode == NavBarScrollMode.HIDE_ON_SCROLL) 0 else 1,
                onSelect = { index ->
                    scope.launch {
                        dataStore.saveNavBarScrollMode(
                            if (index == 0) NavBarScrollMode.HIDE_ON_SCROLL else NavBarScrollMode.ALWAYS_VISIBLE
                        )
                    }
                }
            )
            SettingsOptionRow(
                title = stringResource(R.string.settings_top_bar_scroll),
                summary = stringResource(
                    when (appearance.topBarScrollMode) {
                        TopBarScrollMode.NEVER -> R.string.settings_top_bar_scroll_never
                        TopBarScrollMode.SEARCH_ONLY -> R.string.settings_top_bar_scroll_search
                        TopBarScrollMode.ALL -> R.string.settings_top_bar_scroll_all
                    }
                ),
                options = listOf(
                    stringResource(R.string.settings_top_bar_scroll_never),
                    stringResource(R.string.settings_top_bar_scroll_search),
                    stringResource(R.string.settings_top_bar_scroll_all)
                ),
                selectedIndex = appearance.topBarScrollMode.ordinal,
                onSelect = { index ->
                    scope.launch {
                        dataStore.saveTopBarScrollMode(TopBarScrollMode.entries[index])
                    }
                }
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SettingsLabel(stringResource(R.string.settings_nav_items))
            SettingsGroup {
                available.forEach { destination ->
                    val enabled = destination in enabledOrder
                    val position = enabledOrder.indexOf(destination)
                    AppListItem(
                        title = stringResource(destination.labelRes),
                        modifier = Modifier.clip(RoundedCornerShape(4.dp)),
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (enabled) {
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                dataStore.saveNavDestinations(
                                                    moveDestination(enabledOrder, position, -1)
                                                )
                                            }
                                        },
                                        enabled = position > 0
                                    ) {
                                        Icon(
                                            Icons.Outlined.ArrowUpward,
                                            contentDescription = stringResource(R.string.settings_nav_move_up)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                dataStore.saveNavDestinations(
                                                    moveDestination(enabledOrder, position, 1)
                                                )
                                            }
                                        },
                                        enabled = position in 0 until enabledOrder.lastIndex
                                    ) {
                                        Icon(
                                            Icons.Outlined.ArrowDownward,
                                            contentDescription = stringResource(R.string.settings_nav_move_down)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(4.dp))
                                AppSwitch(
                                    checked = enabled,
                                    onCheckedChange = { checked ->
                                        scope.launch {
                                            dataStore.saveNavDestinations(
                                                toggleDestination(enabledOrder, available, destination, checked)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

/** Everything in the default order when nothing is configured. */
private fun enabledDestinations(
    stored: List<String>,
    available: List<MainDestination>
): List<MainDestination> {
    if (stored.isEmpty()) return available
    val ordered = stored.mapNotNull { name -> available.firstOrNull { it.name == name } }
    // Keep destinations added by a newer build reachable even if the stored list predates them.
    return ordered + available.filterNot { it in ordered }
}

private fun toggleDestination(
    enabledOrder: List<MainDestination>,
    available: List<MainDestination>,
    destination: MainDestination,
    checked: Boolean
): List<String> {
    val current = enabledOrder.toMutableList()
    if (checked) {
        if (destination !in current) current.add(destination)
    } else {
        current.remove(destination)
    }
    // An empty list means "show everything", so disabling must leave at least one destination.
    return if (current.isEmpty()) {
        listOfNotNull(available.firstOrNull()).map { it.name }
    } else {
        current.map { it.name }
    }
}

private fun moveDestination(
    enabledOrder: List<MainDestination>,
    position: Int,
    delta: Int
): List<String> {
    val target = position + delta
    if (position !in enabledOrder.indices || target !in enabledOrder.indices) {
        return enabledOrder.map { it.name }
    }
    val moved = enabledOrder.toMutableList()
    val item = moved.removeAt(position)
    moved.add(target, item)
    return moved.map { it.name }
}

/** A row that shows the current value and opens a picker for a small fixed set of options. */
@Composable
private fun SettingsOptionRow(
    title: String,
    summary: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }

    AppListItem(
        title = title,
        summary = summary,
        modifier = Modifier.clip(RoundedCornerShape(4.dp)),
        trailing = { Icon(Icons.Outlined.ChevronRight, contentDescription = null) },
        onClick = { showDialog = true }
    )

    AppDialog(
        show = showDialog,
        title = title,
        onDismissRequest = { showDialog = false }
    ) {
        options.forEachIndexed { index, option ->
            AppListItem(
                title = option,
                modifier = Modifier.fillMaxWidth(),
                containerColor = if (index == selectedIndex) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                onClick = {
                    onSelect(index)
                    showDialog = false
                }
            )
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            AppTextButton(
                text = stringResource(R.string.common_cancel),
                modifier = Modifier.weight(1f),
                onClick = { showDialog = false }
            )
        }
    }
}
