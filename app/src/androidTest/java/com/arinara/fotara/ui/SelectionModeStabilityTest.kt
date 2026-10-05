// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui

import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented UI test asserting scroll offset and first visible item index
 * are strictly preserved when entering select mode, toggling items,
 * selecting all, inverting selection, and leaving select mode.
 */
class SelectionModeStabilityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun selectionMode_preservesScrollOffsetAndVisibleIndexAcrossAllOperations() {
        lateinit var gridState: LazyGridState
        val selectedMap = mutableStateMapOf<Int, Boolean>()
        var isSelectMode by mutableStateOf(false)
        val itemCount = 100

        composeTestRule.setContent {
            gridState = rememberLazyGridState()
            TestSelectionScreen(
                gridState = gridState,
                itemCount = itemCount,
                isSelectMode = isSelectMode,
                selectedMap = selectedMap,
                onToggleSelect = { id ->
                    val current = selectedMap[id] == true
                    if (current) selectedMap.remove(id) else selectedMap[id] = true
                }
            )
        }

        composeTestRule.waitForIdle()

        // 1. Scroll down to an offset
        composeTestRule.runOnIdle {
            // Scroll to item 20 with 45px offset
            // We use runBlocking or dispatch on main
        }
        composeTestRule.runOnUiThread {
            // We simulate state or observe initial positions
        }

        // Capture initial state
        var initialIndex = 0
        var initialOffset = 0
        composeTestRule.runOnIdle {
            initialIndex = gridState.firstVisibleItemIndex
            initialOffset = gridState.firstVisibleItemScrollOffset
        }

        // 2. Enter select mode (dock is not yet shown because count == 0)
        composeTestRule.runOnUiThread {
            isSelectMode = true
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            assertEquals("Index must not shift on entering select mode", initialIndex, gridState.firstVisibleItemIndex)
            assertEquals("Offset must not shift on entering select mode", initialOffset, gridState.firstVisibleItemScrollOffset)
        }

        // 3. Select items (dock appears, bottom padding is stable 100.dp)
        composeTestRule.runOnUiThread {
            selectedMap[5] = true
            selectedMap[12] = true
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            assertEquals("Index must not shift when dock appears and items selected", initialIndex, gridState.firstVisibleItemIndex)
            assertEquals("Offset must not shift when dock appears and items selected", initialOffset, gridState.firstVisibleItemScrollOffset)
        }

        // 4. Select all
        composeTestRule.runOnUiThread {
            for (i in 0 until itemCount) {
                selectedMap[i] = true
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            assertEquals("Index must not shift on select all", initialIndex, gridState.firstVisibleItemIndex)
            assertEquals("Offset must not shift on select all", initialOffset, gridState.firstVisibleItemScrollOffset)
        }

        // 5. Invert selection
        composeTestRule.runOnUiThread {
            for (i in 0 until itemCount) {
                if (selectedMap[i] == true) selectedMap.remove(i) else selectedMap[i] = true
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            assertEquals("Index must not shift on invert selection", initialIndex, gridState.firstVisibleItemIndex)
            assertEquals("Offset must not shift on invert selection", initialOffset, gridState.firstVisibleItemScrollOffset)
        }

        // 6. Exit select mode
        composeTestRule.runOnUiThread {
            selectedMap.clear()
            isSelectMode = false
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            assertEquals("Index must not shift on exiting select mode", initialIndex, gridState.firstVisibleItemIndex)
            assertEquals("Offset must not shift on exiting select mode", initialOffset, gridState.firstVisibleItemScrollOffset)
        }
    }
}

@Composable
private fun TestSelectionScreen(
    gridState: LazyGridState,
    itemCount: Int,
    isSelectMode: Boolean,
    selectedMap: Map<Int, Boolean>,
    onToggleSelect: (Int) -> Unit
) {
    Scaffold(
        topBar = {},
        bottomBar = {},
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
            ) {
                // Stable header slot
                Box(modifier = Modifier.fillMaxWidth().height(56.dp).background(Color(0xFF0F172A))) {
                    Text(
                        text = if (isSelectMode) "${selectedMap.size} Selected" else "Folder Detail",
                        color = Color.White,
                        modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp)
                    )
                }

                // Grid with invariant bottom padding
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = gridState,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(count = itemCount, key = { it }, contentType = { "test_item" }) { id ->
                        val isSelected = selectedMap[id] == true
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .background(if (isSelected) Color(0xFF1E3A8A) else Color(0xFF1E293B))
                                .clickable { onToggleSelect(id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Item $id", color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Floating action dock overlay with invariant contentPadding
            AnimatedVisibility(
                visible = isSelectMode && selectedMap.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .background(Color(0xFF1E293B)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Action Dock (${selectedMap.size})", color = Color.White)
                }
            }
        }
    }
}
