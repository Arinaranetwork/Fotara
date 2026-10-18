// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.notes

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.AlertDialog
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
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.ui.components.presets.AppButton
import com.arinara.fotara.ui.components.presets.AppButtonVariant
import com.arinara.fotara.ui.components.workspaceGroupedFolderItems
import com.arinara.fotara.ui.theme.ElectricSlateTokens

@Composable
fun NotesDeleteConfirmDialog(
    item: UnifiedNoteItem,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElectricSlateTokens.SurfaceElevated,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = stringResource(R.string.confirm_delete_title),
                color = ElectricSlateTokens.TextPrimary,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = 22.sp
            )
        },
        text = {
            Text(
                text = stringResource(R.string.confirm_delete_message, item.title),
                color = ElectricSlateTokens.TextMuted,
                fontFamily = ElmsSans,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            AppButton(
                text = stringResource(R.string.confirm_delete_confirm),
                onClick = onConfirm,
                variant = AppButtonVariant.DESTRUCTIVE
            )
        },
        dismissButton = {
            AppButton(
                text = stringResource(R.string.confirm_cancel),
                onClick = onDismiss,
                variant = AppButtonVariant.OUTLINED
            )
        }
    )
}

@Composable
fun NotesRenameDialog(
    item: UnifiedNoteItem,
    initialText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElectricSlateTokens.SurfaceElevated,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = stringResource(R.string.action_rename),
                color = ElectricSlateTokens.TextPrimary,
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
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedBorderColor = ElectricSlateTokens.PrimaryAccent,
                    unfocusedBorderColor = ElectricSlateTokens.BorderSubtle,
                    focusedTextColor = ElectricSlateTokens.TextPrimary,
                    unfocusedTextColor = ElectricSlateTokens.TextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            AppButton(
                text = "Save",
                onClick = { onConfirm(text) },
                variant = AppButtonVariant.PRIMARY
            )
        },
        dismissButton = {
            AppButton(
                text = stringResource(R.string.confirm_cancel),
                onClick = onDismiss,
                variant = AppButtonVariant.OUTLINED
            )
        }
    )
}

@Composable
fun NotesMoveDialog(
    item: UnifiedNoteItem,
    folders: List<Folder>,
    workspaces: List<Workspace>,
    onFolderSelected: (Folder) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElectricSlateTokens.SurfaceElevated,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = stringResource(R.string.action_move),
                color = ElectricSlateTokens.TextPrimary,
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
                        keyPrefix = "notes_move"
                    ) { folder ->
                        Surface(
                            color = if (folder.id == item.folderId) ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.2f) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onFolderSelected(folder) }
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
                                    color = ElectricSlateTokens.TextPrimary,
                                    fontFamily = ElmsSans,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    fontWeight = if (folder.id == item.folderId) FontWeight.Bold else FontWeight.Normal
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
                variant = AppButtonVariant.OUTLINED
            )
        }
    )
}

@Composable
fun NotesFolderPickDialog(
    folders: List<Folder>,
    workspaces: List<Workspace>,
    onFolderSelected: (Folder) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElectricSlateTokens.SurfaceElevated,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = stringResource(R.string.select_destination_folder),
                color = ElectricSlateTokens.TextPrimary,
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
                        keyPrefix = "notes_create"
                    ) { folder ->
                        Surface(
                            color = Color.Transparent,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onFolderSelected(folder) }
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
                                    color = ElectricSlateTokens.TextPrimary,
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
                variant = AppButtonVariant.OUTLINED
            )
        }
    )
}
