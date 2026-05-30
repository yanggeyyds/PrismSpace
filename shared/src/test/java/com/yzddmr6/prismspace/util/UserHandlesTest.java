package com.yzddmr6.prismspace.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UserHandlesTest {

	@Test public void uidMathSplitsAndComposesAcrossUsers() {
		final int uid = 112345;

		assertEquals(1, UserHandles.getUserId(uid));
		assertEquals(12345, UserHandles.getAppId(uid));
		assertEquals(1023456, UserHandles.getUid(10, 123456));
	}

	@Test public void systemUserIdIsZero() {
		assertEquals(0, UserHandles.USER_SYSTEM);
	}
}
