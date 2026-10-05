// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.profile

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BannerAnimationGateTest {

    @Test
    fun shouldAnimate_allConditionsMet_returnsTrue() {
        val result = BannerAnimationGate.shouldAnimate(
            isResumed = true,
            isOnScreen = true,
            animatorDurationScale = 1.0f,
            apiLevel = 28
        )
        assertTrue(result)
    }

    @Test
    fun shouldAnimate_notResumed_returnsFalse() {
        val result = BannerAnimationGate.shouldAnimate(
            isResumed = false,
            isOnScreen = true,
            animatorDurationScale = 1.0f,
            apiLevel = 28
        )
        assertFalse(result)
    }

    @Test
    fun shouldAnimate_notOnScreen_returnsFalse() {
        val result = BannerAnimationGate.shouldAnimate(
            isResumed = true,
            isOnScreen = false,
            animatorDurationScale = 1.0f,
            apiLevel = 28
        )
        assertFalse(result)
    }

    @Test
    fun shouldAnimate_animationsDisabled_returnsFalse() {
        val result = BannerAnimationGate.shouldAnimate(
            isResumed = true,
            isOnScreen = true,
            animatorDurationScale = 0.0f,
            apiLevel = 34
        )
        assertFalse(result)
    }

    @Test
    fun shouldAnimate_apiBelow28_returnsFalse() {
        val result = BannerAnimationGate.shouldAnimate(
            isResumed = true,
            isOnScreen = true,
            animatorDurationScale = 1.0f,
            apiLevel = 27
        )
        assertFalse(result)
    }
}
