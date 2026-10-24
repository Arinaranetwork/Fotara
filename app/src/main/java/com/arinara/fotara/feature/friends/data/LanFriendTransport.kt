// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.friends.data

import com.arinara.fotara.feature.friends.model.FriendProfile
import com.arinara.fotara.feature.friends.model.StudyPresenceStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicLong

/**
 * In-memory and cross-instance peer registry for live Friends discovery, presence status,
 * known LAN peer addresses, and follow/friend event delivery.
 */
object FriendPeerRegistry {

    interface PeerListener {
        val localHandle: String
        fun getLocalProfileSnapshot(): FriendProfile?
        fun onRemotePeerAnnounced(profile: FriendProfile)
        fun onRemoteConnectEvent(
            eventType: String,
            senderProfile: FriendProfile,
            targetHandle: String
        )
    }

    private val discoveredPeers = ConcurrentHashMap<String, FriendProfile>()
    private val peerTimestamps = ConcurrentHashMap<String, Long>()
    private val knownPeerAddresses = CopyOnWriteArraySet<InetAddress>()
    private val listeners = CopyOnWriteArraySet<PeerListener>()

    fun normalizeHandle(raw: String): String {
        val trimmed = raw.trim().lowercase()
        if (trimmed.isEmpty()) return ""
        return if (trimmed.startsWith("@")) trimmed else "@$trimmed"
    }

    fun registerListener(listener: PeerListener) {
        listeners.add(listener)
    }

    fun unregisterListener(listener: PeerListener) {
        listeners.remove(listener)
    }

    fun recordKnownPeerAddress(address: InetAddress?) {
        if (address == null || address.isLoopbackAddress || address.isAnyLocalAddress) return
        knownPeerAddresses.add(address)
    }

    fun getKnownPeerAddresses(): Set<InetAddress> = knownPeerAddresses

    fun recordPeer(profile: FriendProfile) {
        val norm = normalizeHandle(profile.handle)
        if (norm.length <= 1) return
        val normalizedProfile = profile.copy(handle = norm)
        discoveredPeers[norm] = normalizedProfile
        peerTimestamps[norm] = System.currentTimeMillis()
        for (listener in listeners) {
            if (!listener.localHandle.equals(norm, ignoreCase = true)) {
                listener.onRemotePeerAnnounced(normalizedProfile)
            }
        }
    }

    fun findByHandle(handle: String): FriendProfile? {
        val norm = normalizeHandle(handle)
        if (norm.length <= 1) return null
        // Check active listeners first for the freshest in-process snapshot
        for (listener in listeners) {
            val snap = listener.getLocalProfileSnapshot()
            if (snap != null && normalizeHandle(snap.handle) == norm) {
                val fresh = snap.copy(handle = norm)
                discoveredPeers[norm] = fresh
                peerTimestamps[norm] = System.currentTimeMillis()
                return fresh
            }
        }
        return discoveredPeers[norm]
    }

    fun dispatchConnectEvent(
        eventType: String,
        senderProfile: FriendProfile,
        targetHandle: String
    ) {
        val normTarget = normalizeHandle(targetHandle)
        recordPeer(senderProfile)
        for (listener in listeners) {
            val listenerHandle = normalizeHandle(listener.localHandle)
            if (listenerHandle == normTarget) {
                listener.onRemoteConnectEvent(eventType, senderProfile, normTarget)
            }
        }
    }

    fun clearAllForTesting() {
        discoveredPeers.clear()
        peerTimestamps.clear()
        knownPeerAddresses.clear()
        listeners.clear()
    }
}

/**
 * Enriched Share Code codec (`FOTARA-FRIEND-<handle>-<base64url>`).
 * Embeds the user's real displayName, pronouns, studyStatus, currentSubject, and counts
 * so pasting a share code populates full profile data even across different subnets.
 */
object FriendShareCodeCodec {
    private const val PREFIX = "FOTARA-FRIEND-"

