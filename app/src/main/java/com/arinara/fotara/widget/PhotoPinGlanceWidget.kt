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
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.arinara.fotara.MainActivity
import com.arinara.fotara.data.db.FotaraDbHelper

data class PinnedPhotoData(
    val photoId: Long,
    val fileUri: String,
    val caption: String?,
    val folderName: String?,
    val folderId: Long,
    val bitmap: Bitmap?
)

class PhotoPinGlanceWidget : GlanceAppWidget() {

    companion object {
        const val PREFS_PHOTO_PIN = "fotara_photo_pin_prefs"
        const val KEY_PINNED_PHOTO_ID_PREFIX = "pin_photo_id_"
        const val KEY_PINNED_PHOTO_ID_DEFAULT = "pin_photo_id_default"

        val ColorBgBase = ColorProvider(day = Color(0xFF0A0D14), night = Color(0xFF0A0D14)) // HomeNearBlack
        val ColorCardSurface = ColorProvider(day = Color(0xFF111726), night = Color(0xFF111726)) // Base +4%
        val ColorScrimSurface = ColorProvider(day = Color(0xCC111726), night = Color(0xCC111726)) // Translucent Card
        val ColorTextPrimary = ColorProvider(day = Color(0xFFEFE8DA), night = Color(0xFFEFE8DA)) // FolderTabCream
        val ColorTextSecondary = ColorProvider(day = Color(0xFFA0A5C2), night = Color(0xFFA0A5C2)) // TextSecondary
        val ColorTextMuted = ColorProvider(day = Color(0xFF6F7491), night = Color(0xFF6F7491)) // TextMuted
        val ColorPrimary = ColorProvider(day = Color(0xFF2563EB), night = Color(0xFF2563EB)) // Primary
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val data = queryPinnedPhoto(context, appWidgetId)

        provideContent {
            GlanceTheme {
                PinnedPhotoLayout(context, data, appWidgetId)
            }
        }
    }

    @Composable
    private fun PinnedPhotoLayout(context: Context, data: PinnedPhotoData?, appWidgetId: Int) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorBgBase)
                .cornerRadius(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (data?.bitmap != null) {
                // Photo Card with Click to View
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .clickable(createOpenPhotoAction(context, data))
                ) {
                    // Render strictly downsampled bitmap
                    Image(
                        provider = ImageProvider(data.bitmap),
                        contentDescription = data.caption ?: "Pinned Coursework Photo",
                        contentScale = ContentScale.Crop,
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .cornerRadius(16.dp)
                    )

                    // Bottom info scrim if folder or caption is present
                    val infoText = data.caption?.ifBlank { null } ?: data.folderName?.ifBlank { null }
                    if (infoText != null) {
                        Column(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .background(ColorScrimSurface)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .cornerRadius(12.dp)
                        ) {
                            Text(
                                text = infoText,
                                style = TextStyle(
                                    color = ColorTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }
            } else {
                // Empty state: Tap to choose coursework photo
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .background(ColorCardSurface)
                        .cornerRadius(16.dp)
                        .clickable(createConfigureAction(context, appWidgetId)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Fotara • Pin Photo",
                        style = TextStyle(
                            color = ColorPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    Text(
                        text = "Tap to pin a study photo or diagram",
                        style = TextStyle(
                            color = ColorTextSecondary,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }

    private fun createOpenPhotoAction(context: Context, data: PinnedPhotoData) = actionStartActivity(
        ComponentName(context, MainActivity::class.java)
    )

    private fun createConfigureAction(context: Context, appWidgetId: Int) = actionStartActivity(
        ComponentName(context, PhotoPinConfigActivity::class.java)
    )

    fun queryPinnedPhoto(context: Context, appWidgetId: Int): PinnedPhotoData? {
        val prefs = context.getSharedPreferences(PREFS_PHOTO_PIN, Context.MODE_PRIVATE)
        val targetId = prefs.getLong("${KEY_PINNED_PHOTO_ID_PREFIX}$appWidgetId", -1L)
            .let { if (it > 0) it else prefs.getLong(KEY_PINNED_PHOTO_ID_DEFAULT, -1L) }

        if (targetId <= 0) return null

        try {
            val dbHelper = FotaraDbHelper(context)
            val db = dbHelper.getSafeReadableDatabase()
            val cursor = db.rawQuery(
                """
                SELECT p.id, p.file_path, p.caption, f.name, p.folder_id
                FROM photos p
                LEFT JOIN folders f ON p.folder_id = f.id
                WHERE p.id = ? AND p.is_trashed = 0
                """.trimIndent(),
                arrayOf(targetId.toString())
            )

            cursor.use { c ->
                if (c.moveToNext()) {
                    val id = c.getLong(0)
                    val uri = c.getString(1)
                    val caption = if (c.isNull(2)) null else c.getString(2)
                    val folderName = if (c.isNull(3)) null else c.getString(3)
                    val folderId = c.getLong(4)

                    // Strictly downsample to max 512x512 to prevent TransactionTooLargeException
                    val downsampled = WidgetBitmapUtils.loadDownsampledBitmap(
                        context = context,
                        fileUriOrPath = uri,
                        maxSize = WidgetBitmapUtils.MAX_WIDGET_BITMAP_SIZE
                    )

                    return PinnedPhotoData(
                        photoId = id,
                        fileUri = uri,
                        caption = caption,
                        folderName = folderName,
                        folderId = folderId,
                        bitmap = downsampled
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("PhotoPinGlanceWidget", "Failed to query pinned photo", e)
        }

        return null
    }
}

class PhotoPinGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PhotoPinGlanceWidget()

    companion object {
        fun notifyDataChanged(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, PhotoPinGlanceWidgetReceiver::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, PhotoPinGlanceWidgetReceiver::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}
