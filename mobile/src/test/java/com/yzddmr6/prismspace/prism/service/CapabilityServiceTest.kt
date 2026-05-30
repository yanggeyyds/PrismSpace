package com.yzddmr6.prismspace.prism.service

import com.yzddmr6.prismspace.prism.model.AppPolicyPlanner
import com.yzddmr6.prismspace.prism.model.CapabilityAvailability
import com.yzddmr6.prismspace.prism.model.PolicyAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilityServiceTest {

    @Test fun shizukuStatusDistinguishesDisconnectedWaitingAuthorizationAndReady() {
        val service = CapabilityService()

        val disconnected = service.buildState(
            profileOwner = true,
            shizukuAvailable = false,
            shizukuReady = false,
            adbReady = false,
            rootDetected = false,
            rootEnabled = false,
        )
        val waitingAuthorization = service.buildState(
            profileOwner = true,
            shizukuAvailable = true,
            shizukuReady = false,
            adbReady = false,
            rootDetected = false,
            rootEnabled = false,
        )
        val ready = service.buildState(
            profileOwner = true,
            shizukuAvailable = true,
            shizukuReady = true,
            adbReady = false,
            rootDetected = false,
            rootEnabled = false,
        )

        assertEquals(CapabilityAvailability.NeedsSetup("Shizuku 未连接"), disconnected.shizuku)
        assertEquals(CapabilityAvailability.NeedsSetup("Shizuku 等待授权"), waitingAuthorization.shizuku)
        assertEquals(CapabilityAvailability.Available, ready.shizuku)
    }

    @Test fun adbReadyUnlocksEnhancedPolicyActionsWithoutShizukuOrRoot() {
        val state = CapabilityService().buildState(
            profileOwner = true,
            shizukuAvailable = false,
            shizukuReady = false,
            adbReady = true,
            rootDetected = false,
            rootEnabled = false,
        )

        assertEquals(CapabilityAvailability.Available, state.adb)
        assertEquals(CapabilityAvailability.Available, AppPolicyPlanner.availability(PolicyAction.RestrictNetwork, state))
        assertEquals(CapabilityAvailability.Available, AppPolicyPlanner.availability(PolicyAction.RestrictBackground, state))
    }

    @Test fun coreDualOpenDoesNotDependOnShizukuAdbOrRoot() {
        val state = CapabilityService().buildState(
            profileOwner = true,
            shizukuAvailable = false,
            shizukuReady = false,
            adbReady = false,
            rootDetected = false,
            rootEnabled = false,
        )

        assertTrue(state.canUseCoreDualOpen)
        assertTrue(AppPolicyPlanner.availability(PolicyAction.RestrictNetwork, state) is CapabilityAvailability.NeedsSetup)
    }
}
