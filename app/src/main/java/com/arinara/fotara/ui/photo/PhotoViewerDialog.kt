// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.photo

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.ui.components.ZoomablePhotoViewport
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.hypot

@Composable
fun PhotoViewerDialog(
    photo: Photo,
    photos: List<Photo> = listOf(photo),
    onDismiss: () -> Unit,
    onSaveNote: ((String?) -> Unit)? = null,
    onAddTag: ((String) -> Unit)? = null,
    onRemoveTag: ((String) -> Unit)? = null,
    onRotatePhoto: ((Photo) -> Unit)? = null,
    onCropPhoto: ((photo: Photo, left: Float, top: Float, right: Float, bottom: Float) -> Unit)? = null,
    onRemoveFromGroup: ((Photo) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val initialIndex = remember(photo, photos) {
        val idx = photos.indexOfFirst { it.id == photo.id }
        if (idx >= 0) idx else 0
    }
    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { photos.size })
    var isCurrentPhotoZoomed by remember { mutableStateOf(false) }
    var currentPhotoOverride by remember(pagerState.currentPage) { mutableStateOf<Photo?>(null) }

    LaunchedEffect(pagerState.currentPage) {
        isCurrentPhotoZoomed = false
        currentPhotoOverride = null
    }

    val currentPhoto = currentPhotoOverride ?: (photos.getOrNull(pagerState.currentPage) ?: photo)
    var imageVersion by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var isCropping by remember { mutableStateOf(false) }
    var isOperating by remember { mutableStateOf(false) }

    var isOcrExpanded by remember { mutableStateOf(false) }
    var copyMessage by remember { mutableStateOf<String?>(null) }

    val formattedDate = remember(currentPhoto.addedAt) {
        SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault()).format(Date(currentPhoto.addedAt))
    }

    Dialog(
        onDismissRequest = {
            if (isCropping) {
                isCropping = false
            } else {
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        BackHandler {
            if (isCropping) {
                isCropping = false
            } else {
                onDismiss()
            }
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MidnightNavy)
        ) {
            if (isCropping) {
                // Interactive 4-Corner Crop Editor
                InteractiveCropEditor(
                    photo = currentPhoto,
                    imageVersion = imageVersion,
                    onApplyCrop = { left, top, right, bottom ->
                        isOperating = true
                        onCropPhoto?.invoke(currentPhoto, left, top, right, bottom)
                        imageVersion = System.currentTimeMillis()
                        isOperating = false
                        isCropping = false
                    },
                    onCancel = { isCropping = false }
                )
            } else {
                // Main Horizontal Pager with Zoom-Gated Swipe
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = !isCurrentPhotoZoomed,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val pagePhoto = photos[page]
                        ZoomablePhotoViewport(
                            photo = pagePhoto,
                            imageVersion = imageVersion,
                            onZoomChanged = { isZoomed ->
                                if (pagerState.currentPage == page) {
                                    isCurrentPhotoZoomed = isZoomed
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Top Bar: Navigation, Title, Rotate, Crop & Export Controls
                Surface(
                    color = MidnightNavy.copy(alpha = 0.92f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .windowInsetsPadding(WindowInsets.statusBars)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Close Note Inspector",
                                tint = FolderTabCream
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 6.dp)
                        ) {
                            Text(
                                text = if (photos.size > 1) {
                                    "${currentPhoto.caption ?: "Note"} (${pagerState.currentPage + 1}/${photos.size})"
                                } else {
                                    currentPhoto.caption ?: "Note Inspector"
                                },
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isCurrentPhotoZoomed) {
                                    "Zoomed: Pan enabled · Double-tap to reset"
                                } else if (photos.size > 1) {
                                    "Swipe next/prev · Pinch or double-tap to zoom"
                                } else {
                                    "Pinch or double-tap to zoom · 100% Offline"
                                },
                                color = if (isCurrentPhotoZoomed) TagAmber else TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        // Rotate 90° Clockwise Button
                        IconButton(
                            onClick = {
                                isOperating = true
                                onRotatePhoto?.invoke(currentPhoto)
                                imageVersion = System.currentTimeMillis()
                                isOperating = false
                            },
                            enabled = !isOperating
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.RotateRight,
                                contentDescription = "Rotate 90° Clockwise",
                                tint = FolderTabCream
                            )
                        }

                        // Re-Crop Button
                        IconButton(
                            onClick = { isCropping = true },
                            enabled = !isOperating
                        ) {
                            Icon(
                                imageVector = Icons.Default.Crop,
                                contentDescription = "Adjust Crop",
                                tint = FolderTabCream
                            )
                        }

                        // Remove from group Button (strictly when viewed within a group)
                        if (onRemoveFromGroup != null) {
                            IconButton(
                                onClick = { onRemoveFromGroup.invoke(currentPhoto) },
                                enabled = !isOperating
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LayersClear,
                                    contentDescription = "Remove from group",
                                    tint = TagCrimson
                                )
                            }
                        }

                        // Share Button
                        IconButton(onClick = {
                            val file = File(currentPhoto.fileUri)
                            if (file.exists()) {
                                try {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/jpeg"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Note"))
                                } catch (_: Exception) {}
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Note",
                                tint = FolderTabCream
                            )
                        }
                    }
                }

                // Expandable Bottom OCR Sheet
                Surface(
                    color = MidnightSurface.copy(alpha = 0.96f),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MidnightCardOutline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Smart Tags
                        var isAddingTag by remember { mutableStateOf(false) }
                        var newTagText by remember { mutableStateOf("") }
                        val activeTags = currentPhoto.getAllSmartTags()

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Smart Tags",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (activeTags.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "(${activeTags.size})",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            if (!isAddingTag && onAddTag != null) {
                                TextButton(onClick = { isAddingTag = true }) {
                                    Text(
                                        text = "+ Add Tag",
                                        color = FolderTabCream,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (isAddingTag) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newTagText,
                                    onValueChange = { newTagText = it.removePrefix("#").replace(" ", "") },
                                    placeholder = { Text("e.g. formula, exam", color = TextMuted, fontSize = 12.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = FolderBodyBlue,
                                        unfocusedBorderColor = MidnightCardOutline
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        val clean = newTagText.trim().lowercase()
                                        if (clean.isNotBlank()) {
                                            onAddTag?.invoke(clean)
                                            val existing = currentPhoto.tags?.split(',', ' ')?.filter { it.isNotBlank() } ?: emptyList()
                                            currentPhotoOverride = currentPhoto.copy(tags = (existing + clean).joinToString(","))
                                            newTagText = ""
                                            isAddingTag = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = FolderBodyBlue),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Add", color = Color.White, fontSize = 12.sp)
                                }
                                TextButton(onClick = { isAddingTag = false; newTagText = "" }) {
                                    Text("Cancel", color = TextSecondary, fontSize = 12.sp)
                                }
                            }
                        }

                        if (activeTags.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(bottom = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                activeTags.forEach { tag ->
                                    Surface(
                                        color = TagAmber.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, TagAmber.copy(alpha = 0.6f))
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "#$tag",
                                                color = TagAmber,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            if (onRemoveTag != null) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove tag",
                                                    tint = TagAmber,
                                                    modifier = Modifier
                                                        .size(12.dp)
                                                        .clickable {
                                                            onRemoveTag.invoke(tag)
                                                            val existing = currentPhoto.tags?.split(',', ' ')?.map { it.trim().lowercase() }?.filter { it.isNotBlank() } ?: emptyList()
                                                            val remaining = existing.filter { it != tag }
                                                            currentPhotoOverride = currentPhoto.copy(tags = if (remaining.isEmpty()) null else remaining.joinToString(","))
                                                        }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Personal Study Notes
                        var isEditingNote by remember { mutableStateOf(false) }
                        var noteText by remember(currentPhoto.note) { mutableStateOf(currentPhoto.note ?: "") }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = FolderTabCream,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Personal Study Notes",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (!isEditingNote && onSaveNote != null) {
                                TextButton(onClick = { isEditingNote = true }) {
                                    Text(
                                        text = if (currentPhoto.note.isNullOrBlank()) "+ Add" else "Edit",
                                        color = FolderTabCream,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (isEditingNote) {
                            Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                                OutlinedTextField(
                                    value = noteText,
                                    onValueChange = { noteText = it },
                                    placeholder = { Text("Add personal study note, formula tip, or reminder...", color = TextMuted) },
                                    maxLines = 4,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = FolderBodyBlue,
                                        unfocusedBorderColor = MidnightCardOutline
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = {
                                        noteText = currentPhoto.note ?: ""
                                        isEditingNote = false
                                    }) {
                                        Text("Cancel", color = TextSecondary)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            onSaveNote?.invoke(noteText.ifBlank { null })
                                            currentPhotoOverride = currentPhoto.copy(note = noteText.ifBlank { null })
                                            isEditingNote = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = FolderBodyBlue),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Save Note", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else if (!currentPhoto.note.isNullOrBlank()) {
                            Text(
                                text = currentPhoto.note,
                                color = TextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            )
                        } else {
                            Text(
                                text = "No study notes added yet.",
                                color = TextMuted,
                                fontSize = 12.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MidnightCardOutline))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Header of OCR drawer
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isOcrExpanded = !isOcrExpanded },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = FolderTabCream,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Recognized OCR Text",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                imageVector = if (isOcrExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                contentDescription = if (isOcrExpanded) "Collapse" else "Expand",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        AnimatedVisibility(visible = isOcrExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                            ) {
                                val textToDisplay = currentPhoto.ocrText ?: "No text recognized in this image."
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MidnightNavy)
                                        .border(1.dp, MidnightCardOutline, RoundedCornerShape(10.dp))
                                        .padding(10.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = textToDisplay,
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 17.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (copyMessage != null) {
                                        Text(
                                            text = copyMessage ?: "",
                                            color = FolderTabCream,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.width(1.dp))
                                    }

                                    Button(
                                        onClick = {
                                            currentPhoto.ocrText?.let {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                                val clip = ClipData.newPlainText("Recognized OCR Text", it)
                                                clipboard?.setPrimaryClip(clip)
                                                copyMessage = "Copied to clipboard!"
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = FolderBodyBlue,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = null,
                                            tint = FolderTabCream,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Copy All Text", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class ActiveCorner {
    NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, BODY
}

@Composable
private fun InteractiveCropEditor(
    photo: Photo,
    imageVersion: Long,
    onApplyCrop: (left: Float, top: Float, right: Float, bottom: Float) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var cropLeft by remember { mutableFloatStateOf(0.08f) }
    var cropTop by remember { mutableFloatStateOf(0.08f) }
    var cropRight by remember { mutableFloatStateOf(0.92f) }
    var cropBottom by remember { mutableFloatStateOf(0.92f) }

    var activeCorner by remember { mutableStateOf(ActiveCorner.NONE) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Top Bar
        Surface(
            color = MidnightNavy,
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                IconButton(onClick = onCancel) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel Crop", tint = FolderTabCream)
                }
                Text(
                    text = "Adjust Crop",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = {
                    cropLeft = 0.02f
                    cropTop = 0.02f
                    cropRight = 0.98f
                    cropBottom = 0.98f
                }) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset Crop", tint = FolderTabCream)
                }
            }
        }

        // Center Crop Canvas
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val canvasWidthPx = constraints.maxWidth.toFloat()
            val canvasHeightPx = constraints.maxHeight.toFloat()

            val file = File(photo.fileUri)
            if (file.exists()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(file)
                        .memoryCacheKey("${photo.fileUri}_$imageVersion")
                        .crossfade(false)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Interactive Drag Overlay
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(canvasWidthPx, canvasHeightPx) {
                        val touchRadiusPx = 48.dp.toPx()
                        detectDragGestures(
                            onDragStart = { downOffset ->
                                val curL = cropLeft * canvasWidthPx
                                val curT = cropTop * canvasHeightPx
                                val curR = cropRight * canvasWidthPx
                                val curB = cropBottom * canvasHeightPx

                                activeCorner = when {
                                    hypot(downOffset.x - curL, downOffset.y - curT) <= touchRadiusPx -> ActiveCorner.TOP_LEFT
                                    hypot(downOffset.x - curR, downOffset.y - curT) <= touchRadiusPx -> ActiveCorner.TOP_RIGHT
                                    hypot(downOffset.x - curL, downOffset.y - curB) <= touchRadiusPx -> ActiveCorner.BOTTOM_LEFT
                                    hypot(downOffset.x - curR, downOffset.y - curB) <= touchRadiusPx -> ActiveCorner.BOTTOM_RIGHT
                                    downOffset.x in curL..curR && downOffset.y in curT..curB -> ActiveCorner.BODY
                                    else -> ActiveCorner.NONE
                                }
                            },
                            onDragEnd = { activeCorner = ActiveCorner.NONE },
                            onDragCancel = { activeCorner = ActiveCorner.NONE },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val dx = dragAmount.x / canvasWidthPx
                                val dy = dragAmount.y / canvasHeightPx

                                when (activeCorner) {
                                    ActiveCorner.TOP_LEFT -> {
                                        cropLeft = (cropLeft + dx).coerceIn(0f, cropRight - 0.1f)
                                        cropTop = (cropTop + dy).coerceIn(0f, cropBottom - 0.1f)
                                    }
                                    ActiveCorner.TOP_RIGHT -> {
                                        cropRight = (cropRight + dx).coerceIn(cropLeft + 0.1f, 1f)
                                        cropTop = (cropTop + dy).coerceIn(0f, cropBottom - 0.1f)
                                    }
                                    ActiveCorner.BOTTOM_LEFT -> {
                                        cropLeft = (cropLeft + dx).coerceIn(0f, cropRight - 0.1f)
                                        cropBottom = (cropBottom + dy).coerceIn(cropTop + 0.1f, 1f)
                                    }
                                    ActiveCorner.BOTTOM_RIGHT -> {
                                        cropRight = (cropRight + dx).coerceIn(cropLeft + 0.1f, 1f)
                                        cropBottom = (cropBottom + dy).coerceIn(cropTop + 0.1f, 1f)
                                    }
                                    ActiveCorner.BODY -> {
                                        val boxWidth = cropRight - cropLeft
                                        val boxHeight = cropBottom - cropTop
                                        val newL = (cropLeft + dx).coerceIn(0f, 1f - boxWidth)
                                        val newT = (cropTop + dy).coerceIn(0f, 1f - boxHeight)
                                        cropLeft = newL
                                        cropRight = newL + boxWidth
                                        cropTop = newT
                                        cropBottom = newT + boxHeight
                                    }
                                    ActiveCorner.NONE -> {}
                                }
                            }
                        )
                    }
            ) {
                val leftPx = cropLeft * size.width
                val topPx = cropTop * size.height
                val rightPx = cropRight * size.width
                val bottomPx = cropBottom * size.height
                val rectW = rightPx - leftPx
                val rectH = bottomPx - topPx

                val scrimColor = Color.Black.copy(alpha = 0.62f)

                // 1. Scrim
                drawRect(scrimColor, Offset.Zero, Size(size.width, topPx))
                drawRect(scrimColor, Offset(0f, bottomPx), Size(size.width, size.height - bottomPx))
                drawRect(scrimColor, Offset(0f, topPx), Size(leftPx, rectH))
                drawRect(scrimColor, Offset(rightPx, topPx), Size(size.width - rightPx, rectH))

                // 2. Crop Frame Border
                drawRect(
                    color = FolderTabCream,
                    topLeft = Offset(leftPx, topPx),
                    size = Size(rectW, rectH),
                    style = Stroke(width = 2.dp.toPx())
                )

                // 3. Rule-of-Thirds Grid
                val oneThirdW = rectW / 3f
                val oneThirdH = rectH / 3f
                val gridColor = Color.White.copy(alpha = 0.28f)
                val gridStroke = Stroke(width = 1.dp.toPx())

                drawLine(gridColor, Offset(leftPx + oneThirdW, topPx), Offset(leftPx + oneThirdW, bottomPx), gridStroke.width)
                drawLine(gridColor, Offset(leftPx + 2 * oneThirdW, topPx), Offset(leftPx + 2 * oneThirdW, bottomPx), gridStroke.width)
                drawLine(gridColor, Offset(leftPx, topPx + oneThirdH), Offset(rightPx, topPx + oneThirdH), gridStroke.width)
                drawLine(gridColor, Offset(leftPx, topPx + 2 * oneThirdH), Offset(rightPx, topPx + 2 * oneThirdH), gridStroke.width)

                // 4. Corner Handles
                val bracketLen = 22.dp.toPx()
                val bracketStroke = Stroke(width = 4.dp.toPx())
                val cornerColor = FolderBodyBlue

                // Top-Left
                drawLine(cornerColor, Offset(leftPx, topPx), Offset(leftPx + bracketLen, topPx), bracketStroke.width)
                drawLine(cornerColor, Offset(leftPx, topPx), Offset(leftPx, topPx + bracketLen), bracketStroke.width)

                // Top-Right
                drawLine(cornerColor, Offset(rightPx, topPx), Offset(rightPx - bracketLen, topPx), bracketStroke.width)
                drawLine(cornerColor, Offset(rightPx, topPx), Offset(rightPx, topPx + bracketLen), bracketStroke.width)

                // Bottom-Left
                drawLine(cornerColor, Offset(leftPx, bottomPx), Offset(leftPx + bracketLen, bottomPx), bracketStroke.width)
                drawLine(cornerColor, Offset(leftPx, bottomPx), Offset(leftPx, bottomPx - bracketLen), bracketStroke.width)

                // Bottom-Right
                drawLine(cornerColor, Offset(rightPx, bottomPx), Offset(rightPx - bracketLen, bottomPx), bracketStroke.width)
                drawLine(cornerColor, Offset(rightPx, bottomPx), Offset(rightPx, bottomPx - bracketLen), bracketStroke.width)
            }
        }

        // Bottom Controls
        Surface(
            color = MidnightNavy,
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    border = ButtonDefaults.outlinedButtonBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MidnightCardOutline))
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        onApplyCrop(cropLeft, cropTop, cropRight, cropBottom)
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FolderBodyBlue)
                ) {
                    Icon(imageVector = Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Apply Crop", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
