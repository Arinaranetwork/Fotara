// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.space

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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

private val SpaceBaseColor = Color(0xFF0A0D14)
private val SpaceCardColor = Color(0xFF111726)
private val SpacePrimaryColor = Color(0xFF2563EB)
private val SpaceAccentColor = Color(0xFFEFE8DA)
private val SpaceTextSecondary = Color(0xFFA0A5C2)
private val SpaceBorderColor = Color(0xFF1E283D)

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
        containerColor = SpaceBaseColor,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
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
                        color = SpaceTextSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = SpaceTextSecondary
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

                // 2. Prominent Private & Locked Spaces Entry
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = if (isPrivateUnlocked) Color(0xFF1E1438) else Color(0xFF13172C),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.2.dp, if (isPrivateUnlocked) Color(0xFF8B5CF6) else Color(0xFF374151)),
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
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isPrivateUnlocked) Color(0xFF8B5CF6).copy(alpha = 0.25f) else Color(0xFF4B5563).copy(alpha = 0.25f))
                                    .border(1.dp, if (isPrivateUnlocked) Color(0xFF8B5CF6) else Color(0xFF6B7280), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPrivateUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                    contentDescription = "Private Vault",
                                    tint = if (isPrivateUnlocked) Color(0xFFA78BFA) else Color(0xFF9CA3AF),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Locked & Private Spaces",
                                        fontFamily = ElmsSans,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = Color(0xFF7C3AED).copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = if (isPrivateUnlocked) "UNLOCKED" else "PIN / BIOMETRIC",
                                            color = Color(0xFFA78BFA),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isPrivateUnlocked) {
                                        if (privateSpaces.isEmpty()) "Vault unlocked. Create or move spaces here." else "${privateSpaces.size} private space(s) accessible"
                                    } else {
                                        "Tap to unlock private study vaults"
                                    },
                                    fontFamily = ElmsSans,
                                    fontSize = 12.sp,
                                    color = SpaceTextSecondary
                                )
                            }
                            TextButton(
                                onClick = {
                                    if (isPrivateUnlocked) {
                                        onLockPrivateVault?.invoke()
                                    } else {
                                        showSecurityGateDialog = true
                                    }
                                }
                            ) {
                                Text(
                                    text = if (isPrivateUnlocked) "Lock" else "Unlock",
                                    color = if (isPrivateUnlocked) Color(0xFFEF4444) else Color(0xFFA78BFA),
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
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

            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = SpaceCardColor),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .border(1.dp, SpaceBorderColor, RoundedCornerShape(8.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = SpacePrimaryColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Create New Space",
                    fontFamily = ElmsSans,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }
    }

    if (showSecurityGateDialog) {
        PrivateSpaceSecurityGateDialog(
            onDismiss = { showSecurityGateDialog = false },
            onSuccess = {
                showSecurityGateDialog = false
                onUnlockPrivateVault?.invoke()
            }
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
    val accentColor = try {
        Color(android.graphics.Color.parseColor(space.colorHex))
    } catch (_: Exception) {
        SpacePrimaryColor
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) SpaceCardColor else Color(0xFF0F1420))
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) SpacePrimaryColor else SpaceBorderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(accentColor.copy(alpha = 0.2f))
                .border(1.dp, accentColor, RoundedCornerShape(8.dp)),
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
                        tint = Color(0xFFA78BFA),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Text(
                text = if (space.id == Space.DEFAULT_SPACE_ID) "Academic Space (Default)" else "Academic Space #${space.id}",
                fontFamily = ElmsSans,
                fontSize = 12.sp,
                color = SpaceTextSecondary
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
                    tint = SpaceTextSecondary,
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
                    .background(SpacePrimaryColor),
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
                .clip(RoundedCornerShape(20.dp))
                .background(SpaceCardColor)
                .border(1.dp, SpaceBorderColor, RoundedCornerShape(20.dp))
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
                fontSize = 13.sp,
                color = SpaceTextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 30) name = it },
                label = { Text("Space Name", color = SpaceTextSecondary) },
                placeholder = { Text("e.g. Semester 5, Research Lab", color = Color(0xFF6B7280)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = SpacePrimaryColor,
                    unfocusedBorderColor = SpaceBorderColor
                ),
                shape = RoundedCornerShape(8.dp),
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
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { selectedColor = hex },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
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
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) SpacePrimaryColor.copy(alpha = 0.3f) else Color(0xFF1E283D))
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) SpacePrimaryColor else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedIcon = iconKey },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = WorkspaceIcons.getIconResId(iconKey)),
                            contentDescription = null,
                            tint = if (isSelected) SpacePrimaryColor else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F1420))
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
                        color = SpaceTextSecondary
                    )
                }
                Switch(
                    checked = isPrivate,
                    onCheckedChange = { isPrivate = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = SpacePrimaryColor
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Text("Cancel", color = SpaceTextSecondary, fontFamily = ElmsSans)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { onCreate(name, selectedIcon, selectedColor, isPrivate) },
                    enabled = name.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = SpacePrimaryColor),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Create", color = Color.White, fontFamily = ElmsSans, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun PrivateSpaceSecurityGateDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isPinVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141936),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF7C3AED).copy(alpha = 0.25f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFFA78BFA),
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
                        color = Color(0xFF8E9AAF),
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
                    color = Color(0xFFEAE3D2).copy(alpha = 0.85f),
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
                    label = { Text("4-6 Digit Security PIN", color = Color(0xFF8E9AAF)) },
                    placeholder = { Text("••••", color = Color(0xFF8E9AAF).copy(alpha = 0.5f)) },
                    singleLine = true,
                    isError = errorMsg != null,
                    visualTransformation = if (isPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (pin.length >= 4) {
                                onSuccess()
                            } else {
                                errorMsg = "PIN must be at least 4 digits"
                            }
                        }
                    ),
                    trailingIcon = {
                        IconButton(onClick = { isPinVisible = !isPinVisible }) {
                            Icon(
                                imageVector = if (isPinVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (isPinVisible) "Hide PIN" else "Show PIN",
                                tint = Color(0xFF8E9AAF)
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF7C3AED),
                        unfocusedBorderColor = Color(0xFF28325E),
                        focusedTextColor = Color(0xFFEAE3D2),
                        unfocusedTextColor = Color(0xFFEAE3D2),
                        cursorColor = Color(0xFF7C3AED)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMsg!!,
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        fontFamily = ElmsSans
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { onSuccess() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFA78BFA)),
                    border = BorderStroke(1.dp, Color(0xFF7C3AED).copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Use Biometrics / Device Lock",
                        fontSize = 13.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pin.length >= 4) {
                        onSuccess()
                    } else {
                        errorMsg = "PIN must be at least 4 digits"
                    }
                },
                enabled = pin.length >= 4,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Unlock", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = ElmsSans)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF8E9AAF), fontFamily = ElmsSans)
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

