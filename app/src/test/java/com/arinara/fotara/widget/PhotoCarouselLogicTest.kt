// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoCarouselLogicTest {

    @Test
    fun nextIndex_incrementsNormally() {
        val count = 5
        val currentIndex = 2
        val nextIndex = (currentIndex + 1) % count
        assertEquals(3, nextIndex)
    }

    @Test
    fun nextIndex_wrapsAroundAtEnd() {
        val count = 5
        val currentIndex = 4
        val nextIndex = (currentIndex + 1) % count
        assertEquals(0, nextIndex)
    }

    @Test
    fun prevIndex_decrementsNormally() {
        val count = 5
        val currentIndex = 3
        val prevIndex = (currentIndex - 1 + count) % count
        assertEquals(2, prevIndex)
    }

    @Test
    fun prevIndex_wrapsAroundAtBeginning() {
        val count = 5
        val currentIndex = 0
        val prevIndex = (currentIndex - 1 + count) % count
        assertEquals(4, prevIndex)
    }

    @Test
    fun indexClamping_whenItemsShrink() {
        val itemsCount = 3
        val savedIndex = 7
        val clamped = savedIndex.coerceIn(0, itemsCount - 1)
        assertEquals(2, clamped)
    }
}
