// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.R
import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.data.model.Workspace
import com.arinara.fotara.data.model.WorkspaceKind
import com.arinara.fotara.data.model.getDisplayName
import com.arinara.fotara.theme.ElmsSans

data class WorkspaceFolderGroup(
    val workspace: Workspace,
    val folders: List<Folder>
)

object WorkspaceFolderGroupingHelper {

    /**
     * Groups folders by workspace in saved order (Home first, then Archive, then custom in their order).
     * Workspaces without folders are omitted.
     * Returns a pair of:
     * - groups: List of WorkspaceFolderGroup
     * - shouldShowHeaders: true if folders exist in 2 or more workspaces; false if 1 or 0 workspaces.
     */
    fun groupFolders(
        folders: List<Folder>,
        workspaces: List<Workspace>
    ): Pair<List<WorkspaceFolderGroup>, Boolean> {
        if (folders.isEmpty()) {
            return Pair(emptyList(), false)
        }

        // Map workspace by id. If folder points to unknown workspace, group under Home (id=1)
        val wsMap = workspaces.associateBy { it.id }
        val homeWs = workspaces.firstOrNull { it.kind == WorkspaceKind.HOME }
            ?: Workspace(id = 1L, uuid = "home", kind = WorkspaceKind.HOME, name = "", position = 0)

        // Group folders by their workspaceId (fallback to Home)
        val foldersByWs = folders.groupBy { f ->
            wsMap[f.workspaceId] ?: homeWs
        }

        // Sort workspaces in saved order: position ASC (Home at pos 0, then others)
        val sortedWorkspaces = foldersByWs.keys.sortedBy { it.position }

        val groups = sortedWorkspaces.mapNotNull { ws ->
            val wsFolders = foldersByWs[ws] ?: emptyList()
            if (wsFolders.isNotEmpty()) {
                WorkspaceFolderGroup(ws, wsFolders)
            } else null
        }

        val shouldShowHeaders = groups.size > 1
        return Pair(groups, shouldShowHeaders)
    }

    fun getWorkspaceDisplayName(workspace: Workspace, homeLabel: String, archiveLabel: String): String {
        return workspace.getDisplayName(homeLabel, archiveLabel)
    }
}

/**
 * LazyListScope extension that emits folder items grouped by workspace with small muted headers.
 * If folders exist in only one workspace, emits the flat list with no headers.
 */
fun LazyListScope.workspaceGroupedFolderItems(
    folders: List<Folder>,
    workspaces: List<Workspace>,
    keyPrefix: String = "folder",
    itemContent: @Composable (folder: Folder) -> Unit
) {
    val (groups, showHeaders) = WorkspaceFolderGroupingHelper.groupFolders(folders, workspaces)

    if (!showHeaders) {
        items(folders, key = { "${keyPrefix}_${it.id}" }) { folder ->
            itemContent(folder)
        }
    } else {
        groups.forEach { group ->
            item(key = "ws_header_${group.workspace.id}") {
                WorkspaceHeaderItem(workspace = group.workspace)
            }
            items(group.folders, key = { "${keyPrefix}_${it.id}" }) { folder ->
                itemContent(folder)
            }
        }
    }
}

@Composable
fun WorkspaceHeaderItem(workspace: Workspace, modifier: Modifier = Modifier) {
    val name = workspace.getDisplayName()

    Text(
        text = name,
        color = Color(0xFF9CA3AF),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = ElmsSans,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp, start = 4.dp)
    )
}

@Composable
fun WorkspaceFolderPickerColumn(
    folders: List<Folder>,
    workspaces: List<Workspace>,
    modifier: Modifier = Modifier,
    itemContent: @Composable (folder: Folder) -> Unit
) {
    val (groups, showHeaders) = WorkspaceFolderGroupingHelper.groupFolders(folders, workspaces)
    Column(modifier = modifier) {
        if (!showHeaders) {
            for (folder in folders) {
                itemContent(folder)
            }
        } else {
            for (group in groups) {
                WorkspaceHeaderItem(workspace = group.workspace)
                for (folder in group.folders) {
                    itemContent(folder)
                }
            }
        }
    }
}

