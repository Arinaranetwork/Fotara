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
    onSelectSpace: (Long) -> Unit,
    onCreateSpace: (name: String, iconKey: String, colorHex: String, isPrivate: Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showCreateDialog by remember { mutableStateOf(false) }

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
                items(spaces, key = { it.id }) { space ->
                    val isActive = space.id == activeSpaceId
                    SpaceItemRow(
                        space = space,
                        isActive = isActive,
                        onClick = {
                            onSelectSpace(space.id)
                            onDismiss()
                        }
                    )
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
    onClick: () -> Unit
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
                text = if (space.id == Space.DEFAULT_SPACE_ID) "Primary coursework vault" else "Dedicated study container",
                fontFamily = ElmsSans,
                fontSize = 12.sp,
                color = SpaceTextSecondary
            )
        }

        if (isActive) {
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
