package com.yzddmr6.prismspace.util;

import android.content.SharedPreferences;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SafeSharedPreferencesTest {

	@Test public void gettersReturnDefaultsOnTypeMismatch() {
		final SharedPreferences delegate = mock(SharedPreferences.class);
		when(delegate.getString("string", "fallback")).thenThrow(new ClassCastException());
		when(delegate.getInt("int", 7)).thenThrow(new ClassCastException());
		when(delegate.getLong("long", 8L)).thenThrow(new ClassCastException());
		when(delegate.getFloat("float", 9F)).thenThrow(new ClassCastException());
		when(delegate.getBoolean("boolean", true)).thenThrow(new ClassCastException());

		final SharedPreferences prefs = SafeSharedPreferences.wrap(delegate);

		assertEquals("fallback", prefs.getString("string", "fallback"));
		assertEquals(7, prefs.getInt("int", 7));
		assertEquals(8L, prefs.getLong("long", 8L));
		assertEquals(9F, prefs.getFloat("float", 9F), 0F);
		assertEquals(true, prefs.getBoolean("boolean", true));
	}

	@Test public void getStringSetReturnsDefaultOnTypeMismatch() {
		final SharedPreferences delegate = mock(SharedPreferences.class);
		final Set<String> fallback = Collections.singleton("fallback");
		when(delegate.getStringSet("set", fallback)).thenThrow(new ClassCastException());

		assertEquals(fallback, SafeSharedPreferences.wrap(delegate).getStringSet("set", fallback));
	}

	@Test(expected = UnsupportedOperationException.class) public void getStringSetReturnsImmutableSet() {
		final SharedPreferences delegate = mock(SharedPreferences.class);
		final Set<String> set = new HashSet<>();
		set.add("value");
		when(delegate.getStringSet("set", null)).thenReturn(set);

		SafeSharedPreferences.wrap(delegate).getStringSet("set", null).add("other");
	}

	@Test(expected = UnsupportedOperationException.class) public void getAllReturnsImmutableMap() {
		final SharedPreferences delegate = mock(SharedPreferences.class);
		final Map<String, Object> values = new HashMap<>();
		values.put("key", "value");
		doReturn(values).when(delegate).getAll();

		@SuppressWarnings({ "rawtypes", "unchecked" }) final Map mutableView = SafeSharedPreferences.wrap(delegate).getAll();
		mutableView.put("other", "value");
	}

	@Test public void wrapIsIdempotent() {
		final SharedPreferences delegate = mock(SharedPreferences.class);
		final SharedPreferences wrapped = SafeSharedPreferences.wrap(delegate);

		assertEquals(wrapped, SafeSharedPreferences.wrap(wrapped));
	}
}
