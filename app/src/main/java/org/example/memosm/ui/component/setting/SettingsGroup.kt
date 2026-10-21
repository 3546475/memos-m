package org.example.memosm.ui.component.setting

import androidx.compose.material3.Text

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import org.example.memosm.model.ThemeStyle
import org.example.memosm.ui.theme.AppSurface
import org.example.memosm.ui.theme.LocalGroupedRow
import org.example.memosm.ui.theme.LocalThemeStyle
import org.example.memosm.ui.theme.MiuixTokens
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * A grouped list of rows.
 *
 * Miuix draws one rounded card and separates its rows with hairlines; the 0.5dp gap between rows
 * shows the divider colour painted by the card itself. Material keeps the familiar stacked blocks
 * with a small gap between them.
 */
@Composable
fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    if (LocalThemeStyle.current == ThemeStyle.MIUIX) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(MiuixTokens.CardCornerRadius))
                .background(MaterialTheme.colorScheme.outlineVariant)
        ) {
            CompositionLocalProvider(LocalGroupedRow provides true) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(MiuixTokens.DividerThickness),
                    content = content
                )
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            content = content
        )
    }
}

@Composable
fun SettingsSurface(content: @Composable ColumnScope.() -> Unit) {
    AppSurface(
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(content = content)
    }
}

/** A small section caption above a group of rows. */
@Composable
fun SettingsLabel(label: String) {
    Text(
        label,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary
    )
}
