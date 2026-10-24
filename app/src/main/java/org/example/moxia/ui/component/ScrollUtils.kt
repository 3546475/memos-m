package org.example.moxia.ui.component

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.unit.Velocity

/**
 * The overscroll effect for a page-level list: none.
 *
 * A page should move only as far as its content goes. The platform effect steps outside that: it
 * stretches the page and springs it back both when everything already fits (there is nothing to
 * scroll at all) and at the ends of a long list, where a fling's leftover velocity turns into a
 * visible bounce. The floating pull-to-refresh chip above the list is the app's feedback for a
 * pull instead, and the gesture itself stays untouched, so that chip still receives the drag.
 */
@Composable
fun rememberPageOverscroll(): OverscrollEffect? {
    // Wraps the platform effect rather than returning null so the drawing node stays the platform's
    // own - the scroll container attaches that node, and only the deltas are dropped here.
    val platform = rememberOverscrollEffect() ?: return null
    return remember(platform) { NoPageOverscroll(platform) }
}

/** The gate itself, kept internal so a unit test can drive it with fakes. */
internal class NoPageOverscroll(
    private val platform: OverscrollEffect
) : OverscrollEffect {

    override val isInProgress: Boolean get() = false

    override val node: DelegatableNode get() = platform.node

    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset
    ): Offset = performScroll(delta)

    override suspend fun applyToFling(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity
    ) {
        performFling(velocity)
    }
}

@Composable
fun rememberScrollContext(
    listState: LazyListState, onScrollDown: () -> Unit = {}, onScrollUp: () -> Unit = {}
): ScrollContext {
    return rememberScrollDirection(
        listState,
        position = { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset },
        isScrolling = { listState.isScrollInProgress }, onScrollDown = onScrollDown, onScrollUp = onScrollUp
    )
}

@Composable
fun rememberStaggeredGridScrollContext(
    listState: LazyStaggeredGridState, onScrollDown: () -> Unit = {}, onScrollUp: () -> Unit = {}
): ScrollContext {
    return rememberScrollDirection(
        listState,
        position = { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset },
        isScrolling = { listState.isScrollInProgress }, onScrollDown = onScrollDown, onScrollUp = onScrollUp
    )
}

@Composable
private fun rememberScrollDirection(
    state: Any,
    position: () -> Pair<Int, Int>,
    isScrolling: () -> Boolean,
    onScrollDown: () -> Unit,
    onScrollUp: () -> Unit
): ScrollContext {
    val scrollContext = remember(state) { ScrollContext() }
    val scrollDown by rememberUpdatedState(onScrollDown)
    val scrollUp by rememberUpdatedState(onScrollUp)

    LaunchedEffect(state) {
        var (previousIndex, previousScrollOffset) = position()

        snapshotFlow { position() }.collect { (currentIndex, currentOffset) ->
            // Only treat user-initiated scrolling as a direction change.
            // Programmatic layout shifts - items inserted above (the drafts card
            // appearing, or the filter row changing height) - move the indices
            // without any scroll gesture, and must not flip the direction state
            // and hide the search bar / nav bar.
            if (!isScrolling()) {
                previousIndex = currentIndex
                previousScrollOffset = currentOffset
                return@collect
            }
            if (currentIndex > previousIndex) {
                scrollContext.isScrollingDown = true
                scrollDown()
            } else if (currentIndex < previousIndex) {
                scrollContext.isScrollingDown = false
                scrollUp()
            } else if (currentOffset > previousScrollOffset + 10) {
                scrollContext.isScrollingDown = true
                scrollDown()
            } else if (currentOffset < previousScrollOffset - 10) {
                scrollContext.isScrollingDown = false
                scrollUp()
            }

            previousIndex = currentIndex
            previousScrollOffset = currentOffset
        }
    }

    return scrollContext
}

class ScrollContext {
    var isScrollingDown by mutableStateOf(false)
}
