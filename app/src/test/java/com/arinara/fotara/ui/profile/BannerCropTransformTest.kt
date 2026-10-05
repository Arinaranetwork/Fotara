// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BannerCropTransformTest {

    @Test
    fun serialization_roundTrip() {
        val original = NormalizedCropRect(0.1f, 0.2f, 0.8f, 0.9f)
        val serialized = original.toSerializedString()
        val parsed = NormalizedCropRect.fromSerializedString(serialized)
        assertNotNull(parsed)
        assertEquals(original.left, parsed!!.left, 0.0001f)
        assertEquals(original.top, parsed.top, 0.0001f)
        assertEquals(original.right, parsed.right, 0.0001f)
        assertEquals(original.bottom, parsed.bottom, 0.0001f)
    }

    @Test
    fun serialization_invalidReturnsNull() {
        assertNull(NormalizedCropRect.fromSerializedString(null))
        assertNull(NormalizedCropRect.fromSerializedString(""))
        assertNull(NormalizedCropRect.fromSerializedString("0,0,1"))
        assertNull(NormalizedCropRect.fromSerializedString("a,b,c,d"))
    }

    @Test
    fun computeTransformParams_fullImageAspectFill() {
        // Image 1000x500, View 500x500. Aspect ratio requires scale=1.0 on height (500/500=1), width scaled to 1000.
        // Scale = max(500/1000, 500/500) = 1.0f
        // Center X = 500. tx = 250 - 500*1 = -250.
        // Center Y = 250. ty = 250 - 250*1 = 0.
        val params = BannerCropTransform.computeTransformParams(
            viewWidth = 500f,
            viewHeight = 500f,
            imageWidth = 1000f,
            imageHeight = 500f,
            crop = NormalizedCropRect.FULL
        )
        assertEquals(1.0f, params.scale, 0.0001f)
        assertEquals(-250f, params.tx, 0.0001f)
        assertEquals(0f, params.ty, 0.0001f)
    }

    @Test
    fun computeTransformParams_subRegionZoomsAndCenters() {
        // Image 1000x1000, View 500x500. Crop center 0.25..0.75 in both dims (500x500 crop).
        // cropW = 500, cropH = 500. Scale = max(500/500, 500/500) = 1.0f.
        // cropCenterX = 0.5 * 1000 = 500. tx = 250 - 500 * 1 = -250.
        val crop = NormalizedCropRect(0.25f, 0.25f, 0.75f, 0.75f)
        val params = BannerCropTransform.computeTransformParams(
            viewWidth = 500f,
            viewHeight = 500f,
            imageWidth = 1000f,
            imageHeight = 1000f,
            crop = crop
        )
        assertEquals(1.0f, params.scale, 0.0001f)
        assertEquals(-250f, params.tx, 0.0001f)
        assertEquals(-250f, params.ty, 0.0001f)
    }
}
