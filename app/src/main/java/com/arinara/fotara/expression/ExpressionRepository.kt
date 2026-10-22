// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.expression

import android.content.Context
import android.content.SharedPreferences
import com.arinara.fotara.data.db.FotaraDbHelper
import com.arinara.fotara.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Repository interface for managing custom expressions, built-in toggles, and formatting settings.
 */
interface ExpressionRepository {
    val customExpressionsFlow: StateFlow<List<CustomExpression>>
    val disabledBuiltInsFlow: StateFlow<Set<String>>
    val defaultDateFormatFlow: StateFlow<String>
    val defaultTimeFormatFlow: StateFlow<String>

    suspend fun saveCustomExpression(expression: CustomExpression): ValidationResult
    suspend fun deleteCustomExpression(id: Long)
    suspend fun duplicateCustomExpression(id: Long): CustomExpression?
    suspend fun toggleBuiltIn(token: String, isEnabled: Boolean)
    suspend fun setDefaultDateFormat(format: String)
    suspend fun setDefaultTimeFormat(format: String)
    suspend fun getNextCounter(scopeId: String?): Int
    suspend fun resetCounter(scopeId: String?)

    suspend fun createExpressionContext(
        scopeId: String? = null,
        page: Int? = null,
        pages: Int? = null,
        title: String? = null,
        folder: String? = null,
        isFileName: Boolean = false
    ): ExpressionContext
}

/**
 * Default implementation of [ExpressionRepository] backed by SQLite and SharedPreferences.
 */
