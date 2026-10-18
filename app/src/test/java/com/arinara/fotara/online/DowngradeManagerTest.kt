// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.online

import android.content.Context
import android.content.ContextWrapper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class DowngradeManagerTest {

    private class TestContext : ContextWrapper(null) {
        override fun getApplicationContext(): Context = this
        override fun getPackageName(): String = "com.arinara.fotara"
    }

    private lateinit var testContext: Context
    private lateinit var downgradeManager: DowngradeManager

    @Before
    fun setUp() {
        testContext = TestContext()
        downgradeManager = DowngradeManager(testContext)
    }

    @Test
    fun getAdbCommand_formatsCorrectly() {
        val cmd = downgradeManager.getAdbCommand("Fotara_2.0.1_Alpha.apk")
        assertEquals("adb install -d -r \"Fotara_2.0.1_Alpha.apk\"", cmd)
    }

    @Test
    fun getAdbCommand_escapesSpacesAndSpecialVersions() {
        val cmd = downgradeManager.getAdbCommand("Fotara 1.9.0 Beta.apk")
        assertEquals("adb install -d -r \"Fotara 1.9.0 Beta.apk\"", cmd)
    }

    @Test
    fun constants_areCorrectlyConfigured() {
        assertEquals("fotara_rollback_v1", DowngradeManager.ROLLBACK_NOTIF_CHANNEL_ID)
        assertEquals(90210, DowngradeManager.ROLLBACK_NOTIF_ID)
        assertEquals(4410, DowngradeManager.SHIZUKU_REQUEST_CODE)
    }

    @Test
    fun isShizukuAvailable_handlesAbsenceGracefully() {
        val available = downgradeManager.isShizukuAvailable()
        assertFalse(available)
    }

    @Test
    fun hasShizukuPermission_handlesAbsenceGracefully() {
        val hasPerm = downgradeManager.hasShizukuPermission()
        assertFalse(hasPerm)
    }

    @Test
    fun executeShizukuDowngrade_failsCleanlyWhenApkMissing() = runBlocking {
        val nonExistentApk = File("non_existent_rollback.apk")
        val result = downgradeManager.executeShizukuDowngrade(nonExistentApk)
        assertTrue(result.isFailure)
    }
}
