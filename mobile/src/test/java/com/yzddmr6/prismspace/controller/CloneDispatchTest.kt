package com.yzddmr6.prismspace.controller

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CloneDispatchTest {

	private fun route(parent: Boolean, system: Boolean, mode: Int,
	                  installerUsable: () -> Boolean = { true },
	                  shizukuGranted: () -> Boolean = { true },
	                  rootGranted: () -> Boolean = { false },
	                  dhizukuGranted: () -> Boolean = { false }) =
		cloneRoute(parent, system, mode, installerUsable, shizukuGranted, rootGranted, dhizukuGranted)

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

	@Test fun dhizukuWhenModeDhizukuAndGrantedAndNotParentNotSystem() {
		assertEquals(CloneRoute.DHIZUKU,
			route(parent = false, system = false, mode = PrismAppClones.MODE_DHIZUKU, dhizukuGranted = { true }))
	}

	@Test fun fileSyncWhenDhizukuModeButNotGranted() {
		// A chosen-but-unavailable privileged mode degrades to the manual file-sync path.
		assertEquals(CloneRoute.FILE_SYNC,
			route(parent = false, system = false, mode = PrismAppClones.MODE_DHIZUKU, dhizukuGranted = { false }))
	}

	@Test fun shizukuPreferredOverDhizukuWhenBothGranted() {
		// Shizuku is checked before Dhizuku in the routing order.
		assertEquals(CloneRoute.SHIZUKU,
			route(parent = false, system = false, mode = PrismAppClones.MODE_SHIZUKU,
			      shizukuGranted = { true }, dhizukuGranted = { true }))
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

	@Test fun dhizukuGrantedNotEvaluatedUnlessReached() {
		// Dhizuku probe must not run when parent-installer or system-enable wins.
		var called = false
		route(parent = true, system = false, mode = PrismAppClones.MODE_DHIZUKU,
		      installerUsable = { true }, dhizukuGranted = { called = true; true })
		assertFalse(called)
		called = false
		route(parent = false, system = true, mode = PrismAppClones.MODE_DHIZUKU, dhizukuGranted = { called = true; true })
		assertFalse(called)
	}

	@Test fun dhizukuGrantedNotEvaluatedWhenShizukuRouteWins() {
		// When mode is Shizuku and Shizuku is granted, the Dhizuku probe is never reached.
		var called = false
		route(parent = false, system = false, mode = PrismAppClones.MODE_SHIZUKU,
		      shizukuGranted = { true }, dhizukuGranted = { called = true; true })
		assertFalse(called)
	}
}
