// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import com.arinara.fotara.data.model.DownsampleQuality
import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.data.model.ImportResult
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.model.PhotoSource
import com.arinara.fotara.data.model.ProfileBorders
import com.arinara.fotara.data.model.SortOrder
import com.arinara.fotara.data.model.StorageBreakdown
import com.arinara.fotara.data.model.StorageLocation
import com.arinara.fotara.data.model.Subfolder
import com.arinara.fotara.data.model.ThemeMode
import com.arinara.fotara.data.model.UserProfile
import com.arinara.fotara.data.model.UserSettings
import com.arinara.fotara.data.storage.PhotoStorageManager
import com.arinara.fotara.util.ProfileImageUtils
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import com.arinara.fotara.data.db.FotaraDbHelper
import com.arinara.fotara.data.model.Workspace
import com.arinara.fotara.data.model.WorkspaceKind
import org.json.JSONArray
import org.json.JSONObject

interface SettingsRepository {
    companion object {
        const val DEVICE_COUNT_DEFAULT_ENABLED = true
        const val KEY_DEVICE_COUNT_ENABLED = "key_device_count_enabled"
        const val MAX_BANNER_GIF_BYTES = 8 * 1024 * 1024L // 8 MB
        const val KEY_SAVED_IMAGE_LOCATION = "key_saved_image_location"
        const val KEY_SAVED_IMAGE_CUSTOM_NAME = "key_saved_image_custom_name"
        const val DEFAULT_SAVED_IMAGE_LOCATION = "pictures_fotara"
        const val DEFAULT_SAVED_IMAGE_CUSTOM_NAME = ""
    }
    val settingsFlow: StateFlow<UserSettings>
    val profileFlow: StateFlow<UserProfile>
    suspend fun updateDeviceCountEnabled(enabled: Boolean)
    suspend fun updateProfileName(name: String)
    suspend fun updateProfileEmail(email: String)
    suspend fun updateProfileBorder(borderId: String)
    suspend fun saveProfileAvatar(bitmap: Bitmap): String?
    suspend fun removeProfileAvatar()
    suspend fun saveProfileBanner(bitmap: Bitmap): String?
    suspend fun saveProfileBannerGif(bytes: ByteArray, crop: String?): String?
    suspend fun removeProfileBanner()
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
    fun getRecentDestinations(): List<com.arinara.fotara.data.model.RecentDestination>
    suspend fun addRecentDestination(destination: com.arinara.fotara.data.model.RecentDestination)
    suspend fun clearRecentDestinations()
    suspend fun updateAutoCheckUpdates(enabled: Boolean)
    suspend fun updateOptInCrashReporting(enabled: Boolean)
    suspend fun updateCombineFileNamePreset(preset: String)
    suspend fun updateSavedImageLocation(locationKey: String, customName: String)
}

