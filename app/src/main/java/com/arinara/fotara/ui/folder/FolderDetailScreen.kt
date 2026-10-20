// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.folder

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import coil.request.ImageRequest
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyColumn
import com.arinara.fotara.ui.components.workspaceGroupedFolderItems
import com.arinara.fotara.data.model.Workspace
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import com.arinara.fotara.data.model.DestinationType
import com.arinara.fotara.data.model.DocumentNote
import com.arinara.fotara.data.model.DocumentType
import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.data.model.PhotoGroup
import com.arinara.fotara.data.model.RecentDestination
import com.arinara.fotara.ui.components.BatchRenameDialog
import com.arinara.fotara.ui.components.GlowCorner
import com.arinara.fotara.ui.components.GroupSliderViewerModal
import com.arinara.fotara.ui.components.ShareFormatChoice
import com.arinara.fotara.ui.components.UnifiedShareDialog
import com.arinara.fotara.ui.components.PullToRefreshLayout
import com.arinara.fotara.ui.components.PullToRefreshHelper
import androidx.compose.ui.res.stringResource
import com.arinara.fotara.R
import com.arinara.fotara.ui.components.linkItCornerGlow
import com.arinara.fotara.ui.components.GlowAnchor
import com.arinara.fotara.ui.components.computeGridFacingGlowCorners
import com.arinara.fotara.ui.components.computeGridGlowAnchors
import com.arinara.fotara.ui.components.linkItGlow
import com.arinara.fotara.ui.components.toGlowAnchor
import androidx.compose.runtime.saveable.rememberSaveable
import com.arinara.fotara.ui.document.PdfViewerScreen
import com.arinara.fotara.util.CombineItem
import com.arinara.fotara.util.CombineManager
import com.arinara.fotara.util.PageLimitExceededException
import com.arinara.fotara.util.PdfSplitManager
import com.arinara.fotara.util.ZipExporter
import kotlinx.coroutines.flow.flowOf
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import com.arinara.fotara.data.repository.SubfolderDeleteResult
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.model.PhotoSource
import com.arinara.fotara.data.model.Subfolder
import com.arinara.fotara.data.model.TagColor
import com.arinara.fotara.data.storage.PhotoStorageManager
import com.arinara.fotara.ocr.FolderSuggestEngine
import com.arinara.fotara.ocr.OcrEngine
import com.arinara.fotara.theme.DockSlatePill
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.theme.FolderBodyBlue
import com.arinara.fotara.theme.FolderTabCream
import com.arinara.fotara.theme.HomeCardBorder
import com.arinara.fotara.theme.HomeCardSurface
import com.arinara.fotara.theme.HomeHeaderButtonSurface
import com.arinara.fotara.theme.HomeMainButtonBlue
import com.arinara.fotara.theme.HomeNearBlack
import com.arinara.fotara.theme.HomeSegmentSelectedPill
import com.arinara.fotara.theme.HomeSubtitleGray
import com.arinara.fotara.theme.MidnightCardOutline
import com.arinara.fotara.theme.MidnightSurface
import com.arinara.fotara.theme.TagAmber
import com.arinara.fotara.theme.TagCrimson
import com.arinara.fotara.theme.TextMuted
import com.arinara.fotara.theme.TextPrimary
import com.arinara.fotara.theme.TextSecondary
import com.arinara.fotara.ui.components.CaptureReviewSliderModal
import com.arinara.fotara.ui.components.CardScheduleBadge
import com.arinara.fotara.ui.components.ScheduleBadge
import com.arinara.fotara.ui.components.ScheduleNoteDialog
import com.arinara.fotara.ui.photo.PhotoViewerDialog
import com.arinara.fotara.util.NoteScheduleManager
import com.arinara.fotara.util.ScheduleAlertType
import com.arinara.fotara.util.ScheduleNoteType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.arinara.fotara.data.model.CanvasNote
import com.arinara.fotara.data.model.TextNote
import com.arinara.fotara.ui.canvas.CanvasNoteQuickActionSheet
import com.arinara.fotara.ui.note.TextNoteQuickActionSheet
import com.arinara.fotara.ui.components.TextNoteShareDialog
import com.arinara.fotara.ui.document.DocxViewerScreen
import com.arinara.fotara.ui.theme.jigglePressClickable
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FolderDetailScreen(
    viewModel: FolderDetailViewModel,
    photoStorageManager: PhotoStorageManager,
    ocrEngine: OcrEngine,
    folderSuggestEngine: FolderSuggestEngine,
    onBackClick: () -> Unit,
    openViewerDirectly: Boolean = false,
    targetPageIndex: Int? = null,
    onOpenGroup: ((folderId: Long, groupId: Long, targetPhotoId: Long?) -> Unit)? = null,
    onOpenTextNote: ((noteId: Long?, folderId: Long, subfolderId: Long?) -> Unit)? = null,
    onOpenDocx: ((documentId: Long) -> Unit)? = null,
    onOpenCanvasNote: ((canvasId: Long?, folderId: Long, subfolderId: Long?) -> Unit)? = null,
    canvasRepository: com.arinara.fotara.canvas.persistence.CanvasRepository? = null,
    canvasAssetManager: com.arinara.fotara.canvas.persistence.CanvasAssetManager? = null,
    searchQuery: String? = null,
    pdfPagePinRepository: com.arinara.fotara.data.repository.PdfPagePinRepository? = null,
    pdfPageDrawingRepository: com.arinara.fotara.data.repository.PdfPageDrawingRepository? = null,
    settingsRepository: com.arinara.fotara.data.repository.SettingsRepository? = null,
    onNavigateToEditor: ((Long, Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current

    var capturedBatchPhotos by remember { mutableStateOf<List<Photo>?>(null) }

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch {
                val imported = viewModel.importGalleryUris(uris)
                if (imported.isNotEmpty()) {
                    capturedBatchPhotos = imported
                }
            }
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = try {
                context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex != -1) cursor.getString(nameIndex) else null
                }
            } catch (_: Exception) { null } ?: "Document"
            val isPdf = fileName.lowercase().endsWith(".pdf") || (context.contentResolver.getType(it)?.contains("pdf") == true)
            viewModel.importDocument(it, fileName, isPdf)
        }
    }

    // Dialog & Sheet States
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showAddPhotoSheet by remember { mutableStateOf(false) }
    var showMultiCapture by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }
    var showRenameFolderDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var subfolderToRename by remember { mutableStateOf<Subfolder?>(null) }
    var inspectingPhoto by remember { mutableStateOf<Photo?>(null) }
    var quickActionPhoto by remember { mutableStateOf<Photo?>(null) }
    var photoToRename by remember { mutableStateOf<Photo?>(null) }
    var subfolderDeleteStatsState by remember { mutableStateOf<SubfolderDeleteResult?>(null) }
    var singleSubfolderToDelete by remember { mutableStateOf<Pair<Subfolder, SubfolderDeleteResult>?>(null) }
    var showSubfolderBulkDeleteConfirm by remember { mutableStateOf(false) }
    var showPhotoBulkDeleteConfirm by remember { mutableStateOf(false) }
    var showMovePhotosDialog by remember { mutableStateOf(false) }
    var showBatchColorDialog by remember { mutableStateOf(false) }
    var inspectingGroup by remember { mutableStateOf<FolderGridItem.Group?>(null) }
    var groupActionTarget by remember { mutableStateOf<FolderGridItem.Group?>(null) }
    var groupToRename by remember { mutableStateOf<PhotoGroup?>(null) }
    var groupToDelete by remember { mutableStateOf<FolderGridItem.Group?>(null) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showAddPhotosToGroupDialog by remember { mutableStateOf(false) }
    var groupForAddingPhotos by remember { mutableStateOf<FolderGridItem.Group?>(null) }
    var groupToColor by remember { mutableStateOf<PhotoGroup?>(null) }
    var showMergeGroupsDialog by remember { mutableStateOf(false) }
    var showAddToGroupDialog by remember { mutableStateOf(false) }
    var photoToCopy by remember { mutableStateOf<Photo?>(null) }
    var groupToMove by remember { mutableStateOf<PhotoGroup?>(null) }
    var showExportFormatDialog by remember { mutableStateOf(false) }
    var groupToExport by remember { mutableStateOf<FolderGridItem.Group?>(null) }

    var showBatchRenameDialog by remember { mutableStateOf(false) }
    var showUnifiedShareDialog by remember { mutableStateOf(false) }
    var shareProcessing by remember { mutableStateOf(false) }
    var shareProgressCurrent by remember { mutableStateOf(0) }
    var shareProgressTotal by remember { mutableStateOf(0) }
    var shareErrorMessage by remember { mutableStateOf<String?>(null) }
    var shareTargetItems by remember { mutableStateOf<List<FolderGridItem>>(emptyList()) }
    var inspectingDocumentId by rememberSaveable { mutableStateOf<Long?>(null) }
    var inspectingDocument by remember { mutableStateOf<DocumentNote?>(null) }
    var inspectingDocx by remember { mutableStateOf<DocumentNote?>(null) }
    var textNoteActionTarget by remember { mutableStateOf<FolderGridItem.TextNoteItem?>(null) }
    var textNoteToRename by remember { mutableStateOf<TextNote?>(null) }
    var textNoteToDelete by remember { mutableStateOf<TextNote?>(null) }
    var textNoteToShare by remember { mutableStateOf<TextNote?>(null) }
    var textNoteToMove by remember { mutableStateOf<TextNote?>(null) }
    var canvasNoteActionTarget by remember { mutableStateOf<FolderGridItem.CanvasNoteItem?>(null) }
    var canvasNoteToRename by remember { mutableStateOf<CanvasNote?>(null) }
    var canvasNoteToMove by remember { mutableStateOf<CanvasNote?>(null) }
    var canvasNoteToDelete by remember { mutableStateOf<CanvasNote?>(null) }
    var documentActionTarget by remember { mutableStateOf<FolderGridItem.Document?>(null) }
    var documentToRename by remember { mutableStateOf<DocumentNote?>(null) }
    var documentToColor by remember { mutableStateOf<DocumentNote?>(null) }
    var documentToDeadline by remember { mutableStateOf<DocumentNote?>(null) }
    var documentToMove by remember { mutableStateOf<DocumentNote?>(null) }
    var documentToDelete by remember { mutableStateOf<DocumentNote?>(null) }
    var pdfToSplit by remember { mutableStateOf<DocumentNote?>(null) }
    var itemToSchedule by remember { mutableStateOf<Triple<ScheduleNoteType, Long, String>?>(null) }
    var itemScheduledAt by remember { mutableStateOf<Long?>(null) }
    var itemAlertType by remember { mutableStateOf(ScheduleAlertType.NOTIFICATION) }
    var itemScheduleTitle by remember { mutableStateOf<String?>(null) }

    val bottomSheetState = rememberModalBottomSheetState()
    val gridState = rememberLazyGridState()
    val highlightAlpha = remember { Animatable(0f) }

    LaunchedEffect(uiState.highlightedPhotoId) {
        val targetId = uiState.highlightedPhotoId ?: return@LaunchedEffect
        if (uiState.photos.isEmpty()) return@LaunchedEffect

        val matchedPhoto = uiState.photos.firstOrNull { it.id == targetId }
        if (matchedPhoto == null) {
            snackbarHostState.showSnackbar("Photo is no longer in this folder")
            viewModel.clearHighlightedPhoto()
            return@LaunchedEffect
        }

        // Search match on a photo that is a MEMBER of a group (Addendum 7 edge case):
        // Highlight the group waypoint for ~1s, then auto-navigate to Group screen for photo highlight.
        if (matchedPhoto.groupId != null && onOpenGroup != null) {
            val parentGroupId = matchedPhoto.groupId
            val groupIndex = uiState.gridItems.indexOfFirst {
                it is FolderGridItem.Group && it.group.id == parentGroupId
            }
            if (groupIndex != -1) {
                val isVisible = gridState.layoutInfo.visibleItemsInfo.any { it.index == groupIndex }
                if (!isVisible) {
                    gridState.animateScrollToItem(groupIndex)
                }
                highlightAlpha.animateTo(0.28f, tween(200, easing = FastOutSlowInEasing))
                delay(800L)
                highlightAlpha.animateTo(0f, tween(200, easing = FastOutSlowInEasing))
                viewModel.clearHighlightedPhoto()
                onOpenGroup(matchedPhoto.folderId, parentGroupId, targetId)
                return@LaunchedEffect
            }
        }

        val targetIndex = uiState.gridItems.indexOfFirst {
            it is FolderGridItem.StandalonePhoto && it.photo.id == targetId
        }
        if (targetIndex != -1) {
            val isVisible = gridState.layoutInfo.visibleItemsInfo.any { it.index == targetIndex }
            if (!isVisible) {
                gridState.animateScrollToItem(targetIndex)
            }
            if (openViewerDirectly && inspectingPhoto == null) {
                inspectingPhoto = matchedPhoto
            }
            highlightAlpha.animateTo(0.28f, tween(250, easing = FastOutSlowInEasing))
            delay(1500L)
            highlightAlpha.animateTo(0f, tween(350, easing = FastOutSlowInEasing))
            viewModel.clearHighlightedPhoto()
        } else {
            snackbarHostState.showSnackbar("Photo is no longer in this folder")
            viewModel.clearHighlightedPhoto()
        }
    }

    LaunchedEffect(uiState.highlightedGroupId) {
        val targetGroupId = uiState.highlightedGroupId ?: return@LaunchedEffect
        if (uiState.gridItems.isEmpty()) return@LaunchedEffect

        val targetIndex = uiState.gridItems.indexOfFirst {
            it is FolderGridItem.Group && it.group.id == targetGroupId
        }
        if (targetIndex != -1) {
            val isVisible = gridState.layoutInfo.visibleItemsInfo.any { it.index == targetIndex }
            if (!isVisible) {
                gridState.animateScrollToItem(targetIndex)
            }
            highlightAlpha.animateTo(0.28f, tween(250, easing = FastOutSlowInEasing))
            delay(1500L)
            highlightAlpha.animateTo(0f, tween(350, easing = FastOutSlowInEasing))
            viewModel.clearHighlightedGroup()
        } else {
            snackbarHostState.showSnackbar("Group is no longer in this folder")
            viewModel.clearHighlightedGroup()
        }
    }

    LaunchedEffect(inspectingDocumentId) {
        val id: Long? = inspectingDocumentId
        if (id != null && (inspectingDocument == null || inspectingDocument?.id != id)) {
            val doc = viewModel.documentRepository?.getDocumentNoteById(id)
            if (doc != null && doc.docType == com.arinara.fotara.data.model.DocumentType.PDF) {
                inspectingDocument = doc
            }
        }
    }

    LaunchedEffect(openViewerDirectly, viewModel.targetDocumentId) {
        val targetId = viewModel.targetDocumentId
        if (openViewerDirectly && targetId != null && inspectingDocument == null) {
            val doc = viewModel.documentRepository?.getDocumentNoteById(targetId)
            if (doc != null && doc.docType == com.arinara.fotara.data.model.DocumentType.PDF) {
                inspectingDocument = doc
                inspectingDocumentId = doc.id
            }
        }
    }

    LaunchedEffect(uiState.highlightedDocumentId) {
        val targetId = uiState.highlightedDocumentId ?: return@LaunchedEffect
        if (uiState.gridItems.isEmpty()) return@LaunchedEffect

        val targetIndex = uiState.gridItems.indexOfFirst {
            it is FolderGridItem.Document && it.documentNote.id == targetId
        }
        if (targetIndex != -1) {
            val isVisible = gridState.layoutInfo.visibleItemsInfo.any { it.index == targetIndex }
            if (!isVisible) {
                gridState.animateScrollToItem(targetIndex)
            }
            val matchedDoc = (uiState.gridItems[targetIndex] as FolderGridItem.Document).documentNote
            if (openViewerDirectly && inspectingDocument == null && matchedDoc.docType == com.arinara.fotara.data.model.DocumentType.PDF) {
                inspectingDocument = matchedDoc
                inspectingDocumentId = matchedDoc.id
            }
            highlightAlpha.animateTo(0.28f, tween(250, easing = FastOutSlowInEasing))
            delay(1500L)
            highlightAlpha.animateTo(0f, tween(350, easing = FastOutSlowInEasing))
            viewModel.clearHighlightedDocument()
        } else {
            snackbarHostState.showSnackbar("Document is no longer in this folder")
            viewModel.clearHighlightedDocument()
        }
    }

    LaunchedEffect(uiState.highlightedTextNoteId) {
        val targetId = uiState.highlightedTextNoteId ?: return@LaunchedEffect
        if (uiState.gridItems.isEmpty()) return@LaunchedEffect

        val targetIndex = uiState.gridItems.indexOfFirst {
            it is FolderGridItem.TextNoteItem && it.textNote.id == targetId
        }
        if (targetIndex != -1) {
            val isVisible = gridState.layoutInfo.visibleItemsInfo.any { it.index == targetIndex }
            if (!isVisible) {
                gridState.animateScrollToItem(targetIndex)
            }
            highlightAlpha.animateTo(0.28f, tween(250, easing = FastOutSlowInEasing))
            delay(1500L)
            highlightAlpha.animateTo(0f, tween(350, easing = FastOutSlowInEasing))
            viewModel.clearHighlightedTextNote()
        } else {
            snackbarHostState.showSnackbar("Note is no longer in this folder")
            viewModel.clearHighlightedTextNote()
        }
    }

    LaunchedEffect(uiState.highlightedCanvasId) {
        val targetId = uiState.highlightedCanvasId ?: return@LaunchedEffect
        if (uiState.gridItems.isEmpty()) return@LaunchedEffect

        val targetIndex = uiState.gridItems.indexOfFirst {
            it is FolderGridItem.CanvasNoteItem && it.canvasNote.id == targetId
        }
        if (targetIndex != -1) {
            val isVisible = gridState.layoutInfo.visibleItemsInfo.any { it.index == targetIndex }
            if (!isVisible) {
                gridState.animateScrollToItem(targetIndex)
            }
            highlightAlpha.animateTo(0.28f, tween(250, easing = FastOutSlowInEasing))
            delay(1500L)
            highlightAlpha.animateTo(0f, tween(350, easing = FastOutSlowInEasing))
            viewModel.clearHighlightedCanvas()
        } else {
            snackbarHostState.showSnackbar("Canvas is no longer in this folder")
            viewModel.clearHighlightedCanvas()
        }
    }

    LaunchedEffect(uiState.userMessage, uiState.pendingUndoAction) {
        val msg = uiState.userMessage ?: return@LaunchedEffect
        if (uiState.pendingUndoAction != null) {
            val result = snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = "Undo",
                duration = androidx.compose.material3.SnackbarDuration.Short
            )
            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                viewModel.undoLastAction()
            } else {
                viewModel.clearPendingUndo()
            }
            viewModel.clearUserMessage()
        } else {
            viewModel.clearUserMessage()
            snackbarHostState.showSnackbar(msg)
        }
    }

    BackHandler(enabled = uiState.isBatchSelectMode || uiState.isSubfolderMultiSelectMode) {
        if (uiState.isBatchSelectMode) {
            viewModel.exitBatchSelectMode()
        }
        if (uiState.isSubfolderMultiSelectMode) {
            viewModel.exitSubfolderMultiSelect()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = ElectricSlateTokens.BgCanvas,
        topBar = {
            if (uiState.isSubfolderMultiSelectMode) {
                // Contextual Action Bar for Subfolder Multi-Select
                TopAppBar(
                    title = {
                        Text(
                            text = "${uiState.selectedSubfolderIds.size} Selected",
                            color = Color.White,
                            fontFamily = ElmsSans,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.exitSubfolderMultiSelect() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Exit Selection",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.selectAllSubfolders() }) {
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = "Select All",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = { viewModel.invertSubfolderSelection() }) {
                            Icon(
                                imageVector = Icons.Default.FlipToBack,
                                contentDescription = stringResource(R.string.action_invert_selection),
                                tint = Color.White
                            )
                        }
                        IconButton(
                            onClick = {
                                if (uiState.selectedSubfolderIds.isNotEmpty()) {
                                    viewModel.getSubfolderDeleteStats(uiState.selectedSubfolderIds.toList()) { stats ->
                                        subfolderDeleteStatsState = stats
                                        showSubfolderBulkDeleteConfirm = true
                                    }
                                }
                            },
                            enabled = uiState.selectedSubfolderIds.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Move Subfolders to Trash",
                                tint = if (uiState.selectedSubfolderIds.isNotEmpty()) TagCrimson else TextMuted
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = ElectricSlateTokens.BgCanvas)
                )
            } else if (uiState.isBatchSelectMode) {
                // Contextual Action Bar for Multi-Select (Rename, Group [2+ photos], Move, Color, Delete)
                TopAppBar(
                    title = {
                        Text(
                            text = "${uiState.totalSelectionCount} Selected",
                            color = Color.White,
                            fontFamily = ElmsSans,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.exitBatchSelectMode() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Exit Selection",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.selectAllPhotos() }) {
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = "Select All",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = { viewModel.invertSelection() }) {
                            Icon(
                                imageVector = Icons.Default.FlipToBack,
                                contentDescription = stringResource(R.string.action_invert_selection),
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = ElectricSlateTokens.BgCanvas)
                )
            } else {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        showRenameFolderDialog = true
                                    }
                                )
                        ) {
                            Text(
                                text = uiState.folder?.name ?: "Folder",
                                color = ElectricSlateTokens.TextPrimary,
                                fontFamily = ElmsSans,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            val tagColor = uiState.folder?.tagColor?.composeColor ?: ElectricSlateTokens.PrimaryAccent
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(tagColor)
                            )
                        }
                    },
                    navigationIcon = {
                        Box(
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(HomeHeaderButtonSurface)
                                .clickable { onBackClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    actions = {
                        Row(
                            modifier = Modifier.padding(end = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Right Action 1: Add Note Button [+]
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(HomeMainButtonBlue)
                                    .clickable { showAddPhotoSheet = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Note",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Right Action 2: Highlighted Overflow Menu [(⋮)]
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(HomeHeaderButtonSurface)
                                    .jigglePressClickable { showOverflowMenu = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Folder Utilities Menu",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )

                                // Overflow Dropdown Menu
                                DropdownMenu(
                                    expanded = showOverflowMenu,
                                    onDismissRequest = { showOverflowMenu = false },
                                    modifier = Modifier
                                        .background(ElectricSlateTokens.SurfaceElevated)
                                        .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                                ) {
                                DropdownMenuItem(
                                    text = { Text("Sort: Newest Uploads", color = ElectricSlateTokens.TextPrimary) },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, null, tint = ElectricSlateTokens.TextMuted) },
                                    onClick = {
                                        viewModel.setSortOption(PhotoSortOption.UPLOAD_DATE_DESC)
                                        showOverflowMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sort: Nearest Deadline", color = ElectricSlateTokens.TextPrimary) },
                                    leadingIcon = { Icon(Icons.Default.Event, null, tint = ElectricSlateTokens.TextMuted) },
                                    onClick = {
                                        viewModel.setSortOption(PhotoSortOption.NEAREST_DEADLINE)
                                        showOverflowMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export Folder to PDF", color = ElectricSlateTokens.TextPrimary) },
                                    leadingIcon = { Icon(Icons.Default.PictureAsPdf, null, tint = ElectricSlateTokens.TextMuted) },
                                    onClick = {
                                        showOverflowMenu = false
                                        viewModel.exportToPdf(context) { pdfFile ->
                                            val uri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                pdfFile
                                            )
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "application/pdf"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Export Folder Notes PDF"))
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (uiState.isBatchSelectMode) "Exit Batch Select" else "Batch Select Mode",
                                            color = ElectricSlateTokens.TextPrimary
                                        )
                                    },
                                    leadingIcon = { Icon(Icons.Default.SelectAll, null, tint = ElectricSlateTokens.TextMuted) },
                                    onClick = {
                                        viewModel.toggleBatchSelectMode()
                                        showOverflowMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Folder Color Label", color = ElectricSlateTokens.TextPrimary) },
                                    leadingIcon = { Icon(Icons.Default.ColorLens, null, tint = ElectricSlateTokens.TextMuted) },
                                    onClick = {
                                        showColorDialog = true
                                        showOverflowMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Rename Folder", color = ElectricSlateTokens.TextPrimary) },
                                    leadingIcon = { Icon(Icons.Default.Edit, null, tint = ElectricSlateTokens.TextMuted) },
                                    onClick = {
                                        showRenameFolderDialog = true
                                        showOverflowMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete Folder", color = ElectricSlateTokens.Destructive) },
                                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = ElectricSlateTokens.Destructive) },
                                    onClick = {
                                        showDeleteConfirmDialog = true
                                        showOverflowMenu = false
                                    }
                                )
                            }
                        }
                    }
                },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = ElectricSlateTokens.BgCanvas)
                )
            }
        },
        bottomBar = {},
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
            ) {
            // Horizontal Subfolder Navigation Tabs
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // "All" tab
                item {
                    val isSelected = uiState.selectedSubfolderId == null && !uiState.isSubfolderMultiSelectMode
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) HomeSegmentSelectedPill else HomeCardSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) HomeSegmentSelectedPill else HomeCardBorder
                        ),
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !uiState.isSubfolderMultiSelectMode) {
                                viewModel.selectSubfolder(null)
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "All Notes",
                                color = if (isSelected) Color.White else if (uiState.isSubfolderMultiSelectMode) TextMuted else TextSecondary,
                                fontFamily = ElmsSans,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                // Subfolder tabs with unified single long-press context menu & multi-select styling
                items(uiState.subfolders, key = { it.id }) { sub ->
                    val isFilterSelected = uiState.selectedSubfolderId == sub.id
                    val isMultiSelected = viewModel.selectedSubfolderMap[sub.id] == true
                    var showSubMenu by remember { mutableStateOf(false) }

                    Box {
                        if (uiState.isSubfolderMultiSelectMode) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isMultiSelected) HomeSegmentSelectedPill else HomeCardSurface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isMultiSelected) HomeSegmentSelectedPill else HomeCardBorder
                                ),
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .combinedClickable(
                                        onClick = { viewModel.toggleSubfolderSelection(sub.id) },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.toggleSubfolderSelection(sub.id)
                                        }
                                    )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    if (isMultiSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = sub.name,
                                        color = if (isMultiSelected) Color.White else TextSecondary,
                                        fontFamily = ElmsSans,
                                        fontSize = 14.sp,
                                        fontWeight = if (isMultiSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isFilterSelected) HomeSegmentSelectedPill else HomeCardSurface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isFilterSelected) HomeSegmentSelectedPill else HomeCardBorder
                                ),
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .combinedClickable(
                                        onClick = { viewModel.selectSubfolder(sub.id) },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            showSubMenu = true
                                        }
                                    )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = sub.name,
                                        color = if (isFilterSelected) Color.White else TextSecondary,
                                        fontFamily = ElmsSans,
                                        fontSize = 14.sp,
                                        fontWeight = if (isFilterSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Unified single long-press context menu
                        DropdownMenu(
                            expanded = showSubMenu,
                            onDismissRequest = { showSubMenu = false },
                            modifier = Modifier
                                .background(ElectricSlateTokens.SurfaceElevated)
                                .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Rename", color = ElectricSlateTokens.TextPrimary, fontFamily = ElmsSans) },
                                leadingIcon = { Icon(Icons.Default.Edit, null, tint = ElectricSlateTokens.TextMuted) },
                                onClick = {
                                    showSubMenu = false
                                    subfolderToRename = sub
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = ElectricSlateTokens.Destructive, fontFamily = ElmsSans) },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = ElectricSlateTokens.Destructive) },
                                onClick = {
                                    showSubMenu = false
                                    viewModel.getSubfolderDeleteStats(listOf(sub.id)) { stats ->
                                        singleSubfolderToDelete = sub to stats
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Select", color = ElectricSlateTokens.TextPrimary, fontFamily = ElmsSans) },
                                leadingIcon = { Icon(Icons.Default.Check, null, tint = ElectricSlateTokens.TextMuted) },
                                onClick = {
                                    showSubMenu = false
                                    viewModel.startSubfolderMultiSelect(sub.id)
                                }
                            )
                        }
                    }
                }

                // "+ Subfolder" creation chip
                if (!uiState.isSubfolderMultiSelectMode) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = HomeCardSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, HomeCardBorder),
                            modifier = Modifier
                                .clickable { viewModel.openAddSubfolderDialog() }
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add subfolder",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Subfolder",
                                    color = Color.White,
                                    fontFamily = ElmsSans,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Photos & Groups Grid
            if (uiState.gridItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "No notes",
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No notes in this folder yet",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap + to capture handwritten notes or import reference slides.",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                val configuration = LocalConfiguration.current
                val screenWidthDp = configuration.screenWidthDp
                val baseDensity = uiState.gridDensity
                val photoColumns = when {
                    screenWidthDp >= 840 -> baseDensity + 3
                    screenWidthDp >= 600 -> baseDensity + 1
                    else -> baseDensity
                }

                val isOverlayOpen = quickActionPhoto != null || photoToRename != null || inspectingPhoto != null || inspectingGroup != null || groupActionTarget != null || groupToRename != null || groupToDelete != null || showCreateGroupDialog || showAddPhotosToGroupDialog || showAddToGroupDialog || photoToCopy != null || groupToMove != null || showExportFormatDialog || groupToExport != null || showBatchRenameDialog || showUnifiedShareDialog || inspectingDocument != null || inspectingDocx != null || textNoteActionTarget != null || canvasNoteActionTarget != null || showMovePhotosDialog || showBatchColorDialog
                val canRefresh = PullToRefreshHelper.canTriggerRefresh(
                    isAtTop = gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0,
                    isMultiSelectActive = uiState.isBatchSelectMode || uiState.isSubfolderMultiSelectMode,
                    isSearchFocused = false,
                    isOverlayOpen = isOverlayOpen
                )

                val gridGlowAnchors = remember(uiState.gridItems, photoColumns) {
                    computeGridGlowAnchors(
                        items = uiState.gridItems.map { it.itemId to it.linkGroupId },
                        columns = photoColumns
                    )
                }

                PullToRefreshLayout(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    enabled = canRefresh,
                    surfaceColor = MidnightSurface,
                    accentColor = HomeMainButtonBlue,
                    borderColor = MidnightCardOutline,
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(photoColumns),
                        state = gridState,
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(items = uiState.gridItems, key = { it.key }, contentType = { it::class.java.simpleName }) { gridItem ->
                            when (gridItem) {
                                is FolderGridItem.StandalonePhoto -> {
                                    val photo = gridItem.photo
                                    val isTarget = uiState.highlightedPhotoId == photo.id
                                    DetailPhotoCard(
                                        photo = photo,
                                        isBatchMode = uiState.isBatchSelectMode,
                                        isSelected = viewModel.selectedPhotoMap[photo.id] == true,
                                        isLinked = gridItem.isLinked,
                                        isHighlighted = isTarget && highlightAlpha.value > 0f,
                                        highlightAlpha = if (isTarget) highlightAlpha.value else 0f,
                                        glowAnchors = gridGlowAnchors[photo.id] ?: emptySet(),
                                        onCardClick = {
                                            if (uiState.isBatchSelectMode) {
                                                viewModel.togglePhotoSelection(photo.id)
                                            } else {
                                                inspectingPhoto = photo
                                            }
                                        },
                                        onCardLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            quickActionPhoto = photo
                                        }
                                    )
                                }
                                is FolderGridItem.Group -> {
                                    val isTarget = uiState.highlightedGroupId == gridItem.group.id
                                    DetailGroupCard(
                                        groupItem = gridItem,
                                        isBatchMode = uiState.isBatchSelectMode,
                                        isSelected = viewModel.selectedGroupMap[gridItem.group.id] == true,
                                        isHighlighted = isTarget && highlightAlpha.value > 0f,
                                        highlightAlpha = if (isTarget) highlightAlpha.value else 0f,
                                        glowAnchors = gridGlowAnchors[gridItem.itemId] ?: emptySet(),
                                        onCardClick = {
                                            if (uiState.isBatchSelectMode) {
                                                viewModel.toggleGroupSelection(gridItem.group.id)
                                            } else {
                                                if (onOpenGroup != null) {
                                                    onOpenGroup(gridItem.group.folderId, gridItem.group.id, null)
                                                } else {
                                                    inspectingGroup = gridItem
                                                }
                                            }
                                        },
                                        onCardLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            groupActionTarget = gridItem
                                        }
                                    )
                                }
                                is FolderGridItem.Document -> {
                                    val doc = gridItem.documentNote
                                    val isTarget = uiState.highlightedDocumentId == doc.id
                                    DetailDocumentCard(
                                        documentItem = gridItem,
                                        isBatchMode = uiState.isBatchSelectMode,
                                        isSelected = viewModel.selectedDocumentMap[doc.id] == true,
                                        isHighlighted = isTarget && highlightAlpha.value > 0f,
                                        highlightAlpha = if (isTarget) highlightAlpha.value else 0f,
                                        glowAnchors = gridGlowAnchors[gridItem.itemId] ?: emptySet(),
                                        onCardClick = {
                                            if (uiState.isBatchSelectMode) {
                                                viewModel.toggleDocumentSelection(doc.id)
                                            } else {
                                                if (doc.docType == DocumentType.PDF) {
                                                    inspectingDocument = doc
                                                    inspectingDocumentId = doc.id
                                                } else {
                                                    if (onOpenDocx != null) {
                                                        onOpenDocx(doc.id)
                                                    } else {
                                                        inspectingDocx = doc
                                                    }
                                                }
                                            }
                                        },
                                        onCardLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            documentActionTarget = gridItem
                                        }
                                    )
                                }
                                is FolderGridItem.TextNoteItem -> {
                                    val textNote = gridItem.textNote
                                    val isTarget = uiState.highlightedTextNoteId == textNote.id
                                    DetailTextNoteCard(
                                        noteItem = gridItem,
                                        isBatchMode = uiState.isBatchSelectMode,
                                        isSelected = viewModel.selectedTextNoteMap[textNote.id] == true,
                                        isHighlighted = isTarget && highlightAlpha.value > 0f,
                                        highlightAlpha = if (isTarget) highlightAlpha.value else 0f,
                                        glowAnchors = gridGlowAnchors[gridItem.itemId] ?: emptySet(),
                                        onCardClick = {
                                            if (uiState.isBatchSelectMode) {
                                                viewModel.toggleTextNoteSelection(textNote.id)
                                            } else {
                                                onOpenTextNote?.invoke(textNote.id, textNote.folderId, textNote.subfolderId)
                                            }
                                        },
                                        onCardLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            textNoteActionTarget = gridItem
                                        }
                                    )
                                }
                                is FolderGridItem.CanvasNoteItem -> {
                                    val canvasNote = gridItem.canvasNote
                                    val isTarget = uiState.highlightedCanvasId == canvasNote.id
                                    DetailCanvasCard(
                                        canvasItem = gridItem,
                                        isBatchMode = uiState.isBatchSelectMode,
                                        isSelected = viewModel.selectedCanvasNoteMap[canvasNote.id] == true,
                                        isHighlighted = isTarget && highlightAlpha.value > 0f,
                                        highlightAlpha = if (isTarget) highlightAlpha.value else 0f,
                                        glowAnchors = gridGlowAnchors[gridItem.itemId] ?: emptySet(),
                                        onCardClick = {
                                            if (uiState.isBatchSelectMode) {
                                                viewModel.toggleCanvasNoteSelection(canvasNote.id)
                                            } else {
                                                onOpenCanvasNote?.invoke(canvasNote.id, canvasNote.folderId, canvasNote.subfolderId)
                                            }
                                        },
                                        onCardLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            canvasNoteActionTarget = gridItem
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Batch Selection Action Dock Overlay
        AnimatedVisibility(
            visible = uiState.isBatchSelectMode && uiState.totalSelectionCount > 0,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            BatchSelectActionDock(
                uiState = uiState,
                onLinkClick = { viewModel.createLinkGroup() },
                onCreateGroupClick = { showCreateGroupDialog = true },
                onMergeGroupsClick = { showMergeGroupsDialog = true },
                onAddToGroupClick = { showAddToGroupDialog = true },
                onRenameClick = {
                    if (uiState.totalSelectionCount == 1 && uiState.selectedPhotoIds.size == 1) {
                        val singlePhoto = uiState.photos.firstOrNull { it.id == uiState.selectedPhotoIds.first() }
                        singlePhoto?.let { photoToRename = it }
                    } else if (uiState.totalSelectionCount == 1 && uiState.selectedGroupIds.size == 1) {
                        val singleGroup = uiState.groups.firstOrNull { it.id == uiState.selectedGroupIds.first() }
                        singleGroup?.let { groupToRename = it }
                    } else if (uiState.totalSelectionCount == 1 && uiState.selectedDocumentIds.size == 1) {
                        val singleDoc = uiState.documents.firstOrNull { it.id == uiState.selectedDocumentIds.first() }
                        singleDoc?.let { documentToRename = it }
                    } else if (uiState.totalSelectionCount == 1 && uiState.selectedTextNoteIds.size == 1) {
                        val singleNote = uiState.textNotes.firstOrNull { it.id == uiState.selectedTextNoteIds.first() }
                        singleNote?.let { textNoteToRename = it }
                    } else {
                        showBatchRenameDialog = true
                    }
                },
                onShareClick = {
                    val selected = uiState.gridItems.filter { item ->
                        when (item) {
                            is FolderGridItem.StandalonePhoto -> item.photo.id in uiState.selectedPhotoIds
                            is FolderGridItem.Group -> item.group.id in uiState.selectedGroupIds
                            is FolderGridItem.Document -> item.documentNote.id in uiState.selectedDocumentIds
                            is FolderGridItem.TextNoteItem -> item.textNote.id in uiState.selectedTextNoteIds
                            is FolderGridItem.CanvasNoteItem -> item.canvasNote.id in uiState.selectedCanvasNoteIds
                        }
                    }
                    shareTargetItems = selected
                    showUnifiedShareDialog = true
                },
                onMoveClick = { showMovePhotosDialog = true },
                onColorClick = { showBatchColorDialog = true },
                onDeleteClick = { showPhotoBulkDeleteConfirm = true }
            )
        }
    }
}

    // Modal Action Sheet for Add Photo Button [+]
    if (showAddPhotoSheet) {
        AddPhotoOptionSheet(
            sheetState = bottomSheetState,
            onDismiss = { showAddPhotoSheet = false },
            onNewCanvas = {
                showAddPhotoSheet = false
                onOpenCanvasNote?.invoke(null, viewModel.folderId, uiState.selectedSubfolderId)
            },
            onNewTextNote = {
                showAddPhotoSheet = false
                onOpenTextNote?.invoke(null, viewModel.folderId, uiState.selectedSubfolderId)
            },
            onMultiCapture = {
                showAddPhotoSheet = false
                showMultiCapture = true
            },
            onImportGallery = {
                showAddPhotoSheet = false
                galleryPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onImportDocument = {
                showAddPhotoSheet = false
                documentPickerLauncher.launch(
                    arrayOf(
                        "application/pdf",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    )
                )
            }
        )
    }

    // Capture Triage Modal (Popup Slider 75% height / 90% width)
    if (capturedBatchPhotos != null) {
        CaptureReviewSliderModal(
            folderName = uiState.folder?.name ?: "Folder",
            initialPhotos = capturedBatchPhotos ?: emptyList(),
            subfolders = uiState.subfolders,
            folderSuggestEngine = folderSuggestEngine,
            availableFolders = uiState.availableFolders,
            photoStorageManager = photoStorageManager,
            onSaveBatch = { savedList, subfolderId ->
                viewModel.saveCapturedBatch(savedList, subfolderId)
                capturedBatchPhotos = null
            },
            onDismiss = {
                capturedBatchPhotos?.forEach { photo ->
                    coroutineScope.launch {
                        photoStorageManager.deletePhotoFiles(photo.fileUri, photo.thumbnailUri)
                    }
                }
                capturedBatchPhotos = null
            }
        )
    }

    // Full-Screen Multi-Capture Camera Viewfinder
    if (showMultiCapture) {
        com.arinara.fotara.ui.capture.MultiCaptureScreen(
            folderId = uiState.folder?.id ?: 1L,
            ocrEngine = ocrEngine,
            folderSuggestEngine = folderSuggestEngine,
            photoStorageManager = photoStorageManager,
            onFinishBatch = { batch ->
                showMultiCapture = false
                capturedBatchPhotos = batch
            },
            onDismiss = { showMultiCapture = false }
        )
    }

    // Contextual Photo Quick Action Sheet (Long-Press on Photo)
    if (quickActionPhoto != null) {
        quickActionPhoto?.let { photo ->
            val isPhotoLinked = uiState.gridItems.filterIsInstance<FolderGridItem.StandalonePhoto>()
                .firstOrNull { it.photo.id == photo.id }?.isLinked == true
            com.arinara.fotara.ui.photo.PhotoQuickActionSheet(
                photo = photo,
                subfolders = uiState.subfolders,
                isLinked = isPhotoLinked,
                onMoveSubfolder = { subId -> viewModel.movePhotoToSubfolder(photo.id, subId) },
                onChangeTagColor = { colorHex -> viewModel.updatePhotoTagColor(photo.id, colorHex) },
                onSetDeadline = { deadlineMs -> viewModel.setPhotoDeadline(photo.id, deadlineMs) },
                onRenamePhoto = { photoToRename = photo },
                onSelectPhoto = { viewModel.startBatchSelection(photo.id) },
                onCopyTo = { photoToCopy = photo },
                onUnlink = { viewModel.unlinkGridItem(photo.id) },
                onSchedule = {
                    itemToSchedule = Triple(ScheduleNoteType.PHOTO, photo.id, photo.caption ?: "Photo Note")
                    itemScheduledAt = photo.scheduledAt
                    itemScheduleTitle = photo.scheduleTitle
                    itemAlertType = try {
                        ScheduleAlertType.valueOf(photo.alertType ?: "NOTIFICATION")
                    } catch (_: Exception) {
                        ScheduleAlertType.NOTIFICATION
                    }
                },
                onTogglePin = { viewModel.togglePin(photo) },
                onDeletePhoto = { viewModel.deletePhoto(photo.id) },
                onDismiss = { quickActionPhoto = null }
            )
        }
    }

    // Full-Screen Note Inspector for existing notes
    if (inspectingPhoto != null) {
        inspectingPhoto?.let { photo ->
            PhotoViewerDialog(
                photo = photo,
                photos = uiState.photos,
                onSaveNote = { note -> viewModel.updatePhotoNote(photo.id, note) },
                onAddTag = { tag -> viewModel.addTagToPhoto(photo.id, tag) },
                onRemoveTag = { tag -> viewModel.removeTagFromPhoto(photo.id, tag) },
                onRotatePhoto = { p ->
                    viewModel.rotatePhoto(p.id) { updated ->
                        inspectingPhoto = updated
                    }
                },
                onCropPhoto = { p, left, top, right, bottom ->
                    viewModel.cropPhoto(p.id, left, top, right, bottom) { updated ->
                        inspectingPhoto = updated
                    }
                },
                onDismiss = { inspectingPhoto = null }
            )
        }
    }

    // Dialog: Folder Color Chooser
    if (showColorDialog) {
        FolderColorChooserDialog(
            onDismiss = { showColorDialog = false },
            onSelectColor = { colorHex ->
                viewModel.updateFolderColor(colorHex)
                showColorDialog = false
            }
        )
    }

    // Dialog: Rename Folder
    if (showRenameFolderDialog) {
        RenameFolderDialog(
            initialName = uiState.folder?.name ?: "",
            onDismiss = { showRenameFolderDialog = false },
            onConfirm = { newName ->
                viewModel.renameFolder(newName)
                showRenameFolderDialog = false
            }
        )
    }

    // Dialog: Rename Subfolder
    subfolderToRename?.let { sub ->
        RenameSubfolderDialog(
            initialName = sub.name,
            onDismiss = { subfolderToRename = null },
            onConfirm = { newName ->
                viewModel.renameSubfolder(sub.id, newName)
                subfolderToRename = null
            }
        )
    }

    // Dialog: Delete Folder Confirmation
    if (showDeleteConfirmDialog) {
        DeleteFolderConfirmDialog(
            folderName = uiState.folder?.name ?: "",
            onDismiss = { showDeleteConfirmDialog = false },
            onConfirm = {
                showDeleteConfirmDialog = false
                viewModel.deleteFolder(onDeleted = onBackClick)
            }
        )
    }

    // Dialog: Add Subfolder
    if (uiState.showAddSubfolderDialog) {
        AddSubfolderDialog(
            onDismiss = { viewModel.closeAddSubfolderDialog() },
            onConfirm = { name ->
                viewModel.createSubfolder(name)
            }
        )
    }


    // Dialog: Rename Photo Note (v1.1)
    photoToRename?.let { target ->
        RenamePhotoNoteDialog(
            initialCaption = target.caption,
            onDismiss = { photoToRename = null },
            onConfirm = { newCaption ->
                viewModel.renamePhoto(target.id, newCaption)
                photoToRename = null
            }
        )
    }

    // Dialog: Delete Single Subfolder (Trash confirmation)
    singleSubfolderToDelete?.let { (sub, stats) ->
        DeleteSubfolderConfirmDialog(
            subfolderName = sub.name,
            photoCount = stats.photoCount,
            sizeKb = (stats.totalSizeBytes + 1023) / 1024,
            onDismiss = { singleSubfolderToDelete = null },
            onConfirm = {
                viewModel.deleteSubfolder(sub.id)
                singleSubfolderToDelete = null
            }
        )
    }

    // Dialog: Subfolder Bulk Delete Confirmation
    if (showSubfolderBulkDeleteConfirm && subfolderDeleteStatsState != null) {
        val stats = subfolderDeleteStatsState!!
        SubfolderBulkDeleteConfirmDialog(
            subfolderCount = stats.subfolderCount,
            photoCount = stats.photoCount,
            sizeKb = (stats.totalSizeBytes + 1023) / 1024,
            onDismiss = {
                showSubfolderBulkDeleteConfirm = false
                subfolderDeleteStatsState = null
            },
            onConfirm = {
                viewModel.deleteSelectedSubfolders()
                showSubfolderBulkDeleteConfirm = false
                subfolderDeleteStatsState = null
            }
        )
    }

    // Dialog: Photo Bulk Delete Confirmation
    if (showPhotoBulkDeleteConfirm) {
        PhotoBulkDeleteConfirmDialog(
            selectedCount = uiState.selectedPhotoIds.size,
            onDismiss = { showPhotoBulkDeleteConfirm = false },
            onConfirm = {
                viewModel.deleteSelectedPhotos()
                showPhotoBulkDeleteConfirm = false
            }
        )
    }


    // Dialog: Move Photos to Folder / Subfolder
    if (showMovePhotosDialog) {
        DestinationPickerDialog(
            title = "Move ${uiState.selectedPhotoIds.size} Notes",
            recentDestinations = uiState.recentDestinations.filter { it.type != DestinationType.GROUP },
            availableFolders = uiState.availableFolders,
            currentFolderId = uiState.folder?.id,
            currentSubfolders = uiState.subfolders,
            workspaces = uiState.workspaces,
            onSelectDestination = { targetFolderId, targetSubId ->
                viewModel.moveSelectedPhotos(targetFolderId, targetSubId)
                showMovePhotosDialog = false
            },
            onDismiss = { showMovePhotosDialog = false }
        )
    }

    // Dialog: Copy Photo to Folder / Subfolder
    photoToCopy?.let { photo ->
        DestinationPickerDialog(
            title = "Copy Note to...",
            recentDestinations = uiState.recentDestinations.filter { it.type != DestinationType.GROUP },
            availableFolders = uiState.availableFolders,
            currentFolderId = uiState.folder?.id,
            currentSubfolders = uiState.subfolders,
            workspaces = uiState.workspaces,
            onSelectDestination = { targetFolderId, targetSubId ->
                viewModel.copyPhoto(photo.id, targetFolderId, targetSubId)
                photoToCopy = null
            },
            onDismiss = { photoToCopy = null }
        )
    }

    // Dialog: Move Group to Folder / Subfolder
    groupToMove?.let { grp ->
        DestinationPickerDialog(
            title = "Move Group \"${grp.name}\" to...",
            recentDestinations = uiState.recentDestinations.filter { it.type != DestinationType.GROUP },
            availableFolders = uiState.availableFolders,
            currentFolderId = uiState.folder?.id,
            currentSubfolders = uiState.subfolders,
            workspaces = uiState.workspaces,
            onSelectDestination = { targetFolderId, targetSubId ->
                viewModel.moveGroup(grp.id, targetFolderId, targetSubId)
                groupToMove = null
            },
            onDismiss = { groupToMove = null }
        )
    }

    // Dialog: Merge Groups
    if (showMergeGroupsDialog) {
        MergeGroupsDialog(
            groupCount = uiState.selectedGroupIds.size,
            onDismiss = { showMergeGroupsDialog = false },
            onConfirm = { groupName ->
                viewModel.mergeSelectedGroups(groupName)
                showMergeGroupsDialog = false
            }
        )
    }

    // Dialog: Add to Group Picker (Task 2)
    if (showAddToGroupDialog) {
        val currentFolderId = uiState.folder?.id ?: viewModel.folderId
        val currentFolderName = uiState.folder?.name ?: "Coursework"
        val sectionResult = remember(currentFolderId, currentFolderName, uiState.availableFolders, uiState.availableGroups) {
            AddToGroupSectionHelper.buildSections(
                currentFolderId = currentFolderId,
                currentFolderName = currentFolderName,
                availableFolders = uiState.availableFolders,
                allGroups = uiState.availableGroups
            )
        }

        AddToGroupPickerDialog(
            sections = sectionResult.sections,
            hasOtherFolderSections = sectionResult.hasOtherFolderSections,
            onDismiss = { showAddToGroupDialog = false },
            onSelectGroup = { grpId, grpName ->
                viewModel.addSelectedPhotosToGroup(grpId, grpName)
                showAddToGroupDialog = false
            }
        )
    }

    // Dialog: Export Format Chooser (PDF / ZIP)
    if (showExportFormatDialog || groupToExport != null) {
        val targetGrp = groupToExport
        FolderExportFormatDialog(
            title = if (targetGrp != null) "Export \"${targetGrp.group.name}\"" else "Export Selected Notes",
            onDismiss = {
                showExportFormatDialog = false
                groupToExport = null
            },
            onExportPdf = {
                if (targetGrp != null) {
                    viewModel.exportGroup(context, targetGrp.group, targetGrp.memberPhotos, false) { file ->
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Export PDF"))
                    }
                } else {
                    viewModel.exportSelected(context, false) { file ->
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Export PDF"))
                    }
                }
                showExportFormatDialog = false
                groupToExport = null
            },
            onExportZip = {
                if (targetGrp != null) {
                    viewModel.exportGroup(context, targetGrp.group, targetGrp.memberPhotos, true) { file ->
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/zip"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Export ZIP"))
                    }
                } else {
                    viewModel.exportSelected(context, true) { file ->
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/zip"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Export ZIP"))
                    }
                }
                showExportFormatDialog = false
                groupToExport = null
            }
        )
    }

    // Dialog: Batch Tag Color Chooser
    if (showBatchColorDialog) {
        BatchTagColorChooserDialog(
            onDismiss = { showBatchColorDialog = false },
            onSelectColor = { colorHex ->
                viewModel.updateSelectedPhotosTagColor(colorHex)
                showBatchColorDialog = false
            }
        )
    }

    // Modal Action Sheet for Long-Pressed Group
    groupActionTarget?.let { targetGroup ->
        val g = targetGroup.group
        GroupQuickActionSheet(
            targetGroup = targetGroup,
            onDismiss = { groupActionTarget = null },
            onTogglePin = {
                val toPin = targetGroup
                groupActionTarget = null
                viewModel.togglePin(toPin)
            },
            onRename = {
                val toRename = g
                groupActionTarget = null
                groupToRename = toRename
            },
            onSchedule = {
                val toSchedule = g
                groupActionTarget = null
                itemToSchedule = Triple(ScheduleNoteType.PHOTO_GROUP, toSchedule.id, toSchedule.name)
                itemScheduledAt = toSchedule.scheduledAt
                itemScheduleTitle = toSchedule.scheduleTitle
                itemAlertType = try {
                    ScheduleAlertType.valueOf(toSchedule.alertType ?: "NOTIFICATION")
                } catch (_: Exception) {
                    ScheduleAlertType.NOTIFICATION
                }
            },
            onUngroup = {
                viewModel.ungroup(g.id)
                groupActionTarget = null
            },
            onAddPhotos = {
                groupForAddingPhotos = targetGroup
                groupActionTarget = null
                showAddPhotosToGroupDialog = true
            },
            onColorLabel = {
                val toColor = g
                groupActionTarget = null
                groupToColor = toColor
            },
            onSelect = {
                viewModel.startBatchSelectionWithGroup(g.id)
                groupActionTarget = null
            },
            onMove = {
                val toMove = g
                groupActionTarget = null
                groupToMove = toMove
            },
            onExport = {
                val toExport = targetGroup
                groupActionTarget = null
                groupToExport = toExport
                showExportFormatDialog = true
            },
            onUnlink = if (targetGroup.isLinked) {
                {
                    viewModel.unlinkGridItem(-g.id)
                    groupActionTarget = null
                }
            } else null,
            onDelete = {
                val toDelete = targetGroup
                groupActionTarget = null
                groupToDelete = toDelete
            }
        )
    }

    // Dialog: Create Note Group
    if (showCreateGroupDialog) {
        CreateGroupDialog(
            selectedCount = uiState.selectedPhotoIds.size,
            onDismiss = { showCreateGroupDialog = false },
            onConfirm = { name ->
                viewModel.createGroupFromSelected(name)
                showCreateGroupDialog = false
            }
        )
    }

    // Dialog: Rename Group
    groupToRename?.let { group ->
        RenameGroupDialog(
            currentName = group.name,
            onDismiss = { groupToRename = null },
            onConfirm = { newName ->
                viewModel.renameGroup(group.id, newName)
                groupToRename = null
            }
        )
    }

    // Dialog: Group Delete Confirmation
    groupToDelete?.let { target ->
        val totalSize = target.memberPhotos.sumOf { it.fileSizeBytes }
        GroupDeleteConfirmDialog(
            group = target.group,
            memberCount = target.memberPhotos.size,
            totalSizeBytes = totalSize,
            onDismiss = { groupToDelete = null },
            onConfirm = {
                viewModel.deleteGroup(target.group.id)
                groupToDelete = null
            }
        )
    }

    // Dialog: Add Photos to Group
    if (showAddPhotosToGroupDialog && groupForAddingPhotos != null) {
        val target = groupForAddingPhotos!!
        val standalonePhotos = uiState.photos.filter { it.groupId == null }
        AddPhotosToGroupDialog(
            groupName = target.group.name,
            availablePhotos = standalonePhotos,
            gridDensity = uiState.gridDensity,
            onDismiss = {
                showAddPhotosToGroupDialog = false
                groupForAddingPhotos = null
            },
            onAdd = { photoIds ->
                viewModel.addPhotosToGroup(target.group.id, photoIds)
                showAddPhotosToGroupDialog = false
                groupForAddingPhotos = null
            }
        )
    }

    // Dialog: Group Color Label
    groupToColor?.let { group ->
        GroupColorDialog(
            onDismiss = { groupToColor = null },
            onSelectColor = { colorHex ->
                viewModel.updateGroupTagColor(group.id, colorHex)
                groupToColor = null
            }
        )
    }

    // Group Scoped Slider Viewer Modal
    inspectingGroup?.let { target ->
        GroupSliderViewerModal(
            group = target.group,
            memberPhotos = target.memberPhotos,
            onDismiss = { inspectingGroup = null },
            onRotatePhoto = { viewModel.rotatePhoto(it.id) },
            onCropPhoto = { inspectingPhoto = it },
            onRemoveFromGroup = { viewModel.removePhotoFromGroup(it.id) }
        )
    }

    if (showBatchRenameDialog) {
        BatchRenameDialog(
            itemCount = uiState.totalSelectionCount,
            initialBaseName = "Note",
            onConfirm = { baseName ->
                viewModel.batchRename(baseName)
                showBatchRenameDialog = false
            },
            onDismiss = { showBatchRenameDialog = false }
        )
    }

    if (showUnifiedShareDialog) {
        val itemsToShare = shareTargetItems.ifEmpty {
            uiState.gridItems.filter { item ->
                when (item) {
                    is FolderGridItem.StandalonePhoto -> item.photo.id in uiState.selectedPhotoIds
                    is FolderGridItem.Group -> item.group.id in uiState.selectedGroupIds
                    is FolderGridItem.Document -> item.documentNote.id in uiState.selectedDocumentIds
                    is FolderGridItem.TextNoteItem -> item.textNote.id in uiState.selectedTextNoteIds
                    is FolderGridItem.CanvasNoteItem -> item.canvasNote.id in uiState.selectedCanvasNoteIds
                }
            }
        }
        val hasTextNotes = itemsToShare.any { it is FolderGridItem.TextNoteItem }
        val combineItems: List<CombineItem> = itemsToShare.mapNotNull { item ->
            when (item) {
                is FolderGridItem.StandalonePhoto -> CombineItem.StandalonePhoto(item.photo)
                is FolderGridItem.Group -> CombineItem.Group(item.group, item.memberPhotos)
                is FolderGridItem.Document -> CombineItem.Document(item.documentNote, item.pages)
                is FolderGridItem.TextNoteItem -> CombineItem.TextNoteItem(item.textNote)
                is FolderGridItem.CanvasNoteItem -> CombineItem.CanvasNoteItem(item.canvasNote)
            }
        }
        val totalPages = combineItems.sumOf { it.pageCount() }

        val folderTitle = uiState.folder?.name ?: "Coursework"
        UnifiedShareDialog(
            itemCount = itemsToShare.size,
            totalPages = totalPages,
            isProcessing = shareProcessing,
            progressCurrent = shareProgressCurrent,
            progressTotal = shareProgressTotal,
            errorMessage = shareErrorMessage,
            containsTextNotes = hasTextNotes,
            fileNamePresetTemplate = uiState.combineFileNamePreset,
            folderName = folderTitle,
            onFormatSelected = { choice, customFileName ->
                if (totalPages > 100 && (choice == ShareFormatChoice.PDF || choice == ShareFormatChoice.WORD)) {
                    shareErrorMessage = "Selection exceeds 100-page limit ($totalPages pages). Please select fewer items."
                    return@UnifiedShareDialog
                }
                shareProcessing = true
                shareProgressCurrent = 0
                shareProgressTotal = totalPages
                coroutineScope.launch {
                    try {
                        val combineManager = CombineManager(context, canvasRepository, canvasAssetManager)
                        val file = when (choice) {
                            ShareFormatChoice.PDF -> {
                                val outputFileName = com.arinara.fotara.util.FileNamePresetHelper.buildFinalFileName(
                                    customName = customFileName,
                                    presetTemplate = uiState.combineFileNamePreset,
                                    folderName = folderTitle,
                                    itemCount = itemsToShare.size,
                                    extension = ".pdf"
                                )
                                combineManager.combineToPdf(folderTitle, combineItems, outputFileName = outputFileName) { cur, tot ->
                                    shareProgressCurrent = cur
                                    shareProgressTotal = tot
                                }
                            }
                            ShareFormatChoice.WORD -> {
                                val outputFileName = com.arinara.fotara.util.FileNamePresetHelper.buildFinalFileName(
                                    customName = customFileName,
                                    presetTemplate = uiState.combineFileNamePreset,
                                    folderName = folderTitle,
                                    itemCount = itemsToShare.size,
                                    extension = ".docx"
                                )
                                combineManager.combineToDocx(folderTitle, combineItems, outputFileName = outputFileName) { cur, tot ->
                                    shareProgressCurrent = cur
                                    shareProgressTotal = tot
                                }
                            }
                            ShareFormatChoice.ORIGINAL -> {
                                ZipExporter.exportGridItemsToZip(context, folderTitle, itemsToShare)
                            }
                        }
                        shareProcessing = false
                        showUnifiedShareDialog = false
                        shareErrorMessage = null
                        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        val mimeType = when (choice) {
                            ShareFormatChoice.PDF -> "application/pdf"
                            ShareFormatChoice.WORD -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                            ShareFormatChoice.ORIGINAL -> "application/zip"
                        }
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = mimeType
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share notes"))
                    } catch (e: Exception) {
                        shareProcessing = false
                        shareErrorMessage = e.message ?: "Export failed"
                    }
                }
            },
            onCancelProcessing = {
                shareProcessing = false
            },
            onDismiss = {
                showUnifiedShareDialog = false
                shareErrorMessage = null
            }
        )
    }

    documentActionTarget?.let { target ->
        DocumentNoteQuickActionSheet(
            target = target,
            onDismiss = { documentActionTarget = null },
            onTogglePin = {
                documentActionTarget = null
                viewModel.togglePin(target)
            },
            onRename = {
                documentToRename = target.documentNote
                documentActionTarget = null
            },
            onColorLabel = {
                documentToColor = target.documentNote
                documentActionTarget = null
            },
            onDeadline = {
                documentToDeadline = target.documentNote
                documentActionTarget = null
            },
            onSchedule = {
                val doc = target.documentNote
                itemToSchedule = Triple(ScheduleNoteType.DOCUMENT, doc.id, doc.name)
                itemScheduledAt = doc.scheduledAt
                itemScheduleTitle = doc.scheduleTitle
                itemAlertType = try {
                    ScheduleAlertType.valueOf(doc.alertType ?: "NOTIFICATION")
                } catch (_: Exception) {
                    ScheduleAlertType.NOTIFICATION
                }
                documentActionTarget = null
            },
            onMove = {
                documentToMove = target.documentNote
                documentActionTarget = null
            },
            onShareAs = {
                shareTargetItems = listOf(target)
                showUnifiedShareDialog = true
                documentActionTarget = null
            },
            onSplitToImages = if (target.documentNote.docType == DocumentType.PDF) {
                {
                    pdfToSplit = target.documentNote
                    documentActionTarget = null
                }
            } else null,
            onSelect = {
                viewModel.startBatchSelectionWithDocument(target.documentNote.id)
                documentActionTarget = null
            },
            onDelete = {
                documentToDelete = target.documentNote
                documentActionTarget = null
            }
        )
    }

    documentToRename?.let { doc ->
        RenameDocumentDialog(
            initialTitle = doc.name,
            onDismiss = { documentToRename = null },
            onConfirm = { newTitle ->
                viewModel.renameDocument(doc.id, newTitle)
                documentToRename = null
            }
        )
    }

    pdfToSplit?.let { doc ->
        SplitPdfConfirmDialog(
            documentName = doc.name,
            pageCount = doc.pageCount,
            onDismiss = { pdfToSplit = null },
            onConfirm = {
                viewModel.splitPdfToImages(doc.id)
                pdfToSplit = null
            }
        )
    }

    documentToDelete?.let { doc ->
        DeleteDocumentConfirmDialog(
            documentName = doc.name,
            onDismiss = { documentToDelete = null },
            onConfirm = {
                viewModel.deleteDocument(doc.id)
                documentToDelete = null
            }
        )
    }

    inspectingDocument?.let { doc ->
        if (doc.docType == DocumentType.PDF) {
            val pages by (viewModel.documentRepository?.getDocumentPages(doc.id) ?: flowOf<List<com.arinara.fotara.data.model.DocumentPage>>(emptyList()))
                .collectAsStateWithLifecycle(initialValue = emptyList())
            val context = LocalContext.current
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ElectricSlateTokens.BgCanvas)
            ) {
                PdfViewerScreen(
                    documentNote = doc,
                    pages = pages,
                    initialPageIndex = targetPageIndex ?: 0,
                    highlightPageIndex = targetPageIndex,
                    pdfPagePinRepository = pdfPagePinRepository,
                    pdfPageDrawingRepository = pdfPageDrawingRepository,
                    settingsRepository = settingsRepository,
                    searchQuery = searchQuery,
                    onNavigateToEditor = onNavigateToEditor,
                    onBack = { 
                        inspectingDocument = null 
                        inspectingDocumentId = null
                    },
                    onShare = {
                        try {
                            val file = File(doc.originFileUri)
                            val shareUri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, shareUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share PDF"))
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "Share failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    onSplitToImages = {
                        inspectingDocument = null
                        inspectingDocumentId = null
                        viewModel.splitPdfToImages(doc.id)
                    },
                    onDelete = {
                        inspectingDocument = null
                        inspectingDocumentId = null
                        viewModel.deleteDocument(doc.id)
                    }
                )
            }
        }
    }

    // Determinate PDF Import Progress Dialog (v1.4 Workstream 2)
    uiState.importProgress?.let { (current, total) ->
        DocumentImportProgressDialog(
            current = current,
            total = total,
            onCancel = { viewModel.cancelImport() }
        )
    }

    // Determinate PDF Split Progress Dialog (Chunk C2)
    uiState.splitProgress?.let { (current, total) ->
        PdfSplitProgressDialog(
            current = current,
            total = total,
            onCancel = { viewModel.cancelSplit() }
        )
    }

    // Full-Screen DOCX In-App Viewer Modal (v1.4 Workstream 3)
    inspectingDocx?.let { doc ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ElectricSlateTokens.BgCanvas)
        ) {
            DocxViewerScreen(
                documentNote = doc,
                onBack = { inspectingDocx = null },
                onDelete = {
                    inspectingDocx = null
                    viewModel.deleteDocument(doc.id)
                }
            )
        }
    }

    // Text Note Quick Action Sheet (v1.4 Workstream 4)
    textNoteActionTarget?.let { target ->
        val textNote = target.textNote
        TextNoteQuickActionSheet(
            note = textNote,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            onDismiss = { textNoteActionTarget = null },
            onRename = {
                textNoteActionTarget = null
                textNoteToRename = textNote
            },
            onMove = {
                textNoteActionTarget = null
                textNoteToMove = textNote
            },
            onColorSelect = { color ->
                viewModel.updateTextNoteTagColor(textNote.id, color)
                textNoteActionTarget = null
            },
            onSetDeadline = {
                textNoteActionTarget = null
                if (textNote.linkedDeadline != null) {
                    viewModel.setTextNoteDeadline(textNote.id, null)
                } else {
                    val tomorrow = System.currentTimeMillis() + 24 * 60 * 60 * 1000L
                    viewModel.setTextNoteDeadline(textNote.id, tomorrow)
                }
            },
            onSchedule = {
                textNoteActionTarget = null
                itemToSchedule = Triple(ScheduleNoteType.TEXT_NOTE, textNote.id, textNote.title)
                itemScheduledAt = textNote.scheduledAt
                itemScheduleTitle = textNote.scheduleTitle
                itemAlertType = try {
                    ScheduleAlertType.valueOf(textNote.alertType ?: "NOTIFICATION")
                } catch (_: Exception) {
                    ScheduleAlertType.NOTIFICATION
                }
            },
            onTogglePin = { viewModel.togglePin(target) },
            onSelect = {
                viewModel.startBatchSelectionWithTextNote(textNote.id)
                textNoteActionTarget = null
            },
            onShare = {
                textNoteActionTarget = null
                textNoteToShare = textNote
            },
            onDelete = {
                textNoteActionTarget = null
                textNoteToDelete = textNote
            }
        )
    }

    // Text Note Share Dialog (.md / .txt only)
    textNoteToShare?.let { note ->
        TextNoteShareDialog(
            note = note,
            onDismiss = { textNoteToShare = null },
            onShareMarkdown = {
                textNoteToShare = null
                com.arinara.fotara.util.TextNoteExporter.shareNoteAsMarkdown(context, note)
            },
            onSharePlainText = {
                textNoteToShare = null
                com.arinara.fotara.util.TextNoteExporter.shareNoteAsPlainText(context, note)
            }
        )
    }

    // Dialog: Rename Text Note
    textNoteToRename?.let { note ->
        RenameTextNoteDialog(
            initialTitle = note.title,
            onDismiss = { textNoteToRename = null },
            onConfirm = { newTitle ->
                viewModel.renameTextNote(note.id, newTitle)
                textNoteToRename = null
            }
        )
    }

    // Dialog: Delete Text Note Confirmation
    textNoteToDelete?.let { note ->
        DeleteTextNoteConfirmDialog(
            title = note.title,
            onDismiss = { textNoteToDelete = null },
            onConfirm = {
                viewModel.deleteTextNote(note.id)
                textNoteToDelete = null
            }
        )
    }

    // Dialog: Move Text Note
    textNoteToMove?.let { note ->
        DestinationPickerDialog(
            title = "Move Note \"${note.title}\" to...",
            recentDestinations = uiState.recentDestinations.filter { it.type != DestinationType.GROUP },
            availableFolders = uiState.availableFolders,
            currentFolderId = uiState.folder?.id,
            currentSubfolders = uiState.subfolders,
            workspaces = uiState.workspaces,
            onSelectDestination = { targetFolderId, targetSubId ->
                viewModel.moveTextNote(note.id, targetFolderId, targetSubId)
                textNoteToMove = null
            },
            onDismiss = { textNoteToMove = null }
        )
    }

    // Canvas Note Quick Action Sheet (v1.5.2 Item 12)
    canvasNoteActionTarget?.let { target ->
        val canvasNote = target.canvasNote
        CanvasNoteQuickActionSheet(
            note = canvasNote,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            onDismiss = { canvasNoteActionTarget = null },
            onRename = {
                canvasNoteActionTarget = null
                canvasNoteToRename = canvasNote
            },
            onMove = {
                canvasNoteActionTarget = null
                canvasNoteToMove = canvasNote
            },
            onMoveToGroup = {
                canvasNoteActionTarget = null
                viewModel.startBatchSelectionWithCanvasNote(canvasNote.id)
                showAddToGroupDialog = true
            },
            onColorSelect = { color ->
                viewModel.updateCanvasNoteTagColor(canvasNote.id, color)
                canvasNoteActionTarget = null
            },
            onSetDeadline = {
                canvasNoteActionTarget = null
                if (canvasNote.linkedDeadline != null) {
                    viewModel.setCanvasNoteDeadline(canvasNote.id, null)
                } else {
                    val tomorrow = System.currentTimeMillis() + 24 * 60 * 60 * 1000L
                    viewModel.setCanvasNoteDeadline(canvasNote.id, tomorrow)
                }
            },
            onSchedule = {
                canvasNoteActionTarget = null
                itemToSchedule = Triple(ScheduleNoteType.CANVAS_NOTE, canvasNote.id, canvasNote.title)
                itemScheduledAt = canvasNote.scheduledAt
                itemScheduleTitle = canvasNote.scheduleTitle
                itemAlertType = try {
                    ScheduleAlertType.valueOf(canvasNote.alertType ?: "NOTIFICATION")
                } catch (_: Exception) {
                    ScheduleAlertType.NOTIFICATION
                }
            },
            onTogglePin = { viewModel.togglePin(target) },
            onSelect = {
                viewModel.startBatchSelectionWithCanvasNote(canvasNote.id)
                canvasNoteActionTarget = null
            },
            onShare = {
                canvasNoteActionTarget = null
                val thumbFile = canvasNote.thumbnailPath?.let { File(it) }
                if (thumbFile != null && thumbFile.exists()) {
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        thumbFile
                    )
                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Canvas Note"))
                } else {
                    shareTargetItems = listOf(target)
                    showUnifiedShareDialog = true
                }
            },
            onDelete = {
                canvasNoteActionTarget = null
                canvasNoteToDelete = canvasNote
            }
        )
    }

    // Dialog: Rename Canvas Note
    canvasNoteToRename?.let { note ->
        RenameCanvasNoteDialog(
            initialTitle = note.title,
            onDismiss = { canvasNoteToRename = null },
            onConfirm = { newTitle ->
                viewModel.renameCanvasNote(note.id, newTitle)
                canvasNoteToRename = null
            }
        )
    }

    // Dialog: Delete Canvas Note Confirmation
    canvasNoteToDelete?.let { note ->
        DeleteCanvasNoteConfirmDialog(
            title = note.title,
            onDismiss = { canvasNoteToDelete = null },
            onConfirm = {
                viewModel.deleteCanvasNote(note.id)
                canvasNoteToDelete = null
            }
        )
    }

    // Dialog: Move Canvas Note
    canvasNoteToMove?.let { note ->
        DestinationPickerDialog(
            title = "Move Canvas Note \"${note.title}\" to...",
            recentDestinations = uiState.recentDestinations.filter { it.type != DestinationType.GROUP },
            availableFolders = uiState.availableFolders,
            currentFolderId = uiState.folder?.id,
            currentSubfolders = uiState.subfolders,
            workspaces = uiState.workspaces,
            onSelectDestination = { targetFolderId, targetSubId ->
                viewModel.moveCanvasNote(note.id, targetFolderId, targetSubId)
                canvasNoteToMove = null
            },
            onDismiss = { canvasNoteToMove = null }
        )
    }

    itemToSchedule?.let { (noteType, noteId, title) ->
        val currentFolderId = uiState.folder?.id ?: 0L
        val scheduleManager = remember { NoteScheduleManager(context) }
        ScheduleNoteDialog(
            noteTitle = title,
            initialScheduledAt = itemScheduledAt,
            initialAlertType = itemAlertType,
            initialScheduleTitle = itemScheduleTitle,
            onDismiss = { itemToSchedule = null },
            onSaveSchedule = { scheduledAt, alertType, scheduleTitle ->
                scheduleManager.scheduleNote(
                    noteType = noteType,
                    noteId = noteId,
                    folderId = currentFolderId,
                    title = title,
                    triggerAtMillis = scheduledAt,
                    alertType = alertType,
                    scheduleTitle = scheduleTitle
                )
                viewModel.refresh()
                itemToSchedule = null
            },
            onClearSchedule = {
                scheduleManager.cancelSchedule(noteType, noteId)
                viewModel.refresh()
                itemToSchedule = null
            }
        )
    }
}

