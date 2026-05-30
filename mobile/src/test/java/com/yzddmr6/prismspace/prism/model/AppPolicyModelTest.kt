package com.yzddmr6.prismspace.prism.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppPolicyModelTest {

    @Test fun defaultPolicyKeepsFilesIsolatedAndSystemDefaults() {
        val policy = AppPolicy()

        assertEquals(FileAccessMode.Isolated, policy.fileAccess)
        assertEquals(BackgroundPolicy.SystemDefault, policy.background)
        assertEquals(NetworkPolicy.SystemDefault, policy.network)
        assertEquals(NotificationPolicy.SystemDefault, policy.notifications)
        assertEquals("隔离", policy.fileAccessSummary)
        assertEquals("系统默认", policy.backgroundSummary)
        assertEquals("系统默认", policy.networkSummary)
    }

    @Test fun profileOwnerActionsAreAvailableWithoutShizukuOrRoot() {
        val state = CapabilityState(
            normal = CapabilityAvailability.Available,
            shizuku = CapabilityAvailability.NeedsSetup("Shizuku 未连接"),
            root = CapabilityAvailability.Unsupported("Root 不可用"),
            profileOwner = CapabilityAvailability.Available,
        )

        assertEquals(CapabilityAvailability.Available, AppPolicyPlanner.availability(PolicyAction.Freeze, state))
        assertEquals(CapabilityAvailability.Available, AppPolicyPlanner.availability(PolicyAction.Suspend, state))
        assertEquals(CapabilityAvailability.Available, AppPolicyPlanner.availability(PolicyAction.ImportExportFiles, state))
    }

    @Test fun enhancedIsolationRequiresShizukuAdbBeforeRootFallback() {
        val state = CapabilityState(
            normal = CapabilityAvailability.Available,
            shizuku = CapabilityAvailability.NeedsSetup("Shizuku 未连接"),
            root = CapabilityAvailability.AvailableButDisabled("Root 可用但未启用"),
            profileOwner = CapabilityAvailability.Available,
        )

        val network = AppPolicyPlanner.availability(PolicyAction.RestrictNetwork, state)
        val background = AppPolicyPlanner.availability(PolicyAction.RestrictBackground, state)
        val sharedMedia = AppPolicyPlanner.availability(PolicyAction.ConfigureSharedMedia, state)

        assertTrue(network is CapabilityAvailability.NeedsSetup)
        assertEquals("需要 Shizuku/ADB；Root 仅作为兜底", (network as CapabilityAvailability.NeedsSetup).reason)
        assertTrue(background is CapabilityAvailability.NeedsSetup)
        assertTrue(sharedMedia is CapabilityAvailability.NeedsSetup)
    }

    @Test fun shizukuAdbUnlocksEnhancedIsolationWithoutRoot() {
        val state = CapabilityState(
            normal = CapabilityAvailability.Available,
            shizuku = CapabilityAvailability.Available,
            root = CapabilityAvailability.Unsupported("Root 不可用"),
            profileOwner = CapabilityAvailability.Available,
        )

        assertEquals(CapabilityAvailability.Available, AppPolicyPlanner.availability(PolicyAction.RestrictNetwork, state))
        assertEquals(CapabilityAvailability.Available, AppPolicyPlanner.availability(PolicyAction.RestrictBackground, state))
        assertEquals(CapabilityAvailability.Available, AppPolicyPlanner.availability(PolicyAction.ConfigureSharedMedia, state))
    }
}
