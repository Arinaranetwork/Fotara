// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.model

import androidx.compose.ui.graphics.Color
import com.arinara.fotara.theme.TagAmber
import com.arinara.fotara.theme.TagCrimson
import com.arinara.fotara.theme.TagEmerald
import com.arinara.fotara.theme.TagRose
import com.arinara.fotara.theme.TagSky
import com.arinara.fotara.theme.TagViolet

enum class TagColor(val hex: String, val displayName: String, val composeColor: Color) {
    CRIMSON("#E63946", "Crimson", TagCrimson),
    AMBER("#F4A261", "Amber", TagAmber),
    EMERALD("#2A9D8F", "Emerald", TagEmerald),
    VIOLET("#9D4EDD", "Violet", TagViolet),
    SKY("#00B4D8", "Sky", TagSky),
    ROSE("#E76F51", "Rose", TagRose);

    companion object {
        fun fromHex(hex: String?): TagColor {
            return entries.firstOrNull { it.hex.equals(hex, ignoreCase = true) } ?: SKY
        }
    }
}
