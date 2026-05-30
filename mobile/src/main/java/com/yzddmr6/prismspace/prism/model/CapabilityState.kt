package com.yzddmr6.prismspace.prism.model

enum class Capability { Normal, Shizuku, Adb, Root, ProfileOwner }

sealed class CapabilityAvailability {
    object Available : CapabilityAvailability()
    data class NeedsSetup(val reason: String) : CapabilityAvailability()
    data class AvailableButDisabled(val reason: String) : CapabilityAvailability()
    data class Unsupported(val reason: String) : CapabilityAvailability()
}

data class CapabilityState(
    val normal: CapabilityAvailability,
    val shizuku: CapabilityAvailability,
    val adb: CapabilityAvailability = CapabilityAvailability.NeedsSetup("ADB 未授权"),
    val root: CapabilityAvailability,
    val profileOwner: CapabilityAvailability,
) {
    val canUseNormal: Boolean get() = normal is CapabilityAvailability.Available
    val canUseCoreDualOpen: Boolean get() = normal is CapabilityAvailability.Available
            && profileOwner is CapabilityAvailability.Available
    val shouldPreferRoot: Boolean get() = !canUseNormal && root is CapabilityAvailability.Available
    val canUseShizukuOrAdb: Boolean get() = shizuku is CapabilityAvailability.Available
            || adb is CapabilityAvailability.Available
    val primaryModeLabel: String get() = when {
        normal is CapabilityAvailability.Available -> "普通模式可用"
        shizuku is CapabilityAvailability.Available -> "Shizuku 模式可用"
        adb is CapabilityAvailability.Available -> "ADB 模式可用"
        root is CapabilityAvailability.Available -> "Root 模式可用"
        else -> "需要配置"
    }
    val shizukuAdbModeLabel: String get() = when {
        shizuku is CapabilityAvailability.Available -> "Shizuku/ADB 模式可用"
        adb is CapabilityAvailability.Available -> "Shizuku/ADB 模式可用"
        shizuku is CapabilityAvailability.NeedsSetup -> shizuku.reason
        adb is CapabilityAvailability.NeedsSetup -> adb.reason
        else -> "Shizuku/ADB 未配置"
    }
}
