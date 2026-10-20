package org.example.memosm.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import org.example.memosm.model.AppFont
import org.example.memosm.model.AppearancePreferences
import org.example.memosm.model.ColorTheme
import org.example.memosm.model.ThemeStyle
import top.yukonga.miuix.kmp.basic.Switch as MiuixSwitch
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.Colors as MiuixColors
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.TextStyles as MiuixTextStyles
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle
import top.yukonga.miuix.kmp.theme.darkColorScheme as miuixDarkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme as miuixLightColorScheme
import top.yukonga.miuix.kmp.theme.platformDynamicColors

/** The active design language; component code picks Miuix equivalents when it is [ThemeStyle.MIUIX]. */
val LocalThemeStyle = staticCompositionLocalOf { ThemeStyle.MD }

/**
 * The Miuix palette in effect. Miuix components need their own theme, so nesting one inside a
 * Material surface reads this instead of the library defaults to stay in sync with the app theme.
 */
val LocalMiuixColors = staticCompositionLocalOf { miuixLightColorScheme() }

/**
 * Miuix (HyperOS) styling for the app.
 *
 * The screens are written against Material 3, so the Miuix palette, corners and type scale are
 * translated into the Material theme rather than replacing every component. Miuix components that
 * are used directly (the switches) are nested in the real [MiuixTheme] with [LocalMiuixColors].
 */
@Composable
fun MiuixMemosMTheme(
    appearance: AppearancePreferences,
    isDark: Boolean,
    content: @Composable () -> Unit
) {
    val colors = miuixColorsFor(appearance, isDark)
    MiuixTheme(colors = colors) {
        val textStyles = MiuixTheme.textStyles
        val typography = remember(appearance.font, textStyles) {
            miuixTypography(appearance.font, textStyles)
        }
        CompositionLocalProvider(
            LocalThemeStyle provides ThemeStyle.MIUIX,
            LocalMiuixColors provides colors
        ) {
            MaterialTheme(
                colorScheme = colors.toMaterialColorScheme(isDark),
                typography = typography,
                shapes = MiuixShapes,
                content = content
            )
        }
    }
}

/** A Miuix switch for rows that own the toggle semantics themselves. */
@Composable
fun MiuixStyledSwitch(checked: Boolean) {
    MiuixTheme(colors = LocalMiuixColors.current) {
        MiuixSwitch(checked = checked, onCheckedChange = null)
    }
}

/** Device colors, neutral palettes, the source-backed Memos colors, or the user's hue. */
@Composable
private fun miuixColorsFor(appearance: AppearancePreferences, isDark: Boolean): MiuixColors =
    when (appearance.colorTheme) {
        // Miuix reads the system palette itself and falls back to its own seed below Android 12.
        ColorTheme.SYSTEM -> platformDynamicColors(dark = isDark)
        ColorTheme.MONOCHROME -> miuixColorsFromSeed(
            seed = Color.hsv(0f, saturation = 0f, value = 0.5f),
            paletteStyle = ThemePaletteStyle.Monochrome,
            isDark = isDark
        )
        ColorTheme.MEMOS -> miuixColorsFromSeed(
            seed = memosColorScheme(isDark).primary,
            paletteStyle = ThemePaletteStyle.TonalSpot,
            isDark = isDark
        )
        ColorTheme.CUSTOM -> appearance.customHue?.let { hue ->
            miuixColorsFromSeed(
                seed = Color.hsv(hue % 360f, saturation = 0.65f, value = 0.75f),
                paletteStyle = ThemePaletteStyle.TonalSpot,
                isDark = isDark
            )
        } ?: if (isDark) miuixDarkColorScheme() else miuixLightColorScheme()
    }

@Composable
private fun miuixColorsFromSeed(
    seed: Color,
    paletteStyle: ThemePaletteStyle,
    isDark: Boolean
): MiuixColors {
    val controller = remember(seed, paletteStyle, isDark) {
        ThemeController(
            colorSchemeMode = if (isDark) ColorSchemeMode.MonetDark else ColorSchemeMode.MonetLight,
            keyColor = seed,
            colorSpec = ThemeColorSpec.Spec2025,
            paletteStyle = paletteStyle
        )
    }
    return controller.currentColors()
}

/** Cards sit on the page color; dividers and secondary text keep Miuix's own contrast pairs. */
private fun MiuixColors.toMaterialColorScheme(isDark: Boolean): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = tertiaryContainer,
        onPrimaryContainer = onTertiaryContainer,
        inversePrimary = primaryVariant,
        secondary = secondaryVariant,
        onSecondary = onSecondaryVariant,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryVariant,
        tertiary = onTertiaryContainer,
        onTertiary = tertiaryContainer,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = surface,
        onBackground = onSurface,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceSecondary,
        surfaceTint = primary,
        inverseSurface = surfaceContainerHighest,
        inverseOnSurface = onSurfaceContainerHighest,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        outline = outline,
        outlineVariant = dividerLine,
        scrim = windowDimming,
        surfaceBright = surfaceContainerHighest,
        surfaceDim = surface,
        surfaceContainerLowest = surfaceContainer,
        surfaceContainerLow = surfaceContainer,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainer,
        surfaceContainerHighest = surfaceContainerHighest
    )
}

/** Miuix cards use 16 dp corners and its dialogs and sheets a little more. */
private val MiuixShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/** Miuix's type scale, keeping Material's weights and line heights where Miuix leaves them open. */
private fun miuixTypography(font: AppFont, styles: MiuixTextStyles): Typography {
    val family = font.fontFamily
    val base = typographyFor(font)
    return base.copy(
        displayLarge = base.displayLarge.adopting(styles.title1, family),
        displayMedium = base.displayMedium.adopting(styles.title2, family),
        displaySmall = base.displaySmall.adopting(styles.title3, family),
        headlineLarge = base.headlineLarge.adopting(styles.title3, family),
        headlineMedium = base.headlineMedium.adopting(styles.title4, family),
        headlineSmall = base.headlineSmall.adopting(styles.headline1, family),
        titleLarge = base.titleLarge.adopting(styles.title4, family),
        titleMedium = base.titleMedium.adopting(styles.headline2, family),
        titleSmall = base.titleSmall.adopting(styles.subtitle, family),
        bodyLarge = base.bodyLarge.adopting(styles.main, family),
        bodyMedium = base.bodyMedium.adopting(styles.body1, family),
        bodySmall = base.bodySmall.adopting(styles.body2, family),
        labelLarge = base.labelLarge.adopting(styles.button, family),
        labelMedium = base.labelMedium.adopting(styles.footnote1, family),
        labelSmall = base.labelSmall.adopting(styles.footnote2, family)
    )
}

private fun TextStyle.adopting(source: TextStyle, family: FontFamily): TextStyle = copy(
    fontFamily = family,
    fontSize = if (source.fontSize.isSpecified) source.fontSize else fontSize,
    lineHeight = if (source.lineHeight.isSpecified) source.lineHeight else lineHeight,
    fontWeight = source.fontWeight ?: fontWeight,
    letterSpacing = if (source.letterSpacing.isSpecified) source.letterSpacing else letterSpacing
)
