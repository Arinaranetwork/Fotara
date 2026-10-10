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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.feature.collab.model.CollabPeer
import com.arinara.fotara.feature.collab.render.PeerCursorRenderer
import com.arinara.fotara.theme.ElmsSans

/**
 * Top dock bar displaying the active collaborative room code, participant halos, and exit action.
 */
@Composable
fun CollabSessionBar(
    roomCode: String,
    peers: List<CollabPeer>,
    onLeaveClick: () -> Unit,
    modifier: Modifier = Modifier,
    onCopyCode: (() -> Unit)? = null
) {
    val clipboardManager = LocalClipboardManager.current
    val barShape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(Color(0xFF111726), barShape)
            .border(1.dp, Color(0xFF161E30), barShape)
            .padding(horizontal = 16.dp, vertical = 8.dp)
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
                // Online presence pulse indicator
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFF2A9D8F), CircleShape)
                )

                // Room Code pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF141B2A))
                        .border(1.dp, Color(0xFF1C2538), RoundedCornerShape(8.dp))
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
                        color = Color(0xFFFFFFFF),
                        letterSpacing = 0.5.sp
                    )

                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "Copy Room Code",
                        tint = Color(0xFFA0A5C2),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Right: Participant Halos + Leave Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                                .background(Color(0xFF0A0D14), CircleShape)
                                .border(2.dp, peerHaloColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initial,
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color(0xFFFFFFFF)
                            )
                        }
                    }

                    if (extraCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(0xFF182236), CircleShape)
                                .border(1.5.dp, Color(0xFF2563EB), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+$extraCount",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 10.sp,
                                color = Color(0xFFFFFFFF)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Leave Room Button
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF182236))
                        .border(1.dp, Color(0xFF2563EB).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .clickable(onClick = onLeaveClick)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Leave",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = Color(0xFFEFE8DA),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
