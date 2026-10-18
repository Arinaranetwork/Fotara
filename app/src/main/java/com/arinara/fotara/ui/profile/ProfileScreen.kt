// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.profile

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalConfiguration
import com.arinara.fotara.data.repository.SettingsRepository
import com.arinara.fotara.ui.components.ProfileBanner
import com.arinara.fotara.ui.components.ProfileBannerDefaults
import com.arinara.fotara.ui.components.ProfileBannerHeader
import com.arinara.fotara.ui.components.SettingsSubScreenHeader
import com.arinara.fotara.ui.components.SettingsSubScreenHeaderDefaults
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Panorama
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.arinara.fotara.R
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.ui.components.presets.AppButton
import com.arinara.fotara.ui.components.presets.AppButtonVariant
import com.arinara.fotara.ui.components.presets.AppCard
import com.arinara.fotara.ui.components.presets.AppScreen
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import com.arinara.fotara.ui.components.LocalBottomOverlayPadding
import com.arinara.fotara.ui.settings.SettingsViewModel
import com.arinara.fotara.util.ProfileImageUtils
import java.io.File

@Composable
fun ProfileScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val profile = uiState.userProfile
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val bottomOverlayPadding = LocalBottomOverlayPadding.current

    var pendingCropUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingCropIsAvatar by rememberSaveable { mutableStateOf(true) }
    var showBorderPicker by remember { mutableStateOf(false) }
    var showClaimUsernameDialog by remember { mutableStateOf(false) }

    val defaultUserName = stringResource(R.string.profile_default_name)
    var nameInput by remember(profile.name) { mutableStateOf(profile.name) }
    var emailInput by remember(profile.email) { mutableStateOf(profile.email) }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pendingCropUriString = uri.toString()
            pendingCropIsAvatar = true
        }
    }

    var showBannerSourceDialog by remember { mutableStateOf(false) }

    val bannerImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val isGif = com.arinara.fotara.util.ProfileImageUtils.isGifUri(context, uri)
            if (isGif) {
                val size = com.arinara.fotara.util.ProfileImageUtils.getUriFileSize(context, uri)
                if (size > SettingsRepository.MAX_BANNER_GIF_BYTES) {
                    android.widget.Toast.makeText(context, context.getString(R.string.profile_banner_gif_too_large), android.widget.Toast.LENGTH_SHORT).show()
                    return@rememberLauncherForActivityResult
                }
            }
            pendingCropUriString = uri.toString()
            pendingCropIsAvatar = false
        }
    }

    val bannerGifPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val isGif = com.arinara.fotara.util.ProfileImageUtils.isGifUri(context, uri)
            if (!isGif) {
                android.widget.Toast.makeText(context, context.getString(R.string.profile_banner_not_a_gif), android.widget.Toast.LENGTH_SHORT).show()
                return@rememberLauncherForActivityResult
            }
            val size = com.arinara.fotara.util.ProfileImageUtils.getUriFileSize(context, uri)
            if (size > SettingsRepository.MAX_BANNER_GIF_BYTES) {
                android.widget.Toast.makeText(context, context.getString(R.string.profile_banner_gif_too_large), android.widget.Toast.LENGTH_SHORT).show()
                return@rememberLauncherForActivityResult
            }
            pendingCropUriString = uri.toString()
            pendingCropIsAvatar = false
        }
    }

    // Active Crop Screen Step
    if (pendingCropUriString != null) {
        val cropUri = remember(pendingCropUriString) { Uri.parse(pendingCropUriString) }
        val screenWidthDp = LocalConfiguration.current.screenWidthDp.toFloat()
        val bannerRatio = screenWidthDp / 230f
        val cleanupStaged = {
            if (cropUri.scheme == "file" && cropUri.path != null) {
                val path = cropUri.path!!
                if (path.contains("cache")) {
                    try { File(path).delete() } catch (_: Exception) {}
                }
            }
        }
        ProfileCropScreen(
            imageUri = cropUri,
            isAvatar = pendingCropIsAvatar,
            aspectRatio = if (pendingCropIsAvatar) 1.0f else bannerRatio,
            onCropSaved = { cropped, normRect ->
                if (pendingCropIsAvatar) {
                    viewModel.saveProfileAvatar(cropped)
                } else {
                    val isGif = com.arinara.fotara.util.ProfileImageUtils.isGifUri(context, cropUri)
                    if (isGif) {
                        val bytes = com.arinara.fotara.util.ProfileImageUtils.readBytesFromUri(context, cropUri)
                        if (bytes != null && bytes.size <= SettingsRepository.MAX_BANNER_GIF_BYTES) {
                            viewModel.saveProfileBannerGif(bytes, normRect.toSerializedString())
                        } else {
                            android.widget.Toast.makeText(context, context.getString(R.string.profile_banner_gif_too_large), android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        viewModel.saveProfileBanner(cropped)
                    }
                }
                cleanupStaged()
                pendingCropUriString = null
            },
            onCancel = {
                cleanupStaged()
                pendingCropUriString = null
            }
        )
        return
    }

    if (showBannerSourceDialog) {
        BannerSourceDialog(
            onDismiss = { showBannerSourceDialog = false },
            onChooseImage = {
                showBannerSourceDialog = false
                bannerImagePickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onChooseGif = {
                showBannerSourceDialog = false
                bannerGifPickerLauncher.launch(arrayOf("image/gif"))
            }
        )
    }

    if (showBorderPicker) {
        BorderPickerDialog(
            currentAvatarPath = profile.avatarPath,
            selectedBorderId = profile.borderId,
            onSelectBorder = { viewModel.updateProfileBorder(it) },
            onDismiss = { showBorderPicker = false }
        )
    }

    if (showClaimUsernameDialog) {
        com.arinara.fotara.feature.friends.ui.ClaimUsernameDialog(
            currentUsername = uiState.claimedUsername,
            canChangeUsername = uiState.canChangeUsername,
            cooldownDaysRemaining = uiState.cooldownDaysRemaining,
            cooldownMessage = uiState.cooldownMessage,
            onDismiss = { showClaimUsernameDialog = false },
            onClaim = { newUsername, displayName ->
                viewModel.claimUsername(newUsername, displayName) { success, msg ->
                    if (success) {
                        showClaimUsernameDialog = false
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    BackHandler {
        onBackClick()
    }

    val scrollState = rememberScrollState()

    AppScreen(
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ElectricSlateTokens.BgCanvas)
                .imePadding()
                .verticalScroll(scrollState)
                .padding(bottom = innerPadding.calculateBottomPadding() + bottomOverlayPadding)
        ) {
            // Header Banner with Gradient Fade, Centered Avatar, and Sub-Screen Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ProfileBannerDefaults.BannerHeight)
            ) {
                ProfileBannerHeader(
                    bannerPath = profile.bannerPath,
                    bannerUpdatedAt = profile.bannerUpdatedAt,
                    bannerCrop = profile.bannerCrop,
                    avatarPath = profile.avatarPath,
                    borderId = profile.borderId,
                    avatarUpdatedAt = profile.avatarUpdatedAt,
                    isOnScreen = true,
                    showEditButton = true,
                    onEditClick = {
                        avatarPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Sub-Screen Header
                SettingsSubScreenHeader(
                    title = stringResource(R.string.profile_title),
                    onBackClick = onBackClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = SettingsSubScreenHeaderDefaults.HorizontalPadding)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Actions Card
            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                containerColor = ElectricSlateTokens.SurfacePanel,
                borderColor = ElectricSlateTokens.BorderSubtle,
                cornerRadius = 20.dp,
                contentPadding = 4.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    // Change Picture
                    ProfileItemRow(
                        icon = Icons.Default.Image,
                        title = stringResource(R.string.profile_action_change_picture),
                        onClick = {
                            avatarPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )

                    if (profile.hasCustomAvatar) {
                        HorizontalDivider(color = ElectricSlateTokens.BorderSubtle, modifier = Modifier.padding(horizontal = 16.dp))
                        ProfileItemRow(
                            icon = Icons.Default.Delete,
                            title = stringResource(R.string.profile_action_remove_picture),
                            titleColor = ElectricSlateTokens.Destructive,
                            onClick = { viewModel.removeProfileAvatar() }
                        )
                    }

                    HorizontalDivider(color = ElectricSlateTokens.BorderSubtle, modifier = Modifier.padding(horizontal = 16.dp))

                    // Change Banner
                    ProfileItemRow(
                        icon = Icons.Default.Panorama,
                        title = stringResource(R.string.profile_action_change_banner),
                        onClick = {
                            showBannerSourceDialog = true
                        }
                    )

                    if (profile.hasCustomBanner) {
                        HorizontalDivider(color = ElectricSlateTokens.BorderSubtle, modifier = Modifier.padding(horizontal = 16.dp))
                        ProfileItemRow(
                            icon = Icons.Default.Delete,
                            title = stringResource(R.string.profile_action_remove_banner),
                            titleColor = ElectricSlateTokens.Destructive,
                            onClick = { viewModel.removeProfileBanner() }
                        )
                    }

                    HorizontalDivider(color = ElectricSlateTokens.BorderSubtle, modifier = Modifier.padding(horizontal = 16.dp))

                    // Choose Border (Disabled - placed at most bottom, gray, under construction)
                    ProfileItemRow(
                        icon = Icons.Default.AutoAwesome,
                        title = stringResource(R.string.profile_action_choose_border),
                        titleColor = Color.Gray,
                        iconTint = Color.Gray,
                        onClick = {
                            Toast.makeText(context, "Under construction", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Name & Email Input Card
            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                containerColor = ElectricSlateTokens.SurfacePanel,
                borderColor = ElectricSlateTokens.BorderSubtle,
                cornerRadius = 20.dp,
                contentPadding = 16.dp
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Name Field (max 30 chars, single line, trimmed, default fallback on save)
                Text(
                    text = stringResource(R.string.profile_name_label),
                    color = ElectricSlateTokens.TextPrimary,
                    fontSize = 14.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { if (it.length <= 30) nameInput = it },
                    singleLine = true,
                    placeholder = { Text(defaultUserName, color = ElectricSlateTokens.TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ElectricSlateTokens.TextPrimary,
                        unfocusedTextColor = ElectricSlateTokens.TextPrimary,
                        focusedContainerColor = ElectricSlateTokens.SurfaceElevated,
                        unfocusedContainerColor = ElectricSlateTokens.SurfaceElevated,
                        focusedBorderColor = ElectricSlateTokens.PrimaryAccent,
                        unfocusedBorderColor = ElectricSlateTokens.BorderSubtle,
                        cursorColor = ElectricSlateTokens.PrimaryAccent,
                        focusedLabelColor = ElectricSlateTokens.PrimaryAccent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val trimmed = nameInput.trim()
                            val finalName = if (trimmed.isEmpty()) defaultUserName else trimmed
                            viewModel.updateProfileName(finalName)
                            focusManager.clearFocus()
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused) {
                                val trimmed = nameInput.trim()
                                val finalName = if (trimmed.isEmpty()) defaultUserName else trimmed
                                viewModel.updateProfileName(finalName)
                            }
                        }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Academic Username Field
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElectricSlateTokens.SurfaceElevated)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Academic Username",
                            color = ElectricSlateTokens.TextPrimary,
                            fontSize = 14.sp,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val isUsernameSet = uiState.claimedUsername.isNotBlank()
                        Text(
                            text = if (isUsernameSet) uiState.claimedUsername else "Not set",
                            color = if (isUsernameSet) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TextMuted,
                            fontSize = 16.sp,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val cooldownText = if (!isUsernameSet) {
                            "Tap to set your academic username"
                        } else if (uiState.canChangeUsername) {
                            "Available to change"
                        } else {
                            "Cooldown active: ${uiState.cooldownDaysRemaining} days remaining"
                        }
                        Text(
                            text = cooldownText,
                            fontSize = 11.sp,
                            fontFamily = ElmsSans,
                            color = if (!isUsernameSet) ElectricSlateTokens.TextMuted else ElectricSlateTokens.PrimaryAccent
                        )
                    }
                    AppButton(
                        text = if (uiState.claimedUsername.isBlank()) "Set" else "Change",
                        onClick = { showClaimUsernameDialog = true },
                        variant = AppButtonVariant.OUTLINED,
                        height = 36.dp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Email Field (max 60 chars, single line, optional)
                Text(
                    text = stringResource(R.string.profile_email_label),
                    color = ElectricSlateTokens.TextPrimary,
                    fontSize = 14.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { if (it.length <= 60) emailInput = it },
                    singleLine = true,
                    placeholder = { Text("user@example.com", color = ElectricSlateTokens.TextMuted.copy(alpha = 0.5f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ElectricSlateTokens.TextPrimary,
                        unfocusedTextColor = ElectricSlateTokens.TextPrimary,
                        focusedContainerColor = ElectricSlateTokens.SurfaceElevated,
                        unfocusedContainerColor = ElectricSlateTokens.SurfaceElevated,
                        focusedBorderColor = ElectricSlateTokens.PrimaryAccent,
                        unfocusedBorderColor = ElectricSlateTokens.BorderSubtle,
                        cursorColor = ElectricSlateTokens.PrimaryAccent,
                        focusedLabelColor = ElectricSlateTokens.PrimaryAccent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            viewModel.updateProfileEmail(emailInput.trim())
                            focusManager.clearFocus()
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused) {
                                viewModel.updateProfileEmail(emailInput.trim())
                            }
                        }
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.profile_email_helper),
                    color = ElectricSlateTokens.TextMuted,
                    fontSize = 12.sp,
                    fontFamily = ElmsSans
                )
            }
        }
    }
    }
}

@Composable
private fun ProfileItemRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    titleColor: Color = ElectricSlateTokens.TextPrimary,
    iconTint: Color = if (titleColor == ElectricSlateTokens.Destructive) ElectricSlateTokens.Destructive else if (titleColor == Color.Gray) Color.Gray else ElectricSlateTokens.TextMuted,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = title,
            color = titleColor,
            fontSize = 16.sp,
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Medium
        )
    }
}
