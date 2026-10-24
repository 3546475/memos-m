package org.example.moxia.ui.theme

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/** Whether the user wants the floating feed header frosted. Defaults to on. */
val LocalTopBarBlurEnabled = staticCompositionLocalOf { true }

/**
 * Whether the user wants the bottom bar frosted. The compose button that sits on the same
 * material follows this switch too, so the bar and its button never disagree.
 */
val LocalBottomBarBlurEnabled = staticCompositionLocalOf { true }

/**
 * Whether the frosted-glass backdrop can be used here at all.
 *
 * miuix-blur's effect pipeline runs on RuntimeShader (API 33+), and the library itself declares
 * minSdk 33 — the app manifest overrides that with `tools:overrideLibrary`, so every call site must
 * be gated instead of relying on the platform.
 */
@Composable
private fun miuixBlurAvailable(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

/** The floating feed header, gated by its own switch. */
@Composable
internal fun topBarBlurSupported(): Boolean =
    LocalTopBarBlurEnabled.current && miuixBlurAvailable()

/** The bottom bar and the compose button, gated by the bottom bar's switch. */
@Composable
internal fun bottomBarBlurSupported(): Boolean =
    LocalBottomBarBlurEnabled.current && miuixBlurAvailable()
