// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CropBoxGeometryTest {

    private val delta = 0.001f

    @Test
    fun computeInitialBox_landscapeImage_squareRatio() {
        val box = CropBoxGeometry.computeInitialBox(
            imageWidth = 1000f,
            imageHeight = 600f,
            aspectRatio = 1.0f
        )
        // Image is wider than 1:1, so height fits fully (600), width is 600 centered
        assertEquals(600f, box.height, delta)
        assertEquals(600f, box.width, delta)
        assertEquals(200f, box.left, delta)
        assertEquals(800f, box.right, delta)
        assertEquals(0f, box.top, delta)
        assertEquals(600f, box.bottom, delta)
    }

    @Test
    fun computeInitialBox_portraitImage_squareRatio() {
        val box = CropBoxGeometry.computeInitialBox(
            imageWidth = 600f,
            imageHeight = 1000f,
            aspectRatio = 1.0f
        )
        // Image is taller than 1:1, so width fits fully (600), height is 600 centered
        assertEquals(600f, box.width, delta)
        assertEquals(600f, box.height, delta)
        assertEquals(0f, box.left, delta)
        assertEquals(600f, box.right, delta)
        assertEquals(200f, box.top, delta)
        assertEquals(800f, box.bottom, delta)
    }

    @Test
    fun computeInitialBox_landscapeImage_bannerRatio() {
        val bannerRatio = 16f / 9f // ~1.777f
        val box = CropBoxGeometry.computeInitialBox(
            imageWidth = 1600f,
            imageHeight = 1000f,
            aspectRatio = bannerRatio
        )
        // Image ratio is 1.6, which is taller than 1.777: width fits fully (1600), height = 1600 / 1.777 = 900 centered
        assertEquals(1600f, box.width, delta)
        assertEquals(900f, box.height, delta)
        assertEquals(0f, box.left, delta)
        assertEquals(1600f, box.right, delta)
        assertEquals(50f, box.top, delta)
        assertEquals(950f, box.bottom, delta)
    }

    @Test
    fun pan_movesBoxWithinBounds() {
        val initial = CropRectF(100f, 100f, 400f, 400f)
        val panned = CropBoxGeometry.pan(
            current = initial,
            dx = 50f,
            dy = -30f,
            imageWidth = 1000f,
            imageHeight = 1000f
        )
        assertEquals(150f, panned.left, delta)
        assertEquals(70f, panned.top, delta)
        assertEquals(450f, panned.right, delta)
        assertEquals(370f, panned.bottom, delta)
        assertEquals(300f, panned.width, delta)
        assertEquals(300f, panned.height, delta)
    }

    @Test
    fun pan_clampsAtAllBoundaries() {
        val initial = CropRectF(100f, 100f, 400f, 400f) // size 300x300

        // Clamp Left and Top
        val clampedTopLeft = CropBoxGeometry.pan(
            current = initial,
            dx = -300f,
            dy = -300f,
            imageWidth = 1000f,
            imageHeight = 1000f
        )
        assertEquals(0f, clampedTopLeft.left, delta)
        assertEquals(0f, clampedTopLeft.top, delta)
        assertEquals(300f, clampedTopLeft.right, delta)
        assertEquals(300f, clampedTopLeft.bottom, delta)

        // Clamp Right and Bottom
        val clampedBottomRight = CropBoxGeometry.pan(
            current = initial,
            dx = 1200f,
            dy = 1200f,
            imageWidth = 1000f,
            imageHeight = 1000f
        )
        assertEquals(700f, clampedBottomRight.left, delta)
        assertEquals(700f, clampedBottomRight.top, delta)
        assertEquals(1000f, clampedBottomRight.right, delta)
        assertEquals(1000f, clampedBottomRight.bottom, delta)
    }

    @Test
    fun resizeCorner_bottomRight_keepsTopLeftFixed() {
        val initial = CropRectF(100f, 100f, 400f, 400f)
        val resized = CropBoxGeometry.resizeCorner(
            current = initial,
            corner = CropCorner.BOTTOM_RIGHT,
            touchX = 600f,
            touchY = 600f,
            imageWidth = 1000f,
            imageHeight = 1000f,
            aspectRatio = 1.0f
        )
        // Top-Left must be strictly fixed at (100, 100)
        assertEquals(100f, resized.left, delta)
        assertEquals(100f, resized.top, delta)
        assertEquals(600f, resized.right, delta)
        assertEquals(600f, resized.bottom, delta)
        assertEquals(500f, resized.width, delta)
        assertEquals(500f, resized.height, delta)
    }

    @Test
    fun resizeCorner_topLeft_keepsBottomRightFixed() {
        val initial = CropRectF(200f, 200f, 600f, 600f)
        val resized = CropBoxGeometry.resizeCorner(
            current = initial,
            corner = CropCorner.TOP_LEFT,
            touchX = 100f,
            touchY = 100f,
            imageWidth = 1000f,
            imageHeight = 1000f,
            aspectRatio = 1.0f
        )
        // Bottom-Right must be strictly fixed at (600, 600)
        assertEquals(600f, resized.right, delta)
        assertEquals(600f, resized.bottom, delta)
        assertEquals(100f, resized.left, delta)
        assertEquals(100f, resized.top, delta)
    }

    @Test
    fun resizeCorner_topRight_keepsBottomLeftFixed() {
        val initial = CropRectF(200f, 200f, 600f, 600f)
        val resized = CropBoxGeometry.resizeCorner(
            current = initial,
            corner = CropCorner.TOP_RIGHT,
            touchX = 700f,
            touchY = 100f,
            imageWidth = 1000f,
            imageHeight = 1000f,
            aspectRatio = 1.0f
        )
        // Bottom-Left must be strictly fixed at (200, 600)
        assertEquals(200f, resized.left, delta)
        assertEquals(600f, resized.bottom, delta)
        assertEquals(700f, resized.right, delta)
        assertEquals(100f, resized.top, delta)
    }

    @Test
    fun resizeCorner_bottomLeft_keepsTopRightFixed() {
        val initial = CropRectF(200f, 200f, 600f, 600f)
        val resized = CropBoxGeometry.resizeCorner(
            current = initial,
            corner = CropCorner.BOTTOM_LEFT,
            touchX = 100f,
            touchY = 700f,
            imageWidth = 1000f,
            imageHeight = 1000f,
            aspectRatio = 1.0f
        )
        // Top-Right must be strictly fixed at (600, 200)
        assertEquals(600f, resized.right, delta)
        assertEquals(200f, resized.top, delta)
        assertEquals(100f, resized.left, delta)
        assertEquals(700f, resized.bottom, delta)
    }

    @Test
    fun resizeCorner_clampsToMinSize() {
        val initial = CropRectF(100f, 100f, 400f, 400f)
        val resized = CropBoxGeometry.resizeCorner(
            current = initial,
            corner = CropCorner.BOTTOM_RIGHT,
            touchX = 110f, // very small
            touchY = 110f,
            imageWidth = 1000f,
            imageHeight = 1000f,
            aspectRatio = 1.0f,
            minWidth = 64f
        )
        assertEquals(100f, resized.left, delta)
        assertEquals(100f, resized.top, delta)
        assertEquals(64f, resized.width, delta)
        assertEquals(64f, resized.height, delta)
    }

    @Test
    fun pinchScale_scalesAroundCenterAndClamps() {
        val initial = CropRectF(200f, 200f, 400f, 400f) // center at (300, 300), size 200x200
        val scaledUp = CropBoxGeometry.pinchScale(
            current = initial,
            scaleFactor = 1.5f,
            imageWidth = 1000f,
            imageHeight = 1000f,
            aspectRatio = 1.0f
        )
        assertEquals(300f, scaledUp.centerX, delta)
        assertEquals(300f, scaledUp.centerY, delta)
        assertEquals(300f, scaledUp.width, delta)
        assertEquals(300f, scaledUp.height, delta)
        assertEquals(150f, scaledUp.left, delta)
        assertEquals(150f, scaledUp.top, delta)

        // Pinch scale clamped to boundaries
        val scaledMax = CropBoxGeometry.pinchScale(
            current = initial,
            scaleFactor = 10.0f,
            imageWidth = 1000f,
            imageHeight = 1000f,
            aspectRatio = 1.0f
        )
        // Center is (300, 300), distance to nearest edge (0,0) is 300, so max width = 600
        assertEquals(600f, scaledMax.width, delta)
        assertEquals(0f, scaledMax.left, delta)
        assertEquals(0f, scaledMax.top, delta)
    }

    @Test
    fun mapDisplayToSourceRect_0Degrees() {
        val displayCrop = CropRectF(100f, 200f, 500f, 600f)
        val src = CropBoxGeometry.mapDisplayToSourceRect(
            displayCrop = displayCrop,
            displayWidth = 1000f,
            displayHeight = 1000f,
            rawSourceWidth = 4000,
            rawSourceHeight = 4000,
            rotationDegrees = 0
        )
        assertEquals(400, src.left)
        assertEquals(800, src.top)
        assertEquals(2000, src.right)
        assertEquals(2400, src.bottom)
    }

    @Test
    fun mapDisplayToSourceRect_90Degrees() {
        // Raw: 4000x3000. Upright display: 3000x4000.
        // Crop top-left quadrant of display (0..1500, 0..2000)
        val displayCrop = CropRectF(0f, 0f, 1500f, 2000f)
        val src = CropBoxGeometry.mapDisplayToSourceRect(
            displayCrop = displayCrop,
            displayWidth = 3000f,
            displayHeight = 4000f,
            rawSourceWidth = 4000,
            rawSourceHeight = 3000,
            rotationDegrees = 90
        )
        // Display u in [0, 0.5], v in [0, 0.5]
        // Under 90 deg: rawLeft = v0*4000=0, rawRight = v1*4000=2000
        // rawTop = (1-u1)*3000 = 1500, rawBottom = (1-u0)*3000 = 3000
        assertEquals(0, src.left)
        assertEquals(1500, src.top)
        assertEquals(2000, src.right)
        assertEquals(3000, src.bottom)
    }

    @Test
    fun mapDisplayToSourceRect_180Degrees() {
        val displayCrop = CropRectF(0f, 0f, 500f, 500f) // top-left quadrant
        val src = CropBoxGeometry.mapDisplayToSourceRect(
            displayCrop = displayCrop,
            displayWidth = 1000f,
            displayHeight = 1000f,
            rawSourceWidth = 2000,
            rawSourceHeight = 2000,
            rotationDegrees = 180
        )
        // 180 deg flips both: top-left quadrant maps to bottom-right quadrant of raw image
        assertEquals(1000, src.left)
        assertEquals(1000, src.top)
        assertEquals(2000, src.right)
        assertEquals(2000, src.bottom)
    }

    @Test
    fun mapDisplayToSourceRect_270Degrees() {
        // Raw: 4000x3000. Upright display: 3000x4000.
        // Crop top-left quadrant of display (0..1500, 0..2000)
        val displayCrop = CropRectF(0f, 0f, 1500f, 2000f)
        val src = CropBoxGeometry.mapDisplayToSourceRect(
            displayCrop = displayCrop,
            displayWidth = 3000f,
            displayHeight = 4000f,
            rawSourceWidth = 4000,
            rawSourceHeight = 3000,
            rotationDegrees = 270
        )
        // Display u in [0, 0.5], v in [0, 0.5]
        // Under 270 deg: rawLeft = (1 - v1) * 4000 = 2000, rawRight = (1 - v0) * 4000 = 4000
        // rawTop = u0 * 3000 = 0, rawBottom = u1 * 3000 = 1500
        assertEquals(2000, src.left)
        assertEquals(0, src.top)
        assertEquals(4000, src.right)
        assertEquals(1500, src.bottom)
    }
}
