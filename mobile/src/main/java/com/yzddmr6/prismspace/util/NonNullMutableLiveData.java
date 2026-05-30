package com.yzddmr6.prismspace.util;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

public class NonNullMutableLiveData<T> extends MutableLiveData<T> {

	public NonNullMutableLiveData(final @NonNull T value) {
		super.setValue(Objects.requireNonNull(value));
	}

	@Override public void setValue(final @NonNull T value) {
		super.setValue(Objects.requireNonNull(value));
	}

	public void notifyChange() {
		super.postValue(super.getValue());
	}

	@SuppressWarnings("ConstantConditions") @NonNull @Override public T getValue() {
		return super.getValue();
	}
}
