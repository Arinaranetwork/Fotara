// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.arinara.fotara.MainActivity
import com.arinara.fotara.R
import com.arinara.fotara.data.db.FotaraDbHelper
import com.arinara.fotara.data.repository.SettingsRepository
import com.arinara.fotara.feature.schedule.engine.ScheduleCutoffEngine
import com.arinara.fotara.feature.schedule.model.ClassSchedule
import com.arinara.fotara.feature.schedule.model.DisabledDaySchedule
import com.arinara.fotara.feature.schedule.model.ScheduleDayNote
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
    val classes: List<TimetableWidgetItem>,
    val targetDayOfWeek: Int = 1,
    val isDisabled: Boolean = false,
    val disabledStatusLabel: String? = null,
    val dayNoteText: String? = null
)

data class DayScheduleData(
    val dayOfWeek: Int,
    val dayShortName: String,
    val isToday: Boolean,
    val classes: List<TimetableWidgetItem>,
    val isDisabled: Boolean = false,
    val isSelected: Boolean = false
)

data class UnifiedScheduleWidgetState(
    val singleDayData: TimetableWidgetData,
    val multiDayPreview: List<DayScheduleData>,
    val fullWeeklyDays: List<DayScheduleData>
)

/**
 * State query and builder engine for the Fotara Schedule AppWidget.
 */
class TimetableGlanceWidget {

    companion object {
        val SIZE_COMPACT = DpSize(180.dp, 100.dp)
        val SIZE_MEDIUM = DpSize(260.dp, 160.dp)
        val SIZE_FULL = DpSize(340.dp, 200.dp)

        const val PREFS_WIDGET_STATE = "fotara_widget_state"
        private const val KEY_SELECTED_DAY_PREFIX = "selected_day_"

        fun getSelectedDayOverride(context: Context, appWidgetId: Int): Int? {
            if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return null
            val prefs = context.getSharedPreferences(PREFS_WIDGET_STATE, Context.MODE_PRIVATE)
            val saved = prefs.getInt("$KEY_SELECTED_DAY_PREFIX$appWidgetId", -1)
            return if (saved in 1..7) saved else null
        }

        fun setSelectedDayOverride(context: Context, appWidgetId: Int, dayOfWeek: Int?) {
            if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
            val prefs = context.getSharedPreferences(PREFS_WIDGET_STATE, Context.MODE_PRIVATE)
            if (dayOfWeek == null || dayOfWeek !in 1..7) {
                prefs.edit().remove("$KEY_SELECTED_DAY_PREFIX$appWidgetId").apply()
            } else {
                prefs.edit().putInt("$KEY_SELECTED_DAY_PREFIX$appWidgetId", dayOfWeek).apply()
            }
        }
    }

