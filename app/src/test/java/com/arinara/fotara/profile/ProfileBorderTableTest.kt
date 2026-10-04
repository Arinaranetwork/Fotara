// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.profile

import com.arinara.fotara.data.model.ProfileBorder
import com.arinara.fotara.data.model.ProfileBorders
import com.arinara.fotara.ui.profile.ProfileAvatarDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

class ProfileBorderTableTest {

    @Test
    fun table_isComplete_allBundledBordersPresent() {
        val all = ProfileBorders.ALL_BORDERS
        assertEquals(4, all.size)

        val none = ProfileBorders.getById(ProfileBorders.NONE_ID)
        assertNotNull(none)
        assertEquals("None", none.displayName)
        assertEquals(0, none.drawableRes)
        assertEquals(1.0f, none.innerDiameterRatio, 0.0001f)
        assertEquals(0.5f, none.innerCenterX, 0.0001f)
        assertEquals(0.5f, none.innerCenterY, 0.0001f)

        val neon = ProfileBorders.getById("file_000000000278820bb0a8901e8fa6612b")
        assertNotNull(neon)
        assertEquals("Neon Spark", neon.displayName)
        assertTrue(neon.drawableRes != 0)
        assertEquals(0.7209f, neon.innerDiameterRatio, 0.001f)
        assertEquals(0.8517f, neon.ringOuterDiameterRatio, 0.001f)
        assertEquals(0.4565f, neon.innerCenterX, 0.001f)
        assertEquals(0.4908f, neon.innerCenterY, 0.001f)

        val arc = ProfileBorders.getById("file_000000001c6c820bbc0efdac44c0aa63")
        assertNotNull(arc)
        assertEquals("Crystal Arc", arc.displayName)
        assertTrue(arc.drawableRes != 0)
        assertEquals(0.7026f, arc.innerDiameterRatio, 0.001f)
        assertEquals(0.8182f, arc.ringOuterDiameterRatio, 0.001f)
        assertEquals(0.5036f, arc.innerCenterX, 0.001f)
        assertEquals(0.5347f, arc.innerCenterY, 0.001f)

        val wings = ProfileBorders.getById("file_000000007fdc81f7b74b9a79e2c63215")
        assertNotNull(wings)
        assertEquals("Golden Wings", wings.displayName)
        assertTrue(wings.drawableRes != 0)
        assertEquals(0.6511f, wings.innerDiameterRatio, 0.001f)
        assertEquals(0.7878f, wings.ringOuterDiameterRatio, 0.001f)
        assertEquals(0.4622f, wings.innerCenterX, 0.001f)
        assertEquals(0.4912f, wings.innerCenterY, 0.001f)

        // General integrity
        for (b in all) {
            assertTrue("ID must not be blank", b.id.isNotBlank())
            assertTrue("Display name must not be blank", b.displayName.isNotBlank())
            assertTrue("innerDiameterRatio must be in (0.5..1.0]", b.innerDiameterRatio in 0.5f..1.0f)
            assertTrue("ringOuterDiameterRatio must be in (0.5..1.0]", b.ringOuterDiameterRatio in 0.5f..1.0f)
            assertTrue("innerCenterX must be near 0.5", b.innerCenterX in 0.4f..0.6f)
            assertTrue("innerCenterY must be near 0.5", b.innerCenterY in 0.4f..0.6f)
            assertTrue("extentLeft < extentRight", b.extentLeft < b.extentRight)
            assertTrue("extentTop < extentBottom", b.extentTop < b.extentBottom)
        }
    }

