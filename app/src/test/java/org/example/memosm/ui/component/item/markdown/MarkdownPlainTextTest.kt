package org.example.memosm.ui.component.item.markdown

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Labels built from memo content must show what the reader sees, not the markdown that styles it.
 * The extractor is lossy on purpose; these cases pin the syntax that actually shows up in memos.
 */
class MarkdownPlainTextTest {

    @Test
    fun `emphasis markers are dropped`() {
        assertEquals("加粗 和 斜体 还有 删除", markdownPlainText("**加粗** 和 *斜体* 还有 ~~删除~~"))
        assertEquals("下划线强调", markdownPlainText("__下划线强调__"))
    }

    @Test
    fun `links keep their label and images disappear`() {
        assertEquals("先看 官网 再说", markdownPlainText("先看 [官网](https://example.com) 再说"))
        assertEquals("http://example.com/a", markdownPlainText("<http://example.com/a>"))
        assertEquals("", markdownPlainText("![img](https://example.com/a.png)"))
    }

    @Test
    fun `code keeps its text without the ticks or the fence`() {
        assertEquals("用 val x = 1 试试", markdownPlainText("用 `val x = 1` 试试"))
        assertEquals("", markdownPlainText("```kotlin\nval x = 1\n```"))
    }

    @Test
    fun `block syntax is stripped but the prose stays`() {
        assertEquals("标题", markdownPlainText("## 标题"))
        assertEquals("第一项 第二项", markdownPlainText("- 第一项\n- 第二项"))
        assertEquals("一 二", markdownPlainText("1. 一\n2. 二"))
        assertEquals("引用的话", markdownPlainText("> 引用的话"))
        assertEquals("做完了", markdownPlainText("- [x] 做完了"))
    }

    @Test
    fun `tags and mentions survive as typed`() {
        assertEquals("#工作/笔记 今天写完了 @bob", markdownPlainText("#工作/笔记 今天写完了 @bob"))
    }

    @Test
    fun `whitespace collapses to single spaces`() {
        assertEquals("一 二", markdownPlainText("一\n\n\n二"))
        assertEquals("", markdownPlainText("   \n  "))
    }
}
