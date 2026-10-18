// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.folder

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import com.arinara.fotara.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.arinara.fotara.data.model.DestinationType
import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.model.PhotoGroup
import com.arinara.fotara.data.model.RecentDestination
import com.arinara.fotara.data.model.Subfolder
import com.arinara.fotara.data.model.TagColor
import com.arinara.fotara.data.model.Workspace
import com.arinara.fotara.ui.components.presets.AppButton
import com.arinara.fotara.ui.components.presets.AppButtonVariant
import com.arinara.fotara.ui.components.workspaceGroupedFolderItems
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import java.io.File

@Composable
fun CreateGroupDialog(
    selectedCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElectricSlateTokens.SurfacePanel,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Create Group ($selectedCount notes)",
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Enter a name for this group (e.g. Exercise 1.5).",
                    color = ElectricSlateTokens.TextMuted,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    placeholder = { Text("Group name", color = ElectricSlateTokens.TextMuted) },
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
            }
        },
        confirmButton = {
            AppButton(
                text = "Create",
                onClick = {
                    if (groupName.trim().isNotBlank()) {
                        onConfirm(groupName.trim())
                    }
                },
                enabled = groupName.trim().isNotBlank(),
                variant = AppButtonVariant.Primary
            )
        },
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = AppButtonVariant.Secondary
            )
        }
    )
}

@Composable
fun RenameGroupDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var groupName by remember { mutableStateOf(currentName) }
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
            Column {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    placeholder = { Text("Group name", color = ElectricSlateTokens.TextMuted) },
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
            }
        },
        confirmButton = {
            AppButton(
                text = "Save",
                onClick = {
                    if (groupName.trim().isNotBlank()) {
                        onConfirm(groupName.trim())
                    }
                },
                enabled = groupName.trim().isNotBlank(),
                variant = AppButtonVariant.Primary
            )
        },
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = AppButtonVariant.Secondary
            )
        }
    )
}

@Composable
fun GroupDeleteConfirmDialog(
    group: PhotoGroup,
    memberCount: Int,
    totalSizeBytes: Long,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val sizeKb = (totalSizeBytes / 1024L).coerceAtLeast(1L)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElectricSlateTokens.SurfacePanel,
        shape = RoundedCornerShape(20.dp),
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
                text = "Move '${group.name}' and all its $memberCount notes ($sizeKb KB) to Trash? Items can be restored within 30 days.",
                color = ElectricSlateTokens.TextMuted,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            AppButton(
                text = "Move to Trash",
                onClick = onConfirm,
                variant = AppButtonVariant.Destructive
            )
        },
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = AppButtonVariant.Secondary
            )
        }
    )
}

@Composable
fun AddPhotosToGroupDialog(
    groupName: String,
    availablePhotos: List<Photo>,
    gridDensity: Int = 3,
    onDismiss: () -> Unit,
    onAdd: (List<Long>) -> Unit
) {
    val selectedIds = remember { mutableStateListOf<Long>() }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElectricSlateTokens.SurfacePanel,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Add Notes to '$groupName'",
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (availablePhotos.isEmpty()) {
                Text(
                    text = "No standalone notes available in this folder to add.",
                    color = ElectricSlateTokens.TextMuted,
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
                        items(availablePhotos, key = { it.id }) { photo ->
                            val isChecked = selectedIds.contains(photo.id)
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        1.dp,
                                        if (isChecked) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.BorderSubtle,
                                        RoundedCornerShape(12.dp)
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
                                        tint = ElectricSlateTokens.PrimaryAccent,
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
            AppButton(
                text = "Add (${selectedIds.size})",
                onClick = { onAdd(selectedIds.toList()) },
                enabled = selectedIds.isNotEmpty(),
                variant = AppButtonVariant.Primary
            )
        },
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = AppButtonVariant.Secondary
            )
        }
    )
}

