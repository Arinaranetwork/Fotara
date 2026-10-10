// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.space

import com.arinara.fotara.data.model.Space
import com.arinara.fotara.data.repository.SpaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeSpaceRepository : SpaceRepository {
    private val spaces = mutableListOf(Space.DEFAULT_SPACE)
    private val _activeSpaceId = MutableStateFlow(Space.DEFAULT_SPACE_ID)
    override val activeSpaceId: StateFlow<Long> = _activeSpaceId.asStateFlow()

    private val _activeSpace = MutableStateFlow(Space.DEFAULT_SPACE)
    override val activeSpace: StateFlow<Space> = _activeSpace.asStateFlow()

    private val _isPrivateSpaceUnlocked = MutableStateFlow(false)
    override val isPrivateSpaceUnlocked: StateFlow<Boolean> = _isPrivateSpaceUnlocked.asStateFlow()

    private val _spacesFlow = MutableStateFlow<List<Space>>(listOf(Space.DEFAULT_SPACE))

    private fun updateFlow() {
        val list = if (_isPrivateSpaceUnlocked.value) spaces else spaces.filter { !it.isPrivate }
        _spacesFlow.value = list.toList()
        _activeSpace.value = spaces.firstOrNull { it.id == _activeSpaceId.value } ?: Space.DEFAULT_SPACE
    }

    override fun observeSpaces(): Flow<List<Space>> = _spacesFlow.asStateFlow()
    override suspend fun getSpacesSync(): List<Space> = _spacesFlow.value
    override suspend fun getSpaceById(id: Long): Space? = spaces.firstOrNull { it.id == id }

    override suspend fun setActiveSpaceId(spaceId: Long) {
        _activeSpaceId.value = spaceId
        updateFlow()
    }

    override suspend fun createSpace(
        name: String,
        iconKey: String,
        colorHex: String,
        isPrivate: Boolean
    ): Result<Space> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return Result.failure(IllegalArgumentException("Name cannot be empty"))
        val id = (spaces.maxOfOrNull { it.id } ?: 0L) + 1L
        val space = Space(
            id = id,
            name = trimmed,
            iconKey = iconKey,
            colorHex = colorHex,
            isPrivate = isPrivate,
            sortOrder = spaces.size
        )
        spaces.add(space)
        updateFlow()
        return Result.success(space)
    }

    override suspend fun updateSpace(space: Space): Result<Unit> {
        val index = spaces.indexOfFirst { it.id == space.id }
        if (index == -1) return Result.failure(NoSuchElementException())
        spaces[index] = space
        updateFlow()
        return Result.success(Unit)
    }

    override suspend fun deleteSpace(id: Long): Result<Unit> {
        if (id == Space.DEFAULT_SPACE_ID) return Result.failure(IllegalStateException("Cannot delete default space"))
        spaces.removeAll { it.id == id }
        if (_activeSpaceId.value == id) {
            _activeSpaceId.value = Space.DEFAULT_SPACE_ID
        }
        updateFlow()
        return Result.success(Unit)
    }

    override suspend fun setPrivateSpaceUnlocked(unlocked: Boolean) {
        _isPrivateSpaceUnlocked.value = unlocked
        updateFlow()
    }
}

class SpaceRepositoryTest {

    private lateinit var repository: FakeSpaceRepository
    private lateinit var manager: ActiveSpaceManager

    @Before
    fun setUp() {
        repository = FakeSpaceRepository()
        manager = ActiveSpaceManager(repository)
    }

    @Test
    fun defaultSpace_isInitializedAndActive() = runBlocking {
        assertEquals(Space.DEFAULT_SPACE_ID, repository.activeSpaceId.value)
        assertEquals("Default Space", repository.activeSpace.value.name)
        val spaces = repository.getSpacesSync()
        assertEquals(1, spaces.size)
    }

    @Test
    fun createSpace_addsNewSpaceAndCanBeActivated() = runBlocking {
        val result = repository.createSpace(
            name = "Research Lab",
            iconKey = "science",
            colorHex = "#059669",
            isPrivate = false
        )
        assertTrue(result.isSuccess)
        val created = result.getOrNull()
        assertNotNull(created)
        assertEquals("Research Lab", created!!.name)

        repository.setActiveSpaceId(created.id)
        assertEquals(created.id, repository.activeSpaceId.value)
        assertEquals("Research Lab", repository.activeSpace.value.name)
    }

    @Test
    fun createSpace_emptyNameFails() = runBlocking {
        val result = repository.createSpace(
            name = "   ",
            iconKey = "science",
            colorHex = "#059669",
            isPrivate = false
        )
        assertTrue(result.isFailure)
    }

    @Test
    fun deleteSpace_preventsDeletingDefaultSpace() = runBlocking {
        val result = repository.deleteSpace(Space.DEFAULT_SPACE_ID)
        assertTrue(result.isFailure)
    }

    @Test
    fun deleteSpace_resetsActiveSpaceToDefaultIfActiveDeleted() = runBlocking {
        val created = repository.createSpace("Temporary Space").getOrThrow()
        repository.setActiveSpaceId(created.id)
        assertEquals(created.id, repository.activeSpaceId.value)

        val deleteResult = repository.deleteSpace(created.id)
        assertTrue(deleteResult.isSuccess)
        assertEquals(Space.DEFAULT_SPACE_ID, repository.activeSpaceId.value)
    }

    @Test
    fun privateSpace_hiddenUntilStealthVaultUnlocked() = runBlocking {
        val privateSpace = repository.createSpace(
            name = "Secret Coursework",
            isPrivate = true
        ).getOrThrow()

        // When locked, private space is not in public visible list
        var visible = repository.getSpacesSync()
        assertFalse(visible.any { it.id == privateSpace.id })

        // Unlock stealth vault
        repository.setPrivateSpaceUnlocked(true)
        visible = repository.getSpacesSync()
        assertTrue(visible.any { it.id == privateSpace.id })

        // Lock again
        repository.setPrivateSpaceUnlocked(false)
        visible = repository.getSpacesSync()
        assertFalse(visible.any { it.id == privateSpace.id })
    }

    @Test
    fun activeSpaceManager_stealthPrivacyTogglesWork() {
        assertFalse(manager.isPrivateFolderUnlocked.value)
        manager.unlockPrivateFolders()
        assertTrue(manager.isPrivateFolderUnlocked.value)
        manager.lockPrivateFolders()
        assertFalse(manager.isPrivateFolderUnlocked.value)

        assertFalse(manager.isGhostWorkspaceUnlocked.value)
        val toggled = manager.toggleGhostWorkspaces()
        assertTrue(toggled)
        assertTrue(manager.isGhostWorkspaceUnlocked.value)
    }
}
