// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components.presets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import com.arinara.fotara.ui.theme.jiggleRotationClickable

@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    navigationIcon: ImageVector = Icons.AutoMirrored.Filled.ArrowBack,
    containerColor: Color = ElectricSlateTokens.BgCanvas,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor)
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                if (onBackClick != null) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .jiggleRotationClickable(onClick = onBackClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = navigationIcon,
                            contentDescription = "Back",
                            tint = ElectricSlateTokens.TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Box {
                    if (subtitle != null) {
                        androidx.compose.foundation.layout.Column {
                            Text(
                                text = title,
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                            Text(
                                text = subtitle,
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                color = ElectricSlateTokens.TextMuted
                            )
                        }
                    } else {
                        Text(
                            text = title,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp,
                            color = ElectricSlateTokens.TextPrimary
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                actions()
            }
        }
    }
}
