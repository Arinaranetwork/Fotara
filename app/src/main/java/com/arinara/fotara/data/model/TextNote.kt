// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.model

data class TextNote(
    val id: Long = 0,
    val folderId: Long,
    val subfolderId: Long? = null,
    val title: String,
    val bodyMarkdown: String,
    val createdAt: Long,
    val updatedAt: Long,
    val addedAt: Long = createdAt,
    val tagColor: String? = null,
    val linkedDeadline: Long? = null,
    val isTrashed: Boolean = false,
    val deletedAt: Long? = null
) {
    val isBlank: Boolean
        get() = title.isBlank() && bodyMarkdown.isBlank()

    fun getPlainTextSnippet(maxLength: Int = 120): String {
        val clean = stripMarkdownFormatting(bodyMarkdown)
        return if (clean.length > maxLength) clean.take(maxLength) + "…" else clean
    }

    companion object {
        fun stripMarkdownFormatting(markdown: String): String {
            return markdown
                .replace(Regex("^#{1,6}\\s+", RegexOption.MULTILINE), "")
                .replace(Regex("\\*\\*\\*(.*?)\\*\\*\\*"), "$1")
                .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
                .replace(Regex("\\*(.*?)\\*"), "$1")
                .replace(Regex("~~(.*?)~~"), "$1")
                .replace(Regex("`{1,3}(.*?)`{1,3}"), "$1")
                .replace(Regex("^>\\s+", RegexOption.MULTILINE), "")
                .replace(Regex("^\\[[ xX]\\]\\s+", RegexOption.MULTILINE), "")
                .replace(Regex("^[-*+]\\s+", RegexOption.MULTILINE), "")
                .replace(Regex("^\\d+\\.\\s+", RegexOption.MULTILINE), "")
                .replace(Regex("\\[([^\\]]+)\\]\\([^)]+\\)"), "$1")
                .replace(Regex("^---+$", RegexOption.MULTILINE), "")
                .trim()
        }
    }
}
