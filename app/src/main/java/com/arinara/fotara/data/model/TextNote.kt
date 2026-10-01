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
    val deletedAt: Long? = null,
    val scheduledAt: Long? = null,
    val alertType: String? = null
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
                .replace(Regex("""(?m)^```[^\n]*$"""), "")
                .replace(Regex("""(?m)^#{1,6}\s+"""), "")
                .replace(Regex("""\*\*\*([^*\n]+?)\*\*\*"""), "$1")
                .replace(Regex("""___([^_\n]+?)___"""), "$1")
                .replace(Regex("""\*\*([^*\n]+?)\*\*"""), "$1")
                .replace(Regex("""__([^_\n]+?)__"""), "$1")
                .replace(Regex("""(?<!\*)\*([^*\n]+?)\*(?!\*)"""), "$1")
                .replace(Regex("""(?<!_)_([^_\n]+?)_(?!_)"""), "$1")
                .replace(Regex("""~~([^~\n]+?)~~"""), "$1")
                .replace(Regex("""`([^`\n]+?)`"""), "$1")
                .replace(Regex("""(?m)^>\s+"""), "")
                .replace(Regex("""(?m)^\s*[-*+]\s*\[[ xX]\]\s*"""), "")
                .replace(Regex("""(?m)^\s*[-*+]\s+"""), "")
                .replace(Regex("""(?m)^\s*\d+\.\s+"""), "")
                .replace(Regex("""\[([^\]\n]+?)\]\([^)\n]+?\)"""), "$1")
                .replace(Regex("""(?m)^---+$"""), "")
                .trim()
        }
    }
}
