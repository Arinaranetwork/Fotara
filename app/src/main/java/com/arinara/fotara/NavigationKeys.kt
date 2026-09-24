// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object HomeNavKey : NavKey

@Serializable
data class FolderDetailNavKey(
    val folderId: Long,
    val initialSubfolderId: Long? = null,
    val targetPhotoId: Long? = null,
    val openViewerDirectly: Boolean = false,
    val targetGroupId: Long? = null
) : NavKey

@Serializable
data class GroupDetailNavKey(
    val folderId: Long,
    val groupId: Long,
    val targetPhotoId: Long? = null
) : NavKey

@Serializable
data object TrashNavKey : NavKey

@Serializable
data object SettingsNavKey : NavKey

@Serializable
data object OnboardingNavKey : NavKey

