package com.yzddmr6.prismspace.util;

import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.os.UserHandle;

import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LauncherAppsCompatTest {

	@Test public void getApplicationInfoNoThrowsReturnsNullWhenLauncherAppsThrowsNameNotFound() throws Exception {
		final LauncherApps launcherApps = mock(LauncherApps.class);
		when(launcherApps.getApplicationInfo(eq("missing.pkg"), eq(0), isNull(UserHandle.class)))
				.thenThrow(new PackageManager.NameNotFoundException("missing.pkg"));

		assertNull(LauncherAppsCompat.getApplicationInfoNoThrows(launcherApps, "missing.pkg", 0, null));
	}
}
