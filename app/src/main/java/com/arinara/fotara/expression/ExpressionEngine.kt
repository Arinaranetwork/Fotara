// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.expression

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Result of custom expression validation.
 */
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)

/**
 * Pure, self-contained parser and resolver engine for Expression Title tokens.
 */
object ExpressionEngine {

    const val MAX_COMBINATION_EXPRESSIONS = 5
    const val MAX_EXPANSION_DEPTH = 5
    const val MAX_CUSTOM_NAME_LENGTH = 24

    private val NAME_REGEX = Regex("^[a-zA-Z0-9_]{1,$MAX_CUSTOM_NAME_LENGTH}$")
    private val FORBIDDEN_FILE_CHARS = Regex("[/\\\\:*?\"<>|]")

    /**
     * Built-in expression catalog with documentation, examples, and capabilities.
     */
    val BUILT_IN_DEFINITIONS: List<ExpressionDefinition> = listOf(
        // Time
        ExpressionDefinition(
            token = "date",
            title = "Current Date",
            description = "Resolves to the current date using default or custom format (e.g. !date:yyyy-MM-dd)",
            category = ExpressionCategory.TIME,
            example = "!date or !date:ddMMyy",
            supportsCustomFormat = true,
            supportsDynamic = true
        ),
        ExpressionDefinition(
            token = "time",
            title = "Current Time",
            description = "Resolves to the current time using default or custom format (e.g. !time:HHmm)",
            category = ExpressionCategory.TIME,
            example = "!time or !time:HH:mm:ss",
            supportsCustomFormat = true,
            supportsDynamic = true
        ),
        ExpressionDefinition(
            token = "datetime",
            title = "Date & Time",
            description = "Resolves to the combined date and time",
            category = ExpressionCategory.TIME,
            example = "!datetime or !datetime:yyyy-MM-dd HH:mm",
            supportsCustomFormat = true,
            supportsDynamic = true
        ),
        ExpressionDefinition(
            token = "day",
            title = "Day Name",
            description = "Full name of the current day (e.g. Wednesday)",
            category = ExpressionCategory.TIME,
            example = "!day",
            supportsDynamic = true
        ),
        ExpressionDefinition(
            token = "month",
            title = "Month Name",
            description = "Full name of the current month (e.g. October)",
            category = ExpressionCategory.TIME,
            example = "!month",
            supportsDynamic = true
        ),
        ExpressionDefinition(
            token = "year",
            title = "Current Year",
            description = "Four-digit current year (e.g. 2026)",
            category = ExpressionCategory.TIME,
            example = "!year",
            supportsDynamic = true
        ),
        ExpressionDefinition(
            token = "week",
            title = "Week Number",
            description = "Current week number of the year (e.g. W43)",
            category = ExpressionCategory.TIME,
            example = "!week",
            supportsDynamic = true
        ),

        // Context
        ExpressionDefinition(
            token = "page",
            title = "Page Number",
            description = "Current active page number (primarily in PDF documents)",
            category = ExpressionCategory.CONTEXT,
            example = "!page"
        ),
        ExpressionDefinition(
            token = "pages",
            title = "Total Pages",
            description = "Total page count of the document",
            category = ExpressionCategory.CONTEXT,
            example = "!pages"
        ),
        ExpressionDefinition(
            token = "title",
            title = "Current Title",
            description = "Title of the containing note, file, or active document",
            category = ExpressionCategory.CONTEXT,
            example = "!title"
        ),
        ExpressionDefinition(
            token = "folder",
            title = "Folder Name",
            description = "Name of the containing subject or workspace folder",
            category = ExpressionCategory.CONTEXT,
            example = "!folder"
        ),
        ExpressionDefinition(
            token = "app",
            title = "App Info",
            description = "App name and version (e.g. Fotara 2.0.0)",
            category = ExpressionCategory.CONTEXT,
            example = "!app"
        ),
        ExpressionDefinition(
            token = "user",
            title = "User Name",
            description = "Name from the user profile if configured, else plain text",
            category = ExpressionCategory.CONTEXT,
            example = "!user"
        ),

        // Sequence & Random
        ExpressionDefinition(
            token = "n",
            title = "Auto Counter",
            description = "Auto-incrementing sequential counter stored per folder scope",
            category = ExpressionCategory.SEQUENCE,
            example = "!n"
        ),
        ExpressionDefinition(
            token = "id",
            title = "Unique Code",
            description = "Short unique 6-character alphanumeric identifier",
            category = ExpressionCategory.SEQUENCE,
            example = "!id"
        ),
        ExpressionDefinition(
            token = "rand",
            title = "Random Number",
            description = "Random 3-digit number (100..999)",
            category = ExpressionCategory.SEQUENCE,
            example = "!rand"
        ),

        // Text Transforms
        ExpressionDefinition(
            token = "upper",
            title = "Uppercase Transform",
            description = "Converts the following text or entire title to UPPERCASE",
            category = ExpressionCategory.TRANSFORM,
            example = "!upper !folder or !upper:text"
        ),
        ExpressionDefinition(
            token = "lower",
            title = "Lowercase Transform",
            description = "Converts the following text or entire title to lowercase",
            category = ExpressionCategory.TRANSFORM,
            example = "!lower !folder or !lower:text"
        ),
        ExpressionDefinition(
            token = "cap",
            title = "Capitalize Transform",
            description = "Capitalizes words in the following text or title",
            category = ExpressionCategory.TRANSFORM,
            example = "!cap !folder or !cap:text"
        )
    )

