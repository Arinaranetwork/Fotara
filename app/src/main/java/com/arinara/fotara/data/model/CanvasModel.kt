// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.model

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.UUID

enum class BackgroundStyle {
    BLANK,
    GRID,
    DOTS,
    RULED
}

enum class NibProfile {
    BALLPOINT,
    CHISEL,
    BRUSH
}

enum class CanvasTool {
    SELECT,
    PEN,
    HIGHLIGHTER,
    ERASER
}

enum class EraserMode {
    STROKE,
    AREA
}

data class CanvasPoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1.0f
)

data class CanvasStroke(
    val id: String = UUID.randomUUID().toString(),
    val tool: CanvasTool = CanvasTool.PEN,
    val color: Long = 0xFFFFFFFF, // ARGB Long
    val size: Float = 4.0f,
    val alpha: Float = 1.0f,
    val nibProfile: NibProfile = NibProfile.BALLPOINT,
    val points: List<CanvasPoint> = emptyList(),
    val isLocked: Boolean = false
)

data class CanvasImage(
    val id: String = UUID.randomUUID().toString(),
    val imageUri: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rotation: Float = 0f,
    val isLocked: Boolean = false
)

data class CanvasLayer(
    val id: Int,
    val name: String,
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val opacity: Float = 1.0f,
    val strokes: List<CanvasStroke> = emptyList(),
    val images: List<CanvasImage> = emptyList()
)

data class CanvasDocument(
    val version: Int = 1,
    val layers: List<CanvasLayer> = listOf(
        CanvasLayer(id = 1, name = "Layer 1")
    ),
    val backgroundStyle: BackgroundStyle = BackgroundStyle.GRID
) {
    fun serialize(): ByteArray {
        val baos = ByteArrayOutputStream()
        DataOutputStream(baos).use { out ->
            out.writeInt(MAGIC_HEADER)
            out.writeInt(version)
            out.writeUTF(backgroundStyle.name)
            out.writeInt(layers.size)
            for (layer in layers) {
                out.writeInt(layer.id)
                out.writeUTF(layer.name)
                out.writeBoolean(layer.isVisible)
                out.writeBoolean(layer.isLocked)
                out.writeFloat(layer.opacity)
                out.writeInt(layer.strokes.size)
                for (stroke in layer.strokes) {
                    out.writeUTF(stroke.id)
                    out.writeUTF(stroke.tool.name)
                    out.writeLong(stroke.color)
                    out.writeFloat(stroke.size)
                    out.writeFloat(stroke.alpha)
                    out.writeUTF(stroke.nibProfile.name)
                    out.writeBoolean(stroke.isLocked)
                    out.writeInt(stroke.points.size)
                    for (point in stroke.points) {
                        out.writeFloat(point.x)
                        out.writeFloat(point.y)
                        out.writeFloat(point.pressure)
                    }
                }
                out.writeInt(layer.images.size)
                for (image in layer.images) {
                    out.writeUTF(image.id)
                    out.writeUTF(image.imageUri)
                    out.writeFloat(image.x)
                    out.writeFloat(image.y)
                    out.writeFloat(image.width)
                    out.writeFloat(image.height)
                    out.writeFloat(image.rotation)
                    out.writeBoolean(image.isLocked)
                }
            }
        }
        return baos.toByteArray()
    }

    companion object {
        private const val MAGIC_HEADER = 0x464F5443 // 'FOTC'

        fun deserialize(bytes: ByteArray?): CanvasDocument {
            if (bytes == null || bytes.isEmpty()) {
                return CanvasDocument()
            }
            return try {
                val bais = ByteArrayInputStream(bytes)
                DataInputStream(bais).use { input ->
                    val magic = input.readInt()
                    if (magic != MAGIC_HEADER) {
                        return CanvasDocument()
                    }
                    val version = input.readInt()
                    val bgStyleName = input.readUTF()
                    val bgStyle = try {
                        BackgroundStyle.valueOf(bgStyleName)
                    } catch (_: Exception) {
                        BackgroundStyle.GRID
                    }
                    val layerCount = input.readInt()
                    val layerList = ArrayList<CanvasLayer>(layerCount)
                    for (i in 0 until layerCount) {
                        val layerId = input.readInt()
                        val layerName = input.readUTF()
                        val isVisible = input.readBoolean()
                        val isLocked = input.readBoolean()
                        val opacity = input.readFloat()
                        val strokeCount = input.readInt()
                        val strokeList = ArrayList<CanvasStroke>(strokeCount)
                        for (s in 0 until strokeCount) {
                            val strokeId = input.readUTF()
                            val toolName = input.readUTF()
                            val tool = try { CanvasTool.valueOf(toolName) } catch (_: Exception) { CanvasTool.PEN }
                            val color = input.readLong()
                            val size = input.readFloat()
                            val alpha = input.readFloat()
                            val nibName = input.readUTF()
                            val nib = try { NibProfile.valueOf(nibName) } catch (_: Exception) { NibProfile.BALLPOINT }
                            val strokeLocked = input.readBoolean()
                            val pointCount = input.readInt()
                            val pointList = ArrayList<CanvasPoint>(pointCount)
                            for (p in 0 until pointCount) {
                                val px = input.readFloat()
                                val py = input.readFloat()
                                val pressure = input.readFloat()
                                pointList.add(CanvasPoint(px, py, pressure))
                            }
                            strokeList.add(
                                CanvasStroke(
                                    id = strokeId,
                                    tool = tool,
                                    color = color,
                                    size = size,
                                    alpha = alpha,
                                    nibProfile = nib,
                                    points = pointList,
                                    isLocked = strokeLocked
                                )
                            )
                        }
                        val imageCount = input.readInt()
                        val imageList = ArrayList<CanvasImage>(imageCount)
                        for (imgIdx in 0 until imageCount) {
                            val imgId = input.readUTF()
                            val imgUri = input.readUTF()
                            val ix = input.readFloat()
                            val iy = input.readFloat()
                            val iw = input.readFloat()
                            val ih = input.readFloat()
                            val rot = input.readFloat()
                            val imgLocked = input.readBoolean()
                            imageList.add(
                                CanvasImage(
                                    id = imgId,
                                    imageUri = imgUri,
                                    x = ix,
                                    y = iy,
                                    width = iw,
                                    height = ih,
                                    rotation = rot,
                                    isLocked = imgLocked
                                )
                            )
                        }
                        layerList.add(
                            CanvasLayer(
                                id = layerId,
                                name = layerName,
                                isVisible = isVisible,
                                isLocked = isLocked,
                                opacity = opacity,
                                strokes = strokeList,
                                images = imageList
                            )
                        )
                    }
                    CanvasDocument(
                        version = version,
                        layers = if (layerList.isEmpty()) listOf(CanvasLayer(id = 1, name = "Layer 1")) else layerList,
                        backgroundStyle = bgStyle
                    )
                }
            } catch (_: Exception) {
                CanvasDocument()
            }
        }
    }
}
