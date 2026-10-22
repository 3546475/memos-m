package org.example.memosm.ui.component.item.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The inline tokenizer and the optional Chinese/Latin spacing, both pure string rules. */
class MarkdownInlineSyntaxTest {
    @Test
    fun `tags, highlights and mentions are tokenised in source order`() {
        val text = "看 #工作/笔记 和 ==重点== 与 @bob 结束"

        val tokens = tokenizeInline(text)

        assertEquals(
            listOf(InlineTokenKind.TAG, InlineTokenKind.HIGHLIGHT, InlineTokenKind.MENTION),
            tokens.map { it.kind }
        )
        assertEquals("#工作/笔记", text.substring(tokens[0].start, tokens[0].end))
        assertEquals("==重点==", text.substring(tokens[1].start, tokens[1].end))
        assertEquals("@bob", text.substring(tokens[2].start, tokens[2].end))
    }

    @Test
    fun `unfinished or lookalike markers stay plain text`() {
        // A lone `#`, an unclosed `==`, and an e-mail address: none of them is markup.
        val text = "开头 # 结尾 和 a == b 以及 a@b.com"

        assertTrue(tokenizeInline(text).isEmpty())
    }

    @Test
    fun `cjk and latin get a thin space between them`() {
        val thin = '\u2009'
        assertEquals("中文${thin}English${thin}测试", addCjkLatinSpacing("中文English测试"))
        assertEquals("版本$thin" + "2", addCjkLatinSpacing("版本2"))
        assertEquals("abc$thin" + "中文", addCjkLatinSpacing("abc中文"))
        // Punctuation is left alone, and so is a pure text run.
        assertEquals("中文，English", addCjkLatinSpacing("中文，English"))
        assertEquals("no change", addCjkLatinSpacing("no change"))
    }

    @Test
    fun `spacing never breaks a tag`() {
        // `#` is punctuation, so nothing is inserted before it: the tag survives as one token.
        assertEquals("这是#tag", addCjkLatinSpacing("这是#tag"))
        val token = tokenizeInline("这是#tag").single()
        assertEquals("#tag", "这是#tag".substring(token.start, token.end))
    }
}
