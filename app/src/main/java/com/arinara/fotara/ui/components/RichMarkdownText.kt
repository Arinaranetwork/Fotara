// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.theme.FolderTabCream
import com.arinara.fotara.theme.TagCrimson
import com.arinara.fotara.ui.note.math.ComposeMathView
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import com.arinara.fotara.util.LinkLauncher
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Builds an [AnnotatedString] supporting inline markdown syntax:
 * - ***bold italic*** or ___bold italic___
 * - **bold** or __bold__
 * - *italic* or _italic_
 * - `inline code`
 * - ~~strikethrough~~
 * - [link text](url)
 */
fun buildRichMarkdownAnnotatedString(
    text: String,
    codeBackground: Color = ElectricSlateTokens.SurfaceElevated,
    codeColor: Color = ElectricSlateTokens.TextPrimary,
    accentColor: Color = ElectricSlateTokens.PrimaryAccent,
    linkColor: Color = ElectricSlateTokens.PrimaryAccent,
    isLatexInstalled: Boolean = true
): AnnotatedString {
    return buildAnnotatedString {
        appendRichMarkdownInternal(
            rawText = text,
            codeBackground = codeBackground,
            codeColor = codeColor,
            accentColor = accentColor,
            linkColor = linkColor,
            depth = 0,
            isLatexInstalled = isLatexInstalled
        )
    }
}

private val INLINE_TOKEN_REGEX = Regex(
    """(\*\*\*(?:[^*]|\*(?!\*\*))+\*\*\*)|""" + // 1: ***bold italic***
    """(___(?:[^_]|_(?!__))+___)|""" +         // 2: ___bold italic___
    """(\*\*(?:[^*]|\*(?!\*))+\*\*)|""" +       // 3: **bold**
    """(__(?:[^_]|_(?!_))+__)|""" +             // 4: __bold__
    """(\*(?:[^*])+\*)|""" +                     // 5: *italic*
    """(_(?:[^_])+_)|""" +                       // 6: _italic_
    """(`[^`\n]+`)|""" +                         // 7: `code`
    """(~~(?:[^~])+~~)|""" +                     // 8: ~~strikethrough~~
    """(\[([^\]]+)\]\(([^)]+)\))|""" +           // 9: [link](url)
    """(\$\$[\s\S]+?\$\$)|""" +                  // 10: $$ block math $$
    """(\\\[[\s\S]+?\\\])|""" +                  // 11: \[ block math \]
    """((?<!\$)\$(?!\$)[^$\n]+?(?<!\$)\$)|""" + // 12: $ inline math $
    """(\\\([^\n]+?\\\))"""                      // 13: \( inline math \)
)

