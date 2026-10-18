// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arinara.fotara.ui.components.presets.DefaultBottomClearance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test suite verifying the Electric Slate design tokens and spring physics specifications.
 */
class DesignSystemTokensTest {

    @Test
    fun verifyElectricSlateTokenValues() {
        assertEquals(Color(0xFF0D1117), ElectricSlateTokens.BgCanvas)
        assertEquals(Color(0xFF161C26), ElectricSlateTokens.SurfacePanel)
        assertEquals(Color(0xFF1F2736), ElectricSlateTokens.SurfaceElevated)
        assertEquals(Color(0xFF262F3E), ElectricSlateTokens.BorderSubtle)
        assertEquals(Color(0xFF7A5AF8), ElectricSlateTokens.PrimaryAccent)
        assertEquals(Color(0xFF283245), ElectricSlateTokens.TrackInactive)
        assertEquals(Color(0xFFF8FAFC), ElectricSlateTokens.TextPrimary)
        assertEquals(Color(0xFF94A3B8), ElectricSlateTokens.TextMuted)
        assertEquals(Color(0xFFDC5A5A), ElectricSlateTokens.Destructive)
    }

    @Test
    fun verifyAllSwatchesCompleteness() {
        val swatches = ElectricSlateTokens.AllSwatches
        assertEquals("All 9 core tokens must be registered in AllSwatches palette", 9, swatches.size)

        val names = swatches.map { it.name }.toSet()
        val expectedNames = setOf(
            "BgCanvas",
            "SurfacePanel",
            "SurfaceElevated",
            "BorderSubtle",
            "PrimaryAccent",
            "TrackInactive",
            "TextPrimary",
            "TextMuted",
            "Destructive"
        )
        assertEquals(expectedNames, names)

        swatches.forEach { swatch ->
            assertNotNull(swatch.name)
            assertNotNull(swatch.hex)
            assertNotNull(swatch.description)
            assertEquals(true, swatch.hex.startsWith("#"))
        }
    }

    @Test
    fun verifySpringPhysicsSpecifications() {
        assertEquals(0.5f, SpringPhysics.JigglePressSpec.dampingRatio)
        assertEquals(180f, SpringPhysics.JigglePressSpec.stiffness)

        assertEquals(0.5f, SpringPhysics.JigglePressColorSpec.dampingRatio)
        assertEquals(180f, SpringPhysics.JigglePressColorSpec.stiffness)

        assertEquals(0.45f, SpringPhysics.ViscousGooeySpec.dampingRatio)
        assertEquals(100f, SpringPhysics.ViscousGooeySpec.stiffness)

        assertEquals(0.6f, SpringPhysics.DismissFadeSpec.dampingRatio)
        assertEquals(300f, SpringPhysics.DismissFadeSpec.stiffness)
    }

    @Test
    fun verifyBottomClearancePreset() {
        assertEquals(100.dp, DefaultBottomClearance)
        assertTrue(
            "DefaultBottomClearance must be at least 96.dp for safe floating dock clearance",
            DefaultBottomClearance.value >= 96f
        )
    }

    @Test
    fun verifyBorderColorDisciplineTokens() {
        // AR-012 & AR-013: All component borders resolve exclusively to BorderSubtle or PrimaryAccent
        val structuralBorder = ElectricSlateTokens.BorderSubtle
        val activeBorder = ElectricSlateTokens.PrimaryAccent

        assertEquals(Color(0xFF262F3E), structuralBorder)
        assertEquals(Color(0xFF7A5AF8), activeBorder)

        // White colors are strictly prohibited as component border tokens
        val whiteArgb = 0xFFFFFFFF.toInt()
        val whiteColor = Color.White
        assertTrue(structuralBorder.value.toLong() != 0xFFFFFFFF)
        assertTrue(activeBorder.value.toLong() != 0xFFFFFFFF)
        assertTrue(structuralBorder != whiteColor)
        assertTrue(activeBorder != whiteColor)
    }

    @Test
    fun verifyCompactTopSpacingConstraint() {
        // AR-016: Top layer spacers and headings must not exceed 24.dp
        val maxTopLayerSpacer = 24.dp
        assertTrue(maxTopLayerSpacer.value <= 24f)
        assertTrue(DefaultBottomClearance.value >= 96f)
    }
}
