// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arinara.fotara.data.model.DestinationType
import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.model.PhotoGroup
import com.arinara.fotara.data.model.PhotoSource
import com.arinara.fotara.data.model.RecentDestination
import com.arinara.fotara.data.model.Subfolder
import com.arinara.fotara.data.repository.DocumentRepository
import com.arinara.fotara.data.repository.FolderRepository
import com.arinara.fotara.data.repository.PhotoRepository
import com.arinara.fotara.data.repository.SettingsRepository
import com.arinara.fotara.data.repository.TextNoteRepository
import com.arinara.fotara.data.storage.PhotoStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipInputStream

data class SharePlacementUiState(
    val stagedItems: List<StagedShareItem> = emptyList(),
    val isStaging: Boolean = false,
    val skippedCount: Int = 0,
    val folders: List<Folder> = emptyList(),
    val selectedFolder: Folder? = null,
    val subfolders: List<Subfolder> = emptyList(),
    val selectedSubfolderId: Long? = null,
    val availableGroups: List<PhotoGroup> = emptyList(),
    val selectedGroupId: Long? = null,
    val recentDestinations: List<RecentDestination> = emptyList(),
    val unlockedFolderIds: Set<Long> = emptySet(),
    val folderToUnlock: Folder? = null,
    val isSidePanelOpen: Boolean = false,
    val isPlacing: Boolean = false,
    val placementProgress: Pair<Int, Int>? = null,
    val statusMessage: String? = null,
    val isFinished: Boolean = false,
    val showExitConfirmDialog: Boolean = false
) {
    val isGroupDestination: Boolean get() = selectedGroupId != null
    val placeableCount: Int get() = ShareSessionEngine.computePlaceableCount(stagedItems, isGroupDestination)
    val placeButtonLabel: String get() = ShareSessionEngine.formatPlaceButtonLabel(placeableCount)
    val shouldShowSidePanel: Boolean get() = ShareSessionEngine.shouldShowSidePanel(stagedItems.size)
    val totalRemaining: Int get() = stagedItems.size
}

