// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.audio.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.audio.recorder.AudioRecorderManager
import com.arinara.fotara.audio.recorder.AudioRecorderState
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.theme.FolderTabCream
import com.arinara.fotara.theme.HomeCardBorder
import com.arinara.fotara.theme.HomeCardSurface
import com.arinara.fotara.theme.TagCrimson
import com.arinara.fotara.theme.TextMuted
import com.arinara.fotara.theme.TextPrimary

@Composable
fun AudioRecordPill(
    recorderState: AudioRecorderState,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, HomeCardBorder, RoundedCornerShape(24.dp)),
        color = HomeCardSurface
    ) {
        when (recorderState) {
            is AudioRecorderState.Idle, is AudioRecorderState.Finalized, is AudioRecorderState.Error -> {
                Row(
                    modifier = Modifier
                        .clickable(onClick = onStartRecording)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Mic,
                        contentDescription = "Record Audio",
                        tint = FolderTabCream,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Record Audio",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                }
            }

            is AudioRecorderState.Recording -> {
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseAlpha"
                )

                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pulsing Red Recording Indicator
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .alpha(pulseAlpha)
                            .clip(CircleShape)
                            .background(TagCrimson)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // Timer Counter
                    val elapsedText = AudioRecorderManager.formatDuration(recorderState.elapsedMs)
                    val maxText = AudioRecorderManager.formatDuration(AudioRecorderManager.MAX_RECORDING_DURATION_MS)
                    Text(
                        text = "$elapsedText / $maxText",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Discard Button
                    IconButton(
                        onClick = onCancelRecording,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Cancel Recording",
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Save / Stop Button
                    IconButton(
                        onClick = onStopRecording,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Done,
                            contentDescription = "Save Recording",
                            tint = FolderTabCream,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
