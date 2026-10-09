// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.audio

import com.arinara.fotara.audio.model.AudioAnnotation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AudioAnnotationRepositoryTest {

    private lateinit var repository: FakeAudioAnnotationRepository

    @Before
    fun setup() {
        repository = FakeAudioAnnotationRepository()
    }

    @Test
    fun insertAndQueryForNote_returnsMatchingAnnotations() = runTest {
        val note1Annotation = AudioAnnotation(
            noteId = 101L,
            filePath = "/data/audio/note_101.m4a",
            durationMs = 60000L
        )
        val note2Annotation = AudioAnnotation(
            noteId = 102L,
            filePath = "/data/audio/note_102.m4a",
            durationMs = 45000L
        )

        val id1 = repository.insertAnnotation(note1Annotation)
        val id2 = repository.insertAnnotation(note2Annotation)

        assertTrue(id1 > 0)
        assertTrue(id2 > 0)

        val listForNote1 = repository.getAnnotationsForNote(101L).first()
        assertEquals(1, listForNote1.size)
        assertEquals("/data/audio/note_101.m4a", listForNote1[0].filePath)

        val listForNote2 = repository.getAnnotationsForNote(102L).first()
        assertEquals(1, listForNote2.size)
        assertEquals(45000L, listForNote2[0].durationMs)
    }

    @Test
    fun getAnnotationsForPdfPage_filtersByDocIdAndPageIndex() = runTest {
        repository.insertAnnotation(
            AudioAnnotation(pdfDocId = 55L, pdfPageIndex = 0, filePath = "/data/audio/pdf55_p0.m4a", durationMs = 30000L)
        )
        repository.insertAnnotation(
            AudioAnnotation(pdfDocId = 55L, pdfPageIndex = 1, filePath = "/data/audio/pdf55_p1.m4a", durationMs = 50000L)
        )
        repository.insertAnnotation(
            AudioAnnotation(pdfDocId = 99L, pdfPageIndex = 0, filePath = "/data/audio/pdf99_p0.m4a", durationMs = 20000L)
        )

        val page0List = repository.getAnnotationsForPdfPage(55L, 0).first()
        assertEquals(1, page0List.size)
        assertEquals("/data/audio/pdf55_p0.m4a", page0List[0].filePath)

        val allForDoc55 = repository.getAllAnnotationsForPdf(55L).first()
        assertEquals(2, allForDoc55.size)
    }

    @Test
    fun deleteAnnotation_removesFromDbAndDeletesFile() = runTest {
        val id = repository.insertAnnotation(
            AudioAnnotation(noteId = 200L, filePath = "/data/audio/to_delete.m4a", durationMs = 15000L)
        )

        assertNotNull(repository.getAnnotationById(id))
        repository.deleteAnnotation(id)

        assertNull(repository.getAnnotationById(id))
        assertTrue(repository.deletedFiles.contains("/data/audio/to_delete.m4a"))
    }
}
