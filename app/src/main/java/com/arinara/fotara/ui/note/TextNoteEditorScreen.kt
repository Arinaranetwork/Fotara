// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.note

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.data.model.TextNote
import com.arinara.fotara.data.repository.TextNoteRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val ScreenNavy = Color(0xFF03071E)
private val TabCream = Color(0xFFEAE3D2)
private val AccentGold = Color(0xFFF77F00)
private val ToolbarBg = Color(0xFF141936)
private val ToolbarBorder = Color(0xFF242C56)
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
    var activeNoteId by remember { mutableStateOf(noteId) }
    var title by remember { mutableStateOf("") }
    var bodyValue by remember { mutableStateOf(TextFieldValue("")) }

    // Undo / Redo history stacks
    val undoStack = remember { mutableListOf<TextFieldValue>() }
    val redoStack = remember { mutableListOf<TextFieldValue>() }

    val coroutineScope = rememberCoroutineScope()
    var autosaveJob by remember { mutableStateOf<Job?>(null) }
    var isAutosaving by remember { mutableStateOf(false) }

    // Load existing note if editing
    LaunchedEffect(noteId) {
        if (noteId != null && noteId > 0) {
            val note = textNoteRepository.getTextNoteByIdOnce(noteId)
            if (note != null) {
                title = note.title
                bodyValue = TextFieldValue(note.bodyMarkdown, selection = TextRange(note.bodyMarkdown.length))
            }
        }
    }

    // Debounced autosave mechanism (300ms)
    fun scheduleAutosave(newTitle: String, newBody: String) {
        autosaveJob?.cancel()
        if (newTitle.isBlank() && newBody.isBlank() && activeNoteId == null) {
            return
        }
        autosaveJob = coroutineScope.launch {
            delay(300)
            isAutosaving = true
            val currentId = activeNoteId
            if (currentId == null || currentId <= 0) {
                val effectiveTitle = newTitle.ifBlank { "Untitled Note" }
                val createdId = textNoteRepository.createTextNote(
                    folderId = folderId,
                    subfolderId = subfolderId,
                    title = effectiveTitle,
                    bodyMarkdown = newBody
                )
                activeNoteId = createdId
            } else {
                textNoteRepository.updateTextNote(
                    id = currentId,
                    title = newTitle.ifBlank { "Untitled Note" },
                    bodyMarkdown = newBody
                )
            }
            isAutosaving = false
        }
    }

    fun updateBodyWithUndo(newValue: TextFieldValue) {
        if (newValue.text != bodyValue.text) {
            undoStack.add(bodyValue)
            if (undoStack.size > 50) undoStack.removeAt(0)
            redoStack.clear()
        }
        bodyValue = newValue
        scheduleAutosave(title, newValue.text)
    }

    fun applyWrapFormatting(prefix: String, suffix: String) {
        val sel = bodyValue.selection
        val text = bodyValue.text
        val selectedText = text.substring(sel.start, sel.end)
        val newText = text.substring(0, sel.start) + prefix + selectedText + suffix + text.substring(sel.end)
        val newCursor = if (sel.collapsed) sel.start + prefix.length else sel.start + prefix.length + selectedText.length + suffix.length
        updateBodyWithUndo(TextFieldValue(newText, selection = TextRange(newCursor)))
    }

    fun applyLinePrefix(linePrefix: String) {
        val sel = bodyValue.selection
        val text = bodyValue.text
        val lineStart = text.lastIndexOf('\n', (sel.start - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val newText = text.substring(0, lineStart) + linePrefix + text.substring(lineStart)
        updateBodyWithUndo(TextFieldValue(newText, selection = TextRange(sel.start + linePrefix.length)))
    }

    val handleBack = {
        // Discard note if completely blank upon exit
        if (title.isBlank() && bodyValue.text.isBlank() && activeNoteId != null) {
            coroutineScope.launch {
                textNoteRepository.purgeTextNotePermanently(activeNoteId!!)
            }
        }
        onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenNavy)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Header
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (activeNoteId == null) "New Text Note" else "Edit Text Note",
                        color = TabCream,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (isAutosaving) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Saving…",
                            color = AccentGold,
                            fontSize = 11.sp
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = handleBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TabCream
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = {
                        val currentId = activeNoteId ?: 0L
                        val note = TextNote(
                            id = currentId,
                            folderId = folderId,
                            subfolderId = subfolderId,
                            title = title.ifBlank { "Untitled Note" },
                            bodyMarkdown = bodyValue.text,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        onShare(note)
                    },
                    enabled = title.isNotBlank() || bodyValue.text.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Note",
                        tint = if (title.isNotBlank() || bodyValue.text.isNotBlank()) TabCream else TextMuted
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = ScreenNavy)
        )

        // Title and Body Editors
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Title Field
            BasicTextField(
                value = title,
                onValueChange = {
                    title = it
                    scheduleAutosave(it, bodyValue.text)
                },
                textStyle = TextStyle(
                    color = TabCream,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                ),
                cursorBrush = SolidColor(AccentGold),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (title.isEmpty()) {
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

            Spacer(modifier = Modifier.height(16.dp))

            // Markdown Body Field
            BasicTextField(
                value = bodyValue,
                onValueChange = { updateBodyWithUndo(it) },
                textStyle = TextStyle(
                    color = TabCream,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    fontFamily = FontFamily.Default
                ),
                cursorBrush = SolidColor(AccentGold),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (bodyValue.text.isEmpty()) {
                            Text(
                                text = "Start writing with markdown formatting (e.g. **bold**, *italic*, # heading)…",
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

        // Anchored Keyboard Formatting Toolbar
        Surface(
            modifier = Modifier
                .fillMaxWidth(),
            color = ToolbarBg,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Undo
                ToolbarIconButton(
                    icon = Icons.AutoMirrored.Filled.Undo,
                    description = "Undo",
                    enabled = undoStack.isNotEmpty()
                ) {
                    if (undoStack.isNotEmpty()) {
                        redoStack.add(bodyValue)
                        val prev = undoStack.removeAt(undoStack.lastIndex)
                        bodyValue = prev
                        scheduleAutosave(title, prev.text)
                    }
                }

                // Redo
                ToolbarIconButton(
                    icon = Icons.AutoMirrored.Filled.Redo,
                    description = "Redo",
                    enabled = redoStack.isNotEmpty()
                ) {
                    if (redoStack.isNotEmpty()) {
                        undoStack.add(bodyValue)
                        val next = redoStack.removeAt(redoStack.lastIndex)
                        bodyValue = next
                        scheduleAutosave(title, next.text)
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Bold
                ToolbarIconButton(icon = Icons.Default.FormatBold, description = "Bold") {
                    applyWrapFormatting("**", "**")
                }

                // Italic
                ToolbarIconButton(icon = Icons.Default.FormatItalic, description = "Italic") {
                    applyWrapFormatting("*", "*")
                }

                // Strikethrough
                ToolbarIconButton(icon = Icons.Default.FormatStrikethrough, description = "Strike") {
                    applyWrapFormatting("~~", "~~")
                }

                // Headings
                ToolbarTextButton(label = "H1") { applyLinePrefix("# ") }
                ToolbarTextButton(label = "H2") { applyLinePrefix("## ") }
                ToolbarTextButton(label = "H3") { applyLinePrefix("### ") }

                // Blockquote
                ToolbarIconButton(icon = Icons.Default.FormatQuote, description = "Quote") {
                    applyLinePrefix("> ")
                }

                // Bullet List
                ToolbarIconButton(icon = Icons.AutoMirrored.Filled.FormatListBulleted, description = "Bullet List") {
                    applyLinePrefix("- ")
                }

                // Numbered List
                ToolbarIconButton(icon = Icons.Default.FormatListNumbered, description = "Numbered List") {
                    applyLinePrefix("1. ")
                }

                // Checklist
                ToolbarIconButton(icon = Icons.Default.CheckBox, description = "Checklist") {
                    applyLinePrefix("- [ ] ")
                }

                // Inline Code
                ToolbarIconButton(icon = Icons.Default.Code, description = "Code") {
                    applyWrapFormatting("`", "`")
                }

                // Code Block
                ToolbarTextButton(label = "</>") {
                    applyWrapFormatting("\n```\n", "\n```\n")
                }

                // Horizontal Rule
                ToolbarIconButton(icon = Icons.Default.HorizontalRule, description = "Divider") {
                    val sel = bodyValue.selection
                    val text = bodyValue.text
                    val newText = text.substring(0, sel.start) + "\n---\n" + text.substring(sel.end)
                    updateBodyWithUndo(TextFieldValue(newText, selection = TextRange(sel.start + 5)))
                }

                // Link
                ToolbarIconButton(icon = Icons.Default.Link, description = "Link") {
                    applyWrapFormatting("[", "](https://)")
                }
            }
        }
    }
}

@Composable
private fun ToolbarIconButton(
    icon: ImageVector,
    description: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(38.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (enabled) TabCream else TextMuted.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ToolbarTextButton(
    label: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(38.dp)
    ) {
        Text(
            text = label,
            color = TabCream,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
