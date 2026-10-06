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

enum class FeedbackCategory(val dbValue: String, val displayName: String) {
    BUG_REPORT("Bug Report", "Bug Report"),
    SUGGESTION("Suggestion", "Suggestion"),
    FEATURE_IDEA("Feature Idea", "Feature Idea"),
    GENERAL("General", "General");

    companion object {
        fun fromString(value: String): FeedbackCategory {
            return entries.firstOrNull {
                it.name.equals(value, ignoreCase = true) ||
                it.dbValue.equals(value, ignoreCase = true) ||
                it.displayName.equals(value, ignoreCase = true)
            } ?: GENERAL
        }
    }
}

data class FeedbackSubmissionResult(
    val success: Boolean,
    val message: String,
    val technicalDetails: String? = null,
    val isRateLimited: Boolean = false,
    val isQueuedOffline: Boolean = false
)

data class ParsedFeedbackError(
    val isRateLimit: Boolean,
    val isPermanentClientError: Boolean,
    val userFriendlyMessage: String,
    val technicalDetails: String
)

object FeedbackPayloadBuilder {
    fun buildPayload(
        id: String,
        installUuid: String,
        category: FeedbackCategory,
        content: String,
        email: String?,
        diagnosticInfo: String?,
        submittedAt: Long = System.currentTimeMillis()
    ): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("uuid", installUuid)
            put("category", category.dbValue)
            put("content", content.trim().take(5000))
            if (!email.isNullOrBlank()) {
                put("email", email.trim().take(255))
            }
            if (!diagnosticInfo.isNullOrBlank()) {
                put("diagnostic_info", diagnosticInfo.trim().take(2000))
            }
            put("submitted_at", submittedAt)
        }
    }

    fun parseResponse(code: Int, responseBody: String): ParsedFeedbackError {
        val trimmedBody = responseBody.trim()
        val isTriggerRateLimit = code == 400 && (
            trimmedBody.contains("P0001", ignoreCase = true) ||
            trimmedBody.contains("Rate limit", ignoreCase = true) ||
            trimmedBody.contains("rate limit", ignoreCase = true)
        )
        val isHttp429 = code == 429
        val isRateLimit = isTriggerRateLimit || isHttp429

        if (isRateLimit) {
            val friendlyMsg = if (trimmedBody.contains("60 seconds", ignoreCase = true)) {
                "Rate limit active. Please wait 60 seconds between submissions. Your feedback is safely stored on device and will sync automatically."
            } else {
                "Daily submission limit reached (5 submissions per 24 hours). Your feedback is safely stored on device and will sync when the next slot opens."
            }
            return ParsedFeedbackError(
                isRateLimit = true,
                isPermanentClientError = false,
                userFriendlyMessage = friendlyMsg,
                technicalDetails = "HTTP $code (Rate limit trigger): ${trimmedBody.ifBlank { "Rate limit exceeded" }}"
            )
        }

        if (code in 400..499) {
            val isSchemaCheck = trimmedBody.contains("violates check constraint", ignoreCase = true) ||
                    trimmedBody.contains("invalid input syntax", ignoreCase = true) ||
                    trimmedBody.contains("null value in column", ignoreCase = true)
            val friendly = if (isSchemaCheck) {
                "The server rejected the submission format. Please check your input and try again."
            } else {
                "The server could not process the submission request ($code)."
            }
            return ParsedFeedbackError(
                isRateLimit = false,
                isPermanentClientError = true,
                userFriendlyMessage = friendly,
                technicalDetails = "HTTP $code (Client Error): ${trimmedBody.ifBlank { "Bad Request" }}"
            )
        }

        if (code in 500..599) {
            return ParsedFeedbackError(
                isRateLimit = false,
                isPermanentClientError = false,
                userFriendlyMessage = "The feedback server is temporarily unavailable. Your feedback is saved on device and will sync when online.",
                technicalDetails = "HTTP $code (Server Error): ${trimmedBody.ifBlank { "Internal Server Error" }}"
            )
        }

        return ParsedFeedbackError(
            isRateLimit = false,
            isPermanentClientError = false,
            userFriendlyMessage = "Unexpected server response ($code). Your feedback is saved locally.",
            technicalDetails = "HTTP $code: $trimmedBody"
        )
    }
}

class FeedbackManager(private val context: Context) {

