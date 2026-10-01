// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.model.PhotoSource
import com.arinara.fotara.data.model.Subfolder
import java.io.File

class FotaraDbHelper(private val context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE folders (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                color_label TEXT NOT NULL,
                is_pinned INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL,
                is_trashed INTEGER NOT NULL DEFAULT 0,
                deleted_at INTEGER,
                is_locked INTEGER NOT NULL DEFAULT 0,
                lock_pin TEXT
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE subfolders (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                folder_id INTEGER NOT NULL,
                name TEXT NOT NULL,
                color_label TEXT,
                created_at INTEGER NOT NULL,
                is_trashed INTEGER NOT NULL DEFAULT 0,
                deleted_at INTEGER,
                FOREIGN KEY(folder_id) REFERENCES folders(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE photo_groups (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                folder_id INTEGER NOT NULL,
                subfolder_id INTEGER,
                name TEXT NOT NULL,
                tag_color TEXT,
                created_at INTEGER NOT NULL,
                added_at INTEGER NOT NULL DEFAULT 0,
                cover_photo_id INTEGER,
                is_trashed INTEGER NOT NULL DEFAULT 0,
                deleted_at INTEGER,
                linked_deadline INTEGER,
                scheduled_at INTEGER,
                alert_type TEXT,
                FOREIGN KEY(folder_id) REFERENCES folders(id) ON DELETE CASCADE,
                FOREIGN KEY(subfolder_id) REFERENCES subfolders(id) ON DELETE SET NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE link_groups (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                item_type TEXT NOT NULL,
                member_ids TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE photos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                file_path TEXT NOT NULL,
                thumbnail_path TEXT,
                folder_id INTEGER NOT NULL,
                subfolder_id INTEGER,
                created_at INTEGER NOT NULL,
                added_at INTEGER NOT NULL,
                tag_color TEXT,
                caption TEXT,
                ocr_text TEXT,
                source TEXT NOT NULL,
                linked_deadline INTEGER,
                scheduled_at INTEGER,
                alert_type TEXT,
                file_size_bytes INTEGER NOT NULL DEFAULT 0,
                note TEXT,
                group_id INTEGER,
                tags TEXT,
                is_trashed INTEGER NOT NULL DEFAULT 0,
                deleted_at INTEGER,
                FOREIGN KEY(folder_id) REFERENCES folders(id) ON DELETE CASCADE,
                FOREIGN KEY(subfolder_id) REFERENCES subfolders(id) ON DELETE SET NULL,
                FOREIGN KEY(group_id) REFERENCES photo_groups(id) ON DELETE SET NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE document_notes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                folder_id INTEGER NOT NULL,
                subfolder_id INTEGER,
                name TEXT NOT NULL,
                doc_type TEXT NOT NULL,
                origin_file_uri TEXT NOT NULL,
                extracted_text TEXT,
                created_at INTEGER NOT NULL,
                added_at INTEGER NOT NULL,
                tag_color TEXT,
                linked_deadline INTEGER,
                scheduled_at INTEGER,
                alert_type TEXT,
                is_trashed INTEGER NOT NULL DEFAULT 0,
                deleted_at INTEGER,
                FOREIGN KEY(folder_id) REFERENCES folders(id) ON DELETE CASCADE,
                FOREIGN KEY(subfolder_id) REFERENCES subfolders(id) ON DELETE SET NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE document_pages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                document_note_id INTEGER NOT NULL,
                page_index INTEGER NOT NULL,
                image_uri TEXT NOT NULL,
                ocr_text TEXT,
                FOREIGN KEY(document_note_id) REFERENCES document_notes(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE text_notes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                folder_id INTEGER NOT NULL,
                subfolder_id INTEGER,
                title TEXT NOT NULL,
                body_markdown TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                added_at INTEGER NOT NULL,
                tag_color TEXT,
                linked_deadline INTEGER,
                scheduled_at INTEGER,
                alert_type TEXT,
                is_trashed INTEGER NOT NULL DEFAULT 0,
                deleted_at INTEGER,
                FOREIGN KEY(folder_id) REFERENCES folders(id) ON DELETE CASCADE,
                FOREIGN KEY(subfolder_id) REFERENCES subfolders(id) ON DELETE SET NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS canvas_notes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                folder_id INTEGER NOT NULL,
                subfolder_id INTEGER,
                title TEXT NOT NULL,
                data_blob BLOB,
                thumbnail_path TEXT,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                added_at INTEGER NOT NULL,
                tag_color TEXT,
                linked_deadline INTEGER,
                scheduled_at INTEGER,
                alert_type TEXT,
                is_trashed INTEGER NOT NULL DEFAULT 0,
                deleted_at INTEGER,
                FOREIGN KEY(folder_id) REFERENCES folders(id) ON DELETE CASCADE,
                FOREIGN KEY(subfolder_id) REFERENCES subfolders(id) ON DELETE SET NULL
            )
            """.trimIndent()
        )

        // SQLite FTS4 Virtual Table for Instant Search (compatible with standard Android libsqlite)
        createFtsTable(db)

        // High-performance B-tree indexes on added_at columns for instant date filtering & widget queries
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_photos_added_at ON photos(added_at)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_photo_groups_added_at ON photo_groups(added_at)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_document_notes_added_at ON document_notes(added_at)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_text_notes_added_at ON text_notes(added_at)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_canvas_notes_added_at ON canvas_notes(added_at)")
    }

    private fun createFtsTable(db: SQLiteDatabase) {
        try {
            db.execSQL(
                """
                CREATE VIRTUAL TABLE IF NOT EXISTS photos_fts USING fts4(
                    photo_id,
                    folder_name,
                    subfolder_name,
                    caption,
                    ocr_text,
                    note
                )
                """.trimIndent()
            )
        } catch (e: Exception) {
            android.util.Log.w("FotaraDbHelper", "FTS4 virtual table initialization fallback: ${e.message}")
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            try {
                db.execSQL("DROP TABLE IF EXISTS photos_fts")
            } catch (_: Exception) {}
            createFtsTable(db)
        }
        if (oldVersion < 3) {
            try {
                db.execSQL("ALTER TABLE photos ADD COLUMN note TEXT")
            } catch (_: Exception) {}
            try {
                db.execSQL("DROP TABLE IF EXISTS photos_fts")
            } catch (_: Exception) {}
            createFtsTable(db)
        }
        if (oldVersion < 4) {
            try {
                db.execSQL("ALTER TABLE folders ADD COLUMN is_trashed INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE folders ADD COLUMN deleted_at INTEGER")
            } catch (_: Exception) {}
            try {
                db.execSQL("ALTER TABLE subfolders ADD COLUMN is_trashed INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE subfolders ADD COLUMN deleted_at INTEGER")
            } catch (_: Exception) {}
            try {
                db.execSQL("ALTER TABLE photos ADD COLUMN is_trashed INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE photos ADD COLUMN deleted_at INTEGER")
            } catch (_: Exception) {}
        }
        if (oldVersion < 5) {
            try {
                db.execSQL("ALTER TABLE folders ADD COLUMN is_locked INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE folders ADD COLUMN lock_pin TEXT")
            } catch (_: Exception) {}
        }
        if (oldVersion < 6) {
            try {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS photo_groups (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        folder_id INTEGER NOT NULL,
                        subfolder_id INTEGER,
                        name TEXT NOT NULL,
                        tag_color TEXT,
                        created_at INTEGER NOT NULL,
                        cover_photo_id INTEGER,
                        is_trashed INTEGER NOT NULL DEFAULT 0,
                        deleted_at INTEGER,
                        FOREIGN KEY(folder_id) REFERENCES folders(id) ON DELETE CASCADE,
                        FOREIGN KEY(subfolder_id) REFERENCES subfolders(id) ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("ALTER TABLE photos ADD COLUMN group_id INTEGER")
            } catch (_: Exception) {}
        }
        if (oldVersion < 7) {
            try {
                db.execSQL("ALTER TABLE photos ADD COLUMN tags TEXT")
            } catch (_: Exception) {}
        }
        if (oldVersion < 8) {
            try {
                db.execSQL("ALTER TABLE photo_groups ADD COLUMN linked_deadline INTEGER")
            } catch (_: Exception) {}
            try {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS link_groups (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        item_type TEXT NOT NULL,
                        member_ids TEXT NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            } catch (_: Exception) {}
        }
        if (oldVersion < 9) {
            try {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS document_notes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        folder_id INTEGER NOT NULL,
                        subfolder_id INTEGER,
                        name TEXT NOT NULL,
                        doc_type TEXT NOT NULL,
                        origin_file_uri TEXT NOT NULL,
                        extracted_text TEXT,
                        created_at INTEGER NOT NULL,
                        added_at INTEGER NOT NULL,
                        tag_color TEXT,
                        linked_deadline INTEGER,
                        is_trashed INTEGER NOT NULL DEFAULT 0,
                        deleted_at INTEGER,
                        FOREIGN KEY(folder_id) REFERENCES folders(id) ON DELETE CASCADE,
                        FOREIGN KEY(subfolder_id) REFERENCES subfolders(id) ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS document_pages (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        document_note_id INTEGER NOT NULL,
                        page_index INTEGER NOT NULL,
                        image_uri TEXT NOT NULL,
                        ocr_text TEXT,
                        FOREIGN KEY(document_note_id) REFERENCES document_notes(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
            } catch (_: Exception) {}
        }
        if (oldVersion < 10) {
            try {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS text_notes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        folder_id INTEGER NOT NULL,
                        subfolder_id INTEGER,
                        title TEXT NOT NULL,
                        body_markdown TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        added_at INTEGER NOT NULL,
                        tag_color TEXT,
                        linked_deadline INTEGER,
                        is_trashed INTEGER NOT NULL DEFAULT 0,
                        deleted_at INTEGER,
                        FOREIGN KEY(folder_id) REFERENCES folders(id) ON DELETE CASCADE,
                        FOREIGN KEY(subfolder_id) REFERENCES subfolders(id) ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
            } catch (_: Exception) {}
        }
        if (oldVersion < 11) {
            try {
                db.execSQL("ALTER TABLE photo_groups ADD COLUMN added_at INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE photo_groups SET added_at = created_at WHERE added_at = 0")
            } catch (_: Exception) {}
            try {
                db.execSQL("ALTER TABLE photos ADD COLUMN scheduled_at INTEGER")
                db.execSQL("ALTER TABLE photos ADD COLUMN alert_type TEXT")
            } catch (_: Exception) {}
            try {
                db.execSQL("ALTER TABLE photo_groups ADD COLUMN scheduled_at INTEGER")
                db.execSQL("ALTER TABLE photo_groups ADD COLUMN alert_type TEXT")
            } catch (_: Exception) {}
            try {
                db.execSQL("ALTER TABLE document_notes ADD COLUMN scheduled_at INTEGER")
                db.execSQL("ALTER TABLE document_notes ADD COLUMN alert_type TEXT")
            } catch (_: Exception) {}
            try {
                db.execSQL("ALTER TABLE text_notes ADD COLUMN scheduled_at INTEGER")
                db.execSQL("ALTER TABLE text_notes ADD COLUMN alert_type TEXT")
            } catch (_: Exception) {}
            try {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS canvas_notes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        folder_id INTEGER NOT NULL,
                        subfolder_id INTEGER,
                        title TEXT NOT NULL,
                        data_blob BLOB,
                        thumbnail_path TEXT,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        added_at INTEGER NOT NULL,
                        tag_color TEXT,
                        linked_deadline INTEGER,
                        scheduled_at INTEGER,
                        alert_type TEXT,
                        is_trashed INTEGER NOT NULL DEFAULT 0,
                        deleted_at INTEGER,
                        FOREIGN KEY(folder_id) REFERENCES folders(id) ON DELETE CASCADE,
                        FOREIGN KEY(subfolder_id) REFERENCES subfolders(id) ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
            } catch (_: Exception) {}
        }
        if (oldVersion < 12) {
            try {
                // 1. Backfill any 0 or null added_at timestamps using created_at
                db.execSQL("UPDATE photos SET added_at = created_at WHERE added_at <= 0")
                db.execSQL("UPDATE photo_groups SET added_at = created_at WHERE added_at <= 0")
                db.execSQL("UPDATE document_notes SET added_at = created_at WHERE added_at <= 0")
                db.execSQL("UPDATE text_notes SET added_at = created_at WHERE added_at <= 0")
                db.execSQL("UPDATE canvas_notes SET added_at = created_at WHERE added_at <= 0")

                // 2. High-performance B-tree indexes on added_at
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_photos_added_at ON photos(added_at)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_photo_groups_added_at ON photo_groups(added_at)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_document_notes_added_at ON document_notes(added_at)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_text_notes_added_at ON text_notes(added_at)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_canvas_notes_added_at ON canvas_notes(added_at)")
            } catch (e: Exception) {
                android.util.Log.e("FotaraDbHelper", "Migration v12 failed: ${e.message}")
            }
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        try {
            db.setForeignKeyConstraintsEnabled(true)
        } catch (_: Exception) {}
    }

    fun getSafeReadableDatabase(): SQLiteDatabase {
        return try {
            readableDatabase
        } catch (e: Exception) {
            android.util.Log.e("FotaraDbHelper", "Unreadable database encountered, resetting...", e)
            try {
                context.deleteDatabase(DATABASE_NAME)
            } catch (_: Exception) {}
            readableDatabase
        }
    }

    fun getSafeWritableDatabase(): SQLiteDatabase {
        return try {
            writableDatabase
        } catch (e: Exception) {
            android.util.Log.e("FotaraDbHelper", "Unwritable database encountered, resetting...", e)
            try {
                context.deleteDatabase(DATABASE_NAME)
            } catch (_: Exception) {}
            writableDatabase
        }
    }

    companion object {
        const val DATABASE_NAME = "fotara.db"
        const val DATABASE_VERSION = 12
    }

    /**
     * Shared query across all non-trashed note types added within [startTime..endTime].
     * Utilizes B-tree indexes on added_at columns for zero-scan offline performance.
     * Reused by search date filters and the Today home widget.
     */
    fun getNotesAddedBetween(startTime: Long, endTime: Long): List<NoteAddedSummary> {
        val results = mutableListOf<NoteAddedSummary>()
        val db = getSafeReadableDatabase()
        val args = arrayOf(startTime.toString(), endTime.toString())

        // 1. Photos (standalone only, not part of a group)
        try {
            db.rawQuery(
                """
                SELECT id, caption, folder_id, subfolder_id, added_at, tag_color, file_path, thumbnail_path
                FROM photos
                WHERE is_trashed = 0 AND group_id IS NULL AND added_at BETWEEN ? AND ?
                ORDER BY added_at DESC
                """.trimIndent(),
                args
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    results.add(
                        NoteAddedSummary(
                            id = cursor.getLong(0),
                            type = NoteType.PHOTO,
                            title = cursor.getString(1) ?: "Photo Note",
                            folderId = cursor.getLong(2),
                            subfolderId = if (cursor.isNull(3)) null else cursor.getLong(3),
                            addedAt = cursor.getLong(4),
                            tagColor = cursor.getString(5),
                            fileUriOrThumbnail = cursor.getString(7) ?: cursor.getString(6)
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        // 2. Photo Groups
        try {
            db.rawQuery(
                """
                SELECT id, name, folder_id, subfolder_id, added_at, tag_color
                FROM photo_groups
                WHERE is_trashed = 0 AND added_at BETWEEN ? AND ?
                ORDER BY added_at DESC
                """.trimIndent(),
                args
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    results.add(
                        NoteAddedSummary(
                            id = cursor.getLong(0),
                            type = NoteType.GROUP,
                            title = cursor.getString(1),
                            folderId = cursor.getLong(2),
                            subfolderId = if (cursor.isNull(3)) null else cursor.getLong(3),
                            addedAt = cursor.getLong(4),
                            tagColor = cursor.getString(5),
                            fileUriOrThumbnail = null
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        // 3. Document Notes
        try {
            db.rawQuery(
                """
                SELECT id, name, folder_id, subfolder_id, added_at, tag_color, origin_file_uri
                FROM document_notes
                WHERE is_trashed = 0 AND added_at BETWEEN ? AND ?
                ORDER BY added_at DESC
                """.trimIndent(),
                args
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    results.add(
                        NoteAddedSummary(
                            id = cursor.getLong(0),
                            type = NoteType.DOCUMENT,
                            title = cursor.getString(1),
                            folderId = cursor.getLong(2),
                            subfolderId = if (cursor.isNull(3)) null else cursor.getLong(3),
                            addedAt = cursor.getLong(4),
                            tagColor = cursor.getString(5),
                            fileUriOrThumbnail = cursor.getString(6)
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        // 4. Text Notes
        try {
            db.rawQuery(
                """
                SELECT id, title, folder_id, subfolder_id, added_at, tag_color
                FROM text_notes
                WHERE is_trashed = 0 AND added_at BETWEEN ? AND ?
                ORDER BY added_at DESC
                """.trimIndent(),
                args
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    results.add(
                        NoteAddedSummary(
                            id = cursor.getLong(0),
                            type = NoteType.TEXT,
                            title = cursor.getString(1),
                            folderId = cursor.getLong(2),
                            subfolderId = if (cursor.isNull(3)) null else cursor.getLong(3),
                            addedAt = cursor.getLong(4),
                            tagColor = cursor.getString(5),
                            fileUriOrThumbnail = null
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        // 5. Canvas Notes
        try {
            db.rawQuery(
                """
                SELECT id, title, folder_id, subfolder_id, added_at, tag_color, thumbnail_path
                FROM canvas_notes
                WHERE is_trashed = 0 AND added_at BETWEEN ? AND ?
                ORDER BY added_at DESC
                """.trimIndent(),
                args
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    results.add(
                        NoteAddedSummary(
                            id = cursor.getLong(0),
                            type = NoteType.CANVAS,
                            title = cursor.getString(1),
                            folderId = cursor.getLong(2),
                            subfolderId = if (cursor.isNull(3)) null else cursor.getLong(3),
                            addedAt = cursor.getLong(4),
                            tagColor = cursor.getString(5),
                            fileUriOrThumbnail = cursor.getString(6)
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        return results.sortedByDescending { it.addedAt }
    }
}

enum class NoteType {
    PHOTO,
    GROUP,
    DOCUMENT,
    TEXT,
    CANVAS
}

data class NoteAddedSummary(
    val id: Long,
    val type: NoteType,
    val title: String,
    val folderId: Long,
    val subfolderId: Long?,
    val addedAt: Long,
    val tagColor: String? = null,
    val fileUriOrThumbnail: String? = null
)

