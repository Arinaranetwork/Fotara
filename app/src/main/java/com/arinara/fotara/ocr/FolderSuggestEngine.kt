// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ocr

import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.data.model.Subfolder

data class FolderSuggestion(
    val folderId: Long,
    val folderName: String,
    val suggestedSubfolderId: Long? = null,
    val suggestedSubfolderName: String? = null,
    val confidence: Float = 0.85f,
    val matchedKeywords: List<String> = emptyList()
)

interface FolderSuggestEngine {
    fun suggestFolder(
        ocrResult: OcrResult,
        folders: List<Folder>,
        subfoldersByFolder: Map<Long, List<Subfolder>> = emptyMap()
    ): FolderSuggestion?
}

class DefaultFolderSuggestEngine : FolderSuggestEngine {

    override fun suggestFolder(
        ocrResult: OcrResult,
        folders: List<Folder>,
        subfoldersByFolder: Map<Long, List<Subfolder>>
    ): FolderSuggestion? {
        if (folders.isEmpty()) return null

        val hint = ocrResult.detectedSubjectHint
        if (hint != null) {
            val directFolderMatch = folders.firstOrNull {
                it.name.contains(hint, ignoreCase = true) || hint.contains(it.name, ignoreCase = true)
            }
            if (directFolderMatch != null) {
                val subfolders = subfoldersByFolder[directFolderMatch.id] ?: emptyList()
                val matchedSub = findBestSubfolder(ocrResult.keywords, subfolders)
                return FolderSuggestion(
                    folderId = directFolderMatch.id,
                    folderName = directFolderMatch.name,
                    suggestedSubfolderId = matchedSub?.id,
                    suggestedSubfolderName = matchedSub?.name,
                    confidence = 0.92f,
                    matchedKeywords = ocrResult.keywords.take(4)
                )
            }
        }

        // Score all folders against OCR keywords
        var bestFolder: Folder? = null
        var highestScore = 0
        val matchedTokens = mutableListOf<String>()

        for (folder in folders) {
            val folderTokens = folder.name.lowercase().split(" ")
            var score = 0
            for (keyword in ocrResult.keywords) {
                if (folderTokens.any { it.contains(keyword) || keyword.contains(it) }) {
                    score += 2
                    matchedTokens.add(keyword)
                }
            }
            if (score > highestScore) {
                highestScore = score
                bestFolder = folder
            }
        }

        if (bestFolder != null && highestScore >= 2) {
            val subfolders = subfoldersByFolder[bestFolder.id] ?: emptyList()
            val matchedSub = findBestSubfolder(ocrResult.keywords, subfolders)
            return FolderSuggestion(
                folderId = bestFolder.id,
                folderName = bestFolder.name,
                suggestedSubfolderId = matchedSub?.id,
                suggestedSubfolderName = matchedSub?.name,
                confidence = 0.78f,
                matchedKeywords = matchedTokens.distinct().take(4)
            )
        }

        // Default to the first folder if present
        val fallback = folders.firstOrNull() ?: return null
        return FolderSuggestion(
            folderId = fallback.id,
            folderName = fallback.name,
            confidence = 0.50f,
            matchedKeywords = emptyList()
        )
    }

    private fun findBestSubfolder(keywords: List<String>, subfolders: List<Subfolder>): Subfolder? {
        if (subfolders.isEmpty()) return null
        for (sub in subfolders) {
            val subName = sub.name.lowercase()
            if (keywords.any { subName.contains(it) || it.contains(subName) }) {
                return sub
            }
        }
        return subfolders.firstOrNull()
    }
}
