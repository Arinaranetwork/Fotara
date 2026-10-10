// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.arinara.fotara.MainActivity
import com.arinara.fotara.data.db.FotaraDbHelper
import com.arinara.fotara.data.repository.SettingsRepository
import com.arinara.fotara.feature.schedule.engine.ScheduleCutoffEngine
import com.arinara.fotara.feature.schedule.model.ClassSchedule
import java.util.Calendar

data class TimetableWidgetItem(
    val id: Long,
    val dayOfWeek: Int,
    val startMinute: Int,
    val endMinute: Int,
    val subjectName: String,
    val roomName: String?,
    val instructorName: String?,
    val linkedFolderId: Long?,
    val colorHex: String?
) {
    val startTimeFormatted: String get() = ClassSchedule.formatMinutesToTime(startMinute)
    val endTimeFormatted: String get() = ClassSchedule.formatMinutesToTime(endMinute)
    val timeRangeFormatted: String get() = "$startTimeFormatted - $endTimeFormatted"
}

data class TimetableWidgetData(
    val headerTitle: String,
    val subheader: String,
    val dayName: String,
    val isRollover: Boolean,
    val classes: List<TimetableWidgetItem>
)

class TimetableGlanceWidget : GlanceAppWidget() {

    companion object {
        val SIZE_4X2 = DpSize(200.dp, 100.dp)
        val SIZE_4X4 = DpSize(200.dp, 200.dp)

        // Design tokens (Android.md compliant)
        val ColorBgBase = ColorProvider(day = Color(0xFF0A0D14), night = Color(0xFF0A0D14)) // HomeNearBlack
        val ColorCardSurface = ColorProvider(day = Color(0xFF111726), night = Color(0xFF111726)) // Base +4%
        val ColorTextPrimary = ColorProvider(day = Color(0xFFEFE8DA), night = Color(0xFFEFE8DA)) // FolderTabCream
        val ColorTextSecondary = ColorProvider(day = Color(0xFFA0A5C2), night = Color(0xFFA0A5C2)) // TextSecondary
        val ColorTextMuted = ColorProvider(day = Color(0xFF6F7491), night = Color(0xFF6F7491)) // TextMuted
        val ColorTagAmber = ColorProvider(day = Color(0xFFF4A261), night = Color(0xFFF4A261)) // TagAmber
        val ColorPrimaryBlue = ColorProvider(day = Color(0xFF2563EB), night = Color(0xFF2563EB)) // Primary
    }

    override val sizeMode = SizeMode.Responsive(setOf(SIZE_4X2, SIZE_4X4))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = queryTimetableData(context)

