// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.navigation

import com.arinara.fotara.ui.components.HomeNavTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TabNavigationStateTest {

    @Test
    fun reSelectTab_whenInSubScreen_triggersPopToRoot() {
        var isSettingsSubScreenOpen = true
        var settingsResetToRootTrigger = 0
        var didScrollToTop = false

        // Simulate canonical re-tap handler from HomeScreen
        val onTabReSelected: (HomeNavTab) -> Unit = { tab ->
            if (tab == HomeNavTab.SETTINGS) {
                if (isSettingsSubScreenOpen) {
                    settingsResetToRootTrigger++
                } else {
                    didScrollToTop = true
                }
            }
        }

        onTabReSelected(HomeNavTab.SETTINGS)

        assertEquals(1, settingsResetToRootTrigger)
        assertFalse(didScrollToTop)
    }

    @Test
    fun reSelectTab_whenAtRoot_triggersScrollToTop() {
        val isSettingsSubScreenOpen = false
        var settingsResetToRootTrigger = 0
        var didScrollToTop = false

        val onTabReSelected: (HomeNavTab) -> Unit = { tab ->
            if (tab == HomeNavTab.SETTINGS) {
                if (isSettingsSubScreenOpen) {
                    settingsResetToRootTrigger++
                } else {
                    didScrollToTop = true
                }
            }
        }

        onTabReSelected(HomeNavTab.SETTINGS)

        assertEquals(0, settingsResetToRootTrigger)
        assertTrue(didScrollToTop)
    }

    @Test
    fun reSelectHomeTab_whenMultiSelectActive_exitsMultiSelect() {
        var isMultiSelectMode = true
        var isSearchActive = false
        var exitedMultiSelect = false
        var didScrollToTop = false

        val onTabReSelected: (HomeNavTab) -> Unit = { tab ->
            if (tab == HomeNavTab.HOME) {
                if (isMultiSelectMode) {
                    exitedMultiSelect = true
                    isMultiSelectMode = false
                } else if (isSearchActive) {
                    isSearchActive = false
                } else {
                    didScrollToTop = true
                }
            }
        }

        onTabReSelected(HomeNavTab.HOME)

        assertTrue(exitedMultiSelect)
        assertFalse(didScrollToTop)

        // Second re-tap after exiting multi-select should scroll to top
        onTabReSelected(HomeNavTab.HOME)
        assertTrue(didScrollToTop)
    }
}
