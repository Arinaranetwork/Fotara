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
    val targetGroupId: Long? = null,
    val targetDocumentId: Long? = null,
    val targetTextNoteId: Long? = null,
    val targetCanvasId: Long? = null,
    val targetPageIndex: Int? = null
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

@Serializable
data class PdfViewerNavKey(val documentId: Long) : NavKey

@Serializable
data class DocxViewerNavKey(val documentId: Long) : NavKey

@Serializable
data class TextNoteEditorNavKey(
    val noteId: Long? = null,
    val folderId: Long,
    val subfolderId: Long? = null
) : NavKey

@Serializable
data object UpdateNavKey : NavKey

@Serializable
data object SupportNavKey : NavKey

@Serializable
data object WhatsNewNavKey : NavKey

@Serializable
data class CanvasNoteNavKey(
    val canvasId: Long? = null,
    val folderId: Long,
    val subfolderId: Long? = null
) : NavKey

