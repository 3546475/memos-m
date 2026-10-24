package org.example.moxia.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import kotlinx.coroutines.delay
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.basic.ButtonDefaults as MiuixButtonDefaults
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.CardDefaults as MiuixCardDefaults
import top.yukonga.miuix.kmp.basic.Checkbox as MiuixCheckbox
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator as MiuixCircularProgressIndicator
import top.yukonga.miuix.kmp.basic.FloatingActionButton as MiuixFab
import top.yukonga.miuix.kmp.basic.HorizontalDivider as MiuixDivider
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator as MiuixLinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults as MiuixProgressDefaults
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.RadioButton as MiuixRadioButton
import top.yukonga.miuix.kmp.basic.SmallTopAppBar as MiuixSmallTopAppBar
import top.yukonga.miuix.kmp.basic.Slider as MiuixSlider
import top.yukonga.miuix.kmp.basic.Surface as MiuixSurface
import top.yukonga.miuix.kmp.basic.Switch as MiuixSwitch
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.basic.TextField as MiuixTextField
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.window.WindowDialog as MiuixWindowDialog
import top.yukonga.miuix.kmp.window.WindowListPopup as MiuixWindowListPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * The Miuix (HyperOS) building blocks every screen is written against.
 *
 * The app has a single design language, so these are thin, Miuix-backed wrappers rather than an
 * adaptive layer: a screen names the intent (a row, a dialog, a chip) and gets the HyperOS
 * component, with the palette supplied by [LocalMiuixColors].
 */

/**
 * A page title bar. [large] picks Miuix's taller bar, which carries a subtitle-sized title and
 * more breathing room above the content.
 */
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    large: Boolean = false,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    if (large) {
        MiuixTopAppBar(
            title = title,
            modifier = modifier,
            navigationIcon = navigationIcon,
            actions = actions
        )
    } else {
        MiuixSmallTopAppBar(
            title = title,
            modifier = modifier,
            navigationIcon = navigationIcon,
            actions = actions
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
    MiuixCard(
        modifier = modifier,
        cornerRadius = cornerRadius,
        colors = MiuixCardDefaults.defaultColors(color = containerColor),
        content = content
    )
}

@Composable
fun AppSurface(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.background,
    content: @Composable () -> Unit
) {
    MiuixSurface(modifier = modifier, shape = RectangleShape, color = containerColor) { content() }
}

@Composable
fun AppFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * The recorded layer behind this button. Passing one switches the button to the floating bar's
     * frosted material: the rim alone is not the material, the blur behind it is.
     */
    backdrop: Backdrop? = null,
    content: @Composable () -> Unit
) {
    // Miuix's FAB does not hand a content colour down, so an untinted Icon would draw in the
    // Material onSurface (black) on top of the accent fill.
    if (backdrop != null) {
        val glass = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.86f)
        CompositionLocalProvider(LocalContentColor provides MiuixTheme.colorScheme.onSurface) {
            Box(
                modifier = modifier
                    .size(56.dp)
                    // The glass still has to sit above the page: without a shadow it read as a
                    // flat disc pasted onto the content.
                    .shadow(elevation = 8.dp, shape = CircleShape, clip = false)
                    .capsuleGlass(backdrop, glass, isLightSurface(MiuixTheme.colorScheme.surface), CircleShape)
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                content()
            }
        }
    } else {
        CompositionLocalProvider(LocalContentColor provides MiuixTheme.colorScheme.onPrimary) {
            MiuixFab(
                onClick = onClick,
                modifier = modifier.capsuleHighlight(
                    lightSurface = isLightSurface(MiuixTheme.colorScheme.surface)
                ),
                containerColor = MiuixTheme.colorScheme.primary,
                content = content
            )
        }
    }
}

