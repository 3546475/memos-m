package org.example.moxia.data

import org.example.moxia.model.TopBarScrollMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The top bar used to have three modes (NEVER / SEARCH_ONLY / ALL); it now has two, and stored
 * values from before the change must still resolve instead of silently falling back.
 */
class TopBarScrollModeTest {
    @Test
    fun `current names resolve`() {
        assertEquals(TopBarScrollMode.ALWAYS, topBarScrollModeOf("ALWAYS"))
        assertEquals(TopBarScrollMode.ON_SCROLL, topBarScrollModeOf("ON_SCROLL"))
    }

    @Test
    fun `retired names map onto the two remaining modes`() {
        assertEquals(TopBarScrollMode.ALWAYS, topBarScrollModeOf("NEVER"))
        assertEquals(TopBarScrollMode.ON_SCROLL, topBarScrollModeOf("ALL"))
        assertEquals(TopBarScrollMode.ON_SCROLL, topBarScrollModeOf("SEARCH_ONLY"))
    }

    @Test
    fun `unknown and missing values stay unresolved`() {
        assertNull(topBarScrollModeOf(null))
        assertNull(topBarScrollModeOf(""))
        assertNull(topBarScrollModeOf("SOMETIMES"))
    }
}
