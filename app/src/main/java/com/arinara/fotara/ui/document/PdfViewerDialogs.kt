// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.
package com.arinara.fotara.ui.document

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.R
import com.arinara.fotara.canvas.engine.DrawingConstants
import com.arinara.fotara.canvas.model.TextBackgroundStyle
import com.arinara.fotara.canvas.model.TextLayerAlignment
import androidx.compose.foundation.layout.height
import com.arinara.fotara.ui.components.presets.AppBottomSheet
import com.arinara.fotara.ui.components.presets.AppButton
import com.arinara.fotara.ui.components.presets.AppButtonVariant
import com.arinara.fotara.ui.components.presets.AppSlider
import com.arinara.fotara.ui.theme.ElectricSlateTokens

@Composable
fun PdfDeleteDocumentConfirmDialog(
    documentTitle: String,
    pageCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AppBottomSheet(
        visible = true,
        onDismiss = onDismiss,
        title = "Delete Document"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Delete \"$documentTitle\"?",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ElectricSlateTokens.TextPrimary
            )
            Text(
                text = "Are you sure you want to permanently delete this document ($pageCount pages)? All page annotations, drawings, and voice notes associated with this document will be permanently deleted. This action cannot be undone.",
                fontSize = 14.sp,
                color = ElectricSlateTokens.TextMuted,
                lineHeight = 20.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    variant = AppButtonVariant.OUTLINED,
                    modifier = Modifier.weight(1f)
                )
                AppButton(
                    text = "Delete Permanently",
                    onClick = onConfirm,
                    variant = AppButtonVariant.DESTRUCTIVE,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun PdfSplitConfirmDialog(
    documentName: String,
    totalCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Split PDF into Images?",
                fontWeight = FontWeight.Bold,
                color = ElectricSlateTokens.TextPrimary
            )
        },
        text = {
            Text(
                text = com.arinara.fotara.util.PdfSplitManager.getConfirmationDescription(documentName, totalCount),
                color = ElectricSlateTokens.TextMuted,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            AppButton(
                text = "Split & Delete PDF",
                onClick = onConfirm,
                variant = AppButtonVariant.DESTRUCTIVE
            )
        },
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
fun PdfClearDrawingConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.pdf_clear_drawing_confirm_title),
                fontWeight = FontWeight.Bold,
                color = ElectricSlateTokens.TextPrimary
            )
        },
        text = {
            Text(
                text = stringResource(R.string.pdf_clear_drawing_confirm_msg),
                color = ElectricSlateTokens.TextMuted,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            AppButton(
                text = stringResource(R.string.pdf_page_menu_clear_drawing),
                onClick = onConfirm,
                variant = AppButtonVariant.DESTRUCTIVE
            )
        },
        dismissButton = {
            AppButton(
                text = stringResource(R.string.confirm_cancel),
                onClick = onDismiss,
                variant = AppButtonVariant.OUTLINED
            )
        },
        containerColor = ElectricSlateTokens.SurfaceElevated,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun PdfTextOptionsDialog(
    isEditing: Boolean,
    textInput: String,
    onTextInputChange: (String) -> Unit,
    textFontSizeSp: Float,
    onTextFontSizeChange: (Float) -> Unit,
    textFontWeight: Int,
    onTextFontWeightChange: (Int) -> Unit,
    textAlignment: TextLayerAlignment,
    onTextAlignmentChange: (TextLayerAlignment) -> Unit,
    textBackgroundStyle: TextBackgroundStyle,
    onTextBackgroundStyleChange: (TextBackgroundStyle) -> Unit,
    textColor: Long,
    onTextColorChange: (Long) -> Unit,
    onConfirm: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edit Text Annotation" else "Text Annotation",
                fontWeight = FontWeight.Bold,
                color = ElectricSlateTokens.TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = onTextInputChange,
                    label = { Text("Annotation Text") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ElectricSlateTokens.TextPrimary,
                        unfocusedTextColor = ElectricSlateTokens.TextPrimary,
                        focusedBorderColor = ElectricSlateTokens.PrimaryAccent,
                        unfocusedBorderColor = ElectricSlateTokens.BorderSubtle,
                        cursorColor = ElectricSlateTokens.PrimaryAccent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Font Size", color = ElectricSlateTokens.TextMuted, fontSize = 14.sp)
                    Text("${textFontSizeSp.toInt()} sp", color = ElectricSlateTokens.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                AppSlider(
                    value = textFontSizeSp,
                    onValueChange = onTextFontSizeChange,
                    valueRange = 12f..48f,
                    steps = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Font Weight", color = ElectricSlateTokens.TextMuted, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val weights = listOf(400 to "Regular", 500 to "Medium", 700 to "Bold")
                    for ((w, label) in weights) {
                        val isSel = textFontWeight == w
                        FilterChip(
                            selected = isSel,
                            onClick = { onTextFontWeightChange(w) },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricSlateTokens.PrimaryAccent,
                                selectedLabelColor = ElectricSlateTokens.TextPrimary,
                                containerColor = ElectricSlateTokens.SurfaceElevated,
                                labelColor = ElectricSlateTokens.TextMuted
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSel,
                                borderColor = ElectricSlateTokens.BorderSubtle,
                                selectedBorderColor = ElectricSlateTokens.PrimaryAccent
                            )
                        )
                    }
                }

                Text("Alignment", color = ElectricSlateTokens.TextMuted, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val aligns = listOf(
                        TextLayerAlignment.LEFT to "Left",
                        TextLayerAlignment.CENTER to "Center",
                        TextLayerAlignment.RIGHT to "Right"
                    )
                    for ((al, label) in aligns) {
                        val isSel = textAlignment == al
                        FilterChip(
                            selected = isSel,
                            onClick = { onTextAlignmentChange(al) },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricSlateTokens.PrimaryAccent,
                                selectedLabelColor = ElectricSlateTokens.TextPrimary,
                                containerColor = ElectricSlateTokens.SurfaceElevated,
                                labelColor = ElectricSlateTokens.TextMuted
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSel,
                                borderColor = ElectricSlateTokens.BorderSubtle,
                                selectedBorderColor = ElectricSlateTokens.PrimaryAccent
                            )
                        )
                    }
                }

                Text("Card Background", color = ElectricSlateTokens.TextMuted, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val styles = listOf(
                        TextBackgroundStyle.TRANSPARENT to "Transparent",
                        TextBackgroundStyle.FROSTED_DARK to "Frosted Dark",
                        TextBackgroundStyle.SOLID_LIGHT to "Solid Light"
                    )
                    for ((s, label) in styles) {
                        val isSel = textBackgroundStyle == s
                        FilterChip(
                            selected = isSel,
                            onClick = { onTextBackgroundStyleChange(s) },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricSlateTokens.PrimaryAccent,
                                selectedLabelColor = ElectricSlateTokens.TextPrimary,
                                containerColor = ElectricSlateTokens.SurfaceElevated,
                                labelColor = ElectricSlateTokens.TextMuted
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSel,
                                borderColor = ElectricSlateTokens.BorderSubtle,
                                selectedBorderColor = ElectricSlateTokens.PrimaryAccent
                            )
                        )
                    }
                }

                Text("Text Color", color = ElectricSlateTokens.TextMuted, fontSize = 14.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(DrawingConstants.CURATED_PALETTE) { colLong ->
                        val isSel = textColor == colLong
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(colLong.toInt()))
                                .border(
                                    width = if (isSel) 2.dp else 1.dp,
                                    color = if (isSel) ElectricSlateTokens.TextPrimary else ElectricSlateTokens.BorderSubtle,
                                    shape = CircleShape
                                )
                                .clickable { onTextColorChange(colLong) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSel) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = if (colLong == 0xFFFFFFFFL) ElectricSlateTokens.BgCanvas else ElectricSlateTokens.TextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            AppButton(
                text = "Done",
                onClick = onConfirm,
                variant = AppButtonVariant.PRIMARY
            )
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onDelete != null) {
                    AppButton(
                        text = "Delete",
                        onClick = onDelete,
                        variant = AppButtonVariant.DESTRUCTIVE
                    )
                }
                AppButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = onDismiss,
                    variant = AppButtonVariant.SECONDARY
                )
            }
        },
        containerColor = ElectricSlateTokens.SurfaceElevated,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun PdfErrorDialog(
    errorMessage: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "PDF Error",
                fontWeight = FontWeight.Bold,
                color = ElectricSlateTokens.Destructive,
                fontSize = 16.sp
            )
        },
        text = {
            Text(
                text = errorMessage,
                color = ElectricSlateTokens.TextMuted,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            AppButton(
                text = "Go Back",
                onClick = onConfirm,
                variant = AppButtonVariant.PRIMARY
            )
        },
        containerColor = ElectricSlateTokens.SurfaceElevated,
        shape = RoundedCornerShape(16.dp)
    )
}