@Composable
private fun DetailDocumentCard(
    documentItem: FolderGridItem.Document,
    isBatchMode: Boolean,
    isSelected: Boolean,
    isHighlighted: Boolean = false,
    highlightAlpha: Float = 0f,
    glowCorner: GlowCorner? = null,
    glowAnchors: Set<GlowAnchor> = emptySet(),
    onCardClick: () -> Unit,
    onCardLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val doc = documentItem.documentNote
    val pageCount = doc.pageCount
    val isPdf = doc.docType == DocumentType.PDF
    val firstPage = documentItem.pages.firstOrNull()
    val dateStr = remember(doc.addedAt) {
        SimpleDateFormat("MMM d", Locale.US).format(Date(doc.addedAt))
    }

    val effectiveAnchors = if (glowAnchors.isNotEmpty()) {
        glowAnchors
    } else if (glowCorner != null) {
        setOf(glowCorner.toGlowAnchor())
    } else {
        emptySet()
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ElectricSlateTokens.SurfacePanel,
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) ElectricSlateTokens.TextPrimary else ElectricSlateTokens.BorderSubtle
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .linkItGlow(
                isLinked = documentItem.isLinked && effectiveAnchors.isNotEmpty(),
                anchors = effectiveAnchors,
                linkedDescription = "Linked document ${documentItem.documentNote.name}",
                cornerRadiusDp = 16f
            )
            .combinedClickable(
                onClick = onCardClick,
                onLongClick = onCardLongClick
            )
    ) {
        Box {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(ElectricSlateTokens.BgCanvas),
                    contentAlignment = Alignment.Center
                ) {
                    val coverImagePath = firstPage?.imageUri
                    if (!coverImagePath.isNullOrBlank()) {
                        val context = LocalContext.current
                        val imageRequest = remember(coverImagePath) {
                            ImageRequest.Builder(context)
                                .data(File(coverImagePath))
                                .crossfade(false)
                                .memoryCacheKey(coverImagePath)
                                .diskCacheKey(coverImagePath)
                                .build()
                        }
                        AsyncImage(
                            model = imageRequest,
                            contentDescription = doc.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isPdf) Icons.Default.PictureAsPdf else Icons.Default.Description,
                                contentDescription = null,
                                tint = if (isPdf) ElectricSlateTokens.Destructive else ElectricSlateTokens.PrimaryAccent,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isPdf) "PDF" else "DOCX",
                                color = ElectricSlateTokens.TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Format Badge in Top-Left (e.g. PDF / DOCX chip)
                    Surface(
                        color = ElectricSlateTokens.SurfaceElevated,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (isPdf) "PDF" else "DOCX",
                                color = if (isPdf) ElectricSlateTokens.Destructive else ElectricSlateTokens.PrimaryAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (isPdf) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.doc_card_page_short, pageCount),
                                    color = ElectricSlateTokens.TextPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    if (doc.isPinned) {
                        Surface(
                            shape = CircleShape,
                            color = ElectricSlateTokens.TrackInactive,
                            modifier = Modifier
                                .align(if (isBatchMode) Alignment.TopCenter else Alignment.TopEnd)
                                .padding(8.dp)
                                .size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned note",
                                tint = ElectricSlateTokens.PrimaryAccent,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }

                    // Multi-select Checkmark Badge
                    if (isBatchMode) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.BgCanvas.copy(alpha = 0.7f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                ElectricSlateTokens.PrimaryAccent
                            ),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(24.dp)
                        ) {
                            if (isSelected) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = ElectricSlateTokens.TextPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Card Footer
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = doc.name,
                            color = ElectricSlateTokens.TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        doc.tagColor?.let { tagHex ->
                            val color = remember(tagHex) { TagColor.fromHex(tagHex).composeColor }
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = CircleShape,
                                color = color,
                                modifier = Modifier.size(8.dp)
                            ) {}
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val pageString = if (pageCount == 1) {
                            stringResource(R.string.doc_card_single_page, pageCount)
                        } else {
                            stringResource(R.string.doc_card_multiple_pages, pageCount)
                        }
                        Text(
                            text = if (isPdf) "$pageString · $dateStr" else dateStr,
                            color = ElectricSlateTokens.TextMuted,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        if (doc.scheduledAt != null) {
                            CardScheduleBadge(
                                scheduledAt = doc.scheduledAt,
                                alertType = doc.alertType
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        if (doc.linkedDeadline != null) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = "Deadline",
                                tint = ElectricSlateTokens.PrimaryAccent,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            // Exposure/brightness highlight overlay for search navigation
            if (isHighlighted && highlightAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = highlightAlpha))
                )
            }
        }
    }
}

