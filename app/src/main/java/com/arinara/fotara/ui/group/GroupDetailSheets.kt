// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.group

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.data.model.CanvasNote
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.ui.theme.ElectricSlateTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupAddOptionsSheet(
    groupName: String,
    onMultiCapture: () -> Unit,
    onPickGallery: () -> Unit,
    onAddExistingNotes: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ElectricSlateTokens.SurfacePanel,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Add to \"$groupName\"",
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Option 1: Multi-Capture Camera
            Surface(
                color = ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.15f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ElectricSlateTokens.PrimaryAccent),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onMultiCapture() }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(ElectricSlateTokens.PrimaryAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = ElectricSlateTokens.TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Multi-Capture Camera",
                            color = ElectricSlateTokens.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Straighten & capture directly into this group",
                            color = ElectricSlateTokens.TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Option 2: Import from Gallery
            Surface(
                color = ElectricSlateTokens.SurfaceElevated,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPickGallery() }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(ElectricSlateTokens.TrackInactive),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Collections,
                            contentDescription = null,
                            tint = ElectricSlateTokens.TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Import from Gallery",
                            color = ElectricSlateTokens.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Select images from library to add directly",
                            color = ElectricSlateTokens.TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Option 3: Add Existing Notes from Folder
            Surface(
                color = ElectricSlateTokens.SurfaceElevated,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAddExistingNotes() }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(ElectricSlateTokens.TrackInactive),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = ElectricSlateTokens.TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Add Existing Notes",
                            color = ElectricSlateTokens.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Fold standalone folder notes into this group",
                            color = ElectricSlateTokens.TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupPhotoContextMenuSheet(
    photo: Photo,
    onTogglePin: (Photo) -> Unit,
    onRename: (Photo) -> Unit,
    onSetCover: (Photo) -> Unit,
    onInspect: (Photo) -> Unit,
    onRemoveFromGroup: (Photo) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ElectricSlateTokens.SurfacePanel,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = photo.caption ?: "Note Actions",
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Action: Pin / Unpin Note
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onTogglePin(photo) }
                    .padding(vertical = 12.dp, horizontal = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = null,
                    tint = ElectricSlateTokens.PrimaryAccent
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = if (photo.isPinned) "Unpin note" else "Pin note",
                    color = ElectricSlateTokens.TextPrimary,
                    fontSize = 14.sp
                )
            }

            // Action: Rename Note
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onRename(photo) }
                    .padding(vertical = 12.dp, horizontal = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = ElectricSlateTokens.PrimaryAccent
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Rename note",
                    color = ElectricSlateTokens.TextPrimary,
                    fontSize = 14.sp
                )
            }

            // Action 1: Set as Cover Photo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSetCover(photo) }
                    .padding(vertical = 12.dp, horizontal = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = ElectricSlateTokens.PrimaryAccent
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Set as cover photo",
                    color = ElectricSlateTokens.TextPrimary,
                    fontSize = 14.sp
                )
            }

            // Action 2: Inspect Note
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onInspect(photo) }
                    .padding(vertical = 12.dp, horizontal = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Collections,
                    contentDescription = null,
                    tint = ElectricSlateTokens.TextMuted
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Inspect note",
                    color = ElectricSlateTokens.TextPrimary,
                    fontSize = 14.sp
                )
            }

            // Action 3: Remove from Group
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onRemoveFromGroup(photo) }
                    .padding(vertical = 12.dp, horizontal = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LayersClear,
                    contentDescription = null,
                    tint = ElectricSlateTokens.Destructive
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Remove from group",
                    color = ElectricSlateTokens.Destructive,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupCanvasNoteContextMenuSheet(
    canvasNote: CanvasNote,
    onRename: (CanvasNote) -> Unit,
    onRemoveFromGroup: (CanvasNote) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ElectricSlateTokens.SurfacePanel,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = canvasNote.title.ifBlank { "Canvas Note Actions" },
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Action 1: Rename Note
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onRename(canvasNote) }
                    .padding(vertical = 12.dp, horizontal = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = ElectricSlateTokens.PrimaryAccent
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Rename canvas note",
                    color = ElectricSlateTokens.TextPrimary,
                    fontSize = 14.sp
                )
            }

            // Action 2: Remove from Group
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onRemoveFromGroup(canvasNote) }
                    .padding(vertical = 12.dp, horizontal = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LayersClear,
                    contentDescription = null,
                    tint = ElectricSlateTokens.Destructive
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Remove from group",
                    color = ElectricSlateTokens.Destructive,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
