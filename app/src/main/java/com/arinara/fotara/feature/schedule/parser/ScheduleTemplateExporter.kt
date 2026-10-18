// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.schedule.parser

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Generates and exports a standard, fully-valid OpenXML Excel (.xlsx) schedule template
 * without external dependencies to keep application bundle size optimal.
 */
object ScheduleTemplateExporter {

    val HEADERS = listOf("Day", "Start Time", "End Time", "Subject", "Room", "Instructor")

    val SAMPLE_ROWS = listOf(
        listOf("Monday", "08:00", "09:30", "Linear Algebra", "Room 301", "Dr. Euler"),
        listOf("Monday", "10:00", "11:30", "Algorithms", "Lab 2", "Prof. Dijkstra"),
        listOf("Tuesday", "09:00", "10:30", "Database Architecture", "Lab 1", "Dr. Codd"),
        listOf("Wednesday", "13:00", "14:30", "Computer Systems", "Hall A", "Prof. Hopper"),
        listOf("Thursday", "10:00", "11:30", "Software Engineering", "Room 204", "Prof. Brooks"),
        listOf("Friday", "08:30", "10:00", "Academic Writing", "Seminar 2", "Dr. Strunk")
    )

    fun generateTemplateXlsxBytes(): ByteArray {
        val allRows = mutableListOf<List<String>>()
        allRows.add(HEADERS)
        allRows.addAll(SAMPLE_ROWS)

        // Build shared strings table
        val sharedStrings = mutableListOf<String>()
        val stringToIndex = mutableMapOf<String, Int>()

        for (row in allRows) {
            for (cell in row) {
                if (!stringToIndex.containsKey(cell)) {
                    stringToIndex[cell] = sharedStrings.size
                    sharedStrings.add(cell)
                }
            }
        }

        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            // 1. [Content_Types].xml
            writeZipEntry(
                zos,
                "[Content_Types].xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>
</Types>"""
            )

            // 2. _rels/.rels
            writeZipEntry(
                zos,
                "_rels/.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""
            )

            // 3. xl/_rels/workbook.xml.rels
            writeZipEntry(
                zos,
                "xl/_rels/workbook.xml.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>
</Relationships>"""
            )

            // 4. xl/workbook.xml
            writeZipEntry(
                zos,
                "xl/workbook.xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Timetable" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>"""
            )

            // 5. xl/sharedStrings.xml
            val ssBuilder = StringBuilder()
            ssBuilder.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            ssBuilder.append("""<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="${sharedStrings.size}" uniqueCount="${sharedStrings.size}">""")
            for (str in sharedStrings) {
                ssBuilder.append("<si><t>").append(escapeXml(str)).append("</t></si>")
            }
            ssBuilder.append("</sst>")
            writeZipEntry(zos, "xl/sharedStrings.xml", ssBuilder.toString())

            // 6. xl/worksheets/sheet1.xml
            val sheetBuilder = StringBuilder()
            sheetBuilder.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            sheetBuilder.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
            sheetBuilder.append("<sheetData>")

            val colLetters = listOf("A", "B", "C", "D", "E", "F")
            for ((rowIndex, row) in allRows.withIndex()) {
                val rowNum = rowIndex + 1
                sheetBuilder.append("""<row r="$rowNum">""")
                for ((colIndex, cellVal) in row.withIndex()) {
                    val colLetter = if (colIndex < colLetters.size) colLetters[colIndex] else "A"
                    val strIndex = stringToIndex[cellVal] ?: 0
                    sheetBuilder.append("""<c r="$colLetter$rowNum" t="s"><v>$strIndex</v></c>""")
                }
                sheetBuilder.append("</row>")
            }
            sheetBuilder.append("</sheetData>")
            sheetBuilder.append("</worksheet>")
            writeZipEntry(zos, "xl/worksheets/sheet1.xml", sheetBuilder.toString())
        }

        return baos.toByteArray()
    }

    fun exportTemplateToDownloads(context: Context): Result<String> {
        return try {
            val bytes = generateTemplateXlsxBytes()
            val filename = "Fotara_Schedule_Template.xlsx"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return Result.failure(IllegalStateException("Could not create file entry in Downloads"))

                resolver.openOutputStream(uri)?.use { out ->
                    out.write(bytes)
                    out.flush()
                } ?: return Result.failure(IllegalStateException("Could not write template to Downloads"))

                Result.success("Downloads/$filename")
            } else {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!dir.exists()) dir.mkdirs()
                val targetFile = File(dir, filename)
                FileOutputStream(targetFile).use { out ->
                    out.write(bytes)
                    out.flush()
                }
                Result.success(targetFile.absolutePath)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun writeZipEntry(zos: ZipOutputStream, entryName: String, content: String) {
        val entry = ZipEntry(entryName)
        zos.putNextEntry(entry)
        val bytes = content.toByteArray(StandardCharsets.UTF_8)
        zos.write(bytes, 0, bytes.size)
        zos.closeEntry()
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
