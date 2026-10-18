// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.friends.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.ui.components.presets.AppCard
import com.arinara.fotara.ui.components.presets.AppListItem
import com.arinara.fotara.ui.components.presets.AppListItemAction
import com.arinara.fotara.ui.theme.ElectricSlateTokens

/**
 * Reusable Friends System entry card placed in SettingsScreen directly below Profile and above General.
 * Strictly adheres to the SettingsCardItem design system tokens for harmonious dark navy presentation.
 */
@Composable
fun FriendsSettingsCard(
    modifier: Modifier = Modifier,
    activeFriendsCount: Int = 0,
    totalFriendsCount: Int = 0,
    onClick: () -> Unit
) {
    val subtitleText = when {
        activeFriendsCount > 0 && totalFriendsCount > 0 ->
            "$activeFriendsCount online • $totalFriendsCount friends"
        totalFriendsCount > 0 ->
            "$totalFriendsCount friends"
        else ->
            "Connect with friends, share notes, study presence"
    }

    AppCard(
        cornerRadius = 24.dp,
        containerColor = ElectricSlateTokens.SurfacePanel,
        borderColor = ElectricSlateTokens.BorderSubtle,
        contentPadding = 0.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        AppListItem(
            title = "Friends",
            subtitle = subtitleText,
            leadingIcon = Icons.Outlined.People,
            leadingIconTint = ElectricSlateTokens.PrimaryAccent,
            action = AppListItemAction.Chevron,
            onClick = onClick
        )
    }
}
