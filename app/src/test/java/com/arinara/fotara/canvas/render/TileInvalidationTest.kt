// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.render

import com.arinara.fotara.canvas.engine.CanvasRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TileInvalidationTest {

    @Test
    fun testZoomTierDiscretization() {
        assertEquals(0, TileGridHelper.getZoomTier(0.05f))
        assertEquals(0, TileGridHelper.getZoomTier(0.24f))
        assertEquals(1, TileGridHelper.getZoomTier(0.25f))
        assertEquals(1, TileGridHelper.getZoomTier(0.74f))
        assertEquals(2, TileGridHelper.getZoomTier(0.75f))
        assertEquals(2, TileGridHelper.getZoomTier(1.0f))
        assertEquals(2, TileGridHelper.getZoomTier(1.49f))
        assertEquals(3, TileGridHelper.getZoomTier(1.5f))
        assertEquals(3, TileGridHelper.getZoomTier(2.99f))
        assertEquals(4, TileGridHelper.getZoomTier(3.0f))
        assertEquals(4, TileGridHelper.getZoomTier(50.0f))
    }

    @Test
    fun testComputeTileBounds() {
        val bounds = TileGridHelper.computeTileBounds(col = 2, row = 3, worldTileSize = 500f)
        assertEquals(1000f, bounds.left, 0.001f)
        assertEquals(1500f, bounds.top, 0.001f)
        assertEquals(1500f, bounds.right, 0.001f)
        assertEquals(2000f, bounds.bottom, 0.001f)
        assertEquals(500f, bounds.width, 0.001f)
        assertEquals(500f, bounds.height, 0.001f)
    }

    @Test
    fun testComputeIntersectingTileKeys() {
        val dirtyRect = CanvasRect(left = 120f, top = 250f, right = 600f, bottom = 800f)
        val worldTileSize = 256f
        val zoomTier = 2

        val keys = TileGridHelper.computeIntersectingTileKeys(dirtyRect, worldTileSize, zoomTier)
        // startCol = floor(120 / 256) = 0, endCol = ceil(600 / 256) = 3 -> cols 0, 1, 2, 3
        // startRow = floor(250 / 256) = 0, endRow = ceil(800 / 256) = 4 -> rows 0, 1, 2, 3, 4
        // total keys = 4 * 5 = 20
        assertEquals(20, keys.size)
        assertTrue(keys.contains(TileKey(0, 0, zoomTier)))
        assertTrue(keys.contains(TileKey(3, 4, zoomTier)))
        assertFalse(keys.contains(TileKey(4, 4, zoomTier)))
    }

    @Test
    fun testRegionalTileInvalidation_MarksOnlyIntersectingTilesDirty() {
        val cacheManager = TileCacheManager()

        val tile1 = CachedTile(
            key = TileKey(0, 0, 2),
            worldBounds = CanvasRect(0f, 0f, 500f, 500f),
            isDirty = false
        )
        val tile2 = CachedTile(
            key = TileKey(1, 0, 2),
            worldBounds = CanvasRect(500f, 0f, 1000f, 500f),
            isDirty = false
        )
        val tile3 = CachedTile(
            key = TileKey(5, 5, 2),
            worldBounds = CanvasRect(2500f, 2500f, 3000f, 3000f),
            isDirty = false
        )

        cacheManager.putTileForTesting(tile1.key, tile1)
        cacheManager.putTileForTesting(tile2.key, tile2)
        cacheManager.putTileForTesting(tile3.key, tile3)

        // Dirty rect only intersects tile1 and tile2 at border (400, 100, 600, 300)
        val dirtyRect = CanvasRect(400f, 100f, 600f, 300f)
        cacheManager.invalidateRegion(dirtyRect)

        assertTrue("tile1 should be marked dirty", tile1.isDirty)
        assertTrue("tile2 should be marked dirty", tile2.isDirty)
        assertFalse("tile3 must NOT be marked dirty", tile3.isDirty)
    }

    @Test
    fun testInvalidateAll_MarksAllTilesDirty() {
        val cacheManager = TileCacheManager()

        val tile1 = CachedTile(
            key = TileKey(0, 0, 1),
            worldBounds = CanvasRect(0f, 0f, 500f, 500f),
            isDirty = false
        )
        val tile2 = CachedTile(
            key = TileKey(1, 1, 1),
            worldBounds = CanvasRect(500f, 500f, 1000f, 1000f),
            isDirty = false
        )

        cacheManager.putTileForTesting(tile1.key, tile1)
        cacheManager.putTileForTesting(tile2.key, tile2)

        cacheManager.invalidateAll()

        assertTrue(tile1.isDirty)
        assertTrue(tile2.isDirty)
    }
}
