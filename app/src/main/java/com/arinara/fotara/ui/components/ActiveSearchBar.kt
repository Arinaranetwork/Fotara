// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import com.arinara.fotara.data.model.Workspace
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.arinara.fotara.R
import com.arinara.fotara.data.model.CanvasNote
import com.arinara.fotara.data.model.DateRange
import com.arinara.fotara.data.model.DocumentNote
import com.arinara.fotara.data.model.DocumentType
import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.model.PhotoGroup
import com.arinara.fotara.data.model.SearchDateFilter
import com.arinara.fotara.data.model.SearchSortOrder
import com.arinara.fotara.data.model.TagColor
import com.arinara.fotara.data.model.TextNote
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.theme.FolderBodyBlue
import com.arinara.fotara.theme.FolderTabCream
import com.arinara.fotara.theme.HomeCardBorder
import com.arinara.fotara.theme.HomeCardSurface
import com.arinara.fotara.theme.HomeMainButtonBlue
import com.arinara.fotara.theme.HomeNearBlack
import com.arinara.fotara.theme.HomeSearchBarBorder
import com.arinara.fotara.theme.HomeSearchBarSurface
import com.arinara.fotara.theme.HomeSubtitleGray
import com.arinara.fotara.theme.TagAmber
import com.arinara.fotara.theme.TextPrimary
import com.arinara.fotara.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ActiveSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean,
    folderResults: List<Folder>,
    groupResults: List<PhotoGroup> = emptyList(),
    photoResults: List<Photo>,
    textNoteResults: List<TextNote> = emptyList(),
    documentResults: List<DocumentNote> = emptyList(),
    canvasNoteResults: List<CanvasNote> = emptyList(),
    recentSearches: List<String>,
    selectedDateFilter: SearchDateFilter,
    onSelectDateFilter: (SearchDateFilter) -> Unit,
    selectedColorFilter: String?,
    onSelectColorFilter: (String?) -> Unit,
    smartTags: List<String> = emptyList(),
    selectedSmartTag: String? = null,
    onSelectSmartTag: (String?) -> Unit = {},
    sortOrder: SearchSortOrder = SearchSortOrder.NEWEST_ADDED,
    onToggleSortOrder: () -> Unit = {},
    customDateRange: DateRange? = null,
    onSelectCustomDateRange: (DateRange) -> Unit = {},
    onClearFilters: () -> Unit,
    onSearchSubmitted: (String) -> Unit,
    onRemoveRecentSearch: (String) -> Unit,
    onClearRecentSearches: () -> Unit,
    onFolderClick: (Long) -> Unit,
    onGroupClick: (PhotoGroup) -> Unit = {},
    onPhotoClick: (Photo) -> Unit,
    onTextNoteClick: (TextNote) -> Unit = {},
    onDocumentClick: (DocumentNote) -> Unit = {},
    onCanvasClick: (CanvasNote) -> Unit = {},
    workspaces: List<Workspace> = emptyList(),
    selectedWorkspaceScopeId: Long? = null,
    onSelectWorkspaceScope: (Long?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    var showSortDropdown by remember { mutableStateOf(false) }
    var showDateDropdown by remember { mutableStateOf(false) }
    var showFilterDropdown by remember { mutableStateOf(false) }

    var showSingleDayDialog by remember { mutableStateOf(false) }
    var showCustomRangeDialog by remember { mutableStateOf(false) }

    val rawOverlayPadding = LocalBottomOverlayPadding.current
    val bottomOverlayPadding = if (rawOverlayPadding > 0.dp) rawOverlayPadding else 96.dp

    BackHandler {
        onDismiss()
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    // Single Day Picker Dialog
    if (showSingleDayDialog) {
        val singleDatePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showSingleDayDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selected = singleDatePickerState.selectedDateMillis
                        if (selected != null) {
                            onSelectCustomDateRange(DateRange(selected, selected + 86400000L - 1L))
                            onSelectDateFilter(SearchDateFilter.SINGLE_DAY)
                        }
                        showSingleDayDialog = false
                    }
                ) {
                    Text(stringResource(R.string.search_apply), color = HomeMainButtonBlue, fontFamily = ElmsSans)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSingleDayDialog = false }) {
                    Text(stringResource(R.string.search_cancel), color = TextSecondary, fontFamily = ElmsSans)
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = HomeCardSurface
            )
        ) {
            DatePicker(
                state = singleDatePickerState,
                title = {
                    Text(
                        text = stringResource(R.string.search_date_single_day_title),
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                        color = TextPrimary,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold
                    )
                },
                headline = {
                    Text(
                        text = stringResource(R.string.search_date_single_day_subtitle),
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 12.dp),
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontFamily = ElmsSans
                    )
                }
            )
        }
    }

    // Custom Date Range Picker Dialog
    if (showCustomRangeDialog) {
        val dateRangePickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showCustomRangeDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val start = dateRangePickerState.selectedStartDateMillis
                        val end = dateRangePickerState.selectedEndDateMillis ?: start
                        if (start != null) {
                            onSelectCustomDateRange(DateRange(start, (end ?: start) + 86400000L - 1L))
                            onSelectDateFilter(SearchDateFilter.CUSTOM_RANGE)
                        }
                        showCustomRangeDialog = false
                    }
                ) {
                    Text(stringResource(R.string.search_apply), color = HomeMainButtonBlue, fontFamily = ElmsSans)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomRangeDialog = false }) {
                    Text(stringResource(R.string.search_cancel), color = TextSecondary, fontFamily = ElmsSans)
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = HomeCardSurface
            )
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                title = {
                    Text(
                        text = stringResource(R.string.search_date_range_title),
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                        color = TextPrimary,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold
                    )
                },
                headline = {
                    Text(
                        text = stringResource(R.string.search_date_range_subtitle),
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 12.dp),
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontFamily = ElmsSans
                    )
                }
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HomeNearBlack)
            .zIndex(24f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .imePadding()
        ) {
            // 1. Header Row (Back arrow + Large "Search" title)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.search_nav_back),
                        tint = TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.search_title),
                    style = TextStyle(
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        color = TextPrimary
                    )
                )
            }

            // 2. Controls Container (Surface card with 3 pills: Sort, Date, Filter)
            Card(
                colors = CardDefaults.cardColors(containerColor = HomeCardSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, HomeCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pill 1: Sort Pill (ALWAYS highlighted blue style)
                    Box(modifier = Modifier.weight(1f)) {
                        val sortLabel = if (sortOrder == SearchSortOrder.NEWEST_ADDED) {
                            stringResource(R.string.search_sort_newest)
                        } else {
                            stringResource(R.string.search_sort_oldest)
                        }

                        Surface(
                            color = HomeMainButtonBlue,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showSortDropdown = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = stringResource(R.string.search_sort_pill),
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = sortLabel,
                                    style = TextStyle(
                                        fontFamily = ElmsSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.White
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showSortDropdown,
                            onDismissRequest = { showSortDropdown = false },
                            modifier = Modifier
                                .background(HomeCardSurface)
                                .border(1.dp, HomeCardBorder, RoundedCornerShape(12.dp))
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.search_sort_newest),
                                        color = if (sortOrder == SearchSortOrder.NEWEST_ADDED) HomeMainButtonBlue else TextPrimary,
                                        fontFamily = ElmsSans,
                                        fontWeight = if (sortOrder == SearchSortOrder.NEWEST_ADDED) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp
                                    )
                                },
                                onClick = {
                                    if (sortOrder != SearchSortOrder.NEWEST_ADDED) {
                                        onToggleSortOrder()
                                    }
                                    showSortDropdown = false
                                },
                                leadingIcon = {
                                    if (sortOrder == SearchSortOrder.NEWEST_ADDED) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = HomeMainButtonBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.search_sort_oldest),
                                        color = if (sortOrder == SearchSortOrder.OLDEST_ADDED) HomeMainButtonBlue else TextPrimary,
                                        fontFamily = ElmsSans,
                                        fontWeight = if (sortOrder == SearchSortOrder.OLDEST_ADDED) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp
                                    )
                                },
                                onClick = {
                                    if (sortOrder != SearchSortOrder.OLDEST_ADDED) {
                                        onToggleSortOrder()
                                    }
                                    showSortDropdown = false
                                },
                                leadingIcon = {
                                    if (sortOrder == SearchSortOrder.OLDEST_ADDED) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = HomeMainButtonBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }

                    // Pill 2: Date Pill (Neutral dark style by default, highlighted blue when non-default)
                    val isDateActive = SearchScreenLogic.isDatePillHighlighted(selectedDateFilter)
                    val dateLabel = SearchScreenLogic.formatDateFilterLabel(
                        selectedDateFilter = selectedDateFilter,
                        customDateRange = customDateRange,
                        defaultAllLabel = stringResource(R.string.search_date_all)
                    )

                    Box(modifier = Modifier.weight(1.3f)) {
                        Surface(
                            color = if (isDateActive) HomeMainButtonBlue else HomeSearchBarSurface,
                            shape = RoundedCornerShape(12.dp),
                            border = if (isDateActive) null else BorderStroke(1.dp, HomeSearchBarBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDateDropdown = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = stringResource(R.string.search_date_pill),
                                    tint = if (isDateActive) Color.White else TextSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = dateLabel,
                                    style = TextStyle(
                                        fontFamily = ElmsSans,
                                        fontWeight = if (isDateActive) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = if (isDateActive) Color.White else TextPrimary
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = if (isDateActive) Color.White else TextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showDateDropdown,
                            onDismissRequest = { showDateDropdown = false },
                            modifier = Modifier
                                .background(HomeCardSurface)
                                .border(1.dp, HomeCardBorder, RoundedCornerShape(12.dp))
                        ) {
                            val dateOptions = listOf(
                                SearchDateFilter.ALL to stringResource(R.string.search_date_all),
                                SearchDateFilter.TODAY to SearchDateFilter.TODAY.label,
                                SearchDateFilter.YESTERDAY to SearchDateFilter.YESTERDAY.label,
                                SearchDateFilter.THIS_WEEK to SearchDateFilter.THIS_WEEK.label,
                                SearchDateFilter.THIS_MONTH to SearchDateFilter.THIS_MONTH.label,
                                SearchDateFilter.THIS_YEAR to SearchDateFilter.THIS_YEAR.label,
                                SearchDateFilter.SINGLE_DAY to "Single Day...",
                                SearchDateFilter.CUSTOM_RANGE to "Custom Range..."
                            )

                            dateOptions.forEach { (filter, label) ->
                                val isSelected = selectedDateFilter == filter
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = label,
                                            color = if (isSelected) HomeMainButtonBlue else TextPrimary,
                                            fontFamily = ElmsSans,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.sp
                                        )
                                    },
                                    onClick = {
                                        showDateDropdown = false
                                        when (filter) {
                                            SearchDateFilter.SINGLE_DAY -> showSingleDayDialog = true
                                            SearchDateFilter.CUSTOM_RANGE -> showCustomRangeDialog = true
                                            else -> onSelectDateFilter(filter)
                                        }
                                    },
                                    leadingIcon = {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = HomeMainButtonBlue,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Pill 3: Filter Pill (Neutral dark style by default, highlighted blue with count badge when active)
                    val activeFilterCount = SearchScreenLogic.calculateActiveFilterCount(selectedColorFilter, selectedSmartTag)
                    val isFilterActive = SearchScreenLogic.isFilterPillHighlighted(selectedColorFilter, selectedSmartTag)

                    Box(modifier = Modifier.weight(1f)) {
                        Surface(
                            color = if (isFilterActive) HomeMainButtonBlue else HomeSearchBarSurface,
                            shape = RoundedCornerShape(12.dp),
                            border = if (isFilterActive) null else BorderStroke(1.dp, HomeSearchBarBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showFilterDropdown = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = stringResource(R.string.search_filter_pill),
                                    tint = if (isFilterActive) Color.White else TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.search_filter_pill),
                                    style = TextStyle(
                                        fontFamily = ElmsSans,
                                        fontWeight = if (isFilterActive) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = if (isFilterActive) Color.White else TextPrimary
                                    ),
                                    maxLines = 1
                                )
                                if (activeFilterCount > 0) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$activeFilterCount",
                                            style = TextStyle(
                                                fontFamily = ElmsSans,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                color = HomeMainButtonBlue
                                            )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = if (isFilterActive) Color.White else TextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showFilterDropdown,
                            onDismissRequest = { showFilterDropdown = false },
                            modifier = Modifier
                                .background(HomeCardSurface)
                                .border(1.dp, HomeCardBorder, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                                .width(260.dp)
                        ) {
                            // Section: Color Tags
                            Text(
                                text = stringResource(R.string.search_filter_color_tags),
                                style = TextStyle(
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TagColor.entries.forEach { tag ->
                                    val isSelected = selectedColorFilter == tag.hex
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(tag.composeColor)
                                            .border(
                                                width = if (isSelected) 2.5.dp else 1.dp,
                                                color = if (isSelected) Color.White else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                if (isSelected) {
                                                    onSelectColorFilter(null)
                                                } else {
                                                    onSelectColorFilter(tag.hex)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Section: Smart Tags
                            if (smartTags.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = stringResource(R.string.search_filter_smart_tags),
                                    style = TextStyle(
                                        fontFamily = ElmsSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    smartTags.forEach { tag ->
                                        val isSelected = selectedSmartTag == tag
                                        Surface(
                                            color = if (isSelected) TagAmber.copy(alpha = 0.35f) else HomeSearchBarSurface,
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(
                                                1.dp,
                                                if (isSelected) TagAmber else HomeSearchBarBorder
                                            ),
                                            modifier = Modifier.clickable {
                                                if (isSelected) {
                                                    onSelectSmartTag(null)
                                                } else {
                                                    onSelectSmartTag(tag)
                                                }
                                            }
                                        ) {
                                            Text(
                                                text = "#$tag",
                                                style = TextStyle(
                                                    fontFamily = ElmsSans,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 12.sp,
                                                    color = if (isSelected) TagAmber else TextPrimary
                                                ),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Section: Clear Filters Action
                            if (activeFilterCount > 0) {
                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = HomeCardBorder)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onClearFilters()
                                            showFilterDropdown = false
                                        }
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        tint = TagColor.CRIMSON.composeColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.search_filter_clear),
                                        style = TextStyle(
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = TagColor.CRIMSON.composeColor
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Search Input Field (Rounded card surface, magnifier, no scan icon, clear button when text present)
            Card(
                colors = CardDefaults.cardColors(containerColor = HomeSearchBarSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, HomeSearchBarBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                text = stringResource(R.string.search_placeholder),
                                style = TextStyle(
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Light,
                                    fontSize = 15.sp,
                                    color = TextSecondary
                                )
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = onQueryChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            textStyle = TextStyle(
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp,
                                color = TextPrimary
                            ),
                            cursorBrush = SolidColor(HomeMainButtonBlue),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    onSearchSubmitted(query)
                                    keyboardController?.hide()
                                }
                            )
                        )
                    }

                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { onQueryChange("") },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.search_clear_input),
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Indeterminate Video-Buffering Progress Bar
            VideoBufferingProgressBar(
                visible = isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .height(2.5.dp)
            )

            // 5. Scope Tabs Row (Always visible segmented container with All + workspaces in saved order)
            val sortedWorkspaces = remember(workspaces) { workspaces.sortedBy { it.position } }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isAllSelected = selectedWorkspaceScopeId == null
                Surface(
                    color = if (isAllSelected) HomeMainButtonBlue else HomeSearchBarSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = if (isAllSelected) null else BorderStroke(1.dp, HomeSearchBarBorder),
                    modifier = Modifier.clickable { onSelectWorkspaceScope(null) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = null,
                            tint = if (isAllSelected) Color.White else TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.search_scope_all),
                            style = TextStyle(
                                fontFamily = ElmsSans,
                                fontSize = 13.sp,
                                fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isAllSelected) Color.White else TextPrimary
                            )
                        )
                    }
                }

                sortedWorkspaces.forEach { ws ->
                    val isWsSelected = selectedWorkspaceScopeId == ws.id
                    Surface(
                        color = if (isWsSelected) HomeMainButtonBlue else HomeSearchBarSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = if (isWsSelected) null else BorderStroke(1.dp, HomeSearchBarBorder),
                        modifier = Modifier.clickable { onSelectWorkspaceScope(ws.id) }
                    ) {
                        Text(
                            text = ws.name,
                            style = TextStyle(
                                fontFamily = ElmsSans,
                                fontSize = 13.sp,
                                fontWeight = if (isWsSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isWsSelected) Color.White else TextPrimary
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            val hasResults = folderResults.isNotEmpty() ||
                groupResults.isNotEmpty() ||
                photoResults.isNotEmpty() ||
                textNoteResults.isNotEmpty() ||
                documentResults.isNotEmpty() ||
                canvasNoteResults.isNotEmpty()

            // Main Content Region
            if (query.trim().isEmpty() && !hasResults) {
                // Idle / Empty Query State
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // 4. Recent Searches Card (Shown only when query is empty AND there is at least one recent search)
                    if (SearchScreenLogic.isRecentSearchesCardVisible(query, recentSearches)) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = HomeCardSurface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, HomeCardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                            tint = TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.search_recent_title),
                                            style = TextStyle(
                                                fontFamily = ElmsSans,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = TextPrimary
                                            )
                                        )
                                    }

                                    Text(
                                        text = stringResource(R.string.search_recent_clear_all),
                                        style = TextStyle(
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp,
                                            color = HomeMainButtonBlue
                                        ),
                                        modifier = Modifier
                                            .clickable { onClearRecentSearches() }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    recentSearches.forEach { item ->
                                        Surface(
                                            color = HomeSearchBarSurface,
                                            shape = RoundedCornerShape(20.dp),
                                            border = BorderStroke(1.dp, HomeSearchBarBorder),
                                            modifier = Modifier.clickable {
                                                onQueryChange(item)
                                                onSearchSubmitted(item)
                                                keyboardController?.hide()
                                            }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(start = 10.dp, top = 6.dp, bottom = 6.dp, end = 6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Search,
                                                    contentDescription = null,
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = item,
                                                    style = TextStyle(
                                                        fontFamily = ElmsSans,
                                                        fontWeight = FontWeight.Medium,
                                                        fontSize = 13.sp,
                                                        color = TextPrimary
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                IconButton(
                                                    onClick = { onRemoveRecentSearch(item) },
                                                    modifier = Modifier.size(18.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = stringResource(R.string.search_recent_remove_item),
                                                        tint = TextSecondary,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 6. Idle State (Anime illustration, bold title, muted paragraph)
                    Spacer(modifier = Modifier.height(24.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.search_illustration),
                            contentDescription = stringResource(R.string.search_idle_illustration_desc),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth(0.65f)
                                .aspectRatio(1.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.search_idle_title),
                            style = TextStyle(
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = TextPrimary
                            ),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.search_idle_subtitle),
                            style = TextStyle(
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = HomeSubtitleGray
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(bottomOverlayPadding + 16.dp))
                }
            } else if (!hasResults && !isLoading) {
                // Zero Results State
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    val emptyTitle = if (selectedWorkspaceScopeId != null) {
                        val wsName = workspaces.firstOrNull { it.id == selectedWorkspaceScopeId }?.name ?: ""
                        stringResource(R.string.search_no_results_in_workspace, wsName)
                    } else if (query.isNotBlank()) {
                        stringResource(R.string.search_empty_matching_query, query)
                    } else {
                        stringResource(R.string.search_empty_matching_filters)
                    }
                    Text(
                        text = emptyTitle,
                        style = TextStyle(
                            fontFamily = ElmsSans,
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val emptyHint = if (selectedDateFilter != SearchDateFilter.ALL) {
                        stringResource(R.string.search_empty_date_hint)
                    } else {
                        stringResource(R.string.search_empty_general_hint)
                    }
                    Text(
                        text = emptyHint,
                        style = TextStyle(
                            fontFamily = ElmsSans,
                            color = TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    val hasActiveFilters = selectedDateFilter != SearchDateFilter.ALL || selectedColorFilter != null || selectedSmartTag != null
                    if (hasActiveFilters) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onClearFilters,
                            colors = ButtonDefaults.buttonColors(containerColor = HomeMainButtonBlue),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.search_reset_filters),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(bottomOverlayPadding))
                }
            } else {
                // 7. Live Results List with Bottom Overlay Padding
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = bottomOverlayPadding + 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (folderResults.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.search_section_folders, folderResults.size),
                                style = TextStyle(
                                    fontFamily = ElmsSans,
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(folderResults, key = { "folder_${it.id}" }) { folder ->
                            SearchFolderResultCard(
                                folder = folder,
                                onClick = { onFolderClick(folder.id) }
                            )
                        }
                    }

                    if (groupResults.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.search_section_groups, groupResults.size),
                                style = TextStyle(
                                    fontFamily = ElmsSans,
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(groupResults, key = { "group_${it.id}" }) { group ->
                            SearchGroupResultCard(
                                group = group,
                                onClick = { onGroupClick(group) }
                            )
                        }
                    }

                    if (photoResults.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.search_section_photos, photoResults.size),
                                style = TextStyle(
                                    fontFamily = ElmsSans,
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(photoResults, key = { "photo_${it.id}" }) { photo ->
                            SearchPhotoResultCard(
                                photo = photo,
                                query = query,
                                onClick = { onPhotoClick(photo) }
                            )
                        }
                    }

                    if (textNoteResults.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.search_section_text_notes, textNoteResults.size),
                                style = TextStyle(
                                    fontFamily = ElmsSans,
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(textNoteResults, key = { "text_note_${it.id}" }) { note ->
                            SearchTextNoteResultCard(
                                note = note,
                                query = query,
                                onClick = { onTextNoteClick(note) }
                            )
                        }
                    }

                    if (documentResults.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.search_section_documents, documentResults.size),
                                style = TextStyle(
                                    fontFamily = ElmsSans,
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(documentResults, key = { "document_${it.id}" }) { doc ->
                            SearchDocumentResultCard(
                                document = doc,
                                query = query,
                                onClick = { onDocumentClick(doc) }
                            )
                        }
                    }

                    if (canvasNoteResults.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.search_section_canvas_notes, canvasNoteResults.size),
                                style = TextStyle(
                                    fontFamily = ElmsSans,
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(canvasNoteResults, key = { "canvas_${it.id}" }) { canvas ->
                            SearchCanvasResultCard(
                                canvasNote = canvas,
                                onClick = { onCanvasClick(canvas) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoBufferingProgressBar(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(HomeCardBorder)
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(200))
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "bufferingAnimation")
            val positionFraction by infiniteTransition.animateFloat(
                initialValue = -0.3f,
                targetValue = 1.3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1100, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "positionFraction"
            )
            val widthFraction by infiniteTransition.animateFloat(
                initialValue = 0.25f,
                targetValue = 0.45f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 700, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "widthFraction"
            )

            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .fillMaxWidth(fraction = widthFraction)
                        .offset(x = (positionFraction * 320).dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                colors = listOf(
                                    HomeMainButtonBlue.copy(alpha = 0.2f),
                                    Color.White,
                                    HomeMainButtonBlue
                                )
                            )
                        )
                )
            }
        }
    }
}

@Composable
fun SearchFolderResultCard(
    folder: Folder,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = HomeCardSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, HomeCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(HomeMainButtonBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    style = TextStyle(
                        fontFamily = ElmsSans,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val countText = if (folder.photoCount == 1) "1 note" else "${folder.photoCount} notes"
                    Text(
                        text = countText,
                        style = TextStyle(
                            fontFamily = ElmsSans,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${formatAddedDate(folder.createdAt)}",
                        style = TextStyle(
                            fontFamily = ElmsSans,
                            color = TextSecondary.copy(alpha = 0.75f),
                            fontSize = 11.sp
                        )
                    )
                }
            }
            val tagColor = try {
                Color(android.graphics.Color.parseColor(folder.colorLabel))
            } catch (e: Exception) {
                HomeMainButtonBlue
            }
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(tagColor)
            )
        }
    }
}

fun getSearchSnippet(text: String, query: String, maxLength: Int = 90): String {
    if (text.isBlank()) return ""
    val cleanText = text.replace("\n", " ").replace("\\s+".toRegex(), " ").trim()
    val cleanQuery = query.trim()
    if (cleanQuery.isBlank() || !cleanText.contains(cleanQuery, ignoreCase = true)) {
        return if (cleanText.length <= maxLength) cleanText else "${cleanText.take(maxLength)}..."
    }

    val index = cleanText.indexOf(cleanQuery, ignoreCase = true)
    val halfLen = (maxLength - cleanQuery.length) / 2
    val start = (index - halfLen).coerceAtLeast(0)
    val end = (start + maxLength).coerceAtMost(cleanText.length)
    val actualStart = (end - maxLength).coerceAtLeast(0)

    val snippet = cleanText.substring(actualStart, end).trim()
    val prefix = if (actualStart > 0) "..." else ""
    val suffix = if (end < cleanText.length) "..." else ""
    return "$prefix$snippet$suffix"
}

fun buildHighlightedSearchSnippet(
    text: String,
    query: String,
    highlightColor: Color = TagAmber,
    maxLength: Int = 90
): AnnotatedString {
    val snippet = getSearchSnippet(text, query, maxLength)
    val cleanQuery = query.trim()
    if (cleanQuery.isBlank()) {
        return AnnotatedString(snippet)
    }

    val lowerSnippet = snippet.lowercase()
    val lowerQuery = cleanQuery.lowercase()
    val builder = AnnotatedString.Builder()

    var cur = 0
    while (cur < snippet.length) {
        val nextMatch = lowerSnippet.indexOf(lowerQuery, cur)
        if (nextMatch == -1) {
            builder.append(snippet.substring(cur))
            break
        }
        if (nextMatch > cur) {
            builder.append(snippet.substring(cur, nextMatch))
        }
        builder.pushStyle(
            SpanStyle(
                color = highlightColor,
                fontWeight = FontWeight.Bold,
                background = highlightColor.copy(alpha = 0.25f)
            )
        )
        builder.append(snippet.substring(nextMatch, nextMatch + cleanQuery.length))
        builder.pop()
        cur = nextMatch + cleanQuery.length
    }
    return builder.toAnnotatedString()
}

@Composable
fun SearchPhotoResultCard(
    photo: Photo,
    query: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = HomeCardSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, HomeCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(HomeSearchBarSurface)
                    .border(0.8.dp, HomeCardBorder, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = FolderTabCream,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = photo.caption ?: "Coursework Note #${photo.id}",
                    style = TextStyle(
                        fontFamily = ElmsSans,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatAddedDate(photo.addedAt),
                    style = TextStyle(
                        fontFamily = ElmsSans,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                if (!photo.ocrText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(TagAmber.copy(alpha = 0.2f))
                                .border(0.6.dp, TagAmber.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "OCR",
                                color = TagAmber,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = buildHighlightedSearchSnippet(photo.ocrText, query, TagAmber, 80),
                            style = TextStyle(
                                fontFamily = ElmsSans,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchGroupResultCard(
    group: PhotoGroup,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = HomeCardSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, HomeCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(TagAmber.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    tint = TagAmber,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    style = TextStyle(
                        fontFamily = ElmsSans,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Photo Group • ${formatAddedDate(group.addedAt)}",
                    style = TextStyle(
                        fontFamily = ElmsSans,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                )
            }
            if (!group.tagColor.isNullOrBlank()) {
                val tagColor = try {
                    Color(android.graphics.Color.parseColor(group.tagColor))
                } catch (e: Exception) {
                    null
                }
                if (tagColor != null) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(tagColor)
                    )
                }
            }
        }
    }
}

@Composable
fun SearchTextNoteResultCard(
    note: TextNote,
    query: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = HomeCardSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, HomeCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(HomeSearchBarSurface)
                    .border(0.8.dp, HomeCardBorder, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = FolderTabCream,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title.ifBlank { "Untitled Note" },
                    style = TextStyle(
                        fontFamily = ElmsSans,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatAddedDate(note.addedAt),
                    style = TextStyle(
                        fontFamily = ElmsSans,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                if (note.bodyMarkdown.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = buildHighlightedSearchSnippet(TextNote.stripMarkdownFormatting(note.bodyMarkdown), query, TagAmber, 85),
                        style = TextStyle(
                            fontFamily = ElmsSans,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                note.tagColor?.let { colorHex ->
                    val tag = TagColor.fromHex(colorHex)
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(tag.composeColor.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = tag.displayName,
                            color = tag.composeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = ElmsSans
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchDocumentResultCard(
    document: DocumentNote,
    query: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = HomeCardSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, HomeCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(HomeSearchBarSurface)
                    .border(0.8.dp, HomeCardBorder, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = FolderTabCream,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.name,
                    style = TextStyle(
                        fontFamily = ElmsSans,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${document.docType.name} Document • ${formatAddedDate(document.addedAt)}",
                    style = TextStyle(
                        fontFamily = ElmsSans,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                )
                if (document.docType == DocumentType.DOCX && !document.extractedText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    val badgeColor = FolderBodyBlue
                    val badgeLabel = "DOCX"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(badgeColor.copy(alpha = 0.2f))
                                .border(0.6.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = badgeLabel,
                                color = badgeColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = buildHighlightedSearchSnippet(document.extractedText, query, badgeColor, 80),
                            style = TextStyle(
                                fontFamily = ElmsSans,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchCanvasResultCard(
    canvasNote: CanvasNote,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = HomeCardSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, HomeCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(TagAmber.copy(alpha = 0.25f))
                    .border(0.8.dp, HomeCardBorder, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Brush,
                    contentDescription = null,
                    tint = TagAmber,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = canvasNote.title,
                    style = TextStyle(
                        fontFamily = ElmsSans,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Canvas Note • ${formatAddedDate(canvasNote.addedAt)}",
                    style = TextStyle(
                        fontFamily = ElmsSans,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}

private fun formatAddedDate(epochMs: Long): String {
    if (epochMs <= 0) return "Recently"
    val sdf = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.US)
    return "Added " + sdf.format(java.util.Date(epochMs))
}
