package org.example.memosm.ui.component.item

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.example.memosm.ui.theme.AppCard

/**
 * Placeholder card shown while the feed has nothing to render yet: a cold start, or the first
 * page still in flight. It mirrors the memo card's header row and body lines, so the layout does
 * not jump when the real cards arrive, and it breathes instead of spinning a loader.
 *
 * The pulse is one shared infinite transition per card; there is no shimmer shader, so a column
 * of skeletons stays cheap to draw while the main thread is still busy with startup.
 */
@Composable
fun MemoSkeletonCard(modifier: Modifier = Modifier) {
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

    AppCard(modifier = modifier.fillMaxWidth(), cornerRadius = 16.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 13.dp, end = 16.dp, bottom = 15.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(bar)
                )
                Spacer(Modifier.width(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    SkeletonBar(color = bar, modifier = Modifier.width(96.dp), height = 10.dp)
                    SkeletonBar(color = bar, modifier = Modifier.width(56.dp), height = 8.dp)
                }
            }
            Spacer(Modifier.height(14.dp))
            SkeletonBar(color = bar, modifier = Modifier.fillMaxWidth(), height = 10.dp)
            Spacer(Modifier.height(8.dp))
            SkeletonBar(color = bar, modifier = Modifier.fillMaxWidth(), height = 10.dp)
            Spacer(Modifier.height(8.dp))
            SkeletonBar(color = bar, modifier = Modifier.fillMaxWidth(0.62f), height = 10.dp)
        }
    }
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
