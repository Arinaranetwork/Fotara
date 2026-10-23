// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.collab.engine

import com.arinara.fotara.feature.collab.model.CollabMessage
import com.arinara.fotara.feature.collab.model.CollabProtocolCodec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArraySet

/**
 * In-memory and local-process registry for active Central Host rooms.
 * Enables deterministic custom room-code collision detection (`ROOM_PROBE`),
 * host lookup (`WHO_HAS_ROOM`), and ban-list enforcement across engine instances.
 */
object CollabRoomRegistry {

    data class HostedRoomRecord(
        val roomCode: String,
        val sessionToken: String,
        val hostPeerId: String,
        val hostUserName: String,
        val createdAt: Long,
        val bannedPeerIds: Set<String> = emptySet(),
        val bannedDeviceIds: Set<String> = emptySet(),
        val engineRef: CollabSessionEngine? = null
    )

    private val activeRooms = ConcurrentHashMap<String, HostedRoomRecord>()

    fun findRoom(roomCode: String): HostedRoomRecord? {
        return activeRooms[roomCode.trim().uppercase()]
    }

    fun registerHostRoom(
        roomCode: String,
        sessionToken: String,
        hostPeerId: String,
        hostUserName: String,
        createdAt: Long = System.currentTimeMillis(),
        engineRef: CollabSessionEngine? = null
    ): Boolean {
        val key = roomCode.trim().uppercase()
        val existing = activeRooms[key]
        if (existing != null && existing.hostPeerId != hostPeerId) {
            return false
        }
        activeRooms[key] = HostedRoomRecord(
            roomCode = key,
            sessionToken = sessionToken,
            hostPeerId = hostPeerId,
            hostUserName = hostUserName,
            createdAt = createdAt,
            bannedPeerIds = existing?.bannedPeerIds ?: emptySet(),
            bannedDeviceIds = existing?.bannedDeviceIds ?: emptySet(),
            engineRef = engineRef
        )
        return true
    }

    fun updateHostOwner(
        roomCode: String,
        newHostPeerId: String,
        newHostUserName: String
    ) {
        val key = roomCode.trim().uppercase()
        activeRooms.computeIfPresent(key) { _, record ->
            record.copy(
                hostPeerId = newHostPeerId,
                hostUserName = newHostUserName
            )
        }
    }

    fun updateRoomBanList(
        roomCode: String,
        bannedPeerIds: Set<String>,
        bannedDeviceIds: Set<String>
    ) {
        val key = roomCode.trim().uppercase()
        activeRooms.computeIfPresent(key) { _, record ->
            record.copy(
                bannedPeerIds = bannedPeerIds,
                bannedDeviceIds = bannedDeviceIds
            )
        }
    }

    fun isPeerBanned(roomCode: String, peerId: String, deviceId: String): Boolean {
        val record = activeRooms[roomCode.trim().uppercase()] ?: return false
        return peerId in record.bannedPeerIds || (deviceId.isNotBlank() && deviceId in record.bannedDeviceIds)
    }

    fun unregisterRoom(roomCode: String, hostPeerId: String? = null) {
        val key = roomCode.trim().uppercase()
        val existing = activeRooms[key] ?: return
        if (hostPeerId == null || existing.hostPeerId == hostPeerId) {
            activeRooms.remove(key)
        }
    }

    fun clearAllForTesting() {
        activeRooms.clear()
    }
}

/**
 * Lightweight, low-CPU LAN/Hotspot transport for Central-Host collaborative sessions.
 * Uses UDP broadcast on port [DISCOVERY_UDP_PORT] for zero-configuration room discovery
 * (`WHO_HAS_ROOM` -> `HOST_ANNOUNCE`) and a direct TCP [ServerSocket] on the Central Host
 * for reliable low-latency stroke, chat, and moderation frame delivery.
 */
