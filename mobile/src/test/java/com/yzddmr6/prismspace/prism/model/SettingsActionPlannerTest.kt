package com.yzddmr6.prismspace.prism.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsActionPlannerTest {

    @Test fun opensManagerWhenShizukuUnavailable() {
        assertEquals(
            ShizukuSettingsAction.OpenManager,
            SettingsActionPlanner.shizukuAction(available = false, authorized = false),
        )
    }

    @Test fun requestsPermissionWhenShizukuAvailableButUnauthorized() {
        assertEquals(
            ShizukuSettingsAction.RequestPermission,
            SettingsActionPlanner.shizukuAction(available = true, authorized = false),
        )
    }

    @Test fun refreshesWhenShizukuAuthorized() {
        assertEquals(
            ShizukuSettingsAction.Refresh,
            SettingsActionPlanner.shizukuAction(available = true, authorized = true),
        )
    }
}
