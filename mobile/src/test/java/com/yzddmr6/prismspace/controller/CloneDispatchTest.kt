package com.yzddmr6.prismspace.controller

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CloneDispatchTest {

    private fun route(parent: Boolean, system: Boolean, mode: Int,
                      installerUsable: () -> Boolean = { true },
                      shizukuGranted: () -> Boolean = { true },
                      rootGranted: () -> Boolean = { false }) =
        cloneRoute(parent, system, mode, installerUsable, shizukuGranted, rootGranted)

    @Test fun parentInstallerWinsWhenParentTargetAndInstallerUsable() {
        assertEquals(CloneRoute.PARENT_INSTALLER,
            route(parent = true, system = true, mode = PrismAppClones.MODE_SHIZUKU, installerUsable = { true }))
    }

    @Test fun notParentInstallerWhenInstallerNotUsable_fallsToSystem() {
        assertEquals(CloneRoute.SYSTEM_ENABLE,
            route(parent = true, system = true, mode = PrismAppClones.MODE_INSTALLER, installerUsable = { false }))
    }

    @Test fun systemEnableWinsOverShizukuAndInProfile() {
        assertEquals(CloneRoute.SYSTEM_ENABLE,
            route(parent = false, system = true, mode = PrismAppClones.MODE_SHIZUKU, shizukuGranted = { true }))
    }

    @Test fun rootWhenModeRootAndGranted() {
        assertEquals(CloneRoute.ROOT,
            route(parent = false, system = false, mode = PrismAppClones.MODE_ROOT, rootGranted = { true }))
    }

    @Test fun shizukuWhenModeShizukuAndGrantedAndNotParentNotSystem() {
        assertEquals(CloneRoute.SHIZUKU,
            route(parent = false, system = false, mode = PrismAppClones.MODE_SHIZUKU, shizukuGranted = { true }))
    }

    @Test fun fileSyncWhenShizukuModeButNotGranted() {
        // A chosen-but-unavailable privileged mode degrades to the manual file-sync path.
        assertEquals(CloneRoute.FILE_SYNC,
            route(parent = false, system = false, mode = PrismAppClones.MODE_SHIZUKU, shizukuGranted = { false }))
    }

    @Test fun installerModeMapsToFileSync() {
        assertEquals(CloneRoute.FILE_SYNC,
            route(parent = false, system = false, mode = PrismAppClones.MODE_INSTALLER))
    }


    @Test fun installerUsableNotEvaluatedWhenNotParentTarget() {
        var called = false
        route(parent = false, system = false, mode = PrismAppClones.MODE_INSTALLER, installerUsable = { called = true; true })
        assertFalse(called)
    }

    @Test fun shizukuGrantedNotEvaluatedUnlessReached() {
        var called = false
        route(parent = true, system = false, mode = PrismAppClones.MODE_SHIZUKU,
              installerUsable = { true }, shizukuGranted = { called = true; true })
        assertFalse(called)
        called = false
        route(parent = false, system = true, mode = PrismAppClones.MODE_SHIZUKU, shizukuGranted = { called = true; true })
        assertFalse(called)
    }
}
