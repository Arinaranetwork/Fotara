// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.collab

import com.arinara.fotara.canvas.engine.StrokeProcessor
import com.arinara.fotara.canvas.model.StrokeElement
import com.arinara.fotara.canvas.model.StrokePoint
import com.arinara.fotara.canvas.model.StrokeToolType
import com.arinara.fotara.canvas.persistence.StrokeCodec
import com.arinara.fotara.data.model.CanvasNote
import com.arinara.fotara.feature.collab.engine.CollabRoomRegistry
import com.arinara.fotara.feature.collab.engine.CollabSessionEngine
import com.arinara.fotara.feature.collab.engine.CollabSessionState
import com.arinara.fotara.feature.collab.model.CollabAudioBitrate
import com.arinara.fotara.feature.collab.model.CollabJoinPolicy
import com.arinara.fotara.feature.collab.model.CollabMessage
import com.arinara.fotara.feature.collab.model.CollabProtocolCodec
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CollabPhase101PrivacyAndEfficiencyTest {

    @Before
    fun setUp() {
        CollabRoomRegistry.clearAllForTesting()
    }

    @After
    fun tearDown() {
        CollabRoomRegistry.clearAllForTesting()
    }

    private fun wireCluster(vararg engines: CollabSessionEngine) {
        val list = engines.toList()
        for (sender in list) {
            sender.transportBridge = { msg ->
                for (receiver in list) {
                    if (receiver !== sender) {
                        receiver.receiveMessage(msg)
                    }
                }
            }
        }
    }

    @Test
    fun testMaxParticipantsLimit_rejectsWhenRoomIsFull() {
        val host = CollabSessionEngine(
            initialLocalPeerId = "peer-host",
            initialLocalUserTag = "Nadia Host",
            initialLocalDeviceId = "dev-host",
            initialLocalUserHandle = "@nadia_host"
        )
        val joiner1 = CollabSessionEngine(
            initialLocalPeerId = "peer-j1",
            initialLocalUserTag = "Member One",
            initialLocalDeviceId = "dev-j1",
            initialLocalUserHandle = "@member1"
        )
        val joiner2 = CollabSessionEngine(
            initialLocalPeerId = "peer-j2",
            initialLocalUserTag = "Member Two",
            initialLocalDeviceId = "dev-j2",
            initialLocalUserHandle = "@member2"
        )
        wireCluster(host, joiner1, joiner2)

        assertTrue(host.createCustomRoom("FT-CAP2", userTag = "Nadia Host"))
        // Set maxParticipants to 2 (Host + 1 Member)
        host.setMaxParticipants(2)
        assertEquals(2, host.maxParticipants.value)

        // First member joins -> total participants = 2 (at capacity)
        assertTrue(joiner1.joinRoom("FT-CAP2", userTag = "Member One", requireActiveHost = true))
        assertEquals(2, host.activePeers.value.size)
        assertEquals(CollabSessionState.IN_ROOM, joiner1.state.value)

        // Second member attempts to join -> rejected with REASON_ROOM_FULL
        assertFalse(joiner2.joinRoom("FT-CAP2", userTag = "Member Two", requireActiveHost = true))
        assertEquals(CollabSessionState.DISCONNECTED, joiner2.state.value)
        assertNull(joiner2.roomCode.value)
        assertTrue(joiner2.lastErrorMessage.value?.contains("full", ignoreCase = true) == true)
        assertFalse(host.activePeers.value.containsKey("peer-j2"))

        // Host increases capacity to 4 -> Joiner 2 can now join!
        host.setMaxParticipants(4)
        assertEquals(4, joiner1.maxParticipants.value)
        assertTrue(joiner2.joinRoom("FT-CAP2", userTag = "Member Two", requireActiveHost = true))
        assertEquals(CollabSessionState.IN_ROOM, joiner2.state.value)
        assertEquals(3, host.activePeers.value.size)
    }

    @Test
    fun testFriendsOnlyPolicy_allowsHostFriends_andRejectsNonFriends() {
        val host = CollabSessionEngine(
            initialLocalPeerId = "peer-host",
            initialLocalUserTag = "Nadia Host",
            initialLocalDeviceId = "dev-host",
            initialLocalUserHandle = "@nadia"
        )
        val friendPeer = CollabSessionEngine(
            initialLocalPeerId = "peer-friend",
            initialLocalUserTag = "Rizky Friend",
            initialLocalDeviceId = "dev-friend",
            initialLocalUserHandle = "@rizky_dev"
        )
        val strangerPeer = CollabSessionEngine(
            initialLocalPeerId = "peer-stranger",
            initialLocalUserTag = "Random Stranger",
            initialLocalDeviceId = "dev-stranger",
            initialLocalUserHandle = "@stranger99"
        )
        wireCluster(host, friendPeer, strangerPeer)

        assertTrue(host.createCustomRoom("FT-FRND", userTag = "Nadia Host"))
        host.syncHostFriends(setOf("@rizky_dev", "sari_art"))
        host.setJoinPolicy(CollabJoinPolicy.FRIENDS_ONLY)
        assertEquals(CollabJoinPolicy.FRIENDS_ONLY, host.joinPolicy.value)

        // Friend joins -> accepted
        assertTrue(friendPeer.joinRoom("FT-FRND", userTag = "Rizky Friend", requireActiveHost = true))
        assertEquals(CollabSessionState.IN_ROOM, friendPeer.state.value)
        assertTrue(host.activePeers.value.containsKey("peer-friend"))

        // Stranger joins -> rejected with REASON_FRIENDS_ONLY
        assertFalse(strangerPeer.joinRoom("FT-FRND", userTag = "Random Stranger", requireActiveHost = true))
        assertEquals(CollabSessionState.DISCONNECTED, strangerPeer.state.value)
        assertTrue(strangerPeer.lastErrorMessage.value?.contains("friends", ignoreCase = true) == true)
        assertFalse(host.activePeers.value.containsKey("peer-stranger"))
    }

    @Test
    fun testCustomPresetPolicy_andNamedPresetPersistence() {
        val host = CollabSessionEngine(
            initialLocalPeerId = "peer-host",
            initialLocalUserTag = "Nadia Host",
            initialLocalDeviceId = "dev-host",
            initialLocalUserHandle = "@nadia"
        )
        val allowedPeer = CollabSessionEngine(
            initialLocalPeerId = "peer-allowed",
            initialLocalUserTag = "Clara Lab",
            initialLocalDeviceId = "dev-allowed",
            initialLocalUserHandle = "@clara_lab"
        )
        val blockedPeer = CollabSessionEngine(
            initialLocalPeerId = "peer-blocked",
            initialLocalUserTag = "Outsider",
            initialLocalDeviceId = "dev-blocked",
            initialLocalUserHandle = "@outsider"
        )
        wireCluster(host, allowedPeer, blockedPeer)

        assertTrue(host.createCustomRoom("FT-PRST", userTag = "Nadia Host"))
        host.setJoinPolicy(CollabJoinPolicy.CUSTOM_PRESET)
        host.addAllowedUserHandle("@clara_lab")
        host.addAllowedUserHandle("budi_design")

        // Save as named preset "Bio Group"
        val preset = host.savePrivacyPreset("Bio Group")
        assertNotNull(preset)
        assertEquals("Bio Group", preset?.name)
        assertEquals(1, host.savedPrivacyPresets.value.size)
        assertEquals(preset?.id, host.activePresetId.value)

        // Allowed peer joins -> accepted
        assertTrue(allowedPeer.joinRoom("FT-PRST", userTag = "Clara Lab", requireActiveHost = true))
        assertEquals(CollabSessionState.IN_ROOM, allowedPeer.state.value)
        assertEquals(CollabJoinPolicy.CUSTOM_PRESET, allowedPeer.joinPolicy.value)
        assertTrue(allowedPeer.allowedUserHandles.value.contains("@clara_lab"))

        // Unlisted peer joins -> rejected with REASON_NOT_IN_PRESET
        assertFalse(blockedPeer.joinRoom("FT-PRST", userTag = "Outsider", requireActiveHost = true))
        assertEquals(CollabSessionState.DISCONNECTED, blockedPeer.state.value)
        assertTrue(blockedPeer.lastErrorMessage.value?.contains("preset", ignoreCase = true) == true)

        // Remove handle and re-apply saved preset
        host.removeAllowedUserHandle("@clara_lab")
        assertFalse(host.allowedUserHandles.value.contains("@clara_lab"))
        host.applyPrivacyPreset(preset!!.id)
        assertTrue(host.allowedUserHandles.value.contains("@clara_lab"))
    }

    @Test
    fun testInvitedOnlyPolicy_allowsOnlyInvitedHandles() {
        val host = CollabSessionEngine(
            initialLocalPeerId = "peer-host",
            initialLocalUserTag = "Nadia Host",
            initialLocalDeviceId = "dev-host",
            initialLocalUserHandle = "@nadia"
        )
        val invitedPeer = CollabSessionEngine(
            initialLocalPeerId = "peer-invited",
            initialLocalUserTag = "DimasVIP",
            initialLocalDeviceId = "dev-invited",
            initialLocalUserHandle = "@dimas_vip"
        )
        val uninvitedFriend = CollabSessionEngine(
            initialLocalPeerId = "peer-uninvited",
            initialLocalUserTag = "Rizky Friend",
            initialLocalDeviceId = "dev-uninvited",
            initialLocalUserHandle = "@rizky_dev"
        )
        wireCluster(host, invitedPeer, uninvitedFriend)

        assertTrue(host.createCustomRoom("FT-INVT", userTag = "Nadia Host"))
        host.syncHostFriends(setOf("@rizky_dev"))
        host.setJoinPolicy(CollabJoinPolicy.INVITED_ONLY)
        host.inviteUser("@dimas_vip")

        // Uninvited friend attempts to join -> rejected because room is INVITED_ONLY
        assertFalse(uninvitedFriend.joinRoom("FT-INVT", userTag = "Rizky Friend", requireActiveHost = true))
        assertEquals(CollabSessionState.DISCONNECTED, uninvitedFriend.state.value)
        assertTrue(uninvitedFriend.lastErrorMessage.value?.contains("Invite-Only", ignoreCase = true) == true)

        // Invited user attempts to join -> accepted
        assertTrue(invitedPeer.joinRoom("FT-INVT", userTag = "DimasVIP", requireActiveHost = true))
        assertEquals(CollabSessionState.IN_ROOM, invitedPeer.state.value)
        assertTrue(host.activePeers.value.containsKey("peer-invited"))
    }

    @Test
    fun testProtocolCodec_privacySettingsAndFloatListRoundTrip() {
        val syncMsg = CollabMessage.RoomSettingsSync(
            sessionToken = "tok-123",
            hostPeerId = "peer-host",
            maxBitrateKbps = CollabAudioBitrate.HIGH_CLARITY_64_KBPS,
            maxParticipants = 8,
            joinPolicy = CollabJoinPolicy.CUSTOM_PRESET,
            allowedUserHandles = setOf("@alice", "@bob"),
            invitedUserHandles = setOf("@charlie")
        )

        val encoded = CollabProtocolCodec.encode(syncMsg)
        val decoded = CollabProtocolCodec.decode(encoded) as? CollabMessage.RoomSettingsSync
        assertNotNull(decoded)
        assertEquals(8, decoded?.maxParticipants)
        assertEquals(CollabJoinPolicy.CUSTOM_PRESET, decoded?.joinPolicy)
        assertEquals(setOf("@alice", "@bob"), decoded?.allowedUserHandles)
        assertEquals(setOf("@charlie"), decoded?.invitedUserHandles)

        val strokeMsg = CollabMessage.StrokeBroadcast(
            strokeId = "st-1",
            points = listOf(10.25f, 5.0f, 20.5f, 15.125f, -5.75f, 30.0f, 100.0f, -12.5f),
            colorHex = "#7A5AF8",
            strokeWidth = 4.5f,
            peerId = "peer-host",
            sessionToken = "tok-123"
        )
        val strokeJson = CollabProtocolCodec.encode(strokeMsg)
        val decodedStroke = CollabProtocolCodec.decode(strokeJson) as? CollabMessage.StrokeBroadcast
        assertNotNull(decodedStroke)
        assertEquals(8, decodedStroke?.points?.size)
        assertEquals(10.25f, decodedStroke!!.points[0], 0.001f)
        assertEquals(-5.75f, decodedStroke.points[4], 0.001f)
        assertEquals(-12.5f, decodedStroke.points[7], 0.001f)
    }

    @Test
    fun testCanvasNoteFullEquality_andStrokeCodecZeroDrift() {
        val baseNote = CanvasNote(
            id = 42L,
            folderId = 1L,
            subfolderId = null,
            title = "Initial Title",
            createdAt = 1000L,
            updatedAt = 2000L,
            thumbnailPath = "/thumb1.png",
            dataBlob = byteArrayOf(1, 2, 3),
            tagColor = null,
            isPinned = false
        )
        val renamedNote = baseNote.copy(title = "Updated Title", updatedAt = 3000L)
        val pinnedNote = baseNote.copy(isPinned = true)
        val identicalNote = baseNote.copy(dataBlob = byteArrayOf(1, 2, 3))

        assertEquals(baseNote, identicalNote)
        assertEquals(baseNote.hashCode(), identicalNote.hashCode())
        assertNotEquals(baseNote, renamedNote)
        assertNotEquals(baseNote, pinnedNote)

        // Verify StrokeCodec does not accumulate quantization drift over 200 sub-step increments
        val points = (0..200).map { i ->
            StrokePoint(
                x = i * 0.07f, // 0.02f fractional remainder per step at 20x scale; would drift by 4px without feedback!
                y = i * 0.08f,
                pressure = 0.5f
            )
        }
        val originalStroke = StrokeElement(
            id = "stroke-drift-test",
            layerId = "layer_default",
            points = points,
            color = 0xFF7A5AF8L,
            width = 3f,
            toolType = StrokeToolType.PEN,
            bounds = StrokeProcessor.computeBounds(points, 3f)
        )
        val bytes = StrokeCodec.encode(originalStroke)
        val restoredStroke = StrokeCodec.decode(bytes, elementId = "stroke-drift-test")!!

        val lastOrig = originalStroke.points.last()
        val lastRestored = restoredStroke.points.last()
        // Error must stay bounded within 1 quantization step (0.05f), not accumulate to 4.0f!
        assertEquals(lastOrig.x, lastRestored.x, 0.06f)
        assertEquals(lastOrig.y, lastRestored.y, 0.06f)
    }
}
