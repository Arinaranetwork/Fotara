// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.update

import com.arinara.fotara.online.ReleaseInfo
import com.arinara.fotara.online.UpdateChannel
import com.arinara.fotara.online.UpdateVersionUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateBannerAndVersionTest {

    @Test
    fun testVersionComparisonStableVsBeta() {
        // 1.6.0 Stable is newer than 1.5.11 Beta
        assertTrue(UpdateVersionUtils.isNewerVersion("1.6.0", "1.5.11 Beta"))

        // 1.6.0 Stable is newer than 1.5.9 Beta
        assertTrue(UpdateVersionUtils.isNewerVersion("1.6.0", "1.5.9 Beta"))

        // 1.6.0 Stable is newer than 1.6.0 Beta
        assertTrue(UpdateVersionUtils.isNewerVersion("1.6.0", "1.6.0 Beta"))

        // 1.6.0 Beta is NOT newer than 1.6.0 Stable
        assertFalse(UpdateVersionUtils.isNewerVersion("1.6.0 Beta", "1.6.0"))

        // Equal Stable versions are NOT newer
        assertFalse(UpdateVersionUtils.isNewerVersion("1.6.0", "1.6.0"))

        // Equal Beta versions are NOT newer
        assertFalse(UpdateVersionUtils.isNewerVersion("1.6.0 Beta", "1.6.0 Beta"))

        // 1.5.8 Beta is newer than 1.5.7 Beta
        assertTrue(UpdateVersionUtils.isNewerVersion("1.5.8 Beta", "1.5.7 Beta"))

        // Prefixes and suffixes: "Fotara v1.6.0" vs "1.5.10 Beta"
        assertTrue(UpdateVersionUtils.isNewerVersion("Fotara v1.6.0", "1.5.10 Beta"))
    }

    @Test
    fun testChannelResolution() {
        assertEquals(UpdateChannel.STABLE, UpdateVersionUtils.resolveChannel("1.6.0"))
        assertEquals(UpdateChannel.STABLE, UpdateVersionUtils.resolveChannel("Fotara 1.6.0"))
        assertEquals(UpdateChannel.BETA, UpdateVersionUtils.resolveChannel("1.6.0 Beta"))
        assertEquals(UpdateChannel.BETA, UpdateVersionUtils.resolveChannel("1.5.11 Beta"))
        assertEquals(UpdateChannel.BETA, UpdateVersionUtils.resolveChannel("1.6.0", isPrerelease = true))
    }

    @Test
    fun testShouldShowUpdatePopupLogic() {
        val releaseStable = ReleaseInfo(
            version = "1.6.0",
            title = "Fotara 1.6.0",
            releaseNotes = "Highlights",
            downloadUrl = "https://example.com/fotara.apk",
            bannerUrl = null,
            htmlUrl = "https://github.com/Arinaranetwork/Fotara/releases/tag/v1.6.0",
            isPrerelease = false
        )

        // Show when newer and not dismissed
        assertTrue(
            UpdateVersionUtils.shouldShowUpdatePopup(
                release = releaseStable,
                currentVersion = "1.5.11 Beta",
                skippedVersion = null,
                isPopupDismissedForSession = false
            )
        )

        // Do not show when dismissed for session
        assertFalse(
            UpdateVersionUtils.shouldShowUpdatePopup(
                release = releaseStable,
                currentVersion = "1.5.11 Beta",
                skippedVersion = null,
                isPopupDismissedForSession = true
            )
        )

        // Do not show when version was explicitly skipped
        assertFalse(
            UpdateVersionUtils.shouldShowUpdatePopup(
                release = releaseStable,
                currentVersion = "1.5.11 Beta",
                skippedVersion = "1.6.0",
                isPopupDismissedForSession = false
            )
        )

        // Show when newer release appears beyond skipped version (e.g. 1.7.0 > skipped 1.6.0)
        val release170 = releaseStable.copy(version = "1.7.0")
        assertTrue(
            UpdateVersionUtils.shouldShowUpdatePopup(
                release = release170,
                currentVersion = "1.5.11 Beta",
                skippedVersion = "1.6.0",
                isPopupDismissedForSession = false
            )
        )
    }
}
