package org.example.memosm.ui.component

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Pins the page-level scroll rule: the platform overscroll never sees a delta, so a page only moves
 * while it has content to reveal - no stretch when it already fits, no bounce at the ends of a long
 * list - while the scroll itself and its fling still run through the usual callbacks.
 */
class NoPageOverscrollTest {

    @Test
    fun `scroll deltas go to the list, never to the platform effect`() {
        val platform = RecordingOverscrollEffect()
        val effect = NoPageOverscroll(platform)

        var performedScrolls = 0
        val consumed = effect.applyToScroll(
            delta = Offset(0f, 24f),
            source = NestedScrollSource.UserInput
        ) { offset ->
            performedScrolls++
            offset
        }

        assertEquals(Offset(0f, 24f), consumed)
        assertEquals(1, performedScrolls)
        assertEquals(0, platform.scrollCalls)
        assertFalse(effect.isInProgress)
    }

    @Test
    fun `flings end in the list, never in a platform bounce`() = runBlocking {
        val platform = RecordingOverscrollEffect()
        val effect = NoPageOverscroll(platform)

        var performedFlings = 0
        effect.applyToFling(Velocity(0f, 2400f)) { _ ->
            performedFlings++
            Velocity.Zero
        }

        assertEquals(1, performedFlings)
        assertEquals(0, platform.flingCalls)
    }
}

/** Counts what reaches the effect the platform would have used. */
private class RecordingOverscrollEffect : OverscrollEffect {

    var scrollCalls = 0
        private set
    var flingCalls = 0
        private set

    override val isInProgress: Boolean get() = false

    override val node: DelegatableNode = object : Modifier.Node() {}

    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset
    ): Offset {
        scrollCalls++
        return performScroll(delta)
    }

    override suspend fun applyToFling(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity
    ) {
        flingCalls++
        performFling(velocity)
    }
}
