// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.academic.dewarp

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 2D point representation in coordinate space.
 */
data class QuadPoint(val x: Float, val y: Float)

/**
 * Pure 3x3 projective transformation (homography) matrix for document dewarping.
 * Elements stored in row-major order:
 * [ m00, m01, m02 ]
 * [ m10, m11, m12 ]
 * [ m20, m21, m22 ]
 */
class PerspectiveDewarpMatrix(
    val values: FloatArray
) {
    init {
        require(values.size == 9) { "PerspectiveDewarpMatrix requires exactly 9 elements." }
    }

    /**
     * Maps a source point (x, y) through the projective transformation into destination space.
     */
    fun mapPoint(x: Float, y: Float): QuadPoint {
        val w = values[6] * x + values[7] * y + values[8]
        val normalizer = if (abs(w) > 1e-7f) 1f / w else 1f
        val outX = (values[0] * x + values[1] * y + values[2]) * normalizer
        val outY = (values[3] * x + values[4] * y + values[5]) * normalizer
        return QuadPoint(outX, outY)
    }

    /**
     * Maps an input QuadPoint.
     */
    fun mapPoint(point: QuadPoint): QuadPoint = mapPoint(point.x, point.y)

    /**
     * Inverts the 3x3 projective transformation matrix.
     * Returns null if matrix is singular (determinant near zero).
     */
    fun invert(): PerspectiveDewarpMatrix? {
        val m00 = values[0]; val m01 = values[1]; val m02 = values[2]
        val m10 = values[3]; val m11 = values[4]; val m12 = values[5]
        val m20 = values[6]; val m21 = values[7]; val m22 = values[8]

        val det = m00 * (m11 * m22 - m12 * m21) -
                m01 * (m10 * m22 - m12 * m20) +
                m02 * (m10 * m21 - m11 * m20)

        if (abs(det) < 1e-9f) return null

        val invDet = 1.0f / det
        val inv = FloatArray(9)

        inv[0] = (m11 * m22 - m12 * m21) * invDet
        inv[1] = (m02 * m21 - m01 * m22) * invDet
        inv[2] = (m01 * m12 - m02 * m11) * invDet

        inv[3] = (m12 * m20 - m10 * m22) * invDet
        inv[4] = (m00 * m22 - m02 * m20) * invDet
        inv[5] = (m02 * m10 - m00 * m12) * invDet

        inv[6] = (m10 * m21 - m11 * m20) * invDet
        inv[7] = (m01 * m20 - m00 * m21) * invDet
        inv[8] = (m00 * m11 - m01 * m10) * invDet

        return PerspectiveDewarpMatrix(inv)
    }

    companion object {
        /**
         * Creates an identity 3x3 homography matrix.
         */
        fun identity(): PerspectiveDewarpMatrix {
            return PerspectiveDewarpMatrix(
                floatArrayOf(
                    1f, 0f, 0f,
                    0f, 1f, 0f,
                    0f, 0f, 1f
                )
            )
        }

        /**
         * Computes the 3x3 projective transformation matrix mapping 4 quadrilateral source corners
         * into 4 destination corners using Gaussian elimination with partial pivoting.
         *
         * @param src 4 source points (top-left, top-right, bottom-right, bottom-left).
         * @param dst 4 destination points.
         */
        fun computeHomography(
            src: List<QuadPoint>,
            dst: List<QuadPoint>
        ): PerspectiveDewarpMatrix {
            require(src.size == 4 && dst.size == 4) {
                "Homography computation requires exactly 4 source and 4 destination points."
            }

            // 8 linear equations for 8 unknowns [h00, h01, h02, h10, h11, h12, h20, h21] with h22 = 1.0
            val a = Array(8) { DoubleArray(8) }
            val b = DoubleArray(8)

            for (i in 0..3) {
                val sx = src[i].x.toDouble()
                val sy = src[i].y.toDouble()
                val dx = dst[i].x.toDouble()
                val dy = dst[i].y.toDouble()

                val row1 = i * 2
                a[row1][0] = sx
                a[row1][1] = sy
                a[row1][2] = 1.0
                a[row1][3] = 0.0
                a[row1][4] = 0.0
                a[row1][5] = 0.0
                a[row1][6] = -dx * sx
                a[row1][7] = -dx * sy
                b[row1] = dx

                val row2 = i * 2 + 1
                a[row2][0] = 0.0
                a[row2][1] = 0.0
                a[row2][2] = 0.0
                a[row2][3] = sx
                a[row2][4] = sy
                a[row2][5] = 1.0
                a[row2][6] = -dy * sx
                a[row2][7] = -dy * sy
                b[row2] = dy
            }

            val h = solveGaussianElimination(a, b) ?: return identity()

            return PerspectiveDewarpMatrix(
                floatArrayOf(
                    h[0].toFloat(), h[1].toFloat(), h[2].toFloat(),
                    h[3].toFloat(), h[4].toFloat(), h[5].toFloat(),
                    h[6].toFloat(), h[7].toFloat(), 1.0f
                )
            )
        }

        /**
         * Convenience factory computing homography from 4 camera quadrilateral corners
         * to an orthogonal destination rectangle of dimension targetWidth x targetHeight.
         */
        fun computeRectangularDewarp(
            srcCorners: List<QuadPoint>,
            targetWidth: Float,
            targetHeight: Float
        ): PerspectiveDewarpMatrix {
            val dstCorners = listOf(
                QuadPoint(0f, 0f),
                QuadPoint(targetWidth, 0f),
                QuadPoint(targetWidth, targetHeight),
                QuadPoint(0f, targetHeight)
            )
            return computeHomography(srcCorners, dstCorners)
        }

        private fun solveGaussianElimination(a: Array<DoubleArray>, b: DoubleArray): DoubleArray? {
            val n = 8
            val augmented = Array(n) { i -> DoubleArray(n + 1) { j -> if (j < n) a[i][j] else b[i] } }

            for (p in 0 until n) {
                // Find pivot
                var maxRow = p
                var maxVal = abs(augmented[p][p])
                for (i in (p + 1) until n) {
                    val currentVal = abs(augmented[i][p])
                    if (currentVal > maxVal) {
                        maxVal = currentVal
                        maxRow = i
                    }
                }

                if (maxVal < 1e-12) return null // Singular or degenerate system

                // Swap rows
                val temp = augmented[p]
                augmented[p] = augmented[maxRow]
                augmented[maxRow] = temp

                // Pivot normalization and elimination
                for (i in (p + 1) until n) {
                    val factor = augmented[i][p] / augmented[p][p]
                    for (j in p..n) {
                        augmented[i][j] -= factor * augmented[p][j]
                    }
                }
            }

            // Back substitution
            val x = DoubleArray(n)
            for (i in (n - 1) downTo 0) {
                var sum = 0.0
                for (j in (i + 1) until n) {
                    sum += augmented[i][j] * x[j]
                }
                x[i] = (augmented[i][n] - sum) / augmented[i][i]
            }

            return x
        }
    }
}

