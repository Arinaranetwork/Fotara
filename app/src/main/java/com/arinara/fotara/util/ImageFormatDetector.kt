// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.util

enum class ImageFormat {
    GIF,
    PNG,
    JPEG,
    WEBP,
    UNKNOWN
}

object ImageFormatDetector {
    private val GIF87A = byteArrayOf(0x47, 0x49, 0x46, 0x38, 0x37, 0x61) // "GIF87a"
    private val GIF89A = byteArrayOf(0x47, 0x49, 0x46, 0x38, 0x39, 0x61) // "GIF89a"

    private fun matchesPrefix(bytes: ByteArray, prefix: ByteArray): Boolean {
        if (bytes.size < prefix.size) return false
        for (i in prefix.indices) {
            if (bytes[i] != prefix[i]) return false
        }
        return true
    }

    fun isGif(bytes: ByteArray): Boolean {
        return matchesPrefix(bytes, GIF87A) || matchesPrefix(bytes, GIF89A)
    }

    fun detectFormat(bytes: ByteArray): ImageFormat {
        if (bytes.size < 3) return ImageFormat.UNKNOWN
        if (isGif(bytes)) return ImageFormat.GIF
        if (bytes.size >= 8 &&
            bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte() &&
            bytes[4] == 0x0D.toByte() && bytes[5] == 0x0A.toByte() &&
            bytes[6] == 0x1A.toByte() && bytes[7] == 0x0A.toByte()
        ) {
            return ImageFormat.PNG
        }
        if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()) {
            return ImageFormat.JPEG
        }
        if (bytes.size >= 12 &&
            bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte() &&
            bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() &&
            bytes[10] == 'B'.code.toByte() && bytes[11] == 'P'.code.toByte()
        ) {
            return ImageFormat.WEBP
        }
        return ImageFormat.UNKNOWN
    }
}
