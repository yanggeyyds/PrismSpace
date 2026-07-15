package com.yzddmr6.prismspace.prism.compose.vm

import android.content.Context
import com.rosan.dhizuku.api.Dhizuku

/**
 * Centralised Dhizuku readiness helpers, mirroring [ShizukuUtil].
 *
 * Dhizuku shares Device Owner privileges with other apps. Unlike Shizuku (which
 * requires ADB/root to start its server), Dhizuku is activated as Device Owner
 * once and keeps running across reboots. The API requires [Dhizuku.init] before
 * any other call; [Dhizuku.init] is idempotent (it caches the binder internally
 * after the first successful handshake), so calling it on every availability
 * check is cheap and always reflects the current state — e.g. if Dhizuku is
 * installed/activated after the first call, subsequent calls pick that up.
 */
internal object DhizukuUtil {

    /** Returns true if Dhizuku is installed, activated and the binder is available. */
    fun isAvailable(context: Context): Boolean = try {
        Dhizuku.init(context.applicationContext)
    } catch (_: RuntimeException) {
        false
    }

    /** Returns true if Dhizuku is available AND this app holds the Dhizuku permission. */
    fun isAuthorized(context: Context): Boolean = try {
        isAvailable(context) && Dhizuku.isPermissionGranted()
    } catch (_: RuntimeException) {
        false
    }
}
