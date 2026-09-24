// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.arinara.fotara.R
import com.arinara.fotara.data.db.FotaraDbHelper

class DueTomorrowWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return DueTomorrowRemoteViewsFactory(applicationContext)
    }
}

class DueTomorrowRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    data class WidgetNoteItem(
        val photoId: Long,
        val folderName: String,
        val colorHex: String?,
        val caption: String?,
        val deadlineMs: Long
    )

    private val items = mutableListOf<WidgetNoteItem>()
    private val dbHelper by lazy { FotaraDbHelper(context) }

    override fun onCreate() {}

    override fun onDataSetChanged() {
        items.clear()
        try {
            val db = dbHelper.getSafeReadableDatabase()
            val now = System.currentTimeMillis()
            val twoDaysMs = 48 * 60 * 60 * 1000L
            val windowEnd = now + twoDaysMs

            val cursor = db.rawQuery(
                """
                SELECT p.id, f.name, f.color_label, p.caption, p.ocr_text, p.linked_deadline
                FROM photos p
                INNER JOIN folders f ON p.folder_id = f.id
                WHERE p.is_trashed = 0 AND f.is_trashed = 0
                  AND p.linked_deadline IS NOT NULL
                  AND p.linked_deadline >= ? AND p.linked_deadline <= ?
                ORDER BY p.linked_deadline ASC
                LIMIT 25
                """.trimIndent(),
                arrayOf(now.toString(), windowEnd.toString())
            )

            cursor.use { c ->
                while (c.moveToNext()) {
                    val photoId = c.getLong(0)
                    val folderName = c.getString(1) ?: "Coursework"
                    val colorHex = if (c.isNull(2)) null else c.getString(2)
                    val caption = if (c.isNull(3)) {
                        val ocr = if (c.isNull(4)) null else c.getString(4)
                        ocr?.lineSequence()?.firstOrNull() ?: "Note"
                    } else {
                        c.getString(3)
                    }
                    val deadlineMs = c.getLong(5)

                    items.add(
                        WidgetNoteItem(
                            photoId = photoId,
                            folderName = folderName,
                            colorHex = colorHex,
                            caption = caption,
                            deadlineMs = deadlineMs
                        )
                    )
                }
            }
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        items.clear()
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_due_tomorrow_item)
        if (position >= items.size) return views

        val item = items[position]
        views.setTextViewText(R.id.widget_item_folder, item.folderName)
        views.setTextViewText(R.id.widget_item_caption, item.caption ?: "Assignment note")

        // Parse folder tag color or default to FolderBodyBlue
        val tagColor = try {
            if (!item.colorHex.isNullOrBlank()) Color.parseColor(item.colorHex) else Color.parseColor("#4D88FF")
        } catch (_: Exception) {
            Color.parseColor("#4D88FF")
        }
        views.setInt(R.id.widget_item_color_bar, "setBackgroundColor", tagColor)

        // Fill-in Intent for tapping note item to launch PhotoViewerDialog
        val fillInIntent = Intent().apply {
            putExtra("photo_id", item.photoId)
            putExtra("direct_view_note", true)
        }
        views.setOnClickFillInIntent(R.id.widget_item_container, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = if (position < items.size) items[position].photoId else position.toLong()

    override fun hasStableIds(): Boolean = true
}
