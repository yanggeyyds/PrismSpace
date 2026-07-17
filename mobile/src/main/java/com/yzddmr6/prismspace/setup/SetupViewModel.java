package com.yzddmr6.prismspace.setup;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.annotation.CheckResult;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.yzddmr6.prismspace.util.UserHandles;
import com.yzddmr6.prismspace.analytics.Analytics;
import com.yzddmr6.prismspace.engine.PrismManager;
import com.yzddmr6.prismspace.mobile.R;
import com.yzddmr6.prismspace.util.DeviceAdmins;
import com.yzddmr6.prismspace.util.DevicePolicies;
import com.yzddmr6.prismspace.util.Modules;
import com.yzddmr6.prismspace.util.Users;

import java.util.Optional;

import static android.app.admin.DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.O;
import static android.os.Build.VERSION_CODES.TIRAMISU;
import static com.yzddmr6.prismspace.analytics.Analytics.Param.ITEM_ID;
import static com.yzddmr6.prismspace.analytics.Analytics.Param.ITEM_NAME;

/**
 * View model for setup activity.
 *
 * <p>Owns the DPM prerequisite-check state machine ({@link #checkManagedProvisioningPrerequisites})
 * and exposes the resolved error state via public fields {@link #message}, {@link #message_params},
 * and {@link #action_extra}. The Compose UI layer
 * ({@link com.yzddmr6.prismspace.setup.compose.SetupController}) consumes these fields and
 * dispatches {@link #buildManagedProfileProvisioningIntentPublic} via an
 * {@code ActivityResultLauncher}.
 *
 * <p>The legacy Fragment-driven state machine and {@link android.os.Parcelable}
 * boilerplate were removed. Only the static DPM logic and the error-state DTO surface remain.
 *
 * Created by Oasis on 2016/4/19.
 */
public class SetupViewModel {

	public @StringRes int message;
	public @Nullable Object[] message_params;
	public int action_extra;

	/** @return null if all prerequisites are met. */
	public static @CheckResult SetupViewModel checkManagedProvisioningPrerequisites(final Context context, final boolean ignore_incomplete_setup) {
		final PackageManager pm = context.getPackageManager();
		if (buildManagedProfileProvisioningIntent(context).resolveActivity(pm) == null)
			return buildErrorVM(R.string.setup_error_missing_managed_provisioning, reason("lack_managed_provisioning"));

		// Check for incomplete provisioning, before DPM.isProvisioningAllowed() check which returns true in this case.
		if (! Users.hasProfile()) for (final int profile_id : PrismManager.getProfileIdsIncludingDisabled(context)) {
			if (Users.isParentProfile(profile_id)) continue;
			final Optional<ComponentName> owner = DevicePolicies.getProfileOwnerAsUser(context, UserHandles.of(profile_id));
			if (owner == null || ! owner.isPresent()) continue;
			final ComponentName profile_owner = owner.get();
			if (! Modules.MODULE_ENGINE.equals(profile_owner.getPackageName())) {
				final CharSequence label = readOwnerLabel(context, profile_owner);
				reason("existent_work_profile").with(ITEM_ID, profile_owner.getPackageName()).with(ITEM_NAME, label != null ? label.toString() : null).send();
				continue;
			}
			if (ignore_incomplete_setup) continue;
			return buildErrorVM(R.string.setup_error_provisioning_incomplete, reason("provisioning_incomplete")).withExtraAction(R.string.button_have_checked);
		}

		// DPM.isProvisioningAllowed() is the one-stop prerequisites checking.
		final DevicePolicyManager dpm = context.getSystemService(DevicePolicyManager.class);
		if (dpm != null && dpm.isProvisioningAllowed(DevicePolicyManager.ACTION_PROVISION_MANAGED_DEVICE))
			Analytics.$().event("device_provision_allowed").send();	// Special analytics
		if (dpm != null && dpm.isProvisioningAllowed(ACTION_PROVISION_MANAGED_PROFILE)) return null;

		final boolean has_managed_users = pm.hasSystemFeature(PackageManager.FEATURE_MANAGED_USERS);
		if (! has_managed_users)
			return buildErrorVM(R.string.setup_error_managed_profile_not_supported, reason("lack_managed_users"));

		final String device_owner = new DevicePolicies(context).getDeviceOwner();
		if (device_owner != null) {
			// Dhizuku as device owner is a supported configuration: it shares Device Owner privileges
			// with PrismSpace so the managed profile can be created through Dhizuku's privileged worker,
			// bypassing the standard ACTION_PROVISION_MANAGED_PROFILE flow (which is blocked when a
			// device owner already exists). Only offer this path when Dhizuku has authorized us.
			final boolean is_dhizuku = "com.rosan.dhizuku".equals(device_owner);
			final boolean is_dhizuku_authorized = is_dhizuku && isDhizukuAuthorized(context);
			if (is_dhizuku_authorized) {
				reason("dhizuku_device_owner").with(ITEM_ID, device_owner).send();
				return null;	// Prerequisites OK — SetupController will use the Dhizuku provisioning path.
			}

			CharSequence owner_label = null;
			try {
				owner_label = pm.getApplicationInfo(device_owner, PackageManager.MATCH_UNINSTALLED_PACKAGES).loadLabel(pm);
			} catch (final PackageManager.NameNotFoundException ignored) {}		// Should never happen.

			final SetupViewModel error = buildErrorVM(R.string.setup_error_managed_device, reason("managed_device").with(ITEM_ID, device_owner));
			error.message_params = new String[] { owner_label != null ? owner_label.toString() : device_owner };
			if (is_dhizuku)
				error.action_extra = R.string.button_setup_space_with_dhizuku;	// Dhizuku installed but not yet authorized.
			else
				error.action_extra = 0;		// Disable the manual-setup prompt, because device owner cannot be removed by 3rd-party.
			return error;
		}

		reason("disallowed").send();		// Disallowed by DPC for unknown reason, just log this but let user have a try.
		return null;
	}

