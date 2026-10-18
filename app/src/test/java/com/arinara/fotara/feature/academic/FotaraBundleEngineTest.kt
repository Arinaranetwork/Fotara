// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.academic

import com.arinara.fotara.feature.academic.bundle.BundleExportItem
import com.arinara.fotara.feature.academic.bundle.CourseworkManifest
import com.arinara.fotara.feature.academic.bundle.FotaraBundleExporter
import com.arinara.fotara.feature.academic.bundle.FotaraBundleImporter
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class FotaraBundleEngineTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var exporter: FotaraBundleExporter
    private lateinit var importer: FotaraBundleImporter

    @Before
    fun setUp() {
        exporter = FotaraBundleExporter()
        importer = FotaraBundleImporter()
    }

    @Test
    fun roundTrip_exportsAndImportsBundleWithVerifiedChecksums() {
        val noteContent1 = "Linear Algebra: Vector spaces and linear transformations.".toByteArray(Charsets.UTF_8)
        val noteContent2 = "# Quantum Mechanics\nSchrodinger wave equations and operators.".toByteArray(Charsets.UTF_8)
        val syllabusJson = "{\"course\":\"PHYS301\",\"finalWeight\":40}".toByteArray(Charsets.UTF_8)

        val items = listOf(
            BundleExportItem("notes/lecture1.txt", "TEXT_NOTE", "Linear Algebra Lecture 1", noteContent1),
            BundleExportItem("notes/lecture2.md", "TEXT_NOTE", "Quantum Mechanics Intro", noteContent2),
            BundleExportItem("syllabus/phys301.json", "SYLLABUS", "Course Syllabus", syllabusJson)
        )

        val baos = ByteArrayOutputStream()
        val exportManifest = exporter.exportBundle(
            outputStream = baos,
            courseTitle = "Advanced Physics & Mathematics",
            courseCode = "PHYS-301",
            academicTerm = "Fall 2026",
            items = items
        )

        assertEquals("Advanced Physics & Mathematics", exportManifest.courseTitle)
        assertEquals(3, exportManifest.itemCount)
        assertTrue(exportManifest.checksumSha256.isNotBlank())

        // Import
        val importResult = importer.importBundle(ByteArrayInputStream(baos.toByteArray()))

        assertTrue(importResult.isSuccess)
        assertNotNull(importResult.manifest)
        assertEquals("Advanced Physics & Mathematics", importResult.manifest?.courseTitle)
        assertEquals("PHYS-301", importResult.manifest?.courseCode)
        assertEquals(3, importResult.extractedItems.size)

        for (extracted in importResult.extractedItems) {
            assertTrue(extracted.isChecksumValid)
        }

        val item1 = importResult.extractedItems.first { it.relativePath == "notes/lecture1.txt" }
        assertArrayEquals(noteContent1, item1.data)
        assertEquals("TEXT_NOTE", item1.entryType)

        val item2 = importResult.extractedItems.first { it.relativePath == "notes/lecture2.md" }
        assertArrayEquals(noteContent2, item2.data)
    }

    @Test
    fun extractBundleToDirectory_writesPayloadsSafelyToDisk() {
        val payload = "Cell biology mitochondria ATP production".toByteArray(Charsets.UTF_8)
        val items = listOf(
            BundleExportItem("bio/chapter1.txt", "TEXT_NOTE", "Cell Biology", payload)
        )

        val bundleFile = tempFolder.newFile("course.fotara")
        exporter.exportBundleToFile(
            destinationFile = bundleFile,
            courseTitle = "Molecular Biology",
            items = items
        )

        val outputDir = tempFolder.newFolder("extracted_bundle")
        val result = importer.extractBundleToDirectory(bundleFile.inputStream(), outputDir)

        assertTrue(result.isSuccess)
        val extractedFile = File(outputDir, "bio/chapter1.txt")
        assertTrue(extractedFile.exists())
        assertEquals("Cell biology mitochondria ATP production", extractedFile.readText(Charsets.UTF_8))
    }

    @Test
    fun zipSlipAttack_isDetectedAndBlocked() {
        // Construct malicious ZIP stream attempting path traversal outside destination
        val baos = ByteArrayOutputStream()
        val zipOut = ZipOutputStream(baos)

        zipOut.putNextEntry(ZipEntry("../malicious.txt"))
        zipOut.write("Malicious script content".toByteArray())
        zipOut.closeEntry()

        zipOut.putNextEntry(ZipEntry("manifest.json"))
        zipOut.write("{}".toByteArray())
        zipOut.closeEntry()
        zipOut.close()

        val result = importer.importBundle(ByteArrayInputStream(baos.toByteArray()))

        assertFalse(result.isSuccess)
        assertTrue(result.validationError?.contains("traversal", ignoreCase = true) == true)
    }

    @Test
    fun corruptedPayload_failsChecksumValidation() {
        val noteContent = "Calculus limits definition".toByteArray(Charsets.UTF_8)
        val items = listOf(
            BundleExportItem("calc.txt", "TEXT_NOTE", "Calculus", noteContent)
        )

        val baos = ByteArrayOutputStream()
        exporter.exportBundle(
            outputStream = baos,
            courseTitle = "Calculus I",
            items = items
        )

        val corruptedBaos = ByteArrayOutputStream()
        val zipIn = java.util.zip.ZipInputStream(ByteArrayInputStream(baos.toByteArray()))
        val zipOut = java.util.zip.ZipOutputStream(corruptedBaos)
        var entry = zipIn.nextEntry
        while (entry != null) {
            val bytes = zipIn.readBytes()
            zipOut.putNextEntry(java.util.zip.ZipEntry(entry.name))
            if (entry.name == "calc.txt") {
                val corruptedPayload = bytes.copyOf()
                corruptedPayload[0] = (corruptedPayload[0] + 1).toByte()
                zipOut.write(corruptedPayload)
            } else {
                zipOut.write(bytes)
            }
            zipOut.closeEntry()
            entry = zipIn.nextEntry
        }
        zipOut.finish()

        val result = importer.importBundle(ByteArrayInputStream(corruptedBaos.toByteArray()))
        assertFalse(result.isSuccess)
    }

    @Test
    fun parseManifest_extractsManifestStructureCorrectly() {
        val sampleJson = """
            {
              "formatVersion": 1,
              "appIdentifier": "com.arinara.fotara",
              "courseTitle": "Organic Chemistry",
              "courseCode": "CHEM201",
              "academicTerm": "Spring 2026",
              "creationTimestamp": 1773000000000,
              "itemCount": 1,
              "checksumSha256": "abcdef123456",
              "entries": [
                {
                  "relativePath": "notes/alkanes.txt",
                  "entryType": "TEXT_NOTE",
                  "title": "Alkanes and Alkenes",
                  "sha256": "9876543210",
                  "byteSize": 1024
                }
              ]
            }
        """.trimIndent()

        val manifest = importer.parseManifest(sampleJson)
        assertNotNull(manifest)
        assertEquals("Organic Chemistry", manifest?.courseTitle)
        assertEquals("CHEM201", manifest?.courseCode)
        assertEquals("Spring 2026", manifest?.academicTerm)
        assertEquals(1, manifest?.entries?.size)
        assertEquals("notes/alkanes.txt", manifest?.entries?.first()?.relativePath)
        assertEquals(1024L, manifest?.entries?.first()?.byteSize)
    }
}
