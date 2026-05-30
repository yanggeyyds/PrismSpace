package com.yzddmr6.prismspace.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VersionsTest {

	@Test public void androidVersionNumbersMatchKnownSdkLevels() {
		assertEquals("15", Versions.getAndroidVersionNumber(35));
		assertEquals("14", Versions.getAndroidVersionNumber(34));
		assertEquals("12L", Versions.getAndroidVersionNumber(32));
		assertEquals("8.1", Versions.getAndroidVersionNumber(27));
		assertEquals("4.4", Versions.getAndroidVersionNumber(19));
	}

	@Test public void androidVersionNumbersHandleFutureSdkLevels() {
		assertEquals("16", Versions.getAndroidVersionNumber(36));
		assertEquals("17", Versions.getAndroidVersionNumber(37));
	}

	@Test public void androidVersionNumbersHandleVeryOldSdkLevels() {
		assertEquals("2.x", Versions.getAndroidVersionNumber(10));
		assertEquals("1.x", Versions.getAndroidVersionNumber(4));
	}
}
