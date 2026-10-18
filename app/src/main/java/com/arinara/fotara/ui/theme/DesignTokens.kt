// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.theme

import androidx.compose.ui.graphics.Color

object ElectricSlateTokens {
    val BgCanvas = Color(0xFF0D1117)        // Main background and dot-grid canvas
    val SurfacePanel = Color(0xFF161C26)    // Cards, bottom docks, settings panels
    val SurfaceElevated = Color(0xFF1F2736) // Dropdowns, floating overlays, tooltips
    val BorderSubtle = Color(0xFF262F3E)    // 1dp subtle structural outline
    val PrimaryAccent = Color(0xFF7A5AF8)   // Electric violet for active states, thumbs, droplet
    val TrackInactive = Color(0xFF283245)   // Inactive tracks, neutral buttons
    val TextPrimary = Color(0xFFF8FAFC)     // High-contrast primary text
    val TextSecondary = Color(0xFF94A3B8)   // Secondary labels and subtitles
    val TextMuted = Color(0xFF94A3B8)       // Secondary labels and descriptions
    val Destructive = Color(0xFFDC5A5A)     // Error states, delete actions, warnings (muted red)

    data class Swatch(
        val name: String,
        val color: Color,
        val hex: String,
        val description: String
    )

    val AllSwatches: List<Swatch> = listOf(
        Swatch("BgCanvas", BgCanvas, "#0D1117", "Main background and dot-grid canvas"),
        Swatch("SurfacePanel", SurfacePanel, "#161C26", "Cards, bottom docks, settings panels"),
        Swatch("SurfaceElevated", SurfaceElevated, "#1F2736", "Dropdowns, floating overlays, tooltips"),
        Swatch("BorderSubtle", BorderSubtle, "#262F3E", "1dp subtle structural outline"),
        Swatch("PrimaryAccent", PrimaryAccent, "#7A5AF8", "Electric violet for active states and droplet"),
        Swatch("TrackInactive", TrackInactive, "#283245", "Inactive tracks, neutral buttons"),
        Swatch("TextPrimary", TextPrimary, "#F8FAFC", "High-contrast primary text"),
        Swatch("TextMuted", TextMuted, "#94A3B8", "Secondary labels and descriptions"),
        Swatch("Destructive", Destructive, "#DC5A5A", "Error states, delete actions, warnings (muted red)")
    )
}
