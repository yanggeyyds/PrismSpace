package com.yzddmr6.prismspace.installer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager.MATCH_SYSTEM_ONLY
import android.content.pm.PackageManager.MATCH_UNINSTALLED_PACKAGES
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.Q
import android.os.Parcelable
import androidx.annotation.RequiresApi
import com.yzddmr6.prismspace.util.Apps
import com.yzddmr6.prismspace.engine.PrismManager
import com.yzddmr6.prismspace.util.DevicePolicies
import com.yzddmr6.prismspace.util.Hacks
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@SuppressLint("InlinedApi") internal const val INVALID_SESSION_ID = PackageInstaller.SessionInfo.INVALID_ID
internal const val EXTRA_INSTALL_INFO = "install"

internal object AppInstallerUtils {

	@JvmStatic fun ensureSystemPackageEnabledAndUnfrozen(context: Context, intent: Intent): Boolean {
		val resolve = context.packageManager.resolveActivity(intent, MATCH_UNINSTALLED_PACKAGES or MATCH_SYSTEM_ONLY)
				?: return false
		if (Apps.isInstalledInCurrentUser(resolve.activityInfo.applicationInfo)) return true
		return DevicePolicies(context).run { isProfileOwner && (enableSystemAppByIntent(intent)
				|| PrismManager.ensureAppFreeToLaunch(context, resolve.activityInfo.packageName).isEmpty()) }
	}
}

@Parcelize data class AppInstallInfo(val caller: String, val callerUid: Int,
                                     var mode: Mode = Mode.INSTALL,
                                     var appId: String? = null,
                                     var appLabel: CharSequence? = null,
                                     var versionName: String? = null,
                                     var targetSdkVersion: Int? = null,
                                     var requestedLegacyExternalStorage: Boolean = false,
                                     var details: CharSequence? = null): Parcelable {
	constructor(context: Context, caller: String, callerUid: Int) : this(caller, callerUid) { this.context = context }

	@IgnoredOnParcel val callerLabel: CharSequence by lazy { Apps.of(context).getAppName(caller) }
	@IgnoredOnParcel @Suppress("JoinDeclarationAndAssignment") lateinit var context: Context

	/** See [PackageInstaller.SessionParams.MODE_INHERIT_EXISTING] */
	enum class Mode { INSTALL, UPDATE, CLONE, INHERIT }
}
