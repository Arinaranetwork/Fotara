// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.note.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Lightweight native KaTeX / LaTeX mathematical expression parser and formatter.
 * Delivers clean, offline mathematical typography for STEM notes without external
 * webview or heavy runtime dependencies.
 */
object KatexMathRenderer {

    private val GREEK_MAP = mapOf(
        "\\alpha" to "α", "\\beta" to "β", "\\gamma" to "γ", "\\delta" to "δ",
        "\\epsilon" to "ε", "\\varepsilon" to "ε", "\\zeta" to "ζ", "\\eta" to "η",
        "\\theta" to "θ", "\\vartheta" to "ϑ", "\\iota" to "ι", "\\kappa" to "κ",
        "\\lambda" to "λ", "\\mu" to "μ", "\\nu" to "ν", "\\xi" to "ξ",
        "\\pi" to "π", "\\varpi" to "ϖ", "\\rho" to "ρ", "\\varrho" to "ϱ",
        "\\sigma" to "σ", "\\varsigma" to "ς", "\\tau" to "τ", "\\upsilon" to "υ",
        "\\phi" to "φ", "\\varphi" to "ϕ", "\\chi" to "χ", "\\psi" to "ψ",
        "\\omega" to "ω",
        "\\Gamma" to "Γ", "\\Delta" to "Δ", "\\Theta" to "Θ", "\\Lambda" to "Λ",
        "\\Xi" to "Ξ", "\\Pi" to "Π", "\\Sigma" to "Σ", "\\Upsilon" to "Υ",
        "\\Phi" to "Φ", "\\Psi" to "Ψ", "\\Omega" to "Ω"
    )

    private val OPERATOR_MAP = mapOf(
        "\\pm" to "±", "\\mp" to "∓", "\\times" to "×", "\\div" to "÷",
        "\\cdot" to "·", "\\ast" to "∗", "\\star" to "⋆", "\\circ" to "∘",
        "\\bullet" to "•", "\\cap" to "∩", "\\cup" to "∪", "\\uplus" to "⊎",
        "\\sqcap" to "⊓", "\\sqcup" to "⊔", "\\vee" to "∨", "\\wedge" to "∧",
        "\\oplus" to "⊕", "\\ominus" to "⊖", "\\otimes" to "⊗", "\\oslash" to "⊘",
        "\\odot" to "⊙", "\\dagger" to "†", "\\ddagger" to "‡", "\\amalg" to "⨿",
        "\\le" to "≤", "\\leq" to "≤", "\\ge" to "≥", "\\geq" to "≥",
        "\\ne" to "≠", "\\neq" to "≠", "\\approx" to "≈", "\\sim" to "∼",
        "\\simeq" to "≃", "\\cong" to "≅", "\\equiv" to "≡", "\\propto" to "∝",
        "\\in" to "∈", "\\notin" to "∉", "\\ni" to "∋", "\\subset" to "⊂",
        "\\subseteq" to "⊆", "\\supset" to "⊃", "\\supseteq" to "⊇",
        "\\to" to "→", "\\rightarrow" to "→", "\\leftarrow" to "←",
        "\\Rightarrow" to "⇒", "\\Leftarrow" to "⇐", "\\Leftrightarrow" to "⇔",
        "\\mapsto" to "↦", "\\nearrow" to "↗", "\\searrow" to "↘",
        "\\int" to "∫", "\\iint" to "∬", "\\iiint" to "∭", "\\oint" to "∮",
        "\\sum" to "∑", "\\prod" to "∏", "\\coprod" to "∐",
        "\\infty" to "∞", "\\partial" to "∂", "\\nabla" to "∇",
        "\\forall" to "∀", "\\exists" to "∃", "\\nexists" to "∄",
        "\\emptyset" to "∅", "\\varnothing" to "∅", "\\angle" to "∠",
        "\\perp" to "⊥", "\\parallel" to "∥"
    )

    private val SUPERSCRIPT_MAP = mapOf(
        '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
        '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
        '+' to '⁺', '-' to '⁻', '=' to '⁼', '(' to '⁽', ')' to '⁾',
        'n' to 'ⁿ', 'i' to 'ⁱ', 'x' to 'ˣ', 'y' to 'ʸ', 't' to 'ᵗ'
    )

