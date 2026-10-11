// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.theme

import androidx.compose.ui.graphics.Color

// Core Brand Palette from UI reference
val MidnightNavy = Color(0xFF03071E)
val MidnightSurface = Color(0xFF070D2B)
val MidnightCardOutline = Color(0xFF142055)

// Distinct Folder Card Palette
val FolderTabCream = Color(0xFFEFE8DA)
val FolderTabCreamBorder = Color(0xFFD8CFBF)
val FolderBodyBlue = Color(0xFF0316A8)
val FolderBodyBlueDark = Color(0xFF020F7A)
val FolderTextWhite = Color(0xFFFFFFFF)

// "+New" Folder Card
val NewFolderBody = Color(0xFF060B24)
val NewFolderBorder = Color(0xFF18224D)
val NewFolderText = Color(0xFFEEEEEE)

// Floating Dock / Search Pill
val DockSlatePill = Color(0xFF4A4C68)
val DockSlatePillLight = Color(0xFF6B6E94)

// Category / Subject Tag Colors
val TagCrimson = Color(0xFFE63946)
val TagAmber = Color(0xFFF4A261)
val TagEmerald = Color(0xFF2A9D8F)
val TagViolet = Color(0xFF9D4EDD)
val TagSky = Color(0xFF00B4D8)
val TagRose = Color(0xFFE76F51)

// Muted & Secondary
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFA0A5C2)
val TextMuted = Color(0xFF6F7491)
val DividerColor = Color(0xFF161E42)

// --- Home Screen Redesign Tokens (IMAGE A) ---
val HomeNearBlack = Color(0xFF0A0D14)
val HomeCardSurface = Color(0xFF111726)
val HomeCardBorder = Color(0xFF161E30)
val HomeHeaderButtonSurface = Color(0xFF131925)
val HomeHeaderButtonBg = HomeHeaderButtonSurface
val HomeTabBarContainer = Color(0xFF121826)
val HomeTabBarSelectedPill = Color(0xFF1B4FC4)
val HomeSegmentSelectedPill = HomeTabBarSelectedPill
val HomeSearchBarSurface = Color(0xFF141B2A)
val HomeSearchBarBorder = Color(0xFF1C2538)
val HomeAddButtonBlue = Color(0xFF2563EB)
val HomeMainButtonBlue = HomeAddButtonBlue
val HomeBottomNavSurface = Color(0xFF111726)
val HomeBottomNavBorder = Color(0xFF1C2333)
val HomeBottomNavActiveHighlight = Color(0xFF182236)
val HomeSubtitleGray = Color(0xFF6B7280)
val HomeUnselectedGray = Color(0xFF6B7280)

/**
 * Modern folder accent colors for icon tiles and LinkIt corner glows.
 */
data class FolderAccent(
    val key: String,
    val name: String,
    val hexCode: String,
    val tileFill: Color,
    val iconTint: Color,
    val glowColor: Color
)

object FolderAccentPalette {
    val Blue = FolderAccent(
        key = "blue",
        name = "Blue",
        hexCode = "#1E3A6B",
        tileFill = Color(0xFF1E3A6B),
        iconTint = Color(0xFF60A5FA),
        glowColor = Color(0xFF3B82F6)
    )
    val Brown = FolderAccent(
        key = "brown",
        name = "Brown",
        hexCode = "#5C4030",
        tileFill = Color(0xFF5C4030),
        iconTint = Color(0xFFFBBF24),
        glowColor = Color(0xFFF59E0B)
    )
    val Purple = FolderAccent(
        key = "purple",
        name = "Purple",
        hexCode = "#3F3270",
        tileFill = Color(0xFF3F3270),
        iconTint = Color(0xFFC084FC),
        glowColor = Color(0xFF8B5CF6)
    )
    val Green = FolderAccent(
        key = "green",
        name = "Green",
        hexCode = "#1E4A38",
        tileFill = Color(0xFF1E4A38),
        iconTint = Color(0xFF4ADE80),
        glowColor = Color(0xFF34D399)
    )
    val Red = FolderAccent(
        key = "red",
        name = "Red",
        hexCode = "#6B2A30",
        tileFill = Color(0xFF6B2A30),
        iconTint = Color(0xFFF87171),
        glowColor = Color(0xFFEF4444)
    )
    val Slate = FolderAccent(
        key = "slate",
        name = "Slate",
        hexCode = "#343C52",
        tileFill = Color(0xFF343C52),
        iconTint = Color(0xFF94A3B8),
        glowColor = Color(0xFF38BDF8)
    )

    val all = listOf(Blue, Brown, Purple, Green, Red, Slate)

    fun fromHexOrDefault(hex: String?, folderId: Long = 0L, folderName: String = ""): FolderAccent {
        if (!hex.isNullOrBlank()) {
            val matched = all.firstOrNull { it.hexCode.equals(hex, ignoreCase = true) }
            if (matched != null) return matched
            when (hex.uppercase()) {
                "#E63946" -> return Red
                "#F4A261" -> return Brown
                "#2A9D8F" -> return Green
                "#9D4EDD" -> return Purple
                "#E76F51" -> return Red
                "#00B4D8" -> {
                    val nameLower = folderName.trim().lowercase()
                    return when {
                        nameLower.contains("testing") -> Blue
                        nameLower.contains("lanjut") -> Brown
                        nameLower.contains("wajib") -> Purple
                        nameLower.contains("fisika") -> Green
                        nameLower.contains("kimia") -> Red
                        nameLower.contains("sejarah") -> Slate
                        else -> {
                            val index = ((folderId xor 0x5DEECE66DL).and(0x7FFFFFFFL) % all.size).toInt()
                            all[index]
                        }
                    }
                }
            }
        }
        val nameLower = folderName.trim().lowercase()
        return when {
            nameLower.contains("testing") -> Blue
            nameLower.contains("lanjut") -> Brown
            nameLower.contains("wajib") -> Purple
            nameLower.contains("fisika") -> Green
            nameLower.contains("kimia") -> Red
            nameLower.contains("sejarah") -> Slate
            else -> {
                val index = ((folderId xor 0x5DEECE66DL).and(0x7FFFFFFFL) % all.size).toInt()
                all[index]
            }
        }
    }
}
