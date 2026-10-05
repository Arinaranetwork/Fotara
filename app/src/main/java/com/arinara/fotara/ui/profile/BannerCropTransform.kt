// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.profile

import android.graphics.Matrix
import kotlin.math.max

data class NormalizedCropRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    fun toSerializedString(): String = "$left,$top,$right,$bottom"

    companion object {
        val FULL = NormalizedCropRect(0f, 0f, 1f, 1f)

        fun fromSerializedString(raw: String?): NormalizedCropRect? {
            if (raw.isNullOrBlank()) return null
            val parts = raw.split(",")
            if (parts.size != 4) return null
            val l = parts[0].toFloatOrNull() ?: return null
            val t = parts[1].toFloatOrNull() ?: return null
            val r = parts[2].toFloatOrNull() ?: return null
            val b = parts[3].toFloatOrNull() ?: return null
            return NormalizedCropRect(
                left = l.coerceIn(0f, 1f),
                top = t.coerceIn(0f, 1f),
                right = r.coerceIn(0f, 1f),
                bottom = b.coerceIn(0f, 1f)
            )
        }
    }
}

object BannerCropTransform {

    data class TransformParams(
        val scale: Float,
        val tx: Float,
        val ty: Float
    )

    fun computeTransformParams(
        viewWidth: Float,
        viewHeight: Float,
        imageWidth: Float,
        imageHeight: Float,
        crop: NormalizedCropRect
    ): TransformParams {
        val cropW = ((crop.right - crop.left) * imageWidth).coerceAtLeast(1f)
        val cropH = ((crop.bottom - crop.top) * imageHeight).coerceAtLeast(1f)
        val scale = max(viewWidth / cropW, viewHeight / cropH)

        val cropCenterX = (crop.left + crop.right) * 0.5f * imageWidth
        val cropCenterY = (crop.top + crop.bottom) * 0.5f * imageHeight

        val tx = viewWidth * 0.5f - cropCenterX * scale
        val ty = viewHeight * 0.5f - cropCenterY * scale

        return TransformParams(scale = scale, tx = tx, ty = ty)
    }

    fun applyToMatrix(
        matrix: Matrix,
        params: TransformParams
    ) {
        matrix.reset()
        matrix.postScale(params.scale, params.scale)
        matrix.postTranslate(params.tx, params.ty)
    }
}
