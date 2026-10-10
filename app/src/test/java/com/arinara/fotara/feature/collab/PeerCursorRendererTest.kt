// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.collab

import androidx.compose.ui.graphics.Color
import com.arinara.fotara.feature.collab.model.CollabPeer
import com.arinara.fotara.feature.collab.render.PeerCursorRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests covering peer cursor math calculations, pill bounds clipping, and color parsing.
 */
class PeerCursorRendererTest {

    @Test
    fun testPeerActiveEvaluation() {
        val now = 1700000000000L
        val activePeer = CollabPeer(
            peerId = "p1",
            userTag = "Alice",
            colorHex = "#2563EB",
            lastActiveMs = now - 5000L
        )
        val inactivePeer = CollabPeer(
            peerId = "p2",
            userTag = "Bob",
            colorHex = "#EFE8DA",
            lastActiveMs = now - 20000L
        )

        assertTrue(PeerCursorRenderer.isPeerActive(activePeer, nowMs = now, activeWindowMs = 15000L))
        assertFalse(PeerCursorRenderer.isPeerActive(inactivePeer, nowMs = now, activeWindowMs = 15000L))
    }

    @Test
    fun testColorHexParsing() {
        // Standard 6-digit hex
        val blue = PeerCursorRenderer.parseColor("#2563EB")
        assertNotNull(blue)

        // Accent hex
        val cream = PeerCursorRenderer.parseColor("#EFE8DA")
        assertNotNull(cream)

        // 8-digit ARGB hex
        val alphaColor = PeerCursorRenderer.parseColor("#802563EB")
        assertNotNull(alphaColor)

        // 3-digit shorthand
        val shortColor = PeerCursorRenderer.parseColor("#F00")
        assertNotNull(shortColor)

        // Invalid fallback
        val fallback = Color(0xFF2563EB)
        val resultNull = PeerCursorRenderer.parseColor(null, fallback)
        assertEquals(fallback, resultNull)

        val resultInvalid = PeerCursorRenderer.parseColor("not_a_color", fallback)
        assertEquals(fallback, resultInvalid)
    }

    @Test
    fun testCalculatePillBoundsInterior() {
        val pill = PeerCursorRenderer.calculatePillBounds(
            cursorX = 100f,
            cursorY = 100f,
            tagText = "Alice",
            charWidth = 8f,
            pillHeight = 24f,
            horizontalPadding = 16f,
            viewportWidth = 1000f,
            viewportHeight = 1000f,
            pointerOffset = 12f
        )

        // Alice is 5 chars -> estimatedTextWidth = 40, pillWidth = 56
        assertEquals(112f, pill.left, 0.01f)
        assertEquals(112f, pill.top, 0.01f)
        assertEquals(168f, pill.right, 0.01f)
        assertEquals(136f, pill.bottom, 0.01f)
        assertEquals(56f, pill.width, 0.01f)
        assertEquals(24f, pill.height, 0.01f)
    }

    @Test
    fun testCalculatePillBoundsRightEdgeFlipping() {
        val viewportWidth = 500f
        val pill = PeerCursorRenderer.calculatePillBounds(
            cursorX = 480f,
            cursorY = 100f,
            tagText = "Collaborator",
            charWidth = 8f,
            pillHeight = 24f,
            horizontalPadding = 16f,
            viewportWidth = viewportWidth,
            viewportHeight = 1000f,
            pointerOffset = 12f
        )

        // Collaborator (12 chars * 8 + 16 = 112 width).
        // 480 + 12 + 112 = 604 > 500 -> must flip left
        // targetLeft = 480 - 12 - 112 = 356
        assertEquals(356f, pill.left, 0.01f)
        assertTrue(pill.right <= viewportWidth)
    }

    @Test
    fun testCalculatePillBoundsBottomEdgeFlipping() {
        val viewportHeight = 800f
        val pill = PeerCursorRenderer.calculatePillBounds(
            cursorX = 100f,
            cursorY = 790f,
            tagText = "Bob",
            charWidth = 8f,
            pillHeight = 24f,
            horizontalPadding = 16f,
            viewportWidth = 1000f,
            viewportHeight = viewportHeight,
            pointerOffset = 12f
        )

        // 790 + 12 + 24 = 826 > 800 -> must flip above
        // targetTop = 790 - 12 - 24 = 754
        assertEquals(754f, pill.top, 0.01f)
        assertTrue(pill.bottom <= viewportHeight)
    }

    @Test
    fun testBuildPointerPathCreation() {
        val path = PeerCursorRenderer.buildPointerPath(50f, 50f, scale = 1.0f)
        assertNotNull(path)
    }
}
