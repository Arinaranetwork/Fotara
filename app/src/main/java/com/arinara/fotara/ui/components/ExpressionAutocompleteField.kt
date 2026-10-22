// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.arinara.fotara.expression.CustomExpression
import com.arinara.fotara.expression.ExpressionContext
import com.arinara.fotara.expression.ExpressionDefinition
import com.arinara.fotara.expression.ExpressionEngine
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import com.arinara.fotara.ui.theme.jigglePressClickable

/**
 * Text field wrapper with autocomplete suggestions triggered by '!' and real-time live preview.
 * Fully compliant with Electric Slate Dark Theme tokens, shape tokens, and TalkBack accessibility.
 */
@Composable
fun ExpressionAutocompleteField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    textStyle: TextStyle = TextStyle(
        color = ElectricSlateTokens.TextPrimary,
        fontSize = 14.sp
    ),
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else 4,
    context: ExpressionContext = ExpressionContext(),
    isFileName: Boolean = false,
    enabled: Boolean = true,
    showPreview: Boolean = true,
    customExpressions: List<CustomExpression> = emptyList(),
    disabledBuiltIns: Set<String> = emptySet(),
    focusRequester: FocusRequester? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    var isFocused by remember { mutableStateOf(false) }

    // Detect active '!' query from current cursor position
    val activeTrigger = remember(value.text, value.selection) {
        findExpressionTrigger(value.text, value.selection.start)
    }

    val suggestions = remember(activeTrigger, customExpressions, disabledBuiltIns) {
        if (activeTrigger != null) {
            ExpressionEngine.getSuggestions(
                query = activeTrigger.query,
                customExpressions = customExpressions,
                disabledBuiltIns = disabledBuiltIns
            )
        } else {
            emptyList()
        }
    }

    val showSuggestionsPopup = isFocused && activeTrigger != null && suggestions.isNotEmpty()

    // Real-time preview calculation
    val previewText = remember(value.text, context, isFileName, customExpressions, disabledBuiltIns) {
        if (value.text.contains("!")) {
            val resolved = ExpressionEngine.resolve(
                input = value.text,
                context = context.copy(isFileName = isFileName),
                customExpressions = customExpressions,
                disabledBuiltIns = disabledBuiltIns
            )
            if (isFileName) {
                ExpressionEngine.sanitizeFileName(resolved)
            } else {
                resolved
            }
        } else {
            ""
        }
    }

    val hasMeaningfulPreview = previewText.isNotBlank() && previewText != value.text

    Column(modifier = modifier.fillMaxWidth()) {
        // Input Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ElectricSlateTokens.SurfacePanel)
                .border(
                    width = 1.dp,
                    color = if (isFocused) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.BorderSubtle,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = singleLine,
                maxLines = maxLines,
                textStyle = textStyle,
                cursorBrush = SolidColor(ElectricSlateTokens.PrimaryAccent),
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                    .onFocusChanged { isFocused = it.isFocused },
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (value.text.isEmpty() && placeholder.isNotEmpty()) {
                            Text(
                                text = placeholder,
                                color = ElectricSlateTokens.TextMuted,
                                fontSize = textStyle.fontSize,
                                fontFamily = textStyle.fontFamily
                            )
                        }
                        innerTextField()
                    }
                }
            )

            // Autocomplete Popup attached to input box
            DropdownMenu(
                expanded = showSuggestionsPopup,
                onDismissRequest = { /* Keep open while user types */ },
                properties = PopupProperties(focusable = false),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .heightIn(max = 240.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ElectricSlateTokens.SurfaceElevated)
                    .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(16.dp))
                    .padding(vertical = 4.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 230.dp)
                ) {
                    items(suggestions, key = { it.token }) { suggestion ->
                        SuggestionItemRow(
                            suggestion = suggestion,
                            onClick = {
                                if (activeTrigger != null) {
                                    val before = value.text.substring(0, activeTrigger.startIndex)
                                    val after = value.text.substring(activeTrigger.cursorIndex)
                                    val replacement = "!${suggestion.token} "
                                    val newText = before + replacement + after
                                    val newCursor = before.length + replacement.length
                                    onValueChange(
                                        TextFieldValue(
                                            text = newText,
                                            selection = TextRange(newCursor)
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        // Live Preview Chip
        AnimatedVisibility(
            visible = showPreview && hasMeaningfulPreview,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(999.dp))
                        .background(ElectricSlateTokens.SurfaceElevated)
                        .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(999.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .semantics {
                            contentDescription = "Live resolved preview: $previewText"
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = null,
                        tint = ElectricSlateTokens.PrimaryAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Preview:",
                        color = ElectricSlateTokens.TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = previewText,
                        color = ElectricSlateTokens.TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Convenience String overload for ExpressionAutocompleteField.
 */
@Composable
fun ExpressionAutocompleteField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    textStyle: TextStyle = TextStyle(
        color = ElectricSlateTokens.TextPrimary,
        fontSize = 14.sp
    ),
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else 4,
    context: ExpressionContext = ExpressionContext(),
    isFileName: Boolean = false,
    enabled: Boolean = true,
    showPreview: Boolean = true,
    customExpressions: List<CustomExpression> = emptyList(),
    disabledBuiltIns: Set<String> = emptySet(),
    focusRequester: FocusRequester? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    var textFieldValue by remember(value) {
        mutableStateOf(
            TextFieldValue(
                text = value,
                selection = TextRange(value.length)
            )
        )
    }

    // Keep internal text state in sync if external value changed
    if (textFieldValue.text != value) {
        textFieldValue = textFieldValue.copy(
            text = value,
            selection = TextRange(value.length)
        )
    }

    ExpressionAutocompleteField(
        value = textFieldValue,
        onValueChange = { newValue ->
            textFieldValue = newValue
            if (newValue.text != value) {
                onValueChange(newValue.text)
            }
        },
        modifier = modifier,
        placeholder = placeholder,
        textStyle = textStyle,
        singleLine = singleLine,
        maxLines = maxLines,
        context = context,
        isFileName = isFileName,
        enabled = enabled,
        showPreview = showPreview,
        customExpressions = customExpressions,
        disabledBuiltIns = disabledBuiltIns,
        focusRequester = focusRequester,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions
    )
}

@Composable
private fun SuggestionItemRow(
    suggestion: ExpressionDefinition,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .jigglePressClickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .semantics {
                contentDescription = "Insert expression ${suggestion.token}, ${suggestion.description}"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "!${suggestion.token}",
                    color = ElectricSlateTokens.PrimaryAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = suggestion.title,
                    color = ElectricSlateTokens.TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = suggestion.description,
                color = ElectricSlateTokens.TextMuted,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(ElectricSlateTokens.TrackInactive)
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = suggestion.category.name,
                color = ElectricSlateTokens.TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private data class TriggerInfo(
    val startIndex: Int,
    val cursorIndex: Int,
    val query: String
)

private fun findExpressionTrigger(text: String, cursor: Int): TriggerInfo? {
    if (cursor <= 0 || cursor > text.length) return null

    var i = cursor - 1
    while (i >= 0) {
        val char = text[i]
        if (char == '!') {
            // Check if escaped with \
            if (i > 0 && text[i - 1] == '\\') {
                return null
            }
            val query = text.substring(i + 1, cursor)
            // Query must only contain identifier chars (letters, digits, _)
            if (query.all { it.isLetterOrDigit() || it == '_' }) {
                return TriggerInfo(
                    startIndex = i,
                    cursorIndex = cursor,
                    query = query
                )
            } else {
                return null
            }
        } else if (!char.isLetterOrDigit() && char != '_') {
            // Hit a separator before finding !
            return null
        }
        i--
    }
    return null
}
