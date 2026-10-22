package org.example.memosm

import org.example.memosm.model.Memo
import org.example.memosm.model.MemoContentType
import org.example.memosm.model.matchesContentType
import org.example.memosm.ui.component.buildTagTree
import org.example.memosm.ui.component.collapsibleTagPaths
import org.example.memosm.ui.component.flattenTagTree
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The profile stat card filters on this device, so the two pure pieces are pinned down here. */
class ProfileStatsFiltersTest {

    @Test
    fun `tag tree nests by path and folds whole branches`() {
        val tree = buildTagTree(
            mapOf(
                "#work/notes" to 2,
                "#idea" to 5,
                "#work" to 9
            )
        )

        assertEquals(listOf("idea", "work"), tree.map { it.path })
        assertEquals(1, tree[1].children.size)
        assertEquals("work/notes", tree[1].children[0].path)

        // Everything visible at first.
        assertEquals(listOf("idea", "work", "work/notes"), flattenTagTree(tree, emptySet()).map { it.path })

        // Folding "work" hides its child.
        assertEquals(listOf("idea", "work"), flattenTagTree(tree, setOf("work")).map { it.path })
        assertEquals(setOf("work"), collapsibleTagPaths(tree))
    }

    @Test
    fun `link detection accepts http and www links only`() {
        assertTrue(memo("see https://example.com/x").matchesContentType(MemoContentType.LINK))
        assertTrue(memo("www.example.com").matchesContentType(MemoContentType.LINK))
        assertFalse(memo("no link here").matchesContentType(MemoContentType.LINK))
    }

    @Test
    fun `code detection covers fences and inline spans`() {
        assertTrue(memo("```kotlin\nval x = 1\n```").matchesContentType(MemoContentType.CODE))
        assertTrue(memo("use `println()` here").matchesContentType(MemoContentType.CODE))
        assertFalse(memo("plain prose").matchesContentType(MemoContentType.CODE))
    }

    @Test
    fun `todo detection requires a task list marker`() {
        assertTrue(memo("- [ ] buy milk").matchesContentType(MemoContentType.TODO))
        assertTrue(memo("- [x] done").matchesContentType(MemoContentType.TODO))
        assertFalse(memo("- plain bullet").matchesContentType(MemoContentType.TODO))
    }

    private fun memo(content: String) = Memo(content = content)
}
