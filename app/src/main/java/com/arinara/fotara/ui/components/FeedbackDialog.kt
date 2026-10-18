// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.online.FeedbackCategory
import com.arinara.fotara.online.FeedbackManager
import com.arinara.fotara.ui.components.presets.AppButton
import com.arinara.fotara.ui.components.presets.AppButtonVariant
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FeedbackDialog(
    feedbackManager: FeedbackManager,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var category by remember { mutableStateOf(FeedbackCategory.BUG_REPORT) }
    var content by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var includeDiagnostics by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var technicalDetails by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }
    var showTechnicalDetails by remember { mutableStateOf(false) }

    var cooldownRemaining by remember {
        mutableIntStateOf(feedbackManager.getCooldownRemainingSeconds())
    }
    var remainingQuota by remember {
        mutableIntStateOf(feedbackManager.getRemainingDailyQuota())
    }
    var nextSlotMs by remember {
        mutableLongStateOf(feedbackManager.getTimeUntilNextSlotMs())
    }

    // Cooldown countdown timer loop (1 second updates)
    LaunchedEffect(Unit) {
        feedbackManager.flushLocalQueue()
        while (true) {
            cooldownRemaining = feedbackManager.getCooldownRemainingSeconds()
            remainingQuota = feedbackManager.getRemainingDailyQuota()
            nextSlotMs = feedbackManager.getTimeUntilNextSlotMs()
            delay(1000L)
        }
    }

    val formatNextSlotText: (Long) -> String = { ms ->
        val hours = ms / (60 * 60 * 1000L)
        val minutes = (ms % (60 * 60 * 1000L)) / (60 * 1000L)
        if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Column {
                Text(
                    text = "Send Suggestion & Feedback",
                    fontWeight = FontWeight.Bold,
                    color = ElectricSlateTokens.TextPrimary,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (remainingQuota > 0) {
                    Text(
                        text = "Daily quota: $remainingQuota / ${FeedbackManager.FEEDBACK_DAILY_LIMIT} submissions remaining",
                        color = ElectricSlateTokens.TextMuted,
                        fontSize = 12.sp
                    )
                } else {
                    Text(
                        text = "Daily limit reached (0 / ${FeedbackManager.FEEDBACK_DAILY_LIMIT}). Next slot opens in ${formatNextSlotText(nextSlotMs)}.",
                        color = ElectricSlateTokens.Destructive,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Result / Status Banner
                if (resultMessage != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = resultMessage!!,
                            color = if (isSuccess) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.Destructive,
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Medium
                        )

                        // Collapsible technical details toggle
                        if (!technicalDetails.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { showTechnicalDetails = !showTechnicalDetails }
                                    .padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (showTechnicalDetails) "Hide technical details" else "Show technical details",
                                    color = ElectricSlateTokens.TextMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (showTechnicalDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = ElectricSlateTokens.TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            if (showTechnicalDetails) {
                                Surface(
                                    color = ElectricSlateTokens.BgCanvas,
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp)
                                ) {
                                    Text(
                                        text = technicalDetails!!,
                                        color = ElectricSlateTokens.TextMuted,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (!isSuccess) {
                    // Category Selection Chips (Row 1: Bug Report & Suggestion)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = category == FeedbackCategory.BUG_REPORT,
                            onClick = {
                                category = FeedbackCategory.BUG_REPORT
                                includeDiagnostics = true
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.BugReport,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("Bug Report", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricSlateTokens.Destructive,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White,
                                containerColor = ElectricSlateTokens.SurfaceElevated,
                                labelColor = ElectricSlateTokens.TextPrimary,
                                iconColor = ElectricSlateTokens.TextMuted
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = category == FeedbackCategory.SUGGESTION,
                            onClick = { category = FeedbackCategory.SUGGESTION },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("Suggestion", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricSlateTokens.PrimaryAccent,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White,
                                containerColor = ElectricSlateTokens.SurfaceElevated,
                                labelColor = ElectricSlateTokens.TextPrimary,
                                iconColor = ElectricSlateTokens.TextMuted
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Category Selection Chips (Row 2: Feature Idea & General)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = category == FeedbackCategory.FEATURE_IDEA,
                            onClick = { category = FeedbackCategory.FEATURE_IDEA },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("Feature Idea", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricSlateTokens.PrimaryAccent,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White,
                                containerColor = ElectricSlateTokens.SurfaceElevated,
                                labelColor = ElectricSlateTokens.TextPrimary,
                                iconColor = ElectricSlateTokens.TextMuted
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = category == FeedbackCategory.GENERAL,
                            onClick = { category = FeedbackCategory.GENERAL },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("General", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricSlateTokens.PrimaryAccent,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White,
                                containerColor = ElectricSlateTokens.SurfaceElevated,
                                labelColor = ElectricSlateTokens.TextPrimary,
                                iconColor = ElectricSlateTokens.TextMuted
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Feedback Content Field
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Describe your suggestion or issue...", color = ElectricSlateTokens.TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ElectricSlateTokens.TextPrimary,
                            unfocusedTextColor = ElectricSlateTokens.TextPrimary,
                            focusedBorderColor = ElectricSlateTokens.PrimaryAccent,
                            unfocusedBorderColor = ElectricSlateTokens.BorderSubtle,
                            cursorColor = ElectricSlateTokens.PrimaryAccent,
                            focusedContainerColor = ElectricSlateTokens.BgCanvas,
                            unfocusedContainerColor = ElectricSlateTokens.BgCanvas
                        ),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Optional Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Contact email (optional for reply)", color = ElectricSlateTokens.TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ElectricSlateTokens.TextPrimary,
                            unfocusedTextColor = ElectricSlateTokens.TextPrimary,
                            focusedBorderColor = ElectricSlateTokens.PrimaryAccent,
                            unfocusedBorderColor = ElectricSlateTokens.BorderSubtle,
                            cursorColor = ElectricSlateTokens.PrimaryAccent,
                            focusedContainerColor = ElectricSlateTokens.BgCanvas,
                            unfocusedContainerColor = ElectricSlateTokens.BgCanvas
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Optional Diagnostics Checkbox
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = includeDiagnostics,
                            onCheckedChange = { includeDiagnostics = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = ElectricSlateTokens.PrimaryAccent,
                                uncheckedColor = ElectricSlateTokens.TextMuted
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Include device diagnostics (device model & Android version)",
                            color = ElectricSlateTokens.TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (isSuccess) {
                AppButton(
                    text = "Close",
                    onClick = onDismiss,
                    variant = AppButtonVariant.PRIMARY
                )
            } else {
                val isButtonEnabled = !isSubmitting && content.isNotBlank() && cooldownRemaining == 0 && remainingQuota > 0
                val buttonText = when {
                    isSubmitting -> "Submitting..."
                    cooldownRemaining > 0 -> "Wait (${cooldownRemaining}s)"
                    remainingQuota <= 0 -> "Daily Limit Reached"
                    else -> "Submit Feedback"
                }
                AppButton(
                    text = buttonText,
                    onClick = {
                        scope.launch {
                            isSubmitting = true
                            val res = feedbackManager.submitFeedback(
                                category = category,
                                content = content,
                                email = email.ifBlank { null },
                                includeDiagnostics = includeDiagnostics
                            )
                            isSubmitting = false
                            resultMessage = res.message
                            technicalDetails = res.technicalDetails
                            isSuccess = res.success
                            cooldownRemaining = feedbackManager.getCooldownRemainingSeconds()
                            remainingQuota = feedbackManager.getRemainingDailyQuota()
                            nextSlotMs = feedbackManager.getTimeUntilNextSlotMs()
                        }
                    },
                    enabled = isButtonEnabled,
                    variant = AppButtonVariant.PRIMARY
                )
            }
        },
        dismissButton = {
            if (!isSuccess) {
                AppButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    enabled = !isSubmitting,
                    variant = AppButtonVariant.SECONDARY
                )
            }
        },
        containerColor = ElectricSlateTokens.SurfacePanel,
        shape = RoundedCornerShape(16.dp)
    )
}
