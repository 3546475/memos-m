package org.example.moxia.ui.theme

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp

/**
 * The insets a page keeps for itself now that every screen runs edge to edge.
 *
 * The windows are edge-to-edge, so a page's own surfaces (background, list, frosted header, image
 * grid, map) have to reach the screen edges instead of stopping at the system bars. What still
 * needs a safe area is the content the user touches: list padding at the bottom, the first row
 * under the status bar, the side cutout in landscape. The vertical insets are therefore applied by
 * the pages themselves — as padding around their chrome or as scroll content padding — and never
 * to a Scaffold's whole content area, which would clip the content at the bars again.
 *
 * Sides stay reserved here: a landscape cutout can sit over the content and cannot be scrolled
 * away from.
 */
@Composable
fun pageContentWindowInsets(): WindowInsets =
    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)

/** Height of the status bar band, for content that continues underneath it. */
@Composable
fun statusBarTop(): Dp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

/** Height the gesture handle (or the three-button navigation bar) occupies at the bottom edge. */
@Composable
fun navigationBarBottom(): Dp =
    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
