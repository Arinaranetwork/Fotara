// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.settings

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.windowInsetsPadding
import com.arinara.fotara.ui.components.ScreenHeader
import com.arinara.fotara.ui.components.ProfileBannerHeader
import com.arinara.fotara.ui.components.SettingsSubScreenHeader
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.arinara.fotara.ui.components.LocalBottomOverlayPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.arinara.fotara.data.repository.SettingsRepository
import com.arinara.fotara.ui.components.ProfileBanner
import com.arinara.fotara.ui.settings.SettingsFadeMath
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.launch
import com.arinara.fotara.ui.profile.BorderPickerDialog
import com.arinara.fotara.ui.profile.CropShape
import com.arinara.fotara.ui.profile.ProfileAvatar
import com.arinara.fotara.ui.profile.ProfileCropScreen
import com.arinara.fotara.ui.profile.ProfileEditBottomSheet
import com.arinara.fotara.ui.profile.ProfileScreen
import com.arinara.fotara.util.ProfileImageUtils
import java.io.File
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arinara.fotara.data.model.DownsampleQuality
import com.arinara.fotara.data.model.SortOrder
import com.arinara.fotara.data.model.StorageLocation
import com.arinara.fotara.data.model.ThemeMode
import com.arinara.fotara.R
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.theme.HomeCardBorder
import com.arinara.fotara.theme.HomeCardSurface
import com.arinara.fotara.theme.HomeMainButtonBlue
import com.arinara.fotara.theme.HomeNearBlack
import com.arinara.fotara.theme.HomeSubtitleGray
import com.arinara.fotara.theme.TagAmber
import com.arinara.fotara.theme.TagCrimson
import kotlinx.coroutines.launch

/**
 * Logical main sections for Settings.
 */
enum class SettingsSection(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
) {
    PROFILE("Profile", "Photo, border, banner, name, email", Icons.Default.Person),
    GENERAL("General", "Grid density, default sort order", Icons.Default.GridView),
    APPEARANCE("Appearance", "Theme mode and display options", Icons.Default.Palette),
    OCR("OCR & Recognition", "On-device text extraction", Icons.Default.TextFields),
    NOTIFICATIONS("Notifications & Deadlines", "Reminders and test alert", Icons.Default.Notifications),
    STORAGE("Storage & Data Management", "Usage, trash, backup, re-indexing", Icons.Default.Storage),
    ABOUT("About & Legal", "Version info and offline architecture", Icons.Default.Info)
}

