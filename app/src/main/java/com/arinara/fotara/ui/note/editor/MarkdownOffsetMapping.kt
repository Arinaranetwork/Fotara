// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.note.editor

import androidx.compose.ui.text.input.OffsetMapping

/**
 * Pure class implementing a robust, bidirectional, monotonically non-decreasing
 * [OffsetMapping] between raw Markdown text (with syntax markers) and rendered text
 * (where selected markers are hidden).
 *
 * Guaranteed invariants:
 * 1. Monotonic: o1 <= o2 => t(o1) <= t(o2)
 * 2. Monotonic: t1 <= t2 => o(t1) <= o(t2)
 * 3. Strict bounds: 0 <= t <= transformedLength, 0 <= o <= originalLength
 * 4. Safe against empty inputs, surrogate pairs, and multi-byte characters.
 */
class MarkdownOffsetMapping(
    val originalLength: Int,
    val transformedLength: Int,
    hiddenRanges: List<IntRange>
) : OffsetMapping {

    data class VisibleSegment(
        val oStart: Int,
        val oEnd: Int,
        val tStart: Int,
        val tEnd: Int
    )

    private val visibleSegments: List<VisibleSegment>

    init {
        // Merge and normalize hidden ranges
        val sortedHidden = hiddenRanges
            .filter { it.first < it.last + 1 && it.first < originalLength && it.last >= 0 }
            .map { it.first.coerceIn(0, originalLength) until (it.last + 1).coerceIn(0, originalLength) }
            .filter { !it.isEmpty() }
            .sortedBy { it.first }

        val mergedHidden = mutableListOf<IntRange>()
        for (range in sortedHidden) {
            if (mergedHidden.isEmpty()) {
                mergedHidden.add(range)
            } else {
                val last = mergedHidden.last()
                if (range.first <= last.last + 1) {
                    mergedHidden[mergedHidden.lastIndex] = last.first..maxOf(last.last, range.last)
                } else {
                    mergedHidden.add(range)
                }
            }
        }

        // Build visible segments
        val segments = mutableListOf<VisibleSegment>()
        var currentO = 0
        var currentT = 0

        for (hidden in mergedHidden) {
            val hiddenStart = hidden.first
            val hiddenEnd = hidden.last + 1

            if (hiddenStart > currentO) {
                val len = hiddenStart - currentO
                segments.add(
                    VisibleSegment(
                        oStart = currentO,
                        oEnd = hiddenStart,
                        tStart = currentT,
                        tEnd = currentT + len
                    )
                )
                currentT += len
            }
            currentO = hiddenEnd
        }

        if (currentO < originalLength) {
            val len = originalLength - currentO
            segments.add(
                VisibleSegment(
                    oStart = currentO,
                    oEnd = originalLength,
                    tStart = currentT,
                    tEnd = currentT + len
                )
            )
            currentT += len
        }

        visibleSegments = segments
    }

    override fun originalToTransformed(offset: Int): Int {
        val clamped = offset.coerceIn(0, originalLength)
        if (visibleSegments.isEmpty()) return 0

        // If offset is before or at the first visible segment
        val first = visibleSegments.first()
        if (clamped <= first.oStart) {
            return first.tStart
        }

        // If offset is after or at the last visible segment
        val last = visibleSegments.last()
        if (clamped >= last.oEnd) {
            return last.tEnd.coerceIn(0, transformedLength)
        }

        // Find which segment or gap the offset belongs to
        for (i in visibleSegments.indices) {
            val seg = visibleSegments[i]
            if (clamped in seg.oStart..seg.oEnd) {
                return (seg.tStart + (clamped - seg.oStart)).coerceIn(0, transformedLength)
            }
            if (i < visibleSegments.size - 1) {
                val nextSeg = visibleSegments[i + 1]
                if (clamped in seg.oEnd until nextSeg.oStart) {
                    // Inside hidden gap: map to end of previous segment (which equals start of next)
                    return seg.tEnd.coerceIn(0, transformedLength)
                }
            }
        }

        return transformedLength
    }

    override fun transformedToOriginal(offset: Int): Int {
        val clamped = offset.coerceIn(0, transformedLength)
        if (visibleSegments.isEmpty()) return 0

        val first = visibleSegments.first()
        if (clamped <= first.tStart) {
            return first.oStart
        }

        val last = visibleSegments.last()
        if (clamped >= last.tEnd) {
            return last.oEnd.coerceIn(0, originalLength)
        }

        for (seg in visibleSegments) {
            if (clamped in seg.tStart..seg.tEnd) {
                return (seg.oStart + (clamped - seg.tStart)).coerceIn(0, originalLength)
            }
        }

        return originalLength
    }
}
