// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.vfx

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object VfxEngine {

    /**
     * Builds an Android ColorMatrix incorporating Brightness, Contrast, Saturation, Temperature,
     * and independent RGB balance adjustments.
     */
    fun buildColorMatrix(params: VfxParameters): ColorMatrix {
        val composite = ColorMatrix()

        // 1. Contrast
        val c = params.contrast
        val t = (1.0f - c) / 2.0f * 255.0f
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                c, 0f, 0f, 0f, t,
                0f, c, 0f, 0f, t,
                0f, 0f, c, 0f, t,
                0f, 0f, 0f, 1f, 0f
            )
        )
        composite.postConcat(contrastMatrix)

        // 2. Brightness
        val bShift = params.brightness * 255.0f
        val brightnessMatrix = ColorMatrix(
            floatArrayOf(
                1f, 0f, 0f, 0f, bShift,
                0f, 1f, 0f, 0f, bShift,
                0f, 0f, 1f, 0f, bShift,
                0f, 0f, 0f, 1f, 0f
            )
        )
        composite.postConcat(brightnessMatrix)

        // 3. Saturation
        val satMatrix = ColorMatrix().apply { setSaturation(params.saturation.coerceAtLeast(0f)) }
        composite.postConcat(satMatrix)

        // 4. Color Temperature (Cool/Blue to Warm/Amber) & Independent RGB balance
        val tempR = if (params.temperature > 0) 1.0f + params.temperature * 0.4f else 1.0f
        val tempB = if (params.temperature < 0) 1.0f + (-params.temperature) * 0.4f else 1.0f
        val effectiveR = params.redBalance * tempR
        val effectiveG = params.greenBalance
        val effectiveB = params.blueBalance * tempB

        val balanceMatrix = ColorMatrix(
            floatArrayOf(
                effectiveR, 0f, 0f, 0f, 0f,
                0f, effectiveG, 0f, 0f, 0f,
                0f, 0f, effectiveB, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        composite.postConcat(balanceMatrix)

        return composite
    }

    /**
     * Builds a ColorMatrixColorFilter suitable for Paint application.
     */
    fun buildColorFilter(params: VfxParameters): ColorMatrixColorFilter {
        return ColorMatrixColorFilter(buildColorMatrix(params))
    }

    /**
     * Performs Chroma Key background knockout on a bitmap.
     * Computes color distance in RGB space against the key color, transitioning to transparent alpha
     * with adjustable tolerance and edge softening.
     */
    fun applyChromaKey(srcBitmap: Bitmap, params: VfxParameters): Bitmap {
        if (!params.chromaKeyEnabled) return srcBitmap

        val width = srcBitmap.width
        val height = srcBitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val pixels = IntArray(width * height)
        srcBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val keyR = ((params.keyColor shr 16) and 0xFF).toFloat()
        val keyG = ((params.keyColor shr 8) and 0xFF).toFloat()
        val keyB = (params.keyColor and 0xFF).toFloat()

        val maxDist = sqrt(255f * 255f * 3f)
        val toleranceDist = params.tolerance * maxDist
        val softeningDist = max(1f, params.edgeSoftening * maxDist)

        for (i in pixels.indices) {
            val color = pixels[i]
            val a = (color ushr 24) and 0xFF
            if (a == 0) continue

            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF

            val dR = r - keyR
            val dG = g - keyG
            val dB = b - keyB
            val dist = sqrt(dR * dR + dG * dG + dB * dB)

            if (dist <= toleranceDist) {
                // Completely within tolerance: knock out to transparent
                pixels[i] = 0x00000000
            } else if (dist < toleranceDist + softeningDist) {
                // Feathered edge region: apply smooth alpha gradient
                val factor = (dist - toleranceDist) / softeningDist
                val newAlpha = (a * factor).toInt().coerceIn(0, 255)
                pixels[i] = (newAlpha shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }
}
