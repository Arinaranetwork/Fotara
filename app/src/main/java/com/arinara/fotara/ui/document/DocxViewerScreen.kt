// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.document

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.arinara.fotara.ui.theme.jigglePressClickable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import com.arinara.fotara.ui.components.presets.AppDropdownMenu
import com.arinara.fotara.ui.components.presets.AppOverflowMenuTrigger
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.ui.components.NoteDetailScheduleChip
import com.arinara.fotara.ui.components.ScheduleNoteDialog
import com.arinara.fotara.ui.components.presets.AppButton
import com.arinara.fotara.ui.components.presets.AppButtonVariant
import com.arinara.fotara.ui.components.presets.AppCard
import com.arinara.fotara.ui.components.presets.AppScreen
import com.arinara.fotara.ui.components.presets.AppTopBar
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import com.arinara.fotara.util.NoteScheduleManager
import com.arinara.fotara.util.ScheduleNoteType
import com.arinara.fotara.util.ScheduleAlertType
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.arinara.fotara.data.model.DocumentNote
import com.arinara.fotara.util.DocxElement
import com.arinara.fotara.util.DocxParser
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocxViewerScreen(
    documentNote: DocumentNote,
    onBack: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var elements by remember { mutableStateOf<List<DocxElement>?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var currentScheduledAt by remember { mutableStateOf(documentNote.scheduledAt) }
    var currentAlertType by remember { mutableStateOf(documentNote.alertType) }
    var currentScheduleTitle by remember { mutableStateOf(documentNote.scheduleTitle) }
    var showScheduleDialog by remember { mutableStateOf(false) }

    val file = remember(documentNote.originFileUri) { File(documentNote.originFileUri) }

    val openWithExternalApp: () -> Unit = {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Open DOCX with"))
        } catch (e: Exception) {
            android.util.Log.e("DocxViewer", "Open with failed: ${e.message}")
        }
    }

    LaunchedEffect(file) {
        isLoading = true
        errorMessage = null
        try {
            val parsed = DocxParser.parseDocx(context, file)
            elements = parsed
            isLoading = false
        } catch (e: Exception) {
            errorMessage = e.message ?: "Failed to read Word document."
            isLoading = false
        }
    }

    AppScreen(
        containerColor = ElectricSlateTokens.BgCanvas,
        topBar = {
            AppTopBar(
                onBackClick = onBack,
                titleContent = {
                    Column(verticalArrangement = Arrangement.Center) {
                        Text(
                            text = documentNote.name,
                            color = ElectricSlateTokens.TextPrimary,
                            fontSize = 20.sp,
                            lineHeight = 24.sp,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Reflowed Word Document",
                            color = ElectricSlateTokens.TextMuted,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Normal
                        )
                        if (currentScheduledAt != null) {
                            Spacer(modifier = Modifier.height(2.dp))
                            NoteDetailScheduleChip(
                                scheduledAt = currentScheduledAt,
                                alertType = currentAlertType,
                                scheduleTitle = currentScheduleTitle,
                                onClick = { showScheduleDialog = true }
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = openWithExternalApp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open in External App",
                            tint = ElectricSlateTokens.PrimaryAccent
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Document",
                            tint = ElectricSlateTokens.Destructive
                        )
                    }

                    // Overflow menu for Schedule...
                    var showOverflowMenu by remember { mutableStateOf(false) }
                    AppOverflowMenuTrigger(
                        expanded = showOverflowMenu,
                        onClick = { showOverflowMenu = true },
                        contentDescription = "More Options",
                        iconTint = ElectricSlateTokens.TextPrimary,
                        containerSize = 36.dp,
                        iconSize = 20.dp
                    ) {
                        AppDropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Schedule...", color = ElectricSlateTokens.TextPrimary) },
                                leadingIcon = { Icon(Icons.Default.Alarm, null, tint = ElectricSlateTokens.TextMuted) },
                                onClick = {
                                    showOverflowMenu = false
                                    showScheduleDialog = true
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = ElectricSlateTokens.PrimaryAccent)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Parsing document structure…",
                            color = ElectricSlateTokens.TextMuted,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            errorMessage != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = ElectricSlateTokens.Destructive,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Cannot Display Document",
                        color = ElectricSlateTokens.TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = ElectricSlateTokens.TextMuted,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    AppButton(
                        text = "Try Opening in External App",
                        onClick = openWithExternalApp,
                        variant = AppButtonVariant.PRIMARY
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AppButton(
                        text = "Go Back",
                        onClick = onBack,
                        variant = AppButtonVariant.OUTLINED
                    )
                }
            }
            elements != null -> {
                SelectionContainer {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        contentPadding = innerPadding
                    ) {
                        // Notice banner explaining reflowable format
                        item {
                            AppCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                containerColor = ElectricSlateTokens.SurfacePanel,
                                borderColor = ElectricSlateTokens.BorderSubtle,
                                cornerRadius = 14.dp,
                                contentPadding = 12.dp
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = ElectricSlateTokens.PrimaryAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Reflowed mobile view. Use 'Open with' icon at top right for original Word pagination.",
                                        color = ElectricSlateTokens.TextMuted,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }

                        items(elements!!) { elem ->
                            when (elem) {
                                is DocxElement.Heading -> {
                                    val size = when (elem.level) {
                                        1 -> 22.sp
                                        2 -> 18.sp
                                        else -> 16.sp
                                    }
                                    Text(
                                        text = elem.text,
                                        color = ElectricSlateTokens.TextPrimary,
                                        fontSize = size,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                                    )
                                }
                                is DocxElement.Paragraph -> {
                                    Text(
                                        text = elem.text,
                                        color = ElectricSlateTokens.TextPrimary,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                                is DocxElement.ListItem -> {
                                    Row(modifier = Modifier.padding(vertical = 3.dp, horizontal = 4.dp)) {
                                        Text(
                                            text = "• ",
                                            color = ElectricSlateTokens.PrimaryAccent,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = elem.text,
                                            color = ElectricSlateTokens.TextPrimary,
                                            fontSize = 14.sp,
                                            lineHeight = 20.sp
                                        )
                                    }
                                }
                                is DocxElement.Table -> {
                                    AppCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        containerColor = ElectricSlateTokens.SurfacePanel,
                                        borderColor = ElectricSlateTokens.BorderSubtle,
                                        cornerRadius = 14.dp,
                                        contentPadding = 8.dp
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState())
                                                .padding(8.dp)
                                        ) {
                                            for ((rIdx, row) in elem.rows.withIndex()) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(if (rIdx % 2 == 0) Color.Transparent else ElectricSlateTokens.BorderSubtle.copy(alpha = 0.3f))
                                                ) {
                                                    for (cell in row) {
                                                        Box(
                                                            modifier = Modifier
                                                                .width(140.dp)
                                                                .border(0.5.dp, ElectricSlateTokens.BorderSubtle)
                                                                .padding(6.dp)
                                                        ) {
                                                            Text(
                                                                text = cell,
                                                                color = ElectricSlateTokens.TextPrimary,
                                                                fontSize = 14.sp,
                                                                fontWeight = if (rIdx == 0) FontWeight.Bold else FontWeight.Normal
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                is DocxElement.Image -> {
                                    AppCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        containerColor = ElectricSlateTokens.SurfacePanel,
                                        borderColor = ElectricSlateTokens.BorderSubtle,
                                        cornerRadius = 16.dp,
                                        contentPadding = 0.dp
                                    ) {
                                        AsyncImage(
                                            model = File(elem.localFilePath),
                                            contentDescription = "Embedded Image",
                                            contentScale = ContentScale.FillWidth,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                        )
                                    }
                                }
                                is DocxElement.Unsupported -> {
                                    AppCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        containerColor = ElectricSlateTokens.SurfaceElevated,
                                        borderColor = ElectricSlateTokens.BorderSubtle,
                                        cornerRadius = 12.dp,
                                        contentPadding = 8.dp
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null,
                                                tint = ElectricSlateTokens.TextMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "[${elem.description} — view via Open with]",
                                                color = ElectricSlateTokens.TextMuted,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showScheduleDialog) {
        val scheduleManager = remember { NoteScheduleManager(context) }
        ScheduleNoteDialog(
            noteTitle = documentNote.name,
            initialScheduledAt = currentScheduledAt,
            initialAlertType = try {
                ScheduleAlertType.valueOf(currentAlertType ?: "NOTIFICATION")
            } catch (_: Exception) {
                ScheduleAlertType.NOTIFICATION
            },
            initialScheduleTitle = currentScheduleTitle,
            onDismiss = { showScheduleDialog = false },
            onSaveSchedule = { scheduledAt, alertType, scheduleTitle ->
                scheduleManager.scheduleNote(
                    noteType = ScheduleNoteType.DOCUMENT,
                    noteId = documentNote.id,
                    folderId = documentNote.folderId,
                    title = documentNote.name,
                    triggerAtMillis = scheduledAt,
                    alertType = alertType,
                    scheduleTitle = scheduleTitle
                )
                currentScheduledAt = scheduledAt
                currentAlertType = alertType.name
                currentScheduleTitle = scheduleTitle
                showScheduleDialog = false
            },
            onClearSchedule = {
                scheduleManager.cancelSchedule(ScheduleNoteType.DOCUMENT, documentNote.id)
                currentScheduledAt = null
                currentAlertType = null
                currentScheduleTitle = null
                showScheduleDialog = false
            }
        )
    }
}
