// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.friends

import android.content.SharedPreferences
import com.arinara.fotara.feature.friends.data.LocalFriendsRepository
import com.arinara.fotara.feature.friends.data.UsernameManager
import com.arinara.fotara.feature.friends.model.StudyPresenceStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap

class FriendsRepositoryTest {

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

    private lateinit var prefs: TestSharedPreferences
    private lateinit var repository: LocalFriendsRepository

    private fun createTestUsernameManager(): UsernameManager {
        return object : UsernameManager(prefs) {
            override suspend fun checkSupabaseAvailability(cleanUsername: String): Result<Boolean> = Result.success(true)
            override suspend fun upsertToSupabase(uuid: String, cleanUsername: String, displayName: String, timestamp: Long): Result<Unit> = Result.success(Unit)
            override suspend fun fetchProfileByUsername(username: String): Result<com.arinara.fotara.feature.friends.data.SupabaseUserProfile?> {
                return if (username == "alex_remote") {
                    Result.success(com.arinara.fotara.feature.friends.data.SupabaseUserProfile("remote-id", "uuid", "alex_remote", "Remote Alex", 1000L))
                } else {
                    Result.success(null)
                }
            }
        }
    }


    @Before
    fun setUp() {
        prefs = TestSharedPreferences()
        repository = LocalFriendsRepository(prefs, createTestUsernameManager())
    }

    @Test
    fun testInitialEmptyFriendsList() = runTest {
        val friends = repository.getFriendsFlow().first()
        assertTrue(friends.isEmpty())
    }

    @Test
    fun testAddFriendSuccess() = runTest {
        val result = repository.addFriend("@alex_m", "Alex Miller")
        assertTrue(result.isSuccess)

        val added = result.getOrNull()
        assertNotNull(added)
        assertEquals("@alex_m", added!!.handle)
        assertEquals("Alex Miller", added.displayName)
        assertEquals(StudyPresenceStatus.OFFLINE, added.studyStatus)

        val currentList = repository.getFriendsFlow().first()
        assertEquals(1, currentList.size)
        assertEquals(added.id, currentList[0].id)
    }

    @Test
    fun testAddFriendFormatsHandleWithLeadingAt() = runTest {
        val result = repository.addFriend("sarah_k", "Sarah Kim")
        assertTrue(result.isSuccess)
        assertEquals("@sarah_k", result.getOrNull()?.handle)
    }

    @Test
    fun testAddFriendDuplicateHandleFails() = runTest {
        repository.addFriend("@alex_m", "Alex Miller")
        val duplicateResult = repository.addFriend("@alex_m", "Alex Duplicate")

        assertFalse(duplicateResult.isSuccess)
        assertTrue(duplicateResult.exceptionOrNull() is IllegalStateException)

        val list = repository.getFriendsFlow().first()
        assertEquals(1, list.size)
    }

    @Test
    fun testAddFriendInvalidHandleValidation() = runTest {
        val emptyHandle = repository.addFriend("", "No Handle")
        assertFalse(emptyHandle.isSuccess)

        val spacesHandle = repository.addFriend("   ", "Spaces Handle")
        assertFalse(spacesHandle.isSuccess)

        val invalidChars = repository.addFriend("@invalid!user#", "Invalid Chars")
        assertFalse(invalidChars.isSuccess)
    }

    @Test
    fun testAddFriendEmptyDisplayNameAutoResolves() = runTest {
        val autoResolved = repository.addFriend("@alex", "")
        assertTrue(autoResolved.isSuccess)
        val friend = autoResolved.getOrNull()
        assertNotNull(friend)
        assertEquals("Alex", friend!!.displayName)
    }

    @Test
    fun testRemoveFriend() = runTest {
        val f1 = repository.addFriend("@alex", "Alex").getOrThrow()
        val f2 = repository.addFriend("@sarah", "Sarah").getOrThrow()

        assertEquals(2, repository.getFriendsFlow().first().size)

        repository.removeFriend(f1.id)

        val afterRemove = repository.getFriendsFlow().first()
        assertEquals(1, afterRemove.size)
        assertEquals(f2.id, afterRemove[0].id)
    }

    @Test
    fun testUpdatePresence() = runTest {
        assertEquals(StudyPresenceStatus.OFFLINE, repository.getMyPresenceStatusFlow().first())

        repository.updatePresence(StudyPresenceStatus.STUDYING, "Organic Chemistry")

        assertEquals(StudyPresenceStatus.STUDYING, repository.getMyPresenceStatusFlow().first())
        assertEquals("Organic Chemistry", repository.getMySubjectFlow().first())

        repository.updatePresence(StudyPresenceStatus.IN_LECTURE, null)
        assertEquals(StudyPresenceStatus.IN_LECTURE, repository.getMyPresenceStatusFlow().first())
        assertEquals(null, repository.getMySubjectFlow().first())
    }

    @Test
    fun testGetMyHandle() {
        val handle = repository.getMyHandle()
        assertTrue(handle.startsWith("@"))
        assertEquals("@scholar", handle)
    }

    @Test
    fun testGenerateShareCode() {
        val code = repository.generateShareCode()
        assertNotNull(code)
        assertTrue(code.startsWith("FOTARA-FRIEND-scholar-"))
    }

    @Test
    fun testToggleFavorite() = runTest {
        val friend = repository.addFriend("@alex", "Alex").getOrThrow()
        assertFalse(friend.isFavorite)

        repository.toggleFavorite(friend.id)
        val updated = repository.getFriendsFlow().first().first { it.id == friend.id }
        assertTrue(updated.isFavorite)

        repository.toggleFavorite(friend.id)
        val reverted = repository.getFriendsFlow().first().first { it.id == friend.id }
        assertFalse(reverted.isFavorite)
    }

