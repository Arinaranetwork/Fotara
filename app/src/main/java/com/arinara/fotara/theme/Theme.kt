// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val FotaraDarkColorScheme = darkColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = BrandPrimaryContainer,
    onPrimaryContainer = Color.White,
    inversePrimary = BrandPrimaryLight,

    secondary = BrandSecondarySlate,
    onSecondary = Color.White,
    secondaryContainer = SurfaceLevel2HighDark,
    onSecondaryContainer = BrandAccentCream,

    tertiary = BrandAccentCream,
    onTertiary = SurfaceLevel1Dark,
    tertiaryContainer = OutlineDark,
    onTertiaryContainer = BrandAccentCream,

    background = BaseBackgroundDark,
    onBackground = Color.White,

    surface = BaseBackgroundDark,
    onSurface = Color.White,

    surfaceContainerLowest = SurfaceLowestDark,
    surfaceContainerLow = SurfaceLevel1Dark,
    surfaceContainer = SurfaceLevel2Dark,
    surfaceContainerHigh = SurfaceLevel2HighDark,
    surfaceContainerHighest = SurfaceLevel3Dark,

    surfaceVariant = SurfaceLevel2Dark,
    onSurfaceVariant = TextSecondary,

    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,

    error = TagCrimson,
    onError = Color.White
)

val FotaraLightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E40AF),
    inversePrimary = BrandPrimaryLight,

    secondary = Color(0xFF475569),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = Color(0xFF0F172A),

    tertiary = Color(0xFF0D9488),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCCFBF1),
    onTertiaryContainer = Color(0xFF115E59),

    background = BaseBackgroundLight,
    onBackground = Color(0xFF0F172A),

    surface = BaseBackgroundLight,
    onSurface = Color(0xFF0F172A),

    surfaceContainerLowest = SurfaceLowestLight,
    surfaceContainerLow = SurfaceLevel1Light,
    surfaceContainer = SurfaceLevel2Light,
    surfaceContainerHigh = SurfaceLevel2HighLight,
    surfaceContainerHighest = SurfaceLevel3Light,

    surfaceVariant = SurfaceLevel2Light,
    onSurfaceVariant = Color(0xFF475569),

    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,

    error = Color(0xFFDC2626),
    onError = Color.White
)

@Composable
fun FotaraTheme(
    darkTheme: Boolean = true, // Dark dominant per Android.md
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) FotaraDarkColorScheme else FotaraLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
