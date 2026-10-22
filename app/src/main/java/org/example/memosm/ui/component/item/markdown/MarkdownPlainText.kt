package org.example.memosm.ui.component.item.markdown

import org.example.memosm.model.Memo
/**
 * The readable text of a markdown snippet: the characters a reader sees, without the syntax that
 * produces the styling.
 *
 * It exists for compact labels (the composer's link chips, pickers) where showing `**加粗**` or
 * `[官网](https://…)` would be noise. Deliberately regex-based and lossy: it only has to be good
 * enough for one-line summaries, not a faithful markdown parser.
 */
fun markdownPlainText(markdown: String): String {
    var text = markdown

    // Fenced code blocks first: their content must not be treated as markdown.
    text = text.replace(FencedCode, " ")
    text = text.replace(IndentedCode, " ")
    text = text.replace(InlineCode, "$1")
    text = text.replace(Image, "")
    text = text.replace(Link, "$1")
    text = text.replace(Autolink, "$1")
    text = text.replace(AtxHeading, "")
    text = text.replace(SetextHeading, "")
    text = text.replace(Blockquote, "")
    text = text.replace(BulletItem, "")
    text = text.replace(NumberedItem, "")
    text = text.replace(TaskBox, "")
    text = text.replace(HorizontalRule, " ")
    text = text.replace(Bold, "$2")
    text = text.replace(Italic, "$2")
    text = text.replace(Strike, "$1")
    text = text.replace(Whitespace, " ")

    return text.trim()
}

private val FencedCode = Regex("```[\\s\\S]*?```")
private val IndentedCode = Regex("(?m)^ {4,}\\S.*$")
private val InlineCode = Regex("`([^`]*)`")
private val Image = Regex("!\\[([^\\]]*)]\\([^)]*\\)")
private val Link = Regex("\\[([^\\]]*)]\\([^)]*\\)")
private val Autolink = Regex("<(https?://[^>]+)>")
private val AtxHeading = Regex("(?m)^ {0,3}#{1,6}[ \\t]+")
private val SetextHeading = Regex("(?m)^ {0,3}(=+|-+)[ \\t]*$")
private val Blockquote = Regex("(?m)^ {0,3}>[ \\t]?")
private val BulletItem = Regex("(?m)^[ \\t]*[-*+][ \\t]+")
private val NumberedItem = Regex("(?m)^[ \\t]*\\d+[.)][ \\t]+")
private val TaskBox = Regex("\\[[ xX]] ?")
private val HorizontalRule = Regex("(?m)^ {0,3}([-*_])([ \\t]*\\1){2,}[ \\t]*$")
private val Bold = Regex("(\\*\\*|__)([\\s\\S]*?)\\1")
private val Italic = Regex("(\\*|_)([^*_\\n]+)\\1")
private val Strike = Regex("~~([\\s\\S]*?)~~")
private val Whitespace = Regex("\\s+")

/**
 * One-line label for a memo: the rendered text, falling back to the server name when the content
 * has nothing readable in it (an image-only memo, for instance).
 */
fun Memo.plainLabel(): String = markdownPlainText(content).take(80).ifBlank { name.orEmpty() }