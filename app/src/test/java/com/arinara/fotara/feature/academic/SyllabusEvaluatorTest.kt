// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.academic

import com.arinara.fotara.feature.academic.syllabus.SyllabusComponent
import com.arinara.fotara.feature.academic.syllabus.SyllabusEvaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyllabusEvaluatorTest {

    private lateinit var evaluator: SyllabusEvaluator

    @Before
    fun setUp() {
        evaluator = SyllabusEvaluator()
    }

    @Test
    fun calculateTotalEarnedPoints_computesWeightedSumCorrectly() {
        val components = listOf(
            SyllabusComponent(id = "c1", name = "Midterm 1", weightPercent = 20f, currentScore = 80f, maxScore = 100f), // 16 pts
            SyllabusComponent(id = "c2", name = "Midterm 2", weightPercent = 20f, currentScore = 90f, maxScore = 100f), // 18 pts
            SyllabusComponent(id = "c3", name = "Homework", weightPercent = 10f, currentScore = 100f, maxScore = 100f), // 10 pts
            SyllabusComponent(id = "c4", name = "Final Exam", weightPercent = 50f, currentScore = null, maxScore = 100f) // 0 pts
        )

        val earned = evaluator.calculateTotalEarnedPoints(components)
        assertEquals(44f, earned, 0.01f)
    }

    @Test
    fun calculateCompletedAndRemainingWeight_handlesPartialSyllabus() {
        val components = listOf(
            SyllabusComponent(id = "c1", name = "Quiz 1", weightPercent = 15f, currentScore = 95f),
            SyllabusComponent(id = "c2", name = "Quiz 2", weightPercent = 15f, currentScore = 85f),
            SyllabusComponent(id = "c3", name = "Project", weightPercent = 30f, currentScore = null),
            SyllabusComponent(id = "c4", name = "Final Exam", weightPercent = 40f, currentScore = null)
        )

        val completedWeight = evaluator.calculateCompletedWeight(components)
        val remainingWeight = evaluator.calculateRemainingWeight(components)

        assertEquals(30f, completedWeight, 0.01f)
        assertEquals(70f, remainingWeight, 0.01f)
    }

    @Test
    fun calculateCurrentStandingAverage_normalizesToCompletedOnly() {
        val components = listOf(
            SyllabusComponent(id = "c1", name = "Assignment 1", weightPercent = 20f, currentScore = 90f, maxScore = 100f),
            SyllabusComponent(id = "c2", name = "Assignment 2", weightPercent = 20f, currentScore = 80f, maxScore = 100f),
            SyllabusComponent(id = "c3", name = "Final", weightPercent = 60f, currentScore = null)
        )

        // Earned: 18 + 16 = 34 pts out of 40 completed weight -> 34/40 * 100 = 85%
        val standing = evaluator.calculateCurrentStandingAverage(components)
        assertEquals(85f, standing, 0.01f)
    }

    @Test
    fun calculateCurrentStandingAverage_returnsZeroWhenEmpty() {
        val components = listOf(
            SyllabusComponent(id = "c1", name = "Final", weightPercent = 100f, currentScore = null)
        )
        val standing = evaluator.calculateCurrentStandingAverage(components)
        assertEquals(0f, standing, 0.001f)
    }

    @Test
    fun computeRequiredScoreForTarget_typicalAttainableProjection() {
        val components = listOf(
            SyllabusComponent(id = "c1", name = "Midterm 1", weightPercent = 20f, currentScore = 85f), // 17 pts
            SyllabusComponent(id = "c2", name = "Midterm 2", weightPercent = 20f, currentScore = 90f), // 18 pts
            SyllabusComponent(id = "c3", name = "Homework", weightPercent = 20f, currentScore = 95f),  // 19 pts
            // Total earned = 54 pts. Remaining weight = 40%
            SyllabusComponent(id = "c4", name = "Final Exam", weightPercent = 40f, currentScore = null)
        )

        // Target A (90%): points needed = 90 - 54 = 36. Required on final = 36 / 40 * 100 = 90.0%
        val projectionA = evaluator.computeRequiredScoreForTarget(components, targetPercent = 90f)
        assertEquals("A", projectionA.letterGrade)
        assertEquals(90f, projectionA.targetPercent, 0.01f)
        assertEquals(90f, projectionA.requiredFinalExamScore, 0.05f)
        assertTrue(projectionA.isAchievable)

        // Target B (80%): points needed = 80 - 54 = 26. Required on final = 26 / 40 * 100 = 65.0%
        val projectionB = evaluator.computeRequiredScoreForTarget(components, targetPercent = 80f)
        assertEquals(65f, projectionB.requiredFinalExamScore, 0.05f)
        assertTrue(projectionB.isAchievable)
    }

    @Test
    fun computeRequiredScoreForTarget_whenTargetAlreadyLockedIn_returnsZero() {
        val components = listOf(
            SyllabusComponent(id = "c1", name = "Assignments", weightPercent = 50f, currentScore = 100f), // 50 pts
            SyllabusComponent(id = "c2", name = "Midterm", weightPercent = 30f, currentScore = 100f),     // 30 pts
            // Earned: 80 pts.
            SyllabusComponent(id = "c3", name = "Final Exam", weightPercent = 20f, currentScore = null)
        )

        // Target C (70%): already surpassed with 80 pts -> required = 0%
        val projectionC = evaluator.computeRequiredScoreForTarget(components, targetPercent = 70f)
        assertEquals(0f, projectionC.requiredFinalExamScore, 0.01f)
        assertTrue(projectionC.isAchievable)
    }

    @Test
    fun computeRequiredScoreForTarget_whenTargetImpossible_marksNotAchievable() {
        val components = listOf(
            SyllabusComponent(id = "c1", name = "Midterm", weightPercent = 70f, currentScore = 50f), // 35 pts earned
            // Remaining weight = 30%
            SyllabusComponent(id = "c2", name = "Final Exam", weightPercent = 30f, currentScore = null)
        )

        // Target A (90%): points needed = 90 - 35 = 55. Required on final = 55 / 30 * 100 = 183.33% (>100)
        val projectionA = evaluator.computeRequiredScoreForTarget(components, targetPercent = 90f)
        assertFalse(projectionA.isAchievable)
        assertTrue(projectionA.requiredFinalExamScore > 100f)
    }

    @Test
    fun computeRequiredScoreForTarget_zeroRemainingWeight_handlesGracefully() {
        val components = listOf(
            SyllabusComponent(id = "c1", name = "Exam 1", weightPercent = 50f, currentScore = 60f), // 30 pts
            SyllabusComponent(id = "c2", name = "Exam 2", weightPercent = 50f, currentScore = 60f)  // 30 pts
            // Total 60 pts, remaining weight = 0%
        )

        val projectionA = evaluator.computeRequiredScoreForTarget(components, targetPercent = 90f)
        assertFalse(projectionA.isAchievable)
        assertTrue(projectionA.requiredFinalExamScore.isInfinite())
    }

    @Test
    fun computeTargetProjections_returnsAllFourStandardGrades() {
        val components = listOf(
            SyllabusComponent(id = "c1", name = "Coursework", weightPercent = 60f, currentScore = 80f),
            SyllabusComponent(id = "c2", name = "Final", weightPercent = 40f, currentScore = null)
        )

        val projections = evaluator.computeTargetProjections(components)
        assertEquals(4, projections.size)
        assertEquals(listOf("A", "B", "C", "D"), projections.map { it.letterGrade })
    }

    @Test
    fun getLetterGrade_mapsCorrectlyAcrossThresholds() {
        assertEquals("A", evaluator.getLetterGrade(95f))
        assertEquals("A", evaluator.getLetterGrade(90f))
        assertEquals("B", evaluator.getLetterGrade(89.9f))
        assertEquals("B", evaluator.getLetterGrade(80f))
        assertEquals("C", evaluator.getLetterGrade(79.9f))
        assertEquals("C", evaluator.getLetterGrade(70f))
        assertEquals("D", evaluator.getLetterGrade(69.9f))
        assertEquals("D", evaluator.getLetterGrade(60f))
        assertEquals("F", evaluator.getLetterGrade(59.9f))
        assertEquals("F", evaluator.getLetterGrade(0f))
    }
}
