package com.yzddmr6.prismspace.prism.service

import com.yzddmr6.prismspace.prism.model.CapabilityAvailability
import com.yzddmr6.prismspace.prism.model.CapabilityState

class CapabilityService {

    fun buildState(
        profileOwner: Boolean,
        shizukuReady: Boolean,
        shizukuAvailable: Boolean = shizukuReady,
        adbReady: Boolean = false,
        rootDetected: Boolean,
        rootEnabled: Boolean,
    ): CapabilityState = CapabilityState(
        normal = CapabilityAvailability.Available,
        shizuku = when {
            shizukuReady -> CapabilityAvailability.Available
            shizukuAvailable -> CapabilityAvailability.NeedsSetup("Shizuku 等待授权")
            else -> CapabilityAvailability.NeedsSetup("Shizuku 未连接")
        },
        adb = if (adbReady) CapabilityAvailability.Available else CapabilityAvailability.NeedsSetup("ADB 未授权"),
        root = when {
            rootEnabled -> CapabilityAvailability.Available
            rootDetected -> CapabilityAvailability.AvailableButDisabled("Root 可用但未启用")
            else -> CapabilityAvailability.Unsupported("Root 不可用")
        },
        profileOwner = if (profileOwner) CapabilityAvailability.Available else CapabilityAvailability.NeedsSetup("双开空间未创建"),
    )
}
