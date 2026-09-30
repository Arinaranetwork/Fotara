// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.widget

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
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
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.arinara.fotara.MainActivity
import com.arinara.fotara.R
import com.arinara.fotara.data.db.FotaraDbHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class WidgetTodayItem(
    val id: Long,
    val title: String,
    val folderName: String,
    val timestamp: Long,
    val isScheduled: Boolean
)

data class WidgetTodayData(
    val addedTodayCount: Int,
    val scheduledCount: Int,
    val items: List<WidgetTodayItem>
)

class DueTomorrowGlanceWidget : GlanceAppWidget() {

    companion object {
        private val SMALL_BOX = DpSize(100.dp, 100.dp)
        private val MEDIUM_ROW = DpSize(200.dp, 120.dp)
        private val LARGE_LIST = DpSize(260.dp, 200.dp)
    }

    override val sizeMode = SizeMode.Responsive(
        setOf(SMALL_BOX, MEDIUM_ROW, LARGE_LIST)
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val todayData = queryTodayData(context)
        val isPreAndroid15 = Build.VERSION.SDK_INT < 35

        provideContent {
            GlanceTheme {
                if (isPreAndroid15) {
                    StaticFallbackLayout(context, todayData)
                } else {
                    InteractiveGlanceLayout(context, todayData)
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun InteractiveGlanceLayout(context: Context, data: WidgetTodayData) {
        val bgPrimary = ColorProvider(day = androidx.compose.ui.graphics.Color(0xFF03071E), night = androidx.compose.ui.graphics.Color(0xFF03071E))
        val textPrimary = ColorProvider(day = androidx.compose.ui.graphics.Color(0xFFEAE3D2), night = androidx.compose.ui.graphics.Color(0xFFEAE3D2))
        val textSecondary = ColorProvider(day = androidx.compose.ui.graphics.Color(0xFF9E9E9E), night = androidx.compose.ui.graphics.Color(0xFF9E9E9E))
        val accentGold = ColorProvider(day = androidx.compose.ui.graphics.Color(0xFFF77F00), night = androidx.compose.ui.graphics.Color(0xFFF77F00))
        val cardBg = ColorProvider(day = androidx.compose.ui.graphics.Color(0xFF141936), night = androidx.compose.ui.graphics.Color(0xFF141936))

        val dateHeaderStr = rememberDateHeader()

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(bgPrimary)
                .padding(12.dp)
                .cornerRadius(16.dp)
        ) {
            // Header Row: App name, date, interactive refresh
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = "Fotara • Today",
                        style = TextStyle(
                            color = accentGold,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = dateHeaderStr,
                        style = TextStyle(
                            color = textSecondary,
                            fontSize = 10.sp
                        )
                    )
                }

                Text(
                    text = "Refresh",
                    style = TextStyle(color = textSecondary, fontSize = 11.sp),
                    modifier = GlanceModifier
                        .clickable(actionRunCallback<RefreshDueWidgetCallback>())
                        .padding(4.dp)
                )
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            // Summary counts row
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Added: ${data.addedTodayCount} · Scheduled: ${data.scheduledCount}",
                    style = TextStyle(color = textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium),
                    modifier = GlanceModifier.defaultWeight()
                )
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            if (data.items.isEmpty()) {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No notes scheduled or added today",
                        style = TextStyle(color = textSecondary, fontSize = 12.sp)
                    )
                }
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(data.items) { item ->
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .background(cardBg)
                                .cornerRadius(8.dp)
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .clickable(actionStartActivity(android.content.ComponentName(context, MainActivity::class.java))),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Text(
                                    text = item.title,
                                    style = TextStyle(color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                                    maxLines = 1
                                )
                                Text(
                                    text = "${item.folderName} • ${if (item.isScheduled) "Scheduled" else "Added"}",
                                    style = TextStyle(color = textSecondary, fontSize = 10.sp),
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = formatTime(item.timestamp),
                                style = TextStyle(
                                    color = if (item.isScheduled) accentGold else textPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun StaticFallbackLayout(context: Context, data: WidgetTodayData) {
        val bgPrimary = ColorProvider(day = androidx.compose.ui.graphics.Color(0xFF03071E), night = androidx.compose.ui.graphics.Color(0xFF03071E))
        val textPrimary = ColorProvider(day = androidx.compose.ui.graphics.Color(0xFFEAE3D2), night = androidx.compose.ui.graphics.Color(0xFFEAE3D2))
        val textSecondary = ColorProvider(day = androidx.compose.ui.graphics.Color(0xFF9E9E9E), night = androidx.compose.ui.graphics.Color(0xFF9E9E9E))
        val accentGold = ColorProvider(day = androidx.compose.ui.graphics.Color(0xFFF77F00), night = androidx.compose.ui.graphics.Color(0xFFF77F00))

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(bgPrimary)
                .padding(12.dp)
                .cornerRadius(16.dp)
                .clickable(actionStartActivity(android.content.ComponentName(context, MainActivity::class.java))),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Fotara • Today",
                style = TextStyle(color = accentGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = "${data.addedTodayCount} Added · ${data.scheduledCount} Scheduled",
                style = TextStyle(color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = "Tap to open Fotara",
                style = TextStyle(color = textSecondary, fontSize = 10.sp)
            )
        }
    }

    private fun rememberDateHeader(): String {
        val sdf = SimpleDateFormat("EEEE, MMM d", Locale.US)
        return sdf.format(Date())
    }

    private fun formatTime(timeMs: Long): String {
        val sdf = SimpleDateFormat("h:mm a", Locale.US)
        return sdf.format(Date(timeMs))
    }

    private fun queryTodayData(context: Context): WidgetTodayData {
        val items = mutableListOf<WidgetTodayItem>()
        var addedCount = 0
        var scheduledCount = 0

        try {
            val dbHelper = FotaraDbHelper(context)
            val db = dbHelper.getSafeReadableDatabase()

            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = cal.timeInMillis
            val endOfDay = startOfDay + 24 * 60 * 60 * 1000L

            // 1. Photos added today or scheduled today
            val photoCursor = db.rawQuery(
                """
                SELECT p.id, p.caption, f.name, p.added_at, p.scheduled_at, p.linked_deadline
                FROM photos p
                INNER JOIN folders f ON p.folder_id = f.id
                WHERE p.is_trashed = 0 AND f.is_trashed = 0
                  AND ((p.added_at >= ? AND p.added_at < ?)
                    OR (p.scheduled_at IS NOT NULL AND p.scheduled_at >= ? AND p.scheduled_at < ?)
                    OR (p.linked_deadline IS NOT NULL AND p.linked_deadline >= ? AND p.linked_deadline < ?))
                ORDER BY p.added_at DESC
                LIMIT 15
                """.trimIndent(),
                arrayOf(
                    startOfDay.toString(), endOfDay.toString(),
                    startOfDay.toString(), endOfDay.toString(),
                    startOfDay.toString(), endOfDay.toString()
                )
            )
            photoCursor.use { c ->
                while (c.moveToNext()) {
                    val id = c.getLong(0)
                    val caption = if (c.isNull(1) || c.getString(1).isBlank()) "Photo Note" else c.getString(1)
                    val folderName = c.getString(2)
                    val addedAt = c.getLong(3)
                    val scheduledAt = if (c.isNull(4)) null else c.getLong(4)
                    val deadline = if (c.isNull(5)) null else c.getLong(5)

                    if (addedAt in startOfDay..<endOfDay) addedCount++
                    val activeSchedule = scheduledAt ?: deadline
                    if (activeSchedule != null && activeSchedule in startOfDay..<endOfDay) scheduledCount++

                    items.add(
                        WidgetTodayItem(
                            id = id,
                            title = caption,
                            folderName = folderName,
                            timestamp = activeSchedule ?: addedAt,
                            isScheduled = activeSchedule != null
                        )
                    )
                }
            }

            // 2. Text notes added or scheduled today
            val textCursor = db.rawQuery(
                """
                SELECT t.id, t.title, f.name, t.added_at, t.scheduled_at, t.linked_deadline
                FROM text_notes t
                INNER JOIN folders f ON t.folder_id = f.id
                WHERE t.is_trashed = 0 AND f.is_trashed = 0
                  AND ((t.added_at >= ? AND t.added_at < ?)
                    OR (t.scheduled_at IS NOT NULL AND t.scheduled_at >= ? AND t.scheduled_at < ?)
                    OR (t.linked_deadline IS NOT NULL AND t.linked_deadline >= ? AND t.linked_deadline < ?))
                ORDER BY t.added_at DESC
                LIMIT 10
                """.trimIndent(),
                arrayOf(
                    startOfDay.toString(), endOfDay.toString(),
                    startOfDay.toString(), endOfDay.toString(),
                    startOfDay.toString(), endOfDay.toString()
                )
            )
            textCursor.use { tc ->
                while (tc.moveToNext()) {
                    val id = tc.getLong(0)
                    val title = if (tc.isNull(1) || tc.getString(1).isBlank()) "Text Note" else tc.getString(1)
                    val folderName = tc.getString(2)
                    val addedAt = tc.getLong(3)
                    val scheduledAt = if (tc.isNull(4)) null else tc.getLong(4)
                    val deadline = if (tc.isNull(5)) null else tc.getLong(5)

                    if (addedAt in startOfDay..<endOfDay) addedCount++
                    val activeSchedule = scheduledAt ?: deadline
                    if (activeSchedule != null && activeSchedule in startOfDay..<endOfDay) scheduledCount++

                    items.add(
                        WidgetTodayItem(
                            id = id,
                            title = title,
                            folderName = folderName,
                            timestamp = activeSchedule ?: addedAt,
                            isScheduled = activeSchedule != null
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        return WidgetTodayData(
            addedTodayCount = addedCount,
            scheduledCount = scheduledCount,
            items = items.sortedByDescending { it.timestamp }.take(15)
        )
    }
}

class RefreshDueWidgetCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        DueTomorrowGlanceWidget().update(context, glanceId)
    }
}

class DueTomorrowGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DueTomorrowGlanceWidget()
}
