// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.theme.FolderAccentPalette
import com.arinara.fotara.theme.FolderTabCream
import com.arinara.fotara.theme.HomeCardBorder
import com.arinara.fotara.theme.HomeCardSurface

/**
 * Modern 1:1 dark folder card matching IMAGE A.
 * Features a top-left rounded accent tile, top-right options menu,
 * downward-flowing typography, and facing LinkIt corner stroke glows.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderCard(
    folder: Folder,
    onClick: () -> Unit,
    onMenuClick: () -> Unit = {},
    onRename: (String) -> Unit = {},
    onCardLongClick: () -> Unit = {},
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    glowCorner: GlowCorner? = null,
    modifier: Modifier = Modifier
) {
    val cornerRadiusDp = 24.dp
    val accent = remember(folder.colorLabel, folder.id, folder.name) {
        FolderAccentPalette.fromHexOrDefault(folder.colorLabel, folder.id, folder.name)
    }
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
            editValue = TextFieldValue(folder.name, TextRange(folder.name.length))
            isEditing = false
        }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    val borderModifier = if (isSelected) {
        Modifier.border(2.5.dp, Color(0xFF2563EB), RoundedCornerShape(cornerRadiusDp))
    } else {
        Modifier.border(1.dp, HomeCardBorder, RoundedCornerShape(cornerRadiusDp))
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .then(borderModifier)
            .clip(RoundedCornerShape(cornerRadiusDp))
            .background(HomeCardSurface)
            .linkItCornerGlow(
                isLinked = glowCorner != null,
                glowColor = accent.glowColor,
                corner = glowCorner ?: GlowCorner.BottomLeft,
                linkedDescription = "Linked folder ${folder.name}",
                cornerRadiusDp = cornerRadiusDp.value,
                strokeWidthDp = 2f
            )
            .combinedClickable(
                enabled = !isEditing,
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCardLongClick()
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp)
        ) {
            // Top Row: Accent icon tile (left) and 3-dots menu button (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // ~42dp icon tile with 12dp radius filled with muted accent color
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.tileFill),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Folder,
                        contentDescription = null,
                        tint = accent.iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // 3-dots overflow button with at least 48dp touch target
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Folder options",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Text section: Title and note count flowing downward
            if (isEditing) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BasicTextField(
                        value = editValue,
                        onValueChange = { editValue = it },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = ElmsSans
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
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                Text(
                    text = folder.name,
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = ElmsSans,
                    letterSpacing = (-0.2).sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 26.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                val noteText = if (folder.photoCount == 1) "1 note" else "${folder.photoCount} notes"
                Text(
                    text = noteText,
                    color = Color(0xFF6B7280),
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Light,
                    fontFamily = ElmsSans
                )
            }
        }
    }
}
