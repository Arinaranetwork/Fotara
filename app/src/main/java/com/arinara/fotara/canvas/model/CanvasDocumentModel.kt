// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.model

import com.arinara.fotara.canvas.engine.CanvasRect
import java.util.UUID

enum class CanvasBackgroundStyle {
    BLANK,
    GRID,
    DOTS,
    RULED
}

enum class StrokeToolType {
    PEN,
    HIGHLIGHTER
}

enum class StrokeBlendMode {
    NORMAL,
    MULTIPLY,
    DARKEN,
    SCREEN
}

/**
 * Individual point within a stroke containing 2D position and normalized stylus/touch pressure.
 */
data class StrokePoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1.0f
)

/**
 * Sealed hierarchy for all infinite canvas elements.
 * Designed for forwards-compatibility: future extensions (shapes, text boxes, frames, link cards)
 * extend CanvasElement without breaking persistence or spatial indexes.
 */
sealed class CanvasElement {
    abstract val id: String
    abstract val layerId: String
    abstract val bounds: CanvasRect
    abstract val zIndex: Int

    abstract fun withLayerId(newLayerId: String): CanvasElement
    abstract fun withZIndex(newZIndex: Int): CanvasElement
    abstract fun translated(dx: Float, dy: Float): CanvasElement
}

/**
 * Freehand drawing stroke with variable pressure, color, width, tool type, and blend mode.
 */
data class StrokeElement(
    override val id: String = UUID.randomUUID().toString(),
    override val layerId: String,
    val points: List<StrokePoint>,
    val color: Long, // ARGB 32-bit packed
    val width: Float,
    val toolType: StrokeToolType = StrokeToolType.PEN,
    val blendMode: StrokeBlendMode = StrokeBlendMode.NORMAL,
    override val bounds: CanvasRect,
    override val zIndex: Int = 0
) : CanvasElement() {

    override fun withLayerId(newLayerId: String): StrokeElement {
        return copy(layerId = newLayerId)
    }

    override fun withZIndex(newZIndex: Int): StrokeElement {
        return copy(zIndex = newZIndex)
    }

    override fun translated(dx: Float, dy: Float): StrokeElement {
        val movedPoints = points.map { it.copy(x = it.x + dx, y = it.y + dy) }
        val movedBounds = CanvasRect(
            left = bounds.left + dx,
            top = bounds.top + dy,
            right = bounds.right + dx,
            bottom = bounds.bottom + dy
        )
        return copy(points = movedPoints, bounds = movedBounds)
    }
}

/**
 * Image element placed on the infinite canvas referencing a managed asset file.
 */
data class ImageElement(
    override val id: String = UUID.randomUUID().toString(),
    override val layerId: String,
    val assetId: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val scale: Float = 1.0f,
    val rotationDegrees: Float = 0.0f,
    override val bounds: CanvasRect,
    override val zIndex: Int = 0
) : CanvasElement() {

    override fun withLayerId(newLayerId: String): ImageElement {
        return copy(layerId = newLayerId)
    }

    override fun withZIndex(newZIndex: Int): ImageElement {
        return copy(zIndex = newZIndex)
    }

    override fun translated(dx: Float, dy: Float): ImageElement {
        val movedBounds = CanvasRect(
            left = bounds.left + dx,
            top = bounds.top + dy,
            right = bounds.right + dx,
            bottom = bounds.bottom + dy
        )
        return copy(x = x + dx, y = y + dy, bounds = movedBounds)
    }
}

/**
 * Canvas layer organizing elements in rendering order.
 */
data class CanvasLayer(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val opacity: Float = 1.0f,
    val order: Int = 0
)

/**
 * Document model holding full state of an infinite study canvas.
 */
data class CanvasDocument(
    val id: Long = 0L,
    val title: String = "Untitled Canvas",
    val backgroundStyle: CanvasBackgroundStyle = CanvasBackgroundStyle.GRID,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val formatVersion: Int = 1,
    val layers: List<CanvasLayer> = listOf(CanvasLayer(id = "layer_default", name = "Layer 1", order = 0)),
    val elements: List<CanvasElement> = emptyList()
) {

    /**
     * Resolves the active layer, defaulting to the top-most visible unlocked layer.
     */
    fun getPrimaryLayerId(): String {
        return layers.sortedByDescending { it.order }.firstOrNull()?.id ?: "layer_default"
    }

    /**
     * Returns all elements belonging to a specific layer.
     */
    fun getElementsForLayer(layerId: String): List<CanvasElement> {
        return elements.filter { it.layerId == layerId }.sortedWith(compareBy<CanvasElement> { it.zIndex }.thenBy { it.id })
    }

    /**
     * Computes the bounding rectangle encompassing all elements in the document.
     */
    fun computeOverallBounds(): CanvasRect {
        if (elements.isEmpty()) return CanvasRect.Empty
        var left = Float.MAX_VALUE
        var top = Float.MAX_VALUE
        var right = -Float.MAX_VALUE
        var bottom = -Float.MAX_VALUE

        for (el in elements) {
            val b = el.bounds
            if (b.left < left) left = b.left
            if (b.top < top) top = b.top
            if (b.right > right) right = b.right
            if (b.bottom > bottom) bottom = b.bottom
        }

        return if (left <= right && top <= bottom) {
            CanvasRect(left, top, right, bottom)
        } else {
            CanvasRect.Empty
        }
    }
}

