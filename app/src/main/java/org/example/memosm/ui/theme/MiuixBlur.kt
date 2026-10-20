package org.example.memosm.ui.theme

import android.os.Build
import androidx.compose.runtime.Composable
import org.example.memosm.model.ThemeStyle

/**
 * Whether the frosted-glass backdrop can be used here.
 *
 * miuix-blur's effect pipeline runs on RuntimeShader (API 33+), and the library itself declares
 * minSdk 33 — the app manifest overrides that with `tools:overrideLibrary`, so every call site must
 * be gated instead of relying on the platform. MD never uses the blur, which keeps the Material
 * look byte-for-byte unchanged.
 */
@Composable
internal fun miuixBlurSupported(): Boolean =
    LocalThemeStyle.current == ThemeStyle.MIUIX && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
