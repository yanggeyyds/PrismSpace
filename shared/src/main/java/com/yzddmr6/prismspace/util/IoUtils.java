package com.yzddmr6.prismspace.util;

import java.io.Closeable;
import java.io.InputStream;

import androidx.annotation.Nullable;

public final class IoUtils {

	public static void closeQuietly(final @Nullable Closeable closeable) {
		if (closeable == null) return;
		try {
			closeable.close();
		} catch (final RuntimeException e) {
			throw e;
		} catch (final Exception ignored) {}
	}

	public static void closeQuietly(final @Nullable InputStream stream) {
		closeQuietly((Closeable) stream);
	}

	private IoUtils() {}
}