@Composable
internal fun DetailPhotoCard(
    photo: Photo,
    isBatchMode: Boolean,
    isSelected: Boolean,
    isLinked: Boolean = false,
    isHighlighted: Boolean = false,
    highlightAlpha: Float = 0f,
    glowCorner: GlowCorner? = null,
    glowAnchors: Set<GlowAnchor> = emptySet(),
    onCardClick: () -> Unit,
    onCardLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dateStr = remember(photo.addedAt) {
        SimpleDateFormat("MMM d", Locale.US).format(Date(photo.addedAt))
    }

    val effectiveAnchors = if (glowAnchors.isNotEmpty()) {
        glowAnchors
    } else if (glowCorner != null) {
        setOf(glowCorner.toGlowAnchor())
    } else {
        emptySet()
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ElectricSlateTokens.SurfacePanel,
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) ElectricSlateTokens.TextPrimary else ElectricSlateTokens.BorderSubtle
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .linkItGlow(
                isLinked = isLinked && effectiveAnchors.isNotEmpty(),
                anchors = effectiveAnchors,
                linkedDescription = "Linked note",
                cornerRadiusDp = 16f
            )
            .combinedClickable(
                onClick = onCardClick,
                onLongClick = onCardLongClick
            )
    ) {
        Box {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(ElectricSlateTokens.BgCanvas),
                    contentAlignment = Alignment.Center
                ) {
                    val imageModel = photo.thumbnailUri ?: photo.fileUri
                    if (imageModel.isNotBlank()) {
                        val context = LocalContext.current
                        val imageRequest = remember(imageModel) {
                            val data = if (imageModel.startsWith("content://") || imageModel.startsWith("file://")) {
                                imageModel
                            } else {
                                File(imageModel)
                            }
                            ImageRequest.Builder(context)
                                .data(data)
                                .crossfade(false)
                                .memoryCacheKey(imageModel)
                                .diskCacheKey(imageModel)
                                .build()
                        }
                        AsyncImage(
                            model = imageRequest,
                            contentDescription = photo.caption ?: "Photo note",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = ElectricSlateTokens.TextMuted,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    if (photo.isPinned) {
                        Surface(
                            shape = CircleShape,
                            color = ElectricSlateTokens.TrackInactive,
                            modifier = Modifier
                                .align(if (isBatchMode) Alignment.TopCenter else Alignment.TopStart)
                                .padding(8.dp)
                                .size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned note",
                                tint = ElectricSlateTokens.PrimaryAccent,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }

                    if (isBatchMode) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isSelected) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TextMuted,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .size(22.dp)
                        )
                    }

                    photo.tag?.let { tag ->
                        Surface(
                            shape = CircleShape,
                            color = tag.composeColor,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(10.dp)
                        ) {}
                    }
                }

                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = photo.caption ?: "Untitled Note",
                        color = ElectricSlateTokens.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dateStr,
                            color = ElectricSlateTokens.TextMuted,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        if (photo.scheduledAt != null) {
                            CardScheduleBadge(
                                scheduledAt = photo.scheduledAt,
                                alertType = photo.alertType
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        if (photo.linkedDeadline != null) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = "Deadline",
                                tint = ElectricSlateTokens.PrimaryAccent,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            // Exposure/brightness highlight overlay for search navigation
            if (isHighlighted && highlightAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = highlightAlpha))
                )
            }
        }
    }
}

