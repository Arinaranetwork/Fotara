// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.diagnostic

import android.app.ActivityManager
import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.Debug
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.os.SystemClock
import android.util.Log
import android.view.WindowManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

data class DiagnosticEvent(
    val id: Long,
    val timestampMs: Long,
    val tag: String,
    val message: String,
    val elapsedSinceAppStartMs: Long,
    val elapsedSinceLastEventMs: Long
)

data class DiagnosticErrorEntry(
    val id: Long,
    val timestampMs: Long,
    val tag: String,
    val message: String,
    val exceptionClass: String,
    val stackTrace: String,
    val threadName: String
)

data class RamSample(
    val timestampMs: Long,
    val jvmUsedMb: Long,
    val jvmAllocatedMb: Long,
    val nativeAllocatedMb: Long,
    val activeScreen: String
)

data class WorkerMemoryRecord(
    val id: Long,
    val timestampMs: Long,
    val workerName: String,
    val startHeapMb: Long,
    val endHeapMb: Long,
    val deltaHeapMb: Long,
    val durationMs: Long
)

data class GcResult(
    val beforeUsedMb: Long,
    val afterUsedMb: Long,
    val freedMb: Long,
    val durationMs: Long
)


data class StartupMetrics(
    val appStartTimeMs: Long = 0L,
    val appCreateStartMs: Long = 0L,
    val appCreateEndMs: Long = 0L,
    val appCreateDurationMs: Long = 0L,
    val activityCreateStartMs: Long = 0L,
    val activityCreateEndMs: Long = 0L,
    val containerInitDurationMs: Long = 0L,
    val setContentStartMs: Long = 0L,
    val firstFrameRenderedMs: Long = 0L,
    val totalColdStartMs: Long = 0L
)

data class HardwareInfo(
    val manufacturer: String,
    val model: String,
    val brand: String,
    val board: String,
    val androidVersion: String,
    val apiLevel: Int,
    val buildFingerprint: String,
    val cpuCores: Int,
    val jvmHeapUsedMb: Long,
    val jvmHeapTotalMb: Long,
    val jvmHeapMaxMb: Long,
    val systemRamAvailableMb: Long,
    val systemRamTotalMb: Long,
    val isLowMemory: Boolean,
    val storageFreeMb: Long,
    val storageTotalMb: Long,
    val nativeHeapAllocatedMb: Long = 0L,
    val nativeHeapSizeMb: Long = 0L,
    val memoryClassMb: Int = 0,
    val largeMemoryClassMb: Int = 0,
    val artGcCount: String = "N/A",
    val artGcTimeMs: String = "N/A",
    val storageUsedMb: Long = 0L,
    val appFilesKb: Long = 0L,
    val appCacheKb: Long = 0L,
    val appDatabaseKb: Long = 0L,
    val securityPatch: String = "Unknown",
    val supportedAbis: String = "Unknown",
    val displayResolution: String = "Unknown",
    val displayDensityDpi: Int = 0,
    val displayRefreshRate: Float = 0f,
    val batteryLevelPct: Int = -1,
    val isPowerSaveMode: Boolean = false,
    val thermalStatus: String = "Unknown",
    val activeThreadCount: Int = 0,
    val previousCrashLog: String? = null
)

object AppDiagnosticLogger {

    private const val PREFS_NAME = "fotara_diagnostic_prefs"
    private const val KEY_OVERLAY_ENABLED = "diagnostic_overlay_enabled"
    private const val KEY_DEVELOPER_MODE_ENABLED = "developer_mode_enabled"
    private const val MAX_EVENT_BUFFER = 400
    private const val MAX_ERROR_BUFFER = 50
    private const val MAX_RAM_HISTORY = 60
    private const val MAX_WORKER_HISTORY = 50

    private var appStartUptimeMs: Long = SystemClock.elapsedRealtime()
    private var lastEventUptimeMs: Long = appStartUptimeMs
    private val eventCounter = AtomicLong(0L)
    private val historySize = AtomicInteger(0)

