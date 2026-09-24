// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.arinara.fotara.data.model.DownsampleQuality
import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.data.model.ImportResult
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.model.PhotoSource
import com.arinara.fotara.data.model.SortOrder
import com.arinara.fotara.data.model.StorageBreakdown
import com.arinara.fotara.data.model.StorageLocation
import com.arinara.fotara.data.model.Subfolder
import com.arinara.fotara.data.model.ThemeMode
import com.arinara.fotara.data.model.UserSettings
import com.arinara.fotara.data.storage.PhotoStorageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

interface SettingsRepository {
    val settingsFlow: StateFlow<UserSettings>
    suspend fun updateSortOrder(sortOrder: SortOrder)
    suspend fun updateGridDensity(density: Int)
    suspend fun updateThemeMode(themeMode: ThemeMode)
    suspend fun updateAutoOcr(enabled: Boolean)
    suspend fun updateOcrLanguage(language: String)
    suspend fun updateDownsampleQuality(quality: DownsampleQuality)
    suspend fun updateReminderLeadTime(hours: Int)
    suspend fun updateDueTomorrowRibbon(enabled: Boolean)
    suspend fun updateStorageLocation(location: StorageLocation)
    suspend fun getStorageBreakdown(): StorageBreakdown
    suspend fun rebuildThumbnails(): Int
    suspend fun rebuildSearchIndex(): Int
    suspend fun exportDataBackup(): String
    suspend fun importDataBackup(jsonString: String): ImportResult
    fun isOnboardingCompleted(): Boolean
    suspend fun setOnboardingCompleted(completed: Boolean)
    fun getRecentSearches(): List<String>
    suspend fun addRecentSearch(query: String)
    suspend fun removeRecentSearch(query: String)
    suspend fun clearRecentSearches()
}

