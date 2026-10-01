// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.note

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.data.model.TextNote
import com.arinara.fotara.data.repository.TextNoteRepository
import com.arinara.fotara.ui.components.RichMarkdownColumn
import com.arinara.fotara.ui.note.editor.EditorActions
import com.arinara.fotara.ui.note.editor.EditorToolbar
import com.arinara.fotara.ui.note.editor.rememberEditorState
import com.arinara.fotara.ui.note.editor.MarkdownVisualTransformation
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.platform.LocalContext
import com.arinara.fotara.ui.components.NoteDetailScheduleChip
import com.arinara.fotara.ui.components.ScheduleNoteDialog
import com.arinara.fotara.util.NoteScheduleManager
import com.arinara.fotara.util.ScheduleNoteType
import com.arinara.fotara.util.ScheduleAlertType
import kotlinx.coroutines.launch

private val ScreenNavy = Color(0xFF03071E)
private val TabCream = Color(0xFFEAE3D2)
private val AccentGold = Color(0xFFF77F00)
private val CardBg = Color(0xFF141936)
private val ToolbarBorder = Color(0xFF28325E)
private val TextMuted = Color(0xFF8E9AAF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextNoteEditorScreen(
    noteId: Long?,
    folderId: Long,
    subfolderId: Long?,
    textNoteRepository: TextNoteRepository,
    onBack: () -> Unit,
    onShare: (TextNote) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val state = rememberEditorState(
        initialNoteId = noteId,
        folderId = folderId,
        subfolderId = subfolderId,
        textNoteRepository = textNoteRepository,
        coroutineScope = coroutineScope
    )

    var isPreviewMode by remember { mutableStateOf(false) }

    val handleExit = {
        coroutineScope.launch {
            val purged = state.purgeIfCompletelyBlank()
            if (!purged) {
                state.flushAutosaveNow()
            }
            onBack()
        }
    }

    BackHandler {
        handleExit()
    }

    DisposableEffect(Unit) {
        onDispose {
            coroutineScope.launch {
                state.flushAutosaveNow()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenNavy)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (state.noteId == null) "New Text Note" else "Edit Text Note",
                            color = TabCream,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (state.isSaving) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Saving…",
                                color = AccentGold,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Text(
                        text = "${state.wordCount} words • ${state.charCount} characters",
                        color = TabCream.copy(alpha = 0.6f),
                        fontSize = 11.5.sp
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = { handleExit() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TabCream
                    )
                }
            },
            actions = {
                // Find & Replace toggle
                IconButton(onClick = { state.showFindReplace = !state.showFindReplace }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Find and Replace",
                        tint = if (state.showFindReplace) AccentGold else TabCream
                    )
                }

                // Preview mode toggle
                IconButton(onClick = { isPreviewMode = !isPreviewMode }) {
                    Icon(
                        imageVector = if (isPreviewMode) Icons.Default.EditNote else Icons.Default.Visibility,
                        contentDescription = if (isPreviewMode) "Edit Mode" else "Preview Mode",
                        tint = if (isPreviewMode) AccentGold else TabCream
                    )
                }

                // Share note
                IconButton(
                    onClick = {
                        val currentId = state.noteId ?: 0L
                        val note = TextNote(
                            id = currentId,
                            folderId = folderId,
                            subfolderId = subfolderId,
                            title = state.title.ifBlank { "Untitled Note" },
                            bodyMarkdown = state.bodyValue.text,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        onShare(note)
                    },
                    enabled = state.title.isNotBlank() || state.bodyValue.text.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Note",
                        tint = if (state.title.isNotBlank() || state.bodyValue.text.isNotBlank()) TabCream else TextMuted
                    )
                }

                // Overflow menu for note actions (Schedule, etc.)
                Box {
                    var showOverflowMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showOverflowMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More Options",
                            tint = TabCream
                        )
                    }
                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false },
                        modifier = Modifier
                            .background(CardBg)
                            .border(1.dp, ToolbarBorder, RoundedCornerShape(8.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Schedule...", color = TabCream) },
                            leadingIcon = { Icon(Icons.Default.Alarm, null, tint = AccentGold) },
                            onClick = {
                                showOverflowMenu = false
                                state.openScheduleDialog()
                            }
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = ScreenNavy)
        )

        // Find & Replace Bar
        AnimatedVisibility(
            visible = state.showFindReplace,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                color = CardBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = state.searchQuery,
                            onValueChange = { state.onSearchQueryChange(it) },
                            textStyle = TextStyle(color = TabCream, fontSize = 13.5.sp),
                            cursorBrush = SolidColor(AccentGold),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .background(ScreenNavy, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            decorationBox = { inner ->
                                if (state.searchQuery.isEmpty()) {
                                    Text("Find in note…", color = TextMuted, fontSize = 13.5.sp)
                                }
                                inner()
                            }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        BasicTextField(
                            value = state.replaceQuery,
                            onValueChange = { state.replaceQuery = it },
                            textStyle = TextStyle(color = TabCream, fontSize = 13.5.sp),
                            cursorBrush = SolidColor(AccentGold),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .background(ScreenNavy, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            decorationBox = { inner ->
                                if (state.replaceQuery.isEmpty()) {
                                    Text("Replace with…", color = TextMuted, fontSize = 13.5.sp)
                                }
                                inner()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (state.searchQuery.isNotEmpty()) {
                                if (state.searchMatchCount > 0) "${state.currentMatchIndex + 1}/${state.searchMatchCount}" else "0 matches"
                            } else "",
                            color = TextMuted,
                            fontSize = 11.5.sp
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Case sensitive toggle
                        TextButton(
                            onClick = {
                                state.caseSensitive = !state.caseSensitive
                                state.onSearchQueryChange(state.searchQuery)
                            }
                        ) {
                            Text(
                                text = "Aa",
                                color = if (state.caseSensitive) AccentGold else TextMuted,
                                fontWeight = if (state.caseSensitive) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Prev match
                        IconButton(
                            onClick = { state.findPrevious() },
                            modifier = Modifier.size(34.dp),
                            enabled = state.searchMatchCount > 0
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous Match", tint = TabCream)
                        }

                        // Next match
                        IconButton(
                            onClick = { state.findNext() },
                            modifier = Modifier.size(34.dp),
                            enabled = state.searchMatchCount > 0
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next Match", tint = TabCream)
                        }

                        // Replace one
                        TextButton(
                            onClick = { state.replaceCurrent() },
                            enabled = state.searchMatchCount > 0
                        ) {
                            Text("Replace", color = AccentGold, fontSize = 12.sp)
                        }

                        // Replace All
                        TextButton(
                            onClick = { state.replaceAll() },
                            enabled = state.searchMatchCount > 0
                        ) {
                            Text("All", color = AccentGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Close bar
                        IconButton(
                            onClick = { state.showFindReplace = false },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close Find", tint = TextMuted)
                        }
                    }
                }
            }
        }

        // Editor Body or Markdown Preview
        if (isPreviewMode) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = state.title.ifBlank { "Untitled Note" },
                    color = TabCream,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                if (state.scheduledAt != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    NoteDetailScheduleChip(
                        scheduledAt = state.scheduledAt,
                        alertType = state.alertType,
                        scheduleTitle = state.scheduleTitle,
                        onClick = { state.openScheduleDialog() }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                RichMarkdownColumn(
                    markdown = state.bodyValue.text.ifBlank { "*This note is empty. Switch to edit mode to start typing.*" },
                    primaryTextColor = TabCream,
                    accentColor = AccentGold,
                    cardBg = CardBg,
                    cardBorder = ToolbarBorder
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Title Input
                BasicTextField(
                    value = state.title,
                    onValueChange = { state.onTitleChange(it) },
                    textStyle = TextStyle(
                        color = TabCream,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    cursorBrush = SolidColor(AccentGold),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (state.title.isEmpty()) {
                                Text(
                                    text = "Note Title…",
                                    color = TextMuted,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                if (state.scheduledAt != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    NoteDetailScheduleChip(
                        scheduledAt = state.scheduledAt,
                        alertType = state.alertType,
                        scheduleTitle = state.scheduleTitle,
                        onClick = { state.openScheduleDialog() }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Rich Markdown Body with Live Styling VisualTransformation
                val visualTransformation = remember(state.bodyValue.selection) {
                    MarkdownVisualTransformation(
                        cursorStart = state.bodyValue.selection.start,
                        cursorEnd = state.bodyValue.selection.end,
                        hideUntouchedMarkers = true,
                        textColor = TabCream,
                        accentColor = AccentGold,
                        codeBgColor = Color(0xFF1E254A),
                        codeTextColor = Color(0xFFE2E8F0),
                        linkColor = Color(0xFF64B5F6),
                        mutedColor = TextMuted
                    )
                }

                BasicTextField(
                    value = state.bodyValue,
                    onValueChange = { newBody ->
                        val oldBody = state.bodyValue
                        // Intercept Enter key for list continuation
                        if (newBody.text.length == oldBody.text.length + 1 &&
                            oldBody.selection.min in oldBody.text.indices &&
                            newBody.text[oldBody.selection.min] == '\n'
                        ) {
                            val handled = EditorActions.handleEnterKey(oldBody)
                            if (handled != null) {
                                state.onBodyChange(handled)
                            } else {
                                state.onBodyChange(newBody)
                            }
                        } else if (newBody.text.length == oldBody.text.length - 1 &&
                            oldBody.selection.min == oldBody.selection.max &&
                            oldBody.selection.min > 0
                        ) {
                            val handled = EditorActions.handleBackspaceKey(oldBody)
                            if (handled != null) {
                                state.onBodyChange(handled)
                            } else {
                                state.onBodyChange(newBody)
                            }
                        } else {
                            state.onBodyChange(newBody)
                        }
                    },
                    visualTransformation = visualTransformation,
                    textStyle = TextStyle(
                        color = TabCream,
                        fontSize = 15.5.sp,
                        lineHeight = 23.sp,
                        fontFamily = FontFamily.Default
                    ),
                    cursorBrush = SolidColor(AccentGold),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Default
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (state.bodyValue.text.isEmpty()) {
                                Text(
                                    text = "Start writing with markdown formatting (e.g. **bold**, *italic*, # heading, - list, - [ ] checklist)…",
                                    color = TextMuted.copy(alpha = 0.7f),
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Keyboard-Docked Formatting Toolbar
            EditorToolbar(state = state)
        }
    }

    // Link Insertion and Edit Dialog
    if (state.showLinkDialog) {
        AlertDialog(
            onDismissRequest = { state.showLinkDialog = false },
            title = {
                Text(
                    text = "Insert or Edit Link",
                    fontWeight = FontWeight.Bold,
                    color = TabCream
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = state.linkDialogLabel,
                        onValueChange = { state.linkDialogLabel = it },
                        label = { Text("Link Text", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = ToolbarBorder,
                            focusedTextColor = TabCream,
                            unfocusedTextColor = TabCream
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.linkDialogUrl,
                        onValueChange = { state.linkDialogUrl = it },
                        label = { Text("Web URL", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = ToolbarBorder,
                            focusedTextColor = TabCream,
                            unfocusedTextColor = TabCream
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { state.confirmLinkDialog() },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold)
                ) {
                    Text("Apply", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { state.showLinkDialog = false }) {
                    Text("Cancel", color = TabCream)
                }
            },
            containerColor = CardBg,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Universal Schedule Dialog for Text Note
    if (state.showScheduleDialog) {
        val scheduleContext = LocalContext.current
        val scheduleManager = remember { NoteScheduleManager(scheduleContext) }
        ScheduleNoteDialog(
            noteTitle = state.title.ifBlank { "Untitled Note" },
            initialScheduledAt = state.scheduledAt,
            initialAlertType = try {
                ScheduleAlertType.valueOf(state.alertType ?: "NOTIFICATION")
            } catch (_: Exception) {
                ScheduleAlertType.NOTIFICATION
            },
            initialScheduleTitle = state.scheduleTitle,
            onDismiss = { state.closeScheduleDialog() },
            onSaveSchedule = { scheduledAt, alertType, scheduleTitle ->
                coroutineScope.launch {
                    state.flushAutosaveNow()
                    val currentNoteId = state.noteId
                    if (currentNoteId != null && currentNoteId > 0) {
                        scheduleManager.scheduleNote(
                            noteType = ScheduleNoteType.TEXT_NOTE,
                            noteId = currentNoteId,
                            folderId = state.folderId,
                            title = state.title.ifBlank { "Untitled Note" },
                            triggerAtMillis = scheduledAt,
                            alertType = alertType,
                            scheduleTitle = scheduleTitle
                        )
                        state.updateScheduleInfo(scheduledAt, alertType.name, scheduleTitle)
                    }
                }
                state.closeScheduleDialog()
            },
            onClearSchedule = {
                val currentNoteId = state.noteId
                if (currentNoteId != null && currentNoteId > 0) {
                    scheduleManager.cancelSchedule(ScheduleNoteType.TEXT_NOTE, currentNoteId)
                    state.updateScheduleInfo(null, null, null)
                }
                state.closeScheduleDialog()
            }
        )
    }
}
