// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.note.math

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.theme.ElmsSans

/**
 * Pure Jetpack Compose Mathematical View component.
 * Delivers hardware-accelerated, offline mathematical typesetting for STEM notes.
 * Strictly zero WebView, zero JavaScript runtime, and zero external network dependencies.
 */
@Composable
fun ComposeMathView(
    latex: String,
    modifier: Modifier = Modifier,
    isDisplayMode: Boolean = true,
    textColor: Color = Color.Unspecified,
    fontSize: TextUnit = if (isDisplayMode) 16.sp else 14.sp
) {
    val resolvedColor = if (textColor != Color.Unspecified) textColor else LocalContentColor.current
    val effectiveColor = if (resolvedColor != Color.Unspecified) resolvedColor else Color(0xFFEAE3D2)

    val rootNode = remember(latex) {
        LatexAstParser.parse(latex)
    }

    if (rootNode is MathNode.ErrorNode) {
        MathErrorCard(
            error = rootNode,
            modifier = modifier
        )
        return
    }

    if (isDisplayMode) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .horizontalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            MathNodeRenderer(
                node = rootNode,
                isDisplayMode = true,
                color = effectiveColor,
                fontSize = fontSize
            )
        }
    } else {
        Box(modifier = modifier) {
            MathNodeRenderer(
                node = rootNode,
                isDisplayMode = false,
                color = effectiveColor,
                fontSize = fontSize
            )
        }
    }
}