    private val BUILT_IN_MAP: Map<String, ExpressionDefinition> =
        BUILT_IN_DEFINITIONS.associateBy { it.token.lowercase(Locale.ROOT) }

    /**
     * Sanitizes a string for safe filesystem usage by stripping or replacing forbidden characters.
     */
    fun sanitizeFileName(input: String): String {
        val sanitized = input.replace(FORBIDDEN_FILE_CHARS, "-")
            .replace(Regex("[\\r\\n\\t]"), " ")
            .trim()
            .trimStart('.')
        return if (sanitized.isBlank()) "Untitled" else sanitized
    }

    /**
     * Counts how many expression tokens (`!token` or `!!token`) are present in a combination string.
     */
    fun countExpressionsInCombination(value: String): Int {
        val matches = Regex("""(?<!\\)!+([a-zA-Z0-9_]+)""").findAll(value)
        return matches.count()
    }

    /**
     * Validates a custom expression for name correctness, uniqueness, 5-expression limit,
     * and circular dependency graph cycles.
     */
    fun validateCustomExpression(
        expression: CustomExpression,
        existing: List<CustomExpression>,
        disabledBuiltIns: Set<String> = emptySet()
    ): ValidationResult {
        val cleanName = expression.name.trim().removePrefix("!")
        if (cleanName.isBlank()) {
            return ValidationResult(false, "Expression name cannot be empty.")
        }
        if (!NAME_REGEX.matches(cleanName)) {
            return ValidationResult(
                false,
                "Name must contain only letters, numbers, or underscores (max $MAX_CUSTOM_NAME_LENGTH chars)."
            )
        }
        if (BUILT_IN_MAP.containsKey(cleanName.lowercase(Locale.ROOT))) {
            return ValidationResult(false, "'!$cleanName' conflicts with a reserved built-in expression.")
        }
        val duplicate = existing.any {
            it.id != expression.id && it.name.equals(cleanName, ignoreCase = true)
        }
        if (duplicate) {
            return ValidationResult(false, "An expression named '!$cleanName' already exists.")
        }

        if (expression.value.isBlank()) {
            return ValidationResult(false, "Expression value cannot be empty.")
        }

        if (expression.type == ExpressionType.COMBINATION) {
            val count = countExpressionsInCombination(expression.value)
            if (count > MAX_COMBINATION_EXPRESSIONS) {
                return ValidationResult(
                    false,
                    "Combinations can contain at most $MAX_COMBINATION_EXPRESSIONS expressions (found $count)."
                )
            }
        }

        // Circular reference & self-reference check
        val combinedList = existing.filter { it.id != expression.id } + expression.copy(name = cleanName)
        val cycle = detectCircularReference(cleanName, combinedList)
        if (cycle != null) {
            return ValidationResult(
                false,
                "Circular reference detected: ${cycle.joinToString(" -> ") { "!$it" }}."
            )
        }

        return ValidationResult(true)
    }

