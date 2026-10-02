// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.settings

import android.content.Intent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.ViewStream
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arinara.fotara.data.model.DownsampleQuality
import com.arinara.fotara.data.model.SortOrder
import com.arinara.fotara.data.model.StorageLocation
import com.arinara.fotara.data.model.ThemeMode
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.theme.HomeCardBorder
import com.arinara.fotara.theme.HomeCardSurface
import com.arinara.fotara.theme.HomeMainButtonBlue
import com.arinara.fotara.theme.HomeNearBlack
import com.arinara.fotara.theme.HomeSubtitleGray
import com.arinara.fotara.theme.TagAmber
import com.arinara.fotara.theme.TagCrimson
import kotlinx.coroutines.launch

/**
 * Fotara v1.5.2 Settings Screen redesign.
 * Formatted as a vertical list of rounded rectangular cards (~#111726, 22dp radius, 12dp spacing, 16dp padding)
 * with a large upright "Settings" title at the top, 42dp neutral slate icon tiles, and Elms Sans typography.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: (() -> Unit)? = null,
    onNavigateToTrash: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val context = LocalContext.current
    val (appVersionName, appVersionCode) = remember(context) {
        try {
            val pInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            val vName = pInfo?.versionName ?: "1.5.2 Beta"
            val vCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pInfo?.longVersionCode ?: 16L
            } else {
                @Suppress("DEPRECATION")
                (pInfo?.versionCode ?: 16).toLong()
            }
            Pair(vName, vCode)
        } catch (_: Exception) {
            Pair("1.5.2 Beta", 16L)
        }
    }

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
        containerColor = HomeNearBlack,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = innerPadding.calculateTopPadding() + contentPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + contentPadding.calculateBottomPadding() + 110.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Header with Large "Settings" Title (same style as Fotara title, no tagline)
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 10.dp)
                ) {
                    if (onBackClick != null) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF131925))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Text(
                        text = "Settings",
                        color = Color.White,
                        fontSize = 38.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-0.5).sp
                    )
                }
            }

            // 1. Default Sort Order
            item {
                SettingsCardItem(
                    title = "Default Sort Order",
                    subtitle = uiState.userSettings.defaultSortOrder.displayName,
                    icon = Icons.Default.GridView,
                    onClick = { showSortOrderDialog = true }
                )
            }

            // 2. Thumbnail Grid Density
            item {
                SettingsCardItem(
                    title = "Thumbnail Grid Density",
                    subtitle = "${uiState.userSettings.gridDensity} columns",
                    icon = Icons.Default.GridView,
                    onClick = { showGridDensityDialog = true }
                )
            }

            // 3. Theme
            item {
                SettingsCardItem(
                    title = "Theme",
                    subtitle = uiState.userSettings.themeMode.displayName,
                    icon = Icons.Default.Palette,
                    onClick = { showThemeDialog = true }
                )
            }

            // 4. Automatic OCR on Capture
            item {
                SettingsCardToggle(
                    title = "Automatic OCR on Capture",
                    subtitle = "Extract handwritten & printed text immediately after capture",
                    icon = Icons.Default.TextFields,
                    checked = uiState.userSettings.autoOcrEnabled,
                    onCheckedChange = { viewModel.updateAutoOcr(it) }
                )
            }

            // 5. OCR Recognition Script
            item {
                SettingsCardItem(
                    title = "OCR Recognition Script",
                    subtitle = uiState.userSettings.ocrLanguage,
                    icon = Icons.Default.Language,
                    onClick = { showOcrLanguageDialog = true }
                )
            }

            // 6. Downsampling Quality Tradeoff
            item {
                SettingsCardItem(
                    title = "Downsampling Quality Tradeoff",
                    subtitle = uiState.userSettings.downsampleQuality.displayName,
                    icon = Icons.Default.Image,
                    onClick = { showQualityDialog = true }
                )
            }

            // 7. System Notification Permission
            item {
                val notificationsEnabled = remember(context) {
                    androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
                }
                SettingsCardItem(
                    title = "System Notification Permission",
                    subtitle = if (notificationsEnabled) "Permission granted" else "Notifications disabled in system settings",
                    icon = Icons.Default.Notifications,
                    onClick = {
                        try {
                            val intent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = android.net.Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                    }
                )
            }

            // 8. Default Reminder Lead Time
            item {
                val leadTimeText = when (uiState.userSettings.reminderLeadTimeHours) {
                    1 -> "1 hour before deadline"
                    3 -> "3 hours before deadline"
                    24 -> "24 hours before deadline"
                    else -> "${uiState.userSettings.reminderLeadTimeHours} hours before deadline"
                }
                SettingsCardItem(
                    title = "Default Reminder Lead Time",
                    subtitle = leadTimeText,
                    icon = Icons.Default.Schedule,
                    onClick = { showLeadTimeDialog = true }
                )
            }

            // 9. "Due Tomorrow" Home Ribbon
            item {
                SettingsCardToggle(
                    title = "\"Due Tomorrow\" Home Ribbon",
                    subtitle = "Display urgent deadline alerts on home dashboard",
                    icon = Icons.Default.ViewStream,
                    checked = uiState.userSettings.dueTomorrowRibbonEnabled,
                    onCheckedChange = { viewModel.updateDueTomorrowRibbon(it) }
                )
            }

            // 10. Test Notification Alert
            item {
                SettingsCardAction(
                    title = "Test Notification Alert",
                    subtitle = "Trigger an immediate test study reminder notification",
                    icon = Icons.Default.Notifications,
                    actionIcon = Icons.Default.PlayArrow,
                    isLoading = false,
                    onClick = {
                        com.arinara.fotara.util.NoteScheduleManager(context).sendTestAlert()
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Test alert dispatched. Check notification tray.")
                        }
                    }
                )
            }

            // 11. Photo Storage Location
            item {
                SettingsCardItem(
                    title = "Photo Storage Location",
                    subtitle = uiState.userSettings.storageLocation.displayName,
                    icon = Icons.Default.Storage,
                    onClick = { showStorageLocationDialog = true }
                )
            }

            // 12. Storage Usage Breakdown
            item {
                SettingsStorageBreakdownCard(
                    formattedPhotos = uiState.storageBreakdown.formattedPhotos,
                    formattedThumbnails = uiState.storageBreakdown.formattedThumbnails,
                    formattedDatabase = uiState.storageBreakdown.formattedDatabase,
                    formattedTotal = uiState.storageBreakdown.formattedTotal
                )
            }

            // 13. Rebuild Thumbnails
            item {
                SettingsCardAction(
                    title = "Rebuild Thumbnails",
                    subtitle = "Regenerate thumbnail cache from originals",
                    icon = Icons.Default.Refresh,
                    actionIcon = Icons.Default.Refresh,
                    isLoading = uiState.isRebuildingThumbnails,
                    onClick = { viewModel.rebuildThumbnails() }
                )
            }

            // 14. Trash / Recycle Bin
            item {
                SettingsCardItem(
                    title = "Trash / Recycle Bin",
                    subtitle = "View and restore deleted folders and notes",
                    icon = Icons.Default.Delete,
                    iconTint = TagCrimson,
                    onClick = onNavigateToTrash
                )
            }

            // 15. Export Backup (JSON)
            item {
                SettingsCardAction(
                    title = "Export Backup (JSON)",
                    subtitle = "Create offline backup of all folders, subfolders, and notes",
                    icon = Icons.Default.Upload,
                    actionIcon = Icons.Default.Upload,
                    isLoading = uiState.isExportingBackup,
                    onClick = { viewModel.requestExportBackup() }
                )
            }

            // 16. Import from Backup
            item {
                SettingsCardAction(
                    title = "Import from Backup",
                    subtitle = "Restore coursework folders and notes from backup JSON",
                    icon = Icons.Default.Download,
                    actionIcon = Icons.Default.Download,
                    isLoading = uiState.isImportingBackup,
                    onClick = { viewModel.requestImportBackup() }
                )
            }

            // 17. Rebuild Search Index
            item {
                SettingsCardAction(
                    title = "Rebuild Search Index",
                    subtitle = "Forces full SQLite FTS4 virtual table re-indexing",
                    icon = Icons.Default.FindInPage,
                    actionIcon = Icons.Default.Refresh,
                    isLoading = uiState.isRebuildingSearchIndex,
                    onClick = { viewModel.rebuildSearchIndex() }
                )
            }

            // 18. About
            item {
                val displayVersion = if (appVersionName.contains("Beta", ignoreCase = true)) {
                    appVersionName
                } else {
                    "$appVersionName Beta"
                }
                SettingsAboutCard(
                    version = "Version $displayVersion (Build $appVersionCode)"
                )
            }

            // 19. Open Source Notices & Licenses
            item {
                SettingsCardItem(
                    title = "Open Source Notices & Licenses",
                    subtitle = "View third-party software attributions",
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    onClick = { viewModel.setLicensesDialogVisible(true) }
                )
            }
        }
    }

    // Dialogs
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
            containerColor = HomeCardSurface,
            title = {
                Text(
                    text = "Backup Export Ready",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Your offline backup JSON is ready (${json.length} characters). You can copy it to your clipboard or share it to save a local file.",
                        color = HomeSubtitleGray,
                        fontSize = 14.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Light
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(HomeNearBlack, RoundedCornerShape(12.dp))
                            .border(1.dp, HomeCardBorder, RoundedCornerShape(12.dp))
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = json.take(1000) + if (json.length > 1000) "\n... (truncated for preview)" else "",
                            color = Color(0xFF94A3B8),
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
                    colors = ButtonDefaults.buttonColors(containerColor = HomeMainButtonBlue)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy JSON", fontFamily = ElmsSans, fontWeight = FontWeight.Medium)
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
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", color = Color.White, fontFamily = ElmsSans)
                    }
                    TextButton(onClick = { viewModel.dismissExportDialog() }) {
                        Text("Close", color = HomeSubtitleGray, fontFamily = ElmsSans)
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
            containerColor = HomeCardSurface,
            title = {
                Text(
                    text = "Import Backup",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Paste your exported Fotara backup JSON string below. New folders, subfolders, and notes will be merged without overwriting existing records.",
                        color = HomeSubtitleGray,
                        fontSize = 14.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Light
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = importInputText,
                        onValueChange = {
                            importInputText = it
                            inputError = null
                        },
                        placeholder = { Text("Paste JSON here...", color = HomeSubtitleGray, fontFamily = ElmsSans) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HomeMainButtonBlue,
                            unfocusedBorderColor = HomeCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        isError = inputError != null
                    )
                    if (inputError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = inputError!!, color = TagCrimson, fontSize = 12.sp, fontFamily = ElmsSans)
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
                    colors = ButtonDefaults.buttonColors(containerColor = HomeMainButtonBlue)
                ) {
                    Text("Import", fontFamily = ElmsSans, fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissImportDialog() }) {
                    Text("Cancel", color = HomeSubtitleGray, fontFamily = ElmsSans)
                }
            }
        )
    }

    // Open Source Licenses Dialog
    if (uiState.showLicensesDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setLicensesDialogVisible(false) },
            containerColor = HomeCardSurface,
            title = {
                Text(
                    text = "Open Source Notices",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontFamily = ElmsSans,
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
                    Text("Close", color = Color.White, fontFamily = ElmsSans)
                }
            }
        )
    }
}

/**
 * Standard clickable settings card (~#111726, 22dp radius, 16dp padding).
 * Holds a 42dp neutral slate icon tile, title (Medium), subtitle (Light, muted), and chevron/value.
 */
