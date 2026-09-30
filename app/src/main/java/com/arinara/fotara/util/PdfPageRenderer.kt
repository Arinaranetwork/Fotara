// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.util

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File
import java.io.IOException

class PdfPasswordException(message: String = "This PDF is password-protected and requires a password to open.") : SecurityException(message)
class PdfCorruptException(message: String = "The PDF file is corrupt or cannot be read.") : IOException(message)

class PdfPageRenderer(val file: File) : Closeable {

    private var pfd: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null
    private val renderMutex = Mutex()

    // Bounded memory cache: 32MB max memory capacity
    private val maxCacheSize = 32 * 1024 * 1024 // 32 MB
    private val pageBitmapCache = object : LruCache<String, Bitmap>(maxCacheSize) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount
        }

        override fun entryRemoved(evicted: Boolean, key: String, oldValue: Bitmap, newValue: Bitmap?) {
            // Do not call oldValue.recycle() here: active Jetpack Compose Image composables
            // may still be drawing evicted frames during rapid list flings.
        }
    }

    val pageCount: Int

    init {
        if (!file.exists() || file.length() == 0L) {
            throw PdfCorruptException("File does not exist or is empty: ${file.absolutePath}")
        }
        try {
            val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            pfd = descriptor
            val r = PdfRenderer(descriptor)
            renderer = r
            pageCount = r.pageCount
        } catch (e: SecurityException) {
            close()
            throw PdfPasswordException("Password required to read: ${file.name}")
        } catch (e: Exception) {
            close()
            throw PdfCorruptException("Failed to initialize PdfRenderer: ${e.message}")
        }
    }

    suspend fun getPageAspectRatio(pageIndex: Int): Float = withContext(Dispatchers.IO) {
        if (pageIndex < 0 || pageIndex >= pageCount) return@withContext 0.707f
        renderMutex.withLock {
            val r = renderer ?: return@withContext 0.707f
            try {
                val page = r.openPage(pageIndex)
                val ratio = if (page.height > 0) page.width.toFloat() / page.height.toFloat() else 0.707f
                page.close()
                ratio
            } catch (_: Exception) {
                0.707f
            }
        }
    }

    suspend fun renderPage(
        pageIndex: Int,
        destWidth: Int,
        destHeight: Int,
        renderScale: Float = 1.0f
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (pageIndex < 0 || pageIndex >= pageCount) return@withContext null

        // Cap dimensions to avoid OutOfMemory on huge displays or extreme zoom
        val targetWidth = ((destWidth * renderScale).toInt().coerceIn(100, 2560))
        val targetHeight = ((destHeight * renderScale).toInt().coerceIn(100, 2560))
        val cacheKey = "${pageIndex}_${targetWidth}x${targetHeight}"

        // Check bounded cache first
        pageBitmapCache.get(cacheKey)?.let { cached ->
            if (!cached.isRecycled) return@withContext cached
        }

        renderMutex.withLock {
            val r = renderer ?: return@withContext null
            var page: PdfRenderer.Page? = null
            try {
                page = r.openPage(pageIndex)

                val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)

                // CRITICAL DEFECT REPAIR: Pre-fill canvas with opaque white
                // PdfRenderer draws vector/text over transparent pixels by default; without white fill,
                // transparent backgrounds render as black or glitchy artifacts.
                bitmap.eraseColor(Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                pageBitmapCache.put(cacheKey, bitmap)
                bitmap
            } catch (e: Exception) {
                android.util.Log.e("PdfPageRenderer", "Error rendering page $pageIndex: ${e.message}")
                null
            } finally {
                try {
                    page?.close()
                } catch (_: Exception) {}
            }
        }
    }

    fun clearCache() {
        pageBitmapCache.evictAll()
    }

    override fun close() {
        pageBitmapCache.evictAll()
        try {
            renderer?.close()
        } catch (_: Exception) {}
        try {
            pfd?.close()
        } catch (_: Exception) {}
        renderer = null
        pfd = null
    }
}