    /**
     * Detects circular dependencies in custom expressions using depth-first search.
     * Returns the cycle path if detected, or null if acyclic.
     */
    private fun detectCircularReference(
        startName: String,
        allCustom: List<CustomExpression>
    ): List<String>? {
        val customMap = allCustom.associateBy { it.name.lowercase(Locale.ROOT) }
        val visited = mutableSetOf<String>()
        val recursionStack = mutableListOf<String>()

        fun dfs(current: String): List<String>? {
            val normalized = current.lowercase(Locale.ROOT)
            if (recursionStack.contains(normalized)) {
                return recursionStack + normalized
            }
            if (visited.contains(normalized)) return null

            visited.add(normalized)
            recursionStack.add(normalized)

            val customDef = customMap[normalized]
            if (customDef != null) {
                val referenced = extractReferencedCustomTokens(customDef.value, customMap.keys)
                for (ref in referenced) {
                    val cycle = dfs(ref)
                    if (cycle != null) return cycle
                }
            }

            recursionStack.removeAt(recursionStack.size - 1)
            return null
        }

        return dfs(startName)
    }

    private fun extractReferencedCustomTokens(
        text: String,
        knownCustomNames: Set<String>
    ): List<String> {
        val matches = Regex("""(?<!\\)!+([a-zA-Z0-9_]+)""").findAll(text)
        return matches.map { it.groupValues[1].lowercase(Locale.ROOT) }
            .filter { knownCustomNames.contains(it) }
            .toList()
    }

    /**
     * Resolves all expressions (static and dynamic) to final text.
     * Used for live preview, file export generation, and display.
     */
    fun resolve(
        input: String,
        context: ExpressionContext = ExpressionContext(),
        customExpressions: List<CustomExpression> = emptyList(),
        disabledBuiltIns: Set<String> = emptySet(),
        depth: Int = 0
    ): String {
        return resolveInternal(
            input = input,
            context = context,
            customExpressions = customExpressions,
            disabledBuiltIns = disabledBuiltIns,
            lockStaticOnly = false,
            depth = depth
        )
    }

    /**
     * Resolves and locks only static expressions (single `!xxx`), preserving dynamic
     * tokens (`!!xxx` or `!xxx~`) as dynamic tokens for continuous real-time updates.
     * Used when saving notes or committing titles.
     */
    fun lockStaticExpressions(
        input: String,
        context: ExpressionContext = ExpressionContext(),
        customExpressions: List<CustomExpression> = emptyList(),
        disabledBuiltIns: Set<String> = emptySet()
    ): String {
        return resolveInternal(
            input = input,
            context = context,
            customExpressions = customExpressions,
            disabledBuiltIns = disabledBuiltIns,
            lockStaticOnly = true,
            depth = 0
        )
    }

