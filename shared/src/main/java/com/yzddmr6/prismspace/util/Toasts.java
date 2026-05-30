package com.yzddmr6.prismspace.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.IntDef;
import androidx.annotation.StringRes;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class Toasts {

	@IntDef({ Toast.LENGTH_SHORT, Toast.LENGTH_LONG }) @Retention(RetentionPolicy.SOURCE) public @interface Duration {}

	private static final Looper MAIN_LOOPER = Looper.getMainLooper();

	public static void showLong(final Context context, final CharSequence text) {
		show(context, text, Toast.LENGTH_LONG);
	}

	public static void showLong(final Context context, final @StringRes int text) {
		show(context, text, Toast.LENGTH_LONG);
	}

	public static void showShort(final Context context, final CharSequence text) {
		show(context, text, Toast.LENGTH_SHORT);
	}

	public static void showShort(final Context context, final @StringRes int text) {
		show(context, text, Toast.LENGTH_SHORT);
	}

	public static void show(final Context context, final @StringRes int text, final @Duration int length) {
		show(context, context.getText(text), length);
	}

	public static void show(final Context context, final CharSequence text, final @Duration int length) {
		if (Looper.myLooper() == MAIN_LOOPER) Toast.makeText(context, text, length).show();
		else new Handler(MAIN_LOOPER).post(() -> Toast.makeText(context, text, length).show());
	}

	private Toasts() {}
}
