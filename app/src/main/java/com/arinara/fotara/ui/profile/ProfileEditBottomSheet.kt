// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Panorama
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.R
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.theme.HomeCardBorder
import com.arinara.fotara.theme.HomeCardSurface
import com.arinara.fotara.theme.TagCrimson

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditBottomSheet(
    hasCustomAvatar: Boolean,
    hasCustomBanner: Boolean,
    onChangePicture: () -> Unit,
    onChooseBorder: () -> Unit,
    onChangeBanner: () -> Unit,
    onEditNameEmail: () -> Unit,
    onRemovePicture: () -> Unit,
    onRemoveBanner: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = HomeCardSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            ProfileSheetActionRow(
                icon = Icons.Default.Image,
                title = stringResource(R.string.profile_action_change_picture),
                onClick = {
                    onDismiss()
                    onChangePicture()
                }
            )

            ProfileSheetActionRow(
                icon = Icons.Default.Panorama,
                title = stringResource(R.string.profile_action_change_banner),
                onClick = {
                    onDismiss()
                    onChangeBanner()
                }
            )

            ProfileSheetActionRow(
                icon = Icons.Default.Edit,
                title = stringResource(R.string.profile_action_edit_name_email),
                onClick = {
                    onDismiss()
                    onEditNameEmail()
                }
            )

            if (hasCustomAvatar || hasCustomBanner) {
                HorizontalDivider(
                    color = HomeCardBorder.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (hasCustomAvatar) {
                ProfileSheetActionRow(
                    icon = Icons.Default.Delete,
                    title = stringResource(R.string.profile_action_remove_picture),
                    titleColor = TagCrimson,
                    iconTint = TagCrimson,
                    onClick = {
                        onDismiss()
                        onRemovePicture()
                    }
                )
            }

            if (hasCustomBanner) {
                ProfileSheetActionRow(
                    icon = Icons.Default.Delete,
                    title = stringResource(R.string.profile_action_remove_banner),
                    titleColor = TagCrimson,
                    iconTint = TagCrimson,
                    onClick = {
                        onDismiss()
                        onRemoveBanner()
                    }
                )
            }

            HorizontalDivider(
                color = HomeCardBorder.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Choose Border (Disabled - placed at most bottom, gray, under construction)
            ProfileSheetActionRow(
                icon = Icons.Default.AutoAwesome,
                title = stringResource(R.string.profile_action_choose_border),
                titleColor = Color.Gray,
                iconTint = Color.Gray,
                onClick = {
                    onDismiss()
                    onChooseBorder()
                }
            )
        }
    }
}

@Composable
private fun ProfileSheetActionRow(
    icon: ImageVector,
    title: String,
    titleColor: Color = Color.White,
    iconTint: Color = Color.White.copy(alpha = 0.85f),
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = title,
            color = titleColor,
            fontSize = 15.sp,
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Medium
        )
    }
}
