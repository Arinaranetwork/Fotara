// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.share

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.arinara.fotara.data.DefaultAppContainer
import com.arinara.fotara.theme.FotaraTheme

class ShareReceiverActivity : ComponentActivity() {

    private val appContainer by lazy {
        DefaultAppContainer(applicationContext)
    }

    private val viewModel: SharePlacementViewModel by viewModels {
        SharePlacementViewModelFactory(
            appContext = applicationContext,
            folderRepository = appContainer.folderRepository,
            photoRepository = appContainer.photoRepository,
            documentRepository = appContainer.documentRepository,
            textNoteRepository = appContainer.textNoteRepository,
            photoStorageManager = appContainer.photoStorageManager,
            settingsRepository = appContainer.settingsRepository,
            workspaceRepository = appContainer.workspaceRepository
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (intent != null) {
            viewModel.processIncomingIntent(intent)
        }

        setContent {
            FotaraTheme {
                SharePlacementScreen(
                    viewModel = viewModel,
                    onFinish = { finish() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.processIncomingIntent(intent)
    }
}
