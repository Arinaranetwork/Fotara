// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data

import com.arinara.fotara.data.model.TagColor
import com.arinara.fotara.test.FakeFolderRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FolderRepositoryTest {

    private lateinit var repository: FakeFolderRepository

    @Before
    fun setup() {
        repository = FakeFolderRepository()
    }

    @Test
    fun getFolders_containsInitialMockFolders() = runTest {
        val folders = repository.getFolders().first()
        assertTrue("Folders should not be empty", folders.isNotEmpty())
        assertTrue("Biology folder should exist", folders.any { it.name == "Biology" })
    }

    @Test
    fun createFolder_addsFolderSuccessfully() = runTest {
        val newId = repository.createFolder(
            name = "Astrophysics",
            colorLabel = TagColor.VIOLET.hex,
            isPinned = true
        )
        assertTrue(newId > 0)

        val folders = repository.getFolders().first()
        val created = folders.firstOrNull { it.id == newId }
        assertNotNull(created)
        assertEquals("Astrophysics", created?.name)
        assertEquals(TagColor.VIOLET.hex, created?.colorLabel)
        assertTrue(created?.isPinned == true)
        assertEquals("Pinned folder should be first in list", "Astrophysics", folders.first().name)
    }

    @Test
    fun subfolderOperations_workCorrectly() = runTest {
        val subfolderId = repository.createSubfolder(
            folderId = 1,
            name = "Microbiology Lab"
        )
        assertTrue(subfolderId > 0)

        val subfolders = repository.getSubfolders(1).first()
        assertTrue(subfolders.any { it.name == "Microbiology Lab" })

        repository.deleteSubfolder(subfolderId)
        val afterDelete = repository.getSubfolders(1).first()
        assertTrue(afterDelete.none { it.id == subfolderId })
    }

    @Test
    fun renameFolder_updatesNameCorrectly() = runTest {
        repository.renameFolder(1, "Molecular Biology")
        val folder = repository.getFolderById(1).first()
        assertNotNull(folder)
        assertEquals("Molecular Biology", folder?.name)
    }

    @Test
    fun updateFolderColor_updatesColorCorrectly() = runTest {
        repository.updateFolderColor(1, TagColor.CRIMSON.hex)
        val folder = repository.getFolderById(1).first()
        assertNotNull(folder)
        assertEquals(TagColor.CRIMSON.hex, folder?.colorLabel)
    }

    @Test
    fun renameSubfolder_updatesSubfolderName() = runTest {
        repository.renameSubfolder(1, "Advanced Genetics")
        val subfolders = repository.getSubfolders(1).first()
        assertTrue(subfolders.any { it.id == 1L && it.name == "Advanced Genetics" })
    }

    @Test
    fun bulkDelete_calculatesStatsAndRemovesFolders() = runTest {
        val stats = repository.getBulkDeleteStats(listOf(1L, 2L))
        assertEquals(2, stats.folderCount)
        assertEquals(3, stats.photoCount)
        assertEquals(3072L, stats.totalSizeBytes)

        val result = repository.deleteFolders(listOf(1L))
        assertEquals(1, result.folderCount)
        val remaining = repository.getFolders().first()
        assertEquals(1, remaining.size)
        assertEquals(2L, remaining.first().id)
    }

    @Test
    fun lockFolder_andUnlockFolder_updatesLockState() = runTest {
        repository.lockFolder(1L, "1234")
        val lockedFolder = repository.getFolderById(1L).first()
        assertNotNull(lockedFolder)
        assertTrue("Folder should be locked", lockedFolder?.isLocked == true)
        assertEquals("1234", lockedFolder?.lockPin)

        repository.updateFolderPin(1L, "9999")
        val updatedFolder = repository.getFolderById(1L).first()
        assertEquals("9999", updatedFolder?.lockPin)

        repository.unlockFolder(1L)
        val unlockedFolder = repository.getFolderById(1L).first()
        assertNotNull(unlockedFolder)
        assertTrue("Folder should be unlocked", unlockedFolder?.isLocked == false)
        assertEquals(null, unlockedFolder?.lockPin)
    }
}
