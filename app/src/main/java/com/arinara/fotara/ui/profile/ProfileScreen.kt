// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.profile

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalConfiguration
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
import androidx.compose.material.icons.filled.Panorama
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.arinara.fotara.theme.HomeCardBorder
import com.arinara.fotara.theme.HomeCardSurface
import com.arinara.fotara.theme.HomeNearBlack
import com.arinara.fotara.theme.HomeSubtitleGray
import com.arinara.fotara.theme.TagCrimson
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

    val defaultUserName = stringResource(R.string.profile_default_name)
    var nameInput by remember(profile.name) { mutableStateOf(profile.name) }
    var emailInput by remember(profile.email) { mutableStateOf(profile.email) }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val stagedFile = ProfileImageUtils.stageUriToCache(context, uri, "avatar_crop")
            val stagedUri = stagedFile?.let { Uri.fromFile(it) } ?: uri
            pendingCropUriString = stagedUri.toString()
            pendingCropIsAvatar = true
        }
    }

    val bannerPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val stagedFile = ProfileImageUtils.stageUriToCache(context, uri, "banner_crop")
            val stagedUri = stagedFile?.let { Uri.fromFile(it) } ?: uri
            pendingCropUriString = stagedUri.toString()
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
                try { File(cropUri.path!!).delete() } catch (_: Exception) {}
            }
        }
        ProfileCropScreen(
            imageUri = cropUri,
            isAvatar = pendingCropIsAvatar,
            aspectRatio = if (pendingCropIsAvatar) 1.0f else bannerRatio,
            onCropSaved = { cropped ->
                if (pendingCropIsAvatar) {
                    viewModel.saveProfileAvatar(cropped)
                } else {
                    viewModel.saveProfileBanner(cropped)
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

    if (showBorderPicker) {
        BorderPickerDialog(
            currentAvatarPath = profile.avatarPath,
            selectedBorderId = profile.borderId,
            onSelectBorder = { viewModel.updateProfileBorder(it) },
            onDismiss = { showBorderPicker = false }
        )
    }

    BackHandler {
        onBackClick()
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HomeNearBlack)
            .imePadding()
            .verticalScroll(scrollState)
            .padding(bottom = bottomOverlayPadding + 24.dp)
    ) {
        // Header Banner with Gradient Fade
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            val bannerFile = remember(profile.bannerPath, profile.bannerUpdatedAt) { profile.bannerPath?.let { File(it) } }
            if (bannerFile != null && bannerFile.exists()) {
                val bannerCacheKey = "${bannerFile.absolutePath}_${if (profile.bannerUpdatedAt > 0L) profile.bannerUpdatedAt else bannerFile.lastModified()}"
                val bannerReq = remember(bannerCacheKey) {
                    ImageRequest.Builder(context)
                        .data(bannerFile)
                        .memoryCacheKey(bannerCacheKey)
                        .diskCacheKey(bannerCacheKey)
                        .crossfade(true)
                        .build()
                }
                AsyncImage(
                    model = bannerReq,
                    contentDescription = "Profile Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Subtle Theme Gradient Fallback
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF1E293B),
                                    Color(0xFF0F172A),
                                    HomeNearBlack
                                )
                            )
                        )
                )
            }

            // Top Scrim for Status Bar & Back Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.65f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Bottom Gradient Fade into HomeNearBlack
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                HomeNearBlack.copy(alpha = 0.75f),
                                HomeNearBlack
                            )
                        )
                    )
            )

            // Top Nav Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(WindowInsets.statusBars.asPaddingValues())
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF131925).copy(alpha = 0.85f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = stringResource(R.string.profile_title),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Bold
                )
            }

            // Centered Live Avatar View
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
            ) {
                ProfileAvatarView(
                    avatarPath = profile.avatarPath,
                    borderId = profile.borderId,
                    avatarSize = 78.dp,
                    avatarUpdatedAt = profile.avatarUpdatedAt,
                    showEditButton = true,
                    onEditClick = {
                        avatarPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Actions Card
        Card(
            colors = CardDefaults.cardColors(containerColor = HomeCardSurface),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, HomeCardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
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
                    HorizontalDivider(color = HomeCardBorder.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                    ProfileItemRow(
                        icon = Icons.Default.Delete,
                        title = stringResource(R.string.profile_action_remove_picture),
                        titleColor = TagCrimson,
                        onClick = { viewModel.removeProfileAvatar() }
                    )
                }

                HorizontalDivider(color = HomeCardBorder.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))

                // Choose Border
                ProfileItemRow(
                    icon = Icons.Default.AutoAwesome,
                    title = stringResource(R.string.profile_action_choose_border),
                    onClick = { showBorderPicker = true }
                )

                HorizontalDivider(color = HomeCardBorder.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))

                // Change Banner
                ProfileItemRow(
                    icon = Icons.Default.Panorama,
                    title = stringResource(R.string.profile_action_change_banner),
                    onClick = {
                        bannerPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )

                if (profile.hasCustomBanner) {
                    HorizontalDivider(color = HomeCardBorder.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                    ProfileItemRow(
                        icon = Icons.Default.Delete,
                        title = stringResource(R.string.profile_action_remove_banner),
                        titleColor = TagCrimson,
                        onClick = { viewModel.removeProfileBanner() }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Name & Email Input Card
        Card(
            colors = CardDefaults.cardColors(containerColor = HomeCardSurface),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, HomeCardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Name Field (max 30 chars, single line, trimmed, default fallback on save)
                Text(
                    text = stringResource(R.string.profile_name_label),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { if (it.length <= 30) nameInput = it },
                    singleLine = true,
                    placeholder = { Text(defaultUserName, color = HomeSubtitleGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF131925),
                        unfocusedContainerColor = Color(0xFF131925),
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = HomeCardBorder
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

                // Email Field (max 60 chars, single line, optional)
                Text(
                    text = stringResource(R.string.profile_email_label),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { if (it.length <= 60) emailInput = it },
                    singleLine = true,
                    placeholder = { Text("user@example.com", color = HomeSubtitleGray.copy(alpha = 0.5f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF131925),
                        unfocusedContainerColor = Color(0xFF131925),
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = HomeCardBorder
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
                    color = HomeSubtitleGray,
                    fontSize = 12.sp,
                    fontFamily = ElmsSans
                )
            }
        }
    }
}

@Composable
private fun ProfileItemRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    titleColor: Color = Color.White,
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
            tint = if (titleColor == TagCrimson) TagCrimson else Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = title,
            color = titleColor,
            fontSize = 15.sp,
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Medium
        )
    }
}
