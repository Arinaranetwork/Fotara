// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.academic

import com.arinara.fotara.feature.academic.occlusion.OcclusionTape
import com.arinara.fotara.feature.academic.occlusion.OcclusionTapeEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OcclusionTapeEngineTest {

    private lateinit var engine: OcclusionTapeEngine

    @Before
    fun setUp() {
        engine = OcclusionTapeEngine()
    }

    @Test
    fun addAndRemoveTape_modifiesTapeCollection() {
        val tape1 = OcclusionTape(id = "t1", x = 0.1f, y = 0.1f, width = 0.2f, height = 0.05f, label = "Mitochondria")
        val tape2 = OcclusionTape(id = "t2", x = 0.5f, y = 0.5f, width = 0.3f, height = 0.08f, label = "Ribosome")

        engine.addTape(tape1)
        engine.addTape(tape2)

        assertEquals(2, engine.totalCount)
        assertEquals(listOf(tape1, tape2), engine.tapes.value)

        engine.removeTape("t1")
        assertEquals(1, engine.totalCount)
        assertEquals(tape2, engine.tapes.value.first())
    }

    @Test
    fun toggleReveal_invertsRevealedStateForTargetOnly() {
        val tape1 = OcclusionTape(id = "t1", x = 0.1f, y = 0.1f, width = 0.2f, height = 0.05f, isRevealed = false)
        val tape2 = OcclusionTape(id = "t2", x = 0.3f, y = 0.3f, width = 0.2f, height = 0.05f, isRevealed = false)

        engine.setTapes(listOf(tape1, tape2))

        engine.toggleReveal("t1")

        val currentTapes = engine.tapes.value
        assertTrue(currentTapes.first { it.id == "t1" }.isRevealed)
        assertFalse(currentTapes.first { it.id == "t2" }.isRevealed)

        // Toggle back
        engine.toggleReveal("t1")
        assertFalse(engine.tapes.value.first { it.id == "t1" }.isRevealed)
    }

    @Test
    fun revealAllAndHideAll_updatesAllTapesGlobally() {
        val tape1 = OcclusionTape(id = "t1", x = 0.1f, y = 0.1f, width = 0.2f, height = 0.05f, isRevealed = false)
        val tape2 = OcclusionTape(id = "t2", x = 0.3f, y = 0.3f, width = 0.2f, height = 0.05f, isRevealed = true)
        val tape3 = OcclusionTape(id = "t3", x = 0.5f, y = 0.5f, width = 0.2f, height = 0.05f, isRevealed = false)

        engine.setTapes(listOf(tape1, tape2, tape3))

        engine.revealAll()
        assertEquals(3, engine.revealedCount)
        assertTrue(engine.tapes.value.all { it.isRevealed })
        assertEquals(1f, engine.quizProgressFraction, 0.001f)

        engine.hideAll()
        assertEquals(0, engine.revealedCount)
        assertTrue(engine.tapes.value.all { !it.isRevealed })
        assertEquals(0f, engine.quizProgressFraction, 0.001f)
    }

    @Test
    fun quizProgressFraction_computesRatioAccurately() {
        val tape1 = OcclusionTape(id = "t1", x = 0.1f, y = 0.1f, width = 0.2f, height = 0.05f, isRevealed = true)
        val tape2 = OcclusionTape(id = "t2", x = 0.3f, y = 0.3f, width = 0.2f, height = 0.05f, isRevealed = false)
        val tape3 = OcclusionTape(id = "t3", x = 0.5f, y = 0.5f, width = 0.2f, height = 0.05f, isRevealed = false)
        val tape4 = OcclusionTape(id = "t4", x = 0.7f, y = 0.7f, width = 0.2f, height = 0.05f, isRevealed = false)

        engine.setTapes(listOf(tape1, tape2, tape3, tape4))

        assertEquals(1, engine.revealedCount)
        assertEquals(4, engine.totalCount)
        assertEquals(0.25f, engine.quizProgressFraction, 0.001f)
    }

    @Test
    fun findTapeAt_detectsPointHitCorrectly() {
        val tape = OcclusionTape(id = "t1", x = 0.2f, y = 0.3f, width = 0.4f, height = 0.2f)
        engine.addTape(tape)

        // Inside
        val hit = engine.findTapeAt(0.3f, 0.4f)
        assertNotNull(hit)
        assertEquals("t1", hit?.id)

        // Outside (left)
        assertNull(engine.findTapeAt(0.1f, 0.4f))
        // Outside (right)
        assertNull(engine.findTapeAt(0.7f, 0.4f))
        // Outside (top)
        assertNull(engine.findTapeAt(0.3f, 0.2f))
        // Outside (bottom)
        assertNull(engine.findTapeAt(0.3f, 0.6f))
    }

    @Test
    fun updateTape_modifiesExistingTapeProperties() {
        val tape = OcclusionTape(id = "t1", x = 0.1f, y = 0.1f, width = 0.2f, height = 0.1f, label = "Initial")
        engine.addTape(tape)

        val updated = tape.copy(label = "Updated Label", width = 0.3f)
        engine.updateTape(updated)

        assertEquals("Updated Label", engine.tapes.value.first().label)
        assertEquals(0.3f, engine.tapes.value.first().width, 0.001f)
    }
}
