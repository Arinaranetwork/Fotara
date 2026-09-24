// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.model.PhotoGroup
import com.arinara.fotara.theme.FolderBodyBlue
import com.arinara.fotara.theme.FolderTabCream
import com.arinara.fotara.theme.MidnightCardOutline
import com.arinara.fotara.theme.MidnightNavy
import com.arinara.fotara.theme.MidnightSurface
import com.arinara.fotara.theme.TagAmber
import com.arinara.fotara.theme.TagCrimson
import com.arinara.fotara.theme.TextMuted
import com.arinara.fotara.theme.TextPrimary
import com.arinara.fotara.theme.TextSecondary
import com.arinara.fotara.ui.components.FloatingDock
import com.arinara.fotara.ui.components.FolderCard
import com.arinara.fotara.ui.components.FolderUnlockDialog
import com.arinara.fotara.ui.components.NewFolderCard
import com.arinara.fotara.ui.components.NewFolderDialog
import com.arinara.fotara.ui.components.ResetFolderPinDialog
import com.arinara.fotara.ui.components.SetFolderLockDialog

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onFolderClick: (Folder) -> Unit,
    onNavigateToPhoto: (folderId: Long, subfolderId: Long?, photoId: Long) -> Unit = { _, _, _ -> },
    onNavigateToGroup: (folderId: Long, subfolderId: Long?, groupId: Long) -> Unit = { _, _, _ -> },
    onOpenTrash: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val keyguardManager = remember { context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager }

    var activeContextFolder by remember { mutableStateOf<Folder?>(null) }
    var showHomeOverflowMenu by remember { mutableStateOf(false) }

    var folderToUnlock by remember { mutableStateOf<Folder?>(null) }
    var folderToLock by remember { mutableStateOf<Folder?>(null) }
    var folderToResetPin by remember { mutableStateOf<Folder?>(null) }
    var isRemovingLock by remember { mutableStateOf(false) }
    var isResettingPin by remember { mutableStateOf(false) }
    var pendingPhotoSearchResult by remember { mutableStateOf<Photo?>(null) }
    var pendingGroupSearchResult by remember { mutableStateOf<PhotoGroup?>(null) }

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
                } else {
                    onFolderClick(target)
                }
            }
        }
    }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    BackHandler(enabled = uiState.isMultiSelectMode) {
        viewModel.exitMultiSelectMode()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MidnightNavy,
        topBar = {
            if (uiState.isMultiSelectMode) {
                TopAppBar(
                    title = {
                        Text(
                            text = "${uiState.selectedFolderIds.size} Selected",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.exitMultiSelectMode() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Exit Multi-Select",
                                tint = FolderTabCream
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.selectAllFolders() }) {
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = "Select All",
                                tint = FolderTabCream
                            )
                        }
                        IconButton(
                            onClick = { viewModel.requestBulkDelete() },
                            enabled = uiState.selectedFolderIds.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Selected Folders",
                                tint = if (uiState.selectedFolderIds.isNotEmpty()) TagCrimson else TextMuted
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightSurface)
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            text = "Fotara",
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        Box {
                            IconButton(onClick = { showHomeOverflowMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    tint = FolderTabCream
                                )
                            }
                            DropdownMenu(
                                expanded = showHomeOverflowMenu,
                                onDismissRequest = { showHomeOverflowMenu = false },
                                modifier = Modifier
                                    .background(MidnightSurface)
                                    .border(1.dp, MidnightCardOutline, RoundedCornerShape(8.dp))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Settings", color = TextPrimary) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = null,
                                            tint = FolderTabCream
                                        )
                                    },
                                    onClick = {
                                        showHomeOverflowMenu = false
                                        onOpenSettings()
                                    }
                                )
                                HorizontalDivider(color = MidnightCardOutline.copy(alpha = 0.6f))
                                DropdownMenuItem(
                                    text = { Text("Trash", color = TextPrimary) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = FolderTabCream
                                        )
                                    },
                                    onClick = {
                                        showHomeOverflowMenu = false
                                        onOpenTrash()
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightNavy)
                )
            }
        },
        bottomBar = {
            if (!uiState.isMultiSelectMode) {
                FloatingDock(
                    onSearchClick = { viewModel.activateSearch() },
                    onCameraClick = {
                        // If folders exist, open first folder or search
                    }
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        val configuration = LocalConfiguration.current
        val screenWidthDp = configuration.screenWidthDp
        val folderColumns = when {
            screenWidthDp >= 840 -> 5
            screenWidthDp >= 600 -> 4
            else -> 2
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MidnightNavy)
                .padding(innerPadding)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(folderColumns),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Header section: Brand & Active deadline summary (hidden during multi-select for clarity)
                if (!uiState.isMultiSelectMode) {
                    item(span = { GridItemSpan(folderColumns) }) {
                        HomeHeader(
                            dueCount = uiState.photosDueTomorrow.size,
                            addedTodayCount = uiState.photosAddedToday.size
                        )
                    }
                }

                // 2-Column Folder cards matching UI mockup with inline long-press rename & card context menu
                items(uiState.folders, key = { it.id }) { folder ->
                    FolderCard(
                        folder = folder,
                        isSelectionMode = uiState.isMultiSelectMode,
                        isSelected = uiState.selectedFolderIds.contains(folder.id),
                        onClick = {
                            if (uiState.isMultiSelectMode) {
                                viewModel.toggleFolderSelection(folder.id)
                            } else if (folder.isLocked) {
                                folderToUnlock = folder
                            } else {
                                onFolderClick(folder)
                            }
                        },
                        onRename = { newName ->
                            viewModel.renameFolder(folder.id, newName)
                        },
                        onCardLongClick = {
                            activeContextFolder = folder
                        }
                    )
                }

                // "+New" Folder Card (hidden during multi-select mode)
                if (!uiState.isMultiSelectMode) {
                    item {
                        NewFolderCard(
                            onClick = { viewModel.openNewFolderDialog() }
                        )
                    }
                }
            }

            if (uiState.showNewFolderDialog) {
                NewFolderDialog(
                    onDismiss = { viewModel.closeNewFolderDialog() },
                    onCreate = { name, color, isPinned ->
                        viewModel.createFolder(name, color, isPinned)
                    }
                )
            }

            // Card-Level Context Menu (Pin/Unpin, Multi-Select entry)
            if (activeContextFolder != null) {
                val folder = activeContextFolder!!
                AlertDialog(
                    onDismissRequest = { activeContextFolder = null },
                    containerColor = MidnightSurface,
                    shape = RoundedCornerShape(16.dp),
                    title = {
                        Text(
                            text = folder.name,
                            color = TextPrimary,
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
                                        tint = FolderTabCream,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(
                                        text = if (folder.isPinned) "Unpin from Top" else "Pin to Top",
                                        color = TextPrimary,
                                        fontSize = 15.sp
                                    )
                                }
                            }

                            // Action 2: Select (Enters Multi-Select Mode)
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
                                        color = TextPrimary,
                                        fontSize = 15.sp
                                    )
                                }
                            }

                            // Action 3: Lock / Unlock Folder
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
                                        tint = if (folder.isLocked) TagAmber else FolderTabCream,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(
                                        text = if (folder.isLocked) "Remove Folder Lock" else "Lock Folder (PIN)",
                                        color = TextPrimary,
                                        fontSize = 15.sp
                                    )
                                }
                            }

                            if (folder.isLocked) {
                                // Action 4: Change PIN
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
                                            tint = FolderTabCream,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Text(
                                            text = "Change Folder PIN",
                                            color = TextPrimary,
                                            fontSize = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { activeContextFolder = null }) {
                            Text("Cancel", color = TextSecondary)
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
                    containerColor = MidnightSurface,
                    shape = RoundedCornerShape(18.dp),
                    title = {
                        Text(
                            text = "Move ${stats.folderCount} Folder${if (stats.folderCount > 1) "s" else ""} to Trash?",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = "Total Notes & Photos: ${stats.photoCount}",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Storage occupied: $formattedMb",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Items moved to Trash can be restored within 30 days before being automatically purged. Associated deadline reminders will be cancelled.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.confirmBulkDelete() },
                            colors = ButtonDefaults.buttonColors(containerColor = TagCrimson)
                        ) {
                            Text("Move to Trash", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.dismissBulkDeleteDialog() }) {
                            Text("Cancel", color = FolderTabCream)
                        }
                    }
                )
            }

            // Keyboard-Docked Search Bar Overlay (Revision 2 Specification)
            if (uiState.isSearchActive) {
                com.arinara.fotara.ui.components.ActiveSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChanged(it) },
                    onDismiss = { viewModel.deactivateSearch() },
                    isLoading = uiState.isSearchLoading,
                    folderResults = uiState.folderSearchResults,
                    groupResults = uiState.groupSearchResults,
                    photoResults = uiState.searchResults,
                    recentSearches = uiState.recentSearches,
                    selectedDateFilter = uiState.searchDateFilter,
                    onSelectDateFilter = { viewModel.setDateFilter(it) },
                    selectedColorFilter = uiState.searchColorFilter,
                    onSelectColorFilter = { viewModel.setColorFilter(it) },
                    smartTags = uiState.availableSmartTags,
                    selectedSmartTag = uiState.selectedSmartTag,
                    onSelectSmartTag = { viewModel.selectSmartTag(it) },
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
                    }
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(
    dueCount: Int,
    addedTodayCount: Int,
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
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
