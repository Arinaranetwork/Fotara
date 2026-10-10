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
import com.arinara.fotara.data.repository.DocumentRepository
import com.arinara.fotara.data.repository.SqliteDocumentRepository
import com.arinara.fotara.data.repository.TextNoteRepository
import com.arinara.fotara.data.repository.SqliteTextNoteRepository
import com.arinara.fotara.data.repository.CanvasNoteRepository
import com.arinara.fotara.data.repository.SqliteCanvasNoteRepository
import com.arinara.fotara.online.UpdateManager
import com.arinara.fotara.online.FeedbackManager
import com.arinara.fotara.util.DeadlineNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

import com.arinara.fotara.data.repository.WorkspaceRepository
import com.arinara.fotara.data.repository.SqliteWorkspaceRepository
import com.arinara.fotara.data.repository.ScheduleRepository
import com.arinara.fotara.data.repository.SqliteScheduleRepository
import com.arinara.fotara.feature.schedule.engine.ScheduleCutoffEngine
import com.arinara.fotara.feature.schedule.notification.ScheduleNotificationScheduler

interface AppContainer {
    val workspaceRepository: WorkspaceRepository
    val folderRepository: FolderRepository
    val photoRepository: PhotoRepository
    val ocrEngine: OcrEngine
    val folderSuggestEngine: FolderSuggestEngine
    val photoStorageManager: PhotoStorageManager
    val deadlineNotificationManager: DeadlineNotificationManager
    val settingsRepository: SettingsRepository
    val documentRepository: DocumentRepository
    val textNoteRepository: TextNoteRepository
    val canvasNoteRepository: CanvasNoteRepository
    val canvasRepository: com.arinara.fotara.canvas.persistence.CanvasRepository
    val canvasAssetManager: com.arinara.fotara.canvas.persistence.CanvasAssetManager
    val noteScheduleManager: com.arinara.fotara.util.NoteScheduleManager
    val photoDrawingRepository: com.arinara.fotara.data.repository.PhotoDrawingRepository
    val pdfPagePinRepository: com.arinara.fotara.data.repository.PdfPagePinRepository
    val pdfPageDrawingRepository: com.arinara.fotara.data.repository.PdfPageDrawingRepository
    val audioAnnotationRepository: com.arinara.fotara.audio.repository.AudioAnnotationRepository
    val scheduleRepository: ScheduleRepository
    val scheduleCutoffEngine: ScheduleCutoffEngine
    val scheduleNotificationScheduler: ScheduleNotificationScheduler
    val updateManager: UpdateManager
    val feedbackManager: FeedbackManager
    val context: Context
    val dialogCoordinator: com.arinara.fotara.coordinator.AppDialogCoordinator
    val deviceRegistry: com.arinara.fotara.online.DeviceRegistry
    val friendsRepository: com.arinara.fotara.feature.friends.data.FriendsRepository
    val spaceRepository: com.arinara.fotara.data.repository.SpaceRepository
    val activeSpaceManager: com.arinara.fotara.feature.space.ActiveSpaceManager
    val packageManager: com.arinara.fotara.feature.packages.loader.FotaraPackageManager
}

class DefaultAppContainer(override val context: Context) : AppContainer {

    private val dbHelper by lazy {
        FotaraDbHelper(context)
    }

    override val photoStorageManager: PhotoStorageManager by lazy {
        PhotoStorageManager(context)
    }

    override val folderRepository: FolderRepository by lazy {
        SqliteFolderRepository(dbHelper, photoStorageManager)
    }

    override val workspaceRepository: WorkspaceRepository by lazy {
        SqliteWorkspaceRepository(dbHelper, folderRepository)
    }

