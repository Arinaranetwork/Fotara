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
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkItGlowTest {

    @Test
    fun computeGlow_sameRowAdjacent_facesInward() {
        val f1 = Folder(id = 1L, name = "Matematika Wajib", colorLabel = "#00B4D8", linkGroupId = 100L)
        val f2 = Folder(id = 2L, name = "Fisika", colorLabel = "#00B4D8", linkGroupId = 100L)
        val folders = listOf(f1, f2)

        val glows = computeFolderGlowOrientations(folders, columns = 2)

        // Same row: left (col 0) glows right side, right (col 1) glows left side
        assertEquals(GlowCorner.RightEdge, glows[1L])
        assertEquals(GlowCorner.LeftEdge, glows[2L])
    }

    @Test
    fun computeGlow_verticalNeighbors_facesTopAndBottom() {
        // Col 0: Top item (row 0), Bottom item (row 1)
        val items = listOf(
            101L to 500L, // row 0, col 0
            999L to null, // row 0, col 1
            102L to 500L, // row 1, col 0
            888L to null  // row 1, col 1
        )
        val glows = com.arinara.fotara.ui.components.computeGridFacingGlowCorners(items, columns = 2)

        // Top card glows bottom side, bottom card glows top side
        assertEquals(GlowCorner.BottomEdge, glows[101L])
        assertEquals(GlowCorner.TopEdge, glows[102L])
    }

    @Test
    fun computeGlow_gridDensities_supports2_3_4Columns() {
        // Test 3 columns:
        // Row 0: [item1 (0,0), item2 (0,1), item3 (0,2)]
        // Item 1 & 2 linked in row 0
        val items3Col = listOf(
            1L to 10L, // (0,0)
            2L to 10L, // (0,1)
            3L to null // (0,2)
        )
        val glows3Col = com.arinara.fotara.ui.components.computeGridFacingGlowCorners(items3Col, columns = 3)
        assertEquals(GlowCorner.RightEdge, glows3Col[1L])
        assertEquals(GlowCorner.LeftEdge, glows3Col[2L])

        // Test 4 columns:
        // Row 0: [1, 2, 3, 4]
        // Row 1: [5, 6, 7, 8]
        // Vertical pair at col 2: item 3 (0,2) and item 7 (1,2)
        val items4Col = listOf(
            1L to null, 2L to null, 3L to 20L, 4L to null,
            5L to null, 6L to null, 7L to 20L, 8L to null
        )
        val glows4Col = com.arinara.fotara.ui.components.computeGridFacingGlowCorners(items4Col, columns = 4)
        assertEquals(GlowCorner.BottomEdge, glows4Col[3L])
        assertEquals(GlowCorner.TopEdge, glows4Col[7L])
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

    @Test
    fun computeGridFacingGlowCorners_docxAndPdfSameRow_glowsFacingRightAndLeft() {
        // Document cards (e.g. DOCX ID 10, PDF ID 11) sharing linkGroupId 888 in same row (columns = 2)
        val items = listOf(
            10L to 888L, // left card: DOCX
            11L to 888L  // right card: PDF
        )
        val glows = com.arinara.fotara.ui.components.computeGridFacingGlowCorners(items, columns = 2)

        // Left card glows its right edge; right card glows its left edge facing partner
        assertEquals(GlowCorner.RightEdge, glows[10L])
        assertEquals(GlowCorner.LeftEdge, glows[11L])
    }

    @Test
    fun computeGridFacingGlowCorners_docxAndPdfVertical_glowsFacingBottomAndTop() {
        // Vertical neighbors in column 0: DOCX at (0,0), PDF at (1,0)
        val items = listOf(
            10L to 888L, 99L to null, // row 0
            11L to 888L, 98L to null  // row 1
        )
        val glows = com.arinara.fotara.ui.components.computeGridFacingGlowCorners(items, columns = 2)

        assertEquals(GlowCorner.BottomEdge, glows[10L])
        assertEquals(GlowCorner.TopEdge, glows[11L])
    }
    @Test
    fun computeGridGlowAnchors_cluster3InL_allFourOrientationsConvergeOnCenter() {
        val groupId = 777L

        // Orientation 1 (User screenshot): TL (0,0), TR (0,1), BL (1,0)
        val itemsL1 = listOf(
            1L to groupId, 2L to groupId,
            3L to groupId, 4L to null
        )
        val anchorsL1 = com.arinara.fotara.ui.components.computeGridGlowAnchors(itemsL1, columns = 2)
        assertEquals(setOf(com.arinara.fotara.ui.components.GlowAnchor.BOTTOM_RIGHT), anchorsL1[1L])
        assertEquals(setOf(com.arinara.fotara.ui.components.GlowAnchor.BOTTOM_LEFT), anchorsL1[2L])
        assertEquals(setOf(com.arinara.fotara.ui.components.GlowAnchor.TOP_RIGHT), anchorsL1[3L])
        assertNull(anchorsL1[4L])

        // Orientation 2: TL (0,0), TR (0,1), BR (1,1)
        val itemsL2 = listOf(
            1L to groupId, 2L to groupId,
            3L to null,    4L to groupId
        )
        val anchorsL2 = com.arinara.fotara.ui.components.computeGridGlowAnchors(itemsL2, columns = 2)
        assertEquals(setOf(com.arinara.fotara.ui.components.GlowAnchor.BOTTOM_RIGHT), anchorsL2[1L])
        assertEquals(setOf(com.arinara.fotara.ui.components.GlowAnchor.BOTTOM_LEFT), anchorsL2[2L])
        assertEquals(setOf(com.arinara.fotara.ui.components.GlowAnchor.TOP_LEFT), anchorsL2[4L])

        // Orientation 3: TL (0,0), BL (1,0), BR (1,1)
        val itemsL3 = listOf(
            1L to groupId, 2L to null,
            3L to groupId, 4L to groupId
        )
        val anchorsL3 = com.arinara.fotara.ui.components.computeGridGlowAnchors(itemsL3, columns = 2)
        assertEquals(setOf(com.arinara.fotara.ui.components.GlowAnchor.BOTTOM_RIGHT), anchorsL3[1L])
        assertEquals(setOf(com.arinara.fotara.ui.components.GlowAnchor.TOP_RIGHT), anchorsL3[3L])
        assertEquals(setOf(com.arinara.fotara.ui.components.GlowAnchor.TOP_LEFT), anchorsL3[4L])

        // Orientation 4: TR (0,1), BL (1,0), BR (1,1)
        val itemsL4 = listOf(
            1L to null,    2L to groupId,
            3L to groupId, 4L to groupId
        )
        val anchorsL4 = com.arinara.fotara.ui.components.computeGridGlowAnchors(itemsL4, columns = 2)
        assertEquals(setOf(com.arinara.fotara.ui.components.GlowAnchor.BOTTOM_LEFT), anchorsL4[2L])
        assertEquals(setOf(com.arinara.fotara.ui.components.GlowAnchor.TOP_RIGHT), anchorsL4[3L])
        assertEquals(setOf(com.arinara.fotara.ui.components.GlowAnchor.TOP_LEFT), anchorsL4[4L])
    }

    @Test
    fun computeGridGlowAnchors_full2x2Cluster_allFourCornersMeet() {
        val groupId = 999L
        val items = listOf(
            1L to groupId, 2L to groupId,
            3L to groupId, 4L to groupId
        )
        val anchors = com.arinara.fotara.ui.components.computeGridGlowAnchors(items, columns = 2)

        assertEquals(setOf(com.arinara.fotara.ui.components.GlowAnchor.BOTTOM_RIGHT), anchors[1L])
        assertEquals(setOf(com.arinara.fotara.ui.components.GlowAnchor.BOTTOM_LEFT), anchors[2L])
        assertEquals(setOf(com.arinara.fotara.ui.components.GlowAnchor.TOP_RIGHT), anchors[3L])
        assertEquals(setOf(com.arinara.fotara.ui.components.GlowAnchor.TOP_LEFT), anchors[4L])
    }

    @Test
    fun computeGridGlowAnchors_threeInARow_retainsPairwiseSideGlows() {
        val groupId = 555L
        // 3 items in a single row across 3 columns: (0,0), (0,1), (0,2)
        val items = listOf(
            1L to groupId, 2L to groupId, 3L to groupId
        )
        val anchors = com.arinara.fotara.ui.components.computeGridGlowAnchors(items, columns = 3)

        assertEquals(setOf(com.arinara.fotara.ui.components.GlowAnchor.RIGHT), anchors[1L])
        assertEquals(setOf(com.arinara.fotara.ui.components.GlowAnchor.LEFT, com.arinara.fotara.ui.components.GlowAnchor.RIGHT), anchors[2L])
        assertEquals(setOf(com.arinara.fotara.ui.components.GlowAnchor.LEFT), anchors[3L])
    }

    @Test
    fun computeGridGlowAnchors_fourInAColumn_retainsPairwiseSideGlows() {
        val groupId = 666L
        // 4 items in col 0 across 4 rows (columns = 2)
        val items = listOf(
            1L to groupId, 100L to null,
            2L to groupId, 101L to null,
            3L to groupId, 102L to null,
            4L to groupId, 103L to null
        )
        val anchors = com.arinara.fotara.ui.components.computeGridGlowAnchors(items, columns = 2)

        assertEquals(setOf(com.arinara.fotara.ui.components.GlowAnchor.BOTTOM), anchors[1L])
        assertEquals(setOf(com.arinara.fotara.ui.components.GlowAnchor.TOP, com.arinara.fotara.ui.components.GlowAnchor.BOTTOM), anchors[2L])
        assertEquals(setOf(com.arinara.fotara.ui.components.GlowAnchor.TOP, com.arinara.fotara.ui.components.GlowAnchor.BOTTOM), anchors[3L])
        assertEquals(setOf(com.arinara.fotara.ui.components.GlowAnchor.TOP), anchors[4L])
    }

    @Test
    fun computeGridGlowAnchors_fiveOrSixMembersAcrossTwoRows_supportsMultipleAnchors() {
        val groupId = 444L
        // Row 0: [1(0,0), 2(0,1), 3(0,2)]
        // Row 1: [4(1,0), 5(1,1), 6(1,2)]
        // Columns = 3.
        // Left 2x2 has [1, 2, 4, 5], Right 2x2 has [2, 3, 5, 6].
        val items = listOf(
            1L to groupId, 2L to groupId, 3L to groupId,
            4L to groupId, 5L to groupId, 6L to groupId
        )
        val anchors = com.arinara.fotara.ui.components.computeGridGlowAnchors(items, columns = 3)

        // Card 1 at (0,0) touches left 2x2 center -> BOTTOM_RIGHT
        assertEquals(setOf(com.arinara.fotara.ui.components.GlowAnchor.BOTTOM_RIGHT), anchors[1L])
        // Card 2 at (0,1) is shared: touches left 2x2 (BOTTOM_LEFT) and right 2x2 (BOTTOM_RIGHT)
        assertEquals(setOf(com.arinara.fotara.ui.components.GlowAnchor.BOTTOM_LEFT, com.arinara.fotara.ui.components.GlowAnchor.BOTTOM_RIGHT), anchors[2L])
        // Card 3 at (0,2) touches right 2x2 center -> BOTTOM_LEFT
        assertEquals(setOf(com.arinara.fotara.ui.components.GlowAnchor.BOTTOM_LEFT), anchors[3L])
        // Card 4 at (1,0) touches left 2x2 center -> TOP_RIGHT
        assertEquals(setOf(com.arinara.fotara.ui.components.GlowAnchor.TOP_RIGHT), anchors[4L])
        // Card 5 at (1,1) touches left 2x2 (TOP_LEFT) and right 2x2 (TOP_RIGHT)
        assertEquals(setOf(com.arinara.fotara.ui.components.GlowAnchor.TOP_LEFT, com.arinara.fotara.ui.components.GlowAnchor.TOP_RIGHT), anchors[5L])
        // Card 6 at (1,2) touches right 2x2 center -> TOP_LEFT
        assertEquals(setOf(com.arinara.fotara.ui.components.GlowAnchor.TOP_LEFT), anchors[6L])
    }

    @Test
    fun computeGridGlowAnchors_differentGroupsSideBySide_doNotCrossLink() {
        val groupA = 1001L
        val groupB = 2002L
        // Row 0: [card 1 (group A), card 2 (group B)]
        val items = listOf(
            1L to groupA,
            2L to groupB
        )
        val anchors = com.arinara.fotara.ui.components.computeGridGlowAnchors(items, columns = 2)
        // Neither card has partners in its own group -> no glow
        assertNull(anchors[1L])
        assertNull(anchors[2L])
    }

    @Test
    fun computeGridGlowAnchors_afterReordering_updatesDeterministically() {
        val groupId = 888L
        // Initial order: (0,0) and (0,1) horizontal
        val itemsInitial = listOf(1L to groupId, 2L to groupId)
        val anchorsInitial = com.arinara.fotara.ui.components.computeGridGlowAnchors(itemsInitial, columns = 2)
        assertEquals(setOf(com.arinara.fotara.ui.components.GlowAnchor.RIGHT), anchorsInitial[1L])
        assertEquals(setOf(com.arinara.fotara.ui.components.GlowAnchor.LEFT), anchorsInitial[2L])

        // After reordering: 2L is moved to (0,0), 1L moved to (1,0) -> vertical pair
        val itemsReordered = listOf(
            2L to groupId, 999L to null,
            1L to groupId, 998L to null
        )
        val anchorsReordered = com.arinara.fotara.ui.components.computeGridGlowAnchors(itemsReordered, columns = 2)
        assertEquals(setOf(com.arinara.fotara.ui.components.GlowAnchor.BOTTOM), anchorsReordered[2L])
        assertEquals(setOf(com.arinara.fotara.ui.components.GlowAnchor.TOP), anchorsReordered[1L])
    }

    @Test
    fun cornerAnchors_reachCardComposables_wiringTest() {
        // Wiring test verifying that folder lists and note lists compute GlowAnchor sets
        // that match the card composable contracts (FolderCard, DetailPhotoCard, DetailDocumentCard, etc.)
        val f1 = Folder(id = 1L, name = "Test 1", colorLabel = "#00B4D8", linkGroupId = 555L)
        val f2 = Folder(id = 2L, name = "Test 2", colorLabel = "#00B4D8", linkGroupId = 555L)
        val f3 = Folder(id = 3L, name = "testing", colorLabel = "#00B4D8", linkGroupId = 555L)
        val dummy = Folder(id = 4L, name = "Other", colorLabel = "#00B4D8")

        val folderList = listOf(f1, f2, f3, dummy)
        val folderAnchors = com.arinara.fotara.ui.components.computeFolderGlowAnchors(folderList, columns = 2)

        // Cards consume glowAnchors = folderGlowAnchors[folder.id] ?: emptySet()
        val card1Anchors = folderAnchors[f1.id] ?: emptySet()
        val card2Anchors = folderAnchors[f2.id] ?: emptySet()
        val card3Anchors = folderAnchors[f3.id] ?: emptySet()
        val card4Anchors = folderAnchors[dummy.id] ?: emptySet()

        assertEquals(setOf(com.arinara.fotara.ui.components.GlowAnchor.BOTTOM_RIGHT), card1Anchors)
        assertEquals(setOf(com.arinara.fotara.ui.components.GlowAnchor.BOTTOM_LEFT), card2Anchors)
        assertEquals(setOf(com.arinara.fotara.ui.components.GlowAnchor.TOP_RIGHT), card3Anchors)
        assertTrue(card4Anchors.isEmpty())

        // Legacy corner mapping compatibility
        val legacyGlows = computeFolderGlowOrientations(folderList, columns = 2)
        assertEquals(GlowCorner.BottomRight, legacyGlows[f1.id])
        assertEquals(GlowCorner.BottomLeft, legacyGlows[f2.id])
        assertEquals(GlowCorner.TopRight, legacyGlows[f3.id])
        assertNull(legacyGlows[dummy.id])
    }
}


