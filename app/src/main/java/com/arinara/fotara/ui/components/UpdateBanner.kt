// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.theme.MidnightCardOutline
import com.arinara.fotara.theme.TagAmber
import com.arinara.fotara.theme.TagEmerald
import com.arinara.fotara.theme.TagViolet
import com.arinara.fotara.theme.TextPrimary
import com.arinara.fotara.theme.TextSecondary

/**
 * Small pill badge displaying release channel ("Stable" or "Beta").
 * Shared across UpdateBanner, WhatsNewScreen, and Settings About card.
 */
@Composable
fun ChannelPill(
    channel: String,
    modifier: Modifier = Modifier
) {
    val isAlpha = channel.equals("Alpha", ignoreCase = true)
    val isBeta = channel.equals("Beta", ignoreCase = true)
    val accentColor = when {
        isAlpha -> TagViolet
        isBeta -> TagAmber
        else -> TagEmerald
    }
    val label = when {
        isAlpha -> "Alpha"
        isBeta -> "Beta"
        else -> "Stable"
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(accentColor.copy(alpha = 0.18f))
            .border(0.8.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = accentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = ElmsSans
        )
    }
}

/**
 * Built-in, fully offline update banner composable:
 * - Subtle gradient background built from theme tokens (midnight & navy).
 * - Left: large ElmsSans Bold version text, tappable readable release link below it.
 * - Right: ChannelPill ("Stable" or "Beta").
 * - Optional top-right close 'X' button with clearance that never collides with the pill.
 */
@Composable
fun UpdateBanner(
    version: String,
    releaseUrl: String?,
    channel: String,
    bannerImageUrl: String? = null,
    onClose: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(16.dp),
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    val displayVersion = remember(version) {
        version
            .replace("Fotara", "", ignoreCase = true)
            .replace("Beta", "", ignoreCase = true)
            .trim('_', '-', ' ', 'v', 'V')
            .ifBlank { "1.6.0" }
    }

    val fullUrl = remember(releaseUrl, version) {
        if (!releaseUrl.isNullOrBlank()) {
            releaseUrl
        } else {
            "https://github.com/Arinaranetwork/Fotara/releases/tag/v${displayVersion}"
        }
    }

    val readableUrl = remember(fullUrl) {
        fullUrl.removePrefix("https://").removePrefix("http://").trimEnd('/')
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF131D38),
                        Color(0xFF0F172A),
                        Color(0xFF0C1326)
                    )
                )
            )
            .border(BorderStroke(1.dp, MidnightCardOutline), shape)
            .semantics { contentDescription = "Fotara update banner" }
    ) {
        // Banner background image behind dark gradient scrim
        if (!bannerImageUrl.isNullOrBlank() && com.arinara.fotara.ui.markdown.ReleaseNotesImageValidator.isAllowedImageUrl(bannerImageUrl)) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(bannerImageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.60f),
                                Color.Black.copy(alpha = 0.88f)
                            )
                        )
                    )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Column: Version and Release Link
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(end = 12.dp)
            ) {
                Text(
                    text = displayVersion,
                    style = TextStyle(
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = TextPrimary
                    ),
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = readableUrl,
                    style = TextStyle(
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textDecoration = TextDecoration.Underline
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable {
                        try {
                            uriHandler.openUri(fullUrl)
                        } catch (_: Exception) {
                            Toast.makeText(context, "Could not open release page", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            // Right Column: Close button (if provided) and Channel Pill
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                if (onClose != null) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.35f))
                            .clickable(onClick = onClose),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                ChannelPill(channel = channel)
            }
        }
    }
}
