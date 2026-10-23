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
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
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
import com.arinara.fotara.feature.collab.model.CollabRole
import com.arinara.fotara.feature.collab.model.CollabRoomCode
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import com.arinara.fotara.ui.theme.jigglePressClickable

/**
 * Modal dialog to enter a custom 4-character collaborative room code (`FT-XXXX`) to host or join
 * a live study canvas.
 * When [activeRoomCode] is non-null, enforces the Active Session Guard so the user cannot create
 * or join another session without explicitly leaving the current session first.
 */
@Composable
fun CollabRoomJoinDialog(
    onDismissRequest: () -> Unit,
    onJoinRoom: (String) -> Unit,
    onCreateRoom: () -> Unit,
    modifier: Modifier = Modifier,
    onCreateCustomRoom: ((String) -> Unit)? = null,
    localUserName: String = "Fotara User",
    localUserRole: CollabRole = CollabRole.MEMBER,
    activeRoomCode: String? = null,
    activePeerCount: Int = 1,
    externalErrorMessage: String? = null,
    onClearExternalError: (() -> Unit)? = null,
    onOpenChat: (() -> Unit)? = null,
    onOpenRoomSettings: (() -> Unit)? = null,
    onLeaveActiveRoom: (() -> Unit)? = null
) {
    var rawSuffix by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var keypadMode by remember { mutableStateOf(false) } // false = 0-9 digits, true = A-Z alphabet
    val clipboardManager = LocalClipboardManager.current

    val displayedError = errorMessage ?: externalErrorMessage
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

                        Column {
                            Text(
                                text = if (activeRoomCode != null) "Active Study Session" else "Live Study Canvas",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                            Text(
                                text = "Connected as $localUserName",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                color = ElectricSlateTokens.PrimaryAccent
                            )
                        }
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
                        text = if (localUserRole == CollabRole.HOST) {
                            "You are hosting this room. If you leave, the room will close for all participants."
                        } else {
                            "You are connected to an active collaborative room. Leave the current room first before creating or joining another session."
                        },
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
                                    text = "Current Room Code • ${localUserRole.name}",
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                    text = "Chat",
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = ElectricSlateTokens.TextPrimary
                                )
                            }
                        }

                        if (onOpenRoomSettings != null) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ElectricSlateTokens.SurfaceElevated)
                                    .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                                    .jigglePressClickable(onClick = onOpenRoomSettings),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = null,
                                    tint = ElectricSlateTokens.TextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(
                                    text = "Settings",
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.SemiBold,
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
                                text = "Leave",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Enter your custom 4-character room code (FT-XXXX) to create a room as Host or join an existing study room.",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = ElectricSlateTokens.TextMuted,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Room Code Slot Boxes: [ FT - ] [ S1 ] [ S2 ] [ S3 ] [ S4 ] + [ Auto ]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Prefix Card
                        Box(
                            modifier = Modifier
                                .height(48.dp)
                                .padding(end = 6.dp)
                                .background(ElectricSlateTokens.SurfaceElevated, RoundedCornerShape(12.dp))
                                .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "FT -",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
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
                                    .size(46.dp)
                                    .padding(horizontal = 2.dp)
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

                        // Quick Randomize / Auto-Fill Custom Code Button
                        Box(
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ElectricSlateTokens.SurfaceElevated)
                                .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                                .jigglePressClickable {
                                    rawSuffix = CollabRoomCode.generateRandomCode().removePrefix(CollabRoomCode.PREFIX)
                                    errorMessage = null
                                    onClearExternalError?.invoke()
                                }
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = "Generate Code",
                                    tint = ElectricSlateTokens.PrimaryAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Auto",
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    color = ElectricSlateTokens.TextPrimary
                                )
                            }
                        }
                    }

                    if (!displayedError.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = displayedError,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            color = ElectricSlateTokens.Destructive,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

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
                                                    onClearExternalError?.invoke()
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
                                        onClearExternalError?.invoke()
                                    }
                                )

                                KeypadKeyButton(
                                    label = "⌫",
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (rawSuffix.isNotEmpty()) {
                                            rawSuffix = rawSuffix.dropLast(1)
                                            errorMessage = null
                                            onClearExternalError?.invoke()
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
                                                    onClearExternalError?.invoke()
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
                                                    onClearExternalError?.invoke()
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Bottom Action Buttons: [ Create Room ] [ Join Room ]
                    val isCodeComplete = rawSuffix.length == 4
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
                                .border(
                                    1.dp,
                                    if (isCodeComplete) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.BorderSubtle,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    when {
                                        rawSuffix.length == 4 -> {
                                            val customFullCode = "FT-$rawSuffix"
                                            if (onCreateCustomRoom != null) {
                                                onCreateCustomRoom(customFullCode)
                                            } else {
                                                onCreateRoom()
                                            }
                                        }
                                        rawSuffix.isEmpty() -> {
                                            onCreateRoom()
                                        }
                                        else -> {
                                            errorMessage = "Enter all 4 characters (or tap Auto) to create your room"
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isCodeComplete) "Create (FT-$rawSuffix)" else "Create Room",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isCodeComplete) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TrackInactive)
                                .border(
                                    1.dp,
                                    if (isCodeComplete) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.BorderSubtle,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable(enabled = isCodeComplete) {
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
                                text = if (isCodeComplete) "Join (FT-$rawSuffix)" else "Join",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isCodeComplete) ElectricSlateTokens.TextPrimary else ElectricSlateTokens.TextMuted
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
