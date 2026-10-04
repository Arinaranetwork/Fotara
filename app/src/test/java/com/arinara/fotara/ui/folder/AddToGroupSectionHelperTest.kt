// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.folder

import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.data.model.PhotoGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddToGroupSectionHelperTest {

    @Test
    fun testBuildSections_noGroups_returnsEmptyResult() {
        val currentFolderId = 1L
        val currentFolderName = "Math"
        val folders = listOf(
            Folder(id = 1L, name = "Math"),
            Folder(id = 2L, name = "Physics")
        )
        val result = AddToGroupSectionHelper.buildSections(
            currentFolderId = currentFolderId,
            currentFolderName = currentFolderName,
            availableFolders = folders,
            allGroups = emptyList()
        )

        assertTrue(result.sections.isEmpty())
        assertFalse(result.hasOtherFolderSections)
        assertEquals(0, result.totalGroupsCount)
    }

    @Test
    fun testBuildSections_currentFolderFirst_otherFoldersInHomeOrder() {
        val currentFolderId = 2L
        val currentFolderName = "Physics"

        // Folders in Home order: Math (1), Physics (2), Chemistry (3), Literature (4)
        val folders = listOf(
            Folder(id = 1L, name = "Math"),
            Folder(id = 2L, name = "Physics"),
            Folder(id = 3L, name = "Chemistry"),
            Folder(id = 4L, name = "Literature")
        )

        val groups = listOf(
            PhotoGroup(id = 101L, folderId = 1L, name = "Algebra Homework"),
            PhotoGroup(id = 201L, folderId = 2L, name = "Optics Lab"),
            PhotoGroup(id = 202L, folderId = 2L, subfolderId = 55L, name = "Thermodynamics"), // subfolder group
            PhotoGroup(id = 301L, folderId = 3L, name = "Organic Reactions")
            // Literature (4) has no groups!
        )

        val result = AddToGroupSectionHelper.buildSections(
            currentFolderId = currentFolderId,
            currentFolderName = currentFolderName,
            availableFolders = folders,
            allGroups = groups
        )

        // 3 sections expected: Physics (current), Math, Chemistry. Literature should be omitted!
        assertEquals(3, result.sections.size)
        assertTrue(result.hasOtherFolderSections)
        assertEquals(4, result.totalGroupsCount)

        // 1st section: Current folder (Physics)
        val s1 = result.sections[0]
        assertEquals(2L, s1.folderId)
        assertEquals("Physics", s1.folderName)
        assertTrue(s1.isCurrentFolder)
        assertEquals(2, s1.groups.size)
        assertEquals(listOf("Optics Lab", "Thermodynamics"), s1.groups.map { it.name })

        // 2nd section: Math (Home order)
        val s2 = result.sections[1]
        assertEquals(1L, s2.folderId)
        assertEquals("Math", s2.folderName)
        assertFalse(s2.isCurrentFolder)
        assertEquals(1, s2.groups.size)
        assertEquals("Algebra Homework", s2.groups[0].name)

        // 3rd section: Chemistry (Home order)
        val s3 = result.sections[2]
        assertEquals(3L, s3.folderId)
        assertEquals("Chemistry", s3.folderName)
        assertFalse(s3.isCurrentFolder)
        assertEquals(1, s3.groups.size)
        assertEquals("Organic Reactions", s3.groups[0].name)
    }

    @Test
    fun testBuildSections_onlyCurrentFolderHasGroups_hasOtherFolderSectionsIsFalse() {
        val currentFolderId = 1L
        val currentFolderName = "Math"
        val folders = listOf(
            Folder(id = 1L, name = "Math"),
            Folder(id = 2L, name = "Physics")
        )
        val groups = listOf(
            PhotoGroup(id = 101L, folderId = 1L, name = "Calculus")
        )

        val result = AddToGroupSectionHelper.buildSections(
            currentFolderId = currentFolderId,
            currentFolderName = currentFolderName,
            availableFolders = folders,
            allGroups = groups
        )

        assertEquals(1, result.sections.size)
        assertFalse(result.hasOtherFolderSections)
        assertEquals("Calculus", result.sections[0].groups[0].name)
    }
}
