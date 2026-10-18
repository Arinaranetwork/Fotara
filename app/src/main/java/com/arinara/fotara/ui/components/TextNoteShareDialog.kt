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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.data.model.TextNote
import com.arinara.fotara.ui.components.presets.AppButton
import com.arinara.fotara.ui.components.presets.AppButtonVariant
import com.arinara.fotara.ui.components.presets.AppCard
import com.arinara.fotara.ui.theme.ElectricSlateTokens

@Composable
fun TextNoteShareDialog(
    note: TextNote,
    onDismiss: () -> Unit,
    onShareMarkdown: () -> Unit,
    onSharePlainText: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Share Text Note",
                fontWeight = FontWeight.Bold,
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Text notes can be shared as raw Markdown or stripped plain text. (PDF and Word formats are not supported for text notes).",
                    color = ElectricSlateTokens.TextMuted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                ShareOptionRow(
                    icon = Icons.Default.Code,
                    title = "Markdown (.md)",
                    subtitle = "Preserves all bold, italic, headings, lists, and code blocks.",
                    onClick = {
                        onDismiss()
                        onShareMarkdown()
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                ShareOptionRow(
                    icon = Icons.Default.Description,
                    title = "Plain Text (.txt)",
                    subtitle = "Strips all formatting markers into clean, readable text.",
                    onClick = {
                        onDismiss()
                        onSharePlainText()
                    }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = AppButtonVariant.OUTLINED
            )
        },
        containerColor = ElectricSlateTokens.SurfaceElevated,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun ShareOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ElectricSlateTokens.PrimaryAccent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    color = ElectricSlateTokens.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = subtitle,
                    color = ElectricSlateTokens.TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
