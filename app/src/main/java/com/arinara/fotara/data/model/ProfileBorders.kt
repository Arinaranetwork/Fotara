// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.model

import androidx.annotation.DrawableRes
import com.arinara.fotara.R

data class ProfileBorder(
    val id: String,
    val displayName: String,
    @get:DrawableRes val drawableRes: Int,
    val innerCenterX: Float,
    val innerCenterY: Float,
    val innerDiameterRatio: Float,
    val ringOuterDiameterRatio: Float,
    val extentLeft: Float,
    val extentTop: Float,
    val extentRight: Float,
    val extentBottom: Float,
    val canvasWidth: Int = 1254,
    val canvasHeight: Int = 1254
) {
    // Backward compatibility getters
    val innerRatio: Float get() = innerDiameterRatio
    val centerOffsetX: Float get() = innerCenterX - 0.5f
    val centerOffsetY: Float get() = innerCenterY - 0.5f
}

object ProfileBorders {
    const val NONE_ID = "none"

    val NONE = ProfileBorder(
        id = NONE_ID,
        displayName = "None",
        drawableRes = 0,
        innerCenterX = 0.5f,
        innerCenterY = 0.5f,
        innerDiameterRatio = 1.0f,
        ringOuterDiameterRatio = 1.0f,
        extentLeft = 0.0f,
        extentTop = 0.0f,
        extentRight = 1.0f,
        extentBottom = 1.0f,
        canvasWidth = 1254,
        canvasHeight = 1254
    )

    val ALL_BORDERS: List<ProfileBorder> = listOf(
        NONE,
        ProfileBorder(
            id = "file_000000000278820bb0a8901e8fa6612b",
            displayName = "Neon Spark",
            drawableRes = R.drawable.file_000000000278820bb0a8901e8fa6612b,
            innerCenterX = 0.4565f,
            innerCenterY = 0.4908f,
            innerDiameterRatio = 0.7209f,
            ringOuterDiameterRatio = 0.8517f,
            extentLeft = 0.0191f,
            extentTop = 0.0463f,
            extentRight = 0.9992f,
            extentBottom = 0.9179f,
            canvasWidth = 1254,
            canvasHeight = 1254
        ),
        ProfileBorder(
            id = "file_000000001c6c820bbc0efdac44c0aa63",
            displayName = "Crystal Arc",
            drawableRes = R.drawable.file_000000001c6c820bbc0efdac44c0aa63,
            innerCenterX = 0.5036f,
            innerCenterY = 0.5347f,
            innerDiameterRatio = 0.7026f,
            ringOuterDiameterRatio = 0.8182f,
            extentLeft = 0.0279f,
            extentTop = 0.0303f,
            extentRight = 0.9769f,
            extentBottom = 0.9689f,
            canvasWidth = 1254,
            canvasHeight = 1254
        ),
        ProfileBorder(
            id = "file_000000007fdc81f7b74b9a79e2c63215",
            displayName = "Golden Wings",
            drawableRes = R.drawable.file_000000007fdc81f7b74b9a79e2c63215,
            innerCenterX = 0.4622f,
            innerCenterY = 0.4912f,
            innerDiameterRatio = 0.6511f,
            ringOuterDiameterRatio = 0.7878f,
            extentLeft = 0.0344f,
            extentTop = 0.0308f,
            extentRight = 0.9916f,
            extentBottom = 0.9700f,
            canvasWidth = 1310,
            canvasHeight = 1200
        )
    )

    fun getById(id: String?): ProfileBorder {
        if (id.isNullOrBlank()) return NONE
        return ALL_BORDERS.firstOrNull { it.id == id } ?: NONE
    }
}
