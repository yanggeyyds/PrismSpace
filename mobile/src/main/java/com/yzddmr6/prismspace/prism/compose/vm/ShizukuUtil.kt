package com.yzddmr6.prismspace.prism.compose.vm

import android.content.pm.PackageManager.PERMISSION_GRANTED
import rikka.shizuku.Shizuku

/**
 * Centralised Shizuku readiness helpers.
 * Mirrors the identical inline checks in HomeViewModel, SettingsViewModel and
 * AppActionSheet (Shizuku.getVersion()>=11 + checkSelfPermission()==PERMISSION_GRANTED).
 */
internal object ShizukuUtil {

    /** Returns true if Shizuku is running (version >= 11). */
    fun isAvailable(): Boolean = try {
        Shizuku.getVersion() >= 11
    } catch (_: RuntimeException) {
        false
    }

    /** Returns true if Shizuku is available AND this app holds the Shizuku permission. */
    fun isAuthorized(): Boolean = try {
        isAvailable() && Shizuku.checkSelfPermission() == PERMISSION_GRANTED
    } catch (_: RuntimeException) {
        false
    }
}
