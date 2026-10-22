package org.example.memosm.ui.component.setting

import android.os.Build
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import org.example.memosm.model.DefaultNavDestinations
import org.example.memosm.model.NavBarScrollMode
import org.example.memosm.model.ThemeStyle
import org.example.memosm.model.TopBarScrollMode
import org.example.memosm.ui.MainDestination
import org.example.memosm.ui.theme.AppDialog
import org.example.memosm.ui.theme.AppListItem
import org.example.memosm.ui.theme.AppSwitch
import org.example.memosm.ui.theme.AppTextButton
import org.example.memosm.ui.theme.LocalThemeStyle
import org.koin.core.context.GlobalContext

/**
 * Navigation preferences, grouped by the object they change: the bottom bar (shape, scrolling,
 * frosting, items), then the top bar, then one reset action at the very bottom.
 *
 * Two things are deliberate. The bar shape is a two-segment control instead of a switch, because
 * "floating" is a choice between two shapes rather than an on/off. And the frosting switches are
 * disabled with a reason when the effect cannot run at all (Miuix styling on Android 13+), instead
 * of silently doing nothing.
 *
 * Destination names are stored as `MainDestination` enum names so the list survives enum additions;
 * an empty list means "everything in the default order".
 */
@Composable
fun NavigationSettingsCard(mapSupported: Boolean) {
    val dataStore = remember { GlobalContext.get().get<DataStoreManager>() }
    val appearance by dataStore.appearance.collectAsState(initial = AppearancePreferences())
    val scope = rememberCoroutineScope()

    val available = remember(mapSupported) {
        MainDestination.entries.filter { it != MainDestination.MAP || mapSupported }
    }
    // Two distinct concepts: which destinations are enabled (and in what order), and the order the
    // rows are listed in the editor (enabled first, then the disabled ones).
    val enabledOrder = remember(appearance.navDestinations, available) {
        enabledDestinations(appearance.navDestinations, available)
    }
    val enabledNames = enabledOrder.map { it.name }.toSet()
    val rowOrder = remember(enabledOrder, available) { destinationRows(enabledOrder, available) }

    var editingItems by rememberSaveable { mutableStateOf(false) }
    var confirmingReset by rememberSaveable { mutableStateOf(false) }

    val blurUnavailable = when {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ->
            stringResource(R.string.settings_blur_needs_android13)
        LocalThemeStyle.current != ThemeStyle.MIUIX ->
            stringResource(R.string.settings_blur_needs_miuix)
        else -> null
    }

    // settingsItem hosts its content in a Box, so the card stacks its groups in an explicit column
    // instead of overlapping them.
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SettingsLabel(stringResource(R.string.settings_nav_section_bottom))
            SettingsGroup {
                SettingSegmentRow(
                    label = stringResource(R.string.settings_nav_style),
                    options = listOf(
                        false to stringResource(R.string.settings_nav_style_docked),
                        true to stringResource(R.string.settings_nav_style_floating)
                    ),
                    selected = appearance.floatingNavBar,
                    onSelect = { floating ->
                        scope.launch { dataStore.saveFloatingNavBar(floating) }
                    }
                )
                SettingToggleRow(
                    label = stringResource(R.string.settings_nav_bar_hide_on_scroll),
                    checked = appearance.navBarScrollMode == NavBarScrollMode.HIDE_ON_SCROLL,
                    onCheckedChange = { hide ->
                        scope.launch {
                            dataStore.saveNavBarScrollMode(
                                if (hide) NavBarScrollMode.HIDE_ON_SCROLL
                                else NavBarScrollMode.ALWAYS_VISIBLE
                            )
                        }
                    }
                )
                SettingToggleRow(
                    label = stringResource(R.string.settings_nav_bar_blur),
                    description = blurUnavailable,
                    enabled = blurUnavailable == null,
                    checked = appearance.bottomBarBlur,
                    onCheckedChange = { on ->
                        scope.launch { dataStore.saveBottomBarBlur(on) }
                    }
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.settings_nav_items),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.weight(1f))
                // The arrows are noise when nobody is reordering, so they live behind this toggle.
                Text(
                    text = stringResource(
                        if (editingItems) R.string.common_done else R.string.settings_nav_items_edit
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { editingItems = !editingItems }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            SettingsGroup {
                rowOrder.forEach { destination ->
                    val enabled = destination.name in enabledNames
                    val position = enabledOrder.indexOf(destination)
                    AppListItem(
                        title = stringResource(destination.labelRes),
                        modifier = Modifier.clip(RoundedCornerShape(4.dp)),
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (editingItems && enabled) {
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                dataStore.saveNavDestinations(
                                                    moveDestination(enabledOrder, position, -1)
                                                        .map { it.name }
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
                                                        .map { it.name }
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
                                                setDestinationEnabled(enabledOrder, destination, checked)
                                                    .map { it.name }
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    )
                }
            }
            if (editingItems) {
                Text(
                    text = stringResource(R.string.settings_nav_items_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 2.dp)
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SettingsLabel(stringResource(R.string.settings_nav_section_top))
            SettingsGroup {
                SettingToggleRow(
                    label = stringResource(R.string.settings_top_bar_hide_on_scroll),
                    checked = appearance.topBarScrollMode == TopBarScrollMode.ON_SCROLL,
                    onCheckedChange = { hide ->
                        scope.launch {
                            dataStore.saveTopBarScrollMode(
                                if (hide) TopBarScrollMode.ON_SCROLL else TopBarScrollMode.ALWAYS
                            )
                        }
                    }
                )
                SettingToggleRow(
                    label = stringResource(R.string.settings_top_bar_blur),
                    description = blurUnavailable,
                    enabled = blurUnavailable == null,
                    checked = appearance.topBarBlur,
                    onCheckedChange = { on ->
                        scope.launch { dataStore.saveTopBarBlur(on) }
                    }
                )
            }
        }

        SettingsGroup {
            SettingsNavigationRow(
                title = stringResource(R.string.settings_nav_reset),
                showChevron = false,
                onClick = { confirmingReset = true }
            )
        }
    }

    AppDialog(
        show = confirmingReset,
        title = stringResource(R.string.settings_nav_reset),
        summary = stringResource(R.string.settings_nav_reset_confirm),
        onDismissRequest = { confirmingReset = false },
        buttons = {
            AppTextButton(
                text = stringResource(R.string.common_cancel),
                modifier = Modifier.weight(1f),
                onClick = { confirmingReset = false }
            )
            AppTextButton(
                text = stringResource(R.string.settings_nav_reset),
                modifier = Modifier.weight(1f),
                onClick = {
                    confirmingReset = false
                    scope.launch {
                        // Restore the shipped defaults rather than an empty preference set, so the
                        // reset is the same as a fresh install. The edit gesture is not part of
                        // this page any more and is left alone.
                        dataStore.saveNavDestinations(DefaultNavDestinations)
                        dataStore.saveNavBarScrollMode(NavBarScrollMode.ALWAYS_VISIBLE)
                        dataStore.saveTopBarScrollMode(TopBarScrollMode.ON_SCROLL)
                        dataStore.saveFloatingNavBar(true)
                        dataStore.saveTopBarBlur(false)
                        dataStore.saveBottomBarBlur(true)
                    }
                }
            )
        }
    )
}

