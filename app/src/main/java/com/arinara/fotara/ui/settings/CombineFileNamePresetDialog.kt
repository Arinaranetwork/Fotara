// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.R
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.theme.HomeCardBorder
import com.arinara.fotara.theme.HomeCardSurface
import com.arinara.fotara.theme.HomeMainButtonBlue
import com.arinara.fotara.theme.HomeNearBlack
import com.arinara.fotara.theme.HomeSubtitleGray
import com.arinara.fotara.theme.TagCrimson
import com.arinara.fotara.util.FileNamePresetHelper

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CombineFileNamePresetDialog(
    initialPreset: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialPreset,
                selection = TextRange(initialPreset.length)
            )
        )
    }

    val currentText = textFieldValue.text
    val validation = remember(currentText) {
        FileNamePresetHelper.validatePreset(currentText)
    }

    val previewText = remember(currentText, validation.isValid) {
        if (validation.isValid) {
            FileNamePresetHelper.resolvePreset(
                presetTemplate = currentText,
                folderName = "CS101",
                itemCount = 3
            )
        } else {
            "—"
        }
    }

    val insertToken: (String) -> Unit = { token ->
        val text = textFieldValue.text
        val selection = textFieldValue.selection
        val start = selection.min
        val end = selection.max
        val newText = text.replaceRange(start, end, token)
        val newCursor = start + token.length
        textFieldValue = TextFieldValue(
            text = newText,
            selection = TextRange(newCursor)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HomeCardSurface,
        title = {
            Text(
                text = stringResource(R.string.setting_combine_file_name_dialog_title),
                color = Color.White,
                fontSize = 18.sp,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.setting_combine_file_name_dialog_desc),
                    color = HomeSubtitleGray,
                    fontSize = 13.sp,
                    fontFamily = ElmsSans
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = { textFieldValue = it },
                    singleLine = true,
                    isError = !validation.isValid,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = HomeMainButtonBlue,
                        unfocusedBorderColor = HomeCardBorder,
                        errorBorderColor = TagCrimson,
                        cursorColor = HomeMainButtonBlue,
                        focusedContainerColor = HomeNearBlack,
                        unfocusedContainerColor = HomeNearBlack
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Token chips
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val tokens = listOf(
                        "{folder}" to "{folder}",
                        "{date}" to "{date}",
                        "{time}" to "{time}",
                        "{count}" to "{count}"
                    )
                    for ((label, token) in tokens) {
                        PresetTokenChip(
                            label = label,
                            onClick = { insertToken(token) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Live Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(HomeNearBlack)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.setting_combine_file_name_preview_label, previewText),
                        color = if (validation.isValid) Color.White else HomeSubtitleGray,
                        fontSize = 13.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Error message
                if (!validation.isValid && validation.errorMessageRes != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val errMessage = if (validation.errorArg != null) {
                        stringResource(validation.errorMessageRes, validation.errorArg)
                    } else {
                        stringResource(validation.errorMessageRes)
                    }
                    Text(
                        text = errMessage,
                        color = TagCrimson,
                        fontSize = 12.sp,
                        fontFamily = ElmsSans
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (validation.isValid) {
                        onSave(currentText.trim())
                    }
                },
                enabled = validation.isValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = HomeMainButtonBlue,
                    contentColor = Color.White,
                    disabledContainerColor = HomeCardBorder,
                    disabledContentColor = HomeSubtitleGray
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.setting_combine_file_name_save),
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        dismissButton = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        val defaultVal = FileNamePresetHelper.DEFAULT_PRESET
                        textFieldValue = TextFieldValue(
                            text = defaultVal,
                            selection = TextRange(defaultVal.length)
                        )
                    }
                ) {
                    Text(
                        text = stringResource(R.string.setting_combine_file_name_reset),
                        color = HomeSubtitleGray,
                        fontSize = 12.sp,
                        fontFamily = ElmsSans
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(R.string.setting_combine_file_name_cancel),
                        color = HomeSubtitleGray,
                        fontFamily = ElmsSans
                    )
                }
            }
        }
    )
}

@Composable
private fun PresetTokenChip(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(HomeNearBlack)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            color = HomeMainButtonBlue,
            fontSize = 12.sp,
            fontFamily = ElmsSans,
            fontWeight = FontWeight.SemiBold
        )
    }
}
