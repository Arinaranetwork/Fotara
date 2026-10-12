// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.note.math

/**
 * Pure Kotlin recursive descent parser that transforms LaTeX mathematical expressions
 * into an Abstract Syntax Tree (AST) composed of [MathNode] structures.
 * Operates offline with zero WebView, zero JavaScript, and zero external runtime dependencies.
 */
object LatexAstParser {

    private val GREEK_LETTERS = mapOf(
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

    private val OPERATORS_AND_RELATIONS = mapOf(
        "\\pm" to ("±" to MathSymbolType.OPERATOR),
        "\\mp" to ("∓" to MathSymbolType.OPERATOR),
        "\\times" to ("×" to MathSymbolType.OPERATOR),
        "\\div" to ("÷" to MathSymbolType.OPERATOR),
        "\\cdot" to ("·" to MathSymbolType.OPERATOR),
        "\\ast" to ("∗" to MathSymbolType.OPERATOR),
        "\\circ" to ("∘" to MathSymbolType.OPERATOR),
        "\\bullet" to ("•" to MathSymbolType.OPERATOR),
        "\\cap" to ("∩" to MathSymbolType.SET_THEORY),
        "\\cup" to ("∪" to MathSymbolType.SET_THEORY),
        "\\in" to ("∈" to MathSymbolType.SET_THEORY),
        "\\notin" to ("∉" to MathSymbolType.SET_THEORY),
        "\\subset" to ("⊂" to MathSymbolType.SET_THEORY),
        "\\subseteq" to ("⊆" to MathSymbolType.SET_THEORY),
        "\\supset" to ("⊃" to MathSymbolType.SET_THEORY),
        "\\supseteq" to ("⊇" to MathSymbolType.SET_THEORY),
        "\\emptyset" to ("∅" to MathSymbolType.SET_THEORY),
        "\\varnothing" to ("∅" to MathSymbolType.SET_THEORY),
        "\\forall" to ("∀" to MathSymbolType.LOGIC),
        "\\exists" to ("∃" to MathSymbolType.LOGIC),
        "\\nexists" to ("∄" to MathSymbolType.LOGIC),
        "\\implies" to ("⟹" to MathSymbolType.LOGIC),
        "\\iff" to ("⟺" to MathSymbolType.LOGIC),
        "\\land" to ("∧" to MathSymbolType.LOGIC),
        "\\lor" to ("∨" to MathSymbolType.LOGIC),
        "\\neg" to ("¬" to MathSymbolType.LOGIC),
        "\\le" to ("≤" to MathSymbolType.RELATION),
        "\\leq" to ("≤" to MathSymbolType.RELATION),
        "\\ge" to ("≥" to MathSymbolType.RELATION),
        "\\geq" to ("≥" to MathSymbolType.RELATION),
        "\\ne" to ("≠" to MathSymbolType.RELATION),
        "\\neq" to ("≠" to MathSymbolType.RELATION),
        "\\approx" to ("≈" to MathSymbolType.RELATION),
        "\\equiv" to ("≡" to MathSymbolType.RELATION),
        "\\sim" to ("∼" to MathSymbolType.RELATION),
        "\\simeq" to ("≃" to MathSymbolType.RELATION),
        "\\cong" to ("≅" to MathSymbolType.RELATION),
        "\\propto" to ("∝" to MathSymbolType.RELATION),
        "\\to" to ("→" to MathSymbolType.ARROW),
        "\\rightarrow" to ("→" to MathSymbolType.ARROW),
        "\\leftarrow" to ("←" to MathSymbolType.ARROW),
        "\\Rightarrow" to ("⇒" to MathSymbolType.ARROW),
        "\\Leftarrow" to ("⇐" to MathSymbolType.ARROW),
        "\\Leftrightarrow" to ("⇔" to MathSymbolType.ARROW),
        "\\mapsto" to ("↦" to MathSymbolType.ARROW),
        "\\infty" to ("∞" to MathSymbolType.OPERATOR),
        "\\partial" to ("∂" to MathSymbolType.OPERATOR),
        "\\nabla" to ("∇" to MathSymbolType.OPERATOR),
        "\\angle" to ("∠" to MathSymbolType.OPERATOR),
        "\\mid" to ("|" to MathSymbolType.RELATION),
        "\\parallel" to ("∥" to MathSymbolType.RELATION),
        "\\dots" to ("…" to MathSymbolType.PUNCTUATION),
        "\\cdots" to ("⋯" to MathSymbolType.PUNCTUATION),
        "\\ldots" to ("…" to MathSymbolType.PUNCTUATION),
        "\\vdots" to ("⋮" to MathSymbolType.PUNCTUATION),
        "\\ddots" to ("⋱" to MathSymbolType.PUNCTUATION)
    )

    private val MATHBB_MAP = mapOf(
        "R" to "ℝ", "C" to "ℂ", "N" to "ℕ", "Z" to "ℤ",
        "Q" to "ℚ", "E" to "𝔼", "P" to "ℙ", "H" to "ℍ"
    )

    private val STANDARD_FUNCTIONS = setOf(
        "sin", "cos", "tan", "cot", "sec", "csc",
        "sinh", "cosh", "tanh", "coth",
        "arcsin", "arccos", "arctan",
        "log", "ln", "exp", "det", "dim", "ker",
        "deg", "gcd", "hom", "inf", "sup", "max", "min", "arg"
    )

    /**
     * Parses an input LaTeX expression into a root [MathNode].
     * Strips enclosing math delimiters (`$$`, `$`, `\[`, `\]`, `\(`, `\)`).
     */
    fun parse(rawInput: String): MathNode {
        var input = rawInput.trim()
        if (input.startsWith("$$") && input.endsWith("$$") && input.length >= 4) {
            input = input.substring(2, input.length - 2).trim()
        } else if (input.startsWith("\\[") && input.endsWith("\\]") && input.length >= 4) {
            input = input.substring(2, input.length - 2).trim()
        } else if (input.startsWith("$") && input.endsWith("$") && input.length >= 2) {
            input = input.substring(1, input.length - 1).trim()
        } else if (input.startsWith("\\(") && input.endsWith("\\)") && input.length >= 4) {
            input = input.substring(2, input.length - 2).trim()
        }

        // Remove \displaystyle formatting instruction
        input = input.replace(Regex("""\\displaystyle\s*"""), "")

        if (input.isEmpty()) {
            return MathNode.RowNode(emptyList())
        }

        return try {
            val parser = ParserInstance(input)
            val result = parser.parseExpression()
            if (result.size == 1) result[0] else MathNode.RowNode(result)
        } catch (e: Exception) {
            MathNode.ErrorNode(rawLatex = rawInput, errorMessage = e.message ?: "Invalid LaTeX syntax")
        }
    }

    private class ParserInstance(private val text: String) {
        private var pos = 0
        private val length = text.length

        fun hasMore(): Boolean = pos < length

        fun peek(): Char = if (pos < length) text[pos] else '\u0000'

        fun next(): Char = if (pos < length) text[pos++] else '\u0000'

        fun skipWhitespace() {
            while (hasMore() && text[pos].isWhitespace()) {
                pos++
            }
        }

        fun parseExpression(stopChars: Set<Char> = emptySet(), stopOnEndEnv: Boolean = false): List<MathNode> {
            val nodes = mutableListOf<MathNode>()

            while (hasMore()) {
                skipWhitespace()
                if (!hasMore()) break

                val ch = peek()
                if (stopChars.contains(ch)) {
                    break
                }

                if (stopOnEndEnv && startsWith("\\end{")) {
                    break
                }

                val node = parseAtom()
                if (node != null) {
                    // Check for trailing superscript (^) or subscript (_)
                    val withScripts = parseScripts(node)
                    nodes.add(withScripts)
                }
            }

            return nodes
        }

        private fun parseScripts(base: MathNode): MathNode {
            var superNode: MathNode? = null
            var subNode: MathNode? = null
            var hasScript = false

            while (hasMore()) {
                skipWhitespace()
                if (!hasMore()) break

                if (peek() == '^') {
                    pos++
                    skipWhitespace()
                    superNode = parseGroupOrSingle()
                    hasScript = true
                } else if (peek() == '_') {
                    pos++
                    skipWhitespace()
                    subNode = parseGroupOrSingle()
                    hasScript = true
                } else {
                    break
                }
            }

            return if (hasScript) {
                if (base is MathNode.BigOperatorNode) {
                    base.copy(lowerLimit = subNode ?: base.lowerLimit, upperLimit = superNode ?: base.upperLimit)
                } else {
                    MathNode.ScriptNode(base = base, superscript = superNode, subscript = subNode)
                }
            } else {
                base
            }
        }

        private fun parseGroupOrSingle(): MathNode {
            skipWhitespace()
            if (!hasMore()) return MathNode.TextNode("")

            if (peek() == '{') {
                val groupContent = extractBraceGroup()
                return parse(groupContent)
            } else {
                val atom = parseAtom() ?: MathNode.TextNode("")
                return atom
            }
        }

        private fun parseAtom(): MathNode? {
            skipWhitespace()
            if (!hasMore()) return null

            val ch = peek()

            return when {
                ch == '\\' -> parseMacro()
                ch == '{' -> {
                    val group = extractBraceGroup()
                    parse(group)
                }
                ch == '(' || ch == ')' || ch == '[' || ch == ']' || ch == '+' || ch == '-' || ch == '=' || ch == '<' || ch == '>' || ch == ',' || ch == '.' || ch == ';' || ch == ':' -> {
                    pos++
                    val symType = when (ch) {
                        '+', '-' -> MathSymbolType.OPERATOR
                        '=', '<', '>' -> MathSymbolType.RELATION
                        ',', '.', ';', ':' -> MathSymbolType.PUNCTUATION
                        else -> MathSymbolType.PUNCTUATION
                    }
                    MathNode.SymbolNode(ch.toString(), symType)
                }
                ch.isLetter() -> {
                    pos++
                    MathNode.TextNode(ch.toString(), MathTextStyle.ITALIC)
                }
                ch.isDigit() -> {
                    val start = pos
                    while (hasMore() && peek().isDigit()) {
                        pos++
                    }
                    MathNode.TextNode(text.substring(start, pos), MathTextStyle.NORMAL)
                }
                else -> {
                    pos++
                    MathNode.TextNode(ch.toString(), MathTextStyle.NORMAL)
                }
            }
        }

        private fun parseMacro(): MathNode {
            val macroStart = pos
            pos++ // skip '\\'

            if (!hasMore()) return MathNode.TextNode("\\")

            // Check single character escapes like \, \; \: \! \{ \} \|
            val nextChar = peek()
            if (!nextChar.isLetter()) {
                pos++
                return when (nextChar) {
                    ',', ';', ':' -> MathNode.TextNode(" ", MathTextStyle.NORMAL)
                    '!' -> MathNode.RowNode(emptyList()) // negative space
                    '{' -> MathNode.SymbolNode("{", MathSymbolType.PUNCTUATION)
                    '}' -> MathNode.SymbolNode("}", MathSymbolType.PUNCTUATION)
                    '\\' -> MathNode.SymbolNode("\n", MathSymbolType.PUNCTUATION)
                    '|' -> MathNode.SymbolNode("∥", MathSymbolType.RELATION)
                    else -> MathNode.TextNode(nextChar.toString(), MathTextStyle.NORMAL)
                }
            }

            // Read full macro identifier
            val idStart = pos
            while (hasMore() && peek().isLetter()) {
                pos++
            }
            val macro = text.substring(idStart, pos)
            val fullMacro = "\\$macro"

            // 1. Fractions (\frac{num}{den}, \dfrac)
            if (macro == "frac" || macro == "dfrac") {
                val num = parseGroupOrSingle()
                val den = parseGroupOrSingle()
                return MathNode.FractionNode(numerator = num, denominator = den)
            }

            // 2. Square roots (\sqrt[n]{x}, \sqrt{x})
            if (macro == "sqrt") {
                skipWhitespace()
                var indexNode: MathNode? = null
                if (peek() == '[') {
                    val indexText = extractBracketGroup()
                    indexNode = parse(indexText)
                }
                val radicand = parseGroupOrSingle()
                return MathNode.RadicalNode(radicand = radicand, index = indexNode)
            }

            // 3. Boxed (\boxed{...})
            if (macro == "boxed") {
                val inner = parseGroupOrSingle()
                return MathNode.BoxedNode(inner = inner)
            }

            // 4. Delimiters (\left ... \right)
            if (macro == "left") {
                skipWhitespace()
                val leftDelim = if (hasMore()) {
                    if (peek() == '\\') {
                        pos++
                        if (hasMore() && (peek() == '{' || peek() == '}')) next().toString() else "{"
                    } else next().toString()
                } else "("

                // Parse content until matching \right
                val contentNodes = mutableListOf<MathNode>()
                var rightDelim = ")"

                while (hasMore()) {
                    skipWhitespace()
                    if (startsWith("\\right")) {
                        pos += 6
                        skipWhitespace()
                        rightDelim = if (hasMore()) {
                            if (peek() == '\\') {
                                pos++
                                if (hasMore() && (peek() == '{' || peek() == '}')) next().toString() else "}"
                            } else next().toString()
                        } else ")"
                        break
                    }
                    val atom = parseAtom()
                    if (atom != null) {
                        contentNodes.add(parseScripts(atom))
                    }
                }
                val contentNode = if (contentNodes.size == 1) contentNodes[0] else MathNode.RowNode(contentNodes)
                return MathNode.DelimiterNode(leftDelimiter = leftDelim, rightDelimiter = rightDelim, content = contentNode)
            }

            // 5. Environments (\begin{cases}, \begin{matrix}, \begin{pmatrix}, \begin{bmatrix}, \begin{aligned})
            if (macro == "begin") {
                val envName = extractBraceGroup()
                return parseEnvironment(envName)
            }

            // 6. Big Operators and Calculus
            if (macro == "sum" || macro == "prod" || macro == "coprod") {
                val sym = when (macro) {
                    "sum" -> "∑"
                    "prod" -> "∏"
                    else -> "∐"
                }
                return MathNode.BigOperatorNode(operatorSymbol = sym, isIntegral = false)
            }
            if (macro == "int" || macro == "iint" || macro == "iiint" || macro == "oint") {
                val sym = when (macro) {
                    "iint" -> "∬"
                    "iiint" -> "∭"
                    "oint" -> "∮"
                    else -> "∫"
                }
                return MathNode.BigOperatorNode(operatorSymbol = sym, isIntegral = true)
            }
            if (macro == "lim") {
                return MathNode.BigOperatorNode(operatorSymbol = "lim", isIntegral = false)
            }

            // 7. Accents (\vec, \hat, \bar, \dot)
            if (macro == "vec" || macro == "hat" || macro == "bar" || macro == "dot") {
                val inner = extractBraceOrSingleChar()
                val decorated = when (macro) {
                    "vec" -> inner + "\u20D7"
                    "hat" -> inner + "\u0302"
                    "bar" -> inner + "\u0304"
                    "dot" -> inner + "\u0307"
                    else -> inner
                }
                return MathNode.TextNode(decorated, MathTextStyle.NORMAL)
            }

            // 8. Fonts and Text styling (\text, \mathbf, \mathbb, \mathrm, \mathcal, \mathit)
            if (macro == "text" || macro == "mathrm") {
                val textContent = extractBraceGroup()
                return MathNode.TextNode(textContent, MathTextStyle.TEXT)
            }
            if (macro == "mathbf") {
                val textContent = extractBraceOrSingleChar()
                return MathNode.TextNode(textContent, MathTextStyle.BOLD)
            }
            if (macro == "mathit") {
                val textContent = extractBraceOrSingleChar()
                return MathNode.TextNode(textContent, MathTextStyle.ITALIC)
            }
            if (macro == "mathbb") {
                val char = extractBraceOrSingleChar()
                val mathbbChar = MATHBB_MAP[char] ?: char
                return MathNode.TextNode(mathbbChar, MathTextStyle.MATHBB)
            }
            if (macro == "mathcal") {
                val char = extractBraceOrSingleChar()
                return MathNode.TextNode(char, MathTextStyle.MATHCAL)
            }

            // 9. Greek Letters
            val greek = GREEK_LETTERS[fullMacro]
            if (greek != null) {
                return MathNode.SymbolNode(greek, MathSymbolType.GREEK)
            }

            // 10. Operators and Relations
            val opRel = OPERATORS_AND_RELATIONS[fullMacro]
            if (opRel != null) {
                return MathNode.SymbolNode(opRel.first, opRel.second)
            }

            // 11. Standard Math Functions (\sin, \cos, \ln, etc.)
            if (STANDARD_FUNCTIONS.contains(macro)) {
                return MathNode.TextNode(macro, MathTextStyle.MATHRM)
            }

            // 12. Spacing macros
            if (macro == "quad" || macro == "qquad") {
                val spaces = if (macro == "qquad") "    " else "  "
                return MathNode.TextNode(spaces, MathTextStyle.NORMAL)
            }

            // Fallback: Return raw macro text safely
            return MathNode.TextNode(macro, MathTextStyle.NORMAL)
        }

        private fun parseEnvironment(envName: String): MathNode {
            val startMarker = "\\begin{$envName}"
            val endMarker = "\\end{$envName}"
            var depth = 1
            var scanPos = pos
            var endIdx = -1

            while (scanPos < length) {
                val nextStart = text.indexOf(startMarker, scanPos)
                val nextEnd = text.indexOf(endMarker, scanPos)

                if (nextEnd == -1) {
                    break
                }

                if (nextStart != -1 && nextStart < nextEnd) {
                    depth++
                    scanPos = nextStart + startMarker.length
                } else {
                    depth--
                    if (depth == 0) {
                        endIdx = nextEnd
                        break
                    }
                    scanPos = nextEnd + endMarker.length
                }
            }

            val bodyText = if (endIdx != -1) {
                val content = text.substring(pos, endIdx)
                pos = endIdx + endMarker.length
                content
            } else {
                val content = text.substring(pos)
                pos = length
                content
            }

            return when (envName) {
                "cases" -> {
                    val rawRows = splitEnvironmentRows(bodyText)
                    val casesList = rawRows.map { row ->
                        val parts = splitRowColumns(row)
                        val expression = parse(parts[0].trim())
                        val condition = if (parts.size > 1) parse(parts[1].trim()) else null
                        expression to condition
                    }
                    MathNode.CasesNode(cases = casesList)
                }
                "aligned", "align" -> {
                    val rawRows = splitEnvironmentRows(bodyText)
                    val lines = rawRows.map { row ->
                        splitRowColumns(row).map { parse(it.trim()) }
                    }
                    MathNode.AlignedNode(lines = lines)
                }
                "matrix", "pmatrix", "bmatrix", "vmatrix", "Vmatrix", "Bmatrix" -> {
                    val bracket = when (envName) {
                        "pmatrix" -> MatrixBracketType.PARENTHESES
                        "bmatrix" -> MatrixBracketType.BRACKETS
                        "vmatrix" -> MatrixBracketType.VERTICAL_BARS
                        "Vmatrix" -> MatrixBracketType.DOUBLE_VERTICAL_BARS
                        "Bmatrix" -> MatrixBracketType.CURLY_BRACES
                        else -> MatrixBracketType.NONE
                    }
                    val rawRows = splitEnvironmentRows(bodyText)
                    val matrixRows = rawRows.map { row ->
                        splitRowColumns(row).map { parse(it.trim()) }
                    }
                    MathNode.MatrixNode(rows = matrixRows, bracketType = bracket)
                }
                else -> {
                    parse(bodyText)
                }
            }
        }

        private fun splitEnvironmentRows(content: String): List<String> {
            val rows = mutableListOf<String>()
            var current = StringBuilder()
            var braceDepth = 0
            var envDepth = 0
            var i = 0
            val len = content.length

            while (i < len) {
                if (content.startsWith("\\begin{", i)) {
                    envDepth++
                    current.append("\\begin{")
                    i += 7
                    continue
                }
                if (content.startsWith("\\end{", i)) {
                    if (envDepth > 0) envDepth--
                    current.append("\\end{")
                    i += 5
                    continue
                }
                val ch = content[i]
                if (ch == '{') {
                    braceDepth++
                    current.append(ch)
                    i++
                    continue
                }
                if (ch == '}') {
                    if (braceDepth > 0) braceDepth--
                    current.append(ch)
                    i++
                    continue
                }
                if (braceDepth == 0 && envDepth == 0 && content.startsWith("\\\\", i)) {
                    rows.add(current.toString().trim())
                    current = StringBuilder()
                    i += 2
                    // Skip optional spacing argument like \\[6pt] or \\[1em]
                    while (i < len && content[i].isWhitespace()) i++
                    if (i < len && content[i] == '[') {
                        val closeBracket = content.indexOf(']', i)
                        if (closeBracket != -1) {
                            i = closeBracket + 1
                        }
                    }
                    continue
                }

                current.append(ch)
                i++
            }

            val lastRow = current.toString().trim()
            if (lastRow.isNotEmpty()) {
                rows.add(lastRow)
            }
            return rows.filter { it.isNotBlank() }
        }

        private fun splitRowColumns(row: String): List<String> {
            val cols = mutableListOf<String>()
            var current = StringBuilder()
            var braceDepth = 0
            var envDepth = 0
            var i = 0
            val len = row.length

            while (i < len) {
                if (row.startsWith("\\begin{", i)) {
                    envDepth++
                    current.append("\\begin{")
                    i += 7
                    continue
                }
                if (row.startsWith("\\end{", i)) {
                    if (envDepth > 0) envDepth--
                    current.append("\\end{")
                    i += 5
                    continue
                }
                val ch = row[i]
                if (ch == '{') {
                    braceDepth++
                    current.append(ch)
                    i++
                    continue
                }
                if (ch == '}') {
                    if (braceDepth > 0) braceDepth--
                    current.append(ch)
                    i++
                    continue
                }
                if (ch == '&' && braceDepth == 0 && envDepth == 0) {
                    cols.add(current.toString().trim())
                    current = StringBuilder()
                    i++
                    continue
                }

                current.append(ch)
                i++
            }

            cols.add(current.toString().trim())
            return cols
        }

        private fun startsWith(prefix: String): Boolean {
            return text.regionMatches(pos, prefix, 0, prefix.length, ignoreCase = false)
        }

        private fun extractBraceGroup(): String {
            skipWhitespace()
            if (!hasMore()) return ""
            if (peek() != '{') {
                return next().toString()
            }
            pos++ // skip '{'
            val start = pos
            var depth = 1
            while (hasMore() && depth > 0) {
                val ch = next()
                if (ch == '{') depth++
                else if (ch == '}') depth--
            }
            val end = if (depth == 0) pos - 1 else pos
            return text.substring(start, end)
        }

        private fun extractBracketGroup(): String {
            skipWhitespace()
            if (!hasMore()) return ""
            if (peek() != '[') return ""
            pos++ // skip '['
            val start = pos
            var depth = 1
            while (hasMore() && depth > 0) {
                val ch = next()
                if (ch == '[') depth++
                else if (ch == ']') depth--
            }
            val end = if (depth == 0) pos - 1 else pos
            return text.substring(start, end)
        }

        private fun extractBraceOrSingleChar(): String {
            skipWhitespace()
            if (!hasMore()) return ""
            return if (peek() == '{') {
                extractBraceGroup()
            } else {
                next().toString()
            }
        }
    }
}
