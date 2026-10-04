// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.online

object UpdateVersionUtils {

    fun cleanVersionString(raw: String): String {
        return raw
            .replace("Fotara", "", ignoreCase = true)
            .trim('_', '-', ' ', 'v', 'V')
            .split("-", "_", " ")[0]
    }

    fun isNewerVersion(remoteTag: String, currentTag: String): Boolean {
        val cleanRemote = cleanVersionString(remoteTag)
        val cleanCurrent = cleanVersionString(currentTag)

        val remoteParts = cleanRemote.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    fun shouldShowUpdatePopup(
        release: ReleaseInfo?,
        currentVersion: String,
        skippedVersion: String?,
        isPopupDismissedForSession: Boolean
    ): Boolean {
        if (release == null) return false
        if (isPopupDismissedForSession) return false
        if (!isNewerVersion(release.version, currentVersion)) return false

        if (skippedVersion != null) {
            if (cleanVersionString(release.version) == cleanVersionString(skippedVersion)) {
                return false
            }
            if (!isNewerVersion(release.version, skippedVersion)) {
                return false
            }
        }
        return true
    }
}
