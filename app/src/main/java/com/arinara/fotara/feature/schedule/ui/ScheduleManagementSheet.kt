// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.schedule.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.feature.schedule.engine.ScheduleCutoffEngine
import com.arinara.fotara.feature.schedule.model.ClassSchedule
import com.arinara.fotara.feature.schedule.notification.ScheduleNotificationScheduler
import com.arinara.fotara.feature.schedule.parser.RawTableData
import com.arinara.fotara.feature.schedule.parser.ScheduleColumnMapping
import com.arinara.fotara.feature.schedule.parser.XlsxDocxScheduleParser
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.theme.HomeCardBorder
import com.arinara.fotara.theme.HomeCardSurface
import com.arinara.fotara.theme.HomeMainButtonBlue
import com.arinara.fotara.theme.HomeNearBlack
import com.arinara.fotara.theme.HomeSubtitleGray
import com.arinara.fotara.theme.TagCrimson
import com.arinara.fotara.theme.TextPrimary
import com.arinara.fotara.theme.TextSecondary
import com.arinara.fotara.theme.TextMuted
import com.arinara.fotara.ui.components.LocalBottomOverlayPadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleManagementSheet(
    isOpen: Boolean,
    schedules: List<ClassSchedule>,
    folders: List<Folder>,
    cutoffTimeStr: String,
    onDismissRequest: () -> Unit,
    onSaveSchedule: (ClassSchedule) -> Unit,
    onDeleteSchedule: (Long) -> Unit,
    onBatchImportSchedules: (List<ClassSchedule>) -> Unit,
    onUpdateCutoffTime: (String) -> Unit
) {
    if (!isOpen) return

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedDayOfWeek by remember {
        mutableIntStateOf(ScheduleCutoffEngine.getCurrentDayOfWeek())
    }

    // Dialog states
    var editingSchedule by remember { mutableStateOf<ClassSchedule?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }
    var scheduleToDelete by remember { mutableStateOf<ClassSchedule?>(null) }
    var isEditingCutoff by remember { mutableStateOf(false) }
    var showAntiProcrastinationScreen by remember { mutableStateOf(false) }

    // Import states
    var pendingImportTableData by remember { mutableStateOf<RawTableData?>(null) }
    var detectedMapping by remember { mutableStateOf(ScheduleColumnMapping()) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val parser = XlsxDocxScheduleParser()
                    val tableData = withContext(Dispatchers.IO) {
                        parser.parseUri(context, uri)
                    }
                    if (tableData.rows.isEmpty()) {
                        Toast.makeText(context, "No table data found in the selected file", Toast.LENGTH_SHORT).show()
                    } else {
                        val mapping = parser.autoDetectMapping(tableData)
                        detectedMapping = mapping
                        pendingImportTableData = tableData
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to read file: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = HomeNearBlack,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4B5563))
            )
        }
    ) {
        val bottomOverlayPadding = LocalBottomOverlayPadding.current

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(bottom = bottomOverlayPadding)
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Class Schedule",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        lineHeight = 28.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Manage weekly schedule & next-day prep",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontFamily = ElmsSans
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { isCreatingNew = true },
                        modifier = Modifier
                            .size(36.dp)
                            .background(HomeCardSurface, CircleShape)
                            .border(1.dp, HomeCardBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Class",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .size(36.dp)
                            .background(HomeCardSurface, CircleShape)
                            .border(1.dp, HomeCardBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Rollover Cutoff Time Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(HomeCardSurface)
                    .border(1.dp, HomeCardBorder, RoundedCornerShape(12.dp))
                    .clickable { isEditingCutoff = true }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = Color(0xFF818CF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Next-Day Rollover Cutoff: $cutoffTimeStr",
                            color = Color(0xFFE2E8F0),
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = "Edit",
                        color = HomeMainButtonBlue,
                        fontSize = 12.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Anti-Procrastination Study Alarm Card (Temporarily Disabled per A-024)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(HomeCardSurface.copy(alpha = 0.5f))
                    .border(1.dp, HomeCardBorder.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text(
                                text = "Anti-Procrastination Study Alarm",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Photo proof challenge (Temporarily Disabled)",
                                color = Color(0xFF6B7280),
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                fontFamily = ElmsSans
                            )
                        }
                    }

                    Text(
                        text = "Disabled",
                        color = Color(0xFF6B7280),
                        fontSize = 12.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Day Selector Pill Bar (Mon .. Sun)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (day in 1..7) {
                    val count = schedules.count { it.dayOfWeek == day }
                    val isSelected = selectedDayOfWeek == day
                    val shortName = ClassSchedule.getEnglishDayShortName(day)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) HomeMainButtonBlue else HomeCardSurface)
                            .border(
                                1.dp,
                                if (isSelected) HomeMainButtonBlue else HomeCardBorder,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedDayOfWeek = day }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = shortName,
                                color = if (isSelected) TextPrimary else Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                fontFamily = ElmsSans,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                            if (count > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(
                                            if (isSelected) TextPrimary.copy(alpha = 0.25f) else Color(0xFF334155),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = count.toString(),
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp,
                                        fontFamily = ElmsSans,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Schedules for Selected Day
            val daySchedules = schedules
                .filter { it.dayOfWeek == selectedDayOfWeek }
                .sortedBy { it.startMinute }

            if (daySchedules.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "No classes scheduled on ${ClassSchedule.getEnglishDayName(selectedDayOfWeek)}",
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Tap (+) to add, import files, or load sample schedule.",
                            color = HomeSubtitleGray,
                            fontSize = 12.sp,
                            fontFamily = ElmsSans
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                val samples = com.arinara.fotara.feature.schedule.model.SampleScheduleDataProvider.createSampleSchedules()
                                onBatchImportSchedules(samples)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HomeCardSurface),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, HomeCardBorder),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                text = "Load Sample Schedule",
                                color = HomeMainButtonBlue,
                                fontSize = 12.sp,
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(daySchedules, key = { it.id }) { schedule ->
                        ScheduleItemCard(
                            schedule = schedule,
                            folders = folders,
                            onEdit = { editingSchedule = schedule },
                            onDelete = { scheduleToDelete = schedule }
                        )
                    }
                }
            }

            // Bottom Actions Bar (Import Timetable & Settings Pill at Right Bottom)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        filePickerLauncher.launch(
                            arrayOf(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                "*/*"
                            )
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HomeCardSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HomeCardBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileUpload,
                        contentDescription = null,
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Import (.xlsx/.docx)",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }

                // Settings Pill at Right Bottom
                Surface(
                    onClick = { isEditingCutoff = true },
                    modifier = Modifier.height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = HomeCardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, HomeCardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Schedule Settings",
                            tint = Color(0xFF818CF8),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Settings",
                            color = TextPrimary,
                            fontFamily = ElmsSans,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Dialog
    if (isCreatingNew || editingSchedule != null) {
        val target = editingSchedule
        AddEditScheduleDialog(
            initial = target,
            defaultDayOfWeek = selectedDayOfWeek,
            folders = folders,
            onDismiss = {
                isCreatingNew = false
                editingSchedule = null
            },
            onSave = { saved ->
                onSaveSchedule(saved)
                isCreatingNew = false
                editingSchedule = null
            }
        )
    }

    // Delete confirmation
    if (scheduleToDelete != null) {
        val toDelete = scheduleToDelete!!
        AlertDialog(
            onDismissRequest = { scheduleToDelete = null },
            title = {
                Text(
                    text = "Delete Schedule",
                    color = TextPrimary,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = "Remove \"${toDelete.subjectName}\" from weekly schedule?",
                    color = Color(0xFFCBD5E1),
                    fontFamily = ElmsSans
                )
            },
            containerColor = HomeCardSurface,
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSchedule(toDelete.id)
                        scheduleToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TagCrimson)
                ) {
                    Text("Delete", fontFamily = ElmsSans)
                }
            },
            dismissButton = {
                TextButton(onClick = { scheduleToDelete = null }) {
                    Text("Cancel", color = TextPrimary, fontFamily = ElmsSans)
                }
            }
        )
    }

    // Schedule Settings & Cutoff Dialog
    if (isEditingCutoff) {
        ScheduleSettingsDialog(
            currentCutoff = cutoffTimeStr,
            onDismiss = { isEditingCutoff = false },
            onConfirmCutoff = { newTime ->
                onUpdateCutoffTime(newTime)
                isEditingCutoff = false
            },
            onLoadSampleData = {
                val samples = com.arinara.fotara.feature.schedule.model.SampleScheduleDataProvider.createSampleSchedules()
                onBatchImportSchedules(samples)
                Toast.makeText(context, "Sample schedule loaded successfully", Toast.LENGTH_SHORT).show()
                isEditingCutoff = false
            }
        )
    }

    // Smart Column Mapping Dialog
    if (pendingImportTableData != null) {
        val tableData = pendingImportTableData!!
        SmartColumnMappingDialog(
            tableData = tableData,
            initialMapping = detectedMapping,
            onDismiss = { pendingImportTableData = null },
            onConfirm = { confirmedMapping ->
                val parser = XlsxDocxScheduleParser()
                val parsedSchedules = parser.mapToSchedules(tableData, confirmedMapping)
                if (parsedSchedules.isNotEmpty()) {
                    onBatchImportSchedules(parsedSchedules)
                    Toast.makeText(context, "Successfully imported ${parsedSchedules.size} classes", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "No class schedule data could be imported", Toast.LENGTH_SHORT).show()
                }
                pendingImportTableData = null
            }
        )
    }

    if (showAntiProcrastinationScreen) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showAntiProcrastinationScreen = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            com.arinara.fotara.alarm.procrastination.ui.AntiProcrastinationAlarmScreen(
                onBackClick = { showAntiProcrastinationScreen = false },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun ScheduleItemCard(
    schedule: ClassSchedule,
    folders: List<Folder>,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val linkedFolder = folders.firstOrNull { it.id == schedule.linkedFolderId }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(HomeCardSurface)
            .border(1.dp, HomeCardBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Time tag
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF38BDF8), CircleShape)
                    )
                    Text(
                        text = schedule.timeRangeFormatted,
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Subject name
                Text(
                    text = schedule.subjectName,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Room & Lecturer
                val details = buildList {
                    if (!schedule.roomName.isNullOrBlank()) add(schedule.roomName)
                    if (!schedule.instructorName.isNullOrBlank()) add(schedule.instructorName)
                }.joinToString(" • ")

                if (details.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = details,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontFamily = ElmsSans,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Linked folder badge
                if (linkedFolder != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E293B))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = linkedFolder.name,
                            color = Color(0xFF93C5FD),
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = TagCrimson.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddEditScheduleDialog(
    initial: ClassSchedule?,
    defaultDayOfWeek: Int,
    folders: List<Folder>,
    onDismiss: () -> Unit,
    onSave: (ClassSchedule) -> Unit
) {
    var subjectName by remember { mutableStateOf(initial?.subjectName ?: "") }
    var dayOfWeek by remember { mutableIntStateOf(initial?.dayOfWeek ?: defaultDayOfWeek) }
    var startTimeStr by remember { mutableStateOf(initial?.startTimeFormatted ?: "08:00") }
    var endTimeStr by remember { mutableStateOf(initial?.endTimeFormatted ?: "09:40") }
    var roomName by remember { mutableStateOf(initial?.roomName ?: "") }
    var instructorName by remember { mutableStateOf(initial?.instructorName ?: "") }
    var selectedFolderId by remember { mutableStateOf<Long?>(initial?.linkedFolderId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initial == null) "Add Class" else "Edit Class",
                color = TextPrimary,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = subjectName,
                    onValueChange = { subjectName = it },
                    label = { Text("Course / Subject", fontFamily = ElmsSans) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = HomeMainButtonBlue,
                        unfocusedBorderColor = HomeCardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Day selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (d in 1..7) {
                        val isSel = dayOfWeek == d
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) HomeMainButtonBlue else HomeCardBorder)
                                .clickable { dayOfWeek = d }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = ClassSchedule.getEnglishDayShortName(d),
                                color = TextPrimary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                fontFamily = ElmsSans,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Time Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startTimeStr,
                        onValueChange = { startTimeStr = it },
                        label = { Text("Start (08:00)", fontFamily = ElmsSans) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = HomeMainButtonBlue,
                            unfocusedBorderColor = HomeCardBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTimeStr,
                        onValueChange = { endTimeStr = it },
                        label = { Text("End (09:40)", fontFamily = ElmsSans) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = HomeMainButtonBlue,
                            unfocusedBorderColor = HomeCardBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = roomName,
                    onValueChange = { roomName = it },
                    label = { Text("Classroom (optional)", fontFamily = ElmsSans) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = HomeMainButtonBlue,
                        unfocusedBorderColor = HomeCardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = instructorName,
                    onValueChange = { instructorName = it },
                    label = { Text("Instructor (optional)", fontFamily = ElmsSans) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = HomeMainButtonBlue,
                        unfocusedBorderColor = HomeCardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        containerColor = HomeCardSurface,
        confirmButton = {
            Button(
                onClick = {
                    if (subjectName.isBlank()) return@Button
                    val startMin = ClassSchedule.parseTimeToMinutes(startTimeStr) ?: 480
                    val endMin = ClassSchedule.parseTimeToMinutes(endTimeStr) ?: (startMin + 100)

                    val schedule = (initial ?: ClassSchedule(
                        dayOfWeek = dayOfWeek,
                        startMinute = startMin,
                        endMinute = endMin,
                        subjectName = subjectName.trim(),
                        roomName = roomName.trim().ifBlank { null },
                        instructorName = instructorName.trim().ifBlank { null },
                        linkedFolderId = selectedFolderId
                    )).copy(
                        dayOfWeek = dayOfWeek,
                        startMinute = startMin,
                        endMinute = endMin,
                        subjectName = subjectName.trim(),
                        roomName = roomName.trim().ifBlank { null },
                        instructorName = instructorName.trim().ifBlank { null },
                        linkedFolderId = selectedFolderId,
                        updatedAt = System.currentTimeMillis()
                    )
                    onSave(schedule)
                },
                colors = ButtonDefaults.buttonColors(containerColor = HomeMainButtonBlue)
            ) {
                Text("Save", fontFamily = ElmsSans)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextPrimary, fontFamily = ElmsSans)
            }
        }
    )
}

@Composable
private fun ScheduleSettingsDialog(
    currentCutoff: String,
    onDismiss: () -> Unit,
    onConfirmCutoff: (String) -> Unit,
    onLoadSampleData: () -> Unit
) {
    val context = LocalContext.current
    var startTimeInput by remember { mutableStateOf("07:00") }
    var endTimeInput by remember { mutableStateOf(currentCutoff) }
    var reminderHourInput by remember { mutableStateOf("18:00") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var reminderStatusMessage by remember { mutableStateOf<String?>(null) }

    val presets = listOf("17:00", "18:00", "19:00", "20:00", "21:00")

    fun validateRollover(): Boolean {
        val s = ClassSchedule.parseTimeToMinutes(startTimeInput)
        val e = ClassSchedule.parseTimeToMinutes(endTimeInput)
        if (s == null || e == null) {
            errorMessage = "Please enter valid times (e.g. 07:00 and 18:00)"
            return false
        }
        val duration = if (e > s) e - s else (1440 - s) + e
        if (duration == 0 || duration >= 1440) {
            errorMessage = "Rollover duration cannot be 24 hours. Maximum is 23 hours."
            return false
        }
        if (duration > 23 * 60) {
            errorMessage = "Rollover duration cannot exceed 23 hours."
            return false
        }
        errorMessage = null
        return true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = HomeMainButtonBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Schedule Settings & Reminders",
                    color = TextPrimary,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Section 1: Rollover Cutoff
                Text(
                    text = "DAY ROLLOVER BOUNDS (MAX 23H)",
                    color = Color(0xFF818CF8),
                    fontSize = 12.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Set daily start and ending cutoff times. Setting a 24-hour duration is disabled (max 23 hours).",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontFamily = ElmsSans
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startTimeInput,
                        onValueChange = {
                            startTimeInput = it
                            errorMessage = null
                        },
                        label = { Text("Start Time", fontFamily = ElmsSans, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HomeMainButtonBlue,
                            unfocusedBorderColor = HomeCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = endTimeInput,
                        onValueChange = {
                            endTimeInput = it
                            errorMessage = null
                        },
                        label = { Text("End Time", fontFamily = ElmsSans, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HomeMainButtonBlue,
                            unfocusedBorderColor = HomeCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                // Quick Presets
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (p in presets) {
                        val isSel = endTimeInput == p
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) HomeMainButtonBlue else HomeCardBorder)
                                .clickable {
                                    endTimeInput = p
                                    errorMessage = null
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = p,
                                color = TextPrimary,
                                fontFamily = ElmsSans,
                                fontSize = 12.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = TagCrimson,
                        fontSize = 12.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Medium
                    )
                }

                HorizontalDivider(color = HomeCardBorder, thickness = 1.dp)

                // Section 2: Class Schedule Reminder
                Text(
                    text = "CLASS SCHEDULE REMINDER",
                    color = Color(0xFF60A5FA),
                    fontSize = 12.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Configure daily reminder notifications alerting your upcoming classes with <day>.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontFamily = ElmsSans
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = reminderHourInput,
                        onValueChange = { reminderHourInput = it },
                        label = { Text("Remind At", fontFamily = ElmsSans, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HomeMainButtonBlue,
                            unfocusedBorderColor = HomeCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Button(
                        onClick = {
                            val timeMin = ClassSchedule.parseTimeToMinutes(reminderHourInput) ?: (18 * 60)
                            val currentDay = ScheduleCutoffEngine.getCurrentDayOfWeek()
                            val dayText = ClassSchedule.getEnglishDayName(currentDay)
                            val scheduler = ScheduleNotificationScheduler(context)
                            scheduler.scheduleConfigurableReminder(
                                hour = timeMin / 60,
                                minute = timeMin % 60,
                                dayText = dayText
                            )
                            scheduler.showDigestNotification(
                                title = "Class Schedule Reminder",
                                message = dayText
                            )
                            reminderStatusMessage = "Notification dispatched for <$dayText>"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HomeCardSurface),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, HomeCardBorder),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Send Reminder", fontFamily = ElmsSans, fontSize = 12.sp)
                    }
                }

                if (reminderStatusMessage != null) {
                    Text(
                        text = reminderStatusMessage ?: "",
                        color = Color(0xFF10B981),
                        fontSize = 12.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Medium
                    )
                }

                HorizontalDivider(color = HomeCardBorder, thickness = 1.dp)

                // Section 3: Sample Data
                Text(
                    text = "SAMPLE TIMETABLE DATA",
                    color = Color(0xFFF59E0B),
                    fontSize = 12.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = {
                        onLoadSampleData()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HomeCardSurface),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HomeCardBorder)
                ) {
                    Text(
                        text = "Seed 5-Day Sample Schedule",
                        color = Color(0xFFF59E0B),
                        fontFamily = ElmsSans,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        containerColor = HomeCardSurface,
        confirmButton = {
            Button(
                onClick = {
                    if (validateRollover()) {
                        onConfirmCutoff(endTimeInput)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = HomeMainButtonBlue)
            ) {
                Text("Apply", fontFamily = ElmsSans)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextPrimary, fontFamily = ElmsSans)
            }
        }
    )
}

@Composable
private fun SmartColumnMappingDialog(
    tableData: RawTableData,
    initialMapping: ScheduleColumnMapping,
    onDismiss: () -> Unit,
    onConfirm: (ScheduleColumnMapping) -> Unit
) {
    var dayCol by remember { mutableIntStateOf(initialMapping.dayColumnIndex) }
    var timeCol by remember { mutableIntStateOf(initialMapping.timeColumnIndex) }
    var subjectCol by remember { mutableIntStateOf(initialMapping.subjectColumnIndex) }
    var roomCol by remember { mutableIntStateOf(initialMapping.roomColumnIndex) }
    var instructorCol by remember { mutableIntStateOf(initialMapping.instructorColumnIndex) }

    val allCols = tableData.headers.indices.toList()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Schedule Column Mapping",
                color = TextPrimary,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Confirm table columns matching your class schedule:",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontFamily = ElmsSans
                )

                // Day Selector
                Column {
                    Text("Day Column:", color = TextPrimary, fontSize = 12.sp, lineHeight = 16.sp, fontFamily = ElmsSans, fontWeight = FontWeight.SemiBold)
                    ColumnSelectorRow(allCols, tableData.headers, dayCol) { dayCol = it }
                }

                // Time Selector
                Column {
                    Text("Time Column:", color = TextPrimary, fontSize = 12.sp, lineHeight = 16.sp, fontFamily = ElmsSans, fontWeight = FontWeight.SemiBold)
                    ColumnSelectorRow(allCols, tableData.headers, timeCol) { timeCol = it }
                }

                // Subject Selector
                Column {
                    Text("Course / Subject Column:", color = TextPrimary, fontSize = 12.sp, lineHeight = 16.sp, fontFamily = ElmsSans, fontWeight = FontWeight.SemiBold)
                    ColumnSelectorRow(allCols, tableData.headers, subjectCol) { subjectCol = it }
                }

                // Room Selector
                Column {
                    Text("Classroom Column (Optional):", color = TextPrimary, fontSize = 12.sp, lineHeight = 16.sp, fontFamily = ElmsSans, fontWeight = FontWeight.SemiBold)
                    ColumnSelectorRow(allCols, tableData.headers, roomCol) { roomCol = it }
                }

                // Instructor Selector
                Column {
                    Text("Instructor Column (Optional):", color = TextPrimary, fontSize = 12.sp, lineHeight = 16.sp, fontFamily = ElmsSans, fontWeight = FontWeight.SemiBold)
                    ColumnSelectorRow(allCols, tableData.headers, instructorCol) { instructorCol = it }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Preview Grid
                Text("Data Preview (top 3 rows):", color = TextMuted, fontSize = 11.sp, lineHeight = 14.sp, fontFamily = ElmsSans)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(HomeNearBlack)
                        .padding(8.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (row in tableData.rows.take(3)) {
                            Text(
                                text = row.joinToString(" | "),
                                color = Color(0xFFCBD5E1),
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                fontFamily = ElmsSans,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        },
        containerColor = HomeCardSurface,
        confirmButton = {
            Button(
                onClick = {
                    val finalMapping = ScheduleColumnMapping(
                        dayColumnIndex = dayCol,
                        timeColumnIndex = timeCol,
                        subjectColumnIndex = subjectCol,
                        roomColumnIndex = roomCol,
                        instructorColumnIndex = instructorCol
                    )
                    onConfirm(finalMapping)
                },
                enabled = dayCol >= 0 && timeCol >= 0 && subjectCol >= 0,
                colors = ButtonDefaults.buttonColors(containerColor = HomeMainButtonBlue)
            ) {
                Text("Import Now", fontFamily = ElmsSans)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextPrimary, fontFamily = ElmsSans)
            }
        }
    )
}

@Composable
private fun ColumnSelectorRow(
    allCols: List<Int>,
    headers: List<String>,
    selectedCol: Int,
    onSelectCol: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (col in allCols) {
            val isSel = selectedCol == col
            val headerName = headers.getOrNull(col)?.ifBlank { "Column $col" } ?: "Column $col"
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSel) HomeMainButtonBlue else HomeCardBorder)
                    .clickable { onSelectCol(col) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = headerName,
                    color = TextPrimary,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontFamily = ElmsSans,
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
