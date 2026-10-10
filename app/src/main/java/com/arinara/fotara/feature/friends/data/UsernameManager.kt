// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.friends.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

open class UsernameManager(
    private val prefs: SharedPreferences,
    private val supabaseUrl: String = SUPABASE_URL,
    private val supabaseKey: String = SUPABASE_KEY,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )

    companion object {
        const val PREFS_NAME = "fotara_username_prefs"
        const val KEY_CLAIMED_USERNAME = "key_claimed_username"
        const val KEY_LAST_CLAIM_TIMESTAMP = "key_last_claim_timestamp"
        const val KEY_INSTALL_UUID = "key_install_uuid"

        const val DEFAULT_USERNAME = "@scholar"
        const val COOLDOWN_DAYS = 7
        const val COOLDOWN_MS = 7 * 24 * 60 * 60 * 1000L

        const val SUPABASE_URL = "https://nrvnhbizyvubcdqvzabv.supabase.co"
        const val SUPABASE_KEY = "sb_publishable_Fn-s-CleInVH76uvH7c9aQ_QnQoMrrs"

        private val USERNAME_REGEX = "^[a-z0-9_]{3,20}$".toRegex()
    }

    fun getInstallUuid(): String {
        val existing = prefs.getString(KEY_INSTALL_UUID, null)
        if (!existing.isNullOrBlank()) {
            return existing
        }
        val generated = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_INSTALL_UUID, generated).apply()
        return generated
    }

    fun getClaimedUsername(): String {
        val raw = prefs.getString(KEY_CLAIMED_USERNAME, null)
        if (raw.isNullOrBlank()) {
            return DEFAULT_USERNAME
        }
        return if (raw.startsWith("@")) raw else "@$raw"
    }

    fun getRemainingCooldownDays(): Int {
        val lastClaimTime = prefs.getLong(KEY_LAST_CLAIM_TIMESTAMP, 0L)
        if (lastClaimTime <= 0L) return 0
        val now = System.currentTimeMillis()
        val elapsed = now - lastClaimTime
        if (elapsed >= COOLDOWN_MS) return 0
        val remainingMs = (COOLDOWN_MS - elapsed).coerceAtLeast(0L)
        val dayMs = 24 * 60 * 60 * 1000L
        val days = ((remainingMs + dayMs - 1) / dayMs).toInt()
        return days.coerceIn(1, COOLDOWN_DAYS)
    }

    fun canChangeUsername(): Pair<Boolean, String?> {
        val remainingDays = getRemainingCooldownDays()
        return if (remainingDays > 0) {
            false to "Username can only be changed once every 7 days. Cooldown active for $remainingDays more days."
        } else {
            true to null
        }
    }

    fun validateUsernameFormat(username: String): Pair<Boolean, String?> {
        val clean = cleanUsername(username)
        val isValid = clean.matches(USERNAME_REGEX)
        return if (isValid) {
            true to null
        } else {
            false to "Username must be 3-20 characters using letters, numbers, or underscores."
        }
    }

    fun cleanUsername(username: String): String {
        return username.trim().removePrefix("@").lowercase()
    }

    open suspend fun checkSupabaseAvailability(cleanUsername: String): Result<Boolean> = withContext(ioDispatcher) {
        var connection: HttpURLConnection? = null
        try {
            val endpoint = "$supabaseUrl/rest/v1/user_profiles?username=eq.$cleanUsername&select=id,install_uuid"
            val url = URL(endpoint)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("User-Agent", "Fotara-Android-App")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("apikey", supabaseKey)
                setRequestProperty("Authorization", "Bearer $supabaseKey")
            }

            val code = connection.responseCode
            if (code in 200..299) {
                val responseText = connection.inputStream?.bufferedReader()?.use { it.readText() } ?: "[]"
                val jsonArray = JSONArray(responseText)
                val myInstallUuid = getInstallUuid()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.optJSONObject(i) ?: continue
                    val otherInstallUuid = obj.optString("install_uuid", "")
                    if (otherInstallUuid.isNotEmpty() && otherInstallUuid != myInstallUuid) {
                        return@withContext Result.success(false)
                    }
                }
                Result.success(true)
            } else {
                // Network error or unexpected HTTP status: gracefully allow (offline resilience)
                Result.success(true)
            }
        } catch (_: Exception) {
            // Gracefully handle offline / connection failure
            Result.success(true)
        } finally {
            connection?.disconnect()
        }
    }

    open suspend fun upsertToSupabase(
        uuid: String,
        cleanUsername: String,
        displayName: String,
        timestamp: Long
    ): Result<Unit> = withContext(ioDispatcher) {
        var connection: HttpURLConnection? = null
        try {
            val endpoint = "$supabaseUrl/rest/v1/user_profiles"
            val url = URL(endpoint)
            val payloadObj = JSONObject().apply {
                put("id", uuid)
                put("install_uuid", uuid)
                put("username", cleanUsername)
                put("display_name", displayName)
                put("updated_at", timestamp)
            }
            val payloadBytes = payloadObj.toString().toByteArray(Charsets.UTF_8)
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
                setRequestProperty("Prefer", "resolution=merge-duplicates")
                setFixedLengthStreamingMode(payloadBytes.size)
            }

            connection.outputStream.use { out ->
                out.write(payloadBytes)
                out.flush()
            }

            val code = connection.responseCode
            Result.success(Unit)
        } catch (_: Exception) {
            // Offline resilience: gracefully ignore remote upsert error and rely on local storage
            Result.success(Unit)
        } finally {
            connection?.disconnect()
        }
    }

    open suspend fun claimUsername(requestedUsername: String, displayName: String): Result<String> {
        // 1) Validate format
        val (isValid, formatError) = validateUsernameFormat(requestedUsername)
        if (!isValid) {
            return Result.failure(IllegalArgumentException(formatError ?: "Invalid username format"))
        }

        val clean = cleanUsername(requestedUsername)

        // 2) Validate cooldown
        val (canChange, cooldownError) = canChangeUsername()
        if (!canChange) {
            return Result.failure(IllegalStateException(cooldownError ?: "Username cooldown is active"))
        }

        // 3) Check Supabase availability
        val availabilityResult = checkSupabaseAvailability(clean)
        val isAvailable = availabilityResult.getOrDefault(true)
        if (!isAvailable) {
            return Result.failure(IllegalStateException("Username @$clean is already taken by another student."))
        }

        // 4) Upsert to Supabase
        val uuid = getInstallUuid()
        val now = System.currentTimeMillis()
        val trimmedDisplayName = displayName.trim().ifEmpty { clean }
        upsertToSupabase(
            uuid = uuid,
            cleanUsername = clean,
            displayName = trimmedDisplayName,
            timestamp = now
        )

        // 5) Persist locally
        val claimedHandle = "@$clean"
        prefs.edit()
            .putString(KEY_CLAIMED_USERNAME, claimedHandle)
            .putLong(KEY_LAST_CLAIM_TIMESTAMP, now)
            .apply()

        return Result.success(claimedHandle)
    }
}
