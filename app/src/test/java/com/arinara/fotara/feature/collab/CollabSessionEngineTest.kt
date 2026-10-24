// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.collab

import com.arinara.fotara.feature.collab.engine.CollabRoomRegistry
import com.arinara.fotara.feature.collab.engine.CollabSessionEngine
import com.arinara.fotara.feature.collab.engine.CollabSessionState
import com.arinara.fotara.feature.collab.model.CollabMessage
import com.arinara.fotara.feature.collab.model.CollabRoomCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the CollabSessionEngine state machine, peer lifecycle, and vector stroke buffer.
 */
class CollabSessionEngineTest {

    private lateinit var engine: CollabSessionEngine
    private val dispatchedMessages = mutableListOf<CollabMessage>()

    @Before
    fun setUp() {
        CollabRoomRegistry.clearAllForTesting()
        engine = CollabSessionEngine(
            initialLocalPeerId = "local_p1",
            initialLocalUserTag = "TestHost",
            initialLocalColorHex = "#2563EB"
        )
        dispatchedMessages.clear()
        engine.transportBridge = { msg ->
            dispatchedMessages.add(msg)
        }
    }

    @Test
    fun testInitialStateIsDisconnected() {
        assertEquals(CollabSessionState.DISCONNECTED, engine.state.value)
        assertNull(engine.roomCode.value)
        assertTrue(engine.activePeers.value.isEmpty())
        assertEquals(0, engine.bufferedStrokesCount.value)
    }

    @Test
    fun testCreateRoomTransitionsToInRoom() {
        val code = engine.createRoom(userTag = "HostUser", colorHex = "#2563EB")

        assertEquals(CollabSessionState.IN_ROOM, engine.state.value)
        assertEquals(code, engine.roomCode.value)
        assertTrue(CollabRoomCode.isValid(code))

        // Local peer should be present in active peers
        val peers = engine.activePeers.value
        assertEquals(1, peers.size)
        val localPeer = peers["local_p1"]
        assertNotNull(localPeer)
        assertEquals("HostUser", localPeer?.userTag)
        assertEquals("#2563EB", localPeer?.colorHex)

        // PeerJoined message should have been dispatched
        val peerJoined = dispatchedMessages.filterIsInstance<CollabMessage.PeerJoined>().firstOrNull()
        assertNotNull(peerJoined)
        assertEquals("local_p1", peerJoined?.peerId)
    }

    @Test
    fun testJoinRoomWithoutActiveHostFails() {
        // Attempting to join a room code that nobody has created/hosted yet MUST fail
        val success = engine.joinRoom("FT-8821", userTag = "CollaboratorBob", colorHex = "#EFE8DA")
        assertFalse(success)
        assertEquals(CollabSessionState.DISCONNECTED, engine.state.value)
        assertNull(engine.roomCode.value)
        assertTrue(engine.activePeers.value.isEmpty())
        assertNotNull(engine.lastErrorMessage.value)
        assertTrue(engine.lastErrorMessage.value!!.contains("FT-8821"))
        assertTrue(engine.lastErrorMessage.value!!.contains("No active host"))
    }

    @Test
    fun testJoinRoomWithValidCode() {
        val hostEngine = CollabSessionEngine(
            initialLocalPeerId = "host_p0",
            initialLocalUserTag = "HostAlice",
            initialLocalColorHex = "#7A5AF8"
        )
        assertTrue(hostEngine.createCustomRoom("FT-8821", userTag = "HostAlice"))

        val success = engine.joinRoom("FT-8821", userTag = "CollaboratorBob", colorHex = "#EFE8DA")
        assertTrue(success)
        assertEquals(CollabSessionState.IN_ROOM, engine.state.value)
        assertEquals("FT-8821", engine.roomCode.value)
        assertEquals("host_p0", engine.hostPeerId.value)

        val peers = engine.activePeers.value
        assertEquals(2, peers.size)
        val localPeer = peers["local_p1"]
        assertNotNull(localPeer)
        assertEquals("CollaboratorBob", localPeer?.userTag)
        assertEquals("#EFE8DA", localPeer?.colorHex)
        assertEquals("HostAlice", peers["host_p0"]?.userTag)

        // Verify JoinRoom and PeerJoined dispatched
        val joinMsg = dispatchedMessages.filterIsInstance<CollabMessage.JoinRoom>().firstOrNull()
        assertNotNull(joinMsg)
        assertEquals("FT-8821", joinMsg?.roomCode)

        val peerJoinedMsg = dispatchedMessages.filterIsInstance<CollabMessage.PeerJoined>().firstOrNull()
        assertNotNull(peerJoinedMsg)
        assertEquals("local_p1", peerJoinedMsg?.peerId)
    }

