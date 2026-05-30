package com.yzddmr6.prismspace.controller

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.UserHandle
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import com.yzddmr6.prismspace.util.Dialogs
import com.yzddmr6.prismspace.util.Apps
import com.yzddmr6.prismspace.analytics.Analytics.Param.ITEM_CATEGORY
import com.yzddmr6.prismspace.analytics.Analytics.Param.ITEM_ID
import com.yzddmr6.prismspace.analytics.analytics
import com.yzddmr6.prismspace.data.PrismAppInfo
import com.yzddmr6.prismspace.data.helper.AppStateTrackingHelper
import com.yzddmr6.prismspace.engine.ClonedHiddenSystemApps
import com.yzddmr6.prismspace.engine.PrismManager
import com.yzddmr6.prismspace.engine.LaunchResult
import com.yzddmr6.prismspace.mobile.R
import com.yzddmr6.prismspace.model.interactive
import com.yzddmr6.prismspace.prism.compose.vm.launchFeedback
import com.yzddmr6.prismspace.prism.compose.vm.prismResolver
import com.yzddmr6.prismspace.shuttle.Shuttle
import com.yzddmr6.prismspace.util.Activities
import com.yzddmr6.prismspace.util.DevicePolicies
import com.yzddmr6.prismspace.util.IntentCompat
import com.yzddmr6.prismspace.util.OwnerUser
import com.yzddmr6.prismspace.util.ProfileUser
import com.yzddmr6.prismspace.util.Toasts
import com.yzddmr6.prismspace.util.Users
import org.jetbrains.annotations.NotNull

object PrismAppControl {

	@JvmStatic fun requestRemoval(activity: Activity, app: PrismAppInfo) {
		analytics().event("action_uninstall").with(ITEM_ID, app.packageName).with(ITEM_CATEGORY, "system").send()

		if (! unfreezeIfNeeded(app)) return

		if (app.isSystem) {
			analytics().event("action_disable_sys_app").with(ITEM_ID, app.packageName).send()
			if (app.isCritical) Dialogs.buildAlert(activity, R.string.dialog_title_warning, R.string.dialog_critical_app_warning)
					.withCancelButton().setPositiveButton(R.string.action_continue) { _,_ -> launchSystemAppSettings(app) }.show()
			else Dialogs.buildAlert(activity, 0, R.string.prompt_disable_sys_app_as_removal)
					.withCancelButton().setPositiveButton(R.string.action_continue) { _,_ -> launchSystemAppSettings(app) }.show() }
		else {
			Activities.startActivity(activity, Intent(Intent.ACTION_UNINSTALL_PACKAGE)
					.setData(Uri.fromParts("package", app.packageName, null)).putExtra(Intent.EXTRA_USER, app.user))
			if (! Users.isProfileRunning(activity, app.user))   // App clone can actually be removed in quiet mode, without callback triggered.
				if (! activity.isDestroyed) AppStateTrackingHelper.requestSyncWhenResumed(activity, app.packageName, app.user) }
	}

	@JvmStatic fun launch(context: Context, app: PrismAppInfo) {
		analytics().event("action_launch").with(ITEM_ID, app.packageName).send()
		// Suspended counts as frozen too (hybrid freeze): ensureAppFreeToLaunch lifts both hide and
		// suspend, but we must route here when EITHER is set — a suspended app won't launch otherwise.
		if (app.isHidden || app.isSuspended) unfreezeAndLaunch(context, app)
		else toastLaunch(context, PrismManager.launchApp(context, app.packageName, app.user), app.label.toString(), app.packageName)
	}

	private fun unfreezeAndLaunch(context: Context, app: PrismAppInfo) {
		val pkg = app.packageName
		val ready = Shuttle(context, to = app.user).invokeNoThrowsWithin(with = pkg) { PrismManager.ensureAppFreeToLaunch(this, it) }
		if (ready == null) return toastLaunch(context, LaunchResult.SpaceNotReady, app.label.toString(), pkg)
		if (ready.isNotEmpty()) return toastLaunch(context, LaunchResult.Unknown(ready), app.label.toString(), pkg)
		toastLaunch(context, PrismManager.launchApp(context, pkg, app.user), Apps.of(context).getAppName(pkg).toString(), pkg)
	}

