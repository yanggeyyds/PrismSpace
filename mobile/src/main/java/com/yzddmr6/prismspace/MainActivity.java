package com.yzddmr6.prismspace;

import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
import static android.content.pm.PackageManager.DONT_KILL_APP;

import android.app.SearchManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.Log;

import androidx.core.view.WindowCompat;
import androidx.fragment.app.FragmentActivity;

import com.yzddmr6.prismspace.analytics.Analytics;
import com.yzddmr6.prismspace.analytics.Analytics.Property;
import com.yzddmr6.prismspace.mobile.BuildConfig;
import com.yzddmr6.prismspace.mobile.R;
import com.yzddmr6.prismspace.prism.compose.host.PrismComposeHostFragment;
import com.yzddmr6.prismspace.prism.compose.nav.AppLaunchSignals;
import com.yzddmr6.prismspace.setup.SetupActivity;
import com.yzddmr6.prismspace.util.CallerAwareActivity;
import com.yzddmr6.prismspace.util.DeviceAdmins;
import com.yzddmr6.prismspace.util.DevicePolicies;
import com.yzddmr6.prismspace.util.Loopers;
import com.yzddmr6.prismspace.util.Modules;
import com.yzddmr6.prismspace.util.PrismLocale;
import com.yzddmr6.prismspace.util.Scopes;
import com.yzddmr6.prismspace.util.Users;

import java.util.List;
import java.util.Optional;

public class MainActivity extends FragmentActivity {

	@Override protected void attachBaseContext(final Context newBase) {
		super.attachBaseContext(PrismLocale.wrap(newBase));   // per-app 中/英 language override
	}

	@Override protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (! Users.isParentProfile()) {
			if (new DevicePolicies(this).isProfileOwner()) {    // Should generally not run in profile, unless the managed profile provision is interrupted or manually provision is not complete.
				onCreateInProfile();
				finish();
				} else startSetupWizard();
			return;
		}
		Users.refreshUsers(this);     // Managed-profile create/remove can happen outside our task; never trust a hot-process cache at the entry point.
		final String caller = CallerAwareActivity.getCallingPackage(this);
		if (Modules.MODULE_ENGINE.equals(caller)) Users.refreshUsers(this);     // Possibly started by PrismProvisioning, refresh user state as profile or its owner may be changed.

		mIsDeviceOwner = new DevicePolicies(this).isProfileOrDeviceOwnerOnCallingUser();
		if (mIsDeviceOwner) {
			startMainUi(savedInstanceState);	// As device owner, always show main UI.
			return;
		}
		if (! Users.hasProfile()) {					// Nothing setup yet
			Log.i(TAG, "Profile not setup yet");
			startSetupWizard();
			return;
		}
		final UserHandle profile = Users.profile;

		final LauncherApps launcher_apps = (LauncherApps) getSystemService(Context.LAUNCHER_APPS_SERVICE);
		final List<LauncherActivityInfo> our_activities_in_launcher;
		if (launcher_apps != null && ! (our_activities_in_launcher = launcher_apps.getActivityList(getPackageName(), profile)).isEmpty()
				&& our_activities_in_launcher.get(0).getComponentName().getClassName().equals(MainActivity.class.getName())) {
			// Main activity is left enabled, probably due to pending post-provisioning in manual setup. Some domestic ROMs may block implicit broadcast, causing ACTION_USER_INITIALIZE being dropped.
			Analytics.$().event("profile_provision_leftover").send();
			Log.w(TAG, "Setup in PrismSpace is not complete, continue it now.");
			try {
				launcher_apps.startMainActivity(our_activities_in_launcher.get(0).getComponentName(), profile, null, null);
			} catch (final RuntimeException e) {
				Analytics.$().logAndReport(TAG, "Error starting self in profile " + Users.toId(profile), e);
				startSetupWizard();
			}
			finish();
			return;
		}
		startMainUi(savedInstanceState);
	}

	@Override protected void onNewIntent(final Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
		// Launcher taps on a running task land here. Do NOT signal reset-to-Home — that would
		// override the last visited tab. The signal only fires on fresh MainActivity creation
		// (see startMainUi below, gated on savedInstanceState == null).
	}

	private void onCreateInProfile() {
		final DevicePolicies policies = new DevicePolicies(this);
		if (! policies.invoke(DevicePolicyManager::isAdminActive)) {
			Analytics.$().event("inactive_device_admin").send();
			startActivity(new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
					.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, DeviceAdmins.getComponentName(this))
					.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.dialog_reactivate_message)));
			return;
		}
		final PackageManager pm = getPackageManager();
		final List<ResolveInfo> resolves = pm.queryBroadcastReceivers(new Intent(Intent.ACTION_USER_INITIALIZE).setPackage(Modules.MODULE_ENGINE), 0);
		final Optional<ResolveInfo> resolve = resolves.stream().filter(r ->
				r.activityInfo.name.startsWith("com.yzddmr6.prismspace.provision")).findFirst();
		if (resolve.isPresent()) {
			Log.w(TAG, "Manual provisioning is pending, resume it now.");
			Analytics.$().event("profile_post_provision_pending").send();
			final ActivityInfo receiver = resolve.get().activityInfo;
			sendBroadcast(new Intent().setClassName(receiver.packageName, receiver.name));
		} else {    // Receiver disabled but launcher entrance is left enabled. The best bet is just disabling the launcher entrance. No provisioning attempt any more.
			Log.w(TAG, "Manual provisioning is finished, but launcher activity is still left enabled. Disable it now.");
			Analytics.$().event("profile_post_provision_activity_leftover").send();
			pm.setComponentEnabledSetting(new ComponentName(this, getClass()), COMPONENT_ENABLED_STATE_DISABLED, DONT_KILL_APP);
		}
	}

	private void startMainUi(final Bundle savedInstanceState) {
		WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
		getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
		getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
		setContentView(R.layout.activity_main);
		if (savedInstanceState != null) return;
		// Fresh MainActivity creation (cold start, force-kill restart). System-killed restoration
		// has savedInstanceState != null and returns above, preserving the last visited tab.
		AppLaunchSignals.INSTANCE.signalResetToHome();
		final PrismComposeHostFragment fragment = new PrismComposeHostFragment();
		final Intent intent = getIntent();
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			final Bundle arguments = new Bundle();
			arguments.putString(SearchManager.QUERY, intent.getStringExtra(SearchManager.QUERY));
			final UserHandle user = intent.getParcelableExtra(Intent.EXTRA_USER);
			if (user != null) arguments.putParcelable(Intent.EXTRA_USER, user);
			fragment.setArguments(arguments);
		}
		getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
		performOverallAnalyticsIfNeeded();
	}

	private void startSetupWizard() {
		startActivity(new Intent(this, SetupActivity.class));
		performOverallAnalyticsIfNeeded();
		finish();
	}

	private void performOverallAnalyticsIfNeeded() {
		if (! BuildConfig.DEBUG && ! Scopes.boot(this).mark("overall_analytics")) return;
		Loopers.addIdleTask(() -> {
			final Analytics analytics = Analytics.$();
			analytics.setProperty(Property.DeviceOwner, mIsDeviceOwner);
			analytics.setProperty(Property.RemoteConfigAvailable, Config.isRemote());
		});
	}

	private boolean mIsDeviceOwner;

	private static final String TAG = "Prism.Main";
}
