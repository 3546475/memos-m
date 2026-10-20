package org.example.memosm.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.example.memosm.model.ThemeStyle
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.CardDefaults as MiuixCardDefaults
import top.yukonga.miuix.kmp.basic.Checkbox as MiuixCheckbox
import top.yukonga.miuix.kmp.basic.FloatingActionButton as MiuixFab
import top.yukonga.miuix.kmp.basic.HorizontalDivider as MiuixDivider
import top.yukonga.miuix.kmp.basic.RadioButton as MiuixRadioButton
import top.yukonga.miuix.kmp.basic.SmallTopAppBar as MiuixSmallTopAppBar
import top.yukonga.miuix.kmp.basic.Surface as MiuixSurface
import top.yukonga.miuix.kmp.basic.Switch as MiuixSwitch
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Theme-aware building blocks. Each component renders the Miuix (HyperOS) equivalent while
 * [ThemeStyle.MIUIX] is active and the original Material 3 component otherwise, so screens stay
 * written once while the whole app can switch design language.
 */
private val usesMiuix: Boolean
    @Composable get() = LocalThemeStyle.current == ThemeStyle.MIUIX

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    large: Boolean = false,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    when {
        usesMiuix && large -> MiuixTopAppBar(
            title = title,
            modifier = modifier,
            navigationIcon = navigationIcon,
            actions = actions
        )
        usesMiuix -> MiuixSmallTopAppBar(
            title = title,
            modifier = modifier,
            navigationIcon = navigationIcon,
            actions = actions
        )
        large -> LargeTopAppBar(
            title = { Text(title) },
            modifier = modifier,
            navigationIcon = navigationIcon,
            actions = actions,
            scrollBehavior = scrollBehavior
        )
        else -> TopAppBar(
            title = { Text(title) },
            modifier = modifier,
            navigationIcon = navigationIcon,
            actions = actions,
            scrollBehavior = scrollBehavior
        )
    }
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    cornerRadius: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    if (usesMiuix) {
        MiuixCard(
            modifier = modifier,
            cornerRadius = cornerRadius,
            colors = MiuixCardDefaults.defaultColors(color = containerColor),
            content = content
        )
    } else {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(cornerRadius),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            content = content
        )
    }
}

@Composable
fun AppSurface(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.background,
    content: @Composable () -> Unit
) {
    if (usesMiuix) {
        MiuixSurface(modifier = modifier, shape = RectangleShape, color = containerColor) { content() }
    } else {
        Surface(modifier = modifier, color = containerColor) { content() }
    }
}

@Composable
fun AppFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    content: @Composable () -> Unit
) {
    if (usesMiuix) {
        MiuixFab(
            onClick = onClick,
            modifier = modifier,
            containerColor = MiuixTheme.colorScheme.primary,
            content = content
        )
    } else {
        FloatingActionButton(
            onClick = onClick,
            modifier = modifier,
            containerColor = containerColor,
            content = content
        )
    }
}

@Composable
fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    if (usesMiuix) {
        MiuixButton(onClick = onClick, modifier = modifier, enabled = enabled, content = content)
    } else {
        Button(onClick = onClick, modifier = modifier, enabled = enabled, content = content)
    }
}

/**
 * A settings-style row. Miuix groups rows inside a shared card, so the container colour is painted
 * by the row itself instead of by a Material surface.
 */
@Composable
fun AppListItem(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    supporting: (@Composable () -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val description: (@Composable () -> Unit)? = when {
        supporting != null -> supporting
        summary != null -> { { Text(summary) } }
        else -> null
    }
    if (usesMiuix) {
        BasicComponent(
            modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(containerColor),
            title = title,
            summary = summary,
            startAction = leading,
            endActions = trailing?.let { { it() } },
            bottomAction = if (supporting != null) supporting else null,
            enabled = enabled,
            onClick = onClick
        )
    } else {
        ListItem(
            modifier = if (onClick != null) modifier.clickableRow(onClick) else modifier,
            content = { Text(title) },
            supportingContent = description,
            leadingContent = leading,
            trailingContent = trailing,
            colors = ListItemDefaults.colors(containerColor = containerColor)
        )
    }
}

@Composable
fun AppDivider(modifier: Modifier = Modifier) {
    if (usesMiuix) {
        MiuixDivider(modifier = modifier)
    } else {
        HorizontalDivider(modifier = modifier)
    }
}

@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    if (usesMiuix) {
        MiuixTheme(colors = LocalMiuixColors.current) {
            MiuixSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = modifier,
                enabled = enabled
            )
        }
    } else {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled
        )
    }
}

@Composable
fun AppCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    if (usesMiuix) {
        MiuixCheckbox(
            state = if (checked) androidx.compose.ui.state.ToggleableState.On else androidx.compose.ui.state.ToggleableState.Off,
            onClick = onCheckedChange?.let { callback -> { callback(!checked) } },
            modifier = modifier,
            enabled = enabled
        )
    } else {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled
        )
    }
}

@Composable
fun AppRadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    if (usesMiuix) {
        MiuixRadioButton(selected = selected, onClick = onClick, modifier = modifier, enabled = enabled)
    } else {
        RadioButton(selected = selected, onClick = onClick, modifier = modifier, enabled = enabled)
    }
}

private fun Modifier.clickableRow(onClick: () -> Unit): Modifier = this.clickable(onClick = onClick)
