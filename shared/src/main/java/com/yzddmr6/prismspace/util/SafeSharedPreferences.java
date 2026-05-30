package com.yzddmr6.prismspace.util;

import android.content.SharedPreferences;

import com.yzddmr6.prismspace.shared.BuildConfig;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class SafeSharedPreferences implements SharedPreferences {

	public static SharedPreferences wrap(final SharedPreferences prefs) {
		if (prefs instanceof SafeSharedPreferences) return prefs;
		return new SafeSharedPreferences(prefs);
	}

	private SafeSharedPreferences(final SharedPreferences delegate) {
		mDelegate = delegate;
	}

	@Override public void registerOnSharedPreferenceChangeListener(final OnSharedPreferenceChangeListener listener) {
		if (BuildConfig.DEBUG && listener.getClass().isAnonymousClass())
			throw new Error("Never use anonymous inner class for listener, since it is weakly-referenced by SharedPreferences instance.");
		mDelegate.registerOnSharedPreferenceChangeListener(listener);
	}

	@Override public void unregisterOnSharedPreferenceChangeListener(final OnSharedPreferenceChangeListener listener) {
		mDelegate.unregisterOnSharedPreferenceChangeListener(listener);
	}

	@Override public String getString(final String key, final String defValue) {
		try {
			return mDelegate.getString(key, defValue);
		} catch (final ClassCastException e) {
			return defValue;
		}
	}

	@Override public Set<String> getStringSet(final String key, final Set<String> defValues) {
		try {
			final Set<String> values = mDelegate.getStringSet(key, defValues);
			return values == null ? null : Collections.unmodifiableSet(values);
		} catch (final ClassCastException e) {
			return defValues;
		}
	}

	@Override public int getInt(final String key, final int defValue) {
		try {
			return mDelegate.getInt(key, defValue);
		} catch (final ClassCastException e) {
			return defValue;
		}
	}

	@Override public long getLong(final String key, final long defValue) {
		try {
			return mDelegate.getLong(key, defValue);
		} catch (final ClassCastException e) {
			return defValue;
		}
	}

	@Override public float getFloat(final String key, final float defValue) {
		try {
			return mDelegate.getFloat(key, defValue);
		} catch (final ClassCastException e) {
			return defValue;
		}
	}

	@Override public boolean getBoolean(final String key, final boolean defValue) {
		try {
			return mDelegate.getBoolean(key, defValue);
		} catch (final ClassCastException e) {
			return defValue;
		}
	}

	@Override public Map<String, ?> getAll() {
		return Collections.unmodifiableMap(mDelegate.getAll());
	}

	@Override public boolean contains(final String key) {
		return mDelegate.contains(key);
	}

	@Override public Editor edit() {
		return mDelegate.edit();
	}

	private final SharedPreferences mDelegate;
}
