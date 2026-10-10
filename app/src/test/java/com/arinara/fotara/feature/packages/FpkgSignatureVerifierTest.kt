// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.packages

import com.arinara.fotara.feature.packages.loader.FpkgSignatureVerifier
import com.arinara.fotara.feature.packages.loader.VerificationResult
import com.arinara.fotara.feature.packages.model.FpkgManifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Unit tests for cryptographic SHA-256 integrity hashing and Arinara signature verification.
 */
class FpkgSignatureVerifierTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testComputeSha256ForByteArray() {
        val emptyBytes = ByteArray(0)
        // Known NIST SHA-256 test vector for empty string
        val emptyExpected = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        assertEquals(emptyExpected, FpkgSignatureVerifier.computeSha256(emptyBytes))

        // Known NIST vector for "hello world"
        val helloWorldBytes = "hello world".toByteArray(Charsets.UTF_8)
        val helloExpected = "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9"
        assertEquals(helloExpected, FpkgSignatureVerifier.computeSha256(helloWorldBytes))
    }

    @Test
    fun testComputeSha256ForFile() {
        val file = tempFolder.newFile("test_package.fpkg")
        file.writeText("Arinara Academic Package Content 2026")

        val expected = FpkgSignatureVerifier.computeSha256("Arinara Academic Package Content 2026".toByteArray(Charsets.UTF_8))
        val computed = FpkgSignatureVerifier.computeSha256(file)

        assertEquals(expected, computed)
    }

    @Test
    fun testVerifyPayloadIntegrity() {
        val payload = "Official payload data stream".toByteArray(Charsets.UTF_8)
        val validSha256 = FpkgSignatureVerifier.computeSha256(payload)

        // Valid integrity
        assertTrue(FpkgSignatureVerifier.verifyPayloadIntegrity(payload, validSha256))
        assertTrue(FpkgSignatureVerifier.verifyPayloadIntegrity(payload, validSha256.uppercase()))

        // Tampered payload
        val tamperedPayload = "Corrupted payload data stream".toByteArray(Charsets.UTF_8)
        assertFalse(FpkgSignatureVerifier.verifyPayloadIntegrity(tamperedPayload, validSha256))

        // Blank or invalid hash
        assertFalse(FpkgSignatureVerifier.verifyPayloadIntegrity(payload, ""))
        assertFalse(FpkgSignatureVerifier.verifyPayloadIntegrity(payload, "invalid_hash"))
    }

    @Test
    fun testVerifyPayloadIntegrityWithFile() {
        val file = tempFolder.newFile("payload_stream.bin")
        val data = "Sample byte sequence".toByteArray(Charsets.UTF_8)
        file.writeBytes(data)

        val hash = FpkgSignatureVerifier.computeSha256(data)
        assertTrue(FpkgSignatureVerifier.verifyPayloadIntegrity(file, hash))

        val nonExistent = File(tempFolder.root, "non_existent.bin")
        assertFalse(FpkgSignatureVerifier.verifyPayloadIntegrity(nonExistent, hash))
    }

    @Test
    fun testGenerateAndVerifyOfficialSignature() {
        val packageId = "com.arinara.fotara.pkg.collab"
        val version = "1.0.0"
        val payloadHash = "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9"

        val signature = FpkgSignatureVerifier.generateOfficialSignature(packageId, version, payloadHash)
        assertNotNull(signature)
        assertEquals(64, signature.length)

        val manifest = FpkgManifest(
            packageId = packageId,
            name = "Collab",
            version = version,
            minFotaraVersion = "2.0.0",
            description = "Desc",
            iconKey = "icon",
            sizeBytes = 1000L,
            permissions = emptyList(),
            signatureSha256 = payloadHash
        )

        // Signature format validity
        assertTrue(FpkgSignatureVerifier.verifyArinaraSignature(manifest))

        // Invalid signature length or characters
        val invalidManifest = manifest.copy(signatureSha256 = "short_invalid")
        assertFalse(FpkgSignatureVerifier.verifyArinaraSignature(invalidManifest))
    }

    @Test
    fun testFullVerifyEndToEnd() {
        val payload = "Real-Time Collab Canvas Payload".toByteArray(Charsets.UTF_8)
        val hash = FpkgSignatureVerifier.computeSha256(payload)

        val manifest = FpkgManifest(
            packageId = "com.arinara.fotara.pkg.collab",
            name = "Real-Time Collab Canvas",
            version = "1.0.0",
            minFotaraVersion = "2.0.0",
            description = "Collab",
            iconKey = "collab",
            sizeBytes = payload.size.toLong(),
            permissions = listOf("NET"),
            signatureSha256 = hash
        )

        // Success verification
        val resultSuccess = FpkgSignatureVerifier.verify(manifest, payload)
        assertTrue(resultSuccess is VerificationResult.Success)

        // Tampered payload verification
        val tampered = "Tampered Canvas Payload".toByteArray(Charsets.UTF_8)
        val resultTampered = FpkgSignatureVerifier.verify(manifest, tampered)
        assertTrue(resultTampered is VerificationResult.TamperedPayload)
    }
}