/**
 * Fotara v1.5.2 Settings Screen redesign.
 * Formatted as 6 main section rounded rectangular cards (~#111726, 22dp radius, 12dp spacing, 16dp padding)
 * on root, with clean un-carded list rows inside each opened detail section, keeping Elms Sans typography.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: (() -> Unit)? = null,
    onNavigateToTrash: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val context = LocalContext.current
    var activeSection by rememberSaveable { mutableStateOf<SettingsSection?>(null) }
    var activeLegalDocument by remember { mutableStateOf<com.arinara.fotara.legal.ParsedLegalDocument?>(null) }

    BackHandler(enabled = activeLegalDocument != null || activeSection != null) {
        if (activeLegalDocument != null) {
            activeLegalDocument = null
        } else {
            activeSection = null
        }
    }

    if (activeLegalDocument != null) {
        com.arinara.fotara.ui.legal.LegalDocumentScreen(
            document = activeLegalDocument!!,
            onBackClick = { activeLegalDocument = null }
        )
        return
    }

    val (appVersionName, appVersionCode) = remember(context) {
        try {
            val pInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            val vName = pInfo?.versionName ?: "1.6.0"
            val vCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pInfo?.longVersionCode ?: 26L
            } else {
                @Suppress("DEPRECATION")
                (pInfo?.versionCode ?: 26).toLong()
            }
            Pair(vName, vCode)
        } catch (_: Exception) {
            Pair("1.6.0", 26L)
        }
    }

    var showSortOrderDialog by remember { mutableStateOf(false) }
    var showGridDensityDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showOcrLanguageDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showLeadTimeDialog by remember { mutableStateOf(false) }
    var showStorageLocationDialog by remember { mutableStateOf(false) }
    var showCombineFileNameDialog by remember { mutableStateOf(false) }
    var showSavedImageLocationDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        val initialMsg = viewModel.consumeFeedbackMessage()
        if (initialMsg != null) {
            snackbarHostState.showSnackbar(initialMsg)
        }
        viewModel.eventFlow.collect { msg ->
            viewModel.consumeFeedbackMessage()
            snackbarHostState.showSnackbar(msg)
        }
    }

    val bottomOverlayPadding = LocalBottomOverlayPadding.current

    var showProfileEditSheet by remember { mutableStateOf(false) }
    var showBorderPicker by remember { mutableStateOf(false) }
    var pendingCropUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingCropIsAvatar by rememberSaveable { mutableStateOf(true) }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pendingCropUriString = uri.toString()
            pendingCropIsAvatar = true
        }
    }

    var showBannerSourceDialog by remember { mutableStateOf(false) }

    val bannerPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val isGif = ProfileImageUtils.isGifUri(context, uri)
            if (isGif) {
                val size = ProfileImageUtils.getUriFileSize(context, uri)
                if (size > SettingsRepository.MAX_BANNER_GIF_BYTES) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.profile_banner_gif_too_large))
                    }
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
            val isGif = ProfileImageUtils.isGifUri(context, uri)
            if (!isGif) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.profile_banner_not_a_gif))
                }
                return@rememberLauncherForActivityResult
            }
            val size = ProfileImageUtils.getUriFileSize(context, uri)
            if (size > SettingsRepository.MAX_BANNER_GIF_BYTES) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.profile_banner_gif_too_large))
                }
                return@rememberLauncherForActivityResult
            }
            pendingCropUriString = uri.toString()
            pendingCropIsAvatar = false
        }
    }

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
                    val isGif = ProfileImageUtils.isGifUri(context, cropUri)
                    if (isGif) {
                        val bytes = ProfileImageUtils.readBytesFromUri(context, cropUri)
                        if (bytes != null && bytes.size <= SettingsRepository.MAX_BANNER_GIF_BYTES) {
                            viewModel.saveProfileBannerGif(bytes, normRect.toSerializedString())
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(context.getString(R.string.profile_banner_gif_too_large))
                            }
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
        AlertDialog(
            onDismissRequest = { showBannerSourceDialog = false },
            containerColor = HomeCardSurface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = stringResource(R.string.profile_action_change_banner),
                    color = Color.White,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showBannerSourceDialog = false
                                bannerPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .padding(vertical = 12.dp, horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.profile_banner_choose_image),
                            color = Color.White,
                            fontFamily = ElmsSans,
                            fontSize = 16.sp
                        )
                    }

                    HorizontalDivider(color = HomeCardBorder.copy(alpha = 0.5f))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showBannerSourceDialog = false
                                bannerGifPickerLauncher.launch(arrayOf("image/gif"))
                            }
                            .padding(vertical = 12.dp, horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.profile_banner_choose_gif),
                            color = Color.White,
                            fontFamily = ElmsSans,
                            fontSize = 16.sp
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showBannerSourceDialog = false }) {
                    Text(
                        text = stringResource(R.string.action_cancel),
                        color = HomeSubtitleGray,
                        fontFamily = ElmsSans
                    )
                }
            }
        )
    }

    if (showProfileEditSheet) {
        ProfileEditBottomSheet(
            hasCustomAvatar = uiState.userProfile.hasCustomAvatar,
            hasCustomBanner = uiState.userProfile.hasCustomBanner,
            onChangePicture = {
                showProfileEditSheet = false
                avatarPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onChooseBorder = {
                showProfileEditSheet = false
                android.widget.Toast.makeText(context, "Under construction", android.widget.Toast.LENGTH_SHORT).show()
            },
            onChangeBanner = {
                showProfileEditSheet = false
                showBannerSourceDialog = true
            },
            onEditNameEmail = {
                showProfileEditSheet = false
                activeSection = SettingsSection.PROFILE
            },
            onRemovePicture = {
                viewModel.removeProfileAvatar()
                showProfileEditSheet = false
            },
            onRemoveBanner = {
                viewModel.removeProfileBanner()
                showProfileEditSheet = false
            },
            onDismiss = { showProfileEditSheet = false }
        )
    }

    if (showBorderPicker) {
        BorderPickerDialog(
            currentAvatarPath = uiState.userProfile.avatarPath,
            selectedBorderId = uiState.userProfile.borderId,
            onSelectBorder = { viewModel.updateProfileBorder(it) },
            onDismiss = { showBorderPicker = false }
        )
    }

    val effectiveBottomPadding = if (bottomOverlayPadding > 0.dp) bottomOverlayPadding else 96.dp

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .padding(bottom = effectiveBottomPadding + 8.dp)
                    .imePadding()
            )
        },
        containerColor = HomeNearBlack,
        modifier = modifier
    ) { innerPadding ->
        if (activeSection == SettingsSection.PROFILE) {
            ProfileScreen(
                viewModel = viewModel,
                onBackClick = { activeSection = null },
                modifier = Modifier.fillMaxSize()
            )
        } else if (activeSection == null) {
            val rootScrollState = rememberScrollState()
            var headerHeightPx by remember { mutableIntStateOf(0) }
            val density = LocalDensity.current
            val bannerHeightPx = with(density) { 230.dp.roundToPx() }
            val thresholdPx = with(density) { 160.dp.toPx() }

            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rootScrollState)
                        .padding(bottom = effectiveBottomPadding + 24.dp)
                ) {
                    // 1. Full-Width Banner with Gradient Fade
                    ProfileBannerHeader(
                        bannerPath = uiState.userProfile.bannerPath,
                        bannerUpdatedAt = uiState.userProfile.bannerUpdatedAt,
                        bannerCrop = uiState.userProfile.bannerCrop,
                        avatarPath = uiState.userProfile.avatarPath,
                        borderId = uiState.userProfile.borderId,
                        avatarUpdatedAt = uiState.userProfile.avatarUpdatedAt,
                        isOnScreen = rootScrollState.value < bannerHeightPx,
                        showEditButton = true,
                        onEditClick = { showProfileEditSheet = true }
                    )

                    // Centered User Name & Optional Email
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 18.dp)
                    ) {
                        val defaultName = stringResource(R.string.profile_default_name)
                        Text(
                            text = uiState.userProfile.resolvedName(defaultName),
                            color = Color.White,
                            fontSize = 22.sp,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState.userProfile.email.isNotBlank()) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = uiState.userProfile.email,
                                color = HomeSubtitleGray,
                                fontSize = 14.sp,
                                fontFamily = ElmsSans
                            )
                        }
                    }

                    // 7 Category Cards
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                    ) {
                        SettingsSection.entries.forEach { section ->
                            SettingsCardItem(
                                title = section.title,
                                subtitle = section.subtitle,
                                icon = section.icon,
                                onClick = { activeSection = section }
                            )
                        }
                    }
                }

                // 2. Pinned Fade Gradient and ScreenHeader above scrolling content
                val fadeHeightDp = with(density) {
                    if (headerHeightPx > 0) (headerHeightPx.toDp() + 24.dp) else 0.dp
                }

                if (fadeHeightDp > 0.dp) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(fadeHeightDp)
                            .align(Alignment.TopCenter)
                            .graphicsLayer {
                                alpha = SettingsFadeMath.computeFadeAlpha(
                                    scrollOffsetPx = rootScrollState.value.toFloat(),
                                    thresholdPx = thresholdPx
                                )
                            }
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        HomeNearBlack,
                                        HomeNearBlack,
                                        HomeNearBlack.copy(alpha = 0.95f),
                                        HomeNearBlack.copy(alpha = 0.5f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }

                ScreenHeader(
                    title = "Settings",
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .onSizeChanged { headerHeightPx = it.height }
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 18.dp,
                    end = 18.dp,
                    top = innerPadding.calculateTopPadding() + contentPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + contentPadding.calculateBottomPadding() + effectiveBottomPadding + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Header of Active Section with Back Arrow to Root
                item {
                    SettingsSubScreenHeader(
                        title = activeSection!!.title,
                        onBackClick = { activeSection = null }
                    )
                }

                // Clean Un-carded List Rows inside Selected Section
                when (activeSection) {
                    SettingsSection.PROFILE -> {}
                    SettingsSection.GENERAL -> {
                        item {
                            SettingsRowItem(
                                title = "Default Sort Order",
                                subtitle = uiState.userSettings.defaultSortOrder.displayName,
                                icon = Icons.Default.GridView,
                                onClick = { showSortOrderDialog = true }
                            )
                        }
                        item { SettingsListDivider() }
                        item {
                            SettingsRowItem(
                                title = "Thumbnail Grid Density",
                                subtitle = "${uiState.userSettings.gridDensity} columns",
                                icon = Icons.Default.GridView,
                                onClick = { showGridDensityDialog = true }
                            )
                        }
                        item { SettingsListDivider() }
                        item {
                            SettingsRowItem(
                                title = "Photo Storage Location",
                                subtitle = uiState.userSettings.storageLocation.displayName,
                                icon = Icons.Default.Storage,
                                onClick = { showStorageLocationDialog = true }
                            )
                        }
                        item { SettingsListDivider() }
                        item {
                            SettingsRowItem(
                                title = stringResource(R.string.setting_combine_file_name_title),
                                subtitle = uiState.userSettings.combineFileNamePreset,
                                icon = Icons.Default.Description,
                                onClick = { showCombineFileNameDialog = true }
                            )
                        }
                        item { SettingsListDivider() }
                        item {
                            SettingsRowItem(
                                title = stringResource(R.string.settings_saved_image_location_title),
                                subtitle = uiState.userSettings.getEffectiveSavedImageRelativePath(),
                                icon = Icons.Default.Image,
                                onClick = { showSavedImageLocationDialog = true }
                            )
                        }
                    }
                    SettingsSection.APPEARANCE -> {
                        item {
                            SettingsRowItem(
                                title = "Theme",
                                subtitle = uiState.userSettings.themeMode.displayName,
                                icon = Icons.Default.Palette,
                                onClick = { showThemeDialog = true }
                            )
                        }
                    }
                    SettingsSection.OCR -> {
                        item {
                            SettingsRowToggle(
                                title = "Automatic OCR on Capture",
                                subtitle = "Extract handwritten & printed text immediately after capture",
                                icon = Icons.Default.TextFields,
                                checked = uiState.userSettings.autoOcrEnabled,
                                onCheckedChange = { viewModel.updateAutoOcr(it) }
                            )
                        }
                        item { SettingsListDivider() }
                        item {
                            SettingsRowItem(
                                title = "OCR Recognition Script",
                                subtitle = uiState.userSettings.ocrLanguage,
                                icon = Icons.Default.Language,
                                onClick = { showOcrLanguageDialog = true }
                            )
                        }
                        item { SettingsListDivider() }
                        item {
                            SettingsRowItem(
                                title = "Downsampling Quality Tradeoff",
                                subtitle = uiState.userSettings.downsampleQuality.displayName,
                                icon = Icons.Default.Image,
                                onClick = { showQualityDialog = true }
                            )
                        }
                    }
                    SettingsSection.NOTIFICATIONS -> {
                        item {
                            val notificationsEnabled = remember(context) {
                                androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
                            }
                            SettingsRowItem(
                                title = "System Notification Permission",
                                subtitle = if (notificationsEnabled) "Permission granted" else "Notifications disabled in system settings",
                                icon = Icons.Default.Notifications,
                                onClick = {
                                    try {
                                        val intent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                            putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = android.net.Uri.fromParts("package", context.packageName, null)
                                        }
                                        context.startActivity(intent)
                                    }
                                }
                            )
                        }
                        item { SettingsListDivider() }
                        item {
                            val leadTimeText = when (uiState.userSettings.reminderLeadTimeHours) {
                                1 -> "1 hour before deadline"
                                3 -> "3 hours before deadline"
                                24 -> "24 hours before deadline"
                                else -> "${uiState.userSettings.reminderLeadTimeHours} hours before deadline"
                            }
                            SettingsRowItem(
                                title = "Default Reminder Lead Time",
                                subtitle = leadTimeText,
                                icon = Icons.Default.Schedule,
                                onClick = { showLeadTimeDialog = true }
                            )
                        }
                        item { SettingsListDivider() }
                        item {
                            SettingsRowToggle(
                                title = "\"Due Tomorrow\" Home Ribbon",
                                subtitle = "Display urgent deadline alerts on home dashboard",
                                icon = Icons.Default.ViewStream,
                                checked = uiState.userSettings.dueTomorrowRibbonEnabled,
                                onCheckedChange = { viewModel.updateDueTomorrowRibbon(it) }
                            )
                        }
                        item { SettingsListDivider() }
                        item {
                            SettingsRowAction(
                                title = "Test Notification Alert",
                                subtitle = "Trigger an immediate test study reminder notification",
                                icon = Icons.Default.Notifications,
                                actionIcon = Icons.Default.PlayArrow,
                                isLoading = false,
                                onClick = {
                                    com.arinara.fotara.util.NoteScheduleManager(context).sendTestAlert()
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Test alert dispatched. Check notification tray.")
                                    }
                                }
                            )
                        }
                    }
                    SettingsSection.STORAGE -> {
                        item {
                            SettingsStorageBreakdownCard(
                                formattedPhotos = uiState.storageBreakdown.formattedPhotos,
                                formattedThumbnails = uiState.storageBreakdown.formattedThumbnails,
                                formattedDatabase = uiState.storageBreakdown.formattedDatabase,
                                formattedTotal = uiState.storageBreakdown.formattedTotal
                            )
                        }
                        item { SettingsListDivider() }
                        item {
                            SettingsRowItem(
                                title = "Trash / Recycle Bin",
                                subtitle = "View and restore deleted folders and notes",
                                icon = Icons.Default.Delete,
                                iconTint = TagCrimson,
                                onClick = onNavigateToTrash
                            )
                        }
                        item { SettingsListDivider() }
                        item {
                            SettingsRowAction(
                                title = "Rebuild Thumbnails",
                                subtitle = "Regenerate thumbnail cache from originals",
                                icon = Icons.Default.Refresh,
                                actionIcon = Icons.Default.Refresh,
                                isLoading = uiState.isRebuildingThumbnails,
                                onClick = { viewModel.rebuildThumbnails() }
                            )
                        }
                        item { SettingsListDivider() }
                        item {
                            SettingsRowAction(
                                title = "Rebuild Search Index",
                                subtitle = "Forces full SQLite FTS4 virtual table re-indexing",
                                icon = Icons.Default.FindInPage,
                                actionIcon = Icons.Default.Refresh,
                                isLoading = uiState.isRebuildingSearchIndex,
                                onClick = { viewModel.rebuildSearchIndex() }
                            )
                        }
                        item { SettingsListDivider() }
                        item {
                            SettingsRowAction(
                                title = "Export Backup (JSON)",
                                subtitle = "Create offline backup of all folders, subfolders, and notes",
                                icon = Icons.Default.Upload,
                                actionIcon = Icons.Default.Upload,
                                isLoading = uiState.isExportingBackup,
                                onClick = { viewModel.requestExportBackup() }
                            )
                        }
                        item { SettingsListDivider() }
                        item {
                            SettingsRowAction(
                                title = "Import from Backup",
                                subtitle = "Restore coursework folders and notes from backup JSON",
                                icon = Icons.Default.Download,
                                actionIcon = Icons.Default.Download,
                                isLoading = uiState.isImportingBackup,
                                onClick = { viewModel.requestImportBackup() }
                            )
                        }
                    }
                    SettingsSection.ABOUT -> {
                        item {
                            SettingsAboutCard(
                                versionName = appVersionName,
                                versionCode = appVersionCode
                            )
                        }
                        item { SettingsListDivider() }
                        item {
                            SettingsRowItem(
                                title = stringResource(R.string.privacy_policy_title),
                                subtitle = stringResource(R.string.privacy_policy_subtitle),
                                icon = Icons.Default.Info,
                                onClick = {
                                    try {
                                        activeLegalDocument = com.arinara.fotara.legal.LegalDocumentLoader(context).loadPrivacyPolicy()
                                    } catch (_: Exception) {}
                                }
                            )
                        }
                        item { SettingsListDivider() }
                        item {
                            SettingsRowItem(
                                title = stringResource(R.string.terms_of_service_title),
                                subtitle = stringResource(R.string.terms_of_service_subtitle),
                                icon = Icons.Default.Description,
                                onClick = {
                                    try {
                                        activeLegalDocument = com.arinara.fotara.legal.LegalDocumentLoader(context).loadTermsOfService()
                                    } catch (_: Exception) {}
                                }
                            )
                        }
                        item { SettingsListDivider() }
                        item {
                            Text(
                                text = stringResource(R.string.settings_privacy_header),
                                color = Color(0xFF60A5FA),
                                fontSize = 13.sp,
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 4.dp)
                            )
                        }
                        item {
                            SettingsRowToggle(
                                title = stringResource(R.string.device_count_switch_title),
                                subtitle = stringResource(R.string.device_count_switch_desc),
                                icon = Icons.Default.Storage,
                                checked = uiState.userSettings.isDeviceCountEnabled,
                                onCheckedChange = { viewModel.updateDeviceCountEnabled(it) }
                            )
                        }
                        val currentDeviceId = uiState.registeredDeviceId
                        if (uiState.userSettings.isDeviceCountEnabled && currentDeviceId != null) {
                            item {
                                DeviceIdExpandablePanel(
                                    deviceId = currentDeviceId,
                                    onCopy = { id ->
                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("Fotara Device ID", id)
                                        clipboard?.setPrimaryClip(clip)
                                        android.widget.Toast.makeText(context, "Device ID copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                        item { SettingsListDivider() }
                        item {
                            SettingsRowItem(
                                title = "Open Source Notices & Licenses",
                                subtitle = "View third-party software attributions",
                                icon = Icons.AutoMirrored.Filled.MenuBook,
                                onClick = { viewModel.setLicensesDialogVisible(true) }
                            )
                        }
                    }
                    null -> {}
                }
            }
        }
    }

    // Dialogs
    if (showSortOrderDialog) {
        OptionSelectionDialog(
            title = "Default Sort Order",
            options = SortOrder.entries.map { it to it.displayName },
            selectedOption = uiState.userSettings.defaultSortOrder,
            onOptionSelected = {
                viewModel.updateSortOrder(it)
                showSortOrderDialog = false
            },
            onDismiss = { showSortOrderDialog = false }
        )
    }

    if (showGridDensityDialog) {
        OptionSelectionDialog(
            title = "Thumbnail Grid Density",
            options = listOf(2 to "2 Columns (Comfortable)", 3 to "3 Columns (Standard)", 4 to "4 Columns (Compact)"),
            selectedOption = uiState.userSettings.gridDensity,
            onOptionSelected = {
                viewModel.updateGridDensity(it)
                showGridDensityDialog = false
            },
            onDismiss = { showGridDensityDialog = false }
        )
    }

    if (showThemeDialog) {
        OptionSelectionDialog(
            title = "Theme",
            options = ThemeMode.entries.map { it to it.displayName },
            selectedOption = uiState.userSettings.themeMode,
            onOptionSelected = {
                viewModel.updateThemeMode(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showOcrLanguageDialog) {
        OptionSelectionDialog(
            title = "OCR Recognition Script",
            options = listOf(
                "Latin" to "Latin (Default - English, French, Spanish, German)",
                "English" to "English Only",
                "Auto" to "Auto-Detect Language"
            ),
            selectedOption = uiState.userSettings.ocrLanguage,
            onOptionSelected = {
                viewModel.updateOcrLanguage(it)
                showOcrLanguageDialog = false
            },
            onDismiss = { showOcrLanguageDialog = false }
        )
    }

    if (showQualityDialog) {
        OptionSelectionDialog(
            title = "Pre-OCR Downsampling Quality",
            options = DownsampleQuality.entries.map { it to it.displayName },
            selectedOption = uiState.userSettings.downsampleQuality,
            onOptionSelected = {
                viewModel.updateDownsampleQuality(it)
                showQualityDialog = false
            },
            onDismiss = { showQualityDialog = false }
        )
    }

    if (showLeadTimeDialog) {
        OptionSelectionDialog(
            title = "Default Reminder Lead Time",
            options = listOf(
                1 to "1 Hour before deadline (H-1)",
                3 to "3 Hours before deadline (H-3)",
                24 to "24 Hours before deadline (H-24)"
            ),
            selectedOption = uiState.userSettings.reminderLeadTimeHours,
            onOptionSelected = {
                viewModel.updateReminderLeadTime(it)
                showLeadTimeDialog = false
            },
            onDismiss = { showLeadTimeDialog = false }
        )
    }

    if (showStorageLocationDialog) {
        OptionSelectionDialog(
            title = "Photo Storage Location",
            options = StorageLocation.entries.map { it to it.displayName },
            selectedOption = uiState.userSettings.storageLocation,
            onOptionSelected = {
                viewModel.updateStorageLocation(it)
                showStorageLocationDialog = false
            },
            onDismiss = { showStorageLocationDialog = false }
        )
    }

    if (showCombineFileNameDialog) {
        CombineFileNamePresetDialog(
            initialPreset = uiState.userSettings.combineFileNamePreset,
            onSave = { newPreset ->
                viewModel.updateCombineFileNamePreset(newPreset)
                showCombineFileNameDialog = false
            },
            onDismiss = { showCombineFileNameDialog = false }
        )
    }

    if (showSavedImageLocationDialog) {
        SavedImageLocationDialog(
            currentLocationKey = uiState.userSettings.savedImageLocation,
            currentCustomName = uiState.userSettings.savedImageCustomName,
            onSave = { locationKey, customName ->
                viewModel.updateSavedImageLocation(locationKey, customName)
                showSavedImageLocationDialog = false
            },
            onDismiss = { showSavedImageLocationDialog = false }
        )
    }

    // Backup Export Dialog
    if (uiState.showExportDialog && uiState.exportedJsonString != null) {
        val json = uiState.exportedJsonString!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissExportDialog() },
            containerColor = HomeCardSurface,
            title = {
                Text(
                    text = "Backup Export Ready",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Your offline backup JSON is ready (${json.length} characters). You can copy it to your clipboard or share it to save a local file.",
                        color = HomeSubtitleGray,
                        fontSize = 14.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Light
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(HomeNearBlack, RoundedCornerShape(12.dp))
                            .border(1.dp, HomeCardBorder, RoundedCornerShape(12.dp))
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = json.take(1000) + if (json.length > 1000) "\n... (truncated for preview)" else "",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Fotara Backup", json)
                        clipboard?.setPrimaryClip(clip)
                        viewModel.dismissExportDialog()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HomeMainButtonBlue)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy JSON", fontFamily = ElmsSans, fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, json)
                                type = "application/json"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Share Fotara Backup")
                            context.startActivity(shareIntent)
                            viewModel.dismissExportDialog()
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", color = Color.White, fontFamily = ElmsSans)
                    }
                    TextButton(onClick = { viewModel.dismissExportDialog() }) {
                        Text("Close", color = HomeSubtitleGray, fontFamily = ElmsSans)
                    }
                }
            }
        )
    }

    // Backup Import Dialog
    if (uiState.showImportDialog) {
        var importInputText by remember { mutableStateOf("") }
        var inputError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { viewModel.dismissImportDialog() },
            containerColor = HomeCardSurface,
            title = {
                Text(
                    text = "Import Backup",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Paste your exported Fotara backup JSON string below. New folders, subfolders, and notes will be merged without overwriting existing records.",
                        color = HomeSubtitleGray,
                        fontSize = 14.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Light
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = importInputText,
                        onValueChange = {
                            importInputText = it
                            inputError = null
                        },
                        placeholder = { Text("Paste JSON here...", color = HomeSubtitleGray, fontFamily = ElmsSans) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HomeMainButtonBlue,
                            unfocusedBorderColor = HomeCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        isError = inputError != null
                    )
                    if (inputError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = inputError!!, color = TagCrimson, fontSize = 12.sp, fontFamily = ElmsSans)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = importInputText.trim()
                        if (trimmed.isBlank()) {
                            inputError = "Please enter backup JSON content."
                        } else {
                            viewModel.executeImportBackup(trimmed)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HomeMainButtonBlue)
                ) {
                    Text("Import", fontFamily = ElmsSans, fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissImportDialog() }) {
                    Text("Cancel", color = HomeSubtitleGray, fontFamily = ElmsSans)
                }
            }
        )
    }

    // Open Source Licenses Dialog
    if (uiState.showLicensesDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setLicensesDialogVisible(false) },
            containerColor = HomeCardSurface,
            title = {
                Text(
                    text = "Open Source Notices",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    LicenseItem(
                        name = "Jetpack Compose & AndroidX",
                        license = "Apache License 2.0",
                        copyright = "Copyright (c) The Android Open Source Project"
                    )
                    LicenseItem(
                        name = "Google ML Kit Text Recognition",
                        license = "Apache License 2.0",
                        copyright = "Copyright (c) Google LLC"
                    )
                    LicenseItem(
                        name = "Kotlin Coroutines & Serialization",
                        license = "Apache License 2.0",
                        copyright = "Copyright (c) JetBrains s.r.o."
                    )
                    LicenseItem(
                        name = "Coil Image Loading",
                        license = "Apache License 2.0",
                        copyright = "Copyright (c) Coil Contributors"
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.setLicensesDialogVisible(false) }) {
                    Text("Close", color = Color.White, fontFamily = ElmsSans)
                }
            }
        )
    }
}

/**
 * Standard clickable settings card (~#111726, 22dp radius, 16dp padding).
 * Holds a 42dp neutral slate icon tile, title (Medium), subtitle (Light, muted), and chevron/value.
 */
