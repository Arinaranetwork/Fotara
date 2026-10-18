// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Standard pill & interactive element token helpers matching Design Audit directive:
 * - Filled Pills (Active / Primary): primaryContainer background, onPrimaryContainer content
 * - Tonal / Neutral Pills (Inactive / Filters): surfaceContainerHigh background, onSurfaceVariant content
 * - Outlined Pills (Secondary / Subtle): Transparent background, outlineVariant 1dp border, onSurface content
 */
object FotaraPillTokens {
    @Composable
    fun activeContainerColor(): Color = MaterialTheme.colorScheme.primaryContainer

    @Composable
    fun activeContentColor(): Color = MaterialTheme.colorScheme.onPrimaryContainer

    @Composable
    fun neutralContainerColor(): Color = MaterialTheme.colorScheme.surfaceContainerHigh

    @Composable
    fun neutralContentColor(): Color = MaterialTheme.colorScheme.onSurfaceVariant

    @Composable
    fun outlinedBorder(): BorderStroke = BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
fun Modifier.fotaraActivePill(): Modifier = this
    .background(MaterialTheme.colorScheme.primaryContainer, FotaraShapes.Pill)

@Composable
fun Modifier.fotaraNeutralPill(): Modifier = this
    .background(MaterialTheme.colorScheme.surfaceContainerHigh, FotaraShapes.Pill)

@Composable
fun Modifier.fotaraOutlinedPill(): Modifier = this
    .background(Color.Transparent, FotaraShapes.Pill)
    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, FotaraShapes.Pill)
