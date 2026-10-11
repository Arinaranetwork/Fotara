// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.schedule.parser

import android.content.Context
import android.net.Uri
import android.util.Log
import com.arinara.fotara.feature.schedule.model.ClassSchedule
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Raw tabular matrix extracted from an Excel or Word timetable.
 */
data class RawTableData(
    val headers: List<String>,
    val rows: List<List<String>>
)

/**
 * Mapped column indexes pointing to academic timetable roles.
 */
data class ScheduleColumnMapping(
    val dayColumnIndex: Int = -1,
    val timeColumnIndex: Int = -1,
    val subjectColumnIndex: Int = -1,
    val roomColumnIndex: Int = -1,
    val instructorColumnIndex: Int = -1
) {
    val isValid: Boolean
        get() = dayColumnIndex >= 0 && timeColumnIndex >= 0 && subjectColumnIndex >= 0
}

/**
 * Zero-dependency, streaming parser for .xlsx and .docx academic timetable tables.
 * Uses standard platform XmlPullParser with zero external libraries to keep APK < 25MB.
 */
class XlsxDocxScheduleParser {

    /**
     * Parses an input stream (Excel .xlsx or Word .docx) into raw table rows.
     */
    fun parseStream(inputStream: InputStream, filenameHint: String = ""): RawTableData {
        val lower = filenameHint.lowercase(Locale.ROOT)
        return when {
            lower.endsWith(".docx") || lower.endsWith(".doc") -> parseDocx(inputStream)
            else -> parseXlsx(inputStream)
        }
    }

