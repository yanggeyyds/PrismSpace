package com.yzddmr6.prismspace.provisioning;

import com.yzddmr6.prismspace.engine.BuildConfig;
import com.yzddmr6.prismspace.util.DevicePolicies;
import com.yzddmr6.prismspace.util.Loopers;
import com.yzddmr6.prismspace.util.ProfileUser;
import com.yzddmr6.prismspace.util.Users;
import com.yzddmr6.prismspace.util.PseudoContentProvider;
import com.yzddmr6.prismspace.perf.Performances;
import com.yzddmr6.prismspace.perf.Stopwatch;

/**
 * Perform incremental provision
 *
 * Created by Oasis on 2017/11/21.
 */
public class AutoIncrementalProvision extends PseudoContentProvider {

	@Override public boolean onCreate() {
		final Stopwatch stopwatch = Performances.startUptimeStopwatch();
		if (Users.isParentProfile()) {
			Loopers.addIdleTask(() -> PrismProvisioning.startOwnerUserPostProvisioningIfNeeded(context()));
		} else if (new DevicePolicies(context()).isProfileOwner()) {	// False if profile is not enabled yet. (during the broadcast ACTION_PROFILE_PROVISIONING_COMPLETE)
			final Thread thread = new Thread(this::startInProfile);
			thread.setPriority(Thread.MIN_PRIORITY);
			thread.start();
		}
		if (BuildConfig.DEBUG) Performances.check(stopwatch, 5, "IncPro.MainThread");
		return false;
	}

	@ProfileUser private void startInProfile() {
		final Stopwatch stopwatch = Performances.startUptimeStopwatch();
		PrismProvisioning.performIncrementalProfileOwnerProvisioningIfNeeded(context());
		if (BuildConfig.DEBUG) Performances.check(stopwatch, 10, "IncPro.WorkerThread");
	}
}
