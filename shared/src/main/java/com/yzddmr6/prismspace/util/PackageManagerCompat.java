package com.yzddmr6.prismspace.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;

import static android.content.pm.PackageManager.MATCH_DISABLED_COMPONENTS;
import static android.content.pm.PackageManager.MATCH_UNINSTALLED_PACKAGES;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.N;

/**
 * Compatibility helpers for package-manager APIs used across users.
 */
public final class PackageManagerCompat {

	@SuppressLint("NewApi") public int getPackageUid(final String pkg, final int user) throws PackageManager.NameNotFoundException {
		if (SDK_INT >= N) {
			final int uid = mPackageManager.getPackageUid(pkg, MATCH_UNINSTALLED_PACKAGES | MATCH_DISABLED_COMPONENTS);
			return user == UserHandles.MY_USER_ID ? uid : UserHandles.getUid(user, UserHandles.getAppId(uid));
		}
		return mPackageManager.getPackageUid(pkg, user);
	}

	public int getPackageUid(final String pkg) throws PackageManager.NameNotFoundException {
		return getPackageUid(pkg, UserHandles.MY_USER_ID);
	}

	public PackageManagerCompat(final Context context) {
		mPackageManager = context.getPackageManager();
	}

	private final PackageManager mPackageManager;
}
