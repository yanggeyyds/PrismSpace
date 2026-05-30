package com.yzddmr6.prismspace.util;

import android.os.Parcel;
import android.os.Process;
import android.os.UserHandle;
import android.util.Pair;

import androidx.annotation.VisibleForTesting;

import com.yzddmr6.prismspace.util.annotation.AppIdInt;
import com.yzddmr6.prismspace.util.annotation.UserIdInt;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.N;

/**
 * Utilities for Android multi-user IDs and {@link UserHandle}.
 */
public final class UserHandles {

	/** A user id constant to indicate the "system" user of the device. */
	public static final @UserIdInt int USER_SYSTEM = 0;

	/** Range of UIDs allocated for each Android user. */
	private static final int PER_USER_RANGE = 100000;

	public static final UserHandle MY_USER_HANDLE = currentUserHandle();
	public static final @UserIdInt int MY_USER_ID = MY_USER_HANDLE != null ? getIdentifier(MY_USER_HANDLE) : USER_SYSTEM;

	/** A user handle to indicate the "system" user of the device. */
	public static final UserHandle SYSTEM = MY_USER_ID == USER_SYSTEM && MY_USER_HANDLE != null ? MY_USER_HANDLE : from(USER_SYSTEM);

	@VisibleForTesting static Pair<Integer, UserHandle> sCache = null;

	/**
	 * Enable multi-user related side effects. Set this to false if there are problems with single-user use-cases.
	 */
	private static final boolean MU_ENABLED = true;

	public static UserHandle getUserHandleForUid(final int uid) {
		return SDK_INT >= N ? UserHandle.getUserHandleForUid(uid) : of(getUserId(uid));
	}

	public static UserHandle of(final @UserIdInt int userId) {
		if (userId == USER_SYSTEM) return SYSTEM;
		final Pair<Integer, UserHandle> cache = sCache;
		if (cache != null && cache.first == userId) return cache.second;
		final UserHandle user = from(userId);
		sCache = new Pair<>(userId, user);
		return user;
	}

	private static UserHandle from(final @UserIdInt int userId) {
		if (MY_USER_HANDLE != null && MY_USER_HANDLE.hashCode() == userId) return MY_USER_HANDLE;
		Parcel parcel = null;
		try {
			parcel = Parcel.obtain();
			final int begin = parcel.dataPosition();
			parcel.writeInt(userId);
			parcel.setDataPosition(begin);
			return UserHandle.CREATOR.createFromParcel(parcel);
		} catch (final RuntimeException e) {
			if (isHostUnitTestAndroidStub(e)) return null;
			throw e;
		} finally {
			if (parcel != null) parcel.recycle();
		}
	}

	/** Returns the Android user ID for a given Linux UID. */
	public static @UserIdInt int getUserId(final int uid) {
		return MU_ENABLED ? uid / PER_USER_RANGE : USER_SYSTEM;
	}

	/** Returns the app ID for a given Linux UID, stripping out the Android user ID. */
	public static @AppIdInt int getAppId(final int uid) {
		return uid % PER_USER_RANGE;
	}

	/** Returns the Linux UID composed from the Android user ID and app ID. */
	public static int getUid(final @UserIdInt int userId, final @AppIdInt int appId) {
		return MU_ENABLED ? userId * PER_USER_RANGE + (appId % PER_USER_RANGE) : appId;
	}

	/** Returns the userId stored in this UserHandle. */
	public static @UserIdInt int getIdentifier(final UserHandle handle) {
		return handle.hashCode();
	}

	private static UserHandle currentUserHandle() {
		try {
			return Process.myUserHandle();
		} catch (final RuntimeException e) {
			if (isHostUnitTestAndroidStub(e)) return null;
			throw e;
		}
	}

	private static boolean isHostUnitTestAndroidStub(final RuntimeException e) {
		final String message = e.getMessage();
		return message != null && message.contains("not mocked");
	}

	private UserHandles() {}
}
