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

class FotaraDbHelper(val context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS workspaces (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                uuid TEXT NOT NULL UNIQUE,
                kind TEXT NOT NULL,
                name TEXT NOT NULL,
                position INTEGER NOT NULL,
                created_at INTEGER NOT NULL,
                icon_key TEXT
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_workspaces_position ON workspaces(position)")

        val now = System.currentTimeMillis()
        db.execSQL(
            "INSERT OR IGNORE INTO workspaces (id, uuid, kind, name, position, created_at) VALUES (1, '$HOME_WORKSPACE_UUID', 'HOME', '', 0, $now)"
        )
        db.execSQL(
            "INSERT OR IGNORE INTO workspaces (id, uuid, kind, name, position, created_at) VALUES (2, '$ARCHIVE_WORKSPACE_UUID', 'ARCHIVE', '', 1, $now)"
        )

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
                lock_pin TEXT,
                workspace_id INTEGER NOT NULL DEFAULT 1
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_folders_workspace_id ON folders(workspace_id)")

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
                schedule_title TEXT,
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
                schedule_title TEXT,
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
                schedule_title TEXT,
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
                schedule_title TEXT,
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
                schedule_title TEXT,
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

        // High-performance B-tree indexes on scheduled_at columns for alarm scheduling, widgets & reminders
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_photos_scheduled_at ON photos(scheduled_at)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_photo_groups_scheduled_at ON photo_groups(scheduled_at)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_document_notes_scheduled_at ON document_notes(scheduled_at)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_text_notes_scheduled_at ON text_notes(scheduled_at)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_canvas_notes_scheduled_at ON canvas_notes(scheduled_at)")

        // v14 Infinite Canvas schema: layers, chunked elements, and assets
        db.execSQL(
            """
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
        )

        db.execSQL(
            """
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
        )

        db.execSQL(
            """
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
        )

        db.execSQL("CREATE INDEX IF NOT EXISTS idx_canvas_layers_canvas_id ON canvas_layers(canvas_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_canvas_elements_canvas_id ON canvas_elements(canvas_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_canvas_elements_layer_id ON canvas_elements(layer_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_canvas_assets_canvas_id ON canvas_assets(canvas_id)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS photo_drawings (
                photo_id INTEGER PRIMARY KEY,
                data BLOB NOT NULL,
                width_px INTEGER NOT NULL,
                height_px INTEGER NOT NULL,
                is_visible INTEGER NOT NULL DEFAULT 1,
                updated_at INTEGER NOT NULL,
                FOREIGN KEY(photo_id) REFERENCES photos(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS pdf_page_pins (
                document_id INTEGER NOT NULL,
                page_index INTEGER NOT NULL,
                pinned_at INTEGER NOT NULL,
                PRIMARY KEY (document_id, page_index),
                FOREIGN KEY(document_id) REFERENCES document_notes(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_pdf_page_pins_doc_id ON pdf_page_pins(document_id)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS pdf_page_drawings (
                document_id INTEGER NOT NULL,
                page_index INTEGER NOT NULL,
                data BLOB NOT NULL,
                page_width REAL NOT NULL,
                page_height REAL NOT NULL,
                is_visible INTEGER NOT NULL DEFAULT 1,
                updated_at INTEGER NOT NULL,
                PRIMARY KEY (document_id, page_index),
                FOREIGN KEY(document_id) REFERENCES document_notes(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_pdf_page_drawings_doc_id ON pdf_page_drawings(document_id)")

        // v19 Class Schedules table for Phase 42 Timetable & Dynamic Capsule
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS class_schedules (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                day_of_week INTEGER NOT NULL,
                start_minute INTEGER NOT NULL,
                end_minute INTEGER NOT NULL,
                subject_name TEXT NOT NULL,
                room_name TEXT,
                instructor_name TEXT,
                linked_folder_id INTEGER,
                color_hex TEXT,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                FOREIGN KEY(linked_folder_id) REFERENCES folders(id) ON DELETE SET NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_class_schedules_day ON class_schedules(day_of_week, start_minute)")

        createFtsTable(db)
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
            db.execSQL(
                """
                CREATE VIRTUAL TABLE IF NOT EXISTS document_notes_fts USING fts4(
                    document_id,
                    folder_name,
                    subfolder_name,
                    name,
                    doc_type,
                    content_text
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
        if (oldVersion < 13) {
            try {
                // 1. Add schedule_title column to all 5 note tables
                db.execSQL("ALTER TABLE photos ADD COLUMN schedule_title TEXT")
                db.execSQL("ALTER TABLE photo_groups ADD COLUMN schedule_title TEXT")
                db.execSQL("ALTER TABLE document_notes ADD COLUMN schedule_title TEXT")
                db.execSQL("ALTER TABLE text_notes ADD COLUMN schedule_title TEXT")
                db.execSQL("ALTER TABLE canvas_notes ADD COLUMN schedule_title TEXT")

                // 2. High-performance B-tree indexes on scheduled_at
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_photos_scheduled_at ON photos(scheduled_at)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_photo_groups_scheduled_at ON photo_groups(scheduled_at)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_document_notes_scheduled_at ON document_notes(scheduled_at)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_text_notes_scheduled_at ON text_notes(scheduled_at)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_canvas_notes_scheduled_at ON canvas_notes(scheduled_at)")
            } catch (e: Exception) {
                android.util.Log.e("FotaraDbHelper", "Migration v13 failed: ${e.message}")
            }
        }
        if (oldVersion < 14) {
            try {
                db.execSQL(
                    """
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
                )

                db.execSQL(
                    """
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
                )

                db.execSQL(
                    """
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
                )

                db.execSQL("CREATE INDEX IF NOT EXISTS idx_canvas_layers_canvas_id ON canvas_layers(canvas_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_canvas_elements_canvas_id ON canvas_elements(canvas_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_canvas_elements_layer_id ON canvas_elements(layer_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_canvas_assets_canvas_id ON canvas_assets(canvas_id)")
            } catch (e: Exception) {
                android.util.Log.e("FotaraDbHelper", "Migration v14 failed: ${e.message}")
            }
        }
        if (oldVersion < 15) {
            try {
                db.execSQL(
                    """
                    CREATE VIRTUAL TABLE IF NOT EXISTS document_notes_fts USING fts4(
                        document_id,
                        folder_name,
                        subfolder_name,
                        name,
                        doc_type,
                        content_text
                    )
                    """.trimIndent()
                )
            } catch (e: Exception) {
                android.util.Log.e("FotaraDbHelper", "Migration v15 failed: ${e.message}")
            }
        }
        if (oldVersion < 16) {
            try {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS workspaces (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        uuid TEXT NOT NULL UNIQUE,
                        kind TEXT NOT NULL,
                        name TEXT NOT NULL,
                        position INTEGER NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_workspaces_position ON workspaces(position)")

                val now = System.currentTimeMillis()
                db.execSQL(
                    "INSERT OR IGNORE INTO workspaces (id, uuid, kind, name, position, created_at) VALUES (1, '$HOME_WORKSPACE_UUID', 'HOME', '', 0, $now)"
                )
                db.execSQL(
                    "INSERT OR IGNORE INTO workspaces (id, uuid, kind, name, position, created_at) VALUES (2, '$ARCHIVE_WORKSPACE_UUID', 'ARCHIVE', '', 1, $now)"
                )

                db.execSQL("ALTER TABLE folders ADD COLUMN workspace_id INTEGER NOT NULL DEFAULT 1")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_folders_workspace_id ON folders(workspace_id)")
                db.execSQL("UPDATE folders SET workspace_id = 1 WHERE workspace_id IS NULL OR workspace_id <= 0")
            } catch (e: Exception) {
                android.util.Log.e("FotaraDbHelper", "Migration v16 failed: ${e.message}")
            }
        }
        if (oldVersion < 17) {
            try {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS photo_drawings (
                        photo_id INTEGER PRIMARY KEY,
                        data BLOB NOT NULL,
                        width_px INTEGER NOT NULL,
                        height_px INTEGER NOT NULL,
                        is_visible INTEGER NOT NULL DEFAULT 1,
                        updated_at INTEGER NOT NULL,
                        FOREIGN KEY(photo_id) REFERENCES photos(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
            } catch (e: Exception) {
                android.util.Log.e("FotaraDbHelper", "Migration v17 failed: ${e.message}")
            }
        }
        if (oldVersion < 18) {
            try {
                db.execSQL("ALTER TABLE workspaces ADD COLUMN icon_key TEXT")
            } catch (e: Exception) {
                android.util.Log.e("FotaraDbHelper", "Migration v18 (icon_key) failed: ${e.message}")
            }
            try {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS pdf_page_pins (
                        document_id INTEGER NOT NULL,
                        page_index INTEGER NOT NULL,
                        pinned_at INTEGER NOT NULL,
                        PRIMARY KEY (document_id, page_index),
                        FOREIGN KEY(document_id) REFERENCES document_notes(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_pdf_page_pins_doc_id ON pdf_page_pins(document_id)")
            } catch (e: Exception) {
                android.util.Log.e("FotaraDbHelper", "Migration v18 (pdf_page_pins) failed: ${e.message}")
            }
            try {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS pdf_page_drawings (
                        document_id INTEGER NOT NULL,
                        page_index INTEGER NOT NULL,
                        data BLOB NOT NULL,
                        page_width REAL NOT NULL,
                        page_height REAL NOT NULL,
                        is_visible INTEGER NOT NULL DEFAULT 1,
                        updated_at INTEGER NOT NULL,
                        PRIMARY KEY (document_id, page_index),
                        FOREIGN KEY(document_id) REFERENCES document_notes(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_pdf_page_drawings_doc_id ON pdf_page_drawings(document_id)")
            } catch (e: Exception) {
                android.util.Log.e("FotaraDbHelper", "Migration v18 (pdf_page_drawings) failed: ${e.message}")
            }
            try {
                db.execSQL("DELETE FROM pdf_page_pins WHERE document_id NOT IN (SELECT id FROM document_notes)")
                db.execSQL("DELETE FROM pdf_page_drawings WHERE document_id NOT IN (SELECT id FROM document_notes)")
            } catch (_: Exception) {}
        }
        if (oldVersion < 19) {
            try {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS class_schedules (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        day_of_week INTEGER NOT NULL,
                        start_minute INTEGER NOT NULL,
                        end_minute INTEGER NOT NULL,
                        subject_name TEXT NOT NULL,
                        room_name TEXT,
                        instructor_name TEXT,
                        linked_folder_id INTEGER,
                        color_hex TEXT,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        FOREIGN KEY(linked_folder_id) REFERENCES folders(id) ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_class_schedules_day ON class_schedules(day_of_week, start_minute)")
            } catch (e: Exception) {
                android.util.Log.e("FotaraDbHelper", "Migration v19 (class_schedules) failed: ${e.message}")
            }
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        try {
            db.setForeignKeyConstraintsEnabled(true)
        } catch (_: Exception) {}
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        try {
            db.execSQL("DELETE FROM pdf_page_pins WHERE document_id NOT IN (SELECT id FROM document_notes)")
            db.execSQL("DELETE FROM pdf_page_drawings WHERE document_id NOT IN (SELECT id FROM document_notes)")
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
        const val DATABASE_VERSION = 19
        const val HOME_WORKSPACE_ID = 1L
        const val ARCHIVE_WORKSPACE_ID = 2L
        const val HOME_WORKSPACE_UUID = "00000000-0000-4000-8000-000000000001"
        const val ARCHIVE_WORKSPACE_UUID = "00000000-0000-4000-8000-000000000002"
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

    /**
     * Shared query across all non-trashed note types with upcoming scheduled alerts.
     * Utilizes B-tree indexes on scheduled_at columns.
     * Feeds AlarmManager reboot re-arming, the Due Tomorrow widget, and the Today widget.
     */
    fun getUpcomingSchedules(nowMs: Long = System.currentTimeMillis()): List<NoteScheduleSummary> {
        val results = mutableListOf<NoteScheduleSummary>()
        val db = getSafeReadableDatabase()
        val args = arrayOf(nowMs.toString())

        // 1. Photos
        try {
            db.rawQuery(
                """
                SELECT id, caption, folder_id, subfolder_id, scheduled_at, alert_type, schedule_title, tag_color, file_path
                FROM photos
                WHERE is_trashed = 0 AND scheduled_at IS NOT NULL AND scheduled_at > ?
                ORDER BY scheduled_at ASC
                """.trimIndent(),
                args
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    results.add(
                        NoteScheduleSummary(
                            id = cursor.getLong(0),
                            type = NoteType.PHOTO,
                            title = if (cursor.isNull(1) || cursor.getString(1).isBlank()) "Photo Note" else cursor.getString(1),
                            folderId = cursor.getLong(2),
                            subfolderId = if (cursor.isNull(3)) null else cursor.getLong(3),
                            scheduledAt = cursor.getLong(4),
                            alertType = cursor.getString(5) ?: "NOTIFICATION",
                            scheduleTitle = cursor.getString(6),
                            tagColor = cursor.getString(7),
                            fileUriOrThumbnail = cursor.getString(8)
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        // 2. Photo Groups
        try {
            db.rawQuery(
                """
                SELECT id, name, folder_id, subfolder_id, scheduled_at, alert_type, schedule_title, tag_color
                FROM photo_groups
                WHERE is_trashed = 0 AND scheduled_at IS NOT NULL AND scheduled_at > ?
                ORDER BY scheduled_at ASC
                """.trimIndent(),
                args
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    results.add(
                        NoteScheduleSummary(
                            id = cursor.getLong(0),
                            type = NoteType.GROUP,
                            title = cursor.getString(1),
                            folderId = cursor.getLong(2),
                            subfolderId = if (cursor.isNull(3)) null else cursor.getLong(3),
                            scheduledAt = cursor.getLong(4),
                            alertType = cursor.getString(5) ?: "NOTIFICATION",
                            scheduleTitle = cursor.getString(6),
                            tagColor = cursor.getString(7)
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        // 3. Document Notes
        try {
            db.rawQuery(
                """
                SELECT id, name, folder_id, subfolder_id, scheduled_at, alert_type, schedule_title, tag_color, origin_file_uri
                FROM document_notes
                WHERE is_trashed = 0 AND scheduled_at IS NOT NULL AND scheduled_at > ?
                ORDER BY scheduled_at ASC
                """.trimIndent(),
                args
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    results.add(
                        NoteScheduleSummary(
                            id = cursor.getLong(0),
                            type = NoteType.DOCUMENT,
                            title = cursor.getString(1),
                            folderId = cursor.getLong(2),
                            subfolderId = if (cursor.isNull(3)) null else cursor.getLong(3),
                            scheduledAt = cursor.getLong(4),
                            alertType = cursor.getString(5) ?: "NOTIFICATION",
                            scheduleTitle = cursor.getString(6),
                            tagColor = cursor.getString(7),
                            fileUriOrThumbnail = cursor.getString(8)
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        // 4. Text Notes
        try {
            db.rawQuery(
                """
                SELECT id, title, folder_id, subfolder_id, scheduled_at, alert_type, schedule_title, tag_color
                FROM text_notes
                WHERE is_trashed = 0 AND scheduled_at IS NOT NULL AND scheduled_at > ?
                ORDER BY scheduled_at ASC
                """.trimIndent(),
                args
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    results.add(
                        NoteScheduleSummary(
                            id = cursor.getLong(0),
                            type = NoteType.TEXT,
                            title = cursor.getString(1),
                            folderId = cursor.getLong(2),
                            subfolderId = if (cursor.isNull(3)) null else cursor.getLong(3),
                            scheduledAt = cursor.getLong(4),
                            alertType = cursor.getString(5) ?: "NOTIFICATION",
                            scheduleTitle = cursor.getString(6),
                            tagColor = cursor.getString(7)
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        // 5. Canvas Notes
        try {
            db.rawQuery(
                """
                SELECT id, title, folder_id, subfolder_id, scheduled_at, alert_type, schedule_title, tag_color, thumbnail_path
                FROM canvas_notes
                WHERE is_trashed = 0 AND scheduled_at IS NOT NULL AND scheduled_at > ?
                ORDER BY scheduled_at ASC
                """.trimIndent(),
                args
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    results.add(
                        NoteScheduleSummary(
                            id = cursor.getLong(0),
                            type = NoteType.CANVAS,
                            title = cursor.getString(1),
                            folderId = cursor.getLong(2),
                            subfolderId = if (cursor.isNull(3)) null else cursor.getLong(3),
                            scheduledAt = cursor.getLong(4),
                            alertType = cursor.getString(5) ?: "NOTIFICATION",
                            scheduleTitle = cursor.getString(6),
                            tagColor = cursor.getString(7),
                            fileUriOrThumbnail = cursor.getString(8)
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        return results.sortedBy { it.scheduledAt }
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

data class NoteScheduleSummary(
    val id: Long,
    val type: NoteType,
    val title: String,
    val folderId: Long,
    val subfolderId: Long?,
    val scheduledAt: Long,
    val alertType: String,
    val scheduleTitle: String?,
    val tagColor: String? = null,
    val fileUriOrThumbnail: String? = null
) {
    val displayScheduleTitle: String
        get() = scheduleTitle?.ifBlank { null } ?: title
}

