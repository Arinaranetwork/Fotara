// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.online

enum class UpdateChannel {
    STABLE,
    BETA
}

object UpdateVersionUtils {

    fun cleanVersionString(raw: String): String {
        return raw
            .replace("Fotara", "", ignoreCase = true)
            .replace("Beta", "", ignoreCase = true)
            .trim('_', '-', ' ', 'v', 'V')
            .split("-", "_", " ")[0]
    }

    fun resolveChannel(rawVersion: String, isPrerelease: Boolean = false): UpdateChannel {
        return VersionInfo.parse(rawVersion, isPrerelease).channel
    }

    fun isBeta(rawVersion: String, isPrerelease: Boolean = false): Boolean {
        return resolveChannel(rawVersion, isPrerelease) == UpdateChannel.BETA
    }

    fun isNewerVersion(
        remoteTag: String,
        currentTag: String,
        remoteIsPrerelease: Boolean = false,
        currentIsPrerelease: Boolean = false
    ): Boolean {
        val remoteInfo = VersionInfo.parse(remoteTag, remoteIsPrerelease)
        val currentInfo = VersionInfo.parse(currentTag, currentIsPrerelease)

        val remoteParts = remoteInfo.numericVersion.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = currentInfo.numericVersion.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }

        // If numeric version is identical, a Stable build is newer than Beta
        val remoteBeta = remoteInfo.channel == UpdateChannel.BETA
        val currentBeta = currentInfo.channel == UpdateChannel.BETA

        if (!remoteBeta && currentBeta) {
            return true
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
        if (!isNewerVersion(release.version, currentVersion, release.isPrerelease)) return false

        if (skippedVersion != null) {
            val isSkippedEqual = cleanVersionString(release.version) == cleanVersionString(skippedVersion) &&
                    (isBeta(release.version, release.isPrerelease) == isBeta(skippedVersion))
            if (isSkippedEqual) {
                return false
            }
            if (!isNewerVersion(release.version, skippedVersion, release.isPrerelease)) {
                return false
            }
        }
        return true
    }
}
