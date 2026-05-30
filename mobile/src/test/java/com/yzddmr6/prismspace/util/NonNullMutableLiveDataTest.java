package com.yzddmr6.prismspace.util;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import org.junit.Test;
import org.junit.Rule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class NonNullMutableLiveDataTest {

	@Rule public final InstantTaskExecutorRule instantTaskExecutor = new InstantTaskExecutorRule();

	@Test public void initialValueIsRequiredAndReadable() {
		final NonNullMutableLiveData<Boolean> liveData = new NonNullMutableLiveData<>(false);

		assertEquals(false, liveData.getValue());
	}

	@Test public void setValueRejectsNull() {
		final NonNullMutableLiveData<Boolean> liveData = new NonNullMutableLiveData<>(false);

		assertThrows(NullPointerException.class, () -> liveData.setValue(null));
	}

	@Test public void constructorRejectsNull() {
		assertThrows(NullPointerException.class, () -> new NonNullMutableLiveData<Boolean>(null));
	}
}
