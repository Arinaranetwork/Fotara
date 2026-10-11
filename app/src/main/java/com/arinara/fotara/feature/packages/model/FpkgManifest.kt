// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.packages.model

import java.util.Locale

/**
 * Immutable metadata manifest for Fotara Modular Packages (.fpkg).
 * Governs package identification, host compatibility, cryptographic integrity,
 * requested permissions, and runtime enablement.
 */
data class FpkgManifest(
    val packageId: String,
    val name: String,
    val version: String,
    val minFotaraVersion: String,
    val description: String,
    val iconKey: String,
    val sizeBytes: Long,
    val permissions: List<String>,
    val signatureSha256: String,
    val isEnabled: Boolean = true,
    val isInstalled: Boolean = false
) {
    /**
     * Formats the byte size into an academic human-readable string (e.g. "3.2 MB", "4.8 MB").
     */
    fun formattedSize(): String {
        return when {
            sizeBytes >= 1024L * 1024L -> {
                String.format(Locale.US, "%.1f MB", sizeBytes.toDouble() / (1024.0 * 1024.0))
            }
            sizeBytes >= 1024L -> {
                String.format(Locale.US, "%.1f KB", sizeBytes.toDouble() / 1024.0)
            }
            else -> "$sizeBytes B"
        }
    }

    /**
     * Serializes this manifest to a standard JSON representation.
     */
    fun toJson(): String {
        val escapedPermissions = permissions.joinToString(separator = ",") { "\"${escapeJson(it)}\"" }
        return """
        {
          "packageId": "${escapeJson(packageId)}",
          "name": "${escapeJson(name)}",
          "version": "${escapeJson(version)}",
          "minFotaraVersion": "${escapeJson(minFotaraVersion)}",
          "description": "${escapeJson(description)}",
          "iconKey": "${escapeJson(iconKey)}",
          "sizeBytes": $sizeBytes,
          "permissions": [$escapedPermissions],
          "signatureSha256": "${escapeJson(signatureSha256)}",
          "isEnabled": $isEnabled,
          "isInstalled": $isInstalled
        }
        """.trimIndent()
    }

    companion object {
        private fun escapeJson(input: String): String {
            return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
        }

        private fun unescapeJson(input: String): String {
            return input.replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\\", "\\")
        }

        /**
         * Parses a JSON string into an [FpkgManifest] instance.
         * Pure Kotlin implementation ensuring reliable execution across both
         * Android runtime and local JVM unit test environments.
         */
        fun fromJson(jsonStr: String): FpkgManifest? {
            try {
                fun extractString(key: String): String? {
                    val pattern = Regex("\"$key\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\"")
                    return pattern.find(jsonStr)?.groupValues?.get(1)?.let { unescapeJson(it) }
                }

                fun extractLong(key: String): Long? {
                    val pattern = Regex("\"$key\"\\s*:\\s*([0-9]+)")
                    return pattern.find(jsonStr)?.groupValues?.get(1)?.toLongOrNull()
                }

                fun extractBoolean(key: String): Boolean? {
                    val pattern = Regex("\"$key\"\\s*:\\s*(true|false)")
                    return pattern.find(jsonStr)?.groupValues?.get(1)?.toBooleanStrictOrNull()
                }

                fun extractStringList(key: String): List<String> {
                    val arrayPattern = Regex("\"$key\"\\s*:\\s*\\[([^\\]]*)\\]")
                    val arrayContent = arrayPattern.find(jsonStr)?.groupValues?.get(1) ?: return emptyList()
                    val itemPattern = Regex("\"((?:\\\\\"|[^\"])*)\"")
                    return itemPattern.findAll(arrayContent).map { unescapeJson(it.groupValues[1]) }.toList()
                }

                val packageId = extractString("packageId") ?: return null
                val name = extractString("name") ?: return null
                val version = extractString("version") ?: "1.0.0"
                val minFotaraVersion = extractString("minFotaraVersion") ?: "2.0.0"
                val description = extractString("description") ?: ""
                val iconKey = extractString("iconKey") ?: ""
                val sizeBytes = extractLong("sizeBytes") ?: 0L
                val permissions = extractStringList("permissions")
                val signatureSha256 = extractString("signatureSha256") ?: ""
                val isEnabled = extractBoolean("isEnabled") ?: true
                val isInstalled = extractBoolean("isInstalled") ?: false

                return FpkgManifest(
                    packageId = packageId,
                    name = name,
                    version = version,
                    minFotaraVersion = minFotaraVersion,
                    description = description,
                    iconKey = iconKey,
                    sizeBytes = sizeBytes,
                    permissions = permissions,
                    signatureSha256 = signatureSha256,
                    isEnabled = isEnabled,
                    isInstalled = isInstalled
                )
            } catch (e: Exception) {
                return null
            }
        }
    }
}
