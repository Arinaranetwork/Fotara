// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.online

import android.content.Context
import android.content.SharedPreferences
import com.arinara.fotara.data.model.DownsampleQuality
import com.arinara.fotara.data.model.ImportResult
import com.arinara.fotara.data.model.RecentDestination
import com.arinara.fotara.data.model.SortOrder
import com.arinara.fotara.data.model.StorageBreakdown
import com.arinara.fotara.data.model.StorageLocation
import com.arinara.fotara.data.model.ThemeMode
import com.arinara.fotara.data.model.UserProfile
import com.arinara.fotara.data.model.UserSettings
import com.arinara.fotara.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class DeviceRegistryTest {

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

    private class DummySettingsRepository : SettingsRepository {
        private val _settingsFlow = MutableStateFlow(UserSettings(isDeviceCountEnabled = true))
        override val settingsFlow: StateFlow<UserSettings> = _settingsFlow.asStateFlow()
        override val profileFlow: StateFlow<UserProfile> = MutableStateFlow(UserProfile()).asStateFlow()

        override suspend fun updateDeviceCountEnabled(enabled: Boolean) {
            _settingsFlow.value = _settingsFlow.value.copy(isDeviceCountEnabled = enabled)
        }

        override suspend fun updateProfileName(name: String) {}
        override suspend fun updateProfileEmail(email: String) {}
        override suspend fun updateProfileBorder(borderId: String) {}
        override suspend fun saveProfileAvatar(bitmap: android.graphics.Bitmap): String? = null
        override suspend fun removeProfileAvatar() {}
        override suspend fun saveProfileBanner(bitmap: android.graphics.Bitmap): String? = null
        override suspend fun saveProfileBannerGif(bytes: ByteArray, crop: String?): String? = null
        override suspend fun removeProfileBanner() {}
        override suspend fun updateSortOrder(sortOrder: SortOrder) {}
        override suspend fun updateGridDensity(density: Int) {}
        override suspend fun updateThemeMode(themeMode: ThemeMode) {}
        override suspend fun updateAutoOcr(enabled: Boolean) {}
        override suspend fun updateOcrLanguage(language: String) {}
        override suspend fun updateDownsampleQuality(quality: DownsampleQuality) {}
        override suspend fun updateReminderLeadTime(hours: Int) {}
        override suspend fun updateDueTomorrowRibbon(enabled: Boolean) {}
        override suspend fun updateStorageLocation(location: StorageLocation) {}
        override suspend fun getStorageBreakdown(): StorageBreakdown = StorageBreakdown()
        override suspend fun rebuildThumbnails(): Int = 0
        override suspend fun rebuildSearchIndex(): Int = 0
        override suspend fun exportDataBackup(): String = "{}"
        override suspend fun importDataBackup(jsonString: String): ImportResult = ImportResult(true, 0, 0, "")
        override fun isOnboardingCompleted(): Boolean = true
        override suspend fun setOnboardingCompleted(completed: Boolean) {}
        override fun getRecentSearches(): List<String> = emptyList()
        override suspend fun addRecentSearch(query: String) {}
        override suspend fun removeRecentSearch(query: String) {}
        override suspend fun clearRecentSearches() {}
        override fun getRecentDestinations(): List<RecentDestination> = emptyList()
        override suspend fun addRecentDestination(destination: RecentDestination) {}
        override suspend fun clearRecentDestinations() {}
        override suspend fun updateAutoCheckUpdates(enabled: Boolean) {}
        override suspend fun updateOptInCrashReporting(enabled: Boolean) {}
        override suspend fun updateCombineFileNamePreset(preset: String) {}
        override suspend fun updateSavedImageLocation(locationKey: String, customName: String) {}
    }

    private lateinit var mockPrefs: MockSharedPreferences
    private lateinit var mockContext: Context
    private lateinit var settingsRepository: DummySettingsRepository
    private lateinit var deviceRegistry: DeviceRegistry

    @Before
    fun setUp() {
        mockPrefs = MockSharedPreferences()
        mockContext = MockContext(mockPrefs)
        settingsRepository = DummySettingsRepository()
        deviceRegistry = DeviceRegistry(
            context = mockContext,
            settingsRepository = settingsRepository,
            scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        )
    }

    @Test
    fun testDeviceIdCreationAndFormat() {
        assertNull(deviceRegistry.getLocalDeviceId())
        val id = deviceRegistry.getOrCreateDeviceId()
        assertNotNull("Generated device ID must not be null", id)
        assertEquals("Generated ID must persist", id, deviceRegistry.getLocalDeviceId())

        // Verify it is a valid UUID v4
        val parsedUuid = UUID.fromString(id)
        assertEquals(4, parsedUuid.version())
    }

    @Test
    fun testDefaultConstantGovernsSetting() {
        assertTrue("SettingsRepository.DEVICE_COUNT_DEFAULT_ENABLED must be true by default", SettingsRepository.DEVICE_COUNT_DEFAULT_ENABLED)
        val defaultSettings = UserSettings()
        assertTrue("UserSettings default must match DEVICE_COUNT_DEFAULT_ENABLED", defaultSettings.isDeviceCountEnabled)
    }

    @Test
    fun testDisablingDeviceCountDeletesLocalIdAndQueuesUnregister() = kotlinx.coroutines.test.runTest {
        val initialId = deviceRegistry.getOrCreateDeviceId()
        assertEquals(initialId, deviceRegistry.getLocalDeviceId())

        // Disable device count
        settingsRepository.updateDeviceCountEnabled(false)
        deviceRegistry.onDeviceCountDisabled()

        // Local ID and statistics must be deleted immediately
        assertNull("Local device ID must be deleted when count is disabled", deviceRegistry.getLocalDeviceId())
        assertEquals(0L, deviceRegistry.getLastSentTimestamp())
        assertNull(deviceRegistry.getLastSentVersion())

        // Pending unregister queue must contain the old ID
        assertEquals("Pending unregister must record the old ID", initialId, deviceRegistry.getPendingUnregisterId())

        // Re-enabling device count creates a brand NEW ID
        settingsRepository.updateDeviceCountEnabled(true)
        val newId = deviceRegistry.getOrCreateDeviceId()
        assertNotNull(newId)
        assertNotEquals("New ID must be different from previous ID", initialId, newId)
    }

    @Test
    fun testDeviceIdFlowEmitsOnGenerationAndRevocation() = kotlinx.coroutines.test.runTest {
        // Initial state before generation is null
        assertNull("Initial deviceIdFlow value must be null", deviceRegistry.deviceIdFlow.value)

        // Generating ID updates flow immediately
        val id = deviceRegistry.getOrCreateDeviceId()
        assertEquals("deviceIdFlow must reflect generated ID", id, deviceRegistry.deviceIdFlow.value)

        // Disabling device count clears flow to null
        deviceRegistry.onDeviceCountDisabled()
        assertNull("deviceIdFlow must become null when disabled", deviceRegistry.deviceIdFlow.value)

        // Clearing for testing clears flow to null
        val id2 = deviceRegistry.getOrCreateDeviceId()
        assertEquals(id2, deviceRegistry.deviceIdFlow.value)
        deviceRegistry.clearLocalDataForTesting()
        assertNull("deviceIdFlow must become null after clearLocalDataForTesting", deviceRegistry.deviceIdFlow.value)
    }
}
