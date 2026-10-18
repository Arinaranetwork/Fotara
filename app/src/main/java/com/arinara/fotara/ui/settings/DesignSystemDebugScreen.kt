// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.ui.components.presets.AppBottomSheet
import com.arinara.fotara.ui.components.presets.AppButton
import com.arinara.fotara.ui.components.presets.AppButtonVariant
import com.arinara.fotara.ui.components.presets.AppCard
import com.arinara.fotara.ui.components.presets.AppFAB
import com.arinara.fotara.ui.components.presets.AppFloatingBar
import com.arinara.fotara.ui.components.presets.AppFloatingBarItem
import com.arinara.fotara.ui.components.presets.AppListItem
import com.arinara.fotara.ui.components.presets.AppListItemAction
import com.arinara.fotara.ui.components.presets.AppNoticeBanner
import com.arinara.fotara.ui.components.presets.AppNoticeVariant
import com.arinara.fotara.ui.components.presets.AppPillChip
import com.arinara.fotara.ui.components.presets.AppScreen
import com.arinara.fotara.ui.components.presets.AppSlider
import com.arinara.fotara.ui.components.presets.AppTopBar
import com.arinara.fotara.ui.components.presets.BottomClearanceSpacer
import com.arinara.fotara.ui.components.presets.ConnectedInspector
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import com.arinara.fotara.ui.theme.jigglePressClickable

