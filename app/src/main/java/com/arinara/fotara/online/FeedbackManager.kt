// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.online

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

enum class FeedbackCategory(val displayName: String) {
    BUG_REPORT("Bug Report"),
    SUGGESTION("Suggestion"),
    FEATURE_IDEA("Feature Idea"),
    GENERAL("General")
}

data class FeedbackSubmissionResult(
    val success: Boolean,
    val message: String
)

class FeedbackManager(private val context: Context) {

    companion object {
        const val DAILY_LIMIT = 5
        const val COOLDOWN_SECONDS = 60
        const val COOLDOWN_MS = COOLDOWN_SECONDS * 1000L
        private const val ROLLING_WINDOW_MS = 24 * 60 * 60 * 1000L
    }

    private val prefs by lazy {
        context.getSharedPreferences("fotara_feedback", Context.MODE_PRIVATE)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Supabase configuration
    private val supabaseUrl = "https://nrvnhbizyvubcdqvzabv.supabase.co"
    private val supabaseKey = "sb_publishable_Fn-s-CleInVH76uvH7c9aQ_QnQoMrrs"

    init {
        // Opportunistically flush pending offline queue on init
        scope.launch {
            flushLocalQueue()
        }
    }

    fun getInstallUuid(): String {
        var uuid = prefs.getString("install_uuid", null)
        if (uuid == null) {
            uuid = UUID.randomUUID().toString()
            prefs.edit().putString("install_uuid", uuid).apply()
        }
        return uuid
    }

    private fun getRecentSubmissionTimestamps(): List<Long> {
        val now = System.currentTimeMillis()
        val raw = prefs.getString("submission_timestamps", "[]") ?: "[]"
        val list = mutableListOf<Long>()
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val ts = array.optLong(i, 0L)
                if (ts > now - ROLLING_WINDOW_MS) {
                    list.add(ts)
                }
            }
        } catch (_: Exception) {}
        return list
    }

    fun getRemainingDailyQuota(): Int {
        val count = getRecentSubmissionTimestamps().size
        return (DAILY_LIMIT - count).coerceAtLeast(0)
    }

    fun getTimeUntilNextSlotMs(): Long {
        val timestamps = getRecentSubmissionTimestamps()
        if (timestamps.size < DAILY_LIMIT) return 0L
        val oldestInWindow = timestamps.minOrNull() ?: return 0L
        val now = System.currentTimeMillis()
        val nextSlotTime = oldestInWindow + ROLLING_WINDOW_MS
        return (nextSlotTime - now).coerceAtLeast(0L)
    }

    private fun recordSubmissionTimestamp() {
        val now = System.currentTimeMillis()
        val valid = getRecentSubmissionTimestamps().toMutableList()
        valid.add(now)
        val array = JSONArray()
        valid.forEach { array.put(it) }
        prefs.edit().putString("submission_timestamps", array.toString()).apply()
    }

    fun getCooldownRemainingSeconds(): Int {
        val lastSubmittedAt = prefs.getLong("last_submitted_at", 0L)
        val elapsed = System.currentTimeMillis() - lastSubmittedAt
        return if (elapsed < COOLDOWN_MS) {
            (((COOLDOWN_MS - elapsed) + 999) / 1000).toInt().coerceAtLeast(1)
        } else {
            0
        }
    }

    fun canSubmit(): Pair<Boolean, String?> {
        val remainingQuota = getRemainingDailyQuota()
        if (remainingQuota <= 0) {
            val nextSlotMs = getTimeUntilNextSlotMs()
            val hours = nextSlotMs / (60 * 60 * 1000L)
            val minutes = (nextSlotMs % (60 * 60 * 1000L)) / (60 * 1000L)
            val timeText = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
            return false to "Daily submission limit reached ($DAILY_LIMIT per 24 hours). Next slot opens in $timeText."
        }
        val cooldownSec = getCooldownRemainingSeconds()
        if (cooldownSec > 0) {
            return false to "Please wait $cooldownSec seconds before submitting your next feedback."
        }
        return true to null
    }

    suspend fun submitFeedback(
        category: FeedbackCategory,
        content: String,
        email: String?,
        includeDiagnostics: Boolean
    ): FeedbackSubmissionResult = withContext(Dispatchers.IO) {
        val (allowed, error) = canSubmit()
        if (!allowed) {
            return@withContext FeedbackSubmissionResult(false, error ?: "Cannot submit feedback at this time.")
        }

        if (content.trim().isBlank()) {
            return@withContext FeedbackSubmissionResult(false, "Feedback content cannot be empty.")
        }

        val appVersion = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pInfo.longVersionCode else @Suppress("DEPRECATION") pInfo.versionCode.toLong()
            "Fotara v${pInfo.versionName} (Build $code)"
        } catch (_: Exception) {
            "Fotara v1.5.0"
        }

        val diagnosticInfo = if (includeDiagnostics) {
            "$appVersion | Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT}) | ${Build.MANUFACTURER} ${Build.MODEL}"
        } else null

        val feedbackId = UUID.randomUUID().toString()
        val payload = JSONObject().apply {
            put("id", feedbackId)
            put("uuid", getInstallUuid())
            put("category", category.name)
            put("content", content.trim())
            if (!email.isNullOrBlank()) put("email", email.trim())
            if (diagnosticInfo != null) put("diagnostic_info", diagnosticInfo)
            put("submitted_at", System.currentTimeMillis())
        }

        // 1. Save locally first so user work is never lost
        saveToLocalQueue(payload)
        prefs.edit().putLong("last_submitted_at", System.currentTimeMillis()).apply()

        // 2. Perform direct remote sync to Supabase
        var networkSuccess = false
        var resultMessage: String
        var connection: HttpURLConnection? = null

        try {
            val url = URL("$supabaseUrl/rest/v1/suggestions")
            val payloadBytes = payload.toString().toByteArray(Charsets.UTF_8)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 15000
                instanceFollowRedirects = true
                doOutput = true
                setRequestProperty("User-Agent", "Fotara-Android-App")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Content-Length", payloadBytes.size.toString())
                setRequestProperty("apikey", supabaseKey)
                setRequestProperty("Authorization", "Bearer $supabaseKey")
                setRequestProperty("Prefer", "return=minimal")
                setFixedLengthStreamingMode(payloadBytes.size)
            }

            connection.outputStream.use { out ->
                out.write(payloadBytes)
                out.flush()
            }

            val code = connection.responseCode
            val responseText = try {
                if (code in 200..299) {
                    connection.inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                }
            } catch (_: Exception) { "" }

            if (code in 200..299) {
                networkSuccess = true
                markAsSyncedInLocalQueue(feedbackId)
                recordSubmissionTimestamp()
                resultMessage = "Thank you! Your feedback has been successfully sent to the developers."
            } else {
                val isRateLimit = code == 429 ||
                        responseText.contains("Rate limit", ignoreCase = true) ||
                        responseText.contains("P0001", ignoreCase = true)

                if (isRateLimit) {
                    recordSubmissionTimestamp()
                    resultMessage = "Server rate limit reached. Your feedback is safely stored on this device and will sync automatically."
                } else if (code in 400..499) {
                    // Permanent client error (e.g. malformed or rejected) - remove from queue so it doesn't loop
                    removeFromLocalQueue(feedbackId)
                    resultMessage = "The server rejected the submission format ($code). Details: $responseText"
                } else {
                    // Server 5xx error
                    resultMessage = "Server unavailable ($code). Your feedback is safely queued on device and will sync when online."
                }
                Log.e("FeedbackManager", "Supabase HTTP $code: $responseText")
            }
        } catch (e: Exception) {
            Log.e("FeedbackManager", "Network connection error: ${e.message}", e)
            networkSuccess = false
            resultMessage = "Unable to connect to server (${e.message ?: "Connection lost"}). Your feedback is saved locally and will send when online."
        } finally {
            connection?.disconnect()
        }

        FeedbackSubmissionResult(networkSuccess, resultMessage)
    }

    suspend fun flushLocalQueue(): Int = withContext(Dispatchers.IO) {
        var syncedCount = 0
        try {
            // Check daily quota before flushing
            if (getRemainingDailyQuota() <= 0) {
                return@withContext 0
            }

            val currentQueueStr = prefs.getString("feedback_queue", "[]") ?: "[]"
            val array = JSONArray(currentQueueStr)
            val updatedArray = JSONArray()

            var sentThisRound = false

            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val isSynced = item.optBoolean("synced", false)
                if (isSynced) {
                    continue // Purge already synced items from queue
                }

                // Flush only one item at a time to respect server pacing
                if (sentThisRound || getRemainingDailyQuota() <= 0) {
                    updatedArray.put(item)
                    continue
                }

                var success = false
                var conn: HttpURLConnection? = null
                try {
                    val sendPayload = JSONObject(item.toString()).apply {
                        remove("synced")
                    }
                    val sendBytes = sendPayload.toString().toByteArray(Charsets.UTF_8)
                    val url = URL("$supabaseUrl/rest/v1/suggestions")
                    conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        connectTimeout = 10000
                        readTimeout = 10000
                        instanceFollowRedirects = true
                        doOutput = true
                        setRequestProperty("User-Agent", "Fotara-Android-App")
                        setRequestProperty("Accept", "application/json")
                        setRequestProperty("Content-Type", "application/json; charset=utf-8")
                        setRequestProperty("Content-Length", sendBytes.size.toString())
                        setRequestProperty("apikey", supabaseKey)
                        setRequestProperty("Authorization", "Bearer $supabaseKey")
                        setRequestProperty("Prefer", "return=minimal")
                        setFixedLengthStreamingMode(sendBytes.size)
                    }

                    conn.outputStream.use { out ->
                        out.write(sendBytes)
                        out.flush()
                    }

                    val code = conn.responseCode
                    val resp = try {
                        if (code in 200..299) conn.inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                        else conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    } catch (_: Exception) { "" }

                    if (code in 200..299) {
                        success = true
                        recordSubmissionTimestamp()
                        prefs.edit().putLong("last_submitted_at", System.currentTimeMillis()).apply()
                        syncedCount++
                        sentThisRound = true
                    } else if (code in 400..499 && !resp.contains("Rate limit", ignoreCase = true) && !resp.contains("P0001")) {
                        // Permanent client error: drop item so queue is not poisoned
                        Log.w("FeedbackManager", "Dropping malformed queued item: $resp")
                        continue
                    }
                } catch (e: Exception) {
                    Log.e("FeedbackManager", "Error flushing queued feedback item: ${e.message}")
                } finally {
                    conn?.disconnect()
                }

                if (!success) {
                    updatedArray.put(item)
                }
            }

            prefs.edit().putString("feedback_queue", updatedArray.toString()).apply()
        } catch (_: Exception) {}
        syncedCount
    }

    private fun saveToLocalQueue(item: JSONObject) {
        try {
            val currentQueueStr = prefs.getString("feedback_queue", "[]") ?: "[]"
            val array = JSONArray(currentQueueStr)
            item.put("synced", false)
            array.put(item)
            prefs.edit().putString("feedback_queue", array.toString()).apply()
        } catch (_: Exception) {}
    }

    private fun markAsSyncedInLocalQueue(feedbackId: String) {
        try {
            val currentQueueStr = prefs.getString("feedback_queue", "[]") ?: "[]"
            val array = JSONArray(currentQueueStr)
            val updated = JSONArray()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                if (obj.optString("id") == feedbackId) {
                    // Mark synced or omit
                    obj.put("synced", true)
                }
                updated.put(obj)
            }
            prefs.edit().putString("feedback_queue", updated.toString()).apply()
        } catch (_: Exception) {}
    }

    private fun removeFromLocalQueue(feedbackId: String) {
        try {
            val currentQueueStr = prefs.getString("feedback_queue", "[]") ?: "[]"
            val array = JSONArray(currentQueueStr)
            val updated = JSONArray()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                if (obj.optString("id") != feedbackId) {
                    updated.put(obj)
                }
            }
            prefs.edit().putString("feedback_queue", updated.toString()).apply()
        } catch (_: Exception) {}
    }
}
