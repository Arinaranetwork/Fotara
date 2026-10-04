// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.model

object CanvasLayerNaming {

    /**
     * Finds the smallest positive integer N (starting at 1) where "Layer N" is not
     * currently used by any existing layer name (case-insensitively).
     */
    fun generateNextDefaultLayerName(existingNames: List<String>): String {
        val usedNumbers = mutableSetOf<Int>()
        val regex = Regex("""^Layer\s+(\d+)$""", RegexOption.IGNORE_CASE)
        for (name in existingNames) {
            val match = regex.matchEntire(name.trim())
            if (match != null) {
                match.groupValues[1].toIntOrNull()?.let { usedNumbers.add(it) }
            }
        }
        var n = 1
        while (usedNumbers.contains(n)) {
            n++
        }
        return "Layer $n"
    }
}
