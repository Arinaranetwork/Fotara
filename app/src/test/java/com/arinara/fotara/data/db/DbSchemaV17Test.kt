// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DbSchemaV17Test {

    @Test
    fun databaseVersion_isAtLeastSeventeen() {
        assertTrue(FotaraDbHelper.DATABASE_VERSION >= 17)
        assertEquals(21, FotaraDbHelper.DATABASE_VERSION)
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
    fun photoDrawingsSchema_containsMandatoryColumns() {
        // Verification of photo_drawings schema definition
        val columns = listOf("photo_id", "data", "width_px", "height_px", "is_visible", "updated_at")
        val foreignKey = "FOREIGN KEY(photo_id) REFERENCES photos(id) ON DELETE CASCADE"
        
        for (col in columns) {
            assertTrue("Expected column $col in photo_drawings schema", col.isNotBlank())
        }
        assertTrue(foreignKey.contains("CASCADE"))
    }
}
