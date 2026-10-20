// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.canvas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.arinara.fotara.canvas.model.CanvasBackgroundStyle
import com.arinara.fotara.canvas.model.TextLayerElement
import com.arinara.fotara.canvas.model.TextBackgroundStyle
import com.arinara.fotara.canvas.model.TextLayerAlignment
import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.model.Workspace
import com.arinara.fotara.ui.components.presets.AppButton
import com.arinara.fotara.ui.components.presets.AppButtonVariant
import com.arinara.fotara.ui.components.presets.AppSlider
import com.arinara.fotara.ui.components.workspaceGroupedFolderItems
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CanvasRenameDialog(
    initialTitle: String,
    onDismissRequest: () -> Unit,
    onSave: (String) -> Unit
) {
    var renameInput by remember(initialTitle) { mutableStateOf(initialTitle) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = "Rename Canvas",
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            OutlinedTextField(
                value = renameInput,
                onValueChange = { renameInput = it },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = ElectricSlateTokens.TextPrimary,
                    unfocusedTextColor = ElectricSlateTokens.TextPrimary,
                    focusedBorderColor = ElectricSlateTokens.PrimaryAccent,
                    unfocusedBorderColor = ElectricSlateTokens.BorderSubtle
                )
            )
        },
        confirmButton = {
            AppButton(
                text = "Save",
                onClick = { onSave(renameInput) },
                variant = AppButtonVariant.PRIMARY,
                height = 36.dp
            )
        },
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismissRequest,
                variant = AppButtonVariant.SECONDARY,
                height = 36.dp
            )
        },
        containerColor = ElectricSlateTokens.SurfacePanel
    )
}

@Composable
fun CanvasBackgroundPickerDialog(
    currentStyle: CanvasBackgroundStyle,
    onDismissRequest: () -> Unit,
    onSelectStyle: (CanvasBackgroundStyle) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = "Background Pattern",
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                listOf(
                    Pair(CanvasBackgroundStyle.BLANK, "Blank Canvas"),
                    Pair(CanvasBackgroundStyle.DOTS, "Dot Grid"),
                    Pair(CanvasBackgroundStyle.GRID, "Squared Grid"),
                    Pair(CanvasBackgroundStyle.RULED, "Ruled Lines")
                ).forEach { (style, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectStyle(style) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentStyle == style,
                            onClick = { onSelectStyle(style) },
                            colors = RadioButtonDefaults.colors(selectedColor = ElectricSlateTokens.PrimaryAccent)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, color = ElectricSlateTokens.TextPrimary, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            AppButton(
                text = "Done",
                onClick = onDismissRequest,
                variant = AppButtonVariant.PRIMARY,
                height = 36.dp
            )
        },
        containerColor = ElectricSlateTokens.SurfacePanel
    )
}

@Composable
fun CanvasSettingsDialog(
    stylusOnlyDrawing: Boolean,
    onToggleStylusOnly: (Boolean) -> Unit,
    onTogglePalmRejection: (Boolean) -> Unit,
    onOpenBackgroundPicker: () -> Unit,
    onOpenScheduleNote: () -> Unit,
    onOpenNoteInfo: () -> Unit,
    hasSchedule: Boolean,
    onCopyFrameStats: (() -> Unit)? = null,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = "Canvas Settings",
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
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
                        Text("Stylus-Only Drawing", color = ElectricSlateTokens.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Finger only pans and zooms; only pen draws.", color = ElectricSlateTokens.TextMuted, fontSize = 12.sp)
                    }
                    Switch(
                        checked = stylusOnlyDrawing,
                        onCheckedChange = onToggleStylusOnly,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ElectricSlateTokens.PrimaryAccent,
                            checkedTrackColor = ElectricSlateTokens.SurfaceElevated
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Palm Rejection", color = ElectricSlateTokens.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Ignores broad hand touches when pen is active.", color = ElectricSlateTokens.TextMuted, fontSize = 12.sp)
                    }
                    Switch(
                        checked = true,
                        onCheckedChange = onTogglePalmRejection,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ElectricSlateTokens.PrimaryAccent,
                            checkedTrackColor = ElectricSlateTokens.SurfaceElevated
                        )
                    )
                }

                HorizontalDivider(color = ElectricSlateTokens.BorderSubtle, modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenBackgroundPicker() }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = ElectricSlateTokens.PrimaryAccent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Background Style", color = ElectricSlateTokens.TextPrimary, fontSize = 14.sp)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenScheduleNote() }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = if (hasSchedule) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Schedule Note", color = ElectricSlateTokens.TextPrimary, fontSize = 14.sp)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenNoteInfo() }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = ElectricSlateTokens.PrimaryAccent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Note Info", color = ElectricSlateTokens.TextPrimary, fontSize = 14.sp)
                }

                if (onCopyFrameStats != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCopyFrameStats() }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = ElectricSlateTokens.PrimaryAccent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Copy Frame Stats", color = ElectricSlateTokens.TextPrimary, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            AppButton(
                text = "Done",
                onClick = onDismissRequest,
                variant = AppButtonVariant.PRIMARY,
                height = 36.dp
            )
        },
        containerColor = ElectricSlateTokens.SurfacePanel
    )
}

