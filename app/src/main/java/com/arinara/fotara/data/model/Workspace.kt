// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.model

import java.util.UUID

enum class WorkspaceKind {
    HOME,
    ARCHIVE,
    CUSTOM
}

data class Workspace(
    val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val kind: WorkspaceKind,
    val name: String = "",
    val position: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isHome: Boolean get() = kind == WorkspaceKind.HOME
    val isArchive: Boolean get() = kind == WorkspaceKind.ARCHIVE
    val isCustom: Boolean get() = kind == WorkspaceKind.CUSTOM
}
