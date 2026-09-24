// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.group

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.model.TagColor
import com.arinara.fotara.theme.DockSlatePill
import com.arinara.fotara.theme.FolderBodyBlue
import com.arinara.fotara.theme.FolderTabCream
import com.arinara.fotara.theme.MidnightCardOutline
import com.arinara.fotara.theme.MidnightNavy
import com.arinara.fotara.theme.MidnightSurface
import com.arinara.fotara.theme.TagCrimson
import com.arinara.fotara.theme.TextMuted
import com.arinara.fotara.theme.TextPrimary
import com.arinara.fotara.theme.TextSecondary
import com.arinara.fotara.ui.folder.DetailPhotoCard
import com.arinara.fotara.ui.photo.PhotoViewerDialog
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    viewModel: GroupDetailViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showOverflowMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showAddPhotosDialog by remember { mutableStateOf(false) }

    var inspectingPhoto by remember { mutableStateOf<Photo?>(null) }

    val gridState = rememberLazyGridState()
    val highlightAlpha = remember { Animatable(0f) }

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
                    // Right Action 1: Add Photos Button [+]
                    IconButton(
                        onClick = { showAddPhotosDialog = true },
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(FolderBodyBlue)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Photos to Group",
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
                            onCardLongClick = { inspectingPhoto = photo }
                        )
                    }
                }
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