@Composable
fun MathNodeRenderer(
    node: MathNode,
    isDisplayMode: Boolean,
    color: Color,
    fontSize: TextUnit,
    modifier: Modifier = Modifier
) {
    when (node) {
        is MathNode.TextNode -> {
            val fontStyle = when (node.style) {
                MathTextStyle.ITALIC -> FontStyle.Italic
                else -> FontStyle.Normal
            }
            val fontWeight = when (node.style) {
                MathTextStyle.BOLD -> FontWeight.Bold
                else -> FontWeight.Normal
            }
            val fontFamily = when (node.style) {
                MathTextStyle.TEXT, MathTextStyle.MATHRM -> ElmsSans
                MathTextStyle.ITALIC -> FontFamily.Serif
                else -> ElmsSans
            }

            Text(
                text = node.text,
                color = color,
                fontSize = fontSize,
                fontFamily = fontFamily,
                fontStyle = fontStyle,
                fontWeight = fontWeight,
                modifier = modifier
            )
        }

        is MathNode.SymbolNode -> {
            val hPadding = when (node.type) {
                MathSymbolType.OPERATOR, MathSymbolType.RELATION, MathSymbolType.ARROW -> 3.dp
                MathSymbolType.LOGIC, MathSymbolType.SET_THEORY -> 2.5.dp
                MathSymbolType.PUNCTUATION -> 1.dp
                else -> 1.dp
            }
            Text(
                text = node.symbol,
                color = color,
                fontSize = fontSize,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Medium,
                modifier = modifier.padding(horizontal = hPadding)
            )
        }

        is MathNode.FractionNode -> {
            Column(
                modifier = modifier
                    .padding(horizontal = 2.dp)
                    .width(IntrinsicSize.Min),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(modifier = Modifier.padding(bottom = 2.dp), contentAlignment = Alignment.Center) {
                    MathNodeRenderer(
                        node = node.numerator,
                        isDisplayMode = isDisplayMode,
                        color = color,
                        fontSize = (fontSize.value * 0.9f).sp
                    )
                }

                // Fraction horizontal divider line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.5.dp)
                        .background(color)
                )

                Box(modifier = Modifier.padding(top = 2.dp), contentAlignment = Alignment.Center) {
                    MathNodeRenderer(
                        node = node.denominator,
                        isDisplayMode = isDisplayMode,
                        color = color,
                        fontSize = (fontSize.value * 0.9f).sp
                    )
                }
            }
        }

        is MathNode.RadicalNode -> {
            Row(
                modifier = modifier.padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Optional index in upper-left crook (e.g. 3 in cube root)
                if (node.index != null) {
                    Box(modifier = Modifier.padding(end = 1.dp, bottom = (fontSize.value * 0.4f).dp)) {
                        MathNodeRenderer(
                            node = node.index,
                            isDisplayMode = isDisplayMode,
                            color = color,
                            fontSize = (fontSize.value * 0.6f).sp
                        )
                    }
                }

                // Square root radical sign
                Text(
                    text = "√",
                    color = color,
                    fontSize = (fontSize.value * 1.25f).sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(end = 1.dp)
                )

                // Radicand with top vinculum line
                Box(
                    modifier = Modifier
                        .padding(top = 1.dp)
                        .drawBehind {
                            drawLine(
                                color = color,
                                start = Offset(0f, 0f),
                                end = Offset(size.width, 0f),
                                strokeWidth = 1.5.dp.toPx()
                            )
                        }
                        .padding(top = 3.dp, start = 2.dp, end = 2.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    MathNodeRenderer(
                        node = node.radicand,
                        isDisplayMode = isDisplayMode,
                        color = color,
                        fontSize = fontSize
                    )
                }
            }
        }

        is MathNode.ScriptNode -> {
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MathNodeRenderer(
                    node = node.base,
                    isDisplayMode = isDisplayMode,
                    color = color,
                    fontSize = fontSize
                )

                if (node.superscript != null && node.subscript != null) {
                    Column(
                        modifier = Modifier.padding(start = 1.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        MathNodeRenderer(
                            node = node.superscript,
                            isDisplayMode = isDisplayMode,
                            color = color,
                            fontSize = (fontSize.value * 0.68f).sp
                        )
                        MathNodeRenderer(
                            node = node.subscript,
                            isDisplayMode = isDisplayMode,
                            color = color,
                            fontSize = (fontSize.value * 0.68f).sp
                        )
                    }
                } else if (node.superscript != null) {
                    Box(
                        modifier = Modifier.padding(start = 1.dp, bottom = (fontSize.value * 0.45f).dp)
                    ) {
                        MathNodeRenderer(
                            node = node.superscript,
                            isDisplayMode = isDisplayMode,
                            color = color,
                            fontSize = (fontSize.value * 0.72f).sp
                        )
                    }
                } else if (node.subscript != null) {
                    Box(
                        modifier = Modifier.padding(start = 1.dp, top = (fontSize.value * 0.35f).dp)
                    ) {
                        MathNodeRenderer(
                            node = node.subscript,
                            isDisplayMode = isDisplayMode,
                            color = color,
                            fontSize = (fontSize.value * 0.72f).sp
                        )
                    }
                }
            }
        }

        is MathNode.BigOperatorNode -> {
            if (node.isIntegral) {
                Row(
                    modifier = modifier.padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = node.operatorSymbol,
                        color = color,
                        fontSize = (fontSize.value * 1.5f).sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Normal
                    )

                    if (node.upperLimit != null || node.lowerLimit != null) {
                        Column(
                            modifier = Modifier.padding(start = 1.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (node.upperLimit != null) {
                                MathNodeRenderer(
                                    node = node.upperLimit,
                                    isDisplayMode = isDisplayMode,
                                    color = color,
                                    fontSize = (fontSize.value * 0.68f).sp
                                )
                            }
                            if (node.lowerLimit != null) {
                                MathNodeRenderer(
                                    node = node.lowerLimit,
                                    isDisplayMode = isDisplayMode,
                                    color = color,
                                    fontSize = (fontSize.value * 0.68f).sp
                                )
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = modifier.padding(horizontal = 3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (node.upperLimit != null) {
                        MathNodeRenderer(
                            node = node.upperLimit,
                            isDisplayMode = isDisplayMode,
                            color = color,
                            fontSize = (fontSize.value * 0.68f).sp
                        )
                    }

                    Text(
                        text = node.operatorSymbol,
                        color = color,
                        fontSize = (fontSize.value * 1.35f).sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Medium
                    )

                    if (node.lowerLimit != null) {
                        MathNodeRenderer(
                            node = node.lowerLimit,
                            isDisplayMode = isDisplayMode,
                            color = color,
                            fontSize = (fontSize.value * 0.68f).sp
                        )
                    }
                }
            }
        }

        is MathNode.MatrixNode -> {
            val (openBracket, closeBracket) = when (node.bracketType) {
                MatrixBracketType.PARENTHESES -> "(" to ")"
                MatrixBracketType.BRACKETS -> "[" to "]"
                MatrixBracketType.VERTICAL_BARS -> "|" to "|"
                MatrixBracketType.DOUBLE_VERTICAL_BARS -> "‖" to "‖"
                MatrixBracketType.CURLY_BRACES -> "{" to "}"
                MatrixBracketType.NONE -> "" to ""
            }

            Row(
                modifier = modifier.padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (openBracket.isNotEmpty()) {
                    Text(
                        text = openBracket,
                        color = color,
                        fontSize = (fontSize.value * (1.1f + node.rows.size * 0.4f)).coerceAtMost(fontSize.value * 3.5f).sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Light,
                        modifier = Modifier.padding(end = 2.dp)
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    node.rows.forEach { rowCells ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            rowCells.forEach { cellNode ->
                                Box(
                                    contentAlignment = Alignment.Center
                                ) {
                                    MathNodeRenderer(
                                        node = cellNode,
                                        isDisplayMode = isDisplayMode,
                                        color = color,
                                        fontSize = fontSize
                                    )
                                }
                            }
                        }
                    }
                }

                if (closeBracket.isNotEmpty()) {
                    Text(
                        text = closeBracket,
                        color = color,
                        fontSize = (fontSize.value * (1.1f + node.rows.size * 0.4f)).coerceAtMost(fontSize.value * 3.5f).sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Light,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
            }
        }

        is MathNode.CasesNode -> {
            Row(
                modifier = modifier.padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "{",
                    color = color,
                    fontSize = (fontSize.value * (1.2f + node.cases.size * 0.5f)).coerceAtMost(fontSize.value * 3.5f).sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.padding(end = 4.dp)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    node.cases.forEach { (expression, condition) ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MathNodeRenderer(
                                node = expression,
                                isDisplayMode = isDisplayMode,
                                color = color,
                                fontSize = fontSize
                            )

                            if (condition != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "if ",
                                        color = color.copy(alpha = 0.8f),
                                        fontSize = fontSize,
                                        fontFamily = ElmsSans
                                    )
                                    MathNodeRenderer(
                                        node = condition,
                                        isDisplayMode = isDisplayMode,
                                        color = color,
                                        fontSize = fontSize
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        is MathNode.BoxedNode -> {
            Box(
                modifier = modifier
                    .border(1.5.dp, color, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                MathNodeRenderer(
                    node = node.inner,
                    isDisplayMode = isDisplayMode,
                    color = color,
                    fontSize = fontSize
                )
            }
        }

        is MathNode.DelimiterNode -> {
            Row(
                modifier = modifier.padding(horizontal = 1.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (node.leftDelimiter != ".") {
                    Text(
                        text = node.leftDelimiter,
                        color = color,
                        fontSize = (fontSize.value * 1.25f).sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Light
                    )
                }

                Box(modifier = Modifier.padding(horizontal = 2.dp)) {
                    MathNodeRenderer(
                        node = node.content,
                        isDisplayMode = isDisplayMode,
                        color = color,
                        fontSize = fontSize
                    )
                }

                if (node.rightDelimiter != ".") {
                    Text(
                        text = node.rightDelimiter,
                        color = color,
                        fontSize = (fontSize.value * 1.25f).sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Light
                    )
                }
            }
        }

        is MathNode.AlignedNode -> {
            Column(
                modifier = modifier.padding(vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                node.lines.forEach { lineParts ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        lineParts.forEach { part ->
                            MathNodeRenderer(
                                node = part,
                                isDisplayMode = isDisplayMode,
                                color = color,
                                fontSize = fontSize
                            )
                        }
                    }
                }
            }
        }

        is MathNode.RowNode -> {
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                node.children.forEach { child ->
                    MathNodeRenderer(
                        node = child,
                        isDisplayMode = isDisplayMode,
                        color = color,
                        fontSize = fontSize
                    )
                }
            }
        }

        is MathNode.ErrorNode -> {
            MathErrorCard(error = node, modifier = modifier)
        }
    }
}

@Composable
private fun MathErrorCard(
    error: MathNode.ErrorNode,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF261014))
            .border(1.dp, Color(0xFFE63946).copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column {
            Text(
                text = "LaTeX Syntax Error: ${error.errorMessage}",
                color = Color(0xFFE63946),
                fontSize = 11.sp,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = error.rawLatex,
                color = Color(0xFFEAE3D2),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