    fun queryUnifiedScheduleState(
        context: Context,
        selectedDayOverride: Int? = null,
        nowMs: Long = System.currentTimeMillis()
    ): UnifiedScheduleWidgetState {
        val allSchedules = mutableListOf<ClassSchedule>()
        val disabledRecords = mutableMapOf<Int, DisabledDaySchedule>()
        val dayNotes = mutableListOf<ScheduleDayNote>()

        try {
            val dbHelper = FotaraDbHelper.getInstance(context.applicationContext ?: context)
            val db = dbHelper.getSafeReadableDatabase()

            db.rawQuery(
                """
                SELECT id, day_of_week, start_minute, end_minute, subject_name, room_name, instructor_name, linked_folder_id, color_hex
                FROM class_schedules
                ORDER BY day_of_week ASC, start_minute ASC
                """.trimIndent(),
                null
            ).use { c ->
                while (c.moveToNext()) {
                    allSchedules.add(
                        ClassSchedule(
                            id = c.getLong(0),
                            dayOfWeek = c.getInt(1),
                            startMinute = c.getInt(2),
                            endMinute = c.getInt(3),
                            subjectName = c.getString(4) ?: "",
                            roomName = if (c.isNull(5)) null else c.getString(5),
                            instructorName = if (c.isNull(6)) null else c.getString(6),
                            linkedFolderId = if (c.isNull(7)) null else c.getLong(7),
                            colorHex = if (c.isNull(8)) null else c.getString(8)
                        )
                    )
                }
            }

            val expiredDays = mutableListOf<Int>()
            db.rawQuery(
                "SELECT day_of_week, disabled_until_ms, mode, updated_at FROM disabled_schedule_days",
                null
            ).use { c ->
                while (c.moveToNext()) {
                    val day = c.getInt(0)
                    val until = c.getLong(1)
                    val mode = c.getString(2) ?: "ONLY_THIS_DAY"
                    val updated = c.getLong(3)
                    if (until > nowMs) {
                        disabledRecords[day] = DisabledDaySchedule(day, until, mode, updated)
                    } else {
                        expiredDays.add(day)
                    }
                }
            }

            if (expiredDays.isNotEmpty()) {
                try {
                    val writeDb = dbHelper.getSafeWritableDatabase()
                    for (day in expiredDays) {
                        writeDb.delete("disabled_schedule_days", "day_of_week = ?", arrayOf(day.toString()))
                    }
                } catch (_: Exception) {}
            }

            db.rawQuery(
                "SELECT id, day_of_week, date_millis, note_text, is_repeating_weekly, created_at, updated_at FROM schedule_day_notes ORDER BY updated_at DESC",
                null
            ).use { c ->
                while (c.moveToNext()) {
                    dayNotes.add(
                        ScheduleDayNote(
                            id = c.getLong(0),
                            dayOfWeek = c.getInt(1),
                            dateMillis = c.getLong(2),
                            noteText = c.getString(3) ?: "",
                            isRepeatingWeekly = c.getInt(4) == 1,
                            createdAt = c.getLong(5),
                            updatedAt = c.getLong(6)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("TimetableGlanceWidget", "Failed to query timetable data", e)
        }

        val disabledDaySet = disabledRecords.keys
        val prefs = context.getSharedPreferences("fotara_settings", Context.MODE_PRIVATE)
        val cutoffStr = prefs.getString(SettingsRepository.KEY_SCHEDULE_ROLLOVER_TIME, "18:00") ?: "18:00"
        val cutoffMinute = ClassSchedule.parseTimeToMinutes(cutoffStr) ?: (18 * 60)

        val currentDay = ScheduleCutoffEngine.getCurrentDayOfWeek()
        val currentMinute = ScheduleCutoffEngine.getCurrentMinuteOfDay()
        val isRollover = selectedDayOverride == null && currentMinute >= cutoffMinute

        val targetDayOfWeek: Int
        val targetDayName: String
        val headerTitle: String
        val targetClasses: List<ClassSchedule>
        val isTargetDayDisabled: Boolean
        val disabledLabel: String?

        if (selectedDayOverride != null && selectedDayOverride in 1..7) {
            targetDayOfWeek = selectedDayOverride
            isTargetDayDisabled = disabledDaySet.contains(targetDayOfWeek)
            disabledLabel = disabledRecords[targetDayOfWeek]?.formatStatusLabel()
            val fullDayName = ClassSchedule.getEnglishDayName(targetDayOfWeek)
            targetDayName = if (targetDayOfWeek == currentDay) "Today ($fullDayName)" else fullDayName
            headerTitle = if (isTargetDayDisabled) "$targetDayName (Off)" else targetDayName
            targetClasses = if (isTargetDayDisabled) {
                emptyList()
            } else {
                allSchedules.filter { it.dayOfWeek == targetDayOfWeek }.sortedBy { it.startMinute }
            }
        } else if (isRollover) {
            var nextDay = if (currentDay == 7) 1 else currentDay + 1
            var found = if (disabledDaySet.contains(nextDay)) {
                emptyList()
            } else {
                allSchedules.filter { it.dayOfWeek == nextDay }.sortedBy { it.startMinute }
            }
            if (found.isEmpty()) {
                for (offset in 2..7) {
                    var candidate = currentDay + offset
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
            isTargetDayDisabled = disabledDaySet.contains(targetDayOfWeek)
            disabledLabel = disabledRecords[targetDayOfWeek]?.formatStatusLabel()
            val isNextDayTomorrow = nextDay == (if (currentDay == 7) 1 else currentDay + 1)
            targetDayName = if (isNextDayTomorrow) "Tomorrow (${ClassSchedule.getEnglishDayShortName(nextDay)})" else ClassSchedule.getEnglishDayName(nextDay)
            headerTitle = if (isNextDayTomorrow) "Next-Day Prep • ${ClassSchedule.getEnglishDayShortName(nextDay)}" else "Prep • $targetDayName"
            targetClasses = found
        } else {
            targetDayOfWeek = currentDay
            isTargetDayDisabled = disabledDaySet.contains(currentDay)
            disabledLabel = disabledRecords[currentDay]?.formatStatusLabel()
            targetDayName = "Today"
            headerTitle = if (isTargetDayDisabled) {
                "Today (${ClassSchedule.getEnglishDayShortName(currentDay)}) (Off)"
            } else {
                "Today (${ClassSchedule.getEnglishDayShortName(currentDay)})"
            }
            targetClasses = if (isTargetDayDisabled) {
                emptyList()
            } else {
                allSchedules.filter { it.dayOfWeek == currentDay }.sortedBy { it.startMinute }
            }
        }

        val targetDateMs = resolveDateMillisForDay(currentDay, targetDayOfWeek, nowMs)
        val activeDayNote = if (isTargetDayDisabled) {
            null
        } else {
            dayNotes.firstOrNull { note ->
                note.dayOfWeek == targetDayOfWeek &&
                    note.noteText.isNotBlank() &&
                    note.isActiveForDate(targetDateMs)
            }?.noteText
        }

        val subheader = when {
            isTargetDayDisabled -> disabledLabel ?: "Schedule disabled for this day"
            targetClasses.isNotEmpty() -> "${targetClasses.size} Scheduled ${if (targetClasses.size == 1) "Class" else "Classes"}"
            else -> "No classes scheduled"
        }

        val singleDayItems = targetClasses.map { it.toWidgetItem() }
        val singleDayData = TimetableWidgetData(
            headerTitle = headerTitle,
            subheader = subheader,
            dayName = targetDayName,
            isRollover = isRollover,
            classes = singleDayItems,
            targetDayOfWeek = targetDayOfWeek,
            isDisabled = isTargetDayDisabled,
            disabledStatusLabel = disabledLabel,
            dayNoteText = activeDayNote
        )

        val fullWeeklyDays = (1..7).map { dayNum ->
            val isDayOff = disabledDaySet.contains(dayNum)
            val dayClasses = if (isDayOff) {
                emptyList()
            } else {
                allSchedules.filter { it.dayOfWeek == dayNum }.map { it.toWidgetItem() }
            }
            DayScheduleData(
                dayOfWeek = dayNum,
                dayShortName = if (isDayOff) "${ClassSchedule.getEnglishDayShortName(dayNum)} (Off)" else ClassSchedule.getEnglishDayShortName(dayNum),
                isToday = (dayNum == currentDay),
                classes = dayClasses,
                isDisabled = isDayOff,
                isSelected = (dayNum == targetDayOfWeek)
            )
        }

        val multiDays = (0..3).map { offset ->
            var d = currentDay + offset
            if (d > 7) d -= 7
            val isDayOff = disabledDaySet.contains(d)
            val dayClasses = if (isDayOff) {
                emptyList()
            } else {
                allSchedules.filter { it.dayOfWeek == d }.map { it.toWidgetItem() }
            }
            DayScheduleData(
                dayOfWeek = d,
                dayShortName = if (isDayOff) "${ClassSchedule.getEnglishDayShortName(d)} (Off)" else ClassSchedule.getEnglishDayShortName(d),
                isToday = (d == currentDay),
                classes = dayClasses,
                isDisabled = isDayOff,
                isSelected = (d == targetDayOfWeek)
            )
        }

        return UnifiedScheduleWidgetState(
            singleDayData = singleDayData,
            multiDayPreview = multiDays,
            fullWeeklyDays = fullWeeklyDays
        )
    }

    private fun resolveDateMillisForDay(currentDay: Int, targetDay: Int, nowMs: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
        val offset = (targetDay - currentDay + 7) % 7
        if (offset > 0) {
            cal.add(Calendar.DAY_OF_YEAR, offset)
        }
        return ScheduleDayNote.normalizeToStartOfDay(cal.timeInMillis)
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

/**
 * RemoteViewsService backing the scrollable ListView inside the Schedule AppWidget.
 */
class TimetableRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        return TimetableRemoteViewsFactory(applicationContext, appWidgetId)
    }
}

/**
 * RemoteViewsFactory supplying class item rows from SQLite for the active widget day.
 */
class TimetableRemoteViewsFactory(
    private val context: Context,
    private val appWidgetId: Int
) : RemoteViewsService.RemoteViewsFactory {

    private var items: List<TimetableWidgetItem> = emptyList()

    override fun onCreate() {
        loadItemsSync()
    }

    override fun onDataSetChanged() {
        val identityToken = Binder.clearCallingIdentity()
        try {
            loadItemsSync()
        } catch (e: Exception) {
            Log.e("TimetableWidgetFactory", "Error in onDataSetChanged", e)
            items = emptyList()
        } finally {
            Binder.restoreCallingIdentity(identityToken)
        }
    }

    private fun loadItemsSync() {
        val selectedDay = TimetableGlanceWidget.getSelectedDayOverride(context, appWidgetId)
        val state = TimetableGlanceWidget().queryUnifiedScheduleState(context, selectedDay)
        items = state.singleDayData.classes
    }

    override fun onDestroy() {
        items = emptyList()
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position !in items.indices) return null
        val item = items[position]

        val rv = RemoteViews(context.packageName, R.layout.widget_timetable_item)
        rv.setTextViewText(R.id.widget_item_time, item.timeRangeFormatted)
        rv.setTextViewText(R.id.widget_item_subject, item.subjectName)

        val details = buildString {
            val room = item.roomName?.trim().orEmpty()
            val instructor = item.instructorName?.trim().orEmpty()
            if (room.isNotEmpty() && instructor.isNotEmpty()) {
                append(room).append(" • ").append(instructor)
            } else if (room.isNotEmpty()) {
                append(room)
            } else if (instructor.isNotEmpty()) {
                append(instructor)
            } else {
                append("Scheduled Class")
            }
        }
        rv.setTextViewText(R.id.widget_item_details, details)

        val fillInIntent = Intent().apply {
            putExtra(TimetableGlanceWidgetReceiver.EXTRA_OPEN_SCHEDULE, true)
            putExtra(TimetableGlanceWidgetReceiver.EXTRA_SCHEDULE_ID, item.id)
            putExtra(TimetableGlanceWidgetReceiver.EXTRA_SCHEDULE_DAY, item.dayOfWeek)
        }
        rv.setOnClickFillInIntent(R.id.widget_item_root, fillInIntent)
        return rv
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long {
        return if (position in items.indices) items[position].id else position.toLong()
    }

    override fun hasStableIds(): Boolean = true
}

/**
 * Native AppWidgetProvider for the Fotara Class Schedule widget.
 * Supports horizontal & vertical resizing, Android 12+ responsive size mapping,
 * 7-day interactive day pills, per-day note banners, disabled day states, and
 * RemoteViewsService list binding.
 */
class TimetableGlanceWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateSingleWidget(context, appWidgetManager, appWidgetId, null)
        }
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list_view)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateSingleWidget(context, appWidgetManager, appWidgetId, newOptions)
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list_view)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        for (id in appWidgetIds) {
            TimetableGlanceWidget.setSelectedDayOverride(context, id, null)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH_WIDGET -> {
                val widgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                )
                if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    // Reset day override to auto (today / rollover) on manual Refresh tap
                    TimetableGlanceWidget.setSelectedDayOverride(context, widgetId, null)
                }
                notifyDataChanged(context)
            }
            ACTION_SELECT_WIDGET_DAY -> {
                val widgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                )
                val day = intent.getIntExtra(EXTRA_SCHEDULE_DAY, -1)
                if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID && day in 1..7) {
                    val currentOverride = TimetableGlanceWidget.getSelectedDayOverride(context, widgetId)
                    if (currentOverride == day) {
                        TimetableGlanceWidget.setSelectedDayOverride(context, widgetId, null)
                    } else {
                        TimetableGlanceWidget.setSelectedDayOverride(context, widgetId, day)
                    }
                    val mgr = AppWidgetManager.getInstance(context)
                    updateSingleWidget(context, mgr, widgetId, null)
                    mgr.notifyAppWidgetViewDataChanged(widgetId, R.id.widget_list_view)
                }
            }
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.arinara.fotara.action.REFRESH_TIMETABLE_WIDGET"
        const val ACTION_SELECT_WIDGET_DAY = "com.arinara.fotara.action.SELECT_WIDGET_DAY"
        const val EXTRA_OPEN_SCHEDULE = "extra_open_schedule"
        const val EXTRA_SCHEDULE_ID = "extra_schedule_id"
        const val EXTRA_SCHEDULE_DAY = "extra_schedule_day"

        private val DAY_PILL_IDS = intArrayOf(
            R.id.widget_day_pill_1,
            R.id.widget_day_pill_2,
            R.id.widget_day_pill_3,
            R.id.widget_day_pill_4,
            R.id.widget_day_pill_5,
            R.id.widget_day_pill_6,
            R.id.widget_day_pill_7
        )

        private val DAY_NAME_IDS = intArrayOf(
            R.id.widget_day_name_1,
            R.id.widget_day_name_2,
            R.id.widget_day_name_3,
            R.id.widget_day_name_4,
            R.id.widget_day_name_5,
            R.id.widget_day_name_6,
            R.id.widget_day_name_7
        )

        private val DAY_COUNT_IDS = intArrayOf(
            R.id.widget_day_count_1,
            R.id.widget_day_count_2,
            R.id.widget_day_count_3,
            R.id.widget_day_count_4,
            R.id.widget_day_count_5,
            R.id.widget_day_count_6,
            R.id.widget_day_count_7
        )

        fun triggerUpdate(context: Context) {
            notifyDataChanged(context)
        }

        fun notifyDataChanged(context: Context) {
            val appContext = context.applicationContext ?: context
            try {
                val appWidgetManager = AppWidgetManager.getInstance(appContext)
                val componentName = ComponentName(appContext, TimetableGlanceWidgetReceiver::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                if (appWidgetIds.isEmpty()) return

                for (appWidgetId in appWidgetIds) {
                    updateSingleWidget(appContext, appWidgetManager, appWidgetId, null)
                }
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list_view)
            } catch (e: Exception) {
                Log.e("TimetableWidget", "Error updating Timetable widget", e)
            }
        }

        fun updateSingleWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            optionsOverride: Bundle? = null
        ) {
            try {
                val options = optionsOverride ?: appWidgetManager.getAppWidgetOptions(appWidgetId)
                val minWidthDp = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
                    ?.takeIf { it > 0 } ?: 250
                val minHeightDp = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 140)
                    ?.takeIf { it > 0 } ?: 140

                val selectedDay = TimetableGlanceWidget.getSelectedDayOverride(context, appWidgetId)
                val state = TimetableGlanceWidget().queryUnifiedScheduleState(context, selectedDay)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val compactRv = buildWidgetRemoteViews(context, appWidgetId, state, widthDp = 160, heightDp = 100)
                    val mediumRv = buildWidgetRemoteViews(context, appWidgetId, state, widthDp = 260, heightDp = 160)
                    val largeRv = buildWidgetRemoteViews(context, appWidgetId, state, widthDp = 350, heightDp = 240)
                    val sizeMap = mapOf(
                        SizeF(140f, 80f) to compactRv,
                        SizeF(230f, 130f) to mediumRv,
                        SizeF(320f, 200f) to largeRv
                    )
                    appWidgetManager.updateAppWidget(appWidgetId, RemoteViews(sizeMap))
                } else {
                    val rv = buildWidgetRemoteViews(context, appWidgetId, state, minWidthDp, minHeightDp)
                    appWidgetManager.updateAppWidget(appWidgetId, rv)
                }
            } catch (e: Exception) {
                Log.e("TimetableWidget", "Failed to update widget $appWidgetId", e)
            }
        }

        internal fun buildWidgetRemoteViews(
            context: Context,
            appWidgetId: Int,
            state: UnifiedScheduleWidgetState,
            widthDp: Int,
            heightDp: Int
        ): RemoteViews {
            val rv = RemoteViews(context.packageName, R.layout.widget_timetable)
            val data = state.singleDayData

            // 1. Header & Subheader
            rv.setTextViewText(R.id.widget_timetable_header, data.headerTitle)
            rv.setTextViewText(R.id.widget_timetable_subheader, data.subheader)

            // Open App PendingIntent on Header click & Empty View click
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_SCHEDULE, true)
                putExtra(EXTRA_SCHEDULE_DAY, data.targetDayOfWeek)
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId * 10 + 1,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            rv.setOnClickPendingIntent(R.id.widget_header_title_box, openAppPendingIntent)
            rv.setOnClickPendingIntent(R.id.widget_empty_view, openAppPendingIntent)

            // Refresh button PendingIntent
            val refreshIntent = Intent(context, TimetableGlanceWidgetReceiver::class.java).apply {
                action = ACTION_REFRESH_WIDGET
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                this.data = Uri.parse("fotara://widget/refresh/$appWidgetId")
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId * 10 + 2,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            rv.setOnClickPendingIntent(R.id.widget_refresh_btn, refreshPendingIntent)

            // 2. Responsive 7-Day Strip (shown when width >= 220dp and height >= 120dp)
            val showWeekStrip = widthDp >= 220 && heightDp >= 120
            rv.setViewVisibility(R.id.widget_week_strip, if (showWeekStrip) View.VISIBLE else View.GONE)

            if (showWeekStrip) {
                state.fullWeeklyDays.forEachIndexed { idx, dayData ->
                    if (idx in 0..6) {
                        val pillId = DAY_PILL_IDS[idx]
                        val nameId = DAY_NAME_IDS[idx]
                        val countId = DAY_COUNT_IDS[idx]

                        val shortLabel = ClassSchedule.getEnglishDayShortName(dayData.dayOfWeek)
                        rv.setTextViewText(nameId, shortLabel)

                        val countText = when {
                            dayData.isDisabled -> "Off"
                            dayData.classes.isEmpty() -> "—"
                            else -> "${dayData.classes.size}"
                        }
                        rv.setTextViewText(countId, countText)

                        if (dayData.isSelected) {
                            rv.setInt(pillId, "setBackgroundResource", R.drawable.widget_day_pill_active_bg)
                            rv.setTextColor(nameId, AndroidColor.parseColor("#F8FAFC"))
                            rv.setTextColor(countId, AndroidColor.parseColor("#F8FAFC"))
                        } else {
                            rv.setInt(pillId, "setBackgroundResource", R.drawable.widget_day_pill_inactive_bg)
                            val nameColor = when {
                                dayData.isDisabled -> AndroidColor.parseColor("#64748B")
                                dayData.isToday -> AndroidColor.parseColor("#7A5AF8")
                                else -> AndroidColor.parseColor("#94A3B8")
                            }
                            rv.setTextColor(nameId, nameColor)
                            rv.setTextColor(countId, AndroidColor.parseColor("#94A3B8"))
                        }

                        val selectDayIntent = Intent(context, TimetableGlanceWidgetReceiver::class.java).apply {
                            action = ACTION_SELECT_WIDGET_DAY
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            putExtra(EXTRA_SCHEDULE_DAY, dayData.dayOfWeek)
                            this.data = Uri.parse("fotara://widget/select_day/$appWidgetId/${dayData.dayOfWeek}")
                        }
                        val selectDayPi = PendingIntent.getBroadcast(
                            context,
                            appWidgetId * 100 + dayData.dayOfWeek,
                            selectDayIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        rv.setOnClickPendingIntent(pillId, selectDayPi)
                    }
                }
            }

            // 3. Per-Day Note Banner (shown when active note exists and height >= 110dp)
            val noteText = data.dayNoteText
            if (!noteText.isNullOrBlank() && heightDp >= 110) {
                rv.setViewVisibility(R.id.widget_day_note_banner, View.VISIBLE)
                rv.setTextViewText(R.id.widget_day_note_text, "📌 Note: $noteText")
                rv.setOnClickPendingIntent(R.id.widget_day_note_banner, openAppPendingIntent)
            } else {
                rv.setViewVisibility(R.id.widget_day_note_banner, View.GONE)
            }

            // 4. Empty State Text
            if (data.isDisabled) {
                rv.setTextViewText(R.id.widget_empty_title, "Day Disabled")
                rv.setTextViewText(
                    R.id.widget_empty_subtitle,
                    "${data.disabledStatusLabel ?: "Schedule disabled"} • Tap to manage"
                )
            } else {
                rv.setTextViewText(R.id.widget_empty_title, "No Classes Scheduled")
                rv.setTextViewText(R.id.widget_empty_subtitle, "Enjoy your free time • Tap to view schedule")
            }

            // 5. RemoteViewsService Adapter & EmptyView Binding
            val serviceIntent = Intent(context, TimetableRemoteViewsService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                this.data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            rv.setRemoteAdapter(R.id.widget_list_view, serviceIntent)
            rv.setEmptyView(R.id.widget_list_view, R.id.widget_empty_view)

            val mutableFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE
            } else {
                0
            }
            val itemClickTemplate = PendingIntent.getActivity(
                context,
                appWidgetId * 10 + 3,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag
            )
            rv.setPendingIntentTemplate(R.id.widget_list_view, itemClickTemplate)

            return rv
        }
    }
}
