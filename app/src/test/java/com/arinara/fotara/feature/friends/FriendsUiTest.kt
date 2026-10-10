// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.friends

import com.arinara.fotara.feature.friends.model.FriendProfile
import com.arinara.fotara.feature.friends.model.StudyPresenceStatus
import com.arinara.fotara.feature.friends.ui.FriendsTab
import com.arinara.fotara.feature.friends.ui.FriendsUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FriendsUiTest {

    @Test
    fun testSettingsCardSubtitleComputation() {
        fun computeSubtitle(activeFriendsCount: Int, totalFriendsCount: Int): String {
            return when {
                activeFriendsCount > 0 && totalFriendsCount > 0 ->
                    "$activeFriendsCount online • $totalFriendsCount friends"
                totalFriendsCount > 0 ->
                    "$totalFriendsCount friends"
                else ->
                    "Connect with friends, share notes, study presence"
            }
        }

        // Empty state
        assertEquals(
            "Connect with friends, share notes, study presence",
            computeSubtitle(0, 0)
        )

        // Only offline contacts
        assertEquals(
            "3 friends",
            computeSubtitle(0, 3)
        )

        // Online contacts available
        assertEquals(
            "2 online • 5 friends",
            computeSubtitle(2, 5)
        )
    }

    @Test
    fun testPresenceFilteringAndActiveSeparation() {
        val friend1 = FriendProfile("1", "@alex", "Alex", studyStatus = StudyPresenceStatus.STUDYING)
        val friend2 = FriendProfile("2", "@beth", "Beth", studyStatus = StudyPresenceStatus.OPEN_TO_COLLAB)
        val friend3 = FriendProfile("3", "@carl", "Carl", studyStatus = StudyPresenceStatus.OFFLINE)
        val friend4 = FriendProfile("4", "@dina", "Dina", studyStatus = StudyPresenceStatus.IN_LECTURE)

        val all = listOf(friend1, friend2, friend3, friend4)
        val active = all.filter { it.isActive }
        val offline = all.filter { !it.isActive }

        assertEquals(3, active.size)
        assertEquals(1, offline.size)
        assertTrue(active.contains(friend1))
        assertTrue(active.contains(friend2))
        assertTrue(active.contains(friend4))
        assertEquals(friend3, offline.first())
    }

    @Test
    fun testSearchQueryFilter() {
        val friend1 = FriendProfile("1", "@alex", "Alexander Hamilton")
        val friend2 = FriendProfile("2", "@beth", "Elizabeth Schuyler")
        val friend3 = FriendProfile("3", "@aaron", "Aaron Burr")

        val list = listOf(friend1, friend2, friend3)

        val queryA = "alex"
        val filteredA = list.filter {
            it.displayName.contains(queryA, ignoreCase = true) || it.handle.contains(queryA, ignoreCase = true)
        }
        assertEquals(1, filteredA.size)
        assertEquals("@alex", filteredA.first().handle)

        val queryHandle = "burr"
        val filteredBurr = list.filter {
            it.displayName.contains(queryHandle, ignoreCase = true) || it.handle.contains(queryHandle, ignoreCase = true)
        }
        assertEquals(1, filteredBurr.size)
        assertEquals("Aaron Burr", filteredBurr.first().displayName)
    }

    @Test
    fun testUiStateActiveAndOfflineFriendsComputation() {
        val friend1 = FriendProfile("1", "@active1", "Active One", studyStatus = StudyPresenceStatus.STUDYING)
        val friend2 = FriendProfile("2", "@active2", "Active Two", studyStatus = StudyPresenceStatus.OPEN_TO_COLLAB)
        val friend3 = FriendProfile("3", "@offline1", "Offline One", studyStatus = StudyPresenceStatus.OFFLINE)

        val state = FriendsUiState(
            friends = listOf(friend1, friend2, friend3)
        )

        assertEquals(3, state.totalFriendsCount)
        assertEquals(2, state.activeFriendsCount)
        assertEquals(2, state.activeFriends.size)
        assertEquals(1, state.offlineFriends.size)
        assertEquals(friend3, state.offlineFriends.first())
    }

    @Test
    fun testFollowersAndFollowingStateFiltering() {
        val follower1 = FriendProfile("f1", "@peer1", "Peer One")
        val follower2 = FriendProfile("f2", "@peer2", "Peer Two")
        val following1 = FriendProfile("f2", "@peer2", "Peer Two")

        val state = FriendsUiState(
            followers = listOf(follower1, follower2),
            following = listOf(following1),
            followersCount = 2,
            followingCount = 1
        )

        assertEquals(2, state.followersCount)
        assertEquals(1, state.followingCount)

        // Check if follower is also followed
        val isFollower1Following = state.following.any { it.id == follower1.id }
        val isFollower2Following = state.following.any { it.id == follower2.id }

        assertFalse(isFollower1Following)
        assertTrue(isFollower2Following)
    }

    @Test
    fun testAvatarInitialsComputation() {
        fun computeInitials(displayName: String): String {
            return displayName
                .split(" ")
                .mapNotNull { it.firstOrNull()?.toString() }
                .take(2)
                .joinToString("")
                .uppercase()
        }

        assertEquals("AH", computeInitials("Alexander Hamilton"))
        assertEquals("B", computeInitials("Beth"))
        assertEquals("JD", computeInitials("John Doe Junior"))
        assertEquals("", computeInitials(""))
    }

    @Test
    fun testFriendsTabValues() {
        val tabs = FriendsTab.entries
        assertEquals(3, tabs.size)
        assertEquals(FriendsTab.FRIENDS, tabs[0])
        assertEquals("Friends", tabs[0].label)
        assertEquals(FriendsTab.FOLLOWERS, tabs[1])
        assertEquals("Followers", tabs[1].label)
        assertEquals(FriendsTab.FOLLOWING, tabs[2])
        assertEquals("Following", tabs[2].label)
    }
}
