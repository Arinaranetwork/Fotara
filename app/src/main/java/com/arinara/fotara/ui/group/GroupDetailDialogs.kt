// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.group

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.data.model.DestinationType
import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.model.RecentDestination
import com.arinara.fotara.data.model.Subfolder
import com.arinara.fotara.data.model.TagColor
import com.arinara.fotara.data.model.Workspace
import com.arinara.fotara.ui.components.presets.AppButton
import com.arinara.fotara.ui.components.presets.AppButtonVariant
import com.arinara.fotara.ui.components.workspaceGroupedFolderItems
import com.arinara.fotara.ui.folder.DetailPhotoCard
import com.arinara.fotara.ui.folder.DetailCanvasCard
import com.arinara.fotara.ui.folder.DetailTextNoteCard
import com.arinara.fotara.ui.folder.FolderGridItem
import com.arinara.fotara.data.model.CanvasNote
import com.arinara.fotara.data.model.TextNote
import com.arinara.fotara.ui.theme.ElectricSlateTokens

@Composable
fun GroupExportDialog(
    groupName: String,
    onExportPdf: () -> Unit,
    onExportZip: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElectricSlateTokens.SurfacePanel,
        title = {
            Text(
                text = "Export \"$groupName\"",
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = ElectricSlateTokens.SurfaceElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExportPdf() }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = ElectricSlateTokens.PrimaryAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "PDF Document (.pdf)",
                                color = ElectricSlateTokens.TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Combines all notes into a single document",
                                color = ElectricSlateTokens.TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Surface(
                    color = ElectricSlateTokens.SurfaceElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExportZip() }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = ElectricSlateTokens.PrimaryAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "ZIP Archive (.zip)",
                                color = ElectricSlateTokens.TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Original high-resolution photos",
                                color = ElectricSlateTokens.TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = AppButtonVariant.SECONDARY,
                height = 36.dp
            )
        }
    )
}

@Composable
fun GroupColorDialog(
    currentColor: String?,
    onSelectColor: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElectricSlateTokens.SurfacePanel,
        title = {
            Text(
                text = "Group Color Label",
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                TagColor.entries.forEach { tag ->
                    val isSelected = currentColor == tag.hex
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(tag.composeColor)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) ElectricSlateTokens.PrimaryAccent else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable {
                                val newColor = if (isSelected) null else tag.hex
                                onSelectColor(newColor)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = ElectricSlateTokens.TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            AppButton(
                text = "Close",
                onClick = onDismiss,
                variant = AppButtonVariant.SECONDARY,
                height = 36.dp
            )
        }
    )
}

@Composable
fun GroupDeleteConfirmDialog(
    groupName: String,
    count: Int,
    totalSizeKb: Long,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElectricSlateTokens.SurfacePanel,
        title = {
            Text(
                text = "Move Group to Trash?",
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Move \"$groupName\" and its $count notes ($totalSizeKb KB) to Trash? Items can be restored within 30 days.",
                color = ElectricSlateTokens.TextMuted,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            AppButton(
                text = "Move to Trash",
                onClick = onConfirm,
                variant = AppButtonVariant.DESTRUCTIVE,
                height = 36.dp
            )
        },
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = AppButtonVariant.SECONDARY,
                height = 36.dp
            )
        }
    )
}