@Composable
fun GroupColorDialog(
    onDismiss: () -> Unit,
    onSelectColor: (String?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElectricSlateTokens.SurfacePanel,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Assign Group Color Label",
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
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
                AppButton(
                    text = "Remove Color Label",
                    onClick = { onSelectColor(null) },
                    variant = AppButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = AppButtonVariant.Secondary
            )
        }
    )
}

@Composable
fun DestinationPickerDialog(
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
        shape = RoundedCornerShape(20.dp),
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
                                        color = ElectricSlateTokens.TrackInactive,
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
                variant = AppButtonVariant.Secondary
            )
        }
    )
}

@Composable
fun FolderColorChooserDialog(
    onDismiss: () -> Unit,
    onSelectColor: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElectricSlateTokens.SurfacePanel,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Choose Folder Color Label",
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
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
                                onSelectColor(tag.hex)
                            }
                    ) {}
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = AppButtonVariant.Secondary
            )
        }
    )
}

@Composable
fun RenameFolderDialog(
    currentName: String = "",
    initialName: String = currentName,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newFolderName by remember(currentName, initialName) { mutableStateOf(if (initialName.isNotBlank()) initialName else currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElectricSlateTokens.SurfacePanel,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Rename Folder",
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            OutlinedTextField(
                value = newFolderName,
                onValueChange = { newFolderName = it },
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
                onClick = { onConfirm(newFolderName) },
                variant = AppButtonVariant.Primary
            )
        },
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = AppButtonVariant.Secondary
            )
        }
    )
}

@Composable
fun RenameSubfolderDialog(
    currentName: String = "",
    initialName: String = currentName,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newSubName by remember(currentName, initialName) { mutableStateOf(if (initialName.isNotBlank()) initialName else currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElectricSlateTokens.SurfacePanel,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Rename Subfolder",
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            OutlinedTextField(
                value = newSubName,
                onValueChange = { newSubName = it },
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
                onClick = { onConfirm(newSubName) },
                variant = AppButtonVariant.Primary
            )
        },
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = AppButtonVariant.Secondary
            )
        }
    )
}

@Composable
fun DeleteFolderConfirmDialog(
    folderName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElectricSlateTokens.SurfacePanel,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Move Folder to Trash?",
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Move \"$folderName\" and all of its notes to Trash? Items can be restored within 30 days.",
                color = ElectricSlateTokens.TextMuted,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            AppButton(
                text = "Move to Trash",
                onClick = onConfirm,
                variant = AppButtonVariant.Destructive
            )
        },
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = AppButtonVariant.Secondary
            )
        }
    )
}

@Composable
fun AddSubfolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElectricSlateTokens.SurfacePanel,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "New Subfolder",
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Subfolder Name", color = ElectricSlateTokens.TextMuted) },
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
                text = "Create",
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                enabled = name.isNotBlank(),
                variant = AppButtonVariant.Primary
            )
        },
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = AppButtonVariant.Secondary
            )
        }
    )
}

@Composable
fun RenamePhotoNoteDialog(
    currentCaption: String? = null,
    initialCaption: String? = currentCaption,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val effectiveCaption = initialCaption ?: currentCaption ?: ""
    var newCaption by remember(currentCaption, initialCaption) { mutableStateOf(effectiveCaption) }
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
                value = newCaption,
                onValueChange = { newCaption = it },
                placeholder = { Text("Enter note caption...", color = ElectricSlateTokens.TextMuted) },
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
                onClick = { onConfirm(newCaption) },
                variant = AppButtonVariant.Primary
            )
        },
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = AppButtonVariant.Secondary
            )
        }
    )
}

@Composable
fun DeleteSubfolderConfirmDialog(
    subfolderName: String,
    photoCount: Int,
    sizeKb: Long,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElectricSlateTokens.SurfacePanel,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Move Subfolder to Trash?",
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Move \"$subfolderName\" and its $photoCount note${if (photoCount != 1) "s" else ""} (${sizeKb} KB) to Trash? Items can be restored within 30 days.",
                color = ElectricSlateTokens.TextMuted,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            AppButton(
                text = "Move to Trash",
                onClick = onConfirm,
                variant = AppButtonVariant.Destructive
            )
        },
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = AppButtonVariant.Secondary
            )
        }
    )
}

