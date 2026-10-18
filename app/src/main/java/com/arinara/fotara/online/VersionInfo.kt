// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.online

/**
 * Pure model and parser for version strings and channels across Fotara.
 * Ensures numeric versions and release channels are cleanly separated and
 * never duplicated in UI labels, snackbars, pills, or dialogs.
 */
data class VersionInfo(
    val numericVersion: String,
    val channel: UpdateChannel
) {
    val displayVersion: String get() = "v$numericVersion"
    val channelLabel: String get() = when (channel) {
        UpdateChannel.ALPHA -> "Alpha"
        UpdateChannel.BETA -> "Beta"
        UpdateChannel.STABLE -> "Stable"
    }

    companion object {
        private val ALPHA_PATTERN = Regex("""(?i)(?:^|[\s_\-])alpha(?:[\s_\-]|$)""")
        private val BETA_PATTERN = Regex("""(?i)(?:^|[\s_\-])beta(?:[\s_\-]|$)""")

        /**
         * Parses a raw version string (e.g. "2.0.1 Alpha", "1.5.7 Beta", "v1.7.0", "Fotara_1.6.0_Beta")
         * into a clean [VersionInfo]. Tolerant of various channel suffixes and casing.
         */
        fun parse(rawVersion: String?, isPrerelease: Boolean = false): VersionInfo {
            if (rawVersion.isNullOrBlank()) {
                return VersionInfo("1.0.0", if (isPrerelease) UpdateChannel.BETA else UpdateChannel.STABLE)
            }

            val trimmed = rawVersion.trim()
            val hasAlphaSuffix = ALPHA_PATTERN.containsMatchIn(trimmed) || trimmed.contains("alpha", ignoreCase = true)
            val hasBetaSuffix = BETA_PATTERN.containsMatchIn(trimmed) || trimmed.contains("beta", ignoreCase = true)

            val channel = when {
                hasAlphaSuffix -> UpdateChannel.ALPHA
                hasBetaSuffix -> UpdateChannel.BETA
                isPrerelease -> UpdateChannel.BETA
                else -> UpdateChannel.STABLE
            }

            val numeric = UpdateVersionUtils.cleanVersionString(trimmed)
            return VersionInfo(
                numericVersion = numeric.ifBlank { "1.0.0" },
                channel = channel
            )
        }
    }
}
