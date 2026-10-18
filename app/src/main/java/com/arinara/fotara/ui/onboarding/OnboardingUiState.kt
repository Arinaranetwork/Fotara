// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.onboarding

enum class OnboardingStep {
    PERMISSIONS,
    STARTER_FOLDERS
}

data class StarterFolderItem(
    val name: String,
    val colorHex: String,
    val description: String
)

data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.PERMISSIONS,
    val isCameraPermissionGranted: Boolean = false,
    val isStoragePermissionGranted: Boolean = false,
    val isCreatingFolders: Boolean = false,
    val starterFolders: List<StarterFolderItem> = listOf(
        StarterFolderItem("Math", "#4CC9F0", "Formulas, theorems, problem sets"),
        StarterFolderItem("Science", "#2EC4B6", "Lab notes, diagrams, experiment data"),
        StarterFolderItem("History", "#FFB703", "Timelines, primary sources, study guides"),
        StarterFolderItem("Literature", "#E63946", "Essays, reading analysis, lecture notes")
    )
)
