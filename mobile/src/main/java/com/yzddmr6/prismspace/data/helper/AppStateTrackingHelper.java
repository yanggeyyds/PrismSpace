package com.yzddmr6.prismspace.data.helper;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.UserHandle;

import com.yzddmr6.prismspace.data.PrismAppListProvider;
import com.yzddmr6.prismspace.util.Users;

/**
 * Helper for tracking the state of apps.
 *
 * Created by Oasis on 2019-1-23.
 */
public class AppStateTrackingHelper {

	/** Force refresh the state of specified app when activity is resumed */
	public static void requestSyncWhenResumed(final Activity activity, final String pkg, final UserHandle user) {
		activity.getFragmentManager().beginTransaction().add(AppStateSyncFragment.create(pkg, user), pkg + "@" + Users.toId(user)).commit();
	}

	@SuppressWarnings("deprecation") public static class AppStateSyncFragment extends Fragment {

		private static final String KEY_PACKAGE = "package", KEY_USER = "user";

		@Override public void onResume() {
			super.onResume();
			final Activity activity = getActivity(); final Bundle args = getArguments();
			final String pkg = args.getString(KEY_PACKAGE); final UserHandle user = args.getParcelable(KEY_USER);
			if (pkg != null && user != null)
				PrismAppListProvider.getInstance(activity).refreshPackage(pkg, user, false);
			activity.getFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
		}

		private static AppStateSyncFragment create(final String pkg, final UserHandle user) {
			final AppStateSyncFragment fragment = new AppStateSyncFragment();
			final Bundle args = new Bundle();
			args.putString(KEY_PACKAGE, pkg);
			args.putParcelable(KEY_USER, user);
			fragment.setArguments(args);
			return fragment;
		}
	}
}
