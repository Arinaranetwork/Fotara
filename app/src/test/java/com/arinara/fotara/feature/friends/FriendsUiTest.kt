// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.friends

import com.arinara.fotara.feature.friends.model.FriendProfile
import com.arinara.fotara.feature.friends.model.StudyPresenceStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FriendsUiTest {

    @Test
    fun testSettingsCardSubtitleComputation() {
        fun computeSubtitle(activeBuddiesCount: Int, totalBuddiesCount: Int): String {
            return when {
                activeBuddiesCount > 0 && totalBuddiesCount > 0 ->
                    "$activeBuddiesCount online • $totalBuddiesCount study contacts"
                totalBuddiesCount > 0 ->
                    "$totalBuddiesCount study contacts"
                else ->
                    "Connect with classmates, share notes, study presence"
            }
        }

        // Empty state
        assertEquals(
            "Connect with classmates, share notes, study presence",
            computeSubtitle(0, 0)
        )

        // Only offline contacts
        assertEquals(
            "3 study contacts",
            computeSubtitle(0, 3)
        )

        // Online contacts available
        assertEquals(
            "2 online • 5 study contacts",
            computeSubtitle(2, 5)
        )
    }

    @Test
    fun testPresenceFilteringAndActiveSeparation() {
        val buddy1 = FriendProfile("1", "@alex", "Alex", studyStatus = StudyPresenceStatus.STUDYING)
        val buddy2 = FriendProfile("2", "@beth", "Beth", studyStatus = StudyPresenceStatus.OPEN_TO_COLLAB)
        val buddy3 = FriendProfile("3", "@carl", "Carl", studyStatus = StudyPresenceStatus.OFFLINE)
        val buddy4 = FriendProfile("4", "@dina", "Dina", studyStatus = StudyPresenceStatus.IN_LECTURE)

        val all = listOf(buddy1, buddy2, buddy3, buddy4)
        val active = all.filter { it.isActive }
        val offline = all.filter { !it.isActive }

        assertEquals(3, active.size)
        assertEquals(1, offline.size)
        assertTrue(active.contains(buddy1))
        assertTrue(active.contains(buddy2))
        assertTrue(active.contains(buddy4))
        assertEquals(buddy3, offline.first())
    }

    @Test
    fun testSearchQueryFilter() {
        val buddy1 = FriendProfile("1", "@alex", "Alexander Hamilton")
        val buddy2 = FriendProfile("2", "@beth", "Elizabeth Schuyler")
        val buddy3 = FriendProfile("3", "@aaron", "Aaron Burr")

        val list = listOf(buddy1, buddy2, buddy3)

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
}
