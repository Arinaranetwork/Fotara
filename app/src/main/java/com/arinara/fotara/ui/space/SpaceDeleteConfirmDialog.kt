// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.space

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.ui.components.presets.AppButton
import com.arinara.fotara.ui.components.presets.AppButtonVariant
import com.arinara.fotara.ui.theme.ElectricSlateTokens

@Composable
fun SpaceDeleteConfirmDialog(
    spaceName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElectricSlateTokens.SurfaceElevated,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "Delete Space?",
                color = ElectricSlateTokens.TextPrimary,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Text(
                text = "Are you sure you want to delete '$spaceName'? Note contents and assignments will remain intact in the default space.",
                color = ElectricSlateTokens.TextMuted,
                fontFamily = ElmsSans,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            AppButton(
                text = "Delete",
                onClick = onConfirm,
                variant = AppButtonVariant.DESTRUCTIVE
            )
        },
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = AppButtonVariant.OUTLINED
            )
        }
    )
}
