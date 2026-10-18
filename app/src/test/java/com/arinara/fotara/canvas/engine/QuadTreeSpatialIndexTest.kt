// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.engine

import com.arinara.fotara.canvas.model.StrokeElement
import com.arinara.fotara.canvas.model.StrokePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class QuadTreeSpatialIndexTest {

    private fun createDummyStroke(id: String, left: Float, top: Float, right: Float, bottom: Float): StrokeElement {
        return StrokeElement(
            id = id,
            layerId = "layer_1",
            points = listOf(StrokePoint(left, top), StrokePoint(right, bottom)),
            color = 0xFF000000,
            width = 2.0f,
            bounds = CanvasRect(left, top, right, bottom)
        )
    }

    @Test
    fun testBasicInsertQueryAndRemove() {
        val tree = QuadTreeSpatialIndex(worldBounds = CanvasRect(-1000f, -1000f, 1000f, 1000f))

        val stroke1 = createDummyStroke("s1", 10f, 10f, 50f, 50f)
        val stroke2 = createDummyStroke("s2", 500f, 500f, 600f, 600f)

        tree.insert(stroke1)
        tree.insert(stroke2)
        assertEquals(2, tree.size())

        // Query window overlapping only s1
        val query1 = tree.query(CanvasRect(0f, 0f, 100f, 100f))
        assertEquals(1, query1.size)
        assertEquals("s1", query1[0].id)

        // Query window overlapping only s2
        val query2 = tree.query(CanvasRect(450f, 450f, 700f, 700f))
        assertEquals(1, query2.size)
        assertEquals("s2", query2[0].id)

        // Query window overlapping both
        val queryAll = tree.query(CanvasRect(-100f, -100f, 1000f, 1000f))
        assertEquals(2, queryAll.size)

        // Remove s1
        tree.remove("s1")
        assertEquals(1, tree.size())
        val queryAfterRemove = tree.query(CanvasRect(0f, 0f, 100f, 100f))
        assertTrue(queryAfterRemove.isEmpty())
    }

    @Test
    fun testCorrectnessAgainstBruteForceOnRandomData() {
        val random = Random(42)
        val worldSize = 5000f
        val tree = QuadTreeSpatialIndex(worldBounds = CanvasRect(-worldSize, -worldSize, worldSize, worldSize))

        val allElements = mutableListOf<StrokeElement>()
        val count = 200

        for (i in 0 until count) {
            val cx = (random.nextFloat() * 2 - 1) * 4000f
            val cy = (random.nextFloat() * 2 - 1) * 4000f
            val w = 10f + random.nextFloat() * 200f
            val h = 10f + random.nextFloat() * 200f

            val stroke = createDummyStroke(
                id = "stroke_$i",
                left = cx - w / 2,
                top = cy - h / 2,
                right = cx + w / 2,
                bottom = cy + h / 2
            )
            allElements.add(stroke)
            tree.insert(stroke)
        }

        assertEquals(count, tree.size())

        // Run 20 random query windows
        for (q in 0 until 20) {
            val qx = (random.nextFloat() * 2 - 1) * 3500f
            val qy = (random.nextFloat() * 2 - 1) * 3500f
            val qw = 100f + random.nextFloat() * 800f
            val qh = 100f + random.nextFloat() * 800f
            val queryWindow = CanvasRect(qx - qw / 2, qy - qh / 2, qx + qw / 2, qy + qh / 2)

            // 1. Spatial index query
            val treeResults = tree.query(queryWindow).map { it.id }.toSet()

            // 2. Brute force linear scan
            val bruteForceResults = allElements.filter { it.bounds.intersects(queryWindow) }.map { it.id }.toSet()

            // Both must match 100%
            assertEquals(
                "Mismatch on query window $q ($queryWindow)",
                bruteForceResults,
                treeResults
            )
        }
    }
}
