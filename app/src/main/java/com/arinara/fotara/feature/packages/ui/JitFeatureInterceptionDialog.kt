// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.packages.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arinara.fotara.feature.packages.loader.FotaraPackageManager
import com.arinara.fotara.feature.packages.model.FpkgManifest
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.theme.HomeAddButtonBlue
import com.arinara.fotara.theme.HomeCardBorder
import com.arinara.fotara.theme.HomeCardSurface
import com.arinara.fotara.theme.HomeSubtitleGray
import com.arinara.fotara.theme.TagCrimson
import com.arinara.fotara.theme.TextPrimary
import com.arinara.fotara.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Interactive Just-In-Time (JIT) interception dialog.
 * Displayed when an uninstalled modular feature (e.g., Live Collaborative Canvas, Mathematical OCR,
 * or Exam Analytics) is tapped within the application workflow.
 * Shows package name, on-demand download size, simulated progress animation with signature verification,
 * and immediately triggers post-install execution without requiring an application restart.
 * Formatted with 20dp dialog corner radius per Android.md shape tokens.
 */
@Composable
fun JitFeatureInterceptionDialog(
    manifest: FpkgManifest,
    onDismissRequest: () -> Unit,
    onLaunch: (FpkgManifest) -> Unit,
    packageManager: FotaraPackageManager? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var isInstalling by remember { mutableStateOf(false) }
    var installStage by remember { mutableStateOf("Ready to install") }
    var progressFraction by remember { mutableFloatStateOf(0f) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun startInstallationFlow() {
        if (isInstalling) return
        isInstalling = true
        errorMessage = null

        coroutineScope.launch {
            // Stage 1: Initiating transfer
            installStage = "Downloading package payload..."
            progressFraction = 0.25f
            delay(300)

            // Stage 2: Verifying cryptographic integrity
            installStage = "Verifying SHA-256 signature..."
            progressFraction = 0.65f
            delay(350)

            // Stage 3: Dynamic registration
            installStage = "Mounting modular add-on..."
            progressFraction = 0.90f

            val installResult = if (packageManager != null) {
                packageManager.installPackage(manifest.packageId)
            } else {
                Result.success(manifest.copy(isInstalled = true, isEnabled = true))
            }

            delay(200)
            progressFraction = 1.0f

            installResult.onSuccess { installedManifest ->
                installStage = "Launching feature..."
                delay(150)
                onLaunch(installedManifest)
            }.onFailure { err ->
                isInstalling = false
                errorMessage = err.message ?: "Installation aborted by security verifier."
            }
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isInstalling) {
                onDismissRequest()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isInstalling,
            dismissOnClickOutside = !isInstalling
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(HomeCardSurface)
                .border(1.dp, HomeCardBorder, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF172036)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = resolveDialogIcon(manifest.iconKey),
                                contentDescription = "Feature Icon",
                                tint = HomeAddButtonBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = "Modular Add-On Required",
                            color = TextPrimary,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            lineHeight = 22.sp
                        )
                    }

                    if (!isInstalling) {
                        IconButton(
                            onClick = onDismissRequest,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Dialog",
                                tint = HomeSubtitleGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Feature Details Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0D121F))
                        .border(1.dp, HomeCardBorder, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = manifest.name,
                                color = TextPrimary,
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF1A2644))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = manifest.formattedSize(),
                                    color = Color(0xFF93C5FD),
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = manifest.description,
                            color = TextSecondary,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Light,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Preserves <25MB Base APK footprint by fetching specialized code and models on demand.",
                            color = HomeSubtitleGray,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Normal,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                // Error Message
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = TagCrimson,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Installation Progress / Actions
                if (isInstalling) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = installStage,
                                color = TextPrimary,
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "${(progressFraction * 100).toInt()}%",
                                color = HomeAddButtonBlue,
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = HomeAddButtonBlue,
                            trackColor = Color(0xFF172036)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismissRequest,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, HomeCardBorder)
                        ) {
                            Text(
                                text = "Cancel",
                                color = HomeSubtitleGray,
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }

                        Button(
                            onClick = { startInstallationFlow() },
                            modifier = Modifier
                                .weight(1.5f)
                                .height(44.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HomeAddButtonBlue)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Download & Launch",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun resolveDialogIcon(iconKey: String): ImageVector {
    return when (iconKey) {
        "collab" -> Icons.Default.ViewInAr
        "whiteboard_ocr" -> Icons.Default.Memory
        "exam_analytics" -> Icons.Default.Timeline
        else -> Icons.Default.Extension
    }
}
