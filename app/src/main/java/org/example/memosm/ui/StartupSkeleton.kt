package org.example.memosm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import org.example.memosm.data.StartupMirror
import org.example.memosm.ui.component.item.MemoSkeletonCard
import org.example.memosm.ui.theme.AppCircularProgressIndicator

/**
 * Launch placeholder for the gates that run before the home page can compose.
 *
 * With a known onboarded account ([showHomeShell]) it draws the home shell's silhouette — the
 * chrome band, the skeleton feed, the floating bottom bar — so a cold start goes from the system
 * splash to the shape of the page instead of a spinner. Without that hint it keeps the plain
 * spinner: the app cannot yet tell whether it is heading for the feed or for setup/login, and a
 * wrong shell would flash before the real screen.
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
    val navBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val chrome = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    // The feed's first card starts here; the placeholders use the same offset, so the reveal does
    // not move them. The value was mirrored by the real feed on the previous run.
    val feedTop = StartupMirror.feedTopInsetDp.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clipToBounds()
    ) {
        Column(Modifier.fillMaxSize()) {
            // Chrome band: as tall as the feed reserves for its header, with a search-bar hint.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(feedTop),
                contentAlignment = Alignment.BottomStart
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(chrome)
                )
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
            // The bottom bar: a floating capsule above the navigation inset.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(navBar + 72.dp)
                    .padding(start = 40.dp, end = 40.dp, top = 8.dp, bottom = navBar + 8.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            )
        }
    }
}
