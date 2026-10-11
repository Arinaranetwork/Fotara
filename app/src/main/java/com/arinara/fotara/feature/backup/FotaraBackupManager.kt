// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.backup

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.arinara.fotara.data.db.FotaraDbHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

sealed class BackupScope {
    object EntireApp : BackupScope()
    data class Selective(
        val spaceUuids: Set<String> = emptySet(),
        val workspaceUuids: Set<String> = emptySet()
    ) : BackupScope()
}

data class BackupManifest(
    val format: String = "fotara_archive_v2",
    val version: Int = 2,
    val createdAt: Long = System.currentTimeMillis(),
    val appVersion: String = "1.9.4-alpha",
    val checksumSha256: String,
    val scopeType: String,
    val spaceCount: Int,
    val workspaceCount: Int,
    val folderCount: Int,
    val photoCount: Int,
    val textNoteCount: Int,
    val documentCount: Int,
    val scheduleCount: Int,
    val attachmentCount: Int
)

data class BackupValidationResult(
    val isValid: Boolean,
    val manifest: BackupManifest? = null,
    val errorMessage: String? = null
)

data class BackupRestoreResult(
    val success: Boolean,
    val restoredFolders: Int = 0,
    val restoredNotes: Int = 0,
    val restoredAttachments: Int = 0,
    val errorMessage: String? = null
)

class FotaraBackupManager(private val context: Context) {

    companion object {
        private const val TAG = "FotaraBackupManager"
        const val MANIFEST_ENTRY = "Manifest.json"
        const val MANIFEST_ENTRY_LEGACY = "manifest.json"
        const val SPACES_ENTRY = "Spaces/spaces.json"
        const val WORKSPACES_ENTRY = "Workspaces/workspaces.json"
        const val FOLDERS_ENTRY = "Folders/folders.json"
        const val SUBFOLDERS_ENTRY = "Folders/subfolders.json"
        const val PHOTOS_ENTRY = "Notes/photos.json"
        const val TEXT_NOTES_ENTRY = "Notes/text_notes.json"
        const val DOCUMENT_NOTES_ENTRY = "Notes/document_notes.json"
        const val DOCUMENT_PAGES_ENTRY = "Notes/document_pages.json"
        const val SCHEDULES_ENTRY = "Notes/class_schedules.json"
        const val ATTACHMENTS_DIR_ENTRY = "Attachments/"
        const val ATTACHMENTS_DIR_ENTRY_LEGACY = "attachments/"
        const val DATABASE_ENTRY_LEGACY = "database.json"

        fun computeSha256(data: ByteArray): String {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(data)
            return digest.joinToString("") { "%02x".format(it) }
        }
    }

    suspend fun isDatabaseEmpty(): Boolean = withContext(Dispatchers.IO) {
        try {
            val dbHelper = FotaraDbHelper(context)
            val db = dbHelper.getSafeReadableDatabase()
            val folderCount = queryCount(db, "folders")
            val photoCount = queryCount(db, "photos")
            val textNoteCount = queryCount(db, "text_notes")
            val docCount = queryCount(db, "document_notes")
            (folderCount == 0 && photoCount == 0 && textNoteCount == 0 && docCount == 0)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if database is empty", e)
            false
        }
    }

