// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.model

enum class SearchDateFilter(val label: String) {
    ALL("All Dates"),
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    THIS_YEAR("This Year"),
    SINGLE_DAY("Single Day"),
    CUSTOM_RANGE("Custom Range")
}

data class DateRange(
    val startMs: Long,
    val endMs: Long
)

enum class SearchSortOrder(val label: String) {
    NEWEST_ADDED("Newest Added"),
    OLDEST_ADDED("Oldest Added")
}
