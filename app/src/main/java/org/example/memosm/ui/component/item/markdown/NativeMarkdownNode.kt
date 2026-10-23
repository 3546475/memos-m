package org.example.memosm.ui.component.item.markdown

import org.example.memosm.ui.theme.AppDivider

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.findChildOfType
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import java.util.UUID

// Helper data class for styles
data class MarkdownStyles(
    val codeBackground: Color,
    val linkColor: Color,
    val strikethroughStyle: SpanStyle,
    val boldStyle: SpanStyle,
    val italicStyle: SpanStyle,
    val codeFontFamily: FontFamily,
    /** Painted behind `==highlight==` runs. */
    val highlightBackground: Color,
    /** Capsule behind a `#tag`, in the theme accent. */
    val tagBackground: Color,
    /** The tag's own text, chosen to stay readable on [tagBackground]. */
    val tagTextColor: Color,
    /** Label typography for `#tag`, matching [MemoTagChip]. */
    val tagFontSize: TextUnit,
    val tagFontWeight: FontWeight?,
    val tagLetterSpacing: TextUnit,
    /** The backticks that stay visible around inline code. */
    val codeTickColor: Color,
    /** Inserts a thin gap between Chinese and Latin while text is built (the 编辑器 setting). */
    val cjkSpacing: Boolean
)

/**
 * Draws markdown text with its span styles untouched.
 *
 * Foundation's [BasicText], not Material3's `Text`: Material rewrites every `LinkAnnotation` of the
 * string with its own link style (`createTextWithLinkStyles`: accent colour plus underline), which
 * overrides the `SpanStyle` of any link range. Links and hashtags are links, so their colours and
 * decorations have to be ours — the URL branches below add the underline the framework used to.
 */
@Composable
fun MarkdownText(
    text: AnnotatedString,
    style: TextStyle,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
    inlineContent: Map<String, InlineTextContent> = emptyMap()
) {
    val defaultColor = LocalContentColor.current
    val textColor = if (style.color.isSpecified) style.color else defaultColor

    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    val drawModifier = modifier.drawBehind {
        layoutResult?.let { layout ->
            text.getStringAnnotations("ROUNDED_BG_COLOR", 0, text.length).forEach { range ->
                try {
                    val color = Color(range.item.toLong(16))
                    val path = layout.getPathForRange(range.start, range.end)
                    val bounds = path.getBounds()
                    // Draw slightly inflated rounded rect for better visuals
                    drawRoundRect(
                        color = color,
                        topLeft = bounds.topLeft,
                        size = bounds.size,
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                } catch (e: Exception) {
                    // Ignore parsing errors
                }
            }
            // Tag capsules: the range already carries the horizontal padding as no-break spaces.
            text.getStringAnnotations(TagCapsuleAnnotation, 0, text.length).forEach { range ->
                try {
                    val color = Color(range.item.toLong(16))
                    val bounds = layout.getPathForRange(range.start, range.end).getBounds()
                    if (bounds.isEmpty) return@forEach
                    // A label-sized run inside a body-sized line sits on that line's baseline, so a
                    // capsule centred on the line box would leave the text against its bottom edge.
                    // Centre on the tag's own glyph box instead, clamped into the line when the
                    // chip still fits so a column of tags does not bleed into its neighbours.
                    val chipHeight = (TagChipHeight).toPx()
                    val glyphOffset = range.start.coerceIn(
                        0, (layout.layoutInput.text.length - 1).coerceAtLeast(0)
                    )
                    val glyph = layout.getBoundingBox(glyphOffset)
                    val centerY = if (glyph.height > 0f) glyph.center.y else bounds.center.y
                    val top = if (bounds.height > chipHeight) {
                        (centerY - chipHeight / 2f).coerceIn(bounds.top, bounds.bottom - chipHeight)
                    } else {
                        centerY - chipHeight / 2f
                    }
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(bounds.left, top),
                        size = Size(bounds.width, chipHeight),
                        cornerRadius = CornerRadius(chipHeight / 2f)
                    )
                } catch (e: NumberFormatException) {
                    // Ignore a malformed annotation.
                }
            }
        }
    }

    BasicText(
        text = text,
        style = style.copy(color = textColor, textAlign = textAlign),
        modifier = drawModifier,
        onTextLayout = { layoutResult = it },
        inlineContent = inlineContent
    )
}

// Local provider for content and callbacks to avoid passing them deep
val LocalMarkdownContent = compositionLocalOf { "" }

/** Marks the range whose background is drawn as the tag capsule (see [MarkdownText]). */
internal const val TagCapsuleAnnotation = "ROUNDED_TAG_CAPSULE"

/**
 * Whether a paragraph's first line should be indented by the 编辑器 setting.
 *
 * Prose gets the indent; a line that is nothing but tags or nothing but mentions does not (it is a
 * row of chips, not a sentence), and neither does anything inside a list.
 */
