// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.folder

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.data.model.TextNote
import com.arinara.fotara.ui.components.GlowAnchor
import com.arinara.fotara.ui.components.GlowCorner
import com.arinara.fotara.ui.components.linkItCornerGlow
import com.arinara.fotara.ui.components.linkItGlow
import com.arinara.fotara.ui.components.toGlowAnchor
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DetailTextNoteCard(
    noteItem: FolderGridItem.TextNoteItem,
    isBatchMode: Boolean,
    isSelected: Boolean,
    isHighlighted: Boolean = false,
    highlightAlpha: Float = 0f,
    glowCorner: GlowCorner? = null,
    glowAnchors: Set<GlowAnchor> = emptySet(),
    onCardClick: () -> Unit,
    onCardLongClick: () -> Unit
) {
    val note = noteItem.textNote
    val tagColor = note.tagColor?.let {
        try { Color(android.graphics.Color.parseColor(it)) } catch (_: Exception) { null }
    }

    val effectiveAnchors = if (glowAnchors.isNotEmpty()) {
        glowAnchors
    } else if (glowCorner != null) {
        setOf(glowCorner.toGlowAnchor())
    } else {
        emptySet()
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(14.dp))
            .linkItGlow(
                isLinked = noteItem.isLinked && effectiveAnchors.isNotEmpty(),
                anchors = effectiveAnchors,
                linkedDescription = "Linked note ${note.title}",
                cornerRadiusDp = 14f
            )
            .combinedClickable(
                onClick = onCardClick,
                onLongClick = onCardLongClick
            ),
        shape = RoundedCornerShape(14.dp),
        color = ElectricSlateTokens.SurfacePanel,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = when {
                isSelected -> ElectricSlateTokens.PrimaryAccent
                isHighlighted -> ElectricSlateTokens.PrimaryAccent.copy(alpha = highlightAlpha)
                tagColor != null -> tagColor.copy(alpha = 0.6f)
                else -> ElectricSlateTokens.BorderSubtle
            }
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (note.isPinned) {
                Surface(
                    shape = CircleShape,
                    color = ElectricSlateTokens.TrackInactive,
                    modifier = Modifier
                        .align(if (isBatchMode) Alignment.TopCenter else Alignment.TopStart)
                        .padding(8.dp)
                        .size(22.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pinned note",
                        tint = ElectricSlateTokens.PrimaryAccent,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // Top header row: Badge + Tag color dot
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = ElectricSlateTokens.PrimaryAccent,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "NOTE",
                                color = ElectricSlateTokens.PrimaryAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Tag color dot
                    if (tagColor != null) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(tagColor)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Title
                Text(
                    text = note.title,
                    color = ElectricSlateTokens.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Plain-text snippet preview
                Text(
                    text = note.getPlainTextSnippet(90).ifBlank { "Empty note…" },
                    color = ElectricSlateTokens.TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Bottom timestamp & deadline
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val formattedDate = SimpleDateFormat("MMM d", Locale.US).format(Date(note.addedAt))
                    Text(
                        text = formattedDate,
                        color = ElectricSlateTokens.TextMuted,
                        fontSize = 10.sp
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    if (note.scheduledAt != null) {
                        com.arinara.fotara.ui.components.CardScheduleBadge(
                            scheduledAt = note.scheduledAt,
                            alertType = note.alertType
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    if (note.linkedDeadline != null) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = "Deadline",
                            tint = ElectricSlateTokens.PrimaryAccent,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // Batch selection overlay checkmark
            if (isBatchMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (isSelected) ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.12f) else Color.Transparent)
                        .padding(8.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.BgCanvas.copy(alpha = 0.7f))
                            .border(1.dp, ElectricSlateTokens.PrimaryAccent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = ElectricSlateTokens.TextPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Exposure/brightness highlight overlay for search navigation
            if (isHighlighted && highlightAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = highlightAlpha))
                )
            }
        }
    }
}
