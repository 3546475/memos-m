package org.example.moxia.ui.nav

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.example.moxia.R
import org.example.moxia.model.ThemeStyle
import org.example.moxia.ui.theme.LocalThemeStyle

enum class MemoFeed(val labelRes: Int) {
    MEMOS(R.string.nav_memos),
    EXPLORE(R.string.nav_explore)
}

@Composable
internal fun MemoFeedTabs(pagerState: PagerState, modifier: Modifier = Modifier) {
    if (LocalThemeStyle.current == ThemeStyle.MIUIX) {
        MiuixFeedTabs(pagerState, modifier)
        return
    }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    PrimaryTabRow(
        selectedTabIndex = pagerState.currentPage,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.primary,
        divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
    ) {
        MemoFeed.entries.forEach { feed ->
            val selected = pagerState.currentPage == feed.ordinal
            Tab(
                selected = selected,
                onClick = {
                    focusManager.clearFocus()
                    scope.launch { pagerState.animateScrollToPage(feed.ordinal) }
                },
                selectedContentColor = MaterialTheme.colorScheme.onSurface,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                text = {
                    Text(
                        text = stringResource(feed.labelRes),
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }
    }
}

/**
 * Miuix / HyperOS renders the feed switcher as plain text plus a thin underline
 * instead of a segmented control: the top of the list is content, and a
 * full-width outlined pill up there reads as a separate widget and fights with
 * the cards underneath.
 */
@Composable
private fun MiuixFeedTabs(pagerState: PagerState, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MemoFeed.entries.forEachIndexed { index, feed ->
            val selected = pagerState.currentPage == index
            val contentColor by animateColorAsState(
                targetValue = if (selected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                label = "feedTabContent"
            )
            val indicatorAlpha by animateFloatAsState(
                targetValue = if (selected) 1f else 0f,
                label = "feedTabIndicator"
            )
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClickLabel = stringResource(feed.labelRes)
                    ) {
                        focusManager.clearFocus()
                        scope.launch { pagerState.animateScrollToPage(index) }
                    }
                    .defaultMinSize(minHeight = 40.dp, minWidth = 44.dp)
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(feed.labelRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = contentColor
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .alpha(indicatorAlpha)
                        .width(16.dp)
                        .height(3.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
            }
        }
    }
}