internal fun shouldIndentParagraph(paragraphText: String, inList: Boolean): Boolean {
    if (inList) return false
    val trimmed = paragraphText.trim()
    if (trimmed.isEmpty()) return false
    if (TagOnlyLine.matches(trimmed)) return false
    if (MentionOnlyLine.matches(trimmed)) return false
    return true
}

private val TagOnlyLine = Regex("^(#[^\\s#]+)(\\s+#[^\\s#]+)*$")
private val MentionOnlyLine = Regex("^(@[A-Za-z0-9_.-]+)(\\s+@[A-Za-z0-9_.-]+)*$")

/**
 * One paragraph of body text. `p > br { margin-top: 0.5em }` gives every explicit line break half a
 * line of extra air, which is what the per-line padding of `docs/段落间距优化.css` looks like in the
 * editor; wrapped text keeps the plain line height, exactly as the snippet does. The styles and
 * annotations of the paragraph are sliced per line, so links and code spans survive the cut.
 */
@Composable
private fun MarkdownParagraphText(
    text: AnnotatedString,
    inlineContent: Map<String, InlineTextContent>,
    lineGap: Dp,
    firstLineIndentEm: Int
) {
    val lines = remember(text) { text.splitLines() }
    val indent = if (firstLineIndentEm > 0) TextIndent(firstLine = firstLineIndentEm.em) else TextIndent.None
    val bodyStyle = typography.bodyLarge.copy(textIndent = indent)
    if (lines.size == 1) {
        MarkdownText(text = text, style = bodyStyle, inlineContent = inlineContent)
        return
    }
    val hangingStyle = typography.bodyLarge.copy(textIndent = TextIndent.None)
    Column(verticalArrangement = Arrangement.spacedBy(lineGap)) {
        lines.forEachIndexed { index, line ->
            MarkdownText(
                text = line,
                style = if (index == 0) bodyStyle else hangingStyle,
                inlineContent = inlineContent
            )
        }
    }
}

/** Splits at explicit line breaks, dropping the empty tail an EOL at the paragraph end leaves. */
internal fun AnnotatedString.splitLines(): List<AnnotatedString> {
    if ('\n' !in text) return listOf(this)
    val lines = mutableListOf<AnnotatedString>()
    var start = 0
    text.forEachIndexed { index, char ->
        if (char == '\n') {
            lines += subSequence(start, index)
            start = index + 1
        }
    }
    if (start < text.length) lines += subSequence(start, text.length)
    return lines
}

/**
 * Heading decoration from `docs/BorderTitle.css`: every level gets a narrow coloured bar before the
 * text and a short rule under it that fades out towards the left. Sizes and weights step down from
 * h1 to h6 instead of the Material display/headline ramp, so the headings stay close to body size.
 */
internal data class HeadingDecoration(
    val color: Color,
    /** Multiplier on the body font size; the CSS sets 1.3em down to 1em. */
    val sizeMultiplier: Float,
    val fontWeight: Int,
    val lineHeightRatio: Float
)

private val HeadingBarWidth = 3.dp
private val HeadingBarTop = 4.dp
private val HeadingTextIndent = 9.dp
private val HeadingRuleThickness = 2.dp

/**
 * Block rhythm from `docs/段落间距优化.css`, in `em` of the body size so it follows the font scale
 * the way the snippet does. It pads every editor line by these amounts; this renderer has no
 * per-line padding, so the numbers set the gap above and below each markdown block instead.
 *
 * Paragraphs were the snippet's 0.3em, which left two paragraphs only 0.6em apart once the
 * surrounding cards and headings were taken into account — raised to 0.5em (1em between two
 * paragraphs), with headings following to 0.75em so they still read as the larger break.
 */
internal const val ParagraphSpacingEm = 0.5f
internal const val HeadingSpacingEm = 0.75f

/**
 * Padding a list line gets in the snippet. The first paragraph of an item drops its top padding so
 * the bullet stays level with the text, which leaves only this bottom padding between two items —
 * the list therefore adds the same amount again as inter-item arrangement, making the visible gap
 * 0.2em.
 */
internal const val ListSpacingEm = 0.1f

/** `p > br { margin-top: 0.5em }` — the air the snippet puts after an explicit line break. */
internal const val LineBreakSpacingEm = 0.5f

/** Set inside lists, where the CSS gives every line only 0.1em instead of the two values above. */
private val LocalListSpacingEm = compositionLocalOf<Float?> { null }

/**
 * Vertical margin around a code panel, in em of the body size; the snippet has none, but a panel
 * with a shadow needs room to fade into instead of sitting on the neighbouring lines. The panel
 * itself lives in [MarkdownCodeBlock].
 */
internal const val CodeBlockSpacingEm = 1f

