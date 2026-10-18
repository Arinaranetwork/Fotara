// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.folder

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
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.data.model.DocumentType
import com.arinara.fotara.ui.theme.ElectricSlateTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPhotoOptionSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onNewCanvas: () -> Unit,
    onNewTextNote: () -> Unit,
    onMultiCapture: () -> Unit,
    onImportGallery: () -> Unit,
    onImportDocument: () -> Unit
) {
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
                text = "Add Coursework Note",
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Option: New Canvas Note
            Surface(
                color = ElectricSlateTokens.SurfaceElevated,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNewCanvas() }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Brush,
                            contentDescription = null,
                            tint = ElectricSlateTokens.PrimaryAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "New Canvas",
                            color = ElectricSlateTokens.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Infinite vector drawing & whiteboard",
                            color = ElectricSlateTokens.TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Option: New Text Note
            Surface(
                color = ElectricSlateTokens.SurfaceElevated,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNewTextNote() }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = ElectricSlateTokens.PrimaryAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "New Text Note",
                            color = ElectricSlateTokens.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Create a rich-text note with Markdown formatting and auto-save",
                            color = ElectricSlateTokens.TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Primary Option: Multi-Capture Camera
            Surface(
                color = ElectricSlateTokens.SurfaceElevated,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
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
                            .background(ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = ElectricSlateTokens.PrimaryAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Multi-Capture Camera",
                            color = ElectricSlateTokens.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Instant rapid capture with auto perspective straightening",
                            color = ElectricSlateTokens.TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Secondary Option: Import from Gallery
            Surface(
                color = ElectricSlateTokens.SurfaceElevated,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onImportGallery() }
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
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Import from Gallery",
                            color = ElectricSlateTokens.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Select existing slide screenshots from your device library",
                            color = ElectricSlateTokens.TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tertiary Option: Import Document (PDF / DOCX)
            Surface(
                color = ElectricSlateTokens.SurfaceElevated,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onImportDocument() }
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
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = ElectricSlateTokens.TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Import Document (PDF / DOCX)",
                            color = ElectricSlateTokens.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Render PDF pages with OCR or extract text from Word documents",
                            color = ElectricSlateTokens.TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupQuickActionSheet(
    targetGroup: FolderGridItem.Group,
    onDismiss: () -> Unit,
    onTogglePin: () -> Unit,
    onRename: () -> Unit,
    onSchedule: () -> Unit,
    onUngroup: () -> Unit,
    onAddPhotos: () -> Unit,
    onColorLabel: () -> Unit,
    onSelect: () -> Unit,
    onMove: () -> Unit,
    onExport: () -> Unit,
    onUnlink: (() -> Unit)? = null,
    onDelete: () -> Unit
) {
    val g = targetGroup.group
    val memberCount = targetGroup.memberPhotos.size
    val totalSizeKb = (targetGroup.memberPhotos.sumOf { it.fileSizeBytes } / 1024L).coerceAtLeast(1L)

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    tint = ElectricSlateTokens.PrimaryAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = g.name,
                    color = ElectricSlateTokens.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$memberCount notes · $totalSizeKb KB",
                color = ElectricSlateTokens.TextMuted,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Action: Pin / Unpin Note
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onTogglePin() }
                    .padding(vertical = 12.dp, horizontal = 8.dp)
            ) {
                Icon(imageVector = Icons.Default.PushPin, contentDescription = null, tint = ElectricSlateTokens.TextMuted)
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = if (g.isPinned) "Unpin Note" else "Pin Note",
                    color = ElectricSlateTokens.TextPrimary,
                    fontSize = 14.sp
                )
            }

            // Action 1: Rename
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onRename() }
                    .padding(vertical = 12.dp, horizontal = 8.dp)
            ) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = ElectricSlateTokens.TextMuted)
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = "Rename", color = ElectricSlateTokens.TextPrimary, fontSize = 14.sp)
            }

            // Action: Schedule Reminder
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSchedule() }
                    .padding(vertical = 12.dp, horizontal = 8.dp)
            ) {
                Icon(imageVector = Icons.Default.Alarm, contentDescription = null, tint = ElectricSlateTokens.TextMuted)
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = "Schedule Reminder", color = ElectricSlateTokens.TextPrimary, fontSize = 14.sp)
            }

            // Action 2: Ungroup
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onUngroup() }
                    .padding(vertical = 12.dp, horizontal = 8.dp)
            ) {
                Icon(imageVector = Icons.Default.LayersClear, contentDescription = null, tint = ElectricSlateTokens.TextMuted)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = "Ungroup", color = ElectricSlateTokens.TextPrimary, fontSize = 14.sp)
                    Text(text = "Dissolve group back to individual notes", color = ElectricSlateTokens.TextMuted, fontSize = 12.sp)
                }
            }

            // Action 3: Add photos
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onAddPhotos() }
                    .padding(vertical = 12.dp, horizontal = 8.dp)
            ) {
                Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, tint = ElectricSlateTokens.TextMuted)
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = "Add photos", color = ElectricSlateTokens.TextPrimary, fontSize = 14.sp)
            }

            // Action 4: Color label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onColorLabel() }
                    .padding(vertical = 12.dp, horizontal = 8.dp)
            ) {
                Icon(imageVector = Icons.Default.ColorLens, contentDescription = null, tint = ElectricSlateTokens.TextMuted)
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = "Color label", color = ElectricSlateTokens.TextPrimary, fontSize = 14.sp)
            }

            // Action 5: Select
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelect() }
                    .padding(vertical = 12.dp, horizontal = 8.dp)
            ) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = ElectricSlateTokens.TextMuted)
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = "Select", color = ElectricSlateTokens.TextPrimary, fontSize = 14.sp)
            }

            // Action: Move Group to Subfolder
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onMove() }
                    .padding(vertical = 12.dp, horizontal = 8.dp)
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = null, tint = ElectricSlateTokens.TextMuted)
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = "Move to Subfolder...", color = ElectricSlateTokens.TextPrimary, fontSize = 14.sp)
            }

            // Action: Export Group
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onExport() }
                    .padding(vertical = 12.dp, horizontal = 8.dp)
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = ElectricSlateTokens.TextMuted)
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = "Export Group...", color = ElectricSlateTokens.TextPrimary, fontSize = 14.sp)
            }

            // Action: Unlink Group
            if (targetGroup.isLinked && onUnlink != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onUnlink() }
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.LinkOff, contentDescription = null, tint = ElectricSlateTokens.TextMuted)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = "Unlink Group", color = ElectricSlateTokens.TextPrimary, fontSize = 14.sp)
                }
            }

            // Action 6: Delete
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onDelete() }
                    .padding(vertical = 12.dp, horizontal = 8.dp)
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = ElectricSlateTokens.Destructive)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = "Delete", color = ElectricSlateTokens.Destructive, fontSize = 14.sp)
                    Text(text = "Move group and its notes to Trash", color = ElectricSlateTokens.TextMuted, fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentNoteQuickActionSheet(
    target: FolderGridItem.Document,
    onDismiss: () -> Unit,
    onTogglePin: () -> Unit,
    onRename: () -> Unit,
    onColorLabel: () -> Unit,
    onDeadline: () -> Unit,
    onSchedule: () -> Unit,
    onMove: () -> Unit,
    onShareAs: () -> Unit,
    onSplitToImages: (() -> Unit)? = null,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val doc = target.documentNote

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ElectricSlateTokens.SurfacePanel,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = doc.name,
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(12.dp))

            DropdownMenuItem(
                text = { Text(if (doc.isPinned) "Unpin Note" else "Pin Note", color = ElectricSlateTokens.TextPrimary) },
                leadingIcon = { Icon(Icons.Default.PushPin, null, tint = ElectricSlateTokens.TextMuted) },
                onClick = onTogglePin
            )
            DropdownMenuItem(
                text = { Text("Rename", color = ElectricSlateTokens.TextPrimary) },
                leadingIcon = { Icon(Icons.Default.Edit, null, tint = ElectricSlateTokens.TextMuted) },
                onClick = onRename
            )
            DropdownMenuItem(
                text = { Text("Color Label", color = ElectricSlateTokens.TextPrimary) },
                leadingIcon = { Icon(Icons.Default.ColorLens, null, tint = ElectricSlateTokens.TextMuted) },
                onClick = onColorLabel
            )
            DropdownMenuItem(
                text = { Text("Deadline", color = ElectricSlateTokens.TextPrimary) },
                leadingIcon = { Icon(Icons.Default.Event, null, tint = ElectricSlateTokens.TextMuted) },
                onClick = onDeadline
            )
            DropdownMenuItem(
                text = { Text("Schedule Reminder", color = ElectricSlateTokens.TextPrimary) },
                leadingIcon = { Icon(Icons.Default.Alarm, null, tint = ElectricSlateTokens.TextMuted) },
                onClick = onSchedule
            )
            DropdownMenuItem(
                text = { Text("Move", color = ElectricSlateTokens.TextPrimary) },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.DriveFileMove, null, tint = ElectricSlateTokens.TextMuted) },
                onClick = onMove
            )
            DropdownMenuItem(
                text = { Text("Share As", color = ElectricSlateTokens.TextPrimary) },
                leadingIcon = { Icon(Icons.Default.Share, null, tint = ElectricSlateTokens.TextMuted) },
                onClick = onShareAs
            )
            if (doc.docType == DocumentType.PDF && onSplitToImages != null) {
                DropdownMenuItem(
                    text = { Text("Split to Images", color = ElectricSlateTokens.TextPrimary) },
                    leadingIcon = { Icon(Icons.Default.LayersClear, null, tint = ElectricSlateTokens.TextMuted) },
                    onClick = onSplitToImages
                )
            }
            DropdownMenuItem(
                text = { Text("Select", color = ElectricSlateTokens.TextPrimary) },
                leadingIcon = { Icon(Icons.Default.CheckCircle, null, tint = ElectricSlateTokens.TextMuted) },
                onClick = onSelect
            )
            HorizontalDivider(color = ElectricSlateTokens.BorderSubtle)
            DropdownMenuItem(
                text = { Text("Delete", color = ElectricSlateTokens.Destructive) },
                leadingIcon = { Icon(Icons.Default.Delete, null, tint = ElectricSlateTokens.Destructive) },
                onClick = onDelete
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
