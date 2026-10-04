// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.arinara.fotara.data.model.ProfileBorders
import java.io.File

/**
 * Centered profile avatar with an overlaid decorative border and optional overlapping edit button.
 * The border image is sized and offset so its inner transparent circle matches the avatar circle
 * with zero gap and zero overlap.
 */
@Composable
fun ProfileAvatarView(
    avatarPath: String?,
    borderId: String?,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 78.dp,
    avatarUpdatedAt: Long = 0L,
    showEditButton: Boolean = false,
    onEditClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val border = remember(borderId) { ProfileBorders.getById(borderId) }

    // Outer container accommodates both the avatar and decorative border extensions
    val borderSize = if (border.drawableRes != 0) {
        avatarSize / border.innerRatio
    } else {
        avatarSize
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(maxOf(avatarSize, borderSize))
    ) {
        // 1. Avatar Circle
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(avatarSize)
                .clip(CircleShape)
                .background(Color(0xFF1B2234))
        ) {
            val file = remember(avatarPath, avatarUpdatedAt) { avatarPath?.let { File(it) } }
            if (file != null && file.exists()) {
                val cacheKey = "${file.absolutePath}_${if (avatarUpdatedAt > 0L) avatarUpdatedAt else file.lastModified()}"
                val imageRequest = remember(cacheKey) {
                    ImageRequest.Builder(context)
                        .data(file)
                        .memoryCacheKey(cacheKey)
                        .diskCacheKey(cacheKey)
                        .crossfade(true)
                        .build()
                }
                AsyncImage(
                    model = imageRequest,
                    contentDescription = "Profile Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Default Avatar",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(avatarSize * 0.55f)
                )
            }
        }

        // 2. Overlaid Decorative Border (Centered with inner-circle precision)
        if (border.drawableRes != 0) {
            val offsetX = (borderSize * -border.centerOffsetX)
            val offsetY = (borderSize * -border.centerOffsetY)

            Image(
                painter = painterResource(border.drawableRes),
                contentDescription = border.displayName,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(borderSize)
                    .offset(x = offsetX, y = offsetY)
            )
        }

        // 3. Circular Edit Pen Button (Overlapping bottom-right of avatar)
        if (showEditButton) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-2).dp, y = (-2).dp)
                    .size(44.dp) // Accessible touch target
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 22.dp),
                        onClick = onEditClick
                    )
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF131925))
                        .border(1.5.dp, Color(0xFF3B82F6), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}