    override val photoRepository: PhotoRepository by lazy {
        SqlitePhotoRepository(
            dbHelper = dbHelper,
            folderRepository = folderRepository,
            photoStorageManager = photoStorageManager,
            photoDrawingRepository = photoDrawingRepository
        )
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
            photoStorageManager = photoStorageManager,
            workspaceRepository = workspaceRepository,
            documentRepositoryProvider = { documentRepository }
        )
    }

    override val documentRepository: DocumentRepository by lazy {
        SqliteDocumentRepository(
            context = context,
            dbHelper = dbHelper,
            photoRepository = photoRepository,
            photoStorageManager = photoStorageManager,
            ocrEngine = ocrEngine,
            folderRepository = folderRepository
        )
    }

    override val textNoteRepository: TextNoteRepository by lazy {
        SqliteTextNoteRepository(dbHelper, folderRepository)
    }

    override val canvasNoteRepository: CanvasNoteRepository by lazy {
        SqliteCanvasNoteRepository(dbHelper, folderRepository)
    }

    override val canvasAssetManager: com.arinara.fotara.canvas.persistence.CanvasAssetManager by lazy {
        com.arinara.fotara.canvas.persistence.CanvasAssetManager(context)
    }

    override val noteScheduleManager: com.arinara.fotara.util.NoteScheduleManager by lazy {
        com.arinara.fotara.util.NoteScheduleManager(context)
    }

    override val canvasRepository: com.arinara.fotara.canvas.persistence.CanvasRepository by lazy {
        com.arinara.fotara.canvas.persistence.DefaultCanvasRepository(
            canvasDao = com.arinara.fotara.canvas.persistence.SqliteCanvasDao(dbHelper),
            assetManager = canvasAssetManager,
            dbHelper = dbHelper
        )
    }

    override val photoDrawingRepository: com.arinara.fotara.data.repository.PhotoDrawingRepository by lazy {
        com.arinara.fotara.data.repository.SqlitePhotoDrawingRepository(dbHelper)
    }

    override val pdfPagePinRepository: com.arinara.fotara.data.repository.PdfPagePinRepository by lazy {
        com.arinara.fotara.data.repository.SqlitePdfPagePinRepository(dbHelper)
    }

    override val pdfPageDrawingRepository: com.arinara.fotara.data.repository.PdfPageDrawingRepository by lazy {
        com.arinara.fotara.data.repository.SqlitePdfPageDrawingRepository(dbHelper)
    }

    override val audioAnnotationRepository: com.arinara.fotara.audio.repository.AudioAnnotationRepository by lazy {
        com.arinara.fotara.audio.repository.SqliteAudioAnnotationRepository(dbHelper)
    }

    override val updateManager: UpdateManager by lazy {
        UpdateManager(context)
    }

    override val feedbackManager: FeedbackManager by lazy {
        FeedbackManager(context)
    }

    override val dialogCoordinator: com.arinara.fotara.coordinator.AppDialogCoordinator by lazy {
        com.arinara.fotara.coordinator.AppDialogCoordinator()
    }

    override val deviceRegistry: com.arinara.fotara.online.DeviceRegistry by lazy {
        com.arinara.fotara.online.DeviceRegistry(
            context = context,
            settingsRepository = settingsRepository
        )
    }

    override val scheduleRepository: ScheduleRepository by lazy {
        SqliteScheduleRepository(dbHelper)
    }

    override val scheduleCutoffEngine: ScheduleCutoffEngine by lazy {
        ScheduleCutoffEngine()
    }

    override val scheduleNotificationScheduler: ScheduleNotificationScheduler by lazy {
        ScheduleNotificationScheduler(context)
    }

    override val friendsRepository: com.arinara.fotara.feature.friends.data.FriendsRepository by lazy {
        com.arinara.fotara.feature.friends.data.LocalFriendsRepository(context)
    }

    override val spaceRepository: com.arinara.fotara.data.repository.SpaceRepository by lazy {
        com.arinara.fotara.data.repository.SqliteSpaceRepository(dbHelper, context)
    }

    override val activeSpaceManager: com.arinara.fotara.feature.space.ActiveSpaceManager by lazy {
        com.arinara.fotara.feature.space.ActiveSpaceManager(spaceRepository)
    }

    override val packageManager: com.arinara.fotara.feature.packages.loader.FotaraPackageManager by lazy {
        com.arinara.fotara.feature.packages.loader.FotaraPackageManager(context)
    }

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                photoRepository.purgeOldTrashedItems(30)
            } catch (e: Exception) {
                android.util.Log.e("AppContainer", "Startup trash purge failed: ${e.message}", e)
            }
            try {
                photoDrawingRepository.cleanOrphanDrawings()
            } catch (e: Exception) {
                android.util.Log.e("AppContainer", "Startup orphan drawings purge failed: ${e.message}", e)
            }
        }
    }
}
