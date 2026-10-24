package org.example.moxia.ui.component.filter

import org.example.moxia.ui.component.buildTagTree
import org.example.moxia.ui.component.collapsibleTagPaths
import org.example.moxia.ui.component.flattenTagTree
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterTagTreeTest {

    private val counts = mapOf(
        "work" to 2,
        "type/book" to 1,
        "type/movie" to 3,
        "type/book/fiction" to 1
    )
    private val tree = buildTagTree(counts)

    @Test
    fun `the tree nests slash-separated tags and sums branch counts`() {
        val rows = flattenTagTree(tree, emptySet())
        assertEquals(
            listOf("type", "type/book", "type/book/fiction", "type/movie", "work"),
            rows.map { it.path }
        )
        // Branch counts follow TagTreeDialog: own count when the branch is itself a tag, else the
        // sum of the immediate children.
        assertEquals(4, rows.first { it.path == "type" }.count)
        assertEquals(1, rows.first { it.path == "type/book" }.count)
    }

    @Test
    fun `collapsing a branch hides its children`() {
        val collapsible = collapsibleTagPaths(tree)
        assertTrue("type" in collapsible)
        assertTrue("type/book" in collapsible)
        assertFalse("work" in collapsible)
        assertEquals(
            listOf("type", "type/book", "type/movie", "work"),
            flattenTagTree(tree, setOf("type/book")).map { it.path }
        )
    }

    @Test
    fun `search keeps matches and their ancestors`() {
        val rows = flattenTagTree(filterTagTree(tree, "book"), emptySet())
        assertEquals(
            listOf("type", "type/book", "type/book/fiction"),
            rows.map { it.path }
        )
    }

    @Test
    fun `search on a branch keeps its whole subtree`() {
        val filtered = filterTagTree(tree, "type")
        assertEquals(listOf("type"), filtered.map { it.path })
        assertEquals(
            listOf("type", "type/book", "type/book/fiction", "type/movie"),
            flattenTagTree(filtered, emptySet()).map { it.path }
        )
    }

    @Test
    fun `search is case-insensitive and unknown terms prune everything`() {
        assertEquals(
            listOf("type", "type/book", "type/book/fiction"),
            flattenTagTree(filterTagTree(tree, "BOOK"), emptySet()).map { it.path }
        )
        assertTrue(filterTagTree(tree, "absent").isEmpty())
    }

    @Test
    fun `an empty query returns the same tree`() {
        assertSame(tree, filterTagTree(tree, "  "))
    }

    @Test
    fun `a branch knows when one of its descendants is selected`() {
        assertTrue(hasSelectedDescendant(setOf("type/book"), "type"))
        assertTrue(hasSelectedDescendant(setOf("type/book/fiction"), "type"))
        assertFalse(hasSelectedDescendant(setOf("type"), "type"))
        // A sibling whose name merely starts with the path is not a descendant.
        assertFalse(hasSelectedDescendant(setOf("typebook"), "type"))
    }
}
