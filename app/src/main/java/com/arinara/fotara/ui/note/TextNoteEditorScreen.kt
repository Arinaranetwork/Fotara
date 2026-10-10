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
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.R
import com.arinara.fotara.data.model.TextNote
import com.arinara.fotara.data.repository.TextNoteRepository
import com.arinara.fotara.ui.components.NoteDetailScheduleChip
import com.arinara.fotara.ui.components.RichMarkdownColumn
import com.arinara.fotara.ui.components.ScheduleNoteDialog
import com.arinara.fotara.ui.note.editor.EditorActions
import com.arinara.fotara.ui.note.editor.EditorChangeHandler
import com.arinara.fotara.ui.note.editor.EditorToolbar
import com.arinara.fotara.ui.note.editor.LineToolType
import com.arinara.fotara.ui.note.editor.MarkdownVisualTransformation
import com.arinara.fotara.ui.note.editor.TextEditorOps
import com.arinara.fotara.ui.note.editor.rememberEditorState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import com.arinara.fotara.theme.TagAmber
import kotlinx.coroutines.delay
import com.arinara.fotara.util.NoteScheduleManager
import com.arinara.fotara.util.ScheduleAlertType
import com.arinara.fotara.util.ScheduleNoteType
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
    onShare: (TextNote) -> Unit,
    highlightQuery: String? = null,
    audioAnnotationRepository: com.arinara.fotara.audio.repository.AudioAnnotationRepository? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val state = rememberEditorState(
        initialNoteId = noteId,
        folderId = folderId,
        subfolderId = subfolderId,
        textNoteRepository = textNoteRepository,
        coroutineScope = coroutineScope
    )

    val audioRepo = remember(audioAnnotationRepository, context) {
        audioAnnotationRepository ?: com.arinara.fotara.audio.repository.SqliteAudioAnnotationRepository(com.arinara.fotara.data.db.FotaraDbHelper(context))
    }
    val audioRecorderManager = remember(context) { com.arinara.fotara.audio.recorder.AudioRecorderManager(context) }
    val audioPlayerManager = remember { com.arinara.fotara.audio.player.AudioPlayerManager() }
    val audioViewModel: com.arinara.fotara.audio.ui.AudioAnnotationViewModel = viewModel(
        key = "audio_note_${state.noteId ?: 0L}",
        factory = com.arinara.fotara.audio.ui.AudioAnnotationViewModelFactory(
            repository = audioRepo,
            recorderManager = audioRecorderManager,
            playerManager = audioPlayerManager,
            noteId = state.noteId
        )
    )
    val audioUiState by audioViewModel.uiState.collectAsState()
    var showAudioDock by remember { mutableStateOf(false) }

    val activeMathSpan = remember(state.bodyValue.text, state.bodyValue.selection) {
        val cursor = state.bodyValue.selection.start
        val doc = com.arinara.fotara.ui.note.editor.MarkdownParser.parse(state.bodyValue.text)
        doc.spans.firstOrNull { span ->
            (span.type == com.arinara.fotara.ui.note.editor.MarkdownSpanType.MATH_INLINE ||
             span.type == com.arinara.fotara.ui.note.editor.MarkdownSpanType.MATH_BLOCK) &&
            cursor in (span.start - 1)..(span.end + 1)
        }
    }

    val rawMathContent = remember(activeMathSpan, state.bodyValue.text) {
        activeMathSpan?.let { span ->
            if (span.contentStart in 0..state.bodyValue.text.length &&
                span.contentEnd in 0..state.bodyValue.text.length &&
                span.contentStart <= span.contentEnd
            ) {
                state.bodyValue.text.substring(span.contentStart, span.contentEnd).trim()
            } else ""
        } ?: ""
    }

    var isPreviewMode by remember { mutableStateOf(false) }
    var activeHighlightQuery by remember { mutableStateOf(highlightQuery) }
    val searchHighlightAlpha = remember { Animatable(0f) }
    var hasHandledInitialHighlight by remember { mutableStateOf(false) }
    val editorScrollState = rememberScrollState()

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
        // Top App Bar - Single line, centered, no subtitle clutter
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (state.noteId == null) stringResource(R.string.editor_title_new) else stringResource(R.string.editor_title_edit),
                        color = TabCream,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (state.isSaving) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.editor_saving),
                            color = AccentGold,
                            fontSize = 11.sp
                        )
                    }
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

                IconButton(
                    onClick = { showAudioDock = !showAudioDock }
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Outlined.Mic,
                        contentDescription = "Voice Annotations",
                        tint = if (audioUiState.annotations.isNotEmpty() || audioUiState.recorderState is com.arinara.fotara.audio.recorder.AudioRecorderState.Recording) AccentGold else TabCream
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

                // Overflow menu for note actions (Info section + Schedule)
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
                        // Info Section matching Viewer Pattern
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.viewer_info_header),
                                color = TextMuted.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.editor_info_counts, state.wordCount, state.charCount),
                                color = TabCream,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.viewer_info_offline),
                                color = TextMuted.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }
                        HorizontalDivider(
                            color = ToolbarBorder,
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

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
                                    Text(stringResource(R.string.editor_find_in_note), color = TextMuted, fontSize = 13.5.sp)
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
                                    Text(stringResource(R.string.editor_replace_with), color = TextMuted, fontSize = 13.5.sp)
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
                                if (state.searchMatchCount > 0) stringResource(R.string.editor_matches_count, state.currentMatchIndex + 1, state.searchMatchCount) else stringResource(R.string.editor_no_matches)
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
                            Text(stringResource(R.string.editor_replace_one), color = AccentGold, fontSize = 12.sp)
                        }

                        // Replace All
                        TextButton(
                            onClick = { state.replaceAll() },
                            enabled = state.searchMatchCount > 0
                        ) {
                            Text(stringResource(R.string.editor_replace_all), color = AccentGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                    markdown = state.bodyValue.text.ifBlank { stringResource(R.string.editor_empty_preview) },
                    primaryTextColor = TabCream,
                    accentColor = AccentGold,
                    cardBg = CardBg,
                    cardBorder = ToolbarBorder,
                    onToggleChecklistLine = { lineIndex ->
                        state.toggleChecklistAtLine(lineIndex)
                    }
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(editorScrollState)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Title Input
                BasicTextField(
                    value = state.title,
                    onValueChange = {
                        if (activeHighlightQuery != null || searchHighlightAlpha.value > 0f) {
                            activeHighlightQuery = null
                            coroutineScope.launch { searchHighlightAlpha.snapTo(0f) }
                        }
                        state.onTitleChange(it)
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
                            if (state.title.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.editor_placeholder_title),
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

                var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

                LaunchedEffect(activeHighlightQuery, textLayoutResult) {
                    val query = activeHighlightQuery
                    val layout = textLayoutResult
                    if (!hasHandledInitialHighlight && !query.isNullOrBlank() && layout != null) {
                        hasHandledInitialHighlight = true
                        val doc = state.bodyValue.text
                        val matchIndex = doc.indexOf(query, ignoreCase = true)
                        if (matchIndex != -1) {
                            val tStart = visualTransformation.lastOffsetMapping.originalToTransformed(matchIndex)
                            val line = layout.getLineForOffset(tStart.coerceIn(0, (layout.layoutInput.text.length - 1).coerceAtLeast(0)))
                            val lineTop = layout.getLineTop(line)
                            editorScrollState.animateScrollTo(lineTop.toInt())
                            searchHighlightAlpha.snapTo(0.35f)
                            delay(3000L)
                            searchHighlightAlpha.animateTo(0f, tween(1000, easing = FastOutSlowInEasing))
                            activeHighlightQuery = null
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .pointerInput(state.bodyValue.text) {
                            detectTapGestures(
                                onTap = { offset ->
                                    if (activeHighlightQuery != null || searchHighlightAlpha.value > 0f) {
                                        activeHighlightQuery = null
                                        coroutineScope.launch { searchHighlightAlpha.snapTo(0f) }
                                    }
                                    val layout = textLayoutResult ?: return@detectTapGestures
                                    val line = layout.getLineForVerticalPosition(offset.y)
                                    if (line in 0 until layout.lineCount) {
                                        val tLineStart = layout.getLineStart(line)
                                        val origOffset = visualTransformation.lastOffsetMapping.transformedToOriginal(tLineStart)
                                        val doc = state.bodyValue.text
                                        val lineStart = if (origOffset == 0) 0 else {
                                            val p = doc.lastIndexOf('\n', origOffset - 1)
                                            if (p == -1) 0 else p + 1
                                        }
                                        val lineEnd = doc.indexOf('\n', origOffset).let { if (it == -1) doc.length else it }
                                        val lineText = doc.substring(lineStart, lineEnd)
                                        val parsed = TextEditorOps.parseLine(0, lineStart, lineEnd, lineText)

                                        if (parsed.prefixType == LineToolType.CHECKBOX) {
                                            val tPrefixStart = visualTransformation.lastOffsetMapping.originalToTransformed(lineStart + parsed.indent.length)
                                            val boxLeft = layout.getHorizontalPosition(tPrefixStart, true)
                                            val hitLeft = boxLeft - 10.dp.toPx()
                                            val hitRight = boxLeft + 38.dp.toPx()
                                            if (offset.x in hitLeft..hitRight) {
                                                state.toggleChecklistAtOffset(origOffset)
                                                return@detectTapGestures
                                            }
                                        } else if (lineText.trim() == "---" || lineText.trim() == "***") {
                                            // Divider: tapping selects it (visible highlight)
                                            val selStart = if (lineEnd >= doc.length && lineStart > 0) lineStart - 1 else lineStart
                                            val selEnd = if (lineEnd < doc.length) lineEnd + 1 else lineEnd
                                            state.updateSelection(TextRange(selStart, selEnd))
                                            return@detectTapGestures
                                        }
                                    }
                                },
                                onLongPress = { offset ->
                                    val layout = textLayoutResult ?: return@detectTapGestures
                                    val line = layout.getLineForVerticalPosition(offset.y)
                                    if (line in 0 until layout.lineCount) {
                                        val tLineStart = layout.getLineStart(line)
                                        val origOffset = visualTransformation.lastOffsetMapping.transformedToOriginal(tLineStart)
                                        val doc = state.bodyValue.text
                                        val lineStart = if (origOffset == 0) 0 else {
                                            val p = doc.lastIndexOf('\n', origOffset - 1)
                                            if (p == -1) 0 else p + 1
                                        }
                                        val lineEnd = doc.indexOf('\n', origOffset).let { if (it == -1) doc.length else it }
                                        val lineText = doc.substring(lineStart, lineEnd)
                                        val parsed = TextEditorOps.parseLine(0, lineStart, lineEnd, lineText)

                                        // Long press on bullet, number, or quote marker selects the whole block line
                                        if (parsed.prefixType in listOf(LineToolType.BULLET_LIST, LineToolType.NUMBERED_LIST, LineToolType.QUOTE)) {
                                            val tPrefixStart = visualTransformation.lastOffsetMapping.originalToTransformed(lineStart + parsed.indent.length)
                                            val markerLeft = layout.getHorizontalPosition(tPrefixStart, true)
                                            if (offset.x in (markerLeft - 10.dp.toPx())..(markerLeft + 36.dp.toPx())) {
                                                val selStart = if (lineEnd >= doc.length && lineStart > 0) lineStart - 1 else lineStart
                                                val selEnd = if (lineEnd < doc.length) lineEnd + 1 else lineEnd
                                                state.updateSelection(TextRange(selStart, selEnd))
                                            }
                                        }
                                    }
                                }
                            )
                        }
                ) {
                    BasicTextField(
                        value = state.bodyValue,
                        onValueChange = { newBody ->
                            if (activeHighlightQuery != null || searchHighlightAlpha.value > 0f) {
                                activeHighlightQuery = null
                                coroutineScope.launch { searchHighlightAlpha.snapTo(0f) }
                            }
                            val processed = EditorChangeHandler.processChange(state.bodyValue, newBody)
                            state.onBodyChange(processed)
                        },
                        onTextLayout = { textLayoutResult = it },
                        visualTransformation = visualTransformation,
                        textStyle = TextStyle(
                            color = TabCream,
                            fontSize = 15.5.sp,
                            lineHeight = 23.sp,
                            fontFamily = com.arinara.fotara.theme.ElmsSans
                        ),
                        cursorBrush = SolidColor(AccentGold),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Default
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                val layout = textLayoutResult ?: return@drawBehind
                                val doc = state.bodyValue.text

                                // Render transient search highlight overlay (4s fading highlight at 35% alpha)
                                if (searchHighlightAlpha.value > 0.005f) {
                                    val q = highlightQuery
                                    if (!q.isNullOrBlank()) {
                                        var mPos = 0
                                        while (mPos <= doc.length - q.length) {
                                            val found = doc.indexOf(q, mPos, ignoreCase = true)
                                            if (found == -1) break
                                            val tStart = visualTransformation.lastOffsetMapping.originalToTransformed(found)
                                            val tEnd = visualTransformation.lastOffsetMapping.originalToTransformed(found + q.length)
                                            val maxLen = layout.layoutInput.text.length
                                            if (tStart in 0..maxLen && tEnd in 0..maxLen && tStart < tEnd) {
                                                val path = layout.getPathForRange(tStart, tEnd)
                                                drawPath(path, color = TagAmber.copy(alpha = searchHighlightAlpha.value))
                                            }
                                            mPos = found + q.length.coerceAtLeast(1)
                                        }
                                    }
                                }

                                val boxSize = 18.dp.toPx()
                                val cornerRadius = CornerRadius(4.dp.toPx())
                                val strokeWidth = 1.8.dp.toPx()
                                val checkStroke = 2.dp.toPx()

                                var searchPos = 0
                                while (searchPos <= doc.length) {
                                    val nextNl = doc.indexOf('\n', searchPos).let { if (it == -1) doc.length else it }
                                    val lineText = doc.substring(searchPos, nextNl)
                                    val parsed = TextEditorOps.parseLine(0, searchPos, nextNl, lineText)

                                    if (parsed.prefixType == LineToolType.CHECKBOX) {
                                        val tPrefixStart = visualTransformation.lastOffsetMapping.originalToTransformed(searchPos + parsed.indent.length)
                                        if (tPrefixStart in 0..layout.layoutInput.text.length) {
                                            val clampedOffset = tPrefixStart.coerceIn(0, (layout.layoutInput.text.length - 1).coerceAtLeast(0))
                                            val visualLine = layout.getLineForOffset(clampedOffset)
                                            val lineTop = layout.getLineTop(visualLine)
                                            val lineBottom = layout.getLineBottom(visualLine)
                                            val boxLeft = layout.getHorizontalPosition(clampedOffset, true) + 2.dp.toPx()
                                            val boxTop = lineTop + (lineBottom - lineTop - boxSize) / 2f

                                            val isChecked = parsed.prefixString.contains(Regex("""\[[xX]\]"""))
                                            if (isChecked) {
                                                drawRoundRect(
                                                    color = AccentGold,
                                                    topLeft = Offset(boxLeft, boxTop),
                                                    size = Size(boxSize, boxSize),
                                                    cornerRadius = cornerRadius,
                                                    style = Fill
                                                )
                                                val path = Path().apply {
                                                    moveTo(boxLeft + boxSize * 0.22f, boxTop + boxSize * 0.52f)
                                                    lineTo(boxLeft + boxSize * 0.42f, boxTop + boxSize * 0.72f)
                                                    lineTo(boxLeft + boxSize * 0.78f, boxTop + boxSize * 0.28f)
                                                }
                                                drawPath(
                                                    path = path,
                                                    color = ScreenNavy,
                                                    style = Stroke(width = checkStroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                                )
                                            } else {
                                                drawRoundRect(
                                                    color = AccentGold,
                                                    topLeft = Offset(boxLeft, boxTop),
                                                    size = Size(boxSize, boxSize),
                                                    cornerRadius = cornerRadius,
                                                    style = Stroke(width = strokeWidth)
                                                )
                                            }
                                        }
                                    } else if (parsed.prefixType == LineToolType.QUOTE) {
                                        val tPrefixStart = visualTransformation.lastOffsetMapping.originalToTransformed(searchPos + parsed.indent.length)
                                        if (tPrefixStart in 0..layout.layoutInput.text.length) {
                                            val clampedOffset = tPrefixStart.coerceIn(0, (layout.layoutInput.text.length - 1).coerceAtLeast(0))
                                            val visualLine = layout.getLineForOffset(clampedOffset)
                                            val lineTop = layout.getLineTop(visualLine)
                                            val lineBottom = layout.getLineBottom(visualLine)
                                            val barLeft = layout.getHorizontalPosition(clampedOffset, true)
                                            drawLine(
                                                color = AccentGold,
                                                start = Offset(barLeft + 2.dp.toPx(), lineTop + 2.dp.toPx()),
                                                end = Offset(barLeft + 2.dp.toPx(), lineBottom - 2.dp.toPx()),
                                                strokeWidth = 3.dp.toPx()
                                            )
                                        }
                                    }

                                    if (nextNl >= doc.length) break
                                    searchPos = nextNl + 1
                                }
                            },
                        decorationBox = { innerTextField ->
                            Box(modifier = Modifier.fillMaxWidth()) {
                                if (state.bodyValue.text.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.editor_placeholder_body),
                                        color = TextMuted.copy(alpha = 0.7f),
                                        fontSize = 15.sp,
                                        lineHeight = 22.sp
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Live Floating Math Preview Card (updates in real-time when cursor is inside or adjacent to LaTeX)
            AnimatedVisibility(
                visible = activeMathSpan != null && rawMathContent.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    color = CardBg,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ToolbarBorder),
                    shadowElevation = 6.dp
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF2563EB).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "fx",
                                    color = Color(0xFF64B5F6),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Serif
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (activeMathSpan?.type == com.arinara.fotara.ui.note.editor.MarkdownSpanType.MATH_BLOCK) {
                                    "Display Math Preview"
                                } else {
                                    "Inline Math Preview"
                                },
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        val mathAnnotated = remember(rawMathContent, activeMathSpan?.type) {
                            com.arinara.fotara.ui.note.editor.KatexMathRenderer.buildMathAnnotatedString(
                                rawLatex = rawMathContent,
                                mathColor = TabCream,
                                isBlock = (activeMathSpan?.type == com.arinara.fotara.ui.note.editor.MarkdownSpanType.MATH_BLOCK)
                            )
                        }
                        Text(
                            text = mathAnnotated,
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = if (activeMathSpan?.type == com.arinara.fotara.ui.note.editor.MarkdownSpanType.MATH_BLOCK) 16.sp else 14.5.sp,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            // Voice / Audio Annotations Dock
            AnimatedVisibility(
                visible = showAudioDock || audioUiState.annotations.isNotEmpty() || audioUiState.recorderState is com.arinara.fotara.audio.recorder.AudioRecorderState.Recording,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    color = CardBg,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ToolbarBorder),
                    shadowElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Voice Annotations (${audioUiState.annotations.size})",
                                color = TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            IconButton(
                                onClick = { showAudioDock = false },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close Audio Dock", tint = TextMuted, modifier = Modifier.size(16.dp))
                            }
                        }

                        com.arinara.fotara.audio.ui.AudioRecordPill(
                            recorderState = audioUiState.recorderState,
                            onStartRecording = {
                                coroutineScope.launch {
                                    if (state.noteId == null) {
                                        state.flushAutosaveNow()
                                    }
                                    audioViewModel.startRecording()
                                }
                            },
                            onStopRecording = { audioViewModel.stopAndSaveRecording() },
                            onCancelRecording = { audioViewModel.cancelRecording() }
                        )

                        audioUiState.annotations.forEach { annotation ->
                            com.arinara.fotara.audio.ui.AudioPlaybackBar(
                                annotation = annotation,
                                playerState = audioUiState.playerState,
                                onPlay = { audioViewModel.playAnnotation(annotation) },
                                onPause = { audioViewModel.pausePlayback() },
                                onResume = { audioViewModel.resumePlayback() },
                                onSeek = { audioViewModel.seekPlayback(it) },
                                onDelete = { audioViewModel.deleteAnnotation(annotation.id) }
                            )
                        }
                    }
                }
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
                    text = stringResource(R.string.editor_dialog_link_title),
                    fontWeight = FontWeight.Bold,
                    color = TabCream
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = state.linkDialogLabel,
                        onValueChange = { state.linkDialogLabel = it },
                        label = { Text(stringResource(R.string.editor_dialog_link_label), color = TextMuted) },
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
                        label = { Text(stringResource(R.string.editor_dialog_link_url), color = TextMuted) },
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
                    Text(stringResource(R.string.editor_dialog_link_apply), color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { state.showLinkDialog = false }) {
                    Text(stringResource(R.string.editor_dialog_link_cancel), color = TabCream)
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
