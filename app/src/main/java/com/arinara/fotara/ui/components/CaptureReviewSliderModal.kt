// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.arinara.fotara.ocr.FolderSuggestEngine
import com.arinara.fotara.ocr.FolderSuggestion
import com.arinara.fotara.ocr.OcrResult
import com.arinara.fotara.data.model.Folder
import java.io.File
import java.io.FileOutputStream
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.model.Subfolder
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import com.arinara.fotara.ui.components.presets.AppButton
import com.arinara.fotara.ui.components.presets.AppButtonVariant
import kotlinx.coroutines.launch

@Composable
fun CaptureReviewSliderModal(
    folderName: String,
    initialPhotos: List<Photo>,
    subfolders: List<Subfolder>,
    onSaveBatch: (List<Photo>, Long?) -> Unit,
    onDismiss: () -> Unit,
    folderSuggestEngine: FolderSuggestEngine? = null,
    availableFolders: List<Folder> = emptyList(),
    subfoldersByFolder: Map<Long, List<Subfolder>> = emptyMap(),
    modifier: Modifier = Modifier
) {
    val reviewPhotos = remember(initialPhotos) {
        mutableStateListOf<Photo>().apply { addAll(initialPhotos) }
    }
    var selectedSubfolderId by remember { mutableStateOf<Long?>(null) }
    var showDiscardAllConfirm by remember { mutableStateOf(false) }
    var cropFeedbackMessage by remember { mutableStateOf<String?>(null) }

    val pagerState = rememberPagerState(pageCount = { reviewPhotos.size })
    val coroutineScope = rememberCoroutineScope()
    var isCurrentPhotoZoomed by remember { mutableStateOf(false) }

    val currentPhoto = reviewPhotos.getOrNull(pagerState.currentPage)
    val folderSuggestion: FolderSuggestion? = remember(currentPhoto?.id, currentPhoto?.ocrText) {
        if (currentPhoto != null && folderSuggestEngine != null && availableFolders.isNotEmpty()) {
            val text = currentPhoto.ocrText ?: ""
            val words = text.split(Regex("\\s+")).filter { it.length > 2 }
            val fakeResult = OcrResult(
                fullText = text,
                detectedSubjectHint = null,
                keywords = words.take(8),
                processingTimeMs = 0L
            )
            folderSuggestEngine.suggestFolder(fakeResult, availableFolders, subfoldersByFolder)
        } else null
    }

    LaunchedEffect(pagerState.currentPage) {
        isCurrentPhotoZoomed = false
    }

    if (showDiscardAllConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardAllConfirm = false },
            containerColor = ElectricSlateTokens.SurfaceElevated,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Discard Captured Notes?",
                    color = ElectricSlateTokens.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to discard these ${reviewPhotos.size} notes without saving?",
                    color = ElectricSlateTokens.TextMuted,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                AppButton(
                    text = "Discard",
                    onClick = {
                        showDiscardAllConfirm = false
                        onDismiss()
                    },
                    variant = AppButtonVariant.DESTRUCTIVE
                )
            },
            dismissButton = {
                AppButton(
                    text = "Keep Reviewing",
                    onClick = { showDiscardAllConfirm = false },
                    variant = AppButtonVariant.SECONDARY
                )
            }
        )
    }

    Dialog(
        onDismissRequest = { showDiscardAllConfirm = true },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        // Scrim background (70% BgCanvas)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ElectricSlateTokens.BgCanvas.copy(alpha = 0.70f))
                .clickable { showDiscardAllConfirm = true },
            contentAlignment = Alignment.Center
        ) {
            // Floating Popup Slider Card: 90% Width, 75% Height
            Surface(
                modifier = modifier
                    .fillMaxWidth(0.90f)
                    .fillMaxHeight(0.75f)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(24.dp))
                    .clickable(enabled = false) {}, // Prevent tap propagation to scrim
                color = ElectricSlateTokens.SurfacePanel,
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                ) {
                    // Header: Review Count Badge, Crop Action, and Trash Discard Action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = ElectricSlateTokens.TrackInactive,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = ElectricSlateTokens.PrimaryAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Review: ${reviewPhotos.size} notes captured",
                                    style = TextStyle(
                                        color = ElectricSlateTokens.TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Perspective Crop / 90° Rotate Adjustment Button
                        IconButton(
                            onClick = {
                                if (reviewPhotos.isNotEmpty()) {
                                    val currentIndex = pagerState.currentPage.coerceIn(0, reviewPhotos.size - 1)
                                    val photoToRotate = reviewPhotos[currentIndex]
                                    val photoFile = File(photoToRotate.fileUri)
                                    if (photoFile.exists()) {
                                        try {
                                            val bmp = BitmapFactory.decodeFile(photoFile.absolutePath)
                                            if (bmp != null) {
                                                val matrix = Matrix().apply { postRotate(90f) }
                                                val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                                                FileOutputStream(photoFile).use { out ->
                                                    rotated.compress(Bitmap.CompressFormat.JPEG, 92, out)
                                                }
                                                if (rotated != bmp) bmp.recycle()
                                                rotated.recycle()

                                                reviewPhotos[currentIndex] = photoToRotate.copy(
                                                    addedAt = System.currentTimeMillis()
                                                )
                                                cropFeedbackMessage = "Adjusted & rotated 90°"
                                            }
                                        } catch (e: Exception) {
                                            cropFeedbackMessage = "Adjustment failed: ${e.message}"
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Crop,
                                contentDescription = "Adjust Perspective Crop",
                                tint = ElectricSlateTokens.TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Per-Photo Discard Button
                        IconButton(
                            onClick = {
                                if (reviewPhotos.isNotEmpty()) {
                                    val currentIndex = pagerState.currentPage.coerceIn(0, reviewPhotos.size - 1)
                                    reviewPhotos.removeAt(currentIndex)
                                    if (reviewPhotos.isEmpty()) {
                                        onDismiss()
                                    }
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Discard This Note",
                                tint = ElectricSlateTokens.Destructive,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Close Dialog Button
                        IconButton(
                            onClick = { showDiscardAllConfirm = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = ElectricSlateTokens.TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (cropFeedbackMessage != null) {
                        Surface(
                            color = ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = cropFeedbackMessage ?: "",
                                color = ElectricSlateTokens.PrimaryAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Body: Horizontal Pager with Snap-to-Center Physics
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(ElectricSlateTokens.BgCanvas)
                            .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(16.dp))
                    ) {
                        if (reviewPhotos.isNotEmpty()) {
                            HorizontalPager(
                                state = pagerState,
                                userScrollEnabled = !isCurrentPhotoZoomed,
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                pageSpacing = 12.dp,
                                modifier = Modifier.fillMaxSize()
                            ) { page ->
                                val photo = reviewPhotos[page]
                                CaptureReviewPage(
                                    photo = photo,
                                    pageNumber = page + 1,
                                    totalPages = reviewPhotos.size,
                                    onCaptionChange = { newCaption ->
                                        reviewPhotos[page] = photo.copy(caption = newCaption.ifBlank { null })
                                    },
                                    onZoomChanged = { isZoomed ->
                                        if (pagerState.currentPage == page) {
                                            isCurrentPhotoZoomed = isZoomed
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Optional Subfolder Assignment Row
                    if (subfolders.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = "File to:",
                                color = ElectricSlateTokens.TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(8.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (folderSuggestion?.suggestedSubfolderName != null) {
                                    item {
                                        Surface(
                                            color = ElectricSlateTokens.SurfaceElevated,
                                            shape = RoundedCornerShape(12.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricSlateTokens.PrimaryAccent),
                                            modifier = Modifier.clickable {
                                                selectedSubfolderId = folderSuggestion.suggestedSubfolderId
                                            }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = ElectricSlateTokens.PrimaryAccent,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = folderSuggestion.suggestedSubfolderName,
                                                    color = ElectricSlateTokens.PrimaryAccent,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                item {
                                    Surface(
                                        color = if (selectedSubfolderId == null) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TrackInactive,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.clickable { selectedSubfolderId = null }
                                    ) {
                                        Text(
                                            text = "All Notes",
                                            color = if (selectedSubfolderId == null) ElectricSlateTokens.TextPrimary else ElectricSlateTokens.TextMuted,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                items(subfolders, key = { it.id }) { sub ->
                                    Surface(
                                        color = if (selectedSubfolderId == sub.id) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TrackInactive,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.clickable { selectedSubfolderId = sub.id }
                                    ) {
                                        Text(
                                            text = sub.name,
                                            color = if (selectedSubfolderId == sub.id) ElectricSlateTokens.TextPrimary else ElectricSlateTokens.TextMuted,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Footer Primary Action: "Save to [Folder Name]"
                    AppButton(
                        text = "Save to $folderName",
                        onClick = {
                            if (reviewPhotos.isNotEmpty()) {
                                onSaveBatch(reviewPhotos.toList(), selectedSubfolderId)
                            }
                        },
                        leadingIcon = Icons.Default.Check,
                        variant = AppButtonVariant.PRIMARY,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CaptureReviewPage(
    photo: Photo,
    pageNumber: Int,
    totalPages: Int,
    onCaptionChange: (String) -> Unit = {},
    onZoomChanged: (Boolean) -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(ElectricSlateTokens.BgCanvas),
        contentAlignment = Alignment.Center
    ) {
        val file = java.io.File(photo.fileUri)
        if (file.exists()) {
            ZoomablePhotoViewport(
                photo = photo,
                onZoomChanged = onZoomChanged,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ElectricSlateTokens.SurfacePanel)
                        .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = ElectricSlateTokens.PrimaryAccent,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = photo.caption ?: "Captured Note #$pageNumber",
                    color = ElectricSlateTokens.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Overlay status & editable caption at bottom
        Surface(
            color = ElectricSlateTokens.SurfacePanel.copy(alpha = 0.85f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Page $pageNumber of $totalPages",
                        color = ElectricSlateTokens.PrimaryAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (!photo.ocrText.isNullOrBlank()) {
                        Text(
                            text = "“${photo.ocrText.take(40)}...”",
                            color = ElectricSlateTokens.TextMuted,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                // Editable caption field (inline rename)
                var currentText by remember(photo.id, photo.caption) {
                    mutableStateOf(photo.caption ?: "")
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = currentText,
                    onValueChange = {
                        currentText = it
                        onCaptionChange(it)
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = ElectricSlateTokens.TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElectricSlateTokens.SurfaceElevated)
                        .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    decorationBox = { innerTextField ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit caption",
                                tint = ElectricSlateTokens.PrimaryAccent,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (currentText.isEmpty()) {
                                    Text(
                                        text = "Add caption / rename note...",
                                        color = ElectricSlateTokens.TextMuted.copy(alpha = 0.7f),
                                        fontSize = 12.sp
                                    )
                                }
                                innerTextField()
                            }
                        }
                    }
                )
            }
        }
    }
}
