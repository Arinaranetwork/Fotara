// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
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
import com.arinara.fotara.data.model.PhotoGroup
import com.arinara.fotara.theme.DockSlatePill
import com.arinara.fotara.theme.FolderTabCream
import com.arinara.fotara.theme.MidnightCardOutline
import com.arinara.fotara.theme.MidnightNavy
import com.arinara.fotara.theme.MidnightSurface
import com.arinara.fotara.theme.TagCrimson
import com.arinara.fotara.theme.TextMuted
import com.arinara.fotara.theme.TextPrimary
import com.arinara.fotara.theme.TextSecondary
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun GroupSliderViewerModal(
    group: PhotoGroup,
    memberPhotos: List<Photo>,
    onDismiss: () -> Unit,
    onRotatePhoto: (Photo) -> Unit,
    onCropPhoto: (Photo) -> Unit,
    onRemoveFromGroup: (Photo) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentMembers = remember(memberPhotos) {
        mutableStateListOf<Photo>().apply { addAll(memberPhotos) }
    }
    val pagerState = rememberPagerState(pageCount = { currentMembers.size })
    val coroutineScope = rememberCoroutineScope()
    var imageVersion by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isCurrentPhotoZoomed by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage) {
        isCurrentPhotoZoomed = false
    }

    Dialog(
        onDismissRequest = onDismiss,
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
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            // Floating Popup Slider Card: 90% Width, 75% Height (Revision 1 item 3)
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
                    // Header: Group Name badge, Page Counter, and Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
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
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = FolderTabCream,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = group.name,
                                    style = TextStyle(
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (currentMembers.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${pagerState.currentPage + 1}/${currentMembers.size}",
                                        style = TextStyle(
                                            color = TextSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Normal
                                        )
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .background(MidnightNavy, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = FolderTabCream,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (currentMembers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No photos in this group",
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        // Slider Viewport (HorizontalPager)
                        HorizontalPager(
                            state = pagerState,
                            userScrollEnabled = !isCurrentPhotoZoomed,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black.copy(alpha = 0.4f))
                        ) { page ->
                            val photo = currentMembers[page]
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                ZoomablePhotoViewport(
                                    photo = photo,
                                    imageVersion = imageVersion,
                                    onZoomChanged = { isZoomed ->
                                        if (pagerState.currentPage == page) {
                                            isCurrentPhotoZoomed = isZoomed
                                        }
                                    }
                                )

                                if (photo.caption != null) {
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(12.dp),
                                        color = MidnightNavy.copy(alpha = 0.85f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = photo.caption,
                                            color = TextPrimary,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Per-Photo Actions: Rotate, Crop, Share, and "Remove from group"
                        val currentPhoto = currentMembers.getOrNull(pagerState.currentPage)
                        if (currentPhoto != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // 90° Rotate
                                    IconButton(
                                        onClick = {
                                            onRotatePhoto(currentPhoto)
                                            imageVersion = System.currentTimeMillis()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.RotateRight,
                                            contentDescription = "Rotate 90°",
                                            tint = FolderTabCream
                                        )
                                    }

                                    // Manual Crop
                                    IconButton(
                                        onClick = { onCropPhoto(currentPhoto) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Crop,
                                            contentDescription = "Adjust Crop",
                                            tint = FolderTabCream
                                        )
                                    }

                                    // Share Out
                                    IconButton(
                                        onClick = {
                                            try {
                                                val file = File(currentPhoto.fileUri)
                                                if (file.exists()) {
                                                    val uri = FileProvider.getUriForFile(
                                                        context,
                                                        "${context.packageName}.fileprovider",
                                                        file
                                                    )
                                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                        type = "image/*"
                                                        putExtra(Intent.EXTRA_STREAM, uri)
                                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }
                                                    context.startActivity(Intent.createChooser(shareIntent, "Share Note"))
                                                }
                                            } catch (_: Exception) {}
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Share Note",
                                            tint = FolderTabCream
                                        )
                                    }
                                }

                                // "Remove from group" action button
                                TextButton(
                                    onClick = {
                                        val photoToRemove = currentPhoto
                                        val prevSize = currentMembers.size
                                        onRemoveFromGroup(photoToRemove)
                                        if (prevSize <= 2) {
                                            // Group auto-dissolves when dropped to 1 member
                                            onDismiss()
                                        } else {
                                            currentMembers.remove(photoToRemove)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                        contentDescription = null,
                                        tint = TagCrimson,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Remove from group",
                                        color = TagCrimson,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
