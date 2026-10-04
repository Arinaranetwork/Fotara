// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.folder

import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.data.model.PhotoGroup

data class GroupSection(
    val folderId: Long,
    val folderName: String,
    val isCurrentFolder: Boolean,
    val groups: List<PhotoGroup>
)

data class GroupSectionsResult(
    val sections: List<GroupSection>,
    val hasOtherFolderSections: Boolean,
    val totalGroupsCount: Int
)

object AddToGroupSectionHelper {

    fun buildSections(
        currentFolderId: Long,
        currentFolderName: String,
        availableFolders: List<Folder>,
        allGroups: List<PhotoGroup>
    ): GroupSectionsResult {
        if (allGroups.isEmpty()) {
            return GroupSectionsResult(
                sections = emptyList(),
                hasOtherFolderSections = false,
                totalGroupsCount = 0
            )
        }

        val sections = mutableListOf<GroupSection>()

        // 1. Current folder's groups first
        val currentFolderGroups = allGroups.filter { it.folderId == currentFolderId }
        if (currentFolderGroups.isNotEmpty()) {
            sections.add(
                GroupSection(
                    folderId = currentFolderId,
                    folderName = currentFolderName,
                    isCurrentFolder = true,
                    groups = currentFolderGroups
                )
            )
        }

        // 2. Other folders in Home order
        val otherFolders = availableFolders.filter { it.id != currentFolderId }
        val otherFolderIds = otherFolders.map { it.id }.toSet()

        for (folder in otherFolders) {
            val folderGroups = allGroups.filter { it.folderId == folder.id }
            if (folderGroups.isNotEmpty()) {
                sections.add(
                    GroupSection(
                        folderId = folder.id,
                        folderName = folder.name,
                        isCurrentFolder = false,
                        groups = folderGroups
                    )
                )
            }
        }

        // Check if there are groups in folders not in availableFolders (edge case)
        val remainingGroups = allGroups.filter { it.folderId != currentFolderId && it.folderId !in otherFolderIds }
        if (remainingGroups.isNotEmpty()) {
            val groupedByFolder = remainingGroups.groupBy { it.folderId }
            for ((fId, fGroups) in groupedByFolder) {
                sections.add(
                    GroupSection(
                        folderId = fId,
                        folderName = "Folder #$fId",
                        isCurrentFolder = false,
                        groups = fGroups
                    )
                )
            }
        }

        val hasOther = sections.any { !it.isCurrentFolder }
        val total = sections.sumOf { it.groups.size }

        return GroupSectionsResult(
            sections = sections,
            hasOtherFolderSections = hasOther,
            totalGroupsCount = total
        )
    }
}
