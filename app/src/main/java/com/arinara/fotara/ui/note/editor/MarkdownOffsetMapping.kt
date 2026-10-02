// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.note.editor

import androidx.compose.ui.text.input.OffsetMapping

/**
 * Represents a mapped region between original markdown text and transformed display text.
 */
data class TextMappingChunk(
    val oStart: Int,
    val oEnd: Int,
    val tStart: Int,
    val tEnd: Int
)

/**
 * Robust, bidirectional, monotonically non-decreasing [OffsetMapping] supporting:
 * 1. 1:1 identity passages (e.g. active editing lines)
 * 2. Hidden syntax markers (tLen = 0)
 * 3. Replaced syntax sprites (e.g. "- [ ] " -> "☐ ")
 *
 * Guarantees monotonic ordering and strict bounds checking to prevent caret jumping or crashes.
 */
class MarkdownOffsetMapping(
    val originalLength: Int,
    val transformedLength: Int,
    val chunks: List<TextMappingChunk>
) : OffsetMapping {

    constructor(
        originalLength: Int,
        transformedLength: Int,
        hiddenRanges: List<IntRange>,
        legacy: Boolean = true
    ) : this(
        originalLength = originalLength,
        transformedLength = transformedLength,
        chunks = buildChunksFromHidden(originalLength, hiddenRanges)
    )

    override fun originalToTransformed(offset: Int): Int {
        val clamped = offset.coerceIn(0, originalLength)
        if (chunks.isEmpty()) return 0

        val first = chunks.first()
        if (clamped <= first.oStart) return first.tStart

        val last = chunks.last()
        if (clamped >= last.oEnd) return last.tEnd.coerceIn(0, transformedLength)

        for (chunk in chunks) {
            if (clamped in chunk.oStart..chunk.oEnd) {
                val oLen = chunk.oEnd - chunk.oStart
                val tLen = chunk.tEnd - chunk.tStart
                return if (oLen <= 0) {
                    chunk.tStart.coerceIn(0, transformedLength)
                } else {
                    val progress = (clamped - chunk.oStart).toLong() * tLen / oLen
                    (chunk.tStart + progress).toInt().coerceIn(chunk.tStart, chunk.tEnd).coerceIn(0, transformedLength)
                }
            }
        }

        return transformedLength
    }

    override fun transformedToOriginal(offset: Int): Int {
        val clamped = offset.coerceIn(0, transformedLength)
        if (chunks.isEmpty()) return 0

        // Prefer visible chunk (tLen > 0) containing clamped offset
        val visibleChunk = chunks.firstOrNull { it.tEnd > it.tStart && clamped in it.tStart..it.tEnd }
        if (visibleChunk != null) {
            val oLen = visibleChunk.oEnd - visibleChunk.oStart
            val tLen = visibleChunk.tEnd - visibleChunk.tStart
            val progress = (clamped - visibleChunk.tStart).toLong() * oLen / tLen
            return (visibleChunk.oStart + progress).toInt().coerceIn(visibleChunk.oStart, visibleChunk.oEnd).coerceIn(0, originalLength)
        }

        // Fallback for chunks with tLen == 0 (e.g. pure hidden text)
        for (chunk in chunks) {
            if (clamped in chunk.tStart..chunk.tEnd) {
                return chunk.oStart.coerceIn(0, originalLength)
            }
        }

        return originalLength
    }

    companion object {
        fun buildChunksFromHidden(
            originalLength: Int,
            hiddenRanges: List<IntRange>
        ): List<TextMappingChunk> {
            val sortedHidden = hiddenRanges
                .filter { it.first <= it.last && it.first < originalLength && it.last >= 0 }
                .map { it.first.coerceIn(0, originalLength)..it.last.coerceIn(0, originalLength - 1) }
                .sortedBy { it.first }

            val merged = mutableListOf<IntRange>()
            for (r in sortedHidden) {
                if (merged.isEmpty()) {
                    merged.add(r)
                } else {
                    val prev = merged.last()
                    if (r.first <= prev.last + 1) {
                        merged[merged.lastIndex] = prev.first..maxOf(prev.last, r.last)
                    } else {
                        merged.add(r)
                    }
                }
            }

            val result = mutableListOf<TextMappingChunk>()
            var curO = 0
            var curT = 0

            for (hidden in merged) {
                // Visible passage before hidden
                if (hidden.first > curO) {
                    val len = hidden.first - curO
                    result.add(TextMappingChunk(curO, hidden.first, curT, curT + len))
                    curT += len
                }
                // Hidden passage
                val hiddenLen = hidden.last - hidden.first + 1
                result.add(TextMappingChunk(hidden.first, hidden.first + hiddenLen, curT, curT))
                curO = hidden.first + hiddenLen
            }

            // Remaining visible passage
            if (curO < originalLength) {
                val len = originalLength - curO
                result.add(TextMappingChunk(curO, originalLength, curT, curT + len))
                curT += len
            }

            return result
        }
    }
}
