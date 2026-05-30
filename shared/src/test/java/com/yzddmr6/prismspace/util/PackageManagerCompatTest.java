package com.yzddmr6.prismspace.util;

import android.content.Context;
import android.content.pm.PackageManager;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PackageManagerCompatTest {

	@Test public void packageUidUsesLegacyUserAwareApiOnHostJvm() throws Exception {
		final Context context = mock(Context.class);
		final PackageManager packageManager = mock(PackageManager.class);
		when(context.getPackageManager()).thenReturn(packageManager);
		when(packageManager.getPackageUid("pkg.test", 10)).thenReturn(1012345);

		assertEquals(1012345, new PackageManagerCompat(context).getPackageUid("pkg.test", 10));
	}
}
