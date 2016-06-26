package com.lorentzos.rxexperiment.continuous;

import rx.functions.Func1;

/**
 *
 */
enum Functions {

	;
	static final Func1<Object, Object> IDENTITY = v -> v;

	@SuppressWarnings("unchecked")
	public static <T> Func1<T, T> identity() {
		return (Func1<T, T>) IDENTITY;
	}
}
