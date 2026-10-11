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

class UpdateManagerLogicTest {

    @Test
    fun cleanVersionString_stripsPrefixesAndSuffixes() {
        assertEquals("2.0.0", UpdateVersionUtils.cleanVersionString("Fotara_2.0.0_Beta"))
        assertEquals("1.5.4", UpdateVersionUtils.cleanVersionString("v1.5.4"))
        assertEquals("2.1.0", UpdateVersionUtils.cleanVersionString("Fotara 2.1.0-RC1"))
        assertEquals("1.0.0", UpdateVersionUtils.cleanVersionString("1.0.0"))
    }

    @Test
    fun isNewerVersion_comparesSemanticVersionsNumerically() {
        // Higher major version
        assertTrue(UpdateVersionUtils.isNewerVersion("2.0.0", "1.5.4"))
        assertFalse(UpdateVersionUtils.isNewerVersion("1.5.4", "2.0.0"))

        // Higher moderate/minor version
        assertTrue(UpdateVersionUtils.isNewerVersion("1.6.0", "1.5.4"))
        assertTrue(UpdateVersionUtils.isNewerVersion("1.10.0", "1.9.0"))
        assertFalse(UpdateVersionUtils.isNewerVersion("1.9.0", "1.10.0"))

        // Higher patch version
        assertTrue(UpdateVersionUtils.isNewerVersion("1.5.5", "1.5.4"))
        assertFalse(UpdateVersionUtils.isNewerVersion("1.5.4", "1.5.4"))

        // Prefixes and suffixes
        assertTrue(UpdateVersionUtils.isNewerVersion("Fotara_2.0.0_Beta", "Fotara_1.5.4_Beta"))
        assertTrue(UpdateVersionUtils.isNewerVersion("v2.1.0", "v2.0.0"))

        // Version 1.5.8 vs 1.5.7 comparison
        assertTrue(UpdateVersionUtils.isNewerVersion("1.5.8 Beta", "1.5.7 Beta"))
        assertFalse(UpdateVersionUtils.isNewerVersion("1.5.7 Beta", "1.5.8 Beta"))
        assertFalse(UpdateVersionUtils.isNewerVersion("1.5.8 Beta", "1.5.8 Beta"))
    }

    @Test
    fun shouldShowUpdatePopup_evaluatesCorrectly() {
        val rel200 = ReleaseInfo(
            version = "2.0.0",
            title = "Fotara 2.0.0",
            releaseNotes = "New features",
            downloadUrl = "https://example.com/app.apk"
        )

        val currentVersion = "1.5.4"

        // 1. When release is null -> false
        assertFalse(
            UpdateVersionUtils.shouldShowUpdatePopup(
                release = null,
                currentVersion = currentVersion,
                skippedVersion = null,
                isPopupDismissedForSession = false
            )
        )

        // 2. Initially newer than current installed version -> true
        assertTrue(
            UpdateVersionUtils.shouldShowUpdatePopup(
                release = rel200,
                currentVersion = currentVersion,
                skippedVersion = null,
                isPopupDismissedForSession = false
            )
        )

        // 3. Dismissing for current session hides popup -> false
        assertFalse(
            UpdateVersionUtils.shouldShowUpdatePopup(
                release = rel200,
                currentVersion = currentVersion,
                skippedVersion = null,
                isPopupDismissedForSession = true
            )
        )

        // 4. Reset session dismiss, but mark 2.0.0 as skipped -> false
        assertFalse(
            UpdateVersionUtils.shouldShowUpdatePopup(
                release = rel200,
                currentVersion = currentVersion,
                skippedVersion = "2.0.0",
                isPopupDismissedForSession = false
            )
        )

        // 5. Newer version 2.1.0 is released -> should show popup even though 2.0.0 was skipped!
        val rel210 = ReleaseInfo(
            version = "2.1.0",
            title = "Fotara 2.1.0",
            releaseNotes = "Major updates",
            downloadUrl = "https://example.com/app.apk"
        )
        assertTrue(
            UpdateVersionUtils.shouldShowUpdatePopup(
                release = rel210,
                currentVersion = currentVersion,
                skippedVersion = "2.0.0",
                isPopupDismissedForSession = false
            )
        )

        // 6. Older version 1.5.0 should not show -> false
        val relOld = ReleaseInfo(
            version = "1.5.0",
            title = "Fotara 1.5.0",
            releaseNotes = "Old release",
            downloadUrl = "https://example.com/app.apk"
        )
        assertFalse(
            UpdateVersionUtils.shouldShowUpdatePopup(
                release = relOld,
                currentVersion = currentVersion,
                skippedVersion = null,
                isPopupDismissedForSession = false
            )
        )
    }

    @Test
    fun isNewerVersion_preemptsOlderDownloadingVersionWhenNewerArrives() {
        // Active download is 1.8.2 Beta, but 1.8.4 Beta arrives on GitHub
        val downloadingVersion = "Fotara_1.8.2_Beta"
        val arrivedVersion = "Fotara_1.8.4_Beta"
        assertTrue(UpdateVersionUtils.isNewerVersion(arrivedVersion, downloadingVersion, remoteIsPrerelease = true, currentIsPrerelease = true))

        // Same version arriving should not preempt
        assertFalse(UpdateVersionUtils.isNewerVersion(downloadingVersion, downloadingVersion, remoteIsPrerelease = true, currentIsPrerelease = true))

        // Skipping 1.8.3 should not prevent 1.8.4 from showing
        val rel184 = ReleaseInfo(
            version = "1.8.4 Beta",
            title = "Fotara 1.8.4",
            releaseNotes = "Update lifecycle fix",
            downloadUrl = "https://example.com/app.apk",
            isPrerelease = true
        )
        assertTrue(
            UpdateVersionUtils.shouldShowUpdatePopup(
                release = rel184,
                currentVersion = "1.8.3 Beta",
                skippedVersion = "1.8.3 Beta",
                isPopupDismissedForSession = false
            )
        )
    }

