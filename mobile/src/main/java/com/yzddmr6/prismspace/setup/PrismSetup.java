package com.yzddmr6.prismspace.setup;

import static com.yzddmr6.prismspace.analytics.Analytics.Param.CONTENT;
import static java.lang.Boolean.FALSE;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.Process;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.annotation.Nullable;

import com.yzddmr6.prismspace.util.Dialogs;
import com.yzddmr6.prismspace.common.app.AppInfo;
import com.yzddmr6.prismspace.common.app.AppListProvider;
import com.yzddmr6.prismspace.util.Hack;
import com.yzddmr6.prismspace.analytics.Analytics;
import com.yzddmr6.prismspace.mobile.BuildConfig;
import com.yzddmr6.prismspace.mobile.R;
import com.yzddmr6.prismspace.shuttle.Shuttle;
import com.yzddmr6.prismspace.util.DeviceAdmins;
import com.yzddmr6.prismspace.util.DevicePolicies;
import com.yzddmr6.prismspace.util.Hacks;
import com.yzddmr6.prismspace.util.Hacks.UserManagerHack;
import com.yzddmr6.prismspace.util.Hacks.UserManagerHack.UserInfo;
import com.yzddmr6.prismspace.util.Modules;
import com.yzddmr6.prismspace.util.OwnerUser;
import com.yzddmr6.prismspace.util.ProfileUser;
import com.yzddmr6.prismspace.util.SafeAsyncTask;
import com.yzddmr6.prismspace.util.Users;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import eu.chainfire.libsuperuser.Shell;

/**
 * Implementation of PrismSpace / MainSpace setup & shutdown.
 *
 * Created by Oasis on 2017/3/8.
 */
public class PrismSetup {

	static final String RES_MAX_USERS = "config_multiuserMaximumUsers";
	private static final String PACKAGE_VERIFIER_INCLUDE_ADB = "verifier_verify_adb_installs";

	public static void requestProfileOwnerSetupWithRoot(final Activity activity) {
		// Pre-flight: if a managed profile already exists in this user,
		// the OS will reject `pm create-user --managed` (max 1 managed profile per parent).
			// Skip Shell.SU.run entirely and surface a clear message instead of triggering
			// the silent-hang path from a rejected managed-profile create command.
		if (hasAnyExistingProfile(activity)) {
			showProfileAlreadyExistsDialog(activity);
			return;
		}
		final ProgressDialog progress = ProgressDialog.show(activity, null, activity.getString(R.string.setup_root_profile_progress), true);
		// Create the managed profile.
		final List<String> commands = Arrays.asList("setprop fw.max_users 10",
				"pm create-user --profileOf " + Users.toId(Process.myUserHandle()) + " --managed PrismSpace", "echo END");
		SafeAsyncTask.execute(activity, context -> Shell.SU.run(commands), (context, result) -> {
			final List<UserInfo> profiles = Hack.into(requireNonNull(context.getSystemService(Context.USER_SERVICE)))
					.with(UserManagerHack.class).getProfiles(Users.toId(Users.current()));
			final Optional<UserHandle> profile_pending_setup = profiles.stream().map(UserInfo::getUserHandle)
					.filter(profile -> ! profile.equals(Users.current()) && isProfileWithoutOwner(context, profile)).findFirst();	// Not yet set up as profile owner
			if (! profile_pending_setup.isPresent()) {		// Profile creation failed — ALWAYS dismiss progress dialog.
				// Always route through the error dialog when root is denied or
				// libsuperuser returns empty stdout.
					Analytics.$().event("setup_prism_root_failed")
						.withRaw("phase", "1")
						.withRaw("commands", commands.stream().collect(joining("\n")))
						.withRaw("fw_max_users", String.valueOf(getSysPropMaxUsers()))
						.withRaw("config_multiuserMaximumUsers", String.valueOf(getResConfigMaxUsers()))
						.with(CONTENT, result == null ? "<null>" : result.stream().collect(joining("\n"))).send();
				dismissProgressAndShowError(context, progress, 1);
				return;
			}

			installPrismInProfileWithRoot(context, progress, profile_pending_setup.get());
		});
	}

	/** Return true if user 0 already has any profile other than itself. */
	private static boolean hasAnyExistingProfile(final Context context) {
		try {
			final List<UserInfo> profiles = Hack.into(requireNonNull(context.getSystemService(Context.USER_SERVICE)))
					.with(UserManagerHack.class).getProfiles(Users.toId(Users.current()));
			return profiles.stream().map(UserInfo::getUserHandle).anyMatch(p -> ! p.equals(Users.current()));
		} catch (final RuntimeException ignored) { return false; }	// Reflection failure: don't block, fall through.
	}

	private static void showProfileAlreadyExistsDialog(final Activity activity) {
		Dialogs.buildAlert(activity, R.string.dialog_title_warning, R.string.dialog_root_setup_profile_exists)
				.withOkButton(null).show();
			Analytics.$().event("setup_prism_root_skipped").withRaw("reason", "existing_profile").send();
	}

