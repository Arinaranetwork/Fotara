// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.canvas

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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.data.model.CanvasNote
import com.arinara.fotara.data.model.TagColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val SheetBg = Color(0xFF0F1422)
private val TabCream = Color(0xFFEAE3D2)
private val DangerRed = Color(0xFFD62828)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasNoteQuickActionSheet(
    note: CanvasNote,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onColorSelect: (String?) -> Unit,
    onSetDeadline: () -> Unit,
    onSchedule: () -> Unit = {},
    onTogglePin: (() -> Unit)? = null,
    onSelect: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SheetBg,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Note Title Header
            Text(
                text = note.title,
                color = TabCream,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.padding(bottom = 2.dp)
            )

            val addedDateStr = remember(note.addedAt) {
                if (note.addedAt > 0) {
                    val sdf = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.US)
                    "Added " + sdf.format(Date(note.addedAt))
                } else "Canvas Note"
            }
            Text(
                text = addedDateStr,
                color = TabCream.copy(alpha = 0.6f),
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Color label row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Clear color option
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .border(1.dp, TabCream.copy(alpha = 0.5f), CircleShape)
                        .clickable { onColorSelect(null); onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.size(10.dp).background(Color.Transparent))
                }

                TagColor.entries.forEach { color ->
                    val isCurrent = note.tagColor == color.hex
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(color.hex)))
                            .border(
                                width = if (isCurrent) 2.5.dp else 0.dp,
                                color = if (isCurrent) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { onColorSelect(color.hex); onDismiss() }
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Color(0xFF242C56)
            )

            // Actions
            onTogglePin?.let { toggle ->
                ActionItem(
                    icon = Icons.Default.PushPin,
                    label = if (note.isPinned) "Unpin Note" else "Pin Note"
                ) {
                    onDismiss()
                    toggle()
                }
            }
            ActionItem(icon = Icons.Default.Edit, label = "Rename") { onDismiss(); onRename() }
            ActionItem(icon = Icons.AutoMirrored.Filled.DriveFileMove, label = "Move to Folder") { onDismiss(); onMove() }
            ActionItem(icon = Icons.Default.Event, label = "Set Deadline") { onDismiss(); onSetDeadline() }
            ActionItem(
                icon = Icons.Default.Alarm,
                label = if (note.scheduledAt != null) "Edit Schedule Reminder" else "Schedule Reminder"
            ) {
                onDismiss()
                onSchedule()
            }
            ActionItem(icon = Icons.Default.CheckCircle, label = "Select Note") { onDismiss(); onSelect() }
            ActionItem(icon = Icons.Default.Share, label = "Share Image (PNG)") { onDismiss(); onShare() }
            ActionItem(icon = Icons.Default.Delete, label = "Delete to Trash", isDanger = true) { onDismiss(); onDelete() }
        }
    }
}

@Composable
private fun ActionItem(
    icon: ImageVector,
    label: String,
    isDanger: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDanger) DangerRed else TabCream,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            color = if (isDanger) DangerRed else TabCream,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
