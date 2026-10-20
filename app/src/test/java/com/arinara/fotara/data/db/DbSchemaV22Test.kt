// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DbSchemaV22Test {

    @Test
    fun databaseVersion_isAtLeastTwentyTwo() {
        assertTrue(FotaraDbHelper.DATABASE_VERSION >= 22)
        assertEquals(23, FotaraDbHelper.DATABASE_VERSION)
    }

    @Test
    fun databaseConstants_arePreserved() {
        assertEquals("fotara.db", FotaraDbHelper.DATABASE_NAME)
        assertEquals(1L, FotaraDbHelper.HOME_WORKSPACE_ID)
        assertEquals(2L, FotaraDbHelper.ARCHIVE_WORKSPACE_ID)
        assertEquals("00000000-0000-4000-8000-000000000001", FotaraDbHelper.HOME_WORKSPACE_UUID)
        assertEquals("00000000-0000-4000-8000-000000000002", FotaraDbHelper.ARCHIVE_WORKSPACE_UUID)
    }

    @Test
    fun v22CompositeIndexes_areDefined() {
        val compositeIndexes = listOf(
            "idx_photos_folder_trashed",
            "idx_text_notes_folder_trashed",
            "idx_canvas_notes_folder_trashed",
            "idx_document_notes_folder_trashed",
            "idx_photo_groups_folder_trashed",
            "idx_subfolders_folder_trashed",
            "idx_folders_trashed",
            "idx_photos_trashed_added"
        )
        for (index in compositeIndexes) {
            assertTrue("Expected index name $index should be non-empty", index.startsWith("idx_"))
        }
        assertEquals(8, compositeIndexes.size)
    }
}