        provideContent {
            GlanceTheme {
                val size = LocalSize.current
                if (size.height < 150.dp) {
                    CompactLayout4x2(context, data)
                } else {
                    ExpandedLayout4x4(context, data)
                }
            }
        }
    }

    @Composable
    private fun CompactLayout4x2(context: Context, data: TimetableWidgetData) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorBgBase)
                .padding(12.dp)
                .cornerRadius(16.dp)
        ) {
            // Header Row
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = data.headerTitle,
                        style = TextStyle(
                            color = if (data.isRollover) ColorTagAmber else ColorPrimaryBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                    Text(
                        text = data.subheader,
                        style = TextStyle(color = ColorTextSecondary, fontSize = 11.sp),
                        maxLines = 1
                    )
                }

                Text(
                    text = "Refresh",
                    style = TextStyle(color = ColorTextMuted, fontSize = 11.sp),
                    modifier = GlanceModifier
                        .clickable(actionRunCallback<RefreshTimetableCallback>())
                        .padding(4.dp)
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            if (data.classes.isEmpty()) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorCardSurface)
                        .cornerRadius(12.dp)
                        .clickable(createOpenScheduleAction(context)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (data.isRollover) "No classes scheduled tomorrow" else "No classes scheduled today",
                        style = TextStyle(color = ColorTextSecondary, fontSize = 12.sp)
                    )
                }
            } else {
                // Show up to 2 class items in compact 4x2 view
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    data.classes.take(2).forEachIndexed { idx, item ->
                        if (idx > 0) Spacer(modifier = GlanceModifier.height(4.dp))
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .background(ColorCardSurface)
                                .cornerRadius(8.dp)
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .clickable(createOpenClassAction(context, item)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.startTimeFormatted,
                                style = TextStyle(
                                    color = ColorTagAmber,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = GlanceModifier.width(8.dp))
                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Text(
                                    text = item.subjectName,
                                    style = TextStyle(color = ColorTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                                    maxLines = 1
                                )
                                val roomInfo = item.roomName?.ifBlank { null } ?: "Classroom"
                                Text(
                                    text = roomInfo,
                                    style = TextStyle(color = ColorTextSecondary, fontSize = 10.sp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ExpandedLayout4x4(context: Context, data: TimetableWidgetData) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorBgBase)
                .padding(12.dp)
                .cornerRadius(16.dp)
        ) {
            // Header Row
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = data.headerTitle,
                        style = TextStyle(
                            color = if (data.isRollover) ColorTagAmber else ColorPrimaryBlue,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                    Text(
                        text = data.subheader,
                        style = TextStyle(color = ColorTextSecondary, fontSize = 11.sp),
                        maxLines = 1
                    )
                }

                Text(
                    text = "Refresh",
                    style = TextStyle(color = ColorTextMuted, fontSize = 11.sp),
                    modifier = GlanceModifier
                        .clickable(actionRunCallback<RefreshTimetableCallback>())
                        .padding(4.dp)
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            if (data.classes.isEmpty()) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorCardSurface)
                        .cornerRadius(12.dp)
                        .clickable(createOpenScheduleAction(context)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (data.isRollover) "No classes scheduled tomorrow" else "No classes remaining today",
                            style = TextStyle(color = ColorTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        )
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            text = "Tap to open full schedule",
                            style = TextStyle(color = ColorTextSecondary, fontSize = 10.sp)
                        )
                    }
                }
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(data.classes) { item ->
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(ColorCardSurface)
                                .cornerRadius(12.dp)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .clickable(createOpenClassAction(context, item)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = item.startTimeFormatted,
                                    style = TextStyle(color = ColorTagAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = item.endTimeFormatted,
                                    style = TextStyle(color = ColorTextMuted, fontSize = 10.sp)
                                )
                            }

                            Spacer(modifier = GlanceModifier.width(12.dp))

                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Text(
                                    text = item.subjectName,
                                    style = TextStyle(color = ColorTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                    maxLines = 1
                                )
                                val room = item.roomName?.ifBlank { null }
                                val lecturer = item.instructorName?.ifBlank { null }
                                val detailStr = when {
                                    room != null && lecturer != null -> "$room • $lecturer"
                                    room != null -> room
                                    lecturer != null -> lecturer
                                    else -> "Regular Class"
                                }
                                Text(
                                    text = detailStr,
                                    style = TextStyle(color = ColorTextSecondary, fontSize = 11.sp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun createOpenScheduleAction(context: Context) = actionStartActivity(
        ComponentName(context, MainActivity::class.java)
    )

    private fun createOpenClassAction(context: Context, item: TimetableWidgetItem) = actionStartActivity(
        ComponentName(context, MainActivity::class.java)
    )

    fun queryTimetableData(context: Context): TimetableWidgetData {
        val allSchedules = mutableListOf<ClassSchedule>()

        try {
            val dbHelper = FotaraDbHelper(context)
            val db = dbHelper.getSafeReadableDatabase()
            val cursor = db.rawQuery(
                """
                SELECT id, day_of_week, start_minute, end_minute, subject_name, room_name, instructor_name, linked_folder_id, color_hex
                FROM class_schedules
                ORDER BY day_of_week ASC, start_minute ASC
                """.trimIndent(),
                null
            )
            cursor.use { c ->
                while (c.moveToNext()) {
                    allSchedules.add(
                        ClassSchedule(
                            id = c.getLong(0),
                            dayOfWeek = c.getInt(1),
                            startMinute = c.getInt(2),
                            endMinute = c.getInt(3),
                            subjectName = c.getString(4),
                            roomName = if (c.isNull(5)) null else c.getString(5),
                            instructorName = if (c.isNull(6)) null else c.getString(6),
                            linkedFolderId = if (c.isNull(7)) null else c.getLong(7),
                            colorHex = if (c.isNull(8)) null else c.getString(8)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("TimetableGlanceWidget", "Failed to query timetable data", e)
        }

        val prefs = context.getSharedPreferences("fotara_settings", Context.MODE_PRIVATE)
        val cutoffStr = prefs.getString(SettingsRepository.KEY_SCHEDULE_ROLLOVER_TIME, "18:00") ?: "18:00"
        val cutoffMinute = ClassSchedule.parseTimeToMinutes(cutoffStr) ?: (18 * 60)

        val currentDay = ScheduleCutoffEngine.getCurrentDayOfWeek()
        val currentMinute = ScheduleCutoffEngine.getCurrentMinuteOfDay()
        val isRollover = currentMinute >= cutoffMinute

        val targetDayOfWeek: Int
        val targetDayName: String
        val headerTitle: String
        val targetClasses: List<ClassSchedule>

        if (isRollover) {
            // Find next academic day
            var nextDay = if (currentDay == 7) 1 else currentDay + 1
            var found = allSchedules.filter { it.dayOfWeek == nextDay }.sortedBy { it.startMinute }
            if (found.isEmpty()) {
                // Search subsequent days (e.g. skip weekend to Monday)
                for (offset in 2..7) {
                    var candidate = (currentDay + offset)
                    if (candidate > 7) candidate -= 7
                    val candidateClasses = allSchedules.filter { it.dayOfWeek == candidate }.sortedBy { it.startMinute }
                    if (candidateClasses.isNotEmpty()) {
                        nextDay = candidate
                        found = candidateClasses
                        break
                    }
                }
            }

            targetDayOfWeek = nextDay
            val isNextDayTomorrow = nextDay == (if (currentDay == 7) 1 else currentDay + 1)
            targetDayName = if (isNextDayTomorrow) "Tomorrow" else ClassSchedule.getEnglishDayName(nextDay)
            headerTitle = if (isNextDayTomorrow) "Fotara • Next-Day Prep" else "Fotara • Prep for $targetDayName"
            targetClasses = found
        } else {
            targetDayOfWeek = currentDay
            targetDayName = "Today"
            headerTitle = "Fotara • Today (${ClassSchedule.getEnglishDayShortName(currentDay)})"
            targetClasses = allSchedules.filter { it.dayOfWeek == currentDay }.sortedBy { it.startMinute }
        }

        var finalTargetClasses = targetClasses
        var finalDayName = targetDayName
        var finalHeaderTitle = headerTitle
        var finalSubheader: String
        var finalIsRollover = isRollover

        if (!isRollover && targetClasses.isEmpty() && allSchedules.isNotEmpty()) {
            // Find next upcoming class day
            var nextDay = if (currentDay == 7) 1 else currentDay + 1
            var found = allSchedules.filter { it.dayOfWeek == nextDay }.sortedBy { it.startMinute }
            if (found.isEmpty()) {
                for (offset in 2..7) {
                    var candidate = (currentDay + offset)
                    if (candidate > 7) candidate -= 7
                    val candidateClasses = allSchedules.filter { it.dayOfWeek == candidate }.sortedBy { it.startMinute }
                    if (candidateClasses.isNotEmpty()) {
                        nextDay = candidate
                        found = candidateClasses
                        break
                    }
                }
            }

            if (found.isNotEmpty()) {
                finalTargetClasses = found
                val isNextTomorrow = nextDay == (if (currentDay == 7) 1 else currentDay + 1)
                val dayLabel = if (isNextTomorrow) "Tomorrow" else ClassSchedule.getEnglishDayName(nextDay)
                val firstSubject = found.first().subjectName
                finalDayName = dayLabel
                finalHeaderTitle = "Fotara • Next: $dayLabel"
                finalSubheader = "Next: $dayLabel ($firstSubject)"
                finalIsRollover = true
            } else {
                finalSubheader = "No classes scheduled"
            }
        } else {
            val ongoing = finalTargetClasses.firstOrNull { it.isActiveAt(currentMinute) }
            finalSubheader = when {
                isRollover -> "${finalTargetClasses.size} Scheduled ${if (finalTargetClasses.size == 1) "Class" else "Classes"}"
                ongoing != null -> "Now: ${ongoing.subjectName} until ${ongoing.endTimeFormatted}"
                finalTargetClasses.isNotEmpty() -> "${finalTargetClasses.size} Scheduled ${if (finalTargetClasses.size == 1) "Class" else "Classes"}"
                else -> "No classes scheduled"
            }
        }

        val mappedItems = finalTargetClasses.map {
            TimetableWidgetItem(
                id = it.id,
                dayOfWeek = it.dayOfWeek,
                startMinute = it.startMinute,
                endMinute = it.endMinute,
                subjectName = it.subjectName,
                roomName = it.roomName,
                instructorName = it.instructorName,
                linkedFolderId = it.linkedFolderId,
                colorHex = it.colorHex
            )
        }

        return TimetableWidgetData(
            headerTitle = finalHeaderTitle,
            subheader = finalSubheader,
            dayName = finalDayName,
            isRollover = finalIsRollover,
            classes = mappedItems
        )
    }
}

class RefreshTimetableCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        TimetableGlanceWidget().update(context, glanceId)
    }
}

class TimetableGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TimetableGlanceWidget()

    companion object {
        fun triggerUpdate(context: Context) {
            notifyDataChanged(context)
        }

        fun notifyDataChanged(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, TimetableGlanceWidgetReceiver::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, TimetableGlanceWidgetReceiver::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}
