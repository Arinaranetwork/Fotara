// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.settings

import android.content.Intent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arinara.fotara.data.model.DownsampleQuality
import com.arinara.fotara.data.model.SortOrder
import com.arinara.fotara.data.model.StorageLocation
import com.arinara.fotara.data.model.ThemeMode
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    onNavigateToTrash: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var showSortOrderDialog by remember { mutableStateOf(false) }
    var showGridDensityDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showOcrLanguageDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showLeadTimeDialog by remember { mutableStateOf(false) }
    var showStorageLocationDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedbackMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MidnightNavy,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = FolderTabCream
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightNavy)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Display & Organization
            item {
                SettingsSection(
                    title = "Display & Organization",
                    icon = Icons.Default.GridView
                ) {
                    SettingsRow(
                        title = "Default Sort Order",
                        subtitle = uiState.userSettings.defaultSortOrder.displayName,
                        onClick = { showSortOrderDialog = true }
                    )
                    SettingsDivider()
                    SettingsRow(
                        title = "Thumbnail Grid Density",
                        subtitle = "${uiState.userSettings.gridDensity} columns",
                        onClick = { showGridDensityDialog = true }
                    )
                    SettingsDivider()
                    SettingsRow(
                        title = "Theme",
                        subtitle = uiState.userSettings.themeMode.displayName,
                        onClick = { showThemeDialog = true }
                    )
                }
            }

            // 2. OCR & Processing
            item {
                SettingsSection(
                    title = "OCR & Processing",
                    icon = Icons.Default.TextFields
                ) {
                    SettingsToggleRow(
                        title = "Automatic OCR on Capture",
                        subtitle = "Extract handwritten & printed text immediately after capture",
                        checked = uiState.userSettings.autoOcrEnabled,
                        onCheckedChange = { viewModel.updateAutoOcr(it) }
                    )
                    SettingsDivider()
                    SettingsRow(
                        title = "OCR Recognition Script",
                        subtitle = uiState.userSettings.ocrLanguage,
                        onClick = { showOcrLanguageDialog = true }
                    )
                    SettingsDivider()
                    SettingsRow(
                        title = "Downsampling Quality Tradeoff",
                        subtitle = uiState.userSettings.downsampleQuality.displayName,
                        onClick = { showQualityDialog = true }
                    )
                }
            }

            // 3. Notifications
            item {
                SettingsSection(
                    title = "Notifications",
                    icon = Icons.Default.Notifications
                ) {
                    val leadTimeText = when (uiState.userSettings.reminderLeadTimeHours) {
                        1 -> "1 hour before deadline"
                        3 -> "3 hours before deadline"
                        24 -> "24 hours before deadline"
                        else -> "${uiState.userSettings.reminderLeadTimeHours} hours before deadline"
                    }
                    SettingsRow(
                        title = "Default Reminder Lead Time",
                        subtitle = leadTimeText,
                        onClick = { showLeadTimeDialog = true }
                    )
                    SettingsDivider()
                    SettingsToggleRow(
                        title = "\"Due Tomorrow\" Home Ribbon",
                        subtitle = "Display urgent deadline alerts on home dashboard",
                        checked = uiState.userSettings.dueTomorrowRibbonEnabled,
                        onCheckedChange = { viewModel.updateDueTomorrowRibbon(it) }
                    )
                }
            }

            // 4. Storage
            item {
                SettingsSection(
                    title = "Storage",
                    icon = Icons.Default.Storage
                ) {
                    SettingsRow(
                        title = "Photo Storage Location",
                        subtitle = uiState.userSettings.storageLocation.displayName,
                        onClick = { showStorageLocationDialog = true }
                    )
                    SettingsDivider()

                    // Storage Breakdown View
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Storage Usage Breakdown",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StoragePill(label = "Photos", size = uiState.storageBreakdown.formattedPhotos)
                            StoragePill(label = "Thumbnails", size = uiState.storageBreakdown.formattedThumbnails)
                            StoragePill(label = "Database", size = uiState.storageBreakdown.formattedDatabase)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Total Fotara Data: ${uiState.storageBreakdown.formattedTotal}",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }

                    SettingsDivider()

                    // Rebuild Thumbnails Action
                    SettingsActionRow(
                        title = "Rebuild Thumbnails",
                        subtitle = "Regenerate thumbnail cache from originals",
                        icon = Icons.Default.Refresh,
                        isLoading = uiState.isRebuildingThumbnails,
                        onClick = { viewModel.rebuildThumbnails() }
                    )

                    SettingsDivider()

                    // Trash Navigation Entry
                    SettingsRow(
                        title = "Trash / Recycle Bin",
                        subtitle = "View and restore deleted folders and notes",
                        leadingIcon = Icons.Default.Delete,
                        leadingIconTint = TagCrimson,
                        onClick = onNavigateToTrash
                    )
                }
            }

            // 5. Data Backup & Import
            item {
                SettingsSection(
                    title = "Data Management",
                    icon = Icons.Default.SdCard
                ) {
                    SettingsActionRow(
                        title = "Export Backup (JSON)",
                        subtitle = "Create offline backup of all folders, subfolders, and notes",
                        icon = Icons.Default.Upload,
                        isLoading = uiState.isExportingBackup,
                        onClick = { viewModel.requestExportBackup() }
                    )
                    SettingsDivider()
                    SettingsActionRow(
                        title = "Import from Backup",
                        subtitle = "Restore coursework folders and notes from backup JSON",
                        icon = Icons.Default.Download,
                        isLoading = uiState.isImportingBackup,
                        onClick = { viewModel.requestImportBackup() }
                    )
                }
            }

            // 6. Search Index
            item {
                SettingsSection(
                    title = "Search",
                    icon = Icons.Default.FindInPage
                ) {
                    SettingsActionRow(
                        title = "Rebuild Search Index",
                        subtitle = "Forces full SQLite FTS4 virtual table re-indexing",
                        icon = Icons.Default.Refresh,
                        isLoading = uiState.isRebuildingSearchIndex,
                        onClick = { viewModel.rebuildSearchIndex() }
                    )
                }
            }

            // 7. About
            item {
                SettingsSection(
                    title = "About",
                    icon = Icons.Default.Info
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Fotara",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Version 1.1.0 Beta",
                            color = FolderTabCream,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Specialized photo organization and on-device OCR for students.",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Arinara Network • 100% Offline Architecture",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                    SettingsDivider()
                    SettingsRow(
                        title = "Open Source Notices & Licenses",
                        subtitle = "View third-party software attributions",
                        leadingIcon = Icons.AutoMirrored.Filled.MenuBook,
                        onClick = { viewModel.setLicensesDialogVisible(true) }
                    )
                }
            }
        }
    }

    // Sort Order Selection Dialog
    if (showSortOrderDialog) {
        OptionSelectionDialog(
            title = "Default Sort Order",
            options = SortOrder.entries.map { it to it.displayName },
            selectedOption = uiState.userSettings.defaultSortOrder,
            onOptionSelected = {
                viewModel.updateSortOrder(it)
                showSortOrderDialog = false
            },
            onDismiss = { showSortOrderDialog = false }
        )
    }

    // Grid Density Selection Dialog
    if (showGridDensityDialog) {
        OptionSelectionDialog(
            title = "Thumbnail Grid Density",
            options = listOf(2 to "2 Columns (Comfortable)", 3 to "3 Columns (Standard)", 4 to "4 Columns (Compact)"),
            selectedOption = uiState.userSettings.gridDensity,
            onOptionSelected = {
                viewModel.updateGridDensity(it)
                showGridDensityDialog = false
            },
            onDismiss = { showGridDensityDialog = false }
        )
    }

    // Theme Selection Dialog
    if (showThemeDialog) {
        OptionSelectionDialog(
            title = "Theme",
            options = ThemeMode.entries.map { it to it.displayName },
            selectedOption = uiState.userSettings.themeMode,
            onOptionSelected = {
                viewModel.updateThemeMode(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    // OCR Language Dialog
    if (showOcrLanguageDialog) {
        OptionSelectionDialog(
            title = "OCR Recognition Script",
            options = listOf(
                "Latin" to "Latin (Default - English, French, Spanish, German)",
                "English" to "English Only",
                "Auto" to "Auto-Detect Language"
            ),
            selectedOption = uiState.userSettings.ocrLanguage,
            onOptionSelected = {
                viewModel.updateOcrLanguage(it)
                showOcrLanguageDialog = false
            },
            onDismiss = { showOcrLanguageDialog = false }
        )
    }

    // Downsampling Quality Dialog
    if (showQualityDialog) {
        OptionSelectionDialog(
            title = "Pre-OCR Downsampling Quality",
            options = DownsampleQuality.entries.map { it to it.displayName },
            selectedOption = uiState.userSettings.downsampleQuality,
            onOptionSelected = {
                viewModel.updateDownsampleQuality(it)
                showQualityDialog = false
            },
            onDismiss = { showQualityDialog = false }
        )
    }

    // Reminder Lead Time Dialog
    if (showLeadTimeDialog) {
        OptionSelectionDialog(
            title = "Default Reminder Lead Time",
            options = listOf(
                1 to "1 Hour before deadline (H-1)",
                3 to "3 Hours before deadline (H-3)",
                24 to "24 Hours before deadline (H-24)"
            ),
            selectedOption = uiState.userSettings.reminderLeadTimeHours,
            onOptionSelected = {
                viewModel.updateReminderLeadTime(it)
                showLeadTimeDialog = false
            },
            onDismiss = { showLeadTimeDialog = false }
        )
    }

    // Storage Location Dialog
    if (showStorageLocationDialog) {
        OptionSelectionDialog(
            title = "Photo Storage Location",
            options = StorageLocation.entries.map { it to it.displayName },
            selectedOption = uiState.userSettings.storageLocation,
            onOptionSelected = {
                viewModel.updateStorageLocation(it)
                showStorageLocationDialog = false
            },
            onDismiss = { showStorageLocationDialog = false }
        )
    }

    // Backup Export Dialog
    if (uiState.showExportDialog && uiState.exportedJsonString != null) {
        val json = uiState.exportedJsonString!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissExportDialog() },
            containerColor = MidnightSurface,
            title = {
                Text(
                    text = "Backup Export Ready",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Your offline backup JSON is ready (${json.length} characters). You can copy it to your clipboard or share it to save a local file.",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(MidnightNavy, RoundedCornerShape(8.dp))
                            .border(1.dp, MidnightCardOutline, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = json.take(1000) + if (json.length > 1000) "\n... (truncated for preview)" else "",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Fotara Backup", json)
                        clipboard?.setPrimaryClip(clip)
                        viewModel.dismissExportDialog()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FolderBodyBlue)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy JSON")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, json)
                                type = "application/json"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Share Fotara Backup")
                            context.startActivity(shareIntent)
                            viewModel.dismissExportDialog()
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = FolderTabCream)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", color = FolderTabCream)
                    }
                    TextButton(onClick = { viewModel.dismissExportDialog() }) {
                        Text("Close", color = TextSecondary)
                    }
                }
            }
        )
    }

    // Backup Import Dialog
    if (uiState.showImportDialog) {
        var importInputText by remember { mutableStateOf("") }
        var inputError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { viewModel.dismissImportDialog() },
            containerColor = MidnightSurface,
            title = {
                Text(
                    text = "Import Backup",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Paste your exported Fotara backup JSON string below. New folders, subfolders, and notes will be merged without overwriting existing records.",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = importInputText,
                        onValueChange = {
                            importInputText = it
                            inputError = null
                        },
                        placeholder = { Text("Paste JSON here...", color = TextMuted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FolderTabCream,
                            unfocusedBorderColor = MidnightCardOutline,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        isError = inputError != null
                    )
                    if (inputError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = inputError!!, color = TagCrimson, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = importInputText.trim()
                        if (trimmed.isBlank()) {
                            inputError = "Please enter backup JSON content."
                        } else {
                            viewModel.executeImportBackup(trimmed)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FolderBodyBlue)
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissImportDialog() }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Open Source Licenses Dialog
    if (uiState.showLicensesDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setLicensesDialogVisible(false) },
            containerColor = MidnightSurface,
            title = {
                Text(
                    text = "Open Source Notices",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    LicenseItem(
                        name = "Jetpack Compose & AndroidX",
                        license = "Apache License 2.0",
                        copyright = "Copyright (c) The Android Open Source Project"
                    )
                    LicenseItem(
                        name = "Google ML Kit Text Recognition",
                        license = "Apache License 2.0",
                        copyright = "Copyright (c) Google LLC"
                    )
                    LicenseItem(
                        name = "Kotlin Coroutines & Serialization",
                        license = "Apache License 2.0",
                        copyright = "Copyright (c) JetBrains s.r.o."
                    )
                    LicenseItem(
                        name = "Coil Image Loading",
                        license = "Apache License 2.0",
                        copyright = "Copyright (c) Coil Contributors"
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.setLicensesDialogVisible(false) }) {
                    Text("Close", color = FolderTabCream)
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MidnightCardOutline)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = FolderTabCream,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    color = FolderTabCream,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            HorizontalDivider(color = MidnightCardOutline.copy(alpha = 0.6f))
            content()
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    leadingIcon: ImageVector? = null,
    leadingIconTint: Color = FolderTabCream,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = leadingIconTint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 13.sp
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 13.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TextPrimary,
                checkedTrackColor = FolderBodyBlue,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = MidnightNavy
            )
        )
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLoading, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 13.sp
            )
        }
        if (isLoading) {
            CircularProgressIndicator(
                color = FolderTabCream,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp)
            )
        } else {
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = FolderTabCream,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun StoragePill(label: String, size: String) {
    Box(
        modifier = Modifier
            .background(MidnightNavy, RoundedCornerShape(8.dp))
            .border(1.dp, MidnightCardOutline, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, color = TextMuted, fontSize = 11.sp)
            Text(text = size, color = TagAmber, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LicenseItem(name: String, license: String, copyright: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text(text = license, color = FolderTabCream, fontSize = 12.sp)
        Text(text = copyright, color = TextMuted, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(6.dp))
        HorizontalDivider(color = MidnightCardOutline.copy(alpha = 0.4f))
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(color = MidnightCardOutline.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun <T> OptionSelectionDialog(
    title: String,
    options: List<Pair<T, String>>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidnightSurface,
        title = {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                options.forEach { (option, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOptionSelected(option) }
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(
                            selected = option == selectedOption,
                            onClick = { onOptionSelected(option) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = FolderBodyBlue,
                                unselectedColor = TextMuted
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            color = if (option == selectedOption) TextPrimary else TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = if (option == selectedOption) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