class DefaultExpressionRepository(
    private val context: Context,
    private val dbHelper: FotaraDbHelper = FotaraDbHelper.getInstance(context),
    private val settingsRepository: SettingsRepository? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ExpressionRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    private val _customExpressionsFlow = MutableStateFlow<List<CustomExpression>>(emptyList())
    override val customExpressionsFlow: StateFlow<List<CustomExpression>> = _customExpressionsFlow.asStateFlow()

    private val _disabledBuiltInsFlow = MutableStateFlow<Set<String>>(loadDisabledBuiltIns())
    override val disabledBuiltInsFlow: StateFlow<Set<String>> = _disabledBuiltInsFlow.asStateFlow()

    private val _defaultDateFormatFlow = MutableStateFlow(
        prefs.getString(KEY_DEFAULT_DATE_FORMAT, DEFAULT_DATE_FORMAT) ?: DEFAULT_DATE_FORMAT
    )
    override val defaultDateFormatFlow: StateFlow<String> = _defaultDateFormatFlow.asStateFlow()

    private val _defaultTimeFormatFlow = MutableStateFlow(
        prefs.getString(KEY_DEFAULT_TIME_FORMAT, DEFAULT_TIME_FORMAT) ?: DEFAULT_TIME_FORMAT
    )
    override val defaultTimeFormatFlow: StateFlow<String> = _defaultTimeFormatFlow.asStateFlow()

    init {
        scope.launch {
            refreshCustomExpressions()
        }
    }

    private suspend fun refreshCustomExpressions() = withContext(ioDispatcher) {
        val list = dbHelper.getAllCustomExpressions()
        _customExpressionsFlow.value = list
    }

    private fun loadDisabledBuiltIns(): Set<String> {
        val raw = prefs.getStringSet(KEY_DISABLED_BUILTINS, emptySet()) ?: emptySet()
        return raw.toSet()
    }

    override suspend fun saveCustomExpression(expression: CustomExpression): ValidationResult =
        withContext(ioDispatcher) {
            val existing = _customExpressionsFlow.value
            val validation = ExpressionEngine.validateCustomExpression(
                expression = expression,
                existing = existing,
                disabledBuiltIns = _disabledBuiltInsFlow.value
            )
            if (!validation.isValid) {
                return@withContext validation
            }

            if (expression.id == 0L) {
                dbHelper.insertCustomExpression(
                    name = expression.name,
                    type = expression.type.name,
                    value = expression.value
                )
            } else {
                dbHelper.updateCustomExpression(
                    id = expression.id,
                    name = expression.name,
                    type = expression.type.name,
                    value = expression.value
                )
            }
            refreshCustomExpressions()
            ValidationResult(true)
        }

    override suspend fun deleteCustomExpression(id: Long) = withContext(ioDispatcher) {
        dbHelper.deleteCustomExpression(id)
        refreshCustomExpressions()
    }

    override suspend fun duplicateCustomExpression(id: Long): CustomExpression? =
        withContext(ioDispatcher) {
            val original = _customExpressionsFlow.value.find { it.id == id } ?: return@withContext null
            var candidateName = "${original.name}_copy"
            var counter = 2
            val existingNames = _customExpressionsFlow.value.map { it.name.lowercase() }.toSet()
            while (existingNames.contains(candidateName.lowercase())) {
                candidateName = "${original.name}_copy$counter"
                counter++
            }
            if (candidateName.length > ExpressionEngine.MAX_CUSTOM_NAME_LENGTH) {
                candidateName = candidateName.take(ExpressionEngine.MAX_CUSTOM_NAME_LENGTH)
            }
            val newExpr = original.copy(
                id = 0L,
                name = candidateName,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            saveCustomExpression(newExpr)
            _customExpressionsFlow.value.find { it.name.equals(candidateName, ignoreCase = true) }
        }

    override suspend fun toggleBuiltIn(token: String, isEnabled: Boolean) = withContext(ioDispatcher) {
        val current = _disabledBuiltInsFlow.value.toMutableSet()
        val normalized = token.lowercase()
        if (isEnabled) {
            current.remove(normalized)
        } else {
            current.add(normalized)
        }
        prefs.edit().putStringSet(KEY_DISABLED_BUILTINS, current).apply()
        _disabledBuiltInsFlow.value = current
    }

    override suspend fun setDefaultDateFormat(format: String) = withContext(ioDispatcher) {
        prefs.edit().putString(KEY_DEFAULT_DATE_FORMAT, format).apply()
        _defaultDateFormatFlow.value = format
    }

    override suspend fun setDefaultTimeFormat(format: String) = withContext(ioDispatcher) {
        prefs.edit().putString(KEY_DEFAULT_TIME_FORMAT, format).apply()
        _defaultTimeFormatFlow.value = format
    }

    override suspend fun getNextCounter(scopeId: String?): Int = withContext(ioDispatcher) {
        val key = scopeId ?: GLOBAL_SCOPE
        dbHelper.getNextExpressionCounter(key)
    }

    override suspend fun resetCounter(scopeId: String?) = withContext(ioDispatcher) {
        val key = scopeId ?: GLOBAL_SCOPE
        dbHelper.resetExpressionCounter(key)
    }

    override suspend fun createExpressionContext(
        scopeId: String?,
        page: Int?,
        pages: Int?,
        title: String?,
        folder: String?,
        isFileName: Boolean
    ): ExpressionContext {
        val userName = settingsRepository?.profileFlow?.value?.name
        return ExpressionContext(
            timestamp = System.currentTimeMillis(),
            page = page,
            pages = pages,
            title = title,
            folder = folder,
            userName = userName,
            scopeId = scopeId,
            dateFormat = _defaultDateFormatFlow.value,
            timeFormat = _defaultTimeFormatFlow.value,
            counterProvider = { sId ->
                // Synchronous peek or counter fetch
                dbHelper.getNextExpressionCounter(sId ?: GLOBAL_SCOPE)
            },
            isFileName = isFileName
        )
    }

    companion object {
        const val PREFS_NAME = "fotara_expression_settings"
        const val KEY_DISABLED_BUILTINS = "key_disabled_builtins"
        const val KEY_DEFAULT_DATE_FORMAT = "key_default_date_format"
        const val KEY_DEFAULT_TIME_FORMAT = "key_default_time_format"

        const val DEFAULT_DATE_FORMAT = "dd/MM/yyyy"
        const val DEFAULT_TIME_FORMAT = "HH:mm"
        const val GLOBAL_SCOPE = "__global__"

        @Volatile
        private var INSTANCE: ExpressionRepository? = null

        fun getInstance(
            context: Context,
            settingsRepository: SettingsRepository? = null
        ): ExpressionRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DefaultExpressionRepository(
                    context = context.applicationContext,
                    settingsRepository = settingsRepository
                ).also { INSTANCE = it }
            }
        }
    }
}
