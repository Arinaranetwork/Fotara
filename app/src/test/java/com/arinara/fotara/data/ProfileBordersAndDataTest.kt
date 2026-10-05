// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data

import com.arinara.fotara.data.model.ProfileBorders
import com.arinara.fotara.data.model.UserProfile
import com.arinara.fotara.test.FakeSettingsRepository
import com.arinara.fotara.ui.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.max

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileBordersAndDataTest {

    private lateinit var fakeRepo: FakeSettingsRepository
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeSettingsRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun profileBorders_allHaveValidTableEntriesAndRatios() {
        val all = ProfileBorders.ALL_BORDERS
        assertTrue("At least 4 border entries must exist", all.size >= 4)

        // Verify "none" border
        val noneBorder = all.first { it.id == ProfileBorders.NONE_ID }
        assertEquals("None", noneBorder.displayName)
        assertEquals(0, noneBorder.drawableRes)
        assertEquals(1.0f, noneBorder.innerRatio, 0.0001f)

        // Verify each border has valid configuration
        for (border in all) {
            assertTrue("Border ID must not be blank", border.id.isNotBlank())
            assertTrue("Border display name must not be blank", border.displayName.isNotBlank())
            assertTrue(
                "Inner ratio for ${border.id} must be between 0.5 and 1.0, was ${border.innerRatio}",
                border.innerRatio in 0.5f..1.0f
            )
            if (border.id != ProfileBorders.NONE_ID) {
                assertTrue("Custom border must have non-zero drawable resource", border.drawableRes != 0)
            }
        }
    }

    @Test
    fun profileBorders_specificMeasuredRatiosMatch() {
        val neon = ProfileBorders.getById("file_000000000278820bb0a8901e8fa6612b")
        assertEquals(0.7209f, neon.innerRatio, 0.005f)

        val arc = ProfileBorders.getById("file_000000001c6c820bbc0efdac44c0aa63")
        assertEquals(0.7014f, arc.innerRatio, 0.005f)

        val wings = ProfileBorders.getById("file_000000007fdc81f7b74b9a79e2c63215")
        assertEquals(0.6511f, wings.innerRatio, 0.005f)
    }

    @Test
    fun profileBorders_getByIdFallbackToNone() {
        assertEquals(ProfileBorders.NONE, ProfileBorders.getById(null))
        assertEquals(ProfileBorders.NONE, ProfileBorders.getById(""))
        assertEquals(ProfileBorders.NONE, ProfileBorders.getById("unknown_border_id"))
    }

    @Test
    fun userProfile_defaultNameFallbackAndProperties() {
        val emptyProfile = UserProfile()
        assertEquals("Fotara User", emptyProfile.resolvedName("Fotara User"))
        assertEquals("", emptyProfile.email)
        assertFalse(emptyProfile.hasCustomAvatar)
        assertFalse(emptyProfile.hasCustomBanner)
        assertFalse(emptyProfile.hasBorder)

        val customProfile = UserProfile(
            name = "  Arinara  ",
            email = "test@example.com",
            avatarPath = "/path/to/avatar.webp",
            bannerPath = "/path/to/banner.webp",
            borderId = "file_000000000278820bb0a8901e8fa6612b"
        )
        assertEquals("Arinara", customProfile.resolvedName())
        assertEquals("test@example.com", customProfile.email)
        assertTrue(customProfile.hasCustomAvatar)
        assertTrue(customProfile.hasCustomBanner)
        assertTrue(customProfile.hasBorder)
    }

    @Test
    fun settingsViewModel_profileUpdatesEmitThroughFlow() = runTest(testDispatcher) {
        val viewModel = SettingsViewModel(fakeRepo)
        advanceUntilIdle()

        viewModel.updateProfileName(" Jane Doe ")
        advanceUntilIdle()
        assertEquals("Jane Doe", viewModel.uiState.value.userProfile.name)

        viewModel.updateProfileEmail(" jane@doe.com ")
        advanceUntilIdle()
        assertEquals("jane@doe.com", viewModel.uiState.value.userProfile.email)

        viewModel.updateProfileBorder("file_000000001c6c820bbc0efdac44c0aa63")
        advanceUntilIdle()
        assertEquals("file_000000001c6c820bbc0efdac44c0aa63", viewModel.uiState.value.userProfile.borderId)
    }

    @Test
    fun cropMath_coverScaleGuaranteesNoEmptyGaps() {
        val frameW = 600f
        val frameH = 400f

        // Case 1: Image wider than frame aspect ratio
        val bmp1W = 1200f
        val bmp1H = 600f
        val coverScale1 = max(frameW / bmp1W, frameH / bmp1H)
        val rendered1W = bmp1W * coverScale1
        val rendered1H = bmp1H * coverScale1
        assertTrue("Rendered width must cover frame", rendered1W >= frameW)
        assertTrue("Rendered height must cover frame", rendered1H >= frameH)

        // Case 2: Image taller than frame aspect ratio
        val bmp2W = 800f
        val bmp2H = 1600f
        val coverScale2 = max(frameW / bmp2W, frameH / bmp2H)
        val rendered2W = bmp2W * coverScale2
        val rendered2H = bmp2H * coverScale2
        assertTrue("Rendered width must cover frame", rendered2W >= frameW)
        assertTrue("Rendered height must cover frame", rendered2H >= frameH)

        // Pan clamping math
        val maxPanX = (rendered2W - frameW) / 2f
        val maxPanY = (rendered2H - frameH) / 2f
        assertTrue("Max pan must be non-negative", maxPanX >= 0f)
        assertTrue("Max pan must be non-negative", maxPanY >= 0f)
    }

    @Test
    fun saveProfileBannerGif_updatesPathAndCropAndLifecycle() = testScope.runTest {
        val viewModel = SettingsViewModel(fakeRepo)
        val gifBytes = "GIF89a...fake_gif_data".toByteArray()
        val cropRect = "0.1,0.2,0.9,0.8"

        viewModel.saveProfileBannerGif(gifBytes, cropRect)
        advanceUntilIdle()

        val profile = viewModel.uiState.value.userProfile
        assertEquals("/fake/files/profile/banner.gif", profile.bannerPath)
        assertEquals(cropRect, profile.bannerCrop)
        assertTrue(profile.isBannerGif)

        // Remove clears all
        viewModel.removeProfileBanner()
        advanceUntilIdle()

        val emptyProfile = viewModel.uiState.value.userProfile
        assertEquals(null, emptyProfile.bannerPath)
        assertEquals(null, emptyProfile.bannerCrop)
        assertFalse(emptyProfile.isBannerGif)
    }
}

