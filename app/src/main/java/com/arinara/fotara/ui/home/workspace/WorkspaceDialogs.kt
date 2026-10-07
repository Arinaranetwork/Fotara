// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.home.workspace

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.arinara.fotara.R
import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.data.model.Workspace
import com.arinara.fotara.data.model.WorkspaceKind
import com.arinara.fotara.data.model.getDisplayName
import com.arinara.fotara.data.repository.WorkspaceContentStats
import com.arinara.fotara.data.repository.WorkspaceError
import com.arinara.fotara.data.repository.WorkspaceValidator
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.arinara.fotara.data.model.WorkspaceIcons
import com.arinara.fotara.data.model.getDisplay
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.theme.HomeCardBorder
import com.arinara.fotara.theme.HomeCardSurface
import com.arinara.fotara.theme.HomeMainButtonBlue
import com.arinara.fotara.theme.HomeSubtitleGray
import com.arinara.fotara.theme.TagCrimson

@Composable
fun AddWorkspaceDialog(
    workspaces: List<Workspace>,
    onDismiss: () -> Unit,
    onCreate: (name: String, iconKey: String?) -> Unit
) {
    var textValue by remember { mutableStateOf(TextFieldValue("")) }
    var selectedIconKey by remember { mutableStateOf(WorkspaceIcons.DEFAULT_KEY) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }

    val emptyError = stringResource(R.string.workspace_error_name_empty)
    val lengthError = stringResource(R.string.workspace_error_name_length)
    val duplicateError = stringResource(R.string.workspace_error_name_duplicate)
    val reservedError = stringResource(R.string.workspace_error_reserved)

    val currentValidationErr = WorkspaceValidator.validateName(textValue.text, workspaces)
    val isSaveEnabled = currentValidationErr == null

    fun submit() {
        if (currentValidationErr != null) {
            errorMessage = when (currentValidationErr) {
                WorkspaceError.NameEmpty -> emptyError
                WorkspaceError.NameTooLong -> lengthError
                WorkspaceError.NameDuplicate -> duplicateError
                WorkspaceError.NameReserved -> reservedError
                else -> emptyError
            }
            return
        }
        onCreate(textValue.text, selectedIconKey)
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HomeCardSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = stringResource(R.string.dialog_add_workspace_title),
                color = Color.White,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = textValue,
                    onValueChange = {
                        textValue = it
                        errorMessage = null
                    },
                    label = { Text(stringResource(R.string.workspace_name_label), color = HomeSubtitleGray, fontFamily = ElmsSans) },
                    singleLine = true,
                    isError = errorMessage != null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (isSaveEnabled) submit() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HomeMainButtonBlue,
                        unfocusedBorderColor = HomeCardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        errorBorderColor = TagCrimson
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage!!,
                        color = TagCrimson,
                        fontSize = 13.sp,
                        fontFamily = ElmsSans
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.workspace_icon_label),
                    color = HomeSubtitleGray,
                    fontFamily = ElmsSans,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(WorkspaceIcons.ALL_ICONS) { iconItem ->
                        val isSelected = selectedIconKey == iconItem.key
                        val iconCd = stringResource(R.string.cd_workspace_icon, iconItem.label)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) HomeMainButtonBlue.copy(alpha = 0.25f) else Color.Transparent)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) HomeMainButtonBlue else HomeCardBorder.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedIconKey = iconItem.key }
                                .semantics { contentDescription = iconCd },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(iconItem.resId),
                                contentDescription = null,
                                tint = if (isSelected) HomeMainButtonBlue else Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { submit() },
                enabled = isSaveEnabled,
                colors = ButtonDefaults.buttonColors(containerColor = HomeMainButtonBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.action_create),
                    color = Color.White,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.confirm_cancel),
                    color = HomeSubtitleGray,
                    fontFamily = ElmsSans
                )
            }
        }
    )
}

@Composable
fun AddWorkspaceDialog(
    workspaces: List<Workspace>,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    AddWorkspaceDialog(
        workspaces = workspaces,
        onDismiss = onDismiss,
        onCreate = { name, _ -> onCreate(name) }
    )
}

