package org.example.memosm.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** 5x5 cells, matching docs/loader-1.js. */
private const val DotGrid = 5

/** The reference loops every 2s. */
private const val DotLoopMillis = 2_000

/** One diagonal step per eighth of the loop: the reference's cells are 15.125 samples apart. */
private const val DotDiagonalSteps = 8f

// Reference geometry: 65 unit cells with 6 unit gaps inside a 389 unit canvas.
private const val DotMarginRatio = 20f / 389f
private const val DotGapRatio = 6f / 389f
private const val DotRadiusRatio = 14.3f / 65f

private val DotMinGap = 0.75.dp

private val TwoPi = (2 * PI).toFloat()

/**
 * Dot-matrix loading animation, ported from `docs/loader-1.js` ("DotMotionLoader").
 *
 * The reference drives 25 rounded squares from 121 sampled frames per 2s loop.
 * Those samples are a pure sinusoid with a phase offset per diagonal, so the
 * same motion is reproduced analytically instead of shipping the sample table:
 * a wave travels from the top-left cell to the bottom-right one, each diagonal
 * a quarter second after the previous, while every cell fades between the
 * muted track colour and [color].
 *
 * Glow is approximated with a slightly expanded, low-alpha copy of the active
 * cell (the reference uses a canvas shadow blur, which Compose cannot draw
 * without an API 31 render effect).
 *
 * Normally the wave free-runs. [progress] scrubs it by hand instead (the
 * pull-to-refresh indicator drives it from the pull distance), and [armed]
 * lights every cell as the "release to refresh" cue.
 */
@Composable
fun AppDotLoader(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    // The reference uses a neutral dark track; deriving it from the tint keeps the grid readable
    // on tinted surfaces too (a white spinner inside a filled button must not draw grey cells).
    trackColor: Color = color.copy(alpha = 0.22f),
    progress: Float? = null,
    armed: Boolean = false,
    /**
     * Scales the whole grid down (cells, glow and track). A queued card status uses it to sit
     * quietly in the corner; the real loading indicators keep the default.
     */
    intensity: Float = 1f
) {
    // The infinite transition only exists while the wave free-runs; a scrubbed loader must not
    // burn a frame clock (and must not fight the value the caller supplies).
    val phaseState = if (progress == null) {
        val transition = rememberInfiniteTransition(label = "dot-loader")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(DotLoopMillis, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "dot-loader-phase"
        )
    } else {
        null
    }

    // Keep the indeterminate progress semantics of the ring this replaces:
    // screen readers and UI tests locate loading indicators through them.
    Canvas(modifier.progressSemantics()) {
        // Read the animation state inside the draw scope: the wave only has to
        // invalidate drawing, never recompose the indicator.
        val phase = progress ?: phaseState?.value ?: 0f
        val level = intensity.coerceIn(0f, 1f)
        drawDotMatrix(
            color = color,
            trackColor = trackColor.copy(alpha = trackColor.alpha * level)
        ) { row, col ->
            level * if (armed) {
                1f
            } else {
                0.5f - 0.5f * sin(TwoPi * (phase - (row + col) / DotDiagonalSteps))
            }
        }
    }
}

/**
 * Static dot-matrix glyph on the same 5x5 grid as [AppDotLoader], for the sync states that are
 * not activity (idle, pending queue, offline, rate limited). [cells] are the lit cells, indexed
 * as `row * 5 + column` with row 0 at the top.
 */
@Composable
fun AppDotGlyph(
    cells: Set<Int>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = color.copy(alpha = 0.18f)
) {
    Canvas(modifier) {
        drawDotMatrix(color, trackColor) { row, col ->
            if (row * DotGrid + col in cells) 1f else 0f
        }
    }
}

/** Shared geometry: 5x5 rounded cells in the reference's proportions, plus its soft glow. */
private fun DrawScope.drawDotMatrix(
    color: Color,
    trackColor: Color,
    intensity: (row: Int, col: Int) -> Float
) {
    val side = min(size.width, size.height)
    val margin = side * DotMarginRatio
    val gap = max(side * DotGapRatio, DotMinGap.toPx())
    val cell = (side - margin * 2f - gap * (DotGrid - 1)) / DotGrid
    if (cell <= 0f) return
    val radius = CornerRadius(cell * DotRadiusRatio)
    val glowRadius = CornerRadius(cell * DotRadiusRatio * 1.16f)

    for (row in 0 until DotGrid) {
        for (col in 0 until DotGrid) {
            val left = margin + col * (cell + gap)
            val top = margin + row * (cell + gap)
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(left, top),
                size = Size(cell, cell),
                cornerRadius = radius
            )

            val level = intensity(row, col)
            if (level <= 0.01f) continue
            drawRoundRect(
                color = color.copy(alpha = level * 0.16f),
                topLeft = Offset(left - cell * 0.08f, top - cell * 0.08f),
                size = Size(cell * 1.16f, cell * 1.16f),
                cornerRadius = glowRadius
            )
            drawRoundRect(
                color = color.copy(alpha = level),
                topLeft = Offset(left, top),
                size = Size(cell, cell),
                cornerRadius = radius
            )
        }
    }
}
