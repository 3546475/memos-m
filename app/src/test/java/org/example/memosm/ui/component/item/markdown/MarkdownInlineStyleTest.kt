package org.example.memosm.ui.component.item.markdown

import android.content.ContextWrapper
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the inline styles the text component actually receives.
 *
 * The builder is plain Kotlin, so this runs on the JVM without composition: it parses real markdown,
 * walks the first paragraph exactly like `NativeMarkdownNodeRecursive` does, and inspects the
 * resulting `AnnotatedString`. It exists because the link colour and decoration belong to us —
 * [MarkdownText] renders with `BasicText`, which unlike Material3's `Text` does not restyle links.
 */
@Suppress("DEPRECATION") // A stub context: only the inline-math branch touches Android, and it is
class MarkdownInlineStyleTest {
    // never reached here. Unit tests run against the mockable android.jar.
    private val context = ContextWrapper(null)

    private val styles = MarkdownStyles(
        codeBackground = Color(0xFF112233),
        linkColor = Color(0xFF4477FF),
        strikethroughStyle = SpanStyle(textDecoration = TextDecoration.LineThrough),
        boldStyle = SpanStyle(fontWeight = FontWeight.Bold),
        italicStyle = SpanStyle(fontStyle = FontStyle.Italic),
        codeFontFamily = FontFamily.Monospace,
        highlightBackground = Color(0xFFEEDDFF),
        tagBackground = Color(0xFFDDE7F0),
        tagTextColor = Color(0xFF224466),
        tagFontSize = 12.sp,
        tagFontWeight = FontWeight.Medium,
        tagLetterSpacing = 0.4.sp,
        codeTickColor = Color(0xFF777777),
        cjkSpacing = false
    )

    private fun render(content: String): AnnotatedString {
        val paragraph = MarkdownCache.parse(content).tree.children
            .first { it.type == MarkdownElementTypes.PARAGRAPH }
        val inlineContent = mutableMapOf<String, InlineTextContent>()
        return buildAnnotatedString {
            paragraph.children.forEach { child ->
                if (child.type != GFMElementTypes.BLOCK_MATH && child.type != MarkdownElementTypes.IMAGE) {
                    visitInlineChild(
                        child = child,
                        content = content,
                        styles = styles,
                        context = context,
                        density = Density(2.75f),
                        fontSizePx = 44f,
                        builder = this,
                        inlineContent = inlineContent,
                        onHashtagClick = null
                    )
                }
            }
        }
    }

    /** Every span that covers [needle] completely. */
    private fun spansOver(text: AnnotatedString, needle: String): List<SpanStyle> {
        val start = text.text.indexOf(needle)
        assertTrue("'$needle' is not in '${text.text}'", start >= 0)
        return text.spanStyles
            .filter { it.start <= start && it.end >= start + needle.length }
            .map { it.item }
    }

    private fun links(text: AnnotatedString): List<LinkAnnotation> =
        text.getLinkAnnotations(0, text.length).map { it.item }

    @Test
    fun `hashtags are theme coloured capsules with their own padding`() {
        val rendered = render("看这个 #工作/笔记 结尾")

        // The chip owns no-break spaces on each side, part of the annotated range.
        assertEquals(
            "看这个 \u00A0\u00A0\u00A0#工作/笔记\u00A0\u00A0\u00A0 结尾", rendered.text
        )
        val capsules = rendered.getStringAnnotations(TagCapsuleAnnotation, 0, rendered.text.length)
        assertEquals(1, capsules.size)
        assertEquals(styles.tagBackground.toHex(), capsules.single().item)

        val tagSpans = spansOver(rendered, "#工作/笔记")
        assertTrue(tagSpans.any { it.color == styles.tagTextColor })
        // The label size of the collapsed tag row, not the body size of the paragraph.
        assertTrue(tagSpans.any { it.fontSize == styles.tagFontSize })
        assertFalse(tagSpans.any { it.textDecoration == TextDecoration.Underline })

        // The whole chip is tappable, so the padding is inside the link range too.
        val link = links(rendered).single()
        assertEquals("#工作/笔记", (link as LinkAnnotation.Clickable).tag)
    }

