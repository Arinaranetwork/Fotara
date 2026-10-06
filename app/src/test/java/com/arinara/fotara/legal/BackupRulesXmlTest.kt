// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.legal

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BackupRulesXmlTest {

    private fun findXmlFile(relativePath: String): File {
        var current: File? = File(".").canonicalFile
        while (current != null) {
            val candidate = File(current, relativePath)
            if (candidate.exists()) {
                return candidate
            }
            current = current.parentFile
        }
        throw AssertionError("Could not locate XML file '$relativePath'")
    }

    @Test
    fun testBackupRulesExcludesLegalAndDevicePrefs() {
        val file = findXmlFile("app/src/main/res/xml/backup_rules.xml")
        assertTrue("backup_rules.xml must exist", file.exists())
        val content = file.readText()

        assertTrue(
            "backup_rules.xml must exclude fotara_legal_prefs.xml",
            content.contains("""<exclude domain="sharedpref" path="fotara_legal_prefs.xml"""")
        )
        assertTrue(
            "backup_rules.xml must exclude fotara_device_prefs.xml",
            content.contains("""<exclude domain="sharedpref" path="fotara_device_prefs.xml"""")
        )
    }

    @Test
    fun testDataExtractionRulesExcludesCloudAndDeviceTransfer() {
        val file = findXmlFile("app/src/main/res/xml/data_extraction_rules.xml")
        assertTrue("data_extraction_rules.xml must exist", file.exists())
        val content = file.readText()

        assertTrue("Must contain cloud-backup tag", content.contains("<cloud-backup>"))
        assertTrue("Must contain device-transfer tag", content.contains("<device-transfer>"))

        val cloudSection = content.substringAfter("<cloud-backup>").substringBefore("</cloud-backup>")
        assertTrue(
            "cloud-backup must exclude fotara_legal_prefs.xml",
            cloudSection.contains("""<exclude domain="sharedpref" path="fotara_legal_prefs.xml"""")
        )
        assertTrue(
            "cloud-backup must exclude fotara_device_prefs.xml",
            cloudSection.contains("""<exclude domain="sharedpref" path="fotara_device_prefs.xml"""")
        )

        val transferSection = content.substringAfter("<device-transfer>").substringBefore("</device-transfer>")
        assertTrue(
            "device-transfer must exclude fotara_legal_prefs.xml",
            transferSection.contains("""<exclude domain="sharedpref" path="fotara_legal_prefs.xml"""")
        )
        assertTrue(
            "device-transfer must exclude fotara_device_prefs.xml",
            transferSection.contains("""<exclude domain="sharedpref" path="fotara_device_prefs.xml"""")
        )
    }

    @Test
    fun testAndroidManifestDeclaresBackupRules() {
        val manifest = findXmlFile("app/src/main/AndroidManifest.xml")
        assertTrue("AndroidManifest.xml must exist", manifest.exists())
        val content = manifest.readText()

        assertTrue(
            "Manifest must configure android:fullBackupContent=@xml/backup_rules",
            content.contains("""android:fullBackupContent="@xml/backup_rules"""")
        )
        assertTrue(
            "Manifest must configure android:dataExtractionRules=@xml/data_extraction_rules",
            content.contains("""android:dataExtractionRules="@xml/data_extraction_rules"""")
        )
    }
}
