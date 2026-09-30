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
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.model.PhotoGroup
import com.arinara.fotara.data.model.PhotoSource
import com.arinara.fotara.data.model.Subfolder
import com.arinara.fotara.data.model.TagColor
import com.arinara.fotara.data.repository.FolderRepository
import com.arinara.fotara.data.repository.PhotoRepository
import com.arinara.fotara.data.repository.DocumentRepository
import com.arinara.fotara.data.repository.TextNoteRepository
import com.arinara.fotara.data.repository.SettingsRepository
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
import java.util.UUID

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
    val isDrawerOpen: Boolean = false,
    val isPlacing: Boolean = false,
    val placementProgress: Pair<Int, Int>? = null,
    val statusMessage: String? = null,
    val isFinished: Boolean = false
) {
    val selectedCount: Int get() = stagedItems.count { it.isSelected }
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
    }

    fun processIncomingIntent(intent: Intent) {
        viewModelScope.launch {
            _uiState.update { it.copy(isStaging = true) }
            val items = withContext(Dispatchers.IO) {
                stageIncomingData(intent)
            }
            _uiState.update {
                it.copy(
                    stagedItems = items.first,
                    skippedCount = items.second,
                    isStaging = false,
                    isFinished = items.first.isEmpty()
                )
            }
        }
    }

    private fun stageIncomingData(intent: Intent): Pair<List<StagedShareItem>, Int> {
        val action = intent.action
        val incomingUris = mutableListOf<Uri>()
        var incomingText: String? = null

        if (action == Intent.ACTION_SEND) {
            val streamUri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
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
            val list = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
            }
            if (list != null) {
                incomingUris.addAll(list)
            }
        }

        val totalIncoming = incomingUris.size + if (incomingText != null) 1 else 0
        val cappedUris = incomingUris.take(30)
        val skipped = (totalIncoming - 30).coerceAtLeast(0)

        val result = mutableListOf<StagedShareItem>()

        // 1. Process capped URIs
        for (uri in cappedUris) {
            try {
                val item = stageUri(uri)
                if (item != null) {
                    result.add(item)
                }
            } catch (e: Exception) {
                android.util.Log.e("SharePlacementVM", "Failed to stage uri: $uri", e)
            }
        }

        // 2. Process text if space remains in 30 cap
        if (incomingText != null && result.size < 30) {
            try {
                val textItem = stageText(incomingText)
                result.add(textItem)
            } catch (e: Exception) {
                android.util.Log.e("SharePlacementVM", "Failed to stage text", e)
            }
        }

        return Pair(result, skipped)
    }

    private fun stageUri(uri: Uri): StagedShareItem? {
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
            mime.startsWith("image/") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png") || lowerName.endsWith(".webp") -> StagedItemType.IMAGE
            mime == "application/pdf" || lowerName.endsWith(".pdf") -> StagedItemType.PDF
            mime.contains("wordprocessingml") || mime.contains("msword") || lowerName.endsWith(".docx") || lowerName.endsWith(".doc") -> StagedItemType.DOCX
            mime.startsWith("text/") || lowerName.endsWith(".txt") || lowerName.endsWith(".md") -> StagedItemType.TEXT
            else -> StagedItemType.IMAGE // Default fallback
        }

        val ext = when (itemType) {
            StagedItemType.IMAGE -> if (lowerName.contains('.')) lowerName.substringAfterLast('.') else "jpg"
            StagedItemType.PDF -> "pdf"
            StagedItemType.DOCX -> "docx"
            StagedItemType.TEXT -> "txt"
        }

        val id = UUID.randomUUID().toString()
        val stagedFile = File(stagingDir, "stage_${id}.$ext")

        cr.openInputStream(uri)?.use { input ->
            FileOutputStream(stagedFile).use { output ->
                input.copyTo(output)
            }
        } ?: return null

        var thumbPath: String? = null
        if (itemType == StagedItemType.IMAGE) {
            thumbPath = generateImageThumbnail(stagedFile)
        } else if (itemType == StagedItemType.PDF) {
            thumbPath = generatePdfThumbnail(stagedFile)
        }

        var snippet: String? = null
        if (itemType == StagedItemType.TEXT) {
            try {
                snippet = stagedFile.readText().take(300)
            } catch (_: Exception) {}
        }

        return StagedShareItem(
            id = id,
            displayName = displayName,
            itemType = itemType,
            mimeType = mime,
            stagedFile = stagedFile,
            fileSizeBytes = stagedFile.length(),
            textBody = snippet,
            thumbnailPath = thumbPath,
            isSelected = true
        )
    }

    private fun stageText(text: String): StagedShareItem {
        val id = UUID.randomUUID().toString()
        val stagedFile = File(stagingDir, "stage_${id}.txt")
        stagedFile.writeText(text)

        val firstLine = text.lineSequence().firstOrNull { it.isNotBlank() } ?: "Shared Note"
        val title = if (firstLine.length > 40) firstLine.take(40) + "..." else firstLine

        return StagedShareItem(
            id = id,
            displayName = title,
            itemType = StagedItemType.TEXT,
            mimeType = "text/plain",
            stagedFile = stagedFile,
            fileSizeBytes = stagedFile.length(),
            textBody = text.take(300),
            thumbnailPath = null,
            isSelected = true
        )
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
        _uiState.update {
            it.copy(
                selectedFolder = folder,
                selectedSubfolderId = null,
                selectedGroupId = null
            )
        }
        loadFolderDetails(folder.id)
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

    fun toggleItemSelection(itemId: String) {
        _uiState.update { state ->
            val updated = state.stagedItems.map {
                if (it.id == itemId) it.copy(isSelected = !it.isSelected) else it
            }
            state.copy(stagedItems = updated)
        }
    }

    fun setAllSelected(selected: Boolean) {
        _uiState.update { state ->
            val updated = state.stagedItems.map { it.copy(isSelected = selected) }
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

    fun setDrawerOpen(open: Boolean) {
        _uiState.update { it.copy(isDrawerOpen = open) }
    }

    fun createFolder(name: String, colorHex: String) {
        viewModelScope.launch {
            val newId = folderRepository.createFolder(name = name.trim(), colorLabel = colorHex, isPinned = false)
            val updatedFolders = folderRepository.getFolders()
            // Selected will update via flow
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
        val itemsToPlace = state.stagedItems.filter { it.isSelected }
        if (itemsToPlace.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isPlacing = true, placementProgress = Pair(0, itemsToPlace.size)) }

            val remainingItems = state.stagedItems.toMutableList()
            var placedCount = 0

            withContext(Dispatchers.IO) {
                for ((index, item) in itemsToPlace.withIndex()) {
                    _uiState.update { it.copy(placementProgress = Pair(index + 1, itemsToPlace.size)) }
                    try {
                        when (item.itemType) {
                            StagedItemType.IMAGE -> {
                                val savedFile = photoStorageManager.saveUriAsPhoto(Uri.fromFile(item.stagedFile))
                                val now = System.currentTimeMillis()
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

                        // Cleanup temp file
                        item.stagedFile.delete()
                        item.thumbnailPath?.let { File(it).delete() }
                        remainingItems.remove(item)
                        placedCount++
                    } catch (e: Exception) {
                        android.util.Log.e("SharePlacementVM", "Error importing item ${item.displayName}", e)
                    }
                }
            }

            if (remainingItems.isEmpty()) {
                _uiState.update {
                    it.copy(
                        stagedItems = emptyList(),
                        isPlacing = false,
                        placementProgress = null,
                        statusMessage = "All $placedCount item${if (placedCount > 1) "s" else ""} saved to ${folder.name}!",
                        isFinished = true
                    )
                }
            } else {
                // Select all remaining items for convenience in next placement
                val updatedRemaining = remainingItems.map { it.copy(isSelected = true) }
                _uiState.update {
                    it.copy(
                        stagedItems = updatedRemaining,
                        isPlacing = false,
                        placementProgress = null,
                        statusMessage = "Placed $placedCount items in ${folder.name}. ${updatedRemaining.size} items remaining to place."
                    )
                }
            }
        }
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
