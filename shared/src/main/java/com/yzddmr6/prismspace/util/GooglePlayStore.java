package com.yzddmr6.prismspace.util;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.preference.Preference;

public final class GooglePlayStore {

	public static final String PACKAGE_NAME = "com.android.vending";
	private static final String APP_URL_PREFIX = "https://play.google.com/store/apps/details?id=";

	public static void showApp(final Context context, final String pkg) {
		final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(APP_URL_PREFIX + pkg)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		updatePlayUrlIntent(context, intent);
		try {
			context.startActivity(intent);
		} catch (final ActivityNotFoundException ignored) {}
	}

	public static void updatePreferenceIntent(final Context context, final Preference preference) {
		updatePlayUrlIntent(context, preference.getIntent());
	}

	private static void updatePlayUrlIntent(final Context context, final Intent intent) {
		if (intent == null || intent.getPackage() != null) return;
		final Uri uri = intent.getData();
		if (uri == null) return;
		intent.setPackage(PACKAGE_NAME);
		final ComponentName component = intent.resolveActivity(context.getPackageManager());
		if (component != null) intent.setComponent(component);
		else intent.setPackage(null);
	}

	private GooglePlayStore() {}
}