@Composable
fun EditWorkspaceDialog(
    workspace: Workspace,
    workspaces: List<Workspace>,
    onDismiss: () -> Unit,
    onSave: (newName: String, newIconKey: String?) -> Unit
) {
    var textValue by remember {
        mutableStateOf(TextFieldValue(workspace.name, TextRange(0, workspace.name.length)))
    }
    var selectedIconKey by remember {
        mutableStateOf(workspace.iconKey ?: WorkspaceIcons.DEFAULT_KEY)
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }

    val emptyError = stringResource(R.string.workspace_error_name_empty)
    val lengthError = stringResource(R.string.workspace_error_name_length)
    val duplicateError = stringResource(R.string.workspace_error_name_duplicate)
    val reservedError = stringResource(R.string.workspace_error_reserved)

    val currentValidationErr = WorkspaceValidator.validateName(textValue.text, workspaces, editingWorkspaceId = workspace.id)
    val isSaveEnabled = currentValidationErr == null

    fun submit() {
        if (currentValidationErr != null) {
            errorMessage = when (currentValidationErr) {
                WorkspaceError.NameEmpty -> emptyError
                WorkspaceError.NameTooLong -> lengthError
                WorkspaceError.NameDuplicate -> duplicateError
                WorkspaceError.NameReserved -> reservedError
                else -> emptyError
            }
            return
        }
        onSave(textValue.text, selectedIconKey)
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HomeCardSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = stringResource(R.string.dialog_edit_workspace_title),
                color = Color.White,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = textValue,
                    onValueChange = {
                        textValue = it
                        errorMessage = null
                    },
                    label = { Text(stringResource(R.string.workspace_name_label), color = HomeSubtitleGray, fontFamily = ElmsSans) },
                    singleLine = true,
                    isError = errorMessage != null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (isSaveEnabled) submit() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HomeMainButtonBlue,
                        unfocusedBorderColor = HomeCardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        errorBorderColor = TagCrimson
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage!!,
                        color = TagCrimson,
                        fontSize = 13.sp,
                        fontFamily = ElmsSans
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.workspace_icon_label),
                    color = HomeSubtitleGray,
                    fontFamily = ElmsSans,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(WorkspaceIcons.ALL_ICONS) { iconItem ->
                        val isSelected = selectedIconKey == iconItem.key
                        val iconCd = stringResource(R.string.cd_workspace_icon, iconItem.label)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) HomeMainButtonBlue.copy(alpha = 0.25f) else Color.Transparent)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) HomeMainButtonBlue else HomeCardBorder.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedIconKey = iconItem.key }
                                .semantics { contentDescription = iconCd },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(iconItem.resId),
                                contentDescription = null,
                                tint = if (isSelected) HomeMainButtonBlue else Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { submit() },
                enabled = isSaveEnabled,
                colors = ButtonDefaults.buttonColors(containerColor = HomeMainButtonBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.action_save),
                    color = Color.White,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.confirm_cancel),
                    color = HomeSubtitleGray,
                    fontFamily = ElmsSans
                )
            }
        }
    )
}

@Composable
fun RenameWorkspaceDialog(
    workspace: Workspace,
    workspaces: List<Workspace>,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    EditWorkspaceDialog(
        workspace = workspace,
        workspaces = workspaces,
        onDismiss = onDismiss,
        onSave = { newName, _ -> onRename(newName) }
    )
}

fun formatWorkspaceSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    val gb = mb / 1024.0
    return "%.1f GB".format(gb)
}

fun formatWorkspaceCountsAndSize(stats: WorkspaceContentStats): String {
    val fStr = if (stats.folderCount == 1) "1 folder" else "${stats.folderCount} folders"
    val nStr = if (stats.noteCount == 1) "1 note" else "${stats.noteCount} notes"
    val sStr = formatWorkspaceSize(stats.totalSizeBytes)
    return "$fStr · $nStr · $sStr"
}

