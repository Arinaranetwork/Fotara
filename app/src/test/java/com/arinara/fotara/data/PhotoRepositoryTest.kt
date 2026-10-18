// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data

import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.model.PhotoSource
import com.arinara.fotara.data.model.TagColor
import com.arinara.fotara.test.FakeFolderRepository
import com.arinara.fotara.test.FakePhotoRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PhotoRepositoryTest {

    private lateinit var folderRepository: FakeFolderRepository
    private lateinit var photoRepository: FakePhotoRepository

    @Before
    fun setup() {
        folderRepository = FakeFolderRepository()
        photoRepository = FakePhotoRepository(folderRepository)
    }

    @Test
    fun getPhotosByFolder_returnsCorrectPhotos() = runTest {
        val bioPhotos = photoRepository.getPhotosByFolder(1).first()
        assertTrue(bioPhotos.isNotEmpty())
        assertTrue(bioPhotos.all { it.folderId == 1L })
    }

    @Test
    fun searchPhotos_matchesOcrTextKeywords() = runTest {
        val results = photoRepository.searchPhotos("mitochondria").first()
        assertTrue("Search should find note containing 'mitochondria'", results.isNotEmpty())
        assertTrue(results.first().ocrText?.contains("mitochondria", ignoreCase = true) == true)
    }

    @Test
    fun addPhoto_updatesFolderPhotoCount() = runTest {
        val initialFolder = folderRepository.getFolderById(1).first()
        val initialCount = initialFolder?.photoCount ?: 0

        val newPhoto = Photo(
            fileUri = "sample://photos/test_note.jpg",
            folderId = 1,
            caption = "Test Note Caption",
            ocrText = "Test OCR content for verification",
            source = PhotoSource.CAMERA,
            tagColor = TagColor.SKY.hex
        )

        val id = photoRepository.addPhoto(newPhoto)
        assertTrue(id > 0)

        val updatedFolder = folderRepository.getFolderById(1).first()
        assertEquals(initialCount + 1, updatedFolder?.photoCount)
    }

    @Test
    fun renamePhoto_updatesCaptionSuccessfully() = runTest {
        val original = photoRepository.getPhotosByFolder(1).first().first()
        val newCaption = "Renamed Microscope Slide #4"
        photoRepository.renamePhoto(original.id, newCaption)

        val updated = photoRepository.getPhotosByFolder(1).first().first { it.id == original.id }
        assertEquals(newCaption, updated.caption)
    }

    @Test
    fun updatePhotoNote_persistsAndSearchesSuccessfully() = runTest {
        val original = photoRepository.getPhotosByFolder(1).first().first()
        val customNote = "Important formula: E=mc^2 will be on the final exam."
        photoRepository.updatePhotoNote(original.id, customNote)

        val updated = photoRepository.getPhotosByFolder(1).first().first { it.id == original.id }
        assertEquals(customNote, updated.note)

        // Verify search matching on custom note
        val searchResults = photoRepository.searchPhotos("formula").first()
        assertTrue(searchResults.any { it.id == original.id })
    }

    @Test
    fun smartTags_extractionFromCaptionNoteAndExplicitTags() {
        val testPhoto = Photo(
            id = 999L,
            fileUri = "sample://photo999.jpg",
            folderId = 1L,
            caption = "Reviewing #derivatives before #exam",
            note = "Check #formula sheet for #exam chapter 4",
            tags = "homework, calculus"
        )

        val tags = testPhoto.getAllSmartTags()
        assertTrue(tags.contains("derivatives"))
        assertTrue(tags.contains("exam"))
        assertTrue(tags.contains("formula"))
        assertTrue(tags.contains("homework"))
        assertTrue(tags.contains("calculus"))
        assertEquals(5, tags.size)
    }

    @Test
    fun getAllSmartTags_aggregatesAcrossFolders() = runTest {
        // Add note in folder 1
        photoRepository.addPhoto(
            Photo(
                fileUri = "sample://p1.jpg",
                folderId = 1L,
                caption = "Physics #lab report",
                note = "Refer to #formula 3"
            )
        )
        // Add note in folder 2
        photoRepository.addPhoto(
            Photo(
                fileUri = "sample://p2.jpg",
                folderId = 2L,
                caption = "Calculus #formula proof",
                note = "On the #exam"
            )
        )

        val allTags = photoRepository.getAllSmartTags().first()
        assertTrue(allTags.contains("formula"))
        assertTrue(allTags.contains("lab"))
        assertTrue(allTags.contains("exam"))
    }

    @Test
    fun getPhotosByTag_returnsCrossFolderPhotos() = runTest {
        val id1 = photoRepository.addPhoto(
            Photo(
                fileUri = "sample://cf1.jpg",
                folderId = 1L,
                caption = "Physics mechanics",
                note = "Important #examPrep"
            )
        )
        val id2 = photoRepository.addPhoto(
            Photo(
                fileUri = "sample://cf2.jpg",
                folderId = 2L,
                caption = "Chemistry reactions",
                note = "Review for #examPrep"
            )
        )

        val matching = photoRepository.getPhotosByTag("examPrep").first()
        assertEquals(2, matching.size)
        assertTrue(matching.any { it.id == id1 && it.folderId == 1L })
        assertTrue(matching.any { it.id == id2 && it.folderId == 2L })
    }

    @Test
    fun addTagToPhoto_and_removeTagFromPhoto() = runTest {
        val original = photoRepository.getPhotosByFolder(1).first().first()
        photoRepository.addTagToPhoto(original.id, "midterm")

        var tags = photoRepository.getPhotoById(original.id)?.getAllSmartTags() ?: emptyList()
        assertTrue(tags.contains("midterm"))

        photoRepository.removeTagFromPhoto(original.id, "midterm")
        tags = photoRepository.getPhotoById(original.id)?.getAllSmartTags() ?: emptyList()
        assertFalse(tags.contains("midterm"))
    }
}
