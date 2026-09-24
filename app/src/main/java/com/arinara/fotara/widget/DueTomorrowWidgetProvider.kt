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
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.arinara.fotara.MainActivity
import com.arinara.fotara.R
import com.arinara.fotara.data.db.FotaraDbHelper

class DueTomorrowWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    @Suppress("DEPRECATION")
    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_due_tomorrow)

        // Query active due note count
        val count = getDueNotesCount(context)

        if (count == 0) {
            views.setViewVisibility(R.id.widget_empty_state, View.VISIBLE)
            views.setViewVisibility(R.id.widget_list, View.GONE)
            views.setTextViewText(R.id.widget_header_count, "")
        } else {
            views.setViewVisibility(R.id.widget_empty_state, View.GONE)
            views.setViewVisibility(R.id.widget_list, View.VISIBLE)
            views.setTextViewText(R.id.widget_header_count, "$count Due")
        }

        // Connect RemoteViews adapter to list
        val serviceIntent = Intent(context, DueTomorrowWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }
        views.setRemoteAdapter(R.id.widget_list, serviceIntent)

        // PendingIntent template for note item clicks (direct deep-link into note)
        val itemClickIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val itemClickPendingIntent = PendingIntent.getActivity(
            context,
            100,
            itemClickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        views.setPendingIntentTemplate(R.id.widget_list, itemClickPendingIntent)

        // Header and empty-card click to open app
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            101,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_header, openAppPendingIntent)
        views.setOnClickPendingIntent(R.id.widget_empty_state, openAppPendingIntent)

        appWidgetManager.updateAppWidget(widgetId, views)
    }

    private fun getDueNotesCount(context: Context): Int {
        return try {
            val dbHelper = FotaraDbHelper(context)
            val db = dbHelper.getSafeReadableDatabase()
            val now = System.currentTimeMillis()
            val windowEnd = now + 48 * 60 * 60 * 1000L

            val cursor = db.rawQuery(
                """
                SELECT COUNT(*)
                FROM photos p
                INNER JOIN folders f ON p.folder_id = f.id
                WHERE p.is_trashed = 0 AND f.is_trashed = 0
                  AND p.linked_deadline IS NOT NULL
                  AND p.linked_deadline >= ? AND p.linked_deadline <= ?
                """.trimIndent(),
                arrayOf(now.toString(), windowEnd.toString())
            )
            cursor.use { c ->
                if (c.moveToNext()) c.getInt(0) else 0
            }
        } catch (_: Exception) {
            0
        }
    }

    companion object {
        @Suppress("DEPRECATION")
        fun notifyDataChanged(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, DueTomorrowWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list)
                val intent = Intent(context, DueTomorrowWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}
