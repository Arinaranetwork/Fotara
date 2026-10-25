// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.collab.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.feature.collab.model.CollabChatMessage
import com.arinara.fotara.feature.collab.model.CollabPeer
import com.arinara.fotara.feature.collab.model.CollabRole
import com.arinara.fotara.feature.collab.render.PeerCursorRenderer
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import com.arinara.fotara.ui.theme.jigglePressClickable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Top dock bar displaying the active collaborative room code, local role badge, participant halos
 * with real UserProfile initials, room chat launcher with unread badge, room settings launcher,
 * and exit action.
 */
@Composable
fun CollabSessionBar(
    roomCode: String,
    peers: List<CollabPeer>,
    onLeaveClick: () -> Unit,
    modifier: Modifier = Modifier,
    localRole: CollabRole = CollabRole.MEMBER,
    canLocalUserDraw: Boolean = true,
    isLocalMicMuted: Boolean = false,
    isRoomMutedByMod: Boolean = false,
    unreadChatCount: Int = 0,
    onToggleMicClick: (() -> Unit)? = null,
    onChatClick: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null,
    onCopyCode: (() -> Unit)? = null
) {
    val clipboardManager = LocalClipboardManager.current
    val barShape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(ElectricSlateTokens.SurfacePanel, barShape)
            .border(1.dp, ElectricSlateTokens.BorderSubtle, barShape)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Room status, code pill, and role/restriction badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(ElectricSlateTokens.PrimaryAccent, CircleShape)
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElectricSlateTokens.SurfaceElevated)
                        .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            clipboardManager.setText(AnnotatedString(roomCode))
                            onCopyCode?.invoke()
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = roomCode,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = ElectricSlateTokens.TextPrimary,
                        letterSpacing = 0.5.sp
                    )

                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "Copy Room Code",
                        tint = ElectricSlateTokens.TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }

                if (localRole == CollabRole.HOST || localRole == CollabRole.ADMIN) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(ElectricSlateTokens.PrimaryAccent)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = localRole.name,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = ElectricSlateTokens.TextPrimary
                        )
                    }
                } else if (!canLocalUserDraw) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(ElectricSlateTokens.SurfaceElevated)
                            .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(999.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "View Only",
                            tint = ElectricSlateTokens.TextMuted,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = "View Only",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp,
                            color = ElectricSlateTokens.TextMuted
                        )
                    }
                }
            }

            // Right: Participant Halos + Chat Button + Settings Button + Leave Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val displayedPeers = peers.take(3)
                val extraCount = peers.size - displayedPeers.size

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((-6).dp)
                ) {
                    for (peer in displayedPeers) {
                        val peerHaloColor = PeerCursorRenderer.parseColor(peer.colorHex)
                        val initials = remember(peer.userTag, peer.userHandle) {
                            val label = peer.userTag.trim()
                                .takeIf { it.isNotBlank() && !it.equals("Fotara User", ignoreCase = true) && !it.equals("My Profile", ignoreCase = true) }
                                ?: peer.userHandle.trim().removePrefix("@")
                            val parts = label.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
                            when {
                                parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
                                parts.size == 1 -> parts[0].take(2).uppercase()
                                else -> "U"
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(ElectricSlateTokens.BgCanvas, CircleShape)
                                .border(2.dp, peerHaloColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                        }
                    }

                    if (extraCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(ElectricSlateTokens.TrackInactive, CircleShape)
                                .border(1.dp, ElectricSlateTokens.BorderSubtle, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+$extraCount",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 10.sp,
                                color = ElectricSlateTokens.TextMuted
                            )
                        }
                    }
                }

                // Microphone Mute / Unmute Button
                if (onToggleMicClick != null) {
                    val effectiveMuted = isLocalMicMuted || isRoomMutedByMod
                    Box(
                        modifier = Modifier
                            .size(36.dp)
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
                            .jigglePressClickable(enabled = !isRoomMutedByMod, onClick = onToggleMicClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (effectiveMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = if (effectiveMuted) "Unmute Microphone" else "Mute Microphone",
                            tint = if (!effectiveMuted) ElectricSlateTokens.TextPrimary
                            else if (isRoomMutedByMod) ElectricSlateTokens.Destructive
                            else ElectricSlateTokens.TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Chat Button with Unread Badge
                if (onChatClick != null) {
                    Row(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElectricSlateTokens.SurfaceElevated)
                            .border(
                                1.dp,
                                if (unreadChatCount > 0) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.BorderSubtle,
                                RoundedCornerShape(12.dp)
                            )
                            .jigglePressClickable(onClick = onChatClick)
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Room Chat",
                            tint = if (unreadChatCount > 0) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Chat",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = ElectricSlateTokens.TextPrimary
                        )
                        if (unreadChatCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .background(ElectricSlateTokens.PrimaryAccent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (unreadChatCount > 9) "9+" else unreadChatCount.toString(),
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = ElectricSlateTokens.TextPrimary
                                )
                            }
                        }
                    }
                }

                // Room Settings Button
                if (onSettingsClick != null) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElectricSlateTokens.SurfaceElevated)
                            .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                            .jigglePressClickable(onClick = onSettingsClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Room Settings",
                            tint = ElectricSlateTokens.TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Leave Room Button
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElectricSlateTokens.TrackInactive)
                        .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                        .jigglePressClickable(onClick = onLeaveClick)
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Leave",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = ElectricSlateTokens.TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Bottom sheet for real-time collaborative room chat between study participants.
 * Features:
 * - Displays real [com.arinara.fotara.data.model.UserProfile] display names for both local and remote peers.
 * - Host-only Pinned Message (`onPinMessage`) with top pinned banner.
 * - Host & Admin message deletion (`onDeleteMessage`).
 * - Chat-blocked enforcement (`isLocalUserChatBlocked`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollabChatSheet(
    roomCode: String,
    peers: List<CollabPeer>,
    messages: List<CollabChatMessage>,
    onSendMessage: (String) -> Unit,
    onDismissRequest: () -> Unit,
    localUserName: String = "Fotara User",
    localRole: CollabRole = CollabRole.MEMBER,
    pinnedMessage: CollabChatMessage? = null,
    isLocalUserChatBlocked: Boolean = false,
    onPinMessage: ((String?) -> Unit)? = null,
    onDeleteMessage: ((String) -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draftText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val canPinMessages = localRole == CollabRole.HOST
    val canDeleteMessages = localRole == CollabRole.HOST || localRole == CollabRole.ADMIN

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

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
                .fillMaxHeight(0.78f)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Study Room Chat • $roomCode",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = ElectricSlateTokens.TextPrimary
                    )
                    Text(
                        text = "${peers.size} ${if (peers.size == 1) "participant" else "participants"} online • Chatting as $localUserName (${localRole.name})",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 11.sp,
                        color = ElectricSlateTokens.TextMuted
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onOpenSettings != null) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ElectricSlateTokens.SurfaceElevated)
                                .border(1.dp, ElectricSlateTokens.BorderSubtle, CircleShape)
                                .jigglePressClickable(onClick = onOpenSettings),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "Room Settings",
                                tint = ElectricSlateTokens.TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
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
                            contentDescription = "Close Chat",
                            tint = ElectricSlateTokens.TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Host-Pinned Message Banner
            if (pinnedMessage != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ElectricSlateTokens.SurfaceElevated)
                        .border(1.dp, ElectricSlateTokens.PrimaryAccent, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = "Pinned Message",
                            tint = ElectricSlateTokens.PrimaryAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text(
                                text = "Pinned by Host • ${pinnedMessage.senderTag}",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = ElectricSlateTokens.PrimaryAccent
                            )
                            Text(
                                text = pinnedMessage.text,
                                fontFamily = ElmsSans,
                                fontSize = 12.sp,
                                color = ElectricSlateTokens.TextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (canPinMessages && onPinMessage != null) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(ElectricSlateTokens.SurfacePanel)
                                .border(1.dp, ElectricSlateTokens.BorderSubtle, CircleShape)
                                .jigglePressClickable { onPinMessage(null) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Unpin Message",
                                tint = ElectricSlateTokens.TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Message List or Empty State
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ElectricSlateTokens.BgCanvas)
                        .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = null,
                            tint = ElectricSlateTokens.PrimaryAccent,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No messages yet",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = ElectricSlateTokens.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Send a message as $localUserName to coordinate with peers in $roomCode.",
                            fontFamily = ElmsSans,
                            fontSize = 12.sp,
                            color = ElectricSlateTokens.TextMuted
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ElectricSlateTokens.BgCanvas)
                        .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(16.dp)),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages, key = { it.messageId }) { msg ->
                        if (msg.isSystemNotice) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = msg.text,
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = ElectricSlateTokens.TextMuted,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(ElectricSlateTokens.SurfaceElevated)
                                        .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(999.dp))
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        } else {
                            val isSelf = msg.isLocal
                            val isPinned = pinnedMessage?.messageId == msg.messageId
                            val roleLabel = when (msg.senderRole) {
                                CollabRole.HOST -> " [HOST]"
                                CollabRole.ADMIN -> " [ADMIN]"
                                CollabRole.MEMBER -> ""
                            }
                            val pronounsLabel = if (msg.senderPronouns.isNotBlank()) " (${msg.senderPronouns})" else ""

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isSelf) Arrangement.End else Arrangement.Start
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth(0.84f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (isSelf) ElectricSlateTokens.PrimaryAccent
                                            else ElectricSlateTokens.SurfaceElevated
                                        )
                                        .border(
                                            width = if (isPinned) 2.dp else 1.dp,
                                            color = if (isSelf || isPinned) ElectricSlateTokens.PrimaryAccent
                                            else ElectricSlateTokens.BorderSubtle,
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = buildString {
                                                append(msg.senderTag)
                                                if (isSelf) append(" (You)")
                                                append(pronounsLabel)
                                                append(roleLabel)
                                            },
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (isSelf) ElectricSlateTokens.TextPrimary else ElectricSlateTokens.PrimaryAccent,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            // Host-Only Pin Action
                                            if (canPinMessages && onPinMessage != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(22.dp)
                                                        .clip(CircleShape)
                                                        .clickable {
                                                            onPinMessage(if (isPinned) null else msg.messageId)
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                                        contentDescription = if (isPinned) "Unpin Message" else "Pin Message",
                                                        tint = if (isSelf) ElectricSlateTokens.TextPrimary else ElectricSlateTokens.TextMuted,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }
                                            }

                                            // Host & Admin Delete Message Action
                                            if (canDeleteMessages && onDeleteMessage != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(22.dp)
                                                        .clip(CircleShape)
                                                        .clickable {
                                                            onDeleteMessage(msg.messageId)
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Delete,
                                                        contentDescription = "Delete Message",
                                                        tint = if (isSelf) ElectricSlateTokens.TextPrimary else ElectricSlateTokens.Destructive,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }
                                            }

                                            Text(
                                                text = timeFormatter.format(Date(msg.timestamp)),
                                                fontFamily = ElmsSans,
                                                fontSize = 10.sp,
                                                color = if (isSelf) ElectricSlateTokens.TextPrimary else ElectricSlateTokens.TextMuted
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = msg.text,
                                        fontFamily = ElmsSans,
                                        fontSize = 14.sp,
                                        color = ElectricSlateTokens.TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Input Row (or Chat-Blocked Banner)
            if (isLocalUserChatBlocked) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ElectricSlateTokens.SurfaceElevated)
                        .border(1.dp, ElectricSlateTokens.Destructive, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Your chat access has been blocked by a moderator.",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = ElectricSlateTokens.Destructive
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = draftText,
                        onValueChange = { draftText = it.take(500) },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                text = "Message as $localUserName...",
                                fontFamily = ElmsSans,
                                fontSize = 14.sp,
                                color = ElectricSlateTokens.TextMuted
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ElectricSlateTokens.SurfaceElevated,
                            unfocusedContainerColor = ElectricSlateTokens.SurfaceElevated,
                            focusedBorderColor = ElectricSlateTokens.PrimaryAccent,
                            unfocusedBorderColor = ElectricSlateTokens.BorderSubtle,
                            focusedTextColor = ElectricSlateTokens.TextPrimary,
                            unfocusedTextColor = ElectricSlateTokens.TextPrimary,
                            cursorColor = ElectricSlateTokens.PrimaryAccent
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                val trimmed = draftText.trim()
                                if (trimmed.isNotEmpty()) {
                                    onSendMessage(trimmed)
                                    draftText = ""
                                }
                            }
                        )
                    )

                    val canSend = draftText.trim().isNotEmpty()
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (canSend) ElectricSlateTokens.PrimaryAccent
                                else ElectricSlateTokens.TrackInactive
                            )
                            .border(
                                1.dp,
                                if (canSend) ElectricSlateTokens.PrimaryAccent
                                else ElectricSlateTokens.BorderSubtle,
                                CircleShape
                            )
                            .jigglePressClickable(enabled = canSend) {
                                val trimmed = draftText.trim()
                                if (trimmed.isNotEmpty()) {
                                    onSendMessage(trimmed)
                                    draftText = ""
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Message",
                            tint = if (canSend) ElectricSlateTokens.TextPrimary else ElectricSlateTokens.TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
