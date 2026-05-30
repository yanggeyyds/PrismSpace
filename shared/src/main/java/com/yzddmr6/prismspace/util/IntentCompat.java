package com.yzddmr6.prismspace.util;

import static android.os.Build.VERSION_CODES.O;

import androidx.annotation.RequiresApi;

public final class IntentCompat {

	public static final String ACTION_SHOW_APP_INFO = "android.intent.action.SHOW_APP_INFO";
	public static final String EXTRA_PACKAGE_NAME = "android.intent.extra.PACKAGE_NAME";
	@RequiresApi(O) public static final String EXTRA_AUTO_LAUNCH_SINGLE_CHOICE = "android.intent.extra.AUTO_LAUNCH_SINGLE_CHOICE";

	private IntentCompat() {}
}
