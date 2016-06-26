package com.lorentzos.rxexperiment.continuous;

/**
 *
 */
public class ContinuousObservableScalarSource<T> extends Continuous<T> {

	public ContinuousObservableScalarSource(T value) {
		super(continuousSubscriber -> continuousSubscriber.onNext(value));
	}

}
