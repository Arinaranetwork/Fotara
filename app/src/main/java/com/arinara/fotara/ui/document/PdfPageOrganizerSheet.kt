// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.document

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.ui.components.presets.AppBottomSheet
import com.arinara.fotara.ui.components.presets.AppButton
import com.arinara.fotara.ui.components.presets.AppButtonVariant
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import com.arinara.fotara.ui.theme.jigglePressClickable
import com.arinara.fotara.util.PdfPageRenderer

@Composable
fun PdfPageOrganizerSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    renderer: PdfPageRenderer?,
    totalPages: Int,
    isApplying: Boolean,
    onApplyChanges: (pageOrder: List<Int>, pageRotations: Map<Int, Int>) -> Unit
) {
    val pageOrder = remember(totalPages) {
        mutableStateListOf(*(0 until totalPages).toList().toTypedArray())
    }
    val pageRotations = remember(totalPages) {
        mutableStateMapOf<Int, Int>()
    }
    val selectedIndices = remember {
        mutableStateListOf<Int>()
    }

    AppBottomSheet(
        visible = visible,
        onDismiss = {
            if (!isApplying) onDismiss()
        },
        title = "Organize Pages (${pageOrder.size})"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Action Controls Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Select All / Deselect All
                val isAllSelected = selectedIndices.size == pageOrder.size && pageOrder.isNotEmpty()
                AppButton(
                    text = if (isAllSelected) "Deselect" else "Select All",
                    onClick = {
                        if (isAllSelected) {
                            selectedIndices.clear()
                        } else {
                            selectedIndices.clear()
                            selectedIndices.addAll(0 until pageOrder.size)
                        }
                    },
                    variant = AppButtonVariant.OUTLINED,
                    height = 36.dp,
                    modifier = Modifier.weight(1f)
                )

                // Rotate 90°
                AppButton(
                    text = "Rotate 90°",
                    onClick = {
                        val targets = if (selectedIndices.isNotEmpty()) {
                            selectedIndices.toList()
                        } else {
                            (0 until pageOrder.size).toList()
                        }
                        for (idx in targets) {
                            val orig = pageOrder[idx]
                            val cur = pageRotations[orig] ?: 0
                            pageRotations[orig] = (cur + 90) % 360
                        }
                    },
                    leadingIcon = Icons.Default.RotateRight,
                    variant = AppButtonVariant.SECONDARY,
                    height = 36.dp,
                    modifier = Modifier.weight(1f)
                )

                // Delete Page(s)
                val canDelete = selectedIndices.isNotEmpty() && selectedIndices.size < pageOrder.size
                AppButton(
                    text = if (selectedIndices.isNotEmpty()) "Delete (${selectedIndices.size})" else "Delete",
                    onClick = {
                        if (canDelete) {
                            val sortedDesc = selectedIndices.sortedDescending()
                            for (idx in sortedDesc) {
                                if (idx in 0 until pageOrder.size) {
                                    pageOrder.removeAt(idx)
                                }
                            }
                            selectedIndices.clear()
                        }
                    },
                    enabled = canDelete,
                    leadingIcon = Icons.Default.Delete,
                    variant = AppButtonVariant.DESTRUCTIVE,
                    height = 36.dp,
                    modifier = Modifier.weight(1f)
                )
            }

            // Reorder Bar (active when exactly 1 page is selected)
            if (selectedIndices.size == 1) {
                val selectedIdx = selectedIndices[0]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ElectricSlateTokens.SurfaceElevated, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Page ${selectedIdx + 1} of ${pageOrder.size}",
                        color = ElectricSlateTokens.TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppButton(
                            text = "Move Left",
                            onClick = {
                                if (selectedIdx > 0) {
                                    val item = pageOrder.removeAt(selectedIdx)
                                    pageOrder.add(selectedIdx - 1, item)
                                    selectedIndices.clear()
                                    selectedIndices.add(selectedIdx - 1)
                                }
                            },
                            enabled = selectedIdx > 0,
                            leadingIcon = Icons.AutoMirrored.Filled.ArrowBack,
                            variant = AppButtonVariant.SECONDARY,
                            height = 32.dp
                        )

                        AppButton(
                            text = "Move Right",
                            onClick = {
                                if (selectedIdx < pageOrder.size - 1) {
                                    val item = pageOrder.removeAt(selectedIdx)
                                    pageOrder.add(selectedIdx + 1, item)
                                    selectedIndices.clear()
                                    selectedIndices.add(selectedIdx + 1)
                                }
                            },
                            enabled = selectedIdx < pageOrder.size - 1,
                            leadingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                            variant = AppButtonVariant.SECONDARY,
                            height = 32.dp
                        )
                    }
                }
            }

            // Thumbnail Grid
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(pageOrder, key = { index, originalPageIndex -> "p_${index}_$originalPageIndex" }) { displayIndex, originalPageIndex ->
                        val isSelected = selectedIndices.contains(displayIndex)
                        val rotation = pageRotations[originalPageIndex] ?: 0

                        OrganizerPageThumbnailCard(
                            displayIndex = displayIndex,
                            originalPageIndex = originalPageIndex,
                            rotation = rotation,
                            isSelected = isSelected,
                            renderer = renderer,
                            onClick = {
                                if (isSelected) {
                                    selectedIndices.remove(displayIndex)
                                } else {
                                    selectedIndices.add(displayIndex)
                                }
                            }
                        )
                    }
                }
            }

            // Bottom Confirmation Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    enabled = !isApplying,
                    variant = AppButtonVariant.OUTLINED,
                    modifier = Modifier.weight(1f)
                )

                AppButton(
                    text = if (isApplying) "Applying..." else "Save Changes (${pageOrder.size} Pages)",
                    onClick = {
                        onApplyChanges(pageOrder.toList(), pageRotations.toMap())
                    },
                    enabled = !isApplying && pageOrder.isNotEmpty(),
                    variant = AppButtonVariant.PRIMARY,
                    modifier = Modifier.weight(1.5f)
                )
            }
        }
    }
}