    @Test
    fun isOlderVersion_accuratelyIdentifiesRollbackCandidates() {
        val currentAppVersion = "2.0.1 Alpha"

        // Past releases should be recognized as older
        assertTrue(UpdateVersionUtils.isOlderVersion("Fotara_2.0.0_Alpha", currentAppVersion))
        assertTrue(UpdateVersionUtils.isOlderVersion("1.9.1 Alpha", currentAppVersion))
        assertTrue(UpdateVersionUtils.isOlderVersion("1.9.0 Beta", currentAppVersion))
        assertTrue(UpdateVersionUtils.isOlderVersion("1.8.4 Beta", currentAppVersion))
        assertTrue(UpdateVersionUtils.isOlderVersion("1.0.0", currentAppVersion))

        // Same version is not older
        assertFalse(UpdateVersionUtils.isOlderVersion("2.0.1 Alpha", currentAppVersion))
        assertFalse(UpdateVersionUtils.isOlderVersion("Fotara_2.0.1_Alpha", currentAppVersion))

        // Newer version is not older
        assertFalse(UpdateVersionUtils.isOlderVersion("2.1.0 Alpha", currentAppVersion))
        assertFalse(UpdateVersionUtils.isOlderVersion("2.0.2 Alpha", currentAppVersion))
        assertFalse(UpdateVersionUtils.isOlderVersion("3.0.0", currentAppVersion))
    }

    @Test
    fun compareVersions_handlesAlphaBetaStableHierarchyAndNumericOrdering() {
        // Numeric difference wins over channel
        assertTrue(UpdateVersionUtils.compareVersions("2.0.1 Alpha", "2.0.0") > 0)
        assertTrue(UpdateVersionUtils.compareVersions("2.0.0", "2.0.1 Alpha") < 0)
        assertTrue(UpdateVersionUtils.compareVersions("2.1.0", "2.0.1") > 0)

        // Equal numeric version: Stable (3) > Beta (2) > Alpha (1)
        assertTrue(UpdateVersionUtils.compareVersions("2.0.0", "2.0.0 Beta") > 0)
        assertTrue(UpdateVersionUtils.compareVersions("2.0.0 Beta", "2.0.0 Alpha") > 0)
        assertTrue(UpdateVersionUtils.compareVersions("2.0.0 Alpha", "2.0.0") < 0)
        assertEquals(0, UpdateVersionUtils.compareVersions("2.0.0 Alpha", "Fotara_2.0.0_Alpha"))
        assertEquals(0, UpdateVersionUtils.compareVersions("2.0.0 Beta", "Fotara_2.0.0_Beta"))
    }

    @Test
    fun rollbackFiltering_sortsCandidatesDescending() {
        val currentVersion = "2.0.1 Alpha"
        val mockReleases = listOf(
            ReleaseInfo("1.8.4 Beta", "Fotara 1.8.4 Beta", "Notes", "https://example.com/184.apk"),
            ReleaseInfo("2.1.0 Alpha", "Fotara 2.1.0 Alpha", "Notes", "https://example.com/210.apk"),
            ReleaseInfo("1.9.0 Beta", "Fotara 1.9.0 Beta", "Notes", "https://example.com/190.apk"),
            ReleaseInfo("2.0.0 Alpha", "Fotara 2.0.0 Alpha", "Notes", "https://example.com/200.apk"),
            ReleaseInfo("2.0.1 Alpha", "Fotara 2.0.1 Alpha", "Notes", "https://example.com/201.apk")
        )

        // Filter for rollback candidates strictly older than currentVersion
        val rollbackCandidates = mockReleases
            .filter { UpdateVersionUtils.isOlderVersion(it.version, currentVersion, it.isPrerelease) }
            .sortedWith { a, b ->
                UpdateVersionUtils.compareVersions(b.version, a.version, b.isPrerelease, a.isPrerelease)
            }

        // Expected order: 2.0.0 Alpha, 1.9.0 Beta, 1.8.4 Beta (2.1.0 Alpha and 2.0.1 Alpha excluded)
        assertEquals(3, rollbackCandidates.size)
        assertEquals("2.0.0 Alpha", rollbackCandidates[0].version)
        assertEquals("1.9.0 Beta", rollbackCandidates[1].version)
        assertEquals("1.8.4 Beta", rollbackCandidates[2].version)
    }

    @Test
    fun cleanVersionString_cleansAlphaAndBetaPrefixes() {
        assertEquals("2.0.1", UpdateVersionUtils.cleanVersionString("Fotara_2.0.1_Alpha"))
        assertEquals("2.0.0", UpdateVersionUtils.cleanVersionString("Fotara_2.0.0_Alpha"))
        assertEquals("1.9.1", UpdateVersionUtils.cleanVersionString("1.9.1-alpha"))
        assertEquals("1.9.0", UpdateVersionUtils.cleanVersionString("Fotara_1.9.0_Beta"))
    }
}
