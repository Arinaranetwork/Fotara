// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.expression

import java.util.Locale

/**
 * Execution context provided when parsing and resolving expression tokens.
 */
data class ExpressionContext(
    val timestamp: Long = System.currentTimeMillis(),
    val page: Int? = null,
    val pages: Int? = null,
    val title: String? = null,
    val folder: String? = null,
    val userName: String? = null,
    val appName: String = "Fotara",
    val appVersion: String = "2.0.0",
    val scopeId: String? = null,
    val locale: Locale = Locale.getDefault(),
    val dateFormat: String = "dd/MM/yyyy",
    val timeFormat: String = "HH:mm",
    val counterProvider: ((scopeId: String?) -> Int)? = null,
    val isFileName: Boolean = false
)
