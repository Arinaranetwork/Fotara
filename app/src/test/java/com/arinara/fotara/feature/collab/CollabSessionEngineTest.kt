// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.collab

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
    fun testJoinRoomWithValidCode() {
        val success = engine.joinRoom("FT-8821", userTag = "CollaboratorBob", colorHex = "#EFE8DA")
        assertTrue(success)
        assertEquals(CollabSessionState.IN_ROOM, engine.state.value)
        assertEquals("FT-8821", engine.roomCode.value)

        val peers = engine.activePeers.value
        assertEquals(1, peers.size)
        val localPeer = peers["local_p1"]
        assertNotNull(localPeer)
        assertEquals("CollaboratorBob", localPeer?.userTag)
        assertEquals("#EFE8DA", localPeer?.colorHex)

        // Verify JoinRoom and PeerJoined dispatched
        val joinMsg = dispatchedMessages.filterIsInstance<CollabMessage.JoinRoom>().firstOrNull()
        assertNotNull(joinMsg)
        assertEquals("FT-8821", joinMsg?.roomCode)

        val peerJoinedMsg = dispatchedMessages.filterIsInstance<CollabMessage.PeerJoined>().firstOrNull()
        assertNotNull(peerJoinedMsg)
        assertEquals("local_p1", peerJoinedMsg?.peerId)
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
}
