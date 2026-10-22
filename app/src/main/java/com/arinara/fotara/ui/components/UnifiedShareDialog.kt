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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.R
import com.arinara.fotara.expression.ExpressionContext
import com.arinara.fotara.expression.ExpressionEngine
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.ui.components.presets.AppButton
import com.arinara.fotara.ui.components.presets.AppButtonVariant
import com.arinara.fotara.ui.components.presets.AppCard
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import com.arinara.fotara.util.FileNamePresetHelper

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
    fileNamePresetTemplate: String = FileNamePresetHelper.DEFAULT_PRESET,
    folderName: String = "Coursework",
    onFormatSelected: (ShareFormatChoice, customFileName: String?) -> Unit,
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
                    color = ElectricSlateTokens.Destructive,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = errorMessage,
                    color = ElectricSlateTokens.TextPrimary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                AppButton(
                    text = "Understood",
                    onClick = onDismiss,
                    variant = AppButtonVariant.PRIMARY
                )
            },
            containerColor = ElectricSlateTokens.SurfacePanel,
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
                    color = ElectricSlateTokens.TextPrimary,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Processing items...",
                            color = ElectricSlateTokens.TextMuted,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "$progressCurrent / $progressTotal pages",
                            color = ElectricSlateTokens.PrimaryAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    val progressFraction = if (progressTotal > 0) {
                        (progressCurrent.toFloat() / progressTotal.toFloat()).coerceIn(0f, 1f)
                    } else 0f
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        color = ElectricSlateTokens.PrimaryAccent,
                        trackColor = ElectricSlateTokens.BgCanvas,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                AppButton(
                    text = "Cancel",
                    onClick = onCancelProcessing,
                    variant = AppButtonVariant.DESTRUCTIVE
                )
            },
            containerColor = ElectricSlateTokens.SurfacePanel,
            shape = RoundedCornerShape(16.dp)
        )
        return
    }

    var customFileName by remember { mutableStateOf("") }
    val defaultBaseName = remember(fileNamePresetTemplate, folderName, itemCount) {
        FileNamePresetHelper.resolvePreset(
            presetTemplate = fileNamePresetTemplate,
            folderName = folderName,
            itemCount = itemCount
        )
    }

    val resolveAndSanitize = { raw: String ->
        if (raw.isNotBlank()) {
            ExpressionEngine.sanitizeFileName(
                ExpressionEngine.lockStaticExpressions(
                    raw,
                    ExpressionContext(folder = folderName, pages = totalPages, isFileName = true)
                )
            )
        } else {
            null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Share As",
                    fontWeight = FontWeight.Bold,
                    color = ElectricSlateTokens.TextPrimary,
                    fontSize = 18.sp
                )
                Text(
                    text = "$itemCount items selected • ~$totalPages total pages",
                    color = ElectricSlateTokens.TextMuted,
                    fontSize = 12.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
            ) {
                ShareOptionCard(
                    icon = Icons.Default.Image,
                    title = "Original Files",
                    subtitle = if (containsTextNotes) "Shares photos, docs, canvas, and text notes in their native formats" else "Share raw photos and document files without re-encoding",
                    enabled = true,
                    onClick = { onFormatSelected(ShareFormatChoice.ORIGINAL, null) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                ShareOptionCard(
                    icon = Icons.Default.PictureAsPdf,
                    title = "Combined PDF Document",
                    subtitle = "Compile all notes and document pages into a single PDF",
                    enabled = true,
                    disabledReason = null,
                    onClick = { onFormatSelected(ShareFormatChoice.PDF, resolveAndSanitize(customFileName)) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                ShareOptionCard(
                    icon = Icons.Default.Description,
                    title = "Word Document (.docx)",
                    subtitle = "Compile images and text into an editable Word document",
                    enabled = true,
                    disabledReason = null,
                    onClick = { onFormatSelected(ShareFormatChoice.WORD, resolveAndSanitize(customFileName)) }
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.share_file_name_label),
                    fontSize = 12.sp,
                    color = ElectricSlateTokens.TextMuted,
                    fontFamily = ElmsSans
                )
                Spacer(modifier = Modifier.height(4.dp))
                ExpressionAutocompleteField(
                    value = customFileName,
                    onValueChange = { customFileName = it },
                    placeholder = defaultBaseName,
                    isFileName = true,
                    context = ExpressionContext(
                        folder = folderName,
                        pages = totalPages,
                        isFileName = true
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.share_file_name_helper),
                    color = ElectricSlateTokens.TextMuted,
                    fontSize = 11.sp,
                    fontFamily = ElmsSans,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = AppButtonVariant.SECONDARY
            )
        },
        containerColor = ElectricSlateTokens.SurfacePanel,
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
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = if (enabled) onClick else null,
        containerColor = if (enabled) ElectricSlateTokens.BgCanvas else ElectricSlateTokens.BgCanvas.copy(alpha = 0.5f),
        borderColor = ElectricSlateTokens.BorderSubtle,
        cornerRadius = 16.dp,
        contentPadding = 14.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (enabled) ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.15f)
                        else ElectricSlateTokens.TrackInactive.copy(alpha = 0.5f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TextMuted,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (enabled) ElectricSlateTokens.TextPrimary else ElectricSlateTokens.TextMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = ElectricSlateTokens.TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
                if (disabledReason != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = disabledReason,
                        color = ElectricSlateTokens.PrimaryAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
