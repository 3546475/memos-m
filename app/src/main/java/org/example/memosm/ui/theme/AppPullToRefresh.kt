package org.example.memosm.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Raw drag distance that arms the refresh. */
private val PullTriggerDistance = 64.dp

/** Diameter of the dot matrix that acts as the pull indicator. */
private val PullIndicatorSize = 32.dp

/** Gap between [AppPullToRefresh.indicatorTopPadding] and the resting dot matrix. */
private val PullIndicatorTopGap = 8.dp

/** Extra travel a pull past the threshold is allowed to add, so the dots give a little. */
private const val PullOvershootFraction = 0.18f

/**
 * Pull-to-refresh container, identical in both design languages.
 *
 * Replaces Material's `PullToRefreshBox` and Miuix's `PullToRefresh`, which animate
 * completely differently (an overlay disc with an arrow that crossfades into a ring versus a
 * header that pushes the content down and draws a ring with an orbiting dot). This version
 * keeps the app's own dot-matrix loader as the single indicator in both themes:
 *
 * - while pulling, the wave is scrubbed by the pull progress instead of looping;
 * - past the threshold every cell stays lit (and a haptic fires), the "release" cue;
 * - while refreshing it free-runs exactly like every other loading indicator;
 * - the dots float over the content, so nothing ever re-measures and no card shifts.
 *
 * [indicatorTopPadding] is the height of the floating chrome above the list (search bar and
 * tabs); the dots rest below it and slide in from behind it.
 */
@Composable
fun AppPullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    indicatorTopPadding: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val triggerPx = with(density) { PullTriggerDistance.toPx() }

    // Drag bookkeeping, all in pixels.
    val pullPx = remember { mutableFloatStateOf(0f) }
    // 0f = dots fully hidden above the chrome, 1f = resting spot; may overshoot slightly.
    val reveal = remember { mutableFloatStateOf(0f) }
    val armed = remember { mutableStateOf(false) }
    val dragging = remember { mutableStateOf(false) }
    // Set between an armed release and the hoisted isRefreshing actually turning true.
    val waiting = remember { mutableStateOf(false) }
    val settling = remember { mutableStateOf(false) }

    val refreshingState = rememberUpdatedState(isRefreshing)
    val onRefreshState = rememberUpdatedState(onRefresh)

    fun applyPull(delta: Float) {
        if (delta == 0f) return
        pullPx.floatValue = (pullPx.floatValue + delta).coerceAtLeast(0f)
        val raw = pullPx.floatValue / triggerPx
        reveal.floatValue = if (raw <= 1f) {
            raw
        } else {
            1f + ((raw - 1f) * 0.35f).coerceAtMost(PullOvershootFraction)
        }
        val nowArmed = pullPx.floatValue >= triggerPx
        if (nowArmed && !armed.value) {
            haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
        }
        armed.value = nowArmed
        dragging.value = true
    }

    suspend fun settleTo(target: Float) {
        settling.value = true
        try {
            animate(
                initialValue = reveal.floatValue,
                targetValue = target,
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) { value, _ -> reveal.floatValue = value }
        } finally {
            settling.value = false
        }
    }

    val connection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                if (refreshingState.value || waiting.value || settling.value) return Offset.Zero
                // Pulling up while the dots are out retracts them before the list scrolls.
                if (available.y >= 0f || pullPx.floatValue <= 0f) return Offset.Zero
                val before = pullPx.floatValue
                applyPull(available.y)
                return Offset(0f, pullPx.floatValue - before)
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                if (refreshingState.value || waiting.value || settling.value) return Offset.Zero
                if (available.y <= 0f) return Offset.Zero
                applyPull(available.y)
                return Offset(0f, available.y)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (refreshingState.value) return Velocity.Zero
                if (pullPx.floatValue <= 0f) {
                    dragging.value = false
                    return Velocity.Zero
                }
                val wasArmed = pullPx.floatValue >= triggerPx
                dragging.value = false
                pullPx.floatValue = 0f
                if (wasArmed) {
                    // Show the dots spinning right away; the hoisted flag should follow
                    // synchronously from onRefresh(). The timeout in the waiting effect
                    // covers a caller that never raises it.
                    waiting.value = true
                    onRefreshState.value()
                }
                return Velocity(0f, available.y)
            }
        }
    }

    // Reveal (or hide) the dots when the refresh state is driven programmatically, and put it
    // away once nothing is happening any more.
    val show = dragging.value || isRefreshing || waiting.value
    LaunchedEffect(show) {
        if (show) {
            if (!dragging.value && reveal.floatValue < 1f) settleTo(1f)
        } else {
            settleTo(0f)
            pullPx.floatValue = 0f
            armed.value = false
        }
    }

    // An armed release can overshoot; settle back to the resting spot while the refresh starts.
    LaunchedEffect(waiting.value) {
        if (!waiting.value) return@LaunchedEffect
        launch { settleTo(1f) }
        withTimeoutOrNull(1_200) {
            while (!refreshingState.value) delay(16)
        }
        if (!refreshingState.value) waiting.value = false
    }

    // A refresh that really started takes over the "waiting" bookkeeping, and a release that
    // overshot the resting spot is pulled back to it.
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            waiting.value = false
            if (reveal.floatValue > 1.001f) settleTo(1f)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .nestedScroll(connection)
    ) {
        content()

        // Always composed: the indicator reads the reveal state inside its own scope, so a pull
        // re-composes this small subtree only, never the list in `content()`.
        PullIndicator(
            reveal = reveal,
            armed = armed,
            refreshing = isRefreshing || waiting.value,
            topPadding = indicatorTopPadding + PullIndicatorTopGap
        )
    }
}

/**
 * The dot matrix alone, no container: it slides down from behind the floating chrome, at
 * reveal 0 sitting one indicator-height above its resting spot and invisible.
 */
@Composable
private fun BoxScope.PullIndicator(
    reveal: MutableFloatState,
    armed: State<Boolean>,
    refreshing: Boolean,
    topPadding: Dp
) {
    val revealValue = reveal.floatValue
    val alpha = revealValue.coerceIn(0f, 1f)
    val offsetY = topPadding - PullIndicatorSize * (1f - revealValue.coerceIn(0f, 1.2f))
    // Nothing to show and nothing running: skip the drawing entirely.
    if (alpha <= 0f && !refreshing) return

    AppDotLoader(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .offset(y = offsetY)
            .size(PullIndicatorSize)
            .graphicsLayer { this.alpha = alpha },
        progress = if (refreshing) null else revealValue.coerceIn(0f, 1f),
        armed = armed.value && !refreshing
    )
}
