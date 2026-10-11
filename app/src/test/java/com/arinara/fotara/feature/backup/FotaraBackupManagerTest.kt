// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FotaraBackupManagerTest {

    @Test
    fun computeSha256_computesCorrectHash() {
        val input = "Hello Fotara Backup".toByteArray(Charsets.UTF_8)
        val hash = FotaraBackupManager.computeSha256(input)
        assertNotNull(hash)
        assertEquals(64, hash.length)
        assertEquals(hash, FotaraBackupManager.computeSha256(input))
    }

    @Test
    fun backupScope_entireApp_isSingleton() {
        val scope = BackupScope.EntireApp
        assertTrue(scope is BackupScope.EntireApp)
    }

    @Test
    fun backupScope_selective_holdsUuids() {
        val scope = BackupScope.Selective(
            spaceUuids = setOf("space-1", "space-2"),
            workspaceUuids = setOf("ws-1")
        )
        assertEquals(2, scope.spaceUuids.size)
        assertEquals(1, scope.workspaceUuids.size)
        assertTrue(scope.spaceUuids.contains("space-1"))
        assertTrue(scope.workspaceUuids.contains("ws-1"))
    }

    @Test
    fun manifestSerialization_roundTripIntegrity() {
        val originalDigest = FotaraBackupManager.computeSha256("{}".toByteArray())
        val manifest = BackupManifest(
            format = "fotara_archive_v1",
            version = 1,
            createdAt = 1791547200000L,
            appVersion = "1.9.3 Alpha",
            checksumSha256 = originalDigest,
            scopeType = "ENTIRE_APP",
            spaceCount = 2,
            workspaceCount = 5,
            folderCount = 10,
            photoCount = 20,
            textNoteCount = 15,
            documentCount = 3,
            scheduleCount = 8,
            attachmentCount = 25
        )

        assertEquals("fotara_archive_v1", manifest.format)
        assertEquals(1, manifest.version)
        assertEquals("1.9.3 Alpha", manifest.appVersion)
        assertEquals(originalDigest, manifest.checksumSha256)
        assertEquals(10, manifest.folderCount)
        assertEquals(35, manifest.photoCount + manifest.textNoteCount)
        assertEquals(25, manifest.attachmentCount)
    }
}