@Composable
fun DesignSystemDebugScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    BackHandler(onBack = onBack)

    // Interactive Demo States
    var cardTapCount by remember { mutableIntStateOf(0) }
    var selectedChipIndex by remember { mutableIntStateOf(0) }
    var sliderValue by remember { mutableFloatStateOf(0.4f) }
    var switchState by remember { mutableStateOf(true) }
    var lastDeletedItem by remember { mutableStateOf<String?>(null) }
    var noticeBannerVariant by remember { mutableStateOf<AppNoticeVariant?>(AppNoticeVariant.INFO) }
    var showNoticeBanner by remember { mutableStateOf(true) }
    var activeFloatingToolIndex by remember { mutableIntStateOf(1) }
    var showConnectedInspector by remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var isSheetMinimized by remember { mutableStateOf(false) }

    AppScreen(
        modifier = modifier,
        topBar = {
            // Preset 1: AppTopBar with Interactive Rotation Jiggle Back Button
            AppTopBar(
                title = "Design System Showcase",
                subtitle = "Electric Slate UI Presets & Tokens",
                onBackClick = onBack,
                actions = {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .jigglePressClickable {
                                Toast.makeText(context, "Search action triggered", Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = ElectricSlateTokens.TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
        },
        minBottomClearance = 120.dp
    ) { screenPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = screenPadding.calculateTopPadding())
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(
                    bottom = screenPadding.calculateBottomPadding()
                ),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                // Section 10: Color Palette Swatches Grid
                item {
                    SectionHeader(title = "10. Electric Slate Token Swatches", subtitle = "Tap any swatch to copy hexadecimal value")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ElectricSlateTokens.AllSwatches.chunked(3).forEach { rowSwatches ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowSwatches.forEach { swatch ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        SwatchItem(
                                            swatch = swatch,
                                            onCopy = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText(swatch.name, swatch.hex))
                                                Toast.makeText(context, "${swatch.name} copied: ${swatch.hex}", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 2: AppCard (Standard vs. Ambient Glow Border)
                item {
                    SectionHeader(title = "2. AppCard Preset", subtitle = "24dp radius with JigglePress and Ambient Glow")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AppCard(
                            modifier = Modifier.weight(1f),
                            onClick = { cardTapCount++ }
                        ) {
                            Text(
                                text = "Standard Card",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Taps: $cardTapCount",
                                fontFamily = ElmsSans,
                                fontSize = 12.sp,
                                color = ElectricSlateTokens.TextMuted
                            )
                        }

                        AppCard(
                            modifier = Modifier.weight(1f),
                            isGlow = true,
                            onClick = { cardTapCount += 2 }
                        ) {
                            Text(
                                text = "Ambient Glow",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = ElectricSlateTokens.PrimaryAccent
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Glow Active",
                                fontFamily = ElmsSans,
                                fontSize = 12.sp,
                                color = ElectricSlateTokens.TextMuted
                            )
                        }
                    }
                }

                // Section 8: AppButton & AppFAB Variants
                item {
                    SectionHeader(title = "8. AppButton & AppFAB Presets", subtitle = "Scale-pop press physics across all variants")
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AppButton(
                                text = "Primary",
                                variant = AppButtonVariant.PRIMARY,
                                leadingIcon = Icons.Default.Check,
                                modifier = Modifier.weight(1f),
                                onClick = { Toast.makeText(context, "Primary Clicked", Toast.LENGTH_SHORT).show() }
                            )
                            AppButton(
                                text = "Secondary",
                                variant = AppButtonVariant.SECONDARY,
                                modifier = Modifier.weight(1f),
                                onClick = { Toast.makeText(context, "Secondary Clicked", Toast.LENGTH_SHORT).show() }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppButton(
                                text = "Destructive",
                                variant = AppButtonVariant.DESTRUCTIVE,
                                modifier = Modifier.weight(1f),
                                onClick = { Toast.makeText(context, "Destructive Clicked", Toast.LENGTH_SHORT).show() }
                            )
                            AppButton(
                                text = "Disabled",
                                variant = AppButtonVariant.PRIMARY,
                                enabled = false,
                                modifier = Modifier.weight(1f),
                                onClick = {}
                            )
                            AppFAB(
                                icon = Icons.Default.Add,
                                onClick = { Toast.makeText(context, "FAB Clicked", Toast.LENGTH_SHORT).show() }
                            )
                        }
                    }
                }

                // Section 9: AppPillChip
                item {
                    SectionHeader(title = "9. AppPillChip Preset", subtitle = "999dp capsule chips with selection spring scale pop")
                    val chipLabels = listOf("All Items", "Recent Notes", "Formulas", "Archived")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(chipLabels.indices.toList()) { idx ->
                            AppPillChip(
                                label = chipLabels[idx],
                                isSelected = selectedChipIndex == idx,
                                trailingCount = if (idx == 0) 42 else (idx * 5 + 3),
                                onClick = { selectedChipIndex = idx }
                            )
                        }
                    }
                }

                // Section 5: AppSlider
                item {
                    SectionHeader(
                        title = "5. Magnetic AppSlider Preset",
                        subtitle = "Tactile thumb jiggle (1.25x) & magnetic snap bounce at steps: ${(sliderValue * 100).toInt()}%"
                    )
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        AppSlider(
                            value = sliderValue,
                            onValueChange = { sliderValue = it },
                            steps = 4
                        )
                    }
                }

                // Section 7: AppListItem
                item {
                    SectionHeader(title = "7. AppListItem Preset", subtitle = "Standardized rows with Chevron, Bouncy Switch, & Shaking Trash")
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = 4.dp
                    ) {
                        AppListItem(
                            title = "Sub-Directory Navigation",
                            subtitle = "Navigates to child folder",
                            leadingIcon = Icons.Default.Folder,
                            action = AppListItemAction.Chevron,
                            onClick = { Toast.makeText(context, "Opening folder", Toast.LENGTH_SHORT).show() }
                        )

                        AppListItem(
                            title = "Tactile Spring Switch",
                            subtitle = if (switchState) "Enabled" else "Disabled",
                            leadingIcon = Icons.Default.Settings,
                            action = AppListItemAction.Toggle(
                                checked = switchState,
                                onCheckedChange = { switchState = it }
                            )
                        )

                        AppListItem(
                            title = "Remove Cached Workspace",
                            subtitle = if (lastDeletedItem != null) "Deleted $lastDeletedItem" else "Tap trash icon to shake and remove",
                            leadingIcon = Icons.Default.Category,
                            action = AppListItemAction.ShakingTrash(
                                onDelete = {
                                    lastDeletedItem = "Workspace #${System.currentTimeMillis() % 1000}"
                                    Toast.makeText(context, "Item purged: $lastDeletedItem", Toast.LENGTH_SHORT).show()
                                }
                            )
                        )
                    }
                }

                // Section 10: AppNoticeBanner
                item {
                    SectionHeader(title = "10. AppNoticeBanner Preset", subtitle = "Overshoot slide enter and DismissFade exit")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppButton(
                            text = "Info",
                            variant = AppButtonVariant.SECONDARY,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                noticeBannerVariant = AppNoticeVariant.INFO
                                showNoticeBanner = true
                            }
                        )
                        AppButton(
                            text = "Warning",
                            variant = AppButtonVariant.SECONDARY,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                noticeBannerVariant = AppNoticeVariant.WARNING
                                showNoticeBanner = true
                            }
                        )
                        AppButton(
                            text = "Error",
                            variant = AppButtonVariant.SECONDARY,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                noticeBannerVariant = AppNoticeVariant.ERROR
                                showNoticeBanner = true
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    AppNoticeBanner(
                        visible = showNoticeBanner,
                        title = when (noticeBannerVariant) {
                            AppNoticeVariant.INFO -> "Information Notice"
                            AppNoticeVariant.WARNING -> "Warning Alert"
                            AppNoticeVariant.ERROR -> "Critical Exception"
                            else -> "Notice"
                        },
                        message = "This banner enters with spring overshoot and exits with a DismissFade collapse.",
                        variant = noticeBannerVariant ?: AppNoticeVariant.INFO,
                        actionText = "Action",
                        onActionClick = { Toast.makeText(context, "Action triggered", Toast.LENGTH_SHORT).show() },
                        onDismiss = { showNoticeBanner = false }
                    )
                }

                // Section 4 & 3: Floating Bar & Connected Inspector
                item {
                    SectionHeader(
                        title = "3 & 4. Floating Bar & Connected Inspector",
                        subtitle = "Dock with organic gooey droplet neck attached to active tool"
                    )
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Inspector Neck: ${if (showConnectedInspector) "Connected" else "Detached"}",
                                fontFamily = ElmsSans,
                                fontSize = 14.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                            AppButton(
                                text = if (showConnectedInspector) "Hide Inspector" else "Show Inspector",
                                variant = AppButtonVariant.PRIMARY,
                                height = 38.dp,
                                onClick = { showConnectedInspector = !showConnectedInspector }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Live preview of ConnectedInspector
                        ConnectedInspector(
                            visible = showConnectedInspector,
                            neckAnchorOffsetRatio = when (activeFloatingToolIndex) {
                                0 -> 0.2f
                                1 -> 0.4f
                                2 -> 0.6f
                                else -> 0.8f
                            }
                        ) {
                            Text(
                                text = "Tool Inspector",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Organic droplet neck bridges directly to active dock tool below using ViscousGooey spring physics.",
                                fontFamily = ElmsSans,
                                fontSize = 12.sp,
                                color = ElectricSlateTokens.TextMuted
                            )
                        }

                        // Mini Floating Bar inside showcase card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CircleShape)
                                .background(ElectricSlateTokens.SurfaceElevated)
                                .border(1.dp, ElectricSlateTokens.BorderSubtle, CircleShape)
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val tools = listOf(
                                Icons.Default.Brush to "Pen",
                                Icons.Default.Edit to "Highlighter",
                                Icons.Default.CropSquare to "Shapes",
                                Icons.Default.Palette to "Colors"
                            )
                            tools.forEachIndexed { idx, tool ->
                                AppFloatingBarItem(
                                    icon = tool.first,
                                    contentDescription = tool.second,
                                    isSelected = activeFloatingToolIndex == idx,
                                    onClick = {
                                        activeFloatingToolIndex = idx
                                        showConnectedInspector = true
                                    }
                                )
                            }
                        }
                    }
                }

                // Section 6: AppBottomSheet
                item {
                    SectionHeader(title = "6. AppBottomSheet Preset", subtitle = "Elastic resistance drag with minimizable bottom pill mode")
                    AppButton(
                        text = "Open Minimizable Bottom Sheet",
                        variant = AppButtonVariant.PRIMARY,
                        leadingIcon = Icons.Default.Timer,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            isSheetMinimized = false
                            showBottomSheet = true
                        }
                    )
                }

                // Guaranteed bottom clearance spacer ensuring last sections are fully visible
                item {
                    BottomClearanceSpacer(100.dp)
                }
            }

        // Overlay: Minimizable AppBottomSheet instance
        AppBottomSheet(
            visible = showBottomSheet,
            isMinimized = isSheetMinimized,
            title = "Academic Stopwatch & Laps",
            minimizedPillText = "Timer Active: 04:12",
            onMinimizedChange = { isSheetMinimized = it },
            onDismiss = {
                showBottomSheet = false
                isSheetMinimized = false
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Lap Records",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = ElectricSlateTokens.TextPrimary
                )

                listOf(
                    "Lap 3" to "+01:14.20 · Total 04:12.80",
                    "Lap 2" to "+01:28.15 · Total 02:58.60",
                    "Lap 1" to "+01:30.45 · Total 01:30.45"
                ).forEach { lap ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElectricSlateTokens.SurfaceElevated)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = lap.first,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ElectricSlateTokens.PrimaryAccent
                        )
                        Text(
                            text = lap.second,
                            fontFamily = ElmsSans,
                            fontSize = 12.sp,
                            color = ElectricSlateTokens.TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                AppButton(
                    text = "Minimize into Pill Bar",
                    variant = AppButtonVariant.SECONDARY,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { isSheetMinimized = true }
                )
            }
        }
    }
}
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(bottom = 6.dp)) {
        Text(
            text = title,
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = ElectricSlateTokens.TextPrimary
        )
        Text(
            text = subtitle,
            fontFamily = ElmsSans,
            fontSize = 12.sp,
            color = ElectricSlateTokens.TextMuted
        )
    }
}

@Composable
private fun SwatchItem(
    swatch: ElectricSlateTokens.Swatch,
    onCopy: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ElectricSlateTokens.SurfacePanel)
            .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
            .jigglePressClickable(onClick = onCopy)
            .padding(10.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(swatch.color)
                    .border(1.dp, ElectricSlateTokens.BorderSubtle.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = swatch.name,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = ElectricSlateTokens.TextPrimary,
                maxLines = 1
            )
            Text(
                text = swatch.hex,
                fontFamily = ElmsSans,
                fontSize = 10.sp,
                color = ElectricSlateTokens.TextMuted
            )
        }
    }
}

@Preview(
    name = "Electric Slate Showcase",
    showBackground = true,
    backgroundColor = 0xFF0D1117,
    device = "spec:width=412dp,height=915dp"
)
@Composable
fun DesignSystemDebugScreenPreview() {
    DesignSystemDebugScreen(onBack = {})
}
