package com.yzddmr6.prismspace;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.yzddmr6.prismspace.provisioning.PrismProvisioning;
import com.yzddmr6.prismspace.util.Users;

/**
 * Handle {@link Intent#ACTION_MY_PACKAGE_REPLACED}
 *
 * Created by Oasis on 2017/7/20.
 */
public class AppUpdateReceiver extends BroadcastReceiver {

	@Override public void onReceive(final Context context, final Intent intent) {
		// Provisioning is idempotent, so it is safe to trigger on every package replacement.
		if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction()))
			if (Users.isParentProfile()) PrismProvisioning.startOwnerUserPostProvisioningIfNeeded(context);
	}
}
