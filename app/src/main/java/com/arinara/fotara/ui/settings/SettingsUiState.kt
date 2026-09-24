// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.settings

import com.arinara.fotara.data.model.StorageBreakdown
import com.arinara.fotara.data.model.UserSettings

data class SettingsUiState(
    val userSettings: UserSettings = UserSettings(),
    val storageBreakdown: StorageBreakdown = StorageBreakdown(),
    val isLoadingStorage: Boolean = false,
    val isRebuildingThumbnails: Boolean = false,
    val isRebuildingSearchIndex: Boolean = false,
    val isExportingBackup: Boolean = false,
    val isImportingBackup: Boolean = false,
    val exportedJsonString: String? = null,
    val showExportDialog: Boolean = false,
    val showImportDialog: Boolean = false,
    val showLicensesDialog: Boolean = false,
    val feedbackMessage: String? = null
)
