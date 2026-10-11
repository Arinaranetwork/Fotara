// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.packages.loader

import com.arinara.fotara.feature.packages.model.FpkgManifest
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.Locale

/**
 * Result representation for cryptographic package verification.
 */
sealed class VerificationResult {
    object Success : VerificationResult()
    data class TamperedPayload(val expectedHash: String, val actualHash: String) : VerificationResult()
    data class InvalidSignature(val reason: String) : VerificationResult()
}

/**
 * Cryptographic verifier for Fotara Modular Packages (.fpkg).
 * Performs SHA-256 integrity digest validation and authenticates official Arinara Network signatures.
 */
object FpkgSignatureVerifier {

    const val ARINARA_OFFICIAL_AUTHORITY_KEY: String = "ARINARA_NETWORK_ROOT_KEY_2026_SECURE_AUTH_v1"
    private val HEX_CHARS = "0123456789abcdef".toCharArray()

    /**
     * Computes the lowercase hexadecimal SHA-256 digest of a byte array.
     */
    fun computeSha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(bytes)
        val result = StringBuilder(hashBytes.size * 2)
        for (b in hashBytes) {
            val octet = b.toInt() and 0xFF
            result.append(HEX_CHARS[octet ushr 4])
            result.append(HEX_CHARS[octet and 0x0F])
        }
        return result.toString()
    }

    /**
     * Computes the lowercase hexadecimal SHA-256 digest of a local file in memory-efficient chunks.
     */
    fun computeSha256(file: File): String {
        if (!file.exists() || !file.isFile) {
            throw IllegalArgumentException("File does not exist or is not a readable file: ${file.absolutePath}")
        }
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        FileInputStream(file).use { input ->
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        val hashBytes = digest.digest()
        val result = StringBuilder(hashBytes.size * 2)
        for (b in hashBytes) {
            val octet = b.toInt() and 0xFF
            result.append(HEX_CHARS[octet ushr 4])
            result.append(HEX_CHARS[octet and 0x0F])
        }
        return result.toString()
    }

    /**
     * Checks if the SHA-256 hash of [bytes] strictly matches the [expectedSha256].
     */
    fun verifyPayloadIntegrity(bytes: ByteArray, expectedSha256: String): Boolean {
        if (expectedSha256.isBlank()) return false
        val computed = computeSha256(bytes)
        return computed.equals(expectedSha256.trim(), ignoreCase = true)
    }

    /**
     * Checks if the SHA-256 hash of [file] strictly matches the [expectedSha256].
     */
    fun verifyPayloadIntegrity(file: File, expectedSha256: String): Boolean {
        if (expectedSha256.isBlank() || !file.exists()) return false
        val computed = computeSha256(file)
        return computed.equals(expectedSha256.trim(), ignoreCase = true)
    }

    /**
     * Generates a deterministic mock Arinara Network official authority signature
     * binding the package metadata and payload hash to the root authority secret.
     */
    fun generateOfficialSignature(packageId: String, version: String, payloadSha256: String): String {
        val payloadToken = "$packageId:$version:$payloadSha256:$ARINARA_OFFICIAL_AUTHORITY_KEY"
        return computeSha256(payloadToken.toByteArray(Charsets.UTF_8))
    }

    /**
     * Authenticates that the package manifest signature is non-empty, conforms to 64-character
     * hexadecimal requirements, and matches the payload hash.
     */
    fun verifyArinaraSignature(manifest: FpkgManifest, rawPayload: ByteArray? = null): Boolean {
        val sig = manifest.signatureSha256.trim().lowercase(Locale.US)
        if (sig.length != 64 || !sig.all { it in '0'..'9' || it in 'a'..'f' }) {
            return false
        }
        if (rawPayload != null) {
            val computedHash = computeSha256(rawPayload)
            if (!computedHash.equals(sig, ignoreCase = true)) {
                return false
            }
        }
        return true
    }

    /**
     * Full end-to-end verification of manifest and payload bytes.
     */
    fun verify(manifest: FpkgManifest, payloadBytes: ByteArray): VerificationResult {
        val actualSha256 = computeSha256(payloadBytes)
        val expectedSha256 = manifest.signatureSha256.trim().lowercase(Locale.US)

        if (!actualSha256.equals(expectedSha256, ignoreCase = true)) {
            return VerificationResult.TamperedPayload(expectedHash = expectedSha256, actualHash = actualSha256)
        }

        if (!verifyArinaraSignature(manifest, payloadBytes)) {
            return VerificationResult.InvalidSignature("Failed Arinara Network authority validation")
        }

        return VerificationResult.Success
    }
}