    @Test
    fun `inline links keep the accent colour and carry their own underline`() {
        val rendered = render("先去 [官网](https://example.com) 看看")

        assertEquals("先去 官网 看看", rendered.text)
        val linkSpans = spansOver(rendered, "官网")
        assertTrue(linkSpans.any { it.color == styles.linkColor })
        assertTrue(linkSpans.any { it.textDecoration == TextDecoration.Underline })

        val link = links(rendered).single()
        assertEquals("https://example.com", (link as LinkAnnotation.Url).url)
    }

    @Test
    fun `autolinks of both spellings are underlined`() {
        listOf("<https://example.com/a>", "https://example.com/a").forEach { source ->
            val rendered = render(source)
            assertEquals("https://example.com/a", rendered.text)
            assertTrue(
                "no underline for $source",
                spansOver(rendered, "https://example.com/a")
                    .any { it.textDecoration == TextDecoration.Underline }
            )
            assertTrue(links(rendered).single() is LinkAnnotation.Url)
        }
    }

    @Test
    fun `emphasis markers map to weight, slant and strike-through`() {
        val rendered = render("**加粗** / *斜体* / ~~删除~~")

        assertEquals("加粗 / 斜体 / 删除", rendered.text)
        assertTrue(spansOver(rendered, "加粗").any { it.fontWeight == FontWeight.Bold })
        assertTrue(spansOver(rendered, "斜体").any { it.fontStyle == FontStyle.Italic })
        assertTrue(
            spansOver(rendered, "删除").any { it.textDecoration == TextDecoration.LineThrough }
        )
    }

    @Test
    fun `inline code keeps its backticks around the rounded background`() {
        val rendered = render("用 `val x = 1` 试试")

        assertEquals("用 `val x = 1` 试试", rendered.text)
        assertTrue(spansOver(rendered, "val x = 1").any { it.fontFamily == FontFamily.Monospace })
        // The marks stay visible, in a muted colour so they read as delimiters.
        assertTrue(spansOver(rendered, "`").any { it.color == styles.codeTickColor })

        // The band covers the code only, not the marks.
        val backgrounds = rendered.getStringAnnotations(
            "ROUNDED_BG_COLOR", 0, rendered.text.length
        )
        assertEquals(1, backgrounds.size)
        assertEquals(styles.codeBackground.toHex(), backgrounds.single().item)
        assertEquals("val x = 1", rendered.text.substring(backgrounds.single().start, backgrounds.single().end))
    }

    @Test
    fun `double equals is a highlight and the marks are dropped`() {
        val rendered = render("重点 ==这段== 文字")

        assertEquals("重点 这段 文字", rendered.text)
        assertTrue(spansOver(rendered, "这段").any { it.background == styles.highlightBackground })
    }

    @Test
    fun `mentions are picked out in the accent colour but are not links`() {
        val rendered = render("联系 @bob 或 a@b.com")

        assertEquals("联系 @bob 或 a@b.com", rendered.text)
        val mention = spansOver(rendered, "@bob")
        assertTrue(mention.any { it.color == styles.linkColor })
        assertTrue(mention.any { it.fontWeight == FontWeight.Medium })
        // An e-mail address is not a mention, and mentions do not become clickable ranges.
        assertTrue(spansOver(rendered, "a@b.com").none { it.fontWeight == FontWeight.Medium })
        assertTrue(links(rendered).isEmpty())
    }

    @Test
    fun `indentation is for prose, not for chips or list lines`() {
        assertTrue(shouldIndentParagraph("今天写了点东西", inList = false))
        assertTrue(shouldIndentParagraph("今天写了点东西 #tag", inList = false))
        assertFalse(shouldIndentParagraph("#tag", inList = false))
        assertFalse(shouldIndentParagraph("#tag\n#另一个", inList = false))
        assertFalse(shouldIndentParagraph("@bob", inList = false))
        assertFalse(shouldIndentParagraph("@bob @alice", inList = false))
        assertFalse(shouldIndentParagraph("今天写了点东西", inList = true))
        assertFalse(shouldIndentParagraph("   ", inList = false))
    }
}
