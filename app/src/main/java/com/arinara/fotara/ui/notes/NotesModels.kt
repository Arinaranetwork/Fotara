// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.notes

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.arinara.fotara.R
import com.arinara.fotara.data.model.DocumentType
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class NoteFilterChip(val labelRes: Int) {
    ALL(R.string.filter_all),
    PHOTOS(R.string.filter_photos),
    DOCUMENTS(R.string.filter_documents),
    TEXT(R.string.filter_text),
    CANVAS(R.string.filter_canvas)
}

enum class UnifiedNoteType {
    PHOTO,
    DOCUMENT,
    TEXT,
    CANVAS
}

data class UnifiedNoteItem(
    val id: Long,
    val type: UnifiedNoteType,
    val title: String,
    val folderId: Long,
    val folderName: String,
    val subfolderId: Long?,
    val addedAt: Long,
    val previewUri: String? = null,
    val documentType: DocumentType? = null,
    val rawItem: Any
)

data class DateGroup(
    val date: LocalDate,
    val label: String,
    val dotColor: Color,
    val items: List<UnifiedNoteItem>
)

object NotesDateUtils {

    val DotBlue = ElectricSlateTokens.PrimaryAccent
    val DotPurple = ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.7f)
    val DotGreen = ElectricSlateTokens.TextMuted
    val DotAmber = ElectricSlateTokens.TextMuted
    val DotCyan = ElectricSlateTokens.TextMuted

    fun getGroupDotColor(date: LocalDate, today: LocalDate, yesterday: LocalDate): Color {
        return when (date) {
            today -> DotBlue
            yesterday -> DotPurple
            else -> DotGreen
        }
    }

    fun formatGroupDateLabel(date: LocalDate, today: LocalDate, yesterday: LocalDate, context: Context? = null): String {
        return when (date) {
            today -> context?.getString(R.string.date_today) ?: "TODAY"
            yesterday -> context?.getString(R.string.date_yesterday) ?: "YESTERDAY"
            else -> {
                val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
                date.format(formatter).uppercase(Locale.getDefault())
            }
        }
    }

    fun formatNoteTimestamp(timestampMs: Long, context: Context): String {
        val zone = ZoneId.systemDefault()
        val noteDateTime = Instant.ofEpochMilli(timestampMs).atZone(zone)
        val noteDate = noteDateTime.toLocalDate()
        val today = LocalDate.now(zone)
        val yesterday = today.minusDays(1)

        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
        val timeStr = noteDateTime.format(timeFormatter)

        return when (noteDate) {
            today -> timeStr
            yesterday -> context.getString(R.string.yesterday_prefix, timeStr)
            else -> {
                val dateFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
                context.getString(R.string.older_date_prefix, noteDateTime.format(dateFormatter), timeStr)
            }
        }
    }
}
