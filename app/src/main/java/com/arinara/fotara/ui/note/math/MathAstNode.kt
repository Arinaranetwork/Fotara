// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.note.math

/**
 * Text styling modifier for mathematical text and variable identifiers.
 */
enum class MathTextStyle {
    NORMAL,
    ITALIC,
    BOLD,
    MATHBB,
    MATHCAL,
    MATHRM,
    TEXT
}

/**
 * Categorization of mathematical symbols.
 */
enum class MathSymbolType {
    GREEK,
    OPERATOR,
    RELATION,
    ARROW,
    LOGIC,
    SET_THEORY,
    PUNCTUATION,
    ACCENT
}

/**
 * Delimiter brackets enclosing matrix cells and determinants.
 */
enum class MatrixBracketType {
    NONE,
    PARENTHESES,
    BRACKETS,
    VERTICAL_BARS,
    DOUBLE_VERTICAL_BARS,
    CURLY_BRACES
}

/**
 * Abstract syntax tree node representing a parsed LaTeX mathematical element.
 */
sealed class MathNode {

    /**
     * Plain textual token or variable symbol.
     */
    data class TextNode(
        val text: String,
        val style: MathTextStyle = MathTextStyle.NORMAL
    ) : MathNode()

    /**
     * Dedicated mathematical symbol (Greek letters, logic, relations, operators).
     */
    data class SymbolNode(
        val symbol: String,
        val type: MathSymbolType = MathSymbolType.OPERATOR
    ) : MathNode()

    /**
     * Fractions (\frac{numerator}{denominator}).
     */
    data class FractionNode(
        val numerator: MathNode,
        val denominator: MathNode
    ) : MathNode()

    /**
     * Radicals (\sqrt{radicand} or \sqrt[index]{radicand}).
     */
    data class RadicalNode(
        val radicand: MathNode,
        val index: MathNode? = null
    ) : MathNode()

    /**
     * Scripts: superscripts (^), subscripts (_), or dual scripts.
     */
    data class ScriptNode(
        val base: MathNode,
        val superscript: MathNode? = null,
        val subscript: MathNode? = null
    ) : MathNode()

    /**
     * Big operators (\sum, \prod, \int, \iint, \iiint, \oint, \lim) with limits.
     */
    data class BigOperatorNode(
        val operatorSymbol: String,
        val lowerLimit: MathNode? = null,
        val upperLimit: MathNode? = null,
        val isIntegral: Boolean = false
    ) : MathNode()

    /**
     * 2D Matrix or Determinant grids.
     */
    data class MatrixNode(
        val rows: List<List<MathNode>>,
        val bracketType: MatrixBracketType = MatrixBracketType.BRACKETS
    ) : MathNode()

    /**
     * Piecewise cases environment (\begin{cases} ... \end{cases}).
     */
    data class CasesNode(
        val cases: List<Pair<MathNode, MathNode?>>
    ) : MathNode()

    /**
     * Boxed formula highlighting (\boxed{...}).
     */
    data class BoxedNode(
        val inner: MathNode
    ) : MathNode()

    /**
     * Expandable bracketed or delimited block (\left( ... \right)).
     */
    data class DelimiterNode(
        val leftDelimiter: String,
        val rightDelimiter: String,
        val content: MathNode
    ) : MathNode()

    /**
     * Multiline derivation or aligned equations (\begin{aligned} ... \end{aligned}).
     */
    data class AlignedNode(
        val lines: List<List<MathNode>>
    ) : MathNode()

    /**
     * Sequence of sibling nodes arranged horizontally.
     */
    data class RowNode(
        val children: List<MathNode>
    ) : MathNode()

    /**
     * Fallback error node capturing syntax parsing errors with diagnostics.
     */
    data class ErrorNode(
        val rawLatex: String,
        val errorMessage: String
    ) : MathNode()
}
