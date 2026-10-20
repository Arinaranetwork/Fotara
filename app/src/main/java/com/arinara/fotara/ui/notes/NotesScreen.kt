// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.notes

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import com.arinara.fotara.ui.components.workspaceGroupedFolderItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import com.arinara.fotara.ui.components.PullToRefreshLayout
import com.arinara.fotara.ui.components.PullToRefreshHelper
import com.arinara.fotara.ui.components.LocalBottomOverlayPadding
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.LaunchedEffect
import com.arinara.fotara.ui.components.ScreenHeader
import com.arinara.fotara.ui.components.ScreenHeaderActionButton
import com.arinara.fotara.ui.home.workspace.WorkspaceTabBar
import com.arinara.fotara.ui.home.workspace.AddWorkspaceDialog
import com.arinara.fotara.ui.home.workspace.EditWorkspaceDialog
import com.arinara.fotara.ui.home.workspace.RenameWorkspaceDialog
import com.arinara.fotara.ui.home.workspace.DeleteWorkspaceConfirmDialog
import com.arinara.fotara.ui.home.workspace.DeleteWorkspaceChoiceDialog
import com.arinara.fotara.ui.home.workspace.DeleteWorkspacePermanentConfirmDialog
import com.arinara.fotara.ui.home.workspace.DeleteWorkspaceProgressDialog
import com.arinara.fotara.ui.home.WorkspaceDeleteStep
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.UploadFile
import com.arinara.fotara.ui.components.presets.AppCard
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.arinara.fotara.theme.FotaraShapes
import com.arinara.fotara.theme.FotaraSpacing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.arinara.fotara.R
import com.arinara.fotara.data.model.DocumentType
import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.theme.HomeAddButtonBlue
import com.arinara.fotara.theme.HomeCardBorder
import com.arinara.fotara.theme.HomeCardSurface
import com.arinara.fotara.theme.HomeHeaderButtonBg
import com.arinara.fotara.theme.HomeMainButtonBlue
import com.arinara.fotara.theme.HomeNearBlack
import com.arinara.fotara.theme.HomeSubtitleGray
import com.arinara.fotara.theme.TagCrimson
import com.arinara.fotara.theme.TextPrimary
import com.arinara.fotara.theme.TextSecondary
import com.arinara.fotara.theme.TextMuted
import com.arinara.fotara.ui.components.SharedFloatingAddButton
import com.arinara.fotara.ui.components.verticalEdgeFade

