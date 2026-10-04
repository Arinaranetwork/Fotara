// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.util

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.BackgroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import com.arinara.fotara.data.model.TextNote

/**
 * Handles layout and pagination of TextNote markdown content for PDF export.
 * Formats headings, bullets, numbered lists, checklists ([x] / [ ]), quotes, code blocks,
 * horizontal rules, and inline styling (bold, italic, code, links).
 */
object TextNotePdfRenderer {

    const val PAGE_WIDTH = 595
    const val PAGE_HEIGHT = 842

    private const val MARGIN_LEFT = 40f
    private const val MARGIN_RIGHT = 40f
    private const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT // 515f
    private const val CONTENT_TOP = 72f
    private const val CONTENT_BOTTOM = 792f // leaves 50pt at bottom
    private const val USABLE_HEIGHT = CONTENT_BOTTOM - CONTENT_TOP

    private sealed class MarkdownBlock {
        data class Title(val title: String) : MarkdownBlock()
        data class Heading(val level: Int, val text: String) : MarkdownBlock()
        data class Paragraph(val text: String) : MarkdownBlock()
        data class BulletItem(val text: String) : MarkdownBlock()
        data class NumberedItem(val prefix: String, val text: String) : MarkdownBlock()
        data class ChecklistItem(val isChecked: Boolean, val text: String) : MarkdownBlock()
        data class Blockquote(val text: String) : MarkdownBlock()
        data class CodeBlock(val lines: List<String>) : MarkdownBlock()
        object Divider : MarkdownBlock()
    }

    private fun parseMarkdownBlocks(textNote: TextNote): List<MarkdownBlock> {
        val blocks = mutableListOf<MarkdownBlock>()
        if (textNote.title.isNotBlank()) {
            blocks.add(MarkdownBlock.Title(textNote.title))
        }

        val rawLines = textNote.bodyMarkdown.lines()
        var i = 0
        while (i < rawLines.size) {
            val line = rawLines[i]
            val trimmed = line.trim()

            if (trimmed.isEmpty()) {
                i++
                continue
            }

            // Code block
            if (trimmed.startsWith("```")) {
                val codeLines = mutableListOf<String>()
                i++
                while (i < rawLines.size && !rawLines[i].trim().startsWith("```")) {
                    codeLines.add(rawLines[i])
                    i++
                }
                if (i < rawLines.size) i++ // skip closing ```
                blocks.add(MarkdownBlock.CodeBlock(codeLines))
                continue
            }

            // Divider
            if (trimmed == "---" || trimmed == "***" || trimmed == "___") {
                blocks.add(MarkdownBlock.Divider)
                i++
                continue
            }

            // Headings
            if (trimmed.startsWith("### ")) {
                blocks.add(MarkdownBlock.Heading(3, trimmed.removePrefix("### ").trim()))
                i++
                continue
            }
            if (trimmed.startsWith("## ")) {
                blocks.add(MarkdownBlock.Heading(2, trimmed.removePrefix("## ").trim()))
                i++
                continue
            }
            if (trimmed.startsWith("# ")) {
                blocks.add(MarkdownBlock.Heading(1, trimmed.removePrefix("# ").trim()))
                i++
                continue
            }

            // Blockquote
            if (trimmed.startsWith(">")) {
                blocks.add(MarkdownBlock.Blockquote(trimmed.removePrefix(">").trim()))
                i++
                continue
            }

            // Checklist
            val checklistRegex = Regex("^[-*+]\\s+\\[([ xX])\\]\\s*(.*)")
            val checklistMatch = checklistRegex.find(trimmed)
            if (checklistMatch != null) {
                val checkMark = checklistMatch.groupValues[1]
                val isChecked = checkMark.equals("x", ignoreCase = true)
                val itemText = checklistMatch.groupValues[2]
                blocks.add(MarkdownBlock.ChecklistItem(isChecked, itemText))
                i++
                continue
            }

            // Bullet item
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ")) {
                val itemText = trimmed.substring(2).trim()
                blocks.add(MarkdownBlock.BulletItem(itemText))
                i++
                continue
            }

            // Numbered item
            val numberedRegex = Regex("^(\\d+\\.)\\s+(.*)")
            val numberedMatch = numberedRegex.find(trimmed)
            if (numberedMatch != null) {
                val prefix = numberedMatch.groupValues[1]
                val itemText = numberedMatch.groupValues[2]
                blocks.add(MarkdownBlock.NumberedItem(prefix, itemText))
                i++
                continue
            }

            // Regular paragraph
            blocks.add(MarkdownBlock.Paragraph(trimmed))
            i++
        }

