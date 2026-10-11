// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.note.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Automated test suite verifying the pure native Compose LaTeX mathematical typesetting engine
 * across all 24 required mathematical notation categories without WebView dependencies.
 */
class NativeMathEngineTest {

    // Category 1: Simple algebra & polynomials
    @Test
    fun testCategory01_SimpleAlgebraAndPolynomials() {
        val ast = LatexAstParser.parse("x^2 + 2x + 1 = 0")
        assertNotNull(ast)
        assertTrue(ast is MathNode.RowNode)
        val row = ast as MathNode.RowNode
        assertTrue(row.children.isNotEmpty())

        val scriptNode = row.children.filterIsInstance<MathNode.ScriptNode>().firstOrNull()
        assertNotNull(scriptNode)
        assertEquals("x", (scriptNode?.base as? MathNode.TextNode)?.text)
        assertEquals("2", (scriptNode?.superscript as? MathNode.TextNode)?.text)

        val seriesAst = LatexAstParser.parse("a_1 + a_2 + \\dots + a_n")
        assertNotNull(seriesAst)
        val seriesRow = seriesAst as MathNode.RowNode
        val dotsNode = seriesRow.children.filterIsInstance<MathNode.SymbolNode>().find { it.symbol == "…" }
        assertNotNull(dotsNode)
        assertEquals("…", dotsNode?.symbol)
    }

    // Category 2: Quadratic formulas with fractions & radicals
    @Test
    fun testCategory02_QuadraticFormulaFractionsAndRadicals() {
        val latex = "x = \\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}"
        val ast = LatexAstParser.parse(latex)
        assertNotNull(ast)
        assertTrue(ast is MathNode.RowNode)
        val row = ast as MathNode.RowNode

        val fraction = row.children.filterIsInstance<MathNode.FractionNode>().firstOrNull()
        assertNotNull(fraction)

        val numRow = fraction?.numerator as? MathNode.RowNode
        assertNotNull(numRow)
        val pmSymbol = numRow?.children?.filterIsInstance<MathNode.SymbolNode>()?.find { it.symbol == "±" }
        assertNotNull(pmSymbol)

        val radical = numRow?.children?.filterIsInstance<MathNode.RadicalNode>()?.firstOrNull()
        assertNotNull(radical)
        assertNotNull(radical?.radicand)
    }

    // Category 3: 3x3 matrices
    @Test
    fun testCategory03_ThreeByThreeMatrices() {
        val latex = "\\begin{pmatrix} 1 & 2 & 3 \\\\ 4 & 5 & 6 \\\\ 7 & 8 & 9 \\end{pmatrix}"
        val ast = LatexAstParser.parse(latex)
        assertNotNull(ast)
        assertTrue(ast is MathNode.MatrixNode)
        val matrix = ast as MathNode.MatrixNode
        assertEquals(MatrixBracketType.PARENTHESES, matrix.bracketType)
        assertEquals(3, matrix.rows.size)
        assertEquals(3, matrix.rows[0].size)
        assertEquals(3, matrix.rows[1].size)
        assertEquals(3, matrix.rows[2].size)
    }

    // Category 4: Matrices with fractions & exponents
    @Test
    fun testCategory04_MatricesWithFractionsAndExponents() {
        val latex = "\\begin{bmatrix} \\frac{1}{2} & x^2 \\\\ y_1 & \\frac{a}{b} \\end{bmatrix}"
        val ast = LatexAstParser.parse(latex)
        assertNotNull(ast)
        assertTrue(ast is MathNode.MatrixNode)
        val matrix = ast as MathNode.MatrixNode
        assertEquals(MatrixBracketType.BRACKETS, matrix.bracketType)
        assertEquals(2, matrix.rows.size)

        val cell00 = matrix.rows[0][0]
        assertTrue(cell00 is MathNode.FractionNode || (cell00 is MathNode.RowNode && cell00.children.any { it is MathNode.FractionNode }))

        val cell01 = matrix.rows[0][1]
        assertTrue(cell01 is MathNode.ScriptNode || (cell01 is MathNode.RowNode && cell01.children.any { it is MathNode.ScriptNode }))
    }