@Composable
private fun headingDecoration(level: Int): HeadingDecoration =
    headingDecorationFor(level, dark = MaterialTheme.colorScheme.background.luminance() < 0.5f)

/** Pure half of [headingDecoration], so the day/night ladder can be asserted without composition. */
internal fun headingDecorationFor(level: Int, dark: Boolean): HeadingDecoration {
    val color = if (dark) {
        when (level) {
            1 -> Color(0xFFFF8A78)
            2 -> Color(0xFFFABE58)
            3 -> Color(0xFFFFEC8B)
            4 -> Color(0xFF87D37C)
            5 -> Color(0xFF89C4F4)
            else -> Color(0xFFC69EFF)
        }
    } else {
        when (level) {
            1 -> Color(0xFFFE7968)
            2 -> Color(0xFFF39C12)
            3 -> Color(0xFFE5C700)
            4 -> Color(0xFF78BA7E)
            5 -> Color(0xFF038AFF)
            else -> Color(0xFF9455EC)
        }
    }
    return HeadingDecoration(
        color = color,
        sizeMultiplier = when (level) {
            1 -> 1.3f
            2 -> 1.2f
            3 -> 1.1f
            else -> 1f
        },
        fontWeight = when (level) {
            1 -> 700
            2 -> 675
            3 -> 650
            4 -> 625
            5 -> 600
            else -> 575
        },
        lineHeightRatio = when (level) {
            1, 2 -> 1.2f
            3 -> 1.3f
            4 -> 1.4f
            else -> 1.5f
        }
    )
}
val LocalOnContentChange = compositionLocalOf<((String) -> Unit)?> { null }
val LocalForceNoTopPadding = compositionLocalOf { false }
val LocalOnHashtagClick = compositionLocalOf<((String) -> Unit)?> { null }
val LocalHeaderScale = compositionLocalOf { 1.0f }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NativeMarkdownNode(
    modifier: Modifier = Modifier,
    node: ASTNode,
    content: String,
    headerScale: Float,
    onContentChange: ((String) -> Unit)? = null,
    onHashtagClick: ((String) -> Unit)? = null
) {
    CompositionLocalProvider(
        LocalMarkdownContent provides content,
        LocalOnContentChange provides onContentChange,
        LocalOnHashtagClick provides onHashtagClick,
        LocalHeaderScale provides headerScale
    ) {
        Column(modifier = modifier) {
            NativeMarkdownNodeRecursive(node)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NativeMarkdownNodeRecursive(node: ASTNode, allowPreview: Boolean = false) {
    val previews = LocalLinkPreviews.current
    val previewContent = LocalMarkdownContent.current
    val previewReferences = LocalMarkdownReferences.current
    val previewUrl = if (previews != null && allowPreview) {
        remember(node, previewContent, previewReferences) {
            standalonePreviewUrl(node, previewContent, previewReferences)
        }
    } else null
    if (previewUrl != null && previews != null) {
        NativeLinkPreview(previewUrl, previews.repository, previews.isOnline)
        return
    }
    val content = LocalMarkdownContent.current
    val onContentChange = LocalOnContentChange.current
    val onHashtagClick = LocalOnHashtagClick.current
    val headerScale = LocalHeaderScale.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val fontSizePx = with(density) { typography.bodyLarge.fontSize.toPx() }
    // `em` of the body size in dp, i.e. 1em of docs/段落间距优化.css.
    val emToDp = fontSizePx / density.density
    // Inside a list the CSS gives every line 0.1em, so both the block gap and the gap after a
    // line break collapse to that value.
    val listSpacingEm = LocalListSpacingEm.current
    val paragraphGap = ((listSpacingEm ?: ParagraphSpacingEm) * emToDp).dp
    val lineBreakGap = ((listSpacingEm ?: LineBreakSpacingEm) * emToDp).dp
    val headingGap = (HeadingSpacingEm * emToDp).dp
    val codeBlockGap = (CodeBlockSpacingEm * emToDp).dp
    // Tops the gap between two list items up to 0.2em; see [ListSpacingEm].
    val listItemGap = (ListSpacingEm * emToDp).dp
    // `text-indent: 2em`, the 编辑器 setting for Chinese typesetting; see [shouldIndentParagraph].
    val indentProse = LocalMarkdownFormat.current.paragraphIndent

    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val styles = MarkdownStyles(
        codeBackground = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
        linkColor = MaterialTheme.colorScheme.primary,
        strikethroughStyle = SpanStyle(textDecoration = TextDecoration.LineThrough),
        boldStyle = SpanStyle(fontWeight = FontWeight.Bold),
        italicStyle = SpanStyle(fontStyle = FontStyle.Italic),
        codeFontFamily = FontFamily.Monospace,
        highlightBackground = highlightBackground(darkTheme),
        // Same chip as the tag row a collapsed card shows (see MemoTagChip): the accent at 10%
        // behind plain accent text, in the label size rather than the body size.
        tagBackground = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
        tagTextColor = MaterialTheme.colorScheme.primary,
        tagFontSize = MaterialTheme.typography.labelMedium.fontSize,
        tagFontWeight = MaterialTheme.typography.labelMedium.fontWeight,
        tagLetterSpacing = MaterialTheme.typography.labelMedium.letterSpacing,
        codeTickColor = MaterialTheme.colorScheme.onSurfaceVariant,
        cjkSpacing = LocalMarkdownFormat.current.cjkSpacing
    )

    when (node.type) {
        MarkdownElementTypes.MARKDOWN_FILE -> {
            // Render all children
            node.children.forEach { child ->
                NativeMarkdownNodeRecursive(child, allowPreview = true)
            }
        }

        MarkdownElementTypes.PARAGRAPH -> {
            val noTopPadding = LocalForceNoTopPadding.current
            // Prose only: a line of chips (tags/mentions) or the lines of a list stay flush.
            val paragraphIndentEm = if (
                indentProse &&
                shouldIndentParagraph(
                    paragraphText = content.substring(node.startOffset, node.endOffset),
                    inList = LocalListSpacingEm.current != null
                )
            ) 2 else 0

            Column(
                modifier = Modifier.padding(
                    top = if (noTopPadding) 0.dp else paragraphGap,
                    bottom = paragraphGap
                )
            ) {
                val children = node.children
                var lastIndex = 0

                children.forEachIndexed { index, child ->
                    if (child.type == GFMElementTypes.BLOCK_MATH || child.type == MarkdownElementTypes.IMAGE) {
                        // Render previous text chunk
                        if (index > lastIndex) {
                            val textNodes = children.subList(lastIndex, index)

                            val (styledText, inlineContentMap) = remember(textNodes, content, styles) {
                                val map = mutableMapOf<String, InlineTextContent>()
                                val text = buildAnnotatedString {
                                    textNodes.forEach { textNode ->
                                        visitInlineChild(
                                            textNode,
                                            content,
                                            styles,
                                            context,
                                            density,
                                            fontSizePx,
                                            this,
                                            map,
                                            onHashtagClick
                                        )
                                    }
                                }
                                text to map
                            }

                            if (styledText.isNotEmpty()) {
                                MarkdownParagraphText(
                                    text = styledText,
                                    inlineContent = inlineContentMap,
                                    lineGap = lineBreakGap,
                                    firstLineIndentEm = paragraphIndentEm
                                )
                            }
                        }

                        if (child.type == GFMElementTypes.BLOCK_MATH) {
                            // Render Block Math
                            val rawText =
                                child.children.joinToString("") { it.getTextInNode(content) }
                            val latex = rawText.trim().removePrefix("$$").removeSuffix("$$").trim()

                            NativeMarkdownLatex(
                                latex = latex,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                inline = false
                            )
                        } else if (child.type == MarkdownElementTypes.IMAGE) {
                            // Render Image
                            NativeMarkdownAttachmentImage(content, child)
                        }

                        lastIndex = index + 1
                    }
                }

                // Render remaining text
                if (lastIndex < children.size) {
                    val textNodes = children.subList(lastIndex, children.size)

                    val (styledText, inlineContentMap) = remember(textNodes, content, styles) {
                        val map = mutableMapOf<String, InlineTextContent>()
                        val text = buildAnnotatedString {
                            textNodes.forEach { textNode ->
                                visitInlineChild(
                                    textNode,
                                    content,
                                    styles,
                                    context,
                                    density,
                                    fontSizePx,
                                    this,
                                    map,
                                    onHashtagClick
                                )
                            }
                        }
                        text to map
                    }

                    if (styledText.isNotEmpty()) {
                        MarkdownParagraphText(
                            text = styledText,
                            inlineContent = inlineContentMap,
                            lineGap = lineBreakGap,
                            firstLineIndentEm = paragraphIndentEm
                        )
                    }
                }
            }
        }

        MarkdownElementTypes.ATX_1, MarkdownElementTypes.ATX_2, MarkdownElementTypes.ATX_3, MarkdownElementTypes.ATX_4, MarkdownElementTypes.ATX_5, MarkdownElementTypes.ATX_6 -> {
            val level = when (node.type) {
                MarkdownElementTypes.ATX_1 -> 1
                MarkdownElementTypes.ATX_2 -> 2
                MarkdownElementTypes.ATX_3 -> 3
                MarkdownElementTypes.ATX_4 -> 4
                MarkdownElementTypes.ATX_5 -> 5
                else -> 6
            }
            val decoration = headingDecoration(level)
            val bodySize = typography.bodyLarge.fontSize
            val scaledSize = bodySize * decoration.sizeMultiplier * headerScale
            // Ensure header is never smaller than the body text
            val finalSize = if (scaledSize >= bodySize) scaledSize else bodySize
            val lineHeight = finalSize * decoration.lineHeightRatio
            val style = typography.bodyLarge.copy(
                fontSize = finalSize,
                fontWeight = FontWeight(decoration.fontWeight),
                lineHeight = lineHeight
            )
            val headerFontSizePx = with(density) { finalSize.toPx() }
            val fontSizeDp = with(density) { finalSize.toDp() }
            // The CSS keeps the bar clear of the line's ascender/descender by a fixed 8px.
            val barHeight = (fontSizeDp * decoration.lineHeightRatio - 8.dp)
                .coerceAtLeast(4.dp)
            val accent = decoration.color

            // Headers contain inline elements usually, or just leaf text
            // Retrieve the text content excluding the # characters
            // BUT intellij-markdown structure for headers usually has: 
            // ATX_1 -> [ATX_CONTENT -> [TEXT]]
            // We can just recursively render inline content.
            // But headers are block elements, so we treat them as text with style.
            val (styledText, _) = remember(node, content, finalSize, styles) {
                val map = mutableMapOf<String, InlineTextContent>()
                val text = buildAnnotatedString {
                    // Header content usually doesn't have complex math, but we support it best effort.
                    node.children.forEach { child ->
                        if (child.type != MarkdownTokenTypes.ATX_HEADER) {
                            visitInlineChild(
                                child,
                                content,
                                styles,
                                context,
                                density,
                                headerFontSizePx,
                                this,
                                map,
                                onHashtagClick
                            )
                        }
                    }
                }
                text to map
            }
            val noTopPadding = LocalForceNoTopPadding.current
            val topPadding = if (noTopPadding) 0.dp else headingGap

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = topPadding, bottom = headingGap)
            ) {
                MarkdownText(
                    text = styledText,
                    style = style,
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            // Coloured bar hugging the first line.
                            val barWidthPx = HeadingBarWidth.toPx()
                            drawRoundRect(
                                color = accent,
                                topLeft = Offset(0f, HeadingBarTop.toPx()),
                                size = Size(barWidthPx, barHeight.toPx()),
                                cornerRadius = CornerRadius(barWidthPx / 2f)
                            )
                            // Rule under the heading: solid near the right edge, gone by the left,
                            // at half opacity — the CSS gradient written right to left.
                            val thickness = HeadingRuleThickness.toPx()
                            val half = accent.copy(alpha = 0.5f)
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    0f to Color.Transparent,
                                    0.3f to Color.Transparent,
                                    0.7f to half,
                                    1f to half
                                ),
                                topLeft = Offset(0f, size.height - thickness),
                                size = Size(size.width, thickness)
                            )
                        }
                        .padding(start = HeadingTextIndent)
                )
            }
        }

        MarkdownElementTypes.UNORDERED_LIST -> {
            // List lines carry the tightest rhythm in docs/段落间距优化.css (0.1em) and the container
            // adds none of its own, so items sit 0.2em apart and 0.4em from a neighbouring
            // paragraph.
            CompositionLocalProvider(LocalListSpacingEm provides ListSpacingEm) {
                Column(verticalArrangement = Arrangement.spacedBy(listItemGap)) {
                    node.children.forEach { child ->
                        if (child.type == MarkdownElementTypes.LIST_ITEM) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                // Check for Checkbox
                                val checkBoxNode = child.findChildOfType(GFMTokenTypes.CHECK_BOX)
                                if (checkBoxNode != null) {
                                    val isChecked = checkBoxNode.getTextInNode(content)
                                        .contains("x", ignoreCase = true)
                                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { checked ->
                                                onContentChange?.invoke(
                                                    toggleCheckbox(
                                                        content,
                                                        checkBoxNode.startOffset,
                                                        checkBoxNode.endOffset,
                                                        checked
                                                    )
                                                )
                                            },
                                            modifier = Modifier
                                                .scale(0.8f)
                                                .offset(x = (-4).dp, y = (-2).dp)
                                                .padding(end = 4.dp)
                                        )
                                    }
                                } else {
                                    Text(
                                        "•",
                                        style = typography.bodyLarge,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                }

                                Column {
                                    var firstChildProcessed = false
                                    child.children.forEach { listChild ->
                                        if (listChild.type != GFMTokenTypes.CHECK_BOX && listChild.type != MarkdownTokenTypes.LIST_BULLET && listChild.type != MarkdownTokenTypes.EOL) {
                                            if (!firstChildProcessed) {
                                                CompositionLocalProvider(LocalForceNoTopPadding provides true) {
                                                    NativeMarkdownNodeRecursive(listChild)
                                                }
                                                firstChildProcessed = true
                                            } else {
                                                NativeMarkdownNodeRecursive(listChild)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            NativeMarkdownNodeRecursive(child)
                        }
                    }
                }
            }
        }

        MarkdownElementTypes.ORDERED_LIST -> {
            var index = 1
            CompositionLocalProvider(LocalListSpacingEm provides ListSpacingEm) {
                Column(verticalArrangement = Arrangement.spacedBy(listItemGap)) {
                    node.children.forEach { child ->
                        if (child.type == MarkdownElementTypes.LIST_ITEM) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "$index.",
                                    style = typography.bodyLarge,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Column {
                                    var firstChildProcessed = false
                                    child.children.forEach { listChild ->
                                        if (listChild.type != MarkdownTokenTypes.LIST_NUMBER && listChild.type != MarkdownTokenTypes.EOL) {
                                            if (!firstChildProcessed) {
                                                CompositionLocalProvider(LocalForceNoTopPadding provides true) {
                                                    NativeMarkdownNodeRecursive(listChild)
                                                }
                                                firstChildProcessed = true
                                            } else {
                                                NativeMarkdownNodeRecursive(listChild)
                                            }
                                        }
                                    }
                                }
                                index++
                            }
                        } else {
                            NativeMarkdownNodeRecursive(child)
                        }
                    }
                }
            }
        }

        MarkdownElementTypes.CODE_BLOCK, MarkdownElementTypes.CODE_FENCE -> {
            var lang = ""
            val sb = StringBuilder()
            node.children.forEach { child ->
                when (child.type) {
                    MarkdownTokenTypes.FENCE_LANG -> {
                        lang = child.getTextInNode(content).toString().trim()
                    }

                    MarkdownTokenTypes.CODE_FENCE_CONTENT, MarkdownTokenTypes.CODE_LINE, MarkdownTokenTypes.EOL -> {
                        sb.append(child.getTextInNode(content))
                    }
                    // Ignore fence delimiters (START/END) and other metadata
                }
            }

            // Remove leading/trailing newlines to avoid extra padding, but preserve indentation
            val code = sb.toString().removePrefix("\n").removeSuffix("\n")
            MarkdownCodeBlock(code = code, language = lang, verticalGap = codeBlockGap)
        }

        MarkdownElementTypes.BLOCK_QUOTE -> {
            val borderColor = MaterialTheme.colorScheme.outlineVariant
            // Draw the border at the measured height instead of asking for the
            // full intrinsic height, which can exceed Compose's packed limits.
            // Intrinsic queries also crash quoted images: AttachmentCard uses
            // BoxWithConstraints, which does not support intrinsic measurement.
            Column(
                modifier = Modifier
                    .padding(vertical = paragraphGap)
                    .drawBehind {
                        val borderWidth = 4.dp.toPx()
                        val radius = 2.dp.toPx()
                        drawRoundRect(
                            color = borderColor,
                            topLeft = Offset(
                                if (layoutDirection == LayoutDirection.Ltr) 0f
                                else size.width - borderWidth,
                                0f
                            ),
                            size = Size(borderWidth, size.height),
                            cornerRadius = CornerRadius(radius, radius)
                        )
                    }
                    .padding(start = 12.dp)
            ) {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                    node.children.forEach { child ->
                        if (child.type != MarkdownTokenTypes.BLOCK_QUOTE) {
                            NativeMarkdownNodeRecursive(child)
                        }
                    }
                }
            }
        }

        MarkdownTokenTypes.HORIZONTAL_RULE -> {
            AppDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }

        MarkdownElementTypes.LINK_DEFINITION -> {
            // Do not render link definitions
        }

        GFMElementTypes.TABLE -> {
            NativeMarkdownTable(content, node)
        }

        GFMElementTypes.BLOCK_MATH -> {
            // $$ ... $$
            val rawText = node.children.joinToString("") { it.getTextInNode(content) }
            val latex = rawText.trim().removePrefix("$$").removeSuffix("$$").trim()

            NativeMarkdownLatex(
                latex = latex,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                inline = false
            )
        }

        MarkdownElementTypes.IMAGE -> {
            // Image handling via AttachmentCard
            // Children: [ "![", LINK_TEXT, "](", LINK_DESTINATION, ")"]
            // or helper to find link
            // We can implement a specific NativeMarkdownAttachment component
            NativeMarkdownAttachmentImage(content, node)
        }

        // Handle other block types or fallthrough
        else -> {
            // If it's a composite node, visit children.
            // If it matches known inline types but we are in block context?
            // Usually PARAGRAPH wraps inline content.
            // If we encounter raw text or unknown nodes at block level:
            if (node.children.isNotEmpty()) {
                node.children.forEach { NativeMarkdownNodeRecursive(it) }
            }
        }
    }
}

