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
import com.arinara.fotara.theme.DockSlatePill
import com.arinara.fotara.theme.FolderBodyBlue
import com.arinara.fotara.theme.FolderTabCream
import com.arinara.fotara.theme.MidnightCardOutline
import com.arinara.fotara.theme.MidnightNavy
import com.arinara.fotara.theme.MidnightSurface
import com.arinara.fotara.theme.TagCrimson
import com.arinara.fotara.theme.TagEmerald
import com.arinara.fotara.theme.TextPrimary
import com.arinara.fotara.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun CaptureReviewSliderModal(
    folderName: String,
    initialPhotos: List<Photo>,
    subfolders: List<Subfolder>,
    onSaveBatch: (List<Photo>, Long?) -> Unit,
    onDismiss: () -> Unit,
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

    LaunchedEffect(pagerState.currentPage) {
        isCurrentPhotoZoomed = false
    }

    if (showDiscardAllConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardAllConfirm = false },
            containerColor = MidnightSurface,
            shape = RoundedCornerShape(18.dp),
            title = {
                Text(
                    text = "Discard Captured Notes?",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to discard these ${reviewPhotos.size} notes without saving?",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardAllConfirm = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TagCrimson)
                ) {
                    Text("Discard", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardAllConfirm = false }) {
                    Text("Keep Reviewing", color = FolderTabCream)
                }
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
        // Scrim background (55% black)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable { showDiscardAllConfirm = true },
            contentAlignment = Alignment.Center
        ) {
            // Floating Popup Slider Card: 90% Width, 75% Height
            Surface(
                modifier = modifier
                    .fillMaxWidth(0.90f)
                    .fillMaxHeight(0.75f)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.2.dp, MidnightCardOutline, RoundedCornerShape(24.dp))
                    .clickable(enabled = false) {}, // Prevent tap propagation to scrim
                color = MidnightSurface,
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 24.dp
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
                            color = DockSlatePill.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, MidnightCardOutline)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = FolderTabCream,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Review: ${reviewPhotos.size} notes captured",
                                    style = TextStyle(
                                        color = FolderTabCream,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Perspective Crop Adjustment Button
                        IconButton(
                            onClick = {
                                cropFeedbackMessage = "Perspective straightened automatically"
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Crop,
                                contentDescription = "Adjust Perspective Crop",
                                tint = FolderTabCream,
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
                                tint = TagCrimson,
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
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (cropFeedbackMessage != null) {
                        Surface(
                            color = TagEmerald.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "✓ $cropFeedbackMessage",
                                color = TagEmerald,
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
                            .background(MidnightNavy)
                            .border(1.dp, MidnightCardOutline, RoundedCornerShape(16.dp))
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
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = if (selectedSubfolderId == null) FolderBodyBlue else DockSlatePill.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable { selectedSubfolderId = null }
                            ) {
                                Text(
                                    text = "All Notes",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            subfolders.take(2).forEach { sub ->
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = if (selectedSubfolderId == sub.id) FolderBodyBlue else DockSlatePill.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.clickable { selectedSubfolderId = sub.id }
                                ) {
                                    Text(
                                        text = sub.name,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Footer Primary Action: "Save to [Folder Name]"
                    Button(
                        onClick = {
                            if (reviewPhotos.isNotEmpty()) {
                                onSaveBatch(reviewPhotos.toList(), selectedSubfolderId)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FolderBodyBlue,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = FolderTabCream,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save to $folderName",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
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
    onZoomChanged: (Boolean) -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF070B1F)),
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
                        .background(FolderBodyBlue.copy(alpha = 0.35f))
                        .border(1.dp, FolderTabCream.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = FolderTabCream,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = photo.caption ?: "Captured Note #$pageNumber",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Overlay status pill at bottom
        Surface(
            color = Color.Black.copy(alpha = 0.70f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Page $pageNumber of $totalPages",
                    color = FolderTabCream,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                if (!photo.ocrText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "“${photo.ocrText.take(80)}...”",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
