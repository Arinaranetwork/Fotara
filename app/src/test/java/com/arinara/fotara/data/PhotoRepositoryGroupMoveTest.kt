// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data

import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.model.PhotoGroup
import com.arinara.fotara.test.FakePhotoRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PhotoRepositoryGroupMoveTest {

    @Test
    fun testAddPhotosToGroup_updatesFolderAndSubfolderAtomically_andPreservesAddedAt() = runBlocking {
        val fakeRepo = FakePhotoRepository()

        // Group belongs to Folder 2, Subfolder 20
        val targetGroupId = fakeRepo.createGroup(
            folderId = 2L,
            subfolderId = 20L,
            name = "Science Project",
            photoIds = emptyList(),
            tagColor = null
        )

        // Photo originally in Folder 1, Subfolder null
        val originalAddedAt = 1700000000000L
        val photo1 = Photo(
            id = 0L,
            fileUri = "file://photo1.jpg",
            thumbnailUri = "file://thumb1.jpg",
            folderId = 1L,
            subfolderId = null,
            groupId = null,
            addedAt = originalAddedAt
        )
        val photo2 = Photo(
            id = 0L,
            fileUri = "file://photo2.jpg",
            thumbnailUri = "file://thumb2.jpg",
            folderId = 1L,
            subfolderId = 5L,
            groupId = null,
            addedAt = originalAddedAt + 5000L
        )
        val p1Id = fakeRepo.addPhoto(photo1)
        val p2Id = fakeRepo.addPhoto(photo2)

        // Add photos to target group
        fakeRepo.addPhotosToGroup(targetGroupId, listOf(p1Id, p2Id))

        val photos = fakeRepo.getAllActivePhotosFlow().first()
        val updatedPhoto1 = photos.firstOrNull { it.id == p1Id }
        val updatedPhoto2 = photos.firstOrNull { it.id == p2Id }

        assertNotNull(updatedPhoto1)
        assertNotNull(updatedPhoto2)

        // Verify group_id, folder_id, subfolder_id are updated to match targetGroup
        assertEquals(targetGroupId, updatedPhoto1!!.groupId)
        assertEquals(2L, updatedPhoto1.folderId)
        assertEquals(20L, updatedPhoto1.subfolderId)
        // Verify added_at is completely preserved
        assertEquals(originalAddedAt, updatedPhoto1.addedAt)

        assertEquals(targetGroupId, updatedPhoto2!!.groupId)
        assertEquals(2L, updatedPhoto2.folderId)
        assertEquals(20L, updatedPhoto2.subfolderId)
        assertEquals(originalAddedAt + 5000L, updatedPhoto2.addedAt)
    }
}
