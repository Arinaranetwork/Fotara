// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.coordinator

import com.arinara.fotara.online.ReleaseInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppDialogCoordinatorTest {

    private fun createDummyRelease(version: String = "1.8.0") = ReleaseInfo(
        version = version,
        title = "Fotara Update",
        releaseNotes = "New features",
        downloadUrl = "https://example.com/app.apk"
    )

    @Test
    fun testInitialStateEmpty() {
        val testDispatcher = StandardTestDispatcher()
        val testScope = TestScope(testDispatcher)
        val coordinator = AppDialogCoordinator(scope = testScope, transitionDelayMs = 250L)

        val state = coordinator.state.value
        assertNull(state.visibleDialog)
        assertTrue(state.queue.isEmpty())
        assertTrue(state.isActivityResumed)
        assertFalse(state.isTransitioning)
        assertFalse(state.isDialogRenderable)
        assertFalse(coordinator.hasActiveAppDialog())
    }

    @Test
    fun testSingleRequestBecomesVisible() {
        val testDispatcher = StandardTestDispatcher()
        val testScope = TestScope(testDispatcher)
        val coordinator = AppDialogCoordinator(scope = testScope, transitionDelayMs = 250L)

        val updateRequest = AppDialogRequest.Update(release = createDummyRelease())
        coordinator.requestDialog(updateRequest)

        val state = coordinator.state.value
        assertEquals("UPDATE", state.visibleDialog?.key)
        assertTrue(state.queue.isEmpty())
        assertTrue(state.isDialogRenderable)
        assertTrue(coordinator.hasActiveAppDialog())
    }

    @Test
    fun testDeduplicationByKey() {
        val testDispatcher = StandardTestDispatcher()
        val testScope = TestScope(testDispatcher)
        val coordinator = AppDialogCoordinator(scope = testScope, transitionDelayMs = 250L)

        val consent1 = AppDialogRequest.Consent()
        val consent2 = AppDialogRequest.Consent()

        coordinator.requestDialog(consent1)
        coordinator.requestDialog(consent2)

        val state = coordinator.state.value
        assertEquals("CONSENT", state.visibleDialog?.key)
        assertTrue(state.queue.isEmpty())
    }

    @Test
    fun testHigherPriorityPreemptsLowerWithoutDismissal() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val coordinator = AppDialogCoordinator(scope = testScope, transitionDelayMs = 250L)

        var laterCalled = false
        var skipCalled = false
        val updateRequest = AppDialogRequest.Update(
            release = createDummyRelease(),
            onLater = { laterCalled = true },
            onSkipVersion = { skipCalled = true }
        )
        val consentRequest = AppDialogRequest.Consent()

        // 1. Present UPDATE dialog
        coordinator.requestDialog(updateRequest)
        assertEquals("UPDATE", coordinator.state.value.visibleDialog?.key)

        // 2. CONSENT arrives (priority 100 > 50): preempts UPDATE without dismissing
        coordinator.requestDialog(consentRequest)
        assertEquals("CONSENT", coordinator.state.value.visibleDialog?.key)
        // UPDATE was NOT dismissed
        assertFalse("Later callback must NOT be called on preemption", laterCalled)
        assertFalse("Skip callback must NOT be called on preemption", skipCalled)
        // UPDATE is preserved in the queue
        assertEquals(1, coordinator.state.value.queue.size)
        assertEquals("UPDATE", coordinator.state.value.queue.first().key)

        // 3. User accepts/dismisses CONSENT
        coordinator.dismissDialog("CONSENT")
        assertNull("Visible dialog is cleared immediately during transition", coordinator.state.value.visibleDialog)
        assertTrue("Coordinator is transitioning during 250ms gap", coordinator.state.value.isTransitioning)

        // Advance 240ms: still transitioning
        advanceTimeBy(240L)
        assertTrue(coordinator.state.value.isTransitioning)

        // Advance past 250ms: UPDATE dialog returns!
        advanceTimeBy(20L)
        assertFalse(coordinator.state.value.isTransitioning)
        assertEquals("UPDATE", coordinator.state.value.visibleDialog?.key)
        assertTrue(coordinator.state.value.queue.isEmpty())
    }

    @Test
    fun testLowerPriorityQueuesBehindHigher() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val coordinator = AppDialogCoordinator(scope = testScope, transitionDelayMs = 250L)

        val consentRequest = AppDialogRequest.Consent()
        val updateRequest = AppDialogRequest.Update(release = createDummyRelease())

        // CONSENT presented first
        coordinator.requestDialog(consentRequest)
        assertEquals("CONSENT", coordinator.state.value.visibleDialog?.key)

        // UPDATE arrives while CONSENT is visible
        coordinator.requestDialog(updateRequest)
        assertEquals("CONSENT", coordinator.state.value.visibleDialog?.key)
        assertEquals(1, coordinator.state.value.queue.size)
        assertEquals("UPDATE", coordinator.state.value.queue.first().key)

        // Dismiss CONSENT
        coordinator.dismissDialog("CONSENT")
        advanceTimeBy(260L)

        // UPDATE becomes visible after gap
        assertEquals("UPDATE", coordinator.state.value.visibleDialog?.key)
    }

    @Test
    fun testActivityResumeAndPauseLifecycle() {
        val testDispatcher = StandardTestDispatcher()
        val testScope = TestScope(testDispatcher)
        val coordinator = AppDialogCoordinator(scope = testScope, transitionDelayMs = 250L)

        coordinator.requestDialog(AppDialogRequest.Consent())
        assertTrue(coordinator.state.value.isDialogRenderable)

        // Activity paused
        coordinator.setActivityResumed(false)
        assertFalse("Dialog must not be renderable when activity is paused", coordinator.state.value.isDialogRenderable)

        // Activity resumed
        coordinator.setActivityResumed(true)
        assertTrue("Dialog must become renderable when activity is resumed", coordinator.state.value.isDialogRenderable)
    }
}
