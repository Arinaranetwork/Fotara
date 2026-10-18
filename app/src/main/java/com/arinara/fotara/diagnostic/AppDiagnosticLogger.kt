// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.diagnostic

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedDeque

data class DiagnosticEvent(
    val id: Long,
    val timestampMs: Long,
    val tag: String,
    val message: String,
    val elapsedSinceAppStartMs: Long,
    val elapsedSinceLastEventMs: Long
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
    val storageTotalMb: Long
)

object AppDiagnosticLogger {

    private const val PREFS_NAME = "fotara_diagnostic_prefs"
    private const val KEY_OVERLAY_ENABLED = "diagnostic_overlay_enabled"
    private const val MAX_EVENT_BUFFER = 400

    private var appStartUptimeMs: Long = SystemClock.elapsedRealtime()
    private var lastEventUptimeMs: Long = appStartUptimeMs
    private var eventCounter: Long = 0L

    private val eventHistory = ConcurrentLinkedDeque<DiagnosticEvent>()
    private val _eventsFlow = MutableStateFlow<List<DiagnosticEvent>>(emptyList())
    val eventsFlow: StateFlow<List<DiagnosticEvent>> = _eventsFlow.asStateFlow()

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
        recordEvent("Logger", "Diagnostic system initialized")
    }

    fun setOverlayEnabled(context: Context, enabled: Boolean) {
        _isOverlayEnabled.value = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_OVERLAY_ENABLED, enabled).apply()
        recordEvent("Overlay", if (enabled) "Overlay enabled" else "Overlay disabled")
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

    fun recordFirstFrameRendered() {
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

        val id = ++eventCounter
        val event = DiagnosticEvent(
            id = id,
            timestampMs = System.currentTimeMillis(),
            tag = tag,
            message = message,
            elapsedSinceAppStartMs = deltaSinceStart,
            elapsedSinceLastEventMs = deltaSinceLast
        )

        eventHistory.addLast(event)
        while (eventHistory.size > MAX_EVENT_BUFFER) {
            eventHistory.pollFirst()
        }

        _eventsFlow.value = eventHistory.toList()
    }

    fun getHardwareInfo(context: Context? = null): HardwareInfo {
        val runtime = Runtime.getRuntime()
        val jvmHeapTotal = runtime.totalMemory() / (1024 * 1024)
        val jvmHeapFree = runtime.freeMemory() / (1024 * 1024)
        val jvmHeapUsed = jvmHeapTotal - jvmHeapFree
        val jvmHeapMax = runtime.maxMemory() / (1024 * 1024)

        val totalRamMb: Long
        val availRamMb: Long
        val isLowMem: Boolean
        if (context != null) {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager?.getMemoryInfo(memInfo)
            totalRamMb = memInfo.totalMem / (1024 * 1024)
            availRamMb = memInfo.availMem / (1024 * 1024)
            isLowMem = memInfo.lowMemory
        } else {
            totalRamMb = jvmHeapMax
            availRamMb = jvmHeapFree
            isLowMem = false
        }

        var freeStorageMb = 0L
        var totalStorageMb = 0L
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            freeStorageMb = (stat.availableBlocksLong * stat.blockSizeLong) / (1024 * 1024)
            totalStorageMb = (stat.blockCountLong * stat.blockSizeLong) / (1024 * 1024)
        } catch (_: Exception) {}

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
            storageTotalMb = totalStorageMb
        )
    }

    fun generateFullReport(context: Context? = null): String {
        val hw = getHardwareInfo(context)
        val metrics = _startupMetricsFlow.value
        val recentEvents = eventHistory.toList()

        val sb = StringBuilder()
        sb.appendLine("## Fotara Diagnostic & Performance Report")
        sb.appendLine("Generated at: ${Date()}")
        sb.appendLine()
        sb.appendLine("### 1. Device & Hardware Specifications")
        sb.appendLine("- **Device**: ${hw.brand} ${hw.model} (${hw.manufacturer}) [Board: ${hw.board}]")
        sb.appendLine("- **OS**: Android ${hw.androidVersion} (API ${hw.apiLevel})")
        sb.appendLine("- **Fingerprint**: `${hw.buildFingerprint}`")
        sb.appendLine("- **CPU Cores**: ${hw.cpuCores}")
        sb.appendLine("- **System RAM**: ${hw.systemRamAvailableMb} MB available / ${hw.systemRamTotalMb} MB total (LowMemory: ${hw.isLowMemory})")
        sb.appendLine("- **JVM Heap**: ${hw.jvmHeapUsedMb} MB used / ${hw.jvmHeapTotalMb} MB allocated / ${hw.jvmHeapMaxMb} MB max")
        sb.appendLine("- **Internal Storage**: ${hw.storageFreeMb} MB free / ${hw.storageTotalMb} MB total")
        sb.appendLine()
        sb.appendLine("### 2. Startup Latency Breakdown")
        sb.appendLine("- **Total Cold Start Time**: **${metrics.totalColdStartMs} ms** (from process launch to first interactive frame)")
        sb.appendLine("- **Application.onCreate Duration**: ${metrics.appCreateDurationMs} ms")
        sb.appendLine("- **AppContainer Initialization**: ${metrics.containerInitDurationMs} ms")
        sb.appendLine("- **Time from SetContent to First Frame**: ${if (metrics.setContentStartMs > 0 && metrics.firstFrameRenderedMs > 0) metrics.firstFrameRenderedMs - metrics.setContentStartMs else 0} ms")
        sb.appendLine()
        sb.appendLine("### 3. Chronological Event Timeline (${recentEvents.size} events captured)")
        sb.appendLine("| Time | T+ (ms) | Delta | Tag | Event Description |")
        sb.appendLine("| :--- | :--- | :--- | :--- | :--- |")
        for (ev in recentEvents) {
            val formattedTime = timeFormat.format(Date(ev.timestampMs))
            sb.appendLine("| $formattedTime | +${ev.elapsedSinceAppStartMs}ms | +${ev.elapsedSinceLastEventMs}ms | ${ev.tag} | ${ev.message} |")
        }
        sb.appendLine()
        return sb.toString()
    }
}
