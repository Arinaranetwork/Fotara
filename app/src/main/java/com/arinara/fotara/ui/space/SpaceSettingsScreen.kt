// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.space

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import com.arinara.fotara.ui.components.SettingsSubScreenHeader
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.data.model.Space
import com.arinara.fotara.data.model.WorkspaceIcons
import com.arinara.fotara.data.repository.SpaceRepository
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.theme.FotaraShapes
import com.arinara.fotara.theme.HomeCardBorder
import com.arinara.fotara.theme.HomeCardSurface
import com.arinara.fotara.theme.HomeMainButtonBlue
import com.arinara.fotara.theme.HomeNearBlack
import com.arinara.fotara.theme.TagCrimson
import com.arinara.fotara.theme.TextMuted
import com.arinara.fotara.theme.TextPrimary
import com.arinara.fotara.theme.TextSecondary
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

private val SpacePresetColors = listOf(
    "#2563EB", // Blue
    "#7C3AED", // Purple
    "#059669", // Emerald
    "#D97706", // Amber
    "#DC2626", // Red
    "#0D9488", // Teal
    "#4F46E5"  // Indigo
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpaceSettingsScreen(
    spaceId: Long,
    spaceRepository: SpaceRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var currentSpace by remember { mutableStateOf<Space?>(null) }
    var spaceName by remember { mutableStateOf("") }
    var selectedIconKey by remember { mutableStateOf("school") }
    var selectedColorHex by remember { mutableStateOf("#2563EB") }
    var isPrivateVault by remember { mutableStateOf(false) }
    var userFeedback by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(spaceId) {
        val sp = spaceRepository.getSpaceById(spaceId) ?: Space.DEFAULT_SPACE
        currentSpace = sp
        spaceName = sp.name
        selectedIconKey = sp.iconKey
        selectedColorHex = sp.colorHex
        isPrivateVault = sp.isPrivate
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Header
        SettingsSubScreenHeader(
            title = "Space Settings",
            subtitle = currentSpace?.name ?: "Academic Space",
            onBackClick = onBack,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Feedback notification banner
            AnimatedVisibility(visible = userFeedback != null) {
                userFeedback?.let { msg ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        shape = FotaraShapes.CardSmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(msg, color = TextPrimary, fontSize = 13.sp, fontFamily = ElmsSans)
                        }
                    }
                }
            }

            // SECTION 1: Space Information & Customization
            Text(
                text = "SPACE INFORMATION",
                fontFamily = ElmsSans,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = FotaraShapes.Card,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Space Name",
                        fontFamily = ElmsSans,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = spaceName,
                        onValueChange = { spaceName = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background
                        ),
                        shape = FotaraShapes.InputField,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Space Icon",
                        fontFamily = ElmsSans,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WorkspaceIcons.ALL_ICONS.take(12).forEach { item ->
                            val isSelected = selectedIconKey == item.key
                            val iconColor = if (isSelected) MaterialTheme.colorScheme.primary else TextMuted
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(FotaraShapes.InputField)
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                        shape = FotaraShapes.InputField
                                    )
                                    .clickable { selectedIconKey = item.key },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = item.resId),
                                    contentDescription = item.label,
                                    tint = iconColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Theme Accent Color",
                        fontFamily = ElmsSans,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SpacePresetColors.forEach { hex ->
                            val isSelected = selectedColorHex.equals(hex, ignoreCase = true)
                            val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { HomeMainButtonBlue }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 0.dp,
                                        color = if (isSelected) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorHex = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Private Vault Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Private Space Vault",
                                    fontFamily = ElmsSans,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Requires biometric or PIN unlock to access",
                                    fontFamily = ElmsSans,
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                        Switch(
                            checked = isPrivateVault,
                            onCheckedChange = { isPrivateVault = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Save Button
                    Button(
                        onClick = {
                            val sp = currentSpace ?: return@Button
                            val updated = sp.copy(
                                name = spaceName.trim().ifBlank { sp.name },
                                iconKey = selectedIconKey,
                                colorHex = selectedColorHex,
                                isPrivate = isPrivateVault,
                                updatedAt = System.currentTimeMillis()
                            )
                            coroutineScope.launch {
                                spaceRepository.updateSpace(updated)
                                currentSpace = updated
                                userFeedback = "Space settings successfully saved"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = FotaraShapes.Button,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Save, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Space Changes", fontFamily = ElmsSans, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    }

                    if (currentSpace?.id != null && currentSpace?.id != Space.DEFAULT_SPACE_ID) {
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete This Space", color = MaterialTheme.colorScheme.error, fontFamily = ElmsSans, fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

        }
    }

    if (showDeleteConfirmDialog && currentSpace != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = FotaraShapes.Dialog,
            title = {
                Text("Delete Space?", color = TextPrimary, fontFamily = ElmsSans, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Are you sure you want to delete '${currentSpace?.name}'? Note contents and assignments will remain intact in the default space.",
                    color = TextSecondary,
                    fontFamily = ElmsSans,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        coroutineScope.launch {
                            currentSpace?.id?.let { spaceRepository.deleteSpace(it) }
                            onBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = FotaraShapes.Button
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.onError, fontFamily = ElmsSans, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondary, fontFamily = ElmsSans)
                }
            }
        )
    }
}
