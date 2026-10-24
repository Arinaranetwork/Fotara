// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.collab.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.feature.collab.model.CollabAccessMode
import com.arinara.fotara.feature.collab.model.CollabAudioBitrate
import com.arinara.fotara.feature.collab.model.CollabJoinPolicy
import com.arinara.fotara.feature.collab.model.CollabPeer
import com.arinara.fotara.feature.collab.model.CollabPrivacyPreset
import com.arinara.fotara.feature.collab.model.CollabRole
import com.arinara.fotara.feature.collab.model.CollabRoomCapacity
import com.arinara.fotara.feature.collab.render.PeerCursorRenderer
import com.arinara.fotara.feature.friends.model.FriendProfile
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.ui.components.presets.AppSlider
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import com.arinara.fotara.ui.theme.jigglePressClickable
import kotlin.math.roundToInt

private val WHITESPACE_REGEX = Regex("\\s+")

/**
 * Room & Audio Settings Bottom Sheet (`CollabRoomSettingsSheet`).
 * Provides:
 * 1. Global & Per-User Canvas Lock Controls (Disable Drawing & Disable Creating New Sheets)
 *    with automatic global-switch sync and configurable Access Mode (`HOST_AND_ADMIN` vs `HOST_ONLY`).
 * 2. Audio Bitrate Selection (`16 / 32 / 64 / 96 kbps`) and Host Room Max Bitrate Cap.
 * 3. Per-User Volume Control (`0% - 200%` via [AppSlider] + Local Mute toggle).
 * 4. Clickable Member Management Card for Host/Admins:
 *    - Make / Remove Admin (Host Only)
 *    - Transfer Host Ownership (Host Only -> target becomes Host, previous Host becomes normal Member)
 *    - Room-Mute / Unmute User (Host & Admin)
 *    - Block / Unblock User Chat (Host & Admin)
 *    - Per-User Disable Drawing & Disable Creating New Sheets (Host & Admin)
 *    - Kick Member — Cannot Join Again (Host Only)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollabRoomSettingsSheet(
    roomCode: String,
    localPeerId: String,
    localRole: CollabRole,
    peers: List<CollabPeer>,
    peerVolumes: Map<String, Float>,
    locallyMutedPeerIds: Set<String>,
    audioBitrateKbps: Int,
    roomMaxBitrateKbps: Int,
    accessMode: CollabAccessMode,
    isGlobalDrawingDisabled: Boolean,
    isGlobalSheetCreationDisabled: Boolean,
    isLocalMicMuted: Boolean = false,
    isRoomMutedByMod: Boolean = false,
    onToggleLocalMicMute: () -> Unit = {},
    maxParticipants: Int = CollabRoomCapacity.DEFAULT_MAX_PARTICIPANTS,
    joinPolicy: CollabJoinPolicy = CollabJoinPolicy.OPEN,
    allowedUserHandles: Set<String> = emptySet(),
    invitedUserHandles: Set<String> = emptySet(),
    savedPrivacyPresets: List<CollabPrivacyPreset> = emptyList(),
    activePresetId: String? = null,
    hostFriends: List<FriendProfile> = emptyList(),
    onSetMaxParticipants: (Int) -> Unit = {},
    onSetJoinPolicy: (CollabJoinPolicy) -> Unit = {},
    onAddAllowedUserHandle: (String) -> Unit = {},
    onRemoveAllowedUserHandle: (String) -> Unit = {},
    onInviteUser: (String) -> Unit = {},
    onRevokeInvitation: (String) -> Unit = {},
    onSavePrivacyPreset: (String) -> Unit = {},
    onApplyPrivacyPreset: (String) -> Unit = {},
    onDeletePrivacyPreset: (String) -> Unit = {},
    onSetPeerVolume: (String, Float) -> Unit,
    onToggleLocalPeerMute: (String) -> Unit,
    onSetAudioBitrate: (Int) -> Unit,
    onSetRoomMaxBitrate: (Int) -> Unit,
    onSetAccessMode: (CollabAccessMode) -> Unit,
    onSetGlobalDrawingDisabled: (Boolean) -> Unit,
    onSetGlobalSheetCreationDisabled: (Boolean) -> Unit,
    onSetPeerDrawingDisabled: (String, Boolean) -> Unit,
    onSetPeerSheetCreationDisabled: (String, Boolean) -> Unit,
    onSetPeerAdmin: (String, Boolean) -> Unit,
    onTransferHostOwnership: (String) -> Unit,
    onSetPeerRoomMuted: (String, Boolean) -> Unit,
    onSetPeerChatBlocked: (String, Boolean) -> Unit,
    onKickPeer: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var expandedPeerId by remember { mutableStateOf<String?>(null) }
    var customHandleInput by remember { mutableStateOf("") }
    var presetNameInput by remember { mutableStateOf("") }

    val isHost = localRole == CollabRole.HOST
    val isAdmin = localRole == CollabRole.ADMIN
    val canManageGlobalLocks = isHost || (isAdmin && accessMode == CollabAccessMode.HOST_AND_ADMIN)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = ElectricSlateTokens.SurfacePanel,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(ElectricSlateTokens.BorderSubtle)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.84f)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            // Sheet Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Room & Audio Settings • $roomCode",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = ElectricSlateTokens.TextPrimary
                    )
                    Text(
                        text = "Your Role: ${localRole.name} • Tap a participant to manage permissions",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 11.sp,
                        color = ElectricSlateTokens.TextMuted
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ElectricSlateTokens.SurfaceElevated)
                        .border(1.dp, ElectricSlateTokens.BorderSubtle, CircleShape)
                        .jigglePressClickable(onClick = onDismissRequest),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close Settings",
                        tint = ElectricSlateTokens.TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // =========================================================================
                // 0. PRIVACY & JOIN CONTROL (MAX USERS, FRIENDS / PRESET / INVITED ONLY)
                // =========================================================================
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(ElectricSlateTokens.BgCanvas)
                            .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Group,
                                    contentDescription = null,
                                    tint = ElectricSlateTokens.PrimaryAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Privacy & Join Control",
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = ElectricSlateTokens.TextPrimary
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(ElectricSlateTokens.SurfaceElevated)
                                    .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(999.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${peers.size} / $maxParticipants joined",
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = if (peers.size >= maxParticipants) ElectricSlateTokens.Destructive else ElectricSlateTokens.PrimaryAccent
                                )
                            }
                        }

                        // 0.1 Max Participants Limit (2..32)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Max Users Allowed to Join",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                            Text(
                                text = "$maxParticipants users",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = ElectricSlateTokens.PrimaryAccent
                            )
                        }

                        if (isHost) {
                            AppSlider(
                                value = maxParticipants.toFloat(),
                                onValueChange = { newMax ->
                                    onSetMaxParticipants(newMax.roundToInt())
                                },
                                valueRange = CollabRoomCapacity.MIN_PARTICIPANTS.toFloat()..CollabRoomCapacity.MAX_PARTICIPANTS.toFloat(),
                                steps = CollabRoomCapacity.MAX_PARTICIPANTS - CollabRoomCapacity.MIN_PARTICIPANTS - 1,
                                activeTrackColor = ElectricSlateTokens.PrimaryAccent,
                                inactiveTrackColor = ElectricSlateTokens.TrackInactive
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (cap in CollabRoomCapacity.PRESETS) {
                                    val isSelected = maxParticipants == cap
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(30.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) ElectricSlateTokens.PrimaryAccent
                                                else ElectricSlateTokens.SurfaceElevated
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) ElectricSlateTokens.PrimaryAccent
                                                else ElectricSlateTokens.BorderSubtle,
                                                RoundedCornerShape(12.dp)
                                            )
                                            .jigglePressClickable {
                                                onSetMaxParticipants(cap)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$cap max",
                                            fontFamily = ElmsSans,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 10.sp,
                                            color = ElectricSlateTokens.TextPrimary
                                        )
                                    }
                                }
                            }
                        }

                        // 0.2 Who Can Join Policy (Anyone, Friends Only, Custom Preset, Invited Only)
                        Text(
                            text = if (isHost) "Who Can Join This Room:" else "Current Room Join Policy:",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = ElectricSlateTokens.TextPrimary
                        )

                        val policyOptions = remember {
                            listOf(
                                CollabJoinPolicy.OPEN to "Anyone",
                                CollabJoinPolicy.FRIENDS_ONLY to "Friends Only",
                                CollabJoinPolicy.CUSTOM_PRESET to "Custom Preset",
                                CollabJoinPolicy.INVITED_ONLY to "Invited Only"
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (idx in 0..1) {
                                    val (policy, label) = policyOptions[idx]
                                    val isSelected = joinPolicy == policy
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(34.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) ElectricSlateTokens.PrimaryAccent
                                                else ElectricSlateTokens.SurfaceElevated
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) ElectricSlateTokens.PrimaryAccent
                                                else ElectricSlateTokens.BorderSubtle,
                                                RoundedCornerShape(12.dp)
                                            )
                                            .jigglePressClickable(enabled = isHost) {
                                                onSetJoinPolicy(policy)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontFamily = ElmsSans,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 11.sp,
                                            color = if (isSelected) ElectricSlateTokens.TextPrimary else ElectricSlateTokens.TextMuted
                                        )
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (idx in 2..3) {
                                    val (policy, label) = policyOptions[idx]
                                    val isSelected = joinPolicy == policy
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(34.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) ElectricSlateTokens.PrimaryAccent
                                                else ElectricSlateTokens.SurfaceElevated
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) ElectricSlateTokens.PrimaryAccent
                                                else ElectricSlateTokens.BorderSubtle,
                                                RoundedCornerShape(12.dp)
                                            )
                                            .jigglePressClickable(enabled = isHost) {
                                                onSetJoinPolicy(policy)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontFamily = ElmsSans,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 11.sp,
                                            color = if (isSelected) ElectricSlateTokens.TextPrimary else ElectricSlateTokens.TextMuted
                                        )
                                    }
                                }
                            }
                        }

                        // Policy Description Banner
                        val policySubtitle = when (joinPolicy) {
                            CollabJoinPolicy.OPEN -> "Anyone with the room code can join up to $maxParticipants participants."
                            CollabJoinPolicy.FRIENDS_ONLY -> "Only users in the Host's Friend List (${hostFriends.size} friends) can join."
                            CollabJoinPolicy.CUSTOM_PRESET -> "Only users matching the selected Custom Preset or allowed username/friend list (${allowedUserHandles.size} allowed) can join."
                            CollabJoinPolicy.INVITED_ONLY -> "Strict Invite-Only mode: only explicitly invited usernames or friends (${invitedUserHandles.size} invited) can join."
                        }
                        Text(
                            text = policySubtitle,
                            fontFamily = ElmsSans,
                            fontSize = 10.sp,
                            color = ElectricSlateTokens.TextMuted
                        )

                        // 0.3 Host Controls for CUSTOM_PRESET and INVITED_ONLY
                        if (isHost && (joinPolicy == CollabJoinPolicy.CUSTOM_PRESET || joinPolicy == CollabJoinPolicy.INVITED_ONLY)) {
                            val isCustomPresetMode = joinPolicy == CollabJoinPolicy.CUSTOM_PRESET
                            val activeHandleSet = if (isCustomPresetMode) allowedUserHandles else invitedUserHandles

                            // Saved Presets Selector + Save Preset Row (for Custom Preset mode)
                            if (isCustomPresetMode) {
                                if (savedPrivacyPresets.isNotEmpty()) {
                                    Text(
                                        text = "Saved Custom Presets:",
                                        fontFamily = ElmsSans,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        color = ElectricSlateTokens.TextPrimary
                                    )
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(savedPrivacyPresets, key = { it.id }) { preset ->
                                            val isPresetActive = activePresetId == preset.id
                                            Row(
                                                modifier = Modifier
                                                    .height(30.dp)
                                                    .clip(RoundedCornerShape(999.dp))
                                                    .background(
                                                        if (isPresetActive) ElectricSlateTokens.PrimaryAccent
                                                        else ElectricSlateTokens.SurfaceElevated
                                                    )
                                                    .border(
                                                        1.dp,
                                                        if (isPresetActive) ElectricSlateTokens.PrimaryAccent
                                                        else ElectricSlateTokens.BorderSubtle,
                                                        RoundedCornerShape(999.dp)
                                                    )
                                                    .jigglePressClickable {
                                                        onApplyPrivacyPreset(preset.id)
                                                    }
                                                    .padding(horizontal = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "${preset.name} (${preset.allowedHandles.size})",
                                                    fontFamily = ElmsSans,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp,
                                                    color = ElectricSlateTokens.TextPrimary
                                                )
                                                Icon(
                                                    imageVector = Icons.Outlined.Close,
                                                    contentDescription = "Delete Preset",
                                                    tint = ElectricSlateTokens.TextMuted,
                                                    modifier = Modifier
                                                        .size(13.dp)
                                                        .clickable { onDeletePrivacyPreset(preset.id) }
                                                )
                                            }
                                        }
                                    }
                                }

                                // Save Current Allowed List as Named Preset
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(ElectricSlateTokens.SurfaceElevated)
                                            .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                                            .padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (presetNameInput.isEmpty()) {
                                            Text(
                                                text = "Preset name (e.g. Lab Group A)...",
                                                fontFamily = ElmsSans,
                                                fontSize = 11.sp,
                                                color = ElectricSlateTokens.TextMuted
                                            )
                                        }
                                        BasicTextField(
                                            value = presetNameInput,
                                            onValueChange = { presetNameInput = it },
                                            singleLine = true,
                                            textStyle = TextStyle(
                                                fontFamily = ElmsSans,
                                                fontSize = 11.sp,
                                                color = ElectricSlateTokens.TextPrimary
                                            ),
                                            cursorBrush = SolidColor(ElectricSlateTokens.PrimaryAccent),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .height(36.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (presetNameInput.isNotBlank() && allowedUserHandles.isNotEmpty()) {
                                                    ElectricSlateTokens.PrimaryAccent
                                                } else {
                                                    ElectricSlateTokens.TrackInactive
                                                }
                                            )
                                            .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                                            .jigglePressClickable(
                                                enabled = presetNameInput.isNotBlank() && allowedUserHandles.isNotEmpty()
                                            ) {
                                                onSavePrivacyPreset(presetNameInput.trim())
                                                presetNameInput = ""
                                            }
                                            .padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Save Preset",
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = ElectricSlateTokens.TextPrimary
                                        )
                                    }
                                }
                            }

                            // Add by @username or Display Name input
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ElectricSlateTokens.SurfaceElevated)
                                        .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (customHandleInput.isEmpty()) {
                                        Text(
                                            text = if (isCustomPresetMode) "Add @username to allowlist..." else "Invite @username...",
                                            fontFamily = ElmsSans,
                                            fontSize = 11.sp,
                                            color = ElectricSlateTokens.TextMuted
                                        )
                                    }
                                    BasicTextField(
                                        value = customHandleInput,
                                        onValueChange = { customHandleInput = it },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            fontFamily = ElmsSans,
                                            fontSize = 11.sp,
                                            color = ElectricSlateTokens.TextPrimary
                                        ),
                                        cursorBrush = SolidColor(ElectricSlateTokens.PrimaryAccent),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (customHandleInput.isNotBlank()) ElectricSlateTokens.PrimaryAccent
                                            else ElectricSlateTokens.TrackInactive
                                        )
                                        .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                                        .jigglePressClickable(enabled = customHandleInput.isNotBlank()) {
                                            val clean = customHandleInput.trim()
                                            if (isCustomPresetMode) {
                                                onAddAllowedUserHandle(clean)
                                            } else {
                                                onInviteUser(clean)
                                            }
                                            customHandleInput = ""
                                        }
                                        .padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = ElectricSlateTokens.TextPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = if (isCustomPresetMode) "Allow" else "Invite",
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = ElectricSlateTokens.TextPrimary
                                        )
                                    }
                                }
                            }

                            // Active Allowed / Invited Handles Chips
                            if (activeHandleSet.isNotEmpty()) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(activeHandleSet.toList(), key = { it }) { handle ->
                                        Row(
                                            modifier = Modifier
                                                .height(28.dp)
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(ElectricSlateTokens.SurfaceElevated)
                                                .border(1.dp, ElectricSlateTokens.PrimaryAccent, RoundedCornerShape(999.dp))
                                                .padding(horizontal = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = handle,
                                                fontFamily = ElmsSans,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 10.sp,
                                                color = ElectricSlateTokens.TextPrimary
                                            )
                                            Icon(
                                                imageVector = Icons.Outlined.Close,
                                                contentDescription = "Remove $handle",
                                                tint = ElectricSlateTokens.TextMuted,
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clickable {
                                                        if (isCustomPresetMode) {
                                                            onRemoveAllowedUserHandle(handle)
                                                        } else {
                                                            onRevokeInvitation(handle)
                                                        }
                                                    }
                                            )
                                        }
                                    }
                                }
                            }

                            // Select from Friend List
                            if (hostFriends.isNotEmpty()) {
                                Text(
                                    text = if (isCustomPresetMode) "Toggle from Friend List:" else "Quick Invite from Friend List:",
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    color = ElectricSlateTokens.TextMuted
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    for (friend in hostFriends) {
                                        val normHandle = CollabPrivacyPreset.normalizeHandle(friend.handle)
                                        val isIncluded = normHandle in activeHandleSet
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(ElectricSlateTokens.SurfaceElevated)
                                                .border(
                                                    1.dp,
                                                    if (isIncluded) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.BorderSubtle,
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .jigglePressClickable {
                                                    if (isCustomPresetMode) {
                                                        if (isIncluded) onRemoveAllowedUserHandle(normHandle)
                                                        else onAddAllowedUserHandle(normHandle)
                                                    } else {
                                                        if (isIncluded) onRevokeInvitation(normHandle)
                                                        else onInviteUser(normHandle)
                                                    }
                                                }
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = friend.displayName.ifBlank { friend.handle },
                                                    fontFamily = ElmsSans,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = ElectricSlateTokens.TextPrimary
                                                )
                                                Text(
                                                    text = normHandle,
                                                    fontFamily = ElmsSans,
                                                    fontSize = 10.sp,
                                                    color = ElectricSlateTokens.TextMuted
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(999.dp))
                                                    .background(
                                                        if (isIncluded) ElectricSlateTokens.PrimaryAccent
                                                        else ElectricSlateTokens.SurfacePanel
                                                    )
                                                    .border(
                                                        1.dp,
                                                        if (isIncluded) ElectricSlateTokens.PrimaryAccent
                                                        else ElectricSlateTokens.BorderSubtle,
                                                        RoundedCornerShape(999.dp)
                                                    )
                                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    if (isIncluded) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = ElectricSlateTokens.TextPrimary,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                    }
                                                    Text(
                                                        text = when {
                                                            isCustomPresetMode && isIncluded -> "Allowed"
                                                            isCustomPresetMode -> "Allow"
                                                            isIncluded -> "Invited"
                                                            else -> "Invite"
                                                        },
                                                        fontFamily = ElmsSans,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp,
                                                        color = ElectricSlateTokens.TextPrimary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // =========================================================================
                // 1. CANVAS & SHEET PERMISSIONS (GLOBAL LOCKS & ACCESS MODE)
                // =========================================================================
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(ElectricSlateTokens.BgCanvas)
                            .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = ElectricSlateTokens.PrimaryAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Canvas & Sheet Permissions",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                        }

                        // Access Mode Selector (Who retains access when restrictions are active)
                        Text(
                            text = if (isHost) {
                                "Who retains access when restrictions are active:"
                            } else {
                                "Current access policy when restricted:"
                            },
                            fontFamily = ElmsSans,
                            fontSize = 11.sp,
                            color = ElectricSlateTokens.TextMuted
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val isHostAndAdminSelected = accessMode == CollabAccessMode.HOST_AND_ADMIN
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isHostAndAdminSelected) ElectricSlateTokens.PrimaryAccent
                                        else ElectricSlateTokens.SurfaceElevated
                                    )
                                    .border(
                                        1.dp,
                                        if (isHostAndAdminSelected) ElectricSlateTokens.PrimaryAccent
                                        else ElectricSlateTokens.BorderSubtle,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .jigglePressClickable(enabled = isHost) {
                                        onSetAccessMode(CollabAccessMode.HOST_AND_ADMIN)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Only Host & Admins",
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (isHostAndAdminSelected) ElectricSlateTokens.TextPrimary
                                    else ElectricSlateTokens.TextMuted
                                )
                            }

                            val isHostOnlySelected = accessMode == CollabAccessMode.HOST_ONLY
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isHostOnlySelected) ElectricSlateTokens.PrimaryAccent
                                        else ElectricSlateTokens.SurfaceElevated
                                    )
                                    .border(
                                        1.dp,
                                        if (isHostOnlySelected) ElectricSlateTokens.PrimaryAccent
                                        else ElectricSlateTokens.BorderSubtle,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .jigglePressClickable(enabled = isHost) {
                                        onSetAccessMode(CollabAccessMode.HOST_ONLY)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Only Host",
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (isHostOnlySelected) ElectricSlateTokens.TextPrimary
                                    else ElectricSlateTokens.TextMuted
                                )
                            }
                        }

                        // Global Disable Drawing Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Disable Drawing (Global)",
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = ElectricSlateTokens.TextPrimary
                                )
                                Text(
                                    text = if (accessMode == CollabAccessMode.HOST_ONLY) {
                                        "Only Host can draw on canvas (auto-syncs when all users disabled)"
                                    } else {
                                        "Only Host & Admins can draw (auto-syncs when all users disabled)"
                                    },
                                    fontFamily = ElmsSans,
                                    fontSize = 10.sp,
                                    color = ElectricSlateTokens.TextMuted
                                )
                            }
                            Switch(
                                checked = isGlobalDrawingDisabled,
                                onCheckedChange = { onSetGlobalDrawingDisabled(it) },
                                enabled = canManageGlobalLocks,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = ElectricSlateTokens.TextPrimary,
                                    checkedTrackColor = ElectricSlateTokens.PrimaryAccent,
                                    uncheckedThumbColor = ElectricSlateTokens.TextMuted,
                                    uncheckedTrackColor = ElectricSlateTokens.SurfacePanel
                                )
                            )
                        }

                        // Global Disable Creating New Sheets Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Disable Creating New Sheets (Global)",
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = ElectricSlateTokens.TextPrimary
                                )
                                Text(
                                    text = if (accessMode == CollabAccessMode.HOST_ONLY) {
                                        "Only Host can add new sheets (auto-syncs when all users disabled)"
                                    } else {
                                        "Only Host & Admins can add sheets (auto-syncs when all users disabled)"
                                    },
                                    fontFamily = ElmsSans,
                                    fontSize = 10.sp,
                                    color = ElectricSlateTokens.TextMuted
                                )
                            }
                            Switch(
                                checked = isGlobalSheetCreationDisabled,
                                onCheckedChange = { onSetGlobalSheetCreationDisabled(it) },
                                enabled = canManageGlobalLocks,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = ElectricSlateTokens.TextPrimary,
                                    checkedTrackColor = ElectricSlateTokens.PrimaryAccent,
                                    uncheckedThumbColor = ElectricSlateTokens.TextMuted,
                                    uncheckedTrackColor = ElectricSlateTokens.SurfacePanel
                                )
                            )
                        }
                    }
                }

                // =========================================================================
                // 2. AUDIO BITRATE & BANDWIDTH QUALITY
                // =========================================================================
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(ElectricSlateTokens.BgCanvas)
                            .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Voice & Audio Bitrate",
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = ElectricSlateTokens.TextPrimary
                                )
                                Text(
                                    text = when {
                                        isRoomMutedByMod -> "Microphone muted by Host/Admin"
                                        isLocalMicMuted -> "Your microphone is muted"
                                        else -> "Your microphone is live in this room"
                                    },
                                    fontFamily = ElmsSans,
                                    fontSize = 11.sp,
                                    color = if (isRoomMutedByMod) ElectricSlateTokens.Destructive else ElectricSlateTokens.TextMuted
                                )
                            }

                            val effectiveMuted = isLocalMicMuted || isRoomMutedByMod
                            Row(
                                modifier = Modifier
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (!effectiveMuted) ElectricSlateTokens.PrimaryAccent
                                        else ElectricSlateTokens.SurfaceElevated
                                    )
                                    .border(
                                        1.dp,
                                        if (!effectiveMuted) ElectricSlateTokens.PrimaryAccent
                                        else ElectricSlateTokens.BorderSubtle,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .jigglePressClickable(enabled = !isRoomMutedByMod, onClick = onToggleLocalMicMute)
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (effectiveMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = if (effectiveMuted) "Unmute Mic" else "Mute Mic",
                                    tint = if (!effectiveMuted) ElectricSlateTokens.TextPrimary else ElectricSlateTokens.TextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (effectiveMuted) "Muted" else "Mic Live",
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (!effectiveMuted) ElectricSlateTokens.TextPrimary else ElectricSlateTokens.TextMuted
                                )
                            }
                        }

                        Text(
                            text = "My Outgoing Bitrate (Room cap: $roomMaxBitrateKbps kbps)",
                            fontFamily = ElmsSans,
                            fontSize = 11.sp,
                            color = ElectricSlateTokens.TextMuted
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (preset in CollabAudioBitrate.PRESETS) {
                                val isSelected = audioBitrateKbps == preset
                                val isAllowed = preset <= roomMaxBitrateKbps
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            when {
                                                isSelected -> ElectricSlateTokens.PrimaryAccent
                                                isAllowed -> ElectricSlateTokens.SurfaceElevated
                                                else -> ElectricSlateTokens.TrackInactive
                                            }
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) ElectricSlateTokens.PrimaryAccent
                                            else ElectricSlateTokens.BorderSubtle,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .jigglePressClickable(enabled = isAllowed) {
                                            onSetAudioBitrate(preset)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$preset kbps",
                                        fontFamily = ElmsSans,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.sp,
                                        color = if (isSelected || isAllowed) ElectricSlateTokens.TextPrimary
                                        else ElectricSlateTokens.TextMuted
                                    )
                                }
                            }
                        }

                        if (isHost) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Host Room Max Bitrate Cap (Protects mobile bandwidth)",
                                fontFamily = ElmsSans,
                                fontSize = 11.sp,
                                color = ElectricSlateTokens.TextMuted
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (preset in CollabAudioBitrate.PRESETS) {
                                    val isSelected = roomMaxBitrateKbps == preset
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(32.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) ElectricSlateTokens.PrimaryAccent
                                                else ElectricSlateTokens.SurfaceElevated
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) ElectricSlateTokens.PrimaryAccent
                                                else ElectricSlateTokens.BorderSubtle,
                                                RoundedCornerShape(12.dp)
                                            )
                                            .jigglePressClickable {
                                                onSetRoomMaxBitrate(preset)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Max $preset",
                                            fontFamily = ElmsSans,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 10.sp,
                                            color = ElectricSlateTokens.TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // =========================================================================
                // 3. PARTICIPANTS: PER-USER VOLUME & CLICK-TO-MANAGE ROLES / MODERATION
                // =========================================================================
                item {
                    Text(
                        text = "Participants (${peers.size})",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = ElectricSlateTokens.TextPrimary
                    )
                }

                items(peers, key = { it.peerId }) { peer ->
                    val isSelf = peer.peerId == localPeerId
                    val isExpanded = expandedPeerId == peer.peerId && !isSelf
                    val canManageThisPeer = !isSelf && (
                        isHost || (isAdmin && peer.role == CollabRole.MEMBER)
                    )
                    val peerColor = PeerCursorRenderer.parseColor(peer.colorHex)
                    val currentVol = peerVolumes[peer.peerId] ?: 1.0f
                    val isLocallyMuted = peer.peerId in locallyMutedPeerIds || currentVol == 0.0f
                    val displayPercent = if (isLocallyMuted) 0 else (currentVol * 100f).roundToInt()
                    val resolvedPeerLabel = remember(peer.userTag, peer.userHandle) {
                        val cleanHandle = peer.userHandle.trim().removePrefix("@")
                        val cleanTag = peer.userTag.trim()
                        when {
                            cleanTag.isNotBlank() &&
                                !cleanTag.equals("Fotara User", ignoreCase = true) &&
                                !cleanTag.equals("My Profile", ignoreCase = true) -> cleanTag
                            cleanHandle.isNotBlank() -> cleanHandle
                            cleanTag.isNotBlank() -> cleanTag
                            else -> "Member"
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(ElectricSlateTokens.BgCanvas)
                            .border(
                                width = if (isExpanded) 2.dp else 1.dp,
                                color = if (isExpanded) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.BorderSubtle,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable(enabled = !isSelf) {
                                expandedPeerId = if (expandedPeerId == peer.peerId) null else peer.peerId
                            }
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Top Row: Avatar + Real UserProfile Name + Role & Status Badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val initials = remember(resolvedPeerLabel) {
                                    val parts = resolvedPeerLabel.trim().split(WHITESPACE_REGEX).filter { it.isNotEmpty() }
                                    when {
                                        parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
                                        parts.size == 1 -> parts[0].take(2).uppercase()
                                        else -> "U"
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(ElectricSlateTokens.SurfaceElevated, CircleShape)
                                        .border(2.dp, peerColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = initials,
                                        fontFamily = ElmsSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = ElectricSlateTokens.TextPrimary
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = if (isSelf) "$resolvedPeerLabel (You)" else resolvedPeerLabel,
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = ElectricSlateTokens.TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(
                                                    if (peer.role == CollabRole.MEMBER) ElectricSlateTokens.SurfaceElevated
                                                    else ElectricSlateTokens.PrimaryAccent
                                                )
                                                .border(
                                                    1.dp,
                                                    if (peer.role == CollabRole.MEMBER) ElectricSlateTokens.BorderSubtle
                                                    else ElectricSlateTokens.PrimaryAccent,
                                                    RoundedCornerShape(999.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = peer.role.name,
                                                fontFamily = ElmsSans,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                color = ElectricSlateTokens.TextPrimary
                                            )
                                        }
                                    }

                                    // Status & Bitrate subtitle
                                    val statusParts = buildList {
                                        if (peer.pronouns.isNotBlank()) add(peer.pronouns)
                                        add("${peer.audioBitrateKbps} kbps")
                                        if (peer.isRoomMuted) add("Room Muted")
                                        if (peer.isChatBlocked) add("Chat Blocked")
                                        if (peer.isDrawingDisabled) add("No Draw")
                                        if (peer.isSheetCreationDisabled) add("No New Sheets")
                                    }
                                    Text(
                                        text = statusParts.joinToString(" • "),
                                        fontFamily = ElmsSans,
                                        fontSize = 11.sp,
                                        color = if (peer.isRoomMuted || peer.isChatBlocked || peer.isDrawingDisabled) {
                                            ElectricSlateTokens.Destructive
                                        } else {
                                            ElectricSlateTokens.TextMuted
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (!isSelf) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                                    tint = ElectricSlateTokens.TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Per-User Local Volume Slider (0% - 200%) for all remote participants
                        if (!isSelf) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isLocallyMuted) ElectricSlateTokens.Destructive
                                            else ElectricSlateTokens.SurfaceElevated
                                        )
                                        .border(1.dp, ElectricSlateTokens.BorderSubtle, CircleShape)
                                        .jigglePressClickable { onToggleLocalPeerMute(peer.peerId) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isLocallyMuted) Icons.AutoMirrored.Outlined.VolumeOff else Icons.AutoMirrored.Outlined.VolumeUp,
                                        contentDescription = if (isLocallyMuted) "Unmute User" else "Mute User",
                                        tint = ElectricSlateTokens.TextPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "User Volume",
                                            fontFamily = ElmsSans,
                                            fontSize = 11.sp,
                                            color = ElectricSlateTokens.TextMuted
                                        )
                                        Text(
                                            text = "$displayPercent%",
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = ElectricSlateTokens.TextPrimary
                                        )
                                    }
                                    AppSlider(
                                        value = if (isLocallyMuted) 0f else currentVol,
                                        onValueChange = { newVol ->
                                            onSetPeerVolume(peer.peerId, newVol)
                                        },
                                        valueRange = 0f..2f,
                                        steps = 3,
                                        activeTrackColor = ElectricSlateTokens.PrimaryAccent,
                                        inactiveTrackColor = ElectricSlateTokens.TrackInactive
                                    )
                                }
                            }
                        }

                        // Expanded User Management Drawer (when clicked by Host or Admin)
                        if (isExpanded && canManageThisPeer) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(ElectricSlateTokens.SurfaceElevated)
                                    .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(14.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Manage $resolvedPeerLabel",
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = ElectricSlateTokens.PrimaryAccent
                                )

                                // Per-user Disable Drawing & Disable Creating New Sheets switches
                                val canToggleThisPeerCanvas = isHost || (isAdmin && accessMode == CollabAccessMode.HOST_AND_ADMIN && peer.role == CollabRole.MEMBER)
                                if (canToggleThisPeerCanvas) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Disable Drawing for $resolvedPeerLabel",
                                            fontFamily = ElmsSans,
                                            fontSize = 12.sp,
                                            color = ElectricSlateTokens.TextPrimary
                                        )
                                        Switch(
                                            checked = peer.isDrawingDisabled,
                                            onCheckedChange = { onSetPeerDrawingDisabled(peer.peerId, it) },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = ElectricSlateTokens.TextPrimary,
                                                checkedTrackColor = ElectricSlateTokens.PrimaryAccent,
                                                uncheckedThumbColor = ElectricSlateTokens.TextMuted,
                                                uncheckedTrackColor = ElectricSlateTokens.SurfacePanel
                                            )
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Disable Creating New Sheets",
                                            fontFamily = ElmsSans,
                                            fontSize = 12.sp,
                                            color = ElectricSlateTokens.TextPrimary
                                        )
                                        Switch(
                                            checked = peer.isSheetCreationDisabled,
                                            onCheckedChange = { onSetPeerSheetCreationDisabled(peer.peerId, it) },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = ElectricSlateTokens.TextPrimary,
                                                checkedTrackColor = ElectricSlateTokens.PrimaryAccent,
                                                uncheckedThumbColor = ElectricSlateTokens.TextMuted,
                                                uncheckedTrackColor = ElectricSlateTokens.SurfacePanel
                                            )
                                        )
                                    }
                                }

                                // Host-Only Role Actions: Make/Remove Admin & Transfer Ownership
                                if (isHost) {
                                    val isPeerAdmin = peer.role == CollabRole.ADMIN
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(38.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    if (isPeerAdmin) ElectricSlateTokens.TrackInactive
                                                    else ElectricSlateTokens.PrimaryAccent
                                                )
                                                .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                                                .jigglePressClickable {
                                                    onSetPeerAdmin(peer.peerId, !isPeerAdmin)
                                                },
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Security,
                                                contentDescription = null,
                                                tint = ElectricSlateTokens.TextPrimary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isPeerAdmin) "Remove Admin" else "Make Admin",
                                                fontFamily = ElmsSans,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = ElectricSlateTokens.TextPrimary
                                            )
                                        }

                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(38.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(ElectricSlateTokens.SurfacePanel)
                                                .border(1.dp, ElectricSlateTokens.PrimaryAccent, RoundedCornerShape(12.dp))
                                                .jigglePressClickable {
                                                    onTransferHostOwnership(peer.peerId)
                                                    expandedPeerId = null
                                                },
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.SwapHoriz,
                                                contentDescription = null,
                                                tint = ElectricSlateTokens.PrimaryAccent,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Transfer Host",
                                                fontFamily = ElmsSans,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = ElectricSlateTokens.PrimaryAccent
                                            )
                                        }
                                    }
                                }

                                // Moderation Actions (Host & Admin): Room Mute & Block Chat
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (peer.isRoomMuted) ElectricSlateTokens.PrimaryAccent
                                                else ElectricSlateTokens.SurfacePanel
                                            )
                                            .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                                            .jigglePressClickable {
                                                onSetPeerRoomMuted(peer.peerId, !peer.isRoomMuted)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (peer.isRoomMuted) "Unmute in Room" else "Mute in Room",
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp,
                                            color = ElectricSlateTokens.TextPrimary
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (peer.isChatBlocked) ElectricSlateTokens.PrimaryAccent
                                                else ElectricSlateTokens.SurfacePanel
                                            )
                                            .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                                            .jigglePressClickable {
                                                onSetPeerChatBlocked(peer.peerId, !peer.isChatBlocked)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (peer.isChatBlocked) "Unblock Chat" else "Block Chat",
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp,
                                            color = ElectricSlateTokens.TextPrimary
                                        )
                                    }
                                }

                                // Host-Only Destructive Action: Kick Member (Cannot Join Again)
                                if (isHost) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(ElectricSlateTokens.Destructive)
                                            .jigglePressClickable {
                                                onKickPeer(peer.peerId)
                                                expandedPeerId = null
                                            },
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.PersonRemove,
                                            contentDescription = null,
                                            tint = ElectricSlateTokens.TextPrimary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Kick Member (Cannot Join Again)",
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = ElectricSlateTokens.TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
