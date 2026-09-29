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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CardBg = Color(0xFF141936)
private val TabCream = Color(0xFFEAE3D2)
private val AccentGold = Color(0xFFF77F00)

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
                color = TabCream,
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
                    color = TabCream.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = baseName,
                    onValueChange = { baseName = it },
                    label = { Text("Base Name", color = TabCream.copy(alpha = 0.6f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TabCream,
                        unfocusedTextColor = TabCream,
                        focusedBorderColor = AccentGold,
                        unfocusedBorderColor = TabCream.copy(alpha = 0.3f),
                        cursorColor = AccentGold
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (baseName.isNotBlank()) {
                        onConfirm(baseName.trim())
                    }
                },
                enabled = baseName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGold)
            ) {
                Text("Rename", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TabCream)
            }
        },
        containerColor = CardBg,
        shape = RoundedCornerShape(16.dp)
    )
}
