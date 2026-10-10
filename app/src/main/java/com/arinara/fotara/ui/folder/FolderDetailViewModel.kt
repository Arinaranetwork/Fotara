// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.folder

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arinara.fotara.data.model.DestinationType
import com.arinara.fotara.data.model.DocumentNote
import com.arinara.fotara.data.model.DocumentPage
import com.arinara.fotara.data.model.LinkGroup
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.model.PhotoGroup
import com.arinara.fotara.data.model.PhotoSource
import com.arinara.fotara.data.model.RecentDestination
import com.arinara.fotara.data.model.TextNote
import com.arinara.fotara.data.model.CanvasNote
import com.arinara.fotara.data.repository.DocumentRepository
import com.arinara.fotara.data.repository.FolderRepository
import com.arinara.fotara.data.repository.PhotoRepository
import com.arinara.fotara.data.repository.SettingsRepository
import com.arinara.fotara.data.repository.SubfolderDeleteResult
import com.arinara.fotara.data.repository.TextNoteRepository
import com.arinara.fotara.data.repository.CanvasNoteRepository
import com.arinara.fotara.data.storage.PhotoStorageManager
import com.arinara.fotara.ocr.FolderSuggestEngine
import com.arinara.fotara.ocr.OcrEngine
import com.arinara.fotara.util.DeadlineNotificationManager
import com.arinara.fotara.util.PdfExporter
import com.arinara.fotara.util.ZipExporter
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateMapOf
import java.io.File

