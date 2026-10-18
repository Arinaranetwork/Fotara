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

class AppDiagnosticLoggerTest {

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
    fun testStartupMilestones() {
        AppDiagnosticLogger.recordAppCreateStart()
        AppDiagnosticLogger.recordAppCreateEnd()
        AppDiagnosticLogger.recordContainerInit(42L)
        AppDiagnosticLogger.recordSetContent()
        AppDiagnosticLogger.recordFirstFrameRendered()

        val metrics = AppDiagnosticLogger.startupMetricsFlow.value
        assertTrue("AppCreate duration should be non-negative", metrics.appCreateDurationMs >= 0)
        assertEquals(42L, metrics.containerInitDurationMs)
        assertTrue("Total cold start should be non-negative", metrics.totalColdStartMs >= 0)
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
        val report = AppDiagnosticLogger.generateFullReport(null)
        assertNotNull(report)
        assertTrue("Report should contain Fotara Diagnostic Report header", report.contains("## Fotara Diagnostic & Performance Report"))
        assertTrue("Report should contain Startup Latency Breakdown section", report.contains("### 2. Startup Latency Breakdown"))
        assertTrue("Report should contain Chronological Event Timeline section", report.contains("### 3. Chronological Event Timeline"))
        assertTrue("Report should contain ReportTest log entry", report.contains("ReportTest"))
    }
}
