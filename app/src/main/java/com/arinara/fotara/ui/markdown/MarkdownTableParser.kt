// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.markdown

enum class TableColumnAlignment {
    START,
    CENTER,
    END
}

data class MarkdownTable(
    val headers: List<String>,
    val alignments: List<TableColumnAlignment>,
    val rows: List<List<String>>
)

sealed interface ReleaseNotesBlock {
    data class Header(val level: Int, val text: String) : ReleaseNotesBlock
    data class Paragraph(val text: String) : ReleaseNotesBlock
    data class BulletItem(val text: String) : ReleaseNotesBlock
    data class NumberedItem(val number: String, val text: String) : ReleaseNotesBlock
    data class ChecklistItem(val isChecked: Boolean, val text: String, val indentLevel: Int) : ReleaseNotesBlock
    data class Blockquote(val text: String) : ReleaseNotesBlock
    data class CodeBlock(val code: String) : ReleaseNotesBlock
    object Divider : ReleaseNotesBlock
    data class Table(val table: MarkdownTable) : ReleaseNotesBlock
    data class Image(
        val alt: String,
        val url: String,
        val linkUrl: String? = null,
        val isAllowed: Boolean = true
    ) : ReleaseNotesBlock
}

object ReleaseNotesImageValidator {
    fun isAllowedImageUrl(url: String): Boolean {
        val trimmed = url.trim()
        if (!trimmed.startsWith("https://", ignoreCase = true)) return false
        val host = try {
            java.net.URI(trimmed).host?.lowercase() ?: return false
        } catch (_: Exception) {
            return false
        }
        return host == "github.com" ||
                host.endsWith(".github.com") ||
                host == "githubusercontent.com" ||
                host.endsWith(".githubusercontent.com") ||
                host == "arinara.network" ||
                host.endsWith(".arinara.network") ||
                host == "arinaranetwork.github.io"
    }
}

object MarkdownTableParser {

    private val SEPARATOR_CELL_REGEX = Regex("""^:?-+:?$""")