	private static boolean isProfileWithoutOwner(final Context context, final UserHandle profile) {
		final Optional<ComponentName> owner = DevicePolicies.getProfileOwnerAsUser(context, profile);
		return owner == null || ! owner.isPresent();
	}

	// Install PrismSpace into the managed profile.
	private static void installPrismInProfileWithRoot(final Activity activity, final ProgressDialog progress, final UserHandle profile) {
		// Disable package verifier before installation, to avoid hanging too long.
		final StringBuilder commands = new StringBuilder();
		final String adb_verify_value_before = Settings.Global.getString(activity.getContentResolver(), PACKAGE_VERIFIER_INCLUDE_ADB);
		if (adb_verify_value_before == null || Integer.parseInt(adb_verify_value_before) != 0)
			commands.append("settings put global ").append(PACKAGE_VERIFIER_INCLUDE_ADB).append(" 0 ; ");

		final ApplicationInfo info; try {
			info = activity.getPackageManager().getApplicationInfo(Modules.MODULE_ENGINE, 0);
		} catch (final NameNotFoundException e) {
			// Defensively dismiss progress and show a clear error.
			dismissProgressAndShowError(activity, progress, 2);
			return;
		}
		final int profile_id = Users.toId(profile);
		commands.append("pm install -r --user ").append(profile_id).append(' ');
		if (BuildConfig.DEBUG) commands.append("-t ");
		commands.append(info.sourceDir).append(" && ");

		if (adb_verify_value_before == null) commands.append("settings delete global ").append(PACKAGE_VERIFIER_INCLUDE_ADB).append(" ; ");
		else commands.append("settings put global ").append(PACKAGE_VERIFIER_INCLUDE_ADB).append(' ').append(adb_verify_value_before).append(" ; ");

		// All following commands must be executed all together with the above one, since this app process will be killed upon "pm install".
		final String flat_admin_component = DeviceAdmins.getComponentName(activity).flattenToString();
		commands.append("dpm set-profile-owner --user ").append(profile_id).append(" ").append(flat_admin_component);
		commands.append(" && am start-user ").append(profile_id);

		SafeAsyncTask.execute(activity, context -> Shell.SU.run(commands.toString()), (context, result) -> {
			final LauncherApps launcher_apps = requireNonNull((LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE));
			if (launcher_apps.getActivityList(context.getPackageName(), profile).isEmpty()) {
					Analytics.$().event("setup_prism_root_failed").withRaw("phase", "2").withRaw("command", commands.toString())
						.with(CONTENT, result == null ? "<null>" : result.stream().collect(joining("\n"))).send();
				dismissProgressAndShowError(context, progress, 2);
				return;
			}
					// Dismiss the progress dialog explicitly; package installation may leave this
					// foreground process alive on modern Android versions.
			if (progress.isShowing()) progress.dismiss();
				Analytics.$().event("setup_prism_root_done").send();
			context.finish();
		});
	}

	private static void dismissProgressAndShowError(final Activity activity, final ProgressDialog progress, final int stage) {
		progress.dismiss();
		Dialogs.buildAlert(activity, null, activity.getString(R.string.dialog_space_setup_failed, stage)).withOkButton(null).show();
	}

	static @Nullable Integer getSysPropMaxUsers() {
		return Hacks.SystemProperties_getInt.invoke("fw.max_users", - 1).statically();
	}

	static @Nullable Integer getResConfigMaxUsers() {
		final Resources sys_res = Resources.getSystem();
		final int res = sys_res.getIdentifier(RES_MAX_USERS, "integer", "android");
		if (res == 0) return null;
		return Resources.getSystem().getInteger(res);
	}

	@OwnerUser public static void requestDeviceOrProfileOwnerDeactivation(final Activity activity) {
		new AlertDialog.Builder(activity).setTitle(R.string.dialog_title_warning).setMessage(R.string.dialog_rescind_message)
				.setPositiveButton(android.R.string.no, null).setNeutralButton(R.string.action_rescind, (d, w) -> {
					try {
						final DevicePolicies policies = new DevicePolicies(activity);
						final AppListProvider<AppInfo> provider = AppListProvider.getInstance(activity);
						final Stream<AppInfo> apps = provider.installedAppsInOwnerUser().stream();

						final List<String> frozen_pkgs = apps.filter(app -> app.isHidden()).map(app -> app.packageName).collect(toList());
						for (final String pkg : frozen_pkgs)
							policies.setApplicationHidden(pkg, false);

						final String[] suspended_pkgs = apps.filter(AppInfo::isSuspended).map(app -> app.packageName).toArray(String[]::new);
						policies.invoke(DevicePolicyManager::setPackagesSuspended, suspended_pkgs, false);
					} finally {
						deactivateDeviceOrProfileOwner(activity);
					}
				}).show();
	}

