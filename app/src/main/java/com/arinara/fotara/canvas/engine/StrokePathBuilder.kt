// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.engine

import android.graphics.Path
import com.arinara.fotara.canvas.model.StrokePoint

/**
 * Shared path builder implementing smooth quadratic Bezier curves through segment midpoints.
 * Used by BOTH live in-progress drawing and committed element rendering to guarantee
 * zero shape shift when the touch gesture ends.
 */
object StrokePathBuilder {

    /**
     * Interface for recording or dispatching path commands.
     * Enables pure logic testing on JVM without Android mock.
     */
    interface PathConsumer {
        fun moveTo(x: Float, y: Float)
        fun lineTo(x: Float, y: Float)
        fun quadTo(cx: Float, cy: Float, x: Float, y: Float)
    }

    /**
     * Builds smooth quadratic curves into an Android [Path] with zero allocations.
     */
    fun buildStrokePath(path: Path, points: List<StrokePoint>) {
        path.rewind()
        if (points.isEmpty()) return

        val p0 = points[0]
        path.moveTo(p0.x, p0.y)

        when (points.size) {
            1 -> {
                // Handled as a dot by caller
            }
            2 -> {
                path.lineTo(points[1].x, points[1].y)
            }
            3 -> {
                path.quadTo(points[1].x, points[1].y, points[2].x, points[2].y)
            }
            else -> {
                val p1 = points[1]
                path.lineTo((p0.x + p1.x) / 2f, (p0.y + p1.y) / 2f)

                for (i in 1 until points.size - 1) {
                    val curr = points[i]
                    val next = points[i + 1]
                    val midX = (curr.x + next.x) / 2f
                    val midY = (curr.y + next.y) / 2f
                    path.quadTo(curr.x, curr.y, midX, midY)
                }

                val last = points.last()
                path.lineTo(last.x, last.y)
            }
        }
    }

    /**
     * Dispatches path operations to a [PathConsumer].
     * Exactly mirrors [buildStrokePath] logic for pure JVM unit verification.
     */
    fun buildPathCommands(points: List<StrokePoint>, consumer: PathConsumer) {
        if (points.isEmpty()) return

        val p0 = points[0]
        consumer.moveTo(p0.x, p0.y)

        when (points.size) {
            1 -> {
                // Dot
            }
            2 -> {
                consumer.lineTo(points[1].x, points[1].y)
            }
            3 -> {
                consumer.quadTo(points[1].x, points[1].y, points[2].x, points[2].y)
            }
            else -> {
                val p1 = points[1]
                consumer.lineTo((p0.x + p1.x) / 2f, (p0.y + p1.y) / 2f)

                for (i in 1 until points.size - 1) {
                    val curr = points[i]
                    val next = points[i + 1]
                    val midX = (curr.x + next.x) / 2f
                    val midY = (curr.y + next.y) / 2f
                    consumer.quadTo(curr.x, curr.y, midX, midY)
                }

                val last = points.last()
                consumer.lineTo(last.x, last.y)
            }
        }
    }
}
