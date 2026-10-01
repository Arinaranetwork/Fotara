// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.db

import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.model.PhotoGroup
import com.arinara.fotara.data.model.TextNote
import com.arinara.fotara.data.model.DocumentNote
import com.arinara.fotara.data.model.DocumentType
import com.arinara.fotara.data.model.CanvasNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseMigrationTest {

    data class LegacyNoteRow(
        val id: Long,
        val createdAt: Long,
        var addedAt: Long
    )

    private fun applyBackfillLogic(rows: List<LegacyNoteRow>): List<LegacyNoteRow> {
        return rows.map { row ->
            if (row.addedAt <= 0L) {
                row.copy(addedAt = row.createdAt)
            } else {
                row
            }
        }
    }

    @Test
    fun testV12Migration_BackfillsZeroAddedAtWithCreatedAt() {
        // Representative 1.4.0 data where added_at was 0 or unpopulated
        val legacy140Photos = listOf(
            LegacyNoteRow(id = 1L, createdAt = 1600000000000L, addedAt = 0L),
            LegacyNoteRow(id = 2L, createdAt = 1650000000000L, addedAt = -1L),
            LegacyNoteRow(id = 3L, createdAt = 1700000000000L, addedAt = 1710000000000L) // already populated
        )

        val migrated = applyBackfillLogic(legacy140Photos)

        // Photo 1: 0 backfilled with createdAt
        assertEquals(1600000000000L, migrated[0].addedAt)
        // Photo 2: -1 backfilled with createdAt
        assertEquals(1650000000000L, migrated[1].addedAt)
        // Photo 3: already populated preserved intact
        assertEquals(1710000000000L, migrated[2].addedAt)
    }

    @Test
    fun testV12Migration_NeverLeavesZeroOrNullAddedAt() {
        val legacyNotes = listOf(
            LegacyNoteRow(id = 101L, createdAt = 1720000000000L, addedAt = 0L),
            LegacyNoteRow(id = 102L, createdAt = 1725000000000L, addedAt = 0L),
            LegacyNoteRow(id = 103L, createdAt = 1728000000000L, addedAt = 1728000000000L)
        )

        val migrated = applyBackfillLogic(legacyNotes)

        migrated.forEach { note ->
            assertTrue("addedAt must be strictly greater than 0", note.addedAt > 0L)
            assertTrue("addedAt must match valid timestamp", note.addedAt >= note.createdAt)
        }
    }

    @Test
    fun testPhotoCopy_GetsNewAddedAtTimestamp() {
        val originalAddedAt = 1600000000000L
        val original = Photo(
            id = 1L,
            folderId = 10L,
            imageUri = "content://media/1",
            createdAt = originalAddedAt,
            addedAt = originalAddedAt
        )

        val copyTimeMs = originalAddedAt + 5000000L
        val copied = original.copy(
            id = 2L,
            createdAt = copyTimeMs,
            addedAt = copyTimeMs
        )

        assertEquals(originalAddedAt, original.addedAt)
        assertEquals(copyTimeMs, copied.addedAt)
        assertNotEquals(original.addedAt, copied.addedAt)
    }

    @Test
    fun testRestoreFromTrash_PreservesOriginalAddedAt() {
        val originalAddedAt = 1600000000000L
        val trashedAt = 1650000000000L
        val note = TextNote(
            id = 5L,
            folderId = 1L,
            title = "Lecture 3",
            bodyMarkdown = "Formulas",
            createdAt = originalAddedAt,
            addedAt = originalAddedAt,
            isTrashed = true,
            deletedAt = trashedAt
        )

        val restored = note.copy(isTrashed = false, deletedAt = null)
        assertEquals(originalAddedAt, restored.addedAt)
    }

    @Test
    fun testSplitToImages_SetsAddedAtToMomentOfSplitting() {
        val splitTimeMs = 1729000000000L
        val pageCount = 3
        val generatedPhotos = (0 until pageCount).map { i ->
            Photo(
                id = (100 + i).toLong(),
                folderId = 1L,
                imageUri = "file:///storage/split_page_$i.png",
                createdAt = splitTimeMs + i,
                addedAt = splitTimeMs + i
            )
        }

        assertEquals(3, generatedPhotos.size)
        generatedPhotos.forEachIndexed { i, photo ->
            assertEquals(splitTimeMs + i, photo.addedAt)
        }
    }

    @Test
    fun testIndexesDefinitionIntegrity() {
        val expectedIndexes = listOf(
            "idx_photos_added_at" to "photos(added_at)",
            "idx_photo_groups_added_at" to "photo_groups(added_at)",
            "idx_document_notes_added_at" to "document_notes(added_at)",
            "idx_text_notes_added_at" to "text_notes(added_at)",
            "idx_canvas_notes_added_at" to "canvas_notes(added_at)"
        )

        assertEquals(5, expectedIndexes.size)
        expectedIndexes.forEach { (indexName, target) ->
            assertTrue(indexName.startsWith("idx_"))
            assertTrue(indexName.endsWith("_added_at"))
            assertTrue(target.contains("added_at"))
        }
    }
}
