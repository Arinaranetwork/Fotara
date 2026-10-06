// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.coordinator

import com.arinara.fotara.online.ReleaseInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AppDialogRequest {
    val key: String
    val priority: Int

    data class Consent(
        override val key: String = KEY_CONSENT,
        override val priority: Int = PRIORITY_CONSENT
    ) : AppDialogRequest

    data class Update(
        val release: ReleaseInfo,
        val onLater: () -> Unit = {},
        val onSkipVersion: () -> Unit = {},
        override val key: String = KEY_UPDATE,
        override val priority: Int = PRIORITY_UPDATE
    ) : AppDialogRequest

    companion object {
        const val KEY_CONSENT = "CONSENT"
        const val KEY_UPDATE = "UPDATE"

        const val PRIORITY_CONSENT = 100
        const val PRIORITY_UPDATE = 50
    }
}

data class AppDialogState(
    val visibleDialog: AppDialogRequest? = null,
    val queue: List<AppDialogRequest> = emptyList(),
    val isActivityResumed: Boolean = true,
    val isTransitioning: Boolean = false
) {
    /**
     * Whether a dialog should currently be rendered on screen.
     */
    val isDialogRenderable: Boolean
        get() = isActivityResumed && visibleDialog != null && !isTransitioning
}

/**
 * App-wide Dialog Coordinator managing modal prompts across the application.
 * - Deduplicates requests by key
 * - Strictly prioritizes CONSENT (100) > UPDATE (50)
 * - Preempts lower priority dialogs without dismissing them
 * - Preserves 250ms gap between consecutive dialogs
 * - Suppresses presentation when activity is not resumed
 */
class AppDialogCoordinator(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
    private val transitionDelayMs: Long = 250L,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val _state = MutableStateFlow(AppDialogState())
    val state: StateFlow<AppDialogState> = _state.asStateFlow()

    private var transitionJob: Job? = null

    /**
     * Request presentation of an app-level dialog.
     */
    fun requestDialog(request: AppDialogRequest) {
        _state.update { current ->
            // 1. Deduplicate by key: if already visible with same key, update content
            if (current.visibleDialog?.key == request.key) {
                return@update current.copy(visibleDialog = request)
            }

            // Remove any existing queued request with same key
            val filteredQueue = current.queue.filter { it.key != request.key }

            // 2. If no dialog is currently visible and not transitioning, present immediately if resumed
            if (current.visibleDialog == null && !current.isTransitioning) {
                return@update current.copy(
                    visibleDialog = request,
                    queue = filteredQueue
                )
            }

            val currentVisible = current.visibleDialog
            if (currentVisible != null && request.priority > currentVisible.priority) {
                // 3. Higher priority preempts lower priority WITHOUT dismissing it:
                // Preempted dialog is placed back into the queue
                val newQueue = (filteredQueue + currentVisible).sortedByDescending { it.priority }
                return@update current.copy(
                    visibleDialog = request,
                    queue = newQueue
                )
            }

            // 4. Otherwise, queue request in descending priority order
            val newQueue = (filteredQueue + request).sortedByDescending { it.priority }
            current.copy(queue = newQueue)
        }
    }

    /**
     * Dismiss the dialog matching the specified key.
     */
    fun dismissDialog(key: String) {
        transitionJob?.cancel()

        _state.update { current ->
            val isCurrentVisible = current.visibleDialog?.key == key
            val newQueue = current.queue.filter { it.key != key }

            if (isCurrentVisible) {
                current.copy(
                    visibleDialog = null,
                    queue = newQueue,
                    isTransitioning = newQueue.isNotEmpty()
                )
            } else {
                current.copy(queue = newQueue)
            }
        }

        // If there are remaining queued dialogs, schedule next dialog presentation after the transition delay
        if (_state.value.isTransitioning) {
            transitionJob = scope.launch {
                delay(transitionDelayMs)
                _state.update { current ->
                    if (current.queue.isEmpty()) {
                        current.copy(isTransitioning = false)
                    } else {
                        val nextDialog = current.queue.first()
                        val remainingQueue = current.queue.drop(1)
                        current.copy(
                            visibleDialog = nextDialog,
                            queue = remainingQueue,
                            isTransitioning = false
                        )
                    }
                }
            }
        }
    }

    /**
     * Notify coordinator of Activity lifecycle resume / pause state.
     */
    fun setActivityResumed(resumed: Boolean) {
        _state.update { current ->
            current.copy(isActivityResumed = resumed)
        }
        if (resumed && _state.value.visibleDialog == null && _state.value.queue.isNotEmpty() && !_state.value.isTransitioning) {
            // Pick next pending dialog if resumed with an empty active slot
            _state.update { current ->
                val nextDialog = current.queue.firstOrNull() ?: return@update current
                current.copy(
                    visibleDialog = nextDialog,
                    queue = current.queue.drop(1)
                )
            }
        }
    }

    /**
     * Returns true if an app-level dialog is currently active, preventing stacking with system prompts.
     */
    fun hasActiveAppDialog(): Boolean {
        val s = _state.value
        return s.visibleDialog != null || s.isTransitioning
    }

    /**
     * Clear all dialogs (used during test teardown).
     */
    fun clearAll() {
        transitionJob?.cancel()
        _state.value = AppDialogState()
    }
}