@Composable
fun GroupAddPhotosDialog(
    groupName: String,
    availablePhotos: List<Photo>,
    availableCanvasNotes: List<CanvasNote> = emptyList(),
    availableTextNotes: List<TextNote> = emptyList(),
    gridDensity: Int,
    onAdd: (List<Long>) -> Unit = {},
    onAddItems: ((photoIds: List<Long>, canvasIds: List<Long>, textNoteIds: List<Long>) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val selectedPhotoIds = remember { mutableStateListOf<Long>() }
    val selectedCanvasIds = remember { mutableStateListOf<Long>() }
    val selectedTextIds = remember { mutableStateListOf<Long>() }
    val totalSelected = selectedPhotoIds.size + selectedCanvasIds.size + selectedTextIds.size
    val totalAvailable = availablePhotos.size + availableCanvasNotes.size + availableTextNotes.size

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElectricSlateTokens.SurfacePanel,
        title = {
            Text(
                text = "Add Notes to \"$groupName\"",
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (totalAvailable == 0) {
                Text(
                    text = "No standalone notes available in this folder to add.",
                    color = ElectricSlateTokens.TextMuted,
                    fontSize = 14.sp
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    Text(
                        text = "Select notes to fold into this group:",
                        color = ElectricSlateTokens.TextMuted,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(gridDensity.coerceIn(2, 4)),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(availablePhotos, key = { "photo_${it.id}" }) { photo ->
                            val isChecked = selectedPhotoIds.contains(photo.id)
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (isChecked) 2.dp else 1.dp,
                                        color = if (isChecked) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.BorderSubtle,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        if (isChecked) selectedPhotoIds.remove(photo.id)
                                        else selectedPhotoIds.add(photo.id)
                                    }
                            ) {
                                DetailPhotoCard(
                                    photo = photo,
                                    isBatchMode = true,
                                    isSelected = isChecked,
                                    onCardClick = {
                                        if (isChecked) selectedPhotoIds.remove(photo.id)
                                        else selectedPhotoIds.add(photo.id)
                                    }
                                )
                            }
                        }

                        items(availableCanvasNotes, key = { "canvas_${it.id}" }) { canvasNote ->
                            val isChecked = selectedCanvasIds.contains(canvasNote.id)
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (isChecked) 2.dp else 1.dp,
                                        color = if (isChecked) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.BorderSubtle,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        if (isChecked) selectedCanvasIds.remove(canvasNote.id)
                                        else selectedCanvasIds.add(canvasNote.id)
                                    }
                            ) {
                                DetailCanvasCard(
                                    canvasItem = FolderGridItem.CanvasNoteItem(canvasNote),
                                    isBatchMode = true,
                                    isSelected = isChecked,
                                    onCardClick = {
                                        if (isChecked) selectedCanvasIds.remove(canvasNote.id)
                                        else selectedCanvasIds.add(canvasNote.id)
                                    },
                                    onCardLongClick = {}
                                )
                            }
                        }

                        items(availableTextNotes, key = { "text_${it.id}" }) { textNote ->
                            val isChecked = selectedTextIds.contains(textNote.id)
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (isChecked) 2.dp else 1.dp,
                                        color = if (isChecked) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.BorderSubtle,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        if (isChecked) selectedTextIds.remove(textNote.id)
                                        else selectedTextIds.add(textNote.id)
                                    }
                            ) {
                                DetailTextNoteCard(
                                    noteItem = FolderGridItem.TextNoteItem(textNote),
                                    isBatchMode = true,
                                    isSelected = isChecked,
                                    onCardClick = {
                                        if (isChecked) selectedTextIds.remove(textNote.id)
                                        else selectedTextIds.add(textNote.id)
                                    },
                                    onCardLongClick = {}
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (totalAvailable > 0) {
                AppButton(
                    text = "Add ($totalSelected)",
                    onClick = {
                        if (onAddItems != null) {
                            onAddItems(selectedPhotoIds.toList(), selectedCanvasIds.toList(), selectedTextIds.toList())
                        } else {
                            onAdd(selectedPhotoIds.toList())
                        }
                    },
                    enabled = totalSelected > 0,
                    variant = AppButtonVariant.PRIMARY,
                    height = 36.dp
                )
            }
        },
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = AppButtonVariant.SECONDARY,
                height = 36.dp
            )
        }
    )
}
@Composable
fun GroupDestinationPickerDialog(
    title: String,
    recentDestinations: List<RecentDestination>,
    availableFolders: List<Folder>,
    currentFolderId: Long?,
    currentSubfolders: List<Subfolder>,
    workspaces: List<Workspace> = emptyList(),
    onSelectDestination: (folderId: Long, subfolderId: Long?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElectricSlateTokens.SurfacePanel,
        title = {
            Text(
                text = title,
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
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
                                color = ElectricSlateTokens.PrimaryAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                            )
                        }
                        items(recentDestinations, key = { "recent_${it.type}_${it.folderId}_${it.subfolderId}_${it.groupId}" }) { recent ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ElectricSlateTokens.SurfaceElevated,
                                border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
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
                                        tint = ElectricSlateTokens.PrimaryAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = recent.title,
                                        color = ElectricSlateTokens.TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        item {
                            Text(
                                text = "All Folders",
                                color = ElectricSlateTokens.TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                    }

                    workspaceGroupedFolderItems(
                        folders = availableFolders,
                        workspaces = workspaces,
                        keyPrefix = "dest_picker"
                    ) { targetF ->
                        val isCurrent = targetF.id == currentFolderId
                        Column {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isCurrent) ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.15f) else ElectricSlateTokens.SurfaceElevated,
                                border = BorderStroke(
                                    1.dp,
                                    if (isCurrent) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.BorderSubtle
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
                                        color = ElectricSlateTokens.TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            if (isCurrent && currentSubfolders.isNotEmpty()) {
                                currentSubfolders.forEach { sub ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = ElectricSlateTokens.SurfaceElevated,
                                        border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
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
                                                tint = ElectricSlateTokens.PrimaryAccent,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = sub.name,
                                                color = ElectricSlateTokens.TextPrimary,
                                                fontSize = 14.sp
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
            AppButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = AppButtonVariant.SECONDARY,
                height = 36.dp
            )
        }
    )
}

@Composable
fun GroupRenameDialog(
    initialName: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElectricSlateTokens.SurfacePanel,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Rename Group",
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = ElectricSlateTokens.TextPrimary,
                    unfocusedTextColor = ElectricSlateTokens.TextPrimary,
                    focusedBorderColor = ElectricSlateTokens.PrimaryAccent,
                    unfocusedBorderColor = ElectricSlateTokens.BorderSubtle,
                    cursorColor = ElectricSlateTokens.PrimaryAccent
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            AppButton(
                text = "Save",
                onClick = {
                    if (newName.isNotBlank()) {
                        onSave(newName.trim())
                    }
                },
                enabled = newName.isNotBlank(),
                variant = AppButtonVariant.PRIMARY,
                height = 36.dp
            )
        },
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = AppButtonVariant.SECONDARY,
                height = 36.dp
            )
        }
    )
}