    private val SUBSCRIPT_MAP = mapOf(
        '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄',
        '5' to '₅', '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉',
        '+' to '₊', '-' to '₋', '=' to '₌', '(' to '₍', ')' to '₎',
        'a' to 'ₐ', 'e' to 'ₑ', 'h' to 'ₕ', 'i' to 'ᵢ', 'j' to 'ⱼ',
        'k' to 'ₖ', 'l' to 'ₗ', 'm' to 'ₘ', 'n' to 'ₙ', 'o' to 'ₒ',
        'p' to 'ₚ', 'r' to 'ᵣ', 's' to 'ₛ', 't' to 'ₜ', 'u' to 'ᵤ',
        'v' to 'ᵥ', 'x' to 'ₓ'
    )

    /**
     * Converts a raw LaTeX expression into clean Unicode mathematical notation.
     */
    fun formatToReadableMath(rawLatex: String): String {
        var result = rawLatex.trim()
        if (result.startsWith("$$") && result.endsWith("$$") && result.length >= 4) {
            result = result.substring(2, result.length - 2).trim()
        } else if (result.startsWith("$") && result.endsWith("$") && result.length >= 2) {
            result = result.substring(1, result.length - 1).trim()
        }

        // 0a. Strip \displaystyle
        result = result.replace(Regex("""\\displaystyle\s*"""), "")

        // 0b. Unwrap \boxed{...} cleanly
        val boxedRegex = Regex("""\\boxed\{([^{}]+)\}""")
        var prevBoxed = ""
        while (prevBoxed != result) {
            prevBoxed = result
            result = boxedRegex.replace(result) { it.groupValues[1] }
        }

        // 0c. Piecewise cases: \begin{cases} ... \end{cases}
        val casesRegex = Regex("""(?s)\\begin\{cases\}([\s\S]*?)\\end\{cases\}""")
        result = casesRegex.replace(result) { match ->
            val body = match.groupValues[1]
            val rows = body.split(Regex("""\\\\""")).map { it.trim() }.filter { it.isNotEmpty() }
            val formattedRows = rows.mapIndexed { idx, row ->
                val parts = row.split('&').map { it.trim() }
                val line = parts.joinToString("  ")
                if (idx == 0) "{ $line" else "  $line"
            }
            formattedRows.joinToString("\n")
        }

        // 0d. Matrices: \begin{pmatrix}, \begin{bmatrix}, \begin{matrix}, etc.
        val matrixRegex = Regex("""(?s)\\begin\{(p|b|v|B|V)?matrix\}([\s\S]*?)\\end\{\1matrix\}""")
        result = matrixRegex.replace(result) { match ->
            val type = match.groupValues[1]
            val body = match.groupValues[2]
            val (openBracket, closeBracket) = when (type) {
                "p" -> "(" to ")"
                "b" -> "[" to "]"
                "v" -> "|" to "|"
                "B" -> "{" to "}"
                "V" -> "‖" to "‖"
                else -> "[" to "]"
            }
            val rows = body.split(Regex("""\\\\""")).map { it.trim() }.filter { it.isNotEmpty() }
            if (rows.size == 1) {
                val rowStr = rows[0].split('&').map { it.trim() }.joinToString("  ")
                "$openBracket $rowStr $closeBracket"
            } else {
                val formattedRows = rows.map { row ->
                    val rowStr = row.split('&').map { it.trim() }.joinToString("  ")
                    "$openBracket $rowStr $closeBracket"
                }
                formattedRows.joinToString("\n")
            }
        }

        // 1. Fractions: \frac{a}{b} -> (a)/(b)
        val fracRegex = Regex("""\\frac\{([^{}]+)\}\{([^{}]+)\}""")
        var prevFrac = ""
        while (prevFrac != result) {
            prevFrac = result
            result = fracRegex.replace(result) { match ->
                val num = match.groupValues[1]
                val den = match.groupValues[2]
                if (num.length == 1 && den.length == 1) "$num/$den" else "($num)/($den)"
            }
        }

        // 2. Square roots: \sqrt{x} -> √(x), \sqrt[n]{x} -> ⁿ√(x)
        val sqrtNRegex = Regex("""\\sqrt\[([^{}\]]+)\]\{([^{}]+)\}""")
        result = sqrtNRegex.replace(result) { match ->
            val n = convertToSuperscript(match.groupValues[1])
            val inner = match.groupValues[2]
            "${n}√($inner)"
        }
        val sqrtRegex = Regex("""\\sqrt\{([^{}]+)\}""")
        result = sqrtRegex.replace(result) { match ->
            "√(${match.groupValues[1]})"
        }

        // 3. Vector accents: \vec{x} -> x⃗, \bar{x} -> x̄, \hat{x} -> x̂
        result = Regex("""\\vec\{([^{}])\}""").replace(result) { "${it.groupValues[1]}\u20D7" }
        result = Regex("""\\vec\s+([a-zA-Z])""").replace(result) { "${it.groupValues[1]}\u20D7" }
        result = Regex("""\\bar\{([^{}])\}""").replace(result) { "${it.groupValues[1]}\u0304" }
        result = Regex("""\\hat\{([^{}])\}""").replace(result) { "${it.groupValues[1]}\u0302" }
        result = Regex("""\\dot\{([^{}])\}""").replace(result) { "${it.groupValues[1]}\u0307" }

        // 4. Superscripts: ^{12} or ^2
        result = Regex("""\^\{([^{}]+)\}""").replace(result) { match ->
            convertToSuperscript(match.groupValues[1])
        }
        result = Regex("""\^([0-9a-zA-Z+-])""").replace(result) { match ->
            convertToSuperscript(match.groupValues[1])
        }

        // 5. Subscripts: _{12} or _2
        result = Regex("""_\{([^{}]+)\}""").replace(result) { match ->
            convertToSubscript(match.groupValues[1])
        }
        result = Regex("""_([0-9a-zA-Z+-])""").replace(result) { match ->
            convertToSubscript(match.groupValues[1])
        }

        // 6. Greek letters
        for ((macro, symbol) in GREEK_MAP) {
            result = result.replace(macro, symbol)
        }

        // 7. Operators & Relations
        for ((macro, symbol) in OPERATOR_MAP) {
            result = result.replace(macro, symbol)
        }

        // 8. Functions: \sin -> sin, \cos -> cos, \det -> det, etc.
        val funcRegex = Regex("""\\(sin|cos|tan|cot|sec|csc|log|ln|exp|lim|det|max|min|deg|ker|dim|gcd|hom|inf|sup|arg)\b""")
        result = funcRegex.replace(result) { it.groupValues[1] }

        // 9. Text macros: \text{abc} -> abc, \mathrm{abc} -> abc
        result = Regex("""\\(text|mathrm|mathbf|mathit)\{([^{}]+)\}""").replace(result) { it.groupValues[2] }

        // 10. LaTeX spacing macros
        result = result.replace(Regex("""\\qquad"""), "    ")
        result = result.replace(Regex("""\\quad"""), "  ")
        result = result.replace(Regex("""\\[,;:]"""), " ")
        result = result.replace(Regex("""\\!"""), "")

        // 11. LaTeX line breaks \\ and alignment &
        result = result.replace(Regex("""\\\\"""), "\n")
        result = result.replace("&", " ")

        // 12. Clean horizontal whitespace while preserving clean linebreaks
        result = result.lines()
            .map { it.replace(Regex("""[^\S\r\n]+"""), " ").trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
        return result
    }

    private fun convertToSuperscript(input: String): String {
        val sb = StringBuilder()
        for (ch in input) {
            sb.append(SUPERSCRIPT_MAP[ch] ?: ch)
        }
        return sb.toString()
    }

    private fun convertToSubscript(input: String): String {
        val sb = StringBuilder()
        for (ch in input) {
            sb.append(SUBSCRIPT_MAP[ch] ?: ch)
        }
        return sb.toString()
    }

    /**
     * Builds an [AnnotatedString] styled specifically for mathematical rendering.
     */
    fun buildMathAnnotatedString(
        rawLatex: String,
        mathColor: Color = Color(0xFF64B5F6),
        isBlock: Boolean = false
    ): AnnotatedString {
        val readable = formatToReadableMath(rawLatex)
        return buildAnnotatedString {
            pushStyle(
                SpanStyle(
                    color = mathColor,
                    fontStyle = FontStyle.Italic,
                    fontWeight = if (isBlock) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = if (isBlock) 16.sp else 14.sp,
                    fontFamily = FontFamily.Serif
                )
            )
            append(if (isBlock) "  $readable  " else readable)
            pop()
        }
    }
}
