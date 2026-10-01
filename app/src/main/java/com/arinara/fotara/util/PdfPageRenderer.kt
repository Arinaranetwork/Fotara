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
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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
            // Do not call oldValue.recycle() directly here: active Compose Image composables
            // may still be drawing frames during fast scrolling. The Android GC handles unreferenced bitmaps safely.
        }
    }

    val pageCount: Int
    private val aspectRatioCache: FloatArray

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
            aspectRatioCache = FloatArray(pageCount) { 0f }
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

        // Fast path: cached aspect ratio without locking renderMutex
        val cached = aspectRatioCache[pageIndex]
        if (cached > 0f) return@withContext cached

        currentCoroutineContext().ensureActive()

        renderMutex.withLock {
            // Double check inside lock
            if (aspectRatioCache[pageIndex] > 0f) return@withContext aspectRatioCache[pageIndex]

            val r = renderer ?: return@withContext 0.707f
            var page: PdfRenderer.Page? = null
            try {
                page = r.openPage(pageIndex)
                val ratio = if (page.height > 0) page.width.toFloat() / page.height.toFloat() else 0.707f
                aspectRatioCache[pageIndex] = ratio
                ratio
            } catch (e: Exception) {
                Log.w("PdfPageRenderer", "Error reading aspect ratio for page $pageIndex: ${e.message}")
                0.707f
            } finally {
                try {
                    page?.close()
                } catch (_: Exception) {}
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

        currentCoroutineContext().ensureActive()

        // Cap dimensions to avoid OutOfMemory on extreme scales or huge displays
        val targetWidth = ((destWidth * renderScale).toInt().coerceIn(100, 2560))
        val targetHeight = ((destHeight * renderScale).toInt().coerceIn(100, 2560))
        val cacheKey = "${pageIndex}_${targetWidth}x${targetHeight}"

        // Fast path: check bounded cache first without lock
        pageBitmapCache.get(cacheKey)?.let { cached ->
            if (!cached.isRecycled) return@withContext cached
        }

        currentCoroutineContext().ensureActive()

        renderMutex.withLock {
            currentCoroutineContext().ensureActive()

            // Double check cache after acquiring lock
            pageBitmapCache.get(cacheKey)?.let { cached ->
                if (!cached.isRecycled) return@withContext cached
            }

            val r = renderer ?: return@withContext null
            var page: PdfRenderer.Page? = null
            try {
                page = r.openPage(pageIndex)

                // Cache aspect ratio if not yet set
                if (aspectRatioCache[pageIndex] <= 0f && page.height > 0) {
                    aspectRatioCache[pageIndex] = page.width.toFloat() / page.height.toFloat()
                }

                currentCoroutineContext().ensureActive()

                val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)

                // DEFECT FIX: Fill destination canvas with opaque white before rendering.
                // PdfRenderer draws vectors/text over transparent pixels by default.
                // Without an opaque white pre-fill, transparent areas render as black artifacts.
                bitmap.eraseColor(Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                pageBitmapCache.put(cacheKey, bitmap)
                bitmap
            } catch (e: Exception) {
                Log.e("PdfPageRenderer", "Error rendering page $pageIndex: ${e.message}")
                null
            } finally {
                try {
                    page?.close()
                } catch (_: Exception) {}
            }
        }
    }

    suspend fun prefetchPage(pageIndex: Int, destWidth: Int, destHeight: Int) {
        if (pageIndex < 0 || pageIndex >= pageCount) return
        withContext(Dispatchers.IO) {
            val targetWidth = destWidth.coerceIn(100, 2560)
            val targetHeight = destHeight.coerceIn(100, 2560)
            val cacheKey = "${pageIndex}_${targetWidth}x${targetHeight}"
            if (pageBitmapCache.get(cacheKey) != null) return@withContext

            try {
                renderPage(pageIndex, targetWidth, targetHeight, 1.0f)
            } catch (_: Exception) {}
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