@Composable
fun SubfolderBulkDeleteConfirmDialog(
    subfolderCount: Int,
    photoCount: Int,
    sizeKb: Long,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElectricSlateTokens.SurfacePanel,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Move Subfolders to Trash?",
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Move $subfolderCount subfolders and their $photoCount note${if (photoCount != 1) "s" else ""} (${sizeKb} KB) to Trash? Items can be restored within 30 days.",
                color = ElectricSlateTokens.TextMuted,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            AppButton(
                text = "Move to Trash",
                onClick = onConfirm,
                variant = AppButtonVariant.Destructive
            )
        },
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = AppButtonVariant.Secondary
            )
        }
    )
}

@Composable
fun PhotoBulkDeleteConfirmDialog(
    count: Int = 0,
    selectedCount: Int = count,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val effectiveCount = if (selectedCount > 0) selectedCount else count
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElectricSlateTokens.SurfacePanel,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Move Notes to Trash?",
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Move $effectiveCount selected note${if (effectiveCount != 1) "s" else ""} to Trash? Items can be restored within 30 days.",
                color = ElectricSlateTokens.TextMuted,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            AppButton(
                text = "Move to Trash",
                onClick = onConfirm,
                variant = AppButtonVariant.Destructive
            )
        },
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = AppButtonVariant.Secondary
            )
        }
    )
}

@Composable
fun MergeGroupsDialog(
    groupCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElectricSlateTokens.SurfacePanel,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Merge Groups",
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Combine $groupCount groups into a single group. All member notes will be merged.",
                    color = ElectricSlateTokens.TextMuted,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("New Group Name", color = ElectricSlateTokens.TextMuted) },
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
            }
        },
        confirmButton = {
            AppButton(
                text = "Merge",
                onClick = {
                    val trimmed = groupName.trim()
                    if (trimmed.isNotEmpty()) {
                        onConfirm(trimmed)
                    }
                },
                enabled = groupName.isNotBlank(),
                variant = AppButtonVariant.Primary
            )
        },
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = AppButtonVariant.Secondary
            )
        }
    )
}

@Composable
fun AddToGroupPickerDialog(
    sections: List<GroupSection> = emptyList(),
    hasOtherFolderSections: Boolean = false,
    sectionResult: GroupSectionsResult? = null,
    onDismiss: () -> Unit,
    onSelectGroup: (groupId: Long, groupName: String) -> Unit
) {
    val effectiveSections = if (sections.isNotEmpty()) sections else sectionResult?.sections ?: emptyList()
    val effectiveHasOther = hasOtherFolderSections || (sectionResult?.hasOtherFolderSections == true)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElectricSlateTokens.SurfacePanel,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column {
                Text(
                    text = stringResource(R.string.add_to_group_title),
                    color = ElectricSlateTokens.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                if (effectiveHasOther) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.add_to_group_other_folder_caption),
                        color = ElectricSlateTokens.TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
            ) {
                if (effectiveSections.isEmpty()) {
                    Text(
                        text = stringResource(R.string.add_to_group_no_groups),
                        color = ElectricSlateTokens.TextMuted,
                        fontSize = 14.sp
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (section in effectiveSections) {
                            item(key = "header_${section.folderId}") {
                                val headerText = if (section.isCurrentFolder) {
                                    stringResource(R.string.add_to_group_this_folder_header, section.folderName)
                                } else {
                                    section.folderName
                                }
                                Text(
                                    text = headerText,
                                    color = ElectricSlateTokens.TextMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }
                            items(section.groups, key = { it.id }) { grp ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = ElectricSlateTokens.SurfaceElevated,
                                    border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSelectGroup(grp.id, grp.name)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Layers, null, tint = ElectricSlateTokens.PrimaryAccent, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(grp.name, color = ElectricSlateTokens.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
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
                text = stringResource(R.string.setting_combine_file_name_cancel),
                onClick = onDismiss,
                variant = AppButtonVariant.Secondary
            )
        }
    )
}

@Composable
fun FolderExportFormatDialog(
    targetGroupName: String? = null,
    title: String? = null,
    onDismiss: () -> Unit,
    onExportPdf: () -> Unit,
    onExportZip: () -> Unit
) {
    val displayTitle = title ?: if (targetGroupName != null) "Export \"$targetGroupName\"" else "Export Selected Notes"
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElectricSlateTokens.SurfacePanel,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = displayTitle,
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Choose an export format:",
                    color = ElectricSlateTokens.TextMuted,
                    fontSize = 14.sp
                )

                // Option 1: PDF Document
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ElectricSlateTokens.SurfaceElevated,
                    border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExportPdf() }
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PictureAsPdf, null, tint = ElectricSlateTokens.PrimaryAccent, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("PDF Document (.pdf)", color = ElectricSlateTokens.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Formatted pages with headers & OCR text", color = ElectricSlateTokens.TextMuted, fontSize = 12.sp)
                        }
                    }
                }

                // Option 2: ZIP Archive
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ElectricSlateTokens.SurfaceElevated,
                    border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExportZip() }
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Folder, null, tint = ElectricSlateTokens.PrimaryAccent, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("ZIP Archive (.zip)", color = ElectricSlateTokens.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Original full-resolution images", color = ElectricSlateTokens.TextMuted, fontSize = 12.sp)
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
                variant = AppButtonVariant.Secondary
            )
        }
    )
}

