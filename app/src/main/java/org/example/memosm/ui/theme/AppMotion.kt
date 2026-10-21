package org.example.memosm.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset
import org.example.memosm.model.ThemeStyle

/**
 * Motion tokens for the app's own transitions (tab switches, list/detail, search bar collapse).
 *
 * HyperOS moves with short, slightly-overshooting springs — Miuix's components use
 * `spring(dampingRatio = 0.9f, stiffness ≈ 700–1755f)` — while Material ships eased tweens. Only the
 * spec changes here, never the transition shape, so the MD path keeps the timing it had before the
 * Miuix migration.
 *
 * These are deliberately not @Composable: `transitionSpec` lambdas are not composable, so callers
 * capture [ThemeStyle] once with [isMiuixMotion] and pass the flag in.
 */
fun isMiuixMotion(themeStyle: ThemeStyle): Boolean = themeStyle == ThemeStyle.MIUIX

fun appFadeSpec(
    miuix: Boolean,
    durationMillis: Int = 220,
    delayMillis: Int = 0
): FiniteAnimationSpec<Float> =
    if (miuix) {
        spring(dampingRatio = 0.9f, stiffness = 700f, visibilityThreshold = 0.001f)
    } else {
        tween(durationMillis = durationMillis, delayMillis = delayMillis)
    }

/** Slide/shape spec for the same transitions; keeps the same MD timing as [appFadeSpec]. */
fun appSlideSpec(
    miuix: Boolean,
    durationMillis: Int = 300
): FiniteAnimationSpec<IntOffset> =
    if (miuix) {
        spring(dampingRatio = 0.9f, stiffness = 550f)
    } else {
        tween(durationMillis = durationMillis)
    }
