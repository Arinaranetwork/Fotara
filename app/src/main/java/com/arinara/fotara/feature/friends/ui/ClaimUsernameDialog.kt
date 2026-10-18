// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.friends.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.theme.FolderTabCream
import com.arinara.fotara.theme.HomeCardBorder
import com.arinara.fotara.theme.HomeCardSurface
import com.arinara.fotara.theme.HomeNearBlack
import com.arinara.fotara.theme.TextSecondary

/**
 * Reusable modal dialog for claiming or updating academic @username with 7-day cooldown verification.
 */
@Composable
fun ClaimUsernameDialog(
    currentUsername: String,
    canChangeUsername: Boolean,
    cooldownDaysRemaining: Int,
    cooldownMessage: String?,
    onDismiss: () -> Unit,
    onClaim: (newUsername: String, displayName: String) -> Unit
) {
    var rawInput by remember { mutableStateOf("") }
    var displayNameInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Claim Academic Username",
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Current username: $currentUsername",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = FolderTabCream
                )

                // Cooldown notice
                if (!canChangeUsername) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2A1C16))
                            .border(BorderStroke(1.dp, Color(0xFF78350F)), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = cooldownMessage ?: "Username can only be changed once every 7 days. Cooldown active for $cooldownDaysRemaining days.",
                            fontFamily = ElmsSans,
                            fontSize = 12.sp,
                            color = Color(0xFFFBBF24)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF102A1E))
                            .border(BorderStroke(1.dp, Color(0xFF065F46)), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Available to change. A 7-day cooldown applies after claiming.",
                            fontFamily = ElmsSans,
                            fontSize = 12.sp,
                            color = Color(0xFF34D399)
                        )
                    }
                }

                OutlinedTextField(
                    value = rawInput,
                    onValueChange = {
                        rawInput = it.removePrefix("@")
                        errorMessage = null
                    },
                    label = { Text("New Username") },
                    prefix = { Text("@") },
                    placeholder = { Text("username") },
                    singleLine = true,
                    enabled = canChangeUsername,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = HomeCardBorder,
                        focusedContainerColor = HomeNearBlack,
                        unfocusedContainerColor = HomeNearBlack,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = displayNameInput,
                    onValueChange = {
                        displayNameInput = it
                        errorMessage = null
                    },
                    label = { Text("Display Name") },
                    placeholder = { Text("Your full name") },
                    singleLine = true,
                    enabled = canChangeUsername,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = HomeCardBorder,
                        focusedContainerColor = HomeNearBlack,
                        unfocusedContainerColor = HomeNearBlack,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        fontFamily = ElmsSans,
                        fontSize = 12.sp,
                        color = Color(0xFFEF4444)
                    )
                }
            }
        },
        containerColor = HomeCardSurface,
        shape = RoundedCornerShape(20.dp),
        confirmButton = {
            Button(
                onClick = {
                    val clean = rawInput.trim().removePrefix("@")
                    if (clean.isEmpty()) {
                        errorMessage = "Please enter a username"
                        return@Button
                    }
                    if (clean.length < 3) {
                        errorMessage = "Username must be at least 3 characters"
                        return@Button
                    }
                    val fullUsername = "@$clean"
                    val finalDisplayName = displayNameInput.trim().ifEmpty { clean }
                    onClaim(fullUsername, finalDisplayName)
                },
                enabled = canChangeUsername,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2638)),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    text = "Claim Username",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    text = "Cancel",
                    fontFamily = ElmsSans,
                    color = TextSecondary
                )
            }
        }
    )
}