    private val isFirstFrameCaptured = AtomicBoolean(false)

    private val eventHistory = ConcurrentLinkedDeque<DiagnosticEvent>()
    private val _eventsFlow = MutableStateFlow<List<DiagnosticEvent>>(emptyList())
    val eventsFlow: StateFlow<List<DiagnosticEvent>> = _eventsFlow.asStateFlow()

    private val errorCounter = AtomicLong(0L)
    private val errorHistory = ConcurrentLinkedDeque<DiagnosticErrorEntry>()
    private val _errorsFlow = MutableStateFlow<List<DiagnosticErrorEntry>>(emptyList())
    val errorsFlow: StateFlow<List<DiagnosticErrorEntry>> = _errorsFlow.asStateFlow()

    private val _isDeveloperModeEnabled = MutableStateFlow(false)
    val isDeveloperModeEnabled: StateFlow<Boolean> = _isDeveloperModeEnabled.asStateFlow()

    private val ramHistory = ConcurrentLinkedDeque<RamSample>()
    private val _ramHistoryFlow = MutableStateFlow<List<RamSample>>(emptyList())
    val ramHistoryFlow: StateFlow<List<RamSample>> = _ramHistoryFlow.asStateFlow()

    private val workerCounter = AtomicLong(0L)
    private val workerMemoryHistory = ConcurrentLinkedDeque<WorkerMemoryRecord>()
    private val _workerMemoryFlow = MutableStateFlow<List<WorkerMemoryRecord>>(emptyList())
    val workerMemoryFlow: StateFlow<List<WorkerMemoryRecord>> = _workerMemoryFlow.asStateFlow()

    private val _startupMetricsFlow = MutableStateFlow(StartupMetrics(appStartTimeMs = appStartUptimeMs))
    val startupMetricsFlow: StateFlow<StartupMetrics> = _startupMetricsFlow.asStateFlow()

    private val _isOverlayEnabled = MutableStateFlow(true)
    val isOverlayEnabled: StateFlow<Boolean> = _isOverlayEnabled.asStateFlow()

    private val _currentScreen = MutableStateFlow("Startup")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isOverlayEnabled.value = prefs.getBoolean(KEY_OVERLAY_ENABLED, true)
        _isDeveloperModeEnabled.value = prefs.getBoolean(KEY_DEVELOPER_MODE_ENABLED, false)
        recordEvent("Logger", "Diagnostic system initialized")

