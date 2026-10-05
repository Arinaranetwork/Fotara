// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.model

data class UserProfile(
    val name: String = "",
    val email: String = "",
    val avatarPath: String? = null,
    val bannerPath: String? = null,
    val borderId: String = "none",
    val avatarUpdatedAt: Long = 0L,
    val bannerUpdatedAt: Long = 0L,
    val bannerCrop: String? = null
) {
    fun resolvedName(defaultFallback: String = "Fotara User"): String {
        val trimmed = name.trim()
        return if (trimmed.isEmpty()) defaultFallback else trimmed
    }

    val hasCustomAvatar: Boolean get() = !avatarPath.isNullOrBlank()
    val hasCustomBanner: Boolean get() = !bannerPath.isNullOrBlank()
    val hasBorder: Boolean get() = borderId.isNotBlank() && borderId != "none"
    val isBannerGif: Boolean get() = bannerPath != null && bannerPath.endsWith(".gif", ignoreCase = true)
}
