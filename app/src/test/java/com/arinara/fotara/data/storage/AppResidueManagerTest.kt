// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.storage

import android.content.Context
import android.os.Environment
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppResidueManagerTest {

    private class MockContext(
        private val cache: File,
        private val extCache: File,
        private val downloads: File,
        private val files: File
    ) : android.content.ContextWrapper(null) {
        override fun getCacheDir(): File = cache
        override fun getExternalCacheDir(): File = extCache
        override fun getExternalFilesDir(type: String?): File = downloads
        override fun getFilesDir(): File = files
        override fun getApplicationContext(): Context = this
    }

    private lateinit var rootTestDir: File
    private lateinit var mockContext: Context
    private lateinit var fakeCacheDir: File
    private lateinit var fakeExternalCacheDir: File
    private lateinit var fakeDownloadsDir: File
    private lateinit var fakeFilesDir: File

    @Before
    fun setUp() {
        val baseTmp = System.getProperty("java.io.tmpdir") ?: "."
        rootTestDir = File(baseTmp, "fotara_residue_test_${System.currentTimeMillis()}").apply { mkdirs() }

        fakeCacheDir = File(rootTestDir, "cache").apply { mkdirs() }
        fakeExternalCacheDir = File(rootTestDir, "external_cache").apply { mkdirs() }
        fakeDownloadsDir = File(rootTestDir, "downloads").apply { mkdirs() }
        fakeFilesDir = File(rootTestDir, "files").apply { mkdirs() }

        mockContext = MockContext(
            cache = fakeCacheDir,
            extCache = fakeExternalCacheDir,
            downloads = fakeDownloadsDir,
            files = fakeFilesDir
        )
    }

    @After
    fun tearDown() {
        try {
            rootTestDir.deleteRecursively()
        } catch (_: Exception) {}
    }

    @Test
    fun calculateAndCleanAppResidue_cleansDisposableFilesWhilePreservingProtectedData() = runTest {
        // 1. Create disposable files
        val leftoverApk = File(fakeDownloadsDir, "Fotara_Update_1.8.2_Beta.apk").apply {
            writeText("dummy-apk-data-12345")
        }
        val partApk = File(fakeDownloadsDir, "Fotara_Update_1.8.2_Beta.apk.part").apply {
            writeText("dummy-part-stream-67890")
        }
        val shareDir = File(fakeCacheDir, "share_staging").apply { mkdirs() }
        val stagedShare = File(shareDir, "staged_photo_1.jpg").apply {
            writeText("staged-share-content")
        }
        val exportDir = File(fakeCacheDir, "exports").apply { mkdirs() }
        val exportedPdf = File(exportDir, "export_doc.pdf").apply {
            writeText("exported-pdf-binary")
        }
        val docxMediaDir = File(fakeCacheDir, "docx_media_1234").apply { mkdirs() }
        val docxImage = File(docxMediaDir, "image1.png").apply {
            writeText("docx-image-data")
        }
        val cropTemp = File(fakeCacheDir, "temp_crop_5678.png").apply {
            writeText("temp-crop-buffer")
        }

        // 2. Create protected coursework files that MUST NEVER BE TOUCHED
        val photosDir = File(fakeFilesDir, "photos").apply { mkdirs() }
        val protectedPhoto = File(photosDir, "note_photo_1.jpg").apply {
            writeText("valuable-coursework-photo")
        }
        val protectedDb = File(fakeCacheDir, "Fotara.db").apply {
            writeText("sqlite-database-marker")
        }

        val residueManager = AppResidueManager(mockContext)

        // 3. Verify detection
        val residueInfo = residueManager.calculateAppResidue()
        assertEquals(6, residueInfo.fileCount)
        assertTrue(residueInfo.totalSizeBytes > 0L)

        // 4. Clean residue with progress tracking
        val recordedProgress = mutableListOf<Float>()
        val reclaimedBytes = residueManager.cleanAppResidue { progress ->
            recordedProgress.add(progress)
        }

        assertTrue(reclaimedBytes > 0L)
        assertTrue(recordedProgress.isNotEmpty())
        assertEquals(1.0f, recordedProgress.last(), 0.01f)

        // 5. Verify disposable files were deleted
        assertFalse(leftoverApk.exists())
        assertFalse(partApk.exists())
        assertFalse(stagedShare.exists())
        assertFalse(exportedPdf.exists())
        assertFalse(docxImage.exists())
        assertFalse(cropTemp.exists())

        // 6. Verify protected files REMAIN INTACT
        assertTrue(protectedPhoto.exists())
        assertTrue(protectedDb.exists())
        assertEquals("valuable-coursework-photo", protectedPhoto.readText())

        // 7. Verify post-clean residue calculation is zero
        val postCleanInfo = residueManager.calculateAppResidue()
        assertEquals(0, postCleanInfo.fileCount)
        assertEquals(0L, postCleanInfo.totalSizeBytes)
    }
}