@Composable
private fun SettingsCardItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    iconTint: Color = Color(0xFF94A3B8),
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = HomeCardSurface),
        border = BorderStroke(1.dp, HomeCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // ~42dp neutral slate icon tile (same style as folder tiles)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E2638)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Medium
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = HomeSubtitleGray,
                        fontSize = 13.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Light,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Settings card with a Switch toggle.
 */
@Composable
private fun SettingsCardToggle(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = HomeCardSurface),
        border = BorderStroke(1.dp, HomeCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E2638)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Medium
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = HomeSubtitleGray,
                        fontSize = 13.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Light,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = HomeMainButtonBlue,
                    uncheckedThumbColor = Color(0xFF94A3B8),
                    uncheckedTrackColor = Color(0xFF1E2638)
                )
            )
        }
    }
}

/**
 * Settings card with an action button or loading spinner.
 */
@Composable
private fun SettingsCardAction(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    actionIcon: ImageVector,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = HomeCardSurface),
        border = BorderStroke(1.dp, HomeCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLoading, onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E2638)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Medium
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = HomeSubtitleGray,
                        fontSize = 13.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Light,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isLoading) {
                CircularProgressIndicator(
                    color = HomeMainButtonBlue,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = title,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Storage breakdown card with pills and usage stats.
 */
@Composable
private fun SettingsStorageBreakdownCard(
    formattedPhotos: String,
    formattedThumbnails: String,
    formattedDatabase: String,
    formattedTotal: String
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = HomeCardSurface),
        border = BorderStroke(1.dp, HomeCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E2638)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Storage Usage Breakdown",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Total Fotara Data: $formattedTotal",
                        color = HomeSubtitleGray,
                        fontSize = 13.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Light
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StoragePill(label = "Photos", size = formattedPhotos, modifier = Modifier.weight(1f))
                StoragePill(label = "Thumbnails", size = formattedThumbnails, modifier = Modifier.weight(1f))
                StoragePill(label = "Database", size = formattedDatabase, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StoragePill(label: String, size: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color(0xFF131925), RoundedCornerShape(10.dp))
            .border(1.dp, HomeCardBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, color = HomeSubtitleGray, fontSize = 11.sp, fontFamily = ElmsSans, fontWeight = FontWeight.Light)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = size, color = TagAmber, fontSize = 13.sp, fontFamily = ElmsSans, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * About Fotara card with app version and legal notices.
 */
@Composable
private fun SettingsAboutCard(version: String) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = HomeCardSurface),
        border = BorderStroke(1.dp, HomeCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E2638)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Fotara",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = version,
                    color = Color(0xFF60A5FA),
                    fontSize = 13.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Specialized photo organization and on-device OCR for students.",
                    color = HomeSubtitleGray,
                    fontSize = 13.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Light,
                    lineHeight = 17.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Arinara Network • 100% Offline Architecture",
                    color = Color(0xFF475569),
                    fontSize = 12.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Light
                )
            }
        }
    }
}

@Composable
private fun LicenseItem(name: String, license: String, copyright: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = name, color = Color.White, fontSize = 14.sp, fontFamily = ElmsSans, fontWeight = FontWeight.Medium)
        Text(text = license, color = Color(0xFF60A5FA), fontSize = 12.sp, fontFamily = ElmsSans, fontWeight = FontWeight.Light)
        Text(text = copyright, color = HomeSubtitleGray, fontSize = 11.sp, fontFamily = ElmsSans, fontWeight = FontWeight.Light)
        Spacer(modifier = Modifier.height(6.dp))
        HorizontalDivider(color = HomeCardBorder)
    }
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
        containerColor = HomeCardSurface,
        title = {
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontFamily = ElmsSans,
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
                                selectedColor = HomeMainButtonBlue,
                                unselectedColor = Color(0xFF64748B)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            color = if (option == selectedOption) Color.White else HomeSubtitleGray,
                            fontSize = 14.sp,
                            fontFamily = ElmsSans,
                            fontWeight = if (option == selectedOption) FontWeight.Medium else FontWeight.Light
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = HomeSubtitleGray, fontFamily = ElmsSans)
            }
        }
    )
}
