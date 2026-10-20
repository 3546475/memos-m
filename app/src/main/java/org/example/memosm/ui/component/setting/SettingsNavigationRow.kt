package org.example.memosm.ui.component.setting

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.example.memosm.ui.theme.AppListItem

@Composable
internal fun SettingsNavigationRow(
    title: String,
    summary: String? = null,
    icon: ImageVector? = null,
    showChevron: Boolean = true,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    supportingContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    AppListItem(
        title = title,
        modifier = modifier.clip(RoundedCornerShape(4.dp)),
        summary = summary,
        supporting = supportingContent,
        leading = icon?.let { { Icon(it, contentDescription = null) } },
        trailing = if (showChevron) {
            { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null) }
        } else null,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        enabled = enabled,
        onClick = onClick
    )
}
