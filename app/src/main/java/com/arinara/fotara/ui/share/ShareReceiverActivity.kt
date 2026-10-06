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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.arinara.fotara.data.DefaultAppContainer
import com.arinara.fotara.legal.LegalConsentManager
import com.arinara.fotara.legal.LegalDocumentLoader
import com.arinara.fotara.theme.FotaraTheme
import com.arinara.fotara.ui.legal.ConsentDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ShareReceiverActivity : ComponentActivity() {

    private val appContainer by lazy {
        DefaultAppContainer(applicationContext)
    }

    private val consentManager by lazy {
        LegalConsentManager(applicationContext)
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
                var isConsentGranted by remember { mutableStateOf(consentManager.isConsentValid()) }

                Box(modifier = Modifier.fillMaxSize()) {
                    if (isConsentGranted) {
                        SharePlacementScreen(
                            viewModel = viewModel,
                            onFinish = { finish() }
                        )
                    } else {
                        ConsentDialog(
                            onAccept = { deviceCountEnabled ->
                                consentManager.recordConsent(
                                    privacyVersion = LegalDocumentLoader.BUNDLED_PRIVACY_VERSION,
                                    termsVersion = LegalDocumentLoader.BUNDLED_TERMS_VERSION
                                )
                                CoroutineScope(Dispatchers.IO).launch {
                                    appContainer.settingsRepository.updateDeviceCountEnabled(deviceCountEnabled)
                                    if (deviceCountEnabled) {
                                        appContainer.deviceRegistry.sendDevicePing()
                                    }
                                }
                                isConsentGranted = true
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.processIncomingIntent(intent)
    }
}