@Composable
fun MoveToWorkspaceDialog(
    folders: List<Folder>,
    workspaces: List<Workspace>,
    onDismiss: () -> Unit,
    onMove: (targetWorkspaceId: Long) -> Unit
) {
    val title = if (folders.size == 1) {
        stringResource(R.string.dialog_move_to_workspace_title_single, folders[0].name)
    } else {
        stringResource(R.string.dialog_move_to_workspace_title_plural, folders.size)
    }

    val commonWorkspaceId = if (folders.isNotEmpty() && folders.all { it.workspaceId == folders[0].workspaceId }) {
        folders[0].workspaceId
    } else null

    var selectedId by remember { mutableStateOf<Long?>(null) }
    val orderedWorkspaces = remember(workspaces) { workspaces.sortedBy { it.position } }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HomeCardSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = title,
                color = Color.White,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                orderedWorkspaces.forEach { ws ->
                    val isCurrent = commonWorkspaceId != null && ws.id == commonWorkspaceId
                    val wsDisplay = ws.getDisplay()
                    val isSelected = selectedId == ws.id

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isCurrent) {
                                selectedId = ws.id
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { if (!isCurrent) selectedId = ws.id },
                            enabled = !isCurrent,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = HomeMainButtonBlue,
                                unselectedColor = HomeSubtitleGray,
                                disabledSelectedColor = HomeSubtitleGray.copy(alpha = 0.4f),
                                disabledUnselectedColor = HomeSubtitleGray.copy(alpha = 0.3f)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            painter = painterResource(wsDisplay.iconResId),
                            contentDescription = null,
                            tint = if (isCurrent) HomeSubtitleGray.copy(alpha = 0.5f) else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = wsDisplay.name,
                            color = if (isCurrent) HomeSubtitleGray.copy(alpha = 0.5f) else Color.White,
                            fontFamily = ElmsSans,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                        if (isCurrent) {
                            Text(
                                text = stringResource(R.string.workspace_current_badge),
                                color = HomeSubtitleGray.copy(alpha = 0.6f),
                                fontFamily = ElmsSans,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedId?.let { onMove(it) } },
                enabled = selectedId != null && (commonWorkspaceId == null || selectedId != commonWorkspaceId),
                colors = ButtonDefaults.buttonColors(containerColor = HomeMainButtonBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.action_move_confirm),
                    color = Color.White,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.confirm_cancel),
                    color = HomeSubtitleGray,
                    fontFamily = ElmsSans
                )
            }
        }
    )
}

@Composable
fun DeleteWorkspaceConfirmDialog(
    workspace: Workspace,
    stats: WorkspaceContentStats?,
    onDismiss: () -> Unit,
    onConfirmDelete: (deleteContents: Boolean) -> Unit
) {
    var alsoDeleteContents by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HomeCardSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            val wsDisplayName = workspace.getDisplayName()
            Text(
                text = stringResource(R.string.delete_workspace_confirm_title, wsDisplayName),
                color = Color.White,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (stats == null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = HomeMainButtonBlue,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Calculating contents...",
                            color = HomeSubtitleGray,
                            fontFamily = ElmsSans,
                            fontSize = 14.sp
                        )
                    }
                } else if (stats.folderCount == 0) {
                    Text(
                        text = stringResource(R.string.delete_workspace_empty_msg),
                        color = HomeSubtitleGray,
                        fontFamily = ElmsSans,
                        fontSize = 14.sp
                    )
                } else {
                    Text(
                        text = formatWorkspaceCountsAndSize(stats),
                        color = Color.White,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { alsoDeleteContents = !alsoDeleteContents }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = alsoDeleteContents,
                            onCheckedChange = { alsoDeleteContents = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = TagCrimson,
                                uncheckedColor = HomeSubtitleGray,
                                checkmarkColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.delete_workspace_checkbox_contents),
                            color = Color.White,
                            fontFamily = ElmsSans,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (alsoDeleteContents) {
                            stringResource(R.string.delete_workspace_helper_checked)
                        } else {
                            stringResource(R.string.delete_workspace_helper_unchecked)
                        },
                        color = HomeSubtitleGray,
                        fontFamily = ElmsSans,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 44.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmDelete(stats != null && stats.folderCount > 0 && alsoDeleteContents) },
                colors = ButtonDefaults.buttonColors(containerColor = TagCrimson),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.action_delete_workspace),
                    color = Color.White,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.confirm_cancel),
                    color = HomeSubtitleGray,
                    fontFamily = ElmsSans
                )
            }
        }
    )
}

