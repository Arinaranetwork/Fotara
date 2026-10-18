// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.document

import com.arinara.fotara.util.PdfSplitManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfSplitManagerTest {

    @Test
    fun testGroupingDecisionForThreePages() {
        // 3 pages is <= 4, must produce standalone notes (no group)
        assertFalse(PdfSplitManager.shouldCreateGroup(3))
    }

    @Test
    fun testGroupingDecisionForFourPages() {
        // 4 pages is <= 4, must produce standalone notes (no group)
        assertFalse(PdfSplitManager.shouldCreateGroup(4))
    }

    @Test
    fun testGroupingDecisionForFivePages() {
        // 5 pages is >= 5, must bundle into a new Photo Group
        assertTrue(PdfSplitManager.shouldCreateGroup(5))
    }

    @Test
    fun testGroupingDecisionForOneHundredPages() {
        // 100 pages is >= 5, must bundle into a new Photo Group
        assertTrue(PdfSplitManager.shouldCreateGroup(100))
    }

    @Test
    fun testConfirmationDescriptionForSmallDocument() {
        val desc3 = PdfSplitManager.getConfirmationDescription("LectureNotes.pdf", 3)
        assertTrue(desc3.contains("3 standalone photo notes"))
        assertFalse(desc3.contains("Photo Group"))

        val desc4 = PdfSplitManager.getConfirmationDescription("Invoice.pdf", 4)
        assertTrue(desc4.contains("4 standalone photo notes"))
        assertFalse(desc4.contains("Photo Group"))
    }

    @Test
    fun testConfirmationDescriptionForGroupDocument() {
        val desc5 = PdfSplitManager.getConfirmationDescription("TaxReport.pdf", 5)
        assertTrue(desc5.contains("Photo Group 'TaxReport.pdf'"))
        assertTrue(desc5.contains("Page 1 as cover"))

        val desc100 = PdfSplitManager.getConfirmationDescription("Contract.pdf", 100)
        assertTrue(desc100.contains("Photo Group 'Contract.pdf'"))
        assertTrue(desc100.contains("Page 1 as cover"))
    }

    @Test
    fun testOpaqueWhitePixelValidation() {
        // 0xFFFFFFFF represented as standard 32-bit integer (-1)
        val whiteInt = -1
        assertTrue(PdfSplitManager.isOpaqueWhite(whiteInt))

        // Transparent pixel (alpha 0)
        val transparentPixel = 0x00FFFFFF
        assertFalse(PdfSplitManager.isOpaqueWhite(transparentPixel))

        // Semi-transparent white
        val semiWhite = 0x80FFFFFF.toInt()
        assertFalse(PdfSplitManager.isOpaqueWhite(semiWhite))

        // Pure black opaque
        val blackPixel = 0xFF000000.toInt()
        assertFalse(PdfSplitManager.isOpaqueWhite(blackPixel))
    }
}
