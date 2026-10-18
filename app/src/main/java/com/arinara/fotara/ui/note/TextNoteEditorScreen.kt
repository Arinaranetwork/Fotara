// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.note

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import android.net.Uri
import java.io.File
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalDensity
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
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.theme.TagAmber
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.delay
import com.arinara.fotara.util.NoteScheduleManager
import com.arinara.fotara.util.ScheduleAlertType
import com.arinara.fotara.util.ScheduleNoteType
import com.arinara.fotara.ui.components.presets.AppScreen
import com.arinara.fotara.ui.components.presets.AppTopBar
import com.arinara.fotara.ui.components.presets.AppCard
import com.arinara.fotara.ui.components.presets.AppButton
import com.arinara.fotara.ui.components.presets.AppButtonVariant
import com.arinara.fotara.ui.components.presets.AppBottomSheet
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import kotlinx.coroutines.launch

private val ScreenNavy = ElectricSlateTokens.BgCanvas
private val TabCream = ElectricSlateTokens.TextPrimary
private val AccentGold = ElectricSlateTokens.PrimaryAccent
private val CardBg = ElectricSlateTokens.SurfacePanel
private val ToolbarBorder = ElectricSlateTokens.BorderSubtle
private val TextMuted = ElectricSlateTokens.TextMuted

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
    audioAnnotationRepository: com.arinara.fotara.audio.repository.AudioAnnotationRepository? = null,
    documentRepository: com.arinara.fotara.data.repository.DocumentRepository? = null,
    onOpenDocument: ((docId: Long, isDocx: Boolean) -> Unit)? = null
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

    val isLatexInstalled = true

    var isPreviewMode by remember { mutableStateOf(false) }
    var activeHighlightQuery by remember { mutableStateOf(highlightQuery) }
    val searchHighlightAlpha = remember { Animatable(0f) }
    var hasHandledInitialHighlight by remember { mutableStateOf(false) }
    val editorScrollState = rememberScrollState()

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showAudioDock = true
            coroutineScope.launch {
                if (state.noteId == null) {
                    state.flushAutosaveNow()
                }
                audioViewModel.startRecording()
            }
        } else {
            Toast.makeText(context, "Microphone permission is required to record audio notes", Toast.LENGTH_SHORT).show()
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                try {
                    val fileName = try {
                        context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (cursor.moveToFirst() && nameIndex != -1) cursor.getString(nameIndex) else null
                        }
                    } catch (_: Exception) { null } ?: "Document_${System.currentTimeMillis()}"
                    val isPdf = fileName.lowercase().endsWith(".pdf") || (context.contentResolver.getType(it)?.contains("pdf") == true)
                    val isDocx = fileName.lowercase().endsWith(".docx") || fileName.lowercase().endsWith(".doc")
                    val docId = if (isPdf) {
                        documentRepository?.importPdf(it, folderId, subfolderId, fileName) ?: System.currentTimeMillis()
                    } else {
                        documentRepository?.importDocx(it, folderId, subfolderId, fileName) ?: System.currentTimeMillis()
                    }
                    state.appendDocumentAttachment(fileName, docId)
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to attach document: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                try {
                    val storageManager = com.arinara.fotara.data.storage.PhotoStorageManager(context)
                    val saved = storageManager.saveUriAsPhoto(it)
                    state.appendImageAttachment(saved.filePath)
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to attach image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

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

    AppScreen(
        modifier = Modifier.fillMaxSize(),
        containerColor = ScreenNavy
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
        AppTopBar(
            title = if (state.noteId == null) stringResource(R.string.editor_title_new) else stringResource(R.string.editor_title_edit),
            subtitle = if (state.isSaving) stringResource(R.string.editor_saving) else null,
            onBackClick = { handleExit() },
            containerColor = ScreenNavy,
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
                    onClick = {
                        showAudioDock = true
                        val isCurrentlyRecording = audioUiState.recorderState is com.arinara.fotara.audio.recorder.AudioRecorderState.Recording
                        if (!isCurrentlyRecording) {
                            val hasMic = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasMic) {
                                coroutineScope.launch {
                                    if (state.noteId == null) {
                                        state.flushAutosaveNow()
                                    }
                                    audioViewModel.startRecording()
                                }
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    }
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
                            .border(1.dp, ToolbarBorder, RoundedCornerShape(12.dp))
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
                                fontSize = 12.sp
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
            }
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
                                .background(ScreenNavy, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            decorationBox = { inner ->
                                if (state.searchQuery.isEmpty()) {
                                    Text(stringResource(R.string.editor_find_in_note), color = TextMuted, fontSize = 14.sp)
                                }
                                inner()
                            }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        BasicTextField(
                            value = state.replaceQuery,
                            onValueChange = { state.replaceQuery = it },
                            textStyle = TextStyle(color = TabCream, fontSize = 14.sp),
                            cursorBrush = SolidColor(AccentGold),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .background(ScreenNavy, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            decorationBox = { inner ->
                                if (state.replaceQuery.isEmpty()) {
                                    Text(stringResource(R.string.editor_replace_with), color = TextMuted, fontSize = 14.sp)
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
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Case sensitive toggle
                        AppButton(
                            text = "Aa",
                            onClick = {
                                state.caseSensitive = !state.caseSensitive
                                state.onSearchQueryChange(state.searchQuery)
                            },
                            variant = if (state.caseSensitive) AppButtonVariant.PRIMARY else AppButtonVariant.SECONDARY,
                            height = 32.dp
                        )

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
                        AppButton(
                            text = stringResource(R.string.editor_replace_one),
                            onClick = { state.replaceCurrent() },
                            enabled = state.searchMatchCount > 0,
                            variant = AppButtonVariant.SECONDARY,
                            height = 32.dp
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        // Replace All
                        AppButton(
                            text = stringResource(R.string.editor_replace_all),
                            onClick = { state.replaceAll() },
                            enabled = state.searchMatchCount > 0,
                            variant = AppButtonVariant.PRIMARY,
                            height = 32.dp
                        )

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
                    .verticalScroll(editorScrollState)
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
                    },
                    onOpenDocument = onOpenDocument
                )
                Spacer(modifier = Modifier.height(20.dp))
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
                val visualTransformation = remember(state.bodyValue.selection, isLatexInstalled) {
                    MarkdownVisualTransformation(
                        cursorStart = state.bodyValue.selection.start,
                        cursorEnd = state.bodyValue.selection.end,
                        hideUntouchedMarkers = true,
                        textColor = TabCream,
                        accentColor = AccentGold,
                        codeBgColor = ElectricSlateTokens.SurfaceElevated,
                        codeTextColor = ElectricSlateTokens.TextPrimary,
                        linkColor = ElectricSlateTokens.PrimaryAccent,
                        mutedColor = TextMuted,
                        isLatexInstalled = isLatexInstalled
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

                val density = LocalDensity.current
                val imeBottom = WindowInsets.ime.getBottom(density)

                LaunchedEffect(state.bodyValue.selection, imeBottom, textLayoutResult) {
                    val layout = textLayoutResult ?: return@LaunchedEffect
                    if (state.bodyValue.selection.collapsed) {
                        val cursorOffset = visualTransformation.lastOffsetMapping.originalToTransformed(state.bodyValue.selection.start)
                        val clampedOffset = cursorOffset.coerceIn(0, (layout.layoutInput.text.length - 1).coerceAtLeast(0))
                        if (layout.lineCount > 0) {
                            val line = layout.getLineForOffset(clampedOffset)
                            val lineTop = layout.getLineTop(line)
                            val lineBottom = layout.getLineBottom(line)
                            val viewportHeight = editorScrollState.viewportSize
                            if (viewportHeight > 0) {
                                val scrollPos = editorScrollState.value
                                val marginPx = with(density) { 72.dp.toPx() }.toInt()
                                if (lineBottom + marginPx > scrollPos + viewportHeight) {
                                    editorScrollState.animateScrollTo((lineBottom + marginPx - viewportHeight).coerceAtLeast(0f).toInt())
                                } else if (lineTop - marginPx < scrollPos) {
                                    editorScrollState.animateScrollTo((lineTop - marginPx).coerceAtLeast(0f).toInt())
                                }
                            }
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
                                    val charOffset = layout.getOffsetForPosition(offset)
                                    val origCharOffset = visualTransformation.lastOffsetMapping.transformedToOriginal(charOffset)
                                    val doc = state.bodyValue.text

                                    // Direct link hit-test
                                    val linkRegex = Regex("""\[([^\]]+)\]\(([^)]+)\)""")
                                    val hitMatch = linkRegex.findAll(doc).firstOrNull { origCharOffset in it.range }
                                    if (hitMatch != null) {
                                        val targetUrl = hitMatch.groupValues[2]
                                        if (targetUrl.startsWith("fotara://doc/")) {
                                            val docIdStr = targetUrl.removePrefix("fotara://doc/")
                                            val docId = docIdStr.toLongOrNull()
                                            if (docId != null) {
                                                val isDocx = hitMatch.groupValues[1].endsWith(".docx", ignoreCase = true)
                                                onOpenDocument?.invoke(docId, isDocx)
                                                return@detectTapGestures
                                            }
                                        } else {
                                            com.arinara.fotara.util.LinkLauncher.openUrl(context, targetUrl)
                                            return@detectTapGestures
                                        }
                                    }
                                    val rawUrlRegex = Regex("""https?://[^\s<>"{}|\\^`\[\]]+""")
                                    val hitRaw = rawUrlRegex.findAll(doc).firstOrNull { origCharOffset in it.range }
                                    if (hitRaw != null) {
                                        com.arinara.fotara.util.LinkLauncher.openUrl(context, hitRaw.value)
                                        return@detectTapGestures
                                    }

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
                                        fontSize = 16.sp,
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
                                val hasMic = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasMic) {
                                    coroutineScope.launch {
                                        if (state.noteId == null) {
                                            state.flushAutosaveNow()
                                        }
                                        audioViewModel.startRecording()
                                    }
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            onStopRecording = { audioViewModel.stopAndSaveRecording() },
                            onCancelRecording = { audioViewModel.cancelRecording() },
                            onPauseRecording = { audioViewModel.pauseRecording() },
                            onResumeRecording = { audioViewModel.resumeRecording() }
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
            EditorToolbar(
                state = state,
                isLatexInstalled = isLatexInstalled,
                onAttachDocumentClick = {
                    documentPickerLauncher.launch(
                        arrayOf(
                            "application/pdf",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                            "application/msword"
                        )
                    )
                },
                onInsertImageClick = {
                    imagePickerLauncher.launch(arrayOf("image/*"))
                }
            )
        }
    }
}

    // Link Insertion and Edit Dialog
    if (state.showLinkDialog) {
        TextNoteLinkDialog(
            label = state.linkDialogLabel,
            onLabelChange = { state.linkDialogLabel = it },
            url = state.linkDialogUrl,
            onUrlChange = { state.linkDialogUrl = it },
            onConfirm = { state.confirmLinkDialog() },
            onDismiss = { state.showLinkDialog = false }
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