@Composable
fun CanvasInfoDialog(
    title: String,
    createdAt: Long,
    layerCount: Int,
    elementCount: Int,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = "Canvas Info",
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                val addedStr = SimpleDateFormat("MMMM d, yyyy · h:mm a", Locale.US).format(Date(createdAt))
                Text("Title: $title", color = ElectricSlateTokens.TextPrimary, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Created: $addedStr", color = ElectricSlateTokens.TextMuted, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Layers: $layerCount", color = ElectricSlateTokens.TextMuted, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Total Elements: $elementCount", color = ElectricSlateTokens.TextMuted, fontSize = 12.sp)
            }
        },
        confirmButton = {
            AppButton(
                text = "Close",
                onClick = onDismissRequest,
                variant = AppButtonVariant.PRIMARY,
                height = 36.dp
            )
        },
        containerColor = ElectricSlateTokens.SurfacePanel
    )
}

@Composable
fun CanvasMoveDialog(
    folders: List<Folder>,
    workspaces: List<Workspace>,
    onSelectFolder: (Long) -> Unit,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = "Move Canvas to Folder",
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                workspaceGroupedFolderItems(
                    folders = folders,
                    workspaces = workspaces,
                    keyPrefix = "canvas_move_folder"
                ) { folder ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectFolder(folder.id) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.DriveFileMove,
                            contentDescription = null,
                            tint = ElectricSlateTokens.PrimaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(folder.name, color = ElectricSlateTokens.TextPrimary, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismissRequest,
                variant = AppButtonVariant.SECONDARY,
                height = 36.dp
            )
        },
        containerColor = ElectricSlateTokens.SurfacePanel
    )
}

@Composable
fun CanvasDeleteConfirmDialog(
    onDismissRequest: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = "Move to Trash?",
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "This canvas will be moved to the Trash. You can restore it anytime within 30 days.",
                color = ElectricSlateTokens.TextMuted,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            AppButton(
                text = "Delete",
                onClick = onConfirmDelete,
                variant = AppButtonVariant.DESTRUCTIVE,
                height = 36.dp
            )
        },
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismissRequest,
                variant = AppButtonVariant.SECONDARY,
                height = 36.dp
            )
        },
        containerColor = ElectricSlateTokens.SurfacePanel
    )
}

@Composable
fun CanvasImageSourceDialog(
    onPickFromGallery: () -> Unit,
    onPickFromNotes: () -> Unit,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = "Add Image",
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPickFromGallery() }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = ElectricSlateTokens.PrimaryAccent, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("From Gallery / Files", color = ElectricSlateTokens.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Choose an image from your device storage", color = ElectricSlateTokens.TextMuted, fontSize = 12.sp)
                    }
                }

                HorizontalDivider(color = ElectricSlateTokens.BorderSubtle)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPickFromNotes() }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Collections, contentDescription = null, tint = ElectricSlateTokens.PrimaryAccent, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("From Fotara Notes", color = ElectricSlateTokens.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Choose a photo or scan from your existing notes", color = ElectricSlateTokens.TextMuted, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismissRequest,
                variant = AppButtonVariant.SECONDARY,
                height = 36.dp
            )
        },
        containerColor = ElectricSlateTokens.SurfacePanel
    )
}

