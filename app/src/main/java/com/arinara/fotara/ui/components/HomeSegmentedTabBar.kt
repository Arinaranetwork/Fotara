// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.R
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.theme.HomeTabBarContainer
import com.arinara.fotara.theme.HomeTabBarSelectedPill

enum class HomeSegment { ALL, FAVORIT, ARSIP }

/**
 * Modern segmented tab bar matching IMAGE A.
 * Features a full-width pill container (~#121826) with three equal segments:
 * - "All": selected with blue filled pill (#1B4FC4), white label, 2x2 grid icon.
 * - "Favorit": star outline icon, muted label.
 * - "Arsip": archive outline icon, muted label.
 * - Thin 1dp vertical divider between unselected segments only.
 */
@Composable
fun HomeSegmentedTabBar(
    selectedSegment: HomeSegment,
    onSegmentSelected: (HomeSegment) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(HomeTabBarContainer)
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Segment 1: All
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .then(
                        if (selectedSegment == HomeSegment.ALL) {
                            Modifier.background(HomeTabBarSelectedPill)
                        } else Modifier
                    )
                    .clickable { onSegmentSelected(HomeSegment.ALL) },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.GridView,
                        contentDescription = null,
                        tint = if (selectedSegment == HomeSegment.ALL) Color.White else Color(0xFF6B7280),
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = stringResource(R.string.tab_all),
                        color = if (selectedSegment == HomeSegment.ALL) Color.White else Color(0xFF6B7280),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = ElmsSans
                    )
                }
            }

            // Divider between All and Favorit (only if neither is selected)
            if (selectedSegment != HomeSegment.ALL && selectedSegment != HomeSegment.FAVORIT) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(18.dp)
                        .background(Color(0xFF232D42))
                )
            }

            // Segment 2: Favorit
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .then(
                        if (selectedSegment == HomeSegment.FAVORIT) {
                            Modifier.background(HomeTabBarSelectedPill)
                        } else Modifier
                    )
                    .clickable { onSegmentSelected(HomeSegment.FAVORIT) },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.StarOutline,
                        contentDescription = null,
                        tint = if (selectedSegment == HomeSegment.FAVORIT) Color.White else Color(0xFF6B7280),
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = stringResource(R.string.tab_favorit),
                        color = if (selectedSegment == HomeSegment.FAVORIT) Color.White else Color(0xFF6B7280),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = ElmsSans
                    )
                }
            }

            // Divider between Favorit and Arsip (only if neither is selected - visible in IMAGE A!)
            if (selectedSegment != HomeSegment.FAVORIT && selectedSegment != HomeSegment.ARSIP) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(18.dp)
                        .background(Color(0xFF232D42))
                )
            }

            // Segment 3: Arsip
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .then(
                        if (selectedSegment == HomeSegment.ARSIP) {
                            Modifier.background(HomeTabBarSelectedPill)
                        } else Modifier
                    )
                    .clickable { onSegmentSelected(HomeSegment.ARSIP) },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Archive,
                        contentDescription = null,
                        tint = if (selectedSegment == HomeSegment.ARSIP) Color.White else Color(0xFF6B7280),
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = stringResource(R.string.tab_arsip),
                        color = if (selectedSegment == HomeSegment.ARSIP) Color.White else Color(0xFF6B7280),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = ElmsSans
                    )
                }
            }
        }
    }
}
