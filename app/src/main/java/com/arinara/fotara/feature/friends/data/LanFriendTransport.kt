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
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

/**
 * In-memory and cross-instance peer registry for live Friends discovery, presence status,
 * and follow/friend event delivery.
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
                discoveredPeers[norm] = snap.copy(handle = norm)
                peerTimestamps[norm] = System.currentTimeMillis()
                return discoveredPeers[norm]
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
            return "@$body" to null
        }
        val handlePart = body.substring(0, firstDash).trim()
        val payloadPart = body.substring(firstDash + 1).trim()
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
 * Lightweight LAN UDP broadcast & unicast transport for live multi-device Friends discovery,
 * presence status synchronization, and follow/friend notifications.
 */
class LanFriendTransport(
    private val listener: FriendPeerRegistry.PeerListener
) : AutoCloseable {

    companion object {
        const val FRIEND_UDP_PORT = 44830
        private const val HEARTBEAT_INTERVAL_MS = 4_000L
        private const val MSG_ANNOUNCE = "FRIEND_ANNOUNCE"
        private const val MSG_WHO_IS = "FRIEND_WHO_IS"
        private const val MSG_CONNECT = "FRIEND_CONNECT"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var udpSocket: DatagramSocket? = null

    private var heartbeatJob: Job? = null

    init {
        FriendPeerRegistry.registerListener(listener)
        startUdpListener()
        startPeriodicHeartbeat()
    }

    fun announceNow() {
        val profile = listener.getLocalProfileSnapshot() ?: return
        if (profile.handle.removePrefix("@").isBlank()) return
        FriendPeerRegistry.recordPeer(profile)
        scope.launch {
            val packetStr = encodeAnnouncePacket(profile)
            sendUdpBroadcast(packetStr)
        }
    }

    fun sendConnectEvent(eventType: String, targetHandle: String) {
        val profile = listener.getLocalProfileSnapshot() ?: return
        val normTarget = FriendPeerRegistry.normalizeHandle(targetHandle)
        if (normTarget.length <= 1) return
        FriendPeerRegistry.dispatchConnectEvent(eventType, profile, normTarget)
        scope.launch {
            val packetStr = "$MSG_CONNECT|$eventType|$normTarget|${profile.toJson()}"
            sendUdpBroadcast(packetStr)
            sendUdpBroadcast(packetStr)
        }
    }

    /**
     * Queries the LAN for [handle] via UDP broadcast and waits up to [timeoutMs] for a live peer response.
     */
    fun queryPeerByHandle(handle: String, timeoutMs: Long = 450L): FriendProfile? {
        val norm = FriendPeerRegistry.normalizeHandle(handle)
        if (norm.length <= 1) return null

        FriendPeerRegistry.findByHandle(norm)?.let { existing ->
            if (existing.studyStatus != StudyPresenceStatus.OFFLINE) {
                return existing
            }
        }

        startUdpListener()
        val localSnap = listener.getLocalProfileSnapshot()
        val whoIsPacket = if (localSnap != null && localSnap.handle.removePrefix("@").isNotBlank()) {
            "$MSG_WHO_IS|$norm|${localSnap.toJson()}"
        } else {
            "$MSG_WHO_IS|$norm|"
        }

        val deadline = System.currentTimeMillis() + timeoutMs.coerceIn(100L, 1_500L)
        var attempts = 0
        while (System.currentTimeMillis() < deadline) {
            if (attempts == 0 || attempts == 3) {
                sendUdpBroadcast(whoIsPacket)
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
            while (isActive) {
                val profile = listener.getLocalProfileSnapshot()
                if (profile != null && profile.handle.removePrefix("@").isNotBlank()) {
                    FriendPeerRegistry.recordPeer(profile)
                    sendUdpBroadcast(encodeAnnouncePacket(profile))
                }
                delay(HEARTBEAT_INTERVAL_MS)
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
                    bind(InetSocketAddress(FRIEND_UDP_PORT))
                }
                udpSocket = socket
                val buffer = ByteArray(8192)
                while (isActive && !socket.isClosed) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val raw = String(packet.data, packet.offset, packet.length, StandardCharsets.UTF_8).trim()
                    handleIncomingPacket(raw, packet.address)
                }
            } catch (_: Exception) {
                // UDP port may be unavailable in pure JVM test environment
            }
        }
    }

    private fun handleIncomingPacket(raw: String, senderAddress: InetAddress?) {
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
                    sendUdpBroadcast(response)
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
                    sendUdpBroadcast(response)
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
                        val packet = DatagramPacket(bytes, bytes.size, addr, FRIEND_UDP_PORT)
                        sender.send(packet)
                    } catch (_: Exception) {
                    }
                }
            }
        } catch (_: Exception) {
        }
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
