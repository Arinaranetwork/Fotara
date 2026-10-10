// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.online

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.arinara.fotara.data.repository.SettingsRepository
import com.arinara.fotara.ui.components.ReleaseNotesRenderer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.io.File

private val ScreenNavy = Color(0xFF03071E)
private val TabCream = Color(0xFFEAE3D2)
private val AccentGold = Color(0xFFF77F00)
private val CardBg = Color(0xFF141936)
private val CardOutline = Color(0xFF232B56)
private val SuccessGreen = Color(0xFF2A9D8F)
private val ErrorRed = Color(0xFFE63946)

@Composable
fun UpdateScreen(
    updateManager: UpdateManager,
    settingsRepository: SettingsRepository? = null,
    onClose: () -> Unit,
    onSkipVersion: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val release by updateManager.latestRelease.collectAsState()
    val rollbackReleases by updateManager.rollbackReleases.collectAsState()
    val activeRollbackTarget by updateManager.activeRollbackTarget.collectAsState()
    val updateState by updateManager.updateState.collectAsState()
    val downloadProgress by updateManager.downloadProgress.collectAsState()
    val errorMessage by updateManager.errorMessage.collectAsState()
    val statusNotice by updateManager.statusNotice.collectAsState()

    var confirmRollbackRelease by remember { mutableStateOf<ReleaseInfo?>(null) }
    var isRollbackExpanded by remember { mutableStateOf(false) }
    var expandedReleaseNotesVersion by remember { mutableStateOf<String?>(null) }

    val currentVersion = remember(context) {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.6.0"
        } catch (_: Exception) {
            "1.6.0"
        }
    }

    var showInstallExplainerDialog by remember { mutableStateOf(false) }
    var showInstallPermissionDialog by remember { mutableStateOf(false) }
    var isClosing by remember { mutableStateOf(false) }
    val safeClose = {
        if (!isClosing) {
            isClosing = true
            onClose()
        }
    }

    // Automatically check for updates on screen launch if idle
    LaunchedEffect(Unit) {
        if (updateState == UpdateState.IDLE) {
            updateManager.checkForUpdates()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenNavy)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "App Update",
                    color = AccentGold,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { scope.launch { updateManager.checkForUpdates(forceRefresh = true) } },
                    enabled = updateState != UpdateState.CHECKING && updateState != UpdateState.DOWNLOADING
                ) {
                    if (updateState == UpdateState.CHECKING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = AccentGold,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Check for newer updates",
                            tint = TabCream
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = safeClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TabCream
                    )
                }
            }

            // Status notice when re-checking or skipping to newer release
            AnimatedVisibility(visible = !statusNotice.isNullOrBlank()) {
                Surface(
                    color = CardBg,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentGold.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = statusNotice ?: "",
                            color = TabCream,
                            fontSize = 12.5.sp,
                            lineHeight = 17.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = { updateManager.clearStatusNotice() },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss notice",
                                tint = TabCream.copy(alpha = 0.6f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (updateState) {
                UpdateState.IDLE, UpdateState.CHECKING -> {
                    // Loading State
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardOutline)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = AccentGold,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "Checking for Updates...",
                                color = TabCream,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Connecting to official GitHub release channel",
                                color = TabCream.copy(alpha = 0.65f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                UpdateState.NO_UPDATE -> {
                    // Up To Date State
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardOutline)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = SuccessGreen.copy(alpha = 0.2f),
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Fotara is Up to Date",
                                color = TabCream,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val currentInfo = remember(currentVersion) { VersionInfo.parse(currentVersion) }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Current: ${currentInfo.displayVersion}",
                                    color = TabCream.copy(alpha = 0.8f),
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                com.arinara.fotara.ui.components.ChannelPill(
                                    channel = currentInfo.channelLabel
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "You have the latest features and security updates installed.",
                                color = TabCream.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { scope.launch { updateManager.checkForUpdates() } },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(0.8f)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Check Again", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                UpdateState.ERROR -> {
                    // Error State
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardOutline)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = ErrorRed,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Unable to Check for Updates",
                                color = TabCream,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = errorMessage ?: "Please check your network connection and try again.",
                                color = TabCream.copy(alpha = 0.7f),
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { scope.launch { updateManager.checkForUpdates() } },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Try Again", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                UpdateState.UPDATE_AVAILABLE, UpdateState.DOWNLOADING, UpdateState.DOWNLOADED -> {
                    // Update Available State
                    val rel = release

                    val targetInfo = remember(rel) { VersionInfo.parse(rel?.version ?: "", rel?.isPrerelease == true) }
                    com.arinara.fotara.ui.components.UpdateBanner(
                        version = targetInfo.displayVersion,
                        releaseUrl = rel?.htmlUrl,
                        channel = targetInfo.channelLabel,
                        bannerImageUrl = rel?.bannerUrl,
                        onClose = null,
                        shape = RoundedCornerShape(16.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // What's New Section
                    Text(
                        text = "What's New in this Release",
                        color = TabCream,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardOutline)
                    ) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            ReleaseNotesRenderer(
                                markdown = rel?.releaseNotes?.ifBlank { "Bug fixes and performance improvements." }
                                    ?: "Bug fixes and performance improvements.",
                                primaryTextColor = TabCream,
                                accentColor = AccentGold,
                                cardBorder = CardOutline
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Progress bar if downloading
                    if (updateState == UpdateState.DOWNLOADING) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Downloading in background...",
                                    color = TabCream,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = "$downloadProgress%",
                                    color = AccentGold,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { downloadProgress / 100f },
                                color = AccentGold,
                                trackColor = CardBg,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Download runs securely in the background. You can leave this screen or minimize the app; the download will continue and notify you when finished.",
                                color = TabCream.copy(alpha = 0.65f),
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                    }

                    // Primary Action Button
                    Button(
                        onClick = {
                            val apk = updateManager.downloadedApkFile
                            if (updateState == UpdateState.DOWNLOADED && apk != null) {
                                if (!updateManager.canRequestPackageInstalls()) {
                                    showInstallPermissionDialog = true
                                } else {
                                    showInstallExplainerDialog = true
                                }
                            } else if (rel != null) {
                                updateManager.startDownload(rel) { _ ->
                                    if (!updateManager.canRequestPackageInstalls()) {
                                        showInstallPermissionDialog = true
                                    } else {
                                        showInstallExplainerDialog = true
                                    }
                                }
                            }
                        },
                        enabled = updateState != UpdateState.DOWNLOADING,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        if (updateState == UpdateState.DOWNLOADING) {
                            CircularProgressIndicator(color = Color.Black, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Downloading ($downloadProgress%)...", color = Color.Black, fontWeight = FontWeight.Bold)
                        } else if (updateState == UpdateState.DOWNLOADED) {
                            Icon(imageVector = Icons.Default.SystemUpdate, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Install Update Now", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        } else {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download & Install Update", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }

                    if (updateState == UpdateState.DOWNLOADING) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { updateManager.cancelDownload() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cancel Download", color = ErrorRed, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Secondary Buttons: Later and Skip
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = safeClose,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Text("Later", color = TabCream)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedButton(
                            onClick = {
                                val currentRel = rel
                                if (currentRel != null) {
                                    updateManager.setSkippedVersion(currentRel.version)
                                } else {
                                    updateManager.cancelDownload()
                                }
                                safeClose()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Text("Skip Version", color = TabCream.copy(alpha = 0.7f))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Re-check action to skip to newer updates directly if published
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                updateManager.checkForUpdates(forceRefresh = true)
                            }
                        },
                        enabled = updateState != UpdateState.DOWNLOADING && updateState != UpdateState.CHECKING,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardOutline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Check for Newer Updates",
                            color = TabCream,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Extra bottom clearance so buttons are never covered by navigation bar
                    Spacer(modifier = Modifier.height(56.dp))
                }
            }

            // Version History & Rollback Section
            if (rollbackReleases.isNotEmpty() || updateState == UpdateState.NO_UPDATE) {
                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardOutline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isRollbackExpanded = !isRollbackExpanded }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Version History & Rollback",
                                    color = TabCream,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (rollbackReleases.isNotEmpty()) {
                                        "${rollbackReleases.size} previous versions available"
                                    } else {
                                        "No earlier releases available in repository"
                                    },
                                    color = TabCream.copy(alpha = 0.65f),
                                    fontSize = 12.sp
                                )
                            }
                            Icon(
                                imageVector = if (isRollbackExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isRollbackExpanded) "Collapse" else "Expand",
                                tint = TabCream.copy(alpha = 0.8f),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        AnimatedVisibility(visible = isRollbackExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (rollbackReleases.isEmpty()) {
                                    Text(
                                        text = "No previous releases available for rollback.",
                                        color = TabCream.copy(alpha = 0.6f),
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                } else {
                                    rollbackReleases.forEach { pastRel ->
                                        RollbackReleaseCard(
                                            release = pastRel,
                                            isDownloading = updateState == UpdateState.DOWNLOADING && activeRollbackTarget?.version == pastRel.version,
                                            downloadProgress = downloadProgress,
                                            isDownloaded = updateState == UpdateState.DOWNLOADED && activeRollbackTarget?.version == pastRel.version,
                                            isNotesExpanded = expandedReleaseNotesVersion == pastRel.version,
                                            onToggleNotes = {
                                                expandedReleaseNotesVersion = if (expandedReleaseNotesVersion == pastRel.version) null else pastRel.version
                                            },
                                            onRollbackClick = {
                                                confirmRollbackRelease = pastRel
                                            },
                                            onInstallClick = {
                                                val apk = updateManager.downloadedApkFile
                                                if (apk != null) {
                                                    if (!updateManager.canRequestPackageInstalls()) {
                                                        showInstallPermissionDialog = true
                                                    } else {
                                                        updateManager.triggerApkInstall(apk)
                                                    }
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

            Spacer(modifier = Modifier.height(56.dp))
        }
    }

    // Permission Dialog for Unknown App Sources
    if (showInstallPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showInstallPermissionDialog = false },
            title = {
                Text(
                    text = "Permission Required",
                    fontWeight = FontWeight.Bold,
                    color = TabCream
                )
            },
            text = {
                Text(
                    text = "To install updates, Android requires permission to install apps outside the store. Tap 'Open Settings' and enable 'Allow from this source' for Fotara, then return here to complete installation.",
                    color = TabCream.copy(alpha = 0.88f),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showInstallPermissionDialog = false
                        updateManager.openInstallPermissionSettings()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold)
                ) {
                    Text("Open Settings", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showInstallPermissionDialog = false }) {
                    Text("Cancel", color = TabCream)
                }
            },
            containerColor = CardBg,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // One-time Sideload Explainer Dialog
    if (showInstallExplainerDialog) {
        AlertDialog(
            onDismissRequest = { showInstallExplainerDialog = false },
            title = {
                Text(
                    text = "Install Update Package",
                    fontWeight = FontWeight.Bold,
                    color = TabCream
                )
            },
            text = {
                Text(
                    text = "The update APK has been downloaded directly from the official Arinara GitHub release channel. Tap proceed to start the system package installer.",
                    color = TabCream.copy(alpha = 0.85f),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showInstallExplainerDialog = false
                        val apk = updateManager.downloadedApkFile
                        if (apk != null) {
                            if (!updateManager.canRequestPackageInstalls()) {
                                showInstallPermissionDialog = true
                            } else {
                                updateManager.triggerApkInstall(apk)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold)
                ) {
                    Text("Proceed to Install", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showInstallExplainerDialog = false }) {
                    Text("Cancel", color = TabCream)
                }
            },
            containerColor = CardBg,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Rollback Cautionary Confirmation Dialog
    if (confirmRollbackRelease != null) {
        val target = confirmRollbackRelease!!
        val targetInfo = remember(target) { VersionInfo.parse(target.version, target.isPrerelease) }
        AlertDialog(
            onDismissRequest = { confirmRollbackRelease = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = AccentGold,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Rollback to ${targetInfo.displayVersion}?",
                        fontWeight = FontWeight.Bold,
                        color = TabCream
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Rolling back downloads and installs an earlier release (${target.version}) over your current installation (${currentVersion}).",
                        color = TabCream.copy(alpha = 0.9f),
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Android package downgrades may carry compatibility differences. We strongly recommend exporting a backup of your study notes and photos first to ensure complete peace of mind.",
                        color = AccentGold.copy(alpha = 0.9f),
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val relToDownload = target
                        confirmRollbackRelease = null
                        updateManager.startRollbackDownload(relToDownload)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold)
                ) {
                    Text("Download & Rollback", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    if (settingsRepository != null) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    try {
                                        val backupJson = settingsRepository.exportDataBackup()
                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("Fotara Backup", backupJson)
                                        clipboard?.setPrimaryClip(clip)
                                        android.widget.Toast.makeText(context, "Notes backup copied to clipboard!", android.widget.Toast.LENGTH_LONG).show()
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Backup failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        ) {
                            Text("Backup First", color = Color(0xFF60A5FA))
                        }
                    }
                    TextButton(onClick = { confirmRollbackRelease = null }) {
                        Text("Cancel", color = TabCream)
                    }
                }
            },
            containerColor = CardBg,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun RollbackReleaseCard(
    release: ReleaseInfo,
    isDownloading: Boolean,
    downloadProgress: Int,
    isDownloaded: Boolean,
    isNotesExpanded: Boolean,
    onToggleNotes: () -> Unit,
    onRollbackClick: () -> Unit,
    onInstallClick: () -> Unit
) {
    val relInfo = remember(release) { VersionInfo.parse(release.version, release.isPrerelease) }
    val formattedDate = remember(release.publishedAt) {
        release.publishedAt?.take(10) ?: ""
    }

    Surface(
        color = Color(0xFF0F142A),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardOutline.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Fotara ${relInfo.displayVersion}",
                            color = TabCream,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        com.arinara.fotara.ui.components.ChannelPill(channel = relInfo.channelLabel)
                    }
                    if (formattedDate.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Released: $formattedDate",
                            color = TabCream.copy(alpha = 0.55f),
                            fontSize = 11.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (isDownloading) {
                    Column(horizontalAlignment = Alignment.End) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = AccentGold,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "$downloadProgress%",
                            color = AccentGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (isDownloaded) {
                    Button(
                        onClick = onInstallClick,
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Install", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedButton(
                        onClick = onRollbackClick,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentGold.copy(alpha = 0.6f)),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(15.dp), tint = AccentGold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rollback", color = AccentGold, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (release.releaseNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .clickable(onClick = onToggleNotes)
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isNotesExpanded) "Hide release notes" else "View release notes",
                        color = Color(0xFF60A5FA),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = if (isNotesExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (isNotesExpanded) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = Color(0xFF0A0D18),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = release.releaseNotes.trim(),
                            color = TabCream.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }
    }
}
