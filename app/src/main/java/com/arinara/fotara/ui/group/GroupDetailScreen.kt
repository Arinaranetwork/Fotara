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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arinara.fotara.data.model.DestinationType
import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.model.RecentDestination
import com.arinara.fotara.data.model.Subfolder
import com.arinara.fotara.data.model.TagColor
import com.arinara.fotara.data.storage.PhotoStorageManager
import com.arinara.fotara.ocr.FolderSuggestEngine
import com.arinara.fotara.ocr.OcrEngine
import com.arinara.fotara.theme.DockSlatePill
import com.arinara.fotara.theme.FolderBodyBlue
import com.arinara.fotara.theme.FolderTabCream
import com.arinara.fotara.theme.MidnightCardOutline
import com.arinara.fotara.theme.MidnightNavy
import com.arinara.fotara.theme.MidnightSurface
import com.arinara.fotara.theme.TagAmber
import com.arinara.fotara.theme.TagCrimson
import com.arinara.fotara.theme.TextMuted
import com.arinara.fotara.theme.TextPrimary
import com.arinara.fotara.theme.TextSecondary
import com.arinara.fotara.ui.capture.MultiCaptureScreen
import com.arinara.fotara.ui.folder.DetailPhotoCard
import com.arinara.fotara.ui.photo.PhotoViewerDialog
import androidx.compose.material.icons.filled.Alarm
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

    val bottomSheetState = rememberModalBottomSheetState()
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
            highlightAlpha.snapTo(0.30f)
            delay(3000L)
            highlightAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            )
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
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
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
                            tint = FolderTabCream
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
                            .background(FolderBodyBlue)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Notes to Group",
                            tint = FolderTabCream,
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
                                .background(DockSlatePill.copy(alpha = 0.40f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Group Menu",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false },
                            modifier = Modifier
                                .background(MidnightSurface)
                                .border(1.dp, MidnightCardOutline, RoundedCornerShape(8.dp))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Rename", color = TextPrimary) },
                                leadingIcon = { Icon(Icons.Default.Edit, null, tint = FolderTabCream) },
                                onClick = {
                                    showOverflowMenu = false
                                    showRenameDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (uiState.group?.linkedDeadline != null) "Edit Deadline" else "Set Deadline",
                                        color = TextPrimary
                                    )
                                },
                                leadingIcon = { Icon(Icons.Default.Event, null, tint = TagAmber) },
                                onClick = {
                                    showOverflowMenu = false
                                    showDeadlineDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Schedule...", color = TextPrimary) },
                                leadingIcon = { Icon(Icons.Default.Alarm, null, tint = FolderTabCream) },
                                onClick = {
                                    showOverflowMenu = false
                                    showScheduleDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Move to Subfolder...", color = TextPrimary) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.DriveFileMove, null, tint = FolderTabCream) },
                                onClick = {
                                    showOverflowMenu = false
                                    showMoveGroupDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export Group...", color = TextPrimary) },
                                leadingIcon = { Icon(Icons.Default.Share, null, tint = FolderTabCream) },
                                onClick = {
                                    showOverflowMenu = false
                                    showExportGroupDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Ungroup", color = TextPrimary) },
                                leadingIcon = { Icon(Icons.Default.LayersClear, null, tint = FolderTabCream) },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.ungroup(onComplete = onBackClick)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Color label", color = TextPrimary) },
                                leadingIcon = { Icon(Icons.Default.ColorLens, null, tint = FolderTabCream) },
                                onClick = {
                                    showOverflowMenu = false
                                    showColorDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = TagCrimson) },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = TagCrimson) },
                                onClick = {
                                    showOverflowMenu = false
                                    showDeleteConfirmDialog = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightNavy)
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MidnightNavy)
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
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No notes in this group",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(photoColumns),
                    state = gridState,
                    contentPadding = PaddingValues(16.dp),
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
        ModalBottomSheet(
            onDismissRequest = { showAddOptionsSheet = false },
            sheetState = bottomSheetState,
            containerColor = MidnightSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Add to \"${uiState.group?.name ?: "Group"}\"",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Option 1: Multi-Capture Camera
                Surface(
                    color = FolderBodyBlue.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FolderBodyBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showAddOptionsSheet = false
                            showMultiCapture = true
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(FolderBodyBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CameraAlt, null, tint = FolderTabCream, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("Multi-Capture Camera", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("Straighten & capture directly into this group", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Option 2: Import from Gallery
                Surface(
                    color = DockSlatePill.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, MidnightCardOutline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showAddOptionsSheet = false
                            galleryPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(DockSlatePill),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Collections, null, tint = FolderTabCream, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("Import from Gallery", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("Select images from library to add directly", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Option 3: Add Existing Notes from Folder
                Surface(
                    color = DockSlatePill.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, MidnightCardOutline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showAddOptionsSheet = false
                            showAddPhotosDialog = true
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(DockSlatePill),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Layers, null, tint = FolderTabCream, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("Add Existing Notes", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("Pick standalone notes in this folder", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
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
            folderSuggestEngine = folderSuggestEngine,
            onSaveBatch = { savedList, subfolderId ->
                viewModel.saveCapturedBatch(savedList, subfolderId)
                capturedBatchPhotos = null
            },
            onDismiss = { capturedBatchPhotos = null }
        )
    }

    // Contextual Action Sheet on Member Photo (Long-Press) (2.5)
    contextMenuPhoto?.let { photo ->
        ModalBottomSheet(
            onDismissRequest = { contextMenuPhoto = null },
            containerColor = MidnightSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = photo.caption ?: "Note Actions",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Action 1: Set as Cover Photo (2.5)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            viewModel.setCoverPhoto(photo.id)
                            contextMenuPhoto = null
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = FolderTabCream)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = "Set as cover photo", color = TextPrimary, fontSize = 15.sp)
                }

                // Action 2: Inspect Note
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            val target = photo
                            contextMenuPhoto = null
                            inspectingPhoto = target
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Collections, contentDescription = null, tint = FolderTabCream)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = "Inspect note", color = TextPrimary, fontSize = 15.sp)
                }

                // Action 3: Remove from Group
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            viewModel.removePhotoFromGroup(photo.id, onAutoDissolved = {
                                contextMenuPhoto = null
                                onBackClick()
                            })
                            contextMenuPhoto = null
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.LayersClear, contentDescription = null, tint = TagCrimson)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = "Remove from group", color = TagCrimson, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
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
                viewModel.removePhotoFromGroup(p.id, onAutoDissolved = {
                    inspectingPhoto = null
                    onBackClick()
                })
            },
            onDismiss = { inspectingPhoto = null }
        )
    }

    // Dialog: Rename Group
    if (showRenameDialog) {
        val currentName = uiState.group?.name ?: ""
        var newName by remember { mutableStateOf(currentName) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            containerColor = MidnightSurface,
            shape = RoundedCornerShape(18.dp),
            title = { Text("Rename Group", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = FolderBodyBlue,
                        unfocusedBorderColor = MidnightCardOutline,
                        cursorColor = FolderBodyBlue
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.renameGroup(newName)
                            showRenameDialog = false
                        }
                    },
                    enabled = newName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = FolderBodyBlue)
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = FolderTabCream)
                }
            }
        )
    }

    // Dialog: Group-Level Deadline (2.9)
    if (showDeadlineDialog) {
        val now = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L
        AlertDialog(
            onDismissRequest = { showDeadlineDialog = false },
            containerColor = MidnightSurface,
            shape = RoundedCornerShape(18.dp),
            title = { Text("Set Group Deadline", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Reminders trigger 24 hours prior:", color = TextSecondary, fontSize = 13.sp)

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DockSlatePill.copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, MidnightCardOutline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setGroupDeadline(now + oneDayMs)
                                showDeadlineDialog = false
                            }
                    ) {
                        Text("Due Tomorrow (Urgent)", color = TextPrimary, fontSize = 14.sp, modifier = Modifier.padding(14.dp))
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DockSlatePill.copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, MidnightCardOutline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setGroupDeadline(now + 3 * oneDayMs)
                                showDeadlineDialog = false
                            }
                    ) {
                        Text("Due in 3 Days", color = TextPrimary, fontSize = 14.sp, modifier = Modifier.padding(14.dp))
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DockSlatePill.copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, MidnightCardOutline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setGroupDeadline(now + 7 * oneDayMs)
                                showDeadlineDialog = false
                            }
                    ) {
                        Text("Due Next Week", color = TextPrimary, fontSize = 14.sp, modifier = Modifier.padding(14.dp))
                    }

                    if (uiState.group?.linkedDeadline != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = TagCrimson.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, TagCrimson.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setGroupDeadline(null)
                                    showDeadlineDialog = false
                                }
                        ) {
                            Text("Remove Deadline", color = TagCrimson, fontSize = 14.sp, modifier = Modifier.padding(14.dp))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDeadlineDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Dialog: Move Group to Subfolder (2.2)
    if (showMoveGroupDialog) {
        DestinationPickerDialog(
            title = "Move Group \"${uiState.group?.name}\" to...",
            recentDestinations = uiState.recentDestinations.filter { it.type != DestinationType.GROUP },
            availableFolders = uiState.availableFolders,
            currentFolderId = uiState.folder?.id,
            currentSubfolders = uiState.subfolders,
            onSelectDestination = { targetFolderId, targetSubId ->
                viewModel.moveGroup(targetFolderId, targetSubId, onMoved = onBackClick)
                showMoveGroupDialog = false
            },
            onDismiss = { showMoveGroupDialog = false }
        )
    }

    // Dialog: Export Group (2.10)
    if (showExportGroupDialog) {
        AlertDialog(
            onDismissRequest = { showExportGroupDialog = false },
            containerColor = MidnightSurface,
            shape = RoundedCornerShape(18.dp),
            title = { Text("Export \"${uiState.group?.name ?: "Group"}\"", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Choose an export format:", color = TextSecondary, fontSize = 13.sp)

                    // PDF Option
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DockSlatePill.copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, MidnightCardOutline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
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
                            }
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PictureAsPdf, null, tint = FolderTabCream, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("PDF Document (.pdf)", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Multi-page coursework notes with group header", color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }

                    // ZIP Option
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DockSlatePill.copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, MidnightCardOutline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
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
                            }
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Folder, null, tint = FolderTabCream, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("ZIP Archive (.zip)", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Original high-resolution photos", color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showExportGroupDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
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
        AlertDialog(
            onDismissRequest = { showColorDialog = false },
            containerColor = MidnightSurface,
            shape = RoundedCornerShape(18.dp),
            title = { Text("Group Color Label", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    TagColor.entries.forEach { tag ->
                        val isSelected = uiState.group?.tagColor == tag.hex
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(tag.composeColor)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    val newColor = if (isSelected) null else tag.hex
                                    viewModel.updateGroupColor(newColor)
                                    showColorDialog = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showColorDialog = false }) {
                    Text("Close", color = FolderTabCream)
                }
            }
        )
    }

    // Dialog: Delete Group Confirmation
    if (showDeleteConfirmDialog) {
        val count = uiState.photos.size
        val totalSizeKb = (uiState.photos.sumOf { it.fileSizeBytes } + 1023) / 1024
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = MidnightSurface,
            shape = RoundedCornerShape(18.dp),
            title = { Text("Move Group to Trash?", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Move \"${uiState.group?.name}\" and its $count notes ($totalSizeKb KB) to Trash? Items can be restored within 30 days.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteGroup(onComplete = onBackClick)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TagCrimson)
                ) {
                    Text("Move to Trash", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = FolderTabCream)
                }
            }
        )
    }

    // Dialog: Add Photos to Group
    if (showAddPhotosDialog) {
        val selectedIds = remember { mutableStateListOf<Long>() }
        AlertDialog(
            onDismissRequest = { showAddPhotosDialog = false },
            containerColor = MidnightSurface,
            shape = RoundedCornerShape(18.dp),
            title = {
                Text(
                    text = "Add Notes to \"${uiState.group?.name}\"",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                if (uiState.availableFolderPhotos.isEmpty()) {
                    Text(
                        text = "No standalone notes available in this folder to add.",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                    ) {
                        Text(
                            text = "Select notes to fold into this group:",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(uiState.gridDensity.coerceIn(2, 4)),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(uiState.availableFolderPhotos, key = { it.id }) { photo ->
                                val isChecked = selectedIds.contains(photo.id)
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            if (isChecked) 2.dp else 1.dp,
                                            if (isChecked) FolderBodyBlue else MidnightCardOutline,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            if (isChecked) selectedIds.remove(photo.id)
                                            else selectedIds.add(photo.id)
                                        }
                                ) {
                                    DetailPhotoCard(
                                        photo = photo,
                                        isBatchMode = true,
                                        isSelected = isChecked,
                                        onCardClick = {
                                            if (isChecked) selectedIds.remove(photo.id)
                                            else selectedIds.add(photo.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (uiState.availableFolderPhotos.isNotEmpty()) {
                    Button(
                        onClick = {
                            viewModel.addPhotosToGroup(selectedIds.toList())
                            showAddPhotosDialog = false
                        },
                        enabled = selectedIds.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = FolderBodyBlue)
                    ) {
                        Text("Add (${selectedIds.size})", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPhotosDialog = false }) {
                    Text("Cancel", color = FolderTabCream)
                }
            }
        )
    }
}

@Composable
private fun DestinationPickerDialog(
    title: String,
    recentDestinations: List<RecentDestination>,
    availableFolders: List<Folder>,
    currentFolderId: Long?,
    currentSubfolders: List<Subfolder>,
    onSelectDestination: (folderId: Long, subfolderId: Long?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidnightSurface,
        shape = RoundedCornerShape(18.dp),
        title = { Text(title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (recentDestinations.isNotEmpty()) {
                        item {
                            Text(
                                text = "Recently Used",
                                color = FolderTabCream,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                            )
                        }
                        items(recentDestinations, key = { "recent_${it.type}_${it.folderId}_${it.subfolderId}_${it.groupId}" }) { recent ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = DockSlatePill.copy(alpha = 0.5f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, FolderTabCream.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val subId = if (recent.type == DestinationType.SUBFOLDER) recent.subfolderId else null
                                        onSelectDestination(recent.folderId, subId)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (recent.type == DestinationType.SUBFOLDER) Icons.AutoMirrored.Filled.DriveFileMove else Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = FolderTabCream,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(recent.title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                        item {
                            Text(
                                text = "All Folders",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                    }

                    items(availableFolders, key = { it.id }) { targetF ->
                        val isCurrent = targetF.id == currentFolderId
                        Column {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isCurrent) FolderBodyBlue.copy(alpha = 0.2f) else DockSlatePill.copy(alpha = 0.35f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isCurrent) FolderBodyBlue else MidnightCardOutline
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectDestination(targetF.id, null)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = targetF.tagColor.composeColor,
                                        modifier = Modifier.size(10.dp)
                                    ) {}
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = targetF.name + if (isCurrent) " (Current Folder)" else "",
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            if (isCurrent && currentSubfolders.isNotEmpty()) {
                                currentSubfolders.forEach { sub ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = DockSlatePill.copy(alpha = 0.2f),
                                        border = androidx.compose.foundation.BorderStroke(0.6.dp, MidnightCardOutline),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 24.dp, top = 4.dp)
                                            .clickable {
                                                onSelectDestination(targetF.id, sub.id)
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.DriveFileMove,
                                                contentDescription = null,
                                                tint = FolderTabCream,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = sub.name,
                                                color = TextPrimary,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
