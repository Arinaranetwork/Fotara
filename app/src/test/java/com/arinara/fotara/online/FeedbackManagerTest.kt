// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.online

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class FeedbackManagerTest {

    @Test
    fun constants_verifyStrictLimitConfiguration() {
        assertEquals(5, FeedbackManager.FEEDBACK_DAILY_LIMIT)
        assertEquals(60L, FeedbackManager.FEEDBACK_COOLDOWN_SECONDS)
        assertEquals(60_000L, FeedbackManager.FEEDBACK_COOLDOWN_MS)
        assertEquals(24 * 60 * 60 * 1000L, FeedbackManager.FEEDBACK_ROLLING_WINDOW_MS)
    }

    @Test
    fun feedbackCategory_dbValuesMatchSupabaseCheckConstraint() {
        assertEquals("Bug Report", FeedbackCategory.BUG_REPORT.dbValue)
        assertEquals("Suggestion", FeedbackCategory.SUGGESTION.dbValue)
        assertEquals("Feature Idea", FeedbackCategory.FEATURE_IDEA.dbValue)
        assertEquals("General", FeedbackCategory.GENERAL.dbValue)

        // Verify case-insensitive fromString resolution
        assertEquals(FeedbackCategory.BUG_REPORT, FeedbackCategory.fromString("BUG_REPORT"))
        assertEquals(FeedbackCategory.BUG_REPORT, FeedbackCategory.fromString("Bug Report"))
        assertEquals(FeedbackCategory.SUGGESTION, FeedbackCategory.fromString("suggestion"))
        assertEquals(FeedbackCategory.FEATURE_IDEA, FeedbackCategory.fromString("feature idea"))
        assertEquals(FeedbackCategory.GENERAL, FeedbackCategory.fromString("Unknown"))
    }

    @Test
    fun parseResponse_distinguishesRateLimitTriggerFromMalformedRequest() {
        // PostgREST surfaces PostgreSQL RAISE EXCEPTION as HTTP 400 with P0001 code
        val rateLimitTriggerBody = """{"code":"P0001","details":null,"hint":null,"message":"Rate limit exceeded: A maximum of 5 submissions per 24 hours per installation is permitted."}"""
        val parsedRateLimit = FeedbackPayloadBuilder.parseResponse(400, rateLimitTriggerBody)

        assertTrue("P0001 trigger must be recognized as rate limit", parsedRateLimit.isRateLimit)
        assertFalse("Rate limit is transient, not permanent client format error", parsedRateLimit.isPermanentClientError)
        assertTrue("User message must be clear English", parsedRateLimit.userFriendlyMessage.contains("Daily submission limit reached"))
        assertTrue("Technical details must preserve status code and trigger body", parsedRateLimit.technicalDetails.contains("HTTP 400"))
        assertTrue("Technical details must contain P0001", parsedRateLimit.technicalDetails.contains("P0001"))
    }

    @Test
    fun parseResponse_recognizes60SecondCooldownTrigger() {
        val cooldownBody = """{"code":"P0001","message":"Rate limit exceeded: Please wait 60 seconds between feedback submissions."}"""
        val parsedCooldown = FeedbackPayloadBuilder.parseResponse(400, cooldownBody)

        assertTrue(parsedCooldown.isRateLimit)
        assertFalse(parsedCooldown.isPermanentClientError)
        assertTrue(parsedCooldown.userFriendlyMessage.contains("60 seconds"))
    }

    @Test
    fun parseResponse_recognizesHttp429TooManyRequests() {
        val parsed429 = FeedbackPayloadBuilder.parseResponse(429, "Too Many Requests")
        assertTrue(parsed429.isRateLimit)
        assertFalse(parsed429.isPermanentClientError)
    }

    @Test
    fun parseResponse_flagsCheckConstraintViolationAsPermanentClientError() {
        val constraintBody = """{"code":"23514","details":null,"hint":null,"message":"new row for relation \"suggestions\" violates check constraint \"suggestions_category_check\""}"""
        val parsedConstraint = FeedbackPayloadBuilder.parseResponse(400, constraintBody)

        assertFalse("Check constraint violation is not a rate limit", parsedConstraint.isRateLimit)
        assertTrue("Check constraint violation is permanent client error", parsedConstraint.isPermanentClientError)
        assertTrue(parsedConstraint.userFriendlyMessage.contains("server rejected the submission format"))
        assertTrue(parsedConstraint.technicalDetails.contains("23514"))
    }

    @Test
    fun parseResponse_handlesServer5xxAsTransient() {
        val serverErr = FeedbackPayloadBuilder.parseResponse(500, """{"message":"Database connection timeout"}""")

        assertFalse(serverErr.isRateLimit)
        assertFalse("5xx error is retryable, not permanent client error", serverErr.isPermanentClientError)
        assertTrue(serverErr.userFriendlyMessage.contains("server is temporarily unavailable"))
        assertTrue(serverErr.technicalDetails.contains("HTTP 500"))
    }

    @Test
    fun payload_doesNotContainSyncedProperty() {
        val payload = FeedbackPayloadBuilder.buildPayload(
            id = "test-id",
            installUuid = "install-uuid",
            category = FeedbackCategory.BUG_REPORT,
            content = "Test content",
            email = "user@test.com",
            diagnosticInfo = "Diag info"
        )
        assertFalse("Raw payload must never contain synced property", payload.has("synced"))

        // Simulate local queue cloning
        val queueItem = org.json.JSONObject(payload.toString()).apply { put("synced", false) }
        assertTrue(queueItem.has("synced"))
        assertFalse("Original payload must not be mutated by queueItem", payload.has("synced"))

        val sanitized = org.json.JSONObject(queueItem.toString()).apply { remove("synced") }
        assertFalse("Sanitized sendPayload must not have synced property", sanitized.has("synced"))
    }
}
