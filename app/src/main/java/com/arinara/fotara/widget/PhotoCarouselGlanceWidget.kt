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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
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
import androidx.glance.layout.fillMaxHeight
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
import com.arinara.fotara.data.db.FotaraDbHelper

data class CarouselPhotoItem(
    val id: Long,
    val fileUri: String,
    val caption: String?,
    val folderName: String,
    val folderId: Long
)

data class CarouselWidgetData(
    val items: List<CarouselPhotoItem>,
    val currentIndex: Int,
    val activeBitmap: Bitmap?
)

class PhotoCarouselGlanceWidget : GlanceAppWidget() {

    companion object {
        const val PREFS_CAROUSEL = "fotara_carousel_prefs"
        const val KEY_CAROUSEL_INDEX_PREFIX = "carousel_idx_"
        val PARAM_WIDGET_ID = ActionParameters.Key<Int>("app_widget_id")

        val SIZE_4X2 = DpSize(220.dp, 110.dp)
        val SIZE_4X3 = DpSize(220.dp, 160.dp)

        val ColorBgBase = ColorProvider(day = Color(0xFF0A0D14), night = Color(0xFF0A0D14)) // HomeNearBlack
        val ColorCardSurface = ColorProvider(day = Color(0xFF111726), night = Color(0xFF111726)) // Base +4%
        val ColorScrimSurface = ColorProvider(day = Color(0xCC111726), night = Color(0xCC111726))
        val ColorTextPrimary = ColorProvider(day = Color(0xFFEFE8DA), night = Color(0xFFEFE8DA)) // FolderTabCream
        val ColorTextSecondary = ColorProvider(day = Color(0xFFA0A5C2), night = Color(0xFFA0A5C2)) // TextSecondary
        val ColorTextMuted = ColorProvider(day = Color(0xFF6F7491), night = Color(0xFF6F7491)) // TextMuted
        val ColorPrimary = ColorProvider(day = Color(0xFF2563EB), night = Color(0xFF2563EB)) // Primary
        val ColorTagAmber = ColorProvider(day = Color(0xFFF4A261), night = Color(0xFFF4A261)) // TagAmber
    }

    override val sizeMode = SizeMode.Responsive(setOf(SIZE_4X2, SIZE_4X3))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val data = queryCarouselData(context, appWidgetId)

