// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.note.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownOffsetMappingTest {

    @Test
    fun testEmptyText() {
        val mapping = MarkdownOffsetMapping(0, 0, emptyList())
        assertEquals(0, mapping.originalToTransformed(0))
        assertEquals(0, mapping.transformedToOriginal(0))
    }

    @Test
    fun testNoHiddenRanges_Identity() {
        val text = "Hello world"
        val mapping = MarkdownOffsetMapping(text.length, text.length, emptyList())
        for (i in 0..text.length) {
            assertEquals(i, mapping.originalToTransformed(i))
            assertEquals(i, mapping.transformedToOriginal(i))
        }
    }

    @Test
    fun testSingleHiddenConstruct_Bold() {
        // Raw: "**bold**", len = 8
        // Hidden: [0..1] and [6..7]
        // Transformed: "bold", len = 4
        val raw = "**bold**"
        val hidden = listOf(0..1, 6..7)
        val mapping = MarkdownOffsetMapping(raw.length, 4, hidden)

        // Monotonic check
        var prevT = -1
        for (i in 0..raw.length) {
            val t = mapping.originalToTransformed(i)
            assertTrue("originalToTransformed must be monotonic: i=$i, t=$t, prevT=$prevT", t >= prevT)
            assertTrue("t must be within [0, 4]", t in 0..4)
            prevT = t
        }

        var prevO = -1
        for (j in 0..4) {
            val o = mapping.transformedToOriginal(j)
            assertTrue("transformedToOriginal must be monotonic: j=$j, o=$o, prevO=$prevO", o >= prevO)
            assertTrue("o must be within [0, 8]", o in 0..8)
            prevO = o
        }

        // Check content mapping: 'b' at raw index 2 maps to transformed index 0
        assertEquals(0, mapping.originalToTransformed(2))
        assertEquals(2, mapping.transformedToOriginal(0))

        // 'd' at raw index 5 maps to transformed index 3
        assertEquals(3, mapping.originalToTransformed(5))
        assertEquals(5, mapping.transformedToOriginal(3))
    }

    @Test
    fun testMultiByteAndInternationalText() {
        // Raw: "### 学习 **日本語**"
        val raw = "### 学习 **日本語**"
        val hidden = listOf(0..3, 8..9, 13..14)
        val transformedLen = raw.length - (4 + 2 + 2)
        val mapping = MarkdownOffsetMapping(raw.length, transformedLen, hidden)

        // Verify monotonicity and strict bounds
        var prevT = -1
        for (i in 0..raw.length) {
            val t = mapping.originalToTransformed(i)
            assertTrue(t >= prevT)
            assertTrue(t in 0..transformedLen)
            prevT = t
        }

        var prevO = -1
        for (j in 0..transformedLen) {
            val o = mapping.transformedToOriginal(j)
            assertTrue(o >= prevO)
            assertTrue(o in 0..raw.length)
            prevO = o
        }
    }
}