	private fun toastLaunch(context: Context, result: LaunchResult, label: String, pkg: String) {
		if (result is LaunchResult.Ok) return
		val fb = launchFeedback(result, label, prismResolver(context))
		Toast.makeText(context, fb.message, Toast.LENGTH_LONG).show()
		val category = (result as? LaunchResult.Unknown)?.reason?.takeIf { it.isNotBlank() }
			?: result::class.simpleName ?: "Unknown"
		analytics().event("app_launch_error").with(ITEM_ID, pkg)
			.with(ITEM_CATEGORY, category).send()
	}

	@JvmStatic fun launchSystemAppSettings(app: PrismAppInfo) {    // Stock app info activity requires the target app not hidden.
		if (unfreezeIfNeeded(app))
			app.context().getSystemService(LauncherApps::class.java)!!.startAppDetailsActivity(ComponentName(app.packageName, ""), app.user, null, null)
	}

	@JvmStatic fun launchExternalAppSettings(vm: AndroidViewModel, app: @NotNull PrismAppInfo) {
		val context = app.context()
		val intent = Intent(IntentCompat.ACTION_SHOW_APP_INFO).setPackage(context.packageName)
				.putExtra(IntentCompat.EXTRA_PACKAGE_NAME, app.packageName).putExtra(Intent.EXTRA_USER, app.user)
		val resolve = context.packageManager.resolveActivity(intent, 0) ?: return
		// Should never happen as module "installer" is always bundled with "mobile".
		intent.component = ComponentName(resolve.activityInfo.packageName, resolve.activityInfo.name)
		vm.interactive(context) { if (unfreezeIfNeeded(app)) Activities.startActivity(context, intent) }
	}

	/** @return true if not frozen (neither hidden nor suspended) or successfully unfrozen, false otherwise. */
	private fun unfreezeIfNeeded(app: PrismAppInfo): Boolean {
		return if (! app.isHidden && ! app.isSuspended) true else unfreeze(app).toastIfNull(app.context()) ?: false
	}

	@JvmStatic fun freeze(app: PrismAppInfo): Boolean {
		val frozen = Shuttle(app.context(), to = app.user).invokeNoThrows(with = app.packageName) {
			ensureAppHiddenState(this, it, true) }.toastIfNull(app.context()) ?: false
		if (frozen && app.isSystem) stopTreatingHiddenSysAppAsDisabled(app)
		return frozen
	}

	@JvmStatic fun unfreeze(app: PrismAppInfo) = unfreeze(app.context(), app.user, app.packageName)
	private fun unfreeze(context: Context, profile: UserHandle, pkg: String) =
		Shuttle(context, to = profile).invokeNoThrows { ensureAppHiddenState(this, pkg, false) }.toastIfNull(context)

	@OwnerUser @ProfileUser private fun ensureAppHiddenState(context: Context, pkg: String, hidden: Boolean): Boolean {
		val policies = DevicePolicies(context)
		// Same hide+suspend hybrid as the whole-space freeze. Only toast on genuine failure.
		if (applyFrozenWithFallback(policies, pkg, hidden)) return true
		val activeAdmins = policies.manager.activeAdmins
		if (activeAdmins != null && activeAdmins.any { pkg == it.packageName })
			Toasts.showLong(context, R.string.toast_error_freezing_active_admin)
		else Toasts.showLong(context, if (hidden) R.string.toast_error_freeze_failure else R.string.toast_error_unfreeze_failure)
		return false
	}

