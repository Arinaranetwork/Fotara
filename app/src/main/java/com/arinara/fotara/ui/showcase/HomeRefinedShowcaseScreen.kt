// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.showcase

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.theme.TagAmber
import com.arinara.fotara.theme.TagCrimson
import com.arinara.fotara.theme.TagEmerald
import androidx.compose.material3.IconButton
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

/**
 * Mock data representation for demonstration folder notebook cards.
 */
data class MockSubjectCard(
    val id: String,
    val title: String,
    val noteCount: Int,
    val audioCount: Int,
    val tagColor: Color,
    val tagName: String,
    val lastModified: String
)

/**
 * Mock data representation for recent activities / notes.
 */
data class MockRecentNote(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    var isPinned: Boolean
)

/**
 * High-fidelity replica of the Fotara Home screen composed exclusively with the 10
 * Electric Slate design system presets and strict design tokens.
 *
 * This screen contains fully interactive mock states allowing the user to experience
 * the spring physics, jiggle taps, floating docks, droplet inspectors, bottom sheets,
 * and pill chips directly on an Android device with zero backend dependencies.
 */
@Composable
fun HomeRefinedShowcaseScreen(
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Screen State
    var showNoticeBanner by remember { mutableStateOf(true) }
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    var selectedFolderId by remember { mutableStateOf("calc3") }
    var gridColumns by remember { mutableIntStateOf(2) }
    var activeDockTab by remember { mutableIntStateOf(0) }
    var isInspectorOpen by remember { mutableStateOf(false) }
    var isTimerSheetOpen by remember { mutableStateOf(false) }

    // Mock Timer State
    var isTimerRunning by remember { mutableStateOf(false) }
    var timerSeconds by remember { mutableIntStateOf(142) }
    val lapTimes = remember { mutableStateListOf("02:15.4", "01:48.9", "00:58.2") }

    // Mock Categories
    val categories = remember {
        listOf("All Notebooks", "Mathematics", "Quantum Physics", "Biochemistry", "Lecture Audio", "Exam Cram")
    }

    // Mock Notebook Cards
    val subjectCards = remember {
        listOf(
            MockSubjectCard("calc3", "Calculus III", 16, 2, ElectricSlateTokens.PrimaryAccent, "MATH 302", "15m ago"),
            MockSubjectCard("chem", "Organic Chemistry", 28, 5, TagAmber, "CHEM 201", "2h ago"),
            MockSubjectCard("phys", "Quantum Mechanics", 11, 4, TagCrimson, "PHYS 401", "Yesterday"),
            MockSubjectCard("bio", "Molecular Genetics", 34, 1, TagEmerald, "BIO 210", "3 days ago")
        )
    }

    // Mock Recent Notes
    val recentNotes = remember {
        mutableStateListOf(
            MockRecentNote("n1", "Fourier Analysis Lecture Notes", "PDF Annotation • Page 14 of 42", Icons.Default.Description, true),
            MockRecentNote("n2", "Exam Formulae & Proof Sheet", "Quick reference card • 4 revisions", Icons.Default.Bookmark, false),
            MockRecentNote("n3", "Thermo Lab Session Audio #02", "00:42:15 • Recorded in Room 204", Icons.Default.Mic, false)
        )
    }

    BackHandler(enabled = isInspectorOpen || isTimerSheetOpen) {
        if (isInspectorOpen) {
            isInspectorOpen = false
        } else if (isTimerSheetOpen) {
            isTimerSheetOpen = false
        }
    }

    AppScreen(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = "Fotara Academic",
                subtitle = "Workspace: Natural Sciences",
                onBackClick = onBack,
                actions = {
                    IconButton(
                        onClick = {
                            Toast.makeText(context, "Search notes tapped", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Notes",
                            tint = ElectricSlateTokens.TextPrimary
                        )
                    }
                    IconButton(
                        onClick = {
                            isTimerSheetOpen = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Study Stopwatch",
                            tint = ElectricSlateTokens.TextPrimary
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
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    bottom = screenPadding.calculateBottomPadding()
                )
            ) {
                // 2. AppNoticeBanner Preset
                item {
                    AnimatedVisibility(
                        visible = showNoticeBanner,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            AppNoticeBanner(
                                visible = showNoticeBanner,
                                title = "Encrypted Local Vault",
                                message = "All 4 subject notebooks and 12 lecture recordings are secured on-device.",
                                variant = AppNoticeVariant.INFO,
                                actionText = "Dismiss",
                                onActionClick = { showNoticeBanner = false },
                                onDismiss = { showNoticeBanner = false }
                            )
                        }
                    }
                }

                // 3. AppPillChip Horizontal Category Filter
                item {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories.size) { index ->
                            val label = categories[index]
                            AppPillChip(
                                label = label,
                                isSelected = selectedCategoryIndex == index,
                                onClick = {
                                    selectedCategoryIndex = index
                                }
                            )
                        }
                    }
                }

                // 4. Density Adjuster: AppSlider Preset
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SUBJECT NOTEBOOKS",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                letterSpacing = 1.sp,
                                color = ElectricSlateTokens.TextMuted
                            )
                            Text(
                                text = "Grid: ${gridColumns} Cols",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = ElectricSlateTokens.PrimaryAccent
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        AppSlider(
                            value = gridColumns.toFloat(),
                            onValueChange = { gridColumns = it.toInt() },
                            steps = 2, // 2 steps between min(1) and max(3) -> points: 1, 2, 3
                            valueRange = 1f..3f
                        )
                    }
                }

                // 5. AppCard Grid Preset
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        val chunked = subjectCards.chunked(gridColumns)
                        chunked.forEach { rowCards ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowCards.forEach { card ->
                                    val isSelected = card.id == selectedFolderId
                                    Box(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        AppCard(
                                            onClick = {
                                                selectedFolderId = card.id
                                                Toast.makeText(context, "${card.title} opened", Toast.LENGTH_SHORT).show()
                                            },
                                            isGlow = isSelected,
                                            cornerRadius = 24.dp
                                        ) {
                                            Column(modifier = Modifier.fillMaxWidth()) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Leading Tag Pill
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(card.tagColor.copy(alpha = 0.2f))
                                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        Text(
                                                            text = card.tagName,
                                                            fontFamily = ElmsSans,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 10.sp,
                                                            color = card.tagColor
                                                        )
                                                    }

                                                    Icon(
                                                        imageVector = Icons.Default.MoreVert,
                                                        contentDescription = "Options",
                                                        tint = ElectricSlateTokens.TextMuted,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(14.dp))

                                                Text(
                                                    text = card.title,
                                                    fontFamily = ElmsSans,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = if (gridColumns == 1) 18.sp else 15.sp,
                                                    color = ElectricSlateTokens.TextPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )

                                                Spacer(modifier = Modifier.height(6.dp))

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Text(
                                                        text = "${card.noteCount} notes",
                                                        fontFamily = ElmsSans,
                                                        fontSize = 12.sp,
                                                        color = ElectricSlateTokens.TextMuted
                                                    )
                                                    if (card.audioCount > 0) {
                                                        Text(
                                                            text = "• ${card.audioCount} audio",
                                                            fontFamily = ElmsSans,
                                                            fontSize = 12.sp,
                                                            color = ElectricSlateTokens.TextMuted
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(10.dp))

                                                Text(
                                                    text = "Updated ${card.lastModified}",
                                                    fontFamily = ElmsSans,
                                                    fontSize = 10.sp,
                                                    color = ElectricSlateTokens.TextMuted.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                    }
                                }
                                // Fill empty slots in the row if odd count
                                if (rowCards.size < gridColumns) {
                                    val emptySlots = gridColumns - rowCards.size
                                    repeat(emptySlots) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                // 6. AppListItem Preset (Recent Activity)
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "RECENT STUDY NOTES",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        color = ElectricSlateTokens.TextMuted,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                items(recentNotes.size) { index ->
                    val note = recentNotes[index]
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        AppListItem(
                            title = note.title,
                            subtitle = note.subtitle,
                            leadingIcon = note.icon,
                            action = if (index == 1) {
                                AppListItemAction.Toggle(
                                    checked = note.isPinned,
                                    onCheckedChange = { checked ->
                                        note.isPinned = checked
                                        recentNotes[index] = note.copy(isPinned = checked)
                                    }
                                )
                            } else if (index == 2) {
                                AppListItemAction.ShakingTrash(
                                    onDelete = {
                                        Toast.makeText(context, "Note deleted with shake animation", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            } else {
                                AppListItemAction.Chevron
                            },
                            onClick = {
                                Toast.makeText(context, "${note.title} tapped", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }

                // 7. Interactive Action Buttons Preview
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "PRESET BUTTON VARIANTS",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        color = ElectricSlateTokens.TextMuted,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppButton(
                            text = "New Canvas",
                            leadingIcon = Icons.Default.Brush,
                            onClick = {
                                Toast.makeText(context, "Primary Action: Canvas", Toast.LENGTH_SHORT).show()
                            },
                            variant = AppButtonVariant.PRIMARY,
                            modifier = Modifier.weight(1f)
                        )
                        AppButton(
                            text = "Import PDF",
                            leadingIcon = Icons.AutoMirrored.Filled.MenuBook,
                            onClick = {
                                Toast.makeText(context, "Secondary Action: PDF", Toast.LENGTH_SHORT).show()
                            },
                            variant = AppButtonVariant.SECONDARY,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Guaranteed bottom clearance spacer ensuring content scrolls above floating dock
                item {
                    BottomClearanceSpacer(100.dp)
                }
            }

        // 8. ConnectedInspector Preset (Popping out over active dock tool)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 88.dp)
        ) {
            ConnectedInspector(
                visible = isInspectorOpen,
                neckAnchorOffsetRatio = 0.72f, // Anchored to "Tools" button
                panelWidth = 300.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quick Creation Tools",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = ElectricSlateTokens.TextPrimary
                    )
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Inspector",
                        tint = ElectricSlateTokens.TextMuted,
                        modifier = Modifier
                            .size(20.dp)
                            .jigglePressClickable { isInspectorOpen = false }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppButton(
                        text = "Voice Memo",
                        leadingIcon = Icons.Default.Mic,
                        onClick = {
                            isInspectorOpen = false
                            Toast.makeText(context, "Recording Voice Memo", Toast.LENGTH_SHORT).show()
                        },
                        variant = AppButtonVariant.SECONDARY,
                        modifier = Modifier.weight(1f),
                        height = 40.dp
                    )
                    AppButton(
                        text = "Draw Note",
                        leadingIcon = Icons.Default.Brush,
                        onClick = {
                            isInspectorOpen = false
                            Toast.makeText(context, "Opening Drawing Canvas", Toast.LENGTH_SHORT).show()
                        },
                        variant = AppButtonVariant.PRIMARY,
                        modifier = Modifier.weight(1f),
                        height = 40.dp
                    )
                }
            }
        }

        // 9. AppFloatingBar Preset with AppFAB
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppFloatingBar {
                    AppFloatingBarItem(
                        icon = Icons.Default.Home,
                        contentDescription = "Home",
                        isSelected = activeDockTab == 0,
                        onClick = {
                            activeDockTab = 0
                            isInspectorOpen = false
                        }
                    )
                    AppFloatingBarItem(
                        icon = Icons.Default.Folder,
                        contentDescription = "Notebooks",
                        isSelected = activeDockTab == 1,
                        onClick = {
                            activeDockTab = 1
                            isInspectorOpen = false
                        }
                    )
                    AppFloatingBarItem(
                        icon = Icons.Default.Brush,
                        contentDescription = "Canvas",
                        isSelected = activeDockTab == 2,
                        onClick = {
                            activeDockTab = 2
                            isInspectorOpen = !isInspectorOpen
                        }
                    )
                    AppFloatingBarItem(
                        icon = Icons.Default.Build,
                        contentDescription = "Tools",
                        isSelected = activeDockTab == 3,
                        onClick = {
                            activeDockTab = 3
                            isInspectorOpen = !isInspectorOpen
                        }
                    )
                }

                // AppFAB
                AppFAB(
                    icon = Icons.Default.Add,
                    contentDescription = "Create New Note",
                    onClick = {
                        isInspectorOpen = !isInspectorOpen
                    }
                )
            }
        }

        // 10. AppBottomSheet Preset (Study Stopwatch & Laps)
        AppBottomSheet(
            visible = isTimerSheetOpen,
            onDismiss = { isTimerSheetOpen = false },
            title = "Study Session Stopwatch"
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = String.format("%02d:%02d", timerSeconds / 60, timerSeconds % 60),
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 40.sp,
                    color = ElectricSlateTokens.TextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppButton(
                        text = if (isTimerRunning) "Stop" else "Start Timer",
                        leadingIcon = if (isTimerRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        onClick = { isTimerRunning = !isTimerRunning },
                        variant = if (isTimerRunning) AppButtonVariant.DESTRUCTIVE else AppButtonVariant.PRIMARY,
                        modifier = Modifier.weight(1f)
                    )
                    AppButton(
                        text = "Lap",
                        onClick = {
                            lapTimes.add(0, String.format("%02d:%02d.%d", timerSeconds / 60, timerSeconds % 60, (0..9).random()))
                        },
                        variant = AppButtonVariant.SECONDARY,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "RECORDED LAPS",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = ElectricSlateTokens.TextMuted,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                lapTimes.take(3).forEachIndexed { idx, lap ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Lap ${lapTimes.size - idx}",
                            fontFamily = ElmsSans,
                            fontSize = 12.sp,
                            color = ElectricSlateTokens.TextMuted
                        )
                        Text(
                            text = lap,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = ElectricSlateTokens.TextPrimary
                        )
                    }
                }
            }
        }
    }
}
}

@Preview(
    name = "Home Refined Showcase Preview",
    showBackground = true,
    backgroundColor = 0xFF0D1117,
    device = "spec:width=412dp,height=915dp"
)
@Composable
fun HomeRefinedShowcaseScreenPreview() {
    HomeRefinedShowcaseScreen()
}
