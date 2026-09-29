// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.online.FeedbackCategory
import com.arinara.fotara.online.FeedbackManager
import kotlinx.coroutines.launch

private val CardBg = Color(0xFF141936)
private val TabCream = Color(0xFFEAE3D2)
private val AccentGold = Color(0xFFF77F00)

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
    var isSuccess by remember { mutableStateOf(false) }

    // Flush any pending queue items when dialog opens
    LaunchedEffect(Unit) {
        feedbackManager.flushLocalQueue()
    }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Text(
                text = "Send Suggestion & Feedback",
                fontWeight = FontWeight.Bold,
                color = TabCream,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (resultMessage != null) {
                    Text(
                        text = resultMessage!!,
                        color = if (isSuccess) Color(0xFF2A9D8F) else Color(0xFFE63946),
                        fontSize = 13.5.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
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
                            label = { Text("🐞 Bug Report", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFE63946),
                                selectedLabelColor = Color.White,
                                containerColor = Color.Transparent,
                                labelColor = TabCream
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = category == FeedbackCategory.SUGGESTION,
                            onClick = { category = FeedbackCategory.SUGGESTION },
                            label = { Text("💬 Suggestion", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF2A9D8F),
                                selectedLabelColor = Color.White,
                                containerColor = Color.Transparent,
                                labelColor = TabCream
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
                            label = { Text("💡 Feature Idea", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentGold,
                                selectedLabelColor = Color.Black,
                                containerColor = Color.Transparent,
                                labelColor = TabCream
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = category == FeedbackCategory.GENERAL,
                            onClick = { category = FeedbackCategory.GENERAL },
                            label = { Text("📝 General", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF3A86FF),
                                selectedLabelColor = Color.White,
                                containerColor = Color.Transparent,
                                labelColor = TabCream
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Feedback Content Field
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Describe your suggestion or issue...", color = TabCream.copy(alpha = 0.6f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TabCream,
                            unfocusedTextColor = TabCream,
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = TabCream.copy(alpha = 0.3f),
                            cursorColor = AccentGold
                        ),
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Optional Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email kontak (opsional untuk balasan)", color = TabCream.copy(alpha = 0.6f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TabCream,
                            unfocusedTextColor = TabCream,
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = TabCream.copy(alpha = 0.3f),
                            cursorColor = AccentGold
                        ),
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
                                checkedColor = AccentGold,
                                uncheckedColor = TabCream.copy(alpha = 0.5f)
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Sertakan info diagnostik (tipe HP & Android)",
                            color = TabCream.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (isSuccess) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold)
                ) {
                    Text("Tutup", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
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
                            isSuccess = res.success
                        }
                    },
                    enabled = !isSubmitting && content.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mengirim...", color = Color.Black, fontWeight = FontWeight.Bold)
                    } else {
                        Text("Kirim Masukan", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        dismissButton = {
            if (!isSuccess) {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isSubmitting
                ) {
                    Text("Batal", color = TabCream)
                }
            }
        },
        containerColor = CardBg,
        shape = RoundedCornerShape(16.dp)
    )
}