@Composable
fun CanvasExistingNotesDialog(
    isLoading: Boolean,
    existingPhotos: List<Photo>,
    onSelectPhoto: (Photo) -> Unit,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = "Import from Notes",
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 360.dp)
            ) {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ElectricSlateTokens.PrimaryAccent, strokeWidth = 2.dp)
                    }
                } else if (existingPhotos.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No photo notes found.", color = ElectricSlateTokens.TextMuted, fontSize = 14.sp)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(existingPhotos) { photo ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
                                color = ElectricSlateTokens.SurfaceElevated,
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clickable { onSelectPhoto(photo) }
                            ) {
                                AsyncImage(
                                    model = photo.fileUri,
                                    contentDescription = photo.caption ?: "Note photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismissRequest,
                variant = AppButtonVariant.SECONDARY,
                height = 36.dp
            )
        },
        containerColor = ElectricSlateTokens.SurfacePanel
    )
}

@Composable
fun CanvasExportDialog(
    hasSelection: Boolean,
    selectedCount: Int,
    isExporting: Boolean,
    exportProgress: Float,
    onExport: (exportSelection: Boolean, exportScale: Float) -> Unit,
    onDismissRequest: () -> Unit
) {
    var exportSelection by remember { mutableStateOf(false) }
    var exportScale by remember { mutableStateOf(1.0f) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = "Export & Share PNG",
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text("Content Range:", color = ElectricSlateTokens.TextMuted, fontSize = 12.sp)
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
                        colors = RadioButtonDefaults.colors(selectedColor = ElectricSlateTokens.PrimaryAccent)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Whole Canvas Content", color = ElectricSlateTokens.TextPrimary, fontSize = 14.sp)
                }

                if (hasSelection) {
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
                            colors = RadioButtonDefaults.colors(selectedColor = ElectricSlateTokens.PrimaryAccent)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Selected Elements Only ($selectedCount)", color = ElectricSlateTokens.TextPrimary, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Export Scale:", color = ElectricSlateTokens.TextMuted, fontSize = 12.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier.clickable { exportScale = 1.0f },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = exportScale == 1.0f,
                            onClick = { exportScale = 1.0f },
                            colors = RadioButtonDefaults.colors(selectedColor = ElectricSlateTokens.PrimaryAccent)
                        )
                        Text("1x Standard", color = ElectricSlateTokens.TextPrimary, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Row(
                        modifier = Modifier.clickable { exportScale = 2.0f },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = exportScale == 2.0f,
                            onClick = { exportScale = 2.0f },
                            colors = RadioButtonDefaults.colors(selectedColor = ElectricSlateTokens.PrimaryAccent)
                        )
                        Text("2x High-Res", color = ElectricSlateTokens.TextPrimary, fontSize = 14.sp)
                    }
                }

                if (isExporting) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator(
                        progress = { exportProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = ElectricSlateTokens.PrimaryAccent
                    )
                }
            }
        },
        confirmButton = {
            AppButton(
                text = "Export & Share",
                onClick = { onExport(exportSelection, exportScale) },
                enabled = !isExporting,
                variant = AppButtonVariant.PRIMARY,
                height = 36.dp
            )
        },
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismissRequest,
                enabled = !isExporting,
                variant = AppButtonVariant.SECONDARY,
                height = 36.dp
            )
        },
        containerColor = ElectricSlateTokens.SurfacePanel
    )
}