@Composable
fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    /** Tonal styling: a quiet fill instead of the accent one, for the secondary action. */
    secondary: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    // MiuixButton paints its own container but does not publish a Material content colour, so
    // a Material Text inside drew in the default on-surface black and vanished on the accent
    // fill. The button's own content colour is forwarded explicitly.
    val colors = if (secondary) {
        // Defaults are the quiet secondary fill; the primary preset is the accent one.
        MiuixButtonDefaults.buttonColors()
    } else {
        MiuixButtonDefaults.buttonColorsPrimary()
    }
    MiuixButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        content = {
            CompositionLocalProvider(LocalContentColor provides colors.contentColor) {
                content()
            }
        }
    )
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
    // Inside a grouped card the row has no surface of its own: the card supplies both the
    // colour and the corners, so rows in a group read as one block.
    val grouped = LocalGroupedRow.current
    BasicComponent(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (grouped) {
                    Modifier
                } else {
                    Modifier
                        .clip(RoundedCornerShape(MiuixTokens.RowCornerRadius))
                        .background(containerColor)
                }
            ),
        title = title,
        summary = summary,
        startAction = leading,
        endActions = trailing?.let { { it() } },
        bottomAction = if (supporting != null) supporting else null,
        enabled = enabled,
        onClick = onClick
    )
}

@Composable
fun AppDivider(
    modifier: Modifier = Modifier,
    thickness: Dp? = null,
    color: Color? = null
) {
    when {
        thickness != null && color != null -> MiuixDivider(modifier = modifier, thickness = thickness, color = color)
        thickness != null -> MiuixDivider(modifier = modifier, thickness = thickness)
        color != null -> MiuixDivider(modifier = modifier, color = color)
        else -> MiuixDivider(modifier = modifier)
    }
}

@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    MiuixTheme(colors = LocalMiuixColors.current) {
        MiuixSwitch(
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
    MiuixCheckbox(
        state = if (checked) androidx.compose.ui.state.ToggleableState.On else androidx.compose.ui.state.ToggleableState.Off,
        onClick = onCheckedChange?.let { callback -> { callback(!checked) } },
        modifier = modifier,
        enabled = enabled
    )
}

@Composable
fun AppRadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    MiuixRadioButton(selected = selected, onClick = onClick, modifier = modifier, enabled = enabled)
}

/** A dialog rendered as Miuix's own window dialog. */
@Composable
fun AppDialog(
    show: Boolean,
    title: String? = null,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    buttons: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit = {}
) {
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
}

@Composable
fun AppTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false
) {
    MiuixTextButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = if (destructive) {
            MiuixButtonDefaults.textButtonColors(textColor = MiuixTheme.colorScheme.error)
        } else {
            MiuixButtonDefaults.textButtonColors()
        }
    )
}

/** A filled text field. The Miuix field has no outlined variant; the label doubles as placeholder. */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    placeholder: String = "",
    enabled: Boolean = true,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    /** Shows a value the user can read and copy but not type into. */
    readOnly: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    MiuixTheme(colors = LocalMiuixColors.current) {
        MiuixTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            label = label.ifEmpty { placeholder },
            useLabelAsPlaceholder = label.isEmpty() && placeholder.isNotEmpty(),
            enabled = enabled,
            readOnly = readOnly,
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon
        )
    }
}

/**
 * A slider driven by plain values. Miuix sliders are continuous, so a stepped slider is emulated
 * by snapping the reported value to the nearest step.
 */
/** State-based slider, mirroring Material's [androidx.compose.material3.Slider] overload that takes a [SliderState]. */
@Composable
fun AppSlider(
    state: SliderState,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null
) {
    AppSlider(
        value = state.value,
        onValueChange = { newValue ->
            state.value = newValue
            onValueChange(newValue)
        },
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = state.steps,
        onValueChangeFinished = onValueChangeFinished
    )
}

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
}

@Composable
fun AppLinearProgressIndicator(
    progress: Float?,
    modifier: Modifier = Modifier,
    color: Color? = null
) {
    MiuixLinearProgressIndicator(
        progress = progress,
        modifier = modifier,
        colors = MiuixProgressDefaults.progressIndicatorColors(
            foregroundColor = color ?: MiuixTheme.colorScheme.primary
        )
    )
}

