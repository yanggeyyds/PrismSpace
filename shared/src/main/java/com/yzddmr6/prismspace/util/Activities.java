package com.yzddmr6.prismspace.util;

import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;

import androidx.annotation.Nullable;

public final class Activities {

	public static @Nullable Activity findActivityFrom(final Context context) {
		if (context instanceof Activity) return (Activity) context;
		if (context instanceof Application || context instanceof Service) return null;
		if (! (context instanceof ContextWrapper)) return null;
		final Context baseContext = ((ContextWrapper) context).getBaseContext();
		if (baseContext == context) return null;
		return findActivityFrom(baseContext);
	}

	public static void startActivity(final Context context, final Intent intent) {
		final Activity activity = findActivityFrom(context);
		if (activity != null) activity.startActivity(intent);
		else context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
	}

	private Activities() {}
}
