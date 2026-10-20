// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data

import com.arinara.fotara.data.model.TextNote
import com.arinara.fotara.test.FakeTextNoteRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TextNoteGroupTest {

    @Test
    fun textNote_defaultGroupIdIsNull() {
        val note = TextNote(
            id = 1L,
            folderId = 10L,
            title = "Test Note",
            bodyMarkdown = "Hello world"
        )
        assertNull(note.groupId)
    }

    @Test
    fun textNote_canBeMovedToGroupAndRetrieved() = runTest {
        val initialNote = TextNote(
            id = 1L,
            folderId = 10L,
            title = "Lecture Notes",
            bodyMarkdown = "# Biology Notes"
        )
        val repository = FakeTextNoteRepository(listOf(initialNote))

        // Before moving, standalone folder query returns 1 item
        val standaloneBefore = repository.getTextNotesByFolder(10L, null).first()
        assertEquals(1, standaloneBefore.size)

        // Move to group 50
        repository.moveTextNoteToGroup(1L, 50L)

        // After moving, standalone query returns 0 items
        val standaloneAfter = repository.getTextNotesByFolder(10L, null).first()
        assertTrue(standaloneAfter.isEmpty())

        // Group query returns the note
        val groupNotes = repository.getTextNotesByGroup(50L).first()
        assertEquals(1, groupNotes.size)
        assertEquals(1L, groupNotes.first().id)
        assertEquals(50L, groupNotes.first().groupId)

        // Move out of group (groupId = null)
        repository.moveTextNoteToGroup(1L, null)
        val standaloneRestored = repository.getTextNotesByFolder(10L, null).first()
        assertEquals(1, standaloneRestored.size)
        assertNull(standaloneRestored.first().groupId)
    }

    @Test
    fun textNotes_batchMoveToGroup() = runTest {
        val note1 = TextNote(id = 1L, folderId = 10L, title = "Note 1", bodyMarkdown = "Body 1")
        val note2 = TextNote(id = 2L, folderId = 10L, title = "Note 2", bodyMarkdown = "Body 2")
        val note3 = TextNote(id = 3L, folderId = 10L, title = "Note 3", bodyMarkdown = "Body 3")
        val repository = FakeTextNoteRepository(listOf(note1, note2, note3))

        // Move notes 1 and 2 to group 77
        repository.moveTextNotesToGroup(listOf(1L, 2L), 77L)

        val groupNotes = repository.getTextNotesByGroup(77L).first()
        assertEquals(2, groupNotes.size)
        assertTrue(groupNotes.any { it.id == 1L })
        assertTrue(groupNotes.any { it.id == 2L })

        // Note 3 remains standalone
        val standalone = repository.getTextNotesByFolder(10L, null).first()
        assertEquals(1, standalone.size)
        assertEquals(3L, standalone.first().id)
    }
}
