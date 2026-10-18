// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.trash

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.ui.home.workspace.RestoreWorkspaceDestinationDialog
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.theme.FolderBodyBlue
import com.arinara.fotara.theme.FolderTabCream
import com.arinara.fotara.theme.MidnightCardOutline
import com.arinara.fotara.theme.MidnightNavy
import com.arinara.fotara.theme.MidnightSurface
import com.arinara.fotara.theme.TagCrimson
import com.arinara.fotara.theme.TextMuted
import com.arinara.fotara.theme.TextPrimary
import com.arinara.fotara.theme.TextSecondary
import com.arinara.fotara.ui.components.presets.AppButton
import com.arinara.fotara.ui.components.presets.AppButtonVariant
import com.arinara.fotara.ui.components.presets.AppCard
import com.arinara.fotara.ui.components.presets.AppScreen
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    viewModel: TrashViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    val totalTrashedCount = uiState.trashedFolders.size + uiState.trashedPhotos.size

    AppScreen(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MidnightNavy,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Trash",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.SemiBold
                    )
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
                    if (totalTrashedCount > 0) {
                        AppButton(
                            text = "Empty Trash",
                            onClick = { viewModel.requestEmptyTrash() },
                            leadingIcon = Icons.Default.DeleteForever,
                            variant = AppButtonVariant.DESTRUCTIVE,
                            height = 36.dp,
                            cornerRadius = 12.dp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightSurface)
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Retention Information Banner
            Surface(
                color = MidnightSurface.copy(alpha = 0.70f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Items in Trash are automatically purged after 30 days.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // Tab Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedTab == TrashTab.ALL,
                    onClick = { viewModel.selectTab(TrashTab.ALL) },
                    label = { Text("All ($totalTrashedCount)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FolderBodyBlue,
                        selectedLabelColor = Color.White,
                        containerColor = MidnightSurface,
                        labelColor = TextSecondary
                    )
                )

                FilterChip(
                    selected = uiState.selectedTab == TrashTab.FOLDERS,
                    onClick = { viewModel.selectTab(TrashTab.FOLDERS) },
                    label = { Text("Folders (${uiState.trashedFolders.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FolderBodyBlue,
                        selectedLabelColor = Color.White,
                        containerColor = MidnightSurface,
                        labelColor = TextSecondary
                    )
                )

                FilterChip(
                    selected = uiState.selectedTab == TrashTab.PHOTOS,
                    onClick = { viewModel.selectTab(TrashTab.PHOTOS) },
                    label = { Text("Notes (${uiState.trashedPhotos.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FolderBodyBlue,
                        selectedLabelColor = Color.White,
                        containerColor = MidnightSurface,
                        labelColor = TextSecondary
                    )
                )
            }

            // Main List Content
            val showFolders = uiState.selectedTab == TrashTab.ALL || uiState.selectedTab == TrashTab.FOLDERS
            val showPhotos = uiState.selectedTab == TrashTab.ALL || uiState.selectedTab == TrashTab.PHOTOS
            val displayedFolders = if (showFolders) uiState.trashedFolders else emptyList()
            val displayedPhotos = if (showPhotos) uiState.trashedPhotos else emptyList()

            if (displayedFolders.isEmpty() && displayedPhotos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Trash is empty",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Deleted folders and study notes will appear here before being permanently purged.",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (displayedFolders.isNotEmpty()) {
                        if (uiState.selectedTab == TrashTab.ALL) {
                            item {
                                Text(
                                    text = "Folders",
                                    color = TextSecondary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }
                        items(displayedFolders, key = { "folder_${it.id}" }) { folder ->
                            TrashedFolderCard(
                                folder = folder,
                                onRestore = { viewModel.requestRestoreFolder(folder) },
                                onDeletePermanently = { viewModel.requestDeleteFolderPermanently(folder) }
                            )
                        }
                    }

                    if (displayedPhotos.isNotEmpty()) {
                        if (uiState.selectedTab == TrashTab.ALL && displayedFolders.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Notes & Photos",
                                    color = TextSecondary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }
                        items(displayedPhotos, key = { "photo_${it.id}" }) { photo ->
                            TrashedPhotoCard(
                                photo = photo,
                                onRestore = { viewModel.requestRestorePhoto(photo) },
                                onDeletePermanently = { viewModel.requestDeletePhotoPermanently(photo) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog: Empty Trash Confirmation
    if (uiState.showEmptyTrashDialog) {
        EmptyTrashConfirmationDialog(
            onDismiss = { viewModel.dismissEmptyTrashDialog() },
            onConfirm = { viewModel.confirmEmptyTrash() }
        )
    }

    // Dialog: Delete Photo Permanently Confirmation
    uiState.photoToDeletePermanently?.let { photo ->
        DeletePhotoPermanentlyDialog(
            photo = photo,
            onDismiss = { viewModel.dismissDeletePhotoPermanentlyDialog() },
            onConfirm = { viewModel.confirmDeletePhotoPermanently() }
        )
    }

    // Dialog: Delete Folder Permanently Confirmation
    uiState.folderToDeletePermanently?.let { folder ->
        DeleteFolderPermanentlyDialog(
            folder = folder,
            onDismiss = { viewModel.dismissDeleteFolderPermanentlyDialog() },
            onConfirm = { viewModel.confirmDeleteFolderPermanently() }
        )
    }

    // Dialog: Orphan Parent Resolution
    uiState.orphanPhotoToRestore?.let { photo ->
        OrphanPhotoRestoreDialog(
            photo = photo,
            parentFolder = uiState.parentFolderForOrphan,
            activeFolders = uiState.activeFolders,
            workspaces = uiState.workspaces,
            onDismiss = { viewModel.dismissOrphanDialog() },
            onRestoreBoth = { viewModel.requestRestoreBoth(photo, photo.folderId) },
            onRestoreToActiveFolder = { targetId ->
                viewModel.confirmRestorePhotoToActiveFolder(photo, targetId)
            }
        )
    }

    uiState.folderToRestoreDestination?.let { folder ->
        RestoreWorkspaceDestinationDialog(
            workspaces = uiState.workspaces,
            initialWorkspaceId = folder.workspaceId,
            onDismiss = { viewModel.dismissRestoreFolderDestinationDialog() },
            onConfirmRestore = { targetWsId ->
                viewModel.confirmRestoreFolder(folder, targetWsId)
            }
        )
    }

    uiState.orphanToRestoreDestination?.let { (photo, parentFolderId) ->
        val initialWsId = uiState.parentFolderForOrphan?.workspaceId ?: 1L
        RestoreWorkspaceDestinationDialog(
            workspaces = uiState.workspaces,
            initialWorkspaceId = initialWsId,
            onDismiss = { viewModel.dismissOrphanRestoreDestinationDialog() },
            onConfirmRestore = { targetWsId ->
                viewModel.confirmRestoreBoth(photo, parentFolderId, targetWsId)
            }
        )
    }
}

@Composable
private fun TrashedFolderCard(
    folder: Folder,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit,
    modifier: Modifier = Modifier
) {
    val daysAgo = formatDaysAgo(folder.deletedAt)
    val remaining = daysRemaining(folder.deletedAt)

    AppCard(
        cornerRadius = 14.dp,
        containerColor = MidnightSurface,
        borderColor = MidnightCardOutline,
        contentPadding = 0.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(folder.tagColor.composeColor.copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = folder.tagColor.composeColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${folder.photoCount} notes · Deleted $daysAgo · Purges in $remaining d",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onRestore) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Restore Folder",
                        tint = FolderTabCream,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = onDeletePermanently) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = "Delete Permanently",
                        tint = TagCrimson,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TrashedPhotoCard(
    photo: Photo,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit,
    modifier: Modifier = Modifier
) {
    val daysAgo = formatDaysAgo(photo.deletedAt)
    val remaining = daysRemaining(photo.deletedAt)

    AppCard(
        cornerRadius = 14.dp,
        containerColor = MidnightSurface,
        borderColor = MidnightCardOutline,
        contentPadding = 0.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ElectricSlateTokens.SurfaceElevated),
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
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = photo.caption ?: "Untitled Note",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Deleted $daysAgo · Purges in $remaining d",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onRestore) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Restore Note",
                        tint = FolderTabCream,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = onDeletePermanently) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = "Delete Permanently",
                        tint = TagCrimson,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private fun formatDaysAgo(deletedAt: Long?): String {
    if (deletedAt == null) return "recently"
    val diffMs = System.currentTimeMillis() - deletedAt
    val days = (diffMs / (24 * 60 * 60 * 1000L)).toInt()
    return when {
        days <= 0 -> "today"
        days == 1 -> "yesterday"
        else -> "$days d ago"
    }
}

private fun daysRemaining(deletedAt: Long?, retentionDays: Int = 30): Int {
    if (deletedAt == null) return retentionDays
    val diffMs = System.currentTimeMillis() - deletedAt
    val elapsedDays = (diffMs / (24 * 60 * 60 * 1000L)).toInt()
    return (retentionDays - elapsedDays).coerceAtLeast(0)
}
