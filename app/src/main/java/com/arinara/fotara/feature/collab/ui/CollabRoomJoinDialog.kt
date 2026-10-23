// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.collab.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arinara.fotara.feature.collab.model.CollabRoomCode
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import com.arinara.fotara.ui.theme.jigglePressClickable

/**
 * Modal dialog to enter a 6-digit collaborative room code (FT-XXXX) or initiate a new room.
 * When [activeRoomCode] is non-null, enforces the Active Session Guard so the user cannot create
 * or join another session without explicitly leaving the current session first.
 */
@Composable
fun CollabRoomJoinDialog(
    onDismissRequest: () -> Unit,
    onJoinRoom: (String) -> Unit,
    onCreateRoom: () -> Unit,
    modifier: Modifier = Modifier,
    activeRoomCode: String? = null,
    activePeerCount: Int = 1,
    onOpenChat: (() -> Unit)? = null,
    onLeaveActiveRoom: (() -> Unit)? = null
) {
    var rawSuffix by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var keypadMode by remember { mutableStateOf(false) } // false = 0-9 digits, true = A-Z alphabet
    val clipboardManager = LocalClipboardManager.current

    val dialogShape = RoundedCornerShape(20.dp)

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .background(ElectricSlateTokens.SurfacePanel, dialogShape)
                .border(1.dp, ElectricSlateTokens.BorderSubtle, dialogShape)
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(ElectricSlateTokens.SurfaceElevated, RoundedCornerShape(12.dp))
                                .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Groups,
                                contentDescription = "Collaborative Canvas",
                                tint = ElectricSlateTokens.PrimaryAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Text(
                            text = if (activeRoomCode != null) "Active Study Session" else "Live Study Canvas",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = ElectricSlateTokens.TextPrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElectricSlateTokens.SurfaceElevated)
                            .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                            .clickable(onClick = onDismissRequest),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close",
                            tint = ElectricSlateTokens.TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (activeRoomCode != null) {
                    // =========================================================================
                    // ACTIVE SESSION GUARD: Prevent creating/joining while a room is active
                    // =========================================================================
                    Text(
                        text = "You are already connected to an active collaborative room. Leave the current room first before creating or joining another session.",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = ElectricSlateTokens.TextMuted,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(ElectricSlateTokens.SurfaceElevated)
                            .border(1.dp, ElectricSlateTokens.PrimaryAccent, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Current Room Code",
                                    fontFamily = ElmsSans,
                                    fontSize = 11.sp,
                                    color = ElectricSlateTokens.TextMuted
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = activeRoomCode,
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    color = ElectricSlateTokens.TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$activePeerCount ${if (activePeerCount == 1) "participant" else "participants"} connected",
                                    fontFamily = ElmsSans,
                                    fontSize = 11.sp,
                                    color = ElectricSlateTokens.PrimaryAccent
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ElectricSlateTokens.SurfacePanel)
                                    .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                                    .jigglePressClickable {
                                        clipboardManager.setText(AnnotatedString(activeRoomCode))
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = "Copy Code",
                                    tint = ElectricSlateTokens.TextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Copy",
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = ElectricSlateTokens.TextPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (onOpenChat != null) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ElectricSlateTokens.PrimaryAccent)
                                    .jigglePressClickable(onClick = onOpenChat),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = ElectricSlateTokens.TextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(
                                    text = "Open Chat",
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = ElectricSlateTokens.TextPrimary
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ElectricSlateTokens.Destructive)
                                .jigglePressClickable {
                                    onLeaveActiveRoom?.invoke()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Leave Session",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Enter 6-digit room code or initiate a new study canvas.",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = ElectricSlateTokens.TextMuted,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Room Code Slot Boxes: [ FT - ] [ S1 ] [ S2 ] [ S3 ] [ S4 ]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Prefix Card
                        Box(
                            modifier = Modifier
                                .height(48.dp)
                                .padding(end = 8.dp)
                                .background(ElectricSlateTokens.SurfaceElevated, RoundedCornerShape(12.dp))
                                .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "FT -",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = ElectricSlateTokens.TextMuted,
                                letterSpacing = 1.sp
                            )
                        }

                        // 4 Character Slots
                        for (i in 0 until 4) {
                            val char = rawSuffix.getOrNull(i)?.toString() ?: ""
                            val isCurrentSlot = rawSuffix.length == i

                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .padding(horizontal = 3.dp)
                                    .background(ElectricSlateTokens.BgCanvas, RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (isCurrentSlot) 2.dp else 1.dp,
                                        color = if (isCurrentSlot) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.BorderSubtle,
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = char,
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = ElectricSlateTokens.TextPrimary
                                )
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = errorMessage ?: "",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            color = ElectricSlateTokens.Destructive
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Keypad Mode Switcher Row: [ Numbers 0-9 ] [ Letters A-Z ]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ElectricSlateTokens.SurfaceElevated, RoundedCornerShape(12.dp))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (!keypadMode) ElectricSlateTokens.PrimaryAccent else Color.Transparent)
                                .clickable { keypadMode = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Digits (0-9)",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = if (!keypadMode) ElectricSlateTokens.TextPrimary else ElectricSlateTokens.TextMuted
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (keypadMode) ElectricSlateTokens.PrimaryAccent else Color.Transparent)
                                .clickable { keypadMode = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Alphabet (A-Z)",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = if (keypadMode) ElectricSlateTokens.TextPrimary else ElectricSlateTokens.TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Custom Keypad Grid
                    if (!keypadMode) {
                        val numRows = listOf(
                            listOf("1", "2", "3", "4", "5"),
                            listOf("6", "7", "8", "9", "0")
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (row in numRows) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    for (key in row) {
                                        KeypadKeyButton(
                                            label = key,
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                if (rawSuffix.length < 4) {
                                                    rawSuffix += key
                                                    errorMessage = null
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                KeypadKeyButton(
                                    label = "Clear",
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        rawSuffix = ""
                                        errorMessage = null
                                    }
                                )

                                KeypadKeyButton(
                                    label = "⌫",
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (rawSuffix.isNotEmpty()) {
                                            rawSuffix = rawSuffix.dropLast(1)
                                            errorMessage = null
                                        }
                                    }
                                )
                            }
                        }
                    } else {
                        val alphaRows = listOf(
                            listOf("A", "B", "C", "D", "E", "F", "G"),
                            listOf("H", "I", "J", "K", "L", "M", "N"),
                            listOf("O", "P", "Q", "R", "S", "T", "U"),
                            listOf("V", "W", "X", "Y", "Z")
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            for ((index, row) in alphaRows.withIndex()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    for (key in row) {
                                        KeypadKeyButton(
                                            label = key,
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                if (rawSuffix.length < 4) {
                                                    rawSuffix += key
                                                    errorMessage = null
                                                }
                                            }
                                        )
                                    }

                                    if (index == 3) {
                                        KeypadKeyButton(
                                            label = "⌫",
                                            modifier = Modifier.weight(2f),
                                            onClick = {
                                                if (rawSuffix.isNotEmpty()) {
                                                    rawSuffix = rawSuffix.dropLast(1)
                                                    errorMessage = null
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Bottom Action Buttons: [ Create Room ] [ Join Room ]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ElectricSlateTokens.SurfaceElevated)
                                .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                                .clickable(onClick = onCreateRoom),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Create Room",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                        }

                        val isJoinEnabled = rawSuffix.length == 4
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isJoinEnabled) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TrackInactive)
                                .border(
                                    1.dp,
                                    if (isJoinEnabled) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.BorderSubtle,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable(enabled = isJoinEnabled) {
                                    val fullCode = "FT-$rawSuffix"
                                    if (CollabRoomCode.isValid(fullCode)) {
                                        onJoinRoom(fullCode)
                                    } else {
                                        errorMessage = "Invalid room code format"
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Join",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isJoinEnabled) ElectricSlateTokens.TextPrimary else ElectricSlateTokens.TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadKeyButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ElectricSlateTokens.SurfaceElevated)
            .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = ElectricSlateTokens.TextPrimary
        )
    }
}
