// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.Calendar

class FileNamePresetHelperTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testSanitizeFileName_removesIllegalCharacters() {
        val input = "Folder: <Math> / \"Physics\" * test? | wow"
        val sanitized = FileNamePresetHelper.sanitizeFileName(input)
        assertEquals("Folder Math Physics test wow", sanitized)
    }

    @Test
    fun testSanitizeFileName_collapsesRepeatedWhitespace() {
        val input = "Hello     World   Test"
        val sanitized = FileNamePresetHelper.sanitizeFileName(input)
        assertEquals("Hello World Test", sanitized)
    }

    @Test
    fun testSanitizeFileName_stripsTrailingDotsAndSpaces() {
        val input = "MyNotes...   "
        val sanitized = FileNamePresetHelper.sanitizeFileName(input)
        assertEquals("MyNotes", sanitized)
    }

    @Test
    fun testSanitizeFileName_limitsTo80Characters() {
        val longString = "A".repeat(120)
        val sanitized = FileNamePresetHelper.sanitizeFileName(longString)
        assertEquals(80, sanitized.length)
        assertEquals("A".repeat(80), sanitized)
    }

    @Test
    fun testSanitizeFileName_rejectsOnlyDots() {
        val input = "....."
        val sanitized = FileNamePresetHelper.sanitizeFileName(input, fallback = "FallbackNotes")
        assertEquals("FallbackNotes", sanitized)
    }

    @Test
    fun testSanitizeFileName_emptyFallsBack() {
        val input = "   "
        val sanitized = FileNamePresetHelper.sanitizeFileName(input, fallback = "Fotara_Combined")
        assertEquals("Fotara_Combined", sanitized)
    }

    @Test
    fun testBuildFinalFileName_appendsExtensionAndAvoidsDuplication() {
        val preset = "{folder}_{date}"
        val folder = "Calculus"

        // Without extension in customName
        val res1 = FileNamePresetHelper.buildFinalFileName(
            customName = "Homework_1",
            presetTemplate = preset,
            folderName = folder,
            itemCount = 5,
            extension = ".pdf"
        )
        assertEquals("Homework_1.pdf", res1)

        // With extension in customName -> no duplication!
        val res2 = FileNamePresetHelper.buildFinalFileName(
            customName = "Homework_1.pdf",
            presetTemplate = preset,
            folderName = folder,
            itemCount = 5,
            extension = ".pdf"
        )
        assertEquals("Homework_1.pdf", res2)

        // Case insensitivity in extension duplication
        val res3 = FileNamePresetHelper.buildFinalFileName(
            customName = "Homework_1.PDF",
            presetTemplate = preset,
            folderName = folder,
            itemCount = 5,
            extension = ".pdf"
        )
        assertEquals("Homework_1.pdf", res3)
    }

    @Test
    fun testBuildFinalFileName_emptyCustomNameFallsBackToPreset() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 4, 10, 30, 0)
        }
        val millis = cal.timeInMillis

        val result = FileNamePresetHelper.buildFinalFileName(
            customName = "   ",
            presetTemplate = "{folder}_{date}",
            folderName = "Physics",
            itemCount = 3,
            extension = ".docx",
            dateMillis = millis
        )
        assertEquals("Physics_2026-10-04.docx", result)
    }

    @Test
    fun testResolvePreset_replacesAllTokens() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 4, 14, 25, 0)
        }
        val millis = cal.timeInMillis

        val resolved = FileNamePresetHelper.resolvePreset(
            presetTemplate = "{folder}_{date}_{time}_{count}items",
            folderName = "Biology: 101",
            itemCount = 7,
            dateMillis = millis
        )
        assertEquals("Biology 101_2026-10-04_14-25_7items", resolved)
    }

    @Test
    fun testValidatePreset_validatesTokensAndLength() {
        // Valid default
        assertTrue(FileNamePresetHelper.validatePreset("{folder}_{date}").isValid)
        assertTrue(FileNamePresetHelper.validatePreset("{folder}_{date}_{time}_{count}").isValid)

        // Unknown token
        val resUnknown = FileNamePresetHelper.validatePreset("{folder}_{unknown_token}")
        assertFalse(resUnknown.isValid)

        // Empty preset
        val resEmpty = FileNamePresetHelper.validatePreset("   ")
        assertFalse(resEmpty.isValid)

        // Too long preset (> 80 chars)
        val resTooLong = FileNamePresetHelper.validatePreset("A".repeat(85))
        assertFalse(resTooLong.isValid)
    }

    @Test
    fun testEnsureUniqueFile_generatesNumericSuffix() {
        val dir = tempFolder.newFolder("exports")
        val baseFile = File(dir, "Notes.pdf")
        baseFile.createNewFile()

        val unique1 = FileNamePresetHelper.ensureUniqueFile(dir, "Notes.pdf")
        assertEquals("Notes (1).pdf", unique1.name)
        unique1.createNewFile()

        val unique2 = FileNamePresetHelper.ensureUniqueFile(dir, "Notes.pdf")
        assertEquals("Notes (2).pdf", unique2.name)
    }
}
