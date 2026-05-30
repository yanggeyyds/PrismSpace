package com.yzddmr6.prismspace.util;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.CheckResult;

import com.google.android.material.snackbar.Snackbar;

/** PrismSpace mobile Snackbar helpers. */
public class Snackbars {

	private static final int DEFAULT_DURATION = 10_000;
	private static final int MAX_LINES = 3;

	@CheckResult public static Snackbar make(final Activity activity, final int textRes) {
		return make(activity.findViewById(android.R.id.content), textRes);
	}

	@CheckResult public static Snackbar make(final View anchor, final int textRes) {
		return tweak(Snackbar.make(anchor, textRes, DEFAULT_DURATION));
	}

	private static Snackbar tweak(final Snackbar snackbar) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) snackbar.getView().setZ(999);
		final TextView message = snackbar.getView().findViewById(com.google.android.material.R.id.snackbar_text);
		message.setMaxLines(MAX_LINES);
		message.setTextColor(0xffffffff);
		return snackbar;
	}

	private Snackbars() {}
}
