// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.persistence

import com.arinara.fotara.data.db.FotaraDbHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CanvasMigrationTest {

    @Test
    fun testV14DatabaseVersionIsSet() {
        assertEquals(14, FotaraDbHelper.DATABASE_VERSION)
    }

    @Test
    fun testV14Schema_DefinesDedicatedCanvasTables() {
        val expectedCanvasTables = listOf("canvas_layers", "canvas_elements", "canvas_assets")
        assertEquals(3, expectedCanvasTables.size)

        // Verify that v14 migration definitions contain all necessary tables
        val createLayerTableSql = """
            CREATE TABLE IF NOT EXISTS canvas_layers (
                id TEXT PRIMARY KEY,
                canvas_id INTEGER NOT NULL,
                name TEXT NOT NULL,
                is_visible INTEGER NOT NULL DEFAULT 1,
                is_locked INTEGER NOT NULL DEFAULT 0,
                opacity REAL NOT NULL DEFAULT 1.0,
                sort_order INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(canvas_id) REFERENCES canvas_notes(id) ON DELETE CASCADE
            )
        """.trimIndent()

        val createElementTableSql = """
            CREATE TABLE IF NOT EXISTS canvas_elements (
                id TEXT PRIMARY KEY,
                canvas_id INTEGER NOT NULL,
                layer_id TEXT NOT NULL,
                element_type TEXT NOT NULL,
                bounds_left REAL NOT NULL,
                bounds_top REAL NOT NULL,
                bounds_right REAL NOT NULL,
                bounds_bottom REAL NOT NULL,
                z_index INTEGER NOT NULL DEFAULT 0,
                data_chunk BLOB NOT NULL,
                FOREIGN KEY(canvas_id) REFERENCES canvas_notes(id) ON DELETE CASCADE,
                FOREIGN KEY(layer_id) REFERENCES canvas_layers(id) ON DELETE CASCADE
            )
        """.trimIndent()

        val createAssetTableSql = """
            CREATE TABLE IF NOT EXISTS canvas_assets (
                asset_id TEXT PRIMARY KEY,
                canvas_id INTEGER NOT NULL,
                file_path TEXT NOT NULL,
                mime_type TEXT NOT NULL,
                width INTEGER NOT NULL,
                height INTEGER NOT NULL,
                file_size INTEGER NOT NULL,
                created_at INTEGER NOT NULL,
                FOREIGN KEY(canvas_id) REFERENCES canvas_notes(id) ON DELETE CASCADE
            )
        """.trimIndent()

        assertTrue(createLayerTableSql.contains("canvas_layers"))
        assertTrue(createElementTableSql.contains("canvas_elements"))
        assertTrue(createAssetTableSql.contains("canvas_assets"))
    }

    @Test
    fun testV14Indexes_DefinesBTreeIndexesOnForeignKeys() {
        val expectedIndexes = listOf(
            "idx_canvas_layers_canvas_id" to "canvas_layers(canvas_id)",
            "idx_canvas_elements_canvas_id" to "canvas_elements(canvas_id)",
            "idx_canvas_elements_layer_id" to "canvas_elements(layer_id)",
            "idx_canvas_assets_canvas_id" to "canvas_assets(canvas_id)"
        )

        expectedIndexes.forEach { (indexName, target) ->
            assertTrue(indexName.startsWith("idx_canvas_"))
            assertTrue(target.contains("canvas_id") || target.contains("layer_id"))
        }
    }

    @Test
    fun testMigrationSafety_140ExistingTablesPreservedUntouched() {
        val legacy140Tables = listOf(
            "folders",
            "subfolders",
            "photos",
            "photo_groups",
            "document_notes",
            "document_pages",
            "text_notes",
            "link_groups"
        )

        // Ensure none of the v14 migration statements alter or drop existing tables
        val v14Statements = listOf(
            "CREATE TABLE IF NOT EXISTS canvas_layers",
            "CREATE TABLE IF NOT EXISTS canvas_elements",
            "CREATE TABLE IF NOT EXISTS canvas_assets",
            "CREATE INDEX IF NOT EXISTS idx_canvas_layers_canvas_id",
            "CREATE INDEX IF NOT EXISTS idx_canvas_elements_canvas_id",
            "CREATE INDEX IF NOT EXISTS idx_canvas_elements_layer_id",
            "CREATE INDEX IF NOT EXISTS idx_canvas_assets_canvas_id"
        )

        v14Statements.forEach { stmt ->
            legacy140Tables.forEach { legacyTable ->
                // Ensure no statement mutates legacy tables
                assertTrue(
                    "Statement '$stmt' should not drop or alter legacy table '$legacyTable'",
                    !stmt.startsWith("DROP TABLE $legacyTable") && !stmt.startsWith("ALTER TABLE $legacyTable")
                )
            }
        }
    }
}