    private fun queryCount(db: SQLiteDatabase, tableName: String): Int {
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $tableName", null)
        return cursor.use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        }
    }

    suspend fun exportBackup(
        outputStream: OutputStream,
        scope: BackupScope = BackupScope.EntireApp,
        onProgress: (Float) -> Unit = {}
    ): Result<BackupManifest> = withContext(Dispatchers.IO) {
        try {
            onProgress(0.1f)
            val dbHelper = FotaraDbHelper(context)
            val db = dbHelper.getSafeReadableDatabase()

            // 1. Gather Database Records
            val spacesArray = queryTableToJson(db, "spaces")
            val workspacesArray = queryTableToJson(db, "workspaces")
            val foldersArray = queryTableToJson(db, "folders")
            val subfoldersArray = queryTableToJson(db, "subfolders")
            val photosArray = queryTableToJson(db, "photos")
            val textNotesArray = queryTableToJson(db, "text_notes")
            val documentNotesArray = queryTableToJson(db, "document_notes")
            val documentPagesArray = queryTableToJson(db, "document_pages")
            val schedulesArray = queryTableToJson(db, "class_schedules")

            val dbJson = JSONObject().apply {
                put("app", "Fotara")
                put("version", 2)
                put("exportedAt", System.currentTimeMillis())
                put("spaces", spacesArray)
                put("workspaces", workspacesArray)
                put("folders", foldersArray)
                put("subfolders", subfoldersArray)
                put("photos", photosArray)
                put("text_notes", textNotesArray)
                put("document_notes", documentNotesArray)
                put("document_pages", documentPagesArray)
                put("class_schedules", schedulesArray)
            }

            val dbBytes = dbJson.toString(2).toByteArray(Charsets.UTF_8)
            val checksum = computeSha256(dbBytes)

            onProgress(0.3f)

            // 2. Identify Attachments
            val attachmentFiles = mutableListOf<File>()
            val baseDirs = listOf("photos", "thumbnails", "documents", "document_pages")
            for (dirName in baseDirs) {
                val dir = File(context.filesDir, dirName)
                if (dir.exists() && dir.isDirectory) {
                    val files = dir.listFiles()
                    if (files != null) {
                        attachmentFiles.addAll(files.filter { it.isFile })
                    }
                }
            }

            // 3. Build Manifest
            val manifest = BackupManifest(
                format = "fotara_archive_v2",
                version = 2,
                createdAt = System.currentTimeMillis(),
                appVersion = "1.9.4-alpha",
                checksumSha256 = checksum,
                scopeType = if (scope is BackupScope.EntireApp) "ENTIRE_APP" else "SELECTIVE",
                spaceCount = spacesArray.length(),
                workspaceCount = workspacesArray.length(),
                folderCount = foldersArray.length(),
                photoCount = photosArray.length(),
                textNoteCount = textNotesArray.length(),
                documentCount = documentNotesArray.length(),
                scheduleCount = schedulesArray.length(),
                attachmentCount = attachmentFiles.size
            )

            val manifestJson = JSONObject().apply {
                put("format", manifest.format)
                put("version", manifest.version)
                put("createdAt", manifest.createdAt)
                put("appVersion", manifest.appVersion)
                put("checksumSha256", manifest.checksumSha256)
                put("scopeType", manifest.scopeType)
                put("spaceCount", manifest.spaceCount)
                put("workspaceCount", manifest.workspaceCount)
                put("folderCount", manifest.folderCount)
                put("photoCount", manifest.photoCount)
                put("textNoteCount", manifest.textNoteCount)
                put("documentCount", manifest.documentCount)
                put("scheduleCount", manifest.scheduleCount)
                put("attachmentCount", manifest.attachmentCount)
            }
            val manifestBytes = manifestJson.toString(2).toByteArray(Charsets.UTF_8)

            onProgress(0.5f)

            // 4. Write Hierarchical ZIP stream
            val zipOut = ZipOutputStream(outputStream)
            zipOut.use { zos ->
                fun writeJsonEntry(name: String, contentBytes: ByteArray) {
                    zos.putNextEntry(ZipEntry(name))
                    zos.write(contentBytes)
                    zos.closeEntry()
                }

                // Root manifest
                writeJsonEntry(MANIFEST_ENTRY, manifestBytes)

                // Hierarchical data files
                writeJsonEntry(SPACES_ENTRY, spacesArray.toString(2).toByteArray(Charsets.UTF_8))
                writeJsonEntry(WORKSPACES_ENTRY, workspacesArray.toString(2).toByteArray(Charsets.UTF_8))
                writeJsonEntry(FOLDERS_ENTRY, foldersArray.toString(2).toByteArray(Charsets.UTF_8))
                writeJsonEntry(SUBFOLDERS_ENTRY, subfoldersArray.toString(2).toByteArray(Charsets.UTF_8))
                writeJsonEntry(PHOTOS_ENTRY, photosArray.toString(2).toByteArray(Charsets.UTF_8))
                writeJsonEntry(TEXT_NOTES_ENTRY, textNotesArray.toString(2).toByteArray(Charsets.UTF_8))
                writeJsonEntry(DOCUMENT_NOTES_ENTRY, documentNotesArray.toString(2).toByteArray(Charsets.UTF_8))
                writeJsonEntry(DOCUMENT_PAGES_ENTRY, documentPagesArray.toString(2).toByteArray(Charsets.UTF_8))
                writeJsonEntry(SCHEDULES_ENTRY, schedulesArray.toString(2).toByteArray(Charsets.UTF_8))

                // Backward-compatibility bundle
                writeJsonEntry(DATABASE_ENTRY_LEGACY, dbBytes)

                // Write attachments
                val totalAttachments = attachmentFiles.size.coerceAtLeast(1)
                attachmentFiles.forEachIndexed { index, file ->
                    val relativePath = file.relativeTo(context.filesDir).path.replace('\\', '/')
                    zos.putNextEntry(ZipEntry("$ATTACHMENTS_DIR_ENTRY$relativePath"))
                    file.inputStream().use { input ->
                        input.copyTo(zos)
                    }
                    zos.closeEntry()
                    onProgress(0.5f + (0.45f * (index + 1) / totalAttachments))
                }
            }

            onProgress(1.0f)
            Result.success(manifest)
        } catch (e: Exception) {
            Log.e(TAG, "Export failed", e)
            Result.failure(e)
        }
    }

    suspend fun validateBackup(inputStream: InputStream): BackupValidationResult = withContext(Dispatchers.IO) {
        try {
            var manifest: BackupManifest? = null
            var databaseBytes: ByteArray? = null
            var hasHierarchicalData = false

            val zipIn = ZipInputStream(inputStream)
            var entry = zipIn.nextEntry
            while (entry != null) {
                val name = entry.name
                when {
                    name.equals(MANIFEST_ENTRY, ignoreCase = true) || name.equals(MANIFEST_ENTRY_LEGACY, ignoreCase = true) -> {
                        val out = ByteArrayOutputStream()
                        zipIn.copyTo(out)
                        val json = JSONObject(out.toString(Charsets.UTF_8.name()))
                        manifest = BackupManifest(
                            format = json.optString("format", "fotara_archive_v2"),
                            version = json.optInt("version", 2),
                            createdAt = json.optLong("createdAt", 0L),
                            appVersion = json.optString("appVersion", ""),
                            checksumSha256 = json.optString("checksumSha256", ""),
                            scopeType = json.optString("scopeType", "ENTIRE_APP"),
                            spaceCount = json.optInt("spaceCount", 0),
                            workspaceCount = json.optInt("workspaceCount", 0),
                            folderCount = json.optInt("folderCount", 0),
                            photoCount = json.optInt("photoCount", 0),
                            textNoteCount = json.optInt("textNoteCount", 0),
                            documentCount = json.optInt("documentCount", 0),
                            scheduleCount = json.optInt("scheduleCount", 0),
                            attachmentCount = json.optInt("attachmentCount", 0)
                        )
                    }
                    name.equals(DATABASE_ENTRY_LEGACY, ignoreCase = true) -> {
                        val out = ByteArrayOutputStream()
                        zipIn.copyTo(out)
                        databaseBytes = out.toByteArray()
                    }
                    name.startsWith("Folders/") || name.startsWith("Notes/") || name.startsWith("Spaces/") -> {
                        hasHierarchicalData = true
                    }
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }

            if (manifest == null) {
                return@withContext BackupValidationResult(
                    isValid = false,
                    errorMessage = "Missing Manifest.json in archive."
                )
            }

            if (databaseBytes == null && !hasHierarchicalData) {
                return@withContext BackupValidationResult(
                    isValid = false,
                    errorMessage = "Missing structured database files in archive."
                )
            }

            if (databaseBytes != null) {
                val actualChecksum = computeSha256(databaseBytes)
                if (!actualChecksum.equals(manifest.checksumSha256, ignoreCase = true)) {
                    return@withContext BackupValidationResult(
                        isValid = false,
                        manifest = manifest,
                        errorMessage = "Integrity check failed: Checksum mismatch. The file may be corrupted."
                    )
                }
            }

            BackupValidationResult(
                isValid = true,
                manifest = manifest
            )
        } catch (e: Exception) {
            Log.e(TAG, "Validation error", e)
            BackupValidationResult(
                isValid = false,
                errorMessage = "Failed to parse archive: ${e.message}"
            )
        }
    }

    suspend fun restoreBackup(
        inputStream: InputStream,
        onProgress: (Float) -> Unit = {}
    ): BackupRestoreResult = withContext(Dispatchers.IO) {
        try {
            onProgress(0.1f)
            var manifest: BackupManifest? = null
            var databaseBytes: ByteArray? = null
            var restoredAttachmentsCount = 0

            val hierarchicalJsonMap = mutableMapOf<String, String>()

            val zipIn = ZipInputStream(inputStream)
            var entry = zipIn.nextEntry
            while (entry != null) {
                val entryName = entry.name
                when {
                    entryName.equals(MANIFEST_ENTRY, ignoreCase = true) || entryName.equals(MANIFEST_ENTRY_LEGACY, ignoreCase = true) -> {
                        val out = ByteArrayOutputStream()
                        zipIn.copyTo(out)
                        val json = JSONObject(out.toString(Charsets.UTF_8.name()))
                        manifest = BackupManifest(
                            format = json.optString("format", "fotara_archive_v2"),
                            version = json.optInt("version", 2),
                            createdAt = json.optLong("createdAt", 0L),
                            appVersion = json.optString("appVersion", ""),
                            checksumSha256 = json.optString("checksumSha256", ""),
                            scopeType = json.optString("scopeType", "ENTIRE_APP"),
                            spaceCount = json.optInt("spaceCount", 0),
                            workspaceCount = json.optInt("workspaceCount", 0),
                            folderCount = json.optInt("folderCount", 0),
                            photoCount = json.optInt("photoCount", 0),
                            textNoteCount = json.optInt("textNoteCount", 0),
                            documentCount = json.optInt("documentCount", 0),
                            scheduleCount = json.optInt("scheduleCount", 0),
                            attachmentCount = json.optInt("attachmentCount", 0)
                        )
                    }
                    entryName.equals(DATABASE_ENTRY_LEGACY, ignoreCase = true) -> {
                        val out = ByteArrayOutputStream()
                        zipIn.copyTo(out)
                        databaseBytes = out.toByteArray()
                    }
                    entryName.endsWith(".json") && (entryName.startsWith("Spaces/") || entryName.startsWith("Workspaces/") || entryName.startsWith("Folders/") || entryName.startsWith("Notes/")) -> {
                        val out = ByteArrayOutputStream()
                        zipIn.copyTo(out)
                        hierarchicalJsonMap[entryName] = out.toString(Charsets.UTF_8.name())
                    }
                    entryName.startsWith(ATTACHMENTS_DIR_ENTRY, ignoreCase = true) || entryName.startsWith(ATTACHMENTS_DIR_ENTRY_LEGACY, ignoreCase = true) -> {
                        val prefix = if (entryName.startsWith(ATTACHMENTS_DIR_ENTRY, ignoreCase = true)) {
                            entryName.substring(0, ATTACHMENTS_DIR_ENTRY.length)
                        } else {
                            entryName.substring(0, ATTACHMENTS_DIR_ENTRY_LEGACY.length)
                        }
                        val relPath = entryName.removePrefix(prefix)
                        if (relPath.isNotEmpty()) {
                            val targetFile = File(context.filesDir, relPath)
                            targetFile.parentFile?.mkdirs()
                            FileOutputStream(targetFile).use { fos ->
                                zipIn.copyTo(fos)
                            }
                            restoredAttachmentsCount++
                        }
                    }
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }

            onProgress(0.5f)

            if (manifest == null || (databaseBytes == null && hierarchicalJsonMap.isEmpty())) {
                return@withContext BackupRestoreResult(
                    success = false,
                    errorMessage = "Corrupted archive: Missing manifest or database files."
                )
            }

            if (databaseBytes != null) {
                val actualChecksum = computeSha256(databaseBytes)
                if (!actualChecksum.equals(manifest.checksumSha256, ignoreCase = true)) {
                    return@withContext BackupRestoreResult(
                        success = false,
                        errorMessage = "Checksum mismatch: Integrity verification failed."
                    )
                }
            }

            onProgress(0.7f)

            // Insert records into SQLite
            val dbHelper = FotaraDbHelper(context)
            val db = dbHelper.getSafeWritableDatabase()

            var restoredFolders = 0
            var restoredNotes = 0

            db.beginTransaction()
            try {
                if (databaseBytes != null) {
                    val dbJson = JSONObject(databaseBytes.toString(Charsets.UTF_8))
                    restoreTableFromJson(db, "spaces", dbJson.optJSONArray("spaces"), conflictAlgorithm = SQLiteDatabase.CONFLICT_IGNORE)
                    restoreTableFromJson(db, "workspaces", dbJson.optJSONArray("workspaces"), conflictAlgorithm = SQLiteDatabase.CONFLICT_IGNORE)
                    restoredFolders = restoreTableFromJson(db, "folders", dbJson.optJSONArray("folders"), conflictAlgorithm = SQLiteDatabase.CONFLICT_IGNORE)
                    restoreTableFromJson(db, "subfolders", dbJson.optJSONArray("subfolders"), conflictAlgorithm = SQLiteDatabase.CONFLICT_IGNORE)
                    val restoredPhotos = restoreTableFromJson(db, "photos", dbJson.optJSONArray("photos"), conflictAlgorithm = SQLiteDatabase.CONFLICT_IGNORE)
                    val restoredTexts = restoreTableFromJson(db, "text_notes", dbJson.optJSONArray("text_notes"), conflictAlgorithm = SQLiteDatabase.CONFLICT_IGNORE)
                    val restoredDocs = restoreTableFromJson(db, "document_notes", dbJson.optJSONArray("document_notes"), conflictAlgorithm = SQLiteDatabase.CONFLICT_IGNORE)
                    restoreTableFromJson(db, "document_pages", dbJson.optJSONArray("document_pages"), conflictAlgorithm = SQLiteDatabase.CONFLICT_IGNORE)
                    restoreTableFromJson(db, "class_schedules", dbJson.optJSONArray("class_schedules"), conflictAlgorithm = SQLiteDatabase.CONFLICT_IGNORE)
                    restoredNotes = restoredPhotos + restoredTexts + restoredDocs
                } else {
                    fun parseArray(jsonKey: String): JSONArray? = hierarchicalJsonMap[jsonKey]?.let { JSONArray(it) }

                    restoreTableFromJson(db, "spaces", parseArray(SPACES_ENTRY), conflictAlgorithm = SQLiteDatabase.CONFLICT_IGNORE)
                    restoreTableFromJson(db, "workspaces", parseArray(WORKSPACES_ENTRY), conflictAlgorithm = SQLiteDatabase.CONFLICT_IGNORE)
                    restoredFolders = restoreTableFromJson(db, "folders", parseArray(FOLDERS_ENTRY), conflictAlgorithm = SQLiteDatabase.CONFLICT_IGNORE)
                    restoreTableFromJson(db, "subfolders", parseArray(SUBFOLDERS_ENTRY), conflictAlgorithm = SQLiteDatabase.CONFLICT_IGNORE)
                    val restoredPhotos = restoreTableFromJson(db, "photos", parseArray(PHOTOS_ENTRY), conflictAlgorithm = SQLiteDatabase.CONFLICT_IGNORE)
                    val restoredTexts = restoreTableFromJson(db, "text_notes", parseArray(TEXT_NOTES_ENTRY), conflictAlgorithm = SQLiteDatabase.CONFLICT_IGNORE)
                    val restoredDocs = restoreTableFromJson(db, "document_notes", parseArray(DOCUMENT_NOTES_ENTRY), conflictAlgorithm = SQLiteDatabase.CONFLICT_IGNORE)
                    restoreTableFromJson(db, "document_pages", parseArray(DOCUMENT_PAGES_ENTRY), conflictAlgorithm = SQLiteDatabase.CONFLICT_IGNORE)
                    restoreTableFromJson(db, "class_schedules", parseArray(SCHEDULES_ENTRY), conflictAlgorithm = SQLiteDatabase.CONFLICT_IGNORE)
                    restoredNotes = restoredPhotos + restoredTexts + restoredDocs
                }

                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }

            onProgress(1.0f)

            BackupRestoreResult(
                success = true,
                restoredFolders = restoredFolders,
                restoredNotes = restoredNotes,
                restoredAttachments = restoredAttachmentsCount
            )
        } catch (e: Exception) {
            Log.e(TAG, "Restore failed", e)
            BackupRestoreResult(
                success = false,
                errorMessage = e.message ?: "Unknown restore error"
            )
        }
    }

    private fun queryTableToJson(db: SQLiteDatabase, tableName: String): JSONArray {
        val array = JSONArray()
        try {
            val cursor = db.rawQuery("SELECT * FROM $tableName", null)
            cursor.use { c ->
                val columnNames = c.columnNames
                while (c.moveToNext()) {
                    val obj = JSONObject()
                    for (i in columnNames.indices) {
                        val colName = columnNames[i]
                        when (c.getType(i)) {
                            android.database.Cursor.FIELD_TYPE_NULL -> obj.put(colName, JSONObject.NULL)
                            android.database.Cursor.FIELD_TYPE_INTEGER -> obj.put(colName, c.getLong(i))
                            android.database.Cursor.FIELD_TYPE_FLOAT -> obj.put(colName, c.getDouble(i))
                            android.database.Cursor.FIELD_TYPE_STRING -> obj.put(colName, c.getString(i))
                            android.database.Cursor.FIELD_TYPE_BLOB -> {
                                val blob = c.getBlob(i)
                                if (blob != null) {
                                    val hex = blob.joinToString("") { "%02x".format(it) }
                                    obj.put(colName, hex)
                                } else {
                                    obj.put(colName, JSONObject.NULL)
                                }
                            }
                        }
                    }
                    array.put(obj)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Table $tableName might not exist or failed query", e)
        }
        return array
    }

    private fun restoreTableFromJson(
        db: SQLiteDatabase,
        tableName: String,
        array: JSONArray?,
        conflictAlgorithm: Int = SQLiteDatabase.CONFLICT_IGNORE
    ): Int {
        if (array == null || array.length() == 0) return 0
        var insertedCount = 0

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val cv = ContentValues()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (obj.isNull(key)) {
                    cv.putNull(key)
                } else {
                    val value = obj.get(key)
                    when (value) {
                        is String -> cv.put(key, value)
                        is Long -> cv.put(key, value)
                        is Int -> cv.put(key, value)
                        is Double -> cv.put(key, value)
                        is Boolean -> cv.put(key, if (value) 1 else 0)
                        else -> cv.put(key, value.toString())
                    }
                }
            }
            val rowId = db.insertWithOnConflict(tableName, null, cv, conflictAlgorithm)
            if (rowId > 0) insertedCount++
        }
        return insertedCount
    }
}