class LanCollabTransport(
    private val onMessageReceived: (CollabMessage) -> Unit
) : AutoCloseable {

    companion object {
        const val DISCOVERY_UDP_PORT = 44829
        private const val SOCKET_TIMEOUT_MS = 3_000
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var udpSocket: DatagramSocket? = null

    @Volatile
    private var tcpServerSocket: ServerSocket? = null

    @Volatile
    private var clientToHostSocket: Socket? = null

    @Volatile
    private var clientToHostWriter: PrintWriter? = null

    private val connectedJoinerWriters = CopyOnWriteArraySet<PrintWriter>()
    private val connectedJoinerSockets = CopyOnWriteArraySet<Socket>()
    private val pendingClientLines = ConcurrentLinkedQueue<String>()

    val localTcpPort: Int
        get() = tcpServerSocket?.localPort ?: 0

    /**
     * Starts the UDP discovery listener for Joiners looking for a Central Host (`HOST_ANNOUNCE`).
     */
    fun startJoinerDiscovery() {
        startUdpListener()
    }

    /**
     * Starts Central Host TCP server and UDP discovery responder on background IO threads.
     */
    fun startHostServer() {
        scope.launch {
            try {
                if (tcpServerSocket == null || tcpServerSocket?.isClosed == true) {
                    val server = ServerSocket(0)
                    tcpServerSocket = server
                    launch {
                        while (isActive && !server.isClosed) {
                            try {
                                val client = server.accept()
                                client.tcpNoDelay = true
                                connectedJoinerSockets.add(client)
                                handleAcceptedJoinerSocket(client)
                            } catch (_: Exception) {
                                break
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Safe fallback in restricted sandbox/test environments
            }
        }
        startUdpListener()
    }

    /**
     * Connects a Joiner directly to a discovered Central Host TCP endpoint.
     */
    fun connectToHost(hostIp: String, hostPort: Int) {
        if (hostIp.isBlank() || hostPort <= 0) return
        scope.launch {
            try {
                val socket = Socket()
                socket.tcpNoDelay = true
                socket.connect(InetSocketAddress(hostIp, hostPort), SOCKET_TIMEOUT_MS)
                clientToHostSocket = socket
                val writer = PrintWriter(
                    OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),
                    true
                )
                clientToHostWriter = writer

                // Flush any queued handshake frames (JoinRoom / PeerJoined) sent while connecting
                synchronized(writer) {
                    while (true) {
                        val queued = pendingClientLines.poll() ?: break
                        writer.println(queued)
                    }
                }

                BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)).use { reader ->
                    while (isActive && !socket.isClosed) {
                        val line = reader.readLine() ?: break
                        val decoded = CollabProtocolCodec.decode(line)
                        if (decoded != null) {
                            onMessageReceived(decoded)
                        }
                    }
                }
            } catch (_: Exception) {
                // Handled by engine timeout / reconnect state
            }
        }
    }

    /**
     * Sends a protocol message over active TCP connections (or UDP for discovery frames).
     */
    fun send(message: CollabMessage) {
        scope.launch {
            val encoded = CollabProtocolCodec.encode(message)
            when (message) {
                is CollabMessage.RoomProbe,
                is CollabMessage.WhoHasRoom,
                is CollabMessage.HostAnnounce -> {
                    startUdpListener()
                    sendUdpBroadcast(encoded)
                }
                else -> {
                    val hostWriter = clientToHostWriter
                    if (hostWriter != null) {
                        try {
                            synchronized(hostWriter) {
                                hostWriter.println(encoded)
                            }
                        } catch (_: Exception) {
                        }
                    } else if (connectedJoinerWriters.isEmpty()) {
                        while (pendingClientLines.size >= 64) {
                            pendingClientLines.poll()
                        }
                        pendingClientLines.offer(encoded)
                    }
                    // Broadcast to all connected Joiners if we are the Host
                    for (writer in connectedJoinerWriters) {
                        try {
                            synchronized(writer) {
                                writer.println(encoded)
                            }
                        } catch (_: Exception) {
                            connectedJoinerWriters.remove(writer)
                        }
                    }
                }
            }
        }
    }

    private fun handleAcceptedJoinerSocket(socket: Socket) {
        scope.launch {
            val writer = try {
                PrintWriter(
                    OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),
                    true
                ).also { connectedJoinerWriters.add(it) }
            } catch (_: Exception) {
                null
            }

            try {
                BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)).use { reader ->
                    while (isActive && !socket.isClosed) {
                        val line = reader.readLine() ?: break
                        val decoded = CollabProtocolCodec.decode(line)
                        if (decoded != null) {
                            onMessageReceived(decoded)
                            // Relay joiner frames across all other connected joiner sockets
                            if (decoded is CollabMessage.StrokeBroadcast ||
                                decoded is CollabMessage.CursorBroadcast ||
                                decoded is CollabMessage.ChatBroadcast ||
                                decoded is CollabMessage.PeerJoined ||
                                decoded is CollabMessage.LeaveRoom ||
                                decoded is CollabMessage.ModMutePeer ||
                                decoded is CollabMessage.BlockPeerChat ||
                                decoded is CollabMessage.DeleteChatMessage ||
                                decoded is CollabMessage.CanvasPermissionSync ||
                                decoded is CollabMessage.RoomSettingsSync ||
                                decoded is CollabMessage.SetPeerRole ||
                                decoded is CollabMessage.TransferHostOwnership ||
                                decoded is CollabMessage.PinChatMessage
                            ) {
                                for (otherWriter in connectedJoinerWriters) {
                                    if (otherWriter !== writer) {
                                        try {
                                            synchronized(otherWriter) {
                                                otherWriter.println(line)
                                            }
                                        } catch (_: Exception) {
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            } finally {
                if (writer != null) {
                    connectedJoinerWriters.remove(writer)
                }
                connectedJoinerSockets.remove(socket)
                try {
                    socket.close()
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun startUdpListener() {
        scope.launch {
            try {
                if (udpSocket != null && udpSocket?.isClosed == false) return@launch
                val socket = DatagramSocket(null).apply {
                    reuseAddress = true
                    broadcast = true
                    bind(InetSocketAddress(DISCOVERY_UDP_PORT))
                }
                udpSocket = socket
                val buffer = ByteArray(8192)
                while (isActive && !socket.isClosed) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val json = String(packet.data, packet.offset, packet.length, StandardCharsets.UTF_8)
                    val decoded = CollabProtocolCodec.decode(json) ?: continue
                    if (decoded is CollabMessage.HostAnnounce && decoded.lanIp.isBlank()) {
                        val senderIp = packet.address?.hostAddress ?: ""
                        onMessageReceived(decoded.copy(lanIp = senderIp))
                    } else {
                        onMessageReceived(decoded)
                    }
                }
            } catch (_: Exception) {
                // UDP port may already be bound or restricted in unit test JVM
            }
        }
    }

    private fun sendUdpBroadcast(payload: String) {
        try {
            val bytes = payload.toByteArray(StandardCharsets.UTF_8)
            DatagramSocket().use { sender ->
                sender.broadcast = true
                val broadcastAddr = InetAddress.getByName("255.255.255.255")
                val packet = DatagramPacket(bytes, bytes.size, broadcastAddr, DISCOVERY_UDP_PORT)
                sender.send(packet)
            }
        } catch (_: Exception) {
        }
    }

    override fun close() {
        try {
            udpSocket?.close()
        } catch (_: Exception) {
        }
        udpSocket = null

        try {
            tcpServerSocket?.close()
        } catch (_: Exception) {
        }
        tcpServerSocket = null

        try {
            clientToHostSocket?.close()
        } catch (_: Exception) {
        }
        clientToHostSocket = null
        clientToHostWriter = null

        for (socket in connectedJoinerSockets) {
            try {
                socket.close()
            } catch (_: Exception) {
            }
        }
        connectedJoinerSockets.clear()
        connectedJoinerWriters.clear()
        scope.cancel()
    }
}
