// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.share

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.theme.DockSlatePill
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
import com.arinara.fotara.ui.components.NewFolderDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharePlacementScreen(
    viewModel: SharePlacementViewModel,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showNewSubfolderDialog by remember { mutableStateOf(false) }
    var newSubfolderName by remember { mutableStateOf("") }

    LaunchedEffect(uiState.isFinished) {
        if (uiState.isFinished) {
            onFinish()
        }
    }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MidnightNavy,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Save to Fotara",
                            color = TextPrimary,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (!uiState.isStaging) {
                            Text(
                                text = "${uiState.selectedCount} of ${uiState.totalRemaining} items selected",
                                color = FolderTabCream,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.cancelAndCleanup() }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = FolderTabCream
                        )
                    }
                },
                actions = {
                    if (!uiState.isStaging && uiState.totalRemaining > 0) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = FolderBodyBlue,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MidnightCardOutline),
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clickable { viewModel.setDrawerOpen(true) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Collections,
                                    contentDescription = null,
                                    tint = FolderTabCream,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Items (${uiState.selectedCount})",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightNavy)
            )
        },
        bottomBar = {
            if (!uiState.isStaging && uiState.totalRemaining > 0) {
                Surface(
                    color = MidnightSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MidnightCardOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        val folder = uiState.selectedFolder
                        val destLabel = if (folder != null) {
                            val subName = uiState.subfolders.find { it.id == uiState.selectedSubfolderId }?.name
                            val groupName = uiState.availableGroups.find { it.id == uiState.selectedGroupId }?.name
                            when {
                                groupName != null -> "${folder.name} > Group: $groupName"
                                subName != null -> "${folder.name} > $subName"
                                else -> "${folder.name} (Folder Root)"
                            }
                        } else {
                            "Select destination folder above"
                        }

                        Text(
                            text = "Destination: $destLabel",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { viewModel.placeSelectedItems() },
                            enabled = uiState.selectedCount > 0 && uiState.selectedFolder != null && !uiState.isPlacing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FolderTabCream,
                                contentColor = MidnightNavy,
                                disabledContainerColor = DockSlatePill,
                                disabledContentColor = TextMuted
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            if (uiState.isPlacing) {
                                CircularProgressIndicator(
                                    color = MidnightNavy,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                val p = uiState.placementProgress
                                val progressText = if (p != null) "Saving ${p.first} of ${p.second}..." else "Saving notes..."
                                Text(
                                    text = progressText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Place Here (${uiState.selectedCount} Items)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        if (uiState.isStaging) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = FolderTabCream)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Staging shared items...",
                        color = TextPrimary,
                        fontSize = 16.sp
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Overflow cap warning banner
                if (uiState.skippedCount > 0) {
                    item {
                        Surface(
                            color = MidnightSurface,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TagAmber.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = TagAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Share bundle capped to 30 items. ${uiState.skippedCount} excess items were skipped.",
                                    color = TagAmber,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                // Section 1: Choose Folder
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Select Coursework Folder",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(
                            onClick = { showNewFolderDialog = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = FolderTabCream)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "New Folder", fontSize = 13.sp)
                        }
                    }
                }

                // Folders Grid
                item {
                    val folders = uiState.folders
                    if (folders.isEmpty()) {
                        Surface(
                            color = MidnightSurface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Text(
                                    text = "No folders created yet",
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { showNewFolderDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = FolderTabCream, contentColor = MidnightNavy)
                                ) {
                                    Text("Create First Folder")
                                }
                            }
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(folders) { folder ->
                                val isSelected = uiState.selectedFolder?.id == folder.id
                                val cardColor = try {
                                    Color(android.graphics.Color.parseColor(folder.colorLabel))
                                } catch (_: Exception) {
                                    FolderTabCream
                                }

                                Surface(
                                    color = if (isSelected) FolderBodyBlue else MidnightSurface,
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) FolderTabCream else MidnightCardOutline
                                    ),
                                    modifier = Modifier
                                        .width(140.dp)
                                        .height(96.dp)
                                        .clickable { viewModel.selectFolder(folder) }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(cardColor)
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = FolderTabCream,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = folder.name,
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 2: Subfolders & Groups (if folder selected)
                if (uiState.selectedFolder != null) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Placement Target",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                TextButton(
                                    onClick = {
                                        newSubfolderName = ""
                                        showNewSubfolderDialog = true
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = FolderTabCream)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Add Subfolder", fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Root option
                                item {
                                    FilterChip(
                                        selected = uiState.selectedSubfolderId == null && uiState.selectedGroupId == null,
                                        onClick = {
                                            viewModel.selectSubfolder(null)
                                            viewModel.selectGroup(null)
                                        },
                                        label = { Text("Folder Root") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = FolderTabCream,
                                            selectedLabelColor = MidnightNavy,
                                            containerColor = MidnightSurface,
                                            labelColor = TextSecondary
                                        )
                                    )
                                }

                                // Subfolders
                                items(uiState.subfolders) { sub ->
                                    val isSubSelected = uiState.selectedSubfolderId == sub.id && uiState.selectedGroupId == null
                                    FilterChip(
                                        selected = isSubSelected,
                                        onClick = { viewModel.selectSubfolder(sub.id) },
                                        label = { Text(sub.name) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = FolderTabCream,
                                            selectedLabelColor = MidnightNavy,
                                            containerColor = MidnightSurface,
                                            labelColor = TextSecondary
                                        )
                                    )
                                }

                                // Photo Groups (if images are present)
                                val hasImages = uiState.stagedItems.any { it.itemType == StagedItemType.IMAGE && it.isSelected }
                                if (hasImages) {
                                    items(uiState.availableGroups) { grp ->
                                        val isGrpSelected = uiState.selectedGroupId == grp.id
                                        FilterChip(
                                            selected = isGrpSelected,
                                            onClick = { viewModel.selectGroup(grp.id) },
                                            label = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.Layers,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(grp.name)
                                                }
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = FolderTabCream,
                                                selectedLabelColor = MidnightNavy,
                                                containerColor = MidnightSurface,
                                                labelColor = TextSecondary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 3: Staged Items Preview Summary
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Staged Notes (${uiState.stagedItems.size})",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row {
                                TextButton(
                                    onClick = {
                                        val anySelected = uiState.stagedItems.any { it.isSelected }
                                        viewModel.setAllSelected(!anySelected)
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = FolderTabCream)
                                ) {
                                    val anySelected = uiState.stagedItems.any { it.isSelected }
                                    Text(if (anySelected) "Deselect All" else "Select All", fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Compact list of items
                        uiState.stagedItems.forEach { item ->
                            StagedItemRow(
                                item = item,
                                onToggle = { viewModel.toggleItemSelection(item.id) },
                                onRemove = { viewModel.removeStagedItem(item.id) }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }

    // Collapsible Drawer / Modal Bottom Sheet for Items Tray
    if (uiState.isDrawerOpen) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.setDrawerOpen(false) },
            containerColor = MidnightSurface,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Notes to Save (${uiState.stagedItems.size})",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = {
                            val anySelected = uiState.stagedItems.any { it.isSelected }
                            viewModel.setAllSelected(!anySelected)
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = FolderTabCream)
                    ) {
                        val anySelected = uiState.stagedItems.any { it.isSelected }
                        Text(if (anySelected) "Deselect All" else "Select All")
                    }
                }

                HorizontalDivider(color = MidnightCardOutline, modifier = Modifier.padding(vertical = 8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.stagedItems) { item ->
                        StagedItemRow(
                            item = item,
                            onToggle = { viewModel.toggleItemSelection(item.id) },
                            onRemove = { viewModel.removeStagedItem(item.id) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.setDrawerOpen(false) },
                    colors = ButtonDefaults.buttonColors(containerColor = FolderTabCream, contentColor = MidnightNavy),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done Selecting (${uiState.selectedCount})", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Create New Folder Dialog
    if (showNewFolderDialog) {
        NewFolderDialog(
            onDismiss = { showNewFolderDialog = false },
            onCreate = { name, colorHex, _ ->
                viewModel.createFolder(name, colorHex)
                showNewFolderDialog = false
            }
        )
    }

    // Create New Subfolder Dialog
    if (showNewSubfolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewSubfolderDialog = false },
            containerColor = MidnightSurface,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "New Subfolder",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Create a subfolder inside ${uiState.selectedFolder?.name ?: "folder"}:",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newSubfolderName,
                        onValueChange = { newSubfolderName = it },
                        placeholder = { Text("Subfolder name (e.g. Chapter 1)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = FolderTabCream,
                            unfocusedBorderColor = MidnightCardOutline
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newSubfolderName.isNotBlank()) {
                            viewModel.createSubfolder(newSubfolderName)
                            showNewSubfolderDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FolderTabCream, contentColor = MidnightNavy)
                ) {
                    Text("Create", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewSubfolderDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun StagedItemRow(
    item: StagedShareItem,
    onToggle: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MidnightSurface,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MidnightCardOutline),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            Checkbox(
                checked = item.isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = FolderTabCream,
                    checkmarkColor = MidnightNavy,
                    uncheckedColor = TextSecondary
                )
            )

            // Thumbnail / Icon Preview
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DockSlatePill),
                contentAlignment = Alignment.Center
            ) {
                if (item.thumbnailPath != null) {
                    AsyncImage(
                        model = item.thumbnailPath,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val icon = when (item.itemType) {
                        StagedItemType.IMAGE -> Icons.Default.Image
                        StagedItemType.PDF -> Icons.Default.PictureAsPdf
                        StagedItemType.DOCX -> Icons.Default.Description
                        StagedItemType.TEXT -> Icons.Default.TextFields
                    }
                    val iconTint = when (item.itemType) {
                        StagedItemType.IMAGE -> FolderTabCream
                        StagedItemType.PDF -> TagCrimson
                        StagedItemType.DOCX -> Color(0xFF4A90E2)
                        StagedItemType.TEXT -> TagAmber
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.displayName,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val typeLabel = item.itemType.name
                    val pillColor = when (item.itemType) {
                        StagedItemType.IMAGE -> FolderTabCream.copy(alpha = 0.2f)
                        StagedItemType.PDF -> TagCrimson.copy(alpha = 0.2f)
                        StagedItemType.DOCX -> Color(0xFF4A90E2).copy(alpha = 0.2f)
                        StagedItemType.TEXT -> TagAmber.copy(alpha = 0.2f)
                    }
                    val textColor = when (item.itemType) {
                        StagedItemType.IMAGE -> FolderTabCream
                        StagedItemType.PDF -> TagCrimson
                        StagedItemType.DOCX -> Color(0xFF4A90E2)
                        StagedItemType.TEXT -> TagAmber
                    }
                    Surface(
                        color = pillColor,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = typeLabel,
                            color = textColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    val sizeFormatted = formatFileSize(item.fileSizeBytes)
                    Text(
                        text = sizeFormatted,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
    return String.format(java.util.Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
