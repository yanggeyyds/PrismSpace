package com.yzddmr6.prismspace.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AppsTest {

	@Test public void installedInCurrentUserReadsInstalledFlag() {
		final ApplicationInfo info = new ApplicationInfo();

		info.flags = ApplicationInfo.FLAG_INSTALLED;
		assertTrue(Apps.isInstalledInCurrentUser(info));

		info.flags = 0;
		assertFalse(Apps.isInstalledInCurrentUser(info));
	}

	@Test public void systemAppReadsSystemFlag() {
		final ApplicationInfo info = new ApplicationInfo();

		info.flags = ApplicationInfo.FLAG_SYSTEM;
		assertTrue(Apps.isSystem(info));

		info.flags = 0;
		assertFalse(Apps.isSystem(info));
	}

	@Test public void appNamesJoinResolvedLabels() throws Exception {
		final Context context = mock(Context.class);
		final PackageManager packageManager = mock(PackageManager.class);
		final ApplicationInfo first = appInfo("pkg.one");
		final ApplicationInfo second = appInfo("pkg.two");

		when(context.getPackageManager()).thenReturn(packageManager);
		when(packageManager.getApplicationInfo(eq("pkg.one"), anyInt())).thenReturn(first);
		when(packageManager.getApplicationInfo(eq("pkg.two"), anyInt())).thenReturn(second);
		when(packageManager.getApplicationLabel(second)).thenThrow(new RuntimeException("missing label"));

		assertEquals("pkg.one, pkg.two", Apps.of(context).getAppNames(Arrays.asList("pkg.one", "pkg.two"), ", "));
	}

	private static ApplicationInfo appInfo(final String pkg) {
		final ApplicationInfo info = new ApplicationInfo();
		info.packageName = pkg;
		return info;
	}
}
