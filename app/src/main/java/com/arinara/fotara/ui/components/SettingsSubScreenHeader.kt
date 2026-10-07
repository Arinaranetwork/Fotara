// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.R
import com.arinara.fotara.theme.ElmsSans

/**
 * Standardized constants for Settings sub-screen headers, derived from GeneralSettings.
 */
object SettingsSubScreenHeaderDefaults {
    val ButtonSize: Dp = 40.dp
    val IconSize: Dp = 20.dp
    val ButtonCornerRadius: Dp = 20.dp
    val ButtonBackgroundColor: Color = Color(0xFF131925)
    val ButtonIconTint: Color = Color.White
    val TitleSpacing: Dp = 12.dp
    val TitleFontSize: TextUnit = 24.sp
    val TitleFontWeight: FontWeight = FontWeight.Medium
    val HorizontalPadding: Dp = 18.dp
    val TopPadding: Dp = 8.dp
    val BottomPadding: Dp = 6.dp
}

/**
 * Unified header component used across all Settings sub-screens, Profile screen,
 * legal documents, and crop editor screens. Ensures identical title and back arrow
 * alignment and sizing at default and maximum font scales.
 */
@Composable
fun SettingsSubScreenHeader(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: ImageVector = Icons.AutoMirrored.Filled.ArrowBack,
    navigationContentDescription: String = stringResource(R.string.cd_back),
    actions: @Composable (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = SettingsSubScreenHeaderDefaults.TopPadding,
                bottom = SettingsSubScreenHeaderDefaults.BottomPadding
            )
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .size(SettingsSubScreenHeaderDefaults.ButtonSize)
                .clip(RoundedCornerShape(SettingsSubScreenHeaderDefaults.ButtonCornerRadius))
                .background(SettingsSubScreenHeaderDefaults.ButtonBackgroundColor)
        ) {
            Icon(
                imageVector = navigationIcon,
                contentDescription = navigationContentDescription,
                tint = SettingsSubScreenHeaderDefaults.ButtonIconTint,
                modifier = Modifier.size(SettingsSubScreenHeaderDefaults.IconSize)
            )
        }

        Spacer(modifier = Modifier.width(SettingsSubScreenHeaderDefaults.TitleSpacing))

        androidx.compose.foundation.layout.Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = SettingsSubScreenHeaderDefaults.TitleFontSize,
                fontFamily = ElmsSans,
                fontWeight = SettingsSubScreenHeaderDefaults.TitleFontWeight
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = com.arinara.fotara.theme.HomeSubtitleGray,
                    fontSize = 12.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (actions != null) {
            actions()
        }
    }
}
