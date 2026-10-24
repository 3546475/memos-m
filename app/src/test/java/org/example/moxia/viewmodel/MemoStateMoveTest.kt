package org.example.moxia.viewmodel

import org.example.moxia.model.MemoState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Archiving a memo has to move it out of the timeline immediately, and unarchiving has to move it
 * back. The list move itself lives in the view model; this covers the decision that drives it.
 */
class MemoStateMoveTest {

    @Test
    fun `archiving a normal memo moves it to the archive`() {
        assertEquals(
            MemoStateMove.TO_ARCHIVED,
            memoStateMove(MemoState.NORMAL, MemoState.ARCHIVED)
        )
    }

    @Test
    fun `a memo without a state is treated as normal`() {
        assertEquals(
            MemoStateMove.TO_ARCHIVED,
            memoStateMove(null, MemoState.ARCHIVED)
        )
        assertEquals(
            MemoStateMove.TO_ARCHIVED,
            memoStateMove(MemoState.NORMAL, MemoState.ARCHIVED)
        )
    }

    @Test
    fun `unarchiving moves the memo back to the timeline`() {
        assertEquals(
            MemoStateMove.TO_USER,
            memoStateMove(MemoState.ARCHIVED, MemoState.NORMAL)
        )
        assertEquals(
            MemoStateMove.TO_USER,
            memoStateMove(MemoState.ARCHIVED, null)
        )
    }

    @Test
    fun `an unchanged state moves nothing`() {
        assertNull(memoStateMove(MemoState.NORMAL, MemoState.NORMAL))
        assertNull(memoStateMove(MemoState.ARCHIVED, MemoState.ARCHIVED))
        assertNull(memoStateMove(null, null))
        assertNull(memoStateMove(null, MemoState.NORMAL))
    }

    @Test
    fun `an unspecified target state moves nothing`() {
        assertNull(memoStateMove(MemoState.NORMAL, MemoState.STATE_UNSPECIFIED))
        assertNull(memoStateMove(MemoState.ARCHIVED, MemoState.STATE_UNSPECIFIED))
    }
}
