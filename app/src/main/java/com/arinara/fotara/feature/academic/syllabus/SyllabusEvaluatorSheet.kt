// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.academic.syllabus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.theme.HomeAddButtonBlue
import com.arinara.fotara.theme.HomeCardBorder
import com.arinara.fotara.theme.HomeCardSurface
import com.arinara.fotara.theme.HomeNearBlack
import com.arinara.fotara.theme.TextMuted
import com.arinara.fotara.theme.TextPrimary
import com.arinara.fotara.theme.TextSecondary
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyllabusEvaluatorSheet(
    initialComponents: List<SyllabusComponent>,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val evaluator = remember { SyllabusEvaluator() }

    var components by remember { mutableStateOf(initialComponents) }
    var targetPercent by remember { mutableFloatStateOf(90f) }

    val currentStanding = remember(components) {
        evaluator.calculateCurrentStandingAverage(components)
    }
    val currentEarned = remember(components) {
        evaluator.calculateTotalEarnedPoints(components)
    }
    val remainingWeight = remember(components) {
        evaluator.calculateRemainingWeight(components)
    }
    val targetProjection = remember(components, targetPercent) {
        evaluator.computeRequiredScoreForTarget(
            components = components,
            targetPercent = targetPercent
        )
    }
    val allProjections = remember(components) {
        evaluator.computeTargetProjections(components)
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = HomeNearBlack,
        tonalElevation = 0.dp,
        dragHandle = null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1B4FC4).copy(alpha = 0.35f))
                            .border(1.dp, HomeAddButtonBlue.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.School,
                            contentDescription = "Academic Syllabus",
                            tint = Color(0xFFEFE8DA),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Syllabus Grade Projection",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            lineHeight = 26.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Coursework & Final Exam Planning",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = TextSecondary
                        )
                    }
                }
                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Current Standing Summary Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(HomeCardSurface)
                    .border(1.dp, HomeCardBorder, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Current Standing",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format(Locale.US, "%.1f%%  (%s)", currentStanding, evaluator.getLetterGrade(currentStanding)),
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            lineHeight = 28.sp,
                            color = Color(0xFFEFE8DA)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = String.format(Locale.US, "Locked: %.1f pts", currentEarned),
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Light,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Locale.US, "Remaining: %.0f%% weight", remainingWeight),
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Light,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Target Grade Selector & Requirement Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF141B2A))
                    .border(1.dp, HomeCardBorder, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = String.format(Locale.US, "Target Grade: %s (%.0f%%)", targetProjection.letterGrade, targetPercent),
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            color = TextPrimary
                        )

                        // Achievability Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (targetProjection.isAchievable) Color(0xFF1E4A38)
                                    else Color(0xFF6B2A30)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (targetProjection.isAchievable) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = if (targetProjection.isAchievable) Color(0xFF4ADE80) else Color(0xFFF87171),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (targetProjection.isAchievable) "Attainable" else "Exceeds 100%",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                color = if (targetProjection.isAchievable) Color(0xFF4ADE80) else Color(0xFFF87171)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Slider(
                        value = targetPercent,
                        onValueChange = { targetPercent = (it / 5f).roundToInt() * 5f },
                        valueRange = 50f..100f,
                        steps = 9,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFEFE8DA),
                            activeTrackColor = HomeAddButtonBlue,
                            inactiveTrackColor = Color(0xFF1C2538)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Required Final Exam Score:",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = if (targetProjection.requiredFinalExamScore.isInfinite()) "Impossible"
                            else String.format(Locale.US, "%.1f / 100", targetProjection.requiredFinalExamScore),
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            lineHeight = 22.sp,
                            color = if (targetProjection.isAchievable) Color(0xFFEFE8DA) else Color(0xFFF87171)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Projection Matrix Summary Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (proj in allProjections) {
                    val isSelected = proj.letterGrade == targetProjection.letterGrade
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) HomeAddButtonBlue.copy(alpha = 0.25f)
                                else HomeCardSurface
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) HomeAddButtonBlue else HomeCardBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = proj.letterGrade,
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                lineHeight = 18.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (proj.isAchievable) String.format(Locale.US, "%.0f%%", proj.requiredFinalExamScore)
                                else "N/A",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                color = if (proj.isAchievable) TextSecondary else Color(0xFFF87171)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Course Components",
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Component List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(components, key = { it.id }) { component ->
                    SyllabusComponentRow(
                        component = component,
                        onScoreChanged = { newScore ->
                            components = components.map { c ->
                                if (c.id == component.id) c.copy(currentScore = newScore)
                                else c
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onDismissRequest,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HomeAddButtonBlue,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Assessment,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save Projections",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

@Composable
private fun SyllabusComponentRow(
    component: SyllabusComponent,
    onScoreChanged: (Float?) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(HomeCardSurface)
            .border(1.dp, HomeCardBorder, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = component.name,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = String.format(Locale.US, "Weight: %.0f%% of total course grade", component.weightPercent),
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Light,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = TextMuted
                )
            }

            // Score Badge / Toggle
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF141B2A))
                    .border(1.dp, HomeCardBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (component.currentScore != null) {
                        String.format(Locale.US, "%.1f / %.0f", component.currentScore, component.maxScore)
                    } else {
                        "Pending Final"
                    },
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = if (component.currentScore != null) Color(0xFFEFE8DA) else TextSecondary
                )
            }
        }
    }
}
