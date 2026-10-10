// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.home.workspace

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.systemGestureExclusion
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
import androidx.compose.ui.res.painterResource
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
import com.arinara.fotara.data.model.getDisplay
import com.arinara.fotara.data.model.getDisplayName
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
    val coroutineScope = rememberCoroutineScope()

    var localWorkspaces by remember(workspaces) { mutableStateOf(workspaces) }

    val customCount = localWorkspaces.count { it.kind == WorkspaceKind.CUSTOM }
    val isAtLimit = customCount >= WorkspaceValidator.MAX_CUSTOM_WORKSPACES

    var dragState by remember { mutableStateOf(TabDragState()) }
    var targetSlotIndex by remember { mutableIntStateOf(-1) }
    val tabPositions = remember { mutableStateMapOf<Long, Float>() }
    val tabWidths = remember { mutableStateMapOf<Long, Float>() }

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
            .padding(horizontal = 16.dp)
            .height(48.dp)
            .systemGestureExclusion()
            .clip(RoundedCornerShape(24.dp))
            .background(HomeTabBarContainer)
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isScrollEnabled = !dragState.isDragging && !dragState.isMoveMode && dragState.state == TabGestureState.IDLE
            // Scrollable tabs section
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .horizontalScroll(scrollState, enabled = isScrollEnabled),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                localWorkspaces.forEachIndexed { index, workspace ->
                    key(workspace.id) {
                        val isSelected = workspace.id == selectedWorkspaceId
                        val isBeingDragged = dragState.isDragging && dragState.activeTabId == workspace.id
                        val isMoveMode = dragState.isMoveMode && dragState.activeTabId == workspace.id
                        val isHeld = dragState.state == TabGestureState.HELD && dragState.activeTabId == workspace.id

                        val scale by animateFloatAsState(
                            targetValue = when {
                                isBeingDragged || isMoveMode -> 1.10f
                                isHeld -> 1.06f
                                else -> 1.0f
                            },
                            animationSpec = tween(120),
                            label = "tabScale"
                        )

                        val elevation by animateFloatAsState(
                            targetValue = when {
                                isBeingDragged || isMoveMode -> 12f
                                isHeld -> 6f
                                else -> 0f
                            },
                            animationSpec = tween(120),
                            label = "tabElevation"
                        )

                        // Neighboring tabs animated slot shift to visually open empty drop slot
                        val draggedId = dragState.activeTabId
                        val isAnyDragging = dragState.isDragging && draggedId != null && targetSlotIndex in 1 until localWorkspaces.size
                        val targetShiftPx = if (isAnyDragging && workspace.id != draggedId) {
                            val draggedIndex = localWorkspaces.indexOfFirst { it.id == draggedId }
                            val thisIndex = index
                            val draggedWidth = tabWidths[draggedId] ?: with(density) { 80.dp.toPx() }
                            val slotSpacing = with(density) { 4.dp.toPx() }
                            val slotShift = draggedWidth + slotSpacing
                            when {
                                draggedIndex < targetSlotIndex && thisIndex in (draggedIndex + 1)..targetSlotIndex -> -slotShift
                                draggedIndex > targetSlotIndex && thisIndex in targetSlotIndex until draggedIndex -> slotShift
                                else -> 0f
                            }
                        } else {
                            0f
                        }

                        val animatedShiftPx by animateFloatAsState(
                            targetValue = targetShiftPx,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "slotShift_${workspace.id}"
                        )

                        val translationX = if (isBeingDragged) {
                            dragState.dragDeltaX
                        } else if (isAnyDragging) {
                            animatedShiftPx
                        } else {
                            0f
                        }
                        val wsDisplayName = workspace.getDisplayName()

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .defaultMinSize(minWidth = 56.dp, minHeight = 44.dp)
                            .onGloballyPositioned { coordinates ->
                                val bounds = coordinates.boundsInParent()
                                tabPositions[workspace.id] = (bounds.left + bounds.right) / 2f
                                tabWidths[workspace.id] = bounds.width
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
                                contentDescription = "$wsDisplayName workspace tab"
                            }
                            .pointerInput(workspace.id, workspace.kind, isSelectMode) {
                                if (isSelectMode || workspace.kind == WorkspaceKind.HOME) {
                                    // Home does not allow drag or long-press; select mode disables tab gestures
                                    return@pointerInput
                                }
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val initialX = down.position.x
                                    val initialY = down.position.y

                                    var stage1Reached = false
                                    var stage2Reached = false
                                    var accumulatedDeltaX = 0f

                                    val holdJob = coroutineScope.launch {
                                        delay(400L)
                                        stage1Reached = true
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        dragState = TabDragState(
                                            state = TabGestureState.HELD,
                                            activeTabId = workspace.id,
                                            isHome = workspace.kind == WorkspaceKind.HOME,
                                            isArchive = workspace.kind == WorkspaceKind.ARCHIVE,
                                            startX = initialX,
                                            startY = initialY,
                                            currentX = initialX,
                                            currentY = initialY,
                                            dragDeltaX = 0f,
                                            showRenamePanel = workspace.kind != WorkspaceKind.HOME
                                        )

                                        delay(600L) // Total ~1000ms
                                        stage2Reached = true
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        dragState = dragState.copy(
                                            state = TabGestureState.MOVE_MODE,
                                            showRenamePanel = false
                                        )
                                    }

                                    try {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull { it.id == down.id } ?: break

                                            if (change.pressed) {
                                                val posChange = change.positionChange()
                                                accumulatedDeltaX += posChange.x
                                                val currentY = change.position.y
                                                val deltaY = currentY - initialY

                                                // Vertical cancel / shake off-screen
                                                if (kotlin.math.abs(deltaY) > 80.dp.toPx()) {
                                                    holdJob.cancel()
                                                    targetSlotIndex = -1
                                                    dragState = TabDragState(state = TabGestureState.IDLE)
                                                    break
                                                }

                                                // Cancel hold if horizontal scroll happened before Stage 1
                                                if (!stage1Reached && kotlin.math.abs(accumulatedDeltaX) > 8.dp.toPx()) {
                                                    holdJob.cancel()
                                                    targetSlotIndex = -1
                                                    dragState = TabDragState(state = TabGestureState.IDLE)
                                                    break
                                                }

                                                // Move mode or dragging past slop
                                                if (stage2Reached || (stage1Reached && kotlin.math.abs(accumulatedDeltaX) > 8.dp.toPx())) {
                                                    change.consume()
                                                    dragState = dragState.copy(
                                                        state = TabGestureState.DRAGGING,
                                                        showRenamePanel = false,
                                                        dragDeltaX = accumulatedDeltaX
                                                    )

                                                    // Calculate target slot dynamically based on current center
                                                    val currentCenter = (tabPositions[workspace.id] ?: 0f) + accumulatedDeltaX
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

                                                    if (closestSlot in 1 until localWorkspaces.size) {
                                                        if (closestSlot != targetSlotIndex) {
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        }
                                                        targetSlotIndex = closestSlot
                                                    }
                                                }
                                            } else {
                                                // Pointer lifted (Up)
                                                holdJob.cancel()
                                                if (dragState.state == TabGestureState.DRAGGING || stage2Reached) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    val currentIndex = localWorkspaces.indexOfFirst { it.id == workspace.id }
                                                    if (targetSlotIndex in 1 until localWorkspaces.size && targetSlotIndex != currentIndex) {
                                                        val finalWorkspaces = WorkspaceReorderHelper.reorderList(
                                                            localWorkspaces,
                                                            workspace.id,
                                                            targetSlotIndex
                                                        )
                                                        localWorkspaces = finalWorkspaces
                                                        onReorderWorkspaces(finalWorkspaces.map { it.id })
                                                    }
                                                    dragState = TabDragState(state = TabGestureState.IDLE)
                                                    targetSlotIndex = -1
                                                } else if (stage1Reached) {
                                                    // Finger lifted without dragging in Stage 1: Panel stays open
                                                    dragState = dragState.copy(
                                                        state = TabGestureState.HELD,
                                                        showRenamePanel = workspace.kind != WorkspaceKind.HOME
                                                    )
                                                    targetSlotIndex = -1
                                                } else {
                                                    dragState = TabDragState(state = TabGestureState.IDLE)
                                                    targetSlotIndex = -1
                                                }
                                                break
                                            }
                                        }
                                    } catch (e: Exception) {
                                        holdJob.cancel()
                                        targetSlotIndex = -1
                                        dragState = TabDragState(state = TabGestureState.IDLE)
                                    }
                                }
                            }
                            .clickable {
                                onWorkspaceSelected(workspace)
                            }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val wsDisplay = workspace.getDisplay()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                painter = painterResource(wsDisplay.iconResId),
                                contentDescription = null,
                                tint = if (isSelected) Color.White else Color(0xFF6B7280),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = wsDisplay.name,
                                color = if (isSelected) Color.White else Color(0xFF6B7280),
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = ElmsSans,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Anchored dropdown menu for Custom and Archive Workspaces
                        if (workspace.kind != WorkspaceKind.HOME && dragState.showRenamePanel && dragState.activeTabId == workspace.id) {
                            DropdownMenu(
                                expanded = true,
                                onDismissRequest = {
                                    dragState = TabDragState(state = TabGestureState.IDLE)
                                },
                                modifier = Modifier
                                    .background(HomeCardSurface)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                if (workspace.kind == WorkspaceKind.CUSTOM) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = stringResource(R.string.action_edit_workspace),
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
                                }
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
