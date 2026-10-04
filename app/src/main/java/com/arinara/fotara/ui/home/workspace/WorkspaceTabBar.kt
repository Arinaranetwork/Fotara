// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.home.workspace

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.R
import com.arinara.fotara.data.model.Workspace
import com.arinara.fotara.data.model.WorkspaceKind
import com.arinara.fotara.data.repository.WorkspaceValidator
import androidx.compose.material.icons.filled.Delete
import com.arinara.fotara.theme.TagCrimson
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.theme.HomeCardBorder
import com.arinara.fotara.theme.HomeCardSurface
import com.arinara.fotara.theme.HomeTabBarContainer
import com.arinara.fotara.theme.HomeTabBarSelectedPill

@Composable
fun WorkspaceTabBar(
    workspaces: List<Workspace>,
    selectedWorkspaceId: Long,
    onWorkspaceSelected: (Workspace) -> Unit,
    onAddClick: () -> Unit,
    onLimitReached: () -> Unit,
    onRenameClick: (Workspace) -> Unit,
    onDeleteClick: (Workspace) -> Unit = {},
    onReorderWorkspaces: (List<Long>) -> Unit,
    isSelectMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    val customCount = workspaces.count { it.kind == WorkspaceKind.CUSTOM }
    val isAtLimit = customCount >= WorkspaceValidator.MAX_CUSTOM_WORKSPACES

    var localWorkspaces by remember(workspaces) { mutableStateOf(workspaces) }
    var dragState by remember { mutableStateOf(TabDragState()) }
    val tabPositions = remember { mutableStateMapOf<Long, Float>() }

    // Auto-scroll selected tab into view
    LaunchedEffect(selectedWorkspaceId, workspaces) {
        val selectedIndex = workspaces.indexOfFirst { it.id == selectedWorkspaceId }
        if (selectedIndex != -1) {
            val approxTabWidthPx = with(density) { 90.dp.toPx() }
            val targetScroll = (selectedIndex * approxTabWidthPx).toInt()
            scrollState.animateScrollTo(targetScroll.coerceIn(0, scrollState.maxValue))
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(HomeTabBarContainer)
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Scrollable tabs section
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .horizontalScroll(scrollState),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                localWorkspaces.forEachIndexed { index, workspace ->
                    val isSelected = workspace.id == selectedWorkspaceId
                    val isBeingDragged = dragState.isDragging && dragState.activeTabId == workspace.id
                    val isHeld = dragState.state == TabGestureState.HELD && dragState.activeTabId == workspace.id

                    val scale by animateFloatAsState(
                        targetValue = if (isBeingDragged || isHeld) 1.06f else 1.0f,
                        animationSpec = tween(150),
                        label = "tabScale"
                    )

                    val elevation by animateFloatAsState(
                        targetValue = if (isBeingDragged || isHeld) 8f else 0f,
                        animationSpec = tween(150),
                        label = "tabElevation"
                    )

                    val translationX = if (isBeingDragged) dragState.dragDeltaX else 0f

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .defaultMinSize(minWidth = 56.dp, minHeight = 44.dp)
                            .onGloballyPositioned { coordinates ->
                                val bounds = coordinates.boundsInParent()
                                tabPositions[workspace.id] = (bounds.left + bounds.right) / 2f
                            }
                            .graphicsLayer {
                                this.scaleX = scale
                                this.scaleY = scale
                                this.shadowElevation = elevation
                                this.translationX = translationX
                                this.shape = RoundedCornerShape(20.dp)
                                this.clip = false
                            }
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) HomeTabBarSelectedPill else Color.Transparent)
                            .semantics {
                                contentDescription = "${workspace.name} workspace tab"
                            }
                            .pointerInput(workspace.id, workspace.kind, isSelectMode) {
                                if (isSelectMode || workspace.kind == WorkspaceKind.HOME) {
                                    // Home does not allow drag or long-press; select mode disables tab gestures
                                    return@pointerInput
                                }
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { offset ->
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        dragState = TabDragState(
                                            state = TabGestureState.HELD,
                                            activeTabId = workspace.id,
                                            isHome = workspace.kind == WorkspaceKind.HOME,
                                            isArchive = workspace.kind == WorkspaceKind.ARCHIVE,
                                            startX = offset.x,
                                            currentX = offset.x,
                                            dragDeltaX = 0f,
                                            showRenamePanel = workspace.kind == WorkspaceKind.CUSTOM
                                        )
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val newDragDelta = dragState.dragDeltaX + dragAmount.x
                                        val newCurrentX = dragState.startX + newDragDelta

                                        if (dragState.state == TabGestureState.HELD && kotlin.math.abs(newDragDelta) > 8.dp.toPx()) {
                                            dragState = dragState.copy(
                                                state = TabGestureState.DRAGGING,
                                                showRenamePanel = false,
                                                dragDeltaX = newDragDelta,
                                                currentX = newCurrentX
                                            )
                                        } else if (dragState.state == TabGestureState.DRAGGING) {
                                            dragState = dragState.copy(
                                                dragDeltaX = newDragDelta,
                                                currentX = newCurrentX
                                            )

                                            // Reorder slots dynamically
                                            val currentCenter = (tabPositions[workspace.id] ?: 0f) + newDragDelta
                                            val currentIndex = localWorkspaces.indexOfFirst { it.id == workspace.id }

                                            // Valid slots are strictly after Home: 1..lastIndex
                                            val candidateSlots = (1 until localWorkspaces.size)
                                            var closestSlot = currentIndex
                                            var minDistance = Float.MAX_VALUE

                                            for (slot in candidateSlots) {
                                                val otherId = localWorkspaces[slot].id
                                                val otherCenter = tabPositions[otherId] ?: continue
                                                val dist = kotlin.math.abs(currentCenter - otherCenter)
                                                if (dist < minDistance) {
                                                    minDistance = dist
                                                    closestSlot = slot
                                                }
                                            }

                                            if (closestSlot in 1 until localWorkspaces.size && closestSlot != currentIndex) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                localWorkspaces = WorkspaceReorderHelper.reorderList(
                                                    localWorkspaces,
                                                    workspace.id,
                                                    closestSlot
                                                )
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        if (dragState.state == TabGestureState.DRAGGING) {
                                            val finalIds = localWorkspaces.map { it.id }
                                            onReorderWorkspaces(finalIds)
                                            dragState = TabDragState(state = TabGestureState.IDLE)
                                        }
                                        // If finger lifted without dragging, HELD state keeps showRenamePanel open
                                    },
                                    onDragCancel = {
                                        localWorkspaces = workspaces
                                        dragState = TabDragState(state = TabGestureState.IDLE)
                                    }
                                )
                            }
                            .clickable {
                                onWorkspaceSelected(workspace)
                            }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            when (workspace.kind) {
                                WorkspaceKind.HOME -> {
                                    Icon(
                                        imageVector = Icons.Filled.GridView,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else Color(0xFF6B7280),
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.workspace_home),
                                        color = if (isSelected) Color.White else Color(0xFF6B7280),
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = ElmsSans
                                    )
                                }
                                WorkspaceKind.ARCHIVE -> {
                                    Icon(
                                        imageVector = Icons.Outlined.Archive,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else Color(0xFF6B7280),
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.workspace_archive),
                                        color = if (isSelected) Color.White else Color(0xFF6B7280),
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = ElmsSans
                                    )
                                }
                                WorkspaceKind.CUSTOM -> {
                                    Text(
                                        text = workspace.name,
                                        color = if (isSelected) Color.White else Color(0xFF6B7280),
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = ElmsSans,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Anchored dropdown menu for Custom Workspaces (contains Rename)
                        if (workspace.kind == WorkspaceKind.CUSTOM && dragState.showRenamePanel && dragState.activeTabId == workspace.id) {
                            DropdownMenu(
                                expanded = true,
                                onDismissRequest = {
                                    dragState = TabDragState(state = TabGestureState.IDLE)
                                },
                                modifier = Modifier
                                    .background(HomeCardSurface)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = stringResource(R.string.action_rename_workspace),
                                            color = Color.White,
                                            fontFamily = ElmsSans,
                                            fontSize = 14.sp
                                        )
                                    },
                                    onClick = {
                                        dragState = TabDragState(state = TabGestureState.IDLE)
                                        onRenameClick(workspace)
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = stringResource(R.string.action_delete_workspace),
                                            color = TagCrimson,
                                            fontFamily = ElmsSans,
                                            fontSize = 14.sp
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = TagCrimson,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        dragState = TabDragState(state = TabGestureState.IDLE)
                                        onDeleteClick(workspace)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Divider before pinned (+) button
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(Color(0xFF232D42))
            )
            Spacer(modifier = Modifier.width(4.dp))

            // Right-pinned (+) Add Workspace Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .alpha(if (isAtLimit) 0.38f else 1.0f)
                    .clickable {
                        if (isAtLimit) {
                            onLimitReached()
                        } else {
                            onAddClick()
                        }
                    }
                    .semantics {
                        contentDescription = "Add workspace"
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