    @Test
    fun testJoinRoomWithRemoteHostAnnounceSucceeds() {
        // Simulate receiving a HostAnnounce from another device on the LAN before joining
        engine.receiveMessage(
            CollabMessage.HostAnnounce(
                roomCode = "FT-9942",
                sessionToken = "lan_token_9942",
                hostPeerId = "remote_host_peer",
                hostUserName = "RemoteHostUser",
                lanIp = "192.168.1.42",
                lanPort = 51234
            )
        )
        val success = engine.joinRoom("FT-9942", userTag = "JoinerEve")
        assertTrue(success)
        assertEquals(CollabSessionState.IN_ROOM, engine.state.value)
        assertEquals("FT-9942", engine.roomCode.value)
        assertEquals("remote_host_peer", engine.hostPeerId.value)
        assertEquals("lan_token_9942", engine.sessionToken.value)
    }

    @Test
    fun testJoinRoomWithInvalidCodeFails() {
        val success = engine.joinRoom("INVALID_CODE")
        assertFalse(success)
        assertEquals(CollabSessionState.DISCONNECTED, engine.state.value)
        assertNull(engine.roomCode.value)
        assertTrue(engine.activePeers.value.isEmpty())
    }

    @Test
    fun testLeaveRoomResetsSession() {
        engine.createRoom()
        assertEquals(CollabSessionState.IN_ROOM, engine.state.value)

        engine.leaveRoom()
        assertEquals(CollabSessionState.DISCONNECTED, engine.state.value)
        assertNull(engine.roomCode.value)
        assertTrue(engine.activePeers.value.isEmpty())
        assertEquals(0, engine.bufferedStrokesCount.value)

        val leaveMsg = dispatchedMessages.filterIsInstance<CollabMessage.LeaveRoom>().firstOrNull()
        assertNotNull(leaveMsg)
        assertEquals("local_p1", leaveMsg?.peerId)
    }

    @Test
    fun testBroadcastStrokeAndCursor() {
        engine.createRoom()

        val stroke = engine.broadcastStroke(
            points = listOf(10f, 20f, 30f, 40f),
            colorHex = "#2563EB",
            strokeWidth = 3.5f,
            strokeId = "s_1"
        )
        assertEquals("s_1", stroke.strokeId)
        val broadcastStroke = dispatchedMessages.filterIsInstance<CollabMessage.StrokeBroadcast>().firstOrNull()
        assertNotNull(broadcastStroke)
        assertEquals("s_1", broadcastStroke?.strokeId)

        val cursor = engine.updateCursor(100f, 200f)
        assertEquals(100f, cursor.x)
        assertEquals(200f, cursor.y)
        assertEquals("local_p1", cursor.peerId)

        val localPeer = engine.activePeers.value["local_p1"]
        assertEquals(100f, localPeer?.lastCursorX)
        assertEquals(200f, localPeer?.lastCursorY)
    }

