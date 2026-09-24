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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Sort
import com.arinara.fotara.data.model.PhotoGroup
import com.arinara.fotara.ui.components.GroupSliderViewerModal
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
import com.arinara.fotara.ui.components.CaptureReviewSliderModal
import com.arinara.fotara.ui.photo.PhotoViewerDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    onOpenGroup: ((folderId: Long, groupId: Long, targetPhotoId: Long?) -> Unit)? = null,
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

    val bottomSheetState = rememberModalBottomSheetState()
    val gridState = rememberLazyGridState()
    val highlightAlpha = remember { Animatable(0f) }

    LaunchedEffect(uiState.highlightedPhotoId, uiState.photos, uiState.gridItems) {
        val targetId = uiState.highlightedPhotoId ?: return@LaunchedEffect
        if (uiState.photos.isEmpty()) return@LaunchedEffect

        val matchedPhoto = uiState.photos.firstOrNull { it.id == targetId }
        if (matchedPhoto == null) {
            snackbarHostState.showSnackbar("Photo is no longer in this folder")
            viewModel.clearHighlightedPhoto()
            return@LaunchedEffect
        }

        // Search match on a photo that is a MEMBER of a group (Addendum 7 edge case):
        // Highlight the group waypoint for ~1s, then auto-navigate to Group screen for the 3s photo highlight.
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
                highlightAlpha.snapTo(0.30f)
                delay(1000L) // Brief ~1s waypoint highlight
                highlightAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                )
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
            highlightAlpha.snapTo(0.30f)
            delay(3000L)
            highlightAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            )
            viewModel.clearHighlightedPhoto()
        } else {
            snackbarHostState.showSnackbar("Photo is no longer in this folder")
            viewModel.clearHighlightedPhoto()
        }
    }

    LaunchedEffect(uiState.highlightedGroupId, uiState.gridItems) {
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
            highlightAlpha.snapTo(0.30f)
            delay(3000L)
            highlightAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            )
            viewModel.clearHighlightedGroup()
        } else {
            snackbarHostState.showSnackbar("Group is no longer in this folder")
            viewModel.clearHighlightedGroup()
        }
    }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
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
        containerColor = MidnightNavy,
        topBar = {
            if (uiState.isSubfolderMultiSelectMode) {
                // Contextual Action Bar for Subfolder Multi-Select
                TopAppBar(
                    title = {
                        Text(
                            text = "${uiState.selectedSubfolderIds.size} Selected",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.exitSubfolderMultiSelect() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Exit Selection",
                                tint = FolderTabCream
                            )
                        }
                    },
                    actions = {
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
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightNavy)
                )
            } else if (uiState.isBatchSelectMode) {
                // Contextual Action Bar for Multi-Select (Rename, Group [2+ photos], Move, Color, Delete)
                TopAppBar(
                    title = {
                        Text(
                            text = "${uiState.totalSelectionCount} Selected",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.exitBatchSelectMode() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Exit Selection",
                                tint = FolderTabCream
                            )
                        }
                    },
                    actions = {
                        // Action 1: Rename (strictly visible when exactly 1 item is selected: branches between photo caption and group name)
                        if (uiState.totalSelectionCount == 1) {
                            if (uiState.selectedPhotoIds.size == 1) {
                                val singlePhotoId = uiState.selectedPhotoIds.first()
                                val singlePhoto = uiState.photos.firstOrNull { it.id == singlePhotoId }
                                IconButton(
                                    onClick = { singlePhoto?.let { photoToRename = it } }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Rename Note",
                                        tint = FolderTabCream
                                    )
                                }
                            } else if (uiState.selectedGroupIds.size == 1) {
                                val singleGroupId = uiState.selectedGroupIds.first()
                                val singleGroup = uiState.groups.firstOrNull { it.id == singleGroupId }
                                IconButton(
                                    onClick = { singleGroup?.let { groupToRename = it } }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Rename Group",
                                        tint = FolderTabCream
                                    )
                                }
                            }
                        }

                        // Action 2: Group (strictly active when 2+ standalone photos are selected and no groups are selected)
                        if (uiState.selectedPhotoIds.size >= 2 && uiState.selectedGroupIds.isEmpty()) {
                            IconButton(onClick = { showCreateGroupDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = "Group Notes",
                                    tint = FolderTabCream
                                )
                            }
                        }

                        // Action 3: Move to Folder
                        IconButton(
                            onClick = { showMovePhotosDialog = true },
                            enabled = uiState.totalSelectionCount > 0
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.DriveFileMove,
                                contentDescription = "Move to Folder",
                                tint = if (uiState.totalSelectionCount > 0) FolderTabCream else TextMuted
                            )
                        }

                        // Action 4: Color Label
                        IconButton(
                            onClick = { showBatchColorDialog = true },
                            enabled = uiState.totalSelectionCount > 0
                        ) {
                            Icon(
                                imageVector = Icons.Default.ColorLens,
                                contentDescription = "Color Label",
                                tint = if (uiState.totalSelectionCount > 0) FolderTabCream else TextMuted
                            )
                        }

                        // Action 5: Delete
                        IconButton(
                            onClick = { showPhotoBulkDeleteConfirm = true },
                            enabled = uiState.totalSelectionCount > 0
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Move to Trash",
                                tint = if (uiState.totalSelectionCount > 0) TagCrimson else TextMuted
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightNavy)
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
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            uiState.folder?.tagColor?.let { tag ->
                                Spacer(modifier = Modifier.width(10.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = tag.composeColor,
                                    modifier = Modifier.size(10.dp)
                                ) {}
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = FolderTabCream
                            )
                        }
                    },
                    actions = {
                        // Right Action 1: Add Photo Button [+]
                        IconButton(
                            onClick = { showAddPhotoSheet = true },
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(FolderBodyBlue)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Photo",
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
                                    contentDescription = "Folder Utilities Menu",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Overflow Dropdown Menu
                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false },
                                modifier = Modifier
                                    .background(MidnightSurface)
                                    .border(1.dp, MidnightCardOutline, RoundedCornerShape(8.dp))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Sort: Newest Uploads", color = TextPrimary) },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, null, tint = FolderTabCream) },
                                    onClick = {
                                        viewModel.setSortOption(PhotoSortOption.UPLOAD_DATE_DESC)
                                        showOverflowMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sort: Nearest Deadline", color = TextPrimary) },
                                    leadingIcon = { Icon(Icons.Default.Event, null, tint = TagAmber) },
                                    onClick = {
                                        viewModel.setSortOption(PhotoSortOption.NEAREST_DEADLINE)
                                        showOverflowMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export Folder to PDF", color = TextPrimary) },
                                    leadingIcon = { Icon(Icons.Default.PictureAsPdf, null, tint = FolderTabCream) },
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
                                            color = TextPrimary
                                        )
                                    },
                                    leadingIcon = { Icon(Icons.Default.SelectAll, null, tint = FolderTabCream) },
                                    onClick = {
                                        viewModel.toggleBatchSelectMode()
                                        showOverflowMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Folder Color Label", color = TextPrimary) },
                                    leadingIcon = { Icon(Icons.Default.ColorLens, null, tint = FolderTabCream) },
                                    onClick = {
                                        showColorDialog = true
                                        showOverflowMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Rename Folder", color = TextPrimary) },
                                    leadingIcon = { Icon(Icons.Default.Edit, null, tint = FolderTabCream) },
                                    onClick = {
                                        showRenameFolderDialog = true
                                        showOverflowMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete Folder", color = TagCrimson) },
                                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = TagCrimson) },
                                    onClick = {
                                        showDeleteConfirmDialog = true
                                        showOverflowMenu = false
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightNavy)
                )
            }
        },

        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) FolderBodyBlue else MidnightSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            if (isSelected) 1.5.dp else 0.8.dp,
                            if (isSelected) FolderBodyBlue else MidnightCardOutline
                        ),
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = !uiState.isSubfolderMultiSelectMode) {
                                viewModel.selectSubfolder(null)
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = "All Notes",
                                color = if (isSelected) Color.White else if (uiState.isSubfolderMultiSelectMode) TextMuted else TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                // Subfolder tabs with unified single long-press context menu & multi-select styling
                items(uiState.subfolders, key = { it.id }) { sub ->
                    val isFilterSelected = uiState.selectedSubfolderId == sub.id
                    val isMultiSelected = uiState.selectedSubfolderIds.contains(sub.id)
                    var showSubMenu by remember { mutableStateOf(false) }

                    Box {
                        if (uiState.isSubfolderMultiSelectMode) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isMultiSelected) FolderBodyBlue else MidnightSurface,
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isMultiSelected) 1.5.dp else 0.8.dp,
                                    if (isMultiSelected) FolderBodyBlue else MidnightCardOutline
                                ),
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
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
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
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
                                        fontSize = 13.sp,
                                        fontWeight = if (isMultiSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isFilterSelected) FolderBodyBlue else MidnightSurface,
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isFilterSelected) 1.5.dp else 0.8.dp,
                                    if (isFilterSelected) FolderBodyBlue else MidnightCardOutline
                                ),
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
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
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = sub.name,
                                        color = if (isFilterSelected) Color.White else TextSecondary,
                                        fontSize = 13.sp,
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
                                .background(MidnightSurface)
                                .border(1.dp, MidnightCardOutline, RoundedCornerShape(8.dp))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Rename", color = TextPrimary) },
                                leadingIcon = { Icon(Icons.Default.Edit, null, tint = FolderTabCream) },
                                onClick = {
                                    showSubMenu = false
                                    subfolderToRename = sub
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = TagCrimson) },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = TagCrimson) },
                                onClick = {
                                    showSubMenu = false
                                    viewModel.getSubfolderDeleteStats(listOf(sub.id)) { stats ->
                                        singleSubfolderToDelete = sub to stats
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Select", color = TextPrimary) },
                                leadingIcon = { Icon(Icons.Default.Check, null, tint = FolderTabCream) },
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
                            shape = RoundedCornerShape(8.dp),
                            color = MidnightSurface,
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, MidnightCardOutline),
                            modifier = Modifier
                                .clickable { viewModel.openAddSubfolderDialog() }
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add subfolder",
                                    tint = FolderTabCream,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Subfolder",
                                    color = FolderTabCream,
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
                            fontSize = 13.sp,
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

                LazyVerticalGrid(
                    columns = GridCells.Fixed(photoColumns),
                    state = gridState,
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.gridItems, key = { it.key }) { gridItem ->
                        when (gridItem) {
                            is FolderGridItem.StandalonePhoto -> {
                                val photo = gridItem.photo
                                val isTarget = uiState.highlightedPhotoId == photo.id
                                DetailPhotoCard(
                                    photo = photo,
                                    isBatchMode = uiState.isBatchSelectMode,
                                    isSelected = uiState.selectedPhotoIds.contains(photo.id),
                                    isHighlighted = isTarget && highlightAlpha.value > 0f,
                                    highlightAlpha = if (isTarget) highlightAlpha.value else 0f,
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
                                    isSelected = uiState.selectedGroupIds.contains(gridItem.group.id),
                                    isHighlighted = isTarget && highlightAlpha.value > 0f,
                                    highlightAlpha = if (isTarget) highlightAlpha.value else 0f,
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
                        }
                    }
                }
            }
        }
    }

    // Modal Action Sheet for Add Photo Button [+]
    if (showAddPhotoSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddPhotoSheet = false },
            sheetState = bottomSheetState,
            containerColor = MidnightSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Add Coursework Note",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Primary Option: Multi-Capture Camera
                Surface(
                    color = FolderBodyBlue.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FolderBodyBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showAddPhotoSheet = false
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
                            Text("Instant rapid capture with auto perspective straightening", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Secondary Option: Import from Gallery
                Surface(
                    color = DockSlatePill.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, MidnightCardOutline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showAddPhotoSheet = false
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
                            Text("Select existing slide screenshots from your device library", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Capture Triage Modal (Popup Slider 75% height / 90% width)
    if (capturedBatchPhotos != null) {
        CaptureReviewSliderModal(
            folderName = uiState.folder?.name ?: "Folder",
            initialPhotos = capturedBatchPhotos ?: emptyList(),
            subfolders = uiState.subfolders,
            onSaveBatch = { savedList, subfolderId ->
                viewModel.saveCapturedBatch(savedList, subfolderId)
                capturedBatchPhotos = null
            },
            onDismiss = { capturedBatchPhotos = null }
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
            com.arinara.fotara.ui.photo.PhotoQuickActionSheet(
                photo = photo,
                subfolders = uiState.subfolders,
                onMoveSubfolder = { subId -> viewModel.movePhotoToSubfolder(photo.id, subId) },
                onChangeTagColor = { colorHex -> viewModel.updatePhotoTagColor(photo.id, colorHex) },
                onSetDeadline = { deadlineMs -> viewModel.setPhotoDeadline(photo.id, deadlineMs) },
                onRenamePhoto = { photoToRename = photo },
                onSelectPhoto = { viewModel.startBatchSelection(photo.id) },
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
        AlertDialog(
            onDismissRequest = { showColorDialog = false },
            containerColor = MidnightSurface,
            shape = RoundedCornerShape(18.dp),
            title = { Text("Choose Folder Color Label", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TagColor.entries.forEach { tag ->
                        Surface(
                            shape = CircleShape,
                            color = tag.composeColor,
                            modifier = Modifier
                                .size(36.dp)
                                .clickable {
                                    viewModel.updateFolderColor(tag.hex)
                                    showColorDialog = false
                                }
                        ) {}
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showColorDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Dialog: Rename Folder
    if (showRenameFolderDialog) {
        var newFolderName by remember { mutableStateOf(uiState.folder?.name ?: "") }
        AlertDialog(
            onDismissRequest = { showRenameFolderDialog = false },
            containerColor = MidnightSurface,
            shape = RoundedCornerShape(18.dp),
            title = { Text("Rename Folder", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = FolderBodyBlue,
                        unfocusedBorderColor = MidnightCardOutline
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renameFolder(newFolderName)
                        showRenameFolderDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FolderBodyBlue)
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameFolderDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Dialog: Rename Subfolder
    if (subfolderToRename != null) {
        val sub = subfolderToRename
        var newSubName by remember(sub) { mutableStateOf(sub?.name ?: "") }
        AlertDialog(
            onDismissRequest = { subfolderToRename = null },
            containerColor = MidnightSurface,
            shape = RoundedCornerShape(18.dp),
            title = { Text("Rename Subfolder", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newSubName,
                    onValueChange = { newSubName = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = FolderBodyBlue,
                        unfocusedBorderColor = MidnightCardOutline
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        sub?.let { viewModel.renameSubfolder(it.id, newSubName) }
                        subfolderToRename = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FolderBodyBlue)
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { subfolderToRename = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Dialog: Delete Folder Confirmation
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = MidnightSurface,
            shape = RoundedCornerShape(18.dp),
            title = { Text("Move Folder to Trash?", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Move \"${uiState.folder?.name}\" and all of its notes to Trash? Items can be restored within 30 days.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteFolder(onDeleted = onBackClick)
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

    // Dialog: Add Subfolder
    if (uiState.showAddSubfolderDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.closeAddSubfolderDialog() },
            containerColor = MidnightSurface,
            shape = RoundedCornerShape(18.dp),
            title = { Text("New Subfolder", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subfolder Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = FolderBodyBlue,
                        unfocusedBorderColor = MidnightCardOutline
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = { if (name.isNotBlank()) viewModel.createSubfolder(name) },
                    colors = ButtonDefaults.buttonColors(containerColor = FolderBodyBlue)
                ) {
                    Text("Create", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeAddSubfolderDialog() }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Dialog: Rename Photo Note (v1.1)
    if (photoToRename != null) {
        val target = photoToRename
        var newCaption by remember(target) { mutableStateOf(target?.caption ?: "") }
        AlertDialog(
            onDismissRequest = { photoToRename = null },
            containerColor = MidnightSurface,
            shape = RoundedCornerShape(18.dp),
            title = { Text("Rename Note", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newCaption,
                    onValueChange = { newCaption = it },
                    placeholder = { Text("Enter note caption...") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = FolderBodyBlue,
                        unfocusedBorderColor = MidnightCardOutline
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        target?.let { viewModel.renamePhoto(it.id, newCaption) }
                        photoToRename = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FolderBodyBlue)
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { photoToRename = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Dialog: Delete Single Subfolder (Trash confirmation)
    singleSubfolderToDelete?.let { (sub, stats) ->
        val sizeKb = (stats.totalSizeBytes + 1023) / 1024
        AlertDialog(
            onDismissRequest = { singleSubfolderToDelete = null },
            containerColor = MidnightSurface,
            shape = RoundedCornerShape(18.dp),
            title = { Text("Move Subfolder to Trash?", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Move \"${sub.name}\" and its ${stats.photoCount} note${if (stats.photoCount != 1) "s" else ""} (${sizeKb} KB) to Trash? Items can be restored within 30 days.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSubfolder(sub.id)
                        singleSubfolderToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TagCrimson)
                ) {
                    Text("Move to Trash", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { singleSubfolderToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Dialog: Subfolder Bulk Delete Confirmation
    if (showSubfolderBulkDeleteConfirm && subfolderDeleteStatsState != null) {
        val stats = subfolderDeleteStatsState!!
        val sizeKb = (stats.totalSizeBytes + 1023) / 1024
        AlertDialog(
            onDismissRequest = {
                showSubfolderBulkDeleteConfirm = false
                subfolderDeleteStatsState = null
            },
            containerColor = MidnightSurface,
            shape = RoundedCornerShape(18.dp),
            title = { Text("Move Subfolders to Trash?", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Move ${stats.subfolderCount} subfolders and their ${stats.photoCount} note${if (stats.photoCount != 1) "s" else ""} (${sizeKb} KB) to Trash? Items can be restored within 30 days.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSelectedSubfolders()
                        showSubfolderBulkDeleteConfirm = false
                        subfolderDeleteStatsState = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TagCrimson)
                ) {
                    Text("Move to Trash", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSubfolderBulkDeleteConfirm = false
                    subfolderDeleteStatsState = null
                }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Dialog: Photo Bulk Delete Confirmation
    if (showPhotoBulkDeleteConfirm) {
        val count = uiState.selectedPhotoIds.size
        AlertDialog(
            onDismissRequest = { showPhotoBulkDeleteConfirm = false },
            containerColor = MidnightSurface,
            shape = RoundedCornerShape(18.dp),
            title = { Text("Move Notes to Trash?", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Move $count selected note${if (count != 1) "s" else ""} to Trash? Items can be restored within 30 days.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSelectedPhotos()
                        showPhotoBulkDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TagCrimson)
                ) {
                    Text("Move to Trash", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPhotoBulkDeleteConfirm = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Dialog: Move Photos to Folder
    if (showMovePhotosDialog) {
        AlertDialog(
            onDismissRequest = { showMovePhotosDialog = false },
            containerColor = MidnightSurface,
            shape = RoundedCornerShape(18.dp),
            title = { Text("Move ${uiState.selectedPhotoIds.size} Notes", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                ) {
                    Text(
                        text = "Select destination folder:",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.availableFolders, key = { it.id }) { targetF ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (targetF.id == uiState.folder?.id) FolderBodyBlue.copy(alpha = 0.2f) else DockSlatePill.copy(alpha = 0.35f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (targetF.id == uiState.folder?.id) FolderBodyBlue else MidnightCardOutline
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.moveSelectedPhotos(targetF.id, null)
                                        showMovePhotosDialog = false
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
                                        text = targetF.name + if (targetF.id == uiState.folder?.id) " (Current)" else "",
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMovePhotosDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Dialog: Batch Tag Color Chooser
    if (showBatchColorDialog) {
        AlertDialog(
            onDismissRequest = { showBatchColorDialog = false },
            containerColor = MidnightSurface,
            shape = RoundedCornerShape(18.dp),
            title = { Text("Assign Color Label", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TagColor.entries.forEach { tag ->
                            Surface(
                                shape = CircleShape,
                                color = tag.composeColor,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable {
                                        viewModel.updateSelectedPhotosTagColor(tag.hex)
                                        showBatchColorDialog = false
                                    }
                            ) {}
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            viewModel.updateSelectedPhotosTagColor(null)
                            showBatchColorDialog = false
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Remove Color Label", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showBatchColorDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Modal Action Sheet for Long-Pressed Group
    groupActionTarget?.let { targetGroup ->
        val g = targetGroup.group
        val memberCount = targetGroup.memberPhotos.size
        val totalSizeKb = (targetGroup.memberPhotos.sumOf { it.fileSizeBytes } / 1024L).coerceAtLeast(1L)

        ModalBottomSheet(
            onDismissRequest = { groupActionTarget = null },
            containerColor = MidnightSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = FolderTabCream,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = g.name,
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$memberCount notes · $totalSizeKb KB",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Action 1: Rename
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            val toRename = g
                            groupActionTarget = null
                            groupToRename = toRename
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = FolderTabCream)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = "Rename", color = TextPrimary, fontSize = 15.sp)
                }

                // Action 2: Ungroup (Non-destructive dissolution, no confirmation dialog)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            viewModel.ungroup(g.id)
                            groupActionTarget = null
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.LayersClear, contentDescription = null, tint = FolderTabCream)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(text = "Ungroup", color = TextPrimary, fontSize = 15.sp)
                        Text(text = "Dissolve group back to individual notes", color = TextMuted, fontSize = 12.sp)
                    }
                }

                // Action 3: Add photos
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            groupForAddingPhotos = targetGroup
                            groupActionTarget = null
                            showAddPhotosToGroupDialog = true
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, tint = FolderTabCream)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = "Add photos", color = TextPrimary, fontSize = 15.sp)
                }

                // Action 4: Color label
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            val toColor = g
                            groupActionTarget = null
                            groupToColor = toColor
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.ColorLens, contentDescription = null, tint = FolderTabCream)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = "Color label", color = TextPrimary, fontSize = 15.sp)
                }

                // Action 5: Select (enters multi-select mode)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            viewModel.startBatchSelectionWithGroup(g.id)
                            groupActionTarget = null
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = FolderTabCream)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = "Select", color = TextPrimary, fontSize = 15.sp)
                }

                // Action 6: Delete (moves group and all members to Trash with confirmation)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            val toDelete = targetGroup
                            groupActionTarget = null
                            groupToDelete = toDelete
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = TagCrimson)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(text = "Delete", color = TagCrimson, fontSize = 15.sp)
                        Text(text = "Move group and its notes to Trash", color = TextMuted, fontSize = 12.sp)
                    }
                }
            }
        }
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
}

@Composable
internal fun DetailPhotoCard(
    photo: Photo,
    isBatchMode: Boolean,
    isSelected: Boolean,
    isHighlighted: Boolean = false,
    highlightAlpha: Float = 0f,
    onCardClick: () -> Unit,
    onCardLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dateStr = remember(photo.addedAt) {
        SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(photo.addedAt))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightSurface),
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) FolderBodyBlue else MidnightCardOutline
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
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
                        .background(Color(0xFF0F173A)),
                    contentAlignment = Alignment.Center
                ) {
                    val imageModel = photo.thumbnailUri ?: photo.fileUri
                    if (imageModel.isNotBlank()) {
                        AsyncImage(
                            model = if (imageModel.startsWith("content://") || imageModel.startsWith("file://")) {
                                imageModel
                            } else {
                                File(imageModel)
                            },
                            contentDescription = photo.caption ?: "Photo note",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    if (isBatchMode) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isSelected) FolderBodyBlue else TextSecondary,
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
                        color = TextPrimary,
                        fontSize = 13.sp,
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
                            color = TextMuted,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        if (photo.linkedDeadline != null) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = "Deadline",
                                tint = TagAmber,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            // 3-second non-blocking translucent warm accent overlay
            if (isHighlighted && highlightAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFFFE082).copy(alpha = highlightAlpha))
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
    onCardClick: () -> Unit,
    onCardLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val group = groupItem.group
    val memberCount = groupItem.memberPhotos.size
    val coverPhoto = groupItem.coverPhoto
    val dateStr = remember(group.createdAt) {
        SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(group.createdAt))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightSurface),
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 2.dp else 1.2.dp,
            if (isSelected) FolderBodyBlue else MidnightCardOutline
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
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
                        .background(Color(0xFF0F173A)),
                    contentAlignment = Alignment.Center
                ) {
                    val imageModel = coverPhoto?.thumbnailUri ?: coverPhoto?.fileUri
                    if (!imageModel.isNullOrBlank()) {
                        AsyncImage(
                            model = if (imageModel.startsWith("content://") || imageModel.startsWith("file://")) {
                                imageModel
                            } else {
                                File(imageModel)
                            },
                            contentDescription = group.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Stacked-photos Badge in Top-Left (Icon + Count, e.g. Layers + "5")
                    Surface(
                        color = MidnightNavy.copy(alpha = 0.88f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, MidnightCardOutline),
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
                                tint = FolderTabCream,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$memberCount",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Multi-select Checkmark Badge
                    if (isBatchMode) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isSelected) FolderBodyBlue else TextSecondary,
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
                            tint = FolderTabCream,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = group.name,
                            color = TextPrimary,
                            fontSize = 13.sp,
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
                            color = TextMuted,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        if (groupItem.sortDeadline != null) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = "Earliest Deadline",
                                tint = TagAmber,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            // 3-second non-blocking translucent warm accent overlay
            if (isHighlighted && highlightAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFFFE082).copy(alpha = highlightAlpha))
                )
            }
        }
    }
}

@Composable
private fun CreateGroupDialog(
    selectedCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidnightSurface,
        shape = RoundedCornerShape(18.dp),
        title = {
            Text(
                text = "Create Group ($selectedCount notes)",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Enter a name for this group (e.g. Latihan 1.5).",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    placeholder = { Text("Group name", color = TextMuted) },
                    singleLine = true,
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = FolderBodyBlue,
                        unfocusedBorderColor = MidnightCardOutline,
                        cursorColor = FolderBodyBlue
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (groupName.trim().isNotBlank()) {
                        onConfirm(groupName.trim())
                    }
                },
                enabled = groupName.trim().isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = FolderBodyBlue)
            ) {
                Text("Create", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = FolderTabCream)
            }
        }
    )
}

@Composable
private fun RenameGroupDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var groupName by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidnightSurface,
        shape = RoundedCornerShape(18.dp),
        title = {
            Text(
                text = "Rename Group",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                androidx.compose.material3.OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    placeholder = { Text("Group name", color = TextMuted) },
                    singleLine = true,
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = FolderBodyBlue,
                        unfocusedBorderColor = MidnightCardOutline,
                        cursorColor = FolderBodyBlue
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (groupName.trim().isNotBlank()) {
                        onConfirm(groupName.trim())
                    }
                },
                enabled = groupName.trim().isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = FolderBodyBlue)
            ) {
                Text("Save", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = FolderTabCream)
            }
        }
    )
}

@Composable
private fun GroupDeleteConfirmDialog(
    group: PhotoGroup,
    memberCount: Int,
    totalSizeBytes: Long,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val sizeKb = (totalSizeBytes / 1024L).coerceAtLeast(1L)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidnightSurface,
        shape = RoundedCornerShape(18.dp),
        title = {
            Text(
                text = "Move Group to Trash?",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Move '${group.name}' and all its $memberCount notes ($sizeKb KB) to Trash? Items can be restored within 30 days.",
                color = TextSecondary,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = TagCrimson)
            ) {
                Text("Move to Trash", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = FolderTabCream)
            }
        }
    )
}

@Composable
private fun AddPhotosToGroupDialog(
    groupName: String,
    availablePhotos: List<Photo>,
    gridDensity: Int = 3,
    onDismiss: () -> Unit,
    onAdd: (List<Long>) -> Unit
) {
    val selectedIds = remember { mutableStateListOf<Long>() }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidnightSurface,
        shape = RoundedCornerShape(18.dp),
        title = {
            Text(
                text = "Add Notes to '$groupName'",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (availablePhotos.isEmpty()) {
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
                        columns = GridCells.Fixed(gridDensity.coerceIn(2, 4)),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(availablePhotos, key = { it.id }) { photo ->
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
                                val model = photo.thumbnailUri ?: photo.fileUri
                                AsyncImage(
                                    model = if (model.startsWith("content://") || model.startsWith("file://")) model else File(model),
                                    contentDescription = photo.caption,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (isChecked) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = FolderBodyBlue,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(selectedIds.toList()) },
                enabled = selectedIds.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = FolderBodyBlue)
            ) {
                Text("Add (${selectedIds.size})", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = FolderTabCream)
            }
        }
    )
}

@Composable
private fun GroupColorDialog(
    onDismiss: () -> Unit,
    onSelectColor: (String?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidnightSurface,
        shape = RoundedCornerShape(18.dp),
        title = { Text("Assign Group Color Label", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TagColor.entries.forEach { tag ->
                        Surface(
                            shape = CircleShape,
                            color = tag.composeColor,
                            modifier = Modifier
                                .size(36.dp)
                                .clickable {
                                    onSelectColor(tag.hex)
                                }
                        ) {}
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { onSelectColor(null) },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Remove Color Label", color = TextSecondary, fontSize = 13.sp)
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
