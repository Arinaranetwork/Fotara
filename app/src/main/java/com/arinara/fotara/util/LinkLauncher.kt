// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent

object LinkLauncher {

    /**
     * Opens a given URL immediately using Chrome Custom Tabs, falling back to the system browser.
     */
    fun openUrl(context: Context, rawUrl: String) {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return

        val formattedUrl = if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
            "https://$trimmed"
        } else {
            trimmed
        }

        val uri = try {
            Uri.parse(formattedUrl)
        } catch (_: Exception) {
            Toast.makeText(context, "Invalid URL: $rawUrl", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
            customTabsIntent.launchUrl(context, uri)
        } catch (_: Exception) {
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
            } catch (_: Exception) {
                Toast.makeText(context, "No browser available to open link", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
