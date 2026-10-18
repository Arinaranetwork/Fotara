// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.canvas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.canvas.model.CanvasLayer
import com.arinara.fotara.ui.components.presets.AppButton
import com.arinara.fotara.ui.components.presets.AppButtonVariant
import com.arinara.fotara.ui.components.presets.AppSlider
import com.arinara.fotara.ui.theme.ElectricSlateTokens

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CanvasLayersSheet(
    layers: List<CanvasLayer>,
    activeLayerId: String,
    onDismissRequest: () -> Unit,
    onAddLayer: () -> Unit,
    onSetActiveLayer: (String) -> Unit,
    onRenameLayer: (String, String) -> Unit,
    onMoveLayerUp: (String) -> Unit,
    onMoveLayerDown: (String) -> Unit,
    onToggleVisibility: (String) -> Unit,
    onToggleLock: (String) -> Unit,
    onDeleteLayer: (String) -> Unit,
    onSetOpacity: (String, Float) -> Unit
) {
    var editingLayerId by remember { mutableStateOf<String?>(null) }
    var editingLayerName by remember { mutableStateOf(TextFieldValue("")) }
    val renameFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = ElectricSlateTokens.SurfacePanel,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Layers",
                    color = ElectricSlateTokens.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                AppButton(
                    text = "Add Layer",
                    onClick = onAddLayer,
                    leadingIcon = Icons.Default.Add,
                    variant = AppButtonVariant.PRIMARY,
                    height = 36.dp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                items(layers.sortedByDescending { it.order }) { layer ->
                    val isActive = layer.id == activeLayerId
                    Surface(
                        color = if (isActive) ElectricSlateTokens.SurfaceElevated else ElectricSlateTokens.SurfacePanel,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            width = if (isActive) 2.dp else 1.dp,
                            color = if (isActive) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.BorderSubtle
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onSetActiveLayer(layer.id) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (editingLayerId == layer.id) {
                                    BasicTextField(
                                        value = editingLayerName,
                                        onValueChange = { editingLayerName = it },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            color = ElectricSlateTokens.TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        cursorBrush = SolidColor(ElectricSlateTokens.PrimaryAccent),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(
                                            onDone = {
                                                onRenameLayer(layer.id, editingLayerName.text)
                                                editingLayerId = null
                                                keyboardController?.hide()
                                            }
                                        ),
                                        modifier = Modifier
                                            .weight(1f, fill = false)
                                            .focusRequester(renameFocusRequester)
                                            .onFocusChanged { focusState ->
                                                if (!focusState.isFocused && editingLayerId == layer.id) {
                                                    onRenameLayer(layer.id, editingLayerName.text)
                                                    editingLayerId = null
                                                }
                                            }
                                    )
                                } else {
                                    Text(
                                        text = layer.name,
                                        color = ElectricSlateTokens.TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier
                                            .weight(1f, fill = false)
                                            .combinedClickable(
                                                onClick = { onSetActiveLayer(layer.id) },
                                                onLongClick = {
                                                    editingLayerId = layer.id
                                                    editingLayerName = TextFieldValue(
                                                        text = layer.name,
                                                        selection = TextRange(0, layer.name.length)
                                                    )
                                                }
                                            )
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onMoveLayerUp(layer.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.KeyboardArrowUp,
                                            contentDescription = "Move Up",
                                            tint = ElectricSlateTokens.TextPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onMoveLayerDown(layer.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Move Down",
                                            tint = ElectricSlateTokens.TextPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onToggleVisibility(layer.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Visibility",
                                            tint = if (layer.isVisible) ElectricSlateTokens.TextPrimary else ElectricSlateTokens.TextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onToggleLock(layer.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (layer.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                            contentDescription = "Lock",
                                            tint = if (layer.isLocked) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteLayer(layer.id) },
                                        enabled = layers.size > 1,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Layer",
                                            tint = if (layers.size > 1) ElectricSlateTokens.Destructive else ElectricSlateTokens.TextMuted.copy(alpha = 0.3f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Opacity",
                                    color = ElectricSlateTokens.TextMuted,
                                    fontSize = 12.sp,
                                    modifier = Modifier.width(48.dp)
                                )
                                AppSlider(
                                    value = layer.opacity,
                                    onValueChange = { onSetOpacity(layer.id, it) },
                                    valueRange = 0.05f..1.0f,
                                    steps = 0,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${(layer.opacity * 100).toInt()}%",
                                    color = ElectricSlateTokens.TextPrimary,
                                    fontSize = 12.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.width(44.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasShareSheet(
    onDismissRequest: () -> Unit,
    onExportPng: () -> Unit,
    onExportPdf: () -> Unit,
    onExportFotara: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = ElectricSlateTokens.SurfacePanel,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Share Canvas Note",
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "SHARE AS IMAGE",
                color = ElectricSlateTokens.TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = ElectricSlateTokens.SurfaceElevated,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExportPng() }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        tint = ElectricSlateTokens.PrimaryAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Export as PNG",
                            color = ElectricSlateTokens.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "High-resolution vector image of current sheet or selection",
                            color = ElectricSlateTokens.TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = ElectricSlateTokens.SurfaceElevated,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExportPdf() }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.DriveFileMove,
                        contentDescription = null,
                        tint = ElectricSlateTokens.PrimaryAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Export Multi-Sheet PDF",
                            color = ElectricSlateTokens.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Complete vector document with all canvas sheets included",
                            color = ElectricSlateTokens.TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "SHARE AS NOTE FILE",
                color = ElectricSlateTokens.TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = ElectricSlateTokens.SurfaceElevated,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExportFotara() }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        tint = ElectricSlateTokens.PrimaryAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Share Note File (.fotara)",
                            color = ElectricSlateTokens.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Native standalone package importable into Fotara",
                            color = ElectricSlateTokens.TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasMoveElementsLayerSheet(
    layers: List<CanvasLayer>,
    onDismissRequest: () -> Unit,
    onSelectLayer: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = ElectricSlateTokens.SurfacePanel,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Move Selection to Layer",
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            layers.forEach { layer ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectLayer(layer.id) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Layers,
                        contentDescription = null,
                        tint = ElectricSlateTokens.PrimaryAccent
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = layer.name,
                        color = ElectricSlateTokens.TextPrimary,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
