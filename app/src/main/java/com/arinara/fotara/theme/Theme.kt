// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FotaraDarkColorScheme = darkColorScheme(
    primary = FolderBodyBlue,
    onPrimary = FolderTextWhite,
    primaryContainer = FolderBodyBlueDark,
    onPrimaryContainer = FolderTabCream,
    secondary = DockSlatePill,
    onSecondary = Color.White,
    secondaryContainer = MidnightSurface,
    onSecondaryContainer = TextSecondary,
    background = MidnightNavy,
    onBackground = Color.White,
    surface = MidnightSurface,
    onSurface = Color.White,
    surfaceVariant = NewFolderBody,
    onSurfaceVariant = TextSecondary,
    outline = MidnightCardOutline
)

@Composable
fun FotaraTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FotaraDarkColorScheme,
        typography = Typography,
        content = content
    )
}
