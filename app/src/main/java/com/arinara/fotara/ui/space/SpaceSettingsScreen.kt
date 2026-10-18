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
import com.arinara.fotara.ui.components.SettingsSubScreenHeader
import com.arinara.fotara.ui.components.presets.AppButton
import com.arinara.fotara.ui.components.presets.AppButtonVariant
import com.arinara.fotara.ui.components.presets.AppCard
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
                    AppCard(
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
                            Text(msg, color = TextPrimary, fontSize = 14.sp, fontFamily = ElmsSans)
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

            AppCard(
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
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) ElectricSlateTokens.PrimaryAccent else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorHex = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, null, tint = ElectricSlateTokens.TextPrimary, modifier = Modifier.size(18.dp))
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
                    AppButton(
                        text = "Save Space Changes",
                        icon = Icons.Default.Save,
                        variant = AppButtonVariant.PRIMARY,
                        onClick = {
                            val sp = currentSpace ?: return@AppButton
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    )

                    if (currentSpace?.id != null && currentSpace?.id != Space.DEFAULT_SPACE_ID) {
                        Spacer(modifier = Modifier.height(12.dp))
                        AppButton(
                            text = "Delete This Space",
                            icon = Icons.Default.Delete,
                            variant = AppButtonVariant.DESTRUCTIVE,
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))

        }
    }

    if (showDeleteConfirmDialog && currentSpace != null) {
        SpaceDeleteConfirmDialog(
            spaceName = currentSpace?.name ?: "",
            onConfirm = {
                showDeleteConfirmDialog = false
                coroutineScope.launch {
                    currentSpace?.id?.let { spaceRepository.deleteSpace(it) }
                    onBack()
                }
            },
            onDismiss = { showDeleteConfirmDialog = false }
        )
    }
}