@Composable
fun AppCircularProgressIndicator(
    modifier: Modifier = Modifier,
    progress: Float? = null,
    strokeWidth: Dp? = null,
    color: Color? = null,
    /**
     * Diameter of the indicator. Miuix draws its ring from this parameter instead of the layout
     * size, so passing a size through [modifier] alone leaves the ring off-centre and at the wrong
     * diameter; every call site that needs a specific size sets this instead.
     *
     * The indeterminate animation is the dot-matrix wave from [AppDotLoader]; [strokeWidth] only
     * affects the determinate ring, which still needs the circular shape to read as progress.
     */
    size: Dp? = null
) {
    val determinate = progress
    if (determinate == null) {
        // Every indeterminate spinner uses the same dot-matrix wave, whatever its size.
        val diameter = size ?: MiuixProgressDefaults.DefaultCircularProgressIndicatorSize
        AppDotLoader(
            modifier = modifier.size(diameter),
            color = color ?: MaterialTheme.colorScheme.primary
        )
    } else {
        // Miuix reads the spinner tint from a ProgressIndicatorColors bundle rather than a bare
        // Colour, so a call site that needs to match its button's content tint passes `color` here.
        MiuixCircularProgressIndicator(
            modifier = modifier,
            progress = determinate,
            colors = MiuixProgressDefaults.progressIndicatorColors(
                foregroundColor = color ?: MiuixTheme.colorScheme.primary
            ),
            strokeWidth = strokeWidth ?: MiuixProgressDefaults.DefaultCircularProgressIndicatorStrokeWidth,
            size = size ?: MiuixProgressDefaults.DefaultCircularProgressIndicatorSize
        )
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
    val shape = RoundedCornerShape(MiuixTokens.PillCornerRadius)
    val containerColor =
        if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.surfaceContainerHigh
    val contentColor =
        if (selected) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurfaceContainer
    MiuixTheme(colors = LocalMiuixColors.current) {
        MiuixSurface(
            modifier = modifier
                .clip(shape)
                // A plain clickable announced no state: TalkBack read the label but never said
                // whether the filter behind it was on. selectable publishes the selection.
                .selectable(
                    selected = selected,
                    enabled = enabled,
                    onClick = onClick
                ),
            shape = shape,
            color = containerColor,
            contentColor = contentColor
        ) {
            // MiuixSurface forwards its own content colour, but Material's Text reads
            // LocalContentColor; without this the label fell back to the default on-surface
            // black and was unreadable on a dark selected fill.
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    content = content
                )
            }
        }
    }
}

/** An anchored menu, rendered as a Miuix window-level list popup (no popup host is required). */
@Composable
fun AppDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    MiuixWindowListPopup(
        show = expanded,
        popupModifier = modifier,
        onDismissRequest = onDismissRequest
    ) {
        ListPopupColumn {
            Column(Modifier.padding(vertical = 6.dp)) { content() }
        }
    }
}

/** A row inside [AppDropdownMenu]. */
@Composable
fun AppDropdownMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        destructive -> MiuixTheme.colorScheme.error
        else -> MiuixTheme.colorScheme.onSurface
    }
    MiuixTheme(colors = LocalMiuixColors.current) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon?.let {
                CompositionLocalProvider(LocalContentColor provides contentColor) { it() }
                Spacer(Modifier.width(12.dp))
            }
            Text(
                text = text,
                color = contentColor,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

/**
 * Snackbar host. Miuix ships its own SnackbarHostState type, so a Miuix host cannot be fed the
 * Material state callers already hold; this renders a Miuix-styled pill driven by the Material
 * state and dismisses it itself, keeping `showSnackbar` coroutines completing normally.
 */
@Composable
fun AppSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val data = hostState.currentSnackbarData ?: return
    LaunchedEffect(data) {
        val durationMs = when (data.visuals.duration) {
            SnackbarDuration.Short -> 4_000L
            SnackbarDuration.Long -> 10_000L
            SnackbarDuration.Indefinite -> Long.MAX_VALUE
        }
        if (durationMs != Long.MAX_VALUE) {
            delay(durationMs)
            data.dismiss()
        }
    }
    Box(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        MiuixTheme(colors = LocalMiuixColors.current) {
            MiuixSurface(
                shape = RoundedCornerShape(24.dp),
                color = MiuixTheme.colorScheme.surfaceContainerHighest
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = data.visuals.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MiuixTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    data.visuals.actionLabel?.let { label ->
                        Spacer(Modifier.width(12.dp))
                        AppTextButton(
                            text = label,
                            onClick = { data.performAction() }
                        )
                    }
                }
            }
        }
    }
}

