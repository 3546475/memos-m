package org.example.moxia.ui.component.setting

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.example.moxia.ui.theme.AppListItem
import org.example.moxia.ui.theme.AppSwitch

/** The row owns the switch semantics so its label and state are announced together. */
@Composable
internal fun SettingToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null,
    icon: ImageVector? = null,
    /** A switch that cannot take effect is shown dimmed with the reason in [description]. */
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh
) {
    AppListItem(
        title = label,
        summary = description,
        leading = icon?.let {
            {
                Icon(
                    it,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        trailing = { AppSwitch(checked = checked, onCheckedChange = null) },
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.45f)
            .clip(RoundedCornerShape(4.dp)),
        containerColor = containerColor,
        enabled = enabled,
        onClick = if (enabled) ({ onCheckedChange(!checked) }) else null
    )
}
