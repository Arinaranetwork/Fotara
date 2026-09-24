// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.photo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.model.Subfolder
import com.arinara.fotara.data.model.TagColor
import com.arinara.fotara.theme.DockSlatePill
import com.arinara.fotara.theme.FolderBodyBlue
import com.arinara.fotara.theme.FolderTabCream
import com.arinara.fotara.theme.MidnightCardOutline
import com.arinara.fotara.theme.MidnightSurface
import com.arinara.fotara.theme.TagAmber
import com.arinara.fotara.theme.TagCrimson
import com.arinara.fotara.theme.TextPrimary
import com.arinara.fotara.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoQuickActionSheet(
    photo: Photo,
    subfolders: List<Subfolder>,
    onMoveSubfolder: (Long?) -> Unit,
    onChangeTagColor: (String?) -> Unit,
    onSetDeadline: (Long?) -> Unit,
    onRenamePhoto: () -> Unit = {},
    onSelectPhoto: () -> Unit = {},
    onDeletePhoto: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState()
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current

    var showSubfolderPicker by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showDeadlinePicker by remember { mutableStateOf(false) }
    var actionFeedback by remember { mutableStateOf<String?>(null) }

    val oneDayMs = 24 * 60 * 60 * 1000L
    val now = System.currentTimeMillis()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MidnightSurface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header: Photo Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(FolderBodyBlue.copy(alpha = 0.35f))
                        .border(1.dp, FolderTabCream.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        tint = FolderTabCream,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = photo.caption ?: "Coursework Note #${photo.id}",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Quick Actions",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            if (actionFeedback != null) {
                Surface(
                    color = FolderBodyBlue.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = actionFeedback ?: "",
                        color = FolderTabCream,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Sub-view: Move Subfolder Chooser
            if (showSubfolderPicker) {
                Text(
                    text = "Select Destination Subfolder:",
                    color = FolderTabCream,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
                ActionItem(
                    icon = Icons.AutoMirrored.Filled.DriveFileMove,
                    title = "All Notes (Root)",
                    onClick = {
                        onMoveSubfolder(null)
                        onDismiss()
                    }
                )
                subfolders.forEach { sub ->
                    ActionItem(
                        icon = Icons.AutoMirrored.Filled.DriveFileMove,
                        title = sub.name,
                        onClick = {
                            onMoveSubfolder(sub.id)
                            onDismiss()
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                return@ModalBottomSheet
            }

            // Sub-view: Color Picker Chooser
            if (showColorPicker) {
                Text(
                    text = "Select Tag Color:",
                    color = FolderTabCream,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TagColor.entries.forEach { tag ->
                        Surface(
                            shape = CircleShape,
                            color = tag.composeColor,
                            modifier = Modifier
                                .size(36.dp)
                                .clickable {
                                    onChangeTagColor(tag.hex)
                                    onDismiss()
                                }
                        ) {}
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                return@ModalBottomSheet
            }

            // Sub-view: Deadline Picker Chooser
            if (showDeadlinePicker) {
                Text(
                    text = "Set Assignment Deadline:",
                    color = FolderTabCream,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
                ActionItem(
                    icon = Icons.Default.Event,
                    title = "Due Tomorrow (Urgent)",
                    onClick = {
                        onSetDeadline(now + oneDayMs)
                        onDismiss()
                    }
                )
                ActionItem(
                    icon = Icons.Default.Event,
                    title = "Due in 3 Days",
                    onClick = {
                        onSetDeadline(now + 3 * oneDayMs)
                        onDismiss()
                    }
                )
                ActionItem(
                    icon = Icons.Default.Event,
                    title = "Due Next Week",
                    onClick = {
                        onSetDeadline(now + 7 * oneDayMs)
                        onDismiss()
                    }
                )
                if (photo.linkedDeadline != null) {
                    ActionItem(
                        icon = Icons.Default.Delete,
                        title = "Remove Deadline",
                        tint = TagAmber,
                        onClick = {
                            onSetDeadline(null)
                            onDismiss()
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                return@ModalBottomSheet
            }

            // Primary Quick Actions List
            ActionItem(
                icon = Icons.Default.Edit,
                title = "Rename Note...",
                onClick = {
                    onRenamePhoto()
                    onDismiss()
                }
            )

            ActionItem(
                icon = Icons.Default.CheckCircle,
                title = "Select",
                onClick = {
                    onSelectPhoto()
                    onDismiss()
                }
            )

            ActionItem(
                icon = Icons.AutoMirrored.Filled.DriveFileMove,
                title = "Move to Subfolder...",
                onClick = { showSubfolderPicker = true }
            )

            ActionItem(
                icon = Icons.Default.ColorLens,
                title = "Change Color Label...",
                onClick = { showColorPicker = true }
            )

            ActionItem(
                icon = Icons.Default.Event,
                title = if (photo.linkedDeadline != null) "Edit Deadline (Due soon)" else "Set Assignment Deadline...",
                tint = if (photo.linkedDeadline != null) TagAmber else FolderTabCream,
                onClick = { showDeadlinePicker = true }
            )

            if (!photo.ocrText.isNullOrBlank()) {
                ActionItem(
                    icon = Icons.Default.ContentCopy,
                    title = "Copy Recognized OCR Text",
                    onClick = {
                        clipboardManager.setText(AnnotatedString(photo.ocrText))
                        actionFeedback = "Copied OCR text to clipboard!"
                    }
                )
            }

            ActionItem(
                icon = Icons.Default.Delete,
                title = "Delete Note",
                tint = TagCrimson,
                onClick = {
                    onDeletePhoto()
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ActionItem(
    icon: ImageVector,
    title: String,
    tint: Color = FolderTabCream,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            color = if (tint == TagCrimson) TagCrimson else TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
