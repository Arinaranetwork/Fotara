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
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
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
import com.arinara.fotara.feature.collab.render.PeerCursorRenderer
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import com.arinara.fotara.ui.theme.jigglePressClickable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Top dock bar displaying the active collaborative room code, participant halos,
 * real-time room chat launcher with unread badge, and exit action.
 */
@Composable
fun CollabSessionBar(
    roomCode: String,
    peers: List<CollabPeer>,
    onLeaveClick: () -> Unit,
    modifier: Modifier = Modifier,
    unreadChatCount: Int = 0,
    onChatClick: (() -> Unit)? = null,
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
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Room status and code pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Online presence indicator
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(ElectricSlateTokens.PrimaryAccent, CircleShape)
                )

                // Room Code pill
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
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Right: Participant Halos + Chat Button + Leave Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Participant Avatars with Color Halos
                val displayedPeers = peers.take(3)
                val extraCount = peers.size - displayedPeers.size

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((-6).dp)
                ) {
                    for (peer in displayedPeers) {
                        val peerHaloColor = PeerCursorRenderer.parseColor(peer.colorHex)
                        val initial = peer.userTag.trim().take(1).uppercase().ifEmpty { "P" }

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(ElectricSlateTokens.BgCanvas, CircleShape)
                                .border(2.dp, peerHaloColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initial,
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
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
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
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

                // Leave Room Button (Muted style)
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElectricSlateTokens.TrackInactive)
                        .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                        .jigglePressClickable(onClick = onLeaveClick)
                        .padding(horizontal = 12.dp),
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
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollabChatSheet(
    roomCode: String,
    peers: List<CollabPeer>,
    messages: List<CollabChatMessage>,
    onSendMessage: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draftText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

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
                .fillMaxHeight(0.75f)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
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
                        text = "${peers.size} ${if (peers.size == 1) "participant" else "participants"} online",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
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
                        contentDescription = "Close Chat",
                        tint = ElectricSlateTokens.TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
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
                            text = "Send a message to coordinate notes and study topics with peers in $roomCode.",
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
                        val isSelf = msg.isLocal
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isSelf) Arrangement.End else Arrangement.Start
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth(0.82f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isSelf) ElectricSlateTokens.PrimaryAccent
                                        else ElectricSlateTokens.SurfaceElevated
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelf) ElectricSlateTokens.PrimaryAccent
                                        else ElectricSlateTokens.BorderSubtle,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isSelf) "${msg.senderTag} (You)" else msg.senderTag,
                                        fontFamily = ElmsSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (isSelf) ElectricSlateTokens.TextPrimary else ElectricSlateTokens.PrimaryAccent
                                    )
                                    Text(
                                        text = timeFormatter.format(Date(msg.timestamp)),
                                        fontFamily = ElmsSans,
                                        fontSize = 10.sp,
                                        color = if (isSelf) ElectricSlateTokens.TextPrimary else ElectricSlateTokens.TextMuted
                                    )
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

            Spacer(modifier = Modifier.height(12.dp))

            // Input Row
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
                            text = "Message study room...",
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
