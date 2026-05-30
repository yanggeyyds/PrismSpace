package com.yzddmr6.prismspace.util;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class Suppliers {

	public static <T> Supplier<T> memoize(final Supplier<T> delegate) {
		if (delegate instanceof NonSerializableMemoizingSupplier || delegate instanceof MemoizingSupplier) return delegate;
		return delegate instanceof Serializable ? new MemoizingSupplier<>(delegate) : new NonSerializableMemoizingSupplier<>(delegate);
	}

	public static <T> Supplier<T> memoizeWithExpiration(final Supplier<T> delegate, final long duration, final TimeUnit unit) {
		return new ExpiringMemoizingSupplier<>(delegate, duration, unit);
	}

	private static final class MemoizingSupplier<T> implements Supplier<T>, Serializable {
		private final Supplier<T> delegate;
		private transient volatile boolean initialized;
		private transient T value;

		private MemoizingSupplier(final Supplier<T> delegate) {
			this.delegate = Objects.requireNonNull(delegate);
		}

		@Override public T get() {
			if (! initialized) synchronized (this) {
				if (! initialized) {
					value = delegate.get();
					initialized = true;
				}
			}
			return value;
		}

		private static final long serialVersionUID = 1L;
	}

	private static final class NonSerializableMemoizingSupplier<T> implements Supplier<T> {
		private volatile Supplier<T> delegate;
		private volatile boolean initialized;
		private T value;

		private NonSerializableMemoizingSupplier(final Supplier<T> delegate) {
			this.delegate = Objects.requireNonNull(delegate);
		}

		@Override public T get() {
			if (! initialized) synchronized (this) {
				if (! initialized) {
					value = delegate.get();
					delegate = null;
					initialized = true;
				}
			}
			return value;
		}
	}

	private static final class ExpiringMemoizingSupplier<T> implements Supplier<T>, Serializable {
		private final Supplier<T> delegate;
		private final long durationNanos;
		private transient volatile T value;
		private transient volatile long expirationNanos;

		private ExpiringMemoizingSupplier(final Supplier<T> delegate, final long duration, final TimeUnit unit) {
			this.delegate = Objects.requireNonNull(delegate);
			Objects.requireNonNull(unit);
			if (duration <= 0) throw new IllegalArgumentException("duration must be positive");
			durationNanos = unit.toNanos(duration);
		}

		@Override public T get() {
			long expires = expirationNanos;
			final long now = System.nanoTime();
			if (expires == 0 || now - expires >= 0) synchronized (this) {
				if (expires == expirationNanos) {
					value = delegate.get();
					expires = now + durationNanos;
					expirationNanos = expires == 0 ? 1 : expires;
				}
			}
			return value;
		}

		private static final long serialVersionUID = 1L;
	}

	private Suppliers() {}
}
