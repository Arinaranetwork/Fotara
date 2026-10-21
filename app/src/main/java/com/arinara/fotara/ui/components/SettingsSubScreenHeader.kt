// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.R
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import com.arinara.fotara.ui.theme.jiggleRotationClickable

/**
 * Standardized constants for Settings sub-screen headers, aligned with AppTopBar standards.
 */
object SettingsSubScreenHeaderDefaults {
    val ButtonSize: Dp = 48.dp
    val IconSize: Dp = 24.dp
    val ButtonCornerRadius: Dp = 24.dp
    val ButtonBackgroundColor: Color = Color.Transparent
    val ButtonIconTint: Color = ElectricSlateTokens.TextPrimary
    val TitleSpacing: Dp = 8.dp
    val TitleFontSize: TextUnit = 22.sp
    val TitleFontWeight: FontWeight = FontWeight.Bold
    val HorizontalPadding: Dp = 16.dp
    val TopPadding: Dp = 0.dp
    val BottomPadding: Dp = 0.dp
    val SubtitleFontSize: TextUnit = 14.sp
    val SubtitleFontWeight: FontWeight = FontWeight.Normal
    val TitleToSubtitleSpacing: Dp = 2.dp
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
            .height(56.dp)
            .padding(horizontal = SettingsSubScreenHeaderDefaults.HorizontalPadding)
    ) {
        Box(
            modifier = Modifier
                .size(SettingsSubScreenHeaderDefaults.ButtonSize)
                .jiggleRotationClickable(onClick = onBackClick),
            contentAlignment = Alignment.Center
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
                color = ElectricSlateTokens.TextPrimary,
                fontSize = SettingsSubScreenHeaderDefaults.TitleFontSize,
                lineHeight = 28.sp,
                fontFamily = ElmsSans,
                fontWeight = SettingsSubScreenHeaderDefaults.TitleFontWeight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(SettingsSubScreenHeaderDefaults.TitleToSubtitleSpacing))
                Text(
                    text = subtitle,
                    color = ElectricSlateTokens.TextMuted,
                    fontSize = SettingsSubScreenHeaderDefaults.SubtitleFontSize,
                    lineHeight = 20.sp,
                    fontFamily = ElmsSans,
                    fontWeight = SettingsSubScreenHeaderDefaults.SubtitleFontWeight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (actions != null) {
            actions()
        }
    }
}