@Composable
fun BatchTagColorChooserDialog(
    onDismiss: () -> Unit,
    onSelectColor: (String?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElectricSlateTokens.SurfacePanel,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Assign Color Label",
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
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
                AppButton(
                    text = "Remove Color Label",
                    onClick = { onSelectColor(null) },
                    variant = AppButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = AppButtonVariant.Secondary
            )
        }
    )
}

@Composable
fun RenameDocumentDialog(
    initialTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newTitle by remember { mutableStateOf(initialTitle) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElectricSlateTokens.SurfacePanel,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Rename Document",
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            OutlinedTextField(
                value = newTitle,
                onValueChange = { newTitle = it },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = ElectricSlateTokens.TextPrimary,
                    unfocusedTextColor = ElectricSlateTokens.TextPrimary,
                    focusedBorderColor = ElectricSlateTokens.PrimaryAccent,
                    unfocusedBorderColor = ElectricSlateTokens.BorderSubtle
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            AppButton(
                text = "Save",
                onClick = {
                    if (newTitle.isNotBlank()) {
                        onConfirm(newTitle)
                    }
                },
                variant = AppButtonVariant.Primary
            )
        },
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = AppButtonVariant.Secondary
            )
        }
    )
}

@Composable
fun SplitPdfConfirmDialog(
    documentName: String,
    pageCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElectricSlateTokens.SurfacePanel,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Split PDF into Images?",
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = com.arinara.fotara.util.PdfSplitManager.getConfirmationDescription(documentName, pageCount),
                color = ElectricSlateTokens.TextMuted,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            AppButton(
                text = "Split & Delete PDF",
                onClick = onConfirm,
                variant = AppButtonVariant.Destructive
            )
        },
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = AppButtonVariant.Secondary
            )
        }
    )
}

@Composable
fun DeleteDocumentConfirmDialog(
    documentName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElectricSlateTokens.SurfacePanel,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Move Document to Trash?",
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Move \"$documentName\" to Trash? It can be restored within 30 days.",
                color = ElectricSlateTokens.TextMuted,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            AppButton(
                text = "Move to Trash",
                onClick = onConfirm,
                variant = AppButtonVariant.Destructive
            )
        },
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = AppButtonVariant.Secondary
            )
        }
    )
}

