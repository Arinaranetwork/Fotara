// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhotoLibrary
import com.arinara.fotara.ui.components.presets.AppButton
import com.arinara.fotara.ui.components.presets.AppButtonVariant
import com.arinara.fotara.ui.components.presets.AppCard
import com.arinara.fotara.ui.components.presets.AppScreen
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onFinishOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun checkAndReportPermissions() {
        val cam = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        val stor = ContextCompat.checkSelfPermission(context, storagePermission) == PackageManager.PERMISSION_GRANTED
        viewModel.updatePermissions(cameraGranted = cam, storageGranted = stor)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        checkAndReportPermissions()
        viewModel.proceedToStarterFolders()
    }

    LaunchedEffect(Unit) {
        checkAndReportPermissions()
    }

    AppScreen(
        modifier = modifier
    ) { innerPadding ->
        when (uiState.currentStep) {
            OnboardingStep.PERMISSIONS -> {
                PermissionsStepContent(
                    uiState = uiState,
                    innerPadding = innerPadding,
                    onRequestPermissions = {
                        val cam = uiState.isCameraPermissionGranted
                        val stor = uiState.isStoragePermissionGranted
                        if (cam && stor) {
                            viewModel.proceedToStarterFolders()
                        } else {
                            permissionLauncher.launch(requiredPermissions)
                        }
                    },
                    onSkip = {
                        viewModel.proceedToStarterFolders()
                    }
                )
            }
            OnboardingStep.STARTER_FOLDERS -> {
                StarterFoldersStepContent(
                    uiState = uiState,
                    innerPadding = innerPadding,
                    onCreateDefaultFolders = {
                        viewModel.createDefaultFolders(onFinishOnboarding)
                    },
                    onStartEmpty = {
                        viewModel.startEmpty(onFinishOnboarding)
                    }
                )
            }
        }
    }
}

@Composable
private fun PermissionsStepContent(
    uiState: OnboardingUiState,
    innerPadding: PaddingValues,
    onRequestPermissions: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = 24.dp,
                end = 24.dp,
                top = innerPadding.calculateTopPadding() + 32.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp
            )
    ) {
        Text(
            text = "Welcome to Fotara",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = ElectricSlateTokens.TextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Fotara organizes notes offline. The app requires local device permissions to photograph and save coursework without cloud uploads.",
            fontSize = 14.sp,
            color = ElectricSlateTokens.TextMuted,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Camera Permission Card
        PermissionExplanationCard(
            title = "Camera Access",
            explanation = "Required to photograph whiteboards, textbook chapters, and handwritten assignments directly into subject folders.",
            icon = Icons.Default.CameraAlt,
            isGranted = uiState.isCameraPermissionGranted
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Storage Permission Card
        PermissionExplanationCard(
            title = "Photos & Storage Access",
            explanation = "Required to store high-resolution notes locally, generate thumbnail cache, and import study photos from your gallery.",
            icon = Icons.Default.PhotoLibrary,
            isGranted = uiState.isStoragePermissionGranted
        )

        Spacer(modifier = Modifier.weight(1f))

        val allGranted = uiState.isCameraPermissionGranted && uiState.isStoragePermissionGranted

        AppButton(
            text = if (allGranted) "Continue to Setup" else "Grant Permissions",
            onClick = onRequestPermissions,
            variant = AppButtonVariant.PRIMARY,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        )

        if (!allGranted) {
            Spacer(modifier = Modifier.height(8.dp))
            AppButton(
                text = "Not Now",
                onClick = onSkip,
                variant = AppButtonVariant.OUTLINED,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )
        }
    }
}

@Composable
private fun StarterFoldersStepContent(
    uiState: OnboardingUiState,
    innerPadding: PaddingValues,
    onCreateDefaultFolders: () -> Unit,
    onStartEmpty: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = 24.dp,
                end = 24.dp,
                top = innerPadding.calculateTopPadding() + 32.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp
            )
    ) {
        Text(
            text = "Subject Folders",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = ElectricSlateTokens.TextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Start with common subjects or build your own?",
            fontSize = 14.sp,
            color = ElectricSlateTokens.TextMuted
        )

        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(uiState.starterFolders) { starter ->
                val color = try {
                    Color(android.graphics.Color.parseColor(starter.colorHex))
                } catch (_: Exception) {
                    ElectricSlateTokens.PrimaryAccent
                }

                AppCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(color, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = ElectricSlateTokens.PrimaryAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = starter.name,
                                color = ElectricSlateTokens.TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = starter.description,
                                color = ElectricSlateTokens.TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isCreatingFolders) {
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                CircularProgressIndicator(
                    color = ElectricSlateTokens.PrimaryAccent,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Creating folders...", color = ElectricSlateTokens.TextMuted, fontSize = 14.sp)
            }
        } else {
            AppButton(
                text = "Create Default Folders",
                onClick = onCreateDefaultFolders,
                variant = AppButtonVariant.PRIMARY,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            AppButton(
                text = "Start Empty",
                onClick = onStartEmpty,
                variant = AppButtonVariant.OUTLINED,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )
        }
    }
}

@Composable
private fun PermissionExplanationCard(
    title: String,
    explanation: String,
    icon: ImageVector,
    isGranted: Boolean
) {
    AppCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .background(ElectricSlateTokens.SurfaceElevated, RoundedCornerShape(12.dp))
                    .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ElectricSlateTokens.PrimaryAccent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        color = ElectricSlateTokens.TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (isGranted) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Granted",
                                tint = ElectricSlateTokens.PrimaryAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Granted", color = ElectricSlateTokens.PrimaryAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Text(text = "Required", color = ElectricSlateTokens.TextMuted, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = explanation,
                    color = ElectricSlateTokens.TextMuted,
                    fontSize = 14.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
