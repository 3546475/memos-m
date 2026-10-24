package org.example.moxia.ui.component.item

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.example.moxia.ui.theme.AppCard

/**
 * Placeholder card shown while the feed has nothing to render yet: a cold start, or the first
 * page still in flight.
 *
 * It mirrors a real memo card's metrics — the same container, the same 12dp insets, a 28dp
 * avatar, two label-height rows for the author and time, and three bodyLarge text lines — so a
 * three-line memo lands on exactly the same height and the swap does not shift the feed. Cards
 * with different content lengths still animate their residual difference through
 * `animateItem()` in the list.
 *
 * The pulse is one infinite transition per card, no shimmer shader: a column of placeholders
 * stays cheap while the main thread is still busy with startup.
 */
@Composable
fun MemoSkeletonCard(modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    // Real text lines: the author row is labelSmall's line height (the name overrides labelLarge
    // with it too), the body renders at bodyLarge.
    val headerLine = with(density) { MaterialTheme.typography.labelSmall.lineHeight.toDp() }
    val bodyLine = with(density) { MaterialTheme.typography.bodyLarge.lineHeight.toDp() }

    val transition = rememberInfiniteTransition(label = "memo-skeleton")
    val pulse by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "memo-skeleton-pulse"
    )
    val bar = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f + 0.07f * pulse)

    AppCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = CardDefaults.cardColors().containerColor,
        cornerRadius = 16.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 12.dp, end = 4.dp, bottom = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(bar)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Line(headerLine) { SkeletonBar(bar, Modifier.width(96.dp), 10.dp) }
                    Line(headerLine) { SkeletonBar(bar, Modifier.width(56.dp), 8.dp) }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 16.dp, bottom = 8.dp)
            ) {
                Line(bodyLine) { SkeletonBar(bar, Modifier.fillMaxWidth(), 10.dp) }
                Line(bodyLine) { SkeletonBar(bar, Modifier.fillMaxWidth(), 10.dp) }
                Line(bodyLine) { SkeletonBar(bar, Modifier.fillMaxWidth(0.62f), 10.dp) }
            }
        }
    }
}

/** A text-line slot: the bar sits in the middle of the line box, so heights match real text. */
@Composable
private fun Line(height: Dp, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        contentAlignment = Alignment.CenterStart,
        content = content
    )
}

@Composable
private fun SkeletonBar(color: Color, modifier: Modifier, height: Dp) {
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(color)
    )
}