private fun AnnotatedString.Builder.appendRichMarkdownInternal(
    rawText: String,
    codeBackground: Color,
    codeColor: Color,
    accentColor: Color,
    linkColor: Color,
    depth: Int,
    isLatexInstalled: Boolean = true
) {
    if (depth > 3 || rawText.isEmpty()) {
        append(rawText)
        return
    }

    var lastIndex = 0
    val matches = INLINE_TOKEN_REGEX.findAll(rawText)

    for (match in matches) {
        val range = match.range
        if (range.first > lastIndex) {
            append(rawText.substring(lastIndex, range.first))
        }

        val fullMatch = match.value
        when {
            // ***bold italic*** or ___bold italic___
            fullMatch.startsWith("***") && fullMatch.endsWith("***") && fullMatch.length >= 6 -> {
                val inner = fullMatch.substring(3, fullMatch.length - 3)
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic))
                appendRichMarkdownInternal(inner, codeBackground, codeColor, accentColor, linkColor, depth + 1)
                pop()
            }
            fullMatch.startsWith("___") && fullMatch.endsWith("___") && fullMatch.length >= 6 -> {
                val inner = fullMatch.substring(3, fullMatch.length - 3)
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic))
                appendRichMarkdownInternal(inner, codeBackground, codeColor, accentColor, linkColor, depth + 1)
                pop()
            }

            // **bold** or __bold__
            fullMatch.startsWith("**") && fullMatch.endsWith("**") && fullMatch.length >= 4 -> {
                val inner = fullMatch.substring(2, fullMatch.length - 2)
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                appendRichMarkdownInternal(inner, codeBackground, codeColor, accentColor, linkColor, depth + 1)
                pop()
            }
            fullMatch.startsWith("__") && fullMatch.endsWith("__") && fullMatch.length >= 4 -> {
                val inner = fullMatch.substring(2, fullMatch.length - 2)
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                appendRichMarkdownInternal(inner, codeBackground, codeColor, accentColor, linkColor, depth + 1)
                pop()
            }

            // *italic* or _italic_
            fullMatch.startsWith("*") && fullMatch.endsWith("*") && fullMatch.length >= 2 -> {
                val inner = fullMatch.substring(1, fullMatch.length - 1)
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                appendRichMarkdownInternal(inner, codeBackground, codeColor, accentColor, linkColor, depth + 1)
                pop()
            }
            fullMatch.startsWith("_") && fullMatch.endsWith("_") && fullMatch.length >= 2 -> {
                val inner = fullMatch.substring(1, fullMatch.length - 1)
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                appendRichMarkdownInternal(inner, codeBackground, codeColor, accentColor, linkColor, depth + 1)
                pop()
            }

            // `code`
            fullMatch.startsWith("`") && fullMatch.endsWith("`") && fullMatch.length >= 2 -> {
                val inner = fullMatch.substring(1, fullMatch.length - 1)
                pushStyle(
                    SpanStyle(
                        fontFamily = ElmsSans,
                        background = codeBackground,
                        color = codeColor,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                )
                append(" $inner ")
                pop()
            }

            // ~~strikethrough~~
            fullMatch.startsWith("~~") && fullMatch.endsWith("~~") && fullMatch.length >= 4 -> {
                val inner = fullMatch.substring(2, fullMatch.length - 2)
                pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                appendRichMarkdownInternal(inner, codeBackground, codeColor, accentColor, linkColor, depth + 1)
                pop()
            }

            // [text](url)
            fullMatch.startsWith("[") && fullMatch.contains("](") && fullMatch.endsWith(")") -> {
                val closeBracket = fullMatch.indexOf("](")
                val linkText = fullMatch.substring(1, closeBracket)
                val url = fullMatch.substring(closeBracket + 2, fullMatch.length - 1)
                pushStringAnnotation(tag = "URL", annotation = url)
                pushStyle(SpanStyle(fontFamily = ElmsSans, color = linkColor, textDecoration = TextDecoration.Underline))
                append(linkText)
                pop()
                pop()
            }

            // $$ or \[ block math \]
            (fullMatch.startsWith("$$") && fullMatch.endsWith("$$") && fullMatch.length >= 4) ||
            (fullMatch.startsWith("\\[") && fullMatch.endsWith("\\]") && fullMatch.length >= 4) -> {
                val inner = fullMatch.substring(2, fullMatch.length - 2).trim()
                if (isLatexInstalled) {
                    val readable = com.arinara.fotara.ui.note.editor.KatexMathRenderer.formatToReadableMath(inner)
                    pushStyle(
                        SpanStyle(
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            background = codeBackground,
                            color = linkColor,
                            fontSize = 16.sp
                        )
                    )
                    append("  $readable  ")
                    pop()
                } else {
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = codeBackground,
                            color = codeColor,
                            fontSize = 12.sp
                        )
                    )
                    append("$$ $inner $$")
                    pop()
                }
            }

            // $ or \( inline math \)
            (fullMatch.startsWith("$") && fullMatch.endsWith("$") && fullMatch.length >= 2) ||
            (fullMatch.startsWith("\\(") && fullMatch.endsWith("\\)") && fullMatch.length >= 4) -> {
                val inner = if (fullMatch.startsWith("$")) {
                    fullMatch.substring(1, fullMatch.length - 1).trim()
                } else {
                    fullMatch.substring(2, fullMatch.length - 2).trim()
                }
                if (isLatexInstalled) {
                    val readable = com.arinara.fotara.ui.note.editor.KatexMathRenderer.formatToReadableMath(inner)
                    pushStyle(
                        SpanStyle(
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Medium,
                            background = codeBackground,
                            color = linkColor,
                            fontSize = 14.sp
                        )
                    )
                    append(" $readable ")
                    pop()
                } else {
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = codeBackground,
                            color = codeColor,
                            fontSize = 12.sp
                        )
                    )
                    append("$$inner$")
                    pop()
                }
            }

            else -> {
                append(fullMatch)
            }
        }
        lastIndex = range.last + 1
    }

    if (lastIndex < rawText.length) {
        append(rawText.substring(lastIndex))
    }
}