    @Test
    fun testPersistenceAcrossRepositoryInstances() = runTest {
        repository.addFriend("@alex", "Alex Miller")
        repository.addFriend("@jordan", "Jordan Baker")
        repository.updatePresence(StudyPresenceStatus.OPEN_TO_COLLAB, "Calculus III")

        // Instantiate second repository with same preferences backing
        val newRepoInstance = LocalFriendsRepository(prefs, createTestUsernameManager())
        val loadedFriends = newRepoInstance.getFriendsFlow().first()

        assertEquals(2, loadedFriends.size)
        assertEquals("@alex", loadedFriends[0].handle)
        assertEquals("@jordan", loadedFriends[1].handle)
        assertEquals(StudyPresenceStatus.OPEN_TO_COLLAB, newRepoInstance.getMyPresenceStatusFlow().first())
        assertEquals("Calculus III", newRepoInstance.getMySubjectFlow().first())
    }

    @Test
    fun testFollowersInitialEmpty() = runTest {
        val followers = repository.getFollowersFlow().first()
        assertEquals(0, followers.size)
        assertTrue(followers.isEmpty())
    }

    @Test
    fun testFollowingInitialEmpty() = runTest {
        val following = repository.getFollowingFlow().first()
        assertEquals(0, following.size)
        assertTrue(following.isEmpty())
    }

    @Test
    fun testFollowUserAddsToFollowing() = runTest {
        val newPeer = com.arinara.fotara.feature.friends.model.FriendProfile(
            id = "peer-99",
            handle = "@casey_r",
            displayName = "Casey Rivera"
        )
        repository.followUser(newPeer)

        val following = repository.getFollowingFlow().first()
        assertEquals(1, following.size)
        assertTrue(following.any { it.handle == "@casey_r" })

        // Adding duplicate should not duplicate
        repository.followUser(newPeer)
        assertEquals(1, repository.getFollowingFlow().first().size)
    }

    @Test
    fun testUnfollowUserRemovesFromFollowing() = runTest {
        val newPeer = com.arinara.fotara.feature.friends.model.FriendProfile(
            id = "peer-99",
            handle = "@casey_r",
            displayName = "Casey Rivera"
        )
        repository.followUser(newPeer)
        assertEquals(1, repository.getFollowingFlow().first().size)

        repository.unfollowUser("peer-99")
        val afterUnfollow = repository.getFollowingFlow().first()
        assertEquals(0, afterUnfollow.size)
        assertFalse(afterUnfollow.any { it.id == "peer-99" })
    }

    @Test
    fun testRemoveFollowerRemovesFromFollowers() = runTest {
        val testFollower = com.arinara.fotara.feature.friends.model.FriendProfile(
            id = "follower-real-1",
            handle = "@real_peer",
            displayName = "Real Peer"
        )
        prefs.edit().putString(
            LocalFriendsRepository.KEY_FOLLOWERS_LIST,
            testFollower.toJson()
        ).apply()

        val repoWithFollower = LocalFriendsRepository(prefs, createTestUsernameManager())
        val initialFollowers = repoWithFollower.getFollowersFlow().first()
        assertEquals(1, initialFollowers.size)

        repoWithFollower.removeFollower("follower-real-1")
        val afterRemove = repoWithFollower.getFollowersFlow().first()
        assertEquals(0, afterRemove.size)
    }

    @Test
    fun testClaimUsernameUpdatesHandleAndCooldown() = runTest {
        assertEquals("@scholar", repository.getClaimedUsername())
        assertEquals("@scholar", repository.getMyHandle())
        assertEquals(0, repository.getUsernameCooldownDays())
        assertTrue(repository.canChangeUsername().first)

        val claimResult = repository.claimUsername("alex_scholar", "Alex Scholar")
        assertTrue(claimResult.isSuccess)
        assertEquals("@alex_scholar", claimResult.getOrNull())

        assertEquals("@alex_scholar", repository.getClaimedUsername())
        assertEquals("@alex_scholar", repository.getMyHandle())
        assertEquals(7, repository.getUsernameCooldownDays())
        assertFalse(repository.canChangeUsername().first)
    }

    @Test
    fun testFindUserByHandleLocal() = runTest {
        repository.addFriend("@charlie", "Charlie Brown")
        val found = repository.findUserByHandle("@charlie")
        assertNotNull(found)
        assertEquals("@charlie", found!!.handle)
        assertEquals("Charlie Brown", found.displayName)
    }

    @Test
    fun testFindUserByHandleRemoteSupabase() = runTest {
        val found = repository.findUserByHandle("@alex_remote")
        assertNotNull(found)
        assertEquals("@alex_remote", found!!.handle)
        assertEquals("Remote Alex", found.displayName)
    }

    @Test
    fun testFindUserByHandleFormatFallback() = runTest {
        val found = repository.findUserByHandle("@some_peer")
        assertNotNull(found)
        assertEquals("@some_peer", found!!.handle)
        assertEquals("Some_peer", found.displayName)
    }

    @Test
    fun testAddFriendAutoResolvesDisplayNameFromRemote() = runTest {
        val result = repository.addFriend("@alex_remote", "")
        assertTrue(result.isSuccess)
        val friend = result.getOrNull()
        assertNotNull(friend)
        assertEquals("Remote Alex", friend!!.displayName)
    }
}

