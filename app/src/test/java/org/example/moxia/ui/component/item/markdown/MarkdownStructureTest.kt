package org.example.moxia.ui.component.item.markdown

import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The block-level contract: how a paragraph is sliced, the published spacing table, and the heading
 * ladder. None of it needs composition, so a style change that drifts from
 * `docs/markdown-rendering.md` fails here instead of being noticed three releases later.
 */
class MarkdownStructureTest {
    @Test
    fun `paragraphs are sliced at explicit line breaks`() {
        val paragraph = buildAnnotatedString {
            append("第一行")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("加粗") }
            append("\n第二行\n")
        }

        val lines = paragraph.splitLines()

        // The trailing EOL must not leave an empty line behind.
        assertEquals(2, lines.size)
        assertEquals("第一行加粗", lines[0].text)
        assertEquals("第二行", lines[1].text)
        assertTrue(lines[0].spanStyles.any { it.item.fontWeight == FontWeight.Bold })
        assertTrue(lines[1].spanStyles.isEmpty())
    }

    @Test
    fun `a single-line paragraph is returned as is`() {
        val paragraph = buildAnnotatedString { append("没有换行") }

        val lines = paragraph.splitLines()

        assertEquals(1, lines.size)
        assertSame(paragraph, lines.single())
    }

    @Test
    fun `the block rhythm matches the published table`() {
        // docs/markdown-rendering.md section 6; update both together.
        assertEquals(0.5f, ParagraphSpacingEm)
        assertEquals(0.75f, HeadingSpacingEm)
        assertEquals(0.1f, ListSpacingEm)
        assertEquals(0.5f, LineBreakSpacingEm)
        assertEquals(1f, CodeBlockSpacingEm)

        // A heading always separates more than a paragraph, a paragraph more than a list line.
        assertTrue(HeadingSpacingEm > ParagraphSpacingEm)
        assertTrue(ParagraphSpacingEm > ListSpacingEm)
        assertTrue(CodeBlockSpacingEm > HeadingSpacingEm)
    }

    @Test
    fun `heading levels step down in size and weight but never below the body`() {
        val ladder = (1..6).map { headingDecorationFor(it, dark = false) }

        assertEquals(1.3f, ladder.first().sizeMultiplier)
        assertEquals(1f, ladder.last().sizeMultiplier)
        assertEquals(700, ladder.first().fontWeight)
        assertTrue(ladder.all { it.sizeMultiplier >= 1f })
        ladder.zipWithNext().forEach { (bigger, smaller) ->
            assertTrue(bigger.sizeMultiplier >= smaller.sizeMultiplier)
            assertTrue(bigger.fontWeight > smaller.fontWeight)
        }
    }

    @Test
    fun `every heading level has its own colour and night is the lighter palette`() {
        val day = (1..6).map { headingDecorationFor(it, dark = false) }
        val night = (1..6).map { headingDecorationFor(it, dark = true) }

        assertEquals(6, day.map { it.color }.toSet().size)
        assertEquals(6, night.map { it.color }.toSet().size)
        day.indices.forEach { index ->
            assertNotEquals(day[index].color, night[index].color)
            // The night bar sits on a dark page, so it has to be the brighter one.
            assertTrue(night[index].color.luminance() > day[index].color.luminance())
        }
    }

    @Test
    fun `headings keep their own line height ramp`() {
        val ratios = (1..6).map { headingDecorationFor(it, dark = false).lineHeightRatio }

        assertEquals(1.2f, ratios[0])
        assertEquals(1.5f, ratios[5])
        ratios.zipWithNext().forEach { (tighter, looser) ->
            assertTrue(tighter <= looser)
        }
        assertFalse(ratios.isEmpty())
    }
}