@Composable
fun NotesScreen(
    viewModel: NotesViewModel,
    onNavigateToPhoto: (folderId: Long, subfolderId: Long?, photoId: Long) -> Unit,
    onNavigateToDocument: (folderId: Long, subfolderId: Long?, docId: Long, targetPageIndex: Int?) -> Unit,
    onNavigateToTextNote: (folderId: Long, subfolderId: Long?, noteId: Long) -> Unit,
    onNavigateToCanvasNote: (folderId: Long, subfolderId: Long?, canvasId: Long) -> Unit,
    onOpenDocx: (documentId: Long) -> Unit,
    onFolderClick: (Folder) -> Unit,
    onCreatePhotoNote: (Folder) -> Unit,
    onCreateTextNote: (Folder) -> Unit,
    onCreateCanvasNote: (Folder) -> Unit,
    onImportDocument: (Folder) -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenTrash: () -> Unit = {},
    onOpenUpdates: () -> Unit = {},
    onOpenWhatsNew: () -> Unit = {},
    onOpenFeedback: () -> Unit = {},
    modifier: Modifier = Modifier,
    listState: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showHeaderOverflowMenu by remember { mutableStateOf(false) }
    var showFabCreateMenu by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<UnifiedNoteItem?>(null) }
    var itemToRename by remember { mutableStateOf<UnifiedNoteItem?>(null) }
    var itemToMove by remember { mutableStateOf<UnifiedNoteItem?>(null) }
    var renameInputText by remember { mutableStateOf("") }
    var pendingCreateAction by remember { mutableStateOf<((Folder) -> Unit)?>(null) }

    val bottomOverlayPadding = LocalBottomOverlayPadding.current
    val fabBottomPadding = (bottomOverlayPadding + 8.dp).coerceAtLeast(80.dp)
    val bottomClearance = 100.dp

    LaunchedEffect(uiState.userMessage) {
        val msg = uiState.userMessage
        if (msg != null) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearUserMessage()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HomeNearBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // 1. Top Header
            ScreenHeader(
                title = stringResource(R.string.notes_title),
                tagline = null,
                actions = {
                    ScreenHeaderActionButton(
                        onClick = { viewModel.toggleFilters() },
                        icon = Icons.Default.Search,
                        contentDescription = stringResource(
                            if (uiState.isFiltersVisible) R.string.cd_hide_filters else R.string.cd_show_filters
                        ),
                        isActive = uiState.isFiltersVisible
                    )

                    Box {
                        ScreenHeaderActionButton(
                            onClick = { showHeaderOverflowMenu = true },
                            icon = Icons.Default.MoreVert,
                            contentDescription = "More"
                        )

                        DropdownMenu(
                            expanded = showHeaderOverflowMenu,
                            onDismissRequest = { showHeaderOverflowMenu = false },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, FotaraShapes.InputField)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Check for Updates", color = TextPrimary, fontFamily = ElmsSans, fontWeight = FontWeight.Medium) },
                                leadingIcon = { Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = TextPrimary) },
                                onClick = {
                                    showHeaderOverflowMenu = false
                                    onOpenUpdates()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Send Feedback", color = TextPrimary, fontFamily = ElmsSans, fontWeight = FontWeight.Medium) },
                                leadingIcon = { Icon(Icons.Default.Feedback, contentDescription = null, tint = TextPrimary) },
                                onClick = {
                                    showHeaderOverflowMenu = false
                                    onOpenFeedback()
                                }
                            )
                            HorizontalDivider(color = HomeCardBorder)
                            DropdownMenuItem(
                                text = { Text("Trash", color = TextPrimary, fontFamily = ElmsSans, fontWeight = FontWeight.Medium) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = TagCrimson) },
                                onClick = {
                                    showHeaderOverflowMenu = false
                                    onOpenTrash()
                                }
                            )
                        }
                    }
                }
            )

            // Workspace Tab Bar
            WorkspaceTabBar(
                workspaces = uiState.workspaces,
                selectedWorkspaceId = uiState.selectedWorkspaceId,
                isSelectMode = false,
                onWorkspaceSelected = { ws -> viewModel.selectWorkspace(ws.id) },
                onAddClick = { viewModel.openAddWorkspaceDialog() },
                onLimitReached = { viewModel.openAddWorkspaceDialog() },
                onRenameClick = { ws -> viewModel.openRenameWorkspaceDialog(ws) },
                onDeleteClick = { ws -> viewModel.initiateDeleteWorkspace(ws) },
                onReorderWorkspaces = { ids -> viewModel.reorderWorkspaces(ids) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 1-Line Dynamic Schedule Capsule (Phase 42)
            // Location Invariant: Positioned strictly below WorkspaceTabBar and immediately above NotesFilterChipsRow
            com.arinara.fotara.feature.schedule.ui.ScheduleCapsule(
                state = uiState.scheduleCapsuleState,
                onClick = { viewModel.openScheduleSheet() }
            )

            // 2. Horizontally scrollable Pill Filter Chips (toggled on demand)
            AnimatedVisibility(
                visible = uiState.isFiltersVisible,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                NotesFilterChipsRow(
                    selectedFilter = uiState.selectedFilter,
                    onFilterSelected = { viewModel.setFilter(it) }
                )
            }

            // 3. Date Grouped Notes List
            if (uiState.dateGroups.isEmpty() && !uiState.isLoading) {
                NotesEmptyState(
                    isFiltering = uiState.selectedFilter != NoteFilterChip.ALL || uiState.searchQuery.isNotBlank(),
                    workspaceName = uiState.selectedWorkspaceName
                )
            } else {
                val isOverlayOpen = itemToDelete != null || itemToRename != null || itemToMove != null || showHeaderOverflowMenu || showFabCreateMenu || pendingCreateAction != null || uiState.showAddWorkspaceDialog || uiState.workspaceToRename != null || uiState.workspaceDeleteStep != WorkspaceDeleteStep.NONE
                val canRefresh = PullToRefreshHelper.canTriggerRefresh(
                    isAtTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0,
                    isMultiSelectActive = false,
                    isSearchFocused = false,
                    isOverlayOpen = isOverlayOpen
                )

                PullToRefreshLayout(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    enabled = canRefresh,
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = bottomOverlayPadding + 80.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalEdgeFade(top = 20.dp)
                    ) {
                        items(uiState.dateGroups, key = { it.date.toString() }) { group ->
                            DateGroupSection(
                                group = group,
                                context = context,
                                onItemClick = { item ->
                                    handleOpenNote(
                                        item = item,
                                        onNavigateToPhoto = onNavigateToPhoto,
                                        onNavigateToDocument = onNavigateToDocument,
                                        onNavigateToTextNote = onNavigateToTextNote,
                                        onNavigateToCanvasNote = onNavigateToCanvasNote,
                                        onOpenDocx = onOpenDocx
                                    )
                                },
                                onRenameClick = { item ->
                                    itemToRename = item
                                    renameInputText = item.title
                                },
                                onMoveClick = { item -> itemToMove = item },
                                onDeleteClick = { item -> itemToDelete = item }
                            )
                        }
                    }
                }
            }
        }


    }

    // Confirmation Dialog for Delete Note
    itemToDelete?.let { item ->
        NotesDeleteConfirmDialog(
            item = item,
            onConfirm = {
                viewModel.deleteNote(item)
                itemToDelete = null
            },
            onDismiss = { itemToDelete = null }
        )
    }

    // Rename Note Dialog
    itemToRename?.let { item ->
        NotesRenameDialog(
            item = item,
            initialText = renameInputText,
            onConfirm = { newName ->
                viewModel.renameNote(item, newName)
                itemToRename = null
            },
            onDismiss = { itemToRename = null }
        )
    }

    // Move to Folder Dialog
    itemToMove?.let { item ->
        NotesMoveDialog(
            item = item,
            folders = uiState.folders,
            workspaces = uiState.workspaces,
            onFolderSelected = { folder ->
                viewModel.moveNote(item, folder.id)
                itemToMove = null
            },
            onDismiss = { itemToMove = null }
        )
    }

    // Quick Folder Selection Dialog for Note Creation from FAB
    pendingCreateAction?.let { action ->
        NotesFolderPickDialog(
            folders = uiState.folders,
            workspaces = uiState.workspaces,
            onFolderSelected = { folder ->
                pendingCreateAction = null
                action(folder)
            },
            onDismiss = { pendingCreateAction = null }
        )
    }

    if (uiState.showAddWorkspaceDialog) {
        AddWorkspaceDialog(
            workspaces = uiState.workspaces,
            onDismiss = { viewModel.closeAddWorkspaceDialog() },
            onCreate = { name, iconKey -> viewModel.createWorkspace(name, iconKey) }
        )
    }

    uiState.workspaceToRename?.let { wsToRename ->
        EditWorkspaceDialog(
            workspace = wsToRename,
            workspaces = uiState.workspaces,
            onDismiss = { viewModel.closeRenameWorkspaceDialog() },
            onSave = { newName, newIconKey -> viewModel.updateWorkspace(wsToRename.id, newName, newIconKey) }
        )
    }

    when (uiState.workspaceDeleteStep) {
        WorkspaceDeleteStep.CONFIRM -> {
            uiState.workspaceToDelete?.let { ws ->
                DeleteWorkspaceConfirmDialog(
                    workspace = ws,
                    stats = uiState.workspaceDeleteStats,
                    onDismiss = { viewModel.dismissDeleteWorkspace() },
                    onConfirmDelete = { alsoDeleteContents ->
                        if (alsoDeleteContents) {
                            viewModel.proceedDeleteChoice()
                        } else {
                            viewModel.deleteWorkspaceMoveFoldersToHome(ws)
                        }
                    }
                )
            }
        }
        WorkspaceDeleteStep.CHOICE -> {
            val ws = uiState.workspaceToDelete
            val stats = uiState.workspaceDeleteStats
            if (ws != null && stats != null) {
                DeleteWorkspaceChoiceDialog(
                    workspace = ws,
                    stats = stats,
                    onDismiss = { viewModel.dismissDeleteWorkspace() },
                    onSelectTrash = { viewModel.executeDeleteWorkspaceContents(ws, permanent = false) },
                    onSelectPermanent = { viewModel.proceedDeletePermanentConfirm() }
                )
            }
        }
        WorkspaceDeleteStep.PERMANENT_CONFIRM -> {
            val ws = uiState.workspaceToDelete
            val stats = uiState.workspaceDeleteStats
            if (ws != null && stats != null) {
                DeleteWorkspacePermanentConfirmDialog(
                    workspace = ws,
                    stats = stats,
                    onDismiss = { viewModel.dismissDeleteWorkspace() },
                    onConfirmDeleteForever = { viewModel.executeDeleteWorkspaceContents(ws, permanent = true) }
                )
            }
        }
        WorkspaceDeleteStep.PROGRESS -> {
            val progress = uiState.workspaceDeleteProgress
            val wsToDelete = uiState.workspaceToDelete
            val deleteWsName = when (wsToDelete?.kind) {
                com.arinara.fotara.data.model.WorkspaceKind.HOME -> stringResource(R.string.workspace_home)
                com.arinara.fotara.data.model.WorkspaceKind.ARCHIVE -> stringResource(R.string.workspace_archive)
                com.arinara.fotara.data.model.WorkspaceKind.CUSTOM -> wsToDelete.name
                null -> ""
            }
            DeleteWorkspaceProgressDialog(
                workspaceName = deleteWsName,
                current = progress?.first ?: 0,
                total = progress?.second ?: 0
            )
        }
        WorkspaceDeleteStep.NONE -> { /* No delete dialog */ }
    }

    // Schedule Management Sheet (Phase 42)
    com.arinara.fotara.feature.schedule.ui.ScheduleManagementSheet(
        isOpen = uiState.isScheduleSheetOpen,
        schedules = uiState.schedules,
        folders = uiState.folders,
        cutoffTimeStr = uiState.scheduleRolloverTime,
        onDismissRequest = { viewModel.closeScheduleSheet() },
        onSaveSchedule = { viewModel.saveSchedule(it) },
        onDeleteSchedule = { viewModel.deleteSchedule(it) },
        onBatchImportSchedules = { viewModel.batchImportSchedules(it) },
        onUpdateCutoffTime = { viewModel.updateScheduleRolloverTime(it) }
    )
}

