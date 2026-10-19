// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.vfx

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.theme.ElmsSans
import java.util.Locale

private val SheetBackground = Color(0xFF0F172A)
private val SectionBorder = Color(0xFF334155)
private val TextWhite = Color(0xFFF8FAFC)
private val TextMuted = Color(0xFF94A3B8)
private val AccentGold = Color(0xFFFBBF24)
private val AccentPurple = Color(0xFF8B5CF6)

private val KEY_COLORS = listOf(
    Pair(0xFF00FF00, "Green"),
    Pair(0xFF0000FF, "Blue"),
    Pair(0xFF000000, "Black"),
    Pair(0xFFFFFFFF, "White")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VfxControlBottomSheet(
    parameters: VfxParameters,
    onParametersChanged: (VfxParameters) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SheetBackground,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = null,
                        tint = AccentPurple,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Visual Effects (VFX)",
                        fontFamily = ElmsSans,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { onParametersChanged(VfxParameters()) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Reset",
                            color = AccentGold,
                            fontSize = 12.sp,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 1: Chroma Key
            Text(
                text = "CHROMA KEY (KNOCKOUT)",
                color = AccentPurple,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ElmsSans,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Enable Chroma Key",
                    color = TextWhite,
                    fontSize = 14.sp,
                    fontFamily = ElmsSans
                )
                Switch(
                    checked = parameters.chromaKeyEnabled,
                    onCheckedChange = { onParametersChanged(parameters.copy(chromaKeyEnabled = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AccentPurple
                    )
                )
            }

            if (parameters.chromaKeyEnabled) {
                Spacer(modifier = Modifier.height(10.dp))

                // Key Color Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Key Color:", color = TextMuted, fontSize = 12.sp, fontFamily = ElmsSans)
                    KEY_COLORS.forEach { (colorLong, label) ->
                        val isSelected = parameters.keyColor == colorLong
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(colorLong))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) AccentGold else SectionBorder,
                                    shape = CircleShape
                                )
                                .clickable { onParametersChanged(parameters.copy(keyColor = colorLong)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                VfxSlider(
                    label = "Tolerance",
                    value = parameters.tolerance,
                    valueRange = 0.05f..0.85f,
                    displayFormat = "${(parameters.tolerance * 100).toInt()}%",
                    onValueChange = { onParametersChanged(parameters.copy(tolerance = it)) }
                )

                VfxSlider(
                    label = "Edge Softening",
                    value = parameters.edgeSoftening,
                    valueRange = 0.01f..0.50f,
                    displayFormat = "${(parameters.edgeSoftening * 100).toInt()}%",
                    onValueChange = { onParametersChanged(parameters.copy(edgeSoftening = it)) }
                )
            }

            HorizontalDivider(color = SectionBorder, modifier = Modifier.padding(vertical = 14.dp))

            // Section 2: Color & Channel Grading
            Text(
                text = "COLOR & CHANNEL GRADING",
                color = AccentPurple,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ElmsSans,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            VfxSlider(
                label = "Brightness",
                value = parameters.brightness,
                valueRange = -0.5f..0.5f,
                displayFormat = "${(parameters.brightness * 100).toInt()}%",
                onValueChange = { onParametersChanged(parameters.copy(brightness = it)) }
            )

            VfxSlider(
                label = "Contrast",
                value = parameters.contrast,
                valueRange = 0.5f..1.8f,
                displayFormat = "${(parameters.contrast * 100).toInt()}%",
                onValueChange = { onParametersChanged(parameters.copy(contrast = it)) }
            )

            VfxSlider(
                label = "Saturation",
                value = parameters.saturation,
                valueRange = 0.0f..2.0f,
                displayFormat = "${(parameters.saturation * 100).toInt()}%",
                onValueChange = { onParametersChanged(parameters.copy(saturation = it)) }
            )

            VfxSlider(
                label = "Temperature",
                value = parameters.temperature,
                valueRange = -0.5f..0.5f,
                displayFormat = if (parameters.temperature < 0) "Cool ${(parameters.temperature * -100).toInt()}%" else "Warm ${(parameters.temperature * 100).toInt()}%",
                onValueChange = { onParametersChanged(parameters.copy(temperature = it)) }
            )

            Spacer(modifier = Modifier.height(6.dp))
            Text("RGB Channel Balance", color = TextMuted, fontSize = 12.sp, fontFamily = ElmsSans)
            Spacer(modifier = Modifier.height(4.dp))

            VfxSlider(
                label = "Red Balance",
                value = parameters.redBalance,
                valueRange = 0.5f..1.5f,
                displayFormat = "${(parameters.redBalance * 100).toInt()}%",
                onValueChange = { onParametersChanged(parameters.copy(redBalance = it)) }
            )

            VfxSlider(
                label = "Green Balance",
                value = parameters.greenBalance,
                valueRange = 0.5f..1.5f,
                displayFormat = "${(parameters.greenBalance * 100).toInt()}%",
                onValueChange = { onParametersChanged(parameters.copy(greenBalance = it)) }
            )

            VfxSlider(
                label = "Blue Balance",
                value = parameters.blueBalance,
                valueRange = 0.5f..1.5f,
                displayFormat = "${(parameters.blueBalance * 100).toInt()}%",
                onValueChange = { onParametersChanged(parameters.copy(blueBalance = it)) }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun VfxSidebarPanel(
    parameters: VfxParameters,
    onParametersChanged: (VfxParameters) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SheetBackground.copy(alpha = 0.95f),
        shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
        border = BorderStroke(1.dp, SectionBorder),
        shadowElevation = 16.dp,
        modifier = modifier
            .width(280.dp)
            .padding(vertical = 54.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = null,
                        tint = AccentPurple,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Visual Effects",
                        fontFamily = ElmsSans,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onParametersChanged(VfxParameters()) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = AccentGold,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted, modifier = Modifier.size(15.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Section 1: Chroma Key
            Text(
                text = "CHROMA KEY (KNOCKOUT)",
                color = AccentPurple,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ElmsSans,
                letterSpacing = 0.8.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Enable Chroma Key",
                    color = TextWhite,
                    fontSize = 12.5.sp,
                    fontFamily = ElmsSans
                )
                Switch(
                    checked = parameters.chromaKeyEnabled,
                    onCheckedChange = { onParametersChanged(parameters.copy(chromaKeyEnabled = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AccentPurple
                    )
                )
            }

            if (parameters.chromaKeyEnabled) {
                Spacer(modifier = Modifier.height(6.dp))

                // Key Color Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Key Color:", color = TextMuted, fontSize = 11.sp, fontFamily = ElmsSans)
                    KEY_COLORS.forEach { (colorLong, label) ->
                        val isSelected = parameters.keyColor == colorLong
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(colorLong))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) AccentGold else SectionBorder,
                                    shape = CircleShape
                                )
                                .clickable { onParametersChanged(parameters.copy(keyColor = colorLong)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                VfxSlider(
                    label = "Tolerance",
                    value = parameters.tolerance,
                    valueRange = 0.05f..0.85f,
                    displayFormat = "${(parameters.tolerance * 100).toInt()}%",
                    onValueChange = { onParametersChanged(parameters.copy(tolerance = it)) }
                )

                VfxSlider(
                    label = "Edge Softening",
                    value = parameters.edgeSoftening,
                    valueRange = 0.01f..0.50f,
                    displayFormat = "${(parameters.edgeSoftening * 100).toInt()}%",
                    onValueChange = { onParametersChanged(parameters.copy(edgeSoftening = it)) }
                )
            }

            HorizontalDivider(color = SectionBorder, modifier = Modifier.padding(vertical = 8.dp))

            // Section 2: Color & Channel Grading
            Text(
                text = "COLOR & CHANNEL GRADING",
                color = AccentPurple,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ElmsSans,
                letterSpacing = 0.8.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            VfxSlider(
                label = "Brightness",
                value = parameters.brightness,
                valueRange = -0.5f..0.5f,
                displayFormat = "${(parameters.brightness * 100).toInt()}%",
                onValueChange = { onParametersChanged(parameters.copy(brightness = it)) }
            )

            VfxSlider(
                label = "Contrast",
                value = parameters.contrast,
                valueRange = 0.5f..1.8f,
                displayFormat = "${(parameters.contrast * 100).toInt()}%",
                onValueChange = { onParametersChanged(parameters.copy(contrast = it)) }
            )

            VfxSlider(
                label = "Saturation",
                value = parameters.saturation,
                valueRange = 0.0f..2.0f,
                displayFormat = "${(parameters.saturation * 100).toInt()}%",
                onValueChange = { onParametersChanged(parameters.copy(saturation = it)) }
            )

            VfxSlider(
                label = "Temperature",
                value = parameters.temperature,
                valueRange = -0.5f..0.5f,
                displayFormat = "${(parameters.temperature * 100).toInt()}%",
                onValueChange = { onParametersChanged(parameters.copy(temperature = it)) }
            )

            HorizontalDivider(color = SectionBorder, modifier = Modifier.padding(vertical = 8.dp))

            // Section 3: Independent RGB Balance
            Text(
                text = "RGB CHANNEL BALANCE",
                color = AccentPurple,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ElmsSans,
                letterSpacing = 0.8.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            VfxSlider(
                label = "Red Balance",
                value = parameters.redBalance,
                valueRange = 0.5f..1.5f,
                displayFormat = "${(parameters.redBalance * 100).toInt()}%",
                onValueChange = { onParametersChanged(parameters.copy(redBalance = it)) }
            )

            VfxSlider(
                label = "Green Balance",
                value = parameters.greenBalance,
                valueRange = 0.5f..1.5f,
                displayFormat = "${(parameters.greenBalance * 100).toInt()}%",
                onValueChange = { onParametersChanged(parameters.copy(greenBalance = it)) }
            )

            VfxSlider(
                label = "Blue Balance",
                value = parameters.blueBalance,
                valueRange = 0.5f..1.5f,
                displayFormat = "${(parameters.blueBalance * 100).toInt()}%",
                onValueChange = { onParametersChanged(parameters.copy(blueBalance = it)) }
            )

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable
private fun VfxSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    displayFormat: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = TextWhite, fontSize = 13.sp, fontFamily = ElmsSans)
            Text(text = displayFormat, color = TextMuted, fontSize = 12.sp, fontFamily = ElmsSans)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = AccentPurple,
                activeTrackColor = AccentPurple,
                inactiveTrackColor = SectionBorder
            )
        )
    }
}

@Composable
fun VfxBottomPanel(
    parameters: VfxParameters,
    onParametersChanged: (VfxParameters) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = com.arinara.fotara.ui.theme.ElectricSlateTokens.SurfacePanel.copy(alpha = 0.98f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        border = BorderStroke(1.dp, com.arinara.fotara.ui.theme.ElectricSlateTokens.BorderSubtle),
        shadowElevation = 16.dp,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 280.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = null,
                        tint = com.arinara.fotara.ui.theme.ElectricSlateTokens.PrimaryAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Visual Effects",
                        fontFamily = ElmsSans,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = com.arinara.fotara.ui.theme.ElectricSlateTokens.TextPrimary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onParametersChanged(VfxParameters()) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = com.arinara.fotara.ui.theme.ElectricSlateTokens.TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = com.arinara.fotara.ui.theme.ElectricSlateTokens.TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            HorizontalDivider(
                color = com.arinara.fotara.ui.theme.ElectricSlateTokens.BorderSubtle,
                modifier = Modifier.padding(vertical = 6.dp)
            )

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Section 1: Chroma Key
                Text(
                    text = "CHROMA KEY (KNOCKOUT)",
                    color = com.arinara.fotara.ui.theme.ElectricSlateTokens.PrimaryAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ElmsSans,
                    letterSpacing = 0.8.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Enable Chroma Key",
                        color = com.arinara.fotara.ui.theme.ElectricSlateTokens.TextPrimary,
                        fontSize = 13.sp,
                        fontFamily = ElmsSans
                    )
                    Switch(
                        checked = parameters.chromaKeyEnabled,
                        onCheckedChange = { onParametersChanged(parameters.copy(chromaKeyEnabled = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = com.arinara.fotara.ui.theme.ElectricSlateTokens.PrimaryAccent,
                            uncheckedThumbColor = com.arinara.fotara.ui.theme.ElectricSlateTokens.TextMuted,
                            uncheckedTrackColor = com.arinara.fotara.ui.theme.ElectricSlateTokens.TrackInactive
                        )
                    )
                }

                if (parameters.chromaKeyEnabled) {
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Key Color:", color = com.arinara.fotara.ui.theme.ElectricSlateTokens.TextMuted, fontSize = 11.sp, fontFamily = ElmsSans)
                        KEY_COLORS.forEach { (colorLong, _) ->
                            val isSelected = parameters.keyColor == colorLong
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorLong))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) com.arinara.fotara.ui.theme.ElectricSlateTokens.PrimaryAccent else com.arinara.fotara.ui.theme.ElectricSlateTokens.BorderSubtle,
                                        shape = CircleShape
                                    )
                                    .clickable { onParametersChanged(parameters.copy(keyColor = colorLong)) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    VfxSlider(
                        label = "Tolerance",
                        value = parameters.tolerance,
                        valueRange = 0.05f..0.85f,
                        displayFormat = "${(parameters.tolerance * 100).toInt()}%",
                        onValueChange = { onParametersChanged(parameters.copy(tolerance = it)) }
                    )

                    VfxSlider(
                        label = "Edge Softening",
                        value = parameters.edgeSoftening,
                        valueRange = 0.01f..0.50f,
                        displayFormat = "${(parameters.edgeSoftening * 100).toInt()}%",
                        onValueChange = { onParametersChanged(parameters.copy(edgeSoftening = it)) }
                    )
                }

                HorizontalDivider(
                    color = com.arinara.fotara.ui.theme.ElectricSlateTokens.BorderSubtle,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Section 2: Color & Channel Grading
                Text(
                    text = "COLOR & CHANNEL GRADING",
                    color = com.arinara.fotara.ui.theme.ElectricSlateTokens.PrimaryAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ElmsSans,
                    letterSpacing = 0.8.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                VfxSlider(
                    label = "Brightness",
                    value = parameters.brightness,
                    valueRange = -0.5f..0.5f,
                    displayFormat = "${(parameters.brightness * 100).toInt()}%",
                    onValueChange = { onParametersChanged(parameters.copy(brightness = it)) }
                )

                VfxSlider(
                    label = "Contrast",
                    value = parameters.contrast,
                    valueRange = 0.5f..1.8f,
                    displayFormat = "${(parameters.contrast * 100).toInt()}%",
                    onValueChange = { onParametersChanged(parameters.copy(contrast = it)) }
                )

                VfxSlider(
                    label = "Saturation",
                    value = parameters.saturation,
                    valueRange = 0.0f..2.0f,
                    displayFormat = "${(parameters.saturation * 100).toInt()}%",
                    onValueChange = { onParametersChanged(parameters.copy(saturation = it)) }
                )

                VfxSlider(
                    label = "Temperature",
                    value = parameters.temperature,
                    valueRange = -0.5f..0.5f,
                    displayFormat = "${(parameters.temperature * 100).toInt()}%",
                    onValueChange = { onParametersChanged(parameters.copy(temperature = it)) }
                )

                HorizontalDivider(
                    color = com.arinara.fotara.ui.theme.ElectricSlateTokens.BorderSubtle,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Section 3: Independent RGB Balance
                Text(
                    text = "RGB CHANNEL BALANCE",
                    color = com.arinara.fotara.ui.theme.ElectricSlateTokens.PrimaryAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ElmsSans,
                    letterSpacing = 0.8.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                VfxSlider(
                    label = "Red Balance",
                    value = parameters.redBalance,
                    valueRange = 0.5f..1.5f,
                    displayFormat = "${(parameters.redBalance * 100).toInt()}%",
                    onValueChange = { onParametersChanged(parameters.copy(redBalance = it)) }
                )

                VfxSlider(
                    label = "Green Balance",
                    value = parameters.greenBalance,
                    valueRange = 0.5f..1.5f,
                    displayFormat = "${(parameters.greenBalance * 100).toInt()}%",
                    onValueChange = { onParametersChanged(parameters.copy(greenBalance = it)) }
                )

                VfxSlider(
                    label = "Blue Balance",
                    value = parameters.blueBalance,
                    valueRange = 0.5f..1.5f,
                    displayFormat = "${(parameters.blueBalance * 100).toInt()}%",
                    onValueChange = { onParametersChanged(parameters.copy(blueBalance = it)) }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

