// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.model

data class AcademicScore(
    val id: Long = 0L,
    val subject: String,
    val title: String,
    val category: String, // "Assignment", "Daily Quiz", "Midterm", "Final Exam"
    val score: Double,
    val maxScore: Double = 100.0,
    val weight: Double = 0.0, // Weight percentage (e.g. 25.0)
    val evidenceUri: String? = null,
    val evidenceName: String? = null,
    val examDateMillis: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val percentage: Double
        get() = if (maxScore > 0.0) (score / maxScore) * 100.0 else 0.0

    companion object {
        val CATEGORIES = listOf("Assignment", "Daily Quiz", "Midterm", "Final Exam")
    }
}

enum class ScoreCalculationMode {
    PURE_AVERAGE,
    CUSTOM_WEIGHTS
}
