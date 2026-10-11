// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.legal

import android.content.Context
import java.io.InputStream
import java.util.regex.Pattern

data class ParsedLegalDocument(
    val title: String,
    val version: Int,
    val effectiveDate: String,
    val body: String,
    val rawText: String
)

object LegalDocumentParser {
    private val VERSION_LINE_PATTERN = Pattern.compile("^Version:\\s*(\\d+)\\s*\\|\\s*Effective:\\s*(\\d{4}-\\d{2}-\\d{2})\\s*$")

    fun parse(rawText: String, expectedTitle: String? = null): ParsedLegalDocument {
        val lines = rawText.lines()
        if (lines.size < 2) {
            throw IllegalStateException("Legal document must contain at least a title and a version metadata line.")
        }

        val title = lines[0].trim()
        if (title.isBlank()) {
            throw IllegalStateException("Legal document title is missing or blank.")
        }
        if (expectedTitle != null && !title.equals(expectedTitle, ignoreCase = true)) {
            throw IllegalStateException("Legal document title mismatch: expected '$expectedTitle', found '$title'.")
        }

        val versionLine = lines[1].trim()
        val matcher = VERSION_LINE_PATTERN.matcher(versionLine)
        if (!matcher.matches()) {
            throw IllegalStateException("Malformed or missing version metadata line: '$versionLine'. Expected 'Version: N | Effective: YYYY-MM-DD'.")
        }

        val version = matcher.group(1)?.toIntOrNull()
            ?: throw IllegalStateException("Invalid version integer in line: '$versionLine'.")
        val effectiveDate = matcher.group(2)
            ?: throw IllegalStateException("Invalid effective date format in line: '$versionLine'.")

        // The displayed body strips line 1 (title) and line 2 (version metadata), along with leading empty lines
        val bodyLines = lines.drop(2)
        val body = bodyLines.joinToString("\n").trimStart()

        return ParsedLegalDocument(
            title = title,
            version = version,
            effectiveDate = effectiveDate,
            body = body,
            rawText = rawText
        )
    }
}

class LegalDocumentLoader(private val context: Context) {

    companion object {
        const val BUNDLED_PRIVACY_VERSION = 1
        const val BUNDLED_TERMS_VERSION = 1
    }

    fun loadPrivacyPolicy(): ParsedLegalDocument {
        val rawText = readAssetContent("PRIVACY.md")
        return LegalDocumentParser.parse(rawText, "Privacy Policy")
    }

    fun loadTermsOfService(): ParsedLegalDocument {
        val rawText = readAssetContent("TERMS.md")
        return LegalDocumentParser.parse(rawText, "Terms of Service")
    }

    private fun readAssetContent(filename: String): String {
        val assetManager = context.assets
        val stream: InputStream = try {
            assetManager.open(filename)
        } catch (_: Exception) {
            try {
                assetManager.open("legal/$filename")
            } catch (e: Exception) {
                throw IllegalStateException("Unable to load legal asset '$filename' from application assets.", e)
            }
        }
        return stream.bufferedReader().use { it.readText() }
    }
}
