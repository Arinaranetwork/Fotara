// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.arinara.fotara.R

/**
 * Global Elms Sans font family with strictly three static weights:
 * - Light (300): secondary text, captions, timestamps, placeholders, muted labels, metadata
 * - Medium (500): body text, button labels, list items, chips, tabs, form inputs, dialog content
 * - Bold (700): screen titles, folder names, card headings, primary action buttons, key metrics
 *
 * All weights map strictly to one of these three. No italics anywhere in the UI.
 */
val ElmsSans = FontFamily(
    // Light (300)
    Font(R.font.elms_sans_light, FontWeight.Thin),
    Font(R.font.elms_sans_light, FontWeight.ExtraLight),
    Font(R.font.elms_sans_light, FontWeight.Light),

    // Medium (500)
    Font(R.font.elms_sans_medium, FontWeight.Normal),
    Font(R.font.elms_sans_medium, FontWeight.Medium),

    // Bold (700)
    Font(R.font.elms_sans_bold, FontWeight.SemiBold),
    Font(R.font.elms_sans_bold, FontWeight.Bold),
    Font(R.font.elms_sans_bold, FontWeight.ExtraBold),
    Font(R.font.elms_sans_bold, FontWeight.Black)
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = ElmsSans,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp
    ),
    displayMedium = TextStyle(
        fontFamily = ElmsSans,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp
    ),
    displaySmall = TextStyle(
        fontFamily = ElmsSans,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = ElmsSans,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = ElmsSans,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = ElmsSans,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleLarge = TextStyle(
        fontFamily = ElmsSans,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontFamily = ElmsSans,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = ElmsSans,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = ElmsSans,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = ElmsSans,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    bodySmall = TextStyle(
        fontFamily = ElmsSans,
        fontWeight = FontWeight.Light,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = ElmsSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = ElmsSans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = ElmsSans,
        fontWeight = FontWeight.Light,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)