    /**
     * Core resolution scanner pipeline.
     */
    private fun resolveInternal(
        input: String,
        context: ExpressionContext,
        customExpressions: List<CustomExpression>,
        disabledBuiltIns: Set<String>,
        lockStaticOnly: Boolean,
        depth: Int
    ): String {
        if (input.isEmpty() || depth > MAX_EXPANSION_DEPTH) return input

        // 1. Process escape placeholder: \! -> temporary sentinel
        val escapedSentinel = "\u0000EXPR_ESCAPED_EXCLAMATION\u0000"
        var working = input.replace("\\!", escapedSentinel)

        // Text transform state across whole input if applied at start
        var globalTransform: String? = null
        if (working.startsWith("!upper ") || working.startsWith("!upper\n")) {
            globalTransform = "upper"
            working = working.substring(7)
        } else if (working.startsWith("!lower ") || working.startsWith("!lower\n")) {
            globalTransform = "lower"
            working = working.substring(7)
        } else if (working.startsWith("!cap ") || working.startsWith("!cap\n")) {
            globalTransform = "cap"
            working = working.substring(5)
        }

        val customMap = customExpressions.associateBy { it.name.lowercase(Locale.ROOT) }
        val resultBuilder = StringBuilder()
        var i = 0
        val n = working.length

        while (i < n) {
            if (working[i] == '!') {
                val tokenStart = i
                var isDynamic = false

                if (i + 1 < n && working[i + 1] == '!') {
                    isDynamic = true
                    i += 2
                } else {
                    i += 1
                }

                // Scan candidate identifier [a-zA-Z0-9_]
                val identStart = i
                while (i < n && (working[i].isLetterOrDigit() || working[i] == '_')) {
                    i++
                }
                val fullIdent = working.substring(identStart, i)

                if (fullIdent.isEmpty()) {
                    resultBuilder.append(working.substring(tokenStart, i))
                    continue
                }

                val fullLower = fullIdent.lowercase(Locale.ROOT)
                var matchedToken: String? = null
                var matchedLength = 0

                // Exact match in customMap
                if (customMap.containsKey(fullLower)) {
                    matchedToken = fullIdent
                    matchedLength = fullIdent.length
                } else if (BUILT_IN_MAP.containsKey(fullLower)) {
                    // Exact match in built-in
                    matchedToken = fullLower
                    matchedLength = fullIdent.length
                } else {
                    // Prefix match in built-ins sorted by length descending
                    val prefixBuiltIn = BUILT_IN_DEFINITIONS
                        .map { it.token.lowercase(Locale.ROOT) }
                        .sortedByDescending { it.length }
                        .firstOrNull { fullLower.startsWith(it) }

                    if (prefixBuiltIn != null) {
                        matchedToken = prefixBuiltIn
                        matchedLength = prefixBuiltIn.length
                    } else {
                        // Prefix match in custom
                        val prefixCustom = customExpressions
                            .map { it.name.lowercase(Locale.ROOT) }
                            .sortedByDescending { it.length }
                            .firstOrNull { fullLower.startsWith(it) }
                        if (prefixCustom != null) {
                            matchedToken = prefixCustom
                            matchedLength = prefixCustom.length
                        }
                    }
                }

                if (matchedToken == null) {
                    // Unknown expression -> retain original literal
                    resultBuilder.append(working.substring(tokenStart, i))
                    continue
                }

                val leftoverIdent = fullIdent.substring(matchedLength)
                // Rewind i back to end of matched token
                i = identStart + matchedLength

                // Check optional format parameter: :parameter
                var formatParam: String? = null
                if (i < n && working[i] == ':') {
                    val paramStart = i + 1
                    i++
                    val isTransform = matchedToken in listOf("upper", "lower", "cap")
                    while (i < n && working[i] != '!' && working[i] != '~' && working[i] != '\n' && working[i] != '\r' && (isTransform || !working[i].isWhitespace())) {
                        i++
                    }
                    formatParam = working.substring(paramStart, i).ifEmpty { null }
                }

                // Check optional dynamic suffix ~
                var hasTilde = false
                if (i < n && working[i] == '~') {
                    isDynamic = true
                    hasTilde = true
                    i++
                }

                val rawTokenString = working.substring(tokenStart, i)

                // If lockStaticOnly mode is on, preserve dynamic expressions as tokens
                if (lockStaticOnly && isDynamic) {
                    resultBuilder.append(rawTokenString)
                    continue
                }

                val lowerMatched = matchedToken.lowercase(Locale.ROOT)

                // Check if it's a text transform with parameter
                if (lowerMatched in listOf("upper", "lower", "cap") && formatParam != null) {
                    val transformed = applyTransform(lowerMatched, formatParam, context.locale)
                    resultBuilder.append(transformed)
                    continue
                }

                if (BUILT_IN_MAP.containsKey(lowerMatched)) {
                    if (disabledBuiltIns.contains(lowerMatched)) {
                        resultBuilder.append(rawTokenString)
                    } else {
                        val resolved = resolveBuiltIn(lowerMatched, formatParam, context)
                        resultBuilder.append(resolved)
                    }
                } else if (customMap.containsKey(lowerMatched)) {
                    val custom = customMap[lowerMatched]!!
                    val expanded = resolveInternal(
                        input = custom.value,
                        context = context,
                        customExpressions = customExpressions,
                        disabledBuiltIns = disabledBuiltIns,
                        lockStaticOnly = lockStaticOnly,
                        depth = depth + 1
                    )
                    resultBuilder.append(expanded)
                } else {
                    resultBuilder.append(rawTokenString)
                }
            } else {
                resultBuilder.append(working[i])
                i++
            }
        }

        var resolvedStr = resultBuilder.toString()

        // Apply global transform if present
        if (globalTransform != null) {
            resolvedStr = applyTransform(globalTransform, resolvedStr, context.locale)
        }

        // Restore escaped exclamation marks: \! -> !
        resolvedStr = resolvedStr.replace(escapedSentinel, "!")

        // If file name sanitization is requested, clean invalid characters
        if (context.isFileName) {
            resolvedStr = sanitizeFileName(resolvedStr)
        }

        return resolvedStr
    }

