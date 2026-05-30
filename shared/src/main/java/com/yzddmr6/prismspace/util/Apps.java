package com.yzddmr6.prismspace.util;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Process;
import android.util.Log;

import androidx.annotation.CheckResult;
import androidx.annotation.Nullable;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static android.content.Intent.CATEGORY_LAUNCHER;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.pm.PackageManager.GET_DISABLED_COMPONENTS;
import static android.content.pm.PackageManager.GET_UNINSTALLED_PACKAGES;
import static android.content.pm.PackageManager.MATCH_UNINSTALLED_PACKAGES;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.O_MR1;
import static com.yzddmr6.prismspace.util.Permissions.INTERACT_ACROSS_USERS;

/** Application package helpers used across PrismSpace modules. */
public final class Apps {

	public static Apps of(final Context context) {
		return new Apps(context);
	}

	/** Check whether specified app is installed on the device, even if not installed in current user. */
	public @CheckResult boolean isInstalledOnDevice(final String pkg) {
		try {
			mContext.getPackageManager().getApplicationInfo(pkg, GET_UNINSTALLED_PACKAGES);
			return true;
		} catch (final NameNotFoundException e) {
			return false;
		}
	}

	public void showInMarket(final String pkg) {
		showInMarket(pkg, null, null);
	}