@Composable
fun GroupNoteRenameDialog(
    initialName: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElectricSlateTokens.SurfacePanel,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Rename Note",
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = ElectricSlateTokens.TextPrimary,
                    unfocusedTextColor = ElectricSlateTokens.TextPrimary,
                    focusedBorderColor = ElectricSlateTokens.PrimaryAccent,
                    unfocusedBorderColor = ElectricSlateTokens.BorderSubtle,
                    cursorColor = ElectricSlateTokens.PrimaryAccent
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            AppButton(
                text = "Save",
                onClick = {
                    if (newName.isNotBlank()) {
                        onSave(newName.trim())
                    }
                },
                enabled = newName.isNotBlank(),
                variant = AppButtonVariant.PRIMARY,
                height = 36.dp
            )
        },
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = AppButtonVariant.SECONDARY,
                height = 36.dp
            )
        }
    )
}

@Composable
fun GroupDeadlineDialog(
    hasExistingDeadline: Boolean,
    onSetDeadline: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val now = System.currentTimeMillis()
    val oneDayMs = 24 * 60 * 60 * 1000L
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElectricSlateTokens.SurfacePanel,
        title = {
            Text(
                text = "Set Group Deadline",
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Reminders trigger 24 hours prior:",
                    color = ElectricSlateTokens.TextMuted,
                    fontSize = 14.sp
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ElectricSlateTokens.SurfaceElevated,
                    border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSetDeadline(now + oneDayMs) }
                ) {
                    Text(
                        text = "Due Tomorrow (Urgent)",
                        color = ElectricSlateTokens.TextPrimary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ElectricSlateTokens.SurfaceElevated,
                    border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSetDeadline(now + 3 * oneDayMs) }
                ) {
                    Text(
                        text = "Due in 3 Days",
                        color = ElectricSlateTokens.TextPrimary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ElectricSlateTokens.SurfaceElevated,
                    border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSetDeadline(now + 7 * oneDayMs) }
                ) {
                    Text(
                        text = "Due Next Week",
                        color = ElectricSlateTokens.TextPrimary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }

                if (hasExistingDeadline) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ElectricSlateTokens.Destructive.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, ElectricSlateTokens.Destructive.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSetDeadline(null) }
                    ) {
                        Text(
                            text = "Remove Deadline",
                            color = ElectricSlateTokens.Destructive,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = AppButtonVariant.SECONDARY,
                height = 36.dp
            )
        }
    )
}

