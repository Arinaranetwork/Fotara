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

    private val registeredDirectoryUsers = mapOf(
        "alex_remote" to "Remote Alex",
        "alex_m" to "Alex Miller",
        "sarah_k" to "Sarah Kim",
        "alex" to "Alex",
        "sarah" to "Sarah",
        "jordan" to "Jordan Baker",
        "charlie" to "Charlie Brown",
        "device_two" to "Device Two"
    )

    private fun createTestUsernameManager(targetPrefs: SharedPreferences = prefs): UsernameManager {
        return object : UsernameManager(targetPrefs) {
            override suspend fun checkSupabaseAvailability(cleanUsername: String): Result<Boolean> = Result.success(true)
            override suspend fun upsertToSupabase(uuid: String, cleanUsername: String, displayName: String, timestamp: Long): Result<Unit> = Result.success(Unit)
            override suspend fun fetchProfileByUsername(username: String): Result<com.arinara.fotara.feature.friends.data.SupabaseUserProfile?> {
                val clean = username.trim().removePrefix("@").lowercase()
                val displayName = registeredDirectoryUsers[clean]
                return if (displayName != null) {
                    Result.success(
                        com.arinara.fotara.feature.friends.data.SupabaseUserProfile(
                            id = "remote-$clean",
                            installUuid = "uuid-$clean",
                            username = clean,
                            displayName = displayName,
                            updatedAt = 1000L
                        )
                    )
                } else {
                    Result.success(null)
                }
            }
        }
    }


    @Before
    fun setUp() {
        com.arinara.fotara.feature.friends.data.FriendPeerRegistry.clearAllForTesting()
        prefs = TestSharedPreferences()
        repository = LocalFriendsRepository(prefs, createTestUsernameManager(), enableLanTransport = false)
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
        assertEquals("", handle)
    }

    @Test
    fun testGenerateShareCode() {
        val code = repository.generateShareCode()
        assertNotNull(code)
        assertTrue(code.startsWith("FOTARA-FRIEND-"))
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
        val newRepoInstance = LocalFriendsRepository(prefs, createTestUsernameManager(), enableLanTransport = false)
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

        val repoWithFollower = LocalFriendsRepository(prefs, createTestUsernameManager(), enableLanTransport = false)
        val initialFollowers = repoWithFollower.getFollowersFlow().first()
        assertEquals(1, initialFollowers.size)

        repoWithFollower.removeFollower("follower-real-1")
        val afterRemove = repoWithFollower.getFollowersFlow().first()
        assertEquals(0, afterRemove.size)
    }

    @Test
    fun testClaimUsernameUpdatesHandleAndCooldown() = runTest {
        assertEquals("", repository.getClaimedUsername())
        assertEquals("", repository.getMyHandle())
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
    fun testFindUserByHandleNonExistentReturnsNull() = runTest {
        val found = repository.findUserByHandle("@nonexistent_ghost_user")
        org.junit.Assert.assertNull(found)
    }

    @Test
    fun testAddFriendNonExistentUsernameFails() = runTest {
        val result = repository.addFriend("@nonexistent_ghost_user", "Ghost User")
        assertFalse(result.isSuccess)
        val msg = result.exceptionOrNull()?.message.orEmpty()
        assertTrue(msg.contains("does not exist", ignoreCase = true))
        assertTrue(repository.getFriendsFlow().first().isEmpty())
    }

    @Test
    fun testAddSelfUsernameFails() = runTest {
        repository.claimUsername("my_own_handle", "Me")
        val result = repository.addFriend("@my_own_handle", "Me")
        assertFalse(result.isSuccess)
        val msg = result.exceptionOrNull()?.message.orEmpty()
        assertTrue(msg.contains("own username", ignoreCase = true))
        assertTrue(repository.getFriendsFlow().first().isEmpty())
    }

    @Test
    fun testAddFriendMalformedShareCodeFails() = runTest {
        val result = repository.addFriend("FOTARA-FRIEND--BROKEN", "")
        assertFalse(result.isSuccess)
        assertTrue(repository.getFriendsFlow().first().isEmpty())
    }

    @Test
    fun testRealUserTwoDeviceSimulationEndToEnd() = runTest {
        val prefsA = TestSharedPreferences()
        val prefsB = TestSharedPreferences()
        val repoA = LocalFriendsRepository(prefsA, createTestUsernameManager(prefsA), enableLanTransport = false)
        val repoB = LocalFriendsRepository(prefsB, createTestUsernameManager(prefsB), enableLanTransport = false)

        // 1. Both users claim their usernames and set display names
        assertTrue(repoA.claimUsername("alice_study", "Alice Chen").isSuccess)
        assertTrue(repoB.claimUsername("bob_notes", "Bob Martinez").isSuccess)
        assertEquals("@alice_study", repoA.getMyHandle())
        assertEquals("@bob_notes", repoB.getMyHandle())

        // 2. Bob goes online: STUDYING "Organic Chemistry II"
        repoB.updatePresence(StudyPresenceStatus.STUDYING, "Organic Chemistry II")
        repoA.updatePresence(StudyPresenceStatus.OPEN_TO_COLLAB, "Physics Lab")

        // 3. Alice tries to add a username that does NOT exist -> must fail
        val nonExistentAttempt = repoA.addFriend("@nobody_exists_404", "")
        assertFalse(nonExistentAttempt.isSuccess)
        assertTrue(repoA.getFriendsFlow().first().isEmpty())

        // 4. Alice searches for Bob (@bob_notes) -> discovers Bob with live online status & subject
        val discoveredBob = repoA.findUserByHandle("@bob_notes")
        assertNotNull(discoveredBob)
        assertEquals("@bob_notes", discoveredBob!!.handle)
        assertEquals("Bob Martinez", discoveredBob.displayName)
        assertEquals(StudyPresenceStatus.STUDYING, discoveredBob.studyStatus)
        assertEquals("Organic Chemistry II", discoveredBob.currentSubject)

        // 5. Alice adds Bob as a friend -> Bob is added with live online status, and Bob receives Alice's connect event
        val addBobResult = repoA.addFriend("@bob_notes", "")
        assertTrue(addBobResult.isSuccess)
        val aliceFriends = repoA.getFriendsFlow().first()
        assertEquals(1, aliceFriends.size)
        assertEquals("@bob_notes", aliceFriends[0].handle)
        assertEquals("Bob Martinez", aliceFriends[0].displayName)
        assertEquals(StudyPresenceStatus.STUDYING, aliceFriends[0].studyStatus)
        assertEquals("Organic Chemistry II", aliceFriends[0].currentSubject)
        assertTrue(aliceFriends[0].isActive)

        // Bob automatically sees Alice in his followers and friends list with her live status!
        val bobFollowers = repoB.getFollowersFlow().first()
        assertEquals(1, bobFollowers.size)
        assertEquals("@alice_study", bobFollowers[0].handle)
        assertEquals("Alice Chen", bobFollowers[0].displayName)
        assertEquals(StudyPresenceStatus.OPEN_TO_COLLAB, bobFollowers[0].studyStatus)
        assertEquals("Physics Lab", bobFollowers[0].currentSubject)

        val bobFriends = repoB.getFriendsFlow().first()
        assertEquals(1, bobFriends.size)
        assertEquals("@alice_study", bobFriends[0].handle)
        assertTrue(bobFriends[0].isActive)

        // 6. Bob changes his presence to OPEN_TO_COLLAB ("Final Exam Prep") -> Alice sees update immediately
        repoB.updatePresence(StudyPresenceStatus.OPEN_TO_COLLAB, "Final Exam Prep")
        val aliceFriendsAfterUpdate = repoA.getFriendsFlow().first()
        assertEquals(StudyPresenceStatus.OPEN_TO_COLLAB, aliceFriendsAfterUpdate[0].studyStatus)
        assertEquals("Final Exam Prep", aliceFriendsAfterUpdate[0].currentSubject)

        // 7. Bob goes OFFLINE -> Alice sees Bob transition to OFFLINE
        repoB.updatePresence(StudyPresenceStatus.OFFLINE, null)
        val aliceFriendsOffline = repoA.getFriendsFlow().first()
        assertEquals(StudyPresenceStatus.OFFLINE, aliceFriendsOffline[0].studyStatus)
        assertFalse(aliceFriendsOffline[0].isActive)

        repoA.close()
        repoB.close()
    }

    @Test
    fun testAddFriendAutoResolvesDisplayNameFromRemote() = runTest {
        val result = repository.addFriend("@alex_remote", "")
        assertTrue(result.isSuccess)
        val friend = result.getOrNull()
        assertNotNull(friend)
        assertEquals("Remote Alex", friend!!.displayName)
    }

    @Test
    fun testMultiDeviceLanPresenceUpdatesFriendOnlineStatusAndSubject() = runTest {
        val added = repository.addFriend("@device_two", "").getOrThrow()
        assertEquals(StudyPresenceStatus.OFFLINE, added.studyStatus)

        // Simulate receiving a live LAN presence packet from Device 2
        com.arinara.fotara.feature.friends.data.FriendPeerRegistry.recordPeer(
            com.arinara.fotara.feature.friends.model.FriendProfile(
                id = "dev2-uuid",
                handle = "@device_two",
                displayName = "Device Two Scholar",
                studyStatus = StudyPresenceStatus.STUDYING,
                currentSubject = "Quantum Mechanics"
            )
        )

        val liveFriends = repository.getFriendsFlow().first()
        assertEquals(1, liveFriends.size)
        val liveFriend = liveFriends[0]
        assertEquals("@device_two", liveFriend.handle)
        assertEquals("Device Two Scholar", liveFriend.displayName)
        assertEquals(StudyPresenceStatus.STUDYING, liveFriend.studyStatus)
        assertEquals("Quantum Mechanics", liveFriend.currentSubject)
    }

    @Test
    fun testAddFriendWithShareCodeDecodesLiveMetadata() = runTest {
        val shareCode = com.arinara.fotara.feature.friends.data.FriendShareCodeCodec.encode(
            com.arinara.fotara.feature.friends.model.FriendProfile(
                id = "maya-id",
                handle = "@maya_lin",
                displayName = "Maya Lin",
                studyStatus = StudyPresenceStatus.OPEN_TO_COLLAB,
                currentSubject = "Linear Algebra"
            )
        )
        val result = repository.addFriend(shareCode, "")
        assertTrue(result.isSuccess)
        val friend = result.getOrThrow()
        assertEquals("@maya_lin", friend.handle)
        assertEquals("Maya Lin", friend.displayName)
        assertEquals(StudyPresenceStatus.OPEN_TO_COLLAB, friend.studyStatus)
        assertEquals("Linear Algebra", friend.currentSubject)
    }
}

