// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components.presets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.arinara.fotara.ui.theme.ElectricSlateTokens

/**
 * Standard minimum bottom clearance constant (100.dp) ensuring buttons, list items,
 * and controls are fully scrollable and never obstructed by bottom floating navigation docks.
 */
val DefaultBottomClearance: Dp = 100.dp

/**
 * Universal screen container preset built on Material 3 Scaffold.
 *
 * Automatically guarantees safe bottom clearance (minimum 96.dp / 100.dp) to screen
 * contents, ensuring LazyColumn, LazyVerticalGrid, and scrollable containers have
 * adequate scrolling space above floating bottom bars and system navigation bars.
 *
 * @param modifier Modifier applied to the outer Scaffold.
 * @param topBar Top app bar slot (e.g. AppTopBar).
 * @param bottomBar Bottom bar slot (e.g. AppFloatingBar).
 * @param floatingActionButton FAB slot (e.g. AppFAB).
 * @param snackbarHost Snackbar host slot.
 * @param containerColor Background canvas color (defaults to ElectricSlateTokens.BgCanvas).
 * @param contentColor Default content color (defaults to ElectricSlateTokens.TextPrimary).
 * @param minBottomClearance Minimum bottom padding enforced on content (defaults to 100.dp).
 * @param content Composable slot receiving the calculated safe PaddingValues to apply to
 *                LazyColumn/LazyVerticalGrid contentPadding or container insets.
 */
@Composable
fun AppScreen(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    containerColor: Color = ElectricSlateTokens.BgCanvas,
    contentColor: Color = ElectricSlateTokens.TextPrimary,
    minBottomClearance: Dp = DefaultBottomClearance,
    content: @Composable (contentPadding: PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = topBar,
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        snackbarHost = snackbarHost,
        containerColor = containerColor,
        contentColor = contentColor
    ) { innerPadding ->
        val safeBottom = if (innerPadding.calculateBottomPadding() >= minBottomClearance) {
            innerPadding.calculateBottomPadding()
        } else {
            minBottomClearance
        }

        val safeContentPadding = PaddingValues(
            start = innerPadding.calculateStartPadding(LayoutDirection.Ltr),
            top = innerPadding.calculateTopPadding(),
            end = innerPadding.calculateEndPadding(LayoutDirection.Ltr),
            bottom = safeBottom
        )

        content(safeContentPadding)
    }
}

/**
 * Utility spacer for standard scrollable Columns to provide guaranteed bottom clearance
 * above floating navigation bars.
 */
@Composable
fun BottomClearanceSpacer(
    clearance: Dp = DefaultBottomClearance,
    modifier: Modifier = Modifier
) {
    Spacer(modifier = modifier.height(clearance))
}