	private static void deactivateDeviceOrProfileOwner(final Activity activity) {
		Analytics.$().event("action_deactivate").send();
		final DevicePolicies policies = new DevicePolicies(activity);
		if (policies.isActiveDeviceOwner())
			policies.getManager().clearDeviceOwnerApp(activity.getPackageName());
		else clearProfileOwner(policies);
		try {	// Since Android 7.1, clearDeviceOwnerApp() itself does remove active device-admin,
			policies.execute(DevicePolicyManager::removeActiveAdmin);
		} catch (final SecurityException ignored) {}		//   thus SecurityException will be thrown here.

		activity.finishAffinity();	// Finish the whole activity stack.
		System.exit(0);		// Force termination of the whole app, to avoid potential inconsistency.
	}

	@SuppressLint("NewApi"/* hidden before N */) private static void clearProfileOwner(final DevicePolicies policies) {
		policies.execute(DevicePolicyManager::clearProfileOwner);
	}

	@ProfileUser public static void requestProfileRemoval(final Activity activity) {
		if (Users.isParentProfile()) throw new IllegalStateException("Must be called in managed profile");
		if (! new DevicePolicies(activity).isProfileOwner()) {
			showPromptForProfileManualRemoval(activity);
			return;
		}
		new AlertDialog.Builder(activity).setTitle(R.string.dialog_title_warning)
				.setMessage(R.string.dialog_destroy_message)
				.setPositiveButton(android.R.string.no, null)
				.setNeutralButton(R.string.action_destroy, (dd, ww) -> requestProfileRemovalConfirmed(activity)).show();
	}

	private static void requestProfileRemovalConfirmed(final Activity activity) {
		if (new Shuttle(activity, Users.getParentProfile()).invokeNoThrows(c -> new DevicePolicies(c).isProfileOwner()) == FALSE)
			destroyProfileLegacy(activity);
		else new AlertDialog.Builder(activity).setTitle(R.string.dialog_title_warning)
				.setMessage(R.string.dialog_destroy_message_for_managed_user)
				.setPositiveButton(android.R.string.no, null)
				.setNeutralButton(R.string.action_destroy, (d, w) -> destroyProfileLegacy(activity)).show();
	}

	private static void showPromptForProfileManualRemoval(final Activity activity) {
		final AlertDialog.Builder dialog = new AlertDialog.Builder(activity).setMessage(R.string.dialog_cannot_destroy_message)
				.setNegativeButton(android.R.string.ok, null);
		final Intent intent = new Intent(Settings.ACTION_SYNC_SETTINGS);
		if (intent.resolveActivity(activity.getPackageManager()) == null) intent.setAction(Settings.ACTION_SETTINGS);	// Fallback to entrance of Settings
		if (intent.resolveActivity(activity.getPackageManager()) != null)
			dialog.setPositiveButton(R.string.open_settings, (d, w) -> activity.startActivity(intent));
		dialog.show();
		Analytics.$().event("cannot_destroy").send();
	}

	/** Compose-path entry: surface the system manual-removal prompt (used when the
	 *  feedback mapper reports routeToSystemRemoval). Delegates to the private impl. */
	@ProfileUser public static void promptManualRemoval(final Activity activity) {
		showPromptForProfileManualRemoval(activity);
	}

	/** Public entry for the Compose two-step confirm flow. Returns a DestroyProfileResult
	 *  the caller maps via destroyProfileFeedback(...) for differentiated UI feedback. */
	@ProfileUser public static DestroyProfileResult destroyProfileDirect(final Activity activity) {
		return destroyProfile(activity);
	}

	@ProfileUser private static DestroyProfileResult destroyProfile(final Activity activity) {
		if (Users.isParentProfile()) throw new IllegalStateException("Must be called in managed profile.");
		final DevicePolicies policies = new DevicePolicies(activity);
		// Pre-flight: without profile-owner authority wipeData would only throw — detect explicitly.
		if (! policies.isProfileOwner()) return DestroyProfileResult.NotProfileOwner.INSTANCE;
		try {
				// Single irreversible operation. wipeData(0) removes the managed profile entirely,
				// and the OS tears down its cross-profile intent filters as part of removal.
			policies.getManager().wipeData(0);
			return DestroyProfileResult.Success.INSTANCE;
		} catch(final RuntimeException e) {
			return new DestroyProfileResult.Failed(e.getMessage());
		}
	}

	/** Legacy entry preserving the original "any failure -> manual-removal prompt" UX
	 *  for the non-Compose callers (requestProfileRemovalConfirmed paths). */
	@ProfileUser private static void destroyProfileLegacy(final Activity activity) {
		if (! (destroyProfile(activity) instanceof DestroyProfileResult.Success))
			showPromptForProfileManualRemoval(activity);
	}
}