@Composable
private fun DetailGroupCard(
    groupItem: FolderGridItem.Group,
    isBatchMode: Boolean,
    isSelected: Boolean,
    isHighlighted: Boolean = false,
    highlightAlpha: Float = 0f,
    glowCorner: GlowCorner? = null,
    glowAnchors: Set<GlowAnchor> = emptySet(),
    onCardClick: () -> Unit,
    onCardLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val group = groupItem.group
    val memberCount = groupItem.memberPhotos.size
    val coverPhoto = groupItem.coverPhoto
    val dateStr = remember(group.createdAt) {
        SimpleDateFormat("MMM d", Locale.US).format(Date(group.createdAt))
    }

    val effectiveAnchors = if (glowAnchors.isNotEmpty()) {
        glowAnchors
    } else if (glowCorner != null) {
        setOf(glowCorner.toGlowAnchor())
    } else {
        emptySet()
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ElectricSlateTokens.SurfacePanel,
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) ElectricSlateTokens.TextPrimary else ElectricSlateTokens.BorderSubtle
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .linkItGlow(
                isLinked = groupItem.isLinked && effectiveAnchors.isNotEmpty(),
                anchors = effectiveAnchors,
                linkedDescription = "Linked group ${groupItem.group.name}",
                cornerRadiusDp = 16f
            )
            .combinedClickable(
                onClick = onCardClick,
                onLongClick = onCardLongClick
            )
    ) {
        Box {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(ElectricSlateTokens.BgCanvas),
                    contentAlignment = Alignment.Center
                ) {
                    val imageModel = coverPhoto?.thumbnailUri ?: coverPhoto?.fileUri
                    if (!imageModel.isNullOrBlank()) {
                        val context = LocalContext.current
                        val imageRequest = remember(imageModel) {
                            val data = if (imageModel.startsWith("content://") || imageModel.startsWith("file://")) {
                                imageModel
                            } else {
                                File(imageModel)
                            }
                            ImageRequest.Builder(context)
                                .data(data)
                                .crossfade(false)
                                .memoryCacheKey(imageModel)
                                .diskCacheKey(imageModel)
                                .build()
                        }
                        AsyncImage(
                            model = imageRequest,
                            contentDescription = group.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = ElectricSlateTokens.TextMuted,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Stacked-photos Badge in Top-Left (Icon + Count, e.g. Layers + "5")
                    Surface(
                        color = ElectricSlateTokens.SurfaceElevated,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = ElectricSlateTokens.PrimaryAccent,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$memberCount",
                                color = ElectricSlateTokens.TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (group.isPinned) {
                        Surface(
                            shape = CircleShape,
                            color = ElectricSlateTokens.TrackInactive,
                            modifier = Modifier
                                .align(if (group.tag != null) Alignment.TopCenter else Alignment.TopEnd)
                                .padding(8.dp)
                                .size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned note",
                                tint = ElectricSlateTokens.PrimaryAccent,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }

                    // Multi-select Checkmark Badge
                    if (isBatchMode) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isSelected) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TextMuted,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .size(22.dp)
                        )
                    }

                    // Tag Color Indicator in Top-Right
                    group.tag?.let { tag ->
                        Surface(
                            shape = CircleShape,
                            color = tag.composeColor,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(12.dp)
                        ) {}
                    }
                }

                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = ElectricSlateTokens.PrimaryAccent,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = group.name,
                            color = ElectricSlateTokens.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$memberCount notes · $dateStr",
                            color = ElectricSlateTokens.TextMuted,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        if (group.scheduledAt != null) {
                            CardScheduleBadge(
                                scheduledAt = group.scheduledAt,
                                alertType = group.alertType
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        if (groupItem.sortDeadline != null) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = "Earliest Deadline",
                                tint = ElectricSlateTokens.PrimaryAccent,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            // Exposure/brightness highlight overlay for search navigation
            if (isHighlighted && highlightAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = highlightAlpha))
                )
            }
        }
    }
}







