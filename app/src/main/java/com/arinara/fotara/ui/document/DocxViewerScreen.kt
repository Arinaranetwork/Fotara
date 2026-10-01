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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.arinara.fotara.ui.components.NoteDetailScheduleChip
import com.arinara.fotara.ui.components.ScheduleNoteDialog
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

private val ScreenNavy = Color(0xFF03071E)
private val TabCream = Color(0xFFEAE3D2)
private val AccentGold = Color(0xFFF77F00)
private val CardBg = Color(0xFF141936)
private val DangerRed = Color(0xFFD62828)
private val TableBorderColor = Color(0xFF2A3362)

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenNavy)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = documentNote.name,
                        color = TabCream,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Reflowed Word Document",
                        color = TabCream.copy(alpha = 0.7f),
                        fontSize = 12.sp
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
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TabCream
                    )
                }
            },
            actions = {
                IconButton(onClick = openWithExternalApp) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open in External App",
                        tint = AccentGold
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Document",
                        tint = DangerRed
                    )
                }

                // Overflow menu for Schedule...
                Box {
                    var showOverflowMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showOverflowMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More Options",
                            tint = TabCream
                        )
                    }
                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false },
                        modifier = Modifier
                            .background(ScreenNavy)
                            .border(1.dp, Color(0xFF28325E), RoundedCornerShape(8.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Schedule...", color = TabCream) },
                            leadingIcon = { Icon(Icons.Default.Alarm, null, tint = AccentGold) },
                            onClick = {
                                showOverflowMenu = false
                                showScheduleDialog = true
                            }
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = ScreenNavy)
        )

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AccentGold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Parsing document structure…",
                            color = TabCream.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                    }
                }
            }
            errorMessage != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(60.dp))
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = DangerRed,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Cannot Display Document",
                        color = TabCream,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = TabCream.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = openWithExternalApp,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold)
                    ) {
                        Text("Try Opening in External App", color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = onBack) {
                        Text("Go Back", color = TabCream)
                    }
                }
            }
            elements != null -> {
                SelectionContainer {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
                    ) {
                        // Notice banner explaining reflowable format
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                colors = CardDefaults.cardColors(containerColor = CardBg),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, TableBorderColor)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = AccentGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Reflowed mobile view. Use 'Open with' icon at top right for original Word pagination.",
                                        color = TabCream.copy(alpha = 0.8f),
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
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
                                        color = TabCream,
                                        fontSize = size,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                                    )
                                }
                                is DocxElement.Paragraph -> {
                                    Text(
                                        text = elem.text,
                                        color = TabCream,
                                        fontSize = 15.sp,
                                        lineHeight = 22.sp,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                                is DocxElement.ListItem -> {
                                    Row(modifier = Modifier.padding(vertical = 3.dp, horizontal = 4.dp)) {
                                        Text(
                                            text = "• ",
                                            color = AccentGold,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = elem.text,
                                            color = TabCream,
                                            fontSize = 15.sp,
                                            lineHeight = 21.sp
                                        )
                                    }
                                }
                                is DocxElement.Table -> {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        colors = CardDefaults.cardColors(containerColor = CardBg),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, TableBorderColor)
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
                                                        .background(if (rIdx % 2 == 0) Color.Transparent else TableBorderColor.copy(alpha = 0.2f))
                                                ) {
                                                    for (cell in row) {
                                                        Box(
                                                            modifier = Modifier
                                                                .width(140.dp)
                                                                .border(0.5.dp, TableBorderColor)
                                                                .padding(6.dp)
                                                        ) {
                                                            Text(
                                                                text = cell,
                                                                color = TabCream,
                                                                fontSize = 13.sp,
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
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        colors = CardDefaults.cardColors(containerColor = CardBg)
                                    ) {
                                        AsyncImage(
                                            model = File(elem.localFilePath),
                                            contentDescription = "Embedded Image",
                                            contentScale = ContentScale.FillWidth,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                                is DocxElement.Unsupported -> {
                                    // Visible explicit placeholder ensuring content is never silently dropped
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        colors = CardDefaults.cardColors(containerColor = TableBorderColor.copy(alpha = 0.4f)),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null,
                                                tint = Color(0xFF8E9AAF),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "[${elem.description} — view via Open with]",
                                                color = Color(0xFFB0B9D0),
                                                fontSize = 12.sp,
                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
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
