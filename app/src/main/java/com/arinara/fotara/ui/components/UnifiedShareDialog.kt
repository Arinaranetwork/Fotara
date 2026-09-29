// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CardBg = Color(0xFF141936)
private val TabCream = Color(0xFFEAE3D2)
private val AccentGold = Color(0xFFF77F00)

enum class ShareFormatChoice {
    ORIGINAL,
    PDF,
    WORD
}

@Composable
fun UnifiedShareDialog(
    itemCount: Int,
    totalPages: Int,
    isProcessing: Boolean = false,
    progressCurrent: Int = 0,
    progressTotal: Int = 0,
    errorMessage: String? = null,
    containsTextNotes: Boolean = false,
    onFormatSelected: (ShareFormatChoice) -> Unit,
    onCancelProcessing: () -> Unit = {},
    onDismiss: () -> Unit
) {
    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = "Page Limit Exceeded",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD62828),
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = errorMessage,
                    color = TabCream,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold)
                ) {
                    Text("Understood", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = CardBg,
            shape = RoundedCornerShape(16.dp)
        )
        return
    }

    if (isProcessing) {
        // Determinate Combine Progress Dialog
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(
                    text = "Generating Document...",
                    fontWeight = FontWeight.Bold,
                    color = TabCream,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Processing items...",
                            color = TabCream.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "$progressCurrent / $progressTotal pages",
                            color = AccentGold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    val progressFraction = if (progressTotal > 0) {
                        (progressCurrent.toFloat() / progressTotal.toFloat()).coerceIn(0f, 1f)
                    } else 0f
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        color = AccentGold,
                        trackColor = Color(0xFF03071E),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onCancelProcessing) {
                    Text("Cancel", color = Color(0xFFD62828))
                }
            },
            containerColor = CardBg,
            shape = RoundedCornerShape(16.dp)
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Share As",
                    fontWeight = FontWeight.Bold,
                    color = TabCream,
                    fontSize = 19.sp
                )
                Text(
                    text = "$itemCount items selected • ~$totalPages total pages",
                    color = TabCream.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                ShareOptionCard(
                    icon = Icons.Default.Image,
                    title = "Original Files",
                    subtitle = if (containsTextNotes) "Shares photos, docs, and text notes in their native formats (.md for text notes)" else "Share raw photos and document files without re-encoding",
                    enabled = true,
                    onClick = { onFormatSelected(ShareFormatChoice.ORIGINAL) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                ShareOptionCard(
                    icon = Icons.Default.PictureAsPdf,
                    title = "Combined PDF Document",
                    subtitle = "Compile all notes and document pages into a single PDF",
                    enabled = !containsTextNotes,
                    disabledReason = if (containsTextNotes) "PDF combine does not include text notes (Markdown/TXT only rule)" else null,
                    onClick = { onFormatSelected(ShareFormatChoice.PDF) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                ShareOptionCard(
                    icon = Icons.Default.Description,
                    title = "Word Document (.docx)",
                    subtitle = "Compile images and text into an editable Word document",
                    enabled = !containsTextNotes,
                    disabledReason = if (containsTextNotes) "Word combine does not include text notes (Markdown/TXT only rule)" else null,
                    onClick = { onFormatSelected(ShareFormatChoice.WORD) }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TabCream)
            }
        },
        containerColor = CardBg,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun ShareOptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    disabledReason: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) Color(0xFF03071E) else Color(0xFF03071E).copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (enabled) AccentGold.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) AccentGold else Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (enabled) TabCream else TabCream.copy(alpha = 0.45f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = if (enabled) TabCream.copy(alpha = 0.65f) else TabCream.copy(alpha = 0.35f),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
                if (disabledReason != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = disabledReason,
                        color = Color(0xFFF77F00),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
