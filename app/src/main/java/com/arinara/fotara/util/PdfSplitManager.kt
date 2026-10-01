// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.util

/**
 * Pure business logic helper for PDF splitting, Photo Group packaging decisions,
 * and canvas background opacity checks.
 */
object PdfSplitManager {

    /**
     * Threshold rule: If a PDF has 5 or more pages (more than 4), it must be
     * bundled into a Photo Group named after the document. If 4 or fewer,
     * it produces standalone loose photo notes.
     */
    const val GROUP_THRESHOLD_PAGE_COUNT = 5

    fun shouldCreateGroup(pageCount: Int): Boolean {
        return pageCount >= GROUP_THRESHOLD_PAGE_COUNT
    }

    fun getConfirmationDescription(documentName: String, pageCount: Int): String {
        val targetDesc = if (shouldCreateGroup(pageCount)) {
            "a new Photo Group '$documentName' (with Page 1 as cover)"
        } else {
            "$pageCount standalone photo notes"
        }
        return "This action permanently deletes the original PDF file and converts all $pageCount pages into $targetDesc in this folder. This action cannot be undone."
    }

    /**
     * Verifies that a 32-bit ARGB pixel integer represents fully opaque white (0xFFFFFFFF).
     */
    fun isOpaqueWhite(colorInt: Int): Boolean {
        return colorInt == -1 || colorInt == -0x1 || (colorInt and -0x1) == -0x1
    }
}
