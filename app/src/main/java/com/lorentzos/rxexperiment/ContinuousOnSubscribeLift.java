package com.lorentzos.rxexperiment;

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
	public void call(ContinuousSubscriber<? super Upstream> continuousSubscriber) {
		ContinuousSubscriber<? super Downstream> subscriber = operator.call(continuousSubscriber);

		source.subscribe(subscriber);
	}
}
