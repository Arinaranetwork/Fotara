// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.audio

import com.arinara.fotara.audio.model.AudioAnnotation
import com.arinara.fotara.audio.repository.AudioAnnotationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.concurrent.atomic.AtomicLong

class FakeAudioAnnotationRepository : AudioAnnotationRepository {

    private val idCounter = AtomicLong(1L)
    val annotations = MutableStateFlow<List<AudioAnnotation>>(emptyList())
    val deletedFiles = mutableListOf<String>()

    override fun getAnnotationsForNote(noteId: Long): Flow<List<AudioAnnotation>> {
        return annotations.map { list -> list.filter { it.noteId == noteId } }
    }

    override fun getAnnotationsForPdfPage(pdfDocId: Long, pageIndex: Int): Flow<List<AudioAnnotation>> {
        return annotations.map { list -> list.filter { it.pdfDocId == pdfDocId && it.pdfPageIndex == pageIndex } }
    }

    override fun getAllAnnotationsForPdf(pdfDocId: Long): Flow<List<AudioAnnotation>> {
        return annotations.map { list -> list.filter { it.pdfDocId == pdfDocId } }
    }

    override suspend fun getAnnotationById(id: Long): AudioAnnotation? {
        return annotations.value.firstOrNull { it.id == id }
    }

    override suspend fun insertAnnotation(annotation: AudioAnnotation): Long {
        val assignedId = if (annotation.id == 0L) idCounter.getAndIncrement() else annotation.id
        val newAnnotation = annotation.copy(id = assignedId)
        annotations.value = annotations.value + newAnnotation
        return assignedId
    }

    override suspend fun deleteAnnotation(id: Long) {
        val existing = getAnnotationById(id)
        if (existing != null) {
            deleteAnnotationFile(existing.filePath)
        }
        annotations.value = annotations.value.filterNot { it.id == id }
    }

    override suspend fun deleteAnnotationFile(filePath: String) {
        deletedFiles.add(filePath)
    }
}
