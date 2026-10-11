// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.theme.HomeHeaderButtonBg
import com.arinara.fotara.theme.HomeMainButtonBlue
import com.arinara.fotara.theme.HomeSubtitleGray

object ScreenHeaderDefaults {
    val HeaderHeight: Dp = 56.dp
    val HorizontalPadding: Dp = 16.dp
    val TopPadding: Dp = 16.dp
    val BottomPadding: Dp = 12.dp
    val TitleToTaglineGap: Dp = 4.dp
    val TaglineSlotHeight: Dp = 18.dp
    val HeaderBottomGap: Dp = 12.dp

    val ActionButtonSize: Dp = 48.dp
    val ActionButtonContentSize: Dp = 40.dp
    val ActionIconSize: Dp = 20.dp
    val ActionButtonSpacing: Dp = 4.dp

    val TitleFontSize: TextUnit = 22.sp
    val TitleLineHeight: TextUnit = 28.sp
    val TitleFontWeight: FontWeight = FontWeight.Bold
    val TitleLetterSpacing: TextUnit = 0.sp

    val TaglineFontSize: TextUnit = 14.sp
    val TaglineLineHeight: TextUnit = 18.sp
    val TaglineFontWeight: FontWeight = FontWeight.Normal
}

/**
 * Shared screen header composable across primary tab destinations (Home, Notes, Settings).
 * Enforces unified title typography, padding, circular action buttons, and aligned vertical baseline.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    tagline: String? = null,
    spaceName: String? = null,
    onTitleClick: (() -> Unit)? = null,
    onTitleLongClick: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    val density = LocalDensity.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ScreenHeaderDefaults.HeaderHeight)
            .padding(
                start = ScreenHeaderDefaults.HorizontalPadding,
                end = ScreenHeaderDefaults.HorizontalPadding
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = if (onTitleClick != null || onTitleLongClick != null) {
                    Modifier
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                        .combinedClickable(
                            onClick = { onTitleClick?.invoke() },
                            onLongClick = { onTitleLongClick?.invoke() }
                        )
                } else Modifier
            ) {
                Text(
                    text = title,
                    style = TextStyle(
                        fontSize = ScreenHeaderDefaults.TitleFontSize,
                        lineHeight = ScreenHeaderDefaults.TitleLineHeight,
                        fontFamily = ElmsSans,
                        fontWeight = ScreenHeaderDefaults.TitleFontWeight,
                        letterSpacing = ScreenHeaderDefaults.TitleLetterSpacing,
                        color = Color.White,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.None
                        )
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (spaceName != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.ArrowDropDown,
                        contentDescription = "Switch Space",
                        tint = Color(0xFFA0A5C2),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = spaceName,
                        style = TextStyle(
                            fontSize = 20.sp,
                            lineHeight = ScreenHeaderDefaults.TitleLineHeight,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = ScreenHeaderDefaults.TitleLetterSpacing,
                            color = Color(0xFFA0A5C2),
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.None
                            )
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (!tagline.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(ScreenHeaderDefaults.TitleToTaglineGap))
                Text(
                    text = tagline,
                    style = TextStyle(
                        fontSize = ScreenHeaderDefaults.TaglineFontSize,
                        lineHeight = ScreenHeaderDefaults.TaglineLineHeight,
                        fontFamily = ElmsSans,
                        fontWeight = ScreenHeaderDefaults.TaglineFontWeight,
                        color = HomeSubtitleGray,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.None
                        )
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (actions != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(ScreenHeaderDefaults.ActionButtonSpacing),
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
        }
    }
}

/**
 * Standardized circular action button for ScreenHeader.
 * Provides a 48dp minimum touch target wrapping a 40dp circular container with 20dp icon.
 */
@Composable
fun ScreenHeaderActionButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    isActive: Boolean = false,
    activeBackground: Color = HomeMainButtonBlue,
    inactiveBackground: Color = HomeHeaderButtonBg
) {
    Box(
        modifier = modifier
            .size(ScreenHeaderDefaults.ActionButtonSize)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(ScreenHeaderDefaults.ActionButtonContentSize)
                .clip(CircleShape)
                .background(if (isActive) activeBackground else inactiveBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(ScreenHeaderDefaults.ActionIconSize)
            )
        }
    }
}
