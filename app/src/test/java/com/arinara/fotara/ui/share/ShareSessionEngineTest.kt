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

class ShareSessionEngineTest {

    private fun createDummyItem(
        id: String,
        type: StagedItemType,
        selected: Boolean = true,
        error: String? = null
    ): StagedShareItem {
        return StagedShareItem(
            id = id,
            displayName = "Item_$id",
            itemType = type,
            mimeType = when (type) {
                StagedItemType.IMAGE -> "image/jpeg"
                StagedItemType.PDF -> "application/pdf"
                StagedItemType.DOCX -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                StagedItemType.TEXT -> "text/plain"
            },
            stagedFile = File("/dummy/stage_$id"),
            fileSizeBytes = 1024L,
            textBody = if (type == StagedItemType.TEXT) "Sample body" else null,
            thumbnailPath = null,
            isSelected = selected,
            errorMessage = error
        )
    }

    @Test
    fun `single item produces exact Place Here 1 label and hides side panel`() {
        val item = createDummyItem("1", StagedItemType.IMAGE)
        val items = listOf(item)

        val count = ShareSessionEngine.computePlaceableCount(items, isGroupDestination = false)
        val label = ShareSessionEngine.formatPlaceButtonLabel(count)
        val showPanel = ShareSessionEngine.shouldShowSidePanel(items.size)

        assertEquals(1, count)
        assertEquals("Place Here (1)", label)
        assertFalse(showPanel)
    }

    @Test
    fun `multiple items produce exact Place Here N label and shows side panel`() {
        val items = listOf(
            createDummyItem("1", StagedItemType.IMAGE),
            createDummyItem("2", StagedItemType.PDF),
            createDummyItem("3", StagedItemType.DOCX)
        )

        val count = ShareSessionEngine.computePlaceableCount(items, isGroupDestination = false)
        val label = ShareSessionEngine.formatPlaceButtonLabel(count)
        val showPanel = ShareSessionEngine.shouldShowSidePanel(items.size)

        assertEquals(3, count)
        assertEquals("Place Here (3)", label)
        assertTrue(showPanel)
    }

    @Test
    fun `capping strictly enforces 30 items limit`() {
        // Exactly 30 items
        val (take30, skip0) = ShareSessionEngine.computeCapBounds(0, 30)
        assertEquals(30, take30)
        assertEquals(0, skip0)

        // 31 items
        val (take30b, skip1) = ShareSessionEngine.computeCapBounds(0, 31)
        assertEquals(30, take30b)
        assertEquals(1, skip1)

        // Incremental share: 20 existing, 15 incoming -> take 10, skip 5
        val (take10, skip5) = ShareSessionEngine.computeCapBounds(20, 15)
        assertEquals(10, take10)
        assertEquals(5, skip5)

        // Full session: 30 existing, 5 incoming -> take 0, skip 5
        val (take0, skip5b) = ShareSessionEngine.computeCapBounds(30, 5)
        assertEquals(0, take0)
        assertEquals(5, skip5b)
    }

    @Test
    fun `error items are excluded from count and cannot be placed`() {
        val validImage = createDummyItem("1", StagedItemType.IMAGE)
        val corruptPdf = createDummyItem("2", StagedItemType.PDF, error = "Corrupt PDF")
        val items = listOf(validImage, corruptPdf)

        val count = ShareSessionEngine.computePlaceableCount(items, isGroupDestination = false)
        assertEquals(1, count)
        assertEquals("Place Here (1)", ShareSessionEngine.formatPlaceButtonLabel(count))
        assertTrue(corruptPdf.isError)
        assertFalse(corruptPdf.isPlaceable)
    }

    @Test
    fun `group destination only accepts images and excludes other types`() {
        val imageItem = createDummyItem("1", StagedItemType.IMAGE)
        val pdfItem = createDummyItem("2", StagedItemType.PDF)
        val docxItem = createDummyItem("3", StagedItemType.DOCX)
        val textItem = createDummyItem("4", StagedItemType.TEXT)
        val items = listOf(imageItem, pdfItem, docxItem, textItem)

        // Folder destination accepts all
        val folderCount = ShareSessionEngine.computePlaceableCount(items, isGroupDestination = false)
        assertEquals(4, folderCount)

        // Group destination only accepts images
        val groupCount = ShareSessionEngine.computePlaceableCount(items, isGroupDestination = true)
        assertEquals(1, groupCount)
        assertEquals("Place Here (1)", ShareSessionEngine.formatPlaceButtonLabel(groupCount))

        assertFalse(pdfItem.canPlaceInDestination(isGroupDestination = true))
        assertEquals("Photo Groups can only contain images", pdfItem.getIneligibilityReason(isGroupDestination = true))
    }

    @Test
    fun `deriveTextTitle respects priority order`() {
        // 1. Subject present
        val title1 = ShareSessionEngine.deriveTextTitle(
            subject = "History Homework Week 4",
            fileName = "notes.txt",
            textBody = "First line of notes"
        )
        assertEquals("History Homework Week 4", title1)

        // 2. No subject, valid filename
        val title2 = ShareSessionEngine.deriveTextTitle(
            subject = null,
            fileName = "Biology_Lab_Report.md",
            textBody = "Introduction to Cells"
        )
        assertEquals("Biology_Lab_Report", title2)

        // 3. No subject, generic filename, first line present
        val title3 = ShareSessionEngine.deriveTextTitle(
            subject = "",
            fileName = "Shared_Item",
            textBody = "Summary of Chapter 3 Quantum Mechanics\nAdditional details below..."
        )
        assertEquals("Summary of Chapter 3 Quantum Mechanics", title3)

        // 4. Blank everything -> fallback
        val title4 = ShareSessionEngine.deriveTextTitle(
            subject = null,
            fileName = null,
            textBody = "   \n\n  "
        )
        assertEquals("Shared Note", title4)
    }

    @Test
    fun `partial placement retains remaining items`() {
        val item1 = createDummyItem("1", StagedItemType.IMAGE)
        val item2 = createDummyItem("2", StagedItemType.PDF)
        val item3 = createDummyItem("3", StagedItemType.TEXT)
        val allItems = listOf(item1, item2, item3)

        // Place only item1
        val remaining = ShareSessionEngine.removePlacedItems(allItems, setOf("1"))

        assertEquals(2, remaining.size)
        assertEquals("2", remaining[0].id)
        assertEquals("3", remaining[1].id)
    }

    @Test
    fun `select all and deselect all ignore error items`() {
        val item1 = createDummyItem("1", StagedItemType.IMAGE, selected = true)
        val item2 = createDummyItem("2", StagedItemType.PDF, selected = true, error = "Corrupt")
        val items = listOf(item1, item2)

        val deselected = ShareSessionEngine.setAllSelected(items, false)
        assertFalse(deselected[0].isSelected)
        assertFalse(deselected[1].isSelected)

        val selected = ShareSessionEngine.setAllSelected(deselected, true)
        assertTrue(selected[0].isSelected)
        assertFalse(selected[1].isSelected) // Error item cannot be selected
    }
}
