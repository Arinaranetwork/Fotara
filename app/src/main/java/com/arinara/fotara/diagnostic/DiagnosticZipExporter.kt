// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.diagnostic

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * DiagnosticZipExporter packages technical diagnostic metrics into a compressed
 * ZIP archive containing itemized CSV tables, stack traces, and markdown summaries.
 * Strictly offline and user-consented; triggers the Android native share chooser.
 */
object DiagnosticZipExporter {

    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val fileNameDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    suspend fun createDiagnosticZip(context: Context): File = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "diagnostics")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }

        // Clean up archives older than 24 hours to prevent cache bloating
        cleanOldZipFiles(exportDir)

        val timestampStr = fileNameDateFormat.format(Date())
        val zipFile = File(exportDir, "Fotara_Diagnostic_$timestampStr.zip")

        val hw = AppDiagnosticLogger.getHardwareInfo(context)
        val summaryReport = AppDiagnosticLogger.generateFullReport(context)
        val eventsCsv = generateActivityEventsCsv(AppDiagnosticLogger.getAllEvents())
        val ramCsv = generateRamTimelineCsv(AppDiagnosticLogger.getAllRamSamples())
        val workerCsv = generateWorkerMemoryCsv(AppDiagnosticLogger.getAllWorkerRecords())
        val stuckText = generateStuckErrorsText(AppDiagnosticLogger.getAllStuckErrors())
        val errorsText = generateErrorsText(AppDiagnosticLogger.getAllErrors(), hw.previousCrashLog)
        val deviceInfoText = generateDeviceInfoText(hw)

        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
            fun addEntry(entryName: String, content: String) {
                zos.putNextEntry(ZipEntry(entryName))
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }

            addEntry("summary.md", summaryReport)
            addEntry("activity_events.csv", eventsCsv)
            addEntry("ram_timeline.csv", ramCsv)
            addEntry("worker_memory.csv", workerCsv)
            addEntry("stuck_errors.txt", stuckText)
            addEntry("errors.txt", errorsText)
            addEntry("device_info.txt", deviceInfoText)
        }

        zipFile
    }

    fun shareDiagnosticZip(context: Context, zipFile: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            zipFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Fotara Technical Diagnostic Bundle")
            putExtra(
                Intent.EXTRA_TEXT,
                "Fotara technical diagnostic telemetry bundle (${zipFile.name}, ${zipFile.length() / 1024} KB)."
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Share Diagnostic Bundle (.zip)")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    internal fun generateActivityEventsCsv(events: List<DiagnosticEvent>): String {
        val sb = StringBuilder()
        sb.appendLine("ID,Timestamp_ms,Formatted_Time,Elapsed_Since_App_Start_ms,Elapsed_Since_Last_Event_ms,Tag,Message")
        for (ev in events) {
            val formattedTime = timeFormat.format(Date(ev.timestampMs))
            sb.appendLine(
                "${ev.id},${ev.timestampMs},${escapeCsv(formattedTime)},${ev.elapsedSinceAppStartMs},${ev.elapsedSinceLastEventMs},${escapeCsv(ev.tag)},${escapeCsv(ev.message)}"
            )
        }
        return sb.toString()
    }

    internal fun generateRamTimelineCsv(samples: List<RamSample>): String {
        val sb = StringBuilder()
        sb.appendLine("Timestamp_ms,Formatted_Time,JVM_Used_MB,JVM_Allocated_MB,Native_Allocated_MB,Active_Screen")
        for (s in samples) {
            val formattedTime = timeFormat.format(Date(s.timestampMs))
            sb.appendLine(
                "${s.timestampMs},${escapeCsv(formattedTime)},${s.jvmUsedMb},${s.jvmAllocatedMb},${s.nativeAllocatedMb},${escapeCsv(s.activeScreen)}"
            )
        }
        return sb.toString()
    }

    internal fun generateWorkerMemoryCsv(records: List<WorkerMemoryRecord>): String {
        val sb = StringBuilder()
        sb.appendLine("ID,Timestamp_ms,Formatted_Time,Worker_Name,Start_Heap_MB,End_Heap_MB,Delta_Heap_MB,Duration_ms")
        for (r in records) {
            val formattedTime = timeFormat.format(Date(r.timestampMs))
            sb.appendLine(
                "${r.id},${r.timestampMs},${escapeCsv(formattedTime)},${escapeCsv(r.workerName)},${r.startHeapMb},${r.endHeapMb},${r.deltaHeapMb},${r.durationMs}"
            )
        }
        return sb.toString()
    }

    internal fun generateStuckErrorsText(stuckErrors: List<StuckErrorRecord>): String {
        val sb = StringBuilder()
        sb.appendLine("=== FOTARA MAIN-THREAD HANG & FREEZE LOGS ===")
        sb.appendLine("Total recorded hitches/freezes: ${stuckErrors.size}")
        sb.appendLine("Generated: ${Date()}")
        sb.appendLine("==============================================")
        sb.appendLine()
        if (stuckErrors.isEmpty()) {
            sb.appendLine("No main-thread hitches or freezes recorded during this session.")
        } else {
            for (stuck in stuckErrors) {
                val severity = if (stuck.isSevereFreeze) "SEVERE FREEZE" else "HITCH"
                val timeStr = timeFormat.format(Date(stuck.timestampMs))
                sb.appendLine("--------------------------------------------------------------------------------")
                sb.appendLine("[$timeStr] [$severity] Duration: ${stuck.durationMs}ms | Screen: ${stuck.activeScreen}")
                sb.appendLine("Top Method: ${stuck.topMethod}")
                sb.appendLine("Full Stack Trace:")
                sb.appendLine(stuck.fullStackTrace.ifBlank { "  (No stack trace available)" })
                sb.appendLine()
            }
        }
        return sb.toString()
    }

    internal fun generateErrorsText(errors: List<DiagnosticErrorEntry>, previousCrashLog: String?): String {
        val sb = StringBuilder()
        sb.appendLine("=== FOTARA CAUGHT EXCEPTIONS & RUNTIME ERRORS ===")
        sb.appendLine("Total recorded errors: ${errors.size}")
        sb.appendLine("Generated: ${Date()}")
        sb.appendLine("==================================================")
        sb.appendLine()
        if (previousCrashLog != null) {
            sb.appendLine("--- PREVIOUS FATAL CRASH LOG ---")
            sb.appendLine(previousCrashLog)
            sb.appendLine("--------------------------------")
            sb.appendLine()
        }
        if (errors.isEmpty()) {
            sb.appendLine("No caught exceptions or runtime errors recorded during this session.")
        } else {
            for (err in errors) {
                val timeStr = timeFormat.format(Date(err.timestampMs))
                sb.appendLine("--------------------------------------------------------------------------------")
                sb.appendLine("[$timeStr] [${err.tag}] ${err.message}")
                sb.appendLine("Exception Class: ${err.exceptionClass}")
                sb.appendLine("Thread: ${err.threadName}")
                sb.appendLine("Stack Trace:")
                sb.appendLine(err.stackTrace.ifBlank { "  (No stack trace available)" })
                sb.appendLine()
            }
        }
        return sb.toString()
    }

    internal fun generateDeviceInfoText(hw: HardwareInfo): String {
        val sb = StringBuilder()
        sb.appendLine("=== FOTARA SYSTEM & HARDWARE SPECIFICATIONS ===")
        sb.appendLine("Generated: ${Date()}")
        sb.appendLine("===============================================")
        sb.appendLine()
        sb.appendLine("DEVICE & PLATFORM:")
        sb.appendLine("  Manufacturer: ${hw.manufacturer}")
        sb.appendLine("  Model: ${hw.model}")
        sb.appendLine("  Brand: ${hw.brand}")
        sb.appendLine("  Board: ${hw.board}")
        sb.appendLine("  Android Version: ${hw.androidVersion} (API ${hw.apiLevel})")
        sb.appendLine("  Security Patch: ${hw.securityPatch}")
        sb.appendLine("  Build Fingerprint: ${hw.buildFingerprint}")
        sb.appendLine("  CPU Cores: ${hw.cpuCores}")
        sb.appendLine("  Supported ABIs: ${hw.supportedAbis}")
        sb.appendLine()
        sb.appendLine("MEMORY & RUNTIME:")
        sb.appendLine("  JVM Heap Used: ${hw.jvmHeapUsedMb} MB")
        sb.appendLine("  JVM Heap Allocated: ${hw.jvmHeapTotalMb} MB")
        sb.appendLine("  JVM Heap Max: ${hw.jvmHeapMaxMb} MB")
        sb.appendLine("  Native Heap Allocated: ${hw.nativeHeapAllocatedMb} MB")
        sb.appendLine("  Native Heap Size: ${hw.nativeHeapSizeMb} MB")
        sb.appendLine("  System RAM Available: ${hw.systemRamAvailableMb} MB")
        sb.appendLine("  System RAM Total: ${hw.systemRamTotalMb} MB")
        sb.appendLine("  Low Memory State: ${hw.isLowMemory}")
        sb.appendLine("  Memory Class: ${hw.memoryClassMb} MB (Large: ${hw.largeMemoryClassMb} MB)")
        sb.appendLine("  Active Runtime Threads: ${hw.activeThreadCount}")
        sb.appendLine("  ART GC Count: ${hw.artGcCount}")
        sb.appendLine("  ART GC Time: ${hw.artGcTimeMs} ms")
        sb.appendLine()
        sb.appendLine("STORAGE:")
        sb.appendLine("  Free Internal Storage: ${hw.storageFreeMb} MB")
        sb.appendLine("  Total Internal Storage: ${hw.storageTotalMb} MB")
        sb.appendLine("  Used Internal Storage: ${hw.storageUsedMb} MB")
        sb.appendLine("  App Files (filesDir): ${hw.appFilesKb} KB")
        sb.appendLine("  App Cache (cacheDir): ${hw.appCacheKb} KB")
        sb.appendLine("  App SQLite DB (DB+WAL): ${hw.appDatabaseKb} KB")
        sb.appendLine()
        sb.appendLine("DISPLAY & ENVIRONMENT:")
        sb.appendLine("  Display Resolution: ${hw.displayResolution}")
        sb.appendLine("  Display Density DPI: ${hw.displayDensityDpi}")
        sb.appendLine("  Display Refresh Rate: ${hw.displayRefreshRate} Hz")
        sb.appendLine("  Battery Level: ${if (hw.batteryLevelPct >= 0) "${hw.batteryLevelPct}%" else "Unknown"}")
        sb.appendLine("  Power Save Mode: ${hw.isPowerSaveMode}")
        sb.appendLine("  Thermal Status: ${hw.thermalStatus}")
        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    private fun cleanOldZipFiles(exportDir: File) {
        try {
            val oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000L)
            exportDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith(".zip") && file.lastModified() < oneDayAgo) {
                    file.delete()
                }
            }
        } catch (_: Exception) {}
    }
}
