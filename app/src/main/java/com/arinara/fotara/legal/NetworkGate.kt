// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.legal

import android.content.Context

/**
 * Universal gate ensuring that NO network communication takes place prior to
 * valid user acceptance of the Privacy Policy and Terms of Service.
 */
object NetworkGate {

    @Volatile
    private var testOverride: Boolean? = null

    fun isConsentGranted(context: Context): Boolean {
        testOverride?.let { return it }
        return LegalConsentManager(context).isConsentValid()
    }

    fun isConsentGranted(consentManager: LegalConsentManager): Boolean {
        testOverride?.let { return it }
        return consentManager.isConsentValid()
    }

    // For unit tests
    internal fun setTestOverride(override: Boolean?) {
        testOverride = override
    }
}
