// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// 1. CENTRALIZED SEMANTIC COLOR TOKENS
// Strict adherence to Android.md (Base #0A0D14, Primary #2563EB, Accent #EFE8DA)
// ============================================================================

// Base Canvas Backgrounds
val BaseBackgroundDark = Color(0xFF0A0D14)
val BaseBackgroundLight = Color(0xFFF8FAFC)

// Surface Level 1 (Cards, Main Containers, subtle depth separation)
val SurfaceLevel1Dark = Color(0xFF111726) // Base +4% Card tint per Android.md
val SurfaceLevel1Light = Color(0xFFFFFFFF)

// Surface Level 2 (Toolbars, Search Pills, elevated modals)
val SurfaceLevel2Dark = Color(0xFF141B2A) // Base +8% Search tint per Android.md
val SurfaceLevel2Light = Color(0xFFF1F5F9)

// Surface Level 2 High (Active highlight, elevated panels)
val SurfaceLevel2HighDark = Color(0xFF182236) // Base +12% Active Highlight per Android.md
val SurfaceLevel2HighLight = Color(0xFFE2E8F0)

// Surface Level 3 (Dialogs, Menus, Hover Sheets)
val SurfaceLevel3Dark = Color(0xFF1F293D)
val SurfaceLevel3Light = Color(0xFFCBD5E1)

// Surface Lowest
val SurfaceLowestDark = Color(0xFF070A10)
val SurfaceLowestLight = Color(0xFFFFFFFF)

// Outlines & Borders
val OutlineDark = Color(0xFF2A354D)
val OutlineVariantDark = Color(0xFF161E30) // Crisp, ultra-subtle 1dp border
val OutlineLight = Color(0xFF94A3B8)
val OutlineVariantLight = Color(0xFFE2E8F0)

// Brand Primaries & Accents
val BrandPrimary = Color(0xFF2563EB) // Primary action blue
val BrandPrimaryContainer = Color(0xFF1D4ED8)
val BrandPrimaryLight = Color(0xFF60A5FA)
val BrandAccentCream = Color(0xFFEFE8DA) // Distinct folder accent cream
val BrandAccentCreamBorder = Color(0xFFD8CFBF)
val BrandSecondarySlate = Color(0xFF4A4C68)

// Subject / Category Tag Palette
val TagCrimson = Color(0xFFDC5A5A)
val TagAmber = Color(0xFFF4A261)
val TagEmerald = Color(0xFF2A9D8F)
val TagViolet = Color(0xFF9D4EDD)
val TagSky = Color(0xFF00B4D8)
val TagRose = Color(0xFFE76F51)

// Typography Neutrals
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFA0A5C2)
val TextMuted = Color(0xFF6F7491)

// ============================================================================
// 2. BACKWARDS-COMPATIBLE ALIASES
// Maps legacy identifiers directly to centralized semantic tokens
// ============================================================================
val MidnightNavy = BaseBackgroundDark
val MidnightSurface = SurfaceLevel1Dark
val MidnightCardOutline = OutlineDark

val FolderTabCream = BrandAccentCream
val FolderTabCreamBorder = BrandAccentCreamBorder
val FolderBodyBlue = BrandPrimary
val FolderBodyBlueDark = BrandPrimaryContainer
val FolderTextWhite = Color(0xFFFFFFFF)

val NewFolderBody = SurfaceLevel2Dark
val NewFolderBorder = OutlineVariantDark
val NewFolderText = Color(0xFFEEEEEE)

val DockSlatePill = BrandSecondarySlate
val DockSlatePillLight = Color(0xFF6B6E94)
val DividerColor = OutlineVariantDark

val HomeNearBlack = BaseBackgroundDark
val HomeCardSurface = SurfaceLevel1Dark
val HomeCardBorder = OutlineVariantDark
val HomeHeaderButtonSurface = SurfaceLevel2Dark
val HomeHeaderButtonBg = SurfaceLevel2Dark
val HomeTabBarContainer = SurfaceLevel1Dark
val HomeTabBarSelectedPill = BrandPrimary
val HomeSegmentSelectedPill = BrandPrimary
val HomeSearchBarSurface = SurfaceLevel2Dark
val HomeSearchBarBorder = OutlineDark
val HomeAddButtonBlue = BrandPrimary
val HomeMainButtonBlue = BrandPrimary
val HomeBottomNavSurface = SurfaceLevel1Dark
val HomeBottomNavBorder = OutlineVariantDark
val HomeBottomNavActiveHighlight = SurfaceLevel2HighDark
val HomeSubtitleGray = TextMuted
val HomeUnselectedGray = TextMuted

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