private fun handleFabCreateSelection(
    folders: List<Folder>,
    action: (Folder) -> Unit,
    setPendingAction: (((Folder) -> Unit)?) -> Unit
) {
    if (folders.isEmpty()) {
        return
    }
    if (folders.size == 1) {
        action(folders[0])
    } else {
        setPendingAction(action)
    }
}

private fun handleOpenNote(
    item: UnifiedNoteItem,
    onNavigateToPhoto: (folderId: Long, subfolderId: Long?, photoId: Long) -> Unit,
    onNavigateToDocument: (folderId: Long, subfolderId: Long?, docId: Long, targetPageIndex: Int?) -> Unit,
    onNavigateToTextNote: (folderId: Long, subfolderId: Long?, noteId: Long) -> Unit,
    onNavigateToCanvasNote: (folderId: Long, subfolderId: Long?, canvasId: Long) -> Unit,
    onOpenDocx: (documentId: Long) -> Unit
) {
    when (item.type) {
        UnifiedNoteType.PHOTO -> onNavigateToPhoto(item.folderId, item.subfolderId, item.id)
        UnifiedNoteType.DOCUMENT -> {
            if (item.documentType == DocumentType.DOCX) {
                onOpenDocx(item.id)
            } else {
                onNavigateToDocument(item.folderId, item.subfolderId, item.id, 0)
            }
        }
        UnifiedNoteType.TEXT -> onNavigateToTextNote(item.folderId, item.subfolderId, item.id)
        UnifiedNoteType.CANVAS -> onNavigateToCanvasNote(item.folderId, item.subfolderId, item.id)
    }
}



