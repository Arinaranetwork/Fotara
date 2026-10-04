// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.arinara.fotara.R
import com.arinara.fotara.online.ReleaseInfo
import com.arinara.fotara.theme.ElmsSans

/**
 * Centered New Update modal dialog matching Screenshot 2:
 * - Large rounded corners (~28dp), dark surface, subtle border, dimmed backdrop.
 * - Top: Landscape banner illustration filling dialog width with rounded top corners.
 * - Top-right: Translucent circular 'X' close button overlaid on image.
 * - Overlaid title: "New Update {version}" in bold white with dark shadow outline.
 * - Dark body: Scrollable release notes / "What's new" bullets.
 * - Bottom: Two equal-width tonal pill buttons ("Later" and "Skip this version").
 */
@Composable
fun NewUpdateDialog(
    release: ReleaseInfo,
    onLater: () -> Unit,
    onSkipVersion: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cleanVersion = remember(release.version) {
        release.version
            .replace("Fotara", "", ignoreCase = true)
            .trim('_', '-', ' ', 'v', 'V')
            .split("-", "_", " ")[0]
    }

    Dialog(
        onDismissRequest = onLater,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onLater
                ),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F131D)),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, Color(0xFF26324A)),
                modifier = modifier
                    .fillMaxWidth(0.88f)
                    .clip(RoundedCornerShape(28.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Consume click to prevent dismissing
                    )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Top: Banner Illustration Area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(195.dp)
                    ) {
                        // Banner image (remote URL with cached Coil image or bundled local fallback)
                        val bannerModel = release.bannerUrl?.takeIf { it.isNotBlank() }
                            ?: R.drawable.fotara_banner_1_5
                        AsyncImage(
                            model = bannerModel,
                            contentDescription = "Update Banner",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        )

                        // Top-right close "X" circular button
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f))
                                .clickable(onClick = onLater),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.close),
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Gradient shadow at bottom of banner for text legibility
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(65.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.85f)
                                        )
                                    )
                                )
                        )

                        // Title: "New Update {version}" overlaid at bottom of banner
                        Text(
                            text = stringResource(R.string.update_popup_title, cleanVersion),
                            color = Color.White,
                            style = TextStyle(
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = ElmsSans,
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.95f),
                                    offset = Offset(2f, 2f),
                                    blurRadius = 8f
                                )
                            ),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp)
                        )
                    }

                    // Body: Release notes / What's new bullet points
                    val parsedBullets = remember(release.releaseNotes) {
                        parseReleaseBullets(release.releaseNotes)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp, vertical = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 210.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (parsedBullets.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.update_popup_default_notes),
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 13.5.sp,
                                    fontFamily = ElmsSans,
                                    lineHeight = 20.sp
                                )
                            } else {
                                parsedBullets.forEach { bullet ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = "•",
                                            color = Color(0xFF60A5FA),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = bullet,
                                            color = Color.White.copy(alpha = 0.88f),
                                            fontSize = 13.5.sp,
                                            fontFamily = ElmsSans,
                                            lineHeight = 19.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Bottom: Two equal-width tonal pill buttons side by side
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // "Later" tonal pill button
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = Color(0xFF383F4D),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clickable(onClick = onLater)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.update_action_later),
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontFamily = ElmsSans,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // "Skip this version" tonal pill button
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = Color(0xFF383F4D),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clickable(onClick = onSkipVersion)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.update_action_skip),
                                        color = Color.White,
                                        fontSize = 13.5.sp,
                                        fontFamily = ElmsSans,
                                        fontWeight = FontWeight.Medium
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

/**
 * Extracts clean user-facing bullet items from release notes markdown or text.
 */
private fun parseReleaseBullets(rawNotes: String?): List<String> {
    if (rawNotes.isNullOrBlank()) return emptyList()
    val lines = rawNotes.lines()
    val bullets = mutableListOf<String>()

    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.startsWith("•") || trimmed.startsWith("-") || trimmed.startsWith("*")) {
            val content = trimmed.removePrefix("•").removePrefix("-").removePrefix("*").trim()
            if (content.isNotBlank() && !content.startsWith("#")) {
                bullets.add(content)
            }
        } else if (trimmed.isNotBlank() && !trimmed.startsWith("#") && !trimmed.startsWith("==")) {
            bullets.add(trimmed)
        }
    }
    return bullets.take(8)
}