/**
 * Pure functions for document layer lifecycle management.
 */
object CanvasDocumentOperations {

    fun addLayer(document: CanvasDocument, layerName: String): CanvasDocument {
        val nextOrder = (document.layers.maxOfOrNull { it.order } ?: 0) + 1
        val newLayer = CanvasLayer(
            id = "layer_${UUID.randomUUID()}",
            name = layerName.ifBlank { "Layer ${nextOrder + 1}" },
            order = nextOrder
        )
        return document.copy(
            layers = document.layers + newLayer,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Removes a layer and all elements belonging to it.
     * Returns the updated document and the list of deleted elements (for undo restoration).
     */
    fun removeLayer(
        document: CanvasDocument,
        layerId: String
    ): Pair<CanvasDocument, List<CanvasElement>> {
        if (document.layers.size <= 1) {
            // Cannot remove the only remaining layer
            return Pair(document, emptyList())
        }

        val layerToRemove = document.layers.find { it.id == layerId } ?: return Pair(document, emptyList())
        val deletedElements = document.elements.filter { it.layerId == layerId }
        val remainingElements = document.elements.filter { it.layerId != layerId }
        val remainingLayers = document.layers.filter { it.id != layerId }

        val updatedDoc = document.copy(
            layers = remainingLayers,
            elements = remainingElements,
            updatedAt = System.currentTimeMillis()
        )
        return Pair(updatedDoc, deletedElements)
    }

    fun reorderLayers(document: CanvasDocument, orderedLayerIds: List<String>): CanvasDocument {
        val updatedLayers = document.layers.map { layer ->
            val idx = orderedLayerIds.indexOf(layer.id)
            if (idx != -1) layer.copy(order = idx) else layer
        }.sortedBy { it.order }

        return document.copy(layers = updatedLayers, updatedAt = System.currentTimeMillis())
    }

    fun renameLayer(document: CanvasDocument, layerId: String, newName: String): CanvasDocument {
        val updatedLayers = document.layers.map {
            if (it.id == layerId) it.copy(name = newName.trim().ifBlank { it.name }) else it
        }
        return document.copy(layers = updatedLayers, updatedAt = System.currentTimeMillis())
    }

    fun setLayerVisibility(document: CanvasDocument, layerId: String, isVisible: Boolean): CanvasDocument {
        val updatedLayers = document.layers.map {
            if (it.id == layerId) it.copy(isVisible = isVisible) else it
        }
        return document.copy(layers = updatedLayers, updatedAt = System.currentTimeMillis())
    }

    fun setLayerLocked(document: CanvasDocument, layerId: String, isLocked: Boolean): CanvasDocument {
        val updatedLayers = document.layers.map {
            if (it.id == layerId) it.copy(isLocked = isLocked) else it
        }
        return document.copy(layers = updatedLayers, updatedAt = System.currentTimeMillis())
    }

    fun setLayerOpacity(document: CanvasDocument, layerId: String, opacity: Float): CanvasDocument {
        val clamped = opacity.coerceIn(0.0f, 1.0f)
        val updatedLayers = document.layers.map {
            if (it.id == layerId) it.copy(opacity = clamped) else it
        }
        return document.copy(layers = updatedLayers, updatedAt = System.currentTimeMillis())
    }

    fun moveElementsToLayer(
        document: CanvasDocument,
        elementIds: Set<String>,
        targetLayerId: String
    ): CanvasDocument {
        val layerExists = document.layers.any { it.id == targetLayerId }
        if (!layerExists) return document

        val updatedElements = document.elements.map { element ->
            if (element.id in elementIds) {
                element.withLayerId(targetLayerId)
            } else {
                element
            }
        }
        return document.copy(elements = updatedElements, updatedAt = System.currentTimeMillis())
    }
}