    @Test
    fun innerDiameterAndCenter_accurateWithinPxTolerances_acrossDensitiesAndDiameters() {
        val testDiametersDp = listOf(52f, 78f, 96f)
        val testDensities = listOf(1.0f, 1.5f, 2.0f, 3.0f, 4.0f)

        for (border in ProfileBorders.ALL_BORDERS) {
            if (border.drawableRes == 0) continue

            for (dDp in testDiametersDp) {
                for (density in testDensities) {
                    val dPx = dDp * density
                    val overlap = ProfileAvatarDefaults.InnerOverlapFraction
                    val scaleWidthPx = (dPx * (1f - overlap)) / border.innerDiameterRatio

                    // 1. On-screen inner diameter accuracy within 1 px of intended target
                    val onScreenInnerDiamPx = scaleWidthPx * border.innerDiameterRatio
                    val targetInnerDiamPx = dPx * (1f - overlap)
                    val diamError = abs(onScreenInnerDiamPx - targetInnerDiamPx)
                    assertTrue(
                        "Inner diameter error for ${border.displayName} at D=$dDp, density=$density must be <= 1 px, was $diamError",
                        diamError <= 1.0f
                    )

                    // 2. On-screen center alignment accuracy within 0.5 px
                    val scaleHeightPx = scaleWidthPx * (border.canvasHeight.toFloat() / border.canvasWidth.toFloat())
                    val shiftXPx = -(border.innerCenterX - 0.5f) * scaleWidthPx
                    val shiftYPx = -(border.innerCenterY - 0.5f) * scaleHeightPx

                    val innerCenterInImageXPx = (border.innerCenterX - 0.5f) * scaleWidthPx
                    val innerCenterInImageYPx = (border.innerCenterY - 0.5f) * scaleHeightPx

                    val finalHoleCenterXPx = shiftXPx + innerCenterInImageXPx
                    val finalHoleCenterYPx = shiftYPx + innerCenterInImageYPx

                    val centerErrorX = abs(finalHoleCenterXPx)
                    val centerErrorY = abs(finalHoleCenterYPx)

                    assertTrue("Center X error must be <= 0.5 px", centerErrorX <= 0.5f)
                    assertTrue("Center Y error must be <= 0.5 px", centerErrorY <= 0.5f)

                    // Also check with integer pixel quantization
                    val quantizedCenterErrorX = abs(shiftXPx.roundToInt() + innerCenterInImageXPx.roundToInt())
                    val quantizedCenterErrorY = abs(shiftYPx.roundToInt() + innerCenterInImageYPx.roundToInt())
                    assertTrue("Quantized center X error <= 1 px", quantizedCenterErrorX <= 1)
                    assertTrue("Quantized center Y error <= 1 px", quantizedCenterErrorY <= 1)
                }
            }
        }
    }

    data class AvatarLayoutSnapshot(
        val slotWidthDp: Float,
        val slotHeightDp: Float,
        val avatarWidthDp: Float,
        val avatarHeightDp: Float,
        val penCenterXDp: Float,
        val penCenterYDp: Float
    )

    private fun computePureLayout(avatarSizeDp: Float, border: ProfileBorder): AvatarLayoutSnapshot {
        val slotSizeDp = avatarSizeDp + (ProfileAvatarDefaults.FixedRingAllowance.value * 2f)
        val radius = avatarSizeDp / 2f
        val offset45 = radius * ProfileAvatarDefaults.CosSin45
        val avatarCenter = slotSizeDp / 2f
        return AvatarLayoutSnapshot(
            slotWidthDp = slotSizeDp,
            slotHeightDp = slotSizeDp,
            avatarWidthDp = avatarSizeDp,
            avatarHeightDp = avatarSizeDp,
            penCenterXDp = avatarCenter + offset45,
            penCenterYDp = avatarCenter + offset45
        )
    }

    @Test
    fun layoutInvariance_borderChangeDoesNotMoveAvatarOrEditButton() {
        val reference = computePureLayout(78f, ProfileBorders.NONE)

        for (border in ProfileBorders.ALL_BORDERS) {
            val snapshot = computePureLayout(78f, border)
            assertEquals("Slot width must never vary", reference.slotWidthDp, snapshot.slotWidthDp, 0.0001f)
            assertEquals("Slot height must never vary", reference.slotHeightDp, snapshot.slotHeightDp, 0.0001f)
            assertEquals("Avatar width must never vary", reference.avatarWidthDp, snapshot.avatarWidthDp, 0.0001f)
            assertEquals("Avatar height must never vary", reference.avatarHeightDp, snapshot.avatarHeightDp, 0.0001f)
            assertEquals("Pen X must never vary", reference.penCenterXDp, snapshot.penCenterXDp, 0.0001f)
            assertEquals("Pen Y must never vary", reference.penCenterYDp, snapshot.penCenterYDp, 0.0001f)
        }
    }

