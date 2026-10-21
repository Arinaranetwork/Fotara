// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.document

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.arinara.fotara.ui.components.presets.AppDropdownMenu
import com.arinara.fotara.ui.components.presets.AppFloatingBar
import com.arinara.fotara.ui.components.presets.AppFloatingBarItem
import com.arinara.fotara.ui.components.presets.AppOverflowMenuTrigger
import com.arinara.fotara.ui.theme.jigglePressClickable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arinara.fotara.ui.components.presets.AppBottomSheet
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import com.arinara.fotara.R
import com.arinara.fotara.ui.components.presets.AppButton
import com.arinara.fotara.ui.components.presets.AppButtonVariant
import com.arinara.fotara.ui.components.presets.AppCard
import com.arinara.fotara.ui.components.presets.AppScreen
import com.arinara.fotara.ui.components.presets.AppTopBar
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.data.model.DocumentNote
import com.arinara.fotara.data.model.DocumentPage
import com.arinara.fotara.ui.components.NoteDetailScheduleChip
import com.arinara.fotara.ui.components.ScheduleNoteDialog
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.filled.Mic
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.arinara.fotara.canvas.render.PhotoDrawingRenderer
import com.arinara.fotara.data.model.PdfPageDrawing
import com.arinara.fotara.data.repository.PdfPageDrawingRepository
import com.arinara.fotara.data.repository.PdfPagePinRepository
import com.arinara.fotara.data.repository.SettingsRepository
import com.arinara.fotara.util.PdfPageGalleryExporter
import com.arinara.fotara.util.PdfSearchHighlighter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import com.arinara.fotara.util.NoteScheduleManager
import com.arinara.fotara.util.PdfCorruptException
import com.arinara.fotara.util.PdfLayoutMath
import com.arinara.fotara.util.PdfPageRenderer
import com.arinara.fotara.util.PdfPasswordException
import com.arinara.fotara.util.PdfSplitManager
import com.arinara.fotara.util.ScheduleAlertType
import com.arinara.fotara.util.ScheduleNoteType
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import com.arinara.fotara.theme.TagAmber
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val ScreenNavy = ElectricSlateTokens.BgCanvas
private val TabCream = ElectricSlateTokens.TextPrimary
private val AccentGold = ElectricSlateTokens.PrimaryAccent
private val CardBg = ElectricSlateTokens.SurfacePanel
private val DangerRed = ElectricSlateTokens.Destructive
private val BorderColor = ElectricSlateTokens.BorderSubtle
private val TextMuted = ElectricSlateTokens.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    documentNote: DocumentNote,
    pages: List<DocumentPage>,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onSplitToImages: () -> Unit,
    onDelete: () -> Unit,
    initialPageIndex: Int = 0,
    highlightPageIndex: Int? = null,
    pdfPagePinRepository: PdfPagePinRepository? = null,
    pdfPageDrawingRepository: PdfPageDrawingRepository? = null,
    settingsRepository: SettingsRepository? = null,
    searchQuery: String? = null,
    onNavigateToEditor: ((Long, Int) -> Unit)? = null,
    audioAnnotationRepository: com.arinara.fotara.audio.repository.AudioAnnotationRepository? = null
) {
    var showSplitConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showPageOrganizerSheet by remember { mutableStateOf(false) }
    var isApplyingOrganizerChanges by remember { mutableStateOf(false) }
    var pageToClearDrawing by remember { mutableStateOf<Int?>(null) }
    var isReadingMode by rememberSaveable { mutableStateOf(false) }
    var pdfRenderer by remember { mutableStateOf<PdfPageRenderer?>(null) }
    var openErrorMessage by remember { mutableStateOf<String?>(null) }
    var currentScheduledAt by remember { mutableStateOf(documentNote.scheduledAt) }
    var currentAlertType by remember { mutableStateOf(documentNote.alertType) }
    var currentScheduleTitle by remember { mutableStateOf(documentNote.scheduleTitle) }
    var showScheduleDialog by remember { mutableStateOf(false) }
    var flashingPageIndex by remember { mutableStateOf<Int?>(null) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val audioRepo = remember(audioAnnotationRepository, context) {
        audioAnnotationRepository ?: com.arinara.fotara.audio.repository.SqliteAudioAnnotationRepository(com.arinara.fotara.data.db.FotaraDbHelper(context))
    }
    val audioRecorderManager = remember(context) { com.arinara.fotara.audio.recorder.AudioRecorderManager(context.applicationContext) }
    val audioPlayerManager = remember(context) { com.arinara.fotara.audio.player.AudioPlayerManager(context.applicationContext) }
    val audioViewModel: com.arinara.fotara.audio.ui.AudioAnnotationViewModel = viewModel(
        key = "audio_pdf_${documentNote.id}",
        factory = com.arinara.fotara.audio.ui.AudioAnnotationViewModelFactory(
            repository = audioRepo,
            recorderManager = audioRecorderManager,
            playerManager = audioPlayerManager,
            pdfDocId = documentNote.id
        )
    )
    val audioUiState by audioViewModel.uiState.collectAsState()
    var showAudioDock by remember { mutableStateOf(false) }
    var userDismissedAudioDock by remember { mutableStateOf(false) }
    var activeAudioPageIndex by remember { mutableStateOf<Int?>(null) }
    val allPdfAnnotations by remember(audioRepo, documentNote.id) {
        audioRepo.getAllAnnotationsForPdf(documentNote.id)
    }.collectAsState(initial = emptyList())

    val file = remember(documentNote.originFileUri) { File(documentNote.originFileUri) }

    // Observe pinned pages
    val pinnedPages: List<Int> by (pdfPagePinRepository?.observePinnedPages(documentNote.id)
        ?: flowOf(emptyList<Int>())).collectAsState(initial = emptyList<Int>())
    val pinnedPageIndices = remember(pinnedPages) { pinnedPages.toSet() }

    // Observe drawings
    val drawingsMap: Map<Int, PdfPageDrawing> by (pdfPageDrawingRepository?.observeDocumentDrawings(documentNote.id)
        ?: flowOf(emptyMap<Int, PdfPageDrawing>())).collectAsState(initial = emptyMap<Int, PdfPageDrawing>())

    // SAF launcher for Android 7-9 gallery save
    var pendingSafSavePageIndex by remember { mutableStateOf<Int?>(null) }
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/jpeg")
    ) { uri ->
        val pIndex = pendingSafSavePageIndex
        pendingSafSavePageIndex = null
        if (uri != null && pIndex != null && pdfRenderer != null) {
            coroutineScope.launch {
                val drawing = drawingsMap[pIndex]
                val bitmap = PdfPageGalleryExporter.renderPageBitmap(pdfRenderer!!, pIndex, drawing)
                if (bitmap != null) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            PdfPageGalleryExporter.writeBitmapToStream(bitmap, out)
                        }
                        Toast.makeText(context, context.getString(R.string.msg_saved), Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, context.getString(R.string.msg_save_failed), Toast.LENGTH_SHORT).show()
                    } finally {
                        bitmap.recycle()
                    }
                } else {
                    Toast.makeText(context, context.getString(R.string.msg_save_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            audioViewModel.startRecording()
        } else {
            Toast.makeText(context, "Microphone permission is required to record audio notes", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(documentNote.id) {
        onDispose {
            PdfSearchHighlighter.clearCache()
            audioViewModel.pausePlayback()
            audioViewModel.cancelRecording()
            pdfRenderer?.close()
            pdfRenderer = null
        }
    }

    LaunchedEffect(file) {
        withContext(Dispatchers.IO) {
            try {
                val renderer = PdfPageRenderer(file)
                withContext(Dispatchers.Main) {
                    pdfRenderer?.close()
                    pdfRenderer = renderer
                    openErrorMessage = null
                }
            } catch (e: PdfPasswordException) {
                withContext(Dispatchers.Main) {
                    pdfRenderer?.close()
                    pdfRenderer = null
                    openErrorMessage = "This document is password-protected and cannot be opened."
                }
            } catch (e: PdfCorruptException) {
                withContext(Dispatchers.Main) {
                    pdfRenderer?.close()
                    pdfRenderer = null
                    openErrorMessage = "This document is corrupt or invalid: ${e.message}"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    pdfRenderer?.close()
                    pdfRenderer = null
                    openErrorMessage = "Failed to open document: ${e.message}"
                }
            }
        }
    }

    val totalPages = pdfRenderer?.pageCount ?: pages.size
    val subtitleText = if (totalPages == 1) "1 page" else "$totalPages pages"

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialPageIndex.coerceIn(0, (totalPages - 1).coerceAtLeast(0))
    )
    val zoomState = remember { PdfViewportZoomState() }

    var hasScrolledToInitialPage by remember { mutableStateOf(false) }

    LaunchedEffect(initialPageIndex, totalPages) {
        if (!hasScrolledToInitialPage && totalPages > 0 && initialPageIndex in 0 until totalPages) {
            hasScrolledToInitialPage = true
            listState.scrollToItem(initialPageIndex)
        }
    }

    val highlightAlpha = remember { Animatable(0f) }
    LaunchedEffect(highlightPageIndex) {
        if (highlightPageIndex != null) {
            highlightAlpha.snapTo(1f)
            delay(2000L)
            highlightAlpha.animateTo(0f, tween(1000, easing = FastOutSlowInEasing))
        }
    }

    LaunchedEffect(zoomState.isZoomed) {
        if (!zoomState.isZoomed) {
            pdfRenderer?.evictHighResCache()
        }
    }

    val currentVisiblePageIndex by remember {
        derivedStateOf { listState.firstVisibleItemIndex }
    }

    LaunchedEffect(currentVisiblePageIndex, allPdfAnnotations, audioUiState.playerState) {
        val pageHasAudio = allPdfAnnotations.any { it.pdfPageIndex == currentVisiblePageIndex }
        val isAudioActive = audioUiState.playerState is com.arinara.fotara.audio.player.AudioPlayerState.Playing ||
                audioUiState.playerState is com.arinara.fotara.audio.player.AudioPlayerState.Paused
        if (pageHasAudio) {
            if (!userDismissedAudioDock) {
                activeAudioPageIndex = currentVisiblePageIndex
                audioViewModel.setPdfPageFilter(currentVisiblePageIndex)
                showAudioDock = true
            }
        } else {
            if (!isAudioActive && audioUiState.recorderState !is com.arinara.fotara.audio.recorder.AudioRecorderState.Recording && activeAudioPageIndex != currentVisiblePageIndex) {
                showAudioDock = false
                userDismissedAudioDock = false
                activeAudioPageIndex = null
                audioViewModel.setPdfPageFilter(null)
            }
        }
    }

    val handleSavePageToGallery: (Int) -> Unit = { pageIndex ->
        val renderer = pdfRenderer
        if (renderer != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                coroutineScope.launch {
                    val drawing = drawingsMap[pageIndex]
                    val bitmap = PdfPageGalleryExporter.renderPageBitmap(renderer, pageIndex, drawing)
                    if (bitmap != null) {
                        try {
                            val settings = settingsRepository?.settingsFlow?.value
                            val relPath = settings?.getEffectiveSavedImageRelativePath() ?: "Pictures/Fotara"
                            val fileName = PdfPageGalleryExporter.buildFileName(documentNote.name, pageIndex + 1)
                            PdfPageGalleryExporter.saveToMediaStore(context, bitmap, fileName, relPath)
                            Toast.makeText(
                                context,
                                context.getString(R.string.msg_saved_to_path, relPath),
                                Toast.LENGTH_SHORT
                            ).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, context.getString(R.string.msg_save_failed), Toast.LENGTH_SHORT).show()
                        } finally {
                            bitmap.recycle()
                        }
                    } else {
                        Toast.makeText(context, context.getString(R.string.msg_save_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                pendingSafSavePageIndex = pageIndex
                val defaultFileName = PdfPageGalleryExporter.buildFileName(documentNote.name, pageIndex + 1)
                createDocumentLauncher.launch(defaultFileName)
            }
        }
    }

    val handleTogglePin: (Int) -> Unit = { pageIndex ->
        coroutineScope.launch {
            if (pinnedPageIndices.contains(pageIndex)) {
                pdfPagePinRepository?.unpinPage(documentNote.id, pageIndex)
            } else {
                if (pinnedPages.size >= 3) {
                    Toast.makeText(context, context.getString(R.string.pdf_pin_limit_reached), Toast.LENGTH_SHORT).show()
                } else {
                    pdfPagePinRepository?.pinPage(documentNote.id, pageIndex)
                }
            }
        }
    }

    BackHandler(enabled = zoomState.isZoomed || isReadingMode) {
        if (zoomState.isZoomed) {
            coroutineScope.launch { zoomState.animateReset() }
        } else if (isReadingMode) {
            isReadingMode = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenNavy)
    ) {
        AppTopBar(
            onBackClick = {
                if (zoomState.isZoomed) {
                    coroutineScope.launch { zoomState.animateReset() }
                } else if (isReadingMode) {
                    isReadingMode = false
                } else {
                    onBack()
                }
            },
            titleContent = {
                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = documentNote.name,
                        color = TabCream,
                        fontSize = 22.sp,
                        lineHeight = 28.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (currentScheduledAt != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        NoteDetailScheduleChip(
                            scheduledAt = currentScheduledAt,
                            alertType = currentAlertType,
                            scheduleTitle = currentScheduleTitle,
                            onClick = { showScheduleDialog = true }
                        )
                    }
                }
            },
            actions = {
                // Split to Images
                IconButton(onClick = { showSplitConfirmDialog = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.CallSplit,
                        contentDescription = "Split to Images",
                        tint = TabCream
                    )
                }

                // Page Organizer
                IconButton(
                    onClick = { showPageOrganizerSheet = true },
                    enabled = pdfRenderer != null
                ) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = "Organize Pages",
                        tint = TabCream
                    )
                }

                // Overflow menu with spring expansion from pivot
                var showOverflowMenu by remember { mutableStateOf(false) }
                AppOverflowMenuTrigger(
                    expanded = showOverflowMenu,
                    onClick = { showOverflowMenu = true },
                    contentDescription = "More Options",
                    iconTint = TabCream,
                    containerSize = 36.dp,
                    iconSize = 20.dp
                ) {
                    AppDropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false }
                    ) {
                        // Reading Mode Toggle
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (isReadingMode) "Exit Reading Mode" else "Reading Mode",
                                    color = if (isReadingMode) AccentGold else TabCream
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AutoStories,
                                    contentDescription = null,
                                    tint = if (isReadingMode) AccentGold else TextMuted
                                )
                            },
                            onClick = {
                                showOverflowMenu = false
                                isReadingMode = !isReadingMode
                                if (!isReadingMode && zoomState.isZoomed) {
                                    coroutineScope.launch { zoomState.animateReset() }
                                } else if (isReadingMode) {
                                    zoomState.reset()
                                }
                            },
                            enabled = pdfRenderer != null
                        )

                        // Share Document
                        DropdownMenuItem(
                            text = { Text("Share PDF...", color = TabCream) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = TextMuted
                                )
                            },
                            onClick = {
                                showOverflowMenu = false
                                onShare()
                            }
                        )

                        HorizontalDivider(
                            color = BorderColor,
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        // Info Section
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.viewer_info_header),
                                color = TabCream.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (zoomState.isZoomed) {
                                Text(
                                    text = stringResource(R.string.viewer_info_zoomed),
                                    color = AccentGold,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            } else if (isReadingMode) {
                                Text(
                                    text = stringResource(R.string.viewer_info_pinch_or_double_tap),
                                    color = TabCream.copy(alpha = 0.85f),
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.viewer_info_offline),
                                    color = TabCream.copy(alpha = 0.6f),
                                    fontSize = 12.sp
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.viewer_info_reading_mode_available),
                                    color = TabCream.copy(alpha = 0.85f),
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.viewer_info_offline),
                                    color = TabCream.copy(alpha = 0.6f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                        HorizontalDivider(
                            color = BorderColor,
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        DropdownMenuItem(
                            text = { Text("Schedule...", color = TabCream) },
                            leadingIcon = { Icon(Icons.Default.Alarm, null, tint = TextMuted) },
                            onClick = {
                                showOverflowMenu = false
                                showScheduleDialog = true
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Export Annotated PDF...", color = TabCream) },
                            leadingIcon = { Icon(Icons.Default.PictureAsPdf, null, tint = AccentGold) },
                            onClick = {
                                showOverflowMenu = false
                                coroutineScope.launch {
                                    try {
                                        Toast.makeText(context, "Exporting annotated PDF...", Toast.LENGTH_SHORT).show()
                                        val exported = com.arinara.fotara.util.PdfExporter.exportAnnotatedPdf(
                                            context = context,
                                            documentFile = file,
                                            documentTitle = documentNote.title,
                                            drawingsMap = drawingsMap
                                        )
                                        val uri = androidx.core.content.FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            exported
                                        )
                                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "application/pdf"
                                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                            putExtra(android.content.Intent.EXTRA_SUBJECT, "${documentNote.title} (Annotated)")
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Annotated PDF"))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )

                        HorizontalDivider(
                            color = BorderColor,
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        DropdownMenuItem(
                            text = { Text("Delete Document...", color = DangerRed) },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = DangerRed) },
                            onClick = {
                                showOverflowMenu = false
                                showDeleteConfirmDialog = true
                            }
                        )
                    }
                }
            }
        )

        // Pinned Pages Chip Row (Task 3)
        if (pinnedPages.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (pinPageIndex in pinnedPages) {
                    var showPinMenu by remember(pinPageIndex) { mutableStateOf(false) }
                    val isFlashing = flashingPageIndex == pinPageIndex
                    Box {
                        Surface(
                            onClick = {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(pinPageIndex)
                                    flashingPageIndex = pinPageIndex
                                    delay(600L)
                                    if (flashingPageIndex == pinPageIndex) {
                                        flashingPageIndex = null
                                    }
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = CardBg,
                            border = BorderStroke(
                                1.dp,
                                if (isFlashing) AccentGold else BorderColor
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .pointerInput(pinPageIndex) {
                                        detectTapGestures(
                                            onTap = {
                                                coroutineScope.launch {
                                                    listState.animateScrollToItem(pinPageIndex)
                                                    flashingPageIndex = pinPageIndex
                                                    delay(600L)
                                                    if (flashingPageIndex == pinPageIndex) {
                                                        flashingPageIndex = null
                                                    }
                                                }
                                            },
                                            onLongPress = { showPinMenu = true }
                                        )
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = null,
                                    tint = AccentGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = stringResource(R.string.pdf_pinned_page_chip, pinPageIndex + 1),
                                    color = TabCream,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = showPinMenu,
                            onDismissRequest = { showPinMenu = false },
                            modifier = Modifier
                                .background(CardBg)
                                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_unpin), color = TabCream) },
                                leadingIcon = { Icon(Icons.Default.PushPin, null, tint = AccentGold) },
                                onClick = {
                                    showPinMenu = false
                                    coroutineScope.launch {
                                        pdfPagePinRepository?.unpinPage(documentNote.id, pinPageIndex)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        val err = openErrorMessage
        val renderer = pdfRenderer

        when {
            err != null -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Icon(
                        imageVector = if (err.contains("password", ignoreCase = true)) Icons.Default.Lock else Icons.Default.Warning,
                        contentDescription = null,
                        tint = DangerRed,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Cannot Open PDF",
                        color = TabCream,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = err,
                        color = TabCream.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    AppButton(
                        text = "Go Back",
                        onClick = onBack,
                        variant = AppButtonVariant.SECONDARY
                    )
                }
            }
            renderer == null -> {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentGold)
                }
            }
            else -> {
                val totalCount = renderer.pageCount
                val configuration = LocalConfiguration.current
                val density = LocalDensity.current
                val screenWidthPx = with(density) { (configuration.screenWidthDp.dp - 32.dp).roundToPx() }

                val zoomModifier = Modifier
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { tapOffset ->
                                coroutineScope.launch {
                                    val viewW = size.width.toFloat()
                                    val viewH = size.height.toFloat()
                                    zoomState.updateViewport(viewW, viewH)
                                    zoomState.animateDoubleTap(tapOffset.x, tapOffset.y)
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        val viewW = size.width.toFloat()
                        val viewH = size.height.toFloat()
                        zoomState.updateViewport(viewW, viewH)

                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            do {
                                val event = awaitPointerEvent()
                                val canceled = event.changes.any { it.isConsumed }
                                if (canceled) break

                                val pressedCount = event.changes.count { it.pressed }
                                if (pressedCount >= 2) {
                                    val zoom = event.calculateZoom()
                                    val pan = event.calculatePan()
                                    val centroid = event.calculateCentroid(useCurrent = true)

                                    if (kotlin.math.abs(zoom - 1f) > 0.001f || pan != Offset.Zero) {
                                        zoomState.onPinch(
                                            zoomChange = zoom,
                                            panChangeX = pan.x,
                                            panChangeY = pan.y,
                                            centroidX = centroid.x,
                                            centroidY = centroid.y
                                        )
                                        event.changes.forEach { it.consume() }
                                    }
                                } else if (pressedCount == 1 && zoomState.isZoomed) {
                                    val pan = event.calculatePan()
                                    // Panning horizontally when zoomed, while allowing vertical scroll to pass through
                                    if (kotlin.math.abs(pan.x) > kotlin.math.abs(pan.y) * 1.5f && pan.x != 0f) {
                                        zoomState.onPan(pan.x, 0f)
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    }

                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clipToBounds()
                        .then(zoomModifier)
                ) {
                    LaunchedEffect(maxWidth, maxHeight) {
                        zoomState.updateViewport(
                            with(density) { maxWidth.toPx() },
                            with(density) { maxHeight.toPx() }
                        )
                    }

                    LazyColumn(
                        state = listState,
                        userScrollEnabled = true,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = zoomState.scale
                                scaleY = zoomState.scale
                                translationX = zoomState.panX
                                translationY = zoomState.panY
                            },
                        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 100.dp)
                    ) {
                        items(totalCount, key = { index -> "${documentNote.id}_page_$index" }) { pageIndex ->
                            val placeholderPage = pages.getOrNull(pageIndex)
                            val drawing = drawingsMap[pageIndex]
                            val pageAudioCount = allPdfAnnotations.count { it.pdfPageIndex == pageIndex }
                            VirtualizedPdfPageView(
                                pageIndex = pageIndex,
                                totalPages = totalCount,
                                renderer = renderer,
                                targetWidthPx = screenWidthPx,
                                placeholderUri = placeholderPage?.imageUri,
                                isZoomed = zoomState.isZoomed,
                                currentZoomScale = zoomState.scale,
                                isHighlighted = pageIndex == highlightPageIndex && highlightAlpha.value > 0.01f,
                                highlightAlpha = highlightAlpha.value,
                                isPinned = pinnedPageIndices.contains(pageIndex),
                                isFlashing = flashingPageIndex == pageIndex,
                                drawing = drawing,
                                file = file,
                                documentId = documentNote.id,
                                searchQuery = searchQuery,
                                audioNotesCount = pageAudioCount,
                                onAudioBadgeClick = {
                                    userDismissedAudioDock = false
                                    activeAudioPageIndex = pageIndex
                                    audioViewModel.setPdfPageFilter(pageIndex)
                                    showAudioDock = true
                                },
                                onPinToggle = { handleTogglePin(pageIndex) },
                                onSaveToGallery = { handleSavePageToGallery(pageIndex) },
                                onDraw = { onNavigateToEditor?.invoke(documentNote.id, pageIndex) },
                                onToggleDrawingVisibility = { isVisible ->
                                    coroutineScope.launch {
                                        pdfPageDrawingRepository?.setVisible(documentNote.id, pageIndex, isVisible)
                                    }
                                },
                                onClearDrawing = { pageToClearDrawing = pageIndex }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    if (zoomState.isZoomed) {
                        Surface(
                            onClick = {
                                coroutineScope.launch { zoomState.animateReset() }
                            },
                            shape = RoundedCornerShape(999.dp),
                            color = CardBg.copy(alpha = 0.92f),
                            border = BorderStroke(1.dp, AccentGold),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 24.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ZoomOutMap,
                                    contentDescription = "Reset Zoom",
                                    tint = AccentGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Reset Zoom (${(zoomState.scale * 100).toInt()}%)",
                                    color = TabCream,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Bottom Floating Bar for Audio & Quick Tools
                    AppFloatingBar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = if (zoomState.isZoomed) 64.dp else 16.dp),
                        visible = !isReadingMode && !showAudioDock,
                        applyWindowInsets = true
                    ) {
                        AppFloatingBarItem(
                            icon = Icons.Default.Mic,
                            contentDescription = "Audio Notes",
                            isSelected = showAudioDock,
                            onClick = {
                                userDismissedAudioDock = false
                                activeAudioPageIndex = currentVisiblePageIndex
                                audioViewModel.setPdfPageFilter(currentVisiblePageIndex)
                                showAudioDock = true
                            }
                        )
                    }
                }
            }
        }

        // Voice / Audio Annotations Dock
        AnimatedVisibility(
            visible = showAudioDock,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.navigationBarsPadding()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                color = CardBg,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderColor),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Audio Notes (${audioUiState.annotations.size})",
                            color = TabCream,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = {
                                userDismissedAudioDock = true
                                showAudioDock = false
                                audioViewModel.stopPlayback()
                                if (audioUiState.recorderState is com.arinara.fotara.audio.recorder.AudioRecorderState.Recording) {
                                    audioViewModel.cancelRecording()
                                }
                                audioViewModel.setPdfPageFilter(null)
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TabCream.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
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
                                audioViewModel.startRecording()
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        onStopRecording = {
                            val totalPagesCount = pdfRenderer?.pageCount ?: pages.size
                            val targetPage = audioUiState.selectedPdfPageIndex ?: listState.firstVisibleItemIndex.coerceIn(0, (totalPagesCount - 1).coerceAtLeast(0))
                            audioViewModel.stopAndSaveRecording(targetPdfPageIndex = targetPage)
                        },
                        onCancelRecording = { audioViewModel.cancelRecording() }
                    )

                    for (annotation in audioUiState.annotations) {
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
    }

    if (showSplitConfirmDialog) {
        val totalCount = pdfRenderer?.pageCount ?: pages.size
        PdfSplitConfirmDialog(
            documentName = documentNote.name,
            totalCount = totalCount,
            onConfirm = {
                showSplitConfirmDialog = false
                onSplitToImages()
            },
            onDismiss = { showSplitConfirmDialog = false }
        )
    }

    pageToClearDrawing?.let { targetPage ->
        PdfClearDrawingConfirmDialog(
            onConfirm = {
                val p = targetPage
                pageToClearDrawing = null
                coroutineScope.launch {
                    pdfPageDrawingRepository?.clearDrawing(documentNote.id, p)
                }
            },
            onDismiss = { pageToClearDrawing = null }
        )
    }

    if (showScheduleDialog) {
        val scheduleManager = remember { NoteScheduleManager(context) }
        ScheduleNoteDialog(
            noteTitle = documentNote.name,
            initialScheduledAt = currentScheduledAt,
            initialAlertType = try {
                ScheduleAlertType.valueOf(currentAlertType ?: "NOTIFICATION")
            } catch (_: Exception) {
                ScheduleAlertType.NOTIFICATION
            },
            initialScheduleTitle = currentScheduleTitle,
            onDismiss = { showScheduleDialog = false },
            onSaveSchedule = { scheduledAt, alertType, scheduleTitle ->
                scheduleManager.scheduleNote(
                    noteType = ScheduleNoteType.DOCUMENT,
                    noteId = documentNote.id,
                    folderId = documentNote.folderId,
                    title = documentNote.name,
                    triggerAtMillis = scheduledAt,
                    alertType = alertType,
                    scheduleTitle = scheduleTitle
                )
                currentScheduledAt = scheduledAt
                currentAlertType = alertType.name
                currentScheduleTitle = scheduleTitle
                showScheduleDialog = false
            },
            onClearSchedule = {
                scheduleManager.cancelSchedule(ScheduleNoteType.DOCUMENT, documentNote.id)
                currentScheduledAt = null
                currentAlertType = null
                currentScheduleTitle = null
                showScheduleDialog = false
            }
        )
    }

    if (showDeleteConfirmDialog) {
        val totalCount = pdfRenderer?.pageCount ?: pages.size
        PdfDeleteDocumentConfirmDialog(
            documentTitle = documentNote.title,
            pageCount = totalCount,
            onConfirm = {
                showDeleteConfirmDialog = false
                onDelete()
            },
            onDismiss = { showDeleteConfirmDialog = false }
        )
    }

    if (showPageOrganizerSheet) {
        val totalCount = pdfRenderer?.pageCount ?: pages.size
        PdfPageOrganizerSheet(
            visible = showPageOrganizerSheet,
            onDismiss = { showPageOrganizerSheet = false },
            renderer = pdfRenderer,
            totalPages = totalCount,
            isApplying = isApplyingOrganizerChanges,
            onApplyChanges = { newOrder, newRotations ->
                isApplyingOrganizerChanges = true
                coroutineScope.launch {
                    try {
                        pdfRenderer?.close()
                        pdfRenderer = null
                        val (success, newRenderer) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            val ok = com.arinara.fotara.util.PdfExporter.reorganizePdfPages(
                                context = context,
                                sourceFile = file,
                                pageOrder = newOrder,
                                pageRotations = newRotations
                            )
                            val r = try { PdfPageRenderer(file) } catch (_: Exception) { null }
                            Pair(ok, r)
                        }
                        pdfRenderer = newRenderer
                        if (success && newRenderer != null) {
                            Toast.makeText(context, "Pages updated successfully", Toast.LENGTH_SHORT).show()
                            showPageOrganizerSheet = false
                        } else {
                            Toast.makeText(context, "Failed to apply page updates", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        val fallback = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            try { PdfPageRenderer(file) } catch (_: Exception) { null }
                        }
                        pdfRenderer = fallback
                        Toast.makeText(context, "Error updating pages: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        isApplyingOrganizerChanges = false
                    }
                }
            }
        )
    }
}

@Composable
private fun VirtualizedPdfPageView(
    pageIndex: Int,
    totalPages: Int,
    renderer: PdfPageRenderer,
    targetWidthPx: Int,
    placeholderUri: String?,
    isZoomed: Boolean = false,
    currentZoomScale: Float = 1.0f,
    isHighlighted: Boolean = false,
    highlightAlpha: Float = 0f,
    isPinned: Boolean = false,
    isFlashing: Boolean = false,
    drawing: PdfPageDrawing? = null,
    file: File,
    documentId: Long,
    searchQuery: String? = null,
    audioNotesCount: Int = 0,
    onAudioBadgeClick: () -> Unit = {},
    onPinToggle: () -> Unit = {},
    onSaveToGallery: () -> Unit = {},
    onDraw: () -> Unit = {},
    onToggleDrawingVisibility: (Boolean) -> Unit = {},
    onClearDrawing: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var baseBitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(null) }
    var highResBitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(null) }
    var renderError by remember(pageIndex) { mutableStateOf(false) }
    var retryCount by remember(pageIndex) { mutableIntStateOf(0) }
    var showCardMenu by remember { mutableStateOf(false) }

    // Search highlights
    var highlightData by remember(searchQuery, pageIndex) { mutableStateOf<com.arinara.fotara.util.PageHighlightData?>(null) }
    LaunchedEffect(searchQuery, pageIndex) {
        if (!searchQuery.isNullOrBlank()) {
            highlightData = PdfSearchHighlighter.getHighlightBoxes(context, documentId, pageIndex, file, searchQuery)
        } else {
            highlightData = null
        }
    }

    // Read true aspect ratio synchronously from renderer cache (no default 0.707f race)
    val aspectRatio = remember(pageIndex) { renderer.getPageAspectRatio(pageIndex) }

    // Prefetch adjacent pages in background without blocking UI
    LaunchedEffect(pageIndex, targetWidthPx) {
        val targetHeightPx = PdfLayoutMath.computeTargetSize(targetWidthPx, aspectRatio).heightPx
        coroutineScope.launch {
            if (pageIndex > 0) renderer.prefetchPage(pageIndex - 1, targetWidthPx, targetHeightPx)
            if (pageIndex < totalPages - 1) renderer.prefetchPage(pageIndex + 1, targetWidthPx, targetHeightPx)
        }
    }

    // 1. Base resolution render (1.0x display density)
    LaunchedEffect(pageIndex, targetWidthPx, retryCount) {
        val targetHeightPx = PdfLayoutMath.computeTargetSize(targetWidthPx, aspectRatio).heightPx
        renderError = false
        val rendered = renderer.renderPage(
            pageIndex = pageIndex,
            destWidth = targetWidthPx,
            destHeight = targetHeightPx,
            renderScale = 1.0f
        )
        if (rendered != null) {
            baseBitmap = rendered
        } else {
            renderError = true
        }
    }

    // 2. High-resolution on-demand render when list is zoomed (scales dynamically with zoom up to 4.0x)
    LaunchedEffect(pageIndex, targetWidthPx, isZoomed, currentZoomScale, retryCount) {
        if (isZoomed && currentZoomScale > 1.05f) {
            delay(150) // Debounce rapid pinch operations
            val targetHeightPx = PdfLayoutMath.computeTargetSize(targetWidthPx, aspectRatio).heightPx
            val sharpRender = renderer.renderPage(
                pageIndex = pageIndex,
                destWidth = targetWidthPx,
                destHeight = targetHeightPx,
                renderScale = currentZoomScale.coerceIn(1.0f, 4.0f)
            )
            if (sharpRender != null) {
                highResBitmap = sharpRender
            }
        } else {
            highResBitmap = null
        }
    }

    val cardBorderColor = when {
        isFlashing -> AccentGold
        isHighlighted && highlightAlpha > 0.01f -> TagAmber.copy(alpha = highlightAlpha)
        else -> BorderColor
    }

    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        containerColor = CardBg,
        borderColor = cardBorderColor,
        cornerRadius = 12.dp,
        contentPadding = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Page ${pageIndex + 1} of $totalPages",
                    color = TabCream.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                if (isPinned) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        tint = AccentGold,
                        modifier = Modifier.size(13.dp)
                    )
                }
                if (audioNotesCount > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = CardBg,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderColor),
                        modifier = Modifier.clickable { onAudioBadgeClick() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Audio Notes",
                                tint = TabCream,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "$audioNotesCount",
                                color = TabCream,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                if (isZoomed && highResBitmap == null && !renderError) {
                    Text(
                        text = "Sharpening…",
                        color = TabCream.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    )
                }

                // 3-dot page menu (hidden while zoomed)
                if (!isZoomed) {
                    Box {
                        IconButton(
                            onClick = { showCardMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.cd_page_options),
                                tint = TabCream,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showCardMenu,
                            onDismissRequest = { showCardMenu = false },
                            modifier = Modifier
                                .background(ElectricSlateTokens.SurfaceElevated)
                                .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                        ) {
                            // 1. Save to gallery
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.pdf_page_menu_save_to_gallery), color = ElectricSlateTokens.TextPrimary) },
                                leadingIcon = { Icon(Icons.Default.Download, null, tint = ElectricSlateTokens.TextMuted) },
                                onClick = {
                                    showCardMenu = false
                                    onSaveToGallery()
                                }
                            )

                            // 2. Pin / Unpin page
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(if (isPinned) R.string.pdf_page_menu_unpin_page else R.string.pdf_page_menu_pin_page),
                                        color = ElectricSlateTokens.TextPrimary
                                    )
                                },
                                leadingIcon = { Icon(Icons.Default.PushPin, null, tint = ElectricSlateTokens.TextMuted) },
                                onClick = {
                                    showCardMenu = false
                                    onPinToggle()
                                }
                            )

                            // 2b. Voice Note
                            DropdownMenuItem(
                                text = { Text("Record Voice Note", color = ElectricSlateTokens.TextPrimary) },
                                leadingIcon = { Icon(Icons.Default.Mic, null, tint = ElectricSlateTokens.TextMuted) },
                                onClick = {
                                    showCardMenu = false
                                    onAudioBadgeClick()
                                }
                            )

                            HorizontalDivider(color = ElectricSlateTokens.BorderSubtle, modifier = Modifier.padding(vertical = 4.dp))

                            // 3. Draw on page / Edit drawing
                            val hasStrokes = drawing?.hasStrokes == true
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(if (hasStrokes) R.string.pdf_page_menu_edit_drawing else R.string.pdf_page_menu_draw_on_page),
                                        color = ElectricSlateTokens.TextPrimary
                                    )
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, null, tint = ElectricSlateTokens.TextMuted) },
                                onClick = {
                                    showCardMenu = false
                                    onDraw()
                                }
                            )

                            if (hasStrokes) {
                                val isDrawingVisible = drawing.isVisible
                                // 4. Hide / Show drawing
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            stringResource(if (isDrawingVisible) R.string.pdf_page_menu_hide_drawing else R.string.pdf_page_menu_show_drawing),
                                            color = ElectricSlateTokens.TextPrimary
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            if (isDrawingVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            null,
                                            tint = ElectricSlateTokens.TextMuted
                                        )
                                    },
                                    onClick = {
                                        showCardMenu = false
                                        onToggleDrawingVisibility(!isDrawingVisible)
                                    }
                                )

                                // 5. Clear drawing
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.pdf_page_menu_clear_drawing), color = ElectricSlateTokens.Destructive) },
                                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = ElectricSlateTokens.Destructive) },
                                    onClick = {
                                        showCardMenu = false
                                        onClearDrawing()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .background(Color.White)
                    .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)),
                contentAlignment = Alignment.Center
            ) {
                val currentBitmap = highResBitmap ?: baseBitmap
                if (isHighlighted && highlightAlpha > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(TagAmber.copy(alpha = highlightAlpha * 0.18f))
                    )
                }
                when {
                    renderError && currentBitmap == null -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = DangerRed,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Failed to render page",
                                color = TabCream,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            AppButton(
                                text = "Retry",
                                onClick = { retryCount++ },
                                leadingIcon = Icons.Default.Refresh,
                                variant = AppButtonVariant.SECONDARY,
                                height = 36.dp
                            )
                        }
                    }
                    currentBitmap != null && !currentBitmap.isRecycled -> {
                        // If drawing is present and visible, render bitmap + strokes together onto the same canvas
                        // to guarantee Multiply blending directly onto the page surface.
                        val hasVisibleDrawing = drawing != null && drawing.isVisible && (drawing.hasStrokes || drawing.textLayers.isNotEmpty())
                        if (hasVisibleDrawing) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawIntoCanvas { composeCanvas ->
                                    val nativeCanvas = composeCanvas.nativeCanvas
                                    val srcRect = android.graphics.Rect(0, 0, currentBitmap.width, currentBitmap.height)
                                    val dstRect = android.graphics.RectF(0f, 0f, size.width, size.height)
                                    nativeCanvas.drawBitmap(currentBitmap, srcRect, dstRect, null)

                                    val scale = if (drawing.pageWidth > 0f) size.width / drawing.pageWidth else 1f
                                    if (drawing.hasStrokes) {
                                        PhotoDrawingRenderer.renderStrokes(
                                            canvas = nativeCanvas,
                                            strokes = drawing.strokes,
                                            scale = scale
                                        )
                                    }
                                    if (drawing.textLayers.isNotEmpty()) {
                                        PhotoDrawingRenderer.renderTextLayers(
                                            canvas = nativeCanvas,
                                            textLayers = drawing.textLayers,
                                            scale = scale
                                        )
                                    }
                                }
                            }
                        } else {
                            Image(
                                bitmap = currentBitmap.asImageBitmap(),
                                contentDescription = "Page ${pageIndex + 1}",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    placeholderUri != null && File(placeholderUri).exists() -> {
                        AsyncImage(
                            model = File(placeholderUri),
                            contentDescription = "Page ${pageIndex + 1} Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> {
                        CircularProgressIndicator(
                            color = AccentGold,
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }

                // Search highlights overlay (translucent rounded rectangles with 2dp corner radius, 35% alpha)
                val hData = highlightData
                if (hData != null && hData.boxes.isNotEmpty()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        for (box in hData.boxes) {
                            val rect = androidx.compose.ui.geometry.Rect(
                                left = box.left * w,
                                top = box.top * h,
                                right = box.right * w,
                                bottom = box.bottom * h
                            )
                            drawRoundRect(
                                color = TagAmber.copy(alpha = 0.35f),
                                topLeft = rect.topLeft,
                                size = rect.size,
                                cornerRadius = cornerRadius
                            )
                        }
                    }
                }
            }
        }
    }
}

