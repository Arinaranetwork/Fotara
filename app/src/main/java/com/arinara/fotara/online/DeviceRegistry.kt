// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.online

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.arinara.fotara.data.repository.SettingsRepository
import com.arinara.fotara.legal.NetworkGate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Anonymous device count reporter communicating with Supabase RPC functions.
 * - Local-only random UUID v4 created on first use after consent
 * - Collects zero hardware IDs, MACs, IMEIs, Advertising IDs, or user info
 * - Sends strictly: device_id, app_version, channel
 * - Throttled to at most once per 24 hours or upon app version change
 * - Deletes local ID and issues unregister request when disabled
 */
class DeviceRegistry(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val supabaseUrl: String = "https://nrvnhbizyvubcdqvzabv.supabase.co",
    private val supabaseKey: String = "sb_publishable_Fn-s-CleInVH76uvH7c9aQ_QnQoMrrs"
) {

    companion object {
        const val PREFS_NAME = "fotara_device_prefs"
        const val KEY_DEVICE_ID = "device_id"
        const val KEY_LAST_SENT_TIMESTAMP = "last_sent_timestamp"
        const val KEY_LAST_SENT_VERSION = "last_sent_version"
        const val KEY_PENDING_UNREGISTER_ID = "pending_unregister_id"
        const val KEY_PENDING_UNREGISTER_TIMESTAMP = "pending_unregister_timestamp"
        const val KEY_PENDING_SEND = "pending_send"

        const val THROTTLE_INTERVAL_MS = 24 * 60 * 60 * 1000L // 24 hours
        const val INITIAL_BACKOFF_MS = 5 * 60 * 1000L // 5 minutes
        const val MAX_BACKOFF_MS = 24 * 60 * 60 * 1000L // 24 hours
        const val MAX_UNREGISTER_RETRY_WINDOW_MS = 7 * 24 * 60 * 60 * 1000L // 7 days
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private var currentBackoffMs: Long = INITIAL_BACKOFF_MS
    private var scheduledJob: Job? = null

    init {
        // Observe settings changes
        scope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                if (!settings.isDeviceCountEnabled) {
                    onDeviceCountDisabled()
                } else if (NetworkGate.isConsentGranted(context)) {
                    sendDevicePing()
                }
            }
        }
    }

    fun getOrCreateDeviceId(): String {
        var id = prefs.getString(KEY_DEVICE_ID, null)
        if (id == null) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, id).apply()
        }
        return id
    }

    fun getLocalDeviceId(): String? = prefs.getString(KEY_DEVICE_ID, null)

    fun getLastSentTimestamp(): Long = prefs.getLong(KEY_LAST_SENT_TIMESTAMP, 0L)

    fun getLastSentVersion(): String? = prefs.getString(KEY_LAST_SENT_VERSION, null)

    fun getPendingUnregisterId(): String? = prefs.getString(KEY_PENDING_UNREGISTER_ID, null)

    fun sendDevicePing() {
        scope.launch {
            executeDevicePing()
        }
    }

    suspend fun executeDevicePing(): Boolean = withContext(Dispatchers.IO) {
        // 1. Consent Gate check
        if (!NetworkGate.isConsentGranted(context)) {
            return@withContext false
        }

        // 2. Settings check
        val isEnabled = try {
            settingsRepository.settingsFlow.first().isDeviceCountEnabled
        } catch (_: Exception) {
            SettingsRepository.DEVICE_COUNT_DEFAULT_ENABLED
        }

        if (!isEnabled) {
            // Process any pending unregister request if present
            executePendingUnregister()
            return@withContext false
        }

        // 3. Current app version resolution
        val currentVersion = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.7.1"
        } catch (_: Exception) {
            "1.7.1"
        }
        val versionInfo = VersionInfo.parse(currentVersion)
        val numericVersion = versionInfo.numericVersion
        val channel = if (versionInfo.channel == UpdateChannel.BETA) "beta" else "stable"

        val lastSentTime = getLastSentTimestamp()
        val lastSentVer = getLastSentVersion()
        val isPendingRetry = prefs.getBoolean(KEY_PENDING_SEND, false)
        val now = System.currentTimeMillis()

        val isVersionChanged = lastSentVer != numericVersion
        val isIntervalElapsed = (now - lastSentTime) >= THROTTLE_INTERVAL_MS

        if (!isVersionChanged && !isIntervalElapsed && !isPendingRetry) {
            return@withContext true // Throttled, no ping needed
        }

        val deviceId = getOrCreateDeviceId()

        // 4. Construct strictly audited payload
        val payload = JSONObject().apply {
            put("p_device_id", deviceId)
            put("p_app_version", numericVersion)
            put("p_channel", channel)
        }

        var connection: HttpURLConnection? = null
        try {
            val url = URL("$supabaseUrl/rest/v1/rpc/register_device")
            val payloadBytes = payload.toString().toByteArray(Charsets.UTF_8)

            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10000
                readTimeout = 10000
                instanceFollowRedirects = true
                doOutput = true
                setRequestProperty("User-Agent", "Fotara-Android-App")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Content-Length", payloadBytes.size.toString())
                setRequestProperty("apikey", supabaseKey)
                setRequestProperty("Authorization", "Bearer $supabaseKey")
                setFixedLengthStreamingMode(payloadBytes.size)
            }

            connection.outputStream.use { out ->
                out.write(payloadBytes)
                out.flush()
            }

            val code = connection.responseCode
            if (code in 200..299) {
                prefs.edit()
                    .putLong(KEY_LAST_SENT_TIMESTAMP, now)
                    .putString(KEY_LAST_SENT_VERSION, numericVersion)
                    .putBoolean(KEY_PENDING_SEND, false)
                    .apply()
                currentBackoffMs = INITIAL_BACKOFF_MS
                return@withContext true
            } else {
                scheduleRetry()
                return@withContext false
            }
        } catch (e: Exception) {
            scheduleRetry()
            return@withContext false
        } finally {
            connection?.disconnect()
        }
    }

    private fun scheduleRetry() {
        prefs.edit().putBoolean(KEY_PENDING_SEND, true).apply()
        scheduledJob?.cancel()
        scheduledJob = scope.launch {
            delay(currentBackoffMs)
            currentBackoffMs = (currentBackoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
            executeDevicePing()
        }
    }

    fun onDeviceCountDisabled() {
        scheduledJob?.cancel()
        val existingId = prefs.getString(KEY_DEVICE_ID, null)

        // Delete local ID and stored send statistics
        prefs.edit()
            .remove(KEY_DEVICE_ID)
            .remove(KEY_LAST_SENT_TIMESTAMP)
            .remove(KEY_LAST_SENT_VERSION)
            .putBoolean(KEY_PENDING_SEND, false)
            .apply()

        // Queue best-effort unregister if an ID previously existed
        if (existingId != null) {
            prefs.edit()
                .putString(KEY_PENDING_UNREGISTER_ID, existingId)
                .putLong(KEY_PENDING_UNREGISTER_TIMESTAMP, System.currentTimeMillis())
                .apply()

            scope.launch {
                executePendingUnregister()
            }
        }
    }

    suspend fun executePendingUnregister(): Boolean = withContext(Dispatchers.IO) {
        val pendingId = prefs.getString(KEY_PENDING_UNREGISTER_ID, null) ?: return@withContext true
        val queuedTime = prefs.getLong(KEY_PENDING_UNREGISTER_TIMESTAMP, 0L)
        val now = System.currentTimeMillis()

        if (now - queuedTime > MAX_UNREGISTER_RETRY_WINDOW_MS) {
            // Drop after 7 days of failures
            prefs.edit()
                .remove(KEY_PENDING_UNREGISTER_ID)
                .remove(KEY_PENDING_UNREGISTER_TIMESTAMP)
                .apply()
            return@withContext true
        }

        if (!NetworkGate.isConsentGranted(context)) {
            return@withContext false
        }

        val payload = JSONObject().apply {
            put("p_device_id", pendingId)
        }

        var connection: HttpURLConnection? = null
        try {
            val url = URL("$supabaseUrl/rest/v1/rpc/unregister_device")
            val payloadBytes = payload.toString().toByteArray(Charsets.UTF_8)

            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10000
                readTimeout = 10000
                doOutput = true
                setRequestProperty("User-Agent", "Fotara-Android-App")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Content-Length", payloadBytes.size.toString())
                setRequestProperty("apikey", supabaseKey)
                setRequestProperty("Authorization", "Bearer $supabaseKey")
                setFixedLengthStreamingMode(payloadBytes.size)
            }

            connection.outputStream.use { out ->
                out.write(payloadBytes)
                out.flush()
            }

            val code = connection.responseCode
            if (code in 200..299 || code == 404) {
                prefs.edit()
                    .remove(KEY_PENDING_UNREGISTER_ID)
                    .remove(KEY_PENDING_UNREGISTER_TIMESTAMP)
                    .apply()
                return@withContext true
            }
            return@withContext false
        } catch (_: Exception) {
            return@withContext false
        } finally {
            connection?.disconnect()
        }
    }

    fun clearLocalDataForTesting() {
        prefs.edit().clear().apply()
    }
}
