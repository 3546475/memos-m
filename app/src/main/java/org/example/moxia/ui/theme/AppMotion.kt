package org.example.moxia.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset

/**
 * Motion tokens for the app's own transitions (tab switches, list/detail, search bar collapse).
 *
 * HyperOS moves with short, slightly-overshooting springs — Miuix's components use
 * `spring(dampingRatio = 0.9f, stiffness ≈ 700–1755f)` — and the app's own transitions follow the
 * same curve, so a screen change never lands on a slower Material tween than the component
 * animation around it.
 *
 * These are deliberately not @Composable: `transitionSpec` lambdas are not composable, so the
 * spec has to be a plain value.
 */
fun appFadeSpec(): FiniteAnimationSpec<Float> =
    spring(dampingRatio = 0.9f, stiffness = 700f, visibilityThreshold = 0.001f)

/** Slide/shape spec for the same transitions. */
fun appSlideSpec(): FiniteAnimationSpec<IntOffset> =
    spring(dampingRatio = 0.9f, stiffness = 550f)

/** Enter spec for overlay-style transitions (full-screen viewers, sheets). */
fun appEnterSpec(): FiniteAnimationSpec<Float> =
    spring(dampingRatio = 0.9f, stiffness = 700f, visibilityThreshold = 0.001f)

/** Exit spec; slightly firmer so dismissals feel settled rather than bouncy. */
fun appExitSpec(): FiniteAnimationSpec<Float> =
    spring(dampingRatio = 0.95f, stiffness = 550f, visibilityThreshold = 0.001f)

/**
 * Press feedback for compact surfaces such as the filter pills: the element dips a few percent
 * while the finger is down and springs back on release. Scale is a graphics-layer transform, so it
 * never moves a neighbour or reflows a row.
 *
 * Pass the same [InteractionSource] the click modifier uses; a fresh one silently never fires.
 */
@Composable
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.96f
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 900f),
        label = "pressScale"
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