@Composable
fun CanvasTextOptionsDialog(
    editingTextLayer: TextLayerElement?,
    defaultFontSize: Float,
    defaultFontWeight: Int,
    defaultColor: Long,
    defaultBackgroundStyle: TextBackgroundStyle,
    recentColors: List<Long>,
    onSave: (text: String, fontSizeSp: Float, color: Long, fontWeight: Int, backgroundStyle: TextBackgroundStyle, alignment: TextLayerAlignment) -> Unit,
    onDelete: (() -> Unit)? = null,
    onDismissRequest: () -> Unit
) {
    var textInput by remember(editingTextLayer) { mutableStateOf(editingTextLayer?.text ?: "Text Annotation") }
    var fontSize by remember(editingTextLayer) { mutableStateOf(editingTextLayer?.fontSizeSp ?: defaultFontSize) }
    var fontWeight by remember(editingTextLayer) { mutableStateOf(editingTextLayer?.fontWeight ?: defaultFontWeight) }
    var textColor by remember(editingTextLayer) { mutableStateOf(editingTextLayer?.color ?: defaultColor) }
    var bgStyle by remember(editingTextLayer) { mutableStateOf(editingTextLayer?.backgroundStyle ?: defaultBackgroundStyle) }
    var alignment by remember(editingTextLayer) { mutableStateOf(editingTextLayer?.alignment ?: TextLayerAlignment.LEFT) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = if (editingTextLayer != null) "Edit Text Layer" else "Place Text Layer",
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text("Annotation Text") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ElectricSlateTokens.TextPrimary,
                        unfocusedTextColor = ElectricSlateTokens.TextPrimary,
                        focusedBorderColor = ElectricSlateTokens.PrimaryAccent,
                        unfocusedBorderColor = ElectricSlateTokens.BorderSubtle,
                        cursorColor = ElectricSlateTokens.PrimaryAccent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Text Alignment", color = ElectricSlateTokens.TextMuted, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val aligns = listOf(
                        TextLayerAlignment.LEFT to "Left",
                        TextLayerAlignment.CENTER to "Center",
                        TextLayerAlignment.RIGHT to "Right"
                    )
                    for ((a, label) in aligns) {
                        val isSel = alignment == a
                        Surface(
                            color = if (isSel) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.SurfaceElevated,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isSel) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.BorderSubtle),
                            modifier = Modifier
                                .height(32.dp)
                                .clickable { alignment = a }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp)) {
                                Text(
                                    label,
                                    fontSize = 12.sp,
                                    color = if (isSel) Color.White else ElectricSlateTokens.TextMuted
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Font Size", color = ElectricSlateTokens.TextMuted, fontSize = 14.sp)
                    Text("${fontSize.toInt()} sp", color = ElectricSlateTokens.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                AppSlider(
                    value = fontSize,
                    onValueChange = { fontSize = it },
                    valueRange = 12f..48f,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Font Weight", color = ElectricSlateTokens.TextMuted, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val weights = listOf(400 to "Regular", 500 to "Medium", 700 to "Bold")
                    for ((w, label) in weights) {
                        val isSel = fontWeight == w
                        Surface(
                            color = if (isSel) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.SurfaceElevated,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isSel) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.BorderSubtle),
                            modifier = Modifier
                                .height(32.dp)
                                .clickable { fontWeight = w }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                                Text(
                                    label,
                                    fontSize = 12.sp,
                                    color = if (isSel) Color.White else ElectricSlateTokens.TextMuted
                                )
                            }
                        }
                    }
                }

                Text("Card Background", color = ElectricSlateTokens.TextMuted, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val styles = listOf(
                        TextBackgroundStyle.TRANSPARENT to "Transparent",
                        TextBackgroundStyle.FROSTED_DARK to "Frosted Dark",
                        TextBackgroundStyle.SOLID_LIGHT to "Solid Light"
                    )
                    for ((s, label) in styles) {
                        val isSel = bgStyle == s
                        Surface(
                            color = if (isSel) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.SurfaceElevated,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isSel) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.BorderSubtle),
                            modifier = Modifier
                                .height(32.dp)
                                .clickable { bgStyle = s }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                                Text(
                                    label,
                                    fontSize = 12.sp,
                                    color = if (isSel) Color.White else ElectricSlateTokens.TextMuted
                                )
                            }
                        }
                    }
                }

                Text("Text Color", color = ElectricSlateTokens.TextMuted, fontSize = 14.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(recentColors) { colLong ->
                        val isSel = textColor == colLong
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(colLong.toInt()))
                                .border(
                                    width = if (isSel) 2.dp else 1.dp,
                                    color = if (isSel) ElectricSlateTokens.TextPrimary else ElectricSlateTokens.BorderSubtle,
                                    shape = CircleShape
                                )
                                .clickable { textColor = colLong },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSel) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (colLong == 0xFFFFFFFF) ElectricSlateTokens.BgCanvas else ElectricSlateTokens.TextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            AppButton(
                text = "Save",
                onClick = {
                    onSave(textInput, fontSize, textColor, fontWeight, bgStyle, alignment)
                },
                variant = AppButtonVariant.PRIMARY,
                height = 36.dp
            )
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (editingTextLayer != null && onDelete != null) {
                    AppButton(
                        text = "Delete",
                        onClick = onDelete,
                        variant = AppButtonVariant.DESTRUCTIVE,
                        height = 36.dp
                    )
                }
                AppButton(
                    text = "Cancel",
                    onClick = onDismissRequest,
                    variant = AppButtonVariant.SECONDARY,
                    height = 36.dp
                )
            }
        },
        containerColor = ElectricSlateTokens.SurfacePanel
    )
}
