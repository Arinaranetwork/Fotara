// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.util

import android.graphics.Bitmap
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ProfileImageUtilsTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun allocateStubBitmap(): Bitmap {
        val field = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe")
        field.isAccessible = true
        val unsafe = field.get(null) as sun.misc.Unsafe
        return unsafe.allocateInstance(Bitmap::class.java) as Bitmap
    }

    @Test
    fun saveWebpAtomically_whenCompressFails_returnsFalseAndCleansUp() {
        // In JVM unit tests with isReturnDefaultValues = true, bitmap.compress returns false
        // This directly verifies the Task 2 bugfix: a failed compress must return false and not leave zero-byte files.
        val dummyBitmap = allocateStubBitmap()
        val targetFile = File(tempFolder.root, "avatar.webp")

        val result = ProfileImageUtils.saveWebpAtomically(
            bitmap = dummyBitmap,
            targetFile = targetFile
        )

        assertFalse("When compress returns false, saveWebpAtomically must report failure", result)
        assertFalse("Target file must not be created when compression fails", targetFile.exists())

        // Verify no leftover .tmp or .bak files
        val files = tempFolder.root.listFiles() ?: emptyArray()
        assertTrue("No temporary or backup files should remain", files.isEmpty())
    }

    @Test
    fun saveWebpAtomically_nullParentDir_returnsFalse() {
        val dummyBitmap = allocateStubBitmap()
        val fileNoParent = File("avatar.webp")
        val result = ProfileImageUtils.saveWebpAtomically(dummyBitmap, fileNoParent)
        assertFalse("File without valid parent directory must return false", result)
    }

    @Test
    fun savePngAtomically_whenCompressFails_returnsFalseAndCleansUp() {
        val dummyBitmap = allocateStubBitmap()
        val targetFile = File(tempFolder.root, "avatar.png")

        val result = ProfileImageUtils.savePngAtomically(
            bitmap = dummyBitmap,
            targetFile = targetFile
        )

        assertFalse("When compress returns false, savePngAtomically must report failure", result)
        assertFalse("Target file must not be created when compression fails", targetFile.exists())

        // Verify no leftover .tmp or .bak files
        val files = tempFolder.root.listFiles() ?: emptyArray()
        assertTrue("No temporary or backup files should remain", files.isEmpty())
    }

    @Test
    fun savePngAtomically_nullParentDir_returnsFalse() {
        val dummyBitmap = allocateStubBitmap()
        val fileNoParent = File("avatar.png")
        val result = ProfileImageUtils.savePngAtomically(dummyBitmap, fileNoParent)
        assertFalse("File without valid parent directory must return false", result)
    }
}
