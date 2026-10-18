// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetBitmapUtilsTest {

    @Test
    fun calculateInSampleSize_returns1_whenDimensionsWithinMax() {
        val sampleSize = WidgetBitmapUtils.calculateInSampleSize(
            rawWidth = 400,
            rawHeight = 300,
            maxSize = 512
        )
        assertEquals(1, sampleSize)
    }

    @Test
    fun calculateInSampleSize_returns1_whenExactMaxSize() {
        val sampleSize = WidgetBitmapUtils.calculateInSampleSize(
            rawWidth = 512,
            rawHeight = 512,
            maxSize = 512
        )
        assertEquals(1, sampleSize)
    }

    @Test
    fun calculateInSampleSize_returnsPowerOfTwo_whenDimensionsExceedMax() {
        val sampleSize1024 = WidgetBitmapUtils.calculateInSampleSize(
            rawWidth = 1024,
            rawHeight = 768,
            maxSize = 512
        )
        assertTrue(sampleSize1024 >= 2)

        val sampleSize2048 = WidgetBitmapUtils.calculateInSampleSize(
            rawWidth = 2048,
            rawHeight = 1536,
            maxSize = 512
        )
        assertTrue(sampleSize2048 >= 4)

        val sampleSize4000 = WidgetBitmapUtils.calculateInSampleSize(
            rawWidth = 4000,
            rawHeight = 3000,
            maxSize = 512
        )
        assertTrue(sampleSize4000 >= 8)
    }

    @Test
    fun calculateInSampleSize_guardsAgainstNegativeOrZero() {
        val sampleSizeZero = WidgetBitmapUtils.calculateInSampleSize(
            rawWidth = 0,
            rawHeight = 0,
            maxSize = 512
        )
        assertEquals(1, sampleSizeZero)
    }
}