    @Test
    fun testPeerLifecycleViaReceiveMessage() {
        engine.createRoom()

        // 1. PeerJoined
        val peerJoined = CollabMessage.PeerJoined(
            peerId = "peer_charlie",
            userTag = "Charlie",
            colorHex = "#2A9D8F"
        )
        engine.receiveMessage(peerJoined)

        var peers = engine.activePeers.value
        assertEquals(2, peers.size)
        val charlie = peers["peer_charlie"]
        assertNotNull(charlie)
        assertEquals("Charlie", charlie?.userTag)
        assertEquals("#2A9D8F", charlie?.colorHex)

        // 2. CursorBroadcast from Charlie
        val cursorMsg = CollabMessage.CursorBroadcast(
            peerId = "peer_charlie",
            x = 450f,
            y = 600f,
            timestamp = 1700000010000L
        )
        engine.receiveMessage(cursorMsg)

        val updatedCharlie = engine.activePeers.value["peer_charlie"]
        assertEquals(450f, updatedCharlie?.lastCursorX)
        assertEquals(600f, updatedCharlie?.lastCursorY)
        assertEquals(1700000010000L, updatedCharlie?.lastActiveMs)

        // 3. LeaveRoom from Charlie
        engine.receiveMessage(CollabMessage.LeaveRoom(peerId = "peer_charlie"))
        peers = engine.activePeers.value
        assertEquals(1, peers.size)
        assertNull(peers["peer_charlie"])
    }

    @Test
    fun testVectorStrokeMergerBufferAndConsumption() {
        engine.createRoom()
        assertEquals(0, engine.bufferedStrokesCount.value)

        val stroke1 = CollabMessage.StrokeBroadcast(
            strokeId = "stroke_1",
            points = listOf(1f, 2f, 3f, 4f),
            colorHex = "#2563EB",
            strokeWidth = 4f
        )
        val stroke2 = CollabMessage.StrokeBroadcast(
            strokeId = "stroke_2",
            points = listOf(5f, 6f, 7f, 8f),
            colorHex = "#EFE8DA",
            strokeWidth = 2f
        )

        engine.receiveMessage(stroke1)
        engine.receiveMessage(stroke2)

        assertEquals(2, engine.bufferedStrokesCount.value)

        val consumed = engine.consumeBufferedStrokes()
        assertEquals(2, consumed.size)
        assertEquals("stroke_1", consumed[0].strokeId)
        assertEquals("stroke_2", consumed[1].strokeId)
        assertEquals(0, engine.bufferedStrokesCount.value)

        // Second consume call returns empty list
        val emptyAgain = engine.consumeBufferedStrokes()
        assertTrue(emptyAgain.isEmpty())
    }

    @Test
    fun testPeerTimeoutEviction() {
        engine.createRoom()

        val now = 1700000020000L
        val activePeer = CollabMessage.PeerJoined("p_active", "ActiveUser", "#2563EB")
        val stalePeer = CollabMessage.PeerJoined("p_stale", "StaleUser", "#EFE8DA")

        engine.receiveMessage(activePeer)
        engine.receiveMessage(stalePeer)

        // Active peer sent cursor 2 seconds ago
        engine.receiveMessage(
            CollabMessage.CursorBroadcast("p_active", 10f, 10f, timestamp = now - 2000L)
        )
        // Stale peer last active 25 seconds ago
        engine.receiveMessage(
            CollabMessage.CursorBroadcast("p_stale", 20f, 20f, timestamp = now - 25000L)
        )

        assertEquals(3, engine.activePeers.value.size) // local + 2 peers

        // Evict with 15s timeout
        val evicted = engine.evictTimedOutPeers(nowMs = now, timeoutMs = 15000L)
        assertEquals(listOf("p_stale"), evicted)

        val remainingPeers = engine.activePeers.value
        assertEquals(2, remainingPeers.size)
        assertNotNull(remainingPeers["local_p1"])
        assertNotNull(remainingPeers["p_active"])
        assertNull(remainingPeers["p_stale"])
    }

    @Test
    fun testReconnectFlow() {
        val code = engine.createRoom()
        assertEquals(CollabSessionState.IN_ROOM, engine.state.value)

        val reconnected = engine.reconnect()
        assertTrue(reconnected)
        assertEquals(CollabSessionState.IN_ROOM, engine.state.value)
        assertEquals(code, engine.roomCode.value)
    }

