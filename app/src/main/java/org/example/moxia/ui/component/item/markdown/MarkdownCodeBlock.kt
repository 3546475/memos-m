package org.example.moxia.ui.component.item.markdown

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.example.moxia.R
import java.util.Locale
import kotlin.math.pow

/**
 * The code panel from the jztheme snippet: `padding: 18px`, `border-radius: 12px`,
 * `font-size: 14px` at `line-height: 1.6`, a drop shadow, and a 4dp border on the leading edge in
 * the theme colour.
 *
 * The snippet's `background: #0b1120` is used for the night palette only. A near-black slab on the
 * light theme reads as a hole punched in the page, so the day palette uses the muted surface
 * instead — plus a hairline outline and a faint shadow, because a translucent tint alone was too
 * close to the memo card behind it to read as a card of its own.
 */
private val CodeBlockDarkBackground = Color(0xFF0B1120)
private val CodeBlockDarkText = Color(0xFFE2E8F0)
private val CodeBlockDarkLabel = Color(0xFF94A3B8)
private val CodeBlockShadowColor = Color(0xFF0F172A)
private val CodeBlockCornerRadius = 12.dp
private val CodeBlockPadding = 18.dp
private val CodeBlockBorderWidth = 4.dp
private val CodeBlockFontSize = 14.sp
private val CodeBlockLineHeight = 22.4.sp // line-height: 1.6 on 14px
private val CodeBlockHeaderGap = 6.dp
private val CodeBlockShadowLayers = 12

private data class CodeBlockShadow(val alpha: Float, val spread: Dp, val offsetY: Dp)

/** Night keeps the snippet's shadow, softened; day gets just enough lift to read as a card. */
private val CodeBlockNightShadow = CodeBlockShadow(alpha = 0.10f, spread = 18.dp, offsetY = 6.dp)

/**
 * The day card's edge is carried by its 1dp outline, so the shadow only hints at lift: 5% over
 * 12dp, a third of the night one.
 */
private val CodeBlockDayShadow = CodeBlockShadow(alpha = 0.05f, spread = 12.dp, offsetY = 3.dp)

/** Lines shown before the block folds itself; the rest is one tap away. */
internal const val CodeBlockFoldLines = 18

/** A tab is four spaces here: a monospace tab stop varies by font and breaks alignment. */
internal const val CodeBlockTabWidth = 4

/** Tabs expanded to spaces; both the drawn text and the copied text go through this. */
internal fun normalizeCodeTabs(code: String): String =
    code.replace("\t", " ".repeat(CodeBlockTabWidth))

internal fun codeBlockLines(code: String): List<String> = code.split('\n')

internal fun isCodeBlockFoldable(lineCount: Int): Boolean = lineCount > CodeBlockFoldLines

/** What a collapsed block shows: its first [CodeBlockFoldLines] lines. */
internal fun foldedCode(lines: List<String>): String =
    lines.take(CodeBlockFoldLines).joinToString("\n")

/**
 * The fence's info string as a label: `kt` becomes `Kotlin`, `sh` becomes `Shell`, a language the
 * app has no name for is shown exactly as the author wrote it, and an empty fence shows nothing.
 */
internal object CodeLanguageLabel {
    private val names = mapOf(
        "c" to "C",
        "c++" to "C++",
        "cpp" to "C++",
        "c#" to "C#",
        "csharp" to "C#",
        "coffee" to "CoffeeScript",
        "coffeescript" to "CoffeeScript",
        "css" to "CSS",
        "dart" to "Dart",
        "go" to "Go",
        "golang" to "Go",
        "gradle" to "Gradle",
        "groovy" to "Groovy",
        "html" to "HTML",
        "ini" to "INI",
        "java" to "Java",
        "js" to "JavaScript",
        "javascript" to "JavaScript",
        "json" to "JSON",
        "kt" to "Kotlin",
        "kotlin" to "Kotlin",
        "php" to "PHP",
        "ps1" to "PowerShell",
        "powershell" to "PowerShell",
        "py" to "Python",
        "python" to "Python",
        "rb" to "Ruby",
        "ruby" to "Ruby",
        "rs" to "Rust",
        "rust" to "Rust",
        "sh" to "Shell",
        "bash" to "Shell",
        "shell" to "Shell",
        "zsh" to "Shell",
        "sql" to "SQL",
        "swift" to "Swift",
        "toml" to "TOML",
        "ts" to "TypeScript",
        "typescript" to "TypeScript",
        "xml" to "XML",
        "yaml" to "YAML",
        "yml" to "YAML"
    )

