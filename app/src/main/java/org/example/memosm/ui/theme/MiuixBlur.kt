package org.example.memosm.ui.theme

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import org.example.memosm.model.ThemeStyle

/**
 * Whether the user wants frosted surfaces. Defaults to on; the main screen provides the stored
 * preference so the top bar, the bottom bar and the compose button follow one switch.
 */
val LocalBlurEnabled = staticCompositionLocalOf { true }

/**
 * Whether the frosted-glass backdrop can be used here.
 *
 * miuix-blur's effect pipeline runs on RuntimeShader (API 33+), and the library itself declares
 * minSdk 33 — the app manifest overrides that with `tools:overrideLibrary`, so every call site must
 * be gated instead of relying on the platform. MD never uses the blur, which keeps the Material
 * look byte-for-byte unchanged, and the user's switch turns it off for the whole app.
 */
@Composable
internal fun miuixBlurSupported(): Boolean =
    LocalBlurEnabled.current &&
        LocalThemeStyle.current == ThemeStyle.MIUIX &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
