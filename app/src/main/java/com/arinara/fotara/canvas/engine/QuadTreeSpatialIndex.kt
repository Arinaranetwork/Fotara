// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.engine

import com.arinara.fotara.canvas.model.CanvasElement

/**
 * High-performance 2D Quadtree spatial index for viewport culling and fast hit testing.
 * Enables O(log N + K) spatial queries across thousands of strokes and images.
 */
class QuadTreeSpatialIndex(
    private val worldBounds: CanvasRect = CanvasRect(-100000f, -100000f, 100000f, 100000f),
    private val maxElementsPerNode: Int = 16,
    private val maxDepth: Int = 8
) {

    private class Node(
        val bounds: CanvasRect,
        val depth: Int
    ) {
        val elements = mutableListOf<CanvasElement>()
        var children: Array<Node>? = null

        val isLeaf: Boolean get() = children == null

        fun subdivide() {
            val midX = bounds.centerX
            val midY = bounds.centerY

            val nw = CanvasRect(bounds.left, bounds.top, midX, midY)
            val ne = CanvasRect(midX, bounds.top, bounds.right, midY)
            val sw = CanvasRect(bounds.left, midY, midX, bounds.bottom)
            val se = CanvasRect(midX, midY, bounds.right, bounds.bottom)

            children = arrayOf(
                Node(nw, depth + 1),
                Node(ne, depth + 1),
                Node(sw, depth + 1),
                Node(se, depth + 1)
            )
        }
    }

    private val root = Node(worldBounds, 0)
    private val elementMap = mutableMapOf<String, CanvasElement>()

    fun size(): Int = elementMap.size

    /**
     * Inserts an element into the spatial index.
     */
    fun insert(element: CanvasElement) {
        elementMap[element.id] = element
        insertRecursive(root, element)
    }

    /**
     * Inserts a collection of elements in batch.
     */
    fun insertAll(elements: Collection<CanvasElement>) {
        elements.forEach { insert(it) }
    }

    private fun insertRecursive(node: Node, element: CanvasElement) {
        if (!node.bounds.intersects(element.bounds)) {
            return
        }

        if (node.isLeaf) {
            node.elements.add(element)
            if (node.elements.size > maxElementsPerNode && node.depth < maxDepth) {
                node.subdivide()
                val existing = ArrayList(node.elements)
                node.elements.clear()
                for (item in existing) {
                    for (child in node.children!!) {
                        insertRecursive(child, item)
                    }
                }
            }
        } else {
            for (child in node.children!!) {
                insertRecursive(child, element)
            }
        }
    }

    /**
     * Removes an element from the index by its stable identifier.
     */
    fun remove(elementId: String): Boolean {
        val element = elementMap.remove(elementId) ?: return false
        return removeRecursive(root, elementId, element.bounds)
    }

    private fun removeRecursive(node: Node, elementId: String, bounds: CanvasRect): Boolean {
        if (!node.bounds.intersects(bounds)) {
            return false
        }

        if (node.isLeaf) {
            return node.elements.removeAll { it.id == elementId }
        }

        var anyRemoved = false
        node.children?.forEach { child ->
            if (removeRecursive(child, elementId, bounds)) {
                anyRemoved = true
            }
        }
        return anyRemoved
    }

    /**
     * Updates an element's position/bounds in the index.
     */
    fun update(element: CanvasElement) {
        remove(element.id)
        insert(element)
    }

    /**
     * Queries for all elements intersecting the given search boundary (e.g. visible viewport).
     * Returns a deduplicated list of candidates.
     */
    fun query(searchBounds: CanvasRect): List<CanvasElement> {
        val resultSet = LinkedHashSet<CanvasElement>()
        queryRecursive(root, searchBounds, resultSet)
        return resultSet.toList()
    }

    private fun queryRecursive(
        node: Node,
        searchBounds: CanvasRect,
        results: MutableSet<CanvasElement>
    ) {
        if (!node.bounds.intersects(searchBounds)) {
            return
        }

        if (node.isLeaf) {
            for (el in node.elements) {
                if (el.bounds.intersects(searchBounds)) {
                    results.add(el)
                }
            }
        } else {
            node.children?.forEach { child ->
                queryRecursive(child, searchBounds, results)
            }
        }
    }

    /**
     * Clears all elements from the spatial index.
     */
    fun clear() {
        elementMap.clear()
        root.elements.clear()
        root.children = null
    }

    /**
     * Rebuilds the index completely from a list of elements.
     */
    fun rebuild(elements: List<CanvasElement>) {
        clear()
        insertAll(elements)
    }
}