    fun displayName(info: String): String? {
        // A fence may carry more than the language, e.g. ```kotlin title=Main.kt.
        val alias = info.trim().lowercase(Locale.ROOT).substringBefore(' ').substringBefore('\t')
        // A fence with no language still gets a label: the header row would otherwise be empty on
        // the language side, and "txt" says exactly what a plain block is.
        if (alias.isEmpty()) return PlainTextLabel
        return names[alias] ?: alias
    }

    /** Shown when the fence names no language. */
    const val PlainTextLabel = "txt"
}

/**
 * One fenced/indented code block: optional header (language on the leading side, copy on the
 * trailing side) above the code, which scrolls sideways — or wraps, if the 编辑器 setting asks for
 * it — and folds after [CodeBlockFoldLines] lines.
 *
 * @param verticalGap margin above and below the panel, so the shadow has room to fade.
 */
@Composable
internal fun MarkdownCodeBlock(
    code: String,
    language: String,
    verticalGap: Dp
) {
    val format = LocalMarkdownFormat.current
    val darkPanel = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val panelColor =
        if (darkPanel) CodeBlockDarkBackground else MaterialTheme.colorScheme.surfaceContainerHighest
    val codeColor = if (darkPanel) CodeBlockDarkText else MaterialTheme.colorScheme.onSurface
    val metaColor = if (darkPanel) CodeBlockDarkLabel else MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(CodeBlockCornerRadius)

    val normalized = remember(code) { normalizeCodeTabs(code) }
    val lines = remember(normalized) { codeBlockLines(normalized) }
    var expanded by remember(normalized) { mutableStateOf(false) }
    val foldable = isCodeBlockFoldable(lines.size)
    val folded = foldable && !expanded
    val visibleCode = remember(normalized, folded) {
        if (folded) foldedCode(lines) else normalized
    }
    val visibleLines = remember(visibleCode) { codeBlockLines(visibleCode) }
    val highlightedText = CodeHighlighter.highlightCode(visibleCode, language, isDarkMode = darkPanel)
    val label = remember(language) { CodeLanguageLabel.displayName(language) }
    val showHeader = normalized.isNotBlank()

    val wrap = format.codeWrapLines
    val showLineNumbers = format.codeLineNumbers
    val codeStyle = MaterialTheme.typography.bodyMedium.copy(
        fontSize = CodeBlockFontSize,
        lineHeight = CodeBlockLineHeight,
        color = codeColor
    )
    // One slice per source line, for the wrapped layout where the gutter needs a row per line.
    val lineSlices = remember(highlightedText) { highlightedText.splitIntoSlices() }
    // Fixed so the code edge does not shift when the numbers gain a digit.
    val gutterWidth = remember(visibleLines.size) {
        ((visibleLines.size.toString().length * 9) + 10).dp
    }

    val trailingEdge = LocalLayoutDirection.current == LayoutDirection.Rtl
    val leadingInset =
        if (trailingEdge) CodeBlockPadding else CodeBlockBorderWidth + CodeBlockPadding
    val trailingInset =
        if (trailingEdge) CodeBlockBorderWidth + CodeBlockPadding else CodeBlockPadding
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = verticalGap)
            .codeBlockShadow(if (darkPanel) CodeBlockNightShadow else CodeBlockDayShadow)
            .clip(shape)
            .background(panelColor)
            // The night panel is dark enough to sit on its own; the day one needs an explicit edge
            // so it does not melt into the memo card.
            .then(
                if (darkPanel) Modifier
                else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            )
            .drawBehind {
                // `border-left: 4px solid` the theme colour, clipped by the rounded outline.
                val borderWidth = CodeBlockBorderWidth.toPx()
                drawRect(
                    color = accent,
                    topLeft = Offset(
                        if (layoutDirection == LayoutDirection.Ltr) 0f else size.width - borderWidth,
                        0f
                    ),
                    size = Size(borderWidth, size.height)
                )
            }
    ) {
        Column {
            if (showHeader) {
                CodeBlockHeader(
                    label = label,
                    color = metaColor,
                    code = normalized,
                    leadingInset = leadingInset
                )
            }
            if (wrap) {
                // A wrapped source line spans several rows, so gutter and text are laid out one
                // source line at a time: the number stays level with the first row either way.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = leadingInset,
                            top = if (showHeader) CodeBlockHeaderGap else CodeBlockPadding,
                            bottom = if (foldable) 8.dp else CodeBlockPadding
                        )
                ) {
                    lineSlices.forEachIndexed { index, slice ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            if (showLineNumbers) {
                                CodeBlockLineNumber(
                                    number = index + 1,
                                    width = gutterWidth,
                                    color = metaColor
                                )
                            }
                            Text(
                                text = slice.blankLinePlaceholder(),
                                style = codeStyle,
                                fontFamily = FontFamily.Monospace,
                                softWrap = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = trailingInset)
                                    .pointerInput(visibleCode, index) {
                                        detectTapGestures(onLongPress = {
                                            copyLine(context, haptics, visibleLines, index)
                                        })
                                    }
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = leadingInset,
                            top = if (showHeader) CodeBlockHeaderGap else CodeBlockPadding,
                            bottom = if (foldable) 8.dp else CodeBlockPadding
                        ),
                    verticalAlignment = Alignment.Top
                ) {
                    if (showLineNumbers) {
                        CodeBlockLineNumbers(
                            count = visibleLines.size,
                            width = gutterWidth,
                            color = metaColor
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(scrollState)
                    ) {
                        Text(
                            text = highlightedText,
                            style = codeStyle,
                            fontFamily = FontFamily.Monospace,
                            softWrap = false,
                            onTextLayout = { layoutResult = it },
                            // Trailing air is part of the scrolled content, so the longest line still
                            // ends clear of the panel edge. A long press copies the line under the
                            // finger — the shortcut for "I only want this one line".
                            modifier = Modifier
                                .padding(end = trailingInset)
                                .pointerInput(visibleCode) {
                                    detectTapGestures(onLongPress = { position ->
                                        val layout = layoutResult ?: return@detectTapGestures
                                        val offset = layout.getOffsetForPosition(position)
                                            .coerceIn(0, visibleCode.length)
                                        val index = visibleCode.take(offset).count { it == '\n' }
                                        copyLine(context, haptics, visibleLines, index)
                                    })
                                }
                        )
                    }
                }
            }
            if (foldable) {
                CodeBlockFoldToggle(
                    expanded = expanded,
                    remaining = lines.size - CodeBlockFoldLines,
                    color = metaColor,
                    leadingInset = leadingInset,
                    trailingInset = trailingInset,
                    onToggle = { expanded = !expanded }
                )
            }
        }
    }
}

