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
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.arinara.fotara.data.AppContainer
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
    onUpdateScreenOpened: (() -> Unit)? = null
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
                        textNoteRepository = appContainer.textNoteRepository
                    )
                )
                HomeScreen(
                    viewModel = homeViewModel,
                    onFolderClick = { folder ->
                        backStack.add(FolderDetailNavKey(folder.id))
                    },
                    onNavigateToPhoto = { folderId, subfolderId, photoId ->
                        backStack.add(FolderDetailNavKey(folderId, subfolderId, photoId))
                    },
                    onNavigateToGroup = { folderId, subfolderId, groupId ->
                        backStack.add(FolderDetailNavKey(folderId = folderId, initialSubfolderId = subfolderId, targetGroupId = groupId))
                    },
                    onNavigateToTextNote = { folderId, noteId ->
                        backStack.add(TextNoteEditorNavKey(folderId = folderId, noteId = noteId))
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
                    feedbackManager = appContainer.feedbackManager,
                    modifier = Modifier.fillMaxSize()
                )
            }

            entry<FolderDetailNavKey> { key ->
                val folderDetailViewModel: FolderDetailViewModel = viewModel(
                    key = "folder_${key.folderId}_${key.initialSubfolderId}_${key.targetPhotoId}_${key.targetGroupId}",
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
                        settingsRepository = appContainer.settingsRepository,
                        documentRepository = appContainer.documentRepository,
                        textNoteRepository = appContainer.textNoteRepository
                    )
                )
                FolderDetailScreen(
                    viewModel = folderDetailViewModel,
                    photoStorageManager = appContainer.photoStorageManager,
                    ocrEngine = appContainer.ocrEngine,
                    folderSuggestEngine = appContainer.folderSuggestEngine,
                    onBackClick = { safePopBack() },
                    openViewerDirectly = key.openViewerDirectly,
                    onOpenGroup = { fId, gId, targetPhotoId ->
                        backStack.add(GroupDetailNavKey(folderId = fId, groupId = gId, targetPhotoId = targetPhotoId))
                    },
                    onOpenTextNote = { nId, fId, sId ->
                        backStack.add(TextNoteEditorNavKey(noteId = nId, folderId = fId, subfolderId = sId))
                    },
                    onOpenDocx = { dId ->
                        backStack.add(DocxViewerNavKey(documentId = dId))
                    },
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
                    modifier = Modifier.fillMaxSize()
                )
            }

            entry<TrashNavKey> {
                val trashViewModel: TrashViewModel = viewModel(
                    factory = TrashViewModel.provideFactory(
                        folderRepository = appContainer.folderRepository,
                        photoRepository = appContainer.photoRepository
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
                        settingsRepository = appContainer.settingsRepository
                    )
                )
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onBackClick = { safePopBack() },
                    onNavigateToTrash = { backStack.add(TrashNavKey) },
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

            entry<TextNoteEditorNavKey> { key ->
                var textNoteToShare by remember { mutableStateOf<com.arinara.fotara.data.model.TextNote?>(null) }
                com.arinara.fotara.ui.note.TextNoteEditorScreen(
                    noteId = key.noteId,
                    folderId = key.folderId,
                    subfolderId = key.subfolderId,
                    textNoteRepository = appContainer.textNoteRepository,
                    onBack = { safePopBack() },
                    onShare = { note -> textNoteToShare = note }
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

            entry<UpdateNavKey> {
                com.arinara.fotara.online.UpdateScreen(
                    updateManager = appContainer.updateManager,
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
        }
    )
}
