// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.vfx

data class VfxParameters(
    // Chroma Key
    val chromaKeyEnabled: Boolean = false,
    val keyColor: Long = 0xFF00FF00, // ARGB green default
    val tolerance: Float = 0.35f, // 0.0 .. 1.0
    val edgeSoftening: Float = 0.15f, // 0.0 .. 1.0

    // Color & Channel Grading
    val brightness: Float = 0.0f, // -1.0 .. 1.0
    val contrast: Float = 1.0f, // 0.0 .. 2.0
    val saturation: Float = 1.0f, // 0.0 .. 2.0
    val temperature: Float = 0.0f, // -1.0 .. 1.0 (Cool to Warm)
    val redBalance: Float = 1.0f, // 0.0 .. 2.0
    val greenBalance: Float = 1.0f, // 0.0 .. 2.0
    val blueBalance: Float = 1.0f // 0.0 .. 2.0
) {
    val isDefault: Boolean
        get() = !chromaKeyEnabled &&
                brightness == 0.0f &&
                contrast == 1.0f &&
                saturation == 1.0f &&
                temperature == 0.0f &&
                redBalance == 1.0f &&
                greenBalance == 1.0f &&
                blueBalance == 1.0f
}
