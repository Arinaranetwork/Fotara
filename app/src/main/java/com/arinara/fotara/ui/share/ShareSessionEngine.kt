// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.share

/**
 * Pure Kotlin engine for managing share session state, limits, eligibility, and title math.
 * Independent of Android framework dependencies to allow rigorous unit testing.
 */
object ShareSessionEngine {

    const val MAX_SHARE_ITEMS = 30

    /**
     * Calculates the number of items to take and skip given incoming item count and existing items.
     * Enforces the hard 30-item cap.
     */
    fun computeCapBounds(
        existingCount: Int,
        newIncomingCount: Int,
        maxCap: Int = MAX_SHARE_ITEMS
    ): Pair<Int, Int> {
        val total = existingCount + newIncomingCount
        val allowedTotal = minOf(total, maxCap)
        val canAdd = maxOf(0, allowedTotal - existingCount)
        val skipped = maxOf(0, newIncomingCount - canAdd)
        return Pair(canAdd, skipped)
    }

    /**
     * Resolves the title of a shared text note according to precedence:
     * 1. Shared Subject (if not blank)
     * 2. File Name (if not blank and not generic)
     * 3. First non-blank line of text body (trimmed, capped at 40 chars)
     * 4. Fallback: "Shared Note"
     */
    fun deriveTextTitle(
        subject: String?,
        fileName: String?,
        textBody: String?
    ): String {
        val cleanSubject = subject?.trim()
        if (!cleanSubject.isNullOrBlank()) {
            return cleanSubject
        }

        val cleanFile = fileName?.trim()?.removeSuffix(".txt")?.removeSuffix(".md")
        if (!cleanFile.isNullOrBlank() &&
            cleanFile != "Shared_Item" &&
            !cleanFile.startsWith("stage_") &&
            cleanFile != "text"
        ) {
            return cleanFile
        }

        val firstLine = textBody?.lineSequence()?.firstOrNull { it.isNotBlank() }?.trim()
        if (!firstLine.isNullOrBlank()) {
            return if (firstLine.length > 40) firstLine.take(40) + "..." else firstLine
        }

        return "Shared Note"
    }

    /**
     * Computes the number of currently selected items that are eligible for placement
     * in the active destination (folder root, subfolder, or photo group).
     */
    fun computePlaceableCount(
        items: List<StagedShareItem>,
        isGroupDestination: Boolean
    ): Int {
        return items.count { it.isSelected && it.canPlaceInDestination(isGroupDestination) }
    }

    /**
     * Produces the exact required button label per specification:
     * - Exactly "Place Here (1)" when count is 1
     * - Exactly "Place Here (N)" when count is N
     * - "Place Here (0)" when count is 0
     */
    fun formatPlaceButtonLabel(placeableCount: Int): String {
        return "Place Here ($placeableCount)"
    }

    /**
     * Toggles the selection of a specific item. Error items cannot be selected.
     */
    fun toggleItemSelection(
        items: List<StagedShareItem>,
        itemId: String
    ): List<StagedShareItem> {
        return items.map { item ->
            if (item.id == itemId && !item.isError) {
                item.copy(isSelected = !item.isSelected)
            } else {
                item
            }
        }
    }

    /**
     * Sets selection for all non-error items.
     */
    fun setAllSelected(
        items: List<StagedShareItem>,
        selected: Boolean
    ): List<StagedShareItem> {
        return items.map { item ->
            if (item.isError) {
                item.copy(isSelected = false)
            } else {
                item.copy(isSelected = selected)
            }
        }
    }

    /**
     * Removes placed items from the staged items list.
     * Remaining unplaced items are retained with their current selection intact.
     */
    fun removePlacedItems(
        currentItems: List<StagedShareItem>,
        placedItemIds: Set<String>
    ): List<StagedShareItem> {
        return currentItems.filter { it.id !in placedItemIds }
    }

    /**
     * Determines whether the right-side item tray panel should be available.
     * "Right side, shown only when more than one item was shared... With a single item there is no panel."
     */
    fun shouldShowSidePanel(totalSharedCount: Int): Boolean {
        return totalSharedCount > 1
    }
}