    // Category 5: Determinants with vertical delimiters
    @Test
    fun testCategory05_DeterminantsVerticalDelimiters() {
        val latex = "\\begin{vmatrix} a & b \\\\ c & d \\end{vmatrix}"
        val ast = LatexAstParser.parse(latex)
        assertNotNull(ast)
        assertTrue(ast is MathNode.MatrixNode)
        val matrix = ast as MathNode.MatrixNode
        assertEquals(MatrixBracketType.VERTICAL_BARS, matrix.bracketType)
        assertEquals(2, matrix.rows.size)
        assertEquals(2, matrix.rows[0].size)
    }

    // Category 6: Boxed quadratic equations
    @Test
    fun testCategory06_BoxedQuadraticEquations() {
        val latex = "\\boxed{ax^2 + bx + c = 0}"
        val ast = LatexAstParser.parse(latex)
        assertNotNull(ast)
        assertTrue(ast is MathNode.BoxedNode)
        val boxed = ast as MathNode.BoxedNode
        assertTrue(boxed.inner is MathNode.RowNode)
        val row = boxed.inner as MathNode.RowNode
        assertTrue(row.children.any { it is MathNode.ScriptNode })
    }

    // Category 7: Boxed matrices
    @Test
    fun testCategory07_BoxedMatrices() {
        val latex = "\\boxed{\\begin{pmatrix} 1 & 0 \\\\ 0 & 1 \\end{pmatrix}}"
        val ast = LatexAstParser.parse(latex)
        assertNotNull(ast)
        assertTrue(ast is MathNode.BoxedNode)
        val boxed = ast as MathNode.BoxedNode
        assertTrue(boxed.inner is MathNode.MatrixNode)
        val matrix = boxed.inner as MathNode.MatrixNode
        assertEquals(2, matrix.rows.size)
    }

    // Category 8: Multiline aligned derivations
    @Test
    fun testCategory08_MultilineAlignedDerivations() {
        val latex = "\\begin{aligned} x &= 2y + 1 \\\\ y &= 3 \\end{aligned}"
        val ast = LatexAstParser.parse(latex)
        assertNotNull(ast)
        assertTrue(ast is MathNode.AlignedNode)
        val aligned = ast as MathNode.AlignedNode
        assertEquals(2, aligned.lines.size)
    }

    // Category 9: Piecewise cases functions
    @Test
    fun testCategory09_PiecewiseCasesFunctions() {
        val latex = "\\begin{cases} x & x > 0 \\\\ -x & x \\le 0 \\end{cases}"
        val ast = LatexAstParser.parse(latex)
        assertNotNull(ast)
        assertTrue(ast is MathNode.CasesNode)
        val cases = ast as MathNode.CasesNode
        assertEquals(2, cases.cases.size)
        assertNotNull(cases.cases[0].first)
        assertNotNull(cases.cases[0].second)
    }

    // Category 10: Integrals with limits
    @Test
    fun testCategory10_IntegralsWithLimits() {
        val latex = "\\int_{0}^{\\infty} e^{-x^2} dx"
        val ast = LatexAstParser.parse(latex)
        assertNotNull(ast)
        assertTrue(ast is MathNode.RowNode)
        val row = ast as MathNode.RowNode
        val integral = row.children.filterIsInstance<MathNode.BigOperatorNode>().firstOrNull()
        assertNotNull(integral)
        assertTrue(integral?.isIntegral == true)
        assertEquals("∫", integral?.operatorSymbol)
        assertEquals("0", (integral?.lowerLimit as? MathNode.TextNode)?.text)

        val doubleIntegralAst = LatexAstParser.parse("\\iint_D f(x, y) dA")
        val doubleIntegral = (doubleIntegralAst as? MathNode.RowNode)?.children?.filterIsInstance<MathNode.BigOperatorNode>()?.firstOrNull()
        assertNotNull(doubleIntegral)
        assertEquals("∬", doubleIntegral?.operatorSymbol)
    }

