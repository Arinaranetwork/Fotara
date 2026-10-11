// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components

import com.arinara.fotara.R
import com.arinara.fotara.data.model.DateRange
import com.arinara.fotara.data.model.SearchDateFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchScreenRedesignTest {

    @Test
    fun testSortPillAlwaysHighlighted() {
        // Rule: Sort pill is ALWAYS rendered in highlighted blue style
        assertTrue(SearchScreenLogic.isSortPillHighlighted())
    }

    @Test
    fun testDatePillHighlightDefaultAndActive() {
        // Neutral dark style when ALL (default)
        assertFalse(SearchScreenLogic.isDatePillHighlighted(SearchDateFilter.ALL))

        // Switches to highlighted blue style for any non-default date filter
        assertTrue(SearchScreenLogic.isDatePillHighlighted(SearchDateFilter.TODAY))
        assertTrue(SearchScreenLogic.isDatePillHighlighted(SearchDateFilter.YESTERDAY))
        assertTrue(SearchScreenLogic.isDatePillHighlighted(SearchDateFilter.THIS_WEEK))
        assertTrue(SearchScreenLogic.isDatePillHighlighted(SearchDateFilter.THIS_MONTH))
        assertTrue(SearchScreenLogic.isDatePillHighlighted(SearchDateFilter.THIS_YEAR))
        assertTrue(SearchScreenLogic.isDatePillHighlighted(SearchDateFilter.SINGLE_DAY))
        assertTrue(SearchScreenLogic.isDatePillHighlighted(SearchDateFilter.CUSTOM_RANGE))
    }

    @Test
    fun testFilterCountBadgeCalculation() {
        // Zero active filters
        assertEquals(0, SearchScreenLogic.calculateActiveFilterCount(null, null))

        // Color filter only
        assertEquals(1, SearchScreenLogic.calculateActiveFilterCount("#E63946", null))

        // Smart tag filter only
        assertEquals(1, SearchScreenLogic.calculateActiveFilterCount(null, "physics"))

        // Both color tag and smart tag active
        assertEquals(2, SearchScreenLogic.calculateActiveFilterCount("#2A9D8F", "organic_chem"))
    }

    @Test
    fun testFilterPillHighlightDefaultAndActive() {
        // Neutral dark style when neither color nor smart tag filter is selected
        assertFalse(SearchScreenLogic.isFilterPillHighlighted(null, null))

        // Highlighted blue style when color filter is selected
        assertTrue(SearchScreenLogic.isFilterPillHighlighted("#E63946", null))

        // Highlighted blue style when smart tag is selected
        assertTrue(SearchScreenLogic.isFilterPillHighlighted(null, "lecture"))

        // Highlighted blue style when both are selected
        assertTrue(SearchScreenLogic.isFilterPillHighlighted("#F4A261", "exam"))
    }

    @Test
    fun testScopeRowVisibilityRules() {
        // Default scopes contains only "All" -> size 1 -> hidden
        val defaultList = SearchScreenLogic.defaultScopes
        assertEquals(1, defaultList.size)
        assertFalse(SearchScreenLogic.shouldShowScopeRow(defaultList))

        // Empty scopes -> hidden
        assertFalse(SearchScreenLogic.shouldShowScopeRow(emptyList()))

        // Multiple scopes (e.g. All, Workspaces) -> visible
        val multipleScopes = listOf(
            SearchScopeItem("all", R.string.search_scope_all),
            SearchScopeItem("custom_workspace", R.string.search_title)
        )
        assertTrue(SearchScreenLogic.shouldShowScopeRow(multipleScopes))
    }

    @Test
    fun testRecentSearchesRemoveSingleItem() {
        val initial = listOf("NaCl", "BAHASA JAWA", "soal bahasa jawa")

        // Remove item from middle
        val afterMiddle = SearchScreenLogic.removeRecentSearch(initial, "BAHASA JAWA")
        assertEquals(listOf("NaCl", "soal bahasa jawa"), afterMiddle)

        // Remove item from head
        val afterHead = SearchScreenLogic.removeRecentSearch(initial, "NaCl")
        assertEquals(listOf("BAHASA JAWA", "soal bahasa jawa"), afterHead)

        // Removing non-existent item leaves list unchanged
        val afterNonExistent = SearchScreenLogic.removeRecentSearch(initial, "Calculus")
        assertEquals(initial, afterNonExistent)
    }

    @Test
    fun testRecentSearchesClearAll() {
        val initial = listOf("NaCl", "BAHASA JAWA", "soal bahasa jawa")
        val cleared = SearchScreenLogic.clearRecentSearches()
        assertTrue(cleared.isEmpty())
    }

    @Test
    fun testRecentSearchesCasingPreservation() {
        val originalQueries = listOf(
            "NaCl",
            "BAHASA JAWA",
            "soal bahasa jawa",
            "DNA & RNA helicase",
            "pH calculation"
        )

        // Ensure removal of one item retains exact casing of other items
        val modified = SearchScreenLogic.removeRecentSearch(originalQueries, "NaCl")
        assertEquals("BAHASA JAWA", modified[0])
        assertEquals("soal bahasa jawa", modified[1])
        assertEquals("DNA & RNA helicase", modified[2])
        assertEquals("pH calculation", modified[3])
    }

    @Test
    fun testRecentSearchesCardVisibility() {
        val populatedList = listOf("NaCl", "BAHASA JAWA")

        // Visible when query is empty and list is non-empty
        assertTrue(SearchScreenLogic.isRecentSearchesCardVisible("", populatedList))
        assertTrue(SearchScreenLogic.isRecentSearchesCardVisible("   ", populatedList))

        // Hidden when query has text
        assertFalse(SearchScreenLogic.isRecentSearchesCardVisible("math", populatedList))
        assertFalse(SearchScreenLogic.isRecentSearchesCardVisible("N", populatedList))

        // Hidden when recent searches list is empty even if query is empty
        assertFalse(SearchScreenLogic.isRecentSearchesCardVisible("", emptyList()))
    }

    @Test
    fun testIdleStateVisibility() {
        // Visible when query is blank
        assertTrue(SearchScreenLogic.isIdleStateVisible(""))
        assertTrue(SearchScreenLogic.isIdleStateVisible("   "))

        // Hidden when user types a query
        assertFalse(SearchScreenLogic.isIdleStateVisible("chem"))
    }

    @Test
    fun testFormatDateFilterLabel() {
        val defaultLabel = "All dates"

        assertEquals(
            defaultLabel,
            SearchScreenLogic.formatDateFilterLabel(SearchDateFilter.ALL, null, defaultLabel)
        )
        assertEquals(
            SearchDateFilter.TODAY.label,
            SearchScreenLogic.formatDateFilterLabel(SearchDateFilter.TODAY, null, defaultLabel)
        )

        // Single day formatted with custom range
        val range = DateRange(1774886400000L, 1774972799999L) // specific timestamp
        val singleDayFormatted = SearchScreenLogic.formatDateFilterLabel(
            SearchDateFilter.SINGLE_DAY,
            range,
            defaultLabel
        )
        assertFalse(singleDayFormatted.isBlank())
    }
}