    /**
     * Splits a row line by unescaped pipe characters '|', respecting escaped pipes '\|'.
     * Trims leading/trailing whitespace and optional leading/trailing outer pipes.
     */
    fun splitRow(line: String): List<String> {
        val trimmed = line.trim()
        val stripped = if (trimmed.startsWith("|") && trimmed.endsWith("|") && trimmed.length >= 2) {
            trimmed.substring(1, trimmed.length - 1)
        } else if (trimmed.startsWith("|")) {
            trimmed.substring(1)
        } else if (trimmed.endsWith("|")) {
            trimmed.substring(0, trimmed.length - 1)
        } else {
            trimmed
        }

        val cells = mutableListOf<String>()
        val current = StringBuilder()
        var i = 0
        while (i < stripped.length) {
            val ch = stripped[i]
            if (ch == '\\' && i + 1 < stripped.length && stripped[i + 1] == '|') {
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
     * Determines if a row qualifies as a valid GitHub-style table separator row.
     */
    fun parseSeparatorAlignments(line: String): List<TableColumnAlignment>? {
        if (!line.contains('-')) return null
        val cells = splitRow(line)
        if (cells.isEmpty()) return null
        val alignments = mutableListOf<TableColumnAlignment>()
        for (rawCell in cells) {
            val cell = rawCell.trim()
            if (!SEPARATOR_CELL_REGEX.matches(cell)) return null
            val align = when {
                cell.startsWith(":") && cell.endsWith(":") -> TableColumnAlignment.CENTER
                cell.endsWith(":") -> TableColumnAlignment.END
                cell.startsWith(":") -> TableColumnAlignment.START
                else -> TableColumnAlignment.START
            }
            alignments.add(align)
        }
        return alignments
    }

    /**
     * Parses release notes markdown text into structured blocks, identifying GitHub tables.
     */
    fun parseReleaseNotes(markdown: String): List<ReleaseNotesBlock> {
        val lines = markdown.lines()
        val blocks = mutableListOf<ReleaseNotesBlock>()
        var inCodeBlock = false
        val codeBuffer = StringBuilder()

        var i = 0
        var prevLineWasBlankOrStart = true

        while (i < lines.size) {
            val rawLine = lines[i]
            val trimmed = rawLine.trim()

            // Fenced code block (```)
            if (trimmed.startsWith("```")) {
                if (inCodeBlock) {
                    blocks.add(ReleaseNotesBlock.CodeBlock(codeBuffer.toString().trimEnd()))
                    codeBuffer.clear()
                    inCodeBlock = false
                } else {
                    inCodeBlock = true
                }
                prevLineWasBlankOrStart = false
                i++
                continue
            }

            if (inCodeBlock) {
                codeBuffer.append(rawLine).append("\n")
                i++
                continue
            }

            if (trimmed.isEmpty()) {
                prevLineWasBlankOrStart = true
                i++
                continue
            }

            // Linked Markdown Image: [![alt](image_url)](link_url)
            val linkedImgMatch = Regex("""^\[!\[(.*?)\]\((.*?)\)\]\((.*?)\)$""").find(trimmed)
            if (linkedImgMatch != null) {
                val alt = linkedImgMatch.groupValues[1]
                val imgUrl = linkedImgMatch.groupValues[2]
                val linkUrl = linkedImgMatch.groupValues[3]
                val isAllowed = ReleaseNotesImageValidator.isAllowedImageUrl(imgUrl)
                blocks.add(ReleaseNotesBlock.Image(alt, imgUrl, linkUrl, isAllowed))
                prevLineWasBlankOrStart = false
                i++
                continue
            }

            // Markdown Image: ![alt](image_url)
            val mdImgMatch = Regex("""^!\[(.*?)\]\((.*?)\)$""").find(trimmed)
            if (mdImgMatch != null) {
                val alt = mdImgMatch.groupValues[1]
                val imgUrl = mdImgMatch.groupValues[2]
                val isAllowed = ReleaseNotesImageValidator.isAllowedImageUrl(imgUrl)
                blocks.add(ReleaseNotesBlock.Image(alt, imgUrl, null, isAllowed))
                prevLineWasBlankOrStart = false
                i++
                continue
            }

            // HTML Image tag: <img ... src="..." ... />
            val htmlImgMatch = Regex("""<img\s+[^>]*src=["'](.*?)["'][^>]*>""", RegexOption.IGNORE_CASE).find(trimmed)
            if (htmlImgMatch != null) {
                val imgUrl = htmlImgMatch.groupValues[1]
                val altMatch = Regex("""alt=["'](.*?)["']""", RegexOption.IGNORE_CASE).find(trimmed)
                val alt = altMatch?.groupValues?.get(1) ?: ""
                val isAllowed = ReleaseNotesImageValidator.isAllowedImageUrl(imgUrl)
                blocks.add(ReleaseNotesBlock.Image(alt, imgUrl, null, isAllowed))
                prevLineWasBlankOrStart = false
                i++
                continue
            }

            // Headers
            if (trimmed.startsWith("# ") || trimmed.startsWith("## ") || trimmed.startsWith("### ")) {
                val level = if (trimmed.startsWith("# ")) 1 else if (trimmed.startsWith("## ")) 2 else 3
                val text = trimmed.removePrefix("#").removePrefix("#").removePrefix("#").trim()
                blocks.add(ReleaseNotesBlock.Header(level, text))
                prevLineWasBlankOrStart = false
                i++
                continue
            }

            // Divider
            if (trimmed == "---" || trimmed == "***" || trimmed == "___") {
                blocks.add(ReleaseNotesBlock.Divider)
                prevLineWasBlankOrStart = false
                i++
                continue
            }

            // Blockquote
            if (trimmed.startsWith(">")) {
                blocks.add(ReleaseNotesBlock.Blockquote(trimmed.removePrefix(">").trim()))
                prevLineWasBlankOrStart = false
                i++
                continue
            }

            // Checklist
            val chkMatch = Regex("""^(\s*[-*+]\s*)\[([ xX])\]\s*(.*)$""").find(rawLine)
            if (chkMatch != null) {
                val isChecked = chkMatch.groupValues[2].equals("x", ignoreCase = true)
                val itemContent = chkMatch.groupValues[3]
                val leadingSpaces = rawLine.takeWhile { it == ' ' }.length
                val indentLevel = (leadingSpaces / 2).coerceIn(0, 3)
                blocks.add(ReleaseNotesBlock.ChecklistItem(isChecked, itemContent, indentLevel))
                prevLineWasBlankOrStart = false
                i++
                continue
            }

            // Bullet list
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                blocks.add(ReleaseNotesBlock.BulletItem(trimmed.substring(2).trim()))
                prevLineWasBlankOrStart = false
                i++
                continue
            }

            // Numbered list
            val numMatch = Regex("""^(\d+)\.\s+(.*)""").find(trimmed)
            if (numMatch != null) {
                blocks.add(ReleaseNotesBlock.NumberedItem(numMatch.groupValues[1], numMatch.groupValues[2]))
                prevLineWasBlankOrStart = false
                i++
                continue
            }

            // Table candidate: must have pipe '|' and be preceded by blank line or start of text
            val hasPipe = rawLine.contains('|')
            if (hasPipe && prevLineWasBlankOrStart && i + 1 < lines.size) {
                val nextTrimmed = lines[i + 1].trim()
                val alignments = parseSeparatorAlignments(nextTrimmed)
                if (alignments != null) {
                    // Valid table start!
                    val headerCells = splitRow(trimmed)
                    val colCount = headerCells.size

                    // Adjust alignments size to match colCount
                    val finalAlignments = if (alignments.size < colCount) {
                        alignments + List(colCount - alignments.size) { TableColumnAlignment.START }
                    } else {
                        alignments.take(colCount)
                    }

                    val bodyRows = mutableListOf<List<String>>()
                    var rowIdx = i + 2
                    while (rowIdx < lines.size) {
                        val rowLine = lines[rowIdx]
                        val rowTrimmed = rowLine.trim()
                        if (rowTrimmed.isEmpty() || !rowTrimmed.contains('|')) {
                            break
                        }
                        val rawCells = splitRow(rowTrimmed)
                        val paddedCells = if (rawCells.size < colCount) {
                            rawCells + List(colCount - rawCells.size) { "" }
                        } else {
                            rawCells.take(colCount)
                        }
                        bodyRows.add(paddedCells)
                        rowIdx++
                    }

                    blocks.add(
                        ReleaseNotesBlock.Table(
                            MarkdownTable(
                                headers = headerCells,
                                alignments = finalAlignments,
                                rows = bodyRows
                            )
                        )
                    )
                    i = rowIdx
                    prevLineWasBlankOrStart = false
                    continue
                }
            }

            // Default regular paragraph
            blocks.add(ReleaseNotesBlock.Paragraph(trimmed))
            prevLineWasBlankOrStart = false
            i++
        }

        if (inCodeBlock && codeBuffer.isNotEmpty()) {
            blocks.add(ReleaseNotesBlock.CodeBlock(codeBuffer.toString().trimEnd()))
        }

        return blocks
    }
}