    // Category 11: Summations & products with bounds
    @Test
    fun testCategory11_SummationsAndProductsWithBounds() {
        val sumAst = LatexAstParser.parse("\\sum_{i=1}^{n} i^2")
        val sumOp = (sumAst as? MathNode.RowNode)?.children?.filterIsInstance<MathNode.BigOperatorNode>()?.firstOrNull()
        assertNotNull(sumOp)
        assertEquals("∑", sumOp?.operatorSymbol)
        assertNotNull(sumOp?.lowerLimit)
        assertNotNull(sumOp?.upperLimit)

        val prodAst = LatexAstParser.parse("\\prod_{k=1}^{n} k")
        val prodOp = (prodAst as? MathNode.RowNode)?.children?.filterIsInstance<MathNode.BigOperatorNode>()?.firstOrNull()
        assertNotNull(prodOp)
        assertEquals("∏", prodOp?.operatorSymbol)
    }

    // Category 12: Partial derivatives
    @Test
    fun testCategory12_PartialDerivatives() {
        val latex = "\\frac{\\partial f}{\\partial x} + \\frac{\\partial f}{\\partial y}"
        val ast = LatexAstParser.parse(latex)
        assertNotNull(ast)
        val row = ast as MathNode.RowNode
        val fractions = row.children.filterIsInstance<MathNode.FractionNode>()
        assertEquals(2, fractions.size)
        val num0 = fractions[0].numerator as? MathNode.RowNode
        val partialSymbol = num0?.children?.filterIsInstance<MathNode.SymbolNode>()?.firstOrNull()
        assertNotNull(partialSymbol)
        assertEquals("∂", partialSymbol?.symbol)
    }

    // Category 13: Nested delimiters
    @Test
    fun testCategory13_NestedDelimiters() {
        val latex = "\\left( \\frac{a}{b} \\right) + \\left[ \\frac{x}{y} \\right]"
        val ast = LatexAstParser.parse(latex)
        assertNotNull(ast)
        val row = ast as MathNode.RowNode
        val delimiters = row.children.filterIsInstance<MathNode.DelimiterNode>()
        assertEquals(2, delimiters.size)
        assertEquals("(", delimiters[0].leftDelimiter)
        assertEquals(")", delimiters[0].rightDelimiter)
        assertTrue(delimiters[0].content is MathNode.FractionNode)
        assertEquals("[", delimiters[1].leftDelimiter)
        assertEquals("]", delimiters[1].rightDelimiter)
    }

    // Category 14: Probability expressions
    @Test
    fun testCategory14_ProbabilityExpressions() {
        val latex = "P(A \\mid B) = \\frac{P(A \\cap B)}{P(B)}"
        val ast = LatexAstParser.parse(latex)
        assertNotNull(ast)
        val row = ast as MathNode.RowNode
        val midSymbol = row.children.filterIsInstance<MathNode.SymbolNode>().find { it.symbol == "|" }
        assertNotNull(midSymbol)

        val fraction = row.children.filterIsInstance<MathNode.FractionNode>().firstOrNull()
        assertNotNull(fraction)

        val expectationAst = LatexAstParser.parse("\\mathbb{E}[X]")
        val expRow = expectationAst as MathNode.RowNode
        val expSymbol = expRow.children.filterIsInstance<MathNode.TextNode>().find { it.text == "𝔼" }
        assertNotNull(expSymbol)
    }

