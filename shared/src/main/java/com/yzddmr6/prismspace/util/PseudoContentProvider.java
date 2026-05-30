package com.yzddmr6.prismspace.util;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import javax.annotation.Nonnull;

/**
 * Process-local ContentProvider base for initialization hooks that need a Context.
 */
public class PseudoContentProvider extends ContentProvider {

	/** Helper method to eliminate nullness checks. Never call it in the constructor. */
	@SuppressWarnings("ConstantConditions") protected @Nonnull Context context() {
		return getContext();
	}

	@Override public @Nullable String getType(final @NonNull Uri uri) {
		return null;
	}

	@Override public @Nullable Uri insert(final @NonNull Uri uri, final @Nullable ContentValues values) {
		return null;
	}

	@Override public int delete(final @NonNull Uri uri, final @Nullable String selection, final @Nullable String[] selectionArgs) {
		return 0;
	}

	@Override public int update(final @NonNull Uri uri, final @Nullable ContentValues values, final @Nullable String selection,
			final @Nullable String[] selectionArgs) {
		return 0;
	}

	@Override public @Nullable Cursor query(final @NonNull Uri uri, final @Nullable String[] projection, final @Nullable String selection,
			final @Nullable String[] selectionArgs, final @Nullable String sortOrder) {
		return null;
	}

	/** PseudoContentProvider never registers itself. */
	@Override public boolean onCreate() {
		return false;
	}
}
