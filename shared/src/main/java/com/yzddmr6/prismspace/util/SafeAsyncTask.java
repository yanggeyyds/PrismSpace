package com.yzddmr6.prismspace.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.AsyncTask;

import java.lang.ref.WeakReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nullable;

/** Minimal Activity-safe AsyncTask helper for legacy setup and provisioning flows. */
public final class SafeAsyncTask {

	@SuppressLint("StaticFieldLeak")
	public static <T> void execute(final Activity activity, final Function<Activity, T> task, final BiConsumer<Activity, T> finish) {
		final WeakReference<Activity> reference = new WeakReference<>(activity);
		new AsyncTask<Void, Void, T>() {
			@Override protected T doInBackground(final Void... voids) {
				final Activity activity = ifActive(reference.get());
				return activity != null ? task.apply(activity) : null;
			}

			@Override protected void onPostExecute(final T result) {
				final Activity activity = ifActive(reference.get());
				if (activity != null) finish.accept(activity, result);
			}
		}.execute();
	}

	public static void execute(final Activity activity, final Consumer<Activity> task, final Consumer<Activity> finish) {
		execute(activity, a -> { task.accept(a); return a; }, (a, r) -> finish.accept(a));
	}

	private static @Nullable Activity ifActive(final Activity activity) {
		return activity == null || activity.isFinishing() || activity.isDestroyed() ? null : activity;
	}

	private SafeAsyncTask() {}
}
