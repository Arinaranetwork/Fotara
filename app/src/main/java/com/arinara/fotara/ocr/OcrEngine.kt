// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ocr

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
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class OcrResult(
    val fullText: String,
    val keywords: List<String>,
    val detectedSubjectHint: String? = null,
    val confidence: Float = 0.95f,
    val processingTimeMs: Long = 0L
)

interface OcrEngine {
    suspend fun extractText(imageUri: String, rawContentHint: String? = null): OcrResult
    fun extractKeywords(text: String): List<String>
}

class MlKitOcrEngine(private val context: Context? = null) : OcrEngine {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    private val commonStopwords = setOf(
        "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
        "of", "with", "by", "from", "up", "about", "into", "over", "after",
        "is", "are", "was", "were", "be", "been", "being", "have", "has", "had",
        "it", "its", "that", "this", "these", "those", "which", "who", "whom"
    )

    private val academicSubjectKeywords = mapOf(
        "Biology" to listOf("cell", "membrane", "mitochondria", "atp", "dna", "rna", "protein", "enzyme", "genetics", "mitosis", "meiosis", "organelle", "chloroplast"),
        "Calculus" to listOf("derivative", "integral", "limit", "dx", "dy", "function", "chain rule", "quotient", "tangent", "curve", "theorem", "series"),
        "Organic Chem" to listOf("carbon", "hydrogen", "bond", "reaction", "alkane", "alkene", "aromatic", "resonance", "synthesis", "mechanism", "isomer"),
        "World History" to listOf("treaty", "war", "revolution", "empire", "century", "dynasty", "monarchy", "republic", "declaration", "constitution", "allies"),
        "Physics" to listOf("velocity", "acceleration", "force", "newton", "gravity", "friction", "kinetic", "potential", "energy", "momentum", "torque", "wave")
    )

    override suspend fun extractText(imageUri: String, rawContentHint: String?): OcrResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()

        if (!rawContentHint.isNullOrBlank()) {
            val text = rawContentHint.trim()
            val keywords = extractKeywords(text)
            return@withContext OcrResult(
                fullText = text,
                keywords = keywords,
                detectedSubjectHint = detectSubjectHint(keywords),
                confidence = 0.95f,
                processingTimeMs = System.currentTimeMillis() - startTime
            )
        }

        var recognizedText = ""
        var confidence = 0.0f

        try {
            val inputImage = createInputImage(imageUri)
            if (inputImage != null) {
                val visionText = recognizer.process(inputImage).awaitTask()
                recognizedText = visionText.text.trim()
                confidence = if (recognizedText.isNotEmpty()) 0.92f else 0.0f
            }
        } catch (_: Exception) {
            recognizedText = ""
            confidence = 0.0f
        }

        val keywords = extractKeywords(recognizedText)
        val detectedSubject = detectSubjectHint(keywords)
        val elapsed = System.currentTimeMillis() - startTime

        OcrResult(
            fullText = recognizedText,
            keywords = keywords,
            detectedSubjectHint = detectedSubject,
            confidence = confidence,
            processingTimeMs = elapsed
        )
    }

    private fun createInputImage(imageUriStr: String): InputImage? {
        val ctx = context ?: return null
        return try {
            if (imageUriStr.startsWith("content://") || imageUriStr.startsWith("file://")) {
                val uri = Uri.parse(imageUriStr)
                InputImage.fromFilePath(ctx, uri)
            } else {
                val file = File(imageUriStr)
                if (file.exists()) {
                    InputImage.fromFilePath(ctx, Uri.fromFile(file))
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    override fun extractKeywords(text: String): List<String> {
        val tokens = text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 2 && it !in commonStopwords }

        return tokens.distinct()
    }

    private fun detectSubjectHint(keywords: List<String>): String? {
        var bestSubject: String? = null
        var maxMatches = 0

        for ((subject, subjectWords) in academicSubjectKeywords) {
            val matchCount = keywords.count { kw ->
                subjectWords.any { sw -> sw.contains(kw) || kw.contains(sw) }
            }
            if (matchCount > maxMatches) {
                maxMatches = matchCount
                bestSubject = subject
            }
        }

        return if (maxMatches > 0) bestSubject else null
    }

    private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result ->
            if (cont.isActive) cont.resume(result)
        }
        addOnFailureListener { exception ->
            if (cont.isActive) cont.resumeWithException(exception)
        }
    }
}
