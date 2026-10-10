// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.arinara.fotara.data.db.FotaraDbHelper
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.theme.FolderTabCream
import com.arinara.fotara.theme.HomeCardBorder
import com.arinara.fotara.theme.HomeCardSurface
import com.arinara.fotara.theme.HomeNearBlack
import com.arinara.fotara.theme.TextMuted
import com.arinara.fotara.theme.TextPrimary
import com.arinara.fotara.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class SelectablePhotoItem(
    val id: Long,
    val fileUri: String,
    val caption: String?,
    val folderName: String?
)

class PhotoPinConfigActivity : ComponentActivity() {

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = HomeNearBlack
            ) {
                PhotoPinConfigScreen(
                    onPhotoSelected = { photoId ->
                        saveSelectionAndFinish(photoId)
                    },
                    onDismiss = {
                        finish()
                    }
                )
            }
        }
    }

    private fun saveSelectionAndFinish(selectedPhotoId: Long) {
        val prefs = getSharedPreferences(PhotoPinGlanceWidget.PREFS_PHOTO_PIN, Context.MODE_PRIVATE)
        prefs.edit().apply {
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                putLong("${PhotoPinGlanceWidget.KEY_PINNED_PHOTO_ID_PREFIX}$appWidgetId", selectedPhotoId)
            }
            putLong(PhotoPinGlanceWidget.KEY_PINNED_PHOTO_ID_DEFAULT, selectedPhotoId)
            apply()
        }

        PhotoPinGlanceWidgetReceiver.notifyDataChanged(this)

        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(Activity.RESULT_OK, resultValue)
        finish()
    }
}

@Composable
fun PhotoPinConfigScreen(
    onPhotoSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val photoList = remember { mutableStateListOf<SelectablePhotoItem>() }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val list = mutableListOf<SelectablePhotoItem>()
            try {
                val dbHelper = FotaraDbHelper(context)
                val db = dbHelper.getSafeReadableDatabase()
                val cursor = db.rawQuery(
                    """
                    SELECT p.id, p.file_path, p.caption, f.name
                    FROM photos p
                    LEFT JOIN folders f ON p.folder_id = f.id
                    WHERE p.is_trashed = 0
                    ORDER BY p.added_at DESC
                    LIMIT 60
                    """.trimIndent(),
                    null
                )
                cursor.use { c ->
                    while (c.moveToNext()) {
                        list.add(
                            SelectablePhotoItem(
                                id = c.getLong(0),
                                fileUri = c.getString(1),
                                caption = if (c.isNull(2)) null else c.getString(2),
                                folderName = if (c.isNull(3)) null else c.getString(3)
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PhotoPinConfigActivity", "Failed to query recent photos", e)
            }

            withContext(Dispatchers.Main) {
                photoList.clear()
                photoList.addAll(list)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Pin Study Photo",
            color = TextPrimary,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Select a formula or study photo to pin to your Home Screen",
            color = TextSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontFamily = ElmsSans
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (photoList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(HomeCardSurface, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No study photos available. Capture or import photos in Fotara first.",
                    color = TextMuted,
                    fontSize = 14.sp,
                    fontFamily = ElmsSans
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(photoList, key = { it.id }) { photo ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = HomeCardSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, HomeCardBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clickable { onPhotoSelected(photo.id) }
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = photo.fileUri,
                                contentDescription = photo.caption,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                            )

                            val badgeText = photo.caption?.ifBlank { null } ?: photo.folderName
                            if (badgeText != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .background(Color(0xCC111726))
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = badgeText,
                                        color = FolderTabCream,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontFamily = ElmsSans
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
