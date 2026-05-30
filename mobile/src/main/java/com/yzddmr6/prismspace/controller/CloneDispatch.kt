package com.yzddmr6.prismspace.controller

/**
 * Single source of truth for which rung of PrismAppClones.cloneApp() fires.
 * [installerUsable] and [shizukuPermissionGranted] are lazy so unavailable paths do
 * not perform permission or package-manager probes until their route is reached.
 */
enum class CloneRoute { PARENT_INSTALLER, SYSTEM_ENABLE, SHIZUKU, ROOT, FILE_SYNC }

/**
 * Four-method clone model for a non-system user app cloned from main space to dual space:
 *  - ROOT      : `pm install-existing` via su (auto, needs root granted).
 *  - SHIZUKU   : installExistingPackage via privileged worker (auto, needs Shizuku authorized).
 *  - FILE_SYNC : transfer the APK into the dual space, user installs manually (no-privilege fallback).
 * The package-scheme installer route is unreachable for user apps under Android 16 managed
 * profiles, so normal mode uses file sync and foreground user confirmation instead.
 */
fun cloneRoute(
	isParentProfileTarget: Boolean,
	isSourceSystemApp: Boolean,
	mode: Int,                                  // @PrismAppClones.AppCloneMode
	installerUsable: () -> Boolean,
	shizukuPermissionGranted: () -> Boolean,
	rootGranted: () -> Boolean = { false },
): CloneRoute = when {
	isParentProfileTarget && installerUsable()                          -> CloneRoute.PARENT_INSTALLER
	isSourceSystemApp                                                   -> CloneRoute.SYSTEM_ENABLE
	mode == PrismAppClones.MODE_ROOT && rootGranted()                  -> CloneRoute.ROOT
	mode == PrismAppClones.MODE_SHIZUKU && shizukuPermissionGranted()  -> CloneRoute.SHIZUKU
	else                                                                -> CloneRoute.FILE_SYNC
}