class DefaultSettingsRepository(
    private val context: Context,
    private val folderRepository: FolderRepository,
    private val photoRepository: PhotoRepository,
    private val photoStorageManager: PhotoStorageManager,
    coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : SettingsRepository {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val _settingsFlow = MutableStateFlow(loadSettings())
    override val settingsFlow: StateFlow<UserSettings> = _settingsFlow.asStateFlow()

    private fun loadSettings(): UserSettings {
        return UserSettings(
            defaultSortOrder = SortOrder.fromName(prefs.getString(KEY_SORT_ORDER, SortOrder.UPLOAD_DATE.name)),
            gridDensity = prefs.getInt(KEY_GRID_DENSITY, 3),
            themeMode = ThemeMode.fromName(prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)),
            autoOcrEnabled = prefs.getBoolean(KEY_AUTO_OCR, true),
            ocrLanguage = prefs.getString(KEY_OCR_LANGUAGE, "Latin") ?: "Latin",
            downsampleQuality = DownsampleQuality.fromName(prefs.getString(KEY_DOWNSAMPLE_QUALITY, DownsampleQuality.HIGH_QUALITY.name)),
            reminderLeadTimeHours = prefs.getInt(KEY_REMINDER_LEAD_TIME, 1),
            dueTomorrowRibbonEnabled = prefs.getBoolean(KEY_DUE_TOMORROW_RIBBON, true),
            storageLocation = StorageLocation.fromName(prefs.getString(KEY_STORAGE_LOCATION, StorageLocation.INTERNAL.name))
        )
    }

    override suspend fun updateSortOrder(sortOrder: SortOrder) = withContext(Dispatchers.IO) {
        prefs.edit().putString(KEY_SORT_ORDER, sortOrder.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(defaultSortOrder = sortOrder)
    }

    override suspend fun updateGridDensity(density: Int) = withContext(Dispatchers.IO) {
        val clamped = density.coerceIn(2, 4)
        prefs.edit().putInt(KEY_GRID_DENSITY, clamped).apply()
        _settingsFlow.value = _settingsFlow.value.copy(gridDensity = clamped)
    }

    override suspend fun updateThemeMode(themeMode: ThemeMode) = withContext(Dispatchers.IO) {
        prefs.edit().putString(KEY_THEME_MODE, themeMode.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(themeMode = themeMode)
    }

    override suspend fun updateAutoOcr(enabled: Boolean) = withContext(Dispatchers.IO) {
        prefs.edit().putBoolean(KEY_AUTO_OCR, enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(autoOcrEnabled = enabled)
    }

    override suspend fun updateOcrLanguage(language: String) = withContext(Dispatchers.IO) {
        prefs.edit().putString(KEY_OCR_LANGUAGE, language).apply()
        _settingsFlow.value = _settingsFlow.value.copy(ocrLanguage = language)
    }

    override suspend fun updateDownsampleQuality(quality: DownsampleQuality) = withContext(Dispatchers.IO) {
        prefs.edit().putString(KEY_DOWNSAMPLE_QUALITY, quality.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(downsampleQuality = quality)
    }

    override suspend fun updateReminderLeadTime(hours: Int) = withContext(Dispatchers.IO) {
        prefs.edit().putInt(KEY_REMINDER_LEAD_TIME, hours).apply()
        _settingsFlow.value = _settingsFlow.value.copy(reminderLeadTimeHours = hours)
    }

    override suspend fun updateDueTomorrowRibbon(enabled: Boolean) = withContext(Dispatchers.IO) {
        prefs.edit().putBoolean(KEY_DUE_TOMORROW_RIBBON, enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(dueTomorrowRibbonEnabled = enabled)
    }

    override suspend fun updateStorageLocation(location: StorageLocation) = withContext(Dispatchers.IO) {
        prefs.edit().putString(KEY_STORAGE_LOCATION, location.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(storageLocation = location)
    }

    override suspend fun getStorageBreakdown(): StorageBreakdown = withContext(Dispatchers.IO) {
        photoStorageManager.calculateStorageBreakdown()
    }

    override suspend fun rebuildThumbnails(): Int = withContext(Dispatchers.IO) {
        val photos = photoRepository.getAllActivePhotos()
        var count = 0
        for (photo in photos) {
            val result = photoStorageManager.rebuildThumbnailForFile(photo.fileUri, photo.thumbnailUri)
            if (result != null) count++
        }
        count
    }

    override suspend fun rebuildSearchIndex(): Int = withContext(Dispatchers.IO) {
        photoRepository.rebuildSearchIndex()
    }

    override suspend fun exportDataBackup(): String = withContext(Dispatchers.IO) {
        val backupJson = JSONObject()
        backupJson.put("version", 1)
        backupJson.put("app", "Fotara")
        backupJson.put("exportedAt", System.currentTimeMillis())

        val foldersList = folderRepository.getFolders().first()
        val foldersArray = JSONArray()
        val subfoldersArray = JSONArray()

        for (folder in foldersList) {
            val fObj = JSONObject().apply {
                put("id", folder.id)
                put("name", folder.name)
                put("colorLabel", folder.colorLabel)
                put("isPinned", if (folder.isPinned) 1 else 0)
                put("createdAt", folder.createdAt)
            }
            foldersArray.put(fObj)

            val subs = folderRepository.getSubfolders(folder.id).first()
            for (sub in subs) {
                val sObj = JSONObject().apply {
                    put("id", sub.id)
                    put("folderId", sub.folderId)
                    put("name", sub.name)
                    put("colorLabel", sub.colorLabel)
                    put("createdAt", sub.createdAt)
                }
                subfoldersArray.put(sObj)
            }
        }
        backupJson.put("folders", foldersArray)
        backupJson.put("subfolders", subfoldersArray)

        val photosList = photoRepository.getAllActivePhotos()
        val photosArray = JSONArray()
        for (photo in photosList) {
            val pObj = JSONObject().apply {
                put("id", photo.id)
                put("fileUri", photo.fileUri)
                put("folderId", photo.folderId)
                put("subfolderId", photo.subfolderId)
                put("createdAt", photo.createdAt)
                put("addedAt", photo.addedAt)
                put("tagColor", photo.tagColor)
                put("caption", photo.caption)
                put("ocrText", photo.ocrText)
                put("source", photo.source.name)
                put("linkedDeadline", photo.linkedDeadline)
                put("fileSizeBytes", photo.fileSizeBytes)
                put("note", photo.note)
            }
            photosArray.put(pObj)
        }
        backupJson.put("photos", photosArray)

        backupJson.toString(2)
    }

    override suspend fun importDataBackup(jsonString: String): ImportResult = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            val app = root.optString("app")
            if (app != "Fotara") {
                return@withContext ImportResult(
                    success = false,
                    message = "Invalid backup format: Not a Fotara backup file."
                )
            }

            val foldersArray = root.optJSONArray("folders") ?: JSONArray()
            val subfoldersArray = root.optJSONArray("subfolders") ?: JSONArray()
            val photosArray = root.optJSONArray("photos") ?: JSONArray()

            val existingFolders = folderRepository.getFolders().first()
            val folderIdMap = mutableMapOf<Long, Long>()
            var foldersAdded = 0

            // 1. Import Folders
            for (i in 0 until foldersArray.length()) {
                val fObj = foldersArray.getJSONObject(i)
                val oldId = fObj.getLong("id")
                val name = fObj.getString("name")
                val colorLabel = fObj.optString("colorLabel", "#0B1BE0")

                val matched = existingFolders.firstOrNull { it.name.equals(name, ignoreCase = true) }
                if (matched != null) {
                    folderIdMap[oldId] = matched.id
                } else {
                    val newId = folderRepository.createFolder(name, colorLabel)
                    folderIdMap[oldId] = newId
                    foldersAdded++
                }
            }

            // 2. Import Subfolders
            val subfolderIdMap = mutableMapOf<Long, Long>()
            for (i in 0 until subfoldersArray.length()) {
                val sObj = subfoldersArray.getJSONObject(i)
                val oldSubId = sObj.getLong("id")
                val oldFolderId = sObj.getLong("folderId")
                val subName = sObj.getString("name")
                val subColor = if (sObj.isNull("colorLabel")) null else sObj.getString("colorLabel")

                val targetFolderId = folderIdMap[oldFolderId]
                if (targetFolderId != null) {
                    val existingSubs = folderRepository.getSubfolders(targetFolderId).first()
                    val matchedSub = existingSubs.firstOrNull { it.name.equals(subName, ignoreCase = true) }
                    if (matchedSub != null) {
                        subfolderIdMap[oldSubId] = matchedSub.id
                    } else {
                        val newSubId = folderRepository.createSubfolder(targetFolderId, subName, subColor)
                        subfolderIdMap[oldSubId] = newSubId
                    }
                }
            }

            // 3. Import Photos
            var photosAdded = 0
            for (i in 0 until photosArray.length()) {
                val pObj = photosArray.getJSONObject(i)
                val oldFolderId = pObj.getLong("folderId")
                val targetFolderId = folderIdMap[oldFolderId] ?: continue

                val oldSubId = if (pObj.isNull("subfolderId")) null else pObj.getLong("subfolderId")
                val targetSubId = oldSubId?.let { subfolderIdMap[it] }

                val fileUri = pObj.getString("fileUri")
                val caption = if (pObj.isNull("caption")) null else pObj.getString("caption")
                val ocrText = if (pObj.isNull("ocrText")) null else pObj.getString("ocrText")
                val tagColor = if (pObj.isNull("tagColor")) null else pObj.getString("tagColor")
                val note = if (pObj.isNull("note")) null else pObj.getString("note")
                val sourceName = pObj.optString("source", "IMPORT")
                val source = try { PhotoSource.valueOf(sourceName) } catch (_: Exception) { PhotoSource.IMPORT }
                val linkedDeadline = if (pObj.isNull("linkedDeadline")) null else pObj.getLong("linkedDeadline")
                val fileSizeBytes = pObj.optLong("fileSizeBytes", 0L)
                val createdAt = pObj.optLong("createdAt", System.currentTimeMillis())

                val photo = Photo(
                    fileUri = fileUri,
                    folderId = targetFolderId,
                    subfolderId = targetSubId,
                    createdAt = createdAt,
                    addedAt = System.currentTimeMillis(),
                    tagColor = tagColor,
                    caption = caption,
                    ocrText = ocrText,
                    source = source,
                    linkedDeadline = linkedDeadline,
                    fileSizeBytes = fileSizeBytes,
                    note = note
                )
                photoRepository.addPhoto(photo)
                photosAdded++
            }

            photoRepository.rebuildSearchIndex()

            ImportResult(
                success = true,
                foldersImported = foldersAdded,
                photosImported = photosAdded,
                message = "Import complete: $foldersAdded folders, $photosAdded notes imported."
            )
        } catch (e: Exception) {
            ImportResult(
                success = false,
                message = "Failed to import backup: ${e.message}"
            )
        }
    }

    override fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) = withContext(Dispatchers.IO) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    override fun getRecentSearches(): List<String> {
        val raw = prefs.getString(KEY_RECENT_SEARCHES, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun addRecentSearch(query: String) = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext
        val current = getRecentSearches().toMutableList()
        current.removeAll { it.equals(trimmed, ignoreCase = true) }
        current.add(0, trimmed)
        val max10 = current.take(10)
        val array = JSONArray(max10)
        prefs.edit().putString(KEY_RECENT_SEARCHES, array.toString()).apply()
    }

    override suspend fun removeRecentSearch(query: String) = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        val current = getRecentSearches().toMutableList()
        current.removeAll { it.equals(trimmed, ignoreCase = true) }
        val array = JSONArray(current)
        prefs.edit().putString(KEY_RECENT_SEARCHES, array.toString()).apply()
    }

    override suspend fun clearRecentSearches() = withContext(Dispatchers.IO) {
        prefs.edit().remove(KEY_RECENT_SEARCHES).apply()
    }

    companion object {
        private const val PREFS_NAME = "fotara_settings"
        private const val KEY_SORT_ORDER = "key_sort_order"
        private const val KEY_GRID_DENSITY = "key_grid_density"
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_AUTO_OCR = "key_auto_ocr"
        private const val KEY_OCR_LANGUAGE = "key_ocr_language"
        private const val KEY_DOWNSAMPLE_QUALITY = "key_downsample_quality"
        private const val KEY_REMINDER_LEAD_TIME = "key_reminder_lead_time"
        private const val KEY_DUE_TOMORROW_RIBBON = "key_due_tomorrow_ribbon"
        private const val KEY_STORAGE_LOCATION = "key_storage_location"
        private const val KEY_ONBOARDING_COMPLETED = "key_onboarding_completed"
        private const val KEY_RECENT_SEARCHES = "key_recent_searches"
    }
}
