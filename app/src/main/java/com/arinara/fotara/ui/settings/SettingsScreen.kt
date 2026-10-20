// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.settings

import android.content.Intent
import com.arinara.fotara.feature.friends.data.FriendsRepository
import com.arinara.fotara.feature.friends.data.LocalFriendsRepository
import com.arinara.fotara.feature.friends.ui.FriendsScreen
import com.arinara.fotara.feature.friends.ui.FriendsSettingsCard
import com.arinara.fotara.feature.friends.ui.FriendsViewModel
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
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
import com.arinara.fotara.ui.components.presets.AppButton
import com.arinara.fotara.ui.components.presets.AppButtonVariant
import com.arinara.fotara.ui.components.presets.AppCard
import com.arinara.fotara.ui.components.presets.AppListItem
import com.arinara.fotara.ui.components.presets.AppListItemAction
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.arinara.fotara.ui.components.LocalBottomOverlayPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Alarm
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
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.collectAsState
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
import com.arinara.fotara.theme.FotaraShapes
import com.arinara.fotara.theme.FotaraSpacing

import androidx.compose.material3.MaterialTheme
import kotlinx.coroutines.launch

/**
 * Logical main sections for Settings.
 */
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Archive
import com.arinara.fotara.data.repository.SpaceRepository
import com.arinara.fotara.data.repository.WorkspaceRepository
import com.arinara.fotara.feature.backup.ui.BackupRestoreScreen

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
    spaceRepository: SpaceRepository? = null,
    workspaceRepository: WorkspaceRepository? = null,
    friendsRepository: FriendsRepository? = null,
    onBackClick: (() -> Unit)? = null,
    onNavigateToTrash: () -> Unit,
    onOpenUpdateScreen: (() -> Unit)? = null,
    onOpenWhatsNew: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    resetToRootTrigger: Int = 0,
    onSubScreenStateChanged: ((Boolean) -> Unit)? = null,
    scrollState: androidx.compose.foundation.ScrollState? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val context = LocalContext.current
    var activeSection by rememberSaveable { mutableStateOf<SettingsSection?>(null) }
    var activeLegalDocument by remember { mutableStateOf<com.arinara.fotara.legal.ParsedLegalDocument?>(null) }
    var showFriendsScreen by rememberSaveable { mutableStateOf(false) }
    var showBackupRestoreScreen by rememberSaveable { mutableStateOf(false) }
    var showDesignSystemDebugScreen by rememberSaveable { mutableStateOf(false) }
    var showHomeRefinedShowcaseScreen by rememberSaveable { mutableStateOf(false) }
    var showDiagnosticScreen by rememberSaveable { mutableStateOf(false) }

    val actualFriendsRepo = remember(context, friendsRepository) {
        friendsRepository ?: LocalFriendsRepository(context)
    }
    val friendsViewModel: FriendsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = FriendsViewModel.provideFactory(actualFriendsRepo)
    )
    val friendsUiState by friendsViewModel.uiState.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(resetToRootTrigger) {
        if (resetToRootTrigger > 0) {
            activeLegalDocument = null
            activeSection = null
            showFriendsScreen = false
            showBackupRestoreScreen = false
            showDesignSystemDebugScreen = false
            showHomeRefinedShowcaseScreen = false
            showDiagnosticScreen = false
        }
    }

    androidx.compose.runtime.LaunchedEffect(activeSection, activeLegalDocument, showFriendsScreen, showBackupRestoreScreen, showDesignSystemDebugScreen, showHomeRefinedShowcaseScreen, showDiagnosticScreen) {
        onSubScreenStateChanged?.invoke(activeSection != null || activeLegalDocument != null || showFriendsScreen || showBackupRestoreScreen || showDesignSystemDebugScreen || showHomeRefinedShowcaseScreen || showDiagnosticScreen)
    }

    BackHandler(enabled = activeLegalDocument != null || showFriendsScreen || showBackupRestoreScreen || showDesignSystemDebugScreen || showHomeRefinedShowcaseScreen || showDiagnosticScreen || activeSection != null) {
        if (activeLegalDocument != null) {
            activeLegalDocument = null
        } else if (showFriendsScreen) {
            showFriendsScreen = false
        } else if (showBackupRestoreScreen) {
            showBackupRestoreScreen = false
        } else if (showDesignSystemDebugScreen) {
            showDesignSystemDebugScreen = false
        } else if (showHomeRefinedShowcaseScreen) {
            showHomeRefinedShowcaseScreen = false
        } else if (showDiagnosticScreen) {
            showDiagnosticScreen = false
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

    if (showFriendsScreen) {
        FriendsScreen(
            viewModel = friendsViewModel,
            onBackClick = { showFriendsScreen = false },
            userProfile = uiState.userProfile,
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    if (showBackupRestoreScreen) {
        BackupRestoreScreen(
            onBack = { showBackupRestoreScreen = false },
            spaceRepository = spaceRepository,
            workspaceRepository = workspaceRepository,
            onDataRestored = { viewModel.refreshStorageBreakdown() }
        )
        return
    }

    if (showDesignSystemDebugScreen) {
        DesignSystemDebugScreen(
            onBack = { showDesignSystemDebugScreen = false },
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    if (showHomeRefinedShowcaseScreen) {
        com.arinara.fotara.ui.showcase.HomeRefinedShowcaseScreen(
            onBack = { showHomeRefinedShowcaseScreen = false },
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    if (showDiagnosticScreen) {
        com.arinara.fotara.diagnostic.DiagnosticInfoScreen(
            onBack = { showDiagnosticScreen = false },
            modifier = Modifier.fillMaxSize()
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
    var versionTapCount by remember { mutableIntStateOf(0) }
    val isDevModeEnabled by com.arinara.fotara.diagnostic.AppDiagnosticLogger.isDeveloperModeEnabled.collectAsState()
    var showGridDensityDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showOcrLanguageDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showLeadTimeDialog by remember { mutableStateOf(false) }
    var showStorageLocationDialog by remember { mutableStateOf(false) }
    var showCombineFileNameDialog by remember { mutableStateOf(false) }
    var showSavedImageLocationDialog by remember { mutableStateOf(false) }
    var showAntiProcrastinationScreen by remember { mutableStateOf(false) }

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

    LaunchedEffect(activeSection) {
        if (activeSection == SettingsSection.STORAGE) {
            viewModel.refreshStorageBreakdown()
            viewModel.refreshAppResidue()
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

    if (showAntiProcrastinationScreen) {
        BackHandler { showAntiProcrastinationScreen = false }
        com.arinara.fotara.alarm.procrastination.ui.AntiProcrastinationAlarmScreen(
            onBackClick = { showAntiProcrastinationScreen = false },
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    if (showBannerSourceDialog) {
        BannerSourceDialog(
            onDismiss = { showBannerSourceDialog = false },
            onPickImage = {
                showBannerSourceDialog = false
                bannerPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onPickGif = {
                showBannerSourceDialog = false
                bannerGifPickerLauncher.launch(arrayOf("image/gif"))
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

    val effectiveBottomPadding = if (bottomOverlayPadding >= 100.dp) bottomOverlayPadding else 100.dp

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .padding(bottom = effectiveBottomPadding + 8.dp)
                    .imePadding()
            )
        },
        containerColor = ElectricSlateTokens.BgCanvas,
        modifier = modifier
    ) { innerPadding ->
        AnimatedContent(
            targetState = activeSection,
            transitionSpec = {
                fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(180))
            },
            label = "SettingsSectionTransition"
        ) { targetSection ->
            if (targetSection == SettingsSection.PROFILE) {
                ProfileScreen(
                    viewModel = viewModel,
                    onBackClick = { activeSection = null },
                    modifier = Modifier.fillMaxSize()
                )
            } else if (targetSection == null) {
            val rootScrollState = scrollState ?: rememberScrollState()
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
                            color = ElectricSlateTokens.TextPrimary,
                            fontSize = 22.sp,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState.userProfile.email.isNotBlank()) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = uiState.userProfile.email,
                                color = ElectricSlateTokens.TextMuted,
                                fontSize = 14.sp,
                                fontFamily = ElmsSans
                            )
                        }
                    }

                    // Category Cards with Friends strictly below Profile and above General
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                    ) {
                        SettingsCardItem(
                            title = SettingsSection.PROFILE.title,
                            subtitle = SettingsSection.PROFILE.subtitle,
                            icon = SettingsSection.PROFILE.icon,
                            onClick = { activeSection = SettingsSection.PROFILE }
                        )
                        FriendsSettingsCard(
                            activeFriendsCount = friendsUiState.activeFriendsCount,
                            totalFriendsCount = friendsUiState.totalFriendsCount,
                            onClick = { showFriendsScreen = true }
                        )
                        SettingsSection.entries.filter { it != SettingsSection.PROFILE }.forEach { section ->
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
                                        ElectricSlateTokens.BgCanvas,
                                        ElectricSlateTokens.BgCanvas,
                                        ElectricSlateTokens.BgCanvas.copy(alpha = 0.95f),
                                        ElectricSlateTokens.BgCanvas.copy(alpha = 0.5f),
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
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + contentPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + contentPadding.calculateBottomPadding() + effectiveBottomPadding + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Header of Active Section with Back Arrow to Root
                item {
                    SettingsSubScreenHeader(
                        title = targetSection.title,
                        onBackClick = { activeSection = null }
                    )
                }

                // Clean Un-carded List Rows inside Selected Section
                when (targetSection) {
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
                        item { SettingsListDivider() }
                        item {
                            SettingsRowItem(
                                title = "Study Alarms & Anti-Procrastination",
                                subtitle = "Photo-proof challenge alarms (Temporarily Disabled)",
                                icon = Icons.Default.Alarm,
                                iconTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                enabled = false,
                                onClick = {}
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
                            SettingsAppResidueCard(
                                formattedSize = uiState.appResidueInfo.formattedSize,
                                fileCount = uiState.appResidueInfo.fileCount,
                                isCleaning = uiState.isCleaningResidue,
                                progress = uiState.residueCleanProgress,
                                onCleanClick = { viewModel.cleanAppResidue() }
                            )
                        }
                        item { SettingsListDivider() }
                        item {
                            SettingsRowItem(
                                title = "Trash / Recycle Bin",
                                subtitle = "View and restore deleted folders and notes",
                                icon = Icons.Default.Delete,
                                iconTint = ElectricSlateTokens.Destructive,
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
                                title = "Backup & Restore (.fotara)",
                                subtitle = "Comprehensive archive export and restore with SAF and integrity verification",
                                icon = Icons.Default.Archive,
                                actionIcon = Icons.AutoMirrored.Filled.ArrowForward,
                                isLoading = false,
                                onClick = { showBackupRestoreScreen = true }
                            )
                        }
                    }
                    SettingsSection.ABOUT -> {
                        item {
                            SettingsAboutCard(
                                versionName = appVersionName,
                                versionCode = appVersionCode,
                                onVersionTap = {
                                    versionTapCount++
                                    if (versionTapCount == 7) {
                                        com.arinara.fotara.diagnostic.AppDiagnosticLogger.setDeveloperModeEnabled(context, true)
                                        android.widget.Toast.makeText(context, "Developer Mode Unlocked!", android.widget.Toast.LENGTH_SHORT).show()
                                    } else if (versionTapCount in 4..6) {
                                        android.widget.Toast.makeText(context, "${7 - versionTapCount} taps away from Developer Mode", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                        item { SettingsListDivider() }
                        item {
                            SettingsRowItem(
                                title = "Diagnostic & Performance Info",
                                subtitle = "Hardware specs, startup breakdown, activity logs & ticker",
                                icon = Icons.Default.Speed,
                                onClick = { showDiagnosticScreen = true }
                            )
                        }
                        item { SettingsListDivider() }
                        item {
                            SettingsRowItem(
                                title = "What's New in Fotara",
                                subtitle = "Release notes, changelogs, and recent features",
                                icon = Icons.Default.NewReleases,
                                onClick = { onOpenWhatsNew?.invoke() }
                            )
                        }
                        item { SettingsListDivider() }
                        item {
                            SettingsRowItem(
                                title = "App Updates & Version Rollback",
                                subtitle = "Check for updates or rollback to a previous version",
                                icon = Icons.Default.SystemUpdate,
                                onClick = { onOpenUpdateScreen?.invoke() }
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
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
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
                        item { SettingsListDivider() }
                        item {
                            Text(
                                text = "DEVELOPER / ADVANCED",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 4.dp)
                            )
                        }
                        item {
                            SettingsRowToggle(
                                title = "Extreme Developer Mode",
                                subtitle = "Unlocks live RAM graph, worker memory delta tracking & leak hints",
                                icon = Icons.Default.Speed,
                                checked = isDevModeEnabled,
                                onCheckedChange = { com.arinara.fotara.diagnostic.AppDiagnosticLogger.setDeveloperModeEnabled(context, it) }
                            )
                        }
                        item {
                            SettingsRowItem(
                                title = "Design System Showcase",
                                subtitle = "Interactive preview of all Electric Slate UI presets",
                                icon = Icons.Default.Palette,
                                onClick = { showDesignSystemDebugScreen = true }
                            )
                        }
                        item {
                            SettingsRowItem(
                                title = "Refined Home UI Replica",
                                subtitle = "Full interactive preview of Home screen with 10 presets",
                                icon = Icons.Default.GridView,
                                onClick = { showHomeRefinedShowcaseScreen = true }
                            )
                        }
                        item {
                            SettingsRowItem(
                                title = "Diagnostic & Performance Info",
                                subtitle = "Hardware specs, startup breakdown, activity logs & ticker",
                                icon = Icons.Default.Speed,
                                onClick = { showDiagnosticScreen = true }
                            )
                        }
                    }
                }
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

    // Open Source Licenses Dialog
    if (uiState.showLicensesDialog) {
        OpenSourceLicensesDialog(
            onDismiss = { viewModel.setLicensesDialogVisible(false) }
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
    iconTint: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    AppCard(
        cornerRadius = 24.dp,
        containerColor = ElectricSlateTokens.SurfacePanel,
        borderColor = ElectricSlateTokens.BorderSubtle,
        contentPadding = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        AppListItem(
            title = title,
            subtitle = subtitle,
            leadingIcon = icon,
            leadingIconTint = if (iconTint != Color.Unspecified) iconTint else ElectricSlateTokens.PrimaryAccent,
            action = AppListItemAction.Chevron,
            onClick = onClick
        )
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
    AppCard(
        cornerRadius = 24.dp,
        containerColor = ElectricSlateTokens.SurfacePanel,
        borderColor = ElectricSlateTokens.BorderSubtle,
        contentPadding = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        AppListItem(
            title = title,
            subtitle = subtitle,
            leadingIcon = icon,
            leadingIconTint = ElectricSlateTokens.PrimaryAccent,
            action = AppListItemAction.Toggle(checked = checked, onCheckedChange = onCheckedChange),
            onClick = { onCheckedChange(!checked) }
        )
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
    AppCard(
        cornerRadius = 24.dp,
        containerColor = ElectricSlateTokens.SurfacePanel,
        borderColor = ElectricSlateTokens.BorderSubtle,
        contentPadding = 0.dp,
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
                    .background(ElectricSlateTokens.SurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ElectricSlateTokens.TextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = ElectricSlateTokens.TextPrimary,
                    fontSize = 16.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Medium
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = ElectricSlateTokens.TextMuted,
                        fontSize = 12.sp,
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
                    color = ElectricSlateTokens.PrimaryAccent,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = title,
                        tint = ElectricSlateTokens.TextMuted,
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
    AppCard(
        cornerRadius = 24.dp,
        containerColor = ElectricSlateTokens.SurfacePanel,
        borderColor = ElectricSlateTokens.BorderSubtle,
        contentPadding = 16.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElectricSlateTokens.SurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = ElectricSlateTokens.TextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Storage Usage Breakdown",
                        color = ElectricSlateTokens.TextPrimary,
                        fontSize = 16.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Total Fotara Data: $formattedTotal",
                        color = ElectricSlateTokens.TextMuted,
                        fontSize = 12.sp,
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
            .background(ElectricSlateTokens.SurfaceElevated, RoundedCornerShape(12.dp))
            .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
            .padding(horizontal = FotaraSpacing.Small, vertical = FotaraSpacing.Small)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, color = ElectricSlateTokens.TextMuted, fontSize = 11.sp, fontFamily = ElmsSans, fontWeight = FontWeight.Light)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = size, color = ElectricSlateTokens.PrimaryAccent, fontSize = 14.sp, fontFamily = ElmsSans, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Dedicated storage cleaner card for disposable application residue (leftover update APKs,
 * partial download files, share staging files, temporary document exports, and cache files).
 * Coursework notes, photos, and SQLite databases are strictly excluded.
 */
@Composable
private fun SettingsAppResidueCard(
    formattedSize: String,
    fileCount: Int,
    isCleaning: Boolean,
    progress: Float,
    onCleanClick: () -> Unit
) {
    AppCard(
        cornerRadius = 24.dp,
        containerColor = ElectricSlateTokens.SurfacePanel,
        borderColor = ElectricSlateTokens.BorderSubtle,
        contentPadding = 16.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElectricSlateTokens.SurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = ElectricSlateTokens.PrimaryAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "App Trash & Temporary Residue",
                        color = ElectricSlateTokens.TextPrimary,
                        fontSize = 16.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Leftover APKs, share staging, and cache residue (coursework notes and photos are preserved)",
                        color = ElectricSlateTokens.TextMuted,
                        fontSize = 12.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Light,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Total disk used indicator row
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ElectricSlateTokens.SurfaceElevated,
                border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Disk Used",
                        color = ElectricSlateTokens.TextMuted,
                        fontSize = 12.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (fileCount > 0) "$formattedSize ($fileCount ${if (fileCount == 1) "file" else "files"})" else "$formattedSize (0 files)",
                        color = if (fileCount > 0) ElectricSlateTokens.TextPrimary else ElectricSlateTokens.TextMuted,
                        fontSize = 14.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (isCleaning) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Cleaning temporary residue...",
                            color = ElectricSlateTokens.PrimaryAccent,
                            fontSize = 12.sp,
                            fontFamily = ElmsSans
                        )
                        Text(
                            text = "${(progress * 100).toInt().coerceIn(0, 100)}%",
                            color = ElectricSlateTokens.PrimaryAccent,
                            fontSize = 12.sp,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = ElectricSlateTokens.PrimaryAccent,
                        trackColor = ElectricSlateTokens.TrackInactive
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            AppButton(
                text = if (fileCount > 0) "Clean Residue Now" else "No Residue to Clean",
                onClick = onCleanClick,
                enabled = !isCleaning && fileCount > 0,
                variant = AppButtonVariant.PRIMARY,
                height = 44.dp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * About Fotara card with app version and legal notices.
 */
@Composable
private fun SettingsAboutCard(
    versionName: String,
    versionCode: Long,
    onVersionTap: () -> Unit = {}
) {
    AppCard(
        cornerRadius = 24.dp,
        containerColor = ElectricSlateTokens.SurfacePanel,
        borderColor = ElectricSlateTokens.BorderSubtle,
        contentPadding = 16.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onVersionTap)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ElectricSlateTokens.SurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = ElectricSlateTokens.TextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                val info = com.arinara.fotara.online.VersionInfo.parse(versionName)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Fotara ${info.displayVersion}",
                        color = ElectricSlateTokens.TextPrimary,
                        fontSize = 16.sp,
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
                    color = ElectricSlateTokens.PrimaryAccent,
                    fontSize = 12.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Specialized photo organization and on-device OCR for students.",
                    color = ElectricSlateTokens.TextMuted,
                    fontSize = 12.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Light,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Arinara Network • 100% Offline Architecture",
                    color = ElectricSlateTokens.TextMuted,
                    fontSize = 12.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Light
                )
            }
        }
    }
}

/**
 * Clean un-carded list item for settings inside an opened section.
 */
@Composable
private fun SettingsRowItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = Color.Unspecified,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    AppListItem(
        title = title,
        subtitle = subtitle,
        leadingIcon = icon,
        leadingIconTint = if (iconTint != Color.Unspecified) iconTint else ElectricSlateTokens.PrimaryAccent,
        action = if (enabled) AppListItemAction.Chevron else null,
        onClick = if (enabled) onClick else null
    )
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
    AppListItem(
        title = title,
        subtitle = subtitle,
        leadingIcon = icon,
        leadingIconTint = ElectricSlateTokens.PrimaryAccent,
        action = AppListItemAction.Toggle(checked = checked, onCheckedChange = onCheckedChange),
        onClick = { onCheckedChange(!checked) }
    )
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
                tint = ElectricSlateTokens.TextMuted,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 16.sp,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = ElectricSlateTokens.TextMuted,
                    fontSize = 12.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Light,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (isLoading) {
            CircularProgressIndicator(
                color = ElectricSlateTokens.PrimaryAccent,
                strokeWidth = 2.dp,
                modifier = Modifier.size(22.dp)
            )
        } else {
            IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = actionIcon,
                    contentDescription = title,
                    tint = ElectricSlateTokens.TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsListDivider() {
    HorizontalDivider(
        color = ElectricSlateTokens.BorderSubtle,
        thickness = 1.dp,
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
        color = ElectricSlateTokens.SurfaceElevated,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
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
                    tint = ElectricSlateTokens.TextMuted,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Anonymous Device Identifier",
                        color = ElectricSlateTokens.TextPrimary,
                        fontSize = 14.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (isExpanded) "Tap to collapse" else "Tap to view identifier",
                        color = ElectricSlateTokens.TextMuted,
                        fontSize = 12.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Light
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = ElectricSlateTokens.TextMuted,
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
                        color = ElectricSlateTokens.BorderSubtle,
                        thickness = 1.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        color = ElectricSlateTokens.BgCanvas,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = deviceId,
                            color = ElectricSlateTokens.TextPrimary,
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
                            color = ElectricSlateTokens.TextMuted,
                            fontSize = 11.sp,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Light,
                            lineHeight = 15.sp,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        )

                        AppButton(
                            text = "Copy",
                            onClick = { onCopy(deviceId) },
                            icon = Icons.Default.ContentCopy,
                            variant = AppButtonVariant.PRIMARY,
                            height = 36.dp
                        )
                    }
                }
            }
        }
    }
}