class DefaultSettingsRepository(
    private val context: Context,
    private val folderRepository: FolderRepository,
    private val photoRepository: PhotoRepository,
    private val photoStorageManager: PhotoStorageManager,
    private val workspaceRepository: WorkspaceRepository? = null,
    private val documentRepositoryProvider: (() -> DocumentRepository?)? = null,
    coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : SettingsRepository {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val profileDir: File by lazy {
        File(context.filesDir, "profile").apply { if (!exists()) mkdirs() }
    }

    private val _settingsFlow = MutableStateFlow(loadSettings())
    override val settingsFlow: StateFlow<UserSettings> = _settingsFlow.asStateFlow()

    private val _profileFlow = MutableStateFlow(loadProfile())
    override val profileFlow: StateFlow<UserProfile> = _profileFlow.asStateFlow()

    private fun loadProfile(): UserProfile {
        val rawAvatar = prefs.getString(KEY_PROFILE_AVATAR_PATH, null)
        val resolvedAvatar = if (rawAvatar != null && File(rawAvatar).exists()) {
            rawAvatar
        } else if (File(profileDir, "avatar.png").exists()) {
            File(profileDir, "avatar.png").absolutePath
        } else if (File(profileDir, "avatar.webp").exists()) {
            File(profileDir, "avatar.webp").absolutePath
        } else {
            null
        }

        val rawBanner = prefs.getString(KEY_PROFILE_BANNER_PATH, null)
        val resolvedBanner = if (rawBanner != null && File(rawBanner).exists()) {
            rawBanner
        } else if (File(profileDir, "banner.gif").exists()) {
            File(profileDir, "banner.gif").absolutePath
        } else if (File(profileDir, "banner.png").exists()) {
            File(profileDir, "banner.png").absolutePath
        } else if (File(profileDir, "banner.webp").exists()) {
            File(profileDir, "banner.webp").absolutePath
        } else {
            null
        }

        // Border feature is temporarily disabled: past users with equipped borders are turned to none
        val savedBorderId = prefs.getString(KEY_PROFILE_BORDER_ID, null)
        if (savedBorderId != null && savedBorderId != ProfileBorders.NONE_ID) {
            prefs.edit().putString(KEY_PROFILE_BORDER_ID, ProfileBorders.NONE_ID).apply()
        }

        return UserProfile(
            name = prefs.getString(KEY_PROFILE_NAME, "") ?: "",
            email = prefs.getString(KEY_PROFILE_EMAIL, "") ?: "",
            avatarPath = resolvedAvatar,
            bannerPath = resolvedBanner,
            borderId = ProfileBorders.NONE_ID,
            avatarUpdatedAt = prefs.getLong(KEY_PROFILE_AVATAR_UPDATED_AT, 0L),
            bannerUpdatedAt = prefs.getLong(KEY_PROFILE_BANNER_UPDATED_AT, 0L),
            bannerCrop = prefs.getString(KEY_PROFILE_BANNER_CROP, null)
        )
    }

    private fun loadSettings(): UserSettings {
        val rawTheme = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        val resolvedTheme = ThemeMode.fromName(rawTheme)
        if (rawTheme.equals("LIGHT", ignoreCase = true)) {
            prefs.edit().putString(KEY_THEME_MODE, ThemeMode.SYSTEM.name).apply()
        }
        return UserSettings(
            defaultSortOrder = SortOrder.fromName(prefs.getString(KEY_SORT_ORDER, SortOrder.UPLOAD_DATE.name)),
            gridDensity = prefs.getInt(KEY_GRID_DENSITY, 3),
            themeMode = resolvedTheme,
            autoOcrEnabled = prefs.getBoolean(KEY_AUTO_OCR, true),
            ocrLanguage = prefs.getString(KEY_OCR_LANGUAGE, "Latin") ?: "Latin",
            downsampleQuality = DownsampleQuality.fromName(prefs.getString(KEY_DOWNSAMPLE_QUALITY, DownsampleQuality.HIGH_QUALITY.name)),
            reminderLeadTimeHours = prefs.getInt(KEY_REMINDER_LEAD_TIME, 1),
            dueTomorrowRibbonEnabled = prefs.getBoolean(KEY_DUE_TOMORROW_RIBBON, true),
            storageLocation = StorageLocation.fromName(prefs.getString(KEY_STORAGE_LOCATION, StorageLocation.INTERNAL.name)),
            autoCheckUpdates = prefs.getBoolean(KEY_AUTO_CHECK_UPDATES, true),
            optInCrashReporting = prefs.getBoolean(KEY_OPT_IN_CRASH_REPORTING, false),
            combineFileNamePreset = prefs.getString(KEY_COMBINE_NAME_PRESET, "{folder}_{date}") ?: "{folder}_{date}",
            isDeviceCountEnabled = prefs.getBoolean(KEY_DEVICE_COUNT_ENABLED, DEVICE_COUNT_DEFAULT_ENABLED),
            savedImageLocation = prefs.getString(KEY_SAVED_IMAGE_LOCATION, DEFAULT_SAVED_IMAGE_LOCATION) ?: DEFAULT_SAVED_IMAGE_LOCATION,
            savedImageCustomName = prefs.getString(KEY_SAVED_IMAGE_CUSTOM_NAME, DEFAULT_SAVED_IMAGE_CUSTOM_NAME) ?: DEFAULT_SAVED_IMAGE_CUSTOM_NAME
        )
    }

    override suspend fun updateSavedImageLocation(locationKey: String, customName: String) = withContext(Dispatchers.IO) {
        val sanitizedCustom = customName.trim().take(30)
        prefs.edit()
            .putString(KEY_SAVED_IMAGE_LOCATION, locationKey)
            .putString(KEY_SAVED_IMAGE_CUSTOM_NAME, sanitizedCustom)
            .apply()
        _settingsFlow.value = _settingsFlow.value.copy(
            savedImageLocation = locationKey,
            savedImageCustomName = sanitizedCustom
        )
    }

    override suspend fun updateDeviceCountEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        prefs.edit().putBoolean(KEY_DEVICE_COUNT_ENABLED, enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(isDeviceCountEnabled = enabled)
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
        val photoCount = photoRepository.rebuildSearchIndex()
        val docCount = documentRepositoryProvider?.invoke()?.rebuildSearchIndex() ?: 0
        photoCount + docCount
    }

    override suspend fun exportDataBackup(): String = withContext(Dispatchers.IO) {
        val backupJson = JSONObject()
        backupJson.put("version", 2)
        backupJson.put("app", "Fotara")
        backupJson.put("exportedAt", System.currentTimeMillis())

        val workspaces = workspaceRepository?.getWorkspacesSync() ?: emptyList()
        val workspacesArray = JSONArray()
        val workspaceUuidMap = mutableMapOf<Long, String>()
        for (ws in workspaces) {
            val wObj = JSONObject().apply {
                put("uuid", ws.uuid)
                put("kind", ws.kind.name)
                put("name", ws.name)
                put("position", ws.position)
                ws.iconKey?.let { put("iconKey", it) }
            }
            workspacesArray.put(wObj)
            workspaceUuidMap[ws.id] = ws.uuid
        }
        backupJson.put("workspaces", workspacesArray)

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
                put("workspaceUuid", workspaceUuidMap[folder.workspaceId] ?: FotaraDbHelper.HOME_WORKSPACE_UUID)
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

            val workspacesArray = root.optJSONArray("workspaces")
            val currentWorkspaces = workspaceRepository?.getWorkspacesSync() ?: emptyList()
            val homeWs = currentWorkspaces.firstOrNull { it.kind == WorkspaceKind.HOME }
                ?: Workspace(id = 1L, uuid = FotaraDbHelper.HOME_WORKSPACE_UUID, kind = WorkspaceKind.HOME)
            val archiveWs = currentWorkspaces.firstOrNull { it.kind == WorkspaceKind.ARCHIVE }
            val archiveTargetId = archiveWs?.id ?: homeWs.id

            val workspaceUuidToIdMap = mutableMapOf<String, Long>()
            workspaceUuidToIdMap[FotaraDbHelper.HOME_WORKSPACE_UUID] = homeWs.id
            workspaceUuidToIdMap[FotaraDbHelper.ARCHIVE_WORKSPACE_UUID] = archiveTargetId

            for (ws in currentWorkspaces) {
                workspaceUuidToIdMap[ws.uuid] = ws.id
            }

            if (workspacesArray != null && workspaceRepository != null) {
                var currentCustomCount = currentWorkspaces.count { it.kind == WorkspaceKind.CUSTOM }
                for (i in 0 until workspacesArray.length()) {
                    val wObj = workspacesArray.getJSONObject(i)
                    val wUuid = wObj.getString("uuid")
                    val wKind = wObj.optString("kind", "CUSTOM")
                    val wName = wObj.optString("name", "")

                    val rawIconKey = if (wObj.has("iconKey") && !wObj.isNull("iconKey")) {
                        wObj.getString("iconKey")
                    } else null
                    val validIconKey = if (rawIconKey != null && com.arinara.fotara.data.model.WorkspaceIcons.isValidKey(rawIconKey)) {
                        rawIconKey
                    } else if (rawIconKey != null) {
                        com.arinara.fotara.data.model.WorkspaceIcons.DEFAULT_KEY
                    } else null

                    if (wKind == "HOME") {
                        workspaceUuidToIdMap[wUuid] = homeWs.id
                    } else if (wKind == "ARCHIVE") {
                        workspaceUuidToIdMap[wUuid] = archiveTargetId
                    } else if (!workspaceUuidToIdMap.containsKey(wUuid)) {
                        if (currentCustomCount < WorkspaceValidator.MAX_CUSTOM_WORKSPACES) {
                            val res = workspaceRepository.createWorkspace(wName, validIconKey)
                            if (res is WorkspaceResult.Success) {
                                workspaceUuidToIdMap[wUuid] = res.data.id
                                currentCustomCount++
                            } else {
                                workspaceUuidToIdMap[wUuid] = homeWs.id
                            }
                        } else {
                            workspaceUuidToIdMap[wUuid] = homeWs.id
                        }
                    }
                }
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
                val wsUuid = fObj.optString("workspaceUuid", "")
                val targetWsId = workspaceUuidToIdMap[wsUuid] ?: homeWs.id

                val matched = existingFolders.firstOrNull { it.name.equals(name, ignoreCase = true) }
                if (matched != null) {
                    folderIdMap[oldId] = matched.id
                } else {
                    val newId = folderRepository.createFolder(name, colorLabel, isPinned = false, workspaceId = targetWsId)
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

    override fun getRecentDestinations(): List<com.arinara.fotara.data.model.RecentDestination> {
        val raw = prefs.getString(KEY_RECENT_DESTINATIONS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<com.arinara.fotara.data.model.RecentDestination>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    com.arinara.fotara.data.model.RecentDestination(
                        type = com.arinara.fotara.data.model.DestinationType.valueOf(obj.getString("type")),
                        folderId = obj.getLong("folderId"),
                        subfolderId = if (obj.isNull("subfolderId")) null else obj.getLong("subfolderId"),
                        groupId = if (obj.isNull("groupId")) null else obj.getLong("groupId"),
                        title = obj.getString("title"),
                        subtitle = if (obj.isNull("subtitle")) null else obj.getString("subtitle"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun addRecentDestination(destination: com.arinara.fotara.data.model.RecentDestination) = withContext(Dispatchers.IO) {
        val current = getRecentDestinations().toMutableList()
        current.removeAll {
            it.type == destination.type &&
            it.folderId == destination.folderId &&
            it.subfolderId == destination.subfolderId &&
            it.groupId == destination.groupId
        }
        current.add(0, destination)
        val max5 = current.take(5)
        val array = JSONArray()
        for (item in max5) {
            val obj = JSONObject().apply {
                put("type", item.type.name)
                put("folderId", item.folderId)
                if (item.subfolderId != null) put("subfolderId", item.subfolderId) else put("subfolderId", JSONObject.NULL)
                if (item.groupId != null) put("groupId", item.groupId) else put("groupId", JSONObject.NULL)
                put("title", item.title)
                if (item.subtitle != null) put("subtitle", item.subtitle) else put("subtitle", JSONObject.NULL)
                put("timestamp", item.timestamp)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_RECENT_DESTINATIONS, array.toString()).apply()
    }

    override suspend fun clearRecentDestinations() = withContext(Dispatchers.IO) {
        prefs.edit().remove(KEY_RECENT_DESTINATIONS).apply()
    }

    override suspend fun updateAutoCheckUpdates(enabled: Boolean) = withContext(Dispatchers.IO) {
        prefs.edit().putBoolean(KEY_AUTO_CHECK_UPDATES, enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(autoCheckUpdates = enabled)
    }

    override suspend fun updateOptInCrashReporting(enabled: Boolean) = withContext(Dispatchers.IO) {
        prefs.edit().putBoolean(KEY_OPT_IN_CRASH_REPORTING, enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(optInCrashReporting = enabled)
    }

    override suspend fun updateCombineFileNamePreset(preset: String) = withContext(Dispatchers.IO) {
        prefs.edit().putString(KEY_COMBINE_NAME_PRESET, preset).apply()
        _settingsFlow.value = _settingsFlow.value.copy(combineFileNamePreset = preset)
    }

    override suspend fun updateProfileName(name: String) = withContext(Dispatchers.IO) {
        val trimmed = name.trim().take(30)
        prefs.edit().putString(KEY_PROFILE_NAME, trimmed).apply()
        _profileFlow.value = _profileFlow.value.copy(name = trimmed)
    }

    override suspend fun updateProfileEmail(email: String) = withContext(Dispatchers.IO) {
        val trimmed = email.trim().take(60)
        prefs.edit().putString(KEY_PROFILE_EMAIL, trimmed).apply()
        _profileFlow.value = _profileFlow.value.copy(email = trimmed)
    }

    override suspend fun updateProfileBorder(borderId: String) = withContext(Dispatchers.IO) {
        // Border feature is temporarily disabled: always persist and emit NONE_ID
        prefs.edit().putString(KEY_PROFILE_BORDER_ID, ProfileBorders.NONE_ID).apply()
        _profileFlow.value = _profileFlow.value.copy(borderId = ProfileBorders.NONE_ID)
    }

    override suspend fun saveProfileAvatar(bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        val targetFile = File(profileDir, "avatar.png")
        val legacyFile = File(profileDir, "avatar.webp")
        val success = ProfileImageUtils.savePngAtomically(
            bitmap = bitmap,
            targetFile = targetFile,
            targetWidth = 512,
            targetHeight = 512
        )
        if (success) {
            if (legacyFile.exists()) legacyFile.delete()
            val path = targetFile.absolutePath
            val now = System.currentTimeMillis()
            prefs.edit()
                .putString(KEY_PROFILE_AVATAR_PATH, path)
                .putLong(KEY_PROFILE_AVATAR_UPDATED_AT, now)
                .apply()
            _profileFlow.value = _profileFlow.value.copy(
                avatarPath = path,
                avatarUpdatedAt = now
            )
            path
        } else {
            null
        }
    }

    override suspend fun removeProfileAvatar() = withContext(Dispatchers.IO) {
        val targetFile = File(profileDir, "avatar.png")
        val legacyFile = File(profileDir, "avatar.webp")
        if (targetFile.exists()) targetFile.delete()
        if (legacyFile.exists()) legacyFile.delete()
        val now = System.currentTimeMillis()
        prefs.edit()
            .remove(KEY_PROFILE_AVATAR_PATH)
            .putLong(KEY_PROFILE_AVATAR_UPDATED_AT, now)
            .apply()
        _profileFlow.value = _profileFlow.value.copy(
            avatarPath = null,
            avatarUpdatedAt = now
        )
    }

    override suspend fun saveProfileBanner(bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        val targetFile = File(profileDir, "banner.png")
        val legacyFile = File(profileDir, "banner.webp")
        val gifFile = File(profileDir, "banner.gif")
        val success = ProfileImageUtils.savePngAtomically(
            bitmap = bitmap,
            targetFile = targetFile,
            targetWidth = 1080
        )
        if (success) {
            if (legacyFile.exists()) legacyFile.delete()
            if (gifFile.exists()) gifFile.delete()
            val path = targetFile.absolutePath
            val now = System.currentTimeMillis()
            prefs.edit()
                .putString(KEY_PROFILE_BANNER_PATH, path)
                .putLong(KEY_PROFILE_BANNER_UPDATED_AT, now)
                .remove(KEY_PROFILE_BANNER_CROP)
                .apply()
            _profileFlow.value = _profileFlow.value.copy(
                bannerPath = path,
                bannerUpdatedAt = now,
                bannerCrop = null
            )
            path
        } else {
            null
        }
    }

    override suspend fun saveProfileBannerGif(bytes: ByteArray, crop: String?): String? = withContext(Dispatchers.IO) {
        if (bytes.size > MAX_BANNER_GIF_BYTES) return@withContext null
        if (!com.arinara.fotara.util.ImageFormatDetector.isGif(bytes)) return@withContext null

        val targetFile = File(profileDir, "banner.gif")
        val pngFile = File(profileDir, "banner.png")
        val webpFile = File(profileDir, "banner.webp")
        val tempFile = File(profileDir, "banner.gif.tmp_${System.currentTimeMillis()}")

        try {
            FileOutputStream(tempFile).use { out ->
                out.write(bytes)
                out.flush()
            }
            if (!tempFile.exists() || tempFile.length() == 0L) {
                if (tempFile.exists()) tempFile.delete()
                return@withContext null
            }

            val replaced = if (targetFile.exists()) {
                val backupFile = File(profileDir, "banner.gif.bak")
                if (backupFile.exists()) backupFile.delete()
                targetFile.renameTo(backupFile)
                if (tempFile.renameTo(targetFile)) {
                    backupFile.delete()
                    true
                } else {
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
                    backupFile.delete()
                    true
                }
            } else {
                tempFile.renameTo(targetFile) || {
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
                    true
                }()
            }

            if (replaced && targetFile.exists() && targetFile.length() > 0L) {
                if (pngFile.exists()) pngFile.delete()
                if (webpFile.exists()) webpFile.delete()
                val path = targetFile.absolutePath
                val now = System.currentTimeMillis()
                val editor = prefs.edit()
                    .putString(KEY_PROFILE_BANNER_PATH, path)
                    .putLong(KEY_PROFILE_BANNER_UPDATED_AT, now)
                if (crop != null) {
                    editor.putString(KEY_PROFILE_BANNER_CROP, crop)
                } else {
                    editor.remove(KEY_PROFILE_BANNER_CROP)
                }
                editor.apply()
                _profileFlow.value = _profileFlow.value.copy(
                    bannerPath = path,
                    bannerUpdatedAt = now,
                    bannerCrop = crop
                )
                path
            } else {
                if (tempFile.exists()) tempFile.delete()
                null
            }
        } catch (_: Exception) {
            if (tempFile.exists()) tempFile.delete()
            null
        }
    }

    override suspend fun removeProfileBanner() = withContext(Dispatchers.IO) {
        val targetFile = File(profileDir, "banner.png")
        val legacyFile = File(profileDir, "banner.webp")
        val gifFile = File(profileDir, "banner.gif")
        if (targetFile.exists()) targetFile.delete()
        if (legacyFile.exists()) legacyFile.delete()
        if (gifFile.exists()) gifFile.delete()
        val now = System.currentTimeMillis()
        prefs.edit()
            .remove(KEY_PROFILE_BANNER_PATH)
            .remove(KEY_PROFILE_BANNER_CROP)
            .putLong(KEY_PROFILE_BANNER_UPDATED_AT, now)
            .apply()
        _profileFlow.value = _profileFlow.value.copy(
            bannerPath = null,
            bannerUpdatedAt = now,
            bannerCrop = null
        )
    }

    companion object {
        const val KEY_PROFILE_BANNER_CROP = "key_profile_banner_crop"
        const val MAX_BANNER_GIF_BYTES = 8 * 1024 * 1024L // 8 MB
        private const val PREFS_NAME = "fotara_settings"
        private const val KEY_DEVICE_COUNT_ENABLED = SettingsRepository.KEY_DEVICE_COUNT_ENABLED
        private const val DEVICE_COUNT_DEFAULT_ENABLED = SettingsRepository.DEVICE_COUNT_DEFAULT_ENABLED
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
        private const val KEY_RECENT_DESTINATIONS = "key_recent_destinations"
        private const val KEY_AUTO_CHECK_UPDATES = "key_auto_check_updates"
        private const val KEY_OPT_IN_CRASH_REPORTING = "key_opt_in_crash_reporting"
        private const val KEY_COMBINE_NAME_PRESET = "key_combine_name_preset"
        private const val KEY_PROFILE_NAME = "key_profile_name"
        private const val KEY_PROFILE_EMAIL = "key_profile_email"
        private const val KEY_PROFILE_AVATAR_PATH = "key_profile_avatar_path"
        private const val KEY_PROFILE_AVATAR_UPDATED_AT = "key_profile_avatar_updated_at"
        private const val KEY_PROFILE_BANNER_PATH = "key_profile_banner_path"
        private const val KEY_PROFILE_BANNER_UPDATED_AT = "key_profile_banner_updated_at"
        private const val KEY_PROFILE_BORDER_ID = "key_profile_border_id"
        private const val KEY_SAVED_IMAGE_LOCATION = SettingsRepository.KEY_SAVED_IMAGE_LOCATION
        private const val KEY_SAVED_IMAGE_CUSTOM_NAME = SettingsRepository.KEY_SAVED_IMAGE_CUSTOM_NAME
        private const val DEFAULT_SAVED_IMAGE_LOCATION = SettingsRepository.DEFAULT_SAVED_IMAGE_LOCATION
        private const val DEFAULT_SAVED_IMAGE_CUSTOM_NAME = SettingsRepository.DEFAULT_SAVED_IMAGE_CUSTOM_NAME
    }
}