class SharePlacementViewModel(
    private val appContext: Context,
    private val folderRepository: FolderRepository,
    private val photoRepository: PhotoRepository,
    private val documentRepository: DocumentRepository,
    private val textNoteRepository: TextNoteRepository,
    private val photoStorageManager: PhotoStorageManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SharePlacementUiState())
    val uiState: StateFlow<SharePlacementUiState> = _uiState.asStateFlow()

    private val stagingDir: File by lazy {
        File(appContext.cacheDir, "share_staging").apply { if (!exists()) mkdirs() }
    }

    init {
        // Observe folders
        viewModelScope.launch {
            folderRepository.getFolders().collect { folderList ->
                _uiState.update { state ->
                    val updatedSelectedFolder = state.selectedFolder?.let { sel ->
                        folderList.find { it.id == sel.id }
                    } ?: folderList.firstOrNull()
                    state.copy(
                        folders = folderList,
                        selectedFolder = updatedSelectedFolder
                    )
                }
                _uiState.value.selectedFolder?.let { loadFolderDetails(it.id) }
            }
        }

        // Load recent destinations
        refreshRecentDestinations()
    }

    fun refreshRecentDestinations() {
        val recents = settingsRepository.getRecentDestinations()
        _uiState.update { it.copy(recentDestinations = recents) }
    }

    fun processIncomingIntent(intent: Intent) {
        viewModelScope.launch {
            _uiState.update { it.copy(isStaging = true) }
            val (newItems, skipped) = withContext(Dispatchers.IO) {
                stageIncomingData(intent)
            }
            _uiState.update { state ->
                val combined = state.stagedItems + newItems
                val totalSkipped = state.skippedCount + skipped
                state.copy(
                    stagedItems = combined,
                    skippedCount = totalSkipped,
                    isStaging = false,
                    isFinished = combined.isEmpty()
                )
            }

            // Generate thumbnails in background
            generateThumbnailsAsync(newItems)
        }
    }

    private fun stageIncomingData(intent: Intent): Pair<List<StagedShareItem>, Int> {
        val action = intent.action
        val incomingUris = mutableListOf<Uri>()
        var incomingText: String? = null
        val incomingSubject: String? = intent.getStringExtra(Intent.EXTRA_SUBJECT)

        if (action == Intent.ACTION_SEND) {
            val streamUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            if (streamUri != null) {
                incomingUris.add(streamUri)
            } else {
                incomingText = intent.getStringExtra(Intent.EXTRA_TEXT)
            }
        } else if (action == Intent.ACTION_SEND_MULTIPLE) {
            val list = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
            }
            if (list != null) {
                incomingUris.addAll(list)
            }
        }

        val totalIncoming = incomingUris.size + (if (incomingText != null) 1 else 0)
        val existingCount = _uiState.value.stagedItems.size
        val (canTake, skipped) = ShareSessionEngine.computeCapBounds(existingCount, totalIncoming)

        val result = mutableListOf<StagedShareItem>()
        var itemsTaken = 0

        // 1. Process URIs up to capacity
        for (uri in incomingUris) {
            if (itemsTaken >= canTake) break
            try {
                val item = stageUri(uri, incomingSubject)
                if (item != null) {
                    result.add(item)
                    itemsTaken++
                }
            } catch (e: Exception) {
                Log.e("SharePlacementVM", "Failed to stage uri: $uri", e)
            }
        }

        // 2. Process text if space remains
        if (incomingText != null && itemsTaken < canTake) {
            try {
                val textItem = stageText(incomingText, incomingSubject)
                result.add(textItem)
            } catch (e: Exception) {
                Log.e("SharePlacementVM", "Failed to stage text", e)
            }
        }

        return Pair(result, skipped)
    }

    private fun stageUri(uri: Uri, subject: String?): StagedShareItem? {
        val cr = appContext.contentResolver
        var displayName = "Shared_Item"
        var mime = cr.getType(uri) ?: "application/octet-stream"

        try {
            cr.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx != -1) {
                        val n = cursor.getString(nameIdx)
                        if (!n.isNullOrBlank()) displayName = n
                    }
                }
            }
        } catch (_: Exception) {}

        val lowerName = displayName.lowercase()
        val itemType = when {
            mime.startsWith("image/") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") ||
                lowerName.endsWith(".png") || lowerName.endsWith(".webp") || lowerName.endsWith(".gif") ||
                lowerName.endsWith(".bmp") || lowerName.endsWith(".heic") || lowerName.endsWith(".heif") -> StagedItemType.IMAGE
            mime == "application/pdf" || lowerName.endsWith(".pdf") -> StagedItemType.PDF
            mime.contains("wordprocessingml") || mime.contains("msword") ||
                lowerName.endsWith(".docx") || lowerName.endsWith(".doc") -> StagedItemType.DOCX
            mime.startsWith("text/") || lowerName.endsWith(".txt") || lowerName.endsWith(".md") -> StagedItemType.TEXT
            else -> StagedItemType.IMAGE
        }

        val ext = when (itemType) {
            StagedItemType.IMAGE -> if (lowerName.contains('.')) lowerName.substringAfterLast('.') else "jpg"
            StagedItemType.PDF -> "pdf"
            StagedItemType.DOCX -> "docx"
            StagedItemType.TEXT -> if (lowerName.endsWith(".md")) "md" else "txt"
        }

        val id = UUID.randomUUID().toString()
        val stagedFile = File(stagingDir, "stage_${id}.$ext")

        try {
            cr.openInputStream(uri)?.use { input ->
                FileOutputStream(stagedFile).use { output ->
                    input.copyTo(output, bufferSize = 8192)
                }
            } ?: return null
        } catch (e: Exception) {
            Log.e("SharePlacementVM", "Failed to stream copy uri: $uri", e)
            return null
        }

        // Validate content integrity
        var errorMsg: String? = null
        var textSnippet: String? = null

        when (itemType) {
            StagedItemType.IMAGE -> {
                try {
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(stagedFile.absolutePath, opts)
                    if (opts.outWidth <= 0 || opts.outHeight <= 0) {
                        errorMsg = "Corrupt image file"
                    }
                } catch (e: Exception) {
                    errorMsg = "Unreadable image"
                }
            }
            StagedItemType.PDF -> {
                var pfd: ParcelFileDescriptor? = null
                var renderer: PdfRenderer? = null
                try {
                    pfd = ParcelFileDescriptor.open(stagedFile, ParcelFileDescriptor.MODE_READ_ONLY)
                    renderer = PdfRenderer(pfd)
                    if (renderer.pageCount == 0) {
                        errorMsg = "Empty or corrupt PDF"
                    }
                } catch (se: SecurityException) {
                    errorMsg = "Password-protected PDF"
                } catch (e: Exception) {
                    errorMsg = "Corrupt or unreadable PDF"
                } finally {
                    try { renderer?.close() } catch (_: Exception) {}
                    try { pfd?.close() } catch (_: Exception) {}
                }
            }
            StagedItemType.DOCX -> {
                try {
                    stagedFile.inputStream().use { inStream ->
                        val zip = ZipInputStream(inStream)
                        var hasDocXml = false
                        var entry = zip.nextEntry
                        while (entry != null) {
                            if (entry.name == "word/document.xml") {
                                hasDocXml = true
                                break
                            }
                            entry = zip.nextEntry
                        }
                        if (!hasDocXml) {
                            errorMsg = "Invalid Word document structure"
                        }
                    }
                } catch (e: Exception) {
                    errorMsg = "Corrupt Word document"
                }
            }
            StagedItemType.TEXT -> {
                try {
                    val content = stagedFile.readText()
                    textSnippet = content.take(300)
                    if (displayName == "Shared_Item" || displayName.startsWith("stage_")) {
                        displayName = ShareSessionEngine.deriveTextTitle(subject, displayName, content)
                    }
                } catch (e: Exception) {
                    errorMsg = "Unreadable text file"
                }
            }
        }

        return StagedShareItem(
            id = id,
            displayName = displayName,
            itemType = itemType,
            mimeType = mime,
            stagedFile = stagedFile,
            fileSizeBytes = stagedFile.length(),
            textBody = textSnippet,
            thumbnailPath = null,
            isSelected = errorMsg == null,
            errorMessage = errorMsg
        )
    }

    private fun stageText(text: String, subject: String?): StagedShareItem {
        val id = UUID.randomUUID().toString()
        val stagedFile = File(stagingDir, "stage_${id}.txt")
        stagedFile.writeText(text)

        val title = ShareSessionEngine.deriveTextTitle(
            subject = subject,
            fileName = null,
            textBody = text
        )

        return StagedShareItem(
            id = id,
            displayName = title,
            itemType = StagedItemType.TEXT,
            mimeType = "text/plain",
            stagedFile = stagedFile,
            fileSizeBytes = stagedFile.length(),
            textBody = text.take(300),
            thumbnailPath = null,
            isSelected = true,
            errorMessage = null
        )
    }

    private fun generateThumbnailsAsync(items: List<StagedShareItem>) {
        viewModelScope.launch(Dispatchers.IO) {
            for (item in items) {
                if (item.isError) continue
                var thumbPath: String? = null
                when (item.itemType) {
                    StagedItemType.IMAGE -> {
                        thumbPath = generateImageThumbnail(item.stagedFile)
                    }
                    StagedItemType.PDF -> {
                        thumbPath = generatePdfThumbnail(item.stagedFile)
                    }
                    else -> {}
                }

                if (thumbPath != null) {
                    _uiState.update { state ->
                        val updated = state.stagedItems.map {
                            if (it.id == item.id) it.copy(thumbnailPath = thumbPath) else it
                        }
                        state.copy(stagedItems = updated)
                    }
                }
            }
        }
    }

    private fun generateImageThumbnail(file: File): String? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            val sample = maxOf(1, maxOf(bounds.outWidth / 300, bounds.outHeight / 300))
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bmp = BitmapFactory.decodeFile(file.absolutePath, opts) ?: return null
            val thumbFile = File(file.parentFile, "thumb_${file.name}.jpg")
            FileOutputStream(thumbFile).use { out ->
                bmp.compress(Bitmap.CompressFormat.JPEG, 75, out)
            }
            bmp.recycle()
            thumbFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    private fun generatePdfThumbnail(file: File): String? {
        return try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            if (renderer.pageCount > 0) {
                val page = renderer.openPage(0)
                val w = 240
                val h = (240f * page.height / page.width).toInt().coerceAtLeast(100)
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                bmp.eraseColor(android.graphics.Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                renderer.close()
                pfd.close()
                val thumbFile = File(file.parentFile, "thumb_${file.name}.jpg")
                FileOutputStream(thumbFile).use { out ->
                    bmp.compress(Bitmap.CompressFormat.JPEG, 80, out)
                }
                bmp.recycle()
                thumbFile.absolutePath
            } else {
                renderer.close()
                pfd.close()
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun selectFolder(folder: Folder) {
        if (folder.isLocked && !_uiState.value.unlockedFolderIds.contains(folder.id)) {
            _uiState.update { it.copy(folderToUnlock = folder) }
            return
        }
        applyFolderSelection(folder)
    }

    private fun applyFolderSelection(folder: Folder) {
        _uiState.update {
            it.copy(
                selectedFolder = folder,
                selectedSubfolderId = null,
                selectedGroupId = null
            )
        }
        loadFolderDetails(folder.id)
    }

    fun unlockFolderSuccess(folderId: Long) {
        val folder = _uiState.value.folders.find { it.id == folderId }
        _uiState.update {
            it.copy(
                unlockedFolderIds = it.unlockedFolderIds + folderId,
                folderToUnlock = null
            )
        }
        if (folder != null) {
            applyFolderSelection(folder)
        }
    }

    fun dismissFolderUnlock() {
        _uiState.update { it.copy(folderToUnlock = null) }
    }

    private fun loadFolderDetails(folderId: Long) {
        viewModelScope.launch {
            folderRepository.getSubfolders(folderId).collect { subList ->
                _uiState.update { it.copy(subfolders = subList) }
            }
        }
        viewModelScope.launch {
            photoRepository.getGroupsByFolder(folderId).collect { groupList ->
                _uiState.update { it.copy(availableGroups = groupList) }
            }
        }
    }

    fun selectSubfolder(subfolderId: Long?) {
        _uiState.update { it.copy(selectedSubfolderId = subfolderId, selectedGroupId = null) }
    }

    fun selectGroup(groupId: Long?) {
        _uiState.update { it.copy(selectedGroupId = groupId) }
    }

    fun selectRecentDestination(recent: RecentDestination) {
        val folder = _uiState.value.folders.find { it.id == recent.folderId } ?: return
        if (folder.isLocked && !_uiState.value.unlockedFolderIds.contains(folder.id)) {
            _uiState.update { it.copy(folderToUnlock = folder) }
            return
        }
        _uiState.update {
            it.copy(
                selectedFolder = folder,
                selectedSubfolderId = recent.subfolderId,
                selectedGroupId = recent.groupId
            )
        }
        loadFolderDetails(folder.id)
    }

    fun toggleItemSelection(itemId: String) {
        _uiState.update { state ->
            val updated = ShareSessionEngine.toggleItemSelection(state.stagedItems, itemId)
            state.copy(stagedItems = updated)
        }
    }

    fun setAllSelected(selected: Boolean) {
        _uiState.update { state ->
            val updated = ShareSessionEngine.setAllSelected(state.stagedItems, selected)
            state.copy(stagedItems = updated)
        }
    }

    fun removeStagedItem(itemId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val item = _uiState.value.stagedItems.find { it.id == itemId }
            item?.stagedFile?.delete()
            item?.thumbnailPath?.let { File(it).delete() }
        }
        _uiState.update { state ->
            val updated = state.stagedItems.filter { it.id != itemId }
            state.copy(
                stagedItems = updated,
                isFinished = updated.isEmpty()
            )
        }
    }

    fun setSidePanelOpen(open: Boolean) {
        _uiState.update { it.copy(isSidePanelOpen = open) }
    }

    fun createFolder(name: String, colorHex: String) {
        viewModelScope.launch {
            val newId = folderRepository.createFolder(name = name.trim(), colorLabel = colorHex, isPinned = false)
            val newFolder = folderRepository.getFolderById(newId)
            if (newFolder != null) {
                applyFolderSelection(newFolder)
            }
        }
    }

    fun createSubfolder(name: String) {
        val folder = _uiState.value.selectedFolder ?: return
        viewModelScope.launch {
            val newSubId = folderRepository.createSubfolder(folderId = folder.id, name = name.trim())
            _uiState.update { it.copy(selectedSubfolderId = newSubId) }
        }
    }

    fun placeSelectedItems() {
        val state = _uiState.value
        val folder = state.selectedFolder ?: return
        val isGroup = state.isGroupDestination
        val itemsToPlace = state.stagedItems.filter { it.isSelected && it.canPlaceInDestination(isGroup) }
        if (itemsToPlace.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isPlacing = true, placementProgress = Pair(0, itemsToPlace.size)) }

            val placedIds = mutableSetOf<String>()
            var placedCount = 0

            withContext(Dispatchers.IO) {
                for ((index, item) in itemsToPlace.withIndex()) {
                    _uiState.update { it.copy(placementProgress = Pair(index + 1, itemsToPlace.size)) }
                    try {
                        val now = System.currentTimeMillis()
                        when (item.itemType) {
                            StagedItemType.IMAGE -> {
                                val savedFile = photoStorageManager.saveUriAsPhoto(Uri.fromFile(item.stagedFile))
                                val photo = Photo(
                                    id = 0L,
                                    fileUri = savedFile.filePath,
                                    thumbnailUri = savedFile.thumbnailPath,
                                    folderId = folder.id,
                                    subfolderId = state.selectedSubfolderId,
                                    groupId = state.selectedGroupId,
                                    createdAt = now,
                                    addedAt = now,
                                    tagColor = null,
                                    caption = item.displayName,
                                    ocrText = null,
                                    source = PhotoSource.IMPORT,
                                    linkedDeadline = null,
                                    fileSizeBytes = savedFile.fileSizeBytes,
                                    note = null,
                                    isTrashed = false,
                                    deletedAt = null,
                                    tags = null
                                )
                                photoRepository.addPhoto(photo)
                            }
                            StagedItemType.PDF -> {
                                documentRepository.importPdf(
                                    uri = Uri.fromFile(item.stagedFile),
                                    folderId = folder.id,
                                    subfolderId = state.selectedSubfolderId,
                                    name = item.displayName
                                )
                            }
                            StagedItemType.DOCX -> {
                                documentRepository.importDocx(
                                    uri = Uri.fromFile(item.stagedFile),
                                    folderId = folder.id,
                                    subfolderId = state.selectedSubfolderId,
                                    name = item.displayName
                                )
                            }
                            StagedItemType.TEXT -> {
                                textNoteRepository.createTextNote(
                                    folderId = folder.id,
                                    subfolderId = state.selectedSubfolderId,
                                    title = item.displayName,
                                    bodyMarkdown = item.textBody ?: item.stagedFile.readText()
                                )
                            }
                        }

                        // Cleanup temp file for successfully placed item
                        item.stagedFile.delete()
                        item.thumbnailPath?.let { File(it).delete() }
                        placedIds.add(item.id)
                        placedCount++
                    } catch (e: Exception) {
                        Log.e("SharePlacementVM", "Error placing item ${item.displayName}", e)
                    }
                }
            }

            // Record recent destination
            val groupName = state.availableGroups.find { it.id == state.selectedGroupId }?.name
            val subName = state.subfolders.find { it.id == state.selectedSubfolderId }?.name
            val destType = when {
                state.selectedGroupId != null -> DestinationType.GROUP
                state.selectedSubfolderId != null -> DestinationType.SUBFOLDER
                else -> DestinationType.FOLDER
            }
            val destTitle = when {
                groupName != null -> groupName
                subName != null -> subName
                else -> folder.name
            }
            settingsRepository.addRecentDestination(
                RecentDestination(
                    type = destType,
                    folderId = folder.id,
                    subfolderId = state.selectedSubfolderId,
                    groupId = state.selectedGroupId,
                    title = destTitle,
                    subtitle = if (destType != DestinationType.FOLDER) folder.name else null
                )
            )
            refreshRecentDestinations()

            // Update remaining items
            val remaining = ShareSessionEngine.removePlacedItems(state.stagedItems, placedIds)

            if (remaining.isEmpty()) {
                _uiState.update {
                    it.copy(
                        stagedItems = emptyList(),
                        isPlacing = false,
                        placementProgress = null,
                        statusMessage = "All $placedCount items saved to ${folder.name}!",
                        isFinished = true
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        stagedItems = remaining,
                        isPlacing = false,
                        placementProgress = null,
                        statusMessage = "Placed $placedCount item${if (placedCount > 1) "s" else ""} in ${folder.name}. ${remaining.size} items remaining."
                    )
                }
            }
        }
    }

    fun requestExit() {
        if (_uiState.value.totalRemaining > 0) {
            _uiState.update { it.copy(showExitConfirmDialog = true) }
        } else {
            cancelAndCleanup()
        }
    }

    fun dismissExitConfirm() {
        _uiState.update { it.copy(showExitConfirmDialog = false) }
    }

    fun confirmExit() {
        _uiState.update { it.copy(showExitConfirmDialog = false) }
        cancelAndCleanup()
    }

    fun cancelAndCleanup() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                stagingDir.listFiles()?.forEach { it.delete() }
            } catch (_: Exception) {}
        }
        _uiState.update { it.copy(isFinished = true) }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}

class SharePlacementViewModelFactory(
    private val appContext: Context,
    private val folderRepository: FolderRepository,
    private val photoRepository: PhotoRepository,
    private val documentRepository: DocumentRepository,
    private val textNoteRepository: TextNoteRepository,
    private val photoStorageManager: PhotoStorageManager,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SharePlacementViewModel(
            appContext = appContext,
            folderRepository = folderRepository,
            photoRepository = photoRepository,
            documentRepository = documentRepository,
            textNoteRepository = textNoteRepository,
            photoStorageManager = photoStorageManager,
            settingsRepository = settingsRepository
        ) as T
    }
}
