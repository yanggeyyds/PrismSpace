package com.yzddmr6.prismspace.util;

import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static android.app.PendingIntent.FLAG_NO_CREATE;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

import android.app.Activity;
import android.app.Application.ActivityLifecycleCallbacks;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import java.util.HashSet;
import java.util.Set;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;

public final class Scopes {

	public static final String KPrefsNameAppScope = "app.scope";
	public static final String KPrefsNameVersionScope = "version.scope";

	public interface Scope {

		boolean isMarked(@NonNull String tag);

		@CheckResult boolean mark(@NonNull String tag);

		void markOnly(@NonNull String tag);

		boolean unmark(@NonNull String tag);

		default ScopedTag tag(@NonNull final String tag) { return new ScopedTag(this, tag); }
	}

	public static class ScopedTag {

		public boolean isMarked() { return mScope.isMarked(mTag); }

		public void markOnly() { mScope.markOnly(mTag); }

		public boolean unmark() { return mScope.unmark(mTag); }

		ScopedTag(final Scope scope, final String tag) {
			mScope = scope;
			mTag = tag;
		}

		private final Scope mScope;
		private final String mTag;
	}

	public static Scope app(final Context context) { return new AppInstallationScope(context); }

	public static Scope version(final Context context) { return new PackageVersionScope(context); }

	public static Scope update(final Context context) { return new PackageUpdateScope(context); }

	public static Scope boot(final Context context) { return new DeviceBootScope(context); }

	public static Scope process() { return ProcessScope.sSingleton; }

	public static Scope session(final Activity activity) {
		if (SessionScope.sSingleton == null) SessionScope.sSingleton = new SessionScope(activity);
		return SessionScope.sSingleton;
	}

	private Scopes() {}
}

class SessionScope extends MemoryBasedScopeImpl {

	private static final int SESSION_TIMEOUT = 5 * 60 * 1000;

	SessionScope(final Activity activity) {
		activity.getApplication().registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {

			@Override public void onActivityResumed(final Activity activity) {
				if (System.currentTimeMillis() >= mTimeLastSession + SESSION_TIMEOUT) mSeen.clear();
			}

			@Override public void onActivityPaused(final Activity activity) {
				mTimeLastSession = System.currentTimeMillis();
			}

			@Override public void onActivityCreated(final Activity activity, final Bundle state) {}

			@Override public void onActivityStarted(final Activity activity) {}

			@Override public void onActivityStopped(final Activity activity) {}

			@Override public void onActivitySaveInstanceState(final Activity activity, final Bundle state) {}

			@Override public void onActivityDestroyed(final Activity activity) {}
		});
	}

	private long mTimeLastSession;

	static SessionScope sSingleton;
}

class DeviceBootScope implements Scopes.Scope {

	@Override public boolean isMarked(@NonNull final String tag) {
		return PendingIntent.getBroadcast(mContext, 0, makeIntent(tag), FLAG_NO_CREATE | FLAG_IMMUTABLE) != null;
	}

	@Override public boolean mark(@NonNull final String tag) {
		final Intent intent = makeIntent(tag);
		final boolean already_marked = PendingIntent.getBroadcast(mContext, 0, intent, FLAG_NO_CREATE | FLAG_IMMUTABLE) != null;
		PendingIntent.getBroadcast(mContext, 0, intent, FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE);
		return ! already_marked;
	}

	@Override public void markOnly(@NonNull final String tag) {
		PendingIntent.getBroadcast(mContext, 0, makeIntent(tag), FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE);
	}

	@Override public boolean unmark(@NonNull final String tag) {
		final PendingIntent mark = PendingIntent.getBroadcast(mContext, 0, makeIntent(tag), FLAG_NO_CREATE | FLAG_IMMUTABLE);
		if (mark == null) return false;
		mark.cancel();
		return true;
	}

	private Intent makeIntent(final String tag) {
		return new Intent("SCOPE:" + tag).setPackage(mContext.getPackageName());
	}

	DeviceBootScope(final Context context) {
		mContext = context.getApplicationContext();
	}

	private final Context mContext;
}

class ProcessScope extends MemoryBasedScopeImpl {

	static final Scopes.Scope sSingleton = new ProcessScope();
}

