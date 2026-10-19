// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.diagnostic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class DiagnosticZipExporterTest {

    @Test
    fun testGenerateActivityEventsCsv() {
        val events = listOf(
            DiagnosticEvent(
                id = 1L,
                timestampMs = 1700000000000L,
                tag = "Navigation",
                message = "Navigated to Home, with subview",
                elapsedSinceAppStartMs = 150L,
                elapsedSinceLastEventMs = 20L
            )
        )

        val csv = DiagnosticZipExporter.generateActivityEventsCsv(events)
        assertTrue(csv.contains("ID,Timestamp_ms,Formatted_Time,Elapsed_Since_App_Start_ms,Elapsed_Since_Last_Event_ms,Tag,Message"))
        assertTrue(csv.contains("1,1700000000000,"))
        assertTrue(csv.contains("Navigation"))
        // Check CSV escaping around comma in message
        assertTrue(csv.contains("\"Navigated to Home, with subview\""))
    }

    @Test
    fun testGenerateRamTimelineCsv() {
        val samples = listOf(
            RamSample(
                timestampMs = 1700000000000L,
                jvmUsedMb = 45L,
                jvmAllocatedMb = 64L,
                nativeAllocatedMb = 28L,
                activeScreen = "CanvasScreen"
            )
        )

        val csv = DiagnosticZipExporter.generateRamTimelineCsv(samples)
        assertTrue(csv.contains("Timestamp_ms,Formatted_Time,JVM_Used_MB,JVM_Allocated_MB,Native_Allocated_MB,Active_Screen"))
        assertTrue(csv.contains("45,64,28,CanvasScreen"))
    }

    @Test
    fun testGenerateWorkerMemoryCsv() {
        val records = listOf(
            WorkerMemoryRecord(
                id = 10L,
                timestampMs = 1700000000000L,
                workerName = "AutosaveWorker",
                startHeapMb = 20L,
                endHeapMb = 25L,
                deltaHeapMb = 5L,
                durationMs = 50L
            )
        )

        val csv = DiagnosticZipExporter.generateWorkerMemoryCsv(records)
        assertTrue(csv.contains("ID,Timestamp_ms,Formatted_Time,Worker_Name,Start_Heap_MB,End_Heap_MB,Delta_Heap_MB,Duration_ms"))
        assertTrue(csv.contains("10,1700000000000,"))
        assertTrue(csv.contains("AutosaveWorker,20,25,5,50"))
    }

    @Test
    fun testGenerateStuckErrorsText() {
        val stuckList = listOf(
            StuckErrorRecord(
                id = 1L,
                timestampMs = 1700000000000L,
                durationMs = 450L,
                activeScreen = "Home",
                topMethod = "com.arinara.fotara.Render.heavyMethod",
                fullStackTrace = "at com.arinara.fotara.Render.heavyMethod(Render.kt:42)",
                isSevereFreeze = false
            ),
            StuckErrorRecord(
                id = 2L,
                timestampMs = 1700000005000L,
                durationMs = 1200L,
                activeScreen = "Canvas",
                topMethod = "com.arinara.fotara.Canvas.export",
                fullStackTrace = "at com.arinara.fotara.Canvas.export(Canvas.kt:100)",
                isSevereFreeze = true
            )
        )

        val text = DiagnosticZipExporter.generateStuckErrorsText(stuckList)
        assertTrue(text.contains("=== FOTARA MAIN-THREAD HANG & FREEZE LOGS ==="))
        assertTrue(text.contains("Total recorded hitches/freezes: 2"))
        assertTrue(text.contains("[HITCH] Duration: 450ms | Screen: Home"))
        assertTrue(text.contains("[SEVERE FREEZE] Duration: 1200ms | Screen: Canvas"))
        assertTrue(text.contains("com.arinara.fotara.Render.heavyMethod"))
    }

    @Test
    fun testGenerateErrorsText() {
        val errors = listOf(
            DiagnosticErrorEntry(
                id = 1L,
                timestampMs = 1700000000000L,
                tag = "Database",
                message = "Corrupted SQLite page",
                exceptionClass = "android.database.sqlite.SQLiteDiskIOException",
                stackTrace = "android.database.sqlite.SQLiteDiskIOException: disk I/O error",
                threadName = "main"
            )
        )

        val text = DiagnosticZipExporter.generateErrorsText(errors, "Crash on line 99")
        assertTrue(text.contains("=== FOTARA CAUGHT EXCEPTIONS & RUNTIME ERRORS ==="))
        assertTrue(text.contains("PREVIOUS FATAL CRASH LOG"))
        assertTrue(text.contains("Crash on line 99"))
        assertTrue(text.contains("Corrupted SQLite page"))
        assertTrue(text.contains("SQLiteDiskIOException"))
    }

    @Test
    fun testGenerateDeviceInfoText() {
        val hw = HardwareInfo(
            manufacturer = "Infinix",
            model = "X6812B",
            brand = "Infinix",
            board = "Infinix-X6812B",
            androidVersion = "11",
            apiLevel = 30,
            buildFingerprint = "Infinix/X6812B/11",
            cpuCores = 8,
            jvmHeapUsedMb = 20L,
            jvmHeapTotalMb = 25L,
            jvmHeapMaxMb = 256L,
            systemRamAvailableMb = 1800L,
            systemRamTotalMb = 6000L,
            isLowMemory = false,
            storageFreeMb = 5000L,
            storageTotalMb = 64000L
        )

        val text = DiagnosticZipExporter.generateDeviceInfoText(hw)
        assertTrue(text.contains("=== FOTARA SYSTEM & HARDWARE SPECIFICATIONS ==="))
        assertTrue(text.contains("Manufacturer: Infinix"))
        assertTrue(text.contains("Model: X6812B"))
        assertTrue(text.contains("Android Version: 11 (API 30)"))
        assertTrue(text.contains("CPU Cores: 8"))
        assertTrue(text.contains("JVM Heap Used: 20 MB"))
    }

    @Test
    fun testZipArchiveStreamIntegrity() {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            fun addEntry(name: String, content: String) {
                zos.putNextEntry(ZipEntry(name))
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }

            addEntry("summary.md", "# Summary")
            addEntry("activity_events.csv", "ID,Tag\n1,Test")
            addEntry("ram_timeline.csv", "Timestamp,MB\n100,50")
            addEntry("worker_memory.csv", "ID,Name\n1,Worker")
            addEntry("stuck_errors.txt", "No stuck errors")
            addEntry("errors.txt", "No errors")
            addEntry("device_info.txt", "Device: Test")
        }

        val zipBytes = baos.toByteArray()
        val entryNames = mutableListOf<String>()

        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                entryNames.add(entry.name)
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        assertEquals(7, entryNames.size)
        assertTrue(entryNames.contains("summary.md"))
        assertTrue(entryNames.contains("activity_events.csv"))
        assertTrue(entryNames.contains("ram_timeline.csv"))
        assertTrue(entryNames.contains("worker_memory.csv"))
        assertTrue(entryNames.contains("stuck_errors.txt"))
        assertTrue(entryNames.contains("errors.txt"))
        assertTrue(entryNames.contains("device_info.txt"))
    }
}