/**
 * Pure memory representation of an ARGB integer pixel image buffer.
 */
data class PixelBuffer(
    val width: Int,
    val height: Int,
    val pixels: IntArray
) {
    init {
        require(pixels.size == width * height) {
            "Pixel array size (${pixels.size}) does not match width * height ($width * $height = ${width * height})."
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PixelBuffer
        return width == other.width && height == other.height && pixels.contentEquals(other.pixels)
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + pixels.contentHashCode()
        return result
    }
}

/**
 * Pure Kotlin contrast enhancement and shadow-suppression optical filter for whiteboard notes.
 * Clarifies classroom and lecture captures into clean, readable study documents.
 */
class WhiteboardContrastFilter {

    /**
     * Filters a pixel buffer to whiten background shadows while boosting pen stroke contrast.
     *
     * @param buffer Input ARGB pixel buffer.
     * @param contrastMultiplier Contrast amplification factor (default: 1.6x).
     * @param shadowReductionThreshold Cutoff luminance [0..255] above which shadows are bleached white.
     */
    fun process(
        buffer: PixelBuffer,
        contrastMultiplier: Float = 1.6f,
        shadowReductionThreshold: Int = 180
    ): PixelBuffer {
        val outputPixels = processPixels(
            pixels = buffer.pixels,
            width = buffer.width,
            height = buffer.height,
            contrastMultiplier = contrastMultiplier,
            shadowReductionThreshold = shadowReductionThreshold
        )
        return PixelBuffer(buffer.width, buffer.height, outputPixels)
    }

    /**
     * Pure array-level processing function for maximum execution speed.
     */
    fun processPixels(
        pixels: IntArray,
        width: Int,
        height: Int,
        contrastMultiplier: Float = 1.6f,
        shadowReductionThreshold: Int = 180
    ): IntArray {
        val total = width * height
        val out = IntArray(total)

        // 1. Calculate approximate luminance distribution to detect ambient lighting
        var sumLuminance = 0L
        for (i in 0 until total) {
            val color = pixels[i]
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            val lum = (77 * r + 150 * g + 29 * b) shr 8
            sumLuminance += lum
        }
        val avgLuminance = if (total > 0) (sumLuminance / total).toInt() else 128
        val effectiveThreshold = max(140, min(230, (avgLuminance + shadowReductionThreshold) / 2))

        // 2. High-contrast stroke separation and shadow bleaching
        for (i in 0 until total) {
            val color = pixels[i]
            val a = (color shr 24) and 0xFF
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF

            val luminance = (77 * r + 150 * g + 29 * b) shr 8
            val maxChannel = max(r, max(g, b))
            val minChannel = min(r, min(g, b))
            val saturation = if (maxChannel > 0) ((maxChannel - minChannel) * 255) / maxChannel else 0

            val newR: Int
            val newG: Int
            val newB: Int

            if (luminance >= effectiveThreshold && saturation < 45) {
                // Background whiteboard with ambient shadow -> bleach to clean white
                val bleachFactor = (luminance - effectiveThreshold).toFloat() / (255 - effectiveThreshold).coerceAtLeast(1)
                newR = (r + (255 - r) * (0.85f + 0.15f * bleachFactor)).toInt().coerceIn(0, 255)
                newG = (g + (255 - g) * (0.85f + 0.15f * bleachFactor)).toInt().coerceIn(0, 255)
                newB = (b + (255 - b) * (0.85f + 0.15f * bleachFactor)).toInt().coerceIn(0, 255)
            } else {
                // Marker ink stroke (black or colored marker) -> heighten contrast
                val centeredR = (r - 128) * contrastMultiplier + 128
                val centeredG = (g - 128) * contrastMultiplier + 128
                val centeredB = (b - 128) * contrastMultiplier + 128

                newR = centeredR.toInt().coerceIn(0, 255)
                newG = centeredG.toInt().coerceIn(0, 255)
                newB = centeredB.toInt().coerceIn(0, 255)
            }

            out[i] = (a shl 24) or (newR shl 16) or (newG shl 8) or newB
        }

        return out
    }
}
