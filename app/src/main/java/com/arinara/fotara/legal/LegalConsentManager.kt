// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.legal

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages persisted legal consent records in fotara_legal_prefs.
 * Consent is valid only when accepted privacy and terms versions are at least
 * equal to the bundled document versions.
 */
class LegalConsentManager(private val context: Context) {

    companion object {
        const val PREFS_NAME = "fotara_legal_prefs"
        const val KEY_PRIVACY_VERSION = "accepted_privacy_version"
        const val KEY_TERMS_VERSION = "accepted_terms_version"
        const val KEY_ACCEPTED_TIMESTAMP = "accepted_timestamp"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isConsentValid(
        requiredPrivacyVersion: Int = LegalDocumentLoader.BUNDLED_PRIVACY_VERSION,
        requiredTermsVersion: Int = LegalDocumentLoader.BUNDLED_TERMS_VERSION
    ): Boolean {
        val acceptedPrivacy = prefs.getInt(KEY_PRIVACY_VERSION, 0)
        val acceptedTerms = prefs.getInt(KEY_TERMS_VERSION, 0)
        val timestamp = prefs.getLong(KEY_ACCEPTED_TIMESTAMP, 0L)

        return timestamp > 0L &&
                acceptedPrivacy >= requiredPrivacyVersion &&
                acceptedTerms >= requiredTermsVersion
    }

    fun recordConsent(
        privacyVersion: Int = LegalDocumentLoader.BUNDLED_PRIVACY_VERSION,
        termsVersion: Int = LegalDocumentLoader.BUNDLED_TERMS_VERSION,
        timestamp: Long = System.currentTimeMillis()
    ) {
        prefs.edit()
            .putInt(KEY_PRIVACY_VERSION, privacyVersion)
            .putInt(KEY_TERMS_VERSION, termsVersion)
            .putLong(KEY_ACCEPTED_TIMESTAMP, timestamp)
            .apply()
    }

    fun getAcceptedPrivacyVersion(): Int = prefs.getInt(KEY_PRIVACY_VERSION, 0)

    fun getAcceptedTermsVersion(): Int = prefs.getInt(KEY_TERMS_VERSION, 0)

    fun getAcceptedTimestamp(): Long = prefs.getLong(KEY_ACCEPTED_TIMESTAMP, 0L)

    fun clearConsent() {
        prefs.edit().clear().apply()
    }
}
