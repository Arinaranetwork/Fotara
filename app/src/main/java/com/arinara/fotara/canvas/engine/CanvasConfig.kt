// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.engine

/**
 * Centralized configuration constants for Canvas Note drawing mode.
 * Establishes safe boundaries, layer capacities, memory budgets, and image limits.
 */
object CanvasConfig {

    /** Total logical width and height extent of the 2D canvas in world units (20,000 x 20,000 px). */
    const val CANVAS_EXTENT_WIDTH: Float = 20_000f
    const val CANVAS_EXTENT_HEIGHT: Float = 20_000f

    /** World coordinate boundaries centered at the origin (0, 0). */
    const val WORLD_MIN_X: Float = -CANVAS_EXTENT_WIDTH / 2f   // -10,000f
    const val WORLD_MAX_X: Float = CANVAS_EXTENT_WIDTH / 2f    // +10,000f
    const val WORLD_MIN_Y: Float = -CANVAS_EXTENT_HEIGHT / 2f  // -10,000f
    const val WORLD_MAX_Y: Float = CANVAS_EXTENT_HEIGHT / 2f   // +10,000f

    /** Maximum allowed layers per canvas note document. */
    const val MAX_LAYERS: Int = 50

    /** Maximum dimension in pixels (width or height) for imported image assets. */
    const val MAX_IMAGE_DIMENSION: Int = 2048

    /** Maximum total canvas elements (strokes + images) allowed in a single canvas note. */
    const val MAX_TOTAL_ELEMENTS: Int = 10_000

    /** Maximum points allowed per individual stroke to prevent memory runaways. */
    const val MAX_POINTS_PER_STROKE: Int = 2_000

    /** Minimum and maximum allowed zoom levels. */
    const val MIN_ZOOM: Float = 0.05f   // 5% zoom
    const val MAX_ZOOM: Float = 50.0f   // 5000% zoom
}