    companion object {
        const val FEEDBACK_DAILY_LIMIT = 5
        const val FEEDBACK_COOLDOWN_SECONDS = 60L
        const val FEEDBACK_COOLDOWN_MS = FEEDBACK_COOLDOWN_SECONDS * 1000L
        const val FEEDBACK_ROLLING_WINDOW_MS = 24 * 60 * 60 * 1000L

        // Backward compatibility constants
        const val DAILY_LIMIT = FEEDBACK_DAILY_LIMIT
        const val COOLDOWN_SECONDS = FEEDBACK_COOLDOWN_SECONDS.toInt()
        const val COOLDOWN_MS = FEEDBACK_COOLDOWN_MS
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
            if (com.arinara.fotara.legal.NetworkGate.isConsentGranted(context)) {
                flushLocalQueue()
            }
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
                if (ts > now - FEEDBACK_ROLLING_WINDOW_MS) {
                    list.add(ts)
                }
            }
        } catch (_: Exception) {}
        return list
    }

    fun getRemainingDailyQuota(): Int {
        val count = getRecentSubmissionTimestamps().size
        return (FEEDBACK_DAILY_LIMIT - count).coerceAtLeast(0)
    }

    fun getTimeUntilNextSlotMs(): Long {
        val timestamps = getRecentSubmissionTimestamps()
        if (timestamps.size < FEEDBACK_DAILY_LIMIT) return 0L
        val oldestInWindow = timestamps.minOrNull() ?: return 0L
        val now = System.currentTimeMillis()
        val nextSlotTime = oldestInWindow + FEEDBACK_ROLLING_WINDOW_MS
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
        return if (elapsed < FEEDBACK_COOLDOWN_MS) {
            (((FEEDBACK_COOLDOWN_MS - elapsed) + 999) / 1000).toInt().coerceAtLeast(1)
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
            return false to "Daily submission limit reached ($FEEDBACK_DAILY_LIMIT per 24 hours). Next slot opens in $timeText."
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
        if (!com.arinara.fotara.legal.NetworkGate.isConsentGranted(context)) {
            return@withContext FeedbackSubmissionResult(
                success = false,
                message = "Consent required before submitting feedback."
            )
        }
        val (allowed, error) = canSubmit()
        if (!allowed) {
            return@withContext FeedbackSubmissionResult(
                success = false,
                message = error ?: "Cannot submit feedback at this time.",
                technicalDetails = null,
                isRateLimited = getRemainingDailyQuota() <= 0 || getCooldownRemainingSeconds() > 0
            )
        }

        if (content.trim().isBlank()) {
            return@withContext FeedbackSubmissionResult(
                success = false,
                message = "Feedback content cannot be empty."
            )
        }

        val appVersion = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pInfo.longVersionCode else @Suppress("DEPRECATION") pInfo.versionCode.toLong()
            val info = VersionInfo.parse(pInfo.versionName)
            "Fotara ${info.displayVersion} (${info.channelLabel}) (Build $code)"
        } catch (_: Exception) {
            "Fotara v1.6.0 (Stable) (Build 26)"
        }

        val diagnosticInfo = if (includeDiagnostics) {
            "$appVersion | Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT}) | ${Build.MANUFACTURER} ${Build.MODEL}"
        } else null

        val feedbackId = UUID.randomUUID().toString()
        val payload = FeedbackPayloadBuilder.buildPayload(
            id = feedbackId,
            installUuid = getInstallUuid(),
            category = category,
            content = content,
            email = email,
            diagnosticInfo = diagnosticInfo,
            submittedAt = System.currentTimeMillis()
        )

        // 1. Save locally first so user feedback is never lost
        saveToLocalQueue(payload)
        prefs.edit().putLong("last_submitted_at", System.currentTimeMillis()).apply()

        // 2. Perform direct remote sync to Supabase PostgREST endpoint
        var networkSuccess = false
        var resultMessage: String
        var technicalDetails: String? = null
        var isRateLimited = false
        var isQueuedOffline = false
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
                val parsed = FeedbackPayloadBuilder.parseResponse(code, responseText)
                resultMessage = parsed.userFriendlyMessage
                technicalDetails = parsed.technicalDetails

                if (parsed.isRateLimit) {
                    isRateLimited = true
                    isQueuedOffline = true
                    recordSubmissionTimestamp()
                } else if (parsed.isPermanentClientError) {
                    // Permanent client error: drop from local queue so it does not retry endlessly
                    removeFromLocalQueue(feedbackId)
                } else {
                    // Server 5xx error: keep in queue for future retry
                    isQueuedOffline = true
                }
                Log.e("FeedbackManager", "Supabase HTTP $code: $responseText")
            }
        } catch (e: Exception) {
            Log.e("FeedbackManager", "Network connection error: ${e.message}", e)
            networkSuccess = false
            isQueuedOffline = true
            resultMessage = "Unable to connect to server (${e.message ?: "Connection lost"}). Your feedback is saved locally and will send when online."
            technicalDetails = "Network Error: ${e.javaClass.simpleName} - ${e.message}"
        } finally {
            connection?.disconnect()
        }

        FeedbackSubmissionResult(
            success = networkSuccess,
            message = resultMessage,
            technicalDetails = technicalDetails,
            isRateLimited = isRateLimited,
            isQueuedOffline = isQueuedOffline
        )
    }

    suspend fun flushLocalQueue(): Int = withContext(Dispatchers.IO) {
        if (!com.arinara.fotara.legal.NetworkGate.isConsentGranted(context)) {
            return@withContext 0
        }
        var syncedCount = 0
        try {
            // Check daily quota and cooldown before flushing
            if (getRemainingDailyQuota() <= 0) {
                return@withContext 0
            }
            if (getCooldownRemainingSeconds() > 0) {
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
                    continue // Purge already synced items
                }

                // Flush strictly ONE item at a time to respect server pacing
                if (sentThisRound || getRemainingDailyQuota() <= 0 || getCooldownRemainingSeconds() > 0) {
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
                    } else {
                        val parsed = FeedbackPayloadBuilder.parseResponse(code, resp)
                        if (parsed.isPermanentClientError) {
                            // Drop permanently malformed item from queue
                            Log.w("FeedbackManager", "Dropping malformed queued feedback item: $resp")
                            continue
                        } else if (parsed.isRateLimit) {
                            // Rate limit reached: pause flush and retain item
                            sentThisRound = true
                        }
                    }
                } catch (e: Exception) {
                    Log.e("FeedbackManager", "Error flushing queued feedback item: ${e.message}")
                    sentThisRound = true
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
