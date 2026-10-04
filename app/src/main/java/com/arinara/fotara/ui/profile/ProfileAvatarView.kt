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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.arinara.fotara.R
import com.arinara.fotara.data.model.ProfileBorders
import java.io.File

object ProfileAvatarDefaults {
    val DefaultAvatarSize: Dp = 78.dp
    val PickerAvatarSize: Dp = 52.dp
    val FixedRingAllowance: Dp = 0.dp
    const val InnerOverlapFraction: Float = 0.015f // 1.5% overlap
    val EditButtonVisibleSize: Dp = 32.dp
    val EditButtonTouchSize: Dp = 44.dp
    val EditButtonIconSize: Dp = 16.dp
    const val CosSin45: Float = 0.7071068f
}

/**
 * Standardized profile avatar with normalized decorative border and 45-degree edit button.
 * - The avatar is clipped to a circle of fixed diameter D, invariant to border selection.
 * - The border is scaled so its inner circle equals D with a ~1.5% anti-aliased overlap seam,
 *   and translated via draw-phase graphicsLayer so its inner circle center equals the avatar center.
 * - The edit button is anchored relative to the avatar circle at 45 degrees, never moving on border changes.
 */
@Composable
fun ProfileAvatar(
    avatarPath: String?,
    borderId: String?,
    modifier: Modifier = Modifier,
    avatarSize: Dp = ProfileAvatarDefaults.DefaultAvatarSize,
    avatarUpdatedAt: Long = 0L,
    showEditButton: Boolean = false,
    onEditClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val border = remember(borderId) { ProfileBorders.getById(borderId) }
    val slotSize = avatarSize + (ProfileAvatarDefaults.FixedRingAllowance * 2)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(slotSize)
    ) {
        // 1. Avatar Circle (Fixed diameter D, identical position for all borders)
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

        // 2. Overlaid Decorative Border (Normalized to avatar center with 1.5% overlap)
        if (border.drawableRes != 0) {
            val overlap = ProfileAvatarDefaults.InnerOverlapFraction
            val scaleWidth = (avatarSize * (1f - overlap)) / border.innerDiameterRatio
            val scaleHeight = scaleWidth * (border.canvasHeight.toFloat() / border.canvasWidth.toFloat())

            // Center of inner hole relative to image center
            val shiftX = scaleWidth * -(border.innerCenterX - 0.5f)
            val shiftY = scaleHeight * -(border.innerCenterY - 0.5f)

            Image(
                painter = painterResource(border.drawableRes),
                contentDescription = border.displayName,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .size(width = scaleWidth, height = scaleHeight)
                    .align(Alignment.Center)
                    .graphicsLayer {
                        clip = false
                        translationX = shiftX.toPx()
                        translationY = shiftY.toPx()
                    }
            )
        }

        // 3. Circular Edit Pen Button (Anchored to avatar circle at 45 degrees)
        if (showEditButton) {
            val radius = avatarSize / 2f
            val offset45 = radius * ProfileAvatarDefaults.CosSin45

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = offset45, y = offset45)
                    .size(ProfileAvatarDefaults.EditButtonTouchSize) // 44dp touch target
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 22.dp),
                        onClick = onEditClick
                    )
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(ProfileAvatarDefaults.EditButtonVisibleSize) // 32dp visible circle
                        .clip(CircleShape)
                        .background(Color(0xFF131925))
                        .border(1.5.dp, Color(0xFF3B82F6), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.cd_edit_profile),
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(ProfileAvatarDefaults.EditButtonIconSize)
                    )
                }
            }
        }
    }
}

/**
 * Backward-compatible wrapper delegating directly to [ProfileAvatar].
 */
@Composable
fun ProfileAvatarView(
    avatarPath: String?,
    borderId: String?,
    modifier: Modifier = Modifier,
    avatarSize: Dp = ProfileAvatarDefaults.DefaultAvatarSize,
    avatarUpdatedAt: Long = 0L,
    showEditButton: Boolean = false,
    onEditClick: () -> Unit = {}
) {
    ProfileAvatar(
        avatarPath = avatarPath,
        borderId = borderId,
        modifier = modifier,
        avatarSize = avatarSize,
        avatarUpdatedAt = avatarUpdatedAt,
        showEditButton = showEditButton,
        onEditClick = onEditClick
    )
}
