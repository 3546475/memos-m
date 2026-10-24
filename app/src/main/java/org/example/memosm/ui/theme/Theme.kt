package org.example.memosm.ui.theme

import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec
import org.example.memosm.data.DataStoreManager
import org.example.memosm.model.AppearancePreferences
import org.example.memosm.model.ColorTheme

/** The device palette, or the neutral Material one below Android 12. Used by the palette previews. */
@Composable
fun systemColorScheme(isDark: Boolean, dynamicColor: Boolean = true): ColorScheme {
    val context = LocalContext.current
    return when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> darkColorScheme()
        else -> lightColorScheme()
    }
}

/**
 * MaterialKolor derives a full tonal palette for a seed, which costs real main-thread time, and
 * the theme is composed again for every activity. The last few results are pure functions of
 * (theme, hue, dark), so keep them for the life of the process.
 */
private val schemeCache = object : LinkedHashMap<String, ColorScheme>(8, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ColorScheme>?) = size > 8
}

private fun cachedScheme(key: String, compute: () -> ColorScheme): ColorScheme =
    synchronized(schemeCache) { schemeCache.getOrPut(key, compute) }

/** Device colors, neutral Material palettes, source-backed Memos colors, or a user-selected hue. */
fun appearanceColorScheme(
    appearance: AppearancePreferences,
    systemColors: ColorScheme,
    isDark: Boolean
): ColorScheme = when (appearance.colorTheme) {
    ColorTheme.SYSTEM -> systemColors
    ColorTheme.MONOCHROME -> cachedScheme("mono-$isDark") {
        dynamicColorScheme(
            seedColor = Color.hsv(0f, saturation = 0f, value = 0.5f),
            isDark = isDark,
            style = PaletteStyle.Monochrome,
            specVersion = ColorSpec.SpecVersion.SPEC_2025
        ).let { colors ->
            colors.copy(
                error = colors.primary, onError = colors.onPrimary,
                errorContainer = colors.primaryContainer, onErrorContainer = colors.onPrimaryContainer
            )
        }
    }
    ColorTheme.MEMOS -> memosColorScheme(isDark)
    ColorTheme.CUSTOM -> appearance.customHue?.let { hue ->
        cachedScheme("custom-$hue-$isDark") {
            dynamicColorScheme(
                seedColor = Color.hsv(hue % 360f, saturation = 0.65f, value = 0.75f),
                isDark = isDark,
                style = PaletteStyle.TonalSpot,
                specVersion = ColorSpec.SpecVersion.SPEC_2025
            )
        }
    } ?: systemColors
}

/** Applies the saved design language: the HyperOS (Miuix) palette, type scale and corners. */
@Composable
fun MemosMTheme(
    darkTheme: Boolean? = null,
    appearance: AppearancePreferences = AppearancePreferences(),
    content: @Composable () -> Unit
) {
    val isDark = darkTheme ?: appearance.themeMode.isDark(isSystemInDarkTheme())
    MiuixMemosMTheme(appearance = appearance, isDark = isDark, content = content)
}

/**
 * Paints the window itself in the page colour.
 *
 * The windows are edge-to-edge, so any pixel the composition does not cover (a few dp under the
 * gesture bar, for instance) shows the platform window background — white — as a thin bright strip.
 * Matching it to the page colour makes such a strip invisible in both themes.
 */
@Composable
fun WindowBackgroundColor() {
    val activity = LocalActivity.current ?: return
    val color = MaterialTheme.colorScheme.background
    SideEffect {
        activity.window?.setBackgroundDrawable(ColorDrawable(color.toArgb()))
    }
}

/** Each activity observes the same saved preferences, including changes made in another activity. */
@Suppress("DEPRECATION") // isStatusBarContrastEnforced is deprecated but still the only switch
@Composable
fun SavedMemosMTheme(dataStoreManager: DataStoreManager, content: @Composable () -> Unit) {
    val appearance by dataStoreManager.appearance.collectAsStateWithLifecycle(
        initialValue = AppearancePreferences(), minActiveState = Lifecycle.State.CREATED
    )
    val isDark = appearance.themeMode.isDark(isSystemInDarkTheme())
    val activity = LocalActivity.current
    val view = LocalView.current
    SideEffect {
        activity?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !isDark
                isAppearanceLightNavigationBars = !isDark
            }
            // Android 10+ may paint a contrast scrim behind the transparent system bars. The
            // app is edge-to-edge and draws the content itself, so both scrims must be off for
            // the page background/content to show through unchanged.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
                window.isStatusBarContrastEnforced = false
            }
        }
    }
    MemosMTheme(appearance = appearance, content = content)
}

/** Expressive styling is local to Profile and its activities. */
@Composable
fun ProfileTheme(content: @Composable () -> Unit) {
    val base = MaterialTheme.colorScheme
    val colors = base.copy(surface = base.surfaceContainerLow, background = base.surfaceContainerLow)
    MaterialExpressiveTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        motionScheme = MotionScheme.expressive(),
        content = content
    )
}