@Composable
private fun NotesFilterChipsRow(
    selectedFilter: NoteFilterChip,
    onFilterSelected: (NoteFilterChip) -> Unit,
    modifier: Modifier = Modifier
) {
    val chips = NoteFilterChip.values()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        chips.forEach { chip ->
            val isSelected = chip == selectedFilter
            Box(
                modifier = Modifier
                    .clip(FotaraShapes.Pill)
                    .then(
                        if (isSelected) {
                            Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                        } else {
                            Modifier
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, FotaraShapes.Pill)
                        }
                    )
                    .clickable { onFilterSelected(chip) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(chip.labelRes),
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontFamily = ElmsSans,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun DateGroupSection(
    group: DateGroup,
    context: Context,
    onItemClick: (UnifiedNoteItem) -> Unit,
    onRenameClick: (UnifiedNoteItem) -> Unit,
    onMoveClick: (UnifiedNoteItem) -> Unit,
    onDeleteClick: (UnifiedNoteItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Group Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Colored Dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(group.dotColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Uppercase Pill Label
                Text(
                    text = group.label,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ElmsSans,
                    letterSpacing = 0.5.sp
                )
            }

            // Item count on right
            val countText = if (group.items.size == 1) {
                context.getString(R.string.item_count_singular, 1)
            } else {
                context.getString(R.string.items_count, group.items.size)
            }
            Text(
                text = countText,
                color = HomeSubtitleGray,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Normal
            )
        }

        // Single rounded card container for all items in this group
        AppCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                group.items.forEachIndexed { index, item ->
                    NoteItemRow(
                        item = item,
                        context = context,
                        onClick = { onItemClick(item) },
                        onRenameClick = { onRenameClick(item) },
                        onMoveClick = { onMoveClick(item) },
                        onDeleteClick = { onDeleteClick(item) }
                    )
                    if (index < group.items.size - 1) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(start = 80.dp, end = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteItemRow(
    item: UnifiedNoteItem,
    context: Context,
    onClick: () -> Unit,
    onRenameClick: () -> Unit,
    onMoveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Thumbnail (~60dp)
        NoteThumbnailTile(item = item)

        Spacer(modifier = Modifier.width(12.dp))

        // Middle (3 lines): Title, Folder, Timestamp
        Column(modifier = Modifier.weight(1f)) {
            // Line 1: Title (bold white, single line, ellipsize)
            Text(
                text = item.title,
                color = TextPrimary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Line 2: Small folder icon + subject/folder name
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = item.folderName,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Line 3: Time (formatted according to today/yesterday/older)
            Text(
                text = NotesDateUtils.formatNoteTimestamp(item.addedAt, context),
                color = HomeSubtitleGray,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Light
            )
        }

        // Right: Three-dot overflow menu
        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, FotaraShapes.InputField)
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_open), color = TextPrimary, fontFamily = ElmsSans) },
                    leadingIcon = { Icon(Icons.Outlined.Description, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(20.dp)) },
                    onClick = {
                        showMenu = false
                        onClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_rename), color = TextPrimary, fontFamily = ElmsSans) },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) },
                    onClick = {
                        showMenu = false
                        onRenameClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_move), color = TextPrimary, fontFamily = ElmsSans) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) },
                    onClick = {
                        showMenu = false
                        onMoveClick()
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_delete), color = TagCrimson, fontFamily = ElmsSans) },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = TagCrimson, modifier = Modifier.size(20.dp)) },
                    onClick = {
                        showMenu = false
                        onDeleteClick()
                    }
                )
            }
        }
    }
}

