// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data

import com.arinara.fotara.data.model.DestinationType
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.model.RecentDestination
import com.arinara.fotara.data.model.ThemeMode
import com.arinara.fotara.test.FakeFolderRepository
import com.arinara.fotara.test.FakePhotoRepository
import com.arinara.fotara.test.FakeSettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class V12FeaturesTest {

    private lateinit var folderRepository: FakeFolderRepository
    private lateinit var photoRepository: FakePhotoRepository
    private lateinit var settingsRepository: FakeSettingsRepository

    @Before
    fun setup() {
        folderRepository = FakeFolderRepository()
        photoRepository = FakePhotoRepository(folderRepository)
        settingsRepository = FakeSettingsRepository()
    }

    @Test
    fun themeMode_removesLightAndMigratesLegacyToSystem() {
        // Only SYSTEM and DARK must exist
        val modes = ThemeMode.entries
        assertEquals(2, modes.size)
        assertTrue(modes.contains(ThemeMode.SYSTEM))
        assertTrue(modes.contains(ThemeMode.DARK))

        // Legacy "LIGHT" setting must resolve to SYSTEM
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromName("LIGHT"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromName("light"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromName("DARK"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromName("SYSTEM"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromName("UNKNOWN"))
    }

    @Test
    fun folderLinkGroup_createAndAutoDissolve() = runTest {
        val linkId = folderRepository.createFolderLinkGroup(listOf(1L, 2L, 3L))
        assertTrue(linkId > 0)

        var linkGroups = folderRepository.getFolderLinkGroups().first()
        assertEquals(1, linkGroups.size)
        assertEquals(listOf(1L, 2L, 3L), linkGroups[0].memberIds)

        // Unlink one folder: 2 remain, group is still intact
        folderRepository.unlinkFolder(1L)
        linkGroups = folderRepository.getFolderLinkGroups().first()
        assertEquals(1, linkGroups.size)
        assertEquals(listOf(2L, 3L), linkGroups[0].memberIds)

        // Unlink another folder: <= 1 remains, group automatically dissolves
        folderRepository.unlinkFolder(2L)
        linkGroups = folderRepository.getFolderLinkGroups().first()
        assertTrue(linkGroups.isEmpty())
    }

    @Test
    fun gridLinkGroup_createAndAutoDissolve() = runTest {
        val linkId = photoRepository.createGridLinkGroup(listOf(101L, 102L, 103L))
        assertTrue(linkId > 0)

        var linkGroups = photoRepository.getGridLinkGroups().first()
        assertEquals(1, linkGroups.size)
        assertEquals(listOf(101L, 102L, 103L), linkGroups[0].memberIds)

        // Unlink one item: 2 remain, group is intact
        photoRepository.unlinkGridItem(101L)
        linkGroups = photoRepository.getGridLinkGroups().first()
        assertEquals(1, linkGroups.size)
        assertEquals(listOf(102L, 103L), linkGroups[0].memberIds)

        // Unlink another item: <= 1 remains, auto-dissolved
        photoRepository.unlinkGridItem(102L)
        linkGroups = photoRepository.getGridLinkGroups().first()
        assertTrue(linkGroups.isEmpty())
    }

    @Test
    fun mergeGroups_consolidatesPhotosAndDissolvesOldGroups() = runTest {
        // Create 2 groups
        val g1 = photoRepository.createGroup(folderId = 1L, subfolderId = null, name = "Group 1", photoIds = listOf(1L))
        val g2 = photoRepository.createGroup(folderId = 1L, subfolderId = null, name = "Group 2", photoIds = listOf(2L))

        var activeGroups = photoRepository.getAllActiveGroups().first()
        assertTrue(activeGroups.any { it.id == g1 })
        assertTrue(activeGroups.any { it.id == g2 })

        // Merge groups into new group in folder 2
        val mergedId = photoRepository.mergeGroups(
            sourceGroupIds = listOf(g1, g2),
            newName = "Merged Science",
            targetFolderId = 2L,
            targetSubfolderId = null
        )
        assertTrue(mergedId > 0)

        activeGroups = photoRepository.getAllActiveGroups().first()
        assertFalse("Old group 1 should be dissolved", activeGroups.any { it.id == g1 })
        assertFalse("Old group 2 should be dissolved", activeGroups.any { it.id == g2 })

        val merged = activeGroups.firstOrNull { it.id == mergedId }
        assertNotNull(merged)
        assertEquals("Merged Science", merged?.name)
        assertEquals(2L, merged?.folderId)

        // Photos 1 and 2 must now have groupId = mergedId and folderId = 2
        val p1 = photoRepository.getPhotoById(1L)
        val p2 = photoRepository.getPhotoById(2L)
        assertEquals(mergedId, p1?.groupId)
        assertEquals(2L, p1?.folderId)
        assertEquals(mergedId, p2?.groupId)
        assertEquals(2L, p2?.folderId)
    }

    @Test
    fun moveGroup_updatesGroupAndMemberPhotos() = runTest {
        val gId = photoRepository.createGroup(folderId = 1L, subfolderId = null, name = "Bio Notes", photoIds = listOf(1L, 2L))

        // Move group to folder 2
        photoRepository.moveGroup(groupId = gId, targetFolderId = 2L, targetSubfolderId = 3L)

        val group = photoRepository.getGroupById(gId)
        assertNotNull(group)
        assertEquals(2L, group?.folderId)
        assertEquals(3L, group?.subfolderId)

        val p1 = photoRepository.getPhotoById(1L)
        val p2 = photoRepository.getPhotoById(2L)
        assertEquals(2L, p1?.folderId)
        assertEquals(3L, p1?.subfolderId)
        assertEquals(2L, p2?.folderId)
        assertEquals(3L, p2?.subfolderId)
    }

    @Test
    fun addPhotosToExistingGroup_assignsGroupId() = runTest {
        val gId = photoRepository.createGroup(folderId = 1L, subfolderId = null, name = "Target Group", photoIds = listOf(1L))
        val p2Before = photoRepository.getPhotoById(2L)
        assertNull(p2Before?.groupId)

        photoRepository.addPhotosToExistingGroup(gId, listOf(2L))
        val p2After = photoRepository.getPhotoById(2L)
        assertEquals(gId, p2After?.groupId)
    }

    @Test
    fun groupCoverPhoto_updatesCorrectly() = runTest {
        val gId = photoRepository.createGroup(folderId = 1L, subfolderId = null, name = "Cover Test", photoIds = listOf(1L, 2L))
        photoRepository.setGroupCoverPhoto(gId, 2L)

        val group = photoRepository.getGroupById(gId)
        assertEquals(2L, group?.coverPhotoId)
    }

    @Test
    fun groupDeadline_updatesCorrectly() = runTest {
        val gId = photoRepository.createGroup(folderId = 1L, subfolderId = null, name = "Deadline Test", photoIds = listOf(1L, 2L))
        val deadline = System.currentTimeMillis() + 86400000L

        photoRepository.updateGroupDeadline(gId, deadline)
        var group = photoRepository.getGroupById(gId)
        assertEquals(deadline, group?.linkedDeadline)

        // Clear deadline
        photoRepository.updateGroupDeadline(gId, null)
        group = photoRepository.getGroupById(gId)
        assertNull(group?.linkedDeadline)
    }

    @Test
    fun copyPhoto_createsIndependentDuplicate() = runTest {
        val original = photoRepository.getPhotoById(1L)
        assertNotNull(original)

        val newId = photoRepository.copyPhoto(
            photoId = 1L,
            targetFolderId = 2L,
            targetSubfolderId = null,
            targetGroupId = null
        )
        assertTrue(newId > 1L)

        val copy = photoRepository.getPhotoById(newId)
        assertNotNull(copy)
        assertEquals(2L, copy?.folderId)
        assertNull(copy?.subfolderId)
        assertNull(copy?.groupId)
        assertEquals(original?.caption, copy?.caption)
        assertEquals(original?.ocrText, copy?.ocrText)
        assertEquals(original?.fileUri, copy?.fileUri)

        // Verifying original is untouched
        val originalCheck = photoRepository.getPhotoById(1L)
        assertEquals(1L, originalCheck?.folderId)
    }

    @Test
    fun recentDestinations_storesUpToFiveAndDedupes() = runTest {
        val dest1 = RecentDestination(DestinationType.FOLDER, folderId = 1L, title = "Folder 1")
        val dest2 = RecentDestination(DestinationType.SUBFOLDER, folderId = 1L, subfolderId = 2L, title = "Subfolder 2")
        val dest3 = RecentDestination(DestinationType.GROUP, folderId = 1L, groupId = 3L, title = "Group 3")
        val dest4 = RecentDestination(DestinationType.FOLDER, folderId = 4L, title = "Folder 4")
        val dest5 = RecentDestination(DestinationType.FOLDER, folderId = 5L, title = "Folder 5")
        val dest6 = RecentDestination(DestinationType.FOLDER, folderId = 6L, title = "Folder 6")

        settingsRepository.addRecentDestination(dest1)
        settingsRepository.addRecentDestination(dest2)
        settingsRepository.addRecentDestination(dest3)
        settingsRepository.addRecentDestination(dest4)
        settingsRepository.addRecentDestination(dest5)

        assertEquals(5, settingsRepository.getRecentDestinations().size)
        assertEquals(dest5.title, settingsRepository.getRecentDestinations()[0].title)

        // Adding 6th: should cap at 5, dropping dest1
        settingsRepository.addRecentDestination(dest6)
        val recents = settingsRepository.getRecentDestinations()
        assertEquals(5, recents.size)
        assertEquals(dest6.title, recents[0].title)
        assertFalse(recents.any { it.title == "Folder 1" })

        // Adding dest2 again: should move to front, still 5 items
        settingsRepository.addRecentDestination(dest2)
        val recentsAfterDedupe = settingsRepository.getRecentDestinations()
        assertEquals(5, recentsAfterDedupe.size)
        assertEquals(dest2.title, recentsAfterDedupe[0].title)

        // Clear
        settingsRepository.clearRecentDestinations()
        assertTrue(settingsRepository.getRecentDestinations().isEmpty())
    }
}
