package com.lorentzos.rxexperiment;

import rx.functions.Action1;

/**
 *
 */
public class ContinuousOperatorDoOnNext<T> implements ContinuousOperator<T, T> {

	private final Action1<? super T> onNext;

	public ContinuousOperatorDoOnNext(Action1<? super T> onNext) {
		this.onNext = onNext;
	}

	@Override
	public ContinuousSubscriber<? super T> call(ContinuousSubscriber<? super T> subscriber) {
		return new ContinuousSubscriber<T>(subscriber) {
			@Override
			public void onNext(T value) {
				onNext.call(value);
				subscriber.onNext(value);
			}
		};
	}
}
