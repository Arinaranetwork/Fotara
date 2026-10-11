// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.util

import com.arinara.fotara.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileNamePresetHelper {

    const val DEFAULT_PRESET = "{folder}_{date}"
    const val MAX_PRESET_LENGTH = 80
    const val MAX_FILE_NAME_LENGTH = 80

    val ALLOWED_TOKENS = setOf("folder", "date", "time", "count")

    private val TOKEN_REGEX = Regex("\\{([^}]+)\\}")
    private val ILLEGAL_CHARS_REGEX = Regex("[\\\\/:*?\"<>|\\x00-\\x1F\\x7F]")
    private val REPEATED_WHITESPACE_REGEX = Regex("\\s+")
    private val TRAILING_DOTS_SPACES_REGEX = Regex("[. ]+$")
    private val ONLY_DOTS_REGEX = Regex("^\\.*$")

    data class ValidationResult(
        val isValid: Boolean,
        val errorMessageRes: Int? = null,
        val errorArg: String? = null
    )

    fun validatePreset(presetText: String): ValidationResult {
        val trimmed = presetText.trim()
        if (trimmed.isEmpty()) {
            return ValidationResult(
                isValid = false,
                errorMessageRes = R.string.setting_combine_file_name_err_empty
            )
        }

        if (trimmed.length > MAX_PRESET_LENGTH) {
            return ValidationResult(
                isValid = false,
                errorMessageRes = R.string.setting_combine_file_name_err_too_long
            )
        }

        val matches = TOKEN_REGEX.findAll(trimmed).toList()
        val unknownTokens = mutableListOf<String>()
        for (match in matches) {
            val tokenName = match.groupValues[1]
            if (tokenName !in ALLOWED_TOKENS) {
                unknownTokens.add("{$tokenName}")
            }
        }

        if (unknownTokens.isNotEmpty()) {
            return ValidationResult(
                isValid = false,
                errorMessageRes = R.string.setting_combine_file_name_err_unknown_tokens,
                errorArg = unknownTokens.joinToString(", ")
            )
        }

        val sampleResolved = resolvePreset(
            presetTemplate = trimmed,
            folderName = "Folder",
            itemCount = 1
        )
        if (sampleResolved.isBlank() || ONLY_DOTS_REGEX.matches(sampleResolved)) {
            return ValidationResult(
                isValid = false,
                errorMessageRes = R.string.setting_combine_file_name_err_empty
            )
        }

        return ValidationResult(isValid = true)
    }

    fun sanitizeFileName(rawName: String, fallback: String = "Fotara_Combined"): String {
        var clean = rawName.trim()
        clean = ILLEGAL_CHARS_REGEX.replace(clean, "")
        clean = REPEATED_WHITESPACE_REGEX.replace(clean, " ")
        clean = TRAILING_DOTS_SPACES_REGEX.replace(clean, "")

        if (clean.length > MAX_FILE_NAME_LENGTH) {
            clean = clean.substring(0, MAX_FILE_NAME_LENGTH)
            clean = TRAILING_DOTS_SPACES_REGEX.replace(clean, "")
        }

        clean = clean.trim()

        if (clean.isBlank() || ONLY_DOTS_REGEX.matches(clean)) {
            return fallback
        }
        return clean
    }

    fun resolvePreset(
        presetTemplate: String,
        folderName: String,
        itemCount: Int,
        dateMillis: Long = System.currentTimeMillis()
    ): String {
        val template = if (presetTemplate.trim().isEmpty()) DEFAULT_PRESET else presetTemplate.trim()
        val dateObj = Date(dateMillis)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val timeFormat = SimpleDateFormat("HH-mm", Locale.US)

        val dateStr = dateFormat.format(dateObj)
        val timeStr = timeFormat.format(dateObj)
        val countStr = itemCount.coerceAtLeast(0).toString()

        val cleanFolderName = sanitizeFileName(folderName, fallback = "Coursework")

        val replaced = template
            .replace("{folder}", cleanFolderName)
            .replace("{date}", dateStr)
            .replace("{time}", timeStr)
            .replace("{count}", countStr)

        return sanitizeFileName(replaced, fallback = "Fotara_Combined")
    }

    fun buildFinalFileName(
        customName: String?,
        presetTemplate: String,
        folderName: String,
        itemCount: Int,
        extension: String,
        dateMillis: Long = System.currentTimeMillis()
    ): String {
        val ext = if (extension.startsWith(".")) extension.lowercase(Locale.US) else ".${extension.lowercase(Locale.US)}"

        var candidateBase: String? = null
        if (!customName.isNullOrBlank()) {
            var raw = customName.trim()
            if (raw.endsWith(ext, ignoreCase = true)) {
                raw = raw.substring(0, raw.length - ext.length)
            }
            val sanitized = sanitizeFileName(raw, fallback = "")
            if (sanitized.isNotBlank()) {
                candidateBase = sanitized
            }
        }

        val baseName = candidateBase ?: resolvePreset(
            presetTemplate = presetTemplate,
            folderName = folderName,
            itemCount = itemCount,
            dateMillis = dateMillis
        )

        return "$baseName$ext"
    }

    fun ensureUniqueFile(directory: File, fileNameWithExt: String): File {
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val target = File(directory, fileNameWithExt)
        if (!target.exists()) {
            return target
        }

        val dotIndex = fileNameWithExt.lastIndexOf('.')
        val (base, ext) = if (dotIndex > 0) {
            Pair(fileNameWithExt.substring(0, dotIndex), fileNameWithExt.substring(dotIndex))
        } else {
            Pair(fileNameWithExt, "")
        }

        var counter = 1
        while (counter < 1000) {
            val candidate = File(directory, "$base ($counter)$ext")
            if (!candidate.exists()) {
                return candidate
            }
            counter++
        }

        return File(directory, "${base}_${System.currentTimeMillis()}$ext")
    }
}