@Composable
fun DeleteWorkspaceChoiceDialog(
    workspace: Workspace,
    stats: WorkspaceContentStats,
    onDismiss: () -> Unit,
    onSelectTrash: () -> Unit,
    onSelectPermanent: () -> Unit
) {
    val freedSize = formatWorkspaceSize(stats.totalSizeBytes)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HomeCardSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = stringResource(R.string.delete_workspace_contents_title),
                color = Color.White,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = HomeCardBorder.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectTrash() }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.delete_workspace_choice_trash),
                                color = Color.White,
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.delete_workspace_choice_trash_sub),
                                color = HomeSubtitleGray,
                                fontFamily = ElmsSans,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Surface(
                    color = TagCrimson.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TagCrimson.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectPermanent() }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = TagCrimson,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.delete_workspace_choice_perm),
                                color = TagCrimson,
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.delete_workspace_choice_perm_sub, freedSize),
                                color = HomeSubtitleGray,
                                fontFamily = ElmsSans,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.confirm_cancel),
                    color = HomeSubtitleGray,
                    fontFamily = ElmsSans
                )
            }
        }
    )
}

@Composable
fun DeleteWorkspacePermanentConfirmDialog(
    workspace: Workspace,
    stats: WorkspaceContentStats,
    onDismiss: () -> Unit,
    onConfirmDeleteForever: () -> Unit
) {
    val countsAndSize = formatWorkspaceCountsAndSize(stats)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HomeCardSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = stringResource(R.string.delete_workspace_perm_title),
                color = Color.White,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Text(
                text = stringResource(R.string.delete_workspace_perm_body, countsAndSize),
                color = HomeSubtitleGray,
                fontFamily = ElmsSans,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmDeleteForever,
                colors = ButtonDefaults.buttonColors(containerColor = TagCrimson),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.delete_workspace_perm_confirm_btn),
                    color = Color.White,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.confirm_cancel),
                    color = HomeSubtitleGray,
                    fontFamily = ElmsSans
                )
            }
        }
    )
}

@Composable
fun DeleteWorkspaceProgressDialog(
    workspaceName: String,
    current: Int,
    total: Int
) {
    BackHandler(enabled = true) { /* non-cancellable */ }

    AlertDialog(
        onDismissRequest = { /* non-cancellable */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        containerColor = HomeCardSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = stringResource(R.string.deleting_workspace_progress_title, workspaceName),
                color = Color.White,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            val progress = if (total > 0) current.toFloat() / total else 0f
            val percent = (progress * 100).toInt().coerceIn(0, 100)

            Column(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = TagCrimson,
                    trackColor = HomeCardBorder
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.deleting_workspace_folder_progress, current, total, percent),
                    color = HomeSubtitleGray,
                    fontFamily = ElmsSans,
                    fontSize = 13.sp
                )
            }
        },
        confirmButton = {}
    )
}

@Composable
fun RestoreWorkspaceDestinationDialog(
    workspaces: List<Workspace>,
    initialWorkspaceId: Long,
    onDismiss: () -> Unit,
    onConfirmRestore: (targetWorkspaceId: Long) -> Unit
) {
    var selectedId by remember { mutableStateOf(initialWorkspaceId) }
    val orderedWorkspaces = remember(workspaces) { workspaces.sortedBy { it.position } }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HomeCardSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = stringResource(R.string.dialog_restore_workspace_title),
                color = Color.White,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                orderedWorkspaces.forEach { ws ->
                    val wsDisplay = ws.getDisplay()
                    val isSelected = selectedId == ws.id

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedId = ws.id }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedId = ws.id },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = HomeMainButtonBlue,
                                unselectedColor = HomeSubtitleGray
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            painter = painterResource(wsDisplay.iconResId),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = wsDisplay.name,
                            color = Color.White,
                            fontFamily = ElmsSans,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmRestore(selectedId) },
                colors = ButtonDefaults.buttonColors(containerColor = HomeMainButtonBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.action_restore_confirm),
                    color = Color.White,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.confirm_cancel),
                    color = HomeSubtitleGray,
                    fontFamily = ElmsSans
                )
            }
        }
    )
}

