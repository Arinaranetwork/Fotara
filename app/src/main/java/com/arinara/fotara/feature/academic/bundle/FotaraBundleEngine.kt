// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.academic.bundle

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Metadata descriptor stored at `manifest.json` in the root of each `.fotara` bundle.
 */
data class CourseworkManifest(
    val formatVersion: Int = 1,
    val appIdentifier: String = "com.arinara.fotara",
    val courseTitle: String,
    val courseCode: String = "",
    val academicTerm: String = "",
    val creationTimestamp: Long = System.currentTimeMillis(),
    val itemCount: Int,
    val checksumSha256: String = "",
    val entries: List<CourseworkBundleEntry> = emptyList()
)

/**
 * Individual asset or study note entry tracked within the coursework manifest.
 */
data class CourseworkBundleEntry(
    val relativePath: String,
    val entryType: String,
    val title: String,
    val sha256: String,
    val byteSize: Long
)

/**
 * Staged item payload ready for packaging into a `.fotara` archive.
 */
data class BundleExportItem(
    val relativePath: String,
    val entryType: String,
    val title: String,
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as BundleExportItem
        return relativePath == other.relativePath && data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        var result = relativePath.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}

/**
 * Extracted item unpacked from a `.fotara` archive.
 */
data class ExtractedBundleItem(
    val relativePath: String,
    val entryType: String,
    val title: String,
    val data: ByteArray,
    val isChecksumValid: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ExtractedBundleItem
        return relativePath == other.relativePath && data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        var result = relativePath.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}

/**
 * Result returned upon inspecting or unpacking a `.fotara` coursework archive.
 */
data class BundleImportResult(
    val manifest: CourseworkManifest?,
    val extractedItems: List<ExtractedBundleItem>,
    val isSuccess: Boolean,
    val validationError: String? = null
)

/**
 * Pure ZIP packaging engine generating authenticated `.fotara` coursework archives.
 */
class FotaraBundleExporter {

    /**
     * Packages a collection of coursework items into an output stream with manifest.json metadata.
     */
    fun exportBundle(
        outputStream: OutputStream,
        courseTitle: String,
        courseCode: String = "",
        academicTerm: String = "",
        items: List<BundleExportItem>
    ): CourseworkManifest {
        val zipOut = ZipOutputStream(outputStream)

        val entries = mutableListOf<CourseworkBundleEntry>()
        val compositeDigest = MessageDigest.getInstance("SHA-256")

        // Write coursework data files
        for (item in items) {
            val itemDigest = computeSha256(item.data)
            compositeDigest.update(item.data)

            val entry = CourseworkBundleEntry(
                relativePath = item.relativePath,
                entryType = item.entryType,
                title = item.title,
                sha256 = itemDigest,
                byteSize = item.data.size.toLong()
            )
            entries.add(entry)

            val zipEntry = ZipEntry(item.relativePath)
            zipOut.putNextEntry(zipEntry)
            zipOut.write(item.data)
            zipOut.closeEntry()
        }

        val compositeChecksum = bytesToHex(compositeDigest.digest())

        val manifest = CourseworkManifest(
            formatVersion = 1,
            appIdentifier = "com.arinara.fotara",
            courseTitle = courseTitle,
            courseCode = courseCode,
            academicTerm = academicTerm,
            creationTimestamp = System.currentTimeMillis(),
            itemCount = items.size,
            checksumSha256 = compositeChecksum,
            entries = entries
        )

        // Write manifest.json
        val manifestJsonBytes = serializeManifest(manifest).toByteArray(Charsets.UTF_8)
        val manifestZipEntry = ZipEntry("manifest.json")
        zipOut.putNextEntry(manifestZipEntry)
        zipOut.write(manifestJsonBytes)
        zipOut.closeEntry()

        zipOut.finish()
        zipOut.flush()

        return manifest
    }

    /**
     * Packages a collection of coursework items into a `.fotara` file on disk.
     */
    fun exportBundleToFile(
        destinationFile: File,
        courseTitle: String,
        courseCode: String = "",
        academicTerm: String = "",
        items: List<BundleExportItem>
    ): CourseworkManifest {
        destinationFile.parentFile?.mkdirs()
        FileOutputStream(destinationFile).use { fos ->
            return exportBundle(fos, courseTitle, courseCode, academicTerm, items)
        }
    }

