// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.note

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.R
import com.arinara.fotara.ui.components.presets.AppButton
import com.arinara.fotara.ui.components.presets.AppButtonVariant
import com.arinara.fotara.ui.theme.ElectricSlateTokens

@Composable
internal fun TextNoteLinkDialog(
    label: String,
    onLabelChange: (String) -> Unit,
    url: String,
    onUrlChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.editor_dialog_link_title),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = ElectricSlateTokens.TextPrimary
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = label,
                    onValueChange = onLabelChange,
                    label = { Text(stringResource(R.string.editor_dialog_link_label), color = ElectricSlateTokens.TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricSlateTokens.PrimaryAccent,
                        unfocusedBorderColor = ElectricSlateTokens.BorderSubtle,
                        focusedTextColor = ElectricSlateTokens.TextPrimary,
                        unfocusedTextColor = ElectricSlateTokens.TextPrimary
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = onUrlChange,
                    label = { Text(stringResource(R.string.editor_dialog_link_url), color = ElectricSlateTokens.TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricSlateTokens.PrimaryAccent,
                        unfocusedBorderColor = ElectricSlateTokens.BorderSubtle,
                        focusedTextColor = ElectricSlateTokens.TextPrimary,
                        unfocusedTextColor = ElectricSlateTokens.TextPrimary
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            AppButton(
                text = stringResource(R.string.editor_dialog_link_apply),
                onClick = onConfirm,
                variant = AppButtonVariant.PRIMARY,
                height = 36.dp
            )
        },
        dismissButton = {
            AppButton(
                text = stringResource(R.string.editor_dialog_link_cancel),
                onClick = onDismiss,
                variant = AppButtonVariant.SECONDARY,
                height = 36.dp
            )
        },
        containerColor = ElectricSlateTokens.SurfaceElevated,
        shape = RoundedCornerShape(16.dp)
    )
}
