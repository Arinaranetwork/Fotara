// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui

import androidx.compose.runtime.mutableStateMapOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test validating selection state map operations and isolation logic
 * for selection mode stability.
 */
class SelectionStabilityLogicTest {

    @Test
    fun selectionStateMap_toggleSelect_updatesOnlyTargetKey() {
        val selectedPhotoMap = mutableStateMapOf<Long, Boolean>()
        selectedPhotoMap[101L] = true
        selectedPhotoMap[102L] = true

        // Toggle item 103 on
        selectedPhotoMap[103L] = true
        assertEquals(3, selectedPhotoMap.size)
        assertTrue(selectedPhotoMap[101L] == true)
        assertTrue(selectedPhotoMap[102L] == true)
        assertTrue(selectedPhotoMap[103L] == true)

        // Toggle item 102 off
        selectedPhotoMap.remove(102L)
        assertEquals(2, selectedPhotoMap.size)
        assertTrue(selectedPhotoMap[101L] == true)
        assertFalse(selectedPhotoMap.containsKey(102L))
        assertTrue(selectedPhotoMap[103L] == true)
    }

    @Test
    fun selectionStateMap_selectAllAndInvert_producesAccurateState() {
        val selectedMap = mutableStateMapOf<Long, Boolean>()
        val allIds = listOf(1L, 2L, 3L, 4L, 5L)

        // Select All
        allIds.forEach { selectedMap[it] = true }
        assertEquals(5, selectedMap.size)
        allIds.forEach { assertTrue(selectedMap[it] == true) }

        // Deselect item 3 and 4
        selectedMap.remove(3L)
        selectedMap.remove(4L)
        assertEquals(3, selectedMap.size)

        // Invert Selection against allIds
        val currentKeys = selectedMap.keys.toSet()
        selectedMap.clear()
        allIds.filterNot { it in currentKeys }.forEach { selectedMap[it] = true }

        assertEquals(2, selectedMap.size)
        assertTrue(selectedMap[3L] == true)
        assertTrue(selectedMap[4L] == true)
        assertFalse(selectedMap.containsKey(1L))
        assertFalse(selectedMap.containsKey(2L))
        assertFalse(selectedMap.containsKey(5L))
    }

    @Test
    fun selectionStateMap_clearOnExit_resetsAllSelections() {
        val selectedFolderMap = mutableStateMapOf<Long, Boolean>()
        selectedFolderMap[10L] = true
        selectedFolderMap[20L] = true
        selectedFolderMap[30L] = true

        assertEquals(3, selectedFolderMap.size)

        // Exit multi-select mode
        selectedFolderMap.clear()
        assertTrue(selectedFolderMap.isEmpty())
        assertFalse(selectedFolderMap.containsKey(10L))
    }
}
