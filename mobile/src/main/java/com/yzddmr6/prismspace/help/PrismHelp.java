package com.yzddmr6.prismspace.help;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import androidx.annotation.StringRes;

import com.yzddmr6.prismspace.mobile.R;
import com.yzddmr6.prismspace.util.Activities;
import com.yzddmr6.prismspace.util.Dialogs;

public final class PrismHelp {

	public static void showSetupHelp(final Context context) {
		show(context, R.string.prism_help_setup_title, R.string.prism_help_setup_body);
	}

	public static void showCloneHelp(final Context context) {
		show(context, R.string.prism_help_clone_title, R.string.prism_help_clone_body);
	}

	public static void showMainSpaceHelp(final Context context) {
		show(context, R.string.prism_help_main_space_title, R.string.prism_help_main_space_body);
	}

	private static void show(final Context context, final @StringRes int title, final @StringRes int message) {
		final Activity activity = Activities.findActivityFrom(context);
		if (activity != null) Dialogs.buildAlert(activity, title, message).withOkButton(null).show();
		else Toast.makeText(context, message, Toast.LENGTH_LONG).show();
	}

	private PrismHelp() {}
}
