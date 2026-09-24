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
                cover_photo_id INTEGER,
                is_trashed INTEGER NOT NULL DEFAULT 0,
                deleted_at INTEGER,
                FOREIGN KEY(folder_id) REFERENCES folders(id) ON DELETE CASCADE,
                FOREIGN KEY(subfolder_id) REFERENCES subfolders(id) ON DELETE SET NULL
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

        // SQLite FTS4 Virtual Table for Instant Search (compatible with standard Android libsqlite)
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
        const val DATABASE_VERSION = 7
    }
}
