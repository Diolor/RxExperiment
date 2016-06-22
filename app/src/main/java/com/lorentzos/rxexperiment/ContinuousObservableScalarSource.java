package com.lorentzos.rxexperiment;

/**
 *
 */
public class ContinuousObservableScalarSource<T> extends Continuous<T> {

	public ContinuousObservableScalarSource(T value) {
		super(continuousSubscriber -> continuousSubscriber.onNext(value));
	}

}