    @Test
    fun testTextAndImageElementSyncAndElementRemovalSync() {
        engine.createRoom()

        val textSync = CollabMessage.TextElementSync(
            elementId = "txt_1",
            layerId = "layer_default",
            text = "Hello Collaborators",
            x = 120f,
            y = 240f,
            width = 200f,
            height = 60f,
            fontSizeSp = 18f,
            color = 0xFFF8FAFC,
            fontWeight = 700,
            backgroundStyle = "TRANSPARENT",
            alignment = "LEFT",
            rotationDegrees = 15f,
            zIndex = 3
        )
        val encodedText = com.arinara.fotara.feature.collab.model.CollabProtocolCodec.encode(textSync)
        val decodedText = com.arinara.fotara.feature.collab.model.CollabProtocolCodec.decode(encodedText) as? CollabMessage.TextElementSync
        assertNotNull(decodedText)
        assertEquals("Hello Collaborators", decodedText?.text)
        assertEquals(15f, decodedText?.rotationDegrees)

        val imgSync = CollabMessage.ImageElementSync(
            elementId = "img_1",
            layerId = "layer_default",
            assetId = "asset_abc",
            x = 50f,
            y = 80f,
            width = 320f,
            height = 240f,
            scale = 1.25f,
            rotationDegrees = 0f,
            zIndex = 4,
            imageBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
        )
        val encodedImg = com.arinara.fotara.feature.collab.model.CollabProtocolCodec.encode(imgSync)
        val decodedImg = com.arinara.fotara.feature.collab.model.CollabProtocolCodec.decode(encodedImg) as? CollabMessage.ImageElementSync
        assertNotNull(decodedImg)
        assertEquals("asset_abc", decodedImg?.assetId)
        assertEquals(imgSync.imageBase64, decodedImg?.imageBase64)

        val remSync = CollabMessage.ElementsRemoveSync(
            elementIds = listOf("txt_1", "stroke_99")
        )
        val encodedRem = com.arinara.fotara.feature.collab.model.CollabProtocolCodec.encode(remSync)
        val decodedRem = com.arinara.fotara.feature.collab.model.CollabProtocolCodec.decode(encodedRem) as? CollabMessage.ElementsRemoveSync
        assertNotNull(decodedRem)
        assertEquals(listOf("txt_1", "stroke_99"), decodedRem?.elementIds)

        engine.receiveMessage(decodedText!!)
        engine.receiveMessage(decodedImg!!)
        engine.receiveMessage(decodedRem!!)

        assertEquals(3, engine.bufferedStrokesCount.value)
        val consumedTexts = engine.consumeBufferedTextElements()
        val consumedImages = engine.consumeBufferedImageElements()
        val consumedRemovals = engine.consumeBufferedRemovedElements()

        assertEquals(1, consumedTexts.size)
        assertEquals("txt_1", consumedTexts[0].elementId)
        assertEquals(1, consumedImages.size)
        assertEquals("img_1", consumedImages[0].elementId)
        assertEquals(1, consumedRemovals.size)
        assertEquals(listOf("txt_1", "stroke_99"), consumedRemovals[0].elementIds)
    }

    @Test
    fun testAudioStreamFrameBroadcastAndReceiveRespectsMute() {
        engine.createRoom()

        var receivedFrame: CollabMessage.AudioStreamFrame? = null
        engine.onAudioFrameReceived = { frame ->
            receivedFrame = frame
        }

        // Initially local mic is unmuted
        assertFalse(engine.isLocalMicMuted.value)
        engine.broadcastAudioFrame("AAECAwQF", sequenceNumber = 1L, sampleRateHz = 16000)
        val sentAudio = dispatchedMessages.filterIsInstance<CollabMessage.AudioStreamFrame>()
        assertEquals(1, sentAudio.size)
        assertEquals("local_p1", sentAudio[0].peerId)

        // When muted locally, broadcastAudioFrame must not dispatch
        engine.setLocalMicMuted(true)
        assertTrue(engine.isLocalMicMuted.value)
        engine.broadcastAudioFrame("BwgJCgsM", sequenceNumber = 2L, sampleRateHz = 16000)
        assertEquals(1, dispatchedMessages.filterIsInstance<CollabMessage.AudioStreamFrame>().size)

        // Receiving audio frame from remote peer invokes callback
        val remoteFrame = CollabMessage.AudioStreamFrame(
            peerId = "peer_remote_2",
            sequenceNumber = 7L,
            sampleRateHz = 16000,
            pcmDataBase64 = "AAECAwQF"
        )
        val encoded = com.arinara.fotara.feature.collab.model.CollabProtocolCodec.encode(remoteFrame)
        val decoded = com.arinara.fotara.feature.collab.model.CollabProtocolCodec.decode(encoded)
        assertNotNull(decoded)
        engine.receiveMessage(decoded!!)
        assertNotNull(receivedFrame)
        assertEquals("peer_remote_2", receivedFrame?.peerId)
        assertEquals(7L, receivedFrame?.sequenceNumber)
    }

