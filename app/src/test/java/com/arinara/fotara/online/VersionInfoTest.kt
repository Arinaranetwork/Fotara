// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.online

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class VersionInfoTest {

    @Test
    fun parse_parsesBetaVersionsCorrectly() {
        val v1 = VersionInfo.parse("1.5.7 Beta")
        assertEquals("1.5.7", v1.numericVersion)
        assertEquals(UpdateChannel.BETA, v1.channel)
        assertEquals("v1.5.7", v1.displayVersion)
        assertEquals("Beta", v1.channelLabel)

        val v2 = VersionInfo.parse("Fotara_1.5.7_Beta")
        assertEquals("1.5.7", v2.numericVersion)
        assertEquals(UpdateChannel.BETA, v2.channel)

        val v3 = VersionInfo.parse("1.7.0-beta")
        assertEquals("1.7.0", v3.numericVersion)
        assertEquals(UpdateChannel.BETA, v3.channel)

        val v4 = VersionInfo.parse("v1.6.1_beta")
        assertEquals("1.6.1", v4.numericVersion)
        assertEquals(UpdateChannel.BETA, v4.channel)
    }

    @Test
    fun parse_parsesStableVersionsCorrectly() {
        val v1 = VersionInfo.parse("1.7.0")
        assertEquals("1.7.0", v1.numericVersion)
        assertEquals(UpdateChannel.STABLE, v1.channel)
        assertEquals("v1.7.0", v1.displayVersion)
        assertEquals("Stable", v1.channelLabel)

        val v2 = VersionInfo.parse("v1.6.0")
        assertEquals("1.6.0", v2.numericVersion)
        assertEquals(UpdateChannel.STABLE, v2.channel)

        val v3 = VersionInfo.parse("Fotara_1.6.0")
        assertEquals("1.6.0", v3.numericVersion)
        assertEquals(UpdateChannel.STABLE, v3.channel)
    }

    @Test
    fun displayVersion_neverContainsChannelTwice() {
        val testInputs = listOf(
            "1.5.7 Beta",
            "1.5.7 Beta Beta",
            "v1.6.0-beta",
            "Fotara_1.7.0_Beta",
            "1.7.0"
        )
        for (input in testInputs) {
            val parsed = VersionInfo.parse(input)
            // displayVersion is purely numeric with leading 'v'
            assertFalse(
                "displayVersion '${parsed.displayVersion}' should not contain 'Beta'",
                parsed.displayVersion.contains("Beta", ignoreCase = true)
            )
            assertFalse(
                "displayVersion '${parsed.displayVersion}' should not contain 'Stable'",
                parsed.displayVersion.contains("Stable", ignoreCase = true)
            )
        }
    }

    @Test
    fun updateComparison_correctlyPrioritizesStableAndNewer() {
        // 1.7.0 is newer than 1.6.0
        assertTrue(UpdateVersionUtils.isNewerVersion("1.7.0", "1.6.0"))
        assertFalse(UpdateVersionUtils.isNewerVersion("1.6.0", "1.7.0"))

        // Equal numeric: Stable is newer than Beta
        assertTrue(UpdateVersionUtils.isNewerVersion("1.7.0", "1.7.0 Beta"))
        assertFalse(UpdateVersionUtils.isNewerVersion("1.7.0 Beta", "1.7.0"))

        // Equal version and channel
        assertFalse(UpdateVersionUtils.isNewerVersion("1.7.0", "1.7.0"))
        assertFalse(UpdateVersionUtils.isNewerVersion("1.7.0 Beta", "1.7.0 Beta"))
    }

    @Test
    fun stringResources_scanNoPlaceholderAppendsBeta() {
        val resDir = File("src/main/res")
        if (!resDir.exists()) {
            // Check relative to project root
            val rootRes = File("app/src/main/res")
            if (rootRes.exists()) {
                scanResDirectory(rootRes)
            }
            return
        }
        scanResDirectory(resDir)
    }

    private fun scanResDirectory(dir: File) {
        val stringFiles = dir.walkTopDown().filter { it.name == "strings.xml" }
        val badPattern = Regex("""%[0-9]*\$?s\s+Beta""", RegexOption.IGNORE_CASE)
        for (file in stringFiles) {
            val text = file.readText()
            val match = badPattern.find(text)
            assertTrue(
                "Found string resource appending 'Beta' to version placeholder in ${file.path}: ${match?.value}",
                match == null
            )
        }
    }
}