	private static Analytics.Event reason(final String reason) {
		return Analytics.$().event("setup_prism_failure").with(Analytics.Param.ITEM_CATEGORY, reason);
	}

	/** Returns true if Dhizuku is activated and has granted permission to this app. */
	private static boolean isDhizukuAuthorized(final Context context) {
		try {
			final Class<?> dhizukuClass = Class.forName("com.rosan.dhizuku.api.Dhizuku");
			final java.lang.reflect.Method init = dhizukuClass.getMethod("init", Context.class);
			final java.lang.reflect.Method isGranted = dhizukuClass.getMethod("isPermissionGranted");
			return (boolean) init.invoke(null, context.getApplicationContext()) && (boolean) isGranted.invoke(null);
		} catch (final Exception e) {
			return false;
		}
	}

	private static SetupViewModel buildErrorVM(final @StringRes int message, final @Nullable Analytics.Event event) {
		if (event != null) event.send();
		final SetupViewModel next = new SetupViewModel();
		next.message = message;
		next.action_extra = R.string.button_setup_help;	// Default extra action, can be overridden by withExtraAction().
		return next;
	}

	private SetupViewModel withExtraAction(final @StringRes int text) { action_extra = text; return this; }

	/** Public accessor for the Compose {@link com.yzddmr6.prismspace.setup.compose.SetupController}.
	 *  Same intent the Java state machine launched; do not modify. */
	public static Intent buildManagedProfileProvisioningIntentPublic(final Context context) {
		return buildManagedProfileProvisioningIntent(context);
	}

	private static Intent buildManagedProfileProvisioningIntent(final Context context) {
		final Intent intent = new Intent(ACTION_PROVISION_MANAGED_PROFILE);
		intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME, DeviceAdmins.getComponentName(context));
		intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_SKIP_ENCRYPTION, true);		// Actually works on Android 7+.
		if (SDK_INT >= O) intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_SKIP_USER_CONSENT, true);
		if (SDK_INT >= TIRAMISU) intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_ALLOW_OFFLINE, true);
		return intent;
	}

	private static CharSequence readOwnerLabel(final Context context, final ComponentName owner) {
		final PackageManager pm = context.getPackageManager();
		try {
			return pm.getReceiverInfo(owner, PackageManager.MATCH_UNINSTALLED_PACKAGES).loadLabel(pm);	// It should be a BroadcastReceiver
		} catch (final PackageManager.NameNotFoundException e) {
			try {
				return pm.getApplicationInfo(owner.getPackageName(), PackageManager.MATCH_UNINSTALLED_PACKAGES).loadLabel(pm);    // If not, use app label
			} catch (final PackageManager.NameNotFoundException ex) {
				return null;
			}
		}
	}

	SetupViewModel() {}

	private static final String TAG = "Prism.Setup";
}
