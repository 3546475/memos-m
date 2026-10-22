package org.example.memosm.ui.component.item.markdown

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The code panel's text rules: tab handling and the fold threshold. */
class CodeBlockRenderingTest {
    @Test
    fun `every tab becomes four spaces`() {
        assertEquals("    val x = 1", normalizeCodeTabs("\tval x = 1"))
        assertEquals("a        b", normalizeCodeTabs("a\t\tb"))
        assertEquals("no tabs here", normalizeCodeTabs("no tabs here"))
    }

    @Test
    fun `lines are split on newlines only`() {
        assertEquals(listOf("a", "b", "c"), codeBlockLines("a\nb\nc"))
        // Interiors are preserved; the renderer strips the fence's own leading/trailing newlines
        // before this point.
        assertEquals(listOf("", "a"), codeBlockLines("\na"))
    }

    @Test
    fun `a block folds only when it is longer than the threshold`() {
        val atThreshold = (1..CodeBlockFoldLines).joinToString("\n") { "line $it" }
        assertFalse(isCodeBlockFoldable(codeBlockLines(atThreshold).size))

        val overThreshold = (1..CodeBlockFoldLines + 1).joinToString("\n") { "line $it" }
        assertTrue(isCodeBlockFoldable(codeBlockLines(overThreshold).size))
    }

    @Test
    fun `a folded block keeps the first lines and drops the rest`() {
        val lines = (1..30).map { "line $it" }

        val folded = foldedCode(lines)

        assertEquals(CodeBlockFoldLines, folded.lines().size)
        assertEquals("line 1", folded.lines().first())
        assertEquals("line $CodeBlockFoldLines", folded.lines().last())
        assertFalse(folded.contains("line ${CodeBlockFoldLines + 1}"))
    }

    @Test
    fun `slices keep one entry per source line, blank lines included`() {
        val highlighted = buildAnnotatedString {
            append("first\n")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("second") }
            append("\n\n")
            append("third")
        }

        val slices = highlighted.splitIntoSlices()

        // The wrapped layout draws one line at a time, so the slice count must match the gutter.
        assertEquals(codeBlockLines(highlighted.text).size, slices.size)
        assertEquals(4, slices.size)
        assertEquals("first", slices[0].text)
        assertEquals("second", slices[1].text)
        assertEquals("", slices[2].text)
        assertEquals("third", slices[3].text)
        // Styles stay on the line they belong to.
        assertTrue(slices[1].spanStyles.any { it.item.fontWeight == FontWeight.Bold })
        assertTrue(slices[2].spanStyles.isEmpty())
    }
}