/**
 * Universal Composable for rendering rich text with inline markdown spans
 * (bold, italic, inline code, strikethrough, links).
 */
@Composable
fun RichMarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = ElectricSlateTokens.TextPrimary,
    fontSize: TextUnit = 14.sp,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    codeBackground: Color = ElectricSlateTokens.SurfaceElevated,
    codeColor: Color = ElectricSlateTokens.TextPrimary,
    accentColor: Color = ElectricSlateTokens.PrimaryAccent,
    linkColor: Color = ElectricSlateTokens.PrimaryAccent,
    textDecoration: TextDecoration? = null
) {
    val context = LocalContext.current
    val isLatexInstalled = true
    val annotated = remember(text, codeBackground, codeColor, accentColor, linkColor, isLatexInstalled) {
        buildRichMarkdownAnnotatedString(
            text = text,
            codeBackground = codeBackground,
            codeColor = codeColor,
            accentColor = accentColor,
            linkColor = linkColor,
            isLatexInstalled = isLatexInstalled
        )
    }

    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val hasLinks = remember(annotated) { annotated.getStringAnnotations("URL", 0, annotated.length).isNotEmpty() }
    val clickModifier = if (hasLinks) {
        modifier.pointerInput(annotated) {
            detectTapGestures { pos ->
                layoutResult?.let { layout ->
                    val offset = layout.getOffsetForPosition(pos)
                    val urlAnnotations = annotated.getStringAnnotations(tag = "URL", start = offset, end = offset)
                    urlAnnotations.firstOrNull()?.let { annotation ->
                        LinkLauncher.openUrl(context, annotation.item)
                    }
                }
            }
        }
    } else {
        modifier
    }

    Text(
        text = annotated,
        modifier = clickModifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        lineHeight = lineHeight,
        maxLines = maxLines,
        overflow = overflow,
        textDecoration = textDecoration,
        onTextLayout = { layoutResult = it }
    )
}

/**
 * Blockquote composable with vertical gold bar and indented italic styling.
 */
@Composable
fun RichMarkdownBlockquote(
    quote: String,
    modifier: Modifier = Modifier,
    accentColor: Color = ElectricSlateTokens.PrimaryAccent,
    backgroundColor: Color = ElectricSlateTokens.SurfacePanel,
    borderColor: Color = ElectricSlateTokens.BorderSubtle,
    textColor: Color = ElectricSlateTokens.TextPrimary.copy(alpha = 0.88f)
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(12.dp))
            .height(IntrinsicSize.Min)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(accentColor)
        )
        Spacer(modifier = Modifier.width(10.dp))
        RichMarkdownText(
            text = quote,
            color = textColor,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            accentColor = accentColor
        )
    }
}

/**
 * Multiline code block composable with dark editor background and monospace font.
 */
@Composable
fun RichMarkdownCodeBlock(
    code: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = ElectricSlateTokens.SurfacePanel,
    borderColor: Color = ElectricSlateTokens.BorderSubtle,
    codeColor: Color = ElectricSlateTokens.TextPrimary
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(12.dp))
            .padding(12.dp)
            .horizontalScroll(rememberScrollState())
    ) {
        Text(
            text = code,
            color = codeColor,
            fontFamily = ElmsSans,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    }
}

/**
 * Dedicated visual Markdown table composable:
 * - Elevated surface with RoundedCornerShape(8.dp)
 * - 1dp border with cardBorder
 * - Tinted header row (Color(0xFF1E254A)) with bold text
 * - Alternating subtle row backgrounds
 * - 1dp grid dividers between cells
 * - Horizontal scroll inside container for wide tables
 * - Rich Markdown formatting in each cell
 */
