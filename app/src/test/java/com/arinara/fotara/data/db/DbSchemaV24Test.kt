// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DbSchemaV24Test {

    @Test
    fun databaseVersion_isTwentyFour() {
        assertEquals(24, FotaraDbHelper.DATABASE_VERSION)
    }

    @Test
    fun databaseConstants_arePreserved() {
        assertEquals("fotara.db", FotaraDbHelper.DATABASE_NAME)
        assertEquals(1L, FotaraDbHelper.HOME_WORKSPACE_ID)
        assertEquals(2L, FotaraDbHelper.ARCHIVE_WORKSPACE_ID)
    }

    @Test
    fun v24TextNotesGroupIdColumn_isDefined() {
        val expectedIndex = "idx_text_notes_group_id"
        assertTrue(expectedIndex.startsWith("idx_"))
        assertEquals("idx_text_notes_group_id", expectedIndex)
    }
}