class MemoryBasedScopeImpl implements Scopes.Scope {

	@Override public boolean isMarked(@NonNull final String tag) {
		return mSeen.contains(tag);
	}

	@Override public boolean mark(@NonNull final String tag) {
		return mSeen.add(tag);
	}

	@Override public void markOnly(@NonNull final String tag) {
		mSeen.add(tag);
	}

	@Override public boolean unmark(@NonNull final String tag) {
		return mSeen.remove(tag);
	}

	final Set<String> mSeen = new HashSet<>();
}

class PackageUpdateScope extends SharedPrefsBasedScopeImpl {

	private static final String PREFS_NAME_UPDATE_SCOPE = "update.scope";
	private static final String PREFS_KEY_LAST_UPDATE_TIME = "update-time";

	PackageUpdateScope(final Context context) {
		super(resetIfLastUpdateTimeChanged(context, context.getSharedPreferences(PREFS_NAME_UPDATE_SCOPE, Context.MODE_PRIVATE)));
	}

	private static SharedPreferences resetIfLastUpdateTimeChanged(final Context context, final SharedPreferences prefs) {
		final long last_update_time = Versions.lastUpdateTime(context);
		if (last_update_time != prefs.getLong(PREFS_KEY_LAST_UPDATE_TIME, 0))
			prefs.edit().clear().putLong(PREFS_KEY_LAST_UPDATE_TIME, last_update_time).apply();
		return prefs;
	}
}

class PackageVersionScope extends SharedPrefsBasedScopeImpl {

	private static final String PREFS_KEY_VERSION_CODE = "version-code";

	PackageVersionScope(final Context context) {
		super(resetIfVersionChanges(context, context.getSharedPreferences(Scopes.KPrefsNameVersionScope, Context.MODE_PRIVATE)));
	}

	private static SharedPreferences resetIfVersionChanges(final Context context, final SharedPreferences prefs) {
		final int version = Versions.code(context);
		if (version != prefs.getInt(PREFS_KEY_VERSION_CODE, 0))
			prefs.edit().clear().putInt(PREFS_KEY_VERSION_CODE, version).apply();
		return prefs;
	}
}

class AppInstallationScope extends SharedPrefsBasedScopeImpl {

	AppInstallationScope(final Context context) {
		super(context.getSharedPreferences(Scopes.KPrefsNameAppScope, Context.MODE_PRIVATE));
	}
}

class SharedPrefsBasedScopeImpl implements Scopes.Scope {

	private static final String PREFS_KEY_PREFIX = "mark-";
	private static final String PREFS_KEY_PREFIX_LEGACY = "first-time-";

	@Override public boolean isMarked(@NonNull final String tag) {
		return mPrefs.getBoolean(PREFS_KEY_PREFIX + tag, false);
	}

	@Override public boolean mark(@NonNull final String tag) {
		final String key = PREFS_KEY_PREFIX + tag;
		if (mPrefs.getBoolean(key, false)) return false;
		mPrefs.edit().putBoolean(key, true).apply();
		return true;
	}

	@Override public void markOnly(@NonNull final String tag) {
		mPrefs.edit().putBoolean(PREFS_KEY_PREFIX + tag, true).apply();
	}

	@Override public boolean unmark(@NonNull final String tag) {
		final String key = PREFS_KEY_PREFIX + tag;
		if (! mPrefs.getBoolean(key, false)) return false;
		mPrefs.edit().putBoolean(key, false).apply();
		return true;
	}

	SharedPrefsBasedScopeImpl(final SharedPreferences prefs) {
		mPrefs = prefs;
		SharedPreferences.Editor editor = null;
		for (final String key : prefs.getAll().keySet()) {
			if (! key.startsWith(PREFS_KEY_PREFIX_LEGACY)) continue;
			if (editor == null) editor = prefs.edit();
			try {
				editor.putBoolean(PREFS_KEY_PREFIX + key.substring(PREFS_KEY_PREFIX_LEGACY.length()), ! prefs.getBoolean(key, true));
			} catch (final ClassCastException ignored) {}
			editor.remove(key);
		}
		if (editor != null) editor.apply();
	}

	private final SharedPreferences mPrefs;
}