	public void showInMarket(final String pkg, final @Nullable String utmSource, final @Nullable String utmCampaign) {
		final StringBuilder uri = new StringBuilder("market://details?id=").append(pkg);
		if (utmSource != null) uri.append("&utm_source=").append(utmSource);
		if (utmCampaign != null) uri.append("&utm_campaign=").append(utmCampaign);
		final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri.toString())).addFlags(FLAG_ACTIVITY_NEW_TASK);
		try {
			mContext.startActivity(intent);
		} catch (final ActivityNotFoundException e) {
			GooglePlayStore.showApp(mContext, pkg);
		}
	}

	/** Check whether specified app is installed in current user, even if hidden by system. */
	public @CheckResult boolean isInstalledInCurrentUser(final String pkg) {
		try {
			final ApplicationInfo info = mContext.getPackageManager().getApplicationInfo(pkg, GET_UNINSTALLED_PACKAGES);
			return isInstalledInCurrentUser(info);
		} catch (final NameNotFoundException e) {
			return false;
		}
	}

	public static @CheckResult boolean isInstalledInCurrentUser(final ApplicationInfo info) {
		return (info.flags & ApplicationInfo.FLAG_INSTALLED) != 0;
	}

	/** Use {@link #isInstalledInCurrentUser(String)} or {@link #isInstalledOnDevice(String)} instead. */
	@Deprecated public @CheckResult boolean isInstalled(final String pkg) {
		return isInstalledInCurrentUser(pkg);
	}

	public static boolean isPrivileged(final ApplicationInfo app) {
		final Integer flags = getPrivateFlags(app);
		if (flags == null) return isSystem(app);
		return (flags & PRIVATE_FLAG_PRIVILEGED) != 0;
	}

	public static @Nullable Integer getPrivateFlags(final ApplicationInfo app) {
		if (ApplicationInfo_privateFlags == NO_SUCH_FIELD) return null;
		try {
			if (ApplicationInfo_privateFlags == null) {
				ApplicationInfo_privateFlags = ApplicationInfo.class.getField("privateFlags");
				if (ApplicationInfo_privateFlags.getType() != int.class) throw new NoSuchFieldException();
			}
			return (Integer) ApplicationInfo_privateFlags.get(app);
		} catch (final NoSuchFieldException | IllegalAccessException e) {
			ApplicationInfo_privateFlags = NO_SUCH_FIELD;
			Log.e(TAG, "Incompatible ROM: No public integer field - ApplicationInfo.privateFlags");
			return null;
		}
	}

	public boolean launch(final String pkg) {
		try {
			mContext.startActivity(new Intent(Intent.ACTION_MAIN).addCategory(CATEGORY_LAUNCHER).setPackage(pkg).addFlags(FLAG_ACTIVITY_NEW_TASK));
			return true;
		} catch (final ActivityNotFoundException e) {
			return false;
		}
	}

	private static final int PRIVATE_FLAG_PRIVILEGED = 1 << 3;

	public static boolean isPrivileged(final PackageManager pm, final int uid) {
		if (uid < 0) return false;
		if (uid < Process.FIRST_APPLICATION_UID) return true;
		final String[] pkgs = pm.getPackagesForUid(uid);
		if (pkgs == null) return false;
		for (final String pkg : pkgs) try {
			if (isPrivileged(pm.getApplicationInfo(pkg, 0))) return true;
		} catch (final NameNotFoundException ignored) {}
		return false;
	}

	public static boolean isSystem(final ApplicationInfo app) {
		return (app.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
	}

	public @CheckResult boolean isEnabled(final String pkg) throws NameNotFoundException {
		final ApplicationInfo appInfo = mContext.getPackageManager().getApplicationInfo(pkg, 0);
		return appInfo.enabled;
	}

	/** Check whether specified app is installed in current user, enabled and not hidden. */
	public @CheckResult boolean isAvailable(final String pkg) {
		try {
			return isEnabled(pkg);
		} catch (final NameNotFoundException e) {
			return false;
		}
	}

	public @CheckResult boolean isInstalledBy(final String... installerPkgs) {
		try {
			return Arrays.asList(installerPkgs).contains(mContext.getPackageManager().getInstallerPackageName(mContext.getPackageName()));
		} catch (final IllegalArgumentException e) {
			return false;
		}
	}

	public @Nullable ApplicationInfo getAppInfo(final String pkg) {
		try {
			@SuppressLint("WrongConstant") final ApplicationInfo info =
					mContext.getPackageManager().getApplicationInfo(pkg, getFlagsMatchKnownPackages(mContext));
			return info;
		} catch (final NameNotFoundException e) {
			@SuppressLint("WrongConstant") final List<ResolveInfo> resolves = mContext.getPackageManager().queryIntentActivities(
					new Intent(Intent.ACTION_MAIN).addCategory(CATEGORY_LAUNCHER).setPackage(pkg),
					MATCH_ANY_USER | GET_UNINSTALLED_PACKAGES | GET_DISABLED_COMPONENTS);
			if (resolves != null && ! resolves.isEmpty()) return resolves.get(0).activityInfo.applicationInfo;
			return null;
		}
	}

	public @Nullable PackageInfo getPackageInfo(final String pkg, final int flags) {
		try {
			@SuppressLint("WrongConstant") final PackageInfo info =
					mContext.getPackageManager().getPackageInfo(pkg, flags | getFlagsMatchKnownPackages(mContext));
			return info;
		} catch (final NameNotFoundException e) {
			return null;
		}
	}

	public static int getFlagsMatchKnownPackages(final Context context) {
		return MATCH_UNINSTALLED_PACKAGES | (SDK_INT <= O_MR1 || Permissions.has(context, INTERACT_ACROSS_USERS) ? MATCH_ANY_USER : 0);
	}

	public @CheckResult CharSequence getAppName(final String pkg) {
		final ApplicationInfo info = getAppInfo(pkg);
		return info != null ? getAppName(info) : pkg;
	}

	public @CheckResult CharSequence getAppName(final ApplicationInfo appInfo) {
		try {
			return appInfo.loadLabel(mContext.getPackageManager());
		} catch (final RuntimeException e) {
			return appInfo.packageName;
		}
	}

	public @CheckResult String getAppNames(final Collection<String> pkgs, final String separator) {
		final StringBuilder appNames = new StringBuilder();
		for (final String pkg : pkgs)
			appNames.append(separator).append(getAppName(pkg));
		return appNames.substring(separator.length());
	}

	private Apps(final Context context) {
		mContext = context;
	}

	private final Context mContext;

	private static Field ApplicationInfo_privateFlags;
	private static final Field NO_SUCH_FIELD;
	static {
		try {
			NO_SUCH_FIELD = Apps.class.getDeclaredField("NO_SUCH_FIELD");
		} catch (final NoSuchFieldException e) {
			throw new LinkageError();
		}
	}

	private static final int MATCH_ANY_USER = 0x00400000;
	private static final String TAG = "Apps";
}
