// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.canvas

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arinara.fotara.canvas.model.CanvasBackgroundStyle
import com.arinara.fotara.canvas.render.CanvasDrawingView
import com.arinara.fotara.canvas.tool.CanvasToolType
import com.arinara.fotara.theme.DockSlatePill
import com.arinara.fotara.theme.FolderBodyBlue
import com.arinara.fotara.theme.FolderTabCream
import com.arinara.fotara.theme.MidnightCardOutline
import com.arinara.fotara.theme.MidnightNavy
import com.arinara.fotara.theme.MidnightSurface
import com.arinara.fotara.theme.TagAmber
import com.arinara.fotara.theme.TagCrimson
import com.arinara.fotara.theme.TextMuted
import com.arinara.fotara.theme.TextPrimary
import com.arinara.fotara.theme.TextSecondary
import com.arinara.fotara.ui.components.ScheduleNoteDialog
import com.arinara.fotara.util.ScheduleAlertType
import com.arinara.fotara.util.ScheduleNoteType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasScreen(
    viewModel: CanvasViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var drawingViewRef by remember { mutableStateOf<CanvasDrawingView?>(null) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf("") }
    var newLayerName by remember { mutableStateOf("") }
    var showAddLayerDialog by remember { mutableStateOf(false) }
    var showMoveElementsLayerPicker by remember { mutableStateOf(false) }

    // System Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val v = drawingViewRef
            if (v != null) {
                viewModel.addImage(uri, v.viewport, v.width.toFloat(), v.height.toFloat())
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightNavy)
    ) {
        // ==========================================
        // Z6: Canvas Area (Native Hardware Viewport)
        // ==========================================
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                CanvasDrawingView(
                    context = ctx,
                    tileCacheManager = viewModel.tileCacheManager,
                    canvasRenderer = viewModel.canvasRenderer,
                    pointerStateMachine = viewModel.pointerStateMachine,
                    toolController = viewModel.toolController,
                    historyManager = viewModel.historyManager,
                    spatialIndex = viewModel.spatialIndex,
                    onDocumentChanged = { updatedDoc, dirtyRect ->
                        viewModel.onDocumentModified(updatedDoc, dirtyRect)
                    }
                ).apply {
                    onViewportChanged = { v ->
                        viewModel.setZoomPercentage(v.scale)
                    }
                    drawingViewRef = this
                }
            },
            update = { view ->
                if (view.documentSnapshot != uiState.document) {
                    view.documentSnapshot = uiState.document
                }
                drawingViewRef = view
            }
        )

        // ==========================================
        // Z1: Top-Left Header (Back, Title, Save State)
        // ==========================================
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 12.dp, top = 8.dp)
                .zIndex(10f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MidnightSurface.copy(alpha = 0.85f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = FolderTabCream
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                color = MidnightSurface.copy(alpha = 0.85f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MidnightCardOutline),
                modifier = Modifier.clickable {
                    renameInput = uiState.title
                    viewModel.setRenameDialogVisible(true)
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = uiState.title,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Alpha label badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(TagAmber.copy(alpha = 0.2f))
                            .border(0.8.dp, TagAmber.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Alpha",
                            color = TagAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Save state indicator
                    when (uiState.saveState) {
                        SaveState.SAVING -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.5.dp,
                                    color = TagAmber
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Saving...", color = TextMuted, fontSize = 11.sp)
                            }
                        }
                        SaveState.SAVED -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Saved",
                                    tint = FolderTabCream.copy(alpha = 0.8f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Saved", color = TextMuted, fontSize = 11.sp)
                            }
                        }
                        SaveState.ERROR -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Save Error",
                                    tint = TagCrimson,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Error", color = TagCrimson, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // Z2: Top-Right Operations Toolbar
        // ==========================================
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 12.dp, top = 8.dp)
                .zIndex(10f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MidnightSurface.copy(alpha = 0.85f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MidnightCardOutline)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Undo
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = uiState.canUndo,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (uiState.canUndo) FolderTabCream else TextMuted.copy(alpha = 0.35f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Redo
                    IconButton(
                        onClick = { viewModel.redo() },
                        enabled = uiState.canRedo,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (uiState.canRedo) FolderTabCream else TextMuted.copy(alpha = 0.35f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Layers (opens Z5)
                    IconButton(
                        onClick = { viewModel.setLayersPanelVisible(true) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Layers",
                            tint = if (uiState.showLayersPanel) TagAmber else FolderTabCream,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Add Image
                    IconButton(
                        onClick = { viewModel.setImageSourceDialogVisible(true) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Add Image",
                            tint = FolderTabCream,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Schedule (C5)
                    IconButton(
                        onClick = { viewModel.setScheduleDialogVisible(true) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "Schedule Note",
                            tint = if (uiState.scheduledAt != null) TagAmber else FolderTabCream,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Export / Share
                    IconButton(
                        onClick = { viewModel.setExportDialogVisible(true) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export PNG",
                            tint = FolderTabCream,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Overflow Menu
                    Box {
                        IconButton(
                            onClick = { showOverflowMenu = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = FolderTabCream,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false },
                            modifier = Modifier.background(MidnightSurface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Background Style", color = TextPrimary) },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.setBackgroundPickerVisible(true)
                                },
                                leadingIcon = { Icon(Icons.Default.Palette, null, tint = FolderTabCream) }
                            )
                            DropdownMenuItem(
                                text = { Text("Canvas Settings", color = TextPrimary) },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.setSettingsDialogVisible(true)
                                },
                                leadingIcon = { Icon(Icons.Default.Settings, null, tint = FolderTabCream) }
                            )
                            DropdownMenuItem(
                                text = { Text("Rename Note", color = TextPrimary) },
                                onClick = {
                                    showOverflowMenu = false
                                    renameInput = uiState.title
                                    viewModel.setRenameDialogVisible(true)
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, null, tint = FolderTabCream) }
                            )
                            DropdownMenuItem(
                                text = { Text("Note Info", color = TextPrimary) },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.setInfoDialogVisible(true)
                                },
                                leadingIcon = { Icon(Icons.Default.Info, null, tint = FolderTabCream) }
                            )
                            DropdownMenuItem(
                                text = { Text("Move to Folder", color = TextPrimary) },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.setMoveDialogVisible(true)
                                },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.DriveFileMove, null, tint = FolderTabCream) }
                            )
                            HorizontalDivider(color = MidnightCardOutline)
                            DropdownMenuItem(
                                text = { Text("Delete Note", color = TagCrimson) },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.setDeleteConfirmDialogVisible(true)
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = TagCrimson) }
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // Z7: Bottom-Left Zoom & Fit-to-Content
        // ==========================================
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 16.dp, bottom = 20.dp)
                .zIndex(10f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MidnightSurface.copy(alpha = 0.85f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MidnightCardOutline)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Zoom percentage chip: tap resets to 100%
                    Text(
                        text = "${uiState.zoomPercentage}%",
                        color = FolderTabCream,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { drawingViewRef?.resetZoom() }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Fit to content action
                    IconButton(
                        onClick = { drawingViewRef?.fitToContent() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitScreen,
                            contentDescription = "Fit to Content",
                            tint = FolderTabCream,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // ==========================================
        // Z4: Tool Options Popup (Anchored Above Z3)
        // ==========================================
        AnimatedVisibility(
            visible = uiState.showToolOptions,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 88.dp)
                .zIndex(12f)
        ) {
            Surface(
                color = MidnightSurface,
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MidnightCardOutline),
                shadowElevation = 16.dp,
                modifier = Modifier
                    .width(320.dp)
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Title and Close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (uiState.toolState.activeTool) {
                                CanvasToolType.PEN -> "Pen Options"
                                CanvasToolType.HIGHLIGHTER -> "Highlighter Options"
                                CanvasToolType.ERASER_STROKE -> "Stroke Eraser"
                                CanvasToolType.ERASER_AREA -> "Area Eraser"
                                CanvasToolType.SELECT -> "Select Options"
                            },
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { viewModel.toggleToolOptions() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Clear, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    when (uiState.toolState.activeTool) {
                        CanvasToolType.PEN, CanvasToolType.HIGHLIGHTER -> {
                            // Size Slider
                            val isHighlighter = uiState.toolState.activeTool == CanvasToolType.HIGHLIGHTER
                            val sizeValue = if (isHighlighter) uiState.toolState.highlighterSize else uiState.toolState.penSize

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Thickness", color = TextSecondary, fontSize = 12.sp)
                                Text("${sizeValue.toInt()} dp", color = FolderTabCream, fontSize = 12.sp)
                            }
                            Slider(
                                value = sizeValue,
                                onValueChange = {
                                    if (isHighlighter) viewModel.setHighlighterSize(it) else viewModel.setPenSize(it)
                                },
                                valueRange = if (isHighlighter) 10f..60f else 2f..32f,
                                colors = SliderDefaults.colors(
                                    thumbColor = FolderTabCream,
                                    activeTrackColor = FolderTabCream,
                                    inactiveTrackColor = MidnightNavy
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Curated Color Palette
                            Text("Palette", color = TextSecondary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(uiState.recentColors) { colorLong ->
                                    val isSelected = if (isHighlighter) {
                                        uiState.toolState.highlighterColor == colorLong
                                    } else {
                                        uiState.toolState.penColor == colorLong
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color(colorLong.toInt()))
                                            .border(
                                                width = if (isSelected) 2.5.dp else 1.dp,
                                                color = if (isSelected) Color.White else MidnightCardOutline,
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                if (isHighlighter) {
                                                    viewModel.setHighlighterColor(colorLong)
                                                } else {
                                                    viewModel.setPenColor(colorLong)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = if (colorLong == 0xFFFFFFFF) Color.Black else Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        CanvasToolType.ERASER_STROKE, CanvasToolType.ERASER_AREA -> {
                            Text("Eraser Radius", color = TextSecondary, fontSize = 12.sp)
                            Slider(
                                value = uiState.toolState.eraserRadius,
                                onValueChange = { viewModel.setEraserRadius(it) },
                                valueRange = 10f..80f,
                                colors = SliderDefaults.colors(
                                    thumbColor = FolderTabCream,
                                    activeTrackColor = FolderTabCream,
                                    inactiveTrackColor = MidnightNavy
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MidnightNavy)
                                    .clickable { viewModel.toggleEraserMode() }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (uiState.toolState.activeTool == CanvasToolType.ERASER_STROKE) "Mode: Delete Stroke" else "Mode: Slice & Erase Area",
                                    color = FolderTabCream,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text("Switch", color = TagAmber, fontSize = 12.sp)
                            }
                        }
                        CanvasToolType.SELECT -> {
                            Text(
                                text = "Tap elements to select or drag a lasso loop to group-select. Use corner and rotation handles to scale and rotate.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // Z3: Bottom Floating Toolbar (Collapsible Dock)
        // ==========================================
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
                .zIndex(10f)
        ) {
            Surface(
                color = DockSlatePill,
                shape = RoundedCornerShape(28.dp),
                shadowElevation = 12.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MidnightCardOutline)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    // Collapsible Handle
                    IconButton(
                        onClick = { viewModel.toggleBottomDock() },
                        modifier = Modifier
                            .size(24.dp)
                            .padding(bottom = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.isBottomDockCollapsed) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Collapse Dock",
                            tint = FolderTabCream.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = !uiState.isBottomDockCollapsed,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            // Select & Move
                            val isSelect = uiState.toolState.activeTool == CanvasToolType.SELECT
                            IconButton(
                                onClick = { viewModel.setTool(CanvasToolType.SELECT) },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelect) FolderTabCream else Color.Transparent)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NearMe,
                                    contentDescription = "Select",
                                    tint = if (isSelect) MidnightNavy else FolderTabCream,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Pen
                            val isPen = uiState.toolState.activeTool == CanvasToolType.PEN
                            IconButton(
                                onClick = { viewModel.setTool(CanvasToolType.PEN) },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (isPen) FolderTabCream else Color.Transparent)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Pen",
                                    tint = if (isPen) MidnightNavy else FolderTabCream,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Highlighter
                            val isHighlighter = uiState.toolState.activeTool == CanvasToolType.HIGHLIGHTER
                            IconButton(
                                onClick = { viewModel.setTool(CanvasToolType.HIGHLIGHTER) },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (isHighlighter) FolderTabCream else Color.Transparent)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Highlighter",
                                    tint = if (isHighlighter) MidnightNavy else FolderTabCream,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Eraser (Tapping toggles Stroke vs Area)
                            val isEraser = uiState.toolState.activeTool == CanvasToolType.ERASER_STROKE || uiState.toolState.activeTool == CanvasToolType.ERASER_AREA
                            IconButton(
                                onClick = {
                                    if (isEraser) {
                                        viewModel.toggleEraserMode()
                                    } else {
                                        viewModel.setTool(CanvasToolType.ERASER_STROKE)
                                    }
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (isEraser) FolderTabCream else Color.Transparent)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CleaningServices,
                                    contentDescription = "Eraser",
                                    tint = if (isEraser) MidnightNavy else FolderTabCream,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Active Color Swatch (Tapping opens Z4)
                            val currentColor = if (isHighlighter) uiState.toolState.highlighterColor else uiState.toolState.penColor
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(currentColor.toInt()))
                                    .border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                                    .clickable { viewModel.toggleToolOptions() }
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // Z8: Contextual Object Bar (Selection Actions)
        // ==========================================
        if (uiState.toolState.selectedElementIds.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 64.dp)
                    .zIndex(15f)
            ) {
                Surface(
                    color = MidnightSurface,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FolderTabCream.copy(alpha = 0.5f)),
                    shadowElevation = 14.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Duplicate
                        IconButton(
                            onClick = { viewModel.duplicateSelectedElements() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, "Duplicate", tint = FolderTabCream, modifier = Modifier.size(18.dp))
                        }

                        // Bring Forward
                        IconButton(
                            onClick = { viewModel.bringForwardSelection() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.VerticalAlignTop, "Bring Forward", tint = FolderTabCream, modifier = Modifier.size(18.dp))
                        }

                        // Send Backward
                        IconButton(
                            onClick = { viewModel.sendBackwardSelection() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.VerticalAlignBottom, "Send Backward", tint = FolderTabCream, modifier = Modifier.size(18.dp))
                        }

                        // Move to Layer
                        IconButton(
                            onClick = { showMoveElementsLayerPicker = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Layers, "Move to Layer", tint = TagAmber, modifier = Modifier.size(18.dp))
                        }

                        // Delete
                        IconButton(
                            onClick = { viewModel.deleteSelectedElements() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Delete, "Delete", tint = TagCrimson, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // ==========================================
        // Z5: Layers Management Panel (BottomSheet)
        // ==========================================
        if (uiState.showLayersPanel) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.setLayersPanelVisible(false) },
                containerColor = MidnightSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Layers",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = {
                                newLayerName = ""
                                showAddLayerDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FolderBodyBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, null, tint = FolderTabCream, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Layer", color = FolderTabCream, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                    ) {
                        items(uiState.document.layers.sortedByDescending { it.order }) { layer ->
                            val isActive = layer.id == uiState.activeLayerId
                            Surface(
                                color = if (isActive) FolderBodyBlue.copy(alpha = 0.35f) else MidnightNavy.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isActive) 1.5.dp else 0.8.dp,
                                    color = if (isActive) FolderTabCream else MidnightCardOutline
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { viewModel.setActiveLayer(layer.id) }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = layer.name,
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                                        )

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            // Move Up
                                            IconButton(
                                                onClick = { viewModel.moveLayerUp(layer.id) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.KeyboardArrowUp, "Move Up", tint = FolderTabCream, modifier = Modifier.size(16.dp))
                                            }
                                            // Move Down
                                            IconButton(
                                                onClick = { viewModel.moveLayerDown(layer.id) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.KeyboardArrowDown, "Move Down", tint = FolderTabCream, modifier = Modifier.size(16.dp))
                                            }
                                            // Visibility
                                            IconButton(
                                                onClick = { viewModel.toggleLayerVisibility(layer.id) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                    contentDescription = "Visibility",
                                                    tint = if (layer.isVisible) FolderTabCream else TextMuted,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            // Lock
                                            IconButton(
                                                onClick = { viewModel.toggleLayerLock(layer.id) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (layer.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                                    contentDescription = "Lock",
                                                    tint = if (layer.isLocked) TagAmber else TextMuted,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            // Delete
                                            IconButton(
                                                onClick = { viewModel.deleteLayer(layer.id) },
                                                enabled = uiState.document.layers.size > 1,
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Layer",
                                                    tint = if (uiState.document.layers.size > 1) TagCrimson else TextMuted.copy(alpha = 0.3f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Opacity Slider
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Opacity", color = TextMuted, fontSize = 11.sp, modifier = Modifier.width(48.dp))
                                        Slider(
                                            value = layer.opacity,
                                            onValueChange = { viewModel.setLayerOpacity(layer.id, it) },
                                            valueRange = 0.1f..1.0f,
                                            modifier = Modifier.weight(1f),
                                            colors = SliderDefaults.colors(
                                                thumbColor = FolderTabCream,
                                                activeTrackColor = FolderTabCream,
                                                inactiveTrackColor = MidnightNavy
                                            )
                                        )
                                        Text("${(layer.opacity * 100).toInt()}%", color = FolderTabCream, fontSize = 11.sp, modifier = Modifier.width(36.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // ==========================================
        // Dialogs & Sheets
        // ==========================================

        // Rename Canvas Dialog
        if (uiState.showRenameDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.setRenameDialogVisible(false) },
                title = { Text("Rename Canvas", color = TextPrimary) },
                text = {
                    OutlinedTextField(
                        value = renameInput,
                        onValueChange = { renameInput = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = FolderTabCream,
                            unfocusedBorderColor = MidnightCardOutline
                        )
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.renameCanvas(renameInput) }) {
                        Text("Save", color = FolderTabCream)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.setRenameDialogVisible(false) }) {
                        Text("Cancel", color = TextMuted)
                    }
                },
                containerColor = MidnightSurface
            )
        }

        // Add Layer Dialog
        if (showAddLayerDialog) {
            AlertDialog(
                onDismissRequest = { showAddLayerDialog = false },
                title = { Text("New Layer", color = TextPrimary) },
                text = {
                    OutlinedTextField(
                        value = newLayerName,
                        onValueChange = { newLayerName = it },
                        placeholder = { Text("e.g. Inking, Background, Annotations", color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = FolderTabCream,
                            unfocusedBorderColor = MidnightCardOutline
                        )
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.addLayer(newLayerName)
                        showAddLayerDialog = false
                    }) {
                        Text("Create", color = FolderTabCream)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddLayerDialog = false }) {
                        Text("Cancel", color = TextMuted)
                    }
                },
                containerColor = MidnightSurface
            )
        }

        // Background Style Picker Dialog
        if (uiState.showBackgroundPicker) {
            AlertDialog(
                onDismissRequest = { viewModel.setBackgroundPickerVisible(false) },
                title = { Text("Background Pattern", color = TextPrimary) },
                text = {
                    Column {
                        val current = uiState.document.backgroundStyle
                        listOf(
                            Pair(CanvasBackgroundStyle.BLANK, "Blank Canvas"),
                            Pair(CanvasBackgroundStyle.DOTS, "Dot Grid"),
                            Pair(CanvasBackgroundStyle.GRID, "Squared Grid"),
                            Pair(CanvasBackgroundStyle.RULED, "Ruled Lines")
                        ).forEach { (style, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setBackgroundStyle(style) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = current == style,
                                    onClick = { viewModel.setBackgroundStyle(style) },
                                    colors = RadioButtonDefaults.colors(selectedColor = FolderTabCream)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(label, color = TextPrimary, fontSize = 14.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.setBackgroundPickerVisible(false) }) {
                        Text("Done", color = FolderTabCream)
                    }
                },
                containerColor = MidnightSurface
            )
        }

        // Canvas Settings Dialog
        if (uiState.showSettingsDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.setSettingsDialogVisible(false) },
                title = { Text("Canvas Settings", color = TextPrimary) },
                text = {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Stylus-Only Drawing", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Finger only pans and zooms; only pen draws.", color = TextSecondary, fontSize = 12.sp)
                            }
                            Switch(
                                checked = uiState.toolState.stylusOnlyDrawing,
                                onCheckedChange = { viewModel.setStylusOnlyMode(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = FolderTabCream, checkedTrackColor = FolderBodyBlue)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Palm Rejection", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Ignores broad hand touches when pen is active.", color = TextSecondary, fontSize = 12.sp)
                            }
                            Switch(
                                checked = true,
                                onCheckedChange = { viewModel.setPalmRejection(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = FolderTabCream, checkedTrackColor = FolderBodyBlue)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.setSettingsDialogVisible(false) }) {
                        Text("Done", color = FolderTabCream)
                    }
                },
                containerColor = MidnightSurface
            )
        }

        // Note Info Dialog
        if (uiState.showInfoDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.setInfoDialogVisible(false) },
                title = { Text("Canvas Info", color = TextPrimary) },
                text = {
                    Column {
                        val addedStr = SimpleDateFormat("MMMM d, yyyy · h:mm a", Locale.US).format(Date(uiState.document.createdAt))
                        Text("Title: ${uiState.title}", color = TextPrimary, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Created: $addedStr", color = TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Layers: ${uiState.document.layers.size}", color = TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Total Elements: ${uiState.document.elements.size}", color = TextSecondary, fontSize = 12.sp)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.setInfoDialogVisible(false) }) {
                        Text("Close", color = FolderTabCream)
                    }
                },
                containerColor = MidnightSurface
            )
        }

        // Move to Folder Dialog
        if (uiState.showMoveDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.setMoveDialogVisible(false) },
                title = { Text("Move Canvas to Folder", color = TextPrimary) },
                text = {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(uiState.folders) { folder ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.moveCanvas(folder.id, null) {
                                            onBack()
                                        }
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.AutoMirrored.Filled.DriveFileMove, null, tint = FolderTabCream, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(folder.name, color = TextPrimary, fontSize = 14.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.setMoveDialogVisible(false) }) {
                        Text("Cancel", color = TextMuted)
                    }
                },
                containerColor = MidnightSurface
            )
        }

        // Delete Confirm Dialog
        if (uiState.showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.setDeleteConfirmDialogVisible(false) },
                title = { Text("Move to Trash?", color = TextPrimary) },
                text = { Text("This canvas will be moved to the Trash. You can restore it anytime within 30 days.", color = TextSecondary) },
                confirmButton = {
                    Button(
                        onClick = { viewModel.deleteToTrash { onBack() } },
                        colors = ButtonDefaults.buttonColors(containerColor = TagCrimson)
                    ) {
                        Text("Delete", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.setDeleteConfirmDialogVisible(false) }) {
                        Text("Cancel", color = TextMuted)
                    }
                },
                containerColor = MidnightSurface
            )
        }

        // Image Source Dialog
        if (uiState.showImageSourceDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.setImageSourceDialogVisible(false) },
                title = { Text("Add Image", color = TextPrimary) },
                text = {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setImageSourceDialogVisible(false)
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PhotoLibrary, null, tint = FolderTabCream, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("From Gallery / Files", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Choose an image from your device storage", color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.setImageSourceDialogVisible(false) }) {
                        Text("Cancel", color = TextMuted)
                    }
                },
                containerColor = MidnightSurface
            )
        }

        // Export PNG Dialog
        if (uiState.showExportDialog) {
            var exportSelection by remember { mutableStateOf(false) }
            var exportScale by remember { mutableStateOf(1.0f) }

            AlertDialog(
                onDismissRequest = { viewModel.setExportDialogVisible(false) },
                title = { Text("Export & Share PNG", color = TextPrimary) },
                text = {
                    Column {
                        Text("Content Range:", color = TextSecondary, fontSize = 12.sp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { exportSelection = false }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = !exportSelection,
                                onClick = { exportSelection = false },
                                colors = RadioButtonDefaults.colors(selectedColor = FolderTabCream)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Whole Canvas Content", color = TextPrimary, fontSize = 13.sp)
                        }

                        if (uiState.toolState.selectedElementIds.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { exportSelection = true }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = exportSelection,
                                    onClick = { exportSelection = true },
                                    colors = RadioButtonDefaults.colors(selectedColor = FolderTabCream)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Selected Elements Only (${uiState.toolState.selectedElementIds.size})", color = TextPrimary, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Export Scale:", color = TextSecondary, fontSize = 12.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Row(
                                modifier = Modifier.clickable { exportScale = 1.0f },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = exportScale == 1.0f,
                                    onClick = { exportScale = 1.0f },
                                    colors = RadioButtonDefaults.colors(selectedColor = FolderTabCream)
                                )
                                Text("1x Standard", color = TextPrimary, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Row(
                                modifier = Modifier.clickable { exportScale = 2.0f },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = exportScale == 2.0f,
                                    onClick = { exportScale = 2.0f },
                                    colors = RadioButtonDefaults.colors(selectedColor = FolderTabCream)
                                )
                                Text("2x High-Res", color = TextPrimary, fontSize = 13.sp)
                            }
                        }

                        if (uiState.isExporting) {
                            Spacer(modifier = Modifier.height(16.dp))
                            CircularProgressIndicator(
                                progress = { uiState.exportProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp),
                                color = FolderTabCream
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.exportCanvas(context, exportSelection, exportScale) },
                        enabled = !uiState.isExporting,
                        colors = ButtonDefaults.buttonColors(containerColor = FolderBodyBlue)
                    ) {
                        Text("Export & Share", color = FolderTabCream)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.setExportDialogVisible(false) },
                        enabled = !uiState.isExporting
                    ) {
                        Text("Cancel", color = TextMuted)
                    }
                },
                containerColor = MidnightSurface
            )
        }

        // Move Elements to Layer Sheet
        if (showMoveElementsLayerPicker) {
            ModalBottomSheet(
                onDismissRequest = { showMoveElementsLayerPicker = false },
                containerColor = MidnightSurface
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Move Selection to Layer", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    uiState.document.layers.forEach { layer ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.moveSelectionToLayer(layer.id)
                                    showMoveElementsLayerPicker = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Layers, null, tint = FolderTabCream)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(layer.name, color = TextPrimary, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // Experimental Notice Dialog (C10-B One-Time Notice)
        if (uiState.showExperimentalNotice) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissExperimentalNotice() },
                title = { Text("Unlimited Canvas (Alpha)", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        text = "Welcome to Unlimited Canvas! This is an experimental vector sketching and whiteboard tool. Features and formats may evolve. We would love your feedback as we refine it.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.dismissExperimentalNotice() },
                        colors = ButtonDefaults.buttonColors(containerColor = FolderBodyBlue)
                    ) {
                        Text("Get Started", color = FolderTabCream)
                    }
                },
                containerColor = MidnightSurface
            )
        }

        // Schedule Note Dialog (C5)
        if (uiState.showScheduleDialog && uiState.canvasId != null) {
            ScheduleNoteDialog(
                noteTitle = uiState.title,
                initialScheduledAt = uiState.scheduledAt,
                initialAlertType = if (uiState.alertType == ScheduleAlertType.ALARM.name) ScheduleAlertType.ALARM else ScheduleAlertType.NOTIFICATION,
                initialScheduleTitle = null,
                onDismiss = { viewModel.setScheduleDialogVisible(false) },
                onSaveSchedule = { _, _, _ ->
                    viewModel.setScheduleDialogVisible(false)
                },
                onClearSchedule = {
                    viewModel.setScheduleDialogVisible(false)
                }
            )
        }

        // User Message Snackbar/Toast banner
        uiState.userMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 64.dp)
                    .zIndex(20f)
            ) {
                Surface(
                    color = TagCrimson,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(msg, color = Color.White, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { viewModel.clearUserMessage() },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Default.Clear, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}
