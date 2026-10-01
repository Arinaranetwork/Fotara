// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.note.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.arinara.fotara.data.repository.TextNoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun rememberEditorState(
    initialNoteId: Long?,
    folderId: Long,
    subfolderId: Long?,
    textNoteRepository: TextNoteRepository,
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): EditorState {
    return remember(initialNoteId, folderId, subfolderId) {
        EditorState(
            noteId = initialNoteId,
            folderId = folderId,
            subfolderId = subfolderId,
            textNoteRepository = textNoteRepository,
            scope = coroutineScope
        )
    }
}

/**
 * Single source of truth state holder for the Fotara text note editor.
 * Owns text editing state, selection, undo/redo history, active syntax states,
 * find & replace, and debounced persistence.
 */
@Stable
class EditorState(
    var noteId: Long?,
    val folderId: Long,
    val subfolderId: Long?,
    private val textNoteRepository: TextNoteRepository,
    private val scope: CoroutineScope
) {
    var title by mutableStateOf("")
        private set

    var bodyValue by mutableStateOf(TextFieldValue(""))
        private set

    var isSaving by mutableStateOf(false)
        private set

    var hasUserEdited by mutableStateOf(false)
        private set

    var isLoaded by mutableStateOf(false)
        private set

    // Undo / Redo Stacks
    private val undoStack = mutableListOf<TextFieldValue>()
    private val redoStack = mutableListOf<TextFieldValue>()
    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set

    // Active Toolbar States
    var isBold by mutableStateOf(false)
        private set
    var isItalic by mutableStateOf(false)
        private set
    var isStrikethrough by mutableStateOf(false)
        private set
    var isInlineCode by mutableStateOf(false)
        private set
    var isH1 by mutableStateOf(false)
        private set
    var isH2 by mutableStateOf(false)
        private set
    var isH3 by mutableStateOf(false)
        private set
    var isQuote by mutableStateOf(false)
        private set
    var isBulletList by mutableStateOf(false)
        private set
    var isNumberedList by mutableStateOf(false)
        private set
    var isChecklist by mutableStateOf(false)
        private set

    // Counters
    var wordCount by mutableStateOf(0)
        private set
    var charCount by mutableStateOf(0)
        private set

    // Find and Replace
    var showFindReplace by mutableStateOf(false)
    var searchQuery by mutableStateOf("")
    var replaceQuery by mutableStateOf("")
    var caseSensitive by mutableStateOf(false)
    var searchMatchCount by mutableStateOf(0)
        private set
    var currentMatchIndex by mutableStateOf(0)
        private set

    // Link Dialog
    var showLinkDialog by mutableStateOf(false)
    var linkDialogLabel by mutableStateOf("")
    var linkDialogUrl by mutableStateOf("https://")

    // Schedule Dialog
    var showScheduleDialog by mutableStateOf(false)
    var scheduledAt by mutableStateOf<Long?>(null)
        private set
    var alertType by mutableStateOf<String?>(null)
        private set
    var scheduleTitle by mutableStateOf<String?>(null)
        private set

    fun openScheduleDialog() {
        showScheduleDialog = true
    }

    fun closeScheduleDialog() {
        showScheduleDialog = false
    }

    fun updateScheduleInfo(newScheduledAt: Long?, newAlertType: String?, newScheduleTitle: String?) {
        scheduledAt = newScheduledAt
        alertType = newAlertType
        scheduleTitle = newScheduleTitle
    }

    private var autosaveJob: Job? = null
    private var lastSnapshotMs = 0L

    init {
        loadNote()
    }

    private fun loadNote() {
        val id = noteId
        if (id != null && id > 0) {
            scope.launch {
                val note = textNoteRepository.getTextNoteByIdOnce(id)
                if (note != null && !hasUserEdited) {
                    title = note.title
                    bodyValue = TextFieldValue(note.bodyMarkdown, selection = TextRange(note.bodyMarkdown.length))
                    scheduledAt = note.scheduledAt
                    alertType = note.alertType
                    scheduleTitle = note.scheduleTitle
                    updateDerivedStates(bodyValue)
                    isLoaded = true
                }
            }
        } else {
            isLoaded = true
        }
    }

    fun onTitleChange(newTitle: String) {
        title = newTitle
        hasUserEdited = true
        scheduleAutosave()
    }

    fun onBodyChange(newValue: TextFieldValue) {
        hasUserEdited = true
        val oldText = bodyValue.text
        val newText = newValue.text

        // Coalesce typing into undo history
        val now = System.currentTimeMillis()
        val isWordBoundary = newText.length < oldText.length ||
                (newText.isNotEmpty() && (newText.last() == ' ' || newText.last() == '\n' || newText.last() in ".,;!?"))

        if (newText != oldText) {
            if (undoStack.isEmpty() || isWordBoundary || now - lastSnapshotMs > 1200L) {
                pushUndo(bodyValue)
                lastSnapshotMs = now
            }
        }

        bodyValue = newValue
        updateDerivedStates(newValue)
        scheduleAutosave()
    }

    private fun pushUndo(snapshot: TextFieldValue) {
        undoStack.add(snapshot)
        if (undoStack.size > 60) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
        canUndo = undoStack.isNotEmpty()
        canRedo = false
    }

    fun executeAction(transform: (TextFieldValue) -> TextFieldValue) {
        hasUserEdited = true
        pushUndo(bodyValue)
        val result = transform(bodyValue)
        bodyValue = result
        updateDerivedStates(result)
        scheduleAutosave()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.add(bodyValue)
            val prev = undoStack.removeAt(undoStack.lastIndex)
            bodyValue = prev
            updateDerivedStates(prev)
            canUndo = undoStack.isNotEmpty()
            canRedo = true
            scheduleAutosave()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.add(bodyValue)
            val next = redoStack.removeAt(redoStack.lastIndex)
            bodyValue = next
            updateDerivedStates(next)
            canUndo = true
            canRedo = redoStack.isNotEmpty()
            scheduleAutosave()
        }
    }

    private fun updateDerivedStates(value: TextFieldValue) {
        // 1. Parser and Active States
        val doc = MarkdownParser.parse(value.text)
        val active = doc.activeTypesAt(value.selection.start, value.selection.end)

        isBold = MarkdownSpanType.BOLD in active || MarkdownSpanType.BOLD_ITALIC in active
        isItalic = MarkdownSpanType.ITALIC in active || MarkdownSpanType.BOLD_ITALIC in active
        isStrikethrough = MarkdownSpanType.STRIKETHROUGH in active
        isInlineCode = MarkdownSpanType.INLINE_CODE in active
        isH1 = MarkdownSpanType.HEADING_1 in active
        isH2 = MarkdownSpanType.HEADING_2 in active
        isH3 = MarkdownSpanType.HEADING_3 in active
        isQuote = MarkdownSpanType.BLOCKQUOTE in active
        isBulletList = MarkdownSpanType.BULLET_LIST in active
        isNumberedList = MarkdownSpanType.NUMBERED_LIST in active
        isChecklist = MarkdownSpanType.CHECKLIST_UNCHECKED in active || MarkdownSpanType.CHECKLIST_CHECKED in active

        // 2. Counts
        val trimmed = value.text.trim()
        wordCount = if (trimmed.isEmpty()) 0 else trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }.size
        charCount = value.text.length

        // 3. Search matches refresh if search bar open
        if (showFindReplace && searchQuery.isNotEmpty()) {
            updateSearchMatches()
        }
    }

    private fun scheduleAutosave() {
        autosaveJob?.cancel()
        if (title.isBlank() && bodyValue.text.isBlank() && noteId == null) {
            return
        }
        autosaveJob = scope.launch {
            delay(350)
            flushAutosaveNow()
        }
    }

    suspend fun flushAutosaveNow() {
        val currentTitle = title.ifBlank { "Untitled Note" }
        val currentBody = bodyValue.text
        if (currentTitle.isBlank() && currentBody.isBlank() && noteId == null) return

        isSaving = true
        val currentId = noteId
        if (currentId == null || currentId <= 0) {
            val createdId = textNoteRepository.createTextNote(
                folderId = folderId,
                subfolderId = subfolderId,
                title = currentTitle,
                bodyMarkdown = currentBody
            )
            noteId = createdId
        } else {
            textNoteRepository.updateTextNote(
                id = currentId,
                title = currentTitle,
                bodyMarkdown = currentBody
            )
        }
        isSaving = false
    }

    suspend fun purgeIfCompletelyBlank(): Boolean {
        if (title.isBlank() && bodyValue.text.isBlank() && noteId != null && noteId!! > 0) {
            textNoteRepository.purgeTextNotePermanently(noteId!!)
            return true
        }
        return false
    }

    // --- Find and Replace Operations ---

    fun onSearchQueryChange(query: String) {
        searchQuery = query
        updateSearchMatches()
    }

    private fun updateSearchMatches() {
        if (searchQuery.isEmpty()) {
            searchMatchCount = 0
            currentMatchIndex = 0
            return
        }
        val text = bodyValue.text
        val regex = Regex(
            Regex.escape(searchQuery),
            if (caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
        )
        val matches = regex.findAll(text).toList()
        searchMatchCount = matches.size
        if (currentMatchIndex >= searchMatchCount) {
            currentMatchIndex = if (searchMatchCount > 0) 0 else 0
        }
    }

    fun findNext() {
        if (searchMatchCount == 0) return
        val text = bodyValue.text
        val regex = Regex(
            Regex.escape(searchQuery),
            if (caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
        )
        val matches = regex.findAll(text).toList()
        if (matches.isEmpty()) return

        currentMatchIndex = (currentMatchIndex + 1) % matches.size
        val target = matches[currentMatchIndex]
        bodyValue = bodyValue.copy(selection = TextRange(target.range.first, target.range.last + 1))
    }

    fun findPrevious() {
        if (searchMatchCount == 0) return
        val text = bodyValue.text
        val regex = Regex(
            Regex.escape(searchQuery),
            if (caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
        )
        val matches = regex.findAll(text).toList()
        if (matches.isEmpty()) return

        currentMatchIndex = if (currentMatchIndex - 1 < 0) matches.size - 1 else currentMatchIndex - 1
        val target = matches[currentMatchIndex]
        bodyValue = bodyValue.copy(selection = TextRange(target.range.first, target.range.last + 1))
    }

    fun replaceCurrent() {
        if (searchMatchCount == 0) return
        val sel = bodyValue.selection
        val text = bodyValue.text
        val start = sel.min.coerceIn(0, text.length)
        val end = sel.max.coerceIn(0, text.length)

        if (start < end) {
            val selected = text.substring(start, end)
            val matches = if (caseSensitive) selected == searchQuery else selected.equals(searchQuery, ignoreCase = true)
            if (matches) {
                executeAction {
                    val newText = text.substring(0, start) + replaceQuery + text.substring(end)
                    TextFieldValue(newText, selection = TextRange(start + replaceQuery.length))
                }
                findNext()
                return
            }
        }
        findNext()
    }

    fun replaceAll() {
        if (searchQuery.isEmpty()) return
        executeAction {
            val regex = Regex(
                Regex.escape(searchQuery),
                if (caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
            )
            val newText = bodyValue.text.replace(regex, replaceQuery)
            TextFieldValue(newText, selection = TextRange(0))
        }
        updateSearchMatches()
    }

    // --- Link Dialog Triggers ---

    fun openLinkDialog() {
        val sel = bodyValue.selection
        val text = bodyValue.text
        val start = sel.min.coerceIn(0, text.length)
        val end = sel.max.coerceIn(0, text.length)

        val doc = MarkdownParser.parse(text)
        val existingLink = doc.findLinkAt(start)

        if (existingLink != null) {
            linkDialogLabel = text.substring(existingLink.contentStart, existingLink.contentEnd)
            linkDialogUrl = existingLink.extra ?: "https://"
        } else {
            linkDialogLabel = if (start < end) text.substring(start, end) else ""
            linkDialogUrl = "https://"
        }
        showLinkDialog = true
    }

    fun confirmLinkDialog() {
        executeAction {
            EditorActions.insertOrEditLink(it, linkDialogLabel, linkDialogUrl)
        }
        showLinkDialog = false
    }
}
