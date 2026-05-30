package com.yzddmr6.prismspace.prism.ui

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import com.yzddmr6.prismspace.analytics.DiagnosticLog
import android.widget.Toast
import com.yzddmr6.prismspace.mobile.R
import com.yzddmr6.prismspace.prism.service.InstallSourcePermissionHelper
import com.yzddmr6.prismspace.prism.service.ProfileDownloadsLauncher
import com.yzddmr6.prismspace.prism.service.openSystemFileManager
import com.yzddmr6.prismspace.settings.PrismSettingsActivity
import com.yzddmr6.prismspace.util.PrismLocale

/**
 * Trampoline that runs INSIDE the dual space (work profile), reached via cross-profile intent
 * forwarding from the main app (see FileBridgeService.openProfileDownloadsFolder). It opens the
 * system file-manager chooser in the profile so the user can find transferred files under
 * Download/PrismSpace. APK install is a separate explicit entry so split packages use a foreground
 * PackageInstaller session instead of relying on whichever file manager opened the folder.
 *
 * Mirrors [ProfileImagePickerActivity]. Translucent + noHistory + finishes immediately.
 */
class ProfileDownloadsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            intent.getStringExtra(ProfileDownloadsLauncher.EXTRA_OPEN_SOURCE_SETTINGS_PACKAGE)?.let { packageName ->
                if (!InstallSourcePermissionHelper.openInstallSourceSettingsOrFallback(this, packageName)) {
                    openProfileEntryForForegroundInstall()
                }
                return
            }
            if (intent.getBooleanExtra(ProfileDownloadsLauncher.EXTRA_OPEN_INSTALL_ENTRY, false)) {
                openProfileEntryForForegroundInstall()
                return
            }
            openSystemFileManager(this)
        } catch (e: Throwable) {
            DiagnosticLog.e(TAG, "Unable to open profile downloads", e)
        } finally {
            finish()
        }
    }

    private fun openProfileEntryForForegroundInstall() {
        val component = ComponentName(this, PrismSettingsActivity::class.java)
        packageManager.setComponentEnabledSetting(
            component,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )
        startActivity(Intent(this, PrismSettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        Toast.makeText(
            this,
            PrismLocale.wrap(this).getString(R.string.lz_pf_install_use_prismspace),
            Toast.LENGTH_LONG,
        ).show()
    }

    private companion object {
        private const val TAG = "Prism.ProfileDownloads"
    }
}
