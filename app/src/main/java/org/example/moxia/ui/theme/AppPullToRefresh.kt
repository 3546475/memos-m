package org.example.moxia.ui.theme

import android.os.SystemClock
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
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
private val PullTriggerDistance = 96.dp

/**
 * Raw drag distance swallowed before the pull reacts at all. Without it a two-pixel drift, the
 * tail of a tap or a scroll that overshoots the top all made the chip pop out.
 */
private val PullStartDistance = 24.dp

/**
 * A pull that begins right after the list was still scrolling is that scroll spilling over the top
 * edge, not a deliberate pull: a hard flick back to the top must not turn into a refresh. The drag
 * is ignored for this long, then the pull starts counting from zero.
 */
private const val PullSpillOverCooldownMs = 200L

/** Diameter of the circular chip that carries the pull-to-refresh dot matrix. */
private val PullIndicatorSize = 40.dp

/** Gap between [AppPullToRefresh.indicatorTopPadding] and the resting dot matrix. */
private val PullIndicatorTopGap = 8.dp

/** Extra travel a pull past the threshold is allowed to add, so the dots give a little. */
private const val PullOvershootFraction = 0.18f

/**
 * Pull-to-refresh container for the Miuix-only app.
 *
 * Replaces the framework pull-to-refresh implementations, which animate
 * completely differently (an overlay disc with an arrow that crossfades into a ring versus a
 * header that pushes the content down and draws a ring with an orbiting dot). This version
 * keeps the app's own dot-matrix loader as the single indicator in both themes:
 *
 * - the first [PullStartDistance] of a pull does nothing at all, so small drags never show the chip;
 * - past that the wave is scrubbed by the pull progress instead of looping, and a light tick marks
 *   the moment the pull starts;
 * - [PullTriggerDistance] arms the refresh: every cell stays lit (and a haptic fires), the
 *   "release" cue;
 * - a pull that starts as scroll spills over the top is ignored for [PullSpillOverCooldownMs];
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
    val startPx = with(density) { PullStartDistance.toPx() }
    // Pull progress is measured past the dead zone, so the chip appears at startPx and is fully
    // out at triggerPx.
    val rampPx = (triggerPx - startPx).coerceAtLeast(1f)

    // Drag bookkeeping, all in pixels.
    val pullPx = remember { mutableFloatStateOf(0f) }
    // 0f = dots fully hidden above the chrome, 1f = resting spot; may overshoot slightly.
    val reveal = remember { mutableFloatStateOf(0f) }
    val armed = remember { mutableStateOf(false) }
    val dragging = remember { mutableStateOf(false) }
    // Set between an armed release and the hoisted isRefreshing actually turning true.
    val waiting = remember { mutableStateOf(false) }
    val settling = remember { mutableStateOf(false) }
    // Uptime of the last drag the list itself consumed, for the spill-over window above.
    val lastListScrollAt = remember { mutableLongStateOf(0L) }

    val refreshingState = rememberUpdatedState(isRefreshing)
    val onRefreshState = rememberUpdatedState(onRefresh)

    fun applyPull(delta: Float) {
        if (delta == 0f) return
        val wasPulling = pullPx.floatValue > startPx
        pullPx.floatValue = (pullPx.floatValue + delta).coerceAtLeast(0f)
        val pulling = pullPx.floatValue > startPx
        if (pulling && !wasPulling) {
            // Tell the finger the pull has actually started; before this nothing reacted at all.
            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        }
        dragging.value = dragging.value || pulling
        val raw = ((pullPx.floatValue - startPx) / rampPx).coerceAtLeast(0f)
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
                // The list moved, so whatever is left over is not a deliberate pull yet.
                if (consumed.y != 0f) lastListScrollAt.longValue = SystemClock.uptimeMillis()
                if (available.y <= 0f) return Offset.Zero
                // A pull that only started because the list ran out of content under the finger
                // (the tail of a flick back to the top) waits out the spill-over window first.
                if (pullPx.floatValue == 0f &&
                    SystemClock.uptimeMillis() - lastListScrollAt.longValue < PullSpillOverCooldownMs
                ) {
                    return Offset.Zero
                }
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
 * Floating circular chip with the dot matrix inside. It slides down from behind the floating
 * chrome: at reveal 0 it sits one chip above its resting spot and invisible.
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

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = if (alpha > 0.05f) 3.dp else 0.dp,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .offset(y = offsetY)
            .size(PullIndicatorSize)
            .graphicsLayer { this.alpha = alpha }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            AppDotLoader(
                modifier = Modifier.fillMaxSize(),
                progress = if (refreshing) null else revealValue.coerceIn(0f, 1f),
                armed = armed.value && !refreshing
            )
        }
    }
}