    private fun resolveBuiltIn(
        token: String,
        formatParam: String?,
        context: ExpressionContext
    ): String {
        val date = Date(context.timestamp)
        return when (token) {
            "date" -> {
                val pattern = formatParam ?: context.dateFormat
                safeFormatDate(date, pattern, context.locale)
            }
            "time" -> {
                val pattern = formatParam ?: context.timeFormat
                safeFormatDate(date, pattern, context.locale)
            }
            "datetime" -> {
                val pattern = formatParam ?: "${context.dateFormat} ${context.timeFormat}"
                safeFormatDate(date, pattern, context.locale)
            }
            "day" -> {
                SimpleDateFormat("EEEE", context.locale).format(date)
            }
            "month" -> {
                SimpleDateFormat("MMMM", context.locale).format(date)
            }
            "year" -> {
                SimpleDateFormat("yyyy", context.locale).format(date)
            }
            "week" -> {
                val cal = Calendar.getInstance(context.locale).apply { time = date }
                val weekNum = cal.get(Calendar.WEEK_OF_YEAR)
                "W$weekNum"
            }
            "page" -> {
                context.page?.toString() ?: "!page"
            }
            "pages" -> {
                context.pages?.toString() ?: "!pages"
            }
            "title" -> {
                context.title ?: "!title"
            }
            "folder" -> {
                context.folder ?: "!folder"
            }
            "app" -> {
                "${context.appName} ${context.appVersion}".trim()
            }
            "user" -> {
                context.userName?.takeIf { it.isNotBlank() } ?: "!user"
            }
            "n" -> {
                val counter = context.counterProvider?.invoke(context.scopeId) ?: 1
                counter.toString()
            }
            "id" -> {
                UUID.randomUUID().toString().replace("-", "").take(6).uppercase(Locale.ROOT)
            }
            "rand" -> {
                (100..999).random().toString()
            }
            else -> "!$token"
        }
    }

    private fun safeFormatDate(date: Date, pattern: String, locale: Locale): String {
        return try {
            SimpleDateFormat(pattern, locale).format(date)
        } catch (_: Exception) {
            SimpleDateFormat("yyyy-MM-dd", locale).format(date)
        }
    }

    private fun applyTransform(type: String, text: String, locale: Locale): String {
        return when (type) {
            "upper" -> text.uppercase(locale)
            "lower" -> text.lowercase(locale)
            "cap" -> text.split(" ").joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
            }
            else -> text
        }
    }

    /**
     * Provides autocomplete suggestions when user is typing after `!`.
     */
    fun getSuggestions(
        query: String,
        customExpressions: List<CustomExpression> = emptyList(),
        disabledBuiltIns: Set<String> = emptySet()
    ): List<ExpressionDefinition> {
        val cleanQuery = query.trim().removePrefix("!").lowercase(Locale.ROOT)

        val builtInMatches = BUILT_IN_DEFINITIONS
            .filter { !disabledBuiltIns.contains(it.token.lowercase(Locale.ROOT)) }
            .filter { it.token.lowercase(Locale.ROOT).startsWith(cleanQuery) }

        val customMatches = customExpressions
            .filter { it.name.lowercase(Locale.ROOT).startsWith(cleanQuery) }
            .map { custom ->
                ExpressionDefinition(
                    token = custom.name,
                    title = "!${custom.name}",
                    description = if (custom.type == ExpressionType.WORD) {
                        "Custom Word: \"${custom.value}\""
                    } else {
                        "Combination: ${custom.value}"
                    },
                    category = ExpressionCategory.CUSTOM,
                    example = "!${custom.name}",
                    supportsDynamic = true
                )
            }

        return (builtInMatches + customMatches).sortedBy { it.token }
    }
}
