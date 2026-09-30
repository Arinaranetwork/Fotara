// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.note

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatClear
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Link
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.data.model.TextNote
import com.arinara.fotara.data.repository.TextNoteRepository
import com.arinara.fotara.ui.components.RichMarkdownColumn
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val ScreenNavy = Color(0xFF03071E)
private val TabCream = Color(0xFFEAE3D2)
private val AccentGold = Color(0xFFF77F00)
private val CardBg = Color(0xFF141936)
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

    // Preview mode and Search/Replace bar toggles
    var isPreviewMode by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }

    // Link insertion modal
    var showLinkDialog by remember { mutableStateOf(false) }
    var linkLabel by remember { mutableStateOf("") }
    var linkTargetUrl by remember { mutableStateOf("https://") }

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

    // Interactive selection-wrap & unwrap toggle
    fun applyWrapFormatting(prefix: String, suffix: String) {
        val sel = bodyValue.selection
        val text = bodyValue.text
        val start = sel.min.coerceIn(0, text.length)
        val end = sel.max.coerceIn(0, text.length)

        if (start < end) {
            val selectedText = text.substring(start, end)
            // If already wrapped internally: unwrap
            if (selectedText.startsWith(prefix) && selectedText.endsWith(suffix) && selectedText.length >= prefix.length + suffix.length) {
                val unwrapped = selectedText.substring(prefix.length, selectedText.length - suffix.length)
                val newText = text.substring(0, start) + unwrapped + text.substring(end)
                updateBodyWithUndo(TextFieldValue(newText, selection = TextRange(start, start + unwrapped.length)))
                return
            }
            // If text immediately outside selection is wrapped: unwrap
            if (start >= prefix.length && end + suffix.length <= text.length) {
                val before = text.substring(start - prefix.length, start)
                val after = text.substring(end, end + suffix.length)
                if (before == prefix && after == suffix) {
                    val newText = text.substring(0, start - prefix.length) + selectedText + text.substring(end + suffix.length)
                    updateBodyWithUndo(TextFieldValue(newText, selection = TextRange(start - prefix.length, end - prefix.length)))
                    return
                }
            }
            // Otherwise, wrap selection
            val newText = text.substring(0, start) + prefix + selectedText + suffix + text.substring(end)
            updateBodyWithUndo(TextFieldValue(newText, selection = TextRange(start + prefix.length, end + prefix.length)))
        } else {
            // Collapsed cursor
            if (start >= prefix.length && start + suffix.length <= text.length) {
                val before = text.substring(start - prefix.length, start)
                val after = text.substring(start, start + suffix.length)
                if (before == prefix && after == suffix) {
                    val newText = text.substring(0, start - prefix.length) + text.substring(start + suffix.length)
                    updateBodyWithUndo(TextFieldValue(newText, selection = TextRange(start - prefix.length)))
                    return
                }
            }
            val newText = text.substring(0, start) + prefix + suffix + text.substring(start)
            updateBodyWithUndo(TextFieldValue(newText, selection = TextRange(start + prefix.length)))
        }
    }

    // Line prefix formatting (Headings, Lists, Blockquotes, Checklists)
    fun applyLinePrefix(linePrefix: String) {
        val sel = bodyValue.selection
        val text = bodyValue.text
        val cursor = sel.min.coerceIn(0, text.length)
        val lineStart = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', cursor).let { if (it == -1) text.length else it }
        val currentLine = text.substring(lineStart, lineEnd)

        // Check if the current line already starts with linePrefix
        if (currentLine.startsWith(linePrefix)) {
            // Toggle off: remove prefix
            val newText = text.substring(0, lineStart) + currentLine.removePrefix(linePrefix) + text.substring(lineEnd)
            val newCursor = (cursor - linePrefix.length).coerceAtLeast(lineStart)
            updateBodyWithUndo(TextFieldValue(newText, selection = TextRange(newCursor)))
            return
        }

        // Strip existing heading or list prefix if replacing
        val headingRegex = Regex("""^#{1,3}\s+""")
        val listRegex = Regex("""^([-*+]\s+|1\.\s+|-\s*\[[ xX]\]\s+|>\s+)""")

        var strippedLine = currentLine
        var removedLen = 0
        if (linePrefix.startsWith("#")) {
            val m = headingRegex.find(currentLine)
            if (m != null) {
                strippedLine = currentLine.substring(m.range.last + 1)
                removedLen = m.value.length
            }
        } else if (linePrefix.startsWith("-") || linePrefix.startsWith("1.") || linePrefix.startsWith(">")) {
            val m = listRegex.find(currentLine)
            if (m != null) {
                strippedLine = currentLine.substring(m.range.last + 1)
                removedLen = m.value.length
            }
        }

        val newLine = linePrefix + strippedLine
        val newText = text.substring(0, lineStart) + newLine + text.substring(lineEnd)
        val newCursor = (cursor - removedLen + linePrefix.length).coerceIn(lineStart, lineStart + newLine.length)
        updateBodyWithUndo(TextFieldValue(newText, selection = TextRange(newCursor)))
    }

    fun clearFormattingOnSelection() {
        val sel = bodyValue.selection
        val text = bodyValue.text
        val start = sel.min.coerceIn(0, text.length)
        val end = sel.max.coerceIn(0, text.length)

        if (start < end) {
            val selected = text.substring(start, end)
            val cleaned = TextNote.stripMarkdownFormatting(selected)
            val newText = text.substring(0, start) + cleaned + text.substring(end)
            updateBodyWithUndo(TextFieldValue(newText, selection = TextRange(start, start + cleaned.length)))
        } else {
            val lineStart = text.lastIndexOf('\n', (start - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
            val lineEnd = text.indexOf('\n', start).let { if (it == -1) text.length else it }
            val line = text.substring(lineStart, lineEnd)
            val cleaned = TextNote.stripMarkdownFormatting(line)
            val newText = text.substring(0, lineStart) + cleaned + text.substring(lineEnd)
            updateBodyWithUndo(TextFieldValue(newText, selection = TextRange(lineStart + cleaned.length)))
        }
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

    // Word and character counters
    val wordCount = remember(bodyValue.text) {
        val trimmed = bodyValue.text.trim()
        if (trimmed.isEmpty()) 0 else trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }.size
    }
    val charCount = bodyValue.text.length

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
                    Text(
                        text = "$wordCount words • $charCount characters",
                        color = TabCream.copy(alpha = 0.6f),
                        fontSize = 11.5.sp
                    )
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
                // Find & Replace toggle
                IconButton(onClick = { showSearchBar = !showSearchBar }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Find and Replace",
                        tint = if (showSearchBar) AccentGold else TabCream
                    )
                }

                // Preview toggle
                IconButton(onClick = { isPreviewMode = !isPreviewMode }) {
                    Icon(
                        imageVector = if (isPreviewMode) Icons.Default.EditNote else Icons.Default.Visibility,
                        contentDescription = if (isPreviewMode) "Edit Mode" else "Preview Mode",
                        tint = if (isPreviewMode) AccentGold else TabCream
                    )
                }

                // Share Note
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

        // Find & Replace Bar
        AnimatedVisibility(
            visible = showSearchBar,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                color = CardBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            textStyle = TextStyle(color = TabCream, fontSize = 13.5.sp),
                            cursorBrush = SolidColor(AccentGold),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .background(ScreenNavy, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            decorationBox = { inner ->
                                if (searchQuery.isEmpty()) {
                                    Text("Find in note…", color = TextMuted, fontSize = 13.5.sp)
                                }
                                inner()
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = replaceQuery,
                            onValueChange = { replaceQuery = it },
                            textStyle = TextStyle(color = TabCream, fontSize = 13.5.sp),
                            cursorBrush = SolidColor(AccentGold),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .background(ScreenNavy, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            decorationBox = { inner ->
                                if (replaceQuery.isEmpty()) {
                                    Text("Replace with…", color = TextMuted, fontSize = 13.5.sp)
                                }
                                inner()
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (searchQuery.isNotEmpty()) {
                                    val newText = bodyValue.text.replace(searchQuery, replaceQuery)
                                    updateBodyWithUndo(TextFieldValue(newText))
                                }
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.FindReplace, contentDescription = "Replace All", tint = AccentGold)
                        }
                    }
                }
            }
        }

        // Main Editor or Markdown Preview
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
                    text = title.ifBlank { "Untitled Note" },
                    color = TabCream,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                RichMarkdownColumn(
                    markdown = bodyValue.text.ifBlank { "*This note is empty. Switch to edit mode to start typing.*" },
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

            // Anchored Keyboard Formatting Toolbar
            Surface(
                modifier = Modifier.fillMaxWidth(),
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

                    // Inline Code
                    ToolbarIconButton(icon = Icons.Default.Code, description = "Inline Code") {
                        applyWrapFormatting("`", "`")
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

                    // Code Block
                    ToolbarTextButton(label = "</>") {
                        applyWrapFormatting("\n```\n", "\n```\n")
                    }

                    // Horizontal Rule
                    ToolbarIconButton(icon = Icons.Default.HorizontalRule, description = "Divider") {
                        val sel = bodyValue.selection
                        val text = bodyValue.text
                        val start = sel.min.coerceIn(0, text.length)
                        val end = sel.max.coerceIn(0, text.length)
                        val newText = text.substring(0, start) + "\n---\n" + text.substring(end)
                        updateBodyWithUndo(TextFieldValue(newText, selection = TextRange(start + 5)))
                    }

                    // Link Dialog Trigger
                    ToolbarIconButton(icon = Icons.Default.Link, description = "Insert Link") {
                        val sel = bodyValue.selection
                        val text = bodyValue.text
                        val start = sel.min.coerceIn(0, text.length)
                        val end = sel.max.coerceIn(0, text.length)
                        linkLabel = if (start < end) text.substring(start, end) else ""
                        linkTargetUrl = "https://"
                        showLinkDialog = true
                    }

                    // Clear Formatting
                    ToolbarIconButton(icon = Icons.Default.FormatClear, description = "Clear Formatting") {
                        clearFormattingOnSelection()
                    }
                }
            }
        }
    }

    // Link Insertion Dialog
    if (showLinkDialog) {
        AlertDialog(
            onDismissRequest = { showLinkDialog = false },
            title = {
                Text(
                    text = "Insert Link",
                    fontWeight = FontWeight.Bold,
                    color = TabCream
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = linkLabel,
                        onValueChange = { linkLabel = it },
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
                        value = linkTargetUrl,
                        onValueChange = { linkTargetUrl = it },
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
                    onClick = {
                        val text = linkLabel.ifBlank { "link" }
                        val url = linkTargetUrl.trim().ifBlank { "https://" }
                        val markdownLink = "[$text]($url)"
                        val sel = bodyValue.selection
                        val oldText = bodyValue.text
                        val start = sel.min.coerceIn(0, oldText.length)
                        val end = sel.max.coerceIn(0, oldText.length)
                        val newText = oldText.substring(0, start) + markdownLink + oldText.substring(end)
                        updateBodyWithUndo(TextFieldValue(newText, selection = TextRange(start + markdownLink.length)))
                        showLinkDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold)
                ) {
                    Text("Insert", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLinkDialog = false }) {
                    Text("Cancel", color = TabCream)
                }
            },
            containerColor = CardBg,
            shape = RoundedCornerShape(16.dp)
        )
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