    private fun computeSha256(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return bytesToHex(digest.digest(data))
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    private fun serializeManifest(m: CourseworkManifest): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"formatVersion\": ${m.formatVersion},\n")
        sb.append("  \"appIdentifier\": \"${escapeJson(m.appIdentifier)}\",\n")
        sb.append("  \"courseTitle\": \"${escapeJson(m.courseTitle)}\",\n")
        sb.append("  \"courseCode\": \"${escapeJson(m.courseCode)}\",\n")
        sb.append("  \"academicTerm\": \"${escapeJson(m.academicTerm)}\",\n")
        sb.append("  \"creationTimestamp\": ${m.creationTimestamp},\n")
        sb.append("  \"itemCount\": ${m.itemCount},\n")
        sb.append("  \"checksumSha256\": \"${m.checksumSha256}\",\n")
        sb.append("  \"entries\": [\n")
        for (i in m.entries.indices) {
            val e = m.entries[i]
            sb.append("    {\n")
            sb.append("      \"relativePath\": \"${escapeJson(e.relativePath)}\",\n")
            sb.append("      \"entryType\": \"${escapeJson(e.entryType)}\",\n")
            sb.append("      \"title\": \"${escapeJson(e.title)}\",\n")
            sb.append("      \"sha256\": \"${e.sha256}\",\n")
            sb.append("      \"byteSize\": ${e.byteSize}\n")
            sb.append("    }${if (i < m.entries.size - 1) "," else ""}\n")
        }
        sb.append("  ]\n")
        sb.append("}\n")
        return sb.toString()
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}

/**
 * Pure ZIP unpacking engine parsing and verifying `.fotara` coursework archives.
 */
class FotaraBundleImporter {