@Composable
private fun SettingsCardItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    iconTint: Color = Color(0xFF94A3B8),
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = HomeCardSurface),
        border = BorderStroke(1.dp, HomeCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // ~42dp neutral slate icon tile (same style as folder tiles)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E2638)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Medium
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = HomeSubtitleGray,
                        fontSize = 13.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Light,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Settings card with a Switch toggle.
 */
@Composable
private fun SettingsCardToggle(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = HomeCardSurface),
        border = BorderStroke(1.dp, HomeCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E2638)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Medium
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = HomeSubtitleGray,
                        fontSize = 13.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Light,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = HomeMainButtonBlue,
                    uncheckedThumbColor = Color(0xFF94A3B8),
                    uncheckedTrackColor = Color(0xFF1E2638)
                )
            )
        }
    }
}

/**
 * Settings card with an action button or loading spinner.
 */
@Composable
private fun SettingsCardAction(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    actionIcon: ImageVector,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = HomeCardSurface),
        border = BorderStroke(1.dp, HomeCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLoading, onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E2638)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Medium
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = HomeSubtitleGray,
                        fontSize = 13.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Light,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isLoading) {
                CircularProgressIndicator(
                    color = HomeMainButtonBlue,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = title,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Storage breakdown card with pills and usage stats.
 */
@Composable
private fun SettingsStorageBreakdownCard(
    formattedPhotos: String,
    formattedThumbnails: String,
    formattedDatabase: String,
    formattedTotal: String
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = HomeCardSurface),
        border = BorderStroke(1.dp, HomeCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E2638)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Storage Usage Breakdown",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Total Fotara Data: $formattedTotal",
                        color = HomeSubtitleGray,
                        fontSize = 13.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Light
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StoragePill(label = "Photos", size = formattedPhotos, modifier = Modifier.weight(1f))
                StoragePill(label = "Thumbnails", size = formattedThumbnails, modifier = Modifier.weight(1f))
                StoragePill(label = "Database", size = formattedDatabase, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StoragePill(label: String, size: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color(0xFF131925), RoundedCornerShape(10.dp))
            .border(1.dp, HomeCardBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, color = HomeSubtitleGray, fontSize = 11.sp, fontFamily = ElmsSans, fontWeight = FontWeight.Light)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = size, color = TagAmber, fontSize = 13.sp, fontFamily = ElmsSans, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * About Fotara card with app version and legal notices.
 */
@Composable
private fun SettingsAboutCard(versionName: String, versionCode: Long) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = HomeCardSurface),
        border = BorderStroke(1.dp, HomeCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E2638)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                val info = com.arinara.fotara.online.VersionInfo.parse(versionName)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Fotara ${info.displayVersion}",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    com.arinara.fotara.ui.components.ChannelPill(
                        channel = info.channelLabel
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Build $versionCode",
                    color = Color(0xFF60A5FA),
                    fontSize = 12.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Specialized photo organization and on-device OCR for students.",
                    color = HomeSubtitleGray,
                    fontSize = 13.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Light,
                    lineHeight = 17.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Arinara Network • 100% Offline Architecture",
                    color = Color(0xFF475569),
                    fontSize = 12.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Light
                )
            }
        }
    }
}