/** A password field. Miuix masks through the same [PasswordVisualTransformation] the value-based field takes. */
// Miuix's value-based TextField only exposes VisualTransformation for masking; the newer
// BasicSecureTextField would have to be re-skinned by hand, so keep the deprecated route.
@Suppress("DEPRECATION")
@Composable
fun AppSecureTextField(
    state: TextFieldState,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    supportingText: String? = null,
    onDone: (() -> Unit)? = null
) {
    Column(modifier = modifier) {
        MiuixTheme(colors = LocalMiuixColors.current) {
            MiuixTextField(
                value = state.text.toString(),
                onValueChange = { state.setTextAndPlaceCursorAtEnd(it) },
                modifier = Modifier.fillMaxWidth(),
                label = label,
                enabled = enabled,
                keyboardOptions = keyboardOptions,
                keyboardActions = KeyboardActions(
                    onDone = onDone?.let { action -> { action() } } ?: {}
                ),
                visualTransformation = PasswordVisualTransformation()
            )
        }
        supportingText?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

/**
 * A labelled primary action. Miuix has no extended FAB, so this renders a pill button.
 */
@Composable
fun AppExtendedFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** See [AppFloatingActionButton]: a backdrop turns this into the frosted bar material. */
    backdrop: Backdrop? = null,
    icon: @Composable () -> Unit,
    text: @Composable () -> Unit
) {
    if (backdrop != null) {
        val glass = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.86f)
        CompositionLocalProvider(LocalContentColor provides MiuixTheme.colorScheme.onSurface) {
            Row(
                modifier = modifier
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(percent = 50),
                        clip = false
                    )
                    .capsuleGlass(backdrop, glass, isLightSurface(MiuixTheme.colorScheme.surface), RoundedCornerShape(percent = 50))
                    .clickable(onClick = onClick)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                icon()
                Spacer(Modifier.width(8.dp))
                text()
            }
        }
    } else {
        MiuixTheme(colors = LocalMiuixColors.current) {
            MiuixButton(
                onClick = onClick,
                // Same glass bead as the floating bar: the extended action ("add comment",
                // "save") is part of the same material family.
                modifier = modifier.capsuleHighlight(
                    lightSurface = isLightSurface(MiuixTheme.colorScheme.surface)
                ),
                colors = MiuixButtonDefaults.buttonColorsPrimary(),
                content = {
                    CompositionLocalProvider(
                        LocalContentColor provides MiuixTheme.colorScheme.onPrimary
                    ) {
                        icon()
                        Spacer(Modifier.width(8.dp))
                        text()
                    }
                }
            )
        }
    }
}

/** A small reaction pill: an accent capsule when the user reacted, grey otherwise. */
@Composable
fun AppReactionChip(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    MiuixTheme(colors = LocalMiuixColors.current) {
        val background = if (selected) {
            MiuixTheme.colorScheme.primary
        } else {
            MiuixTheme.colorScheme.secondaryContainer
        }
        val contentColor = if (selected) {
            MiuixTheme.colorScheme.onPrimary
        } else {
            MiuixTheme.colorScheme.onSecondaryContainer
        }
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Row(
                modifier = modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(background)
                    .clickable(onClick = onClick)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}