/** A run of plain text inside a node, with the optional Chinese/Latin spacing applied. */
private fun AnnotatedString.Builder.appendPlainRun(raw: String, styles: MarkdownStyles) {
    if (raw.isEmpty()) return
    append(if (styles.cjkSpacing) addCjkLatinSpacing(raw) else raw)
}

/**
 * The in-flow padding of a tag capsule. Two no-break spaces per side are part of the annotated
 * range, so the pill's width comes from the text itself instead of overlapping its neighbours — and
 * because the link covers the same range, the whole chip is tappable.
 */
// Roughly the collapsed row's 10dp horizontal chip padding, in no-break spaces at the label size.
private const val TagPadding = "\u00A0\u00A0\u00A0"

/** Height of the `#tag` capsule: label text plus the collapsed row's 4dp vertical padding. */
private val TagChipHeight = 18.dp

private fun AnnotatedString.Builder.appendHashtag(
    tag: String,
    styles: MarkdownStyles,
    onHashtagClick: ((String) -> Unit)?
) {
    pushStringAnnotation(TagCapsuleAnnotation, styles.tagBackground.toHex())
    pushLink(LinkAnnotation.Clickable(tag) { onHashtagClick?.invoke(tag) })
    withStyle(
        SpanStyle(
            color = styles.tagTextColor,
            fontSize = styles.tagFontSize,
            fontWeight = styles.tagFontWeight,
            letterSpacing = styles.tagLetterSpacing,
            textDecoration = TextDecoration.None
        )
    ) {
        append(TagPadding)
        append(tag)
        append(TagPadding)
    }
    pop()
    pop()
}

