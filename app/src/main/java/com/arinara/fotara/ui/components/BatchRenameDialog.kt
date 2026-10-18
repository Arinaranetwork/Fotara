// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.ui.components.presets.AppButton
import com.arinara.fotara.ui.components.presets.AppButtonVariant
import com.arinara.fotara.ui.theme.ElectricSlateTokens

@Composable
fun BatchRenameDialog(
    itemCount: Int,
    initialBaseName: String = "",
    onConfirm: (baseName: String) -> Unit,
    onDismiss: () -> Unit
) {
    var baseName by remember { mutableStateOf(initialBaseName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (itemCount > 1) "Batch Rename $itemCount Items" else "Rename Item",
                fontWeight = FontWeight.Bold,
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (itemCount > 1) {
                        "Enter a base name. Items will be sequentially numbered in grid order (e.g., '${baseName.ifBlank { "Note" }} 1', '${baseName.ifBlank { "Note" }} 2')."
                    } else {
                        "Enter a new name for this item."
                    },
                    color = ElectricSlateTokens.TextMuted,
                    fontSize = 14.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = baseName,
                    onValueChange = { baseName = it },
                    label = { Text("Base Name", color = ElectricSlateTokens.TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ElectricSlateTokens.TextPrimary,
                        unfocusedTextColor = ElectricSlateTokens.TextPrimary,
                        focusedBorderColor = ElectricSlateTokens.PrimaryAccent,
                        unfocusedBorderColor = ElectricSlateTokens.BorderSubtle,
                        cursorColor = ElectricSlateTokens.PrimaryAccent
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            AppButton(
                text = "Rename",
                onClick = {
                    if (baseName.isNotBlank()) {
                        onConfirm(baseName.trim())
                    }
                },
                enabled = baseName.isNotBlank(),
                variant = AppButtonVariant.PRIMARY
            )
        },
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = AppButtonVariant.SECONDARY
            )
        },
        containerColor = ElectricSlateTokens.SurfacePanel,
        shape = RoundedCornerShape(20.dp)
    )
}
