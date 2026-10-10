// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.academic.occlusion

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Geometric occlusion tape masking a diagram, formula, or text region for active recall study.
 * Coordinates (x, y, width, height) are normalized fractions of the study viewport [0.0..1.0].
 *
 * @property id Unique identifier for the occlusion tape slice.
 * @property x Top-left horizontal coordinate [0.0..1.0].
 * @property y Top-left vertical coordinate [0.0..1.0].
 * @property width Tape bounding box width [0.0..1.0].
 * @property height Tape bounding box height [0.0..1.0].
 * @property label Optional mnemonic or answer label revealed when tape is lifted.
 * @property isRevealed Whether the tape has been lifted during quiz review.
 */
data class OcclusionTape(
    val id: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val label: String = "",
    val isRevealed: Boolean = false
) {
    /**
     * Checks if a point in normalized coordinates falls within this tape's bounds.
     */
    fun contains(px: Float, py: Float): Boolean {
        return px in x..(x + width) && py in y..(y + height)
    }
}

/**
 * Pure state holder managing the creation, deletion, reveal state, and quiz progress of occlusion tapes.
 */
class OcclusionTapeEngine(
    initialTapes: List<OcclusionTape> = emptyList()
) {
    private val _tapes = MutableStateFlow(initialTapes)
    val tapes: StateFlow<List<OcclusionTape>> = _tapes.asStateFlow()

    /**
     * Appends a new occlusion tape region.
     */
    fun addTape(tape: OcclusionTape) {
        _tapes.update { current ->
            current + tape
        }
    }

    /**
     * Removes an occlusion tape by its ID.
     */
    fun removeTape(id: String) {
        _tapes.update { current ->
            current.filterNot { it.id == id }
        }
    }

    /**
     * Toggles the revealed state of a specific tape on user tap.
     */
    fun toggleReveal(id: String) {
        _tapes.update { current ->
            current.map { tape ->
                if (tape.id == id) {
                    tape.copy(isRevealed = !tape.isRevealed)
                } else {
                    tape
                }
            }
        }
    }

    /**
     * Reveals all occlusion tapes (useful for post-quiz review).
     */
    fun revealAll() {
        _tapes.update { current ->
            current.map { it.copy(isRevealed = true) }
        }
    }

    /**
     * Hides all occlusion tapes to restart active recall testing.
     */
    fun hideAll() {
        _tapes.update { current ->
            current.map { it.copy(isRevealed = false) }
        }
    }

    /**
     * Updates an existing tape's parameters or bounds.
     */
    fun updateTape(tape: OcclusionTape) {
        _tapes.update { current ->
            current.map { if (it.id == tape.id) tape else it }
        }
    }

    /**
     * Replaces the entire active tape set.
     */
    fun setTapes(newTapes: List<OcclusionTape>) {
        _tapes.value = newTapes
    }

    /**
     * Clears all occlusion tapes.
     */
    fun clearAll() {
        _tapes.value = emptyList()
    }

    /**
     * Returns the count of revealed tapes.
     */
    val revealedCount: Int
        get() = _tapes.value.count { it.isRevealed }

    /**
     * Returns the total count of occlusion tapes.
     */
    val totalCount: Int
        get() = _tapes.value.size

    /**
     * Returns the fraction [0.0..1.0] of tapes that have been tested/revealed.
     */
    val quizProgressFraction: Float
        get() {
            val total = totalCount
            return if (total == 0) 1f else revealedCount.toFloat() / total
        }

    /**
     * Finds the topmost tape covering the specified normalized point.
     */
    fun findTapeAt(px: Float, py: Float): OcclusionTape? {
        return _tapes.value.lastOrNull { it.contains(px, py) }
    }
}
