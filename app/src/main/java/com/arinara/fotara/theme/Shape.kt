// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Corporate shape tokens adhering to Android.md and Design Audit guidelines:
 * - Full Pill: CircleShape or RoundedCornerShape(999.dp) (strictly for pills, chips, search bars, tag badges, bottom floating toolbars)
 * - Large Containers & Bottom Sheets: RoundedCornerShape(28.dp)
 * - Cards & Primary Modules: RoundedCornerShape(16.dp) or RoundedCornerShape(20.dp)
 * - Small Elements & Text Input Fields: RoundedCornerShape(12.dp)
 * - Buttons & Action Targets: RoundedCornerShape(8.dp)
 */
object FotaraShapes {
    // 999.dp / CircleShape tokens for pills, chips, search bars, action toolbars
    val FullPill = CircleShape
    val Pill = RoundedCornerShape(999.dp)

    // 28.dp tokens for bottom sheets and large container dialogs
    val BottomSheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val ContainerLarge = RoundedCornerShape(28.dp)

    // 20.dp tokens for modal dialogs and elevated panels
    val Dialog = RoundedCornerShape(20.dp)
    val CardLarge = RoundedCornerShape(20.dp)

    // 16.dp tokens for standard cards, grid items, and modules
    val Card = RoundedCornerShape(16.dp)

    // 12.dp tokens for inputs, dropdowns, and sub-elements
    val InputField = RoundedCornerShape(12.dp)
    val CardSmall = RoundedCornerShape(12.dp)
    val ElementSmall = RoundedCornerShape(12.dp)

    // 8.dp tokens for compact buttons and micro tags
    val Button = RoundedCornerShape(8.dp)
    val ElementMicro = RoundedCornerShape(8.dp)
}

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
