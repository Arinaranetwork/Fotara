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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arinara.fotara.data.model.CanvasNote
import com.arinara.fotara.data.model.DateRange
import com.arinara.fotara.data.model.DocumentNote
import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.model.PhotoGroup
import com.arinara.fotara.data.model.SearchDateFilter
import com.arinara.fotara.data.model.SearchSortOrder
import com.arinara.fotara.data.model.TagColor
import com.arinara.fotara.data.model.TextNote
import com.arinara.fotara.theme.DockSlatePill
import com.arinara.fotara.theme.FolderBodyBlue
import com.arinara.fotara.theme.FolderTabCream
import com.arinara.fotara.theme.MidnightCardOutline
import com.arinara.fotara.theme.MidnightNavy
import com.arinara.fotara.theme.MidnightSurface
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
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    BackHandler {
        onDismiss()
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightNavy.copy(alpha = 0.96f))
            .zIndex(24f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .imePadding()
        ) {
            // Results & Recents Content Area (occupies upper region above docked search bar)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (query.trim().isEmpty() && selectedDateFilter == SearchDateFilter.ALL && selectedColorFilter == null && selectedSmartTag == null) {
                    // Empty Query State: Recent Searches
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
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
                                    text = "RECENT SEARCHES",
                                    style = TextStyle(
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                )
                            }

                            if (recentSearches.isNotEmpty()) {
                                Text(
                                    text = "Clear Recents",
                                    style = TextStyle(
                                        color = FolderTabCream,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    modifier = Modifier
                                        .clickable { onClearRecentSearches() }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (recentSearches.isEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No recent searches yet.",
                                style = TextStyle(
                                    color = TextSecondary.copy(alpha = 0.7f),
                                    fontSize = 14.sp
                                )
                            )
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                recentSearches.forEach { item ->
                                    Surface(
                                        color = DockSlatePill.copy(alpha = 0.45f),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(0.8.dp, MidnightCardOutline),
                                        modifier = Modifier.clickable {
                                            onQueryChange(item)
                                            onSearchSubmitted(item)
                                        }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = null,
                                                tint = FolderTabCream,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = item,
                                                style = TextStyle(
                                                    color = FolderTabCream,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            IconButton(
                                                onClick = { onRemoveRecentSearch(item) },
                                                modifier = Modifier.size(18.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove recent search",
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        Text(
                            text = "Tap any suggestion or type a keyword to search handwritten formulas, slide diagrams, and lecture notes indexed offline.",
                            style = TextStyle(
                                color = TextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        )
                    }
                } else if (folderResults.isEmpty() && groupResults.isEmpty() && photoResults.isEmpty() && textNoteResults.isEmpty() && documentResults.isEmpty() && canvasNoteResults.isEmpty() && !isLoading) {
                    // Zero Results Empty State
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
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
                        Text(
                            text = if (query.isNotBlank()) "No notes found matching \"$query\"" else "No notes found matching filters",
                            style = TextStyle(
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (selectedDateFilter != SearchDateFilter.ALL) {
                                "No coursework or notes added during the selected date period."
                            } else {
                                "Check spelling or search by general coursework topic or chapter."
                            },
                            style = TextStyle(
                                color = TextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            ),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        val hasActiveFilters = selectedDateFilter != SearchDateFilter.ALL || selectedColorFilter != null || selectedSmartTag != null
                        if (hasActiveFilters) {
                            Spacer(modifier = Modifier.height(16.dp))
                            androidx.compose.material3.Button(
                                onClick = onClearFilters,
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = FolderBodyBlue),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = FolderTabCream,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reset Filters", color = FolderTabCream, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                } else {
                    // Populate Live Results
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (folderResults.isNotEmpty()) {
                            item {
                                Text(
                                    text = "SUBJECT FOLDERS (${folderResults.size})",
                                    style = TextStyle(
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
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "PHOTO GROUPS (${groupResults.size})",
                                    style = TextStyle(
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
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "OCR NOTES & PHOTOS (${photoResults.size})",
                                    style = TextStyle(
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
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "TEXT NOTES (${textNoteResults.size})",
                                    style = TextStyle(
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
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "DOCUMENTS (${documentResults.size})",
                                    style = TextStyle(
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            items(documentResults, key = { "doc_${it.id}" }) { doc ->
                                SearchDocumentResultCard(
                                    document = doc,
                                    onClick = { onDocumentClick(doc) }
                                )
                            }
                        }

                        if (canvasNoteResults.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "CANVAS NOTES (${canvasNoteResults.size})",
                                    style = TextStyle(
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

                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            }

            // Horizontal Filter Controls Chip Row docked directly above floating search bar
            SearchFilterChipRow(
                selectedDateFilter = selectedDateFilter,
                onSelectDateFilter = onSelectDateFilter,
                selectedColorFilter = selectedColorFilter,
                onSelectColorFilter = onSelectColorFilter,
                smartTags = smartTags,
                selectedSmartTag = selectedSmartTag,
                onSelectSmartTag = onSelectSmartTag,
                sortOrder = sortOrder,
                onToggleSortOrder = onToggleSortOrder,
                customDateRange = customDateRange,
                onSelectCustomDateRange = onSelectCustomDateRange,
                onClearFilters = onClearFilters
            )

            // Keyboard-Docked Floating Search Bar Container (docked directly above IME / keyboard)
            Surface(
                color = MidnightSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MidnightCardOutline,
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    ),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Indeterminate Video-Buffering Progress Bar (height: 2.5dp) on top edge of search dock
                    VideoBufferingProgressBar(
                        visible = isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Exit Search",
                                tint = FolderTabCream
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (query.isEmpty()) {
                                Text(
                                    text = "Search notes, subjects, text...",
                                    style = TextStyle(
                                        color = TextSecondary,
                                        fontSize = 15.sp,
                                        fontStyle = FontStyle.Normal
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
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                cursorBrush = SolidColor(FolderTabCream),
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
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search query",
                                    tint = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchFilterChipRow(
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
    modifier: Modifier = Modifier
) {
    var showCustomRangeDialog by remember { mutableStateOf(false) }
    var showSingleDayDialog by remember { mutableStateOf(false) }

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
                    Text("Apply", color = FolderTabCream)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSingleDayDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = MidnightSurface
            )
        ) {
            DatePicker(
                state = singleDatePickerState,
                title = {
                    Text(
                        text = "Select Single Day",
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                        color = TextPrimary
                    )
                },
                headline = {
                    Text(
                        text = "Filter notes added on specific day",
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 12.dp),
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            )
        }
    }

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
                    Text("Apply", color = FolderTabCream)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomRangeDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = MidnightSurface
            )
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                title = {
                    Text(
                        text = "Select Date Range",
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                        color = TextPrimary
                    )
                },
                headline = {
                    Text(
                        text = "Filter notes added between dates",
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 12.dp),
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            )
        }
    }

    val isAnyFilterActive = selectedDateFilter != SearchDateFilter.ALL || selectedColorFilter != null || selectedSmartTag != null

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MidnightNavy.copy(alpha = 0.95f))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sort Order Toggle Chip
        Surface(
            color = DockSlatePill.copy(alpha = 0.65f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, FolderTabCream.copy(alpha = 0.5f)),
            modifier = Modifier.clickable { onToggleSortOrder() }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = "Toggle Sort Order",
                    tint = FolderTabCream,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = sortOrder.label,
                    style = TextStyle(
                        color = FolderTabCream,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }

        if (isAnyFilterActive) {
            Surface(
                color = TagColor.CRIMSON.composeColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, TagColor.CRIMSON.composeColor.copy(alpha = 0.6f)),
                modifier = Modifier.clickable { onClearFilters() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Reset Filters",
                        tint = TagColor.CRIMSON.composeColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Reset",
                        style = TextStyle(
                            color = TagColor.CRIMSON.composeColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        // Removable Active Date Filter Chip
        if (selectedDateFilter != SearchDateFilter.ALL) {
            Surface(
                color = FolderTabCream.copy(alpha = 0.2f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, FolderTabCream),
                modifier = Modifier.clickable { onSelectDateFilter(SearchDateFilter.ALL) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = FolderTabCream,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val dateLabel = when (selectedDateFilter) {
                        SearchDateFilter.SINGLE_DAY -> {
                            if (customDateRange != null) {
                                val sdf = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.US)
                                sdf.format(java.util.Date(customDateRange.startMs))
                            } else "Single Day"
                        }
                        SearchDateFilter.CUSTOM_RANGE -> {
                            if (customDateRange != null) {
                                val sdf = java.text.SimpleDateFormat("MMM d", java.util.Locale.US)
                                "${sdf.format(java.util.Date(customDateRange.startMs))} - ${sdf.format(java.util.Date(customDateRange.endMs))}"
                            } else "Custom Range"
                        }
                        else -> selectedDateFilter.label
                    }
                    Text(
                        text = dateLabel,
                        style = TextStyle(
                            color = FolderTabCream,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove Date Filter",
                        tint = FolderTabCream,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        // Removable Active Color Filter Chip
        if (selectedColorFilter != null) {
            val tag = TagColor.fromHex(selectedColorFilter)
            Surface(
                color = tag.composeColor.copy(alpha = 0.25f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, tag.composeColor),
                modifier = Modifier.clickable { onSelectColorFilter(null) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(tag.composeColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = tag.displayName,
                        style = TextStyle(
                            color = tag.composeColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove Color Filter",
                        tint = tag.composeColor,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        // Removable Active Smart Tag Chip
        if (selectedSmartTag != null) {
            Surface(
                color = TagAmber.copy(alpha = 0.25f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, TagAmber),
                modifier = Modifier.clickable { onSelectSmartTag(null) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = selectedSmartTag,
                        style = TextStyle(
                            color = TagAmber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove Smart Tag Filter",
                        tint = TagAmber,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        // Date Filter Chips
        SearchDateFilter.entries.forEach { filter ->
            val isSelected = selectedDateFilter == filter
            Surface(
                color = if (isSelected) FolderTabCream else DockSlatePill.copy(alpha = 0.5f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    1.dp,
                    if (isSelected) FolderTabCream else MidnightCardOutline
                ),
                modifier = Modifier.clickable {
                    when (filter) {
                        SearchDateFilter.CUSTOM_RANGE -> showCustomRangeDialog = true
                        SearchDateFilter.SINGLE_DAY -> showSingleDayDialog = true
                        else -> onSelectDateFilter(filter)
                    }
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    if (filter != SearchDateFilter.ALL) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = if (isSelected) MidnightNavy else FolderTabCream,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = filter.label,
                        style = TextStyle(
                            color = if (isSelected) MidnightNavy else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                }
            }
        }

        // Color Filter Chips
        TagColor.entries.forEach { tag ->
            val isSelected = selectedColorFilter == tag.hex
            Surface(
                color = if (isSelected) tag.composeColor.copy(alpha = 0.35f) else DockSlatePill.copy(alpha = 0.35f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    if (isSelected) 2.dp else 1.dp,
                    if (isSelected) tag.composeColor else MidnightCardOutline
                ),
                modifier = Modifier.clickable {
                    if (isSelected) onSelectColorFilter(null) else onSelectColorFilter(tag.hex)
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(tag.composeColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = tag.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = TextStyle(
                            color = if (isSelected) tag.composeColor else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                }
            }
        }

        // Smart Tag Chips
        if (smartTags.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .height(18.dp)
                    .width(1.dp)
                    .background(MidnightCardOutline)
            )

            smartTags.forEach { tag ->
                val isSelected = selectedSmartTag == tag
                Surface(
                    color = if (isSelected) TagAmber.copy(alpha = 0.35f) else DockSlatePill.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        if (isSelected) 1.5.dp else 1.dp,
                        if (isSelected) TagAmber else MidnightCardOutline
                    ),
                    modifier = Modifier.clickable {
                        if (isSelected) onSelectSmartTag(null) else onSelectSmartTag(tag)
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "#$tag",
                            style = TextStyle(
                                color = if (isSelected) TagAmber else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        )
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
            .background(MidnightCardOutline)
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(200))
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "bufferingAnimation")
            // Highlight position moving left to right
            val positionFraction by infiniteTransition.animateFloat(
                initialValue = -0.3f,
                targetValue = 1.3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1100, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "positionFraction"
            )
            // Variable highlight segment width (between 25% and 45%)
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
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .fillMaxWidth(fraction = widthFraction)
                        .offset(x = (positionFraction * 320).dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                colors = listOf(
                                    FolderBodyBlue.copy(alpha = 0.2f),
                                    FolderTabCream,
                                    FolderBodyBlue
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
        colors = CardDefaults.cardColors(containerColor = MidnightSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MidnightCardOutline),
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
                    .background(FolderBodyBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = FolderTabCream,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    style = TextStyle(
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${folder.photoCount} note photos",
                        style = TextStyle(
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "· ${formatAddedDate(folder.createdAt)}",
                        style = TextStyle(
                            color = TextSecondary.copy(alpha = 0.75f),
                            fontSize = 11.sp
                        )
                    )
                }
            }
            // Tag color indicator
            val tagColor = try {
                Color(android.graphics.Color.parseColor(folder.colorLabel))
            } catch (e: Exception) {
                FolderTabCream
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

@Composable
fun SearchPhotoResultCard(
    photo: Photo,
    query: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MidnightSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MidnightCardOutline),
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
                    .background(DockSlatePill.copy(alpha = 0.5f))
                    .border(0.8.dp, MidnightCardOutline, RoundedCornerShape(8.dp)),
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
                        color = FolderTabCream.copy(alpha = 0.75f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                if (!photo.ocrText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "“${photo.ocrText.take(90)}...”",
                        style = TextStyle(
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 16.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
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
        colors = CardDefaults.cardColors(containerColor = MidnightSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MidnightCardOutline),
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
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Photo Group · ${formatAddedDate(group.addedAt)}",
                    style = TextStyle(
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
        colors = CardDefaults.cardColors(containerColor = MidnightSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MidnightCardOutline),
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
                    .background(DockSlatePill.copy(alpha = 0.5f))
                    .border(0.8.dp, MidnightCardOutline, RoundedCornerShape(8.dp)),
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
                        color = FolderTabCream.copy(alpha = 0.75f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                if (note.bodyMarkdown.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "“${note.getPlainTextSnippet(90)}”",
                        style = TextStyle(
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
                            fontWeight = FontWeight.Bold
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
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MidnightSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MidnightCardOutline),
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
                    .background(FolderBodyBlue.copy(alpha = 0.35f))
                    .border(0.8.dp, MidnightCardOutline, RoundedCornerShape(8.dp)),
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
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${document.docType.name} Document · ${formatAddedDate(document.addedAt)}",
                    style = TextStyle(
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                )
                if (!document.extractedText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "“${document.extractedText.take(90)}...”",
                        style = TextStyle(
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 16.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
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
        colors = CardDefaults.cardColors(containerColor = MidnightSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MidnightCardOutline),
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
                    .border(0.8.dp, MidnightCardOutline, RoundedCornerShape(8.dp)),
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
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Canvas Note · ${formatAddedDate(canvasNote.addedAt)}",
                    style = TextStyle(
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
