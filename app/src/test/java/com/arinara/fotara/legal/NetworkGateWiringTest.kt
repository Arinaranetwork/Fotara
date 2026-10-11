// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.legal

import android.content.Context
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NetworkGateWiringTest {

    @Before
    fun setUp() {
        NetworkGate.setTestOverride(null)
    }

    @After
    fun tearDown() {
        NetworkGate.setTestOverride(null)
    }

    @Test
    fun testNetworkGateOverrideBlocksAndAllows() {
        NetworkGate.setTestOverride(false)
        assertFalse("NetworkGate must return false when consent override is false", NetworkGate.isConsentGranted(android.content.ContextWrapper(null)))

        NetworkGate.setTestOverride(true)
        assertTrue("NetworkGate must return true when consent override is true", NetworkGate.isConsentGranted(android.content.ContextWrapper(null)))
    }
}
