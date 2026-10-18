// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.coordinator

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arinara.fotara.data.AppContainer
import com.arinara.fotara.legal.LegalConsentManager
import com.arinara.fotara.legal.LegalDocumentLoader
import com.arinara.fotara.ui.components.NewUpdateDialog
import com.arinara.fotara.ui.legal.ConsentDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Root host composable placed at the root of MainActivity above navigation.
 * Renders at most one coordinated dialog at a time according to coordinator state.
 */
@Composable
fun AppDialogCoordinatorHost(
    appContainer: AppContainer,
    modifier: Modifier = Modifier
) {
    val coordinator = appContainer.dialogCoordinator
    val state by coordinator.state.collectAsStateWithLifecycle()

    if (!state.isDialogRenderable) return

    when (val dialog = state.visibleDialog) {
        is AppDialogRequest.Consent -> {
            ConsentDialog(
                onAccept = { deviceCountEnabled ->
                    // 1. Record consent record
                    val legalConsentManager = LegalConsentManager(appContainer.context)
                    legalConsentManager.recordConsent(
                        privacyVersion = LegalDocumentLoader.BUNDLED_PRIVACY_VERSION,
                        termsVersion = LegalDocumentLoader.BUNDLED_TERMS_VERSION
                    )

                    // 2. Persist device count switch setting
                    CoroutineScope(Dispatchers.IO).launch {
                        appContainer.settingsRepository.updateDeviceCountEnabled(deviceCountEnabled)
                        if (deviceCountEnabled) {
                            appContainer.deviceRegistry.sendDevicePing()
                        }
                    }

                    // 3. Dismiss consent dialog from coordinator
                    coordinator.dismissDialog(dialog.key)
                },
                modifier = modifier
            )
        }

        is AppDialogRequest.Update -> {
            NewUpdateDialog(
                release = dialog.release,
                onLater = {
                    dialog.onLater()
                    coordinator.dismissDialog(dialog.key)
                },
                onSkipVersion = {
                    dialog.onSkipVersion()
                    coordinator.dismissDialog(dialog.key)
                },
                modifier = modifier
            )
        }

        null -> {}
    }
}