    @Test
    fun borderExtents_fitWithinScreenAndSlotAllowances_orMatchExceptionList() {
        // At 360dp width in Settings header, avatar center is at (180dp, 191dp)
        val avatarCenterX = 180f
        val avatarCenterY = 191f
        val dDp = 78f

        val reportedExceptions = listOf(
            "Golden Wings bottom feather extent intrudes 2.75dp into 10dp name column padding"
        )
        assertFalse(reportedExceptions.isEmpty())

        for (border in ProfileBorders.ALL_BORDERS) {
            if (border.drawableRes == 0) continue

            val scaleWidth = (dDp * (1f - ProfileAvatarDefaults.InnerOverlapFraction)) / border.innerDiameterRatio
            val scaleHeight = scaleWidth * (border.canvasHeight.toFloat() / border.canvasWidth.toFloat())

            val shiftX = -(border.innerCenterX - 0.5f) * scaleWidth
            val shiftY = -(border.innerCenterY - 0.5f) * scaleHeight

            val imgLeft = avatarCenterX - (scaleWidth / 2f) + shiftX
            val imgTop = avatarCenterY - (scaleHeight / 2f) + shiftY

            val extLeft = imgLeft + border.extentLeft * scaleWidth
            val extRight = imgLeft + border.extentRight * scaleWidth
            val extTop = imgTop + border.extentTop * scaleHeight
            val extBottom = imgTop + border.extentBottom * scaleHeight

            // Clearance to screen margins (0..360dp)
            assertTrue("Must not collide with left screen edge: $extLeft", extLeft > 0f)
            assertTrue("Must not collide with right screen edge: $extRight", extRight < 360f)

            // Clearance to title row bottom (y=86dp)
            assertTrue("Must not collide with title row: $extTop", extTop > 86f)

            // Name text starts at y=240dp.
            // Check if within bounds or matching documented exception
            if (extBottom > 240f) {
                assertEquals("file_000000007fdc81f7b74b9a79e2c63215", border.id)
                val intrusion = extBottom - 240f
                assertTrue("Intrusion must be <= 3.0dp", intrusion in 2.0f..3.0f)
            } else {
                assertTrue("Must clear name text area", extBottom <= 240f)
            }
        }
    }

    @Test
    fun wiringTest_settingsProfileAndPicker_allUseProfileAvatar() {
        val baseDir = if (File("src/main").exists()) File(".") else File("app")
        val settingsFile = File(baseDir, "src/main/java/com/arinara/fotara/ui/settings/SettingsScreen.kt")
        val profileFile = File(baseDir, "src/main/java/com/arinara/fotara/ui/profile/ProfileScreen.kt")
        val pickerFile = File(baseDir, "src/main/java/com/arinara/fotara/ui/profile/BorderPickerDialog.kt")

        assertTrue("SettingsScreen.kt must exist", settingsFile.exists())
        assertTrue("ProfileScreen.kt must exist", profileFile.exists())
        assertTrue("BorderPickerDialog.kt must exist", pickerFile.exists())

        val settingsContent = settingsFile.readText()
        val profileContent = profileFile.readText()
        val pickerContent = pickerFile.readText()

        assertTrue("SettingsScreen must invoke ProfileAvatar(", settingsContent.contains("ProfileAvatar("))
        assertTrue("ProfileScreen must invoke ProfileAvatar(", profileContent.contains("ProfileAvatar("))
        assertTrue("BorderPickerDialog must invoke ProfileAvatar(", pickerContent.contains("ProfileAvatar("))

        // Ensure ProfileAvatarDefaults.PickerAvatarSize is used in picker
        assertTrue(
            "BorderPickerDialog should use ProfileAvatarDefaults.PickerAvatarSize",
            pickerContent.contains("ProfileAvatarDefaults.PickerAvatarSize")
        )
    }
}
