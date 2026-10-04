// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.persistence

import com.arinara.fotara.canvas.engine.CanvasRect
import com.arinara.fotara.canvas.model.StrokeBlendMode
import com.arinara.fotara.canvas.model.StrokeElement
import com.arinara.fotara.canvas.model.StrokePoint
import com.arinara.fotara.canvas.model.StrokeToolType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StrokeCodecTest {

    @Test
    fun testRoundTrip_MultiPointStroke() {
        val points = listOf(
            StrokePoint(10.0f, 20.0f, 0.4f),
            StrokePoint(15.5f, 24.2f, 0.6f),
            StrokePoint(22.1f, 31.8f, 0.8f),
            StrokePoint(35.0f, 50.0f, 1.0f)
        )
        val stroke = StrokeElement(
            id = "test_stroke_1",
            layerId = "layer_1",
            points = points,
            color = 0xFF123456,
            width = 4.5f,
            toolType = StrokeToolType.PEN,
            bounds = CanvasRect(10f, 20f, 35f, 50f),
            zIndex = 3
        )

        val encoded = StrokeCodec.encode(stroke)
        assertTrue(encoded.isNotEmpty())

        val decoded = StrokeCodec.decode(encoded, elementId = stroke.id, layerId = stroke.layerId)
        assertNotNull(decoded)
        assertEquals(stroke.id, decoded!!.id)
        assertEquals(stroke.layerId, decoded.layerId)
        assertEquals(stroke.color, decoded.color)
        assertEquals(stroke.width, decoded.width, 0.001f)
        assertEquals(stroke.toolType, decoded.toolType)
        assertEquals(stroke.zIndex, decoded.zIndex)
        assertEquals(points.size, decoded.points.size)

        // Verify sub-pixel accuracy within 0.06px
        for (i in points.indices) {
            assertEquals(points[i].x, decoded.points[i].x, 0.06f)
            assertEquals(points[i].y, decoded.points[i].y, 0.06f)
            assertEquals(points[i].pressure, decoded.points[i].pressure, 0.05f)
        }
    }

    @Test
    fun testRoundTrip_SinglePointDot() {
        val dotPoint = listOf(StrokePoint(150.0f, 250.0f, 0.75f))
        val dotStroke = StrokeElement(
            id = "dot_1",
            layerId = "layer_1",
            points = dotPoint,
            color = 0xFFAABBCC,
            width = 8.0f,
            toolType = StrokeToolType.HIGHLIGHTER,
            bounds = CanvasRect(146f, 246f, 154f, 254f)
        )

        val encoded = StrokeCodec.encode(dotStroke)
        val decoded = StrokeCodec.decode(encoded, "dot_1", "layer_1")

        assertNotNull(decoded)
        assertEquals(1, decoded!!.points.size)
        assertEquals(150.0f, decoded.points[0].x, 0.06f)
        assertEquals(250.0f, decoded.points[0].y, 0.06f)
        assertEquals(0.75f, decoded.points[0].pressure, 0.05f)
    }

    @Test
    fun testCorruptAndTruncatedBytes_FailSafelyWithoutCrashing() {
        // Null or empty bytes
        assertNull(StrokeCodec.decode(null))
        assertNull(StrokeCodec.decode(ByteArray(0)))

        // Truncated header
        assertNull(StrokeCodec.decode(byteArrayOf(1, 0, 12, 34)))

        // Invalid version byte
        val badVersion = ByteArray(30) { 0 }
        badVersion[0] = 0x99.toByte() // Unsupported version
        assertNull(StrokeCodec.decode(badVersion))

        // Truncated payload: valid header indicating 100 points, but buffer ends immediately
        val truncated = ByteArray(20)
        truncated[0] = StrokeCodec.FORMAT_VERSION_V1
        truncated[1] = 0 // PEN
        // color (8 bytes), width (4 bytes), zIndex (4 bytes)
        truncated[18] = 100.toByte() // Varint pointCount = 100
        assertNull(StrokeCodec.decode(truncated))

        // Giant point count (e.g. 5,000,000 points) to trigger safety bounds check
        val giantPointCountBytes = ByteArray(25)
        giantPointCountBytes[0] = StrokeCodec.FORMAT_VERSION_V1
        giantPointCountBytes[18] = 0xFF.toByte()
        giantPointCountBytes[19] = 0xFF.toByte()
        giantPointCountBytes[20] = 0xFF.toByte()
        giantPointCountBytes[21] = 0x0F.toByte() // ~268 million
        assertNull(StrokeCodec.decode(giantPointCountBytes))
    }

    @Test
    fun testCompressionEfficiency_SignificantlySmallerThanRawFloats() {
        // Simulate a typical 100-point handwriting stroke
        val points = (0 until 100).map { i ->
            StrokePoint(100f + i * 1.5f, 200f + (i % 5) * 2f, 0.5f + (i % 10) * 0.05f)
        }
        val stroke = StrokeElement(
            layerId = "l1",
            points = points,
            color = 0xFFFFFFFF,
            width = 3f,
            bounds = CanvasRect(100f, 200f, 250f, 210f)
        )

        val encoded = StrokeCodec.encode(stroke)

        // Raw uncompressed floats would be: 100 points * (4 bytes X + 4 bytes Y + 4 bytes P) = 1200 bytes
        val rawSize = points.size * 12
        assertTrue("Encoded size (${encoded.size}) should be much smaller than raw float size ($rawSize)", encoded.size < rawSize / 2)
    }

    @Test
    fun testV1Decode_DecodesAsNormalBlendMode_WithGoldenBytes() {
        // Construct authentic V1 binary payload (version 0x01, tool 0 (PEN), color 0xFF00FF00, width 2.0f, zIndex 0, 1 point (100.0, 100.0, 1.0))
        val v1GoldenBytes = byteArrayOf(
            0x01, // Version V1
            0x00, // ToolType.PEN
            0x00, 0x00, 0x00, 0x00, 0xFF.toByte(), 0x00, 0xFF.toByte(), 0x00, // color = 0x00000000FF00FF00L
            0x40, 0x00, 0x00, 0x00, // width = 2.0f (IEEE 754)
            0x00, 0x00, 0x00, 0x00, // zIndex = 0
            0x01, // Point count = 1 (varint)
            0x42, 0xC8.toByte(), 0x00, 0x00, // Base X = 100.0f
            0x42, 0xC8.toByte(), 0x00, 0x00, // Base Y = 100.0f
            0x64 // Base Pressure = 1.0f (encoded as (1.0 * 100) = 100)
        )

        val decoded = StrokeCodec.decode(v1GoldenBytes, elementId = "v1_stroke", layerId = "layer_1")
        assertNotNull(decoded)
        assertEquals("v1_stroke", decoded!!.id)
        assertEquals(StrokeToolType.PEN, decoded.toolType)
        assertEquals(StrokeBlendMode.NORMAL, decoded.blendMode)
        assertEquals(1, decoded.points.size)
        assertEquals(100.0f, decoded.points[0].x, 0.01f)
        assertEquals(100.0f, decoded.points[0].y, 0.01f)
    }

    @Test
    fun testV2RoundTrip_GoldenBytesPerBlendMode() {
        val testModes = listOf(
            StrokeBlendMode.NORMAL to 0.toByte(),
            StrokeBlendMode.MULTIPLY to 1.toByte(),
            StrokeBlendMode.DARKEN to 2.toByte(),
            StrokeBlendMode.SCREEN to 3.toByte()
        )

        for ((mode, expectedByte) in testModes) {
            val stroke = StrokeElement(
                id = "v2_stroke_${mode.name}",
                layerId = "layer_1",
                points = listOf(StrokePoint(50f, 60f, 0.8f)),
                color = 0xFF445566,
                width = 12f,
                toolType = StrokeToolType.HIGHLIGHTER,
                blendMode = mode,
                bounds = CanvasRect(44f, 54f, 56f, 66f),
                zIndex = 1
            )

            val encoded = StrokeCodec.encode(stroke)
            assertTrue(encoded.isNotEmpty())

            // Format Version must be V2 (0x02)
            assertEquals(StrokeCodec.FORMAT_VERSION_V2, encoded[0])

            // BlendMode byte is at index 2 (after version at index 0 and toolType at index 1)
            assertEquals("Blend mode byte mismatch for mode $mode", expectedByte, encoded[2])

            // Decode and verify round trip
            val decoded = StrokeCodec.decode(encoded, stroke.id, stroke.layerId)
            assertNotNull(decoded)
            assertEquals(stroke.id, decoded!!.id)
            assertEquals(StrokeToolType.HIGHLIGHTER, decoded.toolType)
            assertEquals("Decoded blend mode mismatch", mode, decoded.blendMode)
            assertEquals(stroke.color, decoded.color)
            assertEquals(stroke.width, decoded.width, 0.01f)
            assertEquals(1, decoded.points.size)
            assertEquals(50f, decoded.points[0].x, 0.06f)
            assertEquals(60f, decoded.points[0].y, 0.06f)
        }
    }
}
