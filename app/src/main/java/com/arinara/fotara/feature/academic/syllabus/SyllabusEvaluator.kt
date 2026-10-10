// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.academic.syllabus

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Represents a graded component within an academic course syllabus.
 *
 * @property id Unique identifier for the syllabus component.
 * @property name Component title (e.g., "Midterm Examination", "Homework & Labs").
 * @property weightPercent Percentage of total course grade contributed by this component (0..100).
 * @property currentScore Current score earned by student, or null if pending/uncompleted.
 * @property maxScore Maximum attainable score for this component (default: 100.0).
 */
data class SyllabusComponent(
    val id: String,
    val name: String,
    val weightPercent: Float,
    val currentScore: Float?,
    val maxScore: Float = 100f
)

/**
 * Projected final examination performance required to achieve a target letter grade.
 *
 * @property letterGrade Target letter grade (e.g., "A", "B", "C", "D").
 * @property targetPercent Required cumulative course percentage for this grade.
 * @property requiredFinalExamScore Score out of 100 needed on the final exam.
 * @property isAchievable Whether the required score is attainable (<= 100%).
 */
data class TargetGradeProjection(
    val letterGrade: String,
    val targetPercent: Float,
    val requiredFinalExamScore: Float,
    val isAchievable: Boolean
)

/**
 * Pure evaluator engine computing course grade projections and required final exam benchmarks.
 */
class SyllabusEvaluator {

    companion object {
        val STANDARD_TARGET_GRADES = listOf(
            "A" to 90f,
            "B" to 80f,
            "C" to 70f,
            "D" to 60f
        )
    }

    /**
     * Computes the cumulative weighted points currently earned across completed components.
     */
    fun calculateTotalEarnedPoints(components: List<SyllabusComponent>): Float {
        var earned = 0f
        for (component in components) {
            val score = component.currentScore
            if (score != null && component.maxScore > 0f) {
                earned += (score / component.maxScore) * component.weightPercent
            }
        }
        return earned
    }

    /**
     * Computes the total weight percentage allocated to completed components.
     */
    fun calculateCompletedWeight(components: List<SyllabusComponent>): Float {
        return components.filter { it.currentScore != null }.sumOf { it.weightPercent.toDouble() }.toFloat()
    }

    /**
     * Computes the remaining uncompleted weight percentage in the syllabus.
     */
    fun calculateRemainingWeight(components: List<SyllabusComponent>): Float {
        val unscoredWeight = components.filter { it.currentScore == null }.sumOf { it.weightPercent.toDouble() }.toFloat()
        return if (unscoredWeight > 0f) {
            unscoredWeight
        } else {
            max(0f, 100f - calculateCompletedWeight(components))
        }
    }

    /**
     * Computes current standing percentage normalized to completed components only.
     * Returns 0.0 if no components have been completed yet.
     */
    fun calculateCurrentStandingAverage(components: List<SyllabusComponent>): Float {
        val completedWeight = calculateCompletedWeight(components)
        if (completedWeight <= 0f) return 0f
        val earned = calculateTotalEarnedPoints(components)
        return (earned / completedWeight) * 100f
    }

    /**
     * Calculates required final exam score for a specific target course percentage.
     *
     * @param components Syllabus components list.
     * @param targetPercent Target course percentage (e.g., 90.0 for Grade A).
     * @param overrideFinalWeight Optional manual final exam weight; if omitted, deduced from uncompleted weight.
     */
    fun computeRequiredScoreForTarget(
        components: List<SyllabusComponent>,
        targetPercent: Float,
        letterGrade: String = getLetterGrade(targetPercent),
        overrideFinalWeight: Float? = null
    ): TargetGradeProjection {
        val earnedPoints = calculateTotalEarnedPoints(components)
        val remainingWeight = overrideFinalWeight ?: calculateRemainingWeight(components)

        val neededPoints = targetPercent - earnedPoints

        return when {
            neededPoints <= 0f -> {
                // Target already locked in with completed assignments
                TargetGradeProjection(
                    letterGrade = letterGrade,
                    targetPercent = targetPercent,
                    requiredFinalExamScore = 0f,
                    isAchievable = true
                )
            }
            remainingWeight <= 0f -> {
                // No remaining points available to bridge the gap
                TargetGradeProjection(
                    letterGrade = letterGrade,
                    targetPercent = targetPercent,
                    requiredFinalExamScore = Float.POSITIVE_INFINITY,
                    isAchievable = false
                )
            }
            else -> {
                val requiredScore = (neededPoints / remainingWeight) * 100f
                val isAchievable = requiredScore <= 100.001f
                TargetGradeProjection(
                    letterGrade = letterGrade,
                    targetPercent = targetPercent,
                    requiredFinalExamScore = roundTwoDecimals(requiredScore),
                    isAchievable = isAchievable
                )
            }
        }
    }

    /**
     * Computes standard academic projections for letter grades A (90%), B (80%), C (70%), and D (60%).
     */
    fun computeTargetProjections(
        components: List<SyllabusComponent>,
        overrideFinalWeight: Float? = null
    ): List<TargetGradeProjection> {
        return STANDARD_TARGET_GRADES.map { (grade, target) ->
            computeRequiredScoreForTarget(
                components = components,
                targetPercent = target,
                letterGrade = grade,
                overrideFinalWeight = overrideFinalWeight
            )
        }
    }

    /**
     * Maps a cumulative percentage to its corresponding letter grade.
     */
    fun getLetterGrade(percentage: Float): String {
        return when {
            percentage >= 90f -> "A"
            percentage >= 80f -> "B"
            percentage >= 70f -> "C"
            percentage >= 60f -> "D"
            else -> "F"
        }
    }

    private fun roundTwoDecimals(value: Float): Float {
        return (value * 100f).roundToInt() / 100f
    }
}
