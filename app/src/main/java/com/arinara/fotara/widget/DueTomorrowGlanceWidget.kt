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
import java.util.Date
import java.util.Locale

data class WidgetDueNote(
    val id: Long,
    val title: String,
    val folderName: String,
    val deadlineMs: Long
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
        val notes = queryDueNotes(context)
        val isPreAndroid15 = Build.VERSION.SDK_INT < 35

        provideContent {
            GlanceTheme {
                if (isPreAndroid15) {
                    // Fallback to simpler static rendering for pre-Android 15
                    StaticFallbackLayout(context, notes)
                } else {
                    // Full interactive Glance experience
                    InteractiveGlanceLayout(context, notes)
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun InteractiveGlanceLayout(context: Context, notes: List<WidgetDueNote>) {
        val bgPrimary = ColorProvider(day = androidx.compose.ui.graphics.Color(0xFF03071E), night = androidx.compose.ui.graphics.Color(0xFF03071E))
        val textPrimary = ColorProvider(day = androidx.compose.ui.graphics.Color(0xFFEAE3D2), night = androidx.compose.ui.graphics.Color(0xFFEAE3D2))
        val textSecondary = ColorProvider(day = androidx.compose.ui.graphics.Color(0xFF9E9E9E), night = androidx.compose.ui.graphics.Color(0xFF9E9E9E))
        val accentGold = ColorProvider(day = androidx.compose.ui.graphics.Color(0xFFF77F00), night = androidx.compose.ui.graphics.Color(0xFFF77F00))
        val cardBg = ColorProvider(day = androidx.compose.ui.graphics.Color(0xFF141936), night = androidx.compose.ui.graphics.Color(0xFF141936))

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(bgPrimary)
                .padding(12.dp)
                .cornerRadius(16.dp)
        ) {
            // Header Row with Interactive Refresh Action
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Fotara Due",
                    style = TextStyle(
                        color = accentGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )

                // Direct interactive refresh button without opening app
                Text(
                    text = "↻ Refresh",
                    style = TextStyle(color = textSecondary, fontSize = 11.sp),
                    modifier = GlanceModifier
                        .clickable(actionRunCallback<RefreshDueWidgetCallback>())
                        .padding(4.dp)
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            if (notes.isEmpty()) {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No notes due tomorrow",
                        style = TextStyle(color = textSecondary, fontSize = 12.sp)
                    )
                }
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(notes) { note ->
                        val deepLinkIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra("EXTRA_NOTE_ID", note.id)
                        }

                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .background(cardBg)
                                .cornerRadius(8.dp)
                                .padding(8.dp)
                                .clickable(actionStartActivity(android.content.ComponentName(context, MainActivity::class.java))),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Text(
                                    text = note.title,
                                    style = TextStyle(color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                                    maxLines = 1
                                )
                                Text(
                                    text = note.folderName,
                                    style = TextStyle(color = textSecondary, fontSize = 10.sp),
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = formatTime(note.deadlineMs),
                                style = TextStyle(color = accentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun StaticFallbackLayout(context: Context, notes: List<WidgetDueNote>) {
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
                text = "Due Tomorrow",
                style = TextStyle(color = accentGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = if (notes.isNotEmpty()) "${notes.size} Notes Due" else "All caught up",
                style = TextStyle(color = textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = "Tap to open Fotara",
                style = TextStyle(color = textSecondary, fontSize = 10.sp)
            )
        }
    }

    private fun formatTime(timeMs: Long): String {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        return sdf.format(Date(timeMs))
    }

    private fun queryDueNotes(context: Context): List<WidgetDueNote> {
        val list = mutableListOf<WidgetDueNote>()
        try {
            val dbHelper = FotaraDbHelper(context)
            val db = dbHelper.getSafeReadableDatabase()
            val now = System.currentTimeMillis()
            val windowEnd = now + 48 * 60 * 60 * 1000L

            val cursor = db.rawQuery(
                """
                SELECT p.id, p.caption, f.name, p.linked_deadline
                FROM photos p
                INNER JOIN folders f ON p.folder_id = f.id
                WHERE p.is_trashed = 0 AND f.is_trashed = 0
                  AND p.linked_deadline IS NOT NULL
                  AND p.linked_deadline >= ? AND p.linked_deadline <= ?
                ORDER BY p.linked_deadline ASC
                LIMIT 10
                """.trimIndent(),
                arrayOf(now.toString(), windowEnd.toString())
            )
            cursor.use { c ->
                while (c.moveToNext()) {
                    list.add(
                        WidgetDueNote(
                            id = c.getLong(0),
                            title = if (c.isNull(1) || c.getString(1).isBlank()) "Study Note" else c.getString(1),
                            folderName = c.getString(2),
                            deadlineMs = c.getLong(3)
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        return list
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
