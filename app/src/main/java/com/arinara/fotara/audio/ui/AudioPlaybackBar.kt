// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.audio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.audio.model.AudioAnnotation
import com.arinara.fotara.audio.player.AudioPlayerManager
import com.arinara.fotara.audio.player.AudioPlayerState
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.theme.FolderTabCream
import com.arinara.fotara.theme.HomeCardBorder
import com.arinara.fotara.theme.HomeCardSurface
import com.arinara.fotara.theme.TagAmber
import com.arinara.fotara.theme.TagCrimson
import com.arinara.fotara.theme.TextMuted
import com.arinara.fotara.theme.TextPrimary
import com.arinara.fotara.theme.TextSecondary

@Composable
fun AudioPlaybackBar(
    annotation: AudioAnnotation,
    playerState: AudioPlayerState,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSeek: (Long) -> Unit,
    onDelete: () -> Unit,
    onPageAnchorClick: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isCurrentTrack = when (playerState) {
        is AudioPlayerState.Playing -> playerState.filePath == annotation.filePath
        is AudioPlayerState.Paused -> playerState.filePath == annotation.filePath
        is AudioPlayerState.Completed -> playerState.filePath == annotation.filePath
        else -> false
    }

    val isPlaying = playerState is AudioPlayerState.Playing && isCurrentTrack
    val isPaused = playerState is AudioPlayerState.Paused && isCurrentTrack

    val currentPositionMs = when (playerState) {
        is AudioPlayerState.Playing -> if (isCurrentTrack) playerState.currentPositionMs else 0L
        is AudioPlayerState.Paused -> if (isCurrentTrack) playerState.currentPositionMs else 0L
        else -> 0L
    }

    val totalDurationMs = when {
        isCurrentTrack && playerState is AudioPlayerState.Playing -> playerState.totalDurationMs
        isCurrentTrack && playerState is AudioPlayerState.Paused -> playerState.totalDurationMs
        else -> annotation.durationMs
    }.coerceAtLeast(1L)

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, HomeCardBorder, RoundedCornerShape(12.dp)),
        color = HomeCardSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play / Pause Button
            IconButton(
                onClick = {
                    when {
                        isPlaying -> onPause()
                        isPaused -> onResume()
                        else -> onPlay()
                    }
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = FolderTabCream,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Elapsed / Total Duration Text
            val elapsedText = AudioPlayerManager.formatDuration(currentPositionMs)
            val totalText = AudioPlayerManager.formatDuration(totalDurationMs)
            Text(
                text = "$elapsedText / $totalText",
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                color = TextSecondary,
                modifier = Modifier.width(76.dp)
            )

            // Scrubber Slider
            var isUserScrubbing by remember { mutableStateOf(false) }
            var scrubPositionRatio by remember { mutableFloatStateOf(0f) }

            val sliderRatio = if (isUserScrubbing) {
                scrubPositionRatio
            } else {
                (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
            }

            Slider(
                value = sliderRatio,
                onValueChange = { ratio ->
                    isUserScrubbing = true
                    scrubPositionRatio = ratio
                },
                onValueChangeFinished = {
                    isUserScrubbing = false
                    val targetMs = (scrubPositionRatio * totalDurationMs).toLong()
                    onSeek(targetMs)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(24.dp)
                    .padding(horizontal = 4.dp),
                colors = SliderDefaults.colors(
                    thumbColor = FolderTabCream,
                    activeTrackColor = FolderTabCream,
                    inactiveTrackColor = HomeCardBorder
                )
            )

            // Optional PDF Page Anchor Chip
            if (annotation.pdfPageIndex != null) {
                val pageNum = annotation.pdfPageIndex + 1
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(HomeCardBorder)
                        .clickable(enabled = onPageAnchorClick != null) {
                            onPageAnchorClick?.invoke(annotation.pdfPageIndex)
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Page $pageNum",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = TagAmber
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            // Delete Button
            IconButton(
                onClick = { showDeleteConfirmDialog = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Delete Voice Note",
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "Delete Audio Note",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete this audio recording?",
                    fontFamily = ElmsSans,
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete()
                    }
                ) {
                    Text(
                        text = "Delete",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold,
                        color = TagCrimson
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(
                        text = "Cancel",
                        fontFamily = ElmsSans,
                        color = TextMuted
                    )
                }
            },
            containerColor = HomeCardSurface
        )
    }
}