    /**
     * Unpacks and verifies a `.fotara` archive from an input stream in-memory.
     */
    fun importBundle(inputStream: InputStream): BundleImportResult {
        val rawZipBytes = inputStream.readBytes()
        val zipIn = ZipInputStream(ByteArrayInputStream(rawZipBytes))

        var manifestJsonContent: String? = null
        val filePayloads = mutableMapOf<String, ByteArray>()

        try {
            var entry = zipIn.nextEntry
            while (entry != null) {
                // Zip Slip Path Traversal Check
                val normalizedName = entry.name.replace('\\', '/')
                if (normalizedName.contains("..") || normalizedName.startsWith("/")) {
                    return BundleImportResult(
                        manifest = null,
                        extractedItems = emptyList(),
                        isSuccess = false,
                        validationError = "Path traversal violation in bundle entry: ${entry.name}"
                    )
                }

                if (!entry.isDirectory) {
                    val entryBytes = zipIn.readBytes()
                    if (normalizedName == "manifest.json") {
                        manifestJsonContent = entryBytes.toString(Charsets.UTF_8)
                    } else {
                        filePayloads[normalizedName] = entryBytes
                    }
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        } catch (e: Exception) {
            return BundleImportResult(
                manifest = null,
                extractedItems = emptyList<ExtractedBundleItem>(),
                isSuccess = false,
                validationError = "Corrupted archive stream: ${e.message}"
            )
        }

        if (manifestJsonContent == null) {
            return BundleImportResult(
                manifest = null,
                extractedItems = emptyList<ExtractedBundleItem>(),
                isSuccess = false,
                validationError = "Missing manifest.json in .fotara bundle"
            )
        }

        val manifest = parseManifest(manifestJsonContent)
            ?: return BundleImportResult(
                manifest = null,
                extractedItems = emptyList<ExtractedBundleItem>(),
                isSuccess = false,
                validationError = "Unable to parse coursework manifest.json"
            )

        val extracted = mutableListOf<ExtractedBundleItem>()
        val compositeDigest = MessageDigest.getInstance("SHA-256")

        for (manifestEntry in manifest.entries) {
            val bytes = filePayloads[manifestEntry.relativePath]
            if (bytes == null) {
                return BundleImportResult(
                    manifest = manifest,
                    extractedItems = emptyList(),
                    isSuccess = false,
                    validationError = "Missing expected payload file: ${manifestEntry.relativePath}"
                )
            }

            compositeDigest.update(bytes)
            val computedSha = computeSha256(bytes)
            val isChecksumMatch = computedSha.equals(manifestEntry.sha256, ignoreCase = true)

            if (!isChecksumMatch) {
                return BundleImportResult(
                    manifest = manifest,
                    extractedItems = emptyList(),
                    isSuccess = false,
                    validationError = "Checksum mismatch on file: ${manifestEntry.relativePath}"
                )
            }

            extracted.add(
                ExtractedBundleItem(
                    relativePath = manifestEntry.relativePath,
                    entryType = manifestEntry.entryType,
                    title = manifestEntry.title,
                    data = bytes,
                    isChecksumValid = true
                )
            )
        }

        val finalCompositeChecksum = bytesToHex(compositeDigest.digest())
        if (manifest.checksumSha256.isNotBlank() &&
            !finalCompositeChecksum.equals(manifest.checksumSha256, ignoreCase = true)
        ) {
            return BundleImportResult(
                manifest = manifest,
                extractedItems = emptyList(),
                isSuccess = false,
                validationError = "Composite archive checksum mismatch"
            )
        }

        return BundleImportResult(
            manifest = manifest,
            extractedItems = extracted,
            isSuccess = true
        )
    }

    /**
     * Unpacks a `.fotara` archive safely onto a destination disk directory.
     */
    fun extractBundleToDirectory(inputStream: InputStream, destinationDir: File): BundleImportResult {
        val result = importBundle(inputStream)
        if (!result.isSuccess || result.manifest == null) {
            return result
        }

        destinationDir.mkdirs()
        val destCanonicalPath = destinationDir.canonicalPath

        for (item in result.extractedItems) {
            val targetFile = File(destinationDir, item.relativePath)
            val targetCanonicalPath = targetFile.canonicalPath

            // Enforce boundary containment
            if (!targetCanonicalPath.startsWith(destCanonicalPath + File.separator) &&
                targetCanonicalPath != destCanonicalPath
            ) {
                return BundleImportResult(
                    manifest = result.manifest,
                    extractedItems = emptyList(),
                    isSuccess = false,
                    validationError = "Zip Slip security exception: ${item.relativePath}"
                )
            }

            targetFile.parentFile?.mkdirs()
            FileOutputStream(targetFile).use { fos ->
                fos.write(item.data)
            }
        }

        return result
    }

    private fun computeSha256(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return bytesToHex(digest.digest(data))
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    /**
     * Lightweight pure-Kotlin JSON parser for CourseworkManifest.
     */
    internal fun parseManifest(json: String): CourseworkManifest? {
        return try {
            val formatVersion = findIntField(json, "formatVersion") ?: 1
            val appIdentifier = findStringField(json, "appIdentifier") ?: "com.arinara.fotara"
            val courseTitle = findStringField(json, "courseTitle") ?: "Untitled Course"
            val courseCode = findStringField(json, "courseCode") ?: ""
            val academicTerm = findStringField(json, "academicTerm") ?: ""
            val creationTimestamp = findLongField(json, "creationTimestamp") ?: 0L
            val itemCount = findIntField(json, "itemCount") ?: 0
            val checksumSha256 = findStringField(json, "checksumSha256") ?: ""

            val entries = parseEntries(json)

            CourseworkManifest(
                formatVersion = formatVersion,
                appIdentifier = appIdentifier,
                courseTitle = courseTitle,
                courseCode = courseCode,
                academicTerm = academicTerm,
                creationTimestamp = creationTimestamp,
                itemCount = itemCount,
                checksumSha256 = checksumSha256,
                entries = entries
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun findStringField(json: String, key: String): String? {
        val pattern = Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"")
        return pattern.find(json)?.groupValues?.get(1)
    }

    private fun findIntField(json: String, key: String): Int? {
        val pattern = Regex("\"$key\"\\s*:\\s*(\\d+)")
        return pattern.find(json)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun findLongField(json: String, key: String): Long? {
        val pattern = Regex("\"$key\"\\s*:\\s*(\\d+)")
        return pattern.find(json)?.groupValues?.get(1)?.toLongOrNull()
    }

    private fun parseEntries(json: String): List<CourseworkBundleEntry> {
        val entries = mutableListOf<CourseworkBundleEntry>()
        val entriesIndex = json.indexOf("\"entries\"")
        if (entriesIndex == -1) return emptyList()

        val arrayStart = json.indexOf('[', entriesIndex)
        val arrayEnd = json.lastIndexOf(']')
        if (arrayStart == -1 || arrayEnd == -1 || arrayEnd <= arrayStart) return emptyList()

        val arrayContent = json.substring(arrayStart + 1, arrayEnd)
        val objectRegex = Regex("\\{[^\\{\\}]+\\}")
        for (match in objectRegex.findAll(arrayContent)) {
            val obj = match.value
            val relativePath = findStringField(obj, "relativePath") ?: continue
            val entryType = findStringField(obj, "entryType") ?: "FILE"
            val title = findStringField(obj, "title") ?: ""
            val sha256 = findStringField(obj, "sha256") ?: ""
            val byteSize = findLongField(obj, "byteSize") ?: 0L

            entries.add(
                CourseworkBundleEntry(
                    relativePath = relativePath,
                    entryType = entryType,
                    title = title,
                    sha256 = sha256,
                    byteSize = byteSize
                )
            )
        }
        return entries
    }
}
