// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.backup.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.data.model.Space
import com.arinara.fotara.data.model.Workspace
import com.arinara.fotara.data.repository.SpaceRepository
import com.arinara.fotara.data.repository.WorkspaceRepository
import com.arinara.fotara.feature.backup.BackupManifest
import com.arinara.fotara.feature.backup.BackupScope
import com.arinara.fotara.feature.backup.FotaraBackupManager
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.theme.HomeCardBorder
import com.arinara.fotara.theme.HomeCardSurface
import com.arinara.fotara.theme.HomeMainButtonBlue
import com.arinara.fotara.theme.HomeNearBlack
import com.arinara.fotara.theme.TagCrimson
import com.arinara.fotara.theme.TextMuted
import com.arinara.fotara.theme.TextPrimary
import com.arinara.fotara.theme.TextSecondary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupRestoreScreen(
    onBack: () -> Unit,
    spaceRepository: SpaceRepository? = null,
    workspaceRepository: WorkspaceRepository? = null,
    onDataRestored: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val backupManager = remember { FotaraBackupManager(context) }

    var isEntireApp by remember { mutableStateOf(true) }
    var availableSpaces by remember { mutableStateOf<List<Space>>(emptyList()) }
    var availableWorkspaces by remember { mutableStateOf<List<Workspace>>(emptyList()) }
    var selectedSpaceUuids by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedWorkspaceUuids by remember { mutableStateOf<Set<String>>(emptySet()) }

    var isProcessing by remember { mutableStateOf(false) }
    var processingTitle by remember { mutableStateOf("") }
    var progressPercent by remember { mutableFloatStateOf(0f) }

    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var validatedManifest by remember { mutableStateOf<BackupManifest?>(null) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (spaceRepository != null) {
            spaceRepository.observeSpaces().collect { spaces ->
                availableSpaces = spaces
                selectedSpaceUuids = spaces.map { it.uuid }.toSet()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (workspaceRepository != null) {
            val wsList = workspaceRepository.getWorkspacesSync()
            availableWorkspaces = wsList
            selectedWorkspaceUuids = wsList.map { it.uuid }.toSet()
        }
    }

    // SAF Create Document Launcher for Export
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isProcessing = true
                processingTitle = "Exporting .fotara Backup..."
                progressPercent = 0.1f

                try {
                    val scope = if (isEntireApp) {
                        BackupScope.EntireApp
                    } else {
                        BackupScope.Selective(selectedSpaceUuids, selectedWorkspaceUuids)
                    }

                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        val result = backupManager.exportBackup(
                            outputStream = outputStream,
                            scope = scope,
                            onProgress = { p -> progressPercent = p }
                        )

                        if (result.isSuccess) {
                            val manifest = result.getOrThrow()
                            Toast.makeText(
                                context,
                                "Backup exported successfully! (${manifest.folderCount} folders, ${manifest.photoCount + manifest.textNoteCount} notes)",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                "Export failed: ${result.exceptionOrNull()?.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    // SAF Open Document Launcher for Restore
    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isProcessing = true
                processingTitle = "Verifying .fotara Package Integrity..."
                progressPercent = 0.3f

                try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val validation = backupManager.validateBackup(inputStream)
                        if (validation.isValid && validation.manifest != null) {
                            pendingRestoreUri = uri
                            validatedManifest = validation.manifest
                            showRestoreConfirmDialog = true
                        } else {
                            Toast.makeText(
                                context,
                                "Validation failed: ${validation.errorMessage ?: "Corrupted file"}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Cannot read file: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeNearBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Backup & Restore (.fotara)",
                    color = TextPrimary,
                    fontFamily = ElmsSans,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Offline Archive & Complete Data Portability",
                    color = TextSecondary,
                    fontFamily = ElmsSans,
                    fontSize = 12.sp
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // SECTION 1: EXPORT CARD
            Text(
                text = "EXPORT DATA BACKUP",
                fontFamily = ElmsSans,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = HomeCardSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, HomeCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = null,
                            tint = HomeMainButtonBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Create .fotara Archive",
                            color = TextPrimary,
                            fontFamily = ElmsSans,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Generates a compressed, tamper-verified .fotara container bundling all your notes, schedules, workspaces, and media attachments.",
                        color = TextSecondary,
                        fontFamily = ElmsSans,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Option 1: Entire App
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isEntireApp = true }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isEntireApp,
                            onClick = { isEntireApp = true },
                            colors = RadioButtonDefaults.colors(selectedColor = HomeMainButtonBlue)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Entire App Backup",
                                color = TextPrimary,
                                fontFamily = ElmsSans,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "All spaces, workspaces, notes, attachments, and schedules",
                                color = TextSecondary,
                                fontFamily = ElmsSans,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Option 2: Selective
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isEntireApp = false }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = !isEntireApp,
                            onClick = { isEntireApp = false },
                            colors = RadioButtonDefaults.colors(selectedColor = HomeMainButtonBlue)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Selective Export",
                                color = TextPrimary,
                                fontFamily = ElmsSans,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Choose specific spaces or workspaces to bundle",
                                color = TextSecondary,
                                fontFamily = ElmsSans,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Selective Pickers
                    AnimatedVisibility(visible = !isEntireApp) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, start = 12.dp)
                        ) {
                            Text(
                                text = "Select Spaces:",
                                color = TextPrimary,
                                fontFamily = ElmsSans,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            availableSpaces.forEach { sp ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedSpaceUuids = if (sp.uuid in selectedSpaceUuids) {
                                                selectedSpaceUuids - sp.uuid
                                            } else {
                                                selectedSpaceUuids + sp.uuid
                                            }
                                        }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Checkbox(
                                        checked = sp.uuid in selectedSpaceUuids,
                                        onCheckedChange = { checked ->
                                            selectedSpaceUuids = if (checked) {
                                                selectedSpaceUuids + sp.uuid
                                            } else {
                                                selectedSpaceUuids - sp.uuid
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = HomeMainButtonBlue)
                                    )
                                    Text(
                                        text = sp.name,
                                        color = TextPrimary,
                                        fontFamily = ElmsSans,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val timeStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                            exportLauncher.launch("Fotara_Backup_$timeStr.fotara")
                        },
                        enabled = !isProcessing,
                        colors = ButtonDefaults.buttonColors(containerColor = HomeMainButtonBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Export .fotara Backup",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // SECTION 2: RESTORE CARD
            Text(
                text = "RESTORE DATA ARCHIVE",
                fontFamily = ElmsSans,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = HomeCardSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, HomeCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = null,
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Import & Restore .fotara",
                            color = TextPrimary,
                            fontFamily = ElmsSans,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Import a valid .fotara archive to restore coursework records, schedules, and attachments. Checksums are verified before applying any changes.",
                        color = TextSecondary,
                        fontFamily = ElmsSans,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            restoreLauncher.launch(arrayOf("*/*"))
                        },
                        enabled = !isProcessing,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderZip,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Select .fotara File to Restore",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Progress Dialog
    if (isProcessing) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = Color(0xFF0F1422),
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(processingTitle, color = TextPrimary, fontFamily = ElmsSans, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        progress = { progressPercent },
                        color = HomeMainButtonBlue,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "${(progressPercent * 100).toInt()}% completed",
                        color = TextSecondary,
                        fontFamily = ElmsSans,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {}
        )
    }

    // Restore Confirmation Dialog
    if (showRestoreConfirmDialog && validatedManifest != null && pendingRestoreUri != null) {
        val m = validatedManifest!!
        val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US).format(Date(m.createdAt))

        AlertDialog(
            onDismissRequest = {
                showRestoreConfirmDialog = false
                pendingRestoreUri = null
            },
            containerColor = Color(0xFF0F1422),
            shape = RoundedCornerShape(16.dp),
            title = {
                Text("Restore Backup Archive?", color = TextPrimary, fontFamily = ElmsSans, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "The archive is verified and ready to be merged:",
                        color = TextSecondary,
                        fontFamily = ElmsSans,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("• Created: $dateStr", color = TextPrimary, fontFamily = ElmsSans, fontSize = 12.sp)
                    Text("• Folders: ${m.folderCount}", color = TextPrimary, fontFamily = ElmsSans, fontSize = 12.sp)
                    Text("• Notes: ${m.photoCount + m.textNoteCount + m.documentCount}", color = TextPrimary, fontFamily = ElmsSans, fontSize = 12.sp)
                    Text("• Media Attachments: ${m.attachmentCount}", color = TextPrimary, fontFamily = ElmsSans, fontSize = 12.sp)
                    Text("• Schedules: ${m.scheduleCount}", color = TextPrimary, fontFamily = ElmsSans, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Existing local records will not be overwritten; new data will be safely merged.",
                        color = TextMuted,
                        fontFamily = ElmsSans,
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = pendingRestoreUri
                        showRestoreConfirmDialog = false
                        if (uri != null) {
                            coroutineScope.launch {
                                isProcessing = true
                                processingTitle = "Restoring .fotara Data..."
                                progressPercent = 0.2f
                                try {
                                    context.contentResolver.openInputStream(uri)?.use { stream ->
                                        val restoreResult = backupManager.restoreBackup(stream) { p ->
                                            progressPercent = p
                                        }
                                        if (restoreResult.success) {
                                            Toast.makeText(
                                                context,
                                                "Restore complete: ${restoreResult.restoredFolders} folders, ${restoreResult.restoredNotes} notes, ${restoreResult.restoredAttachments} attachments restored.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            onDataRestored()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Restore failed: ${restoreResult.errorMessage}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    isProcessing = false
                                    pendingRestoreUri = null
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HomeMainButtonBlue)
                ) {
                    Text("Restore Now", color = Color.White, fontFamily = ElmsSans, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestoreConfirmDialog = false
                    pendingRestoreUri = null
                }) {
                    Text("Cancel", color = TextSecondary, fontFamily = ElmsSans)
                }
            }
        )
    }
}
