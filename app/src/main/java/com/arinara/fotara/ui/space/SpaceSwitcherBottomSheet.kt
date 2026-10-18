// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.space

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.arinara.fotara.theme.FotaraShapes
import com.arinara.fotara.theme.FotaraSpacing
import com.arinara.fotara.theme.TagCrimson
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import com.arinara.fotara.ui.components.presets.AppButton
import com.arinara.fotara.ui.components.presets.AppButtonVariant
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.arinara.fotara.data.model.Space
import com.arinara.fotara.data.model.WorkspaceIcons
import com.arinara.fotara.theme.ElmsSans

private val SpacePresetColors = listOf(
    "#2563EB", // Blue
    "#7C3AED", // Purple
    "#059669", // Emerald
    "#D97706", // Amber
    "#DC2626", // Red
    "#0D9488", // Teal
    "#4F46E5"  // Indigo
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceSwitcherBottomSheet(
    spaces: List<Space>,
    activeSpaceId: Long,
    isPrivateUnlocked: Boolean = false,
    onSelectSpace: (Long) -> Unit,
    onCreateSpace: (name: String, iconKey: String, colorHex: String, isPrivate: Boolean) -> Unit,
    onOpenSpaceSettings: ((Long) -> Unit)? = null,
    onUnlockPrivateVault: (() -> Unit)? = null,
    onLockPrivateVault: (() -> Unit)? = null,
    onVerifyPin: ((String) -> Boolean)? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showCreateDialog by remember { mutableStateOf(false) }
    var showSecurityGateDialog by remember { mutableStateOf(false) }

    val standardSpaces = remember(spaces) { spaces.filter { !it.isPrivate } }
    val privateSpaces = remember(spaces) { spaces.filter { it.isPrivate } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        shape = FotaraShapes.BottomSheet,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Academic Spaces",
                        fontFamily = ElmsSans,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Isolated vaults for courses and projects",
                        fontFamily = ElmsSans,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // 1. Standard Spaces
                items(standardSpaces, key = { it.id }) { space ->
                    val isActive = space.id == activeSpaceId
                    SpaceItemRow(
                        space = space,
                        isActive = isActive,
                        onClick = {
                            onSelectSpace(space.id)
                            onDismiss()
                        },
                        onOpenSettings = onOpenSpaceSettings?.let { callback ->
                            {
                                onDismiss()
                                callback(space.id)
                            }
                        }
                    )
                }

                // 2. Prominent Private & Locked Spaces Entry (Minimalist Single-Row Card)
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = if (isPrivateUnlocked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = FotaraShapes.InputField,
                        border = BorderStroke(1.dp, if (isPrivateUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isPrivateUnlocked) {
                                    onLockPrivateVault?.invoke()
                                } else {
                                    showSecurityGateDialog = true
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(FotaraShapes.Button)
                                    .background(if (isPrivateUnlocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceContainerHighest),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPrivateUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                    contentDescription = "Private Space",
                                    tint = if (isPrivateUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Private Space",
                                fontFamily = ElmsSans,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            AppButton(
                                text = if (isPrivateUnlocked) "Lock" else "Unlock",
                                height = 36.dp,
                                variant = if (isPrivateUnlocked) AppButtonVariant.DESTRUCTIVE else AppButtonVariant.PRIMARY,
                                onClick = {
                                    if (isPrivateUnlocked) {
                                        onLockPrivateVault?.invoke()
                                    } else {
                                        showSecurityGateDialog = true
                                    }
                                }
                            )
                        }
                    }
                }

                // 3. Unlocked Private Spaces List
                if (isPrivateUnlocked) {
                    items(privateSpaces, key = { it.id }) { space ->
                        val isActive = space.id == activeSpaceId
                        SpaceItemRow(
                            space = space,
                            isActive = isActive,
                            onClick = {
                                onSelectSpace(space.id)
                                onDismiss()
                            },
                            onOpenSettings = onOpenSpaceSettings?.let { callback ->
                                {
                                    onDismiss()
                                    callback(space.id)
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AppButton(
                text = "Create New Space",
                leadingIcon = Icons.Default.Add,
                variant = AppButtonVariant.SECONDARY,
                height = 56.dp,
                onClick = { showCreateDialog = true },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showSecurityGateDialog) {
        PrivateSpaceSecurityGateDialog(
            onDismiss = { showSecurityGateDialog = false },
            onSuccess = {
                showSecurityGateDialog = false
                onUnlockPrivateVault?.invoke()
            },
            onVerifyPin = onVerifyPin
        )
    }

    if (showCreateDialog) {
        CreateSpaceDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, iconKey, colorHex, isPrivate ->
                onCreateSpace(name, iconKey, colorHex, isPrivate)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun SpaceItemRow(
    space: Space,
    isActive: Boolean,
    onClick: () -> Unit,
    onOpenSettings: (() -> Unit)? = null
) {
    val defaultPrimary = MaterialTheme.colorScheme.primary
    val accentColor = try {
        Color(android.graphics.Color.parseColor(space.colorHex))
    } catch (_: Exception) {
        defaultPrimary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FotaraShapes.InputField)
            .background(if (isActive) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.background)
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = FotaraShapes.InputField
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(FotaraShapes.Button)
                .background(accentColor.copy(alpha = 0.2f))
                .border(1.dp, accentColor, FotaraShapes.Button),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = WorkspaceIcons.getIconResId(space.iconKey)),
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = space.name,
                    fontFamily = ElmsSans,
                    fontSize = 16.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    color = Color.White
                )
                if (space.isPrivate) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Private Vault",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Text(
                text = if (space.id == Space.DEFAULT_SPACE_ID) "Academic Space (Default)" else "Academic Space #${space.id}",
                fontFamily = ElmsSans,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (onOpenSettings != null) {
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Space Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (isActive) {
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Active",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun CreateSpaceDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, iconKey: String, colorHex: String, isPrivate: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(SpacePresetColors.first()) }
    var selectedIcon by remember { mutableStateOf("school") }
    var isPrivate by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(FotaraShapes.Dialog)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, FotaraShapes.Dialog)
                .padding(20.dp)
        ) {
            Text(
                text = "New Academic Space",
                fontFamily = ElmsSans,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Set up a distinct vault for your studies or projects.",
                fontFamily = ElmsSans,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 30) name = it },
                label = { Text("Space Name", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                placeholder = { Text("e.g. Semester 5, Research Lab", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                shape = FotaraShapes.InputField,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Accent Color",
                fontFamily = ElmsSans,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(SpacePresetColors) { hex ->
                    val color = Color(android.graphics.Color.parseColor(hex))
                    val isSelected = selectedColor == hex
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) ElectricSlateTokens.PrimaryAccent else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { selectedColor = hex },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = ElectricSlateTokens.TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Icon",
                fontFamily = ElmsSans,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            val sampleIcons = listOf("school", "book", "science", "calculate", "code", "palette", "work", "star")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sampleIcons) { iconKey ->
                    val isSelected = selectedIcon == iconKey
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(FotaraShapes.Button)
                            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceContainerHigh)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = FotaraShapes.Button
                            )
                            .clickable { selectedIcon = iconKey },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = WorkspaceIcons.getIconResId(iconKey)),
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(FotaraShapes.Button)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Private Stealth Vault",
                        fontFamily = ElmsSans,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Text(
                        text = "Shields contents and requires biometric unlock",
                        fontFamily = ElmsSans,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isPrivate,
                    onCheckedChange = { isPrivate = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                AppButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    variant = AppButtonVariant.OUTLINED
                )
                Spacer(modifier = Modifier.width(8.dp))
                AppButton(
                    text = "Create",
                    onClick = { onCreate(name, selectedIcon, selectedColor, isPrivate) },
                    enabled = name.isNotBlank(),
                    variant = AppButtonVariant.PRIMARY
                )
            }
        }
    }
}

@Composable
fun PrivateSpaceSecurityGateDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    onVerifyPin: ((String) -> Boolean)? = null
) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isPinVisible by remember { mutableStateOf(false) }

    fun submitPin() {
        if (pin.length >= 4) {
            val verified = onVerifyPin?.invoke(pin) ?: true
            if (verified) {
                onSuccess()
            } else {
                errorMsg = "Incorrect PIN. Please try again."
            }
        } else {
            errorMsg = "PIN must be at least 4 digits"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Private Space Vault",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ElmsSans
                    )
                    Text(
                        text = "Security Verification",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontFamily = ElmsSans
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Enter your security PIN or authenticate using device biometrics to access locked spaces.",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.5.sp,
                    fontFamily = ElmsSans
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 6 && it.all { ch -> ch.isDigit() }) {
                            pin = it
                            errorMsg = null
                        }
                    },
                    label = { Text("4-6 Digit Security PIN", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    placeholder = { Text("••••", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                    singleLine = true,
                    isError = errorMsg != null,
                    shape = FotaraShapes.InputField,
                    visualTransformation = if (isPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { submitPin() }
                    ),
                    trailingIcon = {
                        IconButton(onClick = { isPinVisible = !isPinVisible }) {
                            Icon(
                                imageVector = if (isPinVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (isPinVisible) "Hide PIN" else "Show PIN",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMsg!!,
                        color = TagCrimson,
                        fontSize = 12.sp,
                        fontFamily = ElmsSans
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                AppButton(
                    text = "Use Biometrics / Device Lock",
                    icon = Icons.Default.Fingerprint,
                    variant = AppButtonVariant.OUTLINED,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val activity = context.findFragmentActivity()
                        if (activity != null) {
                            val executor = ContextCompat.getMainExecutor(context)
                            val biometricPrompt = BiometricPrompt(
                                activity,
                                executor,
                                object : BiometricPrompt.AuthenticationCallback() {
                                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                        super.onAuthenticationSucceeded(result)
                                        onSuccess()
                                    }
                                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                        super.onAuthenticationError(errorCode, errString)
                                        if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                                            errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                                            errorCode != BiometricPrompt.ERROR_CANCELED
                                        ) {
                                            errorMsg = errString.toString()
                                        }
                                    }
                                }
                            )
                            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                                .setTitle("Unlock Private Space")
                                .setSubtitle("Authenticate using biometric identity or device credentials")
                                .setAllowedAuthenticators(
                                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
                                )
                                .build()
                            try {
                                biometricPrompt.authenticate(promptInfo)
                            } catch (e: Exception) {
                                errorMsg = e.message ?: "Authentication error"
                            }
                        } else {
                            errorMsg = "Device authentication not available"
                        }
                    }
                )
            }
        },
        confirmButton = {
            AppButton(
                text = "Unlock",
                enabled = pin.length >= 4,
                variant = AppButtonVariant.PRIMARY,
                onClick = { submitPin() }
            )
        },
        dismissButton = {
            AppButton(
                text = "Cancel",
                variant = AppButtonVariant.OUTLINED,
                onClick = onDismiss
            )
        },
        shape = FotaraShapes.Dialog
    )
}

private fun Context.findFragmentActivity(): FragmentActivity? {
    var cur = this
    while (cur is ContextWrapper) {
        if (cur is FragmentActivity) return cur
        cur = cur.baseContext
    }
    return null
}

