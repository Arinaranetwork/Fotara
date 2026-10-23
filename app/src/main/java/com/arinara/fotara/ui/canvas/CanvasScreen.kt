// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.canvas

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.arinara.fotara.R
import com.arinara.fotara.canvas.model.StrokeBlendMode
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import com.arinara.fotara.ui.components.workspaceGroupedFolderItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import com.arinara.fotara.feature.collab.engine.CollabSessionEngine
import com.arinara.fotara.feature.collab.ui.CollabRoomJoinDialog
import com.arinara.fotara.feature.collab.ui.CollabSessionBar
import com.arinara.fotara.theme.ElmsSans
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arinara.fotara.canvas.model.CanvasBackgroundStyle
import com.arinara.fotara.canvas.render.CanvasDrawingView
import com.arinara.fotara.canvas.tool.CanvasToolType
import com.arinara.fotara.theme.DockSlatePill
import com.arinara.fotara.theme.FolderBodyBlue
import com.arinara.fotara.theme.HomeNearBlack
import com.arinara.fotara.theme.MidnightCardOutline
import com.arinara.fotara.theme.MidnightNavy
import com.arinara.fotara.theme.TextMuted
import com.arinara.fotara.theme.TextPrimary
import com.arinara.fotara.theme.TextSecondary
import com.arinara.fotara.ui.components.ScheduleNoteDialog
import com.arinara.fotara.ui.components.presets.AppButton
import com.arinara.fotara.ui.components.presets.AppButtonVariant
import com.arinara.fotara.ui.components.presets.DefaultBottomClearance
import com.arinara.fotara.ui.components.presets.AppFloatingBar
import com.arinara.fotara.ui.components.presets.AppFloatingBarItem
import com.arinara.fotara.ui.components.presets.AppFloatingBarCustomItem
import com.arinara.fotara.ui.components.presets.AppSlider
import com.arinara.fotara.ui.components.presets.ConnectedInspector
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import com.arinara.fotara.ui.theme.SpringPhysics
import com.arinara.fotara.ui.theme.jigglePressClickable
import com.arinara.fotara.ui.theme.jiggleRotationClickable
import com.arinara.fotara.util.ScheduleAlertType
import com.arinara.fotara.util.ScheduleNoteType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CanvasScreen(
    viewModel: CanvasViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Ensure changes are flushed on system Back or app pause/stop
    BackHandler {
        coroutineScope.launch {
            viewModel.flushSave()
            onBack()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                coroutineScope.launch {
                    viewModel.flushSave()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val realProfileName = remember(uiState.userProfile) {
        uiState.userProfile.resolvedName(defaultFallback = "Fotara User")
    }
    val realProfilePronouns = remember(uiState.userProfile) {
        uiState.userProfile.pronouns.trim()
    }
    val resolvedUserHandle = remember(uiState.myUserHandle, realProfileName) {
        uiState.myUserHandle.ifBlank { realProfileName }
    }
    var persistentCollabDeviceId by remember { mutableStateOf("") }
    var collabPrefsLoaded by remember { mutableStateOf(false) }
    val collabEngine = remember {
        CollabSessionEngine(
            initialLocalUserTag = realProfileName,
            initialLocalPronouns = realProfilePronouns,
            initialLocalUserHandle = resolvedUserHandle,
            enableLanTransport = true
        )
    }
    LaunchedEffect(collabEngine) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val prefs = context.getSharedPreferences("fotara_collab_prefs", android.content.Context.MODE_PRIVATE)
            val deviceId = prefs.getString("collab_device_id", null)
                ?: java.util.UUID.randomUUID().toString().also { generated ->
                    prefs.edit().putString("collab_device_id", generated).apply()
                }
            val rawPresets = prefs.getString("collab_privacy_presets", null).orEmpty()
            val decodedPresets = if (rawPresets.isNotBlank()) {
                com.arinara.fotara.feature.collab.model.CollabPrivacyPreset.decodeList(rawPresets)
            } else {
                emptyList()
            }
            persistentCollabDeviceId = deviceId
            if (decodedPresets.isNotEmpty()) {
                collabEngine.loadSavedPrivacyPresets(decodedPresets)
            }
            collabPrefsLoaded = true
        }
    }
    LaunchedEffect(realProfileName, realProfilePronouns, persistentCollabDeviceId, resolvedUserHandle) {
        collabEngine.syncUserProfile(
            realName = realProfileName,
            pronouns = realProfilePronouns,
            deviceId = persistentCollabDeviceId,
            userHandle = resolvedUserHandle
        )
    }
    LaunchedEffect(uiState.friends) {
        val friendIdentities = uiState.friends.flatMap { friend ->
            listOf(friend.handle, friend.displayName).filter { it.isNotBlank() }
        }
        collabEngine.syncHostFriends(friendIdentities)
    }
    DisposableEffect(collabEngine) {
        onDispose {
            collabEngine.close()
        }
    }
    val collabRoomCode by collabEngine.roomCode.collectAsStateWithLifecycle()
    val collabPeersMap by collabEngine.activePeers.collectAsStateWithLifecycle()
    val collabPeers = remember(collabPeersMap) { collabPeersMap.values.toList() }
    val collabChatMessages by collabEngine.chatMessages.collectAsStateWithLifecycle()
    val collabUnreadChatCount by collabEngine.unreadChatCount.collectAsStateWithLifecycle()
    val collabLocalRole by collabEngine.localRole.collectAsStateWithLifecycle()
    val collabPinnedMessage by collabEngine.pinnedChatMessage.collectAsStateWithLifecycle()
    val collabIsChatBlocked by collabEngine.isLocalUserChatBlocked.collectAsStateWithLifecycle()
    val collabCanDraw by collabEngine.canLocalUserDraw.collectAsStateWithLifecycle()
    val collabCanCreateSheets by collabEngine.canLocalUserCreateSheets.collectAsStateWithLifecycle()
    val collabPeerVolumes by collabEngine.peerVolumes.collectAsStateWithLifecycle()
    val collabLocallyMutedPeers by collabEngine.locallyMutedPeerIds.collectAsStateWithLifecycle()
    val collabAudioBitrate by collabEngine.audioBitrateKbps.collectAsStateWithLifecycle()
    val collabRoomMaxBitrate by collabEngine.roomMaxBitrateKbps.collectAsStateWithLifecycle()
    val collabAccessMode by collabEngine.accessMode.collectAsStateWithLifecycle()
    val collabIsGlobalDrawingDisabled by collabEngine.isGlobalDrawingDisabled.collectAsStateWithLifecycle()
    val collabIsGlobalSheetDisabled by collabEngine.isGlobalSheetCreationDisabled.collectAsStateWithLifecycle()
    val collabMaxParticipants by collabEngine.maxParticipants.collectAsStateWithLifecycle()
    val collabJoinPolicy by collabEngine.joinPolicy.collectAsStateWithLifecycle()
    val collabAllowedHandles by collabEngine.allowedUserHandles.collectAsStateWithLifecycle()
    val collabInvitedHandles by collabEngine.invitedUserHandles.collectAsStateWithLifecycle()
    val collabSavedPresets by collabEngine.savedPrivacyPresets.collectAsStateWithLifecycle()
    val collabActivePresetId by collabEngine.activePresetId.collectAsStateWithLifecycle()
    val collabBufferedStrokesCount by collabEngine.bufferedStrokesCount.collectAsStateWithLifecycle()
    val collabErrorMessage by collabEngine.lastErrorMessage.collectAsStateWithLifecycle()
    LaunchedEffect(collabSavedPresets, collabPrefsLoaded) {
        if (!collabPrefsLoaded) return@LaunchedEffect
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val prefs = context.getSharedPreferences("fotara_collab_prefs", android.content.Context.MODE_PRIVATE)
            val encoded = com.arinara.fotara.feature.collab.model.CollabPrivacyPreset.encodeList(collabSavedPresets)
            prefs.edit().putString("collab_privacy_presets", encoded).apply()
        }
    }
    LaunchedEffect(collabBufferedStrokesCount) {
        if (collabBufferedStrokesCount > 0) {
            val remoteStrokes = collabEngine.consumeBufferedStrokes()
            if (remoteStrokes.isNotEmpty()) {
                viewModel.applyRemoteCollabStrokes(remoteStrokes)
            }
        }
    }
    var showCollabJoinDialog by remember { mutableStateOf(false) }
    var isJoiningCollabRoom by remember { mutableStateOf(false) }
    var showCollabChatSheet by remember { mutableStateOf(false) }
    var showCollabSettingsSheet by remember { mutableStateOf(false) }
    LaunchedEffect(showCollabChatSheet) {
        collabEngine.setChatSheetOpen(showCollabChatSheet)
    }
    LaunchedEffect(collabRoomCode) {
        if (collabRoomCode == null) {
            showCollabChatSheet = false
            showCollabSettingsSheet = false
        }
    }
    var drawingViewRef by remember { mutableStateOf<CanvasDrawingView?>(null) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf("") }
    var showVfxBottomSheet by remember { mutableStateOf(false) }
    var activeVfxImageElement by remember { mutableStateOf<com.arinara.fotara.canvas.model.ImageElement?>(null) }
    var showMoveElementsLayerPicker by remember { mutableStateOf(false) }
    var showArrangeMenu by remember { mutableStateOf(false) }
    var showSecondaryTools by remember { mutableStateOf(false) }
    var topCapsulesHeightPx by remember { mutableIntStateOf(0) }
    var dockHeightPx by remember { mutableIntStateOf(0) }
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    // System Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val v = drawingViewRef
            if (v != null) {
                viewModel.addImage(uri, v.viewport, v.width.toFloat(), v.height.toFloat())
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ElectricSlateTokens.BgCanvas)
    ) {
        // ==========================================
        // Z6: Canvas Area (Native Hardware Viewport)
        // ==========================================
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                CanvasDrawingView(
                    context = ctx,
                    tileCacheManager = viewModel.tileCacheManager,
                    canvasRenderer = viewModel.canvasRenderer,
                    pointerStateMachine = viewModel.pointerStateMachine,
                    toolController = viewModel.toolController,
                    historyManager = viewModel.historyManager,
                    spatialIndex = viewModel.spatialIndex,
                    onDocumentChanged = { updatedDoc, dirtyRect ->
                        if (collabRoomCode == null || collabCanDraw) {
                            val prevElements = uiState.document.elements
                            viewModel.onDocumentModified(updatedDoc, dirtyRect)
                            if (collabRoomCode != null && updatedDoc.elements.size > prevElements.size) {
                                val prevIds = prevElements.mapTo(HashSet(prevElements.size)) { it.id }
                                for (el in updatedDoc.elements) {
                                    if (el is com.arinara.fotara.canvas.model.StrokeElement && el.id !in prevIds && el.points.isNotEmpty()) {
                                        val hexColor = String.format("#%06X", (el.color and 0xFFFFFFL))
                                        val flatPoints = ArrayList<Float>(el.points.size * 2)
                                        for (pt in el.points) {
                                            flatPoints.add(pt.x)
                                            flatPoints.add(pt.y)
                                        }
                                        collabEngine.broadcastStroke(
                                            points = flatPoints,
                                            colorHex = hexColor,
                                            strokeWidth = el.width,
                                            strokeId = el.id
                                        )
                                        val lastPt = el.points.last()
                                        collabEngine.updateCursor(lastPt.x, lastPt.y)
                                    }
                                }
                            }
                        }
                    }
                ).apply {
                    onViewportChanged = { v ->
                        viewModel.setZoomPercentage(v.scale)
                    }
                    onSelectionChanged = { sel ->
                        viewModel.updateSelection(sel)
                    }
                    drawingViewRef = this
                }
            },
            update = { view ->
                if (view.documentSnapshot != uiState.document) {
                    view.documentSnapshot = uiState.document
                }
                if (view.activeLayerId != uiState.activeLayerId) {
                    view.activeLayerId = uiState.activeLayerId
                }
                drawingViewRef = view
            }
        )

        if (collabRoomCode != null && collabPeers.size > 1) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        drawContent()
                        com.arinara.fotara.feature.collab.render.PeerCursorRenderer.renderPeerCursors(
                            drawScope = this,
                            peers = collabPeers,
                            localPeerId = collabEngine.localPeerId,
                            viewportWidth = size.width,
                            viewportHeight = size.height
                        )
                    }
            )
        }

        // ==========================================
        // Z1 & Z2: Top Unified Responsive Header Row
        // ==========================================
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .zIndex(10f)
        ) {
            if (collabRoomCode != null) {
                CollabSessionBar(
                    roomCode = collabRoomCode!!,
                    peers = collabPeers,
                    onLeaveClick = {
                        showCollabChatSheet = false
                        showCollabSettingsSheet = false
                        collabEngine.leaveRoom()
                    },
                    localRole = collabLocalRole,
                    canLocalUserDraw = collabCanDraw,
                    unreadChatCount = collabUnreadChatCount,
                    onChatClick = { showCollabChatSheet = true },
                    onSettingsClick = { showCollabSettingsSheet = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp)
                    .onSizeChanged { topCapsulesHeightPx = it.height },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
            // Left: Back button + Title/Save capsule
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .jiggleRotationClickable {
                            coroutineScope.launch {
                                viewModel.flushSave()
                                onBack()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = ElectricSlateTokens.TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    color = ElectricSlateTokens.SurfacePanel,
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
                    modifier = Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .jigglePressClickable {
                            if (uiState.saveState == SaveState.ERROR) {
                                viewModel.retrySave()
                            } else {
                                renameInput = uiState.title
                                viewModel.setRenameDialogVisible(true)
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = uiState.title,
                                    color = ElectricSlateTokens.TextPrimary,
                                    fontSize = 14.sp,
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Options",
                                    tint = ElectricSlateTokens.TextPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                when (uiState.saveState) {
                                    SaveState.SAVING -> {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(10.dp),
                                            strokeWidth = 1.5.dp,
                                            color = ElectricSlateTokens.PrimaryAccent
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Saving...",
                                            color = ElectricSlateTokens.TextMuted,
                                            fontSize = 10.sp,
                                            fontFamily = ElmsSans
                                        )
                                    }
                                    SaveState.SAVED -> {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Saved",
                                            tint = ElectricSlateTokens.TextMuted,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Saved",
                                            color = ElectricSlateTokens.TextMuted,
                                            fontSize = 10.sp,
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    SaveState.ERROR -> {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Retry Save",
                                            tint = ElectricSlateTokens.Destructive,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Save error",
                                            color = ElectricSlateTokens.Destructive,
                                            fontSize = 10.sp,
                                            fontFamily = ElmsSans
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right: Operations capsule (Undo, Redo, Layers, Add Image, Overflow Menu)
            Surface(
                color = ElectricSlateTokens.SurfacePanel,
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
                modifier = Modifier.height(44.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .jigglePressClickable(enabled = uiState.canUndo) { viewModel.undo() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (uiState.canUndo) ElectricSlateTokens.TextPrimary else ElectricSlateTokens.TextMuted.copy(alpha = 0.35f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .jigglePressClickable(enabled = uiState.canRedo) { viewModel.redo() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (uiState.canRedo) ElectricSlateTokens.TextPrimary else ElectricSlateTokens.TextMuted.copy(alpha = 0.35f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .jigglePressClickable { viewModel.setLayersPanelVisible(true) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Layers",
                            tint = if (uiState.showLayersPanel) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .jigglePressClickable { viewModel.setImageSourceDialogVisible(true) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Add Image",
                            tint = ElectricSlateTokens.TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .jigglePressClickable { showOverflowMenu = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = ElectricSlateTokens.TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false },
                            modifier = Modifier
                                .background(ElectricSlateTokens.SurfaceElevated)
                                .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Share Note", color = ElectricSlateTokens.TextPrimary) },
                                onClick = {
                                    showOverflowMenu = false
                                    showShareSheet = true
                                },
                                leadingIcon = { Icon(Icons.Default.Share, null, tint = ElectricSlateTokens.TextPrimary) }
                            )
                            DropdownMenuItem(
                                text = { Text("Canvas Settings", color = ElectricSlateTokens.TextPrimary) },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.setSettingsDialogVisible(true)
                                },
                                leadingIcon = { Icon(Icons.Default.Settings, null, tint = ElectricSlateTokens.TextPrimary) }
                            )
                            DropdownMenuItem(
                                text = { Text("Rename Note", color = ElectricSlateTokens.TextPrimary) },
                                onClick = {
                                    showOverflowMenu = false
                                    renameInput = uiState.title
                                    viewModel.setRenameDialogVisible(true)
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, null, tint = ElectricSlateTokens.TextMuted) }
                            )
                            DropdownMenuItem(
                                text = { Text("Move to Folder", color = ElectricSlateTokens.TextPrimary) },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.setMoveDialogVisible(true)
                                },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.DriveFileMove, null, tint = ElectricSlateTokens.TextMuted) }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (collabRoomCode != null) "Active Room ($collabRoomCode)" else "Collaborative Study Room",
                                        color = if (collabRoomCode != null) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TextPrimary
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    showCollabJoinDialog = true
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Group,
                                        null,
                                        tint = if (collabRoomCode != null) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TextMuted
                                    )
                                }
                            )
                            if (collabRoomCode != null) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (collabUnreadChatCount > 0) "Room Chat ($collabUnreadChatCount new)" else "Room Chat",
                                            color = ElectricSlateTokens.TextPrimary
                                        )
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        showCollabChatSheet = true
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Group,
                                            null,
                                            tint = ElectricSlateTokens.TextMuted
                                        )
                                    }
                                )
                            }
                            HorizontalDivider(color = ElectricSlateTokens.BorderSubtle)
                            DropdownMenuItem(
                                text = { Text("Delete Note", color = ElectricSlateTokens.Destructive) },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.setDeleteConfirmDialogVisible(true)
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = ElectricSlateTokens.Destructive) }
                            )
                        }
                    }
                }
            }
        }
    }

        // ==========================================
        // Z4: Connected Tool Inspector (Anchored Directly Above Z3 Dock)
        // ==========================================
        val dockHeightDp = with(density) { dockHeightPx.toDp() }
        val baseDockHeight = if (dockHeightDp.value > 0f) dockHeightDp.value else 54f
        val targetZ4Padding = (16f + baseDockHeight - 1f).dp

        val neckRatio = when (uiState.toolState.activeTool) {
            CanvasToolType.SELECT -> 0.14f
            CanvasToolType.PEN -> 0.26f
            CanvasToolType.HIGHLIGHTER -> 0.38f
            CanvasToolType.ERASER -> 0.50f
            CanvasToolType.TEXT -> 0.62f
            CanvasToolType.SHAPE -> 0.74f
        }

        ConnectedInspector(
            visible = uiState.showToolOptions,
            neckAnchorOffsetRatio = neckRatio,
            panelWidth = 320.dp,
            containerColor = ElectricSlateTokens.SurfaceElevated,
            borderColor = ElectricSlateTokens.BorderSubtle,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = targetZ4Padding)
                .zIndex(12f)
        ) {
            // Drag handle pill
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(32.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(ElectricSlateTokens.BorderSubtle)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Title and Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (uiState.toolState.activeTool) {
                        CanvasToolType.PEN -> stringResource(R.string.canvas_tool_pen)
                        CanvasToolType.HIGHLIGHTER -> stringResource(R.string.canvas_tool_highlighter)
                        CanvasToolType.ERASER -> stringResource(R.string.canvas_tool_eraser)
                        CanvasToolType.SELECT -> stringResource(R.string.canvas_tool_select)
                        CanvasToolType.TEXT -> "Text Layer"
                        CanvasToolType.SHAPE -> "Shapes & Smooth"
                    },
                    color = ElectricSlateTokens.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .jigglePressClickable { viewModel.toggleToolOptions() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Close",
                        tint = ElectricSlateTokens.TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (uiState.toolState.activeTool) {
                CanvasToolType.PEN, CanvasToolType.HIGHLIGHTER -> {
                    val isHighlighter = uiState.toolState.activeTool == CanvasToolType.HIGHLIGHTER
                    val sizeValue = if (isHighlighter) uiState.toolState.highlighterSize else uiState.toolState.penSize
                    val alphaValue = if (isHighlighter) 0.5f else uiState.toolState.penAlpha

                    // 1. Size Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Size", color = ElectricSlateTokens.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text("${sizeValue.toInt()} pt", color = ElectricSlateTokens.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    AppSlider(
                        value = sizeValue,
                        onValueChange = {
                            if (isHighlighter) viewModel.setHighlighterSize(it) else viewModel.setPenSize(it)
                        },
                        valueRange = if (isHighlighter) 10f..60f else 2f..32f,
                        steps = 4,
                        activeTrackColor = ElectricSlateTokens.PrimaryAccent,
                        inactiveTrackColor = ElectricSlateTokens.TrackInactive
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // 2. Opacity Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Opacity", color = ElectricSlateTokens.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text("${kotlin.math.round(alphaValue * 100).toInt()}%", color = ElectricSlateTokens.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    AppSlider(
                        value = alphaValue,
                        onValueChange = {
                            if (!isHighlighter) viewModel.setPenAlpha(it)
                        },
                        valueRange = 0.1f..1.0f,
                        steps = 4,
                        enabled = !isHighlighter,
                        activeTrackColor = ElectricSlateTokens.PrimaryAccent,
                        inactiveTrackColor = ElectricSlateTokens.TrackInactive
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3. Curated Color Swatches
                    Text("Palette", color = ElectricSlateTokens.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    val activePalette = uiState.recentColors.ifEmpty {
                        listOf(
                            0xFFF8FAFC, // Off-white / Cream
                            0xFF283245, // Slate Charcoal
                            0xFFEF4444, // Red
                            0xFFF97316, // Orange
                            0xFFFBBF24, // Amber Yellow
                            0xFF10B981, // Emerald Green
                            0xFF3B82F6, // Electric Blue
                            0xFF7A5AF8  // Primary Accent (Electric Violet)
                        )
                    }
                    var showExtendedPalette by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        activePalette.take(7).forEach { colorLong ->
                            val isSelected = if (isHighlighter) {
                                uiState.toolState.highlighterColor == colorLong
                            } else {
                                uiState.toolState.penColor == colorLong
                            }
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .then(
                                        if (isSelected) {
                                            Modifier
                                                .border(2.dp, ElectricSlateTokens.PrimaryAccent, CircleShape)
                                                .padding(2.5.dp)
                                        } else {
                                            Modifier.padding(2.5.dp)
                                        }
                                    )
                                    .clip(CircleShape)
                                    .background(Color(colorLong.toInt()))
                                    .border(0.5.dp, ElectricSlateTokens.BorderSubtle, CircleShape)
                                    .jigglePressClickable {
                                        if (isHighlighter) {
                                            viewModel.setHighlighterColor(colorLong)
                                        } else {
                                            viewModel.setPenColor(colorLong)
                                        }
                                    }
                            )
                        }

                        // Trailing '+' Toggle Extended Palette Button
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .border(1.dp, if (showExtendedPalette) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.BorderSubtle, CircleShape)
                                .jigglePressClickable {
                                    showExtendedPalette = !showExtendedPalette
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "More Colors",
                                tint = if (showExtendedPalette) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (showExtendedPalette) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val extendedColors = listOf(
                            0xFF7A5AF8, // Electric Violet
                            0xFFA855F7, // Purple
                            0xFFEC4899, // Pink
                            0xFF06B6D4, // Cyan
                            0xFF14B8A6, // Teal
                            0xFF84CC16, // Lime
                            0xFFEAB308  // Gold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            extendedColors.forEach { colorLong ->
                                val isSelected = if (isHighlighter) {
                                    uiState.toolState.highlighterColor == colorLong
                                } else {
                                    uiState.toolState.penColor == colorLong
                                }
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .then(
                                            if (isSelected) {
                                                Modifier
                                                    .border(2.dp, ElectricSlateTokens.PrimaryAccent, CircleShape)
                                                    .padding(2.5.dp)
                                            } else {
                                                Modifier.padding(2.5.dp)
                                            }
                                        )
                                        .clip(CircleShape)
                                        .background(Color(colorLong.toInt()))
                                        .border(0.5.dp, ElectricSlateTokens.BorderSubtle, CircleShape)
                                        .jigglePressClickable {
                                            if (isHighlighter) {
                                                viewModel.setHighlighterColor(colorLong)
                                            } else {
                                                viewModel.setPenColor(colorLong)
                                            }
                                        }
                                )
                            }
                            Spacer(modifier = Modifier.size(30.dp))
                        }
                    }

                    // Auto-Smoothen & Shape Snapping toggles for Pen
                    if (!isHighlighter) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Auto-Smoothen Strokes", color = ElectricSlateTokens.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text("Smooth handwriting & organic lines", color = ElectricSlateTokens.TextMuted, fontSize = 10.sp)
                            }
                            Switch(
                                checked = uiState.toolState.autoSmoothen,
                                onCheckedChange = { viewModel.setAutoSmoothen(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = ElectricSlateTokens.PrimaryAccent,
                                    uncheckedThumbColor = ElectricSlateTokens.TextMuted,
                                    uncheckedTrackColor = ElectricSlateTokens.SurfacePanel
                                )
                            )
                        }
                    }

                    // Blending Mode Row for Highlighter
                    if (isHighlighter) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.canvas_blend_mode),
                            color = ElectricSlateTokens.TextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val currentBlend = uiState.toolState.highlighterBlendMode
                            val modes = listOf(
                                StrokeBlendMode.NORMAL to R.string.canvas_blend_normal,
                                StrokeBlendMode.MULTIPLY to R.string.canvas_blend_multiply,
                                StrokeBlendMode.DARKEN to R.string.canvas_blend_darken,
                                StrokeBlendMode.SCREEN to R.string.canvas_blend_screen
                            )
                            for ((mode, strRes) in modes) {
                                val isSelected = currentBlend == mode
                                val modeLabel = stringResource(strRes)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.SurfacePanel)
                                        .border(
                                            1.dp,
                                            if (isSelected) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.BorderSubtle,
                                            CircleShape
                                        )
                                        .jigglePressClickable { viewModel.setHighlighterBlendMode(mode) }
                                        .semantics { contentDescription = modeLabel },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = modeLabel,
                                        color = if (isSelected) Color.White else ElectricSlateTokens.TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
                CanvasToolType.SHAPE -> {
                    Text("Geometric Shape", color = ElectricSlateTokens.TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val primitives = listOf(
                            com.arinara.fotara.canvas.tool.ShapePrimitiveType.RECTANGLE to "Rect",
                            com.arinara.fotara.canvas.tool.ShapePrimitiveType.CIRCLE to "Circle",
                            com.arinara.fotara.canvas.tool.ShapePrimitiveType.ARROW to "Arrow",
                            com.arinara.fotara.canvas.tool.ShapePrimitiveType.LINE to "Line",
                            com.arinara.fotara.canvas.tool.ShapePrimitiveType.AUTO to "Auto"
                        )
                        for ((prim, label) in primitives) {
                            val isSelected = uiState.toolState.activeShapePrimitive == prim
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.SurfacePanel)
                                    .border(1.dp, if (isSelected) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.BorderSubtle, CircleShape)
                                    .jigglePressClickable { viewModel.setShapePrimitive(prim) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    color = if (isSelected) Color.White else ElectricSlateTokens.TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Thickness", color = ElectricSlateTokens.TextSecondary, fontSize = 12.sp)
                        Text("${uiState.toolState.penSize.toInt()} pt", color = ElectricSlateTokens.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    AppSlider(
                        value = uiState.toolState.penSize,
                        onValueChange = { viewModel.setPenSize(it) },
                        valueRange = 2f..32f,
                        steps = 4,
                        activeTrackColor = ElectricSlateTokens.PrimaryAccent,
                        inactiveTrackColor = ElectricSlateTokens.TrackInactive
                    )
                }
                CanvasToolType.ERASER -> {
                    Text(stringResource(R.string.canvas_eraser_size), color = ElectricSlateTokens.TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    AppSlider(
                        value = uiState.toolState.eraserRadius,
                        onValueChange = { viewModel.setEraserRadius(it) },
                        valueRange = 10f..80f,
                        steps = 4,
                        activeTrackColor = ElectricSlateTokens.PrimaryAccent,
                        inactiveTrackColor = ElectricSlateTokens.TrackInactive
                    )
                }
                CanvasToolType.SELECT -> {
                    Text(
                        text = "Tap elements to select or drag a lasso loop to group-select. Use corner and rotation handles to scale and rotate.",
                        color = ElectricSlateTokens.TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
                CanvasToolType.TEXT -> {
                    Text(
                        text = "Tap anywhere on the canvas to place or edit a text annotation.",
                        color = ElectricSlateTokens.TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // ==========================================
        // Z2.5: Sheet Switcher Dock (Max 10 Sheets) & Z3 Bottom Floating Toolbar
        // ==========================================
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
                .onSizeChanged { dockHeightPx = it.height }
                .zIndex(10f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!uiState.isBottomDockCollapsed) {
                // Unified Row directly above Floating Toolbar: Zoom Chip (Left) + Multi-Sheet Switcher Dock (Right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Zoom percentage chip + Fit-to-content (aligned with left margin start = 16.dp)
                    Surface(
                        color = ElectricSlateTokens.SurfaceElevated,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${uiState.zoomPercentage}%",
                                color = ElectricSlateTokens.TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .jigglePressClickable { drawingViewRef?.resetZoom() }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .jigglePressClickable { drawingViewRef?.fitToContent() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FitScreen,
                                    contentDescription = "Fit to Content",
                                    tint = ElectricSlateTokens.TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Right: Sheet Switcher Dock (Permanently visible for sheets >= 1)
                    val resolvedSheets = uiState.document.getResolvedSheets()
                    Surface(
                        color = ElectricSlateTokens.SurfacePanel.copy(alpha = 0.94f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
                        shadowElevation = 8.dp,
                        modifier = Modifier.height(36.dp)
                    ) {
                        LazyRow(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            itemsIndexed(resolvedSheets) { idx, sheet ->
                                val isSheetActive = idx == uiState.document.activeSheetIndex
                                Row(
                                    modifier = Modifier
                                        .height(28.dp)
                                        .clip(CircleShape)
                                        .background(if (isSheetActive) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.SurfaceElevated)
                                        .border(1.dp, if (isSheetActive) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.BorderSubtle, CircleShape)
                                        .jigglePressClickable { viewModel.selectSheet(idx) }
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = sheet.title,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSheetActive) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSheetActive) Color.White else ElectricSlateTokens.TextSecondary
                                    )
                                    if (isSheetActive && resolvedSheets.size > 1) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .clickable { viewModel.deleteSheet(idx) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Delete Sheet",
                                                tint = Color.White.copy(alpha = 0.85f),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (resolvedSheets.size < com.arinara.fotara.canvas.model.CanvasDocument.MAX_SHEETS) {
                                item {
                                    val canAddNewSheet = collabRoomCode == null || collabCanCreateSheets
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(if (canAddNewSheet) ElectricSlateTokens.SurfaceElevated else ElectricSlateTokens.TrackInactive)
                                            .border(1.dp, ElectricSlateTokens.BorderSubtle, CircleShape)
                                            .jigglePressClickable(enabled = canAddNewSheet) { viewModel.addSheet() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add Sheet",
                                            tint = if (canAddNewSheet) ElectricSlateTokens.TextSecondary else ElectricSlateTokens.TextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (uiState.isBottomDockCollapsed) {
                // Collapsed state: Zoom chip on left, slim handle in center
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        color = ElectricSlateTokens.SurfaceElevated,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${uiState.zoomPercentage}%",
                                color = ElectricSlateTokens.TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .jigglePressClickable { drawingViewRef?.resetZoom() }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .jigglePressClickable { drawingViewRef?.fitToContent() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FitScreen,
                                    contentDescription = "Fit to Content",
                                    tint = ElectricSlateTokens.TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Slim bottom edge pill
                    Surface(
                        color = ElectricSlateTokens.SurfacePanel,
                        shape = CircleShape,
                        shadowElevation = 8.dp,
                        border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
                        modifier = Modifier
                            .width(72.dp)
                            .height(24.dp)
                            .clip(CircleShape)
                            .jigglePressClickable { viewModel.toggleBottomDock() }
                            .pointerInput(Unit) {
                                detectVerticalDragGestures { _, dragAmount ->
                                    if (dragAmount < -6f) {
                                        viewModel.toggleBottomDock()
                                    }
                                }
                            }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(ElectricSlateTokens.BorderSubtle)
                            )
                        }
                    }

                    // Spacer on right to balance the zoom pill
                    Spacer(modifier = Modifier.width(96.dp))
                }
            } else {
                AppFloatingBar(
                    visible = true,
                    containerColor = ElectricSlateTokens.SurfacePanel,
                    borderColor = ElectricSlateTokens.BorderSubtle,
                    applyWindowInsets = false,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    // 1. Select
                    val isSelect = uiState.toolState.activeTool == CanvasToolType.SELECT
                    AppFloatingBarItem(
                        icon = Icons.Default.NearMe,
                        contentDescription = stringResource(R.string.canvas_tool_select),
                        isSelected = isSelect,
                        onClick = {
                            if (isSelect) viewModel.toggleToolOptions() else viewModel.setTool(CanvasToolType.SELECT)
                        }
                    )

                    // 2. Pen
                    val isPen = uiState.toolState.activeTool == CanvasToolType.PEN
                    AppFloatingBarItem(
                        icon = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.canvas_tool_pen),
                        isSelected = isPen,
                        onClick = {
                            if (isPen) viewModel.toggleToolOptions() else viewModel.setTool(CanvasToolType.PEN)
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.showPenOptions()
                        }
                    )

                    // 3. Highlighter
                    val isHighlighter = uiState.toolState.activeTool == CanvasToolType.HIGHLIGHTER
                    AppFloatingBarItem(
                        painter = painterResource(id = R.drawable.ic_highlighter),
                        contentDescription = stringResource(R.string.canvas_tool_highlighter),
                        isSelected = isHighlighter,
                        onClick = {
                            if (isHighlighter) viewModel.toggleToolOptions() else viewModel.setTool(CanvasToolType.HIGHLIGHTER)
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.showHighlighterOptions()
                        }
                    )

                    // 4. Eraser
                    val isEraser = uiState.toolState.activeTool == CanvasToolType.ERASER
                    AppFloatingBarItem(
                        painter = painterResource(id = R.drawable.ic_eraser),
                        contentDescription = stringResource(R.string.canvas_tool_eraser),
                        isSelected = isEraser,
                        onClick = {
                            if (isEraser) viewModel.toggleToolOptions() else viewModel.setTool(CanvasToolType.ERASER)
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.showEraserOptions()
                        }
                    )

                    // 5. Text
                    val isText = uiState.toolState.activeTool == CanvasToolType.TEXT
                    AppFloatingBarCustomItem(
                        isSelected = isText,
                        onClick = {
                            if (isText) viewModel.toggleToolOptions() else {
                                viewModel.setTool(CanvasToolType.TEXT)
                                viewModel.showTextOptions()
                            }
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.showTextOptions()
                        }
                    ) {
                        Text(
                            text = "T",
                            color = if (isText) Color.White else ElectricSlateTokens.TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    // 6. Shapes & Smooth
                    val isShape = uiState.toolState.activeTool == CanvasToolType.SHAPE
                    AppFloatingBarItem(
                        painter = painterResource(id = R.drawable.ic_shapes),
                        contentDescription = "Shapes & Smooth",
                        isSelected = isShape,
                        onClick = {
                            if (isShape) viewModel.toggleToolOptions() else {
                                viewModel.setTool(CanvasToolType.SHAPE)
                                viewModel.showShapeOptions()
                            }
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.showShapeOptions()
                        }
                    )

                    // Vertical subtle divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(ElectricSlateTokens.BorderSubtle)
                    )

                    // 7. Active Color Swatch Circle
                    val currentColor = if (isHighlighter) uiState.toolState.highlighterColor else uiState.toolState.penColor
                    AppFloatingBarCustomItem(
                        isSelected = false,
                        onClick = {
                            viewModel.toggleToolOptions()
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color(currentColor.toInt()))
                                .border(1.dp, ElectricSlateTokens.BorderSubtle, CircleShape)
                        )
                    }
                }
            }
        }

        // ==========================================
        // Z8: Contextual Object Bar (Selection Actions)
        // ==========================================
        val isSelectActive = uiState.toolState.activeTool == CanvasToolType.SELECT
        val hasSelection = uiState.toolState.selection.isNotEmpty
        if (isSelectActive && hasSelection) {
            val topOffsetDp = CanvasDockLayoutHelper.computeZ8TopOffset(
                with(density) { topCapsulesHeightPx.toDp() }.value
            ).dp
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = topOffsetDp)
                    .zIndex(15f)
            ) {
                Surface(
                    color = ElectricSlateTokens.SurfaceElevated,
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
                    modifier = Modifier.height(44.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val selectedTextElement = if (hasSelection) {
                            uiState.document.elements.firstOrNull { el ->
                                uiState.toolState.selection.elementIds.contains(el.id) && el is com.arinara.fotara.canvas.model.TextLayerElement
                            } as? com.arinara.fotara.canvas.model.TextLayerElement
                        } else null

                        if (selectedTextElement != null) {
                            IconButton(
                                onClick = { viewModel.showTextOptions(selectedTextElement) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit Text",
                                    tint = ElectricSlateTokens.TextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        val selectedImageElement = if (hasSelection) {
                            uiState.document.elements.firstOrNull { el ->
                                uiState.toolState.selection.elementIds.contains(el.id) && el is com.arinara.fotara.canvas.model.ImageElement
                            } as? com.arinara.fotara.canvas.model.ImageElement
                        } else null

                        if (selectedImageElement != null) {
                            IconButton(
                                onClick = {
                                    activeVfxImageElement = selectedImageElement
                                    showVfxBottomSheet = true
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Image Effects",
                                    tint = ElectricSlateTokens.TextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Duplicate
                        IconButton(
                            onClick = { viewModel.duplicateSelectedElements() },
                            enabled = hasSelection,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = stringResource(R.string.canvas_action_duplicate),
                                tint = if (hasSelection) ElectricSlateTokens.TextPrimary else ElectricSlateTokens.TextMuted.copy(alpha = 0.35f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Arrange & Layer Order
                        Box {
                            IconButton(
                                onClick = { showArrangeMenu = true },
                                enabled = hasSelection,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Layers,
                                    contentDescription = "Arrange & Layer Order",
                                    tint = if (hasSelection) ElectricSlateTokens.TextPrimary else ElectricSlateTokens.TextMuted.copy(alpha = 0.35f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showArrangeMenu && hasSelection,
                                onDismissRequest = { showArrangeMenu = false },
                                modifier = Modifier
                                    .background(ElectricSlateTokens.SurfaceElevated)
                                    .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Bring to Front",
                                            color = ElectricSlateTokens.TextPrimary,
                                            fontSize = 14.sp,
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.KeyboardArrowUp,
                                            contentDescription = null,
                                            tint = ElectricSlateTokens.TextMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        showArrangeMenu = false
                                        viewModel.bringSelectionToFront()
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Bring Forward",
                                            color = ElectricSlateTokens.TextPrimary,
                                            fontSize = 14.sp,
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.KeyboardArrowUp,
                                            contentDescription = null,
                                            tint = ElectricSlateTokens.TextMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        showArrangeMenu = false
                                        viewModel.bringSelectionForward()
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Send Backward",
                                            color = ElectricSlateTokens.TextPrimary,
                                            fontSize = 14.sp,
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = ElectricSlateTokens.TextMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        showArrangeMenu = false
                                        viewModel.sendSelectionBackward()
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Send to Back",
                                            color = ElectricSlateTokens.TextPrimary,
                                            fontSize = 14.sp,
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = ElectricSlateTokens.TextMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        showArrangeMenu = false
                                        viewModel.sendSelectionToBack()
                                    }
                                )
                                HorizontalDivider(
                                    color = ElectricSlateTokens.BorderSubtle,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Move to Layer Above",
                                            color = ElectricSlateTokens.PrimaryAccent,
                                            fontSize = 14.sp,
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.AutoMirrored.Filled.DriveFileMove,
                                            contentDescription = null,
                                            tint = ElectricSlateTokens.PrimaryAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        showArrangeMenu = false
                                        viewModel.moveSelectionToLayerAbove()
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Move to Layer...",
                                            color = ElectricSlateTokens.TextPrimary,
                                            fontSize = 14.sp,
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Layers,
                                            contentDescription = null,
                                            tint = ElectricSlateTokens.TextMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        showArrangeMenu = false
                                        showMoveElementsLayerPicker = true
                                    }
                                )
                            }
                        }

                        // Delete
                        IconButton(
                            onClick = { viewModel.deleteSelectedElements() },
                            enabled = hasSelection,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.canvas_action_delete_selection),
                                tint = if (hasSelection) ElectricSlateTokens.Destructive else ElectricSlateTokens.TextMuted.copy(alpha = 0.35f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // Z5: Layers Management Panel (BottomSheet)
        // ==========================================
        if (uiState.showLayersPanel) {
            CanvasLayersSheet(
                layers = uiState.document.layers,
                activeLayerId = uiState.activeLayerId,
                onDismissRequest = { viewModel.setLayersPanelVisible(false) },
                onAddLayer = { viewModel.addDefaultLayer() },
                onSetActiveLayer = { viewModel.setActiveLayer(it) },
                onRenameLayer = { id, name -> viewModel.renameLayer(id, name) },
                onMoveLayerUp = { viewModel.moveLayerUp(it) },
                onMoveLayerDown = { viewModel.moveLayerDown(it) },
                onToggleVisibility = { viewModel.toggleLayerVisibility(it) },
                onToggleLock = { viewModel.toggleLayerLock(it) },
                onDeleteLayer = { viewModel.deleteLayer(it) },
                onSetOpacity = { id, opacity -> viewModel.setLayerOpacity(id, opacity) }
            )
        }

        // ==========================================
        // Dialogs & Sheets
        // ==========================================

        // Rename Canvas Dialog
        if (uiState.showRenameDialog) {
            CanvasRenameDialog(
                initialTitle = uiState.title.ifBlank { renameInput },
                onDismissRequest = { viewModel.setRenameDialogVisible(false) },
                onSave = {
                    renameInput = it
                    viewModel.renameCanvas(it)
                }
            )
        }

        // Background Style Picker Dialog
        if (uiState.showBackgroundPicker) {
            CanvasBackgroundPickerDialog(
                currentStyle = uiState.document.backgroundStyle,
                onDismissRequest = { viewModel.setBackgroundPickerVisible(false) },
                onSelectStyle = { viewModel.setBackgroundStyle(it) }
            )
        }

        // Canvas Settings Dialog
        if (uiState.showSettingsDialog) {
            CanvasSettingsDialog(
                stylusOnlyDrawing = uiState.toolState.stylusOnlyDrawing,
                palmRejectionEnabled = uiState.toolState.palmRejectionEnabled,
                onToggleStylusOnly = { viewModel.setStylusOnlyMode(it) },
                onTogglePalmRejection = { viewModel.setPalmRejection(it) },
                onOpenBackgroundPicker = {
                    viewModel.setSettingsDialogVisible(false)
                    viewModel.setBackgroundPickerVisible(true)
                },
                onOpenScheduleNote = {
                    viewModel.setSettingsDialogVisible(false)
                    viewModel.setScheduleDialogVisible(true)
                },
                onOpenNoteInfo = {
                    viewModel.setSettingsDialogVisible(false)
                    viewModel.setInfoDialogVisible(true)
                },
                hasSchedule = uiState.scheduledAt != null,
                onCopyFrameStats = if (com.arinara.fotara.BuildConfig.DEBUG) {
                    {
                        val summary = com.arinara.fotara.debug.FrameIntervalRecorder.getSummary()
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                        clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("Frame Stats", summary))
                        android.widget.Toast.makeText(context, context.getString(R.string.msg_frame_stats_copied), android.widget.Toast.LENGTH_SHORT).show()
                    }
                } else null,
                onDismissRequest = { viewModel.setSettingsDialogVisible(false) }
            )
        }

        // Consolidated Share Modal Bottom Sheet
        if (showShareSheet) {
            CanvasShareSheet(
                onDismissRequest = { showShareSheet = false },
                onExportPng = {
                    showShareSheet = false
                    viewModel.setExportDialogVisible(true)
                },
                onExportPdf = {
                    showShareSheet = false
                    viewModel.exportMultiSheetPdf(context)
                },
                onExportFotara = {
                    showShareSheet = false
                    viewModel.exportCanvasNoteFile(context)
                }
            )
        }

        // Visual Effects (VFX) Minimalist Bottom Docked Panel
        if (showVfxBottomSheet && activeVfxImageElement != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(25f),
                contentAlignment = Alignment.BottomCenter
            ) {
                com.arinara.fotara.canvas.vfx.VfxBottomPanel(
                    parameters = activeVfxImageElement!!.vfxParams,
                    onParametersChanged = { newParams ->
                        val el = activeVfxImageElement ?: return@VfxBottomPanel
                        val updated = el.copy(vfxParams = newParams)
                        activeVfxImageElement = updated
                        viewModel.updateImageVfx(el.id, newParams)
                    },
                    onDismiss = {
                        showVfxBottomSheet = false
                        activeVfxImageElement = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                )
            }
        }


        // Note Info Dialog
        if (uiState.showInfoDialog) {
            CanvasInfoDialog(
                title = uiState.title,
                createdAt = uiState.document.createdAt,
                layerCount = uiState.document.layers.size,
                elementCount = uiState.document.elements.size,
                onDismissRequest = { viewModel.setInfoDialogVisible(false) }
            )
        }

        // Move to Folder Dialog
        if (uiState.showMoveDialog) {
            CanvasMoveDialog(
                folders = uiState.folders,
                workspaces = uiState.workspaces,
                onSelectFolder = { folderId ->
                    viewModel.moveCanvas(folderId, null) {
                        onBack()
                    }
                },
                onDismissRequest = { viewModel.setMoveDialogVisible(false) }
            )
        }

        // Delete Confirm Dialog
        if (uiState.showDeleteConfirmDialog) {
            CanvasDeleteConfirmDialog(
                onDismissRequest = { viewModel.setDeleteConfirmDialogVisible(false) },
                onConfirmDelete = { viewModel.deleteToTrash { onBack() } }
            )
        }

        // Image Source Dialog
        if (uiState.showImageSourceDialog) {
            CanvasImageSourceDialog(
                onPickFromGallery = {
                    viewModel.setImageSourceDialogVisible(false)
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onPickFromNotes = {
                    viewModel.loadExistingNotes()
                },
                onDismissRequest = { viewModel.setImageSourceDialogVisible(false) }
            )
        }

        // Existing Notes Picker Dialog
        if (uiState.showExistingNotesDialog) {
            CanvasExistingNotesDialog(
                isLoading = uiState.isLoadingExistingNotes,
                existingPhotos = uiState.existingImageNotes,
                onSelectPhoto = { photo ->
                    val v = drawingViewRef
                    if (v != null) {
                        viewModel.addFromExistingPhoto(
                            photo,
                            v.viewport,
                            v.width.toFloat(),
                            v.height.toFloat()
                        )
                    }
                },
                onDismissRequest = { viewModel.setExistingNotesDialogVisible(false) }
            )
        }

        // Export PNG Dialog
        if (uiState.showExportDialog) {
            CanvasExportDialog(
                hasSelection = uiState.toolState.selectedElementIds.isNotEmpty(),
                selectedCount = uiState.toolState.selectedElementIds.size,
                isExporting = uiState.isExporting,
                exportProgress = uiState.exportProgress,
                onExport = { selection, scale ->
                    viewModel.exportCanvas(context, selection, scale)
                },
                onDismissRequest = { viewModel.setExportDialogVisible(false) }
            )
        }

        // Move Elements to Layer Sheet
        if (showMoveElementsLayerPicker) {
            CanvasMoveElementsLayerSheet(
                layers = uiState.document.layers,
                onDismissRequest = { showMoveElementsLayerPicker = false },
                onSelectLayer = { layerId ->
                    viewModel.moveSelectionToLayer(layerId)
                    showMoveElementsLayerPicker = false
                }
            )
        }


        // Text Layer Placement / Edit Dialog
        if (uiState.showTextOptions) {
            CanvasTextOptionsDialog(
                editingTextLayer = uiState.editingTextLayer,
                defaultFontSize = uiState.toolState.textFontSizeSp,
                defaultFontWeight = uiState.toolState.textFontWeight,
                defaultColor = uiState.toolState.textColor,
                defaultBackgroundStyle = uiState.toolState.textBackgroundStyle,
                recentColors = uiState.recentColors,
                onSave = { text, fontSizeSp, color, fontWeight, bgStyle, alignment ->
                    val v = drawingViewRef
                    val vp = v?.viewport ?: com.arinara.fotara.canvas.engine.ViewportState()
                    val w = v?.width?.toFloat() ?: 1080f
                    val h = v?.height?.toFloat() ?: 1920f
                    viewModel.addOrUpdateTextLayer(
                        text = text,
                        fontSizeSp = fontSizeSp,
                        color = color,
                        fontWeight = fontWeight,
                        backgroundStyle = bgStyle,
                        alignment = alignment,
                        viewport = vp,
                        screenWidth = w,
                        screenHeight = h
                    )
                },
                onDelete = uiState.editingTextLayer?.let { editing ->
                    { viewModel.deleteTextLayer(editing.id) }
                },
                onDismissRequest = { viewModel.dismissTextOptions() }
            )
        }

        // Schedule Note Dialog (C5)
        if (uiState.showScheduleDialog && uiState.canvasId != null) {
            ScheduleNoteDialog(
                noteTitle = uiState.title,
                initialScheduledAt = uiState.scheduledAt,
                initialAlertType = if (uiState.alertType == ScheduleAlertType.ALARM.name) ScheduleAlertType.ALARM else ScheduleAlertType.NOTIFICATION,
                initialScheduleTitle = null,
                onDismiss = { viewModel.setScheduleDialogVisible(false) },
                onSaveSchedule = { scheduledAt, alertType, scheduleTitle ->
                    viewModel.saveSchedule(scheduledAt, alertType, scheduleTitle)
                },
                onClearSchedule = {
                    viewModel.clearSchedule()
                }
            )
        }

        // User Message Snackbar/Toast banner
        uiState.userMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 64.dp)
                    .zIndex(20f)
            ) {
                Surface(
                    color = ElectricSlateTokens.Destructive,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(msg, color = TextPrimary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { viewModel.clearUserMessage() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Clear, null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
        // Collab Room Join Dialog (with Custom Room Code Creation & Active Session Guard)
        if (showCollabJoinDialog) {
            CollabRoomJoinDialog(
                onDismissRequest = {
                    if (!isJoiningCollabRoom) {
                        collabEngine.clearErrorMessage()
                        showCollabJoinDialog = false
                    }
                },
                onJoinRoom = { code ->
                    if (!isJoiningCollabRoom) {
                        isJoiningCollabRoom = true
                        collabEngine.clearErrorMessage()
                        coroutineScope.launch {
                            val joined = try {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    collabEngine.joinRoom(
                                        code = code,
                                        userTag = realProfileName,
                                        colorHex = "#7A5AF8",
                                        pronouns = realProfilePronouns,
                                        requireActiveHost = true,
                                        userHandle = resolvedUserHandle
                                    )
                                }
                            } finally {
                                isJoiningCollabRoom = false
                            }
                            if (joined) {
                                showCollabJoinDialog = false
                            }
                        }
                    }
                },
                onCreateRoom = {
                    val created = collabEngine.createRoom(
                        userTag = realProfileName,
                        colorHex = "#7A5AF8",
                        pronouns = realProfilePronouns,
                        userHandle = resolvedUserHandle
                    )
                    if (created.isNotBlank()) {
                        showCollabJoinDialog = false
                    }
                },
                onCreateCustomRoom = { customCode ->
                    if (collabEngine.createCustomRoom(
                            customCode = customCode,
                            userTag = realProfileName,
                            colorHex = "#7A5AF8",
                            pronouns = realProfilePronouns,
                            userHandle = resolvedUserHandle
                        )
                    ) {
                        showCollabJoinDialog = false
                    }
                },
                localUserName = realProfileName,
                localUserRole = collabLocalRole,
                activeRoomCode = collabRoomCode,
                activePeerCount = collabPeers.size,
                isJoining = isJoiningCollabRoom,
                externalErrorMessage = collabErrorMessage,
                onClearExternalError = { collabEngine.clearErrorMessage() },
                onOpenChat = {
                    showCollabJoinDialog = false
                    showCollabChatSheet = true
                },
                onOpenRoomSettings = {
                    showCollabJoinDialog = false
                    showCollabSettingsSheet = true
                },
                onLeaveActiveRoom = {
                    showCollabChatSheet = false
                    showCollabSettingsSheet = false
                    collabEngine.leaveRoom()
                    showCollabJoinDialog = false
                }
            )
        }

        // Collab Room Real-Time Chat Sheet
        if (showCollabChatSheet && collabRoomCode != null) {
            com.arinara.fotara.feature.collab.ui.CollabChatSheet(
                roomCode = collabRoomCode!!,
                peers = collabPeers,
                messages = collabChatMessages,
                onSendMessage = { msgText ->
                    collabEngine.sendChatMessage(msgText)
                },
                onDismissRequest = { showCollabChatSheet = false },
                localUserName = realProfileName,
                localRole = collabLocalRole,
                pinnedMessage = collabPinnedMessage,
                isLocalUserChatBlocked = collabIsChatBlocked,
                onPinMessage = { msgId ->
                    collabEngine.pinChatMessage(msgId)
                },
                onDeleteMessage = { msgId ->
                    collabEngine.deleteChatMessage(msgId)
                },
                onOpenSettings = {
                    showCollabChatSheet = false
                    showCollabSettingsSheet = true
                }
            )
        }

        // Collab Room & Audio / Moderation / Privacy Settings Sheet
        if (showCollabSettingsSheet && collabRoomCode != null) {
            com.arinara.fotara.feature.collab.ui.CollabRoomSettingsSheet(
                roomCode = collabRoomCode!!,
                localPeerId = collabEngine.localPeerId,
                localRole = collabLocalRole,
                peers = collabPeers,
                peerVolumes = collabPeerVolumes,
                locallyMutedPeerIds = collabLocallyMutedPeers,
                audioBitrateKbps = collabAudioBitrate,
                roomMaxBitrateKbps = collabRoomMaxBitrate,
                accessMode = collabAccessMode,
                isGlobalDrawingDisabled = collabIsGlobalDrawingDisabled,
                isGlobalSheetCreationDisabled = collabIsGlobalSheetDisabled,
                maxParticipants = collabMaxParticipants,
                joinPolicy = collabJoinPolicy,
                allowedUserHandles = collabAllowedHandles,
                invitedUserHandles = collabInvitedHandles,
                savedPrivacyPresets = collabSavedPresets,
                activePresetId = collabActivePresetId,
                hostFriends = uiState.friends,
                onSetMaxParticipants = { maxUsers -> collabEngine.setMaxParticipants(maxUsers) },
                onSetJoinPolicy = { policy -> collabEngine.setJoinPolicy(policy) },
                onAddAllowedUserHandle = { handle -> collabEngine.addAllowedUserHandle(handle) },
                onRemoveAllowedUserHandle = { handle -> collabEngine.removeAllowedUserHandle(handle) },
                onInviteUser = { handle -> collabEngine.inviteUser(handle) },
                onRevokeInvitation = { handle -> collabEngine.revokeInvitation(handle) },
                onSavePrivacyPreset = { presetName -> collabEngine.savePrivacyPreset(presetName) },
                onApplyPrivacyPreset = { presetId -> collabEngine.applyPrivacyPreset(presetId) },
                onDeletePrivacyPreset = { presetId -> collabEngine.deletePrivacyPreset(presetId) },
                onSetPeerVolume = { peerId, vol -> collabEngine.setPeerVolume(peerId, vol) },
                onToggleLocalPeerMute = { peerId -> collabEngine.toggleLocalPeerMute(peerId) },
                onSetAudioBitrate = { kbps -> collabEngine.setAudioBitrateKbps(kbps) },
                onSetRoomMaxBitrate = { maxKbps -> collabEngine.setRoomMaxBitrateKbps(maxKbps) },
                onSetAccessMode = { mode -> collabEngine.setAccessMode(mode) },
                onSetGlobalDrawingDisabled = { disabled -> collabEngine.setGlobalDrawingDisabled(disabled) },
                onSetGlobalSheetCreationDisabled = { disabled -> collabEngine.setGlobalSheetCreationDisabled(disabled) },
                onSetPeerDrawingDisabled = { peerId, disabled -> collabEngine.setPeerDrawingDisabled(peerId, disabled) },
                onSetPeerSheetCreationDisabled = { peerId, disabled -> collabEngine.setPeerSheetCreationDisabled(peerId, disabled) },
                onSetPeerAdmin = { peerId, isAdmin -> collabEngine.setPeerAdmin(peerId, isAdmin) },
                onTransferHostOwnership = { newHostPeerId -> collabEngine.transferHostOwnership(newHostPeerId) },
                onSetPeerRoomMuted = { peerId, isMuted -> collabEngine.setPeerRoomMuted(peerId, isMuted) },
                onSetPeerChatBlocked = { peerId, isBlocked -> collabEngine.setPeerChatBlocked(peerId, isBlocked) },
                onKickPeer = { peerId -> collabEngine.kickPeer(peerId) },
                onDismissRequest = { showCollabSettingsSheet = false }
            )
        }
    }
}
