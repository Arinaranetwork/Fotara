// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.collab

import com.arinara.fotara.feature.collab.model.CollabMessage
import com.arinara.fotara.feature.collab.model.CollabPeer
import com.arinara.fotara.feature.collab.model.CollabProtocolCodec
import com.arinara.fotara.feature.collab.model.CollabRoomCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests covering room code validation, normalization, and protocol serialization.
 */
class CollabProtocolTest {

    @Test
    fun testRoomCodeValidation() {
        // Valid patterns
        assertTrue(CollabRoomCode.isValid("FT-8821"))
        assertTrue(CollabRoomCode.isValid("FT-ABCD"))
        assertTrue(CollabRoomCode.isValid("FT-0000"))
        assertTrue(CollabRoomCode.isValid("FT-ZZZZ"))
        assertTrue(CollabRoomCode.isValid("FT-A1B2"))
        assertTrue(CollabRoomCode.isValid("  ft-8821  ")) // case and trim insensitive

        // Invalid patterns
        assertFalse(CollabRoomCode.isValid(null))
        assertFalse(CollabRoomCode.isValid(""))
        assertFalse(CollabRoomCode.isValid("   "))
        assertFalse(CollabRoomCode.isValid("FT-"))
        assertFalse(CollabRoomCode.isValid("FT-123")) // 3 chars
        assertFalse(CollabRoomCode.isValid("FT-12345")) // 5 chars
        assertFalse(CollabRoomCode.isValid("8821")) // missing prefix
        assertFalse(CollabRoomCode.isValid("FT8821")) // missing hyphen
        assertFalse(CollabRoomCode.isValid("FT-AB@#")) // special characters
        assertFalse(CollabRoomCode.isValid("ROOM-1234"))
    }

    @Test
    fun testRoomCodeNormalization() {
        assertEquals("FT-8821", CollabRoomCode.normalize("8821"))
        assertEquals("FT-ABCD", CollabRoomCode.normalize("ft-abcd"))
        assertEquals("FT-ABCD", CollabRoomCode.normalize("FTABCD"))
        assertEquals("FT-1234", CollabRoomCode.normalize("  1234  "))
        assertEquals("FT-A1B2", CollabRoomCode.normalize("a1b2"))
        assertEquals("", CollabRoomCode.normalize(null))
        assertEquals("", CollabRoomCode.normalize(""))
    }

    @Test
    fun testRandomRoomCodeGeneration() {
        val generatedCodes = mutableSetOf<String>()
        for (i in 1..50) {
            val code = CollabRoomCode.generateRandomCode()
            assertTrue("Generated code $code should be valid", CollabRoomCode.isValid(code))
            generatedCodes.add(code)
        }
        // At least several unique codes should be generated across 50 attempts
        assertTrue(generatedCodes.size > 20)
    }

    @Test
    fun testCollabPeerDataModel() {
        val peer = CollabPeer(
            peerId = "peer_101",
            userTag = "Alice",
            colorHex = "#2563EB",
            lastCursorX = 120.5f,
            lastCursorY = 340.2f,
            lastActiveMs = 1700000000000L
        )

        assertEquals("peer_101", peer.peerId)
        assertEquals("Alice", peer.userTag)
        assertEquals("#2563EB", peer.colorHex)
        assertEquals(120.5f, peer.lastCursorX)
        assertEquals(340.2f, peer.lastCursorY)
        assertEquals(1700000000000L, peer.lastActiveMs)

        val updated = peer.copy(lastCursorX = 150f, lastCursorY = 360f)
        assertEquals(150f, updated.lastCursorX)
        assertEquals(360f, updated.lastCursorY)
    }

