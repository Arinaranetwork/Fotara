// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.arinara.fotara.util.ProfileImageUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ProfileImageUtilsInstrumentedTest {

    @Test
    fun saveWebpAtomically_realBitmap_encodesSavesAndReloads() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val original = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        original.setPixel(10, 10, android.graphics.Color.RED)
        original.setPixel(50, 50, android.graphics.Color.BLUE)

        val targetFile = File(context.cacheDir, "test_avatar_${System.currentTimeMillis()}.webp")
        try {
            val success = ProfileImageUtils.saveWebpAtomically(
                bitmap = original,
                targetFile = targetFile,
                targetWidth = 512,
                targetHeight = 512,
                quality = 90
            )

            assertTrue("saveWebpAtomically must report success", success)
            assertTrue("Target file must exist", targetFile.exists())
            assertTrue("Target file must not be 0 bytes", targetFile.length() > 0L)

            // Reload round trip
            val reloaded = BitmapFactory.decodeFile(targetFile.absolutePath)
            assertNotNull("Reloaded bitmap must be decodable", reloaded)
            assertEquals("Width must match targetWidth", 512, reloaded.width)
            assertEquals("Height must match targetHeight", 512, reloaded.height)
        } finally {
            if (targetFile.exists()) targetFile.delete()
        }
    }
}
