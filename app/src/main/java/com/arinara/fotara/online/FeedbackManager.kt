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

    private fun getDailySubmissionCount(): Int {
        val today = System.currentTimeMillis() / (24 * 60 * 60 * 1000L)
        val recordedDay = prefs.getLong("submission_day", 0L)
        if (today != recordedDay) {
            prefs.edit().putLong("submission_day", today).putInt("daily_count", 0).apply()
            return 0
        }
        return prefs.getInt("daily_count", 0)
    }

    fun getRemainingDailyQuota(): Int {
        return (30 - getDailySubmissionCount()).coerceAtLeast(0)
    }

    private fun incrementSubmissionCount() {
        val today = System.currentTimeMillis() / (24 * 60 * 60 * 1000L)
        val count = getDailySubmissionCount() + 1
        prefs.edit().putLong("submission_day", today).putInt("daily_count", count).apply()
    }

    fun canSubmit(): Pair<Boolean, String?> {
        val count = getDailySubmissionCount()
        if (count >= 30) {
            return false to "Batas harian pengiriman masukan tercapai (30/hari). Terima kasih telah membantu mengembangkan Fotara!"
        }
        val lastSubmittedAt = prefs.getLong("last_submitted_at", 0L)
        val elapsed = System.currentTimeMillis() - lastSubmittedAt
        if (elapsed < 3000L) {
            val remainSec = ((3000L - elapsed) / 1000L).coerceAtLeast(1)
            return false to "Harap tunggu $remainSec detik sebelum mengirim masukan berikutnya."
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
            return@withContext FeedbackSubmissionResult(false, error ?: "Tidak dapat mengirim saat ini")
        }

        if (content.trim().isBlank()) {
            return@withContext FeedbackSubmissionResult(false, "Isi masukan tidak boleh kosong.")
        }

        val appVersion = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pInfo.longVersionCode else @Suppress("DEPRECATION") pInfo.versionCode.toLong()
            "Fotara v${pInfo.versionName} (Build $code)"
        } catch (_: Exception) {
            "Fotara v1.4.0"
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

        // 1. Persist locally first so no user input is ever lost
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
            if (code in 200..299) {
                networkSuccess = true
                markAsSyncedInLocalQueue(feedbackId)
                incrementSubmissionCount()
                resultMessage = "Terima kasih! Masukan Anda telah berhasil dikirim ke server dan diteruskan ke tim pengembang."
            } else {
                val errorBody = try {
                    connection.errorStream?.bufferedReader()?.use { it.readText() }
                } catch (_: Exception) { null }
                Log.e("FeedbackManager", "Supabase error HTTP $code: $errorBody")
                if (code == 429) {
                    resultMessage = "Batas frekuensi server tercapai (10 masukan/jam). Masukan tersimpan di antrean perangkat."
                } else {
                    resultMessage = "Server merespons kode $code. Masukan Anda tersimpan di perangkat dan akan disinkronkan otomatis."
                }
            }
        } catch (e: Exception) {
            Log.e("FeedbackManager", "Failed to connect to Supabase: ${e.message}", e)
            networkSuccess = false
            resultMessage = "Tidak dapat terhubung ke server (${e.message ?: "Koneksi terputus"}). Masukan Anda tersimpan aman di perangkat dan akan dikirim saat online."
        } finally {
            connection?.disconnect()
        }

        // 3. Flush any older unsynced queue items if network succeeded
        if (networkSuccess) {
            flushLocalQueue()
        }

        FeedbackSubmissionResult(networkSuccess, resultMessage)
    }

    suspend fun flushLocalQueue(): Int = withContext(Dispatchers.IO) {
        var syncedCount = 0
        try {
            val currentQueueStr = prefs.getString("feedback_queue", "[]") ?: "[]"
            val array = JSONArray(currentQueueStr)
            val updatedArray = JSONArray()

            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val isSynced = item.optBoolean("synced", false)
                if (isSynced) {
                    updatedArray.put(item)
                    continue
                }

                // Attempt to send pending item
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

                    if (conn.responseCode in 200..299) {
                        success = true
                        item.put("synced", true)
                        syncedCount++
                    }
                } catch (e: Exception) {
                    Log.e("FeedbackManager", "Error flushing queued feedback item: ${e.message}")
                } finally {
                    conn?.disconnect()
                }

                updatedArray.put(item)
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
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i)
                if (obj != null && obj.optString("id") == feedbackId) {
                    obj.put("synced", true)
                    break
                }
            }
            prefs.edit().putString("feedback_queue", array.toString()).apply()
        } catch (_: Exception) {}
    }
}
