package org.example.memosm.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVisibilityTest {
    @Test fun `the app counts as foreground until the last activity stops`() {
        AppVisibility.resetForTest()
        assertTrue(AppVisibility.foreground.value)

        // Opening an overlay: two activities are started, still foreground.
        AppVisibility.onActivityStarted()
        assertTrue(AppVisibility.foreground.value)
        AppVisibility.onActivityStopped()
        assertTrue(AppVisibility.foreground.value)

        // The user leaves: every activity stopped.
        AppVisibility.onActivityStopped()
        assertFalse(AppVisibility.foreground.value)

        // Returning resumes it immediately.
        AppVisibility.onActivityStarted()
        assertTrue(AppVisibility.foreground.value)

        // Unbalanced stops (e.g. a crash in one activity) must not drift negative.
        repeat(3) { AppVisibility.onActivityStopped() }
        assertFalse(AppVisibility.foreground.value)
        AppVisibility.onActivityStarted()
        assertTrue(AppVisibility.foreground.value)
    }
}
