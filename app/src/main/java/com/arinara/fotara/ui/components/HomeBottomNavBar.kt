// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.R
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.theme.HomeBottomNavActiveHighlight
import com.arinara.fotara.theme.HomeBottomNavBorder
import com.arinara.fotara.theme.HomeBottomNavSurface

import com.arinara.fotara.ui.theme.ElectricSlateTokens
import com.arinara.fotara.ui.theme.jigglePressClickable

enum class HomeNavTab { HOME, NOTES, SETTINGS }

/**
 * Shared source of truth for the measured height of the floating bottom overlay.
 * Used for list content padding, snackbar hosts, and floating action buttons across tab-hosted screens.
 */
val LocalBottomOverlayPadding = androidx.compose.runtime.compositionLocalOf { 0.dp }

/**
 * Floating bottom navigation pill adhering strictly to Electric Slate tokens and presets.
 * Features a 999dp capsule pill container with #1F2736 surface,
 * 1dp BorderSubtle, and 3 equal tabs: Home, Notes, Settings.
 */
@Composable
fun HomeBottomNavBar(
    selectedTab: HomeNavTab,
    onTabSelected: (HomeNavTab) -> Unit,
    onTabReSelected: ((HomeNavTab) -> Unit)? = null,
    onHomeTabLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(64.dp)
            .clip(CircleShape)
            .background(ElectricSlateTokens.SurfaceElevated)
            .border(1.dp, ElectricSlateTokens.BorderSubtle, CircleShape)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tab 1: Home
            HomeNavItem(
                icon = Icons.Filled.Home,
                label = stringResource(R.string.nav_home),
                isSelected = selectedTab == HomeNavTab.HOME,
                onClick = {
                    if (selectedTab == HomeNavTab.HOME) {
                        onTabReSelected?.invoke(HomeNavTab.HOME) ?: onTabSelected(HomeNavTab.HOME)
                    } else {
                        onTabSelected(HomeNavTab.HOME)
                    }
                },
                onLongClick = onHomeTabLongClick,
                modifier = Modifier.weight(1f)
            )

            // Tab 2: Notes
            HomeNavItem(
                icon = Icons.Outlined.Description,
                label = stringResource(R.string.nav_notes),
                isSelected = selectedTab == HomeNavTab.NOTES,
                onClick = {
                    if (selectedTab == HomeNavTab.NOTES) {
                        onTabReSelected?.invoke(HomeNavTab.NOTES) ?: onTabSelected(HomeNavTab.NOTES)
                    } else {
                        onTabSelected(HomeNavTab.NOTES)
                    }
                },
                modifier = Modifier.weight(1f)
            )

            // Tab 3: Settings
            HomeNavItem(
                icon = Icons.Outlined.Settings,
                label = stringResource(R.string.nav_settings),
                isSelected = selectedTab == HomeNavTab.SETTINGS,
                onClick = {
                    if (selectedTab == HomeNavTab.SETTINGS) {
                        onTabReSelected?.invoke(HomeNavTab.SETTINGS) ?: onTabSelected(HomeNavTab.SETTINGS)
                    } else {
                        onTabSelected(HomeNavTab.SETTINGS)
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val activeColor = ElectricSlateTokens.PrimaryAccent
    val inactiveColor = ElectricSlateTokens.TextMuted

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(CircleShape)
            .then(
                if (isSelected) {
                    Modifier
                        .background(activeColor.copy(alpha = 0.18f))
                        .border(1.dp, activeColor.copy(alpha = 0.4f), CircleShape)
                } else Modifier
            )
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                } else {
                    Modifier.jigglePressClickable(onClick = onClick)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) activeColor else inactiveColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = label,
                color = if (isSelected) activeColor else inactiveColor,
                fontSize = 11.5.sp,
                lineHeight = 15.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                fontFamily = ElmsSans
            )
        }
    }
}
