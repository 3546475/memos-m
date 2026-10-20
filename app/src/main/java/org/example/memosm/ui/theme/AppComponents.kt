package org.example.memosm.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import org.example.memosm.model.ThemeStyle
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.CardDefaults as MiuixCardDefaults
import top.yukonga.miuix.kmp.basic.Checkbox as MiuixCheckbox
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator as MiuixCircularProgressIndicator
import top.yukonga.miuix.kmp.basic.FloatingActionButton as MiuixFab
import top.yukonga.miuix.kmp.basic.HorizontalDivider as MiuixDivider
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator as MiuixLinearProgressIndicator
import top.yukonga.miuix.kmp.basic.PullToRefresh as MiuixPullToRefresh
import top.yukonga.miuix.kmp.basic.RadioButton as MiuixRadioButton
import top.yukonga.miuix.kmp.basic.SmallTopAppBar as MiuixSmallTopAppBar
import top.yukonga.miuix.kmp.basic.Slider as MiuixSlider
import top.yukonga.miuix.kmp.basic.Surface as MiuixSurface
import top.yukonga.miuix.kmp.basic.Switch as MiuixSwitch
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.basic.TextField as MiuixTextField
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.window.WindowDialog as MiuixWindowDialog
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
    cornerRadius: Dp = MiuixTokens.CardCornerRadius,
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
            modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(MiuixTokens.RowCornerRadius)).background(containerColor),
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

/** A dialog that renders Miuix's window dialog or a Material 3 [AlertDialog]. */
@Composable
fun AppDialog(
    show: Boolean,
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    buttons: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit = {}
) {
    if (usesMiuix) {
        MiuixWindowDialog(
            show = show,
            modifier = modifier,
            title = title,
            summary = summary,
            onDismissRequest = onDismissRequest
        ) {
            Column(Modifier.fillMaxWidth()) {
                content()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = MiuixTokens.RowVerticalPadding),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    content = buttons
                )
            }
        }
    } else if (show) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            title = { Text(title) },
            text = {
                Column {
                    summary?.let { Text(it) }
                    content()
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = buttons)
            }
        )
    }
}

/** A Miuix text button; the Material branch keeps the familiar [TextButton]. */
@Composable
fun AppTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    if (usesMiuix) {
        MiuixTextButton(text = text, onClick = onClick, modifier = modifier, enabled = enabled)
    } else {
        TextButton(onClick = onClick, modifier = modifier, enabled = enabled) { Text(text) }
    }
}

/** A text field; Miuix has no outlined variant, so MIUIX uses its filled field. */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    placeholder: String = "",
    enabled: Boolean = true,
    singleLine: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    isError: Boolean = false,
    supportingText: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    if (usesMiuix) {
        MiuixTheme(colors = LocalMiuixColors.current) {
            MiuixTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = modifier,
                label = label.ifEmpty { placeholder },
                useLabelAsPlaceholder = label.isEmpty() && placeholder.isNotEmpty(),
                enabled = enabled,
                singleLine = singleLine,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon
            )
        }
    } else {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            enabled = enabled,
            singleLine = singleLine,
            label = label.takeIf { it.isNotEmpty() }?.let { { Text(it) } },
            placeholder = placeholder.takeIf { it.isNotEmpty() }?.let { { Text(it) } },
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            isError = isError,
            supportingText = supportingText?.let { { Text(it) } },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon
        )
    }
}

/**
 * A slider driven by plain values. Miuix sliders are continuous, so a stepped Material slider is
 * emulated by snapping the reported value to the nearest step.
 */
@Composable
fun AppSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null
) {
    if (usesMiuix) {
        val snap = { raw: Float ->
            if (steps > 0) {
                val step = (valueRange.endInclusive - valueRange.start) / (steps + 1)
                valueRange.start + ((raw - valueRange.start) / step).roundToInt() * step
            } else {
                raw
            }
        }
        MiuixTheme(colors = LocalMiuixColors.current) {
            MiuixSlider(
                value = value,
                onValueChange = { onValueChange(snap(it)) },
                modifier = modifier,
                enabled = enabled,
                valueRange = valueRange,
                onValueChangeFinished = onValueChangeFinished
            )
        }
    } else {
        val sliderState = remember(value, valueRange, steps) {
            SliderState(value = value, steps = steps, trackRange = valueRange)
        }
        LaunchedEffect(value) { sliderState.value = value }
        Slider(
            state = sliderState,
            onValueChange = onValueChange,
            modifier = modifier,
            enabled = enabled,
            onValueChangeFinished = onValueChangeFinished
        )
    }
}

@Composable
fun AppLinearProgressIndicator(
    progress: Float?,
    modifier: Modifier = Modifier
) {
    if (usesMiuix) {
        MiuixLinearProgressIndicator(progress = progress, modifier = modifier)
    } else if (progress == null) {
        LinearProgressIndicator(modifier = modifier)
    } else {
        LinearProgressIndicator(progress = { progress }, modifier = modifier)
    }
}

@Composable
fun AppCircularProgressIndicator(
    modifier: Modifier = Modifier,
    progress: Float? = null
) {
    if (usesMiuix) {
        MiuixCircularProgressIndicator(modifier = modifier, progress = progress)
    } else if (progress == null) {
        CircularProgressIndicator(modifier = modifier)
    } else {
        CircularProgressIndicator(progress = { progress }, modifier = modifier)
    }
}

/** A single-select capsule used for filters; Miuix has no chip component. */
@Composable
fun AppFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    if (usesMiuix) {
        val shape = RoundedCornerShape(MiuixTokens.PillCornerRadius)
        MiuixTheme(colors = LocalMiuixColors.current) {
            MiuixSurface(
                modifier = modifier
                    .clip(shape)
                    .clickable(enabled = enabled, onClick = onClick),
                shape = shape,
                color = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.surfaceContainerHigh,
                contentColor = if (selected) {
                    MiuixTheme.colorScheme.onPrimary
                } else {
                    MiuixTheme.colorScheme.onSurfaceContainer
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    content = content
                )
            }
        }
    } else {
        FilterChip(
            selected = selected,
            onClick = onClick,
            label = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    content = content
                )
            },
            modifier = modifier,
            enabled = enabled
        )
    }
}

private fun Modifier.clickableRow(onClick: () -> Unit): Modifier = this.clickable(onClick = onClick)

/** Pull-to-refresh container: Miuix's own indicator, or Material's while MD is active. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (usesMiuix) {
        MiuixPullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = modifier
        ) {
            content()
        }
    } else {
        val state = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            state = state,
            modifier = modifier,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = state,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
                )
            }
        ) {
            content()
        }
    }
}
