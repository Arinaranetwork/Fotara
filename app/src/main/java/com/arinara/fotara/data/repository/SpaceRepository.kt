// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.repository

import android.content.ContentValues
import android.content.Context
import com.arinara.fotara.data.db.FotaraDbHelper
import com.arinara.fotara.data.model.Space
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

interface SpaceRepository {
    val activeSpaceId: StateFlow<Long>
    val activeSpace: StateFlow<Space>
    val isPrivateSpaceUnlocked: StateFlow<Boolean>

    fun observeSpaces(): Flow<List<Space>>
    suspend fun getSpacesSync(): List<Space>
    suspend fun getSpaceById(id: Long): Space?
    suspend fun setActiveSpaceId(spaceId: Long)
    suspend fun createSpace(
        name: String,
        iconKey: String = "school",
        colorHex: String = "#2563EB",
        isPrivate: Boolean = false
    ): Result<Space>
    suspend fun updateSpace(space: Space): Result<Unit>
    suspend fun deleteSpace(id: Long): Result<Unit>
    suspend fun setPrivateSpaceUnlocked(unlocked: Boolean)
}

class SqliteSpaceRepository(
    private val dbHelper: FotaraDbHelper,
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : SpaceRepository {

    private val prefs = context.getSharedPreferences("fotara_spaces", Context.MODE_PRIVATE)

    private val _activeSpaceId = MutableStateFlow(
        prefs.getLong("active_space_id", Space.DEFAULT_SPACE_ID)
    )
    override val activeSpaceId: StateFlow<Long> = _activeSpaceId.asStateFlow()

    private val _activeSpace = MutableStateFlow(Space.DEFAULT_SPACE)
    override val activeSpace: StateFlow<Space> = _activeSpace.asStateFlow()

    private val _isPrivateSpaceUnlocked = MutableStateFlow(false)
    override val isPrivateSpaceUnlocked: StateFlow<Boolean> = _isPrivateSpaceUnlocked.asStateFlow()

    private val _spacesFlow = MutableStateFlow<List<Space>>(listOf(Space.DEFAULT_SPACE))

    init {
        scope.launch {
            refreshSync()
        }
    }

    private fun refreshSync() {
        try {
            val db = dbHelper.getSafeReadableDatabase()
            val list = mutableListOf<Space>()
            val cursor = db.rawQuery(
                """
                SELECT id, uuid, name, icon_key, color_hex, is_private, sort_order, created_at, updated_at
                FROM spaces
                ORDER BY sort_order ASC, id ASC
                """.trimIndent(),
                null
            )
            cursor.use { c ->
                while (c.moveToNext()) {
                    list.add(
                        Space(
                            id = c.getLong(0),
                            uuid = c.getString(1),
                            name = c.getString(2),
                            iconKey = c.getString(3) ?: "school",
                            colorHex = c.getString(4) ?: "#2563EB",
                            isPrivate = c.getInt(5) == 1,
                            sortOrder = c.getInt(6),
                            createdAt = c.getLong(7),
                            updatedAt = c.getLong(8)
                        )
                    )
                }
            }

            // Invariant guard: ensure at least Default Space exists
            if (list.isEmpty()) {
                val writeDb = dbHelper.getSafeWritableDatabase()
                val now = System.currentTimeMillis()
                writeDb.execSQL(
                    """
                    INSERT OR IGNORE INTO spaces (id, uuid, name, icon_key, color_hex, is_private, sort_order, created_at, updated_at)
                    VALUES (1, '${Space.DEFAULT_SPACE_UUID}', 'Default Space', 'school', '#2563EB', 0, 0, $now, $now)
                    """.trimIndent()
                )
                list.add(Space.DEFAULT_SPACE)
            }

            // Filter out private spaces if stealth vault is locked
            val isUnlocked = _isPrivateSpaceUnlocked.value
            val visibleList = if (isUnlocked) list else list.filter { !it.isPrivate }
            _spacesFlow.value = visibleList

            // Update active space
            val currentId = _activeSpaceId.value
            val current = list.firstOrNull { it.id == currentId }
                ?: list.firstOrNull()
                ?: Space.DEFAULT_SPACE
            _activeSpace.value = current
            if (current.id != currentId) {
                _activeSpaceId.value = current.id
                prefs.edit().putLong("active_space_id", current.id).apply()
            }
        } catch (e: Exception) {
            android.util.Log.e("SqliteSpaceRepo", "Error refreshing spaces: ${e.message}", e)
        }
    }

    override fun observeSpaces(): Flow<List<Space>> = _spacesFlow.asStateFlow()

    override suspend fun getSpacesSync(): List<Space> = withContext(Dispatchers.IO) {
        if (_spacesFlow.value.isEmpty()) {
            refreshSync()
        }
        _spacesFlow.value
    }

    override suspend fun getSpaceById(id: Long): Space? = withContext(Dispatchers.IO) {
        val list = getSpacesSync()
        list.firstOrNull { it.id == id }
    }

    override suspend fun setActiveSpaceId(spaceId: Long) = withContext(Dispatchers.IO) {
        _activeSpaceId.value = spaceId
        prefs.edit().putLong("active_space_id", spaceId).apply()
        refreshSync()
    }

    override suspend fun createSpace(
        name: String,
        iconKey: String,
        colorHex: String,
        isPrivate: Boolean
    ): Result<Space> = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Space name cannot be empty"))
        }

        try {
            val db = dbHelper.getSafeWritableDatabase()
            val now = System.currentTimeMillis()
            val uuid = UUID.randomUUID().toString()
            val currentSpaces = getSpacesSync()
            val nextSortOrder = (currentSpaces.maxOfOrNull { it.sortOrder } ?: 0) + 1

            val values = ContentValues().apply {
                put("uuid", uuid)
                put("name", trimmed)
                put("icon_key", iconKey.ifBlank { "school" })
                put("color_hex", colorHex.ifBlank { "#2563EB" })
                put("is_private", if (isPrivate) 1 else 0)
                put("sort_order", nextSortOrder)
                put("created_at", now)
                put("updated_at", now)
            }

            val insertedId = db.insert("spaces", null, values)
            if (insertedId == -1L) {
                return@withContext Result.failure(IllegalStateException("Failed to insert space"))
            }

            refreshSync()
            val created = Space(
                id = insertedId,
                uuid = uuid,
                name = trimmed,
                iconKey = iconKey,
                colorHex = colorHex,
                isPrivate = isPrivate,
                sortOrder = nextSortOrder,
                createdAt = now,
                updatedAt = now
            )
            Result.success(created)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateSpace(space: Space): Result<Unit> = withContext(Dispatchers.IO) {
        val trimmed = space.name.trim()
        if (trimmed.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Space name cannot be empty"))
        }

        try {
            val db = dbHelper.getSafeWritableDatabase()
            val now = System.currentTimeMillis()
            val values = ContentValues().apply {
                put("name", trimmed)
                put("icon_key", space.iconKey)
                put("color_hex", space.colorHex)
                put("is_private", if (space.isPrivate) 1 else 0)
                put("sort_order", space.sortOrder)
                put("updated_at", now)
            }

            db.update("spaces", values, "id = ?", arrayOf(space.id.toString()))
            refreshSync()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteSpace(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        if (id == Space.DEFAULT_SPACE_ID) {
            return@withContext Result.failure(IllegalStateException("Default Space cannot be deleted"))
        }

        try {
            val db = dbHelper.getSafeWritableDatabase()
            db.delete("spaces", "id = ?", arrayOf(id.toString()))
            if (_activeSpaceId.value == id) {
                setActiveSpaceId(Space.DEFAULT_SPACE_ID)
            } else {
                refreshSync()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setPrivateSpaceUnlocked(unlocked: Boolean) = withContext(Dispatchers.IO) {
        _isPrivateSpaceUnlocked.value = unlocked
        refreshSync()
    }
}