        provideContent {
            GlanceTheme {
                CarouselLayout(context, data, appWidgetId)
            }
        }
    }

    @Composable
    private fun CarouselLayout(context: Context, data: CarouselWidgetData, appWidgetId: Int) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorBgBase)
                .padding(8.dp)
                .cornerRadius(16.dp)
        ) {
            // Header Bar: Title, Count, Navigation Controls
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    val countStr = if (data.items.isNotEmpty()) " (${data.currentIndex + 1}/${data.items.size})" else ""
                    Text(
                        text = "Fotara Coursework$countStr",
                        style = TextStyle(
                            color = ColorTagAmber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                    val activeItem = data.items.getOrNull(data.currentIndex)
                    val detail = activeItem?.let {
                        val cap = it.caption?.ifBlank { null }
                        if (cap != null) "${it.folderName} • $cap" else it.folderName
                    } ?: "Catatan kuliah & rumus"

                    Text(
                        text = detail,
                        style = TextStyle(color = ColorTextSecondary, fontSize = 10.sp),
                        maxLines = 1
                    )
                }

                if (data.items.size > 1) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Previous Button (<)
                        Text(
                            text = " ◀ ",
                            style = TextStyle(color = ColorTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                            modifier = GlanceModifier
                                .background(ColorCardSurface)
                                .cornerRadius(8.dp)
                                .clickable(actionRunCallback<CarouselPrevCallback>(actionParametersOf(PARAM_WIDGET_ID to appWidgetId)))
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )

                        Spacer(modifier = GlanceModifier.width(4.dp))

                        // Next Button (>)
                        Text(
                            text = " ▶ ",
                            style = TextStyle(color = ColorTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                            modifier = GlanceModifier
                                .background(ColorCardSurface)
                                .cornerRadius(8.dp)
                                .clickable(actionRunCallback<CarouselNextCallback>(actionParametersOf(PARAM_WIDGET_ID to appWidgetId)))
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            // Body Area: Image or Empty State
            if (data.items.isEmpty()) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorCardSurface)
                        .cornerRadius(12.dp)
                        .clickable(createOpenAppAction(context)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada foto catatan kuliah",
                        style = TextStyle(color = ColorTextSecondary, fontSize = 11.sp)
                    )
                }
            } else {
                val activeItem = data.items[data.currentIndex]
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorCardSurface)
                        .cornerRadius(12.dp)
                        .clickable(createOpenPhotoAction(context, activeItem))
                ) {
                    if (data.activeBitmap != null) {
                        Image(
                            provider = ImageProvider(data.activeBitmap),
                            contentDescription = activeItem.caption ?: "Coursework Photo",
                            contentScale = ContentScale.Crop,
                            modifier = GlanceModifier
                                .fillMaxSize()
                                .cornerRadius(12.dp)
                        )
                    } else {
                        Box(
                            modifier = GlanceModifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = activeItem.folderName,
                                style = TextStyle(color = ColorTextSecondary, fontSize = 12.sp)
                            )
                        }
                    }
                }
            }
        }
    }

    private fun createOpenAppAction(context: Context) = actionStartActivity(
        ComponentName(context, MainActivity::class.java)
    )

    private fun createOpenPhotoAction(context: Context, item: CarouselPhotoItem) = actionStartActivity(
        ComponentName(context, MainActivity::class.java)
    )

    fun queryCarouselData(context: Context, appWidgetId: Int): CarouselWidgetData {
        val items = mutableListOf<CarouselPhotoItem>()

        try {
            val dbHelper = FotaraDbHelper(context)
            val db = dbHelper.getSafeReadableDatabase()
            val cursor = db.rawQuery(
                """
                SELECT p.id, p.file_uri, p.caption, f.name, p.folder_id
                FROM photos p
                INNER JOIN folders f ON p.folder_id = f.id
                WHERE p.is_trashed = 0 AND f.is_trashed = 0
                ORDER BY p.added_at DESC
                LIMIT 10
                """.trimIndent(),
                null
            )
            cursor.use { c ->
                while (c.moveToNext()) {
                    items.add(
                        CarouselPhotoItem(
                            id = c.getLong(0),
                            fileUri = c.getString(1),
                            caption = if (c.isNull(2)) null else c.getString(2),
                            folderName = c.getString(3),
                            folderId = c.getLong(4)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("PhotoCarouselGlanceWidget", "Failed to query carousel photos", e)
        }

        val prefs = context.getSharedPreferences(PREFS_CAROUSEL, Context.MODE_PRIVATE)
        val rawIndex = prefs.getInt("${KEY_CAROUSEL_INDEX_PREFIX}$appWidgetId", 0)
        val currentIndex = if (items.isNotEmpty()) rawIndex.coerceIn(0, items.size - 1) else 0

        val activeBitmap = if (items.isNotEmpty()) {
            val activeItem = items[currentIndex]
            WidgetBitmapUtils.loadDownsampledBitmap(
                context = context,
                fileUriOrPath = activeItem.fileUri,
                maxSize = WidgetBitmapUtils.MAX_WIDGET_BITMAP_SIZE
            )
        } else null

        return CarouselWidgetData(
            items = items,
            currentIndex = currentIndex,
            activeBitmap = activeBitmap
        )
    }
}

class CarouselNextCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val appWidgetId = parameters[PhotoCarouselGlanceWidget.PARAM_WIDGET_ID]
            ?: GlanceAppWidgetManager(context).getAppWidgetId(glanceId)

        val prefs = context.getSharedPreferences(PhotoCarouselGlanceWidget.PREFS_CAROUSEL, Context.MODE_PRIVATE)
        val currentIndex = prefs.getInt("${PhotoCarouselGlanceWidget.KEY_CAROUSEL_INDEX_PREFIX}$appWidgetId", 0)

        // Query count
        val count = getPhotoCount(context)
        if (count > 0) {
            val nextIndex = (currentIndex + 1) % count
            prefs.edit().putInt("${PhotoCarouselGlanceWidget.KEY_CAROUSEL_INDEX_PREFIX}$appWidgetId", nextIndex).apply()
        }

        PhotoCarouselGlanceWidget().update(context, glanceId)
    }

    private fun getPhotoCount(context: Context): Int {
        return try {
            val db = FotaraDbHelper(context).getSafeReadableDatabase()
            val cursor = db.rawQuery(
                "SELECT COUNT(*) FROM photos p INNER JOIN folders f ON p.folder_id = f.id WHERE p.is_trashed = 0 AND f.is_trashed = 0",
                null
            )
            cursor.use { if (it.moveToNext()) it.getInt(0).coerceAtMost(10) else 0 }
        } catch (e: Exception) {
            Log.e("PhotoCarouselGlanceWidget", "Failed to query photo count in next callback", e)
            0
        }
    }
}

class CarouselPrevCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val appWidgetId = parameters[PhotoCarouselGlanceWidget.PARAM_WIDGET_ID]
            ?: GlanceAppWidgetManager(context).getAppWidgetId(glanceId)

        val prefs = context.getSharedPreferences(PhotoCarouselGlanceWidget.PREFS_CAROUSEL, Context.MODE_PRIVATE)
        val currentIndex = prefs.getInt("${PhotoCarouselGlanceWidget.KEY_CAROUSEL_INDEX_PREFIX}$appWidgetId", 0)

        val count = getPhotoCount(context)
        if (count > 0) {
            val prevIndex = (currentIndex - 1 + count) % count
            prefs.edit().putInt("${PhotoCarouselGlanceWidget.KEY_CAROUSEL_INDEX_PREFIX}$appWidgetId", prevIndex).apply()
        }

        PhotoCarouselGlanceWidget().update(context, glanceId)
    }

    private fun getPhotoCount(context: Context): Int {
        return try {
            val db = FotaraDbHelper(context).getSafeReadableDatabase()
            val cursor = db.rawQuery(
                "SELECT COUNT(*) FROM photos p INNER JOIN folders f ON p.folder_id = f.id WHERE p.is_trashed = 0 AND f.is_trashed = 0",
                null
            )
            cursor.use { if (it.moveToNext()) it.getInt(0).coerceAtMost(10) else 0 }
        } catch (e: Exception) {
            Log.e("PhotoCarouselGlanceWidget", "Failed to query photo count in prev callback", e)
            0
        }
    }
}

class PhotoCarouselGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PhotoCarouselGlanceWidget()

    companion object {
        fun notifyDataChanged(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, PhotoCarouselGlanceWidgetReceiver::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, PhotoCarouselGlanceWidgetReceiver::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}