class FolderDetailViewModel(
    val folderId: Long,
    private val folderRepository: FolderRepository,
    private val photoRepository: PhotoRepository,
    val photoStorageManager: PhotoStorageManager? = null,
    val ocrEngine: OcrEngine? = null,
    val folderSuggestEngine: FolderSuggestEngine? = null,
    val deadlineNotificationManager: DeadlineNotificationManager? = null,
    val initialSubfolderId: Long? = null,
    val targetPhotoId: Long? = null,
    val targetGroupId: Long? = null,
    val targetDocumentId: Long? = null,
    val targetTextNoteId: Long? = null,
    val targetCanvasId: Long? = null,
    private val settingsRepository: SettingsRepository? = null,
    val documentRepository: DocumentRepository? = null,
    val textNoteRepository: TextNoteRepository? = null,
    val canvasNoteRepository: CanvasNoteRepository? = null,
    val workspaceRepository: com.arinara.fotara.data.repository.WorkspaceRepository? = null
) : ViewModel() {

    val selectedPhotoMap = mutableStateMapOf<Long, Boolean>()
    val selectedGroupMap = mutableStateMapOf<Long, Boolean>()
    val selectedDocumentMap = mutableStateMapOf<Long, Boolean>()
    val selectedTextNoteMap = mutableStateMapOf<Long, Boolean>()
    val selectedCanvasNoteMap = mutableStateMapOf<Long, Boolean>()
    val selectedSubfolderMap = mutableStateMapOf<Long, Boolean>()

    private fun clearAllSelectionMaps() {
        selectedPhotoMap.clear()
        selectedGroupMap.clear()
        selectedDocumentMap.clear()
        selectedTextNoteMap.clear()
        selectedCanvasNoteMap.clear()
        selectedSubfolderMap.clear()
    }

    private val _uiState = MutableStateFlow(
        FolderDetailUiState(
            selectedSubfolderId = initialSubfolderId,
            highlightedPhotoId = targetPhotoId,
            highlightedGroupId = targetGroupId,
            highlightedDocumentId = targetDocumentId,
            highlightedTextNoteId = targetTextNoteId,
            highlightedCanvasId = targetCanvasId
        )
    )
    val uiState: StateFlow<FolderDetailUiState> = _uiState.asStateFlow()

    init {
        loadFolderData()
    }

    private fun loadFolderData() {
        viewModelScope.launch {
            folderRepository.getFolderById(folderId).collect { folder ->
                _uiState.update { it.copy(folder = folder) }
            }
        }

        viewModelScope.launch {
            folderRepository.getSubfolders(folderId).collect { subfolders ->
                _uiState.update { it.copy(subfolders = subfolders) }
            }
        }

        viewModelScope.launch {
            folderRepository.getFolders().collect { folders ->
                _uiState.update { it.copy(availableFolders = folders) }
            }
        }

        viewModelScope.launch {
            photoRepository.getAllActiveGroups().collect { groups ->
                _uiState.update { it.copy(availableGroups = groups) }
            }
        }

        workspaceRepository?.let { repo ->
            viewModelScope.launch {
                repo.observeWorkspaces().collect { workspaces ->
                    _uiState.update { it.copy(workspaces = workspaces) }
                }
            }
        }

        refreshRecentDestinations()

        settingsRepository?.let { repo ->
            viewModelScope.launch {
                repo.settingsFlow.collect { settings ->
                    _uiState.update {
                        it.copy(
                            gridDensity = settings.gridDensity,
                            combineFileNamePreset = settings.combineFileNamePreset
                        )
                    }
                }
            }
        }

        observePhotos()
    }

    private data class FolderDataBundle(
        val allPhotos: List<Photo>,
        val allGroups: List<PhotoGroup>,
        val allDocs: List<DocumentNote>,
        val allTextNotes: List<TextNote>,
        val allCanvasNotes: List<CanvasNote>,
        val gridLinkGroups: List<LinkGroup>,
        val docPagesMap: Map<Long, List<DocumentPage>> = emptyMap()
    )

    private data class GridRenderPayload(
        val photos: List<Photo>,
        val groups: List<PhotoGroup>,
        val documents: List<DocumentNote>,
        val textNotes: List<TextNote>,
        val canvasNotes: List<CanvasNote>,
        val gridItems: List<FolderGridItem>
    )

    private fun observePhotos() {
        val docFlow = documentRepository?.getDocumentNotesByFolder(folderId, null) ?: flowOf(emptyList())
        val docPagesFlow = documentRepository?.getAllDocumentPages() ?: flowOf(emptyMap())
        val textNoteFlow = textNoteRepository?.getTextNotesByFolder(folderId, null) ?: flowOf(emptyList())
        val canvasFlow = canvasNoteRepository?.getCanvasNotesByFolder(folderId, null) ?: flowOf(emptyList())

        val sourceBundleFlow = combine(
            listOf(
                photoRepository.getPhotosByFolder(folderId),
                photoRepository.getGroupsByFolder(folderId),
                docFlow,
                textNoteFlow,
                canvasFlow,
                photoRepository.getGridLinkGroups(),
                docPagesFlow
            )
        ) { array ->
            @Suppress("UNCHECKED_CAST")
            FolderDataBundle(
                allPhotos = array[0] as List<Photo>,
                allGroups = array[1] as List<PhotoGroup>,
                allDocs = array[2] as List<DocumentNote>,
                allTextNotes = array[3] as List<TextNote>,
                allCanvasNotes = array[4] as List<CanvasNote>,
                gridLinkGroups = array[5] as List<LinkGroup>,
                docPagesMap = array[6] as Map<Long, List<DocumentPage>>
            )
        }

        viewModelScope.launch {
            combine(_uiState, sourceBundleFlow) { state, bundle ->
                val currentSubfolder = state.selectedSubfolderId
                val filteredPhotos = if (currentSubfolder == null) bundle.allPhotos else bundle.allPhotos.filter { it.subfolderId == currentSubfolder }
                val filteredGroups = if (currentSubfolder == null) bundle.allGroups else bundle.allGroups.filter { it.subfolderId == currentSubfolder }
                val filteredDocs = if (currentSubfolder == null) bundle.allDocs else bundle.allDocs.filter { it.subfolderId == currentSubfolder }
                val filteredTextNotes = if (currentSubfolder == null) bundle.allTextNotes else bundle.allTextNotes.filter { it.subfolderId == currentSubfolder }
                val filteredCanvasNotes = if (currentSubfolder == null) bundle.allCanvasNotes else bundle.allCanvasNotes.filter { it.subfolderId == currentSubfolder }

                val linkGroupMap = bundle.gridLinkGroups.associateBy { it.id }
                val itemToLinkGroupMap = mutableMapOf<Long, LinkGroup>()
                for (lg in bundle.gridLinkGroups) {
                    for (mId in lg.memberIds) {
                        itemToLinkGroupMap[mId] = lg
                    }
                }

                val standalonePhotos = filteredPhotos.filter { it.groupId == null }
                val standaloneItems = standalonePhotos.map {
                    FolderGridItem.StandalonePhoto(
                        photo = it,
                        linkGroupId = itemToLinkGroupMap[it.id]?.id
                    )
                }

                val groupItems = filteredGroups.map { group ->
                    val members = bundle.allPhotos.filter { it.groupId == group.id }
                    val cover = bundle.allPhotos.firstOrNull { it.id == group.coverPhotoId }
                        ?: members.minByOrNull { it.addedAt }
                        ?: members.firstOrNull()
                    FolderGridItem.Group(
                        group = group,
                        memberPhotos = members,
                        coverPhoto = cover,
                        linkGroupId = itemToLinkGroupMap[-group.id]?.id
                    )
                }

                val docItems = filteredDocs.map { doc ->
                    FolderGridItem.Document(
                        documentNote = doc,
                        pages = bundle.docPagesMap[doc.id] ?: emptyList(),
                        linkGroupId = itemToLinkGroupMap[doc.id + 1_000_000_000L]?.id
                    )
                }

                val textNoteItems = filteredTextNotes.map { note ->
                    FolderGridItem.TextNoteItem(
                        textNote = note,
                        linkGroupId = itemToLinkGroupMap[note.id + 2_000_000_000L]?.id
                    )
                }

                val canvasNoteItems = filteredCanvasNotes.map { note ->
                    FolderGridItem.CanvasNoteItem(
                        canvasNote = note,
                        linkGroupId = itemToLinkGroupMap[note.id + 3_000_000_000L]?.id
                    )
                }

                val allItems = standaloneItems + groupItems + docItems + textNoteItems + canvasNoteItems

                val sortedItems = when (state.sortOption) {
                    PhotoSortOption.UPLOAD_DATE_DESC -> allItems.sortedWith(compareByDescending<FolderGridItem> { it.isPinned }.thenByDescending { it.sortCreatedAt })
                    PhotoSortOption.UPLOAD_DATE_ASC -> allItems.sortedWith(compareByDescending<FolderGridItem> { it.isPinned }.thenBy { it.sortCreatedAt })
                    PhotoSortOption.NEAREST_DEADLINE -> allItems.sortedWith(compareByDescending<FolderGridItem> { it.isPinned }.thenBy { it.sortDeadline ?: Long.MAX_VALUE })
                    PhotoSortOption.COLOR_LABEL -> allItems.sortedWith(compareByDescending<FolderGridItem> { it.isPinned }.thenBy { it.sortColor ?: "ZZZ" })
                }

                // Arrange linked items consecutively, anchored to highest-ranking member
                val finalGridItems = mutableListOf<FolderGridItem>()
                val visitedItemIds = mutableSetOf<Long>()
                val visitedLinkGroupIds = mutableSetOf<Long>()
                val itemMap = allItems.associateBy { it.itemId }

                for (item in sortedItems) {
                    if (visitedItemIds.contains(item.itemId)) continue
                    val lg = item.linkGroupId?.let { linkGroupMap[it] }

                    if (lg != null && !visitedLinkGroupIds.contains(lg.id)) {
                        visitedLinkGroupIds.add(lg.id)
                        for (mId in lg.memberIds) {
                            val memberItem = itemMap[mId]
                            if (memberItem != null && !visitedItemIds.contains(mId)) {
                                finalGridItems.add(memberItem)
                                visitedItemIds.add(mId)
                            }
                        }
                    } else if (lg == null) {
                        finalGridItems.add(item)
                        visitedItemIds.add(item.itemId)
                    }
                }

                val sortedPhotos = when (state.sortOption) {
                    PhotoSortOption.UPLOAD_DATE_DESC -> filteredPhotos.sortedWith(compareByDescending<Photo> { it.isPinned }.thenByDescending { it.addedAt })
                    PhotoSortOption.UPLOAD_DATE_ASC -> filteredPhotos.sortedWith(compareByDescending<Photo> { it.isPinned }.thenBy { it.addedAt })
                    PhotoSortOption.NEAREST_DEADLINE -> filteredPhotos.sortedWith(compareByDescending<Photo> { it.isPinned }.thenBy { it.linkedDeadline ?: Long.MAX_VALUE })
                    PhotoSortOption.COLOR_LABEL -> filteredPhotos.sortedWith(compareByDescending<Photo> { it.isPinned }.thenBy { it.tagColor ?: "ZZZ" })
                }

                GridRenderPayload(sortedPhotos, filteredGroups, filteredDocs, filteredTextNotes, filteredCanvasNotes, finalGridItems)
            }.collect { payload ->
                _uiState.update {
                    it.copy(
                        photos = payload.photos,
                        groups = payload.groups,
                        documents = payload.documents,
                        textNotes = payload.textNotes,
                        canvasNotes = payload.canvasNotes,
                        gridItems = payload.gridItems
                    )
                }
            }
        }
    }

    fun selectSubfolder(subfolderId: Long?) {
        _uiState.update { it.copy(selectedSubfolderId = subfolderId) }
    }

    fun setSortOption(option: PhotoSortOption) {
        _uiState.update { it.copy(sortOption = option) }
    }

    fun openAddSubfolderDialog() {
        _uiState.update { it.copy(showAddSubfolderDialog = true) }
    }

    fun closeAddSubfolderDialog() {
        _uiState.update { it.copy(showAddSubfolderDialog = false) }
    }

    fun createSubfolder(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            folderRepository.createSubfolder(folderId, name.trim())
            _uiState.update {
                it.copy(
                    showAddSubfolderDialog = false,
                    userMessage = "Subfolder \"$name\" created"
                )
            }
        }
    }

    fun renameFolder(newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) {
            _uiState.update { it.copy(userMessage = "Folder name cannot be empty") }
            return
        }
        viewModelScope.launch {
            folderRepository.renameFolder(folderId, trimmed)
            _uiState.update { it.copy(userMessage = "Renamed to \"$trimmed\"") }
        }
    }

    fun renameSubfolder(subfolderId: Long, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) {
            _uiState.update { it.copy(userMessage = "Subfolder name cannot be empty") }
            return
        }
        viewModelScope.launch {
            folderRepository.renameSubfolder(subfolderId, trimmed)
            _uiState.update { it.copy(userMessage = "Subfolder renamed to \"$trimmed\"") }
        }
    }

    fun updateFolderColor(colorHex: String) {
        viewModelScope.launch {
            folderRepository.updateFolderColor(folderId, colorHex)
            _uiState.update { it.copy(userMessage = "Folder label color updated") }
        }
    }

    fun deleteFolder(onDeleted: () -> Unit) {
        viewModelScope.launch {
            folderRepository.deleteFolder(folderId)
            onDeleted()
        }
    }

    suspend fun importGalleryUris(uris: List<Uri>): List<Photo> {
        val results = mutableListOf<Photo>()
        for (uri in uris) {
            try {
                val saved = photoStorageManager?.saveUriAsPhoto(uri) ?: continue
                val ocr = ocrEngine?.extractText(saved.filePath)
                val nextIndex = _uiState.value.photos.size + results.size + 1
                val newPhoto = Photo(
                    fileUri = saved.filePath,
                    thumbnailUri = saved.thumbnailPath,
                    folderId = folderId,
                    caption = "Imported Note #$nextIndex",
                    ocrText = ocr?.fullText,
                    source = PhotoSource.IMPORT,
                    fileSizeBytes = saved.fileSizeBytes
                )
                results.add(newPhoto)
            } catch (_: Exception) {}
        }
        return results
    }

    fun exportToPdf(context: Context, onReady: (File) -> Unit) {
        val gridItems = _uiState.value.gridItems
        val folderName = _uiState.value.folder?.name ?: "Folder"
        if (gridItems.isEmpty()) {
            _uiState.update { it.copy(userMessage = "No photos to export") }
            return
        }
        viewModelScope.launch {
            try {
                val pdfFile = PdfExporter.exportFolderGridToPdf(context, folderName, gridItems)
                onReady(pdfFile)
            } catch (e: Exception) {
                _uiState.update { it.copy(userMessage = "Failed to export PDF: ${e.message}") }
            }
        }
    }

    fun exportGroup(context: Context, group: PhotoGroup, members: List<Photo>, isZip: Boolean, onReady: (File) -> Unit) {
        if (members.isEmpty()) {
            _uiState.update { it.copy(userMessage = "No photos to export") }
            return
        }
        viewModelScope.launch {
            try {
                val exportedFile = if (isZip) {
                    ZipExporter.exportPhotosToZip(context, group.name, members)
                } else {
                    PdfExporter.exportPhotosToPdf(context, group.name, members)
                }
                onReady(exportedFile)
            } catch (e: Exception) {
                _uiState.update { it.copy(userMessage = "Export failed: ${e.message}") }
            }
        }
    }

    fun saveCapturedBatch(photos: List<Photo>, targetSubfolderId: Long?) {
        viewModelScope.launch {
            photos.forEach { photo ->
                val toSave = photo.copy(
                    folderId = folderId,
                    subfolderId = targetSubfolderId ?: photo.subfolderId
                )
                photoRepository.addPhoto(toSave)
            }
            _uiState.update {
                it.copy(userMessage = "Saved ${photos.size} note${if (photos.size > 1) "s" else ""} to folder")
            }
        }
    }

    fun toggleBatchSelectMode() {
        _uiState.update {
            val newMode = !it.isBatchSelectMode
            clearAllSelectionMaps()
            it.copy(
                isBatchSelectMode = newMode,
                selectedPhotoIds = emptySet(),
                selectedGroupIds = emptySet(),
                selectedDocumentIds = emptySet(),
                selectedTextNoteIds = emptySet(),
                selectedCanvasNoteIds = emptySet(),
                isSubfolderMultiSelectMode = if (newMode) false else it.isSubfolderMultiSelectMode,
                selectedSubfolderIds = if (newMode) emptySet() else it.selectedSubfolderIds
            )
        }
    }

    fun exitBatchSelectMode() {
        selectedPhotoMap.clear()
        selectedGroupMap.clear()
        selectedDocumentMap.clear()
        selectedTextNoteMap.clear()
        selectedCanvasNoteMap.clear()
        _uiState.update {
            it.copy(
                isBatchSelectMode = false,
                selectedPhotoIds = emptySet(),
                selectedGroupIds = emptySet(),
                selectedDocumentIds = emptySet(),
                selectedTextNoteIds = emptySet(),
                selectedCanvasNoteIds = emptySet()
            )
        }
    }

    fun selectAllPhotos() {
        _uiState.update {
            val photoIds = it.photos.filter { p -> p.groupId == null }.map { p -> p.id }.toSet()
            val groupIds = it.groups.map { g -> g.id }.toSet()
            val docIds = it.documents.map { d -> d.id }.toSet()
            val textIds = it.textNotes.map { tn -> tn.id }.toSet()
            val canvasIds = it.canvasNotes.map { cn -> cn.id }.toSet()

            selectedPhotoMap.clear()
            photoIds.forEach { id -> selectedPhotoMap[id] = true }
            selectedGroupMap.clear()
            groupIds.forEach { id -> selectedGroupMap[id] = true }
            selectedDocumentMap.clear()
            docIds.forEach { id -> selectedDocumentMap[id] = true }
            selectedTextNoteMap.clear()
            textIds.forEach { id -> selectedTextNoteMap[id] = true }
            selectedCanvasNoteMap.clear()
            canvasIds.forEach { id -> selectedCanvasNoteMap[id] = true }

            it.copy(
                selectedPhotoIds = photoIds,
                selectedGroupIds = groupIds,
                selectedDocumentIds = docIds,
                selectedTextNoteIds = textIds,
                selectedCanvasNoteIds = canvasIds
            )
        }
    }

    fun invertSelection() {
        _uiState.update { current ->
            val allEligiblePhotoIds = current.photos.filter { p -> p.groupId == null }.map { p -> p.id }.toSet()
            val allGroupIds = current.groups.map { g -> g.id }.toSet()
            val allDocumentIds = current.documents.map { d -> d.id }.toSet()
            val allTextNoteIds = current.textNotes.map { tn -> tn.id }.toSet()
            val allCanvasNoteIds = current.canvasNotes.map { cn -> cn.id }.toSet()

            val newPhotoIds = allEligiblePhotoIds - current.selectedPhotoIds
            val newGroupIds = allGroupIds - current.selectedGroupIds
            val newDocIds = allDocumentIds - current.selectedDocumentIds
            val newTextIds = allTextNoteIds - current.selectedTextNoteIds
            val newCanvasIds = allCanvasNoteIds - current.selectedCanvasNoteIds

            selectedPhotoMap.clear()
            newPhotoIds.forEach { id -> selectedPhotoMap[id] = true }
            selectedGroupMap.clear()
            newGroupIds.forEach { id -> selectedGroupMap[id] = true }
            selectedDocumentMap.clear()
            newDocIds.forEach { id -> selectedDocumentMap[id] = true }
            selectedTextNoteMap.clear()
            newTextIds.forEach { id -> selectedTextNoteMap[id] = true }
            selectedCanvasNoteMap.clear()
            newCanvasIds.forEach { id -> selectedCanvasNoteMap[id] = true }

            current.copy(
                selectedPhotoIds = newPhotoIds,
                selectedGroupIds = newGroupIds,
                selectedDocumentIds = newDocIds,
                selectedTextNoteIds = newTextIds,
                selectedCanvasNoteIds = newCanvasIds
            )
        }
    }

    fun selectAllSubfolders() {
        _uiState.update { current ->
            val allIds = current.subfolders.map { it.id }.toSet()
            selectedSubfolderMap.clear()
            allIds.forEach { id -> selectedSubfolderMap[id] = true }
            current.copy(selectedSubfolderIds = allIds)
        }
    }

    fun invertSubfolderSelection() {
        _uiState.update { current ->
            val allSubfolderIds = current.subfolders.map { it.id }.toSet()
            val newIds = allSubfolderIds - current.selectedSubfolderIds
            selectedSubfolderMap.clear()
            newIds.forEach { id -> selectedSubfolderMap[id] = true }
            current.copy(selectedSubfolderIds = newIds)
        }
    }

    fun togglePhotoSelection(photoId: Long) {
        if (selectedPhotoMap[photoId] == true) {
            selectedPhotoMap.remove(photoId)
        } else {
            selectedPhotoMap[photoId] = true
        }
        _uiState.update { current ->
            val set = current.selectedPhotoIds.toMutableSet()
            if (set.contains(photoId)) set.remove(photoId) else set.add(photoId)
            current.copy(selectedPhotoIds = set)
        }
    }

    fun toggleGroupSelection(groupId: Long) {
        if (selectedGroupMap[groupId] == true) {
            selectedGroupMap.remove(groupId)
        } else {
            selectedGroupMap[groupId] = true
        }
        _uiState.update { current ->
            val set = current.selectedGroupIds.toMutableSet()
            if (set.contains(groupId)) set.remove(groupId) else set.add(groupId)
            current.copy(selectedGroupIds = set)
        }
    }

    fun toggleDocumentSelection(docId: Long) {
        if (selectedDocumentMap[docId] == true) {
            selectedDocumentMap.remove(docId)
        } else {
            selectedDocumentMap[docId] = true
        }
        _uiState.update { current ->
            val set = current.selectedDocumentIds.toMutableSet()
            if (set.contains(docId)) set.remove(docId) else set.add(docId)
            current.copy(selectedDocumentIds = set)
        }
    }

    fun toggleTextNoteSelection(textNoteId: Long) {
        if (selectedTextNoteMap[textNoteId] == true) {
            selectedTextNoteMap.remove(textNoteId)
        } else {
            selectedTextNoteMap[textNoteId] = true
        }
        _uiState.update { current ->
            val set = current.selectedTextNoteIds.toMutableSet()
            if (set.contains(textNoteId)) set.remove(textNoteId) else set.add(textNoteId)
            current.copy(selectedTextNoteIds = set)
        }
    }

    fun startBatchSelection(photoId: Long) {
        clearAllSelectionMaps()
        selectedPhotoMap[photoId] = true
        _uiState.update {
            it.copy(
                isBatchSelectMode = true,
                selectedPhotoIds = setOf(photoId),
                selectedGroupIds = emptySet(),
                selectedDocumentIds = emptySet(),
                selectedTextNoteIds = emptySet(),
                selectedCanvasNoteIds = emptySet(),
                isSubfolderMultiSelectMode = false,
                selectedSubfolderIds = emptySet()
            )
        }
    }

    fun startBatchSelectionWithGroup(groupId: Long) {
        clearAllSelectionMaps()
        selectedGroupMap[groupId] = true
        _uiState.update {
            it.copy(
                isBatchSelectMode = true,
                selectedGroupIds = setOf(groupId),
                selectedPhotoIds = emptySet(),
                selectedDocumentIds = emptySet(),
                selectedTextNoteIds = emptySet(),
                selectedCanvasNoteIds = emptySet(),
                isSubfolderMultiSelectMode = false,
                selectedSubfolderIds = emptySet()
            )
        }
    }

    fun startBatchSelectionWithDocument(docId: Long) {
        clearAllSelectionMaps()
        selectedDocumentMap[docId] = true
        _uiState.update {
            it.copy(
                isBatchSelectMode = true,
                selectedDocumentIds = setOf(docId),
                selectedPhotoIds = emptySet(),
                selectedGroupIds = emptySet(),
                selectedTextNoteIds = emptySet(),
                selectedCanvasNoteIds = emptySet(),
                isSubfolderMultiSelectMode = false,
                selectedSubfolderIds = emptySet()
            )
        }
    }

    fun startBatchSelectionWithTextNote(textNoteId: Long) {
        clearAllSelectionMaps()
        selectedTextNoteMap[textNoteId] = true
        _uiState.update {
            it.copy(
                isBatchSelectMode = true,
                selectedTextNoteIds = setOf(textNoteId),
                selectedPhotoIds = emptySet(),
                selectedGroupIds = emptySet(),
                selectedDocumentIds = emptySet(),
                selectedCanvasNoteIds = emptySet(),
                isSubfolderMultiSelectMode = false,
                selectedSubfolderIds = emptySet()
            )
        }
    }

    fun toggleCanvasNoteSelection(canvasId: Long) {
        if (selectedCanvasNoteMap[canvasId] == true) {
            selectedCanvasNoteMap.remove(canvasId)
        } else {
            selectedCanvasNoteMap[canvasId] = true
        }
        _uiState.update { current ->
            val set = current.selectedCanvasNoteIds.toMutableSet()
            if (set.contains(canvasId)) set.remove(canvasId) else set.add(canvasId)
            current.copy(selectedCanvasNoteIds = set)
        }
    }

    fun togglePin(item: FolderGridItem) {
        val currentlyPinnedCount = _uiState.value.gridItems.count { it.isPinned }
        if (!item.isPinned && currentlyPinnedCount >= 4) {
            _uiState.update { it.copy(userMessage = "Maximum 4 pinned notes reached") }
            return
        }
        val newPinned = !item.isPinned
        viewModelScope.launch {
            when (item) {
                is FolderGridItem.StandalonePhoto -> photoRepository.setPhotoPinned(item.photo.id, newPinned)
                is FolderGridItem.Group -> photoRepository.setGroupPinned(item.group.id, newPinned)
                is FolderGridItem.Document -> documentRepository?.setDocumentPinned(item.documentNote.id, newPinned)
                is FolderGridItem.TextNoteItem -> textNoteRepository?.setNotePinned(item.textNote.id, newPinned)
                is FolderGridItem.CanvasNoteItem -> canvasNoteRepository?.setCanvasPinned(item.canvasNote.id, newPinned)
            }
        }
    }

    fun togglePin(photo: Photo) {
        val matchingItem = _uiState.value.gridItems.filterIsInstance<FolderGridItem.StandalonePhoto>().firstOrNull { it.photo.id == photo.id }
        if (matchingItem != null) {
            togglePin(matchingItem)
        } else {
            val currentlyPinnedCount = _uiState.value.gridItems.count { it.isPinned }
            if (!photo.isPinned && currentlyPinnedCount >= 4) {
                _uiState.update { it.copy(userMessage = "Maximum 4 pinned notes reached") }
                return
            }
            val newPinned = !photo.isPinned
            viewModelScope.launch {
                photoRepository.setPhotoPinned(photo.id, newPinned)
            }
        }
    }

    fun startBatchSelectionWithCanvasNote(canvasId: Long) {
        clearAllSelectionMaps()
        selectedCanvasNoteMap[canvasId] = true
        _uiState.update {
            it.copy(
                isBatchSelectMode = true,
                selectedCanvasNoteIds = setOf(canvasId),
                selectedPhotoIds = emptySet(),
                selectedGroupIds = emptySet(),
                selectedDocumentIds = emptySet(),
                selectedTextNoteIds = emptySet(),
                isSubfolderMultiSelectMode = false,
                selectedSubfolderIds = emptySet()
            )
        }
    }

    fun createGroupFromSelected(name: String) {
        val photoIds = _uiState.value.selectedPhotoIds.toList()
        if (photoIds.size < 2 || name.trim().isBlank()) return
        viewModelScope.launch {
            photoRepository.createGroup(
                folderId = folderId,
                subfolderId = _uiState.value.selectedSubfolderId,
                name = name.trim(),
                photoIds = photoIds
            )
            exitBatchSelectMode()
            _uiState.update { it.copy(userMessage = "Group created: $name") }
        }
    }

    fun renameGroup(groupId: Long, newName: String) {
        if (newName.trim().isBlank()) return
        viewModelScope.launch {
            photoRepository.renameGroup(groupId, newName.trim())
            _uiState.update { it.copy(userMessage = "Group renamed") }
        }
    }

    fun updateGroupTagColor(groupId: Long, colorHex: String?) {
        viewModelScope.launch {
            photoRepository.updateGroupTagColor(groupId, colorHex)
            _uiState.update { it.copy(userMessage = "Updated group color") }
        }
    }

    fun ungroup(groupId: Long) {
        val grp = _uiState.value.groups.firstOrNull { it.id == groupId }
        val memberIds = _uiState.value.photos.filter { it.groupId == groupId }.map { it.id }
        viewModelScope.launch {
            photoRepository.ungroup(groupId)
            _uiState.update {
                it.copy(
                    pendingUndoAction = if (grp != null) com.arinara.fotara.ui.folder.UndoAction.Ungroup(grp, memberIds) else null,
                    userMessage = "Group dissolved"
                )
            }
        }
    }

    fun mergeSelectedGroups(name: String) {
        val groupIds = _uiState.value.selectedGroupIds.toList()
        if (groupIds.size < 2 || name.trim().isBlank()) return
        viewModelScope.launch {
            val newGroupId = photoRepository.mergeGroups(
                sourceGroupIds = groupIds,
                newName = name.trim(),
                targetFolderId = folderId,
                targetSubfolderId = _uiState.value.selectedSubfolderId
            )
            recordRecentDestination(
                type = DestinationType.GROUP,
                targetFolderId = folderId,
                targetSubfolderId = _uiState.value.selectedSubfolderId,
                targetGroupId = newGroupId,
                title = name.trim(),
                subtitle = _uiState.value.folder?.name
            )
            exitBatchSelectMode()
            _uiState.update { it.copy(userMessage = "Merged ${groupIds.size} groups into \"${name.trim()}\"") }
        }
    }

    fun addSelectedPhotosToGroup(targetGroupId: Long, groupName: String? = null) {
        val photoIds = _uiState.value.selectedPhotoIds.toList()
        if (photoIds.isEmpty()) return
        viewModelScope.launch {
            photoRepository.addPhotosToExistingGroup(targetGroupId, photoIds)
            if (groupName != null) {
                recordRecentDestination(
                    type = DestinationType.GROUP,
                    targetFolderId = folderId,
                    targetSubfolderId = _uiState.value.selectedSubfolderId,
                    targetGroupId = targetGroupId,
                    title = groupName,
                    subtitle = _uiState.value.folder?.name
                )
            }
            exitBatchSelectMode()
            _uiState.update { it.copy(userMessage = "Added ${photoIds.size} note${if (photoIds.size > 1) "s" else ""} to group") }
        }
    }

    fun createLinkGroup() {
        val photoIds = _uiState.value.selectedPhotoIds.toList()
        val groupIds = _uiState.value.selectedGroupIds.toList()
        val docIds = _uiState.value.selectedDocumentIds.toList()
        val textNoteIds = _uiState.value.selectedTextNoteIds.toList()
        val totalCount = photoIds.size + groupIds.size + docIds.size + textNoteIds.size
        if (totalCount !in 2..4) {
            _uiState.update { it.copy(userMessage = "Select between 2 and 4 items to link") }
            return
        }
        val itemIds = photoIds + groupIds.map { -it } + docIds.map { it + 1_000_000_000L } + textNoteIds.map { it + 2_000_000_000L }
        viewModelScope.launch {
            photoRepository.createGridLinkGroup(itemIds)
            exitBatchSelectMode()
            _uiState.update { it.copy(userMessage = "Items linked") }
        }
    }

    fun unlinkGridItem(itemId: Long) {
        viewModelScope.launch {
            photoRepository.unlinkGridItem(itemId)
            _uiState.update { it.copy(userMessage = "Item unlinked") }
        }
    }

    fun deleteGroup(groupId: Long) {
        val grp = _uiState.value.groups.firstOrNull { it.id == groupId }
        val memberPhotos = _uiState.value.photos.filter { it.groupId == groupId }
        viewModelScope.launch {
            photoRepository.deleteGroup(groupId)
            _uiState.update {
                it.copy(
                    pendingUndoAction = if (grp != null) UndoAction.Delete(memberPhotos, listOf(grp)) else null,
                    userMessage = "Group moved to Trash"
                )
            }
        }
    }

    fun removePhotoFromGroup(photoId: Long) {
        viewModelScope.launch {
            photoRepository.removePhotoFromGroup(photoId)
            _uiState.update { it.copy(userMessage = "Photo removed from group") }
        }
    }

    fun addPhotosToGroup(groupId: Long, photoIds: List<Long>) {
        if (photoIds.isEmpty()) return
        viewModelScope.launch {
            photoRepository.addPhotosToGroup(groupId, photoIds)
            _uiState.update {
                it.copy(userMessage = "Added ${photoIds.size} note${if (photoIds.size > 1) "s" else ""} to group")
            }
        }
    }

    fun startSubfolderMultiSelect(subfolderId: Long) {
        clearAllSelectionMaps()
        selectedSubfolderMap[subfolderId] = true
        _uiState.update {
            it.copy(
                isSubfolderMultiSelectMode = true,
                selectedSubfolderIds = setOf(subfolderId),
                isBatchSelectMode = false,
                selectedPhotoIds = emptySet(),
                selectedGroupIds = emptySet(),
                selectedDocumentIds = emptySet(),
                selectedTextNoteIds = emptySet(),
                selectedCanvasNoteIds = emptySet()
            )
        }
    }

    fun toggleSubfolderSelection(subfolderId: Long) {
        if (selectedSubfolderMap[subfolderId] == true) {
            selectedSubfolderMap.remove(subfolderId)
        } else {
            selectedSubfolderMap[subfolderId] = true
        }
        _uiState.update { current ->
            val set = current.selectedSubfolderIds.toMutableSet()
            if (set.contains(subfolderId)) set.remove(subfolderId) else set.add(subfolderId)
            current.copy(selectedSubfolderIds = set)
        }
    }

    fun exitSubfolderMultiSelect() {
        selectedSubfolderMap.clear()
        _uiState.update {
            it.copy(
                isSubfolderMultiSelectMode = false,
                selectedSubfolderIds = emptySet()
            )
        }
    }

    fun getSubfolderDeleteStats(subfolderIds: List<Long>, onResult: (SubfolderDeleteResult) -> Unit) {
        viewModelScope.launch {
            val stats = folderRepository.getSubfolderDeleteStats(subfolderIds)
            onResult(stats)
        }
    }

    fun deleteSubfolder(subfolderId: Long) {
        viewModelScope.launch {
            folderRepository.deleteSubfolder(subfolderId)
            _uiState.update {
                it.copy(
                    selectedSubfolderId = if (it.selectedSubfolderId == subfolderId) null else it.selectedSubfolderId,
                    userMessage = "Subfolder moved to Trash"
                )
            }
        }
    }

    fun deleteSelectedSubfolders() {
        val ids = _uiState.value.selectedSubfolderIds.toList()
        if (ids.isEmpty()) return
        selectedSubfolderMap.clear()
        viewModelScope.launch {
            val result = folderRepository.deleteSubfolders(ids)
            _uiState.update {
                it.copy(
                    isSubfolderMultiSelectMode = false,
                    selectedSubfolderIds = emptySet(),
                    selectedSubfolderId = if (it.selectedSubfolderId in ids) null else it.selectedSubfolderId,
                    userMessage = "Moved ${result.subfolderCount} subfolder${if (result.subfolderCount > 1) "s" else ""} to Trash"
                )
            }
        }
    }

    fun deleteSelectedPhotos() {
        val photoIds = _uiState.value.selectedPhotoIds.toList()
        val groupIds = _uiState.value.selectedGroupIds.toList()
        val docIds = _uiState.value.selectedDocumentIds.toList()
        val textNoteIds = _uiState.value.selectedTextNoteIds.toList()
        val canvasNoteIds = _uiState.value.selectedCanvasNoteIds.toList()
        if (photoIds.isEmpty() && groupIds.isEmpty() && docIds.isEmpty() && textNoteIds.isEmpty() && canvasNoteIds.isEmpty()) return

        val photosToTrash = _uiState.value.photos.filter { it.id in photoIds }
        val groupsToTrash = _uiState.value.groups.filter { it.id in groupIds }
        val docsToTrash = _uiState.value.documents.filter { it.id in docIds }
        val textNotesToTrash = _uiState.value.textNotes.filter { it.id in textNoteIds }

        viewModelScope.launch {
            if (photoIds.isNotEmpty()) {
                photoIds.forEach { deadlineNotificationManager?.cancelReminder(it) }
                photoRepository.deletePhotos(photoIds)
            }
            if (groupIds.isNotEmpty()) {
                photoRepository.deleteGroups(groupIds)
            }
            if (docIds.isNotEmpty()) {
                docIds.forEach { deadlineNotificationManager?.cancelReminder(it + 1_000_000_000L) }
                docIds.forEach { documentRepository?.deleteDocumentNote(it) }
            }
            if (textNoteIds.isNotEmpty()) {
                textNoteIds.forEach { deadlineNotificationManager?.cancelReminder(it + 2_000_000_000L) }
                textNoteIds.forEach { textNoteRepository?.deleteTextNote(it) }
            }
            if (canvasNoteIds.isNotEmpty()) {
                canvasNoteRepository?.deleteCanvasNotes(canvasNoteIds)
            }
            exitBatchSelectMode()
            val total = photoIds.size + groupIds.size + docIds.size + textNoteIds.size + canvasNoteIds.size
            _uiState.update {
                it.copy(
                    pendingUndoAction = UndoAction.Delete(photosToTrash, groupsToTrash, docsToTrash, textNotesToTrash),
                    userMessage = "Moved $total item${if (total > 1) "s" else ""} to Trash"
                )
            }
        }
    }

    fun moveSelectedPhotos(targetFolderId: Long, targetSubfolderId: Long? = null, targetTitle: String? = null) {
        val photoIds = _uiState.value.selectedPhotoIds.toList()
        val groupIds = _uiState.value.selectedGroupIds.toList()
        val docIds = _uiState.value.selectedDocumentIds.toList()
        val textNoteIds = _uiState.value.selectedTextNoteIds.toList()
        val canvasNoteIds = _uiState.value.selectedCanvasNoteIds.toList()
        if (photoIds.isEmpty() && groupIds.isEmpty() && docIds.isEmpty() && textNoteIds.isEmpty() && canvasNoteIds.isEmpty()) return

        val currentPhotos = _uiState.value.photos.filter { it.id in photoIds }
        val currentGroups = _uiState.value.groups.filter { it.id in groupIds }
        val currentDocs = _uiState.value.documents.filter { it.id in docIds }
        val currentTextNotes = _uiState.value.textNotes.filter { it.id in textNoteIds }
        val photoMoves = currentPhotos.map { Triple(it.id, it.folderId, it.subfolderId) }
        val groupMoves = currentGroups.map { Triple(it.id, it.folderId, it.subfolderId) }
        val docMoves = currentDocs.map { Triple(it.id, it.folderId, it.subfolderId) }
        val textNoteMoves = currentTextNotes.map { Triple(it.id, it.folderId, it.subfolderId) }

        viewModelScope.launch {
            if (photoIds.isNotEmpty()) {
                photoRepository.movePhotos(photoIds, targetFolderId, targetSubfolderId)
            }
            if (groupIds.isNotEmpty()) {
                photoRepository.moveGroups(groupIds, targetFolderId, targetSubfolderId)
            }
            if (docIds.isNotEmpty()) {
                docIds.forEach { documentRepository?.moveDocumentNote(it, targetFolderId, targetSubfolderId) }
            }
            if (textNoteIds.isNotEmpty()) {
                textNoteIds.forEach { textNoteRepository?.moveTextNote(it, targetFolderId, targetSubfolderId) }
            }
            if (canvasNoteIds.isNotEmpty()) {
                canvasNoteRepository?.moveCanvasNotes(canvasNoteIds, targetFolderId, targetSubfolderId)
            }
            if (targetTitle != null) {
                recordRecentDestination(
                    type = if (targetSubfolderId != null) DestinationType.SUBFOLDER else DestinationType.FOLDER,
                    targetFolderId = targetFolderId,
                    targetSubfolderId = targetSubfolderId,
                    targetGroupId = null,
                    title = targetTitle
                )
            }
            exitBatchSelectMode()
            val total = photoIds.size + groupIds.size + docIds.size + textNoteIds.size + canvasNoteIds.size
            _uiState.update {
                it.copy(
                    pendingUndoAction = UndoAction.Move(photoMoves, groupMoves, docMoves, textNoteMoves),
                    userMessage = "Moved $total item${if (total > 1) "s" else ""}"
                )
            }
        }
    }

    fun moveGroup(groupId: Long, targetFolderId: Long, targetSubfolderId: Long? = null, targetTitle: String? = null) {
        val currentGroup = _uiState.value.groups.firstOrNull { it.id == groupId }
        val originalFolderId = currentGroup?.folderId ?: folderId
        val originalSubfolderId = currentGroup?.subfolderId

        viewModelScope.launch {
            photoRepository.moveGroup(groupId, targetFolderId, targetSubfolderId)
            if (targetTitle != null) {
                recordRecentDestination(
                    type = if (targetSubfolderId != null) DestinationType.SUBFOLDER else DestinationType.FOLDER,
                    targetFolderId = targetFolderId,
                    targetSubfolderId = targetSubfolderId,
                    targetGroupId = null,
                    title = targetTitle
                )
            }
            _uiState.update {
                it.copy(
                    pendingUndoAction = com.arinara.fotara.ui.folder.UndoAction.Move(
                        photoMoves = emptyList(),
                        groupMoves = listOf(Triple(groupId, originalFolderId, originalSubfolderId))
                    ),
                    userMessage = "Group moved"
                )
            }
        }
    }

    fun copyPhoto(photoId: Long, targetFolderId: Long, targetSubfolderId: Long? = null, targetGroupId: Long? = null, targetTitle: String? = null) {
        viewModelScope.launch {
            photoRepository.copyPhoto(photoId, targetFolderId, targetSubfolderId, targetGroupId)
            if (targetTitle != null) {
                recordRecentDestination(
                    type = if (targetGroupId != null) DestinationType.GROUP else if (targetSubfolderId != null) DestinationType.SUBFOLDER else DestinationType.FOLDER,
                    targetFolderId = targetFolderId,
                    targetSubfolderId = targetSubfolderId,
                    targetGroupId = targetGroupId,
                    title = targetTitle
                )
            }
            _uiState.update { it.copy(userMessage = "Photo copied") }
        }
    }

    fun undoLastAction() {
        val action = _uiState.value.pendingUndoAction ?: return
        viewModelScope.launch {
            when (action) {
                is UndoAction.Delete -> {
                    for (photo in action.photos) {
                        photoRepository.restorePhoto(photo.id, photo.folderId, photo.subfolderId)
                    }
                    for (group in action.groups) {
                        photoRepository.restoreGroup(group.id)
                    }
                    for (doc in action.documents) {
                        documentRepository?.restoreDocumentNote(doc.id)
                    }
                    for (textNote in action.textNotes) {
                        textNoteRepository?.restoreTextNote(textNote.id)
                    }
                    _uiState.update { it.copy(pendingUndoAction = null, userMessage = "Undo: Items restored") }
                }
                is UndoAction.Move -> {
                    for ((photoId, origFolder, origSub) in action.photoMoves) {
                        photoRepository.movePhotos(listOf(photoId), origFolder, origSub)
                    }
                    for ((groupId, origFolder, origSub) in action.groupMoves) {
                        photoRepository.moveGroups(listOf(groupId), origFolder, origSub)
                    }
                    for ((docId, origFolder, origSub) in action.documentMoves) {
                        documentRepository?.moveDocumentNote(docId, origFolder, origSub)
                    }
                    for ((textNoteId, origFolder, origSub) in action.textNoteMoves) {
                        textNoteRepository?.moveTextNote(textNoteId, origFolder, origSub)
                    }
                    _uiState.update { it.copy(pendingUndoAction = null, userMessage = "Undo: Items moved back") }
                }
                is UndoAction.Ungroup -> {
                    val g = action.originalGroup
                    photoRepository.createGroup(
                        folderId = g.folderId,
                        subfolderId = g.subfolderId,
                        name = g.name,
                        photoIds = action.memberPhotoIds,
                        tagColor = g.tagColor
                    )
                    _uiState.update { it.copy(pendingUndoAction = null, userMessage = "Undo: Group restored") }
                }
            }
        }
    }

    fun clearPendingUndo() {
        _uiState.update { it.copy(pendingUndoAction = null) }
    }

    fun refreshRecentDestinations() {
        val recents = settingsRepository?.getRecentDestinations() ?: emptyList()
        _uiState.update { it.copy(recentDestinations = recents) }
    }

    private fun recordRecentDestination(
        type: DestinationType,
        targetFolderId: Long,
        targetSubfolderId: Long?,
        targetGroupId: Long?,
        title: String,
        subtitle: String? = null
    ) {
        viewModelScope.launch {
            settingsRepository?.addRecentDestination(
                RecentDestination(
                    type = type,
                    folderId = targetFolderId,
                    subfolderId = targetSubfolderId,
                    groupId = targetGroupId,
                    title = title,
                    subtitle = subtitle
                )
            )
            refreshRecentDestinations()
        }
    }

    fun exportSelected(context: Context, isZip: Boolean, onReady: (File) -> Unit) {
        val folderName = _uiState.value.folder?.name ?: "Folder"
        val selectedPhotos = _uiState.value.photos.filter { it.id in _uiState.value.selectedPhotoIds }
        val selectedGroups = _uiState.value.groups.filter { it.id in _uiState.value.selectedGroupIds }
        val selectedDocs = _uiState.value.documents.filter { it.id in _uiState.value.selectedDocumentIds }

        val itemsToExport = if (selectedPhotos.isNotEmpty() || selectedGroups.isNotEmpty() || selectedDocs.isNotEmpty()) {
            val standalone = selectedPhotos.map { FolderGridItem.StandalonePhoto(it) }
            val groups = selectedGroups.map { g ->
                val members = _uiState.value.photos.filter { it.groupId == g.id }
                FolderGridItem.Group(g, members, members.firstOrNull())
            }
            val docs = selectedDocs.map { d ->
                FolderGridItem.Document(d, emptyList())
            }
            standalone + groups + docs
        } else {
            _uiState.value.gridItems
        }

        if (itemsToExport.isEmpty()) {
            _uiState.update { it.copy(userMessage = "No items to export") }
            return
        }

        viewModelScope.launch {
            try {
                val file = if (isZip) {
                    ZipExporter.exportGridItemsToZip(context, folderName, itemsToExport)
                } else {
                    PdfExporter.exportFolderGridToPdf(context, folderName, itemsToExport)
                }
                onReady(file)
            } catch (e: Exception) {
                _uiState.update { it.copy(userMessage = "Export failed: ${e.message}") }
            }
        }
    }

    fun updateSelectedPhotosTagColor(colorHex: String?) {
        val photoIds = _uiState.value.selectedPhotoIds.toList()
        val groupIds = _uiState.value.selectedGroupIds.toList()
        val docIds = _uiState.value.selectedDocumentIds.toList()
        val textNoteIds = _uiState.value.selectedTextNoteIds.toList()
        if (photoIds.isEmpty() && groupIds.isEmpty() && docIds.isEmpty() && textNoteIds.isEmpty()) return
        viewModelScope.launch {
            if (photoIds.isNotEmpty()) {
                photoRepository.updatePhotosTagColor(photoIds, colorHex)
            }
            for (gId in groupIds) {
                photoRepository.updateGroupTagColor(gId, colorHex)
            }
            for (dId in docIds) {
                documentRepository?.updateDocumentTagColor(dId, colorHex)
            }
            for (tnId in textNoteIds) {
                textNoteRepository?.updateTagColor(tnId, colorHex)
            }
            exitBatchSelectMode()
            _uiState.update { it.copy(userMessage = "Updated tag color") }
        }
    }

    fun batchRename(baseName: String) {
        if (baseName.isBlank()) return
        val selectedPhotoIds = _uiState.value.selectedPhotoIds
        val selectedGroupIds = _uiState.value.selectedGroupIds
        val selectedDocumentIds = _uiState.value.selectedDocumentIds
        val selectedTextNoteIds = _uiState.value.selectedTextNoteIds
        val selectedCanvasNoteIds = _uiState.value.selectedCanvasNoteIds
        val totalCount = selectedPhotoIds.size + selectedGroupIds.size + selectedDocumentIds.size + selectedTextNoteIds.size + selectedCanvasNoteIds.size
        if (totalCount == 0) return

        viewModelScope.launch {
            val orderedSelected = _uiState.value.gridItems.filter { item ->
                when (item) {
                    is FolderGridItem.StandalonePhoto -> item.photo.id in selectedPhotoIds
                    is FolderGridItem.Group -> item.group.id in selectedGroupIds
                    is FolderGridItem.Document -> item.documentNote.id in selectedDocumentIds
                    is FolderGridItem.TextNoteItem -> item.textNote.id in selectedTextNoteIds
                    is FolderGridItem.CanvasNoteItem -> item.canvasNote.id in selectedCanvasNoteIds
                }
            }

            if (totalCount == 1) {
                val singleItem = orderedSelected.firstOrNull() ?: return@launch
                when (singleItem) {
                    is FolderGridItem.StandalonePhoto -> photoRepository.renamePhoto(singleItem.photo.id, baseName.trim())
                    is FolderGridItem.Group -> photoRepository.renameGroup(singleItem.group.id, baseName.trim())
                    is FolderGridItem.Document -> documentRepository?.renameDocumentNote(singleItem.documentNote.id, baseName.trim())
                    is FolderGridItem.TextNoteItem -> textNoteRepository?.renameTextNote(singleItem.textNote.id, baseName.trim())
                    is FolderGridItem.CanvasNoteItem -> canvasNoteRepository?.renameCanvasNote(singleItem.canvasNote.id, baseName.trim())
                }
            } else {
                var index = 1
                for (item in orderedSelected) {
                    val newName = "${baseName.trim()} $index"
                    when (item) {
                        is FolderGridItem.StandalonePhoto -> photoRepository.renamePhoto(item.photo.id, newName)
                        is FolderGridItem.Group -> photoRepository.renameGroup(item.group.id, newName)
                        is FolderGridItem.Document -> documentRepository?.renameDocumentNote(item.documentNote.id, newName)
                        is FolderGridItem.TextNoteItem -> textNoteRepository?.renameTextNote(item.textNote.id, newName)
                        is FolderGridItem.CanvasNoteItem -> canvasNoteRepository?.renameCanvasNote(item.canvasNote.id, newName)
                    }
                    index++
                }
            }
            exitBatchSelectMode()
            _uiState.update { it.copy(userMessage = "Renamed $totalCount item${if (totalCount > 1) "s" else ""}") }
        }
    }

    private var currentImportJob: Job? = null

    fun cancelImport() {
        currentImportJob?.cancel()
        currentImportJob = null
        _uiState.update { it.copy(isLoading = false, importProgress = null, userMessage = "Import cancelled") }
    }

    fun importDocument(uri: Uri, name: String, isPdf: Boolean) {
        currentImportJob?.cancel()
        currentImportJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, importProgress = if (isPdf) Pair(0, 1) else null) }
            try {
                if (isPdf) {
                    documentRepository?.importPdf(
                        uri = uri,
                        folderId = folderId,
                        subfolderId = _uiState.value.selectedSubfolderId,
                        name = name,
                        onProgress = { current, total ->
                            _uiState.update { it.copy(importProgress = Pair(current, total)) }
                        }
                    )
                } else {
                    documentRepository?.importDocx(
                        uri = uri,
                        folderId = folderId,
                        subfolderId = _uiState.value.selectedSubfolderId,
                        name = name
                    )
                }
                _uiState.update { it.copy(isLoading = false, importProgress = null, userMessage = "Document imported") }
            } catch (_: kotlinx.coroutines.CancellationException) {
                _uiState.update { it.copy(isLoading = false, importProgress = null, userMessage = "Import cancelled") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, importProgress = null, userMessage = "Import failed: ${e.message}") }
            } finally {
                currentImportJob = null
            }
        }
    }

    private var currentSplitJob: Job? = null

    fun cancelSplit() {
        currentSplitJob?.cancel()
        currentSplitJob = null
        _uiState.update { it.copy(isLoading = false, splitProgress = null, userMessage = "Split cancelled") }
    }

    fun splitPdfToImages(docId: Long) {
        currentSplitJob?.cancel()
        currentSplitJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, splitProgress = Pair(0, 1)) }
            try {
                val createdIds = documentRepository?.splitPdfToImages(docId) { current, total ->
                    _uiState.update { it.copy(splitProgress = Pair(current, total)) }
                } ?: emptyList()
                val msg = if (createdIds.size >= 5) {
                    "Split into Photo Group with ${createdIds.size} pages"
                } else {
                    "Split into ${createdIds.size} photos"
                }
                _uiState.update { it.copy(isLoading = false, splitProgress = null, userMessage = msg) }
            } catch (_: kotlinx.coroutines.CancellationException) {
                _uiState.update { it.copy(isLoading = false, splitProgress = null, userMessage = "Split cancelled") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, splitProgress = null, userMessage = "Split failed: ${e.message}") }
            } finally {
                currentSplitJob = null
            }
        }
    }

    fun renameDocument(docId: Long, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            documentRepository?.renameDocumentNote(docId, newTitle.trim())
            _uiState.update { it.copy(userMessage = "Document renamed") }
        }
    }

    fun updateDocumentTagColor(docId: Long, colorHex: String?) {
        viewModelScope.launch {
            documentRepository?.updateDocumentTagColor(docId, colorHex)
            _uiState.update { it.copy(userMessage = "Document color updated") }
        }
    }

    fun setDocumentDeadline(docId: Long, deadlineMs: Long?) {
        viewModelScope.launch {
            documentRepository?.updateDocumentDeadline(docId, deadlineMs)
            val doc = _uiState.value.documents.firstOrNull { it.id == docId }
            val folderName = _uiState.value.folder?.name ?: "Coursework"
            if (deadlineMs != null && doc != null) {
                deadlineNotificationManager?.scheduleReminder(doc.id + 1_000_000_000L, folderName, doc.name, deadlineMs)
            } else {
                deadlineNotificationManager?.cancelReminder(docId + 1_000_000_000L)
            }
            val msg = if (deadlineMs != null) "Deadline attached" else "Deadline removed"
            _uiState.update { it.copy(userMessage = msg) }
        }
    }

    fun deleteDocument(docId: Long) {
        val doc = _uiState.value.documents.firstOrNull { it.id == docId }
        viewModelScope.launch {
            deadlineNotificationManager?.cancelReminder(docId + 1_000_000_000L)
            documentRepository?.deleteDocumentNote(docId)
            _uiState.update {
                it.copy(
                    pendingUndoAction = if (doc != null) UndoAction.Delete(emptyList(), emptyList(), listOf(doc)) else null,
                    userMessage = "Document moved to Trash"
                )
            }
        }
    }

    fun moveDocument(docId: Long, targetFolderId: Long, targetSubfolderId: Long? = null, targetTitle: String? = null) {
        val doc = _uiState.value.documents.firstOrNull { it.id == docId }
        val origFolderId = doc?.folderId ?: folderId
        val origSubfolderId = doc?.subfolderId
        viewModelScope.launch {
            documentRepository?.moveDocumentNote(docId, targetFolderId, targetSubfolderId)
            _uiState.update {
                it.copy(
                    pendingUndoAction = UndoAction.Move(emptyList(), emptyList(), listOf(Triple(docId, origFolderId, origSubfolderId))),
                    userMessage = "Document moved"
                )
            }
        }
    }

    fun renameTextNote(textNoteId: Long, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            textNoteRepository?.renameTextNote(textNoteId, newTitle.trim())
            _uiState.update { it.copy(userMessage = "Note renamed") }
        }
    }

    fun updateTextNoteTagColor(textNoteId: Long, colorHex: String?) {
        viewModelScope.launch {
            textNoteRepository?.updateTagColor(textNoteId, colorHex)
            _uiState.update { it.copy(userMessage = "Note color updated") }
        }
    }

    fun setTextNoteDeadline(textNoteId: Long, deadlineMs: Long?) {
        viewModelScope.launch {
            textNoteRepository?.updateDeadline(textNoteId, deadlineMs)
            val note = _uiState.value.textNotes.firstOrNull { it.id == textNoteId }
            val folderName = _uiState.value.folder?.name ?: "Coursework"
            if (deadlineMs != null && note != null) {
                deadlineNotificationManager?.scheduleReminder(note.id + 2_000_000_000L, folderName, note.title, deadlineMs)
            } else {
                deadlineNotificationManager?.cancelReminder(textNoteId + 2_000_000_000L)
            }
            val msg = if (deadlineMs != null) "Deadline attached" else "Deadline removed"
            _uiState.update { it.copy(userMessage = msg) }
        }
    }

    fun deleteTextNote(textNoteId: Long) {
        val note = _uiState.value.textNotes.firstOrNull { it.id == textNoteId }
        viewModelScope.launch {
            deadlineNotificationManager?.cancelReminder(textNoteId + 2_000_000_000L)
            textNoteRepository?.deleteTextNote(textNoteId)
            _uiState.update {
                it.copy(
                    pendingUndoAction = if (note != null) UndoAction.Delete(emptyList(), emptyList(), emptyList(), listOf(note)) else null,
                    userMessage = "Note moved to Trash"
                )
            }
        }
    }

    fun moveTextNote(textNoteId: Long, targetFolderId: Long, targetSubfolderId: Long? = null) {
        val note = _uiState.value.textNotes.firstOrNull { it.id == textNoteId }
        val origFolderId = note?.folderId ?: folderId
        val origSubfolderId = note?.subfolderId
        viewModelScope.launch {
            textNoteRepository?.moveTextNote(textNoteId, targetFolderId, targetSubfolderId)
            _uiState.update {
                it.copy(
                    pendingUndoAction = UndoAction.Move(emptyList(), emptyList(), emptyList(), listOf(Triple(textNoteId, origFolderId, origSubfolderId))),
                    userMessage = "Note moved"
                )
            }
        }
    }

    fun movePhotoToSubfolder(photoId: Long, subfolderId: Long?) {
        viewModelScope.launch {
            val photo = _uiState.value.photos.firstOrNull { it.id == photoId }
            if (photo != null) {
                photoRepository.updatePhoto(photo.copy(subfolderId = subfolderId))
                _uiState.update { it.copy(userMessage = "Moved note") }
            }
        }
    }

    fun updatePhotoTagColor(photoId: Long, colorHex: String?) {
        viewModelScope.launch {
            val photo = _uiState.value.photos.firstOrNull { it.id == photoId }
            if (photo != null) {
                photoRepository.updatePhoto(photo.copy(tagColor = colorHex))
                _uiState.update { it.copy(userMessage = "Updated note tag color") }
            }
        }
    }

    fun setPhotoDeadline(photoId: Long, deadlineMs: Long?) {
        viewModelScope.launch {
            val photo = _uiState.value.photos.firstOrNull { it.id == photoId }
            if (photo != null) {
                photoRepository.updatePhoto(photo.copy(linkedDeadline = deadlineMs))
                val folderName = _uiState.value.folder?.name ?: "Coursework"
                if (deadlineMs != null) {
                    deadlineNotificationManager?.scheduleReminder(photo.id, folderName, photo.caption, deadlineMs)
                } else {
                    deadlineNotificationManager?.cancelReminder(photo.id)
                }
                val msg = if (deadlineMs != null) "Deadline attached" else "Deadline removed"
                _uiState.update { it.copy(userMessage = msg) }
            }
        }
    }

    private var isRefreshingData = false

    fun refresh() {
        if (isRefreshingData) return
        isRefreshingData = true
        _uiState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            try {
                kotlinx.coroutines.withTimeoutOrNull(com.arinara.fotara.ui.components.PullToRefreshHelper.REFRESH_TIMEOUT_MS) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        folderRepository.refresh()
                        photoRepository.refresh()
                        documentRepository?.refresh()
                        textNoteRepository?.refresh()
                        canvasNoteRepository?.refresh()
                    }
                }
            } catch (_: Exception) {
            } finally {
                isRefreshingData = false
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun deleteCanvasNote(canvasId: Long) {
        viewModelScope.launch {
            canvasNoteRepository?.deleteCanvasNote(canvasId)
            _uiState.update { it.copy(userMessage = "Canvas note moved to Trash") }
        }
    }

    fun renameCanvasNote(canvasId: Long, newTitle: String) {
        viewModelScope.launch {
            canvasNoteRepository?.renameCanvasNote(canvasId, newTitle)
            _uiState.update { it.copy(userMessage = "Canvas note renamed") }
        }
    }

    fun setCanvasNoteDeadline(canvasId: Long, deadlineMs: Long?) {
        viewModelScope.launch {
            canvasNoteRepository?.updateDeadline(canvasId, deadlineMs)
            val msg = if (deadlineMs != null) "Deadline attached" else "Deadline removed"
            _uiState.update { it.copy(userMessage = msg) }
        }
    }

    fun moveCanvasNote(canvasId: Long, targetFolderId: Long, targetSubfolderId: Long?) {
        viewModelScope.launch {
            canvasNoteRepository?.moveCanvasNote(canvasId, targetFolderId, targetSubfolderId)
            _uiState.update { it.copy(userMessage = "Canvas note moved") }
        }
    }

    fun updateCanvasNoteTagColor(canvasId: Long, colorHex: String?) {
        viewModelScope.launch {
            canvasNoteRepository?.updateTagColor(canvasId, colorHex)
            _uiState.update { it.copy(userMessage = "Color label updated") }
        }
    }

    fun deletePhoto(photoId: Long) {
        viewModelScope.launch {
            deadlineNotificationManager?.cancelReminder(photoId)
            photoRepository.deletePhoto(photoId)
            _uiState.update { it.copy(userMessage = "Note moved to Trash") }
        }
    }

    fun renamePhoto(photoId: Long, newCaption: String) {
        viewModelScope.launch {
            photoRepository.renamePhoto(photoId, newCaption)
            _uiState.update { it.copy(userMessage = "Photo renamed") }
        }
    }

    fun updatePhotoNote(photoId: Long, newNote: String?) {
        viewModelScope.launch {
            photoRepository.updatePhotoNote(photoId, newNote)
            _uiState.update { it.copy(userMessage = "Note updated") }
        }
    }

    fun addTagToPhoto(photoId: Long, tag: String) {
        viewModelScope.launch {
            photoRepository.addTagToPhoto(photoId, tag)
            _uiState.update { it.copy(userMessage = "Tag added") }
        }
    }

    fun removeTagFromPhoto(photoId: Long, tag: String) {
        viewModelScope.launch {
            photoRepository.removeTagFromPhoto(photoId, tag)
            _uiState.update { it.copy(userMessage = "Tag removed") }
        }
    }

    fun rotatePhoto(photoId: Long, onUpdated: ((Photo) -> Unit)? = null) {
        viewModelScope.launch {
            val updated = photoRepository.rotatePhotoClockwise(photoId)
            if (updated != null) {
                val newOcrText = try {
                    ocrEngine?.extractText(updated.fileUri)?.fullText
                } catch (_: Exception) { null } ?: updated.ocrText
                val finalPhoto = updated.copy(ocrText = newOcrText)
                photoRepository.updatePhoto(finalPhoto)
                _uiState.update { it.copy(userMessage = "Photo rotated 90°") }
                onUpdated?.invoke(finalPhoto)
            } else {
                _uiState.update { it.copy(userMessage = "Unable to edit photo. Original file preserved.") }
            }
        }
    }

    fun cropPhoto(
        photoId: Long,
        leftFraction: Float,
        topFraction: Float,
        rightFraction: Float,
        bottomFraction: Float,
        onUpdated: ((Photo) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val updated = photoRepository.cropPhoto(photoId, leftFraction, topFraction, rightFraction, bottomFraction)
            if (updated != null) {
                val newOcrText = try {
                    ocrEngine?.extractText(updated.fileUri)?.fullText
                } catch (_: Exception) { null } ?: updated.ocrText
                val finalPhoto = updated.copy(ocrText = newOcrText)
                photoRepository.updatePhoto(finalPhoto)
                _uiState.update { it.copy(userMessage = "Photo re-cropped and text re-indexed") }
                onUpdated?.invoke(finalPhoto)
            } else {
                _uiState.update { it.copy(userMessage = "Unable to edit photo. Original file preserved.") }
            }
        }
    }

    fun clearHighlightedPhoto() {
        _uiState.update { it.copy(highlightedPhotoId = null) }
    }

    fun clearHighlightedGroup() {
        _uiState.update { it.copy(highlightedGroupId = null) }
    }

    fun clearHighlightedDocument() {
        _uiState.update { it.copy(highlightedDocumentId = null) }
    }

    fun clearHighlightedTextNote() {
        _uiState.update { it.copy(highlightedTextNoteId = null) }
    }

    fun clearHighlightedCanvas() {
        _uiState.update { it.copy(highlightedCanvasId = null) }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    companion object {
        fun provideFactory(
            folderId: Long,
            folderRepository: FolderRepository,
            photoRepository: PhotoRepository,
            photoStorageManager: PhotoStorageManager,
            ocrEngine: OcrEngine,
            folderSuggestEngine: FolderSuggestEngine,
            deadlineNotificationManager: DeadlineNotificationManager? = null,
            initialSubfolderId: Long? = null,
            targetPhotoId: Long? = null,
            targetGroupId: Long? = null,
            targetDocumentId: Long? = null,
            targetTextNoteId: Long? = null,
            targetCanvasId: Long? = null,
            settingsRepository: SettingsRepository? = null,
            documentRepository: DocumentRepository? = null,
            textNoteRepository: TextNoteRepository? = null,
            canvasNoteRepository: CanvasNoteRepository? = null,
            workspaceRepository: com.arinara.fotara.data.repository.WorkspaceRepository? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return FolderDetailViewModel(
                    folderId = folderId,
                    folderRepository = folderRepository,
                    photoRepository = photoRepository,
                    photoStorageManager = photoStorageManager,
                    ocrEngine = ocrEngine,
                    folderSuggestEngine = folderSuggestEngine,
                    deadlineNotificationManager = deadlineNotificationManager,
                    initialSubfolderId = initialSubfolderId,
                    targetPhotoId = targetPhotoId,
                    targetGroupId = targetGroupId,
                    targetDocumentId = targetDocumentId,
                    targetTextNoteId = targetTextNoteId,
                    targetCanvasId = targetCanvasId,
                    settingsRepository = settingsRepository,
                    documentRepository = documentRepository,
                    textNoteRepository = textNoteRepository,
                    canvasNoteRepository = canvasNoteRepository,
                    workspaceRepository = workspaceRepository
                ) as T
            }
        }
    }
}