@Composable
fun RichMarkdownTable(
    headers: List<String>,
    rows: List<List<String>>,
    modifier: Modifier = Modifier,
    backgroundColor: Color = ElectricSlateTokens.SurfacePanel,
    headerBackground: Color = ElectricSlateTokens.SurfaceElevated,
    borderColor: Color = ElectricSlateTokens.BorderSubtle,
    headerTextColor: Color = ElectricSlateTokens.TextPrimary,
    textColor: Color = ElectricSlateTokens.TextPrimary,
    accentColor: Color = ElectricSlateTokens.PrimaryAccent
) {
    if (headers.isEmpty()) return

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(12.dp))
    ) {
        val containerWidth = maxWidth
        val numCols = headers.size
        val dividerTotalWidth = if (numCols > 1) (numCols - 1).dp else 0.dp
        val availableContentWidth = (containerWidth - dividerTotalWidth).coerceAtLeast(0.dp)

        val naturalColWidths = remember(headers, rows) {
            headers.indices.map { colIndex ->
                val headerLen = headers.getOrNull(colIndex)?.length ?: 0
                val maxRowLen = rows.maxOfOrNull { it.getOrNull(colIndex)?.length ?: 0 } ?: 0
                val maxLen = maxOf(headerLen, maxRowLen)
                (maxOf(80, maxLen * 9) + 24).dp.coerceIn(80.dp, 300.dp)
            }
        }

        val totalNaturalWidth = naturalColWidths.fold(0.dp) { acc, w -> acc + w }
        val requiresHorizontalScroll = totalNaturalWidth > availableContentWidth

        val finalColWidths = if (!requiresHorizontalScroll && totalNaturalWidth > 0.dp && availableContentWidth > 0.dp) {
            val ratio = availableContentWidth.value / totalNaturalWidth.value
            naturalColWidths.map { (it.value * ratio).dp }
        } else {
            naturalColWidths
        }

        val scrollModifier = if (requiresHorizontalScroll) {
            Modifier.horizontalScroll(rememberScrollState())
        } else {
            Modifier.fillMaxWidth()
        }

        Box(modifier = scrollModifier) {
            Column(modifier = if (!requiresHorizontalScroll) Modifier.fillMaxWidth() else Modifier) {
                // Header Row
                Row(
                    modifier = Modifier
                        .background(headerBackground)
                        .height(IntrinsicSize.Min)
                        .then(if (!requiresHorizontalScroll) Modifier.fillMaxWidth() else Modifier),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    headers.forEachIndexed { colIndex, headerText ->
                        if (colIndex > 0) {
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .fillMaxHeight()
                                    .background(borderColor)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(finalColWidths[colIndex])
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            RichMarkdownText(
                                text = headerText,
                                color = headerTextColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                accentColor = accentColor
                            )
                        }
                    }
                }

                // Divider between header and body
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(borderColor)
                )

                // Body Rows
                rows.forEachIndexed { rowIndex, rowCells ->
                    if (rowIndex > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(borderColor.copy(alpha = 0.6f))
                        )
                    }
                    Row(
                        modifier = Modifier
                            .background(if (rowIndex % 2 == 1) ElectricSlateTokens.SurfaceElevated.copy(alpha = 0.5f) else backgroundColor)
                            .height(IntrinsicSize.Min)
                            .then(if (!requiresHorizontalScroll) Modifier.fillMaxWidth() else Modifier),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        headers.indices.forEach { colIndex ->
                            if (colIndex > 0) {
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .fillMaxHeight()
                                        .background(borderColor.copy(alpha = 0.6f))
                                )
                            }
                            val cellText = rowCells.getOrNull(colIndex) ?: ""
                            Box(
                                modifier = Modifier
                                    .width(finalColWidths[colIndex])
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                val imgRegex = Regex("""!\[([^\]]*)\]\(([^)]+)\)""")
                                val imgMatch = imgRegex.find(cellText)
                                if (imgMatch != null) {
                                    val alt = imgMatch.groupValues[1]
                                    val url = imgMatch.groupValues[2]
                                    val beforeText = cellText.substring(0, imgMatch.range.first).trim()
                                    val afterText = cellText.substring(imgMatch.range.last + 1).trim()
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        if (beforeText.isNotEmpty()) {
                                            RichMarkdownText(
                                                text = beforeText,
                                                color = textColor.copy(alpha = 0.92f),
                                                fontSize = 14.sp,
                                                accentColor = accentColor
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                        }
                                        AsyncImage(
                                            model = url,
                                            contentDescription = alt.ifBlank { "Table cell image" },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.Fit
                                        )
                                        if (afterText.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            RichMarkdownText(
                                                text = afterText,
                                                color = textColor.copy(alpha = 0.92f),
                                                fontSize = 14.sp,
                                                accentColor = accentColor
                                            )
                                        }
                                    }
                                } else {
                                    RichMarkdownText(
                                        text = cellText,
                                        color = textColor.copy(alpha = 0.92f),
                                        fontSize = 14.sp,
                                        accentColor = accentColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun isTableSeparatorLine(line: String): Boolean {
    val trimmed = line.trim()
    if (!trimmed.contains('-') || !trimmed.contains('|')) return false
    val cells = parseTableRowCells(trimmed)
    if (cells.isEmpty()) return false
    return cells.all { cell -> cell.matches(Regex("""^:?-+:?$""")) }
}

internal fun parseTableRowCells(line: String): List<String> {
    var trimmed = line.trim()
    if (trimmed.startsWith("|") && trimmed.endsWith("|") && trimmed.length >= 2) {
        trimmed = trimmed.substring(1, trimmed.length - 1)
    } else if (trimmed.startsWith("|")) {
        trimmed = trimmed.substring(1)
    } else if (trimmed.endsWith("|")) {
        trimmed = trimmed.substring(0, trimmed.length - 1)
    }
    val cells = mutableListOf<String>()
    val current = StringBuilder()
    var i = 0
    while (i < trimmed.length) {
        val ch = trimmed[i]
        if (ch == '\\' && i + 1 < trimmed.length && trimmed[i + 1] == '|') {
            current.append('|')
            i += 2
        } else if (ch == '|') {
            cells.add(current.toString().trim())
            current.clear()
            i++
        } else {
            current.append(ch)
            i++
        }
    }
    cells.add(current.toString().trim())
    return cells
}

/**
 * High-level markdown document column that parses and renders:
 * - H1, H2, H3 headers
 * - Blockquotes (`> `)
 * - Bullet lists (`- `, `* `)
 * - Numbered lists (`1. `, `2. `)
 * - Code blocks (```)
 * - Markdown tables (`| ... |`)
 * - Paragraphs with inline bold/italic/code/strikethrough
 */
@Composable
fun RichMarkdownColumn(
    markdown: String,
    modifier: Modifier = Modifier,
    primaryTextColor: Color = ElectricSlateTokens.TextPrimary,
    accentColor: Color = ElectricSlateTokens.PrimaryAccent,
    cardBg: Color = ElectricSlateTokens.SurfacePanel,
    cardBorder: Color = ElectricSlateTokens.BorderSubtle,
    onToggleChecklistLine: ((Int) -> Unit)? = null,
    onOpenDocument: ((docId: Long, isDocx: Boolean) -> Unit)? = null
) {
    val lines = remember(markdown) { markdown.lines() }

    Column(modifier = modifier.fillMaxWidth()) {
        var inCodeBlock = false
        val codeBlockBuffer = StringBuilder()

        var lineIndex = 0
        while (lineIndex < lines.size) {
            val rawLine = lines[lineIndex]
            val trimmed = rawLine.trim()

            // Code block handling (```)
            if (trimmed.startsWith("```")) {
                if (inCodeBlock) {
                    // Close code block
                    inCodeBlock = false
                    RichMarkdownCodeBlock(
                        code = codeBlockBuffer.toString().trimEnd(),
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                    codeBlockBuffer.clear()
                } else {
                    // Start code block
                    inCodeBlock = true
                }
                lineIndex++
                continue
            }

            if (inCodeBlock) {
                codeBlockBuffer.append(rawLine).append("\n")
                lineIndex++
                continue
            }

            // Document attachments: [title.pdf](fotara://doc/123) or [title.docx](fotara://doc/123)
            val docMatch = Regex("""^\[([^\]]+)\]\(fotara://doc/(\d+)\)$""").find(trimmed)
            if (docMatch != null) {
                val title = docMatch.groupValues[1]
                val docId = docMatch.groupValues[2].toLongOrNull() ?: 0L
                val isDocx = title.endsWith(".docx", ignoreCase = true)
                DocumentAttachmentCard(
                    title = title,
                    docId = docId,
                    isDocx = isDocx,
                    onOpenDocument = onOpenDocument,
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    accentColor = accentColor,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
                lineIndex++
                continue
            }

            // Resizable Markdown Image
            val imgMatch = Regex("""^!\[([^\]]*)\]\(([^)]+)\)$""").find(trimmed)
            if (imgMatch != null) {
                val alt = imgMatch.groupValues[1]
                val url = imgMatch.groupValues[2]
                ResizableMarkdownImage(
                    imageUrl = url,
                    altText = alt,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                lineIndex++
                continue
            }

            // Skip empty lines
            if (trimmed.isEmpty()) {
                lineIndex++
                continue
            }

            // Table detection
            if (trimmed.contains('|') && lineIndex + 1 < lines.size && isTableSeparatorLine(lines[lineIndex + 1])) {
                val headerCells = parseTableRowCells(trimmed)
                lineIndex += 2 // Skip header line and separator line
                val dataRows = mutableListOf<List<String>>()
                while (lineIndex < lines.size) {
                    val nextRow = lines[lineIndex].trim()
                    if (nextRow.isEmpty() || !nextRow.contains('|') || nextRow.startsWith("```") || nextRow.startsWith("#")) {
                        break
                    }
                    dataRows.add(parseTableRowCells(nextRow))
                    lineIndex++
                }
                RichMarkdownTable(
                    headers = headerCells,
                    rows = dataRows,
                    backgroundColor = cardBg,
                    headerBackground = ElectricSlateTokens.SurfaceElevated,
                    borderColor = cardBorder,
                    headerTextColor = ElectricSlateTokens.TextPrimary,
                    textColor = primaryTextColor,
                    accentColor = accentColor,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                continue
            }

            // Display Math Block ($$ ... $$ or \[ ... \])
            if (trimmed.startsWith("$$") || trimmed.startsWith("\\[")) {
                val isBracket = trimmed.startsWith("\\[")
                val endDelimiter = if (isBracket) "\\]" else "$$"
                val startDelimiter = if (isBracket) "\\[" else "$$"
                val mathBuffer = StringBuilder()
                if (trimmed.endsWith(endDelimiter) && trimmed.length >= 4) {
                    mathBuffer.append(trimmed.substring(2, trimmed.length - 2).trim())
                    lineIndex++
                } else {
                    mathBuffer.append(trimmed.removePrefix(startDelimiter).trim())
                    lineIndex++
                    while (lineIndex < lines.size) {
                        val currentRawLine = lines[lineIndex]
                        val mathLine = currentRawLine.trim()
                        val endIdx = mathLine.indexOf(endDelimiter)
                        if (endIdx != -1) {
                            val beforeEnd = mathLine.substring(0, endIdx).trim()
                            if (beforeEnd.isNotEmpty()) {
                                mathBuffer.append("\n").append(beforeEnd)
                            }
                            lineIndex++
                            break
                        } else {
                            mathBuffer.append("\n").append(currentRawLine)
                            lineIndex++
                        }
                    }
                }

                val mathCode = mathBuffer.toString().trim()
                if (mathCode.isNotEmpty()) {
                    ComposeMathView(
                        latex = mathCode,
                        isDisplayMode = true,
                        textColor = ElectricSlateTokens.TextPrimary,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
                continue
            }

            when {
                // Header 1 (# Title)
                trimmed.startsWith("# ") -> {
                    Text(
                        text = trimmed.removePrefix("# ").trim(),
                        color = accentColor,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    )
                }

                // Header 2 (## Section)
                trimmed.startsWith("## ") -> {
                    Text(
                        text = trimmed.removePrefix("## ").trim(),
                        color = accentColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }

                // Header 3 (### Subtitle / Version)
                trimmed.startsWith("### ") -> {
                    Text(
                        text = trimmed.removePrefix("### ").trim(),
                        color = primaryTextColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }

                // Blockquote (> Quote)
                trimmed.startsWith("> ") || trimmed.startsWith(">") -> {
                    val quoteContent = trimmed.removePrefix(">").trim()
                    RichMarkdownBlockquote(
                        quote = quoteContent,
                        accentColor = accentColor,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // Checklist items (- [ ] or - [x] with optional indentation)
                Regex("""^(\s*[-*+]\s*)\[([ xX])\]\s*(.*)$""").containsMatchIn(rawLine) -> {
                    val chkMatch = Regex("""^(\s*[-*+]\s*)\[([ xX])\]\s*(.*)$""").find(rawLine)!!
                    val isChecked = chkMatch.groupValues[2].equals("x", ignoreCase = true)
                    val itemContent = chkMatch.groupValues[3]
                    val leadingSpaces = rawLine.takeWhile { it == ' ' }.length
                    val indentLevel = (leadingSpaces / 2).coerceIn(0, 3)

                    val currentLineIdx = lineIndex
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = (indentLevel * 24).dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = onToggleChecklistLine != null) {
                                onToggleChecklistLine?.invoke(currentLineIdx)
                            }
                            .padding(vertical = 4.dp, horizontal = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(18.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isChecked) accentColor else Color.Transparent)
                                .border(
                                    BorderStroke(2.dp, accentColor),
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isChecked) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Checked",
                                    tint = ElectricSlateTokens.BgCanvas,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        RichMarkdownText(
                            text = itemContent,
                            color = if (isChecked) primaryTextColor.copy(alpha = 0.5f) else primaryTextColor.copy(alpha = 0.92f),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            accentColor = accentColor,
                            textDecoration = if (isChecked) TextDecoration.LineThrough else null
                        )
                    }
                }

                // Bullet item (- Item or * Item)
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    val itemContent = trimmed.substring(2).trim()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 7.dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        RichMarkdownText(
                            text = itemContent,
                            color = primaryTextColor.copy(alpha = 0.92f),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            accentColor = accentColor
                        )
                    }
                }

                // Numbered list (1. Item)
                trimmed.matches(Regex("""^\d+\.\s+.*""")) -> {
                    val dotIndex = trimmed.indexOf('.')
                    val number = trimmed.substring(0, dotIndex)
                    val content = trimmed.substring(dotIndex + 1).trim()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(accentColor.copy(alpha = 0.2f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = number,
                                color = accentColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        RichMarkdownText(
                            text = content,
                            color = primaryTextColor.copy(alpha = 0.92f),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            accentColor = accentColor
                        )
                    }
                }

                // Divider (--- or ***)
                trimmed == "---" || trimmed == "***" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(cardBorder)
                            .padding(vertical = 6.dp)
                    )
                }

                // Regular paragraph
                else -> {
                    RichMarkdownText(
                        text = trimmed,
                        color = primaryTextColor.copy(alpha = 0.88f),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        accentColor = accentColor,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
            lineIndex++
        }

        // Flush any unclosed code block
        if (inCodeBlock && codeBlockBuffer.isNotEmpty()) {
            RichMarkdownCodeBlock(
                code = codeBlockBuffer.toString().trimEnd(),
                modifier = Modifier.padding(vertical = 6.dp)
            )
        }
    }
}

@Composable
fun DocumentAttachmentCard(
    title: String,
    docId: Long,
    isDocx: Boolean,
    onOpenDocument: ((Long, Boolean) -> Unit)?,
    cardBg: Color,
    cardBorder: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val badgeColor = if (isDocx) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.Destructive
    val badgeLabel = if (isDocx) "DOCX" else "PDF"
    val icon = if (isDocx) Icons.Default.Description else Icons.Default.PictureAsPdf

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(12.dp))
            .clickable(enabled = onOpenDocument != null) {
                onOpenDocument?.invoke(docId, isDocx)
            }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(badgeColor.copy(alpha = 0.2f))
                .border(1.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = badgeLabel,
                tint = badgeColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = ElmsSans,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ElectricSlateTokens.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = badgeColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = badgeLabel,
                        color = badgeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Internal Viewer Engine",
                    fontSize = 12.sp,
                    color = ElectricSlateTokens.TextMuted
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = "Open Document",
            tint = accentColor,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun ResizableMarkdownImage(
    imageUrl: String,
    altText: String,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ElectricSlateTokens.SurfacePanel)
                .border(BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle), RoundedCornerShape(12.dp))
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 3.0f)
                    }
                }
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = altText.ifBlank { "Embedded Image" },
                modifier = Modifier
                    .fillMaxWidth(fraction = (0.95f * scale).coerceIn(0.2f, 1f))
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.FillWidth
            )
        }
        if (altText.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = altText,
                fontFamily = ElmsSans,
                fontSize = 12.sp,
                color = ElectricSlateTokens.TextMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

