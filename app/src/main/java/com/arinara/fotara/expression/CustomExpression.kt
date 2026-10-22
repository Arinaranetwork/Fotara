// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.expression

/**
 * Type of custom expression defined by the user.
 */
enum class ExpressionType {
    /**
     * Type A: A custom word that resolves to static or templated text.
     * Example: !sig -> "Created by Andi"
     */
    WORD,

    /**
     * Type B: A combination of up to 5 expressions with plain text connectors.
     * Example: !log -> "!day + !date + !time"
     */
    COMBINATION
}

/**
 * Category grouping for built-in and custom expressions.
 */
enum class ExpressionCategory {
    TIME,
    CONTEXT,
    SEQUENCE,
    TRANSFORM,
    CUSTOM
}

/**
 * Entity representing a user-defined custom expression.
 */
data class CustomExpression(
    val id: Long = 0,
    val name: String,
    val type: ExpressionType,
    val value: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Definition and documentation for an expression token.
 */
data class ExpressionDefinition(
    val token: String,
    val title: String,
    val description: String,
    val category: ExpressionCategory,
    val example: String,
    val supportsCustomFormat: Boolean = false,
    val supportsDynamic: Boolean = false,
    val isEnabled: Boolean = true
) {
    val displayToken: String get() = "!$token"
}
