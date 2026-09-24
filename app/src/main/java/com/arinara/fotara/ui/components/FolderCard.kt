// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.theme.FolderBodyBlue
import com.arinara.fotara.theme.FolderTabCream
import com.arinara.fotara.theme.FolderTextWhite

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderCard(
    folder: Folder,
    onClick: () -> Unit,
    onRename: (String) -> Unit = {},
    onCardLongClick: () -> Unit = {},
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cornerRadiusDp = 24.dp
    val tagColor = folder.tagColor.composeColor
    val haptic = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var isEditing by remember { mutableStateOf(false) }
    var editValue by remember(folder.name) {
        mutableStateOf(
            TextFieldValue(
                text = folder.name,
                selection = TextRange(0, folder.name.length)
            )
        )
    }
    val focusRequester = remember { FocusRequester() }

    if (isEditing) {
        BackHandler {
            // Cancel without saving on back gesture
            editValue = TextFieldValue(folder.name, TextRange(folder.name.length))
            isEditing = false
        }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    val borderModifier = if (isSelected) {
        Modifier.border(2.5.dp, FolderTabCream, RoundedCornerShape(cornerRadiusDp))
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .aspectRatio(0.82f)
            .then(borderModifier)
            .clip(RoundedCornerShape(cornerRadiusDp))
            .combinedClickable(
                enabled = !isEditing,
                onClick = onClick,
                onLongClick = {
                    if (!isSelectionMode) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCardLongClick()
                    }
                }
            )
    ) {
        // Custom background: Cream tab top + Royal Blue body with bezier curve transition
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = cornerRadiusDp.toPx()

            // 1. Draw overall ivory/cream card base
            drawRoundRect(
                color = FolderTabCream,
                size = size,
                cornerRadius = CornerRadius(r, r)
            )

            // 2. Draw royal blue main body with curved top transition
            val topH = h * 0.18f
            val shoulderH = h * 0.28f
            val curveStart = w * 0.72f

            val bluePath = Path().apply {
                moveTo(0f, topH)
                lineTo(curveStart, topH)
                // Smooth cubic bezier curve creating the folder tab shoulder
                cubicTo(
                    x1 = curveStart + (w - curveStart) * 0.4f,
                    y1 = topH,
                    x2 = curveStart + (w - curveStart) * 0.4f,
                    y2 = shoulderH,
                    x3 = w,
                    y3 = shoulderH
                )
                // Down to bottom right corner
                lineTo(w, h - r)
                arcTo(
                    rect = Rect(w - 2 * r, h - 2 * r, w, h),
                    startAngleDegrees = 0f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                // Across to bottom left corner
                lineTo(r, h)
                arcTo(
                    rect = Rect(0f, h - 2 * r, 2 * r, h),
                    startAngleDegrees = 90f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                close()
            }
            drawPath(path = bluePath, color = FolderBodyBlue)
        }

        // Folder card contents
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Top Bar: Selection indicator, Pin indicator, and Top-Right Color Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) FolderBodyBlue else Color.White.copy(alpha = 0.85f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) FolderTabCream else Color.LightGray),
                        modifier = Modifier.size(20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = FolderTabCream,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
                if (folder.isPinned) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pinned folder",
                        tint = Color(0xFF6B6559),
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (folder.isLocked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked folder",
                        tint = FolderTabCream,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Surface(
                    shape = CircleShape,
                    color = tagColor,
                    modifier = Modifier.size(10.dp)
                ) {}
            }

            Spacer(modifier = Modifier.weight(1f))

            // Lower Body: Subject Name (Static vs Inline Editable on text-only long-press)
            if (isEditing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        .border(1.dp, FolderTabCream, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = editValue,
                        onValueChange = { editValue = it },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        textStyle = TextStyle(
                            color = FolderTextWhite,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic
                        ),
                        cursorBrush = SolidColor(FolderTabCream),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                val trimmed = editValue.text.trim()
                                if (trimmed.isNotEmpty()) {
                                    onRename(trimmed)
                                }
                                isEditing = false
                                keyboardController?.hide()
                            }
                        )
                    )
                    IconButton(
                        onClick = {
                            val trimmed = editValue.text.trim()
                            if (trimmed.isNotEmpty()) {
                                onRename(trimmed)
                            }
                            isEditing = false
                            keyboardController?.hide()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Confirm Rename",
                            tint = FolderTabCream,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                Text(
                    text = folder.name,
                    color = FolderTextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = (-0.3).sp,
                    maxLines = 2,
                    modifier = Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = {
                            if (!isSelectionMode && !folder.isLocked) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                editValue = TextFieldValue(
                                    text = folder.name,
                                    selection = TextRange(0, folder.name.length)
                                )
                                isEditing = true
                            }
                        }
                    )
                )
            }

            if (folder.isLocked) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = FolderTabCream.copy(alpha = 0.85f),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Locked",
                        color = FolderTabCream.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else if (folder.photoCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${folder.photoCount} note${if (folder.photoCount > 1) "s" else ""}",
                    color = FolderTextWhite.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