	@JvmStatic fun setSuspended(app: PrismAppInfo, suspended: Boolean) =
		Shuttle(app.context(), to = app.user).invoke(with = app.packageName) { setPackageSuspended(this, it, suspended) }
	private fun setPackageSuspended(context: Context, pkg: String, suspended: Boolean)
			= setPackagesSuspended(context, arrayOf(pkg), suspended).isEmpty()
	fun setPackagesSuspended(context: Context, pkgs: Array<String>, suspended: Boolean): Array<String>
			= DevicePolicies(context).invoke(DevicePolicyManager::setPackagesSuspended, pkgs, suspended)

	/** Bulk suspend/restore every app of one dual space, routed through [Shuttle] so the
	 * DPM call runs as profile owner inside the work profile. The low-level
	 * [setPackagesSuspended] helper requires a profile context.
	 * @return packages that could not be updated, or null if the profile is not ready. */
	@JvmStatic fun setSpaceSuspended(apps: List<PrismAppInfo>, suspended: Boolean): Array<String>? {
		if (apps.isEmpty()) return emptyArray()
		val pkgs = apps.map { it.packageName }.toTypedArray()
		return Shuttle(apps.first().context(), to = apps.first().user)
				.invokeNoThrows(with = pkgs) { setPackagesSuspended(this, it, suspended) }
	}

	/** Whole-space freeze: hide every user clone of one dual space, routed through
	 * [Shuttle] so the DPM call runs as profile owner inside the work profile. It uses the same
	 * freeze mechanism as per-app freeze so badge and recovery behavior stay consistent; unfreeze
	 * also lifts any lingering suspended flag.
	 *  @return packages that could not be updated, or null if the profile is not ready. */
	@JvmStatic fun setSpaceFrozen(apps: List<PrismAppInfo>, frozen: Boolean): Array<String>? {
		if (apps.isEmpty()) return emptyArray()
		val pkgs = apps.map { it.packageName }.toTypedArray()
		return Shuttle(apps.first().context(), to = apps.first().user).invokeNoThrows(with = pkgs) { ps ->
			val policies = DevicePolicies(this)
			ps.filter { pkg -> ! applyFrozenWithFallback(policies, pkg, frozen) }.toTypedArray()
		}
	}

	/** Freeze/unfreeze [pkg] resiliently across OEM quirks.
	 * Strategy:
	 * - freeze: prefer hide for the clean "app vanishes" UX; if hide does not take effect, fall
	 *   back to suspend through a different enforcement path.
	 * - unfreeze: always clear both hide and suspend, since either could have frozen it.
	 * setPackagesSuspended returns the packages it could not update; empty means success.
	 * Runs inside the profile process (profile-owner DPM). */
	@OwnerUser @ProfileUser private fun applyFrozenWithFallback(policies: DevicePolicies, pkg: String, frozen: Boolean): Boolean {
		val hideOk = policies.setApplicationHidden(pkg, frozen) ||
				policies.invoke(DevicePolicyManager::isApplicationHidden, pkg) == frozen
		return if (frozen) {
			if (hideOk) true
			else policies.invoke(DevicePolicyManager::setPackagesSuspended, arrayOf(pkg), true).isEmpty()
		} else {
			val unsuspendOk = policies.invoke(DevicePolicyManager::setPackagesSuspended, arrayOf(pkg), false).isEmpty()
			hideOk && unsuspendOk
		}
	}

	@JvmStatic fun unfreezeInitiallyFrozenSystemApp(app: PrismAppInfo) =
		Shuttle(app.context(), to = app.user).invokeNoThrows(with = app.packageName) {
			PrismManager.ensureAppHiddenState(this, it, false) }.toastIfNull(app.context())
			?.also { if (it) stopTreatingHiddenSysAppAsDisabled(app) }

	private fun stopTreatingHiddenSysAppAsDisabled(app: PrismAppInfo) =
		Shuttle(app.context(), to = app.user).invokeNoThrows(with = app.packageName) {
			ClonedHiddenSystemApps.setCloned(this, it) }.toastIfNull(app.context())

	private fun <T> T?.toastIfNull(context: Context) = apply {
		if (this == null) Toasts.showLong(context, R.string.prompt_space_not_ready)
	}
}
