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
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.animation.core.animateFloatAsState
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.arinara.fotara.ui.components.LocalBottomOverlayPadding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.ui.graphics.graphicsLayer
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
import com.arinara.fotara.theme.TextPrimary
import com.arinara.fotara.ui.components.ActiveSearchBar
import com.arinara.fotara.ui.components.BatchRenameDialog
import com.arinara.fotara.ui.components.FeedbackDialog
import com.arinara.fotara.ui.components.FloatingDock
import com.arinara.fotara.ui.components.FloatingSearchBarPill
import com.arinara.fotara.ui.components.SharedFloatingAddButton
import com.arinara.fotara.ui.home.TabContainerBottomState
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.foundation.lazy.LazyColumn
import com.arinara.fotara.ui.components.workspaceGroupedFolderItems
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
import com.arinara.fotara.ui.home.workspace.EditWorkspaceDialog
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
import com.arinara.fotara.data.model.Space
import com.arinara.fotara.data.repository.SpaceRepository
import com.arinara.fotara.feature.space.ActiveSpaceManager
import com.arinara.fotara.feature.packages.loader.FotaraPackageManager
import com.arinara.fotara.feature.friends.data.FriendsRepository
import com.arinara.fotara.ui.space.SpaceSwitcherBottomSheet
import com.arinara.fotara.feature.academic.syllabus.SyllabusComponent
import com.arinara.fotara.feature.academic.syllabus.SyllabusEvaluatorSheet
import androidx.compose.material.icons.outlined.School

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
    onNavigateToDocument: (folderId: Long, subfolderId: Long?, docId: Long, targetPageIndex: Int?, searchQuery: String?) -> Unit = { _, _, _, _, _ -> },
    onNavigateToTextNote: (folderId: Long, subfolderId: Long?, noteId: Long, highlightQuery: String?) -> Unit = { _, _, _, _ -> },
    onNavigateToCanvasNote: (folderId: Long, subfolderId: Long?, canvasId: Long) -> Unit = { _, _, _ -> },
    onOpenDocx: (documentId: Long) -> Unit = {},
    onOpenTrash: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenUpdates: () -> Unit = {},
    onOpenSupport: () -> Unit = {},
    onOpenWhatsNew: () -> Unit = {},
    onOpenSpaceSettings: (spaceId: Long) -> Unit = {},
    feedbackManager: FeedbackManager? = null,
    dialogCoordinator: com.arinara.fotara.coordinator.AppDialogCoordinator? = null,
    spaceRepository: SpaceRepository? = null,
    activeSpaceManager: ActiveSpaceManager? = null,
    packageManager: FotaraPackageManager? = null,
    friendsRepository: FriendsRepository? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val keyguardManager = remember { context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager }

    var selectedNavTab by rememberSaveable { mutableStateOf(HomeNavTab.HOME) }

    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val homeGridState = rememberLazyGridState()
    val notesListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val settingsScrollState = androidx.compose.foundation.rememberScrollState()
    var settingsResetToRootTrigger by rememberSaveable { mutableIntStateOf(0) }
    var isSettingsSubScreenOpen by rememberSaveable { mutableStateOf(false) }
    val saveableStateHolder = androidx.compose.runtime.saveable.rememberSaveableStateHolder()

    var showSpaceSwitcher by remember { mutableStateOf(false) }
    val spaces by (spaceRepository?.observeSpaces() ?: kotlinx.coroutines.flow.flowOf(emptyList())).collectAsStateWithLifecycle(emptyList())
    val activeSpace by (spaceRepository?.activeSpace ?: kotlinx.coroutines.flow.flowOf(Space.DEFAULT_SPACE)).collectAsStateWithLifecycle(Space.DEFAULT_SPACE)

    var activeContextFolder by remember { mutableStateOf<Folder?>(null) }
    var folderToRename by remember { mutableStateOf<Folder?>(null) }
    var renameInputText by remember { mutableStateOf("") }
    var showHomeOverflowMenu by remember { mutableStateOf(false) }
    var showSyllabusEvaluator by remember { mutableStateOf(false) }
    var showBatchRenameDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }

    LaunchedEffect(updateManager) {
        if (updateManager != null) {
            try {
                val rel = updateManager.checkForUpdates()
                if (rel != null && updateManager.shouldShowUpdatePopup(rel)) {
                    dialogCoordinator?.requestDialog(
                        com.arinara.fotara.coordinator.AppDialogRequest.Update(
                            release = rel,
                            onLater = {
                                updateManager.isPopupDismissedForSession = true
                            },
                            onSkipVersion = {
                                updateManager.setSkippedVersion(rel.version)
                                updateManager.isPopupDismissedForSession = true
                            }
                        )
                    )
                }
            } catch (e: Exception) {
                android.util.Log.w("HomeScreen", "Background update check failed", e)
            }
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
    val bottomBarState = remember { TabContainerBottomState() }
    var showNotesCreateMenu by remember { mutableStateOf(false) }
    var pendingNotesCreateAction by remember { mutableStateOf<((Folder) -> Unit)?>(null) }
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
                            onNavigateToDocument(folderId, subfolderId, docId, pageIndex, uiState.searchQuery)
                        }
                    }
                } else if (pendingTextNoteSearchResult != null) {
                    val note = pendingTextNoteSearchResult!!
                    pendingTextNoteSearchResult = null
                    viewModel.onTextNoteSearchResultClicked(note) { folderId, subfolderId, noteId ->
                        onNavigateToTextNote(folderId, subfolderId, noteId, uiState.searchQuery)
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

    val bottomOverlayPaddingDp = bottomBarState.bottomOverlayPaddingDp

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
        topBar = {},
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(HomeNearBlack)
                .padding(top = 0.dp)
        ) {
            // Tab Contents (Wrapped with SaveableStateHolder to preserve sub-screen states across tabs)
            saveableStateHolder.SaveableStateProvider(selectedNavTab) {
                when (selectedNavTab) {
                    HomeNavTab.SETTINGS -> {
                        if (settingsViewModel != null) {
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                friendsRepository = friendsRepository,
                                packageManager = packageManager,
                                onBackClick = { selectedNavTab = HomeNavTab.HOME },
                                onNavigateToTrash = onOpenTrash,
                                onOpenUpdateScreen = onOpenUpdates,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(0.dp),
                                resetToRootTrigger = settingsResetToRootTrigger,
                                onSubScreenStateChanged = { isSettingsSubScreenOpen = it },
                                scrollState = settingsScrollState
                            )
                        } else {
                            // Fallback if settingsViewModel was not passed
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(HomeNearBlack)
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Settings",
                                    color = TextPrimary,
                                    fontSize = 22.sp,
                                    lineHeight = 28.sp,
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    HomeNavTab.NOTES -> {
                        if (notesViewModel != null) {
                            com.arinara.fotara.ui.notes.NotesScreen(
                                viewModel = notesViewModel,
                                onNavigateToPhoto = onNavigateToPhoto,
                                onNavigateToDocument = { fId, sId, dId, pIdx -> onNavigateToDocument(fId, sId, dId, pIdx, null) },
                                onNavigateToTextNote = { fId, sId, nId -> onNavigateToTextNote(fId, sId, nId, null) },
                                onNavigateToCanvasNote = onNavigateToCanvasNote,
                                onOpenDocx = onOpenDocx,
                                onFolderClick = onFolderClick,
                                onCreatePhotoNote = { folder -> onFolderClick(folder) },
                                onCreateTextNote = { folder -> onNavigateToTextNote(folder.id, null, -1L, null) },
                                onCreateCanvasNote = { folder -> onNavigateToCanvasNote(folder.id, null, -1L) },
                                onImportDocument = { folder -> onFolderClick(folder) },
                                onOpenSettings = { selectedNavTab = HomeNavTab.SETTINGS },
                                onOpenTrash = onOpenTrash,
                                onOpenUpdates = onOpenUpdates,
                                onOpenWhatsNew = onOpenWhatsNew,
                                onOpenFeedback = { showFeedbackDialog = true },
                                modifier = Modifier.fillMaxSize(),
                                listState = notesListState
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
                        // Dynamic Header Slot: ScreenHeader and Multi-Select action header overlay
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                        ) {
                            // Normal Header (Brand & actions)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        alpha = if (uiState.isMultiSelectMode) 0f else 1f
                                    }
                            ) {
                                ScreenHeader(
                                    title = "Fotara",
                                    spaceName = activeSpace.name,
                                    onTitleClick = {
                                        if (spaceRepository != null) {
                                            showSpaceSwitcher = true
                                        }
                                    },
                                    onTitleLongClick = {
                                        if (activeSpaceManager != null) {
                                            coroutineScope.launch {
                                                val isUnlocked = activeSpaceManager.toggleStealthVault()
                                                snackbarHostState.showSnackbar(
                                                    if (isUnlocked) "Stealth Space Vault Unlocked" else "Stealth Space Vault Locked"
                                                )
                                            }
                                        }
                                    },
                                    tagline = stringResource(R.string.home_tagline),
                                    actions = {
                                        // 1: Magnifier search trigger
                                        ScreenHeaderActionButton(
                                            onClick = { if (!uiState.isMultiSelectMode) viewModel.activateSearch() },
                                            icon = Icons.Default.Search,
                                            contentDescription = "Search"
                                        )

                                        // 2: Vertical dots menu trigger
                                        Box(
                                            modifier = Modifier.size(ScreenHeaderDefaults.ActionButtonSize),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            ScreenHeaderActionButton(
                                                onClick = { if (!uiState.isMultiSelectMode) showHomeOverflowMenu = true },
                                                icon = Icons.Default.MoreVert,
                                                contentDescription = "Options"
                                            )

                                            DropdownMenu(
                                                expanded = showHomeOverflowMenu && !uiState.isMultiSelectMode,
                                                onDismissRequest = { showHomeOverflowMenu = false },
                                                modifier = Modifier
                                                    .background(HomeCardSurface)
                                                    .border(1.dp, HomeCardBorder, RoundedCornerShape(12.dp))
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Select", color = TextPrimary, fontFamily = ElmsSans, fontWeight = FontWeight.Medium) },
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
                                                    text = { Text("What's New", color = TextPrimary, fontFamily = ElmsSans, fontWeight = FontWeight.Medium) },
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
                                                    text = { Text("Check for Updates", color = TextPrimary, fontFamily = ElmsSans, fontWeight = FontWeight.Medium) },
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = Icons.Default.SystemUpdate,
                                                            contentDescription = null,
                                                            tint = TextPrimary
                                                        )
                                                    },
                                                    onClick = {
                                                        showHomeOverflowMenu = false
                                                        onOpenUpdates()
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Send Feedback", color = TextPrimary, fontFamily = ElmsSans, fontWeight = FontWeight.Medium) },
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = Icons.Default.Feedback,
                                                            contentDescription = null,
                                                            tint = TextPrimary
                                                        )
                                                    },
                                                    onClick = {
                                                        showHomeOverflowMenu = false
                                                        showFeedbackDialog = true
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Support Fotara", color = TextPrimary, fontFamily = ElmsSans, fontWeight = FontWeight.Medium) },
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
                                                    text = { Text("Trash", color = TextPrimary, fontFamily = ElmsSans, fontWeight = FontWeight.Medium) },
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

                            // Multi-Select Action Bar Overlay
                            if (uiState.isMultiSelectMode) {
                                Row(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { viewModel.exitMultiSelectMode() },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Exit Multi-Select",
                                            tint = TextPrimary
                                        )
                                    }
                                    Text(
                                        text = "${uiState.selectedFolderIds.size} Selected",
                                        color = TextPrimary,
                                        fontSize = 16.sp,
                                        lineHeight = 22.sp,
                                        fontFamily = ElmsSans,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(start = 8.dp)
                                    )
                                    if (uiState.selectedFolderIds.size in 2..4) {
                                        IconButton(
                                            onClick = { viewModel.linkSelectedFolders() },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Link,
                                                contentDescription = "Link Folders",
                                                tint = TextPrimary
                                            )
                                        }
                                    }
                                    if (uiState.selectedFolderIds.isNotEmpty()) {
                                        IconButton(
                                            onClick = { showBatchRenameDialog = true },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Rename Selected Folders",
                                                tint = TextPrimary
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                val selected = uiState.folders.filter { it.id in uiState.selectedFolderIds }
                                                viewModel.openMoveFoldersDialog(selected)
                                            },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.DriveFileMove,
                                                contentDescription = stringResource(R.string.folder_menu_move_to_workspace),
                                                tint = TextPrimary
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { viewModel.selectAllFolders() },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SelectAll,
                                            contentDescription = "Select All",
                                            tint = TextPrimary
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.invertFolderSelection() },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FlipToBack,
                                            contentDescription = stringResource(R.string.action_invert_selection),
                                            tint = TextPrimary
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.requestBulkDelete() },
                                        enabled = uiState.selectedFolderIds.isNotEmpty(),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Selected Folders",
                                            tint = if (uiState.selectedFolderIds.isNotEmpty()) TagCrimson else Color(0xFF64748B)
                                        )
                                    }
                                }
                            }
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
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }

                        // Content Area
                        val bottomStackDp = bottomBarState.bottomOverlayPaddingDp
                        val dynamicBottomPadding = HomeLayoutHelper.computeBottomContentPadding(
                            measuredBottomStackHeightDp = bottomStackDp.value,
                            additionalBufferDp = 16f,
                            fallbackPaddingDp = 170f
                        ).dp

                        val isOverlayOpen = folderToUnlock != null || activeContextFolder != null || folderToLock != null || folderToResetPin != null || uiState.showNewFolderDialog || uiState.showBulkDeleteDialog || uiState.showAddWorkspaceDialog || uiState.workspaceToRename != null || uiState.foldersToMoveWorkspace != null || uiState.workspaceDeleteStep != WorkspaceDeleteStep.NONE || pendingNotesCreateAction != null
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
                                            color = TextPrimary,
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
                                        start = 16.dp,
                                        end = 16.dp,
                                        top = 20.dp,
                                        bottom = dynamicBottomPadding
                                    ),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalEdgeFade(top = 20.dp)
                                ) {
                                    items(
                                        items = uiState.folders,
                                        key = { it.id },
                                        contentType = { "folder_card" }
                                    ) { folder ->
                                        FolderCard(
                                            folder = folder,
                                            isSelectionMode = uiState.isMultiSelectMode,
                                            isSelected = viewModel.selectedFolderMap[folder.id] == true,
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
        }

        // Bottom Stack: Persistent Floating Dock & Bottom Navigation Bar
            // Kept measured with graphicsLayer alpha to prevent content padding shifts
            val fabAlpha by animateFloatAsState(
                targetValue = if (selectedNavTab == HomeNavTab.SETTINGS) 0f else 1f,
                label = "fabAlpha"
            )
            val searchPillAlpha by animateFloatAsState(
                targetValue = if (selectedNavTab == HomeNavTab.HOME) 1f else 0f,
                label = "searchPillAlpha"
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .onSizeChanged {
                        if (it.height > 0) {
                            bottomBarState.onHeightMeasured(it.height, density)
                        }
                    }
                    .graphicsLayer {
                        alpha = if (uiState.isMultiSelectMode) 0f else 1f
                    }
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
                        .padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Unified Floating Action Row: search pill (Home) + single persistent (+) button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Search Bar Pill (visible on Home, placeholder space preserved on Notes to maintain row geometry)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .graphicsLayer {
                                    alpha = searchPillAlpha
                                }
                        ) {
                            if (searchPillAlpha > 0.01f) {
                                FloatingSearchBarPill(
                                    onClick = {
                                        if (!uiState.isMultiSelectMode && selectedNavTab == HomeNavTab.HOME) {
                                            viewModel.activateSearch()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Right: Single (+) Action Button hosted at tab container level
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            SharedFloatingAddButton(
                                onClick = {
                                    if (!uiState.isMultiSelectMode) {
                                        when (selectedNavTab) {
                                            HomeNavTab.HOME -> viewModel.openNewFolderDialog()
                                            HomeNavTab.NOTES -> showNotesCreateMenu = true
                                            HomeNavTab.SETTINGS -> { /* hidden */ }
                                        }
                                    }
                                },
                                contentDescription = when (selectedNavTab) {
                                    HomeNavTab.NOTES -> stringResource(R.string.create_note)
                                    else -> stringResource(R.string.menu_new_folder)
                                },
                                modifier = Modifier.graphicsLayer {
                                    alpha = fabAlpha
                                }
                            )

                            // Upward menu for (+) note creation on Notes tab
                            DropdownMenu(
                                expanded = showNotesCreateMenu && selectedNavTab == HomeNavTab.NOTES,
                                onDismissRequest = { showNotesCreateMenu = false },
                                modifier = Modifier
                                    .background(HomeCardSurface)
                                    .border(1.dp, HomeCardBorder, RoundedCornerShape(12.dp))
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_new_photo_note), color = TextPrimary, fontFamily = ElmsSans, fontWeight = FontWeight.Medium) },
                                    leadingIcon = { Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(20.dp)) },
                                    onClick = {
                                        showNotesCreateMenu = false
                                        handleNotesCreateAction(uiState.folders, onFolderClick) { pendingNotesCreateAction = it }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_new_text_note), color = TextPrimary, fontFamily = ElmsSans, fontWeight = FontWeight.Medium) },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Outlined.NoteAdd, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(20.dp)) },
                                    onClick = {
                                        showNotesCreateMenu = false
                                        handleNotesCreateAction(uiState.folders, { folder -> onNavigateToTextNote(folder.id, null, -1L, null) }) { pendingNotesCreateAction = it }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_new_canvas_note), color = TextPrimary, fontFamily = ElmsSans, fontWeight = FontWeight.Medium) },
                                    leadingIcon = { Icon(Icons.Outlined.Draw, contentDescription = null, tint = Color(0xFFA78BFA), modifier = Modifier.size(20.dp)) },
                                    onClick = {
                                        showNotesCreateMenu = false
                                        handleNotesCreateAction(uiState.folders, { folder -> onNavigateToCanvasNote(folder.id, null, -1L) }) { pendingNotesCreateAction = it }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_import_document), color = TextPrimary, fontFamily = ElmsSans, fontWeight = FontWeight.Medium) },
                                    leadingIcon = { Icon(Icons.Outlined.UploadFile, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp)) },
                                    onClick = {
                                        showNotesCreateMenu = false
                                        handleNotesCreateAction(uiState.folders, onFolderClick) { pendingNotesCreateAction = it }
                                    }
                                )
                            }
                        }
                    }

                    // Floating bottom navigation bar (hidden while keyboard is open)
                    if (!isImeVisible) {
                        HomeBottomNavBar(
                            selectedTab = selectedNavTab,
                            onTabSelected = { if (!uiState.isMultiSelectMode) selectedNavTab = it },
                            onHomeTabLongClick = {
                                if (activeSpaceManager != null) {
                                    coroutineScope.launch {
                                        val isGhostVisible = activeSpaceManager.toggleGhostWorkspaceVisibility()
                                        snackbarHostState.showSnackbar(
                                            if (isGhostVisible) "Ghost Workspaces Revealed" else "Ghost Workspaces Shielded"
                                        )
                                    }
                                }
                            },
                            onTabReSelected = { tab ->
                                when (tab) {
                                    HomeNavTab.SETTINGS -> {
                                        if (isSettingsSubScreenOpen) {
                                            settingsResetToRootTrigger++
                                        } else {
                                            coroutineScope.launch {
                                                settingsScrollState.animateScrollTo(0)
                                            }
                                        }
                                    }
                                    HomeNavTab.NOTES -> {
                                        coroutineScope.launch {
                                            notesListState.animateScrollToItem(0)
                                        }
                                    }
                                    HomeNavTab.HOME -> {
                                        if (uiState.isMultiSelectMode) {
                                            viewModel.exitMultiSelectMode()
                                        } else if (uiState.isSearchActive) {
                                            viewModel.deactivateSearch()
                                        } else {
                                            coroutineScope.launch {
                                                homeGridState.animateScrollToItem(0)
                                            }
                                        }
                                    }
                                }
                            }
                        )
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
                            color = TextPrimary,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            lineHeight = 22.sp
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
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
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
                            Text("Save", color = TextPrimary, fontFamily = ElmsSans, fontWeight = FontWeight.Medium)
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
                            color = TextPrimary,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            lineHeight = 22.sp
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
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = if (folder.isPinned) "Unpin from Top" else "Pin to Top",
                                        color = TextPrimary,
                                        fontFamily = ElmsSans,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp
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
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Rename Folder",
                                        color = TextPrimary,
                                        fontFamily = ElmsSans,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp
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
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Select",
                                        color = TextPrimary,
                                        fontFamily = ElmsSans,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp
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
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = if (folder.isLocked) "Remove Folder Lock" else "Lock Folder (PIN)",
                                        color = TextPrimary,
                                        fontFamily = ElmsSans,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp
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
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Change Folder PIN",
                                            color = TextPrimary,
                                            fontFamily = ElmsSans,
                                            fontSize = 14.sp,
                                            lineHeight = 20.sp
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
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Unlink Folder",
                                            color = TagCrimson,
                                            fontFamily = ElmsSans,
                                            fontSize = 14.sp,
                                            lineHeight = 20.sp
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
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Link to another folder",
                                            color = TextPrimary,
                                            fontFamily = ElmsSans,
                                            fontSize = 14.sp,
                                            lineHeight = 20.sp
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
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Move to Trash",
                                        color = TagCrimson,
                                        fontFamily = ElmsSans,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp
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
                                    val currentQuery = uiState.searchQuery
                                    viewModel.onDocumentSearchResultClicked(doc) { folderId, subfolderId, docId, pageIndex ->
                                        onNavigateToDocument(folderId, subfolderId, docId, pageIndex, currentQuery)
                                    }
                                }
                            }
                            pendingTextNoteSearchResult != null -> {
                                val note = pendingTextNoteSearchResult!!
                                pendingTextNoteSearchResult = null
                                val currentQuery = uiState.searchQuery
                                viewModel.onTextNoteSearchResultClicked(note) { folderId, subfolderId, noteId ->
                                    onNavigateToTextNote(folderId, subfolderId, noteId, currentQuery)
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
                            color = TextPrimary,
                            fontSize = 16.sp,
                            lineHeight = 22.sp,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = "Total Notes & Photos: ${stats.photoCount}",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Storage occupied: $formattedMb",
                                color = HomeSubtitleGray,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
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
                            Text("Move to Trash", color = TextPrimary, fontFamily = ElmsSans, fontWeight = FontWeight.Medium)
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
                            val currentQuery = uiState.searchQuery
                            viewModel.onTextNoteSearchResultClicked(note) { folderId, subfolderId, noteId ->
                                onNavigateToTextNote(folderId, subfolderId, noteId, currentQuery)
                            }
                        }
                    },
                    onDocumentClick = { doc ->
                        val targetFolder = uiState.folders.firstOrNull { it.id == doc.folderId }
                        if (targetFolder != null && targetFolder.isLocked) {
                            pendingDocumentSearchResult = doc
                            folderToUnlock = targetFolder
                        } else {
                            val currentQuery = uiState.searchQuery
                            viewModel.onDocumentSearchResultClicked(doc) { folderId, subfolderId, docId, pageIndex ->
                                onNavigateToDocument(folderId, subfolderId, docId, pageIndex, currentQuery)
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

    if (showSpaceSwitcher && spaceRepository != null) {
        SpaceSwitcherBottomSheet(
            spaces = spaces,
            activeSpaceId = activeSpace.id,
            onSelectSpace = { newId ->
                coroutineScope.launch {
                    spaceRepository.setActiveSpaceId(newId)
                    showSpaceSwitcher = false
                }
            },
            onCreateSpace = { name, iconKey, colorHex, isPrivate ->
                coroutineScope.launch {
                    spaceRepository.createSpace(
                        name = name,
                        iconKey = iconKey,
                        colorHex = colorHex,
                        isPrivate = isPrivate
                    )
                }
            },
            onOpenSpaceSettings = { sId ->
                showSpaceSwitcher = false
                onOpenSpaceSettings(sId)
            },
            onDismiss = { showSpaceSwitcher = false }
        )
    }

    if (showSyllabusEvaluator) {
        SyllabusEvaluatorSheet(
            initialComponents = listOf(
                SyllabusComponent("1", "Midterm Exam", 30f, 85f),
                SyllabusComponent("2", "Assignments & Quizzes", 20f, 92f),
                SyllabusComponent("3", "Final Examination", 50f, null)
            ),
            onDismissRequest = { showSyllabusEvaluator = false }
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
            val wsToDelete = uiState.workspaceToDelete
            val deleteWsName = when (wsToDelete?.kind) {
                WorkspaceKind.HOME -> stringResource(R.string.workspace_home)
                WorkspaceKind.ARCHIVE -> stringResource(R.string.workspace_archive)
                WorkspaceKind.CUSTOM -> wsToDelete.name
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

    // Quick Folder Selection Dialog for Note Creation from FAB on Notes Tab
    pendingNotesCreateAction?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingNotesCreateAction = null },
            containerColor = HomeCardSurface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = stringResource(R.string.select_destination_folder),
                    color = TextPrimary,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    lineHeight = 22.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        workspaceGroupedFolderItems(
                            folders = uiState.folders,
                            workspaces = uiState.workspaces,
                            keyPrefix = "home_notes_create"
                        ) { folder ->
                            Surface(
                                color = Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        pendingNotesCreateAction = null
                                        action(folder)
                                    }
                                    .padding(vertical = 8.dp, horizontal = 12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Folder,
                                        contentDescription = null,
                                        tint = Color(0xFF60A5FA),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = folder.name,
                                        color = TextPrimary,
                                        fontFamily = ElmsSans,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { pendingNotesCreateAction = null }) {
                    Text(stringResource(R.string.confirm_cancel), color = HomeSubtitleGray, fontFamily = ElmsSans)
                }
            }
        )
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
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TagAmber.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Due Tomorrow",
                        tint = TagAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$dueCount note${if (dueCount > 1) "s" else ""} due tomorrow",
                        color = TagAmber,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

private fun handleNotesCreateAction(
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