@Composable
private fun NoteThumbnailTile(
    item: UnifiedNoteItem,
    modifier: Modifier = Modifier
) {
    val tileSize = 58.dp
    val tileShape = FotaraShapes.InputField

    when (item.type) {
        UnifiedNoteType.PHOTO -> {
            Box(
                modifier = modifier
                    .size(tileSize)
                    .clip(tileShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = item.previewUri,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        UnifiedNoteType.DOCUMENT -> {
            val docLabel = if (item.documentType == DocumentType.PDF) "PDF" else "DOCX"
            Box(
                modifier = modifier
                    .size(tileSize)
                    .clip(tileShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = docLabel,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = docLabel,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ElmsSans
                    )
                }
            }
        }

        UnifiedNoteType.TEXT -> {
            Box(
                modifier = modifier
                    .size(tileSize)
                    .clip(tileShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = "Text Note",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "TXT",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ElmsSans
                    )
                }
            }
        }

        UnifiedNoteType.CANVAS -> {
            if (!item.previewUri.isNullOrBlank()) {
                Box(
                    modifier = modifier
                        .size(tileSize)
                        .clip(tileShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = item.previewUri,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Box(
                    modifier = modifier
                        .size(tileSize)
                        .clip(tileShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Draw,
                            contentDescription = "Canvas Note",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "CANVAS",
                            color = MaterialTheme.colorScheme.tertiary,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
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
private fun NotesEmptyState(
    isFiltering: Boolean,
    workspaceName: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.empty_notes_title),
            color = TextPrimary,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isFiltering) {
                stringResource(R.string.empty_notes_filter_subtitle)
            } else {
                stringResource(R.string.workspace_notes_empty_state, workspaceName)
            },
            color = TextSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Light,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