/** `==text==` gets a soft accent background; the text colour stays the body's. */
private fun AnnotatedString.Builder.appendHighlight(raw: String, styles: MarkdownStyles) {
    if (raw.isEmpty()) return
    withStyle(SpanStyle(background = styles.highlightBackground)) {
        appendPlainRun(raw, styles)
    }
}

/** `@user` is picked out in the accent colour; memos has no per-user screen to open yet. */
private fun AnnotatedString.Builder.appendMention(mention: String, styles: MarkdownStyles) {
    withStyle(SpanStyle(color = styles.linkColor, fontWeight = FontWeight.Medium)) {
        append(mention)
    }
}

// Inline content builder, NOT Composable
fun AnnotatedString.Builder.appendInlineChildren(
    node: ASTNode,
    content: String,
    styles: MarkdownStyles,
    context: Context,
    density: Density,
    fontSizePx: Float,
    inlineContent: MutableMap<String, InlineTextContent>,
    onHashtagClick: ((String) -> Unit)?
) {
    if (node.children.isEmpty()) {
        val text = node.getTextInNode(content).toString()
        // Tags, `==highlights==` and `@mentions` are decorated one run at a time; everything
        // between them is plain text, which is where the optional CJK/Latin spacing goes.
        var cursor = 0
        tokenizeInline(text).forEach { token ->
            appendPlainRun(text.substring(cursor, token.start), styles)
            when (token.kind) {
                InlineTokenKind.TAG ->
                    appendHashtag(text.substring(token.start, token.end), styles, onHashtagClick)
                InlineTokenKind.HIGHLIGHT ->
                    appendHighlight(text.substring(token.start + 2, token.end - 2), styles)
                InlineTokenKind.MENTION ->
                    appendMention(text.substring(token.start, token.end), styles)
            }
            cursor = token.end
        }
        appendPlainRun(text.substring(cursor), styles)
        return
    }

    node.children.forEach { child ->
        visitInlineChild(
            child,
            content,
            styles,
            context,
            density,
            fontSizePx,
            this,
            inlineContent,
            onHashtagClick
        )
    }
}

