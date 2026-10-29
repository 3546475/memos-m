package org.example.moxia.ui.theme

import top.yukonga.miuix.kmp.blur.BackdropEffectScope
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.runtimeShaderEffect

/*
 * The droplet lens of the liquid-glass bead: a rounded rect of clear glass lying on the bar.
 *
 * The bead magnifies what is under it, and near its rim the surface turns away: samples are pulled
 * inwards there, and red and blue are pulled by slightly different amounts. That band is the whole
 * of the "edge" of the effect — away from the rim the lens is a plain magnifier, which is why the
 * middle of the bead shows no colour split at all.
 *
 * The bead's rectangle is passed in rather than taken from the effect's own size: the effect is
 * drawn over the whole bar so that its sampling coordinates are the bar's, and the bead is only the
 * part of that surface the lens covers. That also keeps the lens anchored while the bead grows,
 * travels and shrinks, none of which move the surface it reads.
 *
 * The distance and gradient are the rounded-box pair from Kyant0's Backdrop catalog
 * (`com.kyant.backdrop.internal.Shaders`), Apache-2.0,
 * https://github.com/Kyant0/AndroidLiquidGlass.
 */

private const val RoundedBoxSdf = """
float sdRoundedBox(float2 coord, float2 halfSize, float radius) {
    float2 q = abs(coord) - halfSize + float2(radius);
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
}

float2 gradRoundedBox(float2 coord, float2 halfSize, float radius) {
    float2 q = abs(coord) - halfSize + float2(radius);
    if (q.x >= 0.0 || q.y >= 0.0) {
        return sign(coord) * normalize(max(q, float2(1e-5)));
    }
    float gradX = step(q.y, q.x);
    return sign(coord) * float2(gradX, 1.0 - gradX);
}"""

/**
 * The bead.
 *
 * - the surface is read from `1 / magnification` of the pixel's distance to [center], so what the
 *   bead covers is enlarged;
 * - within [rimHeight] of the bead's rim the samples are pulled towards [center] by up to
 *   [bendDepth]: that is the bead's curvature, the part that reads as refraction;
 * - red and blue follow that same bend by slightly different amounts ([dispersion]), so the fringe
 *   appears along the rim and nowhere else.
 *
 * Every sample stays inside the bead's own field, which is narrower than the bead itself, so the
 * lens never reads past the surface it lies on.
 */
private const val LiquidDropletShader = """
uniform shader content;

uniform float2 center;
uniform float2 halfSize;
uniform float cornerRadius;
uniform float magnification;
uniform float rimHeight;
uniform float bendDepth;
uniform float dispersion;

$RoundedBoxSdf

half4 main(float2 coord) {
    float2 halfBead = max(halfSize, float2(1.0));
    float2 centered = coord - center;
    float radius = clamp(cornerRadius, 0.0, min(halfBead.x, halfBead.y));

    float sd = sdRoundedBox(centered, halfBead, radius);
    // 0 at the rim, 1 once the band is behind us: only the rim bends.
    float band = clamp(-sd / max(rimHeight, 1.0), 0.0, 1.0);
    float bend = (1.0 - band) * (1.0 - band);

    float2 lensed = centered / max(magnification, 1.0);
    float2 dir = gradRoundedBox(centered, halfBead, radius);
    float2 base = lensed - dir * (bendDepth * bend) + center;

    half4 green = content.eval(base);
    if (dispersion <= 0.0) {
        return green;
    }
    float spread = bendDepth * bend * dispersion;
    half4 red = content.eval(base - dir * spread);
    half4 blue = content.eval(base + dir * spread);
    return half4(red.r, green.g, blue.b, green.a);
}"""

/**
 * Draws the droplet lens for a bead of [halfWidth] x [halfHeight] centred on [centerX]/[centerY].
 *
 * All values are in the surface's own pixels — the effect is drawn over the bar, so they are the
 * bar's pixels.
 *
 * @param magnification How much bigger the bead reads; 1.0 disables the lens.
 * @param rimHeight Width of the band along the bead's rim where the refraction happens.
 * @param bendDepth How far that rim pulls its samples inwards.
 * @param dispersion Relative spread between the red and blue samples along that bend; 0 disables
 *   the fringes.
 */
internal fun BackdropEffectScope.liquidDroplet(
    magnification: Float,
    centerX: Float,
    centerY: Float,
    halfWidth: Float,
    halfHeight: Float,
    rimHeight: Float,
    bendDepth: Float,
    dispersion: Float
) {
    if (!isRuntimeShaderSupported()) return
    if (magnification <= 1f) return
    // A blur earlier in the same effect block records the backdrop at 1 / downscaleFactor
    // resolution, so every pixel-space uniform is expressed in that layer's pixels.
    val scale = downscaleFactor.coerceAtLeast(1).toFloat()
    runtimeShaderEffect(
        key = "LiquidDroplet",
        shaderString = LiquidDropletShader,
        uniformShaderName = "content"
    ) {
        setFloatUniform("center", centerX / scale, centerY / scale)
        setFloatUniform("halfSize", halfWidth / scale, halfHeight / scale)
        setFloatUniform("cornerRadius", halfHeight / scale)
        setFloatUniform("magnification", magnification)
        setFloatUniform("rimHeight", rimHeight.coerceAtLeast(1f) / scale)
        setFloatUniform("bendDepth", bendDepth.coerceAtLeast(0f) / scale)
        setFloatUniform("dispersion", dispersion.coerceAtLeast(0f))
    }
}