@Composable
private fun BatchSelectActionDock(
    uiState: FolderDetailUiState,
    onLinkClick: () -> Unit,
    onCreateGroupClick: () -> Unit,
    onMergeGroupsClick: () -> Unit,
    onAddToGroupClick: () -> Unit,
    onRenameClick: () -> Unit,
    onShareClick: () -> Unit,
    onMoveClick: () -> Unit,
    onColorClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ElectricSlateTokens.SurfacePanel,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
        modifier = modifier.fillMaxWidth()
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Link items (2..4 selected)
            if (uiState.totalSelectionCount in 2..4) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onLinkClick() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "Link Items",
                            tint = ElectricSlateTokens.TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Link",
                            color = ElectricSlateTokens.TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 2. Group notes (2+ photos, no groups)
            if (uiState.selectedPhotoIds.size >= 2 && uiState.selectedGroupIds.isEmpty()) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onCreateGroupClick() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Group",
                            tint = ElectricSlateTokens.TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text("Group", color = ElectricSlateTokens.TextPrimary, fontSize = 11.sp)
                    }
                }
            }

            // 3. Merge groups (2+ groups, no standalone photos)
            if (uiState.selectedGroupIds.size >= 2 && uiState.selectedPhotoIds.isEmpty()) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onMergeGroupsClick() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MergeType,
                            contentDescription = "Merge",
                            tint = ElectricSlateTokens.TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text("Merge", color = ElectricSlateTokens.TextPrimary, fontSize = 11.sp)
                    }
                }
            }

            // 4. Add to group (1+ photos or canvas notes, no groups)
            if ((uiState.selectedPhotoIds.isNotEmpty() || uiState.selectedCanvasNoteIds.isNotEmpty()) && uiState.selectedGroupIds.isEmpty()) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onAddToGroupClick() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GroupAdd,
                            contentDescription = "Add to Group",
                            tint = ElectricSlateTokens.TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text("Add to Grp", color = ElectricSlateTokens.TextPrimary, fontSize = 11.sp)
                    }
                }
            }

            // 5. Rename (1 item or batch)
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onRenameClick() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Rename",
                        tint = ElectricSlateTokens.TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text("Rename", color = ElectricSlateTokens.TextPrimary, fontSize = 11.sp)
                }
            }

            // 6. Share As
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onShareClick() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = ElectricSlateTokens.TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text("Share As", color = ElectricSlateTokens.TextPrimary, fontSize = 11.sp)
                }
            }

            // 7. Move
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onMoveClick() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.DriveFileMove,
                        contentDescription = "Move",
                        tint = ElectricSlateTokens.TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text("Move", color = ElectricSlateTokens.TextPrimary, fontSize = 11.sp)
                }
            }

            // 8. Color
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onColorClick() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                    Icon(
                        imageVector = Icons.Default.ColorLens,
                        contentDescription = "Color",
                        tint = ElectricSlateTokens.TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text("Color", color = ElectricSlateTokens.TextPrimary, fontSize = 11.sp)
                }
            }

            // 9. Delete
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onDeleteClick() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = ElectricSlateTokens.Destructive,
                        modifier = Modifier.size(24.dp)
                    )
                    Text("Delete", color = ElectricSlateTokens.Destructive, fontSize = 11.sp)
                }
            }
        }
    }
}