@Composable
private fun LicenseItem(name: String, license: String, copyright: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = name, color = Color.White, fontSize = 14.sp, fontFamily = ElmsSans, fontWeight = FontWeight.Medium)
        Text(text = license, color = Color(0xFF60A5FA), fontSize = 12.sp, fontFamily = ElmsSans, fontWeight = FontWeight.Light)
        Text(text = copyright, color = HomeSubtitleGray, fontSize = 11.sp, fontFamily = ElmsSans, fontWeight = FontWeight.Light)
        Spacer(modifier = Modifier.height(6.dp))
        HorizontalDivider(color = HomeCardBorder)
    }
}

@Composable
private fun <T> OptionSelectionDialog(
    title: String,
    options: List<Pair<T, String>>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HomeCardSurface,
        title = {
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                options.forEach { (option, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOptionSelected(option) }
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(
                            selected = option == selectedOption,
                            onClick = { onOptionSelected(option) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = HomeMainButtonBlue,
                                unselectedColor = Color(0xFF64748B)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            color = if (option == selectedOption) Color.White else HomeSubtitleGray,
                            fontSize = 14.sp,
                            fontFamily = ElmsSans,
                            fontWeight = if (option == selectedOption) FontWeight.Medium else FontWeight.Light
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = HomeSubtitleGray, fontFamily = ElmsSans)
            }
        }
    )
}

