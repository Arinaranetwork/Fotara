// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.packages

import com.arinara.fotara.feature.packages.model.FpkgManifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying manifest data structure, formatting, and JSON serialization.
 */
class FpkgManifestTest {

    @Test
    fun testFpkgManifestPropertiesAndDefaults() {
        val manifest = FpkgManifest(
            packageId = "com.arinara.fotara.pkg.collab",
            name = "Real-Time Collab Canvas",
            version = "1.0.0",
            minFotaraVersion = "2.0.0",
            description = "Real-Time peer collaboration",
            iconKey = "collab",
            sizeBytes = 3_355_443L,
            permissions = listOf("INTERNET", "SOCKET"),
            signatureSha256 = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890"
        )

        assertEquals("com.arinara.fotara.pkg.collab", manifest.packageId)
        assertEquals("Real-Time Collab Canvas", manifest.name)
        assertEquals("1.0.0", manifest.version)
        assertEquals("2.0.0", manifest.minFotaraVersion)
        assertEquals(3_355_443L, manifest.sizeBytes)
        assertEquals(listOf("INTERNET", "SOCKET"), manifest.permissions)
        assertTrue(manifest.isEnabled)
        assertFalse(manifest.isInstalled)
    }

    @Test
    fun testFormattedSizeMegabytes() {
        val collab = FpkgManifest(
            packageId = "pkg.collab",
            name = "Collab",
            version = "1.0.0",
            minFotaraVersion = "2.0.0",
            description = "Collab",
            iconKey = "icon",
            sizeBytes = 3_355_443L, // ~3.2 MB
            permissions = emptyList(),
            signatureSha256 = "sig"
        )
        assertEquals("3.2 MB", collab.formattedSize())

        val ocr = collab.copy(sizeBytes = 5_033_164L) // ~4.8 MB
        assertEquals("4.8 MB", ocr.formattedSize())

        val analytics = collab.copy(sizeBytes = 2_202_009L) // ~2.1 MB
        assertEquals("2.1 MB", analytics.formattedSize())
    }

    @Test
    fun testFormattedSizeKilobytesAndBytes() {
        val kbManifest = FpkgManifest(
            packageId = "pkg.small",
            name = "Small",
            version = "1.0.0",
            minFotaraVersion = "2.0.0",
            description = "Small",
            iconKey = "icon",
            sizeBytes = 512 * 1024L,
            permissions = emptyList(),
            signatureSha256 = "sig"
        )
        assertEquals("512.0 KB", kbManifest.formattedSize())

        val byteManifest = kbManifest.copy(sizeBytes = 420L)
        assertEquals("420 B", byteManifest.formattedSize())
    }

    @Test
    fun testJsonSerializationAndDeserialization() {
        val original = FpkgManifest(
            packageId = "com.arinara.fotara.pkg.whiteboard_ocr",
            name = "Advanced Mathematical OCR",
            version = "2.1.0",
            minFotaraVersion = "2.0.0",
            description = "Extracts \"LaTeX\" formulas & matrices\nwith high precision.",
            iconKey = "whiteboard_ocr",
            sizeBytes = 5_033_164L,
            permissions = listOf("NEURAL_NPU", "CAMERA_RAW"),
            signatureSha256 = "11223344556677889900aabbccddeeff11223344556677889900aabbccddeeff",
            isEnabled = false,
            isInstalled = true
        )

        val json = original.toJson()
        assertNotNull(json)
        assertTrue(json.contains("com.arinara.fotara.pkg.whiteboard_ocr"))

        val parsed = FpkgManifest.fromJson(json)
        assertNotNull(parsed)
        assertEquals(original.packageId, parsed?.packageId)
        assertEquals(original.name, parsed?.name)
        assertEquals(original.version, parsed?.version)
        assertEquals(original.minFotaraVersion, parsed?.minFotaraVersion)
        assertEquals(original.description, parsed?.description)
        assertEquals(original.iconKey, parsed?.iconKey)
        assertEquals(original.sizeBytes, parsed?.sizeBytes)
        assertEquals(original.permissions, parsed?.permissions)
        assertEquals(original.signatureSha256, parsed?.signatureSha256)
        assertEquals(false, parsed?.isEnabled)
        assertEquals(true, parsed?.isInstalled)
    }

    @Test
    fun testFromJsonMalformedReturnsNull() {
        assertNull(FpkgManifest.fromJson(""))
        assertNull(FpkgManifest.fromJson("{ invalid json content }"))
        assertNull(FpkgManifest.fromJson("{\"missingPackageId\": true}"))
    }

    @Test
    fun testManifestCopy() {
        val original = FpkgManifest(
            packageId = "pkg.1",
            name = "Test",
            version = "1.0.0",
            minFotaraVersion = "2.0.0",
            description = "Desc",
            iconKey = "key",
            sizeBytes = 100L,
            permissions = listOf("P1"),
            signatureSha256 = "sig"
        )
        val modified = original.copy(isInstalled = true, isEnabled = false)
        assertTrue(modified.isInstalled)
        assertFalse(modified.isEnabled)
        assertEquals(original.packageId, modified.packageId)
    }
}
