package org.example.moxia.ui.component.setting

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.example.moxia.R
import org.example.moxia.data.DataStoreManager
import org.example.moxia.model.AppearancePreferences
import org.example.moxia.model.EditGesture
import org.example.moxia.ui.theme.AppDialog
import org.example.moxia.ui.theme.AppListItem
import org.example.moxia.ui.theme.AppTextButton
import org.example.moxia.ui.theme.MiuixTokens
import org.koin.core.context.GlobalContext

/**
 * Which gesture on a memo card opens its editor. "None" leaves the card as a plain tap into the
 * detail page; the card menu always offers 编辑 as well.
 *
 * It lives on 常规: it describes how a memo card is interacted with, not how the bars look.
 */
@Composable
internal fun EditGestureSettingRow(
    dataStore: DataStoreManager = remember { GlobalContext.get().get() }
) {
    val appearance by dataStore.appearance.collectAsStateWithLifecycle(
        initialValue = AppearancePreferences()
    )
    val scope = rememberCoroutineScope()
    val options = EditGesture.entries
    val labels = options.map { gesture ->
        stringResource(
            when (gesture) {
                EditGesture.NONE -> R.string.settings_edit_gesture_none
                EditGesture.SINGLE_TAP -> R.string.settings_edit_gesture_single_tap
                EditGesture.DOUBLE_TAP -> R.string.settings_edit_gesture_double_tap
                EditGesture.LONG_PRESS -> R.string.settings_edit_gesture_long_press
            }
        )
    }
    val selectedIndex = options.indexOf(appearance.editGesture).coerceAtLeast(0)
    SettingsOptionRow(
        title = stringResource(R.string.settings_edit_gesture),
        summary = labels[selectedIndex],
        options = labels,
        selectedIndex = selectedIndex,
        onSelect = { index -> scope.launch { dataStore.saveEditGesture(options[index]) } }
    )
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
        trailing = {
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MiuixTokens.ChevronColor
            )
        },
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
