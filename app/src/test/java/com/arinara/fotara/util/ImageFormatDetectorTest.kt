// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageFormatDetectorTest {

    @Test
    fun isGif_and_detectFormat_detectsGif87a() {
        val bytes = "GIF87a...".toByteArray(Charsets.US_ASCII)
        assertTrue(ImageFormatDetector.isGif(bytes))
        assertEquals(ImageFormat.GIF, ImageFormatDetector.detectFormat(bytes))
    }

    @Test
    fun isGif_and_detectFormat_detectsGif89a() {
        val bytes = "GIF89a...".toByteArray(Charsets.US_ASCII)
        assertTrue(ImageFormatDetector.isGif(bytes))
        assertEquals(ImageFormat.GIF, ImageFormatDetector.detectFormat(bytes))
    }

    @Test
    fun detectFormat_detectsPng() {
        val bytes = byteArrayOf(
            0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(),
            0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte(),
            0x00, 0x00
        )
        assertFalse(ImageFormatDetector.isGif(bytes))
        assertEquals(ImageFormat.PNG, ImageFormatDetector.detectFormat(bytes))
    }

    @Test
    fun detectFormat_detectsJpeg() {
        val bytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0x00)
        assertFalse(ImageFormatDetector.isGif(bytes))
        assertEquals(ImageFormat.JPEG, ImageFormatDetector.detectFormat(bytes))
    }

    @Test
    fun detectFormat_detectsWebp() {
        val bytes = byteArrayOf(
            'R'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 'F'.code.toByte(),
            0x00, 0x00, 0x00, 0x00,
            'W'.code.toByte(), 'E'.code.toByte(), 'B'.code.toByte(), 'P'.code.toByte()
        )
        assertFalse(ImageFormatDetector.isGif(bytes))
        assertEquals(ImageFormat.WEBP, ImageFormatDetector.detectFormat(bytes))
    }

    @Test
    fun detectFormat_handlesTruncatedAndEmpty() {
        assertEquals(ImageFormat.UNKNOWN, ImageFormatDetector.detectFormat(ByteArray(0)))
        assertEquals(ImageFormat.UNKNOWN, ImageFormatDetector.detectFormat(byteArrayOf(0x47, 0x49)))
        assertFalse(ImageFormatDetector.isGif(ByteArray(0)))
        assertFalse(ImageFormatDetector.isGif(byteArrayOf(0x47, 0x49, 0x46)))
    }

    @Test
    fun detectFormat_handlesUnknownData() {
        val randomBytes = byteArrayOf(0x12, 0x34, 0x56, 0x78, 0x9A.toByte(), 0xBC.toByte())
        assertFalse(ImageFormatDetector.isGif(randomBytes))
        assertEquals(ImageFormat.UNKNOWN, ImageFormatDetector.detectFormat(randomBytes))
    }
}
