// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components

import com.arinara.fotara.R
import com.arinara.fotara.data.model.DateRange
import com.arinara.fotara.data.model.SearchDateFilter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Encapsulates a selectable search scope.
 */
data class SearchScopeItem(
    val id: String,
    val labelResId: Int
)

/**
 * Pure logic rules and calculations for the redesigned Search screen.
 */
object SearchScreenLogic {

    val defaultScopes: List<SearchScopeItem> = listOf(
        SearchScopeItem(id = "all", labelResId = R.string.search_scope_all)
    )

    /**
     * Sort pill is ALWAYS rendered in highlighted blue style.
     */
    fun isSortPillHighlighted(): Boolean = true

    /**
     * Date pill switches to highlighted blue style when non-default filter is active.
     */
    fun isDatePillHighlighted(selectedDateFilter: SearchDateFilter): Boolean =
        selectedDateFilter != SearchDateFilter.ALL

    /**
     * Calculates the active filter count: active color tag + active smart tag.
     */
    fun calculateActiveFilterCount(
        selectedColorFilter: String?,
        selectedSmartTag: String?
    ): Int {
        val colorCount = if (selectedColorFilter != null) 1 else 0
        val smartTagCount = if (selectedSmartTag != null) 1 else 0
        return colorCount + smartTagCount
    }

    /**
     * Filter pill switches to highlighted blue style when color or smart-tag filters are active.
     */
    fun isFilterPillHighlighted(
        selectedColorFilter: String?,
        selectedSmartTag: String?
    ): Boolean = calculateActiveFilterCount(selectedColorFilter, selectedSmartTag) > 0

    /**
     * Scope tabs row is hidden when scopes size <= 1, visible when > 1.
     */
    fun shouldShowScopeRow(scopes: List<SearchScopeItem>): Boolean =
        scopes.size > 1

    /**
     * Removes an entry from recent searches while preserving original casing and order.
     */
    fun removeRecentSearch(
        currentList: List<String>,
        targetItem: String
    ): List<String> = currentList.filter { it != targetItem }

    /**
     * Clears all recent searches.
     */
    fun clearRecentSearches(): List<String> = emptyList()

    /**
     * Recent searches card is shown only when query is empty and at least one entry exists.
     */
    fun isRecentSearchesCardVisible(
        query: String,
        recentSearches: List<String>
    ): Boolean = query.trim().isEmpty() && recentSearches.isNotEmpty()

    /**
     * Idle block (illustration + description) is visible when query is empty.
     */
    fun isIdleStateVisible(query: String): Boolean =
        query.trim().isEmpty()

    /**
     * Formats the human-readable label for the date pill.
     */
    fun formatDateFilterLabel(
        selectedDateFilter: SearchDateFilter,
        customDateRange: DateRange?,
        defaultAllLabel: String
    ): String {
        return when (selectedDateFilter) {
            SearchDateFilter.ALL -> defaultAllLabel
            SearchDateFilter.SINGLE_DAY -> {
                if (customDateRange != null) {
                    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.US)
                    sdf.format(Date(customDateRange.startMs))
                } else {
                    selectedDateFilter.label
                }
            }
            SearchDateFilter.CUSTOM_RANGE -> {
                if (customDateRange != null) {
                    val sdf = SimpleDateFormat("MMM d", Locale.US)
                    "${sdf.format(Date(customDateRange.startMs))} - ${sdf.format(Date(customDateRange.endMs))}"
                } else {
                    selectedDateFilter.label
                }
            }
            else -> selectedDateFilter.label
        }
    }
}
