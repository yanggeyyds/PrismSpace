package com.yzddmr6.prismspace.prism.service

import android.Manifest
import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import com.yzddmr6.prismspace.appops.AppOpsCompat
import com.yzddmr6.prismspace.mobile.R
import com.yzddmr6.prismspace.util.DevicePolicies
import com.yzddmr6.prismspace.util.PrismLocale

object InstallSourcePermissionHelper {

    const val SYSTEM_FILE_MANAGER_PACKAGE = "com.android.fileexplorer"

    private val fileManagerSourcePackages = listOf(
        SYSTEM_FILE_MANAGER_PACKAGE,
        "bin.mt.plus",
    )

    private val downloadsSourcePackages = listOf(
        "com.google.android.documentsui",
        "com.android.documentsui",
    )

    private val systemSourcePackages = setOf(
        SYSTEM_FILE_MANAGER_PACKAGE,
        "com.google.android.documentsui",
        "com.android.documentsui",
    )

    val knownSourcePackages: List<String>
        get() = fileManagerSourcePackages + downloadsSourcePackages

    data class SourceState(
        val packageName: String,
        val installed: Boolean,
        val declaresInstallPermission: Boolean,
        val canInstall: Boolean,
    )

    fun openDownloadsSourceSettingsIfNeeded(context: Context): Boolean {
        val source = sourceToRequestBeforeOpeningDownloads(sourceStates(context))
            ?: return false
        return openUnknownSourceSettings(context, source)
    }

    fun downloadsSourceReadyForApkInstall(context: Context): Boolean =
        anySourceReadyForInstall(sourceStates(context))

    fun firstSourceNeedingPermission(sources: List<SourceState>): String? =
        sourcesByPriority(sources).firstOrNull {
            it.installed && it.declaresInstallPermission && !it.canInstall
        }?.packageName

    fun anySourceReadyForInstall(sources: List<SourceState>): Boolean =
        sources.any { it.installed && it.declaresInstallPermission && it.canInstall }

    fun sourceToRequestBeforeOpeningDownloads(sources: List<SourceState>): String? =
        if (anySourceReadyForInstall(sources)) null else firstSourceNeedingPermission(sources)

    fun sourceToRequestForPreferred(sources: List<SourceState>, preferredPackageName: String): String? =
        if (anySourceReadyForInstall(sources)) {
            null
        } else {
            sources.firstOrNull {
                it.packageName == preferredPackageName && it.installed && it.declaresInstallPermission && !it.canInstall
            }?.packageName ?: firstSourceNeedingPermission(sources)
        }

    private fun sourcesByPriority(sources: List<SourceState>): List<SourceState> {
        val byPackage = sources.associateBy { it.packageName }
        return knownSourcePackages.mapNotNull { byPackage[it] } + sources.filter { it.packageName !in knownSourcePackages }
    }

    private fun sourceStates(context: Context): List<SourceState> =
        knownSourcePackages.map { packageName ->
            val installed = isInstalled(context, packageName)
            SourceState(
                packageName = packageName,
                installed = installed,
                declaresInstallPermission = installed && declaresUnknownAppInstallPermission(context, packageName),
                canInstall = installed && canInstallUnknownApps(context, packageName),
            )
        }

    fun canInstallUnknownApps(context: Context, packageName: String): Boolean {
        val pm = context.packageManager
        val info = runCatching { pm.getApplicationInfo(packageName, 0) }.getOrNull() ?: return false
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsCompat.OPSTR_REQUEST_INSTALL_PACKAGES, info.uid, packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsCompat.OPSTR_REQUEST_INSTALL_PACKAGES, info.uid, packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openUnknownSourceSettings(context: Context, packageName: String): Boolean {
        if (!ensureInstallSourceAvailable(context, packageName)) return false
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:$packageName"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(intent)
            Toast.makeText(
                context,
                PrismLocale.wrap(context).getString(R.string.lz_pf_install_source_need_perm),
                Toast.LENGTH_LONG,
            ).show()
            true
        }.getOrDefault(false)
    }

    fun openInstallSourceSettingsOrFallback(context: Context, preferredPackageName: String): Boolean {
        ensureInstallSourceAvailable(context, preferredPackageName)
        val packageName = sourceToRequestForPreferred(sourceStates(context), preferredPackageName) ?: return false
        return openUnknownSourceSettings(context, packageName)
    }

    private fun ensureInstallSourceAvailable(context: Context, packageName: String): Boolean {
        if (!isInstalled(context, packageName) && packageName in systemSourcePackages) {
            runCatching {
                val policies = DevicePolicies(context)
                if (policies.isProfileOwner) policies.enableSystemApp(packageName)
            }
        }
        if (!isInstalled(context, packageName)) return false
        runCatching {
            val policies = DevicePolicies(context)
            if (!policies.isProfileOwner) return@runCatching
            runCatching { policies.setApplicationHidden(packageName, false) }
            runCatching {
                policies.invoke(DevicePolicyManager::setPackagesSuspended, arrayOf(packageName), false)
            }
        }
        return true
    }

    private fun isInstalled(context: Context, packageName: String): Boolean =
        runCatching { context.packageManager.getApplicationInfo(packageName, 0); true }.getOrDefault(false)

    private fun declaresUnknownAppInstallPermission(context: Context, packageName: String): Boolean =
        runCatching {
            context.packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
                .requestedPermissions
                ?.contains(Manifest.permission.REQUEST_INSTALL_PACKAGES) == true
        }.getOrDefault(false)
}
