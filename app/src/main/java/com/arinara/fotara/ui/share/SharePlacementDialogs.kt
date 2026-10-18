// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.share

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.theme.FolderTabCream
import com.arinara.fotara.theme.MidnightCardOutline
import com.arinara.fotara.theme.MidnightSurface
import com.arinara.fotara.theme.TextPrimary
import com.arinara.fotara.theme.TextSecondary
import com.arinara.fotara.ui.components.presets.AppButton
import com.arinara.fotara.ui.components.presets.AppButtonVariant

@Composable
fun SharePlacementExitConfirmDialog(
    totalRemaining: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidnightSurface,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "Discard Unplaced Items?",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Text(
                text = "You still have $totalRemaining unplaced item${if (totalRemaining > 1) "s" else ""}. If you exit now, unplaced items will be discarded. (Already placed items will remain saved in their folders).",
                color = TextSecondary,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            AppButton(
                text = "Discard & Exit",
                onClick = onConfirm,
                variant = AppButtonVariant.DESTRUCTIVE,
                height = 36.dp,
                cornerRadius = 12.dp
            )
        },
        dismissButton = {
            AppButton(
                text = "Stay",
                onClick = onDismiss,
                variant = AppButtonVariant.OUTLINED,
                height = 36.dp,
                cornerRadius = 12.dp
            )
        }
    )
}

@Composable
fun SharePlacementNewSubfolderDialog(
    folderName: String,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var newSubfolderName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidnightSurface,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "New Subfolder",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column {
                Text(
                    text = "Create a subfolder inside $folderName:",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = newSubfolderName,
                    onValueChange = { newSubfolderName = it },
                    placeholder = { Text("Subfolder name (e.g. Chapter 1)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = FolderTabCream,
                        unfocusedBorderColor = MidnightCardOutline
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            AppButton(
                text = "Create",
                onClick = {
                    if (newSubfolderName.isNotBlank()) {
                        onCreate(newSubfolderName)
                    }
                },
                variant = AppButtonVariant.PRIMARY,
                height = 36.dp,
                cornerRadius = 12.dp
            )
        },
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = AppButtonVariant.OUTLINED,
                height = 36.dp,
                cornerRadius = 12.dp
            )
        }
    )
}