    fun isShareCode(raw: String): Boolean {
        return raw.trim().startsWith(PREFIX, ignoreCase = true)
    }

    fun encode(profile: FriendProfile): String {
        val cleanHandle = profile.handle.trim().removePrefix("@").ifBlank { "scholar" }
        val json = profile.copy(handle = "@$cleanHandle").toJson()
        val encodedPayload = try {
            Base64.getUrlEncoder().withoutPadding()
                .encodeToString(json.toByteArray(StandardCharsets.UTF_8))
        } catch (_: Exception) {
            Integer.toHexString(cleanHandle.hashCode() and 0xFFFF).uppercase()
        }
        return "$PREFIX$cleanHandle-$encodedPayload"
    }

    fun decode(rawCodeOrHandle: String): Pair<String, FriendProfile?> {
        val trimmed = rawCodeOrHandle.trim()
        if (!trimmed.startsWith(PREFIX, ignoreCase = true)) {
            return trimmed to null
        }
        val body = trimmed.substring(PREFIX.length)
        val firstDash = body.indexOf('-')
        if (firstDash <= 0) {
            return "" to null
        }
        val handlePart = body.substring(0, firstDash).trim()
        val payloadPart = body.substring(firstDash + 1).trim()
        if (handlePart.isBlank() || payloadPart.isBlank()) {
            return "" to null
        }
        val decodedProfile = try {
            val bytes = Base64.getUrlDecoder().decode(payloadPart)
            val json = String(bytes, StandardCharsets.UTF_8)
            FriendProfile.fromJson(json)
        } catch (_: Exception) {
            null
        }
        return "@$handlePart" to decodedProfile
    }
}

/**
 * Lightweight LAN UDP broadcast, gateway unicast, known-peer unicast, and `/24` subnet discovery
 * transport for live multi-device Friends discovery, presence status synchronization, and
 * follow/friend notifications.
 */