    /**
     * Parses a Uri from content resolver.
     */
    fun parseUri(context: Context, uri: Uri, displayName: String = ""): RawTableData {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            return parseStream(stream, displayName.ifBlank { uri.lastPathSegment ?: "" })
        } ?: throw IllegalArgumentException("Cannot open stream for URI: $uri")
    }

    // =========================================================================
    // EXCEL (.xlsx) STREAMING PARSER
    // =========================================================================

    fun parseXlsx(inputStream: InputStream): RawTableData {
        val sharedStrings = mutableListOf<String>()
        val rawGrid = mutableListOf<List<String>>()

        // Step 1: Pass 1 to extract sharedStrings.xml
        val zipBytes = inputStream.readBytes()

        // Read shared strings
        ZipInputStream(zipBytes.inputStream()).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val name = entry.name.lowercase(Locale.ROOT)
                if (name.contains("sharedstrings.xml")) {
                    sharedStrings.addAll(parseSharedStringsXml(zis))
                    break
                }
                entry = zis.nextEntry
            }
        }

        // Step 2: Pass 2 to extract worksheet cells (prefer sheet1.xml)
        ZipInputStream(zipBytes.inputStream()).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val name = entry.name.lowercase(Locale.ROOT)
                if (name.contains("worksheets/sheet1.xml") || (name.contains("sheet") && name.endsWith(".xml"))) {
                    rawGrid.addAll(parseSheetXml(zis, sharedStrings))
                    if (rawGrid.isNotEmpty()) break
                }
                entry = zis.nextEntry
            }
        }

        if (rawGrid.isEmpty()) {
            return RawTableData(emptyList(), emptyList())
        }

        val headerRow = rawGrid.first()
        val dataRows = if (rawGrid.size > 1) rawGrid.subList(1, rawGrid.size) else emptyList()
        return RawTableData(headers = headerRow, rows = dataRows)
    }

    private fun parseSharedStringsXml(inputStream: InputStream): List<String> {
        val strings = mutableListOf<String>()
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(inputStream, "UTF-8")

        var eventType = parser.eventType
        val currentText = StringBuilder()
        var insideSi = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "si" -> {
                            insideSi = true
                            currentText.setLength(0)
                        }
                        "t" -> {
                            // Text node follows
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideSi) {
                        currentText.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "si") {
                        strings.add(currentText.toString().trim())
                        insideSi = false
                    }
                }
            }
            eventType = parser.next()
        }
        return strings
    }

    private fun parseSheetXml(inputStream: InputStream, sharedStrings: List<String>): List<List<String>> {
        val grid = mutableListOf<List<String>>()
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(inputStream, "UTF-8")

        var eventType = parser.eventType
        val currentRow = mutableMapOf<Int, String>()
        var currentCellCol = -1
        var cellType: String? = null
        val cellText = StringBuilder()
        var insideCell = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "row" -> {
                            currentRow.clear()
                        }
                        "c" -> {
                            insideCell = true
                            cellText.setLength(0)
                            val cellRef = parser.getAttributeValue(null, "r") ?: ""
                            currentCellCol = colRefToColIndex(cellRef)
                            cellType = parser.getAttributeValue(null, "t")
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideCell) {
                        cellText.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "c" -> {
                            insideCell = false
                            if (currentCellCol >= 0) {
                                val rawVal = cellText.toString().trim()
                                val resolved = if (cellType == "s") {
                                    val index = rawVal.toIntOrNull()
                                    if (index != null && index in sharedStrings.indices) {
                                        sharedStrings[index]
                                    } else rawVal
                                } else {
                                    rawVal
                                }
                                currentRow[currentCellCol] = resolved
                            }
                        }
                        "row" -> {
                            if (currentRow.isNotEmpty()) {
                                val maxCol = currentRow.keys.maxOrNull() ?: 0
                                val rowList = ArrayList<String>(maxCol + 1)
                                for (c in 0..maxCol) {
                                    rowList.add(currentRow[c] ?: "")
                                }
                                if (rowList.any { it.isNotBlank() }) {
                                    grid.add(rowList)
                                }
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return grid
    }

    private fun colRefToColIndex(cellRef: String): Int {
        var col = 0
        var foundCol = false
        for (ch in cellRef) {
            if (ch in 'A'..'Z') {
                col = col * 26 + (ch - 'A' + 1)
                foundCol = true
            } else if (ch in 'a'..'z') {
                col = col * 26 + (ch - 'a' + 1)
                foundCol = true
            } else {
                break
            }
        }
        return if (foundCol) col - 1 else 0
    }

    // =========================================================================
    // WORD (.docx) STREAMING TABLE PARSER
    // =========================================================================

    fun parseDocx(inputStream: InputStream): RawTableData {
        val grid = mutableListOf<List<String>>()
        ZipInputStream(inputStream).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (entry.name.lowercase(Locale.ROOT) == "word/document.xml") {
                    grid.addAll(parseDocxDocumentXml(zis))
                    break
                }
                entry = zis.nextEntry
            }
        }

        if (grid.isEmpty()) {
            return RawTableData(emptyList(), emptyList())
        }

        val headerRow = grid.first()
        val dataRows = if (grid.size > 1) grid.subList(1, grid.size) else emptyList()
        return RawTableData(headers = headerRow, rows = dataRows)
    }

    private fun parseDocxDocumentXml(inputStream: InputStream): List<List<String>> {
        val grid = mutableListOf<List<String>>()
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(inputStream, "UTF-8")

        var eventType = parser.eventType
        var insideTable = false
        var insideRow = false
        var insideCell = false
        val currentRow = mutableListOf<String>()
        val currentCellText = StringBuilder()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "tbl" -> insideTable = true
                        "tr" -> {
                            if (insideTable) {
                                insideRow = true
                                currentRow.clear()
                            }
                        }
                        "tc" -> {
                            if (insideRow) {
                                insideCell = true
                                currentCellText.setLength(0)
                            }
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideCell) {
                        currentCellText.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "tc" -> {
                            if (insideCell) {
                                currentRow.add(currentCellText.toString().trim())
                                insideCell = false
                            }
                        }
                        "tr" -> {
                            if (insideRow) {
                                if (currentRow.any { it.isNotBlank() }) {
                                    grid.add(ArrayList(currentRow))
                                }
                                insideRow = false
                            }
                        }
                        "tbl" -> {
                            insideTable = false
                            if (grid.isNotEmpty()) {
                                return grid
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return grid
    }

    // =========================================================================
    // SMART COLUMN MAPPING HEURISTICS
    // =========================================================================

    /**
     * Auto-detects columns for Day, Time, Subject, Room, and Lecturer.
     */
    fun autoDetectColumnMapping(tableData: RawTableData): ScheduleColumnMapping = autoDetectMapping(tableData)

    fun autoDetectMapping(tableData: RawTableData): ScheduleColumnMapping {
        val allCols = tableData.headers.size.coerceAtLeast(
            tableData.rows.maxOfOrNull { it.size } ?: 0
        )
        if (allCols == 0) return ScheduleColumnMapping()

        var bestDayCol = -1
        var bestTimeCol = -1
        var bestSubjectCol = -1
        var bestRoomCol = -1
        var bestInstructorCol = -1

        for (colIdx in 0 until allCols) {
            val headerText = tableData.headers.getOrNull(colIdx)?.lowercase(Locale.ROOT) ?: ""
            val sampleCells = tableData.rows.take(5).mapNotNull { it.getOrNull(colIdx) }

            // Day detection
            if (bestDayCol == -1) {
                if (headerText.contains("hari") || headerText.contains("day") ||
                    sampleCells.any { ClassSchedule.parseDayOfWeek(it) != null }) {
                    bestDayCol = colIdx
                    continue
                }
            }

            // Time detection
            if (bestTimeCol == -1) {
                if (headerText.contains("jam") || headerText.contains("waktu") ||
                    headerText.contains("time") || headerText.contains("pukul") ||
                    sampleCells.any { it.contains(":") || it.contains(".") && it.any { c -> c.isDigit() } }) {
                    bestTimeCol = colIdx
                    continue
                }
            }

            // Subject detection
            if (bestSubjectCol == -1) {
                if (headerText.contains("mata kuliah") || headerText.contains("matkul") ||
                    headerText.contains("subject") || headerText.contains("kuliah") ||
                    headerText.contains("course") || headerText.contains("nama") ||
                    headerText.contains("pelajaran")) {
                    bestSubjectCol = colIdx
                    continue
                }
            }

            // Room detection
            if (bestRoomCol == -1) {
                if (headerText.contains("ruang") || headerText.contains("room") ||
                    headerText.contains("kelas") || headerText.contains("lokasi") ||
                    headerText.contains("gedung") || headerText.contains("lab")) {
                    bestRoomCol = colIdx
                    continue
                }
            }

            // Instructor detection
            if (bestInstructorCol == -1) {
                if (headerText.contains("dosen") || headerText.contains("pengajar") ||
                    headerText.contains("lecturer") || headerText.contains("guru") ||
                    headerText.contains("instructor") || headerText.contains("prof")) {
                    bestInstructorCol = colIdx
                    continue
                }
            }
        }

        // Fallback heuristics if header didn't catch subject or day
        if (bestSubjectCol == -1) {
            for (colIdx in 0 until allCols) {
                if (colIdx != bestDayCol && colIdx != bestTimeCol && colIdx != bestRoomCol && colIdx != bestInstructorCol) {
                    bestSubjectCol = colIdx
                    break
                }
            }
        }

        return ScheduleColumnMapping(
            dayColumnIndex = bestDayCol,
            timeColumnIndex = bestTimeCol,
            subjectColumnIndex = bestSubjectCol,
            roomColumnIndex = bestRoomCol,
            instructorColumnIndex = bestInstructorCol
        )
    }

    /**
     * Converts raw table rows into domain [ClassSchedule] entries using the confirmed mapping.
     * Intelligently propagates day values down merged cells.
     */
    fun mapToSchedules(
        tableData: RawTableData,
        mapping: ScheduleColumnMapping,
        colorHex: String = "#2563EB"
    ): List<ClassSchedule> {
        if (!mapping.isValid) return emptyList()

        val results = mutableListOf<ClassSchedule>()
        var lastKnownDayOfWeek = 1

        for (row in tableData.rows) {
            val dayRaw = row.getOrNull(mapping.dayColumnIndex)?.trim() ?: ""
            val timeRaw = row.getOrNull(mapping.timeColumnIndex)?.trim() ?: ""
            val subjectRaw = row.getOrNull(mapping.subjectColumnIndex)?.trim() ?: ""
            val roomRaw = if (mapping.roomColumnIndex >= 0) row.getOrNull(mapping.roomColumnIndex)?.trim() else null
            val instructorRaw = if (mapping.instructorColumnIndex >= 0) row.getOrNull(mapping.instructorColumnIndex)?.trim() else null

            if (subjectRaw.isBlank()) continue

            // Resolve Day
            val detectedDay = ClassSchedule.parseDayOfWeek(dayRaw)
            if (detectedDay != null) {
                lastKnownDayOfWeek = detectedDay
            }

            // Resolve Start & End Time
            val (startMin, endMin) = parseTimeRange(timeRaw)
            if (startMin == null) continue

            results.add(
                ClassSchedule(
                    dayOfWeek = lastKnownDayOfWeek,
                    startMinute = startMin,
                    endMinute = endMin ?: (startMin + 100), // Default 100-min lecture if no end time given
                    subjectName = subjectRaw,
                    roomName = roomRaw?.ifBlank { null },
                    instructorName = instructorRaw?.ifBlank { null },
                    colorHex = colorHex
                )
            )
        }

        return results
    }

    private fun parseTimeRange(timeStr: String): Pair<Int?, Int?> {
        val cleaned = timeStr.trim().replace(" ", "").replace('.', ':')
        val separators = listOf("-", "–", "s/d", "sd", "to")
        for (sep in separators) {
            if (cleaned.contains(sep)) {
                val parts = cleaned.split(sep)
                if (parts.size >= 2) {
                    val s = ClassSchedule.parseTimeToMinutes(parts[0])
                    val e = ClassSchedule.parseTimeToMinutes(parts[1])
                    return Pair(s, e)
                }
            }
        }
        val single = ClassSchedule.parseTimeToMinutes(cleaned)
        return Pair(single, null)
    }
}
