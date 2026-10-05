// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class ScreenHeaderTest {

    @Test
    fun screenHeaderDefaults_layoutMetricsMatchSpecification() {
        assertEquals(18.dp, ScreenHeaderDefaults.HorizontalPadding)
        assertEquals(16.dp, ScreenHeaderDefaults.TopPadding)
        assertEquals(12.dp, ScreenHeaderDefaults.BottomPadding)
        assertEquals(4.dp, ScreenHeaderDefaults.TitleToTaglineGap)
        assertEquals(18.dp, ScreenHeaderDefaults.TaglineSlotHeight)
        assertEquals(12.dp, ScreenHeaderDefaults.HeaderBottomGap)
    }

    @Test
    fun screenHeaderDefaults_actionButtonMetricsMatchSpecification() {
        assertEquals(48.dp, ScreenHeaderDefaults.ActionButtonSize)
        assertEquals(40.dp, ScreenHeaderDefaults.ActionButtonContentSize)
        assertEquals(20.dp, ScreenHeaderDefaults.ActionIconSize)
        assertEquals(12.dp, ScreenHeaderDefaults.ActionButtonSpacing)
        assertTrue(
            "Touch target must be at least 44dp",
            ScreenHeaderDefaults.ActionButtonSize.value >= 44f
        )
    }

    @Test
    fun screenHeaderDefaults_typographyTokensMatchSpecification() {
        assertEquals(32.sp, ScreenHeaderDefaults.TitleFontSize)
        assertEquals(38.sp, ScreenHeaderDefaults.TitleLineHeight)
        assertEquals(FontWeight.Bold, ScreenHeaderDefaults.TitleFontWeight)
        assertEquals((-0.5).sp, ScreenHeaderDefaults.TitleLetterSpacing)

        assertEquals(14.sp, ScreenHeaderDefaults.TaglineFontSize)
        assertEquals(18.sp, ScreenHeaderDefaults.TaglineLineHeight)
        assertEquals(FontWeight.Normal, ScreenHeaderDefaults.TaglineFontWeight)
    }

    @Test
    fun screenHeader_fontScaleLayoutValidation() {
        val fontScales = listOf(0.85f, 1.0f, 1.3f, 1.5f, 2.0f)
        for (scale in fontScales) {
            // Scaled font sp in pixels equivalent at 160dpi (1dp = 1px)
            val titleScaledPx = ScreenHeaderDefaults.TitleFontSize.value * scale
            val titleLineHeightPx = ScreenHeaderDefaults.TitleLineHeight.value * scale
            val taglineScaledPx = ScreenHeaderDefaults.TaglineFontSize.value * scale
            val taglineLineHeightPx = ScreenHeaderDefaults.TaglineLineHeight.value * scale

            // Verify line height is always strictly greater than font size to prevent descender clipping
            assertTrue(
                "Title line height must accommodate title at font scale $scale",
                titleLineHeightPx >= titleScaledPx
            )
            assertTrue(
                "Tagline line height must accommodate tagline at font scale $scale",
                taglineLineHeightPx >= taglineScaledPx
            )
        }
    }

    @Test
    fun folderCardDefaults_buttonGeometryMatchesErgonomicRules() {
        assertEquals(24.dp, FolderCardDefaults.CardCornerRadius)
        assertTrue(
            "Folder card touch target must be at least 44dp",
            FolderCardDefaults.ThreeDotButtonSize.value >= 44f
        )
        assertEquals(20.dp, FolderCardDefaults.ThreeDotIconSize)
        assertEquals(22.dp, FolderCardDefaults.VisualCenterOffset)
    }

    @Test
    fun folderCardDefaults_visualCenterIsWithinCornerArcAcrossDensities() {
        val cornerRadiusDp = FolderCardDefaults.CardCornerRadius.value // 24dp
        val touchTargetDp = FolderCardDefaults.ThreeDotButtonSize.value // 44dp
        val visualCenterDp = FolderCardDefaults.VisualCenterOffset.value // 22dp

        // The top-right rounded corner arc center is at (24dp from right, 24dp from top)
        // Visual center is at 22dp from right, 22dp from top
        val deltaX = visualCenterDp - cornerRadiusDp // 22 - 24 = -2dp
        val deltaY = visualCenterDp - cornerRadiusDp // 22 - 24 = -2dp
        val distanceToArcCenter = sqrt(deltaX * deltaX + deltaY * deltaY) // ~2.83dp

        // The center is inside the corner arc radius boundary (distance < 24dp)
        assertTrue(
            "Visual center should be within corner radius arc",
            distanceToArcCenter < cornerRadiusDp
        )

        // Verify across densities 1.0 (mdpi), 2.0 (xhdpi), 3.0 (xxhdpi), 4.0 (xxxhdpi)
        val densities = listOf(1.0f, 2.0f, 3.0f, 4.0f)
        for (density in densities) {
            val touchTargetPx = touchTargetDp * density
            assertTrue(
                "Touch target in px must be >= 44 * density",
                touchTargetPx >= 44f * density
            )

            val centerPx = visualCenterDp * density
            val arcCenterPx = cornerRadiusDp * density

            val pxDeltaX = centerPx - arcCenterPx
            val pxDeltaY = centerPx - arcCenterPx
            val pxDist = sqrt(pxDeltaX * pxDeltaX + pxDeltaY * pxDeltaY)

            assertTrue(
                "Center in px must stay within arc boundary at density $density",
                pxDist < arcCenterPx
            )
        }
    }

    @Test
    fun folderCardDefaults_zeroCollisionWithFolderTile() {
        // Tile is at start = 14dp, size = 52dp -> ends at 66dp
        val tileEndFromLeftDp = 14f + 52f // 66dp
        // Minimum card width in a 2-column grid on standard 360dp width screen:
        val minCardWidthDp = 150f

        val buttonTouchTargetDp = FolderCardDefaults.ThreeDotButtonSize.value // 44dp
        val buttonStartFromLeftDp = minCardWidthDp - buttonTouchTargetDp // 150 - 44 = 106dp

        val horizontalGap = buttonStartFromLeftDp - tileEndFromLeftDp
        assertTrue(
            "There must be positive separation between tile and three-dot button (gap: ${horizontalGap}dp)",
            horizontalGap > 0f
        )
    }
}
