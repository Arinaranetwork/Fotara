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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
import androidx.glance.layout.fillMaxHeight
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

data class DayScheduleData(
    val dayOfWeek: Int,
    val dayShortName: String,
    val isToday: Boolean,
    val classes: List<TimetableWidgetItem>
)

data class UnifiedScheduleWidgetState(
    val singleDayData: TimetableWidgetData,
    val multiDayPreview: List<DayScheduleData>,
    val fullWeeklyDays: List<DayScheduleData>
)

class TimetableGlanceWidget : GlanceAppWidget() {

    companion object {
        val SIZE_COMPACT = DpSize(180.dp, 100.dp)
        val SIZE_MEDIUM = DpSize(260.dp, 160.dp)
        val SIZE_FULL = DpSize(340.dp, 200.dp)

        // Adaptive Light and Dark theme tokens (Android.md compliant)
        val ColorBgBase = ColorProvider(day = Color(0xFFF8FAFC), night = Color(0xFF0A0D14))
        val ColorCardSurface = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF111726))
        val ColorCardSurfaceAlt = ColorProvider(day = Color(0xFFF1F5F9), night = Color(0xFF1A2234))
        val ColorCardBorder = ColorProvider(day = Color(0xFFE2E8F0), night = Color(0xFF1E283D))
        val ColorTextPrimary = ColorProvider(day = Color(0xFF0F172A), night = Color(0xFFEFE8DA))
        val ColorTextSecondary = ColorProvider(day = Color(0xFF64748B), night = Color(0xFFA0A5C2))
        val ColorTextMuted = ColorProvider(day = Color(0xFF94A3B8), night = Color(0xFF6F7491))
        val ColorTagAmber = ColorProvider(day = Color(0xFFD97706), night = Color(0xFFF4A261))
        val ColorPrimaryBlue = ColorProvider(day = Color(0xFF2563EB), night = Color(0xFF3B82F6))
        val ColorActiveIndicator = ColorProvider(day = Color(0xFF16A34A), night = Color(0xFF22C55E))
    }

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = queryUnifiedScheduleState(context)

        provideContent {
            GlanceTheme {
                val size = LocalSize.current
                when {
                    size.width >= 340.dp -> {
                        FullWeeklyTableLayout(context, state)
                    }
                    size.width >= 240.dp -> {
                        MediumMultiDayLayout(context, state)
                    }
                    else -> {
                        CompactSingleDayLayout(context, state.singleDayData)
                    }
                }
            }
        }
    }

    /**
     * Compact (1-day) view for smaller widget slots (<240dp).
     */
    @Composable
    private fun CompactSingleDayLayout(context: Context, data: TimetableWidgetData) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorBgBase)
                .padding(12.dp)
                .cornerRadius(16.dp)
        ) {
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
                        text = "No Classes",
                        style = TextStyle(color = ColorTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    )
                }
            } else {
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    data.classes.take(2).forEachIndexed { idx, item ->
                        if (idx > 0) Spacer(modifier = GlanceModifier.height(4.dp))
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .background(ColorCardSurface)
                                .cornerRadius(8.dp)
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .clickable(createOpenScheduleAction(context)),
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

    /**
     * Medium view (240dp - 340dp width): 3 to 4 days preview.
     */
    @Composable
    private fun MediumMultiDayLayout(context: Context, state: UnifiedScheduleWidgetState) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorBgBase)
                .padding(12.dp)
                .cornerRadius(16.dp)
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = "Class Schedule - ${state.singleDayData.dayName}",
                        style = TextStyle(color = ColorPrimaryBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                    Text(
                        text = state.singleDayData.subheader,
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

            // Multi-day grid columns (3 or 4 days)
            Row(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .clickable(createOpenScheduleAction(context))
            ) {
                state.multiDayPreview.take(3).forEachIndexed { index, day ->
                    if (index > 0) Spacer(modifier = GlanceModifier.width(6.dp))
                    Column(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight()
                            .background(if (day.isToday) ColorCardSurfaceAlt else ColorCardSurface)
                            .cornerRadius(10.dp)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = day.dayShortName,
                            style = TextStyle(
                                color = if (day.isToday) ColorPrimaryBlue else ColorTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(4.dp))

                        if (day.classes.isEmpty()) {
                            Text(
                                text = "Free",
                                style = TextStyle(color = ColorTextMuted, fontSize = 10.sp)
                            )
                        } else {
                            day.classes.take(3).forEach { cls ->
                                Text(
                                    text = "${cls.startTimeFormatted} ${cls.subjectName}",
                                    style = TextStyle(color = ColorTextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Medium),
                                    maxLines = 1,
                                    modifier = GlanceModifier.padding(bottom = 2.dp)
                                )
                            }
                            if (day.classes.size > 3) {
                                Text(
                                    text = "+${day.classes.size - 3} more",
                                    style = TextStyle(color = ColorTagAmber, fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Full width view (>= 340dp width): Complete 7-day weekly schedule table matrix.
     */
    @Composable
    private fun FullWeeklyTableLayout(context: Context, state: UnifiedScheduleWidgetState) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorBgBase)
                .padding(12.dp)
                .cornerRadius(16.dp)
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = "Weekly Schedule",
                        style = TextStyle(color = ColorPrimaryBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                    Text(
                        text = "7-Day Timetable Matrix",
                        style = TextStyle(color = ColorTextSecondary, fontSize = 11.sp),
                        maxLines = 1
                    )
                }

                Text(
                    text = "Open App",
                    style = TextStyle(color = ColorTextMuted, fontSize = 11.sp),
                    modifier = GlanceModifier
                        .clickable(createOpenScheduleAction(context))
                        .padding(4.dp)
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // 7 Days Grid Matrix
            Row(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .clickable(createOpenScheduleAction(context))
            ) {
                state.fullWeeklyDays.forEachIndexed { index, day ->
                    if (index > 0) Spacer(modifier = GlanceModifier.width(4.dp))
                    Column(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight()
                            .background(if (day.isToday) ColorCardSurfaceAlt else ColorCardSurface)
                            .cornerRadius(8.dp)
                            .padding(4.dp)
                    ) {
                        Text(
                            text = day.dayShortName,
                            style = TextStyle(
                                color = if (day.isToday) ColorPrimaryBlue else ColorTextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(4.dp))

                        if (day.classes.isEmpty()) {
                            Text(
                                text = "—",
                                style = TextStyle(color = ColorTextMuted, fontSize = 9.sp)
                            )
                        } else {
                            day.classes.take(2).forEach { cls ->
                                Text(
                                    text = "${cls.startTimeFormatted} ${cls.subjectName}",
                                    style = TextStyle(color = ColorTextPrimary, fontSize = 8.sp),
                                    maxLines = 1,
                                    modifier = GlanceModifier.padding(bottom = 2.dp)
                                )
                            }
                            if (day.classes.size > 2) {
                                Text(
                                    text = "+${day.classes.size - 2}",
                                    style = TextStyle(color = ColorTagAmber, fontSize = 8.sp, fontWeight = FontWeight.Bold),
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

    fun queryUnifiedScheduleState(context: Context): UnifiedScheduleWidgetState {
        val allSchedules = mutableListOf<ClassSchedule>()

        try {
            val dbHelper = FotaraDbHelper.getInstance(context)
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

        val disabledDaySet = mutableSetOf<Int>()
        val nowMs = System.currentTimeMillis()
        try {
            val dbHelper = FotaraDbHelper.getInstance(context)
            val db = dbHelper.getSafeReadableDatabase()
            db.rawQuery("SELECT day_of_week, disabled_until_ms FROM disabled_schedule_days", null).use { c ->
                while (c.moveToNext()) {
                    val day = c.getInt(0)
                    val until = c.getLong(1)
                    if (until > nowMs) {
                        disabledDaySet.add(day)
                    }
                }
            }
        } catch (_: Exception) {}

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
        val isCurrentDayDisabled = disabledDaySet.contains(currentDay)

        if (isRollover) {
            var nextDay = if (currentDay == 7) 1 else currentDay + 1
            var found = if (disabledDaySet.contains(nextDay)) emptyList() else allSchedules.filter { it.dayOfWeek == nextDay }.sortedBy { it.startMinute }
            if (found.isEmpty()) {
                for (offset in 2..7) {
                    var candidate = (currentDay + offset)
                    if (candidate > 7) candidate -= 7
                    if (!disabledDaySet.contains(candidate)) {
                        val candidateClasses = allSchedules.filter { it.dayOfWeek == candidate }.sortedBy { it.startMinute }
                        if (candidateClasses.isNotEmpty()) {
                            nextDay = candidate
                            found = candidateClasses
                            break
                        }
                    }
                }
            }

            targetDayOfWeek = nextDay
            val isNextDayTomorrow = nextDay == (if (currentDay == 7) 1 else currentDay + 1)
            targetDayName = if (isNextDayTomorrow) "Tomorrow" else ClassSchedule.getEnglishDayName(nextDay)
            headerTitle = if (isNextDayTomorrow) "Next-Day Prep" else "Prep for $targetDayName"
            targetClasses = found
        } else {
            targetDayOfWeek = currentDay
            targetDayName = "Today"
            headerTitle = if (isCurrentDayDisabled) "Today (${ClassSchedule.getEnglishDayShortName(currentDay)}) (Off)" else "Today (${ClassSchedule.getEnglishDayShortName(currentDay)})"
            targetClasses = if (isCurrentDayDisabled) emptyList() else allSchedules.filter { it.dayOfWeek == currentDay }.sortedBy { it.startMinute }
        }

        val subheader = when {
            isCurrentDayDisabled && !isRollover -> "Schedule disabled for today"
            isRollover -> "${targetClasses.size} Scheduled ${if (targetClasses.size == 1) "Class" else "Classes"}"
            targetClasses.isNotEmpty() -> "${targetClasses.size} Scheduled ${if (targetClasses.size == 1) "Class" else "Classes"}"
            else -> "No classes scheduled"
        }

        val singleDayItems = targetClasses.map { it.toWidgetItem() }
        val singleDayData = TimetableWidgetData(
            headerTitle = headerTitle,
            subheader = subheader,
            dayName = targetDayName,
            isRollover = isRollover,
            classes = singleDayItems
        )

        // 7-day mapping (1=Monday ... 7=Sunday)
        val fullWeeklyDays = (1..7).map { dayNum ->
            val isDayOff = disabledDaySet.contains(dayNum)
            val dayClasses = if (isDayOff) emptyList() else allSchedules.filter { it.dayOfWeek == dayNum }.map { it.toWidgetItem() }
            DayScheduleData(
                dayOfWeek = dayNum,
                dayShortName = if (isDayOff) "${ClassSchedule.getEnglishDayShortName(dayNum)} (Off)" else ClassSchedule.getEnglishDayShortName(dayNum),
                isToday = (dayNum == currentDay),
                classes = dayClasses
            )
        }

        // Multi-day preview starting from current day
        val multiDays = (0..3).map { offset ->
            var d = currentDay + offset
            if (d > 7) d -= 7
            val isDayOff = disabledDaySet.contains(d)
            val dayClasses = if (isDayOff) emptyList() else allSchedules.filter { it.dayOfWeek == d }.map { it.toWidgetItem() }
            DayScheduleData(
                dayOfWeek = d,
                dayShortName = if (isDayOff) "${ClassSchedule.getEnglishDayShortName(d)} (Off)" else ClassSchedule.getEnglishDayShortName(d),
                isToday = (d == currentDay),
                classes = dayClasses
            )
        }
        return UnifiedScheduleWidgetState(
            singleDayData = singleDayData,
            multiDayPreview = multiDays,
            fullWeeklyDays = fullWeeklyDays
        )
    }

    private fun ClassSchedule.toWidgetItem(): TimetableWidgetItem = TimetableWidgetItem(
        id = id,
        dayOfWeek = dayOfWeek,
        startMinute = startMinute,
        endMinute = endMinute,
        subjectName = subjectName,
        roomName = roomName,
        instructorName = instructorName,
        linkedFolderId = linkedFolderId,
        colorHex = colorHex
    )
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
        private var debounceJob: Job? = null
        private val updateScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        fun triggerUpdate(context: Context) {
            notifyDataChanged(context)
        }

        fun notifyDataChanged(context: Context) {
            val appContext = context.applicationContext
            try {
                val appWidgetManager = AppWidgetManager.getInstance(appContext)
                val componentName = ComponentName(appContext, TimetableGlanceWidgetReceiver::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

                // 1. Guard: If user has no Timetable widget placed on home screen, abort immediately! Zero overhead.
                if (appWidgetIds.isEmpty()) {
                    return
                }

                // 2. Debounce rapid updates to coalesce multiple mutations (e.g., adding classes or batch import) into a single pass
                synchronized(this) {
                    debounceJob?.cancel()
                    debounceJob = updateScope.launch {
                        delay(350L)
                        try {
                            val manager = GlanceAppWidgetManager(appContext)
                            val glanceIds = manager.getGlanceIds(TimetableGlanceWidget::class.java)
                            if (glanceIds.isEmpty()) return@launch

                            val widget = TimetableGlanceWidget()
                            for (glanceId in glanceIds) {
                                widget.update(appContext, glanceId)
                            }
                        } catch (e: Exception) {
                            Log.e("TimetableWidget", "Failed to update glance widgets", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("TimetableWidget", "Error checking widget IDs", e)
            }
        }
    }
}
