// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.home

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.arinara.fotara.ui.components.LocalBottomOverlayPadding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.ui.components.verticalEdgeFade
import com.arinara.fotara.ui.components.PullToRefreshLayout
import com.arinara.fotara.ui.components.PullToRefreshHelper
import com.arinara.fotara.ui.home.HomeLayoutHelper
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arinara.fotara.R
import com.arinara.fotara.data.model.CanvasNote
import com.arinara.fotara.data.model.DocumentNote
import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.model.PhotoGroup
import com.arinara.fotara.data.model.TextNote
import com.arinara.fotara.online.FeedbackManager
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.theme.FolderBodyBlue
import com.arinara.fotara.theme.HomeCardBorder
import com.arinara.fotara.theme.HomeCardSurface
import com.arinara.fotara.theme.HomeHeaderButtonBg
import com.arinara.fotara.theme.HomeMainButtonBlue
import com.arinara.fotara.theme.HomeNearBlack
import com.arinara.fotara.theme.HomeSubtitleGray
import com.arinara.fotara.theme.TagAmber
import com.arinara.fotara.theme.TagCrimson
import com.arinara.fotara.ui.components.ActiveSearchBar
import com.arinara.fotara.ui.components.BatchRenameDialog
import com.arinara.fotara.ui.components.FeedbackDialog
import com.arinara.fotara.ui.components.FloatingDock
import com.arinara.fotara.ui.components.FolderCard
import com.arinara.fotara.ui.components.FolderUnlockDialog
import com.arinara.fotara.ui.components.HomeBottomNavBar
import com.arinara.fotara.ui.components.HomeNavTab
import com.arinara.fotara.ui.components.NewFolderDialog
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import com.arinara.fotara.ui.home.WorkspaceDeleteStep
import com.arinara.fotara.ui.components.ScreenHeader
import com.arinara.fotara.ui.components.ScreenHeaderActionButton
import com.arinara.fotara.ui.components.ScreenHeaderDefaults
import com.arinara.fotara.ui.home.workspace.WorkspaceTabBar
import com.arinara.fotara.ui.home.workspace.AddWorkspaceDialog
import com.arinara.fotara.ui.home.workspace.RenameWorkspaceDialog
import com.arinara.fotara.ui.home.workspace.MoveToWorkspaceDialog
import com.arinara.fotara.ui.home.workspace.DeleteWorkspaceConfirmDialog
import com.arinara.fotara.ui.home.workspace.DeleteWorkspaceChoiceDialog
import com.arinara.fotara.ui.home.workspace.DeleteWorkspacePermanentConfirmDialog
import com.arinara.fotara.ui.home.workspace.DeleteWorkspaceProgressDialog
import com.arinara.fotara.data.model.WorkspaceKind
import com.arinara.fotara.data.repository.WorkspaceValidator
import com.arinara.fotara.ui.components.ResetFolderPinDialog
import com.arinara.fotara.ui.components.SetFolderLockDialog
import com.arinara.fotara.ui.components.computeFolderGlowAnchors
import com.arinara.fotara.ui.components.computeFolderGlowOrientations
import com.arinara.fotara.ui.settings.SettingsScreen
import com.arinara.fotara.ui.settings.SettingsViewModel

/**
 * Fotara v1.5.2 Home Screen matching IMAGE A specification:
 * - Near-black background (#0A0D14), edge-to-edge
 * - Upright "Fotara" title with "Your notes, organized" subtitle
 * - Header search & overflow circular buttons
 * - Segmented tab bar: All, Favorit, Arsip
 * - 2-column dark folder grid with facing LinkIt corner stroke glow
 * - Non-overlapping bottom stack: FloatingDock + HomeBottomNavBar
 * - Integrated Settings and blank Notes tabs with seamless bottom nav
 */
