// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.R
import com.arinara.fotara.ui.components.presets.AppButton
import com.arinara.fotara.ui.components.presets.AppButtonVariant
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.theme.FotaraShapes
import com.arinara.fotara.theme.HomeCardSurface
import com.arinara.fotara.theme.HomeSubtitleGray
import com.arinara.fotara.theme.TagAmber
import com.arinara.fotara.theme.TagEmerald

@Composable
internal fun BannerSourceDialog(
    onDismiss: () -> Unit,
    onPickImage: () -> Unit,
    onPickGif: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = FotaraShapes.Dialog,
        title = {
            Text(
                text = stringResource(R.string.profile_action_change_banner),
                color = Color.White,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPickImage() }
                        .padding(vertical = 12.dp, horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.profile_banner_choose_image),
                        color = Color.White,
                        fontFamily = ElmsSans,
                        fontSize = 16.sp
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPickGif() }
                        .padding(vertical = 12.dp, horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = TagEmerald,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.profile_banner_choose_gif),
                        color = Color.White,
                        fontFamily = ElmsSans,
                        fontSize = 16.sp
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            AppButton(
                text = stringResource(R.string.action_cancel),
                onClick = onDismiss,
                variant = AppButtonVariant.SECONDARY,
                height = 36.dp
            )
        }
    )
}

@Composable
internal fun OpenSourceLicensesDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HomeCardSurface,
        title = {
            Text(
                text = "Open Source Notices",
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
                    .height(280.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                LicenseItem(
                    name = "Jetpack Compose & AndroidX",
                    license = "Apache License 2.0",
                    copyright = "Copyright (c) The Android Open Source Project"
                )
                LicenseItem(
                    name = "Google ML Kit Text Recognition",
                    license = "Apache License 2.0",
                    copyright = "Copyright (c) Google LLC"
                )
                LicenseItem(
                    name = "Kotlin Coroutines & Serialization",
                    license = "Apache License 2.0",
                    copyright = "Copyright (c) JetBrains s.r.o."
                )
                LicenseItem(
                    name = "Coil Image Loading",
                    license = "Apache License 2.0",
                    copyright = "Copyright (c) Coil Contributors"
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            AppButton(
                text = "Close",
                onClick = onDismiss,
                variant = AppButtonVariant.SECONDARY,
                height = 36.dp
            )
        }
    )
}

@Composable
private fun LicenseItem(name: String, license: String, copyright: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = name, color = Color.White, fontSize = 14.sp, fontFamily = ElmsSans, fontWeight = FontWeight.Medium)
        Text(text = license, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontFamily = ElmsSans, fontWeight = FontWeight.Light)
        Text(text = copyright, color = HomeSubtitleGray, fontSize = 11.sp, fontFamily = ElmsSans, fontWeight = FontWeight.Light)
        Spacer(modifier = Modifier.height(6.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
internal fun <T> OptionSelectionDialog(
    title: String,
    options: List<Pair<T, String>>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = FotaraShapes.Dialog,
        title = {
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                options.forEach { (option, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOptionSelected(option) }
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(
                            selected = option == selectedOption,
                            onClick = { onOptionSelected(option) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            color = if (option == selectedOption) Color.White else HomeSubtitleGray,
                            fontSize = 14.sp,
                            fontFamily = ElmsSans,
                            fontWeight = if (option == selectedOption) FontWeight.Medium else FontWeight.Light
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = AppButtonVariant.SECONDARY,
                height = 36.dp
            )
        }
    )
}

@Composable
internal fun SavedImageLocationDialog(
    currentLocationKey: String,
    currentCustomName: String,
    onSave: (locationKey: String, customName: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedOption by remember { mutableStateOf(currentLocationKey) }
    var customNameText by remember { mutableStateOf(currentCustomName) }

    val trimmed = customNameText.trim()
    val isCustomValid = selectedOption != "custom" || (
        trimmed.isNotEmpty() && trimmed.length <= 30 && trimmed.matches(Regex("^[a-zA-Z0-9 _-]+$"))
    )

    val currentEffectivePath = when (selectedOption) {
        "dcim_fotara" -> "DCIM/Fotara"
        "custom" -> {
            val folder = trimmed.ifEmpty { "Fotara" }
            "Pictures/$folder"
        }
        else -> "Pictures/Fotara"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = FotaraShapes.Dialog,
        title = {
            Text(
                text = stringResource(R.string.settings_saved_image_location_title),
                color = Color.White,
                fontSize = 18.sp,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = currentEffectivePath,
                    color = TagAmber,
                    fontSize = 12.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Option 1: Pictures/Fotara
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedOption = "pictures_fotara" }
                        .padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = selectedOption == "pictures_fotara",
                        onClick = { selectedOption = "pictures_fotara" },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.primary,
                            unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.settings_saved_image_location_pictures),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontFamily = ElmsSans
                    )
                }

                // Option 2: DCIM/Fotara
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedOption = "dcim_fotara" }
                        .padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = selectedOption == "dcim_fotara",
                        onClick = { selectedOption = "dcim_fotara" },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.primary,
                            unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.settings_saved_image_location_dcim),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontFamily = ElmsSans
                    )
                }

                // Option 3: Custom folder under Pictures
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedOption = "custom" }
                        .padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = selectedOption == "custom",
                        onClick = { selectedOption = "custom" },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.primary,
                            unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.settings_saved_image_location_custom),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontFamily = ElmsSans
                    )
                }

                if (selectedOption == "custom") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customNameText,
                        onValueChange = { input ->
                            val filtered = input.filter { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it == ' ' || it == '-' || it == '_' }.take(30)
                            customNameText = filtered
                        },
                        label = { Text(stringResource(R.string.settings_saved_image_custom_folder_hint)) },
                        singleLine = true,
                        shape = FotaraShapes.InputField,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.settings_saved_image_legacy_notice),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontFamily = ElmsSans
                    )
                }
            }
        },
        confirmButton = {
            AppButton(
                text = "Save",
                onClick = { onSave(selectedOption, customNameText.trim()) },
                enabled = isCustomValid,
                variant = AppButtonVariant.PRIMARY,
                height = 36.dp
            )
        },
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = AppButtonVariant.SECONDARY,
                height = 36.dp
            )
        }
    )
}
