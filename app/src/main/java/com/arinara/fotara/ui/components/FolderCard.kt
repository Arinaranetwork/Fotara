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
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.arinara.fotara.R
import com.arinara.fotara.theme.TagCrimson
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

object FolderCardDefaults {
    val CardCornerRadius = 24.dp
    val ThreeDotButtonSize = 44.dp
    val ThreeDotIconSize = 20.dp
    val VisualCenterOffset = 22.dp
}

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
    glowAnchors: Set<GlowAnchor> = emptySet(),
    onPinClick: () -> Unit = {},
    onRenameClick: () -> Unit = {},
    onSelectClick: () -> Unit = {},
    onMoveToWorkspaceClick: () -> Unit = {},
    onLockClick: () -> Unit = {},
    onUnlinkClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
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

    val effectiveAnchors = if (glowAnchors.isNotEmpty()) {
        glowAnchors
    } else if (glowCorner != null) {
        setOf(glowCorner.toGlowAnchor())
    } else {
        emptySet()
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .then(borderModifier)
            .clip(RoundedCornerShape(cornerRadiusDp))
            .background(HomeCardSurface)
            .linkItGlow(
                isLinked = effectiveAnchors.isNotEmpty(),
                glowColor = accent.glowColor,
                anchors = effectiveAnchors,
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

                // Space reserved for the top-right corner 3-dots button
                Spacer(modifier = Modifier.size(FolderCardDefaults.ThreeDotButtonSize))
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

        // 3-dots overflow button with anchored dropdown menu, moved toward top-right corner (~20dp visual center, >=44dp touch target)
        var isMenuExpanded by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            IconButton(
                onClick = {
                    onMenuClick()
                    isMenuExpanded = true
                },
                modifier = Modifier.size(FolderCardDefaults.ThreeDotButtonSize)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Folder options",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(FolderCardDefaults.ThreeDotIconSize)
                )
            }

            DropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = { isMenuExpanded = false },
                modifier = Modifier
                    .background(HomeCardSurface)
                    .border(1.dp, HomeCardBorder, RoundedCornerShape(12.dp))
            ) {
                // 1. Pin to Top
                DropdownMenuItem(
                    text = {
                        Text(
                            text = if (folder.isPinned) stringResource(R.string.folder_menu_unpin) else stringResource(R.string.folder_menu_pin),
                            color = Color.White,
                            fontFamily = ElmsSans,
                            fontSize = 15.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = {
                        isMenuExpanded = false
                        onPinClick()
                    }
                )

                // 2. Rename Folder
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.folder_menu_rename),
                            color = Color.White,
                            fontFamily = ElmsSans,
                            fontSize = 15.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = {
                        isMenuExpanded = false
                        onRenameClick()
                    }
                )

                // 3. Select
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.folder_menu_select),
                            color = Color.White,
                            fontFamily = ElmsSans,
                            fontSize = 15.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CropFree,
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = {
                        isMenuExpanded = false
                        onSelectClick()
                    }
                )

                // 3b. Move to workspace
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.folder_menu_move_to_workspace),
                            color = Color.White,
                            fontFamily = ElmsSans,
                            fontSize = 15.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.DriveFileMove,
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = {
                        isMenuExpanded = false
                        onMoveToWorkspaceClick()
                    }
                )

                HorizontalDivider(color = HomeCardBorder)

                // 4. Lock Folder (PIN)
                DropdownMenuItem(
                    text = {
                        Text(
                            text = if (folder.isLocked) stringResource(R.string.folder_menu_unlock) else stringResource(R.string.folder_menu_lock),
                            color = Color.White,
                            fontFamily = ElmsSans,
                            fontSize = 15.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (folder.isLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = {
                        isMenuExpanded = false
                        onLockClick()
                    }
                )

                // 5. Unlink Folder (only if linked)
                if (folder.linkGroupId != null) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.folder_menu_unlink),
                                color = TagCrimson,
                                fontFamily = ElmsSans,
                                fontSize = 15.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.LinkOff,
                                contentDescription = null,
                                tint = TagCrimson,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            isMenuExpanded = false
                            onUnlinkClick()
                        }
                    )
                }

                // 6. Move to Trash
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.folder_menu_trash),
                            color = TagCrimson,
                            fontFamily = ElmsSans,
                            fontSize = 15.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = TagCrimson,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = {
                        isMenuExpanded = false
                        onDeleteClick()
                    }
                )
            }
        }
    }
}