    @Test
    fun testDisableDrawingBlocksMemberFromDrawing() {
        val hostEngine = CollabSessionEngine(
            initialLocalPeerId = "host_p0",
            initialLocalUserTag = "HostAlice"
        )
        hostEngine.createCustomRoom("FT-5500", userTag = "HostAlice")

        assertTrue(engine.joinRoom("FT-5500", userTag = "MemberBob"))
        assertTrue(engine.canLocalUserDraw.value)

        // Host disables drawing for MemberBob
        engine.receiveMessage(
            CollabMessage.CanvasPermissionSync(
                actorPeerId = "host_p0",
                drawingDisabledPeerIds = setOf("local_p1")
            )
        )
        assertFalse(engine.canLocalUserDraw.value)

        // Attempting to broadcast text or image while drawing is disabled must be blocked
        dispatchedMessages.clear()
        assertFalse(
            engine.broadcastTextElement(
                CollabMessage.TextElementSync(
                    elementId = "t_blocked",
                    layerId = "l1",
                    text = "Blocked",
                    x = 0f,
                    y = 0f,
                    width = 100f,
                    height = 40f,
                    fontSizeSp = 16f,
                    color = 0xFFFFFFFF,
                    fontWeight = 400,
                    backgroundStyle = "TRANSPARENT",
                    alignment = "LEFT",
                    rotationDegrees = 0f,
                    zIndex = 1
                )
            )
        )
        assertTrue(dispatchedMessages.filterIsInstance<CollabMessage.TextElementSync>().isEmpty())
    }

    @Test
    fun testHostLeaveAndHostConnectionLostTerminatesMemberAndAdminSession() {
        val hostEngine = CollabSessionEngine(
            initialLocalPeerId = "host_p0",
            initialLocalUserTag = "HostAlice"
        )
        hostEngine.createCustomRoom("FT-7711", userTag = "HostAlice")

        // Join as Member and promote to Admin
        assertTrue(engine.joinRoom("FT-7711", userTag = "AdminBob"))
        engine.receiveMessage(
            CollabMessage.SetPeerRole(
                hostPeerId = "host_p0",
                targetPeerId = "local_p1",
                newRole = com.arinara.fotara.feature.collab.model.CollabRole.ADMIN
            )
        )
        assertEquals(com.arinara.fotara.feature.collab.model.CollabRole.ADMIN, engine.localRole.value)
        assertEquals(CollabSessionState.IN_ROOM, engine.state.value)

        // Case 1: Host sends RoomClosed
        engine.receiveMessage(
            CollabMessage.RoomClosed(
                roomCode = "FT-7711",
                hostPeerId = "host_p0",
                reason = "The host has left and closed the room."
            )
        )
        assertEquals(CollabSessionState.DISCONNECTED, engine.state.value)
        assertNull(engine.roomCode.value)
        assertTrue(engine.activePeers.value.isEmpty())
        assertEquals("Session ended because the host left the room.", engine.lastErrorMessage.value)

        // Case 2: Re-create & join, then TCP EOF (handleHostConnectionLost) fires before UDP RoomClosed arrives
        hostEngine.leaveRoom()
        assertTrue(hostEngine.createCustomRoom("FT-7722", userTag = "HostAlice"))
        assertTrue(engine.joinRoom("FT-7722", userTag = "AdminBob"))
        assertEquals(CollabSessionState.IN_ROOM, engine.state.value)

        engine.handleHostConnectionLost()
        assertEquals(CollabSessionState.DISCONNECTED, engine.state.value)
        assertNull(engine.roomCode.value)
        assertTrue(engine.activePeers.value.isEmpty())
        assertEquals("Session ended because the host left the room.", engine.lastErrorMessage.value)
    }

