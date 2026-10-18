// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.trash

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.model.Workspace
import com.arinara.fotara.theme.FolderBodyBlue
import com.arinara.fotara.theme.MidnightNavy
import com.arinara.fotara.theme.MidnightSurface
import com.arinara.fotara.theme.TextPrimary
import com.arinara.fotara.theme.TextSecondary
import com.arinara.fotara.ui.components.WorkspaceFolderPickerColumn
import com.arinara.fotara.ui.components.presets.AppButton
import com.arinara.fotara.ui.components.presets.AppButtonVariant

@Composable
fun EmptyTrashConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidnightSurface,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "Empty Trash?",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "All folders and notes in Trash will be permanently removed from your device. This action cannot be undone.",
                color = TextSecondary,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            AppButton(
                text = "Empty Trash",
                onClick = onConfirm,
                variant = AppButtonVariant.DESTRUCTIVE,
                height = 36.dp,
                cornerRadius = 12.dp
            )
        },
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = AppButtonVariant.OUTLINED,
                height = 36.dp,
                cornerRadius = 12.dp
            )
        }
    )
}

@Composable
fun DeletePhotoPermanentlyDialog(
    photo: Photo,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidnightSurface,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "Delete Note Permanently?",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Permanently delete \"${photo.caption ?: "Untitled Note"}\"? It will be removed immediately from storage.",
                color = TextSecondary,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            AppButton(
                text = "Delete Permanently",
                onClick = onConfirm,
                variant = AppButtonVariant.DESTRUCTIVE,
                height = 36.dp,
                cornerRadius = 12.dp
            )
        },
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = AppButtonVariant.OUTLINED,
                height = 36.dp,
                cornerRadius = 12.dp
            )
        }
    )
}

@Composable
fun DeleteFolderPermanentlyDialog(
    folder: Folder,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidnightSurface,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "Delete Folder Permanently?",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Permanently delete \"${folder.name}\" and all of its notes? This action cannot be undone.",
                color = TextSecondary,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            AppButton(
                text = "Delete Permanently",
                onClick = onConfirm,
                variant = AppButtonVariant.DESTRUCTIVE,
                height = 36.dp,
                cornerRadius = 12.dp
            )
        },
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = AppButtonVariant.OUTLINED,
                height = 36.dp,
                cornerRadius = 12.dp
            )
        }
    )
}

@Composable
fun OrphanPhotoRestoreDialog(
    photo: Photo,
    parentFolder: Folder?,
    activeFolders: List<Folder>,
    workspaces: List<Workspace>,
    onDismiss: () -> Unit,
    onRestoreBoth: () -> Unit,
    onRestoreToActiveFolder: (Long) -> Unit
) {
    var selectedFolderIdForOrphan by remember { mutableStateOf<Long?>(activeFolders.firstOrNull()?.id) }
    var showFolderSelector by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidnightSurface,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "Original Folder in Trash",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "The original folder \"${parentFolder?.name ?: "Folder"}\" is also in Trash. Restore both, or select an active folder?",
                    color = TextSecondary,
                    fontSize = 14.sp
                )

                if (showFolderSelector && activeFolders.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Choose Destination Folder:",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    WorkspaceFolderPickerColumn(
                        folders = activeFolders,
                        workspaces = workspaces
                    ) { f ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedFolderIdForOrphan == f.id) FolderBodyBlue else MidnightNavy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedFolderIdForOrphan = f.id }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = f.name,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (showFolderSelector) {
                AppButton(
                    text = "Restore to Selected",
                    onClick = {
                        selectedFolderIdForOrphan?.let { targetId ->
                            onRestoreToActiveFolder(targetId)
                        }
                    },
                    variant = AppButtonVariant.PRIMARY,
                    height = 36.dp,
                    cornerRadius = 12.dp
                )
            } else {
                AppButton(
                    text = "Restore Both",
                    onClick = onRestoreBoth,
                    variant = AppButtonVariant.PRIMARY,
                    height = 36.dp,
                    cornerRadius = 12.dp
                )
            }
        },
        dismissButton = {
            if (!showFolderSelector && activeFolders.isNotEmpty()) {
                AppButton(
                    text = "Choose Folder",
                    onClick = { showFolderSelector = true },
                    variant = AppButtonVariant.OUTLINED,
                    height = 36.dp,
                    cornerRadius = 12.dp
                )
            } else {
                AppButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    variant = AppButtonVariant.OUTLINED,
                    height = 36.dp,
                    cornerRadius = 12.dp
                )
            }
        }
    )
}
