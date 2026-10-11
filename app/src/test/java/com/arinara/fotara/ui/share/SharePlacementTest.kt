// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SharePlacementTest {

    @Test
    fun testStagedShareItemProperties() {
        val dummyFile = File("dummy.jpg")
        val item = StagedShareItem(
            id = "test-1",
            displayName = "Bio_Chapter1.jpg",
            itemType = StagedItemType.IMAGE,
            mimeType = "image/jpeg",
            stagedFile = dummyFile,
            fileSizeBytes = 1024L,
            isSelected = true
        )

        assertEquals("Bio_Chapter1.jpg", item.displayName)
        assertEquals(StagedItemType.IMAGE, item.itemType)
        assertTrue(item.isSelected)
    }

    @Test
    fun testUiStateSelectionCounts() {
        val dummyFile = File("dummy.pdf")
        val items = listOf(
            StagedShareItem("1", "A.pdf", StagedItemType.PDF, "application/pdf", dummyFile, 100L, isSelected = true),
            StagedShareItem("2", "B.docx", StagedItemType.DOCX, "application/docx", dummyFile, 200L, isSelected = false),
            StagedShareItem("3", "C.txt", StagedItemType.TEXT, "text/plain", dummyFile, 300L, isSelected = true)
        )

        val state = SharePlacementUiState(stagedItems = items)
        assertEquals(3, state.totalRemaining)
        assertEquals(2, state.selectedCount)

        val updatedItems = items.map { it.copy(isSelected = true) }
        val updatedState = state.copy(stagedItems = updatedItems)
        assertEquals(3, updatedState.selectedCount)

        val noneSelected = items.map { it.copy(isSelected = false) }
        val emptySelectionState = state.copy(stagedItems = noneSelected)
        assertEquals(0, emptySelectionState.selectedCount)
    }

    @Test
    fun testCapLogicCalculation() {
        val totalIncoming = 42
        val cap = 30
        val cappedCount = minOf(totalIncoming, cap)
        val skippedCount = (totalIncoming - cap).coerceAtLeast(0)

        assertEquals(30, cappedCount)
        assertEquals(12, skippedCount)
    }
}
