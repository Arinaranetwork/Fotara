// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.note.math

import android.content.Context
import com.arinara.fotara.feature.packages.loader.FotaraPackageManager

/**
 * Accessor for the modular LaTeX typesetting add-on package lifecycle.
 * Dictates whether mathematical parsing, toolbar fx actions, and live rendering are enabled.
 */
object LatexAddonManager {
    const val PACKAGE_ID = "addon.latex.math"

    /**
     * Checks if the LaTeX math add-on is installed on device.
     */
    fun isInstalled(context: Context): Boolean {
        return try {
            FotaraPackageManager.getInstance(context).isPackageInstalled(PACKAGE_ID)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Checks if the LaTeX math add-on is installed using an existing [FotaraPackageManager] instance.
     */
    fun isInstalled(packageManager: FotaraPackageManager): Boolean {
        return packageManager.isPackageInstalled(PACKAGE_ID)
    }
}
