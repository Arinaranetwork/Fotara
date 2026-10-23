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
import java.net.NetworkInterface
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
        val lanIp: String = "",
        val lanPort: Int = 0,
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
        lanIp: String = "",
        lanPort: Int = 0,
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
            lanIp = lanIp.ifBlank { existing?.lanIp ?: "" },
            lanPort = if (lanPort > 0) lanPort else (existing?.lanPort ?: 0),
            bannedPeerIds = existing?.bannedPeerIds ?: emptySet(),
            bannedDeviceIds = existing?.bannedDeviceIds ?: emptySet(),
            engineRef = engineRef ?: existing?.engineRef
        )
        return true
    }

    fun registerRemoteHostAnnouncement(announce: CollabMessage.HostAnnounce) {
        val key = announce.roomCode.trim().uppercase()
        if (key.isBlank() || announce.hostPeerId.isBlank()) return
        val existing = activeRooms[key]
        activeRooms[key] = HostedRoomRecord(
            roomCode = key,
            sessionToken = announce.sessionToken.ifBlank { existing?.sessionToken ?: "" },
            hostPeerId = announce.hostPeerId,
            hostUserName = announce.hostUserName.ifBlank { existing?.hostUserName ?: "Host" },
            createdAt = if (announce.createdAt > 0L) announce.createdAt else (existing?.createdAt ?: System.currentTimeMillis()),
            lanIp = announce.lanIp.ifBlank { existing?.lanIp ?: "" },
            lanPort = if (announce.lanPort > 0) announce.lanPort else (existing?.lanPort ?: 0),
            bannedPeerIds = existing?.bannedPeerIds ?: emptySet(),
            bannedDeviceIds = existing?.bannedDeviceIds ?: emptySet(),
            engineRef = existing?.engineRef
        )
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

    init {
        startUdpListener()
    }

    /**
     * Starts the UDP discovery listener for Joiners looking for a Central Host (`HOST_ANNOUNCE`).
     */
    fun startJoinerDiscovery() {
        startUdpListener()
    }

    /**
     * Starts Central Host TCP server and UDP discovery responder.
     * Binds [ServerSocket] synchronously so [localTcpPort] is immediately valid when
     * `createRoom` broadcasts `HostAnnounce`.
     */
    fun startHostServer() {
        var boundServer: ServerSocket? = tcpServerSocket
        if (boundServer == null || boundServer.isClosed) {
            boundServer = try {
                ServerSocket(0).also { tcpServerSocket = it }
            } catch (_: Exception) {
                null
            }
        }
        if (boundServer != null) {
            scope.launch {
                while (isActive && !boundServer.isClosed) {
                    try {
                        val client = boundServer.accept()
                        client.tcpNoDelay = true
                        connectedJoinerSockets.add(client)
                        handleAcceptedJoinerSocket(client)
                    } catch (_: Exception) {
                        break
                    }
                }
            }
        }
        startUdpListener()
    }

    /**
     * Broadcasts a LAN `WhoHasRoom` discovery probe and waits up to [timeoutMs] for an active
     * Central Host to respond with `HostAnnounce`.
     */
    fun probeRoomHost(
        roomCode: String,
        joinerPeerId: String,
        joinerDeviceId: String,
        joinerUserName: String,
        timeoutMs: Long = 650L
    ): CollabRoomRegistry.HostedRoomRecord? {
        val normalized = roomCode.trim().uppercase()
        CollabRoomRegistry.findRoom(normalized)?.let { existing ->
            if (existing.hostPeerId != joinerPeerId) return existing
        }

        startUdpListener()
        val whoHasPayload = CollabProtocolCodec.encode(
            CollabMessage.WhoHasRoom(
                roomCode = normalized,
                joinerPeerId = joinerPeerId,
                joinerDeviceId = joinerDeviceId,
                joinerUserName = joinerUserName
            )
        )
        val probePayload = CollabProtocolCodec.encode(
            CollabMessage.RoomProbe(
                roomCode = normalized,
                requesterPeerId = joinerPeerId
            )
        )

        val deadline = System.currentTimeMillis() + timeoutMs.coerceIn(100L, 2_500L)
        var sentCount = 0
        while (System.currentTimeMillis() < deadline) {
            if (sentCount == 0 || sentCount == 4) {
                sendUdpBroadcast(whoHasPayload)
                sendUdpBroadcast(probePayload)
            }
            sentCount++
            try {
                Thread.sleep(50L)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            }
            val discovered = CollabRoomRegistry.findRoom(normalized)
            if (discovered != null && discovered.hostPeerId != joinerPeerId) {
                return discovered
            }
        }
        return CollabRoomRegistry.findRoom(normalized)?.takeIf { it.hostPeerId != joinerPeerId }
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
                is CollabMessage.HostAnnounce,
                is CollabMessage.RoomClosed -> {
                    startUdpListener()
                    sendUdpBroadcast(encoded)
                    if (message is CollabMessage.RoomClosed) {
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
                    if (decoded is CollabMessage.HostAnnounce) {
                        val senderIp = packet.address?.hostAddress ?: ""
                        val resolvedAnnounce = if (decoded.lanIp.isBlank() && senderIp.isNotBlank()) {
                            decoded.copy(lanIp = senderIp)
                        } else {
                            decoded
                        }
                        CollabRoomRegistry.registerRemoteHostAnnouncement(resolvedAnnounce)
                        onMessageReceived(resolvedAnnounce)
                    } else if (decoded is CollabMessage.RoomClosed) {
                        CollabRoomRegistry.unregisterRoom(decoded.roomCode, decoded.hostPeerId)
                        onMessageReceived(decoded)
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
            val targets = LinkedHashSet<InetAddress>()
            try {
                targets.add(InetAddress.getByName("255.255.255.255"))
            } catch (_: Exception) {
            }
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces()
                while (interfaces != null && interfaces.hasMoreElements()) {
                    val netIf = interfaces.nextElement()
                    if (!netIf.isUp || netIf.isLoopback) continue
                    for (ifAddr in netIf.interfaceAddresses) {
                        val bcast = ifAddr.broadcast
                        if (bcast != null) {
                            targets.add(bcast)
                        }
                    }
                }
            } catch (_: Exception) {
            }

            DatagramSocket().use { sender ->
                sender.broadcast = true
                for (addr in targets) {
                    try {
                        val packet = DatagramPacket(bytes, bytes.size, addr, DISCOVERY_UDP_PORT)
                        sender.send(packet)
                    } catch (_: Exception) {
                    }
                }
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
