package com.yzddmr6.prismspace.prism.model

enum class ShizukuSettingsAction {
    OpenManager,
    RequestPermission,
    Refresh,
}

object SettingsActionPlanner {
    fun shizukuAction(available: Boolean, authorized: Boolean): ShizukuSettingsAction = when {
        authorized -> ShizukuSettingsAction.Refresh
        available -> ShizukuSettingsAction.RequestPermission
        else -> ShizukuSettingsAction.OpenManager
    }
}