/**
 * Clean un-carded list item for settings inside an opened section.
 */
@Composable
private fun SettingsRowItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = Color(0xFF94A3B8),
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = HomeSubtitleGray,
                    fontSize = 13.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Light,
                    lineHeight = 17.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF64748B),
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Clean un-carded toggle row for settings inside an opened section.
 */
@Composable
private fun SettingsRowToggle(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 4.dp, vertical = 10.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = HomeSubtitleGray,
                    fontSize = 13.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Light,
                    lineHeight = 17.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = HomeMainButtonBlue,
                uncheckedThumbColor = Color(0xFF94A3B8),
                uncheckedTrackColor = Color(0xFF1E2638)
            )
        )
    }
}

/**
 * Clean un-carded action row for settings inside an opened section.
 */
@Composable
private fun SettingsRowAction(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    actionIcon: ImageVector,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLoading, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = HomeSubtitleGray,
                    fontSize = 13.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Light,
                    lineHeight = 17.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (isLoading) {
            CircularProgressIndicator(
                color = HomeMainButtonBlue,
                strokeWidth = 2.dp,
                modifier = Modifier.size(22.dp)
            )
        } else {
            IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = actionIcon,
                    contentDescription = title,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsListDivider() {
    HorizontalDivider(
        color = Color(0xFF161E30),
        thickness = 0.8.dp,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun DeviceIdExpandablePanel(
    deviceId: String,
    onCopy: (String) -> Unit
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    Surface(
        color = Color(0xFF111726),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .clickable { isExpanded = !isExpanded }
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Anonymous Device Identifier",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (isExpanded) "Tap to collapse" else "Tap to view identifier",
                        color = HomeSubtitleGray,
                        fontSize = 12.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Light
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(22.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp)
                ) {
                    HorizontalDivider(
                        color = Color(0xFF1E293B),
                        thickness = 1.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        color = Color(0xFF0A0D14),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = deviceId,
                            color = Color(0xFFE2E8F0),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Random local UUID v4. No hardware identifiers or personal data are collected.",
                            color = Color(0xFF6F7491),
                            fontSize = 11.sp,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Light,
                            lineHeight = 15.sp,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        )

                        Button(
                            onClick = { onCopy(deviceId) },
                            colors = ButtonDefaults.buttonColors(containerColor = HomeMainButtonBlue),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Copy",
                                fontSize = 12.sp,
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedImageLocationDialog(
    currentLocationKey: String,
    currentCustomName: String,
    onSave: (locationKey: String, customName: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedOption by remember { mutableStateOf(currentLocationKey) }
    var customNameText by remember { mutableStateOf(currentCustomName) }

    val trimmed = customNameText.trim()
    val isCustomValid = selectedOption != "custom" || (
        trimmed.isNotEmpty() && trimmed.length <= 30 && trimmed.matches(Regex("^[a-zA-Z0-9 _-]+$"))
    )

    val currentEffectivePath = when (selectedOption) {
        "dcim_fotara" -> "DCIM/Fotara"
        "custom" -> {
            val folder = trimmed.ifEmpty { "Fotara" }
            "Pictures/$folder"
        }
        else -> "Pictures/Fotara"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HomeCardSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = stringResource(R.string.settings_saved_image_location_title),
                color = Color.White,
                fontSize = 18.sp,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = currentEffectivePath,
                    color = TagAmber,
                    fontSize = 13.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Option 1: Pictures/Fotara
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedOption = "pictures_fotara" }
                        .padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = selectedOption == "pictures_fotara",
                        onClick = { selectedOption = "pictures_fotara" },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = HomeMainButtonBlue,
                            unselectedColor = Color(0xFF64748B)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.settings_saved_image_location_pictures),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontFamily = ElmsSans
                    )
                }

                // Option 2: DCIM/Fotara
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedOption = "dcim_fotara" }
                        .padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = selectedOption == "dcim_fotara",
                        onClick = { selectedOption = "dcim_fotara" },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = HomeMainButtonBlue,
                            unselectedColor = Color(0xFF64748B)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.settings_saved_image_location_dcim),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontFamily = ElmsSans
                    )
                }

                // Option 3: Custom folder under Pictures
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedOption = "custom" }
                        .padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = selectedOption == "custom",
                        onClick = { selectedOption = "custom" },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = HomeMainButtonBlue,
                            unselectedColor = Color(0xFF64748B)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.settings_saved_image_location_custom),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontFamily = ElmsSans
                    )
                }

                if (selectedOption == "custom") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customNameText,
                        onValueChange = { input ->
                            val filtered = input.filter { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it == ' ' || it == '-' || it == '_' }.take(30)
                            customNameText = filtered
                        },
                        label = { Text(stringResource(R.string.settings_saved_image_custom_folder_hint)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HomeMainButtonBlue,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = HomeMainButtonBlue,
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.settings_saved_image_legacy_notice),
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontFamily = ElmsSans
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedOption, customNameText.trim()) },
                enabled = isCustomValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = HomeMainButtonBlue,
                    disabledContainerColor = HomeMainButtonBlue.copy(alpha = 0.38f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save", color = Color.White, fontFamily = ElmsSans, fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = HomeSubtitleGray, fontFamily = ElmsSans)
            }
        }
    )
}
