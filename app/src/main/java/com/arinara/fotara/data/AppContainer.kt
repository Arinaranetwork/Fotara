// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data

import android.content.Context
import com.arinara.fotara.data.db.FotaraDbHelper
import com.arinara.fotara.data.repository.FolderRepository
import com.arinara.fotara.data.repository.PhotoRepository
import com.arinara.fotara.data.repository.SqliteFolderRepository
import com.arinara.fotara.data.repository.SqlitePhotoRepository
import com.arinara.fotara.data.storage.PhotoStorageManager
import com.arinara.fotara.data.repository.SettingsRepository
import com.arinara.fotara.data.repository.DefaultSettingsRepository
import com.arinara.fotara.ocr.DefaultFolderSuggestEngine
import com.arinara.fotara.ocr.FolderSuggestEngine
import com.arinara.fotara.ocr.MlKitOcrEngine
import com.arinara.fotara.ocr.OcrEngine
import com.arinara.fotara.util.DeadlineNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

interface AppContainer {
    val folderRepository: FolderRepository
    val photoRepository: PhotoRepository
    val ocrEngine: OcrEngine
    val folderSuggestEngine: FolderSuggestEngine
    val photoStorageManager: PhotoStorageManager
    val deadlineNotificationManager: DeadlineNotificationManager
    val settingsRepository: SettingsRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    private val dbHelper by lazy {
        FotaraDbHelper(context)
    }

    override val photoStorageManager: PhotoStorageManager by lazy {
        PhotoStorageManager(context)
    }

    override val folderRepository: FolderRepository by lazy {
        SqliteFolderRepository(dbHelper, photoStorageManager)
    }

    override val photoRepository: PhotoRepository by lazy {
        SqlitePhotoRepository(dbHelper, folderRepository, photoStorageManager)
    }

    override val ocrEngine: OcrEngine by lazy {
        MlKitOcrEngine(context)
    }

    override val folderSuggestEngine: FolderSuggestEngine by lazy {
        DefaultFolderSuggestEngine()
    }

    override val deadlineNotificationManager: DeadlineNotificationManager by lazy {
        DeadlineNotificationManager(context)
    }

    override val settingsRepository: SettingsRepository by lazy {
        DefaultSettingsRepository(
            context = context,
            folderRepository = folderRepository,
            photoRepository = photoRepository,
            photoStorageManager = photoStorageManager
        )
    }

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                photoRepository.purgeOldTrashedItems(30)
            } catch (e: Exception) {
                android.util.Log.e("AppContainer", "Startup trash purge failed: ${e.message}", e)
            }
        }
    }
}
