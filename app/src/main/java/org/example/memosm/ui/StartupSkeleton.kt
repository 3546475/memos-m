package org.example.memosm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.example.memosm.data.StartupMirror
import org.example.memosm.ui.component.item.MemoSkeletonCard
import org.example.memosm.ui.theme.AppCircularProgressIndicator

/**
 * Launch placeholder for the gates that run before the home page can compose.
 *
 * With a known onboarded account ([showHomeShell]) it draws the home shell's silhouette — the app
 * bar, the skeleton feed — so a cold start goes from the system splash to the shape of the page
 * instead of a spinner. Without that hint it keeps the plain spinner: the app cannot yet tell
 * whether it is heading for the feed or for setup/login, and a wrong shell would flash before the
 * real screen.
 */
@Composable
fun StartupPlaceholder(showHomeShell: Boolean, modifier: Modifier = Modifier) {
    if (!showHomeShell) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AppCircularProgressIndicator()
        }
        return
    }
    StartupSkeletonShell(modifier)
}

@Composable
private fun StartupSkeletonShell(modifier: Modifier) {
    val chrome = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    // The first card starts below the app bar (the list pane's own top) and below the space the
    // feed reserves for its header. Both numbers are mirrored by the real screen on layout, so the
    // placeholders land exactly where the cards will.
    val feedTop = (StartupMirror.feedPaneTopDp + StartupMirror.feedTopInsetDp).dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clipToBounds()
    ) {
        Column(Modifier.fillMaxSize()) {
            // The app bar's silhouette: the tab row on the left, the action icons on the right,
            // over the same band the real chrome covers. The rest of the band is the gap the feed
            // reserves for its own header.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(feedTop)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = statusBar + 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ChromeBar(chrome, width = 34.dp)
                    ChromeBar(chrome, width = 24.dp)
                    Spacer(Modifier.weight(1f))
                    ChromeDot(chrome)
                    ChromeDot(chrome)
                }
            }
            // The feed area: the same placeholder cards the first page will replace in place.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(5) { MemoSkeletonCard() }
            }
        }
    }
}

@Composable
private fun ChromeBar(color: Color, width: Dp) {
    Box(
        modifier = Modifier
            .size(width = width, height = 12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(color)
    )
}

@Composable
private fun ChromeDot(color: Color) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(color)
    )
}
