package org.example.moxia.ui.component.setting

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
import org.example.moxia.model.ThemeStyle
import org.example.moxia.ui.theme.AppSurface
import org.example.moxia.ui.theme.LocalGroupedRow
import org.example.moxia.ui.theme.LocalThemeStyle
import org.example.moxia.ui.theme.MiuixTokens
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * A grouped list of rows.
 *
 * Miuix shows a group as one solid card: the rows carry no surface, no corners and no separators,
 * and the card's rounded background is the only shape the user sees. Material keeps the familiar
 * stacked blocks with a small gap between them.
 */
@Composable
fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    if (LocalThemeStyle.current == ThemeStyle.MIUIX) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(MiuixTokens.CardCornerRadius))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            CompositionLocalProvider(LocalGroupedRow provides true) {
                Column(content = content)
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