@Composable
fun DocumentImportProgressDialog(
    current: Int,
    total: Int,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Cannot dismiss without cancel */ },
        containerColor = ElectricSlateTokens.SurfacePanel,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Importing Document",
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (total > 0) "Rendering page $current of $total..." else "Preparing document...",
                    color = ElectricSlateTokens.TextMuted,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (total > 0) {
                    LinearProgressIndicator(
                        progress = { (current.toFloat() / total.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = ElectricSlateTokens.PrimaryAccent,
                        trackColor = ElectricSlateTokens.TrackInactive
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = ElectricSlateTokens.PrimaryAccent,
                        trackColor = ElectricSlateTokens.TrackInactive
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${if (total > 0) (current * 100 / total) else 0}% complete",
                    color = ElectricSlateTokens.TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onCancel,
                variant = AppButtonVariant.Destructive
            )
        }
    )
}

@Composable
fun PdfSplitProgressDialog(
    current: Int,
    total: Int,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Cannot dismiss without cancel */ },
        containerColor = ElectricSlateTokens.SurfacePanel,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Splitting PDF into Images",
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (total > 0) "Rendering page $current of $total..." else "Preparing pages...",
                    color = ElectricSlateTokens.TextMuted,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (total > 0) {
                    LinearProgressIndicator(
                        progress = { (current.toFloat() / total.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = ElectricSlateTokens.PrimaryAccent,
                        trackColor = ElectricSlateTokens.TrackInactive
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = ElectricSlateTokens.PrimaryAccent,
                        trackColor = ElectricSlateTokens.TrackInactive
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${if (total > 0) (current * 100 / total) else 0}% complete",
                    color = ElectricSlateTokens.TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onCancel,
                variant = AppButtonVariant.Destructive
            )
        }
    )
}

@Composable
fun RenameTextNoteDialog(
    initialTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newTitle by remember { mutableStateOf(initialTitle) }
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
                value = newTitle,
                onValueChange = { newTitle = it },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = ElectricSlateTokens.TextPrimary,
                    unfocusedTextColor = ElectricSlateTokens.TextPrimary,
                    focusedBorderColor = ElectricSlateTokens.PrimaryAccent,
                    unfocusedBorderColor = ElectricSlateTokens.BorderSubtle
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            AppButton(
                text = "Save",
                onClick = {
                    if (newTitle.isNotBlank()) {
                        onConfirm(newTitle)
                    }
                },
                variant = AppButtonVariant.Primary
            )
        },
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = AppButtonVariant.Secondary
            )
        }
    )
}

@Composable
fun DeleteTextNoteConfirmDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElectricSlateTokens.SurfacePanel,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Move Note to Trash?",
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Move \"$title\" to Trash? It can be restored within 30 days.",
                color = ElectricSlateTokens.TextMuted,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            AppButton(
                text = "Move to Trash",
                onClick = onConfirm,
                variant = AppButtonVariant.Destructive
            )
        },
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = AppButtonVariant.Secondary
            )
        }
    )
}

@Composable
fun RenameCanvasNoteDialog(
    initialTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newTitle by remember { mutableStateOf(initialTitle) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElectricSlateTokens.SurfacePanel,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Rename Canvas Note",
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            OutlinedTextField(
                value = newTitle,
                onValueChange = { newTitle = it },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = ElectricSlateTokens.TextPrimary,
                    unfocusedTextColor = ElectricSlateTokens.TextPrimary,
                    focusedBorderColor = ElectricSlateTokens.PrimaryAccent,
                    unfocusedBorderColor = ElectricSlateTokens.BorderSubtle
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            AppButton(
                text = "Save",
                onClick = {
                    if (newTitle.isNotBlank()) {
                        onConfirm(newTitle)
                    }
                },
                variant = AppButtonVariant.Primary
            )
        },
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = AppButtonVariant.Secondary
            )
        }
    )
}

@Composable
fun DeleteCanvasNoteConfirmDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElectricSlateTokens.SurfacePanel,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Move Note to Trash?",
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Move \"$title\" to Trash? It can be restored within 30 days.",
                color = ElectricSlateTokens.TextMuted,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            AppButton(
                text = "Move to Trash",
                onClick = onConfirm,
                variant = AppButtonVariant.Destructive
            )
        },
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = AppButtonVariant.Secondary
            )
        }
    )
}

