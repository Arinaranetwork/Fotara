// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.engine

import com.arinara.fotara.canvas.model.StrokePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StrokePathBuilderTest {

    private class RecordingPathConsumer : StrokePathBuilder.PathConsumer {
        val operations = mutableListOf<String>()

        override fun moveTo(x: Float, y: Float) {
            operations.add("moveTo($x, $y)")
        }

        override fun lineTo(x: Float, y: Float) {
            operations.add("lineTo($x, $y)")
        }

        override fun quadTo(cx: Float, cy: Float, x: Float, y: Float) {
            operations.add("quadTo($cx, $cy, $x, $y)")
        }
    }

    @Test
    fun testEmptyPoints_NoOperations() {
        val consumer = RecordingPathConsumer()
        StrokePathBuilder.buildPathCommands(emptyList(), consumer)
        assertTrue(consumer.operations.isEmpty())
    }

    @Test
    fun testSinglePointDot_MovesToPoint() {
        val consumer = RecordingPathConsumer()
        val points = listOf(StrokePoint(50f, 60f, 1f))
        StrokePathBuilder.buildPathCommands(points, consumer)

        assertEquals(1, consumer.operations.size)
        assertEquals("moveTo(50.0, 60.0)", consumer.operations[0])
    }

    @Test
    fun testTwoPoints_MovesAndDrawsLine() {
        val consumer = RecordingPathConsumer()
        val points = listOf(
            StrokePoint(10f, 10f, 1f),
            StrokePoint(20f, 20f, 1f)
        )
        StrokePathBuilder.buildPathCommands(points, consumer)

        assertEquals(2, consumer.operations.size)
        assertEquals("moveTo(10.0, 10.0)", consumer.operations[0])
        assertEquals("lineTo(20.0, 20.0)", consumer.operations[1])
    }

    @Test
    fun testThreePoints_SmoothQuadraticCurve() {
        val consumer = RecordingPathConsumer()
        val points = listOf(
            StrokePoint(0f, 0f, 1f),
            StrokePoint(10f, 20f, 1f),
            StrokePoint(20f, 0f, 1f)
        )
        StrokePathBuilder.buildPathCommands(points, consumer)

        assertEquals(2, consumer.operations.size)
        assertEquals("moveTo(0.0, 0.0)", consumer.operations[0])
        assertEquals("quadTo(10.0, 20.0, 20.0, 0.0)", consumer.operations[1])
    }

    @Test
    fun testManyPoints_SmoothMidpointQuadraticChains() {
        val consumer = RecordingPathConsumer()
        val points = listOf(
            StrokePoint(0f, 0f, 1f),
            StrokePoint(10f, 10f, 1f),
            StrokePoint(20f, 15f, 1f),
            StrokePoint(30f, 10f, 1f),
            StrokePoint(40f, 0f, 1f)
        )
        StrokePathBuilder.buildPathCommands(points, consumer)

        assertTrue(consumer.operations.size >= 4)
        assertEquals("moveTo(0.0, 0.0)", consumer.operations[0])
        assertTrue(consumer.operations[1].startsWith("lineTo"))
        assertTrue(consumer.operations[2].startsWith("quadTo"))
        assertEquals("lineTo(40.0, 0.0)", consumer.operations.last())
    }
}
