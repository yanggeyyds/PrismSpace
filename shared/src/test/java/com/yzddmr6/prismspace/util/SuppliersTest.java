package com.yzddmr6.prismspace.util;

import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;

public class SuppliersTest {

	@Test public void memoizeCallsDelegateOnlyOnce() {
		final AtomicInteger calls = new AtomicInteger();
		final Supplier<Integer> supplier = Suppliers.memoize(calls::incrementAndGet);

		assertEquals(1, supplier.get().intValue());
		assertEquals(1, supplier.get().intValue());
		assertEquals(1, calls.get());
	}

	@Test public void memoizeWithExpirationRefreshesAfterDuration() throws InterruptedException {
		final AtomicInteger calls = new AtomicInteger();
		final Supplier<Integer> supplier = Suppliers.memoizeWithExpiration(calls::incrementAndGet, 10, TimeUnit.MILLISECONDS);

		assertEquals(1, supplier.get().intValue());
		assertEquals(1, supplier.get().intValue());
		Thread.sleep(30);
		assertEquals(2, supplier.get().intValue());
	}

	@Test(expected = IllegalArgumentException.class) public void memoizeWithExpirationRejectsNonPositiveDuration() {
		Suppliers.memoizeWithExpiration(() -> 1, 0, TimeUnit.MILLISECONDS);
	}
}
