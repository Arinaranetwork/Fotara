// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.collab.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backspace
import androidx.compose.material.icons.outlined.Close
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arinara.fotara.feature.collab.model.CollabRoomCode
import com.arinara.fotara.theme.ElmsSans

/**
 * Modal dialog to enter a 6-digit collaborative room code (FT-XXXX) or initiate a new room.
 */
@Composable
fun CollabRoomJoinDialog(
    onDismissRequest: () -> Unit,
    onJoinRoom: (String) -> Unit,
    onCreateRoom: () -> Unit,
    modifier: Modifier = Modifier
) {
    var rawSuffix by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var keypadMode by remember { mutableStateOf(false) } // false = 0-9 digits, true = A-Z alphabet

    val dialogShape = RoundedCornerShape(20.dp)

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .background(Color(0xFF111726), dialogShape)
                .border(1.dp, Color(0xFF161E30), dialogShape)
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
                                .background(Color(0xFF141B2A), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF1C2538), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Groups,
                                contentDescription = "Collaborative Canvas",
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Text(
                            text = "Live Study Canvas",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color(0xFFFFFFFF)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF141B2A))
                            .clickable(onClick = onDismissRequest),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close",
                            tint = Color(0xFFA0A5C2),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Enter 6-digit room code or initiate a new study canvas.",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    color = Color(0xFF6F7491),
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
                            .background(Color(0xFF141B2A), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF1C2538), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "FT -",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFFA0A5C2),
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
                                .background(Color(0xFF0A0D14), RoundedCornerShape(8.dp))
                                .border(
                                    width = if (isCurrentSlot) 1.5.dp else 1.dp,
                                    color = if (isCurrentSlot) Color(0xFF2563EB) else Color(0xFF161E30),
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = char,
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFFFFFFFF)
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
                        color = Color(0xFFE63946)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Keypad Mode Switcher Row: [ Numbers 0-9 ] [ Letters A-Z ]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF141B2A), RoundedCornerShape(8.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (!keypadMode) Color(0xFF1B4FC4) else Color.Transparent)
                            .clickable { keypadMode = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Digits (0-9)",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = if (!keypadMode) Color(0xFFFFFFFF) else Color(0xFFA0A5C2)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (keypadMode) Color(0xFF1B4FC4) else Color.Transparent)
                            .clickable { keypadMode = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Alphabet (A-Z)",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = if (keypadMode) Color(0xFFFFFFFF) else Color(0xFFA0A5C2)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Custom Keypad Grid
                if (!keypadMode) {
                    // Numeric 0-9 Keypad
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

                        // Utility Row: [ Clear ] [ Backspace ]
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
                    // Alphabetic Keypad (A-Z in 4 rows)
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
                                    // Append Backspace key in the last row
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
                    // Create Room Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF141B2A))
                            .border(1.dp, Color(0xFF1C2538), RoundedCornerShape(8.dp))
                            .clickable(onClick = onCreateRoom),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Create Room",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = Color(0xFFA0A5C2)
                        )
                    }

                    // Join Room Button
                    val isJoinEnabled = rawSuffix.length == 4
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isJoinEnabled) Color(0xFF2563EB) else Color(0xFF182236))
                            .border(
                                1.dp,
                                if (isJoinEnabled) Color(0xFF2563EB) else Color(0xFF1C2538),
                                RoundedCornerShape(8.dp)
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
                            fontSize = 13.sp,
                            color = if (isJoinEnabled) Color(0xFFFFFFFF) else Color(0xFF6F7491)
                        )
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
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF141B2A))
            .border(1.dp, Color(0xFF1C2538), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = Color(0xFFFFFFFF)
        )
    }
}
