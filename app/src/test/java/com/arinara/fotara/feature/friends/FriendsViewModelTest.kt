// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.friends

import android.content.SharedPreferences
import com.arinara.fotara.feature.friends.data.LocalFriendsRepository
import com.arinara.fotara.feature.friends.data.UsernameManager
import com.arinara.fotara.feature.friends.model.FriendProfile
import com.arinara.fotara.feature.friends.ui.FriendsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap

@OptIn(ExperimentalCoroutinesApi::class)
class FriendsViewModelTest {

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
    private lateinit var repository: LocalFriendsRepository
    private lateinit var viewModel: FriendsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        prefs = TestSharedPreferences()
        val testUsernameManager = object : UsernameManager(prefs, ioDispatcher = testDispatcher) {
            override suspend fun checkSupabaseAvailability(cleanUsername: String): Result<Boolean> = Result.success(true)
            override suspend fun upsertToSupabase(uuid: String, cleanUsername: String, displayName: String, timestamp: Long): Result<Unit> = Result.success(Unit)
        }
        repository = LocalFriendsRepository(prefs, testUsernameManager)
        viewModel = FriendsViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialUiState() = runTest(testDispatcher) {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals("@scholar", state.claimedUsername)
        assertEquals("@scholar", state.myHandle)
        assertTrue(state.canChangeUsername)
        assertEquals(0, state.cooldownDaysRemaining)
        assertEquals(3, state.followers.size)
        assertEquals(3, state.followersCount)
        assertEquals(2, state.following.size)
        assertEquals(2, state.followingCount)
    }

    @Test
    fun testFollowAndUnfollowUser() = runTest(testDispatcher) {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()
        val newPeer = FriendProfile(id = "user-100", handle = "@new_peer", displayName = "New Peer")
        viewModel.followUser(newPeer)
        advanceUntilIdle()

        val stateAfterFollow = viewModel.uiState.value
        assertEquals(3, stateAfterFollow.following.size)
        assertEquals(3, stateAfterFollow.followingCount)
        assertTrue(stateAfterFollow.following.any { it.id == "user-100" })

        viewModel.unfollowUser("user-100")
        advanceUntilIdle()

        val stateAfterUnfollow = viewModel.uiState.value
        assertEquals(2, stateAfterUnfollow.following.size)
        assertEquals(2, stateAfterUnfollow.followingCount)
        assertFalse(stateAfterUnfollow.following.any { it.id == "user-100" })
    }

    @Test
    fun testRemoveFollower() = runTest(testDispatcher) {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()
        val followerToRemove = viewModel.uiState.value.followers.first()
        viewModel.removeFollower(followerToRemove.id)
        advanceUntilIdle()

        val stateAfterRemove = viewModel.uiState.value
        assertEquals(2, stateAfterRemove.followers.size)
        assertEquals(2, stateAfterRemove.followersCount)
        assertFalse(stateAfterRemove.followers.any { it.id == followerToRemove.id })
    }

    @Test
    fun testClaimUsernameSuccessUpdatesUiState() = runTest(testDispatcher) {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()
        var claimSuccess = false
        var resultMessage = ""
        viewModel.claimUsername("master_scholar", "Master Scholar") { success, msg ->
            claimSuccess = success
            resultMessage = msg
        }
        advanceUntilIdle()

        assertTrue(claimSuccess)
        assertTrue(resultMessage.contains("@master_scholar"))

        val state = viewModel.uiState.value
        assertEquals("@master_scholar", state.claimedUsername)
        assertEquals("@master_scholar", state.myHandle)
        assertEquals(7, state.cooldownDaysRemaining)
        assertFalse(state.canChangeUsername)
    }

    @Test
    fun testClaimUsernameInvalidFormatFails() = runTest(testDispatcher) {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()
        var claimSuccess = true
        var errorMessage = ""
        viewModel.claimUsername("ab", "Alex") { success, msg ->
            claimSuccess = success
            errorMessage = msg
        }
        advanceUntilIdle()

        assertFalse(claimSuccess)
        assertEquals("Username must be 3-20 characters using letters, numbers, or underscores.", errorMessage)
    }
}
