// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.arinara.fotara.data.AppContainer
import com.arinara.fotara.diagnostic.AppDiagnosticLogger
import com.arinara.fotara.diagnostic.DiagnosticInfoScreen
import com.arinara.fotara.ui.folder.FolderDetailScreen
import com.arinara.fotara.ui.folder.FolderDetailViewModel
import com.arinara.fotara.ui.group.GroupDetailScreen
import com.arinara.fotara.ui.group.GroupDetailViewModel
import com.arinara.fotara.ui.home.HomeScreen
import com.arinara.fotara.ui.home.HomeViewModel
import com.arinara.fotara.ui.onboarding.OnboardingScreen
import com.arinara.fotara.ui.onboarding.OnboardingViewModel
import com.arinara.fotara.ui.settings.SettingsScreen
import com.arinara.fotara.ui.settings.SettingsViewModel
import com.arinara.fotara.ui.trash.TrashScreen
import com.arinara.fotara.ui.trash.TrashViewModel

@Composable
fun MainNavigation(
    appContainer: AppContainer,
    deepLinkPhotoId: Long? = null,
    deepLinkDirectView: Boolean = false,
    openUpdateScreen: Boolean = false,
    onUpdateScreenOpened: (() -> Unit)? = null,
    openDiagnosticScreen: Boolean = false,
    onDiagnosticScreenOpened: (() -> Unit)? = null
) {
    val initialKey = remember {
        if (appContainer.settingsRepository.isOnboardingCompleted()) HomeNavKey else OnboardingNavKey
    }
    val backStack = rememberNavBackStack(initialKey)

    LaunchedEffect(openUpdateScreen) {
        if (openUpdateScreen) {
            if (backStack.none { it is UpdateNavKey }) {
                backStack.add(UpdateNavKey)
            }
            onUpdateScreenOpened?.invoke()
        }
    }

    LaunchedEffect(openDiagnosticScreen) {
        if (openDiagnosticScreen) {
            if (backStack.none { it is DiagnosticNavKey }) {
                backStack.add(DiagnosticNavKey)
            }
            onDiagnosticScreenOpened?.invoke()
        }
    }

    LaunchedEffect(backStack.lastOrNull()) {
        val currentKey = backStack.lastOrNull()
        val screenName = when (currentKey) {
            is HomeNavKey -> "Home"
            is FolderDetailNavKey -> "FolderDetail"
            is GroupDetailNavKey -> "GroupDetail"
            is TrashNavKey -> "Trash"
            is SettingsNavKey -> "Settings"
            is OnboardingNavKey -> "Onboarding"
            is PdfViewerNavKey -> "PdfViewer"
            is DocxViewerNavKey -> "DocxViewer"
            is TextNoteEditorNavKey -> "TextNoteEditor"
            is UpdateNavKey -> "Updates"
            is SupportNavKey -> "Support"
            is WhatsNewNavKey -> "WhatsNew"
            is CanvasNoteNavKey -> "CanvasNote"
            is PdfPageEditorNavKey -> "PdfPageEditor"
            is SpaceSettingsNavKey -> "SpaceSettings"
            is DiagnosticNavKey -> "Diagnostic"
            else -> currentKey?.let { it::class.simpleName } ?: "Unknown"
        }
        AppDiagnosticLogger.recordScreen(screenName)
    }

    LaunchedEffect(deepLinkPhotoId) {
        val photoId = deepLinkPhotoId ?: return@LaunchedEffect
        if (photoId <= 0) return@LaunchedEffect
        val photo = appContainer.photoRepository.getPhotoById(photoId)
        if (photo != null && !photo.isTrashed) {
            backStack.add(
                FolderDetailNavKey(
                    folderId = photo.folderId,
                    initialSubfolderId = photo.subfolderId,
                    targetPhotoId = photo.id,
                    openViewerDirectly = deepLinkDirectView
                )
            )
        }
    }

    val safePopBack = {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = { safePopBack() },
        transitionSpec = {
            ContentTransform(
                targetContentEnter = slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth / 8 },
                    animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(durationMillis = 120)),
                initialContentExit = fadeOut(animationSpec = tween(durationMillis = 80)),
                targetContentZIndex = 1f
            )
        },
        popTransitionSpec = {
            ContentTransform(
                targetContentEnter = fadeIn(animationSpec = tween(durationMillis = 120)),
                initialContentExit = slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth / 8 },
                    animationSpec = tween(durationMillis = 130, easing = FastOutLinearInEasing)
                ) + fadeOut(animationSpec = tween(durationMillis = 90)),
                targetContentZIndex = 0f
            )
        },
        entryProvider = entryProvider {
            entry<OnboardingNavKey> {
                val onboardingViewModel: OnboardingViewModel = viewModel(
                    factory = OnboardingViewModel.provideFactory(
                        folderRepository = appContainer.folderRepository,
                        settingsRepository = appContainer.settingsRepository
                    )
                )
                OnboardingScreen(
                    viewModel = onboardingViewModel,
                    onFinishOnboarding = {
                        backStack.clear()
                        backStack.add(HomeNavKey)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            entry<HomeNavKey> {
                val homeViewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.provideFactory(
                        folderRepository = appContainer.folderRepository,
                        photoRepository = appContainer.photoRepository,
                        deadlineNotificationManager = appContainer.deadlineNotificationManager,
                        settingsRepository = appContainer.settingsRepository,
                        textNoteRepository = appContainer.textNoteRepository,
                        documentRepository = appContainer.documentRepository,
                        canvasNoteRepository = appContainer.canvasNoteRepository,
                        workspaceRepository = appContainer.workspaceRepository
                    )
                )
                HomeScreen(
                    viewModel = homeViewModel,
                    settingsViewModelProvider = {
                        viewModel(
                            factory = SettingsViewModel.provideFactory(
                                settingsRepository = appContainer.settingsRepository,
                                deviceRegistry = appContainer.deviceRegistry,
                                friendsRepository = appContainer.friendsRepository
                            )
                        )
                    },
                    notesViewModelProvider = {
                        viewModel(
                            factory = com.arinara.fotara.ui.notes.NotesViewModel.provideFactory(
                                photoRepository = appContainer.photoRepository,
                                documentRepository = appContainer.documentRepository,
                                textNoteRepository = appContainer.textNoteRepository,
                                canvasNoteRepository = appContainer.canvasNoteRepository,
                                folderRepository = appContainer.folderRepository,
                                workspaceRepository = appContainer.workspaceRepository,
                                scheduleRepository = appContainer.scheduleRepository,
                                scheduleCutoffEngine = appContainer.scheduleCutoffEngine,
                                scheduleNotificationScheduler = appContainer.scheduleNotificationScheduler,
                                settingsRepository = appContainer.settingsRepository
                            )
                        )
                    },
                    updateManager = appContainer.updateManager,
                    dialogCoordinator = appContainer.dialogCoordinator,
                    spaceRepository = appContainer.spaceRepository,
                    workspaceRepository = appContainer.workspaceRepository,
                    activeSpaceManager = appContainer.activeSpaceManager,
                    friendsRepository = appContainer.friendsRepository,
                    onFolderClick = { folder ->
                        backStack.add(FolderDetailNavKey(folder.id))
                    },
                    onNavigateToPhoto = { folderId, subfolderId, photoId ->
                        backStack.add(FolderDetailNavKey(folderId = folderId, initialSubfolderId = subfolderId, targetPhotoId = photoId, openViewerDirectly = true))
                    },
                    onNavigateToGroup = { folderId, subfolderId, groupId ->
                        backStack.add(FolderDetailNavKey(folderId = folderId, initialSubfolderId = subfolderId, targetGroupId = groupId))
                    },
                    onNavigateToDocument = { folderId, subfolderId, docId, targetPageIndex, searchQuery ->
                        backStack.add(FolderDetailNavKey(
                            folderId = folderId,
                            initialSubfolderId = subfolderId,
                            targetDocumentId = docId,
                            openViewerDirectly = true,
                            targetPageIndex = targetPageIndex,
                            searchQuery = searchQuery
                        ))
                    },
                    onNavigateToTextNote = { folderId, subfolderId, noteId, highlightQuery ->
                        if (noteId != -1L) {
                            backStack.add(TextNoteEditorNavKey(
                                noteId = noteId,
                                folderId = folderId,
                                subfolderId = subfolderId,
                                highlightQuery = highlightQuery
                            ))
                        } else {
                            backStack.add(TextNoteEditorNavKey(
                                noteId = null,
                                folderId = folderId,
                                subfolderId = subfolderId
                            ))
                        }
                    },
                    onNavigateToCanvasNote = { folderId, subfolderId, canvasId ->
                        backStack.add(FolderDetailNavKey(folderId = folderId, initialSubfolderId = subfolderId, targetCanvasId = canvasId))
                    },
                    onOpenDocx = { docId ->
                        backStack.add(DocxViewerNavKey(documentId = docId))
                    },
                    onOpenTrash = {
                        backStack.add(TrashNavKey)
                    },
                    onOpenSettings = {
                        backStack.add(SettingsNavKey)
                    },
                    onOpenUpdates = {
                        backStack.add(UpdateNavKey)
                    },
                    onOpenSupport = {
                        backStack.add(SupportNavKey)
                    },
                    onOpenWhatsNew = {
                        backStack.add(WhatsNewNavKey)
                    },
                    onOpenSpaceSettings = { sId ->
                        backStack.add(SpaceSettingsNavKey(sId))
                    },
                    feedbackManager = appContainer.feedbackManager,
                    modifier = Modifier.fillMaxSize()
                )
            }

            entry<FolderDetailNavKey> { key ->
                val folderDetailViewModel: FolderDetailViewModel = viewModel(
                    key = "folder_${key.folderId}_${key.initialSubfolderId}_${key.targetPhotoId}_${key.targetGroupId}_${key.targetDocumentId}_${key.targetTextNoteId}_${key.targetCanvasId}",
                    factory = FolderDetailViewModel.provideFactory(
                        folderId = key.folderId,
                        folderRepository = appContainer.folderRepository,
                        photoRepository = appContainer.photoRepository,
                        photoStorageManager = appContainer.photoStorageManager,
                        ocrEngine = appContainer.ocrEngine,
                        folderSuggestEngine = appContainer.folderSuggestEngine,
                        deadlineNotificationManager = appContainer.deadlineNotificationManager,
                        initialSubfolderId = key.initialSubfolderId,
                        targetPhotoId = key.targetPhotoId,
                        targetGroupId = key.targetGroupId,
                        targetDocumentId = key.targetDocumentId,
                        targetTextNoteId = key.targetTextNoteId,
                        targetCanvasId = key.targetCanvasId,
                        settingsRepository = appContainer.settingsRepository,
                        documentRepository = appContainer.documentRepository,
                        textNoteRepository = appContainer.textNoteRepository,
                        canvasNoteRepository = appContainer.canvasNoteRepository,
                        workspaceRepository = appContainer.workspaceRepository
                    )
                )
                FolderDetailScreen(
                    viewModel = folderDetailViewModel,
                    photoStorageManager = appContainer.photoStorageManager,
                    ocrEngine = appContainer.ocrEngine,
                    folderSuggestEngine = appContainer.folderSuggestEngine,
                    onBackClick = { safePopBack() },
                    openViewerDirectly = key.openViewerDirectly,
                    targetPageIndex = key.targetPageIndex,
                    searchQuery = key.searchQuery,
                    pdfPagePinRepository = appContainer.pdfPagePinRepository,
                    pdfPageDrawingRepository = appContainer.pdfPageDrawingRepository,
                    settingsRepository = appContainer.settingsRepository,
                    onNavigateToEditor = { docId, pageIndex ->
                        backStack.add(PdfPageEditorNavKey(documentId = docId, pageIndex = pageIndex))
                    },
                    onOpenGroup = { fId, gId, targetPhotoId ->
                        backStack.add(GroupDetailNavKey(folderId = fId, groupId = gId, targetPhotoId = targetPhotoId))
                    },
                    onOpenTextNote = { nId, fId, sId ->
                        backStack.add(TextNoteEditorNavKey(noteId = nId, folderId = fId, subfolderId = sId))
                    },
                    onOpenDocx = { dId ->
                        backStack.add(DocxViewerNavKey(documentId = dId))
                    },
                    onOpenCanvasNote = { cId, fId, sId ->
                        backStack.add(CanvasNoteNavKey(canvasId = cId, folderId = fId, subfolderId = sId))
                    },
                    canvasRepository = appContainer.canvasRepository,
                    canvasAssetManager = appContainer.canvasAssetManager,
                    modifier = Modifier.fillMaxSize()
                )
            }

            entry<GroupDetailNavKey> { key ->
                val groupDetailViewModel: GroupDetailViewModel = viewModel(
                    key = "group_${key.folderId}_${key.groupId}_${key.targetPhotoId}",
                    factory = GroupDetailViewModel.provideFactory(
                        groupId = key.groupId,
                        folderId = key.folderId,
                        targetPhotoId = key.targetPhotoId,
                        photoRepository = appContainer.photoRepository,
                        folderRepository = appContainer.folderRepository,
                        canvasNoteRepository = appContainer.canvasNoteRepository,
                        workspaceRepository = appContainer.workspaceRepository,
                        settingsRepository = appContainer.settingsRepository,
                        ocrEngine = appContainer.ocrEngine,
                        photoStorageManager = appContainer.photoStorageManager,
                        deadlineNotificationManager = appContainer.deadlineNotificationManager
                    )
                )
                GroupDetailScreen(
                    viewModel = groupDetailViewModel,
                    photoStorageManager = appContainer.photoStorageManager,
                    ocrEngine = appContainer.ocrEngine,
                    folderSuggestEngine = appContainer.folderSuggestEngine,
                    onBackClick = { safePopBack() },
                    onOpenCanvasNote = { cId, fId, sId ->
                        backStack.add(CanvasNoteNavKey(canvasId = cId, folderId = fId, subfolderId = sId))
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            entry<TrashNavKey> {
                val trashViewModel: TrashViewModel = viewModel(
                    factory = TrashViewModel.provideFactory(
                        folderRepository = appContainer.folderRepository,
                        photoRepository = appContainer.photoRepository,
                        workspaceRepository = appContainer.workspaceRepository
                    )
                )
                TrashScreen(
                    viewModel = trashViewModel,
                    onBackClick = { safePopBack() },
                    modifier = Modifier.fillMaxSize()
                )
            }

            entry<SettingsNavKey> {
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.provideFactory(
                        settingsRepository = appContainer.settingsRepository,
                        deviceRegistry = appContainer.deviceRegistry,
                        friendsRepository = appContainer.friendsRepository
                    )
                )
                SettingsScreen(
                    viewModel = settingsViewModel,
                    spaceRepository = appContainer.spaceRepository,
                    workspaceRepository = appContainer.workspaceRepository,
                    friendsRepository = appContainer.friendsRepository,
                    onBackClick = { safePopBack() },
                    onNavigateToTrash = { backStack.add(TrashNavKey) },
                    onOpenUpdateScreen = { backStack.add(UpdateNavKey) },
                    onOpenWhatsNew = { backStack.add(WhatsNewNavKey) },
                    modifier = Modifier.fillMaxSize()
                )
            }

            entry<DocxViewerNavKey> { key ->
                var docNote by remember { mutableStateOf<com.arinara.fotara.data.model.DocumentNote?>(null) }
                val coroutineScope = rememberCoroutineScope()
                LaunchedEffect(key.documentId) {
                    docNote = appContainer.documentRepository.getDocumentNoteById(key.documentId)
                }
                docNote?.let { doc ->
                    com.arinara.fotara.ui.document.DocxViewerScreen(
                        documentNote = doc,
                        onBack = { safePopBack() },
                        onDelete = {
                            coroutineScope.launch {
                                appContainer.documentRepository.deleteDocumentNote(doc.id)
                                safePopBack()
                            }
                        }
                    )
                }
            }

            entry<PdfViewerNavKey> { key ->
                var docNote by remember { mutableStateOf<com.arinara.fotara.data.model.DocumentNote?>(null) }
                var pages by remember { mutableStateOf<List<com.arinara.fotara.data.model.DocumentPage>>(emptyList()) }
                val context = androidx.compose.ui.platform.LocalContext.current
                val coroutineScope = rememberCoroutineScope()
                LaunchedEffect(key.documentId) {
                    docNote = appContainer.documentRepository.getDocumentNoteById(key.documentId)
                    appContainer.documentRepository.getDocumentPages(key.documentId).collect {
                        pages = it
                    }
                }
                docNote?.let { doc ->
                    com.arinara.fotara.ui.document.PdfViewerScreen(
                        documentNote = doc,
                        pages = pages,
                        initialPageIndex = 0,
                        pdfPagePinRepository = appContainer.pdfPagePinRepository,
                        pdfPageDrawingRepository = appContainer.pdfPageDrawingRepository,
                        settingsRepository = appContainer.settingsRepository,
                        onBack = { safePopBack() },
                        onSplitToImages = {},
                        onDelete = {
                            coroutineScope.launch {
                                appContainer.documentRepository.deleteDocumentNote(doc.id)
                                safePopBack()
                            }
                        },
                        onShare = {
                            try {
                                val file = java.io.File(doc.originFileUri)
                                val shareUri = androidx.core.content.FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(android.content.Intent.EXTRA_STREAM, shareUri)
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share PDF"))
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Share failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            entry<TextNoteEditorNavKey> { key ->
                var textNoteToShare by remember { mutableStateOf<com.arinara.fotara.data.model.TextNote?>(null) }
                com.arinara.fotara.ui.note.TextNoteEditorScreen(
                    noteId = key.noteId,
                    folderId = key.folderId,
                    subfolderId = key.subfolderId,
                    textNoteRepository = appContainer.textNoteRepository,
                    documentRepository = appContainer.documentRepository,
                    onBack = { safePopBack() },
                    onShare = { note -> textNoteToShare = note },
                    highlightQuery = key.highlightQuery,
                    audioAnnotationRepository = appContainer.audioAnnotationRepository,
                    onOpenDocument = { docId, isDocx ->
                        if (isDocx) {
                            backStack.add(DocxViewerNavKey(docId))
                        } else {
                            backStack.add(PdfViewerNavKey(docId))
                        }
                    }
                )
                textNoteToShare?.let { note ->
                    val context = androidx.compose.ui.platform.LocalContext.current
                    com.arinara.fotara.ui.components.TextNoteShareDialog(
                        note = note,
                        onDismiss = { textNoteToShare = null },
                        onShareMarkdown = {
                            textNoteToShare = null
                            com.arinara.fotara.util.TextNoteExporter.shareNoteAsMarkdown(context, note)
                        },
                        onSharePlainText = {
                            textNoteToShare = null
                            com.arinara.fotara.util.TextNoteExporter.shareNoteAsPlainText(context, note)
                        }
                    )
                }
            }

            entry<CanvasNoteNavKey> { key ->
                val canvasViewModel: com.arinara.fotara.ui.canvas.CanvasViewModel = viewModel(
                    key = "canvas_${key.canvasId}_${key.folderId}_${key.subfolderId}",
                    factory = com.arinara.fotara.ui.canvas.CanvasViewModelFactory(
                        canvasId = key.canvasId,
                        folderId = key.folderId,
                        subfolderId = key.subfolderId,
                        canvasNoteRepository = appContainer.canvasNoteRepository,
                        canvasRepository = appContainer.canvasRepository,
                        canvasAssetManager = appContainer.canvasAssetManager,
                        photoRepository = appContainer.photoRepository,
                        folderRepository = appContainer.folderRepository,
                        settingsRepository = appContainer.settingsRepository,
                        scheduleManager = appContainer.noteScheduleManager,
                        workspaceRepository = appContainer.workspaceRepository
                    )
                )
                com.arinara.fotara.ui.canvas.CanvasScreen(
                    viewModel = canvasViewModel,
                    onBack = { safePopBack() }
                )
            }

            entry<UpdateNavKey> {
                com.arinara.fotara.online.UpdateScreen(
                    updateManager = appContainer.updateManager,
                    downgradeManager = appContainer.downgradeManager,
                    settingsRepository = appContainer.settingsRepository,
                    onClose = { safePopBack() },
                    onSkipVersion = { safePopBack() }
                )
            }

            entry<SupportNavKey> {
                com.arinara.fotara.ui.support.SupportScreen(
                    onBack = { safePopBack() }
                )
            }

            entry<WhatsNewNavKey> {
                com.arinara.fotara.online.WhatsNewScreen(
                    updateManager = appContainer.updateManager,
                    onBack = { safePopBack() }
                )
            }

            entry<PdfPageEditorNavKey> { key ->
                com.arinara.fotara.ui.document.PdfPageEditorScreen(
                    documentId = key.documentId,
                    pageIndex = key.pageIndex,
                    documentRepository = appContainer.documentRepository,
                    pdfPageDrawingRepository = appContainer.pdfPageDrawingRepository,
                    onBack = { safePopBack() },
                    modifier = Modifier.fillMaxSize()
                )
            }

            entry<SpaceSettingsNavKey> { key ->
                com.arinara.fotara.ui.space.SpaceSettingsScreen(
                    spaceId = key.spaceId,
                    spaceRepository = appContainer.spaceRepository,
                    onBack = { safePopBack() },
                    modifier = Modifier.fillMaxSize()
                )
            }

            entry<DiagnosticNavKey> {
                DiagnosticInfoScreen(
                    onBack = { safePopBack() },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    )
}
