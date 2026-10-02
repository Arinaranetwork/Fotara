// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui

import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.theme.FolderAccentPalette
import com.arinara.fotara.ui.components.GlowCorner
import com.arinara.fotara.ui.components.computeFolderGlowOrientations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LinkItGlowTest {

    @Test
    fun computeGlow_sameRowAdjacent_facesInward() {
        val f1 = Folder(id = 1L, name = "Matematika Wajib", colorLabel = "#00B4D8", linkGroupId = 100L)
        val f2 = Folder(id = 2L, name = "Fisika", colorLabel = "#00B4D8", linkGroupId = 100L)
        val folders = listOf(f1, f2)

        val glows = computeFolderGlowOrientations(folders, columns = 2)

        // Same row: left (index 0) glows bottom-right, right (index 1) glows bottom-left
        assertEquals(GlowCorner.BottomRight, glows[1L])
        assertEquals(GlowCorner.BottomLeft, glows[2L])
    }

    @Test
    fun computeGlow_acrossRows_facesFacingCorners() {
        // Row 0: dummy (0,0), Lanjut (0,1)
        // Row 1: Wajib (1,0), dummy (1,1)
        val d1 = Folder(id = 10L, name = "Testing", colorLabel = "#00B4D8")
        val fLanjut = Folder(id = 1L, name = "Matematika Lanjut", colorLabel = "#00B4D8", linkGroupId = 200L)
        val fWajib = Folder(id = 2L, name = "Matematika Wajib", colorLabel = "#00B4D8", linkGroupId = 200L)
        val d2 = Folder(id = 20L, name = "Kimia", colorLabel = "#00B4D8")

        val folders = listOf(d1, fLanjut, fWajib, d2)
        val glows = computeFolderGlowOrientations(folders, columns = 2)

        // Upper-right (row 0, col 1) faces lower-left (row 1, col 0)
        assertEquals(GlowCorner.BottomLeft, glows[fLanjut.id])
        assertEquals(GlowCorner.TopRight, glows[fWajib.id])
    }

    @Test
    fun computeGlow_partnerNotRendered_suppressesGlow() {
        // If partner is filtered out or missing from the list, glow is suppressed
        val f1 = Folder(id = 1L, name = "Matematika Wajib", colorLabel = "#00B4D8", linkGroupId = 300L)
        val folders = listOf(f1)

        val glows = computeFolderGlowOrientations(folders, columns = 2)
        assertNull("Glow must be suppressed if partner is not in the list", glows[f1.id])
    }

    @Test
    fun folderAccentPalette_resolvesAccentsFromKnownNames() {
        val testingAccent = FolderAccentPalette.fromHexOrDefault("#00B4D8", 1L, "Testing")
        assertEquals("blue", testingAccent.key)

        val lanjutAccent = FolderAccentPalette.fromHexOrDefault("#00B4D8", 2L, "Matematika Lanjut")
        assertEquals("brown", lanjutAccent.key)

        val wajibAccent = FolderAccentPalette.fromHexOrDefault("#00B4D8", 3L, "Matematika Wajib")
        assertEquals("purple", wajibAccent.key)

        val fisikaAccent = FolderAccentPalette.fromHexOrDefault("#00B4D8", 4L, "Fisika")
        assertEquals("green", fisikaAccent.key)

        val kimiaAccent = FolderAccentPalette.fromHexOrDefault("#00B4D8", 5L, "Kimia")
        assertEquals("red", kimiaAccent.key)

        val sejarahAccent = FolderAccentPalette.fromHexOrDefault("#00B4D8", 6L, "Sejarah")
        assertEquals("slate", sejarahAccent.key)
    }

    @Test
    fun folderAccentPalette_fallbackIsDeterministic() {
        val a1 = FolderAccentPalette.fromHexOrDefault(null, 42L, "Custom Subject")
        val a2 = FolderAccentPalette.fromHexOrDefault(null, 42L, "Custom Subject")
        assertEquals(a1.key, a2.key)
        assertNotNull(a1.tileFill)
        assertNotNull(a1.glowColor)
    }
}
