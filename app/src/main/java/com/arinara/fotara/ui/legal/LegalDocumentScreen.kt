// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.legal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.legal.ParsedLegalDocument
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.theme.HomeNearBlack
import com.arinara.fotara.theme.HomeSubtitleGray
import com.arinara.fotara.theme.MidnightNavy
import com.arinara.fotara.ui.components.LocalBottomOverlayPadding
import com.arinara.fotara.ui.components.ReleaseNotesRenderer

/**
 * Full-screen reader for canonical Legal Documents (Privacy Policy, Terms of Service).
 * Rendered with Markdown support, strictly suppressing remote image loading.
 */
@Composable
fun LegalDocumentScreen(
    document: ParsedLegalDocument,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val bottomOverlayPadding = LocalBottomOverlayPadding.current
    val effectiveBottomSpacer = if (bottomOverlayPadding > 0.dp) bottomOverlayPadding + 32.dp else 120.dp

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightNavy)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Top app bar with back navigation and title
        com.arinara.fotara.ui.components.SettingsSubScreenHeader(
            title = document.title,
            subtitle = "Version ${document.version}, effective ${document.effectiveDate}",
            onBackClick = onBackClick,
            modifier = Modifier.padding(horizontal = com.arinara.fotara.ui.components.SettingsSubScreenHeaderDefaults.HorizontalPadding)
        )

        // Scrollable content area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                ReleaseNotesRenderer(
                    markdown = document.body,
                    primaryTextColor = Color.White.copy(alpha = 0.90f),
                    accentColor = Color(0xFF60A5FA),
                    surfaceColor = HomeNearBlack,
                    cardBorder = Color(0xFF26324A),
                    allowImages = false // Never load remote images in legal documents
                )

                // Bottom padding respecting bottom overlay bar
                Spacer(modifier = Modifier.height(effectiveBottomSpacer))
            }
        }
    }
}
