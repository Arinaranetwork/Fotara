// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.R
import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.data.model.Workspace
import com.arinara.fotara.data.repository.FolderBulkDeleteResult
import com.arinara.fotara.ui.components.workspaceGroupedFolderItems
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.theme.FolderBodyBlue
import com.arinara.fotara.theme.HomeCardBorder
import com.arinara.fotara.theme.HomeCardSurface
import com.arinara.fotara.theme.HomeMainButtonBlue
import com.arinara.fotara.theme.HomeSubtitleGray
import com.arinara.fotara.theme.TagAmber
import com.arinara.fotara.theme.TagCrimson
import com.arinara.fotara.theme.TextPrimary
import com.arinara.fotara.theme.TextSecondary
import com.arinara.fotara.ui.components.presets.AppButton
import com.arinara.fotara.ui.components.presets.AppButtonVariant
import com.arinara.fotara.ui.theme.ElectricSlateTokens

@Composable
internal fun HomeRenameFolderDialog(
    folder: Folder,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(folder.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HomeCardSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Rename Folder",
                color = TextPrimary,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = 22.sp
            )
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Folder Name", color = HomeSubtitleGray, fontFamily = ElmsSans) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HomeMainButtonBlue,
                    unfocusedBorderColor = HomeCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            AppButton(
                text = "Save",
                onClick = { onSave(text) },
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
internal fun HomeFolderContextMenuDialog(
    folder: Folder,
    onDismiss: () -> Unit,
    onPin: () -> Unit,
    onRename: () -> Unit,
    onSelect: () -> Unit,
    onLockUnlock: () -> Unit,
    onChangePin: () -> Unit,
    onLinkUnlink: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HomeCardSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = folder.name,
                color = TextPrimary,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = 22.sp
            )
        },
        text = {
            Column {
                // Action 1: Pin / Unpin
                Surface(
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPin() }
                        .padding(vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = null,
                            tint = ElectricSlateTokens.PrimaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (folder.isPinned) "Unpin from Top" else "Pin to Top",
                            color = TextPrimary,
                            fontFamily = ElmsSans,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }

                // Action 2: Rename
                Surface(
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRename() }
                        .padding(vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = ElectricSlateTokens.PrimaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Rename Folder",
                            color = TextPrimary,
                            fontFamily = ElmsSans,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }

                // Action 3: Select (Enters Multi-Select Mode)
                Surface(
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect() }
                        .padding(vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = FolderBodyBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Select",
                            color = TextPrimary,
                            fontFamily = ElmsSans,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }

                // Action 4: Lock / Unlock Folder
                Surface(
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLockUnlock() }
                        .padding(vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (folder.isLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (folder.isLocked) TagAmber else ElectricSlateTokens.PrimaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (folder.isLocked) "Remove Folder Lock" else "Lock Folder (PIN)",
                            color = TextPrimary,
                            fontFamily = ElmsSans,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }

                if (folder.isLocked) {
                    // Action 5: Change PIN
                    Surface(
                        color = Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChangePin() }
                            .padding(vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Password,
                                contentDescription = null,
                                tint = ElectricSlateTokens.PrimaryAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Change Folder PIN",
                                color = TextPrimary,
                                fontFamily = ElmsSans,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                // Action 6: Link It / Unlink
                Surface(
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLinkUnlink() }
                        .padding(vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (folder.linkGroupId != null) Icons.Default.LinkOff else Icons.Default.Link,
                            contentDescription = null,
                            tint = if (folder.linkGroupId != null) TagCrimson else ElectricSlateTokens.PrimaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (folder.linkGroupId != null) "Unlink Folder" else "Link to another folder",
                            color = if (folder.linkGroupId != null) TagCrimson else TextPrimary,
                            fontFamily = ElmsSans,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }

                // Action 7: Move to Trash / Delete
                Surface(
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDelete() }
                        .padding(vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = TagCrimson,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Move to Trash",
                            color = TagCrimson,
                            fontFamily = ElmsSans,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
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

@Composable
internal fun HomeBulkDeleteDialog(
    stats: FolderBulkDeleteResult,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val formattedMb = "%.1f MB".format(stats.totalSizeBytes / (1024f * 1024f))
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HomeCardSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Move ${stats.folderCount} Folder${if (stats.folderCount > 1) "s" else ""} to Trash?",
                color = TextPrimary,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Total Notes & Photos: ${stats.photoCount}",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Storage occupied: $formattedMb",
                    color = HomeSubtitleGray,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontFamily = ElmsSans
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Items moved to Trash can be restored within 30 days before being automatically purged. Associated deadline reminders will be cancelled.",
                    color = HomeSubtitleGray,
                    fontSize = 12.sp,
                    fontFamily = ElmsSans,
                    lineHeight = 16.sp
                )
            }
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
internal fun HomeFirstLaunchRestoreDialog(
    isRestoring: Boolean,
    onDismiss: () -> Unit,
    onImport: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HomeCardSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Welcome to Fotara",
                color = TextPrimary,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 24.sp
            )
        },
        text = {
            Column {
                Text(
                    text = "Do you want to import a .fotara file to restore your app data?",
                    color = TextSecondary,
                    fontFamily = ElmsSans,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                if (isRestoring) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = HomeMainButtonBlue,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Restoring backup...",
                            color = TextPrimary,
                            fontFamily = ElmsSans,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            AppButton(
                text = "Import Backup",
                onClick = onImport,
                enabled = !isRestoring,
                variant = AppButtonVariant.PRIMARY,
                height = 36.dp
            )
        },
        dismissButton = {
            AppButton(
                text = "Start Fresh",
                onClick = onDismiss,
                enabled = !isRestoring,
                variant = AppButtonVariant.SECONDARY,
                height = 36.dp
            )
        }
    )
}

@Composable
internal fun HomeDestinationFolderDialog(
    folders: List<Folder>,
    workspaces: List<Workspace>,
    onDismiss: () -> Unit,
    onSelectFolder: (Folder) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HomeCardSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = stringResource(R.string.select_destination_folder),
                color = TextPrimary,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = 22.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    workspaceGroupedFolderItems(
                        folders = folders,
                        workspaces = workspaces,
                        keyPrefix = "home_notes_create"
                    ) { folder ->
                        Surface(
                            color = Color.Transparent,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectFolder(folder) }
                                .padding(vertical = 8.dp, horizontal = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Folder,
                                    contentDescription = null,
                                    tint = ElectricSlateTokens.PrimaryAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = folder.name,
                                    color = TextPrimary,
                                    fontFamily = ElmsSans,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            AppButton(
                text = stringResource(R.string.confirm_cancel),
                onClick = onDismiss,
                variant = AppButtonVariant.SECONDARY,
                height = 36.dp
            )
        }
    )
}
