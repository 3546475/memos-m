package org.example.moxia.ui.component.setting

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import android.os.Build
import kotlinx.coroutines.launch
import org.example.moxia.ui.component.LocalNetworkPermission
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.example.moxia.R
import org.example.moxia.ui.theme.AppDialog
import org.example.moxia.ui.theme.AppSlider
import org.example.moxia.ui.theme.AppListItem
import org.example.moxia.ui.theme.MiuixTokens
import org.example.moxia.ui.theme.AppTextButton
import org.example.moxia.ui.theme.AppTextField

enum class AppSettingsCategory { GENERAL, APPEARANCE, LINK_PREVIEWS, NETWORK }

@Composable
fun AppSettingsCard(
    pageSize: Int,
    onPageSizeChange: (Int) -> Unit,
    headerScale: Float,
    onHeaderScaleChange: (Float) -> Unit,
    linkPreviewEnabled: Boolean,
    onLinkPreviewEnabledChange: (Boolean) -> Unit,
    category: AppSettingsCategory
) {
    var showPageSizeDialog by rememberSaveable { mutableStateOf(false) }
    val networkPermission = LocalNetworkPermission.current
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (category == AppSettingsCategory.LINK_PREVIEWS) {
            SettingToggleRow(
                label = stringResource(R.string.profile_app_settings_link_previews),
                checked = linkPreviewEnabled,
                onCheckedChange = onLinkPreviewEnabledChange
            )

        }

        if (category == AppSettingsCategory.NETWORK && Build.VERSION.SDK_INT >= 37) {
                AppListItem(
                    title = stringResource(R.string.local_network_permission_title),
                    summary = stringResource(
                        if (networkPermission.granted) R.string.local_network_permission_allowed
                        else R.string.local_network_permission_description
                    ),
                    trailing = { Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MiuixTokens.ChevronColor) },
                    modifier = Modifier.clip(RoundedCornerShape(4.dp)),
                    onClick = {
                        if (networkPermission.granted) networkPermission.openSettings()
                        else scope.launch { networkPermission.requestAccess() }
                    }
                )
            }

        if (category == AppSettingsCategory.GENERAL) {
            AppListItem(
                title = stringResource(R.string.profile_app_settings_page_size),
                summary = pageSize.toString(),
                trailing = { Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MiuixTokens.ChevronColor) },
                modifier = Modifier.clip(RoundedCornerShape(4.dp)),
                onClick = { showPageSizeDialog = true }
            )

        }

        if (category == AppSettingsCategory.APPEARANCE) {
            AppListItem(
                title = stringResource(R.string.profile_app_settings_header_scale),
                modifier = Modifier.clip(RoundedCornerShape(4.dp)),
                supporting = {
                    Column {
                        Text(
                            text = String.format(
                                stringResource(R.string.profile_app_settings_header_scale_format),
                                headerScale
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val sliderState = remember { SliderState(value = headerScale, steps = 14, trackRange = 0.5f..2.0f) }
                        LaunchedEffect(headerScale) { sliderState.value = headerScale }
                        AppSlider(state = sliderState, onValueChange = onHeaderScaleChange, valueRange = 0.5f..2.0f)
                    }
                }
            )
        }
    }

    var textValue by rememberSaveable { mutableStateOf(pageSize.toString()) }
    var isError by rememberSaveable { mutableStateOf(false) }

    AppDialog(
        show = showPageSizeDialog,
        title = stringResource(R.string.profile_app_settings_page_size),
        onDismissRequest = { showPageSizeDialog = false },
        buttons = {
            AppTextButton(
                text = stringResource(R.string.common_cancel),
                modifier = Modifier.weight(1f),
                onClick = { showPageSizeDialog = false }
            )
            AppTextButton(
                text = stringResource(R.string.common_save),
                modifier = Modifier.weight(1f),
                enabled = !isError && textValue.isNotBlank(),
                onClick = {
                    val parsed = textValue.toIntOrNull()
                    if (parsed != null && parsed >= 1) {
                        onPageSizeChange(parsed)
                        showPageSizeDialog = false
                    }
                }
            )
        }
    ) {
        AppTextField(
            value = textValue,
            onValueChange = { newValue ->
                textValue = newValue
                val parsed = newValue.toIntOrNull()
                isError = parsed == null || parsed < 1
            },
            label = stringResource(R.string.profile_app_settings_page_size_hint),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
