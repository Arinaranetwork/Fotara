// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.friends

import android.content.SharedPreferences
import com.arinara.fotara.feature.friends.data.UsernameManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap

@OptIn(ExperimentalCoroutinesApi::class)
class UsernameManagerTest {

    private class TestSharedPreferences : SharedPreferences {
        private val data = ConcurrentHashMap<String, Any>()

        override fun getAll(): MutableMap<String, *> = data
        override fun getString(key: String, defValue: String?): String? = data[key] as? String ?: defValue
        override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? = data[key] as? Set<String> ?: defValues
        override fun getInt(key: String, defValue: Int): Int = data[key] as? Int ?: defValue
        override fun getLong(key: String, defValue: Long): Long = data[key] as? Long ?: defValue
        override fun getFloat(key: String, defValue: Float): Float = data[key] as? Float ?: defValue
        override fun getBoolean(key: String, defValue: Boolean): Boolean = data[key] as? Boolean ?: defValue
        override fun contains(key: String): Boolean = data.containsKey(key)
        override fun edit(): SharedPreferences.Editor = Editor()
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        inner class Editor : SharedPreferences.Editor {
            private val temp = mutableMapOf<String, Any?>()
            private var clear = false

            override fun putString(key: String, value: String?): SharedPreferences.Editor { temp[key] = value; return this }
            override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor { temp[key] = values; return this }
            override fun putInt(key: String, value: Int): SharedPreferences.Editor { temp[key] = value; return this }
            override fun putLong(key: String, value: Long): SharedPreferences.Editor { temp[key] = value; return this }
            override fun putFloat(key: String, value: Float): SharedPreferences.Editor { temp[key] = value; return this }
            override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor { temp[key] = value; return this }
            override fun remove(key: String): SharedPreferences.Editor { temp[key] = null; return this }
            override fun clear(): SharedPreferences.Editor { clear = true; return this }
            override fun commit(): Boolean { apply(); return true }
            override fun apply() {
                if (clear) data.clear()
                for ((k, v) in temp) {
                    if (v == null) data.remove(k) else data[k] = v
                }
            }
        }
    }

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var prefs: TestSharedPreferences
    private lateinit var usernameManager: UsernameManager

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        prefs = TestSharedPreferences()
        usernameManager = object : UsernameManager(prefs, ioDispatcher = testDispatcher) {
            override suspend fun checkSupabaseAvailability(cleanUsername: String): Result<Boolean> {
                return Result.success(true)
            }
            override suspend fun upsertToSupabase(
                uuid: String,
                cleanUsername: String,
                displayName: String,
                timestamp: Long
            ): Result<Unit> {
                return Result.success(Unit)
            }
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testDefaultClaimedUsername() {
        assertEquals("@scholar", usernameManager.getClaimedUsername())
    }

    @Test
    fun testInstallUuidPersistence() {
        val uuid1 = usernameManager.getInstallUuid()
        assertNotNull(uuid1)
        assertTrue(uuid1.isNotEmpty())

        val uuid2 = usernameManager.getInstallUuid()
        assertEquals(uuid1, uuid2)
    }

    @Test
    fun testValidateUsernameFormatValidCases() {
        val validCases = listOf("scholar", "@scholar", "jordan_b", "alex123", "student_42_xyz", "abc")
        for (candidate in validCases) {
            val (isValid, errorMsg) = usernameManager.validateUsernameFormat(candidate)
            assertTrue("Expected valid for $candidate", isValid)
            assertEquals(null, errorMsg)
        }
    }

    @Test
    fun testValidateUsernameFormatInvalidCases() {
        val invalidCases = listOf(
            "",
            "@",
            "ab", // Too short (< 3)
            "this_is_way_too_long_for_a_username_25", // Too long (> 20)
            "alex-smith", // Hyphen disallowed
            "alex smith", // Space disallowed
            "alex@scholar", // @ inside disallowed
            "alex!special" // Special char disallowed
        )
        for (candidate in invalidCases) {
            val (isValid, errorMsg) = usernameManager.validateUsernameFormat(candidate)
            assertFalse("Expected invalid for $candidate", isValid)
            assertEquals("Username must be 3-20 characters using letters, numbers, or underscores.", errorMsg)
        }
    }

    @Test
    fun testInitialCooldownIsInactive() {
        assertEquals(0, usernameManager.getRemainingCooldownDays())
        val (canChange, errorMsg) = usernameManager.canChangeUsername()
        assertTrue(canChange)
        assertEquals(null, errorMsg)
    }

    @Test
    fun testCooldownActiveCalculation() {
        val now = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L

        // Just claimed now -> 7 days remaining
        prefs.edit().putLong(UsernameManager.KEY_LAST_CLAIM_TIMESTAMP, now - 1000L).apply()
        assertEquals(7, usernameManager.getRemainingCooldownDays())
        val (canChangeNow, msgNow) = usernameManager.canChangeUsername()
        assertFalse(canChangeNow)
        assertEquals("Username can only be changed once every 7 days. Cooldown active for 7 more days.", msgNow)

        // Claimed 3 days ago -> 4 days remaining
        prefs.edit().putLong(UsernameManager.KEY_LAST_CLAIM_TIMESTAMP, now - (3 * dayMs)).apply()
        assertEquals(4, usernameManager.getRemainingCooldownDays())
        val (canChange3d, msg3d) = usernameManager.canChangeUsername()
        assertFalse(canChange3d)
        assertEquals("Username can only be changed once every 7 days. Cooldown active for 4 more days.", msg3d)

        // Claimed 6.5 days ago -> 1 day remaining
        prefs.edit().putLong(UsernameManager.KEY_LAST_CLAIM_TIMESTAMP, now - (6 * dayMs + 12 * 3600 * 1000L)).apply()
        assertEquals(1, usernameManager.getRemainingCooldownDays())
        val (canChange6d, msg6d) = usernameManager.canChangeUsername()
        assertFalse(canChange6d)
        assertEquals("Username can only be changed once every 7 days. Cooldown active for 1 more days.", msg6d)

        // Claimed 7.1 days ago -> Cooldown expired
        prefs.edit().putLong(UsernameManager.KEY_LAST_CLAIM_TIMESTAMP, now - (7 * dayMs + 3600 * 1000L)).apply()
        assertEquals(0, usernameManager.getRemainingCooldownDays())
        val (canChangeExpired, msgExpired) = usernameManager.canChangeUsername()
        assertTrue(canChangeExpired)
        assertEquals(null, msgExpired)
    }

    @Test
    fun testClaimUsernameInvalidFormatFails() = runTest(testDispatcher) {
        val result = usernameManager.claimUsername("ab", "Alex")
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("Username must be 3-20 characters using letters, numbers, or underscores.", result.exceptionOrNull()?.message)
    }

    @Test
    fun testClaimUsernameSuccessOfflineResilient() = runTest(testDispatcher) {
        val result = usernameManager.claimUsername("jordan_b", "Jordan Baker")
        assertTrue(result.isSuccess)
        assertEquals("@jordan_b", result.getOrNull())
        assertEquals("@jordan_b", usernameManager.getClaimedUsername())

        // Cooldown is now active
        assertEquals(7, usernameManager.getRemainingCooldownDays())
        assertFalse(usernameManager.canChangeUsername().first)
    }

    @Test
    fun testClaimUsernameCooldownActiveFails() = runTest(testDispatcher) {
        // Claim first time
        val firstClaim = usernameManager.claimUsername("alex_w", "Alex Wong")
        assertTrue(firstClaim.isSuccess)

        // Attempting to claim again while cooldown is active
        val secondClaim = usernameManager.claimUsername("alex_2", "Alex Wong")
        assertFalse(secondClaim.isSuccess)
        assertTrue(secondClaim.exceptionOrNull() is IllegalStateException)
        assertTrue(secondClaim.exceptionOrNull()?.message?.contains("Cooldown active for") == true)
    }

    @Test
    fun testClaimUsernameTakenByAnotherStudentFails() = runTest(testDispatcher) {
        val customManager = object : UsernameManager(prefs, ioDispatcher = testDispatcher) {
            override suspend fun checkSupabaseAvailability(cleanUsername: String): Result<Boolean> {
                return Result.success(false)
            }
        }

        val result = customManager.claimUsername("sarah_k", "Sarah Kim")
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertEquals("Username @sarah_k is already taken by another student.", result.exceptionOrNull()?.message)
    }
}