@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    settingsViewModel: SettingsViewModel? = null,
    notesViewModel: com.arinara.fotara.ui.notes.NotesViewModel? = null,
    updateManager: com.arinara.fotara.online.UpdateManager? = null,
    onFolderClick: (Folder) -> Unit,
    onNavigateToPhoto: (folderId: Long, subfolderId: Long?, photoId: Long) -> Unit = { _, _, _ -> },
    onNavigateToGroup: (folderId: Long, subfolderId: Long?, groupId: Long) -> Unit = { _, _, _ -> },
    onNavigateToDocument: (folderId: Long, subfolderId: Long?, docId: Long, targetPageIndex: Int?) -> Unit = { _, _, _, _ -> },
    onNavigateToTextNote: (folderId: Long, subfolderId: Long?, noteId: Long) -> Unit = { _, _, _ -> },
    onNavigateToCanvasNote: (folderId: Long, subfolderId: Long?, canvasId: Long) -> Unit = { _, _, _ -> },
    onOpenDocx: (documentId: Long) -> Unit = {},
    onOpenTrash: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenUpdates: () -> Unit = {},
    onOpenSupport: () -> Unit = {},
    onOpenWhatsNew: () -> Unit = {},
    feedbackManager: FeedbackManager? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val keyguardManager = remember { context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager }

    var selectedNavTab by remember { mutableStateOf(HomeNavTab.HOME) }

    var activeContextFolder by remember { mutableStateOf<Folder?>(null) }
    var folderToRename by remember { mutableStateOf<Folder?>(null) }
    var renameInputText by remember { mutableStateOf("") }
    var showHomeOverflowMenu by remember { mutableStateOf(false) }
    var showBatchRenameDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var updatePopupRelease by remember { mutableStateOf<com.arinara.fotara.online.ReleaseInfo?>(null) }

    LaunchedEffect(updateManager) {
        if (updateManager != null) {
            try {
                val rel = updateManager.checkForUpdates()
                if (rel != null && updateManager.shouldShowUpdatePopup(rel)) {
                    updatePopupRelease = rel
                }
            } catch (_: Exception) {}
        }
    }

    var folderToUnlock by remember { mutableStateOf<Folder?>(null) }
    var folderToLock by remember { mutableStateOf<Folder?>(null) }
    var folderToResetPin by remember { mutableStateOf<Folder?>(null) }
    var isRemovingLock by remember { mutableStateOf(false) }
    var isResettingPin by remember { mutableStateOf(false) }
    var pendingPhotoSearchResult by remember { mutableStateOf<Photo?>(null) }
    var pendingGroupSearchResult by remember { mutableStateOf<PhotoGroup?>(null) }
    var pendingDocumentSearchResult by remember { mutableStateOf<DocumentNote?>(null) }
    var pendingTextNoteSearchResult by remember { mutableStateOf<TextNote?>(null) }
    var pendingCanvasSearchResult by remember { mutableStateOf<CanvasNote?>(null) }
    var bottomStackHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    val deviceLockLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val target = folderToUnlock ?: folderToResetPin
            if (isResettingPin && target != null) {
                folderToResetPin = target
                folderToUnlock = null
                isResettingPin = false
            } else if (target != null) {
                folderToUnlock = null
                if (isRemovingLock) {
                    isRemovingLock = false
                    viewModel.unlockFolder(target.id)
                } else if (pendingPhotoSearchResult != null) {
                    val photo = pendingPhotoSearchResult!!
                    pendingPhotoSearchResult = null
                    viewModel.onPhotoSearchResultClicked(photo) { folderId, subfolderId, photoId ->
                        onNavigateToPhoto(folderId, subfolderId, photoId)
                    }
                } else if (pendingGroupSearchResult != null) {
                    val group = pendingGroupSearchResult!!
                    pendingGroupSearchResult = null
                    viewModel.onGroupSearchResultClicked(group) { folderId, subfolderId, groupId ->
                        onNavigateToGroup(folderId, subfolderId, groupId)
                    }
                } else if (pendingDocumentSearchResult != null) {
                    val doc = pendingDocumentSearchResult
                    pendingDocumentSearchResult = null
                    if (doc != null) {
                        viewModel.onDocumentSearchResultClicked(doc) { folderId, subfolderId, docId, pageIndex ->
                            onNavigateToDocument(folderId, subfolderId, docId, pageIndex)
                        }
                    }
                } else if (pendingTextNoteSearchResult != null) {
                    val note = pendingTextNoteSearchResult!!
                    pendingTextNoteSearchResult = null
                    viewModel.onTextNoteSearchResultClicked(note) { folderId, subfolderId, noteId ->
                        onNavigateToTextNote(folderId, subfolderId, noteId)
                    }
                } else if (pendingCanvasSearchResult != null) {
                    val canvas = pendingCanvasSearchResult
                    pendingCanvasSearchResult = null
                    if (canvas != null) {
                        viewModel.onCanvasSearchResultClicked(canvas) { folderId, subfolderId, canvasId ->
                            onNavigateToCanvasNote(folderId, subfolderId, canvasId)
                        }
                    }
                } else {
                    onFolderClick(target)
                }
            }
        }
    }

    LaunchedEffect(uiState.userMessage) {
        val msg = uiState.userMessage
        if (msg != null) {
            viewModel.clearUserMessage()
            snackbarHostState.showSnackbar(msg)
        }
    }

    BackHandler(enabled = uiState.isMultiSelectMode) {
        viewModel.exitMultiSelectMode()
    }

    BackHandler(enabled = !uiState.isMultiSelectMode && selectedNavTab != HomeNavTab.HOME) {
        selectedNavTab = HomeNavTab.HOME
    }

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val folderColumns = when {
        screenWidthDp >= 840 -> 5
        screenWidthDp >= 600 -> 4
        else -> 2
    }

    val folderGlowAnchors = remember(uiState.folders, folderColumns) {
        computeFolderGlowAnchors(uiState.folders, folderColumns)
    }

    val isImeVisible = WindowInsets.isImeVisible

    val bottomStackDp = with(density) { bottomStackHeightPx.toDp() }
    val bottomOverlayPaddingDp = if (bottomStackDp > 0.dp) bottomStackDp else 96.dp

    CompositionLocalProvider(LocalBottomOverlayPadding provides bottomOverlayPaddingDp) {
        Scaffold(
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .padding(bottom = bottomOverlayPaddingDp + 8.dp)
                        .imePadding()
                )
            },
            containerColor = HomeNearBlack,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (uiState.isMultiSelectMode) {
                TopAppBar(
                    title = {
                        Text(
                            text = "${uiState.selectedFolderIds.size} Selected",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { viewModel.exitMultiSelectMode() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Exit Multi-Select",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        if (uiState.selectedFolderIds.size in 2..4) {
                            IconButton(
                                onClick = { viewModel.linkSelectedFolders() },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = "Link Folders",
                                    tint = Color.White
                                )
                            }
                        }
                        if (uiState.selectedFolderIds.isNotEmpty()) {
                            IconButton(
                                onClick = { showBatchRenameDialog = true },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Rename Selected Folders",
                                    tint = Color.White
                                )
                            }
                            IconButton(
                                onClick = {
                                    val selected = uiState.folders.filter { it.id in uiState.selectedFolderIds }
                                    viewModel.openMoveFoldersDialog(selected)
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.DriveFileMove,
                                    contentDescription = stringResource(R.string.folder_menu_move_to_workspace),
                                    tint = Color.White
                                )
                            }
                        }
                        IconButton(
                            onClick = { viewModel.selectAllFolders() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = "Select All",
                                tint = Color.White
                            )
                        }
                        IconButton(
                            onClick = { viewModel.invertFolderSelection() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlipToBack,
                                contentDescription = stringResource(R.string.action_invert_selection),
                                tint = Color.White
                            )
                        }
                        IconButton(
                            onClick = { viewModel.requestBulkDelete() },
                            enabled = uiState.selectedFolderIds.isNotEmpty(),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Selected Folders",
                                tint = if (uiState.selectedFolderIds.isNotEmpty()) TagCrimson else Color(0xFF64748B)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = HomeCardSurface)
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(HomeNearBlack)
                .padding(top = if (uiState.isMultiSelectMode) innerPadding.calculateTopPadding() else 0.dp)
        ) {
            // Tab Contents
            when (selectedNavTab) {
                HomeNavTab.SETTINGS -> {
                    if (settingsViewModel != null) {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onBackClick = { selectedNavTab = HomeNavTab.HOME },
                            onNavigateToTrash = onOpenTrash,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(0.dp)
                        )
                    } else {
                        // Fallback if settingsViewModel was not passed
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(HomeNearBlack)
                                .padding(18.dp)
                        ) {
                            Text(
                                text = "Settings",
                                color = Color.White,
                                fontSize = 38.sp,
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                HomeNavTab.NOTES -> {
                    if (notesViewModel != null) {
                        com.arinara.fotara.ui.notes.NotesScreen(
                            viewModel = notesViewModel,
                            onNavigateToPhoto = onNavigateToPhoto,
                            onNavigateToDocument = onNavigateToDocument,
                            onNavigateToTextNote = onNavigateToTextNote,
                            onNavigateToCanvasNote = onNavigateToCanvasNote,
                            onOpenDocx = onOpenDocx,
                            onFolderClick = onFolderClick,
                            onCreatePhotoNote = { folder -> onFolderClick(folder) },
                            onCreateTextNote = { folder -> onNavigateToTextNote(folder.id, null, -1L) },
                            onCreateCanvasNote = { folder -> onNavigateToCanvasNote(folder.id, null, -1L) },
                            onImportDocument = { folder -> onFolderClick(folder) },
                            onOpenSettings = { selectedNavTab = HomeNavTab.SETTINGS },
                            onOpenTrash = onOpenTrash,
                            onOpenUpdates = onOpenUpdates,
                            onOpenWhatsNew = onOpenWhatsNew,
                            onOpenFeedback = { showFeedbackDialog = true },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(HomeNearBlack)
                        )
                    }
                }

                HomeNavTab.HOME -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.statusBars)
                    ) {
                        // Header section: Brand & Top-right circular buttons (hidden during multi-select)
                        if (!uiState.isMultiSelectMode) {
                            ScreenHeader(
                                title = "Fotara",
                                tagline = stringResource(R.string.home_tagline),
                                actions = {
                                    // 1: Magnifier search trigger
                                    ScreenHeaderActionButton(
                                        onClick = { viewModel.activateSearch() },
                                        icon = Icons.Default.Search,
                                        contentDescription = "Search"
                                    )

                                    // 2: Vertical dots menu trigger
                                    Box(
                                        modifier = Modifier.size(ScreenHeaderDefaults.ActionButtonSize),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        ScreenHeaderActionButton(
                                            onClick = { showHomeOverflowMenu = true },
                                            icon = Icons.Default.MoreVert,
                                            contentDescription = "Options"
                                        )

                                        DropdownMenu(
                                            expanded = showHomeOverflowMenu,
                                            onDismissRequest = { showHomeOverflowMenu = false },
                                            modifier = Modifier
                                                .background(HomeCardSurface)
                                                .border(1.dp, HomeCardBorder, RoundedCornerShape(12.dp))
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Select Folders", color = Color.White, fontFamily = ElmsSans, fontWeight = FontWeight.Medium) },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.SelectAll,
                                                        contentDescription = null,
                                                        tint = Color(0xFF60A5FA)
                                                    )
                                                },
                                                onClick = {
                                                    showHomeOverflowMenu = false
                                                    viewModel.enterMultiSelectMode(null)
                                                }
                                            )
                                            HorizontalDivider(color = HomeCardBorder)
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.menu_add_workspace), color = Color.White, fontFamily = ElmsSans, fontWeight = FontWeight.Medium) },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.Add,
                                                        contentDescription = null,
                                                        tint = Color(0xFF60A5FA)
                                                    )
                                                },
                                                onClick = {
                                                    showHomeOverflowMenu = false
                                                    viewModel.openAddWorkspaceDialog()
                                                }
                                            )
                                            HorizontalDivider(color = HomeCardBorder)
                                            DropdownMenuItem(
                                                text = { Text("What's New", color = Color.White, fontFamily = ElmsSans, fontWeight = FontWeight.Medium) },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.NewReleases,
                                                        contentDescription = null,
                                                        tint = TagAmber
                                                    )
                                                },
                                                onClick = {
                                                    showHomeOverflowMenu = false
                                                    onOpenWhatsNew()
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Check for Updates", color = Color.White, fontFamily = ElmsSans, fontWeight = FontWeight.Medium) },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.SystemUpdate,
                                                        contentDescription = null,
                                                        tint = Color.White
                                                    )
                                                },
                                                onClick = {
                                                    showHomeOverflowMenu = false
                                                    onOpenUpdates()
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Send Feedback", color = Color.White, fontFamily = ElmsSans, fontWeight = FontWeight.Medium) },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.Feedback,
                                                        contentDescription = null,
                                                        tint = Color.White
                                                    )
                                                },
                                                onClick = {
                                                    showHomeOverflowMenu = false
                                                    showFeedbackDialog = true
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Support Fotara", color = Color.White, fontFamily = ElmsSans, fontWeight = FontWeight.Medium) },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.VolunteerActivism,
                                                        contentDescription = null,
                                                        tint = TagCrimson
                                                    )
                                                },
                                                onClick = {
                                                    showHomeOverflowMenu = false
                                                    onOpenSupport()
                                                }
                                            )
                                            HorizontalDivider(color = HomeCardBorder)
                                            DropdownMenuItem(
                                                text = { Text("Trash", color = Color.White, fontFamily = ElmsSans, fontWeight = FontWeight.Medium) },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = null,
                                                        tint = TagCrimson
                                                    )
                                                },
                                                onClick = {
                                                    showHomeOverflowMenu = false
                                                    onOpenTrash()
                                                }
                                            )
                                        }
                                    }
                                }
                            )
                        }

                            // Workspace Tab Bar (Phase 27 - Batch 2A / Phase 28 - Batch 2B)
                            WorkspaceTabBar(
                                workspaces = uiState.workspaces,
                                selectedWorkspaceId = uiState.selectedWorkspaceId,
                                isSelectMode = uiState.isMultiSelectMode,
                                onWorkspaceSelected = { ws -> viewModel.selectWorkspace(ws.id) },
                                onAddClick = { viewModel.openAddWorkspaceDialog() },
                                onLimitReached = { viewModel.openAddWorkspaceDialog() },
                                onRenameClick = { ws -> viewModel.openRenameWorkspaceDialog(ws) },
                                onDeleteClick = { ws -> viewModel.initiateDeleteWorkspace(ws) },
                                onReorderWorkspaces = { ids -> viewModel.reorderWorkspaces(ids) }
                            )

                            // Due Tomorrow Deadline Summary (if active)
                            if (uiState.photosDueTomorrow.isNotEmpty()) {
                                HomeHeader(
                                    dueCount = uiState.photosDueTomorrow.size,
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                                )
                            }

                        // Content Area
                        val bottomStackDp = with(density) { bottomStackHeightPx.toDp() }
                        val dynamicBottomPadding = HomeLayoutHelper.computeBottomContentPadding(
                            measuredBottomStackHeightDp = bottomStackDp.value,
                            additionalBufferDp = 16f,
                            fallbackPaddingDp = 170f
                        ).dp

                        val homeGridState = rememberLazyGridState()
                        val isOverlayOpen = folderToUnlock != null || activeContextFolder != null || folderToLock != null || folderToResetPin != null || uiState.showNewFolderDialog || uiState.showBulkDeleteDialog || uiState.showAddWorkspaceDialog || uiState.workspaceToRename != null || uiState.foldersToMoveWorkspace != null || uiState.workspaceDeleteStep != WorkspaceDeleteStep.NONE
                        val canRefresh = PullToRefreshHelper.canTriggerRefresh(
                            isAtTop = homeGridState.firstVisibleItemIndex == 0 && homeGridState.firstVisibleItemScrollOffset == 0,
                            isMultiSelectActive = uiState.isMultiSelectMode,
                            isSearchFocused = uiState.isSearchActive,
                            isOverlayOpen = isOverlayOpen
                        )

                        PullToRefreshLayout(
                            isRefreshing = uiState.isRefreshing,
                            onRefresh = { viewModel.refresh() },
                            enabled = canRefresh,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (uiState.folders.isEmpty()) {
                                val currentWs = uiState.workspaces.firstOrNull { it.id == uiState.selectedWorkspaceId }
                                val wsDisplayName = when (currentWs?.kind) {
                                    WorkspaceKind.HOME -> stringResource(R.string.workspace_home)
                                    WorkspaceKind.ARCHIVE -> stringResource(R.string.workspace_archive)
                                    WorkspaceKind.CUSTOM -> currentWs.name
                                    null -> stringResource(R.string.workspace_home)
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 24.dp, vertical = 60.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.workspace_empty_state_title, wsDisplayName),
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = ElmsSans,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = stringResource(R.string.workspace_empty_state_hint),
                                            color = Color(0xFF9CA3AF),
                                            fontSize = 14.sp,
                                            fontFamily = ElmsSans,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(folderColumns),
                                    state = homeGridState,
                                    contentPadding = PaddingValues(
                                        start = 18.dp,
                                        end = 18.dp,
                                        top = 20.dp,
                                        bottom = dynamicBottomPadding
                                    ),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalEdgeFade(top = 20.dp)
                                ) {
                                    items(uiState.folders, key = { it.id }) { folder ->
                                        FolderCard(
                                            folder = folder,
                                            isSelectionMode = uiState.isMultiSelectMode,
                                            isSelected = uiState.selectedFolderIds.contains(folder.id),
                                            glowAnchors = folderGlowAnchors[folder.id] ?: emptySet(),
                                            onClick = {
                                                if (uiState.isMultiSelectMode) {
                                                    viewModel.toggleFolderSelection(folder.id)
                                                } else if (folder.isLocked) {
                                                    folderToUnlock = folder
                                                } else {
                                                    onFolderClick(folder)
                                                }
                                            },
                                            onPinClick = {
                                                viewModel.togglePinFolder(folder.id)
                                            },
                                            onRenameClick = {
                                                renameInputText = folder.name
                                                folderToRename = folder
                                            },
                                            onMoveToWorkspaceClick = {
                                                viewModel.openMoveFoldersDialog(listOf(folder))
                                            },
                                            onSelectClick = {
                                                viewModel.enterMultiSelectMode(folder.id)
                                            },
                                            onLockClick = {
                                                if (folder.isLocked) {
                                                    isRemovingLock = true
                                                    folderToUnlock = folder
                                                } else {
                                                    folderToLock = folder
                                                }
                                            },
                                            onUnlinkClick = {
                                                viewModel.unlinkFolder(folder.id)
                                            },
                                            onDeleteClick = {
                                                viewModel.deleteFolder(folder.id)
                                            },
                                            onRename = { newName ->
                                                viewModel.renameFolder(folder.id, newName)
                                            },
                                            onCardLongClick = {
                                                if (uiState.isMultiSelectMode) {
                                                    viewModel.toggleFolderSelection(folder.id)
                                                } else {
                                                    activeContextFolder = folder
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Stack: Floating Dock & Bottom Navigation Bar
            // Always above content, protected with gradient scrim to avoid collisions
            if (!uiState.isMultiSelectMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .onSizeChanged { bottomStackHeightPx = it.height }
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    HomeNearBlack.copy(alpha = 0.85f),
                                    HomeNearBlack
                                )
                            )
                        )
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .imePadding()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Search bar + '+' action button row (only visible on HOME tab)
                        if (selectedNavTab == HomeNavTab.HOME) {
                            FloatingDock(
                                onSearchClick = { viewModel.activateSearch() },
                                onNewFolderClick = { viewModel.openNewFolderDialog() }
                            )
                        }

                        // Floating bottom navigation bar (hidden while keyboard is open)
                        if (!isImeVisible) {
                            HomeBottomNavBar(
                                selectedTab = selectedNavTab,
                                onTabSelected = { selectedNavTab = it }
                            )
                        }
                    }
                }
            }

            // New Folder Dialog
            if (uiState.showNewFolderDialog) {
                NewFolderDialog(
                    onDismiss = { viewModel.closeNewFolderDialog() },
                    onCreate = { name, color, isPinned ->
                        viewModel.createFolder(name, color, isPinned)
                    }
                )
            }

            // Single Folder Rename Dialog
            if (folderToRename != null) {
                val f = folderToRename!!
                AlertDialog(
                    onDismissRequest = { folderToRename = null },
                    containerColor = HomeCardSurface,
                    shape = RoundedCornerShape(20.dp),
                    title = {
                        Text(
                            text = "Rename Folder",
                            color = Color.White,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    text = {
                        OutlinedTextField(
                            value = renameInputText,
                            onValueChange = { renameInputText = it },
                            label = { Text("Folder Name", color = HomeSubtitleGray, fontFamily = ElmsSans) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = HomeMainButtonBlue,
                                unfocusedBorderColor = HomeCardBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.renameFolder(f.id, renameInputText)
                                folderToRename = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HomeMainButtonBlue)
                        ) {
                            Text("Save", color = Color.White, fontFamily = ElmsSans, fontWeight = FontWeight.Medium)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { folderToRename = null }) {
                            Text("Cancel", color = HomeSubtitleGray, fontFamily = ElmsSans)
                        }
                    }
                )
            }

            // Card-Level Context Menu (Pin/Unpin, Rename, Lock, Link It / Unlink, Select, Delete)
            if (activeContextFolder != null) {
                val folder = activeContextFolder!!
                AlertDialog(
                    onDismissRequest = { activeContextFolder = null },
                    containerColor = HomeCardSurface,
                    shape = RoundedCornerShape(20.dp),
                    title = {
                        Text(
                            text = folder.name,
                            color = Color.White,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    text = {
                        Column {
                            // Action 1: Pin / Unpin
                            Surface(
                                color = Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.togglePinFolder(folder.id)
                                        activeContextFolder = null
                                    }
                                    .padding(vertical = 12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PushPin,
                                        contentDescription = null,
                                        tint = Color(0xFF60A5FA),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(
                                        text = if (folder.isPinned) "Unpin from Top" else "Pin to Top",
                                        color = Color.White,
                                        fontFamily = ElmsSans,
                                        fontSize = 15.sp
                                    )
                                }
                            }

                            // Action 2: Rename
                            Surface(
                                color = Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        renameInputText = folder.name
                                        folderToRename = folder
                                        activeContextFolder = null
                                    }
                                    .padding(vertical = 12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = Color(0xFF60A5FA),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(
                                        text = "Rename Folder",
                                        color = Color.White,
                                        fontFamily = ElmsSans,
                                        fontSize = 15.sp
                                    )
                                }
                            }

                            // Action 3: Select (Enters Multi-Select Mode)
                            Surface(
                                color = Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val fId = folder.id
                                        activeContextFolder = null
                                        viewModel.enterMultiSelectMode(fId)
                                    }
                                    .padding(vertical = 12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = FolderBodyBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(
                                        text = "Select",
                                        color = Color.White,
                                        fontFamily = ElmsSans,
                                        fontSize = 15.sp
                                    )
                                }
                            }

                            // Action 4: Lock / Unlock Folder
                            Surface(
                                color = Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val f = folder
                                        activeContextFolder = null
                                        if (f.isLocked) {
                                            isRemovingLock = true
                                            folderToUnlock = f
                                        } else {
                                            folderToLock = f
                                        }
                                    }
                                    .padding(vertical = 12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (folder.isLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = if (folder.isLocked) TagAmber else Color(0xFF60A5FA),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(
                                        text = if (folder.isLocked) "Remove Folder Lock" else "Lock Folder (PIN)",
                                        color = Color.White,
                                        fontFamily = ElmsSans,
                                        fontSize = 15.sp
                                    )
                                }
                            }

                            if (folder.isLocked) {
                                // Action 5: Change PIN
                                Surface(
                                    color = Color.Transparent,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val f = folder
                                            activeContextFolder = null
                                            isResettingPin = true
                                            folderToUnlock = f
                                        }
                                        .padding(vertical = 12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Password,
                                            contentDescription = null,
                                            tint = Color(0xFF60A5FA),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Text(
                                            text = "Change Folder PIN",
                                            color = Color.White,
                                            fontFamily = ElmsSans,
                                            fontSize = 15.sp
                                        )
                                    }
                                }
                            }

                            // Action 6: Link It / Unlink
                            if (folder.linkGroupId != null) {
                                Surface(
                                    color = Color.Transparent,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.unlinkFolder(folder.id)
                                            activeContextFolder = null
                                        }
                                        .padding(vertical = 12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.LinkOff,
                                            contentDescription = null,
                                            tint = TagCrimson,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Text(
                                            text = "Unlink Folder",
                                            color = TagCrimson,
                                            fontFamily = ElmsSans,
                                            fontSize = 15.sp
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    color = Color.Transparent,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val fId = folder.id
                                            activeContextFolder = null
                                            viewModel.enterMultiSelectMode(fId)
                                        }
                                        .padding(vertical = 12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Link,
                                            contentDescription = null,
                                            tint = Color(0xFF60A5FA),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Text(
                                            text = "Link to another folder",
                                            color = Color.White,
                                            fontFamily = ElmsSans,
                                            fontSize = 15.sp
                                        )
                                    }
                                }
                            }

                            // Action 7: Move to Trash / Delete
                            Surface(
                                color = Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val fId = folder.id
                                        activeContextFolder = null
                                        viewModel.deleteFolder(fId)
                                    }
                                    .padding(vertical = 12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = TagCrimson,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(
                                        text = "Move to Trash",
                                        color = TagCrimson,
                                        fontFamily = ElmsSans,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { activeContextFolder = null }) {
                            Text("Cancel", color = HomeSubtitleGray, fontFamily = ElmsSans)
                        }
                    }
                )
            }

            // Folder Privacy Lock Dialogs
            if (folderToUnlock != null) {
                val folder = folderToUnlock!!
                FolderUnlockDialog(
                    folder = folder,
                    onDismiss = {
                        folderToUnlock = null
                        pendingPhotoSearchResult = null
                        pendingGroupSearchResult = null
                        isRemovingLock = false
                        isResettingPin = false
                    },
                    onUnlocked = {
                        val target = folderToUnlock!!
                        folderToUnlock = null
                        when {
                            isRemovingLock -> {
                                isRemovingLock = false
                                viewModel.unlockFolder(target.id)
                            }
                            isResettingPin -> {
                                isResettingPin = false
                                folderToResetPin = target
                            }
                            pendingGroupSearchResult != null -> {
                                val group = pendingGroupSearchResult!!
                                pendingGroupSearchResult = null
                                viewModel.onGroupSearchResultClicked(group) { folderId, subfolderId, groupId ->
                                    onNavigateToGroup(folderId, subfolderId, groupId)
                                }
                            }
                            pendingPhotoSearchResult != null -> {
                                val photo = pendingPhotoSearchResult!!
                                pendingPhotoSearchResult = null
                                viewModel.onPhotoSearchResultClicked(photo) { folderId, subfolderId, photoId ->
                                    onNavigateToPhoto(folderId, subfolderId, photoId)
                                }
                            }
                            pendingDocumentSearchResult != null -> {
                                val doc = pendingDocumentSearchResult
                                pendingDocumentSearchResult = null
                                if (doc != null) {
                                    viewModel.onDocumentSearchResultClicked(doc) { folderId, subfolderId, docId, pageIndex ->
                                        onNavigateToDocument(folderId, subfolderId, docId, pageIndex)
                                    }
                                }
                            }
                            pendingTextNoteSearchResult != null -> {
                                val note = pendingTextNoteSearchResult!!
                                pendingTextNoteSearchResult = null
                                viewModel.onTextNoteSearchResultClicked(note) { folderId, subfolderId, noteId ->
                                    onNavigateToTextNote(folderId, subfolderId, noteId)
                                }
                            }
                            pendingCanvasSearchResult != null -> {
                                val canvas = pendingCanvasSearchResult
                                pendingCanvasSearchResult = null
                                if (canvas != null) {
                                    viewModel.onCanvasSearchResultClicked(canvas) { folderId, subfolderId, canvasId ->
                                        onNavigateToCanvasNote(folderId, subfolderId, canvasId)
                                    }
                                }
                            }
                            else -> {
                                onFolderClick(target)
                            }
                        }
                    },
                    onUseDeviceLock = {
                        if (keyguardManager?.isDeviceSecure == true) {
                            val intent = keyguardManager.createConfirmDeviceCredentialIntent(
                                "Unlock ${folder.name}",
                                "Confirm device screen lock or biometrics to access this folder"
                            )
                            if (intent != null) {
                                deviceLockLauncher.launch(intent)
                            }
                        }
                    },
                    onForgotPin = {
                        if (keyguardManager?.isDeviceSecure == true) {
                            isResettingPin = true
                            val intent = keyguardManager.createConfirmDeviceCredentialIntent(
                                "Reset PIN for ${folder.name}",
                                "Confirm device screen lock or biometrics to reset your folder PIN"
                            )
                            if (intent != null) {
                                deviceLockLauncher.launch(intent)
                            }
                        }
                    }
                )
            }

            if (folderToLock != null) {
                val folder = folderToLock!!
                SetFolderLockDialog(
                    folder = folder,
                    onDismiss = { folderToLock = null },
                    onConfirmPin = { pin ->
                        viewModel.lockFolder(folder.id, pin)
                        folderToLock = null
                    }
                )
            }

            if (folderToResetPin != null) {
                val folder = folderToResetPin!!
                ResetFolderPinDialog(
                    folder = folder,
                    onDismiss = { folderToResetPin = null },
                    onSaveNewPin = { newPin ->
                        viewModel.updateFolderPin(folder.id, newPin)
                        folderToResetPin = null
                    },
                    onRemoveLock = {
                        viewModel.unlockFolder(folder.id)
                        folderToResetPin = null
                    }
                )
            }

            // Bulk Delete Confirmation Dialog
            if (uiState.showBulkDeleteDialog && uiState.bulkDeleteStats != null) {
                val stats = uiState.bulkDeleteStats!!
                val formattedMb = "%.1f MB".format(stats.totalSizeBytes / (1024f * 1024f))
                AlertDialog(
                    onDismissRequest = { viewModel.dismissBulkDeleteDialog() },
                    containerColor = HomeCardSurface,
                    shape = RoundedCornerShape(20.dp),
                    title = {
                        Text(
                            text = "Move ${stats.folderCount} Folder${if (stats.folderCount > 1) "s" else ""} to Trash?",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = "Total Notes & Photos: ${stats.photoCount}",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Storage occupied: $formattedMb",
                                color = HomeSubtitleGray,
                                fontSize = 13.sp,
                                fontFamily = ElmsSans
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Items moved to Trash can be restored within 30 days before being automatically purged. Associated deadline reminders will be cancelled.",
                                color = HomeSubtitleGray,
                                fontSize = 12.sp,
                                fontFamily = ElmsSans,
                                lineHeight = 16.sp
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.confirmBulkDelete() },
                            colors = ButtonDefaults.buttonColors(containerColor = TagCrimson)
                        ) {
                            Text("Move to Trash", color = Color.White, fontFamily = ElmsSans, fontWeight = FontWeight.Medium)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.dismissBulkDeleteDialog() }) {
                            Text("Cancel", color = HomeSubtitleGray, fontFamily = ElmsSans)
                        }
                    }
                )
            }

            // Keyboard-Docked Search Bar Overlay
            if (uiState.isSearchActive) {
                ActiveSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChanged(it) },
                    onDismiss = { viewModel.deactivateSearch() },
                    isLoading = uiState.isSearchLoading,
                    folderResults = uiState.folderSearchResults,
                    groupResults = uiState.groupSearchResults,
                    photoResults = uiState.searchResults,
                    textNoteResults = uiState.textNoteSearchResults,
                    documentResults = uiState.documentSearchResults,
                    canvasNoteResults = uiState.canvasNoteSearchResults,
                    recentSearches = uiState.recentSearches,
                    workspaces = uiState.workspaces,
                    selectedWorkspaceScopeId = uiState.searchWorkspaceScopeId,
                    onSelectWorkspaceScope = { viewModel.setSearchWorkspaceScope(it) },
                    selectedDateFilter = uiState.searchDateFilter,
                    onSelectDateFilter = { viewModel.setDateFilter(it) },
                    selectedColorFilter = uiState.searchColorFilter,
                    onSelectColorFilter = { viewModel.setColorFilter(it) },
                    smartTags = uiState.availableSmartTags,
                    selectedSmartTag = uiState.selectedSmartTag,
                    onSelectSmartTag = { viewModel.selectSmartTag(it) },
                    sortOrder = uiState.searchSortOrder,
                    onToggleSortOrder = {
                        val next = if (uiState.searchSortOrder == com.arinara.fotara.data.model.SearchSortOrder.NEWEST_ADDED) {
                            com.arinara.fotara.data.model.SearchSortOrder.OLDEST_ADDED
                        } else {
                            com.arinara.fotara.data.model.SearchSortOrder.NEWEST_ADDED
                        }
                        viewModel.setSortOrder(next)
                    },
                    customDateRange = uiState.customDateRange,
                    onSelectCustomDateRange = { viewModel.setCustomDateRange(it) },
                    onClearFilters = { viewModel.clearFilters() },
                    onSearchSubmitted = { viewModel.submitSearch(it) },
                    onRemoveRecentSearch = { viewModel.removeRecentSearch(it) },
                    onClearRecentSearches = { viewModel.clearRecentSearches() },
                    onFolderClick = { folderId ->
                        val targetFolder = uiState.folders.firstOrNull { it.id == folderId }
                        if (targetFolder != null) {
                            if (targetFolder.isLocked) {
                                folderToUnlock = targetFolder
                            } else {
                                viewModel.submitSearch(uiState.searchQuery)
                                viewModel.deactivateSearch()
                                onFolderClick(targetFolder)
                            }
                        }
                    },
                    onGroupClick = { group ->
                        val targetFolder = uiState.folders.firstOrNull { it.id == group.folderId }
                        if (targetFolder != null && targetFolder.isLocked) {
                            pendingGroupSearchResult = group
                            folderToUnlock = targetFolder
                        } else {
                            viewModel.onGroupSearchResultClicked(group) { folderId, subfolderId, groupId ->
                                onNavigateToGroup(folderId, subfolderId, groupId)
                            }
                        }
                    },
                    onPhotoClick = { photo ->
                        val targetFolder = uiState.folders.firstOrNull { it.id == photo.folderId }
                        if (targetFolder != null && targetFolder.isLocked) {
                            pendingPhotoSearchResult = photo
                            folderToUnlock = targetFolder
                        } else {
                            viewModel.onPhotoSearchResultClicked(photo) { folderId, subfolderId, photoId ->
                                onNavigateToPhoto(folderId, subfolderId, photoId)
                            }
                        }
                    },
                    onTextNoteClick = { note ->
                        val targetFolder = uiState.folders.firstOrNull { it.id == note.folderId }
                        if (targetFolder != null && targetFolder.isLocked) {
                            pendingTextNoteSearchResult = note
                            folderToUnlock = targetFolder
                        } else {
                            viewModel.onTextNoteSearchResultClicked(note) { folderId, subfolderId, noteId ->
                                onNavigateToTextNote(folderId, subfolderId, noteId)
                            }
                        }
                    },
                    onDocumentClick = { doc ->
                        val targetFolder = uiState.folders.firstOrNull { it.id == doc.folderId }
                        if (targetFolder != null && targetFolder.isLocked) {
                            pendingDocumentSearchResult = doc
                            folderToUnlock = targetFolder
                        } else {
                            viewModel.onDocumentSearchResultClicked(doc) { folderId, subfolderId, docId, pageIndex ->
                                onNavigateToDocument(folderId, subfolderId, docId, pageIndex)
                            }
                        }
                    },
                    onCanvasClick = { canvas ->
                        val targetFolder = uiState.folders.firstOrNull { it.id == canvas.folderId }
                        if (targetFolder != null && targetFolder.isLocked) {
                            pendingCanvasSearchResult = canvas
                            folderToUnlock = targetFolder
                        } else {
                            viewModel.onCanvasSearchResultClicked(canvas) { folderId, subfolderId, canvasId ->
                                onNavigateToCanvasNote(folderId, subfolderId, canvasId)
                            }
                        }
                    }
                )
            }
        }
    }

    if (showBatchRenameDialog) {
        BatchRenameDialog(
            itemCount = uiState.selectedFolderIds.size,
            initialBaseName = uiState.folders.firstOrNull { it.id in uiState.selectedFolderIds }?.name ?: "",
            onConfirm = { baseName ->
                viewModel.batchRenameFolders(baseName)
                showBatchRenameDialog = false
            },
            onDismiss = { showBatchRenameDialog = false }
        )
    }

    if (showFeedbackDialog && feedbackManager != null) {
        FeedbackDialog(
            feedbackManager = feedbackManager,
            onDismiss = { showFeedbackDialog = false }
        )
    }

    updatePopupRelease?.let { release ->
        com.arinara.fotara.ui.components.NewUpdateDialog(
            release = release,
            onLater = {
                updateManager?.isPopupDismissedForSession = true
                updatePopupRelease = null
            },
            onSkipVersion = {
                updateManager?.setSkippedVersion(release.version)
                updateManager?.isPopupDismissedForSession = true
                updatePopupRelease = null
            }
        )
    }

    if (uiState.showAddWorkspaceDialog) {
        AddWorkspaceDialog(
            workspaces = uiState.workspaces,
            onDismiss = { viewModel.closeAddWorkspaceDialog() },
            onCreate = { name -> viewModel.createWorkspace(name) }
        )
    }

    uiState.workspaceToRename?.let { wsToRename ->
        RenameWorkspaceDialog(
            workspace = wsToRename,
            workspaces = uiState.workspaces,
            onDismiss = { viewModel.closeRenameWorkspaceDialog() },
            onRename = { newName -> viewModel.renameWorkspace(wsToRename.id, newName) }
        )
    }

    uiState.foldersToMoveWorkspace?.let { folders ->
        MoveToWorkspaceDialog(
            folders = folders,
            workspaces = uiState.workspaces,
            onDismiss = { viewModel.closeMoveFoldersDialog() },
            onMove = { targetWsId ->
                viewModel.moveFoldersToWorkspace(folders.map { it.id }, targetWsId)
            }
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
            DeleteWorkspaceProgressDialog(
                workspaceName = uiState.workspaceToDelete?.name ?: "",
                current = progress?.first ?: 0,
                total = progress?.second ?: 0
            )
        }
        WorkspaceDeleteStep.NONE -> { /* No delete dialog */ }
    }
    }
}

@Composable
private fun HomeHeader(
    dueCount: Int,
    modifier: Modifier = Modifier
) {
    if (dueCount > 0) {
        Column(modifier = modifier.fillMaxWidth()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = TagAmber.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TagAmber.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Due Tomorrow",
                        tint = TagAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "$dueCount note${if (dueCount > 1) "s" else ""} due tomorrow",
                        color = TagAmber,
                        fontSize = 13.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
