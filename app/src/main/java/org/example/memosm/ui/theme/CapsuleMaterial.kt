package org.example.memosm.ui.theme

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.pow
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop

/**
 * The floating bottom bar's "glass" rim, shared by every surface that adopted the same material.
 *
 * It is two strokes: a narrow white bevel that is brightest at the top, and a fainter glow just
 * inside it. Both are white with different alphas in each theme, because a white line over a white
 * card and a white line over a black one read very differently.
 */
val CapsuleEdgeWidth = 1.dp
val CapsuleGlowWidth = 2.dp

fun capsuleEdgeBrush(lightSurface: Boolean): Brush = if (lightSurface) {
    Brush.verticalGradient(
        0f to Color.White.copy(alpha = 0.55f),
        0.45f to Color.White.copy(alpha = 0.18f),
        1f to Color.White.copy(alpha = 0.08f)
    )
} else {
    Brush.verticalGradient(
        0f to Color.White.copy(alpha = 0.35f),
        0.4f to Color.White.copy(alpha = 0.15f),
        1f to Color.White.copy(alpha = 0.20f)
    )
}

fun capsuleGlowBrush(lightSurface: Boolean): Brush = if (lightSurface) {
    Brush.verticalGradient(
        0f to Color.White.copy(alpha = 0.05f),
        1f to Color.White.copy(alpha = 0f)
    )
} else {
    Brush.verticalGradient(
        0f to Color.White.copy(alpha = 0.10f),
        0.5f to Color.White.copy(alpha = 0.03f),
        1f to Color.White.copy(alpha = 0.05f)
    )
}

/**
 * Paints the rim over whatever the modified node already drew.
 *
 * @param lightSurface picks the day or night alphas; pass `surface.luminance() > 0.5f`.
 * @param cornerRadius the shape's radius, or null for a stadium (the height's half, which is what
 *   a pill and a circle both use).
 */
fun Modifier.capsuleHighlight(
    lightSurface: Boolean,
    cornerRadius: Dp? = null,
    edgeWidth: Dp = CapsuleEdgeWidth,
    glowWidth: Dp = CapsuleGlowWidth
): Modifier = drawWithContent {
    drawContent()
    val edgePx = edgeWidth.toPx()
    val glowPx = glowWidth.toPx()
    if (size.width <= 0f || size.height <= 0f) return@drawWithContent
    val radius = cornerRadius?.toPx() ?: (size.height / 2f)
    drawRoundRect(
        brush = capsuleGlowBrush(lightSurface),
        topLeft = Offset(glowPx / 2f, glowPx / 2f),
        size = Size(size.width - glowPx, size.height - glowPx),
        cornerRadius = CornerRadius((radius - glowPx / 2f).coerceAtLeast(0f)),
        style = Stroke(width = glowPx)
    )
    drawRoundRect(
        brush = capsuleEdgeBrush(lightSurface),
        topLeft = Offset(edgePx / 2f, edgePx / 2f),
        size = Size(size.width - edgePx, size.height - edgePx),
        cornerRadius = CornerRadius((radius - edgePx / 2f).coerceAtLeast(0f)),
        style = Stroke(width = edgePx)
    )
}

/** True when the theme's surface is a light one, i.e. the day palette is in use. */
fun isLightSurface(color: Color): Boolean = color.luminance() > 0.5f

/**
 * The full floating-bar material: frosted backdrop, translucent fill, then the rim.
 *
 * The rim alone is only half of it — without the blur the surface reads as a flat pill. Every
 * surface that adopted the rim has to sample something behind it, which is why this takes the
 * caller's recorded [Backdrop].
 *
 * @param shape the surface's outline; the blur is clipped to it so the fill cannot bleed.
 * @param cornerRadius the same radius for the rim, or null when [shape] is a stadium.
 */
fun Modifier.capsuleGlass(
    backdrop: Backdrop,
    surfaceColor: Color,
    /** Read from the page surface by the caller, exactly like the bar. */
    lightSurface: Boolean,
    shape: Shape,
    cornerRadius: Dp? = null,
    // The floating navigation bar blurs at 24 px (MainScreen's NavBlurRadius); every surface that
    // wears this material uses the same value so they frost identically.
    blurRadius: Float = 24f
): Modifier = this
    .clip(shape)
    // Exactly the floating bar's effect list: one blur, nothing chained after it. Any extra effect
    // (colour grading, noise) makes this surface stop matching the bar it is supposed to copy.
    .drawBackdrop(
        backdrop = backdrop,
        shape = { RectangleShape },
        effects = { blur(blurRadius, blurRadius) }
    )
    .background(surfaceColor)
    .capsuleHighlight(lightSurface = lightSurface, cornerRadius = cornerRadius)

/**
 * The floating bar's shadow: a stack of faint capsules rather than
 * [androidx.compose.ui.draw.shadow].
 *
 * It is drawn in the same pass as the surface, so a fade carries it along instead of letting it pop
 * in a beat late. Day keeps a soft ambient shadow; night has none, because the reference bar is
 * separated from black by its rim and a dark halo over gray cards is exactly the effect to avoid.
 *
 * @param cornerRadius the shape's radius, or null for a stadium.
 * @param appear 0..1 fade for the whole shadow.
 */
fun Modifier.capsuleShadow(
    lightSurface: Boolean,
    cornerRadius: Dp? = null,
    spread: Dp = CapsuleShadowSpread,
    offsetY: Dp = CapsuleShadowOffsetY,
    layers: Int = CapsuleShadowLayers,
    appear: Float = 1f
): Modifier = drawWithContent {
    // A node that has not been laid out yet measures a few pixels, and a shadow drawn from that is
    // a solid square that then "snaps" into a capsule. Radius is also taken from the caller (a
    // fixed value) rather than the measured height for the same reason.
    val minHeightPx = 20.dp.toPx()
    if (size.height >= minHeightPx && size.width > 0f && appear > 0f) {
        val maxAlpha = if (lightSurface) 0.05f else 0f
        if (maxAlpha > 0f) {
            // Each stacked capsule is faint; together they reach maxAlpha at the surface and fade to
            // nothing at the edge of the spread.
            val layerAlpha = 1f - (1f - maxAlpha).pow(1f / layers)
            val stepPx = spread.toPx() / layers
            val offsetPx = offsetY.toPx()
            val radius = cornerRadius?.toPx() ?: (size.height / 2f)
            for (i in layers downTo 1) {
                val grow = stepPx * i
                drawRoundRect(
                    color = Color.Black.copy(alpha = layerAlpha * appear),
                    topLeft = Offset(-grow, -grow + offsetPx),
                    size = Size(size.width + grow * 2f, size.height + grow * 2f),
                    cornerRadius = CornerRadius(radius + grow)
                )
            }
        }
    }
    drawContent()
}

/** Spread of that shadow, matching the floating bar. */
val CapsuleShadowSpread = 34.dp

/** Downward offset of that shadow. */
val CapsuleShadowOffsetY = 4.dp

/** Layer count; more layers read as a smoother gradient. */
const val CapsuleShadowLayers = 16