/** 1-based gutter beside the code; drawn only when the 编辑器 setting asks for it. */
@Composable
private fun CodeBlockLineNumbers(count: Int, width: Dp, color: Color) {
    Column(horizontalAlignment = Alignment.End) {
        for (line in 1..count) {
            CodeBlockLineNumber(number = line, width = width, color = color)
        }
    }
}

@Composable
private fun CodeBlockLineNumber(number: Int, width: Dp, color: Color) {
    Text(
        text = number.toString(),
        style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = CodeBlockFontSize,
            lineHeight = CodeBlockLineHeight,
            color = color
        ),
        fontFamily = FontFamily.Monospace,
        textAlign = TextAlign.End,
        modifier = Modifier
            .width(width)
            .padding(end = 10.dp)
    )
}

/** One slice per source line, blank lines included, so the gutter can be drawn line by line. */
internal fun AnnotatedString.splitIntoSlices(): List<AnnotatedString> {
    val slices = mutableListOf<AnnotatedString>()
    var start = 0
    text.forEachIndexed { index, char ->
        if (char == '\n') {
            slices += subSequence(start, index)
            start = index + 1
        }
    }
    slices += subSequence(start, text.length)
    return slices
}

/** An empty line still has to occupy a row, so it is drawn as a no-break space. */
private fun AnnotatedString.blankLinePlaceholder(): AnnotatedString =
    if (text.isEmpty()) AnnotatedString("\u00A0") else this