        return blocks
    }

    fun parseMarkdownToSpannable(rawText: String, defaultColor: Int = Color.rgb(33, 37, 41)): SpannableStringBuilder {
        // Replace markdown links [label](url) with label
        val linkRegex = Regex("\\[([^\\]]+)\\]\\([^\\)]+\\)")
        val textWithoutLinks = linkRegex.replace(rawText) { match -> match.groupValues[1] }

        val sb = SpannableStringBuilder()
        var i = 0
        val len = textWithoutLinks.length

        while (i < len) {
            // Inline code `...`
            if (textWithoutLinks[i] == '`') {
                val endIdx = textWithoutLinks.indexOf('`', i + 1)
                if (endIdx != -1) {
                    val code = textWithoutLinks.substring(i + 1, endIdx)
                    val start = sb.length
                    sb.append(code)
                    sb.setSpan(TypefaceSpan("monospace"), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    sb.setSpan(BackgroundColorSpan(Color.rgb(240, 242, 245)), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    i = endIdx + 1
                    continue
                }
            }

            // Bold-italic ***...***
            if (textWithoutLinks.startsWith("***", i)) {
                val endIdx = textWithoutLinks.indexOf("***", i + 3)
                if (endIdx != -1) {
                    val content = textWithoutLinks.substring(i + 3, endIdx)
                    val start = sb.length
                    sb.append(content)
                    sb.setSpan(StyleSpan(Typeface.BOLD_ITALIC), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    i = endIdx + 3
                    continue
                }
            }

            // Bold **...**
            if (textWithoutLinks.startsWith("**", i)) {
                val endIdx = textWithoutLinks.indexOf("**", i + 2)
                if (endIdx != -1) {
                    val content = textWithoutLinks.substring(i + 2, endIdx)
                    val start = sb.length
                    sb.append(content)
                    sb.setSpan(StyleSpan(Typeface.BOLD), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    i = endIdx + 2
                    continue
                }
            }

            // Italic *...*
            if (textWithoutLinks[i] == '*' && (i + 1 < len && textWithoutLinks[i + 1] != ' ')) {
                val endIdx = textWithoutLinks.indexOf('*', i + 1)
                if (endIdx != -1) {
                    val content = textWithoutLinks.substring(i + 1, endIdx)
                    val start = sb.length
                    sb.append(content)
                    sb.setSpan(StyleSpan(Typeface.ITALIC), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    i = endIdx + 1
                    continue
                }
            }

            // Strikethrough ~~...~~
            if (textWithoutLinks.startsWith("~~", i)) {
                val endIdx = textWithoutLinks.indexOf("~~", i + 2)
                if (endIdx != -1) {
                    val content = textWithoutLinks.substring(i + 2, endIdx)
                    val start = sb.length
                    sb.append(content)
                    sb.setSpan(StrikethroughSpan(), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    i = endIdx + 2
                    continue
                }
            }

            sb.append(textWithoutLinks[i])
            i++
        }

        return sb
    }

    private class RenderableItem(
        val height: Float,
        val drawAction: (canvas: Canvas, x: Float, y: Float) -> Unit
    )

    private fun layoutBlocks(blocks: List<MarkdownBlock>): List<RenderableItem> {
        val items = mutableListOf<RenderableItem>()

        for (block in blocks) {
            when (block) {
                is MarkdownBlock.Title -> {
                    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = 20f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        color = Color.rgb(17, 24, 39)
                    }
                    val layout = StaticLayout.Builder.obtain(block.title, 0, block.title.length, paint, CONTENT_WIDTH.toInt())
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setIncludePad(false)
                        .build()
                    val totalH = layout.height.toFloat() + 20f
                    items.add(RenderableItem(totalH) { canvas, x, y ->
                        canvas.save()
                        canvas.translate(x, y)
                        layout.draw(canvas)
                        canvas.restore()
                    })
                }

                is MarkdownBlock.Heading -> {
                    val size = when (block.level) {
                        1 -> 16f
                        2 -> 14f
                        else -> 12f
                    }
                    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = size
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        color = Color.rgb(17, 24, 39)
                    }
                    val text = parseMarkdownToSpannable(block.text, paint.color)
                    val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, CONTENT_WIDTH.toInt())
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setIncludePad(false)
                        .build()
                    val totalH = layout.height.toFloat() + 14f
                    items.add(RenderableItem(totalH) { canvas, x, y ->
                        canvas.save()
                        canvas.translate(x, y + 4f)
                        layout.draw(canvas)
                        canvas.restore()
                    })
                }

                is MarkdownBlock.Paragraph -> {
                    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = 11f
                        typeface = Typeface.DEFAULT
                        color = Color.rgb(33, 37, 41)
                    }
                    val text = parseMarkdownToSpannable(block.text, paint.color)
                    val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, CONTENT_WIDTH.toInt())
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(0f, 1.25f)
                        .setIncludePad(false)
                        .build()
                    val totalH = layout.height.toFloat() + 10f
                    items.add(RenderableItem(totalH) { canvas, x, y ->
                        canvas.save()
                        canvas.translate(x, y)
                        layout.draw(canvas)
                        canvas.restore()
                    })
                }

                is MarkdownBlock.BulletItem -> {
                    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = 11f
                        typeface = Typeface.DEFAULT
                        color = Color.rgb(33, 37, 41)
                    }
                    val text = parseMarkdownToSpannable(block.text, paint.color)
                    val indent = 16f
                    val availW = (CONTENT_WIDTH - indent).toInt()
                    val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, availW)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(0f, 1.25f)
                        .setIncludePad(false)
                        .build()
                    val totalH = layout.height.toFloat() + 6f
                    items.add(RenderableItem(totalH) { canvas, x, y ->
                        // Draw bullet point
                        val bulletPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.rgb(55, 65, 81)
                            style = Paint.Style.FILL
                        }
                        canvas.drawCircle(x + 5f, y + 7f, 2.5f, bulletPaint)
                        canvas.save()
                        canvas.translate(x + indent, y)
                        layout.draw(canvas)
                        canvas.restore()
                    })
                }

                is MarkdownBlock.NumberedItem -> {
                    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = 11f
                        typeface = Typeface.DEFAULT
                        color = Color.rgb(33, 37, 41)
                    }
                    val prefixPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = 11f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        color = Color.rgb(55, 65, 81)
                    }
                    val prefixW = prefixPaint.measureText(block.prefix) + 6f
                    val indent = maxOf(20f, prefixW)
                    val availW = (CONTENT_WIDTH - indent).toInt()
                    val text = parseMarkdownToSpannable(block.text, paint.color)
                    val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, availW)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(0f, 1.25f)
                        .setIncludePad(false)
                        .build()
                    val totalH = layout.height.toFloat() + 6f
                    items.add(RenderableItem(totalH) { canvas, x, y ->
                        canvas.drawText(block.prefix, x, y + 10f, prefixPaint)
                        canvas.save()
                        canvas.translate(x + indent, y)
                        layout.draw(canvas)
                        canvas.restore()
                    })
                }

                is MarkdownBlock.ChecklistItem -> {
                    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = 11f
                        typeface = Typeface.DEFAULT
                        color = if (block.isChecked) Color.GRAY else Color.rgb(33, 37, 41)
                    }
                    val indent = 20f
                    val availW = (CONTENT_WIDTH - indent).toInt()
                    val text = parseMarkdownToSpannable(block.text, paint.color)
                    val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, availW)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(0f, 1.25f)
                        .setIncludePad(false)
                        .build()
                    val totalH = layout.height.toFloat() + 6f
                    items.add(RenderableItem(totalH) { canvas, x, y ->
                        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            style = Paint.Style.STROKE
                            strokeWidth = 1.2f
                            color = if (block.isChecked) Color.rgb(224, 169, 109) else Color.GRAY
                        }
                        val boxRect = RectF(x, y + 2f, x + 11f, y + 13f)
                        canvas.drawRoundRect(boxRect, 2f, 2f, boxPaint)

                        if (block.isChecked) {
                            val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                style = Paint.Style.STROKE
                                strokeWidth = 1.5f
                                strokeCap = Paint.Cap.ROUND
                                color = Color.rgb(224, 169, 109)
                            }
                            canvas.drawLine(x + 2.5f, y + 7f, x + 5f, y + 10.5f, checkPaint)
                            canvas.drawLine(x + 5f, y + 10.5f, x + 9f, y + 4.5f, checkPaint)
                        }

                        canvas.save()
                        canvas.translate(x + indent, y)
                        layout.draw(canvas)
                        canvas.restore()
                    })
                }

                is MarkdownBlock.Blockquote -> {
                    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = 11f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                        color = Color.rgb(75, 85, 99)
                    }
                    val indent = 16f
                    val availW = (CONTENT_WIDTH - indent).toInt()
                    val text = parseMarkdownToSpannable(block.text, paint.color)
                    val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, availW)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(0f, 1.25f)
                        .setIncludePad(false)
                        .build()
                    val totalH = layout.height.toFloat() + 8f
                    items.add(RenderableItem(totalH) { canvas, x, y ->
                        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.rgb(224, 169, 109) // AccentGold
                            strokeWidth = 3f
                            style = Paint.Style.STROKE
                            strokeCap = Paint.Cap.ROUND
                        }
                        canvas.drawLine(x + 2f, y + 2f, x + 2f, y + totalH - 6f, barPaint)

                        canvas.save()
                        canvas.translate(x + indent, y)
                        layout.draw(canvas)
                        canvas.restore()
                    })
                }

                is MarkdownBlock.CodeBlock -> {
                    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = 10f
                        typeface = Typeface.MONOSPACE
                        color = Color.rgb(30, 41, 59)
                    }
                    val codeContent = block.lines.joinToString("\n")
                    val layout = StaticLayout.Builder.obtain(codeContent, 0, codeContent.length, paint, (CONTENT_WIDTH - 16f).toInt())
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(0f, 1.2f)
                        .setIncludePad(false)
                        .build()
                    val totalH = layout.height.toFloat() + 16f
                    items.add(RenderableItem(totalH) { canvas, x, y ->
                        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.rgb(241, 245, 249)
                            style = Paint.Style.FILL
                        }
                        canvas.drawRoundRect(RectF(x, y, x + CONTENT_WIDTH, y + totalH - 4f), 4f, 4f, bgPaint)

                        canvas.save()
                        canvas.translate(x + 8f, y + 6f)
                        layout.draw(canvas)
                        canvas.restore()
                    })
                }

                is MarkdownBlock.Divider -> {
                    val totalH = 16f
                    items.add(RenderableItem(totalH) { canvas, x, y ->
                        val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.rgb(226, 232, 240)
                            strokeWidth = 1f
                        }
                        canvas.drawLine(x, y + 8f, x + CONTENT_WIDTH, y + 8f, divPaint)
                    })
                }
            }
        }

        return items
    }

    /**
     * Splits renderable items into pages.
     */
    private fun paginateItems(items: List<RenderableItem>): List<List<RenderableItem>> {
        val pages = mutableListOf<MutableList<RenderableItem>>()
        var currentPage = mutableListOf<RenderableItem>()
        var currentY = 0f

        for (item in items) {
            if (currentY + item.height > USABLE_HEIGHT && currentPage.isNotEmpty()) {
                pages.add(currentPage)
                currentPage = mutableListOf()
                currentY = 0f
            }
            currentPage.add(item)
            currentY += item.height
        }

        if (currentPage.isNotEmpty() || pages.isEmpty()) {
            pages.add(currentPage)
        }

        return pages
    }

    /**
     * Estimates page count quickly based on textNote content for pre-checks.
     */
    fun estimatePageCount(textNote: TextNote): Int {
        val blocks = parseMarkdownBlocks(textNote)
        val items = layoutBlocks(blocks)
        val pages = paginateItems(items)
        return pages.size.coerceAtLeast(1)
    }

    /**
     * Renders text note pages into [pdfDoc]. Returns the number of pages rendered.
     */
    fun renderTextNote(
        pdfDoc: PdfDocument,
        textNote: TextNote,
        startPageNum: Int,
        totalPages: Int,
        onPageRendered: ((pageNum: Int) -> Unit)? = null
    ): Int {
        val blocks = parseMarkdownBlocks(textNote)
        val items = layoutBlocks(blocks)
        val pages = paginateItems(items)

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10f
            color = Color.GRAY
        }
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 0.8f
        }

        var pageIndex = 0
        for (pageItems in pages) {
            val pageNum = startPageNum + pageIndex
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
            val pdfPage = pdfDoc.startPage(pageInfo)
            val canvas = pdfPage.canvas

            canvas.drawColor(Color.WHITE)

            // Header
            canvas.drawText("Page $pageNum of $totalPages • Fotara Text Note", MARGIN_LEFT, 36f, headerPaint)
            canvas.drawLine(MARGIN_LEFT, 48f, MARGIN_LEFT + CONTENT_WIDTH, 48f, dividerPaint)

            var y = CONTENT_TOP
            for (item in pageItems) {
                item.drawAction(canvas, MARGIN_LEFT, y)
                y += item.height
            }

            pdfDoc.finishPage(pdfPage)
            onPageRendered?.invoke(pageNum)
            pageIndex++
        }

        return pages.size
    }
}
