// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.legal

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap

class ConsentValidityTest {

    private class MockSharedPreferences : SharedPreferences {
        private val data = ConcurrentHashMap<String, Any>()

        override fun getAll(): MutableMap<String, *> = data
        override fun getString(key: String, defValue: String?): String? = data[key] as? String ?: defValue
        override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? = data[key] as? Set<String> ?: defValues
        override fun getInt(key: String, defValue: Int): Int = data[key] as? Int ?: defValue
        override fun getLong(key: String, defValue: Long): Long = data[key] as? Long ?: defValue
        override fun getFloat(key: String, defValue: Float): Float = data[key] as? Float ?: defValue
        override fun getBoolean(key: String, defValue: Boolean): Boolean = data[key] as? Boolean ?: defValue
        override fun contains(key: String): Boolean = data.containsKey(key)
        override fun edit(): SharedPreferences.Editor = Editor()
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        inner class Editor : SharedPreferences.Editor {
            private val temp = mutableMapOf<String, Any?>()
            private var clear = false

            override fun putString(key: String, value: String?): SharedPreferences.Editor { temp[key] = value; return this }
            override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor { temp[key] = values; return this }
            override fun putInt(key: String, value: Int): SharedPreferences.Editor { temp[key] = value; return this }
            override fun putLong(key: String, value: Long): SharedPreferences.Editor { temp[key] = value; return this }
            override fun putFloat(key: String, value: Float): SharedPreferences.Editor { temp[key] = value; return this }
            override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor { temp[key] = value; return this }
            override fun remove(key: String): SharedPreferences.Editor { temp[key] = null; return this }
            override fun clear(): SharedPreferences.Editor { clear = true; return this }
            override fun commit(): Boolean { apply(); return true }
            override fun apply() {
                if (clear) data.clear()
                for ((k, v) in temp) {
                    if (v == null) data.remove(k) else data[k] = v
                }
            }
        }
    }

    private class MockContext(private val prefs: SharedPreferences) : android.content.ContextWrapper(null) {
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences = prefs
        override fun getApplicationContext(): Context = this
    }

    private lateinit var mockPrefs: MockSharedPreferences
    private lateinit var mockContext: Context
    private lateinit var consentManager: LegalConsentManager

    @Before
    fun setUp() {
        mockPrefs = MockSharedPreferences()
        mockContext = MockContext(mockPrefs)
        consentManager = LegalConsentManager(mockContext)
    }

    @Test
    fun testFreshInstallHasNoConsent() {
        assertFalse("Fresh install with no stored records must be invalid", consentManager.isConsentValid())
    }

    @Test
    fun testPartialOrOlderConsentIsInvalid() {
        // Privacy accepted at version 0, terms version 1
        mockPrefs.edit()
            .putInt(LegalConsentManager.KEY_PRIVACY_VERSION, 0)
            .putInt(LegalConsentManager.KEY_TERMS_VERSION, 1)
            .putLong(LegalConsentManager.KEY_ACCEPTED_TIMESTAMP, 1000L)
            .apply()

        assertFalse("Older privacy version must fail validation", consentManager.isConsentValid(requiredPrivacyVersion = 1, requiredTermsVersion = 1))

        // Privacy 1, terms 0
        mockPrefs.edit()
            .putInt(LegalConsentManager.KEY_PRIVACY_VERSION, 1)
            .putInt(LegalConsentManager.KEY_TERMS_VERSION, 0)
            .putLong(LegalConsentManager.KEY_ACCEPTED_TIMESTAMP, 1000L)
            .apply()

        assertFalse("Older terms version must fail validation", consentManager.isConsentValid(requiredPrivacyVersion = 1, requiredTermsVersion = 1))
    }

    @Test
    fun testEqualOrNewerConsentIsValid() {
        // Equal version accepted
        consentManager.recordConsent(privacyVersion = 1, termsVersion = 1)
        assertTrue("Matching versions must be valid", consentManager.isConsentValid(1, 1))

        // Newer version accepted
        consentManager.recordConsent(privacyVersion = 2, termsVersion = 2)
        assertTrue("Newer versions must be valid", consentManager.isConsentValid(1, 1))
    }

    @Test
    fun testVersionBumpRequiresNewConsent() {
        // User accepted v1
        consentManager.recordConsent(privacyVersion = 1, termsVersion = 1)
        assertTrue(consentManager.isConsentValid(1, 1))

        // App updates and requires v2 of Privacy Policy
        assertFalse("Bump in required privacy version must require re-consent", consentManager.isConsentValid(requiredPrivacyVersion = 2, requiredTermsVersion = 1))

        // App updates and requires v2 of Terms of Service
        assertFalse("Bump in required terms version must require re-consent", consentManager.isConsentValid(requiredPrivacyVersion = 1, requiredTermsVersion = 2))
    }
}
