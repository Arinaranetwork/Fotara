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

import androidx.compose.animation.core.LinearEasing
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.graphics.Color
import kotlin.math.sin

private val AccentGold = Color(0xFFFBBF24)

@Composable
fun AudioRecordPill(
    recorderState: AudioRecorderState,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onPauseRecording: (() -> Unit)? = null,
    onResumeRecording: (() -> Unit)? = null,
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
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pulsing Red Recording Indicator or amber when paused
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .alpha(if (recorderState.isPaused) 0.6f else pulseAlpha)
                            .clip(CircleShape)
                            .background(if (recorderState.isPaused) AccentGold else TagCrimson)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // Live Waveform Visualizer
                    RecordingWaveformBars(
                        amplitude = recorderState.amplitude,
                        isPaused = recorderState.isPaused,
                        modifier = Modifier.padding(horizontal = 4.dp)
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

                    Spacer(modifier = Modifier.width(6.dp))

                    // Pause / Resume Toggle
                    if (recorderState.isPaused) {
                        IconButton(
                            onClick = { onResumeRecording?.invoke() },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Resume Recording",
                                tint = AccentGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { onPauseRecording?.invoke() },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Pause,
                                contentDescription = "Pause Recording",
                                tint = FolderTabCream,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Discard Button
                    IconButton(
                        onClick = onCancelRecording,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Cancel Recording",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Save / Stop Button
                    IconButton(
                        onClick = onStopRecording,
                        modifier = Modifier.size(34.dp)
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

@Composable
private fun RecordingWaveformBars(
    amplitude: Int,
    isPaused: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    // Normalize amplitude between 0f and 1f (max amplitude typically up to 32767)
    val normAmp = (amplitude.toFloat() / 24000f).coerceIn(0f, 1f)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val barCount = 5
        for (i in 0 until barCount) {
            val factor = if (isPaused) {
                0.2f
            } else {
                val wave = sin(phase + i * 1.1).toFloat() * 0.5f + 0.5f
                0.2f + 0.8f * (normAmp * 0.7f + wave * 0.3f)
            }
            val barHeight = (4.dp + 16.dp * factor)
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(if (isPaused) TextMuted else FolderTabCream)
            )
        }
    }
}