class LanFriendTransport(
    private val listener: FriendPeerRegistry.PeerListener
) : AutoCloseable {

    companion object {
        const val FRIEND_UDP_PORT = 44830
        private const val HEARTBEAT_INTERVAL_MS = 4_000L
        private const val SUBNET_SWEEP_MIN_INTERVAL_MS = 2_000L
        private const val MSG_ANNOUNCE = "FRIEND_ANNOUNCE"
        private const val MSG_WHO_IS = "FRIEND_WHO_IS"
        private const val MSG_CONNECT = "FRIEND_CONNECT"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var udpSocket: DatagramSocket? = null

    private var heartbeatJob: Job? = null
    private val lastSubnetSweepMs = AtomicLong(0L)

    init {
        FriendPeerRegistry.registerListener(listener)
        ensureUdpSocket()
        startPeriodicHeartbeat()
    }

    fun announceNow() {
        val profile = listener.getLocalProfileSnapshot() ?: return
        if (profile.handle.removePrefix("@").isBlank()) return
        FriendPeerRegistry.recordPeer(profile)
        ensureUdpSocket()
        scope.launch {
            val packetStr = encodeAnnouncePacket(profile)
            sendUdpDiscovery(packetStr, includeSubnetSweep = false)
        }
    }

    /**
     * Broadcasts our presence and actively queries all peers on the LAN (`FRIEND_WHO_IS|*`)
     * using broadcast, known-peer unicast, gateway unicast, and `/24` subnet unicast sweep.
     */
    fun refreshPeersNow() {
        ensureUdpSocket()
        val profile = listener.getLocalProfileSnapshot()
        if (profile != null && profile.handle.removePrefix("@").isNotBlank()) {
            FriendPeerRegistry.recordPeer(profile)
        }
        scope.launch {
            if (profile != null && profile.handle.removePrefix("@").isNotBlank()) {
                sendUdpDiscovery(encodeAnnouncePacket(profile), includeSubnetSweep = false)
            }
            val whoIs = if (profile != null && profile.handle.removePrefix("@").isNotBlank()) {
                "$MSG_WHO_IS|*|${profile.toJson()}"
            } else {
                "$MSG_WHO_IS|*|"
            }
            sendUdpDiscovery(whoIs, includeSubnetSweep = true)
        }
    }

    fun sendConnectEvent(eventType: String, targetHandle: String) {
        val profile = listener.getLocalProfileSnapshot() ?: return
        val normTarget = FriendPeerRegistry.normalizeHandle(targetHandle)
        if (normTarget.length <= 1) return
        FriendPeerRegistry.dispatchConnectEvent(eventType, profile, normTarget)
        ensureUdpSocket()
        scope.launch {
            val packetStr = "$MSG_CONNECT|$eventType|$normTarget|${profile.toJson()}"
            sendUdpDiscovery(packetStr, includeSubnetSweep = true)
            delay(60L)
            sendUdpDiscovery(packetStr, includeSubnetSweep = false)
        }
    }

    /**
     * Queries the LAN for [handle] via UDP broadcast + `/24` unicast sweep and waits up to
     * [timeoutMs] for a live peer response.
     */
    fun queryPeerByHandle(handle: String, timeoutMs: Long = 750L): FriendProfile? {
        val norm = FriendPeerRegistry.normalizeHandle(handle)
        if (norm.length <= 1) return null

        FriendPeerRegistry.findByHandle(norm)?.let { existing ->
            if (existing.studyStatus != StudyPresenceStatus.OFFLINE) {
                return existing
            }
        }

        ensureUdpSocket()
        val localSnap = listener.getLocalProfileSnapshot()
        val whoIsSpecific = if (localSnap != null && localSnap.handle.removePrefix("@").isNotBlank()) {
            "$MSG_WHO_IS|$norm|${localSnap.toJson()}"
        } else {
            "$MSG_WHO_IS|$norm|"
        }
        val whoIsAll = if (localSnap != null && localSnap.handle.removePrefix("@").isNotBlank()) {
            "$MSG_WHO_IS|*|${localSnap.toJson()}"
        } else {
            "$MSG_WHO_IS|*|"
        }

        val deadline = System.currentTimeMillis() + timeoutMs.coerceIn(100L, 2_000L)
        var attempts = 0
        while (System.currentTimeMillis() < deadline) {
            if (attempts == 0) {
                sendUdpDiscovery(whoIsSpecific, includeSubnetSweep = true)
                sendUdpDiscovery(whoIsAll, includeSubnetSweep = false)
            } else if (attempts == 4) {
                sendUdpDiscovery(whoIsSpecific, includeSubnetSweep = false)
            }
            attempts++
            try {
                Thread.sleep(45L)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            }
            val found = FriendPeerRegistry.findByHandle(norm)
            if (found != null) {
                return found
            }
        }
        return FriendPeerRegistry.findByHandle(norm)
    }

    private fun startPeriodicHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            var tick = 0
            while (isActive) {
                ensureUdpSocket()
                val profile = listener.getLocalProfileSnapshot()
                if (profile != null && profile.handle.removePrefix("@").isNotBlank()) {
                    FriendPeerRegistry.recordPeer(profile)
                    sendUdpDiscovery(encodeAnnouncePacket(profile), includeSubnetSweep = false)
                    if (tick % 3 == 0) {
                        val whoIs = "$MSG_WHO_IS|*|${profile.toJson()}"
                        sendUdpDiscovery(whoIs, includeSubnetSweep = (tick == 0))
                    }
                } else if (tick % 3 == 0) {
                    sendUdpDiscovery("$MSG_WHO_IS|*|", includeSubnetSweep = (tick == 0))
                }
                tick++
                delay(HEARTBEAT_INTERVAL_MS)
            }
        }
    }

    @Synchronized
    private fun ensureUdpSocket(): DatagramSocket? {
        val existing = udpSocket
        if (existing != null && !existing.isClosed) {
            return existing
        }
        return try {
            val socket = DatagramSocket(null).apply {
                reuseAddress = true
                broadcast = true
                bind(InetSocketAddress(FRIEND_UDP_PORT))
            }
            udpSocket = socket
            scope.launch {
                val buffer = ByteArray(8192)
                try {
                    while (isActive && !socket.isClosed) {
                        val packet = DatagramPacket(buffer, buffer.size)
                        socket.receive(packet)
                        val raw = String(packet.data, packet.offset, packet.length, StandardCharsets.UTF_8).trim()
                        handleIncomingPacket(raw, packet.address)
                    }
                } catch (_: Exception) {
                }
            }
            socket
        } catch (_: Exception) {
            // UDP port may be unavailable in pure JVM test environment
            null
        }
    }

    private fun handleIncomingPacket(raw: String, senderAddress: InetAddress?) {
        FriendPeerRegistry.recordKnownPeerAddress(senderAddress)
        when {
            raw.startsWith("$MSG_ANNOUNCE|") -> {
                val json = raw.substringAfter("$MSG_ANNOUNCE|")
                val profile = FriendProfile.fromJson(json) ?: return
                FriendPeerRegistry.recordPeer(profile)
            }
            raw.startsWith("$MSG_WHO_IS|") -> {
                val remainder = raw.substringAfter("$MSG_WHO_IS|")
                val targetHandle = remainder.substringBefore("|").trim()
                val requesterJson = remainder.substringAfter("|", "").trim()
                if (requesterJson.startsWith("{")) {
                    FriendProfile.fromJson(requesterJson)?.let { requesterProfile ->
                        FriendPeerRegistry.recordPeer(requesterProfile)
                    }
                }
                val myProfile = listener.getLocalProfileSnapshot() ?: return
                val myNorm = FriendPeerRegistry.normalizeHandle(myProfile.handle)
                if (myNorm.length > 1 && (targetHandle == "*" || targetHandle.equals(myNorm, ignoreCase = true))) {
                    val response = encodeAnnouncePacket(myProfile)
                    if (senderAddress != null) {
                        sendUdpUnicast(response, senderAddress)
                    }
                    sendUdpDiscovery(response, includeSubnetSweep = false)
                }
            }
            raw.startsWith("$MSG_CONNECT|") -> {
                val parts = raw.split("|", limit = 4)
                if (parts.size < 4) return
                val eventType = parts[1]
                val targetHandle = parts[2]
                val senderProfile = FriendProfile.fromJson(parts[3]) ?: return
                FriendPeerRegistry.dispatchConnectEvent(eventType, senderProfile, targetHandle)
                val myProfile = listener.getLocalProfileSnapshot()
                if (myProfile != null && FriendPeerRegistry.normalizeHandle(myProfile.handle).equals(targetHandle, ignoreCase = true)) {
                    val response = encodeAnnouncePacket(myProfile)
                    if (senderAddress != null) {
                        sendUdpUnicast(response, senderAddress)
                    }
                    sendUdpDiscovery(response, includeSubnetSweep = false)
                }
            }
        }
    }

    private fun encodeAnnouncePacket(profile: FriendProfile): String {
        return "$MSG_ANNOUNCE|${profile.toJson()}"
    }

    private fun sendUdpUnicast(payload: String, target: InetAddress) {
        try {
            val bytes = payload.toByteArray(StandardCharsets.UTF_8)
            DatagramSocket().use { sender ->
                val packet = DatagramPacket(bytes, bytes.size, target, FRIEND_UDP_PORT)
                sender.send(packet)
            }
        } catch (_: Exception) {
        }
    }

    private fun sendUdpDiscovery(payload: String, includeSubnetSweep: Boolean) {
        try {
            val bytes = payload.toByteArray(StandardCharsets.UTF_8)
            val targets = LinkedHashSet<InetAddress>()
            val sweepSubnets = ArrayList<ByteArray>()

            // 1. Add known peer unicast addresses first
            targets.addAll(FriendPeerRegistry.getKnownPeerAddresses())

            // 2. Add global broadcast
            try {
                targets.add(InetAddress.getByName("255.255.255.255"))
            } catch (_: Exception) {
            }

            // 3. Inspect active network interfaces for broadcast, calculated subnet broadcast, and gateway .1
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
                        val inetAddr = ifAddr.address
                        if (inetAddr is Inet4Address && !inetAddr.isLoopbackAddress) {
                            val rawIp = inetAddr.address
                            if (rawIp.size == 4) {
                                // Calculate subnet broadcast and gateway from prefix
                                val prefix = ifAddr.networkPrefixLength.toInt().coerceIn(8, 30)
                                val mask = if (prefix == 0) 0 else (-1 shl (32 - prefix))
                                val ipInt = ((rawIp[0].toInt() and 0xFF) shl 24) or
                                    ((rawIp[1].toInt() and 0xFF) shl 16) or
                                    ((rawIp[2].toInt() and 0xFF) shl 8) or
                                    (rawIp[3].toInt() and 0xFF)
                                val bcastInt = ipInt or mask.inv()
                                val gatewayInt = (ipInt and mask) or 1
                                try {
                                    targets.add(intToInet4Address(bcastInt))
                                    targets.add(intToInet4Address(gatewayInt))
                                    // Also add /24 broadcast & gateway in case hotspot reports /16 or /32
                                    targets.add(
                                        InetAddress.getByAddress(
                                            byteArrayOf(rawIp[0], rawIp[1], rawIp[2], 255.toByte())
                                        )
                                    )
                                    targets.add(
                                        InetAddress.getByAddress(
                                            byteArrayOf(rawIp[0], rawIp[1], rawIp[2], 1.toByte())
                                        )
                                    )
                                } catch (_: Exception) {
                                }
                                if (isPrivateIpv4(rawIp)) {
                                    sweepSubnets.add(rawIp)
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            }

            DatagramSocket().use { sender ->
                sender.broadcast = true
                for (addr in targets) {
                    try {
                        val packet = DatagramPacket(bytes, bytes.size, addr, FRIEND_UDP_PORT)
                        sender.send(packet)
                    } catch (_: Exception) {
                    }
                }

                // 4. Optional /24 unicast sweep when Wi-Fi AP / Android packet filter drops UDP broadcasts
                if (includeSubnetSweep && sweepSubnets.isNotEmpty()) {
                    val now = System.currentTimeMillis()
                    val prev = lastSubnetSweepMs.get()
                    if (now - prev >= SUBNET_SWEEP_MIN_INTERVAL_MS && lastSubnetSweepMs.compareAndSet(prev, now)) {
                        for (rawIp in sweepSubnets.take(2)) {
                            val selfHost = rawIp[3].toInt() and 0xFF
                            for (host in 1..254) {
                                if (host == selfHost) continue
                                try {
                                    val dest = InetAddress.getByAddress(
                                        byteArrayOf(rawIp[0], rawIp[1], rawIp[2], host.toByte())
                                    )
                                    val packet = DatagramPacket(bytes, bytes.size, dest, FRIEND_UDP_PORT)
                                    sender.send(packet)
                                } catch (_: Exception) {
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun isPrivateIpv4(ip: ByteArray): Boolean {
        val b0 = ip[0].toInt() and 0xFF
        val b1 = ip[1].toInt() and 0xFF
        return b0 == 10 ||
            (b0 == 172 && b1 in 16..31) ||
            (b0 == 192 && b1 == 168)
    }

    private fun intToInet4Address(value: Int): InetAddress {
        return InetAddress.getByAddress(
            byteArrayOf(
                ((value ushr 24) and 0xFF).toByte(),
                ((value ushr 16) and 0xFF).toByte(),
                ((value ushr 8) and 0xFF).toByte(),
                (value and 0xFF).toByte()
            )
        )
    }

    override fun close() {
        FriendPeerRegistry.unregisterListener(listener)
        heartbeatJob?.cancel()
        try {
            udpSocket?.close()
        } catch (_: Exception) {
        }
        udpSocket = null
        scope.cancel()
    }
}
