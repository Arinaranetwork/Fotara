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
                SELECT id, note_type, folder_name, color_label, caption, due_time FROM (
                    SELECT p.id, 'photo' AS note_type, f.name AS folder_name, f.color_label,
                           COALESCE(p.caption, (CASE WHEN p.ocr_text IS NOT NULL THEN substr(p.ocr_text, 1, 40) ELSE 'Photo Note' END)) AS caption,
                           COALESCE(p.scheduled_at, p.linked_deadline) AS due_time
                    FROM photos p
                    INNER JOIN folders f ON p.folder_id = f.id
                    WHERE p.is_trashed = 0 AND f.is_trashed = 0
                      AND ((p.scheduled_at IS NOT NULL AND p.scheduled_at >= ? AND p.scheduled_at <= ?)
                        OR (p.linked_deadline IS NOT NULL AND p.linked_deadline >= ? AND p.linked_deadline <= ?))
                    UNION ALL
                    SELECT d.id, 'document' AS note_type, f.name AS folder_name, f.color_label,
                           d.name AS caption,
                           COALESCE(d.scheduled_at, d.linked_deadline) AS due_time
                    FROM document_notes d
                    INNER JOIN folders f ON d.folder_id = f.id
                    WHERE d.is_trashed = 0 AND f.is_trashed = 0
                      AND ((d.scheduled_at IS NOT NULL AND d.scheduled_at >= ? AND d.scheduled_at <= ?)
                        OR (d.linked_deadline IS NOT NULL AND d.linked_deadline >= ? AND d.linked_deadline <= ?))
                    UNION ALL
                    SELECT t.id, 'text' AS note_type, f.name AS folder_name, f.color_label,
                           t.title AS caption,
                           COALESCE(t.scheduled_at, t.linked_deadline) AS due_time
                    FROM text_notes t
                    INNER JOIN folders f ON t.folder_id = f.id
                    WHERE t.is_trashed = 0 AND f.is_trashed = 0
                      AND ((t.scheduled_at IS NOT NULL AND t.scheduled_at >= ? AND t.scheduled_at <= ?)
                        OR (t.linked_deadline IS NOT NULL AND t.linked_deadline >= ? AND t.linked_deadline <= ?))
                )
                ORDER BY due_time ASC
                LIMIT 25
                """.trimIndent(),
                arrayOf(
                    now.toString(), windowEnd.toString(), now.toString(), windowEnd.toString(),
                    now.toString(), windowEnd.toString(), now.toString(), windowEnd.toString(),
                    now.toString(), windowEnd.toString(), now.toString(), windowEnd.toString()
                )
            )

            cursor.use { c ->
                while (c.moveToNext()) {
                    val id = c.getLong(0)
                    val noteType = c.getString(1)
                    val folderName = c.getString(2) ?: "Coursework"
                    val colorHex = if (c.isNull(3)) null else c.getString(3)
                    val caption = if (c.isNull(4) || c.getString(4).isBlank()) {
                        when (noteType) {
                            "document" -> "Document"
                            "text" -> "Text Note"
                            else -> "Photo Note"
                        }
                    } else {
                        c.getString(4)
                    }
                    val deadlineMs = c.getLong(5)

                    items.add(
                        WidgetNoteItem(
                            photoId = id,
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
