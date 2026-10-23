// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.collab

import com.arinara.fotara.feature.collab.engine.CollabRoomRegistry
import com.arinara.fotara.feature.collab.engine.CollabSessionEngine
import com.arinara.fotara.feature.collab.engine.CollabSessionState
import com.arinara.fotara.feature.collab.model.CollabAccessMode
import com.arinara.fotara.feature.collab.model.CollabAudioBitrate
import com.arinara.fotara.feature.collab.model.CollabMessage
import com.arinara.fotara.feature.collab.model.CollabProtocolCodec
import com.arinara.fotara.feature.collab.model.CollabRole
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CollabPhase100ArchitectureTest {

    @Before
    fun setUp() {
        CollabRoomRegistry.clearAllForTesting()
    }

    @After
    fun tearDown() {
        CollabRoomRegistry.clearAllForTesting()
    }

    /**
     * Connects multiple [CollabSessionEngine] instances together in-memory so any outgoing
     * message from one engine is delivered to all other engines in the cluster.
     */
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
    fun testCustomRoomCodeCreation_collisionRejection_andRealUserProfileIdentity() {
        val host1 = CollabSessionEngine(
            initialLocalPeerId = "peer-host-1",
            initialLocalUserTag = "Nadia Putri",
            initialLocalDeviceId = "dev-host-1",
            initialLocalPronouns = "she/her"
        )
        val host2 = CollabSessionEngine(
            initialLocalPeerId = "peer-host-2",
            initialLocalUserTag = "Jordan Lee",
            initialLocalDeviceId = "dev-host-2"
        )
        val joiner = CollabSessionEngine(
            initialLocalPeerId = "peer-joiner-1",
            initialLocalUserTag = "Rizky Pratama",
            initialLocalDeviceId = "dev-joiner-1",
            initialLocalPronouns = "he/him"
        )
        wireCluster(host1, host2, joiner)

        // 1. Host 1 creates custom room FT-MATH
        assertTrue(host1.createCustomRoom("FT-MATH", userTag = "Nadia Putri", pronouns = "she/her"))
        assertEquals("FT-MATH", host1.roomCode.value)
        assertEquals(CollabRole.HOST, host1.localRole.value)

        // 2. Host 2 tries to create the same custom code FT-MATH -> rejected due to collision!
        assertFalse(host2.createCustomRoom("FT-MATH", userTag = "Jordan Lee"))
        assertNull(host2.roomCode.value)
        assertTrue(host2.lastErrorMessage.value?.contains("Nadia Putri") == true)

        // 3. Joiner joins FT-MATH with real UserProfile name
        assertTrue(
            joiner.joinRoom(
                code = "FT-MATH",
                userTag = "Rizky Pratama",
                pronouns = "he/him",
                requireActiveHost = true
            )
        )
        assertEquals("FT-MATH", joiner.roomCode.value)
        assertEquals(CollabRole.MEMBER, joiner.localRole.value)
        assertEquals("peer-host-1", joiner.hostPeerId.value)

        // 4. Both Host and Joiner see each other's real UserProfile names in activePeers and Chat
        assertEquals("Rizky Pratama", host1.activePeers.value["peer-joiner-1"]?.userTag)
        assertEquals("Nadia Putri", joiner.activePeers.value["peer-host-1"]?.userTag)

        joiner.sendChatMessage("Hi Nadia! Ready to study.")
        val hostReceivedChat = host1.chatMessages.value.last()
        assertEquals("Rizky Pratama", hostReceivedChat.senderTag)
        assertEquals("he/him", hostReceivedChat.senderPronouns)
    }

    @Test
    fun testHostOnlyPinChatMessage_blocksJoinersAndAdminsFromPinning() {
        val host = CollabSessionEngine(
            initialLocalPeerId = "peer-host",
            initialLocalUserTag = "Nadia Putri"
        )
        val joiner = CollabSessionEngine(
            initialLocalPeerId = "peer-joiner",
            initialLocalUserTag = "Rizky Pratama"
        )
        wireCluster(host, joiner)

        host.createCustomRoom("FT-1010", userTag = "Nadia Putri")
        joiner.joinRoom("FT-1010", userTag = "Rizky Pratama")

        val msg = host.sendChatMessage("Exam covers chapters 1 to 5.")
        assertNotNull(msg)
        val msgId = msg!!.messageId

        // Joiner (Member) cannot pin a message
        assertFalse(joiner.pinChatMessage(msgId))
        assertNull(joiner.pinnedChatMessage.value)

        // Even if Host promotes Joiner to Admin, Admin STILL cannot pin a message (Host-only!)
        assertTrue(host.setPeerAdmin("peer-joiner", true))
        assertEquals(CollabRole.ADMIN, joiner.localRole.value)
        assertFalse(joiner.pinChatMessage(msgId))
        assertNull(host.pinnedChatMessage.value)

        // Host pins the message -> succeeds and syncs to Joiner
        assertTrue(host.pinChatMessage(msgId))
        assertEquals(msgId, host.pinnedChatMessage.value?.messageId)
        assertEquals(msgId, joiner.pinnedChatMessage.value?.messageId)
        assertEquals("Exam covers chapters 1 to 5.", joiner.pinnedChatMessage.value?.text)

        // Host unpins the message -> clears on both
        assertTrue(host.pinChatMessage(null))
        assertNull(host.pinnedChatMessage.value)
        assertNull(joiner.pinnedChatMessage.value)
    }

    @Test
    fun testPerUserVolume_audioBitrateCap_andHostKickPermanentSessionBan() {
        val host = CollabSessionEngine(
            initialLocalPeerId = "peer-host",
            initialLocalUserTag = "Nadia Putri",
            initialLocalDeviceId = "dev-host"
        )
        val member = CollabSessionEngine(
            initialLocalPeerId = "peer-member",
            initialLocalUserTag = "Dimas Kusuma",
            initialLocalDeviceId = "dev-dimas-persistent"
        )
        wireCluster(host, member)

        host.createCustomRoom("FT-2020")
        member.joinRoom("FT-2020")

        // 1. Per-user volume control (0% - 200%) & local mute
        assertEquals(1.0f, host.getEffectivePeerVolume("peer-member"), 0.001f)
        host.setPeerVolume("peer-member", 1.75f) // 175% boost
        assertEquals(1.75f, host.getEffectivePeerVolume("peer-member"), 0.001f)
        host.toggleLocalPeerMute("peer-member")
        assertEquals(0.0f, host.getEffectivePeerVolume("peer-member"), 0.001f)
        host.toggleLocalPeerMute("peer-member")
        assertEquals(1.75f, host.getEffectivePeerVolume("peer-member"), 0.001f)

        // 2. Audio Bitrate & Host Max Bitrate Cap
        member.setAudioBitrateKbps(CollabAudioBitrate.HIGH_CLARITY_64_KBPS)
        assertEquals(64, member.audioBitrateKbps.value)
        // Host lowers room max bitrate cap to 32 kbps -> Member automatically clamps to 32 kbps
        assertTrue(host.setRoomMaxBitrateKbps(CollabAudioBitrate.BALANCED_32_KBPS))
        assertEquals(32, member.roomMaxBitrateKbps.value)
        assertEquals(32, member.audioBitrateKbps.value)

        // 3. Host kicks Member -> Member is disconnected and CANNOT rejoin FT-2020
        assertTrue(host.kickPeer("peer-member"))
        assertEquals(CollabSessionState.DISCONNECTED, member.state.value)
        assertNull(member.roomCode.value)
        assertFalse(host.activePeers.value.containsKey("peer-member"))

        // Kicked member attempts to rejoin FT-2020 -> blocked!
        assertFalse(member.joinRoom("FT-2020"))
        assertEquals(CollabSessionState.DISCONNECTED, member.state.value)

        // Even if the kicked user creates a brand new CollabSessionEngine with a new peerId
        // on the same device (same persistent deviceId), the Host registry still blocks them!
        val memberRecreatedEngine = CollabSessionEngine(
            initialLocalPeerId = "peer-member-new-uuid",
            initialLocalUserTag = "Dimas Kusuma",
            initialLocalDeviceId = "dev-dimas-persistent"
        )
        wireCluster(host, memberRecreatedEngine)
        assertFalse(memberRecreatedEngine.joinRoom("FT-2020"))
        assertEquals(CollabSessionState.DISCONNECTED, memberRecreatedEngine.state.value)
    }

    @Test
    fun testAdminPromotion_moderationMuteBlockDelete_ownershipTransfer_andHostLeaveClosesRoom() {
        val host = CollabSessionEngine(
            initialLocalPeerId = "peer-host",
            initialLocalUserTag = "Nadia Putri"
        )
        val adminCandidate = CollabSessionEngine(
            initialLocalPeerId = "peer-admin",
            initialLocalUserTag = "Rizky Pratama"
        )
        val normalMember = CollabSessionEngine(
            initialLocalPeerId = "peer-member",
            initialLocalUserTag = "Alya Sari"
        )
        wireCluster(host, adminCandidate, normalMember)

        host.createCustomRoom("FT-3030")
        adminCandidate.joinRoom("FT-3030")
        normalMember.joinRoom("FT-3030")

        // 1. Promote Rizky to Admin
        assertTrue(host.setPeerAdmin("peer-admin", true))
        assertEquals(CollabRole.ADMIN, adminCandidate.localRole.value)

        // 2. Admin mutes Normal Member in room & blocks Normal Member's chat
        assertTrue(adminCandidate.setPeerRoomMuted("peer-member", true))
        assertTrue(normalMember.roomMutedPeerIds.value.contains("peer-member"))
        assertEquals(0.0f, host.getEffectivePeerVolume("peer-member"), 0.001f)

        assertTrue(adminCandidate.setPeerChatBlocked("peer-member", true))
        assertTrue(normalMember.isLocalUserChatBlocked.value)
        // Blocked member cannot send chat messages
        assertNull(normalMember.sendChatMessage("Can anyone hear me?"))

        // Admin cannot mute or block the Host
        assertFalse(adminCandidate.setPeerRoomMuted("peer-host", true))
        assertFalse(adminCandidate.setPeerChatBlocked("peer-host", true))

        // Unblock member chat, send a message, and have Admin delete the chat message
        assertTrue(adminCandidate.setPeerChatBlocked("peer-member", false))
        assertFalse(normalMember.isLocalUserChatBlocked.value)
        val memberMsg = normalMember.sendChatMessage("Temporary message to delete")
        assertNotNull(memberMsg)
        assertTrue(host.chatMessages.value.any { it.messageId == memberMsg!!.messageId })

        // Admin deletes the chat message -> removed for everyone
        assertTrue(adminCandidate.deleteChatMessage(memberMsg!!.messageId))
        assertFalse(host.chatMessages.value.any { it.messageId == memberMsg.messageId })
        assertFalse(normalMember.chatMessages.value.any { it.messageId == memberMsg.messageId })

        // 3. Transfer Host Ownership from Nadia (peer-host) to Rizky (peer-admin):
        //    Rizky becomes HOST, and Nadia becomes a normal MEMBER (explicitly NOT ADMIN!)
        assertTrue(host.transferHostOwnership("peer-admin"))
        assertEquals(CollabRole.HOST, adminCandidate.localRole.value)
        assertEquals(CollabRole.MEMBER, host.localRole.value)
        assertFalse(host.adminPeerIds.value.contains("peer-host"))
        assertFalse(adminCandidate.adminPeerIds.value.contains("peer-host"))
        assertEquals("peer-admin", normalMember.hostPeerId.value)

        // 4. When the current Host (now Rizky / adminCandidate) leaves the room, EVERYBODY leaves!
        adminCandidate.leaveRoom()
        assertEquals(CollabSessionState.DISCONNECTED, adminCandidate.state.value)
        assertEquals(CollabSessionState.DISCONNECTED, host.state.value)
        assertEquals(CollabSessionState.DISCONNECTED, normalMember.state.value)
        assertNull(host.roomCode.value)
        assertNull(normalMember.roomCode.value)
    }

    @Test
    fun testGlobalAndPerUserCanvasRestrictions_autoSyncGlobalSwitch_andHostOnlyVsHostAndAdminMode() {
        val host = CollabSessionEngine(
            initialLocalPeerId = "peer-host",
            initialLocalUserTag = "Nadia Putri"
        )
        val admin = CollabSessionEngine(
            initialLocalPeerId = "peer-admin",
            initialLocalUserTag = "Rizky Pratama"
        )
        val member1 = CollabSessionEngine(
            initialLocalPeerId = "peer-m1",
            initialLocalUserTag = "Alya Sari"
        )
        val member2 = CollabSessionEngine(
            initialLocalPeerId = "peer-m2",
            initialLocalUserTag = "Dimas Kusuma"
        )
        wireCluster(host, admin, member1, member2)

        host.createCustomRoom("FT-4040")
        admin.joinRoom("FT-4040")
        member1.joinRoom("FT-4040")
        member2.joinRoom("FT-4040")
        host.setPeerAdmin("peer-admin", true)

        // Default AccessMode is HOST_AND_ADMIN (eligible targets = member1, member2)
        assertEquals(CollabAccessMode.HOST_AND_ADMIN, host.accessMode.value)
        assertFalse(host.isGlobalDrawingDisabled.value)
        assertTrue(member1.canLocalUserDraw.value)
        assertTrue(member2.canLocalUserDraw.value)
        assertTrue(admin.canLocalUserDraw.value)
        assertTrue(host.canLocalUserDraw.value)

        // Step 1: Disable drawing for member1 only -> global switch stays false
        assertTrue(host.setPeerDrawingDisabled("peer-m1", true))
        assertFalse(host.isGlobalDrawingDisabled.value)
        assertFalse(member1.canLocalUserDraw.value)
        member1.broadcastStroke(listOf(0f, 0f, 10f, 10f))
        assertEquals(0, host.bufferedStrokesCount.value)
        assertTrue(member2.canLocalUserDraw.value)

        // Step 2: Disable drawing for member2 -> now ALL eligible users (member1 & member2) are disabled!
        // Basic logic: The global switch automatically becomes active (true)!
        assertTrue(host.setPeerDrawingDisabled("peer-m2", true))
        assertTrue(host.isGlobalDrawingDisabled.value)
        assertTrue(member1.isGlobalDrawingDisabled.value)
        assertFalse(member1.canLocalUserDraw.value)
        assertFalse(member2.canLocalUserDraw.value)
        // Under HOST_AND_ADMIN, Admin and Host still have access
        assertTrue(admin.canLocalUserDraw.value)
        assertTrue(host.canLocalUserDraw.value)

        // Step 3: Re-enable member1 -> global switch automatically turns OFF while member2 stays disabled!
        assertTrue(host.setPeerDrawingDisabled("peer-m1", false))
        assertFalse(host.isGlobalDrawingDisabled.value)
        assertTrue(member1.canLocalUserDraw.value)
        assertFalse(member2.canLocalUserDraw.value)

        // Step 4: Verify the same auto-sync logic for Disable Creating New Sheets
        assertTrue(host.setPeerSheetCreationDisabled("peer-m1", true))
        assertFalse(host.isGlobalSheetCreationDisabled.value)
        assertTrue(host.setPeerSheetCreationDisabled("peer-m2", true))
        assertTrue(host.isGlobalSheetCreationDisabled.value)
        assertFalse(member1.canLocalUserCreateSheets.value)
        assertFalse(member2.canLocalUserCreateSheets.value)
        assertTrue(admin.canLocalUserCreateSheets.value)

        // Step 5: Switch Access Mode to HOST_ONLY while Global Lock is active ->
        // Now even ADMIN is restricted; ONLY HOST has access!
        assertTrue(host.setGlobalDrawingDisabled(true))
        assertTrue(host.setAccessMode(CollabAccessMode.HOST_ONLY))
        assertEquals(CollabAccessMode.HOST_ONLY, admin.accessMode.value)
        assertFalse(admin.canLocalUserDraw.value)
        assertFalse(admin.canLocalUserCreateSheets.value)
        assertFalse(member1.canLocalUserDraw.value)
        assertTrue(host.canLocalUserDraw.value)
        assertTrue(host.canLocalUserCreateSheets.value)
    }

    @Test
    fun testCollabProtocolCodec_roundTripAllPhase100Messages() {
        val samples = listOf(
            CollabMessage.RoomProbe("FT-MATH", "peer-1"),
            CollabMessage.WhoHasRoom("FT-MATH", "peer-2", "dev-2", "Rizky Pratama"),
            CollabMessage.HostAnnounce("FT-MATH", "tok-123", "peer-1", "Nadia Putri", "192.168.1.10", 51234, 64, 1761200000L),
            CollabMessage.JoinRejected("FT-MATH", "tok-123", "peer-3", "dev-3", CollabMessage.JoinRejected.REASON_BANNED_BY_HOST),
            CollabMessage.PinChatMessage("tok-123", "peer-1", "msg-99", 1761200100L),
            CollabMessage.DeleteChatMessage("tok-123", "peer-admin", "msg-99", 1761200200L),
            CollabMessage.KickPeer("tok-123", "peer-1", "peer-3", "dev-3", "Dimas Kusuma", "Removed by host"),
            CollabMessage.SetPeerRole("tok-123", "peer-1", "peer-2", CollabRole.ADMIN),
            CollabMessage.TransferHostOwnership("tok-123", "peer-1", "peer-2", 1761200300L),
            CollabMessage.ModMutePeer("tok-123", "peer-2", "peer-4", true),
            CollabMessage.BlockPeerChat("tok-123", "peer-2", "peer-4", true),
            CollabMessage.CanvasPermissionSync(
                sessionToken = "tok-123",
                actorPeerId = "peer-1",
                accessMode = CollabAccessMode.HOST_ONLY,
                isGlobalDrawingDisabled = true,
                isGlobalSheetCreationDisabled = false,
                drawingDisabledPeerIds = setOf("peer-2", "peer-4"),
                sheetCreationDisabledPeerIds = setOf("peer-4")
            ),
            CollabMessage.RoomSettingsSync("tok-123", "peer-1", 32),
            CollabMessage.RoomClosed("FT-MATH", "tok-123", "peer-1", "HOST_LEFT")
        )

        for (msg in samples) {
            val encoded = CollabProtocolCodec.encode(msg)
            val decoded = CollabProtocolCodec.decode(encoded)
            assertEquals("Failed round-trip for ${msg.messageType}", msg, decoded)
        }
    }
}
