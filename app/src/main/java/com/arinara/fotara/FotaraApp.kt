// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara

import android.app.Application
import android.os.Build
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import java.io.File
import java.util.Date

class FotaraApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()

        // Install process-level crash handler once
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                com.arinara.fotara.diagnostic.AppDiagnosticLogger.recordError("FatalCrash", throwable.message ?: "Uncaught exception", throwable)
                val logFile = File(filesDir, "crash.log")
                logFile.writeText(
                    "FOTARA FATAL CRASH [${Date()}]\n" +
                    "Thread: ${thread.name}\n" +
                    Log.getStackTraceString(throwable)
                )
            } catch (_: Exception) {}
            previousHandler?.uncaughtException(thread, throwable)
        }

        com.arinara.fotara.diagnostic.AppDiagnosticLogger.recordAppCreateStart()
        com.arinara.fotara.diagnostic.AppDiagnosticLogger.initialize(this)
        com.arinara.fotara.diagnostic.AppDiagnosticLogger.recordAppCreateEnd()
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.15)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024 * 1024) // 50 MB
                    .build()
            }
            .respectCacheHeaders(false)
            .crossfade(true)
            .build()
    }
}
