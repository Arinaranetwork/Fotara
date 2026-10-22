// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.expression

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExpressionEngineTest {

    private val fixedTimestamp = 1792672200000L // A fixed Thursday in Oct 2026
    private val fixedContext = ExpressionContext(
        timestamp = fixedTimestamp,
        page = 3,
        pages = 10,
        title = "Physics Lecture",
        folder = "Science",
        userName = "Andi",
        appName = "Fotara",
        appVersion = "2.0.0",
        scopeId = "folder_1",
        locale = Locale.US,
        dateFormat = "yyyy-MM-dd",
        timeFormat = "HH:mm",
        counterProvider = { 42 }
    )

    @Test
    fun testBuiltInTimeExpressions() {
        val dateExpected = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(fixedTimestamp))
        val timeExpected = SimpleDateFormat("HH:mm", Locale.US).format(Date(fixedTimestamp))

        assertEquals(dateExpected, ExpressionEngine.resolve("!date", fixedContext))
        assertEquals(timeExpected, ExpressionEngine.resolve("!time", fixedContext))
        assertEquals("$dateExpected $timeExpected", ExpressionEngine.resolve("!datetime", fixedContext))
        assertEquals("Thursday", ExpressionEngine.resolve("!day", fixedContext))
        assertEquals("October", ExpressionEngine.resolve("!month", fixedContext))
        assertEquals("2026", ExpressionEngine.resolve("!year", fixedContext))
        assertTrue(ExpressionEngine.resolve("!week", fixedContext).startsWith("W"))
    }

    @Test
    fun testCustomFormatParameters() {
        val customDate = ExpressionEngine.resolve("!date:ddMMyy", fixedContext)
        val expected = SimpleDateFormat("ddMMyy", Locale.US).format(Date(fixedTimestamp))
        assertEquals(expected, customDate)

        val customTime = ExpressionEngine.resolve("!time:HHmm", fixedContext)
        val expectedTime = SimpleDateFormat("HHmm", Locale.US).format(Date(fixedTimestamp))
        assertEquals(expectedTime, customTime)

        val customDash = ExpressionEngine.resolve("!date:yyyy/MM/dd", fixedContext)
        val expectedDash = SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(fixedTimestamp))
        assertEquals(expectedDash, customDash)
    }

    @Test
    fun testContextExpressions() {
        assertEquals("3", ExpressionEngine.resolve("!page", fixedContext))
        assertEquals("10", ExpressionEngine.resolve("!pages", fixedContext))
        assertEquals("Physics Lecture", ExpressionEngine.resolve("!title", fixedContext))
        assertEquals("Science", ExpressionEngine.resolve("!folder", fixedContext))
        assertEquals("Fotara 2.0.0", ExpressionEngine.resolve("!app", fixedContext))
        assertEquals("Andi", ExpressionEngine.resolve("!user", fixedContext))
    }

    @Test
    fun testUserWithoutProfile_leavesAsPlainText() {
        val emptyUserContext = fixedContext.copy(userName = null)
        assertEquals("!user", ExpressionEngine.resolve("!user", emptyUserContext))
    }

    @Test
    fun testSequenceAndRandom() {
        assertEquals("42", ExpressionEngine.resolve("!n", fixedContext))

        val id = ExpressionEngine.resolve("!id", fixedContext)
        assertEquals(6, id.length)
        assertTrue(id.all { it.isLetterOrDigit() })

        val rand = ExpressionEngine.resolve("!rand", fixedContext)
        val randInt = rand.toIntOrNull()
        assertTrue(randInt != null && randInt in 100..999)
    }

    @Test
    fun testTextTransforms() {
        assertEquals("SCIENCE", ExpressionEngine.resolve("!upper:science", fixedContext))
        assertEquals("science", ExpressionEngine.resolve("!lower:SCIENCE", fixedContext))
        assertEquals("Hello World", ExpressionEngine.resolve("!cap:hello world", fixedContext))

        // Global transform at string start
        assertEquals("PHYSICS LECTURE NOTES", ExpressionEngine.resolve("!upper Physics Lecture Notes", fixedContext))
        assertEquals("physics lecture notes", ExpressionEngine.resolve("!lower Physics Lecture Notes", fixedContext))
        assertEquals("Physics Lecture Notes", ExpressionEngine.resolve("!cap physics lecture notes", fixedContext))
    }

    @Test
    fun testEscapeSyntax() {
        assertEquals("!date", ExpressionEngine.resolve("\\!date", fixedContext))
        assertEquals("Report !date: 2026-10-22", ExpressionEngine.resolve("Report \\!date: !date:yyyy-MM-dd", fixedContext))
        assertEquals("!", ExpressionEngine.resolve("\\!", fixedContext))
    }

    @Test
    fun testUnknownExpressions_remainPlainText() {
        assertEquals("!foobar", ExpressionEngine.resolve("!foobar", fixedContext))
        assertEquals("Note !unknown_123 text", ExpressionEngine.resolve("Note !unknown_123 text", fixedContext))
    }

    @Test
    fun testDisabledBuiltIns_remainPlainText() {
        val disabled = setOf("date", "time")
        assertEquals("!date", ExpressionEngine.resolve("!date", fixedContext, disabledBuiltIns = disabled))
        assertEquals("Thursday", ExpressionEngine.resolve("!day", fixedContext, disabledBuiltIns = disabled))
    }

    @Test
    fun testCustomWordExpression() {
        val custom = listOf(
            CustomExpression(
                id = 1L,
                name = "sig",
                type = ExpressionType.WORD,
                value = "Created by Andi"
            )
        )
        val resolved = ExpressionEngine.resolve("Doc by !sig", fixedContext, customExpressions = custom)
        assertEquals("Doc by Created by Andi", resolved)
    }

    @Test
    fun testCustomCombinationExpression() {
        val custom = listOf(
            CustomExpression(
                id = 2L,
                name = "log",
                type = ExpressionType.COMBINATION,
                value = "!day !date !time"
            )
        )
        val dateExpected = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(fixedTimestamp))
        val timeExpected = SimpleDateFormat("HH:mm", Locale.US).format(Date(fixedTimestamp))
        val resolved = ExpressionEngine.resolve("[!log]", fixedContext, customExpressions = custom)
        assertEquals("[Thursday $dateExpected $timeExpected]", resolved)
    }

    @Test
    fun testFiveExpressionLimitValidation() {
        // 5 expressions: Valid
        val validCombo = CustomExpression(
            name = "validCombo",
            type = ExpressionType.COMBINATION,
            value = "!date + !time - !day / !month * !year"
        )
        val resValid = ExpressionEngine.validateCustomExpression(validCombo, emptyList())
        assertTrue(resValid.isValid)

        // 6 expressions: Rejected
        val invalidCombo = CustomExpression(
            name = "tooMany",
            type = ExpressionType.COMBINATION,
            value = "!date + !time + !day + !month + !year + !week"
        )
        val resInvalid = ExpressionEngine.validateCustomExpression(invalidCombo, emptyList())
        assertFalse(resInvalid.isValid)
        assertTrue(resInvalid.errorMessage?.contains("at most 5 expressions") == true)
    }

    @Test
    fun testCircularReferenceDetection() {
        // Direct self-reference
        val selfRef = CustomExpression(name = "loop", type = ExpressionType.WORD, value = "Text with !loop")
        val selfResult = ExpressionEngine.validateCustomExpression(selfRef, emptyList())
        assertFalse(selfResult.isValid)
        assertTrue(selfResult.errorMessage?.contains("Circular reference detected") == true)

        // Indirect cycle: A -> B, B -> A
        val existingA = CustomExpression(id = 1L, name = "exprA", type = ExpressionType.WORD, value = "Includes !exprB")
        val candidateB = CustomExpression(id = 2L, name = "exprB", type = ExpressionType.WORD, value = "Calls !exprA")
        val cycleResult = ExpressionEngine.validateCustomExpression(candidateB, listOf(existingA))
        assertFalse(cycleResult.isValid)
        assertTrue(cycleResult.errorMessage?.contains("Circular reference detected") == true)
    }

    @Test
    fun testStaticVsDynamicMode() {
        // Single ! is static, !! or ~ is dynamic
        val template = "Static: !date, Dynamic1: !!date, Dynamic2: !date~"

        // 1. Live resolve resolves everything
        val dateExpected = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(fixedTimestamp))
        val fullResolved = ExpressionEngine.resolve(template, fixedContext)
        assertEquals("Static: $dateExpected, Dynamic1: $dateExpected, Dynamic2: $dateExpected", fullResolved)

        // 2. Lock static only replaces single !, preserving !! and ~
        val locked = ExpressionEngine.lockStaticExpressions(template, fixedContext)
        assertEquals("Static: $dateExpected, Dynamic1: !!date, Dynamic2: !date~", locked)
    }

    @Test
    fun testAdjacentAndMultipleExpressions() {
        val dateExpected = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(fixedTimestamp))
        val timeExpected = SimpleDateFormat("HH:mm", Locale.US).format(Date(fixedTimestamp))

        assertEquals("${dateExpected}_$timeExpected", ExpressionEngine.resolve("!date_!time", fixedContext))
        assertEquals("$dateExpected$timeExpected", ExpressionEngine.resolve("!date!time", fixedContext))
        assertEquals("Report $dateExpected $timeExpected - Final", ExpressionEngine.resolve("Report !date !time - Final", fixedContext))
    }

    @Test
    fun testFileNameSanitization() {
        val dirty = "Project / Task : * ? < > | File.pdf"
        val clean = ExpressionEngine.sanitizeFileName(dirty)
        assertFalse(clean.contains("/"))
        assertFalse(clean.contains(":"))
        assertFalse(clean.contains("*"))
        assertFalse(clean.contains("?"))
        assertFalse(clean.contains("<"))
        assertFalse(clean.contains(">"))
        assertFalse(clean.contains("|"))

        val fileContext = fixedContext.copy(isFileName = true)
        val resolved = ExpressionEngine.resolve("Report: !title / !date:yyyy/MM/dd", fileContext)
        assertFalse(resolved.contains("/"))
        assertFalse(resolved.contains(":"))
    }

    @Test
    fun testAutocompleteSuggestions() {
        val custom = listOf(CustomExpression(id = 1L, name = "signature", type = ExpressionType.WORD, value = "Andi"))
        val suggestions = ExpressionEngine.getSuggestions("!da", customExpressions = custom)
        assertTrue(suggestions.any { it.token == "date" })
        assertTrue(suggestions.any { it.token == "datetime" })
        assertTrue(suggestions.any { it.token == "day" })
        assertFalse(suggestions.any { it.token == "time" })

        val customSuggestions = ExpressionEngine.getSuggestions("!sig", customExpressions = custom)
        assertTrue(customSuggestions.any { it.token == "signature" })
    }
}
