// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.academic.scores

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import com.arinara.fotara.theme.FotaraShapes
import com.arinara.fotara.theme.TagAmber
import com.arinara.fotara.theme.TagEmerald
import com.arinara.fotara.data.model.AcademicScore
import com.arinara.fotara.data.model.ScoreCalculationMode
import com.arinara.fotara.data.repository.ScoreRepository
import com.arinara.fotara.ui.components.presets.AppScreen
import com.arinara.fotara.ui.components.presets.AppTopBar
import com.arinara.fotara.ui.components.presets.AppCard
import com.arinara.fotara.ui.components.presets.AppPillChip
import com.arinara.fotara.ui.components.presets.AppButton
import com.arinara.fotara.ui.components.presets.AppButtonVariant
import com.arinara.fotara.ui.components.presets.AppListItem
import com.arinara.fotara.ui.components.presets.AppListItemAction
import com.arinara.fotara.ui.components.presets.BottomClearanceSpacer
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import com.arinara.fotara.ui.theme.jigglePressClickable
import com.arinara.fotara.theme.ElmsSans
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoresTrackerScreen(
    scoreRepository: ScoreRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allScores by scoreRepository.observeScores().collectAsState(initial = emptyList())

    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var calculationMode by remember { mutableStateOf(ScoreCalculationMode.CUSTOM_WEIGHTS) }
    var targetGpaPercentage by remember { mutableDoubleStateOf(85.0) } // Default 85% (~3.7 GPA)

    var showAddDialog by remember { mutableStateOf(false) }
    var editingScore by remember { mutableStateOf<AcademicScore?>(null) }
    var showTargetGpaDialog by remember { mutableStateOf(false) }
    var showPresetDialog by remember { mutableStateOf(false) }

    val filteredScores = remember(allScores, selectedCategoryFilter) {
        if (selectedCategoryFilter == null) allScores
        else allScores.filter { it.category.equals(selectedCategoryFilter, ignoreCase = true) }
    }

    // Calculations
    val pureAverage = remember(allScores) {
        if (allScores.isEmpty()) 0.0
        else allScores.map { it.percentage }.average()
    }

    val totalWeight = remember(allScores) {
        allScores.sumOf { it.weight }
    }

    val weightedScore = remember(allScores, totalWeight) {
        if (allScores.isEmpty() || totalWeight <= 0.0) 0.0
        else allScores.sumOf { (it.percentage * it.weight) } / totalWeight
    }

    val currentScore = if (calculationMode == ScoreCalculationMode.PURE_AVERAGE) pureAverage else weightedScore

    // Projection calculation: Remaining required score on final exam
    val finalExamScore = remember(allScores) {
        allScores.firstOrNull { it.category.equals("Final Exam", ignoreCase = true) }
    }

    val finalExamWeight = finalExamScore?.weight ?: 0.0
    val remainingNonFinalWeight = totalWeight - finalExamWeight

    val projectedFinalExamNeeded: Double? = remember(allScores, targetGpaPercentage, calculationMode, totalWeight) {
        if (calculationMode == ScoreCalculationMode.CUSTOM_WEIGHTS && finalExamWeight > 0.0 && totalWeight >= 99.0) {
            val nonFinalWeightedSum = allScores.filter { !it.category.equals("Final Exam", ignoreCase = true) }
                .sumOf { it.percentage * it.weight }
            val needed = (targetGpaPercentage * 100.0 - nonFinalWeightedSum) / finalExamWeight
            needed.coerceIn(0.0, 100.0)
        } else null
    }

    // Countdown calculation for upcoming final exam
    val daysUntilFinalExam: Long? = remember(allScores) {
        val exam = allScores.firstOrNull { it.examDateMillis != null && it.examDateMillis!! > System.currentTimeMillis() }
        exam?.examDateMillis?.let {
            val diff = it - System.currentTimeMillis()
            TimeUnit.MILLISECONDS.toDays(diff).coerceAtLeast(0)
        }
    }

    AppScreen(
        modifier = modifier,
        containerColor = ElectricSlateTokens.BgCanvas
    ) {
        // Top App Bar
        AppTopBar(
            title = "Scores",
            onBackClick = onBack,
            containerColor = ElectricSlateTokens.BgCanvas,
            actions = {
                OutlinedButton(
                    onClick = { showTargetGpaDialog = true },
                    border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricSlateTokens.PrimaryAccent),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(
                        text = "Target: ${"%.1f".format(Locale.US, targetGpaPercentage)}%",
                        fontSize = 12.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { showPresetDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.IosShare,
                        contentDescription = "Presets",
                        tint = ElectricSlateTokens.TextPrimary
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Metrics and Mode Card
            item {
                AppCard(
                    cornerRadius = 24.dp,
                    containerColor = ElectricSlateTokens.SurfacePanel,
                    borderColor = ElectricSlateTokens.BorderSubtle,
                    isGlow = true,
                    glowColor = ElectricSlateTokens.PrimaryAccent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (calculationMode == ScoreCalculationMode.PURE_AVERAGE) "Pure Average" else "Weighted Average",
                                color = ElectricSlateTokens.TextMuted,
                                fontSize = 12.sp,
                                fontFamily = ElmsSans
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${"%.1f".format(Locale.US, currentScore)}%",
                                color = ElectricSlateTokens.TextPrimary,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = ElmsSans
                            )
                        }

                        // Mode toggle button
                        OutlinedButton(
                            onClick = {
                                calculationMode = if (calculationMode == ScoreCalculationMode.PURE_AVERAGE) {
                                    ScoreCalculationMode.CUSTOM_WEIGHTS
                                } else {
                                    ScoreCalculationMode.PURE_AVERAGE
                                }
                            },
                            border = BorderStroke(1.dp, ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Scale,
                                contentDescription = null,
                                tint = ElectricSlateTokens.PrimaryAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (calculationMode == ScoreCalculationMode.PURE_AVERAGE) "Average" else "Weighted",
                                color = ElectricSlateTokens.PrimaryAccent,
                                fontSize = 12.sp,
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Weight balance visual indicator in Weighted Mode
                    if (calculationMode == ScoreCalculationMode.CUSTOM_WEIGHTS) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val isBalanced = totalWeight in 99.9..100.1
                        Surface(
                            color = if (isBalanced) TagEmerald.copy(alpha = 0.15f) else TagAmber.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isBalanced) TagEmerald else TagAmber.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isBalanced) Icons.Default.Check else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (isBalanced) TagEmerald else TagAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isBalanced) {
                                        "Weights Total: 100% (Balanced)"
                                    } else {
                                        "Weights Total: ${"%.1f".format(Locale.US, totalWeight)}% (Target: 100%)"
                                    },
                                    color = if (isBalanced) TagEmerald else TagAmber,
                                    fontSize = 12.sp,
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Projection & Countdown Banner
                    if (projectedFinalExamNeeded != null || daysUntilFinalExam != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                if (projectedFinalExamNeeded != null) {
                                    Text(
                                        text = "Needed on Final Exam: ${"%.1f".format(Locale.US, projectedFinalExamNeeded)}% to hit target (${"%.1f".format(Locale.US, targetGpaPercentage)}%)",
                                        color = ElectricSlateTokens.PrimaryAccent,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = ElmsSans
                                    )
                                }
                                if (daysUntilFinalExam != null) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Upcoming Exam in $daysUntilFinalExam day(s)",
                                        color = ElectricSlateTokens.TextMuted,
                                        fontSize = 11.5.sp,
                                        fontFamily = ElmsSans
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Category Chips Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppPillChip(
                        label = "All (${allScores.size})",
                        isSelected = selectedCategoryFilter == null,
                        onClick = { selectedCategoryFilter = null }
                    )

                    AcademicScore.CATEGORIES.forEach { category ->
                        val count = allScores.count { it.category.equals(category, ignoreCase = true) }
                        AppPillChip(
                            label = "$category ($count)",
                            isSelected = selectedCategoryFilter == category,
                            onClick = { selectedCategoryFilter = if (selectedCategoryFilter == category) null else category }
                        )
                    }
                }
            }

            // Add Score Button
            item {
                AppButton(
                    text = "Add Score Entry",
                    leadingIcon = Icons.Default.Add,
                    variant = AppButtonVariant.PRIMARY,
                    onClick = {
                        editingScore = null
                        showAddDialog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Empty state or Scores List
            if (filteredScores.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Scale,
                                contentDescription = null,
                                tint = ElectricSlateTokens.TextMuted.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No academic scores recorded yet",
                                color = ElectricSlateTokens.TextMuted,
                                fontSize = 14.sp,
                                fontFamily = ElmsSans
                            )
                        }
                    }
                }
            } else {
                items(filteredScores, key = { it.id }) { score ->
                    ScoreCard(
                        score = score,
                        onEdit = {
                            editingScore = score
                            showAddDialog = true
                        },
                        onDelete = {
                            coroutineScope.launch {
                                scoreRepository.deleteScore(score.id)
                            }
                        },
                        onOpenEvidence = { uriString ->
                            try {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse(uriString)
                                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot open evidence file: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            item {
                BottomClearanceSpacer(100.dp)
            }
        }
    }

    // Add / Edit Score Dialog
    if (showAddDialog) {
        ScoreEditDialog(
            initialScore = editingScore,
            onDismiss = { showAddDialog = false },
            onSave = { updated ->
                coroutineScope.launch {
                    if (updated.id > 0) {
                        scoreRepository.updateScore(updated)
                    } else {
                        scoreRepository.addScore(updated)
                    }
                    showAddDialog = false
                }
            }
        )
    }

    // Target GPA Setting Dialog
    if (showTargetGpaDialog) {
        TargetGpaDialog(
            currentPercentage = targetGpaPercentage,
            onDismiss = { showTargetGpaDialog = false },
            onSave = { newPercentage ->
                targetGpaPercentage = newPercentage
                showTargetGpaDialog = false
            }
        )
    }

    // Presets Export / Import Dialog
    if (showPresetDialog) {
        SchemePresetDialog(
            allScores = allScores,
            onDismiss = { showPresetDialog = false },
            onImport = { importedScores ->
                coroutineScope.launch {
                    scoreRepository.importPresetScores(importedScores)
                    showPresetDialog = false
                }
            }
        )
    }
}

@Composable
private fun ScoreCard(
    score: AcademicScore,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenEvidence: (String) -> Unit
) {
    AppCard(
        cornerRadius = 24.dp,
        containerColor = ElectricSlateTokens.SurfacePanel,
        borderColor = ElectricSlateTokens.BorderSubtle,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = score.category,
                        color = ElectricSlateTokens.PrimaryAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = score.subject,
                    color = ElectricSlateTokens.TextMuted,
                    fontSize = 12.sp,
                    fontFamily = ElmsSans
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = ElectricSlateTokens.TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = ElectricSlateTokens.Destructive,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = score.title,
                    color = ElectricSlateTokens.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = ElmsSans,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Weight: ${"%.1f".format(Locale.US, score.weight)}%",
                    color = ElectricSlateTokens.TextMuted,
                    fontSize = 12.sp,
                    fontFamily = ElmsSans
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${"%.1f".format(Locale.US, score.percentage)}%",
                    color = if (score.percentage >= 80.0) TagEmerald else if (score.percentage >= 60.0) TagAmber else ElectricSlateTokens.Destructive,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ElmsSans
                )
                Text(
                    text = "${score.score} / ${score.maxScore}",
                    color = ElectricSlateTokens.TextMuted,
                    fontSize = 11.5.sp,
                    fontFamily = ElmsSans
                )
            }
        }

        // Evidence link pill if attached
        if (!score.evidenceUri.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                color = ElectricSlateTokens.SurfaceElevated,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
                modifier = Modifier.clickable { onOpenEvidence(score.evidenceUri) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = null,
                        tint = ElectricSlateTokens.PrimaryAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = score.evidenceName ?: "Evidence Attachment",
                        color = ElectricSlateTokens.TextPrimary,
                        fontSize = 11.sp,
                        fontFamily = ElmsSans,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        tint = ElectricSlateTokens.TextMuted,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ScoreEditDialog(
    initialScore: AcademicScore?,
    onDismiss: () -> Unit,
    onSave: (AcademicScore) -> Unit
) {
    val context = LocalContext.current
    var subject by remember { mutableStateOf(initialScore?.subject ?: "") }
    var title by remember { mutableStateOf(initialScore?.title ?: "") }
    var category by remember { mutableStateOf(initialScore?.category ?: AcademicScore.CATEGORIES.first()) }
    var scoreStr by remember { mutableStateOf(initialScore?.score?.toString() ?: "") }
    var maxScoreStr by remember { mutableStateOf(initialScore?.maxScore?.toString() ?: "100.0") }
    var weightStr by remember { mutableStateOf(initialScore?.weight?.toString() ?: "0.0") }
    var evidenceUri by remember { mutableStateOf(initialScore?.evidenceUri) }
    var evidenceName by remember { mutableStateOf(initialScore?.evidenceName) }
    var examDaysStr by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            evidenceUri = uri.toString()
            evidenceName = uri.lastPathSegment ?: "Attached File"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Text(
                text = if (initialScore == null) "Add Score" else "Edit Score",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ElmsSans
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Course / Subject", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    shape = FotaraShapes.InputField,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Assessment Title", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    shape = FotaraShapes.InputField,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Chips Selector
                Text("Category", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontFamily = ElmsSans)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AcademicScore.CATEGORIES.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            shape = FotaraShapes.Pill,
                            label = { Text(cat, fontSize = 11.sp, fontFamily = ElmsSans) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = scoreStr,
                        onValueChange = { scoreStr = it },
                        label = { Text("Score", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        singleLine = true,
                        shape = FotaraShapes.InputField,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxScoreStr,
                        onValueChange = { maxScoreStr = it },
                        label = { Text("Max", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        singleLine = true,
                        shape = FotaraShapes.InputField,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = weightStr,
                        onValueChange = { weightStr = it },
                        label = { Text("Weight %", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        singleLine = true,
                        shape = FotaraShapes.InputField,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Evidence Attachment
                OutlinedButton(
                    onClick = { filePickerLauncher.launch("*/*") },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    shape = FotaraShapes.InputField,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.AttachFile, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (evidenceUri != null) (evidenceName ?: "Evidence Attached") else "Attach Evidence (Photo / PDF)",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 12.sp,
                        fontFamily = ElmsSans,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sVal = scoreStr.toDoubleOrNull() ?: 0.0
                    val mVal = maxScoreStr.toDoubleOrNull() ?: 100.0
                    val wVal = weightStr.toDoubleOrNull() ?: 0.0
                    val examDate = examDaysStr.toLongOrNull()?.let {
                        System.currentTimeMillis() + TimeUnit.DAYS.toMillis(it)
                    } ?: initialScore?.examDateMillis

                    val updated = AcademicScore(
                        id = initialScore?.id ?: 0L,
                        subject = subject.ifBlank { "General" },
                        title = title.ifBlank { "Assessment" },
                        category = category,
                        score = sVal,
                        maxScore = mVal,
                        weight = wVal,
                        evidenceUri = evidenceUri,
                        evidenceName = evidenceName,
                        examDateMillis = examDate
                    )
                    onSave(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                shape = FotaraShapes.Button
            ) {
                Text("Save", color = MaterialTheme.colorScheme.onPrimary, fontFamily = ElmsSans, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = ElmsSans)
            }
        },
        shape = FotaraShapes.Dialog
    )
}

@Composable
private fun TargetGpaDialog(
    currentPercentage: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var textVal by remember { mutableStateOf(currentPercentage.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Text("Set Target Academic Goal", color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = ElmsSans)
        },
        text = {
            Column {
                Text(
                    text = "Specify your target score percentage to calculate required scores on remaining exams.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontFamily = ElmsSans
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = textVal,
                    onValueChange = { textVal = it },
                    label = { Text("Target Percentage (e.g. 85.0%)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    shape = FotaraShapes.InputField,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = textVal.toDoubleOrNull() ?: 85.0
                    onSave(p)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                shape = FotaraShapes.Button
            ) {
                Text("Apply", color = MaterialTheme.colorScheme.onPrimary, fontFamily = ElmsSans, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = ElmsSans)
            }
        },
        shape = FotaraShapes.Dialog
    )
}

@Composable
private fun SchemePresetDialog(
    allScores: List<AcademicScore>,
    onDismiss: () -> Unit,
    onImport: (List<AcademicScore>) -> Unit
) {
    val context = LocalContext.current
    var importJsonText by remember { mutableStateOf("") }
    var isImportMode by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Text("Scheme Preset Sharing", color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = ElmsSans)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!isImportMode) {
                    Text(
                        text = "Export your grading scheme and assessment weights to share with classmates.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontFamily = ElmsSans
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            val jsonArray = JSONArray()
                            for (score in allScores) {
                                val obj = JSONObject().apply {
                                    put("subject", score.subject)
                                    put("title", score.title)
                                    put("category", score.category)
                                    put("score", score.score)
                                    put("maxScore", score.maxScore)
                                    put("weight", score.weight)
                                }
                                jsonArray.put(obj)
                            }
                            val jsonString = jsonArray.toString(2)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Fotara Scheme Preset", jsonString))
                            Toast.makeText(context, "Scheme preset copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                        shape = FotaraShapes.Button,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Copy Scheme to Clipboard", color = MaterialTheme.colorScheme.onPrimary, fontFamily = ElmsSans, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { isImportMode = true },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        shape = FotaraShapes.Button,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Import Preset from Classmate", color = MaterialTheme.colorScheme.onBackground, fontFamily = ElmsSans)
                    }
                } else {
                    Text(
                        text = "Paste JSON scheme preset below to load weights and assessment structure:",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontFamily = ElmsSans
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        placeholder = { Text("[{\"subject\":\"Math\", ...}]", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                        shape = FotaraShapes.InputField,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            try {
                                val array = JSONArray(importJsonText)
                                val list = mutableListOf<AcademicScore>()
                                for (i in 0 until array.length()) {
                                    val obj = array.getJSONObject(i)
                                    list.add(
                                        AcademicScore(
                                            subject = obj.optString("subject", "General"),
                                            title = obj.optString("title", "Assessment"),
                                            category = obj.optString("category", "Assignment"),
                                            score = obj.optDouble("score", 0.0),
                                            maxScore = obj.optDouble("maxScore", 100.0),
                                            weight = obj.optDouble("weight", 0.0)
                                        )
                                    )
                                }
                                onImport(list)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Invalid JSON preset format: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TagEmerald, contentColor = Color.White),
                        shape = FotaraShapes.Button,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Load Imported Preset", color = Color.White, fontFamily = ElmsSans, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = ElmsSans)
            }
        },
        shape = FotaraShapes.Dialog
    )
}
