// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.group

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.ui.components.CaptureReviewSliderModal
import com.arinara.fotara.ui.components.workspaceGroupedFolderItems
import com.arinara.fotara.data.model.Workspace
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arinara.fotara.data.model.DestinationType
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.storage.PhotoStorageManager
import com.arinara.fotara.ocr.FolderSuggestEngine
import com.arinara.fotara.ocr.OcrEngine
import com.arinara.fotara.ui.capture.MultiCaptureScreen
import com.arinara.fotara.ui.folder.DetailPhotoCard
import com.arinara.fotara.ui.photo.PhotoViewerDialog
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import com.arinara.fotara.ui.components.presets.AppBottomSheet
import com.arinara.fotara.ui.components.presets.AppButton
import com.arinara.fotara.ui.components.presets.AppButtonVariant
import com.arinara.fotara.ui.components.NoteDetailScheduleChip
import com.arinara.fotara.ui.components.ScheduleNoteDialog
import com.arinara.fotara.util.NoteScheduleManager
import com.arinara.fotara.util.ScheduleNoteType
import com.arinara.fotara.util.ScheduleAlertType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    viewModel: GroupDetailViewModel,
    photoStorageManager: PhotoStorageManager? = null,
    ocrEngine: OcrEngine? = null,
    folderSuggestEngine: FolderSuggestEngine? = null,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showOverflowMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showUngroupConfirmDialog by remember { mutableStateOf(false) }
    var showAddPhotosDialog by remember { mutableStateOf(false) }
    var showAddOptionsSheet by remember { mutableStateOf(false) }
    var showMultiCapture by remember { mutableStateOf(false) }
    var showDeadlineDialog by remember { mutableStateOf(false) }
    var showMoveGroupDialog by remember { mutableStateOf(false) }
    var showScheduleDialog by remember { mutableStateOf(false) }
    var showExportGroupDialog by remember { mutableStateOf(false) }

    var capturedBatchPhotos by remember { mutableStateOf<List<Photo>?>(null) }
    var inspectingPhoto by remember { mutableStateOf<Photo?>(null) }
    var contextMenuPhoto by remember { mutableStateOf<Photo?>(null) }

    val gridState = rememberLazyGridState()
    val highlightAlpha = remember { Animatable(0f) }

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

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    // Search result landing highlight inside group
    LaunchedEffect(uiState.highlightedPhotoId, uiState.photos) {
        val targetId = uiState.highlightedPhotoId ?: return@LaunchedEffect
        val targetIndex = uiState.photos.indexOfFirst { it.id == targetId }
        if (targetIndex != -1) {
            val isVisible = gridState.layoutInfo.visibleItemsInfo.any { it.index == targetIndex }
            if (!isVisible) {
                gridState.animateScrollToItem(targetIndex)
            }
            highlightAlpha.animateTo(0.28f, tween(250, easing = FastOutSlowInEasing))
            delay(1500L)
            highlightAlpha.animateTo(0f, tween(350, easing = FastOutSlowInEasing))
            viewModel.clearHighlightedPhoto()
        }
    }

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val baseDensity = uiState.gridDensity
    val photoColumns = when {
        screenWidthDp >= 840 -> baseDensity + 3
        screenWidthDp >= 600 -> baseDensity + 1
        else -> baseDensity
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = uiState.group?.name ?: "Group",
                            color = ElectricSlateTokens.TextPrimary,
                            fontSize = 22.sp,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        uiState.group?.tag?.let { tag ->
                            Spacer(modifier = Modifier.width(10.dp))
                            Surface(
                                shape = CircleShape,
                                color = tag.composeColor,
                                modifier = Modifier.size(10.dp)
                            ) {}
                        }
                        uiState.group?.let { group ->
                            if (group.scheduledAt != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                NoteDetailScheduleChip(
                                    scheduledAt = group.scheduledAt,
                                    alertType = group.alertType,
                                    scheduleTitle = group.scheduleTitle,
                                    onClick = { showScheduleDialog = true }
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Folder",
                            tint = ElectricSlateTokens.TextPrimary
                        )
                    }
                },
                actions = {
                    // Right Action 1: Add Photos Dropdown Button [+]
                    IconButton(
                        onClick = { showAddOptionsSheet = true },
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ElectricSlateTokens.PrimaryAccent)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Notes to Group",
                            tint = ElectricSlateTokens.TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Right Action 2: Highlighted Overflow Menu [(⋮)]
                    Box(modifier = Modifier.padding(end = 12.dp)) {
                        IconButton(
                            onClick = { showOverflowMenu = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ElectricSlateTokens.TrackInactive)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Group Menu",
                                tint = ElectricSlateTokens.TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false },
                            modifier = Modifier
                                .background(ElectricSlateTokens.SurfaceElevated)
                                .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Rename", color = ElectricSlateTokens.TextPrimary) },
                                leadingIcon = { Icon(Icons.Default.Edit, null, tint = ElectricSlateTokens.TextMuted) },
                                onClick = {
                                    showOverflowMenu = false
                                    showRenameDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (uiState.group?.linkedDeadline != null) "Edit Deadline" else "Set Deadline",
                                        color = ElectricSlateTokens.TextPrimary
                                    )
                                },
                                leadingIcon = { Icon(Icons.Default.Event, null, tint = ElectricSlateTokens.TextMuted) },
                                onClick = {
                                    showOverflowMenu = false
                                    showDeadlineDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Schedule...", color = ElectricSlateTokens.TextPrimary) },
                                leadingIcon = { Icon(Icons.Default.Alarm, null, tint = ElectricSlateTokens.TextMuted) },
                                onClick = {
                                    showOverflowMenu = false
                                    showScheduleDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Move to Subfolder...", color = ElectricSlateTokens.TextPrimary) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.DriveFileMove, null, tint = ElectricSlateTokens.TextMuted) },
                                onClick = {
                                    showOverflowMenu = false
                                    showMoveGroupDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export Group...", color = ElectricSlateTokens.TextPrimary) },
                                leadingIcon = { Icon(Icons.Default.Share, null, tint = ElectricSlateTokens.TextMuted) },
                                onClick = {
                                    showOverflowMenu = false
                                    showExportGroupDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Ungroup", color = ElectricSlateTokens.TextPrimary) },
                                leadingIcon = { Icon(Icons.Default.LayersClear, null, tint = ElectricSlateTokens.TextMuted) },
                                onClick = {
                                    showOverflowMenu = false
                                    showUngroupConfirmDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Color label", color = ElectricSlateTokens.TextPrimary) },
                                leadingIcon = { Icon(Icons.Default.ColorLens, null, tint = ElectricSlateTokens.TextMuted) },
                                onClick = {
                                    showOverflowMenu = false
                                    showColorDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = ElectricSlateTokens.Destructive) },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = ElectricSlateTokens.Destructive) },
                                onClick = {
                                    showOverflowMenu = false
                                    showDeleteConfirmDialog = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ElectricSlateTokens.BgCanvas)
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ElectricSlateTokens.BgCanvas)
                .padding(innerPadding)
        ) {
            if (uiState.photos.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = ElectricSlateTokens.TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No notes in this group",
                            color = ElectricSlateTokens.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        AppButton(
                            text = "Add Notes to Group",
                            onClick = { showAddOptionsSheet = true },
                            leadingIcon = Icons.Default.Add,
                            variant = AppButtonVariant.PRIMARY
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(photoColumns),
                    state = gridState,
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.photos, key = { it.id }) { photo ->
                        val isTarget = uiState.highlightedPhotoId == photo.id
                        DetailPhotoCard(
                            photo = photo,
                            isBatchMode = false,
                            isSelected = false,
                            isHighlighted = isTarget && highlightAlpha.value > 0f,
                            highlightAlpha = if (isTarget) highlightAlpha.value else 0f,
                            onCardClick = { inspectingPhoto = photo },
                            onCardLongClick = { contextMenuPhoto = photo }
                        )
                    }
                }
            }
        }
    }

    // Modal Sheet for Add Button [+] Options (2.4)
    if (showAddOptionsSheet) {
        GroupAddOptionsSheet(
            groupName = uiState.group?.name ?: "Group",
            onMultiCapture = {
                showAddOptionsSheet = false
                showMultiCapture = true
            },
            onPickGallery = {
                showAddOptionsSheet = false
                galleryPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onAddExistingNotes = {
                showAddOptionsSheet = false
                showAddPhotosDialog = true
            },
            onDismiss = { showAddOptionsSheet = false }
        )
    }

    // Full-Screen Multi-Capture Camera Viewfinder
    if (showMultiCapture && photoStorageManager != null && ocrEngine != null && folderSuggestEngine != null) {
        MultiCaptureScreen(
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

    // Capture Triage Modal (Review before committing into group)
    if (capturedBatchPhotos != null) {
        CaptureReviewSliderModal(
            folderName = uiState.group?.name ?: "Group",
            initialPhotos = capturedBatchPhotos ?: emptyList(),
            subfolders = uiState.subfolders,
            photoStorageManager = photoStorageManager,
            onSaveBatch = { savedList, subfolderId ->
                viewModel.saveCapturedBatch(savedList, subfolderId)
                capturedBatchPhotos = null
            },
            onDismiss = {
                capturedBatchPhotos?.forEach { photo ->
                    coroutineScope.launch {
                        photoStorageManager?.deletePhotoFiles(photo.fileUri, photo.thumbnailUri)
                    }
                }
                capturedBatchPhotos = null
            }
        )
    }

    // Contextual Action Sheet on Member Photo (Long-Press) (2.5)
    contextMenuPhoto?.let { photo ->
        GroupPhotoContextMenuSheet(
            photo = photo,
            onTogglePin = { target ->
                contextMenuPhoto = null
                viewModel.togglePhotoPin(target)
            },
            onSetCover = { target ->
                contextMenuPhoto = null
                viewModel.setCoverPhoto(target.id)
            },
            onInspect = { target ->
                contextMenuPhoto = null
                inspectingPhoto = target
            },
            onRemoveFromGroup = { target ->
                contextMenuPhoto = null
                viewModel.removePhotoFromGroup(target.id)
            },
            onDismiss = { contextMenuPhoto = null }
        )
    }

    // Full-Screen Note Inspector scoped to this group's member photos
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
            onSetAsCover = { p ->
                viewModel.setCoverPhoto(p.id)
            },
            onRemoveFromGroup = { p ->
                inspectingPhoto = null
                viewModel.removePhotoFromGroup(p.id)
            },
            onDismiss = { inspectingPhoto = null }
        )
    }

    // Dialog: Rename Group
    if (showRenameDialog) {
        GroupRenameDialog(
            initialName = uiState.group?.name ?: "",
            onSave = { newName ->
                viewModel.renameGroup(newName)
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false }
        )
    }

    // Dialog: Group-Level Deadline (2.9)
    if (showDeadlineDialog) {
        GroupDeadlineDialog(
            hasExistingDeadline = uiState.group?.linkedDeadline != null,
            onSetDeadline = { deadlineMs ->
                viewModel.setGroupDeadline(deadlineMs)
                showDeadlineDialog = false
            },
            onDismiss = { showDeadlineDialog = false }
        )
    }

    // Dialog: Move Group to Subfolder (2.2)
    if (showMoveGroupDialog) {
        GroupDestinationPickerDialog(
            title = "Move Group \"${uiState.group?.name}\" to...",
            recentDestinations = uiState.recentDestinations.filter { it.type != DestinationType.GROUP },
            availableFolders = uiState.availableFolders,
            currentFolderId = uiState.folder?.id,
            currentSubfolders = uiState.subfolders,
            workspaces = uiState.workspaces,
            onSelectDestination = { targetFolderId, targetSubId ->
                viewModel.moveGroup(targetFolderId, targetSubId, onMoved = onBackClick)
                showMoveGroupDialog = false
            },
            onDismiss = { showMoveGroupDialog = false }
        )
    }

    // Dialog: Export Group (2.10)
    if (showExportGroupDialog) {
        GroupExportDialog(
            groupName = uiState.group?.name ?: "Group",
            onExportPdf = {
                viewModel.exportGroup(context, isZip = false) { file ->
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Group PDF"))
                }
                showExportGroupDialog = false
            },
            onExportZip = {
                viewModel.exportGroup(context, isZip = true) { file ->
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/zip"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Group ZIP"))
                }
                showExportGroupDialog = false
            },
            onDismiss = { showExportGroupDialog = false }
        )
    }

    // Dialog: Schedule Note Group
    if (showScheduleDialog && uiState.group != null) {
        val group = uiState.group!!
        val scheduleManager = remember { NoteScheduleManager(context) }
        ScheduleNoteDialog(
            noteTitle = group.name,
            initialScheduledAt = group.scheduledAt,
            initialAlertType = try {
                ScheduleAlertType.valueOf(group.alertType ?: "NOTIFICATION")
            } catch (_: Exception) {
                ScheduleAlertType.NOTIFICATION
            },
            initialScheduleTitle = group.scheduleTitle,
            onDismiss = { showScheduleDialog = false },
            onSaveSchedule = { scheduledAt, alertType, scheduleTitle ->
                scheduleManager.scheduleNote(
                    noteType = ScheduleNoteType.PHOTO_GROUP,
                    noteId = group.id,
                    folderId = group.folderId,
                    title = group.name,
                    triggerAtMillis = scheduledAt,
                    alertType = alertType,
                    scheduleTitle = scheduleTitle
                )
                viewModel.refresh()
                showScheduleDialog = false
            },
            onClearSchedule = {
                scheduleManager.cancelSchedule(ScheduleNoteType.PHOTO_GROUP, group.id)
                viewModel.refresh()
                showScheduleDialog = false
            }
        )
    }

    // Dialog: Color Label
    if (showColorDialog) {
        GroupColorDialog(
            currentColor = uiState.group?.tagColor,
            onSelectColor = { newColor ->
                viewModel.updateGroupColor(newColor)
                showColorDialog = false
            },
            onDismiss = { showColorDialog = false }
        )
    }

    // Dialog: Delete Group Confirmation
    if (showDeleteConfirmDialog) {
        val count = uiState.photos.size
        val totalSizeKb = (uiState.photos.sumOf { it.fileSizeBytes } + 1023) / 1024
        GroupDeleteConfirmDialog(
            groupName = uiState.group?.name ?: "",
            count = count,
            totalSizeKb = totalSizeKb,
            onConfirm = {
                showDeleteConfirmDialog = false
                viewModel.deleteGroup(onComplete = onBackClick)
            },
            onDismiss = { showDeleteConfirmDialog = false }
        )
    }

    // Dialog: Add Photos to Group
    if (showAddPhotosDialog) {
        GroupAddPhotosDialog(
            groupName = uiState.group?.name ?: "",
            availablePhotos = uiState.availableFolderPhotos,
            gridDensity = uiState.gridDensity,
            onAdd = { ids ->
                viewModel.addPhotosToGroup(ids)
                showAddPhotosDialog = false
            },
            onDismiss = { showAddPhotosDialog = false }
        )
    }

    // Sheet: Ungroup Confirmation (Phase 88)
    if (showUngroupConfirmDialog) {
        AppBottomSheet(
            visible = true,
            onDismiss = { showUngroupConfirmDialog = false },
            title = "Ungroup Notes"
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Ungroup \"${uiState.group?.name ?: "Group"}\"?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricSlateTokens.TextPrimary
                )
                Text(
                    text = "All notes in this group will be converted to standalone loose notes in this folder. The group container, tags, and deadline will be removed.",
                    fontSize = 14.sp,
                    color = ElectricSlateTokens.TextMuted,
                    lineHeight = 20.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppButton(
                        text = "Cancel",
                        onClick = { showUngroupConfirmDialog = false },
                        variant = AppButtonVariant.OUTLINED,
                        modifier = Modifier.weight(1f)
                    )
                    AppButton(
                        text = "Ungroup",
                        onClick = {
                            showUngroupConfirmDialog = false
                            viewModel.ungroup(onComplete = onBackClick)
                        },
                        variant = AppButtonVariant.PRIMARY,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