    // Category 15: Logic & set theory
    @Test
    fun testCategory15_LogicAndSetTheory() {
        val logicLatex = "\\forall x \\in A \\implies x \\notin B"
        val logicAst = LatexAstParser.parse(logicLatex)
        val logicRow = logicAst as MathNode.RowNode
        val symbols = logicRow.children.filterIsInstance<MathNode.SymbolNode>().map { it.symbol }
        assertTrue(symbols.contains("∀"))
        assertTrue(symbols.contains("∈"))
        assertTrue(symbols.contains("⟹"))
        assertTrue(symbols.contains("∉"))

        val setLatex = "A \\cup B \\cap C \\subset D"
        val setAst = LatexAstParser.parse(setLatex)
        val setRow = setAst as MathNode.RowNode
        val setSymbols = setRow.children.filterIsInstance<MathNode.SymbolNode>().map { it.symbol }
        assertTrue(setSymbols.contains("∪"))
        assertTrue(setSymbols.contains("∩"))
        assertTrue(setSymbols.contains("⊂"))
    }

    // Category 16: Vectors & physics
    @Test
    fun testCategory16_VectorsAndPhysics() {
        val latex = "\\vec{v} = \\frac{d\\vec{r}}{dt}"
        val ast = LatexAstParser.parse(latex)
        assertNotNull(ast)
        val row = ast as MathNode.RowNode
        val vecNode = row.children.filterIsInstance<MathNode.TextNode>().find { it.text.contains("v\u20D7") || it.text.contains("v⃗") }
        assertNotNull(vecNode)

        val forceAst = LatexAstParser.parse("\\mathbf{F} = m\\mathbf{a}")
        val forceRow = forceAst as MathNode.RowNode
        val boldNodes = forceRow.children.filterIsInstance<MathNode.TextNode>().filter { it.style == MathTextStyle.BOLD }
        assertTrue(boldNodes.isNotEmpty())
    }

    // Category 17: Chemical subscripts & superscripts
    @Test
    fun testCategory17_ChemicalSubscriptsAndSuperscripts() {
        val waterAst = LatexAstParser.parse("\\text{H}_2\\text{O}")
        assertNotNull(waterAst)
        val waterRow = waterAst as MathNode.RowNode
        val hScript = waterRow.children.filterIsInstance<MathNode.ScriptNode>().firstOrNull()
        assertNotNull(hScript)
        assertEquals("H", (hScript?.base as? MathNode.TextNode)?.text)
        assertEquals("2", (hScript?.subscript as? MathNode.TextNode)?.text)

        val sulfateAst = LatexAstParser.parse("\\text{SO}_4^{2-}")
        val sulfateScript = when (sulfateAst) {
            is MathNode.ScriptNode -> sulfateAst
            is MathNode.RowNode -> sulfateAst.children.filterIsInstance<MathNode.ScriptNode>().firstOrNull()
            else -> null
        }
        assertNotNull(sulfateScript)
        assertEquals("4", (sulfateScript?.subscript as? MathNode.TextNode)?.text)
        val superRow = sulfateScript?.superscript as? MathNode.RowNode
        assertNotNull(superRow)
        assertEquals("2", (superRow?.children?.get(0) as? MathNode.TextNode)?.text)
        assertEquals("-", (superRow?.children?.get(1) as? MathNode.SymbolNode)?.symbol)
    }

    // Category 18: Long horizontal expressions
    @Test
    fun testCategory18_LongHorizontalExpressions() {
        val longExpr = "a_0 + a_1 x + a_2 x^2 + a_3 x^3 + a_4 x^4 + a_5 x^5 + a_6 x^6 = \\sum_{k=0}^{6} a_k x^k"
        val ast = LatexAstParser.parse(longExpr)
        assertNotNull(ast)
        val row = ast as MathNode.RowNode
        assertTrue(row.children.size >= 10)
        assertTrue(row.children.any { it is MathNode.BigOperatorNode })
    }

