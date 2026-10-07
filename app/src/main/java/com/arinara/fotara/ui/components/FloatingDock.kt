// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.arinara.fotara.theme.HomeAddButtonBlue
import com.arinara.fotara.theme.HomeCardSurface
import com.arinara.fotara.theme.HomeSearchBarBorder
import com.arinara.fotara.theme.HomeSearchBarSurface

/**
 * Modern search bar and main '+' button row matching IMAGE A.
 * Features a dark full-pill search field and a circular blue action button
 * opening an upward-anchored menu with "New folder" and "Capture notes".
 */
/**
 * Search bar pill component (~52dp tall) used in the bottom dock.
 */
@Composable
fun FloatingSearchBarPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(HomeSearchBarSurface)
            .border(1.dp, HomeSearchBarBorder, RoundedCornerShape(26.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color(0xFF6B7280),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = stringResource(R.string.search_hint),
                color = Color(0xFF6B7280),
                fontSize = 15.sp,
                fontWeight = FontWeight.Light,
                fontFamily = ElmsSans
            )
        }
    }
}

@Composable
fun FloatingDock(
    onSearchClick: () -> Unit,
    onNewFolderClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FloatingSearchBarPill(
            onClick = onSearchClick,
            modifier = Modifier.weight(1f)
        )

        // Circular '+' Button (52dp, #2563EB) opens New Folder directly
        SharedFloatingAddButton(
            onClick = onNewFolderClick,
            contentDescription = stringResource(R.string.menu_new_folder)
        )
    }
}
