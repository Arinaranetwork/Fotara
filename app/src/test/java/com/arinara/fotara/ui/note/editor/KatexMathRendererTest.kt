// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.note.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KatexMathRendererTest {

    @Test
    fun formatToReadableMath_stripsDisplayStyle() {
        val input = "\\displaystyle \\sum_{i=1}^{n} i"
        val output = KatexMathRenderer.formatToReadableMath(input)
        assertFalse(output.contains("displaystyle"))
        assertTrue(output.contains("∑"))
    }

    @Test
    fun formatToReadableMath_unwrapsBoxedExpression() {
        val input = "\\boxed{E = mc^2}"
        val output = KatexMathRenderer.formatToReadableMath(input)
        assertFalse(output.contains("boxed"))
        assertTrue(output.contains("E = mc²"))
    }

    @Test
    fun formatToReadableMath_formatsCasesPiecewise() {
        val input = """
            \begin{cases}
            x + 1 & \text{if } x > 0 \\
            0 & \text{otherwise}
            \end{cases}
        """.trimIndent()
        val output = KatexMathRenderer.formatToReadableMath(input)
        assertFalse(output.contains("begin{cases}"))
        assertFalse(output.contains("end{cases}"))
        assertTrue(output.contains("{ x + 1"))
        assertTrue(output.contains("if x > 0"))
        assertTrue(output.contains("otherwise"))
    }

    @Test
    fun formatToReadableMath_formatsMatrices() {
        val pmatrixInput = """
            \begin{pmatrix}
            1 & 2 \\
            3 & 4
            \end{pmatrix}
        """.trimIndent()
        val pmatrixOutput = KatexMathRenderer.formatToReadableMath(pmatrixInput)
        assertFalse(pmatrixOutput.contains("pmatrix"))
        assertTrue(pmatrixOutput.contains("( 1 2 )") || pmatrixOutput.contains("( 1  2 )") || pmatrixOutput.contains("( 1"))
        assertTrue(pmatrixOutput.contains("3") && pmatrixOutput.contains("4"))

        val bmatrixInput = """
            \begin{bmatrix}
            a & b \\
            c & d
            \end{bmatrix}
        """.trimIndent()
        val bmatrixOutput = KatexMathRenderer.formatToReadableMath(bmatrixInput)
        assertFalse(bmatrixOutput.contains("bmatrix"))
        assertTrue(bmatrixOutput.contains("[") && bmatrixOutput.contains("]"))
    }

    @Test
    fun formatToReadableMath_handlesSpacingMacros() {
        val input = "a \\quad b \\qquad c \\, d"
        val output = KatexMathRenderer.formatToReadableMath(input)
        assertFalse(output.contains("quad"))
        assertFalse(output.contains("qquad"))
        assertTrue(output.contains("a") && output.contains("b") && output.contains("c") && output.contains("d"))
    }

    @Test
    fun formatToReadableMath_handlesMathFunctionsAndOperators() {
        val input = "\\det(A) + \\ker(T) + \\gcd(a, b)"
        val output = KatexMathRenderer.formatToReadableMath(input)
        assertTrue(output.contains("det(A)"))
        assertTrue(output.contains("ker(T)"))
        assertTrue(output.contains("gcd(a, b)"))
        assertFalse(output.contains("\\det"))
    }

    @Test
    fun formatToReadableMath_handlesFractionsAndRoots() {
        val frac = "\\frac{1}{2} + \\frac{a+b}{c+d}"
        val fracOutput = KatexMathRenderer.formatToReadableMath(frac)
        assertTrue(fracOutput.contains("1/2"))
        assertTrue(fracOutput.contains("(a+b)/(c+d)"))

        val sqrt = "\\sqrt{x} + \\sqrt[3]{8}"
        val sqrtOutput = KatexMathRenderer.formatToReadableMath(sqrt)
        assertTrue(sqrtOutput.contains("√(x)"))
        assertTrue(sqrtOutput.contains("³√(8)"))
    }

    @Test
    fun formatToReadableMath_handlesSubscriptsAndSuperscripts() {
        val input = "x^2 + y_1 + a_{12}"
        val output = KatexMathRenderer.formatToReadableMath(input)
        assertTrue(output.contains("x²"))
        assertTrue(output.contains("y₁"))
        assertTrue(output.contains("a₁₂"))
    }
}