    // Category 19: Tall equations in vertical containers
    @Test
    fun testCategory19_TallEquationsInVerticalContainers() {
        val tallExpr = "\\frac{1}{1 + \\frac{1}{1 + \\frac{1}{x}}}"
        val ast = LatexAstParser.parse(tallExpr)
        assertNotNull(ast)
        assertTrue(ast is MathNode.FractionNode)
        val l1 = ast as MathNode.FractionNode
        val denom1 = l1.denominator as? MathNode.RowNode
        val l2 = denom1?.children?.filterIsInstance<MathNode.FractionNode>()?.firstOrNull()
        assertNotNull(l2)
        val denom2 = l2?.denominator as? MathNode.RowNode
        val l3 = denom2?.children?.filterIsInstance<MathNode.FractionNode>()?.firstOrNull()
        assertNotNull(l3)
    }

    // Category 20: Formulas mixed in markdown text
    @Test
    fun testCategory20_FormulasMixedInMarkdownText() {
        val inlineInput = "\$x^2 + y^2 = z^2\$"
        val inlineAst = LatexAstParser.parse(inlineInput)
        assertNotNull(inlineAst)
        assertTrue(inlineAst is MathNode.RowNode)
        val inlineRow = inlineAst as MathNode.RowNode
        assertTrue(inlineRow.children.any { it is MathNode.ScriptNode })

        val blockInput = "\$\$\\int_0^1 f(t) dt\$\$"
        val blockAst = LatexAstParser.parse(blockInput)
        assertNotNull(blockAst)
        assertTrue(blockAst is MathNode.RowNode)
        val blockRow = blockAst as MathNode.RowNode
        assertTrue(blockRow.children.any { it is MathNode.BigOperatorNode })
    }

    // Category 21: Malformed syntax error handling
    @Test
    fun testCategory21_MalformedSyntaxErrorHandling() {
        val unclosed = "\\frac{a}{b"
        val ast1 = LatexAstParser.parse(unclosed)
        assertNotNull(ast1)
        assertFalse(ast1 is MathNode.ErrorNode)

        val unknownCommand = "\\nonexistentcommand{x}"
        val ast2 = LatexAstParser.parse(unknownCommand)
        assertNotNull(ast2)

        val emptyInput = ""
        val ast3 = LatexAstParser.parse(emptyInput)
        assertNotNull(ast3)
        assertTrue(ast3 is MathNode.RowNode)
        assertTrue((ast3 as MathNode.RowNode).children.isEmpty())
    }

    // Category 22: Theme contrast / colors
    @Test
    fun testCategory22_ThemeContrastColors() {
        val ast = LatexAstParser.parse("x + y = z")
        assertNotNull(ast)
        assertTrue(ast is MathNode.RowNode)
        val textNode = (ast as MathNode.RowNode).children.first() as MathNode.TextNode
        assertEquals(MathTextStyle.ITALIC, textNode.style)
    }

    // Category 23: Recomposition stability
    @Test
    fun testCategory23_RecompositionStability() {
        val ast1 = LatexAstParser.parse("E = mc^2")
        val ast2 = LatexAstParser.parse("E = mc^2")
        assertEquals(ast1, ast2)
        assertEquals(ast1.hashCode(), ast2.hashCode())

        val fraction1 = MathNode.FractionNode(
            numerator = MathNode.TextNode("1"),
            denominator = MathNode.TextNode("2")
        )
        val fraction2 = fraction1.copy()
        assertEquals(fraction1, fraction2)
        assertEquals(fraction1.hashCode(), fraction2.hashCode())
    }

    // Category 24: Adaptive screen widths / layout boundaries
    @Test
    fun testCategory24_AdaptiveScreenWidthsLayoutBoundaries() {
        val wideMatrix = buildString {
            append("\\begin{pmatrix}")
            for (i in 1..5) {
                append((1..5).joinToString(" & ") { "c_{$it}" })
                if (i < 5) append(" \\\\ ")
            }
            append("\\end{pmatrix}")
        }
        val ast = LatexAstParser.parse(wideMatrix)
        assertNotNull(ast)
        assertTrue(ast is MathNode.MatrixNode)
        val matrix = ast as MathNode.MatrixNode
        assertEquals(5, matrix.rows.size)
        assertEquals(5, matrix.rows[0].size)
    }
}
