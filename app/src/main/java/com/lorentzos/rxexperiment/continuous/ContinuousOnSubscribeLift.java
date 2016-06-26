package com.lorentzos.rxexperiment.continuous;

import rx.Subscription;

/**
 *
 */
public class ContinuousOnSubscribeLift<Upstream, Downstream> implements Continuous.OnSubscribe<Upstream> {

	private final ContinuousOperator<? extends Upstream, ? super Downstream> operator;
	private final Continuous<? extends Downstream> source;

	public ContinuousOnSubscribeLift(Continuous<? extends Downstream> continuous, ContinuousOperator<? extends Upstream, ? super Downstream> operator) {
		source = continuous;
		this.operator = operator;
	}

	@Override
	public void call(ContinuousSubscriber<? super Upstream> child) {
		ContinuousSubscriber<? super Downstream> subscriber = operator.call(child);

		Subscription subscription = source.subscribe(subscriber);
		child.add(subscription);
	}
}
