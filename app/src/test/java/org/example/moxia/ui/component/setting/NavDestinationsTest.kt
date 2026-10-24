package org.example.moxia.ui.component.setting

import org.example.moxia.ui.MainDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the bottom-bar destination rules. The editor used to return "every destination" from the
 * enabled list, which made each switch read as on and brought a disabled destination back on the
 * next toggle.
 */
class NavDestinationsTest {
    private val available = listOf(
        MainDestination.MEMOS,
        MainDestination.MAP,
        MainDestination.ATTACHMENTS,
        MainDestination.PROFILE
    )

    @Test
    fun `an empty preference means every destination in the default order`() {
        assertEquals(available, enabledDestinations(emptyList(), available))
    }

    @Test
    fun `disabled destinations stay disabled`() {
        val stored = listOf(
            MainDestination.MEMOS.name,
            MainDestination.ATTACHMENTS.name,
            MainDestination.PROFILE.name
        )
        val enabled = enabledDestinations(stored, available)

        assertEquals(3, enabled.size)
        assertFalse(MainDestination.MAP in enabled)
    }

    @Test
    fun `map and attachments can both be turned off`() {
        val stored = listOf(MainDestination.MEMOS.name, MainDestination.PROFILE.name)
        val enabled = enabledDestinations(stored, available)

        assertTrue(MainDestination.MEMOS in enabled)
        assertTrue(MainDestination.PROFILE in enabled)
        assertFalse(MainDestination.MAP in enabled)
        assertFalse(MainDestination.ATTACHMENTS in enabled)
    }

    @Test
    fun `turning one off does not revive the previously disabled one`() {
        // Walk: turn map off, then attachments off, starting from the default (all enabled).
        val afterMap = setDestinationEnabled(available, MainDestination.MAP, checked = false)
        val afterAttachments = setDestinationEnabled(afterMap, MainDestination.ATTACHMENTS, checked = false)

        assertEquals(
            listOf(MainDestination.MEMOS, MainDestination.PROFILE),
            afterAttachments
        )
    }

    @Test
    fun `the last destination cannot be turned off`() {
        val onlyOne = listOf(MainDestination.MEMOS)
        assertEquals(onlyOne, setDestinationEnabled(onlyOne, MainDestination.MEMOS, checked = false))
    }

    @Test
    fun `turning one back on appends it`() {
        val enabled = listOf(MainDestination.MEMOS, MainDestination.PROFILE)
        val restored = setDestinationEnabled(enabled, MainDestination.MAP, checked = true)
        assertEquals(
            listOf(MainDestination.MEMOS, MainDestination.PROFILE, MainDestination.MAP),
            restored
        )
    }

    @Test
    fun `rows list the enabled destinations first and keep the disabled ones reachable`() {
        val enabled = listOf(MainDestination.PROFILE, MainDestination.MEMOS)
        val rows = destinationRows(enabled, available)

        assertEquals(listOf(MainDestination.PROFILE, MainDestination.MEMOS), rows.take(2))
        assertEquals(available.toSet(), rows.toSet())
    }

    @Test
    fun `reordering respects the bounds`() {
        val enabled = listOf(MainDestination.MEMOS, MainDestination.PROFILE)
        assertEquals(
            listOf(MainDestination.PROFILE, MainDestination.MEMOS),
            moveDestination(enabled, position = 1, delta = -1)
        )
        // Moving the first item up is a no-op rather than an exception.
        assertEquals(enabled, moveDestination(enabled, position = 0, delta = -1))
        assertEquals(enabled, moveDestination(enabled, position = 1, delta = 1))
    }
}