private fun copyLine(
    context: Context,
    haptics: androidx.compose.ui.hapticfeedback.HapticFeedback,
    lines: List<String>,
    index: Int
) {
    val line = lines.getOrNull(index) ?: return
    copyCodeToClipboard(context, line)
    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    Toast.makeText(
        context,
        context.getString(R.string.code_block_line_copied, index + 1),
        Toast.LENGTH_SHORT
    ).show()
}

@Composable
private fun CodeBlockHeader(
    label: String?,
    color: Color,
    code: String,
    leadingInset: Dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = leadingInset, top = 6.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label ?: "",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                letterSpacing = 0.4.sp,
                color = color
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        CodeBlockCopyButton(code = code, tint = color)
    }
}

@Composable
private fun CodeBlockCopyButton(code: String, tint: Color) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var copied by remember { mutableStateOf(false) }
    // The tick is the only confirmation, so it has to time out on its own.
    LaunchedEffect(copied) {
        if (copied) {
            delay(1200)
            copied = false
        }
    }

    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(RoundedCornerShape(8.dp))
            .semantics { role = Role.Button }
            .clickable {
                // Only the code itself, never the fence lines around it.
                copyCodeToClipboard(context, code)
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                copied = true
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
            contentDescription = stringResource(
                if (copied) R.string.code_block_copied else R.string.code_block_copy
            ),
            tint = tint,
            modifier = Modifier.size(15.dp)
        )
    }
}

@Composable
private fun CodeBlockFoldToggle(
    expanded: Boolean,
    remaining: Int,
    color: Color,
    leadingInset: Dp,
    trailingInset: Dp,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = leadingInset, end = trailingInset, bottom = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .semantics { role = Role.Button }
            .clickable(onClick = onToggle)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (expanded) {
                stringResource(R.string.code_block_collapse)
            } else {
                stringResource(R.string.code_block_expand, remaining)
            },
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp, color = color)
        )
        Icon(
            imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .padding(start = 2.dp)
                .size(14.dp)
        )
    }
}

private fun copyCodeToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("code", text))
}

/**
 * `box-shadow: 0 12px 30px rgba(15, 23, 42, 0.18)` softened to the given strength and approximated
 * with a stack of faint rounded rectangles — the same technique as the floating bar's shadow, since
 * a real blur filter is not guaranteed on every GPU. It draws before the slab, which covers the
 * middle of the stack, and stays inside the block's margin so it cannot wash over the line above.
 */
private fun Modifier.codeBlockShadow(shadow: CodeBlockShadow): Modifier = drawWithContent {
    // A not-yet-measured node is a few pixels tall, and a shadow drawn from that would be a solid
    // square that snaps into place once the real size arrives.
    if (shadow.alpha > 0f && size.height >= 20.dp.toPx() && size.width > 0f) {
        val layerAlpha = 1f - (1f - shadow.alpha).pow(1f / CodeBlockShadowLayers)
        val stepPx = shadow.spread.toPx() / CodeBlockShadowLayers
        val offsetPx = shadow.offsetY.toPx()
        val radius = CodeBlockCornerRadius.toPx()
        for (layer in CodeBlockShadowLayers downTo 1) {
            val grow = stepPx * layer
            drawRoundRect(
                color = CodeBlockShadowColor,
                alpha = layerAlpha,
                topLeft = Offset(-grow, -grow + offsetPx),
                size = Size(size.width + grow * 2f, size.height + grow * 2f),
                cornerRadius = CornerRadius(radius + grow)
            )
        }
    }
    drawContent()
}
