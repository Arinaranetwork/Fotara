// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.util

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

data class HighlightBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

data class PageHighlightData(
    val imageWidth: Int,
    val imageHeight: Int,
    val boxes: List<HighlightBox>
)

object PdfSearchHighlighter {

    private const val MAX_BOXES_PER_PAGE = 200

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    // In-memory cache per (document, page, query)
    private val cache = ConcurrentHashMap<String, PageHighlightData>()

    fun clearCache() {
        cache.clear()
    }

    suspend fun getHighlightBoxes(
        context: Context,
        documentId: Long,
        pageIndex: Int,
        imageFile: File,
        query: String?
    ): PageHighlightData? = withContext(Dispatchers.IO) {
        if (query.isNullOrBlank() || !imageFile.exists()) return@withContext null

        val normalizedQuery = PdfPageMatcher.normalizeForSearch(query.trim())
        val terms = normalizedQuery.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (terms.isEmpty()) return@withContext null

        val cacheKey = "${documentId}_${pageIndex}_$normalizedQuery"
        cache[cacheKey]?.let { return@withContext it }

        try {
            val inputImage = InputImage.fromFilePath(context, Uri.fromFile(imageFile))
            val recognizedText = recognizer.process(inputImage).awaitTask()

            val boxes = mutableListOf<HighlightBox>()
            for (block in recognizedText.textBlocks) {
                for (line in block.lines) {
                    for (element in line.elements) {
                        val normElement = PdfPageMatcher.normalizeForSearch(element.text)
                        if (terms.any { normElement.contains(it) }) {
                            element.boundingBox?.let { rect ->
                                boxes.add(
                                    HighlightBox(
                                        left = rect.left.toFloat(),
                                        top = rect.top.toFloat(),
                                        right = rect.right.toFloat(),
                                        bottom = rect.bottom.toFloat()
                                    )
                                )
                            }
                            if (boxes.size >= MAX_BOXES_PER_PAGE) break
                        }
                    }
                    if (boxes.size >= MAX_BOXES_PER_PAGE) break
                }
                if (boxes.size >= MAX_BOXES_PER_PAGE) break
            }

            val data = PageHighlightData(
                imageWidth = inputImage.width,
                imageHeight = inputImage.height,
                boxes = boxes
            )
            cache[cacheKey] = data
            data
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result ->
            if (cont.isActive) cont.resume(result)
        }
        addOnFailureListener { exc ->
            if (cont.isActive) cont.resumeWith(Result.failure(exc))
        }
    }
}
