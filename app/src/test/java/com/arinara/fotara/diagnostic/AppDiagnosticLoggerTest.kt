// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.diagnostic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppDiagnosticLoggerTest {

    @Before
    fun setUp() {
        AppDiagnosticLogger.resetForTesting()
    }

    @Test
    fun testRecordEventAndBufferFlow() {
        val testTag = "TestTag"
        val testMsg = "Diagnostic event validation message"
        AppDiagnosticLogger.recordEvent(testTag, testMsg)

        val events = AppDiagnosticLogger.eventsFlow.value
        assertTrue("Events list should not be empty", events.isNotEmpty())
        val lastEvent = events.last()
        assertEquals(testTag, lastEvent.tag)
        assertEquals(testMsg, lastEvent.message)
        assertTrue(lastEvent.elapsedSinceAppStartMs >= 0)
    }

    @Test
    fun testStartupMilestonesAndColdStartIdempotency() {
        AppDiagnosticLogger.recordAppCreateStart()
        AppDiagnosticLogger.recordAppCreateEnd()
        AppDiagnosticLogger.recordContainerInit(42L)
        AppDiagnosticLogger.recordSetContent()
        AppDiagnosticLogger.recordFirstFrameRendered()

        val initialMetrics = AppDiagnosticLogger.startupMetricsFlow.value
        assertTrue("AppCreate duration should be non-negative", initialMetrics.appCreateDurationMs >= 0)
        assertEquals(42L, initialMetrics.containerInitDurationMs)
        assertTrue("Total cold start should be non-negative", initialMetrics.totalColdStartMs >= 0)
        val initialFirstFrameTime = initialMetrics.firstFrameRenderedMs
        val initialColdStart = initialMetrics.totalColdStartMs

        // Simulate navigation re-entry to Home screen
        Thread.sleep(10)
        AppDiagnosticLogger.recordFirstFrameRendered()

        val reenteredMetrics = AppDiagnosticLogger.startupMetricsFlow.value
        assertEquals("Cold start latency must not be overwritten on re-entry", initialColdStart, reenteredMetrics.totalColdStartMs)
        assertEquals("First frame timestamp must remain frozen on re-entry", initialFirstFrameTime, reenteredMetrics.firstFrameRenderedMs)
    }

    @Test
    fun testRecordErrorAndExceptions() {
        val ex = IllegalStateException("Sample test failure exception")
        AppDiagnosticLogger.recordError("TestEngine", "Failed to decode matrix", ex)

        val errors = AppDiagnosticLogger.errorsFlow.value
        assertEquals(1, errors.size)
        val err = errors.first()
        assertEquals("TestEngine", err.tag)
        assertEquals("Failed to decode matrix", err.message)
        assertEquals(IllegalStateException::class.java.name, err.exceptionClass)
        assertTrue("Stack trace should not be empty", err.stackTrace.contains("Sample test failure exception"))
    }

    @Test
    fun testScreenTracking() {
        AppDiagnosticLogger.recordScreen("DiagnosticScreen")
        assertEquals("DiagnosticScreen", AppDiagnosticLogger.currentScreen.value)

        AppDiagnosticLogger.recordScreen("SettingsScreen")
        assertEquals("SettingsScreen", AppDiagnosticLogger.currentScreen.value)
    }

    @Test
    fun testGenerateFullReportNullContext() {
        AppDiagnosticLogger.recordEvent("ReportTest", "Report verification log")
        AppDiagnosticLogger.recordError("EngineTest", "Sample caught error", RuntimeException("Boom"))

        val report = AppDiagnosticLogger.generateFullReport(null)
        assertNotNull(report)
        assertTrue("Report should contain Fotara Diagnostic Report header", report.contains("## Fotara Diagnostic & Performance Report"))
        assertTrue("Report should contain Startup Latency Breakdown section", report.contains("Startup Latency Breakdown"))
        assertTrue("Report should contain Memory & RAM Specifications section", report.contains("Memory & RAM Specifications"))
        assertTrue("Report should contain Disk & Storage Footprint section", report.contains("Disk & Storage Footprint"))
        assertTrue("Report should contain Hardware, Display & Environment section", report.contains("Hardware, Display & Environment"))
        assertTrue("Report should contain Recorded Code Errors & Exceptions section", report.contains("Recorded Code Errors & Exceptions"))
        assertTrue("Report should contain Chronological Event Timeline section", report.contains("Chronological Event Timeline"))
        assertTrue("Report should contain EngineTest error entry", report.contains("EngineTest"))
    }

    @Test
    fun testRamSamplingAndTimelineBuffer() {
        AppDiagnosticLogger.recordRamSample("HomeScreen")
        AppDiagnosticLogger.recordRamSample("CanvasScreen")

        val samples = AppDiagnosticLogger.ramHistoryFlow.value
        assertEquals(2, samples.size)
        assertEquals("HomeScreen", samples[0].activeScreen)
        assertEquals("CanvasScreen", samples[1].activeScreen)
        assertTrue(samples[1].jvmUsedMb >= 0)
    }

    @Test
    fun testWorkerMemoryTrackingAndDelta() {
        AppDiagnosticLogger.recordWorkerMemory(
            workerName = "CanvasAutosaveWorker",
            startHeapMb = 30L,
            endHeapMb = 35L,
            durationMs = 45L
        )

        val workers = AppDiagnosticLogger.workerMemoryFlow.value
        assertEquals(1, workers.size)
        val record = workers.first()
        assertEquals("CanvasAutosaveWorker", record.workerName)
        assertEquals(30L, record.startHeapMb)
        assertEquals(35L, record.endHeapMb)
        assertEquals(5L, record.deltaHeapMb)
        assertEquals(45L, record.durationMs)
    }

    @Test
    fun testManualGcAndMeasureDelta() {
        val res = AppDiagnosticLogger.runManualGcAndMeasureDelta()
        assertNotNull(res)
        assertTrue(res.durationMs >= 0)
        assertTrue(res.beforeUsedMb >= 0)
        assertTrue(res.afterUsedMb >= 0)
    }

    @Test
    fun testZeroOverheadMasterDisable() {
        AppDiagnosticLogger.setDiagnosticSystemEnabledForTesting(false)

        AppDiagnosticLogger.recordEvent("BlockedTag", "Should not be recorded")
        AppDiagnosticLogger.recordError("BlockedError", "Should not be recorded")
        AppDiagnosticLogger.recordRamSample("BlockedScreen")
        AppDiagnosticLogger.recordWorkerMemory("BlockedWorker", 10L, 20L, 5L)
        AppDiagnosticLogger.recordScreen("NewScreen")

        assertTrue("Events must be empty when master disabled", AppDiagnosticLogger.eventsFlow.value.isEmpty())
        assertTrue("Errors must be empty when master disabled", AppDiagnosticLogger.errorsFlow.value.isEmpty())
        assertTrue("RAM history must be empty when master disabled", AppDiagnosticLogger.ramHistoryFlow.value.isEmpty())
        assertTrue("Worker memory must be empty when master disabled", AppDiagnosticLogger.workerMemoryFlow.value.isEmpty())
        assertEquals("Startup", AppDiagnosticLogger.currentScreen.value)

        // Re-enable and verify logging resumes
        AppDiagnosticLogger.setDiagnosticSystemEnabledForTesting(true)
        AppDiagnosticLogger.recordEvent("AllowedTag", "Should be recorded now")
        assertEquals(1, AppDiagnosticLogger.eventsFlow.value.size)
        assertEquals("AllowedTag", AppDiagnosticLogger.eventsFlow.value.first().tag)
    }

    @Test
    fun testEventDeduplicationWithin150ms() {
        AppDiagnosticLogger.recordEvent("NavTag", "DuplicateNavigation")
        AppDiagnosticLogger.recordEvent("NavTag", "DuplicateNavigation")

        val events = AppDiagnosticLogger.eventsFlow.value
        assertEquals("Identical events logged immediately must be de-duplicated", 1, events.size)
    }

    @Test
    fun testExportGetters() {
        AppDiagnosticLogger.recordEvent("Tag1", "Message1")
        assertEquals(1, AppDiagnosticLogger.getAllEvents().size)

        AppDiagnosticLogger.recordError("Err1", "ErrorMsg1")
        assertEquals(1, AppDiagnosticLogger.getAllErrors().size)

        AppDiagnosticLogger.recordRamSample("SampleScreen")
        assertEquals(1, AppDiagnosticLogger.getAllRamSamples().size)

        AppDiagnosticLogger.recordWorkerMemory("Worker1", 10L, 15L, 25L)
        assertEquals(1, AppDiagnosticLogger.getAllWorkerRecords().size)
    }
}

