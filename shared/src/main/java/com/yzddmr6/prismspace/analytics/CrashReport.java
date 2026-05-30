package com.yzddmr6.prismspace.analytics;

import android.os.Process;

import androidx.annotation.NonNull;

import com.yzddmr6.prismspace.shared.BuildConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Lazy initializer for crash handler.
 *
 * Created by Oasis on 2017/7/14.
 */
public abstract class CrashReport {

	static void logException(final Throwable t) { DiagnosticLog.INSTANCE.e(TAG, "Reported exception", t); }
	static void log(final String message) { DiagnosticLog.INSTANCE.d(TAG, message); }
	static void setProperty(final String key, final String value) {
		sProperties.put(key, value);
		DiagnosticLog.INSTANCE.d(TAG, "property " + key + "=" + value);
	}
	static void setProperty(final String key, final int value) { setProperty(key, String.valueOf(value)); }
	static void setProperty(final String key, final boolean value) { setProperty(key, String.valueOf(value)); }

	public static void initCrashHandler() {
		final Thread.UncaughtExceptionHandler current_exception_handler = Thread.getDefaultUncaughtExceptionHandler();
		if (! (current_exception_handler instanceof LocalThreadExceptionHandler))
			Thread.setDefaultUncaughtExceptionHandler(new LocalThreadExceptionHandler(current_exception_handler));
	}

	private static class LocalThreadExceptionHandler implements Thread.UncaughtExceptionHandler {

		@Override public void uncaughtException(final @NonNull Thread thread, final @NonNull Throwable e) {
			if (BuildConfig.DEBUG) DiagnosticLog.INSTANCE.e(TAG, "Handling:", e);
			DiagnosticLog.INSTANCE.e(TAG, "Uncaught exception in " + thread.getName() + " user=" + Process.myUserHandle().hashCode() + " properties=" + sProperties, e);
			if (mOriginalHandler != null) mOriginalHandler.uncaughtException(thread, e);
		}

		LocalThreadExceptionHandler(final Thread.UncaughtExceptionHandler default_handler) {
			mOriginalHandler = default_handler;
		}

		private final Thread.UncaughtExceptionHandler mOriginalHandler;
	}

	private static final Map<String, String> sProperties = new ConcurrentHashMap<>();
	private static final String TAG = "Prism.Crash";
}
