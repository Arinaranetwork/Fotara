// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.model

enum class SortOrder(val displayName: String) {
    UPLOAD_DATE("Upload Date"),
    NEAREST_DEADLINE("Nearest Deadline"),
    COLOR_LABEL("Color Label");

    companion object {
        fun fromName(name: String?): SortOrder {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: UPLOAD_DATE
        }
    }
}

enum class ThemeMode(val displayName: String) {
    SYSTEM("System Default"),
    DARK("Dark Mode"),
    LIGHT("Light Mode");

    companion object {
        fun fromName(name: String?): ThemeMode {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: SYSTEM
        }
    }
}

enum class DownsampleQuality(val displayName: String) {
    HIGH_QUALITY("High Quality (Slower)"),
    FAST_PROCESSING("Fast Processing");

    companion object {
        fun fromName(name: String?): DownsampleQuality {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: HIGH_QUALITY
        }
    }
}

enum class StorageLocation(val displayName: String) {
    INTERNAL("Internal App Storage"),
    EXTERNAL("Scoped External / SD Card");

    companion object {
        fun fromName(name: String?): StorageLocation {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: INTERNAL
        }
    }
}

data class StorageBreakdown(
    val photosSizeBytes: Long = 0L,
    val thumbnailsSizeBytes: Long = 0L,
    val databaseSizeBytes: Long = 0L
) {
    val totalSizeBytes: Long get() = photosSizeBytes + thumbnailsSizeBytes + databaseSizeBytes

    val formattedPhotos: String get() = formatBytes(photosSizeBytes)
    val formattedThumbnails: String get() = formatBytes(thumbnailsSizeBytes)
    val formattedDatabase: String get() = formatBytes(databaseSizeBytes)
    val formattedTotal: String get() = formatBytes(totalSizeBytes)

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(java.util.Locale.US, "%.1f GB", gb)
            mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(java.util.Locale.US, "%.1f KB", kb)
            else -> "$bytes B"
        }
    }
}

data class UserSettings(
    val defaultSortOrder: SortOrder = SortOrder.UPLOAD_DATE,
    val gridDensity: Int = 3,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val autoOcrEnabled: Boolean = true,
    val ocrLanguage: String = "Latin",
    val downsampleQuality: DownsampleQuality = DownsampleQuality.HIGH_QUALITY,
    val reminderLeadTimeHours: Int = 1,
    val dueTomorrowRibbonEnabled: Boolean = true,
    val storageLocation: StorageLocation = StorageLocation.INTERNAL
)

data class ImportResult(
    val success: Boolean,
    val foldersImported: Int = 0,
    val photosImported: Int = 0,
    val message: String
)
