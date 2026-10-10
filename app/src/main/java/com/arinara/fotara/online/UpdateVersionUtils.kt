// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.online

enum class UpdateChannel {
    STABLE,
    BETA,
    ALPHA
}

object UpdateVersionUtils {

    fun cleanVersionString(raw: String): String {
        return raw
            .replace("Fotara", "", ignoreCase = true)
            .replace("Alpha", "", ignoreCase = true)
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

    fun isAlpha(rawVersion: String, isPrerelease: Boolean = false): Boolean {
        return resolveChannel(rawVersion, isPrerelease) == UpdateChannel.ALPHA
    }

    /**
     * Compares two versions numerically and by channel rank.
     * Returns:
     *  > 0 if [tagA] is newer than [tagB]
     *  < 0 if [tagA] is older than [tagB]
     *  0 if both are identical in version and channel
     */
    fun compareVersions(
        tagA: String,
        tagB: String,
        isPrereleaseA: Boolean = false,
        isPrereleaseB: Boolean = false
    ): Int {
        val infoA = VersionInfo.parse(tagA, isPrereleaseA)
        val infoB = VersionInfo.parse(tagB, isPrereleaseB)

        val partsA = infoA.numericVersion.split(".").mapNotNull { it.toIntOrNull() }
        val partsB = infoB.numericVersion.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(partsA.size, partsB.size)
        for (i in 0 until maxLen) {
            val a = partsA.getOrElse(i) { 0 }
            val b = partsB.getOrElse(i) { 0 }
            if (a > b) return 1
            if (a < b) return -1
        }

        // Channels precedence for identical numeric version: STABLE (3) > BETA (2) > ALPHA (1)
        fun channelRank(channel: UpdateChannel): Int = when (channel) {
            UpdateChannel.STABLE -> 3
            UpdateChannel.BETA -> 2
            UpdateChannel.ALPHA -> 1
        }

        val rankA = channelRank(infoA.channel)
        val rankB = channelRank(infoB.channel)
        return rankA.compareTo(rankB)
    }

    fun isNewerVersion(
        remoteTag: String,
        currentTag: String,
        remoteIsPrerelease: Boolean = false,
        currentIsPrerelease: Boolean = false
    ): Boolean {
        return compareVersions(remoteTag, currentTag, remoteIsPrerelease, currentIsPrerelease) > 0
    }

    fun isOlderVersion(
        remoteTag: String,
        currentTag: String,
        remoteIsPrerelease: Boolean = false,
        currentIsPrerelease: Boolean = false
    ): Boolean {
        return compareVersions(remoteTag, currentTag, remoteIsPrerelease, currentIsPrerelease) < 0
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
                    (resolveChannel(release.version, release.isPrerelease) == resolveChannel(skippedVersion))
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
