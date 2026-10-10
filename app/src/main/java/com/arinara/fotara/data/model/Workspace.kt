// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.model

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.arinara.fotara.R
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
    val createdAt: Long = System.currentTimeMillis(),
    val iconKey: String? = null,
    val spaceId: Long = 1L
) {
    val isHome: Boolean get() = kind == WorkspaceKind.HOME
    val isArchive: Boolean get() = kind == WorkspaceKind.ARCHIVE
    val isCustom: Boolean get() = kind == WorkspaceKind.CUSTOM
}

data class WorkspaceDisplay(
    val kind: WorkspaceKind,
    val name: String,
    @get:DrawableRes val iconResId: Int
)

/**
 * Universal display name resolver for Workspace across all UI and presentation layers.
 * Maps HOME -> "Home", ARCHIVE -> "Archive", and CUSTOM -> stored name.
 */
fun Workspace.getDisplayName(context: Context): String = when (kind) {
    WorkspaceKind.HOME -> context.getString(R.string.workspace_home)
    WorkspaceKind.ARCHIVE -> context.getString(R.string.workspace_archive)
    WorkspaceKind.CUSTOM -> name
}

fun Workspace.getDisplayName(homeLabel: String, archiveLabel: String): String = when (kind) {
    WorkspaceKind.HOME -> homeLabel
    WorkspaceKind.ARCHIVE -> archiveLabel
    WorkspaceKind.CUSTOM -> name
}

@Composable
fun Workspace.getDisplayName(): String = when (kind) {
    WorkspaceKind.HOME -> stringResource(R.string.workspace_home)
    WorkspaceKind.ARCHIVE -> stringResource(R.string.workspace_archive)
    WorkspaceKind.CUSTOM -> name
}

@Composable
fun Workspace.getDisplay(): WorkspaceDisplay = WorkspaceDisplay(
    kind = kind,
    name = getDisplayName(),
    iconResId = when (kind) {
        WorkspaceKind.HOME -> R.drawable.ic_ws_grid
        WorkspaceKind.ARCHIVE -> R.drawable.ic_ws_archive
        WorkspaceKind.CUSTOM -> WorkspaceIcons.getIconResId(iconKey)
    }
)

fun Workspace.getDisplay(context: Context): WorkspaceDisplay = WorkspaceDisplay(
    kind = kind,
    name = getDisplayName(context),
    iconResId = when (kind) {
        WorkspaceKind.HOME -> R.drawable.ic_ws_grid
        WorkspaceKind.ARCHIVE -> R.drawable.ic_ws_archive
        WorkspaceKind.CUSTOM -> WorkspaceIcons.getIconResId(iconKey)
    }
)

fun Workspace.getDisplay(homeLabel: String, archiveLabel: String): WorkspaceDisplay = WorkspaceDisplay(
    kind = kind,
    name = getDisplayName(homeLabel, archiveLabel),
    iconResId = when (kind) {
        WorkspaceKind.HOME -> R.drawable.ic_ws_grid
        WorkspaceKind.ARCHIVE -> R.drawable.ic_ws_archive
        WorkspaceKind.CUSTOM -> WorkspaceIcons.getIconResId(iconKey)
    }
)
