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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
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
                        Toast.makeText(context, "Tidak ada data tabel ditemukan di berkas tersebut", Toast.LENGTH_SHORT).show()
                    } else {
                        val mapping = parser.autoDetectMapping(tableData)
                        detectedMapping = mapping
                        pendingImportTableData = tableData
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal membaca berkas: ${e.message}", Toast.LENGTH_LONG).show()
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
                        text = "Jadwal Kuliah",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        lineHeight = 28.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Atur jadwal mingguan & persiapan besok",
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
                            contentDescription = "Tambah Kelas",
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
                            contentDescription = "Tutup",
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
                            text = "Batas Rollover Persiapan Besok: Pukul $cutoffTimeStr",
                            color = Color(0xFFE2E8F0),
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = "Ubah",
                        color = HomeMainButtonBlue,
                        fontSize = 12.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Day Selector Pill Bar (Senin .. Minggu)
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
                    val shortName = ClassSchedule.getIndonesianDayShortName(day)

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
                            text = "Tidak ada kelas di hari ${ClassSchedule.getIndonesianDayName(selectedDayOfWeek)}",
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Ketuk (+) untuk membuat atau Impor dari berkas Excel/Word.",
                            color = HomeSubtitleGray,
                            fontSize = 12.sp,
                            fontFamily = ElmsSans
                        )
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

            // Bottom Actions Bar (Import Timetable)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
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
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HomeCardSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HomeCardBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileUpload,
                        contentDescription = null,
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Impor Jadwal (.xlsx / .docx)",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Medium
                    )
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
                    text = "Hapus Jadwal",
                    color = TextPrimary,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = "Hapus kelas \"${toDelete.subjectName}\" dari jadwal mingguan?",
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
                    Text("Hapus", fontFamily = ElmsSans)
                }
            },
            dismissButton = {
                TextButton(onClick = { scheduleToDelete = null }) {
                    Text("Batal", color = TextPrimary, fontFamily = ElmsSans)
                }
            }
        )
    }

    // Cutoff Time Dialog
    if (isEditingCutoff) {
        CutoffTimeDialog(
            currentCutoff = cutoffTimeStr,
            onDismiss = { isEditingCutoff = false },
            onConfirm = { newTime ->
                onUpdateCutoffTime(newTime)
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
                    Toast.makeText(context, "Berhasil mengimpor ${parsedSchedules.size} jadwal kelas", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Tidak ada data kelas yang dapat diimpor", Toast.LENGTH_SHORT).show()
                }
                pendingImportTableData = null
            }
        )
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
                        contentDescription = "Hapus",
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
                text = if (initial == null) "Tambah Kelas" else "Edit Kelas",
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
                    label = { Text("Mata Kuliah / Pelajaran", fontFamily = ElmsSans) },
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
                                text = ClassSchedule.getIndonesianDayShortName(d),
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
                        label = { Text("Mulai (08:00)", fontFamily = ElmsSans) },
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
                        label = { Text("Selesai (09:40)", fontFamily = ElmsSans) },
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
                    label = { Text("Ruangan (opsional)", fontFamily = ElmsSans) },
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
                    label = { Text("Dosen / Pengajar (opsional)", fontFamily = ElmsSans) },
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
                Text("Simpan", fontFamily = ElmsSans)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = TextPrimary, fontFamily = ElmsSans)
            }
        }
    )
}

@Composable
private fun CutoffTimeDialog(
    currentCutoff: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val presets = listOf("17:00", "18:00", "19:00", "20:00", "21:00")
    var selectedTime by remember { mutableStateOf(currentCutoff) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Batas Rollover Persiapan",
                color = TextPrimary,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Setelah jam ini, kapsul jadwal akan otomatis beralih menampilkan kelas besok untuk persiapan tas dan buku malam hari.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontFamily = ElmsSans
                )
                Spacer(modifier = Modifier.height(4.dp))
                for (p in presets) {
                    val isSel = selectedTime == p
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) HomeMainButtonBlue else HomeCardBorder)
                            .clickable { selectedTime = p }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Pukul $p",
                            color = TextPrimary,
                            fontFamily = ElmsSans,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        containerColor = HomeCardSurface,
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedTime) },
                colors = ButtonDefaults.buttonColors(containerColor = HomeMainButtonBlue)
            ) {
                Text("Terapkan", fontFamily = ElmsSans)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = TextPrimary, fontFamily = ElmsSans)
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
                text = "Pemetaan Kolom Jadwal",
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
                    text = "Konfirmasi kolom tabel yang sesuai dengan jadwal kuliah Anda:",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontFamily = ElmsSans
                )

                // Day Selector
                Column {
                    Text("Kolom Hari:", color = TextPrimary, fontSize = 12.sp, lineHeight = 16.sp, fontFamily = ElmsSans, fontWeight = FontWeight.SemiBold)
                    ColumnSelectorRow(allCols, tableData.headers, dayCol) { dayCol = it }
                }

                // Time Selector
                Column {
                    Text("Kolom Jam / Waktu:", color = TextPrimary, fontSize = 12.sp, lineHeight = 16.sp, fontFamily = ElmsSans, fontWeight = FontWeight.SemiBold)
                    ColumnSelectorRow(allCols, tableData.headers, timeCol) { timeCol = it }
                }

                // Subject Selector
                Column {
                    Text("Kolom Mata Kuliah:", color = TextPrimary, fontSize = 12.sp, lineHeight = 16.sp, fontFamily = ElmsSans, fontWeight = FontWeight.SemiBold)
                    ColumnSelectorRow(allCols, tableData.headers, subjectCol) { subjectCol = it }
                }

                // Room Selector
                Column {
                    Text("Kolom Ruang (Opsional):", color = TextPrimary, fontSize = 12.sp, lineHeight = 16.sp, fontFamily = ElmsSans, fontWeight = FontWeight.SemiBold)
                    ColumnSelectorRow(allCols, tableData.headers, roomCol) { roomCol = it }
                }

                // Instructor Selector
                Column {
                    Text("Kolom Dosen (Opsional):", color = TextPrimary, fontSize = 12.sp, lineHeight = 16.sp, fontFamily = ElmsSans, fontWeight = FontWeight.SemiBold)
                    ColumnSelectorRow(allCols, tableData.headers, instructorCol) { instructorCol = it }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Preview Grid
                Text("Pratinjau Data (3 baris teratas):", color = TextMuted, fontSize = 11.sp, lineHeight = 14.sp, fontFamily = ElmsSans)
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
                Text("Impor Sekarang", fontFamily = ElmsSans)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = TextPrimary, fontFamily = ElmsSans)
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
            val headerName = headers.getOrNull(col)?.ifBlank { "Kolom $col" } ?: "Kolom $col"
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
