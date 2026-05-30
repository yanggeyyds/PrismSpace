package com.yzddmr6.prismspace.prism.model

import com.yzddmr6.prismspace.prism.service.CapabilityService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrismModelTest {

    @Test fun capabilitySummaryPrefersLeastPrivilege() {
        val state = CapabilityState(
            normal = CapabilityAvailability.Available,
            shizuku = CapabilityAvailability.NeedsSetup("Shizuku 未连接"),
            root = CapabilityAvailability.AvailableButDisabled("Root 可用但未启用"),
            profileOwner = CapabilityAvailability.Available,
        )

        assertTrue(state.canUseNormal)
        assertFalse(state.shouldPreferRoot)
        assertEquals("普通模式可用", state.primaryModeLabel)
    }

    @Test fun capabilityServiceMarksMissingProfileOwner() {
        val state = CapabilityService().buildState(
            profileOwner = false,
            shizukuReady = false,
            rootDetected = true,
            rootEnabled = false,
        )

        assertEquals("普通模式可用", state.primaryModeLabel)
        assertTrue(state.profileOwner is CapabilityAvailability.NeedsSetup)
    }
}
