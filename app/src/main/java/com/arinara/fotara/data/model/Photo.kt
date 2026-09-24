// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.model

enum class PhotoSource {
    CAMERA,
    SCREENSHOT,
    IMPORT
}

data class Photo(
    val id: Long = 0,
    val fileUri: String,
    val thumbnailUri: String? = null,
    val folderId: Long,
    val subfolderId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val addedAt: Long = System.currentTimeMillis(),
    val tagColor: String? = null,
    val caption: String? = null,
    val ocrText: String? = null,
    val source: PhotoSource = PhotoSource.CAMERA,
    val linkedDeadline: Long? = null,
    val fileSizeBytes: Long = 0L,
    val note: String? = null,
    val groupId: Long? = null,
    val tags: String? = null,
    val isTrashed: Boolean = false,
    val deletedAt: Long? = null
) {
    val tag: TagColor? get() = tagColor?.let { TagColor.fromHex(it) }

    fun getAllSmartTags(): List<String> {
        val extracted = linkedSetOf<String>()

        // 1. Explicit tags (comma/space separated)
        tags?.split(',', ' ')?.forEach { raw ->
            val cleaned = raw.trim().removePrefix("#").lowercase()
            if (cleaned.isNotBlank()) {
                extracted.add(cleaned)
            }
        }

        // 2. Hashtags extracted from caption and note via regex
        val hashtagRegex = Regex("""#([a-zA-Z0-9_]{2,24})""")
        caption?.let { text ->
            hashtagRegex.findAll(text).forEach { match ->
                extracted.add(match.groupValues[1].lowercase())
            }
        }
        note?.let { text ->
            hashtagRegex.findAll(text).forEach { match ->
                extracted.add(match.groupValues[1].lowercase())
            }
        }

        return extracted.toList().sorted()
    }
}
