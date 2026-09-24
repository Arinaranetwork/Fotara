// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
    deepLinkDirectView: Boolean = false
) {
    val initialKey = remember {
        if (appContainer.settingsRepository.isOnboardingCompleted()) HomeNavKey else OnboardingNavKey
    }
    val backStack = rememberNavBackStack(initialKey)

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

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
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
                        settingsRepository = appContainer.settingsRepository
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
                    onOpenTrash = {
                        backStack.add(TrashNavKey)
                    },
                    onOpenSettings = {
                        backStack.add(SettingsNavKey)
                    },
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
                        settingsRepository = appContainer.settingsRepository
                    )
                )
                FolderDetailScreen(
                    viewModel = folderDetailViewModel,
                    photoStorageManager = appContainer.photoStorageManager,
                    ocrEngine = appContainer.ocrEngine,
                    folderSuggestEngine = appContainer.folderSuggestEngine,
                    onBackClick = { backStack.removeLastOrNull() },
                    openViewerDirectly = key.openViewerDirectly,
                    onOpenGroup = { fId, gId, targetPhotoId ->
                        backStack.add(GroupDetailNavKey(folderId = fId, groupId = gId, targetPhotoId = targetPhotoId))
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
                        ocrEngine = appContainer.ocrEngine
                    )
                )
                GroupDetailScreen(
                    viewModel = groupDetailViewModel,
                    onBackClick = { backStack.removeLastOrNull() },
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
                    onBackClick = { backStack.removeLastOrNull() },
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
                    onBackClick = { backStack.removeLastOrNull() },
                    onNavigateToTrash = { backStack.add(TrashNavKey) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    )
}
