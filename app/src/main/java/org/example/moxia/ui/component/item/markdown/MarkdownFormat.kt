package org.example.moxia.ui.component.item.markdown

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * How markdown is laid out, from the 编辑器 settings group. Everything defaults to off, so a screen
 * that forgets to provide it renders exactly like the stock build.
 */
data class MarkdownFormat(
    /** 1-based gutter beside code blocks; hidden while long lines wrap, where it cannot line up. */
    val codeLineNumbers: Boolean = false,
    /** Wrap long code lines instead of letting them scroll sideways. */
    val codeWrapLines: Boolean = false,
    /** `text-indent: 2em` on every paragraph, as Chinese typesetting expects. */
    val paragraphIndent: Boolean = false,
    /** Thin space between Chinese characters and Latin letters or digits (pangu style). */
    val cjkSpacing: Boolean = false
) {
    companion object {
        val Default = MarkdownFormat()
    }
}

val LocalMarkdownFormat = compositionLocalOf { MarkdownFormat.Default }

/**
 * Marker tint behind `==text==`.
 *
 * The theme accent at low alpha turned into a grey wash (the light palette's primary is a muted
 * steel blue), which is not what a reader expects from a highlighter. A warm amber is used instead,
 * tuned per palette so the body text keeps its contrast.
 */
internal fun highlightBackground(darkTheme: Boolean): Color =
    Color(0xFFFFD54F).copy(alpha = if (darkTheme) 0.30f else 0.55f)

/** The kinds of inline markup the renderer decorates inside a plain text node. */
internal enum class InlineTokenKind { TAG, HIGHLIGHT, MENTION }

/** Half-open range into the source text of one text node. */
internal data class InlineToken(val kind: InlineTokenKind, val start: Int, val end: Int)

internal val TagPattern = Regex("#[^\\s#]+")

/** `==高亮==`; the marks cannot be empty and cannot contain `=` or a line break. */
internal val HighlightPattern = Regex("==([^=\\n]+)==")

/** `@name`; the look-behind keeps `user@host` out of it. */
internal val MentionPattern = Regex("(?<![\\w.@])@[A-Za-z0-9_.-]{1,64}")

/**
 * Splits one text node into the runs the renderer decorates, scanning left to right so the earliest
 * marker wins. Offsets are into [text]; whatever lies between tokens is plain text.
 */
internal fun tokenizeInline(text: String): List<InlineToken> {
    val tokens = mutableListOf<InlineToken>()
    var index = 0
    while (index < text.length) {
        val match = when {
            text[index] == '#' -> TagPattern.find(text, index)
            text[index] == '=' && index + 1 < text.length && text[index + 1] == '=' ->
                HighlightPattern.find(text, index)
            text[index] == '@' -> MentionPattern.find(text, index)
            else -> null
        }?.takeIf { it.range.first == index }
        if (match == null) {
            index++
            continue
        }
        val kind = when (text[index]) {
            '#' -> InlineTokenKind.TAG
            '=' -> InlineTokenKind.HIGHLIGHT
            else -> InlineTokenKind.MENTION
        }
        tokens += InlineToken(kind, match.range.first, match.range.last + 1)
        index = match.range.last + 1
    }
    return tokens
}

private const val ThinSpace = '\u2009'

internal fun isCjkCharacter(char: Char): Boolean {
    val code = char.code
    return code in 0x3040..0x30FF || // kana
        code in 0x3400..0x4DBF || // CJK extension A
        code in 0x4E00..0x9FFF || // CJK unified ideographs
        code in 0xF900..0xFAFF || // compatibility ideographs
        code in 0xAC00..0xD7AF // hangul syllables
}

private fun isLatinOrDigit(char: Char): Boolean =
    char in 'A'..'Z' || char in 'a'..'z' || char in '0'..'9'

/**
 * Inserts a thin space where Chinese meets Latin letters or digits, the "pangu" rule: `前面Android`
 * reads better as `前面 Android`. Only those two boundaries are touched, so punctuation, `#标签`
 * and `@用户` stay intact.
 */
internal fun addCjkLatinSpacing(text: String): String {
    if (text.length < 2) return text
    val builder = StringBuilder(text.length + 8)
    text.forEachIndexed { index, char ->
        if (index > 0) {
            val previous = text[index - 1]
            val boundary = (isCjkCharacter(previous) && isLatinOrDigit(char)) ||
                (isLatinOrDigit(previous) && isCjkCharacter(char))
            if (boundary) builder.append(ThinSpace)
        }
        builder.append(char)
    }
    return builder.toString()
}

/**
 * The tags the renderer draws as pills, in document order and without duplicates. The card chip
 * row uses this so a tag sitting behind the truncation is still reachable.
 */
fun hashtagsIn(text: String): List<String> = tokenizeInline(text)
    .filter { it.kind == InlineTokenKind.TAG }
    .map { text.substring(it.start, it.end) }
    .distinct()
