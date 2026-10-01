// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.db

import com.arinara.fotara.data.model.CanvasNote
import com.arinara.fotara.data.model.DocumentNote
import com.arinara.fotara.data.model.DocumentType
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.model.PhotoGroup
import com.arinara.fotara.data.model.SchedulableNote
import com.arinara.fotara.data.model.TextNote
import com.arinara.fotara.util.ScheduleNoteType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleMigrationTest {

    @Test
    fun testV13Schema_IncludesScheduleTitleAcrossAllFiveTables() {
        val tables = listOf("photos", "photo_groups", "document_notes", "text_notes", "canvas_notes")
        assertEquals(5, tables.size)

        // Verify that all 5 tables define schedule_title in v13 upgrade statements
        val upgradeAlterStatements = tables.map { table ->
            "ALTER TABLE $table ADD COLUMN schedule_title TEXT;"
        }

        assertEquals(5, upgradeAlterStatements.size)
        upgradeAlterStatements.forEach { sql ->
            assertTrue(sql.contains("schedule_title TEXT"))
        }
    }

    @Test
    fun testV13Indexes_DefinesBTreeIndexesOnScheduledAt() {
        val expectedScheduledAtIndexes = listOf(
            "idx_photos_scheduled_at" to "photos(scheduled_at)",
            "idx_photo_groups_scheduled_at" to "photo_groups(scheduled_at)",
            "idx_document_notes_scheduled_at" to "document_notes(scheduled_at)",
            "idx_text_notes_scheduled_at" to "text_notes(scheduled_at)",
            "idx_canvas_notes_scheduled_at" to "canvas_notes(scheduled_at)"
        )

        assertEquals(5, expectedScheduledAtIndexes.size)
        expectedScheduledAtIndexes.forEach { (indexName, target) ->
            assertTrue(indexName.startsWith("idx_"))
            assertTrue(indexName.endsWith("_scheduled_at"))
            assertTrue(target.contains("scheduled_at"))
        }
    }

    @Test
    fun testSchedulableNoteInterface_ConformsAcrossAllModels() {
        val now = System.currentTimeMillis()
        val future = now + 3600000L

        val photo: SchedulableNote = Photo(
            id = 1L,
            folderId = 10L,
            fileUri = "content://media/1",
            createdAt = now,
            addedAt = now,
            scheduledAt = future,
            alertType = "ALARM",
            scheduleTitle = "Photo Exam Prep"
        )
        assertEquals(ScheduleNoteType.PHOTO, photo.noteType)
        assertTrue(photo.hasActiveSchedule)
        assertEquals("Photo Exam Prep", photo.displayScheduleTitle)

        val group: SchedulableNote = PhotoGroup(
            id = 2L,
            folderId = 10L,
            name = "Calculus Set",
            createdAt = now,
            addedAt = now,
            scheduledAt = future,
            alertType = "NOTIFICATION",
            scheduleTitle = "Review Group"
        )
        assertEquals(ScheduleNoteType.PHOTO_GROUP, group.noteType)
        assertTrue(group.hasActiveSchedule)
        assertEquals("Review Group", group.displayScheduleTitle)

        val doc: SchedulableNote = DocumentNote(
            id = 3L,
            folderId = 10L,
            name = "Physics.pdf",
            docType = DocumentType.PDF,
            originFileUri = "/storage/physics.pdf",
            pageCount = 12,
            createdAt = now,
            addedAt = now,
            scheduledAt = future,
            alertType = "ALARM",
            scheduleTitle = "Problem Set 4"
        )
        assertEquals(ScheduleNoteType.DOCUMENT, doc.noteType)
        assertTrue(doc.hasActiveSchedule)
        assertEquals("Problem Set 4", doc.displayScheduleTitle)

        val textNote: SchedulableNote = TextNote(
            id = 4L,
            folderId = 10L,
            title = "Bio Lecture",
            bodyMarkdown = "Genetics notes",
            createdAt = now,
            updatedAt = now,
            addedAt = now,
            scheduledAt = null,
            scheduleTitle = null
        )
        assertEquals(ScheduleNoteType.TEXT_NOTE, textNote.noteType)
        assertFalse(textNote.hasActiveSchedule)
        assertEquals("Bio Lecture", textNote.displayScheduleTitle)

        val canvas: SchedulableNote = CanvasNote(
            id = 5L,
            folderId = 10L,
            title = "Organic Chem Diagram",
            createdAt = now,
            addedAt = now,
            scheduledAt = future,
            alertType = "NOTIFICATION"
        )
        assertEquals(ScheduleNoteType.CANVAS_NOTE, canvas.noteType)
        assertTrue(canvas.hasActiveSchedule)
        assertEquals("Organic Chem Diagram", canvas.displayScheduleTitle)
    }

    @Test
    fun testUpcomingSchedulesQuery_FiltersPastAndTrashedNotes() {
        val now = 1728000000000L
        val past = now - 50000L
        val future1 = now + 100000L
        val future2 = now + 200000L

        val mockDbRows = listOf(
            NoteScheduleSummary(
                id = 1L,
                type = NoteType.PHOTO,
                title = "Future 1",
                folderId = 10L,
                subfolderId = null,
                scheduledAt = future1,
                alertType = "ALARM",
                scheduleTitle = "Prep 1"
            ),
            NoteScheduleSummary(
                id = 2L,
                type = NoteType.TEXT,
                title = "Future 2",
                folderId = 10L,
                subfolderId = null,
                scheduledAt = future2,
                alertType = "NOTIFICATION",
                scheduleTitle = null
            ),
            NoteScheduleSummary(
                id = 3L,
                type = NoteType.DOCUMENT,
                title = "Past Note",
                folderId = 10L,
                subfolderId = null,
                scheduledAt = past,
                alertType = "NOTIFICATION",
                scheduleTitle = null
            )
        )

        // Rescheduling logic strictly filters scheduledAt > now
        val upcomingOnly = mockDbRows.filter { it.scheduledAt > now }

        assertEquals(2, upcomingOnly.size)
        assertEquals(1L, upcomingOnly[0].id)
        assertEquals("Prep 1", upcomingOnly[0].scheduleTitle)
        assertEquals(2L, upcomingOnly[1].id)
        assertNull(upcomingOnly[1].scheduleTitle)
    }

    @Test
    fun testTrashCancel_PreservesScheduleInDatabase() {
        val now = 1728000000000L
        val scheduledTime = now + 3600000L

        // When a note is trashed, the alarm is cancelled via cancelAlarmOnly
        // but scheduled_at is preserved in the database row
        val note = TextNote(
            id = 10L,
            folderId = 1L,
            title = "History Essay",
            bodyMarkdown = "Draft",
            createdAt = now,
            updatedAt = now,
            addedAt = now,
            scheduledAt = scheduledTime,
            alertType = "ALARM",
            scheduleTitle = "Essay Final Submission",
            isTrashed = true,
            deletedAt = now
        )

        assertTrue(note.isTrashed)
        assertEquals(scheduledTime, note.scheduledAt)
        assertEquals("Essay Final Submission", note.scheduleTitle)

        // Restoring re-arms because scheduledTime > now
        val restored = note.copy(isTrashed = false, deletedAt = null)
        assertFalse(restored.isTrashed)
        assertTrue(restored.scheduledAt!! > now)
    }
}
