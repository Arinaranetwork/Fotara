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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arinara.fotara.data.model.BackgroundStyle
import com.arinara.fotara.data.model.CanvasPoint
import com.arinara.fotara.data.model.CanvasStroke
import com.arinara.fotara.data.model.CanvasTool
import com.arinara.fotara.data.model.NibProfile
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
import com.arinara.fotara.util.NoteScheduleManager
import com.arinara.fotara.util.ScheduleAlertType
import com.arinara.fotara.util.ScheduleNoteType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasScreen(
    viewModel: CanvasViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showOverflowMenu by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf("") }
    var newLayerName by remember { mutableStateOf("") }
    var showAddLayerDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.addImageAttachment(uri.toString())
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightNavy)
    ) {
        // ==========================================
        // Z6: Infinite Interactive Drawing Viewport
        // ==========================================
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(uiState.zoomScale, uiState.panOffset) {
                    // Two-finger pan and zoom gesture
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        if (zoom != 1.0f) {
                            viewModel.setZoomScale(uiState.zoomScale * zoom)
                        }
                        if (pan != Offset.Zero) {
                            viewModel.updatePan(pan)
                        }
                    }
                }
                .pointerInput(uiState.activeTool, uiState.zoomScale, uiState.panOffset) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val changes = event.changes
                            // Single finger drawing touch
                            if (changes.size == 1) {
                                val change = changes[0]
                                val screenPos = change.position
                                val w = size.width
                                val h = size.height
                                val canvasX = (screenPos.x - w / 2f - uiState.panOffset.x) / uiState.zoomScale
                                val canvasY = (screenPos.y - h / 2f - uiState.panOffset.y) / uiState.zoomScale
                                val pressure = if (change.type == PointerType.Stylus) change.pressure else 1.0f
                                val pt = CanvasPoint(canvasX, canvasY, pressure)

                                if (change.pressed && !change.previousPressed) {
                                    viewModel.startDrawingStroke(pt)
                                    change.consume()
                                } else if (change.pressed && change.previousPressed) {
                                    viewModel.appendDrawingPoint(pt)
                                    change.consume()
                                } else if (!change.pressed && change.previousPressed) {
                                    viewModel.finishDrawingStroke()
                                    change.consume()
                                }
                            }
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val zoom = uiState.zoomScale
                val pan = uiState.panOffset
                val bg = uiState.document.backgroundStyle

                // Draw Background Patterns
                when (bg) {
                    BackgroundStyle.GRID -> {
                        val gridStep = 40f * zoom
                        val startX = (w / 2f + pan.x) % gridStep
                        val startY = (h / 2f + pan.y) % gridStep
                        var x = startX
                        while (x < w) {
                            drawLine(
                                color = MidnightCardOutline.copy(alpha = 0.35f),
                                start = Offset(x, 0f),
                                end = Offset(x, h),
                                strokeWidth = 1f
                            )
                            x += gridStep
                        }
                        var y = startY
                        while (y < h) {
                            drawLine(
                                color = MidnightCardOutline.copy(alpha = 0.35f),
                                start = Offset(0f, y),
                                end = Offset(w, y),
                                strokeWidth = 1f
                            )
                            y += gridStep
                        }
                    }
                    BackgroundStyle.DOTS -> {
                        val dotStep = 36f * zoom
                        val startX = (w / 2f + pan.x) % dotStep
                        val startY = (h / 2f + pan.y) % dotStep
                        var x = startX
                        while (x < w) {
                            var y = startY
                            while (y < h) {
                                drawCircle(
                                    color = TextMuted.copy(alpha = 0.3f),
                                    radius = 1.5f * zoom.coerceIn(0.5f, 2.5f),
                                    center = Offset(x, y)
                                )
                                y += dotStep
                            }
                            x += dotStep
                        }
                    }
                    BackgroundStyle.RULED -> {
                        val lineStep = 32f * zoom
                        val startY = (h / 2f + pan.y) % lineStep
                        var y = startY
                        while (y < h) {
                            drawLine(
                                color = MidnightCardOutline.copy(alpha = 0.5f),
                                start = Offset(0f, y),
                                end = Offset(w, y),
                                strokeWidth = 1f
                            )
                            y += lineStep
                        }
                    }
                    BackgroundStyle.BLANK -> {}
                }

                // Render Canvas Layers & Strokes
                fun toScreen(pt: CanvasPoint): Offset {
                    return Offset(
                        x = pt.x * zoom + w / 2f + pan.x,
                        y = pt.y * zoom + h / 2f + pan.y
                    )
                }

                fun renderStroke(stroke: CanvasStroke, layerOpacity: Float) {
                    val pts = stroke.points
                    if (pts.size < 2) return

                    val path = Path()
                    val p0 = toScreen(pts[0])
                    path.moveTo(p0.x, p0.y)

                    for (i in 1 until pts.size) {
                        val pPrev = toScreen(pts[i - 1])
                        val pCurr = toScreen(pts[i])
                        val midX = (pPrev.x + pCurr.x) / 2f
                        val midY = (pPrev.y + pCurr.y) / 2f
                        path.quadraticTo(pPrev.x, pPrev.y, midX, midY)
                    }
                    val pLast = toScreen(pts.last())
                    path.lineTo(pLast.x, pLast.y)

                    val strokeColor = Color(stroke.color.toULong())
                    val effectiveAlpha = (stroke.alpha * layerOpacity).coerceIn(0f, 1f)
                    val effectiveWidth = stroke.size * zoom

                    drawPath(
                        path = path,
                        color = strokeColor.copy(alpha = effectiveAlpha),
                        style = Stroke(
                            width = effectiveWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        ),
                        blendMode = if (stroke.tool == CanvasTool.HIGHLIGHTER) BlendMode.SrcOver else BlendMode.SrcOver
                    )
                }

                // Draw existing strokes across visible layers
                for (layer in uiState.document.layers) {
                    if (!layer.isVisible) continue
                    for (stroke in layer.strokes) {
                        renderStroke(stroke, layer.opacity)
                    }
                }

                // Draw active stroke currently being drawn
                uiState.currentDrawingStroke?.let { inProgress ->
                    renderStroke(inProgress, 1.0f)
                }
            }
        }

        // ==========================================
        // Z1 & Z2: Top Bar Header Region
        // ==========================================
        Surface(
            color = MidnightNavy.copy(alpha = 0.88f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MidnightCardOutline.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                // Z1: Top-Left Navigation & Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = FolderTabCream
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(
                        modifier = Modifier.clickable {
                            renameInput = uiState.title
                            viewModel.setRenameDialogVisible(true)
                        }
                    ) {
                        Text(
                            text = uiState.title,
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    color = FolderTabCream,
                                    strokeWidth = 1.5.dp,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Saving...", color = TextMuted, fontSize = 11.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = TagAmber,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Saved", color = TextMuted, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Z2: Top-Right Operations Toolbar
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = uiState.canUndo
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (uiState.canUndo) FolderTabCream else TextMuted
                        )
                    }
                    IconButton(
                        onClick = { viewModel.redo() },
                        enabled = uiState.canRedo
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (uiState.canRedo) FolderTabCream else TextMuted
                        )
                    }
                    IconButton(onClick = { viewModel.setLayersPanelVisible(true) }) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Layers",
                            tint = FolderTabCream
                        )
                    }
                    IconButton(onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Add Image",
                            tint = FolderTabCream
                        )
                    }
                    IconButton(onClick = { viewModel.setScheduleDialogVisible(true) }) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "Schedule",
                            tint = if (uiState.scheduledAt != null) TagAmber else FolderTabCream
                        )
                    }
                    IconButton(onClick = { viewModel.exportPng(context) }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export PNG",
                            tint = FolderTabCream
                        )
                    }
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = FolderTabCream
                            )
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false },
                            containerColor = MidnightSurface
                        ) {
                            DropdownMenuItem(
                                text = { Text("Background Pattern", color = TextPrimary) },
                                leadingIcon = { Icon(Icons.Default.GridOn, null, tint = FolderTabCream) },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.setBackgroundPickerVisible(true)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Canvas Info", color = TextPrimary) },
                                leadingIcon = { Icon(Icons.Default.Info, null, tint = FolderTabCream) },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.setInfoDialogVisible(true)
                                }
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // Z7: Bottom-Left Viewport Controls
        // ==========================================
        Surface(
            color = MidnightSurface.copy(alpha = 0.9f),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MidnightCardOutline),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 16.dp, bottom = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                // Zoom Percentage Chip
                Text(
                    text = "${(uiState.zoomScale * 100).toInt()}%",
                    color = FolderTabCream,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { viewModel.resetZoom() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                // Fit to Content Button
                Surface(
                    color = FolderBodyBlue,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.clickable { viewModel.fitToContent() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitScreen,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Fit", color = TextPrimary, fontSize = 11.sp)
                    }
                }
            }
        }

        // ==========================================
        // Z4: Tool Options Flyout Popup
        // ==========================================
        AnimatedVisibility(
            visible = uiState.showToolOptions,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 76.dp)
        ) {
            Surface(
                color = MidnightSurface,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MidnightCardOutline),
                shadowElevation = 8.dp,
                modifier = Modifier.width(320.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${uiState.activeTool.name} Options",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        IconButton(
                            onClick = { viewModel.toggleToolOptions() },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Close", tint = TextMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Stroke Width Slider
                    Text(
                        text = "Stroke Size: ${uiState.strokeSize.toInt()}px",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Slider(
                        value = uiState.strokeSize,
                        onValueChange = { viewModel.setStrokeSize(it) },
                        valueRange = 2f..40f,
                        colors = SliderDefaults.colors(
                            thumbColor = FolderTabCream,
                            activeTrackColor = FolderTabCream,
                            inactiveTrackColor = DockSlatePill
                        )
                    )

                    // Color Palette
                    Text(text = "Ink Color", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    val colors = listOf(
                        0xFFFFFFFF to "White",
                        0xFFEBD8B8 to "Cream",
                        0xFFF59E0B to "Amber",
                        0xFFEF4444 to "Crimson",
                        0xFF38BDF8 to "Sky",
                        0xFF10B981 to "Emerald",
                        0xFFA855F7 to "Purple"
                    )
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        colors.forEach { (colorLong, _) ->
                            val isSelected = uiState.strokeColor == colorLong
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorLong.toULong()))
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) FolderTabCream else MidnightCardOutline,
                                        shape = CircleShape
                                    )
                                    .clickable { viewModel.setStrokeColor(colorLong) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Nib Profile
                    Text(text = "Nib Profile", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        NibProfile.entries.forEach { profile ->
                            val isSelected = uiState.nibProfile == profile
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setNibProfile(profile) },
                                label = { Text(profile.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FolderTabCream,
                                    selectedLabelColor = MidnightNavy,
                                    containerColor = MidnightNavy,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // Z3: Bottom Floating Tool Dock
        // ==========================================
        Surface(
            color = MidnightSurface.copy(alpha = 0.95f),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MidnightCardOutline),
            shadowElevation = 10.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                ToolButton(
                    icon = Icons.Default.NearMe,
                    label = "Select",
                    isSelected = uiState.activeTool == CanvasTool.SELECT,
                    onClick = { viewModel.setTool(CanvasTool.SELECT) }
                )
                ToolButton(
                    icon = Icons.Default.Edit,
                    label = "Pen",
                    isSelected = uiState.activeTool == CanvasTool.PEN,
                    onClick = { viewModel.setTool(CanvasTool.PEN) }
                )
                ToolButton(
                    icon = Icons.Default.Brush,
                    label = "Highlighter",
                    isSelected = uiState.activeTool == CanvasTool.HIGHLIGHTER,
                    onClick = { viewModel.setTool(CanvasTool.HIGHLIGHTER) }
                )
                ToolButton(
                    icon = Icons.Default.Clear,
                    label = "Eraser",
                    isSelected = uiState.activeTool == CanvasTool.ERASER,
                    onClick = { viewModel.setTool(CanvasTool.ERASER) }
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Active Color Swatch Trigger for Z4
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(uiState.strokeColor.toULong()))
                        .border(1.5.dp, FolderTabCream, CircleShape)
                        .clickable { viewModel.toggleToolOptions() }
                )
            }
        }
    }

    // ==========================================
    // Z5: Layers Management Panel (ModalBottomSheet)
    // ==========================================
    if (uiState.showLayersPanel) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.setLayersPanelVisible(false) },
            containerColor = MidnightSurface,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Layers (${uiState.document.layers.size})",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = {
                            newLayerName = ""
                            showAddLayerDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FolderTabCream, contentColor = MidnightNavy)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Layer")
                    }
                }

                HorizontalDivider(color = MidnightCardOutline, modifier = Modifier.padding(vertical = 10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.document.layers.reversed()) { layer ->
                        val isActive = layer.id == uiState.activeLayerId
                        Surface(
                            color = if (isActive) FolderBodyBlue else MidnightNavy,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isActive) 1.5.dp else 1.dp,
                                color = if (isActive) FolderTabCream else MidnightCardOutline
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setActiveLayer(layer.id) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = layer.name,
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${layer.strokes.size} strokes",
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                }

                                IconButton(onClick = { viewModel.toggleLayerVisibility(layer.id) }) {
                                    Icon(
                                        imageVector = if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Visibility",
                                        tint = if (layer.isVisible) FolderTabCream else TextMuted
                                    )
                                }

                                IconButton(onClick = { viewModel.toggleLayerLock(layer.id) }) {
                                    Icon(
                                        imageVector = if (layer.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                        contentDescription = "Lock",
                                        tint = if (layer.isLocked) TagAmber else TextMuted
                                    )
                                }

                                if (uiState.document.layers.size > 1) {
                                    IconButton(onClick = { viewModel.deleteLayer(layer.id) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = TagCrimson
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Rename Dialog
    if (uiState.showRenameDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setRenameDialogVisible(false) },
            containerColor = MidnightSurface,
            shape = RoundedCornerShape(16.dp),
            title = { Text("Rename Canvas", color = TextPrimary, fontWeight = FontWeight.Bold) },
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
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.renameCanvas(renameInput) },
                    colors = ButtonDefaults.buttonColors(containerColor = FolderTabCream, contentColor = MidnightNavy)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setRenameDialogVisible(false) }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Background Pattern Picker Dialog
    if (uiState.showBackgroundPicker) {
        AlertDialog(
            onDismissRequest = { viewModel.setBackgroundPickerVisible(false) },
            containerColor = MidnightSurface,
            shape = RoundedCornerShape(16.dp),
            title = { Text("Canvas Background Pattern", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BackgroundStyle.entries.forEach { style ->
                        val isSelected = uiState.document.backgroundStyle == style
                        Surface(
                            color = if (isSelected) FolderBodyBlue else MidnightNavy,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) FolderTabCream else MidnightCardOutline
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setBackgroundStyle(style)
                                    viewModel.setBackgroundPickerVisible(false)
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = style.name.lowercase().replaceFirstChar { it.uppercase() },
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.setBackgroundPickerVisible(false) }) {
                    Text("Close", color = FolderTabCream)
                }
            }
        )
    }

    // Add Layer Dialog
    if (showAddLayerDialog) {
        AlertDialog(
            onDismissRequest = { showAddLayerDialog = false },
            containerColor = MidnightSurface,
            shape = RoundedCornerShape(16.dp),
            title = { Text("New Layer", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newLayerName,
                    onValueChange = { newLayerName = it },
                    placeholder = { Text("Layer Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = FolderTabCream,
                        unfocusedBorderColor = MidnightCardOutline
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addLayer(newLayerName)
                        showAddLayerDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FolderTabCream, contentColor = MidnightNavy)
                ) {
                    Text("Add", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddLayerDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Canvas Info Dialog
    if (uiState.showInfoDialog) {
        val totalStrokes = uiState.document.layers.sumOf { it.strokes.size }
        val totalPoints = uiState.document.layers.sumOf { l -> l.strokes.sumOf { it.points.size } }
        AlertDialog(
            onDismissRequest = { viewModel.setInfoDialogVisible(false) },
            containerColor = MidnightSurface,
            shape = RoundedCornerShape(16.dp),
            title = { Text("Canvas Information", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Title: ${uiState.title}", color = TextSecondary)
                    Text(text = "Total Layers: ${uiState.document.layers.size}", color = TextSecondary)
                    Text(text = "Total Strokes: $totalStrokes", color = TextSecondary)
                    Text(text = "Vector Points: $totalPoints", color = TextSecondary)
                    Text(text = "Background: ${uiState.document.backgroundStyle.name}", color = TextSecondary)
                    Text(text = "Format: Fotara Vector Canvas 1.5.0", color = TextMuted, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.setInfoDialogVisible(false) }) {
                    Text("OK", color = FolderTabCream)
                }
            }
        )
    }

    // Note Scheduling Dialog (Milestone 2 Integration)
    if (uiState.showScheduleDialog && uiState.canvasId != null) {
        val scheduleManager = remember { NoteScheduleManager(context) }
        val initialAlertType = try {
            ScheduleAlertType.valueOf(uiState.alertType ?: "NOTIFICATION")
        } catch (_: Exception) {
            ScheduleAlertType.NOTIFICATION
        }
        ScheduleNoteDialog(
            noteTitle = uiState.title,
            initialScheduledAt = uiState.scheduledAt,
            initialAlertType = initialAlertType,
            onDismiss = { viewModel.setScheduleDialogVisible(false) },
            onSaveSchedule = { scheduledAt, alertType ->
                scheduleManager.scheduleNote(
                    noteType = ScheduleNoteType.CANVAS_NOTE,
                    noteId = uiState.canvasId!!,
                    folderId = uiState.folderId,
                    title = uiState.title,
                    triggerAtMillis = scheduledAt,
                    alertType = alertType
                )
                viewModel.setScheduleDialogVisible(false)
            },
            onClearSchedule = {
                scheduleManager.cancelSchedule(ScheduleNoteType.CANVAS_NOTE, uiState.canvasId!!)
                viewModel.setScheduleDialogVisible(false)
            }
        )
    }
}

@Composable
private fun ToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) FolderBodyBlue else Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, FolderTabCream) else null,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) FolderTabCream else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
