package com.yzddmr6.prismspace.util;

import org.junit.Test;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertTrue;

public class IoUtilsTest {

	@Test public void closeQuietlyClosesCloseable() {
		final AtomicBoolean closed = new AtomicBoolean();

		IoUtils.closeQuietly((Closeable) () -> closed.set(true));

		assertTrue(closed.get());
	}

	@Test public void closeQuietlyIgnoresCheckedException() {
		IoUtils.closeQuietly((Closeable) () -> { throw new IOException("ignored"); });
	}

	@Test(expected = IllegalStateException.class) public void closeQuietlyKeepsRuntimeExceptionVisible() {
		IoUtils.closeQuietly((Closeable) () -> { throw new IllegalStateException("visible"); });
	}

	@Test public void closeQuietlyAcceptsNull() {
		IoUtils.closeQuietly((Closeable) null);
	}
}