    @Test
    fun testRealUsernameResolvesInsteadOfFotaraUserAndRealTimeCursorAndLiveStrokeStreaming() {
        val hostEngine = CollabSessionEngine(
            initialLocalPeerId = "host_p0",
            initialLocalUserTag = "Fotara User",
            initialLocalUserHandle = "@sepli_host"
        )
        val roomCode = hostEngine.createRoom(
            userTag = "Fotara User",
            userHandle = "@sepli_host"
        )
        assertTrue(roomCode.isNotBlank())
        assertEquals("sepli_host", hostEngine.activePeers.value["host_p0"]?.userTag)

        val joinerEngine = CollabSessionEngine(
            initialLocalPeerId = "joiner_p2",
            initialLocalUserTag = "Fotara User",
            initialLocalUserHandle = "@alex_member"
        )
        // Pre-populate stale buffers before joinRoom to verify A-008 buffer cleanup on join
        joinerEngine.receiveMessage(
            CollabMessage.TextElementSync(
                elementId = "stale_txt",
                layerId = "l1",
                text = "Stale",
                x = 0f,
                y = 0f,
                width = 50f,
                height = 20f,
                fontSizeSp = 14f,
                color = 0xFFFFFFFF,
                fontWeight = 400,
                backgroundStyle = "TRANSPARENT",
                alignment = "LEFT",
                rotationDegrees = 0f,
                zIndex = 1,
                peerId = "other"
            )
        )
        assertTrue(joinerEngine.joinRoom(roomCode, userTag = "Fotara User", userHandle = "@alex_member"))
        assertTrue(joinerEngine.consumeBufferedTextElements().isEmpty())
        assertEquals("alex_member", joinerEngine.activePeers.value["joiner_p2"]?.userTag)
        assertEquals("alex_member", hostEngine.activePeers.value["joiner_p2"]?.userTag)

        // Verify A-009: real-time cursor callback and live progressive stroke updates with same strokeId
        var cursorCallbackCount = 0
        var latestJoinerCursorX = 0f
        var latestJoinerCursorY = 0f
        hostEngine.onPeerCursorsUpdated = { peers ->
            val joinerPeer = peers.firstOrNull { it.peerId == "joiner_p2" }
            if (joinerPeer != null) {
                cursorCallbackCount++
                latestJoinerCursorX = joinerPeer.lastCursorX
                latestJoinerCursorY = joinerPeer.lastCursorY
            }
        }
        hostEngine.receiveMessage(
            CollabMessage.CursorBroadcast(
                peerId = "joiner_p2",
                x = 140f,
                y = 280f,
                timestamp = 1000L
            )
        )
        assertTrue(cursorCallbackCount >= 1)
        assertEquals(140f, latestJoinerCursorX, 0.01f)
        assertEquals(280f, latestJoinerCursorY, 0.01f)

        // Live stroke streaming: same strokeId with growing points list must enqueue each progressive update
        hostEngine.receiveMessage(
            CollabMessage.StrokeBroadcast(
                strokeId = "live_stroke_1",
                points = listOf(10f, 10f, 20f, 20f),
                colorHex = "#7A5AF8",
                strokeWidth = 4f,
                peerId = "joiner_p2"
            )
        )
        hostEngine.receiveMessage(
            CollabMessage.StrokeBroadcast(
                strokeId = "live_stroke_1",
                points = listOf(10f, 10f, 20f, 20f, 35f, 45f),
                colorHex = "#7A5AF8",
                strokeWidth = 4f,
                peerId = "joiner_p2"
            )
        )
        val buffered = hostEngine.consumeBufferedStrokes()
        assertEquals(2, buffered.size)
        assertEquals(4, buffered[0].points.size)
        assertEquals(6, buffered[1].points.size)
    }
}
