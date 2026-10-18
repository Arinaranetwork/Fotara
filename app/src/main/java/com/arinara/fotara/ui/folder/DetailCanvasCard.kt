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
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.GridOn
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
import com.arinara.fotara.data.model.CanvasNote
import com.arinara.fotara.ui.components.CardScheduleBadge
import com.arinara.fotara.ui.components.GlowAnchor
import com.arinara.fotara.ui.components.GlowCorner
import com.arinara.fotara.ui.components.ScheduleBadge
import com.arinara.fotara.ui.components.linkItCornerGlow
import com.arinara.fotara.ui.components.linkItGlow
import com.arinara.fotara.ui.components.toGlowAnchor
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DetailCanvasCard(
    canvasItem: FolderGridItem.CanvasNoteItem,
    isBatchMode: Boolean,
    isSelected: Boolean,
    isHighlighted: Boolean = false,
    highlightAlpha: Float = 0f,
    glowCorner: GlowCorner? = null,
    glowAnchors: Set<GlowAnchor> = emptySet(),
    onCardClick: () -> Unit,
    onCardLongClick: () -> Unit
) {
    val note = canvasItem.canvasNote
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
                isLinked = canvasItem.isLinked && effectiveAnchors.isNotEmpty(),
                anchors = effectiveAnchors,
                linkedDescription = "Linked canvas ${note.title}",
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
                    .padding(10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header: Badge & Date
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Brush,
                                    contentDescription = null,
                                    tint = ElectricSlateTokens.PrimaryAccent,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "CANVAS",
                                    color = ElectricSlateTokens.PrimaryAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Real-Time Active Collaborator Presence Indicator
                        if (canvasItem.collaboratorCount > 0 || canvasItem.hasActiveCollaborator) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(ElectricSlateTokens.PrimaryAccent)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (canvasItem.collaboratorCount > 1) "${canvasItem.collaboratorCount} live" else "Live",
                                        color = ElectricSlateTokens.PrimaryAccent,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    val dateStr = SimpleDateFormat("MMM d", Locale.US).format(Date(note.addedAt))
                    Text(
                        text = dateStr,
                        color = ElectricSlateTokens.TextMuted,
                        fontSize = 11.sp
                    )
                }

                // Middle: Visual Graphic Canvas Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElectricSlateTokens.BgCanvas),
                    contentAlignment = Alignment.Center
                ) {
                    val thumbFile = note.thumbnailPath?.let { java.io.File(it) }
                    if (thumbFile != null && thumbFile.exists() && thumbFile.length() > 0) {
                        coil.compose.AsyncImage(
                            model = thumbFile,
                            contentDescription = "Canvas Preview",
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = null,
                            tint = ElectricSlateTokens.BorderSubtle,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Title & Footer
                Column {
                    Text(
                        text = note.title,
                        color = ElectricSlateTokens.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (note.scheduledAt != null) {
                            CardScheduleBadge(
                                scheduledAt = note.scheduledAt,
                                alertType = note.alertType
                            )
                        } else {
                            Text(
                                text = "Vector Drawing",
                                color = ElectricSlateTokens.TextMuted,
                                fontSize = 11.sp
                            )
                        }

                        if (tagColor != null) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(tagColor)
                            )
                        }
                    }
                }
            }

            // Selection Checkmark Overlay in Batch Mode
            if (isBatchMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.BgCanvas.copy(alpha = 0.7f))
                        .border(1.dp, ElectricSlateTokens.PrimaryAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = ElectricSlateTokens.TextPrimary,
                            modifier = Modifier.size(14.dp)
                        )
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
