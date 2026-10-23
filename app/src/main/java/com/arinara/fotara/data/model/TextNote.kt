// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.model

import com.arinara.fotara.util.ScheduleNoteType

data class TextNote(
    override val id: Long = 0,
    override val folderId: Long,
    override val subfolderId: Long? = null,
    val groupId: Long? = null,
    override val title: String,
    val bodyMarkdown: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val addedAt: Long = createdAt,
    val tagColor: String? = null,
    val linkedDeadline: Long? = null,
    val isTrashed: Boolean = false,
    val deletedAt: Long? = null,
    override val scheduledAt: Long? = null,
    override val alertType: String? = null,
    override val scheduleTitle: String? = null,
    override val isPinned: Boolean = false
) : SchedulableNote {
    override val noteType: ScheduleNoteType get() = ScheduleNoteType.TEXT_NOTE
    val isBlank: Boolean
        get() = title.isBlank() && bodyMarkdown.isBlank()

    fun getPlainTextSnippet(maxLength: Int = 120): String {
        val clean = stripMarkdownFormatting(bodyMarkdown)
        return if (clean.length > maxLength) clean.take(maxLength) + "…" else clean
    }

    companion object {
        private val CODE_FENCE_REGEX = Regex("""(?m)^```[^\n]*$""")
        private val HEADING_REGEX = Regex("""(?m)^#{1,6}\s+""")
        private val BOLD_ITALIC_STAR_REGEX = Regex("""\*\*\*([^*\n]+?)\*\*\*""")
        private val BOLD_ITALIC_UNDER_REGEX = Regex("""___([^_\n]+?)___""")
        private val BOLD_STAR_REGEX = Regex("""\*\*([^*\n]+?)\*\*""")
        private val BOLD_UNDER_REGEX = Regex("""__([^_\n]+?)__""")
        private val ITALIC_STAR_REGEX = Regex("""(?<!\*)\*([^*\n]+?)\*(?!\*)""")
        private val ITALIC_UNDER_REGEX = Regex("""(?<!_)_([^_\n]+?)_(?!_)""")
        private val STRIKE_REGEX = Regex("""~~([^~\n]+?)~~""")
        private val INLINE_CODE_REGEX = Regex("""`([^`\n]+?)`""")
        private val BLOCKQUOTE_REGEX = Regex("""(?m)^>\s+""")
        private val CHECKLIST_REGEX = Regex("""(?m)^\s*[-*+]\s*\[[ xX]\]\s*""")
        private val BULLET_REGEX = Regex("""(?m)^\s*[-*+]\s+""")
        private val NUMBERED_LIST_REGEX = Regex("""(?m)^\s*\d+\.\s+""")
        private val LINK_REGEX = Regex("""\[([^\]\n]+?)\]\([^)\n]+?\)""")
        private val HR_REGEX = Regex("""(?m)^---+$""")

        fun stripMarkdownFormatting(markdown: String): String {
            return markdown
                .replace(CODE_FENCE_REGEX, "")
                .replace(HEADING_REGEX, "")
                .replace(BOLD_ITALIC_STAR_REGEX, "$1")
                .replace(BOLD_ITALIC_UNDER_REGEX, "$1")
                .replace(BOLD_STAR_REGEX, "$1")
                .replace(BOLD_UNDER_REGEX, "$1")
                .replace(ITALIC_STAR_REGEX, "$1")
                .replace(ITALIC_UNDER_REGEX, "$1")
                .replace(STRIKE_REGEX, "$1")
                .replace(INLINE_CODE_REGEX, "$1")
                .replace(BLOCKQUOTE_REGEX, "")
                .replace(CHECKLIST_REGEX, "")
                .replace(BULLET_REGEX, "")
                .replace(NUMBERED_LIST_REGEX, "")
                .replace(LINK_REGEX, "$1")
                .replace(HR_REGEX, "")
                .trim()
        }
    }
}