@Composable
private fun OrganizerPageThumbnailCard(
    displayIndex: Int,
    originalPageIndex: Int,
    rotation: Int,
    isSelected: Boolean,
    renderer: PdfPageRenderer?,
    onClick: () -> Unit
) {
    var thumbnail by remember(originalPageIndex) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(originalPageIndex) { mutableStateOf(true) }

    LaunchedEffect(originalPageIndex, renderer) {
        if (renderer != null) {
            isLoading = true
            try {
                val targetWidth = 220
                val aspect = renderer.getPageAspectRatio(originalPageIndex)
                val targetHeight = com.arinara.fotara.util.PdfLayoutMath.computeTargetSize(targetWidth, aspect).heightPx
                thumbnail = renderer.renderPage(originalPageIndex, targetWidth, targetHeight)
            } catch (_: Exception) {
                thumbnail = null
            } finally {
                isLoading = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.72f)
            .clip(RoundedCornerShape(12.dp))
            .background(ElectricSlateTokens.SurfaceElevated)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.BorderSubtle,
                shape = RoundedCornerShape(12.dp)
            )
            .jigglePressClickable(onClick = onClick)
    ) {
        val bmp = thumbnail
        if (bmp != null && !bmp.isRecycled) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Page ${displayIndex + 1}",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .graphicsLayer {
                        rotationZ = rotation.toFloat()
                    }
            )
        } else if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    color = ElectricSlateTokens.PrimaryAccent,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            }
        }

        // Page Number Pill
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = ElectricSlateTokens.BgCanvas.copy(alpha = 0.85f),
            border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp)
        ) {
            Text(
                text = "${displayIndex + 1}",
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }

        // Selection Indicator Checkbox
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(20.dp)
                    .background(ElectricSlateTokens.PrimaryAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = ElectricSlateTokens.TextPrimary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
