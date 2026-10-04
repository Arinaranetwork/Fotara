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
    val innerRatio: Float,
    val centerOffsetX: Float = 0f,
    val centerOffsetY: Float = 0f
)

object ProfileBorders {
    const val NONE_ID = "none"

    val NONE = ProfileBorder(
        id = NONE_ID,
        displayName = "None",
        drawableRes = 0,
        innerRatio = 1.0f,
        centerOffsetX = 0f,
        centerOffsetY = 0f
    )

    val ALL_BORDERS: List<ProfileBorder> = listOf(
        NONE,
        ProfileBorder(
            id = "file_000000000278820bb0a8901e8fa6612b",
            displayName = "Neon Spark",
            drawableRes = R.drawable.file_000000000278820bb0a8901e8fa6612b,
            innerRatio = 0.7209f,
            centerOffsetX = -0.0435f,
            centerOffsetY = -0.0092f
        ),
        ProfileBorder(
            id = "file_000000001c6c820bbc0efdac44c0aa63",
            displayName = "Crystal Arc",
            drawableRes = R.drawable.file_000000001c6c820bbc0efdac44c0aa63,
            innerRatio = 0.7014f,
            centerOffsetX = 0.0028f,
            centerOffsetY = 0.0335f
        ),
        ProfileBorder(
            id = "file_000000007fdc81f7b74b9a79e2c63215",
            displayName = "Golden Wings",
            drawableRes = R.drawable.file_000000007fdc81f7b74b9a79e2c63215,
            innerRatio = 0.6511f,
            centerOffsetX = -0.0378f,
            centerOffsetY = -0.0088f
        )
    )

    fun getById(id: String?): ProfileBorder {
        if (id.isNullOrBlank()) return NONE
        return ALL_BORDERS.firstOrNull { it.id == id } ?: NONE
    }
}
