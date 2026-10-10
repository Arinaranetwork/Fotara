// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.packages

import com.arinara.fotara.feature.packages.loader.FotaraPackageManager
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Unit tests verifying package lifecycle management, signature enforcement,
 * persistence, and storage quota accounting.
 */
class FotaraPackageManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var packageManager: FotaraPackageManager
    private lateinit var packagesDir: File

    @Before
    fun setUp() {
        packagesDir = tempFolder.newFolder("packages")
        packageManager = FotaraPackageManager(packagesDir)
    }

    @Test
    fun testInitialCatalogHasOfficialPackages() {
        val available = packageManager.getAvailablePackages()
        assertEquals(3, available.size)

        val packageIds = available.map { it.packageId }
        assertTrue(packageIds.contains(FotaraPackageManager.PACKAGE_COLLAB))
        assertTrue(packageIds.contains(FotaraPackageManager.PACKAGE_WHITEBOARD_OCR))
        assertTrue(packageIds.contains(FotaraPackageManager.PACKAGE_EXAM_ANALYTICS))

        val installed = packageManager.getInstalledPackages()
        assertTrue(installed.isEmpty())
        assertFalse(packageManager.isPackageInstalled(FotaraPackageManager.PACKAGE_COLLAB))
        assertEquals(0L, packageManager.getInstalledStorageBytes())
        assertEquals(0L, packageManager.getReclaimedStorageBytes())
    }

    @Test
    fun testInstallOfficialPackageSuccess() = runTest {
        val result = packageManager.installPackage(FotaraPackageManager.PACKAGE_COLLAB)
        assertTrue(result.isSuccess)

        val manifest = result.getOrNull()
        assertNotNull(manifest)
        assertEquals(FotaraPackageManager.PACKAGE_COLLAB, manifest?.packageId)
        assertTrue(manifest?.isInstalled == true)
        assertTrue(manifest?.isEnabled == true)

        assertTrue(packageManager.isPackageInstalled(FotaraPackageManager.PACKAGE_COLLAB))
        assertEquals(1, packageManager.getInstalledPackages().size)
        assertEquals(2, packageManager.getAvailablePackages().size)
        assertEquals(3_355_443L, packageManager.getInstalledStorageBytes())

        // Verify physical files on disk
        val packageDir = File(packagesDir, FotaraPackageManager.PACKAGE_COLLAB)
        assertTrue(packageDir.exists())
        assertTrue(File(packageDir, FotaraPackageManager.MANIFEST_FILENAME).exists())
        assertTrue(File(packageDir, FotaraPackageManager.PAYLOAD_FILENAME).exists())
    }

    @Test
    fun testInstallWithTamperedPayloadFails() = runTest {
        val tamperedPayload = "CORRUPTED_MALICIOUS_BYTES".toByteArray(Charsets.UTF_8)
        val result = packageManager.installPackage(
            packageId = FotaraPackageManager.PACKAGE_COLLAB,
            payload = tamperedPayload
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)

        assertFalse(packageManager.isPackageInstalled(FotaraPackageManager.PACKAGE_COLLAB))
        assertEquals(0, packageManager.getInstalledPackages().size)

        // Ensure no corrupt artifacts remained
        val packageDir = File(packagesDir, FotaraPackageManager.PACKAGE_COLLAB)
        assertFalse(packageDir.exists())
    }

    @Test
    fun testTogglePackageEnablement() {
        val installResult = packageManager.installPackageSync(FotaraPackageManager.PACKAGE_WHITEBOARD_OCR)
        assertTrue(installResult.isSuccess)

        var installed = packageManager.getInstalledPackages().first()
        assertTrue(installed.isEnabled)

        val toggleFalseSuccess = packageManager.togglePackage(FotaraPackageManager.PACKAGE_WHITEBOARD_OCR, false)
        assertTrue(toggleFalseSuccess)

        installed = packageManager.getInstalledPackages().first()
        assertFalse(installed.isEnabled)

        val toggleTrueSuccess = packageManager.togglePackage(FotaraPackageManager.PACKAGE_WHITEBOARD_OCR, true)
        assertTrue(toggleTrueSuccess)

        installed = packageManager.getInstalledPackages().first()
        assertTrue(installed.isEnabled)
    }

    @Test
    fun testUninstallPackageAndReclaimStorage() = runTest {
        packageManager.installPackage(FotaraPackageManager.PACKAGE_EXAM_ANALYTICS)
        assertTrue(packageManager.isPackageInstalled(FotaraPackageManager.PACKAGE_EXAM_ANALYTICS))

        val uninstallResult = packageManager.uninstallPackage(FotaraPackageManager.PACKAGE_EXAM_ANALYTICS)
        assertTrue(uninstallResult.isSuccess)

        val reclaimedBytes = uninstallResult.getOrNull()
        assertEquals(2_202_009L, reclaimedBytes)
        assertEquals(2_202_009L, packageManager.getReclaimedStorageBytes())

        assertFalse(packageManager.isPackageInstalled(FotaraPackageManager.PACKAGE_EXAM_ANALYTICS))
        assertEquals(0, packageManager.getInstalledPackages().size)
        assertEquals(3, packageManager.getAvailablePackages().size)

        // Verify folder removed
        val packageDir = File(packagesDir, FotaraPackageManager.PACKAGE_EXAM_ANALYTICS)
        assertFalse(packageDir.exists())
    }

    @Test
    fun testPersistenceAcrossManagerInstances() {
        packageManager.installPackageSync(FotaraPackageManager.PACKAGE_COLLAB)
        packageManager.installPackageSync(FotaraPackageManager.PACKAGE_WHITEBOARD_OCR)

        // Create new instance pointing to same storage
        val newManager = FotaraPackageManager(packagesDir)
        val installed = newManager.getInstalledPackages()

        assertEquals(2, installed.size)
        assertTrue(newManager.isPackageInstalled(FotaraPackageManager.PACKAGE_COLLAB))
        assertTrue(newManager.isPackageInstalled(FotaraPackageManager.PACKAGE_WHITEBOARD_OCR))
        assertEquals(1, newManager.getAvailablePackages().size)
    }
}