fun visitInlineChild(
    child: ASTNode,
    content: String,
    styles: MarkdownStyles,
    context: Context,
    density: Density,
    fontSizePx: Float,
    builder: AnnotatedString.Builder,
    inlineContent: MutableMap<String, InlineTextContent>,
    onHashtagClick: ((String) -> Unit)?
) {
    with(builder) {
        when (child.type) {
            MarkdownElementTypes.STRONG -> {
                withStyle(styles.boldStyle) {
                    child.children.forEach { c ->
                        if (c.type != MarkdownTokenTypes.EMPH) {
                            visitInlineChild(
                                c,
                                content,
                                styles,
                                context,
                                density,
                                fontSizePx,
                                this,
                                inlineContent,
                                onHashtagClick
                            )
                        }
                    }
                }
            }

            MarkdownElementTypes.EMPH -> {
                withStyle(styles.italicStyle) {
                    child.children.forEach { c ->
                        if (c.type != MarkdownTokenTypes.EMPH) {
                            visitInlineChild(
                                c,
                                content,
                                styles,
                                context,
                                density,
                                fontSizePx,
                                this,
                                inlineContent,
                                onHashtagClick
                            )
                        }
                    }
                }
            }

            MarkdownElementTypes.AUTOLINK -> {
                val text = child.getTextInNode(content).toString()
                val destination = text.removePrefix("<").removeSuffix(">")

                pushLink(LinkAnnotation.Url(destination))
                // The underline is ours: BasicText does not add link decoration the way Material3's
                // Text did, and a link that is only coloured reads as emphasised text.
                withStyle(SpanStyle(color = styles.linkColor, textDecoration = TextDecoration.Underline)) {
                    append(destination)
                }
                pop()
            }

            GFMTokenTypes.GFM_AUTOLINK -> {
                val text = child.getTextInNode(content).toString()
                pushLink(LinkAnnotation.Url(text))
                // The underline is ours: BasicText does not add link decoration the way Material3's
                // Text did, and a link that is only coloured reads as emphasised text.
                withStyle(SpanStyle(color = styles.linkColor, textDecoration = TextDecoration.Underline)) {
                    append(text)
                }
                pop()
            }

            GFMElementTypes.INLINE_MATH -> {
                val rawText = child.getTextInNode(content).toString()
                val latex = rawText.removePrefix("$").removeSuffix("$").trim()
                val id = "inline_math_${UUID.randomUUID()}"

                // Dynamic measurement, memoised per (latex, font size) — laying out a throwaway
                // MTMathView for every occurrence is what made formula-heavy notes stutter.
                val (widthEm, heightEm) = LatexMetrics.measure(context, latex, fontSizePx)
                    ?: run {
                        val fallbackWidth = (latex.length * 0.6).coerceAtLeast(1.0)
                        fallbackWidth to 1.5
                    }

                inlineContent[id] = InlineTextContent(
                    Placeholder(
                        width = widthEm.em,
                        height = heightEm.em,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                    )
                ) {
                    NativeMarkdownLatex(
                        latex = latex,
                        inline = true,
                        modifier = Modifier.fillMaxHeight()
                    )
                }

                appendInlineContent(id, "($latex)")
            }

            MarkdownElementTypes.CODE_SPAN -> {
                // The backticks stay on screen: without them an inline span is indistinguishable
                // from emphasised body text.
                val raw = child.getTextInNode(content).toString()
                val ticks = raw.takeWhile { it == '`' }.ifEmpty { "`" }
                val inner = raw.removePrefix(ticks).removeSuffix(ticks)
                withStyle(SpanStyle(color = styles.codeTickColor, fontFamily = styles.codeFontFamily)) {
                    append(ticks)
                }
                pushStringAnnotation("ROUNDED_BG_COLOR", styles.codeBackground.toHex())
                withStyle(SpanStyle(fontFamily = styles.codeFontFamily)) {
                    append(inner)
                }
                pop()
                withStyle(SpanStyle(color = styles.codeTickColor, fontFamily = styles.codeFontFamily)) {
                    append(ticks)
                }
            }

            MarkdownElementTypes.LINK_DEFINITION, MarkdownElementTypes.INLINE_LINK -> {
                val linkText =
                    child.findChildOfType(MarkdownElementTypes.LINK_TEXT)?.getTextInNode(content)
                        ?.toString() ?: "Link"
                val linkDest = child.findChildOfType(MarkdownElementTypes.LINK_DESTINATION)
                    ?.getTextInNode(content)?.toString() ?: ""

                pushLink(LinkAnnotation.Url(linkDest))
                // The underline is ours: BasicText does not add link decoration the way Material3's
                // Text did, and a link that is only coloured reads as emphasised text.
                withStyle(SpanStyle(color = styles.linkColor, textDecoration = TextDecoration.Underline)) {
                    val linkTextNode = child.findChildOfType(MarkdownElementTypes.LINK_TEXT)
                    if (linkTextNode != null) {
                        linkTextNode.children.forEach { lc ->
                            if (lc.type != MarkdownTokenTypes.LBRACKET && lc.type != MarkdownTokenTypes.RBRACKET) {
                                visitInlineChild(
                                    lc,
                                    content,
                                    styles,
                                    context,
                                    density,
                                    fontSizePx,
                                    this,
                                    inlineContent,
                                    onHashtagClick
                                )
                            }
                        }
                    } else {
                        append(linkText)
                    }
                }
                pop()
            }

            GFMElementTypes.STRIKETHROUGH -> {
                withStyle(styles.strikethroughStyle) {
                    child.children.forEach { c ->
                        if (c.type != GFMTokenTypes.TILDE) {
                            visitInlineChild(
                                c,
                                content,
                                styles,
                                context,
                                density,
                                fontSizePx,
                                this,
                                inlineContent,
                                onHashtagClick
                            )
                        }
                    }
                }
            }

            else -> {
                appendInlineChildren(
                    child,
                    content,
                    styles,
                    context,
                    density,
                    fontSizePx,
                    inlineContent,
                    onHashtagClick
                )
            }
        }
    }
}
