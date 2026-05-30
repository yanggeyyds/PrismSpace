package com.yzddmr6.prismspace.prism.model

import androidx.annotation.StringRes
import com.yzddmr6.prismspace.mobile.R

data class PrismOverviewState(
    @StringRes val statusTitleRes: Int,
    @StringRes val statusBodyRes: Int,
    @StringRes val primaryActionLabelRes: Int,
    val primaryAction: PrismOverviewAction,
) {
    companion object {
        fun from(
            hasManagedProfile: Boolean,
            profilePaused: Boolean,
            shizukuReady: Boolean,
            rootEnabled: Boolean,
            lastDiagnosticFailed: Boolean,
        ): PrismOverviewState {
            val action = when {
                lastDiagnosticFailed -> PrismOverviewAction.OpenDiagnostics
                !hasManagedProfile || profilePaused -> PrismOverviewAction.CreateOrRepairProfile
                shizukuReady -> PrismOverviewAction.AddApp
                else -> PrismOverviewAction.AddApp
            }
            return PrismOverviewState(
                statusTitleRes = when {
                    !hasManagedProfile -> R.string.prism_home_status_missing_title
                    profilePaused -> R.string.prism_home_status_paused_title
                    else -> R.string.prism_home_status_ready_title
                },
                statusBodyRes = when {
                    !hasManagedProfile -> R.string.prism_home_status_missing_body
                    profilePaused -> R.string.prism_home_status_paused_body
                    else -> R.string.prism_home_status_ready_body
                },
                primaryActionLabelRes = when (action) {
                    PrismOverviewAction.CreateOrRepairProfile -> R.string.prism_home_action_create_or_repair
                    PrismOverviewAction.AddApp -> R.string.prism_home_add_app
                    PrismOverviewAction.AuthorizeShizuku -> R.string.prism_settings_open_shizuku
                    PrismOverviewAction.OpenDiagnostics -> R.string.prism_home_action_open_diagnostics
                },
                primaryAction = action,
            )
        }
    }
}

enum class PrismOverviewAction {
    CreateOrRepairProfile,
    AddApp,
    AuthorizeShizuku,
    OpenDiagnostics,
}
