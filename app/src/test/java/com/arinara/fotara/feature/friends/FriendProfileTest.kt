// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.friends

import com.arinara.fotara.feature.friends.model.FriendProfile
import com.arinara.fotara.feature.friends.model.StudyPresenceStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FriendProfileTest {

    @Test
    fun testDefaultValues() {
        val profile = FriendProfile(
            id = "test-1",
            handle = "@scholar",
            displayName = "Alex Scholar"
        )

        assertEquals("test-1", profile.id)
        assertEquals("@scholar", profile.handle)
        assertEquals("Alex Scholar", profile.displayName)
        assertNull(profile.avatarUrl)
        assertEquals(StudyPresenceStatus.OFFLINE, profile.studyStatus)
        assertNull(profile.currentSubject)
        assertFalse(profile.isFavorite)
        assertFalse(profile.isActive)
        assertEquals(0, profile.followersCount)
        assertEquals(0, profile.followingCount)
    }

    @Test
    fun testStudyPresenceStatusEnum() {
        assertEquals("Offline", StudyPresenceStatus.OFFLINE.displayLabel)
        assertEquals("Studying", StudyPresenceStatus.STUDYING.displayLabel)
        assertEquals("In Lecture", StudyPresenceStatus.IN_LECTURE.displayLabel)
        assertEquals("Open to Collab", StudyPresenceStatus.OPEN_TO_COLLAB.displayLabel)
        assertEquals(4, StudyPresenceStatus.entries.size)
    }

    @Test
    fun testFormattedHandle() {
        val withAt = FriendProfile("1", "@alex", "Alex")
        val withoutAt = FriendProfile("2", "sarah", "Sarah")

        assertEquals("@alex", withAt.formattedHandle)
        assertEquals("@sarah", withoutAt.formattedHandle)
    }

    @Test
    fun testIsActiveFlag() {
        val offline = FriendProfile("1", "@alex", "Alex", studyStatus = StudyPresenceStatus.OFFLINE)
        val studying = FriendProfile("2", "@alex", "Alex", studyStatus = StudyPresenceStatus.STUDYING)
        val inLecture = FriendProfile("3", "@alex", "Alex", studyStatus = StudyPresenceStatus.IN_LECTURE)
        val collab = FriendProfile("4", "@alex", "Alex", studyStatus = StudyPresenceStatus.OPEN_TO_COLLAB)

        assertFalse(offline.isActive)
        assertTrue(studying.isActive)
        assertTrue(inLecture.isActive)
        assertTrue(collab.isActive)
    }

    @Test
    fun testJsonSerializationAndDeserialization() {
        val original = FriendProfile(
            id = "friend-123",
            handle = "@jordan_b",
            displayName = "Jordan Baker",
            avatarUrl = "content://media/avatar/1.jpg",
            studyStatus = StudyPresenceStatus.STUDYING,
            currentSubject = "Linear Algebra & Vector Spaces",
            joinedAt = 1760000000000L,
            isFavorite = true,
            followersCount = 42,
            followingCount = 18
        )

        val json = original.toJson()
        assertNotNull(json)
        assertTrue(json.contains("\"id\":\"friend-123\""))
        assertTrue(json.contains("\"handle\":\"@jordan_b\""))
        assertTrue(json.contains("\"studyStatus\":\"STUDYING\""))
        assertTrue(json.contains("\"isFavorite\":true"))
        assertTrue(json.contains("\"followersCount\":42"))
        assertTrue(json.contains("\"followingCount\":18"))

        val restored = FriendProfile.fromJson(json)
        assertNotNull(restored)
        assertEquals(original.id, restored!!.id)
        assertEquals(original.handle, restored.handle)
        assertEquals(original.displayName, restored.displayName)
        assertEquals(original.avatarUrl, restored.avatarUrl)
        assertEquals(original.studyStatus, restored.studyStatus)
        assertEquals(original.currentSubject, restored.currentSubject)
        assertEquals(original.joinedAt, restored.joinedAt)
        assertEquals(original.isFavorite, restored.isFavorite)
        assertEquals(original.followersCount, restored.followersCount)
        assertEquals(original.followingCount, restored.followingCount)
    }

    @Test
    fun testJsonDeserializationFallbackForMissingCounts() {
        val legacyJson = """{"id":"leg-1","handle":"@old","displayName":"Old Peer","avatarUrl":null,"studyStatus":"OFFLINE","currentSubject":null,"joinedAt":1700000000000,"isFavorite":false}"""
        val restored = FriendProfile.fromJson(legacyJson)
        assertNotNull(restored)
        assertEquals("leg-1", restored!!.id)
        assertEquals("@old", restored.handle)
        assertEquals(0, restored.followersCount)
        assertEquals(0, restored.followingCount)
    }

    @Test
    fun testJsonSerializationWithNulls() {
        val original = FriendProfile(
            id = "friend-456",
            handle = "@taylor",
            displayName = "Taylor Swift",
            avatarUrl = null,
            studyStatus = StudyPresenceStatus.OFFLINE,
            currentSubject = null,
            isFavorite = false
        )

        val json = original.toJson()
        val restored = FriendProfile.fromJson(json)
        assertNotNull(restored)
        assertEquals(original.id, restored!!.id)
        assertNull(restored.avatarUrl)
        assertNull(restored.currentSubject)
        assertFalse(restored.isFavorite)
    }

    @Test
    fun testMalformedJsonReturnsNull() {
        assertNull(FriendProfile.fromJson(""))
        assertNull(FriendProfile.fromJson("not-json"))
        assertNull(FriendProfile.fromJson("{ incomplete"))
    }
}
