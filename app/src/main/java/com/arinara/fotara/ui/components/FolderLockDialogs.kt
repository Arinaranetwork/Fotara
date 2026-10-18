// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.ui.components.presets.AppButton
import com.arinara.fotara.ui.components.presets.AppButtonVariant
import com.arinara.fotara.ui.theme.ElectricSlateTokens

@Composable
fun FolderUnlockDialog(
    folder: Folder,
    onDismiss: () -> Unit,
    onUnlocked: () -> Unit,
    onUseDeviceLock: () -> Unit,
    onForgotPin: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isPinVisible by remember { mutableStateOf(false) }

    fun verifyPin() {
        val trimmed = pin.trim()
        if (trimmed == folder.lockPin) {
            onUnlocked()
        } else {
            errorMessage = "Incorrect PIN. Try again or use device lock."
            pin = ""
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElectricSlateTokens.SurfacePanel,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.2f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = ElectricSlateTokens.PrimaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Unlock Folder",
                        color = ElectricSlateTokens.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = folder.name,
                        color = ElectricSlateTokens.TextMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Enter your 4-digit PIN to access this study folder.",
                    color = ElectricSlateTokens.TextMuted,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                            pin = it
                            errorMessage = null
                            if (it.length == 4) {
                                if (it == folder.lockPin) {
                                    onUnlocked()
                                } else {
                                    errorMessage = "Incorrect PIN."
                                }
                            }
                        }
                    },
                    label = { Text("4-Digit PIN") },
                    singleLine = true,
                    visualTransformation = if (isPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { verifyPin() }),
                    trailingIcon = {
                        IconButton(onClick = { isPinVisible = !isPinVisible }) {
                            Icon(
                                imageVector = if (isPinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isPinVisible) "Hide PIN" else "Show PIN",
                                tint = ElectricSlateTokens.TextMuted
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ElectricSlateTokens.TextPrimary,
                        unfocusedTextColor = ElectricSlateTokens.TextPrimary,
                        focusedBorderColor = ElectricSlateTokens.PrimaryAccent,
                        unfocusedBorderColor = ElectricSlateTokens.BorderSubtle,
                        cursorColor = ElectricSlateTokens.PrimaryAccent,
                        focusedLabelColor = ElectricSlateTokens.PrimaryAccent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage!!,
                        color = ElectricSlateTokens.Destructive,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                AppButton(
                    text = "Use Device Lock / Biometrics",
                    onClick = onUseDeviceLock,
                    variant = AppButtonVariant.OUTLINED,
                    leadingIcon = Icons.Default.Fingerprint,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                AppButton(
                    text = "Forgot PIN? Verify with phone lock",
                    onClick = onForgotPin,
                    variant = AppButtonVariant.SECONDARY,
                    height = 36.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            AppButton(
                text = "Unlock",
                onClick = { verifyPin() },
                enabled = pin.length == 4,
                variant = AppButtonVariant.PRIMARY
            )
        },
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = AppButtonVariant.SECONDARY
            )
        }
    )
}

@Composable
fun SetFolderLockDialog(
    folder: Folder,
    onDismiss: () -> Unit,
    onConfirmPin: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isPinVisible by remember { mutableStateOf(false) }

    fun validateAndSave() {
        when {
            pin.length != 4 -> {
                errorMessage = "PIN must be exactly 4 digits"
            }
            pin != confirmPin -> {
                errorMessage = "PINs do not match"
            }
            else -> {
                onConfirmPin(pin)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElectricSlateTokens.SurfacePanel,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.2f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = ElectricSlateTokens.PrimaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Lock Folder",
                        color = ElectricSlateTokens.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = folder.name,
                        color = ElectricSlateTokens.TextMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Set a 4-digit PIN to protect this folder. You can also use your device fingerprint or screen lock to open it anytime.",
                    color = ElectricSlateTokens.TextMuted,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                            pin = it
                            errorMessage = null
                        }
                    },
                    label = { Text("4-Digit PIN") },
                    singleLine = true,
                    visualTransformation = if (isPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Next
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ElectricSlateTokens.TextPrimary,
                        unfocusedTextColor = ElectricSlateTokens.TextPrimary,
                        focusedBorderColor = ElectricSlateTokens.PrimaryAccent,
                        unfocusedBorderColor = ElectricSlateTokens.BorderSubtle,
                        cursorColor = ElectricSlateTokens.PrimaryAccent,
                        focusedLabelColor = ElectricSlateTokens.PrimaryAccent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = {
                        if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                            confirmPin = it
                            errorMessage = null
                        }
                    },
                    label = { Text("Confirm PIN") },
                    singleLine = true,
                    visualTransformation = if (isPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { validateAndSave() }),
                    trailingIcon = {
                        IconButton(onClick = { isPinVisible = !isPinVisible }) {
                            Icon(
                                imageVector = if (isPinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isPinVisible) "Hide PIN" else "Show PIN",
                                tint = ElectricSlateTokens.TextMuted
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ElectricSlateTokens.TextPrimary,
                        unfocusedTextColor = ElectricSlateTokens.TextPrimary,
                        focusedBorderColor = ElectricSlateTokens.PrimaryAccent,
                        unfocusedBorderColor = ElectricSlateTokens.BorderSubtle,
                        cursorColor = ElectricSlateTokens.PrimaryAccent,
                        focusedLabelColor = ElectricSlateTokens.PrimaryAccent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage!!,
                        color = ElectricSlateTokens.Destructive,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            AppButton(
                text = "Lock Folder",
                onClick = { validateAndSave() },
                enabled = pin.length == 4 && confirmPin.length == 4,
                variant = AppButtonVariant.PRIMARY
            )
        },
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = AppButtonVariant.SECONDARY
            )
        }
    )
}

@Composable
fun ResetFolderPinDialog(
    folder: Folder,
    onDismiss: () -> Unit,
    onSaveNewPin: (String) -> Unit,
    onRemoveLock: () -> Unit
) {
    var newPin by remember { mutableStateOf("") }
    var confirmNewPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isPinVisible by remember { mutableStateOf(false) }

    fun validateAndSave() {
        when {
            newPin.length != 4 -> {
                errorMessage = "New PIN must be 4 digits"
            }
            newPin != confirmNewPin -> {
                errorMessage = "PINs do not match"
            }
            else -> {
                onSaveNewPin(newPin)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElectricSlateTokens.SurfacePanel,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.2f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = ElectricSlateTokens.PrimaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Reset Folder Lock",
                        color = ElectricSlateTokens.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = folder.name,
                        color = ElectricSlateTokens.TextMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Device identity verified. Set a new 4-digit PIN for this folder, or remove the lock entirely.",
                    color = ElectricSlateTokens.TextMuted,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = newPin,
                    onValueChange = {
                        if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                            newPin = it
                            errorMessage = null
                        }
                    },
                    label = { Text("New 4-Digit PIN") },
                    singleLine = true,
                    visualTransformation = if (isPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Next
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ElectricSlateTokens.TextPrimary,
                        unfocusedTextColor = ElectricSlateTokens.TextPrimary,
                        focusedBorderColor = ElectricSlateTokens.PrimaryAccent,
                        unfocusedBorderColor = ElectricSlateTokens.BorderSubtle,
                        cursorColor = ElectricSlateTokens.PrimaryAccent,
                        focusedLabelColor = ElectricSlateTokens.PrimaryAccent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = confirmNewPin,
                    onValueChange = {
                        if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                            confirmNewPin = it
                            errorMessage = null
                        }
                    },
                    label = { Text("Confirm New PIN") },
                    singleLine = true,
                    visualTransformation = if (isPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { validateAndSave() }),
                    trailingIcon = {
                        IconButton(onClick = { isPinVisible = !isPinVisible }) {
                            Icon(
                                imageVector = if (isPinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isPinVisible) "Hide PIN" else "Show PIN",
                                tint = ElectricSlateTokens.TextMuted
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ElectricSlateTokens.TextPrimary,
                        unfocusedTextColor = ElectricSlateTokens.TextPrimary,
                        focusedBorderColor = ElectricSlateTokens.PrimaryAccent,
                        unfocusedBorderColor = ElectricSlateTokens.BorderSubtle,
                        cursorColor = ElectricSlateTokens.PrimaryAccent,
                        focusedLabelColor = ElectricSlateTokens.PrimaryAccent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage!!,
                        color = ElectricSlateTokens.Destructive,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                AppButton(
                    text = "Remove Lock Entirely",
                    onClick = onRemoveLock,
                    variant = AppButtonVariant.DESTRUCTIVE,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            AppButton(
                text = "Save New PIN",
                onClick = { validateAndSave() },
                enabled = newPin.length == 4 && confirmNewPin.length == 4,
                variant = AppButtonVariant.PRIMARY
            )
        },
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = AppButtonVariant.SECONDARY
            )
        }
    )
}
