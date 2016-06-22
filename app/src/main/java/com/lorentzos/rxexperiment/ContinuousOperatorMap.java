package com.lorentzos.rxexperiment;

import rx.functions.Func1;

/**
 *
 */
public class ContinuousOperatorMap<Downstream, Upstream> implements ContinuousOperator<Downstream, Upstream> {

	private final Func1<? super Upstream, ? extends Downstream> mapper;

	public ContinuousOperatorMap(Func1<? super Upstream, ? extends Downstream> mapper) {
		this.mapper = mapper;
	}

	@Override
	public ContinuousSubscriber<? super Upstream> call(ContinuousSubscriber<? super Downstream> continuousSubscriber) {

		return new ContinuousSubscriber<Upstream>() {
			@Override
			public void onNext(Upstream upstream) {
				Downstream downstream = mapper.call(upstream);
				continuousSubscriber.onNext(downstream);
			}
		};
	}
}