    @Test
    fun testCollabProtocolCodecJoinRoom() {
        val message = CollabMessage.JoinRoom(
            roomCode = "FT-9912",
            userTag = "Prof Smith",
            colorHex = "#EFE8DA"
        )
        val encoded = CollabProtocolCodec.encode(message)
        assertTrue(encoded.contains("\"type\":\"JOIN_ROOM\""))
        assertTrue(encoded.contains("\"roomCode\":\"FT-9912\""))

        val decoded = CollabProtocolCodec.decode(encoded)
        assertNotNull(decoded)
        assertTrue(decoded is CollabMessage.JoinRoom)
        val joinDecoded = decoded as CollabMessage.JoinRoom
        assertEquals("FT-9912", joinDecoded.roomCode)
        assertEquals("Prof Smith", joinDecoded.userTag)
        assertEquals("#EFE8DA", joinDecoded.colorHex)
    }

    @Test
    fun testCollabProtocolCodecPeerJoined() {
        val message = CollabMessage.PeerJoined(
            peerId = "p_445",
            userTag = "Bob",
            colorHex = "#2563EB"
        )
        val encoded = CollabProtocolCodec.encode(message)
        val decoded = CollabProtocolCodec.decode(encoded)
        assertNotNull(decoded)
        assertTrue(decoded is CollabMessage.PeerJoined)
        val peerDecoded = decoded as CollabMessage.PeerJoined
        assertEquals("p_445", peerDecoded.peerId)
        assertEquals("Bob", peerDecoded.userTag)
        assertEquals("#2563EB", peerDecoded.colorHex)
    }

    @Test
    fun testCollabProtocolCodecStrokeBroadcast() {
        val points = listOf(10.0f, 20.0f, 30.0f, 40.0f, 50.0f, 60.0f)
        val message = CollabMessage.StrokeBroadcast(
            strokeId = "stroke_778",
            points = points,
            colorHex = "#2A9D8F",
            strokeWidth = 5.5f
        )
        val encoded = CollabProtocolCodec.encode(message)
        val decoded = CollabProtocolCodec.decode(encoded)
        assertNotNull(decoded)
        assertTrue(decoded is CollabMessage.StrokeBroadcast)
        val strokeDecoded = decoded as CollabMessage.StrokeBroadcast
        assertEquals("stroke_778", strokeDecoded.strokeId)
        assertEquals(points, strokeDecoded.points)
        assertEquals("#2A9D8F", strokeDecoded.colorHex)
        assertEquals(5.5f, strokeDecoded.strokeWidth)
    }

    @Test
    fun testCollabProtocolCodecCursorBroadcast() {
        val message = CollabMessage.CursorBroadcast(
            peerId = "peer_990",
            x = 245.5f,
            y = 512.0f,
            timestamp = 1700000005000L
        )
        val encoded = CollabProtocolCodec.encode(message)
        val decoded = CollabProtocolCodec.decode(encoded)
        assertNotNull(decoded)
        assertTrue(decoded is CollabMessage.CursorBroadcast)
        val cursorDecoded = decoded as CollabMessage.CursorBroadcast
        assertEquals("peer_990", cursorDecoded.peerId)
        assertEquals(245.5f, cursorDecoded.x)
        assertEquals(512.0f, cursorDecoded.y)
        assertEquals(1700000005000L, cursorDecoded.timestamp)
    }

    @Test
    fun testCollabProtocolCodecLeaveRoom() {
        val message = CollabMessage.LeaveRoom(peerId = "peer_990")
        val encoded = CollabProtocolCodec.encode(message)
        val decoded = CollabProtocolCodec.decode(encoded)
        assertNotNull(decoded)
        assertTrue(decoded is CollabMessage.LeaveRoom)
        val leaveDecoded = decoded as CollabMessage.LeaveRoom
        assertEquals("peer_990", leaveDecoded.peerId)
    }

    @Test
    fun testCollabProtocolCodecMalformedInput() {
        assertNull(CollabProtocolCodec.decode(""))
        assertNull(CollabProtocolCodec.decode("not a json"))
        assertNull(CollabProtocolCodec.decode("{\"type\":\"UNKNOWN_TYPE\"}"))
    }
}
