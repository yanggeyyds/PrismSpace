package com.yzddmr6.prismspace.prism.model

import com.yzddmr6.prismspace.mobile.R
import org.junit.Assert.assertEquals
import org.junit.Test

class PrismOverviewStateTest {

    @Test fun readyProfileShowsAddAppAsNextAction() {
        val state = PrismOverviewState.from(
            hasManagedProfile = true,
            profilePaused = false,
            shizukuReady = false,
            rootEnabled = false,
            lastDiagnosticFailed = false,
        )

        assertEquals(R.string.prism_home_status_ready_title, state.statusTitleRes)
        assertEquals(R.string.prism_home_status_ready_body, state.statusBodyRes)
        assertEquals(R.string.prism_home_add_app, state.primaryActionLabelRes)
        assertEquals(PrismOverviewAction.AddApp, state.primaryAction)
    }

    @Test fun missingProfileShowsRepairAsNextAction() {
        val state = PrismOverviewState.from(
            hasManagedProfile = false,
            profilePaused = false,
            shizukuReady = false,
            rootEnabled = false,
            lastDiagnosticFailed = false,
        )

        assertEquals(R.string.prism_home_status_missing_title, state.statusTitleRes)
        assertEquals(R.string.prism_home_status_missing_body, state.statusBodyRes)
        assertEquals(R.string.prism_home_action_create_or_repair, state.primaryActionLabelRes)
        assertEquals(PrismOverviewAction.CreateOrRepairProfile, state.primaryAction)
    }

    @Test fun diagnosticFailureTakesPriorityOverSetupHints() {
        val state = PrismOverviewState.from(
            hasManagedProfile = true,
            profilePaused = false,
            shizukuReady = false,
            rootEnabled = true,
            lastDiagnosticFailed = true,
        )

        assertEquals(R.string.prism_home_action_open_diagnostics, state.primaryActionLabelRes)
        assertEquals(PrismOverviewAction.OpenDiagnostics, state.primaryAction)
    }
}