        // Check for previous crash log from a fatal crash
        try {
            val crashFile = File(context.filesDir, "crash.log")
            if (crashFile.exists()) {
                val crashText = crashFile.readText()
                if (crashText.isNotBlank()) {
                    recordEvent("CrashRecovery", "Detected previous fatal crash log (${crashFile.length()} bytes)")
                }
            }
        } catch (_: Exception) {}
    }

    fun setOverlayEnabled(context: Context, enabled: Boolean) {
        _isOverlayEnabled.value = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_OVERLAY_ENABLED, enabled).apply()
        recordEvent("Overlay", if (enabled) "Overlay enabled" else "Overlay disabled")
    }

    fun setDeveloperModeEnabled(context: Context, enabled: Boolean) {
        _isDeveloperModeEnabled.value = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DEVELOPER_MODE_ENABLED, enabled).apply()
        recordEvent("DeveloperMode", if (enabled) "Developer mode enabled" else "Developer mode disabled")
    }

    fun recordRamSample(screenName: String = _currentScreen.value) {
        val runtime = Runtime.getRuntime()
        val jvmUsed = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val jvmAlloc = runtime.totalMemory() / (1024 * 1024)
        val nativeAlloc = Debug.getNativeHeapAllocatedSize() / (1024 * 1024)
        val sample = RamSample(
            timestampMs = System.currentTimeMillis(),
            jvmUsedMb = jvmUsed,
            jvmAllocatedMb = jvmAlloc,
            nativeAllocatedMb = nativeAlloc,
            activeScreen = screenName
        )
        ramHistory.addLast(sample)
        while (ramHistory.size > MAX_RAM_HISTORY) {
            ramHistory.pollFirst()
        }
        _ramHistoryFlow.value = ramHistory.toList()
    }

    fun recordWorkerMemory(workerName: String, startHeapMb: Long, endHeapMb: Long, durationMs: Long) {
        val id = workerCounter.incrementAndGet()
        val delta = endHeapMb - startHeapMb
        val record = WorkerMemoryRecord(
            id = id,
            timestampMs = System.currentTimeMillis(),
            workerName = workerName,
            startHeapMb = startHeapMb,
            endHeapMb = endHeapMb,
            deltaHeapMb = delta,
            durationMs = durationMs
        )
        workerMemoryHistory.addLast(record)
        while (workerMemoryHistory.size > MAX_WORKER_HISTORY) {
            workerMemoryHistory.pollFirst()
        }
        _workerMemoryFlow.value = workerMemoryHistory.toList()
        val deltaStr = if (delta >= 0) "+${delta}MB" else "${delta}MB"
        recordEvent("WorkerMemory", "[$workerName] ${startHeapMb}MB -> ${endHeapMb}MB ($deltaStr in ${durationMs}ms)")
    }

    fun runManualGcAndMeasureDelta(): GcResult {
        val runtime = Runtime.getRuntime()
        val beforeUsed = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val start = SystemClock.elapsedRealtime()
        System.gc()
        System.runFinalization()
        val duration = SystemClock.elapsedRealtime() - start
        val afterUsed = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val freed = if (beforeUsed > afterUsed) beforeUsed - afterUsed else 0L

        recordRamSample(_currentScreen.value)
        recordEvent("ManualGC", "Forced System.gc() freed ${freed}MB in ${duration}ms (${beforeUsed}MB -> ${afterUsed}MB)")
        return GcResult(
            beforeUsedMb = beforeUsed,
            afterUsedMb = afterUsed,
            freedMb = freed,
            durationMs = duration
        )
    }


    fun recordAppCreateStart() {
        val now = SystemClock.elapsedRealtime()
        appStartUptimeMs = now
        lastEventUptimeMs = now
        _startupMetricsFlow.update { it.copy(appStartTimeMs = now, appCreateStartMs = now) }
        recordEvent("Lifecycle", "FotaraApp.onCreate started")
    }

    fun recordAppCreateEnd() {
        val now = SystemClock.elapsedRealtime()
        val duration = now - _startupMetricsFlow.value.appCreateStartMs
        _startupMetricsFlow.update { it.copy(appCreateEndMs = now, appCreateDurationMs = duration) }
        recordEvent("Lifecycle", "FotaraApp.onCreate finished (${duration}ms)")
    }

    fun recordActivityCreateStart() {
        val now = SystemClock.elapsedRealtime()
        _startupMetricsFlow.update { it.copy(activityCreateStartMs = now) }
        recordEvent("Lifecycle", "MainActivity.onCreate started")
    }

    fun recordContainerInit(durationMs: Long) {
        _startupMetricsFlow.update { it.copy(containerInitDurationMs = durationMs) }
        recordEvent("Container", "DefaultAppContainer initialized (${durationMs}ms)")
    }

    fun recordSetContent() {
        val now = SystemClock.elapsedRealtime()
        _startupMetricsFlow.update { it.copy(setContentStartMs = now) }
        recordEvent("UI", "MainActivity setContent invoked")
    }

    /**
     * Records the first frame rendered for the application.
     * Guarded with an atomic flag so subsequent screen navigations and recompositions
     * do NOT corrupt the genuine initial cold start latency.
     */
    fun recordFirstFrameRendered() {
        if (!isFirstFrameCaptured.compareAndSet(false, true)) {
            recordEvent("Navigation", "Home UI screen re-entered")
            return
        }
        val now = SystemClock.elapsedRealtime()
        val totalCold = now - appStartUptimeMs
        _startupMetricsFlow.update {
            it.copy(
                firstFrameRenderedMs = now,
                totalColdStartMs = totalCold
            )
        }
        recordEvent("UI", "Home UI first frame displayed! Total cold start: ${totalCold}ms")
    }

    fun recordScreen(screenName: String) {
        _currentScreen.value = screenName
        recordEvent("Navigation", "Navigated to $screenName")
    }

    fun recordEvent(tag: String, message: String) {
        val now = SystemClock.elapsedRealtime()
        val deltaSinceStart = now - appStartUptimeMs
        val deltaSinceLast = now - lastEventUptimeMs
        lastEventUptimeMs = now

        val id = eventCounter.incrementAndGet()
        val event = DiagnosticEvent(
            id = id,
            timestampMs = System.currentTimeMillis(),
            tag = tag,
            message = message,
            elapsedSinceAppStartMs = deltaSinceStart,
            elapsedSinceLastEventMs = deltaSinceLast
        )

        eventHistory.addLast(event)
        if (historySize.incrementAndGet() > MAX_EVENT_BUFFER) {
            if (eventHistory.pollFirst() != null) {
                historySize.decrementAndGet()
            }
        }

        _eventsFlow.value = eventHistory.toList()
    }

    fun recordError(tag: String, message: String, throwable: Throwable? = null) {
        val id = errorCounter.incrementAndGet()
        val exClass = throwable?.javaClass?.name ?: "None"
        val stackTrace = throwable?.stackTraceToString() ?: ""
        val entry = DiagnosticErrorEntry(
            id = id,
            timestampMs = System.currentTimeMillis(),
            tag = tag,
            message = message,
            exceptionClass = exClass,
            stackTrace = stackTrace,
            threadName = Thread.currentThread().name
        )

        errorHistory.addLast(entry)
        while (errorHistory.size > MAX_ERROR_BUFFER) {
            errorHistory.pollFirst()
        }

        _errorsFlow.value = errorHistory.toList()
        recordEvent("Error", "[$tag] $message ($exClass)")
    }

    private fun getFolderSize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var total = 0L
        val files = dir.listFiles() ?: return 0L
        for (f in files) {
            total += if (f.isDirectory) getFolderSize(f) else f.length()
        }
        return total
    }

    fun getHardwareInfo(context: Context? = null): HardwareInfo {
        val runtime = Runtime.getRuntime()
        val jvmHeapTotal = runtime.totalMemory() / (1024 * 1024)
        val jvmHeapFree = runtime.freeMemory() / (1024 * 1024)
        val jvmHeapUsed = jvmHeapTotal - jvmHeapFree
        val jvmHeapMax = runtime.maxMemory() / (1024 * 1024)

        val nativeAllocMb = Debug.getNativeHeapAllocatedSize() / (1024 * 1024)
        val nativeSizeMb = Debug.getNativeHeapSize() / (1024 * 1024)

        val totalRamMb: Long
        val availRamMb: Long
        val isLowMem: Boolean
        var memClass = 0
        var largeMemClass = 0

        if (context != null) {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager?.getMemoryInfo(memInfo)
            totalRamMb = memInfo.totalMem / (1024 * 1024)
            availRamMb = memInfo.availMem / (1024 * 1024)
            isLowMem = memInfo.lowMemory
            memClass = actManager?.memoryClass ?: 0
            largeMemClass = actManager?.largeMemoryClass ?: 0
        } else {
            totalRamMb = jvmHeapMax
            availRamMb = jvmHeapFree
            isLowMem = false
        }

        var freeStorageMb = 0L
        var totalStorageMb = 0L
        var usedStorageMb = 0L
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            freeStorageMb = (stat.availableBlocksLong * stat.blockSizeLong) / (1024 * 1024)
            totalStorageMb = (stat.blockCountLong * stat.blockSizeLong) / (1024 * 1024)
            usedStorageMb = if (totalStorageMb >= freeStorageMb) totalStorageMb - freeStorageMb else 0L
        } catch (_: Exception) {}

        var appFilesKb = 0L
        var appCacheKb = 0L
        var appDatabaseKb = 0L
        var crashLogText: String? = null

        var screenRes = "Unknown"
        var densityDpi = 0
        var refreshRate = 0f
        var batteryLevel = -1
        var isPowerSave = false
        var thermal = "Unknown"

        if (context != null) {
            try {
                appFilesKb = getFolderSize(context.filesDir) / 1024
                appCacheKb = getFolderSize(context.cacheDir) / 1024

                val dbFile = context.getDatabasePath("fotara.db")
                val walFile = context.getDatabasePath("fotara.db-wal")
                val shmFile = context.getDatabasePath("fotara.db-shm")
                val dbTotalBytes = (if (dbFile.exists()) dbFile.length() else 0L) +
                        (if (walFile.exists()) walFile.length() else 0L) +
                        (if (shmFile.exists()) shmFile.length() else 0L)
                appDatabaseKb = dbTotalBytes / 1024

                val crashFile = File(context.filesDir, "crash.log")
                if (crashFile.exists()) {
                    crashLogText = crashFile.readText()
                }

                val dm = context.resources.displayMetrics
                screenRes = "${dm.widthPixels} x ${dm.heightPixels} px"
                densityDpi = dm.densityDpi

                if (Build.VERSION.SDK_INT >= 30) {
                    refreshRate = context.display.mode.refreshRate
                } else {
                    val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                    @Suppress("DEPRECATION")
                    refreshRate = wm?.defaultDisplay?.refreshRate ?: 60f
                }

                val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                batteryLevel = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1

                val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                isPowerSave = pm?.isPowerSaveMode ?: false

                thermal = if (Build.VERSION.SDK_INT >= 29 && pm != null) {
                    when (pm.currentThermalStatus) {
                        PowerManager.THERMAL_STATUS_NONE -> "Nominal"
                        PowerManager.THERMAL_STATUS_LIGHT -> "Light Throttling"
                        PowerManager.THERMAL_STATUS_MODERATE -> "Moderate Throttling"
                        PowerManager.THERMAL_STATUS_SEVERE -> "Severe Throttling"
                        PowerManager.THERMAL_STATUS_CRITICAL -> "Critical Throttling"
                        PowerManager.THERMAL_STATUS_EMERGENCY -> "Emergency"
                        PowerManager.THERMAL_STATUS_SHUTDOWN -> "Shutdown"
                        else -> "Normal"
                    }
                } else "Supported API < 29"
            } catch (_: Exception) {}
        }

        val gcCount = try { Debug.getRuntimeStat("art.gc.gc-count") ?: "N/A" } catch (_: Exception) { "N/A" }
        val gcTimeMs = try { Debug.getRuntimeStat("art.gc.gc-time-ms") ?: "N/A" } catch (_: Exception) { "N/A" }

        val secPatch = if (Build.VERSION.SDK_INT >= 23) (Build.VERSION.SECURITY_PATCH ?: "N/A") else "N/A"
        val supportedAbis = Build.SUPPORTED_ABIS?.joinToString(", ") ?: "Unknown"

        return HardwareInfo(
            manufacturer = Build.MANUFACTURER ?: "Unknown",
            model = Build.MODEL ?: "Unknown",
            brand = Build.BRAND ?: "Unknown",
            board = Build.BOARD ?: "Unknown",
            androidVersion = Build.VERSION.RELEASE ?: "Unknown",
            apiLevel = Build.VERSION.SDK_INT,
            buildFingerprint = Build.FINGERPRINT ?: "Unknown",
            cpuCores = runtime.availableProcessors(),
            jvmHeapUsedMb = jvmHeapUsed,
            jvmHeapTotalMb = jvmHeapTotal,
            jvmHeapMaxMb = jvmHeapMax,
            systemRamAvailableMb = availRamMb,
            systemRamTotalMb = totalRamMb,
            isLowMemory = isLowMem,
            storageFreeMb = freeStorageMb,
            storageTotalMb = totalStorageMb,
            nativeHeapAllocatedMb = nativeAllocMb,
            nativeHeapSizeMb = nativeSizeMb,
            memoryClassMb = memClass,
            largeMemoryClassMb = largeMemClass,
            artGcCount = gcCount,
            artGcTimeMs = gcTimeMs,
            storageUsedMb = usedStorageMb,
            appFilesKb = appFilesKb,
            appCacheKb = appCacheKb,
            appDatabaseKb = appDatabaseKb,
            securityPatch = secPatch,
            supportedAbis = supportedAbis,
            displayResolution = screenRes,
            displayDensityDpi = densityDpi,
            displayRefreshRate = refreshRate,
            batteryLevelPct = batteryLevel,
            isPowerSaveMode = isPowerSave,
            thermalStatus = thermal,
            activeThreadCount = Thread.activeCount(),
            previousCrashLog = crashLogText
        )
    }

    fun generateFullReport(context: Context? = null): String {
        val hw = getHardwareInfo(context)
        val metrics = _startupMetricsFlow.value
        val recentEvents = eventHistory.toList()
        val errors = errorHistory.toList()

        val sb = StringBuilder()
        sb.appendLine("## Fotara Diagnostic & Performance Report")
        sb.appendLine("Generated at: ${Date()}")
        sb.appendLine()
        sb.appendLine("### 1. Startup Latency Breakdown")
        sb.appendLine("- **Total Cold Start Time**: **${metrics.totalColdStartMs} ms** (from process launch to first interactive frame)")
        sb.appendLine("- **Application.onCreate Duration**: ${metrics.appCreateDurationMs} ms")
        sb.appendLine("- **AppContainer Initialization**: ${metrics.containerInitDurationMs} ms")
        val setContentToFrame = if (metrics.setContentStartMs > 0 && metrics.firstFrameRenderedMs > 0) {
            metrics.firstFrameRenderedMs - metrics.setContentStartMs
        } else 0L
        sb.appendLine("- **Time from SetContent to First Frame**: $setContentToFrame ms")
        sb.appendLine()
        sb.appendLine("### 2. Memory & RAM Specifications")
        val heapPercent = if (hw.jvmHeapMaxMb > 0) (hw.jvmHeapUsedMb * 100 / hw.jvmHeapMaxMb) else 0
        sb.appendLine("- **JVM Heap**: ${hw.jvmHeapUsedMb} MB used / ${hw.jvmHeapTotalMb} MB allocated / ${hw.jvmHeapMaxMb} MB max ($heapPercent% used)")
        sb.appendLine("- **Native Heap**: ${hw.nativeHeapAllocatedMb} MB allocated / ${hw.nativeHeapSizeMb} MB total")
        val usedRam = if (hw.systemRamTotalMb >= hw.systemRamAvailableMb) hw.systemRamTotalMb - hw.systemRamAvailableMb else 0
        sb.appendLine("- **System RAM**: ${hw.systemRamAvailableMb} MB available / ${hw.systemRamTotalMb} MB total (${usedRam} MB used, LowMemory: ${hw.isLowMemory})")
        if (hw.memoryClassMb > 0) {
            sb.appendLine("- **Memory Classes**: Standard ${hw.memoryClassMb} MB, Large Heap ${hw.largeMemoryClassMb} MB")
        }
        sb.appendLine("- **ART Garbage Collection**: Runs: ${hw.artGcCount}, Total Duration: ${hw.artGcTimeMs} ms")
        sb.appendLine()
        sb.appendLine("### 3. Disk & Storage Footprint")
        sb.appendLine("- **Internal Storage**: ${hw.storageFreeMb} MB free / ${hw.storageTotalMb} MB total (${hw.storageUsedMb} MB used)")
        sb.appendLine("- **App Internal Files**: ${hw.appFilesKb} KB (`context.filesDir`)")
        sb.appendLine("- **App Cache Directory**: ${hw.appCacheKb} KB (`context.cacheDir`)")
        sb.appendLine("- **Fotara SQLite Database**: ${hw.appDatabaseKb} KB (DB + WAL + SHM)")
        val totalAppKb = hw.appFilesKb + hw.appCacheKb + hw.appDatabaseKb
        sb.appendLine("- **Total App Data Footprint**: ${totalAppKb} KB (~${totalAppKb / 1024} MB)")
        sb.appendLine()
        sb.appendLine("### 4. Hardware, Display & Environment")
        sb.appendLine("- **Device**: ${hw.brand} ${hw.model} (${hw.manufacturer}) [Board: ${hw.board}]")
        sb.appendLine("- **OS**: Android ${hw.androidVersion} (API ${hw.apiLevel}), Security Patch: ${hw.securityPatch}")
        sb.appendLine("- **Fingerprint**: `${hw.buildFingerprint}`")
        sb.appendLine("- **CPU Cores**: ${hw.cpuCores} (Supported ABIs: ${hw.supportedAbis})")
        sb.appendLine("- **Display**: ${hw.displayResolution} @ ${hw.displayDensityDpi} dpi, ${String.format(Locale.US, "%.1f", hw.displayRefreshRate)} Hz")
        sb.appendLine("- **Battery & Power**: ${if (hw.batteryLevelPct >= 0) "${hw.batteryLevelPct}%" else "Unknown"}, Power Save Mode: ${hw.isPowerSaveMode}, Thermal State: ${hw.thermalStatus}")
        sb.appendLine("- **Runtime Threads**: ${hw.activeThreadCount} active threads")
        sb.appendLine()
        sb.appendLine("### 5. Recorded Code Errors & Exceptions (${errors.size} recorded)")
        if (hw.previousCrashLog != null) {
            sb.appendLine("#### Previous Fatal Crash Log:")
            sb.appendLine("```")
            sb.appendLine(hw.previousCrashLog.take(2000))
            sb.appendLine("```")
            sb.appendLine()
        }
        if (errors.isEmpty()) {
            sb.appendLine("*No runtime exceptions or code errors recorded during this session.*")
        } else {
            for (err in errors) {
                val formattedTime = timeFormat.format(Date(err.timestampMs))
                sb.appendLine("- **[$formattedTime] [${err.tag}]** ${err.message} (Thread: ${err.threadName}, Exception: `${err.exceptionClass}`)")
                if (err.stackTrace.isNotBlank()) {
                    sb.appendLine("  ```")
                    sb.appendLine("  " + err.stackTrace.lines().take(10).joinToString("\n  "))
                    sb.appendLine("  ```")
                }
            }
        }
        sb.appendLine()
        sb.appendLine("### 6. Chronological Event Timeline (${recentEvents.size} events captured)")
        sb.appendLine("| Time | T+ (ms) | Delta | Tag | Event Description |")
        sb.appendLine("| :--- | :--- | :--- | :--- | :--- |")
        for (ev in recentEvents) {
            val formattedTime = timeFormat.format(Date(ev.timestampMs))
            sb.appendLine("| $formattedTime | +${ev.elapsedSinceAppStartMs}ms | +${ev.elapsedSinceLastEventMs}ms | ${ev.tag} | ${ev.message} |")
        }
        sb.appendLine()
        return sb.toString()
    }

    fun resetForTesting() {
        isFirstFrameCaptured.set(false)
        appStartUptimeMs = SystemClock.elapsedRealtime()
        lastEventUptimeMs = appStartUptimeMs
        eventCounter.set(0L)
        historySize.set(0)
        eventHistory.clear()
        _eventsFlow.value = emptyList()
        errorCounter.set(0L)
        errorHistory.clear()
        _errorsFlow.value = emptyList()
        _isDeveloperModeEnabled.value = false
        ramHistory.clear()
        _ramHistoryFlow.value = emptyList()
        workerCounter.set(0L)
        workerMemoryHistory.clear()
        _workerMemoryFlow.value = emptyList()
        _startupMetricsFlow.value = StartupMetrics(appStartTimeMs = appStartUptimeMs)
    }
}
