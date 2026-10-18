// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.diagnostic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MainThreadWatchdogTest {

    @Test
    fun extractTopAppMethod_isolatesFotaraAppClassAndLine() {
        val stackTrace = arrayOf(
            StackTraceElement("android.os.MessageQueue", "nativePollOnce", "MessageQueue.java", -2),
            StackTraceElement("android.os.MessageQueue", "next", "MessageQueue.java", 335),
            StackTraceElement("android.os.Looper", "loopOnce", "Looper.java", 186),
            StackTraceElement("com.arinara.fotara.diagnostic.MainThreadWatchdog", "run", "MainThreadWatchdog.kt", 60),
            StackTraceElement("com.arinara.fotara.canvas.CanvasDrawingView", "onDrawStrokes", "CanvasDrawingView.kt", 142),
            StackTraceElement("com.arinara.fotara.canvas.CanvasDrawingView", "onDraw", "CanvasDrawingView.kt", 88)
        )

        val topMethod = MainThreadWatchdog.extractTopAppMethod(stackTrace)
        assertEquals("CanvasDrawingView.onDrawStrokes(CanvasDrawingView.kt:142)", topMethod)
    }

    @Test
    fun extractTopAppMethod_withoutAppFrames_fallsBackToTopNonSystem() {
        val stackTrace = arrayOf(
            StackTraceElement("java.lang.Thread", "sleep", "Thread.java", -1),
            StackTraceElement("org.custom.render.Renderer", "renderLoop", "Renderer.java", 45)
        )

        val topMethod = MainThreadWatchdog.extractTopAppMethod(stackTrace)
        assertEquals("Renderer.renderLoop", topMethod)
    }

    @Test
    fun formatStackTrace_formatsNeatlyWithIndent() {
        val stackTrace = arrayOf(
            StackTraceElement("com.arinara.fotara.MainActivity", "onCreate", "MainActivity.kt", 45),
            StackTraceElement("android.app.Activity", "performCreate", "Activity.java", 8000)
        )

        val formatted = MainThreadWatchdog.formatStackTrace(stackTrace)
        assertTrue(formatted.contains("at com.arinara.fotara.MainActivity.onCreate(MainActivity.kt:45)"))
        assertTrue(formatted.contains("at android.app.Activity.performCreate(Activity.java:8000)"))
    }

    @Test
    fun stuckErrorRecord_classificationOfSevereFreeze() {
        val hitch = StuckErrorRecord(
            id = 1L,
            timestampMs = 1000L,
            durationMs = 250L,
            activeScreen = "Home",
            topMethod = "HomeViewModel.loadNotes",
            fullStackTrace = "",
            isSevereFreeze = false
        )
        assertTrue(!hitch.isSevereFreeze)

        val freeze = StuckErrorRecord(
            id = 2L,
            timestampMs = 2000L,
            durationMs = 650L,
            activeScreen = "Canvas",
            topMethod = "CanvasDrawingView.onDraw",
            fullStackTrace = "",
            isSevereFreeze = true
        )
        assertTrue(freeze.isSevereFreeze)
    }
}
